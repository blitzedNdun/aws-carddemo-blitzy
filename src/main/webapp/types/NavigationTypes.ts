/**
 * NavigationTypes.ts
 * 
 * TypeScript interface definitions for navigation and routing that preserve CICS 
 * transaction flow patterns while enabling modern React Router navigation.
 * 
 * This file maintains exact functional equivalence with original CICS XCTL program
 * flow and pseudo-conversational processing patterns transformed for React web interface.
 * 
 * Key CICS Navigation Patterns Preserved:
 * - Transaction-based routing (each screen tied to 4-character transaction code)
 * - Pseudo-conversational state management across screen transitions
 * - Function key navigation (F3=Exit, F7=Page Up, F8=Page Down, F12=Cancel)
 * - Breadcrumb trail navigation for user orientation and F3/Back functionality
 * - Session state preservation across React Router navigation transitions
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0
 */

import { BaseScreenData } from './CommonTypes';
import type { Location } from 'history';
import { KeyboardEvent } from 'react';

/**
 * Route Definition Interface
 * 
 * Defines React Router route configuration that maps CICS transaction codes to
 * React components while preserving exact navigation flow from original BMS maps.
 * 
 * Based on analysis of COMEN01.bms and COADM01.bms:
 * - Each menu option corresponds to a specific transaction code
 * - Navigation follows CICS XCTL pattern with controlled program transfers
 * - Authentication requirements preserved from original RACF security model
 * - Breadcrumb generation supports F3=Exit and navigation history tracking
 * 
 * Maps to original CICS navigation patterns:
 * - EXEC CICS XCTL PROGRAM(targetProgram) COMMAREA(data)
 * - Pseudo-conversational processing with preserved session state
 * - Transaction-based security checking matching RACF user authorization
 */
export interface RouteDefinition {
  /** React Router path pattern (e.g., '/menu/main', '/account/view/:id') */
  path: string;
  
  /** React component class name or functional component reference */
  component: string;
  
  /** 4-character CICS transaction code (e.g., 'CMEN', 'CAVW', 'CAUP') */
  transactionCode: string;
  
  /** Screen title displayed in navigation and breadcrumbs (40 characters max) */
  title: string;
  
  /** Indicates if route requires user authentication (maps to RACF security) */
  requiresAuth: boolean;
  
  /** Breadcrumb configuration for navigation trail and F3=Exit functionality */
  breadcrumb: {
    /** Display text in breadcrumb trail */
    label: string;
    /** Parent route path for hierarchical navigation */
    parentPath?: string;
    /** Indicates if this route can be a breadcrumb parent */
    isParent: boolean;
  };
}

/**
 * Navigation Context Interface
 * 
 * Maintains comprehensive navigation state that replicates CICS pseudo-conversational
 * processing patterns in React single-page application environment.
 * 
 * Preserves exact CICS session management behavior:
 * - Current and previous screen tracking (EXEC CICS RETURN TRANSID pattern)
 * - User session data equivalent to CICS terminal control table entries
 * - Return path functionality matching CICS program control flow
 * - Transaction ID tracking for audit trail and debugging
 * 
 * Integrates with React Router Location object to track:
 * - Current pathname for active route identification
 * - Search parameters for query string data passing
 * - Navigation state for complex data transfer between screens
 * - Hash values for anchor-based navigation within screens
 */
export interface NavigationContext {
  /** Current screen data including BMS header fields and transaction info */
  currentScreen: BaseScreenData & {
    /** Current React Router location object */
    location: Location;
    /** Screen-specific state data */
    screenState: Record<string, any>;
  };
  
  /** Previous screen data for back navigation and F3=Exit functionality */
  previousScreen: BaseScreenData & {
    /** Previous React Router location for navigation history */
    location: Location;
    /** Preserved screen state from previous navigation */
    screenState: Record<string, any>;
  } | null;
  
  /** User session data preserving CICS pseudo-conversational state */
  sessionData: {
    /** Unique session identifier matching CICS terminal ID concept */
    sessionId: string;
    /** Session creation timestamp for timeout management */
    startTime: Date;
    /** Last activity timestamp for idle session handling */
    lastActivity: Date;
    /** Session timeout value in minutes (default: 20 minutes matching CICS) */
    timeoutMinutes: number;
    /** Additional session-specific data storage */
    attributes: Record<string, any>;
  };
  
  /** Current user role for menu filtering and security enforcement */
  userRole: 'user' | 'admin' | 'guest';
  
  /** Return path for F3=Exit and Cancel button navigation */
  returnPath: string | null;
  
  /** Current transaction ID for audit logging and error tracking */
  transactionId: string;
}

/**
 * Breadcrumb Data Interface
 * 
 * Defines navigation breadcrumb structure that provides user orientation
 * and supports F3=Exit functionality by maintaining hierarchical path tracking.
 * 
 * Replicates CICS screen navigation patterns:
 * - Hierarchical menu structure from COMEN01.bms and COADM01.bms
 * - Back navigation equivalent to F3=Exit function key behavior
 * - Transaction code display for technical support and debugging
 * - Active/inactive breadcrumb states for visual navigation feedback
 * 
 * Supports both main user menu and admin menu navigation paths:
 * - Main Menu -> Account View -> Account Update (3-level hierarchy)
 * - Admin Menu -> User Management -> User Details (3-level hierarchy)
 * - Consistent F3=Exit behavior returning to appropriate parent level
 */
export interface BreadcrumbData {
  /** Route path corresponding to this breadcrumb level */
  path: string;
  
  /** Display title for breadcrumb navigation (matches screen TITLE01/TITLE02) */
  title: string;
  
  /** Associated CICS transaction code for this navigation level */
  transactionCode: string;
  
  /** Breadcrumb hierarchy level (0=Home, 1=Main Menu, 2=Subscreen, etc.) */
  level: number;
  
  /** Indicates if this breadcrumb represents the current active screen */
  isActive: boolean;
}

/**
 * Function Key Mapping Interface
 * 
 * Defines keyboard event handling for function key navigation that preserves
 * exact CICS PF key behavior in React web interface using browser keyboard events.
 * 
 * Standard CICS Function Key Mappings Preserved:
 * - F3=Exit: Return to previous screen or main menu
 * - F7=Page Up: Scroll up or previous page in paginated displays
 * - F8=Page Down: Scroll down or next page in paginated displays  
 * - F12=Cancel: Cancel current operation and return without saving
 * - ENTER=Continue: Process current screen and proceed to next step
 * - CLEAR=Clear: Clear all input fields (Ctrl+L alternative)
 * 
 * Keyboard Event Integration:
 * - key: Modern KeyboardEvent.key property (e.g., 'F3', 'Enter', 'Escape')
 * - keyCode: Legacy KeyboardEvent.keyCode for browser compatibility
 * - preventDefault(): Stops default browser behavior for captured keys
 * - stopPropagation(): Prevents event bubbling to parent components
 */
export interface FunctionKeyMapping {
  /** Function key identifier (e.g., 'F3', 'F7', 'F8', 'F12', 'Enter') */
  key: string;
  
  /** Action description for user documentation and accessibility */
  action: string;
  
  /** Alternative key combination (e.g., 'Escape' for F12, 'Alt+F3' for F3) */
  alternativeKey?: string;
  
  /** Target route or navigation destination when key is pressed */
  navigationTarget: string | null;
  
  /** Keyboard event handler function with full React KeyboardEvent support */
  handler: (event: KeyboardEvent<HTMLElement>) => void;
}