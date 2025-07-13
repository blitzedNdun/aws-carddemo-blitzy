/**
 * CommonTypes.ts
 * 
 * Shared TypeScript type definitions for common BMS field structures, screen headers,
 * form field attributes, and message types used across all 18 React components
 * transformed from BMS mapsets.
 * 
 * This file provides TypeScript interfaces that replicate BMS attribute bytes,
 * field positioning, and validation patterns to maintain exact functional
 * equivalence with the original COBOL/CICS application while enabling modern
 * React component development with Material-UI integration.
 * 
 * Copyright (c) 2023 CardDemo Application
 * Technology transformation: COBOL/CICS/BMS → Java/Spring Boot/React
 */

/**
 * BMS Attribute Type Definitions
 * 
 * These types replicate the original BMS attribute bytes (ASKIP, UNPROT, NORM, etc.)
 * as used in all 18 BMS mapsets and translate them to Material-UI component properties.
 */

/**
 * BMS attribute byte types matching original BMS definitions
 * Used to control field protection, input behavior, and visual appearance
 */
export type BmsAttributeType = 
  | 'ASKIP'    // Auto-skip protected field (read-only display)
  | 'UNPROT'   // Unprotected field allowing user input
  | 'PROT'     // Protected field preventing user modification
  | 'NUM'      // Numeric-only input validation
  | 'IC'       // Initial cursor positioning
  | 'BRT'      // Bright intensity for emphasis
  | 'NORM'     // Normal intensity for standard display
  | 'FSET'     // Field set attribute for change detection
  | 'DRK';     // Dark attribute for masked input (passwords)

/**
 * BMS color attribute types from original mapsets
 * Maps to Material-UI theme color palette
 */
export type BmsColorType = 
  | 'BLUE'      // theme.palette.info.main - Informational messages and help text
  | 'YELLOW'    // theme.palette.warning.main - Highlighted fields and important notices
  | 'GREEN'     // theme.palette.text.primary - Normal data display and field labels
  | 'RED'       // theme.palette.error.main - Error messages and validation failures
  | 'TURQUOISE' // theme.palette.info.light - Secondary informational content
  | 'NEUTRAL';  // theme.palette.text.secondary - Standard text content

/**
 * BMS highlight attribute types
 * Controls field highlighting and emphasis
 */
export type BmsHighlightType = 
  | 'OFF'       // No highlighting
  | 'UNDERLINE' // Underlined text for emphasis
  | 'BLINK'     // Blinking text (converted to animation in React)
  | 'REVERSE';  // Reverse video (converted to background color in React)

/**
 * Message severity levels for user feedback
 * Used in error and success message displays
 */
export type MessageSeverity = 
  | 'error'     // Critical errors requiring user attention
  | 'warning'   // Warning messages for user awareness
  | 'info'      // Informational messages
  | 'success';  // Success confirmations

/**
 * Field Position Interface
 * 
 * Represents the row and column positioning from original BMS POS attributes
 * Maintains exact field positioning for React component layout
 */
export interface FieldPosition {
  /** Row position (1-24 for 24-line terminal) */
  row: number;
  /** Column position (1-80 for 80-character width) */
  column: number;
}

/**
 * Form Field Attributes Interface
 * 
 * Core interface that replicates BMS field attributes and maps them to
 * Material-UI TextField and input component properties. This interface
 * ensures exact functional equivalence with original BMS field behavior.
 */
export interface FormFieldAttributes {
  /** BMS attribute byte controlling field behavior and protection */
  attrb: BmsAttributeType;
  
  /** BMS color attribute for visual styling */
  color: BmsColorType;
  
  /** BMS highlight attribute for emphasis */
  hilight: BmsHighlightType;
  
  /** Field length restriction (equivalent to BMS LENGTH attribute) */
  length: number;
  
  /** Field position on screen (equivalent to BMS POS attribute) */
  pos: FieldPosition;
  
  /** Initial/default value for the field (equivalent to BMS INITIAL) */
  initial?: string;
  
  /** Input picture format for validation (equivalent to BMS PICIN) */
  picin?: string;
  
  /** Validation requirements (equivalent to BMS VALIDN) */
  validn?: string;
}

/**
 * Base Screen Data Interface
 * 
 * Common header fields present across all 18 BMS maps as identified
 * in COSGN00, COACTVW, and other BMS definitions. These fields appear
 * consistently in the header section of every screen.
 */
export interface BaseScreenData {
  /** Transaction name (4 characters) - equivalent to BMS TRNNAME field */
  trnname: string;
  
  /** Program name (8 characters) - equivalent to BMS PGMNAME field */
  pgmname: string;
  
  /** Current date (8 characters, format: mm/dd/yy) - equivalent to BMS CURDATE field */
  curdate: string;
  
  /** Current time (9 characters, format: hh:mm:ss) - equivalent to BMS CURTIME field */
  curtime: string;
  
  /** Primary title line (40 characters) - equivalent to BMS TITLE01 field */
  title01: string;
  
  /** Secondary title line (40 characters) - equivalent to BMS TITLE02 field */
  title02: string;
}

/**
 * Validation Rule Interface
 * 
 * Defines validation rules for form fields, replicating BMS validation
 * behavior while providing enhanced client-side validation capabilities
 */
export interface ValidationRule {
  /** Type of validation (required, pattern, range, etc.) */
  type: 'required' | 'pattern' | 'range' | 'length' | 'custom';
  
  /** Regular expression pattern for pattern validation */
  pattern?: string;
  
  /** Required field indicator */
  required: boolean;
  
  /** Validation error message to display */
  message: string;
}

/**
 * Error Message Interface
 * 
 * Standardized error message structure for consistent error handling
 * across all React components. Replaces BMS ERRMSG field functionality
 * with enhanced error information.
 */
export interface ErrorMessage {
  /** Error message text (equivalent to BMS ERRMSG content) */
  message: string;
  
  /** Error code for identification and logging */
  code?: string;
  
  /** Field name associated with the error */
  field?: string;
  
  /** Severity level of the error */
  severity: MessageSeverity;
  
  /** Timestamp when error occurred */
  timestamp: Date;
}

/**
 * Success Message Interface
 * 
 * Standardized success message structure for user feedback
 * on successful operations and confirmations
 */
export interface SuccessMessage {
  /** Success message text */
  message: string;
  
  /** Action that was completed successfully */
  action?: string;
  
  /** Timestamp when success occurred */
  timestamp: Date;
  
  /** Additional details about the successful operation */
  details?: string;
}

/**
 * Screen State Interface
 * 
 * Manages the overall state of a screen/component, including loading states,
 * read-only modes, change tracking, and message display. This replaces
 * CICS screen-level state management.
 */
export interface ScreenState {
  /** Loading indicator for async operations */
  isLoading: boolean;
  
  /** Read-only mode indicator (equivalent to all fields being ASKIP) */
  isReadOnly: boolean;
  
  /** Indicates if screen has unsaved changes */
  hasChanges: boolean;
  
  /** Last update timestamp for optimistic locking */
  lastUpdated?: Date;
  
  /** Current error message to display */
  errorMessage?: ErrorMessage;
  
  /** Current success message to display */
  successMessage?: SuccessMessage;
}

/**
 * Constants for BMS-equivalent behaviors
 */

/** Standard field lengths from BMS definitions */
export const FIELD_LENGTHS = {
  USERID: 8,      // User ID field length
  PASSWORD: 8,    // Password field length
  TRNNAME: 4,     // Transaction name length
  PGMNAME: 8,     // Program name length
  DATE: 8,        // Date field length (mm/dd/yy)
  TIME: 9,        // Time field length (hh:mm:ss)
  TITLE: 40,      // Title field length
  ERRMSG: 78,     // Error message field length
  ACCOUNT_ID: 11, // Account ID length
  CARD_NUMBER: 16,// Card number length
} as const;

/** Function key mappings from original BMS */
export const FUNCTION_KEYS = {
  F3: 'EXIT',     // Exit function
  F7: 'PAGEUP',   // Page up navigation
  F8: 'PAGEDOWN', // Page down navigation
  F12: 'CANCEL',  // Cancel operation
  ENTER: 'ENTER', // Submit/Enter function
} as const;

/** Screen dimensions matching original terminal */
export const SCREEN_DIMENSIONS = {
  ROWS: 24,       // Terminal screen rows
  COLUMNS: 80,    // Terminal screen columns
} as const;