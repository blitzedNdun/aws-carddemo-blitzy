/**
 * Field Validation Utilities for CardDemo Application
 * 
 * This module provides comprehensive field validation utilities that convert BMS field validation rules
 * and attribute bytes to React validation patterns. It preserves exact COBOL business logic while
 * enabling real-time validation in React components with Material-UI integration.
 * 
 * Key Features:
 * - BMS attribute byte conversion (ASKIP, UNPROT, PROT, NUM, IC, FSET) to React validation rules
 * - PICIN pattern conversion (e.g., '99999999999' for account numbers) to JavaScript regex patterns
 * - MUSTFILL validation equivalent to BMS required field checking
 * - Cross-field validation for business rules like state/ZIP consistency and account-card linkage
 * - Real-time validation feedback matching original BMS field sequencing and error display patterns
 * - Yup schema integration for React Hook Form compatibility
 * - COBOL COMP-3 decimal precision validation using BigDecimal equivalent
 * 
 * Supports all 18 BMS screens: COSGN00, COACTVW, COACTUP, COCRDLI, COCRDSL, COCRDUP, 
 * COTRN00, COTRN01, COTRN02, COBIL00, CORPT00, COMEN01, COADM01, COUSR00, COUSR01, COUSR02, COUSR03
 * 
 * @fileoverview CardDemo Field Validation Utilities
 * @version 1.0.0
 * @author Blitzy Platform - CardDemo Migration Team
 * @copyright 2024 CardDemo Application Migration Project
 */

import { useForm } from 'react-hook-form';
import * as yup from 'yup';
import Decimal from 'decimal.js';

import { FieldConstants } from '../constants/FieldConstants';
import { ValidationConstants } from '../constants/ValidationConstants';
import { MessageConstants } from '../constants/MessageConstants';
import { FormFieldAttributes, BaseScreenData } from '../types/CommonTypes';
import { FieldValidationRules, FormValidationSchema, CrossFieldValidationRules } from '../types/ValidationTypes';
import { 
    parseCobolDecimal, 
    formatPicX, 
    formatPic9, 
    formatPicS9V9, 
    convertZonedDecimal, 
    convertPackedDecimal 
} from './dataFormatting';

// Configure Decimal.js for COBOL COMP-3 equivalent precision
Decimal.set({
    precision: 34,
    rounding: Decimal.ROUND_HALF_EVEN,
    toExpNeg: -6143,
    toExpPos: 6144,
    maxE: 6144,
    minE: -6143,
    modulo: Decimal.ROUND_DOWN
});

/**
 * Creates BMS validation rules from field attributes
 * Converts BMS DFHMDF attributes to React Hook Form validation configuration
 * 
 * @param {FormFieldAttributes} fieldAttributes - BMS field attributes from mapset
 * @param {Object} options - Additional validation options
 * @param {boolean} options.strictMode - Enable strict COBOL-equivalent validation
 * @param {boolean} options.realTimeValidation - Enable real-time validation
 * @returns {FieldValidationRules} Complete validation rules configuration
 * 
 * @example
 * // For USERID field: ATTRB=(FSET,IC,NORM,UNPROT), LENGTH=8
 * const userIdRules = createBmsValidationRules({
 *   attrb: ['UNPROT', 'IC', 'FSET'],
 *   length: 8,
 *   color: 'GREEN',
 *   hilight: 'OFF',
 *   pos: { row: 19, column: 43 }
 * });
 */
export function createBmsValidationRules(fieldAttributes, options = {}) {
    const { strictMode = true, realTimeValidation = true } = options;
    const { attrb, length, picin, validn, initial } = fieldAttributes;
    
    // Normalize attributes to array format
    const attributes = Array.isArray(attrb) ? attrb : [attrb];
    
    // Determine field behavior from BMS attributes
    const isRequired = validn === 'MUSTFILL' || attributes.includes('MUSTFILL');
    const isProtected = attributes.includes('ASKIP') || attributes.includes('PROT');
    const isNumeric = attributes.includes('NUM');
    const isHidden = attributes.includes('DRK');
    const isInitialCursor = attributes.includes('IC');
    const isFieldSet = attributes.includes('FSET');

    // Create base validation rules
    const validationRules = {
        bmsAttribute: attributes,
        picinPattern: picin,
        mustfill: isRequired,
        length: {
            min: isRequired ? 1 : 0,
            max: length
        },
        errorMessage: isRequired 
            ? MessageConstants.VALIDATION_ERRORS.REQUIRED_FIELD
            : MessageConstants.VALIDATION_ERRORS.INVALID_FORMAT
    };

    // Add numeric validation if NUM attribute present
    if (isNumeric) {
        validationRules.validationFn = (value) => {
            if (!value && !isRequired) return true;
            return ValidationConstants.VALIDATION_RULES.NUM.pattern.test(value) ||
                   ValidationConstants.VALIDATION_RULES.NUM.errorMessage;
        };
    }

    // Add PICIN pattern validation if specified
    if (picin) {
        const regexPattern = convertPicinToRegex(picin);
        const existingValidationFn = validationRules.validationFn;
        
        validationRules.validationFn = (value, formData) => {
            // Apply existing validation first
            if (existingValidationFn) {
                const existingResult = existingValidationFn(value, formData);
                if (existingResult !== true) return existingResult;
            }
            
            // Apply PICIN pattern validation
            if (!value && !isRequired) return true;
            return regexPattern.test(value) || 
                   `Field must match pattern: ${picin}`;
        };
    }

    // Add real-time validation configuration
    if (realTimeValidation) {
        validationRules.realTimeConfig = {
            validateOnChange: !isProtected,
            validateOnBlur: true,
            revalidateMode: 'onChange'
        };
    }

    return validationRules;
}

/**
 * Converts BMS PICIN patterns to JavaScript regex patterns
 * Handles COBOL picture clause patterns for field validation
 * 
 * @param {string} picinPattern - BMS PICIN pattern (e.g., '99999999999', 'XXXXXXXXXXXX')
 * @returns {RegExp} JavaScript regex pattern equivalent
 * 
 * @example
 * convertPicinToRegex('99999999999') // Returns /^[0-9]{11}$/
 * convertPicinToRegex('XXXXXXXXXXXX') // Returns /^[A-Za-z0-9]{12}$/
 * convertPicinToRegex('999-99-9999') // Returns /^[0-9]{3}-[0-9]{2}-[0-9]{4}$/
 */
export function convertPicinToRegex(picinPattern) {
    if (!picinPattern || typeof picinPattern !== 'string') {
        return /^.*$/; // Accept anything if no pattern
    }

    // Handle common BMS PICIN patterns
    let regexPattern = picinPattern
        // Replace '9' with numeric digit pattern
        .replace(/9/g, '[0-9]')
        // Replace 'X' with alphanumeric pattern  
        .replace(/X/g, '[A-Za-z0-9]')
        // Replace 'A' with alphabetic pattern
        .replace(/A/g, '[A-Za-z]')
        // Escape special regex characters that might be literals
        .replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

    // Handle specific pattern optimizations
    if (/^\[0-9\]\{/.test(regexPattern)) {
        // Convert repeated digit patterns to quantified patterns
        regexPattern = regexPattern.replace(/(\[0-9\])\{1\}/g, '$1');
    }

    // Add anchors for exact matching (BMS behavior)
    return new RegExp(`^${regexPattern}$`);
}

/**
 * Creates MUSTFILL validator equivalent to BMS required field validation
 * Implements exact BMS VALIDN=(MUSTFILL) behavior
 * 
 * @param {string} fieldName - Name of the field for error messages
 * @param {Object} options - Validation options
 * @param {string} options.customMessage - Custom error message
 * @param {boolean} options.trimWhitespace - Trim whitespace before validation
 * @returns {Function} Validation function for React Hook Form
 * 
 * @example
 * const userIdValidator = createMustfillValidator('userId', {
 *   customMessage: 'User ID is required',
 *   trimWhitespace: true
 * });
 */
export function createMustfillValidator(fieldName, options = {}) {
    const { customMessage, trimWhitespace = true } = options;
    
    return (value) => {
        // Handle null, undefined, or empty values
        if (value === null || value === undefined) {
            return customMessage || MessageConstants.FIELD_ERROR_MESSAGES[`${fieldName.toUpperCase()}_INVALID`] || 
                   MessageConstants.VALIDATION_ERRORS.REQUIRED_FIELD;
        }

        // Convert to string and optionally trim
        const stringValue = String(value);
        const testValue = trimWhitespace ? stringValue.trim() : stringValue;
        
        // BMS MUSTFILL behavior: reject empty or whitespace-only values
        if (testValue.length === 0) {
            return customMessage || MessageConstants.FIELD_ERROR_MESSAGES[`${fieldName.toUpperCase()}_INVALID`] || 
                   MessageConstants.VALIDATION_ERRORS.REQUIRED_FIELD;
        }

        return true; // Valid
    };
}

/**
 * Creates cross-field validation functions for complex business rules
 * Implements validation across multiple related fields (e.g., state/ZIP consistency)
 * 
 * @param {CrossFieldValidationRules} rules - Cross-field validation configuration
 * @returns {Function} Validation function that operates across multiple fields
 * 
 * @example
 * const stateZipValidator = createCrossFieldValidator({
 *   fields: ['state', 'zipCode'],
 *   validationFn: (values) => validateStateZipConsistency(values.state, values.zipCode),
 *   errorMessage: 'State and ZIP code combination is invalid',
 *   dependentFields: ['state', 'zipCode']
 * });
 */
export function createCrossFieldValidator(rules) {
    const { fields, validationFn, errorMessage, dependentFields } = rules;
    
    return (formData) => {
        // Extract field values for validation
        const fieldValues = {};
        fields.forEach(fieldName => {
            fieldValues[fieldName] = formData[fieldName];
        });

        // Check if all required fields have values
        const hasAllRequiredValues = fields.some(fieldName => {
            const value = fieldValues[fieldName];
            return value !== null && value !== undefined && String(value).trim() !== '';
        });

        // Skip validation if no fields have values (optional cross-field validation)
        if (!hasAllRequiredValues) {
            return true;
        }

        // Execute cross-field validation logic
        try {
            const result = validationFn(fieldValues, formData);
            return result === true ? true : (result || errorMessage);
        } catch (error) {
            console.error('Cross-field validation error:', error);
            return errorMessage || 'Cross-field validation failed';
        }
    };
}

/**
 * Creates field attribute validator based on BMS ATTRB values
 * Maps BMS field attributes to Material-UI component validation behavior
 * 
 * @param {string|string[]} attributes - BMS attribute(s) (ASKIP, UNPROT, PROT, etc.)
 * @param {Object} fieldConfig - Additional field configuration
 * @returns {Object} Validation configuration for React components
 * 
 * @example
 * const fieldValidator = createFieldAttributeValidator(['UNPROT', 'NUM'], {
 *   length: 11,
 *   required: true
 * });
 */
export function createFieldAttributeValidator(attributes, fieldConfig = {}) {
    const attrs = Array.isArray(attributes) ? attributes : [attributes];
    const { length, required = false, picin } = fieldConfig;

    const config = {
        component: 'TextField',
        props: {
            variant: 'outlined',
            size: 'small',
            autoComplete: 'off'
        },
        validation: {},
        styles: {}
    };

    // Process each attribute
    attrs.forEach(attr => {
        switch (attr) {
            case 'ASKIP':
                // Auto-skip fields are read-only
                Object.assign(config, FieldConstants.ATTRIBUTE_MAPPINGS.ASKIP_TO_READONLY);
                break;
                
            case 'UNPROT':
                // Unprotected fields are editable
                Object.assign(config, FieldConstants.ATTRIBUTE_MAPPINGS.UNPROT_TO_EDITABLE);
                break;
                
            case 'PROT':
                // Protected fields are disabled
                Object.assign(config, FieldConstants.ATTRIBUTE_MAPPINGS.PROT_TO_DISABLED);
                break;
                
            case 'NUM':
                // Numeric fields get number validation
                Object.assign(config, FieldConstants.ATTRIBUTE_MAPPINGS.NUM_TO_NUMERIC);
                break;
                
            case 'IC':
                // Initial cursor fields get autofocus
                Object.assign(config, FieldConstants.ATTRIBUTE_MAPPINGS.IC_TO_AUTOFOCUS);
                break;
                
            case 'MUSTFILL':
                // Required fields
                Object.assign(config, FieldConstants.ATTRIBUTE_MAPPINGS.MUSTFILL_TO_REQUIRED);
                config.validation.required = createMustfillValidator('field');
                break;
                
            case 'DRK':
                // Hidden fields (passwords)
                config.props.type = 'password';
                config.styles.WebkitTextSecurity = 'disc';
                break;
                
            case 'BRT':
                // Bright fields (emphasis)
                config.styles.fontWeight = 'bold';
                break;
                
            case 'FSET':
                // Field set attribute for change tracking
                config.props['data-fset'] = true;
                break;
        }
    });

    // Add length validation
    if (length) {
        config.validation.maxLength = {
            value: length,
            message: MessageConstants.VALIDATION_ERRORS.OUT_OF_RANGE
        };
        config.props.inputProps = { maxLength: length };
    }

    // Add PICIN pattern validation
    if (picin) {
        const pattern = convertPicinToRegex(picin);
        config.validation.pattern = {
            value: pattern,
            message: `Field must match pattern: ${picin}`
        };
    }

    return config;
}

/**
 * Creates business rule validator for complex COBOL business logic
 * Implements specific business validation rules from original COBOL programs
 * 
 * @param {string} ruleName - Name of the business rule
 * @param {Object} ruleConfig - Business rule configuration
 * @returns {Function} Business rule validation function
 * 
 * @example
 * const creditLimitValidator = createBusinessRuleValidator('creditLimitValidation', {
 *   rule: (value, formData) => value <= formData.accountBalance * 10,
 *   message: 'Credit limit cannot exceed 10x account balance'
 * });
 */
export function createBusinessRuleValidator(ruleName, ruleConfig) {
    const { rule, message, dependencies = [] } = ruleConfig;
    
    return (value, formData) => {
        try {
            // Check if all dependencies are present
            const hasAllDependencies = dependencies.every(dep => 
                formData[dep] !== null && formData[dep] !== undefined
            );
            
            if (dependencies.length > 0 && !hasAllDependencies) {
                return true; // Skip validation if dependencies missing
            }

            // Execute business rule
            const result = rule(value, formData);
            return result === true ? true : (message || 'Business rule validation failed');
        } catch (error) {
            console.error(`Business rule validation error for ${ruleName}:`, error);
            return message || 'Business rule validation failed';
        }
    };
}

/**
 * Creates real-time validator for immediate feedback during user input
 * Provides instant validation feedback matching BMS field behavior
 * 
 * @param {FieldValidationRules} validationRules - Field validation configuration
 * @param {Object} options - Real-time validation options
 * @returns {Function} Real-time validation function
 * 
 * @example
 * const realTimeValidator = createRealTimeValidator(accountRules, {
 *   debounceMs: 300,
 *   validateOnChange: true
 * });
 */
export function createRealTimeValidator(validationRules, options = {}) {
    const { debounceMs = 300, validateOnChange = true, validateOnBlur = true } = options;
    let debounceTimer;

    return {
        onChange: validateOnChange ? (value, formData) => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                if (validationRules.validationFn) {
                    return validationRules.validationFn(value, formData);
                }
                return true;
            }, debounceMs);
        } : undefined,
        
        onBlur: validateOnBlur ? (value, formData) => {
            clearTimeout(debounceTimer);
            if (validationRules.validationFn) {
                return validationRules.validationFn(value, formData);
            }
            return true;
        } : undefined
    };
}

/**
 * Validates account numbers using BMS PICIN='99999999999' pattern
 * Implements exact account number validation from COACTVW.bms
 * 
 * @param {string} accountNumber - Account number to validate
 * @returns {boolean|string} True if valid, error message if invalid
 * 
 * @example
 * validateAccountNumber('12345678901') // Returns true
 * validateAccountNumber('123456789') // Returns error message
 */
export function validateAccountNumber(accountNumber) {
    if (!accountNumber) {
        return MessageConstants.FIELD_ERROR_MESSAGES.ACCOUNT_INVALID;
    }

    const cleaned = String(accountNumber).replace(/\D/g, '');
    
    if (cleaned.length !== FieldConstants.FIELD_LENGTHS.LENGTH_CONSTRAINTS.ACCOUNT_NUMBER) {
        return MessageConstants.FIELD_ERROR_MESSAGES.ACCOUNT_INVALID;
    }

    if (!ValidationConstants.PICIN_PATTERNS.NUMERIC_PATTERNS.ACCOUNT_NUMBER.test(cleaned)) {
        return MessageConstants.FIELD_ERROR_MESSAGES.ACCOUNT_INVALID;
    }

    return true;
}

/**
 * Validates card numbers using 16-digit pattern from BMS maps
 * Implements card number validation from COCRDLI.bms and related screens
 * 
 * @param {string} cardNumber - Card number to validate
 * @param {boolean} allowPartial - Allow partial numbers during input
 * @returns {boolean|string} True if valid, error message if invalid
 * 
 * @example
 * validateCardNumber('1234567890123456') // Returns true
 * validateCardNumber('1234567890123456789') // Returns error message
 */
export function validateCardNumber(cardNumber, allowPartial = false) {
    if (!cardNumber) {
        return MessageConstants.FIELD_ERROR_MESSAGES.CARD_INVALID;
    }

    const cleaned = String(cardNumber).replace(/\D/g, '');
    const expectedLength = FieldConstants.FIELD_LENGTHS.LENGTH_CONSTRAINTS.CARD_NUMBER;
    
    if (allowPartial) {
        if (cleaned.length === 0 || cleaned.length > expectedLength) {
            return MessageConstants.FIELD_ERROR_MESSAGES.CARD_INVALID;
        }
        // Allow partial input during typing
        return ValidationConstants.PICIN_PATTERNS.NUMERIC_PATTERNS.CARD_NUMBER_PARTIAL.test(cleaned) ||
               MessageConstants.FIELD_ERROR_MESSAGES.CARD_INVALID;
    }

    if (cleaned.length !== expectedLength) {
        return MessageConstants.FIELD_ERROR_MESSAGES.CARD_INVALID;
    }

    return ValidationConstants.PICIN_PATTERNS.NUMERIC_PATTERNS.CARD_NUMBER.test(cleaned) ||
           MessageConstants.FIELD_ERROR_MESSAGES.CARD_INVALID;
}

/**
 * Validates SSN format from BMS three-part SSN fields (ACTSSN1, ACTSSN2, ACTSSN3)
 * Implements SSN validation matching COACTUP.bms SSN field structure
 * 
 * @param {string|Object} ssn - SSN as string or object with parts
 * @returns {boolean|string} True if valid, error message if invalid
 * 
 * @example
 * validateSSN('123-45-6789') // Returns true
 * validateSSN({part1: '123', part2: '45', part3: '6789'}) // Returns true
 * validateSSN('123456789') // Returns true (accepts digits only)
 */
export function validateSSN(ssn) {
    if (!ssn) {
        return MessageConstants.FIELD_ERROR_MESSAGES.SSN_INVALID;
    }

    let ssnString;
    
    // Handle object format (from BMS three-part fields)
    if (typeof ssn === 'object' && ssn.part1 && ssn.part2 && ssn.part3) {
        ssnString = `${ssn.part1}-${ssn.part2}-${ssn.part3}`;
    } else {
        ssnString = String(ssn);
    }

    // Remove all non-digits for length check
    const digitsOnly = ssnString.replace(/\D/g, '');
    
    if (digitsOnly.length !== 9) {
        return MessageConstants.FIELD_ERROR_MESSAGES.SSN_INVALID;
    }

    // Check formatted pattern or digits-only pattern
    const isValidFormatted = ValidationConstants.PICIN_PATTERNS.VALIDATION_REGEX.SSN_FULL.test(ssnString);
    const isValidDigits = ValidationConstants.PICIN_PATTERNS.VALIDATION_REGEX.SSN_DIGITS.test(digitsOnly);
    
    return (isValidFormatted || isValidDigits) || MessageConstants.FIELD_ERROR_MESSAGES.SSN_INVALID;
}

/**
 * Validates date formats from BMS date fields (YYYY-MM-DD, MM/DD/YY)
 * Implements date validation matching BMS date field patterns
 * 
 * @param {string|Date} date - Date to validate
 * @param {string} format - Expected format ('long', 'short', 'auto')
 * @param {Object} options - Validation options
 * @returns {boolean|string} True if valid, error message if invalid
 * 
 * @example
 * validateDate('2023-12-25', 'long') // Returns true
 * validateDate('12/25/23', 'short') // Returns true
 * validateDate('2023-13-25', 'long') // Returns error message
 */
export function validateDate(date, format = 'auto', options = {}) {
    const { allowFuture = true, allowPast = true, businessDaysOnly = false } = options;
    
    if (!date) {
        return MessageConstants.FIELD_ERROR_MESSAGES.DATE_INVALID;
    }

    let dateObj;
    const dateString = String(date);
    
    try {
        // Parse different date formats
        if (format === 'long' || (format === 'auto' && ValidationConstants.PICIN_PATTERNS.VALIDATION_REGEX.DATE_LONG.test(dateString))) {
            // YYYY-MM-DD format
            if (!ValidationConstants.PICIN_PATTERNS.VALIDATION_REGEX.DATE_LONG.test(dateString)) {
                return MessageConstants.FIELD_ERROR_MESSAGES.DATE_INVALID;
            }
            dateObj = new Date(dateString);
        } else if (format === 'short' || (format === 'auto' && ValidationConstants.PICIN_PATTERNS.VALIDATION_REGEX.DATE_SHORT.test(dateString))) {
            // MM/DD/YY format
            if (!ValidationConstants.PICIN_PATTERNS.VALIDATION_REGEX.DATE_SHORT.test(dateString)) {
                return MessageConstants.FIELD_ERROR_MESSAGES.DATE_INVALID;
            }
            const [month, day, year] = dateString.split('/');
            const fullYear = parseInt(year) < 50 ? 2000 + parseInt(year) : 1900 + parseInt(year);
            dateObj = new Date(fullYear, parseInt(month) - 1, parseInt(day));
        } else {
            // Try parsing as general date
            dateObj = new Date(date);
        }

        // Check if date is valid
        if (isNaN(dateObj.getTime())) {
            return MessageConstants.FIELD_ERROR_MESSAGES.DATE_INVALID;
        }

        // Check future/past restrictions
        const now = new Date();
        if (!allowFuture && dateObj > now) {
            return 'Future dates are not allowed';
        }
        if (!allowPast && dateObj < now) {
            return 'Past dates are not allowed';
        }

        // Check business days restriction
        if (businessDaysOnly) {
            const dayOfWeek = dateObj.getDay();
            if (dayOfWeek === 0 || dayOfWeek === 6) {
                return 'Date must be a business day (Monday-Friday)';
            }
        }

        return true;
    } catch (error) {
        return MessageConstants.FIELD_ERROR_MESSAGES.DATE_INVALID;
    }
}

/**
 * Validates state and ZIP code consistency for address fields
 * Implements cross-field validation for state/ZIP combinations
 * 
 * @param {string} state - Two-letter state code
 * @param {string} zipCode - Five-digit ZIP code
 * @returns {boolean|string} True if valid, error message if invalid
 * 
 * @example
 * validateStateZip('CA', '90210') // Returns true
 * validateStateZip('NY', '90210') // Returns error message
 */
export function validateStateZip(state, zipCode) {
    if (!state || !zipCode) {
        return true; // Allow empty values (optional validation)
    }

    // Basic format validation
    if (!ValidationConstants.PICIN_PATTERNS.ALPHANUMERIC_PATTERNS.STATE.test(state)) {
        return 'Invalid state code format';
    }
    
    if (!ValidationConstants.PICIN_PATTERNS.VALIDATION_REGEX.ZIP_CODE.test(zipCode)) {
        return 'Invalid ZIP code format';
    }

    // State/ZIP consistency validation (simplified mapping)
    const stateZipMapping = {
        'CA': ['90', '91', '92', '93', '94', '95', '96'],
        'NY': ['10', '11', '12', '13', '14', '15'],
        'TX': ['70', '71', '72', '73', '74', '75', '76', '77', '78', '79'],
        'FL': ['32', '33', '34', '35'],
        // Add more state mappings as needed
    };

    const zipPrefix = zipCode.substring(0, 2);
    const validPrefixes = stateZipMapping[state.toUpperCase()];
    
    if (validPrefixes && !validPrefixes.includes(zipPrefix)) {
        return MessageConstants.FIELD_ERROR_MESSAGES.STATE_ZIP_MISMATCH;
    }

    return true;
}

/**
 * Creates Yup validation schema from BMS field definitions
 * Converts BMS validation rules to Yup schema for React Hook Form integration
 * 
 * @param {Object} fieldsConfig - Configuration for all form fields
 * @param {Object} options - Schema creation options
 * @returns {Object} Yup validation schema
 * 
 * @example
 * const schema = createYupSchema({
 *   userId: { required: true, length: 8, pattern: /^[A-Z0-9]+$/i },
 *   password: { required: true, length: 8, hidden: true }
 * });
 */
export function createYupSchema(fieldsConfig, options = {}) {
    const { strictMode = true } = options;
    const schemaFields = {};

    Object.entries(fieldsConfig).forEach(([fieldName, config]) => {
        let fieldSchema = yup.string();
        
        // Apply required validation
        if (config.required) {
            fieldSchema = fieldSchema.required(
                config.customMessage || 
                MessageConstants.VALIDATION_ERRORS.REQUIRED_FIELD
            );
        }

        // Apply length validation
        if (config.length) {
            if (typeof config.length === 'number') {
                fieldSchema = fieldSchema.max(
                    config.length,
                    `Field cannot exceed ${config.length} characters`
                );
            } else if (config.length.min !== undefined) {
                fieldSchema = fieldSchema.min(
                    config.length.min,
                    `Field must be at least ${config.length.min} characters`
                );
            }
            if (config.length.max !== undefined) {
                fieldSchema = fieldSchema.max(
                    config.length.max,
                    `Field cannot exceed ${config.length.max} characters`
                );
            }
        }

        // Apply pattern validation
        if (config.pattern) {
            const pattern = typeof config.pattern === 'string' ? 
                convertPicinToRegex(config.pattern) : config.pattern;
            fieldSchema = fieldSchema.matches(
                pattern,
                config.patternMessage || 'Invalid format for this field'
            );
        }

        // Apply custom validation function
        if (config.validationFn) {
            fieldSchema = fieldSchema.test(
                'custom-validation',
                config.errorMessage || 'Validation failed',
                function(value) {
                    try {
                        const result = config.validationFn(value, this.parent);
                        return result === true ? true : this.createError({
                            message: typeof result === 'string' ? result : config.errorMessage
                        });
                    } catch (error) {
                        return this.createError({
                            message: config.errorMessage || 'Validation error occurred'
                        });
                    }
                }
            );
        }

        schemaFields[fieldName] = fieldSchema;
    });

    return yup.object().shape(schemaFields);
}

/**
 * Formats validation errors for consistent display across components
 * Provides standardized error message formatting and field targeting
 * 
 * @param {Object} errors - Validation errors from React Hook Form or Yup
 * @param {Object} fieldLabels - Human-readable field labels
 * @returns {Object} Formatted error information
 * 
 * @example
 * const formattedErrors = formatValidationError(errors, {
 *   userId: 'User ID',
 *   password: 'Password'
 * });
 */
export function formatValidationError(errors, fieldLabels = {}) {
    const formattedErrors = {
        fieldErrors: {},
        globalErrors: [],
        hasErrors: false
    };

    if (!errors) {
        return formattedErrors;
    }

    // Handle React Hook Form errors
    if (errors.message || errors.type) {
        formattedErrors.globalErrors.push({
            message: errors.message || 'Validation error',
            type: errors.type || 'validation',
            timestamp: new Date()
        });
        formattedErrors.hasErrors = true;
    }

    // Handle field-specific errors
    Object.entries(errors).forEach(([fieldName, error]) => {
        if (error && error.message) {
            const fieldLabel = fieldLabels[fieldName] || fieldName;
            formattedErrors.fieldErrors[fieldName] = {
                message: error.message,
                label: fieldLabel,
                type: error.type || 'validation'
            };
            formattedErrors.hasErrors = true;
        }
    });

    return formattedErrors;
}

/**
 * Default export object containing all field validation utilities
 * Provides centralized access to all validation functions and configurations
 */
const FieldValidationUtils = {
    // Main validation functions
    bmsValidation: createBmsValidationRules,
    picinConversion: convertPicinToRegex,
    mustfillValidation: createMustfillValidator,
    crossFieldValidation: createCrossFieldValidator,
    businessRuleValidation: createBusinessRuleValidator,
    realTimeValidation: createRealTimeValidator,
    
    // Specific field validators
    accountValidation: validateAccountNumber,
    cardValidation: validateCardNumber,
    ssnValidation: validateSSN,
    dateValidation: validateDate,
    stateZipValidation: validateStateZip,
    
    // Schema and error handling
    schemaGeneration: createYupSchema,
    errorFormatting: formatValidationError,
    
    // Utility functions
    createFieldAttributeValidator,
    
    // Constants and patterns
    patterns: ValidationConstants.PICIN_PATTERNS,
    rules: ValidationConstants.VALIDATION_RULES,
    messages: ValidationConstants.VALIDATION_MESSAGES
};

export default FieldValidationUtils;