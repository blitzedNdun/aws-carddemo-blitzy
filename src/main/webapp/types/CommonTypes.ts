/**
 * CardDemo - Common TypeScript Type Definitions
 * 
 * This file contains shared TypeScript type definitions for common BMS field structures,
 * screen headers, form field attributes, and message types used across all 18 React 
 * components transformed from BMS mapsets.
 * 
 * Preserves the exact field structures and validation patterns from the original 
 * COBOL/BMS implementation while providing modern TypeScript type safety.
 */

// ===================================================================
// BMS ATTRIBUTE TYPES - Direct mapping from BMS attribute bytes
// ===================================================================

/**
 * BMS Attribute Type - Maps BMS ATTRB parameter values to TypeScript literals
 * Replicates BMS attribute bytes (ASKIP, UNPROT, PROT, NORM, BRT, etc.)
 */
export type BmsAttributeType = 
  | 'ASKIP'    // Auto-skip protected field (read-only display)
  | 'UNPROT'   // Unprotected field allowing user input
  | 'PROT'     // Protected field preventing user modification
  | 'NORM'     // Normal intensity for standard display
  | 'BRT'      // Bright intensity for emphasis
  | 'FSET'     // Field set attribute for change detection
  | 'IC'       // Initial cursor positioning
  | 'NUM'      // Numeric-only input validation
  | 'DRK';     // Dark/hidden field (used for password masking)

/**
 * BMS Color Type - Maps BMS COLOR parameter values to TypeScript literals
 * Preserves original 3270 terminal color scheme for UI consistency
 */
export type BmsColorType = 
  | 'BLUE'      // Info/navigation elements (theme.palette.info.main)
  | 'RED'       // Error messages and validation failures (theme.palette.error.main)
  | 'YELLOW'    // Highlighted fields and warnings (theme.palette.warning.main)
  | 'GREEN'     // Input fields and normal data (theme.palette.text.primary)
  | 'TURQUOISE' // Field labels and descriptive text
  | 'NEUTRAL';  // General text and informational content

/**
 * BMS Highlight Type - Maps BMS HILIGHT parameter values to TypeScript literals
 * Controls field emphasis and visual styling
 */
export type BmsHighlightType = 
  | 'UNDERLINE'  // Underlined text for emphasis
  | 'REVERSE'    // Reverse video highlighting
  | 'BLINK'      // Blinking text for urgent attention
  | 'OFF';       // No highlighting applied

/**
 * Message Severity Type - Standardizes message severity levels across components
 * Maps to Material-UI Alert severity levels for consistent user feedback
 */
export type MessageSeverity = 
  | 'error'    // Critical errors requiring immediate attention
  | 'warning'  // Important warnings or validation issues
  | 'info'     // Informational messages and status updates
  | 'success'; // Success confirmations and completion messages

// ===================================================================
// CORE INTERFACE DEFINITIONS
// ===================================================================

/**
 * Form Field Attributes Interface
 * 
 * Replicates BMS field definition attributes in TypeScript format.
 * Maps directly to BMS DFHMDF parameters preserving exact field behavior.
 * 
 * Based on analysis of COSGN00.bms and COACTVW.bms field definitions.
 */
export interface FormFieldAttributes {
  /**
   * Field attribute type - Maps to BMS ATTRB parameter
   * Controls field protection, input validation, and display characteristics
   */
  attrb: BmsAttributeType[];
  
  /**
   * Field color - Maps to BMS COLOR parameter  
   * Determines field color scheme matching original 3270 terminal colors
   */
  color: BmsColorType;
  
  /**
   * Field highlighting - Maps to BMS HILIGHT parameter
   * Controls text emphasis and visual styling
   */
  hilight: BmsHighlightType;
  
  /**
   * Field length - Maps to BMS LENGTH parameter
   * Defines maximum character length for input validation
   */
  length: number;
  
  /**
   * Field position - Maps to BMS POS parameter
   * Row and column positioning for original 24x80 terminal layout
   */
  pos: FieldPosition;
  
  /**
   * Initial field value - Maps to BMS INITIAL parameter
   * Default or placeholder text displayed in the field
   */
  initial?: string;
  
  /**
   * Input picture format - Maps to BMS PICIN parameter
   * Defines input format mask for data entry validation
   */
  picin?: string;
  
  /**
   * Field validation rules - Maps to BMS VALIDN parameter
   * Defines validation constraints like MUSTFILL, format checks, etc.
   */
  validn?: ValidationRule[];
}

/**
 * Field Position Interface
 * 
 * Represents BMS POS=(row,column) parameter in structured format.
 * Maintains exact field positioning from original 24x80 terminal layout.
 */
export interface FieldPosition {
  /**
   * Row position (1-24) - Maps to BMS POS first parameter
   * Vertical position on the original 24-line terminal display
   */
  row: number;
  
  /**
   * Column position (1-80) - Maps to BMS POS second parameter  
   * Horizontal position on the original 80-column terminal display
   */
  column: number;
}

/**
 * Validation Rule Interface
 * 
 * Defines field validation constraints equivalent to BMS VALIDN parameter.
 * Supports comprehensive validation rules for business logic enforcement.
 */
export interface ValidationRule {
  /**
   * Validation type - Defines the category of validation to apply
   * Maps to BMS validation types like MUSTFILL, MUSTENTER, etc.
   */
  type: 'MUSTFILL' | 'MUSTENTER' | 'NUMERIC' | 'ALPHA' | 'ALNUM' | 'DATE' | 'TIME' | 'RANGE' | 'PATTERN';
  
  /**
   * Validation pattern - Regular expression or format pattern
   * Used for pattern matching validation (e.g., date format, account number format)
   */
  pattern?: string;
  
  /**
   * Required field indicator - Maps to BMS MUSTFILL validation
   * Indicates whether field value is mandatory for form submission
   */
  required: boolean;
  
  /**
   * Validation error message - Custom error message for validation failures
   * Displayed when validation rules are not met
   */
  message: string;
}

/**
 * Base Screen Data Interface
 * 
 * Common fields present across all 18 BMS maps per Section 0 transformation requirements.
 * Represents the standard header information displayed on every screen.
 * 
 * Derived from analysis of COSGN00.bms and COACTVW.bms common header fields.
 */
export interface BaseScreenData {
  /**
   * Transaction name - Maps to BMS TRNNAME field (4 characters)
   * Identifies the current transaction code for traceability
   */
  trnname: string;
  
  /**
   * Program name - Maps to BMS PGMNAME field (8 characters)
   * Identifies the executing program for debugging and audit purposes
   */
  pgmname: string;
  
  /**
   * Current date - Maps to BMS CURDATE field (8 characters, mm/dd/yy format)
   * System date displayed consistently across all screens
   */
  curdate: string;
  
  /**
   * Current time - Maps to BMS CURTIME field (8-9 characters, hh:mm:ss format)
   * System time displayed consistently across all screens
   */
  curtime: string;
  
  /**
   * Screen title line 1 - Maps to BMS TITLE01 field (40 characters)
   * Primary screen title and functional description
   */
  title01: string;
  
  /**
   * Screen title line 2 - Maps to BMS TITLE02 field (40 characters)  
   * Secondary screen title and additional context information
   */
  title02: string;
}

/**
 * Error Message Interface
 * 
 * Standardized error message structure for consistent error handling across all 
 * React components. Maps to BMS ERRMSG field behavior with enhanced metadata.
 */
export interface ErrorMessage {
  /**
   * Error message text - Maps to BMS ERRMSG field (78 characters)
   * Human-readable error description displayed to the user
   */
  message: string;
  
  /**
   * Error code - System error code for technical reference
   * Enables error tracking and debugging across the application
   */
  code: string;
  
  /**
   * Field identifier - Identifies the specific field that caused the error
   * Enables precise error highlighting and user guidance
   */
  field?: string;
  
  /**
   * Message severity - Categorizes the error importance level
   * Controls visual presentation and user attention priority
   */
  severity: MessageSeverity;
  
  /**
   * Error timestamp - When the error occurred
   * Enables error tracking and audit trail functionality
   */
  timestamp: Date;
}

/**
 * Success Message Interface
 * 
 * Standardized success message structure for positive user feedback.
 * Provides consistent success notification across all React components.
 */
export interface SuccessMessage {
  /**
   * Success message text - Positive confirmation message
   * Human-readable success description displayed to the user
   */
  message: string;
  
  /**
   * Action performed - Describes the successful operation
   * Provides context about what was accomplished
   */
  action: string;
  
  /**
   * Success timestamp - When the successful action occurred
   * Enables success tracking and audit trail functionality
   */
  timestamp: Date;
  
  /**
   * Additional details - Optional supplementary information
   * Provides additional context or next steps for the user
   */
  details?: string;
}

/**
 * Screen State Interface
 * 
 * Manages the overall state of a React component screen, including loading states,
 * error conditions, and change tracking. Provides consistent state management
 * patterns across all 18 transformed BMS components.
 */
export interface ScreenState {
  /**
   * Loading indicator - Whether the screen is currently loading data
   * Controls loading spinner display and user interaction blocking
   */
  isLoading: boolean;
  
  /**
   * Read-only mode indicator - Whether the screen is in view-only mode
   * Controls field protection and form submission capabilities
   */
  isReadOnly: boolean;
  
  /**
   * Change detection - Whether the screen has unsaved changes
   * Enables change tracking and prevents accidental data loss
   */
  hasChanges: boolean;
  
  /**
   * Last update timestamp - When the screen data was last refreshed
   * Provides data freshness indication and cache management
   */
  lastUpdated: Date;
  
  /**
   * Current error message - Active error displayed to the user
   * Centralized error state management for consistent error handling
   */
  errorMessage?: ErrorMessage;
  
  /**
   * Current success message - Active success notification displayed to the user
   * Centralized success state management for consistent positive feedback
   */
  successMessage?: SuccessMessage;
}