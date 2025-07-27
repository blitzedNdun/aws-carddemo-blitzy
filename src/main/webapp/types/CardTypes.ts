/**
 * CardTypes.ts
 * 
 * TypeScript interface definitions for card management screens (COCRDLI/COCRDSL/COCRDUP) including
 * card list pagination, search criteria, update forms, and validation schemas for card numbers and
 * security fields matching the original BMS mapset layouts.
 * 
 * This file maintains exact functional equivalence with original COBOL BMS definitions from:
 * - app/bms/COCRDLI.bms (Card List Screen with pagination)
 * - app/bms/COCRDSL.bms (Card Search/Selection Screen)
 * - app/bms/COCRDUP.bms (Card Update Screen)
 * - app/cpy-bms/COCRDLI.CPY (Card List Copybook)
 * - app/cpy-bms/COCRDSL.CPY (Card Search Copybook)
 * - app/cpy-bms/COCRDUP.CPY (Card Update Copybook)
 * 
 * Key mappings preserve:
 * - ACCTSID (11-char account number) → AccountId with validation
 * - CARDSID (16-char card number) → CardNumber with Luhn algorithm validation
 * - Card status (1-char Y/N/A/I/C/S) → CardStatus union type
 * - 7-row pagination layout → CardRowData array with pagination metadata
 * - BMS field attributes → FormFieldAttributes integration
 * - Expiry date decomposition → ExpiryDate interface with validation
 * - Optimistic locking → version field for concurrent update control
 * - Card security validation → CVV and Luhn algorithm type definitions
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0
 */

import { FormFieldAttributes, BaseScreenData } from './CommonTypes';
import { AccountId } from './AccountTypes';
import { FormValidationSchema } from './ValidationTypes';

/**
 * Card Number Type Definition
 * Maps BMS CARDSID field with 16-character numeric card identifier
 * Based on COCRDLI.bms CARDSID field: LENGTH=16, PIC X(16)
 * Implements Luhn algorithm validation for credit card number validation
 * Supports major card brands: Visa, MasterCard, American Express, Discover
 */
export type CardNumber = string;

/**
 * Card Status Type Definition
 * Maps BMS CRDSTS field for card active/inactive status and additional states
 * Based on COCRDSL.bms CRDSTCD field: LENGTH=1, possible values expanded for business logic
 * - Y: Active card (can be used for transactions)
 * - N: Inactive card (cannot be used, but account remains valid)
 * - A: Active (alias for Y for backward compatibility)
 * - I: Inactive (alias for N for backward compatibility)
 * - C: Cancelled (permanently deactivated)
 * - S: Suspended (temporarily blocked, can be reactivated)
 */
export type CardStatus = 'Y' | 'N' | 'A' | 'I' | 'C' | 'S';

/**
 * Expiry Date Interface
 * Handles BMS expiry date field decomposition from month/year display to complete date structure
 * Based on COCRDSL.bms expiry fields:
 * - EXPMON: LENGTH=2 (MM format, 01-12 range)
 * - EXPYEAR: LENGTH=4 (YYYY format)
 * - EXPDAY: LENGTH=2 (DD format, calculated as last day of month)
 * Provides validation and business logic for card expiration
 */
export interface ExpiryDate {
  /** Month component (2 digits, MM format, 01-12 range) */
  month: string;
  /** Year component (4 digits, YYYY format) */
  year: string;
  /** Day component (2 digits, DD format, last day of expiry month) */
  day: string;
  /** Computed validation status for the complete expiry date */
  isValid: boolean;
  /** Computed flag indicating if the card has expired (current date > expiry date) */
  isExpired: boolean;
}

/**
 * Card Row Data Interface
 * Represents a single card row in the COCRDLI pagination display
 * Based on COCRDLI.bms card row structure (CRDSEL1-7, ACCTNO1-7, CRDNUM1-7, CRDSTS1-7)
 * Each row displays one card with selection capability for detail operations
 */
export interface CardRowData {
  /** Selection field value (1 character) - CRDSEL1-7 fields */
  selection: string;
  /** Account number (11 characters) - ACCTNO1-7 fields */
  accountNumber: string;
  /** Card number (16 characters) - CRDNUM1-7 fields */
  cardNumber: CardNumber;
  /** Card status (1 character) - CRDSTS1-7 fields */
  cardStatus: CardStatus;
  /** Indicates if this row is currently selected by user */
  isSelected: boolean;
  /** Indicates if this row should be visible (for filtering) */
  isVisible: boolean;
}

/**
 * Card List Data Interface
 * Complete card listing structure for the COCRDLI screen with pagination support
 * Maps all fields from COCRDLI.bms preserving exact field structure and 7-row pagination
 * 
 * Key features:
 * - 7 cards per page matching BMS layout (CRDSEL1-7 through CRDSTS1-7)
 * - Account ID and card number search filters (ACCTSID, CARDSID)
 * - Page navigation with F7/F8 function keys
 * - Selection tracking for detail operations
 * - Error handling with ERRMSG display
 */
export interface CardListData {
  /** Base screen data (transaction, program, date, time, titles) */
  baseScreenData: BaseScreenData;
  /** Current page number (3 characters) - PAGENO field */
  pageNumber: string;
  /** Account ID filter (11 characters) - ACCTSID field with UNPROT attribute */
  accountId: AccountId;
  /** Card number filter (16 characters) - CARDSID field with UNPROT attribute */
  cardNumber: CardNumber;
  /** Array of 7 card rows for display - maps to CRDSEL1-7/ACCTNO1-7/CRDNUM1-7/CRDSTS1-7 */
  cardRows: CardRowData[];
  /** Total number of cards matching search criteria */
  totalCards: number;
  /** Indicates if more pages are available (enables F8=Forward) */
  hasMorePages: boolean;
  /** Array of selected card numbers for batch operations */
  selectedCards: CardNumber[];
  /** Error message (78 characters) - ERRMSG field */
  errorMessage: string;
  /** BMS field attributes for form field rendering and validation */
  fieldAttributes: Record<string, FormFieldAttributes>;
}

/**
 * Card Search Criteria Interface
 * Complete search and display structure for the COCRDSL screen
 * Maps all fields from COCRDSL.bms for card search and detail display
 * 
 * Key features:
 * - Account ID and card number search inputs (ACCTSID, CARDSID)
 * - Card detail display when found (CRDNAME, CRDSTCD, EXPMON, EXPYEAR)
 * - Read-only mode for display-only operations
 * - ENTER=Search functionality with validation
 */
export interface CardSearchCriteria {
  /** Base screen data (transaction, program, date, time, titles) */
  baseScreenData: BaseScreenData;
  /** Account ID search field (11 characters) - ACCTSID field with UNPROT/IC attributes */
  accountId: AccountId;
  /** Card number search field (16 characters) - CARDSID field with UNPROT attribute */
  cardNumber: CardNumber;
  /** Card details data when card is found */
  cardDetails: CardDetailsData | null;
  /** Error message (80 characters) - ERRMSG field */
  errorMessage: string;
  /** Indicates if a card was found for the search criteria */
  isCardFound: boolean;
  /** Indicates if screen is in read-only mode (all fields ASKIP) */
  readOnlyMode: boolean;
  /** BMS field attributes for form field rendering and validation */
  fieldAttributes: Record<string, FormFieldAttributes>;
}

/**
 * Card Update Form Data Interface
 * Complete form data structure for the COCRDUP screen with all editable fields
 * Maps all input fields from COCRDUP.bms preserving UNPROT field structure
 * 
 * Key features:
 * - Account ID (protected field, pre-filled) - ACCTSID with PROT attribute
 * - Card number (editable) - CARDSID with UNPROT attribute
 * - Card name (editable, 50 characters) - CRDNAME with UNPROT attribute
 * - Card status (editable, Y/N) - CRDSTCD with UNPROT attribute
 * - Expiry date components (editable) - EXPMON/EXPYEAR with UNPROT, EXPDAY hidden
 * - Form validation and optimistic locking
 * - Save/Cancel operations with F5/F12 function keys
 */
export interface CardUpdateFormData {
  /** Base screen data (transaction, program, date, time, titles) */
  baseScreenData: BaseScreenData;
  /** Account ID (11 characters) - ACCTSID field with PROT attribute (read-only) */
  accountId: AccountId;
  /** Card number (16 characters) - CARDSID field with UNPROT attribute */
  cardNumber: CardNumber;
  /** Name on card (50 characters) - CRDNAME field with UNPROT attribute */
  cardName: string;
  /** Card status (1 character) - CRDSTCD field with UNPROT attribute */
  cardStatus: CardStatus;
  /** Expiry month (2 digits) - EXPMON field with UNPROT attribute */
  expiryMonth: string;
  /** Expiry year (4 digits) - EXPYEAR field with UNPROT attribute */
  expiryYear: string;
  /** Expiry day (2 digits) - EXPDAY field with PROT/DRK attributes (calculated) */
  expiryDay: string;
  /** User confirmation for update operation (Y/N) */
  confirmation: string;
  /** Error message (80 characters) - ERRMSG field */
  errorMessage: string;
  /** Form validation result from React Hook Form integration */
  validationResult: ValidationResult;
  /** Optimistic locking version for concurrent update control */
  optimisticLockVersion: number;
  /** Audit trail information for change tracking */
  auditTrail: CardAuditTrail;
  /** BMS field attributes for form field rendering and validation */
  fieldAttributes: Record<string, FormFieldAttributes>;
}

/**
 * Card Details Data Interface
 * Complete card information structure for display and validation
 * Provides comprehensive card data with security validation capabilities
 * 
 * Key features:
 * - Card number with Luhn algorithm validation
 * - CVV code validation (3-4 digits)
 * - Expiry date validation and expiration checking
 * - Security validation for card authentication
 * - Activity tracking with last used date
 */
export interface CardDetailsData {
  /** Card number (16 characters) with Luhn validation */
  cardNumber: CardNumber;
  /** Associated account ID (11 characters) */
  accountId: AccountId;
  /** Name on card (50 characters) - CRDNAME from COCRDSL */
  cardName: string;
  /** Card status (Y/N/A/I/C/S) - CRDSTCD from COCRDSL */
  cardStatus: CardStatus;
  /** Complete expiry date structure with validation */
  expiryDate: ExpiryDate;
  /** CVV security code (3-4 digits, for validation only) */
  cvvCode: string;
  /** Indicates if card is currently active for transactions */
  isActive: boolean;
  /** Last transaction date for activity tracking */
  lastUsedDate: string | null;
  /** Luhn algorithm validation result for card number */
  luhnValidation: {
    isValid: boolean;
    algorithm: 'luhn';
    checkDigit: number;
  };
  /** Security validation results for authentication */
  securityValidation: {
    cvvValid: boolean;
    expiryValid: boolean;
    statusValid: boolean;
  };
}

/**
 * Card Validation Schema Interface
 * Comprehensive validation rules for card management forms
 * Implements all BMS validation constraints and business rules from original COBOL logic
 * 
 * Validation categories:
 * - Field-level validation (length, format, required)
 * - Card-specific validation (Luhn algorithm, CVV format)
 * - Cross-field validation (expiry date consistency, account/card relationship)
 * - Business rule validation (status transitions, security requirements)
 */
export interface CardValidationSchema {
  /** Card number validation rules (16-char with Luhn algorithm) */
  cardNumberValidation: {
    required: boolean;
    length: { min: number; max: number };
    pattern: RegExp;
    luhnValidation: boolean;
    supportedBrands: string[];
    errorMessage: string;
  };
  /** Account ID validation rules (11-char numeric, MUSTFILL) */
  accountIdValidation: {
    required: boolean;
    pattern: RegExp;
    length: { min: number; max: number };
    mustExist: boolean;
    errorMessage: string;
  };
  /** Card name validation rules (50-char alphanumeric with spaces) */
  cardNameValidation: {
    required: boolean;
    pattern: RegExp;
    length: { min: number; max: number };
    allowedCharacters: string;
    errorMessage: string;
  };
  /** Card status validation rules (valid status codes with transition logic) */
  cardStatusValidation: {
    required: boolean;
    allowedValues: CardStatus[];
    transitionRules: Record<CardStatus, CardStatus[]>;
    errorMessage: string;
  };
  /** Expiry date validation rules (month/year with future validation) */
  expiryDateValidation: {
    required: boolean;
    monthRange: { min: number; max: number };
    yearRange: { min: number; max: number };
    mustBeFuture: boolean;
    minimumMonthsValid: number;
    errorMessage: string;
  };
  /** CVV validation rules (3-4 digits depending on card brand) */
  cvvValidation: {
    required: boolean;
    pattern: RegExp;
    length: { visa: number; mastercard: number; amex: number; discover: number };
    errorMessage: string;
  };
  /** Luhn algorithm validation for card number verification */
  luhnValidation: {
    enabled: boolean;
    algorithm: 'luhn';
    errorMessage: string;
  };
  /** Form-level validation rules (cross-field dependencies) */
  crossFieldValidation: {
    /** Account ID and card number must be associated */
    accountCardRelationship: boolean;
    /** Card status transitions must follow business rules */
    statusTransitionValidation: boolean;
    /** Expiry date must be consistent with card lifecycle */
    expiryDateConsistency: boolean;
    /** Card name must match account holder when required */
    cardNameAccountHolderMatch: boolean;
  };
}

/**
 * Card Audit Trail Interface
 * Audit information for card management operations
 * Tracks all changes for compliance and troubleshooting
 * 
 * Features:
 * - Created/modified timestamps and user tracking
 * - Version control for optimistic locking
 * - Change history for audit compliance
 * - Supports regulatory requirements for card management
 */
export interface CardAuditTrail {
  /** User ID who created the card record */
  createdBy: string;
  /** Timestamp when card was created */
  createdDate: Date;
  /** User ID who last modified the card record */
  lastModifiedBy: string;
  /** Timestamp of last modification */
  lastModifiedDate: Date;
  /** Version number for optimistic locking (incremented on each update) */
  version: number;
  /** Array of change history entries for audit trail */
  changeHistory: {
    /** Timestamp of the change */
    changeDate: Date;
    /** User who made the change */
    changedBy: string;
    /** Field that was changed */
    fieldName: string;
    /** Previous value before change */
    oldValue: string;
    /** New value after change */
    newValue: string;
    /** Reason for the change (optional) */
    changeReason?: string;
  }[];
}

/**
 * Validation Result Interface (local definition for CardUpdateFormData)
 * Structured validation result containing field-level and form-level validation outcomes
 * Provides comprehensive validation feedback for card management forms
 */
interface ValidationResult {
  /** Overall validation status */
  isValid: boolean;
  /** Collection of field-level validation errors */
  fieldErrors: Record<string, string>;
  /** Form-level validation errors (cross-field validation) */
  formErrors: string[];
  /** Non-blocking validation warnings for user guidance */
  warnings: string[];
  /** Validation timestamp for change tracking */
  validatedAt: Date;
}