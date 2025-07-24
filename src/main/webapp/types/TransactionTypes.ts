/**
 * TransactionTypes.ts
 * 
 * TypeScript interface definitions for transaction processing screens (COTRN00/COTRN01/COTRN02)
 * including transaction list data, search criteria, add transaction forms, and financial precision 
 * types for amounts.
 * 
 * This file provides comprehensive type definitions for the three transaction processing screens:
 * - COTRN00: Transaction List - displays paginated transaction history with search functionality
 * - COTRN01: Transaction View - displays detailed transaction information for search/lookup
 * - COTRN02: Transaction Add - form for adding new transactions with validation
 * 
 * All financial amounts maintain exact PostgreSQL NUMERIC(12,2) precision matching COBOL COMP-3
 * decimal precision requirements. Transaction processing follows original CICS pseudo-conversational
 * patterns with React state management.
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0
 */

import { BaseScreenData } from './CommonTypes';
import { ValidationResult } from './ValidationTypes';

/**
 * Transaction Amount Type Definition
 * Maintains exact decimal precision for financial calculations
 * 
 * Based on COTRN02.bms amount format hint: (-99999999.99)
 * Maps to PostgreSQL NUMERIC(12,2) for exact decimal precision
 * Supports range: -9999999999.99 to 9999999999.99
 * 
 * This type ensures exact financial precision matching COBOL COMP-3 behavior:
 * - 10 digits for integer part (including sign)
 * - 2 digits for decimal places
 * - No floating point precision errors
 */
export type TransactionAmount = string; // String representation to maintain exact decimal precision

/**
 * Transaction Type Code Definition
 * Maps to COTRN01/COTRN02 TTYPCD field (2 characters)
 * 
 * Based on BMS field analysis:
 * - TTYPCD field: LENGTH=2, UNPROT (user input)
 * - Represents transaction type classification
 */
export type TransactionType = 
  | 'PU'  // Purchase transaction
  | 'CA'  // Cash advance  
  | 'RT'  // Return/refund
  | 'FE'  // Fee transaction
  | 'IN'  // Interest charge
  | 'PM'  // Payment
  | 'AD'  // Adjustment
  | 'TR'; // Transfer

/**
 * Transaction Category Code Definition  
 * Maps to COTRN01/COTRN02 TCATCD field (4 characters)
 * 
 * Based on BMS field analysis:
 * - TCATCD field: LENGTH=4, UNPROT (user input)
 * - Represents detailed transaction categorization
 */
export type TransactionCategory =
  | 'RTLP'  // Retail purchase
  | 'CASH'  // Cash advance
  | 'GASF'  // Gas/fuel purchase
  | 'GROC'  // Grocery purchase
  | 'REST'  // Restaurant/dining
  | 'FEES'  // Bank fees
  | 'INTR'  // Interest charges
  | 'PAYM'  // Payment received
  | 'REFN'  // Refund processed
  | 'TRAV'  // Travel expenses
  | 'ONLN'  // Online purchase
  | 'RECV'  // Recurring charge
  | 'TRAN'; // Transfer

/**
 * Transaction Row Data Interface
 * Represents individual transaction row in COTRN00 transaction list display
 * 
 * Maps to BMS fields:
 * - SEL0001-SEL0010: Selection field (1 char, UNPROT)
 * - TRNID01-TRNID10: Transaction ID (16 chars, ASKIP/FSET)
 * - TDATE01-TDATE10: Transaction date (8 chars, ASKIP/FSET)
 * - TDESC01-TDESC10: Description (26 chars, ASKIP/FSET)
 * - TAMT001-TAMT010: Amount (12 chars, ASKIP/FSET)
 */
export interface TransactionRowData {
  /** Selection indicator ('S' for selected, ' ' for unselected) */
  selection: string;
  
  /** Transaction ID (16 characters) - primary key for transaction lookup */
  transactionId: string;
  
  /** Transaction date (8 characters) - format: mm/dd/yy */
  transactionDate: string;
  
  /** Transaction description (26 characters) - merchant or transaction description */
  description: string;
  
  /** Transaction amount (12 characters) - formatted with sign and decimal places */
  amount: TransactionAmount;
  
  /** Indicates if this row is currently selected by user */
  isSelected: boolean;
  
  /** Indicates if this row should be visible (for filtering/pagination) */
  isVisible: boolean;
}

/**
 * Transaction List Data Interface  
 * Complete data structure for COTRN00 transaction list screen
 * 
 * Based on COTRN00.bms analysis:
 * - Standard BMS header fields (TRNNAME, PGMNAME, CURDATE, CURTIME, TITLE01, TITLE02)
 * - Page number field (PAGENUM - 8 chars, ASKIP/FSET)
 * - Search transaction ID field (TRNIDIN - 16 chars, UNPROT)
 * - 10 transaction row fields (SEL0001-SEL0010, TRNID01-TRNID10, etc.)
 * - Error message field (ERRMSG - 78 chars, ASKIP/BRT/FSET)
 */
export interface TransactionListData {
  /** Standard BMS header fields for screen identification and display */
  baseScreenData: BaseScreenData;
  
  /** Current page number for pagination (8 characters) */
  pageNumber: string;
  
  /** Search transaction ID input field (16 characters) */
  searchTransactionId: string;
  
  /** Array of up to 10 transaction rows for display */
  transactionRows: TransactionRowData[];
  
  /** Total number of transactions matching current criteria */
  totalTransactions: number;
  
  /** Indicates if more pages are available for navigation */
  hasMorePages: boolean;
  
  /** Array of currently selected transaction IDs */
  selectedTransactions: string[];
  
  /** Current error message for display (78 characters max) */
  errorMessage: string;
  
  /** BMS field attributes for dynamic field behavior control */
  fieldAttributes: {
    pageNumber: { color: string; hilight: string; };
    searchField: { color: string; hilight: string; };
    transactionRows: { color: string; hilight: string; };
    errorMessage: { color: string; hilight: string; };
  };
}

/**
 * Transaction Details Data Interface
 * Complete transaction information for detailed view and processing
 * 
 * Maps to COTRN01.bms transaction detail fields:
 * - TRNID: Transaction ID (16 chars)
 * - CARDNUM: Card number (16 chars) 
 * - TTYPCD: Transaction type code (2 chars)
 * - TCATCD: Transaction category code (4 chars)
 * - TRNSRC: Transaction source (10 chars)
 * - TDESC: Description (60 chars)
 * - TRNAMT: Amount (12 chars)
 * - TORIGDT: Original date (10 chars)
 * - TPROCDT: Process date (10 chars)
 * - MID: Merchant ID (9 chars)
 * - MNAME: Merchant name (30 chars)
 * - MCITY: Merchant city (25 chars)
 * - MZIP: Merchant ZIP (10 chars)
 */
export interface TransactionDetailsData {
  /** Unique transaction identifier (16 characters) */
  transactionId: string;
  
  /** Associated account ID (11 characters) */
  accountId: string;
  
  /** Associated card number (16 characters) */
  cardNumber: string;
  
  /** Transaction type code (2 characters) */
  transactionType: TransactionType;
  
  /** Transaction category code (4 characters) */
  transactionCategory: TransactionCategory;
  
  /** Transaction amount with exact decimal precision */
  transactionAmount: TransactionAmount;
  
  /** Transaction description (60 characters) */
  description: string;
  
  /** Transaction timestamp for audit trail */
  transactionTimestamp: Date;
  
  /** Merchant name (30 characters) */
  merchantName: string;
  
  /** Merchant city (25 characters) */
  merchantCity: string;
  
  /** Merchant ZIP code (10 characters) */
  merchantZip: string;
}

/**
 * Transaction Search Criteria Interface
 * Data structure for COTRN01 transaction search/view screen
 * 
 * Based on COTRN01.bms analysis:
 * - Standard BMS header fields
 * - Search transaction ID input (TRNIDIN - 16 chars, UNPROT/IC)
 * - Transaction detail display fields (all ASKIP/NORM for read-only)
 * - Error message field (ERRMSG - 78 chars, ASKIP/BRT/FSET)
 */
export interface TransactionSearchCriteria {
  /** Standard BMS header fields for screen identification */
  baseScreenData: BaseScreenData;
  
  /** Transaction ID to search for (16 characters) */
  searchTransactionId: string;
  
  /** Complete transaction details if found */
  transactionDetails?: TransactionDetailsData;
  
  /** Current error message for display */
  errorMessage: string;
  
  /** Indicates if transaction was found and details are available */
  isTransactionFound: boolean;
  
  /** Indicates if screen is in read-only mode for transaction display */
  readOnlyMode: boolean;
  
  /** BMS field attributes for field behavior control */
  fieldAttributes: {
    searchField: { color: string; hilight: string; attrb: string; };
    detailFields: { color: string; hilight: string; attrb: string; };
    errorMessage: { color: string; hilight: string; attrb: string; };
  };
}

/**
 * Add Transaction Form Data Interface
 * Complete form data structure for COTRN02 add transaction screen
 * 
 * Based on COTRN02.bms analysis:
 * - Standard BMS header fields
 * - Account/Card input fields (ACTIDIN - 11 chars, CARDNIN - 16 chars)
 * - Transaction detail fields (all UNPROT for user input)
 * - Confirmation field (CONFIRM - 1 char, UNPROT)
 * - Error message field (ERRMSG - 78 chars, ASKIP/BRT/FSET)
 */
export interface AddTransactionFormData {
  /** Standard BMS header fields for screen identification */
  baseScreenData: BaseScreenData;
  
  /** Account ID for the transaction (11 characters) */
  accountId: string;
  
  /** Card number for the transaction (16 characters) */
  cardNumber: string;
  
  /** Transaction type code (2 characters) */
  transactionType: TransactionType;
  
  /** Transaction category code (4 characters) */
  transactionCategory: TransactionCategory;
  
  /** Transaction source (10 characters) */
  transactionSource: string;
  
  /** Transaction description (60 characters) */
  description: string;
  
  /** Transaction amount with exact decimal precision */
  transactionAmount: TransactionAmount;
  
  /** Original transaction date (YYYY-MM-DD format) */
  originalDate: string;
  
  /** Process date (YYYY-MM-DD format) */
  processDate: string;
  
  /** Merchant ID (9 characters) */
  merchantId: string;
  
  /** Merchant name (30 characters) */
  merchantName: string;
  
  /** Merchant city (25 characters) */
  merchantCity: string;
  
  /** Merchant ZIP code (10 characters) */
  merchantZip: string;
  
  /** User confirmation (Y/N) */
  confirmation: string;
  
  /** Current error message for display */
  errorMessage: string;
  
  /** Form validation results */
  validationResult: ValidationResult;
  
  /** BMS field attributes for dynamic field behavior */
  fieldAttributes: {
    accountField: { color: string; hilight: string; attrb: string; };
    cardField: { color: string; hilight: string; attrb: string; };
    typeFields: { color: string; hilight: string; attrb: string; };
    amountField: { color: string; hilight: string; attrb: string; };
    dateFields: { color: string; hilight: string; attrb: string; };
    merchantFields: { color: string; hilight: string; attrb: string; };
    confirmField: { color: string; hilight: string; attrb: string; };
    errorMessage: { color: string; hilight: string; attrb: string; };
  };
}

/**
 * Transaction Validation Schema Interface
 * Comprehensive validation rules for transaction processing forms
 * 
 * Implements business validation rules equivalent to original COBOL validation:
 * - Transaction ID format validation (16 alphanumeric characters)
 * - Account ID validation (11 numeric characters with check digit)
 * - Card number validation (16 digits with Luhn algorithm)
 * - Amount validation (PostgreSQL NUMERIC(12,2) precision)
 * - Date validation (proper date format and business rules)
 * - Merchant information validation
 * - Cross-field validation for business rule enforcement
 */
export interface TransactionValidationSchema {
  /** Transaction ID validation rules */
  transactionIdValidation: {
    pattern: RegExp;
    required: boolean;
    length: { min: number; max: number; };
    errorMessage: string;
  };
  
  /** Account ID validation rules */
  accountIdValidation: {
    pattern: RegExp;
    required: boolean;
    length: { min: number; max: number; };
    checkDigit: boolean;
    errorMessage: string;
  };
  
  /** Card number validation rules */
  cardNumberValidation: {
    pattern: RegExp;
    required: boolean;
    length: { min: number; max: number; };
    luhnCheck: boolean;
    errorMessage: string;
  };
  
  /** Amount validation rules */
  amountValidation: {
    pattern: RegExp;
    required: boolean;
    precision: { scale: number; precision: number; };  
    range: { min: string; max: string; };
    errorMessage: string;
  };
  
  /** Date validation rules */
  dateValidation: {
    pattern: RegExp;
    required: boolean;
    format: string;
    businessRules: {
      allowFutureDates: boolean;
      maxDaysInPast: number;
    };
    errorMessage: string;
  };
  
  /** Merchant validation rules */
  merchantValidation: {
    merchantId: {
      pattern: RegExp;
      required: boolean;
      length: { min: number; max: number; };
    };
    merchantName: {
      pattern: RegExp;
      required: boolean;
      length: { min: number; max: number; };
    };
    errorMessage: string;
  };
  
  /** Confirmation validation rules */
  confirmationValidation: {
    pattern: RegExp;
    required: boolean;
    validValues: string[];
    errorMessage: string;
  };
  
  /** Cross-field validation rules */
  crossFieldValidation: {
    accountCardConsistency: {
      fields: string[];
      validationFn: (accountId: string, cardNumber: string) => boolean;
      errorMessage: string;
    };
    dateRangeConsistency: {
      fields: string[];
      validationFn: (originalDate: string, processDate: string) => boolean;
      errorMessage: string;
    };
    amountBusinessRules: {
      fields: string[];
      validationFn: (amount: TransactionAmount, transactionType: TransactionType) => boolean;
      errorMessage: string;
    };
  };
}