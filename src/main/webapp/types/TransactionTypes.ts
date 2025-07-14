/**
 * TransactionTypes.ts
 * 
 * TypeScript interface definitions for transaction processing screens (COTRN00/COTRN01/COTRN02)
 * including transaction list data, search criteria, add transaction forms, and financial precision
 * types for amounts.
 * 
 * This file provides comprehensive type definitions for the three transaction processing screens
 * that replicate the original BMS mapset structures while enabling modern React component
 * development with exact functional equivalence to the COBOL/CICS application.
 * 
 * Key Features:
 * - Transaction list display with pagination and search (COTRN00)
 * - Transaction search and detailed view (COTRN01)
 * - Add transaction form with validation (COTRN02)
 * - Financial amount precision types for PostgreSQL NUMERIC(12,2) compatibility
 * - BMS field attribute mapping for form validation
 * - Date range filtering and transaction history queries
 * 
 * Copyright (c) 2023 CardDemo Application
 * Technology transformation: COBOL/CICS/BMS → Java/Spring Boot/React
 */

import { BaseScreenData } from './CommonTypes';
import { ValidationResult } from './ValidationTypes';

/**
 * Transaction Amount Type
 * 
 * Represents monetary amounts with exact decimal precision matching PostgreSQL NUMERIC(12,2)
 * and COBOL COMP-3 PIC S9(10)V99 fields. This type ensures exact financial calculations
 * and prevents floating-point precision errors in transaction processing.
 */
export type TransactionAmount = {
  /** 
   * String representation of the amount with exact decimal precision
   * Format: "-99999999.99" to "99999999.99"
   * Compatible with PostgreSQL NUMERIC(12,2) and Java BigDecimal
   */
  value: string;
  
  /** 
   * Formatted display value for UI presentation
   * Includes currency symbol and thousand separators
   * Example: "$1,234.56" or "($1,234.56)" for negative amounts
   */
  displayValue: string;
  
  /** 
   * Numeric value for calculations (use with caution)
   * Provided for compatibility but string value should be preferred
   */
  numericValue: number;
};

/**
 * Transaction Type Enumeration
 * 
 * Defines the types of transactions supported by the system
 * Maps to the TRANTYPE reference file and BMS TTYPCD field
 */
export type TransactionType = 
  | 'DEBIT'      // Debit transaction (withdrawal, purchase)
  | 'CREDIT'     // Credit transaction (deposit, refund)
  | 'PAYMENT'    // Payment transaction (bill payment, transfer)
  | 'ADJUSTMENT' // Adjustment transaction (fee, correction)
  | 'INQUIRY'    // Inquiry transaction (balance check, no financial impact)
  | 'AUTHORIZATION'; // Authorization transaction (pre-authorization)

/**
 * Transaction Category Enumeration
 * 
 * Defines the categories of transactions for classification and reporting
 * Maps to the TRANCATG reference file and BMS TCATCD field
 */
export type TransactionCategory = 
  | 'PURCHASE'   // Purchase transaction
  | 'CASH'       // Cash advance or ATM withdrawal
  | 'TRANSFER'   // Transfer between accounts
  | 'PAYMENT'    // Payment processing
  | 'FEE'        // Service fee or charge
  | 'INTEREST'   // Interest charge or payment
  | 'ADJUSTMENT' // Account adjustment
  | 'REFUND'     // Refund transaction
  | 'REVERSAL'   // Transaction reversal
  | 'OTHER';     // Other miscellaneous transactions

/**
 * Transaction Details Data Interface
 * 
 * Comprehensive transaction information structure used for transaction display,
 * editing, and processing. Maps to the complete transaction record structure
 * from the TRANSACT VSAM file and BMS transaction detail fields.
 */
export interface TransactionDetailsData {
  /** 
   * Unique transaction identifier (16 characters)
   * Equivalent to BMS TRNID field and VSAM TRANSACT key
   */
  transactionId: string;
  
  /** 
   * Account identifier associated with the transaction (11 characters)
   * Links to ACCTDAT VSAM file and BMS ACCTID field
   */
  accountId: string;
  
  /** 
   * Card number associated with the transaction (16 characters)
   * Links to CARDDAT VSAM file and BMS CARDNUM field
   */
  cardNumber: string;
  
  /** 
   * Transaction type code (see TransactionType enum)
   * Equivalent to BMS TTYPCD field (2 characters)
   */
  transactionType: TransactionType;
  
  /** 
   * Transaction category code (see TransactionCategory enum)
   * Equivalent to BMS TCATCD field (4 characters)
   */
  transactionCategory: TransactionCategory;
  
  /** 
   * Transaction amount with exact decimal precision
   * Equivalent to BMS TRNAMT field and PostgreSQL NUMERIC(12,2)
   */
  transactionAmount: TransactionAmount;
  
  /** 
   * Transaction description (60 characters)
   * Equivalent to BMS TDESC field
   */
  description: string;
  
  /** 
   * Transaction timestamp (ISO 8601 format)
   * Combines original date and time fields for modern date handling
   */
  transactionTimestamp: string;
  
  /** 
   * Merchant name for the transaction (30 characters)
   * Equivalent to BMS MNAME field
   */
  merchantName: string;
  
  /** 
   * Merchant city (25 characters)
   * Equivalent to BMS MCITY field
   */
  merchantCity: string;
  
  /** 
   * Merchant ZIP code (10 characters)
   * Equivalent to BMS MZIP field
   */
  merchantZip: string;
}

/**
 * Transaction Row Data Interface
 * 
 * Individual transaction row structure for the transaction list display (COTRN00).
 * Represents a single row in the 10-row transaction listing with selection support.
 */
export interface TransactionRowData {
  /** 
   * Selection field for the transaction row (1 character)
   * Equivalent to BMS SEL000x fields, supports 'S' for select
   */
  selection: string;
  
  /** 
   * Transaction ID for the row (16 characters)
   * Equivalent to BMS TRNID0x fields
   */
  transactionId: string;
  
  /** 
   * Transaction date for display (8 characters, MM/DD/YY format)
   * Equivalent to BMS TDATE0x fields
   */
  transactionDate: string;
  
  /** 
   * Transaction description for display (26 characters)
   * Equivalent to BMS TDESC0x fields
   */
  description: string;
  
  /** 
   * Transaction amount for display (12 characters)
   * Equivalent to BMS TAMT00x fields
   */
  amount: string;
  
  /** 
   * Selection state for UI management
   * Indicates if this row is currently selected
   */
  isSelected: boolean;
  
  /** 
   * Visibility state for UI management
   * Controls whether this row is visible in the current page
   */
  isVisible: boolean;
}

/**
 * Transaction List Data Interface
 * 
 * Data structure for the transaction list screen (COTRN00) including pagination,
 * search functionality, and transaction row data. Maintains exact functional
 * equivalence with the original BMS mapset structure.
 */
export interface TransactionListData {
  /** 
   * Common BMS header fields (trnname, pgmname, dates, titles)
   * Inherited from BaseScreenData for consistent screen header management
   */
  baseScreenData: BaseScreenData;
  
  /** 
   * Current page number for pagination control
   * Equivalent to BMS PAGENUM field (8 characters)
   */
  pageNumber: string;
  
  /** 
   * Search transaction ID input field (16 characters)
   * Equivalent to BMS TRNIDIN field for transaction search
   */
  searchTransactionId: string;
  
  /** 
   * Array of transaction rows for display (maximum 10 rows)
   * Equivalent to BMS SEL000x/TRNID0x/TDATE0x/TDESC0x/TAMT00x fields
   */
  transactionRows: TransactionRowData[];
  
  /** 
   * Total number of transactions available
   * Used for pagination calculation and display
   */
  totalTransactions: number;
  
  /** 
   * Indicates if more pages are available
   * Controls forward navigation availability
   */
  hasMorePages: boolean;
  
  /** 
   * Array of selected transaction IDs
   * Tracks which transactions are currently selected
   */
  selectedTransactions: string[];
  
  /** 
   * Error message for display (78 characters)
   * Equivalent to BMS ERRMSG field
   */
  errorMessage: string;
  
  /** 
   * Field attributes for form validation and display control
   * Maps BMS field attributes to React component properties
   */
  fieldAttributes: {
    searchTransactionId: {
      isRequired: boolean;
      maxLength: number;
      isReadOnly: boolean;
    };
    transactionRows: {
      isSelectable: boolean;
      maxDisplayRows: number;
    };
  };
}

/**
 * Transaction Search Criteria Interface
 * 
 * Data structure for the transaction search screen (COTRN01) including search
 * input, transaction details display, and field state management.
 */
export interface TransactionSearchCriteria {
  /** 
   * Common BMS header fields (trnname, pgmname, dates, titles)
   * Inherited from BaseScreenData for consistent screen header management
   */
  baseScreenData: BaseScreenData;
  
  /** 
   * Transaction ID search input field (16 characters)
   * Equivalent to BMS TRNIDIN field for transaction lookup
   */
  searchTransactionId: string;
  
  /** 
   * Complete transaction details for display
   * Populated when transaction is found and displayed
   */
  transactionDetails: TransactionDetailsData;
  
  /** 
   * Error message for display (78 characters)
   * Equivalent to BMS ERRMSG field
   */
  errorMessage: string;
  
  /** 
   * Indicates if transaction was found
   * Controls display of transaction details section
   */
  isTransactionFound: boolean;
  
  /** 
   * Read-only mode indicator
   * Controls field protection and interaction behavior
   */
  readOnlyMode: boolean;
  
  /** 
   * Field attributes for form validation and display control
   * Maps BMS field attributes to React component properties
   */
  fieldAttributes: {
    searchTransactionId: {
      isRequired: boolean;
      maxLength: number;
      isReadOnly: boolean;
      autoFocus: boolean;
    };
    transactionDetails: {
      isReadOnly: boolean;
      showFullDetails: boolean;
    };
  };
}

/**
 * Add Transaction Form Data Interface
 * 
 * Data structure for the add transaction screen (COTRN02) including all input
 * fields, validation states, and form processing controls.
 */
export interface AddTransactionFormData {
  /** 
   * Common BMS header fields (trnname, pgmname, dates, titles)
   * Inherited from BaseScreenData for consistent screen header management
   */
  baseScreenData: BaseScreenData;
  
  /** 
   * Account ID input field (11 characters)
   * Equivalent to BMS ACTIDIN field
   */
  accountId: string;
  
  /** 
   * Card number input field (16 characters)
   * Equivalent to BMS CARDNIN field
   */
  cardNumber: string;
  
  /** 
   * Transaction type code (2 characters)
   * Equivalent to BMS TTYPCD field
   */
  transactionType: TransactionType;
  
  /** 
   * Transaction category code (4 characters)
   * Equivalent to BMS TCATCD field
   */
  transactionCategory: TransactionCategory;
  
  /** 
   * Transaction source (10 characters)
   * Equivalent to BMS TRNSRC field
   */
  transactionSource: string;
  
  /** 
   * Transaction description (60 characters)
   * Equivalent to BMS TDESC field
   */
  description: string;
  
  /** 
   * Transaction amount with exact decimal precision
   * Equivalent to BMS TRNAMT field and PostgreSQL NUMERIC(12,2)
   */
  transactionAmount: TransactionAmount;
  
  /** 
   * Original transaction date (10 characters, YYYY-MM-DD format)
   * Equivalent to BMS TORIGDT field
   */
  originalDate: string;
  
  /** 
   * Process date (10 characters, YYYY-MM-DD format)
   * Equivalent to BMS TPROCDT field
   */
  processDate: string;
  
  /** 
   * Merchant ID (9 characters)
   * Equivalent to BMS MID field
   */
  merchantId: string;
  
  /** 
   * Merchant name (30 characters)
   * Equivalent to BMS MNAME field
   */
  merchantName: string;
  
  /** 
   * Merchant city (25 characters)
   * Equivalent to BMS MCITY field
   */
  merchantCity: string;
  
  /** 
   * Merchant ZIP code (10 characters)
   * Equivalent to BMS MZIP field
   */
  merchantZip: string;
  
  /** 
   * Confirmation field (1 character, Y/N)
   * Equivalent to BMS CONFIRM field
   */
  confirmation: string;
  
  /** 
   * Error message for display (78 characters)
   * Equivalent to BMS ERRMSG field
   */
  errorMessage: string;
  
  /** 
   * Validation result for form processing
   * Comprehensive validation state for all fields
   */
  validationResult: ValidationResult;
  
  /** 
   * Field attributes for form validation and display control
   * Maps BMS field attributes to React component properties
   */
  fieldAttributes: {
    accountId: {
      isRequired: boolean;
      maxLength: number;
      isReadOnly: boolean;
      autoFocus: boolean;
    };
    cardNumber: {
      isRequired: boolean;
      maxLength: number;
      isReadOnly: boolean;
    };
    transactionType: {
      isRequired: boolean;
      maxLength: number;
      isReadOnly: boolean;
    };
    transactionCategory: {
      isRequired: boolean;
      maxLength: number;
      isReadOnly: boolean;
    };
    transactionSource: {
      isRequired: boolean;
      maxLength: number;
      isReadOnly: boolean;
    };
    description: {
      isRequired: boolean;
      maxLength: number;
      isReadOnly: boolean;
    };
    transactionAmount: {
      isRequired: boolean;
      minValue: string;
      maxValue: string;
      isReadOnly: boolean;
    };
    originalDate: {
      isRequired: boolean;
      dateFormat: string;
      isReadOnly: boolean;
    };
    processDate: {
      isRequired: boolean;
      dateFormat: string;
      isReadOnly: boolean;
    };
    merchantId: {
      isRequired: boolean;
      maxLength: number;
      isReadOnly: boolean;
    };
    merchantName: {
      isRequired: boolean;
      maxLength: number;
      isReadOnly: boolean;
    };
    merchantCity: {
      isRequired: boolean;
      maxLength: number;
      isReadOnly: boolean;
    };
    merchantZip: {
      isRequired: boolean;
      maxLength: number;
      isReadOnly: boolean;
    };
    confirmation: {
      isRequired: boolean;
      allowedValues: string[];
      isReadOnly: boolean;
    };
  };
}

/**
 * Transaction Validation Schema Interface
 * 
 * Comprehensive validation rules for transaction processing forms including
 * field-level validation, cross-field validation, and business rule validation.
 */
export interface TransactionValidationSchema {
  /** 
   * Transaction ID validation rules
   * Validates format, length, and uniqueness
   */
  transactionIdValidation: {
    pattern: string;
    maxLength: number;
    isRequired: boolean;
    uniquenessCheck: boolean;
  };
  
  /** 
   * Account ID validation rules
   * Validates format, length, and existence
   */
  accountIdValidation: {
    pattern: string;
    maxLength: number;
    isRequired: boolean;
    existenceCheck: boolean;
  };
  
  /** 
   * Card number validation rules
   * Validates format, length, and Luhn algorithm
   */
  cardNumberValidation: {
    pattern: string;
    maxLength: number;
    isRequired: boolean;
    luhnCheck: boolean;
  };
  
  /** 
   * Amount validation rules
   * Validates range, precision, and format
   */
  amountValidation: {
    minValue: string;
    maxValue: string;
    precision: number;
    scale: number;
    isRequired: boolean;
  };
  
  /** 
   * Date validation rules
   * Validates format, range, and business rules
   */
  dateValidation: {
    format: string;
    minDate: string;
    maxDate: string;
    isRequired: boolean;
  };
  
  /** 
   * Merchant validation rules
   * Validates format, length, and existence
   */
  merchantValidation: {
    idPattern: string;
    nameMaxLength: number;
    cityMaxLength: number;
    zipPattern: string;
    isRequired: boolean;
  };
  
  /** 
   * Confirmation validation rules
   * Validates allowed values and requirement
   */
  confirmationValidation: {
    allowedValues: string[];
    isRequired: boolean;
  };
  
  /** 
   * Cross-field validation rules
   * Business rules spanning multiple fields
   */
  crossFieldValidation: {
    accountCardRelationship: boolean;
    dateConsistency: boolean;
    amountReasonableness: boolean;
    merchantLocationConsistency: boolean;
  };
}