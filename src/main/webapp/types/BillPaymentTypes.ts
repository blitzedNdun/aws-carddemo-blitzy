/**
 * BillPaymentTypes.ts
 * 
 * TypeScript interface definitions for bill payment screen (COBIL00) including payment 
 * form data, confirmation workflow, EFT processing, and payment history tracking types.
 * 
 * This file provides comprehensive type definitions for the BillPaymentComponent React 
 * screen that replicates the original COBIL00 BMS mapset structure while enabling modern
 * payment processing capabilities with Spring Boot PaymentService integration.
 * 
 * Key Features:
 * - Bill payment form data structure matching COBIL00 BMS field definitions
 * - Payment confirmation workflow with transaction verification capabilities
 * - EFT payment method management for bank account routing and validation
 * - Payment history tracking with audit trail for compliance and reporting
 * - Financial amount precision types for PostgreSQL NUMERIC(12,2) compatibility
 * - BMS field attribute mapping for React form validation integration
 * 
 * Technical Integration:
 * - Maps to PaymentService REST endpoints (/api/payments/process)
 * - Integrates with Spring Security for payment authorization
 * - Uses PostgreSQL payment tables for transaction persistence
 * - Supports Redis session management for payment workflow state
 * 
 * Copyright (c) 2023 CardDemo Application
 * Technology transformation: COBOL/CICS/BMS → Java/Spring Boot/React
 */

import { FormFieldAttributes } from './CommonTypes';
import { ValidationResult } from './ValidationTypes';
import { TransactionAmount } from './TransactionTypes';

/**
 * Payment Status Enumeration
 * 
 * Defines the possible states of a payment transaction throughout its lifecycle
 * from initiation to completion. Maps to payment status codes in the PostgreSQL
 * payment tables and provides comprehensive status tracking capabilities.
 */
export type PaymentStatus = 
  | 'INITIATED'     // Payment initiated but not processed
  | 'PROCESSING'    // Payment currently being processed
  | 'COMPLETED'     // Payment successfully completed
  | 'FAILED'        // Payment failed during processing
  | 'CANCELLED'     // Payment cancelled by user or system
  | 'PENDING'       // Payment pending approval or verification
  | 'REVERSED'      // Payment reversed or refunded
  | 'REJECTED';     // Payment rejected due to validation failure

/**
 * Payment Type Enumeration
 * 
 * Defines the types of payment methods supported by the system for bill payments.
 * Maps to payment type codes used in EFT processing and payment method validation.
 */
export type PaymentType = 
  | 'ACH'           // Automated Clearing House transfer
  | 'WIRE'          // Wire transfer
  | 'CHECK'         // Electronic check payment
  | 'DEBIT'         // Debit card payment
  | 'CREDIT'        // Credit card payment
  | 'ONLINE'        // Online banking payment
  | 'MOBILE';       // Mobile payment method

/**
 * Bill Payment Form Data Interface
 * 
 * Primary interface for the bill payment form screen (COBIL00) containing all
 * input fields, validation states, and payment processing information. This
 * interface maintains exact functional equivalence with the original BMS mapset
 * while enabling modern React form processing capabilities.
 */
export interface BillPaymentFormData {
  /**
   * Account ID for the payment transaction (11 characters)
   * Equivalent to BMS ACTIDIN field - links to account in PostgreSQL accounts table
   * Required field with validation for account existence and payment eligibility
   */
  accountId: string;

  /**
   * Current account balance for display (14 characters)
   * Equivalent to BMS CURBAL field - shows current balance before payment
   * Display-only field populated from AccountService with exact decimal precision
   */
  currentBalance: TransactionAmount;

  /**
   * Payment amount with exact decimal precision
   * Financial amount for the bill payment with PostgreSQL NUMERIC(12,2) precision
   * Requires validation for sufficient funds and reasonable payment limits
   */
  paymentAmount: TransactionAmount;

  /**
   * Payment confirmation field (1 character, Y/N)
   * Equivalent to BMS CONFIRM field - user confirmation for payment processing
   * Required field with validation for Y/N values before payment execution
   */
  confirmation: string;

  /**
   * Payment date for scheduling (10 characters, YYYY-MM-DD format)
   * Allows immediate payment or future-dated payment scheduling
   * Validates for business day scheduling and reasonable date ranges
   */
  paymentDate: string;

  /**
   * Payment method selection
   * Specifies the type of payment method for EFT processing
   * Links to PaymentMethodData for detailed payment routing information
   */
  paymentMethod: PaymentType;

  /**
   * Bank routing number for EFT processing (9 characters)
   * Required for ACH and wire transfer payments
   * Validates using ABA routing number format and bank validation services
   */
  routingNumber: string;

  /**
   * Bank account number for EFT processing (up to 20 characters)
   * Required for ACH and wire transfer payments
   * Validates format and performs basic account number validation
   */
  accountNumber: string;

  /**
   * BMS field attributes for form validation and display control
   * Maps original BMS field attributes to React component properties
   * Enables exact replication of BMS field behavior in React forms
   */
  fieldAttributes: {
    accountId: FormFieldAttributes;
    currentBalance: FormFieldAttributes;
    paymentAmount: FormFieldAttributes;
    confirmation: FormFieldAttributes;
    paymentDate: FormFieldAttributes;
    paymentMethod: FormFieldAttributes;
    routingNumber: FormFieldAttributes;
    accountNumber: FormFieldAttributes;
  };
}

/**
 * Payment Confirmation Data Interface
 * 
 * Interface for payment confirmation workflow containing transaction verification
 * information and processing status. Used after payment submission to provide
 * user feedback and transaction tracking capabilities.
 */
export interface PaymentConfirmationData {
  /**
   * Account ID for the confirmed payment (11 characters)
   * Links back to the original account for the payment transaction
   * Provides transaction context for confirmation display
   */
  accountId: string;

  /**
   * Confirmed payment amount with exact decimal precision
   * Final payment amount after validation and processing
   * Matches the original payment amount with any applied fees or adjustments
   */
  paymentAmount: TransactionAmount;

  /**
   * Payment confirmation number (16 characters)
   * Unique identifier for the payment confirmation
   * Generated by PaymentService for transaction tracking and customer reference
   */
  confirmationNumber: string;

  /**
   * Internal transaction ID for system tracking (20 characters)
   * Links to transaction processing system for audit and reconciliation
   * Used for payment status tracking and dispute resolution
   */
  transactionId: string;

  /**
   * Current payment processing status
   * Indicates the current state of the payment in the processing pipeline
   * Updates in real-time as payment moves through processing stages
   */
  paymentStatus: PaymentStatus;

  /**
   * Payment processing timestamp (ISO 8601 format)
   * Records when the payment was processed by the system
   * Used for audit trail and transaction timing analysis
   */
  processedTimestamp: string;

  /**
   * Error message if payment processing failed (optional)
   * Provides detailed error information for failed payments
   * Formatted for user display with actionable error resolution guidance
   */
  errorMessage?: string;

  /**
   * Validation result for the payment confirmation
   * Comprehensive validation status including any warnings or issues
   * Used for payment quality assurance and compliance verification
   */
  validationResult: ValidationResult;
}

/**
 * Payment Method Data Interface
 * 
 * Interface for EFT payment method information including bank account details,
 * routing information, and payment method validation status. Supports multiple
 * payment methods per customer for flexible payment processing options.
 */
export interface PaymentMethodData {
  /**
   * Payment method type classification
   * Specifies the category of payment method for processing routing
   * Determines validation rules and processing requirements
   */
  paymentType: PaymentType;

  /**
   * Bank routing number for EFT processing (9 characters)
   * ABA routing number for ACH and wire transfer processing
   * Validates against Federal Reserve routing directory
   */
  routingNumber: string;

  /**
   * Bank account number for EFT processing (up to 20 characters)
   * Customer's bank account number for payment processing
   * Encrypted at rest and masked for security compliance
   */
  accountNumber: string;

  /**
   * Bank account type classification
   * Specifies checking, savings, or other account types
   * Used for ACH processing and fee calculation
   */
  accountType: 'CHECKING' | 'SAVINGS' | 'BUSINESS' | 'MONEY_MARKET';

  /**
   * Bank name for display and verification (40 characters)
   * Financial institution name for payment method identification
   * Populated from routing number lookup for user verification
   */
  bankName: string;

  /**
   * Account holder name for verification (40 characters)
   * Name on the bank account for payment authorization
   * Must match customer name for fraud prevention
   */
  accountHolderName: string;

  /**
   * Default payment method indicator
   * Indicates if this is the customer's preferred payment method
   * Used for payment form pre-population and user experience
   */
  isDefault: boolean;

  /**
   * Last used date for payment method (ISO 8601 format)
   * Records when this payment method was last used
   * Used for payment method ranking and inactive method cleanup
   */
  lastUsedDate: string;

  /**
   * Payment method validation status
   * Indicates if the payment method has been verified and approved
   * Required for payment processing authorization
   */
  validationStatus: 'VERIFIED' | 'PENDING' | 'FAILED' | 'EXPIRED';
}

/**
 * Payment History Data Interface
 * 
 * Interface for payment transaction history tracking with comprehensive audit
 * trail information. Provides complete payment transaction lifecycle tracking
 * for compliance, reporting, and customer service purposes.
 */
export interface PaymentHistoryData {
  /**
   * Unique payment identifier (20 characters)
   * Primary key for payment transaction tracking
   * Links to payment processing system for detailed transaction information
   */
  paymentId: string;

  /**
   * Account ID for the payment transaction (11 characters)
   * Links to customer account for payment history queries
   * Enables account-specific payment history retrieval
   */
  accountId: string;

  /**
   * Payment amount with exact decimal precision
   * Historical payment amount for reporting and analysis
   * Maintains exact decimal precision for financial compliance
   */
  paymentAmount: TransactionAmount;

  /**
   * Payment processing date (ISO 8601 format)
   * Date when the payment was processed by the system
   * Used for payment history chronological ordering
   */
  paymentDate: string;

  /**
   * Final payment status for historical record
   * Status of the payment at completion of processing
   * Provides historical payment outcome for reporting
   */
  paymentStatus: PaymentStatus;

  /**
   * Payment confirmation number for customer reference (16 characters)
   * Customer-facing confirmation number for payment tracking
   * Used for customer service inquiries and dispute resolution
   */
  confirmationNumber: string;

  /**
   * Internal transaction ID for system tracking (20 characters)
   * Links to comprehensive transaction processing audit trail
   * Used for payment reconciliation and compliance reporting
   */
  transactionId: string;

  /**
   * Payment processing audit trail
   * Comprehensive log of payment processing steps and status changes
   * Provides complete audit history for compliance and troubleshooting
   */
  auditTrail: {
    /** Processing step identifier */
    step: string;
    /** Step processing timestamp */
    timestamp: string;
    /** Processing status for the step */
    status: PaymentStatus;
    /** Additional details or error information */
    details?: string;
  }[];

  /**
   * Payment method information used for the transaction
   * Snapshot of payment method details at time of payment
   * Provides historical context for payment processing analysis
   */
  paymentMethod: {
    paymentType: PaymentType;
    routingNumber: string;
    accountNumber: string; // Masked for security
    bankName: string;
  };

  /**
   * User ID who processed the payment
   * Tracks which user initiated or processed the payment
   * Used for audit trail and payment processing accountability
   */
  processedBy: string;
}

/**
 * Bill Payment Validation Schema Interface
 * 
 * Comprehensive validation rules for bill payment form processing including
 * field-level validation, cross-field validation, and business rule validation.
 * Ensures payment data integrity and compliance with business rules.
 */
export interface BillPaymentValidationSchema {
  /**
   * Account ID validation rules
   * Validates account existence, payment eligibility, and format requirements
   * Ensures account is active and authorized for payment processing
   */
  accountIdValidation: {
    /** Account ID format pattern (11 numeric characters) */
    pattern: string;
    /** Maximum length constraint */
    maxLength: number;
    /** Required field indicator */
    isRequired: boolean;
    /** Account existence validation in database */
    existenceCheck: boolean;
    /** Payment eligibility validation */
    paymentEligibilityCheck: boolean;
  };

  /**
   * Payment amount validation rules
   * Validates payment amount format, range, and business rules
   * Ensures payment amounts are reasonable and within account limits
   */
  paymentAmountValidation: {
    /** Minimum payment amount */
    minAmount: TransactionAmount;
    /** Maximum payment amount */
    maxAmount: TransactionAmount;
    /** Decimal precision validation */
    precision: number;
    /** Required field indicator */
    isRequired: boolean;
    /** Sufficient funds validation */
    sufficientFundsCheck: boolean;
    /** Daily payment limit validation */
    dailyLimitCheck: boolean;
  };

  /**
   * Confirmation validation rules
   * Validates payment confirmation input and requirements
   * Ensures explicit user confirmation for payment processing
   */
  confirmationValidation: {
    /** Allowed confirmation values */
    allowedValues: string[];
    /** Required field indicator */
    isRequired: boolean;
    /** Confirmation requirement for large payments */
    largePaymentConfirmation: boolean;
  };

  /**
   * Payment method validation rules
   * Validates payment method selection and EFT information
   * Ensures payment method is valid and properly configured
   */
  paymentMethodValidation: {
    /** Supported payment types */
    supportedTypes: PaymentType[];
    /** Routing number format validation */
    routingNumberPattern: string;
    /** Account number format validation */
    accountNumberPattern: string;
    /** Bank name requirement */
    bankNameRequired: boolean;
    /** Payment method verification requirement */
    verificationRequired: boolean;
  };

  /**
   * Cross-field validation rules
   * Business rules spanning multiple fields for comprehensive validation
   * Ensures payment data consistency and business rule compliance
   */
  crossFieldValidation: {
    /** Account balance vs payment amount validation */
    balanceAmountConsistency: boolean;
    /** Payment date and method compatibility */
    dateMethodCompatibility: boolean;
    /** Payment limits based on account type */
    accountTypePaymentLimits: boolean;
    /** EFT information completeness validation */
    eftInformationCompleteness: boolean;
  };
}