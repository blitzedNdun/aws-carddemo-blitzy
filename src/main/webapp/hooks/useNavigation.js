/**
 * useNavigation.js - Custom React Hook for CICS XCTL-Equivalent Navigation
 * 
 * Implements programmatic navigation control through React Router, providing 
 * CICS XCTL-equivalent inter-component navigation with declarative routing,
 * session state preservation, and browser history management that maintains
 * original mainframe transaction flow patterns.
 * 
 * This hook replaces EXEC CICS XCTL commands with modern React Router navigation
 * while preserving identical transaction boundaries, return path logic, and
 * pseudo-conversational state management through JWT tokens and Redis session storage.
 * 
 * Key Features:
 * - Programmatic navigation control with React Router useNavigate() hook
 * - Protected routes with JWT token validation and role-based access control
 * - Navigation state management using location.state preservation
 * - Browser history API integration for F3/PF3 return functionality
 * - Session correlation and context preservation across route transitions
 * - Error boundary navigation with automatic recovery patterns
 * - Dynamic route parameters replacing COMMAREA data passing
 * - Timeout-aware navigation with automatic logout redirection
 * 
 * Architecture Notes:
 * - Maintains compatibility with existing CICS transaction flow patterns
 * - Provides exact functional equivalence to CICS program control operations
 * - Integrates with Spring Cloud Gateway routing for microservice communication
 * - Supports graceful degradation when authentication services are unavailable
 * 
 * @author Blitzy Platform - CardDemo Migration Team
 * @version 1.0.0
 * @since 2024-01-01
 */

import { useEffect, useCallback, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useSessionState } from './useSessionState.js';
import { isTokenExpired } from '../utils/authUtils.js';

// =============================================================================
// NAVIGATION CONSTANTS AND CONFIGURATION
// =============================================================================

/**
 * Navigation Configuration Constants
 * Based on CICS transaction flow patterns and Spring Cloud Gateway routing
 */
const NAVIGATION_CONFIG = {
  // Route mappings from CICS transaction codes to React Router paths
  TRANSACTION_ROUTES: {
    // Authentication and Menu Navigation
    'COSGN': '/login',                    // Sign-on transaction
    'COMEN': '/menu',                     // Main menu transaction
    'COADM': '/admin',                    // Admin menu transaction
    
    // Account Management Transactions
    'COACTVW': '/account/view',           // Account view transaction
    'COACTUP': '/account/update',         // Account update transaction
    'COACTAD': '/account/add',            // Account add transaction
    
    // Card Management Transactions
    'COCRDLI': '/card/list',              // Card list transaction
    'COCRDSL': '/card/select',            // Card select transaction
    'COCRDUP': '/card/update',            // Card update transaction
    'COCRDAD': '/card/add',               // Card add transaction
    
    // Transaction Processing
    'COTRN00': '/transaction/list',       // Transaction list
    'COTRN01': '/transaction/view',       // Transaction view
    'COTRN02': '/transaction/add',        // Transaction add
    
    // User Management Transactions
    'COUSR00': '/user/list',              // User list transaction
    'COUSR01': '/user/view',              // User view transaction
    'COUSR02': '/user/update',            // User update transaction
    'COUSR03': '/user/add',               // User add transaction
    
    // Reporting and Bill Payment
    'CORPT00': '/reports',                // Report generation
    'COBIL00': '/billpay',                // Bill payment
    
    // System and Error Routes
    'ERROR': '/error',                    // System error display
    'LOGOUT': '/logout',                  // Logout processing
    'TIMEOUT': '/timeout'                 // Session timeout
  },
  
  // Default routes for navigation fallback
  DEFAULT_ROUTES: {
    LOGIN: '/login',
    MENU: '/menu',
    ADMIN_MENU: '/admin',
    ERROR: '/error',
    UNAUTHORIZED: '/unauthorized',
    NOT_FOUND: '/not-found'
  },
  
  // Navigation state keys for location.state preservation
  STATE_KEYS: {
    RETURN_PATH: 'returnPath',
    RETURN_STATE: 'returnState',
    TRANSACTION_DATA: 'transactionData',
    ERROR_CONTEXT: 'errorContext',
    NAVIGATION_CONTEXT: 'navigationContext',
    SESSION_CORRELATION: 'sessionCorrelation'
  },
  
  // Function key mappings for navigation operations
  FUNCTION_KEYS: {
    F3: 'F3',        // Exit/Return
    F4: 'F4',        // Clear
    F5: 'F5',        // Refresh
    F7: 'F7',        // Page backward
    F8: 'F8',        // Page forward
    F12: 'F12',      // Cancel
    ENTER: 'Enter',  // Submit/Continue
    ESCAPE: 'Escape' // Cancel/Exit
  },
  
  // Navigation timing and retry configuration
  NAVIGATION_TIMING: {
    REDIRECT_DELAY_MS: 100,          // Delay before navigation redirect
    RETRY_ATTEMPTS: 3,               // Maximum navigation retry attempts
    RETRY_DELAY_MS: 500,             // Delay between retry attempts
    LOGOUT_REDIRECT_DELAY_MS: 2000   // Delay before logout redirect
  }
};

/**
 * Navigation Event Types
 * Defines all possible navigation lifecycle events
 */
const NAVIGATION_EVENTS = {
  NAVIGATE: 'navigation:navigate',
  NAVIGATE_PROTECTED: 'navigation:navigate_protected',
  NAVIGATE_BACK: 'navigation:navigate_back',
  NAVIGATE_EXIT: 'navigation:navigate_exit',
  NAVIGATE_ERROR: 'navigation:navigate_error',
  NAVIGATE_TIMEOUT: 'navigation:navigate_timeout',
  NAVIGATE_UNAUTHORIZED: 'navigation:navigate_unauthorized'
};

// =============================================================================
// UTILITY FUNCTIONS
// =============================================================================

/**
 * Validates if a route path is protected and requires authentication
 * 
 * @param {string} path - Route path to validate
 * @returns {boolean} True if route requires authentication
 */
const isProtectedRoute = (path) => {
  // Public routes that don't require authentication
  const publicRoutes = [
    '/login',
    '/logout',
    '/error',
    '/unauthorized',
    '/not-found',
    '/timeout'
  ];
  
  return !publicRoutes.includes(path) && !path.startsWith('/public');
};

/**
 * Validates if user has permission to access a specific route
 * 
 * @param {string} path - Route path to validate
 * @param {Object} user - Current user context
 * @returns {boolean} True if user has access permission
 */
const hasRoutePermission = (path, user) => {
  if (!user || !user.roles) return false;
  
  // Admin routes require admin role
  if (path.startsWith('/admin')) {
    return user.roles.includes('ROLE_ADMIN');
  }
  
  // User management routes require admin role
  if (path.startsWith('/user')) {
    return user.roles.includes('ROLE_ADMIN');
  }
  
  // All other protected routes require user role
  return user.roles.includes('ROLE_USER') || user.roles.includes('ROLE_ADMIN');
};

/**
 * Constructs navigation state object for location.state preservation
 * 
 * @param {Object} options - Navigation options
 * @param {string} options.returnPath - Return path for F3/back navigation
 * @param {Object} options.returnState - Return state data
 * @param {Object} options.transactionData - Transaction data to pass
 * @param {Object} options.errorContext - Error context information
 * @param {string} options.sessionCorrelation - Session correlation ID
 * @returns {Object} Navigation state object
 */
const buildNavigationState = (options = {}) => {
  const {
    returnPath,
    returnState,
    transactionData,
    errorContext,
    sessionCorrelation
  } = options;
  
  return {
    [NAVIGATION_CONFIG.STATE_KEYS.RETURN_PATH]: returnPath,
    [NAVIGATION_CONFIG.STATE_KEYS.RETURN_STATE]: returnState,
    [NAVIGATION_CONFIG.STATE_KEYS.TRANSACTION_DATA]: transactionData,
    [NAVIGATION_CONFIG.STATE_KEYS.ERROR_CONTEXT]: errorContext,
    [NAVIGATION_CONFIG.STATE_KEYS.NAVIGATION_CONTEXT]: {
      timestamp: new Date().toISOString(),
      sessionCorrelation: sessionCorrelation,
      userAgent: navigator.userAgent,
      referrer: document.referrer
    }
  };
};

/**
 * Extracts transaction code from route path
 * 
 * @param {string} path - Route path
 * @returns {string|null} Transaction code or null if not found
 */
const getTransactionCodeFromPath = (path) => {
  const routeEntry = Object.entries(NAVIGATION_CONFIG.TRANSACTION_ROUTES)
    .find(([, routePath]) => routePath === path);
  
  return routeEntry ? routeEntry[0] : null;
};

// =============================================================================
// MAIN HOOK IMPLEMENTATION
// =============================================================================

/**
 * useNavigation - Main Navigation Hook
 * 
 * Provides comprehensive navigation management with CICS XCTL-equivalent
 * functionality, session state preservation, and authentication integration.
 * 
 * @returns {Object} Navigation state and management functions
 */
const useNavigation = () => {
  // =============================================================================
  // HOOKS AND STATE MANAGEMENT
  // =============================================================================
  
  // React Router hooks
  const navigate = useNavigate();
  const location = useLocation();
  
  // Session state management
  const {
    isAuthenticated,
    currentUser,
    sessionId,
    isSessionValid,
    terminateSession,
    storeNavigationContext,
    recordActivity
  } = useSessionState();
  
  // Navigation state
  const [navigationHistory, setNavigationHistory] = useState([]);
  const [isNavigating, setIsNavigating] = useState(false);
  const [navigationError, setNavigationError] = useState(null);
  const [canNavigateBack, setCanNavigateBack] = useState(false);
  
  // Refs for navigation management
  const navigationTimeoutRef = useRef(null);
  const retryCountRef = useRef(0);
  const lastNavigationRef = useRef(null);
  
  // =============================================================================
  // NAVIGATION UTILITY FUNCTIONS
  // =============================================================================
  
  /**
   * Records navigation activity and updates session context
   * 
   * @param {string} fromPath - Source path
   * @param {string} toPath - Destination path
   * @param {Object} options - Navigation options
   */
  const recordNavigation = useCallback((fromPath, toPath, options = {}) => {
    try {
      // Record user activity for session management
      recordActivity();
      
      // Update navigation history
      setNavigationHistory(prev => [
        ...prev.slice(-9), // Keep only last 9 entries
        {
          from: fromPath,
          to: toPath,
          timestamp: new Date().toISOString(),
          sessionId: sessionId,
          options: options
        }
      ]);
      
      // Store navigation context in session
      storeNavigationContext({
        from: fromPath,
        to: toPath,
        timestamp: new Date().toISOString(),
        sessionId: sessionId,
        state: options.state || null
      });
      
      // Update last navigation reference
      lastNavigationRef.current = {
        from: fromPath,
        to: toPath,
        timestamp: Date.now()
      };
      
    } catch (error) {
      console.error('Error recording navigation:', error);
    }
  }, [sessionId, recordActivity, storeNavigationContext]);
  
  /**
   * Validates navigation request and session state
   * 
   * @param {string} path - Target path
   * @param {Object} options - Navigation options
   * @returns {Object} Validation result
   */
  const validateNavigation = useCallback((path, options = {}) => {
    try {
      // Check if navigation is currently in progress
      if (isNavigating) {
        return {
          isValid: false,
          error: 'Navigation already in progress',
          errorCode: 'NAVIGATION_IN_PROGRESS'
        };
      }
      
      // Validate path format
      if (!path || typeof path !== 'string') {
        return {
          isValid: false,
          error: 'Invalid navigation path',
          errorCode: 'INVALID_PATH'
        };
      }
      
      // Check if route requires authentication
      if (isProtectedRoute(path)) {
        // Validate session state
        if (!isSessionValid()) {
          return {
            isValid: false,
            error: 'Session expired or invalid',
            errorCode: 'SESSION_INVALID',
            redirectTo: NAVIGATION_CONFIG.DEFAULT_ROUTES.LOGIN
          };
        }
        
        // Check user authorization
        if (!hasRoutePermission(path, currentUser)) {
          return {
            isValid: false,
            error: 'Insufficient permissions for route',
            errorCode: 'INSUFFICIENT_PERMISSIONS',
            redirectTo: NAVIGATION_CONFIG.DEFAULT_ROUTES.UNAUTHORIZED
          };
        }
      }
      
      // Check for duplicate navigation (prevent double-click issues)
      if (lastNavigationRef.current && 
          Date.now() - lastNavigationRef.current.timestamp < 500 &&
          lastNavigationRef.current.to === path) {
        return {
          isValid: false,
          error: 'Duplicate navigation request',
          errorCode: 'DUPLICATE_NAVIGATION'
        };
      }
      
      return {
        isValid: true,
        message: 'Navigation validation successful'
      };
      
    } catch (error) {
      console.error('Navigation validation error:', error);
      return {
        isValid: false,
        error: 'Navigation validation failed',
        errorCode: 'VALIDATION_ERROR',
        details: error.message
      };
    }
  }, [isNavigating, isSessionValid, currentUser]);
  
  // =============================================================================
  // CORE NAVIGATION FUNCTIONS
  // =============================================================================
  
  /**
   * Navigates to a specific screen with state preservation
   * Replaces EXEC CICS XCTL functionality with React Router navigation
   * 
   * @param {string} path - Target route path
   * @param {Object} options - Navigation options
   * @param {Object} options.state - State to pass to destination
   * @param {boolean} options.replace - Whether to replace current history entry
   * @param {string} options.returnPath - Return path for F3 navigation
   * @param {Object} options.transactionData - Transaction data to pass
   * @returns {Promise<Object>} Navigation result
   */
  const navigateToScreen = useCallback(async (path, options = {}) => {
    try {
      setIsNavigating(true);
      setNavigationError(null);
      
      // Validate navigation request
      const validation = validateNavigation(path, options);
      if (!validation.isValid) {
        if (validation.redirectTo) {
          // Redirect to appropriate page for authentication/authorization issues
          navigate(validation.redirectTo, { replace: true });
        }
        
        setNavigationError({
          error: validation.error,
          errorCode: validation.errorCode
        });
        
        return {
          success: false,
          error: validation.error,
          errorCode: validation.errorCode
        };
      }
      
      // Build navigation state with return path information
      const navigationState = buildNavigationState({
        returnPath: options.returnPath || location.pathname,
        returnState: options.returnState || location.state,
        transactionData: options.transactionData,
        sessionCorrelation: sessionId,
        ...options.state
      });
      
      // Record navigation activity
      recordNavigation(location.pathname, path, options);
      
      // Perform navigation
      const navigateOptions = {
        replace: options.replace || false,
        state: navigationState
      };
      
      // Add retry logic for navigation failures
      let retryCount = 0;
      while (retryCount < NAVIGATION_CONFIG.NAVIGATION_TIMING.RETRY_ATTEMPTS) {
        try {
          navigate(path, navigateOptions);
          
          // Set navigation timeout for cleanup
          if (navigationTimeoutRef.current) {
            clearTimeout(navigationTimeoutRef.current);
          }
          
          navigationTimeoutRef.current = setTimeout(() => {
            setIsNavigating(false);
          }, NAVIGATION_CONFIG.NAVIGATION_TIMING.REDIRECT_DELAY_MS);
          
          return {
            success: true,
            path: path,
            state: navigationState,
            message: 'Navigation successful'
          };
          
        } catch (navError) {
          retryCount++;
          if (retryCount < NAVIGATION_CONFIG.NAVIGATION_TIMING.RETRY_ATTEMPTS) {
            await new Promise(resolve => 
              setTimeout(resolve, NAVIGATION_CONFIG.NAVIGATION_TIMING.RETRY_DELAY_MS)
            );
          } else {
            throw navError;
          }
        }
      }
      
    } catch (error) {
      console.error('Navigation error:', error);
      
      setNavigationError({
        error: error.message,
        errorCode: 'NAVIGATION_ERROR',
        details: error.stack
      });
      
      return {
        success: false,
        error: error.message,
        errorCode: 'NAVIGATION_ERROR'
      };
      
    } finally {
      setIsNavigating(false);
    }
  }, [navigate, location, sessionId, validateNavigation, recordNavigation]);
  
  /**
   * Navigates to main menu based on user role
   * Implements CICS menu navigation with role-based routing
   * 
   * @param {Object} options - Navigation options
   * @returns {Promise<Object>} Navigation result
   */
  const navigateToMenu = useCallback(async (options = {}) => {
    try {
      // Determine appropriate menu based on user role
      const menuPath = currentUser?.roles?.includes('ROLE_ADMIN') 
        ? NAVIGATION_CONFIG.DEFAULT_ROUTES.ADMIN_MENU 
        : NAVIGATION_CONFIG.DEFAULT_ROUTES.MENU;
      
      return await navigateToScreen(menuPath, {
        ...options,
        replace: true, // Replace login page in history
        transactionData: {
          menuType: currentUser?.roles?.includes('ROLE_ADMIN') ? 'ADMIN' : 'USER',
          userRole: currentUser?.roles?.[0] || 'ROLE_USER'
        }
      });
      
    } catch (error) {
      console.error('Menu navigation error:', error);
      return {
        success: false,
        error: error.message,
        errorCode: 'MENU_NAVIGATION_ERROR'
      };
    }
  }, [currentUser, navigateToScreen]);
  
  /**
   * Handles exit navigation (F3/PF3 functionality)
   * Implements CICS return/exit logic with history management
   * 
   * @param {Object} options - Exit options
   * @returns {Promise<Object>} Navigation result
   */
  const handleExit = useCallback(async (options = {}) => {
    try {
      // Get return path from location state
      const returnPath = location.state?.[NAVIGATION_CONFIG.STATE_KEYS.RETURN_PATH];
      const returnState = location.state?.[NAVIGATION_CONFIG.STATE_KEYS.RETURN_STATE];
      
      // Determine exit destination
      let exitPath;
      if (options.forcePath) {
        exitPath = options.forcePath;
      } else if (returnPath && returnPath !== location.pathname) {
        exitPath = returnPath;
      } else {
        // Default to menu navigation
        exitPath = currentUser?.roles?.includes('ROLE_ADMIN') 
          ? NAVIGATION_CONFIG.DEFAULT_ROUTES.ADMIN_MENU 
          : NAVIGATION_CONFIG.DEFAULT_ROUTES.MENU;
      }
      
      return await navigateToScreen(exitPath, {
        replace: options.replace || false,
        state: returnState,
        transactionData: {
          exitFrom: location.pathname,
          exitReason: options.reason || 'USER_EXIT'
        }
      });
      
    } catch (error) {
      console.error('Exit navigation error:', error);
      return {
        success: false,
        error: error.message,
        errorCode: 'EXIT_NAVIGATION_ERROR'
      };
    }
  }, [location, currentUser, navigateToScreen]);
  
  /**
   * Navigates with state preservation
   * Provides enhanced navigation with complex state management
   * 
   * @param {string} path - Target path
   * @param {Object} state - State to preserve
   * @param {Object} options - Navigation options
   * @returns {Promise<Object>} Navigation result
   */
  const navigateWithState = useCallback(async (path, state, options = {}) => {
    return await navigateToScreen(path, {
      ...options,
      state: {
        ...state,
        preservedState: true
      },
      transactionData: {
        ...options.transactionData,
        statePreserved: true,
        stateKeys: Object.keys(state)
      }
    });
  }, [navigateToScreen]);
  
  /**
   * Navigates back in browser history
   * Implements browser back functionality with state preservation
   * 
   * @param {Object} options - Navigation options
   * @returns {Promise<Object>} Navigation result
   */
  const goBack = useCallback(async (options = {}) => {
    try {
      setIsNavigating(true);
      
      // Check if we can go back in history
      if (navigationHistory.length === 0) {
        return await navigateToMenu(options);
      }
      
      // Record navigation activity
      recordNavigation(location.pathname, 'BACK', options);
      
      // Use browser history API
      const steps = options.steps || -1;
      navigate(steps);
      
      return {
        success: true,
        action: 'BACK',
        steps: steps,
        message: 'Back navigation successful'
      };
      
    } catch (error) {
      console.error('Back navigation error:', error);
      return {
        success: false,
        error: error.message,
        errorCode: 'BACK_NAVIGATION_ERROR'
      };
    } finally {
      setIsNavigating(false);
    }
  }, [navigationHistory, location, navigate, navigateToMenu, recordNavigation]);
  
  /**
   * Navigates only if user is authorized
   * Implements authorization-aware navigation with fallback
   * 
   * @param {string} path - Target path
   * @param {Object} options - Navigation options
   * @returns {Promise<Object>} Navigation result
   */
  const navigateIfAuthorized = useCallback(async (path, options = {}) => {
    try {
      // Check authorization first
      if (isProtectedRoute(path) && !hasRoutePermission(path, currentUser)) {
        return await navigateToScreen(NAVIGATION_CONFIG.DEFAULT_ROUTES.UNAUTHORIZED, {
          replace: true,
          state: {
            attemptedPath: path,
            requiredRole: path.startsWith('/admin') ? 'ROLE_ADMIN' : 'ROLE_USER'
          }
        });
      }
      
      return await navigateToScreen(path, options);
      
    } catch (error) {
      console.error('Authorized navigation error:', error);
      return {
        success: false,
        error: error.message,
        errorCode: 'AUTHORIZED_NAVIGATION_ERROR'
      };
    }
  }, [currentUser, navigateToScreen]);
  
  /**
   * Checks if back navigation is possible
   * 
   * @returns {boolean} True if back navigation is available
   */
  const canGoBack = useCallback(() => {
    return navigationHistory.length > 0 || 
           (window.history.length > 1 && 
            location.state?.[NAVIGATION_CONFIG.STATE_KEYS.RETURN_PATH]);
  }, [navigationHistory, location]);
  
  // =============================================================================
  // SIDE EFFECTS AND LIFECYCLE MANAGEMENT
  // =============================================================================
  
  /**
   * Update navigation state based on location changes
   */
  useEffect(() => {
    setCanNavigateBack(canGoBack());
  }, [canGoBack]);
  
  /**
   * Handle session expiration during navigation
   */
  useEffect(() => {
    const checkSessionDuringNavigation = async () => {
      if (isNavigating && !isSessionValid()) {
        console.log('Session expired during navigation, redirecting to login');
        
        setIsNavigating(false);
        await terminateSession({ redirect: true, reason: 'SESSION_EXPIRED' });
      }
    };
    
    checkSessionDuringNavigation();
  }, [isNavigating, isSessionValid, terminateSession]);
  
  /**
   * Clean up navigation timeouts on unmount
   */
  useEffect(() => {
    return () => {
      if (navigationTimeoutRef.current) {
        clearTimeout(navigationTimeoutRef.current);
      }
    };
  }, []);
  
  // =============================================================================
  // RETURN HOOK API
  // =============================================================================
  
  return {
    // Navigation state
    isNavigating,
    navigationError,
    navigationHistory,
    canNavigateBack,
    currentPath: location.pathname,
    currentState: location.state,
    
    // Core navigation functions
    navigateToScreen,
    navigateToMenu,
    handleExit,
    navigateWithState,
    goBack,
    navigateIfAuthorized,
    canGoBack,
    
    // Utility functions
    isProtectedRoute,
    hasRoutePermission: (path) => hasRoutePermission(path, currentUser),
    getTransactionCode: () => getTransactionCodeFromPath(location.pathname),
    
    // State extraction helpers
    getReturnPath: () => location.state?.[NAVIGATION_CONFIG.STATE_KEYS.RETURN_PATH],
    getTransactionData: () => location.state?.[NAVIGATION_CONFIG.STATE_KEYS.TRANSACTION_DATA],
    getNavigationContext: () => location.state?.[NAVIGATION_CONFIG.STATE_KEYS.NAVIGATION_CONTEXT],
    
    // Configuration constants
    TRANSACTION_ROUTES: NAVIGATION_CONFIG.TRANSACTION_ROUTES,
    DEFAULT_ROUTES: NAVIGATION_CONFIG.DEFAULT_ROUTES,
    NAVIGATION_EVENTS
  };
};

// =============================================================================
// ADDITIONAL EXPORTED FUNCTIONS
// =============================================================================

/**
 * Navigates to menu screen
 * Standalone function for use outside of React components
 * 
 * @param {Object} navigate - React Router navigate function
 * @param {Object} user - Current user context
 * @param {Object} options - Navigation options
 * @returns {Promise<void>} Navigation completion
 */
export const navigateToMenu = async (navigate, user, options = {}) => {
  try {
    const menuPath = user?.roles?.includes('ROLE_ADMIN') 
      ? NAVIGATION_CONFIG.DEFAULT_ROUTES.ADMIN_MENU 
      : NAVIGATION_CONFIG.DEFAULT_ROUTES.MENU;
    
    navigate(menuPath, {
      replace: true,
      state: buildNavigationState({
        transactionData: {
          menuType: user?.roles?.includes('ROLE_ADMIN') ? 'ADMIN' : 'USER',
          userRole: user?.roles?.[0] || 'ROLE_USER'
        },
        ...options.state
      })
    });
  } catch (error) {
    console.error('Menu navigation error:', error);
  }
};

/**
 * Navigates to a specific screen
 * Standalone function for use outside of React components
 * 
 * @param {Object} navigate - React Router navigate function
 * @param {string} path - Target path
 * @param {Object} options - Navigation options
 * @returns {Promise<void>} Navigation completion
 */
export const navigateToScreen = async (navigate, path, options = {}) => {
  try {
    navigate(path, {
      replace: options.replace || false,
      state: buildNavigationState({
        returnPath: options.returnPath,
        transactionData: options.transactionData,
        ...options.state
      })
    });
  } catch (error) {
    console.error('Screen navigation error:', error);
  }
};

/**
 * Handles exit navigation
 * Standalone function for use outside of React components
 * 
 * @param {Object} navigate - React Router navigate function
 * @param {Object} location - Current location
 * @param {Object} user - Current user context
 * @param {Object} options - Exit options
 * @returns {Promise<void>} Navigation completion
 */
export const handleExit = async (navigate, location, user, options = {}) => {
  try {
    const returnPath = location.state?.[NAVIGATION_CONFIG.STATE_KEYS.RETURN_PATH];
    const returnState = location.state?.[NAVIGATION_CONFIG.STATE_KEYS.RETURN_STATE];
    
    let exitPath;
    if (options.forcePath) {
      exitPath = options.forcePath;
    } else if (returnPath && returnPath !== location.pathname) {
      exitPath = returnPath;
    } else {
      exitPath = user?.roles?.includes('ROLE_ADMIN') 
        ? NAVIGATION_CONFIG.DEFAULT_ROUTES.ADMIN_MENU 
        : NAVIGATION_CONFIG.DEFAULT_ROUTES.MENU;
    }
    
    navigate(exitPath, {
      replace: options.replace || false,
      state: returnState
    });
  } catch (error) {
    console.error('Exit navigation error:', error);
  }
};

/**
 * Navigates with state preservation
 * Standalone function for use outside of React components
 * 
 * @param {Object} navigate - React Router navigate function
 * @param {string} path - Target path
 * @param {Object} state - State to preserve
 * @param {Object} options - Navigation options
 * @returns {Promise<void>} Navigation completion
 */
export const navigateWithState = async (navigate, path, state, options = {}) => {
  try {
    navigate(path, {
      replace: options.replace || false,
      state: buildNavigationState({
        ...state,
        preservedState: true,
        ...options.state
      })
    });
  } catch (error) {
    console.error('State navigation error:', error);
  }
};

/**
 * Navigates back in browser history
 * Standalone function for use outside of React components
 * 
 * @param {Object} navigate - React Router navigate function
 * @param {Object} options - Navigation options
 * @returns {Promise<void>} Navigation completion
 */
export const goBack = async (navigate, options = {}) => {
  try {
    const steps = options.steps || -1;
    navigate(steps);
  } catch (error) {
    console.error('Back navigation error:', error);
  }
};

/**
 * Navigates only if user is authorized
 * Standalone function for use outside of React components
 * 
 * @param {Object} navigate - React Router navigate function
 * @param {string} path - Target path
 * @param {Object} user - Current user context
 * @param {Object} options - Navigation options
 * @returns {Promise<void>} Navigation completion
 */
export const navigateIfAuthorized = async (navigate, path, user, options = {}) => {
  try {
    if (isProtectedRoute(path) && !hasRoutePermission(path, user)) {
      navigate(NAVIGATION_CONFIG.DEFAULT_ROUTES.UNAUTHORIZED, {
        replace: true,
        state: buildNavigationState({
          attemptedPath: path,
          requiredRole: path.startsWith('/admin') ? 'ROLE_ADMIN' : 'ROLE_USER'
        })
      });
      return;
    }
    
    await navigateToScreen(navigate, path, options);
  } catch (error) {
    console.error('Authorized navigation error:', error);
  }
};

/**
 * Checks if back navigation is possible
 * Standalone function for use outside of React components
 * 
 * @param {Object} location - Current location
 * @returns {boolean} True if back navigation is available
 */
export const canGoBack = (location) => {
  return window.history.length > 1 || 
         location.state?.[NAVIGATION_CONFIG.STATE_KEYS.RETURN_PATH];
};

// Export the main hook as default
export default useNavigation;