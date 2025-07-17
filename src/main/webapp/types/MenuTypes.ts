/**
 * CardDemo - Menu Navigation Type Definitions
 * 
 * TypeScript interface definitions for menu navigation screens (COMEN01/COADM01)
 * including role-based menu options, navigation state management, and administrative
 * access control types.
 * 
 * This file transforms the original BMS menu definitions into modern TypeScript
 * interfaces while preserving exact functional equivalence with the COBOL/CICS
 * menu navigation system.
 * 
 * Maps COMEN01.bms (Main Menu) and COADM01.bms (Admin Menu) structures to React
 * components with role-based access control and Spring Security integration.
 */

import { FormFieldAttributes } from './CommonTypes';
import { RouteDefinition } from './NavigationTypes';

// ===================================================================
// MENU TYPE DEFINITIONS - Maps menu classification and user roles
// ===================================================================

/**
 * Menu Type - Classifies different menu categories in the system
 * Maps to original CICS menu hierarchy preserving access control patterns
 */
export type MenuType = 
  | 'MAIN'     // Main menu (COMEN01) - Standard user menu
  | 'ADMIN'    // Admin menu (COADM01) - Administrative functions
  | 'READONLY' // Read-only menu - View-only access
  | 'GUEST';   // Guest menu - Limited functionality

/**
 * User Role Type - Maps RACF group assignments to application access levels
 * Preserves original security model while enabling role-based navigation
 */
export type UserRole = 
  | 'ADMIN'    // Administrative users with full system access
  | 'USER'     // Regular users with standard transaction access  
  | 'READONLY' // Read-only users with view-only access
  | 'GUEST';   // Limited access users with restricted functionality

/**
 * Admin Level Type - Defines administrative access hierarchy
 * Maps to original RACF administrative group structure
 */
export type AdminLevel = 
  | 'SUPERUSER'  // Full administrative access to all functions
  | 'SYSADMIN'   // System administration functions
  | 'USERADMIN'  // User management functions
  | 'AUDITOR'    // Audit and reporting functions
  | 'OPERATOR';  // Operations and monitoring functions

// ===================================================================
// MENU OPTION INTERFACES - Core menu item definitions
// ===================================================================

/**
 * Menu Option Data Interface
 * 
 * Represents individual menu options in the main menu (COMEN01).
 * Maps directly to OPTN001-OPTN012 fields in the BMS definition with
 * enhanced metadata for role-based access control and React Router integration.
 * 
 * Based on analysis of COMEN01.bms option field structure.
 */
export interface MenuOptionData {
  /**
   * Option number - Sequential option identifier (1-12)
   * Maps to BMS OPTN001-OPTN012 field sequence for user selection
   */
  optionNumber: number;
  
  /**
   * Option text - Display text for the menu option (40 characters max)
   * Maps to BMS OPTN###I field content preserving original display format
   */
  optionText: string;
  
  /**
   * Transaction code - CICS transaction code for this option
   * Maps to original 4-character CICS transaction identifier for navigation
   */
  transactionCode: string;
  
  /**
   * Route path - React Router path for this menu option
   * Maps to RouteDefinition.path for modern web application routing
   */
  routePath: string;
  
  /**
   * Visibility flag - Whether option is visible to current user
   * Enables dynamic menu generation based on user role and permissions
   */
  isVisible: boolean;
  
  /**
   * Authentication requirement - Whether option requires user authentication
   * Maps to RouteDefinition.requiresAuth for security integration
   */
  requiresAuth: boolean;
  
  /**
   * User roles - Array of user roles that can access this option
   * Enables role-based menu filtering matching Spring Security authorities
   */
  userRoles: UserRole[];
  
  /**
   * Option description - Detailed description for accessibility and help
   * Provides additional context for screen readers and user guidance
   */
  description: string;
  
  /**
   * Enabled flag - Whether option is currently enabled for selection
   * Enables conditional menu item availability based on system state
   */
  isEnabled: boolean;
  
  /**
   * Keyboard shortcut - Keyboard shortcut key for this option
   * Maps original function key behavior to modern keyboard shortcuts
   */
  keyboardShortcut?: string;
  
  /**
   * Field attributes - BMS field attributes for display formatting
   * Maps to FormFieldAttributes for consistent field rendering
   */
  fieldAttributes: {
    attrb: FormFieldAttributes['attrb'];
    picin: FormFieldAttributes['picin'];
    validn: FormFieldAttributes['validn'];
    length: FormFieldAttributes['length'];
  };
}

/**
 * Admin Menu Data Interface
 * 
 * Represents administrative menu options in the admin menu (COADM01).
 * Extends standard menu functionality with administrative access controls,
 * audit requirements, and enhanced permission management.
 * 
 * Based on analysis of COADM01.bms admin option field structure.
 */
export interface AdminMenuData {
  /**
   * Option number - Sequential option identifier (1-12)
   * Maps to BMS OPTN001-OPTN012 field sequence for admin selection
   */
  optionNumber: number;
  
  /**
   * Option text - Display text for the admin menu option (40 characters max)
   * Maps to BMS OPTN###I field content preserving original display format
   */
  optionText: string;
  
  /**
   * Transaction code - CICS transaction code for this admin option
   * Maps to original 4-character CICS transaction identifier for navigation
   */
  transactionCode: string;
  
  /**
   * Route path - React Router path for this admin menu option
   * Maps to RouteDefinition.path for modern web application routing
   */
  routePath: string;
  
  /**
   * Admin level - Required administrative access level
   * Maps to AdminLevel for hierarchical permission control
   */
  adminLevel: AdminLevel;
  
  /**
   * Permissions - Array of specific permissions required for this option
   * Enables fine-grained access control beyond role-based security
   */
  permissions: string[];
  
  /**
   * Access control - Additional access control requirements
   * Provides extended security configuration for sensitive operations
   */
  accessControl: {
    requiresSecondaryAuth: boolean;
    allowedTimeWindows?: string[];
    ipRestrictions?: string[];
    concurrentUserLimit?: number;
  };
  
  /**
   * Audit required - Whether this option requires audit logging
   * Enables comprehensive audit trail for administrative actions
   */
  auditRequired: boolean;
  
  /**
   * Visibility flag - Whether option is visible to current admin user
   * Enables dynamic admin menu generation based on admin level
   */
  isVisible: boolean;
  
  /**
   * Enabled flag - Whether option is currently enabled for selection
   * Enables conditional admin menu item availability based on system state
   */
  isEnabled: boolean;
  
  /**
   * Option description - Detailed description for accessibility and help
   * Provides additional context for screen readers and user guidance
   */
  description: string;
  
  /**
   * Field attributes - BMS field attributes for display formatting
   * Maps to FormFieldAttributes for consistent field rendering
   */
  fieldAttributes: {
    attrb: FormFieldAttributes['attrb'];
    picin: FormFieldAttributes['picin'];
    validn: FormFieldAttributes['validn'];
    length: FormFieldAttributes['length'];
  };
}

// ===================================================================
// NAVIGATION STATE INTERFACES - State management for menu navigation
// ===================================================================

/**
 * Navigation State Interface
 * 
 * Manages navigation state for React Router integration preserving CICS XCTL
 * program flow. Maintains complete session context equivalent to CICS COMMAREA
 * and terminal storage functionality for pseudo-conversational processing.
 * 
 * Integrates with React Router and Spring Security for modern web navigation.
 */
export interface NavigationState {
  /**
   * Current menu - Currently active menu type
   * Tracks the active menu context for proper navigation handling
   */
  currentMenu: MenuType;
  
  /**
   * Selected option - Currently selected menu option number
   * Maps to BMS OPTION field value for user selection tracking
   */
  selectedOption: number | null;
  
  /**
   * Navigation context - Current navigation context information
   * Maintains session context across screen transitions
   */
  navigationContext: {
    previousScreen?: string;
    returnPath?: string;
    breadcrumbTrail: string[];
    sessionId: string;
    transactionId: string;
  };
  
  /**
   * Menu history - Navigation history for back/forward functionality
   * Enables CICS RETURN equivalent functionality and breadcrumb navigation
   */
  menuHistory: Array<{
    menuType: MenuType;
    selectedOption: number;
    timestamp: Date;
    transactionCode: string;
  }>;
  
  /**
   * Return path - Navigation path for return operations
   * Maintains navigation stack for proper CICS-equivalent return behavior
   */
  returnPath: string | null;
  
  /**
   * Session state - Current user session information
   * Maintains user context across screen transitions like CICS terminal storage
   */
  sessionState: {
    userId: string;
    userRole: UserRole;
    adminLevel?: AdminLevel;
    loginTime: Date;
    lastActivity: Date;
    sessionTimeout: number;
  };
  
  /**
   * Error message - Current error message for display
   * Maps to BMS ERRMSG field for error handling and user feedback
   */
  errorMessage: string | null;
  
  /**
   * Validation state - Current form validation state
   * Enables comprehensive validation tracking for menu option selection
   */
  validationState: {
    isValid: boolean;
    errors: Record<string, string>;
    warnings: Record<string, string>;
  };
  
  /**
   * Form data - Current form data for menu interaction
   * Maintains form state across navigation operations
   */
  formData: Record<string, any>;
}

/**
 * Role-Based Menu Configuration Interface
 * 
 * Defines menu configuration based on user roles and permissions.
 * Enables dynamic menu generation matching Spring Security authorities
 * while preserving original CICS security model behavior.
 * 
 * Maps RACF group assignments to modern role-based access control.
 */
export interface RoleBasedMenuConfig {
  /**
   * Menu type - Type of menu this configuration applies to
   * Maps to MenuType for configuration categorization
   */
  menuType: MenuType;
  
  /**
   * User role - User role this configuration applies to
   * Maps to UserRole for role-based menu filtering
   */
  userRole: UserRole;
  
  /**
   * Available options - Array of menu options available to this role
   * Enables dynamic menu generation based on user permissions
   */
  availableOptions: MenuOptionData[] | AdminMenuData[];
  
  /**
   * Restricted options - Array of menu options restricted for this role
   * Provides explicit restriction configuration for security enforcement
   */
  restrictedOptions: number[];
  
  /**
   * Default option - Default menu option for this role
   * Enables automatic selection based on user preferences or role defaults
   */
  defaultOption: number | null;
  
  /**
   * Max menu level - Maximum menu depth accessible by this role
   * Controls navigation depth for hierarchical menu access
   */
  maxMenuLevel: number;
  
  /**
   * Role hierarchy - Role hierarchy information for inheritance
   * Enables role-based permission inheritance and escalation
   */
  roleHierarchy: {
    parentRole?: UserRole;
    childRoles: UserRole[];
    inheritPermissions: boolean;
  };
  
  /**
   * Permission matrix - Detailed permission matrix for this role
   * Provides fine-grained permission control beyond basic role assignment
   */
  permissionMatrix: Record<string, boolean>;
  
  /**
   * Menu generation - Configuration for dynamic menu generation
   * Controls how menus are generated and displayed for this role
   */
  menuGeneration: {
    sortOrder: 'NUMERIC' | 'ALPHABETIC' | 'CUSTOM';
    groupingEnabled: boolean;
    customGroups?: Array<{
      groupName: string;
      optionNumbers: number[];
    }>;
    hideEmptyGroups: boolean;
  };
}

// ===================================================================
// SCREEN DATA INTERFACES - Complete screen data structures
// ===================================================================

/**
 * Main Menu Screen Data Interface
 * 
 * Complete data structure for the main menu screen (COMEN01).
 * Combines BaseScreenData with menu-specific data and navigation state.
 * 
 * Preserves exact BMS field mapping while providing modern React state management.
 */
export interface MainMenuScreenData {
  /**
   * Base screen data - Common screen header information
   * Maps to BaseScreenData for consistent screen structure
   */
  baseScreenData: {
    trnname: string;
    pgmname: string;
    curdate: string;
    curtime: string;
    title01: string;
    title02: string;
  };
  
  /**
   * Menu options - Array of available menu options
   * Maps to OPTN001-OPTN012 fields with role-based filtering
   */
  menuOptions: MenuOptionData[];
  
  /**
   * Selected option - Currently selected menu option
   * Maps to BMS OPTION field for user selection processing
   */
  selectedOption: number | null;
  
  /**
   * Error message - Current error message for display
   * Maps to BMS ERRMSG field for error handling and user feedback
   */
  errorMessage: string | null;
  
  /**
   * Navigation state - Current navigation state
   * Maintains navigation context for proper screen flow
   */
  navigationState: NavigationState;
  
  /**
   * User role - Current user's role for menu filtering
   * Maps to UserRole for role-based menu generation
   */
  userRole: UserRole;
  
  /**
   * Available transactions - Array of transaction codes available to user
   * Enables dynamic transaction filtering based on user permissions
   */
  availableTransactions: string[];
  
  /**
   * Field attributes - BMS field attributes for form rendering
   * Maps to FormFieldAttributes for consistent field display
   */
  fieldAttributes: {
    option: {
      attrb: FormFieldAttributes['attrb'];
      picin: FormFieldAttributes['picin'];
      validn: FormFieldAttributes['validn'];
      length: FormFieldAttributes['length'];
    };
  };
}

/**
 * Admin Menu Screen Data Interface
 * 
 * Complete data structure for the admin menu screen (COADM01).
 * Combines BaseScreenData with admin-specific data and enhanced security controls.
 * 
 * Preserves exact BMS field mapping while providing administrative functionality.
 */
export interface AdminMenuScreenData {
  /**
   * Base screen data - Common screen header information
   * Maps to BaseScreenData for consistent screen structure
   */
  baseScreenData: {
    trnname: string;
    pgmname: string;
    curdate: string;
    curtime: string;
    title01: string;
    title02: string;
  };
  
  /**
   * Admin menu options - Array of available admin menu options
   * Maps to OPTN001-OPTN012 fields with admin-level filtering
   */
  adminMenuOptions: AdminMenuData[];
  
  /**
   * Selected option - Currently selected admin menu option
   * Maps to BMS OPTION field for admin selection processing
   */
  selectedOption: number | null;
  
  /**
   * Error message - Current error message for display
   * Maps to BMS ERRMSG field for error handling and user feedback
   */
  errorMessage: string | null;
  
  /**
   * Navigation state - Current navigation state
   * Maintains navigation context for proper screen flow
   */
  navigationState: NavigationState;
  
  /**
   * Admin level - Current user's administrative access level
   * Maps to AdminLevel for hierarchical permission control
   */
  adminLevel: AdminLevel;
  
  /**
   * Permissions - Array of specific permissions for current admin user
   * Enables fine-grained access control for administrative functions
   */
  permissions: string[];
  
  /**
   * Audit log - Audit trail information for administrative actions
   * Provides comprehensive audit trail for security and compliance
   */
  auditLog: Array<{
    timestamp: Date;
    userId: string;
    action: string;
    transactionCode: string;
    result: 'SUCCESS' | 'FAILURE' | 'WARNING';
    details?: string;
  }>;
  
  /**
   * Field attributes - BMS field attributes for form rendering
   * Maps to FormFieldAttributes for consistent field display
   */
  fieldAttributes: {
    option: {
      attrb: FormFieldAttributes['attrb'];
      picin: FormFieldAttributes['picin'];
      validn: FormFieldAttributes['validn'];
      length: FormFieldAttributes['length'];
    };
  };
}