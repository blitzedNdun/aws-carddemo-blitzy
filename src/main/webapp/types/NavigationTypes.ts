/**
 * NavigationTypes.ts
 * 
 * TypeScript interface definitions for navigation and routing including React Router 
 * configuration, session state preservation, function key mappings, and breadcrumb 
 * navigation matching original CICS program flow patterns.
 * 
 * This file provides comprehensive type definitions for implementing React Router-based
 * navigation that preserves the exact transaction flow and pseudo-conversational 
 * behavior of the original COBOL/CICS application while enabling modern web navigation
 * patterns with JWT authentication and Redis session management.
 * 
 * Key Features:
 * - React Router integration with CICS transaction code preservation
 * - Session state management for pseudo-conversational processing  
 * - Function key mapping for PF key equivalents (F3=Exit, F7=Page Up, etc.)
 * - Breadcrumb navigation with path tracking for user orientation
 * - JWT token and Redis session integration for stateless navigation
 * 
 * Copyright (c) 2023 CardDemo Application
 * Technology transformation: COBOL/CICS/BMS → Java/Spring Boot/React
 */

import { Location } from 'react-router-dom';
import { KeyboardEvent } from 'react';
import { BaseScreenData } from './CommonTypes';

/**
 * Route Definition Interface
 * 
 * Defines React Router configuration for each screen component, maintaining
 * the original CICS transaction flow patterns while enabling modern declarative
 * routing. Each route maps to an original CICS transaction code for traceability.
 * 
 * This interface replaces EXEC CICS XCTL program control with React Router
 * navigation while preserving identical transaction boundaries and flow control.
 */
export interface RouteDefinition {
  /** React Router path pattern (e.g., '/menu/main', '/account/view/:id') */
  path: string;
  
  /** React component to render for this route */
  component: React.ComponentType<any>;
  
  /** Original CICS transaction code for API traceability (e.g., 'CM00', 'CAVW') */
  transactionCode: string;
  
  /** Screen title for display in header and breadcrumbs */
  title: string;
  
  /** Indicates if route requires authenticated user with valid JWT token */
  requiresAuth: boolean;
  
  /** Breadcrumb display configuration for navigation path tracking */
  breadcrumb: {
    /** Display label for breadcrumb navigation */
    label: string;
    /** Indicates if this route should appear in breadcrumb trail */
    visible: boolean;
    /** Parent route path for hierarchical breadcrumb structure */
    parent?: string;
  };
}

/**
 * Navigation Context Interface
 * 
 * Maintains user session state and navigation context across React Router
 * transitions, replicating CICS pseudo-conversational processing patterns.
 * 
 * This interface combines React Router location state with Redis session
 * management to preserve user context, transaction history, and return path
 * information equivalent to CICS terminal storage and COMMAREA passing.
 */
export interface NavigationContext {
  /** Current screen data including BMS header fields and transaction info */
  currentScreen: BaseScreenData & {
    /** Current route path */
    route: string;
    /** Screen component name */
    component: string;
    /** Loading state indicator */
    isLoading: boolean;
    /** Error state indicator */
    hasError: boolean;
  };
  
  /** Previous screen information for F3/Back navigation functionality */
  previousScreen?: BaseScreenData & {
    /** Previous route path for return navigation */
    route: string;
    /** Previous component name */
    component: string;
    /** Timestamp when previous screen was visited */
    timestamp: Date;
  };
  
  /** Session data preserved across navigation transitions */
  sessionData: {
    /** JWT authentication token for API calls */
    token: string;
    /** Redis session identifier for distributed state management */
    sessionId: string;
    /** Session expiration timestamp */
    expires: Date;
    /** User preferences and settings */
    preferences: Record<string, any>;
  };
  
  /** Current user role for authorization and menu filtering */
  userRole: 'ADMIN' | 'USER' | 'GUEST';
  
  /** Return path for F3/Exit navigation (equivalent to CICS RETURN) */
  returnPath?: string;
  
  /** Unique transaction identifier for distributed tracing */
  transactionId: string;
}

/**
 * Breadcrumb Data Interface
 * 
 * Defines navigation path tracking and breadcrumb display information
 * for user orientation and navigation assistance. Provides hierarchical
 * navigation structure that maps to original CICS transaction flow.
 * 
 * This interface enables F3/Back button functionality and navigation
 * path display equivalent to CICS program flow tracking.
 */
export interface BreadcrumbData {
  /** Route path for navigation (e.g., '/menu/main', '/account/view/123') */
  path: string;
  
  /** Display title for breadcrumb navigation */
  title: string;
  
  /** Original CICS transaction code for reference */
  transactionCode: string;
  
  /** Hierarchical level in navigation structure (0=root, 1=first level, etc.) */
  level: number;
  
  /** Indicates if this breadcrumb represents the current active screen */
  isActive: boolean;
}

/**
 * Function Key Mapping Interface
 * 
 * Defines keyboard event handling for PF key equivalents, maintaining
 * the original mainframe function key behavior while adapting to modern
 * browser constraints and providing touch device alternatives.
 * 
 * This interface implements the complete function key mapping from original
 * BMS definitions (F3=Exit, F7=Page Up, F8=Page Down, F12=Cancel) with
 * browser-compatible alternatives and touch device support.
 */
export interface FunctionKeyMapping {
  /** Primary function key code (e.g., 'F3', 'F7', 'F8', 'F12', 'Enter') */
  key: string;
  
  /** Action description for user display and accessibility */
  action: string;
  
  /** Alternative key combination for browsers that reserve function keys */
  alternativeKey?: string;
  
  /** Target route or navigation action for this function key */
  navigationTarget?: {
    /** Route path to navigate to */
    route: string;
    /** Whether to replace current history entry */
    replace: boolean;
    /** State to pass to target route */
    state?: Record<string, any>;
  };
  
  /** Keyboard event handler function */
  handler: (event: KeyboardEvent<HTMLElement>) => void;
}

/**
 * Navigation State Management Types
 * 
 * Additional utility types for comprehensive navigation state management
 * and React Router integration with CICS-equivalent behavior preservation.
 */

/**
 * Navigation Action Types
 * 
 * Defines the types of navigation actions that can be performed,
 * mapping to original CICS program control commands.
 */
export type NavigationAction = 
  | 'NAVIGATE'    // Standard navigation to new screen
  | 'RETURN'      // Return to previous screen (F3/Exit)
  | 'CANCEL'      // Cancel current operation (F12/Cancel)
  | 'REFRESH'     // Refresh current screen (F5/Refresh)
  | 'PAGE_UP'     // Page backward in list (F7/Page Up)
  | 'PAGE_DOWN'   // Page forward in list (F8/Page Down)
  | 'SUBMIT'      // Submit form (Enter/Continue)
  | 'CLEAR';      // Clear form fields (F4/Clear)

/**
 * Navigation Result Interface
 * 
 * Defines the outcome of navigation operations with status information
 * equivalent to CICS RESP and RESP2 codes for error handling and logging.
 */
export interface NavigationResult {
  /** Success indicator for navigation operation */
  success: boolean;
  
  /** Error message if navigation failed */
  error?: string;
  
  /** Navigation action that was performed */
  action: NavigationAction;
  
  /** Source route path */
  from: string;
  
  /** Target route path */
  to: string;
  
  /** Timestamp of navigation operation */
  timestamp: Date;
  
  /** Session context at time of navigation */
  sessionContext?: Partial<NavigationContext>;
}

/**
 * Menu Configuration Interface
 * 
 * Defines dynamic menu generation based on user role, replicating
 * the original COMEN01 and COADM01 BMS menu structures with role-based
 * option filtering and dynamic menu item generation.
 */
export interface MenuConfiguration {
  /** Menu type identifier ('MAIN' or 'ADMIN') */
  menuType: 'MAIN' | 'ADMIN';
  
  /** User role for menu filtering */
  userRole: 'ADMIN' | 'USER' | 'GUEST';
  
  /** Available menu options based on role */
  options: MenuOption[];
  
  /** Menu header information */
  header: BaseScreenData;
  
  /** Function key mappings for this menu */
  functionKeys: FunctionKeyMapping[];
}

/**
 * Menu Option Interface
 * 
 * Defines individual menu options with navigation targets and display
 * information, equivalent to OPTN001-OPTN012 fields in BMS definitions.
 */
export interface MenuOption {
  /** Option number (1-12) for user selection */
  optionNumber: number;
  
  /** Display text for menu option */
  displayText: string;
  
  /** Target route path for navigation */
  targetRoute: string;
  
  /** Original CICS transaction code */
  transactionCode: string;
  
  /** Indicates if option is available for current user role */
  isAvailable: boolean;
  
  /** Indicates if option requires special permissions */
  requiresPermission?: string;
  
  /** Description for accessibility and help text */
  description?: string;
}

/**
 * Session Recovery Interface
 * 
 * Defines session recovery information for handling navigation failures
 * and session timeout scenarios with graceful degradation.
 */
export interface SessionRecovery {
  /** Last known valid route path */
  lastValidRoute: string;
  
  /** Session recovery timestamp */
  recoveryTimestamp: Date;
  
  /** Recovery action to perform */
  recoveryAction: 'LOGOUT' | 'REFRESH' | 'REDIRECT';
  
  /** Recovery target route */
  recoveryTarget?: string;
  
  /** Session state to restore */
  sessionState?: Partial<NavigationContext>;
  
  /** Error information that triggered recovery */
  errorInfo?: {
    /** Error message */
    message: string;
    /** Error code */
    code: string;
    /** Stack trace for debugging */
    stack?: string;
  };
}

/**
 * Route Guard Configuration Interface
 * 
 * Defines authentication and authorization rules for protected routes,
 * implementing JWT token validation and role-based access control
 * equivalent to RACF security group enforcement.
 */
export interface RouteGuardConfig {
  /** Route path pattern to protect */
  routePath: string;
  
  /** Required authentication level */
  authLevel: 'AUTHENTICATED' | 'ADMIN' | 'USER' | 'GUEST';
  
  /** Required permissions for access */
  requiredPermissions: string[];
  
  /** Redirect route for unauthorized access */
  redirectRoute: string;
  
  /** JWT token validation requirements */
  tokenValidation: {
    /** Require valid JWT token */
    requireToken: boolean;
    /** Require non-expired token */
    requireValidExpiry: boolean;
    /** Required token claims */
    requiredClaims: string[];
  };
  
  /** Session validation requirements */
  sessionValidation: {
    /** Require active Redis session */
    requireActiveSession: boolean;
    /** Session timeout threshold */
    sessionTimeout: number;
    /** Session renewal on access */
    renewOnAccess: boolean;
  };
}