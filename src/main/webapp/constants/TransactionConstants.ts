/**
 * CardDemo - Transaction Constants
 * 
 * TypeScript constants file defining all transaction-related identifiers extracted from BMS mapsets
 * including transaction codes, screen titles, component mappings, and navigation flow patterns.
 * Provides centralized transaction configuration ensuring traceability between original CICS 
 * transactions and React components while enabling type-safe transaction handling.
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

// Transaction endpoint mappings preserving original CICS transaction identifiers
export const TRANSACTION_ENDPOINTS = {
  LOGIN: '/api/auth/login',
  MENU: '/api/menu/main',
  ACCOUNT_VIEW: '/api/account/view',
  ACCOUNT_UPDATE: '/api/account/update',
  CARD_LIST: '/api/card/list',
  TRANSACTION_LIST: '/api/transaction/list',
  USER_MANAGEMENT: '/api/user/management'
} as const;

// API base paths for microservice endpoints
export const API_BASE_PATHS = {
  AUTH_SERVICE: '/api/auth',
  ACCOUNT_SERVICE: '/api/account',
  CARD_SERVICE: '/api/card',
  TRANSACTION_SERVICE: '/api/transaction',
  USER_SERVICE: '/api/user'
} as const;

// Transaction codes mapping each BMS mapset to original CICS transaction identifiers
export const TRANSACTION_CODES = {
  COSGN00: 'COSGN00',  // Login/Sign-on transaction
  COMEN01: 'COMEN01',  // Main menu navigation
  COACTVW: 'COACTVW',  // Account view transaction
  COACTUP: 'COACTUP',  // Account update transaction
  COCRDLI: 'COCRDLI',  // Card list display
  COCRDSL: 'COCRDSL',  // Card search functionality
  COCRDUP: 'COCRDUP',  // Card update transaction
  COTRN00: 'COTRN00',  // Transaction list display
  COTRN01: 'COTRN01',  // Transaction search
  COTRN02: 'COTRN02',  // Add new transaction
  COBIL00: 'COBIL00',  // Bill payment processing
  CORPT00: 'CORPT00',  // Report generation
  COADM01: 'COADM01',  // Administrative menu
  COUSR00: 'COUSR00',  // User list display
  COUSR01: 'COUSR01',  // User creation
  COUSR02: 'COUSR02',  // User update/modification
  COUSR03: 'COUSR03'   // User search functionality
} as const;

// Screen titles extracted from BMS mapset definitions
export const SCREEN_TITLES = {
  LOGIN_TITLE: 'CardDemo - Sign On',
  MAIN_MENU_TITLE: 'Main Menu',
  ACCOUNT_VIEW_TITLE: 'View Account',
  ACCOUNT_UPDATE_TITLE: 'Update Account',
  CARD_LIST_TITLE: 'List Credit Cards',
  CARD_SEARCH_TITLE: 'Search Credit Cards',
  CARD_UPDATE_TITLE: 'Update Credit Card',
  TRANSACTION_LIST_TITLE: 'List Transactions',
  TRANSACTION_SEARCH_TITLE: 'Search Transactions',
  ADD_TRANSACTION_TITLE: 'Add Transaction',
  BILL_PAYMENT_TITLE: 'Bill Payment',
  REPORT_GENERATION_TITLE: 'Report Generation',
  ADMIN_MENU_TITLE: 'Administrative Menu',
  USER_LIST_TITLE: 'User List',
  USER_CREATE_TITLE: 'Create User',
  USER_UPDATE_TITLE: 'Update User',
  USER_SEARCH_TITLE: 'Search Users'
} as const;

// Component mappings linking BMS maps to their corresponding React component paths
export const COMPONENT_MAPPINGS = {
  LOGIN_COMPONENT: 'components/auth/LoginComponent',
  MAIN_MENU_COMPONENT: 'components/menu/MainMenuComponent',
  ACCOUNT_VIEW_COMPONENT: 'components/account/AccountViewComponent',
  ACCOUNT_UPDATE_COMPONENT: 'components/account/AccountUpdateComponent',
  CARD_LIST_COMPONENT: 'components/card/CardListComponent',
  CARD_SEARCH_COMPONENT: 'components/card/CardSearchComponent',
  CARD_UPDATE_COMPONENT: 'components/card/CardUpdateComponent',
  TRANSACTION_LIST_COMPONENT: 'components/transaction/TransactionListComponent',
  TRANSACTION_SEARCH_COMPONENT: 'components/transaction/TransactionSearchComponent',
  ADD_TRANSACTION_COMPONENT: 'components/transaction/AddTransactionComponent',
  BILL_PAYMENT_COMPONENT: 'components/billing/BillPaymentComponent',
  REPORT_GENERATION_COMPONENT: 'components/reports/ReportGenerationComponent',
  ADMIN_MENU_COMPONENT: 'components/admin/AdminMenuComponent',
  USER_LIST_COMPONENT: 'components/user/UserListComponent',
  USER_CREATE_COMPONENT: 'components/user/UserCreateComponent',
  USER_UPDATE_COMPONENT: 'components/user/UserUpdateComponent',
  USER_SEARCH_COMPONENT: 'components/user/UserSearchComponent'
} as const;

// Transaction flow patterns preserving original CICS XCTL navigation patterns
export const TRANSACTION_FLOW = {
  // XCTL patterns - equivalent to CICS program control transfers
  XCTL_PATTERNS: {
    FROM_LOGIN: [TRANSACTION_CODES.COMEN01], // Login -> Main Menu
    FROM_MAIN_MENU: [
      TRANSACTION_CODES.COACTVW,   // Menu -> Account View
      TRANSACTION_CODES.COCRDLI,   // Menu -> Card List
      TRANSACTION_CODES.COTRN00,   // Menu -> Transaction List
      TRANSACTION_CODES.CORPT00,   // Menu -> Reports
      TRANSACTION_CODES.COADM01    // Menu -> Admin (if authorized)
    ],
    FROM_ADMIN_MENU: [
      TRANSACTION_CODES.COUSR00,   // Admin -> User Management
      TRANSACTION_CODES.COMEN01    // Admin -> Main Menu
    ],
    FROM_ACCOUNT_VIEW: [
      TRANSACTION_CODES.COACTUP,   // Account View -> Account Update
      TRANSACTION_CODES.COCRDLI,   // Account View -> Card List
      TRANSACTION_CODES.COMEN01    // Account View -> Main Menu
    ],
    FROM_CARD_LIST: [
      TRANSACTION_CODES.COCRDUP,   // Card List -> Card Update
      TRANSACTION_CODES.COCRDSL,   // Card List -> Card Search
      TRANSACTION_CODES.COTRN00,   // Card List -> Transaction List
      TRANSACTION_CODES.COMEN01    // Card List -> Main Menu
    ],
    FROM_TRANSACTION_LIST: [
      TRANSACTION_CODES.COTRN01,   // Transaction List -> Transaction Search
      TRANSACTION_CODES.COTRN02,   // Transaction List -> Add Transaction
      TRANSACTION_CODES.COMEN01    // Transaction List -> Main Menu
    ]
  },

  // Navigation sequences preserving pseudo-conversational flow
  NAVIGATION_SEQUENCES: {
    MAIN_FLOW: [
      TRANSACTION_CODES.COSGN00,   // 1. Login
      TRANSACTION_CODES.COMEN01,   // 2. Main Menu
      TRANSACTION_CODES.COACTVW    // 3. Account View
    ],
    ACCOUNT_MANAGEMENT: [
      TRANSACTION_CODES.COACTVW,   // 1. View Account
      TRANSACTION_CODES.COACTUP    // 2. Update Account
    ],
    CARD_MANAGEMENT: [
      TRANSACTION_CODES.COCRDLI,   // 1. List Cards
      TRANSACTION_CODES.COCRDSL,   // 2. Search Cards
      TRANSACTION_CODES.COCRDUP    // 3. Update Card
    ],
    TRANSACTION_MANAGEMENT: [
      TRANSACTION_CODES.COTRN00,   // 1. List Transactions
      TRANSACTION_CODES.COTRN01,   // 2. Search Transactions
      TRANSACTION_CODES.COTRN02    // 3. Add Transaction
    ],
    USER_MANAGEMENT: [
      TRANSACTION_CODES.COUSR00,   // 1. List Users
      TRANSACTION_CODES.COUSR01,   // 2. Create User
      TRANSACTION_CODES.COUSR02,   // 3. Update User
      TRANSACTION_CODES.COUSR03    // 4. Search Users
    ]
  },

  // Return patterns for maintaining proper navigation flow
  RETURN_PATTERNS: {
    CANCEL_OPERATIONS: TRANSACTION_CODES.COMEN01,  // F3=Exit -> Main Menu
    SUCCESSFUL_UPDATES: TRANSACTION_CODES.COMEN01, // Successful operations -> Main Menu
    ERROR_RECOVERY: 'PREVIOUS_SCREEN',             // Errors -> Previous screen
    LOGOUT: TRANSACTION_CODES.COSGN00              // Sign off -> Login
  },

  // Menu flow configuration
  MENU_FLOW: {
    MAIN_MENU_OPTIONS: {
      '1': TRANSACTION_CODES.COACTVW,  // Account View
      '2': TRANSACTION_CODES.COCRDLI,  // Card List
      '3': TRANSACTION_CODES.COTRN00,  // Transaction List
      '4': TRANSACTION_CODES.COBIL00,  // Bill Payment
      '5': TRANSACTION_CODES.CORPT00,  // Reports
      '6': TRANSACTION_CODES.COADM01   // Admin Menu (role-based)
    },
    ADMIN_MENU_OPTIONS: {
      '1': TRANSACTION_CODES.COUSR00,  // User List
      '2': TRANSACTION_CODES.COUSR01,  // Create User
      '3': TRANSACTION_CODES.COUSR02,  // Update User
      '4': TRANSACTION_CODES.COUSR03,  // Search Users
      '5': TRANSACTION_CODES.COMEN01   // Return to Main Menu
    }
  },

  // Authentication flow patterns
  AUTHENTICATION_FLOW: {
    LOGIN_SUCCESS: TRANSACTION_CODES.COMEN01,      // Successful login -> Main Menu
    LOGIN_FAILURE: TRANSACTION_CODES.COSGN00,      // Failed login -> Login screen
    SESSION_TIMEOUT: TRANSACTION_CODES.COSGN00,    // Timeout -> Login screen
    FORCED_LOGOUT: TRANSACTION_CODES.COSGN00       // Security logout -> Login screen
  }
} as const;

// Type definitions for transaction constants
export type TransactionCode = keyof typeof TRANSACTION_CODES;
export type ScreenTitle = keyof typeof SCREEN_TITLES;
export type ComponentMapping = keyof typeof COMPONENT_MAPPINGS;
export type TransactionEndpoint = keyof typeof TRANSACTION_ENDPOINTS;
export type ApiBasePath = keyof typeof API_BASE_PATHS;

// Utility functions for transaction constant lookups
export const getTransactionTitle = (code: TransactionCode): string => {
  const titleMap: Record<TransactionCode, string> = {
    COSGN00: SCREEN_TITLES.LOGIN_TITLE,
    COMEN01: SCREEN_TITLES.MAIN_MENU_TITLE,
    COACTVW: SCREEN_TITLES.ACCOUNT_VIEW_TITLE,
    COACTUP: SCREEN_TITLES.ACCOUNT_UPDATE_TITLE,
    COCRDLI: SCREEN_TITLES.CARD_LIST_TITLE,
    COCRDSL: SCREEN_TITLES.CARD_SEARCH_TITLE,
    COCRDUP: SCREEN_TITLES.CARD_UPDATE_TITLE,
    COTRN00: SCREEN_TITLES.TRANSACTION_LIST_TITLE,
    COTRN01: SCREEN_TITLES.TRANSACTION_SEARCH_TITLE,
    COTRN02: SCREEN_TITLES.ADD_TRANSACTION_TITLE,
    COBIL00: SCREEN_TITLES.BILL_PAYMENT_TITLE,
    CORPT00: SCREEN_TITLES.REPORT_GENERATION_TITLE,
    COADM01: SCREEN_TITLES.ADMIN_MENU_TITLE,
    COUSR00: SCREEN_TITLES.USER_LIST_TITLE,
    COUSR01: SCREEN_TITLES.USER_CREATE_TITLE,
    COUSR02: SCREEN_TITLES.USER_UPDATE_TITLE,
    COUSR03: SCREEN_TITLES.USER_SEARCH_TITLE
  };
  return titleMap[code];
};

export const getComponentPath = (code: TransactionCode): string => {
  const componentMap: Record<TransactionCode, string> = {
    COSGN00: COMPONENT_MAPPINGS.LOGIN_COMPONENT,
    COMEN01: COMPONENT_MAPPINGS.MAIN_MENU_COMPONENT,
    COACTVW: COMPONENT_MAPPINGS.ACCOUNT_VIEW_COMPONENT,
    COACTUP: COMPONENT_MAPPINGS.ACCOUNT_UPDATE_COMPONENT,
    COCRDLI: COMPONENT_MAPPINGS.CARD_LIST_COMPONENT,
    COCRDSL: COMPONENT_MAPPINGS.CARD_SEARCH_COMPONENT,
    COCRDUP: COMPONENT_MAPPINGS.CARD_UPDATE_COMPONENT,
    COTRN00: COMPONENT_MAPPINGS.TRANSACTION_LIST_COMPONENT,
    COTRN01: COMPONENT_MAPPINGS.TRANSACTION_SEARCH_COMPONENT,
    COTRN02: COMPONENT_MAPPINGS.ADD_TRANSACTION_COMPONENT,
    COBIL00: COMPONENT_MAPPINGS.BILL_PAYMENT_COMPONENT,
    CORPT00: COMPONENT_MAPPINGS.REPORT_GENERATION_COMPONENT,
    COADM01: COMPONENT_MAPPINGS.ADMIN_MENU_COMPONENT,
    COUSR00: COMPONENT_MAPPINGS.USER_LIST_COMPONENT,
    COUSR01: COMPONENT_MAPPINGS.USER_CREATE_COMPONENT,
    COUSR02: COMPONENT_MAPPINGS.USER_UPDATE_COMPONENT,
    COUSR03: COMPONENT_MAPPINGS.USER_SEARCH_COMPONENT
  };
  return componentMap[code];
};

// Validation functions for transaction flow integrity
export const isValidTransactionFlow = (from: TransactionCode, to: TransactionCode): boolean => {
  const patternKey = `FROM_${from}` as string;
  const fromPatternsKey = patternKey as keyof typeof TRANSACTION_FLOW.XCTL_PATTERNS;
  const fromPatterns = TRANSACTION_FLOW.XCTL_PATTERNS[fromPatternsKey];
  return fromPatterns ? (fromPatterns as readonly string[]).includes(to) : false;
};

export const getNextTransactionOptions = (current: TransactionCode): TransactionCode[] => {
  const patternKey = `FROM_${current}` as string;
  const fromPatternsKey = patternKey as keyof typeof TRANSACTION_FLOW.XCTL_PATTERNS;
  const patterns = TRANSACTION_FLOW.XCTL_PATTERNS[fromPatternsKey];
  return patterns ? [...patterns] as TransactionCode[] : [];
};

// Transaction routing configuration for React Router integration
export const TRANSACTION_ROUTES = {
  [TRANSACTION_CODES.COSGN00]: '/login',
  [TRANSACTION_CODES.COMEN01]: '/menu',
  [TRANSACTION_CODES.COACTVW]: '/account/view',
  [TRANSACTION_CODES.COACTUP]: '/account/update',
  [TRANSACTION_CODES.COCRDLI]: '/card/list',
  [TRANSACTION_CODES.COCRDSL]: '/card/search',
  [TRANSACTION_CODES.COCRDUP]: '/card/update',
  [TRANSACTION_CODES.COTRN00]: '/transaction/list',
  [TRANSACTION_CODES.COTRN01]: '/transaction/search',
  [TRANSACTION_CODES.COTRN02]: '/transaction/add',
  [TRANSACTION_CODES.COBIL00]: '/billing/payment',
  [TRANSACTION_CODES.CORPT00]: '/reports',
  [TRANSACTION_CODES.COADM01]: '/admin',
  [TRANSACTION_CODES.COUSR00]: '/user/list',
  [TRANSACTION_CODES.COUSR01]: '/user/create',
  [TRANSACTION_CODES.COUSR02]: '/user/update',
  [TRANSACTION_CODES.COUSR03]: '/user/search'
} as const;

// Export all constants as default for convenient importing
export default {
  TRANSACTION_ENDPOINTS,
  API_BASE_PATHS,
  TRANSACTION_CODES,
  SCREEN_TITLES,
  COMPONENT_MAPPINGS,
  TRANSACTION_FLOW,
  TRANSACTION_ROUTES,
  getTransactionTitle,
  getComponentPath,
  isValidTransactionFlow,
  getNextTransactionOptions
};