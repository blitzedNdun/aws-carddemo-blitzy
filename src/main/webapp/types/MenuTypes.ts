/**
 * MenuTypes.ts
 * 
 * TypeScript interface definitions for menu navigation screens (COMEN01/COADM01) 
 * including role-based menu options, navigation state management, and administrative 
 * access control types.
 * 
 * This file maintains exact functional equivalence with original COBOL BMS menu
 * definitions while providing modern TypeScript interfaces for React component
 * development and Spring Security integration.
 * 
 * Key Features Preserved:
 * - Role-based menu option visibility matching RACF security model
 * - Dynamic menu generation based on user permissions and admin levels
 * - Navigation state preservation across React Router transitions
 * - Field validation and keyboard navigation equivalent to BMS behavior
 * - Transaction code mapping for each menu option preserving CICS flow
 * 
 * Based on transformation of:
 * - app/bms/COMEN01.bms: Main menu screen with 12 selectable options
 * - app/bms/COADM01.bms: Admin menu screen with 12 administrative functions
 * - app/cpy-bms/COMEN01.CPY: Main menu data structure definitions
 * - app/cpy-bms/COADM01.CPY: Admin menu data structure definitions
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0
 */

import { FormFieldAttributes } from './CommonTypes';
import { RouteDefinition } from './NavigationTypes';

/**
 * Menu Type Union
 * Defines the available menu types in the CardDemo application
 * Maps to CICS transaction patterns for menu classification
 */
export type MenuType = 'main' | 'admin' | 'user' | 'guest';

/**
 * User Role Union  
 * Defines user role hierarchy matching Spring Security authorities
 * Replaces RACF group membership with modern role-based access control
 */
export type UserRole = 'ADMIN' | 'USER' | 'GUEST' | 'READONLY';

/**
 * Admin Level Union
 * Defines administrative privilege levels for hierarchical access control
 * Enables fine-grained permission management within admin menu options
 */
export type AdminLevel = 'SUPER_ADMIN' | 'ADMIN' | 'POWER_USER' | 'OPERATOR';

/**
 * Menu Option Data Interface
 * 
 * Defines individual menu option structure for COMEN01 main menu screen
 * with dynamic generation based on user roles and permissions.
 * 
 * Based on COMEN01.bms analysis:
 * - 12 menu options (OPTN001-OPTN012) with 40-character display text
 * - Numeric option selection (1-12) via OPTION field 
 * - Role-based visibility and access control
 * - Transaction code mapping for navigation routing
 * - Field attributes preservation from BMS DFHMDF definitions
 * 
 * Maps to Spring Security integration:
 * - userRoles array matches @PreAuthorize annotation role checks
 * - requiresAuth enables authentication requirement enforcement
 * - isVisible controls runtime menu option display based on permissions
 */
export interface MenuOptionData {
  /** Numeric option identifier (1-12) matching BMS option numbering */
  optionNumber: number;
  
  /** Menu option display text (40 characters max, from BMS INITIAL) */
  optionText: string;
  
  /** CICS transaction code for this menu option (4 characters) */
  transactionCode: string;
  
  /** React Router path for navigation when option is selected */
  routePath: string;
  
  /** Controls runtime visibility based on user permissions */
  isVisible: boolean;
  
  /** Indicates if authentication is required to access this option */
  requiresAuth: boolean;
  
  /** Array of user roles authorized to access this menu option */
  userRoles: UserRole[];
  
  /** Detailed description of menu option functionality */
  description: string;
  
  /** Controls whether option can be selected (disabled state) */
  isEnabled: boolean;
  
  /** Keyboard shortcut for quick access (e.g., 'Alt+1' for option 1) */
  keyboardShortcut?: string;
  
  /** BMS field attributes for proper rendering and validation */
  fieldAttributes: FormFieldAttributes;
}

/**
 * Admin Menu Data Interface
 * 
 * Defines administrative menu option structure for COADM01 admin menu screen
 * with hierarchical access control and audit trail requirements.
 * 
 * Based on COADM01.bms analysis:
 * - 12 admin options (OPTN001-OPTN012) with administrative functions
 * - Hierarchical permission structure with admin level requirements
 * - Audit logging requirements for sensitive administrative operations
 * - Access control matrix for fine-grained permission management
 * - Enhanced security validation beyond standard menu options
 * 
 * Administrative Features:
 * - adminLevel controls minimum privilege level required
 * - permissions array enables granular access control
 * - accessControl provides IP restrictions and time-based access
 * - auditRequired ensures comprehensive logging for compliance
 */
export interface AdminMenuData {
  /** Numeric option identifier (1-12) matching BMS admin option numbering */
  optionNumber: number;
  
  /** Admin menu option display text (40 characters max) */
  optionText: string;
  
  /** CICS transaction code for this administrative function */
  transactionCode: string;
  
  /** React Router path for admin function navigation */
  routePath: string;
  
  /** Minimum administrative level required to access this option */
  adminLevel: AdminLevel;
  
  /** Specific permissions required beyond admin level */
  permissions: string[];
  
  /** Access control restrictions (IP whitelist, time windows, etc.) */
  accessControl: {
    /** Allowed IP address ranges for enhanced security */
    allowedIpRanges?: string[];
    /** Time window restrictions (e.g., business hours only) */
    timeRestrictions?: {
      startHour: number;
      endHour: number;
      allowedDays: number[]; // 0-6, Sunday=0
    };
    /** Maximum concurrent sessions for this admin function */
    maxConcurrentSessions?: number;
  };
  
  /** Indicates if this operation requires audit trail logging */
  auditRequired: boolean;
  
  /** Controls runtime visibility based on admin permissions */
  isVisible: boolean;
  
  /** Controls whether admin option can be selected */
  isEnabled: boolean;
  
  /** Detailed description of administrative function */
  description: string;
  
  /** BMS field attributes for proper rendering and validation */
  fieldAttributes: FormFieldAttributes;
}

/**
 * Navigation State Interface
 * 
 * Manages comprehensive navigation state for React Router integration while
 * preserving CICS XCTL program flow patterns and pseudo-conversational processing.
 * 
 * Preserves CICS Navigation Patterns:
 * - currentMenu tracks active menu equivalent to CICS terminal control
 * - selectedOption maintains user selection across screen transitions
 * - navigationContext preserves pseudo-conversational state
 * - menuHistory enables F3=Exit and back navigation functionality
 * - returnPath supports CICS RETURN TRANSID behavior
 * - sessionState maintains data across navigation transitions
 * 
 * React Router Integration:
 * - Seamless integration with React Router location and history objects
 * - State preservation during route transitions and browser navigation
 * - Form data persistence during multi-screen workflows
 * - Validation state maintenance across navigation boundaries
 */
export interface NavigationState {
  /** Currently active menu identifier (main, admin, etc.) */
  currentMenu: MenuType;
  
  /** Currently selected menu option number (1-12) */
  selectedOption: number | null;
  
  /** Navigation context preserving CICS pseudo-conversational state */
  navigationContext: {
    /** Previous menu for back navigation (F3=Exit functionality) */
    previousMenu: MenuType | null;
    /** Route parameters passed during navigation */
    routeParams: Record<string, string>;
    /** Query string parameters from URL */
    queryParams: Record<string, string>;
    /** Navigation timestamp for session management */
    timestamp: Date;
  };
  
  /** Menu navigation history for breadcrumb and back button support */
  menuHistory: Array<{
    /** Menu type in navigation history */
    menuType: MenuType;
    /** Selected option at this history point */
    selectedOption: number | null;
    /** Timestamp of this navigation event */
    timestamp: Date;
    /** Route path at this history point */
    routePath: string;
  }>;
  
  /** Return path for F3=Exit and Cancel navigation */
  returnPath: string | null;
  
  /** Session state data preserved across navigation */
  sessionState: {
    /** User session identifier */
    sessionId: string;
    /** Session creation timestamp */
    startTime: Date;
    /** Last activity timestamp for timeout management */
    lastActivity: Date;
    /** User authentication status */
    isAuthenticated: boolean;
    /** Current user role for permission checking */
    userRole: UserRole;
    /** Administrative level (if applicable) */
    adminLevel?: AdminLevel;
  };
  
  /** Current error message for display in ERRMSG field */
  errorMessage: string | null;
  
  /** Form validation state across navigation boundaries */
  validationState: {
    /** Field-level validation errors */
    fieldErrors: Record<string, string>;
    /** Form-level validation status */
    isValid: boolean;
    /** Validation timestamp */
    lastValidated: Date | null;
  };
  
  /** Form data preservation during navigation */
  formData: Record<string, any>;
}

/**
 * Role-Based Menu Configuration Interface
 * 
 * Defines comprehensive role-based menu configuration that maps Spring Security
 * authorities to menu visibility and access control, replacing RACF group
 * membership with modern permission-based security model.
 * 
 * Spring Security Integration:
 * - menuType corresponds to @Secured annotation menu access
 * - userRole maps to @PreAuthorize role checking
 * - availableOptions filters menu based on hasRole() expressions
 * - restrictedOptions enforces @DenyAll equivalent restrictions
 * - permissionMatrix enables @PostFilter equivalent data filtering
 * 
 * Features:
 * - Dynamic menu generation based on user permissions
 * - Hierarchical role inheritance (ADMIN inherits USER permissions)
 * - Runtime permission checking with caching for performance
 * - Menu option filtering based on complex permission combinations
 */
export interface RoleBasedMenuConfig {
  /** Type of menu this configuration applies to */
  menuType: MenuType;
  
  /** User role this configuration is designed for */
  userRole: UserRole;
  
  /** Menu options available to this role */
  availableOptions: number[];
  
  /** Menu options explicitly restricted for this role */
  restrictedOptions: number[];
  
  /** Default menu option to highlight/select for this role */
  defaultOption: number | null;
  
  /** Maximum menu access level for hierarchical menus */
  maxMenuLevel: number;
  
  /** Role hierarchy definition for inheritance */
  roleHierarchy: {
    /** Parent roles that this role inherits permissions from */
    inheritsFrom: UserRole[];
    /** Child roles that inherit permissions from this role */
    grantsTo: UserRole[];
  };
  
  /** Permission matrix for fine-grained access control */
  permissionMatrix: {
    /** Option number mapped to required permissions */
    [optionNumber: number]: {
      /** Required permissions for access */
      requiredPermissions: string[];
      /** Optional permissions for enhanced functionality */
      optionalPermissions: string[];
      /** Conditions that must be met (time, IP, etc.) */
      conditions: Record<string, any>;
    };
  };
  
  /** Menu generation configuration */
  menuGeneration: {
    /** Include disabled options in menu display */
    showDisabled: boolean;
    /** Sort options by priority vs. numeric order */
    sortByPriority: boolean;
    /** Group related options together */
    enableGrouping: boolean;
    /** Custom menu titles based on role */
    customTitles: Record<number, string>;
  };
}

/**
 * Main Menu Screen Data Interface
 * 
 * Complete screen data structure for COMEN01 main menu screen preserving
 * exact BMS field layout and behavior while enabling React component integration.
 * 
 * Based on COMEN01.bms and COMEN01.CPY analysis:
 * - baseScreenData includes standard header fields (TRNNAME, PGMNAME, etc.)
 * - menuOptions array contains all 12 configurable menu options
 * - selectedOption tracks user selection in OPTION field (2 chars, numeric)
 * - errorMessage maps to ERRMSG field (78 characters)
 * - navigationState preserves pseudo-conversational processing
 * - userRole enables role-based menu option filtering
 * - availableTransactions lists valid transaction codes for validation
 * - fieldAttributes maintain BMS field behavior in React components
 */
export interface MainMenuScreenData {
  /** Standard BMS header fields present on all screens */
  baseScreenData: {
    /** Transaction name (4 characters) */
    trnname: string;
    /** Program name (8 characters) */
    pgmname: string;
    /** Current date (8 characters, mm/dd/yy format) */
    curdate: string;
    /** Current time (8 characters, hh:mm:ss format) */
    curtime: string;
    /** Primary screen title (40 characters) */
    title01: string;
    /** Secondary screen title (40 characters) */
    title02: string;
  };
  
  /** Array of 12 menu options with role-based visibility */
  menuOptions: MenuOptionData[];
  
  /** Currently selected option number (1-12, null if none selected) */
  selectedOption: number | null;
  
  /** Error message for display in ERRMSG field (78 characters max) */
  errorMessage: string | null;
  
  /** Complete navigation state for React Router integration */
  navigationState: NavigationState;
  
  /** Current user role for menu filtering */
  userRole: UserRole;
  
  /** Available transaction codes for this user */
  availableTransactions: string[];
  
  /** Field attributes for option selection and display fields */
  fieldAttributes: {
    /** Attributes for menu option display fields (OPTN001-OPTN012) */
    menuOptionsFields: FormFieldAttributes[];
    /** Attributes for option selection field (OPTION) */
    optionSelectionField: FormFieldAttributes;
    /** Attributes for error message field (ERRMSG) */
    errorMessageField: FormFieldAttributes;
  };
}

/**
 * Admin Menu Screen Data Interface
 * 
 * Complete screen data structure for COADM01 admin menu screen with enhanced
 * security controls and audit trail capabilities for administrative functions.
 * 
 * Based on COADM01.bms and COADM01.CPY analysis:
 * - Identical field structure to main menu but with admin-specific options
 * - Enhanced security validation and access control checking
 * - Comprehensive audit logging for administrative operations
 * - Permission matrix integration for fine-grained access control
 * - Administrative session management with elevated timeout handling
 * 
 * Administrative Features:
 * - adminMenuOptions array contains administrative function definitions
 * - adminLevel tracks current user's administrative privilege level
 * - permissions array lists specific admin permissions granted
 * - auditLog maintains session audit trail for compliance
 * - Enhanced field attributes for secure administrative input validation
 */
export interface AdminMenuScreenData {
  /** Standard BMS header fields present on all screens */
  baseScreenData: {
    /** Transaction name (4 characters, typically 'CADM' for admin) */
    trnname: string;
    /** Program name (8 characters) */
    pgmname: string;
    /** Current date (8 characters, mm/dd/yy format) */
    curdate: string;
    /** Current time (8 characters, hh:mm:ss format) */
    curtime: string;
    /** Primary screen title (40 characters, 'Admin Menu') */
    title01: string;
    /** Secondary screen title (40 characters) */
    title02: string;
  };
  
  /** Array of 12 administrative menu options with access control */
  adminMenuOptions: AdminMenuData[];
  
  /** Currently selected admin option number (1-12, null if none selected) */
  selectedOption: number | null;
  
  /** Error message for display in ERRMSG field (78 characters max) */
  errorMessage: string | null;
  
  /** Complete navigation state for React Router integration */
  navigationState: NavigationState;
  
  /** Current user's administrative privilege level */
  adminLevel: AdminLevel;
  
  /** Specific administrative permissions granted to current user */
  permissions: string[];
  
  /** Audit log entries for current session */
  auditLog: Array<{
    /** Timestamp of administrative action */
    timestamp: Date;
    /** Administrative action performed */
    action: string;
    /** Target of administrative action (user, account, etc.) */
    target: string;
    /** Result of administrative action */
    result: 'success' | 'failure' | 'pending';
    /** Additional details about the action */
    details: string;
  }>;
  
  /** Field attributes for admin option selection and display fields */
  fieldAttributes: {
    /** Attributes for admin menu option display fields (OPTN001-OPTN012) */
    adminOptionsFields: FormFieldAttributes[];
    /** Attributes for option selection field (OPTION) with enhanced validation */
    optionSelectionField: FormFieldAttributes;
    /** Attributes for error message field (ERRMSG) with admin context */
    errorMessageField: FormFieldAttributes;
  };
}