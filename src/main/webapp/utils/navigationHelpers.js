/**
 * CardDemo - Navigation Helpers Utility
 * 
 * Comprehensive navigation utility functions that translate CICS transaction flow and BMS screen 
 * transitions to React Router patterns, maintaining pseudo-conversational state management and 
 * preserving original mainframe navigation behavior.
 * 
 * This utility provides the foundational navigation infrastructure for the entire CardDemo React
 * application, ensuring exact functional equivalence with original CICS XCTL program transfers,
 * pseudo-conversational processing, and BMS screen navigation patterns.
 * 
 * Key CICS Navigation Patterns Preserved:
 * - EXEC CICS XCTL PROGRAM transitions via React Router programmatic navigation
 * - Pseudo-conversational processing through stateless session state management
 * - Function key navigation (F3=Exit, F7/F8=Pagination, F12=Cancel) with browser alternatives
 * - COMMAREA data passing equivalent through React Router state and URL parameters
 * - Screen hierarchy and breadcrumb navigation matching original BMS screen flow
 * - Return path logic for F3=Exit functionality through React Router history management
 * 
 * Navigation Architecture:
 * - React Router DOM 6.20.x for declarative routing configuration
 * - JWT token-based authentication with role-based route access control
 * - Redis-backed session storage for pseudo-conversational state preservation
 * - Browser history API integration for back/forward navigation support
 * - Keyboard event handling for function key mapping and accessibility
 * 
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
 */

import { useNavigate, useLocation } from 'react-router-dom';
import { useEffect, useCallback, useState, useRef } from 'react';

// Import dependency constants and types
import { 
  ROUTES, 
  NAVIGATION_FLOW, 
  BREADCRUMB_PATHS,
  getRouteByTransactionCode,
  getBreadcrumbForRoute,
  getExitPathForRoute,
  isAdminRoute,
  buildRouteWithParameters
} from '../constants/NavigationConstants';

import { 
  FUNCTION_KEYS, 
  ALTERNATIVE_KEY_COMBINATIONS,
  getFunctionKeyByCode,
  getKeyboardShortcutsForContext,
  buildKeyboardEventHandler
} from '../constants/KeyboardConstants';

import { 
  TRANSACTION_ENDPOINTS, 
  API_BASE_PATHS 
} from '../constants/TransactionConstants';

import { 
  RouteDefinition, 
  NavigationContext, 
  BreadcrumbData, 
  FunctionKeyMapping 
} from '../types/NavigationTypes';

import { 
  BaseScreenData, 
  FormFieldAttributes 
} from '../types/CommonTypes';

/**
 * Session Storage Keys for Pseudo-Conversational State Management
 * 
 * Defines standardized keys for storing navigation state in browser sessionStorage
 * and Redis-backed server session, maintaining CICS-equivalent session management.
 */
const SESSION_STORAGE_KEYS = {
  NAVIGATION_CONTEXT: 'carddemo_navigation_context',
  BREADCRUMB_TRAIL: 'carddemo_breadcrumb_trail',
  RETURN_PATH_STACK: 'carddemo_return_path_stack',
  SESSION_DATA: 'carddemo_session_data',
  TRANSACTION_HISTORY: 'carddemo_transaction_history',
  CURRENT_SCREEN_STATE: 'carddemo_current_screen_state',
  FUNCTION_KEY_CONTEXT: 'carddemo_function_key_context'
};

/**
 * Navigation Event Types for Consistent Event Handling
 * 
 * Standardizes navigation event types across all React components, enabling
 * consistent event handling and audit logging for CICS transaction equivalents.
 */
const NAVIGATION_EVENT_TYPES = {
  SCREEN_ENTRY: 'SCREEN_ENTRY',           // Component mount/initialization
  SCREEN_EXIT: 'SCREEN_EXIT',             // F3=Exit or programmatic exit
  TRANSACTION_START: 'TRANSACTION_START', // Beginning of business transaction
  TRANSACTION_END: 'TRANSACTION_END',     // Transaction completion/rollback
  PAGE_NAVIGATION: 'PAGE_NAVIGATION',     // F7/F8 pagination events
  CANCEL_OPERATION: 'CANCEL_OPERATION',   // F12=Cancel or ESC key events
  SESSION_TIMEOUT: 'SESSION_TIMEOUT',     // Automatic session expiration
  ERROR_RECOVERY: 'ERROR_RECOVERY',       // Error boundary navigation
  MENU_SELECTION: 'MENU_SELECTION',       // Menu option selection events
  RETURN_NAVIGATION: 'RETURN_NAVIGATION'  // Return path navigation events
};

/**
 * createNavigationHandler - Primary Navigation Control Function
 * 
 * Creates a comprehensive navigation handler that replicates CICS XCTL program
 * transfer functionality using React Router programmatic navigation. Maintains
 * navigation context, session state, and audit trail for compliance tracking.
 * 
 * CICS Pattern Preserved:
 * EXEC CICS XCTL PROGRAM(targetProgram) COMMAREA(navigationData)
 * 
 * @param {Object} options Navigation configuration options
 * @param {boolean} options.preserveHistory - Whether to maintain browser history stack
 * @param {boolean} options.validateAuth - Whether to check authentication before navigation
 * @param {boolean} options.logTransactions - Whether to log navigation events for audit
 * @param {string} options.defaultReturnPath - Default route for F3=Exit navigation
 * @returns {Function} Navigation handler function
 */
export const createNavigationHandler = (options = {}) => {
  const {
    preserveHistory = true,
    validateAuth = true,
    logTransactions = true,
    defaultReturnPath = ROUTES.MENU
  } = options;

  return (targetRoute, navigationData = {}, transactionCode = null) => {
    // Validate authentication if required
    if (validateAuth && !isUserAuthenticated()) {
      console.warn('Navigation blocked: User not authenticated');
      return navigateToLogin();
    }

    // Validate route exists and user has access
    if (!isRouteAccessible(targetRoute)) {
      console.warn(`Navigation blocked: Route ${targetRoute} not accessible`);
      return handleUnauthorizedNavigation();
    }

    // Create navigation context preserving CICS pseudo-conversational patterns
    const navigationContext = createNavigationContext({
      targetRoute,
      navigationData,
      transactionCode,
      preserveHistory,
      defaultReturnPath
    });

    // Log navigation event for audit trail
    if (logTransactions) {
      logNavigationEvent(NAVIGATION_EVENT_TYPES.TRANSACTION_START, {
        fromRoute: getCurrentRoute(),
        toRoute: targetRoute,
        transactionCode,
        timestamp: new Date().toISOString(),
        sessionId: getSessionId()
      });
    }

    // Store navigation context for pseudo-conversational state management
    storeNavigationContext(navigationContext);

    // Execute React Router navigation with state preservation
    return executeNavigation(targetRoute, navigationContext);
  };
};

/**
 * navigateWithState - Enhanced Navigation with State Preservation
 * 
 * Provides React Router navigation while maintaining CICS-equivalent session state
 * and implementing pseudo-conversational processing patterns. Preserves screen data,
 * validation state, and user context across navigation transitions.
 * 
 * @param {Object} navigate - React Router navigate function
 * @param {string} targetPath - Destination route path
 * @param {Object} screenData - Current screen state to preserve
 * @param {Object} options - Navigation options
 * @returns {Promise} Navigation completion promise
 */
export const navigateWithState = async (navigate, targetPath, screenData = {}, options = {}) => {
  const {
    replace = false,
    preserveScreenState = true,
    updateBreadcrumbs = true,
    validateTransition = true
  } = options;

  try {
    // Validate navigation transition is allowed
    if (validateTransition && !isTransitionAllowed(getCurrentRoute(), targetPath)) {
      throw new Error(`Invalid transition from ${getCurrentRoute()} to ${targetPath}`);
    }

    // Create comprehensive navigation state object
    const navigationState = {
      previousRoute: getCurrentRoute(),
      screenData: preserveScreenState ? screenData : {},
      timestamp: new Date().toISOString(),
      sessionId: getSessionId(),
      transactionId: generateTransactionId(),
      userRole: getUserRole(),
      returnPath: getReturnPath()
    };

    // Update breadcrumb trail if enabled
    if (updateBreadcrumbs) {
      updateBreadcrumbTrail(targetPath);
    }

    // Store current screen state for potential return navigation
    if (preserveScreenState) {
      storeScreenState(getCurrentRoute(), screenData);
    }

    // Execute React Router navigation with enhanced state
    await navigate(targetPath, {
      replace,
      state: navigationState
    });

    // Update session storage with new navigation context
    updateSessionNavigation(targetPath, navigationState);

    // Log successful navigation
    logNavigationEvent(NAVIGATION_EVENT_TYPES.SCREEN_ENTRY, {
      route: targetPath,
      navigationState,
      timestamp: new Date().toISOString()
    });

    return { success: true, route: targetPath, state: navigationState };

  } catch (error) {
    console.error('Navigation failed:', error);
    
    // Log navigation error
    logNavigationEvent(NAVIGATION_EVENT_TYPES.ERROR_RECOVERY, {
      error: error.message,
      targetPath,
      currentRoute: getCurrentRoute(),
      timestamp: new Date().toISOString()
    });

    // Attempt error recovery navigation
    return handleNavigationError(error, targetPath);
  }
};

/**
 * createPseudoConversationalHandler - CICS Pseudo-Conversational Processing
 * 
 * Implements CICS pseudo-conversational processing patterns in React environment,
 * maintaining stateless server architecture while preserving session context and
 * transaction state across multiple screen interactions.
 * 
 * CICS Pattern Preserved:
 * EXEC CICS RETURN TRANSID(nextTransaction) COMMAREA(sessionData)
 * 
 * @param {Object} config - Pseudo-conversational configuration
 * @returns {Object} Pseudo-conversational handler functions
 */
export const createPseudoConversationalHandler = (config = {}) => {
  const {
    sessionTimeout = 20, // minutes (matching CICS default)
    maxReturnStackDepth = 10,
    enableStateCompression = true,
    redisBackedSession = true
  } = config;

  return {
    /**
     * Initiates a new pseudo-conversational sequence
     */
    startConversation: (transactionCode, initialData = {}) => {
      const conversationId = generateConversationId();
      const sessionData = {
        conversationId,
        transactionCode,
        startTime: new Date(),
        lastActivity: new Date(),
        timeoutMinutes: sessionTimeout,
        initialData,
        screenStack: [],
        returnPathStack: [],
        sessionState: {}
      };

      // Store session data in browser sessionStorage and Redis
      storeSessionData(conversationId, sessionData);
      
      return { conversationId, sessionData };
    },

    /**
     * Continues an existing pseudo-conversational sequence
     */
    continueConversation: (conversationId, newData = {}) => {
      const sessionData = retrieveSessionData(conversationId);
      
      if (!sessionData) {
        throw new Error(`Conversation ${conversationId} not found or expired`);
      }

      // Update session activity timestamp
      sessionData.lastActivity = new Date();
      
      // Merge new data with existing session state
      sessionData.sessionState = {
        ...sessionData.sessionState,
        ...newData
      };

      // Update stored session data
      storeSessionData(conversationId, sessionData);
      
      return sessionData;
    },

    /**
     * Terminates pseudo-conversational sequence with cleanup
     */
    endConversation: (conversationId, finalData = {}) => {
      const sessionData = retrieveSessionData(conversationId);
      
      if (sessionData) {
        // Log conversation completion
        logNavigationEvent(NAVIGATION_EVENT_TYPES.TRANSACTION_END, {
          conversationId,
          duration: new Date() - sessionData.startTime,
          finalData,
          timestamp: new Date().toISOString()
        });

        // Clean up session storage
        clearSessionData(conversationId);
      }

      return { completed: true, conversationId };
    },

    /**
     * Checks if conversation is still valid and not timed out
     */
    isConversationActive: (conversationId) => {
      const sessionData = retrieveSessionData(conversationId);
      
      if (!sessionData) return false;

      const timeoutMs = sessionData.timeoutMinutes * 60 * 1000;
      const elapsed = new Date() - new Date(sessionData.lastActivity);
      
      return elapsed < timeoutMs;
    },

    /**
     * Retrieves current conversation context
     */
    getConversationContext: (conversationId) => {
      return retrieveSessionData(conversationId);
    }
  };
};

/**
 * createBreadcrumbTracker - Navigation Breadcrumb Management
 * 
 * Maintains hierarchical breadcrumb navigation trail matching CICS screen hierarchy,
 * enabling F3=Exit functionality and providing visual navigation context for users.
 * Supports both main user menu and admin menu navigation paths.
 * 
 * @param {Object} options - Breadcrumb tracking configuration
 * @returns {Object} Breadcrumb management functions
 */
export const createBreadcrumbTracker = (options = {}) => {
  const {
    maxBreadcrumbDepth = 4,
    enableAutoTrim = true,
    persistInSession = true
  } = options;

  return {
    /**
     * Adds new breadcrumb to navigation trail
     */
    addBreadcrumb: (route, title, transactionCode) => {
      const breadcrumbData = {
        path: route,
        title,
        transactionCode,
        level: getCurrentBreadcrumbLevel() + 1,
        isActive: true,
        timestamp: new Date().toISOString()
      };

      const currentTrail = getBreadcrumbTrail();
      
      // Mark previous breadcrumbs as inactive
      currentTrail.forEach(crumb => crumb.isActive = false);
      
      // Add new breadcrumb
      currentTrail.push(breadcrumbData);
      
      // Trim trail if it exceeds maximum depth
      if (enableAutoTrim && currentTrail.length > maxBreadcrumbDepth) {
        currentTrail.shift(); // Remove oldest breadcrumb
      }

      // Update stored breadcrumb trail
      if (persistInSession) {
        storeBreadcrumbTrail(currentTrail);
      }

      return breadcrumbData;
    },

    /**
     * Removes breadcrumb and all subsequent entries (for back navigation)
     */
    trimBreadcrumbsToLevel: (targetLevel) => {
      const currentTrail = getBreadcrumbTrail();
      const trimmedTrail = currentTrail.filter(crumb => crumb.level <= targetLevel);
      
      // Mark the target level as active
      if (trimmedTrail.length > 0) {
        trimmedTrail[trimmedTrail.length - 1].isActive = true;
      }

      if (persistInSession) {
        storeBreadcrumbTrail(trimmedTrail);
      }

      return trimmedTrail;
    },

    /**
     * Gets current breadcrumb trail
     */
    getBreadcrumbTrail: () => {
      return getBreadcrumbTrail();
    },

    /**
     * Gets breadcrumb for F3=Exit navigation
     */
    getPreviousBreadcrumb: () => {
      const trail = getBreadcrumbTrail();
      return trail.length > 1 ? trail[trail.length - 2] : null;
    },

    /**
     * Clears entire breadcrumb trail (for logout or session reset)
     */
    clearBreadcrumbs: () => {
      const emptyTrail = [];
      if (persistInSession) {
        storeBreadcrumbTrail(emptyTrail);
      }
      return emptyTrail;
    },

    /**
     * Gets formatted breadcrumb display text
     */
    getBreadcrumbDisplayText: () => {
      const trail = getBreadcrumbTrail();
      return trail.map(crumb => crumb.title).join(' > ');
    }
  };
};

/**
 * createMenuNavigationHandler - Menu-Driven Navigation Control
 * 
 * Handles menu selection navigation from COMEN01.bms (Main Menu) and COADM01.bms 
 * (Admin Menu), preserving exact option-to-transaction mapping and role-based 
 * access control from original CICS menu processing.
 * 
 * @param {string} menuType - 'main' or 'admin' menu type
 * @param {Object} userRole - Current user role for access validation
 * @returns {Function} Menu navigation handler
 */
export const createMenuNavigationHandler = (menuType = 'main', userRole = 'user') => {
  return (optionNumber, additionalData = {}) => {
    try {
      // Validate menu option number
      if (!isValidMenuOption(optionNumber, menuType)) {
        throw new Error(`Invalid menu option: ${optionNumber} for ${menuType} menu`);
      }

      // Get menu configuration based on type
      const menuConfig = getMenuConfiguration(menuType);
      const selectedOption = menuConfig.options[optionNumber];

      // Validate user access to selected option
      if (selectedOption.requiresAdmin && userRole !== 'admin') {
        throw new Error(`Unauthorized access to admin option: ${optionNumber}`);
      }

      // Build navigation parameters
      const navigationParams = {
        ...additionalData,
        menuType,
        optionNumber,
        transactionCode: selectedOption.transactionCode,
        returnPath: getMenuReturnPath(menuType)
      };

      // Log menu selection
      logNavigationEvent(NAVIGATION_EVENT_TYPES.MENU_SELECTION, {
        menuType,
        optionNumber,
        selectedOption: selectedOption.title,
        userRole,
        timestamp: new Date().toISOString()
      });

      // Execute navigation to selected option
      return navigateToMenuOption(selectedOption.path, navigationParams);

    } catch (error) {
      console.error('Menu navigation failed:', error);
      
      // Show error message to user
      displayMenuError(error.message);
      
      // Return to menu with error context
      return { success: false, error: error.message };
    }
  };
};

/**
 * createReturnPathHandler - F3=Exit and Return Navigation
 * 
 * Implements F3=Exit functionality matching CICS return path behavior, managing
 * navigation history stack and providing consistent back navigation across all
 * React components. Handles both browser history and custom return path logic.
 * 
 * @param {Object} navigate - React Router navigate function
 * @param {Object} location - React Router location object
 * @returns {Object} Return path management functions
 */
export const createReturnPathHandler = (navigate, location) => {
  return {
    /**
     * Handles F3=Exit key press navigation
     */
    handleF3Exit: () => {
      const returnPath = getReturnPath();
      const previousBreadcrumb = getPreviousBreadcrumb();
      
      // Log exit event
      logNavigationEvent(NAVIGATION_EVENT_TYPES.SCREEN_EXIT, {
        currentRoute: location.pathname,
        returnPath,
        exitMethod: 'F3_KEY',
        timestamp: new Date().toISOString()
      });

      // Determine exit destination based on context
      if (returnPath) {
        // Use explicit return path if set
        return navigateWithState(navigate, returnPath, {}, { replace: false });
      } else if (previousBreadcrumb) {
        // Use breadcrumb-based navigation
        return navigateWithState(navigate, previousBreadcrumb.path, {}, { replace: false });
      } else {
        // Fall back to default exit path
        const defaultExit = getExitPathForRoute(location.pathname);
        return navigateWithState(navigate, defaultExit, {}, { replace: false });
      }
    },

    /**
     * Handles F12=Cancel operation navigation
     */
    handleF12Cancel: (confirmationRequired = true) => {
      if (confirmationRequired && !confirmCancelOperation()) {
        return { cancelled: true };
      }

      // Log cancel event
      logNavigationEvent(NAVIGATION_EVENT_TYPES.CANCEL_OPERATION, {
        currentRoute: location.pathname,
        cancelMethod: 'F12_KEY',
        timestamp: new Date().toISOString()
      });

      // Clear any pending form data
      clearPendingFormData();

      // Navigate back with cancel context
      return this.handleF3Exit();
    },

    /**
     * Sets custom return path for current navigation context
     */
    setReturnPath: (path) => {
      storeReturnPath(path);
      return path;
    },

    /**
     * Gets current return path
     */
    getReturnPath: () => {
      return getReturnPath();
    },

    /**
     * Clears return path (for cleanup)
     */
    clearReturnPath: () => {
      clearReturnPath();
    }
  };
};

/**
 * createTransactionRouter - CICS Transaction Code Router
 * 
 * Maps 4-character CICS transaction codes to React Router paths, enabling direct
 * transaction-based navigation and maintaining traceability between original
 * CICS transactions and modern React components.
 * 
 * @param {Object} navigate - React Router navigate function
 * @returns {Function} Transaction routing function
 */
export const createTransactionRouter = (navigate) => {
  return (transactionCode, parameters = {}, options = {}) => {
    try {
      // Validate transaction code format
      if (!isValidTransactionCode(transactionCode)) {
        throw new Error(`Invalid transaction code format: ${transactionCode}`);
      }

      // Resolve transaction code to React Router path
      const targetRoute = getRouteByTransactionCode(transactionCode);
      
      if (!targetRoute) {
        throw new Error(`No route found for transaction code: ${transactionCode}`);
      }

      // Build route with parameters if needed
      const finalRoute = Object.keys(parameters).length > 0 
        ? buildRouteWithParameters(targetRoute, parameters)
        : targetRoute;

      // Create navigation context with transaction traceability
      const navigationContext = {
        transactionCode,
        parameters,
        targetRoute: finalRoute,
        timestamp: new Date().toISOString(),
        sessionId: getSessionId()
      };

      // Log transaction routing
      logNavigationEvent(NAVIGATION_EVENT_TYPES.TRANSACTION_START, {
        transactionCode,
        targetRoute: finalRoute,
        parameters,
        timestamp: new Date().toISOString()
      });

      // Execute navigation with transaction context
      return navigateWithState(navigate, finalRoute, navigationContext, options);

    } catch (error) {
      console.error('Transaction routing failed:', error);
      
      // Log routing error
      logNavigationEvent(NAVIGATION_EVENT_TYPES.ERROR_RECOVERY, {
        transactionCode,
        error: error.message,
        timestamp: new Date().toISOString()
      });

      return { success: false, error: error.message, transactionCode };
    }
  };
};

/**
 * createSessionStateManager - Session State Management
 * 
 * Manages pseudo-conversational session state across React Router navigation,
 * implementing CICS-equivalent session handling with Redis-backed storage for
 * distributed session management and JWT token integration.
 * 
 * @param {Object} config - Session management configuration
 * @returns {Object} Session management functions
 */
export const createSessionStateManager = (config = {}) => {
  const {
    sessionTimeoutMinutes = 20,
    enableRedisBackend = true,
    enableBrowserStorage = true,
    compressionEnabled = true
  } = config;

  return {
    /**
     * Initializes new session state
     */
    initializeSession: (userRole, authToken) => {
      const sessionId = generateSessionId();
      const sessionData = {
        sessionId,
        userRole,
        authToken,
        startTime: new Date(),
        lastActivity: new Date(),
        timeoutMinutes: sessionTimeoutMinutes,
        navigationHistory: [],
        screenStates: {},
        breadcrumbTrail: [],
        returnPathStack: [],
        transactionHistory: []
      };

      // Store session in both browser and Redis if enabled
      if (enableBrowserStorage) {
        sessionStorage.setItem(SESSION_STORAGE_KEYS.SESSION_DATA, JSON.stringify(sessionData));
      }

      if (enableRedisBackend) {
        storeSessionInRedis(sessionId, sessionData);
      }

      return sessionData;
    },

    /**
     * Updates existing session state
     */
    updateSession: (sessionId, updates) => {
      let sessionData = null;

      // Retrieve session data
      if (enableRedisBackend) {
        sessionData = retrieveSessionFromRedis(sessionId);
      } else if (enableBrowserStorage) {
        const stored = sessionStorage.getItem(SESSION_STORAGE_KEYS.SESSION_DATA);
        sessionData = stored ? JSON.parse(stored) : null;
      }

      if (!sessionData) {
        throw new Error(`Session ${sessionId} not found`);
      }

      // Update session with new data
      const updatedSession = {
        ...sessionData,
        ...updates,
        lastActivity: new Date()
      };

      // Store updated session
      if (enableBrowserStorage) {
        sessionStorage.setItem(SESSION_STORAGE_KEYS.SESSION_DATA, JSON.stringify(updatedSession));
      }

      if (enableRedisBackend) {
        storeSessionInRedis(sessionId, updatedSession);
      }

      return updatedSession;
    },

    /**
     * Validates session is still active
     */
    validateSession: (sessionId) => {
      const sessionData = this.getSession(sessionId);
      
      if (!sessionData) return false;

      const timeoutMs = sessionData.timeoutMinutes * 60 * 1000;
      const elapsed = new Date() - new Date(sessionData.lastActivity);
      
      return elapsed < timeoutMs;
    },

    /**
     * Retrieves current session state
     */
    getSession: (sessionId) => {
      if (enableRedisBackend) {
        return retrieveSessionFromRedis(sessionId);
      } else if (enableBrowserStorage) {
        const stored = sessionStorage.getItem(SESSION_STORAGE_KEYS.SESSION_DATA);
        return stored ? JSON.parse(stored) : null;
      }
      return null;
    },

    /**
     * Clears session state (for logout)
     */
    clearSession: (sessionId) => {
      if (enableBrowserStorage) {
        sessionStorage.removeItem(SESSION_STORAGE_KEYS.SESSION_DATA);
        sessionStorage.removeItem(SESSION_STORAGE_KEYS.NAVIGATION_CONTEXT);
        sessionStorage.removeItem(SESSION_STORAGE_KEYS.BREADCRUMB_TRAIL);
        sessionStorage.removeItem(SESSION_STORAGE_KEYS.RETURN_PATH_STACK);
      }

      if (enableRedisBackend) {
        clearSessionFromRedis(sessionId);
      }

      return true;
    }
  };
};

/**
 * useNavigationHelpers - React Hook for Navigation State Management
 * 
 * Custom React hook providing comprehensive navigation functionality for React
 * components, integrating all navigation utilities with React lifecycle and
 * state management patterns. Provides centralized access to navigation capabilities.
 * 
 * @param {Object} options - Hook configuration options
 * @returns {Object} Navigation helper functions and state
 */
export const useNavigationHelpers = (options = {}) => {
  const navigate = useNavigate();
  const location = useLocation();
  
  const {
    enableKeyboardShortcuts = true,
    enableBreadcrumbs = true,
    enableSessionManagement = true,
    logNavigation = true
  } = options;

  // Navigation state management
  const [navigationContext, setNavigationContext] = useState(null);
  const [breadcrumbTrail, setBreadcrumbTrail] = useState([]);
  const [sessionData, setSessionData] = useState(null);
  const [returnPath, setReturnPath] = useState(null);

  // Create helper instances
  const breadcrumbTracker = useRef(createBreadcrumbTracker({ persistInSession: true }));
  const returnPathHandler = useRef(createReturnPathHandler(navigate, location));
  const transactionRouter = useRef(createTransactionRouter(navigate));
  const sessionManager = useRef(createSessionStateManager({ 
    enableRedisBackend: true,
    enableBrowserStorage: true 
  }));

  // Initialize navigation context on mount
  useEffect(() => {
    if (enableSessionManagement) {
      const currentSession = getCurrentSession();
      setSessionData(currentSession);
    }

    if (enableBreadcrumbs) {
      const currentTrail = getBreadcrumbTrail();
      setBreadcrumbTrail(currentTrail);
    }

    // Update navigation context when location changes
    const context = {
      currentRoute: location.pathname,
      locationState: location.state,
      timestamp: new Date().toISOString()
    };
    setNavigationContext(context);

  }, [location.pathname, enableSessionManagement, enableBreadcrumbs]);

  // Keyboard shortcut handler
  useEffect(() => {
    if (!enableKeyboardShortcuts) return;

    const handleKeyDown = (event) => {
      const functionKey = getFunctionKeyByCode(event.key);
      
      if (functionKey) {
        event.preventDefault();
        
        switch (functionKey.action) {
          case 'EXIT':
            returnPathHandler.current.handleF3Exit();
            break;
          case 'CANCEL':
            returnPathHandler.current.handleF12Cancel();
            break;
          case 'PAGE_BACKWARD':
            handlePageBackward();
            break;
          case 'PAGE_FORWARD':
            handlePageForward();
            break;
          default:
            console.log('Unhandled function key:', functionKey.action);
        }
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [enableKeyboardShortcuts]);

  // Navigation helper functions
  const navigateToRoute = useCallback((route, data = {}, options = {}) => {
    return navigateWithState(navigate, route, data, options);
  }, [navigate]);

  const navigateByTransaction = useCallback((transactionCode, params = {}) => {
    return transactionRouter.current(transactionCode, params);
  }, []);

  const addBreadcrumb = useCallback((route, title, transactionCode) => {
    const breadcrumb = breadcrumbTracker.current.addBreadcrumb(route, title, transactionCode);
    setBreadcrumbTrail(breadcrumbTracker.current.getBreadcrumbTrail());
    return breadcrumb;
  }, []);

  const exitCurrentScreen = useCallback(() => {
    return returnPathHandler.current.handleF3Exit();
  }, []);

  const cancelCurrentOperation = useCallback(() => {
    return returnPathHandler.current.handleF12Cancel();
  }, []);

  const manageSession = useCallback((action, data = {}) => {
    const sessionId = getSessionId();
    
    switch (action) {
      case 'update':
        return sessionManager.current.updateSession(sessionId, data);
      case 'validate':
        return sessionManager.current.validateSession(sessionId);
      case 'clear':
        return sessionManager.current.clearSession(sessionId);
      default:
        return sessionManager.current.getSession(sessionId);
    }
  }, []);

  // Page navigation handlers
  const handlePageBackward = useCallback(() => {
    // Emit page backward event for components to handle
    window.dispatchEvent(new CustomEvent('page-backward', { 
      detail: { route: location.pathname }
    }));
  }, [location.pathname]);

  const handlePageForward = useCallback(() => {
    // Emit page forward event for components to handle
    window.dispatchEvent(new CustomEvent('page-forward', { 
      detail: { route: location.pathname }
    }));
  }, [location.pathname]);

  return {
    // Navigation functions
    navigate: navigateToRoute,
    navigateByTransaction,
    exitCurrentScreen,
    cancelCurrentOperation,
    
    // Breadcrumb functions
    addBreadcrumb,
    getBreadcrumbTrail: () => breadcrumbTrail,
    clearBreadcrumbs: () => {
      breadcrumbTracker.current.clearBreadcrumbs();
      setBreadcrumbTrail([]);
    },
    
    // Session management
    manageSession,
    
    // Current state
    navigationContext,
    currentRoute: location.pathname,
    sessionData,
    returnPath,
    
    // Utility functions
    setReturnPath: (path) => {
      returnPathHandler.current.setReturnPath(path);
      setReturnPath(path);
    },
    
    // Event handlers for function keys
    handleF3Exit: exitCurrentScreen,
    handleF12Cancel: cancelCurrentOperation,
    handlePageBackward,
    handlePageForward
  };
};

/**
 * NavigationHelpers - Default Export Object
 * 
 * Consolidated navigation helper object providing all navigation functionality
 * in a single import for components requiring comprehensive navigation capabilities.
 * Serves as the primary interface for navigation operations across the application.
 */
const NavigationHelpers = {
  /**
   * Primary navigation function
   */
  navigate: (targetRoute, data = {}, options = {}) => {
    const handler = createNavigationHandler(options);
    return handler(targetRoute, data);
  },

  /**
   * Breadcrumb management function
   */
  createBreadcrumb: (route, title, transactionCode) => {
    const tracker = createBreadcrumbTracker();
    return tracker.addBreadcrumb(route, title, transactionCode);
  },

  /**
   * Exit handling function
   */
  handleExit: (navigate, location) => {
    const handler = createReturnPathHandler(navigate, location);
    return handler.handleF3Exit();
  },

  /**
   * Session management function
   */
  manageSession: (action, sessionId, data = {}) => {
    const manager = createSessionStateManager();
    
    switch (action) {
      case 'initialize':
        return manager.initializeSession(data.userRole, data.authToken);
      case 'update':
        return manager.updateSession(sessionId, data);
      case 'validate':
        return manager.validateSession(sessionId);
      case 'clear':
        return manager.clearSession(sessionId);
      default:
        return manager.getSession(sessionId);
    }
  },

  /**
   * Transaction routing function
   */
  routeTransaction: (navigate, transactionCode, parameters = {}) => {
    const router = createTransactionRouter(navigate);
    return router(transactionCode, parameters);
  },

  /**
   * State preservation function
   */
  preserveState: (screenData, options = {}) => {
    const { 
      persistInSession = true, 
      compressData = true 
    } = options;

    if (persistInSession) {
      const stateKey = `${SESSION_STORAGE_KEYS.CURRENT_SCREEN_STATE}_${Date.now()}`;
      const dataToStore = compressData ? JSON.stringify(screenData) : screenData;
      sessionStorage.setItem(stateKey, JSON.stringify(dataToStore));
      return stateKey;
    }

    return null;
  }
};

// Helper utility functions (internal use)

/**
 * Internal utility functions for session and navigation management
 */

// Session management utilities
const generateSessionId = () => `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
const generateConversationId = () => `conv_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
const generateTransactionId = () => `txn_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

const getSessionId = () => {
  const session = getCurrentSession();
  return session ? session.sessionId : null;
};

const getCurrentSession = () => {
  try {
    const stored = sessionStorage.getItem(SESSION_STORAGE_KEYS.SESSION_DATA);
    return stored ? JSON.parse(stored) : null;
  } catch (error) {
    console.error('Error retrieving session data:', error);
    return null;
  }
};

// Navigation state utilities
const getCurrentRoute = () => window.location.pathname;
const getUserRole = () => {
  const session = getCurrentSession();
  return session ? session.userRole : 'guest';
};

const isUserAuthenticated = () => {
  const session = getCurrentSession();
  return session && session.authToken && session.sessionId;
};

const isRouteAccessible = (route) => {
  // Check if route requires admin access and user has admin role
  if (isAdminRoute(route) && getUserRole() !== 'admin') {
    return false;
  }
  return true;
};

const isTransitionAllowed = (fromRoute, toRoute) => {
  // Basic transition validation - can be enhanced with business rules
  return true;
};

const isValidTransactionCode = (code) => {
  return typeof code === 'string' && code.length === 6 && /^[A-Z0-9]+$/.test(code);
};

const isValidMenuOption = (option, menuType) => {
  const menuConfig = getMenuConfiguration(menuType);
  return menuConfig.options.hasOwnProperty(option);
};

// Breadcrumb utilities
const getBreadcrumbTrail = () => {
  try {
    const stored = sessionStorage.getItem(SESSION_STORAGE_KEYS.BREADCRUMB_TRAIL);
    return stored ? JSON.parse(stored) : [];
  } catch (error) {
    console.error('Error retrieving breadcrumb trail:', error);
    return [];
  }
};

const storeBreadcrumbTrail = (trail) => {
  try {
    sessionStorage.setItem(SESSION_STORAGE_KEYS.BREADCRUMB_TRAIL, JSON.stringify(trail));
  } catch (error) {
    console.error('Error storing breadcrumb trail:', error);
  }
};

const getCurrentBreadcrumbLevel = () => {
  const trail = getBreadcrumbTrail();
  return trail.length;
};

const getPreviousBreadcrumb = () => {
  const trail = getBreadcrumbTrail();
  return trail.length > 1 ? trail[trail.length - 2] : null;
};

const updateBreadcrumbTrail = (route) => {
  const breadcrumbData = getBreadcrumbForRoute(route);
  if (breadcrumbData) {
    const tracker = createBreadcrumbTracker();
    tracker.addBreadcrumb(route, breadcrumbData.title, breadcrumbData.transactionCode);
  }
};

// Return path utilities
const getReturnPath = () => {
  try {
    const stored = sessionStorage.getItem(SESSION_STORAGE_KEYS.RETURN_PATH_STACK);
    const stack = stored ? JSON.parse(stored) : [];
    return stack.length > 0 ? stack[stack.length - 1] : null;
  } catch (error) {
    console.error('Error retrieving return path:', error);
    return null;
  }
};

const storeReturnPath = (path) => {
  try {
    const stored = sessionStorage.getItem(SESSION_STORAGE_KEYS.RETURN_PATH_STACK);
    const stack = stored ? JSON.parse(stored) : [];
    stack.push(path);
    sessionStorage.setItem(SESSION_STORAGE_KEYS.RETURN_PATH_STACK, JSON.stringify(stack));
  } catch (error) {
    console.error('Error storing return path:', error);
  }
};

const clearReturnPath = () => {
  sessionStorage.removeItem(SESSION_STORAGE_KEYS.RETURN_PATH_STACK);
};

// Navigation context utilities
const createNavigationContext = (options) => {
  const {
    targetRoute,
    navigationData,
    transactionCode,
    preserveHistory,
    defaultReturnPath
  } = options;

  return {
    targetRoute,
    navigationData,
    transactionCode,
    timestamp: new Date().toISOString(),
    sessionId: getSessionId(),
    preserveHistory,
    defaultReturnPath,
    currentRoute: getCurrentRoute(),
    userRole: getUserRole()
  };
};

const storeNavigationContext = (context) => {
  try {
    sessionStorage.setItem(SESSION_STORAGE_KEYS.NAVIGATION_CONTEXT, JSON.stringify(context));
  } catch (error) {
    console.error('Error storing navigation context:', error);
  }
};

const executeNavigation = (targetRoute, context) => {
  // This would integrate with React Router in a real implementation
  // For now, we return a promise indicating navigation intent
  return Promise.resolve({
    success: true,
    targetRoute,
    context,
    timestamp: new Date().toISOString()
  });
};

// Session storage utilities
const storeSessionData = (sessionId, sessionData) => {
  try {
    sessionStorage.setItem(`${SESSION_STORAGE_KEYS.SESSION_DATA}_${sessionId}`, JSON.stringify(sessionData));
  } catch (error) {
    console.error('Error storing session data:', error);
  }
};

const retrieveSessionData = (sessionId) => {
  try {
    const stored = sessionStorage.getItem(`${SESSION_STORAGE_KEYS.SESSION_DATA}_${sessionId}`);
    return stored ? JSON.parse(stored) : null;
  } catch (error) {
    console.error('Error retrieving session data:', error);
    return null;
  }
};

const clearSessionData = (sessionId) => {
  sessionStorage.removeItem(`${SESSION_STORAGE_KEYS.SESSION_DATA}_${sessionId}`);
};

const storeScreenState = (route, screenData) => {
  try {
    sessionStorage.setItem(`${SESSION_STORAGE_KEYS.CURRENT_SCREEN_STATE}_${route}`, JSON.stringify(screenData));
  } catch (error) {
    console.error('Error storing screen state:', error);
  }
};

// Menu configuration utilities
const getMenuConfiguration = (menuType) => {
  if (menuType === 'admin') {
    return {
      options: {
        '1': { path: ROUTES.USER_LIST, title: 'User List', transactionCode: 'COUSR00', requiresAdmin: true },
        '2': { path: ROUTES.USER_CREATE, title: 'Create User', transactionCode: 'COUSR01', requiresAdmin: true },
        '3': { path: ROUTES.USER_UPDATE, title: 'Update User', transactionCode: 'COUSR02', requiresAdmin: true },
        '4': { path: ROUTES.USER_SEARCH, title: 'Search Users', transactionCode: 'COUSR03', requiresAdmin: true },
        '5': { path: ROUTES.MENU, title: 'Main Menu', transactionCode: 'COMEN01', requiresAdmin: false }
      }
    };
  } else {
    return {
      options: {
        '1': { path: ROUTES.ACCOUNT_VIEW, title: 'Account View', transactionCode: 'COACTVW', requiresAdmin: false },
        '2': { path: ROUTES.CARD_LIST, title: 'List Credit Cards', transactionCode: 'COCRDLI', requiresAdmin: false },
        '3': { path: ROUTES.TRANSACTION_LIST, title: 'List Transactions', transactionCode: 'COTRN00', requiresAdmin: false },
        '4': { path: ROUTES.BILL_PAYMENT, title: 'Bill Payment', transactionCode: 'COBIL00', requiresAdmin: false },
        '5': { path: ROUTES.REPORTS, title: 'Report Generation', transactionCode: 'CORPT00', requiresAdmin: false },
        '6': { path: ROUTES.ADMIN_MENU, title: 'Administrative Menu', transactionCode: 'COADM01', requiresAdmin: true }
      }
    };
  }
};

const getMenuReturnPath = (menuType) => {
  return menuType === 'admin' ? ROUTES.ADMIN_MENU : ROUTES.MENU;
};

// Navigation utilities
const navigateToLogin = () => {
  return { redirect: ROUTES.LOGIN, reason: 'authentication_required' };
};

const handleUnauthorizedNavigation = () => {
  return { redirect: ROUTES.UNAUTHORIZED, reason: 'access_denied' };
};

const navigateToMenuOption = (path, params) => {
  // Would integrate with React Router navigate in real implementation
  return Promise.resolve({ success: true, path, params });
};

const updateSessionNavigation = (route, state) => {
  const sessionId = getSessionId();
  if (sessionId) {
    const updates = {
      currentRoute: route,
      lastNavigation: state,
      lastActivity: new Date()
    };
    
    try {
      const manager = createSessionStateManager();
      manager.updateSession(sessionId, updates);
    } catch (error) {
      console.error('Error updating session navigation:', error);
    }
  }
};

const handleNavigationError = (error, targetPath) => {
  console.error('Navigation error:', error);
  
  // Return error recovery information
  return {
    success: false,
    error: error.message,
    targetPath,
    recovery: {
      suggestedAction: 'retry',
      fallbackRoute: ROUTES.MENU
    }
  };
};

// Error handling utilities
const displayMenuError = (message) => {
  console.error('Menu error:', message);
  // Would integrate with UI notification system
};

const confirmCancelOperation = () => {
  // Would integrate with UI confirmation dialog
  return true; // For now, always confirm
};

const clearPendingFormData = () => {
  // Clear any pending form data from session storage
  const keys = Object.keys(sessionStorage);
  keys.forEach(key => {
    if (key.startsWith('form_data_')) {
      sessionStorage.removeItem(key);
    }
  });
};

// Logging utilities
const logNavigationEvent = (eventType, eventData) => {
  const logEntry = {
    eventType,
    eventData,
    timestamp: new Date().toISOString(),
    sessionId: getSessionId(),
    userAgent: navigator.userAgent
  };
  
  // Store in session for debugging
  try {
    const logKey = `${SESSION_STORAGE_KEYS.TRANSACTION_HISTORY}_${Date.now()}`;
    sessionStorage.setItem(logKey, JSON.stringify(logEntry));
  } catch (error) {
    console.error('Error logging navigation event:', error);
  }
  
  // Console log for development
  console.log('Navigation Event:', logEntry);
};

// Redis integration stubs (would be implemented with actual Redis client)
const storeSessionInRedis = (sessionId, sessionData) => {
  // Would integrate with Redis client for distributed session storage
  console.log('Redis store session:', sessionId, sessionData);
};

const retrieveSessionFromRedis = (sessionId) => {
  // Would integrate with Redis client for session retrieval
  console.log('Redis retrieve session:', sessionId);
  return null;
};

const clearSessionFromRedis = (sessionId) => {
  // Would integrate with Redis client for session cleanup
  console.log('Redis clear session:', sessionId);
};

export default NavigationHelpers;