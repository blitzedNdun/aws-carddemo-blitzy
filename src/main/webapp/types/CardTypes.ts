/**
 * CardDemo - Card Management TypeScript Interface Definitions
 * 
 * This file contains comprehensive TypeScript interface definitions for card management
 * screens (COCRDLI/COCRDSL/COCRDUP) including card list pagination, search criteria,
 * update forms, and validation schemas that maintain exact functional equivalence
 * with the original BMS mapset layouts.
 * 
 * Maps BMS field structures from COCRDLI.bms, COCRDSL.bms, and COCRDUP.bms to TypeScript
 * interfaces while preserving exact field lengths, validation rules, and business logic
 * constraints from the original COBOL/BMS implementations.
 * 
 * Implements card number validation with Luhn algorithm support and 16-digit format
 * validation to ensure exact compatibility with the original CARDDAT VSAM file structure.
 * 
 * @author Blitzy Platform - Enterprise-grade transformation agent
 * @version 1.0.0
 * @since 2024-01-01
 */

import { FormFieldAttributes, BaseScreenData } from './CommonTypes';
import { AccountId } from './AccountTypes';
import { FormValidationSchema } from './ValidationTypes';

// ===================================================================
// CARD TYPE DEFINITIONS
// ===================================================================

/**
 * Card Number Type - Maps to BMS CARDSID field (16 characters)
 * 
 * Represents 16-digit credit card numbers with Luhn algorithm validation.
 * Maps to CARDSIDI field in COCRDLI, COCRDSL, and COCRDUP copybooks.
 * 
 * BMS Definition: CARDSIDI PIC X(16) (COCRDLI line 72)
 * PICIN Pattern: '9999999999999999' for 16-digit numeric validation
 * 
 * Validation Requirements:
 * - Exactly 16 digits
 * - Luhn algorithm checksum validation
 * - No spaces or special characters
 */
export type CardNumber = string;

/**
 * Card Status Type - Maps to BMS CRDSTCD field (1 character)
 * 
 * Represents card active status with Y/N validation.
 * Maps to CRDSTCDI field in COCRDSL and COCRDUP copybooks.
 * 
 * BMS Definition: CRDSTCDI PIC X(1) (COCRDSL line 78)
 * Valid Values: 'Y' (Active), 'N' (Inactive)
 */
export type CardStatus = 'Y' | 'N';

// ===================================================================
// HELPER INTERFACE DEFINITIONS
// ===================================================================

/**
 * Expiry Date Interface - Structured date representation for card expiry
 * 
 * Represents card expiry date fields split into month/year/day components
 * matching the BMS update screen date input pattern.
 * 
 * Maps to COCRDUP expiry date fields:
 * - EXPMON (Month component - 2 digits)
 * - EXPYEAR (Year component - 4 digits)
 * - EXPDAY (Day component - 2 digits, system generated)
 */
export interface ExpiryDate {
  /**
   * Month component (2 digits) - Maps to BMS EXPMON field
   * BMS Definition: EXPMONI PIC X(2) with JUSTIFY=(RIGHT)
   * Valid range: 01-12
   */
  month: string;
  
  /**
   * Year component (4 digits) - Maps to BMS EXPYEAR field
   * BMS Definition: EXPYEARI PIC X(4) with JUSTIFY=(RIGHT)
   * Valid range: current year to current year + 10
   */
  year: string;
  
  /**
   * Day component (2 digits) - Maps to BMS EXPDAY field
   * BMS Definition: EXPDAYI PIC X(2) - System generated (usually last day of month)
   * Valid range: 01-31 (depends on month)
   */
  day: string;
  
  /**
   * Validation flag - Whether the expiry date is valid
   * Indicates if the date is properly formatted and not expired
   */
  isValid: boolean;
  
  /**
   * Expiry flag - Whether the card is expired
   * Indicates if the expiry date is in the past
   */
  isExpired: boolean;
}

/**
 * Card Row Data Interface - Individual card row in the list screen
 * 
 * Represents a single row of card data in the COCRDLI card list screen.
 * Maps to the repeating card row structure (CRDSEL1-7, ACCTNO1-7, etc.).
 * 
 * Based on COCRDLI.bms rows 140-323 field definitions.
 */
export interface CardRowData {
  /**
   * Selection field - Maps to BMS CRDSEL fields
   * BMS Definition: CRDSEL1I PIC X(1) (COCRDLI line 78)
   * User selection indicator for card operations
   */
  selection: string;
  
  /**
   * Account number - Maps to BMS ACCTNO fields
   * BMS Definition: ACCTNO1I PIC X(11) (COCRDLI line 84)
   * 11-digit account number associated with the card
   */
  accountNumber: string;
  
  /**
   * Card number - Maps to BMS CRDNUM fields
   * BMS Definition: CRDNUM1I PIC X(16) (COCRDLI line 90)
   * 16-digit credit card number
   */
  cardNumber: CardNumber;
  
  /**
   * Card status - Maps to BMS CRDSTS fields
   * BMS Definition: CRDSTS1I PIC X(1) (COCRDLI line 96)
   * Card active status (Y/N)
   */
  cardStatus: CardStatus;
  
  /**
   * Selection state - Whether this row is currently selected
   * UI state tracking for row selection operations
   */
  isSelected: boolean;
  
  /**
   * Visibility state - Whether this row is visible in the current view
   * UI state tracking for pagination and filtering
   */
  isVisible: boolean;
}

// ===================================================================
// MAIN SCREEN INTERFACES
// ===================================================================

/**
 * Card List Data Interface - COCRDLI Screen Implementation
 * 
 * Represents the complete data structure for the card list screen including
 * pagination, search criteria, and card row data. Maps directly to the
 * COCRDLI.bms structure with all field attributes preserved.
 * 
 * Based on COCRDLI.bms complete field layout (lines 17-561).
 */
export interface CardListData {
  /**
   * Base screen data - Common header fields across all screens
   * Includes transaction name, program name, date, time, and titles
   */
  baseScreenData: BaseScreenData;
  
  /**
   * Page number - Maps to BMS PAGENO field
   * BMS Definition: PAGENOI PIC X(3) (COCRDLI line 60)
   * Current page number for pagination display
   */
  pageNumber: string;
  
  /**
   * Account ID search criteria - Maps to BMS ACCTSID field
   * BMS Definition: ACCTSIDI PIC X(11) (COCRDLI line 66)
   * Account number filter for card list display
   */
  accountId: AccountId;
  
  /**
   * Card number search criteria - Maps to BMS CARDSID field
   * BMS Definition: CARDSIDI PIC X(16) (COCRDLI line 72)
   * Card number filter for card list display
   */
  cardNumber: CardNumber;
  
  /**
   * Card rows data - Array of card row structures
   * Maps to COCRDLI.bms rows 1-7 (CRDSEL1-7, ACCTNO1-7, etc.)
   * Maximum 7 rows per page as per original BMS design
   */
  cardRows: CardRowData[];
  
  /**
   * Total card count - Total number of cards matching criteria
   * Used for pagination calculations and display
   */
  totalCards: number;
  
  /**
   * More pages indicator - Whether additional pages exist
   * Controls forward navigation availability
   */
  hasMorePages: boolean;
  
  /**
   * Selected cards - Array of selected card numbers
   * Tracks user selections for batch operations
   */
  selectedCards: CardNumber[];
  
  /**
   * Error message - Maps to BMS ERRMSG field
   * BMS Definition: ERRMSGI PIC X(78) (COCRDLI line 288)
   * Error message text for display
   */
  errorMessage: string;
  
  /**
   * Field attributes - BMS field attribute mappings
   * Maps all COCRDLI field attributes for UI rendering
   */
  fieldAttributes: Record<string, FormFieldAttributes>;
}

/**
 * Card Search Criteria Interface - COCRDSL Screen Implementation
 * 
 * Represents the complete data structure for the card search screen including
 * search criteria and card detail display. Maps directly to the COCRDSL.bms
 * structure with all field attributes preserved.
 * 
 * Based on COCRDSL.bms complete field layout (lines 17-158).
 */
export interface CardSearchCriteria {
  /**
   * Base screen data - Common header fields across all screens
   * Includes transaction name, program name, date, time, and titles
   */
  baseScreenData: BaseScreenData;
  
  /**
   * Account ID search criteria - Maps to BMS ACCTSID field
   * BMS Definition: ACCTSIDI PIC X(11) (COCRDSL line 60)
   * Account number for card search
   */
  accountId: AccountId;
  
  /**
   * Card number search criteria - Maps to BMS CARDSID field
   * BMS Definition: CARDSIDI PIC X(16) (COCRDSL line 66)
   * Card number for card search
   */
  cardNumber: CardNumber;
  
  /**
   * Card details - Found card information
   * Populated when search criteria match an existing card
   */
  cardDetails: CardDetailsData;
  
  /**
   * Error message - Maps to BMS ERRMSG field
   * BMS Definition: ERRMSGI PIC X(80) (COCRDSL line 147)
   * Error message text for display
   */
  errorMessage: string;
  
  /**
   * Card found indicator - Whether search found a matching card
   * Controls card details display and form behavior
   */
  isCardFound: boolean;
  
  /**
   * Read-only mode - Whether card details are read-only
   * Controls field protection and input capabilities
   */
  readOnlyMode: boolean;
  
  /**
   * Field attributes - BMS field attribute mappings
   * Maps all COCRDSL field attributes for UI rendering
   */
  fieldAttributes: Record<string, FormFieldAttributes>;
}

/**
 * Card Update Form Data Interface - COCRDUP Screen Implementation
 * 
 * Represents the complete data structure for the card update screen including
 * all editable fields and validation state. Maps directly to the COCRDUP.bms
 * structure with all field attributes preserved.
 * 
 * Based on COCRDUP.bms complete field layout (lines 17-169).
 */
export interface CardUpdateFormData {
  /**
   * Base screen data - Common header fields across all screens
   * Includes transaction name, program name, date, time, and titles
   */
  baseScreenData: BaseScreenData;
  
  /**
   * Account ID - Maps to BMS ACCTSID field (protected)
   * BMS Definition: ACCTSIDI PIC X(11) (COCRDUP line 60)
   * Account number associated with the card (read-only)
   */
  accountId: AccountId;
  
  /**
   * Card number - Maps to BMS CARDSID field (unprotected)
   * BMS Definition: CARDSIDI PIC X(16) (COCRDUP line 66)
   * Card number for update operations
   */
  cardNumber: CardNumber;
  
  /**
   * Card name - Maps to BMS CRDNAME field (unprotected)
   * BMS Definition: CRDNAMEI PIC X(50) (COCRDUP line 72)
   * Name on card for update operations
   */
  cardName: string;
  
  /**
   * Card status - Maps to BMS CRDSTCD field (unprotected)
   * BMS Definition: CRDSTCDI PIC X(1) (COCRDUP line 78)
   * Card active status (Y/N)
   */
  cardStatus: CardStatus;
  
  /**
   * Expiry month - Maps to BMS EXPMON field (unprotected)
   * BMS Definition: EXPMONI PIC X(2) (COCRDUP line 84)
   * Card expiry month (01-12)
   */
  expiryMonth: string;
  
  /**
   * Expiry year - Maps to BMS EXPYEAR field (unprotected)
   * BMS Definition: EXPYEARI PIC X(4) (COCRDUP line 90)
   * Card expiry year (4 digits)
   */
  expiryYear: string;
  
  /**
   * Expiry day - Maps to BMS EXPDAY field (protected)
   * BMS Definition: EXPDAYI PIC X(2) (COCRDUP line 96)
   * Card expiry day (system generated)
   */
  expiryDay: string;
  
  /**
   * Confirmation flag - User confirmation for update operations
   * Controls whether the user has confirmed the update
   */
  confirmation: boolean;
  
  /**
   * Error message - Maps to BMS ERRMSG field
   * BMS Definition: ERRMSGI PIC X(80) (COCRDUP line 108)
   * Error message text for display
   */
  errorMessage: string;
  
  /**
   * Validation result - Form validation state
   * Contains validation results for all form fields
   */
  validationResult: any; // ValidationResult from ValidationTypes
  
  /**
   * Optimistic lock version - Version control for concurrent updates
   * Prevents lost update problems in multi-user scenarios
   */
  optimisticLockVersion: number;
  
  /**
   * Audit trail - Change tracking information
   * Tracks who made changes and when for audit purposes
   */
  auditTrail: CardAuditTrail;
  
  /**
   * Field attributes - BMS field attribute mappings
   * Maps all COCRDUP field attributes for UI rendering
   */
  fieldAttributes: Record<string, FormFieldAttributes>;
}

/**
 * Card Details Data Interface - Complete card information
 * 
 * Represents comprehensive card details including validation state and
 * security information. Used across all card management screens for
 * complete card data representation.
 * 
 * Combines data from all three BMS screens for complete card profile.
 */
export interface CardDetailsData {
  /**
   * Card number - 16-digit credit card number
   * Primary identifier for card records
   */
  cardNumber: CardNumber;
  
  /**
   * Account ID - Associated account number
   * Links card to account for relationship management
   */
  accountId: AccountId;
  
  /**
   * Card name - Name on the card
   * Cardholder name for identification
   */
  cardName: string;
  
  /**
   * Card status - Active status indicator
   * Current card status (Y/N)
   */
  cardStatus: CardStatus;
  
  /**
   * Expiry date - Card expiration information
   * Structured expiry date with validation
   */
  expiryDate: ExpiryDate;
  
  /**
   * CVV code - Card verification value
   * Security code for card verification (not stored in BMS)
   */
  cvvCode: string;
  
  /**
   * Active flag - Whether card is currently active
   * Computed from card status and expiry date
   */
  isActive: boolean;
  
  /**
   * Last used date - Most recent card usage
   * Tracking information for card activity
   */
  lastUsedDate: Date;
  
  /**
   * Luhn validation - Card number validation result
   * Luhn algorithm validation for card number integrity
   */
  luhnValidation: boolean;
  
  /**
   * Security validation - Overall security validation result
   * Comprehensive security validation including all checks
   */
  securityValidation: boolean;
}

// ===================================================================
// VALIDATION SCHEMA INTERFACES
// ===================================================================

/**
 * Card Validation Schema Interface - Comprehensive card validation
 * 
 * Implements complete validation schema for all card management operations
 * including field-level validation, cross-field validation, and business
 * rule validation patterns.
 * 
 * Maps BMS VALIDN, PICIN, and MUSTFILL parameters to modern validation.
 */
export interface CardValidationSchema {
  /**
   * Card number validation - 16-digit format and Luhn algorithm
   * Validates card number format, length, and checksum
   */
  cardNumberValidation: FormValidationSchema<{ cardNumber: CardNumber }>;
  
  /**
   * Account ID validation - 11-digit account number format
   * Validates account number format and existence
   */
  accountIdValidation: FormValidationSchema<{ accountId: AccountId }>;
  
  /**
   * Card name validation - Name on card format and length
   * Validates cardholder name format and character restrictions
   */
  cardNameValidation: FormValidationSchema<{ cardName: string }>;
  
  /**
   * Card status validation - Y/N status validation
   * Validates card status format and business rules
   */
  cardStatusValidation: FormValidationSchema<{ cardStatus: CardStatus }>;
  
  /**
   * Expiry date validation - Date format and future date validation
   * Validates expiry date format and business rules
   */
  expiryDateValidation: FormValidationSchema<{ expiryDate: ExpiryDate }>;
  
  /**
   * CVV validation - Security code format validation
   * Validates CVV format and length requirements
   */
  cvvValidation: FormValidationSchema<{ cvvCode: string }>;
  
  /**
   * Luhn validation - Card number checksum validation
   * Implements Luhn algorithm for card number integrity
   */
  luhnValidation: FormValidationSchema<{ cardNumber: CardNumber }>;
  
  /**
   * Cross-field validation - Multi-field business rule validation
   * Validates relationships between card fields and business rules
   */
  crossFieldValidation: FormValidationSchema<CardUpdateFormData>;
}

/**
 * Card Audit Trail Interface - Change tracking for card operations
 * 
 * Implements comprehensive audit trail functionality for card management
 * operations including create, update, and delete operations with full
 * change history tracking.
 * 
 * Provides audit compliance equivalent to CICS audit trail functionality.
 */
export interface CardAuditTrail {
  /**
   * Created by - User who created the card record
   * Maps to CICS user ID for audit trail tracking
   */
  createdBy: string;
  
  /**
   * Created date - When the card record was created
   * Timestamp of initial card creation
   */
  createdDate: Date;
  
  /**
   * Last modified by - User who last modified the card record
   * Maps to CICS user ID for change tracking
   */
  lastModifiedBy: string;
  
  /**
   * Last modified date - When the card record was last modified
   * Timestamp of most recent card modification
   */
  lastModifiedDate: Date;
  
  /**
   * Version number - Optimistic locking version
   * Incremented with each update for concurrency control
   */
  version: number;
  
  /**
   * Change history - Complete history of card changes
   * Array of all changes made to the card record
   */
  changeHistory: Array<{
    changeDate: Date;
    changedBy: string;
    changeType: 'CREATE' | 'UPDATE' | 'DELETE' | 'STATUS_CHANGE';
    oldValues: Partial<CardDetailsData>;
    newValues: Partial<CardDetailsData>;
    changeReason: string;
  }>;
}