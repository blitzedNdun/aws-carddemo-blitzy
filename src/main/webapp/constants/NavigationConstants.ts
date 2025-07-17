/**
 * Navigation Constants for CardDemo Application
 * 
 * This file provides centralized React Router navigation paths and routing configuration
 * extracted from BMS screen flow patterns. Maps original CICS XCTL navigation to modern
 * declarative routing while preserving user experience and security hierarchies.
 * 
 * Enables consistent routing across all React components while maintaining exact
 * functional equivalence with the original COBOL/CICS navigation system.
 */

import { TRANSACTION_ENDPOINTS } from './TransactionConstants';
import { BreadcrumbData } from '../types/NavigationTypes';

/**
 * Core Route Path Constants
 * Maps each BMS screen to its corresponding React Router path
 * preserving original transaction code references for audit traceability
 */
export const ROUTES = {
  /**
   * Authentication Routes
   * Maps COSGN00.bms login screen to modern authentication flow
   */
  LOGIN: '/login',
  
  /**
   * Menu Navigation Routes
   * Maps COMEN01.bms and COADM01.bms to role-based menu systems
   */
  MENU: {
    MAIN: '/menu',
    ADMIN: '/admin'
  },
  
  /**
   * Account Management Routes
   * Maps COACTVW.bms and COACTUP.bms to account operations
   */
  ACCOUNT_VIEW: '/accounts/view',
  ACCOUNT_UPDATE: '/accounts/update/:accountId',
  
  /**
   * Card Management Routes
   * Maps COCRDLI.bms, COCRDSL.bms, and COCRDUP.bms to card operations
   */
  CARD_LIST: '/cards/list',
  CARD_SEARCH: '/cards/search',
  CARD_DETAIL: '/cards/detail/:cardNumber',
  CARD_UPDATE: '/cards/update/:cardNumber',
  
  /**
   * Transaction Processing Routes
   * Maps COTRN00.bms, COTRN01.bms, and COTRN02.bms to transaction operations
   */
  TRANSACTION_LIST: '/transactions/list',
  TRANSACTION_SEARCH: '/transactions/search',
  TRANSACTION_DETAIL: '/transactions/detail/:transactionId',
  TRANSACTION_ADD: '/transactions/add',
  
  /**
   * Bill Payment Routes
   * Maps COBIL00.bms to payment processing
   */
  BILL_PAYMENT: '/payments/bill',
  
  /**
   * Report Generation Routes
   * Maps CORPT00.bms to report functionality
   */
  REPORT_GENERATION: '/reports',
  
  /**
   * User Management Routes
   * Maps COUSR00.bms, COUSR01.bms, COUSR02.bms, COUSR03.bms to user administration
   */
  USER_LIST: '/users/list',
  USER_ADD: '/users/add',
  USER_UPDATE: '/users/update/:userId',
  USER_DELETE: '/users/delete/:userId',
  
  /**
   * Exit Paths
   * Defines logout and system exit navigation paths
   */
  EXIT_PATHS: {
    LOGOUT: '/logout',
    SYSTEM_EXIT: '/exit'
  }
} as const;

/**
 * Navigation Flow Constants
 * Maps original CICS XCTL patterns to React Router navigation sequences
 * preserving exact user experience and security hierarchies
 */
export const NAVIGATION_FLOW = {
  /**
   * XCTL Pattern Mapping
   * Preserves original CICS transfer control navigation patterns
   * Maps source transaction to allowed destination transactions
   */
  XCTL_PATTERNS: {
    // Authentication Flow - COSGN00.bms navigation
    [ROUTES.LOGIN]: {
      SUCCESS_ADMIN: ROUTES.MENU.ADMIN,
      SUCCESS_USER: ROUTES.MENU.MAIN,
      FAILURE: ROUTES.LOGIN
    },
    
    // Main Menu Flow - COMEN01.bms navigation
    [ROUTES.MENU.MAIN]: [
      ROUTES.ACCOUNT_VIEW,
      ROUTES.CARD_LIST,
      ROUTES.TRANSACTION_LIST,
      ROUTES.BILL_PAYMENT,
      ROUTES.REPORT_GENERATION,
      ROUTES.EXIT_PATHS.LOGOUT
    ],
    
    // Admin Menu Flow - COADM01.bms navigation
    [ROUTES.MENU.ADMIN]: [
      ROUTES.USER_LIST,
      ROUTES.USER_ADD,
      ROUTES.ACCOUNT_VIEW,
      ROUTES.CARD_LIST,
      ROUTES.TRANSACTION_LIST,
      ROUTES.REPORT_GENERATION,
      ROUTES.EXIT_PATHS.LOGOUT
    ],
    
    // Account Management Flow - COACTVW.bms to COACTUP.bms
    [ROUTES.ACCOUNT_VIEW]: [
      ROUTES.ACCOUNT_UPDATE,
      ROUTES.CARD_LIST,
      ROUTES.TRANSACTION_LIST,
      ROUTES.MENU.MAIN
    ],
    
    // Card Management Flow - COCRDLI.bms to COCRDSL.bms to COCRDUP.bms
    [ROUTES.CARD_LIST]: [
      ROUTES.CARD_SEARCH,
      ROUTES.CARD_DETAIL,
      ROUTES.CARD_UPDATE,
      ROUTES.MENU.MAIN
    ],
    
    [ROUTES.CARD_SEARCH]: [
      ROUTES.CARD_DETAIL,
      ROUTES.CARD_UPDATE,
      ROUTES.CARD_LIST
    ],
    
    [ROUTES.CARD_DETAIL]: [
      ROUTES.CARD_UPDATE,
      ROUTES.CARD_LIST,
      ROUTES.TRANSACTION_LIST
    ],
    
    // Transaction Processing Flow - COTRN00.bms to COTRN01.bms to COTRN02.bms
    [ROUTES.TRANSACTION_LIST]: [
      ROUTES.TRANSACTION_SEARCH,
      ROUTES.TRANSACTION_DETAIL,
      ROUTES.TRANSACTION_ADD,
      ROUTES.MENU.MAIN
    ],
    
    [ROUTES.TRANSACTION_SEARCH]: [
      ROUTES.TRANSACTION_DETAIL,
      ROUTES.TRANSACTION_ADD,
      ROUTES.TRANSACTION_LIST
    ],
    
    [ROUTES.TRANSACTION_DETAIL]: [
      ROUTES.TRANSACTION_LIST,
      ROUTES.ACCOUNT_VIEW,
      ROUTES.CARD_DETAIL
    ],
    
    // User Management Flow - COUSR00.bms to COUSR01.bms/COUSR02.bms/COUSR03.bms
    [ROUTES.USER_LIST]: [
      ROUTES.USER_ADD,
      ROUTES.USER_UPDATE,
      ROUTES.USER_DELETE,
      ROUTES.MENU.ADMIN
    ],
    
    [ROUTES.USER_ADD]: [
      ROUTES.USER_LIST,
      ROUTES.MENU.ADMIN
    ],
    
    [ROUTES.USER_UPDATE]: [
      ROUTES.USER_LIST,
      ROUTES.MENU.ADMIN
    ],
    
    [ROUTES.USER_DELETE]: [
      ROUTES.USER_LIST,
      ROUTES.MENU.ADMIN
    ]
  },
  
  /**
   * Return Path Mapping
   * Maps F3 (Exit) and back button navigation patterns
   * preserving original CICS return behavior
   */
  RETURN_PATHS: {
    // Account Management Returns
    [ROUTES.ACCOUNT_VIEW]: ROUTES.MENU.MAIN,
    [ROUTES.ACCOUNT_UPDATE]: ROUTES.ACCOUNT_VIEW,
    
    // Card Management Returns
    [ROUTES.CARD_LIST]: ROUTES.MENU.MAIN,
    [ROUTES.CARD_SEARCH]: ROUTES.CARD_LIST,
    [ROUTES.CARD_DETAIL]: ROUTES.CARD_LIST,
    [ROUTES.CARD_UPDATE]: ROUTES.CARD_LIST,
    
    // Transaction Processing Returns
    [ROUTES.TRANSACTION_LIST]: ROUTES.MENU.MAIN,
    [ROUTES.TRANSACTION_SEARCH]: ROUTES.TRANSACTION_LIST,
    [ROUTES.TRANSACTION_DETAIL]: ROUTES.TRANSACTION_LIST,
    [ROUTES.TRANSACTION_ADD]: ROUTES.TRANSACTION_LIST,
    
    // Bill Payment Returns
    [ROUTES.BILL_PAYMENT]: ROUTES.MENU.MAIN,
    
    // Report Generation Returns
    [ROUTES.REPORT_GENERATION]: ROUTES.MENU.MAIN,
    
    // User Management Returns
    [ROUTES.USER_LIST]: ROUTES.MENU.ADMIN,
    [ROUTES.USER_ADD]: ROUTES.USER_LIST,
    [ROUTES.USER_UPDATE]: ROUTES.USER_LIST,
    [ROUTES.USER_DELETE]: ROUTES.USER_LIST,
    
    // Menu Returns
    [ROUTES.MENU.MAIN]: ROUTES.LOGIN,
    [ROUTES.MENU.ADMIN]: ROUTES.LOGIN
  },
  
  /**
   * Menu Hierarchy Definition
   * Maps role-based menu structures from COMEN01.bms and COADM01.bms
   * supporting dynamic menu generation based on user roles
   */
  MENU_HIERARCHY: {
    // Regular User Menu Structure (COMEN01.bms)
    REGULAR_USER: {
      level: 1,
      title: 'Main Menu',
      path: ROUTES.MENU.MAIN,
      options: [
        {
          id: 1,
          title: 'View Account Information',
          path: ROUTES.ACCOUNT_VIEW,
          description: 'Display account details and customer information'
        },
        {
          id: 2,
          title: 'View Credit Card Information',
          path: ROUTES.CARD_LIST,
          description: 'Browse and manage credit card portfolio'
        },
        {
          id: 3,
          title: 'View Transaction Information',
          path: ROUTES.TRANSACTION_LIST,
          description: 'Review transaction history and details'
        },
        {
          id: 4,
          title: 'Make a Payment',
          path: ROUTES.BILL_PAYMENT,
          description: 'Process account payments and adjustments'
        },
        {
          id: 5,
          title: 'Generate Reports',
          path: ROUTES.REPORT_GENERATION,
          description: 'Create and download account reports'
        }
      ]
    },
    
    // Administrator Menu Structure (COADM01.bms)
    ADMINISTRATOR: {
      level: 1,
      title: 'Admin Menu',
      path: ROUTES.MENU.ADMIN,
      options: [
        {
          id: 1,
          title: 'User Management',
          path: ROUTES.USER_LIST,
          description: 'Manage system users and access controls'
        },
        {
          id: 2,
          title: 'Add New User',
          path: ROUTES.USER_ADD,
          description: 'Create new user accounts and assign roles'
        },
        {
          id: 3,
          title: 'View Account Information',
          path: ROUTES.ACCOUNT_VIEW,
          description: 'Administrative account access and review'
        },
        {
          id: 4,
          title: 'View Credit Card Information',
          path: ROUTES.CARD_LIST,
          description: 'Administrative card management and oversight'
        },
        {
          id: 5,
          title: 'View Transaction Information',
          path: ROUTES.TRANSACTION_LIST,
          description: 'Administrative transaction monitoring and analysis'
        },
        {
          id: 6,
          title: 'Generate Reports',
          path: ROUTES.REPORT_GENERATION,
          description: 'Administrative reporting and system analytics'
        }
      ]
    }
  }
} as const;

/**
 * Breadcrumb Navigation Constants
 * Implements breadcrumb navigation path tracking for user orientation
 * and F3/Back button functionality preserving CICS screen hierarchy
 */
export const BREADCRUMB_PATHS = {
  /**
   * Path Mapping
   * Maps each route to its breadcrumb configuration
   * preserving original CICS navigation hierarchy
   */
  PATH_MAPPING: {
    // Authentication Level
    [ROUTES.LOGIN]: {
      path: ROUTES.LOGIN,
      title: 'Login',
      transactionCode: 'COSGN00',
      level: 0,
      isActive: true
    },
    
    // Menu Level
    [ROUTES.MENU.MAIN]: {
      path: ROUTES.MENU.MAIN,
      title: 'Main Menu',
      transactionCode: 'COMEN01',
      level: 1,
      isActive: true
    },
    
    [ROUTES.MENU.ADMIN]: {
      path: ROUTES.MENU.ADMIN,
      title: 'Admin Menu',
      transactionCode: 'COADM01',
      level: 1,
      isActive: true
    },
    
    // Account Management Level
    [ROUTES.ACCOUNT_VIEW]: {
      path: ROUTES.ACCOUNT_VIEW,
      title: 'Account View',
      transactionCode: 'COACTVW',
      level: 2,
      isActive: true
    },
    
    [ROUTES.ACCOUNT_UPDATE]: {
      path: ROUTES.ACCOUNT_UPDATE,
      title: 'Account Update',
      transactionCode: 'COACTUP',
      level: 3,
      isActive: true
    },
    
    // Card Management Level
    [ROUTES.CARD_LIST]: {
      path: ROUTES.CARD_LIST,
      title: 'Card List',
      transactionCode: 'COCRDLI',
      level: 2,
      isActive: true
    },
    
    [ROUTES.CARD_SEARCH]: {
      path: ROUTES.CARD_SEARCH,
      title: 'Card Search',
      transactionCode: 'COCRDSL',
      level: 3,
      isActive: true
    },
    
    [ROUTES.CARD_DETAIL]: {
      path: ROUTES.CARD_DETAIL,
      title: 'Card Detail',
      transactionCode: 'COCRDSL',
      level: 3,
      isActive: true
    },
    
    [ROUTES.CARD_UPDATE]: {
      path: ROUTES.CARD_UPDATE,
      title: 'Card Update',
      transactionCode: 'COCRDUP',
      level: 4,
      isActive: true
    },
    
    // Transaction Processing Level
    [ROUTES.TRANSACTION_LIST]: {
      path: ROUTES.TRANSACTION_LIST,
      title: 'Transaction List',
      transactionCode: 'COTRN00',
      level: 2,
      isActive: true
    },
    
    [ROUTES.TRANSACTION_SEARCH]: {
      path: ROUTES.TRANSACTION_SEARCH,
      title: 'Transaction Search',
      transactionCode: 'COTRN01',
      level: 3,
      isActive: true
    },
    
    [ROUTES.TRANSACTION_DETAIL]: {
      path: ROUTES.TRANSACTION_DETAIL,
      title: 'Transaction Detail',
      transactionCode: 'COTRN01',
      level: 3,
      isActive: true
    },
    
    [ROUTES.TRANSACTION_ADD]: {
      path: ROUTES.TRANSACTION_ADD,
      title: 'Add Transaction',
      transactionCode: 'COTRN02',
      level: 3,
      isActive: true
    },
    
    // Bill Payment Level
    [ROUTES.BILL_PAYMENT]: {
      path: ROUTES.BILL_PAYMENT,
      title: 'Bill Payment',
      transactionCode: 'COBIL00',
      level: 2,
      isActive: true
    },
    
    // Report Generation Level
    [ROUTES.REPORT_GENERATION]: {
      path: ROUTES.REPORT_GENERATION,
      title: 'Report Generation',
      transactionCode: 'CORPT00',
      level: 2,
      isActive: true
    },
    
    // User Management Level
    [ROUTES.USER_LIST]: {
      path: ROUTES.USER_LIST,
      title: 'User List',
      transactionCode: 'COUSR00',
      level: 2,
      isActive: true
    },
    
    [ROUTES.USER_ADD]: {
      path: ROUTES.USER_ADD,
      title: 'Add User',
      transactionCode: 'COUSR01',
      level: 3,
      isActive: true
    },
    
    [ROUTES.USER_UPDATE]: {
      path: ROUTES.USER_UPDATE,
      title: 'Update User',
      transactionCode: 'COUSR02',
      level: 3,
      isActive: true
    },
    
    [ROUTES.USER_DELETE]: {
      path: ROUTES.USER_DELETE,
      title: 'Delete User',
      transactionCode: 'COUSR03',
      level: 3,
      isActive: true
    }
  } as Record<string, BreadcrumbData>,
  
  /**
   * Screen Title Mapping
   * Maps transaction codes to display titles for consistent UI presentation
   */
  SCREEN_TITLES: {
    COSGN00: 'CardDemo - Login Screen',
    COMEN01: 'CardDemo - Main Menu',
    COADM01: 'CardDemo - Admin Menu',
    COACTVW: 'CardDemo - Account View',
    COACTUP: 'CardDemo - Account Update',
    COCRDLI: 'CardDemo - Card List',
    COCRDSL: 'CardDemo - Card Search',
    COCRDUP: 'CardDemo - Card Update',
    COTRN00: 'CardDemo - Transaction List',
    COTRN01: 'CardDemo - Transaction Search',
    COTRN02: 'CardDemo - Add Transaction',
    COBIL00: 'CardDemo - Bill Payment',
    CORPT00: 'CardDemo - Report Generation',
    COUSR00: 'CardDemo - User List',
    COUSR01: 'CardDemo - Add User',
    COUSR02: 'CardDemo - Update User',
    COUSR03: 'CardDemo - Delete User'
  },
  
  /**
   * Hierarchy Level Definitions
   * Defines navigation depth levels for breadcrumb display
   */
  HIERARCHY_LEVELS: {
    AUTHENTICATION: 0,
    MENU: 1,
    FUNCTION: 2,
    DETAIL: 3,
    MAINTENANCE: 4
  }
} as const;

/**
 * Route Parameter Constants
 * Defines dynamic route parameters for parameterized navigation
 * replacing COMMAREA data passing with URL-based parameter routing
 */
export const ROUTE_PARAMETERS = {
  /**
   * Account Parameters
   * Maps account-related route parameters
   */
  ACCOUNT: {
    ACCOUNT_ID: 'accountId',
    CUSTOMER_ID: 'customerId',
    ACCOUNT_STATUS: 'accountStatus'
  },
  
  /**
   * Card Parameters
   * Maps card-related route parameters
   */
  CARD: {
    CARD_NUMBER: 'cardNumber',
    CARD_TYPE: 'cardType',
    CARD_STATUS: 'cardStatus'
  },
  
  /**
   * Transaction Parameters
   * Maps transaction-related route parameters
   */
  TRANSACTION: {
    TRANSACTION_ID: 'transactionId',
    TRANSACTION_TYPE: 'transactionType',
    DATE_RANGE: 'dateRange'
  },
  
  /**
   * User Parameters
   * Maps user management route parameters
   */
  USER: {
    USER_ID: 'userId',
    USER_TYPE: 'userType',
    USER_STATUS: 'userStatus'
  },
  
  /**
   * Search Parameters
   * Maps search-related route parameters
   */
  SEARCH: {
    SEARCH_TYPE: 'searchType',
    SEARCH_VALUE: 'searchValue',
    PAGE_NUMBER: 'pageNumber',
    PAGE_SIZE: 'pageSize'
  }
} as const;

/**
 * Navigation Utility Functions
 * Helper functions for navigation constant manipulation and validation
 */
export const NavigationUtils = {
  /**
   * Builds breadcrumb trail for a given route path
   * @param currentPath - Current route path
   * @returns Array of breadcrumb data representing navigation trail
   */
  buildBreadcrumbTrail: (currentPath: string): BreadcrumbData[] => {
    const trail: BreadcrumbData[] = [];
    const pathMapping = BREADCRUMB_PATHS.PATH_MAPPING;
    
    // Start with login/authentication
    const loginBreadcrumb = pathMapping[ROUTES.LOGIN];
    if (loginBreadcrumb) {
      trail.push({ ...loginBreadcrumb, isActive: false });
    }
    
    // Add menu level based on current path
    const isAdminPath = currentPath.includes('/admin') || currentPath.includes('/users');
    const menuPath = isAdminPath ? ROUTES.MENU.ADMIN : ROUTES.MENU.MAIN;
    const menuBreadcrumb = pathMapping[menuPath];
    if (menuBreadcrumb) {
      trail.push({ ...menuBreadcrumb, isActive: false });
    }
    
    // Add current path if different from menu
    if (currentPath !== menuPath) {
      const currentBreadcrumb = pathMapping[currentPath];
      if (currentBreadcrumb) {
        trail.push({ ...currentBreadcrumb, isActive: true });
      }
    }
    
    return trail;
  },
  
  /**
   * Gets the return path for a given route (F3 Exit functionality)
   * @param currentPath - Current route path
   * @returns Return destination path
   */
  getReturnPath: (currentPath: string): string => {
    return NAVIGATION_FLOW.RETURN_PATHS[currentPath] || ROUTES.LOGIN;
  },
  
  /**
   * Gets valid navigation destinations from current route
   * @param currentPath - Current route path
   * @returns Array of valid navigation destination paths
   */
  getValidDestinations: (currentPath: string): string[] => {
    const destinations = NAVIGATION_FLOW.XCTL_PATTERNS[currentPath];
    return Array.isArray(destinations) ? destinations : [];
  },
  
  /**
   * Validates if a route path is valid for navigation
   * @param path - Route path to validate
   * @returns Boolean indicating if path is valid
   */
  isValidRoute: (path: string): boolean => {
    const allRoutes = Object.values(ROUTES).flat();
    return allRoutes.includes(path);
  },
  
  /**
   * Extracts route parameters from a path template
   * @param pathTemplate - Path template with parameters
   * @returns Object containing parameter definitions
   */
  extractRouteParameters: (pathTemplate: string): Record<string, string> => {
    const params: Record<string, string> = {};
    const paramMatches = pathTemplate.match(/:([^/]+)/g);
    
    if (paramMatches) {
      paramMatches.forEach(match => {
        const paramName = match.slice(1); // Remove the ':'
        params[paramName] = paramName;
      });
    }
    
    return params;
  }
};

/**
 * Export default consolidated navigation constants
 */
export default {
  ROUTES,
  NAVIGATION_FLOW,
  BREADCRUMB_PATHS,
  ROUTE_PARAMETERS,
  NavigationUtils
};