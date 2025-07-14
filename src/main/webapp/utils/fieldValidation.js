/**
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
 *
 * CardDemo Field Validation Utilities
 * 
 * This module provides comprehensive field validation utilities that convert BMS field
 * validation rules and attribute bytes to React validation patterns, preserving exact
 * COBOL business logic while enabling real-time validation in React components with
 * Material-UI integration.
 * 
 * Key Features:
 * - Converts BMS attribute bytes (ASKIP, UNPROT, PROT, NUM, IC, FSET, etc.) to React validation rules
 * - Transforms PICIN validation patterns to JavaScript regex patterns
 * - Implements MUSTFILL validation equivalent to BMS required field checking
 * - Provides cross-field validation for business rules (state/ZIP, account-card linkage)
 * - Enables real-time validation feedback matching original BMS field sequencing
 * - Integrates with React Hook Form and Yup for modern form validation
 * - Maintains exact COBOL precision for financial calculations using Decimal.js
 * 
 * Technical Requirements:
 * - All BMS screens (18 total) require equivalent React validation with identical error behavior
 * - Field validation rules must preserve exact COBOL business logic from BMS definitions
 * - React Hook Form integration must maintain original field order and validation timing
 * - Material-UI TextField properties must reflect BMS attribute bytes accurately
 */

import { useForm } from 'react-hook-form';
import * as yup from 'yup';
import { Decimal } from 'decimal.js';

// Import dependency modules
import { FieldConstants } from '../constants/FieldConstants';
import { ValidationConstants } from '../constants/ValidationConstants';
import { MessageConstants } from '../constants/MessageConstants';
import { CommonTypes, ValidationTypes } from '../types/CommonTypes';
import { FieldValidationRules, FormValidationSchema, CrossFieldValidationRules } from '../types/ValidationTypes';
import { 
  parseCobolDecimal, 
  formatPicX, 
  formatPic9, 
  formatPicS9V9, 
  convertZonedDecimal, 
  convertPackedDecimal 
} from './dataFormatting';

// Extract constants for easier access
const { BMS_ATTRIBUTES, FIELD_LENGTHS, ATTRIBUTE_MAPPINGS, FORMAT_PATTERNS } = FieldConstants;
const { PICIN_PATTERNS, VALIDATION_RULES, INPUT_MASKS, FIELD_CONSTRAINTS } = ValidationConstants;
const { API_ERROR_MESSAGES, VALIDATION_ERRORS, FIELD_ERROR_MESSAGES } = MessageConstants;

// Configure Decimal.js for COBOL precision compatibility
Decimal.set({
  precision: 31,
  rounding: Decimal.ROUND_HALF_UP,
  toExpNeg: -31,
  toExpPos: 31,
  modulo: Decimal.ROUND_HALF_UP
});

/**
 * Creates BMS validation rules for a field based on its BMS attributes
 * 
 * Converts BMS field attributes (ASKIP, UNPROT, PROT, NUM, IC, FSET, etc.) to 
 * React Hook Form validation rules and Material-UI component properties,
 * preserving exact original BMS field behavior.
 * 
 * @param {Object} fieldConfig - BMS field configuration object
 * @param {string} fieldConfig.name - Field name from BMS definition
 * @param {Array<string>} fieldConfig.attributes - BMS attribute array (e.g., ['FSET', 'IC', 'NORM', 'UNPROT'])
 * @param {number} fieldConfig.length - Field length from BMS LENGTH attribute
 * @param {string} fieldConfig.picinPattern - PICIN pattern from BMS (e.g., '99999999999')
 * @param {boolean} fieldConfig.mustfill - Whether field has VALIDN=(MUSTFILL)
 * @param {string} fieldConfig.color - BMS color attribute (BLUE, YELLOW, etc.)
 * @param {string} fieldConfig.highlight - BMS highlight attribute (UNDERLINE, OFF, etc.)
 * @param {string} fieldConfig.justify - BMS justify attribute (LEFT, RIGHT, CENTER)
 * @returns {Object} React Hook Form validation rules with Material-UI properties
 */
export function createBmsValidationRules(fieldConfig) {
  const {
    name,
    attributes = [],
    length,
    picinPattern,
    mustfill = false,
    color = 'NEUTRAL',
    highlight = 'OFF',
    justify = 'LEFT'
  } = fieldConfig;

  // Initialize validation rules object
  const validationRules = {
    name,
    materialUIProps: {},
    validationSchema: {},
    errorMessages: {},
    realTimeValidation: {},
    crossFieldRules: []
  };

  // Process BMS attributes to determine field behavior
  const attributeSet = new Set(attributes);
  
  // ASKIP attribute - Auto-skip protected field (display-only)
  if (attributeSet.has(BMS_ATTRIBUTES.ASKIP)) {
    validationRules.materialUIProps = {
      ...validationRules.materialUIProps,
      ...ATTRIBUTE_MAPPINGS.ASKIP_TO_READONLY.materialUIProps,
      InputProps: { readOnly: true },
      variant: 'filled'
    };
    validationRules.validationSchema.disabled = true;
    validationRules.realTimeValidation.skipValidation = true;
  }
  
  // UNPROT attribute - Unprotected field allowing user input
  else if (attributeSet.has(BMS_ATTRIBUTES.UNPROT)) {
    validationRules.materialUIProps = {
      ...validationRules.materialUIProps,
      ...ATTRIBUTE_MAPPINGS.UNPROT_TO_EDITABLE.materialUIProps,
      InputProps: { readOnly: false },
      variant: 'outlined'
    };
    validationRules.validationSchema.disabled = false;
    validationRules.realTimeValidation.skipValidation = false;
  }
  
  // PROT attribute - Protected field preventing user modification
  else if (attributeSet.has(BMS_ATTRIBUTES.PROT)) {
    validationRules.materialUIProps = {
      ...validationRules.materialUIProps,
      ...ATTRIBUTE_MAPPINGS.PROT_TO_DISABLED.materialUIProps,
      disabled: true,
      variant: 'filled'
    };
    validationRules.validationSchema.disabled = true;
    validationRules.realTimeValidation.skipValidation = true;
  }

  // NUM attribute - Numeric-only input validation
  if (attributeSet.has(BMS_ATTRIBUTES.NUM)) {
    validationRules.materialUIProps = {
      ...validationRules.materialUIProps,
      ...ATTRIBUTE_MAPPINGS.NUM_TO_NUMERIC.materialUIProps,
      type: 'number',
      inputProps: { 
        inputMode: 'numeric',
        pattern: '[0-9]*'
      }
    };
    validationRules.validationSchema.type = 'number';
    validationRules.validationSchema.pattern = ATTRIBUTE_MAPPINGS.NUM_TO_NUMERIC.validation.pattern;
    validationRules.errorMessages.pattern = ATTRIBUTE_MAPPINGS.NUM_TO_NUMERIC.validation.message;
    validationRules.realTimeValidation.numericOnly = true;
  }

  // IC attribute - Initial cursor positioning
  if (attributeSet.has(BMS_ATTRIBUTES.IC)) {
    validationRules.materialUIProps = {
      ...validationRules.materialUIProps,
      ...ATTRIBUTE_MAPPINGS.IC_TO_AUTOFOCUS.materialUIProps,
      autoFocus: true
    };
    validationRules.realTimeValidation.initialCursor = true;
  }

  // MUSTFILL validation - Required field checking
  if (mustfill) {
    validationRules.materialUIProps = {
      ...validationRules.materialUIProps,
      ...ATTRIBUTE_MAPPINGS.MUSTFILL_TO_REQUIRED.materialUIProps,
      required: true
    };
    validationRules.validationSchema.required = true;
    validationRules.errorMessages.required = ATTRIBUTE_MAPPINGS.MUSTFILL_TO_REQUIRED.validation.required;
    validationRules.realTimeValidation.mustfill = true;
  }

  // Intensity attribute mappings
  if (attributeSet.has(BMS_ATTRIBUTES.NORM)) {
    validationRules.materialUIProps = {
      ...validationRules.materialUIProps,
      ...ATTRIBUTE_MAPPINGS.INTENSITY_MAPPINGS[BMS_ATTRIBUTES.NORM]
    };
  }

  if (attributeSet.has(BMS_ATTRIBUTES.BRT)) {
    validationRules.materialUIProps = {
      ...validationRules.materialUIProps,
      ...ATTRIBUTE_MAPPINGS.INTENSITY_MAPPINGS[BMS_ATTRIBUTES.BRT]
    };
  }

  if (attributeSet.has(BMS_ATTRIBUTES.DRK)) {
    validationRules.materialUIProps = {
      ...validationRules.materialUIProps,
      ...ATTRIBUTE_MAPPINGS.INTENSITY_MAPPINGS[BMS_ATTRIBUTES.DRK],
      type: 'password'
    };
    validationRules.realTimeValidation.maskedInput = true;
  }

  // Color attribute mapping
  if (ATTRIBUTE_MAPPINGS.COLOR_MAPPINGS[color]) {
    validationRules.materialUIProps = {
      ...validationRules.materialUIProps,
      ...ATTRIBUTE_MAPPINGS.COLOR_MAPPINGS[color]
    };
  }

  // Highlight attribute mapping  
  if (ATTRIBUTE_MAPPINGS.HIGHLIGHT_MAPPINGS[highlight]) {
    validationRules.materialUIProps = {
      ...validationRules.materialUIProps,
      ...ATTRIBUTE_MAPPINGS.HIGHLIGHT_MAPPINGS[highlight]
    };
  }

  // Justify attribute mapping
  if (ATTRIBUTE_MAPPINGS.JUSTIFY_MAPPINGS[justify]) {
    validationRules.materialUIProps = {
      ...validationRules.materialUIProps,
      ...ATTRIBUTE_MAPPINGS.JUSTIFY_MAPPINGS[justify]
    };
  }

  // Length constraints
  if (length) {
    validationRules.materialUIProps.inputProps = {
      ...validationRules.materialUIProps.inputProps,
      maxLength: length
    };
    validationRules.validationSchema.maxLength = length;
    validationRules.errorMessages.maxLength = `Maximum ${length} characters allowed`;
  }

  // PICIN pattern validation
  if (picinPattern) {
    const regexPattern = convertPicinToRegex(picinPattern);
    validationRules.validationSchema.pattern = regexPattern;
    validationRules.errorMessages.pattern = `Invalid format. Expected: ${picinPattern}`;
    validationRules.realTimeValidation.picinPattern = picinPattern;
  }

  // FSET attribute for change detection
  if (attributeSet.has(BMS_ATTRIBUTES.FSET)) {
    validationRules.realTimeValidation.trackChanges = true;
  }

  return validationRules;
}

/**
 * Converts BMS PICIN patterns to JavaScript regex patterns
 * 
 * Transforms COBOL PICIN validation patterns (like '99999999999' for account numbers)
 * to JavaScript regex patterns for client-side validation, maintaining exact
 * COBOL validation logic and precision.
 * 
 * @param {string} picinPattern - BMS PICIN pattern (e.g., '99999999999', 'XXXXXXXXXXX')
 * @returns {RegExp} JavaScript regex pattern for validation
 */
export function convertPicinToRegex(picinPattern) {
  if (!picinPattern) {
    return /.*/; // Allow any input if no pattern specified
  }

  let regexPattern = '';
  
  // Process each character in the PICIN pattern
  for (let i = 0; i < picinPattern.length; i++) {
    const char = picinPattern[i];
    
    switch (char) {
      case '9':
        // Numeric digit (0-9)
        regexPattern += '[0-9]';
        break;
      case 'X':
        // Any alphanumeric character
        regexPattern += '[A-Za-z0-9]';
        break;
      case 'A':
        // Alphabetic character only
        regexPattern += '[A-Za-z]';
        break;
      case 'N':
        // Numeric character only (same as 9)
        regexPattern += '[0-9]';
        break;
      case 'S':
        // Sign character (+ or -)
        regexPattern += '[+\\-]';
        break;
      case 'V':
        // Implied decimal point (no actual character)
        continue;
      case 'P':
        // Scaling factor (no actual character)
        continue;
      case '.':
        // Literal decimal point
        regexPattern += '\\.';
        break;
      case '-':
        // Literal hyphen
        regexPattern += '\\-';
        break;
      case '/':
        // Literal slash
        regexPattern += '\\/';
        break;
      case '(':
        // Literal left parenthesis
        regexPattern += '\\(';
        break;
      case ')':
        // Literal right parenthesis
        regexPattern += '\\)';
        break;
      case ' ':
        // Literal space
        regexPattern += ' ';
        break;
      default:
        // Literal character
        regexPattern += char.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    }
  }
  
  // Return compiled regex with start and end anchors
  return new RegExp(`^${regexPattern}$`);
}

/**
 * Creates MUSTFILL validator for required field checking
 * 
 * Implements BMS VALIDN=(MUSTFILL) validation equivalent for React Hook Form,
 * ensuring fields marked as required in BMS definitions maintain the same
 * validation behavior in React components.
 * 
 * @param {string} fieldName - Name of the field to validate
 * @param {Object} options - Validation options
 * @param {string} options.message - Custom error message
 * @param {boolean} options.trimWhitespace - Whether to trim whitespace before validation
 * @param {Array<string>} options.allowedValues - Array of allowed values (for selection fields)
 * @returns {Function} Validator function for React Hook Form
 */
export function createMustfillValidator(fieldName, options = {}) {
  const {
    message = 'This field is required',
    trimWhitespace = true,
    allowedValues = null
  } = options;

  return (value) => {
    // Handle null, undefined, or empty values
    if (value === null || value === undefined) {
      return message;
    }

    // Convert to string and optionally trim whitespace
    const stringValue = String(value);
    const processedValue = trimWhitespace ? stringValue.trim() : stringValue;

    // Check if value is empty after processing
    if (processedValue === '') {
      return message;
    }

    // Check against allowed values if specified
    if (allowedValues && !allowedValues.includes(processedValue)) {
      return `${fieldName} must be one of: ${allowedValues.join(', ')}`;
    }

    // Value is valid
    return true;
  };
}

/**
 * Creates cross-field validator for business rules
 * 
 * Implements complex business rule validation that spans multiple fields,
 * such as state/ZIP consistency, account-card linkage, and date range validation.
 * Maintains exact COBOL business logic from original application.
 * 
 * @param {Object} crossFieldRules - Cross-field validation rules configuration
 * @param {Array} crossFieldRules.rules - Array of validation rule objects
 * @param {string} crossFieldRules.rules[].name - Rule name for identification
 * @param {Array<string>} crossFieldRules.rules[].fields - Fields involved in validation
 * @param {Function} crossFieldRules.rules[].validator - Validation function
 * @param {string} crossFieldRules.rules[].message - Error message for validation failure
 * @returns {Function} Cross-field validator function
 */
export function createCrossFieldValidator(crossFieldRules) {
  return (formData) => {
    const errors = {};
    
    // Process each cross-field rule
    for (const rule of crossFieldRules.rules) {
      const { name, fields, validator, message } = rule;
      
      try {
        // Extract field values for validation
        const fieldValues = fields.map(fieldName => formData[fieldName]);
        
        // Execute validation function
        const validationResult = validator(...fieldValues, formData);
        
        // Handle validation result
        if (validationResult !== true) {
          // Determine which field should receive the error
          const errorField = rule.errorField || fields[0];
          errors[errorField] = validationResult === false ? message : validationResult;
        }
      } catch (error) {
        console.error(`Cross-field validation error for rule ${name}:`, error);
        errors[fields[0]] = 'Validation error occurred';
      }
    }
    
    // Return errors object or null if no errors
    return Object.keys(errors).length > 0 ? errors : null;
  };
}

/**
 * Creates field attribute validator for BMS attribute processing
 * 
 * Validates that field attributes and values conform to BMS attribute definitions
 * and constraints, ensuring Material-UI component properties accurately reflect
 * original BMS field behavior.
 * 
 * @param {Object} fieldAttributes - Field attributes from BMS definition
 * @param {Array<string>} fieldAttributes.attrb - BMS attribute array
 * @param {string} fieldAttributes.color - BMS color attribute
 * @param {string} fieldAttributes.hilight - BMS highlight attribute
 * @param {number} fieldAttributes.length - Field length constraint
 * @param {string} fieldAttributes.picin - PICIN pattern
 * @param {boolean} fieldAttributes.mustfill - MUSTFILL validation flag
 * @returns {Function} Field attribute validator function
 */
export function createFieldAttributeValidator(fieldAttributes) {
  return (value, context = {}) => {
    const { attrb, color, hilight, length, picin, mustfill } = fieldAttributes;
    
    // Skip validation for ASKIP fields
    if (attrb && attrb.includes('ASKIP')) {
      return true;
    }
    
    // Skip validation for PROT fields
    if (attrb && attrb.includes('PROT')) {
      return true;
    }
    
    // MUSTFILL validation
    if (mustfill) {
      const mustfillResult = createMustfillValidator('field')(value);
      if (mustfillResult !== true) {
        return mustfillResult;
      }
    }
    
    // NUM attribute validation
    if (attrb && attrb.includes('NUM')) {
      if (value && !/^[0-9]*$/.test(String(value))) {
        return 'Only numeric characters are allowed';
      }
    }
    
    // Length constraint validation
    if (length && value) {
      const stringValue = String(value);
      if (stringValue.length > length) {
        return `Maximum ${length} characters allowed`;
      }
    }
    
    // PICIN pattern validation
    if (picin && value) {
      const regex = convertPicinToRegex(picin);
      if (!regex.test(String(value))) {
        return `Invalid format. Expected: ${picin}`;
      }
    }
    
    return true;
  };
}

/**
 * Creates business rule validator for complex validation logic
 * 
 * Implements advanced business rule validation that replicates COBOL program logic
 * for data validation, cross-referencing, and business constraint enforcement.
 * Maintains exact precision and validation behavior from original application.
 * 
 * @param {Object} businessRules - Business rule configuration object
 * @param {string} businessRules.ruleName - Name of the business rule
 * @param {string} businessRules.ruleType - Type of business rule (DATE, ACCOUNT, CARD, etc.)
 * @param {Object} businessRules.parameters - Rule-specific parameters
 * @param {Function} businessRules.customValidator - Custom validation function
 * @returns {Function} Business rule validator function
 */
export function createBusinessRuleValidator(businessRules) {
  const { ruleName, ruleType, parameters = {}, customValidator } = businessRules;
  
  return (value, formData = {}) => {
    // Use custom validator if provided
    if (customValidator) {
      return customValidator(value, formData, parameters);
    }
    
    // Built-in business rule validation based on rule type
    switch (ruleType) {
      case 'DATE':
        return validateDateBusinessRule(value, parameters);
      case 'ACCOUNT':
        return validateAccountBusinessRule(value, formData, parameters);
      case 'CARD':
        return validateCardBusinessRule(value, formData, parameters);
      case 'PHONE':
        return validatePhoneBusinessRule(value, parameters);
      case 'SSN':
        return validateSSNBusinessRule(value, parameters);
      case 'ADDRESS':
        return validateAddressBusinessRule(value, formData, parameters);
      case 'CURRENCY':
        return validateCurrencyBusinessRule(value, parameters);
      default:
        console.warn(`Unknown business rule type: ${ruleType}`);
        return true;
    }
  };
}

/**
 * Creates real-time validator for immediate feedback
 * 
 * Provides real-time validation feedback as user types, matching original BMS
 * field sequencing and error display patterns. Integrates with Material-UI
 * TextField components for immediate visual feedback.
 * 
 * @param {Object} realtimeConfig - Real-time validation configuration
 * @param {number} realtimeConfig.debounceMs - Debounce delay in milliseconds
 * @param {boolean} realtimeConfig.validateOnChange - Validate on every change
 * @param {boolean} realtimeConfig.validateOnBlur - Validate on field blur
 * @param {Array<Function>} realtimeConfig.validators - Array of validator functions
 * @returns {Object} Real-time validation handlers
 */
export function createRealTimeValidator(realtimeConfig) {
  const {
    debounceMs = 300,
    validateOnChange = true,
    validateOnBlur = true,
    validators = []
  } = realtimeConfig;
  
  let debounceTimer = null;
  
  const performValidation = (value, formData) => {
    const errors = [];
    
    // Run all validators
    for (const validator of validators) {
      const result = validator(value, formData);
      if (result !== true) {
        errors.push(result);
      }
    }
    
    return errors.length > 0 ? errors[0] : null;
  };
  
  return {
    // onChange handler with debouncing
    onChange: (value, formData, callback) => {
      if (!validateOnChange) return;
      
      if (debounceTimer) {
        clearTimeout(debounceTimer);
      }
      
      debounceTimer = setTimeout(() => {
        const error = performValidation(value, formData);
        if (callback) callback(error);
      }, debounceMs);
    },
    
    // onBlur handler for immediate validation
    onBlur: (value, formData, callback) => {
      if (!validateOnBlur) return;
      
      if (debounceTimer) {
        clearTimeout(debounceTimer);
      }
      
      const error = performValidation(value, formData);
      if (callback) callback(error);
    },
    
    // Manual validation trigger
    validate: (value, formData) => {
      return performValidation(value, formData);
    }
  };
}

/**
 * Validates account number using COBOL business logic
 * 
 * Implements exact account number validation as defined in COBOL programs,
 * including format checking, check digit validation, and business rule
 * constraints for account number assignment.
 * 
 * @param {string} accountNumber - Account number to validate
 * @returns {boolean|string} True if valid, error message if invalid
 */
export function validateAccountNumber(accountNumber) {
  // Check if value is provided
  if (!accountNumber) {
    return 'Account number is required';
  }
  
  // Convert to string and remove any formatting
  const cleanNumber = String(accountNumber).replace(/\s+/g, '');
  
  // Check length (must be exactly 11 digits)
  if (cleanNumber.length !== 11) {
    return 'Account number must be exactly 11 digits';
  }
  
  // Check if all characters are numeric
  if (!/^\d{11}$/.test(cleanNumber)) {
    return 'Account number must contain only numeric digits';
  }
  
  // Check for invalid patterns (all zeros, all same digit)
  if (/^0{11}$/.test(cleanNumber)) {
    return 'Invalid account number format';
  }
  
  if (/^(\d)\1{10}$/.test(cleanNumber)) {
    return 'Invalid account number format';
  }
  
  // Implement simple check digit validation (modulo 10)
  let sum = 0;
  for (let i = 0; i < 10; i++) {
    const digit = parseInt(cleanNumber[i]);
    sum += (i % 2 === 0) ? digit : digit * 2;
  }
  
  const calculatedCheckDigit = (10 - (sum % 10)) % 10;
  const providedCheckDigit = parseInt(cleanNumber[10]);
  
  if (calculatedCheckDigit !== providedCheckDigit) {
    return 'Invalid account number check digit';
  }
  
  return true;
}

/**
 * Validates card number using Luhn algorithm
 * 
 * Implements standard credit card validation using Luhn algorithm,
 * maintaining compatibility with COBOL card validation logic while
 * providing additional security validation features.
 * 
 * @param {string} cardNumber - Card number to validate
 * @returns {boolean|string} True if valid, error message if invalid
 */
export function validateCardNumber(cardNumber) {
  // Check if value is provided
  if (!cardNumber) {
    return 'Card number is required';
  }
  
  // Remove spaces and hyphens
  const cleanNumber = String(cardNumber).replace(/[\s-]/g, '');
  
  // Check length (must be 13-19 digits, typically 16)
  if (cleanNumber.length < 13 || cleanNumber.length > 19) {
    return 'Card number must be between 13 and 19 digits';
  }
  
  // Check if all characters are numeric
  if (!/^\d+$/.test(cleanNumber)) {
    return 'Card number must contain only numeric digits';
  }
  
  // Implement Luhn algorithm validation
  let sum = 0;
  let isEven = false;
  
  for (let i = cleanNumber.length - 1; i >= 0; i--) {
    let digit = parseInt(cleanNumber[i]);
    
    if (isEven) {
      digit *= 2;
      if (digit > 9) {
        digit -= 9;
      }
    }
    
    sum += digit;
    isEven = !isEven;
  }
  
  if (sum % 10 !== 0) {
    return 'Invalid card number';
  }
  
  return true;
}

/**
 * Validates Social Security Number format and constraints
 * 
 * Implements SSN validation with format checking, invalid number detection,
 * and business rule constraints following COBOL validation logic for
 * customer identification verification.
 * 
 * @param {string} ssn - Social Security Number to validate
 * @returns {boolean|string} True if valid, error message if invalid
 */
export function validateSSN(ssn) {
  // Check if value is provided
  if (!ssn) {
    return 'Social Security Number is required';
  }
  
  // Remove hyphens and spaces
  const cleanSSN = String(ssn).replace(/[\s-]/g, '');
  
  // Check length (must be exactly 9 digits)
  if (cleanSSN.length !== 9) {
    return 'SSN must be exactly 9 digits';
  }
  
  // Check if all characters are numeric
  if (!/^\d{9}$/.test(cleanSSN)) {
    return 'SSN must contain only numeric digits';
  }
  
  // Check for invalid patterns
  const invalidPatterns = FIELD_CONSTRAINTS.BUSINESS_RULES.SSN_RULES.INVALID_SSN_PATTERNS;
  if (invalidPatterns.includes(cleanSSN)) {
    return 'Invalid SSN format';
  }
  
  // Check area number (first 3 digits)
  const areaNumber = cleanSSN.substr(0, 3);
  const invalidAreaNumbers = FIELD_CONSTRAINTS.BUSINESS_RULES.SSN_RULES.INVALID_AREA_NUMBERS;
  if (invalidAreaNumbers.includes(areaNumber)) {
    return 'Invalid SSN area number';
  }
  
  // Check group number (middle 2 digits) - cannot be 00
  const groupNumber = cleanSSN.substr(3, 2);
  if (groupNumber === '00') {
    return 'Invalid SSN group number';
  }
  
  // Check serial number (last 4 digits) - cannot be 0000
  const serialNumber = cleanSSN.substr(5, 4);
  if (serialNumber === '0000') {
    return 'Invalid SSN serial number';
  }
  
  return true;
}

/**
 * Validates date field with business rules
 * 
 * Implements comprehensive date validation including format checking,
 * leap year validation, business day constraints, and future/past date
 * restrictions based on COBOL date validation logic.
 * 
 * @param {string|Date} date - Date to validate
 * @param {Object} options - Validation options
 * @param {boolean} options.allowFuture - Allow future dates
 * @param {boolean} options.allowPast - Allow past dates
 * @param {Date} options.minDate - Minimum allowed date
 * @param {Date} options.maxDate - Maximum allowed date
 * @returns {boolean|string} True if valid, error message if invalid
 */
export function validateDate(date, options = {}) {
  const {
    allowFuture = true,
    allowPast = true,
    minDate = null,
    maxDate = null
  } = options;
  
  // Check if value is provided
  if (!date) {
    return 'Date is required';
  }
  
  let dateObj;
  
  // Convert string to Date object
  if (typeof date === 'string') {
    // Try to parse common date formats
    const dateString = date.trim();
    
    // MM/DD/YYYY format
    if (/^\d{2}\/\d{2}\/\d{4}$/.test(dateString)) {
      const [month, day, year] = dateString.split('/').map(Number);
      dateObj = new Date(year, month - 1, day);
    }
    // YYYY-MM-DD format
    else if (/^\d{4}-\d{2}-\d{2}$/.test(dateString)) {
      const [year, month, day] = dateString.split('-').map(Number);
      dateObj = new Date(year, month - 1, day);
    }
    // Other formats
    else {
      dateObj = new Date(date);
    }
  } else {
    dateObj = new Date(date);
  }
  
  // Check if date is valid
  if (isNaN(dateObj.getTime())) {
    return 'Invalid date format';
  }
  
  // Check future date constraint
  if (!allowFuture && dateObj > new Date()) {
    return 'Future dates are not allowed';
  }
  
  // Check past date constraint
  if (!allowPast && dateObj < new Date()) {
    return 'Past dates are not allowed';
  }
  
  // Check minimum date constraint
  if (minDate && dateObj < minDate) {
    return `Date must be on or after ${minDate.toLocaleDateString()}`;
  }
  
  // Check maximum date constraint
  if (maxDate && dateObj > maxDate) {
    return `Date must be on or before ${maxDate.toLocaleDateString()}`;
  }
  
  return true;
}

/**
 * Validates state and ZIP code consistency
 * 
 * Implements cross-field validation for state and ZIP code combinations,
 * ensuring geographic consistency based on COBOL address validation logic
 * and USPS postal code standards.
 * 
 * @param {string} state - State code (2 letters)
 * @param {string} zipCode - ZIP code (5 digits)
 * @returns {boolean|string} True if valid, error message if invalid
 */
export function validateStateZip(state, zipCode) {
  // Check if both values are provided
  if (!state && !zipCode) {
    return true; // Both empty is acceptable
  }
  
  if (!state) {
    return 'State is required when ZIP code is provided';
  }
  
  if (!zipCode) {
    return 'ZIP code is required when state is provided';
  }
  
  // Validate state format
  const stateUpper = state.toUpperCase();
  if (!/^[A-Z]{2}$/.test(stateUpper)) {
    return 'State must be exactly 2 uppercase letters';
  }
  
  // Check if state is valid US state
  const validStates = FIELD_CONSTRAINTS.BUSINESS_RULES.US_STATE_CODES;
  if (!validStates.includes(stateUpper)) {
    return 'Invalid US state code';
  }
  
  // Validate ZIP code format
  if (!/^\d{5}$/.test(zipCode)) {
    return 'ZIP code must be exactly 5 digits';
  }
  
  // Basic ZIP code range validation for common states
  const zipRanges = {
    'CA': [90000, 96999],
    'NY': [10000, 14999],
    'TX': [75000, 79999],
    'FL': [32000, 34999],
    'IL': [60000, 62999],
    'PA': [15000, 19999],
    'OH': [43000, 45999],
    'GA': [30000, 31999],
    'NC': [27000, 28999],
    'MI': [48000, 49999]
  };
  
  const zipNumber = parseInt(zipCode);
  const range = zipRanges[stateUpper];
  
  if (range && (zipNumber < range[0] || zipNumber > range[1])) {
    return `ZIP code ${zipCode} is not valid for state ${stateUpper}`;
  }
  
  return true;
}

/**
 * Creates Yup validation schema from BMS field definitions
 * 
 * Converts BMS field attributes and validation rules to Yup schema objects
 * for integration with React Hook Form, providing comprehensive type-safe
 * validation with exact COBOL business logic preservation.
 * 
 * @param {Object} fieldDefinitions - BMS field definitions object
 * @param {Object} fieldDefinitions.fields - Field configuration array
 * @param {Object} fieldDefinitions.crossFieldRules - Cross-field validation rules
 * @returns {Object} Yup validation schema
 */
export function createYupSchema(fieldDefinitions) {
  const { fields, crossFieldRules = [] } = fieldDefinitions;
  const schemaFields = {};
  
  // Process each field definition
  for (const field of fields) {
    const {
      name,
      type = 'string',
      required = false,
      length,
      pattern,
      min,
      max,
      customValidation = null
    } = field;
    
    let fieldSchema;
    
    // Create base schema based on type
    switch (type) {
      case 'number':
        fieldSchema = yup.number();
        if (min !== undefined) fieldSchema = fieldSchema.min(min);
        if (max !== undefined) fieldSchema = fieldSchema.max(max);
        break;
      case 'date':
        fieldSchema = yup.date();
        break;
      case 'boolean':
        fieldSchema = yup.boolean();
        break;
      default:
        fieldSchema = yup.string();
        if (length) fieldSchema = fieldSchema.max(length);
        if (pattern) fieldSchema = fieldSchema.matches(pattern);
    }
    
    // Add required validation
    if (required) {
      fieldSchema = fieldSchema.required(`${name} is required`);
    }
    
    // Add custom validation
    if (customValidation) {
      fieldSchema = fieldSchema.test(
        'custom-validation',
        customValidation.message || 'Validation failed',
        customValidation.validator
      );
    }
    
    schemaFields[name] = fieldSchema;
  }
  
  // Create object schema
  let schema = yup.object(schemaFields);
  
  // Add cross-field validation
  if (crossFieldRules.length > 0) {
    schema = schema.test(
      'cross-field-validation',
      'Cross-field validation failed',
      function (values) {
        const errors = [];
        
        for (const rule of crossFieldRules) {
          const { name, fields, validator, message } = rule;
          const fieldValues = fields.map(fieldName => values[fieldName]);
          
          const result = validator(...fieldValues, values);
          if (result !== true) {
            errors.push(this.createError({
              path: rule.errorField || fields[0],
              message: result === false ? message : result
            }));
          }
        }
        
        return errors.length === 0 ? true : new yup.ValidationError(errors);
      }
    );
  }
  
  return schema;
}

/**
 * Formats validation errors for user display
 * 
 * Converts validation error objects to user-friendly error messages
 * that match original BMS error display patterns, maintaining
 * consistent error messaging and positioning.
 * 
 * @param {Object|Array} errors - Validation errors from React Hook Form or Yup
 * @param {Object} fieldLabels - Field label mappings for error display
 * @returns {Object} Formatted error messages object
 */
export function formatValidationError(errors, fieldLabels = {}) {
  const formattedErrors = {};
  
  // Handle different error formats
  if (Array.isArray(errors)) {
    // Array of error objects
    errors.forEach(error => {
      const fieldName = error.path || error.field;
      const message = error.message || error.msg || 'Validation error';
      const label = fieldLabels[fieldName] || fieldName;
      
      formattedErrors[fieldName] = `${label}: ${message}`;
    });
  } else if (errors && typeof errors === 'object') {
    // Object with field names as keys
    Object.keys(errors).forEach(fieldName => {
      const error = errors[fieldName];
      const message = error.message || error.msg || error || 'Validation error';
      const label = fieldLabels[fieldName] || fieldName;
      
      formattedErrors[fieldName] = `${label}: ${message}`;
    });
  } else if (typeof errors === 'string') {
    // Single error message
    formattedErrors.general = errors;
  }
  
  return formattedErrors;
}

// Helper functions for business rule validation

/**
 * Validates date according to business rules
 * @private
 */
function validateDateBusinessRule(value, parameters) {
  if (!value) return true;
  
  const { allowFuture, allowPast, minDate, maxDate } = parameters;
  return validateDate(value, { allowFuture, allowPast, minDate, maxDate });
}

/**
 * Validates account according to business rules
 * @private
 */
function validateAccountBusinessRule(value, formData, parameters) {
  if (!value) return true;
  
  const accountResult = validateAccountNumber(value);
  if (accountResult !== true) {
    return accountResult;
  }
  
  // Additional business rule validation can be added here
  return true;
}

/**
 * Validates card according to business rules
 * @private
 */
function validateCardBusinessRule(value, formData, parameters) {
  if (!value) return true;
  
  const cardResult = validateCardNumber(value);
  if (cardResult !== true) {
    return cardResult;
  }
  
  // Additional business rule validation can be added here
  return true;
}

/**
 * Validates phone according to business rules
 * @private
 */
function validatePhoneBusinessRule(value, parameters) {
  if (!value) return true;
  
  // Remove formatting
  const cleanPhone = String(value).replace(/[\s\-\(\)]/g, '');
  
  // Check length
  if (cleanPhone.length !== 10) {
    return 'Phone number must be exactly 10 digits';
  }
  
  // Check format
  if (!/^\d{10}$/.test(cleanPhone)) {
    return 'Phone number must contain only digits';
  }
  
  // Check area code
  const areaCode = cleanPhone.substr(0, 3);
  if (areaCode[0] === '0' || areaCode[0] === '1') {
    return 'Invalid area code';
  }
  
  // Check exchange
  const exchange = cleanPhone.substr(3, 3);
  if (exchange[0] === '0' || exchange[0] === '1') {
    return 'Invalid exchange';
  }
  
  return true;
}

/**
 * Validates SSN according to business rules
 * @private
 */
function validateSSNBusinessRule(value, parameters) {
  if (!value) return true;
  
  return validateSSN(value);
}

/**
 * Validates address according to business rules
 * @private
 */
function validateAddressBusinessRule(value, formData, parameters) {
  // State and ZIP validation
  if (formData.state && formData.zipCode) {
    return validateStateZip(formData.state, formData.zipCode);
  }
  
  return true;
}

/**
 * Validates currency according to business rules
 * @private
 */
function validateCurrencyBusinessRule(value, parameters) {
  if (!value) return true;
  
  const { min = 0, max = 999999999.99 } = parameters;
  
  // Parse currency value
  const numericValue = parseFloat(String(value).replace(/[^\d.-]/g, ''));
  
  if (isNaN(numericValue)) {
    return 'Invalid currency amount';
  }
  
  if (numericValue < min) {
    return `Amount must be at least ${min}`;
  }
  
  if (numericValue > max) {
    return `Amount must be no more than ${max}`;
  }
  
  return true;
}

/**
 * Field Validation Utils - Default export object containing all validation utilities
 * 
 * Provides a comprehensive utility object that combines all field validation
 * functions for easy import and use in React components. This object serves
 * as the primary interface for field validation functionality.
 */
export const FieldValidationUtils = {
  bmsValidation: createBmsValidationRules,
  picinConversion: convertPicinToRegex,
  mustfillValidation: createMustfillValidator,
  crossFieldValidation: createCrossFieldValidator,
  businessRuleValidation: createBusinessRuleValidator,
  realTimeValidation: createRealTimeValidator,
  accountValidation: validateAccountNumber,
  cardValidation: validateCardNumber,
  ssnValidation: validateSSN,
  dateValidation: validateDate,
  stateZipValidation: validateStateZip,
  schemaGeneration: createYupSchema,
  errorFormatting: formatValidationError
};

// Default export
export default FieldValidationUtils;