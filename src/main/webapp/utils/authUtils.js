/**
 * authUtils.js
 * 
 * Authentication utility functions for JWT token management, session validation, 
 * user role extraction, and authentication state management that support the 
 * pseudo-conversational session architecture replacing CICS terminal storage behavior.
 * 
 * This module provides comprehensive authentication utilities aligned with the 
 * Spring Security 6.x JWT authentication framework and Redis-backed session management.
 * All functions maintain compatibility with the modernized CardDemo security architecture
 * while preserving original CICS authentication behavior patterns.
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0
 */

import { jwtDecode } from 'jwt-decode';
import { MessageConstants } from '../constants/MessageConstants.ts';
import { BaseScreenData, FormFieldAttributes } from '../types/CommonTypes.ts';

// ==============================================================================
// AUTHENTICATION CONSTANTS AND CONFIGURATION
// ==============================================================================

/**
 * Authentication configuration constants
 * These values align with Spring Security JWT configuration and Redis session management
 */
const AUTH_CONFIG = {
  // JWT token storage keys for browser localStorage
  TOKEN_STORAGE_KEY: 'carddemo_jwt_token',
  USER_CONTEXT_KEY: 'carddemo_user_context',
  SESSION_EXPIRY_KEY: 'carddemo_session_expiry',
  
  // Session timeout configuration (matching CICS terminal timeout)
  DEFAULT_SESSION_TIMEOUT_MINUTES: 30,
  SESSION_WARNING_MINUTES: 5,
  TOKEN_REFRESH_THRESHOLD_MINUTES: 5,
  
  // User role mappings from CICS SEC-USR-TYPE to Spring Security authorities
  USER_ROLES: {
    ADMIN: 'A',    // Maps to ROLE_ADMIN in Spring Security
    USER: 'U'      // Maps to ROLE_USER in Spring Security
  },
  
  // Spring Security role authorities
  SPRING_ROLES: {
    ROLE_ADMIN: 'ROLE_ADMIN',
    ROLE_USER: 'ROLE_USER'
  }
} as const;

// ==============================================================================
// JWT TOKEN VALIDATION FUNCTIONS
// ==============================================================================

/**
 * Validates a JWT token for structure, signature, and basic claims
 * Performs client-side validation without server verification
 * 
 * @param {string} token - JWT token string to validate
 * @returns {boolean} - True if token has valid structure and claims
 * 
 * Implementation Notes:
 * - Validates JWT structure (header.payload.signature format)
 * - Checks for required claims (sub, exp, iat, user_id, user_type)
 * - Does not perform signature verification (server-side responsibility)
 * - Provides first-line validation for client-side authentication state
 */
export const validateToken = (token) => {
  try {
    // Basic token format validation
    if (!token || typeof token !== 'string') {
      console.warn('Invalid token format: token must be a non-empty string');
      return false;
    }
    
    // JWT structure validation (three parts separated by dots)
    const parts = token.split('.');
    if (parts.length !== 3) {
      console.warn('Invalid JWT structure: token must have three parts (header.payload.signature)');
      return false;
    }
    
    // Decode and validate JWT payload
    const decoded = jwtDecode(token);
    
    // Validate required standard JWT claims
    if (!decoded.sub || !decoded.exp || !decoded.iat) {
      console.warn('Missing required JWT standard claims (sub, exp, iat)');
      return false;
    }
    
    // Validate CardDemo-specific claims for Spring Security integration
    if (!decoded.user_id || !decoded.user_type) {
      console.warn('Missing required CardDemo claims (user_id, user_type)');
      return false;
    }
    
    // Validate user_type is one of the expected values
    const validUserTypes = Object.values(AUTH_CONFIG.USER_ROLES);
    if (!validUserTypes.includes(decoded.user_type)) {
      console.warn(`Invalid user_type: ${decoded.user_type}. Expected: ${validUserTypes.join(', ')}`);
      return false;
    }
    
    // Validate token is not expired (basic client-side check)
    const currentTime = Math.floor(Date.now() / 1000);
    if (decoded.exp < currentTime) {
      console.warn('Token has expired');
      return false;
    }
    
    return true;
    
  } catch (error) {
    console.error('Token validation error:', error.message);
    return false;
  }
};

/**
 * Checks if a JWT token has expired or will expire within the refresh threshold
 * Supports proactive token refresh to maintain seamless user experience
 * 
 * @param {string} token - JWT token to check for expiration
 * @returns {boolean} - True if token is expired or needs refresh
 * 
 * Implementation Notes:
 * - Compares token expiration with current time
 * - Includes refresh threshold to enable proactive token renewal
 * - Accounts for clock skew and network latency in expiration logic
 * - Integrates with automatic session timeout detection
 */
export const isTokenExpired = (token) => {
  try {
    if (!token || !validateToken(token)) {
      return true;
    }
    
    const decoded = jwtDecode(token);
    const currentTime = Math.floor(Date.now() / 1000);
    const refreshThreshold = AUTH_CONFIG.TOKEN_REFRESH_THRESHOLD_MINUTES * 60;
    
    // Check if token is expired or needs refresh
    return decoded.exp <= (currentTime + refreshThreshold);
    
  } catch (error) {
    console.error('Error checking token expiration:', error.message);
    return true; // Assume expired on error for security
  }
};

// ==============================================================================
// USER ROLE AND CONTEXT EXTRACTION FUNCTIONS  
// ==============================================================================

/**
 * Extracts user role from JWT token and maps to Spring Security authorities
 * Provides role-based access control integration for React components
 * 
 * @param {string} token - JWT token containing user role information
 * @returns {string|null} - Spring Security role authority or null if invalid
 * 
 * Role Mapping Logic:
 * - CICS user_type 'A' → ROLE_ADMIN (full administrative access)
 * - CICS user_type 'U' → ROLE_USER (standard user access)
 * - Invalid or missing role → null (no access granted)
 * 
 * Integration Notes:
 * - Maps legacy CICS SEC-USR-TYPE values to modern Spring Security authorities
 * - Supports @PreAuthorize annotation role validation on backend services
 * - Enables React component conditional rendering based on user privileges
 */
export const extractUserRole = (token) => {
  try {
    if (!token || !validateToken(token)) {
      return null;
    }
    
    const decoded = jwtDecode(token);
    const userType = decoded.user_type;
    
    // Map CICS user types to Spring Security roles
    switch (userType) {
      case AUTH_CONFIG.USER_ROLES.ADMIN:
        return AUTH_CONFIG.SPRING_ROLES.ROLE_ADMIN;
      case AUTH_CONFIG.USER_ROLES.USER:
        return AUTH_CONFIG.SPRING_ROLES.ROLE_USER;
      default:
        console.warn(`Unknown user type: ${userType}`);
        return null;
    }
    
  } catch (error) {
    console.error('Error extracting user role:', error.message);
    return null;
  }
};

/**
 * Extracts comprehensive user context from JWT token claims
 * Provides complete user session information for React component state management
 * 
 * @param {string} token - JWT token containing user context claims
 * @returns {object|null} - User context object or null if invalid token
 * 
 * Returned User Context Structure:
 * - userId: User identifier from JWT sub claim
 * - userType: Original CICS user type (A/U)
 * - role: Mapped Spring Security role authority
 * - sessionId: Session correlation ID for Redis session management
 * - expiresAt: Token expiration timestamp
 * - issuedAt: Token issuance timestamp
 * 
 * Usage Notes:
 * - Provides complete authentication context for React components
 * - Supports session correlation with Redis-backed server-side sessions
 * - Enables comprehensive audit logging with user attribution
 */
export const getUserFromToken = (token) => {
  try {
    if (!token || !validateToken(token)) {
      return null;
    }
    
    const decoded = jwtDecode(token);
    
    // Extract comprehensive user context from JWT claims
    const userContext = {
      userId: decoded.user_id || decoded.sub,
      userName: decoded.user_name || decoded.user_id,
      userType: decoded.user_type,
      role: extractUserRole(token),
      sessionId: decoded.session_id || decoded.jti,
      expiresAt: new Date(decoded.exp * 1000),
      issuedAt: new Date(decoded.iat * 1000),
      roles: decoded.roles || [extractUserRole(token)],
      
      // Additional context for pseudo-conversational session management
      lastActivity: new Date(),
      sessionTimeout: AUTH_CONFIG.DEFAULT_SESSION_TIMEOUT_MINUTES
    };
    
    return userContext;
    
  } catch (error) {
    console.error('Error extracting user context:', error.message);
    return null;
  }
};

// ==============================================================================
// SESSION MANAGEMENT AND TIMEOUT FUNCTIONS
// ==============================================================================

/**
 * Calculates and returns session expiry information for timeout management
 * Supports CICS-equivalent session timeout behavior with configurable TTL
 * 
 * @param {string} token - JWT token to analyze for expiration timing
 * @returns {object|null} - Session expiry information or null if invalid
 * 
 * Returned Expiry Information:
 * - expiresAt: Absolute expiration timestamp
 * - remainingMinutes: Minutes until session expires
 * - needsRefresh: Boolean indicating if proactive refresh is needed
 * - warningThreshold: Boolean indicating if warning should be displayed
 * 
 * Session Management Notes:
 * - Aligns with Redis session TTL configuration on server side
 * - Provides early warning for session timeout (5 minutes default)
 * - Supports automatic logout redirection on session expiration
 * - Maintains consistency with CICS terminal timeout behavior
 */
export const getSessionExpiry = (token) => {
  try {
    if (!token || !validateToken(token)) {
      return null;
    }
    
    const decoded = jwtDecode(token);
    const currentTime = Date.now();
    const expirationTime = decoded.exp * 1000;
    const remainingMs = expirationTime - currentTime;
    const remainingMinutes = Math.floor(remainingMs / (1000 * 60));
    
    // Calculate session timeout thresholds
    const warningThreshold = AUTH_CONFIG.SESSION_WARNING_MINUTES;
    const refreshThreshold = AUTH_CONFIG.TOKEN_REFRESH_THRESHOLD_MINUTES;
    
    const sessionExpiry = {
      expiresAt: new Date(expirationTime),
      remainingMinutes: Math.max(0, remainingMinutes),
      remainingMs: Math.max(0, remainingMs),
      needsRefresh: remainingMinutes <= refreshThreshold,
      warningThreshold: remainingMinutes <= warningThreshold,
      isExpired: remainingMs <= 0,
      
      // Additional timing information for UI components
      warningTime: new Date(expirationTime - (warningThreshold * 60 * 1000)),
      refreshTime: new Date(expirationTime - (refreshThreshold * 60 * 1000))
    };
    
    return sessionExpiry;
    
  } catch (error) {
    console.error('Error calculating session expiry:', error.message);
    return null;
  }
};

// ==============================================================================
// AUTHENTICATION STATE MANAGEMENT FUNCTIONS
// ==============================================================================

/**
 * Retrieves stored authentication token from browser localStorage
 * Provides centralized token access with validation and error handling
 * 
 * @returns {string|null} - Valid JWT token or null if not available
 * 
 * Implementation Notes:
 * - Checks localStorage for stored authentication token
 * - Validates token structure and expiration before returning
 * - Automatically clears invalid or expired tokens
 * - Supports secure token storage patterns for web applications
 */
export const getStoredToken = () => {
  try {
    const token = localStorage.getItem(AUTH_CONFIG.TOKEN_STORAGE_KEY);
    
    if (!token) {
      return null;
    }
    
    // Validate stored token
    if (!validateToken(token)) {
      // Clear invalid token from storage
      clearAuthenticationState();
      return null;
    }
    
    // Check if token is expired
    if (isTokenExpired(token)) {
      // Clear expired token from storage
      clearAuthenticationState();
      return null;
    }
    
    return token;
    
  } catch (error) {
    console.error('Error retrieving stored token:', error.message);
    clearAuthenticationState();
    return null;
  }
};

/**
 * Stores authentication token and user context in browser localStorage
 * Provides secure token storage with comprehensive context preservation
 * 
 * @param {string} token - Valid JWT token to store
 * @param {object} userContext - Optional user context to store
 * @returns {boolean} - True if storage was successful
 * 
 * Storage Management:
 * - Validates token before storage to prevent invalid data persistence
 * - Stores user context separately for efficient access
 * - Calculates and stores session expiry information
 * - Provides atomic storage operations to prevent inconsistent state
 */
export const storeAuthenticationToken = (token, userContext = null) => {
  try {
    if (!token || !validateToken(token)) {
      console.error('Cannot store invalid authentication token');
      return false;
    }
    
    // Extract user context if not provided
    const context = userContext || getUserFromToken(token);
    if (!context) {
      console.error('Cannot extract user context from token');
      return false;
    }
    
    // Calculate session expiry
    const sessionExpiry = getSessionExpiry(token);
    if (!sessionExpiry) {
      console.error('Cannot calculate session expiry for token');
      return false;
    }
    
    // Store authentication data atomically
    localStorage.setItem(AUTH_CONFIG.TOKEN_STORAGE_KEY, token);
    localStorage.setItem(AUTH_CONFIG.USER_CONTEXT_KEY, JSON.stringify(context));
    localStorage.setItem(AUTH_CONFIG.SESSION_EXPIRY_KEY, JSON.stringify(sessionExpiry));
    
    return true;
    
  } catch (error) {
    console.error('Error storing authentication token:', error.message);
    clearAuthenticationState();
    return false;
  }
};

/**
 * Retrieves stored user context from browser localStorage
 * Provides efficient access to user session information without token decoding
 * 
 * @returns {object|null} - User context object or null if not available
 * 
 * Context Management:
 * - Returns cached user context for efficient component rendering
 * - Validates context consistency with stored token
 * - Automatically refreshes context if token has changed
 * - Supports React component authentication state management
 */
export const getStoredUserContext = () => {
  try {
    const contextJson = localStorage.getItem(AUTH_CONFIG.USER_CONTEXT_KEY);
    const token = getStoredToken();
    
    if (!contextJson || !token) {
      return null;
    }
    
    const context = JSON.parse(contextJson);
    
    // Validate context consistency with current token
    const currentContext = getUserFromToken(token);
    if (!currentContext || context.userId !== currentContext.userId) {
      // Context is inconsistent, refresh it
      const refreshedContext = storeAuthenticationToken(token) ? currentContext : null;
      return refreshedContext;
    }
    
    return context;
    
  } catch (error) {
    console.error('Error retrieving stored user context:', error.message);
    return null;
  }
};

/**
 * Checks if user is currently authenticated with valid session
 * Provides comprehensive authentication state validation for route guards
 * 
 * @returns {boolean} - True if user has valid authentication session
 * 
 * Authentication Validation:
 * - Verifies presence of valid JWT token
 * - Checks token expiration status
 * - Validates user context consistency
 * - Supports React Router protected route implementation
 */
export const isAuthenticated = () => {
  try {
    const token = getStoredToken();
    const userContext = getStoredUserContext();
    
    return !!(token && userContext && !isTokenExpired(token));
    
  } catch (error) {
    console.error('Error checking authentication status:', error.message);
    return false;
  }
};

/**
 * Checks if current user has administrative privileges
 * Provides role-based access control for administrative functions
 * 
 * @returns {boolean} - True if user has administrative role
 * 
 * Role Validation:
 * - Extracts role from stored authentication token
 * - Validates ROLE_ADMIN authority for administrative access
 * - Supports conditional rendering of administrative UI components
 * - Integrates with Spring Security @PreAuthorize backend authorization
 */
export const isAdminUser = () => {
  try {
    const token = getStoredToken();
    if (!token) {
      return false;
    }
    
    const role = extractUserRole(token);
    return role === AUTH_CONFIG.SPRING_ROLES.ROLE_ADMIN;
    
  } catch (error) {
    console.error('Error checking admin status:', error.message);
    return false;
  }
};

/**
 * Clears all authentication state from browser storage
 * Provides secure logout functionality with complete state cleanup
 * 
 * @returns {void}
 * 
 * Cleanup Operations:
 * - Removes JWT token from localStorage
 * - Clears user context and session expiry data
 * - Resets authentication-related browser state
 * - Supports secure logout and session termination
 */
export const clearAuthenticationState = () => {
  try {
    localStorage.removeItem(AUTH_CONFIG.TOKEN_STORAGE_KEY);
    localStorage.removeItem(AUTH_CONFIG.USER_CONTEXT_KEY);
    localStorage.removeItem(AUTH_CONFIG.SESSION_EXPIRY_KEY);
    
    // Clear any other authentication-related storage
    sessionStorage.removeItem(AUTH_CONFIG.TOKEN_STORAGE_KEY);
    sessionStorage.removeItem(AUTH_CONFIG.USER_CONTEXT_KEY);
    
  } catch (error) {
    console.error('Error clearing authentication state:', error.message);
  }
};

// ==============================================================================
// SESSION TIMEOUT AND REFRESH UTILITIES
// ==============================================================================

/**
 * Checks if session timeout warning should be displayed to user
 * Supports proactive session management with user notification
 * 
 * @returns {boolean} - True if timeout warning should be shown
 * 
 * Warning Logic:
 * - Calculates remaining session time from stored token
 * - Compares with configurable warning threshold (5 minutes default)
 * - Enables proactive user notification before automatic logout
 * - Supports session extension through user interaction
 */
export const shouldShowTimeoutWarning = () => {
  try {
    const token = getStoredToken();
    if (!token) {
      return false;
    }
    
    const sessionExpiry = getSessionExpiry(token);
    if (!sessionExpiry) {
      return false;
    }
    
    return sessionExpiry.warningThreshold && !sessionExpiry.isExpired;
    
  } catch (error) {
    console.error('Error checking timeout warning status:', error.message);
    return false;
  }
};

/**
 * Formats remaining session time for user display
 * Provides user-friendly time remaining notification
 * 
 * @returns {string} - Formatted time remaining string
 * 
 * Format Options:
 * - "X minutes remaining" for times greater than 1 minute
 * - "Less than 1 minute remaining" for final minute
 * - "Session expired" for expired sessions
 * - Supports internationalization and custom formatting
 */
export const getFormattedTimeRemaining = () => {
  try {
    const token = getStoredToken();
    if (!token) {
      return 'Session expired';
    }
    
    const sessionExpiry = getSessionExpiry(token);
    if (!sessionExpiry) {
      return 'Session expired';
    }
    
    if (sessionExpiry.isExpired) {
      return 'Session expired';
    }
    
    const minutes = sessionExpiry.remainingMinutes;
    if (minutes > 1) {
      return `${minutes} minutes remaining`;
    } else if (minutes === 1) {
      return '1 minute remaining';
    } else {
      return 'Less than 1 minute remaining';
    }
    
  } catch (error) {
    console.error('Error formatting time remaining:', error.message);
    return 'Session status unknown';
  }
};

/**
 * Creates timeout error message for user notification
 * Provides consistent error messaging for session timeout scenarios
 * 
 * @returns {object} - Formatted error message object for UI display
 * 
 * Message Structure:
 * - message: User-friendly timeout notification text
 * - code: Error code for programmatic handling
 * - severity: Message severity level for UI styling
 * - timestamp: Error occurrence timestamp for logging
 */
export const createTimeoutErrorMessage = () => {
  return {
    message: MessageConstants.API_ERROR_MESSAGES.TIMEOUT_ERROR || 'Your session has expired. Please log in again.',
    code: 'SESSION_TIMEOUT',
    severity: 'warning',
    timestamp: new Date(),
    field: null,
    action: 'redirect_to_login'
  };
};

/**
 * Creates authentication failed error message
 * Provides consistent error messaging for authentication failures
 * 
 * @returns {object} - Formatted error message object for UI display
 */
export const createAuthenticationErrorMessage = () => {
  return {
    message: MessageConstants.API_ERROR_MESSAGES.AUTHENTICATION_FAILED || 'Authentication failed. Please check your credentials.',
    code: 'AUTHENTICATION_FAILED',
    severity: 'error',
    timestamp: new Date(),
    field: null,
    action: 'retry_login'
  };
};

// ==============================================================================
// UTILITY FUNCTIONS FOR COMPONENT INTEGRATION
// ==============================================================================

/**
 * Creates base screen data structure for React components
 * Provides standardized screen header information across all components
 * 
 * @param {string} transactionCode - CICS-equivalent transaction code
 * @param {string} programName - Component/program identifier
 * @param {string} title01 - Primary screen title
 * @param {string} title02 - Secondary screen title
 * @returns {BaseScreenData} - Formatted screen data object
 * 
 * Implementation Notes:
 * - Maintains compatibility with original BMS header structure
 * - Provides consistent date/time formatting across components
 * - Supports audit trail requirements with transaction identification
 * - Integrates with React component state management patterns
 */
export const createBaseScreenData = (transactionCode, programName, title01, title02 = '') => {
  const now = new Date();
  
  const screenData = {
    trnname: transactionCode.padEnd(4, ' ').substring(0, 4),
    pgmname: programName.padEnd(8, ' ').substring(0, 8),
    curdate: now.toLocaleDateString('en-US', { 
      month: '2-digit', 
      day: '2-digit', 
      year: '2-digit' 
    }),
    curtime: now.toLocaleTimeString('en-US', { 
      hour12: false,
      hour: '2-digit',
      minute: '2-digit', 
      second: '2-digit'
    }),
    title01: title01.padEnd(40, ' ').substring(0, 40),
    title02: title02.padEnd(40, ' ').substring(0, 40)
  };
  
  return screenData;
};

/**
 * Creates default form field attributes for BMS compatibility
 * Provides standardized field attributes that maintain BMS field behavior
 * 
 * @param {object} overrides - Attribute overrides for specific field requirements
 * @returns {FormFieldAttributes} - Complete field attribute object
 * 
 * Default Attributes:
 * - attrb: ['UNPROT', 'NORM'] - Standard input field behavior
 * - color: 'GREEN' - Default field color matching BMS conventions
 * - hilight: 'OFF' - No special highlighting
 * - length: 50 - Standard field length
 * - pos: {row: 1, column: 1} - Default positioning
 */
export const createDefaultFieldAttributes = (overrides = {}) => {
  const defaultAttributes = {
    attrb: ['UNPROT', 'NORM'],
    color: 'GREEN',
    hilight: 'OFF',
    length: 50,
    pos: { row: 1, column: 1 },
    initial: '',
    picin: undefined,
    validn: undefined
  };
  
  return { ...defaultAttributes, ...overrides };
};

// ==============================================================================
// EXPORTED AUTHENTICATION UTILITIES
// ==============================================================================

// Export all authentication utility functions for React component integration
export default {
  // Core JWT validation and processing
  validateToken,
  extractUserRole,
  isTokenExpired,
  getUserFromToken,
  getSessionExpiry,
  
  // Authentication state management
  getStoredToken,
  storeAuthenticationToken,
  getStoredUserContext,
  isAuthenticated,
  isAdminUser,
  clearAuthenticationState,
  
  // Session timeout and warning utilities
  shouldShowTimeoutWarning,
  getFormattedTimeRemaining,
  createTimeoutErrorMessage,
  createAuthenticationErrorMessage,
  
  // Component integration utilities
  createBaseScreenData,
  createDefaultFieldAttributes,
  
  // Configuration constants
  AUTH_CONFIG
};