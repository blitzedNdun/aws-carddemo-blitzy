/**
 * Transaction Constants
 * 
 * TypeScript constants file defining all transaction-related identifiers extracted from BMS mapsets
 * including transaction codes, screen titles, component mappings, and navigation flow patterns.
 * Provides centralized transaction configuration ensuring traceability between original CICS transactions
 * and React components while enabling type-safe transaction handling.
 * 
 * Maps original CICS transaction codes to React components and REST API endpoints
 * maintaining exact functional equivalence as specified in the technical requirements.
 */

// ==========================================
// Transaction Codes - Direct BMS Mapping
// ==========================================

/**
 * Original CICS transaction codes from BMS mapsets
 * Maps each BMS mapset to its corresponding transaction identifier for audit traceability
 */
export const TRANSACTION_CODES = {
  // Authentication and Security
  COSGN00: 'COSGN00',    // Login Screen - Sign On Transaction
  
  // Main Menu Navigation
  COMEN01: 'COMEN01',    // Main Menu Screen - Menu Navigation
  COADM01: 'COADM01',    // Admin Menu Screen - Administrative Menu
  
  // Account Management
  COACTVW: 'COACTVW',    // Account View Screen - Account Information Display
  COACTUP: 'COACTUP',    // Account Update Screen - Account Information Update
  
  // Credit Card Management
  COCRDLI: 'COCRDLI',    // Card List Screen - Card Listing and Browse
  COCRDSL: 'COCRDSL',    // Card Search Screen - Card Search and Selection
  COCRDUP: 'COCRDUP',    // Card Update Screen - Card Information Update
  
  // Transaction Processing
  COTRN00: 'COTRN00',    // Transaction List Screen - Transaction History
  COTRN01: 'COTRN01',    // Transaction Detail Screen - Transaction Details
  COTRN02: 'COTRN02',    // Add Transaction Screen - Transaction Entry
  
  // Payment Processing
  COBIL00: 'COBIL00',    // Bill Payment Screen - Customer Payment Processing
  
  // Reporting and Analytics
  CORPT00: 'CORPT00',    // Report Generation Screen - Report Processing
  
  // User Management
  COUSR00: 'COUSR00',    // User List Screen - User Directory
  COUSR01: 'COUSR01',    // User Create Screen - New User Creation
  COUSR02: 'COUSR02',    // User Update Screen - User Information Update
  COUSR03: 'COUSR03',    // User Delete Screen - User Removal
} as const;

// ==========================================
// Screen Titles - From BMS Mapsets
// ==========================================

/**
 * Screen titles extracted from BMS mapset titles for consistent header display
 * Preserves original mainframe screen titles for user familiarity
 */
export const SCREEN_TITLES = {
  // Authentication
  LOGIN_TITLE: 'CardDemo - Login Screen',
  
  // Navigation
  MAIN_MENU_TITLE: 'CardDemo - Main Menu Screen',
  ADMIN_MENU_TITLE: 'CardDemo - Admin Menu Screen',
  
  // Account Management
  ACCOUNT_VIEW_TITLE: 'CardDemo - Account Viewer Screen',
  ACCOUNT_UPDATE_TITLE: 'CardDemo - Account Update Screen',
  
  // Card Management
  CARD_LIST_TITLE: 'CardDemo - Card Listing Screen',
  CARD_SEARCH_TITLE: 'CardDemo - Card Search Screen',
  CARD_UPDATE_TITLE: 'CardDemo - Card Update Screen',
  
  // Transaction Processing
  TRANSACTION_LIST_TITLE: 'CardDemo - Transaction List',
  TRANSACTION_SEARCH_TITLE: 'CardDemo - Transaction Search',
  ADD_TRANSACTION_TITLE: 'CardDemo - Add Transaction',
  
  // Payment Processing
  BILL_PAYMENT_TITLE: 'CardDemo - Bill Payment Screen',
  
  // Reporting
  REPORT_GENERATION_TITLE: 'CardDemo - Report Generation Screen',
  
  // User Management
  USER_LIST_TITLE: 'CardDemo - User List Screen',
  USER_CREATE_TITLE: 'CardDemo - User Create Screen',
  USER_UPDATE_TITLE: 'CardDemo - User Update Screen',
  USER_SEARCH_TITLE: 'CardDemo - User Search Screen',
} as const;

// ==========================================
// REST API Endpoints - Transaction Mapping
// ==========================================

/**
 * REST API endpoints mapping original CICS transaction codes to Spring Boot microservices
 * Each endpoint corresponds to a specific transaction type enabling consistent API routing
 */
export const TRANSACTION_ENDPOINTS = {
  // Authentication Service
  LOGIN: '/api/auth/login',
  
  // Menu Navigation Service
  MENU: '/api/menu/main',
  
  // Account Management Service
  ACCOUNT_VIEW: '/api/accounts/view',
  ACCOUNT_UPDATE: '/api/accounts/update',
  
  // Card Management Service
  CARD_LIST: '/api/cards/list',
  
  // Transaction Processing Service
  TRANSACTION_LIST: '/api/transactions/list',
  
  // User Management Service
  USER_MANAGEMENT: '/api/users/manage',
} as const;

// ==========================================
// API Base Paths - Service Organization
// ==========================================

/**
 * Base API paths for microservice organization
 * Maps business domains to their corresponding Spring Boot service base paths
 */
export const API_BASE_PATHS = {
  // Core Services
  AUTH_SERVICE: '/api/auth',
  ACCOUNT_SERVICE: '/api/accounts',
  CARD_SERVICE: '/api/cards',
  TRANSACTION_SERVICE: '/api/transactions',
  USER_SERVICE: '/api/users',
} as const;

// ==========================================
// Component Mappings - BMS to React
// ==========================================

/**
 * Component mappings linking BMS maps to their corresponding React component paths
 * Maintains traceability between original CICS screens and React components
 */
export const COMPONENT_MAPPINGS = {
  // Authentication
  LOGIN_COMPONENT: '/login',
  
  // Navigation
  MAIN_MENU_COMPONENT: '/menu',
  ADMIN_MENU_COMPONENT: '/admin',
  
  // Account Management
  ACCOUNT_VIEW_COMPONENT: '/accounts/view',
  ACCOUNT_UPDATE_COMPONENT: '/accounts/update',
  
  // Card Management
  CARD_LIST_COMPONENT: '/cards/list',
  CARD_SEARCH_COMPONENT: '/cards/search',
  CARD_UPDATE_COMPONENT: '/cards/update',
  
  // Transaction Processing
  TRANSACTION_LIST_COMPONENT: '/transactions/list',
  TRANSACTION_SEARCH_COMPONENT: '/transactions/search',
  ADD_TRANSACTION_COMPONENT: '/transactions/add',
  
  // Payment Processing
  BILL_PAYMENT_COMPONENT: '/payments/bill',
  
  // Reporting
  REPORT_GENERATION_COMPONENT: '/reports/generate',
  
  // User Management
  USER_LIST_COMPONENT: '/users/list',
  USER_CREATE_COMPONENT: '/users/create',
  USER_UPDATE_COMPONENT: '/users/update',
  USER_SEARCH_COMPONENT: '/users/search',
} as const;

// ==========================================
// Transaction Flow - Navigation Patterns
// ==========================================

/**
 * Transaction flow constants preserving original CICS XCTL navigation patterns
 * Defines navigation sequences and return patterns for React Router implementation
 */
export const TRANSACTION_FLOW = {
  // CICS XCTL Navigation Patterns
  XCTL_PATTERNS: {
    // Authentication flow
    LOGIN_TO_MENU: { from: TRANSACTION_CODES.COSGN00, to: TRANSACTION_CODES.COMEN01 },
    LOGIN_TO_ADMIN: { from: TRANSACTION_CODES.COSGN00, to: TRANSACTION_CODES.COADM01 },
    
    // Menu navigation flows
    MENU_TO_ACCOUNT_VIEW: { from: TRANSACTION_CODES.COMEN01, to: TRANSACTION_CODES.COACTVW },
    MENU_TO_CARD_LIST: { from: TRANSACTION_CODES.COMEN01, to: TRANSACTION_CODES.COCRDLI },
    MENU_TO_TRANSACTION_LIST: { from: TRANSACTION_CODES.COMEN01, to: TRANSACTION_CODES.COTRN00 },
    MENU_TO_BILL_PAYMENT: { from: TRANSACTION_CODES.COMEN01, to: TRANSACTION_CODES.COBIL00 },
    
    // Account management flows
    ACCOUNT_VIEW_TO_UPDATE: { from: TRANSACTION_CODES.COACTVW, to: TRANSACTION_CODES.COACTUP },
    
    // Card management flows
    CARD_LIST_TO_SEARCH: { from: TRANSACTION_CODES.COCRDLI, to: TRANSACTION_CODES.COCRDSL },
    CARD_LIST_TO_UPDATE: { from: TRANSACTION_CODES.COCRDLI, to: TRANSACTION_CODES.COCRDUP },
    
    // Transaction processing flows
    TRANSACTION_LIST_TO_DETAIL: { from: TRANSACTION_CODES.COTRN00, to: TRANSACTION_CODES.COTRN01 },
    TRANSACTION_LIST_TO_ADD: { from: TRANSACTION_CODES.COTRN00, to: TRANSACTION_CODES.COTRN02 },
    
    // Admin flows
    ADMIN_TO_USER_LIST: { from: TRANSACTION_CODES.COADM01, to: TRANSACTION_CODES.COUSR00 },
    ADMIN_TO_REPORTS: { from: TRANSACTION_CODES.COADM01, to: TRANSACTION_CODES.CORPT00 },
    
    // User management flows
    USER_LIST_TO_CREATE: { from: TRANSACTION_CODES.COUSR00, to: TRANSACTION_CODES.COUSR01 },
    USER_LIST_TO_UPDATE: { from: TRANSACTION_CODES.COUSR00, to: TRANSACTION_CODES.COUSR02 },
    USER_LIST_TO_DELETE: { from: TRANSACTION_CODES.COUSR00, to: TRANSACTION_CODES.COUSR03 },
  },
  
  // Navigation Sequences
  NAVIGATION_SEQUENCES: {
    // Primary navigation paths
    MAIN_FLOW: [
      TRANSACTION_CODES.COSGN00,   // Login
      TRANSACTION_CODES.COMEN01,   // Main Menu
      TRANSACTION_CODES.COACTVW,   // Account View
    ],
    
    // Administrative flow
    ADMIN_FLOW: [
      TRANSACTION_CODES.COSGN00,   // Login
      TRANSACTION_CODES.COADM01,   // Admin Menu
      TRANSACTION_CODES.COUSR00,   // User Management
    ],
    
    // Transaction processing flow
    TRANSACTION_FLOW: [
      TRANSACTION_CODES.COMEN01,   // Main Menu
      TRANSACTION_CODES.COTRN00,   // Transaction List
      TRANSACTION_CODES.COTRN01,   // Transaction Detail
    ],
    
    // Card management flow
    CARD_FLOW: [
      TRANSACTION_CODES.COMEN01,   // Main Menu
      TRANSACTION_CODES.COCRDLI,   // Card List
      TRANSACTION_CODES.COCRDUP,   // Card Update
    ],
  },
  
  // Return Patterns - F3/F12 Navigation
  RETURN_PATTERNS: {
    // F3 Exit patterns (return to previous screen)
    EXIT_PATTERNS: {
      [TRANSACTION_CODES.COACTVW]: TRANSACTION_CODES.COMEN01,
      [TRANSACTION_CODES.COACTUP]: TRANSACTION_CODES.COACTVW,
      [TRANSACTION_CODES.COCRDLI]: TRANSACTION_CODES.COMEN01,
      [TRANSACTION_CODES.COCRDUP]: TRANSACTION_CODES.COCRDLI,
      [TRANSACTION_CODES.COTRN00]: TRANSACTION_CODES.COMEN01,
      [TRANSACTION_CODES.COTRN01]: TRANSACTION_CODES.COTRN00,
      [TRANSACTION_CODES.COTRN02]: TRANSACTION_CODES.COTRN00,
      [TRANSACTION_CODES.COBIL00]: TRANSACTION_CODES.COMEN01,
      [TRANSACTION_CODES.CORPT00]: TRANSACTION_CODES.COADM01,
      [TRANSACTION_CODES.COUSR00]: TRANSACTION_CODES.COADM01,
      [TRANSACTION_CODES.COUSR01]: TRANSACTION_CODES.COUSR00,
      [TRANSACTION_CODES.COUSR02]: TRANSACTION_CODES.COUSR00,
      [TRANSACTION_CODES.COUSR03]: TRANSACTION_CODES.COUSR00,
    },
    
    // F12 Cancel patterns (return to main menu)
    CANCEL_PATTERNS: {
      [TRANSACTION_CODES.COACTUP]: TRANSACTION_CODES.COMEN01,
      [TRANSACTION_CODES.COCRDUP]: TRANSACTION_CODES.COMEN01,
      [TRANSACTION_CODES.COTRN02]: TRANSACTION_CODES.COMEN01,
      [TRANSACTION_CODES.COUSR01]: TRANSACTION_CODES.COADM01,
      [TRANSACTION_CODES.COUSR02]: TRANSACTION_CODES.COADM01,
      [TRANSACTION_CODES.COUSR03]: TRANSACTION_CODES.COADM01,
    },
  },
  
  // Menu Navigation
  MENU_FLOW: {
    // Main menu option mappings
    MAIN_MENU_OPTIONS: {
      '1': TRANSACTION_CODES.COACTVW,    // Account View
      '2': TRANSACTION_CODES.COACTUP,    // Account Update
      '3': TRANSACTION_CODES.COCRDLI,    // Card List
      '4': TRANSACTION_CODES.COTRN00,    // Transaction List
      '5': TRANSACTION_CODES.COBIL00,    // Bill Payment
      '6': TRANSACTION_CODES.CORPT00,    // Reports
      '7': TRANSACTION_CODES.COADM01,    // Admin Menu
    },
    
    // Admin menu option mappings
    ADMIN_MENU_OPTIONS: {
      '1': TRANSACTION_CODES.COUSR00,    // User Management
      '2': TRANSACTION_CODES.CORPT00,    // Reports
      '3': TRANSACTION_CODES.COMEN01,    // Return to Main Menu
    },
  },
  
  // Authentication Flow
  AUTHENTICATION_FLOW: {
    // Role-based menu routing
    ROLE_ROUTING: {
      'USER': TRANSACTION_CODES.COMEN01,     // Standard user → Main Menu
      'ADMIN': TRANSACTION_CODES.COADM01,    // Admin user → Admin Menu
      'GUEST': TRANSACTION_CODES.COACTVW,    // Guest user → Account View (read-only)
    },
    
    // Authentication states
    AUTH_STATES: {
      LOGGED_OUT: 'LOGGED_OUT',
      AUTHENTICATING: 'AUTHENTICATING',
      AUTHENTICATED: 'AUTHENTICATED',
      SESSION_EXPIRED: 'SESSION_EXPIRED',
    },
  },
} as const;

// ==========================================
// Type Definitions for Type Safety
// ==========================================

/**
 * Type definitions for transaction constants ensuring compile-time validation
 */
export type TransactionCode = typeof TRANSACTION_CODES[keyof typeof TRANSACTION_CODES];
export type ScreenTitle = typeof SCREEN_TITLES[keyof typeof SCREEN_TITLES];
export type TransactionEndpoint = typeof TRANSACTION_ENDPOINTS[keyof typeof TRANSACTION_ENDPOINTS];
export type ApiBasePath = typeof API_BASE_PATHS[keyof typeof API_BASE_PATHS];
export type ComponentMapping = typeof COMPONENT_MAPPINGS[keyof typeof COMPONENT_MAPPINGS];

// ==========================================
// Utility Functions
// ==========================================

/**
 * Utility functions for transaction constant operations
 */
export const TransactionUtils = {
  /**
   * Get component path for a transaction code
   */
  getComponentPath(transactionCode: TransactionCode): string {
    const mapping: Record<TransactionCode, string> = {
      [TRANSACTION_CODES.COSGN00]: COMPONENT_MAPPINGS.LOGIN_COMPONENT,
      [TRANSACTION_CODES.COMEN01]: COMPONENT_MAPPINGS.MAIN_MENU_COMPONENT,
      [TRANSACTION_CODES.COADM01]: COMPONENT_MAPPINGS.ADMIN_MENU_COMPONENT,
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
      [TRANSACTION_CODES.COUSR00]: COMPONENT_MAPPINGS.USER_LIST_COMPONENT,
      [TRANSACTION_CODES.COUSR01]: COMPONENT_MAPPINGS.USER_CREATE_COMPONENT,
      [TRANSACTION_CODES.COUSR02]: COMPONENT_MAPPINGS.USER_UPDATE_COMPONENT,
      [TRANSACTION_CODES.COUSR03]: COMPONENT_MAPPINGS.USER_SEARCH_COMPONENT,
    };
    
    return mapping[transactionCode] || COMPONENT_MAPPINGS.MAIN_MENU_COMPONENT;
  },

  /**
   * Get screen title for a transaction code
   */
  getScreenTitle(transactionCode: TransactionCode): string {
    const mapping: Record<TransactionCode, string> = {
      [TRANSACTION_CODES.COSGN00]: SCREEN_TITLES.LOGIN_TITLE,
      [TRANSACTION_CODES.COMEN01]: SCREEN_TITLES.MAIN_MENU_TITLE,
      [TRANSACTION_CODES.COADM01]: SCREEN_TITLES.ADMIN_MENU_TITLE,
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
      [TRANSACTION_CODES.COUSR00]: SCREEN_TITLES.USER_LIST_TITLE,
      [TRANSACTION_CODES.COUSR01]: SCREEN_TITLES.USER_CREATE_TITLE,
      [TRANSACTION_CODES.COUSR02]: SCREEN_TITLES.USER_UPDATE_TITLE,
      [TRANSACTION_CODES.COUSR03]: SCREEN_TITLES.USER_SEARCH_TITLE,
    };
    
    return mapping[transactionCode] || SCREEN_TITLES.MAIN_MENU_TITLE;
  },

  /**
   * Get next transaction in navigation flow
   */
  getNextTransaction(currentTransaction: TransactionCode, userRole: string = 'USER'): TransactionCode {
    // Check authentication flow first
    if (currentTransaction === TRANSACTION_CODES.COSGN00) {
      return TRANSACTION_FLOW.AUTHENTICATION_FLOW.ROLE_ROUTING[userRole as keyof typeof TRANSACTION_FLOW.AUTHENTICATION_FLOW.ROLE_ROUTING] || TRANSACTION_CODES.COMEN01;
    }
    
    // Return next transaction in sequence or main menu
    return TRANSACTION_CODES.COMEN01;
  },

  /**
   * Get previous transaction for F3 exit
   */
  getPreviousTransaction(currentTransaction: TransactionCode): TransactionCode {
    const exitPatterns = TRANSACTION_FLOW.RETURN_PATTERNS.EXIT_PATTERNS;
    return (exitPatterns as any)[currentTransaction] || TRANSACTION_CODES.COMEN01;
  },

  /**
   * Get cancel transaction for F12 cancel
   */
  getCancelTransaction(currentTransaction: TransactionCode): TransactionCode {
    const cancelPatterns = TRANSACTION_FLOW.RETURN_PATTERNS.CANCEL_PATTERNS;
    return (cancelPatterns as any)[currentTransaction] || TRANSACTION_CODES.COMEN01;
  },
} as const;

// Export all constants for global access
export default {
  TRANSACTION_CODES,
  SCREEN_TITLES,
  TRANSACTION_ENDPOINTS,
  API_BASE_PATHS,
  COMPONENT_MAPPINGS,
  TRANSACTION_FLOW,
  TransactionUtils,
};