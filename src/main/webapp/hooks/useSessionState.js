/**
 * useSessionState.js
 * 
 * Custom React hook implementing pseudo-conversational state management through JWT tokens 
 * and Redis session storage, providing session initialization, maintenance, timeout handling, 
 * and termination that preserves CICS terminal storage behavior while supporting stateless 
 * microservices architecture.
 * 
 * This hook replaces traditional CICS pseudo-conversational processing with modern
 * JWT-based authentication and Redis-backed session management, maintaining identical
 * session lifecycle patterns while enabling horizontal scaling and cloud-native deployment.
 * 
 * Key Features:
 * - JWT token management with automatic refresh and validation
 * - Redis session storage for complex state preservation across REST API calls
 * - Session timeout detection with configurable TTL expiration
 * - Error context preservation for recovery procedures
 * - Navigation state management with React Router integration
 * - Automatic logout redirection on session expiration
 * - Session lifecycle event handling equivalent to CICS transaction boundaries
 * 
 * @fileoverview CardDemo Session State Management Hook
 * @version 1.0.0
 * @author Blitzy Platform - CardDemo Migration Team  
 * @copyright 2024 CardDemo Application Migration Project
 * @license Apache-2.0
 */

import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { tokenManager } from '../utils/apiClient.js';
import { isTokenExpired } from '../utils/authUtils.js';

// ============================================================================
// SESSION MANAGEMENT CONSTANTS AND CONFIGURATION
// ============================================================================

/**
 * Session configuration constants aligned with CICS terminal timeout behavior
 * and Redis TTL management for distributed session storage
 */
const SESSION_CONFIG = {
  // Session timeout configuration (matching CICS terminal timeout)
  DEFAULT_SESSION_TIMEOUT_MINUTES: 30,
  SESSION_WARNING_THRESHOLD_MINUTES: 5,
  TOKEN_REFRESH_THRESHOLD_MINUTES: 5,
  
  // Session state storage keys for browser localStorage and Redis
  SESSION_STATE_KEY: 'carddemo_session_state',
  ERROR_CONTEXT_KEY: 'carddemo_error_context',
  NAVIGATION_CONTEXT_KEY: 'carddemo_navigation_context',
  
  // Session lifecycle event types
  SESSION_EVENTS: {
    INITIALIZED: 'session_initialized',
    REFRESHED: 'session_refreshed',
    WARNING: 'session_warning',
    EXPIRED: 'session_expired',
    TERMINATED: 'session_terminated',
    ERROR: 'session_error'
  },
  
  // Redis session management API endpoints
  API_ENDPOINTS: {
    SESSION_INIT: '/api/session/initialize',
    SESSION_REFRESH: '/api/session/refresh',
    SESSION_TERMINATE: '/api/session/terminate',
    SESSION_STATE: '/api/session/state',
    ERROR_CONTEXT: '/api/session/error-context'
  }
} as const;

// ============================================================================
// SESSION STATE INITIALIZATION AND MANAGEMENT
// ============================================================================

/**
 * Initializes a new session with JWT token validation and Redis session creation
 * Replaces CICS pseudo-conversational session initialization with modern authentication
 * 
 * @param {string} token - JWT authentication token from login process
 * @param {Object} userContext - User authentication context and profile information
 * @param {Object} navigationState - Initial navigation context for session routing
 * @returns {Promise<Object>} Session initialization result with success status and session data
 * 
 * Implementation Notes:
 * - Validates JWT token structure and expiration before session creation
 * - Creates Redis session entry with configurable TTL matching CICS timeout
 * - Establishes session correlation ID for distributed tracing across microservices
 * - Initializes error context storage for recovery procedures
 * - Sets up automatic session timeout detection and warning mechanisms
 */
export const initializeSession = async (token, userContext = null, navigationState = null) => {
  try {
    // Validate JWT token before session initialization
    if (!token || !tokenManager.isTokenValid(token) || isTokenExpired(token)) {
      console.warn('Cannot initialize session with invalid or expired token');
      return {
        success: false,
        error: 'INVALID_TOKEN',
        message: 'Session initialization failed: invalid authentication token'
      };
    }

    // Store JWT token in secure storage
    tokenManager.setToken(token);

    // Extract session correlation ID from JWT token for distributed tracing
    const tokenData = JSON.parse(atob(token.split('.')[1]));
    const sessionId = tokenData.jti || tokenData.session_id || `session-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;

    // Initialize session state object with CICS-equivalent context
    const initialSessionState = {
      sessionId: sessionId,
      userId: tokenData.user_id || tokenData.sub,
      userType: tokenData.user_type,
      roles: tokenData.roles || [],
      startTime: new Date().toISOString(),
      lastActivity: new Date().toISOString(),
      expiresAt: new Date(tokenData.exp * 1000).toISOString(),
      timeoutWarning: false,
      navigationHistory: navigationState ? [navigationState] : [],
      errorContext: null,
      sessionData: userContext || {}
    };

    // Create Redis session entry via API call
    const sessionResponse = await fetch(SESSION_CONFIG.API_ENDPOINTS.SESSION_INIT, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        'X-Session-ID': sessionId,
        'X-Request-Timestamp': new Date().toISOString()
      },
      body: JSON.stringify({
        sessionId: sessionId,
        userId: initialSessionState.userId,
        sessionState: initialSessionState,
        ttlMinutes: SESSION_CONFIG.DEFAULT_SESSION_TIMEOUT_MINUTES
      })
    });

    if (!sessionResponse.ok) {
      throw new Error(`Session initialization failed: ${sessionResponse.status} ${sessionResponse.statusText}`);
    }

    const sessionData = await sessionResponse.json();

    // Store session state in browser localStorage for client-side access
    localStorage.setItem(SESSION_CONFIG.SESSION_STATE_KEY, JSON.stringify(initialSessionState));

    // Set session ID in sessionStorage for API request headers
    sessionStorage.setItem('carddemo-session-id', sessionId);

    console.log('Session initialized successfully:', {
      sessionId: sessionId,
      userId: initialSessionState.userId,
      expiresAt: initialSessionState.expiresAt
    });

    return {
      success: true,
      sessionId: sessionId,
      sessionState: initialSessionState,
      serverResponse: sessionData
    };

  } catch (error) {
    console.error('Session initialization error:', error);
    
    // Clean up any partial session state on initialization failure
    tokenManager.removeToken();
    localStorage.removeItem(SESSION_CONFIG.SESSION_STATE_KEY);
    sessionStorage.removeItem('carddemo-session-id');

    return {
      success: false,
      error: 'INITIALIZATION_FAILED',
      message: error.message || 'Session initialization failed due to system error',
      details: error
    };
  }
};

/**
 * Refreshes current session with token renewal and Redis TTL extension
 * Maintains session continuity equivalent to CICS pseudo-conversational processing
 * 
 * @param {boolean} forceRefresh - Force token refresh even if not near expiration
 * @returns {Promise<Object>} Session refresh result with updated token and session data
 * 
 * Implementation Notes:
 * - Checks token expiration and performs automatic renewal when needed
 * - Extends Redis session TTL to prevent premature session expiration
 * - Updates session activity timestamp for accurate timeout calculation
 * - Preserves all session context and navigation history during refresh
 * - Handles token refresh failures with graceful degradation
 */
export const refreshSession = async (forceRefresh = false) => {
  try {
    const currentToken = tokenManager.getToken();
    
    if (!currentToken) {
      return {
        success: false,
        error: 'NO_TOKEN',
        message: 'No active session to refresh'
      };
    }

    // Check if token refresh is needed
    const tokenExpired = isTokenExpired(currentToken);
    const needsRefresh = forceRefresh || tokenExpired || tokenManager.getTokenExpiry() - Date.now() < (SESSION_CONFIG.TOKEN_REFRESH_THRESHOLD_MINUTES * 60 * 1000);

    if (!needsRefresh) {
      // Token is still valid, just update activity timestamp
      const sessionState = JSON.parse(localStorage.getItem(SESSION_CONFIG.SESSION_STATE_KEY) || '{}');
      sessionState.lastActivity = new Date().toISOString();
      localStorage.setItem(SESSION_CONFIG.SESSION_STATE_KEY, JSON.stringify(sessionState));
      
      return {
        success: true,
        refreshed: false,
        message: 'Session is still valid, no refresh needed'
      };
    }

    // Attempt token refresh
    const refreshResult = await tokenManager.refreshTokenIfNeeded();
    
    if (!refreshResult) {
      return {
        success: false,
        error: 'REFRESH_FAILED',
        message: 'Token refresh failed, session may be expired'
      };
    }

    // Get refreshed token and update session state
    const refreshedToken = tokenManager.getToken();
    const tokenData = JSON.parse(atob(refreshedToken.split('.')[1]));
    const sessionId = sessionStorage.getItem('carddemo-session-id');

    // Update session state with refreshed information
    const sessionState = JSON.parse(localStorage.getItem(SESSION_CONFIG.SESSION_STATE_KEY) || '{}');
    sessionState.lastActivity = new Date().toISOString();
    sessionState.expiresAt = new Date(tokenData.exp * 1000).toISOString();
    sessionState.timeoutWarning = false;

    // Refresh Redis session via API call
    const refreshResponse = await fetch(SESSION_CONFIG.API_ENDPOINTS.SESSION_REFRESH, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${refreshedToken}`,
        'X-Session-ID': sessionId,
        'X-Request-Timestamp': new Date().toISOString()
      },
      body: JSON.stringify({
        sessionId: sessionId,
        newExpiresAt: sessionState.expiresAt,
        ttlMinutes: SESSION_CONFIG.DEFAULT_SESSION_TIMEOUT_MINUTES
      })
    });

    if (refreshResponse.ok) {
      const refreshData = await refreshResponse.json();
      sessionState.sessionData = { ...sessionState.sessionData, ...refreshData.sessionData };
    }

    // Update stored session state
    localStorage.setItem(SESSION_CONFIG.SESSION_STATE_KEY, JSON.stringify(sessionState));

    console.log('Session refreshed successfully:', {
      sessionId: sessionId,
      newExpiresAt: sessionState.expiresAt
    });

    return {
      success: true,
      refreshed: true,
      sessionState: sessionState,
      message: 'Session refreshed successfully'
    };

  } catch (error) {
    console.error('Session refresh error:', error);
    
    return {
      success: false,
      error: 'REFRESH_ERROR',
      message: error.message || 'Session refresh failed due to system error',
      details: error
    };
  }
};

/**
 * Terminates current session with complete cleanup and logout redirection
 * Implements secure logout equivalent to CICS transaction termination
 * 
 * @param {string} reason - Reason for session termination (logout, timeout, error)
 * @param {boolean} redirectToLogin - Whether to redirect to login screen after termination
 * @returns {Promise<Object>} Session termination result with cleanup confirmation
 * 
 * Implementation Notes:
 * - Invalidates JWT token and clears all browser authentication state
 * - Removes Redis session entry and clears distributed session data
 * - Cleans up error context and navigation history storage
 * - Provides automatic redirection to login screen when requested
 * - Logs session termination event for audit trail compliance
 */
export const terminateSession = async (reason = 'logout', redirectToLogin = true) => {
  try {
    const sessionId = sessionStorage.getItem('carddemo-session-id');
    const currentToken = tokenManager.getToken();

    // Attempt to terminate Redis session via API call
    if (sessionId && currentToken) {
      try {
        await fetch(SESSION_CONFIG.API_ENDPOINTS.SESSION_TERMINATE, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${currentToken}`,
            'X-Session-ID': sessionId,
            'X-Request-Timestamp': new Date().toISOString()
          },
          body: JSON.stringify({
            sessionId: sessionId,
            terminationReason: reason,
            timestamp: new Date().toISOString()
          })
        });
      } catch (apiError) {
        console.warn('Failed to terminate server session, continuing with client cleanup:', apiError);
      }
    }

    // Clear all authentication and session state from browser storage
    tokenManager.removeToken();
    localStorage.removeItem(SESSION_CONFIG.SESSION_STATE_KEY);
    localStorage.removeItem(SESSION_CONFIG.ERROR_CONTEXT_KEY);
    localStorage.removeItem(SESSION_CONFIG.NAVIGATION_CONTEXT_KEY);
    sessionStorage.removeItem('carddemo-session-id');
    sessionStorage.clear();

    console.log('Session terminated successfully:', {
      sessionId: sessionId,
      reason: reason,
      timestamp: new Date().toISOString()
    });

    return {
      success: true,
      sessionId: sessionId,
      reason: reason,
      message: 'Session terminated successfully',
      redirectToLogin: redirectToLogin
    };

  } catch (error) {
    console.error('Session termination error:', error);
    
    // Force cleanup even if termination fails
    tokenManager.removeToken();
    localStorage.clear();
    sessionStorage.clear();

    return {
      success: false,
      error: 'TERMINATION_ERROR',
      message: error.message || 'Session termination encountered errors but cleanup completed',
      details: error,
      redirectToLogin: redirectToLogin
    };
  }
};

// ============================================================================
// MAIN SESSION STATE HOOK IMPLEMENTATION
// ============================================================================

/**
 * Main useSessionState hook providing comprehensive session management for CardDemo React application
 * Implements pseudo-conversational state management replacing CICS terminal storage behavior
 * 
 * @returns {Object} Session state and management functions
 * 
 * Returned Object Structure:
 * - sessionState: Current session information and user context
 * - isAuthenticated: Boolean indicating valid authentication status
 * - isSessionWarning: Boolean indicating session timeout warning threshold
 * - sessionTimeRemaining: Minutes remaining until session expiration
 * - initialize: Function to initialize new session with authentication token
 * - refresh: Function to refresh current session and extend TTL
 * - terminate: Function to terminate session and perform cleanup
 * - updateSessionData: Function to update session context data
 * - preserveErrorContext: Function to store error state for recovery
 * - clearErrorContext: Function to clear stored error state
 * 
 * Hook Features:
 * - Automatic session timeout detection with configurable warning thresholds
 * - JWT token validation and automatic refresh capabilities
 * - Redis session state synchronization across browser tabs and instances
 * - Error context preservation for user experience continuity
 * - Navigation state management with React Router integration
 * - Real-time session expiry countdown and user notifications
 */
export default function useSessionState() {
  // Session state management with React hooks
  const [sessionState, setSessionState] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isSessionWarning, setIsSessionWarning] = useState(false);
  const [sessionTimeRemaining, setSessionTimeRemaining] = useState(0);
  const [errorContext, setErrorContext] = useState(null);

  // Navigation hook for automatic redirection
  const navigate = useNavigate();

  // Refs for timeout management and cleanup
  const timeoutCheckInterval = useRef(null);
  const sessionRefreshTimer = useRef(null);

  /**
   * Loads existing session state from browser storage and validates authentication
   * Restores session context after page refresh or browser restart
   */
  const loadExistingSession = useCallback(async () => {
    try {
      const storedSessionState = localStorage.getItem(SESSION_CONFIG.SESSION_STATE_KEY);
      const currentToken = tokenManager.getToken();

      if (!storedSessionState || !currentToken) {
        setIsAuthenticated(false);
        setSessionState(null);
        return;
      }

      const parsedSessionState = JSON.parse(storedSessionState);
      
      // Validate token and session expiration
      if (isTokenExpired(currentToken) || new Date() > new Date(parsedSessionState.expiresAt)) {
        console.log('Stored session has expired, terminating...');
        await terminateSession('expired', true);
        navigate('/login?reason=session-expired');
        return;
      }

      // Restore session state
      setSessionState(parsedSessionState);
      setIsAuthenticated(true);

      // Calculate time remaining
      const expirationTime = new Date(parsedSessionState.expiresAt).getTime();
      const currentTime = Date.now();
      const remainingMs = expirationTime - currentTime;
      const remainingMinutes = Math.floor(remainingMs / (1000 * 60));
      
      setSessionTimeRemaining(Math.max(0, remainingMinutes));
      setIsSessionWarning(remainingMinutes <= SESSION_CONFIG.SESSION_WARNING_THRESHOLD_MINUTES);

      // Load error context if available
      const storedErrorContext = localStorage.getItem(SESSION_CONFIG.ERROR_CONTEXT_KEY);
      if (storedErrorContext) {
        setErrorContext(JSON.parse(storedErrorContext));
      }

      console.log('Existing session loaded successfully:', {
        sessionId: parsedSessionState.sessionId,
        userId: parsedSessionState.userId,
        remainingMinutes: remainingMinutes
      });

    } catch (error) {
      console.error('Error loading existing session:', error);
      setIsAuthenticated(false);
      setSessionState(null);
    }
  }, [navigate]);

  /**
   * Starts session timeout monitoring with automatic refresh and warning detection
   * Implements CICS-equivalent session timeout behavior with proactive token refresh
   */
  const startSessionMonitoring = useCallback(() => {
    // Clear any existing intervals
    if (timeoutCheckInterval.current) {
      clearInterval(timeoutCheckInterval.current);
    }
    if (sessionRefreshTimer.current) {
      clearTimeout(sessionRefreshTimer.current);
    }

    // Session timeout check every 30 seconds
    timeoutCheckInterval.current = setInterval(async () => {
      const currentToken = tokenManager.getToken();
      const storedSessionState = localStorage.getItem(SESSION_CONFIG.SESSION_STATE_KEY);

      if (!currentToken || !storedSessionState) {
        console.log('No active session to monitor');
        setIsAuthenticated(false);
        return;
      }

      try {
        const parsedSessionState = JSON.parse(storedSessionState);
        const expirationTime = new Date(parsedSessionState.expiresAt).getTime();
        const currentTime = Date.now();
        const remainingMs = expirationTime - currentTime;
        const remainingMinutes = Math.floor(remainingMs / (1000 * 60));

        setSessionTimeRemaining(Math.max(0, remainingMinutes));

        // Check for session expiration
        if (remainingMs <= 0 || isTokenExpired(currentToken)) {
          console.log('Session has expired, terminating...');
          await terminateSession('expired', true);
          navigate('/login?reason=session-expired');
          return;
        }

        // Check for warning threshold
        const shouldShowWarning = remainingMinutes <= SESSION_CONFIG.SESSION_WARNING_THRESHOLD_MINUTES;
        setIsSessionWarning(shouldShowWarning);

        // Automatic refresh when approaching expiration
        const shouldRefresh = remainingMinutes <= SESSION_CONFIG.TOKEN_REFRESH_THRESHOLD_MINUTES;
        if (shouldRefresh && !sessionRefreshTimer.current) {
          console.log('Automatically refreshing session due to approaching expiration');
          sessionRefreshTimer.current = setTimeout(async () => {
            await refreshSession(false);
            sessionRefreshTimer.current = null;
          }, 1000);
        }

      } catch (error) {
        console.error('Session monitoring error:', error);
      }
    }, 30000); // Check every 30 seconds

  }, [navigate]);

  /**
   * Initialize session with authentication token and user context
   * Wrapper function for external session initialization
   */
  const initialize = useCallback(async (token, userContext = null, navigationState = null) => {
    const result = await initializeSession(token, userContext, navigationState);
    
    if (result.success) {
      setSessionState(result.sessionState);
      setIsAuthenticated(true);
      startSessionMonitoring();
    } else {
      setIsAuthenticated(false);
      setSessionState(null);
    }
    
    return result;
  }, [startSessionMonitoring]);

  /**
   * Refresh current session and extend TTL
   * Wrapper function for external session refresh
   */
  const refresh = useCallback(async (forceRefresh = false) => {
    const result = await refreshSession(forceRefresh);
    
    if (result.success && result.refreshed) {
      setSessionState(result.sessionState);
      // Reset refresh timer since we just refreshed
      if (sessionRefreshTimer.current) {
        clearTimeout(sessionRefreshTimer.current);
        sessionRefreshTimer.current = null;
      }
    }
    
    return result;
  }, []);

  /**
   * Terminate current session with cleanup and optional redirection
   * Wrapper function for external session termination
   */
  const terminate = useCallback(async (reason = 'logout', redirectToLogin = true) => {
    const result = await terminateSession(reason, redirectToLogin);
    
    // Clear local state regardless of API result
    setSessionState(null);
    setIsAuthenticated(false);
    setSessionTimeRemaining(0);
    setIsSessionWarning(false);
    setErrorContext(null);
    
    // Clear monitoring timers
    if (timeoutCheckInterval.current) {
      clearInterval(timeoutCheckInterval.current);
      timeoutCheckInterval.current = null;
    }
    if (sessionRefreshTimer.current) {
      clearTimeout(sessionRefreshTimer.current);
      sessionRefreshTimer.current = null;
    }
    
    // Redirect to login if requested
    if (result.redirectToLogin) {
      const queryParam = reason === 'expired' ? '?reason=session-expired' : '?reason=logout';
      navigate(`/login${queryParam}`);
    }
    
    return result;
  }, [navigate]);

  /**
   * Update session data with new context information
   * Preserves session state while updating specific data elements
   */
  const updateSessionData = useCallback(async (newData) => {
    try {
      if (!sessionState) {
        return { success: false, error: 'NO_SESSION', message: 'No active session to update' };
      }

      const updatedSessionState = {
        ...sessionState,
        sessionData: { ...sessionState.sessionData, ...newData },
        lastActivity: new Date().toISOString()
      };

      // Update local storage
      localStorage.setItem(SESSION_CONFIG.SESSION_STATE_KEY, JSON.stringify(updatedSessionState));
      
      // Update React state
      setSessionState(updatedSessionState);

      // Optionally sync with Redis session (fire and forget)
      const sessionId = sessionStorage.getItem('carddemo-session-id');
      const currentToken = tokenManager.getToken();
      
      if (sessionId && currentToken) {
        fetch(SESSION_CONFIG.API_ENDPOINTS.SESSION_STATE, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${currentToken}`,
            'X-Session-ID': sessionId
          },
          body: JSON.stringify({
            sessionData: updatedSessionState.sessionData,
            lastActivity: updatedSessionState.lastActivity
          })
        }).catch(error => {
          console.warn('Failed to sync session data with server:', error);
        });
      }

      return { success: true, sessionState: updatedSessionState };

    } catch (error) {
      console.error('Error updating session data:', error);
      return { 
        success: false, 
        error: 'UPDATE_FAILED', 
        message: error.message || 'Failed to update session data' 
      };
    }
  }, [sessionState]);

  /**
   * Preserve error context for recovery procedures
   * Stores error state with TTL expiration for user experience continuity
   */
  const preserveErrorContext = useCallback(async (errorInfo) => {
    try {
      const errorContext = {
        error: errorInfo,
        timestamp: new Date().toISOString(),
        sessionId: sessionState?.sessionId,
        userId: sessionState?.userId,
        expiresAt: new Date(Date.now() + (30 * 60 * 1000)).toISOString() // 30 minute TTL
      };

      // Store in local storage
      localStorage.setItem(SESSION_CONFIG.ERROR_CONTEXT_KEY, JSON.stringify(errorContext));
      setErrorContext(errorContext);

      // Store in Redis session for distributed access
      const sessionId = sessionStorage.getItem('carddemo-session-id');
      const currentToken = tokenManager.getToken();
      
      if (sessionId && currentToken) {
        fetch(SESSION_CONFIG.API_ENDPOINTS.ERROR_CONTEXT, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${currentToken}`,
            'X-Session-ID': sessionId
          },
          body: JSON.stringify(errorContext)
        }).catch(error => {
          console.warn('Failed to store error context on server:', error);
        });
      }

      return { success: true, errorContext: errorContext };

    } catch (error) {
      console.error('Error preserving error context:', error);
      return { 
        success: false, 
        error: 'PRESERVE_FAILED', 
        message: error.message || 'Failed to preserve error context' 
      };
    }
  }, [sessionState]);

  /**
   * Clear stored error context
   * Removes error state from both local and distributed storage
   */
  const clearErrorContext = useCallback(async () => {
    try {
      // Clear local storage
      localStorage.removeItem(SESSION_CONFIG.ERROR_CONTEXT_KEY);
      setErrorContext(null);

      // Clear Redis session error context
      const sessionId = sessionStorage.getItem('carddemo-session-id');
      const currentToken = tokenManager.getToken();
      
      if (sessionId && currentToken) {
        fetch(SESSION_CONFIG.API_ENDPOINTS.ERROR_CONTEXT, {
          method: 'DELETE',
          headers: {
            'Authorization': `Bearer ${currentToken}`,
            'X-Session-ID': sessionId
          }
        }).catch(error => {
          console.warn('Failed to clear error context on server:', error);
        });
      }

      return { success: true };

    } catch (error) {
      console.error('Error clearing error context:', error);
      return { 
        success: false, 
        error: 'CLEAR_FAILED', 
        message: error.message || 'Failed to clear error context' 
      };
    }
  }, []);

  // Initialize session state on hook mount
  useEffect(() => {
    loadExistingSession();
  }, [loadExistingSession]);

  // Start session monitoring when authenticated
  useEffect(() => {
    if (isAuthenticated && sessionState) {
      startSessionMonitoring();
    }

    // Cleanup timers on unmount or when authentication changes
    return () => {
      if (timeoutCheckInterval.current) {
        clearInterval(timeoutCheckInterval.current);
      }
      if (sessionRefreshTimer.current) {
        clearTimeout(sessionRefreshTimer.current);
      }
    };
  }, [isAuthenticated, sessionState, startSessionMonitoring]);

  // Return session state and management functions
  return {
    // Session state information
    sessionState,
    isAuthenticated,
    isSessionWarning,
    sessionTimeRemaining,
    errorContext,

    // Session management functions
    initialize,
    refresh,
    terminate,
    updateSessionData,
    preserveErrorContext,
    clearErrorContext
  };
}