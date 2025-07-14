/**
 * CardTypes.ts
 * 
 * TypeScript interface definitions for card management screens (COCRDLI/COCRDSL/COCRDUP)
 * including card list pagination, search criteria, update forms, and validation schemas 
 * for card numbers and security fields.
 * 
 * This file provides comprehensive type definitions for the Card List, Card Search, and
 * Card Update React components, ensuring exact functional equivalence with the original
 * COBOL/BMS implementation while supporting modern TypeScript development with enhanced
 * validation and type safety.
 * 
 * Key Features:
 * - Exact BMS field mapping from COCRDLI, COCRDSL, and COCRDUP mapsets
 * - Card number validation with Luhn algorithm support for 16-digit validation
 * - Pagination support for card listing with 7 cards per page
 * - Search and filter capabilities for account ID and card number
 * - Optimistic locking for concurrent update protection
 * - CVV code validation and security field handling
 * - Comprehensive audit trail support for card modifications
 * 
 * BMS Source Files:
 * - COCRDLI.bms: Card List screen definition with pagination
 * - COCRDSL.bms: Card Search screen definition with detailed view
 * - COCRDUP.bms: Card Update screen definition with form validation
 * - COCRDLI.CPY: Card List copybook structure
 * - COCRDSL.CPY: Card Search copybook structure
 * - COCRDUP.CPY: Card Update copybook structure
 * 
 * Copyright (c) 2023 CardDemo Application
 * Technology transformation: COBOL/CICS/BMS → Java/Spring Boot/React
 */

import { FormFieldAttributes, BaseScreenData } from './CommonTypes';
import { AccountId } from './AccountTypes';
import { FormValidationSchema } from './ValidationTypes';

/**
 * Card Number Type
 * 
 * Represents a credit card number as a 16-character string with validation
 * support for Luhn algorithm verification. Maps to CARDSID field in BMS
 * definitions with LENGTH=16 and PICIN='9999999999999999' validation.
 */
export type CardNumber = string;

/**
 * Card Status Type
 * 
 * Represents the card active status as a single character field matching
 * CRDSTCD field in BMS definition. Values: 'Y' (Active) or 'N' (Inactive).
 */
export type CardStatus = 'Y' | 'N';

/**
 * Expiry Date Interface
 * 
 * Represents card expiry date components as used in COCRDSL and COCRDUP
 * screens where expiry date is displayed as separate month/year fields.
 * Matches the BMS field structure with validation and formatting.
 */
export interface ExpiryDate {
  /** Month component (2 digits, 01-12) - equivalent to EXPMON field */
  month: string;
  /** Year component (4 digits) - equivalent to EXPYEAR field */
  year: string;
  /** Day component (2 digits, 01-31) - equivalent to EXPDAY field */
  day: string;
  /** Validation flag indicating if expiry date is valid */
  isValid: boolean;
  /** Indicates if the card has expired */
  isExpired: boolean;
}

/**
 * Card Row Data Interface
 * 
 * Represents a single card row in the COCRDLI card list display.
 * Maps to the repeating card row structure (CRDSEL1-7, ACCTNO1-7, etc.)
 * with 7 cards displayed per page.
 */
export interface CardRowData {
  /** Selection indicator for the card row - equivalent to CRDSEL1-7 fields */
  selection: string;
  /** Account number associated with the card - equivalent to ACCTNO1-7 fields */
  accountNumber: string;
  /** Card number for display - equivalent to CRDNUM1-7 fields */
  cardNumber: string;
  /** Card status (Y/N) - equivalent to CRDSTS1-7 fields */
  cardStatus: CardStatus;
  /** Indicates if this row is selected for action */
  isSelected: boolean;
  /** Indicates if this row is visible in current page */
  isVisible: boolean;
}

/**
 * Card List Data Interface
 * 
 * Represents the complete data structure for the COCRDLI card list screen
 * including pagination support, search criteria, and card row data.
 * Maps to the complete BMS mapset structure with all required fields.
 */
export interface CardListData {
  /** Base screen data including transaction name, program name, titles, and timestamps */
  baseScreenData: BaseScreenData;
  
  /** Current page number for pagination - equivalent to PAGENO field */
  pageNumber: number;
  
  /** Account ID for filtering cards - equivalent to ACCTSID field */
  accountId: string;
  
  /** Card number for filtering - equivalent to CARDSID field */
  cardNumber: string;
  
  /** Array of card row data (max 7 per page) - equivalent to CRDSEL1-7 structure */
  cardRows: CardRowData[];
  
  /** Total number of cards matching search criteria */
  totalCards: number;
  
  /** Indicates if more pages are available */
  hasMorePages: boolean;
  
  /** Array of selected card identifiers */
  selectedCards: string[];
  
  /** Error message for display - equivalent to ERRMSG field */
  errorMessage: string;
  
  /** BMS field attributes for form rendering and validation */
  fieldAttributes: {
    accountId: FormFieldAttributes;
    cardNumber: FormFieldAttributes;
    pageNumber: FormFieldAttributes;
    cardRows: FormFieldAttributes[];
  };
}

/**
 * Card Search Criteria Interface
 * 
 * Represents the search criteria and detailed card information for the COCRDSL
 * card search screen. Includes both input criteria and result data display.
 */
export interface CardSearchCriteria {
  /** Base screen data including transaction name, program name, titles, and timestamps */
  baseScreenData: BaseScreenData;
  
  /** Account ID for search - equivalent to ACCTSID field */
  accountId: string;
  
  /** Card number for search - equivalent to CARDSID field */
  cardNumber: string;
  
  /** Card details returned from search */
  cardDetails: {
    /** Name on card - equivalent to CRDNAME field */
    cardName: string;
    /** Card active status - equivalent to CRDSTCD field */
    cardStatus: CardStatus;
    /** Card expiry date components - equivalent to EXPMON/EXPYEAR fields */
    expiryDate: ExpiryDate;
  };
  
  /** Error message for display - equivalent to ERRMSG field */
  errorMessage: string;
  
  /** Indicates if a card was found for the search criteria */
  isCardFound: boolean;
  
  /** Read-only mode indicator for display-only access */
  readOnlyMode: boolean;
  
  /** BMS field attributes for form rendering and validation */
  fieldAttributes: {
    accountId: FormFieldAttributes;
    cardNumber: FormFieldAttributes;
    cardName: FormFieldAttributes;
    cardStatus: FormFieldAttributes;
    expiryDate: FormFieldAttributes;
  };
}

/**
 * Card Update Form Data Interface
 * 
 * Represents the complete form data structure for the COCRDUP card update screen
 * including editable fields, validation state, and optimistic locking support.
 */
export interface CardUpdateFormData {
  /** Base screen data including transaction name, program name, titles, and timestamps */
  baseScreenData: BaseScreenData;
  
  /** Account ID (protected field) - equivalent to ACCTSID field */
  accountId: string;
  
  /** Card number for update - equivalent to CARDSID field */
  cardNumber: string;
  
  /** Name on card (editable) - equivalent to CRDNAME field */
  cardName: string;
  
  /** Card active status (editable) - equivalent to CRDSTCD field */
  cardStatus: CardStatus;
  
  /** Expiry month (editable) - equivalent to EXPMON field */
  expiryMonth: string;
  
  /** Expiry year (editable) - equivalent to EXPYEAR field */
  expiryYear: string;
  
  /** Expiry day (calculated) - equivalent to EXPDAY field */
  expiryDay: string;
  
  /** User confirmation for update operation */
  confirmation: boolean;
  
  /** Error message for display - equivalent to ERRMSG field */
  errorMessage: string;
  
  /** Validation result for form state */
  validationResult: {
    isValid: boolean;
    fieldErrors: Record<string, string>;
  };
  
  /** Optimistic locking version for concurrent update protection */
  optimisticLockVersion: number;
  
  /** Audit trail information for change tracking */
  auditTrail: {
    lastModifiedBy: string;
    lastModifiedDate: Date;
    changeReason: string;
  };
  
  /** BMS field attributes for form rendering and validation */
  fieldAttributes: {
    accountId: FormFieldAttributes;
    cardNumber: FormFieldAttributes;
    cardName: FormFieldAttributes;
    cardStatus: FormFieldAttributes;
    expiryMonth: FormFieldAttributes;
    expiryYear: FormFieldAttributes;
    expiryDay: FormFieldAttributes;
  };
}

/**
 * Card Details Data Interface
 * 
 * Represents detailed card information including security fields and validation
 * support for card number format and CVV codes with Luhn algorithm validation.
 */
export interface CardDetailsData {
  /** 16-digit card number with Luhn validation */
  cardNumber: string;
  
  /** Associated account identifier */
  accountId: string;
  
  /** Cardholder name */
  cardName: string;
  
  /** Card active status */
  cardStatus: CardStatus;
  
  /** Card expiry date */
  expiryDate: Date;
  
  /** CVV security code (3 digits) */
  cvvCode: string;
  
  /** Indicates if card is currently active */
  isActive: boolean;
  
  /** Last usage timestamp */
  lastUsedDate: Date;
  
  /** Luhn algorithm validation result */
  luhnValidation: {
    isValid: boolean;
    checksum: number;
  };
  
  /** Security validation results */
  securityValidation: {
    cvvValid: boolean;
    expiryValid: boolean;
    statusValid: boolean;
  };
}

/**
 * Card Validation Schema Interface
 * 
 * Comprehensive validation schema for card management forms including
 * card number format validation, Luhn algorithm checking, and security
 * field validation patterns.
 */
export interface CardValidationSchema {
  /** Card number validation with Luhn algorithm */
  cardNumberValidation: {
    pattern: string; // 16-digit numeric pattern
    luhnCheck: boolean;
    required: boolean;
    errorMessage: string;
  };
  
  /** Account ID validation */
  accountIdValidation: {
    pattern: string; // 11-digit numeric pattern
    required: boolean;
    errorMessage: string;
  };
  
  /** Card name validation */
  cardNameValidation: {
    minLength: number;
    maxLength: number;
    required: boolean;
    errorMessage: string;
  };
  
  /** Card status validation */
  cardStatusValidation: {
    allowedValues: CardStatus[];
    required: boolean;
    errorMessage: string;
  };
  
  /** Expiry date validation */
  expiryDateValidation: {
    monthPattern: string; // 01-12
    yearPattern: string;  // 4-digit year
    futureDate: boolean;  // Must be future date
    required: boolean;
    errorMessage: string;
  };
  
  /** CVV validation */
  cvvValidation: {
    pattern: string; // 3-digit numeric
    required: boolean;
    errorMessage: string;
  };
  
  /** Luhn algorithm validation function */
  luhnValidation: (cardNumber: string) => boolean;
  
  /** Cross-field validation for related fields */
  crossFieldValidation: {
    cardAccountValidation: (cardNumber: string, accountId: string) => boolean;
    expiryStatusValidation: (expiryDate: ExpiryDate, cardStatus: CardStatus) => boolean;
  };
}

/**
 * Card Audit Trail Interface
 * 
 * Audit trail information for card modifications including change history,
 * user tracking, and timestamp management for regulatory compliance.
 */
export interface CardAuditTrail {
  /** User who created the card */
  createdBy: string;
  
  /** Card creation timestamp */
  createdDate: Date;
  
  /** User who last modified the card */
  lastModifiedBy: string;
  
  /** Last modification timestamp */
  lastModifiedDate: Date;
  
  /** Version number for optimistic locking */
  version: number;
  
  /** Change history with detailed modification records */
  changeHistory: Array<{
    changeDate: Date;
    changedBy: string;
    changeType: 'CREATE' | 'UPDATE' | 'DELETE' | 'STATUS_CHANGE';
    fieldName: string;
    oldValue: string;
    newValue: string;
    reason: string;
  }>;
}