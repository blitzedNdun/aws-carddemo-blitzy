/**
 * CardDemo API Client Utilities
 * 
 * This module provides comprehensive HTTP communication utilities for the CardDemo React application,
 * handling communication with Spring Boot microservices while maintaining exact transaction boundaries
 * and behavioral equivalence with the original CICS processing patterns.
 * 
 * Key Features:
 * - JWT authentication with automatic token refresh and session management
 * - Request/response interceptors for distributed tracing and error handling
 * - COMMAREA to JSON DTO conversion maintaining exact COBOL data integrity
 * - API wrapper functions mapping CICS transaction codes to REST endpoint URLs
 * - Error handling that provides BMS-equivalent error message display patterns
 * - Session state management equivalent to CICS pseudo-conversational processing
 * 
 * Architecture:
 * - Axios HTTP client with configured interceptors for authentication and error handling
 * - Token manager for JWT lifecycle management with automatic refresh
 * - Domain-specific API wrappers (authentication, account, transaction) for organized endpoint access
 * - Comprehensive error handling mapping Spring Boot errors to BMS-equivalent user experience
 * - Data transformation utilities ensuring exact COBOL precision preservation
 * 
 * @author Blitzy Platform - CardDemo Migration Team
 * @version 1.0.0
 * @since 2024-01-01
 */

import axios from 'axios';
import jwt from 'jsonwebtoken';
import { TransactionConstants } from '../constants/TransactionConstants';
import { MessageConstants } from '../constants/MessageConstants';
import { ApiTypes } from '../types/ApiTypes';
import { DataFormatting } from './dataFormatting';

// ===============================================================================
// CONSTANTS AND CONFIGURATION
// ===============================================================================

/**
 * API Configuration Constants
 * Defines base URL patterns and request/response defaults matching CICS transaction patterns
 */
const API_CONFIG = {
    BASE_URL: process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080',
    TIMEOUT: 30000, // 30 seconds to match CICS transaction timeout
    MAX_RETRIES: 3,
    RETRY_DELAY: 1000,
    DEFAULT_HEADERS: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'X-Requested-With': 'XMLHttpRequest',
        'X-API-Version': '1.0',
        'X-Client-Type': 'React-Frontend'
    }
};

/**
 * JWT Configuration Constants
 * Manages JWT token lifecycle and refresh patterns
 */
const JWT_CONFIG = {
    TOKEN_STORAGE_KEY: 'cardDemo_jwt_token',
    REFRESH_TOKEN_STORAGE_KEY: 'cardDemo_refresh_token',
    TOKEN_EXPIRY_BUFFER: 300, // 5 minutes buffer before expiry
    REFRESH_RETRY_ATTEMPTS: 3,
    REFRESH_RETRY_DELAY: 2000
};

/**
 * Session Configuration Constants
 * Manages pseudo-conversational session state equivalent to CICS processing
 */
const SESSION_CONFIG = {
    SESSION_STORAGE_KEY: 'cardDemo_session_id',
    SESSION_TIMEOUT: 1800, // 30 minutes to match CICS session timeout
    HEARTBEAT_INTERVAL: 300000, // 5 minutes heartbeat
    MAX_CONCURRENT_REQUESTS: 10
};

// ===============================================================================
// UTILITY FUNCTIONS
// ===============================================================================

/**
 * Generates a unique correlation ID for request tracing
 * Enables distributed tracing equivalent to CICS transaction ID tracking
 * 
 * @returns {string} Unique correlation ID in format: YYYYMMDD-HHMMSS-RANDOM
 */
function generateCorrelationId() {
    const now = new Date();
    const datePart = now.toISOString().slice(0, 10).replace(/-/g, '');
    const timePart = now.toTimeString().slice(0, 8).replace(/:/g, '');
    const randomPart = Math.random().toString(36).substring(2, 8).toUpperCase();
    return `${datePart}-${timePart}-${randomPart}`;
}

/**
 * Generates session ID for pseudo-conversational processing
 * Maintains session state equivalent to CICS terminal session management
 * 
 * @returns {string} Session ID in format: SESS-TIMESTAMP-RANDOM
 */
function generateSessionId() {
    const timestamp = Date.now().toString(36).toUpperCase();
    const random = Math.random().toString(36).substring(2, 8).toUpperCase();
    return `SESS-${timestamp}-${random}`;
}

/**
 * Safely retrieves data from localStorage with error handling
 * 
 * @param {string} key - Storage key
 * @returns {string|null} Stored value or null if not found/error
 */
function getFromStorage(key) {
    try {
        return localStorage.getItem(key);
    } catch (error) {
        console.error(`Error reading from localStorage for key ${key}:`, error);
        return null;
    }
}

/**
 * Safely stores data in localStorage with error handling
 * 
 * @param {string} key - Storage key
 * @param {string} value - Value to store
 * @returns {boolean} Success status
 */
function setInStorage(key, value) {
    try {
        localStorage.setItem(key, value);
        return true;
    } catch (error) {
        console.error(`Error writing to localStorage for key ${key}:`, error);
        return false;
    }
}

/**
 * Safely removes data from localStorage with error handling
 * 
 * @param {string} key - Storage key
 * @returns {boolean} Success status
 */
function removeFromStorage(key) {
    try {
        localStorage.removeItem(key);
        return true;
    } catch (error) {
        console.error(`Error removing from localStorage for key ${key}:`, error);
        return false;
    }
}

// ===============================================================================
// TOKEN MANAGEMENT
// ===============================================================================

/**
 * Token Manager
 * Handles JWT token lifecycle including storage, validation, and automatic refresh
 * Maintains authentication state equivalent to RACF user session management
 */
export const tokenManager = {
    /**
     * Sets JWT token in storage with expiration tracking
     * 
     * @param {string} token - JWT token string
     * @param {string} refreshToken - Refresh token for automatic renewal
     * @returns {boolean} Success status
     */
    setToken(token, refreshToken = null) {
        try {
            if (!token) {
                console.error('Cannot set empty token');
                return false;
            }

            const success = setInStorage(JWT_CONFIG.TOKEN_STORAGE_KEY, token);
            if (success && refreshToken) {
                setInStorage(JWT_CONFIG.REFRESH_TOKEN_STORAGE_KEY, refreshToken);
            }

            return success;
        } catch (error) {
            console.error('Error setting token:', error);
            return false;
        }
    },

    /**
     * Retrieves JWT token from storage
     * 
     * @returns {string|null} JWT token or null if not found
     */
    getToken() {
        return getFromStorage(JWT_CONFIG.TOKEN_STORAGE_KEY);
    },

    /**
     * Retrieves refresh token from storage
     * 
     * @returns {string|null} Refresh token or null if not found
     */
    getRefreshToken() {
        return getFromStorage(JWT_CONFIG.REFRESH_TOKEN_STORAGE_KEY);
    },

    /**
     * Removes JWT token from storage
     * 
     * @returns {boolean} Success status
     */
    removeToken() {
        const tokenRemoved = removeFromStorage(JWT_CONFIG.TOKEN_STORAGE_KEY);
        const refreshTokenRemoved = removeFromStorage(JWT_CONFIG.REFRESH_TOKEN_STORAGE_KEY);
        return tokenRemoved && refreshTokenRemoved;
    },

    /**
     * Validates JWT token and checks expiration
     * 
     * @param {string} token - JWT token to validate (optional, uses stored token if not provided)
     * @returns {boolean} True if token is valid and not expired
     */
    isTokenValid(token = null) {
        try {
            const tokenToValidate = token || this.getToken();
            if (!tokenToValidate) {
                return false;
            }

            // Decode token without verification (we trust our own backend)
            const decoded = jwt.decode(tokenToValidate);
            if (!decoded || !decoded.exp) {
                return false;
            }

            // Check if token is expired with buffer
            const now = Math.floor(Date.now() / 1000);
            const expirationWithBuffer = decoded.exp - JWT_CONFIG.TOKEN_EXPIRY_BUFFER;
            
            return now < expirationWithBuffer;
        } catch (error) {
            console.error('Error validating token:', error);
            return false;
        }
    },

    /**
     * Gets token expiration timestamp
     * 
     * @param {string} token - JWT token (optional, uses stored token if not provided)
     * @returns {number|null} Expiration timestamp or null if invalid
     */
    getTokenExpiry(token = null) {
        try {
            const tokenToCheck = token || this.getToken();
            if (!tokenToCheck) {
                return null;
            }

            const decoded = jwt.decode(tokenToCheck);
            return decoded && decoded.exp ? decoded.exp * 1000 : null;
        } catch (error) {
            console.error('Error getting token expiry:', error);
            return null;
        }
    },

    /**
     * Refreshes JWT token if needed
     * Implements automatic token refresh with retry logic
     * 
     * @returns {Promise<boolean>} Promise resolving to success status
     */
    async refreshTokenIfNeeded() {
        try {
            const currentToken = this.getToken();
            if (!currentToken) {
                return false;
            }

            // Check if token needs refresh
            if (this.isTokenValid(currentToken)) {
                return true; // Token is still valid
            }

            const refreshToken = this.getRefreshToken();
            if (!refreshToken) {
                return false;
            }

            // Attempt to refresh token
            const response = await axios.post(
                `${API_CONFIG.BASE_URL}${TransactionConstants.API_BASE_PATHS.AUTH_SERVICE}/refresh`,
                { refreshToken },
                {
                    headers: API_CONFIG.DEFAULT_HEADERS,
                    timeout: API_CONFIG.TIMEOUT
                }
            );

            if (response.data && response.data.status === '00') {
                const newToken = response.data.data.token;
                const newRefreshToken = response.data.data.refreshToken;
                
                return this.setToken(newToken, newRefreshToken);
            }

            return false;
        } catch (error) {
            console.error('Error refreshing token:', error);
            return false;
        }
    }
};

// ===============================================================================
// ERROR HANDLING
// ===============================================================================

/**
 * API Error Handler
 * Translates Spring Boot error responses to BMS-equivalent error display patterns
 * Maintains exact error message compatibility with original COBOL error handling
 * 
 * @param {Error} error - Axios error object
 * @param {string} correlationId - Request correlation ID for tracking
 * @returns {Object} Standardized error response object
 */
export function handleApiError(error, correlationId = null) {
    // Default error response structure
    const errorResponse = {
        error: {
            code: '99',
            type: 'SYSTEM',
            message: MessageConstants.API_ERROR_MESSAGES.SERVER_ERROR,
            fieldErrors: []
        },
        status: '99',
        message: MessageConstants.API_ERROR_MESSAGES.SERVER_ERROR,
        timestamp: new Date().toISOString(),
        correlationId: correlationId || generateCorrelationId()
    };

    try {
        if (axios.isAxiosError(error)) {
            const { response, request, code } = error;

            if (response) {
                // Server responded with error status
                const { status, data } = response;
                
                switch (status) {
                    case 400:
                        errorResponse.error.code = '02';
                        errorResponse.error.type = 'VALIDATION';
                        errorResponse.status = '02';
                        errorResponse.message = data.message || MessageConstants.VALIDATION_ERRORS.INVALID_FORMAT;
                        
                        // Map field errors if present
                        if (data.fieldErrors && Array.isArray(data.fieldErrors)) {
                            errorResponse.error.fieldErrors = data.fieldErrors;
                        }
                        break;

                    case 401:
                        errorResponse.error.code = '04';
                        errorResponse.error.type = 'SECURITY';
                        errorResponse.status = '04';
                        errorResponse.message = MessageConstants.API_ERROR_MESSAGES.AUTHENTICATION_FAILED;
                        break;

                    case 403:
                        errorResponse.error.code = '04';
                        errorResponse.error.type = 'SECURITY';
                        errorResponse.status = '04';
                        errorResponse.message = MessageConstants.API_ERROR_MESSAGES.AUTHORIZATION_DENIED;
                        break;

                    case 404:
                        errorResponse.error.code = '03';
                        errorResponse.error.type = 'NOT_FOUND';
                        errorResponse.status = '03';
                        errorResponse.message = data.message || 'Resource not found';
                        break;

                    case 503:
                        errorResponse.error.code = '06';
                        errorResponse.error.type = 'SYSTEM';
                        errorResponse.status = '06';
                        errorResponse.message = MessageConstants.API_ERROR_MESSAGES.SERVER_ERROR;
                        break;

                    default:
                        errorResponse.error.code = '05';
                        errorResponse.error.type = 'SYSTEM';
                        errorResponse.status = '05';
                        errorResponse.message = data.message || MessageConstants.API_ERROR_MESSAGES.SERVER_ERROR;
                }
            } else if (request) {
                // Request was made but no response received
                if (code === 'ECONNABORTED') {
                    errorResponse.error.code = '06';
                    errorResponse.error.type = 'SYSTEM';
                    errorResponse.status = '06';
                    errorResponse.message = MessageConstants.API_ERROR_MESSAGES.TIMEOUT_ERROR;
                } else {
                    errorResponse.error.code = '06';
                    errorResponse.error.type = 'SYSTEM';
                    errorResponse.status = '06';
                    errorResponse.message = MessageConstants.API_ERROR_MESSAGES.NETWORK_ERROR;
                }
            }
        }

        // Log error for debugging
        console.error('API Error:', {
            originalError: error,
            standardizedError: errorResponse,
            correlationId: errorResponse.correlationId
        });

        return errorResponse;
    } catch (processingError) {
        console.error('Error processing API error:', processingError);
        return errorResponse;
    }
}

// ===============================================================================
// AXIOS CLIENT CONFIGURATION
// ===============================================================================

/**
 * Creates configured Axios instance with interceptors
 * Provides centralized HTTP client configuration with authentication and error handling
 * 
 * @param {Object} config - Optional configuration overrides
 * @returns {Object} Configured Axios instance
 */
export function createApiClient(config = {}) {
    const axiosConfig = {
        baseURL: API_CONFIG.BASE_URL,
        timeout: API_CONFIG.TIMEOUT,
        headers: { ...API_CONFIG.DEFAULT_HEADERS },
        ...config
    };

    const client = axios.create(axiosConfig);

    // Request interceptor for authentication and request preprocessing
    client.interceptors.request.use(
        async (config) => {
            try {
                // Generate correlation ID for request tracing
                const correlationId = generateCorrelationId();
                config.headers['X-Correlation-ID'] = correlationId;

                // Add session ID for pseudo-conversational processing
                let sessionId = getFromStorage(SESSION_CONFIG.SESSION_STORAGE_KEY);
                if (!sessionId) {
                    sessionId = generateSessionId();
                    setInStorage(SESSION_CONFIG.SESSION_STORAGE_KEY, sessionId);
                }
                config.headers['X-Session-ID'] = sessionId;

                // Add JWT token if available and valid
                const token = tokenManager.getToken();
                if (token && tokenManager.isTokenValid(token)) {
                    config.headers.Authorization = `Bearer ${token}`;
                } else if (token) {
                    // Try to refresh token if expired
                    const refreshed = await tokenManager.refreshTokenIfNeeded();
                    if (refreshed) {
                        const newToken = tokenManager.getToken();
                        config.headers.Authorization = `Bearer ${newToken}`;
                    }
                }

                // Add request timestamp
                config.headers['X-Request-Timestamp'] = new Date().toISOString();

                // Transform request data if needed
                if (config.data && typeof config.data === 'object') {
                    config.data = {
                        ...config.data,
                        correlationId,
                        sessionId,
                        timestamp: new Date().toISOString()
                    };
                }

                return config;
            } catch (error) {
                console.error('Request interceptor error:', error);
                return Promise.reject(error);
            }
        },
        (error) => {
            console.error('Request interceptor error:', error);
            return Promise.reject(error);
        }
    );

    // Response interceptor for response preprocessing and error handling
    client.interceptors.response.use(
        (response) => {
            try {
                // Extract correlation ID from response
                const correlationId = response.headers['x-correlation-id'] || 
                                    response.config.headers['X-Correlation-ID'];

                // Standardize response structure
                if (response.data && typeof response.data === 'object') {
                    response.data = {
                        ...response.data,
                        correlationId,
                        timestamp: response.data.timestamp || new Date().toISOString()
                    };
                }

                return response;
            } catch (error) {
                console.error('Response interceptor error:', error);
                return response;
            }
        },
        async (error) => {
            const correlationId = error.config && error.config.headers ? 
                                error.config.headers['X-Correlation-ID'] : null;

            // Handle authentication errors
            if (error.response && error.response.status === 401) {
                const refreshed = await tokenManager.refreshTokenIfNeeded();
                if (refreshed && error.config) {
                    // Retry the original request with new token
                    error.config.headers.Authorization = `Bearer ${tokenManager.getToken()}`;
                    return client.request(error.config);
                } else {
                    // Clear invalid tokens
                    tokenManager.removeToken();
                    removeFromStorage(SESSION_CONFIG.SESSION_STORAGE_KEY);
                }
            }

            return Promise.reject(handleApiError(error, correlationId));
        }
    );

    return client;
}

// Create default API client instance
const defaultApiClient = createApiClient();

// ===============================================================================
// CORE API CLIENT METHODS
// ===============================================================================

/**
 * Core API Client
 * Provides fundamental HTTP methods with authentication and error handling
 * Default export providing unified interface for all API communication
 */
const apiClient = {
    /**
     * Performs authenticated login request
     * Maps to COSGN00 transaction functionality
     * 
     * @param {Object} credentials - User credentials
     * @param {string} credentials.userId - User ID (8 characters max)
     * @param {string} credentials.password - Password (8 characters max)
     * @returns {Promise<Object>} Login response with JWT token
     */
    async login(credentials) {
        try {
            const correlationId = generateCorrelationId();
            
            // Validate credentials format
            if (!credentials || !credentials.userId || !credentials.password) {
                throw new Error('User ID and password are required');
            }

            // Format credentials to match COBOL field requirements
            const formattedCredentials = {
                userId: DataFormatting.formatPicX(credentials.userId, 8),
                password: credentials.password, // Keep password as-is for validation
                correlationId
            };

            const response = await defaultApiClient.post(
                TransactionConstants.TRANSACTION_ENDPOINTS.LOGIN,
                formattedCredentials
            );

            // Store tokens if login successful
            if (response.data && response.data.status === '00') {
                const { token, refreshToken } = response.data.data;
                tokenManager.setToken(token, refreshToken);
            }

            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    },

    /**
     * Performs logout request and clears authentication state
     * 
     * @returns {Promise<Object>} Logout response
     */
    async logout() {
        try {
            const correlationId = generateCorrelationId();
            
            const response = await defaultApiClient.post(
                `${TransactionConstants.API_BASE_PATHS.AUTH_SERVICE}/logout`,
                { correlationId }
            );

            // Clear authentication state
            tokenManager.removeToken();
            removeFromStorage(SESSION_CONFIG.SESSION_STORAGE_KEY);

            return response.data;
        } catch (error) {
            // Clear authentication state even if logout fails
            tokenManager.removeToken();
            removeFromStorage(SESSION_CONFIG.SESSION_STORAGE_KEY);
            throw handleApiError(error);
        }
    },

    /**
     * Performs GET request with authentication
     * 
     * @param {string} url - Request URL
     * @param {Object} config - Optional Axios config
     * @returns {Promise<Object>} Response data
     */
    async get(url, config = {}) {
        try {
            const response = await defaultApiClient.get(url, config);
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    },

    /**
     * Performs POST request with authentication
     * 
     * @param {string} url - Request URL
     * @param {Object} data - Request payload
     * @param {Object} config - Optional Axios config
     * @returns {Promise<Object>} Response data
     */
    async post(url, data, config = {}) {
        try {
            const response = await defaultApiClient.post(url, data, config);
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    },

    /**
     * Performs PUT request with authentication
     * 
     * @param {string} url - Request URL
     * @param {Object} data - Request payload
     * @param {Object} config - Optional Axios config
     * @returns {Promise<Object>} Response data
     */
    async put(url, data, config = {}) {
        try {
            const response = await defaultApiClient.put(url, data, config);
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    },

    /**
     * Performs DELETE request with authentication
     * 
     * @param {string} url - Request URL
     * @param {Object} config - Optional Axios config
     * @returns {Promise<Object>} Response data
     */
    async delete(url, config = {}) {
        try {
            const response = await defaultApiClient.delete(url, config);
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    },

    /**
     * Sets authentication token for subsequent requests
     * 
     * @param {string} token - JWT token
     * @param {string} refreshToken - Refresh token (optional)
     * @returns {boolean} Success status
     */
    setAuthToken(token, refreshToken = null) {
        return tokenManager.setToken(token, refreshToken);
    },

    /**
     * Clears authentication token
     * 
     * @returns {boolean} Success status
     */
    clearAuthToken() {
        return tokenManager.removeToken();
    },

    /**
     * Refreshes authentication token
     * 
     * @returns {Promise<boolean>} Success status
     */
    async refreshToken() {
        return await tokenManager.refreshTokenIfNeeded();
    }
};

// ===============================================================================
// DOMAIN-SPECIFIC API WRAPPERS
// ===============================================================================

/**
 * Authentication API Wrapper
 * Provides authentication-specific API methods mapping to COSGN00 transaction patterns
 */
export const authenticationApi = {
    /**
     * Authenticates user with credentials
     * Maps to COSGN00 CICS transaction functionality
     * 
     * @param {Object} credentials - User credentials
     * @returns {Promise<Object>} Authentication response
     */
    async login(credentials) {
        return await apiClient.login(credentials);
    },

    /**
     * Logs out current user
     * 
     * @returns {Promise<Object>} Logout response
     */
    async logout() {
        return await apiClient.logout();
    },

    /**
     * Refreshes current authentication token
     * 
     * @returns {Promise<Object>} Refresh response
     */
    async refreshToken() {
        const refreshed = await tokenManager.refreshTokenIfNeeded();
        return {
            status: refreshed ? '00' : '04',
            message: refreshed ? 'Token refreshed successfully' : 'Token refresh failed',
            data: refreshed ? { token: tokenManager.getToken() } : null,
            timestamp: new Date().toISOString(),
            correlationId: generateCorrelationId()
        };
    },

    /**
     * Validates current authentication token
     * 
     * @returns {Promise<Object>} Validation response
     */
    async validateToken() {
        const isValid = tokenManager.isTokenValid();
        return {
            status: isValid ? '00' : '04',
            message: isValid ? 'Token is valid' : 'Token is invalid or expired',
            data: {
                valid: isValid,
                expiry: tokenManager.getTokenExpiry()
            },
            timestamp: new Date().toISOString(),
            correlationId: generateCorrelationId()
        };
    }
};

/**
 * Account API Wrapper
 * Provides account-specific API methods mapping to COACTVW/COACTUP transaction patterns
 */
export const accountApi = {
    /**
     * Retrieves account details
     * Maps to COACTVW CICS transaction functionality
     * 
     * @param {string} accountNumber - Account number (11 digits)
     * @returns {Promise<Object>} Account details response
     */
    async getAccountDetails(accountNumber) {
        try {
            const formattedAccountNumber = DataFormatting.formatAccountNumber(accountNumber);
            const url = `${TransactionConstants.TRANSACTION_ENDPOINTS.ACCOUNT_VIEW}/${formattedAccountNumber}`;
            return await apiClient.get(url);
        } catch (error) {
            throw handleApiError(error);
        }
    },

    /**
     * Updates account information
     * Maps to COACTUP CICS transaction functionality
     * 
     * @param {string} accountNumber - Account number (11 digits)
     * @param {Object} accountData - Account data to update
     * @returns {Promise<Object>} Update response
     */
    async updateAccount(accountNumber, accountData) {
        try {
            const formattedAccountNumber = DataFormatting.formatAccountNumber(accountNumber);
            const url = `${TransactionConstants.TRANSACTION_ENDPOINTS.ACCOUNT_UPDATE}/${formattedAccountNumber}`;
            
            // Format account data to match COBOL field requirements
            const formattedData = {
                accountNumber: formattedAccountNumber,
                ...accountData,
                correlationId: generateCorrelationId()
            };

            return await apiClient.put(url, formattedData);
        } catch (error) {
            throw handleApiError(error);
        }
    },

    /**
     * Retrieves paginated account list
     * 
     * @param {Object} params - Query parameters
     * @param {number} params.page - Page number (0-based)
     * @param {number} params.size - Page size
     * @param {string} params.sortBy - Sort field
     * @param {string} params.sortOrder - Sort order (ASC/DESC)
     * @returns {Promise<Object>} Paginated account list response
     */
    async getAccountList(params = {}) {
        try {
            const queryParams = new URLSearchParams({
                page: params.page || 0,
                size: params.size || 10,
                sortBy: params.sortBy || 'accountNumber',
                sortOrder: params.sortOrder || 'ASC'
            });

            const url = `${TransactionConstants.API_BASE_PATHS.ACCOUNT_SERVICE}/list?${queryParams}`;
            return await apiClient.get(url);
        } catch (error) {
            throw handleApiError(error);
        }
    },

    /**
     * Searches accounts with criteria
     * 
     * @param {Object} searchCriteria - Search criteria
     * @param {string} searchCriteria.accountNumber - Account number filter
     * @param {string} searchCriteria.customerName - Customer name filter
     * @param {string} searchCriteria.status - Account status filter
     * @returns {Promise<Object>} Search results response
     */
    async searchAccounts(searchCriteria) {
        try {
            const url = `${TransactionConstants.API_BASE_PATHS.ACCOUNT_SERVICE}/search`;
            const formattedCriteria = {
                ...searchCriteria,
                correlationId: generateCorrelationId()
            };

            return await apiClient.post(url, formattedCriteria);
        } catch (error) {
            throw handleApiError(error);
        }
    }
};

/**
 * Transaction API Wrapper
 * Provides transaction-specific API methods mapping to COTRN00/COTRN01/COTRN02 transaction patterns
 */
export const transactionApi = {
    /**
     * Retrieves transaction history with pagination
     * Maps to COTRN00 CICS transaction functionality
     * 
     * @param {string} accountNumber - Account number (11 digits)
     * @param {Object} params - Query parameters
     * @param {number} params.page - Page number (0-based)
     * @param {number} params.size - Page size
     * @param {string} params.sortBy - Sort field
     * @param {string} params.sortOrder - Sort order (ASC/DESC)
     * @returns {Promise<Object>} Paginated transaction history response
     */
    async getTransactionHistory(accountNumber, params = {}) {
        try {
            const formattedAccountNumber = DataFormatting.formatAccountNumber(accountNumber);
            const queryParams = new URLSearchParams({
                page: params.page || 0,
                size: params.size || 10,
                sortBy: params.sortBy || 'transactionDate',
                sortOrder: params.sortOrder || 'DESC'
            });

            const url = `${TransactionConstants.TRANSACTION_ENDPOINTS.TRANSACTION_LIST}/${formattedAccountNumber}?${queryParams}`;
            return await apiClient.get(url);
        } catch (error) {
            throw handleApiError(error);
        }
    },

    /**
     * Adds new transaction
     * Maps to COTRN02 CICS transaction functionality
     * 
     * @param {Object} transactionData - Transaction data
     * @param {string} transactionData.accountNumber - Account number
     * @param {string} transactionData.transactionType - Transaction type
     * @param {number} transactionData.amount - Transaction amount
     * @param {string} transactionData.description - Transaction description
     * @returns {Promise<Object>} Add transaction response
     */
    async addTransaction(transactionData) {
        try {
            const url = `${TransactionConstants.API_BASE_PATHS.TRANSACTION_SERVICE}/add`;
            
            // Format transaction data to match COBOL field requirements
            const formattedData = {
                accountNumber: DataFormatting.formatAccountNumber(transactionData.accountNumber),
                transactionType: DataFormatting.formatPicX(transactionData.transactionType, 4),
                amount: DataFormatting.formatCurrency(transactionData.amount),
                description: DataFormatting.formatPicX(transactionData.description, 50),
                correlationId: generateCorrelationId(),
                ...transactionData
            };

            return await apiClient.post(url, formattedData);
        } catch (error) {
            throw handleApiError(error);
        }
    },

    /**
     * Searches transactions with criteria
     * Maps to COTRN01 CICS transaction functionality
     * 
     * @param {Object} searchCriteria - Search criteria
     * @param {string} searchCriteria.transactionId - Transaction ID filter
     * @param {string} searchCriteria.accountNumber - Account number filter
     * @param {string} searchCriteria.dateFrom - Date range start
     * @param {string} searchCriteria.dateTo - Date range end
     * @param {string} searchCriteria.transactionType - Transaction type filter
     * @returns {Promise<Object>} Search results response
     */
    async searchTransactions(searchCriteria) {
        try {
            const url = `${TransactionConstants.API_BASE_PATHS.TRANSACTION_SERVICE}/search`;
            const formattedCriteria = {
                ...searchCriteria,
                correlationId: generateCorrelationId()
            };

            return await apiClient.post(url, formattedCriteria);
        } catch (error) {
            throw handleApiError(error);
        }
    },

    /**
     * Retrieves specific transaction details
     * Maps to COTRN01 CICS transaction functionality
     * 
     * @param {string} transactionId - Transaction ID
     * @returns {Promise<Object>} Transaction details response
     */
    async getTransactionDetails(transactionId) {
        try {
            const url = `${TransactionConstants.API_BASE_PATHS.TRANSACTION_SERVICE}/details/${transactionId}`;
            return await apiClient.get(url);
        } catch (error) {
            throw handleApiError(error);
        }
    }
};

// ===============================================================================
// EXPORTS
// ===============================================================================

// Export default API client
export default apiClient;