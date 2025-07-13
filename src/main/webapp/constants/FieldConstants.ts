/**
 * CardDemo Field Constants
 * 
 * TypeScript constants file containing all field-related definitions extracted from BMS maps.
 * Provides centralized field configuration for React components ensuring exact preservation
 * of original BMS field behaviors while enabling modern TypeScript type checking.
 * 
 * This file maps all BMS DFHMDF field definitions to TypeScript constants for:
 * - Field lengths and validation rules
 * - Positioning coordinates and layout
 * - Attribute mappings for field behavior
 * - Format patterns for input/output validation
 */

// =============================================================================
// BMS FIELD ATTRIBUTES - Direct mapping from BMS ATTRB definitions
// =============================================================================

/**
 * BMS field attributes as TypeScript enum for compile-time type safety.
 * Maps directly to CICS BMS ATTRB parameter values maintaining exact behavior.
 */
export const BMS_ATTRIBUTES = {
  // Field protection attributes
  ASKIP: 'ASKIP' as const,     // Skip field - display only, cursor skips over
  UNPROT: 'UNPROT' as const,   // Unprotected - allows user input
  PROT: 'PROT' as const,       // Protected - display only, no input allowed
  
  // Input validation attributes  
  NUM: 'NUM' as const,         // Numeric only input validation
  IC: 'IC' as const,           // Initial cursor - cursor positioned here first
  FSET: 'FSET' as const,       // Field set flag for BMS processing
  
  // Display intensity attributes
  NORM: 'NORM' as const,       // Normal intensity display
  BRT: 'BRT' as const,         // Bright intensity display
  DRK: 'DRK' as const,         // Dark intensity display (password fields)
  
  // Validation flags
  MUSTFILL: 'MUSTFILL' as const, // Required field validation
} as const;

// Type for BMS attribute values
export type BMSAttribute = typeof BMS_ATTRIBUTES[keyof typeof BMS_ATTRIBUTES];

// =============================================================================
// FIELD POSITIONING - BMS POS attribute mappings to CSS Grid coordinates
// =============================================================================

/**
 * Field positioning constants extracted from BMS POS attributes.
 * Converts 3270 terminal coordinates to modern CSS Grid layout system.
 */
export const FIELD_POSITIONING = {
  /**
   * Direct position mapping from BMS POS(row,col) to CSS Grid coordinates.
   * Maintains exact 24x80 terminal layout dimensions.
   */
  POSITION_MAPPING: {
    // Terminal boundaries - 3270 standard 24 rows x 80 columns
    MAX_ROWS: 24,
    MAX_COLS: 80,
    
    // Common header positions from COSGN00, COACTVW, COACTUP patterns
    HEADER_ROW_1: 1,
    HEADER_ROW_2: 2,
    HEADER_ROW_3: 3,
    
    // Standard field positions extracted from BMS maps
    TRAN_LABEL: { row: 1, col: 1 },      // "Tran:" label position
    TRAN_NAME: { row: 1, col: 7 },       // Transaction name field
    TITLE_01: { row: 1, col: 21 },       // Primary title position
    DATE_LABEL: { row: 1, col: 65 },     // "Date:" label position  
    CURRENT_DATE: { row: 1, col: 71 },   // Date field position
    
    PROG_LABEL: { row: 2, col: 1 },      // "Prog:" label position
    PROG_NAME: { row: 2, col: 7 },       // Program name field
    TITLE_02: { row: 2, col: 21 },       // Secondary title position
    TIME_LABEL: { row: 2, col: 65 },     // "Time:" label position
    CURRENT_TIME: { row: 2, col: 71 },   // Time field position
    
    // Login screen specific positions from COSGN00.bms
    USER_ID_LABEL: { row: 19, col: 29 }, // "User ID     :" label
    USER_ID_FIELD: { row: 19, col: 43 }, // User ID input field
    PASSWORD_LABEL: { row: 20, col: 29 }, // "Password    :" label  
    PASSWORD_FIELD: { row: 20, col: 43 }, // Password input field
    
    // Error message positioning
    ERROR_MSG: { row: 23, col: 1 },      // Error message line
    FUNCTION_KEYS: { row: 24, col: 1 },  // Function key instructions
  },
  
  /**
   * CSS Grid coordinate conversion utilities.
   * Converts BMS POS(row,col) to CSS Grid row/column values.
   */
  GRID_COORDINATES: {
    /**
     * Convert BMS row/col position to CSS Grid coordinates
     */
    toGridPosition: (row: number, col: number) => ({
      gridRow: row,
      gridColumn: col,
    }),
    
    /**
     * Convert position object to CSS Grid area string
     */
    toGridArea: (startRow: number, startCol: number, endRow?: number, endCol?: number) => 
      `${startRow} / ${startCol} / ${endRow || startRow + 1} / ${endCol || startCol + 1}`,
  },
  
  /**
   * Layout calculation utilities for field positioning.
   */
  LAYOUT_UTILS: {
    /**
     * Calculate field span based on BMS LENGTH attribute
     */
    calculateFieldSpan: (length: number) => Math.max(1, Math.ceil(length / 1)),
    
    /**
     * Validate position is within terminal boundaries
     */
    validatePosition: (row: number, col: number) => 
      row >= 1 && row <= 24 && col >= 1 && col <= 80,
    
    /**
     * Calculate next field position based on current field length
     */
    getNextFieldPosition: (currentRow: number, currentCol: number, fieldLength: number) => {
      const nextCol = currentCol + fieldLength;
      return nextCol <= 80 
        ? { row: currentRow, col: nextCol }
        : { row: currentRow + 1, col: 1 };
    },
  },
} as const;

// =============================================================================
// FIELD LENGTHS - BMS LENGTH attribute definitions
// =============================================================================

/**
 * Field length constraints extracted from BMS LENGTH definitions.
 * Maintains exact character counts for Material-UI TextField components.
 */
export const FIELD_LENGTHS = {
  /**
   * Standard field length constraints from BMS maps analysis.
   */
  LENGTH_CONSTRAINTS: {
    // Authentication fields from COSGN00.bms
    USER_ID: 8,           // USERID field LENGTH=8
    PASSWORD: 8,          // PASSWD field LENGTH=8
    TRANSACTION_NAME: 4,  // TRNNAME field LENGTH=4
    PROGRAM_NAME: 8,      // PGMNAME field LENGTH=8
    
    // Date/Time fields
    DATE_FIELD: 8,        // CURDATE field LENGTH=8 (mm/dd/yy)
    TIME_FIELD: 9,        // CURTIME field LENGTH=9 (hh:mm:ss)
    
    // Account fields from COACTVW.bms and COACTUP.bms
    ACCOUNT_NUMBER: 11,   // ACCTSID field LENGTH=11 (99999999999)
    ACCOUNT_STATUS: 1,    // ACSTTUS field LENGTH=1 (Y/N)
    CREDIT_LIMIT: 15,     // ACRDLIM field LENGTH=15 (currency format)
    CURRENT_BALANCE: 15,  // ACURBAL field LENGTH=15 (currency format)
    
    // Customer fields
    CUSTOMER_ID: 9,       // ACSTNUM field LENGTH=9
    SSN_FULL: 12,         // ACSTSSN field LENGTH=12 (999-99-9999)
    SSN_PART1: 3,         // ACTSSN1 field LENGTH=3 (999)
    SSN_PART2: 2,         // ACTSSN2 field LENGTH=2 (99) 
    SSN_PART3: 4,         // ACTSSN3 field LENGTH=4 (9999)
    FICO_SCORE: 3,        // ACSTFCO field LENGTH=3
    
    // Name fields
    FIRST_NAME: 25,       // ACSFNAM field LENGTH=25
    MIDDLE_NAME: 25,      // ACSMNAM field LENGTH=25
    LAST_NAME: 25,        // ACSLNAM field LENGTH=25
    
    // Address fields
    ADDRESS_LINE1: 50,    // ACSADL1 field LENGTH=50
    ADDRESS_LINE2: 50,    // ACSADL2 field LENGTH=50
    CITY: 50,             // ACSCITY field LENGTH=50
    STATE: 2,             // ACSSTTE field LENGTH=2
    ZIP_CODE: 5,          // ACSZIPC field LENGTH=5
    COUNTRY: 3,           // ACSCTRY field LENGTH=3
    
    // Phone fields
    PHONE_AREA: 3,        // ACSPH1A field LENGTH=3
    PHONE_EXCHANGE: 3,    // ACSPH1B field LENGTH=3  
    PHONE_NUMBER: 4,      // ACSPH1C field LENGTH=4
    PHONE_FULL: 13,       // ACSPHN1 field LENGTH=13
    
    // Transaction fields
    TRANSACTION_ID: 16,   // TRNIDIN field LENGTH=16
    CARD_NUMBER: 16,      // CARDNUM field LENGTH=16
    TRANSACTION_TYPE: 2,  // TTYPCD field LENGTH=2
    TRANSACTION_CATEGORY: 4, // TCATCD field LENGTH=4
    
    // Government and financial fields
    GOVT_ID: 20,          // ACSGOVT field LENGTH=20
    EFT_ACCOUNT: 10,      // ACSEFTC field LENGTH=10
    PRIMARY_FLAG: 1,      // ACSPFLG field LENGTH=1
    
    // Date component fields (from COACTUP.bms)
    YEAR_FIELD: 4,        // OPNYEAR, EXPYEAR, etc. LENGTH=4
    MONTH_FIELD: 2,       // OPNMON, EXPMON, etc. LENGTH=2
    DAY_FIELD: 2,         // OPNDAY, EXPDAY, etc. LENGTH=2
    
    // Message fields
    ERROR_MESSAGE: 78,    // ERRMSG field LENGTH=78
    INFO_MESSAGE: 45,     // INFOMSG field LENGTH=45
    TITLE_FIELD: 40,      // TITLE01, TITLE02 field LENGTH=40
  },
  
  /**
   * Maximum length constraints for validation.
   */
  MAX_LENGTHS: {
    SINGLE_CHAR: 1,
    SHORT_CODE: 4,
    MEDIUM_TEXT: 25,
    LONG_TEXT: 50,
    FULL_LINE: 78,
    SCREEN_WIDTH: 80,
  },
  
  /**
   * Default field sizes for common input types.
   */
  DEFAULT_SIZES: {
    TEXT_INPUT: 25,
    NUMBER_INPUT: 15,
    CODE_INPUT: 8,
    FLAG_INPUT: 1,
    MESSAGE_OUTPUT: 78,
  },
} as const;

// =============================================================================
// ATTRIBUTE MAPPINGS - BMS attributes to Material-UI properties
// =============================================================================

/**
 * Mapping constants for converting BMS field attributes to Material-UI component properties.
 * Preserves original BMS field behavior in modern React components.
 */
export const ATTRIBUTE_MAPPINGS = {
  /**
   * BMS ASKIP attribute mapping to Material-UI readonly property.
   * ASKIP fields are display-only, cursor skips over them.
   */
  ASKIP_TO_READONLY: {
    condition: BMS_ATTRIBUTES.ASKIP,
    materialUIProps: {
      InputProps: { readOnly: true },
      variant: 'filled' as const,
      disabled: false,  // Not disabled, just readonly for accessibility
    },
    cssClass: 'bms-askip-field',
  },
  
  /**
   * BMS UNPROT attribute mapping to Material-UI editable property.
   * UNPROT fields allow user input and modification.
   */
  UNPROT_TO_EDITABLE: {
    condition: BMS_ATTRIBUTES.UNPROT,
    materialUIProps: {
      InputProps: { readOnly: false },
      variant: 'outlined' as const,
      disabled: false,
    },
    cssClass: 'bms-unprot-field',
  },
  
  /**
   * BMS PROT attribute mapping to Material-UI disabled property.
   * PROT fields are completely protected from user interaction.
   */
  PROT_TO_DISABLED: {
    condition: BMS_ATTRIBUTES.PROT,
    materialUIProps: {
      disabled: true,
      variant: 'filled' as const,
      InputProps: { readOnly: true },
    },
    cssClass: 'bms-prot-field',
  },
  
  /**
   * BMS NUM attribute mapping to Material-UI numeric validation.
   * NUM fields accept only numeric input with validation.
   */
  NUM_TO_NUMERIC: {
    condition: BMS_ATTRIBUTES.NUM,
    materialUIProps: {
      type: 'number' as const,
      inputProps: { 
        inputMode: 'numeric' as const,
        pattern: '[0-9]*',
      },
    },
    validation: {
      pattern: /^[0-9]*$/,
      message: 'Only numeric characters are allowed',
    },
    cssClass: 'bms-num-field',
  },
  
  /**
   * BMS IC attribute mapping to Material-UI autoFocus property.
   * IC fields receive initial cursor position when screen loads.
   */
  IC_TO_AUTOFOCUS: {
    condition: BMS_ATTRIBUTES.IC,
    materialUIProps: {
      autoFocus: true,
    },
    cssClass: 'bms-ic-field',
  },
  
  /**
   * BMS MUSTFILL validation mapping to Material-UI required property.
   * MUSTFILL fields must have a value before form submission.
   */
  MUSTFILL_TO_REQUIRED: {
    condition: BMS_ATTRIBUTES.MUSTFILL,
    materialUIProps: {
      required: true,
    },
    validation: {
      required: 'This field is required',
    },
    cssClass: 'bms-mustfill-field',
  },
  
  /**
   * BMS intensity attribute mappings to Material-UI styling.
   */
  INTENSITY_MAPPINGS: {
    [BMS_ATTRIBUTES.NORM]: {
      color: 'primary' as const,
      cssClass: 'bms-norm-intensity',
    },
    [BMS_ATTRIBUTES.BRT]: {
      color: 'secondary' as const,
      cssClass: 'bms-brt-intensity',
      sx: { fontWeight: 'bold' },
    },
    [BMS_ATTRIBUTES.DRK]: {
      type: 'password' as const,
      cssClass: 'bms-drk-intensity',
      sx: { opacity: 0.7 },
    },
  },
  
  /**
   * BMS color attribute mappings to Material-UI color themes.
   */
  COLOR_MAPPINGS: {
    BLUE: { color: 'primary' as const },
    YELLOW: { color: 'warning' as const },
    TURQUOISE: { color: 'info' as const },
    GREEN: { color: 'success' as const },
    RED: { color: 'error' as const },
    NEUTRAL: { color: 'inherit' as const },
  },
  
  /**
   * BMS highlight attribute mappings to Material-UI styling.
   */
  HIGHLIGHT_MAPPINGS: {
    UNDERLINE: {
      sx: { textDecoration: 'underline' },
      cssClass: 'bms-underline',
    },
    OFF: {
      sx: { textDecoration: 'none' },
      cssClass: 'bms-highlight-off',
    },
  },
  
  /**
   * BMS justify attribute mappings to Material-UI text alignment.
   */
  JUSTIFY_MAPPINGS: {
    LEFT: { sx: { textAlign: 'left' } },
    RIGHT: { sx: { textAlign: 'right' } },
    CENTER: { sx: { textAlign: 'center' } },
  },
} as const;

// =============================================================================
// FORMAT PATTERNS - BMS PICIN/PICOUT definitions for validation
// =============================================================================

/**
 * Format patterns extracted from BMS PICIN and PICOUT definitions.
 * Provides input validation and output formatting for field data.
 */
export const FORMAT_PATTERNS = {
  /**
   * Input patterns from BMS PICIN definitions for field validation.
   */
  INPUT_PATTERNS: {
    // Numeric patterns from PICIN definitions
    ACCOUNT_NUMBER: {
      pattern: '99999999999',    // ACCTSID PICIN='99999999999'
      regex: /^\d{11}$/,
      mask: '99999999999',
      placeholder: '11-digit account number',
    },
    
    // Date patterns for date field validation
    DATE_YYYY_MM_DD: {
      pattern: '9999-99-99',
      regex: /^\d{4}-\d{2}-\d{2}$/,
      mask: '9999-99-99',
      placeholder: 'YYYY-MM-DD',
    },
    
    DATE_MM_DD_YY: {
      pattern: '99/99/99',
      regex: /^\d{2}\/\d{2}\/\d{2}$/,
      mask: '99/99/99',
      placeholder: 'MM/DD/YY',
    },
    
    // Time patterns
    TIME_HH_MM_SS: {
      pattern: '99:99:99',
      regex: /^\d{2}:\d{2}:\d{2}$/,
      mask: '99:99:99',
      placeholder: 'HH:MM:SS',
    },
    
    // SSN patterns for different parts
    SSN_FULL: {
      pattern: '999-99-9999',
      regex: /^\d{3}-\d{2}-\d{4}$/,
      mask: '999-99-9999',
      placeholder: 'XXX-XX-XXXX',
    },
    
    SSN_PART1: {
      pattern: '999',
      regex: /^\d{3}$/,
      mask: '999',
      placeholder: '999',
    },
    
    SSN_PART2: {
      pattern: '99',
      regex: /^\d{2}$/,
      mask: '99', 
      placeholder: '99',
    },
    
    SSN_PART3: {
      pattern: '9999',
      regex: /^\d{4}$/,
      mask: '9999',
      placeholder: '9999',
    },
    
    // Phone number patterns  
    PHONE_FULL: {
      pattern: '(999) 999-9999',
      regex: /^\(\d{3}\) \d{3}-\d{4}$/,
      mask: '(999) 999-9999',
      placeholder: '(XXX) XXX-XXXX',
    },
    
    PHONE_AREA: {
      pattern: '999',
      regex: /^\d{3}$/,
      mask: '999',
      placeholder: 'XXX',
    },
    
    // Card number pattern
    CARD_NUMBER: {
      pattern: '9999 9999 9999 9999',
      regex: /^\d{4} \d{4} \d{4} \d{4}$/,
      mask: '9999 9999 9999 9999',
      placeholder: 'XXXX XXXX XXXX XXXX',
    },
    
    // FICO score pattern
    FICO_SCORE: {
      pattern: '999',
      regex: /^[3-8]\d{2}$/,  // FICO scores range 300-850
      mask: '999',
      placeholder: '300-850',
    },
  },
  
  /**
   * Output patterns from BMS PICOUT definitions for display formatting.
   */
  OUTPUT_PATTERNS: {
    // Currency formatting from PICOUT='+ZZZ,ZZZ,ZZZ.99'
    CURRENCY_AMOUNT: {
      pattern: '+ZZZ,ZZZ,ZZZ.99',
      formatter: (value: number) => 
        new Intl.NumberFormat('en-US', {
          style: 'currency',
          currency: 'USD',
          minimumFractionDigits: 2,
          maximumFractionDigits: 2,
        }).format(value),
      parser: (value: string) => 
        parseFloat(value.replace(/[^0-9.-]/g, '')),
    },
    
    // Positive currency without sign
    CURRENCY_POSITIVE: {
      pattern: 'ZZZ,ZZZ,ZZZ.99',
      formatter: (value: number) => 
        Math.abs(value).toLocaleString('en-US', {
          minimumFractionDigits: 2,
          maximumFractionDigits: 2,
        }),
    },
    
    // Right-justified numeric
    NUMERIC_RIGHT: {
      pattern: 'ZZZ,ZZZ,ZZ9',
      formatter: (value: number) => 
        value.toLocaleString('en-US').padStart(10),
    },
    
    // Date formatting
    DATE_DISPLAY: {
      pattern: 'MM/DD/YYYY',
      formatter: (date: Date) => 
        date.toLocaleDateString('en-US', {
          month: '2-digit',
          day: '2-digit',
          year: 'numeric',
        }),
    },
    
    // Time formatting
    TIME_DISPLAY: {
      pattern: 'HH:MM:SS',
      formatter: (date: Date) => 
        date.toLocaleTimeString('en-US', {
          hour12: false,
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit',
        }),
    },
  },
  
  /**
   * Validation utility functions for format checking.
   */
  VALIDATION_UTILS: {
    /**
     * Validate input against BMS PICIN pattern
     */
    validateInput: (value: string, patternKey: keyof typeof FORMAT_PATTERNS.INPUT_PATTERNS) => {
      const pattern = FORMAT_PATTERNS.INPUT_PATTERNS[patternKey];
      return pattern.regex.test(value);
    },
    
    /**
     * Format value according to BMS PICOUT pattern
     */
    formatOutput: (value: any, patternKey: keyof typeof FORMAT_PATTERNS.OUTPUT_PATTERNS) => {
      const pattern = FORMAT_PATTERNS.OUTPUT_PATTERNS[patternKey];
      return pattern.formatter(value);
    },
    
    /**
     * Apply input mask to value
     */
    applyMask: (value: string, mask: string) => {
      let result = '';
      let valueIndex = 0;
      
      for (let i = 0; i < mask.length && valueIndex < value.length; i++) {
        if (mask[i] === '9') {
          if (/\d/.test(value[valueIndex])) {
            result += value[valueIndex];
            valueIndex++;
          } else {
            break;
          }
        } else {
          result += mask[i];
        }
      }
      
      return result;
    },
  },
} as const;

// =============================================================================
// TYPE DEFINITIONS
// =============================================================================

/**
 * TypeScript type definitions for field configuration objects.
 */
export type FieldPosition = {
  row: number;
  col: number;
};

export type FieldLength = number;

export type AttributeMapping = {
  condition: BMSAttribute;
  materialUIProps: Record<string, any>;
  cssClass?: string;
  validation?: {
    pattern?: RegExp;
    required?: string;
    message?: string;
  };
};

export type FormatPattern = {
  pattern: string;
  regex?: RegExp;
  mask?: string;
  placeholder?: string;
  formatter?: (value: any) => string;
  parser?: (value: string) => any;
};

/**
 * Complete field configuration type combining all BMS attributes.
 */
export type BMSFieldConfig = {
  name: string;
  length: FieldLength;
  position: FieldPosition;
  attributes: BMSAttribute[];
  inputPattern?: keyof typeof FORMAT_PATTERNS.INPUT_PATTERNS;
  outputPattern?: keyof typeof FORMAT_PATTERNS.OUTPUT_PATTERNS;
  color?: string;
  highlight?: string;
  justify?: 'LEFT' | 'RIGHT' | 'CENTER';
  validation?: {
    required?: boolean;
    pattern?: RegExp;
    message?: string;
  };
};

/**
 * Default export combining all field constants for easy import.
 */
export default {
  BMS_ATTRIBUTES,
  FIELD_POSITIONING,
  FIELD_LENGTHS,
  ATTRIBUTE_MAPPINGS,
  FORMAT_PATTERNS,
} as const;