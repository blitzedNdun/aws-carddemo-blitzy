/**
 * CardDemo API Client Utilities
 * 
 * Comprehensive API client for CardDemo React application that handles HTTP communication
 * with Spring Boot microservices while maintaining exact functional equivalence with
 * original CICS transaction processing patterns.
 * 
 * Key Features:
 * - JWT authentication with automatic token management and refresh
 * - COMMAREA to JSON DTO conversion preserving COBOL data structures
 * - Transaction boundary preservation matching original CICS processing
 * - BMS-equivalent error handling and message display
 * - Session management with Redis-backed pseudo-conversational state
 * - Request/response interceptors for distributed tracing and monitoring
 * - Circuit breaker integration with Spring Cloud Gateway
 * - Exact decimal precision preservation for financial calculations
 * 
 * This module replaces CICS COMMAREA data passing with structured JSON DTOs while
 * maintaining identical request/response semantics and error handling patterns
 * extracted from the original BMS mapset definitions.
 * 
 * @fileoverview CardDemo API Client
 * @version 1.0.0
 * @author Blitzy Platform - CardDemo Migration Team
 * @copyright 2024 CardDemo Application Migration Project
 * @license Apache-2.0
 */

import axios from 'axios';
import jwt from 'jsonwebtoken';
import { TransactionConstants } from '../constants/TransactionConstants';
import { MessageConstants } from '../constants/MessageConstants';
import { ApiTypes } from '../types/ApiTypes';
import { DataFormatting } from './dataFormatting';

// Configure axios defaults for CardDemo application
const DEFAULT_TIMEOUT = 30000; // 30 second timeout matching CICS transaction timeout
const DEFAULT_RETRY_ATTEMPTS = 3;
const DEFAULT_RETRY_DELAY = 1000; // 1 second base delay

/**
 * Creates and configures an Axios HTTP client instance with CardDemo-specific settings
 * Implements request/response interceptors for authentication, error handling, and
 * distributed tracing integration with Spring Cloud Gateway
 * 
 * @param {Object} config - Optional configuration overrides
 * @returns {Object} Configured axios instance with interceptors
 */
export function createApiClient(config = {}) {
    // Create axios instance with default configuration
    const client = axios.create({
        baseURL: config.baseURL || process.env.REACT_APP_API_BASE_URL || '/api',
        timeout: config.timeout || DEFAULT_TIMEOUT,
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            'X-Requested-With': 'XMLHttpRequest',
            'User-Agent': 'CardDemo-React/1.0'
        }
    });

    // Request interceptor for authentication and correlation tracking
    client.interceptors.request.use(
        (requestConfig) => {
            // Generate correlation ID for distributed tracing
            const correlationId = `req-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
            requestConfig.headers['X-Correlation-ID'] = correlationId;
            requestConfig.headers['X-Request-Timestamp'] = new Date().toISOString();

            // Add JWT token if available
            const token = tokenManager.getToken();
            if (token && tokenManager.isTokenValid()) {
                requestConfig.headers['Authorization'] = `Bearer ${token}`;
            }

            // Add session ID for pseudo-conversational state
            const sessionId = sessionStorage.getItem('carddemo-session-id');
            if (sessionId) {
                requestConfig.headers['X-Session-ID'] = sessionId;
            }

            // Log request for debugging (in development)
            if (process.env.NODE_ENV === 'development') {
                console.debug('API Request:', {
                    url: requestConfig.url,
                    method: requestConfig.method,
                    correlationId,
                    headers: requestConfig.headers
                });
            }

            return requestConfig;
        },
        (error) => {
            console.error('Request interceptor error:', error);
            return Promise.reject(error);
        }
    );

    // Response interceptor for error handling and session management
    client.interceptors.response.use(
        (response) => {
            // Extract correlation ID for tracing
            const correlationId = response.headers['x-correlation-id'] || 'unknown';
            
            // Update session ID if provided
            const sessionId = response.headers['x-session-id'];
            if (sessionId) {
                sessionStorage.setItem('carddemo-session-id', sessionId);
            }

            // Log response for debugging (in development)
            if (process.env.NODE_ENV === 'development') {
                console.debug('API Response:', {
                    url: response.config.url,
                    status: response.status,
                    correlationId,
                    data: response.data
                });
            }

            // Transform response to match ApiResponse interface
            if (response.data && typeof response.data === 'object') {
                response.data.correlationId = correlationId;
                response.data.timestamp = response.data.timestamp || new Date().toISOString();
            }

            return response;
        },
        async (error) => {
            const correlationId = error.response?.headers['x-correlation-id'] || 'unknown';
            
            // Handle authentication errors
            if (error.response?.status === 401) {
                // Try to refresh token once
                try {
                    const refreshed = await tokenManager.refreshTokenIfNeeded();
                    if (refreshed && error.config && !error.config._retry) {
                        error.config._retry = true;
                        error.config.headers['Authorization'] = `Bearer ${tokenManager.getToken()}`;
                        return client.request(error.config);
                    }
                } catch (refreshError) {
                    console.error('Token refresh failed:', refreshError);
                }
                
                // Clear invalid token and redirect to login
                tokenManager.removeToken();
                window.location.href = '/login';
                return Promise.reject(error);
            }

            // Handle session timeout
            if (error.response?.status === 440) {
                sessionStorage.removeItem('carddemo-session-id');
                window.location.href = '/login?reason=session-timeout';
                return Promise.reject(error);
            }

            // Transform error to standard format
            const transformedError = handleApiError(error, correlationId);
            return Promise.reject(transformedError);
        }
    );

    return client;
}

/**
 * Default API client instance with CardDemo configuration
 * Pre-configured with authentication, error handling, and distributed tracing
 */
const defaultClient = createApiClient();

/**
 * Main API client object providing all HTTP methods with CardDemo-specific enhancements
 * Implements CICS transaction equivalent REST operations with identical data handling
 */
export const apiClient = {
    /**
     * User authentication - equivalent to COSGN00 transaction
     * @param {Object} credentials - User credentials {userId, password}
     * @returns {Promise<ApiResponse>} Authentication response with JWT token
     */
    async login(credentials) {
        try {
            const response = await defaultClient.post(
                TransactionConstants.TRANSACTION_ENDPOINTS.LOGIN,
                {
                    userId: DataFormatting.formatPicX(credentials.userId, 8).trim(),
                    password: credentials.password,
                    timestamp: new Date().toISOString()
                }
            );

            // Store JWT token and session information
            if (response.data.token) {
                tokenManager.setToken(response.data.token);
            }

            return response.data;
        } catch (error) {
            throw handleApiError(error, 'login-request');
        }
    },

    /**
     * User logout - clears authentication state
     * @returns {Promise<ApiResponse>} Logout confirmation
     */
    async logout() {
        try {
            const response = await defaultClient.post('/api/auth/logout');
            
            // Clear all authentication and session data
            tokenManager.removeToken();
            sessionStorage.removeItem('carddemo-session-id');
            sessionStorage.clear();
            
            return response.data;
        } catch (error) {
            // Even if logout fails, clear local state
            tokenManager.removeToken();
            sessionStorage.clear();
            throw handleApiError(error, 'logout-request');
        }
    },

    /**
     * Generic GET request with CICS-equivalent data formatting
     * @param {string} url - API endpoint URL
     * @param {Object} params - Query parameters
     * @returns {Promise<ApiResponse>} GET response data
     */
    async get(url, params = {}) {
        try {
            const response = await defaultClient.get(url, { params });
            return response.data;
        } catch (error) {
            throw handleApiError(error, `get-${url}`);
        }
    },

    /**
     * Generic POST request with COMMAREA to JSON conversion
     * @param {string} url - API endpoint URL  
     * @param {Object} data - Request payload data
     * @returns {Promise<ApiResponse>} POST response data
     */
    async post(url, data = {}) {
        try {
            const response = await defaultClient.post(url, data);
            return response.data;
        } catch (error) {
            throw handleApiError(error, `post-${url}`);
        }
    },

    /**
     * Generic PUT request for update operations
     * @param {string} url - API endpoint URL
     * @param {Object} data - Update payload data  
     * @returns {Promise<ApiResponse>} PUT response data
     */
    async put(url, data = {}) {
        try {
            const response = await defaultClient.put(url, data);
            return response.data;
        } catch (error) {
            throw handleApiError(error, `put-${url}`);
        }
    },

    /**
     * Generic DELETE request for deletion operations
     * @param {string} url - API endpoint URL
     * @returns {Promise<ApiResponse>} DELETE response confirmation
     */
    async delete(url) {
        try {
            const response = await defaultClient.delete(url);
            return response.data;
        } catch (error) {
            throw handleApiError(error, `delete-${url}`);
        }
    },

    /**
     * Sets authentication token for subsequent requests
     * @param {string} token - JWT authentication token
     */
    setAuthToken(token) {
        tokenManager.setToken(token);
    },

    /**
     * Clears authentication token and session data
     */
    clearAuthToken() {
        tokenManager.removeToken();
        sessionStorage.removeItem('carddemo-session-id');
    },

    /**
     * Refreshes JWT token if needed
     * @returns {Promise<boolean>} True if token was refreshed successfully
     */
    async refreshToken() {
        return await tokenManager.refreshTokenIfNeeded();
    }
};

/**
 * Authentication API endpoints - equivalent to COSGN00 transaction processing
 * Handles JWT token management and user authentication with RACF-equivalent security
 */
export const authenticationApi = {
    /**
     * User login with CICS-equivalent authentication
     * @param {Object} credentials - {userId, password}
     * @returns {Promise<ApiResponse>} Authentication response
     */
    async login(credentials) {
        return await apiClient.login(credentials);
    },

    /**
     * User logout with session cleanup
     * @returns {Promise<ApiResponse>} Logout confirmation
     */
    async logout() {
        return await apiClient.logout();
    },

    /**
     * Refresh expired JWT token
     * @returns {Promise<ApiResponse>} New token response
     */
    async refreshToken() {
        try {
            const response = await defaultClient.post('/api/auth/refresh', {
                refreshToken: localStorage.getItem('carddemo-refresh-token')
            });

            if (response.data.token) {
                tokenManager.setToken(response.data.token);
            }

            return response.data;
        } catch (error) {
            throw handleApiError(error, 'token-refresh');
        }
    },

    /**
     * Validate current JWT token
     * @returns {Promise<ApiResponse>} Token validation result
     */
    async validateToken() {
        try {
            const response = await defaultClient.get('/api/auth/validate');
            return response.data;
        } catch (error) {
            throw handleApiError(error, 'token-validation');
        }
    }
};

/**
 * Account management API - equivalent to COACTVW, COACTUP transactions
 * Handles account viewing and updates with exact COBOL data precision
 */
export const accountApi = {
    /**
     * Get account details - equivalent to COACTVW transaction
     * @param {string} accountId - 11-digit account number
     * @returns {Promise<ApiResponse>} Account details with COBOL formatting
     */
    async getAccountDetails(accountId) {
        try {
            const formattedAccountId = DataFormatting.formatAccountNumber(accountId);
            const response = await defaultClient.get(
                `${TransactionConstants.API_BASE_PATHS.ACCOUNT_SERVICE}/view/${formattedAccountId}`
            );

            // Transform financial data maintaining COBOL precision
            if (response.data.accountData) {
                const data = response.data.accountData;
                data.creditLimit = DataFormatting.parseCobolDecimal(data.creditLimit, 10, 2);
                data.currentBalance = DataFormatting.parseCobolDecimal(data.currentBalance, 10, 2);
                data.cashCreditLimit = DataFormatting.parseCobolDecimal(data.cashCreditLimit, 10, 2);
            }

            return response.data;
        } catch (error) {
            throw handleApiError(error, 'account-details');
        }
    },

    /**
     * Update account information - equivalent to COACTUP transaction
     * @param {string} accountId - Account identifier
     * @param {Object} accountData - Updated account information
     * @returns {Promise<ApiResponse>} Update confirmation
     */
    async updateAccount(accountId, accountData) {
        try {
            const formattedAccountId = DataFormatting.formatAccountNumber(accountId);
            
            // Format financial fields with COBOL precision
            const formattedData = {
                ...accountData,
                creditLimit: accountData.creditLimit ? 
                    DataFormatting.formatPicS9V9(accountData.creditLimit, 10, 2) : undefined,
                cashCreditLimit: accountData.cashCreditLimit ?
                    DataFormatting.formatPicS9V9(accountData.cashCreditLimit, 10, 2) : undefined
            };

            const response = await defaultClient.put(
                `${TransactionConstants.API_BASE_PATHS.ACCOUNT_SERVICE}/update/${formattedAccountId}`,
                formattedData
            );

            return response.data;
        } catch (error) {
            throw handleApiError(error, 'account-update');
        }
    },

    /**
     * Get paginated account list
     * @param {Object} params - Query parameters {page, size, sortBy}
     * @returns {Promise<PaginatedResponse>} Account list with pagination
     */
    async getAccountList(params = {}) {
        try {
            const response = await defaultClient.get(
                `${TransactionConstants.API_BASE_PATHS.ACCOUNT_SERVICE}/list`,
                { params }
            );
            return response.data;
        } catch (error) {
            throw handleApiError(error, 'account-list');
        }
    },

    /**
     * Search accounts by criteria
     * @param {Object} criteria - Search criteria
     * @returns {Promise<PaginatedResponse>} Search results
     */
    async searchAccounts(criteria) {
        try {
            const response = await defaultClient.post(
                `${TransactionConstants.API_BASE_PATHS.ACCOUNT_SERVICE}/search`,
                criteria
            );
            return response.data;
        } catch (error) {
            throw handleApiError(error, 'account-search');
        }
    }
};

/**
 * Transaction processing API - equivalent to COTRN00, COTRN01, COTRN02 transactions  
 * Handles transaction viewing, searching, and creation with exact decimal precision
 */
export const transactionApi = {
    /**
     * Get transaction history - equivalent to COTRN00 transaction
     * @param {string} accountId - Account identifier
     * @param {Object} params - Query parameters {page, size, startDate, endDate}
     * @returns {Promise<PaginatedResponse>} Transaction history with pagination
     */
    async getTransactionHistory(accountId, params = {}) {
        try {
            const formattedAccountId = DataFormatting.formatAccountNumber(accountId);
            const response = await defaultClient.get(
                `${TransactionConstants.API_BASE_PATHS.TRANSACTION_SERVICE}/history/${formattedAccountId}`,
                { params }
            );

            // Format transaction amounts with COBOL precision
            if (response.data.data && Array.isArray(response.data.data)) {
                response.data.data.forEach(transaction => {
                    if (transaction.amount) {
                        transaction.formattedAmount = DataFormatting.formatCurrency(transaction.amount);
                    }
                });
            }

            return response.data;
        } catch (error) {
            throw handleApiError(error, 'transaction-history');
        }
    },

    /**
     * Add new transaction - equivalent to COTRN02 transaction
     * @param {Object} transactionData - Transaction details
     * @returns {Promise<ApiResponse>} Transaction creation confirmation
     */
    async addTransaction(transactionData) {
        try {
            // Format financial data maintaining COBOL precision
            const formattedData = {
                ...transactionData,
                amount: DataFormatting.formatPicS9V9(transactionData.amount, 10, 2),
                accountId: DataFormatting.formatAccountNumber(transactionData.accountId),
                cardNumber: DataFormatting.formatCardNumber(transactionData.cardNumber),
                transactionDate: DataFormatting.formatToCcyymmdd(new Date(transactionData.transactionDate))
            };

            const response = await defaultClient.post(
                `${TransactionConstants.API_BASE_PATHS.TRANSACTION_SERVICE}/add`,
                formattedData
            );

            return response.data;
        } catch (error) {
            throw handleApiError(error, 'transaction-add');
        }
    },

    /**
     * Search transactions - equivalent to COTRN01 transaction
     * @param {Object} searchCriteria - Search parameters
     * @returns {Promise<PaginatedResponse>} Search results
     */
    async searchTransactions(searchCriteria) {
        try {
            const response = await defaultClient.post(
                `${TransactionConstants.API_BASE_PATHS.TRANSACTION_SERVICE}/search`,
                searchCriteria
            );

            // Format search results
            if (response.data.data && Array.isArray(response.data.data)) {
                response.data.data.forEach(transaction => {
                    if (transaction.amount) {
                        transaction.formattedAmount = DataFormatting.formatCurrency(transaction.amount);
                    }
                });
            }

            return response.data;
        } catch (error) {
            throw handleApiError(error, 'transaction-search');
        }
    },

    /**
     * Get specific transaction details
     * @param {string} transactionId - Transaction identifier
     * @returns {Promise<ApiResponse>} Transaction details
     */
    async getTransactionDetails(transactionId) {
        try {
            const response = await defaultClient.get(
                `${TransactionConstants.API_BASE_PATHS.TRANSACTION_SERVICE}/details/${transactionId}`
            );

            // Format transaction amount
            if (response.data.transaction && response.data.transaction.amount) {
                response.data.transaction.formattedAmount = 
                    DataFormatting.formatCurrency(response.data.transaction.amount);
            }

            return response.data;
        } catch (error) {
            throw handleApiError(error, 'transaction-details');
        }
    }
};

/**
 * Comprehensive error handler that transforms API errors to BMS-equivalent format
 * Maps HTTP status codes and Spring Boot error responses to original COBOL error patterns
 * 
 * @param {Error} error - Axios error object
 * @param {string} context - Error context for debugging
 * @returns {ErrorResponse} Standardized error response
 */
export function handleApiError(error, context = 'unknown') {
    let errorResponse = {
        error: {
            category: 'SYSTEM',
            type: 'UNKNOWN_ERROR',
            severity: 'MEDIUM'
        },
        code: 'UNKNOWN_ERROR',
        message: MessageConstants.API_ERROR_MESSAGES.SERVER_ERROR,
        details: {
            description: 'An unexpected error occurred',
            context: { requestContext: context }
        },
        timestamp: new Date().toISOString()
    };

    // Handle Axios errors
    if (axios.isAxiosError(error)) {
        const response = error.response;
        const request = error.request;

        if (response) {
            // Server responded with error status
            errorResponse.error.category = getErrorCategory(response.status);
            errorResponse.code = response.data?.code || `HTTP_${response.status}`;
            errorResponse.details.description = response.data?.message || response.statusText;
            errorResponse.details.context = {
                ...errorResponse.details.context,
                status: response.status,
                url: response.config?.url,
                method: response.config?.method
            };

            // Map specific HTTP status codes to BMS-equivalent messages
            switch (response.status) {
                case 400:
                    errorResponse.message = MessageConstants.VALIDATION_ERRORS.INVALID_FORMAT;
                    errorResponse.error.type = 'VALIDATION_ERROR';
                    break;
                case 401:
                    errorResponse.message = MessageConstants.API_ERROR_MESSAGES.AUTHENTICATION_FAILED;
                    errorResponse.error.type = 'AUTHENTICATION_FAILED';
                    errorResponse.error.severity = 'HIGH';
                    break;
                case 403:
                    errorResponse.message = MessageConstants.API_ERROR_MESSAGES.AUTHORIZATION_DENIED;
                    errorResponse.error.type = 'AUTHORIZATION_DENIED';
                    errorResponse.error.severity = 'HIGH';
                    break;
                case 404:
                    errorResponse.message = 'The requested resource was not found';
                    errorResponse.error.type = 'RESOURCE_NOT_FOUND';
                    break;
                case 408:
                case 504:
                    errorResponse.message = MessageConstants.API_ERROR_MESSAGES.TIMEOUT_ERROR;
                    errorResponse.error.type = 'TIMEOUT_ERROR';
                    break;
                case 500:
                case 502:
                case 503:
                    errorResponse.message = MessageConstants.API_ERROR_MESSAGES.SERVER_ERROR;
                    errorResponse.error.type = 'SERVER_ERROR';
                    errorResponse.error.severity = 'CRITICAL';
                    break;
                default:
                    errorResponse.message = `Server error: ${response.status}`;
            }

            // Extract Spring Boot error details if available
            if (response.data && typeof response.data === 'object') {
                if (response.data.fieldErrors) {
                    errorResponse.details.fieldErrors = response.data.fieldErrors;
                }
                if (response.data.businessStatus) {
                    errorResponse.details.businessStatus = response.data.businessStatus;
                }
            }

        } else if (request) {
            // Network error - no response received
            errorResponse.error.category = 'NETWORK';
            errorResponse.error.type = 'NETWORK_ERROR';
            errorResponse.error.severity = 'HIGH';
            errorResponse.code = 'NETWORK_ERROR';
            errorResponse.message = MessageConstants.API_ERROR_MESSAGES.NETWORK_ERROR;
            errorResponse.details.description = 'Network request failed - no response received';
        } else {
            // Request setup error
            errorResponse.error.type = 'REQUEST_SETUP_ERROR';
            errorResponse.code = 'REQUEST_SETUP_ERROR';
            errorResponse.message = 'Error setting up the request';
            errorResponse.details.description = error.message;
        }
    } else {
        // Non-Axios error
        errorResponse.error.type = 'CLIENT_ERROR';
        errorResponse.code = 'CLIENT_ERROR';
        errorResponse.message = error.message || 'An unexpected client error occurred';
        errorResponse.details.description = error.stack || error.toString();
    }

    // Log error for debugging
    console.error('API Error:', {
        context,
        error: errorResponse,
        originalError: error
    });

    return errorResponse;
}

/**
 * Maps HTTP status codes to error categories
 * @private
 */
function getErrorCategory(status) {
    if (status >= 400 && status < 500) {
        if (status === 401 || status === 403) return 'SECURITY';
        if (status === 422) return 'VALIDATION';
        return 'BUSINESS';
    }
    if (status >= 500) return 'SYSTEM';
    return 'UNKNOWN';
}

/**
 * JWT token management utilities with automatic refresh and validation
 * Handles token storage, expiration checking, and automatic renewal
 */
export const tokenManager = {
    /**
     * Sets JWT token in secure storage
     * @param {string} token - JWT token string
     */
    setToken(token) {
        if (!token) return;
        
        try {
            // Store token securely
            localStorage.setItem('carddemo-auth-token', token);
            
            // Decode and store expiration info
            const decoded = jwt.decode(token);
            if (decoded && decoded.exp) {
                localStorage.setItem('carddemo-token-exp', decoded.exp.toString());
                
                // Store user information for session management
                if (decoded.userId) {
                    sessionStorage.setItem('carddemo-user-id', decoded.userId);
                }
                if (decoded.roles) {
                    sessionStorage.setItem('carddemo-user-roles', JSON.stringify(decoded.roles));
                }
            }
        } catch (error) {
            console.error('Error storing token:', error);
        }
    },

    /**
     * Gets current JWT token from storage
     * @returns {string|null} JWT token or null if not available
     */
    getToken() {
        return localStorage.getItem('carddemo-auth-token');
    },

    /**
     * Removes JWT token and clears related session data
     */
    removeToken() {
        localStorage.removeItem('carddemo-auth-token');
        localStorage.removeItem('carddemo-token-exp');
        localStorage.removeItem('carddemo-refresh-token');
        sessionStorage.removeItem('carddemo-user-id');
        sessionStorage.removeItem('carddemo-user-roles');
        sessionStorage.removeItem('carddemo-session-id');
    },

    /**
     * Checks if current token is valid and not expired
     * @returns {boolean} True if token is valid
     */
    isTokenValid() {
        const token = this.getToken();
        if (!token) return false;

        try {
            const expiry = localStorage.getItem('carddemo-token-exp');
            if (!expiry) return false;

            const expiryTime = parseInt(expiry, 10) * 1000; // Convert to milliseconds
            const currentTime = Date.now();
            const bufferTime = 5 * 60 * 1000; // 5 minute buffer

            return currentTime < (expiryTime - bufferTime);
        } catch (error) {
            console.error('Error validating token:', error);
            return false;
        }
    },

    /**
     * Gets token expiration time
     * @returns {Date|null} Expiration date or null
     */
    getTokenExpiry() {
        try {
            const expiry = localStorage.getItem('carddemo-token-exp');
            if (!expiry) return null;
            
            return new Date(parseInt(expiry, 10) * 1000);
        } catch (error) {
            console.error('Error getting token expiry:', error);
            return null;
        }
    },

    /**
     * Automatically refreshes token if needed
     * @returns {Promise<boolean>} True if token was refreshed successfully
     */
    async refreshTokenIfNeeded() {
        if (this.isTokenValid()) {
            return true; // Token is still valid
        }

        try {
            const refreshToken = localStorage.getItem('carddemo-refresh-token');
            if (!refreshToken) {
                return false; // No refresh token available
            }

            const response = await authenticationApi.refreshToken();
            if (response.token) {
                this.setToken(response.token);
                return true;
            }

            return false;
        } catch (error) {
            console.error('Token refresh failed:', error);
            this.removeToken(); // Clear invalid tokens
            return false;
        }
    }
};

// Export default apiClient for convenient usage
export default apiClient;