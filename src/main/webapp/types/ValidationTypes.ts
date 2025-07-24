/**
 * ValidationTypes.ts
 * 
 * TypeScript interface definitions for form validation schemas including BMS field attribute mapping,
 * Yup validation rules, cross-field validation, and type-safe form validation patterns for all 18
 * React components transformed from BMS mapsets.
 * 
 * This file provides comprehensive validation type definitions that maintain exact functional
 * equivalence with original COBOL BMS field validation while enabling modern React Hook Form
 * integration with Yup schema validation for enhanced user experience.
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0
 */

import { ValidationError } from 'yup';
import { RegisterOptions } from 'react-hook-form';
import { FocusEvent } from 'react';
import { FormFieldAttributes } from './CommonTypes';

/**
 * BMS Attribute Type Definition
 * Union type representing all possible BMS DFHMDF ATTRB values
 * Used for mapping BMS field attributes to React Hook Form validation rules
 */
export type BmsAttributeType = 
  | 'ASKIP'    // Auto-skip protected field (read-only display)
  | 'UNPROT'   // Unprotected field allowing user input
  | 'PROT'     // Protected field preventing user modification
  | 'NORM'     // Normal intensity for standard display
  | 'BRT'      // Bright intensity for emphasis
  | 'FSET'     // Field set attribute for change detection
  | 'IC'       // Initial cursor positioning
  | 'DRK'      // Dark attribute for hidden fields (passwords)
  | 'NUM'      // Numeric-only input validation
  | 'MUSTFILL'; // Required field validation (from BMS VALIDN attribute)

/**
 * Field Validator Function Type
 * Defines the signature for custom field validation functions
 * Used in FieldValidationRules for complex business logic validation
 */
export type FieldValidatorFunction = (
  value: any,
  formData?: Record<string, any>
) => boolean | string | Promise<boolean | string>;

/**
 * Field Validation Rules Interface
 * Maps BMS field attributes to React Hook Form validation configuration
 * Provides comprehensive field-level validation rule definition
 * 
 * Based on analysis of BMS files:
 * - COSGN00.bms: USERID (8 char, UNPROT, IC), PASSWD (8 char, DRK, UNPROT)
 * - COACTVW.bms: ACCTSID (11 char, UNPROT, PICIN='99999999999', MUSTFILL)
 * - COCRDUP.bms: CARDSID (16 char, UNPROT), CRDNAME (50 char, UNPROT)
 */
export interface FieldValidationRules {
  /** BMS attribute bytes controlling field behavior and validation */
  bmsAttribute: BmsAttributeType | BmsAttributeType[];
  
  /** Input picture pattern from BMS PICIN attribute (e.g., '99999999999' for numeric fields) */
  picinPattern?: string | RegExp;
  
  /** Indicates if field is required (from BMS VALIDN=MUSTFILL attribute) */
  mustfill: boolean;
  
  /** Field length constraint from BMS LENGTH attribute */
  length: {
    /** Minimum field length (typically 1 for required fields) */
    min?: number;
    /** Maximum field length (from BMS LENGTH attribute) */
    max: number;
  };
  
  /** Custom validation function for complex business rules */
  validationFn?: FieldValidatorFunction;
  
  /** Error message to display when validation fails */
  errorMessage: string;
}

/**
 * Validation Result Interface
 * Structured validation result containing field-level and form-level validation outcomes
 * Provides comprehensive validation feedback for React components
 */
export interface ValidationResult {
  /** Overall validation status */
  isValid: boolean;
  
  /** Collection of validation errors with field context */
  errors: ValidationError[];
  
  /** Field-specific error mapping for targeted error display */
  fieldErrors: Record<string, string>;
  
  /** Non-blocking validation warnings for user guidance */
  warnings: string[];
}

/**
 * Form Validation Schema Interface
 * Generic interface for type-safe form validation across all 18 React components
 * Integrates React Hook Form with Yup validation schemas
 * 
 * @template T - Form data type (e.g., LoginFormData, AccountViewData, CardUpdateData)
 */
export interface FormValidationSchema<T extends Record<string, any>> {
  /** Yup validation schema for the form data type */
  schema: any; // Yup.ObjectSchema<T> would be ideal but causes circular dependency
  
  /** 
   * Validates a single field value with business rules
   * @param fieldName - Name of the field being validated
   * @param value - Current field value
   * @param formData - Complete form data for cross-field validation
   * @returns Promise resolving to validation result
   */
  validateField(
    fieldName: keyof T,
    value: any,
    formData?: Partial<T>
  ): Promise<ValidationResult>;
  
  /** 
   * Validates the entire form with all business rules
   * @param formData - Complete form data object
   * @returns Promise resolving to comprehensive validation result
   */
  validateForm(formData: T): Promise<ValidationResult>;
  
  /** 
   * Gets field-specific error message for display
   * @param fieldName - Name of the field
   * @param errors - Validation errors from form validation
   * @returns Error message string or undefined if no error
   */
  getFieldError(fieldName: keyof T, errors: ValidationError[]): string | undefined;
  
  /** 
   * Resets validation state for the form
   * Used when form is reset or reinitialized
   */
  resetValidation(): void;
}

/**
 * Cross-Field Validation Rules Interface
 * Defines validation rules that span multiple fields (e.g., state/ZIP consistency)
 * Implements complex business validation requirements from original COBOL logic
 * 
 * Examples from BMS analysis:
 * - State and ZIP code consistency validation
 * - Account and card number cross-reference validation
 * - Date range validation (start date before end date)
 * - Credit limit vs. account balance validation
 */
export interface CrossFieldValidationRules {
  /** Array of field names involved in the cross-field validation */
  fields: string[];
  
  /** 
   * Validation function for cross-field business rules
   * @param values - Object containing values for all fields in the validation rule
   * @param formData - Complete form data for additional context
   * @returns Validation result (true for valid, string for error message)
   */
  validationFn: (
    values: Record<string, any>,
    formData?: Record<string, any>
  ) => boolean | string | Promise<boolean | string>;
  
  /** Error message to display when cross-field validation fails */
  errorMessage: string;
  
  /** 
   * Array of dependent field names that should trigger re-validation
   * when their values change
   */
  dependentFields: string[];
}

/**
 * Field Validation Context Interface
 * Provides context information for field validation functions
 * Enables validation functions to access form state and BMS attributes
 */
export interface FieldValidationContext {
  /** BMS field attributes from the original map definition */
  fieldAttributes: FormFieldAttributes;
  
  /** Current form data for cross-field validation context */
  formData: Record<string, any>;
  
  /** Field focus event for blur validation triggers */
  focusEvent?: FocusEvent<HTMLInputElement>;
  
  /** Indicates if validation is triggered by user interaction or programmatic change */
  isUserTriggered: boolean;
}

/**
 * BMS Picture Input Validation Interface
 * Handles BMS PICIN attribute validation patterns
 * Converts COBOL picture clauses to JavaScript validation logic
 */
export interface BmsPictureValidation {
  /** Original BMS PICIN pattern (e.g., '99999999999', 'XXXXXXXXXXXX') */
  originalPattern: string;
  
  /** JavaScript RegExp equivalent of the BMS pattern */
  regexPattern: RegExp;
  
  /** Input mask for user guidance (e.g., '###-##-####' for SSN) */
  inputMask?: string;
  
  /** Validation function that enforces the picture pattern */
  validate: (value: string) => boolean;
  
  /** Error message when picture validation fails */
  errorMessage: string;
}

/**
 * React Hook Form Integration Options
 * Maps BMS validation rules to React Hook Form RegisterOptions
 * Enables seamless integration with React Hook Form validation
 */
export interface ReactHookFormOptions {
  /** React Hook Form register options derived from BMS attributes */
  registerOptions: RegisterOptions;
  
  /** Field validation rules configuration */
  validationRules: FieldValidationRules;
  
  /** Cross-field validation dependencies */
  crossFieldRules?: CrossFieldValidationRules[];
  
  /** BMS picture input validation configuration */
  pictureValidation?: BmsPictureValidation;
}

/**
 * Validation Error Context Interface
 * Provides detailed context for validation errors
 * Enables specific error handling and user guidance
 */
export interface ValidationErrorContext {
  /** Field name where the validation error occurred */
  fieldName: string;
  
  /** Field value that failed validation */
  fieldValue: any;
  
  /** BMS attribute that triggered the validation failure */
  bmsAttribute?: BmsAttributeType;
  
  /** Validation rule type that failed */
  ruleType: 'required' | 'pattern' | 'length' | 'custom' | 'cross-field';
  
  /** Additional context data for error handling */
  context?: Record<string, any>;
}

/**
 * Form Validation State Interface
 * Manages form-level validation state for React components
 * Tracks validation status across all form fields
 */
export interface FormValidationState {
  /** Overall form validation status */
  isValid: boolean;
  
  /** Individual field validation states */
  fieldStates: Record<string, {
    isValid: boolean;
    error?: string;
    warning?: string;
    hasBeenValidated: boolean;
  }>;
  
  /** Cross-field validation results */
  crossFieldErrors: string[];
  
  /** Indicates if form has been submitted (for validation display control) */
  hasBeenSubmitted: boolean;
  
  /** Timestamp of last validation execution */
  lastValidated?: Date;
}

/**
 * BMS Validation Rule Factory Interface
 * Factory for creating validation rules from BMS field definitions
 * Provides consistent validation rule generation across all screens
 */
export interface BmsValidationRuleFactory {
  /** 
   * Creates field validation rules from BMS field attributes
   * @param fieldAttributes - BMS field attributes from the original map
   * @returns Configured field validation rules
   */
  createFieldRules(fieldAttributes: FormFieldAttributes): FieldValidationRules;
  
  /** 
   * Creates React Hook Form options from BMS attributes
   * @param fieldAttributes - BMS field attributes
   * @param customRules - Additional custom validation rules
   * @returns React Hook Form register options
   */
  createReactHookFormOptions(
    fieldAttributes: FormFieldAttributes,
    customRules?: Partial<FieldValidationRules>
  ): ReactHookFormOptions;
  
  /** 
   * Creates cross-field validation rules for complex business logic
   * @param fields - Array of field names involved in cross-field validation
   * @param validationLogic - Business logic validation function
   * @returns Cross-field validation configuration
   */
  createCrossFieldRules(
    fields: string[],
    validationLogic: (values: Record<string, any>) => boolean | string
  ): CrossFieldValidationRules;
}