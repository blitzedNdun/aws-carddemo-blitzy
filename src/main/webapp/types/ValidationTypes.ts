/**
 * ValidationTypes.ts
 * 
 * TypeScript interface definitions for form validation schemas including BMS field attribute
 * mapping, Yup validation rules, cross-field validation, and type-safe form validation 
 * patterns for all 18 React components transformed from BMS mapsets.
 * 
 * This file provides comprehensive validation type definitions that replicate BMS validation
 * behavior (MUSTFILL, PICIN, length constraints) while enabling modern React Hook Form
 * integration with Yup schema validation for enhanced user experience and type safety.
 * 
 * Key Features:
 * - BMS attribute byte mapping to React Hook Form validation rules
 * - Type-safe validation schema definitions for all form fields
 * - Cross-field validation support for complex business rules
 * - Comprehensive error handling and message formatting
 * - Integration with Material-UI field components for validation display
 * 
 * Copyright (c) 2023 CardDemo Application
 * Technology transformation: COBOL/CICS/BMS → Java/Spring Boot/React
 */

import { ValidationError } from 'yup';
import { RegisterOptions } from 'react-hook-form';
import { FocusEvent } from 'react';
import { FormFieldAttributes } from './CommonTypes';

/**
 * BMS Attribute Type for validation rule mapping
 * 
 * Extends the base BmsAttributeType from CommonTypes to include validation-specific
 * attributes used in BMS field definitions. These map directly to React Hook Form
 * validation rules and Material-UI field properties.
 */
export type BmsAttributeType = 
  | 'ASKIP'    // Auto-skip protected field (read-only, no validation needed)
  | 'UNPROT'   // Unprotected field requiring validation
  | 'PROT'     // Protected field (disabled, no validation)
  | 'NUM'      // Numeric-only input validation
  | 'MUSTFILL' // Required field validation (equivalent to BMS VALIDN=MUSTFILL)
  | 'IC'       // Initial cursor positioning
  | 'BRT'      // Bright intensity for emphasis
  | 'NORM'     // Normal intensity for standard display
  | 'FSET'     // Field set attribute for change detection
  | 'DRK';     // Dark attribute for masked input (passwords)

/**
 * Field Validator Function Type
 * 
 * Type definition for custom validation functions that can be used in field
 * validation rules. Supports both synchronous and asynchronous validation
 * patterns for complex business rule validation.
 */
export type FieldValidatorFunction = (
  value: any,
  formData?: Record<string, any>
) => boolean | string | Promise<boolean | string>;

/**
 * Field Validation Rules Interface
 * 
 * Maps BMS field attributes to React Hook Form validation rules and Yup schema
 * validation patterns. This interface provides type-safe validation rule
 * definitions that preserve BMS validation behavior while enabling modern
 * client-side validation with enhanced user experience.
 */
export interface FieldValidationRules {
  /** 
   * BMS attribute byte controlling field behavior and validation requirements
   * Used to determine which validation rules to apply and field interaction behavior
   */
  bmsAttribute: BmsAttributeType;
  
  /**
   * PICIN pattern for input format validation (equivalent to BMS PICIN attribute)
   * Examples: '99999999999' for account numbers, 'XXXXXXXXXXXX' for names
   * Converted to regex patterns for React Hook Form validation
   */
  picinPattern?: string;
  
  /**
   * MUSTFILL validation requirement (equivalent to BMS VALIDN=MUSTFILL)
   * When true, field is required and must contain non-empty value
   */
  mustfill: boolean;
  
  /**
   * Field length constraint (equivalent to BMS LENGTH attribute)
   * Enforces maximum character length for input fields
   */
  length: {
    min?: number;
    max: number;
  };
  
  /**
   * Custom validation function for complex business rules
   * Supports both synchronous and asynchronous validation patterns
   */
  validationFn?: FieldValidatorFunction;
  
  /**
   * Error message template for validation failures
   * Supports parameterized messages with field name and value substitution
   */
  errorMessage: {
    required?: string;
    pattern?: string;
    minLength?: string;
    maxLength?: string;
    custom?: string;
  };
}

/**
 * Form Validation Schema Interface
 * 
 * Generic interface for type-safe form validation schemas that can be used
 * across all 18 React components. Provides comprehensive validation support
 * including field-level validation, form-level validation, and error handling.
 */
export interface FormValidationSchema<T extends Record<string, any>> {
  /**
   * Yup validation schema for the form data structure
   * Provides compile-time type checking and runtime validation
   */
  schema: any; // Yup schema object
  
  /**
   * Validate individual field with real-time feedback
   * Returns validation result with error details if validation fails
   */
  validateField: (
    fieldName: keyof T,
    value: any,
    formData?: Partial<T>
  ) => Promise<ValidationResult>;
  
  /**
   * Validate entire form data structure
   * Returns comprehensive validation result including all field errors
   */
  validateForm: (formData: T) => Promise<ValidationResult>;
  
  /**
   * Get field-specific error message
   * Returns formatted error message for display in Material-UI components
   */
  getFieldError: (
    fieldName: keyof T,
    errors: Record<string, ValidationError>
  ) => string | undefined;
  
  /**
   * Reset validation state for form or specific fields
   * Clears validation errors and resets form state
   */
  resetValidation: (fieldNames?: Array<keyof T>) => void;
}

/**
 * Cross-Field Validation Rules Interface
 * 
 * Supports complex validation scenarios where field values must be validated
 * against other fields in the form. Examples include state/ZIP code consistency,
 * date range validation, and account-card relationship validation.
 */
export interface CrossFieldValidationRules {
  /**
   * Primary fields involved in cross-field validation
   * These fields trigger the validation when their values change
   */
  fields: string[];
  
  /**
   * Validation function that receives multiple field values
   * Returns true if validation passes, string error message if fails
   */
  validationFn: (fieldValues: Record<string, any>) => boolean | string | Promise<boolean | string>;
  
  /**
   * Error message template for cross-field validation failures
   * Supports field name substitution and detailed error descriptions
   */
  errorMessage: string;
  
  /**
   * Dependent fields that should be re-validated when primary fields change
   * Supports cascading validation scenarios
   */
  dependentFields?: string[];
}

/**
 * Validation Result Interface
 * 
 * Standardized result structure for all validation operations providing
 * comprehensive error information and validation state details.
 */
export interface ValidationResult {
  /**
   * Overall validation result - true if all validations pass
   */
  isValid: boolean;
  
  /**
   * Collection of validation errors organized by error type
   * Provides structured error information for debugging and logging
   */
  errors: {
    fieldErrors: Record<string, ValidationError>;
    crossFieldErrors: Record<string, string>;
    formErrors: string[];
  };
  
  /**
   * Field-specific error messages for UI display
   * Formatted for direct use in Material-UI TextField helperText
   */
  fieldErrors: Record<string, string>;
  
  /**
   * Non-blocking validation warnings
   * Used for informational messages that don't prevent form submission
   */
  warnings?: Record<string, string>;
}

/**
 * BMS Field Attribute to React Hook Form Validation Mapping
 * 
 * Utility type that maps BMS field attributes to React Hook Form RegisterOptions
 * providing type-safe validation rule conversion from BMS definitions to modern
 * React form validation patterns.
 */
export type BmsToReactValidationMapping = {
  [K in BmsAttributeType]: {
    registerOptions: RegisterOptions;
    materialUIProps: Record<string, any>;
    validationTrigger: 'onChange' | 'onBlur' | 'onSubmit';
  };
};

/**
 * Field Validation Context Interface
 * 
 * Provides contextual information for field validation including form state,
 * user interaction patterns, and validation timing controls.
 */
export interface FieldValidationContext {
  /** Current form data for cross-field validation */
  formData: Record<string, any>;
  
  /** Field interaction state for validation timing */
  fieldState: {
    isTouched: boolean;
    isDirty: boolean;
    isFocused: boolean;
  };
  
  /** Validation trigger event information */
  triggerEvent: {
    type: 'change' | 'blur' | 'focus' | 'submit';
    target: {
      name: string;
      value: any;
    };
  };
  
  /** Previous validation result for comparison */
  previousValidation?: ValidationResult;
}

/**
 * Validation Rule Builder Interface
 * 
 * Fluent interface for building validation rules from BMS field attributes
 * providing type-safe validation rule construction and rule composition.
 */
export interface ValidationRuleBuilder {
  /**
   * Add BMS attribute-based validation rule
   */
  withBmsAttribute(attribute: BmsAttributeType): ValidationRuleBuilder;
  
  /**
   * Add PICIN pattern validation
   */
  withPicinPattern(pattern: string): ValidationRuleBuilder;
  
  /**
   * Add length constraint validation
   */
  withLength(min?: number, max?: number): ValidationRuleBuilder;
  
  /**
   * Add custom validation function
   */
  withCustomValidation(fn: FieldValidatorFunction): ValidationRuleBuilder;
  
  /**
   * Add required field validation
   */
  withRequired(isRequired: boolean): ValidationRuleBuilder;
  
  /**
   * Build final validation rule configuration
   */
  build(): FieldValidationRules;
}

/**
 * Form Validation Configuration Interface
 * 
 * Configuration object for form-level validation settings including validation
 * timing, error handling, and integration with React Hook Form and Material-UI.
 */
export interface FormValidationConfig {
  /** Validation timing strategy */
  validationMode: 'onChange' | 'onBlur' | 'onSubmit' | 'all';
  
  /** Error display configuration */
  errorDisplay: {
    showInline: boolean;
    showSummary: boolean;
    highlightInvalidFields: boolean;
    scrollToFirstError: boolean;
  };
  
  /** Cross-field validation settings */
  crossFieldValidation: {
    enabled: boolean;
    debounceMs: number;
    validateOnEveryChange: boolean;
  };
  
  /** Integration settings */
  integration: {
    reactHookForm: {
      mode: 'onChange' | 'onBlur' | 'onSubmit' | 'all';
      reValidateMode: 'onChange' | 'onBlur' | 'onSubmit';
    };
    materialUI: {
      errorColor: string;
      helperTextEnabled: boolean;
      requiredIndicator: string;
    };
  };
}

/**
 * Validation Error Context Interface
 * 
 * Provides detailed context information for validation errors including
 * field context, user actions, and error classification for enhanced
 * error handling and user experience.
 */
export interface ValidationErrorContext {
  /** Field that triggered the validation error */
  fieldName: string;
  
  /** User action that triggered validation */
  userAction: 'typing' | 'blur' | 'focus' | 'submit' | 'clear';
  
  /** Error classification */
  errorType: 'required' | 'format' | 'length' | 'business_rule' | 'system';
  
  /** Additional context for error handling */
  context: {
    fieldValue: any;
    formData: Record<string, any>;
    validationAttempts: number;
    timestamp: Date;
  };
}

/**
 * Default validation configuration constants
 * 
 * Provides default values for validation configuration matching BMS
 * validation behavior and modern React form validation best practices.
 */
export const DEFAULT_VALIDATION_CONFIG: FormValidationConfig = {
  validationMode: 'onBlur',
  errorDisplay: {
    showInline: true,
    showSummary: false,
    highlightInvalidFields: true,
    scrollToFirstError: true,
  },
  crossFieldValidation: {
    enabled: true,
    debounceMs: 300,
    validateOnEveryChange: false,
  },
  integration: {
    reactHookForm: {
      mode: 'onBlur',
      reValidateMode: 'onChange',
    },
    materialUI: {
      errorColor: '#d32f2f',
      helperTextEnabled: true,
      requiredIndicator: ' *',
    },
  },
};

/**
 * BMS attribute to validation rule mapping
 * 
 * Maps BMS field attributes to React Hook Form validation rules and
 * Material-UI component properties for seamless integration.
 */
export const BMS_VALIDATION_MAPPING: BmsToReactValidationMapping = {
  ASKIP: {
    registerOptions: { disabled: true },
    materialUIProps: { InputProps: { readOnly: true } },
    validationTrigger: 'onSubmit',
  },
  UNPROT: {
    registerOptions: { disabled: false },
    materialUIProps: { InputProps: { readOnly: false } },
    validationTrigger: 'onBlur',
  },
  PROT: {
    registerOptions: { disabled: true },
    materialUIProps: { disabled: true },
    validationTrigger: 'onSubmit',
  },
  NUM: {
    registerOptions: { 
      pattern: {
        value: /^[0-9]*$/,
        message: 'Only numeric values are allowed',
      },
    },
    materialUIProps: { 
      inputProps: { inputMode: 'numeric' as const },
      type: 'number',
    },
    validationTrigger: 'onChange',
  },
  MUSTFILL: {
    registerOptions: { required: 'This field is required' },
    materialUIProps: { required: true },
    validationTrigger: 'onBlur',
  },
  IC: {
    registerOptions: {},
    materialUIProps: { autoFocus: true },
    validationTrigger: 'onBlur',
  },
  BRT: {
    registerOptions: {},
    materialUIProps: { sx: { fontWeight: 'bold' } },
    validationTrigger: 'onBlur',
  },
  NORM: {
    registerOptions: {},
    materialUIProps: { sx: { fontWeight: 'normal' } },
    validationTrigger: 'onBlur',
  },
  FSET: {
    registerOptions: {},
    materialUIProps: { onChange: true },
    validationTrigger: 'onChange',
  },
  DRK: {
    registerOptions: {},
    materialUIProps: { 
      type: 'password',
      InputProps: { style: { WebkitTextSecurity: 'disc' } },
    },
    validationTrigger: 'onBlur',
  },
};