/**
 * CardDemo - Navigation Constants
 * 
 * TypeScript constants file defining React Router navigation paths and routing configuration
 * extracted from BMS screen flow patterns. Maps original CICS XCTL navigation to modern
 * declarative routing while preserving user experience and security hierarchies.
 * 
 * This file provides centralized navigation configuration for consistent routing across
 * all React components, maintaining exact functional equivalence with original CICS
 * transaction flow patterns and pseudo-conversational processing behavior.
 * 
 * Key CICS Navigation Patterns Preserved:
 * - Transaction-based routing with 4-character transaction code mapping
 * - Pseudo-conversational flow using React Router state management
 * - Function key navigation (F3=Exit, F7/F8=Pagination, F12=Cancel)
 * - Breadcrumb trail navigation for user orientation and back functionality
 * - Role-based menu access matching RACF security patterns
 * - XCTL program transfer patterns using React Router navigation
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

import { TRANSACTION_ENDPOINTS } from './TransactionConstants';
import { BreadcrumbData } from '../types/NavigationTypes';

/**
 * Primary Route Definitions
 * 
 * Maps each BMS screen to its corresponding React Router path, preserving exact
 * navigation flow from original CICS transaction patterns. Each route corresponds
 * to a specific BMS mapset and maintains transaction code traceability.
 * 
 * Route Structure Analysis from BMS Files:
 * - COSGN00.bms -> /login (entry point with USERID/PASSWD fields)
 * - COMEN01.bms -> /menu (main menu with 12 option slots)
 * - COACTVW.bms -> /account/view (account display with customer details)
 * - COACTUP.bms -> /account/update (account modification form)
 * - COCRDLI.bms -> /card/list (card listing with pagination)
 * - All paths preserve original transaction semantics for audit compliance
 */
export const ROUTES = {
  // Authentication and Session Management Routes
  LOGIN: '/login',                           // COSGN00 - Sign-on screen with authentication
  LOGOUT: '/logout',                         // Session termination and cleanup
  SESSION_TIMEOUT: '/session-timeout',       // Automatic session expiration handling
  
  // Main Navigation Routes - User Level
  MENU: '/menu',                            // COMEN01 - Main menu navigation hub
  DASHBOARD: '/dashboard',                   // User dashboard with summary information
  
  // Account Management Routes
  ACCOUNT_VIEW: '/account/view',            // COACTVW - Account viewing with customer details
  ACCOUNT_UPDATE: '/account/update',        // COACTUP - Account information modification
  ACCOUNT_VIEW_BY_ID: '/account/view/:accountId',  // Dynamic account access
  ACCOUNT_UPDATE_BY_ID: '/account/update/:accountId', // Dynamic account modification
  
  // Card Management Routes
  CARD_LIST: '/card/list',                  // COCRDLI - Credit card listing display
  CARD_SEARCH: '/card/search',              // COCRDSL - Card search functionality
  CARD_UPDATE: '/card/update',              // COCRDUP - Card information updates
  CARD_VIEW_BY_NUMBER: '/card/view/:cardNumber',      // Dynamic card access
  CARD_UPDATE_BY_NUMBER: '/card/update/:cardNumber',  // Dynamic card modification
  
  // Transaction Management Routes
  TRANSACTION_LIST: '/transaction/list',     // COTRN00 - Transaction history display
  TRANSACTION_SEARCH: '/transaction/search', // COTRN01 - Transaction search and filtering
  TRANSACTION_ADD: '/transaction/add',       // COTRN02 - New transaction creation
  TRANSACTION_VIEW_BY_ID: '/transaction/view/:transactionId', // Dynamic transaction access
  
  // Billing and Payment Routes
  BILL_PAYMENT: '/billing/payment',         // COBIL00 - Bill payment processing
  BILLING_HISTORY: '/billing/history',      // Payment history and records
  
  // Reporting Routes
  REPORTS: '/reports',                      // CORPT00 - Report generation hub
  ACCOUNT_STATEMENT: '/reports/statement',   // Account statement generation
  TRANSACTION_REPORT: '/reports/transactions', // Transaction activity reports
  
  // Administrative Routes - Admin Role Required
  ADMIN_MENU: '/admin',                     // COADM01 - Administrative menu hub
  USER_LIST: '/user/list',                  // COUSR00 - User account listing
  USER_CREATE: '/user/create',              // COUSR01 - New user creation
  USER_UPDATE: '/user/update',              // COUSR02 - User account modification
  USER_SEARCH: '/user/search',              // COUSR03 - User search functionality
  USER_VIEW_BY_ID: '/user/view/:userId',    // Dynamic user account access
  USER_UPDATE_BY_ID: '/user/update/:userId', // Dynamic user modification
  
  // System and Error Routes
  ERROR: '/error',                          // General error display page
  NOT_FOUND: '/not-found',                  // 404 error for invalid routes
  UNAUTHORIZED: '/unauthorized',            // 403 error for access violations
  
  // Exit Paths - Matching CICS F3=Exit Functionality
  EXIT_PATHS: {
    LOGIN_EXIT: '/',                        // Exit from login returns to root
    MENU_EXIT: '/login',                    // F3 from menu signs user out
    SUB_SCREEN_EXIT: '/menu',               // F3 from sub-screens returns to menu
    ADMIN_EXIT: '/menu',                    // F3 from admin returns to main menu
    CANCEL_OPERATION: 'PREVIOUS_SCREEN'     // F12 cancel returns to previous screen
  }
} as const;

/**
 * Navigation Flow Configuration
 * 
 * Defines CICS XCTL program transfer patterns using React Router navigation.
 * Preserves exact pseudo-conversational flow and transaction boundaries while
 * enabling modern single-page application user experience.
 * 
 * XCTL Pattern Analysis:
 * - Each navigation preserves session state equivalent to COMMAREA passing
 * - Transaction boundaries maintained for audit logging and security
 * - Return path management matches CICS RETURN TRANSID behavior
 * - Menu hierarchy enforced through role-based access control
 */
export const NAVIGATION_FLOW = {
  // CICS XCTL Transfer Patterns - Program Control Flow
  XCTL_PATTERNS: {
    // Authentication Flow - Entry Point Control
    FROM_ROOT: [ROUTES.LOGIN],                    // Application entry -> Login
    FROM_LOGIN: [ROUTES.MENU, ROUTES.ADMIN_MENU], // Successful auth -> Menu (role-based)
    
    // Main Menu Navigation - Primary Transaction Flow
    FROM_MENU: [
      ROUTES.ACCOUNT_VIEW,                        // Menu option 1 -> Account View
      ROUTES.CARD_LIST,                          // Menu option 2 -> Card List
      ROUTES.TRANSACTION_LIST,                   // Menu option 3 -> Transaction List
      ROUTES.BILL_PAYMENT,                       // Menu option 4 -> Bill Payment
      ROUTES.REPORTS,                            // Menu option 5 -> Reports
      ROUTES.ADMIN_MENU,                         // Menu option 6 -> Admin (if authorized)
      ROUTES.LOGOUT                              // Menu exit -> Logout
    ],
    
    // Administrative Flow - Admin Role Navigation
    FROM_ADMIN_MENU: [
      ROUTES.USER_LIST,                          // Admin option 1 -> User List
      ROUTES.USER_CREATE,                        // Admin option 2 -> Create User
      ROUTES.USER_UPDATE,                        // Admin option 3 -> Update User
      ROUTES.USER_SEARCH,                        // Admin option 4 -> Search Users
      ROUTES.MENU                                // Admin option 5 -> Return to Main Menu
    ],
    
    // Account Management Flow - Account Operations
    FROM_ACCOUNT_VIEW: [
      ROUTES.ACCOUNT_UPDATE,                     // View -> Update account
      ROUTES.CARD_LIST,                          // View -> Related cards
      ROUTES.TRANSACTION_LIST,                   // View -> Account transactions
      ROUTES.MENU                                // F3=Exit -> Main Menu
    ],
    
    FROM_ACCOUNT_UPDATE: [
      ROUTES.ACCOUNT_VIEW,                       // Update success -> View updated account
      ROUTES.MENU                                // F3=Exit/Cancel -> Main Menu
    ],
    
    // Card Management Flow - Card Operations
    FROM_CARD_LIST: [
      ROUTES.CARD_UPDATE,                        // List -> Update selected card
      ROUTES.CARD_SEARCH,                        // List -> Search cards
      ROUTES.TRANSACTION_LIST,                   // List -> Card transactions
      ROUTES.ACCOUNT_VIEW,                       // List -> Parent account
      ROUTES.MENU                                // F3=Exit -> Main Menu
    ],
    
    FROM_CARD_SEARCH: [
      ROUTES.CARD_LIST,                          // Search results -> Card list
      ROUTES.CARD_UPDATE,                        // Search -> Update found card
      ROUTES.MENU                                // F3=Exit -> Main Menu
    ],
    
    FROM_CARD_UPDATE: [
      ROUTES.CARD_LIST,                          // Update success -> Card list
      ROUTES.ACCOUNT_VIEW,                       // Update -> Parent account
      ROUTES.MENU                                // F3=Exit/Cancel -> Main Menu
    ],
    
    // Transaction Management Flow - Transaction Operations
    FROM_TRANSACTION_LIST: [
      ROUTES.TRANSACTION_SEARCH,                 // List -> Search transactions
      ROUTES.TRANSACTION_ADD,                    // List -> Add new transaction
      ROUTES.ACCOUNT_VIEW,                       // List -> Parent account
      ROUTES.CARD_LIST,                          // List -> Related cards
      ROUTES.MENU                                // F3=Exit -> Main Menu
    ],
    
    FROM_TRANSACTION_SEARCH: [
      ROUTES.TRANSACTION_LIST,                   // Search results -> Transaction list
      ROUTES.TRANSACTION_ADD,                    // Search -> Add transaction
      ROUTES.MENU                                // F3=Exit -> Main Menu
    ],
    
    FROM_TRANSACTION_ADD: [
      ROUTES.TRANSACTION_LIST,                   // Add success -> Transaction list
      ROUTES.MENU                                // F3=Exit/Cancel -> Main Menu
    ],
    
    // User Management Flow - Administrative User Operations
    FROM_USER_LIST: [
      ROUTES.USER_CREATE,                        // List -> Create new user
      ROUTES.USER_UPDATE,                        // List -> Update selected user
      ROUTES.USER_SEARCH,                        // List -> Search users
      ROUTES.ADMIN_MENU                          // F3=Exit -> Admin Menu
    ],
    
    FROM_USER_CREATE: [
      ROUTES.USER_LIST,                          // Create success -> User list
      ROUTES.ADMIN_MENU                          // F3=Exit/Cancel -> Admin Menu
    ],
    
    FROM_USER_UPDATE: [
      ROUTES.USER_LIST,                          // Update success -> User list
      ROUTES.ADMIN_MENU                          // F3=Exit/Cancel -> Admin Menu
    ],
    
    FROM_USER_SEARCH: [
      ROUTES.USER_LIST,                          // Search results -> User list
      ROUTES.USER_UPDATE,                        // Search -> Update found user
      ROUTES.ADMIN_MENU                          // F3=Exit -> Admin Menu
    ]
  },
  
  // Return Path Management - F3=Exit and Cancel Operations
  RETURN_PATHS: {
    // Standard F3=Exit Paths
    DEFAULT_EXIT: ROUTES.MENU,                   // Default F3 destination
    LOGIN_EXIT: ROUTES.LOGIN,                    // Session timeout destination
    ADMIN_EXIT: ROUTES.ADMIN_MENU,              // Admin screen F3 destination
    
    // Cancel Operation Paths - F12=Cancel
    CANCEL_ACCOUNT_UPDATE: ROUTES.ACCOUNT_VIEW,  // Cancel account changes
    CANCEL_CARD_UPDATE: ROUTES.CARD_LIST,        // Cancel card changes
    CANCEL_TRANSACTION_ADD: ROUTES.TRANSACTION_LIST, // Cancel transaction add
    CANCEL_USER_CREATE: ROUTES.USER_LIST,        // Cancel user creation
    CANCEL_USER_UPDATE: ROUTES.USER_LIST,        // Cancel user changes
    
    // Error Recovery Paths
    ERROR_RECOVERY: ROUTES.MENU,                 // General error recovery
    SESSION_RECOVERY: ROUTES.LOGIN,              // Session error recovery
    AUTH_FAILURE: ROUTES.LOGIN                   // Authentication failure recovery
  },
  
  // Menu Hierarchy - Role-Based Navigation Structure
  MENU_HIERARCHY: {
    // Main User Menu Structure (COMEN01.bms)
    MAIN_MENU: {
      level: 1,
      parent: null,
      title: 'Main Menu',
      children: [
        { option: '1', path: ROUTES.ACCOUNT_VIEW, title: 'Account View' },
        { option: '2', path: ROUTES.CARD_LIST, title: 'List Credit Cards' },
        { option: '3', path: ROUTES.TRANSACTION_LIST, title: 'List Transactions' },
        { option: '4', path: ROUTES.BILL_PAYMENT, title: 'Bill Payment' },
        { option: '5', path: ROUTES.REPORTS, title: 'Report Generation' },
        { option: '6', path: ROUTES.ADMIN_MENU, title: 'Administrative Menu', requiresAdmin: true }
      ]
    },
    
    // Administrative Menu Structure (COADM01.bms)
    ADMIN_MENU: {
      level: 1,
      parent: ROUTES.MENU,
      title: 'Administrative Menu',
      requiresAdmin: true,
      children: [
        { option: '1', path: ROUTES.USER_LIST, title: 'User List' },
        { option: '2', path: ROUTES.USER_CREATE, title: 'Create User' },
        { option: '3', path: ROUTES.USER_UPDATE, title: 'Update User' },
        { option: '4', path: ROUTES.USER_SEARCH, title: 'Search Users' },
        { option: '5', path: ROUTES.MENU, title: 'Return to Main Menu' }
      ]
    },
    
    // Sub-Menu Hierarchies
    ACCOUNT_SUBMENU: {
      level: 2,
      parent: ROUTES.MENU,
      title: 'Account Management',
      children: [
        { path: ROUTES.ACCOUNT_VIEW, title: 'View Account Details' },
        { path: ROUTES.ACCOUNT_UPDATE, title: 'Update Account Information' }
      ]
    },
    
    CARD_SUBMENU: {
      level: 2,
      parent: ROUTES.MENU,
      title: 'Card Management',
      children: [
        { path: ROUTES.CARD_LIST, title: 'List Credit Cards' },
        { path: ROUTES.CARD_SEARCH, title: 'Search Credit Cards' },
        { path: ROUTES.CARD_UPDATE, title: 'Update Credit Card' }
      ]
    },
    
    TRANSACTION_SUBMENU: {
      level: 2,
      parent: ROUTES.MENU,
      title: 'Transaction Management',
      children: [
        { path: ROUTES.TRANSACTION_LIST, title: 'List Transactions' },
        { path: ROUTES.TRANSACTION_SEARCH, title: 'Search Transactions' },
        { path: ROUTES.TRANSACTION_ADD, title: 'Add Transaction' }
      ]
    },
    
    USER_MANAGEMENT_SUBMENU: {
      level: 2,
      parent: ROUTES.ADMIN_MENU,
      title: 'User Management',
      requiresAdmin: true,
      children: [
        { path: ROUTES.USER_LIST, title: 'List Users' },
        { path: ROUTES.USER_CREATE, title: 'Create New User' },
        { path: ROUTES.USER_UPDATE, title: 'Update User Information' },
        { path: ROUTES.USER_SEARCH, title: 'Search Users' }
      ]
    }
  }
} as const;

/**
 * Breadcrumb Navigation Configuration
 * 
 * Provides hierarchical navigation path tracking for user orientation and F3=Exit
 * functionality. Maintains exact CICS screen hierarchy while enabling modern
 * breadcrumb navigation patterns in React interface.
 * 
 * Breadcrumb Structure Analysis:
 * - Level 0: Root/Home (login screen)
 * - Level 1: Main navigation hubs (menu, admin menu)  
 * - Level 2: Functional screens (account view, card list, etc.)
 * - Level 3: Detail/edit screens (account update, card update, etc.)
 * - Active tracking supports visual feedback and navigation state
 */
export const BREADCRUMB_PATHS = {
  // Path to Breadcrumb Mapping - Route Resolution
  PATH_MAPPING: {
    [ROUTES.LOGIN]: {
      path: ROUTES.LOGIN,
      title: 'Sign On',
      transactionCode: 'COSGN00',
      level: 0,
      isActive: false
    } as BreadcrumbData,
    
    [ROUTES.MENU]: {
      path: ROUTES.MENU,
      title: 'Main Menu',
      transactionCode: 'COMEN01',
      level: 1,
      isActive: false
    } as BreadcrumbData,
    
    [ROUTES.ADMIN_MENU]: {
      path: ROUTES.ADMIN_MENU,
      title: 'Administrative Menu',
      transactionCode: 'COADM01',
      level: 1,
      isActive: false
    } as BreadcrumbData,
    
    [ROUTES.ACCOUNT_VIEW]: {
      path: ROUTES.ACCOUNT_VIEW,
      title: 'View Account',
      transactionCode: 'COACTVW',
      level: 2,
      isActive: false
    } as BreadcrumbData,
    
    [ROUTES.ACCOUNT_UPDATE]: {
      path: ROUTES.ACCOUNT_UPDATE,
      title: 'Update Account',
      transactionCode: 'COACTUP',
      level: 3,
      isActive: false
    } as BreadcrumbData,
    
    [ROUTES.CARD_LIST]: {
      path: ROUTES.CARD_LIST,
      title: 'List Credit Cards',
      transactionCode: 'COCRDLI',
      level: 2,
      isActive: false
    } as BreadcrumbData,
    
    [ROUTES.CARD_SEARCH]: {
      path: ROUTES.CARD_SEARCH,
      title: 'Search Credit Cards',
      transactionCode: 'COCRDSL',
      level: 2,
      isActive: false
    } as BreadcrumbData,
    
    [ROUTES.CARD_UPDATE]: {
      path: ROUTES.CARD_UPDATE,
      title: 'Update Credit Card',
      transactionCode: 'COCRDUP',
      level: 3,
      isActive: false
    } as BreadcrumbData,
    
    [ROUTES.TRANSACTION_LIST]: {
      path: ROUTES.TRANSACTION_LIST,
      title: 'List Transactions',
      transactionCode: 'COTRN00',
      level: 2,
      isActive: false
    } as BreadcrumbData,
    
    [ROUTES.TRANSACTION_SEARCH]: {
      path: ROUTES.TRANSACTION_SEARCH,
      title: 'Search Transactions',
      transactionCode: 'COTRN01',
      level: 2,
      isActive: false
    } as BreadcrumbData,
    
    [ROUTES.TRANSACTION_ADD]: {
      path: ROUTES.TRANSACTION_ADD,
      title: 'Add Transaction',
      transactionCode: 'COTRN02',
      level: 3,
      isActive: false
    } as BreadcrumbData,
    
    [ROUTES.BILL_PAYMENT]: {
      path: ROUTES.BILL_PAYMENT,
      title: 'Bill Payment',
      transactionCode: 'COBIL00',
      level: 2,
      isActive: false
    } as BreadcrumbData,
    
    [ROUTES.REPORTS]: {
      path: ROUTES.REPORTS,
      title: 'Report Generation',
      transactionCode: 'CORPT00',
      level: 2,
      isActive: false
    } as BreadcrumbData,
    
    [ROUTES.USER_LIST]: {
      path: ROUTES.USER_LIST,
      title: 'User List',
      transactionCode: 'COUSR00',
      level: 2,
      isActive: false
    } as BreadcrumbData,
    
    [ROUTES.USER_CREATE]: {
      path: ROUTES.USER_CREATE,
      title: 'Create User',
      transactionCode: 'COUSR01',
      level: 3,
      isActive: false
    } as BreadcrumbData,
    
    [ROUTES.USER_UPDATE]: {
      path: ROUTES.USER_UPDATE,
      title: 'Update User',
      transactionCode: 'COUSR02',
      level: 3,
      isActive: false
    } as BreadcrumbData,
    
    [ROUTES.USER_SEARCH]: {
      path: ROUTES.USER_SEARCH,
      title: 'Search Users',
      transactionCode: 'COUSR03',
      level: 2,
      isActive: false
    } as BreadcrumbData
  },
  
  // Screen Titles - BMS TITLE01/TITLE02 Field Mapping
  SCREEN_TITLES: {
    LOGIN_SCREEN: 'CardDemo - Sign On',
    MAIN_MENU_SCREEN: 'Main Menu',
    ADMIN_MENU_SCREEN: 'Administrative Menu',
    ACCOUNT_VIEW_SCREEN: 'View Account',
    ACCOUNT_UPDATE_SCREEN: 'Update Account',
    CARD_LIST_SCREEN: 'List Credit Cards',
    CARD_SEARCH_SCREEN: 'Search Credit Cards',
    CARD_UPDATE_SCREEN: 'Update Credit Card',
    TRANSACTION_LIST_SCREEN: 'List Transactions',
    TRANSACTION_SEARCH_SCREEN: 'Search Transactions',
    TRANSACTION_ADD_SCREEN: 'Add Transaction',
    BILL_PAYMENT_SCREEN: 'Bill Payment',
    REPORT_GENERATION_SCREEN: 'Report Generation',
    USER_LIST_SCREEN: 'User List',
    USER_CREATE_SCREEN: 'Create User',
    USER_UPDATE_SCREEN: 'Update User',
    USER_SEARCH_SCREEN: 'Search Users'
  },
  
  // Hierarchy Levels - Navigation Depth Configuration
  HIERARCHY_LEVELS: {
    ROOT_LEVEL: 0,              // Login/Authentication level
    MENU_LEVEL: 1,              // Main/Admin menu level
    FUNCTION_LEVEL: 2,          // Primary function screens
    DETAIL_LEVEL: 3,            // Detail/edit screens
    MAX_BREADCRUMB_DEPTH: 4     // Maximum supported navigation depth
  }
} as const;

/**
 * Route Parameter Constants
 * 
 * Defines dynamic route parameters for data passing between screens, replacing
 * CICS COMMAREA functionality with modern URL-based parameter passing while
 * maintaining data type safety and validation.
 * 
 * Parameter Analysis from BMS Screens:
 * - accountId: Account number from COACTVW.bms ACCTSID field (11 digits)
 * - cardNumber: Card number for card operations (16 digits with validation)
 * - transactionId: Transaction identifier for transaction details
 * - userId: User identifier for administrative operations
 */
export const ROUTE_PARAMETERS = {
  // Account Management Parameters
  ACCOUNT_ID: 'accountId',                  // 11-digit account number parameter
  CUSTOMER_ID: 'customerId',                // Customer identifier parameter
  
  // Card Management Parameters  
  CARD_NUMBER: 'cardNumber',                // 16-digit card number parameter
  CARD_ID: 'cardId',                        // Internal card identifier
  
  // Transaction Management Parameters
  TRANSACTION_ID: 'transactionId',          // Unique transaction identifier
  REFERENCE_NUMBER: 'referenceNumber',      // Transaction reference number
  
  // User Management Parameters (Admin)
  USER_ID: 'userId',                        // User account identifier
  USERNAME: 'username',                     // Login username parameter
  
  // Search and Filter Parameters
  SEARCH_TERM: 'searchTerm',                // General search parameter
  FILTER_TYPE: 'filterType',                // Search filter type
  PAGE_NUMBER: 'page',                      // Pagination page number
  PAGE_SIZE: 'size',                        // Pagination page size
  SORT_FIELD: 'sortBy',                     // Sort field parameter
  SORT_ORDER: 'order',                      // Sort order (asc/desc)
  
  // Date Range Parameters
  START_DATE: 'startDate',                  // Date range start
  END_DATE: 'endDate',                      // Date range end
  
  // Session and Navigation Parameters
  RETURN_PATH: 'returnPath',                // F3=Exit return destination
  TRANSACTION_CODE: 'transactionCode',      // Original CICS transaction code
  SESSION_ID: 'sessionId',                  // Session tracking identifier
  
  // Parameter Patterns for Route Matching
  PATTERNS: {
    ACCOUNT_ID_PATTERN: ':accountId(\\d{11})',           // 11-digit account validation
    CARD_NUMBER_PATTERN: ':cardNumber(\\d{16})',         // 16-digit card validation
    TRANSACTION_ID_PATTERN: ':transactionId([A-Z0-9]+)', // Alphanumeric transaction ID
    USER_ID_PATTERN: ':userId([A-Z0-9]{1,8})',          // 1-8 character user ID
    DATE_PATTERN: ':date(\\d{4}-\\d{2}-\\d{2})',        // YYYY-MM-DD date format
    PAGE_PATTERN: ':page(\\d+)',                         // Positive integer pages
    SORT_PATTERN: ':sortBy([a-zA-Z_]+)',                 // Field name sorting
    ORDER_PATTERN: ':order(asc|desc)'                    // Sort order validation
  }
} as const;

// Type definitions for navigation constants
export type RouteKey = keyof typeof ROUTES;
export type NavigationFlowKey = keyof typeof NAVIGATION_FLOW.XCTL_PATTERNS;
export type BreadcrumbPathKey = keyof typeof BREADCRUMB_PATHS.PATH_MAPPING;
export type RouteParameterKey = keyof typeof ROUTE_PARAMETERS;
export type ScreenTitleKey = keyof typeof BREADCRUMB_PATHS.SCREEN_TITLES;
export type HierarchyLevelKey = keyof typeof BREADCRUMB_PATHS.HIERARCHY_LEVELS;

// Utility functions for navigation operations
export const getRouteByTransactionCode = (transactionCode: string): string | null => {
  const transactionToRouteMap: Record<string, string> = {
    'COSGN00': ROUTES.LOGIN,
    'COMEN01': ROUTES.MENU,
    'COACTVW': ROUTES.ACCOUNT_VIEW,
    'COACTUP': ROUTES.ACCOUNT_UPDATE,
    'COCRDLI': ROUTES.CARD_LIST,
    'COCRDSL': ROUTES.CARD_SEARCH,
    'COCRDUP': ROUTES.CARD_UPDATE,
    'COTRN00': ROUTES.TRANSACTION_LIST,
    'COTRN01': ROUTES.TRANSACTION_SEARCH,
    'COTRN02': ROUTES.TRANSACTION_ADD,
    'COBIL00': ROUTES.BILL_PAYMENT,
    'CORPT00': ROUTES.REPORTS,
    'COADM01': ROUTES.ADMIN_MENU,
    'COUSR00': ROUTES.USER_LIST,
    'COUSR01': ROUTES.USER_CREATE,
    'COUSR02': ROUTES.USER_UPDATE,
    'COUSR03': ROUTES.USER_SEARCH
  };
  
  return transactionToRouteMap[transactionCode] || null;
};

export const getBreadcrumbForRoute = (route: string): BreadcrumbData | null => {
  return BREADCRUMB_PATHS.PATH_MAPPING[route as keyof typeof BREADCRUMB_PATHS.PATH_MAPPING] || null;
};

export const getExitPathForRoute = (currentRoute: string): string => {
  // Determine appropriate F3=Exit destination based on current route
  if (currentRoute.startsWith('/admin')) {
    return NAVIGATION_FLOW.RETURN_PATHS.ADMIN_EXIT;
  } else if (currentRoute === ROUTES.LOGIN) {
    return ROUTES.EXIT_PATHS.LOGIN_EXIT;
  } else if (currentRoute === ROUTES.MENU) {
    return ROUTES.EXIT_PATHS.MENU_EXIT;
  } else {
    return NAVIGATION_FLOW.RETURN_PATHS.DEFAULT_EXIT;
  }
};

export const isAdminRoute = (route: string): boolean => {
  const adminRoutes = [
    ROUTES.ADMIN_MENU,
    ROUTES.USER_LIST,
    ROUTES.USER_CREATE,
    ROUTES.USER_UPDATE,
    ROUTES.USER_SEARCH
  ];
  
  return adminRoutes.includes(route) || route.includes('/admin') || route.includes('/user');
};

export const getMenuOptionsForRole = (userRole: 'user' | 'admin' | 'guest') => {
  if (userRole === 'admin') {
    return NAVIGATION_FLOW.MENU_HIERARCHY.MAIN_MENU.children;
  } else if (userRole === 'user') {
    return NAVIGATION_FLOW.MENU_HIERARCHY.MAIN_MENU.children.filter(option => !option.requiresAdmin);
  } else {
    return []; // Guest users have no menu access
  }
};

export const buildRouteWithParameters = (route: string, parameters: Record<string, string | number>): string => {
  let buildRoute = route;
  
  Object.entries(parameters).forEach(([key, value]) => {
    const paramPattern = `:${key}`;
    if (buildRoute.includes(paramPattern)) {
      buildRoute = buildRoute.replace(paramPattern, String(value));
    }
  });
  
  return buildRoute;
};

// Export all navigation constants as default for convenient importing
export default {
  ROUTES,
  NAVIGATION_FLOW,
  BREADCRUMB_PATHS,
  ROUTE_PARAMETERS,
  getRouteByTransactionCode,
  getBreadcrumbForRoute,
  getExitPathForRoute,
  isAdminRoute,
  getMenuOptionsForRole,
  buildRouteWithParameters
};