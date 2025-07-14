/**
 * CardDemo Validation Constants
 * 
 * TypeScript constants file containing comprehensive input validation rules and patterns
 * extracted from BMS field definitions and copybook structures. Provides Yup schema
 * configurations, regex patterns, and validation messages for React Hook Form ensuring
 * exact preservation of original COBOL validation logic while enabling modern client-side
 * validation.
 * 
 * This file transforms all BMS VALIDN, PICIN, and MUSTFILL attributes into TypeScript
 * validation constants for React Hook Form with Yup schema validation.
 */

import { number } from 'yup';
import { FIELD_LENGTHS } from './FieldConstants';

// =============================================================================
// PICIN PATTERNS - Input validation patterns from BMS PICIN definitions
// =============================================================================

/**
 * PICIN validation patterns extracted from BMS field definitions.
 * Each pattern maintains exact COBOL validation logic for client-side validation.
 */
export const PICIN_PATTERNS = {
  /**
   * Numeric input patterns for fields with BMS PICIN numeric definitions.
   * Preserves exact COBOL numeric validation behavior.
   */
  NUMERIC_PATTERNS: {
    // Account number pattern from ACCTSID PICIN='99999999999'
    ACCOUNT_NUMBER: {
      pattern: /^\d{11}$/,
      message: 'Account number must be exactly 11 digits',
      mask: '99999999999',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.ACCOUNT_NUMBER,
    },
    
    // Card number pattern for 16-digit card numbers
    CARD_NUMBER: {
      pattern: /^\d{16}$/,
      message: 'Card number must be exactly 16 digits',
      mask: '9999999999999999',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.CARD_NUMBER,
    },
    
    // Customer ID pattern for 9-digit customer identifiers
    CUSTOMER_ID: {
      pattern: /^\d{9}$/,
      message: 'Customer ID must be exactly 9 digits',
      mask: '999999999',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.CUSTOMER_ID,
    },
    
    // FICO score pattern with business logic validation (300-850)
    FICO_SCORE: {
      pattern: /^[3-8]\d{2}$/,
      message: 'FICO score must be between 300 and 850',
      mask: '999',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.FICO_SCORE,
      min: 300,
      max: 850,
    },
    
    // Year field validation for 4-digit years
    YEAR_FIELD: {
      pattern: /^\d{4}$/,
      message: 'Year must be exactly 4 digits',
      mask: '9999',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.YEAR_FIELD,
      min: 1900,
      max: 2099,
    },
    
    // Month field validation (01-12)
    MONTH_FIELD: {
      pattern: /^(0[1-9]|1[0-2])$/,
      message: 'Month must be between 01 and 12',
      mask: '99',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.MONTH_FIELD,
      min: 1,
      max: 12,
    },
    
    // Day field validation (01-31)
    DAY_FIELD: {
      pattern: /^(0[1-9]|[12]\d|3[01])$/,
      message: 'Day must be between 01 and 31',
      mask: '99',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.DAY_FIELD,
      min: 1,
      max: 31,
    },
    
    // ZIP code validation for 5-digit ZIP codes
    ZIP_CODE: {
      pattern: /^\d{5}$/,
      message: 'ZIP code must be exactly 5 digits',
      mask: '99999',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.ZIP_CODE,
    },
    
    // Phone number component validations
    PHONE_AREA: {
      pattern: /^[2-9]\d{2}$/,
      message: 'Area code must be 3 digits starting with 2-9',
      mask: '999',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.PHONE_AREA,
    },
    
    PHONE_EXCHANGE: {
      pattern: /^[2-9]\d{2}$/,
      message: 'Exchange must be 3 digits starting with 2-9',
      mask: '999',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.PHONE_EXCHANGE,
    },
    
    PHONE_NUMBER: {
      pattern: /^\d{4}$/,
      message: 'Phone number must be exactly 4 digits',
      mask: '9999',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.PHONE_NUMBER,
    },
    
    // Transaction ID pattern for 16-character transaction identifiers
    TRANSACTION_ID: {
      pattern: /^[A-Z0-9]{16}$/,
      message: 'Transaction ID must be exactly 16 alphanumeric characters',
      mask: 'AAAAAAAAAAAAAAAA',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.TRANSACTION_ID,
    },
    
    // Numeric amount validation with decimal precision
    CURRENCY_AMOUNT: {
      pattern: /^\d+(\.\d{1,2})?$/,
      message: 'Amount must be a valid currency value with up to 2 decimal places',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.CREDIT_LIMIT,
    },
  },
  
  /**
   * Alphanumeric input patterns for mixed character fields.
   * Preserves COBOL alphanumeric validation logic.
   */
  ALPHANUMERIC_PATTERNS: {
    // User ID pattern for 8-character user identifiers
    USER_ID: {
      pattern: /^[A-Z0-9]{1,8}$/,
      message: 'User ID must be 1-8 uppercase letters or numbers',
      mask: 'AAAAAAAA',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.USER_ID,
    },
    
    // Password pattern for 8-character passwords
    PASSWORD: {
      pattern: /^.{1,8}$/,
      message: 'Password must be 1-8 characters',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.PASSWORD,
    },
    
    // Name field patterns
    FIRST_NAME: {
      pattern: /^[A-Za-z\s\-'\.]{1,25}$/,
      message: 'First name must be 1-25 characters, letters, spaces, hyphens, apostrophes, and periods only',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.FIRST_NAME,
    },
    
    MIDDLE_NAME: {
      pattern: /^[A-Za-z\s\-'\.]{0,25}$/,
      message: 'Middle name must be up to 25 characters, letters, spaces, hyphens, apostrophes, and periods only',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.MIDDLE_NAME,
    },
    
    LAST_NAME: {
      pattern: /^[A-Za-z\s\-'\.]{1,25}$/,
      message: 'Last name must be 1-25 characters, letters, spaces, hyphens, apostrophes, and periods only',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.LAST_NAME,
    },
    
    // Address field patterns
    ADDRESS_LINE: {
      pattern: /^[A-Za-z0-9\s\-'\.#]{1,50}$/,
      message: 'Address must be 1-50 characters, letters, numbers, spaces, and common punctuation only',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.ADDRESS_LINE1,
    },
    
    CITY: {
      pattern: /^[A-Za-z\s\-'\.]{1,50}$/,
      message: 'City must be 1-50 characters, letters, spaces, hyphens, apostrophes, and periods only',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.CITY,
    },
    
    STATE: {
      pattern: /^[A-Z]{2}$/,
      message: 'State must be exactly 2 uppercase letters',
      mask: 'AA',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.STATE,
    },
    
    COUNTRY: {
      pattern: /^[A-Z]{2,3}$/,
      message: 'Country must be 2-3 uppercase letters',
      mask: 'AAA',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.COUNTRY,
    },
    
    // Government ID pattern
    GOVERNMENT_ID: {
      pattern: /^[A-Z0-9\-]{1,20}$/,
      message: 'Government ID must be 1-20 uppercase letters, numbers, and hyphens only',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.GOVT_ID,
    },
    
    // Account group pattern
    ACCOUNT_GROUP: {
      pattern: /^[A-Z0-9]{1,10}$/,
      message: 'Account group must be 1-10 uppercase letters or numbers',
      maxLength: 10,
    },
    
    // EFT account pattern
    EFT_ACCOUNT: {
      pattern: /^[A-Z0-9]{1,10}$/,
      message: 'EFT account must be 1-10 uppercase letters or numbers',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.EFT_ACCOUNT,
    },
  },
  
  /**
   * Account-specific validation patterns for financial fields.
   * Maintains exact COBOL business rule validation.
   */
  ACCOUNT_PATTERNS: {
    // Account status validation (Y/N only)
    ACCOUNT_STATUS: {
      pattern: /^[YN]$/,
      message: 'Account status must be Y or N',
      mask: 'A',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.ACCOUNT_STATUS,
    },
    
    // Primary card holder flag (Y/N only)
    PRIMARY_FLAG: {
      pattern: /^[YN]$/,
      message: 'Primary flag must be Y or N',
      mask: 'A',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.PRIMARY_FLAG,
    },
    
    // Selection field pattern (S for select, space for no selection)
    SELECTION_FIELD: {
      pattern: /^[Ss ]?$/,
      message: 'Selection must be S or blank',
      mask: 'A',
      maxLength: 1,
    },
    
    // Confirmation field pattern (Y/N only)
    CONFIRMATION: {
      pattern: /^[YN]$/,
      message: 'Confirmation must be Y or N',
      mask: 'A',
      maxLength: 1,
    },
    
    // Transaction type validation
    TRANSACTION_TYPE: {
      pattern: /^[A-Z0-9]{1,2}$/,
      message: 'Transaction type must be 1-2 uppercase letters or numbers',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.TRANSACTION_TYPE,
    },
    
    // Transaction category validation
    TRANSACTION_CATEGORY: {
      pattern: /^[A-Z0-9]{1,4}$/,
      message: 'Transaction category must be 1-4 uppercase letters or numbers',
      maxLength: FIELD_LENGTHS.LENGTH_CONSTRAINTS.TRANSACTION_CATEGORY,
    },
  },
  
  /**
   * Composite validation regex patterns for complex field formats.
   * Provides unified validation for multi-part fields.
   */
  VALIDATION_REGEX: {
    // SSN full format validation (999-99-9999)
    SSN_FULL: /^\d{3}-\d{2}-\d{4}$/,
    
    // Phone number full format validation ((999) 999-9999)
    PHONE_FULL: /^\(\d{3}\) \d{3}-\d{4}$/,
    
    // Card number with spaces (9999 9999 9999 9999)
    CARD_NUMBER_FORMATTED: /^\d{4} \d{4} \d{4} \d{4}$/,
    
    // Date format validation (MM/DD/YYYY)
    DATE_FORMAT: /^(0[1-9]|1[0-2])\/(0[1-9]|[12]\d|3[01])\/\d{4}$/,
    
    // Time format validation (HH:MM:SS)
    TIME_FORMAT: /^([01]\d|2[0-3]):([0-5]\d):([0-5]\d)$/,
    
    // Email format validation (basic)
    EMAIL_FORMAT: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
    
    // Currency format validation ($999,999.99)
    CURRENCY_FORMAT: /^\$?\d{1,3}(,\d{3})*(\.\d{2})?$/,
  },
} as const;

// =============================================================================
// VALIDATION RULES - BMS VALIDN and MUSTFILL attribute mappings
// =============================================================================

/**
 * Validation rules extracted from BMS VALIDN attributes and field requirements.
 * Maps BMS validation attributes to React Hook Form validation rules.
 */
export const VALIDATION_RULES = {
  /**
   * MUSTFILL validation rules for required fields.
   * Extracted from BMS VALIDN=(MUSTFILL) attributes.
   */
  MUSTFILL: {
    // Login screen required fields
    USER_ID: {
      required: true,
      message: 'User ID is required',
    },
    PASSWORD: {
      required: true,
      message: 'Password is required',
    },
    
    // Account screen required fields
    ACCOUNT_NUMBER: {
      required: true,
      message: 'Account number is required',
    },
    
    // Customer information required fields
    FIRST_NAME: {
      required: true,
      message: 'First name is required',
    },
    LAST_NAME: {
      required: true,
      message: 'Last name is required',
    },
    
    // Address required fields
    ADDRESS_LINE1: {
      required: true,
      message: 'Address line 1 is required',
    },
    CITY: {
      required: true,
      message: 'City is required',
    },
    STATE: {
      required: true,
      message: 'State is required',
    },
    ZIP_CODE: {
      required: true,
      message: 'ZIP code is required',
    },
    
    // Date component required fields
    YEAR_FIELD: {
      required: true,
      message: 'Year is required',
    },
    MONTH_FIELD: {
      required: true,
      message: 'Month is required',
    },
    DAY_FIELD: {
      required: true,
      message: 'Day is required',
    },
  },
  
  /**
   * NUM validation rules for numeric-only fields.
   * Extracted from BMS ATTRB=(NUM) attributes.
   */
  NUM: {
    // Account and financial fields
    ACCOUNT_NUMBER: {
      type: 'number',
      message: 'Account number must contain only numbers',
      schema: number().positive().integer(),
    },
    CARD_NUMBER: {
      type: 'number',
      message: 'Card number must contain only numbers',
      schema: number().positive().integer(),
    },
    CUSTOMER_ID: {
      type: 'number',
      message: 'Customer ID must contain only numbers',
      schema: number().positive().integer(),
    },
    FICO_SCORE: {
      type: 'number',
      message: 'FICO score must be a number between 300 and 850',
      schema: number().min(300).max(850).integer(),
    },
    
    // Date component numeric validation
    YEAR_FIELD: {
      type: 'number',
      message: 'Year must be a number',
      schema: number().min(1900).max(2099).integer(),
    },
    MONTH_FIELD: {
      type: 'number',
      message: 'Month must be a number between 1 and 12',
      schema: number().min(1).max(12).integer(),
    },
    DAY_FIELD: {
      type: 'number',
      message: 'Day must be a number between 1 and 31',
      schema: number().min(1).max(31).integer(),
    },
    
    // Phone number components
    PHONE_AREA: {
      type: 'number',
      message: 'Area code must contain only numbers',
      schema: number().min(200).max(999).integer(),
    },
    PHONE_EXCHANGE: {
      type: 'number',
      message: 'Exchange must contain only numbers',
      schema: number().min(200).max(999).integer(),
    },
    PHONE_NUMBER: {
      type: 'number',
      message: 'Phone number must contain only numbers',
      schema: number().min(0).max(9999).integer(),
    },
    
    // ZIP code numeric validation
    ZIP_CODE: {
      type: 'number',
      message: 'ZIP code must contain only numbers',
      schema: number().min(0).max(99999).integer(),
    },
    
    // Currency amount validation
    CURRENCY_AMOUNT: {
      type: 'number',
      message: 'Amount must be a valid number',
      schema: number().min(0).max(999999999.99),
    },
  },
  
  /**
   * Required field definitions based on BMS field analysis.
   * Fields that must have values for successful form submission.
   */
  REQUIRED_FIELDS: [
    'USER_ID',
    'PASSWORD',
    'ACCOUNT_NUMBER',
    'FIRST_NAME',
    'LAST_NAME',
    'ADDRESS_LINE1',
    'CITY',
    'STATE',
    'ZIP_CODE',
    'YEAR_FIELD',
    'MONTH_FIELD',
    'DAY_FIELD',
  ],
  
  /**
   * Optional field definitions based on BMS field analysis.
   * Fields that can be empty without preventing form submission.
   */
  OPTIONAL_FIELDS: [
    'MIDDLE_NAME',
    'ADDRESS_LINE2',
    'PHONE_AREA',
    'PHONE_EXCHANGE',
    'PHONE_NUMBER',
    'GOVERNMENT_ID',
    'EFT_ACCOUNT',
    'ACCOUNT_GROUP',
    'TRANSACTION_ID',
  ],
} as const;

// =============================================================================
// INPUT MASKS - Format masks for structured data entry
// =============================================================================

/**
 * Input masks for structured data fields.
 * Provides consistent formatting for user input while maintaining validation.
 */
export const INPUT_MASKS = {
  /**
   * SSN input mask for Social Security Number formatting.
   * Maintains 999-99-9999 format from BMS field definitions.
   */
  SSN_MASK: '999-99-9999',
  
  /**
   * Phone number input mask for telephone formatting.
   * Maintains (999) 999-9999 format from BMS field definitions.
   */
  PHONE_MASK: '(999) 999-9999',
  
  /**
   * Date input mask for date formatting.
   * Maintains MM/DD/YYYY format from BMS field definitions.
   */
  DATE_MASK: '99/99/9999',
  
  /**
   * Card number input mask for credit card formatting.
   * Maintains 9999 9999 9999 9999 format with spaces.
   */
  CARD_NUMBER_MASK: '9999 9999 9999 9999',
  
  /**
   * Account number input mask for account formatting.
   * Maintains 99999999999 format from BMS PICIN definitions.
   */
  ACCOUNT_NUMBER_MASK: '99999999999',
  
  /**
   * Time input mask for time formatting.
   * Maintains HH:MM:SS format from BMS field definitions.
   */
  TIME_MASK: '99:99:99',
  
  /**
   * ZIP code input mask for postal code formatting.
   * Maintains 99999 format from BMS field definitions.
   */
  ZIP_CODE_MASK: '99999',
  
  /**
   * Customer ID input mask for customer identifier formatting.
   * Maintains 999999999 format from BMS field definitions.
   */
  CUSTOMER_ID_MASK: '999999999',
  
  /**
   * FICO score input mask for credit score formatting.
   * Maintains 999 format from BMS field definitions.
   */
  FICO_SCORE_MASK: '999',
  
  /**
   * Transaction ID input mask for transaction identifier formatting.
   * Maintains AAAAAAAAAAAAAAAA format from BMS field definitions.
   */
  TRANSACTION_ID_MASK: 'AAAAAAAAAAAAAAAA',
} as const;

// =============================================================================
// FIELD CONSTRAINTS - Length, range, and business rule constraints
// =============================================================================

/**
 * Field constraints extracted from BMS LENGTH definitions and business rules.
 * Provides comprehensive validation rules for all field types.
 */
export const FIELD_CONSTRAINTS = {
  /**
   * Minimum length constraints for various field types.
   * Ensures minimum data quality requirements.
   */
  MIN_LENGTHS: {
    USER_ID: 1,
    PASSWORD: 1,
    ACCOUNT_NUMBER: 11,
    CARD_NUMBER: 16,
    CUSTOMER_ID: 9,
    FIRST_NAME: 1,
    LAST_NAME: 1,
    ADDRESS_LINE1: 1,
    CITY: 1,
    STATE: 2,
    ZIP_CODE: 5,
    COUNTRY: 2,
    PHONE_AREA: 3,
    PHONE_EXCHANGE: 3,
    PHONE_NUMBER: 4,
    FICO_SCORE: 3,
    YEAR_FIELD: 4,
    MONTH_FIELD: 1,
    DAY_FIELD: 1,
  },
  
  /**
   * Maximum length constraints imported from FieldConstants.
   * Ensures compatibility with BMS field definitions.
   */
  MAX_LENGTHS: {
    USER_ID: FIELD_LENGTHS.LENGTH_CONSTRAINTS.USER_ID,
    PASSWORD: FIELD_LENGTHS.LENGTH_CONSTRAINTS.PASSWORD,
    ACCOUNT_NUMBER: FIELD_LENGTHS.LENGTH_CONSTRAINTS.ACCOUNT_NUMBER,
    CARD_NUMBER: FIELD_LENGTHS.LENGTH_CONSTRAINTS.CARD_NUMBER,
    CUSTOMER_ID: FIELD_LENGTHS.LENGTH_CONSTRAINTS.CUSTOMER_ID,
    FIRST_NAME: FIELD_LENGTHS.LENGTH_CONSTRAINTS.FIRST_NAME,
    MIDDLE_NAME: FIELD_LENGTHS.LENGTH_CONSTRAINTS.MIDDLE_NAME,
    LAST_NAME: FIELD_LENGTHS.LENGTH_CONSTRAINTS.LAST_NAME,
    ADDRESS_LINE1: FIELD_LENGTHS.LENGTH_CONSTRAINTS.ADDRESS_LINE1,
    ADDRESS_LINE2: FIELD_LENGTHS.LENGTH_CONSTRAINTS.ADDRESS_LINE2,
    CITY: FIELD_LENGTHS.LENGTH_CONSTRAINTS.CITY,
    STATE: FIELD_LENGTHS.LENGTH_CONSTRAINTS.STATE,
    ZIP_CODE: FIELD_LENGTHS.LENGTH_CONSTRAINTS.ZIP_CODE,
    COUNTRY: FIELD_LENGTHS.LENGTH_CONSTRAINTS.COUNTRY,
    PHONE_AREA: FIELD_LENGTHS.LENGTH_CONSTRAINTS.PHONE_AREA,
    PHONE_EXCHANGE: FIELD_LENGTHS.LENGTH_CONSTRAINTS.PHONE_EXCHANGE,
    PHONE_NUMBER: FIELD_LENGTHS.LENGTH_CONSTRAINTS.PHONE_NUMBER,
    PHONE_FULL: FIELD_LENGTHS.LENGTH_CONSTRAINTS.PHONE_FULL,
    FICO_SCORE: FIELD_LENGTHS.LENGTH_CONSTRAINTS.FICO_SCORE,
    YEAR_FIELD: FIELD_LENGTHS.LENGTH_CONSTRAINTS.YEAR_FIELD,
    MONTH_FIELD: FIELD_LENGTHS.LENGTH_CONSTRAINTS.MONTH_FIELD,
    DAY_FIELD: FIELD_LENGTHS.LENGTH_CONSTRAINTS.DAY_FIELD,
    TRANSACTION_ID: FIELD_LENGTHS.LENGTH_CONSTRAINTS.TRANSACTION_ID,
    GOVT_ID: FIELD_LENGTHS.LENGTH_CONSTRAINTS.GOVT_ID,
    EFT_ACCOUNT: FIELD_LENGTHS.LENGTH_CONSTRAINTS.EFT_ACCOUNT,
    ERROR_MESSAGE: FIELD_LENGTHS.LENGTH_CONSTRAINTS.ERROR_MESSAGE,
    INFO_MESSAGE: FIELD_LENGTHS.LENGTH_CONSTRAINTS.INFO_MESSAGE,
  },
  
  /**
   * Numeric range limits for various field types.
   * Implements business rule validation from COBOL logic.
   */
  RANGE_LIMITS: {
    FICO_SCORE: {
      min: 300,
      max: 850,
      message: 'FICO score must be between 300 and 850',
    },
    YEAR_FIELD: {
      min: 1900,
      max: 2099,
      message: 'Year must be between 1900 and 2099',
    },
    MONTH_FIELD: {
      min: 1,
      max: 12,
      message: 'Month must be between 1 and 12',
    },
    DAY_FIELD: {
      min: 1,
      max: 31,
      message: 'Day must be between 1 and 31',
    },
    PHONE_AREA: {
      min: 200,
      max: 999,
      message: 'Area code must be between 200 and 999',
    },
    PHONE_EXCHANGE: {
      min: 200,
      max: 999,
      message: 'Exchange must be between 200 and 999',
    },
    PHONE_NUMBER: {
      min: 0,
      max: 9999,
      message: 'Phone number must be between 0000 and 9999',
    },
    ZIP_CODE: {
      min: 0,
      max: 99999,
      message: 'ZIP code must be between 00000 and 99999',
    },
    CREDIT_LIMIT: {
      min: 0,
      max: 999999999.99,
      message: 'Credit limit must be between $0.00 and $999,999,999.99',
    },
    CURRENT_BALANCE: {
      min: -999999999.99,
      max: 999999999.99,
      message: 'Current balance must be between -$999,999,999.99 and $999,999,999.99',
    },
  },
  
  /**
   * Business rule constraints extracted from COBOL application logic.
   * Implements complex validation rules beyond simple format checking.
   */
  BUSINESS_RULES: {
    // Account status validation
    ACCOUNT_STATUS_VALUES: ['Y', 'N'],
    
    // Primary flag validation
    PRIMARY_FLAG_VALUES: ['Y', 'N'],
    
    // Confirmation field validation
    CONFIRMATION_VALUES: ['Y', 'N'],
    
    // Selection field validation
    SELECTION_VALUES: ['S', 's', ' ', ''],
    
    // US state codes validation
    US_STATE_CODES: [
      'AL', 'AK', 'AZ', 'AR', 'CA', 'CO', 'CT', 'DE', 'FL', 'GA',
      'HI', 'ID', 'IL', 'IN', 'IA', 'KS', 'KY', 'LA', 'ME', 'MD',
      'MA', 'MI', 'MN', 'MS', 'MO', 'MT', 'NE', 'NV', 'NH', 'NJ',
      'NM', 'NY', 'NC', 'ND', 'OH', 'OK', 'OR', 'PA', 'RI', 'SC',
      'SD', 'TN', 'TX', 'UT', 'VT', 'VA', 'WA', 'WV', 'WI', 'WY',
    ],
    
    // Common country codes
    COUNTRY_CODES: ['US', 'CA', 'MX', 'GB', 'FR', 'DE', 'IT', 'ES', 'JP', 'AU'],
    
    // Date validation rules
    DATE_RULES: {
      // Leap year validation
      isLeapYear: (year: number) => 
        (year % 4 === 0 && year % 100 !== 0) || (year % 400 === 0),
      
      // Days in month validation
      getDaysInMonth: (month: number, year: number) => {
        const daysInMonth = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
        if (month === 2 && ((year % 4 === 0 && year % 100 !== 0) || (year % 400 === 0))) {
          return 29;
        }
        return daysInMonth[month - 1];
      },
      
      // Validate complete date
      validateDate: (year: number, month: number, day: number) => {
        if (month < 1 || month > 12) return false;
        const maxDays = FIELD_CONSTRAINTS.BUSINESS_RULES.DATE_RULES.getDaysInMonth(month, year);
        return day >= 1 && day <= maxDays;
      },
    },
    
    // Phone number validation rules
    PHONE_RULES: {
      // Invalid area codes
      INVALID_AREA_CODES: ['000', '001', '002', '003', '004', '005', '006', '007', '008', '009'],
      
      // Invalid exchanges
      INVALID_EXCHANGES: ['000', '001', '002', '003', '004', '005', '006', '007', '008', '009'],
    },
    
    // Card number validation rules
    CARD_RULES: {
      // Luhn algorithm validation
      luhnCheck: (cardNumber: string) => {
        const digits = cardNumber.replace(/\s+/g, '').split('').map(Number);
        let sum = 0;
        let isEven = false;
        
        for (let i = digits.length - 1; i >= 0; i--) {
          let digit = digits[i];
          
          if (isEven) {
            digit *= 2;
            if (digit > 9) {
              digit -= 9;
            }
          }
          
          sum += digit;
          isEven = !isEven;
        }
        
        return sum % 10 === 0;
      },
    },
    
    // SSN validation rules
    SSN_RULES: {
      // Invalid SSN patterns
      INVALID_SSN_PATTERNS: [
        '000000000',
        '123456789',
        '111111111',
        '222222222',
        '333333333',
        '444444444',
        '555555555',
        '666666666',
        '777777777',
        '888888888',
        '999999999',
      ],
      
      // Invalid area numbers
      INVALID_AREA_NUMBERS: ['000', '666', '900', '901', '902', '903', '904', '905', '906', '907', '908', '909'],
    },
  },
} as const;

// =============================================================================
// VALIDATION MESSAGE CONSTANTS
// =============================================================================

/**
 * Centralized validation error messages maintaining consistency with original BMS error messages.
 * Preserves user experience from original COBOL application.
 */
export const VALIDATION_MESSAGES = {
  REQUIRED: 'This field is required',
  INVALID_FORMAT: 'Invalid format',
  INVALID_LENGTH: 'Invalid length',
  INVALID_RANGE: 'Value is out of range',
  INVALID_CHARACTERS: 'Invalid characters',
  NUMERIC_ONLY: 'Only numeric characters are allowed',
  ALPHANUMERIC_ONLY: 'Only letters and numbers are allowed',
  INVALID_DATE: 'Invalid date',
  INVALID_PHONE: 'Invalid phone number',
  INVALID_EMAIL: 'Invalid email address',
  INVALID_SSN: 'Invalid Social Security Number',
  INVALID_CARD_NUMBER: 'Invalid card number',
  INVALID_ACCOUNT_NUMBER: 'Invalid account number',
  INVALID_FICO_SCORE: 'Invalid FICO score',
  INVALID_STATE: 'Invalid state code',
  INVALID_COUNTRY: 'Invalid country code',
  INVALID_ZIP_CODE: 'Invalid ZIP code',
  PASSWORDS_DONT_MATCH: 'Passwords do not match',
  FIELD_TOO_SHORT: 'Field is too short',
  FIELD_TOO_LONG: 'Field is too long',
  FUTURE_DATE_NOT_ALLOWED: 'Future date is not allowed',
  PAST_DATE_NOT_ALLOWED: 'Past date is not allowed',
  INVALID_SELECTION: 'Invalid selection',
} as const;

/**
 * Default export combining all validation constants for easy import.
 */
export default {
  PICIN_PATTERNS,
  VALIDATION_RULES,
  INPUT_MASKS,
  FIELD_CONSTRAINTS,
  VALIDATION_MESSAGES,
} as const;