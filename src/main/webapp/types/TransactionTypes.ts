/**
 * CardDemo - Transaction Processing TypeScript Type Definitions
 * 
 * This file contains comprehensive TypeScript interface definitions for transaction
 * processing screens (COTRN00, COTRN01, COTRN02) including transaction list data,
 * search criteria, add transaction forms, and financial precision types for amounts.
 * 
 * Maps BMS field structures from COTRN00.bms, COTRN01.bms, and COTRN02.bms to
 * TypeScript interfaces while maintaining exact field lengths, precision, and
 * validation behavior from the original COBOL/BMS implementation.
 * 
 * Implements PostgreSQL NUMERIC(12,2) precision for financial transaction amounts
 * and maintains transaction business logic consistency across all transaction operations.
 */

import { BaseScreenData } from './CommonTypes';
import { ValidationResult } from './ValidationTypes';

// ===================================================================
// FINANCIAL PRECISION TYPES
// ===================================================================

/**
 * Transaction Amount Type
 * 
 * Represents financial transaction amounts with exact decimal precision matching
 * PostgreSQL NUMERIC(12,2) storage format. Maintains COBOL COMP-3 precision 
 * equivalence for financial calculations.
 * 
 * Format: Up to 10 digits before decimal point, exactly 2 digits after decimal
 * Range: -9999999999.99 to 9999999999.99
 * 
 * Based on COTRN02.bms field TRNAMT with LENGTH=12 and BMS format (-99999999.99)
 */
export type TransactionAmount = string; // Stored as string to maintain exact precision

/**
 * Transaction Type - Maps to TTYPCD field in BMS definitions
 * 
 * 2-character transaction type codes from TRANTYPE reference table.
 * Determines transaction category and processing rules.
 * 
 * Maps to COTRN01.bms TTYPCD field (PIC X(2)) and COTRN02.bms TTYPCD field
 */
export type TransactionType = 
  | '00' // Purchase
  | '01' // Cash Advance  
  | '02' // Credit
  | '03' // Debit
  | '04' // Reversal
  | '05' // Adjustment
  | '06' // Fee
  | '07' // Interest
  | '08' // Payment
  | '09' // Transfer
  | '10' // Refund
  | '11' // Chargeback
  | '12' // Authorization
  | '13' // Settlement
  | '14' // Void
  | '15'; // Other

/**
 * Transaction Category - Maps to TCATCD field in BMS definitions
 * 
 * 4-character transaction category codes from TRANCATG reference table.
 * Provides detailed transaction classification for reporting and analysis.
 * 
 * Maps to COTRN01.bms TCATCD field (PIC X(4)) and COTRN02.bms TCATCD field
 */
export type TransactionCategory = 
  | '5010' // Grocery Stores
  | '5020' // Gas Stations
  | '5030' // Restaurants
  | '5040' // Retail
  | '5050' // Online Shopping
  | '5060' // Travel
  | '5070' // Entertainment
  | '5080' // Healthcare
  | '5090' // Utilities
  | '5100' // ATM Cash
  | '5110' // Bank Fees
  | '5120' // Interest Charges
  | '5130' // Payments
  | '5140' // Transfers
  | '5150' // Adjustments
  | '5160' // Reversals
  | '5170' // Chargebacks
  | '5180' // Other
  | '5190' // Uncategorized
  | '5200'; // System Generated

// ===================================================================
// TRANSACTION DETAIL INTERFACES
// ===================================================================

/**
 * Transaction Details Data Interface
 * 
 * Comprehensive transaction detail structure for COTRN01 (Transaction View) screen.
 * Contains all transaction fields displayed in the transaction detail view including
 * card information, amounts, dates, and merchant details.
 * 
 * Maps to COTRN01.bms field layout with exact field lengths preserved.
 */
export interface TransactionDetailsData {
  /**
   * Transaction ID - Maps to TRNID field (PIC X(16))
   * Unique identifier for the transaction record
   */
  transactionId: string;
  
  /**
   * Account ID - Derived from account-card relationship
   * Links transaction to the account for balance management
   */
  accountId: string;
  
  /**
   * Card Number - Maps to CARDNUM field (PIC X(16))
   * Credit card number used for the transaction (masked for security)
   */
  cardNumber: string;
  
  /**
   * Transaction Type - Maps to TTYPCD field (PIC X(2))
   * Type code indicating the nature of the transaction
   */
  transactionType: TransactionType;
  
  /**
   * Transaction Category - Maps to TCATCD field (PIC X(4))
   * Category code for transaction classification and reporting
   */
  transactionCategory: TransactionCategory;
  
  /**
   * Transaction Amount - Maps to TRNAMT field (PIC X(12))
   * Financial amount with PostgreSQL NUMERIC(12,2) precision
   */
  transactionAmount: TransactionAmount;
  
  /**
   * Description - Maps to TDESC field (PIC X(60))
   * Transaction description for user identification
   */
  description: string;
  
  /**
   * Transaction Timestamp - Derived from TORIGDT/TPROCDT fields
   * ISO 8601 formatted timestamp for transaction processing
   */
  transactionTimestamp: string;
  
  /**
   * Merchant Name - Maps to MNAME field (PIC X(30))
   * Name of the merchant where transaction occurred
   */
  merchantName: string;
  
  /**
   * Merchant City - Maps to MCITY field (PIC X(25))
   * City where the merchant is located
   */
  merchantCity: string;
  
  /**
   * Merchant ZIP Code - Maps to MZIP field (PIC X(10))
   * ZIP code of the merchant location
   */
  merchantZip: string;
}

/**
 * Transaction Row Data Interface
 * 
 * Individual transaction row structure for COTRN00 (Transaction List) screen.
 * Represents a single transaction entry in the paginated transaction list.
 * 
 * Maps to COTRN00.bms transaction row fields (SEL, TRNID, TDATE, TDESC, TAMT)
 */
export interface TransactionRowData {
  /**
   * Selection Field - Maps to SEL0001-SEL0010 fields (PIC X(1))
   * User selection character for transaction operations ('S' for select)
   */
  selection: string;
  
  /**
   * Transaction ID - Maps to TRNID01-TRNID10 fields (PIC X(16))
   * Unique identifier for the transaction record
   */
  transactionId: string;
  
  /**
   * Transaction Date - Maps to TDATE01-TDATE10 fields (PIC X(8))
   * Date of the transaction in MM/DD/YY format
   */
  transactionDate: string;
  
  /**
   * Description - Maps to TDESC01-TDESC10 fields (PIC X(26))
   * Abbreviated transaction description for list display
   */
  description: string;
  
  /**
   * Amount - Maps to TAMT001-TAMT010 fields (PIC X(12))
   * Transaction amount formatted for display
   */
  amount: TransactionAmount;
  
  /**
   * Selection State - UI state for row selection
   * Indicates whether the transaction row is currently selected
   */
  isSelected: boolean;
  
  /**
   * Visibility State - UI state for row display
   * Controls whether the transaction row is visible in the current view
   */
  isVisible: boolean;
}

// ===================================================================
// SCREEN DATA INTERFACES
// ===================================================================

/**
 * Transaction List Data Interface
 * 
 * Complete data structure for COTRN00 (Transaction List) screen.
 * Manages paginated transaction listing with search functionality and user selection.
 * 
 * Maps to COTRN00.bms screen structure with pagination and selection capabilities.
 */
export interface TransactionListData {
  /**
   * Base Screen Data - Common BMS header fields
   * Includes TRNNAME, PGMNAME, CURDATE, CURTIME, TITLE01, TITLE02
   */
  baseScreenData: BaseScreenData;
  
  /**
   * Page Number - Maps to PAGENUM field (PIC X(8))
   * Current page number for pagination display
   */
  pageNumber: string;
  
  /**
   * Search Transaction ID - Maps to TRNIDIN field (PIC X(16))
   * Transaction ID search filter input by user
   */
  searchTransactionId: string;
  
  /**
   * Transaction Rows - Array of transaction row data
   * Up to 10 transaction rows per page (SEL0001-SEL0010 structure)
   */
  transactionRows: TransactionRowData[];
  
  /**
   * Total Transactions - Total count of transactions matching search criteria
   * Used for pagination calculation and display
   */
  totalTransactions: number;
  
  /**
   * Has More Pages - Pagination indicator
   * Indicates whether additional pages are available for browsing
   */
  hasMorePages: boolean;
  
  /**
   * Selected Transactions - Array of selected transaction IDs
   * Tracks user selections for batch operations
   */
  selectedTransactions: string[];
  
  /**
   * Error Message - Maps to ERRMSG field (PIC X(78))
   * Error message displayed for validation failures or system errors
   */
  errorMessage: string;
  
  /**
   * Field Attributes - BMS field attribute mappings
   * Controls field protection, highlighting, and validation behavior
   */
  fieldAttributes: Record<string, any>;
}

/**
 * Transaction Search Criteria Interface
 * 
 * Search and filter criteria structure for COTRN01 (Transaction View) screen.
 * Manages transaction lookup functionality with validation and error handling.
 * 
 * Maps to COTRN01.bms input structure for transaction search operations.
 */
export interface TransactionSearchCriteria {
  /**
   * Base Screen Data - Common BMS header fields
   * Includes TRNNAME, PGMNAME, CURDATE, CURTIME, TITLE01, TITLE02
   */
  baseScreenData: BaseScreenData;
  
  /**
   * Search Transaction ID - Maps to TRNIDIN field (PIC X(16))
   * Transaction ID input for search operations
   */
  searchTransactionId: string;
  
  /**
   * Transaction Details - Retrieved transaction details
   * Populated when transaction is found and displayed
   */
  transactionDetails: TransactionDetailsData | null;
  
  /**
   * Error Message - Maps to ERRMSG field (PIC X(78))
   * Error message displayed for search failures or validation errors
   */
  errorMessage: string;
  
  /**
   * Transaction Found Status - Search result indicator
   * Indicates whether the searched transaction was found
   */
  isTransactionFound: boolean;
  
  /**
   * Read-Only Mode - Screen protection status
   * Controls whether transaction details are in view-only mode
   */
  readOnlyMode: boolean;
  
  /**
   * Field Attributes - BMS field attribute mappings
   * Controls field protection, highlighting, and validation behavior
   */
  fieldAttributes: Record<string, any>;
}

/**
 * Add Transaction Form Data Interface
 * 
 * Complete form data structure for COTRN02 (Add Transaction) screen.
 * Manages transaction entry form with validation and confirmation workflow.
 * 
 * Maps to COTRN02.bms input structure with all required transaction fields.
 */
export interface AddTransactionFormData {
  /**
   * Base Screen Data - Common BMS header fields
   * Includes TRNNAME, PGMNAME, CURDATE, CURTIME, TITLE01, TITLE02
   */
  baseScreenData: BaseScreenData;
  
  /**
   * Account ID - Maps to ACTIDIN field (PIC X(11))
   * Account ID input for transaction association
   */
  accountId: string;
  
  /**
   * Card Number - Maps to CARDNIN field (PIC X(16))
   * Alternative card number input for transaction association
   */
  cardNumber: string;
  
  /**
   * Transaction Type - Maps to TTYPCD field (PIC X(2))
   * Transaction type code selection
   */
  transactionType: TransactionType;
  
  /**
   * Transaction Category - Maps to TCATCD field (PIC X(4))
   * Transaction category code selection
   */
  transactionCategory: TransactionCategory;
  
  /**
   * Transaction Source - Maps to TRNSRC field (PIC X(10))
   * Source system or channel identifier
   */
  transactionSource: string;
  
  /**
   * Description - Maps to TDESC field (PIC X(60))
   * Transaction description entered by user
   */
  description: string;
  
  /**
   * Transaction Amount - Maps to TRNAMT field (PIC X(12))
   * Transaction amount with validation for format (-99999999.99)
   */
  transactionAmount: TransactionAmount;
  
  /**
   * Original Date - Maps to TORIGDT field (PIC X(10))
   * Original transaction date in YYYY-MM-DD format
   */
  originalDate: string;
  
  /**
   * Process Date - Maps to TPROCDT field (PIC X(10))
   * Processing date in YYYY-MM-DD format
   */
  processDate: string;
  
  /**
   * Merchant ID - Maps to MID field (PIC X(9))
   * Merchant identifier for transaction
   */
  merchantId: string;
  
  /**
   * Merchant Name - Maps to MNAME field (PIC X(30))
   * Merchant name for transaction
   */
  merchantName: string;
  
  /**
   * Merchant City - Maps to MCITY field (PIC X(25))
   * Merchant city for transaction
   */
  merchantCity: string;
  
  /**
   * Merchant ZIP Code - Maps to MZIP field (PIC X(10))
   * Merchant ZIP code for transaction
   */
  merchantZip: string;
  
  /**
   * Confirmation - Maps to CONFIRM field (PIC X(1))
   * User confirmation for transaction submission (Y/N)
   */
  confirmation: string;
  
  /**
   * Error Message - Maps to ERRMSG field (PIC X(78))
   * Error message displayed for validation failures or system errors
   */
  errorMessage: string;
  
  /**
   * Validation Result - Form validation status
   * Comprehensive validation result including field-specific errors
   */
  validationResult: ValidationResult;
  
  /**
   * Field Attributes - BMS field attribute mappings
   * Controls field protection, highlighting, and validation behavior
   */
  fieldAttributes: Record<string, any>;
}

// ===================================================================
// VALIDATION SCHEMA INTERFACES
// ===================================================================

/**
 * Transaction Validation Schema Interface
 * 
 * Comprehensive validation schema for transaction processing forms.
 * Implements BMS field validation rules and business logic constraints
 * equivalent to COBOL validation paragraphs.
 * 
 * Provides type-safe validation patterns for all transaction operations.
 */
export interface TransactionValidationSchema {
  /**
   * Transaction ID Validation - Validates transaction ID format and existence
   * Implements 16-character alphanumeric validation with existence checking
   */
  transactionIdValidation: {
    pattern: RegExp;
    message: string;
    required: boolean;
    checkExistence?: boolean;
  };
  
  /**
   * Account ID Validation - Validates account ID format and linkage
   * Implements 11-character numeric validation with account verification
   */
  accountIdValidation: {
    pattern: RegExp;
    message: string;
    required: boolean;
    checkLinkage?: boolean;
  };
  
  /**
   * Card Number Validation - Validates card number format and status
   * Implements 16-character numeric validation with card verification
   */
  cardNumberValidation: {
    pattern: RegExp;
    message: string;
    required: boolean;
    checkStatus?: boolean;
  };
  
  /**
   * Amount Validation - Validates transaction amount format and range
   * Implements PostgreSQL NUMERIC(12,2) validation with business rules
   */
  amountValidation: {
    pattern: RegExp;
    message: string;
    required: boolean;
    minValue: number;
    maxValue: number;
    decimalPlaces: number;
  };
  
  /**
   * Date Validation - Validates date format and business rules
   * Implements YYYY-MM-DD format validation with date range checking
   */
  dateValidation: {
    pattern: RegExp;
    message: string;
    required: boolean;
    minDate?: string;
    maxDate?: string;
  };
  
  /**
   * Merchant Validation - Validates merchant information completeness
   * Implements merchant ID, name, city, and ZIP validation
   */
  merchantValidation: {
    merchantIdPattern: RegExp;
    merchantNamePattern: RegExp;
    merchantCityPattern: RegExp;
    merchantZipPattern: RegExp;
    messages: {
      merchantId: string;
      merchantName: string;
      merchantCity: string;
      merchantZip: string;
    };
  };
  
  /**
   * Confirmation Validation - Validates confirmation input
   * Implements Y/N validation for transaction confirmation
   */
  confirmationValidation: {
    pattern: RegExp;
    message: string;
    required: boolean;
    allowedValues: string[];
  };
  
  /**
   * Cross-Field Validation - Validates field relationships and dependencies
   * Implements business logic validation across multiple fields
   */
  crossFieldValidation: {
    accountCardConsistency: (accountId: string, cardNumber: string) => string | undefined;
    dateConsistency: (originalDate: string, processDate: string) => string | undefined;
    amountCategoryConsistency: (amount: TransactionAmount, category: TransactionCategory) => string | undefined;
  };
}