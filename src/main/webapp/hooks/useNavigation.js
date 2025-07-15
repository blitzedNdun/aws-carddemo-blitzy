/**
 * useNavigation.js
 * 
 * Custom React hook implementing CICS XCTL-equivalent navigation patterns through React Router,
 * providing programmatic navigation control with history management, route guards, and dynamic
 * route parameters that preserve original mainframe transaction flow while enabling modern
 * URL-based navigation.
 * 
 * This hook implements comprehensive navigation management for the CardDemo application,
 * providing React Router useNavigate() hook integration that replaces EXEC CICS XCTL
 * for inter-component navigation with declarative routing, browser history API integration
 * enabling back/forward navigation with state preservation, and protected routes with JWT
 * token validation ensuring authorized access to restricted components.
 * 
 * Key Features:
 * - Programmatic navigation control with React Router useNavigate() hook replacement for EXEC CICS XCTL
 * - Browser history API integration enabling back/forward navigation with state preservation
 * - Protected routes with JWT token validation ensuring authorized access to restricted components
 * - Location state management for maintaining context between screen transitions
 * - Session state validation during navigation operations
 * - F3/PF3 exit functionality with return path logic
 * - Dynamic route parameters replacing COMMAREA data passing
 * - Error navigation with automatic recovery to previous valid state
 * 
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0
 */

import { useEffect, useCallback, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import useSessionState from './useSessionState';
import { isTokenExpired } from '../utils/authUtils';

// ============================================================================
// CONSTANTS AND CONFIGURATION
// ============================================================================

/**
 * Navigation configuration constants
 * Aligned with CICS transaction flow patterns and React Router behavior
 */
const NAVIGATION_CONFIG = {
  // Route definitions matching CICS transaction codes
  ROUTES: {
    LOGIN: '/login',
    MAIN_MENU: '/menu',
    ACCOUNT_VIEW: '/account/view',
    ACCOUNT_UPDATE: '/account/update',
    CARD_LIST: '/card/list',
    CARD_UPDATE: '/card/update',
    TRANSACTION_LIST: '/transaction/list',
    TRANSACTION_ADD: '/transaction/add',
    BILL_PAYMENT: '/bill/payment',
    USER_MANAGEMENT: '/user/management',
    REPORTS: '/reports',
    ADMIN_MENU: '/admin/menu',
    EXIT: '/exit'
  },
  
  // Session storage keys for navigation state
  STORAGE_KEYS: {
    NAVIGATION_HISTORY: 'cardDemo_navigation_history',
    CURRENT_COMPONENT: 'cardDemo_current_component',
    RETURN_PATH: 'cardDemo_return_path',
    NAVIGATION_STATE: 'cardDemo_navigation_state'
  },
  
  // Default navigation options
  DEFAULT_OPTIONS: {
    replace: false,
    preserveState: true,
    validateSession: true,
    trackHistory: true,
    maxHistoryLength: 10
  },
  
  // Protected routes requiring authentication
  PROTECTED_ROUTES: [
    '/menu',
    '/account',
    '/card',
    '/transaction',
    '/bill',
    '/user',
    '/reports',
    '/admin'
  ],
  
  // Admin-only routes
  ADMIN_ROUTES: [
    '/admin',
    '/user/management',
    '/reports'
  ]
};

/**
 * Navigation state enumeration
 * Represents different navigation states and transitions
 */
const NAVIGATION_STATES = {
  IDLE: 'IDLE',
  NAVIGATING: 'NAVIGATING',
  VALIDATING: 'VALIDATING',
  UNAUTHORIZED: 'UNAUTHORIZED',
  ERROR: 'ERROR'
};

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

/**
 * Checks if a route requires authentication
 * 
 * @param {string} path - Route path to check
 * @returns {boolean} True if route requires authentication
 */
const isProtectedRoute = (path) => {
  return NAVIGATION_CONFIG.PROTECTED_ROUTES.some(route => 
    path.startsWith(route)
  );
};

/**
 * Checks if a route requires admin privileges
 * 
 * @param {string} path - Route path to check
 * @returns {boolean} True if route requires admin access
 */
const isAdminRoute = (path) => {
  return NAVIGATION_CONFIG.ADMIN_ROUTES.some(route => 
    path.startsWith(route)
  );
};

/**
 * Generates navigation correlation ID for tracking
 * 
 * @returns {string} Unique correlation ID
 */
const generateNavigationId = () => {
  return `NAV-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
};

/**
 * Extracts component name from route path
 * Simulates CICS transaction code extraction
 * 
 * @param {string} path - Route path
 * @returns {string} Component name
 */
const getComponentNameFromPath = (path) => {
  const segments = path.split('/').filter(segment => segment);
  if (segments.length === 0) return 'HOME';
  
  const componentMap = {
    'login': 'COSGN00C',
    'menu': 'COMEN01C',
    'account': 'COACTVWC',
    'card': 'COCRDLIC',
    'transaction': 'COTRN00C',
    'bill': 'COBIL00C',
    'user': 'COUSR00C',
    'reports': 'CORPT00C',
    'admin': 'COADM01C'
  };
  
  return componentMap[segments[0]] || segments[0].toUpperCase();
};

/**
 * Stores navigation history in session storage
 * Equivalent to CICS temporary storage for navigation context
 * 
 * @param {Object} navigationEntry - Navigation entry to store
 */
const storeNavigationHistory = (navigationEntry) => {
  try {
    const historyKey = NAVIGATION_CONFIG.STORAGE_KEYS.NAVIGATION_HISTORY;
    const stored = sessionStorage.getItem(historyKey);
    const history = stored ? JSON.parse(stored) : [];
    
    // Add new entry
    history.push({
      ...navigationEntry,
      timestamp: new Date().toISOString(),
      navigationId: generateNavigationId()
    });
    
    // Maintain maximum history length
    if (history.length > NAVIGATION_CONFIG.DEFAULT_OPTIONS.maxHistoryLength) {
      history.shift();
    }
    
    sessionStorage.setItem(historyKey, JSON.stringify(history));
  } catch (error) {
    console.error('Failed to store navigation history:', error);
  }
};

/**
 * Retrieves navigation history from session storage
 * 
 * @returns {Array} Navigation history entries
 */
const getNavigationHistory = () => {
  try {
    const historyKey = NAVIGATION_CONFIG.STORAGE_KEYS.NAVIGATION_HISTORY;
    const stored = sessionStorage.getItem(historyKey);
    return stored ? JSON.parse(stored) : [];
  } catch (error) {
    console.error('Failed to retrieve navigation history:', error);
    return [];
  }
};

/**
 * Gets the previous navigation entry for return path logic
 * Implements F3/PF3 return functionality
 * 
 * @returns {Object|null} Previous navigation entry or null
 */
const getPreviousNavigation = () => {
  const history = getNavigationHistory();
  return history.length > 1 ? history[history.length - 2] : null;
};

// ============================================================================
// MAIN HOOK IMPLEMENTATION
// ============================================================================

/**
 * Custom React hook for CICS XCTL-equivalent navigation
 * Implements programmatic navigation control with history management and route guards
 * 
 * @param {Object} options - Navigation hook configuration options
 * @returns {Object} Navigation functions and state
 */
const useNavigation = (options = {}) => {
  // Configuration
  const {
    validateSession = true,
    trackHistory = true,
    autoRedirectOnExpiry = true,
    onNavigationStart = null,
    onNavigationComplete = null,
    onNavigationError = null
  } = { ...NAVIGATION_CONFIG.DEFAULT_OPTIONS, ...options };
  
  // React Router hooks
  const location = useLocation();
  const navigate = useNavigate();
  
  // Session state management
  const {
    isActive: isSessionActive,
    isExpired: isSessionExpired,
    sessionData,
    terminateSession,
    validateSession: validateSessionState
  } = useSessionState();
  
  // Navigation state tracking
  const navigationStateRef = useRef(NAVIGATION_STATES.IDLE);
  const currentComponentRef = useRef(getComponentNameFromPath(location.pathname));
  
  // ============================================================================
  // SESSION VALIDATION FUNCTIONS
  // ============================================================================
  
  /**
   * Validates session state before navigation
   * Implements JWT token validation and session state checking
   * 
   * @returns {Promise<boolean>} True if session is valid for navigation
   */
  const validateNavigationSession = useCallback(async () => {
    if (!validateSession) return true;
    
    try {
      // Check if session state is active
      if (!isSessionActive) {
        return false;
      }
      
      // Validate JWT token expiration
      const token = localStorage.getItem('cardDemo_jwt_token');
      if (!token) {
        return false;
      }
      
      const tokenStatus = isTokenExpired(token);
      if (tokenStatus.isExpired) {
        // Auto-terminate expired session
        if (autoRedirectOnExpiry) {
          await terminateSession('TOKEN_EXPIRED');
        }
        return false;
      }
      
      // Validate session state if available
      if (sessionData && validateSessionState) {
        const sessionValid = await validateSessionState();
        return sessionValid;
      }
      
      return true;
    } catch (error) {
      console.error('Session validation failed during navigation:', error);
      return false;
    }
  }, [validateSession, isSessionActive, sessionData, validateSessionState, terminateSession, autoRedirectOnExpiry]);
  
  /**
   * Checks if user is authorized for a specific route
   * Implements route-based authorization checking
   * 
   * @param {string} path - Route path to check
   * @returns {boolean} True if user is authorized
   */
  const isAuthorizedForRoute = useCallback((path) => {
    // Check if route requires authentication
    if (!isProtectedRoute(path)) {
      return true;
    }
    
    // Check session state
    if (!isSessionActive) {
      return false;
    }
    
    // Check admin routes
    if (isAdminRoute(path)) {
      return sessionData?.user?.isAdmin || false;
    }
    
    return true;
  }, [isSessionActive, sessionData]);
  
  // ============================================================================
  // CORE NAVIGATION FUNCTIONS
  // ============================================================================
  
  /**
   * Navigates to a specific route with state preservation
   * Implements EXEC CICS XCTL equivalent functionality
   * 
   * @param {string} path - Target route path
   * @param {Object} options - Navigation options
   * @returns {Promise<boolean>} True if navigation succeeded
   */
  const navigateToScreen = useCallback(async (path, options = {}) => {
    const {
      state = null,
      replace = false,
      preserveState = true,
      skipValidation = false
    } = options;
    
    try {
      navigationStateRef.current = NAVIGATION_STATES.NAVIGATING;
      
      // Trigger navigation start callback
      if (onNavigationStart) {
        onNavigationStart(path, options);
      }
      
      // Validate session if required
      if (!skipValidation) {
        navigationStateRef.current = NAVIGATION_STATES.VALIDATING;
        const sessionValid = await validateNavigationSession();
        
        if (!sessionValid) {
          navigationStateRef.current = NAVIGATION_STATES.UNAUTHORIZED;
          
          // Redirect to login if session invalid
          navigate(NAVIGATION_CONFIG.ROUTES.LOGIN, {
            replace: true,
            state: { 
              returnUrl: path, 
              reason: 'SESSION_INVALID',
              message: 'Please log in to continue'
            }
          });
          
          return false;
        }
        
        // Check route authorization
        if (!isAuthorizedForRoute(path)) {
          navigationStateRef.current = NAVIGATION_STATES.UNAUTHORIZED;
          
          // Redirect to appropriate menu based on user role
          const redirectPath = sessionData?.user?.isAdmin 
            ? NAVIGATION_CONFIG.ROUTES.ADMIN_MENU 
            : NAVIGATION_CONFIG.ROUTES.MAIN_MENU;
          
          navigate(redirectPath, {
            replace: true,
            state: { 
              error: 'Access denied',
              message: 'You do not have permission to access this resource'
            }
          });
          
          return false;
        }
      }
      
      // Prepare navigation state
      const navigationState = preserveState ? {
        ...state,
        previousPath: location.pathname,
        previousState: location.state,
        navigationId: generateNavigationId(),
        timestamp: new Date().toISOString()
      } : state;
      
      // Store navigation history
      if (trackHistory) {
        storeNavigationHistory({
          fromPath: location.pathname,
          toPath: path,
          fromComponent: currentComponentRef.current,
          toComponent: getComponentNameFromPath(path),
          navigationState,
          userId: sessionData?.user?.userId
        });
      }
      
      // Update current component reference
      currentComponentRef.current = getComponentNameFromPath(path);
      sessionStorage.setItem(
        NAVIGATION_CONFIG.STORAGE_KEYS.CURRENT_COMPONENT,
        currentComponentRef.current
      );
      
      // Perform navigation
      navigate(path, {
        replace,
        state: navigationState
      });
      
      navigationStateRef.current = NAVIGATION_STATES.IDLE;
      
      // Trigger navigation complete callback
      if (onNavigationComplete) {
        onNavigationComplete(path, navigationState);
      }
      
      return true;
      
    } catch (error) {
      console.error('Navigation failed:', error);
      navigationStateRef.current = NAVIGATION_STATES.ERROR;
      
      // Trigger navigation error callback
      if (onNavigationError) {
        onNavigationError(error, path);
      }
      
      return false;
    }
  }, [
    location, 
    navigate, 
    validateNavigationSession, 
    isAuthorizedForRoute, 
    sessionData, 
    trackHistory,
    onNavigationStart,
    onNavigationComplete,
    onNavigationError
  ]);
  
  /**
   * Navigates to main menu with role-based routing
   * Implements CICS main menu navigation pattern
   * 
   * @param {Object} options - Navigation options
   * @returns {Promise<boolean>} True if navigation succeeded
   */
  const navigateToMenu = useCallback(async (options = {}) => {
    try {
      // Determine appropriate menu based on user role
      const menuPath = sessionData?.user?.isAdmin 
        ? NAVIGATION_CONFIG.ROUTES.ADMIN_MENU 
        : NAVIGATION_CONFIG.ROUTES.MAIN_MENU;
      
      return await navigateToScreen(menuPath, {
        ...options,
        replace: true,
        state: {
          ...options.state,
          menuType: sessionData?.user?.isAdmin ? 'ADMIN' : 'USER',
          returnFromMenu: true
        }
      });
    } catch (error) {
      console.error('Menu navigation failed:', error);
      return false;
    }
  }, [sessionData, navigateToScreen]);
  
  /**
   * Handles F3/PF3 exit functionality with return path logic
   * Implements CICS program control return behavior
   * 
   * @param {Object} options - Exit options
   * @returns {Promise<boolean>} True if exit succeeded
   */
  const handleExit = useCallback(async (options = {}) => {
    const {
      confirmExit = false,
      returnToMenu = true,
      preserveState = false
    } = options;
    
    try {
      // Confirm exit if required
      if (confirmExit) {
        const confirmed = window.confirm('Are you sure you want to exit?');
        if (!confirmed) return false;
      }
      
      // Check for return path in navigation history
      const previousNavigation = getPreviousNavigation();
      
      if (previousNavigation && !returnToMenu) {
        // Return to previous screen
        return await navigateToScreen(previousNavigation.fromPath, {
          replace: true,
          state: preserveState ? previousNavigation.navigationState : null
        });
      } else {
        // Return to appropriate menu
        return await navigateToMenu({
          replace: true,
          state: {
            exitedFrom: location.pathname,
            exitedFromComponent: currentComponentRef.current
          }
        });
      }
    } catch (error) {
      console.error('Exit navigation failed:', error);
      return false;
    }
  }, [location, navigateToScreen, navigateToMenu]);
  
  /**
   * Navigates with preserved state context
   * Implements CICS COMMAREA equivalent data passing
   * 
   * @param {string} path - Target route path
   * @param {Object} state - State to preserve
   * @param {Object} options - Navigation options
   * @returns {Promise<boolean>} True if navigation succeeded
   */
  const navigateWithState = useCallback(async (path, state, options = {}) => {
    return await navigateToScreen(path, {
      ...options,
      state: {
        ...state,
        preservedContext: true,
        sourceComponent: currentComponentRef.current,
        sourceTimestamp: new Date().toISOString()
      }
    });
  }, [navigateToScreen]);
  
  /**
   * Navigates back in history (browser back button equivalent)
   * Implements F3/PF3 return functionality
   * 
   * @param {Object} options - Back navigation options
   * @returns {Promise<boolean>} True if back navigation succeeded
   */
  const goBack = useCallback(async (options = {}) => {
    const {
      steps = 1,
      validateTarget = true
    } = options;
    
    try {
      // Validate session before navigating back
      if (validateTarget) {
        const sessionValid = await validateNavigationSession();
        if (!sessionValid) {
          return await navigateToScreen(NAVIGATION_CONFIG.ROUTES.LOGIN, {
            replace: true,
            state: { reason: 'SESSION_INVALID' }
          });
        }
      }
      
      // Use browser history API for back navigation
      navigate(-steps);
      
      return true;
    } catch (error) {
      console.error('Back navigation failed:', error);
      return false;
    }
  }, [navigate, validateNavigationSession, navigateToScreen]);
  
  /**
   * Navigates only if user is authorized for the target route
   * Implements protected route navigation with authorization checking
   * 
   * @param {string} path - Target route path
   * @param {Object} options - Navigation options
   * @returns {Promise<boolean>} True if navigation succeeded
   */
  const navigateIfAuthorized = useCallback(async (path, options = {}) => {
    try {
      // Check authorization first
      if (!isAuthorizedForRoute(path)) {
        // Redirect to appropriate menu
        const redirectPath = sessionData?.user?.isAdmin 
          ? NAVIGATION_CONFIG.ROUTES.ADMIN_MENU 
          : NAVIGATION_CONFIG.ROUTES.MAIN_MENU;
        
        return await navigateToScreen(redirectPath, {
          replace: true,
          state: {
            error: 'Access denied',
            message: 'You do not have permission to access this resource',
            attemptedPath: path
          }
        });
      }
      
      return await navigateToScreen(path, options);
    } catch (error) {
      console.error('Authorized navigation failed:', error);
      return false;
    }
  }, [isAuthorizedForRoute, sessionData, navigateToScreen]);
  
  /**
   * Checks if back navigation is possible
   * Implements navigation state validation
   * 
   * @returns {boolean} True if back navigation is possible
   */
  const canGoBack = useCallback(() => {
    const history = getNavigationHistory();
    return history.length > 1;
  }, []);
  
  // ============================================================================
  // LIFECYCLE EFFECTS
  // ============================================================================
  
  /**
   * Track location changes and update current component
   */
  useEffect(() => {
    const newComponent = getComponentNameFromPath(location.pathname);
    currentComponentRef.current = newComponent;
    
    sessionStorage.setItem(
      NAVIGATION_CONFIG.STORAGE_KEYS.CURRENT_COMPONENT,
      newComponent
    );
  }, [location.pathname]);
  
  /**
   * Handle session expiration during navigation
   */
  useEffect(() => {
    if (isSessionExpired && autoRedirectOnExpiry) {
      const currentPath = location.pathname;
      
      // Don't redirect if already on login page
      if (currentPath !== NAVIGATION_CONFIG.ROUTES.LOGIN) {
        navigate(NAVIGATION_CONFIG.ROUTES.LOGIN, {
          replace: true,
          state: {
            reason: 'SESSION_EXPIRED',
            message: 'Your session has expired. Please log in again.',
            returnUrl: currentPath
          }
        });
      }
    }
  }, [isSessionExpired, autoRedirectOnExpiry, location.pathname, navigate]);
  
  // ============================================================================
  // RETURN HOOK INTERFACE
  // ============================================================================
  
  return {
    // Core navigation functions
    navigateToScreen,
    navigateToMenu,
    handleExit,
    navigateWithState,
    goBack,
    navigateIfAuthorized,
    
    // Navigation state queries
    canGoBack,
    isProtectedRoute,
    isAdminRoute,
    isAuthorizedForRoute,
    
    // Current navigation state
    currentComponent: currentComponentRef.current,
    currentPath: location.pathname,
    currentState: location.state,
    navigationState: navigationStateRef.current,
    
    // Navigation history
    navigationHistory: getNavigationHistory(),
    previousNavigation: getPreviousNavigation(),
    
    // Session context
    isSessionActive,
    isSessionExpired,
    sessionData,
    
    // Utility functions
    generateNavigationId,
    getComponentNameFromPath,
    
    // Configuration
    routes: NAVIGATION_CONFIG.ROUTES,
    navigationStates: NAVIGATION_STATES
  };
};

// ============================================================================
// INDIVIDUAL EXPORT FUNCTIONS
// ============================================================================

/**
 * Standalone navigate to menu function
 * Can be used outside of React components
 * 
 * @param {Object} sessionData - Current session data
 * @param {Function} navigate - React Router navigate function
 * @param {Object} options - Navigation options
 * @returns {Promise<boolean>} True if navigation succeeded
 */
export const navigateToMenu = async (sessionData, navigate, options = {}) => {
  const menuPath = sessionData?.user?.isAdmin 
    ? NAVIGATION_CONFIG.ROUTES.ADMIN_MENU 
    : NAVIGATION_CONFIG.ROUTES.MAIN_MENU;
  
  navigate(menuPath, {
    replace: true,
    state: {
      ...options.state,
      menuType: sessionData?.user?.isAdmin ? 'ADMIN' : 'USER',
      returnFromMenu: true
    }
  });
  
  return true;
};

/**
 * Standalone navigate to screen function
 * Can be used outside of React components
 * 
 * @param {string} path - Target route path
 * @param {Function} navigate - React Router navigate function
 * @param {Object} options - Navigation options
 * @returns {Promise<boolean>} True if navigation succeeded
 */
export const navigateToScreen = async (path, navigate, options = {}) => {
  const {
    state = null,
    replace = false
  } = options;
  
  navigate(path, {
    replace,
    state: {
      ...state,
      navigationId: generateNavigationId(),
      timestamp: new Date().toISOString()
    }
  });
  
  return true;
};

/**
 * Standalone handle exit function
 * Can be used outside of React components
 * 
 * @param {Function} navigate - React Router navigate function
 * @param {Object} sessionData - Current session data
 * @param {Object} options - Exit options
 * @returns {Promise<boolean>} True if exit succeeded
 */
export const handleExit = async (navigate, sessionData, options = {}) => {
  const {
    confirmExit = false,
    returnToMenu = true
  } = options;
  
  if (confirmExit) {
    const confirmed = window.confirm('Are you sure you want to exit?');
    if (!confirmed) return false;
  }
  
  const menuPath = sessionData?.user?.isAdmin 
    ? NAVIGATION_CONFIG.ROUTES.ADMIN_MENU 
    : NAVIGATION_CONFIG.ROUTES.MAIN_MENU;
  
  navigate(menuPath, {
    replace: true,
    state: {
      exitedFrom: window.location.pathname,
      returnFromExit: true
    }
  });
  
  return true;
};

/**
 * Standalone navigate with state function
 * Can be used outside of React components
 * 
 * @param {string} path - Target route path
 * @param {Object} state - State to preserve
 * @param {Function} navigate - React Router navigate function
 * @param {Object} options - Navigation options
 * @returns {Promise<boolean>} True if navigation succeeded
 */
export const navigateWithState = async (path, state, navigate, options = {}) => {
  navigate(path, {
    ...options,
    state: {
      ...state,
      preservedContext: true,
      timestamp: new Date().toISOString()
    }
  });
  
  return true;
};

/**
 * Standalone go back function
 * Can be used outside of React components
 * 
 * @param {Function} navigate - React Router navigate function
 * @param {number} steps - Number of steps to go back
 * @returns {boolean} True if back navigation succeeded
 */
export const goBack = (navigate, steps = 1) => {
  navigate(-steps);
  return true;
};

/**
 * Standalone navigate if authorized function
 * Can be used outside of React components
 * 
 * @param {string} path - Target route path
 * @param {Object} sessionData - Current session data
 * @param {Function} navigate - React Router navigate function
 * @param {Object} options - Navigation options
 * @returns {Promise<boolean>} True if navigation succeeded
 */
export const navigateIfAuthorized = async (path, sessionData, navigate, options = {}) => {
  // Check authorization
  if (isProtectedRoute(path) && !sessionData?.isActive) {
    navigate(NAVIGATION_CONFIG.ROUTES.LOGIN, {
      replace: true,
      state: {
        returnUrl: path,
        reason: 'AUTH_REQUIRED'
      }
    });
    return false;
  }
  
  if (isAdminRoute(path) && !sessionData?.user?.isAdmin) {
    const redirectPath = NAVIGATION_CONFIG.ROUTES.MAIN_MENU;
    navigate(redirectPath, {
      replace: true,
      state: {
        error: 'Access denied',
        message: 'You do not have permission to access this resource'
      }
    });
    return false;
  }
  
  return await navigateToScreen(path, navigate, options);
};

/**
 * Standalone can go back function
 * Can be used outside of React components
 * 
 * @returns {boolean} True if back navigation is possible
 */
export const canGoBack = () => {
  const history = getNavigationHistory();
  return history.length > 1;
};

// ============================================================================
// EXPORTS
// ============================================================================

export default useNavigation;
export { NAVIGATION_CONFIG, NAVIGATION_STATES };