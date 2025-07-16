/**
 * Transaction Constants for CardDemo Application
 * 
 * This file provides centralized transaction-related identifiers extracted from 
 * BMS mapsets ensuring traceability between original CICS transactions and React 
 * components while enabling type-safe transaction handling.
 * 
 * Each constant preserves exact CICS transaction codes and screen titles for 
 * audit traceability and seamless integration between legacy mainframe patterns 
 * and modern React-based user interface components.
 */

/**
 * Transaction API Endpoints
 * Maps BMS transaction codes to their corresponding REST API endpoints
 * following the service-per-transaction microservices architecture
 */
export const TRANSACTION_ENDPOINTS = {
  LOGIN: '/api/auth/login',
  MENU: '/api/menu/main',
  ACCOUNT_VIEW: '/api/accounts/view',
  ACCOUNT_UPDATE: '/api/accounts/update',
  CARD_LIST: '/api/cards/list',
  TRANSACTION_LIST: '/api/transactions/list',
  USER_MANAGEMENT: '/api/users/management'
} as const;

/**
 * API Base Paths for Microservices
 * Defines base URL paths for each Spring Boot microservice
 * supporting the distributed transaction processing architecture
 */
export const API_BASE_PATHS = {
  AUTH_SERVICE: '/api/auth',
  ACCOUNT_SERVICE: '/api/accounts',
  CARD_SERVICE: '/api/cards',
  TRANSACTION_SERVICE: '/api/transactions',
  USER_SERVICE: '/api/users'
} as const;

/**
 * Transaction Codes
 * Preserves exact CICS transaction identifiers from BMS mapsets
 * for complete audit traceability and system integration
 */
export const TRANSACTION_CODES = {
  // Authentication Transaction
  COSGN00: 'COSGN00',
  
  // Menu Navigation Transactions  
  COMEN01: 'COMEN01',
  COADM01: 'COADM01',
  
  // Account Management Transactions
  COACTVW: 'COACTVW',
  COACTUP: 'COACTUP',
  
  // Card Management Transactions
  COCRDLI: 'COCRDLI',
  COCRDSL: 'COCRDSL',
  COCRDUP: 'COCRDUP',
  
  // Transaction Processing
  COTRN00: 'COTRN00',
  COTRN01: 'COTRN01',
  COTRN02: 'COTRN02',
  
  // Bill Payment and Reporting
  COBIL00: 'COBIL00',
  CORPT00: 'CORPT00',
  
  // User Management Transactions
  COUSR00: 'COUSR00',
  COUSR01: 'COUSR01',
  COUSR02: 'COUSR02',
  COUSR03: 'COUSR03'
} as const;

/**
 * Screen Titles
 * Extracted from BMS mapset headers providing consistent display titles
 * for React component headers and navigation breadcrumbs
 */
export const SCREEN_TITLES = {
  LOGIN_TITLE: 'CardDemo - Login Screen',
  MAIN_MENU_TITLE: 'CardDemo - Main Menu Screen',
  ACCOUNT_VIEW_TITLE: 'CardDemo - Account Viewer Screen',
  ACCOUNT_UPDATE_TITLE: 'CardDemo - Account Update Screen',
  CARD_LIST_TITLE: 'CardDemo - Card Listing Screen',
  CARD_SEARCH_TITLE: 'CardDemo - Card Selection Screen',
  CARD_UPDATE_TITLE: 'CardDemo - Card Update Screen',
  TRANSACTION_LIST_TITLE: 'CardDemo - Transaction List',
  TRANSACTION_SEARCH_TITLE: 'CardDemo - Transaction View',
  ADD_TRANSACTION_TITLE: 'CardDemo - Transaction Add',
  BILL_PAYMENT_TITLE: 'CardDemo - Bill Payment Screen',
  REPORT_GENERATION_TITLE: 'CardDemo - Report Generation Screen',
  ADMIN_MENU_TITLE: 'CardDemo - Admin Menu Screen',
  USER_LIST_TITLE: 'CardDemo - List Users',
  USER_CREATE_TITLE: 'CardDemo - Add User',
  USER_UPDATE_TITLE: 'CardDemo - Update User',
  USER_SEARCH_TITLE: 'CardDemo - Delete User'
} as const;

/**
 * Component Mappings
 * Links BMS mapsets to their corresponding React component file paths
 * enabling consistent component resolution and lazy loading
 */
export const COMPONENT_MAPPINGS = {
  LOGIN_COMPONENT: '/components/auth/LoginComponent',
  MAIN_MENU_COMPONENT: '/components/menu/MainMenuComponent',
  ACCOUNT_VIEW_COMPONENT: '/components/account/AccountViewComponent',
  ACCOUNT_UPDATE_COMPONENT: '/components/account/AccountUpdateComponent',
  CARD_LIST_COMPONENT: '/components/card/CardListComponent',
  CARD_SEARCH_COMPONENT: '/components/card/CardSearchComponent',
  CARD_UPDATE_COMPONENT: '/components/card/CardUpdateComponent',
  TRANSACTION_LIST_COMPONENT: '/components/transaction/TransactionListComponent',
  TRANSACTION_SEARCH_COMPONENT: '/components/transaction/TransactionSearchComponent',
  ADD_TRANSACTION_COMPONENT: '/components/transaction/AddTransactionComponent',
  BILL_PAYMENT_COMPONENT: '/components/payment/BillPaymentComponent',
  REPORT_GENERATION_COMPONENT: '/components/report/ReportGenerationComponent',
  ADMIN_MENU_COMPONENT: '/components/admin/AdminMenuComponent',
  USER_LIST_COMPONENT: '/components/user/UserListComponent',
  USER_CREATE_COMPONENT: '/components/user/UserCreateComponent',
  USER_UPDATE_COMPONENT: '/components/user/UserUpdateComponent',
  USER_SEARCH_COMPONENT: '/components/user/UserSearchComponent'
} as const;

/**
 * Transaction Flow
 * Preserves original CICS XCTL navigation patterns for React Router
 * ensuring consistent navigation flow and user experience
 */
export const TRANSACTION_FLOW = {
  /**
   * XCTL Patterns
   * Maps original CICS XCTL transaction linkages to React Router navigation
   */
  XCTL_PATTERNS: {
    // Authentication flow
    [TRANSACTION_CODES.COSGN00]: [TRANSACTION_CODES.COMEN01, TRANSACTION_CODES.COADM01],
    
    // Main menu navigation
    [TRANSACTION_CODES.COMEN01]: [
      TRANSACTION_CODES.COACTVW,
      TRANSACTION_CODES.COCRDLI,
      TRANSACTION_CODES.COTRN00,
      TRANSACTION_CODES.COBIL00,
      TRANSACTION_CODES.CORPT00
    ],
    
    // Admin menu navigation
    [TRANSACTION_CODES.COADM01]: [
      TRANSACTION_CODES.COUSR00,
      TRANSACTION_CODES.COUSR01,
      TRANSACTION_CODES.COUSR02,
      TRANSACTION_CODES.COUSR03
    ],
    
    // Account management flow
    [TRANSACTION_CODES.COACTVW]: [TRANSACTION_CODES.COACTUP],
    
    // Card management flow
    [TRANSACTION_CODES.COCRDLI]: [TRANSACTION_CODES.COCRDSL, TRANSACTION_CODES.COCRDUP],
    [TRANSACTION_CODES.COCRDSL]: [TRANSACTION_CODES.COCRDUP],
    
    // Transaction processing flow
    [TRANSACTION_CODES.COTRN00]: [TRANSACTION_CODES.COTRN01, TRANSACTION_CODES.COTRN02],
    [TRANSACTION_CODES.COTRN01]: [TRANSACTION_CODES.COTRN02],
    
    // User management flow
    [TRANSACTION_CODES.COUSR00]: [TRANSACTION_CODES.COUSR01, TRANSACTION_CODES.COUSR02, TRANSACTION_CODES.COUSR03],
    [TRANSACTION_CODES.COUSR01]: [TRANSACTION_CODES.COUSR00],
    [TRANSACTION_CODES.COUSR02]: [TRANSACTION_CODES.COUSR00],
    [TRANSACTION_CODES.COUSR03]: [TRANSACTION_CODES.COUSR00]
  },
  
  /**
   * Navigation Sequences
   * Defines standard navigation sequences for common user workflows
   */
  NAVIGATION_SEQUENCES: {
    // Standard user workflow
    STANDARD_USER_FLOW: [
      TRANSACTION_CODES.COSGN00,
      TRANSACTION_CODES.COMEN01,
      TRANSACTION_CODES.COACTVW,
      TRANSACTION_CODES.COCRDLI,
      TRANSACTION_CODES.COTRN00
    ],
    
    // Administrative workflow
    ADMIN_WORKFLOW: [
      TRANSACTION_CODES.COSGN00,
      TRANSACTION_CODES.COADM01,
      TRANSACTION_CODES.COUSR00,
      TRANSACTION_CODES.COUSR01
    ],
    
    // Account management workflow
    ACCOUNT_MANAGEMENT: [
      TRANSACTION_CODES.COACTVW,
      TRANSACTION_CODES.COACTUP
    ],
    
    // Card management workflow
    CARD_MANAGEMENT: [
      TRANSACTION_CODES.COCRDLI,
      TRANSACTION_CODES.COCRDSL,
      TRANSACTION_CODES.COCRDUP
    ],
    
    // Transaction processing workflow
    TRANSACTION_PROCESSING: [
      TRANSACTION_CODES.COTRN00,
      TRANSACTION_CODES.COTRN01,
      TRANSACTION_CODES.COTRN02
    ]
  },
  
  /**
   * Return Patterns
   * Defines standard return navigation patterns for PF3 (Exit) functionality
   */
  RETURN_PATTERNS: {
    // Return to main menu from transaction screens
    [TRANSACTION_CODES.COACTVW]: TRANSACTION_CODES.COMEN01,
    [TRANSACTION_CODES.COACTUP]: TRANSACTION_CODES.COACTVW,
    [TRANSACTION_CODES.COCRDLI]: TRANSACTION_CODES.COMEN01,
    [TRANSACTION_CODES.COCRDSL]: TRANSACTION_CODES.COCRDLI,
    [TRANSACTION_CODES.COCRDUP]: TRANSACTION_CODES.COCRDLI,
    [TRANSACTION_CODES.COTRN00]: TRANSACTION_CODES.COMEN01,
    [TRANSACTION_CODES.COTRN01]: TRANSACTION_CODES.COTRN00,
    [TRANSACTION_CODES.COTRN02]: TRANSACTION_CODES.COTRN00,
    [TRANSACTION_CODES.COBIL00]: TRANSACTION_CODES.COMEN01,
    [TRANSACTION_CODES.CORPT00]: TRANSACTION_CODES.COMEN01,
    
    // Return to admin menu from user management screens
    [TRANSACTION_CODES.COUSR00]: TRANSACTION_CODES.COADM01,
    [TRANSACTION_CODES.COUSR01]: TRANSACTION_CODES.COUSR00,
    [TRANSACTION_CODES.COUSR02]: TRANSACTION_CODES.COUSR00,
    [TRANSACTION_CODES.COUSR03]: TRANSACTION_CODES.COUSR00,
    
    // Return to login from menu screens
    [TRANSACTION_CODES.COMEN01]: TRANSACTION_CODES.COSGN00,
    [TRANSACTION_CODES.COADM01]: TRANSACTION_CODES.COSGN00
  },
  
  /**
   * Menu Flow
   * Defines menu-specific navigation flows for role-based access
   */
  MENU_FLOW: {
    // Regular user menu options
    REGULAR_USER_OPTIONS: [
      TRANSACTION_CODES.COACTVW,
      TRANSACTION_CODES.COCRDLI,
      TRANSACTION_CODES.COTRN00,
      TRANSACTION_CODES.COBIL00,
      TRANSACTION_CODES.CORPT00
    ],
    
    // Administrator menu options
    ADMIN_OPTIONS: [
      TRANSACTION_CODES.COUSR00,
      TRANSACTION_CODES.COUSR01,
      TRANSACTION_CODES.COUSR02,
      TRANSACTION_CODES.COUSR03
    ],
    
    // Account management sub-menu
    ACCOUNT_SUBMENU: [
      TRANSACTION_CODES.COACTVW,
      TRANSACTION_CODES.COACTUP
    ],
    
    // Card management sub-menu
    CARD_SUBMENU: [
      TRANSACTION_CODES.COCRDLI,
      TRANSACTION_CODES.COCRDSL,
      TRANSACTION_CODES.COCRDUP
    ],
    
    // Transaction management sub-menu
    TRANSACTION_SUBMENU: [
      TRANSACTION_CODES.COTRN00,
      TRANSACTION_CODES.COTRN01,
      TRANSACTION_CODES.COTRN02
    ]
  },
  
  /**
   * Authentication Flow
   * Defines authentication-specific navigation patterns
   */
  AUTHENTICATION_FLOW: {
    // Login success navigation based on user role
    LOGIN_SUCCESS_ADMIN: TRANSACTION_CODES.COADM01,
    LOGIN_SUCCESS_USER: TRANSACTION_CODES.COMEN01,
    
    // Logout navigation
    LOGOUT_DESTINATION: TRANSACTION_CODES.COSGN00,
    
    // Session timeout navigation
    SESSION_TIMEOUT_DESTINATION: TRANSACTION_CODES.COSGN00,
    
    // Access denied navigation
    ACCESS_DENIED_DESTINATION: TRANSACTION_CODES.COSGN00
  }
} as const;

/**
 * Type Definitions
 * Provides TypeScript type safety for transaction constants usage
 */
export type TransactionCode = keyof typeof TRANSACTION_CODES;
export type ScreenTitle = keyof typeof SCREEN_TITLES;
export type ComponentMapping = keyof typeof COMPONENT_MAPPINGS;
export type ApiEndpoint = keyof typeof TRANSACTION_ENDPOINTS;
export type ApiBasePath = keyof typeof API_BASE_PATHS;

/**
 * Utility Functions
 * Helper functions for transaction constant manipulation and validation
 */
export const TransactionUtils = {
  /**
   * Gets the screen title for a given transaction code
   */
  getScreenTitle: (transactionCode: string): string => {
    const titleMap: Record<string, string> = {
      [TRANSACTION_CODES.COSGN00]: SCREEN_TITLES.LOGIN_TITLE,
      [TRANSACTION_CODES.COMEN01]: SCREEN_TITLES.MAIN_MENU_TITLE,
      [TRANSACTION_CODES.COACTVW]: SCREEN_TITLES.ACCOUNT_VIEW_TITLE,
      [TRANSACTION_CODES.COACTUP]: SCREEN_TITLES.ACCOUNT_UPDATE_TITLE,
      [TRANSACTION_CODES.COCRDLI]: SCREEN_TITLES.CARD_LIST_TITLE,
      [TRANSACTION_CODES.COCRDSL]: SCREEN_TITLES.CARD_SEARCH_TITLE,
      [TRANSACTION_CODES.COCRDUP]: SCREEN_TITLES.CARD_UPDATE_TITLE,
      [TRANSACTION_CODES.COTRN00]: SCREEN_TITLES.TRANSACTION_LIST_TITLE,
      [TRANSACTION_CODES.COTRN01]: SCREEN_TITLES.TRANSACTION_SEARCH_TITLE,
      [TRANSACTION_CODES.COTRN02]: SCREEN_TITLES.ADD_TRANSACTION_TITLE,
      [TRANSACTION_CODES.COBIL00]: SCREEN_TITLES.BILL_PAYMENT_TITLE,
      [TRANSACTION_CODES.CORPT00]: SCREEN_TITLES.REPORT_GENERATION_TITLE,
      [TRANSACTION_CODES.COADM01]: SCREEN_TITLES.ADMIN_MENU_TITLE,
      [TRANSACTION_CODES.COUSR00]: SCREEN_TITLES.USER_LIST_TITLE,
      [TRANSACTION_CODES.COUSR01]: SCREEN_TITLES.USER_CREATE_TITLE,
      [TRANSACTION_CODES.COUSR02]: SCREEN_TITLES.USER_UPDATE_TITLE,
      [TRANSACTION_CODES.COUSR03]: SCREEN_TITLES.USER_SEARCH_TITLE
    };
    
    return titleMap[transactionCode] || 'CardDemo - Unknown Screen';
  },
  
  /**
   * Gets the component path for a given transaction code
   */
  getComponentPath: (transactionCode: string): string => {
    const componentMap: Record<string, string> = {
      [TRANSACTION_CODES.COSGN00]: COMPONENT_MAPPINGS.LOGIN_COMPONENT,
      [TRANSACTION_CODES.COMEN01]: COMPONENT_MAPPINGS.MAIN_MENU_COMPONENT,
      [TRANSACTION_CODES.COACTVW]: COMPONENT_MAPPINGS.ACCOUNT_VIEW_COMPONENT,
      [TRANSACTION_CODES.COACTUP]: COMPONENT_MAPPINGS.ACCOUNT_UPDATE_COMPONENT,
      [TRANSACTION_CODES.COCRDLI]: COMPONENT_MAPPINGS.CARD_LIST_COMPONENT,
      [TRANSACTION_CODES.COCRDSL]: COMPONENT_MAPPINGS.CARD_SEARCH_COMPONENT,
      [TRANSACTION_CODES.COCRDUP]: COMPONENT_MAPPINGS.CARD_UPDATE_COMPONENT,
      [TRANSACTION_CODES.COTRN00]: COMPONENT_MAPPINGS.TRANSACTION_LIST_COMPONENT,
      [TRANSACTION_CODES.COTRN01]: COMPONENT_MAPPINGS.TRANSACTION_SEARCH_COMPONENT,
      [TRANSACTION_CODES.COTRN02]: COMPONENT_MAPPINGS.ADD_TRANSACTION_COMPONENT,
      [TRANSACTION_CODES.COBIL00]: COMPONENT_MAPPINGS.BILL_PAYMENT_COMPONENT,
      [TRANSACTION_CODES.CORPT00]: COMPONENT_MAPPINGS.REPORT_GENERATION_COMPONENT,
      [TRANSACTION_CODES.COADM01]: COMPONENT_MAPPINGS.ADMIN_MENU_COMPONENT,
      [TRANSACTION_CODES.COUSR00]: COMPONENT_MAPPINGS.USER_LIST_COMPONENT,
      [TRANSACTION_CODES.COUSR01]: COMPONENT_MAPPINGS.USER_CREATE_COMPONENT,
      [TRANSACTION_CODES.COUSR02]: COMPONENT_MAPPINGS.USER_UPDATE_COMPONENT,
      [TRANSACTION_CODES.COUSR03]: COMPONENT_MAPPINGS.USER_SEARCH_COMPONENT
    };
    
    return componentMap[transactionCode] || '/components/common/NotFoundComponent';
  },
  
  /**
   * Validates if a transaction code is valid
   */
  isValidTransactionCode: (transactionCode: string): boolean => {
    return Object.values(TRANSACTION_CODES).includes(transactionCode as any);
  },
  
  /**
   * Gets the return destination for a transaction (PF3 Exit functionality)
   */
  getReturnDestination: (transactionCode: string): string => {
    return TRANSACTION_FLOW.RETURN_PATTERNS[transactionCode as keyof typeof TRANSACTION_FLOW.RETURN_PATTERNS] || TRANSACTION_CODES.COSGN00;
  },
  
  /**
   * Gets valid navigation destinations from current transaction
   */
  getValidNavigationDestinations: (transactionCode: string): string[] => {
    return TRANSACTION_FLOW.XCTL_PATTERNS[transactionCode as keyof typeof TRANSACTION_FLOW.XCTL_PATTERNS] || [];
  }
};

/**
 * Export default consolidated constants for easy import
 */
export default {
  TRANSACTION_ENDPOINTS,
  API_BASE_PATHS,
  TRANSACTION_CODES,
  SCREEN_TITLES,
  COMPONENT_MAPPINGS,
  TRANSACTION_FLOW,
  TransactionUtils
};