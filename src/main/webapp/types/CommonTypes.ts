/**
 * CommonTypes.ts
 * 
 * Shared TypeScript type definitions for common BMS field structures, screen headers,
 * form field attributes, and message types used across all 18 React components
 * transformed from BMS mapsets.
 * 
 * This file maintains exact functional equivalence with original COBOL BMS definitions
 * while providing modern TypeScript interfaces for React component development.
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0
 */

/**
 * BMS Attribute Type Definition
 * Maps original BMS DFHMDF ATTRB values to TypeScript union type
 * Based on COSGN00.bms and COACTVW.bms attribute analysis
 */
export type BmsAttributeType = 
  | 'ASKIP'    // Auto-skip protected field (read-only display)
  | 'UNPROT'   // Unprotected field allowing user input
  | 'PROT'     // Protected field preventing user modification
  | 'NORM'     // Normal intensity for standard display
  | 'BRT'      // Bright intensity for emphasis
  | 'FSET'     // Field set attribute for change detection
  | 'IC'       // Initial cursor positioning
  | 'DRK'      // Dark attribute for hidden fields (passwords)
  | 'NUM';     // Numeric-only input validation

/**
 * BMS Color Type Definition
 * Maps original BMS COLOR attribute values to TypeScript union type
 * Preserves exact color scheme from 3270 terminal display
 */
export type BmsColorType = 
  | 'BLUE'       // Informational messages and help text
  | 'YELLOW'     // Highlighted fields and important notices
  | 'GREEN'      // Normal data display and field labels
  | 'RED'        // Error messages and validation failures
  | 'TURQUOISE'  // Interactive field labels and prompts
  | 'NEUTRAL';   // Standard text display

/**
 * BMS Highlight Type Definition
 * Maps original BMS HILIGHT attribute values to TypeScript union type
 * Controls field emphasis and visual differentiation
 */
export type BmsHighlightType = 
  | 'OFF'        // No highlighting applied
  | 'UNDERLINE'  // Underlined text for emphasis
  | 'BLINK'      // Blinking text for urgent attention
  | 'REVERSE';   // Reverse video for high contrast

/**
 * Message Severity Type Definition
 * Standardizes message severity levels across all React components
 * Enables consistent error handling and user feedback patterns
 */
export type MessageSeverity = 
  | 'error'      // Critical errors requiring immediate attention
  | 'warning'    // Warning conditions that need user awareness
  | 'info'       // Informational messages for user guidance
  | 'success';   // Success confirmations and positive feedback

/**
 * Field Position Interface
 * Represents BMS POS=(row,column) positioning in React component coordinates
 * Maintains exact field positioning from original 24x80 terminal layout
 */
export interface FieldPosition {
  /** Row position (1-24 range matching BMS terminal coordinates) */
  row: number;
  /** Column position (1-80 range matching BMS terminal coordinates) */
  column: number;
}

/**
 * Form Field Attributes Interface
 * Comprehensive mapping of BMS DFHMDF attributes to TypeScript interface
 * Enables precise replication of BMS field behavior in React components
 * 
 * Maps to Material-UI TextField and HTML input attributes:
 * - attrb: Controls field protection and input behavior
 * - color: Determines field text and background colors
 * - hilight: Controls field emphasis and visual effects
 * - length: Enforces maximum field length validation
 * - pos: Maintains exact field positioning from BMS layout
 * - initial: Sets default field values from BMS INITIAL attribute
 * - picin: Input picture format for numeric fields (e.g., '99999999999')
 * - validn: Field validation rules (e.g., 'MUSTFILL' for required fields)
 */
export interface FormFieldAttributes {
  /** BMS attribute byte controlling field behavior (ASKIP, UNPROT, PROT, etc.) */
  attrb: BmsAttributeType | BmsAttributeType[];
  /** Field display color matching BMS COLOR attribute */
  color: BmsColorType;
  /** Field highlighting style matching BMS HILIGHT attribute */
  hilight: BmsHighlightType;
  /** Maximum field length from BMS LENGTH attribute */
  length: number;
  /** Field position in BMS coordinate system */
  pos: FieldPosition;
  /** Default field value from BMS INITIAL attribute */
  initial?: string;
  /** Input picture format for validation (e.g., '99999999999' for numeric) */
  picin?: string;
  /** Field validation rule (e.g., 'MUSTFILL', 'NUMERIC') */
  validn?: string;
}

/**
 * Base Screen Data Interface
 * Common header fields present across all 18 BMS maps
 * Maintains exact field structure from COSGN00.bms and COACTVW.bms analysis
 * 
 * These fields appear consistently across all screens:
 * - Transaction name (TRNNAME) - 4 character transaction code
 * - Program name (PGMNAME) - 8 character program identifier  
 * - Current date (CURDATE) - 8 character date display (mm/dd/yy)
 * - Current time (CURTIME) - 8-9 character time display (hh:mm:ss)
 * - Title fields (TITLE01, TITLE02) - 40 character screen titles
 */
export interface BaseScreenData {
  /** Transaction name (4 characters) - displayed in header */
  trnname: string;
  /** Program name (8 characters) - current program identifier */
  pgmname: string;
  /** Current date (8 characters) - format: mm/dd/yy */
  curdate: string;
  /** Current time (8-9 characters) - format: hh:mm:ss */
  curtime: string;
  /** Primary screen title (40 characters) */
  title01: string;
  /** Secondary screen title (40 characters) */
  title02: string;
}

/**
 * Validation Rule Interface
 * Defines field-level validation rules matching BMS validation behavior
 * Supports React Hook Form integration with Yup schema validation
 */
export interface ValidationRule {
  /** Validation rule type (required, pattern, range, custom) */
  type: 'required' | 'pattern' | 'range' | 'custom' | 'cross-field';
  /** Regex pattern for format validation (when type is 'pattern') */
  pattern?: string | RegExp;
  /** Indicates if field is required (matching BMS MUSTFILL validation) */
  required: boolean;
  /** Error message to display when validation fails */
  message: string;
}

/**
 * Error Message Interface
 * Standardized error message structure for consistent error handling
 * Replaces BMS ERRMSG field with structured error information
 */
export interface ErrorMessage {
  /** Error message text for user display */
  message: string;
  /** Error code for programmatic handling and logging */
  code: string;
  /** Field name associated with the error (for field-specific errors) */
  field?: string;
  /** Error severity level */
  severity: MessageSeverity;
  /** Error timestamp for audit and debugging */
  timestamp: Date;
}

/**
 * Success Message Interface
 * Standardized success message structure for positive user feedback
 * Provides consistent success confirmation across all screens
 */
export interface SuccessMessage {
  /** Success message text for user display */
  message: string;
  /** Action that was completed successfully */
  action: string;
  /** Success timestamp for audit trail */
  timestamp: Date;
  /** Additional details about the successful operation */
  details?: string;
}

/**
 * Screen State Interface
 * Manages comprehensive screen state for React components
 * Replaces CICS pseudo-conversational state with React state management
 * 
 * Tracks:
 * - Loading states during API calls
 * - Read-only mode for protected screens
 * - Change detection for unsaved modifications
 * - Last update timestamp for optimistic locking
 * - Current error and success messages
 */
export interface ScreenState {
  /** Indicates if screen is currently loading data or processing */
  isLoading: boolean;
  /** Indicates if screen is in read-only mode (matches BMS ASKIP behavior) */
  isReadOnly: boolean;
  /** Indicates if screen has unsaved changes (matches BMS FSET behavior) */
  hasChanges: boolean;
  /** Timestamp of last screen update for optimistic locking */
  lastUpdated: Date | null;
  /** Current error message being displayed */
  errorMessage: ErrorMessage | null;
  /** Current success message being displayed */
  successMessage: SuccessMessage | null;
}