/**
 * NavigationConstants.ts
 * 
 * TypeScript constants file defining React Router navigation paths and routing configuration
 * extracted from BMS screen flow patterns. Maps original CICS XCTL navigation to modern
 * declarative routing while preserving user experience and security hierarchies.
 * 
 * This file provides centralized navigation configuration for consistent routing across
 * all React components, maintaining exact functional equivalence with original COBOL/CICS
 * navigation patterns while enabling modern web application routing capabilities.
 * 
 * Key Features:
 * - Complete BMS screen to React Router path mapping for all 18 screens
 * - Original CICS transaction code preservation for audit traceability
 * - Role-based navigation supporting original RACF security hierarchy
 * - Breadcrumb navigation with hierarchical path tracking
 * - XCTL pattern mapping for pseudo-conversational flow preservation
 * - Dynamic route parameter support for COMMAREA data replacement
 * 
 * Technology Transformation: COBOL/CICS/BMS → Java/Spring Boot/React
 * Original System: IBM CICS Transaction Server with BMS screen definitions
 * Target System: React Router with TypeScript navigation constants
 * 
 * Copyright (c) 2023 CardDemo Application - Mainframe Modernization
 */

import { TRANSACTION_ENDPOINTS } from './TransactionConstants';
import { BreadcrumbData } from '../types/NavigationTypes';

// ==========================================
// Core Route Constants - BMS Screen Mapping
// ==========================================

/**
 * Primary route definitions mapping each BMS screen to its corresponding React Router path.
 * Maintains original CICS transaction code references in path structure for audit traceability
 * and preserves user navigation patterns from mainframe terminal interface.
 * 
 * Path Structure Convention:
 * - Authentication: /auth/* (COSGN00 → /auth/login)
 * - Menu Navigation: /menu/* (COMEN01 → /menu/main, COADM01 → /menu/admin)
 * - Account Management: /accounts/* (COACTVW → /accounts/view)
 * - Card Management: /cards/* (COCRDLI → /cards/list)
 * - Transaction Processing: /transactions/* (COTRN00 → /transactions/list)
 * - User Management: /users/* (COUSR00 → /users/list)
 * - Reports: /reports/* (CORPT00 → /reports/generate)
 * - Payments: /payments/* (COBIL00 → /payments/bill)
 */
export const ROUTES = {
  // Authentication Routes - COSGN00 BMS Mapset
  LOGIN: '/auth/login',
  LOGOUT: '/auth/logout',
  
  // Menu Navigation Routes - COMEN01 and COADM01 BMS Mapsets
  MENU: '/menu/main',
  ADMIN_MENU: '/menu/admin',
  
  // Account Management Routes - COACTVW and COACTUP BMS Mapsets
  ACCOUNT_VIEW: '/accounts/view',
  ACCOUNT_UPDATE: '/accounts/update',
  ACCOUNT_SEARCH: '/accounts/search',
  
  // Card Management Routes - COCRDLI, COCRDSL, COCRDUP BMS Mapsets
  CARD_LIST: '/cards/list',
  CARD_SEARCH: '/cards/search',
  CARD_UPDATE: '/cards/update',
  CARD_SELECT: '/cards/select',
  
  // Transaction Processing Routes - COTRN00, COTRN01, COTRN02 BMS Mapsets
  TRANSACTION_LIST: '/transactions/list',
  TRANSACTION_DETAIL: '/transactions/detail',
  TRANSACTION_ADD: '/transactions/add',
  TRANSACTION_SEARCH: '/transactions/search',
  
  // User Management Routes - COUSR00, COUSR01, COUSR02, COUSR03 BMS Mapsets
  USER_LIST: '/users/list',
  USER_CREATE: '/users/create',
  USER_UPDATE: '/users/update',
  USER_DELETE: '/users/delete',
  USER_SEARCH: '/users/search',
  
  // Payment Processing Routes - COBIL00 BMS Mapset
  BILL_PAYMENT: '/payments/bill',
  
  // Reports and Analytics Routes - CORPT00 BMS Mapset
  REPORTS: '/reports/generate',
  REPORT_VIEW: '/reports/view',
  
  // Exit and Navigation Support Routes
  EXIT_PATHS: {
    // F3 Exit patterns - Return to previous screen
    RETURN_TO_MENU: '/menu/main',
    RETURN_TO_ADMIN: '/menu/admin',
    RETURN_TO_ACCOUNT_VIEW: '/accounts/view',
    RETURN_TO_CARD_LIST: '/cards/list',
    RETURN_TO_TRANSACTION_LIST: '/transactions/list',
    RETURN_TO_USER_LIST: '/users/list',
    
    // F12 Cancel patterns - Return to main menu
    CANCEL_TO_MENU: '/menu/main',
    CANCEL_TO_ADMIN: '/menu/admin',
    
    // Session termination
    SESSION_TIMEOUT: '/auth/login',
    FORCE_LOGOUT: '/auth/login',
  },
  
  // Dynamic Route Parameters for COMMAREA Data Replacement
  PARAMETERIZED_ROUTES: {
    // Account-specific routes with account ID parameter
    ACCOUNT_VIEW_WITH_ID: '/accounts/view/:accountId',
    ACCOUNT_UPDATE_WITH_ID: '/accounts/update/:accountId',
    
    // Card-specific routes with card number parameter
    CARD_UPDATE_WITH_ID: '/cards/update/:cardNumber',
    CARD_SELECT_WITH_ID: '/cards/select/:cardNumber',
    
    // Transaction-specific routes with transaction ID parameter
    TRANSACTION_DETAIL_WITH_ID: '/transactions/detail/:transactionId',
    
    // User-specific routes with user ID parameter
    USER_UPDATE_WITH_ID: '/users/update/:userId',
    USER_DELETE_WITH_ID: '/users/delete/:userId',
    
    // Report-specific routes with report ID parameter
    REPORT_VIEW_WITH_ID: '/reports/view/:reportId',
  },
} as const;

// ==========================================
// Navigation Flow Constants - XCTL Pattern Mapping
// ==========================================

/**
 * Navigation flow patterns extracted from original CICS XCTL program control.
 * Defines screen-to-screen navigation sequences, return paths, and menu hierarchy
 * to preserve pseudo-conversational processing behavior in React Router implementation.
 * 
 * XCTL Patterns: Direct equivalents of EXEC CICS XCTL PROGRAM commands
 * Return Paths: F3/Back button navigation targets
 * Menu Hierarchy: Role-based menu structure with permission filtering
 */
export const NAVIGATION_FLOW = {
  // CICS XCTL Program Control Patterns
  XCTL_PATTERNS: {
    // Authentication Flow - From COSGN00 BMS
    LOGIN_TO_MAIN_MENU: {
      from: ROUTES.LOGIN,
      to: ROUTES.MENU,
      condition: 'USER_AUTHENTICATED',
      userRole: 'USER',
      preserveState: false,
    },
    LOGIN_TO_ADMIN_MENU: {
      from: ROUTES.LOGIN,
      to: ROUTES.ADMIN_MENU,
      condition: 'ADMIN_AUTHENTICATED',
      userRole: 'ADMIN',
      preserveState: false,
    },
    
    // Main Menu Navigation - From COMEN01 BMS
    MENU_TO_ACCOUNT_VIEW: {
      from: ROUTES.MENU,
      to: ROUTES.ACCOUNT_VIEW,
      condition: 'OPTION_1_SELECTED',
      menuOption: 1,
      preserveState: true,
    },
    MENU_TO_ACCOUNT_UPDATE: {
      from: ROUTES.MENU,
      to: ROUTES.ACCOUNT_UPDATE,
      condition: 'OPTION_2_SELECTED',
      menuOption: 2,
      preserveState: true,
    },
    MENU_TO_CARD_LIST: {
      from: ROUTES.MENU,
      to: ROUTES.CARD_LIST,
      condition: 'OPTION_3_SELECTED',
      menuOption: 3,
      preserveState: true,
    },
    MENU_TO_TRANSACTION_LIST: {
      from: ROUTES.MENU,
      to: ROUTES.TRANSACTION_LIST,
      condition: 'OPTION_4_SELECTED',
      menuOption: 4,
      preserveState: true,
    },
    MENU_TO_BILL_PAYMENT: {
      from: ROUTES.MENU,
      to: ROUTES.BILL_PAYMENT,
      condition: 'OPTION_5_SELECTED',
      menuOption: 5,
      preserveState: true,
    },
    MENU_TO_REPORTS: {
      from: ROUTES.MENU,
      to: ROUTES.REPORTS,
      condition: 'OPTION_6_SELECTED',
      menuOption: 6,
      preserveState: true,
    },
    MENU_TO_ADMIN_MENU: {
      from: ROUTES.MENU,
      to: ROUTES.ADMIN_MENU,
      condition: 'OPTION_7_SELECTED',
      menuOption: 7,
      userRole: 'ADMIN',
      preserveState: false,
    },
    
    // Admin Menu Navigation - From COADM01 BMS
    ADMIN_TO_USER_LIST: {
      from: ROUTES.ADMIN_MENU,
      to: ROUTES.USER_LIST,
      condition: 'OPTION_1_SELECTED',
      menuOption: 1,
      userRole: 'ADMIN',
      preserveState: true,
    },
    ADMIN_TO_REPORTS: {
      from: ROUTES.ADMIN_MENU,
      to: ROUTES.REPORTS,
      condition: 'OPTION_2_SELECTED',
      menuOption: 2,
      userRole: 'ADMIN',
      preserveState: true,
    },
    ADMIN_TO_MAIN_MENU: {
      from: ROUTES.ADMIN_MENU,
      to: ROUTES.MENU,
      condition: 'OPTION_3_SELECTED',
      menuOption: 3,
      preserveState: false,
    },
    
    // Account Management Flow - From COACTVW and COACTUP BMS
    ACCOUNT_VIEW_TO_UPDATE: {
      from: ROUTES.ACCOUNT_VIEW,
      to: ROUTES.ACCOUNT_UPDATE,
      condition: 'UPDATE_SELECTED',
      preserveState: true,
      passParameter: 'accountId',
    },
    
    // Card Management Flow - From COCRDLI, COCRDSL, COCRDUP BMS
    CARD_LIST_TO_SELECT: {
      from: ROUTES.CARD_LIST,
      to: ROUTES.CARD_SELECT,
      condition: 'CARD_SELECTED',
      preserveState: true,
      passParameter: 'cardNumber',
    },
    CARD_LIST_TO_UPDATE: {
      from: ROUTES.CARD_LIST,
      to: ROUTES.CARD_UPDATE,
      condition: 'UPDATE_SELECTED',
      preserveState: true,
      passParameter: 'cardNumber',
    },
    CARD_SELECT_TO_UPDATE: {
      from: ROUTES.CARD_SELECT,
      to: ROUTES.CARD_UPDATE,
      condition: 'UPDATE_SELECTED',
      preserveState: true,
      passParameter: 'cardNumber',
    },
    
    // Transaction Processing Flow - From COTRN00, COTRN01, COTRN02 BMS
    TRANSACTION_LIST_TO_DETAIL: {
      from: ROUTES.TRANSACTION_LIST,
      to: ROUTES.TRANSACTION_DETAIL,
      condition: 'TRANSACTION_SELECTED',
      preserveState: true,
      passParameter: 'transactionId',
    },
    TRANSACTION_LIST_TO_ADD: {
      from: ROUTES.TRANSACTION_LIST,
      to: ROUTES.TRANSACTION_ADD,
      condition: 'ADD_SELECTED',
      preserveState: true,
    },
    
    // User Management Flow - From COUSR00, COUSR01, COUSR02, COUSR03 BMS
    USER_LIST_TO_CREATE: {
      from: ROUTES.USER_LIST,
      to: ROUTES.USER_CREATE,
      condition: 'CREATE_SELECTED',
      userRole: 'ADMIN',
      preserveState: true,
    },
    USER_LIST_TO_UPDATE: {
      from: ROUTES.USER_LIST,
      to: ROUTES.USER_UPDATE,
      condition: 'UPDATE_SELECTED',
      userRole: 'ADMIN',
      preserveState: true,
      passParameter: 'userId',
    },
    USER_LIST_TO_DELETE: {
      from: ROUTES.USER_LIST,
      to: ROUTES.USER_DELETE,
      condition: 'DELETE_SELECTED',
      userRole: 'ADMIN',
      preserveState: true,
      passParameter: 'userId',
    },
  },
  
  // Return Navigation Paths - F3 Exit Button Functionality
  RETURN_PATHS: {
    // Account Management Returns
    [ROUTES.ACCOUNT_VIEW]: ROUTES.MENU,
    [ROUTES.ACCOUNT_UPDATE]: ROUTES.ACCOUNT_VIEW,
    [ROUTES.ACCOUNT_SEARCH]: ROUTES.MENU,
    
    // Card Management Returns
    [ROUTES.CARD_LIST]: ROUTES.MENU,
    [ROUTES.CARD_SEARCH]: ROUTES.CARD_LIST,
    [ROUTES.CARD_UPDATE]: ROUTES.CARD_LIST,
    [ROUTES.CARD_SELECT]: ROUTES.CARD_LIST,
    
    // Transaction Processing Returns
    [ROUTES.TRANSACTION_LIST]: ROUTES.MENU,
    [ROUTES.TRANSACTION_DETAIL]: ROUTES.TRANSACTION_LIST,
    [ROUTES.TRANSACTION_ADD]: ROUTES.TRANSACTION_LIST,
    [ROUTES.TRANSACTION_SEARCH]: ROUTES.TRANSACTION_LIST,
    
    // User Management Returns
    [ROUTES.USER_LIST]: ROUTES.ADMIN_MENU,
    [ROUTES.USER_CREATE]: ROUTES.USER_LIST,
    [ROUTES.USER_UPDATE]: ROUTES.USER_LIST,
    [ROUTES.USER_DELETE]: ROUTES.USER_LIST,
    [ROUTES.USER_SEARCH]: ROUTES.USER_LIST,
    
    // Payment Processing Returns
    [ROUTES.BILL_PAYMENT]: ROUTES.MENU,
    
    // Reports Returns
    [ROUTES.REPORTS]: ROUTES.ADMIN_MENU,
    [ROUTES.REPORT_VIEW]: ROUTES.REPORTS,
    
    // Menu Returns
    [ROUTES.ADMIN_MENU]: ROUTES.LOGIN,
    [ROUTES.MENU]: ROUTES.LOGIN,
  },
  
  // Menu Hierarchy - Role-Based Navigation Structure
  MENU_HIERARCHY: {
    // Main Menu Structure - COMEN01 BMS
    MAIN_MENU: {
      route: ROUTES.MENU,
      title: 'Main Menu',
      transactionCode: 'COMEN01',
      userRole: 'USER',
      level: 1,
      options: [
        {
          optionNumber: 1,
          displayText: '1. Account View',
          targetRoute: ROUTES.ACCOUNT_VIEW,
          transactionCode: 'COACTVW',
          requiredRole: 'USER',
        },
        {
          optionNumber: 2,
          displayText: '2. Account Update',
          targetRoute: ROUTES.ACCOUNT_UPDATE,
          transactionCode: 'COACTUP',
          requiredRole: 'USER',
        },
        {
          optionNumber: 3,
          displayText: '3. Card List',
          targetRoute: ROUTES.CARD_LIST,
          transactionCode: 'COCRDLI',
          requiredRole: 'USER',
        },
        {
          optionNumber: 4,
          displayText: '4. Transaction List',
          targetRoute: ROUTES.TRANSACTION_LIST,
          transactionCode: 'COTRN00',
          requiredRole: 'USER',
        },
        {
          optionNumber: 5,
          displayText: '5. Bill Payment',
          targetRoute: ROUTES.BILL_PAYMENT,
          transactionCode: 'COBIL00',
          requiredRole: 'USER',
        },
        {
          optionNumber: 6,
          displayText: '6. Reports',
          targetRoute: ROUTES.REPORTS,
          transactionCode: 'CORPT00',
          requiredRole: 'USER',
        },
        {
          optionNumber: 7,
          displayText: '7. Admin Menu',
          targetRoute: ROUTES.ADMIN_MENU,
          transactionCode: 'COADM01',
          requiredRole: 'ADMIN',
        },
      ],
    },
    
    // Admin Menu Structure - COADM01 BMS
    ADMIN_MENU: {
      route: ROUTES.ADMIN_MENU,
      title: 'Admin Menu',
      transactionCode: 'COADM01',
      userRole: 'ADMIN',
      level: 1,
      options: [
        {
          optionNumber: 1,
          displayText: '1. User Management',
          targetRoute: ROUTES.USER_LIST,
          transactionCode: 'COUSR00',
          requiredRole: 'ADMIN',
        },
        {
          optionNumber: 2,
          displayText: '2. Reports',
          targetRoute: ROUTES.REPORTS,
          transactionCode: 'CORPT00',
          requiredRole: 'ADMIN',
        },
        {
          optionNumber: 3,
          displayText: '3. Return to Main Menu',
          targetRoute: ROUTES.MENU,
          transactionCode: 'COMEN01',
          requiredRole: 'ADMIN',
        },
      ],
    },
    
    // Account Management Hierarchy
    ACCOUNT_HIERARCHY: {
      route: ROUTES.ACCOUNT_VIEW,
      title: 'Account Management',
      transactionCode: 'COACTVW',
      userRole: 'USER',
      level: 2,
      parentRoute: ROUTES.MENU,
      subRoutes: [
        {
          route: ROUTES.ACCOUNT_UPDATE,
          title: 'Account Update',
          transactionCode: 'COACTUP',
          level: 3,
        },
        {
          route: ROUTES.ACCOUNT_SEARCH,
          title: 'Account Search',
          transactionCode: 'COACTVW',
          level: 3,
        },
      ],
    },
    
    // Card Management Hierarchy
    CARD_HIERARCHY: {
      route: ROUTES.CARD_LIST,
      title: 'Card Management',
      transactionCode: 'COCRDLI',
      userRole: 'USER',
      level: 2,
      parentRoute: ROUTES.MENU,
      subRoutes: [
        {
          route: ROUTES.CARD_SEARCH,
          title: 'Card Search',
          transactionCode: 'COCRDSL',
          level: 3,
        },
        {
          route: ROUTES.CARD_UPDATE,
          title: 'Card Update',
          transactionCode: 'COCRDUP',
          level: 3,
        },
        {
          route: ROUTES.CARD_SELECT,
          title: 'Card Select',
          transactionCode: 'COCRDSL',
          level: 3,
        },
      ],
    },
    
    // Transaction Processing Hierarchy
    TRANSACTION_HIERARCHY: {
      route: ROUTES.TRANSACTION_LIST,
      title: 'Transaction Processing',
      transactionCode: 'COTRN00',
      userRole: 'USER',
      level: 2,
      parentRoute: ROUTES.MENU,
      subRoutes: [
        {
          route: ROUTES.TRANSACTION_DETAIL,
          title: 'Transaction Detail',
          transactionCode: 'COTRN01',
          level: 3,
        },
        {
          route: ROUTES.TRANSACTION_ADD,
          title: 'Add Transaction',
          transactionCode: 'COTRN02',
          level: 3,
        },
        {
          route: ROUTES.TRANSACTION_SEARCH,
          title: 'Transaction Search',
          transactionCode: 'COTRN00',
          level: 3,
        },
      ],
    },
    
    // User Management Hierarchy
    USER_HIERARCHY: {
      route: ROUTES.USER_LIST,
      title: 'User Management',
      transactionCode: 'COUSR00',
      userRole: 'ADMIN',
      level: 2,
      parentRoute: ROUTES.ADMIN_MENU,
      subRoutes: [
        {
          route: ROUTES.USER_CREATE,
          title: 'Create User',
          transactionCode: 'COUSR01',
          level: 3,
        },
        {
          route: ROUTES.USER_UPDATE,
          title: 'Update User',
          transactionCode: 'COUSR02',
          level: 3,
        },
        {
          route: ROUTES.USER_DELETE,
          title: 'Delete User',
          transactionCode: 'COUSR03',
          level: 3,
        },
        {
          route: ROUTES.USER_SEARCH,
          title: 'Search Users',
          transactionCode: 'COUSR00',
          level: 3,
        },
      ],
    },
  },
} as const;

// ==========================================
// Breadcrumb Navigation Constants
// ==========================================

/**
 * Breadcrumb navigation configuration providing hierarchical path tracking
 * and navigation assistance. Maps each route to its breadcrumb representation
 * with parent-child relationships and display information.
 * 
 * Implements F3/Back button functionality and navigation path display
 * equivalent to CICS program flow tracking and terminal navigation.
 */
export const BREADCRUMB_PATHS = {
  // Path to Screen Title Mapping
  PATH_MAPPING: {
    [ROUTES.LOGIN]: {
      path: ROUTES.LOGIN,
      title: 'Login',
      transactionCode: 'COSGN00',
      level: 0,
      isActive: false,
    } as BreadcrumbData,
    
    [ROUTES.MENU]: {
      path: ROUTES.MENU,
      title: 'Main Menu',
      transactionCode: 'COMEN01',
      level: 1,
      isActive: false,
    } as BreadcrumbData,
    
    [ROUTES.ADMIN_MENU]: {
      path: ROUTES.ADMIN_MENU,
      title: 'Admin Menu',
      transactionCode: 'COADM01',
      level: 1,
      isActive: false,
    } as BreadcrumbData,
    
    [ROUTES.ACCOUNT_VIEW]: {
      path: ROUTES.ACCOUNT_VIEW,
      title: 'Account View',
      transactionCode: 'COACTVW',
      level: 2,
      isActive: false,
    } as BreadcrumbData,
    
    [ROUTES.ACCOUNT_UPDATE]: {
      path: ROUTES.ACCOUNT_UPDATE,
      title: 'Account Update',
      transactionCode: 'COACTUP',
      level: 3,
      isActive: false,
    } as BreadcrumbData,
    
    [ROUTES.CARD_LIST]: {
      path: ROUTES.CARD_LIST,
      title: 'Card List',
      transactionCode: 'COCRDLI',
      level: 2,
      isActive: false,
    } as BreadcrumbData,
    
    [ROUTES.CARD_UPDATE]: {
      path: ROUTES.CARD_UPDATE,
      title: 'Card Update',
      transactionCode: 'COCRDUP',
      level: 3,
      isActive: false,
    } as BreadcrumbData,
    
    [ROUTES.TRANSACTION_LIST]: {
      path: ROUTES.TRANSACTION_LIST,
      title: 'Transaction List',
      transactionCode: 'COTRN00',
      level: 2,
      isActive: false,
    } as BreadcrumbData,
    
    [ROUTES.TRANSACTION_DETAIL]: {
      path: ROUTES.TRANSACTION_DETAIL,
      title: 'Transaction Detail',
      transactionCode: 'COTRN01',
      level: 3,
      isActive: false,
    } as BreadcrumbData,
    
    [ROUTES.TRANSACTION_ADD]: {
      path: ROUTES.TRANSACTION_ADD,
      title: 'Add Transaction',
      transactionCode: 'COTRN02',
      level: 3,
      isActive: false,
    } as BreadcrumbData,
    
    [ROUTES.USER_LIST]: {
      path: ROUTES.USER_LIST,
      title: 'User List',
      transactionCode: 'COUSR00',
      level: 2,
      isActive: false,
    } as BreadcrumbData,
    
    [ROUTES.USER_CREATE]: {
      path: ROUTES.USER_CREATE,
      title: 'Create User',
      transactionCode: 'COUSR01',
      level: 3,
      isActive: false,
    } as BreadcrumbData,
    
    [ROUTES.USER_UPDATE]: {
      path: ROUTES.USER_UPDATE,
      title: 'Update User',
      transactionCode: 'COUSR02',
      level: 3,
      isActive: false,
    } as BreadcrumbData,
    
    [ROUTES.BILL_PAYMENT]: {
      path: ROUTES.BILL_PAYMENT,
      title: 'Bill Payment',
      transactionCode: 'COBIL00',
      level: 2,
      isActive: false,
    } as BreadcrumbData,
    
    [ROUTES.REPORTS]: {
      path: ROUTES.REPORTS,
      title: 'Reports',
      transactionCode: 'CORPT00',
      level: 2,
      isActive: false,
    } as BreadcrumbData,
  },
  
  // Screen Title Constants from BMS Mapsets
  SCREEN_TITLES: {
    // Authentication Screens
    LOGIN_SCREEN: 'CardDemo - Login Screen',
    LOGOUT_SCREEN: 'CardDemo - Logout',
    
    // Menu Screens
    MAIN_MENU_SCREEN: 'CardDemo - Main Menu Screen',
    ADMIN_MENU_SCREEN: 'CardDemo - Admin Menu Screen',
    
    // Account Management Screens
    ACCOUNT_VIEW_SCREEN: 'CardDemo - Account Viewer Screen',
    ACCOUNT_UPDATE_SCREEN: 'CardDemo - Account Update Screen',
    ACCOUNT_SEARCH_SCREEN: 'CardDemo - Account Search Screen',
    
    // Card Management Screens
    CARD_LIST_SCREEN: 'CardDemo - Card Listing Screen',
    CARD_SEARCH_SCREEN: 'CardDemo - Card Search Screen',
    CARD_UPDATE_SCREEN: 'CardDemo - Card Update Screen',
    CARD_SELECT_SCREEN: 'CardDemo - Card Selection Screen',
    
    // Transaction Processing Screens
    TRANSACTION_LIST_SCREEN: 'CardDemo - Transaction List',
    TRANSACTION_DETAIL_SCREEN: 'CardDemo - Transaction Detail',
    TRANSACTION_ADD_SCREEN: 'CardDemo - Add Transaction',
    TRANSACTION_SEARCH_SCREEN: 'CardDemo - Transaction Search',
    
    // User Management Screens
    USER_LIST_SCREEN: 'CardDemo - User List Screen',
    USER_CREATE_SCREEN: 'CardDemo - User Create Screen',
    USER_UPDATE_SCREEN: 'CardDemo - User Update Screen',
    USER_DELETE_SCREEN: 'CardDemo - User Delete Screen',
    USER_SEARCH_SCREEN: 'CardDemo - User Search Screen',
    
    // Payment Processing Screens
    BILL_PAYMENT_SCREEN: 'CardDemo - Bill Payment Screen',
    
    // Reports Screens
    REPORTS_SCREEN: 'CardDemo - Report Generation Screen',
    REPORT_VIEW_SCREEN: 'CardDemo - Report View Screen',
  },
  
  // Hierarchical Navigation Levels
  HIERARCHY_LEVELS: {
    // Level 0: Authentication
    AUTHENTICATION_LEVEL: 0,
    
    // Level 1: Main Navigation
    MAIN_NAVIGATION_LEVEL: 1,
    
    // Level 2: Functional Areas
    FUNCTIONAL_AREA_LEVEL: 2,
    
    // Level 3: Detail Operations
    DETAIL_OPERATION_LEVEL: 3,
    
    // Level 4: Sub-operations
    SUB_OPERATION_LEVEL: 4,
    
    // Maximum navigation depth
    MAX_DEPTH: 4,
    
    // Breadcrumb display configuration
    BREADCRUMB_CONFIG: {
      // Maximum breadcrumb items to display
      MAX_BREADCRUMB_ITEMS: 5,
      
      // Separator character between breadcrumb items
      BREADCRUMB_SEPARATOR: ' > ',
      
      // Show transaction codes in breadcrumbs
      SHOW_TRANSACTION_CODES: true,
      
      // Enable breadcrumb navigation clicks
      ENABLE_BREADCRUMB_NAVIGATION: true,
      
      // Home breadcrumb configuration
      HOME_BREADCRUMB: {
        path: ROUTES.MENU,
        title: 'Home',
        transactionCode: 'COMEN01',
        level: 0,
        isActive: false,
      } as BreadcrumbData,
    },
  },
} as const;

// ==========================================
// Utility Functions for Navigation
// ==========================================

/**
 * Navigation utility functions providing programmatic access to navigation
 * constants and route resolution for React Router integration.
 */
export const NavigationUtils = {
  /**
   * Get breadcrumb data for a given route path
   */
  getBreadcrumbData(path: string): BreadcrumbData | null {
    return BREADCRUMB_PATHS.PATH_MAPPING[path as keyof typeof BREADCRUMB_PATHS.PATH_MAPPING] || null;
  },
  
  /**
   * Get return path for F3 exit navigation
   */
  getReturnPath(currentPath: string): string {
    return NAVIGATION_FLOW.RETURN_PATHS[currentPath as keyof typeof NAVIGATION_FLOW.RETURN_PATHS] || ROUTES.MENU;
  },
  
  /**
   * Get screen title for a given route
   */
  getScreenTitle(path: string): string {
    const breadcrumbData = this.getBreadcrumbData(path);
    if (breadcrumbData) {
      const titleKey = `${breadcrumbData.title.toUpperCase().replace(' ', '_')}_SCREEN` as keyof typeof BREADCRUMB_PATHS.SCREEN_TITLES;
      return BREADCRUMB_PATHS.SCREEN_TITLES[titleKey] || breadcrumbData.title;
    }
    return 'CardDemo Application';
  },
  
  /**
   * Check if route requires admin privileges
   */
  requiresAdminRole(path: string): boolean {
    return path.includes('/admin') || 
           path.includes('/users') || 
           path === ROUTES.ADMIN_MENU || 
           path === ROUTES.REPORTS;
  },
  
  /**
   * Get menu hierarchy for a given user role
   */
  getMenuHierarchy(userRole: 'ADMIN' | 'USER' | 'GUEST') {
    if (userRole === 'ADMIN') {
      return NAVIGATION_FLOW.MENU_HIERARCHY.ADMIN_MENU;
    }
    return NAVIGATION_FLOW.MENU_HIERARCHY.MAIN_MENU;
  },
  
  /**
   * Generate breadcrumb trail for a route path
   */
  generateBreadcrumbTrail(currentPath: string): BreadcrumbData[] {
    const breadcrumbData = this.getBreadcrumbData(currentPath);
    if (!breadcrumbData) {
      return [];
    }
    
    const trail: BreadcrumbData[] = [];
    
    // Add home breadcrumb
    trail.push(BREADCRUMB_PATHS.HIERARCHY_LEVELS.BREADCRUMB_CONFIG.HOME_BREADCRUMB);
    
    // Add intermediate breadcrumbs based on hierarchy level
    if (breadcrumbData.level > 1) {
      const returnPath = this.getReturnPath(currentPath);
      const parentBreadcrumb = this.getBreadcrumbData(returnPath);
      if (parentBreadcrumb && parentBreadcrumb.level > 0) {
        trail.push(parentBreadcrumb);
      }
    }
    
    // Add current breadcrumb
    trail.push({ ...breadcrumbData, isActive: true });
    
    return trail;
  },
} as const;

// Export all navigation constants for global access
export default {
  ROUTES,
  NAVIGATION_FLOW,
  BREADCRUMB_PATHS,
  NavigationUtils,
};