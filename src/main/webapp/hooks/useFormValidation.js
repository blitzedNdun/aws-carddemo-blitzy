/**
 * useFormValidation.js
 * 
 * Custom React hook implementing BMS field validation rule preservation with real-time 
 * validation feedback, providing React Hook Form integration with Yup schemas for 
 * comprehensive field-level and cross-field validation that maintains exact business 
 * logic of original COBOL validation routines.
 * 
 * This hook serves as the primary interface for form validation in the CardDemo application,
 * converting BMS mapset definitions to React Hook Form validation schemas while preserving
 * exact mainframe field behavior, validation timing, and error display patterns.
 * 
 * Key Features:
 * - BMS attribute byte translation to Material-UI TextField properties
 * - Real-time field validation with debounced feedback
 * - Cross-field validation for complex business rules
 * - COBOL-equivalent validation logic preservation
 * - Yup schema generation from BMS field definitions
 * - Material-UI integration for error display and field state management
 * - React Hook Form integration for optimized form performance
 * 
 * Technical Implementation:
 * - Maintains exact BMS field sequencing and validation timing
 * - Preserves COBOL COMP-3 decimal precision in validation logic
 * - Replicates BMS MUSTFILL, PICIN, and cross-field validation patterns
 * - Provides Material-UI error state management matching BMS error display
 * - Integrates with existing field validation utilities and BMS attribute mapping
 * 
 * @version 1.0.0
 * @since 2024-01-15
 * 
 * Copyright (c) 2024 CardDemo Application
 * Technology transformation: COBOL/CICS/BMS → Java/Spring Boot/React
 */

import { useState, useEffect, useCallback, useMemo } from 'react';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { useMemo as useReactMemo } from 'react';

// Import field validation utilities and BMS attribute mapping
import { FieldValidationUtils } from '../utils/fieldValidation';
import { useBmsFieldAttributes } from './useBmsFieldAttributes';

// Destructure validation utilities for easier access
const {
  bmsValidation,
  picinConversion,
  mustfillValidation,
  crossFieldValidation,
  businessRuleValidation,
  realTimeValidation,
  accountValidation,
  cardValidation,
  ssnValidation,
  dateValidation,
  stateZipValidation,
  schemaGeneration,
  errorFormatting
} = FieldValidationUtils;

/**
 * Creates comprehensive BMS validation schema from field definitions
 * 
 * Converts BMS mapset field definitions to Yup validation schemas that preserve
 * exact COBOL validation logic while providing React Hook Form integration.
 * Maintains field sequencing, validation timing, and error display patterns
 * identical to original BMS screens.
 * 
 * @param {Object} fieldDefinitions - BMS field definitions configuration
 * @param {Array} fieldDefinitions.fields - Array of field definition objects
 * @param {string} fieldDefinitions.fields[].name - Field name from BMS definition
 * @param {Array<string>} fieldDefinitions.fields[].attributes - BMS attributes (ASKIP, UNPROT, etc.)
 * @param {number} fieldDefinitions.fields[].length - Field length from BMS LENGTH attribute
 * @param {string} fieldDefinitions.fields[].picinPattern - PICIN pattern for format validation
 * @param {boolean} fieldDefinitions.fields[].mustfill - VALIDN=(MUSTFILL) requirement
 * @param {string} fieldDefinitions.fields[].dataType - Field data type (string, number, date)
 * @param {Array} fieldDefinitions.crossFieldRules - Cross-field validation rules
 * @param {Object} fieldDefinitions.businessRules - Business rule validation configuration
 * @returns {Object} Yup validation schema with BMS-equivalent validation logic
 */
export function createBmsValidationSchema(fieldDefinitions) {
  const { fields = [], crossFieldRules = [], businessRules = {} } = fieldDefinitions;
  
  // Create base schema fields object
  const schemaFields = {};
  
  // Process each field definition to create Yup validation rules
  fields.forEach(field => {
    const {
      name,
      attributes = [],
      length,
      picinPattern,
      mustfill = false,
      dataType = 'string',
      minValue,
      maxValue,
      allowedValues,
      customValidation
    } = field;
    
    // Skip validation for protected fields (ASKIP, PROT)
    if (attributes.includes('ASKIP') || attributes.includes('PROT')) {
      return;
    }
    
    // Create base Yup schema based on data type
    let fieldSchema;
    
    switch (dataType) {
      case 'number':
        fieldSchema = yup.number()
          .transform((value, originalValue) => {
            // Handle empty strings as undefined for optional fields
            if (originalValue === '') return undefined;
            return value;
          })
          .nullable();
        
        // Add numeric constraints
        if (minValue !== undefined) fieldSchema = fieldSchema.min(minValue);
        if (maxValue !== undefined) fieldSchema = fieldSchema.max(maxValue);
        
        // Add NUM attribute validation for numeric fields
        if (attributes.includes('NUM')) {
          fieldSchema = fieldSchema.test(
            'numeric-only',
            'Only numeric characters are allowed',
            (value) => value === undefined || value === null || /^\d*\.?\d*$/.test(String(value))
          );
        }
        break;
        
      case 'date':
        fieldSchema = yup.date()
          .transform((value, originalValue) => {
            if (originalValue === '') return undefined;
            return value;
          })
          .nullable();
        break;
        
      case 'boolean':
        fieldSchema = yup.boolean();
        break;
        
      default:
        fieldSchema = yup.string()
          .transform((value) => value === null ? undefined : value)
          .nullable();
        
        // Add length constraint
        if (length) {
          fieldSchema = fieldSchema.max(length, `Maximum ${length} characters allowed`);
        }
        
        // Add PICIN pattern validation
        if (picinPattern) {
          const regexPattern = picinConversion(picinPattern);
          fieldSchema = fieldSchema.matches(
            regexPattern,
            `Invalid format. Expected: ${picinPattern}`
          );
        }
        
        // Add allowed values validation
        if (allowedValues && allowedValues.length > 0) {
          fieldSchema = fieldSchema.oneOf(
            allowedValues,
            `Must be one of: ${allowedValues.join(', ')}`
          );
        }
    }
    
    // Add MUSTFILL required validation
    if (mustfill) {
      fieldSchema = fieldSchema.required(`${name} is required`);
    }
    
    // Add custom validation if provided
    if (customValidation) {
      fieldSchema = fieldSchema.test(
        'custom-validation',
        customValidation.message || 'Validation failed',
        customValidation.validator
      );
    }
    
    // Add business rule validation
    if (businessRules[name]) {
      const businessRule = businessRules[name];
      fieldSchema = fieldSchema.test(
        'business-rule',
        businessRule.message || 'Business rule validation failed',
        (value, context) => {
          const validator = businessRuleValidation(businessRule);
          return validator(value, context.parent);
        }
      );
    }
    
    schemaFields[name] = fieldSchema;
  });
  
  // Create object schema with field validations
  let schema = yup.object(schemaFields);
  
  // Add cross-field validation rules
  if (crossFieldRules.length > 0) {
    schema = schema.test(
      'cross-field-validation',
      'Cross-field validation failed',
      function (values) {
        const errors = [];
        
        crossFieldRules.forEach(rule => {
          const { name, fields: ruleFields, validator, message, errorField } = rule;
          
          try {
            const fieldValues = ruleFields.map(fieldName => values[fieldName]);
            const result = validator(...fieldValues, values);
            
            if (result !== true) {
              errors.push(this.createError({
                path: errorField || ruleFields[0],
                message: result === false ? message : result
              }));
            }
          } catch (error) {
            console.error(`Cross-field validation error for rule ${name}:`, error);
            errors.push(this.createError({
              path: errorField || ruleFields[0],
              message: 'Cross-field validation error'
            }));
          }
        });
        
        if (errors.length > 0) {
          throw new yup.ValidationError(errors);
        }
        
        return true;
      }
    );
  }
  
  return schema;
}

/**
 * Custom hook for individual field validation
 * 
 * Provides real-time validation for a single field with BMS-equivalent behavior,
 * including debounced validation, error display, and Material-UI integration.
 * Maintains exact field validation timing and error display patterns.
 * 
 * @param {Object} fieldConfig - Field configuration object
 * @param {string} fieldConfig.name - Field name
 * @param {Object} fieldConfig.validationRules - Validation rules for the field
 * @param {Object} fieldConfig.formContext - Form context for cross-field validation
 * @param {Object} options - Validation options
 * @param {number} options.debounceMs - Debounce delay for real-time validation
 * @param {boolean} options.validateOnChange - Enable validation on field change
 * @param {boolean} options.validateOnBlur - Enable validation on field blur
 * @returns {Object} Field validation handlers and state
 */
export function useFieldValidation(fieldConfig, options = {}) {
  const {
    name,
    validationRules = {},
    formContext = {}
  } = fieldConfig;
  
  const {
    debounceMs = 300,
    validateOnChange = true,
    validateOnBlur = true
  } = options;
  
  const [fieldError, setFieldError] = useState(null);
  const [isValidating, setIsValidating] = useState(false);
  
  // Create real-time validator for the field
  const validator = useMemo(() => {
    const validators = [];
    
    // Add validation rules based on field configuration
    if (validationRules.required) {
      validators.push(mustfillValidation(name, {
        message: validationRules.requiredMessage || `${name} is required`
      }));
    }
    
    if (validationRules.pattern) {
      validators.push((value) => {
        if (!value) return true;
        const regex = new RegExp(validationRules.pattern);
        return regex.test(String(value)) || validationRules.patternMessage || 'Invalid format';
      });
    }
    
    if (validationRules.maxLength) {
      validators.push((value) => {
        if (!value) return true;
        return String(value).length <= validationRules.maxLength || 
               `Maximum ${validationRules.maxLength} characters allowed`;
      });
    }
    
    // Add business rule validation
    if (validationRules.businessRule) {
      validators.push(businessRuleValidation(validationRules.businessRule));
    }
    
    return realTimeValidation({
      debounceMs,
      validateOnChange,
      validateOnBlur,
      validators
    });
  }, [name, validationRules, debounceMs, validateOnChange, validateOnBlur]);
  
  // Field validation handlers
  const validateField = useCallback((value) => {
    setIsValidating(true);
    
    const result = validator.validate(value, formContext);
    
    setFieldError(result);
    setIsValidating(false);
    
    return result;
  }, [validator, formContext]);
  
  const handleChange = useCallback((value) => {
    if (validateOnChange) {
      validator.onChange(value, formContext, setFieldError);
    }
  }, [validator, formContext, validateOnChange]);
  
  const handleBlur = useCallback((value) => {
    if (validateOnBlur) {
      validator.onBlur(value, formContext, setFieldError);
    }
  }, [validator, formContext, validateOnBlur]);
  
  const clearError = useCallback(() => {
    setFieldError(null);
  }, []);
  
  return {
    fieldError,
    isValidating,
    validateField,
    handleChange,
    handleBlur,
    clearError,
    hasError: Boolean(fieldError)
  };
}

/**
 * Custom hook for cross-field validation
 * 
 * Implements complex validation rules that span multiple fields, such as
 * state/ZIP consistency, account-card linkage, and date range validation.
 * Maintains exact COBOL business logic while providing React integration.
 * 
 * @param {Array} validationRules - Array of cross-field validation rules
 * @param {Object} formValues - Current form values for validation
 * @param {Object} options - Validation options
 * @returns {Object} Cross-field validation results and handlers
 */
export function useCrossFieldValidation(validationRules = [], formValues = {}, options = {}) {
  const [crossFieldErrors, setCrossFieldErrors] = useState({});
  const [isValidating, setIsValidating] = useState(false);
  
  // Create cross-field validator
  const validator = useMemo(() => {
    return crossFieldValidation({ rules: validationRules });
  }, [validationRules]);
  
  // Validate all cross-field rules
  const validateCrossFields = useCallback(() => {
    setIsValidating(true);
    
    try {
      const errors = validator(formValues);
      setCrossFieldErrors(errors || {});
    } catch (error) {
      console.error('Cross-field validation error:', error);
      setCrossFieldErrors({ general: 'Validation error occurred' });
    } finally {
      setIsValidating(false);
    }
  }, [validator, formValues]);
  
  // Validate specific fields
  const validateSpecificFields = useCallback((fieldNames) => {
    const relevantRules = validationRules.filter(rule => 
      rule.fields.some(field => fieldNames.includes(field))
    );
    
    if (relevantRules.length === 0) return;
    
    setIsValidating(true);
    
    try {
      const errors = crossFieldValidation({ rules: relevantRules })(formValues);
      setCrossFieldErrors(prevErrors => ({
        ...prevErrors,
        ...errors
      }));
    } catch (error) {
      console.error('Specific cross-field validation error:', error);
    } finally {
      setIsValidating(false);
    }
  }, [validationRules, formValues]);
  
  // Clear cross-field errors
  const clearCrossFieldErrors = useCallback(() => {
    setCrossFieldErrors({});
  }, []);
  
  // Effect to validate when form values change
  useEffect(() => {
    if (Object.keys(formValues).length > 0) {
      validateCrossFields();
    }
  }, [formValues, validateCrossFields]);
  
  return {
    crossFieldErrors,
    isValidating,
    validateCrossFields,
    validateSpecificFields,
    clearCrossFieldErrors,
    hasErrors: Object.keys(crossFieldErrors).length > 0
  };
}

/**
 * Formats validation errors for Material-UI display
 * 
 * Converts validation error objects to user-friendly error messages that
 * match original BMS error display patterns, maintaining consistent error
 * messaging and positioning throughout the application.
 * 
 * @param {Object|Array} errors - Validation errors from React Hook Form or Yup
 * @param {Object} fieldLabels - Field label mappings for error display
 * @param {Object} options - Formatting options
 * @returns {Object} Formatted error messages for Material-UI components
 */
export function formatValidationErrors(errors, fieldLabels = {}, options = {}) {
  const {
    includeFieldLabels = true,
    errorPrefix = '',
    maxErrorsPerField = 1
  } = options;
  
  if (!errors) return {};
  
  const formattedErrors = {};
  
  // Handle React Hook Form errors object
  if (errors && typeof errors === 'object' && !Array.isArray(errors)) {
    Object.keys(errors).forEach(fieldName => {
      const error = errors[fieldName];
      let errorMessage = '';
      
      if (error) {
        if (typeof error === 'string') {
          errorMessage = error;
        } else if (error.message) {
          errorMessage = error.message;
        } else if (error.type) {
          errorMessage = `Validation error: ${error.type}`;
        }
        
        // Add field label if requested
        if (includeFieldLabels && fieldLabels[fieldName]) {
          errorMessage = `${fieldLabels[fieldName]}: ${errorMessage}`;
        }
        
        // Add prefix if specified
        if (errorPrefix) {
          errorMessage = `${errorPrefix}${errorMessage}`;
        }
        
        formattedErrors[fieldName] = errorMessage;
      }
    });
  }
  
  // Handle array of errors
  else if (Array.isArray(errors)) {
    errors.forEach(error => {
      const fieldName = error.path || error.field || 'general';
      const message = error.message || error.msg || 'Validation error';
      
      let errorMessage = message;
      
      if (includeFieldLabels && fieldLabels[fieldName]) {
        errorMessage = `${fieldLabels[fieldName]}: ${errorMessage}`;
      }
      
      if (errorPrefix) {
        errorMessage = `${errorPrefix}${errorMessage}`;
      }
      
      formattedErrors[fieldName] = errorMessage;
    });
  }
  
  return formattedErrors;
}

/**
 * Creates BMS field validation properties for Material-UI components
 * 
 * Converts BMS field attributes to Material-UI TextField properties with
 * validation integration, preserving exact mainframe field behavior while
 * providing modern React component integration.
 * 
 * @param {Object} fieldConfig - BMS field configuration
 * @param {Object} validationState - Current validation state
 * @param {Object} bmsAttributes - BMS field attributes from useBmsFieldAttributes
 * @returns {Object} Complete Material-UI TextField properties with validation
 */
export function createBmsFieldValidationProps(fieldConfig, validationState = {}, bmsAttributes = {}) {
  const {
    name,
    label,
    attributes = [],
    length,
    picinPattern,
    mustfill = false,
    color = 'NEUTRAL',
    highlight = 'OFF',
    initialValue = ''
  } = fieldConfig;
  
  const {
    fieldError,
    isValidating = false,
    handleChange,
    handleBlur
  } = validationState;
  
  // Get BMS attribute mapping
  const bmsProps = bmsAttributes.mapAttributesToProps ? 
    bmsAttributes.mapAttributesToProps(fieldConfig) : {};
  
  // Create base Material-UI properties
  const materialUIProps = {
    name,
    label,
    defaultValue: initialValue,
    error: Boolean(fieldError),
    helperText: fieldError || '',
    disabled: attributes.includes('PROT') || attributes.includes('ASKIP'),
    required: mustfill,
    InputProps: {
      readOnly: attributes.includes('ASKIP'),
      ...bmsProps.InputProps
    },
    inputProps: {
      maxLength: length,
      'data-bms-field': name,
      'data-bms-attributes': attributes.join(','),
      ...bmsProps.inputProps
    },
    sx: {
      ...bmsProps.sx,
      // Add validation state styling
      '& .MuiInputBase-input': {
        ...bmsProps.sx?.['& .MuiInputBase-input'],
        ...(isValidating && {
          background: 'rgba(255, 255, 0, 0.1)' // Subtle validation indicator
        })
      }
    },
    ...bmsProps
  };
  
  // Add validation handlers
  if (handleChange) {
    materialUIProps.onChange = (event) => {
      handleChange(event.target.value);
      if (bmsProps.onChange) {
        bmsProps.onChange(event);
      }
    };
  }
  
  if (handleBlur) {
    materialUIProps.onBlur = (event) => {
      handleBlur(event.target.value);
      if (bmsProps.onBlur) {
        bmsProps.onBlur(event);
      }
    };
  }
  
  return materialUIProps;
}

/**
 * Main form validation hook with BMS integration
 * 
 * Primary hook that provides comprehensive form validation with BMS field
 * attribute preservation, real-time validation, cross-field validation,
 * and Material-UI integration. Maintains exact COBOL business logic while
 * providing modern React form handling capabilities.
 * 
 * @param {Object} config - Form validation configuration
 * @param {Object} config.fieldDefinitions - BMS field definitions
 * @param {Array} config.crossFieldRules - Cross-field validation rules
 * @param {Object} config.businessRules - Business rule validation configuration
 * @param {Object} config.validationOptions - Validation behavior options
 * @param {Object} config.initialValues - Initial form values
 * @returns {Object} Form validation methods and state
 */
export default function useFormValidation(config) {
  const {
    fieldDefinitions = { fields: [] },
    crossFieldRules = [],
    businessRules = {},
    validationOptions = {},
    initialValues = {}
  } = config;
  
  // Get BMS field attributes utilities
  const bmsAttributes = useBmsFieldAttributes();
  
  // Create Yup validation schema
  const validationSchema = useMemo(() => {
    return createBmsValidationSchema({
      fields: fieldDefinitions.fields,
      crossFieldRules,
      businessRules
    });
  }, [fieldDefinitions.fields, crossFieldRules, businessRules]);
  
  // Initialize React Hook Form with Yup resolver
  const formMethods = useForm({
    defaultValues: initialValues,
    resolver: yupResolver(validationSchema),
    mode: validationOptions.mode || 'onChange',
    reValidateMode: validationOptions.reValidateMode || 'onChange',
    shouldFocusError: validationOptions.shouldFocusError !== false
  });
  
  const {
    handleSubmit,
    formState: { errors, isValid, isSubmitting, touchedFields, dirtyFields },
    watch,
    getValues,
    setValue,
    clearErrors,
    setError,
    reset,
    trigger
  } = formMethods;
  
  // Watch all form values for cross-field validation
  const watchedValues = watch();
  
  // Cross-field validation hook
  const {
    crossFieldErrors,
    validateCrossFields,
    validateSpecificFields,
    clearCrossFieldErrors
  } = useCrossFieldValidation(crossFieldRules, watchedValues, validationOptions);
  
  // Combined errors (field errors + cross-field errors)
  const allErrors = useMemo(() => {
    return { ...errors, ...crossFieldErrors };
  }, [errors, crossFieldErrors]);
  
  // Create field validation properties
  const createFieldProps = useCallback((fieldName) => {
    const fieldConfig = fieldDefinitions.fields.find(field => field.name === fieldName);
    if (!fieldConfig) {
      console.warn(`Field configuration not found for: ${fieldName}`);
      return {};
    }
    
    const fieldError = allErrors[fieldName];
    const validationState = {
      fieldError: fieldError?.message || fieldError,
      isValidating: false,
      handleChange: (value) => {
        setValue(fieldName, value);
        if (touchedFields[fieldName]) {
          trigger(fieldName);
        }
      },
      handleBlur: () => {
        trigger(fieldName);
      }
    };
    
    return createBmsFieldValidationProps(fieldConfig, validationState, bmsAttributes);
  }, [fieldDefinitions.fields, allErrors, setValue, trigger, touchedFields, bmsAttributes]);
  
  // Validate specific fields
  const validateFields = useCallback(async (fieldNames) => {
    const fieldValidationResult = await trigger(fieldNames);
    validateSpecificFields(fieldNames);
    return fieldValidationResult;
  }, [trigger, validateSpecificFields]);
  
  // Validate entire form
  const validateForm = useCallback(async () => {
    const fieldValidationResult = await trigger();
    validateCrossFields();
    return fieldValidationResult && Object.keys(crossFieldErrors).length === 0;
  }, [trigger, validateCrossFields, crossFieldErrors]);
  
  // Clear all errors
  const clearAllErrors = useCallback(() => {
    clearErrors();
    clearCrossFieldErrors();
  }, [clearErrors, clearCrossFieldErrors]);
  
  // Format errors for display
  const formattedErrors = useMemo(() => {
    const fieldLabels = fieldDefinitions.fields.reduce((acc, field) => {
      acc[field.name] = field.label || field.name;
      return acc;
    }, {});
    
    return formatValidationErrors(allErrors, fieldLabels);
  }, [allErrors, fieldDefinitions.fields]);
  
  // Submit handler with comprehensive validation
  const onSubmit = useCallback(async (data) => {
    try {
      // Validate all fields and cross-field rules
      const isFormValid = await validateForm();
      
      if (!isFormValid) {
        console.warn('Form validation failed');
        return { success: false, errors: formattedErrors };
      }
      
      return { success: true, data };
    } catch (error) {
      console.error('Form submission error:', error);
      return { success: false, errors: { general: 'Form submission failed' } };
    }
  }, [validateForm, formattedErrors]);
  
  // Reset form with new values
  const resetForm = useCallback((newValues = {}) => {
    reset(newValues);
    clearCrossFieldErrors();
  }, [reset, clearCrossFieldErrors]);
  
  return {
    // Form methods and state
    ...formMethods,
    
    // Enhanced validation methods
    validateFields,
    validateForm,
    clearAllErrors,
    resetForm,
    
    // Error handling
    allErrors,
    formattedErrors,
    hasErrors: Object.keys(allErrors).length > 0,
    
    // Field property generation
    createFieldProps,
    
    // Submit handling
    onSubmit: handleSubmit(onSubmit),
    
    // Form state
    isValid: isValid && Object.keys(crossFieldErrors).length === 0,
    isSubmitting,
    touchedFields,
    dirtyFields,
    
    // BMS integration
    bmsAttributes,
    
    // Validation schema
    validationSchema,
    
    // Current form values
    values: watchedValues
  };
}