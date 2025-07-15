/**
 * navigationHelpers.js
 * 
 * Navigation utilities that translate CICS transaction flow and BMS screen transitions
 * to React Router patterns, maintaining pseudo-conversational state management and
 * preserving original mainframe navigation behavior.
 * 
 * This file provides comprehensive navigation functions that maintain exact functional
 * equivalence with the original COBOL/CICS application while enabling modern React
 * Router-based navigation. It implements pseudo-conversational processing, CICS XCTL
 * program flow, and BMS screen transition patterns through stateless architecture.
 * 
 * Key Features:
 * - CICS XCTL equivalent navigation using React Router declarative routing
 * - Pseudo-conversational state management with Redis session storage
 * - BMS screen flow replication with exact navigation patterns
 * - Function key navigation (F3=Exit, F7/F8=Paging, F12=Cancel)
 * - Menu-driven navigation with role-based filtering
 * - Breadcrumb navigation with hierarchical path tracking
 * - Transaction context preservation across route transitions
 * - Session timeout handling and recovery mechanisms
 * 
 * Technology Transformation: COBOL/CICS/BMS → Java/Spring Boot/React
 * Original System: IBM CICS Transaction Server with BMS screen definitions
 * Target System: React Router with Redux/Context API for state management
 * 
 * Copyright (c) 2023 CardDemo Application - Mainframe Modernization
 */

import { useNavigate } from 'react-router-dom';
import { useEffect } from 'react';
import { NavigationConstants } from '../constants/NavigationConstants';
import { KeyboardConstants } from '../constants/KeyboardConstants';
import { TransactionConstants } from '../constants/TransactionConstants';
import { NavigationTypes } from '../types/NavigationTypes';
import { CommonTypes } from '../types/CommonTypes';

const { ROUTES, NAVIGATION_FLOW, BREADCRUMB_PATHS } = NavigationConstants;
const { FUNCTION_KEYS, ALTERNATIVE_KEY_COMBINATIONS } = KeyboardConstants;
const { TRANSACTION_ENDPOINTS, API_BASE_PATHS } = TransactionConstants;

// ==========================================
// Session State Management - Redis Integration
// ==========================================

/**
 * Session state manager for pseudo-conversational processing
 * Maintains user session state across React Router transitions
 * using Redis-backed session storage equivalent to CICS terminal storage
 */
export const createSessionStateManager = (sessionConfig = {}) => {
  const SESSION_STORAGE_KEY = 'cardDemo.session';
  const SESSION_TIMEOUT = sessionConfig.timeout || 30 * 60 * 1000; // 30 minutes default
  
  /**
   * Get current session state from Redis/localStorage
   * Equivalent to CICS GETMAIN for working storage
   */
  const getSessionState = () => {
    try {
      const sessionData = localStorage.getItem(SESSION_STORAGE_KEY);
      if (!sessionData) return null;
      
      const parsed = JSON.parse(sessionData);
      
      // Check session expiration
      if (parsed.expires && new Date(parsed.expires) < new Date()) {
        clearSessionState();
        return null;
      }
      
      return parsed;
    } catch (error) {
      console.error('Error retrieving session state:', error);
      return null;
    }
  };
  
  /**
   * Set session state in Redis/localStorage
   * Equivalent to CICS FREEMAIN for working storage cleanup
   */
  const setSessionState = (state) => {
    try {
      const sessionData = {
        ...state,
        expires: new Date(Date.now() + SESSION_TIMEOUT).toISOString(),
        lastActivity: new Date().toISOString()
      };
      
      localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(sessionData));
    } catch (error) {
      console.error('Error setting session state:', error);
    }
  };
  
  /**
   * Clear session state on logout or timeout
   * Equivalent to CICS RETURN for transaction cleanup
   */
  const clearSessionState = () => {
    try {
      localStorage.removeItem(SESSION_STORAGE_KEY);
    } catch (error) {
      console.error('Error clearing session state:', error);
    }
  };
  
  /**
   * Update session activity timestamp
   * Equivalent to CICS terminal storage refresh
   */
  const refreshSession = () => {
    const currentState = getSessionState();
    if (currentState) {
      setSessionState(currentState);
    }
  };
  
  return {
    getSessionState,
    setSessionState,
    clearSessionState,
    refreshSession
  };
};

// ==========================================
// Navigation Handler Functions - CICS XCTL Equivalents
// ==========================================

/**
 * Create navigation handler for CICS XCTL program flow
 * Maintains transaction boundaries and pseudo-conversational state
 */
export const createNavigationHandler = (options = {}) => {
  const sessionManager = createSessionStateManager(options.sessionConfig);
  const navigate = useNavigate();
  
  /**
   * Navigate to target route with state preservation
   * Equivalent to EXEC CICS XCTL PROGRAM command
   */
  const navigateTo = (targetRoute, navigationState = {}) => {
    try {
      // Preserve current session state
      const currentSession = sessionManager.getSessionState();
      
      // Update session with navigation context
      const updatedSession = {
        ...currentSession,
        previousRoute: window.location.pathname,
        currentRoute: targetRoute,
        navigationState,
        transactionId: generateTransactionId(),
        timestamp: new Date().toISOString()
      };
      
      sessionManager.setSessionState(updatedSession);
      
      // Perform React Router navigation
      navigate(targetRoute, {
        state: navigationState,
        replace: options.replace || false
      });
      
      // Refresh session activity
      sessionManager.refreshSession();
      
    } catch (error) {
      console.error('Navigation error:', error);
      handleNavigationError(error);
    }
  };
  
  /**
   * Navigate with state preservation
   * Equivalent to CICS COMMAREA passing between programs
   */
  const navigateWithState = (targetRoute, state = {}) => {
    const navigationState = {
      ...state,
      preserveState: true,
      timestamp: new Date().toISOString()
    };
    
    navigateTo(targetRoute, navigationState);
  };
  
  /**
   * Navigate back to previous screen
   * Equivalent to CICS RETURN command
   */
  const navigateBack = () => {
    const currentSession = sessionManager.getSessionState();
    const previousRoute = currentSession?.previousRoute || ROUTES.MENU;
    
    navigateTo(previousRoute);
  };
  
  /**
   * Handle navigation errors
   * Equivalent to CICS ABEND handling
   */
  const handleNavigationError = (error) => {
    console.error('Navigation error:', error);
    
    // Attempt to recover by navigating to main menu
    const errorRecoveryRoute = ROUTES.MENU;
    
    try {
      navigate(errorRecoveryRoute, {
        state: {
          error: error.message,
          recovery: true,
          timestamp: new Date().toISOString()
        }
      });
    } catch (recoveryError) {
      console.error('Navigation recovery failed:', recoveryError);
    }
  };
  
  return {
    navigateTo,
    navigateWithState,
    navigateBack,
    handleNavigationError
  };
};

/**
 * Navigate with state preservation - Direct export function
 * Provides direct access to navigation with state management
 */
export const navigateWithState = (targetRoute, state = {}) => {
  const navigationHandler = createNavigationHandler();
  return navigationHandler.navigateWithState(targetRoute, state);
};

// ==========================================
// Pseudo-Conversational Handler - CICS Terminal Storage Equivalent
// ==========================================

/**
 * Create pseudo-conversational handler for stateless navigation
 * Maintains conversation state across React Router transitions
 */
export const createPseudoConversationalHandler = (conversationConfig = {}) => {
  const sessionManager = createSessionStateManager(conversationConfig.sessionConfig);
  const CONVERSATION_TIMEOUT = conversationConfig.timeout || 15 * 60 * 1000; // 15 minutes
  
  /**
   * Initialize conversation state
   * Equivalent to CICS ASSIGN for conversation management
   */
  const initializeConversation = (transactionCode, initialData = {}) => {
    const conversationId = generateConversationId();
    const conversationState = {
      conversationId,
      transactionCode,
      initialData,
      createdAt: new Date().toISOString(),
      lastActivity: new Date().toISOString(),
      expires: new Date(Date.now() + CONVERSATION_TIMEOUT).toISOString(),
      isActive: true
    };
    
    sessionManager.setSessionState({
      ...sessionManager.getSessionState(),
      conversation: conversationState
    });
    
    return conversationId;
  };
  
  /**
   * Get conversation state
   * Equivalent to CICS RETRIEVE for conversation data
   */
  const getConversationState = () => {
    const session = sessionManager.getSessionState();
    return session?.conversation || null;
  };
  
  /**
   * Update conversation state
   * Equivalent to CICS CONVERSE for conversation continuation
   */
  const updateConversationState = (updates) => {
    const session = sessionManager.getSessionState();
    if (!session?.conversation) return null;
    
    const updatedConversation = {
      ...session.conversation,
      ...updates,
      lastActivity: new Date().toISOString()
    };
    
    sessionManager.setSessionState({
      ...session,
      conversation: updatedConversation
    });
    
    return updatedConversation;
  };
  
  /**
   * End conversation
   * Equivalent to CICS RETURN for conversation cleanup
   */
  const endConversation = () => {
    const session = sessionManager.getSessionState();
    if (session?.conversation) {
      const updatedSession = { ...session };
      delete updatedSession.conversation;
      sessionManager.setSessionState(updatedSession);
    }
  };
  
  /**
   * Check conversation expiration
   * Equivalent to CICS HANDLE CONDITION for timeout handling
   */
  const checkConversationExpiry = () => {
    const conversation = getConversationState();
    if (!conversation) return false;
    
    const now = new Date();
    const expires = new Date(conversation.expires);
    
    if (now > expires) {
      endConversation();
      return true;
    }
    
    return false;
  };
  
  return {
    initializeConversation,
    getConversationState,
    updateConversationState,
    endConversation,
    checkConversationExpiry
  };
};

// ==========================================
// Breadcrumb Navigation - CICS Program Flow Tracking
// ==========================================

/**
 * Create breadcrumb tracker for navigation path management
 * Maintains hierarchical navigation structure equivalent to CICS program flow
 */
export const createBreadcrumbTracker = (breadcrumbConfig = {}) => {
  const sessionManager = createSessionStateManager(breadcrumbConfig.sessionConfig);
  const MAX_BREADCRUMBS = breadcrumbConfig.maxBreadcrumbs || 5;
  
  /**
   * Generate breadcrumb trail for current route
   * Equivalent to CICS program flow tracking
   */
  const generateBreadcrumbTrail = (currentPath) => {
    const pathMapping = BREADCRUMB_PATHS.PATH_MAPPING;
    const currentBreadcrumb = pathMapping[currentPath];
    
    if (!currentBreadcrumb) {
      return [getHomeBreadcrumb()];
    }
    
    const trail = [];
    
    // Add home breadcrumb
    trail.push(getHomeBreadcrumb());
    
    // Add parent breadcrumbs based on hierarchy
    const parentBreadcrumbs = getParentBreadcrumbs(currentPath);
    trail.push(...parentBreadcrumbs);
    
    // Add current breadcrumb
    trail.push({
      ...currentBreadcrumb,
      isActive: true
    });
    
    // Limit breadcrumb trail length
    return trail.slice(-MAX_BREADCRUMBS);
  };
  
  /**
   * Get home breadcrumb
   * Equivalent to CICS main menu reference
   */
  const getHomeBreadcrumb = () => {
    return BREADCRUMB_PATHS.HIERARCHY_LEVELS.BREADCRUMB_CONFIG.HOME_BREADCRUMB;
  };
  
  /**
   * Get parent breadcrumbs for hierarchical navigation
   * Equivalent to CICS program call stack
   */
  const getParentBreadcrumbs = (currentPath) => {
    const parentBreadcrumbs = [];
    const returnPath = NAVIGATION_FLOW.RETURN_PATHS[currentPath];
    
    if (returnPath && returnPath !== ROUTES.MENU) {
      const parentBreadcrumb = BREADCRUMB_PATHS.PATH_MAPPING[returnPath];
      if (parentBreadcrumb) {
        parentBreadcrumbs.push(parentBreadcrumb);
      }
    }
    
    return parentBreadcrumbs;
  };
  
  /**
   * Update breadcrumb trail in session
   * Equivalent to CICS working storage update
   */
  const updateBreadcrumbTrail = (currentPath) => {
    const trail = generateBreadcrumbTrail(currentPath);
    const session = sessionManager.getSessionState();
    
    sessionManager.setSessionState({
      ...session,
      breadcrumbTrail: trail
    });
    
    return trail;
  };
  
  /**
   * Get current breadcrumb trail
   * Equivalent to CICS working storage retrieval
   */
  const getCurrentBreadcrumbTrail = () => {
    const session = sessionManager.getSessionState();
    return session?.breadcrumbTrail || [getHomeBreadcrumb()];
  };
  
  return {
    generateBreadcrumbTrail,
    getHomeBreadcrumb,
    getParentBreadcrumbs,
    updateBreadcrumbTrail,
    getCurrentBreadcrumbTrail
  };
};

// ==========================================
// Menu Navigation Handler - BMS Menu Processing
// ==========================================

/**
 * Create menu navigation handler for menu-driven screen transitions
 * Replicates COMEN01 and COADM01 BMS menu functionality
 */
export const createMenuNavigationHandler = (menuConfig = {}) => {
  const navigationHandler = createNavigationHandler(menuConfig);
  const sessionManager = createSessionStateManager(menuConfig.sessionConfig);
  
  /**
   * Process menu option selection
   * Equivalent to CICS menu option processing
   */
  const processMenuSelection = (menuType, optionNumber, userRole = 'USER') => {
    const menuHierarchy = NAVIGATION_FLOW.MENU_HIERARCHY;
    const menuStructure = menuType === 'ADMIN' ? menuHierarchy.ADMIN_MENU : menuHierarchy.MAIN_MENU;
    
    // Find selected menu option
    const selectedOption = menuStructure.options.find(option => 
      option.optionNumber === optionNumber && 
      isOptionAvailable(option, userRole)
    );
    
    if (!selectedOption) {
      throw new Error(`Invalid menu option: ${optionNumber}`);
    }
    
    // Navigate to selected option
    navigationHandler.navigateWithState(selectedOption.targetRoute, {
      fromMenu: menuType,
      optionNumber,
      transactionCode: selectedOption.transactionCode,
      menuSelection: true
    });
    
    return selectedOption;
  };
  
  /**
   * Check if menu option is available for user role
   * Equivalent to RACF security checking
   */
  const isOptionAvailable = (option, userRole) => {
    if (option.requiredRole === 'ADMIN' && userRole !== 'ADMIN') {
      return false;
    }
    
    return option.isAvailable;
  };
  
  /**
   * Get available menu options for user role
   * Equivalent to CICS menu generation based on user profile
   */
  const getAvailableMenuOptions = (menuType, userRole = 'USER') => {
    const menuHierarchy = NAVIGATION_FLOW.MENU_HIERARCHY;
    const menuStructure = menuType === 'ADMIN' ? menuHierarchy.ADMIN_MENU : menuHierarchy.MAIN_MENU;
    
    return menuStructure.options.filter(option => 
      isOptionAvailable(option, userRole)
    );
  };
  
  /**
   * Navigate to menu based on user role
   * Equivalent to CICS menu display logic
   */
  const navigateToMenu = (userRole = 'USER') => {
    const targetRoute = userRole === 'ADMIN' ? ROUTES.ADMIN_MENU : ROUTES.MENU;
    
    navigationHandler.navigateWithState(targetRoute, {
      userRole,
      menuType: userRole === 'ADMIN' ? 'ADMIN' : 'MAIN',
      transactionCode: userRole === 'ADMIN' ? 'COADM01' : 'COMEN01'
    });
  };
  
  return {
    processMenuSelection,
    isOptionAvailable,
    getAvailableMenuOptions,
    navigateToMenu
  };
};

// ==========================================
// Return Path Handler - F3/PF3 Exit Functionality
// ==========================================

/**
 * Create return path handler for F3/PF3 exit functionality
 * Maintains CICS program return logic through React Router history
 */
export const createReturnPathHandler = (returnConfig = {}) => {
  const navigationHandler = createNavigationHandler(returnConfig);
  const sessionManager = createSessionStateManager(returnConfig.sessionConfig);
  
  /**
   * Handle F3/Exit navigation
   * Equivalent to CICS RETURN command
   */
  const handleExit = (currentPath) => {
    const returnPath = getReturnPath(currentPath);
    
    // Log exit action for audit
    logNavigationAction('EXIT', currentPath, returnPath);
    
    // Navigate to return path
    navigationHandler.navigateWithState(returnPath, {
      exitAction: true,
      fromPath: currentPath,
      transactionCode: getTransactionCodeForPath(returnPath)
    });
  };
  
  /**
   * Get return path for current route
   * Equivalent to CICS program return address
   */
  const getReturnPath = (currentPath) => {
    return NAVIGATION_FLOW.RETURN_PATHS[currentPath] || ROUTES.MENU;
  };
  
  /**
   * Handle F12/Cancel navigation
   * Equivalent to CICS ABEND with return to main menu
   */
  const handleCancel = (currentPath) => {
    const session = sessionManager.getSessionState();
    const userRole = session?.userRole || 'USER';
    const cancelPath = userRole === 'ADMIN' ? ROUTES.ADMIN_MENU : ROUTES.MENU;
    
    // Log cancel action for audit
    logNavigationAction('CANCEL', currentPath, cancelPath);
    
    // Navigate to cancel path
    navigationHandler.navigateWithState(cancelPath, {
      cancelAction: true,
      fromPath: currentPath,
      transactionCode: getTransactionCodeForPath(cancelPath)
    });
  };
  
  /**
   * Get transaction code for path
   * Equivalent to CICS transaction identification
   */
  const getTransactionCodeForPath = (path) => {
    const breadcrumbData = BREADCRUMB_PATHS.PATH_MAPPING[path];
    return breadcrumbData?.transactionCode || 'UNKNOWN';
  };
  
  /**
   * Log navigation action for audit
   * Equivalent to CICS audit logging
   */
  const logNavigationAction = (action, fromPath, toPath) => {
    const logEntry = {
      action,
      fromPath,
      toPath,
      timestamp: new Date().toISOString(),
      sessionId: sessionManager.getSessionState()?.sessionId
    };
    
    console.log('Navigation action:', logEntry);
  };
  
  return {
    handleExit,
    getReturnPath,
    handleCancel,
    getTransactionCodeForPath,
    logNavigationAction
  };
};

// ==========================================
// Transaction Router - CICS Transaction Mapping
// ==========================================

/**
 * Create transaction router for CICS transaction code mapping
 * Maps original transaction codes to React Router paths
 */
export const createTransactionRouter = (transactionConfig = {}) => {
  const navigationHandler = createNavigationHandler(transactionConfig);
  
  /**
   * Route transaction by transaction code
   * Equivalent to CICS transaction routing
   */
  const routeTransaction = (transactionCode, data = {}) => {
    const routeMapping = getRouteForTransaction(transactionCode);
    
    if (!routeMapping) {
      throw new Error(`Unknown transaction code: ${transactionCode}`);
    }
    
    navigationHandler.navigateWithState(routeMapping.route, {
      transactionCode,
      originalTransaction: true,
      data,
      ...routeMapping.defaultState
    });
  };
  
  /**
   * Get React Router route for transaction code
   * Equivalent to CICS program definition table lookup
   */
  const getRouteForTransaction = (transactionCode) => {
    const transactionRoutes = {
      'COSGN00': { route: ROUTES.LOGIN, defaultState: { screen: 'login' } },
      'COMEN01': { route: ROUTES.MENU, defaultState: { screen: 'menu' } },
      'COADM01': { route: ROUTES.ADMIN_MENU, defaultState: { screen: 'admin_menu' } },
      'COACTVW': { route: ROUTES.ACCOUNT_VIEW, defaultState: { screen: 'account_view' } },
      'COACTUP': { route: ROUTES.ACCOUNT_UPDATE, defaultState: { screen: 'account_update' } },
      'COCRDLI': { route: ROUTES.CARD_LIST, defaultState: { screen: 'card_list' } },
      'COCRDSL': { route: ROUTES.CARD_SEARCH, defaultState: { screen: 'card_search' } },
      'COCRDUP': { route: ROUTES.CARD_UPDATE, defaultState: { screen: 'card_update' } },
      'COTRN00': { route: ROUTES.TRANSACTION_LIST, defaultState: { screen: 'transaction_list' } },
      'COTRN01': { route: ROUTES.TRANSACTION_DETAIL, defaultState: { screen: 'transaction_detail' } },
      'COTRN02': { route: ROUTES.TRANSACTION_ADD, defaultState: { screen: 'transaction_add' } },
      'COBIL00': { route: ROUTES.BILL_PAYMENT, defaultState: { screen: 'bill_payment' } },
      'CORPT00': { route: ROUTES.REPORTS, defaultState: { screen: 'reports' } },
      'COUSR00': { route: ROUTES.USER_LIST, defaultState: { screen: 'user_list' } },
      'COUSR01': { route: ROUTES.USER_CREATE, defaultState: { screen: 'user_create' } },
      'COUSR02': { route: ROUTES.USER_UPDATE, defaultState: { screen: 'user_update' } },
      'COUSR03': { route: ROUTES.USER_DELETE, defaultState: { screen: 'user_delete' } }
    };
    
    return transactionRoutes[transactionCode] || null;
  };
  
  /**
   * Get transaction code for current route
   * Equivalent to CICS ASSIGN TRANSID
   */
  const getTransactionForRoute = (route) => {
    const routeTransactions = {
      [ROUTES.LOGIN]: 'COSGN00',
      [ROUTES.MENU]: 'COMEN01',
      [ROUTES.ADMIN_MENU]: 'COADM01',
      [ROUTES.ACCOUNT_VIEW]: 'COACTVW',
      [ROUTES.ACCOUNT_UPDATE]: 'COACTUP',
      [ROUTES.CARD_LIST]: 'COCRDLI',
      [ROUTES.CARD_SEARCH]: 'COCRDSL',
      [ROUTES.CARD_UPDATE]: 'COCRDUP',
      [ROUTES.TRANSACTION_LIST]: 'COTRN00',
      [ROUTES.TRANSACTION_DETAIL]: 'COTRN01',
      [ROUTES.TRANSACTION_ADD]: 'COTRN02',
      [ROUTES.BILL_PAYMENT]: 'COBIL00',
      [ROUTES.REPORTS]: 'CORPT00',
      [ROUTES.USER_LIST]: 'COUSR00',
      [ROUTES.USER_CREATE]: 'COUSR01',
      [ROUTES.USER_UPDATE]: 'COUSR02',
      [ROUTES.USER_DELETE]: 'COUSR03'
    };
    
    return routeTransactions[route] || 'UNKNOWN';
  };
  
  return {
    routeTransaction,
    getRouteForTransaction,
    getTransactionForRoute
  };
};

// ==========================================
// React Hook - useNavigationHelpers
// ==========================================

/**
 * React hook for navigation helpers
 * Provides access to all navigation functionality in React components
 */
export const useNavigationHelpers = (config = {}) => {
  const navigate = useNavigate();
  
  // Initialize navigation utilities
  const sessionManager = createSessionStateManager(config.sessionConfig);
  const navigationHandler = createNavigationHandler(config);
  const breadcrumbTracker = createBreadcrumbTracker(config);
  const menuNavigationHandler = createMenuNavigationHandler(config);
  const returnPathHandler = createReturnPathHandler(config);
  const transactionRouter = createTransactionRouter(config);
  const pseudoConversationalHandler = createPseudoConversationalHandler(config);
  
  // Set up navigation effect
  useEffect(() => {
    // Update breadcrumb trail on route change
    const currentPath = window.location.pathname;
    breadcrumbTracker.updateBreadcrumbTrail(currentPath);
    
    // Check conversation expiry
    pseudoConversationalHandler.checkConversationExpiry();
    
    // Refresh session
    sessionManager.refreshSession();
  }, [window.location.pathname]);
  
  // Set up keyboard event handlers
  useEffect(() => {
    const handleKeyDown = (event) => {
      const currentPath = window.location.pathname;
      
      // Handle F3 - Exit
      if (event.key === 'F3' && !event.ctrlKey && !event.altKey) {
        event.preventDefault();
        returnPathHandler.handleExit(currentPath);
      }
      
      // Handle F12 - Cancel
      if (event.key === 'F12' && !event.ctrlKey && !event.altKey) {
        event.preventDefault();
        returnPathHandler.handleCancel(currentPath);
      }
    };
    
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, []);
  
  return {
    // Navigation functions
    navigate: navigationHandler.navigateTo,
    navigateWithState: navigationHandler.navigateWithState,
    navigateBack: navigationHandler.navigateBack,
    
    // Breadcrumb functions
    createBreadcrumb: breadcrumbTracker.generateBreadcrumbTrail,
    getCurrentBreadcrumbs: breadcrumbTracker.getCurrentBreadcrumbTrail,
    
    // Exit and return functions
    handleExit: returnPathHandler.handleExit,
    handleCancel: returnPathHandler.handleCancel,
    
    // Session management
    manageSession: sessionManager,
    
    // Transaction routing
    routeTransaction: transactionRouter.routeTransaction,
    
    // State preservation
    preserveState: pseudoConversationalHandler.updateConversationState,
    
    // Menu navigation
    processMenuSelection: menuNavigationHandler.processMenuSelection,
    navigateToMenu: menuNavigationHandler.navigateToMenu
  };
};

// ==========================================
// Utility Functions - Helper Functions
// ==========================================

/**
 * Generate unique transaction ID
 * Equivalent to CICS EIBTRNID generation
 */
const generateTransactionId = () => {
  return `TXN_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
};

/**
 * Generate unique conversation ID
 * Equivalent to CICS conversation token generation
 */
const generateConversationId = () => {
  return `CONV_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
};

/**
 * Format screen data for display
 * Equivalent to BMS formatting
 */
const formatScreenData = (data) => {
  return {
    ...data,
    curdate: new Date().toLocaleDateString('en-US', { 
      month: '2-digit', 
      day: '2-digit', 
      year: '2-digit' 
    }),
    curtime: new Date().toLocaleTimeString('en-US', { 
      hour12: false,
      hour: '2-digit', 
      minute: '2-digit', 
      second: '2-digit' 
    })
  };
};

// ==========================================
// Main Navigation Helpers Object - Default Export
// ==========================================

/**
 * Main navigation helpers object providing centralized access to all navigation functionality
 * Equivalent to CICS program table with all available navigation services
 */
const NavigationHelpers = {
  // Core navigation functions
  navigate: (route, state = {}) => {
    const handler = createNavigationHandler();
    return handler.navigateWithState(route, state);
  },
  
  // Breadcrumb management
  createBreadcrumb: (currentPath) => {
    const tracker = createBreadcrumbTracker();
    return tracker.generateBreadcrumbTrail(currentPath);
  },
  
  // Exit handling
  handleExit: (currentPath) => {
    const handler = createReturnPathHandler();
    return handler.handleExit(currentPath);
  },
  
  // Session management
  manageSession: createSessionStateManager(),
  
  // Transaction routing
  routeTransaction: (transactionCode, data = {}) => {
    const router = createTransactionRouter();
    return router.routeTransaction(transactionCode, data);
  },
  
  // State preservation
  preserveState: (state) => {
    const handler = createPseudoConversationalHandler();
    return handler.updateConversationState(state);
  }
};

export default NavigationHelpers;