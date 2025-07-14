/**
 * useSessionState.js
 * 
 * Custom React hook implementing pseudo-conversational state management through JWT tokens
 * and Redis session storage, providing session initialization, maintenance, timeout handling,
 * and termination that preserves CICS terminal storage behavior while supporting stateless
 * microservices architecture.
 * 
 * This hook implements comprehensive session management for the CardDemo application,
 * providing JWT token-based authentication context and Redis-backed session storage
 * that maintains complex session state, navigation history, and temporary data storage
 * across stateless REST API calls, replacing CICS pseudo-conversational processing.
 * 
 * Key Features:
 * - Session lifecycle management with JWT token validation and Redis session retrieval
 * - Automatic session timeout handling with configurable TTL expiration
 * - Error context preservation through Redis session storage for recovery procedures
 * - Navigation context preservation across React Router transitions
 * - Stateless architecture support with distributed session management
 * - Pseudo-conversational flow equivalent to CICS temporary storage behavior
 * 
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0
 */

import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { tokenManager } from '../utils/apiClient';
import { isTokenExpired } from '../utils/authUtils';

// ============================================================================
// CONSTANTS AND CONFIGURATION
// ============================================================================

/**
 * Session management configuration
 * Aligned with CICS terminal storage behavior and Redis TTL settings
 */
const SESSION_CONFIG = {
  // Redis session storage configuration
  REDIS_SESSION_PREFIX: 'carddemo:session:',
  REDIS_ERROR_CONTEXT_PREFIX: 'carddemo:error:',
  REDIS_NAVIGATION_PREFIX: 'carddemo:navigation:',
  
  // Session timeout configuration (30 minutes matching CICS terminal timeout)
  DEFAULT_SESSION_TIMEOUT_MS: 30 * 60 * 1000,
  
  // Warning threshold (5 minutes before expiration)
  SESSION_WARNING_THRESHOLD_MS: 5 * 60 * 1000,
  
  // Refresh interval for session validation (30 seconds)
  SESSION_CHECK_INTERVAL_MS: 30 * 1000,
  
  // Redis TTL configuration (aligned with JWT token expiration)
  REDIS_TTL_SECONDS: 1800, // 30 minutes
  ERROR_CONTEXT_TTL_SECONDS: 3600, // 1 hour for error recovery
  
  // Session storage keys
  SESSION_DATA_KEY: 'carddemo_session_data',
  ERROR_CONTEXT_KEY: 'carddemo_error_context',
  NAVIGATION_HISTORY_KEY: 'carddemo_navigation_history',
  
  // API endpoints for session management
  SESSION_ENDPOINTS: {
    CREATE: '/api/session/create',
    REFRESH: '/api/session/refresh',
    VALIDATE: '/api/session/validate',
    TERMINATE: '/api/session/terminate',
    STORE_STATE: '/api/session/store',
    RETRIEVE_STATE: '/api/session/retrieve'
  }
};

/**
 * Session state enumeration
 * Represents different session lifecycle states
 */
const SESSION_STATES = {
  UNINITIALIZED: 'UNINITIALIZED',
  INITIALIZING: 'INITIALIZING',
  ACTIVE: 'ACTIVE',
  REFRESHING: 'REFRESHING',
  WARNING: 'WARNING',
  EXPIRED: 'EXPIRED',
  TERMINATED: 'TERMINATED',
  ERROR: 'ERROR'
};

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

/**
 * Generates unique session correlation ID for distributed tracing
 * 
 * @returns {string} Unique correlation ID
 */
const generateCorrelationId = () => {
  return `SESS-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
};

/**
 * Calculates session expiry time based on JWT token and configuration
 * 
 * @param {string} token - JWT token
 * @returns {Object} Session expiry information
 */
const calculateSessionExpiry = (token) => {
  try {
    const tokenExpiry = tokenManager.getTokenExpiry();
    const currentTime = Date.now();
    
    if (!tokenExpiry) {
      return {
        expiresAt: new Date(currentTime + SESSION_CONFIG.DEFAULT_SESSION_TIMEOUT_MS),
        timeUntilExpiry: SESSION_CONFIG.DEFAULT_SESSION_TIMEOUT_MS,
        isExpired: false,
        isWarning: false
      };
    }
    
    const expiryTime = tokenExpiry.getTime();
    const timeUntilExpiry = expiryTime - currentTime;
    const isExpired = timeUntilExpiry <= 0;
    const isWarning = timeUntilExpiry <= SESSION_CONFIG.SESSION_WARNING_THRESHOLD_MS && !isExpired;
    
    return {
      expiresAt: tokenExpiry,
      timeUntilExpiry: Math.max(0, timeUntilExpiry),
      isExpired,
      isWarning
    };
  } catch (error) {
    console.error('Error calculating session expiry:', error);
    return {
      expiresAt: new Date(Date.now() + SESSION_CONFIG.DEFAULT_SESSION_TIMEOUT_MS),
      timeUntilExpiry: SESSION_CONFIG.DEFAULT_SESSION_TIMEOUT_MS,
      isExpired: false,
      isWarning: false
    };
  }
};

/**
 * Stores session data in Redis through API call
 * 
 * @param {string} sessionId - Session identifier
 * @param {Object} sessionData - Session data to store
 * @param {number} ttl - Time to live in seconds
 * @returns {Promise<boolean>} Storage success status
 */
const storeSessionData = async (sessionId, sessionData, ttl = SESSION_CONFIG.REDIS_TTL_SECONDS) => {
  try {
    const response = await fetch(SESSION_CONFIG.SESSION_ENDPOINTS.STORE_STATE, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${tokenManager.getToken()}`
      },
      body: JSON.stringify({
        sessionId,
        data: sessionData,
        ttl,
        correlationId: generateCorrelationId()
      })
    });
    
    return response.ok;
  } catch (error) {
    console.error('Error storing session data:', error);
    return false;
  }
};

/**
 * Retrieves session data from Redis through API call
 * 
 * @param {string} sessionId - Session identifier
 * @returns {Promise<Object|null>} Retrieved session data or null
 */
const retrieveSessionData = async (sessionId) => {
  try {
    const response = await fetch(SESSION_CONFIG.SESSION_ENDPOINTS.RETRIEVE_STATE, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${tokenManager.getToken()}`
      },
      body: JSON.stringify({
        sessionId,
        correlationId: generateCorrelationId()
      })
    });
    
    if (!response.ok) {
      return null;
    }
    
    const result = await response.json();
    return result.data || null;
  } catch (error) {
    console.error('Error retrieving session data:', error);
    return null;
  }
};

// ============================================================================
// SESSION LIFECYCLE FUNCTIONS
// ============================================================================

/**
 * Initializes session with JWT token and Redis session creation
 * Implements session initialization per Section 7.6.3 requirements
 * 
 * @param {string} token - JWT access token
 * @param {Object} initialState - Initial session state
 * @returns {Promise<Object>} Session initialization result
 */
export const initializeSession = async (token, initialState = {}) => {
  try {
    if (!token) {
      throw new Error('Token is required for session initialization');
    }
    
    // Validate token before initializing session
    const tokenValidation = isTokenExpired(token);
    if (tokenValidation.isExpired) {
      throw new Error('Cannot initialize session with expired token');
    }
    
    // Generate unique session ID
    const sessionId = generateCorrelationId();
    
    // Calculate session expiry
    const sessionExpiry = calculateSessionExpiry(token);
    
    // Create initial session data
    const sessionData = {
      sessionId,
      userId: tokenManager.getToken() ? JSON.parse(atob(tokenManager.getToken().split('.')[1])).sub : null,
      correlationId: sessionId,
      createdAt: new Date().toISOString(),
      lastActivity: new Date().toISOString(),
      expiresAt: sessionExpiry.expiresAt.toISOString(),
      
      // Session state
      state: initialState,
      
      // Navigation context
      navigationHistory: [],
      currentLocation: window.location.pathname,
      
      // Error context
      errorContext: null,
      
      // Pseudo-conversational state
      conversationalState: {
        isActive: true,
        lastInteraction: new Date().toISOString(),
        transactionCount: 0
      }
    };
    
    // Store session data in Redis
    const stored = await storeSessionData(sessionId, sessionData);
    if (!stored) {
      console.warn('Failed to store session data in Redis, using local storage fallback');
    }
    
    // Store session metadata locally
    localStorage.setItem(SESSION_CONFIG.SESSION_DATA_KEY, JSON.stringify({
      sessionId,
      userId: sessionData.userId,
      createdAt: sessionData.createdAt,
      expiresAt: sessionData.expiresAt
    }));
    
    // Create session via API
    const response = await fetch(SESSION_CONFIG.SESSION_ENDPOINTS.CREATE, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        sessionId,
        initialState,
        correlationId: sessionId
      })
    });
    
    if (!response.ok) {
      throw new Error(`Session creation failed: ${response.status}`);
    }
    
    return {
      success: true,
      sessionId,
      sessionData,
      expiresAt: sessionExpiry.expiresAt,
      timeUntilExpiry: sessionExpiry.timeUntilExpiry
    };
    
  } catch (error) {
    console.error('Session initialization failed:', error);
    return {
      success: false,
      error: error.message,
      errorCode: 'SESSION_INIT_FAILED'
    };
  }
};

/**
 * Terminates session with secure logout and Redis session cleanup
 * Implements session termination per Section 7.6.3 requirements
 * 
 * @param {string} sessionId - Session identifier
 * @param {string} reason - Termination reason
 * @returns {Promise<Object>} Session termination result
 */
export const terminateSession = async (sessionId, reason = 'USER_LOGOUT') => {
  try {
    if (!sessionId) {
      throw new Error('Session ID is required for termination');
    }
    
    // Retrieve current session data for cleanup
    const sessionData = await retrieveSessionData(sessionId);
    
    // Call session termination API
    const response = await fetch(SESSION_CONFIG.SESSION_ENDPOINTS.TERMINATE, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${tokenManager.getToken()}`
      },
      body: JSON.stringify({
        sessionId,
        reason,
        correlationId: generateCorrelationId()
      })
    });
    
    // Clean up local storage
    localStorage.removeItem(SESSION_CONFIG.SESSION_DATA_KEY);
    localStorage.removeItem(SESSION_CONFIG.ERROR_CONTEXT_KEY);
    localStorage.removeItem(SESSION_CONFIG.NAVIGATION_HISTORY_KEY);
    
    // Clean up session storage
    sessionStorage.clear();
    
    // Clear JWT tokens
    tokenManager.removeToken();
    
    return {
      success: true,
      sessionId,
      reason,
      terminatedAt: new Date().toISOString(),
      sessionData: sessionData || null
    };
    
  } catch (error) {
    console.error('Session termination failed:', error);
    
    // Force cleanup even if API call fails
    localStorage.removeItem(SESSION_CONFIG.SESSION_DATA_KEY);
    localStorage.removeItem(SESSION_CONFIG.ERROR_CONTEXT_KEY);
    localStorage.removeItem(SESSION_CONFIG.NAVIGATION_HISTORY_KEY);
    sessionStorage.clear();
    tokenManager.removeToken();
    
    return {
      success: false,
      error: error.message,
      errorCode: 'SESSION_TERMINATION_FAILED',
      forcedCleanup: true
    };
  }
};

/**
 * Refreshes session with JWT token validation and Redis session retrieval
 * Implements session refresh per Section 7.6.3 requirements
 * 
 * @param {string} sessionId - Session identifier
 * @returns {Promise<Object>} Session refresh result
 */
export const refreshSession = async (sessionId) => {
  try {
    if (!sessionId) {
      throw new Error('Session ID is required for refresh');
    }
    
    // Attempt to refresh JWT token
    const refreshedToken = await tokenManager.refreshTokenIfNeeded();
    if (!refreshedToken) {
      throw new Error('Token refresh failed');
    }
    
    // Retrieve current session data
    const sessionData = await retrieveSessionData(sessionId);
    if (!sessionData) {
      throw new Error('Session data not found in Redis');
    }
    
    // Update session activity
    const updatedSessionData = {
      ...sessionData,
      lastActivity: new Date().toISOString(),
      conversationalState: {
        ...sessionData.conversationalState,
        lastInteraction: new Date().toISOString(),
        transactionCount: (sessionData.conversationalState.transactionCount || 0) + 1
      }
    };
    
    // Store updated session data
    const stored = await storeSessionData(sessionId, updatedSessionData);
    if (!stored) {
      console.warn('Failed to store updated session data');
    }
    
    // Call session refresh API
    const response = await fetch(SESSION_CONFIG.SESSION_ENDPOINTS.REFRESH, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${refreshedToken}`
      },
      body: JSON.stringify({
        sessionId,
        correlationId: generateCorrelationId()
      })
    });
    
    if (!response.ok) {
      throw new Error(`Session refresh failed: ${response.status}`);
    }
    
    const result = await response.json();
    
    // Calculate new expiry
    const sessionExpiry = calculateSessionExpiry(refreshedToken);
    
    return {
      success: true,
      sessionId,
      sessionData: updatedSessionData,
      token: refreshedToken,
      expiresAt: sessionExpiry.expiresAt,
      timeUntilExpiry: sessionExpiry.timeUntilExpiry,
      refreshedAt: new Date().toISOString()
    };
    
  } catch (error) {
    console.error('Session refresh failed:', error);
    return {
      success: false,
      error: error.message,
      errorCode: 'SESSION_REFRESH_FAILED'
    };
  }
};

// ============================================================================
// MAIN HOOK IMPLEMENTATION
// ============================================================================

/**
 * Custom React hook for session state management
 * Implements pseudo-conversational state management through JWT tokens and Redis session storage
 * 
 * @param {Object} options - Hook configuration options
 * @returns {Object} Session state and management functions
 */
const useSessionState = (options = {}) => {
  // Configuration
  const {
    autoRefresh = true,
    warningThreshold = SESSION_CONFIG.SESSION_WARNING_THRESHOLD_MS,
    onSessionExpired = null,
    onSessionWarning = null,
    onSessionRefresh = null
  } = options;
  
  // React Router navigation
  const navigate = useNavigate();
  
  // State management
  const [sessionState, setSessionState] = useState(SESSION_STATES.UNINITIALIZED);
  const [sessionData, setSessionData] = useState(null);
  const [sessionError, setSessionError] = useState(null);
  const [timeUntilExpiry, setTimeUntilExpiry] = useState(0);
  const [isWarning, setIsWarning] = useState(false);
  
  // Refs for cleanup
  const intervalRef = useRef(null);
  const warningTimeoutRef = useRef(null);
  const expiryTimeoutRef = useRef(null);
  
  // ============================================================================
  // SESSION VALIDATION AND MONITORING
  // ============================================================================
  
  /**
   * Validates current session state
   * Checks JWT token validity and session data consistency
   */
  const validateSession = useCallback(async () => {
    try {
      const token = tokenManager.getToken();
      if (!token) {
        setSessionState(SESSION_STATES.UNINITIALIZED);
        setSessionData(null);
        return false;
      }
      
      // Check token expiration
      const tokenStatus = isTokenExpired(token);
      if (tokenStatus.isExpired) {
        setSessionState(SESSION_STATES.EXPIRED);
        setSessionError('Session expired');
        
        // Trigger expiration callback
        if (onSessionExpired) {
          onSessionExpired(sessionData);
        }
        
        return false;
      }
      
      // Get session metadata from local storage
      const sessionMetadata = localStorage.getItem(SESSION_CONFIG.SESSION_DATA_KEY);
      if (!sessionMetadata) {
        setSessionState(SESSION_STATES.UNINITIALIZED);
        return false;
      }
      
      const metadata = JSON.parse(sessionMetadata);
      
      // Validate session API
      const response = await fetch(SESSION_CONFIG.SESSION_ENDPOINTS.VALIDATE, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          sessionId: metadata.sessionId,
          correlationId: generateCorrelationId()
        })
      });
      
      if (!response.ok) {
        throw new Error(`Session validation failed: ${response.status}`);
      }
      
      // Calculate session expiry
      const sessionExpiry = calculateSessionExpiry(token);
      setTimeUntilExpiry(sessionExpiry.timeUntilExpiry);
      
      // Check for warning state
      if (sessionExpiry.isWarning && !isWarning) {
        setIsWarning(true);
        setSessionState(SESSION_STATES.WARNING);
        
        // Trigger warning callback
        if (onSessionWarning) {
          onSessionWarning(sessionExpiry.timeUntilExpiry);
        }
      } else if (!sessionExpiry.isWarning && isWarning) {
        setIsWarning(false);
        setSessionState(SESSION_STATES.ACTIVE);
      }
      
      return true;
      
    } catch (error) {
      console.error('Session validation failed:', error);
      setSessionState(SESSION_STATES.ERROR);
      setSessionError(error.message);
      return false;
    }
  }, [sessionData, isWarning, onSessionExpired, onSessionWarning]);
  
  /**
   * Starts session monitoring
   * Sets up automatic session validation and refresh
   */
  const startSessionMonitoring = useCallback(() => {
    // Clear existing intervals
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
    }
    
    // Set up session validation interval
    intervalRef.current = setInterval(async () => {
      const isValid = await validateSession();
      
      if (!isValid) {
        stopSessionMonitoring();
        return;
      }
      
      // Auto-refresh if enabled and approaching expiry
      if (autoRefresh && isWarning) {
        try {
          const sessionMetadata = localStorage.getItem(SESSION_CONFIG.SESSION_DATA_KEY);
          if (sessionMetadata) {
            const metadata = JSON.parse(sessionMetadata);
            const refreshResult = await refreshSession(metadata.sessionId);
            
            if (refreshResult.success) {
              setSessionState(SESSION_STATES.ACTIVE);
              setIsWarning(false);
              setTimeUntilExpiry(refreshResult.timeUntilExpiry);
              
              // Trigger refresh callback
              if (onSessionRefresh) {
                onSessionRefresh(refreshResult);
              }
            }
          }
        } catch (error) {
          console.error('Auto-refresh failed:', error);
        }
      }
    }, SESSION_CONFIG.SESSION_CHECK_INTERVAL_MS);
  }, [validateSession, autoRefresh, isWarning, onSessionRefresh]);
  
  /**
   * Stops session monitoring
   * Clears all intervals and timeouts
   */
  const stopSessionMonitoring = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
    
    if (warningTimeoutRef.current) {
      clearTimeout(warningTimeoutRef.current);
      warningTimeoutRef.current = null;
    }
    
    if (expiryTimeoutRef.current) {
      clearTimeout(expiryTimeoutRef.current);
      expiryTimeoutRef.current = null;
    }
  }, []);
  
  // ============================================================================
  // SESSION MANAGEMENT FUNCTIONS
  // ============================================================================
  
  /**
   * Initializes session with authentication
   * Creates new session with JWT token and Redis storage
   */
  const initializeSessionState = useCallback(async (token, initialState = {}) => {
    try {
      setSessionState(SESSION_STATES.INITIALIZING);
      setSessionError(null);
      
      const result = await initializeSession(token, initialState);
      
      if (result.success) {
        setSessionState(SESSION_STATES.ACTIVE);
        setSessionData(result.sessionData);
        setTimeUntilExpiry(result.timeUntilExpiry);
        
        // Start monitoring
        startSessionMonitoring();
        
        return result;
      } else {
        setSessionState(SESSION_STATES.ERROR);
        setSessionError(result.error);
        return result;
      }
    } catch (error) {
      console.error('Session initialization failed:', error);
      setSessionState(SESSION_STATES.ERROR);
      setSessionError(error.message);
      return {
        success: false,
        error: error.message,
        errorCode: 'INITIALIZATION_ERROR'
      };
    }
  }, [startSessionMonitoring]);
  
  /**
   * Terminates current session
   * Cleans up session data and redirects to login
   */
  const terminateSessionState = useCallback(async (reason = 'USER_LOGOUT') => {
    try {
      stopSessionMonitoring();
      
      const sessionMetadata = localStorage.getItem(SESSION_CONFIG.SESSION_DATA_KEY);
      let sessionId = null;
      
      if (sessionMetadata) {
        const metadata = JSON.parse(sessionMetadata);
        sessionId = metadata.sessionId;
      }
      
      const result = await terminateSession(sessionId, reason);
      
      setSessionState(SESSION_STATES.TERMINATED);
      setSessionData(null);
      setSessionError(null);
      setTimeUntilExpiry(0);
      setIsWarning(false);
      
      // Redirect to login
      navigate('/login', { 
        replace: true,
        state: { reason, message: 'Session terminated' }
      });
      
      return result;
    } catch (error) {
      console.error('Session termination failed:', error);
      
      // Force cleanup and redirect
      setSessionState(SESSION_STATES.TERMINATED);
      setSessionData(null);
      setSessionError(error.message);
      
      navigate('/login', { 
        replace: true,
        state: { reason: 'ERROR', message: error.message }
      });
      
      return {
        success: false,
        error: error.message,
        errorCode: 'TERMINATION_ERROR'
      };
    }
  }, [stopSessionMonitoring, navigate]);
  
  /**
   * Refreshes current session
   * Updates session data and extends expiry
   */
  const refreshSessionState = useCallback(async () => {
    try {
      setSessionState(SESSION_STATES.REFRESHING);
      
      const sessionMetadata = localStorage.getItem(SESSION_CONFIG.SESSION_DATA_KEY);
      if (!sessionMetadata) {
        throw new Error('No session metadata found');
      }
      
      const metadata = JSON.parse(sessionMetadata);
      const result = await refreshSession(metadata.sessionId);
      
      if (result.success) {
        setSessionState(SESSION_STATES.ACTIVE);
        setSessionData(result.sessionData);
        setTimeUntilExpiry(result.timeUntilExpiry);
        setIsWarning(false);
        
        // Trigger refresh callback
        if (onSessionRefresh) {
          onSessionRefresh(result);
        }
        
        return result;
      } else {
        setSessionState(SESSION_STATES.ERROR);
        setSessionError(result.error);
        return result;
      }
    } catch (error) {
      console.error('Session refresh failed:', error);
      setSessionState(SESSION_STATES.ERROR);
      setSessionError(error.message);
      return {
        success: false,
        error: error.message,
        errorCode: 'REFRESH_ERROR'
      };
    }
  }, [onSessionRefresh]);
  
  /**
   * Stores temporary data in session
   * Equivalent to CICS temporary storage
   */
  const storeTemporaryData = useCallback(async (key, data) => {
    try {
      const sessionMetadata = localStorage.getItem(SESSION_CONFIG.SESSION_DATA_KEY);
      if (!sessionMetadata) {
        throw new Error('No active session');
      }
      
      const metadata = JSON.parse(sessionMetadata);
      
      // Store in Redis with error context TTL
      const storageKey = `${SESSION_CONFIG.REDIS_ERROR_CONTEXT_PREFIX}${metadata.sessionId}:${key}`;
      const stored = await storeSessionData(storageKey, data, SESSION_CONFIG.ERROR_CONTEXT_TTL_SECONDS);
      
      if (!stored) {
        // Fallback to local storage
        const errorContextKey = `${SESSION_CONFIG.ERROR_CONTEXT_KEY}:${key}`;
        localStorage.setItem(errorContextKey, JSON.stringify(data));
      }
      
      return { success: true, key, data };
    } catch (error) {
      console.error('Failed to store temporary data:', error);
      return { success: false, error: error.message };
    }
  }, []);
  
  /**
   * Retrieves temporary data from session
   * Equivalent to CICS temporary storage retrieval
   */
  const retrieveTemporaryData = useCallback(async (key) => {
    try {
      const sessionMetadata = localStorage.getItem(SESSION_CONFIG.SESSION_DATA_KEY);
      if (!sessionMetadata) {
        throw new Error('No active session');
      }
      
      const metadata = JSON.parse(sessionMetadata);
      
      // Try to retrieve from Redis first
      const storageKey = `${SESSION_CONFIG.REDIS_ERROR_CONTEXT_PREFIX}${metadata.sessionId}:${key}`;
      const data = await retrieveSessionData(storageKey);
      
      if (data) {
        return { success: true, key, data };
      }
      
      // Fallback to local storage
      const errorContextKey = `${SESSION_CONFIG.ERROR_CONTEXT_KEY}:${key}`;
      const localData = localStorage.getItem(errorContextKey);
      
      if (localData) {
        return { success: true, key, data: JSON.parse(localData) };
      }
      
      return { success: false, error: 'Data not found' };
    } catch (error) {
      console.error('Failed to retrieve temporary data:', error);
      return { success: false, error: error.message };
    }
  }, []);
  
  // ============================================================================
  // LIFECYCLE EFFECTS
  // ============================================================================
  
  /**
   * Initialize session on component mount
   * Check for existing session and validate
   */
  useEffect(() => {
    const initializeExistingSession = async () => {
      try {
        const token = tokenManager.getToken();
        if (!token) {
          setSessionState(SESSION_STATES.UNINITIALIZED);
          return;
        }
        
        const tokenStatus = isTokenExpired(token);
        if (tokenStatus.isExpired) {
          setSessionState(SESSION_STATES.EXPIRED);
          terminateSessionState('TOKEN_EXPIRED');
          return;
        }
        
        const sessionMetadata = localStorage.getItem(SESSION_CONFIG.SESSION_DATA_KEY);
        if (!sessionMetadata) {
          setSessionState(SESSION_STATES.UNINITIALIZED);
          return;
        }
        
        const metadata = JSON.parse(sessionMetadata);
        const sessionData = await retrieveSessionData(metadata.sessionId);
        
        if (sessionData) {
          setSessionState(SESSION_STATES.ACTIVE);
          setSessionData(sessionData);
          
          const sessionExpiry = calculateSessionExpiry(token);
          setTimeUntilExpiry(sessionExpiry.timeUntilExpiry);
          setIsWarning(sessionExpiry.isWarning);
          
          startSessionMonitoring();
        } else {
          setSessionState(SESSION_STATES.UNINITIALIZED);
        }
      } catch (error) {
        console.error('Failed to initialize existing session:', error);
        setSessionState(SESSION_STATES.ERROR);
        setSessionError(error.message);
      }
    };
    
    initializeExistingSession();
  }, [startSessionMonitoring, terminateSessionState]);
  
  /**
   * Cleanup on component unmount
   */
  useEffect(() => {
    return () => {
      stopSessionMonitoring();
    };
  }, [stopSessionMonitoring]);
  
  // ============================================================================
  // RETURN HOOK INTERFACE
  // ============================================================================
  
  return {
    // Session state
    sessionState,
    sessionData,
    isActive: sessionState === SESSION_STATES.ACTIVE,
    isExpired: sessionState === SESSION_STATES.EXPIRED,
    isWarning,
    timeUntilExpiry,
    sessionError,
    
    // Session management functions
    initializeSession: initializeSessionState,
    terminateSession: terminateSessionState,
    refreshSession: refreshSessionState,
    
    // Temporary data management (CICS temporary storage equivalent)
    storeTemporaryData,
    retrieveTemporaryData,
    
    // Session validation
    validateSession,
    
    // Monitoring control
    startMonitoring: startSessionMonitoring,
    stopMonitoring: stopSessionMonitoring,
    
    // Session metadata
    sessionId: sessionData?.sessionId || null,
    userId: sessionData?.userId || null,
    correlationId: sessionData?.correlationId || null,
    
    // Formatted time display
    formattedTimeRemaining: timeUntilExpiry > 0 ? 
      `${Math.floor(timeUntilExpiry / 60000)}:${Math.floor((timeUntilExpiry % 60000) / 1000).toString().padStart(2, '0')}` : 
      '0:00'
  };
};

// ============================================================================
// EXPORTS
// ============================================================================

export default useSessionState;
export { initializeSession, terminateSession, refreshSession, SESSION_STATES };