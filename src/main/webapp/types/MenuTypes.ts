/**
 * MenuTypes.ts
 * 
 * TypeScript interface definitions for menu navigation screens (COMEN01/COADM01) 
 * including role-based menu options, navigation state management, and administrative 
 * access control types.
 * 
 * This file provides comprehensive type definitions for implementing React Router-based
 * menu navigation that preserves the exact behavior of CICS transaction processing
 * while enabling modern role-based menu generation with Spring Security integration.
 * 
 * Key Features:
 * - Dynamic menu generation based on user roles and permissions
 * - Administrative access controls with audit requirements
 * - Navigation state preservation for CICS XCTL program flow
 * - Role-based menu visibility matching Spring Security authorities
 * - BMS field attribute mapping for Material-UI integration
 * 
 * Original BMS Maps:
 * - COMEN01.bms → Main Menu (12 options, role-based filtering)
 * - COADM01.bms → Admin Menu (12 options, administrative functions)
 * 
 * Copyright (c) 2023 CardDemo Application
 * Technology transformation: COBOL/CICS/BMS → Java/Spring Boot/React
 */

import { FormFieldAttributes } from './CommonTypes';
import { RouteDefinition } from './NavigationTypes';

/**
 * Menu Type Enumeration
 * 
 * Defines the different types of menus available in the system,
 * corresponding to the original BMS mapsets.
 */
export type MenuType = 'MAIN' | 'ADMIN';

/**
 * User Role Enumeration
 * 
 * Defines user roles for menu visibility and access control,
 * matching Spring Security authority hierarchy.
 */
export type UserRole = 'ADMIN' | 'USER' | 'GUEST';

/**
 * Administrative Level Enumeration
 * 
 * Defines different levels of administrative access for admin menu functions,
 * providing granular control over administrative capabilities.
 */
export type AdminLevel = 'SUPER_ADMIN' | 'ADMIN' | 'SUPERVISOR' | 'OPERATOR';

/**
 * Menu Option Data Interface
 * 
 * Defines individual menu options for the main menu (COMEN01), including
 * role-based visibility, authentication requirements, and navigation targets.
 * Each option maps to one of the 12 OPTN001-OPTN012 fields in the BMS definition.
 */
export interface MenuOptionData {
  /** Option number (1-12) corresponding to BMS OPTN001-OPTN012 fields */
  optionNumber: number;
  
  /** Display text for the menu option (40 characters max, matching BMS LENGTH=40) */
  optionText: string;
  
  /** Original CICS transaction code for API traceability */
  transactionCode: string;
  
  /** React Router path for navigation */
  routePath: string;
  
  /** Indicates if option is visible to current user role */
  isVisible: boolean;
  
  /** Indicates if option requires authentication */
  requiresAuth: boolean;
  
  /** Array of user roles that can access this option */
  userRoles: UserRole[];
  
  /** Detailed description of the option functionality */
  description: string;
  
  /** Indicates if option is currently enabled/selectable */
  isEnabled: boolean;
  
  /** Keyboard shortcut key (1-12) for quick access */
  keyboardShortcut: string;
  
  /** BMS field attributes for styling and validation */
  fieldAttributes: FormFieldAttributes;
}

/**
 * Admin Menu Data Interface
 * 
 * Defines administrative menu options for the admin menu (COADM01), including
 * administrative level requirements, permissions, and audit tracking.
 */
export interface AdminMenuData {
  /** Option number (1-12) corresponding to BMS OPTN001-OPTN012 fields */
  optionNumber: number;
  
  /** Display text for the admin menu option (40 characters max) */
  optionText: string;
  
  /** Original CICS transaction code for API traceability */
  transactionCode: string;
  
  /** React Router path for navigation */
  routePath: string;
  
  /** Required administrative level for access */
  adminLevel: AdminLevel;
  
  /** Specific permissions required for this option */
  permissions: string[];
  
  /** Access control requirements and restrictions */
  accessControl: {
    /** Minimum security clearance required */
    minSecurityLevel: number;
    /** IP restrictions if applicable */
    ipRestrictions?: string[];
    /** Time-based access restrictions */
    timeRestrictions?: {
      /** Start time for access (24-hour format) */
      startTime: string;
      /** End time for access (24-hour format) */
      endTime: string;
    };
  };
  
  /** Indicates if audit logging is required for this option */
  auditRequired: boolean;
  
  /** Indicates if option is visible to current admin user */
  isVisible: boolean;
  
  /** Indicates if option is currently enabled/selectable */
  isEnabled: boolean;
  
  /** Detailed description of the administrative function */
  description: string;
  
  /** BMS field attributes for styling and validation */
  fieldAttributes: FormFieldAttributes;
}

/**
 * Navigation State Interface
 * 
 * Manages navigation state for React Router integration while preserving
 * CICS XCTL program flow patterns and pseudo-conversational processing.
 */
export interface NavigationState {
  /** Current menu type being displayed */
  currentMenu: MenuType;
  
  /** Currently selected option (1-12 or empty string) */
  selectedOption: string;
  
  /** Navigation context for session and state management */
  navigationContext: {
    /** Current route path */
    currentRoute: string;
    /** Previous route for back navigation */
    previousRoute?: string;
    /** Session identifier for Redis session management */
    sessionId: string;
    /** Transaction correlation ID */
    transactionId: string;
  };
  
  /** Menu navigation history for breadcrumb and back functionality */
  menuHistory: {
    /** Route path */
    route: string;
    /** Menu type */
    menuType: MenuType;
    /** Timestamp of navigation */
    timestamp: Date;
    /** Selected option at time of navigation */
    selectedOption?: string;
  }[];
  
  /** Return path for F3/Exit navigation (equivalent to CICS RETURN) */
  returnPath: string;
  
  /** Session state preservation for pseudo-conversational processing */
  sessionState: {
    /** JWT authentication token */
    token: string;
    /** User role for menu filtering */
    userRole: UserRole;
    /** Session expiration timestamp */
    expires: Date;
    /** User preferences */
    preferences: Record<string, any>;
  };
  
  /** Current error message (equivalent to BMS ERRMSG field) */
  errorMessage: string;
  
  /** Validation state for form fields */
  validationState: {
    /** Option field validation status */
    optionValid: boolean;
    /** Validation error message */
    validationError?: string;
    /** Last validation timestamp */
    lastValidated?: Date;
  };
  
  /** Form data including user input */
  formData: {
    /** Selected option value (2 characters max, matching BMS field) */
    selectedOption: string;
    /** Any additional form state */
    additionalData?: Record<string, any>;
  };
}

/**
 * Role-Based Menu Configuration Interface
 * 
 * Configures menu visibility and options based on user roles,
 * implementing Spring Security authority-based access control.
 */
export interface RoleBasedMenuConfig {
  /** Type of menu being configured */
  menuType: MenuType;
  
  /** User role for configuration */
  userRole: UserRole;
  
  /** Available menu options for this role */
  availableOptions: MenuOptionData[] | AdminMenuData[];
  
  /** Menu options that are restricted for this role */
  restrictedOptions: number[];
  
  /** Default option to select/highlight */
  defaultOption: number;
  
  /** Maximum menu level access (for hierarchical menus) */
  maxMenuLevel: number;
  
  /** Role hierarchy for inheritance of permissions */
  roleHierarchy: {
    /** Current role */
    currentRole: UserRole;
    /** Parent roles that inherit permissions */
    parentRoles: UserRole[];
    /** Child roles that inherit from current role */
    childRoles: UserRole[];
  };
  
  /** Permission matrix for fine-grained access control */
  permissionMatrix: {
    /** Permission name */
    permission: string;
    /** Granted/denied status */
    granted: boolean;
    /** Source of permission (role, group, user) */
    source: string;
  }[];
  
  /** Menu generation configuration */
  menuGeneration: {
    /** Show only enabled options */
    hideDisabledOptions: boolean;
    /** Show option numbers */
    showOptionNumbers: boolean;
    /** Enable keyboard shortcuts */
    enableKeyboardShortcuts: boolean;
    /** Custom menu title */
    customTitle?: string;
  };
}

/**
 * Main Menu Screen Data Interface
 * 
 * Complete screen data structure for the main menu (COMEN01),
 * including all BMS fields and React component state.
 */
export interface MainMenuScreenData {
  /** Base screen data (headers, titles, date/time) */
  baseScreenData: {
    /** Transaction name (TRNNAME field) */
    trnname: string;
    /** Program name (PGMNAME field) */
    pgmname: string;
    /** Current date (CURDATE field) */
    curdate: string;
    /** Current time (CURTIME field) */
    curtime: string;
    /** Primary title (TITLE01 field) */
    title01: string;
    /** Secondary title (TITLE02 field) */
    title02: string;
  };
  
  /** Array of menu options (OPTN001-OPTN012 fields) */
  menuOptions: MenuOptionData[];
  
  /** Currently selected option (OPTION field) */
  selectedOption: string;
  
  /** Error message (ERRMSG field) */
  errorMessage: string;
  
  /** Navigation state for React Router integration */
  navigationState: NavigationState;
  
  /** Current user role for menu filtering */
  userRole: UserRole;
  
  /** Available transactions for current user */
  availableTransactions: string[];
  
  /** Field attributes for all BMS fields */
  fieldAttributes: {
    /** Option input field attributes */
    optionField: FormFieldAttributes;
    /** Error message field attributes */
    errorField: FormFieldAttributes;
    /** Menu option field attributes */
    menuOptionFields: FormFieldAttributes[];
  };
}

/**
 * Admin Menu Screen Data Interface
 * 
 * Complete screen data structure for the admin menu (COADM01),
 * including all BMS fields and administrative controls.
 */
export interface AdminMenuScreenData {
  /** Base screen data (headers, titles, date/time) */
  baseScreenData: {
    /** Transaction name (TRNNAME field) */
    trnname: string;
    /** Program name (PGMNAME field) */
    pgmname: string;
    /** Current date (CURDATE field) */
    curdate: string;
    /** Current time (CURTIME field) */
    curtime: string;
    /** Primary title (TITLE01 field) */
    title01: string;
    /** Secondary title (TITLE02 field) */
    title02: string;
  };
  
  /** Array of admin menu options (OPTN001-OPTN012 fields) */
  adminMenuOptions: AdminMenuData[];
  
  /** Currently selected option (OPTION field) */
  selectedOption: string;
  
  /** Error message (ERRMSG field) */
  errorMessage: string;
  
  /** Navigation state for React Router integration */
  navigationState: NavigationState;
  
  /** Current administrative level */
  adminLevel: AdminLevel;
  
  /** Current admin permissions */
  permissions: string[];
  
  /** Audit log for administrative actions */
  auditLog: {
    /** Action performed */
    action: string;
    /** Timestamp of action */
    timestamp: Date;
    /** User who performed action */
    user: string;
    /** Additional details */
    details?: string;
  }[];
  
  /** Field attributes for all BMS fields */
  fieldAttributes: {
    /** Option input field attributes */
    optionField: FormFieldAttributes;
    /** Error message field attributes */
    errorField: FormFieldAttributes;
    /** Admin menu option field attributes */
    adminMenuOptionFields: FormFieldAttributes[];
  };
}