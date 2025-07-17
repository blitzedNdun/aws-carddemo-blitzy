/**
 * CardDemo - Bill Payment TypeScript Type Definitions
 * 
 * This file contains comprehensive TypeScript interface definitions for the bill payment
 * screen (COBIL00) including payment form data, confirmation workflow, EFT processing,
 * and payment history tracking types.
 * 
 * Maps BMS field structures from COBIL00.bms to TypeScript interfaces while maintaining
 * exact field lengths, validation behavior, and processing workflow from the original
 * COBOL/BMS implementation.
 * 
 * Implements PostgreSQL NUMERIC(12,2) precision for payment amounts and maintains
 * payment processing business logic consistency across all bill payment operations.
 * 
 * Based on analysis of COBIL00.bms and COBIL00.CPY for field structure and validation rules.
 */

import { FormFieldAttributes } from './CommonTypes';
import { ValidationResult } from './ValidationTypes';
import { TransactionAmount } from './TransactionTypes';

// ===================================================================
// PAYMENT TYPE DEFINITIONS
// ===================================================================

/**
 * Payment Status Type
 * 
 * Defines the possible states of a bill payment transaction throughout its lifecycle.
 * Maps to payment processing workflow states for tracking and audit purposes.
 */
export type PaymentStatus = 
  | 'PENDING'     // Payment initiated but not yet processed
  | 'PROCESSING'  // Payment currently being processed by EFT system
  | 'COMPLETED'   // Payment successfully processed and confirmed
  | 'FAILED'      // Payment failed due to insufficient funds or system error
  | 'CANCELLED'   // Payment cancelled by user or system
  | 'REJECTED'    // Payment rejected by external payment processor
  | 'REVERSED'    // Payment reversed due to error or dispute
  | 'TIMEOUT'     // Payment timed out during processing
  | 'CONFIRMED';  // Payment confirmed and recorded in account

/**
 * Payment Type Definition
 * 
 * Specifies the method of payment processing for bill payment transactions.
 * Determines validation rules and processing workflow for different payment methods.
 */
export type PaymentType = 
  | 'BALANCE'     // Pay full account balance (default COBIL00 behavior)
  | 'MINIMUM'     // Pay minimum payment amount
  | 'CUSTOM'      // Pay custom amount specified by user
  | 'AUTOPAY'     // Automatic payment enrollment
  | 'EXTERNAL'    // External bank account payment (EFT)
  | 'INTERNAL';   // Internal account transfer

// ===================================================================
// CORE PAYMENT INTERFACES
// ===================================================================

/**
 * Bill Payment Form Data Interface
 * 
 * Primary data structure for COBIL00 bill payment screen form.
 * Contains all input fields and validation state for payment processing.
 * 
 * Maps to COBIL00.bms field layout:
 * - ACTIDIN: Account ID input (PIC X(11))
 * - CURBAL: Current balance display (PIC X(14))
 * - CONFIRM: Payment confirmation (PIC X(1))
 * - ERRMSG: Error message display (PIC X(78))
 * 
 * Preserves exact field positioning and validation behavior from original BMS implementation.
 */
export interface BillPaymentFormData {
  /**
   * Account ID - Maps to ACTIDIN field (PIC X(11))
   * 11-character numeric account identifier for payment processing
   * Required field with format validation and account existence checking
   */
  accountId: string;
  
  /**
   * Current Balance - Maps to CURBAL field (PIC X(14))
   * Account balance with PostgreSQL NUMERIC(12,2) precision
   * Display-only field populated from account data
   */
  currentBalance: TransactionAmount;
  
  /**
   * Payment Amount - Derived from current balance for full payment
   * Uses TransactionAmount type for exact decimal precision
   * Automatically set to currentBalance for bill payment workflow
   */
  paymentAmount: TransactionAmount;
  
  /**
   * Payment Confirmation - Maps to CONFIRM field (PIC X(1))
   * User confirmation for payment processing (Y/N validation)
   * Required field with Y/N validation and confirmation workflow
   */
  confirmation: string;
  
  /**
   * Payment Date - System-generated payment processing date
   * ISO 8601 formatted date for payment timestamp
   * Set by system when payment is processed
   */
  paymentDate: string;
  
  /**
   * Payment Method - Payment processing method selection
   * Determines payment routing and validation rules
   * Defaults to 'BALANCE' for full balance payment
   */
  paymentMethod: PaymentType;
  
  /**
   * Routing Number - Bank routing number for EFT payments
   * 9-digit ABA routing number for external bank transfers
   * Required when paymentMethod is 'EXTERNAL'
   */
  routingNumber: string;
  
  /**
   * Account Number - Bank account number for EFT payments
   * External bank account number for payment processing
   * Required when paymentMethod is 'EXTERNAL'
   */
  accountNumber: string;
  
  /**
   * Field Attributes - BMS field attribute mappings
   * Maps BMS DFHMDF attributes to TypeScript field definitions
   * Controls field protection, highlighting, and validation behavior
   */
  fieldAttributes: Record<string, FormFieldAttributes>;
}

/**
 * Payment Confirmation Data Interface
 * 
 * Transaction confirmation structure for payment verification workflow.
 * Contains confirmation details and processing results for payment transactions.
 * 
 * Used in payment confirmation dialog and transaction verification process.
 */
export interface PaymentConfirmationData {
  /**
   * Account ID - Account associated with payment
   * Links payment confirmation to specific account
   */
  accountId: string;
  
  /**
   * Payment Amount - Confirmed payment amount
   * Exact amount processed with PostgreSQL NUMERIC(12,2) precision
   */
  paymentAmount: TransactionAmount;
  
  /**
   * Confirmation Number - Unique payment confirmation identifier
   * System-generated confirmation number for payment tracking
   */
  confirmationNumber: string;
  
  /**
   * Transaction ID - Internal transaction identifier
   * Links payment to transaction record for audit trail
   */
  transactionId: string;
  
  /**
   * Payment Status - Current payment processing status
   * Real-time status of payment processing workflow
   */
  paymentStatus: PaymentStatus;
  
  /**
   * Processed Timestamp - Payment processing completion time
   * ISO 8601 formatted timestamp when payment was processed
   */
  processedTimestamp: string;
  
  /**
   * Error Message - Payment processing error details
   * Detailed error message if payment processing fails
   */
  errorMessage: string;
  
  /**
   * Validation Result - Payment validation outcome
   * Comprehensive validation result for payment processing
   */
  validationResult: ValidationResult;
}

/**
 * Payment Method Data Interface
 * 
 * EFT payment method and routing information for external bank transfers.
 * Contains banking details and validation status for payment processing.
 * 
 * Used for external payment method configuration and EFT processing.
 */
export interface PaymentMethodData {
  /**
   * Payment Type - Method of payment processing
   * Determines validation rules and processing workflow
   */
  paymentType: PaymentType;
  
  /**
   * Routing Number - Bank ABA routing number
   * 9-digit routing number for external bank identification
   */
  routingNumber: string;
  
  /**
   * Account Number - External bank account number
   * Account number for EFT payment processing
   */
  accountNumber: string;
  
  /**
   * Account Type - Type of external bank account
   * Checking or savings account designation
   */
  accountType: 'CHECKING' | 'SAVINGS';
  
  /**
   * Bank Name - Name of external bank
   * Financial institution name for payment processing
   */
  bankName: string;
  
  /**
   * Account Holder Name - Name on external bank account
   * Account holder name for payment verification
   */
  accountHolderName: string;
  
  /**
   * Default Payment Method - Whether this is the default payment method
   * Indicates if this payment method is the user's default choice
   */
  isDefault: boolean;
  
  /**
   * Last Used Date - Date payment method was last used
   * Timestamp of most recent usage for tracking purposes
   */
  lastUsedDate: string;
  
  /**
   * Validation Status - Payment method validation state
   * Current validation status of payment method configuration
   */
  validationStatus: 'VALID' | 'INVALID' | 'PENDING' | 'EXPIRED';
}

/**
 * Payment History Data Interface
 * 
 * Payment transaction tracking and audit trail information.
 * Contains comprehensive payment history details for account management.
 * 
 * Used for payment history display and audit trail functionality.
 */
export interface PaymentHistoryData {
  /**
   * Payment ID - Unique payment identifier
   * System-generated unique identifier for payment record
   */
  paymentId: string;
  
  /**
   * Account ID - Account associated with payment
   * Links payment to specific account for history tracking
   */
  accountId: string;
  
  /**
   * Payment Amount - Amount paid in transaction
   * Exact payment amount with PostgreSQL NUMERIC(12,2) precision
   */
  paymentAmount: TransactionAmount;
  
  /**
   * Payment Date - Date payment was processed
   * ISO 8601 formatted date of payment processing
   */
  paymentDate: string;
  
  /**
   * Payment Status - Final payment status
   * Status of payment at completion of processing
   */
  paymentStatus: PaymentStatus;
  
  /**
   * Confirmation Number - Payment confirmation identifier
   * Confirmation number provided to user for payment verification
   */
  confirmationNumber: string;
  
  /**
   * Transaction ID - Associated transaction identifier
   * Links payment to transaction record for complete audit trail
   */
  transactionId: string;
  
  /**
   * Audit Trail - Payment processing audit information
   * Detailed audit trail of payment processing steps
   */
  auditTrail: {
    /**
     * Processing Steps - Array of processing step details
     * Chronological list of payment processing steps
     */
    processingSteps: {
      step: string;
      timestamp: string;
      status: string;
      details: string;
    }[];
    
    /**
     * User ID - User who initiated payment
     * Identifier of user who processed the payment
     */
    userId: string;
    
    /**
     * Session ID - Session identifier for payment processing
     * Session tracking for payment processing workflow
     */
    sessionId: string;
    
    /**
     * IP Address - Client IP address for payment processing
     * Network source of payment processing request
     */
    ipAddress: string;
  };
  
  /**
   * Payment Method - Method used for payment processing
   * Payment method information for audit and tracking
   */
  paymentMethod: PaymentMethodData;
  
  /**
   * Processed By - System or user who processed payment
   * Identifier of processing entity for audit purposes
   */
  processedBy: string;
}

// ===================================================================
// VALIDATION SCHEMA INTERFACES
// ===================================================================

/**
 * Bill Payment Validation Schema Interface
 * 
 * Comprehensive validation schema for bill payment form processing.
 * Implements BMS field validation rules and business logic constraints
 * equivalent to COBOL validation paragraphs.
 * 
 * Provides type-safe validation patterns for all bill payment operations.
 */
export interface BillPaymentValidationSchema {
  /**
   * Account ID Validation - Validates account ID format and existence
   * Implements 11-character numeric validation with account verification
   * Maps to ACTIDIN field validation in COBIL00.bms
   */
  accountIdValidation: {
    /**
     * Pattern - Regular expression for account ID format
     * Validates 11-digit numeric format (99999999999)
     */
    pattern: RegExp;
    
    /**
     * Message - Error message for validation failure
     * Displayed when account ID validation fails
     */
    message: string;
    
    /**
     * Required - Whether field is mandatory
     * Account ID is required for payment processing
     */
    required: boolean;
    
    /**
     * Check Existence - Whether to verify account exists
     * Validates account ID against account database
     */
    checkExistence: boolean;
    
    /**
     * Check Status - Whether to verify account status
     * Validates account is active and eligible for payments
     */
    checkStatus: boolean;
  };
  
  /**
   * Payment Amount Validation - Validates payment amount format and range
   * Implements PostgreSQL NUMERIC(12,2) validation with business rules
   * Maps to payment amount calculation and validation logic
   */
  paymentAmountValidation: {
    /**
     * Pattern - Regular expression for amount format
     * Validates decimal format with up to 12 digits and 2 decimal places
     */
    pattern: RegExp;
    
    /**
     * Message - Error message for validation failure
     * Displayed when payment amount validation fails
     */
    message: string;
    
    /**
     * Required - Whether field is mandatory
     * Payment amount is required for payment processing
     */
    required: boolean;
    
    /**
     * Minimum Value - Minimum allowed payment amount
     * Minimum payment amount validation constraint
     */
    minValue: number;
    
    /**
     * Maximum Value - Maximum allowed payment amount
     * Maximum payment amount validation constraint
     */
    maxValue: number;
    
    /**
     * Decimal Places - Number of decimal places allowed
     * Fixed at 2 for currency precision
     */
    decimalPlaces: number;
  };
  
  /**
   * Confirmation Validation - Validates confirmation input
   * Implements Y/N validation for payment confirmation
   * Maps to CONFIRM field validation in COBIL00.bms
   */
  confirmationValidation: {
    /**
     * Pattern - Regular expression for confirmation format
     * Validates Y/N input format
     */
    pattern: RegExp;
    
    /**
     * Message - Error message for validation failure
     * Displayed when confirmation validation fails
     */
    message: string;
    
    /**
     * Required - Whether field is mandatory
     * Confirmation is required for payment processing
     */
    required: boolean;
    
    /**
     * Allowed Values - Valid confirmation values
     * Array of accepted confirmation values (Y, N)
     */
    allowedValues: string[];
  };
  
  /**
   * Payment Method Validation - Validates payment method selection and details
   * Implements payment method validation with EFT account verification
   * Validates routing number, account number, and payment method consistency
   */
  paymentMethodValidation: {
    /**
     * Routing Number Pattern - Regular expression for routing number format
     * Validates 9-digit ABA routing number format
     */
    routingNumberPattern: RegExp;
    
    /**
     * Account Number Pattern - Regular expression for account number format
     * Validates external bank account number format
     */
    accountNumberPattern: RegExp;
    
    /**
     * Payment Type Validation - Validates payment type selection
     * Ensures payment type is valid and supported
     */
    paymentTypeValidation: {
      allowedTypes: PaymentType[];
      message: string;
    };
    
    /**
     * EFT Validation - Validates EFT payment details
     * Validates routing and account numbers for external payments
     */
    eftValidation: {
      routingMessage: string;
      accountMessage: string;
      required: boolean;
    };
  };
  
  /**
   * Cross-Field Validation - Validates field relationships and dependencies
   * Implements business logic validation across multiple fields
   * Equivalent to COBOL validation paragraphs for complex business rules
   */
  crossFieldValidation: {
    /**
     * Account Balance Consistency - Validates payment amount against balance
     * Ensures payment amount does not exceed account balance
     */
    accountBalanceConsistency: (accountId: string, paymentAmount: TransactionAmount) => string | undefined;
    
    /**
     * Payment Method Consistency - Validates payment method with account type
     * Ensures payment method is compatible with account configuration
     */
    paymentMethodConsistency: (paymentType: PaymentType, accountId: string) => string | undefined;
    
    /**
     * EFT Account Validation - Validates external account details
     * Verifies routing number and account number consistency
     */
    eftAccountValidation: (routingNumber: string, accountNumber: string) => string | undefined;
    
    /**
     * Confirmation Consistency - Validates confirmation with payment details
     * Ensures confirmation is appropriate for payment amount and method
     */
    confirmationConsistency: (confirmation: string, paymentAmount: TransactionAmount) => string | undefined;
  };
}