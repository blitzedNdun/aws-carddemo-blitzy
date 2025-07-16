/**
 * CardDemo Data Formatting Utilities
 * 
 * This module provides exact COBOL data type conversion and formatting utilities
 * for the CardDemo application, ensuring precise preservation of mainframe data
 * types and arithmetic precision during the React frontend transformation.
 * 
 * Critical Requirements:
 * - Maintains exact COBOL COMP-3 decimal precision using JavaScript BigDecimal
 * - Preserves financial calculation accuracy with no rounding errors
 * - Handles COBOL calendar logic including leap year calculations
 * - Converts between BMS copybook structures and React component data formats
 * 
 * @author Blitzy Platform - CardDemo Migration Team
 * @version 1.0.0
 * @since 2024-01-01
 */

import { Decimal } from 'decimal.js';

// Configure Decimal.js to match COBOL COMP-3 precision
// Using 31 significant digits to match COBOL's maximum precision
// Half-up rounding mode to match COBOL arithmetic behavior
Decimal.set({
    precision: 31,
    rounding: Decimal.ROUND_HALF_UP,
    toExpNeg: -7,
    toExpPos: 21,
    minE: -324,
    maxE: 308
});

// COBOL date format constants
const COBOL_DATE_FORMAT = 'CCYYMMDD';
const COBOL_CENTURY_PIVOT = 50; // Years 00-49 = 20xx, 50-99 = 19xx

/**
 * Formats a numeric value as currency with exact COBOL precision
 * Handles COBOL PIC S9(n)V9(2) COMP-3 financial amounts
 * 
 * @param {number|string|Decimal} amount - The amount to format
 * @param {number} decimalPlaces - Number of decimal places (default: 2)
 * @param {boolean} showSign - Whether to show positive sign (default: false)
 * @returns {string} Formatted currency string with exact precision
 */
export function formatCurrency(amount, decimalPlaces = 2, showSign = false) {
    if (amount === null || amount === undefined || amount === '') {
        return '0.00';
    }

    try {
        // Convert to Decimal to preserve exact precision
        const decimalAmount = new Decimal(amount);
        
        // Format with exact decimal places
        const formatted = decimalAmount.toFixed(decimalPlaces);
        
        // Add thousands separators
        const parts = formatted.split('.');
        parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
        
        let result = parts.join('.');
        
        // Add sign if required or if negative
        if (showSign && decimalAmount.isPositive()) {
            result = '+' + result;
        }
        
        return result;
    } catch (error) {
        console.error('Error formatting currency:', error);
        return '0.00';
    }
}

/**
 * Formats a JavaScript Date object to COBOL CCYYMMDD format
 * Preserves exact date representation without timezone conversion
 * 
 * @param {Date} date - JavaScript Date object
 * @returns {string} Date in CCYYMMDD format
 */
export function formatDate(date) {
    if (!date || !(date instanceof Date) || isNaN(date.getTime())) {
        return '00000000';
    }

    const year = date.getFullYear().toString().padStart(4, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    
    return year + month + day;
}

/**
 * Converts COBOL COMMAREA structure to JSON format
 * Handles BMS copybook field mapping with exact data preservation
 * 
 * @param {Object} commarea - COBOL COMMAREA structure
 * @param {Object} fieldMap - Mapping of COBOL fields to JSON properties
 * @returns {Object} JSON representation of COMMAREA
 */
export function convertCommareaToJson(commarea, fieldMap) {
    if (!commarea || !fieldMap) {
        return {};
    }

    const result = {};
    
    for (const [cobolField, jsonField] of Object.entries(fieldMap)) {
        if (commarea.hasOwnProperty(cobolField)) {
            const value = commarea[cobolField];
            
            // Handle different data types based on field characteristics
            if (typeof value === 'string') {
                // Trim trailing spaces from COBOL character fields
                result[jsonField] = value.trim();
            } else if (typeof value === 'number') {
                // Preserve numeric precision
                result[jsonField] = value;
            } else {
                result[jsonField] = value;
            }
        }
    }
    
    return result;
}

/**
 * Formats account number with COBOL PIC 99999999999 pattern
 * Ensures 11-digit account number with leading zeros
 * 
 * @param {string|number} accountNumber - Account number to format
 * @returns {string} Formatted 11-digit account number
 */
export function formatAccountNumber(accountNumber) {
    if (!accountNumber) {
        return '00000000000';
    }

    // Remove any non-digit characters
    const digits = accountNumber.toString().replace(/\D/g, '');
    
    // Pad with leading zeros to 11 digits
    return digits.padStart(11, '0').substring(0, 11);
}

/**
 * Formats card number with COBOL PIC X(16) pattern
 * Handles credit card number formatting with exact length
 * 
 * @param {string} cardNumber - Card number to format
 * @param {boolean} masked - Whether to mask middle digits (default: false)
 * @returns {string} Formatted 16-character card number
 */
export function formatCardNumber(cardNumber, masked = false) {
    if (!cardNumber) {
        return '0000000000000000';
    }

    // Remove any non-digit characters
    const digits = cardNumber.toString().replace(/\D/g, '');
    
    // Pad or truncate to 16 digits
    const paddedNumber = digits.padStart(16, '0').substring(0, 16);
    
    if (masked) {
        // Show first 4 and last 4 digits, mask middle 8
        return paddedNumber.substring(0, 4) + '********' + paddedNumber.substring(12);
    }
    
    return paddedNumber;
}

/**
 * Parses COBOL decimal string to JavaScript Decimal
 * Handles COBOL COMP-3 and display numeric formats
 * 
 * @param {string} cobolDecimal - COBOL decimal string
 * @param {number} scale - Number of decimal places
 * @returns {Decimal} Parsed decimal value
 */
export function parseCobolDecimal(cobolDecimal, scale = 2) {
    if (!cobolDecimal) {
        return new Decimal(0);
    }

    try {
        // Remove any formatting characters
        const cleanDecimal = cobolDecimal.toString().replace(/[^0-9.-]/g, '');
        
        // Handle implicit decimal point for COMP-3 format
        if (scale > 0 && !cleanDecimal.includes('.')) {
            const integerPart = cleanDecimal.substring(0, cleanDecimal.length - scale);
            const decimalPart = cleanDecimal.substring(cleanDecimal.length - scale);
            const formatted = (integerPart || '0') + '.' + decimalPart;
            return new Decimal(formatted);
        }
        
        return new Decimal(cleanDecimal);
    } catch (error) {
        console.error('Error parsing COBOL decimal:', error);
        return new Decimal(0);
    }
}

/**
 * Formats SSN with COBOL PIC X(12) pattern
 * Handles Social Security Number formatting with exact spacing
 * 
 * @param {string} ssn - SSN to format
 * @param {boolean} masked - Whether to mask digits (default: false)
 * @returns {string} Formatted SSN (XXX-XX-XXXX)
 */
export function formatSSN(ssn, masked = false) {
    if (!ssn) {
        return '000-00-0000';
    }

    // Remove any non-digit characters
    const digits = ssn.toString().replace(/\D/g, '');
    
    // Pad to 9 digits
    const paddedSSN = digits.padStart(9, '0').substring(0, 9);
    
    if (masked) {
        return 'XXX-XX-' + paddedSSN.substring(5);
    }
    
    return paddedSSN.substring(0, 3) + '-' + 
           paddedSSN.substring(3, 5) + '-' + 
           paddedSSN.substring(5, 9);
}

/**
 * Formats string field with COBOL PIC X(n) pattern
 * Handles character field padding and truncation
 * 
 * @param {string} value - Value to format
 * @param {number} length - Target length
 * @param {boolean} rightPadded - Whether to right-pad (default: true)
 * @returns {string} Formatted string with exact length
 */
export function formatPicX(value, length, rightPadded = true) {
    if (!value) {
        return ' '.repeat(length);
    }

    const stringValue = value.toString();
    
    if (stringValue.length >= length) {
        return stringValue.substring(0, length);
    }
    
    if (rightPadded) {
        return stringValue.padEnd(length, ' ');
    } else {
        return stringValue.padStart(length, ' ');
    }
}

/**
 * Formats numeric field with COBOL PIC 9(n) pattern
 * Handles numeric display fields with leading zeros
 * 
 * @param {number|string} value - Value to format
 * @param {number} length - Target length
 * @returns {string} Formatted numeric string with leading zeros
 */
export function formatPic9(value, length) {
    if (!value && value !== 0) {
        return '0'.repeat(length);
    }

    // Remove any non-digit characters
    const digits = value.toString().replace(/\D/g, '');
    
    // Pad with leading zeros and truncate to length
    return digits.padStart(length, '0').substring(0, length);
}

/**
 * Formats signed decimal with COBOL PIC S9(n)V9(m) pattern
 * Handles signed numeric fields with decimal places
 * 
 * @param {number|string|Decimal} value - Value to format
 * @param {number} integerDigits - Number of integer digits
 * @param {number} decimalDigits - Number of decimal digits
 * @returns {string} Formatted signed decimal string
 */
export function formatPicS9V9(value, integerDigits, decimalDigits) {
    if (!value && value !== 0) {
        return '0'.repeat(integerDigits + decimalDigits + 1); // +1 for decimal point
    }

    try {
        const decimal = new Decimal(value);
        const formatted = decimal.toFixed(decimalDigits);
        const parts = formatted.split('.');
        
        // Format integer part with leading zeros
        let integerPart = parts[0];
        if (integerPart.startsWith('-')) {
            integerPart = '-' + integerPart.substring(1).padStart(integerDigits, '0');
        } else {
            integerPart = integerPart.padStart(integerDigits, '0');
        }
        
        return integerPart + '.' + parts[1];
    } catch (error) {
        console.error('Error formatting PIC S9V9:', error);
        return '0'.repeat(integerDigits) + '.' + '0'.repeat(decimalDigits);
    }
}

/**
 * Converts COBOL zoned decimal to JavaScript number
 * Handles COBOL zoned decimal format with sign in last nibble
 * 
 * @param {string} zonedDecimal - COBOL zoned decimal string
 * @param {number} scale - Number of decimal places
 * @returns {Decimal} Converted decimal value
 */
export function convertZonedDecimal(zonedDecimal, scale = 0) {
    if (!zonedDecimal) {
        return new Decimal(0);
    }

    try {
        // COBOL zoned decimal format: sign is in the last nibble
        const digits = zonedDecimal.slice(0, -1);
        const lastChar = zonedDecimal.slice(-1);
        
        // Determine sign from last character (A-I = positive, J-R = negative)
        const isNegative = lastChar >= 'J' && lastChar <= 'R';
        const lastDigit = lastChar.charCodeAt(0) - (isNegative ? 'J'.charCodeAt(0) : 'A'.charCodeAt(0));
        
        const allDigits = digits + lastDigit.toString();
        
        // Apply scale if specified
        let result = allDigits;
        if (scale > 0) {
            const integerPart = allDigits.substring(0, allDigits.length - scale);
            const decimalPart = allDigits.substring(allDigits.length - scale);
            result = (integerPart || '0') + '.' + decimalPart;
        }
        
        return new Decimal(isNegative ? '-' + result : result);
    } catch (error) {
        console.error('Error converting zoned decimal:', error);
        return new Decimal(0);
    }
}

/**
 * Converts COBOL packed decimal to JavaScript number
 * Handles COBOL COMP-3 packed decimal format
 * 
 * @param {string} packedDecimal - COBOL packed decimal string (hex representation)
 * @param {number} scale - Number of decimal places
 * @returns {Decimal} Converted decimal value
 */
export function convertPackedDecimal(packedDecimal, scale = 0) {
    if (!packedDecimal) {
        return new Decimal(0);
    }

    try {
        // Remove any spaces or non-hex characters
        const cleanHex = packedDecimal.replace(/[^0-9A-Fa-f]/g, '');
        
        // Last nibble contains sign (C = positive, D = negative)
        const lastNibble = cleanHex.slice(-1).toUpperCase();
        const isNegative = lastNibble === 'D';
        
        // Extract digits from all nibbles except sign
        let digits = '';
        for (let i = 0; i < cleanHex.length - 1; i += 2) {
            if (i + 1 < cleanHex.length - 1) {
                digits += cleanHex[i] + cleanHex[i + 1];
            } else {
                digits += cleanHex[i];
            }
        }
        
        // Apply scale if specified
        let result = digits;
        if (scale > 0) {
            const integerPart = digits.substring(0, digits.length - scale);
            const decimalPart = digits.substring(digits.length - scale);
            result = (integerPart || '0') + '.' + decimalPart;
        }
        
        return new Decimal(isNegative ? '-' + result : result);
    } catch (error) {
        console.error('Error converting packed decimal:', error);
        return new Decimal(0);
    }
}

/**
 * Formats phone number with COBOL PIC X(13) pattern
 * Handles phone number formatting with area code
 * 
 * @param {string} phoneNumber - Phone number to format
 * @returns {string} Formatted phone number (XXX) XXX-XXXX
 */
export function formatPhoneNumber(phoneNumber) {
    if (!phoneNumber) {
        return '(000) 000-0000';
    }

    // Remove any non-digit characters
    const digits = phoneNumber.toString().replace(/\D/g, '');
    
    // Pad to 10 digits
    const paddedPhone = digits.padStart(10, '0').substring(0, 10);
    
    return '(' + paddedPhone.substring(0, 3) + ') ' + 
           paddedPhone.substring(3, 6) + '-' + 
           paddedPhone.substring(6, 10);
}

/**
 * Parses COBOL CCYYMMDD date string to JavaScript Date
 * Handles COBOL date format with century and leap year logic
 * 
 * @param {string} ccyymmdd - COBOL date string in CCYYMMDD format
 * @returns {Date} JavaScript Date object
 */
export function parseCcyymmdd(ccyymmdd) {
    if (!ccyymmdd || ccyymmdd.length !== 8) {
        return new Date(1900, 0, 1); // Default to 1900-01-01
    }

    try {
        const year = parseInt(ccyymmdd.substring(0, 4), 10);
        const month = parseInt(ccyymmdd.substring(4, 6), 10) - 1; // JavaScript months are 0-based
        const day = parseInt(ccyymmdd.substring(6, 8), 10);
        
        // Validate date components
        if (year < 1900 || year > 2099 || month < 0 || month > 11 || day < 1 || day > 31) {
            return new Date(1900, 0, 1);
        }
        
        const date = new Date(year, month, day);
        
        // Verify the date is valid (handles leap years automatically)
        if (date.getFullYear() !== year || date.getMonth() !== month || date.getDate() !== day) {
            return new Date(1900, 0, 1);
        }
        
        return date;
    } catch (error) {
        console.error('Error parsing CCYYMMDD date:', error);
        return new Date(1900, 0, 1);
    }
}

/**
 * Formats JavaScript Date to COBOL CCYYMMDD format
 * Ensures exact date representation for COBOL compatibility
 * 
 * @param {Date} date - JavaScript Date object
 * @returns {string} Date in CCYYMMDD format
 */
export function formatToCcyymmdd(date) {
    if (!date || !(date instanceof Date) || isNaN(date.getTime())) {
        return '19000101';
    }

    const year = date.getFullYear().toString().padStart(4, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    
    return year + month + day;
}