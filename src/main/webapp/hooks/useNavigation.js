/**
 * useNavigation.js
 * 
 * Custom React hook implementing CICS XCTL-equivalent navigation patterns through React Router,
 * providing programmatic navigation control with history management, route guards, and dynamic
 * route parameters that preserve original mainframe transaction flow while enabling modern
 * URL-based navigation.
 * 
 * This hook replaces traditional CICS program control operations (EXEC CICS XCTL, EXEC CICS LINK)
 * with React Router-based navigation that maintains identical transaction boundaries and flow
 * control while providing enhanced user experience through browser history integration and
 * state preservation mechanisms.
 * 
 * Key Features:
 * - Programmatic navigation control replacing EXEC CICS XCTL operations
 * - Protected route access with JWT token validation
 * - Navigation state management with location.state preservation
 * - History management supporting F3/PF3 return functionality
 * - Dynamic route parameters for context passing equivalent to COMMAREA
 * - Session timeout detection with automatic logout redirection
 * - Error boundary navigation for recovery procedures
 * - Transaction boundary maintenance aligned with original CICS flow
 * 
 * @fileoverview CardDemo Navigation Management Hook
 * @version 1.0.0
 * @author Blitzy Platform - CardDemo Migration Team
 * @copyright 2024 CardDemo Application Migration Project
 * @license Apache-2.0
 */

import { useEffect, useState, useCallback, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useSessionState } from './useSessionState.js';
import { isTokenExpired } from '../utils/authUtils.js';

// ============================================================================
// NAVIGATION CONFIGURATION AND CONSTANTS
// ============================================================================

/**
 * Navigation configuration constants aligned with CICS transaction codes
 * and React Router path structures for consistent navigation behavior
 */
const NAVIGATION_CONFIG = {
  // Route paths mapped to original CICS transaction codes
  ROUTES: {
    LOGIN: '/login',           // CC00 - COSGN00
    MENU: '/menu',             // CM00 - COMEN01
    ADMIN_MENU: '/admin',      // CA00 - COADM01
    ACCOUNT_VIEW: '/accounts/view',     // CAVW - COACTVW
    ACCOUNT_UPDATE: '/accounts/update', // CAUP - COACTUP
    CARD_LIST: '/cards/list',           // CCLI - COCRDLI
    CARD_DETAIL: '/cards/detail',       // CCDL - COCRDSL
    CARD_UPDATE: '/cards/update',       // CCUP - COCRDUP
    TRANSACTION_LIST: '/transactions/list',  // CT00 - COTRN00
    TRANSACTION_ADD: '/transactions/add',    // CT02 - COTRN02
    BILL_PAYMENT: '/payments/bill',          // CB00 - COBIL00
    USER_LIST: '/users/list',                // CU00 - COUSR00
    USER_ADD: '/users/add',                  // CU01 - COUSR01
    USER_UPDATE: '/users/update',            // CU02 - COUSR02
    REPORT_GENERATION: '/reports/generate'    // CR00 - CORPT00
  },

  // Admin-only routes requiring ROLE_ADMIN authority
  ADMIN_ROUTES: [
    '/admin',
    '/users/list',
    '/users/add',
    '/users/update',
    '/reports/generate'
  ],

  // Navigation state keys for React Router location.state
  STATE_KEYS: {
    RETURN_PATH: 'returnPath',
    CONTEXT_DATA: 'contextData',
    ERROR_CONTEXT: 'errorContext',
    TRANSACTION_ID: 'transactionId',
    PREVIOUS_COMPONENT: 'previousComponent'
  },

  // Navigation events for audit trail and monitoring
  NAVIGATION_EVENTS: {
    NAVIGATION_START: 'navigation_start',
    NAVIGATION_SUCCESS: 'navigation_success',
    NAVIGATION_BLOCKED: 'navigation_blocked',
    NAVIGATION_ERROR: 'navigation_error',
    UNAUTHORIZED_ACCESS: 'unauthorized_access',
    SESSION_EXPIRED: 'session_expired'
  }
} as const;

// ============================================================================
// NAVIGATION UTILITY FUNCTIONS
// ============================================================================

/**
 * Validates if user has permission to access a specific route
 * Implements role-based access control equivalent to CICS resource authorization
 * 
 * @param {string} path - Target route path to validate
 * @param {Object} sessionState - Current user session information
 * @returns {Object} Validation result with access permission and details
 */
const validateRouteAccess = (path, sessionState) => {
  try {
    // Check if user is authenticated
    if (!sessionState || !sessionState.isAuthenticated) {
      return {
        allowed: false,
        reason: 'NOT_AUTHENTICATED',
        redirectTo: NAVIGATION_CONFIG.ROUTES.LOGIN,
        message: 'Authentication required to access this resource'
      };
    }

    // Check for admin-only routes
    const isAdminRoute = NAVIGATION_CONFIG.ADMIN_ROUTES.some(adminPath => 
      path.startsWith(adminPath)
    );

    if (isAdminRoute) {
      const userRole = sessionState.sessionState?.userType;
      const hasAdminRole = userRole === 'A'; // CICS SEC-USR-TYPE Admin

      if (!hasAdminRole) {
        return {
          allowed: false,
          reason: 'INSUFFICIENT_PRIVILEGES',
          redirectTo: NAVIGATION_CONFIG.ROUTES.MENU,
          message: 'Administrative privileges required to access this resource'
        };
      }
    }

    return {
      allowed: true,
      reason: 'AUTHORIZED',
      message: 'Access granted'
    };

  } catch (error) {
    console.error('Route access validation error:', error);
    return {
      allowed: false,
      reason: 'VALIDATION_ERROR',
      redirectTo: NAVIGATION_CONFIG.ROUTES.LOGIN,
      message: 'Unable to validate route access'
    };
  }
};

/**
 * Creates standardized navigation state object for React Router location.state
 * Maintains context information equivalent to CICS COMMAREA data passing
 * 
 * @param {string} fromPath - Source route path for return navigation
 * @param {Object} contextData - Additional context data to preserve
 * @param {Object} options - Navigation options and metadata
 * @returns {Object} Navigation state object for React Router
 */
const createNavigationState = (fromPath, contextData = null, options = {}) => {
  const navigationState = {
    [NAVIGATION_CONFIG.STATE_KEYS.RETURN_PATH]: fromPath,
    [NAVIGATION_CONFIG.STATE_KEYS.CONTEXT_DATA]: contextData,
    [NAVIGATION_CONFIG.STATE_KEYS.TRANSACTION_ID]: options.transactionId || `nav-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
    [NAVIGATION_CONFIG.STATE_KEYS.PREVIOUS_COMPONENT]: options.previousComponent || null,
    timestamp: new Date().toISOString(),
    userId: options.userId || null,
    sessionId: options.sessionId || null
  };

  // Add error context if provided
  if (options.errorContext) {
    navigationState[NAVIGATION_CONFIG.STATE_KEYS.ERROR_CONTEXT] = options.errorContext;
  }

  return navigationState;
};

/**
 * Extracts context data from React Router location state
 * Provides access to preserved navigation context equivalent to CICS data areas
 * 
 * @param {Object} location - React Router location object
 * @returns {Object} Extracted context data and navigation information
 */
const extractNavigationContext = (location) => {
  const state = location.state || {};
  
  return {
    returnPath: state[NAVIGATION_CONFIG.STATE_KEYS.RETURN_PATH] || null,
    contextData: state[NAVIGATION_CONFIG.STATE_KEYS.CONTEXT_DATA] || null,
    errorContext: state[NAVIGATION_CONFIG.STATE_KEYS.ERROR_CONTEXT] || null,
    transactionId: state[NAVIGATION_CONFIG.STATE_KEYS.TRANSACTION_ID] || null,
    previousComponent: state[NAVIGATION_CONFIG.STATE_KEYS.PREVIOUS_COMPONENT] || null,
    timestamp: state.timestamp || null,
    userId: state.userId || null,
    sessionId: state.sessionId || null
  };
};

// ============================================================================
// MAIN NAVIGATION HOOK IMPLEMENTATION
// ============================================================================

/**
 * Main useNavigation hook providing comprehensive navigation management for CardDemo React application
 * Implements CICS XCTL-equivalent navigation patterns with modern React Router capabilities
 * 
 * @returns {Object} Navigation functions and state management utilities
 * 
 * Returned Object Structure:
 * - navigateToMenu: Function to navigate to role-appropriate main menu
 * - navigateToScreen: Function to navigate to specific screen with context
 * - handleExit: Function to handle F3/PF3 exit operations with return logic
 * - navigateWithState: Function to navigate with preserved state context
 * - goBack: Function to navigate back in history with fallback logic
 * - navigateIfAuthorized: Function to navigate with authorization checking
 * - canGoBack: Function to check if back navigation is available
 * - currentContext: Current navigation context from location state
 * - navigationHistory: History of navigation operations for audit trail
 * 
 * Hook Features:
 * - JWT token validation before navigation operations
 * - Role-based access control for protected routes
 * - Navigation state preservation across component transitions
 * - Browser history integration with fallback navigation logic
 * - Session timeout detection with automatic logout redirection
 * - Error context preservation for recovery procedures
 * - Audit trail maintenance for navigation operations
 */
export default function useNavigation() {
  // React Router hooks for navigation and location management
  const navigate = useNavigate();
  const location = useLocation();
  
  // Session state management for authentication and authorization
  const sessionState = useSessionState();
  
  // Navigation state management
  const [navigationHistory, setNavigationHistory] = useState([]);
  const [currentContext, setCurrentContext] = useState(null);
  const [isNavigating, setIsNavigating] = useState(false);
  
  // Navigation monitoring and cleanup
  const navigationTimer = useRef(null);
  const abortController = useRef(null);

  /**
   * Updates navigation context when location changes
   * Maintains current navigation state for context-aware operations
   */
  useEffect(() => {
    const context = extractNavigationContext(location);
    setCurrentContext(context);
    
    // Add to navigation history for audit trail
    setNavigationHistory(prev => [
      ...prev.slice(-9), // Keep last 10 entries
      {
        path: location.pathname,
        search: location.search,
        context: context,
        timestamp: new Date().toISOString(),
        userId: sessionState.sessionState?.userId || null
      }
    ]);
  }, [location, sessionState.sessionState?.userId]);

  /**
   * Monitors session state for authentication changes
   * Handles automatic logout redirection on session expiration
   */
  useEffect(() => {
    // Check for session expiration during navigation
    if (sessionState.isAuthenticated === false && location.pathname !== NAVIGATION_CONFIG.ROUTES.LOGIN) {
      console.log('Session expired during navigation, redirecting to login');
      navigate(NAVIGATION_CONFIG.ROUTES.LOGIN + '?reason=session-expired', { replace: true });
    }
  }, [sessionState.isAuthenticated, location.pathname, navigate]);

  /**
   * Navigates to appropriate main menu based on user role
   * Implements role-based menu selection equivalent to CICS menu routing
   * 
   * @param {Object} options - Navigation options and context data
   * @returns {Promise<boolean>} Navigation success status
   */
  const navigateToMenu = useCallback(async (options = {}) => {
    try {
      setIsNavigating(true);
      
      // Validate session state
      if (!sessionState.isAuthenticated) {
        console.warn('Cannot navigate to menu: user not authenticated');
        navigate(NAVIGATION_CONFIG.ROUTES.LOGIN, { replace: true });
        return false;
      }

      // Determine appropriate menu based on user role
      const userType = sessionState.sessionState?.userType;
      const targetPath = userType === 'A' ? NAVIGATION_CONFIG.ROUTES.ADMIN_MENU : NAVIGATION_CONFIG.ROUTES.MENU;
      
      // Create navigation state
      const navigationState = createNavigationState(
        location.pathname,
        options.contextData,
        {
          transactionId: options.transactionId,
          previousComponent: 'navigation_hook',
          userId: sessionState.sessionState?.userId,
          sessionId: sessionState.sessionState?.sessionId
        }
      );

      // Perform navigation
      navigate(targetPath, { state: navigationState, replace: options.replace || false });
      
      console.log(`Navigated to ${userType === 'A' ? 'admin' : 'main'} menu:`, {
        targetPath,
        userId: sessionState.sessionState?.userId,
        transactionId: navigationState.transactionId
      });

      return true;

    } catch (error) {
      console.error('Error navigating to menu:', error);
      return false;
    } finally {
      setIsNavigating(false);
    }
  }, [sessionState, location.pathname, navigate]);

  /**
   * Navigates to a specific screen with context preservation
   * Implements CICS XCTL-equivalent program transfer with data passing
   * 
   * @param {string} screenPath - Target screen route path
   * @param {Object} contextData - Context data to pass to target screen
   * @param {Object} options - Navigation options and metadata
   * @returns {Promise<boolean>} Navigation success status
   */
  const navigateToScreen = useCallback(async (screenPath, contextData = null, options = {}) => {
    try {
      setIsNavigating(true);

      // Validate screen path
      if (!screenPath || typeof screenPath !== 'string') {
        console.error('Invalid screen path provided for navigation');
        return false;
      }

      // Validate route access
      const accessValidation = validateRouteAccess(screenPath, sessionState);
      if (!accessValidation.allowed) {
        console.warn(`Navigation blocked: ${accessValidation.reason}`, {
          targetPath: screenPath,
          redirectTo: accessValidation.redirectTo,
          message: accessValidation.message
        });

        if (accessValidation.redirectTo) {
          navigate(accessValidation.redirectTo, { replace: true });
        }
        return false;
      }

      // Create navigation state with context preservation
      const navigationState = createNavigationState(
        location.pathname,
        contextData,
        {
          transactionId: options.transactionId,
          previousComponent: options.previousComponent || 'navigation_hook',
          userId: sessionState.sessionState?.userId,
          sessionId: sessionState.sessionState?.sessionId,
          errorContext: options.errorContext
        }
      );

      // Perform navigation with state preservation
      navigate(screenPath, { 
        state: navigationState, 
        replace: options.replace || false 
      });

      console.log('Screen navigation completed:', {
        targetPath: screenPath,
        hasContext: !!contextData,
        transactionId: navigationState.transactionId,
        userId: sessionState.sessionState?.userId
      });

      return true;

    } catch (error) {
      console.error('Error navigating to screen:', error);
      return false;
    } finally {
      setIsNavigating(false);
    }
  }, [sessionState, location.pathname, navigate]);

  /**
   * Handles exit operations with return path logic
   * Implements F3/PF3 function key equivalent with intelligent return navigation
   * 
   * @param {Object} options - Exit options and fallback paths
   * @returns {Promise<boolean>} Exit operation success status
   */
  const handleExit = useCallback(async (options = {}) => {
    try {
      setIsNavigating(true);

      // Extract return path from current navigation context
      const returnPath = currentContext?.returnPath || options.fallbackPath;
      
      if (returnPath && returnPath !== location.pathname) {
        // Navigate to specific return path
        const navigationState = createNavigationState(
          location.pathname,
          options.contextData,
          {
            transactionId: options.transactionId,
            previousComponent: 'exit_operation',
            userId: sessionState.sessionState?.userId,
            sessionId: sessionState.sessionState?.sessionId
          }
        );

        navigate(returnPath, { state: navigationState });
        
        console.log('Exit navigation to return path:', {
          returnPath,
          currentPath: location.pathname
        });

        return true;
      } else {
        // Fallback to browser history back operation
        if (window.history.length > 1) {
          navigate(-1);
          console.log('Exit navigation using browser history back');
          return true;
        } else {
          // Ultimate fallback to main menu
          return await navigateToMenu({ replace: true });
        }
      }

    } catch (error) {
      console.error('Error handling exit operation:', error);
      // Fallback to menu navigation on error
      return await navigateToMenu({ replace: true });
    } finally {
      setIsNavigating(false);
    }
  }, [currentContext, location.pathname, navigate, navigateToMenu, sessionState]);

  /**
   * Navigates with comprehensive state preservation
   * Provides maximum context preservation for complex navigation scenarios
   * 
   * @param {string} targetPath - Target route path
   * @param {Object} stateData - Complete state data to preserve
   * @param {Object} options - Navigation options
   * @returns {Promise<boolean>} Navigation success status
   */
  const navigateWithState = useCallback(async (targetPath, stateData = {}, options = {}) => {
    try {
      setIsNavigating(true);

      // Validate target path and authorization
      const accessValidation = validateRouteAccess(targetPath, sessionState);
      if (!accessValidation.allowed) {
        console.warn(`Navigation with state blocked: ${accessValidation.reason}`);
        if (accessValidation.redirectTo) {
          navigate(accessValidation.redirectTo, { replace: true });
        }
        return false;
      }

      // Merge current context with new state data
      const enhancedStateData = {
        ...currentContext?.contextData,
        ...stateData,
        preservedAt: new Date().toISOString(),
        sourceComponent: options.sourceComponent || 'navigation_hook'
      };

      // Create comprehensive navigation state
      const navigationState = createNavigationState(
        location.pathname,
        enhancedStateData,
        {
          transactionId: options.transactionId,
          previousComponent: options.previousComponent,
          userId: sessionState.sessionState?.userId,
          sessionId: sessionState.sessionState?.sessionId,
          errorContext: options.errorContext
        }
      );

      // Perform navigation with enhanced state
      navigate(targetPath, { 
        state: navigationState, 
        replace: options.replace || false 
      });

      console.log('Navigation with state completed:', {
        targetPath,
        stateKeys: Object.keys(enhancedStateData),
        transactionId: navigationState.transactionId
      });

      return true;

    } catch (error) {
      console.error('Error navigating with state:', error);
      return false;
    } finally {
      setIsNavigating(false);
    }
  }, [sessionState, currentContext, location.pathname, navigate]);

  /**
   * Navigates back in browser history with intelligent fallback
   * Provides reliable back navigation with menu fallback for edge cases
   * 
   * @param {Object} options - Back navigation options
   * @returns {Promise<boolean>} Back navigation success status
   */
  const goBack = useCallback(async (options = {}) => {
    try {
      setIsNavigating(true);

      // Check if back navigation is possible
      if (window.history.length > 1 && !options.forceMenu) {
        navigate(-1);
        console.log('Back navigation using browser history');
        return true;
      } else {
        // Fallback to menu navigation
        console.log('Back navigation fallback to menu');
        return await navigateToMenu({ replace: options.replace || false });
      }

    } catch (error) {
      console.error('Error during back navigation:', error);
      // Ultimate fallback to menu
      return await navigateToMenu({ replace: true });
    } finally {
      setIsNavigating(false);
    }
  }, [navigate, navigateToMenu]);

  /**
   * Navigates only if user is authorized for the target route
   * Implements comprehensive authorization checking before navigation
   * 
   * @param {string} targetPath - Target route path to check and navigate to
   * @param {Object} contextData - Context data for navigation
   * @param {Object} options - Navigation options
   * @returns {Promise<Object>} Navigation result with authorization details
   */
  const navigateIfAuthorized = useCallback(async (targetPath, contextData = null, options = {}) => {
    try {
      // Check token expiration first
      const token = sessionState.sessionState?.token;
      if (token && isTokenExpired(token)) {
        console.warn('Cannot navigate: JWT token has expired');
        await sessionState.terminate('expired', true);
        return {
          success: false,
          reason: 'TOKEN_EXPIRED',
          message: 'Session has expired, please log in again'
        };
      }

      // Validate route access
      const accessValidation = validateRouteAccess(targetPath, sessionState);
      if (!accessValidation.allowed) {
        return {
          success: false,
          reason: accessValidation.reason,
          message: accessValidation.message,
          redirected: !!accessValidation.redirectTo
        };
      }

      // Perform authorized navigation
      const navigationSuccess = await navigateToScreen(targetPath, contextData, options);
      
      return {
        success: navigationSuccess,
        reason: navigationSuccess ? 'NAVIGATION_SUCCESS' : 'NAVIGATION_FAILED',
        message: navigationSuccess ? 'Navigation completed successfully' : 'Navigation failed due to system error'
      };

    } catch (error) {
      console.error('Error in authorized navigation:', error);
      return {
        success: false,
        reason: 'NAVIGATION_ERROR',
        message: error.message || 'Navigation failed due to unexpected error'
      };
    }
  }, [sessionState, navigateToScreen]);

  /**
   * Checks if back navigation is available
   * Provides navigation state information for UI component control
   * 
   * @returns {boolean} True if back navigation is available
   */
  const canGoBack = useCallback(() => {
    try {
      // Check if return path is available in current context
      const hasReturnPath = !!currentContext?.returnPath;
      
      // Check browser history length
      const hasBrowserHistory = window.history.length > 1;
      
      // Navigation is available if either condition is true
      return hasReturnPath || hasBrowserHistory;

    } catch (error) {
      console.error('Error checking back navigation availability:', error);
      return false;
    }
  }, [currentContext]);

  // Cleanup navigation timers on unmount
  useEffect(() => {
    return () => {
      if (navigationTimer.current) {
        clearTimeout(navigationTimer.current);
      }
      if (abortController.current) {
        abortController.current.abort();
      }
    };
  }, []);

  // Return navigation functions and state
  return {
    // Primary navigation functions
    navigateToMenu,
    navigateToScreen,
    handleExit,
    navigateWithState,
    goBack,
    navigateIfAuthorized,
    canGoBack,

    // Navigation state and context
    currentContext,
    navigationHistory,
    isNavigating,

    // Utility functions for advanced use cases
    validateRouteAccess: (path) => validateRouteAccess(path, sessionState),
    createNavigationState,
    extractNavigationContext: () => extractNavigationContext(location)
  };
}

// ============================================================================
// NAMED EXPORT FUNCTIONS FOR DIRECT USAGE
// ============================================================================

/**
 * Navigate to role-appropriate main menu
 * Standalone function for direct usage without hook instantiation
 */
export const navigateToMenu = (navigate, sessionState, location, options = {}) => {
  const userType = sessionState?.sessionState?.userType;
  const targetPath = userType === 'A' ? NAVIGATION_CONFIG.ROUTES.ADMIN_MENU : NAVIGATION_CONFIG.ROUTES.MENU;
  
  const navigationState = createNavigationState(
    location.pathname,
    options.contextData,
    {
      transactionId: options.transactionId,
      previousComponent: 'standalone_navigation',
      userId: sessionState?.sessionState?.userId,
      sessionId: sessionState?.sessionState?.sessionId
    }
  );

  navigate(targetPath, { state: navigationState, replace: options.replace || false });
};

/**
 * Navigate to specific screen with context
 * Standalone function for direct usage without hook instantiation
 */
export const navigateToScreen = (navigate, location, screenPath, contextData = null, options = {}) => {
  const navigationState = createNavigationState(
    location.pathname,
    contextData,
    {
      transactionId: options.transactionId,
      previousComponent: 'standalone_navigation',
      userId: options.userId,
      sessionId: options.sessionId,
      errorContext: options.errorContext
    }
  );

  navigate(screenPath, { 
    state: navigationState, 
    replace: options.replace || false 
  });
};

/**
 * Handle exit operation with return logic
 * Standalone function for direct usage without hook instantiation
 */
export const handleExit = (navigate, location, options = {}) => {
  const context = extractNavigationContext(location);
  const returnPath = context?.returnPath || options.fallbackPath;
  
  if (returnPath && returnPath !== location.pathname) {
    const navigationState = createNavigationState(
      location.pathname,
      options.contextData,
      {
        transactionId: options.transactionId,
        previousComponent: 'standalone_exit',
        userId: options.userId,
        sessionId: options.sessionId
      }
    );

    navigate(returnPath, { state: navigationState });
  } else {
    navigate(-1);
  }
};

/**
 * Navigate with comprehensive state preservation
 * Standalone function for direct usage without hook instantiation
 */
export const navigateWithState = (navigate, location, targetPath, stateData = {}, options = {}) => {
  const enhancedStateData = {
    ...stateData,
    preservedAt: new Date().toISOString(),
    sourceComponent: options.sourceComponent || 'standalone_navigation'
  };

  const navigationState = createNavigationState(
    location.pathname,
    enhancedStateData,
    {
      transactionId: options.transactionId,
      previousComponent: options.previousComponent,
      userId: options.userId,
      sessionId: options.sessionId,
      errorContext: options.errorContext
    }
  );

  navigate(targetPath, { 
    state: navigationState, 
    replace: options.replace || false 
  });
};

/**
 * Navigate back with fallback logic
 * Standalone function for direct usage without hook instantiation
 */
export const goBack = (navigate, options = {}) => {
  if (window.history.length > 1 && !options.forceMenu) {
    navigate(-1);
  } else {
    navigate(NAVIGATION_CONFIG.ROUTES.MENU, { replace: options.replace || false });
  }
};

/**
 * Navigate only if authorized
 * Standalone function for direct usage without hook instantiation
 */
export const navigateIfAuthorized = (navigate, location, sessionState, targetPath, contextData = null, options = {}) => {
  const accessValidation = validateRouteAccess(targetPath, sessionState);
  
  if (!accessValidation.allowed) {
    console.warn(`Navigation blocked: ${accessValidation.reason}`);
    if (accessValidation.redirectTo) {
      navigate(accessValidation.redirectTo, { replace: true });
    }
    return false;
  }

  navigateToScreen(navigate, location, targetPath, contextData, options);
  return true;
};

/**
 * Check if back navigation is available
 * Standalone function for direct usage without hook instantiation
 */
export const canGoBack = (location) => {
  const context = extractNavigationContext(location);
  const hasReturnPath = !!context?.returnPath;
  const hasBrowserHistory = window.history.length > 1;
  
  return hasReturnPath || hasBrowserHistory;
};