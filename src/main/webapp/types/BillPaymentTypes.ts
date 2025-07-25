/**
 * BillPaymentTypes.ts
 * 
 * TypeScript interface definitions for bill payment screen (COBIL00) including payment form data,
 * confirmation workflow, EFT processing, and payment history tracking types.
 * 
 * This file provides comprehensive type definitions for the Bill Payment Component (CB00 - COBIL00)
 * including payment form data structures, EFT account information, payment confirmation workflow,
 * and payment history tracking for audit trail compliance.
 * 
 * Based on COBIL00.bms analysis:
 * - Account ID input field (ACTIDIN - 11 characters, UNPROT, IC)
 * - Current balance display (CURBAL - 14 characters, ASKIP/FSET)
 * - Payment confirmation field (CONFIRM - 1 character, UNPROT Y/N)
 * - Error message display (ERRMSG - 78 characters, ASKIP/BRT/FSET)
 * 
 * All financial amounts maintain exact PostgreSQL NUMERIC(12,2) precision matching COBOL COMP-3
 * decimal precision requirements. Payment processing follows original CICS pseudo-conversational
 * patterns with React state management and Spring Boot REST API integration.
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0
 */

import { FormFieldAttributes } from './CommonTypes';
import { ValidationResult } from './ValidationTypes';
import { TransactionAmount } from './TransactionTypes';

/**
 * Payment Status Type Definition
 * Represents the current status of a bill payment transaction throughout its lifecycle
 * 
 * Status progression:
 * PENDING → PROCESSING → COMPLETED (success path)
 * PENDING → PROCESSING → FAILED (failure path)
 * Any status can transition to CANCELLED
 */
export type PaymentStatus = 
  | 'PENDING'     // Payment initiated but not yet processed
  | 'PROCESSING'  // Payment currently being processed by EFT system
  | 'COMPLETED'   // Payment successfully completed and posted
  | 'FAILED'      // Payment failed due to insufficient funds or technical error
  | 'CANCELLED'   // Payment cancelled by user or system before completion
  | 'REVERSED';   // Payment reversed due to dispute or error correction

/**
 * Payment Type Definition
 * Defines the payment method type for bill payment processing
 * 
 * Maps to payment processing workflows:
 * - EFT: Electronic Funds Transfer from bank account
 * - CREDIT: Credit card payment processing
 * - DEBIT: Debit card payment processing
 * - ACH: Automated Clearing House transfer
 */
export type PaymentType = 
  | 'EFT'      // Electronic Funds Transfer
  | 'CREDIT'   // Credit card payment
  | 'DEBIT'    // Debit card payment
  | 'ACH';     // ACH bank transfer

/**
 * Bill Payment Form Data Interface
 * Complete form data structure for COBIL00 bill payment screen
 * 
 * Based on COBIL00.bms field analysis:
 * - Standard BMS header fields (TRNNAME, PGMNAME, CURDATE, CURTIME, TITLE01, TITLE02)
 * - Account ID input field (ACTIDIN - 11 characters, UNPROT, IC)
 * - Current balance display (CURBAL - 14 characters, ASKIP/FSET)
 * - Payment confirmation field (CONFIRM - 1 character, UNPROT)
 * - Error message field (ERRMSG - 78 characters, ASKIP/BRT/FSET)
 * 
 * Extended for comprehensive payment processing:
 * - Payment amount with exact decimal precision
 * - Payment date selection and scheduling
 * - Payment method and routing information
 * - EFT account details for bank transfers
 */
export interface BillPaymentFormData {
  /** Account ID for bill payment (11 characters) - maps to ACTIDIN field */
  accountId: string;
  
  /** Current account balance (14 characters) - maps to CURBAL field, read-only display */
  currentBalance: TransactionAmount;
  
  /** Payment amount with exact decimal precision - user input for payment amount */
  paymentAmount: TransactionAmount;
  
  /** User confirmation for payment processing (Y/N) - maps to CONFIRM field */
  confirmation: string;
  
  /** Payment date for processing (YYYY-MM-DD format) - defaults to current date */
  paymentDate: string;
  
  /** Payment method type selection (EFT, CREDIT, DEBIT, ACH) */
  paymentMethod: PaymentType;
  
  /** Bank routing number for EFT payments (9 digits) */
  routingNumber: string;
  
  /** Bank account number for EFT payments (variable length, encrypted) */
  accountNumber: string;
  
  /** BMS field attributes for dynamic field behavior control */
  fieldAttributes: {
    /** Account ID field attributes */
    accountId: FormFieldAttributes;
    /** Current balance field attributes (read-only) */
    currentBalance: FormFieldAttributes;
    /** Payment amount field attributes */
    paymentAmount: FormFieldAttributes;
    /** Confirmation field attributes */
    confirmation: FormFieldAttributes;
    /** Payment date field attributes */
    paymentDate: FormFieldAttributes;
    /** Payment method field attributes */
    paymentMethod: FormFieldAttributes;
    /** EFT routing number field attributes */
    routingNumber: FormFieldAttributes;
    /** EFT account number field attributes */
    accountNumber: FormFieldAttributes;
  };
}

/**
 * Payment Confirmation Data Interface
 * Data structure for payment confirmation and transaction verification workflow
 * 
 * Used in the confirmation step after user submits payment form but before final processing.
 * Provides comprehensive confirmation details and transaction verification information
 * for user review and final approval before payment execution.
 */
export interface PaymentConfirmationData {
  /** Account ID being charged for the payment */
  accountId: string;
  
  /** Final payment amount after validation and formatting */
  paymentAmount: TransactionAmount;
  
  /** Unique confirmation number generated for this payment */
  confirmationNumber: string;
  
  /** Transaction ID assigned to this payment for tracking */
  transactionId: string;
  
  /** Current payment processing status */
  paymentStatus: PaymentStatus;
  
  /** Timestamp when payment confirmation was generated */
  processedTimestamp: Date;
  
  /** Error message if payment confirmation failed */
  errorMessage?: string;
  
  /** Validation result from payment form processing */
  validationResult: ValidationResult;
}

/**
 * Payment Method Data Interface
 * Comprehensive payment method information for EFT processing and payment routing
 * 
 * Stores detailed information about customer payment methods including
 * bank account details, validation status, and usage history for
 * efficient payment processing and audit trail maintenance.
 */
export interface PaymentMethodData {
  /** Type of payment method (EFT, CREDIT, DEBIT, ACH) */
  paymentType: PaymentType;
  
  /** Bank routing number for ACH/EFT payments (9 digits) */
  routingNumber: string;
  
  /** Bank account number (encrypted for security) */
  accountNumber: string;
  
  /** Bank account type (CHECKING, SAVINGS, BUSINESS) */
  accountType: 'CHECKING' | 'SAVINGS' | 'BUSINESS';
  
  /** Financial institution name */
  bankName: string;
  
  /** Account holder name (must match customer name) */
  accountHolderName: string;
  
  /** Indicates if this is the default payment method for the customer */
  isDefault: boolean;
  
  /** Date this payment method was last used successfully */
  lastUsedDate?: Date;
  
  /** Validation status of the payment method */
  validationStatus: 'VALID' | 'INVALID' | 'PENDING' | 'EXPIRED';
}

/**
 * Payment History Data Interface
 * Comprehensive payment transaction history for audit trail and customer inquiry
 * 
 * Maintains complete payment history including successful payments, failed attempts,
 * and cancelled transactions for regulatory compliance and customer service support.
 * Supports both individual payment tracking and bulk payment history reporting.
 */
export interface PaymentHistoryData {
  /** Unique payment identifier for tracking and reference */
  paymentId: string;
  
  /** Account ID associated with this payment */
  accountId: string;
  
  /** Payment amount with exact decimal precision */
  paymentAmount: TransactionAmount;
  
  /** Date when payment was processed or attempted */
  paymentDate: Date;
  
  /** Final status of the payment transaction */
  paymentStatus: PaymentStatus;
  
  /** Confirmation number generated for successful payments */
  confirmationNumber?: string;
  
  /** Associated transaction ID for account posting */
  transactionId?: string;
  
  /** Audit trail information for compliance and debugging */
  auditTrail: {
    /** User ID who initiated the payment */
    initiatedBy: string;
    /** Timestamp when payment was initiated */
    initiatedAt: Date;
    /** User ID who processed the payment (if different) */
    processedBy?: string;
    /** Timestamp when payment processing completed */
    processedAt?: Date;
    /** Additional audit notes or error details */
    notes?: string;
  };
  
  /** Payment method used for this transaction */
  paymentMethod: PaymentMethodData;
  
  /** User ID who processed the payment (system or user ID) */
  processedBy: string;
}

/**
 * Bill Payment Validation Schema Interface
 * Comprehensive validation rules for bill payment form processing
 * 
 * Implements business validation rules equivalent to original COBOL validation:
 * - Account ID validation (11 numeric characters with check digit)
 * - Payment amount validation (PostgreSQL NUMERIC(12,2) precision)
 * - Payment confirmation validation (Y/N values only)
 * - Payment method validation (routing number, account number formats)
 * - Cross-field validation for business rule enforcement
 */
export interface BillPaymentValidationSchema {
  /** Account ID validation rules */
  accountIdValidation: {
    /** Pattern for account ID format validation */
    pattern: RegExp;
    /** Indicates if account ID is required */
    required: boolean;
    /** Length constraints for account ID */
    length: { min: number; max: number; };
    /** Check digit validation required */
    checkDigit: boolean;
    /** Error message for account ID validation failures */
    errorMessage: string;
  };
  
  /** Payment amount validation rules */
  paymentAmountValidation: {
    /** Pattern for amount format validation */
    pattern: RegExp;
    /** Indicates if payment amount is required */
    required: boolean;
    /** Decimal precision constraints */
    precision: { scale: number; precision: number; };
    /** Valid amount range constraints */
    range: { min: string; max: string; };
    /** Error message for amount validation failures */
    errorMessage: string;
  };
  
  /** Payment confirmation validation rules */
  confirmationValidation: {
    /** Pattern for confirmation value validation (Y/N) */
    pattern: RegExp;
    /** Indicates if confirmation is required */
    required: boolean;
    /** Valid confirmation values */
    validValues: string[];
    /** Error message for confirmation validation failures */
    errorMessage: string;
  };
  
  /** Payment method validation rules */
  paymentMethodValidation: {
    /** Routing number validation */
    routingNumber: {
      /** Pattern for routing number format (9 digits) */
      pattern: RegExp;
      /** Indicates if routing number is required */
      required: boolean;
      /** Length constraints */
      length: { min: number; max: number; };
      /** ABA routing number checksum validation */
      checksumValidation: boolean;
    };
    /** Account number validation */
    accountNumber: {
      /** Pattern for account number format */
      pattern: RegExp;
      /** Indicates if account number is required */
      required: boolean;
      /** Length constraints */
      length: { min: number; max: number; };
      /** Account number format validation */
      formatValidation: boolean;
    };
    /** Account holder name validation */
    accountHolderName: {
      /** Pattern for name format validation */
      pattern: RegExp;
      /** Indicates if name is required */
      required: boolean;
      /** Length constraints */
      length: { min: number; max: number; };
    };
    /** Error message for payment method validation failures */
    errorMessage: string;
  };
  
  /** Cross-field validation rules */
  crossFieldValidation: {
    /** Payment amount vs account balance validation */
    amountBalanceCheck: {
      /** Fields involved in validation */
      fields: string[];
      /** Validation function for amount/balance consistency */
      validationFn: (paymentAmount: TransactionAmount, currentBalance: TransactionAmount) => boolean;
      /** Error message for amount/balance validation failures */
      errorMessage: string;
    };
    /** Payment method consistency validation */
    paymentMethodConsistency: {
      /** Fields involved in validation */
      fields: string[];
      /** Validation function for payment method data consistency */
      validationFn: (paymentType: PaymentType, routingNumber: string, accountNumber: string) => boolean;
      /** Error message for payment method validation failures */
      errorMessage: string;
    };
    /** Payment date business rules validation */
    paymentDateValidation: {
      /** Fields involved in validation */
      fields: string[];
      /** Validation function for payment date business rules */
      validationFn: (paymentDate: string) => boolean;
      /** Error message for payment date validation failures */
      errorMessage: string;
    };
  };
}