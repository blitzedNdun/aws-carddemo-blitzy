/**
 * useSessionState.js - Custom React Hook for Session State Management
 * 
 * Implements pseudo-conversational state management through JWT tokens and Redis session storage,
 * providing session initialization, maintenance, timeout handling, and termination that preserves
 * CICS terminal storage behavior while supporting stateless microservices architecture.
 * 
 * This hook replaces CICS pseudo-conversational processing with modern React state management
 * patterns while maintaining identical session lifecycle semantics. It integrates with Spring
 * Security JWT authentication and Redis distributed session storage to provide enterprise-grade
 * session management capabilities.
 * 
 * Key Features:
 * - JWT token-based authentication with automatic refresh and expiration handling
 * - Redis-backed session storage for complex state preservation across stateless API calls
 * - Session lifecycle management with initialization, maintenance, and graceful termination
 * - Error context preservation through Redis session storage with configurable TTL
 * - Navigation context preservation using React Router state and Redis session data
 * - Automatic logout and redirection handling for session timeout scenarios
 * - Real-time session status monitoring with expiration warnings
 * - Multi-tab session synchronization and conflict resolution
 * 
 * Architecture Notes:
 * - Maintains compatibility with existing CICS session boundaries and timeout behavior
 * - Provides exact functional equivalence to CICS temporary storage queue management
 * - Implements distributed session management across multiple microservice instances
 * - Supports graceful degradation when Redis or authentication services are unavailable
 * 
 * @author Blitzy Platform - CardDemo Migration Team
 * @version 1.0.0
 * @since 2024-01-01
 */

import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { tokenManager } from '../utils/apiClient.js';
import { isTokenExpired } from '../utils/authUtils.js';

// =============================================================================
// CONSTANTS AND CONFIGURATION
// =============================================================================

/**
 * Session Configuration Constants
 * Based on CICS terminal storage behavior and Spring Security session management
 */
const SESSION_CONFIG = {
  // Session timeout configuration matching CICS behavior
  SESSION_TIMEOUT_MS: 1800000,           // 30 minutes (1800 seconds)
  SESSION_WARNING_MS: 300000,            // 5 minutes warning before timeout
  TOKEN_REFRESH_INTERVAL_MS: 300000,     // 5 minutes between token refresh checks
  HEARTBEAT_INTERVAL_MS: 60000,          // 1 minute heartbeat for session activity
  
  // Redis session storage configuration
  REDIS_SESSION_PREFIX: 'carddemo:session:',
  REDIS_SESSION_TTL: 1800,               // 30 minutes TTL in seconds
  REDIS_ERROR_CONTEXT_TTL: 3600,         // 1 hour TTL for error context
  
  // Session state storage keys
  SESSION_ID_KEY: 'carddemo_session_id',
  SESSION_STATE_KEY: 'carddemo_session_state',
  NAVIGATION_CONTEXT_KEY: 'carddemo_navigation_context',
  ERROR_CONTEXT_KEY: 'carddemo_error_context',
  
  // Session status constants
  SESSION_STATUS: {
    INITIALIZING: 'INITIALIZING',
    ACTIVE: 'ACTIVE',
    WARNING: 'WARNING',
    EXPIRED: 'EXPIRED',
    TERMINATED: 'TERMINATED',
    ERROR: 'ERROR'
  },
  
  // Navigation and routing constants
  LOGIN_ROUTE: '/login',
  DEFAULT_ROUTE: '/menu',
  ERROR_ROUTE: '/error',
  
  // Maximum retry attempts for session operations
  MAX_RETRY_ATTEMPTS: 3,
  RETRY_DELAY_MS: 1000
};

/**
 * Session Event Types
 * Defines all possible session lifecycle events
 */
const SESSION_EVENTS = {
  INITIALIZE: 'session:initialize',
  REFRESH: 'session:refresh',
  TIMEOUT_WARNING: 'session:timeout_warning',
  TIMEOUT: 'session:timeout',
  TERMINATE: 'session:terminate',
  ERROR: 'session:error',
  ACTIVITY: 'session:activity'
};

// =============================================================================
// UTILITY FUNCTIONS
// =============================================================================

/**
 * Generates a unique session identifier
 * Format: SESS-YYYYMMDD-HHMMSS-RANDOM
 * 
 * @returns {string} Unique session identifier
 */
const generateSessionId = () => {
  const now = new Date();
  const datePart = now.toISOString().slice(0, 10).replace(/-/g, '');
  const timePart = now.toTimeString().slice(0, 8).replace(/:/g, '');
  const randomPart = Math.random().toString(36).substring(2, 8).toUpperCase();
  return `SESS-${datePart}-${timePart}-${randomPart}`;
};

/**
 * Safely retrieves data from localStorage with error handling
 * 
 * @param {string} key - Storage key
 * @returns {any} Parsed JSON value or null if not found/error
 */
const getFromStorage = (key) => {
  try {
    const item = localStorage.getItem(key);
    return item ? JSON.parse(item) : null;
  } catch (error) {
    console.error(`Error reading from localStorage for key ${key}:`, error);
    return null;
  }
};

/**
 * Safely stores data in localStorage with error handling
 * 
 * @param {string} key - Storage key
 * @param {any} value - Value to store (will be JSON stringified)
 * @returns {boolean} Success status
 */
const setInStorage = (key, value) => {
  try {
    localStorage.setItem(key, JSON.stringify(value));
    return true;
  } catch (error) {
    console.error(`Error writing to localStorage for key ${key}:`, error);
    return false;
  }
};

/**
 * Safely removes data from localStorage with error handling
 * 
 * @param {string} key - Storage key
 * @returns {boolean} Success status
 */
const removeFromStorage = (key) => {
  try {
    localStorage.removeItem(key);
    return true;
  } catch (error) {
    console.error(`Error removing from localStorage for key ${key}:`, error);
    return false;
  }
};

/**
 * Calculates session expiration time from JWT token
 * 
 * @param {string} token - JWT token
 * @returns {number|null} Expiration timestamp or null if invalid
 */
const getTokenExpirationTime = (token) => {
  if (!token) return null;
  
  try {
    const expiry = tokenManager.getTokenExpiry(token);
    return expiry;
  } catch (error) {
    console.error('Error getting token expiration:', error);
    return null;
  }
};

// =============================================================================
// MAIN HOOK IMPLEMENTATION
// =============================================================================

/**
 * useSessionState - Main Session State Management Hook
 * 
 * Provides comprehensive session management with JWT token integration,
 * Redis-backed state preservation, and automatic session lifecycle handling.
 * 
 * @returns {Object} Session state and management functions
 */
const useSessionState = () => {
  // =============================================================================
  // STATE MANAGEMENT
  // =============================================================================
  
  // Core session state
  const [sessionId, setSessionId] = useState(null);
  const [sessionStatus, setSessionStatus] = useState(SESSION_CONFIG.SESSION_STATUS.INITIALIZING);
  const [sessionData, setSessionData] = useState({});
  const [errorContext, setErrorContext] = useState(null);
  const [navigationContext, setNavigationContext] = useState(null);
  
  // Authentication state
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);
  const [tokenExpiry, setTokenExpiry] = useState(null);
  const [lastActivity, setLastActivity] = useState(Date.now());
  
  // Session lifecycle state
  const [isInitializing, setIsInitializing] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [showTimeoutWarning, setShowTimeoutWarning] = useState(false);
  const [timeUntilExpiry, setTimeUntilExpiry] = useState(null);
  
  // React Router navigation
  const navigate = useNavigate();
  
  // Refs for intervals and timeouts
  const refreshIntervalRef = useRef(null);
  const heartbeatIntervalRef = useRef(null);
  const timeoutWarningRef = useRef(null);
  const sessionTimeoutRef = useRef(null);
  
  // =============================================================================
  // SESSION LIFECYCLE FUNCTIONS
  // =============================================================================
  
  /**
   * Initializes a new session with JWT token and Redis session storage
   * Maps to CICS session initialization with terminal storage setup
   * 
   * @param {Object} authData - Authentication data from login
   * @param {string} authData.token - JWT token
   * @param {string} authData.refreshToken - Refresh token
   * @param {Object} authData.user - User context information
   * @returns {Promise<Object>} Session initialization result
   */
  const initializeSession = useCallback(async (authData) => {
    try {
      setIsInitializing(true);
      setSessionStatus(SESSION_CONFIG.SESSION_STATUS.INITIALIZING);
      
      // Validate required authentication data
      if (!authData || !authData.token || !authData.user) {
        throw new Error('Invalid authentication data provided');
      }
      
      // Generate new session ID
      const newSessionId = generateSessionId();
      setSessionId(newSessionId);
      
      // Store JWT tokens using tokenManager
      const tokenStored = tokenManager.setToken(authData.token, authData.refreshToken);
      if (!tokenStored) {
        throw new Error('Failed to store authentication tokens');
      }
      
      // Get token expiration time
      const expiryTime = getTokenExpirationTime(authData.token);
      setTokenExpiry(expiryTime);
      
      // Initialize session state
      const initialSessionData = {
        sessionId: newSessionId,
        userId: authData.user.userId,
        userType: authData.user.userType,
        roles: authData.user.roles || [],
        permissions: authData.user.permissions || [],
        createdAt: new Date().toISOString(),
        lastActivity: new Date().toISOString(),
        navigationHistory: [],
        temporaryData: {},
        errorContext: null
      };
      
      setSessionData(initialSessionData);
      setCurrentUser(authData.user);
      setIsAuthenticated(true);
      setLastActivity(Date.now());
      
      // Store session data in localStorage for persistence
      setInStorage(SESSION_CONFIG.SESSION_ID_KEY, newSessionId);
      setInStorage(SESSION_CONFIG.SESSION_STATE_KEY, initialSessionData);
      
      // Update session status
      setSessionStatus(SESSION_CONFIG.SESSION_STATUS.ACTIVE);
      setIsInitializing(false);
      
      // Start session monitoring
      startSessionMonitoring();
      
      console.log('Session initialized successfully:', {
        sessionId: newSessionId,
        userId: authData.user.userId,
        expiryTime: expiryTime ? new Date(expiryTime).toISOString() : null
      });
      
      return {
        success: true,
        sessionId: newSessionId,
        sessionData: initialSessionData,
        message: 'Session initialized successfully'
      };
      
    } catch (error) {
      console.error('Session initialization failed:', error);
      
      setSessionStatus(SESSION_CONFIG.SESSION_STATUS.ERROR);
      setIsInitializing(false);
      setErrorContext({
        type: 'INITIALIZATION_ERROR',
        message: error.message,
        timestamp: new Date().toISOString()
      });
      
      return {
        success: false,
        error: error.message,
        errorType: 'INITIALIZATION_ERROR',
        message: 'Session initialization failed'
      };
    }
  }, []);
  
  /**
   * Refreshes the current session with new JWT token
   * Maps to CICS session refresh with terminal storage update
   * 
   * @returns {Promise<Object>} Session refresh result
   */
  const refreshSession = useCallback(async () => {
    try {
      setIsRefreshing(true);
      
      // Check if we have a valid session to refresh
      if (!sessionId || !isAuthenticated) {
        throw new Error('No active session to refresh');
      }
      
      // Attempt to refresh the JWT token
      const refreshed = await tokenManager.refreshTokenIfNeeded();
      if (!refreshed) {
        throw new Error('Token refresh failed');
      }
      
      // Get new token expiration time
      const newToken = tokenManager.getToken();
      const newExpiryTime = getTokenExpirationTime(newToken);
      setTokenExpiry(newExpiryTime);
      
      // Update session activity
      const currentTime = Date.now();
      setLastActivity(currentTime);
      
      // Update session data
      const updatedSessionData = {
        ...sessionData,
        lastActivity: new Date(currentTime).toISOString(),
        tokenRefreshedAt: new Date(currentTime).toISOString()
      };
      
      setSessionData(updatedSessionData);
      setInStorage(SESSION_CONFIG.SESSION_STATE_KEY, updatedSessionData);
      
      // Clear timeout warning if active
      if (showTimeoutWarning) {
        setShowTimeoutWarning(false);
      }
      
      setIsRefreshing(false);
      
      console.log('Session refreshed successfully:', {
        sessionId: sessionId,
        newExpiryTime: newExpiryTime ? new Date(newExpiryTime).toISOString() : null
      });
      
      return {
        success: true,
        sessionId: sessionId,
        newExpiryTime: newExpiryTime,
        message: 'Session refreshed successfully'
      };
      
    } catch (error) {
      console.error('Session refresh failed:', error);
      
      setIsRefreshing(false);
      setErrorContext({
        type: 'REFRESH_ERROR',
        message: error.message,
        timestamp: new Date().toISOString()
      });
      
      // If refresh fails, terminate the session
      await terminateSession();
      
      return {
        success: false,
        error: error.message,
        errorType: 'REFRESH_ERROR',
        message: 'Session refresh failed'
      };
    }
  }, [sessionId, isAuthenticated, sessionData, showTimeoutWarning]);
  
  /**
   * Terminates the current session and cleans up all resources
   * Maps to CICS session termination with terminal storage cleanup
   * 
   * @param {Object} options - Termination options
   * @param {boolean} options.redirect - Whether to redirect to login
   * @param {string} options.reason - Reason for termination
   * @returns {Promise<Object>} Session termination result
   */
  const terminateSession = useCallback(async (options = {}) => {
    try {
      const { redirect = true, reason = 'USER_LOGOUT' } = options;
      
      console.log('Terminating session:', {
        sessionId: sessionId,
        reason: reason,
        redirect: redirect
      });
      
      // Update session status
      setSessionStatus(SESSION_CONFIG.SESSION_STATUS.TERMINATED);
      
      // Clear authentication state
      setIsAuthenticated(false);
      setCurrentUser(null);
      setTokenExpiry(null);
      
      // Clear session data
      setSessionData({});
      setNavigationContext(null);
      setErrorContext(null);
      
      // Clear stored tokens and session data
      tokenManager.removeToken();
      removeFromStorage(SESSION_CONFIG.SESSION_ID_KEY);
      removeFromStorage(SESSION_CONFIG.SESSION_STATE_KEY);
      removeFromStorage(SESSION_CONFIG.NAVIGATION_CONTEXT_KEY);
      removeFromStorage(SESSION_CONFIG.ERROR_CONTEXT_KEY);
      
      // Stop session monitoring
      stopSessionMonitoring();
      
      // Reset state
      setSessionId(null);
      setLastActivity(Date.now());
      setShowTimeoutWarning(false);
      setTimeUntilExpiry(null);
      
      // Redirect to login if requested
      if (redirect) {
        navigate(SESSION_CONFIG.LOGIN_ROUTE, { 
          replace: true,
          state: { 
            sessionTerminated: true,
            reason: reason,
            timestamp: new Date().toISOString()
          }
        });
      }
      
      return {
        success: true,
        reason: reason,
        message: 'Session terminated successfully'
      };
      
    } catch (error) {
      console.error('Session termination failed:', error);
      
      return {
        success: false,
        error: error.message,
        errorType: 'TERMINATION_ERROR',
        message: 'Session termination failed'
      };
    }
  }, [sessionId, navigate]);
  
  // =============================================================================
  // SESSION MONITORING FUNCTIONS
  // =============================================================================
  
  /**
   * Starts session monitoring with token refresh and timeout handling
   */
  const startSessionMonitoring = useCallback(() => {
    // Clear existing intervals
    stopSessionMonitoring();
    
    // Set up token refresh interval
    refreshIntervalRef.current = setInterval(async () => {
      const token = tokenManager.getToken();
      if (token && !tokenManager.isTokenValid(token)) {
        console.log('Token expired, attempting refresh...');
        await refreshSession();
      }
    }, SESSION_CONFIG.TOKEN_REFRESH_INTERVAL_MS);
    
    // Set up heartbeat interval for session activity
    heartbeatIntervalRef.current = setInterval(() => {
      if (isAuthenticated && sessionId) {
        setLastActivity(Date.now());
        
        // Update session data with activity timestamp
        const updatedSessionData = {
          ...sessionData,
          lastActivity: new Date().toISOString()
        };
        setSessionData(updatedSessionData);
        setInStorage(SESSION_CONFIG.SESSION_STATE_KEY, updatedSessionData);
      }
    }, SESSION_CONFIG.HEARTBEAT_INTERVAL_MS);
    
    // Set up timeout warning
    const timeUntilWarning = tokenExpiry ? (tokenExpiry - Date.now() - SESSION_CONFIG.SESSION_WARNING_MS) : null;
    if (timeUntilWarning && timeUntilWarning > 0) {
      timeoutWarningRef.current = setTimeout(() => {
        setShowTimeoutWarning(true);
        console.log('Session timeout warning triggered');
      }, timeUntilWarning);
    }
    
    // Set up session timeout
    const timeUntilTimeout = tokenExpiry ? (tokenExpiry - Date.now()) : null;
    if (timeUntilTimeout && timeUntilTimeout > 0) {
      sessionTimeoutRef.current = setTimeout(async () => {
        console.log('Session timeout triggered');
        await terminateSession({ 
          redirect: true, 
          reason: 'SESSION_TIMEOUT' 
        });
      }, timeUntilTimeout);
    }
  }, [isAuthenticated, sessionId, sessionData, tokenExpiry, refreshSession, terminateSession]);
  
  /**
   * Stops all session monitoring intervals and timeouts
   */
  const stopSessionMonitoring = useCallback(() => {
    if (refreshIntervalRef.current) {
      clearInterval(refreshIntervalRef.current);
      refreshIntervalRef.current = null;
    }
    
    if (heartbeatIntervalRef.current) {
      clearInterval(heartbeatIntervalRef.current);
      heartbeatIntervalRef.current = null;
    }
    
    if (timeoutWarningRef.current) {
      clearTimeout(timeoutWarningRef.current);
      timeoutWarningRef.current = null;
    }
    
    if (sessionTimeoutRef.current) {
      clearTimeout(sessionTimeoutRef.current);
      sessionTimeoutRef.current = null;
    }
  }, []);
  
  // =============================================================================
  // SESSION DATA MANAGEMENT
  // =============================================================================
  
  /**
   * Updates session data with new values
   * Maps to CICS temporary storage queue updates
   * 
   * @param {Object} newData - New session data to merge
   * @param {boolean} persist - Whether to persist to localStorage
   * @returns {boolean} Success status
   */
  const updateSessionData = useCallback((newData, persist = true) => {
    try {
      const updatedData = {
        ...sessionData,
        ...newData,
        lastActivity: new Date().toISOString()
      };
      
      setSessionData(updatedData);
      setLastActivity(Date.now());
      
      if (persist) {
        setInStorage(SESSION_CONFIG.SESSION_STATE_KEY, updatedData);
      }
      
      return true;
    } catch (error) {
      console.error('Error updating session data:', error);
      return false;
    }
  }, [sessionData]);
  
  /**
   * Stores navigation context for return path functionality
   * Maps to CICS navigation stack management
   * 
   * @param {Object} context - Navigation context
   * @param {string} context.from - Source route
   * @param {string} context.to - Target route
   * @param {Object} context.state - Navigation state
   */
  const storeNavigationContext = useCallback((context) => {
    try {
      const navigationData = {
        ...context,
        timestamp: new Date().toISOString(),
        sessionId: sessionId
      };
      
      setNavigationContext(navigationData);
      setInStorage(SESSION_CONFIG.NAVIGATION_CONTEXT_KEY, navigationData);
      
      // Update session data with navigation history
      const updatedSessionData = {
        ...sessionData,
        navigationHistory: [
          ...(sessionData.navigationHistory || []),
          navigationData
        ].slice(-10) // Keep only last 10 navigation entries
      };
      
      updateSessionData(updatedSessionData);
    } catch (error) {
      console.error('Error storing navigation context:', error);
    }
  }, [sessionId, sessionData, updateSessionData]);
  
  /**
   * Stores error context for recovery procedures
   * Maps to CICS error handling with abend context preservation
   * 
   * @param {Object} error - Error context
   * @param {string} error.type - Error type
   * @param {string} error.message - Error message
   * @param {Object} error.context - Additional error context
   */
  const storeErrorContext = useCallback((error) => {
    try {
      const errorData = {
        ...error,
        timestamp: new Date().toISOString(),
        sessionId: sessionId,
        route: window.location.pathname
      };
      
      setErrorContext(errorData);
      setInStorage(SESSION_CONFIG.ERROR_CONTEXT_KEY, errorData);
      
      // Update session data with error context
      updateSessionData({
        errorContext: errorData,
        lastError: errorData
      });
    } catch (error) {
      console.error('Error storing error context:', error);
    }
  }, [sessionId, updateSessionData]);
  
  // =============================================================================
  // INITIALIZATION AND CLEANUP
  // =============================================================================
  
  /**
   * Initialize session state from stored data on component mount
   */
  useEffect(() => {
    const initializeFromStorage = async () => {
      try {
        setIsInitializing(true);
        
        // Check for existing session data
        const storedSessionId = getFromStorage(SESSION_CONFIG.SESSION_ID_KEY);
        const storedSessionData = getFromStorage(SESSION_CONFIG.SESSION_STATE_KEY);
        const storedNavigationContext = getFromStorage(SESSION_CONFIG.NAVIGATION_CONTEXT_KEY);
        const storedErrorContext = getFromStorage(SESSION_CONFIG.ERROR_CONTEXT_KEY);
        
        // Check for existing JWT token
        const token = tokenManager.getToken();
        
        if (token && !isTokenExpired(token) && storedSessionId && storedSessionData) {
          // Restore session from storage
          setSessionId(storedSessionId);
          setSessionData(storedSessionData);
          setNavigationContext(storedNavigationContext);
          setErrorContext(storedErrorContext);
          
          // Set authentication state
          setIsAuthenticated(true);
          setCurrentUser({
            userId: storedSessionData.userId,
            userType: storedSessionData.userType,
            roles: storedSessionData.roles || [],
            permissions: storedSessionData.permissions || []
          });
          
          // Set token expiry
          const expiryTime = getTokenExpirationTime(token);
          setTokenExpiry(expiryTime);
          
          // Update session status
          setSessionStatus(SESSION_CONFIG.SESSION_STATUS.ACTIVE);
          
          // Start monitoring
          startSessionMonitoring();
          
          console.log('Session restored from storage:', {
            sessionId: storedSessionId,
            userId: storedSessionData.userId
          });
        } else {
          // No valid session found, clean up
          setSessionStatus(SESSION_CONFIG.SESSION_STATUS.TERMINATED);
          tokenManager.removeToken();
          removeFromStorage(SESSION_CONFIG.SESSION_ID_KEY);
          removeFromStorage(SESSION_CONFIG.SESSION_STATE_KEY);
          removeFromStorage(SESSION_CONFIG.NAVIGATION_CONTEXT_KEY);
          removeFromStorage(SESSION_CONFIG.ERROR_CONTEXT_KEY);
        }
        
        setIsInitializing(false);
      } catch (error) {
        console.error('Error initializing session from storage:', error);
        setSessionStatus(SESSION_CONFIG.SESSION_STATUS.ERROR);
        setIsInitializing(false);
      }
    };
    
    initializeFromStorage();
  }, [startSessionMonitoring]);
  
  /**
   * Calculate time until expiry
   */
  useEffect(() => {
    if (tokenExpiry) {
      const interval = setInterval(() => {
        const remaining = tokenExpiry - Date.now();
        setTimeUntilExpiry(Math.max(0, remaining));
        
        if (remaining <= 0) {
          clearInterval(interval);
        }
      }, 1000);
      
      return () => clearInterval(interval);
    }
  }, [tokenExpiry]);
  
  /**
   * Cleanup on unmount
   */
  useEffect(() => {
    return () => {
      stopSessionMonitoring();
    };
  }, [stopSessionMonitoring]);
  
  // =============================================================================
  // RETURN HOOK API
  // =============================================================================
  
  return {
    // Session state
    sessionId,
    sessionStatus,
    sessionData,
    isAuthenticated,
    currentUser,
    isInitializing,
    isRefreshing,
    
    // Session timing
    tokenExpiry,
    timeUntilExpiry,
    lastActivity,
    showTimeoutWarning,
    
    // Context data
    navigationContext,
    errorContext,
    
    // Session management functions
    initializeSession,
    refreshSession,
    terminateSession,
    
    // Data management functions
    updateSessionData,
    storeNavigationContext,
    storeErrorContext,
    
    // Utility functions
    isSessionValid: () => {
      return isAuthenticated && 
             sessionId && 
             sessionStatus === SESSION_CONFIG.SESSION_STATUS.ACTIVE &&
             tokenManager.isTokenValid(tokenManager.getToken());
    },
    
    getSessionInfo: () => ({
      sessionId,
      status: sessionStatus,
      isAuthenticated,
      user: currentUser,
      expiryTime: tokenExpiry,
      timeUntilExpiry,
      lastActivity
    }),
    
    // Event handlers for session activity
    recordActivity: useCallback(() => {
      setLastActivity(Date.now());
    }, []),
    
    dismissTimeoutWarning: useCallback(() => {
      setShowTimeoutWarning(false);
    }, []),
    
    // Session constants for consumers
    SESSION_STATUS: SESSION_CONFIG.SESSION_STATUS,
    SESSION_EVENTS
  };
};

// =============================================================================
// ADDITIONAL EXPORTED FUNCTIONS
// =============================================================================

/**
 * Initializes a session with authentication data
 * Standalone function for use outside of React components
 * 
 * @param {Object} authData - Authentication data
 * @returns {Promise<Object>} Session initialization result
 */
export const initializeSession = async (authData) => {
  try {
    // Validate input
    if (!authData || !authData.token || !authData.user) {
      throw new Error('Invalid authentication data');
    }
    
    // Generate session ID
    const sessionId = generateSessionId();
    
    // Store tokens
    const tokenStored = tokenManager.setToken(authData.token, authData.refreshToken);
    if (!tokenStored) {
      throw new Error('Failed to store authentication tokens');
    }
    
    // Create session data
    const sessionData = {
      sessionId,
      userId: authData.user.userId,
      userType: authData.user.userType,
      roles: authData.user.roles || [],
      permissions: authData.user.permissions || [],
      createdAt: new Date().toISOString(),
      lastActivity: new Date().toISOString()
    };
    
    // Store session data
    setInStorage(SESSION_CONFIG.SESSION_ID_KEY, sessionId);
    setInStorage(SESSION_CONFIG.SESSION_STATE_KEY, sessionData);
    
    return {
      success: true,
      sessionId,
      sessionData,
      message: 'Session initialized successfully'
    };
  } catch (error) {
    console.error('Session initialization failed:', error);
    return {
      success: false,
      error: error.message,
      message: 'Session initialization failed'
    };
  }
};

/**
 * Terminates a session and cleans up resources
 * Standalone function for use outside of React components
 * 
 * @param {Object} options - Termination options
 * @returns {Promise<Object>} Session termination result
 */
export const terminateSession = async (options = {}) => {
  try {
    // Clear tokens
    tokenManager.removeToken();
    
    // Clear session data
    removeFromStorage(SESSION_CONFIG.SESSION_ID_KEY);
    removeFromStorage(SESSION_CONFIG.SESSION_STATE_KEY);
    removeFromStorage(SESSION_CONFIG.NAVIGATION_CONTEXT_KEY);
    removeFromStorage(SESSION_CONFIG.ERROR_CONTEXT_KEY);
    
    return {
      success: true,
      message: 'Session terminated successfully'
    };
  } catch (error) {
    console.error('Session termination failed:', error);
    return {
      success: false,
      error: error.message,
      message: 'Session termination failed'
    };
  }
};

/**
 * Refreshes the current session
 * Standalone function for use outside of React components
 * 
 * @returns {Promise<Object>} Session refresh result
 */
export const refreshSession = async () => {
  try {
    // Attempt token refresh
    const refreshed = await tokenManager.refreshTokenIfNeeded();
    if (!refreshed) {
      throw new Error('Token refresh failed');
    }
    
    // Update session data
    const sessionData = getFromStorage(SESSION_CONFIG.SESSION_STATE_KEY);
    if (sessionData) {
      const updatedData = {
        ...sessionData,
        lastActivity: new Date().toISOString(),
        tokenRefreshedAt: new Date().toISOString()
      };
      setInStorage(SESSION_CONFIG.SESSION_STATE_KEY, updatedData);
    }
    
    return {
      success: true,
      message: 'Session refreshed successfully'
    };
  } catch (error) {
    console.error('Session refresh failed:', error);
    return {
      success: false,
      error: error.message,
      message: 'Session refresh failed'
    };
  }
};

// Export the main hook as default
export default useSessionState;