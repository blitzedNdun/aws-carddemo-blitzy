/**
 * CardDemo - Navigation and Routing Type Definitions
 * 
 * This file contains TypeScript interface definitions for navigation and routing
 * components that preserve CICS transaction flow patterns while implementing
 * modern React Router functionality. These types ensure seamless pseudo-conversational
 * processing equivalent to the original CICS XCTL and LINK behaviors.
 * 
 * Maintains exact functional equivalence with the original COBOL/CICS navigation
 * while providing modern web application routing capabilities.
 */

import { BaseScreenData } from './CommonTypes';
import { Location } from 'react-router-dom';
import { KeyboardEvent } from 'react';

// ===================================================================
// NAVIGATION ACTION TYPES - Maps CICS program control operations
// ===================================================================

/**
 * Navigation Action Type - Maps CICS program control operations to React Router actions
 * Preserves original CICS transaction flow semantics while enabling modern routing
 */
export type NavigationAction = 
  | 'XCTL'     // Transfer control (CICS XCTL) - Navigate to new component without return
  | 'LINK'     // Link to program (CICS LINK) - Navigate with return path preservation  
  | 'RETURN'   // Return to caller (CICS RETURN) - Go back to previous screen
  | 'EXIT'     // Exit transaction (F3 key) - Return to main menu
  | 'CANCEL'   // Cancel operation (F12 key) - Cancel and return without changes
  | 'SUBMIT'   // Submit form data (ENTER key) - Process current screen and navigate
  | 'REFRESH'  // Refresh current screen - Reload current data without navigation
  | 'PAGE_UP'  // Page up navigation (F7 key) - Previous page in paginated data
  | 'PAGE_DOWN'; // Page down navigation (F8 key) - Next page in paginated data

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
 * Transaction Code Type - Maps CICS transaction codes to React components
 * Maintains original 4-character transaction naming convention
 */
export type TransactionCode = 
  | 'SIGN'     // Sign-on/Authentication screen
  | 'MENU'     // Main menu screen
  | 'ADMN'     // Admin menu screen  
  | 'ACVW'     // Account view screen
  | 'ACUP'     // Account update screen
  | 'CALI'     // Card list screen
  | 'CAUP'     // Card update screen
  | 'CCVW'     // Credit card view screen
  | 'CCUP'     // Credit card update screen
  | 'TRNS'     // Transaction list screen
  | 'TRAN'     // Transaction details screen
  | 'BILL'     // Bill payment screen
  | 'USER'     // User management screen
  | 'REPT'     // Reports screen
  | string;    // Allow for additional transaction codes

// ===================================================================
// CORE NAVIGATION INTERFACES
// ===================================================================

/**
 * Route Definition Interface
 * 
 * Defines React Router configuration that maintains CICS transaction flow patterns.
 * Maps each CICS transaction to a React Router route with equivalent security and
 * navigation characteristics.
 * 
 * Based on analysis of COMEN01.bms and COADM01.bms menu structures.
 */
export interface RouteDefinition {
  /**
   * React Router path pattern - URL path for the route
   * Maps to React Router path parameter for component routing
   */
  path: string;
  
  /**
   * React component reference - Component to render for this route
   * Corresponds to the COBOL program that would be XCTLed to in CICS
   */
  component: React.ComponentType<any>;
  
  /**
   * CICS transaction code - Original 4-character transaction identifier
   * Preserves traceability to original CICS transaction for audit purposes
   */
  transactionCode: TransactionCode;
  
  /**
   * Screen title - Human-readable title for the route
   * Displayed in browser title and navigation breadcrumbs
   */
  title: string;
  
  /**
   * Authentication requirement - Whether route requires user authentication
   * Maps to original CICS security checking equivalent to RACF verification
   */
  requiresAuth: boolean;
  
  /**
   * Breadcrumb configuration - Navigation path display information
   * Enables breadcrumb navigation for user orientation and F3/Back functionality
   */
  breadcrumb: BreadcrumbData;
}

/**
 * Navigation Context Interface
 * 
 * Preserves user session state across screen transitions to replicate CICS
 * pseudo-conversational processing. Maintains complete session context
 * equivalent to CICS COMMAREA and terminal storage functionality.
 * 
 * Integrates BaseScreenData to preserve common BMS header information.
 */
export interface NavigationContext {
  /**
   * Current screen data - Active screen information and state
   * Combines BaseScreenData with current React Router location
   */
  currentScreen: BaseScreenData & {
    location: Location;
    transactionCode: TransactionCode;
  };
  
  /**
   * Previous screen data - Last accessed screen for return navigation
   * Enables CICS RETURN equivalent functionality and breadcrumb navigation
   */
  previousScreen?: BaseScreenData & {
    location: Location;
    transactionCode: TransactionCode;
  };
  
  /**
   * Session data - Persistent user session information
   * Maintains user context across screen transitions like CICS terminal storage
   */
  sessionData: {
    userId: string;
    sessionId: string;
    loginTime: Date;
    lastActivity: Date;
    preferences: Record<string, any>;
  };
  
  /**
   * User role - Current user's security access level
   * Maps from original RACF group assignments for authorization control
   */
  userRole: UserRole;
  
  /**
   * Return path - Navigation path for RETURN and EXIT operations
   * Maintains navigation stack for proper CICS-equivalent return behavior
   */
  returnPath?: string;
  
  /**
   * Transaction ID - Unique identifier for current business transaction
   * Enables transaction tracking and audit trail equivalent to CICS transaction logging
   */
  transactionId: string;
}

/**
 * Breadcrumb Data Interface
 * 
 * Implements navigation path tracking for user orientation and F3/Back button
 * functionality. Replicates CICS program flow visibility while providing
 * modern web navigation patterns.
 */
export interface BreadcrumbData {
  /**
   * Route path - React Router path for this breadcrumb level
   * Enables click navigation to any level in the breadcrumb trail
   */
  path: string;
  
  /**
   * Display title - Human-readable title for breadcrumb display
   * Provides user-friendly navigation context and screen identification
   */
  title: string;
  
  /**
   * Transaction code - CICS transaction code for this navigation level
   * Maintains traceability to original CICS program structure
   */
  transactionCode: TransactionCode;
  
  /**
   * Breadcrumb level - Depth level in the navigation hierarchy
   * Controls breadcrumb display depth and navigation context
   */
  level: number;
  
  /**
   * Active indicator - Whether this breadcrumb represents the current screen
   * Controls visual styling and navigation state display
   */
  isActive: boolean;
}

/**
 * Function Key Mapping Interface
 * 
 * Implements PF key equivalents for browser-based navigation that preserves
 * original CICS function key behavior. Maps 3270 terminal function keys to
 * modern keyboard events and navigation actions.
 * 
 * Standard mappings: F3=Exit, F7=Page Up, F8=Page Down, F12=Cancel
 */
export interface FunctionKeyMapping {
  /**
   * Primary key code - Main keyboard key for this function
   * Maps to React KeyboardEvent.key value for event handling
   */
  key: string;
  
  /**
   * Navigation action - Action to perform when key is pressed
   * Maps to CICS-equivalent operation (XCTL, RETURN, etc.)
   */
  action: NavigationAction;
  
  /**
   * Alternative key code - Secondary keyboard key for this function
   * Provides flexibility for different keyboard layouts and user preferences
   */
  alternativeKey?: string;
  
  /**
   * Navigation target - Target route for navigation actions
   * Specifies destination path for XCTL and LINK equivalent operations
   */
  navigationTarget?: string;
  
  /**
   * Key event handler - Custom handler function for complex key operations
   * Enables specialized processing beyond simple navigation actions
   */
  handler: (event: KeyboardEvent<HTMLElement>, context: NavigationContext) => void;
}

// ===================================================================
// UTILITY TYPES AND CONSTANTS
// ===================================================================

/**
 * Navigation State Type - Represents the current state of navigation system
 * Tracks navigation context and provides state management for routing
 */
export type NavigationState = {
  isNavigating: boolean;
  currentContext: NavigationContext;
  breadcrumbTrail: BreadcrumbData[];
  functionKeys: Map<string, FunctionKeyMapping>;
};

/**
 * Route Guard Type - Function type for route protection and access control
 * Implements security checking equivalent to CICS RACF authorization
 */
export type RouteGuard = (
  route: RouteDefinition, 
  context: NavigationContext
) => boolean | Promise<boolean>;

/**
 * Navigation Event Type - Represents navigation events for tracking and logging
 * Provides audit trail functionality equivalent to CICS transaction logging
 */
export type NavigationEvent = {
  timestamp: Date;
  userId: string;
  sessionId: string;
  fromTransaction: TransactionCode;
  toTransaction: TransactionCode;
  action: NavigationAction;
  success: boolean;
  errorMessage?: string;
};

/**
 * Default Function Key Mappings - Standard PF key equivalents
 * Replicates original CICS terminal function key behavior in web browser
 */
export const DEFAULT_FUNCTION_KEYS: Record<string, Omit<FunctionKeyMapping, 'handler'>> = {
  'F3': {
    key: 'F3',
    action: 'EXIT',
    alternativeKey: 'Escape',
    navigationTarget: '/menu'
  },
  'F7': {
    key: 'F7',
    action: 'PAGE_UP',
    alternativeKey: 'PageUp'
  },
  'F8': {
    key: 'F8', 
    action: 'PAGE_DOWN',
    alternativeKey: 'PageDown'
  },
  'F12': {
    key: 'F12',
    action: 'CANCEL',
    alternativeKey: 'Escape'
  },
  'ENTER': {
    key: 'Enter',
    action: 'SUBMIT'
  }
} as const;

/**
 * Standard Transaction Codes - Maps original CICS transaction codes to routes
 * Maintains exact mapping from COBOL programs to React components
 */
export const TRANSACTION_ROUTES: Record<TransactionCode, string> = {
  'SIGN': '/auth/login',
  'MENU': '/menu/main', 
  'ADMN': '/menu/admin',
  'ACVW': '/account/view',
  'ACUP': '/account/update',
  'CALI': '/card/list',
  'CAUP': '/card/update', 
  'CCVW': '/card/view',
  'CCUP': '/card/update',
  'TRNS': '/transaction/list',
  'TRAN': '/transaction/details',
  'BILL': '/billing/payment',
  'USER': '/user/management',
  'REPT': '/reports'
} as const;