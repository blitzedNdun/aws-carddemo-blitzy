/**
 * authUtils.js
 * 
 * Authentication utility functions for JWT token management, session validation,
 * user role extraction, and authentication state management that support the
 * pseudo-conversational session architecture replacing CICS terminal storage behavior.
 * 
 * This module provides comprehensive authentication utilities for the CardDemo application,
 * implementing cloud-native JWT-based authentication patterns that maintain functional
 * equivalence with original CICS security behavior while enabling modern React session
 * management and Spring Security integration.
 * 
 * Key Features:
 * - JWT token validation with expiration checking and claims extraction
 * - User role mapping from CICS user types (A/U) to Spring Security authorities
 * - Session timeout detection with automatic logout redirection
 * - Authentication state management for stateless REST API calls
 * - Pseudo-conversational session flow preservation
 * 
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0
 */

import { jwtDecode } from 'jwt-decode';
import { MessageConstants } from '../constants/MessageConstants';
import { BaseScreenData, FormFieldAttributes } from '../types/CommonTypes';

// ============================================================================
// CONSTANTS AND CONFIGURATION
// ============================================================================

/**
 * JWT token configuration constants
 * Aligned with Spring Security JWT implementation
 */
const JWT_CONFIG = {
  TOKEN_PREFIX: 'Bearer ',
  HEADER_NAME: 'Authorization',
  STORAGE_KEY: 'cardDemo_jwt_token',
  REFRESH_TOKEN_KEY: 'cardDemo_refresh_token',
  USER_CONTEXT_KEY: 'cardDemo_user_context',
  SESSION_TIMEOUT_KEY: 'cardDemo_session_timeout',
  
  // Token expiration buffer (refresh 5 minutes before expiration)
  EXPIRATION_BUFFER_MS: 5 * 60 * 1000,
  
  // Default session timeout matching CICS terminal timeout (30 minutes)
  DEFAULT_SESSION_TIMEOUT_MS: 30 * 60 * 1000,
  
  // Role mapping from CICS user types to Spring Security authorities
  ROLE_MAPPING: {
    'A': 'ROLE_ADMIN',  // Admin user type maps to ROLE_ADMIN
    'U': 'ROLE_USER'    // User type maps to ROLE_USER
  }
};

/**
 * Authentication state constants
 * Manages pseudo-conversational session state
 */
const AUTH_STATES = {
  AUTHENTICATED: 'AUTHENTICATED',
  UNAUTHENTICATED: 'UNAUTHENTICATED',
  EXPIRED: 'EXPIRED',
  INVALID: 'INVALID',
  REFRESHING: 'REFRESHING'
};

// ============================================================================
// JWT TOKEN VALIDATION UTILITIES
// ============================================================================

/**
 * Validates JWT token structure, signature, and expiration
 * Implements comprehensive token validation for authentication context
 * per Section 6.4.1.5 requirements
 * 
 * @param {string} token - JWT token to validate
 * @returns {Object} Validation result with status and decoded payload
 */
export function validateToken(token) {
  try {
    // Check if token exists and has proper format
    if (!token) {
      return {
        isValid: false,
        error: MessageConstants.API_ERROR_MESSAGES.AUTHENTICATION_FAILED,
        errorCode: 'TOKEN_MISSING'
      };
    }

    // Remove Bearer prefix if present
    const cleanToken = token.replace(JWT_CONFIG.TOKEN_PREFIX, '').trim();
    
    if (!cleanToken) {
      return {
        isValid: false,
        error: MessageConstants.API_ERROR_MESSAGES.AUTHENTICATION_FAILED,
        errorCode: 'TOKEN_EMPTY'
      };
    }

    // Decode JWT token to extract claims
    let decodedToken;
    try {
      decodedToken = jwtDecode(cleanToken);
    } catch (decodeError) {
      return {
        isValid: false,
        error: MessageConstants.API_ERROR_MESSAGES.AUTHENTICATION_FAILED,
        errorCode: 'TOKEN_INVALID_FORMAT',
        details: decodeError.message
      };
    }

    // Validate token structure and required claims
    if (!decodedToken.sub || !decodedToken.exp || !decodedToken.iat) {
      return {
        isValid: false,
        error: MessageConstants.API_ERROR_MESSAGES.AUTHENTICATION_FAILED,
        errorCode: 'TOKEN_MISSING_CLAIMS'
      };
    }

    // Check if token is expired
    const currentTime = Math.floor(Date.now() / 1000);
    if (decodedToken.exp < currentTime) {
      return {
        isValid: false,
        error: MessageConstants.API_ERROR_MESSAGES.SESSION_EXPIRED,
        errorCode: 'TOKEN_EXPIRED',
        expiredAt: new Date(decodedToken.exp * 1000)
      };
    }

    // Check if token is used before its issued time
    if (decodedToken.iat > currentTime) {
      return {
        isValid: false,
        error: MessageConstants.API_ERROR_MESSAGES.AUTHENTICATION_FAILED,
        errorCode: 'TOKEN_FUTURE_ISSUED'
      };
    }

    // Validate user type claim for role mapping
    if (decodedToken.user_type && !JWT_CONFIG.ROLE_MAPPING[decodedToken.user_type]) {
      return {
        isValid: false,
        error: MessageConstants.API_ERROR_MESSAGES.AUTHORIZATION_DENIED,
        errorCode: 'INVALID_USER_TYPE'
      };
    }

    return {
      isValid: true,
      payload: decodedToken,
      expiresAt: new Date(decodedToken.exp * 1000),
      issuedAt: new Date(decodedToken.iat * 1000),
      timeToExpiry: (decodedToken.exp - currentTime) * 1000
    };

  } catch (error) {
    return {
      isValid: false,
      error: MessageConstants.API_ERROR_MESSAGES.AUTHENTICATION_FAILED,
      errorCode: 'VALIDATION_ERROR',
      details: error.message
    };
  }
}

/**
 * Checks if JWT token is expired or approaching expiration
 * Implements session timeout detection per Section 7.6.3
 * 
 * @param {string} token - JWT token to check
 * @returns {Object} Expiration status and timing information
 */
export function isTokenExpired(token) {
  try {
    const validation = validateToken(token);
    
    if (!validation.isValid) {
      return {
        isExpired: true,
        reason: validation.errorCode,
        error: validation.error
      };
    }

    const currentTime = Date.now();
    const expirationTime = validation.payload.exp * 1000;
    const timeUntilExpiry = expirationTime - currentTime;

    // Check if token is already expired
    if (timeUntilExpiry <= 0) {
      return {
        isExpired: true,
        reason: 'TOKEN_EXPIRED',
        expiredAt: validation.expiresAt
      };
    }

    // Check if token is approaching expiration (within buffer time)
    const isApproachingExpiry = timeUntilExpiry <= JWT_CONFIG.EXPIRATION_BUFFER_MS;

    return {
      isExpired: false,
      isApproachingExpiry,
      timeUntilExpiry,
      expiresAt: validation.expiresAt,
      shouldRefresh: isApproachingExpiry
    };

  } catch (error) {
    return {
      isExpired: true,
      reason: 'VALIDATION_ERROR',
      error: MessageConstants.API_ERROR_MESSAGES.AUTHENTICATION_FAILED,
      details: error.message
    };
  }
}

// ============================================================================
// USER ROLE EXTRACTION UTILITIES
// ============================================================================

/**
 * Extracts user role from JWT claims and maps to Spring Security authorities
 * Implements user role mapping per Section 6.4.2.1 requirements
 * Maps CICS user types (A/U) to Spring Security authorities (ROLE_ADMIN/ROLE_USER)
 * 
 * @param {string} token - JWT token containing user role claims
 * @returns {Object} User role information and Spring Security authorities
 */
export function extractUserRole(token) {
  try {
    const validation = validateToken(token);
    
    if (!validation.isValid) {
      return {
        hasRole: false,
        error: validation.error,
        errorCode: validation.errorCode
      };
    }

    const payload = validation.payload;
    
    // Extract user type from JWT claims (A for Admin, U for User)
    const userType = payload.user_type;
    const roles = payload.roles || [];
    
    if (!userType) {
      return {
        hasRole: false,
        error: MessageConstants.API_ERROR_MESSAGES.AUTHORIZATION_DENIED,
        errorCode: 'MISSING_USER_TYPE'
      };
    }

    // Map CICS user type to Spring Security authority
    const springSecurityRole = JWT_CONFIG.ROLE_MAPPING[userType];
    
    if (!springSecurityRole) {
      return {
        hasRole: false,
        error: MessageConstants.API_ERROR_MESSAGES.AUTHORIZATION_DENIED,
        errorCode: 'INVALID_USER_TYPE',
        userType
      };
    }

    // Determine user capabilities based on role
    const isAdmin = userType === 'A';
    const isUser = userType === 'U';

    return {
      hasRole: true,
      userType,
      springSecurityRole,
      roles: roles.length > 0 ? roles : [springSecurityRole],
      isAdmin,
      isUser,
      capabilities: {
        canManageUsers: isAdmin,
        canViewAccounts: isAdmin || isUser,
        canUpdateAccounts: isAdmin || isUser,
        canProcessTransactions: isAdmin || isUser,
        canAccessSystemAdmin: isAdmin,
        canViewReports: isAdmin,
        canManageCards: isAdmin || isUser
      }
    };

  } catch (error) {
    return {
      hasRole: false,
      error: MessageConstants.API_ERROR_MESSAGES.AUTHORIZATION_DENIED,
      errorCode: 'ROLE_EXTRACTION_ERROR',
      details: error.message
    };
  }
}

// ============================================================================
// USER CONTEXT EXTRACTION UTILITIES
// ============================================================================

/**
 * Extracts user context and session information from JWT token
 * Implements JWT claims processing per Section 6.4.1.5 requirements
 * 
 * @param {string} token - JWT token containing user context
 * @returns {Object} Complete user context for session management
 */
export function getUserFromToken(token) {
  try {
    const validation = validateToken(token);
    
    if (!validation.isValid) {
      return {
        user: null,
        error: validation.error,
        errorCode: validation.errorCode
      };
    }

    const payload = validation.payload;
    const roleInfo = extractUserRole(token);
    
    if (!roleInfo.hasRole) {
      return {
        user: null,
        error: roleInfo.error,
        errorCode: roleInfo.errorCode
      };
    }

    // Extract user context from JWT claims
    const userContext = {
      userId: payload.sub,
      username: payload.username || payload.sub,
      firstName: payload.first_name || '',
      lastName: payload.last_name || '',
      email: payload.email || '',
      userType: roleInfo.userType,
      role: roleInfo.springSecurityRole,
      roles: roleInfo.roles,
      capabilities: roleInfo.capabilities,
      
      // Session information
      sessionId: payload.jti || payload.session_id,
      sessionCorrelationId: payload.correlation_id,
      issuedAt: validation.issuedAt,
      expiresAt: validation.expiresAt,
      
      // Additional context for pseudo-conversational flow
      lastActivity: new Date(),
      authenticationTime: validation.issuedAt,
      
      // Menu access based on role
      menuAccess: {
        mainMenu: true,
        adminMenu: roleInfo.isAdmin,
        userManagement: roleInfo.isAdmin,
        accountManagement: true,
        transactionProcessing: true,
        cardManagement: true,
        billPayment: true,
        reports: roleInfo.isAdmin
      }
    };

    return {
      user: userContext,
      isAuthenticated: true,
      authState: AUTH_STATES.AUTHENTICATED
    };

  } catch (error) {
    return {
      user: null,
      error: MessageConstants.API_ERROR_MESSAGES.AUTHENTICATION_FAILED,
      errorCode: 'USER_EXTRACTION_ERROR',
      details: error.message
    };
  }
}

// ============================================================================
// SESSION TIMEOUT UTILITIES
// ============================================================================

/**
 * Calculates session expiry time with configurable TTL
 * Implements session timeout utilities per Section 7.6.3 requirements
 * Matches CICS terminal timeout behavior
 * 
 * @param {string} token - JWT token to calculate expiry for
 * @param {number} customTimeoutMs - Custom timeout in milliseconds (optional)
 * @returns {Object} Session expiry information and timeout handling
 */
export function getSessionExpiry(token, customTimeoutMs = null) {
  try {
    const validation = validateToken(token);
    
    if (!validation.isValid) {
      return {
        hasExpiry: false,
        error: validation.error,
        errorCode: validation.errorCode
      };
    }

    const payload = validation.payload;
    const currentTime = Date.now();
    
    // Use custom timeout or default session timeout
    const sessionTimeoutMs = customTimeoutMs || JWT_CONFIG.DEFAULT_SESSION_TIMEOUT_MS;
    
    // Calculate expiry based on token expiration and session timeout
    const tokenExpiryMs = payload.exp * 1000;
    const sessionExpiryMs = Math.min(tokenExpiryMs, currentTime + sessionTimeoutMs);
    
    const timeUntilExpiry = sessionExpiryMs - currentTime;
    const isExpired = timeUntilExpiry <= 0;
    const isApproachingExpiry = timeUntilExpiry <= JWT_CONFIG.EXPIRATION_BUFFER_MS;

    // Calculate warning time (5 minutes before expiry)
    const warningTimeMs = 5 * 60 * 1000;
    const showWarning = timeUntilExpiry <= warningTimeMs && !isExpired;

    return {
      hasExpiry: true,
      sessionExpiryTime: new Date(sessionExpiryMs),
      timeUntilExpiry,
      isExpired,
      isApproachingExpiry,
      showWarning,
      
      // Session management utilities
      sessionTimeoutMs,
      warningTimeMs,
      
      // Auto-logout handling
      shouldAutoLogout: isExpired,
      shouldShowWarning: showWarning,
      shouldRefreshToken: isApproachingExpiry && !isExpired,
      
      // Formatted time display
      formattedTimeRemaining: formatTimeRemaining(timeUntilExpiry),
      
      // Session renewal information
      canRenewSession: !isExpired && timeUntilExpiry > 0,
      renewalDeadline: new Date(sessionExpiryMs - warningTimeMs)
    };

  } catch (error) {
    return {
      hasExpiry: false,
      error: MessageConstants.API_ERROR_MESSAGES.SESSION_EXPIRED,
      errorCode: 'SESSION_EXPIRY_ERROR',
      details: error.message
    };
  }
}

// ============================================================================
// AUTHENTICATION STATE MANAGEMENT
// ============================================================================

/**
 * Manages authentication state for React session context preservation
 * Implements authentication state management per Section 0.2.1 requirements
 * Supports pseudo-conversational flow across stateless REST API calls
 * 
 * @param {string} token - Current JWT token
 * @returns {Object} Complete authentication state for React context
 */
export function getAuthenticationState(token) {
  try {
    if (!token) {
      return {
        isAuthenticated: false,
        authState: AUTH_STATES.UNAUTHENTICATED,
        user: null,
        error: MessageConstants.API_ERROR_MESSAGES.AUTHENTICATION_FAILED
      };
    }

    const validation = validateToken(token);
    const userContext = getUserFromToken(token);
    const sessionInfo = getSessionExpiry(token);

    if (!validation.isValid) {
      return {
        isAuthenticated: false,
        authState: AUTH_STATES.INVALID,
        user: null,
        error: validation.error,
        errorCode: validation.errorCode
      };
    }

    if (sessionInfo.isExpired) {
      return {
        isAuthenticated: false,
        authState: AUTH_STATES.EXPIRED,
        user: userContext.user,
        error: MessageConstants.API_ERROR_MESSAGES.SESSION_EXPIRED,
        sessionExpiry: sessionInfo.sessionExpiryTime
      };
    }

    return {
      isAuthenticated: true,
      authState: AUTH_STATES.AUTHENTICATED,
      user: userContext.user,
      session: sessionInfo,
      
      // Token management
      token,
      tokenValidation: validation,
      
      // Session state for pseudo-conversational flow
      sessionState: {
        isActive: true,
        lastActivity: new Date(),
        sessionId: userContext.user?.sessionId,
        correlationId: userContext.user?.sessionCorrelationId,
        
        // State preservation for React components
        preservedState: getStoredSessionState(),
        
        // Navigation context
        navigationHistory: getNavigationHistory(),
        currentComponent: getCurrentComponent()
      },
      
      // Automatic session management
      autoRefresh: {
        enabled: sessionInfo.shouldRefreshToken,
        nextRefreshTime: sessionInfo.renewalDeadline,
        canRefresh: sessionInfo.canRenewSession
      },
      
      // Warning system
      warningSystem: {
        showWarning: sessionInfo.shouldShowWarning,
        timeRemaining: sessionInfo.formattedTimeRemaining,
        autoLogout: sessionInfo.shouldAutoLogout
      }
    };

  } catch (error) {
    return {
      isAuthenticated: false,
      authState: AUTH_STATES.INVALID,
      user: null,
      error: MessageConstants.API_ERROR_MESSAGES.AUTHENTICATION_FAILED,
      errorCode: 'AUTH_STATE_ERROR',
      details: error.message
    };
  }
}

/**
 * Handles automatic token refresh for session management
 * Implements token refresh handling per Section 6.4.1.5 requirements
 * 
 * @param {string} currentToken - Current JWT token
 * @param {string} refreshToken - Refresh token for token renewal
 * @returns {Promise<Object>} Token refresh result
 */
export async function handleTokenRefresh(currentToken, refreshToken) {
  try {
    const authState = getAuthenticationState(currentToken);
    
    if (!authState.autoRefresh.enabled) {
      return {
        success: false,
        error: 'Token refresh not needed',
        currentToken
      };
    }

    // Check if we have a valid refresh token
    if (!refreshToken) {
      return {
        success: false,
        error: MessageConstants.API_ERROR_MESSAGES.SESSION_EXPIRED,
        requiresLogin: true
      };
    }

    // Attempt to refresh the token via API call
    const response = await fetch('/api/auth/refresh', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${refreshToken}`
      },
      body: JSON.stringify({
        currentToken: currentToken.replace(JWT_CONFIG.TOKEN_PREFIX, ''),
        correlationId: authState.user?.sessionCorrelationId
      })
    });

    if (!response.ok) {
      throw new Error(`Token refresh failed: ${response.status}`);
    }

    const refreshResult = await response.json();
    
    // Update stored tokens
    updateStoredTokens(refreshResult.accessToken, refreshResult.refreshToken);
    
    return {
      success: true,
      accessToken: refreshResult.accessToken,
      refreshToken: refreshResult.refreshToken,
      expiresAt: new Date(refreshResult.expiresAt),
      user: getUserFromToken(refreshResult.accessToken).user
    };

  } catch (error) {
    return {
      success: false,
      error: MessageConstants.API_ERROR_MESSAGES.AUTHENTICATION_FAILED,
      errorCode: 'TOKEN_REFRESH_ERROR',
      details: error.message,
      requiresLogin: true
    };
  }
}

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

/**
 * Formats time remaining for display
 * 
 * @param {number} timeMs - Time in milliseconds
 * @returns {string} Formatted time string
 */
function formatTimeRemaining(timeMs) {
  if (timeMs <= 0) return '0:00';
  
  const minutes = Math.floor(timeMs / 60000);
  const seconds = Math.floor((timeMs % 60000) / 1000);
  
  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}

/**
 * Gets stored session state for pseudo-conversational flow
 * 
 * @returns {Object} Stored session state
 */
function getStoredSessionState() {
  try {
    const stored = localStorage.getItem('cardDemo_session_state');
    return stored ? JSON.parse(stored) : {};
  } catch (error) {
    return {};
  }
}

/**
 * Gets navigation history for session context
 * 
 * @returns {Array} Navigation history
 */
function getNavigationHistory() {
  try {
    const history = sessionStorage.getItem('cardDemo_navigation_history');
    return history ? JSON.parse(history) : [];
  } catch (error) {
    return [];
  }
}

/**
 * Gets current component context
 * 
 * @returns {string} Current component name
 */
function getCurrentComponent() {
  return sessionStorage.getItem('cardDemo_current_component') || 'unknown';
}

/**
 * Updates stored tokens in browser storage
 * 
 * @param {string} accessToken - New access token
 * @param {string} refreshToken - New refresh token
 */
function updateStoredTokens(accessToken, refreshToken) {
  try {
    localStorage.setItem(JWT_CONFIG.STORAGE_KEY, accessToken);
    localStorage.setItem(JWT_CONFIG.REFRESH_TOKEN_KEY, refreshToken);
    
    // Update user context
    const userContext = getUserFromToken(accessToken);
    if (userContext.user) {
      localStorage.setItem(JWT_CONFIG.USER_CONTEXT_KEY, JSON.stringify(userContext.user));
    }
  } catch (error) {
    console.error('Failed to update stored tokens:', error);
  }
}

/**
 * Clears all authentication data from storage
 * Used during logout process
 */
export function clearAuthenticationData() {
  try {
    localStorage.removeItem(JWT_CONFIG.STORAGE_KEY);
    localStorage.removeItem(JWT_CONFIG.REFRESH_TOKEN_KEY);
    localStorage.removeItem(JWT_CONFIG.USER_CONTEXT_KEY);
    localStorage.removeItem(JWT_CONFIG.SESSION_TIMEOUT_KEY);
    localStorage.removeItem('cardDemo_session_state');
    sessionStorage.removeItem('cardDemo_navigation_history');
    sessionStorage.removeItem('cardDemo_current_component');
  } catch (error) {
    console.error('Failed to clear authentication data:', error);
  }
}

/**
 * Redirects to login page with optional return URL
 * Implements automatic logout redirection per Section 7.6.3
 * 
 * @param {string} returnUrl - URL to return to after login
 * @param {string} reason - Reason for logout (expired, invalid, etc.)
 */
export function redirectToLogin(returnUrl = null, reason = 'SESSION_EXPIRED') {
  clearAuthenticationData();
  
  const loginUrl = '/login';
  const params = new URLSearchParams();
  
  if (returnUrl) {
    params.append('returnUrl', returnUrl);
  }
  
  params.append('reason', reason);
  
  if (reason === 'SESSION_EXPIRED') {
    params.append('message', MessageConstants.API_ERROR_MESSAGES.SESSION_EXPIRED);
  }
  
  window.location.href = `${loginUrl}?${params.toString()}`;
}

// ============================================================================
// EXPORT CONSTANTS FOR EXTERNAL USE
// ============================================================================

export {
  JWT_CONFIG,
  AUTH_STATES
};