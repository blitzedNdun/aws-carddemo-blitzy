/**
 * MessageConstants.ts
 * 
 * TypeScript constants file containing all user-facing text content extracted from BMS definitions
 * including field labels, error messages, help text, and system messages. Preserves exact original
 * messaging while providing centralized text management for React components and future
 * internationalization support.
 * 
 * This file maintains character-for-character compatibility with the original COBOL/BMS messaging
 * to ensure consistent user experience during the mainframe modernization process.
 * 
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0
 */

// ============================================================================
// COMMON SCREEN LABELS AND HEADERS
// ============================================================================

/**
 * Common screen header labels used across all BMS screens
 */
export const SCREEN_HEADERS = {
  TRANSACTION_LABEL: 'Tran:',
  PROGRAM_LABEL: 'Prog:',
  DATE_LABEL: 'Date:',
  TIME_LABEL: 'Time:',
  APPLICATION_ID_LABEL: 'AppID:',
  SYSTEM_ID_LABEL: 'SysID:',
  PAGE_LABEL: 'Page ',
  PAGE_COLON: 'Page:',
  
  // Date/Time formats
  DATE_FORMAT_DISPLAY: 'mm/dd/yy',
  TIME_FORMAT_DISPLAY: 'hh:mm:ss',
  TIME_FORMAT_ALT: 'Ahh:mm:ss',
} as const;

/**
 * Main screen titles and application branding
 */
export const SCREEN_TITLES = {
  MAIN_MENU: 'Main Menu',
  LOGIN_SCREEN: 'CardDemo - Login Screen',
  CREDIT_CARD_DEMO: 'This is a Credit Card Demo Application for Mainframe Modernization',
  VIEW_ACCOUNT: 'View Account',
  UPDATE_ACCOUNT: 'Update Account',
  LIST_CREDIT_CARDS: 'List Credit Cards',
  LIST_TRANSACTIONS: 'List Transactions',
  LIST_USERS: 'List Users',
  BILL_PAYMENT: 'Bill Payment',
  CUSTOMER_DETAILS: 'Customer Details',
} as const;

// ============================================================================
// FUNCTION KEY HELP TEXT
// ============================================================================

/**
 * Function key help text and descriptions
 * Maintains exact original BMS function key messaging
 */
export const FUNCTION_KEY_HELP = {
  F3_EXIT_TEXT: 'F3=Exit',
  F7_PAGEUP_TEXT: 'F7=Backward',
  F8_PAGEDOWN_TEXT: 'F8=Forward',
  F12_CANCEL_TEXT: 'F12=Cancel',
  ENTER_SUBMIT_TEXT: 'ENTER=Continue',
  
  // Additional function keys from BMS screens
  F3_BACK_TEXT: 'F3=Back',
  F4_CLEAR_TEXT: 'F4=Clear',
  F5_SAVE_TEXT: 'F5=Save',
  ENTER_SIGNIN_TEXT: 'ENTER=Sign-on',
  ENTER_PROCESS_TEXT: 'ENTER=Process',
  
  // Combined function key lines
  BASIC_NAVIGATION: 'ENTER=Continue  F3=Exit',
  SIGNIN_NAVIGATION: 'ENTER=Sign-on  F3=Exit',
  PROCESS_NAVIGATION: 'ENTER=Process F3=Exit',
  EXTENDED_NAVIGATION: 'ENTER=Continue  F3=Back  F7=Backward  F8=Forward',
  BILL_PAYMENT_NAVIGATION: 'ENTER=Continue  F3=Back  F4=Clear',
  ACCOUNT_UPDATE_NAVIGATION: 'ENTER=Process F3=Exit F5=Save F12=Cancel',
  CARD_LIST_NAVIGATION: 'F3=Exit F7=Backward  F8=Forward',
} as const;

// ============================================================================
// KEYBOARD INSTRUCTIONS
// ============================================================================

/**
 * Keyboard navigation instructions and accessibility information
 */
export const KEYBOARD_INSTRUCTIONS = {
  BASIC_NAVIGATION: 'Use Tab to navigate between fields. Press Enter to submit.',
  FUNCTION_KEY_LIST: 'Function Keys: F3=Exit, F7=Previous Page, F8=Next Page, F12=Cancel',
  ALTERNATIVE_COMBINATIONS: 'Alternative: Ctrl+3=Exit, Ctrl+7=Previous, Ctrl+8=Next, Ctrl+12=Cancel',
  
  // Screen-specific navigation instructions
  MENU_SELECTION: 'Please select an option :',
  TRANSACTION_SELECTION: 'Type \'S\' to View Transaction details from the list',
  USER_MANAGEMENT: 'Type \'U\' to Update or \'D\' to Delete a User from the list',
  LOGIN_INSTRUCTIONS: 'Type your User ID and Password, then press ENTER:',
  BILL_PAYMENT_CONFIRMATION: 'Do you want to pay your balance now. Please confirm: ',
} as const;

// ============================================================================
// HELP TEXT AND TOOLTIPS
// ============================================================================

/**
 * Help text, tooltips, and accessibility descriptions
 */
export const HELP_TEXT = {
  TOOLTIP_MESSAGES: {
    USER_ID_FIELD: 'Enter your 8-character User ID',
    PASSWORD_FIELD: 'Enter your 8-character Password',
    ACCOUNT_NUMBER: 'Enter 11-digit Account Number',
    CARD_NUMBER: 'Enter 16-digit Credit Card Number',
    CONFIRMATION_FIELD: 'Enter Y for Yes or N for No',
    SEARCH_TRANSACTION: 'Enter Transaction ID to search',
    SEARCH_USER: 'Enter User ID to search',
    BILL_PAYMENT_ACCOUNT: 'Enter Account ID for bill payment',
  },
  
  ACCESSIBILITY_DESCRIPTIONS: {
    REQUIRED_FIELD: 'This field is required',
    PROTECTED_FIELD: 'This field is read-only',
    NUMERIC_FIELD: 'This field accepts numbers only',
    CURRENCY_FIELD: 'This field displays currency amounts',
    DATE_FIELD: 'This field displays dates in MM/DD/YY format',
    SELECTION_FIELD: 'Use this field to select an item from the list',
  },
  
  MOBILE_INSTRUCTIONS: {
    TOUCH_NAVIGATION: 'Tap fields to edit. Use virtual keyboard.',
    SWIPE_GESTURE: 'Swipe left/right for page navigation where applicable',
    BUTTON_ALTERNATIVES: 'Function key actions available as buttons',
  },
  
  // Field format hints from BMS definitions
  FIELD_FORMAT_HINTS: {
    EIGHT_CHAR: '(8 Char)',
    DATE_FORMAT: '(YYYY-MM-DD)',
    YES_NO: '(Y/N)',
    PHONE_FORMAT: '(###) ###-####',
    SSN_FORMAT: '###-##-####',
    CURRENCY_FORMAT: '$#,###.##',
  },
} as const;

// ============================================================================
// API ERROR MESSAGES
// ============================================================================

/**
 * API and system error messages
 */
export const API_ERROR_MESSAGES = {
  AUTHENTICATION_FAILED: 'Authentication failed. Please check your credentials and try again.',
  AUTHORIZATION_DENIED: 'You are not authorized to access this resource.',
  SERVER_ERROR: 'A server error occurred. Please try again later.',
  NETWORK_ERROR: 'Network connection error. Please check your connection and try again.',
  TIMEOUT_ERROR: 'Request timed out. Please try again.',
  
  // Additional system errors
  SESSION_EXPIRED: 'Your session has expired. Please log in again.',
  RESOURCE_NOT_FOUND: 'The requested resource was not found.',
  INVALID_REQUEST: 'Invalid request format. Please check your input.',
  SERVICE_UNAVAILABLE: 'Service temporarily unavailable. Please try again later.',
  RATE_LIMIT_EXCEEDED: 'Too many requests. Please wait and try again.',
} as const;

// ============================================================================
// VALIDATION ERROR MESSAGES
// ============================================================================

/**
 * Form validation error messages
 */
export const VALIDATION_ERRORS = {
  REQUIRED_FIELD: 'This field is required.',
  INVALID_FORMAT: 'Invalid format. Please check your input.',
  OUT_OF_RANGE: 'Value is out of acceptable range.',
  CROSS_FIELD_ERROR: 'Field values are inconsistent with each other.',
  BUSINESS_RULE_VIOLATION: 'Input violates business rules.',
  
  // Length validation errors
  TOO_SHORT: 'Input is too short.',
  TOO_LONG: 'Input is too long.',
  EXACT_LENGTH_REQUIRED: 'Exact length required.',
  
  // Type validation errors
  NUMERIC_ONLY: 'This field accepts numbers only.',
  ALPHA_ONLY: 'This field accepts letters only.',
  ALPHANUMERIC_ONLY: 'This field accepts letters and numbers only.',
  
  // Pattern validation errors
  INVALID_EMAIL: 'Invalid email format.',
  INVALID_PHONE: 'Invalid phone number format.',
  INVALID_DATE: 'Invalid date format.',
  INVALID_CURRENCY: 'Invalid currency format.',
} as const;

// ============================================================================
// FIELD-SPECIFIC ERROR MESSAGES
// ============================================================================

/**
 * Field-specific error messages preserving original COBOL error text
 */
export const FIELD_ERROR_MESSAGES = {
  ACCOUNT_INVALID: 'Invalid Account Number. Please enter a valid 11-digit account number.',
  CARD_INVALID: 'Invalid Credit Card Number. Please enter a valid 16-digit card number.',
  SSN_INVALID: 'Invalid Social Security Number. Please enter in format ###-##-####.',
  DATE_INVALID: 'Invalid Date. Please enter date in MM/DD/YY format.',
  STATE_ZIP_MISMATCH: 'State and ZIP code do not match.',
  BALANCE_INSUFFICIENT: 'Insufficient balance for this transaction.',
  
  // User management errors
  USER_ID_INVALID: 'Invalid User ID. Must be 8 characters.',
  PASSWORD_INVALID: 'Invalid Password. Must be 8 characters.',
  USER_NOT_FOUND: 'User not found in the system.',
  DUPLICATE_USER: 'User ID already exists.',
  
  // Transaction errors
  TRANSACTION_NOT_FOUND: 'Transaction not found.',
  INVALID_TRANSACTION_ID: 'Invalid Transaction ID format.',
  TRANSACTION_ALREADY_PROCESSED: 'Transaction has already been processed.',
  
  // Account management errors
  ACCOUNT_NOT_FOUND: 'Account not found.',
  ACCOUNT_CLOSED: 'Account is closed.',
  ACCOUNT_SUSPENDED: 'Account is suspended.',
  CREDIT_LIMIT_EXCEEDED: 'Credit limit exceeded.',
  
  // Customer information errors
  CUSTOMER_NOT_FOUND: 'Customer not found.',
  INVALID_CUSTOMER_ID: 'Invalid Customer ID format.',
  ADDRESS_INCOMPLETE: 'Address information is incomplete.',
  PHONE_NUMBER_INVALID: 'Invalid phone number format.',
  
  // Bill payment errors
  PAYMENT_AMOUNT_INVALID: 'Invalid payment amount.',
  PAYMENT_EXCEEDS_BALANCE: 'Payment amount exceeds current balance.',
  PAYMENT_PROCESSING_ERROR: 'Error processing payment. Please try again.',
} as const;

// ============================================================================
// FORM FIELD LABELS
// ============================================================================

/**
 * Form field labels extracted from BMS definitions
 */
export const FORM_LABELS = {
  // Login screen labels
  USER_ID_LABEL: 'User ID     :',
  PASSWORD_LABEL: 'Password    :',
  
  // Account information labels
  ACCOUNT_NUMBER_LABEL: 'Account Number :',
  ACCOUNT_STATUS_LABEL: 'Active Y/N: ',
  ACCOUNT_OPENED_LABEL: 'Opened:',
  ACCOUNT_OPENED_COLON: 'Opened :',
  ACCOUNT_EXPIRY_LABEL: 'Expiry:',
  ACCOUNT_EXPIRY_COLON: 'Expiry :',
  ACCOUNT_REISSUE_LABEL: 'Reissue:',
  ACCOUNT_GROUP_LABEL: 'Account Group:',
  CREDIT_LIMIT_LABEL: 'Credit Limit        :',
  CASH_CREDIT_LIMIT_LABEL: 'Cash credit Limit   :',
  CURRENT_BALANCE_LABEL: 'Current Balance     :',
  CURRENT_CYCLE_CREDIT_LABEL: 'Current Cycle Credit:',
  CURRENT_CYCLE_DEBIT_LABEL: 'Current Cycle Debit :',
  
  // Customer information labels
  CUSTOMER_ID_LABEL: 'Customer id  :',
  CUSTOMER_SSN_LABEL: 'SSN:',
  CUSTOMER_DOB_LABEL: 'Date of birth:',
  CUSTOMER_FICO_LABEL: 'FICO Score:',
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
  GOVERNMENT_ID_LABEL: 'Government Issued Id Ref    : ',
  EFT_ACCOUNT_LABEL: 'EFT Account Id: ',
  PRIMARY_CARDHOLDER_LABEL: 'Primary Card Holder Y/N:',
  
  // Credit card labels
  CREDIT_CARD_NUMBER_LABEL: 'Credit Card Number:',
  CARD_SELECT_LABEL: 'Select    ',
  CARD_ACCOUNT_NUMBER_LABEL: 'Account Number',
  CARD_NUMBER_LABEL: ' Card Number ',
  CARD_ACTIVE_LABEL: 'Active ',
  
  // Transaction labels
  TRANSACTION_ID_LABEL: ' Transaction ID ',
  TRANSACTION_DATE_LABEL: '  Date  ',
  TRANSACTION_DESCRIPTION_LABEL: '     Description          ',
  TRANSACTION_AMOUNT_LABEL: '   Amount   ',
  SEARCH_TRANSACTION_LABEL: 'Search Tran ID:',
  TRANSACTION_SELECT_LABEL: 'Sel',
  
  // User management labels
  SEARCH_USER_LABEL: 'Search User ID:',
  USER_ID_COLUMN_LABEL: 'User ID ',
  USER_FIRST_NAME_LABEL: '     First Name     ',
  USER_LAST_NAME_LABEL: '     Last Name      ',
  USER_TYPE_LABEL: 'Type',
  
  // Bill payment labels
  BILL_ACCOUNT_ID_LABEL: 'Enter Acct ID:',
  CURRENT_BALANCE_DISPLAY: 'Your current balance is: ',
  PAYMENT_CONFIRMATION_LABEL: 'Do you want to pay your balance now. Please confirm: ',
} as const;

// ============================================================================
// TABLE SEPARATORS AND FORMATTING
// ============================================================================

/**
 * Table separators and formatting characters from BMS screens
 */
export const TABLE_FORMATTING = {
  // Dashes for column separators
  SHORT_DASH: '---',
  MEDIUM_DASH: '--------',
  LONG_DASH: '---------------',
  EXTRA_LONG_DASH: '--------------------',
  DESCRIPTION_DASH: '--------------------------',
  AMOUNT_DASH: '------------',
  TYPE_DASH: '----',
  
  // Special separators
  SEPARATOR_LINE: '------------------------------------------------------------------------',
  NATIONAL_RESERVE_BORDER: '+========================================+',
  NATIONAL_RESERVE_HEADER: '|%%%%%%%  NATIONAL RESERVE NOTE  %%%%%%%%|',
  NATIONAL_RESERVE_COUNTRY: '|%(1)  THE UNITED STATES OF KICSLAND (1)%|',
  NATIONAL_RESERVE_SYMBOL1: '|%$$              ___       ********  $$%|',
  NATIONAL_RESERVE_SYMBOL2: '|%$    {x}       (o o)                 $%|',
  NATIONAL_RESERVE_SYMBOL3: '|%$     ******  (  V  )      O N E     $%|',
  NATIONAL_RESERVE_SYMBOL4: '|%(1)          ---m-m---             (1)%|',
  NATIONAL_RESERVE_FOOTER: '|%%~~~~~~~~~~~ ONE DOLLAR ~~~~~~~~~~~~~%%|',
  
  // Date separators
  DATE_SEPARATOR: '-',
  SSN_SEPARATOR: '-',
  PHONE_SEPARATOR: '-',
} as const;

// ============================================================================
// SUCCESS AND CONFIRMATION MESSAGES
// ============================================================================

/**
 * Success messages and confirmation text
 */
export const SUCCESS_MESSAGES = {
  LOGIN_SUCCESS: 'Login successful. Welcome to CardDemo.',
  ACCOUNT_UPDATED: 'Account information updated successfully.',
  PAYMENT_PROCESSED: 'Payment processed successfully.',
  USER_CREATED: 'User created successfully.',
  USER_UPDATED: 'User updated successfully.',
  USER_DELETED: 'User deleted successfully.',
  TRANSACTION_COMPLETED: 'Transaction completed successfully.',
  
  // Operation confirmations
  SAVE_CONFIRMATION: 'Changes saved successfully.',
  DELETE_CONFIRMATION: 'Item deleted successfully.',
  CANCEL_CONFIRMATION: 'Operation cancelled.',
  PROCESS_CONFIRMATION: 'Process completed successfully.',
  
  // Data retrieval confirmations
  ACCOUNT_FOUND: 'Account information retrieved.',
  TRANSACTION_FOUND: 'Transaction details retrieved.',
  USER_FOUND: 'User information retrieved.',
  SEARCH_RESULTS: 'Search completed successfully.',
} as const;

// ============================================================================
// SYSTEM STATUS MESSAGES
// ============================================================================

/**
 * System status and informational messages
 */
export const SYSTEM_MESSAGES = {
  LOADING: 'Loading...',
  PROCESSING: 'Processing...',
  SAVING: 'Saving...',
  SEARCHING: 'Searching...',
  CONNECTING: 'Connecting...',
  
  // Status indicators
  ONLINE: 'System Online',
  OFFLINE: 'System Offline',
  MAINTENANCE: 'System under maintenance',
  READY: 'System Ready',
  
  // Progress indicators
  PLEASE_WAIT: 'Please wait...',
  OPERATION_IN_PROGRESS: 'Operation in progress...',
  ALMOST_COMPLETE: 'Almost complete...',
  FINALIZING: 'Finalizing...',
} as const;

// ============================================================================
// PLACEHOLDER AND DEFAULT VALUES
// ============================================================================

/**
 * Default values and placeholders from BMS definitions
 */
export const DEFAULT_VALUES = {
  PASSWORD_PLACEHOLDER: '________',
  EMPTY_SPACE: ' ',
  SSN_PLACEHOLDER: '999-99-9999',
  PHONE_PLACEHOLDER: '(___) ___-____',
  
  // Initial values for form fields
  INITIAL_SPACE: '        ',
  INITIAL_SINGLE_SPACE: ' ',
  INITIAL_YEAR: 'YYYY',
  INITIAL_MONTH: 'MM',
  INITIAL_DAY: 'DD',
} as const;

// ============================================================================
// RESPONSIVE DESIGN MESSAGES
// ============================================================================

/**
 * Messages for responsive design and mobile adaptations
 */
export const RESPONSIVE_MESSAGES = {
  MOBILE_MENU: 'Menu',
  MOBILE_BACK: 'Back',
  MOBILE_NEXT: 'Next',
  MOBILE_PREVIOUS: 'Previous',
  MOBILE_SEARCH: 'Search',
  MOBILE_FILTER: 'Filter',
  MOBILE_SORT: 'Sort',
  
  // Mobile-specific instructions
  MOBILE_SWIPE_LEFT: 'Swipe left for next page',
  MOBILE_SWIPE_RIGHT: 'Swipe right for previous page',
  MOBILE_TAP_TO_EDIT: 'Tap to edit',
  MOBILE_DOUBLE_TAP: 'Double tap to select',
  
  // Responsive breakpoint messages
  DESKTOP_VIEW: 'Desktop View',
  TABLET_VIEW: 'Tablet View',
  MOBILE_VIEW: 'Mobile View',
} as const;

// ============================================================================
// INTERNATIONALIZATION STRUCTURE
// ============================================================================

/**
 * Internationalization-ready message structure for future localization
 */
export const I18N_KEYS = {
  // Common actions
  ACTIONS: {
    SAVE: 'actions.save',
    CANCEL: 'actions.cancel',
    DELETE: 'actions.delete',
    EDIT: 'actions.edit',
    VIEW: 'actions.view',
    SEARCH: 'actions.search',
    CLEAR: 'actions.clear',
    SUBMIT: 'actions.submit',
    RESET: 'actions.reset',
    CLOSE: 'actions.close',
  },
  
  // Form validation
  VALIDATION: {
    REQUIRED: 'validation.required',
    INVALID_FORMAT: 'validation.invalid_format',
    TOO_SHORT: 'validation.too_short',
    TOO_LONG: 'validation.too_long',
    OUT_OF_RANGE: 'validation.out_of_range',
  },
  
  // Error categories
  ERRORS: {
    AUTHENTICATION: 'errors.authentication',
    AUTHORIZATION: 'errors.authorization',
    NETWORK: 'errors.network',
    SERVER: 'errors.server',
    VALIDATION: 'errors.validation',
  },
  
  // Success messages
  SUCCESS: {
    SAVE: 'success.save',
    DELETE: 'success.delete',
    CREATE: 'success.create',
    UPDATE: 'success.update',
    PROCESS: 'success.process',
  },
} as const;

// ============================================================================
// ACCESSIBILITY CONSTANTS
// ============================================================================

/**
 * Accessibility-related constants for screen readers and assistive technologies
 */
export const ACCESSIBILITY = {
  ARIA_LABELS: {
    MAIN_NAVIGATION: 'Main navigation',
    BREADCRUMB: 'Breadcrumb navigation',
    FORM_SECTION: 'Form section',
    DATA_TABLE: 'Data table',
    SEARCH_RESULTS: 'Search results',
    ERROR_MESSAGE: 'Error message',
    SUCCESS_MESSAGE: 'Success message',
    LOADING_INDICATOR: 'Loading indicator',
  },
  
  SCREEN_READER_TEXT: {
    REQUIRED_FIELD: 'Required field',
    INVALID_INPUT: 'Invalid input',
    FIELD_ERROR: 'Field has error',
    FORM_SUBMITTED: 'Form submitted',
    PAGE_LOADING: 'Page loading',
    CONTENT_LOADED: 'Content loaded',
  },
  
  KEYBOARD_SHORTCUTS: {
    SKIP_TO_CONTENT: 'Skip to main content',
    SKIP_TO_NAVIGATION: 'Skip to navigation',
    OPEN_MENU: 'Open menu',
    CLOSE_MODAL: 'Close modal',
    SEARCH_FOCUS: 'Focus search',
  },
} as const;

// ============================================================================
// EXPORT ALL CONSTANTS
// ============================================================================

// Export all constants for easy access
export default {
  SCREEN_HEADERS,
  SCREEN_TITLES,
  FUNCTION_KEY_HELP,
  KEYBOARD_INSTRUCTIONS,
  HELP_TEXT,
  API_ERROR_MESSAGES,
  VALIDATION_ERRORS,
  FIELD_ERROR_MESSAGES,
  FORM_LABELS,
  TABLE_FORMATTING,
  SUCCESS_MESSAGES,
  SYSTEM_MESSAGES,
  DEFAULT_VALUES,
  RESPONSIVE_MESSAGES,
  I18N_KEYS,
  ACCESSIBILITY,
} as const;