/**
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * CardDemo API Client
 * 
 * This module provides comprehensive HTTP communication utilities for React components
 * to interact with Spring Boot microservices through Spring Cloud Gateway. It maintains
 * exact functional equivalence with original CICS transaction boundaries while implementing
 * modern REST API patterns with JWT authentication and distributed session management.
 * 
 * Key Features:
 * - JWT token management with automatic refresh capabilities
 * - Request/response interceptors for session management and distributed tracing
 * - COMMAREA data structure conversion to JSON DTOs with exact data integrity
 * - Error handling that translates Spring Boot errors to BMS-equivalent display patterns
 * - API wrapper functions mapping original CICS transaction codes to REST endpoints
 * - Consistent transaction boundary preservation equivalent to CICS syncpoint behavior
 * 
 * Architecture:
 * - Axios HTTP client with interceptors for authentication and error handling
 * - Token manager for secure JWT storage and refresh token processing
 * - API wrappers for each business domain (authentication, accounts, transactions)
 * - Error handling system that maps Spring Boot errors to COMMAREA-equivalent patterns
 * - Data formatting utilities for COBOL-to-JSON conversion with exact precision
 */

import axios from 'axios';
import jwt from 'jsonwebtoken';
import { TransactionConstants } from '../constants/TransactionConstants';
import { MessageConstants } from '../constants/MessageConstants';
import { ApiTypes } from '../types/ApiTypes';
import { DataFormatting } from './dataFormatting';

// ============================================================================
// CONFIGURATION AND CONSTANTS
// ============================================================================

/**
 * API Client Configuration
 * 
 * Base configuration for all HTTP requests including timeout settings,
 * retry policies, and header management that preserve CICS transaction
 * processing characteristics while enabling modern REST API communication.
 */
const API_CONFIG = {
  // Base URL for all API requests - Spring Cloud Gateway endpoint
  baseURL: process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080',
  
  // Request timeout (30 seconds) - matches CICS transaction timeout
  timeout: 30000,
  
  // Retry configuration for network failures
  maxRetries: 3,
  retryDelay: 1000,
  
  // Authentication configuration
  authTokenHeader: 'Authorization',
  authTokenPrefix: 'Bearer',
  
  // Session management configuration
  sessionIdHeader: 'X-Session-ID',
  correlationIdHeader: 'X-Correlation-ID',
  
  // API versioning
  apiVersion: 'v1',
  apiVersionHeader: 'X-API-Version',
  
  // Response type configuration
  responseType: 'json',
  
  // Request headers
  defaultHeaders: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'X-Requested-With': 'XMLHttpRequest'
  }
};

/**
 * Token Storage Configuration
 * 
 * Secure token storage configuration using sessionStorage for security
 * and localStorage for persistence, matching CICS session behavior.
 */
const TOKEN_CONFIG = {
  // Storage keys for JWT tokens
  accessTokenKey: 'carddemo_access_token',
  refreshTokenKey: 'carddemo_refresh_token',
  
  // Token expiration buffer (5 minutes before actual expiration)
  expirationBufferMs: 5 * 60 * 1000,
  
  // Default token storage (sessionStorage for security)
  defaultStorage: sessionStorage,
  
  // Refresh token storage (localStorage for persistence)
  refreshTokenStorage: localStorage
};

// ============================================================================
// AXIOS INSTANCE CREATION AND CONFIGURATION
// ============================================================================

/**
 * Create configured Axios instance with interceptors
 * 
 * Creates the main HTTP client with request/response interceptors for:
 * - Authentication header injection
 * - Correlation ID tracking
 * - Session management
 * - Error handling and retry logic
 * - Response data transformation
 * 
 * @param {Object} config - Additional configuration options
 * @returns {Object} Configured Axios instance
 */
export function createApiClient(config = {}) {
  // Create Axios instance with base configuration
  const client = axios.create({
    ...API_CONFIG,
    ...config
  });

  // Request interceptor for authentication and session management
  client.interceptors.request.use(
    async (requestConfig) => {
      try {
        // Generate correlation ID for distributed tracing
        const correlationId = generateCorrelationId();
        
        // Add correlation ID to headers
        requestConfig.headers[API_CONFIG.correlationIdHeader] = correlationId;
        
        // Add API version header
        requestConfig.headers[API_CONFIG.apiVersionHeader] = API_CONFIG.apiVersion;
        
        // Add session ID if available
        const sessionId = getSessionId();
        if (sessionId) {
          requestConfig.headers[API_CONFIG.sessionIdHeader] = sessionId;
        }
        
        // Add authentication token if available
        const token = tokenManager.getToken();
        if (token) {
          // Check if token needs refresh
          if (tokenManager.isTokenValid()) {
            requestConfig.headers[API_CONFIG.authTokenHeader] = 
              `${API_CONFIG.authTokenPrefix} ${token}`;
          } else {
            // Attempt token refresh
            try {
              await tokenManager.refreshTokenIfNeeded();
              const refreshedToken = tokenManager.getToken();
              if (refreshedToken) {
                requestConfig.headers[API_CONFIG.authTokenHeader] = 
                  `${API_CONFIG.authTokenPrefix} ${refreshedToken}`;
              }
            } catch (refreshError) {
              console.error('Token refresh failed:', refreshError);
              // Clear invalid tokens
              tokenManager.removeToken();
              // Redirect to login if needed
              throw new Error(MessageConstants.API_ERROR_MESSAGES.SESSION_EXPIRED);
            }
          }
        }
        
        // Add request timestamp for audit logging
        requestConfig.metadata = {
          startTime: new Date(),
          correlationId: correlationId
        };
        
        return requestConfig;
      } catch (error) {
        return Promise.reject(error);
      }
    },
    (error) => {
      return Promise.reject(error);
    }
  );

  // Response interceptor for error handling and data transformation
  client.interceptors.response.use(
    (response) => {
      try {
        // Add response timing information
        if (response.config.metadata) {
          response.config.metadata.endTime = new Date();
          response.config.metadata.duration = 
            response.config.metadata.endTime - response.config.metadata.startTime;
        }
        
        // Transform response data if needed
        if (response.data && typeof response.data === 'object') {
          // Add screen data if response contains BMS-equivalent fields
          if (response.data.screenData) {
            response.data.screenData = transformScreenData(response.data.screenData);
          }
          
          // Format financial data using COBOL precision
          if (response.data.data) {
            response.data.data = transformFinancialData(response.data.data);
          }
        }
        
        return response;
      } catch (error) {
        console.error('Response transformation error:', error);
        return response;
      }
    },
    async (error) => {
      // Handle different types of errors
      const originalRequest = error.config;
      
      // Handle network errors
      if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
        return Promise.reject(createNetworkError(
          MessageConstants.API_ERROR_MESSAGES.TIMEOUT_ERROR,
          'TIMEOUT'
        ));
      }
      
      // Handle authentication errors
      if (error.response?.status === 401) {
        // Try to refresh token if not already retrying
        if (!originalRequest._retry) {
          originalRequest._retry = true;
          
          try {
            await tokenManager.refreshTokenIfNeeded();
            const newToken = tokenManager.getToken();
            
            if (newToken) {
              originalRequest.headers[API_CONFIG.authTokenHeader] = 
                `${API_CONFIG.authTokenPrefix} ${newToken}`;
              return client(originalRequest);
            }
          } catch (refreshError) {
            console.error('Token refresh failed:', refreshError);
            tokenManager.removeToken();
            return Promise.reject(createAuthenticationError(
              MessageConstants.API_ERROR_MESSAGES.SESSION_EXPIRED,
              'SESSION_EXPIRED'
            ));
          }
        }
        
        return Promise.reject(createAuthenticationError(
          MessageConstants.API_ERROR_MESSAGES.AUTHENTICATION_FAILED,
          'AUTHENTICATION_FAILED'
        ));
      }
      
      // Handle authorization errors
      if (error.response?.status === 403) {
        return Promise.reject(createAuthorizationError(
          MessageConstants.API_ERROR_MESSAGES.AUTHORIZATION_DENIED,
          'AUTHORIZATION_DENIED'
        ));
      }
      
      // Handle server errors
      if (error.response?.status >= 500) {
        return Promise.reject(createServerError(
          MessageConstants.API_ERROR_MESSAGES.SERVER_ERROR,
          'SERVER_ERROR'
        ));
      }
      
      // Handle validation errors
      if (error.response?.status === 400 || error.response?.status === 422) {
        return Promise.reject(createValidationError(
          error.response.data?.message || MessageConstants.API_ERROR_MESSAGES.INVALID_REQUEST,
          'VALIDATION_ERROR',
          error.response.data?.fieldErrors
        ));
      }
      
      // Handle other errors
      return Promise.reject(handleApiError(error));
    }
  );

  return client;
}

// ============================================================================
// TOKEN MANAGEMENT SYSTEM
// ============================================================================

/**
 * Token Manager
 * 
 * Comprehensive JWT token management system that handles:
 * - Secure token storage with automatic cleanup
 * - Token validation and expiration checking
 * - Automatic token refresh with retry logic
 * - Session management equivalent to CICS pseudo-conversational processing
 */
export const tokenManager = {
  /**
   * Store authentication token securely
   * 
   * @param {string} token - JWT access token
   * @param {string} refreshToken - JWT refresh token (optional)
   * @param {Object} options - Storage options
   */
  setToken(token, refreshToken = null, options = {}) {
    try {
      if (!token) {
        throw new Error('Token is required');
      }
      
      // Validate token format
      if (!token.includes('.') || token.split('.').length !== 3) {
        throw new Error('Invalid JWT token format');
      }
      
      // Store access token in sessionStorage for security
      const storage = options.persistent ? localStorage : TOKEN_CONFIG.defaultStorage;
      storage.setItem(TOKEN_CONFIG.accessTokenKey, token);
      
      // Store refresh token in localStorage for persistence
      if (refreshToken) {
        TOKEN_CONFIG.refreshTokenStorage.setItem(TOKEN_CONFIG.refreshTokenKey, refreshToken);
      }
      
      // Store token metadata
      const tokenData = jwt.decode(token);
      if (tokenData) {
        const metadata = {
          userId: tokenData.sub,
          userRole: tokenData.role,
          issuedAt: tokenData.iat,
          expiresAt: tokenData.exp,
          sessionId: tokenData.sessionId
        };
        
        storage.setItem(TOKEN_CONFIG.accessTokenKey + '_metadata', JSON.stringify(metadata));
      }
      
    } catch (error) {
      console.error('Error storing token:', error);
      throw error;
    }
  },

  /**
   * Retrieve stored authentication token
   * 
   * @param {boolean} checkExpiration - Whether to check token expiration
   * @returns {string|null} JWT access token or null if not found/expired
   */
  getToken(checkExpiration = true) {
    try {
      const token = TOKEN_CONFIG.defaultStorage.getItem(TOKEN_CONFIG.accessTokenKey) ||
                   localStorage.getItem(TOKEN_CONFIG.accessTokenKey);
      
      if (!token) {
        return null;
      }
      
      // Check expiration if requested
      if (checkExpiration && !this.isTokenValid()) {
        this.removeToken();
        return null;
      }
      
      return token;
    } catch (error) {
      console.error('Error retrieving token:', error);
      return null;
    }
  },

  /**
   * Remove stored authentication tokens
   */
  removeToken() {
    try {
      // Remove from both storage locations
      TOKEN_CONFIG.defaultStorage.removeItem(TOKEN_CONFIG.accessTokenKey);
      TOKEN_CONFIG.defaultStorage.removeItem(TOKEN_CONFIG.accessTokenKey + '_metadata');
      localStorage.removeItem(TOKEN_CONFIG.accessTokenKey);
      localStorage.removeItem(TOKEN_CONFIG.accessTokenKey + '_metadata');
      TOKEN_CONFIG.refreshTokenStorage.removeItem(TOKEN_CONFIG.refreshTokenKey);
      
      // Clear session ID
      clearSessionId();
      
    } catch (error) {
      console.error('Error removing token:', error);
    }
  },

  /**
   * Check if current token is valid and not expired
   * 
   * @returns {boolean} True if token is valid and not expired
   */
  isTokenValid() {
    try {
      const token = TOKEN_CONFIG.defaultStorage.getItem(TOKEN_CONFIG.accessTokenKey) ||
                   localStorage.getItem(TOKEN_CONFIG.accessTokenKey);
      
      if (!token) {
        return false;
      }
      
      const tokenData = jwt.decode(token);
      if (!tokenData || !tokenData.exp) {
        return false;
      }
      
      // Check expiration with buffer
      const expirationTime = tokenData.exp * 1000; // Convert to milliseconds
      const currentTime = Date.now();
      const isExpired = currentTime >= (expirationTime - TOKEN_CONFIG.expirationBufferMs);
      
      return !isExpired;
    } catch (error) {
      console.error('Error validating token:', error);
      return false;
    }
  },

  /**
   * Get token expiration time
   * 
   * @returns {Date|null} Token expiration date or null if not available
   */
  getTokenExpiry() {
    try {
      const token = this.getToken(false);
      if (!token) {
        return null;
      }
      
      const tokenData = jwt.decode(token);
      if (!tokenData || !tokenData.exp) {
        return null;
      }
      
      return new Date(tokenData.exp * 1000);
    } catch (error) {
      console.error('Error getting token expiry:', error);
      return null;
    }
  },

  /**
   * Refresh authentication token if needed
   * 
   * @returns {Promise<string|null>} New access token or null if refresh failed
   */
  async refreshTokenIfNeeded() {
    try {
      // Check if refresh is needed
      if (this.isTokenValid()) {
        return this.getToken(false);
      }
      
      // Get refresh token
      const refreshToken = TOKEN_CONFIG.refreshTokenStorage.getItem(TOKEN_CONFIG.refreshTokenKey);
      if (!refreshToken) {
        throw new Error('No refresh token available');
      }
      
      // Call refresh endpoint
      const response = await axios.post(
        `${API_CONFIG.baseURL}${TransactionConstants.API_BASE_PATHS.AUTH_SERVICE}/refresh`,
        { refreshToken: refreshToken },
        {
          headers: {
            'Content-Type': 'application/json',
            [API_CONFIG.apiVersionHeader]: API_CONFIG.apiVersion
          }
        }
      );
      
      if (response.data && response.data.data) {
        const { accessToken, refreshToken: newRefreshToken } = response.data.data;
        
        // Store new tokens
        this.setToken(accessToken, newRefreshToken);
        
        return accessToken;
      }
      
      throw new Error('Invalid refresh response');
    } catch (error) {
      console.error('Token refresh failed:', error);
      this.removeToken();
      throw error;
    }
  }
};

// ============================================================================
// SESSION MANAGEMENT
// ============================================================================

/**
 * Generate correlation ID for distributed tracing
 * 
 * @returns {string} Unique correlation ID
 */
function generateCorrelationId() {
  return 'CARD-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
}

/**
 * Get current session ID
 * 
 * @returns {string|null} Session ID or null if not available
 */
function getSessionId() {
  try {
    const token = tokenManager.getToken(false);
    if (!token) {
      return null;
    }
    
    const tokenData = jwt.decode(token);
    return tokenData?.sessionId || null;
  } catch (error) {
    console.error('Error getting session ID:', error);
    return null;
  }
}

/**
 * Clear session ID
 */
function clearSessionId() {
  // Session ID is stored in token, so clearing token clears session
  // Additional session cleanup can be added here if needed
}

// ============================================================================
// DATA TRANSFORMATION UTILITIES
// ============================================================================

/**
 * Transform screen data for BMS compatibility
 * 
 * @param {Object} screenData - Raw screen data from API
 * @returns {Object} Transformed screen data
 */
function transformScreenData(screenData) {
  try {
    if (!screenData || typeof screenData !== 'object') {
      return screenData;
    }
    
    // Transform date/time fields to BMS format
    const transformed = { ...screenData };
    
    if (transformed.curdate) {
      transformed.curdate = DataFormatting.formatDate(transformed.curdate, 'MM/DD/YY');
    }
    
    if (transformed.curtime) {
      const timeDate = new Date(transformed.curtime);
      transformed.curtime = timeDate.toLocaleTimeString('en-US', {
        hour12: false,
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      });
    }
    
    // Ensure field lengths match BMS definitions
    if (transformed.trnname) {
      transformed.trnname = DataFormatting.formatPicX(transformed.trnname, 4);
    }
    
    if (transformed.pgmname) {
      transformed.pgmname = DataFormatting.formatPicX(transformed.pgmname, 8);
    }
    
    if (transformed.title01) {
      transformed.title01 = DataFormatting.formatPicX(transformed.title01, 40);
    }
    
    if (transformed.title02) {
      transformed.title02 = DataFormatting.formatPicX(transformed.title02, 40);
    }
    
    return transformed;
  } catch (error) {
    console.error('Error transforming screen data:', error);
    return screenData;
  }
}

/**
 * Transform financial data with COBOL precision
 * 
 * @param {Object} data - Raw financial data from API
 * @returns {Object} Transformed financial data
 */
function transformFinancialData(data) {
  try {
    if (!data || typeof data !== 'object') {
      return data;
    }
    
    const transformed = { ...data };
    
    // Transform currency fields
    const currencyFields = [
      'accountCurrentBalance', 'creditLimit', 'cashCreditLimit',
      'currentCycleCredit', 'currentCycleDebit', 'amount',
      'transactionAmount', 'balance', 'paymentAmount'
    ];
    
    currencyFields.forEach(field => {
      if (transformed[field] !== undefined && transformed[field] !== null) {
        transformed[field] = DataFormatting.formatCurrency(transformed[field], 2, true, false);
      }
    });
    
    // Transform account numbers
    if (transformed.accountNumber) {
      transformed.accountNumber = DataFormatting.formatAccountNumber(transformed.accountNumber);
    }
    
    // Transform card numbers
    if (transformed.cardNumber) {
      transformed.cardNumber = DataFormatting.formatCardNumber(transformed.cardNumber, true);
    }
    
    // Transform SSN
    if (transformed.ssn) {
      transformed.ssn = DataFormatting.formatSSN(transformed.ssn, true);
    }
    
    // Transform dates
    const dateFields = ['dateOpened', 'expiryDate', 'reissueDate', 'dateOfBirth', 'transactionDate'];
    dateFields.forEach(field => {
      if (transformed[field]) {
        transformed[field] = DataFormatting.formatDate(transformed[field], 'MM/DD/YYYY');
      }
    });
    
    return transformed;
  } catch (error) {
    console.error('Error transforming financial data:', error);
    return data;
  }
}

// ============================================================================
// ERROR HANDLING SYSTEM
// ============================================================================

/**
 * Handle API errors with BMS-equivalent error display patterns
 * 
 * @param {Error} error - The error object
 * @returns {Object} Formatted error response
 */
export function handleApiError(error) {
  try {
    // Handle Axios errors
    if (axios.isAxiosError(error)) {
      const response = error.response;
      const request = error.request;
      
      // Network errors
      if (!response && request) {
        return createNetworkError(
          MessageConstants.API_ERROR_MESSAGES.NETWORK_ERROR,
          'NETWORK_ERROR'
        );
      }
      
      // Response errors
      if (response) {
        const status = response.status;
        const data = response.data;
        
        // Map HTTP status to business status codes
        const businessStatus = ApiTypes.HTTP_TO_BUSINESS_STATUS[status] || '99';
        
        // Extract error details
        let errorMessage = MessageConstants.API_ERROR_MESSAGES.SERVER_ERROR;
        let errorDetails = 'An unexpected error occurred';
        let fieldErrors = [];
        
        if (data && typeof data === 'object') {
          if (data.message) {
            errorMessage = data.message;
          }
          
          if (data.details) {
            errorDetails = data.details;
          }
          
          if (data.error && data.error.fieldErrors) {
            fieldErrors = data.error.fieldErrors;
          }
        }
        
        return {
          error: {
            type: getErrorType(status),
            code: `HTTP_${status}`,
            details: errorDetails,
            fieldErrors: fieldErrors
          },
          code: businessStatus,
          message: errorMessage,
          details: errorDetails,
          timestamp: new Date(),
          correlationId: error.config?.metadata?.correlationId
        };
      }
    }
    
    // Handle other errors
    return {
      error: {
        type: 'system',
        code: 'UNKNOWN_ERROR',
        details: error.message || 'An unknown error occurred'
      },
      code: '99',
      message: MessageConstants.API_ERROR_MESSAGES.SERVER_ERROR,
      details: error.message || 'An unknown error occurred',
      timestamp: new Date()
    };
  } catch (handlingError) {
    console.error('Error handling API error:', handlingError);
    return createSystemError(
      MessageConstants.API_ERROR_MESSAGES.SERVER_ERROR,
      'ERROR_HANDLING_FAILED'
    );
  }
}

/**
 * Create network error response
 * 
 * @param {string} message - Error message
 * @param {string} code - Error code
 * @returns {Object} Network error response
 */
function createNetworkError(message, code) {
  return {
    error: {
      type: 'network',
      code: code,
      details: 'Network connection failed'
    },
    code: '99',
    message: message,
    details: 'Network connection failed',
    timestamp: new Date()
  };
}

/**
 * Create authentication error response
 * 
 * @param {string} message - Error message
 * @param {string} code - Error code
 * @returns {Object} Authentication error response
 */
function createAuthenticationError(message, code) {
  return {
    error: {
      type: 'authentication',
      code: code,
      details: 'Authentication failed'
    },
    code: '99',
    message: message,
    details: 'Authentication failed',
    timestamp: new Date()
  };
}

/**
 * Create authorization error response
 * 
 * @param {string} message - Error message
 * @param {string} code - Error code
 * @returns {Object} Authorization error response
 */
function createAuthorizationError(message, code) {
  return {
    error: {
      type: 'authorization',
      code: code,
      details: 'Authorization denied'
    },
    code: '99',
    message: message,
    details: 'Authorization denied',
    timestamp: new Date()
  };
}

/**
 * Create server error response
 * 
 * @param {string} message - Error message
 * @param {string} code - Error code
 * @returns {Object} Server error response
 */
function createServerError(message, code) {
  return {
    error: {
      type: 'system',
      code: code,
      details: 'Server error occurred'
    },
    code: '99',
    message: message,
    details: 'Server error occurred',
    timestamp: new Date()
  };
}

/**
 * Create validation error response
 * 
 * @param {string} message - Error message
 * @param {string} code - Error code
 * @param {Array} fieldErrors - Field-specific errors
 * @returns {Object} Validation error response
 */
function createValidationError(message, code, fieldErrors = []) {
  return {
    error: {
      type: 'validation',
      code: code,
      details: 'Validation failed',
      fieldErrors: fieldErrors
    },
    code: '98',
    message: message,
    details: 'Validation failed',
    timestamp: new Date()
  };
}

/**
 * Create system error response
 * 
 * @param {string} message - Error message
 * @param {string} code - Error code
 * @returns {Object} System error response
 */
function createSystemError(message, code) {
  return {
    error: {
      type: 'system',
      code: code,
      details: 'System error occurred'
    },
    code: '99',
    message: message,
    details: 'System error occurred',
    timestamp: new Date()
  };
}

/**
 * Get error type based on HTTP status code
 * 
 * @param {number} status - HTTP status code
 * @returns {string} Error type
 */
function getErrorType(status) {
  if (status === 401) return 'authentication';
  if (status === 403) return 'authorization';
  if (status === 404) return 'not_found';
  if (status === 409) return 'conflict';
  if (status === 422 || status === 400) return 'validation';
  if (status === 408) return 'timeout';
  if (status >= 500) return 'system';
  return 'system';
}

// ============================================================================
// MAIN API CLIENT INSTANCE
// ============================================================================

/**
 * Main API client instance
 * 
 * Pre-configured Axios instance with all interceptors and middleware
 * for communication with Spring Boot microservices.
 */
const mainApiClient = createApiClient();

/**
 * Generic API client object with common HTTP methods
 * 
 * Provides standard HTTP methods (GET, POST, PUT, DELETE) with
 * consistent error handling and response transformation.
 */
const apiClient = {
  /**
   * Perform GET request
   * 
   * @param {string} url - Request URL
   * @param {Object} config - Request configuration
   * @returns {Promise<Object>} Response data
   */
  async get(url, config = {}) {
    try {
      const response = await mainApiClient.get(url, config);
      return response.data;
    } catch (error) {
      throw handleApiError(error);
    }
  },

  /**
   * Perform POST request
   * 
   * @param {string} url - Request URL
   * @param {Object} data - Request data
   * @param {Object} config - Request configuration
   * @returns {Promise<Object>} Response data
   */
  async post(url, data = {}, config = {}) {
    try {
      const response = await mainApiClient.post(url, data, config);
      return response.data;
    } catch (error) {
      throw handleApiError(error);
    }
  },

  /**
   * Perform PUT request
   * 
   * @param {string} url - Request URL
   * @param {Object} data - Request data
   * @param {Object} config - Request configuration
   * @returns {Promise<Object>} Response data
   */
  async put(url, data = {}, config = {}) {
    try {
      const response = await mainApiClient.put(url, data, config);
      return response.data;
    } catch (error) {
      throw handleApiError(error);
    }
  },

  /**
   * Perform DELETE request
   * 
   * @param {string} url - Request URL
   * @param {Object} config - Request configuration
   * @returns {Promise<Object>} Response data
   */
  async delete(url, config = {}) {
    try {
      const response = await mainApiClient.delete(url, config);
      return response.data;
    } catch (error) {
      throw handleApiError(error);
    }
  },

  /**
   * Set authentication token
   * 
   * @param {string} token - JWT access token
   * @param {string} refreshToken - JWT refresh token
   */
  setAuthToken(token, refreshToken = null) {
    tokenManager.setToken(token, refreshToken);
  },

  /**
   * Clear authentication token
   */
  clearAuthToken() {
    tokenManager.removeToken();
  },

  /**
   * Refresh authentication token
   * 
   * @returns {Promise<string>} New access token
   */
  async refreshToken() {
    return await tokenManager.refreshTokenIfNeeded();
  },

  /**
   * User login
   * 
   * @param {string} userId - User ID (8 characters)
   * @param {string} password - Password (8 characters)
   * @returns {Promise<Object>} Login response
   */
  async login(userId, password) {
    try {
      const loginData = {
        userId: DataFormatting.formatPicX(userId, 8, false),
        password: DataFormatting.formatPicX(password, 8, false)
      };
      
      const response = await this.post(TransactionConstants.TRANSACTION_ENDPOINTS.LOGIN, loginData);
      
      // Store tokens if login successful
      if (response.data && response.data.accessToken) {
        this.setAuthToken(response.data.accessToken, response.data.refreshToken);
      }
      
      return response;
    } catch (error) {
      throw handleApiError(error);
    }
  },

  /**
   * User logout
   * 
   * @returns {Promise<Object>} Logout response
   */
  async logout() {
    try {
      const response = await this.post(`${TransactionConstants.API_BASE_PATHS.AUTH_SERVICE}/logout`);
      
      // Clear stored tokens
      this.clearAuthToken();
      
      return response;
    } catch (error) {
      // Clear tokens even if logout fails
      this.clearAuthToken();
      throw handleApiError(error);
    }
  }
};

// ============================================================================
// AUTHENTICATION API
// ============================================================================

/**
 * Authentication API wrapper
 * 
 * Provides authentication-specific API methods equivalent to
 * COSGN00 (Sign-on) transaction functionality.
 */
export const authenticationApi = {
  /**
   * User login with COSGN00 transaction equivalent
   * 
   * @param {string} userId - User ID (8 characters, as per BMS USERID field)
   * @param {string} password - Password (8 characters, as per BMS PASSWD field)
   * @returns {Promise<Object>} Login response with user role and session information
   */
  async login(userId, password) {
    try {
      // Validate input format to match BMS field specifications
      if (!userId || userId.length > 8) {
        throw new Error(MessageConstants.FIELD_ERROR_MESSAGES.USER_ID_INVALID);
      }
      
      if (!password || password.length > 8) {
        throw new Error(MessageConstants.FIELD_ERROR_MESSAGES.PASSWORD_INVALID);
      }
      
      // Format inputs to match COBOL field definitions
      const loginData = {
        userId: DataFormatting.formatPicX(userId.trim(), 8, false),
        password: DataFormatting.formatPicX(password.trim(), 8, false),
        transactionCode: TransactionConstants.TRANSACTION_CODES.COSGN00
      };
      
      const response = await apiClient.post(TransactionConstants.TRANSACTION_ENDPOINTS.LOGIN, loginData);
      
      // Store authentication tokens
      if (response.data && response.data.accessToken) {
        tokenManager.setToken(response.data.accessToken, response.data.refreshToken);
      }
      
      return response;
    } catch (error) {
      throw handleApiError(error);
    }
  },

  /**
   * User logout
   * 
   * @returns {Promise<Object>} Logout response
   */
  async logout() {
    try {
      const response = await apiClient.post(`${TransactionConstants.API_BASE_PATHS.AUTH_SERVICE}/logout`, {
        sessionId: getSessionId()
      });
      
      // Clear stored tokens
      tokenManager.removeToken();
      
      return response;
    } catch (error) {
      // Clear tokens even if logout fails
      tokenManager.removeToken();
      throw handleApiError(error);
    }
  },

  /**
   * Refresh authentication token
   * 
   * @returns {Promise<Object>} Refresh response with new tokens
   */
  async refreshToken() {
    try {
      const newToken = await tokenManager.refreshTokenIfNeeded();
      
      return {
        status: '00',
        message: MessageConstants.SUCCESS_MESSAGES.LOGIN_SUCCESS,
        data: {
          accessToken: newToken
        }
      };
    } catch (error) {
      throw handleApiError(error);
    }
  },

  /**
   * Validate current authentication token
   * 
   * @returns {Promise<Object>} Validation response
   */
  async validateToken() {
    try {
      const isValid = tokenManager.isTokenValid();
      
      if (!isValid) {
        throw new Error(MessageConstants.API_ERROR_MESSAGES.SESSION_EXPIRED);
      }
      
      const response = await apiClient.get(`${TransactionConstants.API_BASE_PATHS.AUTH_SERVICE}/validate`);
      
      return response;
    } catch (error) {
      throw handleApiError(error);
    }
  }
};

// ============================================================================
// ACCOUNT API
// ============================================================================

/**
 * Account API wrapper
 * 
 * Provides account management API methods equivalent to
 * COACTVW (Account View) and COACTUP (Account Update) transaction functionality.
 */
export const accountApi = {
  /**
   * Get account details with COACTVW transaction equivalent
   * 
   * @param {string} accountNumber - Account number (11 digits, as per BMS ACCTSID field)
   * @returns {Promise<Object>} Account details response
   */
  async getAccountDetails(accountNumber) {
    try {
      // Validate and format account number
      if (!accountNumber) {
        throw new Error(MessageConstants.FIELD_ERROR_MESSAGES.ACCOUNT_INVALID);
      }
      
      const formattedAccountNumber = DataFormatting.formatAccountNumber(accountNumber);
      
      const response = await apiClient.get(
        `${TransactionConstants.TRANSACTION_ENDPOINTS.ACCOUNT_VIEW}/${formattedAccountNumber}`,
        {
          params: {
            transactionCode: TransactionConstants.TRANSACTION_CODES.COACTVW
          }
        }
      );
      
      return response;
    } catch (error) {
      throw handleApiError(error);
    }
  },

  /**
   * Update account information with COACTUP transaction equivalent
   * 
   * @param {string} accountNumber - Account number
   * @param {Object} accountData - Account data to update
   * @returns {Promise<Object>} Update response
   */
  async updateAccount(accountNumber, accountData) {
    try {
      // Validate account number
      if (!accountNumber) {
        throw new Error(MessageConstants.FIELD_ERROR_MESSAGES.ACCOUNT_INVALID);
      }
      
      const formattedAccountNumber = DataFormatting.formatAccountNumber(accountNumber);
      
      // Format account data according to COBOL field specifications
      const updateData = {
        accountNumber: formattedAccountNumber,
        ...transformAccountData(accountData),
        transactionCode: TransactionConstants.TRANSACTION_CODES.COACTUP
      };
      
      const response = await apiClient.put(
        `${TransactionConstants.TRANSACTION_ENDPOINTS.ACCOUNT_UPDATE}/${formattedAccountNumber}`,
        updateData
      );
      
      return response;
    } catch (error) {
      throw handleApiError(error);
    }
  },

  /**
   * Get account list with pagination
   * 
   * @param {Object} options - Search and pagination options
   * @returns {Promise<Object>} Account list response
   */
  async getAccountList(options = {}) {
    try {
      const params = {
        page: options.page || 1,
        size: options.size || 10,
        sortBy: options.sortBy || 'accountNumber',
        sortOrder: options.sortOrder || 'asc',
        ...options.filters
      };
      
      const response = await apiClient.get(
        `${TransactionConstants.API_BASE_PATHS.ACCOUNT_SERVICE}/list`,
        { params }
      );
      
      return response;
    } catch (error) {
      throw handleApiError(error);
    }
  },

  /**
   * Search accounts by criteria
   * 
   * @param {Object} searchCriteria - Search criteria
   * @returns {Promise<Object>} Search results
   */
  async searchAccounts(searchCriteria) {
    try {
      const searchData = {
        ...searchCriteria,
        transactionCode: TransactionConstants.TRANSACTION_CODES.COACTVW
      };
      
      const response = await apiClient.post(
        `${TransactionConstants.API_BASE_PATHS.ACCOUNT_SERVICE}/search`,
        searchData
      );
      
      return response;
    } catch (error) {
      throw handleApiError(error);
    }
  }
};

/**
 * Transform account data for API requests
 * 
 * @param {Object} accountData - Raw account data
 * @returns {Object} Formatted account data
 */
function transformAccountData(accountData) {
  const transformed = { ...accountData };
  
  // Format currency fields
  const currencyFields = ['creditLimit', 'cashCreditLimit', 'currentBalance'];
  currencyFields.forEach(field => {
    if (transformed[field] !== undefined) {
      transformed[field] = DataFormatting.parseCobolDecimal(transformed[field], 31, 2);
    }
  });
  
  // Format date fields
  const dateFields = ['dateOpened', 'expiryDate', 'reissueDate'];
  dateFields.forEach(field => {
    if (transformed[field]) {
      transformed[field] = DataFormatting.formatDate(transformed[field], 'CCYYMMDD');
    }
  });
  
  // Format text fields
  const textFields = ['accountGroup', 'accountStatus'];
  textFields.forEach(field => {
    if (transformed[field]) {
      transformed[field] = DataFormatting.formatPicX(transformed[field], 10, false);
    }
  });
  
  return transformed;
}

// ============================================================================
// TRANSACTION API
// ============================================================================

/**
 * Transaction API wrapper
 * 
 * Provides transaction management API methods equivalent to
 * COTRN00 (Transaction List) and COTRN02 (Add Transaction) transaction functionality.
 */
export const transactionApi = {
  /**
   * Get transaction history with COTRN00 transaction equivalent
   * 
   * @param {Object} options - Search and pagination options
   * @returns {Promise<Object>} Transaction history response
   */
  async getTransactionHistory(options = {}) {
    try {
      const params = {
        page: options.page || 1,
        size: options.size || 10,
        accountNumber: options.accountNumber,
        startDate: options.startDate,
        endDate: options.endDate,
        transactionType: options.transactionType,
        transactionCode: TransactionConstants.TRANSACTION_CODES.COTRN00
      };
      
      // Remove undefined values
      Object.keys(params).forEach(key => {
        if (params[key] === undefined) {
          delete params[key];
        }
      });
      
      const response = await apiClient.get(
        TransactionConstants.TRANSACTION_ENDPOINTS.TRANSACTION_LIST,
        { params }
      );
      
      return response;
    } catch (error) {
      throw handleApiError(error);
    }
  },

  /**
   * Add new transaction with COTRN02 transaction equivalent
   * 
   * @param {Object} transactionData - Transaction data
   * @returns {Promise<Object>} Add transaction response
   */
  async addTransaction(transactionData) {
    try {
      // Format transaction data according to COBOL field specifications
      const formattedData = {
        ...transformTransactionData(transactionData),
        transactionCode: TransactionConstants.TRANSACTION_CODES.COTRN02
      };
      
      const response = await apiClient.post(
        `${TransactionConstants.API_BASE_PATHS.TRANSACTION_SERVICE}/add`,
        formattedData
      );
      
      return response;
    } catch (error) {
      throw handleApiError(error);
    }
  },

  /**
   * Search transactions by criteria
   * 
   * @param {Object} searchCriteria - Search criteria
   * @returns {Promise<Object>} Search results
   */
  async searchTransactions(searchCriteria) {
    try {
      const searchData = {
        ...searchCriteria,
        transactionCode: TransactionConstants.TRANSACTION_CODES.COTRN00
      };
      
      // Format transaction ID if provided (as per BMS TRNIDIN field)
      if (searchData.transactionId) {
        searchData.transactionId = DataFormatting.formatPicX(searchData.transactionId, 16, false);
      }
      
      const response = await apiClient.post(
        `${TransactionConstants.API_BASE_PATHS.TRANSACTION_SERVICE}/search`,
        searchData
      );
      
      return response;
    } catch (error) {
      throw handleApiError(error);
    }
  },

  /**
   * Get transaction details
   * 
   * @param {string} transactionId - Transaction ID
   * @returns {Promise<Object>} Transaction details response
   */
  async getTransactionDetails(transactionId) {
    try {
      if (!transactionId) {
        throw new Error(MessageConstants.FIELD_ERROR_MESSAGES.INVALID_TRANSACTION_ID);
      }
      
      const formattedTransactionId = DataFormatting.formatPicX(transactionId, 16, false);
      
      const response = await apiClient.get(
        `${TransactionConstants.API_BASE_PATHS.TRANSACTION_SERVICE}/details/${formattedTransactionId}`,
        {
          params: {
            transactionCode: TransactionConstants.TRANSACTION_CODES.COTRN01
          }
        }
      );
      
      return response;
    } catch (error) {
      throw handleApiError(error);
    }
  }
};

/**
 * Transform transaction data for API requests
 * 
 * @param {Object} transactionData - Raw transaction data
 * @returns {Object} Formatted transaction data
 */
function transformTransactionData(transactionData) {
  const transformed = { ...transactionData };
  
  // Format account number
  if (transformed.accountNumber) {
    transformed.accountNumber = DataFormatting.formatAccountNumber(transformed.accountNumber);
  }
  
  // Format card number
  if (transformed.cardNumber) {
    transformed.cardNumber = DataFormatting.formatCardNumber(transformed.cardNumber, false, false);
  }
  
  // Format transaction amount
  if (transformed.amount !== undefined) {
    transformed.amount = DataFormatting.parseCobolDecimal(transformed.amount, 31, 2);
  }
  
  // Format transaction date
  if (transformed.transactionDate) {
    transformed.transactionDate = DataFormatting.formatDate(transformed.transactionDate, 'CCYYMMDD');
  }
  
  // Format description
  if (transformed.description) {
    transformed.description = DataFormatting.formatPicX(transformed.description, 26, false);
  }
  
  return transformed;
}

// ============================================================================
// EXPORTS
// ============================================================================

// Export main API client as default
export default apiClient;

// Export all API modules and utilities
export {
  apiClient,
  authenticationApi,
  accountApi,
  transactionApi,
  tokenManager,
  handleApiError,
  createApiClient
};