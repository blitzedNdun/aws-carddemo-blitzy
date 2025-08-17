/**
 * api.js
 *
 * JavaScript API client utility that provides centralized HTTP communication between 
 * the React frontend and Spring Boot REST endpoints. This module handles authentication, 
 * request/response formatting, and error management for all CICS transaction code mappings.
 *
 * This API client replaces CICS SEND/RECEIVE MAP operations with REST API communication
 * while maintaining identical request/response patterns and session state management
 * through Spring Session with Redis backend.
 *
 * Key Features:
 * - Axios-based HTTP client with Spring Boot backend integration
 * - Request/response interceptors for authentication token management
 * - Automatic JSON serialization/deserialization with COBOL data type compatibility
 * - Transaction-specific API methods mapping to CICS transaction codes
 * - Comprehensive error handling and retry logic for resilient communication
 * - Session timeout detection and automatic authentication handling
 * - Precise numeric formatting using COBOL data conversion utilities
 *
 * Authentication Flow:
 * - XSRF-TOKEN and JSESSIONID cookie pair for secure session identification
 * - Automatic token injection into request headers via interceptors
 * - Session state managed server-side through Spring Session
 * - Automatic session timeout detection and redirect handling
 */

import axios from 'axios';
import { formatDecimal } from './CobolDataConverter.js';

// API Configuration Constants
const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || '/api';
const API_TIMEOUT = parseInt(process.env.REACT_APP_API_TIMEOUT) || 30000; // 30 seconds
const MAX_RETRY_ATTEMPTS = 3;
const RETRY_DELAY_MS = 1000;

/**
 * Create configured axios instance with Spring Boot backend settings
 * Replaces CICS terminal-to-program communication with REST API calls
 */
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_TIMEOUT,
  withCredentials: true, // Enable session cookies (JSESSIONID)
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'X-Requested-With': 'XMLHttpRequest', // CSRF protection requirement
  },
});

/**
 * Request interceptor to add authentication tokens to all requests
 * Handles XSRF-TOKEN injection and session state management
 */
apiClient.interceptors.request.use(
  (config) => {
    // Extract XSRF token from cookie for CSRF protection
    const xsrfToken = getCookie('XSRF-TOKEN');
    if (xsrfToken) {
      config.headers['X-XSRF-TOKEN'] = xsrfToken;
    }

    // Add session ID from cookie if available
    const sessionId = getCookie('JSESSIONID');
    if (sessionId) {
      config.headers['X-Session-ID'] = sessionId;
    }

    // Log API request for debugging (development only)
    if (process.env.NODE_ENV === 'development') {
      console.log(`API Request: ${config.method?.toUpperCase()} ${config.url}`, {
        data: config.data,
        headers: config.headers,
      });
    }

    return config;
  },
  (error) => {
    console.error('API Request Interceptor Error:', error);
    return Promise.reject(error);
  }
);

/**
 * Response interceptor for error handling and session timeout detection
 * Provides automatic retry logic and session management
 */
apiClient.interceptors.response.use(
  (response) => {
    // Log successful API response (development only)
    if (process.env.NODE_ENV === 'development') {
      console.log(`API Response: ${response.status} ${response.config?.url}`, {
        data: response.data,
        headers: response.headers,
      });
    }

    // Process numeric data with COBOL precision formatting
    if (response.data && typeof response.data === 'object') {
      response.data = processNumericData(response.data);
    }

    return response;
  },
  async (error) => {
    const originalRequest = error.config;

    // Handle session timeout (401 Unauthorized)
    if (error.response?.status === 401 && !originalRequest._retry) {
      console.warn('Session expired, redirecting to sign-on');
      
      // Clear any cached session data
      clearAuthenticationState();
      
      // Redirect to sign-on page
      if (window.location.pathname !== '/signin') {
        window.location.href = '/signin';
      }
      
      return Promise.reject(error);
    }

    // Handle server errors with retry logic (500-level errors)
    if (error.response?.status >= 500 && 
        originalRequest._retryCount < MAX_RETRY_ATTEMPTS && 
        !originalRequest._retry) {
      
      originalRequest._retryCount = (originalRequest._retryCount || 0) + 1;
      originalRequest._retry = true;

      console.warn(`API request failed, retrying (${originalRequest._retryCount}/${MAX_RETRY_ATTEMPTS})...`);
      
      // Wait before retrying
      await new Promise(resolve => setTimeout(resolve, RETRY_DELAY_MS));
      
      return apiClient(originalRequest);
    }

    // Handle network errors
    if (!error.response) {
      console.error('Network error - check backend connectivity:', error.message);
      
      // Provide user-friendly error message
      error.userMessage = 'Unable to connect to server. Please check your internet connection and try again.';
    } else {
      // Extract error message from response
      const errorMessage = error.response.data?.message || 
                          error.response.data?.error || 
                          `Server error: ${error.response.status}`;
      
      error.userMessage = errorMessage;
      
      console.error(`API Error: ${error.response.status} ${error.config?.url}`, {
        message: errorMessage,
        data: error.response.data,
        headers: error.response.headers,
      });
    }

    return Promise.reject(error);
  }
);

/**
 * Helper function to get cookie value by name
 * Used for extracting session and CSRF tokens
 */
function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) {
    return parts.pop()?.split(';').shift();
  }
  return null;
}

/**
 * Clear authentication state on session timeout
 * Removes cached tokens and session data
 */
function clearAuthenticationState() {
  // Clear session storage
  sessionStorage.clear();
  
  // Clear relevant local storage items
  localStorage.removeItem('userRole');
  localStorage.removeItem('lastActivity');
  
  // Note: HTTP-only cookies are automatically cleared by the browser
  // when the session expires on the server side
}

/**
 * Process numeric data in API responses using COBOL-compatible formatting
 * Ensures proper decimal precision for financial calculations
 */
function processNumericData(data) {
  if (Array.isArray(data)) {
    return data.map(item => processNumericData(item));
  }
  
  if (data && typeof data === 'object') {
    const processed = {};
    
    for (const [key, value] of Object.entries(data)) {
      // Process monetary amounts with proper decimal formatting
      if (key.toLowerCase().includes('amount') || 
          key.toLowerCase().includes('balance') || 
          key.toLowerCase().includes('limit') ||
          key.toLowerCase().includes('fee')) {
        
        processed[key] = formatDecimal(value, 2);
      }
      // Process interest rates with higher precision
      else if (key.toLowerCase().includes('rate') || 
               key.toLowerCase().includes('percent')) {
        
        processed[key] = formatDecimal(value, 4);
      }
      // Recursively process nested objects
      else if (value && typeof value === 'object') {
        processed[key] = processNumericData(value);
      }
      // Keep other values as-is
      else {
        processed[key] = value;
      }
    }
    
    return processed;
  }
  
  return data;
}

/**
 * Generic API request wrapper with error handling
 * Provides consistent error handling across all API calls
 */
async function makeApiRequest(method, url, data = null, config = {}) {
  try {
    const response = await apiClient.request({
      method,
      url,
      data,
      ...config,
    });
    
    return {
      success: true,
      data: response.data,
      status: response.status,
      headers: response.headers,
    };
  } catch (error) {
    return {
      success: false,
      error: error.userMessage || error.message,
      status: error.response?.status,
      data: error.response?.data,
    };
  }
}

// =============================================================================
// CICS Transaction Code API Methods
// Each method maps to a specific CICS transaction code and maintains
// identical functionality while providing modern REST API access patterns
// =============================================================================

/**
 * Sign On API - Maps to CICS transaction code CC00
 * Handles user authentication and session establishment
 * 
 * @param {Object} credentials - User login credentials
 * @param {string} credentials.userId - User ID
 * @param {string} credentials.password - User password
 * @returns {Promise<Object>} Authentication response with user details and session info
 */
export async function signOn(credentials) {
  const response = await makeApiRequest('POST', '/auth/signin', credentials);
  
  if (response.success) {
    // Store user session information
    sessionStorage.setItem('userId', response.data.userId);
    sessionStorage.setItem('userRole', response.data.userRole);
    sessionStorage.setItem('lastActivity', new Date().toISOString());
    
    // Log successful authentication
    console.log(`User ${response.data.userId} authenticated successfully`);
  }
  
  return response;
}

/**
 * Main Menu API - Maps to CICS transaction code CO01 
 * Retrieves main menu options based on user role and permissions
 * 
 * @returns {Promise<Object>} Menu configuration with available options
 */
export async function mainMenu() {
  return makeApiRequest('GET', '/menu/main');
}

/**
 * Admin Menu API - Maps to CICS transaction code COADM01
 * Retrieves administrative menu options for privileged users
 * 
 * @returns {Promise<Object>} Admin menu configuration with system options
 */
export async function adminMenu() {
  return makeApiRequest('GET', '/menu/admin');
}

/**
 * View Account API - Maps to CICS transaction code COACTVW
 * Retrieves detailed account information including balances and status
 * 
 * @param {string} accountId - Account identifier (11 digits)
 * @returns {Promise<Object>} Complete account details with transaction history
 */
export async function viewAccount(accountId) {
  if (!accountId || !/^\d{11}$/.test(accountId)) {
    return {
      success: false,
      error: 'Account ID must be exactly 11 digits',
    };
  }
  
  return makeApiRequest('GET', `/accounts/${accountId}`);
}

/**
 * Update Account API - Maps to CICS transaction code COACTUP
 * Updates account information including limits, status, and contact details
 * 
 * @param {string} accountId - Account identifier
 * @param {Object} accountData - Updated account information
 * @returns {Promise<Object>} Updated account details with confirmation
 */
export async function updateAccount(accountId, accountData) {
  if (!accountId || !/^\d{11}$/.test(accountId)) {
    return {
      success: false,
      error: 'Account ID must be exactly 11 digits',
    };
  }
  
  return makeApiRequest('PUT', `/accounts/${accountId}`, accountData);
}

/**
 * List Cards API - Maps to CICS transaction code COCRDLI
 * Retrieves list of credit cards associated with an account
 * 
 * @param {string} accountId - Account identifier
 * @param {Object} options - Pagination and filtering options
 * @param {number} options.pageSize - Number of records per page (default: 10)
 * @param {number} options.pageNumber - Page number (1-based, default: 1)
 * @returns {Promise<Object>} Paginated list of credit cards
 */
export async function listCards(accountId, options = {}) {
  if (!accountId || !/^\d{11}$/.test(accountId)) {
    return {
      success: false,
      error: 'Account ID must be exactly 11 digits',
    };
  }
  
  const { pageSize = 10, pageNumber = 1 } = options;
  
  return makeApiRequest('GET', `/accounts/${accountId}/cards`, null, {
    params: {
      pageSize,
      pageNumber,
    },
  });
}

/**
 * View Card API - Maps to CICS transaction code COCRDSL
 * Retrieves detailed information for a specific credit card
 * 
 * @param {string} cardNumber - Credit card number (16 digits)
 * @returns {Promise<Object>} Complete card details including limits and status
 */
export async function viewCard(cardNumber) {
  if (!cardNumber || !/^\d{16}$/.test(cardNumber)) {
    return {
      success: false,
      error: 'Card number must be exactly 16 digits',
    };
  }
  
  return makeApiRequest('GET', `/cards/${cardNumber}`);
}

/**
 * Update Card API - Maps to CICS transaction code COCRDUP
 * Updates credit card information including limits, status, and security settings
 * 
 * @param {string} cardNumber - Credit card number
 * @param {Object} cardData - Updated card information
 * @returns {Promise<Object>} Updated card details with confirmation
 */
export async function updateCard(cardNumber, cardData) {
  if (!cardNumber || !/^\d{16}$/.test(cardNumber)) {
    return {
      success: false,
      error: 'Card number must be exactly 16 digits',
    };
  }
  
  return makeApiRequest('PUT', `/cards/${cardNumber}`, cardData);
}

/**
 * List Transactions API - Maps to CICS transaction code COTRN00
 * Retrieves paginated list of transactions with filtering capabilities
 * 
 * @param {Object} criteria - Search and filtering criteria
 * @param {string} criteria.accountId - Account identifier (optional)
 * @param {string} criteria.cardNumber - Card number (optional)
 * @param {string} criteria.startDate - Start date (YYYY-MM-DD format)
 * @param {string} criteria.endDate - End date (YYYY-MM-DD format)
 * @param {number} criteria.pageSize - Number of records per page (default: 10)
 * @param {number} criteria.pageNumber - Page number (1-based, default: 1)
 * @returns {Promise<Object>} Paginated transaction list with summary totals
 */
export async function listTransactions(criteria = {}) {
  const {
    accountId,
    cardNumber,
    startDate,
    endDate,
    pageSize = 10,
    pageNumber = 1,
  } = criteria;
  
  // Validate account ID if provided
  if (accountId && !/^\d{11}$/.test(accountId)) {
    return {
      success: false,
      error: 'Account ID must be exactly 11 digits',
    };
  }
  
  // Validate card number if provided
  if (cardNumber && !/^\d{16}$/.test(cardNumber)) {
    return {
      success: false,
      error: 'Card number must be exactly 16 digits',
    };
  }
  
  return makeApiRequest('GET', '/transactions', null, {
    params: {
      accountId,
      cardNumber,
      startDate,
      endDate,
      pageSize,
      pageNumber,
    },
  });
}

/**
 * View Transaction API - Maps to CICS transaction code COTRN01
 * Retrieves detailed information for a specific transaction
 * 
 * @param {string} transactionId - Transaction identifier
 * @returns {Promise<Object>} Complete transaction details including authorization data
 */
export async function viewTransaction(transactionId) {
  if (!transactionId) {
    return {
      success: false,
      error: 'Transaction ID is required',
    };
  }
  
  return makeApiRequest('GET', `/transactions/${transactionId}`);
}

/**
 * Create Transaction API - Maps to CICS transaction code COTRN02
 * Creates a new transaction (manual entry or adjustment)
 * 
 * @param {Object} transactionData - New transaction information
 * @param {string} transactionData.accountId - Account identifier
 * @param {string} transactionData.cardNumber - Card number
 * @param {string} transactionData.amount - Transaction amount
 * @param {string} transactionData.description - Transaction description
 * @param {string} transactionData.transactionType - Type of transaction
 * @returns {Promise<Object>} Created transaction details with confirmation
 */
export async function createTransaction(transactionData) {
  const { accountId, cardNumber } = transactionData;
  
  // Validate required account ID
  if (!accountId || !/^\d{11}$/.test(accountId)) {
    return {
      success: false,
      error: 'Account ID must be exactly 11 digits',
    };
  }
  
  // Validate required card number
  if (!cardNumber || !/^\d{16}$/.test(cardNumber)) {
    return {
      success: false,
      error: 'Card number must be exactly 16 digits',
    };
  }
  
  return makeApiRequest('POST', '/transactions', transactionData);
}

// =============================================================================
// Export configured axios instance and API methods
// =============================================================================

// Named exports for individual API methods and client
export { apiClient };

// Default export (alias for apiClient to match schema requirements)
export default apiClient;

// Create API object alias for alternative import syntax
export const api = apiClient;