/**
 * Field Constants for CardDemo Application
 * 
 * This file contains all field-related definitions extracted from BMS maps
 * including field lengths, validation rules, positioning coordinates, 
 * attribute mappings, and format patterns. Provides centralized field 
 * configuration for React components ensuring exact preservation of 
 * original BMS field behaviors while enabling modern TypeScript type checking.
 * 
 * Generated from BMS maps: COSGN00, COACTVW, COACTUP, COCRDLI, COCRDSL, 
 * COCRDUP, COTRN00, COTRN01, COTRN02, COBIL00, CORPT00, COMEN01, COADM01,
 * COUSR00, COUSR01, COUSR02, COUSR03
 */

// ============================================================================
// BMS FIELD ATTRIBUTES
// ============================================================================

/**
 * BMS field attributes mapped to their behavior characteristics
 * These correspond directly to DFHMDF ATTRB values in BMS definitions
 */
export const BMS_ATTRIBUTES = {
  // Protection and Input Control
  ASKIP: 'askip' as const,     // Auto-skip field (read-only, cursor skips)
  UNPROT: 'unprot' as const,   // Unprotected field (user can input)
  PROT: 'prot' as const,       // Protected field (read-only, cursor can focus)
  
  // Data Type and Validation
  NUM: 'num' as const,         // Numeric data only
  IC: 'ic' as const,           // Initial cursor position
  FSET: 'fset' as const,       // Field set indicator
  
  // Display Attributes
  NORM: 'norm' as const,       // Normal intensity
  BRT: 'brt' as const,         // Bright intensity  
  DRK: 'drk' as const,         // Dark/invisible (passwords)
  
  // Validation Rules
  MUSTFILL: 'mustfill' as const, // Required field validation
  
  // Additional Display Properties
  HILIGHT: 'hilight' as const,   // Highlight attribute
  UNDERLINE: 'underline' as const, // Underline attribute
  JUSTIFY_RIGHT: 'justify_right' as const, // Right justification
} as const;

// ============================================================================
// FIELD LENGTH CONSTRAINTS
// ============================================================================

/**
 * Field length definitions extracted from BMS LENGTH attributes
 * These ensure exact character count matching for Material-UI TextField components
 */
export const FIELD_LENGTHS = {
  
  LENGTH_CONSTRAINTS: {
    // Authentication Fields
    USER_ID: 8,
    PASSWORD: 8,
    TRANSACTION_CODE: 4,
    
    // Account Management
    ACCOUNT_NUMBER: 11,
    ACCOUNT_STATUS: 1,
    ACCOUNT_GROUP: 10,
    
    // Card Management  
    CARD_NUMBER: 16,
    CARD_STATUS: 1,
    CARD_SELECTION: 1,
    
    // Customer Information
    CUSTOMER_ID: 9,
    CUSTOMER_SSN: 12,
    FIRST_NAME: 25,
    MIDDLE_NAME: 25,
    LAST_NAME: 25,
    DATE_OF_BIRTH: 10,
    FICO_SCORE: 3,
    
    // Address Fields
    ADDRESS_LINE_1: 50,
    ADDRESS_LINE_2: 50,
    CITY: 50,
    STATE: 2,
    ZIP_CODE: 5,
    EXTENDED_ZIP: 10,
    COUNTRY: 3,
    
    // Contact Information
    PHONE_NUMBER: 13,
    GOVERNMENT_ID: 20,
    EFT_ACCOUNT_ID: 10,
    PRIMARY_FLAG: 1,
    
    // Transaction Fields
    TRANSACTION_ID: 16,
    TRANSACTION_TYPE: 2,
    TRANSACTION_CATEGORY: 4,
    TRANSACTION_SOURCE: 10,
    TRANSACTION_DESCRIPTION: 60,
    MERCHANT_ID: 9,
    MERCHANT_NAME: 30,
    MERCHANT_CITY: 25,
    MERCHANT_ZIP: 10,
    
    // Financial Fields
    TRANSACTION_AMOUNT: 12,
    CREDIT_LIMIT: 15,
    CASH_LIMIT: 15,
    CURRENT_BALANCE: 15,
    CYCLE_CREDIT: 15,
    CYCLE_DEBIT: 15,
    
    // Date and Time Fields
    DATE_SHORT: 8,          // mm/dd/yy format
    DATE_LONG: 10,          // yyyy-mm-dd format
    TIME_SHORT: 8,          // hh:mm:ss format
    TIME_LONG: 9,           // Ahh:mm:ss format
    
    // System Fields
    PROGRAM_NAME: 8,
    APPLICATION_ID: 8,
    SYSTEM_ID: 8,
    PAGE_NUMBER: 3,
    
    // Display Fields
    TITLE_SHORT: 40,
    TITLE_LONG: 66,
    ERROR_MESSAGE: 78,
    INFO_MESSAGE: 45,
    FUNCTION_KEYS: 78,
    
    // Report Fields
    REPORT_TITLE: 60,
    REPORT_LINE: 80,
  } as const,

  MAX_LENGTHS: {
    // Screen dimensions
    SCREEN_WIDTH: 80,
    SCREEN_HEIGHT: 24,
    
    // Common maximums
    SHORT_TEXT: 10,
    MEDIUM_TEXT: 30,
    LONG_TEXT: 60,
    FULL_LINE: 78,
    
    // Specific field categories
    NAMES: 25,
    ADDRESSES: 50,
    DESCRIPTIONS: 60,
    AMOUNTS: 15,
    DATES: 10,
    CODES: 16,
  } as const,

  DEFAULT_SIZES: {
    // Input field defaults
    CODE_FIELD: 8,
    NAME_FIELD: 25,
    ADDRESS_FIELD: 50,
    AMOUNT_FIELD: 15,
    DATE_FIELD: 10,
    FLAG_FIELD: 1,
    
    // List display defaults
    LIST_SELECTION: 1,
    LIST_ITEM_CODE: 11,
    LIST_ITEM_NAME: 30,
    LIST_ITEM_STATUS: 1,
  } as const,
} as const;

// ============================================================================
// FIELD POSITIONING COORDINATES
// ============================================================================

/**
 * Field positioning constants from BMS POS attributes
 * Maintains precise field layout for CSS Grid implementation
 */
export const FIELD_POSITIONING = {
  
  POSITION_MAPPING: {
    // Standard screen layout positions
    HEADER_ROW_1: 1,
    HEADER_ROW_2: 2,
    HEADER_ROW_3: 3,
    CONTENT_START: 4,
    CONTENT_END: 20,
    INFO_ROW: 22,
    ERROR_ROW: 23,
    FUNCTION_KEY_ROW: 24,
    
    // Column positions for common fields
    LABEL_COLUMN: 1,
    FIELD_START_COLUMN: 8,
    MIDDLE_COLUMN: 40,
    RIGHT_COLUMN: 65,
    
    // Authentication screen positions
    LOGIN_USER_ROW: 19,
    LOGIN_USER_COL: 43,
    LOGIN_PASS_ROW: 20,
    LOGIN_PASS_COL: 43,
    
    // Account view positions
    ACCOUNT_NUMBER_ROW: 5,
    ACCOUNT_NUMBER_COL: 38,
    ACCOUNT_STATUS_ROW: 5,
    ACCOUNT_STATUS_COL: 70,
    
    // Transaction positions
    TRANSACTION_ID_ROW: 6,
    TRANSACTION_ID_COL: 21,
    CARD_NUMBER_ROW: 10,
    CARD_NUMBER_COL: 58,
  } as const,

  GRID_COORDINATES: {
    // CSS Grid coordinates (1-based to match BMS)
    GRID_COLUMNS: 80,
    GRID_ROWS: 24,
    
    // Common field spans
    SHORT_FIELD_SPAN: 8,
    MEDIUM_FIELD_SPAN: 15,
    LONG_FIELD_SPAN: 30,
    FULL_WIDTH_SPAN: 78,
    
    // Header layout
    TRANSACTION_NAME_START: 7,
    TRANSACTION_NAME_SPAN: 4,
    TITLE_START: 21,
    TITLE_SPAN: 40,
    DATE_START: 71,
    DATE_SPAN: 8,
    
    // Form field layout
    LABEL_SPAN: 15,
    FIELD_SPAN: 20,
    GAP_SPAN: 5,
  } as const,

  LAYOUT_UTILS: {
    // Utility functions for position calculations
    calculateGridColumn: (pos: number) => pos,
    calculateGridRow: (pos: number) => pos,
    calculateFieldSpan: (length: number) => Math.max(length, 8),
    
    // Standard field positioning helpers
    getFieldPosition: (row: number, col: number) => ({ row, col }),
    getFieldDimensions: (length: number, rows: number = 1) => ({ length, rows }),
    
    // Layout validation
    isValidPosition: (row: number, col: number) => 
      row >= 1 && row <= 24 && col >= 1 && col <= 80,
    isValidLength: (length: number, col: number) => 
      col + length <= 80,
  } as const,
} as const;

// ============================================================================
// ATTRIBUTE MAPPINGS FOR REACT COMPONENTS
// ============================================================================

/**
 * Mapping BMS attributes to Material-UI component properties
 * Enables seamless translation from BMS behavior to React component behavior
 */
export const ATTRIBUTE_MAPPINGS = {
  
  ASKIP_TO_READONLY: {
    // Auto-skip fields become read-only inputs
    component: 'TextField' as const,
    props: {
      InputProps: { readOnly: true },
      tabIndex: -1,
      variant: 'standard' as const,
      size: 'small' as const,
    },
    styles: {
      backgroundColor: '#f5f5f5',
      cursor: 'default',
    },
  } as const,

  UNPROT_TO_EDITABLE: {
    // Unprotected fields become editable inputs
    component: 'TextField' as const,
    props: {
      variant: 'outlined' as const,
      size: 'small' as const,
      autoComplete: 'off',
    },
    styles: {
      backgroundColor: '#ffffff',
    },
  } as const,

  PROT_TO_DISABLED: {
    // Protected fields become disabled inputs
    component: 'TextField' as const,
    props: {
      disabled: true,
      variant: 'standard' as const,
      size: 'small' as const,
    },
    styles: {
      backgroundColor: '#e0e0e0',
    },
  } as const,

  NUM_TO_NUMERIC: {
    // Numeric fields get number input type and validation
    component: 'TextField' as const,
    props: {
      type: 'text' as const, // Use text with pattern for better control
      inputMode: 'numeric' as const,
      pattern: '[0-9]*',
    },
    validation: {
      pattern: /^[0-9]*$/,
      message: 'Numeric characters only',
    },
    styles: {
      textAlign: 'right' as const,
    },
  } as const,

  IC_TO_AUTOFOCUS: {
    // Initial cursor fields get autofocus
    component: 'TextField' as const,
    props: {
      autoFocus: true,
    },
  } as const,

  MUSTFILL_TO_REQUIRED: {
    // MUSTFILL validation becomes required fields
    component: 'TextField' as const,
    props: {
      required: true,
    },
    validation: {
      required: 'This field is required',
    },
    styles: {
      '& .MuiFormLabel-asterisk': {
        color: 'red',
      },
    },
  } as const,
} as const;

// ============================================================================
// FORMAT PATTERNS AND VALIDATION
// ============================================================================

/**
 * Field format patterns from BMS PICIN/PICOUT definitions
 * Used for input validation and display formatting
 */
export const FORMAT_PATTERNS = {
  
  // Numeric Patterns (from PICIN)
  ACCOUNT_NUMBER_PATTERN: /^[0-9]{11}$/,           // 99999999999
  CARD_NUMBER_PATTERN: /^[0-9]{16}$/,              // 9999999999999999
  TRANSACTION_ID_PATTERN: /^[A-Z0-9]{16}$/,        // Alphanumeric 16 chars
  USER_ID_PATTERN: /^[A-Z0-9]{1,8}$/i,             // Up to 8 alphanumeric
  
  // Currency Patterns (from PICOUT)
  CURRENCY_DISPLAY_PATTERN: /^\+?[\d,]{1,12}\.99$/,  // +ZZZ,ZZZ,ZZZ.99
  AMOUNT_INPUT_PATTERN: /^\d{1,10}(\.\d{0,2})?$/,    // Up to 10 digits, 2 decimal
  
  // Date Patterns
  DATE_SHORT_PATTERN: /^(0[1-9]|1[0-2])\/(0[1-9]|[12]\d|3[01])\/\d{2}$/, // mm/dd/yy
  DATE_LONG_PATTERN: /^\d{4}-\d{2}-\d{2}$/,          // yyyy-mm-dd
  TIME_PATTERN: /^([01]\d|2[0-3]):([0-5]\d):([0-5]\d)$/, // hh:mm:ss
  
  // Text Patterns
  NAME_PATTERN: /^[A-Za-z\s\-'\.]{1,25}$/,           // Names with common punctuation
  ADDRESS_PATTERN: /^[A-Za-z0-9\s\-'\.#]{1,50}$/,    // Addresses with numbers
  PHONE_PATTERN: /^\(\d{3}\)\s\d{3}-\d{4}$/,         // (999) 999-9999
  SSN_PATTERN: /^\d{3}-\d{2}-\d{4}$/,                // 999-99-9999
  ZIP_PATTERN: /^\d{5}(-\d{4})?$/,                   // 99999 or 99999-9999
  
  // Code Patterns
  STATE_PATTERN: /^[A-Z]{2}$/,                       // Two letter state codes
  COUNTRY_PATTERN: /^[A-Z]{3}$/,                     // Three letter country codes
  TRANSACTION_TYPE_PATTERN: /^[A-Z0-9]{2}$/,         // Two character type codes
  CATEGORY_PATTERN: /^[A-Z0-9]{4}$/,                 // Four character category codes
  
  // Status Patterns
  STATUS_PATTERN: /^[YN]$/,                          // Y/N status flags
  SELECTION_PATTERN: /^[X\s]$/,                      // X for selected, space for not
  
  // Validation Messages
  VALIDATION_MESSAGES: {
    ACCOUNT_NUMBER: 'Account number must be exactly 11 digits',
    CARD_NUMBER: 'Card number must be exactly 16 digits',
    REQUIRED_FIELD: 'This field is required',
    INVALID_FORMAT: 'Invalid format for this field',
    NUMERIC_ONLY: 'Only numeric characters allowed',
    INVALID_DATE: 'Invalid date format',
    INVALID_CURRENCY: 'Invalid currency amount format',
    FIELD_TOO_LONG: 'Field exceeds maximum length',
    FIELD_TOO_SHORT: 'Field does not meet minimum length',
  } as const,
  
  // Input Masks (for react-input-mask or similar)
  INPUT_MASKS: {
    ACCOUNT_NUMBER: '99999999999',
    CARD_NUMBER: '9999 9999 9999 9999',
    PHONE_NUMBER: '(999) 999-9999',
    SSN: '999-99-9999',
    DATE_SHORT: '99/99/99',
    DATE_LONG: '9999-99-99',
    ZIP_CODE: '99999',
    ZIP_PLUS_4: '99999-9999',
  } as const,
  
  // Format Functions
  FORMAT_FUNCTIONS: {
    formatCurrency: (value: number): string => 
      new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD',
        minimumFractionDigits: 2
      }).format(value),
      
    formatAccountNumber: (value: string): string =>
      value.replace(/(\d{4})(\d{4})(\d{3})/, '$1-$2-$3'),
      
    formatCardNumber: (value: string): string =>
      value.replace(/(\d{4})(\d{4})(\d{4})(\d{4})/, '$1 $2 $3 $4'),
      
    formatPhone: (value: string): string =>
      value.replace(/(\d{3})(\d{3})(\d{4})/, '($1) $2-$3'),
      
    formatSSN: (value: string): string =>
      value.replace(/(\d{3})(\d{2})(\d{4})/, '$1-$2-$3'),
      
    formatDate: (value: string): string => {
      const date = new Date(value);
      return date.toLocaleDateString('en-US', {
        month: '2-digit',
        day: '2-digit',
        year: '2-digit'
      });
    },
  } as const,
} as const;

// ============================================================================
// FIELD TYPE DEFINITIONS
// ============================================================================

/**
 * TypeScript type definitions for field attributes and properties
 */
export type BMSAttribute = keyof typeof BMS_ATTRIBUTES;
export type FieldLength = keyof typeof FIELD_LENGTHS.LENGTH_CONSTRAINTS;
export type AttributeMapping = keyof typeof ATTRIBUTE_MAPPINGS;
export type FormatPattern = keyof typeof FORMAT_PATTERNS;

/**
 * Field definition interface combining all field properties
 */
export interface FieldDefinition {
  name: string;
  length: number;
  attributes: BMSAttribute[];
  position: { row: number; col: number };
  validation?: {
    pattern?: RegExp;
    required?: boolean;
    message?: string;
  };
  formatting?: {
    mask?: string;
    display?: (value: any) => string;
  };
  component?: {
    type: string;
    props: Record<string, any>;
    styles?: Record<string, any>;
  };
}

/**
 * Screen definition interface for complete BMS map representation
 */
export interface ScreenDefinition {
  name: string;
  size: { width: number; height: number };
  fields: Record<string, FieldDefinition>;
  title: string;
  functionKeys: string[];
}

// Export all constants as a single object for convenience
export const FieldConstants = {
  BMS_ATTRIBUTES,
  FIELD_LENGTHS,
  FIELD_POSITIONING,
  ATTRIBUTE_MAPPINGS,
  FORMAT_PATTERNS,
} as const;

export default FieldConstants;