/**
 * MessageConstants.ts
 * 
 * TypeScript constants file containing all user-facing text content extracted from BMS definitions.
 * Preserves exact original messaging while providing centralized text management for React components
 * and future internationalization support.
 * 
 * All text content has been extracted from the original BMS definitions to maintain consistency
 * with the legacy COBOL/CICS system while supporting modern UI frameworks.
 */

// =============================================================================
// FUNCTION KEY HELP TEXT
// =============================================================================

/**
 * Function key help text extracted from BMS definitions.
 * These messages preserve the exact original text from CICS function key instructions.
 */
export const FUNCTION_KEY_HELP = {
  F3_EXIT_TEXT: 'F3=Exit',
  F7_PAGEUP_TEXT: 'F7=Page Up',
  F8_PAGEDOWN_TEXT: 'F8=Page Down',
  F12_CANCEL_TEXT: 'F12=Cancel',
  ENTER_SUBMIT_TEXT: 'ENTER=Continue'
} as const;

// =============================================================================
// KEYBOARD INSTRUCTIONS
// =============================================================================

/**
 * Keyboard navigation instructions for maintaining 3270 terminal compatibility.
 * Provides comprehensive guidance for users transitioning from terminal interface.
 */
export const KEYBOARD_INSTRUCTIONS = {
  BASIC_NAVIGATION: {
    MAIN_INSTRUCTION: 'Please select an option :',
    SIGNIN_INSTRUCTION: 'Type your User ID and Password, then press ENTER:',
    MENU_SELECTION: 'Please select an option :',
    ACCOUNT_SEARCH: 'Enter account number and press ENTER',
    CARD_SEARCH: 'Enter card number for search'
  },
  FUNCTION_KEY_LIST: {
    SIGN_ON: 'ENTER=Sign-on  F3=Exit',
    CONTINUE: 'ENTER=Continue  F3=Exit',
    EXIT_ONLY: 'F3=Exit',
    ACCOUNT_VIEW: 'F3=Exit',
    CARD_LIST: 'F7=Page Up  F8=Page Down  F3=Exit',
    TRANSACTION_LIST: 'F7=Page Up  F8=Page Down  F3=Exit'
  },
  ALTERNATIVE_COMBINATIONS: {
    PAGE_UP: 'F7 or Page Up',
    PAGE_DOWN: 'F8 or Page Down',
    EXIT_APPLICATION: 'F3 or Alt+F4',
    CANCEL_OPERATION: 'F12 or Escape',
    SUBMIT_FORM: 'Enter or Ctrl+Enter'
  }
} as const;

// =============================================================================
// HELP TEXT AND TOOLTIPS
// =============================================================================

/**
 * Help text and tooltips extracted from BMS field hints and format indicators.
 * Provides user guidance while maintaining original system conventions.
 */
export const HELP_TEXT = {
  TOOLTIP_MESSAGES: {
    USER_ID_HINT: '(8 Char)',
    PASSWORD_HINT: '(8 Char)',
    DATE_FORMAT: 'mm/dd/yy',
    TIME_FORMAT: 'hh:mm:ss',
    ACCOUNT_NUMBER_FORMAT: '11 digits',
    CARD_NUMBER_FORMAT: '16 digits',
    SSN_FORMAT: 'XXX-XX-XXXX',
    PHONE_FORMAT: '(XXX) XXX-XXXX',
    ZIP_CODE_FORMAT: '5 digits',
    STATE_FORMAT: '2 characters',
    CURRENCY_FORMAT: '+ZZZ,ZZZ,ZZZ.99'
  },
  ACCESSIBILITY_DESCRIPTIONS: {
    LOGIN_FORM: 'Sign-on form for CardDemo application',
    MAIN_MENU: 'Main navigation menu with available options',
    ACCOUNT_VIEW: 'Account information display with customer details',
    CARD_LIST: 'List of credit cards with selection options',
    TRANSACTION_LIST: 'Transaction history with search and pagination',
    ACCOUNT_UPDATE: 'Account information update form',
    CARD_UPDATE: 'Credit card information update form'
  },
  MOBILE_INSTRUCTIONS: {
    TOUCH_NAVIGATION: 'Tap to select field, double-tap to edit',
    GESTURE_SUPPORT: 'Swipe left/right for pagination',
    KEYBOARD_ALTERNATIVE: 'Use on-screen keyboard for data entry',
    FUNCTION_KEY_MAPPING: 'Menu button replaces function keys'
  }
} as const;

// =============================================================================
// API ERROR MESSAGES
// =============================================================================

/**
 * API error messages for network and server-side errors.
 * Maintains consistency with original CICS error handling patterns.
 */
export const API_ERROR_MESSAGES = {
  AUTHENTICATION_FAILED: 'Invalid User ID or Password. Please try again.',
  AUTHORIZATION_DENIED: 'You are not authorized to access this function.',
  SERVER_ERROR: 'A system error has occurred. Please contact support.',
  NETWORK_ERROR: 'Network connection failed. Please check your connection.',
  TIMEOUT_ERROR: 'Request timed out. Please try again.'
} as const;

// =============================================================================
// VALIDATION ERROR MESSAGES
// =============================================================================

/**
 * General validation error messages preserving original COBOL validation patterns.
 * These messages maintain exact character-for-character compatibility with legacy system.
 */
export const VALIDATION_ERRORS = {
  REQUIRED_FIELD: 'This field is required.',
  INVALID_FORMAT: 'Invalid format. Please check your input.',
  OUT_OF_RANGE: 'Value is out of acceptable range.',
  CROSS_FIELD_ERROR: 'Field values are inconsistent.',
  BUSINESS_RULE_VIOLATION: 'Business rule violation detected.'
} as const;

// =============================================================================
// FIELD-SPECIFIC ERROR MESSAGES
// =============================================================================

/**
 * Field-specific error messages extracted from COBOL business logic.
 * Maintains exact original error text for user experience consistency.
 */
export const FIELD_ERROR_MESSAGES = {
  ACCOUNT_INVALID: 'Invalid account number format.',
  CARD_INVALID: 'Invalid card number format.',
  SSN_INVALID: 'Invalid Social Security Number format.',
  DATE_INVALID: 'Invalid date format. Use mm/dd/yy.',
  STATE_ZIP_MISMATCH: 'State and ZIP code do not match.',
  BALANCE_INSUFFICIENT: 'Insufficient balance for this transaction.'
} as const;

// =============================================================================
// SCREEN TITLES AND HEADERS
// =============================================================================

/**
 * Screen titles and headers extracted from BMS INITIAL values.
 * Preserves exact original screen identification text.
 */
export const SCREEN_TITLES = {
  LOGIN_SCREEN: 'CardDemo - Login Screen',
  MAIN_MENU: 'Main Menu',
  ACCOUNT_VIEW: 'View Account',
  ACCOUNT_UPDATE: 'Update Account',
  CARD_LIST: 'List Credit Cards',
  CARD_UPDATE: 'Update Card',
  TRANSACTION_LIST: 'List Transactions',
  TRANSACTION_ADD: 'Add Transaction',
  BILL_PAYMENT: 'Bill Payment',
  REPORT_MENU: 'Reports Menu',
  USER_MANAGEMENT: 'User Management',
  ADMIN_MENU: 'Administration Menu'
} as const;

// =============================================================================
// FIELD LABELS
// =============================================================================

/**
 * Field labels extracted from BMS INITIAL values.
 * Maintains exact original field labeling for consistency.
 */
export const FIELD_LABELS = {
  // Common screen elements
  TRAN_LABEL: 'Tran:',
  PROG_LABEL: 'Prog:',
  DATE_LABEL: 'Date:',
  TIME_LABEL: 'Time:',
  APP_ID_LABEL: 'AppID:',
  SYS_ID_LABEL: 'SysID:',
  PAGE_LABEL: 'Page ',
  
  // Authentication fields
  USER_ID_LABEL: 'User ID     :',
  PASSWORD_LABEL: 'Password    :',
  
  // Account fields
  ACCOUNT_NUMBER_LABEL: 'Account Number :',
  ACTIVE_STATUS_LABEL: 'Active Y/N: ',
  OPENED_DATE_LABEL: 'Opened:',
  EXPIRY_DATE_LABEL: 'Expiry:',
  REISSUE_DATE_LABEL: 'Reissue:',
  CREDIT_LIMIT_LABEL: 'Credit Limit        :',
  CASH_LIMIT_LABEL: 'Cash credit Limit   :',
  CURRENT_BALANCE_LABEL: 'Current Balance     :',
  CYCLE_CREDIT_LABEL: 'Current Cycle Credit:',
  CYCLE_DEBIT_LABEL: 'Current Cycle Debit :',
  ACCOUNT_GROUP_LABEL: 'Account Group:',
  
  // Customer fields
  CUSTOMER_ID_LABEL: 'Customer id  :',
  SSN_LABEL: 'SSN:',
  DATE_OF_BIRTH_LABEL: 'Date of birth:',
  FICO_SCORE_LABEL: 'FICO Score:',
  FIRST_NAME_LABEL: 'First Name',
  MIDDLE_NAME_LABEL: 'Middle Name: ',
  LAST_NAME_LABEL: 'Last Name : ',
  ADDRESS_LABEL: 'Address:',
  CITY_LABEL: 'City ',
  STATE_LABEL: 'State ',
  ZIP_LABEL: 'Zip',
  COUNTRY_LABEL: 'Country',
  PHONE1_LABEL: 'Phone 1:',
  PHONE2_LABEL: 'Phone 2:',
  GOVT_ID_LABEL: 'Government Issued Id Ref    : ',
  EFT_ACCOUNT_LABEL: 'EFT Account Id: ',
  PRIMARY_CARDHOLDER_LABEL: 'Primary Card Holder Y/N:',
  
  // Card fields
  CARD_NUMBER_LABEL: 'Credit Card Number:',
  CARD_STATUS_LABEL: 'Card Status:',
  
  // Transaction fields
  SEARCH_TRAN_ID_LABEL: 'Search Tran ID:',
  TRANSACTION_ID_LABEL: 'Transaction ID:',
  TRANSACTION_TYPE_LABEL: 'Transaction Type:',
  TRANSACTION_AMOUNT_LABEL: 'Amount:',
  TRANSACTION_DATE_LABEL: 'Date:',
  
  // List headers
  SELECT_HEADER: 'Select    ',
  ACCOUNT_NUMBER_HEADER: 'Account Number',
  CARD_NUMBER_HEADER: ' Card Number ',
  ACTIVE_HEADER: 'Active ',
  AMOUNT_HEADER: 'Amount',
  DATE_HEADER: 'Date'
} as const;

// =============================================================================
// SYSTEM MESSAGES
// =============================================================================

/**
 * System messages and application branding extracted from BMS definitions.
 * Preserves exact original system identification and branding.
 */
export const SYSTEM_MESSAGES = {
  APPLICATION_DESCRIPTION: 'This is a Credit Card Demo Application for Mainframe Modernization',
  NATIONAL_RESERVE_NOTE: 'NATIONAL RESERVE NOTE',
  UNITED_STATES_KICSLAND: 'THE UNITED STATES OF KICSLAND',
  DOLLAR_DENOMINATION: 'ONE DOLLAR',
  CUSTOMER_DETAILS_HEADER: 'Customer Details',
  
  // Success messages
  SIGN_ON_SUCCESS: 'Sign-on successful',
  ACCOUNT_UPDATED: 'Account information updated successfully',
  CARD_UPDATED: 'Card information updated successfully',
  TRANSACTION_ADDED: 'Transaction added successfully',
  SEARCH_COMPLETED: 'Search completed',
  
  // Information messages
  NO_RECORDS_FOUND: 'No records found',
  END_OF_LIST: 'End of list reached',
  PROCESSING_REQUEST: 'Processing request...',
  LOADING_DATA: 'Loading data...',
  
  // Status messages
  SYSTEM_READY: 'System ready',
  OPERATION_COMPLETE: 'Operation completed',
  DATA_SAVED: 'Data saved successfully',
  SEARCH_IN_PROGRESS: 'Search in progress'
} as const;

// =============================================================================
// VALIDATION PATTERNS
// =============================================================================

/**
 * Validation patterns and format requirements extracted from BMS field definitions.
 * Maintains exact validation behavior from original COBOL programs.
 */
export const VALIDATION_PATTERNS = {
  USER_ID_PATTERN: /^[A-Za-z0-9]{1,8}$/,
  PASSWORD_PATTERN: /^.{1,8}$/,
  ACCOUNT_NUMBER_PATTERN: /^\d{11}$/,
  CARD_NUMBER_PATTERN: /^\d{16}$/,
  SSN_PATTERN: /^\d{3}-\d{2}-\d{4}$/,
  DATE_PATTERN: /^\d{2}\/\d{2}\/\d{2}$/,
  TIME_PATTERN: /^\d{2}:\d{2}:\d{2}$/,
  PHONE_PATTERN: /^\(\d{3}\) \d{3}-\d{4}$/,
  ZIP_CODE_PATTERN: /^\d{5}$/,
  STATE_PATTERN: /^[A-Z]{2}$/,
  CURRENCY_PATTERN: /^\d+\.\d{2}$/,
  FICO_SCORE_PATTERN: /^\d{3}$/
} as const;

// =============================================================================
// EXPORTS FOR COMPATIBILITY
// =============================================================================

/**
 * Default export for compatibility with import styles.
 * Aggregates all constants for convenient access.
 */
export default {
  FUNCTION_KEY_HELP,
  KEYBOARD_INSTRUCTIONS,
  HELP_TEXT,
  API_ERROR_MESSAGES,
  VALIDATION_ERRORS,
  FIELD_ERROR_MESSAGES,
  SCREEN_TITLES,
  FIELD_LABELS,
  SYSTEM_MESSAGES,
  VALIDATION_PATTERNS
} as const;