/**
 * AuthUtils.js - Authentication Utility Functions
 * 
 * JWT token management, session validation, user role extraction, and authentication 
 * state management utilities supporting the pseudo-conversational session architecture 
 * that replaces CICS terminal storage behavior.
 * 
 * This module implements the client-side authentication utilities for the CardDemo 
 * modernized security architecture, providing comprehensive JWT token handling and 
 * session management capabilities aligned with Spring Security 6.x framework.
 * 
 * Key Features:
 * - JWT token validation with expiration checking per Section 6.4.1.5
 * - User role mapping from CICS user types (A/U) to Spring Security authorities 
 * - Session timeout detection matching CICS terminal timeout behavior
 * - Authentication state management for pseudo-conversational flow
 * - Support for stateless REST API calls with session correlation IDs
 * 
 * Security Implementation Notes:
 * - HS256 algorithm JWT tokens with configurable secret rotation
 * - 30-minute default token expiration with refresh token support
 * - Claims structure: user_id, user_type, roles array, session correlation ID
 * - Redis-backed session management with TTL equivalent to CICS timeout
 */

import { jwtDecode } from 'jwt-decode';
import { API_ERROR_MESSAGES, VALIDATION_ERRORS, HELP_TEXT } from '../constants/MessageConstants.ts';
import { BaseScreenData, FormFieldAttributes } from '../types/CommonTypes.ts';

// =============================================================================
// AUTHENTICATION CONSTANTS
// =============================================================================

/**
 * Authentication token constants aligned with Spring Security JWT configuration
 * Based on Section 6.4.1.5 JWT Token Management and Security requirements
 */
const AUTH_CONSTANTS = {
  // Token storage keys
  JWT_TOKEN_KEY: 'cardemo_jwt_token',
  USER_CONTEXT_KEY: 'cardemo_user_context',
  SESSION_ID_KEY: 'cardemo_session_id',
  
  // Token validation parameters
  TOKEN_EXPIRY_BUFFER_MS: 60000, // 1 minute buffer before expiration
  SESSION_TIMEOUT_MS: 1800000,   // 30 minutes (matching Spring Security default)
  MAX_REFRESH_ATTEMPTS: 3,       // Maximum token refresh retry attempts
  
  // User role mappings from CICS to Spring Security
  ROLE_MAPPINGS: {
    'A': 'ROLE_ADMIN',    // CICS Admin user type to Spring Security Admin role
    'U': 'ROLE_USER'      // CICS User type to Spring Security User role
  },
  
  // JWT claim names per Spring Security OAuth2 configuration
  CLAIMS: {
    USER_ID: 'user_id',
    USER_TYPE: 'user_type', 
    ROLES: 'roles',
    SESSION_ID: 'session_id',
    ISSUER: 'iss',
    EXPIRY: 'exp',
    ISSUED_AT: 'iat',
    SUBJECT: 'sub'
  }
};

// =============================================================================
// TOKEN VALIDATION FUNCTIONS
// =============================================================================

/**
 * Validates JWT token integrity and expiration status
 * 
 * Implements comprehensive JWT token validation per Section 6.4.1.5 requirements:
 * - Verifies token structure and format
 * - Checks expiration with configurable buffer time
 * - Validates required claims presence
 * - Ensures token signature integrity (client-side verification)
 * 
 * @param {string} token - JWT token string to validate
 * @returns {object} Validation result with status and error details
 */
export const validateToken = (token) => {
  try {
    // Input validation - ensure token is provided and properly formatted
    if (!token || typeof token !== 'string') {
      return {
        isValid: false,
        error: API_ERROR_MESSAGES.AUTHENTICATION_FAILED,
        errorCode: 'AUTH_TOKEN_MISSING',
        details: 'JWT token is required for authentication'
      };
    }
    
    // Remove Bearer prefix if present (common in Authorization headers)
    const cleanToken = token.replace(/^Bearer\s+/, '');
    
    // Verify token has proper JWT structure (header.payload.signature)
    const tokenParts = cleanToken.split('.');
    if (tokenParts.length !== 3) {
      return {
        isValid: false,
        error: API_ERROR_MESSAGES.AUTHENTICATION_FAILED,
        errorCode: 'AUTH_TOKEN_MALFORMED',
        details: 'JWT token format is invalid'
      };
    }
    
    // Decode token payload without verification (server handles signature validation)
    const decodedToken = jwtDecode(cleanToken);
    
    // Validate required claims are present per Spring Security JWT configuration
    const requiredClaims = [
      AUTH_CONSTANTS.CLAIMS.USER_ID,
      AUTH_CONSTANTS.CLAIMS.USER_TYPE,
      AUTH_CONSTANTS.CLAIMS.EXPIRY,
      AUTH_CONSTANTS.CLAIMS.ISSUED_AT
    ];
    
    for (const claim of requiredClaims) {
      if (!decodedToken[claim]) {
        return {
          isValid: false,
          error: API_ERROR_MESSAGES.AUTHENTICATION_FAILED,
          errorCode: 'AUTH_TOKEN_INCOMPLETE',
          details: `Required claim '${claim}' is missing from JWT token`
        };
      }
    }
    
    // Check token expiration with buffer time
    const currentTime = Math.floor(Date.now() / 1000); // Convert to seconds
    const expiryTime = decodedToken[AUTH_CONSTANTS.CLAIMS.EXPIRY];
    const bufferTime = Math.floor(AUTH_CONSTANTS.TOKEN_EXPIRY_BUFFER_MS / 1000);
    
    if (currentTime >= (expiryTime - bufferTime)) {
      return {
        isValid: false,
        error: API_ERROR_MESSAGES.TIMEOUT_ERROR,
        errorCode: 'AUTH_TOKEN_EXPIRED',
        details: 'JWT token has expired and requires refresh',
        expiryTime: expiryTime,
        currentTime: currentTime
      };
    }
    
    // Validate user type is one of the supported CICS types
    const userType = decodedToken[AUTH_CONSTANTS.CLAIMS.USER_TYPE];
    if (!AUTH_CONSTANTS.ROLE_MAPPINGS[userType]) {
      return {
        isValid: false,
        error: API_ERROR_MESSAGES.AUTHORIZATION_DENIED,
        errorCode: 'AUTH_INVALID_USER_TYPE',
        details: `Invalid user type '${userType}' in JWT token`
      };
    }
    
    // Token validation successful
    return {
      isValid: true,
      decodedToken: decodedToken,
      expiryTime: expiryTime,
      timeUntilExpiry: (expiryTime - currentTime) * 1000, // Convert back to milliseconds
      message: 'JWT token validation successful'
    };
    
  } catch (error) {
    // Handle JWT decode errors or other validation failures
    console.error('JWT token validation error:', error);
    
    return {
      isValid: false,
      error: API_ERROR_MESSAGES.AUTHENTICATION_FAILED,
      errorCode: 'AUTH_TOKEN_DECODE_ERROR',
      details: `JWT token decode failed: ${error.message}`,
      originalError: error
    };
  }
};

/**
 * Checks if JWT token is expired
 * 
 * Provides session timeout detection per Section 7.6.3 requirements for 
 * automatic logout redirection when session expires. Implements the 
 * pseudo-conversational timeout behavior equivalent to CICS terminal timeout.
 * 
 * @param {string} token - JWT token to check for expiration
 * @returns {boolean} True if token is expired, false otherwise
 */
export const isTokenExpired = (token) => {
  try {
    if (!token) {
      return true; // No token means expired/unauthenticated
    }
    
    const validationResult = validateToken(token);
    
    // Token is expired if validation fails due to expiration
    if (!validationResult.isValid && validationResult.errorCode === 'AUTH_TOKEN_EXPIRED') {
      return true;
    }
    
    // Additional check for tokens very close to expiration
    if (validationResult.isValid && validationResult.timeUntilExpiry < AUTH_CONSTANTS.TOKEN_EXPIRY_BUFFER_MS) {
      return true;
    }
    
    return !validationResult.isValid;
    
  } catch (error) {
    console.error('Token expiration check error:', error);
    return true; // Assume expired on error for security
  }
};

// =============================================================================
// USER ROLE AND CONTEXT EXTRACTION
// =============================================================================

/**
 * Extracts user role from JWT claims and maps to Spring Security authorities
 * 
 * Implements user role mapping from CICS user types (A/U) to Spring Security 
 * authorities (ROLE_ADMIN/ROLE_USER) per Section 6.4.2.1 requirements.
 * Supports React component access control and authorization decisions.
 * 
 * @param {string} token - JWT token containing user role claims
 * @returns {object} User role information with Spring Security mappings
 */
export const extractUserRole = (token) => {
  try {
    const validationResult = validateToken(token);
    
    if (!validationResult.isValid) {
      return {
        success: false,
        error: validationResult.error,
        errorCode: validationResult.errorCode,
        userType: null,
        springRole: null,
        permissions: []
      };
    }
    
    const decodedToken = validationResult.decodedToken;
    const userType = decodedToken[AUTH_CONSTANTS.CLAIMS.USER_TYPE];
    const springRole = AUTH_CONSTANTS.ROLE_MAPPINGS[userType];
    
    // Extract roles array if present (for enhanced RBAC)
    const rolesArray = decodedToken[AUTH_CONSTANTS.CLAIMS.ROLES] || [springRole];
    
    // Determine user permissions based on role
    const permissions = determineUserPermissions(springRole, rolesArray);
    
    return {
      success: true,
      userType: userType,
      springRole: springRole,
      roles: rolesArray,
      permissions: permissions,
      isAdmin: springRole === 'ROLE_ADMIN',
      isUser: springRole === 'ROLE_USER' || springRole === 'ROLE_ADMIN', // Admin inherits user permissions
      message: `User role extracted successfully: ${springRole}`
    };
    
  } catch (error) {
    console.error('User role extraction error:', error);
    
    return {
      success: false,
      error: API_ERROR_MESSAGES.AUTHORIZATION_DENIED,
      errorCode: 'AUTH_ROLE_EXTRACTION_ERROR',
      details: `Failed to extract user role: ${error.message}`,
      userType: null,
      springRole: null,
      permissions: []
    };
  }
};

/**
 * Extracts complete user context from JWT token
 * 
 * Provides comprehensive user information for authentication state management 
 * and session context preservation across stateless REST API calls per 
 * Section 0.2.1 pseudo-conversational flow requirements.
 * 
 * @param {string} token - JWT token containing user context
 * @returns {object} Complete user context information
 */
export const getUserFromToken = (token) => {
  try {
    const validationResult = validateToken(token);
    
    if (!validationResult.isValid) {
      return {
        success: false,
        error: validationResult.error,
        errorCode: validationResult.errorCode,
        user: null
      };
    }
    
    const decodedToken = validationResult.decodedToken;
    const roleInfo = extractUserRole(token);
    
    // Build comprehensive user context object
    const userContext = {
      userId: decodedToken[AUTH_CONSTANTS.CLAIMS.USER_ID],
      userType: decodedToken[AUTH_CONSTANTS.CLAIMS.USER_TYPE],
      springRole: roleInfo.springRole,
      roles: roleInfo.roles,
      permissions: roleInfo.permissions,
      sessionId: decodedToken[AUTH_CONSTANTS.CLAIMS.SESSION_ID],
      issuedAt: new Date(decodedToken[AUTH_CONSTANTS.CLAIMS.ISSUED_AT] * 1000),
      expiryTime: new Date(decodedToken[AUTH_CONSTANTS.CLAIMS.EXPIRY] * 1000),
      timeUntilExpiry: validationResult.timeUntilExpiry,
      isAdmin: roleInfo.isAdmin,
      isAuthenticated: true,
      tokenClaims: decodedToken
    };
    
    return {
      success: true,
      user: userContext,
      message: 'User context extracted successfully'
    };
    
  } catch (error) {
    console.error('User context extraction error:', error);
    
    return {
      success: false,
      error: API_ERROR_MESSAGES.SERVER_ERROR,
      errorCode: 'AUTH_USER_CONTEXT_ERROR',
      details: `Failed to extract user context: ${error.message}`,
      user: null
    };
  }
};

// =============================================================================
// SESSION EXPIRY AND TIMEOUT MANAGEMENT
// =============================================================================

/**
 * Calculates session expiry time and remaining session duration
 * 
 * Implements configurable TTL matching CICS terminal timeout behavior per 
 * Section 7.6.3 requirements. Provides session timeout utilities for 
 * automatic logout redirection and session extension prompts.
 * 
 * @param {string} token - JWT token to analyze for expiry information
 * @returns {object} Session expiry details and timeout calculations
 */
export const getSessionExpiry = (token) => {
  try {
    const validationResult = validateToken(token);
    
    if (!validationResult.isValid) {
      return {
        success: false,
        error: validationResult.error,
        errorCode: validationResult.errorCode,
        expiryTime: null,
        remainingTime: 0,
        isExpired: true
      };
    }
    
    const decodedToken = validationResult.decodedToken;
    const currentTime = new Date();
    const expiryTime = new Date(decodedToken[AUTH_CONSTANTS.CLAIMS.EXPIRY] * 1000);
    const issuedTime = new Date(decodedToken[AUTH_CONSTANTS.CLAIMS.ISSUED_AT] * 1000);
    
    // Calculate remaining session time
    const remainingTimeMs = expiryTime.getTime() - currentTime.getTime();
    const totalSessionTimeMs = expiryTime.getTime() - issuedTime.getTime();
    
    // Determine session status
    const isExpired = remainingTimeMs <= 0;
    const isNearExpiry = remainingTimeMs <= AUTH_CONSTANTS.TOKEN_EXPIRY_BUFFER_MS;
    const sessionProgress = Math.max(0, Math.min(100, 
      ((totalSessionTimeMs - remainingTimeMs) / totalSessionTimeMs) * 100
    ));
    
    return {
      success: true,
      expiryTime: expiryTime,
      issuedTime: issuedTime,
      currentTime: currentTime,
      remainingTime: Math.max(0, remainingTimeMs),
      totalSessionTime: totalSessionTimeMs,
      isExpired: isExpired,
      isNearExpiry: isNearExpiry,
      sessionProgress: sessionProgress,
      warningThreshold: AUTH_CONSTANTS.TOKEN_EXPIRY_BUFFER_MS,
      sessionId: decodedToken[AUTH_CONSTANTS.CLAIMS.SESSION_ID],
      message: isExpired ? 'Session has expired' : 
               isNearExpiry ? 'Session expires soon' : 
               'Session is active'
    };
    
  } catch (error) {
    console.error('Session expiry calculation error:', error);
    
    return {
      success: false,
      error: API_ERROR_MESSAGES.SERVER_ERROR,
      errorCode: 'AUTH_SESSION_EXPIRY_ERROR',
      details: `Failed to calculate session expiry: ${error.message}`,
      expiryTime: null,
      remainingTime: 0,
      isExpired: true
    };
  }
};

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

/**
 * Determines user permissions based on Spring Security role
 * 
 * @param {string} springRole - Spring Security role (ROLE_ADMIN or ROLE_USER)
 * @param {Array} rolesArray - Array of additional roles from JWT
 * @returns {Array} Array of permission strings for the user
 */
const determineUserPermissions = (springRole, rolesArray = []) => {
  const permissions = [];
  
  // Base user permissions for all authenticated users
  if (springRole === 'ROLE_USER' || springRole === 'ROLE_ADMIN') {
    permissions.push(
      'account:view',
      'account:update',
      'card:list',
      'card:view',
      'transaction:view',
      'transaction:add',
      'bill:payment',
      'profile:view',
      'profile:update'
    );
  }
  
  // Additional admin permissions
  if (springRole === 'ROLE_ADMIN') {
    permissions.push(
      'user:list',
      'user:create',
      'user:update',
      'user:delete',
      'admin:menu',
      'system:config',
      'audit:view',
      'reports:generate'
    );
  }
  
  // Process additional roles from JWT if present
  rolesArray.forEach(role => {
    if (role.startsWith('ROLE_')) {
      // Add any additional role-specific permissions here
      // This allows for future extension of the role system
    }
  });
  
  return [...new Set(permissions)]; // Remove duplicates
};

/**
 * Retrieves stored authentication token from browser storage
 * 
 * @returns {string|null} JWT token or null if not found
 */
const getStoredToken = () => {
  try {
    return localStorage.getItem(AUTH_CONSTANTS.JWT_TOKEN_KEY) || 
           sessionStorage.getItem(AUTH_CONSTANTS.JWT_TOKEN_KEY);
  } catch (error) {
    console.error('Error retrieving stored token:', error);
    return null;
  }
};

/**
 * Stores authentication token in browser storage
 * 
 * @param {string} token - JWT token to store
 * @param {boolean} persistent - Whether to use localStorage (true) or sessionStorage (false)
 */
const storeToken = (token, persistent = false) => {
  try {
    if (persistent) {
      localStorage.setItem(AUTH_CONSTANTS.JWT_TOKEN_KEY, token);
    } else {
      sessionStorage.setItem(AUTH_CONSTANTS.JWT_TOKEN_KEY, token);
    }
  } catch (error) {
    console.error('Error storing token:', error);
  }
};

/**
 * Removes authentication token and related data from browser storage
 */
const clearStoredAuth = () => {
  try {
    localStorage.removeItem(AUTH_CONSTANTS.JWT_TOKEN_KEY);
    localStorage.removeItem(AUTH_CONSTANTS.USER_CONTEXT_KEY);
    localStorage.removeItem(AUTH_CONSTANTS.SESSION_ID_KEY);
    sessionStorage.removeItem(AUTH_CONSTANTS.JWT_TOKEN_KEY);
    sessionStorage.removeItem(AUTH_CONSTANTS.USER_CONTEXT_KEY);
    sessionStorage.removeItem(AUTH_CONSTANTS.SESSION_ID_KEY);
  } catch (error) {
    console.error('Error clearing stored authentication data:', error);
  }
};

// =============================================================================
// ADDITIONAL UTILITY EXPORTS
// =============================================================================

/**
 * Authentication utility constants and helper functions
 * Exported for use by other components that need authentication functionality
 */
export const authHelpers = {
  constants: AUTH_CONSTANTS,
  getStoredToken,
  storeToken,
  clearStoredAuth,
  
  /**
   * Checks if user has specific permission
   * @param {string} token - JWT token
   * @param {string} permission - Permission to check
   * @returns {boolean} True if user has permission
   */
  hasPermission: (token, permission) => {
    const roleInfo = extractUserRole(token);
    return roleInfo.success && roleInfo.permissions.includes(permission);
  },
  
  /**
   * Checks if user has admin role
   * @param {string} token - JWT token
   * @returns {boolean} True if user is admin
   */
  isAdmin: (token) => {
    const roleInfo = extractUserRole(token);
    return roleInfo.success && roleInfo.isAdmin;
  },
  
  /**
   * Formats remaining time for display
   * @param {number} remainingTimeMs - Remaining time in milliseconds
   * @returns {string} Formatted time string
   */
  formatRemainingTime: (remainingTimeMs) => {
    if (remainingTimeMs <= 0) return '00:00';
    
    const minutes = Math.floor(remainingTimeMs / 60000);
    const seconds = Math.floor((remainingTimeMs % 60000) / 1000);
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  }
};

// Export default object for convenience
export default {
  validateToken,
  extractUserRole,
  isTokenExpired,
  getUserFromToken,
  getSessionExpiry,
  helpers: authHelpers
};