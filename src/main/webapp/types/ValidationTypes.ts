/**
 * CardDemo - Validation TypeScript Type Definitions
 * 
 * This file contains comprehensive TypeScript interface definitions for form validation
 * schemas including BMS field attribute mapping, Yup validation rules, cross-field 
 * validation, and type-safe form validation patterns for all 18 React components.
 * 
 * Maps BMS VALIDN, PICIN, and MUSTFILL parameters to modern React Hook Form validation
 * while preserving exact business logic and validation behavior from original COBOL/BMS.
 * 
 * Implements type-safe validation patterns that maintain functional equivalence with
 * the original mainframe validation rules while providing enhanced user experience.
 */

import { ValidationError } from 'yup';
import { RegisterOptions } from 'react-hook-form';
import { FocusEvent } from 'react';
import { FormFieldAttributes } from './CommonTypes';

// ===================================================================
// BMS ATTRIBUTE TYPE DEFINITIONS
// ===================================================================

/**
 * BMS Attribute Type - Maps BMS ATTRB parameter values to validation contexts
 * Extends CommonTypes.BmsAttributeType for validation-specific usage
 */
export type BmsAttributeType = 
  | 'ASKIP'    // Auto-skip protected field - no validation needed
  | 'UNPROT'   // Unprotected field - full validation applies
  | 'PROT'     // Protected field - read-only, no input validation
  | 'MUSTFILL' // Required field validation (maps to BMS VALIDN=MUSTFILL)
  | 'MUSTENTER'// Must enter validation - field required for processing
  | 'NUMERIC'  // Numeric-only input validation (maps to BMS NUM attribute)
  | 'ALPHA'    // Alphabetic-only input validation
  | 'ALNUM'    // Alphanumeric input validation
  | 'PICIN'    // Picture format validation (maps to BMS PICIN parameter)
  | 'FSET'     // Field set for change detection
  | 'IC';      // Initial cursor - validation applies immediately

// ===================================================================
// CORE VALIDATION INTERFACES
// ===================================================================

/**
 * Field Validation Rules Interface
 * 
 * Maps BMS field definition attributes to React Hook Form validation rules.
 * Preserves exact validation behavior from original BMS DFHMDF parameters
 * including VALIDN, PICIN, LENGTH, and ATTRB combinations.
 * 
 * Based on analysis of COSGN00.bms, COACTVW.bms, and COCRDUP.bms field definitions.
 */
export interface FieldValidationRules {
  /**
   * BMS attribute type - Maps to BMS ATTRB parameter
   * Determines the primary validation behavior for the field
   */
  bmsAttribute: BmsAttributeType[];
  
  /**
   * PICIN pattern - Maps to BMS PICIN parameter
   * Defines input format mask for data entry validation
   * Example: '99999999999' for 11-digit account numbers
   */
  picinPattern?: string;
  
  /**
   * Must fill indicator - Maps to BMS VALIDN=(MUSTFILL)
   * Indicates whether field value is mandatory for form submission
   */
  mustfill: boolean;
  
  /**
   * Field length constraints - Maps to BMS LENGTH parameter
   * Defines minimum and maximum character length for input validation
   */
  length: {
    min?: number;
    max: number;
  };
  
  /**
   * Custom validation function - Implements complex business rules
   * Equivalent to COBOL validation paragraphs for field-specific logic
   */
  validationFn?: (value: any, allValues?: Record<string, any>) => string | undefined;
  
  /**
   * Error message - Custom error message for validation failures
   * Displayed when validation rules are not met, preserving original message format
   */
  errorMessage: string;
}

/**
 * Form Validation Schema Generic Interface
 * 
 * Provides type-safe form validation across all 18 React components.
 * Generic type T represents the form data structure for each specific screen.
 * 
 * Implements comprehensive validation patterns that maintain CICS transaction
 * validation behavior while providing modern React Hook Form integration.
 */
export interface FormValidationSchema<T = Record<string, any>> {
  /**
   * Validation schema definition - Yup schema object for form validation
   * Contains all field validation rules in structured format
   */
  schema: any; // Yup.ObjectSchema<T> - avoiding direct Yup import for flexibility
  
  /**
   * Individual field validation - Validates single field value
   * Replicates BMS field-level validation with immediate feedback
   */
  validateField: (fieldName: keyof T, value: any, formData?: Partial<T>) => ValidationResult;
  
  /**
   * Full form validation - Validates all form fields collectively
   * Implements cross-field validation and business rule checking
   */
  validateForm: (formData: T) => ValidationResult;
  
  /**
   * Get field error - Retrieves specific field error message
   * Provides formatted error message for display in UI components
   */
  getFieldError: (fieldName: keyof T, errors: ValidationError[]) => string | undefined;
  
  /**
   * Reset validation - Clears all validation state
   * Resets form to initial state equivalent to CICS screen refresh
   */
  resetValidation: () => void;
}

/**
 * Cross-Field Validation Rules Interface
 * 
 * Defines complex validation rules that span multiple fields, implementing
 * business logic equivalent to COBOL validation paragraphs that check
 * field relationships and data consistency.
 * 
 * Examples: state/ZIP code consistency, account-card linkage, date range validation.
 */
export interface CrossFieldValidationRules {
  /**
   * Primary fields - Main fields involved in the cross-validation
   * Array of field names that trigger the validation when changed
   */
  fields: string[];
  
  /**
   * Cross-validation function - Implements complex business rule validation
   * Returns error message if validation fails, undefined if successful
   */
  validationFn: (values: Record<string, any>) => string | undefined;
  
  /**
   * Error message - Cross-field validation error message
   * Displayed when business rule validation fails
   */
  errorMessage: string;
  
  /**
   * Dependent fields - Additional fields that affect validation outcome
   * Fields that don't trigger validation but influence the result
   */
  dependentFields?: string[];
}

/**
 * Validation Result Interface
 * 
 * Standardized result structure for all validation operations.
 * Provides comprehensive validation outcome information for UI display
 * and error handling consistent across all React components.
 */
export interface ValidationResult {
  /**
   * Overall validation status - Whether validation passed
   * True if all validation rules are satisfied
   */
  isValid: boolean;
  
  /**
   * General errors - Form-level or system-level errors
   * Errors that don't relate to specific fields
   */
  errors: string[];
  
  /**
   * Field-specific errors - Mapping of field names to error messages
   * Enables precise error highlighting and user guidance
   */
  fieldErrors: Record<string, string>;
  
  /**
   * Warning messages - Non-blocking validation warnings
   * Informational messages that don't prevent form submission
   */
  warnings?: string[];
}

// ===================================================================
// FUNCTION TYPE DEFINITIONS
// ===================================================================

/**
 * Field Validator Function Type
 * 
 * Type definition for custom field validation functions that implement
 * COBOL business logic equivalent validation rules.
 * 
 * @param value - Current field value being validated
 * @param event - Optional focus event for blur validation
 * @param formData - Complete form data for cross-field validation
 * @returns Error message if validation fails, undefined if successful
 */
export type FieldValidatorFunction = (
  value: any,
  event?: FocusEvent<HTMLInputElement>,
  formData?: Record<string, any>
) => string | undefined;

// ===================================================================
// SPECIALIZED VALIDATION TYPES
// ===================================================================

/**
 * BMS PICIN Pattern Types - Common validation patterns from BMS definitions
 * Extracted from analysis of BMS field PICIN parameters across all mapsets
 */
export type PicinPatternType = 
  | '99999999999'      // 11-digit account numbers (ACCTSID in COACTVW)
  | '9999999999999999' // 16-digit card numbers (CRDNUM in COCRDUP)
  | '999999999'        // 9-digit customer IDs
  | '99/99/9999'       // Date format MM/DD/YYYY
  | '99/9999'          // Expiry date format MM/YYYY
  | '999'              // CVV codes
  | '99999'            // ZIP codes
  | '999.99'           // Currency amounts
  | 'X(50)'            // Text fields up to 50 characters
  | 'X(8)'             // Short text fields (user IDs, etc.)
  | 'A'                // Single character codes (Y/N, status codes)
  | '999-99-9999';     // SSN format

/**
 * Validation Trigger Types - When validation should execute
 * Maps to BMS field behavior and React Hook Form validation modes
 */
export type ValidationTriggerType = 
  | 'onChange'   // Validate on every keystroke
  | 'onBlur'     // Validate when field loses focus (BMS standard behavior)
  | 'onSubmit'   // Validate only on form submission
  | 'onTouched'  // Validate after field has been touched
  | 'immediate'; // Validate immediately (for IC fields)

/**
 * BMS Field Protection Status - Maps to BMS ATTRB field protection
 * Determines validation applicability and user interaction capability
 */
export type FieldProtectionStatus = 
  | 'unprotected'  // UNPROT - full validation applies
  | 'protected'    // PROT - read-only, no validation
  | 'autoskip'     // ASKIP - display only, skip in tab order
  | 'dark';        // DRK - hidden field (password, etc.)

// ===================================================================
// VALIDATION SCHEMA BUILDERS
// ===================================================================

/**
 * Field Validation Schema Builder Interface
 * 
 * Provides methods to construct validation schemas from BMS field attributes.
 * Enables systematic conversion of BMS DFHMDF parameters to React validation rules.
 */
export interface FieldValidationSchemaBuilder {
  /**
   * Build from BMS attributes - Creates validation schema from FormFieldAttributes
   * Converts BMS field definition to React Hook Form validation rules
   */
  buildFromBmsAttributes: (fieldAttributes: FormFieldAttributes) => FieldValidationRules;
  
  /**
   * Apply PICIN pattern - Converts BMS PICIN to validation pattern
   * Creates appropriate input mask and validation regex
   */
  applyPicinPattern: (pattern: PicinPatternType) => RegisterOptions;
  
  /**
   * Apply MUSTFILL validation - Implements required field validation
   * Equivalent to BMS VALIDN=(MUSTFILL) parameter
   */
  applyMustfillValidation: (required: boolean) => RegisterOptions;
  
  /**
   * Apply length constraints - Implements field length validation
   * Maps BMS LENGTH parameter to min/max length validation
   */
  applyLengthConstraints: (min: number, max: number) => RegisterOptions;
  
  /**
   * Apply numeric validation - Implements numeric-only input validation
   * Equivalent to BMS NUM attribute with appropriate pattern matching
   */
  applyNumericValidation: (allowDecimal?: boolean, precision?: number) => RegisterOptions;
}

/**
 * Cross-Field Validation Schema Builder Interface
 * 
 * Provides methods to construct complex validation schemas that span multiple fields.
 * Implements business logic equivalent to COBOL validation paragraphs.
 */
export interface CrossFieldValidationSchemaBuilder {
  /**
   * Add state-ZIP validation - Validates state and ZIP code consistency
   * Implements business rule checking for valid state/ZIP combinations
   */
  addStateZipValidation: (stateField: string, zipField: string) => CrossFieldValidationRules;
  
  /**
   * Add account-card validation - Validates account and card number linkage
   * Ensures card numbers are associated with correct account IDs
   */
  addAccountCardValidation: (accountField: string, cardField: string) => CrossFieldValidationRules;
  
  /**
   * Add date range validation - Validates date range consistency
   * Ensures start dates are before end dates with appropriate business rules
   */
  addDateRangeValidation: (startDateField: string, endDateField: string) => CrossFieldValidationRules;
  
  /**
   * Add conditional required validation - Implements conditional field requirements
   * Fields become required based on values in other fields
   */
  addConditionalRequiredValidation: (
    triggerField: string, 
    triggerValue: any, 
    requiredFields: string[]
  ) => CrossFieldValidationRules;
}