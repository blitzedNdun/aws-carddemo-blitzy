/**
 * MessageConstants.ts
 * 
 * TypeScript constants file containing all user-facing text content extracted from BMS definitions.
 * Preserves exact original messaging while providing centralized text management for React 
 * components and future internationalization support.
 * 
 * All text constants maintain character-for-character compatibility with original COBOL/BMS definitions
 * to ensure consistent user experience during the mainframe modernization process.
 */

// ==============================================================================
// FUNCTION KEY HELP TEXT
// Extracted from BMS function key instruction lines
// ==============================================================================

export const FUNCTION_KEY_HELP = {
  F3_EXIT_TEXT: 'F3=Exit',
  F7_PAGEUP_TEXT: 'F7=Backward',
  F8_PAGEDOWN_TEXT: 'F8=Forward',
  F12_CANCEL_TEXT: 'F12=Cancel',
  ENTER_SUBMIT_TEXT: 'ENTER=Continue'
} as const;

// ==============================================================================
// KEYBOARD INSTRUCTIONS
// Comprehensive keyboard navigation help for accessibility and user guidance
// ==============================================================================

export const KEYBOARD_INSTRUCTIONS = {
  BASIC_NAVIGATION: 'Use Tab to move between fields, Enter to submit, and function keys for actions',
  FUNCTION_KEY_LIST: 'F3=Exit, F5=Save, F7=Backward, F8=Forward, F12=Cancel, ENTER=Continue',
  ALTERNATIVE_COMBINATIONS: 'Alternative key combinations available based on screen context'
} as const;

// ==============================================================================
// HELP TEXT
// Tooltips, hints, and accessibility descriptions from BMS field definitions
// ==============================================================================

export const HELP_TEXT = {
  TOOLTIP_MESSAGES: {
    // Login screen tooltips
    USER_ID_TOOLTIP: 'Enter your 8-character user ID',
    PASSWORD_TOOLTIP: 'Enter your 8-character password',
    LOGIN_INSTRUCTION: 'Type your User ID and Password, then press ENTER:',
    
    // Account management tooltips
    ACCOUNT_NUMBER_TOOLTIP: 'Enter 11-digit account number',
    CREDIT_CARD_TOOLTIP: 'Enter 16-digit credit card number',
    SSN_TOOLTIP: 'Enter Social Security Number in format 999-99-9999',
    DATE_TOOLTIP: 'Enter date in YYYY-MM-DD format',
    
    // Menu navigation tooltips
    MENU_SELECTION_TOOLTIP: 'Please select an option :',
    OPTION_SELECTION_TOOLTIP: 'Enter the number corresponding to your choice',
    
    // General form tooltips
    REQUIRED_FIELD_TOOLTIP: 'This field is required',
    FORMAT_HINT_TOOLTIP: 'Follow the format shown in parentheses'
  },
  
  ACCESSIBILITY_DESCRIPTIONS: {
    // Screen reader descriptions
    LOGIN_SCREEN_DESC: 'Credit Card Demo Application Login Screen',
    MAIN_MENU_DESC: 'Main Menu - Select from available options',
    ACCOUNT_VIEW_DESC: 'Account Details Display Screen',
    ACCOUNT_UPDATE_DESC: 'Account Information Update Form',
    CARD_LIST_DESC: 'Credit Card Listing and Selection Screen',
    TRANSACTION_LIST_DESC: 'Transaction History Display',
    USER_MANAGEMENT_DESC: 'User Administration Interface',
    
    // Field descriptions for screen readers
    FORM_FIELD_DESC: 'Form input field',
    REQUIRED_FIELD_DESC: 'Required form field',
    OPTIONAL_FIELD_DESC: 'Optional form field',
    READ_ONLY_FIELD_DESC: 'Read-only display field'
  },
  
  MOBILE_INSTRUCTIONS: {
    TOUCH_NAVIGATION: 'Tap fields to enter data, use virtual keyboard for input',
    GESTURE_HELP: 'Swipe left/right for page navigation where available',
    MOBILE_FUNCTION_KEYS: 'Function key actions available via menu buttons'
  }
} as const;

// ==============================================================================
// API ERROR MESSAGES
// Server-side error messages for API communication failures
// ==============================================================================

export const API_ERROR_MESSAGES = {
  AUTHENTICATION_FAILED: 'Authentication failed. Please check your credentials and try again.',
  AUTHORIZATION_DENIED: 'Access denied. You do not have permission to perform this action.',
  SERVER_ERROR: 'Internal server error. Please try again later or contact system administrator.',
  NETWORK_ERROR: 'Network connection error. Please check your connection and try again.',
  TIMEOUT_ERROR: 'Request timeout. The server did not respond in time. Please try again.'
} as const;

// ==============================================================================
// VALIDATION ERRORS
// Client-side field validation error messages
// ==============================================================================

export const VALIDATION_ERRORS = {
  REQUIRED_FIELD: 'This field is required and cannot be empty.',
  INVALID_FORMAT: 'The format of this field is invalid. Please check the required format.',
  OUT_OF_RANGE: 'The value entered is outside the acceptable range.',
  CROSS_FIELD_ERROR: 'The combination of values in related fields is invalid.',
  BUSINESS_RULE_VIOLATION: 'The entered data violates business rules. Please review and correct.'
} as const;

// ==============================================================================
// FIELD ERROR MESSAGES
// Specific field validation errors preserving original COBOL error text
// ==============================================================================

export const FIELD_ERROR_MESSAGES = {
  ACCOUNT_INVALID: 'Invalid account number. Please enter a valid 11-digit account number.',
  CARD_INVALID: 'Invalid credit card number. Please enter a valid 16-digit card number.',
  SSN_INVALID: 'Invalid Social Security Number. Please use format 999-99-9999.',
  DATE_INVALID: 'Invalid date format. Please enter date in YYYY-MM-DD format.',
  STATE_ZIP_MISMATCH: 'State and ZIP code combination is invalid.',
  BALANCE_INSUFFICIENT: 'Insufficient balance for this transaction.'
} as const;

// ==============================================================================
// SCREEN TITLES AND HEADERS
// Page titles and section headers from BMS map definitions
// ==============================================================================

export const SCREEN_TITLES = {
  // Main application screens
  LOGIN_TITLE: 'CardDemo Login',
  MAIN_MENU_TITLE: 'Main Menu',
  ADMIN_MENU_TITLE: 'Administration Menu',
  
  // Account management screens
  ACCOUNT_VIEW_TITLE: 'View Account',
  ACCOUNT_UPDATE_TITLE: 'Update Account',
  CUSTOMER_DETAILS_TITLE: 'Customer Details',
  
  // Card management screens
  CARD_LIST_TITLE: 'List Credit Cards',
  CARD_UPDATE_TITLE: 'Update Credit Card',
  
  // Transaction screens
  TRANSACTION_VIEW_TITLE: 'View Transactions',
  TRANSACTION_LIST_TITLE: 'Transaction History',
  ADD_TRANSACTION_TITLE: 'Add Transaction',
  
  // User management screens
  USER_LIST_TITLE: 'User List',
  USER_DETAIL_TITLE: 'User Details',
  ADD_USER_TITLE: 'Add User',
  UPDATE_USER_TITLE: 'Update User',
  
  // Reporting screens
  REPORTS_TITLE: 'Reports',
  BILL_PAYMENT_TITLE: 'Bill Payment'
} as const;

// ==============================================================================
// FIELD LABELS
// All field labels extracted from BMS INITIAL values
// ==============================================================================

export const FIELD_LABELS = {
  // Standard header fields
  TRANSACTION_LABEL: 'Tran:',
  PROGRAM_LABEL: 'Prog:',
  DATE_LABEL: 'Date:',
  TIME_LABEL: 'Time:',
  APPLICATION_ID_LABEL: 'AppID:',
  SYSTEM_ID_LABEL: 'SysID:',
  PAGE_LABEL: 'Page ',
  
  // Authentication fields
  USER_ID_LABEL: 'User ID     :',
  PASSWORD_LABEL: 'Password    :',
  
  // Account information fields
  ACCOUNT_NUMBER_LABEL: 'Account Number :',
  ACCOUNT_STATUS_LABEL: 'Active Y/N: ',
  ACCOUNT_OPENED_LABEL: 'Opened:',
  ACCOUNT_EXPIRY_LABEL: 'Expiry:',
  ACCOUNT_REISSUE_LABEL: 'Reissue:',
  ACCOUNT_GROUP_LABEL: 'Account Group:',
  
  // Financial fields
  CREDIT_LIMIT_LABEL: 'Credit Limit        :',
  CASH_CREDIT_LIMIT_LABEL: 'Cash credit Limit   :',
  CURRENT_BALANCE_LABEL: 'Current Balance     :',
  CURRENT_CYCLE_CREDIT_LABEL: 'Current Cycle Credit:',
  CURRENT_CYCLE_DEBIT_LABEL: 'Current Cycle Debit :',
  
  // Customer information fields
  CUSTOMER_ID_LABEL: 'Customer id  :',
  SSN_LABEL: 'SSN:',
  DATE_OF_BIRTH_LABEL: 'Date of birth:',
  FICO_SCORE_LABEL: 'FICO Score:',
  FIRST_NAME_LABEL: 'First Name',
  MIDDLE_NAME_LABEL: 'Middle Name: ',
  LAST_NAME_LABEL: 'Last Name : ',
  
  // Address fields
  ADDRESS_LABEL: 'Address:',
  CITY_LABEL: 'City ',
  STATE_LABEL: 'State ',
  ZIP_LABEL: 'Zip',
  COUNTRY_LABEL: 'Country',
  
  // Contact fields
  PHONE1_LABEL: 'Phone 1:',
  PHONE2_LABEL: 'Phone 2:',
  GOVERNMENT_ID_LABEL: 'Government Issued Id Ref    : ',
  EFT_ACCOUNT_LABEL: 'EFT Account Id: ',
  PRIMARY_CARD_HOLDER_LABEL: 'Primary Card Holder Y/N:',
  
  // Card fields
  CREDIT_CARD_NUMBER_LABEL: 'Credit Card Number:',
  CARD_STATUS_LABEL: 'Active ',
  
  // Transaction fields
  ENTER_ACCOUNT_LABEL: 'Enter Acct #:',
  
  // User management fields
  USER_TYPE_LABEL: 'User Type: '
} as const;

// ==============================================================================
// FORMAT HINTS
// Field format instructions displayed to users
// ==============================================================================

export const FORMAT_HINTS = {
  // Character length hints
  EIGHT_CHAR_HINT: '(8 Char)',
  
  // Date format hints
  DATE_FORMAT_HINT: 'mm/dd/yy',
  DATE_FORMAT_FULL_HINT: 'YYYY-MM-DD',
  TIME_FORMAT_HINT: 'hh:mm:ss',
  TIME_FORMAT_ALT_HINT: 'Ahh:mm:ss',
  
  // User type hints
  USER_TYPE_HINT: '(A=Admin, U=User)',
  
  // Other format hints
  OR_HINT: '(or)',
  SSN_FORMAT_HINT: '999-99-9999'
} as const;

// ==============================================================================
// TABLE HEADERS
// Column headers for data grids and lists
// ==============================================================================

export const TABLE_HEADERS = {
  // Card listing table
  SELECT_HEADER: 'Select    ',
  ACCOUNT_NUMBER_HEADER: 'Account Number',
  CARD_NUMBER_HEADER: ' Card Number ',
  ACTIVE_HEADER: 'Active ',
  
  // Table separators
  SELECT_SEPARATOR: '------',
  ACCOUNT_SEPARATOR: '---------------',
  CARD_SEPARATOR: '---------------',
  ACTIVE_SEPARATOR: '--------'
} as const;

// ==============================================================================
// INSTRUCTION MESSAGES
// User instruction and guidance messages
// ==============================================================================

export const INSTRUCTION_MESSAGES = {
  // Login instructions
  LOGIN_INSTRUCTION: 'Type your User ID and Password, then press ENTER:',
  
  // Menu instructions
  MENU_SELECTION_INSTRUCTION: 'Please select an option :',
  
  // General instructions
  APPLICATION_DESCRIPTION: 'This is a Credit Card Demo Application for Mainframe Modernization'
} as const;

// ==============================================================================
// SUCCESS MESSAGES
// Operation confirmation messages
// ==============================================================================

export const SUCCESS_MESSAGES = {
  LOGIN_SUCCESS: 'Login successful',
  ACCOUNT_UPDATED: 'Account information has been updated successfully',
  CARD_UPDATED: 'Credit card information has been updated successfully',
  TRANSACTION_ADDED: 'Transaction has been added successfully',
  USER_ADDED: 'User has been added successfully',
  USER_UPDATED: 'User information has been updated successfully',
  DATA_SAVED: 'Data has been saved successfully',
  OPERATION_COMPLETED: 'Operation completed successfully'
} as const;

// ==============================================================================
// FUNCTION KEY COMBINATIONS
// Complete function key instruction combinations from BMS screens
// ==============================================================================

export const FUNCTION_KEY_COMBINATIONS = {
  // Login screen
  LOGIN_KEYS: 'ENTER=Sign-on  F3=Exit',
  
  // Main menu
  MENU_KEYS: 'ENTER=Continue  F3=Exit',
  
  // Account screens
  ACCOUNT_VIEW_KEYS: '  F3=Exit ',
  ACCOUNT_UPDATE_KEYS: 'ENTER=Process F3=Exit',
  ACCOUNT_UPDATE_FULL_KEYS: 'ENTER=Process F3=Exit F5=Save F12=Cancel',
  
  // Card listing
  CARD_LIST_KEYS: '  F3=Exit F7=Backward  F8=Forward',
  
  // General combinations
  CONTINUE_EXIT_KEYS: 'ENTER=Continue  F3=Exit',
  PROCESS_EXIT_KEYS: 'ENTER=Process F3=Exit',
  SAVE_CANCEL_KEYS: 'F5=Save F12=Cancel'
} as const;

// ==============================================================================
// NATIONAL RESERVE NOTE DISPLAY
// ASCII art and special display elements from login screen
// ==============================================================================

export const SPECIAL_DISPLAYS = {
  NATIONAL_RESERVE_NOTE: {
    LINE1: '+========================================+',
    LINE2: '|%%%%%%%  NATIONAL RESERVE NOTE  %%%%%%%%|',
    LINE3: '|%(1)  THE UNITED STATES OF KICSLAND (1)%|',
    LINE4: '|%$$              ___       ********  $$%|',
    LINE5: '|%$    {x}       (o o)                 $%|',
    LINE6: '|%$     ******  (  V  )      O N E     $%|',
    LINE7: '|%(1)          ---m-m---             (1)%|',
    LINE8: '|%%~~~~~~~~~~~ ONE DOLLAR ~~~~~~~~~~~~~%%|',
    LINE9: '+========================================+'
  }
} as const;

// ==============================================================================
// DEFAULT VALUES AND PLACEHOLDERS
// Default values and placeholder text from BMS definitions
// ==============================================================================

export const DEFAULT_VALUES = {
  // Password placeholder
  PASSWORD_PLACEHOLDER: '________',
  
  // Empty space placeholders
  EMPTY_SPACE: ' ',
  EMPTY_FIELD: '        ',
  
  // SSN format examples
  SSN_EXAMPLE_1: '999',
  SSN_EXAMPLE_2: '99',
  SSN_EXAMPLE_3: '9999'
} as const;

// ==============================================================================
// TYPE DEFINITIONS
// TypeScript type definitions for message constants
// ==============================================================================

export type FunctionKeyHelpType = typeof FUNCTION_KEY_HELP;
export type KeyboardInstructionsType = typeof KEYBOARD_INSTRUCTIONS;
export type HelpTextType = typeof HELP_TEXT;
export type ApiErrorMessagesType = typeof API_ERROR_MESSAGES;
export type ValidationErrorsType = typeof VALIDATION_ERRORS;
export type FieldErrorMessagesType = typeof FIELD_ERROR_MESSAGES;

// Export all constants as a single object for easy importing
export const MessageConstants = {
  FUNCTION_KEY_HELP,
  KEYBOARD_INSTRUCTIONS,
  HELP_TEXT,
  API_ERROR_MESSAGES,
  VALIDATION_ERRORS,
  FIELD_ERROR_MESSAGES,
  SCREEN_TITLES,
  FIELD_LABELS,
  FORMAT_HINTS,
  TABLE_HEADERS,
  INSTRUCTION_MESSAGES,
  SUCCESS_MESSAGES,
  FUNCTION_KEY_COMBINATIONS,
  SPECIAL_DISPLAYS,
  DEFAULT_VALUES
} as const;

export default MessageConstants;