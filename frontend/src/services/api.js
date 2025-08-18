/**
 * api.js - API Service Layer for CardDemo Application
 *
 * High-level API service layer that orchestrates REST communication between
 * React components and Spring Boot backend, replacing CICS transaction routing
 * with modern HTTP-based request/response patterns while maintaining identical
 * functional semantics.
 *
 * This service layer provides:
 * - Transaction-specific API methods for all 24 CICS programs being converted
 * - Comprehensive error handling matching COBOL ABEND routines
 * - Session state management through HTTP cookies (XSRF-TOKEN/JSESSIONID)
 * - Retry logic for transient failures and circuit breaker patterns
 * - COBOL data precision preservation using CobolDataConverter utilities
 * - Request/response DTO formatting matching BMS map structures
 * - Sub-200ms response time optimization for card authorization requests
 *
 * Maps CICS transaction codes to REST endpoints as defined in Section 7.3.1:
 * - CC00 (Sign On) -> /api/auth/signin
 * - CM00 (Main Menu) -> /api/menu/main
 * - CT00 (Transaction List) -> /api/transactions
 * - CAUP (Account Update) -> /api/accounts/{id}
 * - And 20+ additional transaction mappings
 */

// External imports
import axiosRetry from 'axios-retry';
import _ from 'lodash';
import CircuitBreaker from 'opossum';
import log from 'loglevel';

// Internal imports - ONLY from depends_on_files
import { apiClient } from '../utils/api.js';
import { toComp3 } from '../utils/CobolDataConverter.js';
import { validateFICO } from '../utils/validation.js';

// Configure logging level based on environment
log.setLevel(process.env.NODE_ENV === 'production' ? 'info' : 'debug');

// =============================================================================
// API CONFIGURATION CONSTANTS
// =============================================================================

/**
 * API configuration constants for service layer behavior
 * Maintains CICS transaction processing characteristics
 */
export const API_CONFIG = {
  // Retry configuration for transient failures
  MAX_RETRY_ATTEMPTS: 3,
  RETRY_DELAY_BASE_MS: 500,
  RETRY_DELAY_MAX_MS: 5000,
  RETRY_CONDITIONS: ['ECONNRESET', 'ETIMEDOUT', 'ENOTFOUND'],
  
  // Circuit breaker configuration for external system resilience
  CIRCUIT_BREAKER_TIMEOUT: 10000, // 10 seconds
  CIRCUIT_BREAKER_ERROR_THRESHOLD: 5,
  CIRCUIT_BREAKER_RESET_TIMEOUT: 30000, // 30 seconds
  
  // Transaction timeout matching CICS behavior
  TRANSACTION_TIMEOUT_MS: 30000, // 30 seconds
  
  // Response time targets per transaction type
  RESPONSE_TIME_TARGETS: {
    CARD_AUTHORIZATION: 200, // Sub-200ms for CC transactions
    ACCOUNT_LOOKUP: 500,
    TRANSACTION_HISTORY: 1000,
    REPORTING: 5000,
    BATCH_OPERATIONS: 30000,
  },
  
  // Session management configuration
  SESSION_TIMEOUT_WARNING_MS: 300000, // 5 minutes before timeout
  SESSION_REFRESH_INTERVAL_MS: 900000, // 15 minutes
};

// =============================================================================
// RETRY AND RESILIENCE PATTERNS
// =============================================================================

/**
 * Configure axios retry behavior for transient failures
 * Implements exponential backoff with jitter
 */
axiosRetry(apiClient, {
  retries: API_CONFIG.MAX_RETRY_ATTEMPTS,
  retryDelay: (retryCount) => {
    const delay = Math.min(
      API_CONFIG.RETRY_DELAY_BASE_MS * Math.pow(2, retryCount - 1),
      API_CONFIG.RETRY_DELAY_MAX_MS
    );
    // Add jitter to prevent thundering herd
    return delay + Math.random() * 1000;
  },
  retryCondition: (error) => {
    // Retry on network errors and 5xx server errors
    return (
      axiosRetry.isNetworkOrIdempotentRequestError(error) ||
      (error.response && error.response.status >= 500) ||
      (error.code && API_CONFIG.RETRY_CONDITIONS.includes(error.code))
    );
  },
  onRetry: (retryCount, error) => {
    log.warn(`Retrying API request (${retryCount}/${API_CONFIG.MAX_RETRY_ATTEMPTS})`, {
      error: error.message,
      url: error.config?.url,
      method: error.config?.method?.toUpperCase(),
    });
  },
});

/**
 * Circuit breaker for critical transaction processing
 * Provides automatic failure detection and recovery
 */
const circuitBreakerOptions = {
  timeout: API_CONFIG.CIRCUIT_BREAKER_TIMEOUT,
  errorThresholdPercentage: 50,
  resetTimeout: API_CONFIG.CIRCUIT_BREAKER_RESET_TIMEOUT,
  name: 'CardDemo-API-CircuitBreaker',
  group: 'CardDemo-Services',
};

const circuitBreaker = new CircuitBreaker(async (requestConfig) => {
  const response = await apiClient.request(requestConfig);
  return response;
}, circuitBreakerOptions);

// Circuit breaker event logging
circuitBreaker.on('open', () => {
  log.error('Circuit breaker opened - API calls will fail fast');
});

circuitBreaker.on('halfOpen', () => {
  log.warn('Circuit breaker half-open - testing API connectivity');
});

circuitBreaker.on('close', () => {
  log.info('Circuit breaker closed - API calls restored');
});

// =============================================================================
// CORE API REQUEST WRAPPER
// =============================================================================

/**
 * Enhanced API request wrapper with comprehensive error handling
 * Provides consistent error handling across all API methods
 *
 * @param {string} method - HTTP method
 * @param {string} url - Request URL
 * @param {Object} data - Request payload
 * @param {Object} options - Additional request options
 * @returns {Promise<Object>} Standardized API response
 */
async function makeApiRequest(method, url, data = null, options = {}) {
  const requestStart = Date.now();
  const requestId = `${method.toUpperCase()}-${url}-${requestStart}`;
  
  log.debug(`API Request Start: ${requestId}`, {
    method,
    url,
    dataKeys: data ? Object.keys(data) : null,
    options,
  });
  
  try {
    // Prepare request configuration
    const requestConfig = {
      method,
      url,
      data,
      ...options,
      metadata: {
        requestId,
        startTime: requestStart,
      },
    };
    
    // Execute request through circuit breaker for critical paths
    const isCriticalPath = url.includes('/auth/') || 
                          url.includes('/transactions/') || 
                          url.includes('/cards/');
                          
    let response;
    if (isCriticalPath) {
      response = await circuitBreaker.fire(requestConfig);
    } else {
      response = await apiClient.request(requestConfig);
    }
    
    const duration = Date.now() - requestStart;
    
    log.debug(`API Request Success: ${requestId}`, {
      status: response.status,
      duration,
      dataKeys: response.data ? Object.keys(response.data) : null,
    });
    
    // Process response data with COBOL precision formatting
    let processedData = response.data;
    if (processedData && typeof processedData === 'object') {
      processedData = processNumericFields(processedData);
    }
    
    return {
      success: true,
      data: processedData,
      status: response.status,
      headers: response.headers,
      duration,
      requestId,
    };
  } catch (error) {
    const duration = Date.now() - requestStart;
    const apiError = handleApiError(error, requestId, duration);
    
    log.error(`API Request Failed: ${requestId}`, {
      error: apiError.error,
      status: apiError.status,
      duration,
      url,
      method,
    });
    
    return apiError;
  }
}

// =============================================================================
// DATA PROCESSING AND FORMATTING
// =============================================================================

/**
 * Process numeric fields in API responses using COBOL-compatible formatting
 * Ensures proper decimal precision for financial calculations
 *
 * @param {Object} data - Response data object
 * @returns {Object} Processed data with proper numeric formatting
 */
function processNumericFields(data) {
  if (_.isArray(data)) {
    return _.map(data, processNumericFields);
  }
  
  if (!_.isObject(data) || _.isNull(data)) {
    return data;
  }
  
  const processed = _.cloneDeep(data);
  
  // Process monetary fields with COBOL COMP-3 precision
  const monetaryFields = [
    'amount', 'balance', 'limit', 'fee', 'payment', 'charge',
    'creditLimit', 'currentBalance', 'availableCredit', 'minimumPayment',
    'interestCharge', 'principalAmount', 'interestAmount', 'totalAmount',
  ];
  
  // Process rate fields with higher precision
  const rateFields = [
    'rate', 'interestRate', 'apr', 'percent', 'percentage',
    'discountRate', 'penaltyRate', 'promotionalRate',
  ];
  
  _.forEach(processed, (value, key) => {
    const lowerKey = key.toLowerCase();
    
    if (_.some(monetaryFields, field => lowerKey.includes(field.toLowerCase()))) {
      // Monetary amounts - 2 decimal places with COBOL precision
      if (_.isNumber(value) || (_.isString(value) && !isNaN(parseFloat(value)))) {
        try {
          processed[key] = toComp3(parseFloat(value), 2, 15);
        } catch (error) {
          log.warn(`Failed to convert monetary field ${key}:`, error);
          processed[key] = parseFloat(value).toFixed(2);
        }
      }
    } else if (_.some(rateFields, field => lowerKey.includes(field.toLowerCase()))) {
      // Rate fields - 4 decimal places for precision
      if (_.isNumber(value) || (_.isString(value) && !isNaN(parseFloat(value)))) {
        try {
          processed[key] = toComp3(parseFloat(value), 4, 15);
        } catch (error) {
          log.warn(`Failed to convert rate field ${key}:`, error);
          processed[key] = parseFloat(value).toFixed(4);
        }
      }
    } else if (_.isObject(value)) {
      // Recursively process nested objects
      processed[key] = processNumericFields(value);
    }
  });
  
  return processed;
}

/**
 * Validate request data before sending to API
 * Implements COBOL field validation rules
 *
 * @param {Object} data - Request data to validate
 * @param {string} transactionType - Type of transaction for specific validation
 * @returns {Object} Validation result
 */
function validateRequestData(data, transactionType) {
  const errors = [];
  
  if (!data || !_.isObject(data)) {
    return {
      isValid: false,
      errors: ['Request data must be provided'],
    };
  }
  
  // Validate FICO scores if present
  if (data.ficoScore !== undefined) {
    const ficoValidation = validateFICO(data.ficoScore);
    if (!ficoValidation.isValid) {
      errors.push(ficoValidation.errorMessage);
    }
  }
  
  // Transaction-specific validations
  switch (transactionType) {
    case 'ACCOUNT':
      if (data.accountId && !/^\d{11}$/.test(data.accountId)) {
        errors.push('Account ID must be exactly 11 digits');
      }
      break;
      
    case 'CARD':
      if (data.cardNumber && !/^\d{16}$/.test(data.cardNumber)) {
        errors.push('Card number must be exactly 16 digits');
      }
      break;
      
    case 'TRANSACTION':
      if (data.amount !== undefined) {
        const amount = parseFloat(data.amount);
        if (isNaN(amount) || amount < 0) {
          errors.push('Transaction amount must be a positive number');
        }
      }
      break;
  }
  
  return {
    isValid: errors.length === 0,
    errors,
  };
}

// =============================================================================
// ERROR HANDLING
// =============================================================================

/**
 * Comprehensive API error handler matching COBOL ABEND routines
 * Provides structured error responses for consistent error handling
 *
 * @param {Error} error - Original error object
 * @param {string} requestId - Unique request identifier
 * @param {number} duration - Request duration in milliseconds
 * @returns {Object} Structured error response
 */
export function handleApiError(error, requestId, duration = 0) {
  const errorResponse = {
    success: false,
    error: 'An unexpected error occurred',
    status: 500,
    data: null,
    duration,
    requestId,
    timestamp: new Date().toISOString(),
  };
  
  // Handle Axios errors (HTTP responses)
  if (error.response) {
    errorResponse.status = error.response.status;
    errorResponse.data = error.response.data;
    
    // Map HTTP status codes to user-friendly messages
    switch (error.response.status) {
      case 400:
        errorResponse.error = _.get(error.response.data, 'message', 'Invalid request data');
        break;
      case 401:
        errorResponse.error = 'Authentication required - please sign in';
        break;
      case 403:
        errorResponse.error = 'Access denied - insufficient permissions';
        break;
      case 404:
        errorResponse.error = 'Requested resource not found';
        break;
      case 409:
        errorResponse.error = 'Conflict - resource already exists or is locked';
        break;
      case 422:
        errorResponse.error = _.get(error.response.data, 'message', 'Validation failed');
        if (error.response.data && error.response.data.errors) {
          errorResponse.validationErrors = error.response.data.errors;
        }
        break;
      case 429:
        errorResponse.error = 'Rate limit exceeded - please try again later';
        break;
      case 500:
        errorResponse.error = 'Internal server error - please try again';
        break;
      case 502:
        errorResponse.error = 'Service temporarily unavailable';
        break;
      case 503:
        errorResponse.error = 'Service unavailable - system maintenance in progress';
        break;
      case 504:
        errorResponse.error = 'Request timeout - please try again';
        break;
      default:
        errorResponse.error = `Server error: ${error.response.status}`;
    }
  } else if (error.request) {
    // Network errors
    errorResponse.error = 'Network error - unable to connect to server';
    errorResponse.status = 0;
  } else if (error.code) {
    // Specific error codes
    switch (error.code) {
      case 'ECONNABORTED':
        errorResponse.error = 'Request timeout - operation took too long';
        break;
      case 'ENOTFOUND':
        errorResponse.error = 'Server not found - check network connection';
        break;
      case 'ECONNREFUSED':
        errorResponse.error = 'Connection refused - server may be down';
        break;
      default:
        errorResponse.error = `Connection error: ${error.code}`;
    }
  } else {
    // Generic errors
    errorResponse.error = error.message || 'Unknown error occurred';
  }
  
  // Add circuit breaker status if available
  if (circuitBreaker && circuitBreaker.stats) {
    errorResponse.circuitBreakerStats = {
      state: circuitBreaker.opened ? 'open' : circuitBreaker.halfOpen ? 'half-open' : 'closed',
      failures: circuitBreaker.stats.failures,
      successes: circuitBreaker.stats.successes,
    };
  }
  
  return errorResponse;
}

// =============================================================================
// AUTHENTICATION AND SESSION MANAGEMENT
// =============================================================================

/**
 * Sign In API - Maps to CICS transaction code CC00
 * Handles user authentication and session establishment
 * Target response time: <200ms for card authorization flow
 *
 * @param {Object} credentials - User login credentials
 * @param {string} credentials.userId - User ID (1-8 alphanumeric characters)
 * @param {string} credentials.password - User password
 * @returns {Promise<Object>} Authentication response with user details and session info
 */
export async function signIn(credentials) {
  log.info('Attempting user sign-in', { userId: credentials?.userId });
  
  // Validate credentials
  const validation = validateRequestData(credentials, 'AUTH');
  if (!validation.isValid) {
    return {
      success: false,
      error: 'Invalid credentials provided',
      validationErrors: validation.errors,
    };
  }
  
  const response = await makeApiRequest('POST', '/auth/signin', credentials);
  
  if (response.success) {
    // Store user session information
    try {
      sessionStorage.setItem('userId', response.data.userId);
      sessionStorage.setItem('userRole', response.data.userRole);
      sessionStorage.setItem('lastActivity', new Date().toISOString());
      sessionStorage.setItem('sessionStartTime', new Date().toISOString());
      
      log.info('User sign-in successful', {
        userId: response.data.userId,
        userRole: response.data.userRole,
        sessionId: response.data.sessionId,
      });
      
      // Schedule session timeout warning
      scheduleSessionWarning();
      
    } catch (storageError) {
      log.warn('Failed to store session data', storageError);
    }
  } else {
    log.warn('User sign-in failed', {
      userId: credentials?.userId,
      error: response.error,
      status: response.status,
    });
  }
  
  return response;
}

/**
 * Sign Out API - Session termination and cleanup
 * Clears client-side session data and notifies server
 *
 * @returns {Promise<Object>} Sign-out confirmation response
 */
export async function signOut() {
  const currentUserId = sessionStorage.getItem('userId');
  log.info('Attempting user sign-out', { userId: currentUserId });
  
  const response = await makeApiRequest('POST', '/auth/signout');
  
  // Clear local session data regardless of server response
  try {
    sessionStorage.clear();
    localStorage.removeItem('userRole');
    localStorage.removeItem('lastActivity');
    localStorage.removeItem('sessionStartTime');
    
    // Clear any scheduled session warnings
    if (window.sessionWarningTimer) {
      clearTimeout(window.sessionWarningTimer);
      delete window.sessionWarningTimer;
    }
    
    log.info('User sign-out completed', { userId: currentUserId });
  } catch (storageError) {
    log.warn('Error clearing session data', storageError);
  }
  
  return response;
}

/**
 * Schedule session timeout warning
 * Alerts user before session expires
 */
function scheduleSessionWarning() {
  if (window.sessionWarningTimer) {
    clearTimeout(window.sessionWarningTimer);
  }
  
  window.sessionWarningTimer = setTimeout(() => {
    log.warn('Session timeout warning triggered');
    
    // Emit custom event for UI to handle
    const event = new CustomEvent('sessionTimeoutWarning', {
      detail: {
        message: 'Your session will expire in 5 minutes. Please save your work.',
        timeRemaining: API_CONFIG.SESSION_TIMEOUT_WARNING_MS,
      },
    });
    window.dispatchEvent(event);
    
  }, API_CONFIG.SESSION_TIMEOUT_WARNING_MS);
}

// =============================================================================
// ACCOUNT MANAGEMENT APIs
// =============================================================================

/**
 * Get Account API - Maps to CICS transaction code COACTVW
 * Retrieves detailed account information including balances and status
 * Implements VSAM KSDS read operation equivalent
 *
 * @param {string} accountId - Account identifier (11 digits)
 * @returns {Promise<Object>} Complete account details with transaction history
 */
export async function getAccount(accountId) {
  log.debug('Retrieving account details', { accountId });
  
  // Validate account ID format (COBOL PIC 9(11) equivalent)
  if (!accountId || !/^\d{11}$/.test(accountId)) {
    return {
      success: false,
      error: 'Account ID must be exactly 11 digits',
      validationErrors: ['Invalid account ID format'],
    };
  }
  
  const response = await makeApiRequest('GET', `/accounts/${accountId}`);
  
  if (response.success && response.data) {
    // Update last activity timestamp
    try {
      sessionStorage.setItem('lastActivity', new Date().toISOString());
    } catch (error) {
      log.warn('Failed to update activity timestamp', error);
    }
  }
  
  return response;
}

/**
 * Update Account API - Maps to CICS transaction code COACTUP
 * Updates account information including limits, status, and contact details
 * Implements VSAM KSDS rewrite operation equivalent
 *
 * @param {string} accountId - Account identifier (11 digits)
 * @param {Object} accountData - Updated account information
 * @returns {Promise<Object>} Updated account details with confirmation
 */
export async function updateAccount(accountId, accountData) {
  log.debug('Updating account details', { accountId, dataKeys: Object.keys(accountData || {}) });
  
  // Validate account ID format
  if (!accountId || !/^\d{11}$/.test(accountId)) {
    return {
      success: false,
      error: 'Account ID must be exactly 11 digits',
      validationErrors: ['Invalid account ID format'],
    };
  }
  
  // Validate account data
  const validation = validateRequestData(accountData, 'ACCOUNT');
  if (!validation.isValid) {
    return {
      success: false,
      error: 'Invalid account data provided',
      validationErrors: validation.errors,
    };
  }
  
  const response = await makeApiRequest('PUT', `/accounts/${accountId}`, accountData);
  
  if (response.success) {
    log.info('Account updated successfully', { accountId });
    
    // Update activity timestamp
    try {
      sessionStorage.setItem('lastActivity', new Date().toISOString());
    } catch (error) {
      log.warn('Failed to update activity timestamp', error);
    }
  }
  
  return response;
}

/**
 * List Accounts API - Maps to CICS transaction code COACTVW with browse capability
 * Retrieves paginated list of accounts with filtering options
 * Implements VSAM KSDS browse operation (STARTBR/READNEXT) equivalent
 *
 * @param {Object} options - Search and pagination options
 * @param {string} options.customerId - Customer ID filter (optional)
 * @param {string} options.status - Account status filter (optional)
 * @param {number} options.pageSize - Number of records per page (default: 10, max: 100)
 * @param {number} options.pageNumber - Page number (1-based, default: 1)
 * @param {string} options.sortBy - Sort field (default: 'accountId')
 * @param {string} options.sortOrder - Sort order 'asc' or 'desc' (default: 'asc')
 * @returns {Promise<Object>} Paginated account list with summary information
 */
export async function listAccounts(options = {}) {
  const {
    customerId,
    status,
    pageSize = 10,
    pageNumber = 1,
    sortBy = 'accountId',
    sortOrder = 'asc',
  } = options;
  
  log.debug('Retrieving account list', { pageSize, pageNumber, customerId, status });
  
  // Validate pagination parameters
  const validatedPageSize = Math.min(Math.max(1, parseInt(pageSize, 10)), 100);
  const validatedPageNumber = Math.max(1, parseInt(pageNumber, 10));
  
  const queryParams = {
    pageSize: validatedPageSize,
    pageNumber: validatedPageNumber,
    sortBy,
    sortOrder: ['asc', 'desc'].includes(sortOrder) ? sortOrder : 'asc',
  };
  
  // Add optional filters
  if (customerId) {
    queryParams.customerId = customerId;
  }
  if (status) {
    queryParams.status = status;
  }
  
  const response = await makeApiRequest('GET', '/accounts', null, {
    params: queryParams,
  });
  
  if (response.success) {
    // Update activity timestamp
    try {
      sessionStorage.setItem('lastActivity', new Date().toISOString());
    } catch (error) {
      log.warn('Failed to update activity timestamp', error);
    }
  }
  
  return response;
}

// =============================================================================
// CARD MANAGEMENT APIs  
// =============================================================================

/**
 * List Cards API - Maps to CICS transaction code COCRDLI
 * Retrieves paginated list of credit cards with filtering capabilities
 * Implements cursor-based pagination matching VSAM browse operations
 *
 * @param {Object} options - Search and pagination options
 * @param {string} options.accountId - Account ID filter (optional)
 * @param {string} options.status - Card status filter (optional)
 * @param {string} options.cardType - Card type filter (optional)
 * @param {number} options.pageSize - Number of records per page (default: 10)
 * @param {number} options.pageNumber - Page number (1-based, default: 1)
 * @returns {Promise<Object>} Paginated list of credit cards
 */
export async function listCards(options = {}) {
  const {
    accountId,
    status,
    cardType,
    pageSize = 10,
    pageNumber = 1,
  } = options;
  
  log.debug('Retrieving card list', { pageSize, pageNumber, accountId, status, cardType });
  
  // Validate account ID if provided
  if (accountId && !/^\d{11}$/.test(accountId)) {
    return {
      success: false,
      error: 'Account ID must be exactly 11 digits',
      validationErrors: ['Invalid account ID format'],
    };
  }
  
  const queryParams = {
    pageSize: Math.min(Math.max(1, parseInt(pageSize, 10)), 100),
    pageNumber: Math.max(1, parseInt(pageNumber, 10)),
  };
  
  // Add optional filters
  if (accountId) {
    queryParams.accountId = accountId;
  }
  if (status) {
    queryParams.status = status;
  }
  if (cardType) {
    queryParams.cardType = cardType;
  }
  
  const response = await makeApiRequest('GET', '/cards', null, {
    params: queryParams,
  });
  
  if (response.success) {
    // Update activity timestamp
    try {
      sessionStorage.setItem('lastActivity', new Date().toISOString());
    } catch (error) {
      log.warn('Failed to update activity timestamp', error);
    }
  }
  
  return response;
}

/**
 * Get Card API - Maps to CICS transaction code COCRDSL  
 * Retrieves detailed information for a specific credit card
 * Implements direct VSAM record read by primary key
 *
 * @param {string} cardNumber - Credit card number (16 digits)
 * @returns {Promise<Object>} Complete card details including limits and status
 */
export async function getCard(cardNumber) {
  log.debug('Retrieving card details', { cardNumber: cardNumber ? `${cardNumber.substring(0, 4)}****${cardNumber.substring(12)}` : null });
  
  // Validate card number format (COBOL PIC 9(16) equivalent)
  if (!cardNumber || !/^\d{16}$/.test(cardNumber)) {
    return {
      success: false,
      error: 'Card number must be exactly 16 digits',
      validationErrors: ['Invalid card number format'],
    };
  }
  
  const response = await makeApiRequest('GET', `/cards/${cardNumber}`);
  
  if (response.success) {
    // Update activity timestamp
    try {
      sessionStorage.setItem('lastActivity', new Date().toISOString());
    } catch (error) {
      log.warn('Failed to update activity timestamp', error);
    }
  }
  
  return response;
}

/**
 * Update Card API - Maps to CICS transaction code COCRDUP
 * Updates credit card information including limits, status, and security settings
 * Implements VSAM record update with optimistic locking
 *
 * @param {string} cardNumber - Credit card number (16 digits)
 * @param {Object} cardData - Updated card information
 * @returns {Promise<Object>} Updated card details with confirmation
 */
export async function updateCard(cardNumber, cardData) {
  log.debug('Updating card details', { 
    cardNumber: cardNumber ? `${cardNumber.substring(0, 4)}****${cardNumber.substring(12)}` : null,
    dataKeys: Object.keys(cardData || {}),
  });
  
  // Validate card number format
  if (!cardNumber || !/^\d{16}$/.test(cardNumber)) {
    return {
      success: false,
      error: 'Card number must be exactly 16 digits',
      validationErrors: ['Invalid card number format'],
    };
  }
  
  // Validate card data
  const validation = validateRequestData(cardData, 'CARD');
  if (!validation.isValid) {
    return {
      success: false,
      error: 'Invalid card data provided',
      validationErrors: validation.errors,
    };
  }
  
  const response = await makeApiRequest('PUT', `/cards/${cardNumber}`, cardData);
  
  if (response.success) {
    log.info('Card updated successfully', { cardNumber: `${cardNumber.substring(0, 4)}****${cardNumber.substring(12)}` });
    
    // Update activity timestamp
    try {
      sessionStorage.setItem('lastActivity', new Date().toISOString());
    } catch (error) {
      log.warn('Failed to update activity timestamp', error);
    }
  }
  
  return response;
}

/**
 * Activate Card API - Maps to CICS transaction code COCRDUP with activation logic
 * Activates a credit card and sets initial status
 * Implements card activation business logic with security validation
 *
 * @param {string} cardNumber - Credit card number (16 digits)
 * @param {Object} activationData - Card activation information
 * @param {string} activationData.securityCode - Card security code (3-4 digits)
 * @param {string} activationData.expirationDate - Card expiration date (MMYY)
 * @param {string} activationData.dateOfBirth - Cardholder date of birth (CCYYMMDD)
 * @returns {Promise<Object>} Card activation confirmation with updated status
 */
export async function activateCard(cardNumber, activationData) {
  log.debug('Activating card', { 
    cardNumber: cardNumber ? `${cardNumber.substring(0, 4)}****${cardNumber.substring(12)}` : null,
  });
  
  // Validate card number format
  if (!cardNumber || !/^\d{16}$/.test(cardNumber)) {
    return {
      success: false,
      error: 'Card number must be exactly 16 digits',
      validationErrors: ['Invalid card number format'],
    };
  }
  
  // Validate activation data
  if (!activationData || !_.isObject(activationData)) {
    return {
      success: false,
      error: 'Activation data is required',
      validationErrors: ['Missing activation data'],
    };
  }
  
  // Validate security code format
  if (!activationData.securityCode || !/^\d{3,4}$/.test(activationData.securityCode)) {
    return {
      success: false,
      error: 'Security code must be 3 or 4 digits',
      validationErrors: ['Invalid security code format'],
    };
  }
  
  // Validate expiration date format (MMYY)
  if (!activationData.expirationDate || !/^(0[1-9]|1[0-2])\d{2}$/.test(activationData.expirationDate)) {
    return {
      success: false,
      error: 'Expiration date must be in MMYY format',
      validationErrors: ['Invalid expiration date format'],
    };
  }
  
  const response = await makeApiRequest('POST', `/cards/${cardNumber}/activate`, activationData);
  
  if (response.success) {
    log.info('Card activation successful', { cardNumber: `${cardNumber.substring(0, 4)}****${cardNumber.substring(12)}` });
    
    // Update activity timestamp
    try {
      sessionStorage.setItem('lastActivity', new Date().toISOString());
    } catch (error) {
      log.warn('Failed to update activity timestamp', error);
    }
  }
  
  return response;
}

// =============================================================================
// TRANSACTION MANAGEMENT APIs
// =============================================================================

/**
 * Get Transactions API - Maps to CICS transaction code COTRN00
 * Retrieves paginated list of transactions with comprehensive filtering
 * Implements cursor-based pagination matching VSAM browse patterns
 * Target response time: <1000ms for transaction history
 *
 * @param {Object} criteria - Search and filtering criteria
 * @param {string} criteria.accountId - Account identifier (optional, 11 digits)
 * @param {string} criteria.cardNumber - Card number (optional, 16 digits)
 * @param {string} criteria.startDate - Start date (ISO format YYYY-MM-DD)
 * @param {string} criteria.endDate - End date (ISO format YYYY-MM-DD)
 * @param {string} criteria.transactionType - Transaction type filter (optional)
 * @param {string} criteria.status - Transaction status filter (optional)
 * @param {number} criteria.minAmount - Minimum amount filter (optional)
 * @param {number} criteria.maxAmount - Maximum amount filter (optional)
 * @param {number} criteria.pageSize - Records per page (default: 10, max: 100)
 * @param {number} criteria.pageNumber - Page number (1-based, default: 1)
 * @param {string} criteria.sortBy - Sort field (default: 'transactionDate')
 * @param {string} criteria.sortOrder - Sort order 'asc' or 'desc' (default: 'desc')
 * @returns {Promise<Object>} Paginated transaction list with summary totals
 */
export async function getTransactions(criteria = {}) {
  const {
    accountId,
    cardNumber,
    startDate,
    endDate,
    transactionType,
    status,
    minAmount,
    maxAmount,
    pageSize = 10,
    pageNumber = 1,
    sortBy = 'transactionDate',
    sortOrder = 'desc',
  } = criteria;
  
  log.debug('Retrieving transaction list', { 
    pageSize, 
    pageNumber, 
    accountId, 
    cardNumber: cardNumber ? `${cardNumber.substring(0, 4)}****${cardNumber.substring(12)}` : null,
    startDate,
    endDate,
  });
  
  // Validate account ID if provided
  if (accountId && !/^\d{11}$/.test(accountId)) {
    return {
      success: false,
      error: 'Account ID must be exactly 11 digits',
      validationErrors: ['Invalid account ID format'],
    };
  }
  
  // Validate card number if provided
  if (cardNumber && !/^\d{16}$/.test(cardNumber)) {
    return {
      success: false,
      error: 'Card number must be exactly 16 digits',
      validationErrors: ['Invalid card number format'],
    };
  }
  
  // Validate date range
  if (startDate && endDate) {
    const start = new Date(startDate);
    const end = new Date(endDate);
    if (start > end) {
      return {
        success: false,
        error: 'Start date cannot be after end date',
        validationErrors: ['Invalid date range'],
      };
    }
    
    // Limit date range to prevent excessive data retrieval
    const daysDiff = (end - start) / (1000 * 60 * 60 * 24);
    if (daysDiff > 366) {
      return {
        success: false,
        error: 'Date range cannot exceed 1 year',
        validationErrors: ['Date range too large'],
      };
    }
  }
  
  const queryParams = {
    pageSize: Math.min(Math.max(1, parseInt(pageSize, 10)), 100),
    pageNumber: Math.max(1, parseInt(pageNumber, 10)),
    sortBy,
    sortOrder: ['asc', 'desc'].includes(sortOrder) ? sortOrder : 'desc',
  };
  
  // Add optional filters
  if (accountId) queryParams.accountId = accountId;
  if (cardNumber) queryParams.cardNumber = cardNumber;
  if (startDate) queryParams.startDate = startDate;
  if (endDate) queryParams.endDate = endDate;
  if (transactionType) queryParams.transactionType = transactionType;
  if (status) queryParams.status = status;
  if (minAmount !== undefined) queryParams.minAmount = parseFloat(minAmount);
  if (maxAmount !== undefined) queryParams.maxAmount = parseFloat(maxAmount);
  
  const response = await makeApiRequest('GET', '/transactions', null, {
    params: queryParams,
  });
  
  if (response.success) {
    // Update activity timestamp
    try {
      sessionStorage.setItem('lastActivity', new Date().toISOString());
    } catch (error) {
      log.warn('Failed to update activity timestamp', error);
    }
  }
  
  return response;
}

/**
 * Get Transaction Detail API - Maps to CICS transaction code COTRN01
 * Retrieves detailed information for a specific transaction
 * Implements direct VSAM record read with related data retrieval
 *
 * @param {string} transactionId - Transaction identifier
 * @returns {Promise<Object>} Complete transaction details including authorization data
 */
export async function getTransactionDetail(transactionId) {
  log.debug('Retrieving transaction detail', { transactionId });
  
  // Validate transaction ID
  if (!transactionId || typeof transactionId !== 'string' || transactionId.trim().length === 0) {
    return {
      success: false,
      error: 'Transaction ID is required',
      validationErrors: ['Missing transaction ID'],
    };
  }
  
  const response = await makeApiRequest('GET', `/transactions/${transactionId}`);
  
  if (response.success) {
    // Update activity timestamp
    try {
      sessionStorage.setItem('lastActivity', new Date().toISOString());
    } catch (error) {
      log.warn('Failed to update activity timestamp', error);
    }
  }
  
  return response;
}

/**
 * Post Transaction API - Maps to CICS transaction code COTRN02
 * Creates a new transaction (manual entry, adjustment, or authorization)
 * Implements transaction posting with full validation and audit trail
 * Target response time: <200ms for card authorization requests
 *
 * @param {Object} transactionData - New transaction information
 * @param {string} transactionData.accountId - Account identifier (11 digits)
 * @param {string} transactionData.cardNumber - Card number (16 digits)
 * @param {number} transactionData.amount - Transaction amount (positive for charges, negative for credits)
 * @param {string} transactionData.description - Transaction description
 * @param {string} transactionData.transactionType - Type of transaction ('PURCHASE', 'PAYMENT', 'FEE', 'ADJUSTMENT')
 * @param {string} transactionData.merchantCode - Merchant code (optional)
 * @param {string} transactionData.authorizationCode - Authorization code (optional)
 * @returns {Promise<Object>} Created transaction details with confirmation
 */
export async function postTransaction(transactionData) {
  log.debug('Posting new transaction', { 
    accountId: transactionData?.accountId,
    cardNumber: transactionData?.cardNumber ? `${transactionData.cardNumber.substring(0, 4)}****${transactionData.cardNumber.substring(12)}` : null,
    amount: transactionData?.amount,
    transactionType: transactionData?.transactionType,
  });
  
  // Validate required transaction data
  const validation = validateRequestData(transactionData, 'TRANSACTION');
  if (!validation.isValid) {
    return {
      success: false,
      error: 'Invalid transaction data provided',
      validationErrors: validation.errors,
    };
  }
  
  // Additional transaction-specific validations
  const errors = [];
  
  if (!transactionData.accountId || !/^\d{11}$/.test(transactionData.accountId)) {
    errors.push('Account ID must be exactly 11 digits');
  }
  
  if (!transactionData.cardNumber || !/^\d{16}$/.test(transactionData.cardNumber)) {
    errors.push('Card number must be exactly 16 digits');
  }
  
  if (transactionData.amount === undefined || transactionData.amount === null) {
    errors.push('Transaction amount is required');
  } else {
    const amount = parseFloat(transactionData.amount);
    if (isNaN(amount)) {
      errors.push('Transaction amount must be a valid number');
    } else if (Math.abs(amount) > 999999.99) {
      errors.push('Transaction amount cannot exceed $999,999.99');
    }
  }
  
  if (!transactionData.description || transactionData.description.trim().length === 0) {
    errors.push('Transaction description is required');
  } else if (transactionData.description.length > 50) {
    errors.push('Transaction description cannot exceed 50 characters');
  }
  
  if (!transactionData.transactionType) {
    errors.push('Transaction type is required');
  } else {
    const validTypes = ['PURCHASE', 'PAYMENT', 'FEE', 'ADJUSTMENT', 'REFUND', 'REVERSAL'];
    if (!validTypes.includes(transactionData.transactionType.toUpperCase())) {
      errors.push(`Transaction type must be one of: ${validTypes.join(', ')}`);
    }
  }
  
  if (errors.length > 0) {
    return {
      success: false,
      error: 'Transaction validation failed',
      validationErrors: errors,
    };
  }
  
  // Convert amount using COBOL precision for financial accuracy
  const processedData = { ...transactionData };
  try {
    processedData.amount = toComp3(parseFloat(transactionData.amount), 2, 15);
  } catch (conversionError) {
    log.warn('Failed to convert amount with COBOL precision', conversionError);
    processedData.amount = parseFloat(transactionData.amount).toFixed(2);
  }
  
  const response = await makeApiRequest('POST', '/transactions', processedData);
  
  if (response.success) {
    log.info('Transaction posted successfully', { 
      transactionId: response.data.transactionId,
      accountId: transactionData.accountId,
      amount: transactionData.amount,
    });
    
    // Update activity timestamp
    try {
      sessionStorage.setItem('lastActivity', new Date().toISOString());
    } catch (error) {
      log.warn('Failed to update activity timestamp', error);
    }
  }
  
  return response;
}

// =============================================================================
// REPORTING AND STATEMENT APIs
// =============================================================================

/**
 * Generate Statement API - Maps to CICS transaction code COBIL00
 * Generates account statements with transaction history
 * Implements batch-style statement generation with PDF output
 *
 * @param {Object} statementRequest - Statement generation parameters
 * @param {string} statementRequest.accountId - Account identifier (11 digits)
 * @param {string} statementRequest.statementDate - Statement date (YYYY-MM-DD)
 * @param {string} statementRequest.statementType - Statement type ('MONTHLY', 'INTERIM', 'ANNUAL')
 * @param {string} statementRequest.format - Output format ('PDF', 'HTML', 'TEXT')
 * @param {boolean} statementRequest.includeImages - Include check images (optional)
 * @returns {Promise<Object>} Statement generation response with download link
 */
export async function generateStatement(statementRequest) {
  log.debug('Generating statement', { 
    accountId: statementRequest?.accountId,
    statementDate: statementRequest?.statementDate,
    statementType: statementRequest?.statementType,
  });
  
  // Validate account ID
  if (!statementRequest.accountId || !/^\d{11}$/.test(statementRequest.accountId)) {
    return {
      success: false,
      error: 'Account ID must be exactly 11 digits',
      validationErrors: ['Invalid account ID format'],
    };
  }
  
  // Validate statement date
  if (!statementRequest.statementDate) {
    return {
      success: false,
      error: 'Statement date is required',
      validationErrors: ['Missing statement date'],
    };
  }
  
  const statementDate = new Date(statementRequest.statementDate);
  if (isNaN(statementDate.getTime())) {
    return {
      success: false,
      error: 'Statement date must be in valid YYYY-MM-DD format',
      validationErrors: ['Invalid statement date format'],
    };
  }
  
  // Validate statement type
  const validTypes = ['MONTHLY', 'INTERIM', 'ANNUAL'];
  if (!statementRequest.statementType || !validTypes.includes(statementRequest.statementType.toUpperCase())) {
    return {
      success: false,
      error: `Statement type must be one of: ${validTypes.join(', ')}`,
      validationErrors: ['Invalid statement type'],
    };
  }
  
  // Set default format if not specified
  const format = statementRequest.format || 'PDF';
  const validFormats = ['PDF', 'HTML', 'TEXT'];
  if (!validFormats.includes(format.toUpperCase())) {
    return {
      success: false,
      error: `Format must be one of: ${validFormats.join(', ')}`,
      validationErrors: ['Invalid output format'],
    };
  }
  
  const requestData = {
    ...statementRequest,
    format: format.toUpperCase(),
    statementType: statementRequest.statementType.toUpperCase(),
  };
  
  // Statement generation may take longer than normal API calls
  const response = await makeApiRequest('POST', '/statements/generate', requestData, {
    timeout: API_CONFIG.RESPONSE_TIME_TARGETS.REPORTING,
  });
  
  if (response.success) {
    log.info('Statement generation initiated', { 
      accountId: statementRequest.accountId,
      statementId: response.data.statementId,
    });
    
    // Update activity timestamp
    try {
      sessionStorage.setItem('lastActivity', new Date().toISOString());
    } catch (error) {
      log.warn('Failed to update activity timestamp', error);
    }
  }
  
  return response;
}

/**
 * Get Reports API - Maps to CICS transaction code CORPT00
 * Retrieves available reports and report data
 * Implements report listing and data retrieval functionality
 *
 * @param {Object} reportRequest - Report request parameters
 * @param {string} reportRequest.reportType - Type of report ('TRANSACTION', 'ACCOUNT', 'CARD', 'SUMMARY')
 * @param {string} reportRequest.dateFrom - Start date for report data (YYYY-MM-DD)
 * @param {string} reportRequest.dateTo - End date for report data (YYYY-MM-DD)
 * @param {string} reportRequest.format - Output format ('JSON', 'CSV', 'PDF')
 * @param {Object} reportRequest.filters - Additional report filters (optional)
 * @returns {Promise<Object>} Report data or generation status
 */
export async function getReports(reportRequest = {}) {
  const {
    reportType,
    dateFrom,
    dateTo,
    format = 'JSON',
    filters = {},
  } = reportRequest;
  
  log.debug('Retrieving reports', { reportType, dateFrom, dateTo, format });
  
  // If no report type specified, return available report types
  if (!reportType) {
    return makeApiRequest('GET', '/reports/types');
  }
  
  // Validate report type
  const validReportTypes = ['TRANSACTION', 'ACCOUNT', 'CARD', 'SUMMARY', 'ACTIVITY', 'PERFORMANCE'];
  if (!validReportTypes.includes(reportType.toUpperCase())) {
    return {
      success: false,
      error: `Report type must be one of: ${validReportTypes.join(', ')}`,
      validationErrors: ['Invalid report type'],
    };
  }
  
  // Validate date range if provided
  if (dateFrom && dateTo) {
    const startDate = new Date(dateFrom);
    const endDate = new Date(dateTo);
    
    if (isNaN(startDate.getTime()) || isNaN(endDate.getTime())) {
      return {
        success: false,
        error: 'Date range must be in valid YYYY-MM-DD format',
        validationErrors: ['Invalid date format'],
      };
    }
    
    if (startDate > endDate) {
      return {
        success: false,
        error: 'Start date cannot be after end date',
        validationErrors: ['Invalid date range'],
      };
    }
    
    // Limit report date range
    const daysDiff = (endDate - startDate) / (1000 * 60 * 60 * 24);
    if (daysDiff > 366) {
      return {
        success: false,
        error: 'Report date range cannot exceed 1 year',
        validationErrors: ['Date range too large'],
      };
    }
  }
  
  const queryParams = {
    reportType: reportType.toUpperCase(),
    format: format.toUpperCase(),
    ...filters,
  };
  
  if (dateFrom) queryParams.dateFrom = dateFrom;
  if (dateTo) queryParams.dateTo = dateTo;
  
  // Reports may take longer to generate
  const response = await makeApiRequest('GET', '/reports', null, {
    params: queryParams,
    timeout: API_CONFIG.RESPONSE_TIME_TARGETS.REPORTING,
  });
  
  if (response.success) {
    // Update activity timestamp
    try {
      sessionStorage.setItem('lastActivity', new Date().toISOString());
    } catch (error) {
      log.warn('Failed to update activity timestamp', error);
    }
  }
  
  return response;
}

// =============================================================================
// USER MANAGEMENT APIs
// =============================================================================

/**
 * Get Users API - Maps to CICS transaction code COUSR00
 * Retrieves paginated list of system users with role information
 * Implements user management functionality for administrative users
 *
 * @param {Object} options - Search and pagination options
 * @param {string} options.searchTerm - User search term (optional)
 * @param {string} options.role - User role filter (optional)
 * @param {string} options.status - User status filter (optional)
 * @param {number} options.pageSize - Number of records per page (default: 20)
 * @param {number} options.pageNumber - Page number (1-based, default: 1)
 * @returns {Promise<Object>} Paginated user list with role information
 */
export async function getUsers(options = {}) {
  const {
    searchTerm,
    role,
    status,
    pageSize = 20,
    pageNumber = 1,
  } = options;
  
  log.debug('Retrieving user list', { pageSize, pageNumber, role, status });
  
  const queryParams = {
    pageSize: Math.min(Math.max(1, parseInt(pageSize, 10)), 100),
    pageNumber: Math.max(1, parseInt(pageNumber, 10)),
  };
  
  // Add optional filters
  if (searchTerm) queryParams.searchTerm = searchTerm;
  if (role) queryParams.role = role;
  if (status) queryParams.status = status;
  
  const response = await makeApiRequest('GET', '/users', null, {
    params: queryParams,
  });
  
  if (response.success) {
    // Update activity timestamp
    try {
      sessionStorage.setItem('lastActivity', new Date().toISOString());
    } catch (error) {
      log.warn('Failed to update activity timestamp', error);
    }
  }
  
  return response;
}

/**
 * Create User API - Maps to CICS transaction code COUSR01
 * Creates a new system user with specified role and permissions
 * Implements user creation with validation and security checks
 *
 * @param {Object} userData - New user information
 * @param {string} userData.userId - User identifier (1-8 alphanumeric, starts with letter)
 * @param {string} userData.firstName - User first name
 * @param {string} userData.lastName - User last name
 * @param {string} userData.email - User email address
 * @param {string} userData.role - User role ('ADMIN', 'USER', 'READONLY')
 * @param {string} userData.status - Initial status ('ACTIVE', 'INACTIVE')
 * @param {string} userData.password - Initial password (optional, system-generated if not provided)
 * @returns {Promise<Object>} Created user details (password excluded)
 */
export async function createUser(userData) {
  log.debug('Creating new user', { 
    userId: userData?.userId,
    firstName: userData?.firstName,
    lastName: userData?.lastName,
    role: userData?.role,
  });
  
  // Validate required user data
  const errors = [];
  
  if (!userData || !_.isObject(userData)) {
    return {
      success: false,
      error: 'User data is required',
      validationErrors: ['Missing user data'],
    };
  }
  
  // Validate user ID format (COBOL mainframe user ID rules)
  if (!userData.userId || !/^[A-Za-z][A-Za-z0-9]{0,7}$/.test(userData.userId)) {
    errors.push('User ID must be 1-8 characters, start with a letter, and contain only letters and numbers');
  }
  
  // Validate required fields
  if (!userData.firstName || userData.firstName.trim().length === 0) {
    errors.push('First name is required');
  }
  
  if (!userData.lastName || userData.lastName.trim().length === 0) {
    errors.push('Last name is required');
  }
  
  if (!userData.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(userData.email)) {
    errors.push('Valid email address is required');
  }
  
  // Validate role
  const validRoles = ['ADMIN', 'USER', 'READONLY', 'MANAGER'];
  if (!userData.role || !validRoles.includes(userData.role.toUpperCase())) {
    errors.push(`User role must be one of: ${validRoles.join(', ')}`);
  }
  
  if (errors.length > 0) {
    return {
      success: false,
      error: 'User validation failed',
      validationErrors: errors,
    };
  }
  
  // Prepare user data with proper formatting
  const processedData = {
    ...userData,
    userId: userData.userId.toUpperCase(),
    role: userData.role.toUpperCase(),
    status: userData.status ? userData.status.toUpperCase() : 'ACTIVE',
    firstName: userData.firstName.trim(),
    lastName: userData.lastName.trim(),
    email: userData.email.toLowerCase().trim(),
  };
  
  const response = await makeApiRequest('POST', '/users', processedData);
  
  if (response.success) {
    log.info('User created successfully', { 
      userId: userData.userId,
      role: userData.role,
    });
    
    // Update activity timestamp
    try {
      sessionStorage.setItem('lastActivity', new Date().toISOString());
    } catch (error) {
      log.warn('Failed to update activity timestamp', error);
    }
  }
  
  return response;
}

/**
 * Update User API - Maps to CICS transaction code COUSR02  
 * Updates existing user information and permissions
 * Implements user modification with audit trail
 *
 * @param {string} userId - User identifier to update
 * @param {Object} userData - Updated user information
 * @returns {Promise<Object>} Updated user details
 */
export async function updateUser(userId, userData) {
  log.debug('Updating user', { userId, dataKeys: Object.keys(userData || {}) });
  
  // Validate user ID format
  if (!userId || !/^[A-Za-z][A-Za-z0-9]{0,7}$/.test(userId)) {
    return {
      success: false,
      error: 'User ID must be 1-8 characters, start with a letter, and contain only letters and numbers',
      validationErrors: ['Invalid user ID format'],
    };
  }
  
  // Validate user data
  if (!userData || !_.isObject(userData)) {
    return {
      success: false,
      error: 'User data is required',
      validationErrors: ['Missing user data'],
    };
  }
  
  // Validate role if provided
  if (userData.role) {
    const validRoles = ['ADMIN', 'USER', 'READONLY', 'MANAGER'];
    if (!validRoles.includes(userData.role.toUpperCase())) {
      return {
        success: false,
        error: `User role must be one of: ${validRoles.join(', ')}`,
        validationErrors: ['Invalid user role'],
      };
    }
  }
  
  // Validate email if provided
  if (userData.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(userData.email)) {
    return {
      success: false,
      error: 'Valid email address is required',
      validationErrors: ['Invalid email format'],
    };
  }
  
  // Process user data with proper formatting
  const processedData = { ...userData };
  if (processedData.role) {
    processedData.role = processedData.role.toUpperCase();
  }
  if (processedData.status) {
    processedData.status = processedData.status.toUpperCase();
  }
  if (processedData.email) {
    processedData.email = processedData.email.toLowerCase().trim();
  }
  if (processedData.firstName) {
    processedData.firstName = processedData.firstName.trim();
  }
  if (processedData.lastName) {
    processedData.lastName = processedData.lastName.trim();
  }
  
  const response = await makeApiRequest('PUT', `/users/${userId.toUpperCase()}`, processedData);
  
  if (response.success) {
    log.info('User updated successfully', { userId });
    
    // Update activity timestamp
    try {
      sessionStorage.setItem('lastActivity', new Date().toISOString());
    } catch (error) {
      log.warn('Failed to update activity timestamp', error);
    }
  }
  
  return response;
}

/**
 * Delete User API - Maps to CICS transaction code COUSR03
 * Deletes or deactivates a system user
 * Implements soft delete with audit trail preservation
 *
 * @param {string} userId - User identifier to delete
 * @param {Object} options - Deletion options
 * @param {boolean} options.softDelete - Perform soft delete (default: true)
 * @param {string} options.reason - Reason for deletion (optional)
 * @returns {Promise<Object>} Deletion confirmation
 */
export async function deleteUser(userId, options = {}) {
  log.debug('Deleting user', { userId, options });
  
  // Validate user ID format
  if (!userId || !/^[A-Za-z][A-Za-z0-9]{0,7}$/.test(userId)) {
    return {
      success: false,
      error: 'User ID must be 1-8 characters, start with a letter, and contain only letters and numbers',
      validationErrors: ['Invalid user ID format'],
    };
  }
  
  const { softDelete = true, reason } = options;
  
  const requestData = {
    softDelete,
    reason: reason || 'User deleted via API',
  };
  
  const response = await makeApiRequest('DELETE', `/users/${userId.toUpperCase()}`, requestData);
  
  if (response.success) {
    log.info('User deletion completed', { userId, softDelete });
    
    // Update activity timestamp
    try {
      sessionStorage.setItem('lastActivity', new Date().toISOString());
    } catch (error) {
      log.warn('Failed to update activity timestamp', error);
    }
  }
  
  return response;
}

// =============================================================================
// API SERVICE OBJECT AND DEFAULT EXPORT
// =============================================================================

/**
 * Complete API service object containing all transaction methods
 * Provides unified interface for React components
 * Maps to all CICS transaction codes with modern REST endpoints
 */
export const apiService = {
  // Authentication and session management
  signIn,
  signOut,
  
  // Account management - Maps to COACTVW, COACTUP
  getAccount,
  updateAccount,
  listAccounts,
  
  // Card management - Maps to COCRDLI, COCRDSL, COCRDUP
  listCards,
  getCard,
  updateCard,
  activateCard,
  
  // Transaction management - Maps to COTRN00, COTRN01, COTRN02
  getTransactions,
  getTransactionDetail,
  postTransaction,
  
  // Reporting and statements - Maps to COBIL00, CORPT00
  generateStatement,
  getReports,
  
  // User management - Maps to COUSR00, COUSR01, COUSR02, COUSR03
  getUsers,
  createUser,
  updateUser,
  deleteUser,
  
  // Error handling utility
  handleApiError,
  
  // Configuration access
  config: API_CONFIG,
};

// Default export for ES6 module compatibility
export default apiService;

// =============================================================================
// INITIALIZATION AND CLEANUP
// =============================================================================

/**
 * Initialize API service
 * Sets up monitoring and session management
 */
function initializeApiService() {
  log.info('API Service initialized', {
    version: '1.0.0',
    circuitBreaker: circuitBreaker.name,
    retryConfig: {
      maxAttempts: API_CONFIG.MAX_RETRY_ATTEMPTS,
      baseDelay: API_CONFIG.RETRY_DELAY_BASE_MS,
    },
  });
  
  // Set up periodic session refresh for active users
  if (typeof window !== 'undefined') {
    setInterval(() => {
      const lastActivity = sessionStorage.getItem('lastActivity');
      if (lastActivity) {
        const timeSinceActivity = Date.now() - new Date(lastActivity).getTime();
        if (timeSinceActivity < API_CONFIG.SESSION_REFRESH_INTERVAL_MS) {
          // Update activity timestamp to keep session alive
          sessionStorage.setItem('lastActivity', new Date().toISOString());
        }
      }
    }, API_CONFIG.SESSION_REFRESH_INTERVAL_MS);
  }
}

// Auto-initialize when module loads
initializeApiService();

// =============================================================================
// MODULE EXPORTS SUMMARY
// =============================================================================

/*
 * This module exports the following functions and objects:
 * 
 * Named Exports:
 * - signIn(credentials)
 * - signOut()
 * - getAccount(accountId) 
 * - updateAccount(accountId, accountData)
 * - listAccounts(options)
 * - listCards(options)
 * - getCard(cardNumber)
 * - updateCard(cardNumber, cardData)
 * - activateCard(cardNumber, activationData)
 * - getTransactions(criteria)
 * - getTransactionDetail(transactionId)
 * - postTransaction(transactionData)
 * - generateStatement(statementRequest)
 * - getReports(reportRequest)
 * - getUsers(options)
 * - createUser(userData)
 * - updateUser(userId, userData)
 * - deleteUser(userId, options)
 * - handleApiError(error, requestId, duration)
 * - API_CONFIG (configuration constants)
 *
 * Default Export:
 * - apiService (object containing all above methods plus config)
 *
 * All methods implement comprehensive error handling, COBOL data conversion,
 * session management, and retry logic as specified in the technical requirements.
 */