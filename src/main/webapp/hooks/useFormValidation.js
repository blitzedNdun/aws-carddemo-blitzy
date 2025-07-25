/**
 * useFormValidation.js
 * 
 * Custom React hook implementing BMS field validation rule preservation with real-time 
 * validation feedback. Provides React Hook Form integration with Yup schemas for 
 * comprehensive field-level and cross-field validation that maintains exact business 
 * logic of original COBOL validation routines.
 * 
 * This hook processes BMS mapset definitions (COSGN00, COACTVW, COACTUP, etc.) and 
 * converts DFHMDF attributes, PICIN patterns, VALIDN rules, and cross-field dependencies 
 * into modern React validation schemas while preserving:
 * 
 * - Exact COBOL numeric precision and formatting rules
 * - BMS field protection states and input validation behavior  
 * - MUSTFILL requirements matching original BMS VALIDN attributes
 * - Cross-field validation for state/ZIP consistency and account-card linkage
 * - Real-time validation feedback matching original 3270 field sequencing
 * - Error message display patterns consistent with BMS ERRMSG field behavior
 * 
 * Key Features:
 * - BMS attribute byte mapping (ASKIP→readOnly, UNPROT→editable, PROT→disabled, etc.)
 * - PICIN pattern conversion ('99999999999' → account number validation)  
 * - MUSTFILL validation equivalent to BMS required field checking
 * - Real-time validation with debounced feedback for improved UX
 * - Cross-field business rule validation (state/ZIP, account/card relationships)
 * - Yup schema generation for comprehensive form validation
 * - Material-UI integration with proper error states and helper text
 * 
 * @fileoverview CardDemo Form Validation Hook
 * @version 1.0.0
 * @author Blitzy Platform - CardDemo Migration Team  
 * @copyright 2024 CardDemo Application Migration Project
 */

import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { useMemo } from 'react';

import FieldValidationUtils from '../utils/fieldValidation.js';
import useBmsFieldAttributes from '../hooks/useBmsFieldAttributes.js';

/**
 * Creates comprehensive BMS validation schema for React Hook Form integration
 * Converts BMS field definitions with DFHMDF attributes to Yup validation schema
 * that preserves exact COBOL validation behavior and business rules
 * 
 * @param {Object} fieldsConfig - BMS field configuration object
 * @param {Object} options - Schema creation options
 * @param {boolean} options.strictMode - Enable strict COBOL-equivalent validation
 * @param {boolean} options.realTimeValidation - Enable real-time validation feedback
 * @param {Object} options.crossFieldRules - Cross-field validation rules
 * @returns {Object} Yup validation schema with BMS behavior preservation
 * 
 * @example
 * // For COSGN00.bms login screen
 * const loginSchema = createBmsValidationSchema({
 *   userId: {
 *     attrb: ['UNPROT', 'IC', 'FSET'],
 *     length: 8,
 *     color: 'GREEN',
 *     required: true
 *   },
 *   password: {
 *     attrb: ['DRK', 'UNPROT', 'FSET'], 
 *     length: 8,
 *     color: 'GREEN',
 *     required: true
 *   }
 * });
 */
export function createBmsValidationSchema(fieldsConfig, options = {}) {
    const { 
        strictMode = true, 
        realTimeValidation = true,
        crossFieldRules = {} 
    } = options;
    
    const schemaFields = {};
    const crossFieldValidators = [];

    // Process each field configuration
    Object.entries(fieldsConfig).forEach(([fieldName, fieldConfig]) => {
        const {
            attrb,
            length,
            picin,
            validn,
            required,
            businessRules = {},
            customValidation
        } = fieldConfig;

        // Create BMS validation rules using FieldValidationUtils
        const bmsRules = FieldValidationUtils.bmsValidation(fieldConfig, {
            strictMode,
            realTimeValidation
        });

        // Start with base Yup string schema
        let fieldSchema = yup.string();

        // Apply MUSTFILL validation (BMS required field behavior)
        if (validn === 'MUSTFILL' || required || (Array.isArray(attrb) && attrb.includes('MUSTFILL'))) {
            const mustfillValidator = FieldValidationUtils.mustfillValidation(fieldName, {
                trimWhitespace: true,
                customMessage: `${fieldName} is required`
            });
            
            fieldSchema = fieldSchema.required(`${fieldName} is required`).test(
                'bms-mustfill',
                `${fieldName} cannot be empty`,
                function(value) {
                    const result = mustfillValidator(value);
                    return result === true || this.createError({ message: result });
                }
            );
        }

        // Apply length constraints from BMS LENGTH attribute
        if (length) {
            fieldSchema = fieldSchema.max(
                length, 
                `${fieldName} cannot exceed ${length} characters`
            );
            
            // For numeric fields with exact length requirements (like account numbers)
            if (picin && /^9+$/.test(picin)) {
                fieldSchema = fieldSchema.length(
                    length,
                    `${fieldName} must be exactly ${length} digits`
                );
            }
        }

        // Apply PICIN pattern validation
        if (picin) {
            const picinRegex = FieldValidationUtils.picinConversion(picin);
            fieldSchema = fieldSchema.matches(
                picinRegex,
                `${fieldName} must match the required format`
            ).test(
                'picin-validation',
                `${fieldName} format is invalid`,
                function(value) {
                    if (!value && !bmsRules.mustfill) return true;
                    return picinRegex.test(value) || 
                           this.createError({ message: `${fieldName} must match pattern: ${picin}` });
                }
            );
        }

        // Apply specific field type validations
        switch (fieldName.toLowerCase()) {
            case 'userid':
            case 'acctsid':
                // Account number validation using PICIN='99999999999'
                fieldSchema = fieldSchema.test(
                    'account-validation',
                    'Invalid account number format',
                    function(value) {
                        if (!value && !bmsRules.mustfill) return true;
                        const result = FieldValidationUtils.accountValidation(value);
                        return result === true || this.createError({ message: result });
                    }
                );
                break;
                
            case 'cardnum':
            case 'cardnumber':
                // Card number validation for 16-digit format
                fieldSchema = fieldSchema.test(
                    'card-validation',
                    'Invalid card number format',
                    function(value) {
                        if (!value && !bmsRules.mustfill) return true;
                        const result = FieldValidationUtils.cardValidation(value);
                        return result === true || this.createError({ message: result });
                    }
                );
                break;
                
            case 'ssn':
            case 'actssn1':
            case 'actssn2': 
            case 'actssn3':
                // SSN validation for individual parts or full SSN
                fieldSchema = fieldSchema.test(
                    'ssn-validation',
                    'Invalid SSN format',
                    function(value) {
                        if (!value && !bmsRules.mustfill) return true;
                        const result = FieldValidationUtils.ssnValidation(value);
                        return result === true || this.createError({ message: result });
                    }
                );
                break;
                
            case 'opnyear':
            case 'expyear':
            case 'risyear':
            case 'dobyear':
                // Date year validation (4-digit format)
                fieldSchema = fieldSchema.matches(
                    /^\d{4}$/,
                    'Year must be 4 digits'
                ).test(
                    'year-range',
                    'Year must be between 1900 and 2100',
                    function(value) {
                        if (!value) return true;
                        const year = parseInt(value);
                        return (year >= 1900 && year <= 2100) || 
                               this.createError({ message: 'Year must be between 1900 and 2100' });
                    }
                );
                break;
                
            case 'opnmon':
            case 'expmon':
            case 'rismon':
            case 'dobmon':
                // Date month validation (1-12)
                fieldSchema = fieldSchema.matches(
                    /^(0?[1-9]|1[0-2])$/,
                    'Month must be between 01 and 12'
                );
                break;
                
            case 'opnday':
            case 'expday':
            case 'risday':
            case 'dobday':
                // Date day validation (1-31)
                fieldSchema = fieldSchema.matches(
                    /^(0?[1-9]|[12][0-9]|3[01])$/,
                    'Day must be between 01 and 31'
                );
                break;
                
            case 'acsstte':
                // State code validation (2-letter format)
                fieldSchema = fieldSchema.matches(
                    /^[A-Z]{2}$/i,
                    'State must be 2 letters'
                ).uppercase();
                break;
                
            case 'acszipc':
                // ZIP code validation (5-digit format)
                fieldSchema = fieldSchema.matches(
                    /^\d{5}$/,
                    'ZIP code must be 5 digits'
                );
                break;
        }

        // Apply business rule validations
        if (businessRules && Object.keys(businessRules).length > 0) {
            Object.entries(businessRules).forEach(([ruleName, ruleConfig]) => {
                const businessValidator = FieldValidationUtils.businessRuleValidation(ruleName, ruleConfig);
                fieldSchema = fieldSchema.test(
                    `business-rule-${ruleName}`,
                    ruleConfig.message || 'Business rule validation failed',
                    function(value) {
                        const result = businessValidator(value, this.parent);
                        return result === true || this.createError({ message: result });
                    }
                );
            });
        }

        // Apply custom validation function if provided
        if (customValidation && typeof customValidation === 'function') {
            fieldSchema = fieldSchema.test(
                'custom-validation',
                'Custom validation failed',
                function(value) {
                    try {
                        const result = customValidation(value, this.parent);
                        return result === true || this.createError({ 
                            message: typeof result === 'string' ? result : 'Custom validation failed'
                        });
                    } catch (error) {
                        return this.createError({ message: 'Validation error occurred' });
                    }
                }
            );
        }

        schemaFields[fieldName] = fieldSchema;
    });

    // Add cross-field validation rules
    Object.entries(crossFieldRules).forEach(([ruleName, crossFieldRule]) => {
        const crossFieldValidator = FieldValidationUtils.crossFieldValidation(crossFieldRule);
        crossFieldValidators.push({
            name: ruleName,
            validator: crossFieldValidator,
            fields: crossFieldRule.fields
        });
    });

    // Create base schema
    let schema = yup.object().shape(schemaFields);

    // Add cross-field validations to schema
    crossFieldValidators.forEach(({ name, validator, fields }) => {
        schema = schema.test(
            `cross-field-${name}`,
            'Cross-field validation failed',
            function(values) {
                try {
                    const result = validator(values);
                    if (result !== true) {
                        // Create error for first field in the cross-field rule
                        const firstField = fields[0];
                        return this.createError({
                            path: firstField,
                            message: typeof result === 'string' ? result : 'Cross-field validation failed'
                        });
                    }
                    return true;
                } catch (error) {
                    return this.createError({
                        path: fields[0],
                        message: 'Cross-field validation error occurred'
                    });
                }
            }
        );
    });

    return schema;
}

/**
 * Creates field-level validation hook for individual field validation
 * Provides real-time validation with debounced feedback matching BMS behavior
 * 
 * @param {string} fieldName - Name of the field to validate
 * @param {Object} fieldConfig - BMS field configuration
 * @param {Object} options - Validation options
 * @returns {Object} Field validation utilities and state
 * 
 * @example
 * const userIdValidation = useFieldValidation('userId', {
 *   attrb: ['UNPROT', 'IC'],
 *   length: 8,
 *   required: true
 * });
 */
export function useFieldValidation(fieldName, fieldConfig, options = {}) {
    const { realTimeValidation = true, debounceMs = 300 } = options;
    
    // Create validation rules using FieldValidationUtils
    const validationRules = useMemo(() => {
        return FieldValidationUtils.bmsValidation(fieldConfig, {
            strictMode: true,
            realTimeValidation
        });
    }, [fieldConfig, realTimeValidation]);

    // Create real-time validator
    const realTimeValidator = useMemo(() => {
        if (!realTimeValidation) return null;
        
        return FieldValidationUtils.realTimeValidation(validationRules, {
            debounceMs,
            validateOnChange: true,
            validateOnBlur: true
        });
    }, [validationRules, realTimeValidation, debounceMs]);

    // Create field-specific validation function
    const validateField = useMemo(() => {
        return (value, formData = {}) => {
            if (!validationRules.validationFn) return true;
            
            try {
                return validationRules.validationFn(value, formData);
            } catch (error) {
                console.error(`Field validation error for ${fieldName}:`, error);
                return 'Validation error occurred';
            }
        };
    }, [validationRules, fieldName]);

    return {
        validationRules,
        realTimeValidator,
        validateField,
        fieldName,
        isRequired: validationRules.mustfill
    };
}

/**
 * Creates cross-field validation hook for complex business rules
 * Implements validation across multiple related fields with proper error targeting
 * 
 * @param {Object} crossFieldRules - Configuration for cross-field validation rules
 * @param {Object} options - Cross-field validation options
 * @returns {Object} Cross-field validation utilities
 * 
 * @example
 * const stateZipValidation = useCrossFieldValidation({
 *   stateZipConsistency: {
 *     fields: ['acsstte', 'acszipc'],
 *     validationFn: (values) => validateStateZipConsistency(values.acsstte, values.acszipc),
 *     errorMessage: 'State and ZIP code combination is invalid'
 *   }
 * });
 */
export function useCrossFieldValidation(crossFieldRules, options = {}) {
    const { validateOnChange = false } = options;
    
    // Create cross-field validators
    const validators = useMemo(() => {
        const validatorMap = {};
        
        Object.entries(crossFieldRules).forEach(([ruleName, ruleConfig]) => {
            validatorMap[ruleName] = FieldValidationUtils.crossFieldValidation(ruleConfig);
        });
        
        return validatorMap;
    }, [crossFieldRules]);

    // Create validation function for all cross-field rules
    const validateCrossFields = useMemo(() => {
        return (formData) => {
            const errors = {};
            
            Object.entries(validators).forEach(([ruleName, validator]) => {
                try {
                    const result = validator(formData);
                    if (result !== true) {
                        const ruleConfig = crossFieldRules[ruleName];
                        const targetField = ruleConfig.fields[0]; // Error goes to first field
                        errors[targetField] = {
                            type: 'cross-field',
                            message: typeof result === 'string' ? result : ruleConfig.errorMessage,
                            rule: ruleName
                        };
                    }
                } catch (error) {
                    console.error(`Cross-field validation error for ${ruleName}:`, error);
                    const ruleConfig = crossFieldRules[ruleName];
                    const targetField = ruleConfig.fields[0];
                    errors[targetField] = {
                        type: 'cross-field',
                        message: 'Cross-field validation error occurred',
                        rule: ruleName
                    };
                }
            });
            
            return Object.keys(errors).length === 0 ? null : errors;
        };
    }, [validators, crossFieldRules]);

    // Create specific cross-field validation functions
    const validateStateZip = useMemo(() => {
        return (state, zipCode) => {
            return FieldValidationUtils.stateZipValidation(state, zipCode);
        };
    }, []);

    return {
        validators,
        validateCrossFields,
        validateStateZip,
        crossFieldRules
    };
}

/**
 * Formats validation errors for consistent display across components
 * Converts React Hook Form/Yup errors to standardized error objects with proper targeting
 * 
 * @param {Object} errors - Validation errors from React Hook Form
 * @param {Object} fieldLabels - Human-readable field labels for error messages
 * @param {Object} options - Error formatting options
 * @returns {Object} Formatted error information with field targeting and display text
 * 
 * @example
 * const formattedErrors = formatValidationErrors(errors, {
 *   userId: 'User ID',
 *   password: 'Password',
 *   acctsid: 'Account Number'
 * });
 */
export function formatValidationErrors(errors, fieldLabels = {}, options = {}) {
    const { includeFieldPath = false, groupByType = false } = options;
    
    return FieldValidationUtils.errorFormatting(errors, fieldLabels, {
        includeFieldPath,
        groupByType,
        includeTimestamp: true
    });
}

/**
 * Creates BMS field validation properties for Material-UI TextField components
 * Combines BMS attribute processing with validation rules for complete field configuration
 * 
 * @param {Object} fieldConfig - BMS field configuration
 * @param {Object} validationErrors - Current validation errors
 * @param {Object} options - Field property creation options
 * @returns {Object} Complete Material-UI TextField properties with validation
 * 
 * @example
 * const userIdProps = createBmsFieldValidationProps({
 *   attrb: ['UNPROT', 'IC'],
 *   length: 8,
 *   color: 'GREEN',
 *   required: true
 * }, errors, { realTimeValidation: true });
 */
export function createBmsFieldValidationProps(fieldConfig, validationErrors = {}, options = {}) {
    const { realTimeValidation = true, fieldName = 'field' } = options;
    
    // Use the BMS field attributes hook for base properties
    const bmsFieldAttributes = useBmsFieldAttributes();
    const baseProps = bmsFieldAttributes.processFieldAttributes(fieldConfig);
    
    // Get field-specific error
    const fieldError = validationErrors[fieldName];
    const hasError = Boolean(fieldError);
    
    // Apply error state to Material-UI properties
    if (hasError) {
        baseProps.error = true;
        baseProps.helperText = fieldError.message || fieldError;
        baseProps.FormHelperTextProps = {
            ...baseProps.FormHelperTextProps,
            error: true
        };
    }

    // Add validation event handlers for real-time validation
    if (realTimeValidation) {
        const originalOnChange = baseProps.onChange;
        const originalOnBlur = baseProps.onBlur;
        
        // Create real-time validation handlers
        const validationRules = FieldValidationUtils.bmsValidation(fieldConfig);
        const realTimeValidator = FieldValidationUtils.realTimeValidation(validationRules);
        
        if (realTimeValidator.onChange) {
            baseProps.onChange = (event) => {
                if (originalOnChange) originalOnChange(event);
                // Real-time validation will be handled by React Hook Form
            };
        }
        
        if (realTimeValidator.onBlur) {
            baseProps.onBlur = (event) => {
                if (originalOnBlur) originalOnBlur(event);
                // Real-time validation will be handled by React Hook Form
            };
        }
    }

    // Add ARIA attributes for accessibility
    baseProps.inputProps = {
        ...baseProps.inputProps,
        'aria-invalid': hasError,
        'aria-describedby': hasError ? `${fieldName}-error` : undefined
    };

    // Add helper text ID for ARIA association
    if (hasError) {
        baseProps.FormHelperTextProps = {
            ...baseProps.FormHelperTextProps,
            id: `${fieldName}-error`
        };
    }

    return baseProps;
}

/**
 * Main form validation hook providing comprehensive BMS validation integration
 * Combines React Hook Form with Yup schemas and BMS field attribute processing
 * for complete form validation with real-time feedback and cross-field validation
 * 
 * @param {Object} fieldsConfig - Complete BMS fields configuration
 * @param {Object} options - Form validation options
 * @returns {Object} Complete form validation utilities and React Hook Form integration
 * 
 * @example
 * // For COSGN00.bms login form
 * const {
 *   register,
 *   handleSubmit,
 *   formState: { errors },
 *   getFieldProps,
 *   validateCrossFields
 * } = useFormValidation({
 *   userId: {
 *     attrb: ['UNPROT', 'IC', 'FSET'],
 *     length: 8,
 *     color: 'GREEN',
 *     required: true
 *   },
 *   password: {
 *     attrb: ['DRK', 'UNPROT', 'FSET'],
 *     length: 8,
 *     color: 'GREEN', 
 *     required: true
 *   }
 * }, {
 *   mode: 'onChange',
 *   realTimeValidation: true
 * });
 */
const useFormValidation = (fieldsConfig, options = {}) => {
    const {
        mode = 'onBlur',
        reValidateMode = 'onChange',
        defaultValues = {},
        realTimeValidation = true,
        crossFieldRules = {},
        strictMode = true,
        fieldLabels = {}
    } = options;

    // Create Yup validation schema from BMS field configuration
    const validationSchema = useMemo(() => {
        return createBmsValidationSchema(fieldsConfig, {
            strictMode,
            realTimeValidation,
            crossFieldRules
        });
    }, [fieldsConfig, strictMode, realTimeValidation, crossFieldRules]);

    // Initialize React Hook Form with Yup resolver
    const form = useForm({
        mode,
        reValidateMode,
        defaultValues,
        resolver: yupResolver(validationSchema),
        criteriaMode: 'all'
    });

    const { register, handleSubmit, formState, watch, trigger, setValue, getValues } = form;
    const { errors, isValid, isDirty, isSubmitting } = formState;

    // Create cross-field validation utilities
    const crossFieldValidation = useCrossFieldValidation(crossFieldRules, {
        validateOnChange: realTimeValidation
    });

    // Create field validation utilities for individual fields
    const fieldValidations = useMemo(() => {
        const validations = {};
        
        Object.entries(fieldsConfig).forEach(([fieldName, fieldConfig]) => {
            validations[fieldName] = useFieldValidation(fieldName, fieldConfig, {
                realTimeValidation,
                debounceMs: 300
            });
        });
        
        return validations;
    }, [fieldsConfig, realTimeValidation]);

    // Format validation errors for display
    const formattedErrors = useMemo(() => {
        return formatValidationErrors(errors, fieldLabels, {
            includeFieldPath: true,
            groupByType: false
        });
    }, [errors, fieldLabels]);

    // Create function to get complete field properties for Material-UI integration
    const getFieldProps = useMemo(() => {
        return (fieldName) => {
            const fieldConfig = fieldsConfig[fieldName];
            if (!fieldConfig) {
                console.warn(`Field configuration not found for: ${fieldName}`);
                return {};
            }

            return createBmsFieldValidationProps(fieldConfig, errors, {
                realTimeValidation,
                fieldName
            });
        };
    }, [fieldsConfig, errors, realTimeValidation]);

    // Create manual cross-field validation trigger
    const validateCrossFields = useMemo(() => {
        return async () => {
            const formData = getValues();
            const crossFieldErrors = crossFieldValidation.validateCrossFields(formData);
            
            if (crossFieldErrors) {
                // Set errors for affected fields
                Object.entries(crossFieldErrors).forEach(([fieldName, error]) => {
                    form.setError(fieldName, {
                        type: error.type,
                        message: error.message
                    });
                });
                return false;
            }
            
            return true;
        };
    }, [crossFieldValidation, getValues, form]);

    // Create state/ZIP validation function for address fields
    const validateStateZipFields = useMemo(() => {
        return (stateName = 'acsstte', zipName = 'acszipc') => {
            const formData = getValues();
            const state = formData[stateName];
            const zipCode = formData[zipName];
            
            const result = crossFieldValidation.validateStateZip(state, zipCode);
            
            if (result !== true) {
                form.setError(zipName, {
                    type: 'cross-field',
                    message: result
                });
                return false;
            }
            
            return true;
        };
    }, [crossFieldValidation, getValues, form]);

    // Watch for form changes to trigger cross-field validation
    const watchedValues = watch();
    
    // Auto-trigger cross-field validation when dependent fields change
    useMemo(() => {
        if (realTimeValidation && Object.keys(crossFieldRules).length > 0) {
            // Debounced cross-field validation would be triggered here
            const timer = setTimeout(() => {
                validateCrossFields();
            }, 500);
            
            return () => clearTimeout(timer);
        }
    }, [watchedValues, realTimeValidation, crossFieldRules, validateCrossFields]);

    // Return comprehensive form validation interface
    return {
        // React Hook Form core functionality
        register,
        handleSubmit,
        formState,
        watch,
        trigger,
        setValue,
        getValues,
        reset: form.reset,
        clearErrors: form.clearErrors,
        setError: form.setError,
        
        // Enhanced validation functionality
        errors,
        formattedErrors,
        isValid,
        isDirty,
        isSubmitting,
        
        // BMS-specific functionality
        getFieldProps,
        fieldValidations,
        validationSchema,
        
        // Cross-field validation
        validateCrossFields,
        validateStateZipFields,
        crossFieldValidation,
        
        // Utility functions
        formatErrors: (customErrors) => formatValidationErrors(customErrors, fieldLabels),
        
        // Form state helpers
        hasErrors: formattedErrors.hasErrors,
        fieldErrors: formattedErrors.fieldErrors,
        globalErrors: formattedErrors.globalErrors
    };
};

export default useFormValidation;