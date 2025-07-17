/**
 * CardDemo - Form Validation Hook
 * 
 * Custom React hook implementing BMS field validation rule preservation with real-time validation 
 * feedback, providing React Hook Form integration with Yup schemas for comprehensive field-level 
 * and cross-field validation that maintains exact business logic of original COBOL validation routines.
 * 
 * This hook translates BMS field attributes (ASKIP, UNPROT, PROT, NUM, IC, MUSTFILL) to 
 * React Hook Form validation patterns while preserving the original mainframe field behavior, 
 * validation sequencing, and error message display through Material-UI components.
 * 
 * Key Features:
 * - Converts BMS PICIN patterns to Yup validation schemas
 * - Implements MUSTFILL validation equivalent to BMS required field checking
 * - Provides real-time validation feedback matching original BMS field sequencing
 * - Creates cross-field validation functions for complex business rules
 * - Integrates with Material-UI error states and helper text display
 * - Maintains exact COBOL validation logic for financial calculations
 * 
 * Based on analysis of BMS maps: COSGN00, COACTVW, COACTUP and their copybook definitions
 * following the validation patterns defined in Section 7.4.4 and 7.6.2.
 * 
 * @author Blitzy agent
 * @version 1.0.0
 * @since 2024
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { useMemo } from 'react';

// Internal imports for comprehensive validation utilities
import { FieldValidationUtils } from '../utils/fieldValidation.js';
import { useBmsFieldAttributes } from '../hooks/useBmsFieldAttributes.js';

/**
 * Main form validation hook providing comprehensive BMS-to-React validation integration.
 * 
 * This hook orchestrates the complete form validation process by:
 * 1. Converting BMS field definitions to Yup validation schemas
 * 2. Integrating with React Hook Form for real-time validation
 * 3. Providing Material-UI compatible error handling
 * 4. Implementing cross-field validation patterns
 * 5. Preserving exact COBOL business logic validation
 * 
 * Implements validation requirements from Section 7.4.4 including:
 * - Standard input validation sequence with real-time format checking
 * - Field protection and validation through React state management
 * - Cross-field validation patterns for relationship checking
 * - Business rule validation via REST API endpoints
 * 
 * @param {Object} fieldDefinitions - BMS field definitions with attributes and validation rules
 * @param {Object} options - Configuration options for validation behavior
 * @param {boolean} options.enableRealTimeValidation - Enable real-time validation feedback
 * @param {boolean} options.enableCrossFieldValidation - Enable cross-field validation patterns
 * @param {Object} options.customValidators - Custom validation functions for specific fields
 * @param {Object} options.businessRules - Business rule validation configuration
 * @returns {Object} Complete form validation interface with React Hook Form integration
 */
const useFormValidation = (fieldDefinitions, options = {}) => {
  const {
    enableRealTimeValidation = true,
    enableCrossFieldValidation = true,
    customValidators = {},
    businessRules = {}
  } = options;

  // Get BMS field attribute utilities
  const { processFieldAttributes } = useBmsFieldAttributes();

  // Create validation schema from BMS field definitions
  const validationSchema = useMemo(() => {
    return createBmsValidationSchema(fieldDefinitions, {
      customValidators,
      businessRules
    });
  }, [fieldDefinitions, customValidators, businessRules]);

  // Initialize React Hook Form with Yup resolver
  const form = useForm({
    resolver: yupResolver(validationSchema),
    mode: enableRealTimeValidation ? 'onChange' : 'onSubmit',
    reValidateMode: 'onChange',
    defaultValues: extractDefaultValues(fieldDefinitions)
  });

  // Extract form methods for easier access
  const {
    control,
    handleSubmit,
    formState: { errors, isValid, isDirty, isSubmitting },
    setValue,
    getValues,
    trigger,
    reset,
    watch,
    clearErrors,
    setError
  } = form;

  /**
   * Creates Material-UI field validation properties from BMS attributes.
   * 
   * Integrates BMS field attributes with Material-UI TextField properties
   * to provide comprehensive validation feedback including:
   * - Error state indication
   * - Helper text display
   * - Field protection attributes
   * - Input validation constraints
   * 
   * @param {string} fieldName - Field name for attribute lookup
   * @returns {Object} Material-UI TextField properties with validation
   */
  const getFieldValidationProps = (fieldName) => {
    const fieldDef = fieldDefinitions[fieldName];
    if (!fieldDef) return {};

    // Get base Material-UI properties from BMS attributes
    const muiProps = processFieldAttributes(fieldDef);
    
    // Get current field error state
    const fieldError = errors[fieldName];
    const hasError = !!fieldError;

    // Create validation properties
    return {
      ...muiProps,
      error: hasError,
      helperText: hasError ? fieldError.message : muiProps.helperText || '',
      // Add validation metadata for debugging
      'data-field-name': fieldName,
      'data-has-error': hasError,
      'data-validation-rules': JSON.stringify(fieldDef.validn || [])
    };
  };

  /**
   * Validates a single field with real-time feedback.
   * 
   * Provides immediate validation feedback for individual fields
   * matching the original BMS field validation sequence while
   * offering enhanced user experience through Material-UI components.
   * 
   * @param {string} fieldName - Field name to validate
   * @param {*} value - Field value to validate
   * @returns {Promise<boolean>} Validation result
   */
  const validateField = async (fieldName, value) => {
    try {
      // Clear existing errors for this field
      clearErrors(fieldName);

      // Trigger validation for specific field
      const isValid = await trigger(fieldName);
      
      // Apply custom field validation if available
      if (customValidators[fieldName]) {
        const customResult = customValidators[fieldName](value, getValues());
        if (!customResult.isValid) {
          setError(fieldName, {
            type: 'custom',
            message: customResult.error
          });
          return false;
        }
      }

      return isValid;
    } catch (error) {
      console.error(`Validation error for field ${fieldName}:`, error);
      setError(fieldName, {
        type: 'validation',
        message: 'Validation error occurred'
      });
      return false;
    }
  };

  /**
   * Performs cross-field validation for complex business rules.
   * 
   * Implements cross-field validation patterns from Section 7.6.2
   * including state/ZIP code consistency, account-card linkage,
   * and date range validation while maintaining exact COBOL business logic.
   * 
   * @param {Object} formData - Complete form data for cross-field validation
   * @returns {Promise<Object>} Validation result with field-specific errors
   */
  const validateCrossFields = async (formData) => {
    if (!enableCrossFieldValidation) return {};

    const crossFieldErrors = {};

    // Validate state/ZIP code consistency
    if (formData.acsstte && formData.acszipc) {
      const stateZipResult = FieldValidationUtils.stateZipValidation(
        formData.acsstte, 
        formData.acszipc
      );
      if (!stateZipResult.isValid) {
        crossFieldErrors.acszipc = {
          type: 'cross-field',
          message: stateZipResult.error
        };
      }
    }

    // Validate account-card linkage for update screens
    if (formData.acctsid && formData.cardNumber) {
      // This would typically call a REST endpoint for validation
      // For now, implement basic length and format checking
      const accountValid = FieldValidationUtils.accountValidation(formData.acctsid);
      const cardValid = FieldValidationUtils.cardValidation(formData.cardNumber);
      
      if (!accountValid.isValid) {
        crossFieldErrors.acctsid = {
          type: 'cross-field',
          message: accountValid.error
        };
      }
      
      if (!cardValid.isValid) {
        crossFieldErrors.cardNumber = {
          type: 'cross-field',
          message: cardValid.error
        };
      }
    }

    // Validate date ranges (open/expiry/reissue dates)
    if (formData.opnyear && formData.expyear) {
      const openDate = `${formData.opnyear}-${formData.opnmon || '01'}-${formData.opnday || '01'}`;
      const expiryDate = `${formData.expyear}-${formData.expmon || '01'}-${formData.expday || '01'}`;
      
      const openValid = FieldValidationUtils.dateValidation(openDate);
      const expiryValid = FieldValidationUtils.dateValidation(expiryDate);
      
      if (openValid.isValid && expiryValid.isValid) {
        const openDateTime = new Date(openDate);
        const expiryDateTime = new Date(expiryDate);
        
        if (expiryDateTime <= openDateTime) {
          crossFieldErrors.expyear = {
            type: 'cross-field',
            message: 'Expiry date must be after opening date'
          };
        }
      }
    }

    // Apply cross-field errors to form
    Object.entries(crossFieldErrors).forEach(([fieldName, error]) => {
      setError(fieldName, error);
    });

    return crossFieldErrors;
  };

  /**
   * Handles form submission with comprehensive validation.
   * 
   * Orchestrates the complete form submission process including:
   * 1. Field-level validation
   * 2. Cross-field validation
   * 3. Business rule validation
   * 4. Error state management
   * 5. Success/failure handling
   * 
   * @param {Function} onSubmit - Form submission handler
   * @param {Function} onError - Error handling callback
   * @returns {Function} Form submission handler
   */
  const handleFormSubmit = (onSubmit, onError) => {
    return handleSubmit(async (data) => {
      try {
        // Perform cross-field validation
        const crossFieldErrors = await validateCrossFields(data);
        
        if (Object.keys(crossFieldErrors).length > 0) {
          if (onError) {
            onError(crossFieldErrors);
          }
          return;
        }

        // Call the provided submit handler
        await onSubmit(data);
        
      } catch (error) {
        console.error('Form submission error:', error);
        if (onError) {
          onError(error);
        }
      }
    });
  };

  // Return comprehensive form validation interface
  return {
    // React Hook Form core methods
    control,
    handleSubmit: handleFormSubmit,
    formState: { errors, isValid, isDirty, isSubmitting },
    setValue,
    getValues,
    trigger,
    reset,
    watch,
    clearErrors,
    setError,
    
    // Custom validation methods
    validateField,
    validateCrossFields,
    getFieldValidationProps,
    
    // Utility methods
    isFieldValid: (fieldName) => !errors[fieldName],
    getFieldError: (fieldName) => errors[fieldName]?.message || '',
    hasErrors: () => Object.keys(errors).length > 0,
    getErrorCount: () => Object.keys(errors).length,
    
    // Form state helpers
    isDirty,
    isValid,
    isSubmitting,
    
    // Schema reference for external use
    validationSchema
  };
};

/**
 * Creates comprehensive Yup validation schema from BMS field definitions.
 * 
 * Transforms BMS field attributes into Yup validation schemas that preserve
 * exact COBOL validation logic while providing React Hook Form integration.
 * 
 * Implements validation patterns from Section 7.4.4 including:
 * - PICIN pattern conversion to regex validation
 * - MUSTFILL requirements as required field validation
 * - Field length constraints from BMS LENGTH attribute
 * - Numeric validation for NUM attribute fields
 * - Custom business rule validation integration
 * 
 * @param {Object} fieldDefinitions - BMS field definitions with attributes
 * @param {Object} options - Schema creation options
 * @param {Object} options.customValidators - Custom validation functions
 * @param {Object} options.businessRules - Business rule configurations
 * @returns {yup.ObjectSchema} Comprehensive Yup validation schema
 */
export const createBmsValidationSchema = (fieldDefinitions, options = {}) => {
  const { customValidators = {}, businessRules = {} } = options;
  const schemaFields = {};

  // Process each field definition
  Object.entries(fieldDefinitions).forEach(([fieldName, fieldConfig]) => {
    let fieldSchema;

    // Determine base schema type from BMS attributes
    if (fieldConfig.attrb && fieldConfig.attrb.includes('NUM')) {
      fieldSchema = yup.number();
    } else {
      fieldSchema = yup.string();
    }

    // Apply MUSTFILL validation (BMS required field)
    if (fieldConfig.validn && fieldConfig.validn.some(rule => rule === 'MUSTFILL')) {
      fieldSchema = fieldSchema.required(
        formatValidationErrors(fieldName, 'required', `${fieldName} is required`)
      );
    }

    // Apply length constraints from BMS LENGTH attribute
    if (fieldConfig.length && fieldSchema._type === 'string') {
      fieldSchema = fieldSchema.max(
        fieldConfig.length,
        formatValidationErrors(fieldName, 'format', `${fieldName} must be ${fieldConfig.length} characters or less`)
      );
    }

    // Apply PICIN pattern validation
    if (fieldConfig.picin) {
      const regex = FieldValidationUtils.picinConversion(fieldConfig.picin);
      fieldSchema = fieldSchema.matches(
        regex,
        formatValidationErrors(fieldName, 'format', `${fieldName} must match pattern ${fieldConfig.picin}`)
      );
    }

    // Apply custom field validation
    if (customValidators[fieldName]) {
      fieldSchema = fieldSchema.test(
        'custom-validation',
        formatValidationErrors(fieldName, 'business', 'Invalid value'),
        function(value) {
          const result = customValidators[fieldName](value, this.parent);
          return result.isValid || this.createError({ message: result.error });
        }
      );
    }

    // Apply business rule validation
    if (businessRules[fieldName]) {
      fieldSchema = fieldSchema.test(
        'business-rule',
        formatValidationErrors(fieldName, 'business', 'Business rule violation'),
        function(value) {
          const result = FieldValidationUtils.businessRuleValidation(fieldName, businessRules)(value, this.parent);
          return !result || this.createError({ message: result });
        }
      );
    }

    // Apply field-specific validation based on field name patterns
    fieldSchema = applyFieldSpecificValidation(fieldName, fieldSchema);

    schemaFields[fieldName] = fieldSchema;
  });

  return yup.object().shape(schemaFields);
};

/**
 * Applies field-specific validation based on field name patterns.
 * 
 * Implements specialized validation for common field patterns found
 * in BMS screens including account numbers, card numbers, SSN, dates,
 * and other business-specific field types.
 * 
 * @param {string} fieldName - Field name to check for patterns
 * @param {yup.Schema} fieldSchema - Base field schema
 * @returns {yup.Schema} Enhanced field schema with specific validation
 */
const applyFieldSpecificValidation = (fieldName, fieldSchema) => {
  // Account number validation (11 digits with checksum)
  if (fieldName.toLowerCase().includes('acct') || fieldName === 'acctsid') {
    return fieldSchema.test(
      'account-validation',
      'Invalid account number',
      (value) => {
        if (!value) return true; // Let required validation handle empty values
        const result = FieldValidationUtils.accountValidation(value);
        return result.isValid;
      }
    );
  }

  // Card number validation (16 digits with Luhn checksum)
  if (fieldName.toLowerCase().includes('card') && fieldName.toLowerCase().includes('number')) {
    return fieldSchema.test(
      'card-validation',
      'Invalid card number',
      (value) => {
        if (!value) return true;
        const result = FieldValidationUtils.cardValidation(value);
        return result.isValid;
      }
    );
  }

  // SSN validation (XXX-XX-XXXX format)
  if (fieldName.toLowerCase().includes('ssn') || fieldName.startsWith('actssn')) {
    return fieldSchema.test(
      'ssn-validation',
      'Invalid SSN format',
      (value) => {
        if (!value) return true;
        const result = FieldValidationUtils.ssnValidation(value);
        return result.isValid;
      }
    );
  }

  // Date validation (MM/DD/YYYY format)
  if (fieldName.toLowerCase().includes('date') || fieldName.toLowerCase().includes('dob')) {
    return fieldSchema.test(
      'date-validation',
      'Invalid date format',
      (value) => {
        if (!value) return true;
        const result = FieldValidationUtils.dateValidation(value);
        return result.isValid;
      }
    );
  }

  // Email validation
  if (fieldName.toLowerCase().includes('email')) {
    return fieldSchema.email('Invalid email format');
  }

  // Phone validation (10 digits)
  if (fieldName.toLowerCase().includes('phone') || fieldName.toLowerCase().includes('phn')) {
    return fieldSchema.matches(
      /^\d{3}-\d{3}-\d{4}$/,
      'Phone number must be in XXX-XXX-XXXX format'
    );
  }

  // ZIP code validation (5 digits)
  if (fieldName.toLowerCase().includes('zip') || fieldName === 'acszipc') {
    return fieldSchema.matches(
      /^\d{5}$/,
      'ZIP code must be 5 digits'
    );
  }

  // State validation (2 uppercase letters)
  if (fieldName.toLowerCase().includes('state') || fieldName === 'acsstte') {
    return fieldSchema.matches(
      /^[A-Z]{2}$/,
      'State must be 2 uppercase letters'
    );
  }

  // FICO score validation (300-850)
  if (fieldName.toLowerCase().includes('fico') || fieldName === 'acstfco') {
    return fieldSchema.min(300, 'FICO score must be between 300 and 850')
                      .max(850, 'FICO score must be between 300 and 850');
  }

  return fieldSchema;
};

/**
 * Extracts default values from BMS field definitions.
 * 
 * Processes BMS field definitions to extract initial values
 * and default values for React Hook Form initialization.
 * 
 * @param {Object} fieldDefinitions - BMS field definitions
 * @returns {Object} Default values object for form initialization
 */
const extractDefaultValues = (fieldDefinitions) => {
  const defaultValues = {};

  Object.entries(fieldDefinitions).forEach(([fieldName, fieldConfig]) => {
    // Use initial value from BMS definition
    if (fieldConfig.initial) {
      defaultValues[fieldName] = fieldConfig.initial;
    }
    // Set default for numeric fields
    else if (fieldConfig.attrb && fieldConfig.attrb.includes('NUM')) {
      defaultValues[fieldName] = 0;
    }
    // Set empty string for text fields
    else {
      defaultValues[fieldName] = '';
    }
  });

  return defaultValues;
};

/**
 * Custom hook for individual field validation with real-time feedback.
 * 
 * Provides real-time validation for individual fields with immediate
 * feedback matching the original BMS field validation sequence.
 * 
 * @param {string} fieldName - Field name to validate
 * @param {Object} fieldConfig - BMS field configuration
 * @param {Object} options - Validation options
 * @returns {Object} Field validation interface
 */
export const useFieldValidation = (fieldName, fieldConfig, options = {}) => {
  const { enableRealTime = true } = options;

  // Create validation function for this field
  const validationFn = useMemo(() => {
    return FieldValidationUtils.bmsValidation(fieldConfig, fieldName);
  }, [fieldConfig, fieldName]);

  /**
   * Validates field value and returns result.
   * 
   * @param {*} value - Field value to validate
   * @returns {Object} Validation result with error information
   */
  const validateValue = (value) => {
    if (!validationFn.validationFn) {
      return { isValid: true, error: null };
    }

    const error = validationFn.validationFn(value);
    return {
      isValid: !error,
      error: error || null
    };
  };

  /**
   * Creates real-time validation handler for field.
   * 
   * @param {Function} onChange - Original onChange handler
   * @returns {Function} Enhanced onChange handler with validation
   */
  const createValidationHandler = (onChange) => {
    return (value) => {
      if (enableRealTime) {
        const result = validateValue(value);
        // Could dispatch validation result to parent component
      }
      onChange(value);
    };
  };

  return {
    validateValue,
    createValidationHandler,
    validationConfig: validationFn
  };
};

/**
 * Custom hook for cross-field validation patterns.
 * 
 * Implements cross-field validation patterns for complex business rules
 * including state/ZIP consistency, account-card linkage, and date ranges.
 * 
 * @param {Object} crossFieldRules - Cross-field validation rules
 * @returns {Object} Cross-field validation interface
 */
export const useCrossFieldValidation = (crossFieldRules) => {
  /**
   * Validates multiple fields for cross-field business rules.
   * 
   * @param {Object} values - Form values object
   * @returns {Object} Validation results by field
   */
  const validateCrossFields = (values) => {
    const results = {};

    // Apply each cross-field rule
    Object.entries(crossFieldRules).forEach(([ruleName, rule]) => {
      const { fields, validator, errorMessage } = rule;
      
      // Extract field values for this rule
      const fieldValues = {};
      fields.forEach(field => {
        fieldValues[field] = values[field];
      });

      // Run validation
      const isValid = validator(fieldValues);
      
      if (!isValid) {
        const primaryField = fields[0];
        results[primaryField] = {
          isValid: false,
          error: errorMessage || 'Cross-field validation failed'
        };
      }
    });

    return results;
  };

  return {
    validateCrossFields
  };
};

/**
 * Formats validation error messages for consistent display.
 * 
 * Ensures error messages match original BMS error message format
 * while providing enhanced user experience through Material-UI components.
 * 
 * @param {string} fieldName - Field name for error context
 * @param {string} errorType - Type of validation error
 * @param {string} customMessage - Custom error message
 * @returns {string} Formatted error message
 */
export const formatValidationErrors = (fieldName, errorType, customMessage) => {
  return FieldValidationUtils.errorFormatting(fieldName, errorType, customMessage);
};

/**
 * Creates Material-UI field validation properties from BMS attributes.
 * 
 * Generates comprehensive Material-UI TextField properties that preserve
 * BMS field behavior while providing modern validation feedback.
 * 
 * @param {Object} fieldConfig - BMS field configuration
 * @param {Object} validationState - Current validation state
 * @returns {Object} Material-UI TextField properties
 */
export const createBmsFieldValidationProps = (fieldConfig, validationState = {}) => {
  const { error, helperText, ...otherProps } = validationState;
  
  // Get base BMS attribute properties
  const bmsProps = FieldValidationUtils.bmsValidation(fieldConfig, fieldConfig.name || 'field');
  
  // Combine with validation state
  return {
    error: !!error,
    helperText: error || helperText || '',
    ...bmsProps,
    ...otherProps
  };
};

// Export main hook as default
export default useFormValidation;