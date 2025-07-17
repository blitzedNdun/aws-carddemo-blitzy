/**
 * CardDemo Field Validation Utilities
 * 
 * This module provides comprehensive field validation utilities that convert BMS field
 * validation rules and attribute bytes to React validation patterns, preserving exact
 * COBOL business logic while enabling real-time validation in React components with
 * Material-UI integration.
 * 
 * Key Features:
 * - Converts BMS PICIN patterns to JavaScript regex validation
 * - Maps BMS attribute bytes (ASKIP, UNPROT, PROT, NUM, IC, BRT, NORM, FSET) to React field validation
 * - Implements MUSTFILL validation equivalent to BMS required field checking
 * - Creates cross-field validation functions for complex business rules
 * - Provides real-time validation feedback matching original BMS field sequencing
 * - Maintains exact COBOL arithmetic precision for financial calculations
 * 
 * Generated from BMS maps: COSGN00, COACTVW, COACTUP, COCRDLI, COCRDUP, COTRN00-02, COBIL00
 * 
 * @author Blitzy agent
 * @version 1.0.0
 */

import { string, number, object, date, ValidationSchema, reach } from 'yup';
import { Decimal } from 'decimal.js';
import { FieldConstants } from '../constants/FieldConstants';
import { ValidationConstants } from '../constants/ValidationConstants';
import { MessageConstants } from '../constants/MessageConstants';
import { FormFieldAttributes, BaseScreenData } from '../types/CommonTypes';
import { FieldValidationRules, FormValidationSchema, CrossFieldValidationRules } from '../types/ValidationTypes';
import { DataFormatting } from '../utils/dataFormatting';

// Configure Decimal.js to match COBOL COMP-3 precision exactly
Decimal.set({
    precision: 31,
    rounding: Decimal.ROUND_HALF_UP,
    toExpNeg: -7,
    toExpPos: 21,
    minE: -324,
    maxE: 308
});

/**
 * Creates comprehensive validation rules from BMS field attributes
 * Maps BMS ATTRB parameter combinations to React Hook Form validation patterns
 * 
 * @param {FormFieldAttributes} fieldAttributes - BMS field attribute definition
 * @param {string} fieldName - Field name for error message customization
 * @returns {FieldValidationRules} Complete validation rule configuration
 */
export function createBmsValidationRules(fieldAttributes, fieldName) {
    const { BMS_ATTRIBUTES, ATTRIBUTE_MAPPINGS } = FieldConstants;
    const { VALIDATION_RULES, VALIDATION_ERRORS } = ValidationConstants;
    const { FIELD_ERROR_MESSAGES } = MessageConstants;
    
    // Extract BMS attributes from field definition
    const attributes = fieldAttributes.attrb || [];
    const length = fieldAttributes.length || 0;
    const picin = fieldAttributes.picin || '';
    const validn = fieldAttributes.validn || [];
    
    // Initialize validation rules object
    const validationRules = {
        bmsAttribute: attributes,
        picinPattern: picin,
        mustfill: validn.includes('MUSTFILL'),
        length: {
            min: 0,
            max: length
        },
        errorMessage: FIELD_ERROR_MESSAGES[fieldName] || VALIDATION_ERRORS.INVALID_FORMAT
    };
    
    // Apply MUSTFILL validation if present
    if (validationRules.mustfill) {
        validationRules.validationFn = (value) => {
            if (!value || value.toString().trim() === '') {
                return VALIDATION_ERRORS.REQUIRED_FIELD;
            }
            return undefined;
        };
    }
    
    // Apply PICIN pattern validation
    if (picin) {
        const regex = convertPicinToRegex(picin);
        const originalValidation = validationRules.validationFn;
        
        validationRules.validationFn = (value) => {
            // Check required field first
            if (originalValidation) {
                const requiredError = originalValidation(value);
                if (requiredError) return requiredError;
            }
            
            // Check PICIN pattern
            if (value && !regex.test(value.toString())) {
                return `Invalid format. Expected pattern: ${picin}`;
            }
            
            return undefined;
        };
    }
    
    // Apply numeric validation for NUM attribute
    if (attributes.includes(BMS_ATTRIBUTES.NUM)) {
        const originalValidation = validationRules.validationFn;
        
        validationRules.validationFn = (value) => {
            if (originalValidation) {
                const error = originalValidation(value);
                if (error) return error;
            }
            
            if (value && !/^\d+$/.test(value.toString())) {
                return 'Only numeric characters allowed';
            }
            
            return undefined;
        };
    }
    
    // Apply length validation
    if (length > 0) {
        const originalValidation = validationRules.validationFn;
        
        validationRules.validationFn = (value) => {
            if (originalValidation) {
                const error = originalValidation(value);
                if (error) return error;
            }
            
            if (value && value.toString().length > length) {
                return `Maximum length is ${length} characters`;
            }
            
            return undefined;
        };
    }
    
    return validationRules;
}

/**
 * Converts COBOL PICIN patterns to JavaScript regex patterns
 * Maps BMS PICIN='99999999999' style patterns to equivalent regex
 * 
 * @param {string} picinPattern - BMS PICIN pattern (e.g., '99999999999')
 * @returns {RegExp} JavaScript regex pattern for validation
 */
export function convertPicinToRegex(picinPattern) {
    if (!picinPattern) {
        return /^.*$/; // Accept any input if no pattern specified
    }
    
    // Remove quotes if present
    const cleanPattern = picinPattern.replace(/^['"]|['"]$/g, '');
    
    // Convert COBOL PICIN patterns to regex
    let regexPattern = cleanPattern
        // '9' means any digit (0-9)
        .replace(/9/g, '\\d')
        // 'X' means any alphanumeric character
        .replace(/X/g, '[A-Za-z0-9]')
        // 'A' means any alphabetic character
        .replace(/A/g, '[A-Za-z]')
        // 'Z' means any digit or space (for numeric display)
        .replace(/Z/g, '[\\d\\s]')
        // Handle special formatting characters
        .replace(/\./g, '\\.')
        .replace(/\-/g, '\\-')
        .replace(/\(/g, '\\(')
        .replace(/\)/g, '\\)')
        .replace(/\//g, '\\/')
        .replace(/\:/g, '\\:');
    
    // Anchor the pattern to match the entire string
    regexPattern = '^' + regexPattern + '$';
    
    return new RegExp(regexPattern);
}

/**
 * Creates MUSTFILL validator equivalent to BMS VALIDN=(MUSTFILL)
 * Implements exact required field checking behavior from original BMS
 * 
 * @param {string} fieldName - Field name for error message customization
 * @param {string} customMessage - Custom error message (optional)
 * @returns {function} Validation function compatible with React Hook Form
 */
export function createMustfillValidator(fieldName, customMessage) {
    const { VALIDATION_ERRORS } = ValidationConstants;
    const { FIELD_ERROR_MESSAGES } = MessageConstants;
    
    return (value) => {
        // Check for null, undefined, or empty string
        if (value === null || value === undefined) {
            return customMessage || FIELD_ERROR_MESSAGES[fieldName] || VALIDATION_ERRORS.REQUIRED_FIELD;
        }
        
        // Convert to string and check if empty or only whitespace
        const stringValue = value.toString().trim();
        if (stringValue === '') {
            return customMessage || FIELD_ERROR_MESSAGES[fieldName] || VALIDATION_ERRORS.REQUIRED_FIELD;
        }
        
        return undefined; // Field is valid
    };
}

/**
 * Creates cross-field validation functions for complex business rules
 * Implements multi-field validation patterns from original COBOL programs
 * 
 * @param {CrossFieldValidationRules} rules - Cross-field validation rules
 * @returns {function} Validation function that checks multiple fields
 */
export function createCrossFieldValidator(rules) {
    const { VALIDATION_ERRORS } = ValidationConstants;
    
    return (values) => {
        const errors = {};
        
        // Validate each cross-field rule
        for (const rule of rules.rules) {
            const { fields, validator, errorMessage } = rule;
            
            // Extract values for specified fields
            const fieldValues = {};
            for (const field of fields) {
                fieldValues[field] = values[field];
            }
            
            // Run validation function
            const isValid = validator(fieldValues);
            
            if (!isValid) {
                // Add error to primary field
                const primaryField = fields[0];
                errors[primaryField] = errorMessage || VALIDATION_ERRORS.CROSS_FIELD_ERROR;
            }
        }
        
        return Object.keys(errors).length > 0 ? errors : undefined;
    };
}

/**
 * Creates field attribute validator for BMS attribute combinations
 * Maps BMS ATTRB parameters to React component validation properties
 * 
 * @param {string[]} attributes - Array of BMS attribute strings
 * @param {FormFieldAttributes} fieldConfig - Complete field configuration
 * @returns {object} Material-UI TextField props with validation
 */
export function createFieldAttributeValidator(attributes, fieldConfig) {
    const { BMS_ATTRIBUTES, ATTRIBUTE_MAPPINGS } = FieldConstants;
    
    let props = {};
    
    // Process each attribute
    for (const attr of attributes) {
        switch (attr) {
            case BMS_ATTRIBUTES.ASKIP:
                props = { ...props, ...ATTRIBUTE_MAPPINGS.ASKIP_TO_READONLY };
                break;
                
            case BMS_ATTRIBUTES.UNPROT:
                props = { ...props, ...ATTRIBUTE_MAPPINGS.UNPROT_TO_EDITABLE };
                break;
                
            case BMS_ATTRIBUTES.PROT:
                props = { ...props, ...ATTRIBUTE_MAPPINGS.PROT_TO_DISABLED };
                break;
                
            case BMS_ATTRIBUTES.NUM:
                props = { ...props, ...ATTRIBUTE_MAPPINGS.NUM_TO_NUMERIC };
                break;
                
            case BMS_ATTRIBUTES.IC:
                props = { ...props, ...ATTRIBUTE_MAPPINGS.IC_TO_AUTOFOCUS };
                break;
                
            case BMS_ATTRIBUTES.MUSTFILL:
                props = { ...props, ...ATTRIBUTE_MAPPINGS.MUSTFILL_TO_REQUIRED };
                break;
                
            case BMS_ATTRIBUTES.BRT:
                props = { ...props, ...ATTRIBUTE_MAPPINGS.BRT_TO_BOLD };
                break;
                
            case BMS_ATTRIBUTES.NORM:
                props = { ...props, ...ATTRIBUTE_MAPPINGS.NORM_TO_NORMAL };
                break;
                
            case BMS_ATTRIBUTES.DRK:
                props = { ...props, ...ATTRIBUTE_MAPPINGS.DRK_TO_DIMMED };
                break;
                
            case BMS_ATTRIBUTES.FSET:
                props = { ...props, ...ATTRIBUTE_MAPPINGS.FSET_TO_CHANGE_TRACKING };
                break;
        }
    }
    
    // Add field length constraint
    if (fieldConfig.length) {
        props.inputProps = {
            ...props.inputProps,
            maxLength: fieldConfig.length
        };
    }
    
    return props;
}

/**
 * Creates business rule validator for complex validation logic
 * Implements COBOL business rule validation patterns
 * 
 * @param {string} fieldName - Field name for business rule identification
 * @param {object} businessRules - Business rule configuration
 * @returns {function} Validation function implementing business rules
 */
export function createBusinessRuleValidator(fieldName, businessRules) {
    const { VALIDATION_ERRORS } = ValidationConstants;
    
    return (value, allValues) => {
        const rules = businessRules[fieldName];
        
        if (!rules) {
            return undefined; // No business rules for this field
        }
        
        // Execute each business rule
        for (const rule of rules) {
            const isValid = rule.validator(value, allValues);
            
            if (!isValid) {
                return rule.errorMessage || VALIDATION_ERRORS.BUSINESS_RULE_VIOLATION;
            }
        }
        
        return undefined; // All business rules passed
    };
}

/**
 * Creates real-time validator for immediate feedback
 * Provides instant validation feedback matching original BMS field sequencing
 * 
 * @param {FieldValidationRules} validationRules - Field validation configuration
 * @returns {function} Real-time validation function
 */
export function createRealTimeValidator(validationRules) {
    return (value) => {
        // Run validation function if present
        if (validationRules.validationFn) {
            const error = validationRules.validationFn(value);
            if (error) {
                return {
                    isValid: false,
                    error: error
                };
            }
        }
        
        return {
            isValid: true,
            error: null
        };
    };
}

/**
 * Validates account number with exact COBOL business logic
 * Implements 11-digit account number validation with checksum
 * 
 * @param {string} accountNumber - Account number to validate
 * @returns {object} Validation result with error details
 */
export function validateAccountNumber(accountNumber) {
    const { PICIN_PATTERNS } = ValidationConstants;
    const { FIELD_ERROR_MESSAGES } = MessageConstants;
    
    if (!accountNumber || accountNumber.trim() === '') {
        return {
            isValid: false,
            error: FIELD_ERROR_MESSAGES.ACCOUNT_INVALID
        };
    }
    
    // Check 11-digit format
    const pattern = PICIN_PATTERNS.NUMERIC_PATTERNS.ACCOUNT_NUMBER;
    if (!pattern.regex.test(accountNumber)) {
        return {
            isValid: false,
            error: pattern.message
        };
    }
    
    // Validate checksum (simplified Luhn algorithm)
    const digits = accountNumber.split('').map(Number);
    let sum = 0;
    
    for (let i = 0; i < digits.length; i++) {
        let digit = digits[i];
        if (i % 2 === 0) {
            digit *= 2;
            if (digit > 9) {
                digit -= 9;
            }
        }
        sum += digit;
    }
    
    if (sum % 10 !== 0) {
        return {
            isValid: false,
            error: 'Invalid account number checksum'
        };
    }
    
    return {
        isValid: true,
        error: null
    };
}

/**
 * Validates card number with exact COBOL business logic
 * Implements 16-digit card number validation with checksum
 * 
 * @param {string} cardNumber - Card number to validate
 * @returns {object} Validation result with error details
 */
export function validateCardNumber(cardNumber) {
    const { PICIN_PATTERNS } = ValidationConstants;
    const { FIELD_ERROR_MESSAGES } = MessageConstants;
    
    if (!cardNumber || cardNumber.trim() === '') {
        return {
            isValid: false,
            error: FIELD_ERROR_MESSAGES.CARD_INVALID
        };
    }
    
    // Check 16-digit format
    const pattern = PICIN_PATTERNS.NUMERIC_PATTERNS.CARD_NUMBER;
    if (!pattern.regex.test(cardNumber)) {
        return {
            isValid: false,
            error: pattern.message
        };
    }
    
    // Validate using Luhn algorithm
    const digits = cardNumber.split('').map(Number);
    let sum = 0;
    
    for (let i = digits.length - 1; i >= 0; i--) {
        let digit = digits[i];
        if ((digits.length - i) % 2 === 0) {
            digit *= 2;
            if (digit > 9) {
                digit -= 9;
            }
        }
        sum += digit;
    }
    
    if (sum % 10 !== 0) {
        return {
            isValid: false,
            error: 'Invalid card number checksum'
        };
    }
    
    return {
        isValid: true,
        error: null
    };
}

/**
 * Validates Social Security Number with exact COBOL format
 * Implements XXX-XX-XXXX SSN validation matching original system
 * 
 * @param {string} ssn - SSN to validate
 * @returns {object} Validation result with error details
 */
export function validateSSN(ssn) {
    const { PICIN_PATTERNS } = ValidationConstants;
    const { FIELD_ERROR_MESSAGES } = MessageConstants;
    
    if (!ssn || ssn.trim() === '') {
        return {
            isValid: false,
            error: FIELD_ERROR_MESSAGES.SSN_INVALID
        };
    }
    
    // Check SSN format (XXX-XX-XXXX)
    const ssnPattern = /^\d{3}-\d{2}-\d{4}$/;
    if (!ssnPattern.test(ssn)) {
        return {
            isValid: false,
            error: 'SSN must be in XXX-XX-XXXX format'
        };
    }
    
    // Check for invalid patterns
    const parts = ssn.split('-');
    const area = parts[0];
    const group = parts[1];
    const serial = parts[2];
    
    // Area number cannot be 000, 666, or 900-999
    if (area === '000' || area === '666' || (area >= '900' && area <= '999')) {
        return {
            isValid: false,
            error: 'Invalid SSN area number'
        };
    }
    
    // Group number cannot be 00
    if (group === '00') {
        return {
            isValid: false,
            error: 'Invalid SSN group number'
        };
    }
    
    // Serial number cannot be 0000
    if (serial === '0000') {
        return {
            isValid: false,
            error: 'Invalid SSN serial number'
        };
    }
    
    return {
        isValid: true,
        error: null
    };
}

/**
 * Validates date format with exact COBOL calendar logic
 * Implements MM/DD/YYYY date validation preserving COBOL date handling
 * 
 * @param {string} dateString - Date string to validate
 * @returns {object} Validation result with error details
 */
export function validateDate(dateString) {
    const { FIELD_ERROR_MESSAGES } = MessageConstants;
    const { formatDate } = DataFormatting;
    
    if (!dateString || dateString.trim() === '') {
        return {
            isValid: false,
            error: FIELD_ERROR_MESSAGES.DATE_INVALID
        };
    }
    
    // Check date format (MM/DD/YYYY)
    const datePattern = /^(0[1-9]|1[0-2])\/(0[1-9]|[12]\d|3[01])\/(\d{4})$/;
    if (!datePattern.test(dateString)) {
        return {
            isValid: false,
            error: 'Date must be in MM/DD/YYYY format'
        };
    }
    
    // Parse date components
    const [month, day, year] = dateString.split('/').map(Number);
    
    // Create date object and validate
    const date = new Date(year, month - 1, day);
    
    if (date.getFullYear() !== year || 
        date.getMonth() !== (month - 1) || 
        date.getDate() !== day) {
        return {
            isValid: false,
            error: 'Invalid date value'
        };
    }
    
    // Check reasonable date range (1900-2099)
    if (year < 1900 || year > 2099) {
        return {
            isValid: false,
            error: 'Date year must be between 1900 and 2099'
        };
    }
    
    return {
        isValid: true,
        error: null
    };
}

/**
 * Validates state and ZIP code consistency for address validation
 * Implements cross-field validation for state/ZIP business rules
 * 
 * @param {string} state - State code (2 characters)
 * @param {string} zipCode - ZIP code (5 digits)
 * @returns {object} Validation result with error details
 */
export function validateStateZip(state, zipCode) {
    const { FIELD_ERROR_MESSAGES } = MessageConstants;
    
    if (!state || !zipCode) {
        return {
            isValid: false,
            error: 'Both state and ZIP code are required'
        };
    }
    
    // Validate state format (2 uppercase letters)
    if (!/^[A-Z]{2}$/.test(state)) {
        return {
            isValid: false,
            error: 'State must be 2 uppercase letters'
        };
    }
    
    // Validate ZIP code format (5 digits)
    if (!/^\d{5}$/.test(zipCode)) {
        return {
            isValid: false,
            error: 'ZIP code must be 5 digits'
        };
    }
    
    // Simple state/ZIP validation (basic ranges)
    const stateZipRanges = {
        'CA': { min: 90000, max: 96999 },
        'NY': { min: 10000, max: 14999 },
        'TX': { min: 75000, max: 79999 },
        'FL': { min: 32000, max: 34999 }
    };
    
    const zipNum = parseInt(zipCode);
    const range = stateZipRanges[state];
    
    if (range && (zipNum < range.min || zipNum > range.max)) {
        return {
            isValid: false,
            error: FIELD_ERROR_MESSAGES.STATE_ZIP_MISMATCH
        };
    }
    
    return {
        isValid: true,
        error: null
    };
}

/**
 * Creates Yup validation schema from BMS field definitions
 * Generates type-safe validation schemas for React Hook Form integration
 * 
 * @param {object} fieldDefinitions - BMS field definitions
 * @returns {ValidationSchema} Yup validation schema
 */
export function createYupSchema(fieldDefinitions) {
    const schema = {};
    
    for (const [fieldName, fieldConfig] of Object.entries(fieldDefinitions)) {
        let fieldSchema;
        
        // Determine base schema type
        if (fieldConfig.attributes?.includes('NUM')) {
            fieldSchema = number();
        } else {
            fieldSchema = string();
        }
        
        // Apply MUSTFILL validation
        if (fieldConfig.attributes?.includes('MUSTFILL') || fieldConfig.mustfill) {
            fieldSchema = fieldSchema.required(`${fieldName} is required`);
        }
        
        // Apply length constraints
        if (fieldConfig.length) {
            if (fieldSchema._type === 'string') {
                fieldSchema = fieldSchema.max(fieldConfig.length, 
                    `${fieldName} must be ${fieldConfig.length} characters or less`);
            }
        }
        
        // Apply PICIN pattern validation
        if (fieldConfig.picin) {
            const regex = convertPicinToRegex(fieldConfig.picin);
            fieldSchema = fieldSchema.matches(regex, 
                `${fieldName} must match pattern ${fieldConfig.picin}`);
        }
        
        // Apply custom validation
        if (fieldConfig.validationFn) {
            fieldSchema = fieldSchema.test('custom', 
                fieldConfig.errorMessage || 'Invalid value', 
                fieldConfig.validationFn);
        }
        
        schema[fieldName] = fieldSchema;
    }
    
    return object(schema);
}

/**
 * Formats validation error messages for consistent display
 * Ensures error messages match original BMS error message format
 * 
 * @param {string} fieldName - Field name for error context
 * @param {string} errorType - Type of validation error
 * @param {string} customMessage - Custom error message (optional)
 * @returns {string} Formatted error message
 */
export function formatValidationError(fieldName, errorType, customMessage) {
    const { VALIDATION_ERRORS, FIELD_ERROR_MESSAGES } = MessageConstants;
    
    // Use custom message if provided
    if (customMessage) {
        return customMessage;
    }
    
    // Use field-specific error message if available
    if (FIELD_ERROR_MESSAGES[fieldName]) {
        return FIELD_ERROR_MESSAGES[fieldName];
    }
    
    // Use generic error message based on error type
    switch (errorType) {
        case 'required':
            return VALIDATION_ERRORS.REQUIRED_FIELD;
        case 'format':
            return VALIDATION_ERRORS.INVALID_FORMAT;
        case 'range':
            return VALIDATION_ERRORS.OUT_OF_RANGE;
        case 'business':
            return VALIDATION_ERRORS.BUSINESS_RULE_VIOLATION;
        default:
            return VALIDATION_ERRORS.INVALID_FORMAT;
    }
}

/**
 * Field Validation Utils - Default export providing complete validation toolkit
 * Exposes all validation functions in a structured utility object
 */
export const FieldValidationUtils = {
    // BMS validation functions
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
    
    // Schema generation and formatting
    schemaGeneration: createYupSchema,
    errorFormatting: formatValidationError
};

export default FieldValidationUtils;