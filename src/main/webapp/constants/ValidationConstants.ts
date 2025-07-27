/**
 * Validation Constants for CardDemo Application
 * 
 * This file contains comprehensive input validation rules and patterns extracted 
 * from BMS field definitions and copybook structures. Provides Yup schema 
 * configurations, regex patterns, and validation messages for React Hook Form 
 * ensuring exact preservation of original COBOL validation logic while enabling 
 * modern client-side validation.
 * 
 * Extracted from BMS maps: COSGN00, COACTVW, COACTUP, COCRDLI, COCRDSL, 
 * COCRDUP, COTRN00, COTRN01, COTRN02, COBIL00, CORPT00, COMEN01, COADM01,
 * COUSR00, COUSR01, COUSR02, COUSR03 and corresponding copybooks.
 */

import { number } from 'yup';
import { FIELD_LENGTHS } from './FieldConstants';

// ============================================================================
// PICIN PATTERNS - Extracted from BMS PICIN Definitions
// ============================================================================

/**
 * Numeric patterns from BMS PICIN definitions
 * These correspond directly to PICIN='pattern' in BMS maps
 */
export const PICIN_PATTERNS = {
  
  NUMERIC_PATTERNS: {
    // Account and Card Numbers (from COACTVW.bms PICIN='99999999999')
    ACCOUNT_NUMBER: /^[0-9]{11}$/,           // Exactly 11 digits
    CARD_NUMBER: /^[0-9]{16}$/,              // Exactly 16 digits  
    CARD_NUMBER_PARTIAL: /^[0-9]{1,16}$/,    // Up to 16 digits for input
    
    // Transaction and ID patterns
    TRANSACTION_ID: /^[A-Z0-9]{16}$/,        // 16 alphanumeric characters
    CUSTOMER_ID: /^[0-9]{9}$/,               // Exactly 9 digits
    USER_ID: /^[A-Z0-9]{1,8}$/i,             // Up to 8 alphanumeric
    
    // Financial amount patterns (supporting COBOL COMP-3 precision)
    AMOUNT_WHOLE: /^[0-9]{1,10}$/,           // Whole dollar amounts
    AMOUNT_DECIMAL: /^[0-9]{1,10}(\.[0-9]{1,2})?$/, // With cents
    CREDIT_LIMIT: /^[0-9]{1,13}(\.[0-9]{2})?$/, // Up to 999,999,999.99
    BALANCE_AMOUNT: /^-?[0-9]{1,13}(\.[0-9]{2})?$/, // Signed amounts
    
    // Code patterns
    TRANSACTION_TYPE: /^[A-Z0-9]{2}$/,       // 2-character type codes
    CATEGORY_CODE: /^[A-Z0-9]{4}$/,          // 4-character category codes
    FICO_SCORE: /^[0-9]{3}$/,                // 3-digit FICO score (300-850)
    PAGE_NUMBER: /^[0-9]{1,3}$/,             // Page numbers
  } as const,

  ALPHANUMERIC_PATTERNS: {
    // Name patterns (preserving COBOL PIC X validation)
    FIRST_NAME: /^[A-Za-z\s\-'\.]{1,25}$/,    // Names with common punctuation
    MIDDLE_NAME: /^[A-Za-z\s\-'\.]{0,25}$/,   // Optional middle name
    LAST_NAME: /^[A-Za-z\s\-'\.]{1,25}$/,     // Required last name
    FULL_NAME: /^[A-Za-z\s\-'\.]{1,75}$/,     // Combined name fields
    
    // Address patterns
    ADDRESS_LINE: /^[A-Za-z0-9\s\-'\.#\/]{1,50}$/, // Address with numbers/symbols
    CITY: /^[A-Za-z\s\-'\.]{1,50}$/,          // City names
    STATE: /^[A-Z]{2}$/,                      // 2-letter state codes
    ZIP_CODE: /^[0-9]{5}$/,                   // 5-digit ZIP
    ZIP_EXTENDED: /^[0-9]{5}-[0-9]{4}$/,      // ZIP+4 format
    COUNTRY: /^[A-Z]{3}$/,                    // 3-letter country codes
    
    // Contact information patterns
    PHONE_DIGITS: /^[0-9]{10}$/,              // 10 digits for phone
    GOVERNMENT_ID: /^[A-Z0-9]{1,20}$/,        // Government issued ID
    EFT_ACCOUNT: /^[A-Z0-9]{1,10}$/,          // EFT account identifier
    
    // Transaction description
    TRANSACTION_DESC: /^[A-Za-z0-9\s\-'\.#\/]{1,60}$/, // Transaction descriptions
    MERCHANT_NAME: /^[A-Za-z0-9\s\-'\.#\/&]{1,30}$/,   // Merchant names
    TRANSACTION_SOURCE: /^[A-Z0-9]{1,10}$/,    // Transaction source codes
  } as const,

  ACCOUNT_PATTERNS: {
    // Account-specific validation patterns
    ACCOUNT_STATUS: /^[YN]$/,                 // Y/N active status
    ACCOUNT_GROUP: /^[A-Z0-9]{1,10}$/,        // Account group codes
    CARD_STATUS: /^[ACDEIS]$/,                // Active/Closed/Disabled/Expired/Issued/Suspended
    PRIMARY_FLAG: /^[YN]$/,                   // Primary cardholder flag
    SELECTION_FLAG: /^[X\s]$/,                // X for selected, space for not
  } as const,

  VALIDATION_REGEX: {
    // Date patterns (matching BMS date formatting)
    DATE_SHORT: /^(0[1-9]|1[0-2])\/(0[1-9]|[12]\d|3[01])\/\d{2}$/, // mm/dd/yy
    DATE_LONG: /^\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01])$/,    // yyyy-mm-dd
    TIME_24H: /^([01]\d|2[0-3]):([0-5]\d):([0-5]\d)$/,            // hh:mm:ss
    
    // SSN pattern (matching BMS SSN field structure)
    SSN_FULL: /^\d{3}-\d{2}-\d{4}$/,          // 999-99-9999 format
    SSN_DIGITS: /^\d{9}$/,                    // 9 digits only
    
    // User type validation (from COUSR01.bms)
    USER_TYPE: /^[AU]$/,                      // A=Admin, U=User
    
    // Password strength (preserving original 8-char requirement)
    PASSWORD_BASIC: /^.{8}$/,                 // Exactly 8 characters (original requirement)
    PASSWORD_STRONG: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*?&]{8}$/, // Enhanced with special chars
  } as const,
} as const;

// ============================================================================
// VALIDATION RULES - BMS Attribute Mapping to Yup Schemas  
// ============================================================================

/**
 * Validation rules extracted from BMS VALIDN and ATTRB attributes
 * Maps BMS validation attributes to Yup validation schema rules
 */
export const VALIDATION_RULES = {
  
  MUSTFILL: {
    // Maps to BMS VALIDN=(MUSTFILL) attribute
    required: true,
    errorMessage: 'This field is required',
    yupSchema: 'required',
    yupMessage: 'This field is required',
  } as const,

  NUM: {
    // Maps to BMS ATTRB=(NUM) attribute for numeric-only fields
    pattern: /^[0-9]*$/,
    errorMessage: 'Only numeric characters are allowed',
    yupSchema: 'matches',
    yupPattern: /^[0-9]*$/,
    yupMessage: 'Only numeric characters are allowed',
  } as const,

  REQUIRED_FIELDS: {
    // Fields that must be filled based on BMS analysis
    LOGIN: ['USERID', 'PASSWD'],
    ACCOUNT_SEARCH: ['ACCTSID'],
    CARD_SEARCH: ['ACCTSID'],
    TRANSACTION_ADD: ['TRNAMT', 'TDESC', 'TTYPCD', 'TCATCD'],
    USER_ADD: ['USERID', 'PASSWD', 'FNAME', 'LNAME', 'USRTYPE'],
    
    // Cross-field validation requirements
    SSN_COMPLETE: ['ACTSSN1', 'ACTSSN2', 'ACTSSN3'], // All SSN parts required
    DATE_COMPLETE: ['YEAR', 'MONTH', 'DAY'],          // All date parts required
    PHONE_COMPLETE: ['ACSPH1A', 'ACSPH1B', 'ACSPH1C'], // All phone parts required
  } as const,

  OPTIONAL_FIELDS: {
    // Fields that can be empty (ATTRB does not include MUSTFILL)
    CUSTOMER_INFO: ['ACSMNAM', 'ACSADL2', 'ACSPHN2', 'ACSGOVT'],
    ACCOUNT_DETAILS: ['AADDGRP', 'ACSEFTC'],
    TRANSACTION_OPTIONAL: ['TRNSRC', 'TORIGDT', 'TPROCDT'],
  } as const,
} as const;

// ============================================================================
// INPUT MASKS - Format Templates for Masked Input Components
// ============================================================================

/**
 * Input masks for formatted field entry
 * Based on BMS field structures and COBOL data formatting requirements
 */
export const INPUT_MASKS = {
  
  SSN_MASK: '999-99-9999',                   // Social Security Number
  PHONE_MASK: '(999) 999-9999',              // Phone number formatting
  DATE_MASK: '99/99/99',                     // Short date format (mm/dd/yy)
  DATE_LONG_MASK: '9999-99-99',              // Long date format (yyyy-mm-dd)
  CARD_NUMBER_MASK: '9999 9999 9999 9999',   // Credit card formatting
  ACCOUNT_NUMBER_MASK: '99999999999',        // Account number (no formatting)
  
  // Additional masks for specific field types
  ZIP_MASK: '99999',                         // ZIP code
  ZIP_EXTENDED_MASK: '99999-9999',           // ZIP+4 format
  TIME_MASK: '99:99:99',                     // Time format (hh:mm:ss)
  AMOUNT_MASK: '9999999999.99',              // Currency amount
  
  // Partial masks for composite fields
  SSN_PART1_MASK: '999',                     // First 3 digits of SSN
  SSN_PART2_MASK: '99',                      // Middle 2 digits of SSN  
  SSN_PART3_MASK: '9999',                    // Last 4 digits of SSN
  PHONE_AREA_MASK: '999',                    // Area code
  PHONE_EXCHANGE_MASK: '999',                // Exchange
  PHONE_NUMBER_MASK: '9999',                 // Number
  
  // Date component masks
  YEAR_MASK: '9999',                         // 4-digit year
  MONTH_MASK: '99',                          // 2-digit month
  DAY_MASK: '99',                            // 2-digit day
} as const;

// ============================================================================
// FIELD CONSTRAINTS - Length and Range Validation
// ============================================================================

/**
 * Field constraints combining BMS LENGTH attributes with business rules
 * Ensures exact preservation of COBOL field size limits and data precision
 */
export const FIELD_CONSTRAINTS = {
  
  MIN_LENGTHS: {
    // Minimum required lengths for various field types
    USER_ID: 3,                              // Minimum user ID length
    PASSWORD: 8,                             // Exactly 8 characters (BMS requirement)
    FIRST_NAME: 1,                           // At least 1 character
    LAST_NAME: 1,                            // At least 1 character
    ACCOUNT_NUMBER: 11,                      // Exactly 11 digits
    CARD_NUMBER: 16,                         // Exactly 16 digits
    TRANSACTION_DESC: 1,                     // At least 1 character
    TRANSACTION_AMOUNT: 1,                   // At least some amount
    ZIP_CODE: 5,                             // Exactly 5 digits
    STATE: 2,                                // Exactly 2 characters
    COUNTRY: 3,                              // Exactly 3 characters
    PHONE_DIGITS: 10,                        // Exactly 10 digits
  } as const,

  MAX_LENGTHS: {
    // Maximum allowed lengths (from FIELD_LENGTHS.LENGTH_CONSTRAINTS)
    USER_ID: FIELD_LENGTHS.LENGTH_CONSTRAINTS.USER_ID,
    PASSWORD: FIELD_LENGTHS.LENGTH_CONSTRAINTS.PASSWORD,
    FIRST_NAME: FIELD_LENGTHS.LENGTH_CONSTRAINTS.FIRST_NAME,
    MIDDLE_NAME: FIELD_LENGTHS.LENGTH_CONSTRAINTS.MIDDLE_NAME,
    LAST_NAME: FIELD_LENGTHS.LENGTH_CONSTRAINTS.LAST_NAME,
    ACCOUNT_NUMBER: FIELD_LENGTHS.LENGTH_CONSTRAINTS.ACCOUNT_NUMBER,
    CARD_NUMBER: FIELD_LENGTHS.LENGTH_CONSTRAINTS.CARD_NUMBER,
    CUSTOMER_ID: FIELD_LENGTHS.LENGTH_CONSTRAINTS.CUSTOMER_ID,
    TRANSACTION_DESC: FIELD_LENGTHS.LENGTH_CONSTRAINTS.TRANSACTION_DESCRIPTION,
    ADDRESS_LINE_1: FIELD_LENGTHS.LENGTH_CONSTRAINTS.ADDRESS_LINE_1,
    ADDRESS_LINE_2: FIELD_LENGTHS.LENGTH_CONSTRAINTS.ADDRESS_LINE_2,
    CITY: FIELD_LENGTHS.LENGTH_CONSTRAINTS.CITY,
    PHONE_NUMBER: FIELD_LENGTHS.LENGTH_CONSTRAINTS.PHONE_NUMBER,
    GOVERNMENT_ID: FIELD_LENGTHS.LENGTH_CONSTRAINTS.GOVERNMENT_ID,
    ERROR_MESSAGE: FIELD_LENGTHS.LENGTH_CONSTRAINTS.ERROR_MESSAGE,
  } as const,

  RANGE_LIMITS: {
    // Numeric range constraints based on COBOL data definitions
    FICO_SCORE: { min: 300, max: 850 },       // Standard FICO range
    TRANSACTION_AMOUNT: { min: 0.01, max: 999999999.99 }, // COMP-3 precision
    CREDIT_LIMIT: { min: 0, max: 999999999.99 }, // Maximum credit limit
    BALANCE_AMOUNT: { min: -999999999.99, max: 999999999.99 }, // Signed balance
    PAGE_NUMBER: { min: 1, max: 999 },        // Page numbering
    YEAR: { min: 1900, max: 2099 },           // Date year range
    MONTH: { min: 1, max: 12 },               // Month range
    DAY: { min: 1, max: 31 },                 // Day range (basic validation)
    
    // Age-related constraints
    AGE_MINIMUM: 18,                          // Minimum age for account
    AGE_MAXIMUM: 120,                         // Maximum reasonable age
    
    // Security constraints
    PASSWORD_ATTEMPTS: 3,                     // Maximum login attempts
    SESSION_TIMEOUT: 30,                      // Session timeout in minutes
  } as const,

  BUSINESS_RULES: {
    // Business validation rules extracted from COBOL logic
    CREDIT_UTILIZATION_MAX: 100,              // Maximum credit utilization %
    CASH_ADVANCE_LIMIT_PCT: 50,               // Cash advance as % of credit limit
    TRANSACTION_DAILY_LIMIT: 50000.00,        // Daily transaction limit
    ACCOUNT_CLOSURE_BALANCE: 0.00,            // Account must be zero to close
    
    // Status validation rules
    VALID_ACCOUNT_STATUSES: ['Y', 'N'],       // Active/Inactive
    VALID_CARD_STATUSES: ['A', 'C', 'D', 'E', 'I', 'S'], // All valid card statuses
    VALID_USER_TYPES: ['A', 'U'],             // Admin/User
    VALID_PRIMARY_FLAGS: ['Y', 'N'],          // Yes/No primary cardholder
    
    // Date validation rules
    FUTURE_DATE_ALLOWED: ['EXPIRY', 'REISSUE'], // Fields that can have future dates
    PAST_DATE_REQUIRED: ['DOB', 'OPENED'],     // Fields that must be past dates
    
    // Cross-field validation rules
    EXPIRY_AFTER_OPENED: true,                // Expiry must be after opened date
    REISSUE_AFTER_OPENED: true,               // Reissue must be after opened date
    CASH_LIMIT_LE_CREDIT: true,               // Cash limit <= credit limit
  } as const,
} as const;

// ============================================================================
// YUP SCHEMA BUILDERS - Factory Functions for Validation Schemas
// ============================================================================

/**
 * Yup schema builder functions that create validation schemas
 * matching BMS field validation requirements exactly
 */
export const YUP_SCHEMA_BUILDERS = {
  
  /**
   * Creates a required string field with length validation
   */
  requiredString: (fieldName: string, minLength: number, maxLength: number) => ({
    schema: `string().required('${fieldName} is required').min(${minLength}, '${fieldName} must be at least ${minLength} characters').max(${maxLength}, '${fieldName} cannot exceed ${maxLength} characters')`,
    validation: {
      required: true,
      minLength,
      maxLength,
      pattern: null,
    },
  }),

  /**
   * Creates an optional string field with length validation
   */
  optionalString: (fieldName: string, maxLength: number) => ({
    schema: `string().max(${maxLength}, '${fieldName} cannot exceed ${maxLength} characters')`,
    validation: {
      required: false,
      maxLength,
      pattern: null,
    },
  }),

  /**
   * Creates a numeric field with range validation
   */
  numericField: (fieldName: string, min: number, max: number, precision: number = 2) => ({
    schema: `number().required('${fieldName} is required').min(${min}, '${fieldName} must be at least ${min}').max(${max}, '${fieldName} cannot exceed ${max}').test('decimal', '${fieldName} can have at most ${precision} decimal places', (value) => !value || (value * Math.pow(10, ${precision})) % 1 === 0)`,
    validation: {
      required: true,
      min,
      max,
      precision,
    },
  }),

  /**
   * Creates a pattern-based validation field
   */
  patternField: (fieldName: string, pattern: RegExp, message: string) => ({
    schema: `string().required('${fieldName} is required').matches(${pattern}, '${message}')`,
    validation: {
      required: true,
      pattern,
      message,
    },
  }),

  /**
   * Creates a date field with range validation
   */
  dateField: (fieldName: string, pastOnly: boolean = false, futureOnly: boolean = false) => {
    let schema = `date().required('${fieldName} is required').typeError('${fieldName} must be a valid date')`;
    
    if (pastOnly) {
      schema += `.max(new Date(), '${fieldName} cannot be in the future')`;
    } else if (futureOnly) {
      schema += `.min(new Date(), '${fieldName} cannot be in the past')`;
    }
    
    return {
      schema,
      validation: {
        required: true,
        type: 'date',
        pastOnly,
        futureOnly,
      },
    };
  },

  /**
   * Creates a select field with predefined options
   */
  selectField: (fieldName: string, options: string[], required: boolean = true) => ({
    schema: required 
      ? `string().required('${fieldName} is required').oneOf([${options.map(o => `'${o}'`).join(', ')}], '${fieldName} must be one of: ${options.join(', ')}')`
      : `string().oneOf([${options.map(o => `'${o}'`).join(', ')}, ''], '${fieldName} must be one of: ${options.join(', ')} or empty')`,
    validation: {
      required,
      options,
    },
  }),
} as const;

// ============================================================================
// VALIDATION MESSAGE TEMPLATES
// ============================================================================

/**
 * Standardized validation error messages matching original BMS error messaging
 * Ensures consistent user experience with original mainframe application
 */
export const VALIDATION_MESSAGES = {
  
  REQUIRED: {
    GENERIC: 'This field is required',
    USER_ID: 'User ID is required',
    PASSWORD: 'Password is required', 
    ACCOUNT_NUMBER: 'Account number is required',
    CARD_NUMBER: 'Card number is required',
    FIRST_NAME: 'First name is required',
    LAST_NAME: 'Last name is required',
    TRANSACTION_AMOUNT: 'Transaction amount is required',
    TRANSACTION_DESC: 'Transaction description is required',
  } as const,

  FORMAT: {
    INVALID_FORMAT: 'Invalid format for this field',
    NUMERIC_ONLY: 'Only numeric characters are allowed',
    ALPHA_ONLY: 'Only alphabetic characters are allowed',
    ALPHANUMERIC_ONLY: 'Only letters and numbers are allowed',
    INVALID_DATE: 'Invalid date format (mm/dd/yy)',
    INVALID_TIME: 'Invalid time format (hh:mm:ss)',
    INVALID_PHONE: 'Invalid phone number format',
    INVALID_SSN: 'Invalid Social Security Number format',
    INVALID_EMAIL: 'Invalid email address format',
  } as const,

  LENGTH: {
    TOO_SHORT: (field: string, min: number) => `${field} must be at least ${min} characters`,
    TOO_LONG: (field: string, max: number) => `${field} cannot exceed ${max} characters`,
    EXACT_LENGTH: (field: string, length: number) => `${field} must be exactly ${length} characters`,
    FIELD_TOO_LONG: 'Field exceeds maximum length',
    FIELD_TOO_SHORT: 'Field does not meet minimum length',
  } as const,

  RANGE: {
    TOO_SMALL: (field: string, min: number) => `${field} must be at least ${min}`,
    TOO_LARGE: (field: string, max: number) => `${field} cannot exceed ${max}`,
    OUT_OF_RANGE: (field: string, min: number, max: number) => `${field} must be between ${min} and ${max}`,
    INVALID_FICO: 'FICO score must be between 300 and 850',
    INVALID_AMOUNT: 'Invalid currency amount',
  } as const,

  business: {
    ACCOUNT_NOT_FOUND: 'Account number not found',
    CARD_NOT_FOUND: 'Card number not found',
    INVALID_LOGIN: 'Invalid User ID or Password',
    ACCOUNT_INACTIVE: 'Account is not active',
    CARD_EXPIRED: 'Card has expired',
    INSUFFICIENT_FUNDS: 'Insufficient available credit',
    DUPLICATE_USER: 'User ID already exists',
    INVALID_USER_TYPE: 'User type must be A (Admin) or U (User)',
    PASSWORD_COMPLEXITY: 'Password must be exactly 8 characters',
    FUTURE_DATE_NOT_ALLOWED: 'Future dates are not allowed for this field',
    PAST_DATE_NOT_ALLOWED: 'Past dates are not allowed for this field',
    INVALID_CROSS_REFERENCE: 'Cross-field validation failed',
    EXPIRY_BEFORE_OPENED: 'Expiry date cannot be before account opened date',
    CASH_LIMIT_EXCEEDS_CREDIT: 'Cash advance limit cannot exceed credit limit',
  } as const,

  SYSTEM: {
    VALIDATION_ERROR: 'Validation error occurred',
    NETWORK_ERROR: 'Network error - please try again',
    SERVER_ERROR: 'Server error - please contact support',
    SESSION_EXPIRED: 'Your session has expired - please log in again',
    ACCESS_DENIED: 'Access denied - insufficient privileges',
    SYSTEM_UNAVAILABLE: 'System temporarily unavailable',
  } as const,
} as const;

// ============================================================================
// CROSS-FIELD VALIDATION RULES
// ============================================================================

/**
 * Cross-field validation rules extracted from BMS field relationships
 * and business logic requirements from COBOL programs
 */
export const CROSS_FIELD_VALIDATION = {
  
  DATE_RELATIONSHIPS: {
    // Expiry date must be after opened date
    EXPIRY_AFTER_OPENED: {
      fields: ['OPNYEAR', 'OPNMON', 'OPNDAY', 'EXPYEAR', 'EXPMON', 'EXPDAY'],
      rule: 'expiry > opened',
      message: 'Expiry date must be after account opened date',
    },
    
    // Reissue date must be after opened date
    REISSUE_AFTER_OPENED: {
      fields: ['OPNYEAR', 'OPNMON', 'OPNDAY', 'RISYEAR', 'RISMON', 'RISDAY'],
      rule: 'reissue > opened',
      message: 'Reissue date must be after account opened date',
    },
    
    // Date of birth must be in the past
    DOB_IN_PAST: {
      fields: ['DOBYEAR', 'DOBMON', 'DOBDAY'],
      rule: 'dob < today',
      message: 'Date of birth must be in the past',
    },
  } as const,

  AMOUNT_RELATIONSHIPS: {
    // Cash advance limit cannot exceed credit limit
    CASH_LIMIT_LE_CREDIT: {
      fields: ['ACRDLIM', 'ACSHLIM'],
      rule: 'cashLimit <= creditLimit',
      message: 'Cash advance limit cannot exceed credit limit',
    },
    
    // Current balance cannot exceed credit limit
    BALANCE_LE_CREDIT: {
      fields: ['ACRDLIM', 'ACURBAL'],
      rule: 'balance <= creditLimit',
      message: 'Current balance cannot exceed credit limit',
    },
  } as const,

  COMPOSITE_FIELD_VALIDATION: {
    // SSN parts must all be provided
    SSN_COMPLETE: {
      fields: ['ACTSSN1', 'ACTSSN2', 'ACTSSN3'],
      rule: 'all_or_none',
      message: 'All SSN parts must be provided',
    },
    
    // Phone number parts must all be provided
    PHONE_COMPLETE: {
      fields: ['ACSPH1A', 'ACSPH1B', 'ACSPH1C'],
      rule: 'all_or_none',
      message: 'All phone number parts must be provided',
    },
    
    // Date parts must all be provided
    DATE_COMPLETE: {
      fields: ['YEAR', 'MONTH', 'DAY'],
      rule: 'all_or_none',
      message: 'Complete date must be provided',
    },
  } as const,

  CONDITIONAL_REQUIREMENTS: {
    // If account is active, certain fields are required
    ACTIVE_ACCOUNT_REQUIREMENTS: {
      condition: 'ACSTTUS === "Y"',
      requiredFields: ['ACRDLIM', 'ACSHLIM'],
      message: 'Credit limits are required for active accounts',
    },
    
    // If user type is Admin, additional validation applies
    ADMIN_USER_REQUIREMENTS: {
      condition: 'USRTYPE === "A"',
      requiredFields: ['FNAME', 'LNAME'],
      message: 'Full name is required for administrator accounts',
    },
  } as const,
} as const;

// ============================================================================
// FIELD-SPECIFIC VALIDATION CONFIGURATIONS
// ============================================================================

/**
 * Pre-configured validation schemas for specific field types
 * Ready-to-use validation configurations for common field patterns
 */
export const FIELD_VALIDATION_CONFIGS = {
  
  // Authentication fields
  USER_ID: {
    pattern: PICIN_PATTERNS.ALPHANUMERIC_PATTERNS.FIRST_NAME,
    minLength: FIELD_CONSTRAINTS.MIN_LENGTHS.USER_ID,
    maxLength: FIELD_CONSTRAINTS.MAX_LENGTHS.USER_ID,
    required: true,
    mask: null,
    yupSchema: YUP_SCHEMA_BUILDERS.patternField('User ID', PICIN_PATTERNS.NUMERIC_PATTERNS.USER_ID, 'User ID must be 1-8 alphanumeric characters'),
  },
  
  PASSWORD: {
    pattern: PICIN_PATTERNS.VALIDATION_REGEX.PASSWORD_BASIC,
    minLength: FIELD_CONSTRAINTS.MIN_LENGTHS.PASSWORD,
    maxLength: FIELD_CONSTRAINTS.MAX_LENGTHS.PASSWORD,
    required: true,
    mask: null,
    yupSchema: YUP_SCHEMA_BUILDERS.requiredString('Password', 8, 8),
  },
  
  // Account fields  
  ACCOUNT_NUMBER: {
    pattern: PICIN_PATTERNS.NUMERIC_PATTERNS.ACCOUNT_NUMBER,
    minLength: FIELD_CONSTRAINTS.MIN_LENGTHS.ACCOUNT_NUMBER,
    maxLength: FIELD_CONSTRAINTS.MAX_LENGTHS.ACCOUNT_NUMBER,
    required: true,
    mask: INPUT_MASKS.ACCOUNT_NUMBER_MASK,
    yupSchema: YUP_SCHEMA_BUILDERS.patternField('Account Number', PICIN_PATTERNS.NUMERIC_PATTERNS.ACCOUNT_NUMBER, 'Account number must be exactly 11 digits'),
  },
  
  CARD_NUMBER: {
    pattern: PICIN_PATTERNS.NUMERIC_PATTERNS.CARD_NUMBER,
    minLength: FIELD_CONSTRAINTS.MIN_LENGTHS.CARD_NUMBER,
    maxLength: FIELD_CONSTRAINTS.MAX_LENGTHS.CARD_NUMBER,
    required: true,
    mask: INPUT_MASKS.CARD_NUMBER_MASK,
    yupSchema: YUP_SCHEMA_BUILDERS.patternField('Card Number', PICIN_PATTERNS.NUMERIC_PATTERNS.CARD_NUMBER, 'Card number must be exactly 16 digits'),
  },
  
  // Name fields
  FIRST_NAME: {
    pattern: PICIN_PATTERNS.ALPHANUMERIC_PATTERNS.FIRST_NAME,
    minLength: FIELD_CONSTRAINTS.MIN_LENGTHS.FIRST_NAME,
    maxLength: FIELD_CONSTRAINTS.MAX_LENGTHS.FIRST_NAME,
    required: true,
    mask: null,
    yupSchema: YUP_SCHEMA_BUILDERS.patternField('First Name', PICIN_PATTERNS.ALPHANUMERIC_PATTERNS.FIRST_NAME, 'First name can only contain letters, spaces, hyphens, apostrophes, and periods'),
  },
  
  LAST_NAME: {
    pattern: PICIN_PATTERNS.ALPHANUMERIC_PATTERNS.LAST_NAME,
    minLength: FIELD_CONSTRAINTS.MIN_LENGTHS.LAST_NAME,
    maxLength: FIELD_CONSTRAINTS.MAX_LENGTHS.LAST_NAME,
    required: true,
    mask: null,
    yupSchema: YUP_SCHEMA_BUILDERS.patternField('Last Name', PICIN_PATTERNS.ALPHANUMERIC_PATTERNS.LAST_NAME, 'Last name can only contain letters, spaces, hyphens, apostrophes, and periods'),
  },
  
  // Contact fields
  PHONE_NUMBER: {
    pattern: PICIN_PATTERNS.VALIDATION_REGEX.SSN_DIGITS,
    minLength: FIELD_CONSTRAINTS.MIN_LENGTHS.PHONE_DIGITS,
    maxLength: FIELD_CONSTRAINTS.MIN_LENGTHS.PHONE_DIGITS,
    required: false,
    mask: INPUT_MASKS.PHONE_MASK,
    yupSchema: YUP_SCHEMA_BUILDERS.patternField('Phone Number', PICIN_PATTERNS.ALPHANUMERIC_PATTERNS.PHONE_DIGITS, 'Phone number must be exactly 10 digits'),
  },
  
  SSN: {
    pattern: PICIN_PATTERNS.VALIDATION_REGEX.SSN_FULL,
    minLength: 11, // Including dashes
    maxLength: 11,
    required: false,
    mask: INPUT_MASKS.SSN_MASK,
    yupSchema: YUP_SCHEMA_BUILDERS.patternField('Social Security Number', PICIN_PATTERNS.VALIDATION_REGEX.SSN_FULL, 'SSN must be in format 999-99-9999'),
  },
  
  // Amount fields
  TRANSACTION_AMOUNT: {
    pattern: PICIN_PATTERNS.NUMERIC_PATTERNS.AMOUNT_DECIMAL,
    minLength: FIELD_CONSTRAINTS.MIN_LENGTHS.TRANSACTION_AMOUNT,
    maxLength: FIELD_CONSTRAINTS.MAX_LENGTHS.TRANSACTION_DESC,
    required: true,
    mask: INPUT_MASKS.AMOUNT_MASK,
    yupSchema: YUP_SCHEMA_BUILDERS.numericField('Transaction Amount', FIELD_CONSTRAINTS.RANGE_LIMITS.TRANSACTION_AMOUNT.min, FIELD_CONSTRAINTS.RANGE_LIMITS.TRANSACTION_AMOUNT.max, 2),
  },
  
  // Status fields
  ACCOUNT_STATUS: {
    pattern: PICIN_PATTERNS.ACCOUNT_PATTERNS.ACCOUNT_STATUS,
    minLength: 1,
    maxLength: 1,
    required: true,
    mask: null,
    yupSchema: YUP_SCHEMA_BUILDERS.selectField('Account Status', FIELD_CONSTRAINTS.BUSINESS_RULES.VALID_ACCOUNT_STATUSES),
  },
  
  USER_TYPE: {
    pattern: PICIN_PATTERNS.VALIDATION_REGEX.USER_TYPE,
    minLength: 1,
    maxLength: 1,
    required: true,
    mask: null,
    yupSchema: YUP_SCHEMA_BUILDERS.selectField('User Type', FIELD_CONSTRAINTS.BUSINESS_RULES.VALID_USER_TYPES),
  },
} as const;

// ============================================================================
// EXPORT CONSOLIDATED VALIDATION CONSTANTS
// ============================================================================

/**
 * Main export object consolidating all validation constants
 * Provides centralized access to all validation rules and patterns
 */
export const ValidationConstants = {
  PICIN_PATTERNS,
  VALIDATION_RULES,
  INPUT_MASKS,
  FIELD_CONSTRAINTS,
  YUP_SCHEMA_BUILDERS,
  VALIDATION_MESSAGES,
  CROSS_FIELD_VALIDATION,
  FIELD_VALIDATION_CONFIGS,
} as const;

export default ValidationConstants;