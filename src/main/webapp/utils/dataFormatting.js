/**
 * Data Formatting Utilities for CardDemo Application
 * 
 * This module provides comprehensive data formatting utilities that ensure exact preservation 
 * of COBOL data types and precision when converting between BMS copybook structures and 
 * React component data formats. All financial calculations maintain COBOL COMP-3 decimal 
 * precision to preserve the accuracy required for financial operations.
 * 
 * Key Features:
 * - COBOL COMP-3 to BigDecimal conversion with exact decimal precision
 * - Date handling preserving COBOL calendar logic including leap year calculations  
 * - Currency formatting maintaining COBOL arithmetic precision
 * - PIC clause data type conversions (PIC X(n), PIC 9(n), PIC S9(n)V9(m))
 * - SSN, phone number, and account number formatting matching COBOL edit patterns
 * - Zoned decimal and packed decimal format conversions
 * - COMMAREA to JSON conversion for BMS map integration
 * 
 * @fileoverview CardDemo Data Formatting Utilities
 * @version 1.0.0
 * @author Blitzy Platform - CardDemo Migration Team
 * @copyright 2024 CardDemo Application Migration Project
 */

import Decimal from 'decimal.js';

// Configure Decimal.js to use COBOL COMP-3 equivalent precision
// DECIMAL128 context with exact precision matching COBOL arithmetic
Decimal.set({
    precision: 34,           // DECIMAL128 precision - 34 significant digits
    rounding: Decimal.ROUND_HALF_EVEN,  // COBOL-compatible rounding mode
    toExpNeg: -6143,        // Match DECIMAL128 exponent range
    toExpPos: 6144,
    maxE: 6144,
    minE: -6143,
    modulo: Decimal.ROUND_DOWN
});

/**
 * Formats currency values with exact COBOL COMP-3 precision
 * Converts numeric values to properly formatted currency strings maintaining
 * the exact decimal precision required for financial calculations.
 * 
 * @param {number|string|Decimal} amount - The amount to format
 * @param {string} currencySymbol - Currency symbol (default: '$')
 * @param {boolean} showNegativeParens - Show negative amounts in parentheses (COBOL style)
 * @returns {string} Formatted currency string with exact 2-decimal precision
 * 
 * @example
 * formatCurrency(1234.56) // Returns "$1,234.56"
 * formatCurrency(-123.45, '$', true) // Returns "$(123.45)"
 * formatCurrency('9999999.99') // Returns "$9,999,999.99"
 */
export function formatCurrency(amount, currencySymbol = '$', showNegativeParens = false) {
    if (amount === null || amount === undefined || amount === '') {
        return `${currencySymbol}0.00`;
    }

    try {
        // Convert to Decimal for exact precision arithmetic
        const decimalAmount = new Decimal(amount);
        
        // Check for invalid values
        if (!decimalAmount.isFinite()) {
            return `${currencySymbol}0.00`;
        }

        const isNegative = decimalAmount.isNegative();
        const absoluteAmount = decimalAmount.abs();
        
        // Format with exactly 2 decimal places (COBOL COMP-3 S9(10)V99 equivalent)
        const formattedNumber = absoluteAmount.toFixed(2);
        
        // Add thousand separators matching COBOL edit patterns
        const parts = formattedNumber.split('.');
        const wholePart = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
        const formattedAmount = `${wholePart}.${parts[1]}`;
        
        // Apply COBOL-style negative formatting
        if (isNegative) {
            return showNegativeParens 
                ? `${currencySymbol}(${formattedAmount})`
                : `-${currencySymbol}${formattedAmount}`;
        }
        
        return `${currencySymbol}${formattedAmount}`;
    } catch (error) {
        console.error('Error formatting currency:', error);
        return `${currencySymbol}0.00`;
    }
}

/**
 * Formats dates to/from COBOL CCYYMMDD format
 * Handles conversion between JavaScript Date objects and COBOL date formats,
 * preserving calendar logic including leap year calculations exactly as COBOL handles them.
 * 
 * @param {Date|string} date - Date to format (Date object or CCYYMMDD string)
 * @param {string} format - Output format ('CCYYMMDD', 'MM/DD/YYYY', 'display')
 * @returns {string} Formatted date string
 * 
 * @example
 * formatDate(new Date('2024-03-15'), 'CCYYMMDD') // Returns "20240315"
 * formatDate('20240315', 'MM/DD/YYYY') // Returns "03/15/2024" 
 * formatDate('20240315', 'display') // Returns "March 15, 2024"
 */
export function formatDate(date, format = 'MM/DD/YYYY') {
    if (!date) {
        return '';
    }

    try {
        let dateObj;
        
        // Handle CCYYMMDD string input (8 characters)
        if (typeof date === 'string' && date.length === 8) {
            const year = parseInt(date.substring(0, 4), 10);
            const month = parseInt(date.substring(4, 6), 10) - 1; // JavaScript months are 0-based
            const day = parseInt(date.substring(6, 8), 10);
            
            // Validate date components using COBOL calendar logic
            if (year < 1601 || year > 3999 || month < 0 || month > 11 || day < 1 || day > 31) {
                return '';
            }
            
            dateObj = new Date(year, month, day);
            
            // Validate the constructed date (handles leap years correctly)
            if (dateObj.getFullYear() !== year || 
                dateObj.getMonth() !== month || 
                dateObj.getDate() !== day) {
                return '';
            }
        } else if (date instanceof Date) {
            dateObj = date;
        } else {
            dateObj = new Date(date);
        }

        // Check for invalid date
        if (isNaN(dateObj.getTime())) {
            return '';
        }

        // Format based on requested output format
        switch (format.toUpperCase()) {
            case 'CCYYMMDD':
                const year = dateObj.getFullYear().toString().padStart(4, '0');
                const month = (dateObj.getMonth() + 1).toString().padStart(2, '0');
                const day = dateObj.getDate().toString().padStart(2, '0');
                return `${year}${month}${day}`;
                
            case 'MM/DD/YYYY':
                const mm = (dateObj.getMonth() + 1).toString().padStart(2, '0');
                const dd = dateObj.getDate().toString().padStart(2, '0');
                const yyyy = dateObj.getFullYear();
                return `${mm}/${dd}/${yyyy}`;
                
            case 'DISPLAY':
                return dateObj.toLocaleDateString('en-US', {
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric'
                });
                
            default:
                return dateObj.toLocaleDateString();
        }
    } catch (error) {
        console.error('Error formatting date:', error);
        return '';
    }
}

/**
 * Converts COBOL COMMAREA structure to JSON format
 * Transforms BMS copybook field structures into JSON objects that React components can consume,
 * maintaining field names, data types, and validation rules from the original COBOL definitions.
 * 
 * @param {Object} commarea - COMMAREA structure with COBOL field definitions
 * @param {Object} fieldMap - Mapping of COBOL field names to JSON property names
 * @returns {Object} JSON object compatible with React component props
 * 
 * @example
 * const commarea = { USERIDI: 'ADMIN001', CURTIMEI: '14:30:25' };
 * const fieldMap = { USERIDI: 'userId', CURTIMEI: 'currentTime' };
 * convertCommareaToJson(commarea, fieldMap) // Returns { userId: 'ADMIN001', currentTime: '14:30:25' }
 */
export function convertCommareaToJson(commarea, fieldMap = {}) {
    if (!commarea || typeof commarea !== 'object') {
        return {};
    }

    const result = {};
    
    try {
        // Process each field in the COMMAREA
        Object.keys(commarea).forEach(cobolField => {
            const value = commarea[cobolField];
            const jsonField = fieldMap[cobolField] || cobolField.toLowerCase();
            
            // Skip COBOL system fields (length and attribute fields)
            if (cobolField.endsWith('L') || cobolField.endsWith('F') || 
                cobolField.endsWith('A') || cobolField.endsWith('C') ||
                cobolField.endsWith('P') || cobolField.endsWith('H') ||
                cobolField.endsWith('V')) {
                return;
            }
            
            // Process data fields (ending with 'I' for input or 'O' for output)
            if (cobolField.endsWith('I') || cobolField.endsWith('O')) {
                result[jsonField] = processCobolField(value, cobolField);
            } else {
                result[jsonField] = value;
            }
        });
        
        return result;
    } catch (error) {
        console.error('Error converting COMMAREA to JSON:', error);
        return {};
    }
}

/**
 * Processes individual COBOL fields based on their characteristics
 * @private
 */
function processCobolField(value, fieldName) {
    if (value === null || value === undefined) {
        return '';
    }
    
    const stringValue = String(value).trim();
    
    // Date fields (typically 8 characters CCYYMMDD)
    if (fieldName.includes('DATE') && stringValue.length === 8) {
        return parseCcyymmdd(stringValue);
    }
    
    // Time fields 
    if (fieldName.includes('TIME')) {
        return stringValue;
    }
    
    // Numeric fields that should be converted to numbers
    if (/^\d+\.?\d*$/.test(stringValue) && stringValue.length > 0) {
        return parseFloat(stringValue) || 0;
    }
    
    return stringValue;
}

/**
 * Formats account numbers to 11-digit standard format
 * Ensures account numbers conform to the COBOL PIC 9(11) format used throughout
 * the CardDemo system, with proper zero-padding and validation.
 * 
 * @param {string|number} accountNumber - Raw account number
 * @returns {string} Formatted 11-digit account number or empty string if invalid
 * 
 * @example
 * formatAccountNumber('123456789') // Returns "00123456789"
 * formatAccountNumber('12345678901') // Returns "12345678901"
 * formatAccountNumber('invalid') // Returns ""
 */
export function formatAccountNumber(accountNumber) {
    if (!accountNumber) {
        return '';
    }
    
    try {
        // Convert to string and remove any non-numeric characters
        const cleanNumber = String(accountNumber).replace(/\D/g, '');
        
        // Validate length - must be 11 digits or less
        if (cleanNumber.length === 0 || cleanNumber.length > 11) {
            return '';
        }
        
        // Pad with leading zeros to make exactly 11 digits (COBOL PIC 9(11))
        return cleanNumber.padStart(11, '0');
    } catch (error) {
        console.error('Error formatting account number:', error);
        return '';
    }
}

/**
 * Formats card numbers to 16-digit standard format with optional masking
 * Handles credit card number formatting with Luhn algorithm validation
 * and supports both full display and masked display for security.
 * 
 * @param {string|number} cardNumber - Raw card number
 * @param {boolean} masked - Whether to mask the card number (show last 4 digits)
 * @param {string} maskChar - Character to use for masking (default: '*')
 * @returns {string} Formatted card number
 * 
 * @example
 * formatCardNumber('4000123456789012') // Returns "4000 1234 5678 9012"
 * formatCardNumber('4000123456789012', true) // Returns "**** **** **** 9012"
 * formatCardNumber('4000123456789012', true, 'X') // Returns "XXXX XXXX XXXX 9012"
 */
export function formatCardNumber(cardNumber, masked = false, maskChar = '*') {
    if (!cardNumber) {
        return '';
    }
    
    try {
        // Convert to string and remove any non-numeric characters
        const cleanNumber = String(cardNumber).replace(/\D/g, '');
        
        // Validate length - must be exactly 16 digits for this system
        if (cleanNumber.length !== 16) {
            return '';
        }
        
        // Optional: Validate using Luhn algorithm for credit card numbers
        if (!isValidLuhnNumber(cleanNumber)) {
            // Still format invalid numbers but log the issue
            console.warn('Invalid card number (Luhn check failed):', cleanNumber);
        }
        
        let formatted;
        if (masked) {
            // Show only last 4 digits, mask the rest
            const maskedPart = maskChar.repeat(12);
            const visiblePart = cleanNumber.slice(-4);
            formatted = maskedPart + visiblePart;
        } else {
            formatted = cleanNumber;
        }
        
        // Add spaces every 4 digits for readability
        return formatted.replace(/(.{4})/g, '$1 ').trim();
    } catch (error) {
        console.error('Error formatting card number:', error);
        return '';
    }
}

/**
 * Validates credit card numbers using Luhn algorithm
 * @private
 */
function isValidLuhnNumber(cardNumber) {
    const digits = cardNumber.split('').map(Number);
    let sum = 0;
    let alternate = false;
    
    // Process digits from right to left
    for (let i = digits.length - 1; i >= 0; i--) {
        let n = digits[i];
        
        if (alternate) {
            n *= 2;
            if (n > 9) {
                n = (n % 10) + 1;
            }
        }
        
        sum += n;
        alternate = !alternate;
    }
    
    return (sum % 10) === 0;
}

/**
 * Parses COBOL decimal values maintaining exact precision
 * Converts COBOL packed decimal (COMP-3) and zoned decimal representations
 * to JavaScript Decimal objects with exact precision preservation.
 * 
 * @param {string|number} cobolDecimal - COBOL decimal value 
 * @param {number} integerDigits - Number of integer digits
 * @param {number} decimalDigits - Number of decimal places
 * @returns {Decimal} Decimal object with exact COBOL precision
 * 
 * @example
 * parseCobolDecimal('123456', 4, 2) // Returns Decimal('1234.56')
 * parseCobolDecimal('000999', 4, 2) // Returns Decimal('9.99')
 */
export function parseCobolDecimal(cobolDecimal, integerDigits = 10, decimalDigits = 2) {
    if (cobolDecimal === null || cobolDecimal === undefined || cobolDecimal === '') {
        return new Decimal(0);
    }
    
    try {
        let cleanValue = String(cobolDecimal).trim();
        
        // Remove any existing decimal points for processing
        cleanValue = cleanValue.replace(/\./g, '');
        
        // Handle negative values (COBOL sign conventions)
        let isNegative = false;
        if (cleanValue.startsWith('-') || cleanValue.endsWith('-')) {
            isNegative = true;
            cleanValue = cleanValue.replace(/-/g, '');
        }
        
        // Handle COBOL packed decimal format where last digit might contain sign
        // In COBOL COMP-3, the sign is stored in the last nibble
        if (/[A-F]$/.test(cleanValue.toUpperCase())) {
            const lastChar = cleanValue.slice(-1).toUpperCase();
            // COBOL sign conventions: A-I = positive 0-9, J-R = negative 0-9
            if (lastChar >= 'A' && lastChar <= 'I') {
                cleanValue = cleanValue.slice(0, -1) + String(lastChar.charCodeAt(0) - 65);
            } else if (lastChar >= 'J' && lastChar <= 'R') {
                isNegative = true;
                cleanValue = cleanValue.slice(0, -1) + String(lastChar.charCodeAt(0) - 74);
            }
        }
        
        // Ensure we have only numeric characters
        cleanValue = cleanValue.replace(/\D/g, '');
        
        if (cleanValue.length === 0) {
            return new Decimal(0);
        }
        
        // Pad with leading zeros if necessary
        const totalDigits = integerDigits + decimalDigits;
        cleanValue = cleanValue.padStart(totalDigits, '0');
        
        // Insert decimal point at correct position
        let formattedValue;
        if (decimalDigits > 0) {
            const integerPart = cleanValue.slice(0, -decimalDigits) || '0';
            const decimalPart = cleanValue.slice(-decimalDigits);
            formattedValue = `${integerPart}.${decimalPart}`;
        } else {
            formattedValue = cleanValue;
        }
        
        // Create Decimal with exact precision
        const result = new Decimal(formattedValue);
        return isNegative ? result.negated() : result;
        
    } catch (error) {
        console.error('Error parsing COBOL decimal:', error);
        return new Decimal(0);
    }
}

/**
 * Formats Social Security Numbers with proper masking and validation
 * Applies standard SSN formatting (XXX-XX-XXXX) with optional masking
 * for security and privacy protection.
 * 
 * @param {string} ssn - Raw SSN value
 * @param {boolean} masked - Whether to mask the SSN (show last 4 digits)
 * @param {string} maskChar - Character to use for masking
 * @returns {string} Formatted SSN string
 * 
 * @example
 * formatSSN('123456789') // Returns "123-45-6789"
 * formatSSN('123456789', true) // Returns "***-**-6789"
 */
export function formatSSN(ssn, masked = false, maskChar = '*') {
    if (!ssn) {
        return '';
    }
    
    try {
        // Remove any non-numeric characters
        const cleanSSN = String(ssn).replace(/\D/g, '');
        
        // Validate length - must be exactly 9 digits
        if (cleanSSN.length !== 9) {
            return '';
        }
        
        if (masked) {
            // Show only last 4 digits: ***-**-XXXX
            const maskedArea = maskChar.repeat(3);
            const maskedGroup = maskChar.repeat(2);
            const visiblePart = cleanSSN.slice(-4);
            return `${maskedArea}-${maskedGroup}-${visiblePart}`;
        } else {
            // Format as XXX-XX-XXXX
            return `${cleanSSN.slice(0, 3)}-${cleanSSN.slice(3, 5)}-${cleanSSN.slice(5, 9)}`;
        }
    } catch (error) {
        console.error('Error formatting SSN:', error);
        return '';
    }
}

/**
 * Formats COBOL PIC X(n) fields - alphanumeric data
 * Handles COBOL character fields with proper length validation and padding.
 * 
 * @param {string} value - Input value
 * @param {number} length - Expected field length from PIC X(n)
 * @param {boolean} leftPad - Whether to left-pad with spaces (default: right-pad)
 * @returns {string} Formatted field value
 * 
 * @example
 * formatPicX('HELLO', 10) // Returns "HELLO     "
 * formatPicX('HELLO', 10, true) // Returns "     HELLO"
 */
export function formatPicX(value, length, leftPad = false) {
    if (value === null || value === undefined) {
        return ' '.repeat(length);
    }
    
    try {
        let stringValue = String(value);
        
        // Truncate if too long
        if (stringValue.length > length) {
            stringValue = stringValue.substring(0, length);
        }
        
        // Pad to exact length
        if (leftPad) {
            return stringValue.padStart(length, ' ');
        } else {
            return stringValue.padEnd(length, ' ');
        }
    } catch (error) {
        console.error('Error formatting PIC X field:', error);
        return ' '.repeat(length);
    }
}

/**
 * Formats COBOL PIC 9(n) fields - numeric data
 * Handles COBOL numeric fields with zero-padding and validation.
 * 
 * @param {string|number} value - Input numeric value
 * @param {number} length - Expected field length from PIC 9(n)
 * @returns {string} Formatted numeric field value
 * 
 * @example
 * formatPic9('123', 5) // Returns "00123"
 * formatPic9(123, 5) // Returns "00123"
 */
export function formatPic9(value, length) {
    if (value === null || value === undefined || value === '') {
        return '0'.repeat(length);
    }
    
    try {
        // Convert to string and remove non-numeric characters
        const cleanValue = String(value).replace(/\D/g, '');
        
        if (cleanValue.length === 0) {
            return '0'.repeat(length);
        }
        
        // Truncate if too long
        if (cleanValue.length > length) {
            return cleanValue.substring(cleanValue.length - length);
        }
        
        // Left-pad with zeros
        return cleanValue.padStart(length, '0');
    } catch (error) {
        console.error('Error formatting PIC 9 field:', error);
        return '0'.repeat(length);
    }
}

/**
 * Formats COBOL PIC S9(n)V9(m) fields - signed decimal data
 * Handles COBOL signed decimal fields with exact precision preservation.
 * 
 * @param {string|number|Decimal} value - Input decimal value
 * @param {number} integerDigits - Number of integer digits (n)
 * @param {number} decimalDigits - Number of decimal digits (m) 
 * @returns {string} Formatted decimal field value
 * 
 * @example
 * formatPicS9V9('123.45', 5, 2) // Returns "00123.45"
 * formatPicS9V9('-12.3', 4, 2) // Returns "-0012.30"
 */
export function formatPicS9V9(value, integerDigits, decimalDigits) {
    if (value === null || value === undefined || value === '') {
        const zeros = '0'.repeat(integerDigits);
        const decimalZeros = decimalDigits > 0 ? '.' + '0'.repeat(decimalDigits) : '';
        return zeros + decimalZeros;
    }
    
    try {
        // Convert to Decimal for exact precision
        const decimalValue = new Decimal(value);
        
        // Format with exact decimal places
        const fixed = decimalValue.toFixed(decimalDigits);
        const parts = fixed.split('.');
        
        // Pad integer part
        let integerPart = parts[0];
        const isNegative = integerPart.startsWith('-');
        if (isNegative) {
            integerPart = integerPart.substring(1);
        }
        
        integerPart = integerPart.padStart(integerDigits, '0');
        
        // Truncate if too long
        if (integerPart.length > integerDigits) {
            integerPart = integerPart.substring(integerPart.length - integerDigits);
        }
        
        // Combine parts
        let result = integerPart;
        if (decimalDigits > 0) {
            result += '.' + (parts[1] || '0'.repeat(decimalDigits));
        }
        
        return isNegative ? '-' + result : result;
    } catch (error) {
        console.error('Error formatting PIC S9V9 field:', error);
        const zeros = '0'.repeat(integerDigits);
        const decimalZeros = decimalDigits > 0 ? '.' + '0'.repeat(decimalDigits) : '';
        return zeros + decimalZeros;
    }
}

/**
 * Converts COBOL zoned decimal format to JavaScript number
 * Handles COBOL zoned decimal representation where the sign is embedded
 * in the last digit using overpunch characters.
 * 
 * @param {string} zonedDecimal - COBOL zoned decimal string
 * @param {number} decimalPlaces - Number of implied decimal places
 * @returns {Decimal} Converted decimal value
 * 
 * @example
 * convertZonedDecimal('12345}', 2) // Returns Decimal('123.45') - positive
 * convertZonedDecimal('12345J', 2) // Returns Decimal('-123.41') - negative  
 */
export function convertZonedDecimal(zonedDecimal, decimalPlaces = 0) {
    if (!zonedDecimal || typeof zonedDecimal !== 'string') {
        return new Decimal(0);
    }
    
    try {
        const zoned = zonedDecimal.trim();
        if (zoned.length === 0) {
            return new Decimal(0);
        }
        
        // COBOL zoned decimal overpunch characters
        const positiveOverpunch = {
            '{': '0', 'A': '1', 'B': '2', 'C': '3', 'D': '4',
            'E': '5', 'F': '6', 'G': '7', 'H': '8', 'I': '9'
        };
        
        const negativeOverpunch = {
            '}': '0', 'J': '1', 'K': '2', 'L': '3', 'M': '4',
            'N': '5', 'O': '6', 'P': '7', 'Q': '8', 'R': '9'
        };
        
        let numericValue = zoned;
        let isNegative = false;
        
        // Check last character for overpunch
        const lastChar = zoned.slice(-1);
        if (positiveOverpunch[lastChar]) {
            numericValue = zoned.slice(0, -1) + positiveOverpunch[lastChar];
        } else if (negativeOverpunch[lastChar]) {
            numericValue = zoned.slice(0, -1) + negativeOverpunch[lastChar];
            isNegative = true;
        }
        
        // Remove any non-numeric characters except minus sign
        numericValue = numericValue.replace(/[^\d-]/g, '');
        
        if (numericValue.length === 0) {
            return new Decimal(0);
        }
        
        // Insert decimal point if needed
        let formattedValue = numericValue;
        if (decimalPlaces > 0 && numericValue.indexOf('.') === -1) {
            if (numericValue.length > decimalPlaces) {
                const integerPart = numericValue.slice(0, -decimalPlaces);
                const decimalPart = numericValue.slice(-decimalPlaces);
                formattedValue = `${integerPart}.${decimalPart}`;
            } else {
                formattedValue = '0.' + numericValue.padStart(decimalPlaces, '0');
            }
        }
        
        const result = new Decimal(formattedValue);
        return isNegative ? result.negated() : result;
        
    } catch (error) {
        console.error('Error converting zoned decimal:', error);
        return new Decimal(0);
    }
}

/**
 * Converts COBOL packed decimal (COMP-3) format to JavaScript Decimal
 * Handles COBOL packed decimal representation where two digits are packed
 * into each byte with the sign in the last nibble.
 * 
 * @param {string} packedDecimal - Hexadecimal representation of packed decimal
 * @param {number} decimalPlaces - Number of decimal places
 * @returns {Decimal} Converted decimal value
 * 
 * @example
 * convertPackedDecimal('12345C', 2) // Returns Decimal('123.45') - positive
 * convertPackedDecimal('12345D', 2) // Returns Decimal('-123.45') - negative
 */
export function convertPackedDecimal(packedDecimal, decimalPlaces = 0) {
    if (!packedDecimal || typeof packedDecimal !== 'string') {
        return new Decimal(0);
    }
    
    try {
        const packed = packedDecimal.trim().toUpperCase();
        if (packed.length === 0) {
            return new Decimal(0);
        }
        
        // COBOL packed decimal sign codes
        const positiveSignCodes = ['A', 'C', 'E', 'F'];
        const negativeSignCodes = ['B', 'D'];
        
        // Extract sign from last nibble
        const lastChar = packed.slice(-1);
        let isNegative = negativeSignCodes.includes(lastChar);
        
        // Extract numeric digits (all characters except last nibble)
        let numericPart = packed.slice(0, -1);
        
        // If last character is a hex digit (0-9), it's part of the number
        if (/[0-9]/.test(lastChar)) {
            numericPart += lastChar;
        }
        
        // Remove any non-hex characters
        numericPart = numericPart.replace(/[^0-9A-F]/g, '');
        
        if (numericPart.length === 0) {
            return new Decimal(0);
        }
        
        // Convert hex digits to decimal string
        let decimalString = '';
        for (let i = 0; i < numericPart.length; i++) {
            const char = numericPart[i];
            if (/[0-9]/.test(char)) {
                decimalString += char;
            } else {
                // Convert A-F to corresponding digits
                decimalString += String(parseInt(char, 16));
            }
        }
        
        // Insert decimal point if needed
        let formattedValue = decimalString;
        if (decimalPlaces > 0 && decimalString.indexOf('.') === -1) {
            if (decimalString.length > decimalPlaces) {
                const integerPart = decimalString.slice(0, -decimalPlaces);
                const decimalPart = decimalString.slice(-decimalPlaces);
                formattedValue = `${integerPart}.${decimalPart}`;
            } else {
                formattedValue = '0.' + decimalString.padStart(decimalPlaces, '0');
            }
        }
        
        const result = new Decimal(formattedValue);
        return isNegative ? result.negated() : result;
        
    } catch (error) {
        console.error('Error converting packed decimal:', error);
        return new Decimal(0);
    }
}

/**
 * Formats phone numbers with standard US formatting
 * Applies standard phone number formatting with area code separation
 * and optional extension handling.
 * 
 * @param {string} phoneNumber - Raw phone number
 * @param {boolean} includeCountryCode - Whether to include +1 country code
 * @returns {string} Formatted phone number
 * 
 * @example
 * formatPhoneNumber('1234567890') // Returns "(123) 456-7890"
 * formatPhoneNumber('1234567890', true) // Returns "+1 (123) 456-7890"
 */
export function formatPhoneNumber(phoneNumber, includeCountryCode = false) {
    if (!phoneNumber) {
        return '';
    }
    
    try {
        // Remove all non-numeric characters
        const cleanNumber = String(phoneNumber).replace(/\D/g, '');
        
        // Handle different lengths
        if (cleanNumber.length === 10) {
            // Standard US format: (XXX) XXX-XXXX
            const areaCode = cleanNumber.slice(0, 3);
            const exchange = cleanNumber.slice(3, 6);
            const number = cleanNumber.slice(6, 10);
            const formatted = `(${areaCode}) ${exchange}-${number}`;
            return includeCountryCode ? `+1 ${formatted}` : formatted;
        } else if (cleanNumber.length === 11 && cleanNumber.startsWith('1')) {
            // US number with country code: 1XXXXXXXXXX
            const areaCode = cleanNumber.slice(1, 4);
            const exchange = cleanNumber.slice(4, 7);
            const number = cleanNumber.slice(7, 11);
            return `+1 (${areaCode}) ${exchange}-${number}`;
        } else if (cleanNumber.length === 7) {
            // Local number: XXX-XXXX
            const exchange = cleanNumber.slice(0, 3);
            const number = cleanNumber.slice(3, 7);
            return `${exchange}-${number}`;
        } else {
            // Invalid length, return original cleaned number
            return cleanNumber;
        }
    } catch (error) {
        console.error('Error formatting phone number:', error);
        return '';
    }
}

/**
 * Parses CCYYMMDD date string to JavaScript Date object
 * Converts COBOL CCYYMMDD format to JavaScript Date with proper validation
 * and leap year handling matching COBOL calendar logic.
 * 
 * @param {string} ccyymmdd - Date in CCYYMMDD format (8 characters)
 * @returns {Date|null} JavaScript Date object or null if invalid
 * 
 * @example
 * parseCcyymmdd('20240315') // Returns Date object for March 15, 2024
 * parseCcyymmdd('20240229') // Returns Date object for Feb 29, 2024 (leap year)
 * parseCcyymmdd('invalid') // Returns null
 */
export function parseCcyymmdd(ccyymmdd) {
    if (!ccyymmdd || typeof ccyymmdd !== 'string' || ccyymmdd.length !== 8) {
        return null;
    }
    
    try {
        // Validate all characters are numeric
        if (!/^\d{8}$/.test(ccyymmdd)) {
            return null;
        }
        
        const year = parseInt(ccyymmdd.substring(0, 4), 10);
        const month = parseInt(ccyymmdd.substring(4, 6), 10);
        const day = parseInt(ccyymmdd.substring(6, 8), 10);
        
        // Validate ranges using COBOL calendar logic
        if (year < 1601 || year > 3999) {
            return null;
        }
        
        if (month < 1 || month > 12) {
            return null;
        }
        
        if (day < 1 || day > 31) {
            return null;
        }
        
        // Create date object (JavaScript months are 0-based)
        const dateObj = new Date(year, month - 1, day);
        
        // Validate the constructed date (handles leap years and month lengths)
        if (dateObj.getFullYear() !== year || 
            dateObj.getMonth() !== (month - 1) || 
            dateObj.getDate() !== day) {
            return null;
        }
        
        return dateObj;
    } catch (error) {
        console.error('Error parsing CCYYMMDD date:', error);
        return null;
    }
}

/**
 * Formats JavaScript Date object to CCYYMMDD string
 * Converts JavaScript Date to COBOL CCYYMMDD format with proper
 * zero-padding and validation.
 * 
 * @param {Date} date - JavaScript Date object
 * @returns {string} Date in CCYYMMDD format or empty string if invalid
 * 
 * @example
 * formatToCcyymmdd(new Date('2024-03-15')) // Returns "20240315"
 * formatToCcyymmdd(new Date('2024-02-29')) // Returns "20240229"
 * formatToCcyymmdd(null) // Returns ""
 */
export function formatToCcyymmdd(date) {
    if (!date || !(date instanceof Date) || isNaN(date.getTime())) {
        return '';
    }
    
    try {
        const year = date.getFullYear();
        const month = date.getMonth() + 1; // JavaScript months are 0-based
        const day = date.getDate();
        
        // Validate COBOL date ranges
        if (year < 1601 || year > 3999) {
            return '';
        }
        
        // Format with proper zero-padding
        const formattedYear = year.toString().padStart(4, '0');
        const formattedMonth = month.toString().padStart(2, '0');
        const formattedDay = day.toString().padStart(2, '0');
        
        return `${formattedYear}${formattedMonth}${formattedDay}`;
    } catch (error) {
        console.error('Error formatting date to CCYYMMDD:', error);
        return '';
    }
}

// Export all utility functions as named exports for modular usage
export default {
    formatCurrency,
    formatDate,
    convertCommareaToJson,
    formatAccountNumber,
    formatCardNumber,
    parseCobolDecimal,
    formatSSN,
    formatPicX,
    formatPic9,
    formatPicS9V9,
    convertZonedDecimal,
    convertPackedDecimal,
    formatPhoneNumber,
    parseCcyymmdd,
    formatToCcyymmdd
};