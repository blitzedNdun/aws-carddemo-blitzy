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
 * CardDemo Data Formatting Utilities
 * 
 * This module provides exact preservation of COBOL data types and precision
 * when converting between BMS copybook structures and React component data formats.
 * 
 * Key Requirements:
 * - All COBOL COMP-3 decimal precision must be exactly replicated using JavaScript BigDecimal
 * - Financial calculations must produce identical results with exact decimal precision
 * - Date handling must preserve COBOL calendar logic including leap year calculations
 * - No modification of existing COBOL data precision or rounding behavior is permitted
 */

import { Decimal } from 'decimal.js';

// Configure Decimal.js to match COBOL COMP-3 precision characteristics
// COBOL COMP-3 uses up to 31 decimal digits of precision
Decimal.set({
  precision: 31,              // Maximum precision for COBOL COMP-3 fields
  rounding: Decimal.ROUND_HALF_UP,  // COBOL standard rounding mode
  toExpNeg: -31,             // Exponential notation threshold
  toExpPos: 31,              // Exponential notation threshold
  modulo: Decimal.ROUND_HALF_UP
});

/**
 * Format currency amounts with exact COBOL precision preservation
 * Handles COBOL PIC +ZZZ,ZZZ,ZZZ.99 format equivalent
 * 
 * @param {number|string|Decimal} amount - The amount to format
 * @param {number} scale - Decimal places (default 2 for currency)
 * @param {boolean} showSign - Whether to show + sign for positive amounts
 * @param {boolean} suppressZeros - Whether to suppress leading zeros (Z format)
 * @returns {string} Formatted currency string
 */
export function formatCurrency(amount, scale = 2, showSign = false, suppressZeros = true) {
  try {
    if (amount === null || amount === undefined || amount === '') {
      return suppressZeros ? '' : '0.00';
    }

    // Convert to Decimal for exact precision
    const decimal = new Decimal(amount);
    
    // Handle zero values according to COBOL Z format
    if (decimal.isZero()) {
      return suppressZeros ? '' : '0.00';
    }

    // Apply scale with exact rounding
    const scaled = decimal.toFixed(scale, Decimal.ROUND_HALF_UP);
    const parts = scaled.split('.');
    const integerPart = parts[0];
    const decimalPart = parts[1] || '';

    // Add thousands separators (COBOL comma formatting)
    const formattedInteger = Math.abs(parseInt(integerPart))
      .toString()
      .replace(/\B(?=(\d{3})+(?!\d))/g, ',');

    // Construct final format
    let result = '';
    if (decimal.isNegative()) {
      result = '-' + formattedInteger;
    } else if (showSign) {
      result = '+' + formattedInteger;
    } else {
      result = formattedInteger;
    }

    // Add decimal part if scale > 0
    if (scale > 0) {
      result += '.' + decimalPart.padEnd(scale, '0');
    }

    return result;
  } catch (error) {
    console.error('Error formatting currency:', error);
    return suppressZeros ? '' : '0.00';
  }
}

/**
 * Format dates maintaining COBOL CCYYMMDD format
 * Preserves COBOL calendar logic including leap year handling
 * 
 * @param {Date|string} date - Date to format
 * @param {string} format - Output format ('CCYYMMDD', 'MM/DD/YYYY', 'display')
 * @returns {string} Formatted date string
 */
export function formatDate(date, format = 'CCYYMMDD') {
  try {
    if (!date) return '';

    let dateObj;
    if (typeof date === 'string') {
      // Handle CCYYMMDD input format
      if (date.length === 8 && /^\d{8}$/.test(date)) {
        const year = parseInt(date.substring(0, 4));
        const month = parseInt(date.substring(4, 6));
        const day = parseInt(date.substring(6, 8));
        dateObj = new Date(year, month - 1, day); // month is 0-based in JS
      } else {
        dateObj = new Date(date);
      }
    } else if (date instanceof Date) {
      dateObj = date;
    } else {
      return '';
    }

    // Validate date is valid
    if (isNaN(dateObj.getTime())) {
      return '';
    }

    switch (format.toLowerCase()) {
      case 'ccyymmdd':
        const year = dateObj.getFullYear().toString().padStart(4, '0');
        const month = (dateObj.getMonth() + 1).toString().padStart(2, '0');
        const day = dateObj.getDate().toString().padStart(2, '0');
        return year + month + day;
      
      case 'mm/dd/yyyy':
        const mm = (dateObj.getMonth() + 1).toString().padStart(2, '0');
        const dd = dateObj.getDate().toString().padStart(2, '0');
        const yyyy = dateObj.getFullYear().toString();
        return `${mm}/${dd}/${yyyy}`;
      
      case 'display':
        return dateObj.toLocaleDateString('en-US', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit'
        });
      
      default:
        return formatDate(date, 'CCYYMMDD');
    }
  } catch (error) {
    console.error('Error formatting date:', error);
    return '';
  }
}

/**
 * Convert COBOL COMMAREA structure to JSON object
 * Maintains field ordering and data type preservation
 * 
 * @param {Object} commareaBuffer - Raw COMMAREA data buffer
 * @param {Object} fieldMap - Field mapping configuration
 * @returns {Object} JSON representation of COMMAREA
 */
export function convertCommareaToJson(commareaBuffer, fieldMap) {
  try {
    if (!commareaBuffer || !fieldMap) {
      return {};
    }

    const result = {};
    
    // Process each field according to its COBOL definition
    Object.keys(fieldMap).forEach(fieldName => {
      const fieldDef = fieldMap[fieldName];
      const offset = fieldDef.offset || 0;
      const length = fieldDef.length || 0;
      const type = fieldDef.type || 'X';
      const scale = fieldDef.scale || 0;

      // Extract field data from buffer
      let fieldData = '';
      if (commareaBuffer.length >= offset + length) {
        fieldData = commareaBuffer.substring(offset, offset + length);
      } else if (commareaBuffer.length > offset) {
        fieldData = commareaBuffer.substring(offset);
      }

      // Convert based on COBOL type
      switch (type.toUpperCase()) {
        case 'X':
          result[fieldName] = formatPicX(fieldData, length);
          break;
        case '9':
          result[fieldName] = formatPic9(fieldData, length);
          break;
        case 'S9V9':
          result[fieldName] = formatPicS9V9(fieldData, length - scale, scale);
          break;
        case 'COMP':
        case 'COMP-3':
          result[fieldName] = convertPackedDecimal(fieldData, length, scale);
          break;
        default:
          result[fieldName] = fieldData.trim();
      }
    });

    return result;
  } catch (error) {
    console.error('Error converting COMMAREA to JSON:', error);
    return {};
  }
}

/**
 * Format account numbers per COBOL PIC 99999999999 (11 digits)
 * 
 * @param {string|number} accountNumber - Account number to format
 * @param {boolean} withDashes - Whether to include formatting dashes
 * @returns {string} Formatted account number
 */
export function formatAccountNumber(accountNumber, withDashes = false) {
  try {
    if (!accountNumber) return '';

    // Remove any non-numeric characters
    const numericOnly = accountNumber.toString().replace(/\D/g, '');
    
    // Pad to 11 digits
    const padded = numericOnly.padStart(11, '0');
    
    // Format with dashes if requested: XXXX-XXXX-XXX
    if (withDashes && padded.length === 11) {
      return `${padded.substring(0, 4)}-${padded.substring(4, 8)}-${padded.substring(8, 11)}`;
    }
    
    return padded;
  } catch (error) {
    console.error('Error formatting account number:', error);
    return '';
  }
}

/**
 * Format card numbers per COBOL PIC X(16) with masking
 * 
 * @param {string} cardNumber - Card number to format
 * @param {boolean} masked - Whether to mask the number (show only last 4 digits)
 * @param {boolean} withSpaces - Whether to include spacing (XXXX XXXX XXXX XXXX)
 * @returns {string} Formatted card number
 */
export function formatCardNumber(cardNumber, masked = true, withSpaces = true) {
  try {
    if (!cardNumber) return '';

    // Remove any non-numeric characters
    const numericOnly = cardNumber.toString().replace(/\D/g, '');
    
    // Pad to 16 digits
    const padded = numericOnly.padStart(16, '0');
    
    if (padded.length !== 16) {
      return ''; // Invalid card number length
    }

    let formatted = padded;
    
    // Apply masking if requested
    if (masked) {
      formatted = '**** **** **** ' + padded.substring(12, 16);
    } else if (withSpaces) {
      // Add spaces: XXXX XXXX XXXX XXXX
      formatted = padded.substring(0, 4) + ' ' +
                  padded.substring(4, 8) + ' ' +
                  padded.substring(8, 12) + ' ' +
                  padded.substring(12, 16);
    }
    
    return formatted;
  } catch (error) {
    console.error('Error formatting card number:', error);
    return '';
  }
}

/**
 * Parse COBOL decimal values maintaining exact precision
 * Handles both display and computational formats
 * 
 * @param {string} cobolValue - COBOL decimal string
 * @param {number} precision - Total digits
 * @param {number} scale - Decimal places
 * @returns {Decimal} Parsed decimal value
 */
export function parseCobolDecimal(cobolValue, precision = 31, scale = 0) {
  try {
    if (!cobolValue) return new Decimal(0);

    // Handle COBOL signed notation (trailing sign)
    let numericValue = cobolValue.toString().trim();
    let isNegative = false;

    // Check for COBOL sign conventions
    if (numericValue.endsWith('-') || numericValue.endsWith('}')) {
      isNegative = true;
      numericValue = numericValue.slice(0, -1);
    } else if (numericValue.endsWith('+') || numericValue.endsWith('{')) {
      numericValue = numericValue.slice(0, -1);
    }

    // Remove any formatting characters
    numericValue = numericValue.replace(/[^\d.]/g, '');

    // Apply decimal scale if no decimal point present
    if (scale > 0 && !numericValue.includes('.')) {
      const len = numericValue.length;
      if (len > scale) {
        numericValue = numericValue.substring(0, len - scale) + '.' + 
                      numericValue.substring(len - scale);
      } else {
        numericValue = '0.' + numericValue.padStart(scale, '0');
      }
    }

    const decimal = new Decimal(numericValue || '0');
    return isNegative ? decimal.negated() : decimal;
  } catch (error) {
    console.error('Error parsing COBOL decimal:', error);
    return new Decimal(0);
  }
}

/**
 * Format Social Security Numbers per COBOL pattern XXX-XX-XXXX
 * 
 * @param {string} ssn - SSN to format
 * @param {boolean} masked - Whether to mask the SSN (show only last 4 digits)
 * @returns {string} Formatted SSN
 */
export function formatSSN(ssn, masked = true) {
  try {
    if (!ssn) return '';

    // Remove any non-numeric characters
    const numericOnly = ssn.toString().replace(/\D/g, '');
    
    if (numericOnly.length !== 9) {
      return ''; // Invalid SSN length
    }

    if (masked) {
      return 'XXX-XX-' + numericOnly.substring(5, 9);
    } else {
      return numericOnly.substring(0, 3) + '-' +
             numericOnly.substring(3, 5) + '-' +
             numericOnly.substring(5, 9);
    }
  } catch (error) {
    console.error('Error formatting SSN:', error);
    return '';
  }
}

/**
 * Format COBOL PIC X(n) text fields with proper padding and trimming
 * 
 * @param {string} value - Text value to format
 * @param {number} length - Field length
 * @param {boolean} rightPad - Whether to right-pad with spaces
 * @returns {string} Formatted text field
 */
export function formatPicX(value, length, rightPad = true) {
  try {
    if (value === null || value === undefined) {
      return rightPad ? ''.padEnd(length, ' ') : '';
    }

    const stringValue = value.toString();
    
    // Truncate if too long
    if (stringValue.length > length) {
      return stringValue.substring(0, length);
    }
    
    // Pad if too short
    if (rightPad) {
      return stringValue.padEnd(length, ' ');
    } else {
      return stringValue.trimRight();
    }
  } catch (error) {
    console.error('Error formatting PIC X field:', error);
    return rightPad ? ''.padEnd(length, ' ') : '';
  }
}

/**
 * Format COBOL PIC 9(n) numeric fields with zero padding
 * 
 * @param {string|number} value - Numeric value to format
 * @param {number} length - Field length
 * @param {boolean} leftPad - Whether to left-pad with zeros
 * @returns {string} Formatted numeric field
 */
export function formatPic9(value, length, leftPad = true) {
  try {
    if (value === null || value === undefined || value === '') {
      return leftPad ? '0'.repeat(length) : '';
    }

    // Remove any non-numeric characters
    const numericOnly = value.toString().replace(/\D/g, '');
    
    // Truncate if too long
    if (numericOnly.length > length) {
      return numericOnly.substring(0, length);
    }
    
    // Pad if too short
    if (leftPad) {
      return numericOnly.padStart(length, '0');
    } else {
      return numericOnly;
    }
  } catch (error) {
    console.error('Error formatting PIC 9 field:', error);
    return leftPad ? '0'.repeat(length) : '';
  }
}

/**
 * Format COBOL PIC S9(n)V9(m) signed decimal fields
 * Maintains exact precision and sign handling
 * 
 * @param {string|number|Decimal} value - Decimal value to format
 * @param {number} integerDigits - Number of integer digits
 * @param {number} decimalDigits - Number of decimal digits
 * @param {boolean} signSeparate - Whether sign is separate character
 * @returns {string} Formatted signed decimal field
 */
export function formatPicS9V9(value, integerDigits, decimalDigits, signSeparate = false) {
  try {
    if (value === null || value === undefined || value === '') {
      const zeros = '0'.repeat(integerDigits) + 
                   (decimalDigits > 0 ? '.' + '0'.repeat(decimalDigits) : '');
      return signSeparate ? '+' + zeros : zeros;
    }

    const decimal = new Decimal(value);
    const isNegative = decimal.isNegative();
    const absoluteValue = decimal.absoluteValue();
    
    // Format with exact scale
    const formatted = absoluteValue.toFixed(decimalDigits, Decimal.ROUND_HALF_UP);
    const parts = formatted.split('.');
    const intPart = parts[0] || '0';
    const decPart = parts[1] || '';

    // Pad integer part
    let result = intPart.padStart(integerDigits, '0');
    
    // Add decimal part if needed
    if (decimalDigits > 0) {
      result += '.' + decPart.padEnd(decimalDigits, '0');
    }

    // Handle sign
    if (signSeparate) {
      result = (isNegative ? '-' : '+') + result;
    } else if (isNegative) {
      // COBOL trailing sign convention
      result = result + '-';
    }

    return result;
  } catch (error) {
    console.error('Error formatting PIC S9V9 field:', error);
    const zeros = '0'.repeat(integerDigits) + 
                 (decimalDigits > 0 ? '.' + '0'.repeat(decimalDigits) : '');
    return signSeparate ? '+' + zeros : zeros;
  }
}

/**
 * Convert COBOL zoned decimal format to JavaScript number
 * Handles COBOL sign representation in last nibble
 * 
 * @param {string} zonedValue - Zoned decimal string
 * @param {number} precision - Total precision
 * @param {number} scale - Decimal scale
 * @returns {Decimal} Converted decimal value
 */
export function convertZonedDecimal(zonedValue, precision = 31, scale = 0) {
  try {
    if (!zonedValue) return new Decimal(0);

    let value = zonedValue.toString();
    let isNegative = false;

    // Check last character for COBOL zoned decimal sign
    const lastChar = value.charAt(value.length - 1);
    
    // COBOL zoned decimal sign conventions
    // Positive: {ABCDEFGHI} represent +0 to +9
    // Negative: }JKLMNOPQR represent -0 to -9
    if (lastChar === '{') {
      value = value.substring(0, value.length - 1) + '0';
    } else if (lastChar >= 'A' && lastChar <= 'I') {
      value = value.substring(0, value.length - 1) + String.fromCharCode(lastChar.charCodeAt(0) - 'A'.charCodeAt(0) + '1'.charCodeAt(0));
    } else if (lastChar === '}') {
      isNegative = true;
      value = value.substring(0, value.length - 1) + '0';
    } else if (lastChar >= 'J' && lastChar <= 'R') {
      isNegative = true;
      value = value.substring(0, value.length - 1) + String.fromCharCode(lastChar.charCodeAt(0) - 'J'.charCodeAt(0) + '1'.charCodeAt(0));
    }

    // Apply scale
    if (scale > 0 && !value.includes('.')) {
      const len = value.length;
      if (len > scale) {
        value = value.substring(0, len - scale) + '.' + value.substring(len - scale);
      } else {
        value = '0.' + value.padStart(scale, '0');
      }
    }

    const decimal = new Decimal(value || '0');
    return isNegative ? decimal.negated() : decimal;
  } catch (error) {
    console.error('Error converting zoned decimal:', error);
    return new Decimal(0);
  }
}

/**
 * Convert COBOL packed decimal (COMP-3) format to JavaScript number
 * Maintains exact precision for financial calculations
 * 
 * @param {string|ArrayBuffer|Uint8Array} packedValue - Packed decimal data
 * @param {number} precision - Total precision (digits)
 * @param {number} scale - Decimal scale
 * @returns {Decimal} Converted decimal value
 */
export function convertPackedDecimal(packedValue, precision = 31, scale = 0) {
  try {
    if (!packedValue) return new Decimal(0);

    let bytes;
    if (typeof packedValue === 'string') {
      // Check if this looks like a hex string or regular decimal
      const isHexLike = /^[0-9A-Fa-f]+$/.test(packedValue);
      
      if (isHexLike) {
        // For hex string representation like "1234C", interpret as packed decimal
        // Special handling: if string ends with C/D (common COBOL signs), parse accordingly
        if (packedValue.length % 2 === 1) {
          // Odd length: pad with leading zero
          packedValue = '0' + packedValue;
        }
        
        bytes = [];
        for (let i = 0; i < packedValue.length; i += 2) {
          bytes.push(parseInt(packedValue.substr(i, 2), 16));
        }
      } else {
        // Fallback: try to parse as regular decimal
        return parseCobolDecimal(packedValue.toString(), precision, scale);
      }
    } else if (packedValue instanceof ArrayBuffer) {
      bytes = new Uint8Array(packedValue);
    } else if (packedValue instanceof Uint8Array) {
      bytes = packedValue;
    } else {
      // Fallback: try to parse as regular decimal
      return parseCobolDecimal(packedValue.toString(), precision, scale);
    }

    let result = '';
    let isNegative = false;

    // Process each byte
    for (let i = 0; i < bytes.length; i++) {
      const byte = bytes[i];
      const highNibble = (byte >> 4) & 0x0F;
      const lowNibble = byte & 0x0F;

      if (i === bytes.length - 1) {
        // Last byte: high nibble is last digit, low nibble is sign
        if (highNibble <= 9) {
          result += highNibble.toString();
        }
        isNegative = (lowNibble === 0x0D || lowNibble === 0x0B); // D or B = negative
      } else {
        // Regular byte: both nibbles are digits
        result += highNibble.toString() + lowNibble.toString();
      }
    }

    // Apply decimal scale
    if (scale > 0 && result.length > scale) {
      const integerPart = result.substring(0, result.length - scale);
      const decimalPart = result.substring(result.length - scale);
      result = integerPart + '.' + decimalPart;
    } else if (scale > 0 && result.length > 0) {
      result = '0.' + result.padStart(scale, '0');
    }

    const decimal = new Decimal(result || '0');
    return isNegative ? decimal.negated() : decimal;
  } catch (error) {
    console.error('Error converting packed decimal:', error);
    return new Decimal(0);
  }
}

/**
 * Format phone numbers per COBOL pattern (XXX) XXX-XXXX
 * 
 * @param {string} phoneNumber - Phone number to format
 * @param {boolean} withParentheses - Whether to include parentheses around area code
 * @returns {string} Formatted phone number
 */
export function formatPhoneNumber(phoneNumber, withParentheses = true) {
  try {
    if (!phoneNumber) return '';

    // Remove any non-numeric characters
    const numericOnly = phoneNumber.toString().replace(/\D/g, '');
    
    if (numericOnly.length === 10) {
      // Standard US format
      if (withParentheses) {
        return `(${numericOnly.substring(0, 3)}) ${numericOnly.substring(3, 6)}-${numericOnly.substring(6, 10)}`;
      } else {
        return `${numericOnly.substring(0, 3)}-${numericOnly.substring(3, 6)}-${numericOnly.substring(6, 10)}`;
      }
    } else if (numericOnly.length === 11 && numericOnly.startsWith('1')) {
      // US format with country code
      const areaCode = numericOnly.substring(1, 4);
      const exchange = numericOnly.substring(4, 7);
      const number = numericOnly.substring(7, 11);
      if (withParentheses) {
        return `1 (${areaCode}) ${exchange}-${number}`;
      } else {
        return `1-${areaCode}-${exchange}-${number}`;
      }
    } else {
      // Return as-is for international or non-standard formats
      return numericOnly;
    }
  } catch (error) {
    console.error('Error formatting phone number:', error);
    return '';
  }
}

/**
 * Parse CCYYMMDD format string to JavaScript Date object
 * Preserves COBOL calendar logic including leap year calculations
 * 
 * @param {string} ccyymmdd - Date string in CCYYMMDD format
 * @param {boolean} strict - Whether to validate date components strictly
 * @returns {Date|null} Parsed Date object or null if invalid
 */
export function parseCcyymmdd(ccyymmdd, strict = true) {
  try {
    if (!ccyymmdd || ccyymmdd.length !== 8) {
      return null;
    }

    const yearStr = ccyymmdd.substring(0, 4);
    const monthStr = ccyymmdd.substring(4, 6);
    const dayStr = ccyymmdd.substring(6, 8);

    const year = parseInt(yearStr, 10);
    const month = parseInt(monthStr, 10);
    const day = parseInt(dayStr, 10);

    // Basic validation
    if (isNaN(year) || isNaN(month) || isNaN(day)) {
      return null;
    }

    if (strict) {
      // Validate ranges
      if (year < 1900 || year > 2999) return null;
      if (month < 1 || month > 12) return null;
      if (day < 1 || day > 31) return null;

      // Validate day based on month and leap year
      const daysInMonth = getDaysInMonth(year, month);
      if (day > daysInMonth) return null;
    }

    // Create date (month is 0-based in JavaScript)
    const date = new Date(year, month - 1, day);
    
    // Verify the date components match (handles invalid dates like Feb 30)
    if (strict && (date.getFullYear() !== year || 
                   date.getMonth() !== month - 1 || 
                   date.getDate() !== day)) {
      return null;
    }

    return date;
  } catch (error) {
    console.error('Error parsing CCYYMMDD:', error);
    return null;
  }
}

/**
 * Format JavaScript Date object to CCYYMMDD format string
 * Maintains COBOL date format conventions
 * 
 * @param {Date} date - Date object to format
 * @param {boolean} validate - Whether to validate the date first
 * @returns {string} Date string in CCYYMMDD format or empty string if invalid
 */
export function formatToCcyymmdd(date, validate = true) {
  try {
    if (!date || !(date instanceof Date)) {
      return '';
    }

    if (validate && isNaN(date.getTime())) {
      return '';
    }

    const year = date.getFullYear().toString().padStart(4, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');

    return year + month + day;
  } catch (error) {
    console.error('Error formatting to CCYYMMDD:', error);
    return '';
  }
}

/**
 * Helper function to get days in a month, accounting for leap years
 * Implements COBOL leap year logic
 * 
 * @param {number} year - The year
 * @param {number} month - The month (1-12)
 * @returns {number} Number of days in the month
 */
function getDaysInMonth(year, month) {
  // COBOL leap year rules: divisible by 4, except century years must be divisible by 400
  const isLeapYear = (year % 4 === 0 && year % 100 !== 0) || (year % 400 === 0);
  
  const daysInMonth = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  
  if (month === 2 && isLeapYear) {
    return 29;
  }
  
  return daysInMonth[month - 1] || 31;
}