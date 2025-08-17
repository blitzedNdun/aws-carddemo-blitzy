/**
 * CobolDataConverter.js
 *
 * JavaScript utility module for converting between COBOL data types and JavaScript types,
 * ensuring exact numeric precision and format compatibility when exchanging data between
 * the React frontend and Spring Boot backend that processes COBOL-migrated business logic.
 *
 * This module maintains COBOL COMP-3 packed decimal precision using decimal.js library
 * to prevent JavaScript's floating-point precision errors in financial calculations.
 *
 * Key Features:
 * - COBOL COMP-3 packed decimal conversion with exact precision
 * - COBOL picture clause parsing and formatting
 * - Fixed-length field padding and truncation matching COBOL behavior
 * - Financial calculation precision preservation
 * - Currency formatting with proper decimal places
 */

import { Decimal } from 'decimal.js';

/**
 * COBOL decimal separator constants for internationalization support
 */
export const COBOL_DECIMAL_SEPARATORS = {
  US: '.',
  EUROPEAN: ',',
  DEFAULT: '.',
};

/**
 * COBOL rounding modes matching COBOL ROUNDED clause behavior
 */
export const COBOL_ROUNDING_MODES = {
  HALF_UP: Decimal.ROUND_HALF_UP,        // COBOL default ROUNDED behavior
  HALF_DOWN: Decimal.ROUND_HALF_DOWN,
  HALF_EVEN: Decimal.ROUND_HALF_EVEN,    // Banker's rounding
  UP: Decimal.ROUND_UP,                  // Always round away from zero
  DOWN: Decimal.ROUND_DOWN,              // Always round toward zero (truncate)
  CEILING: Decimal.ROUND_CEIL,           // Round toward positive infinity
  FLOOR: Decimal.ROUND_FLOOR,             // Round toward negative infinity
};

// Configure Decimal.js to use COBOL-compatible defaults
Decimal.set({
  precision: 31,                         // Maximum COBOL precision (31 digits)
  rounding: COBOL_ROUNDING_MODES.HALF_UP, // COBOL ROUNDED clause default
  toExpNeg: -7,                          // Prevent scientific notation for small numbers
  toExpPos: 21,                          // Prevent scientific notation for large numbers
  maxE: 9e15,                            // Maximum exponent
  minE: -9e15,                           // Minimum exponent
  modulo: Decimal.ROUND_DOWN,            // Modulo operation behavior
  crypto: false,                          // Use Math.random for performance
});

/**
 * Convert COBOL COMP-3 packed decimal representation to JavaScript Decimal
 *
 * COBOL COMP-3 packs two decimal digits into each byte, with the last half-byte
 * containing the sign (C=positive, D=negative, F=unsigned positive).
 *
 * @param {ArrayBuffer|Uint8Array|string} packedData - COMP-3 packed decimal data
 * @param {number} scale - Number of decimal places (from COBOL PIC clause)
 * @param {number} precision - Total number of digits (from COBOL PIC clause)
 * @returns {Decimal} Unpacked decimal value with exact precision
 *
 * @example
 * // COBOL: PIC S9(7)V99 COMP-3 VALUE 1234567.89
 * const result = fromComp3([0x01, 0x23, 0x45, 0x67, 0x89, 0x0C], 2, 9);
 * console.log(result.toString()); // "1234567.89"
 */
export function fromComp3(packedData, scale = 0, _precision = 15) {
  if (!packedData) {
    return new Decimal(0);
  }

  // Convert input to Uint8Array for consistent processing
  let bytes;
  if (typeof packedData === 'string') {
    // Handle hex string input (e.g., "01234567890C")
    const cleanHex = packedData.replace(/[^0-9A-Fa-f]/g, '');
    bytes = new Uint8Array(cleanHex.length / 2);
    for (let i = 0; i < cleanHex.length; i += 2) {
      bytes[i / 2] = parseInt(cleanHex.substr(i, 2), 16);
    }
  } else if (packedData instanceof ArrayBuffer) {
    bytes = new Uint8Array(packedData);
  } else {
    bytes = new Uint8Array(packedData);
  }

  if (bytes.length === 0) {
    return new Decimal(0);
  }

  // Extract digits from packed format
  let digits = '';
  let isNegative = false;

  // Process all bytes except the last one
  for (let i = 0; i < bytes.length - 1; i++) {
    const byte = bytes[i];
    const leftDigit = (byte >> 4) & 0x0F;
    const rightDigit = byte & 0x0F;
    digits += leftDigit.toString() + rightDigit.toString();
  }

  // Process the last byte (contains final digit and sign)
  if (bytes.length > 0) {
    const lastByte = bytes[bytes.length - 1];
    const digit = (lastByte >> 4) & 0x0F;
    const sign = lastByte & 0x0F;

    digits += digit.toString();

    // COBOL sign conventions: C/F = positive, D = negative
    isNegative = (sign === 0x0D);
  }

  // Apply decimal scaling before removing leading zeros to preserve COBOL precision
  if (scale > 0 && digits.length > scale) {
    const integerPart = digits.slice(0, -scale);
    const fractionalPart = digits.slice(-scale);
    digits = `${integerPart}.${fractionalPart}`;
  } else if (scale > 0) {
    // All digits are fractional - pad with leading zeros if needed
    digits = `0.${digits.padStart(scale, '0')}`;
  }

  // Remove leading zeros from integer part only, preserve at least one digit
  if (digits.includes('.')) {
    const parts = digits.split('.');
    parts[0] = parts[0].replace(/^0+/, '') || '0';
    digits = parts.join('.');
  } else {
    digits = digits.replace(/^0+/, '') || '0';
  }

  // Create Decimal with proper sign
  let result = new Decimal(digits);
  if (isNegative) {
    result = result.times(-1);
  }

  return result;
}

/**
 * Convert JavaScript Decimal to COBOL COMP-3 packed decimal representation
 *
 * @param {Decimal|number|string} value - Value to pack
 * @param {number} scale - Number of decimal places (from COBOL PIC clause)
 * @param {number} precision - Total number of digits (from COBOL PIC clause)
 * @returns {Uint8Array} COMP-3 packed decimal bytes
 *
 * @example
 * // Convert 1234567.89 to COMP-3 for PIC S9(7)V99 COMP-3
 * const packed = toComp3(new Decimal('1234567.89'), 2, 9);
 * // Returns: [0x01, 0x23, 0x45, 0x67, 0x89, 0x0C]
 */
export function toComp3(value, scale = 0, precision = 15) {
  const decimal = new Decimal(value || 0);
  const isNegative = decimal.isNegative();
  const absoluteValue = decimal.abs();

  // Scale the number and convert to integer representation
  const scaledValue = absoluteValue.times(new Decimal(10).pow(scale));
  const integerValue = scaledValue.toFixed(0, Decimal.ROUND_HALF_UP);

  // Pad to required precision
  let digits = integerValue.padStart(precision, '0');

  // Truncate if too long
  if (digits.length > precision) {
    digits = digits.slice(-precision);
  }

  // Calculate number of bytes needed (precision + 1) / 2, rounded up
  const byteCount = Math.ceil((precision + 1) / 2);
  const packedBytes = new Uint8Array(byteCount);

  // Pack digits into bytes (COMP-3 format)
  let digitIndex = 0;
  
  // Pack pairs of digits into all bytes except the last
  for (let i = 0; i < byteCount - 1; i++) {
    const leftDigit = parseInt(digits[digitIndex] || '0', 10);
    const rightDigit = parseInt(digits[digitIndex + 1] || '0', 10);
    packedBytes[i] = (leftDigit << 4) | rightDigit;
    digitIndex += 2;
  }

  // Pack the last byte with final digit and sign
  const finalDigit = parseInt(digits[digitIndex] || '0', 10);
  const sign = isNegative ? 0x0D : 0x0C; // D for negative, C for positive
  packedBytes[byteCount - 1] = (finalDigit << 4) | sign;

  return packedBytes;
}

/**
 * Format a monetary value for display with proper currency formatting
 *
 * @param {Decimal|number|string} amount - Monetary amount
 * @param {string} currencyCode - ISO currency code (default: 'USD')
 * @param {string} locale - Locale for formatting (default: 'en-US')
 * @param {boolean} showCents - Whether to show cents for whole dollar amounts
 * @returns {string} Formatted currency string
 *
 * @example
 * formatCurrency(new Decimal('1234567.89')) // "$1,234,567.89"
 * formatCurrency(new Decimal('1000.00'), 'USD', 'en-US', false) // "$1,000"
 */
export function formatCurrency(amount, currencyCode = 'USD', locale = 'en-US', showCents = true) {
  const decimal = new Decimal(amount || 0);
  const numericValue = parseFloat(decimal.toFixed(2));

  try {
    const formatter = new Intl.NumberFormat(locale, {
      style: 'currency',
      currency: currencyCode,
      minimumFractionDigits: showCents ? 2 : 0,
      maximumFractionDigits: 2,
    });

    return formatter.format(numericValue);
  } catch (error) {
    // Fallback formatting if Intl.NumberFormat fails
    const sign = decimal.isNegative() ? '-' : '';
    const absValue = decimal.abs();
    const formatted = absValue.toFixed(showCents ? 2 : 0);

    // Add thousand separators
    const parts = formatted.split('.');
    parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');

    return `${sign}$${parts.join('.')}`;
  }
}

/**
 * Pad string to specified length using COBOL-style padding rules
 *
 * @param {string} str - String to pad
 * @param {number} length - Target length
 * @param {string} padChar - Character to use for padding (default: space)
 * @param {string} direction - 'left', 'right', or 'both' (default: 'right')
 * @returns {string} Padded string
 *
 * @example
 * padString('HELLO', 10) // "HELLO     "
 * padString('123', 5, '0', 'left') // "00123"
 */
export function padString(str, length, padChar = ' ', direction = 'right') {
  if (!str) {str = '';}
  const inputStr = str.toString();

  if (inputStr.length >= length) {
    return inputStr.slice(0, length);
  }

  const padLength = length - inputStr.length;
  const padding = padChar.repeat(padLength);

  switch (direction.toLowerCase()) {
    case 'left':
      return padding + inputStr;
    case 'both': {
      const leftPad = Math.floor(padLength / 2);
      const rightPad = padLength - leftPad;
      return padChar.repeat(leftPad) + inputStr + padChar.repeat(rightPad);
    }
    case 'right':
    default:
      return inputStr + padding;
  }
}

/**
 * Trim string using COBOL-style trimming rules
 *
 * @param {string} str - String to trim
 * @param {string} direction - 'left', 'right', or 'both' (default: 'both')
 * @param {string} trimChar - Character to trim (default: space)
 * @returns {string} Trimmed string
 *
 * @example
 * trimString('  HELLO  ') // "HELLO"
 * trimString('000123000', 'left', '0') // "123000"
 */
export function trimString(str, direction = 'both', trimChar = ' ') {
  if (!str) {return '';}
  const inputStr = str.toString();

  const escapeRegExp = (char) => char.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const escapedChar = escapeRegExp(trimChar);

  switch (direction.toLowerCase()) {
    case 'left':
      return inputStr.replace(new RegExp(`^${escapedChar}+`), '');
    case 'right':
      return inputStr.replace(new RegExp(`${escapedChar}+$`), '');
    case 'both':
    default:
      return inputStr.replace(new RegExp(`^${escapedChar}+|${escapedChar}+$`, 'g'), '');
  }
}

/**
 * Parse COBOL numeric value based on picture clause specification
 *
 * @param {string} value - String value to parse
 * @param {string} picClause - COBOL picture clause (e.g., "S9(7)V99", "9(5)", "S9(3)V9(2)")
 * @returns {Decimal} Parsed decimal value with proper precision
 *
 * @example
 * parseCobolNumber('1234567', 'S9(7)V99') // Treats as 12345.67 (implied decimal)
 * parseCobolNumber('12345.67', '9(5)V99') // Parses as 12345.67
 * parseCobolNumber('-123', 'S9(3)') // Parses as -123
 */
export function parseCobolNumber(value, picClause) {
  if (!value && value !== 0) {
    return new Decimal(0);
  }

  const strValue = value.toString().trim();
  if (strValue === '' || strValue === '-') {
    return new Decimal(0);
  }

  // Parse picture clause to determine format
  const picInfo = parsePictureClause(picClause);

  // Handle signed values
  let numericValue = strValue;
  let isNegative = false;

  if (picInfo.signed) {
    if (numericValue.startsWith('-') || numericValue.startsWith('+')) {
      isNegative = numericValue.startsWith('-');
      numericValue = numericValue.slice(1);
    }
  }

  // Remove any existing decimal points for implied decimal processing
  const cleanValue = numericValue.replace(/[^\d]/g, '');

  if (cleanValue === '') {
    return new Decimal(0);
  }

  // Handle implied decimal point (COBOL picture clause takes precedence)
  let result;
  if (picInfo.impliedDecimal && picInfo.decimalPlaces > 0) {
    // Insert decimal point at correct position based on COBOL picture clause
    if (cleanValue.length > picInfo.decimalPlaces) {
      const integerPart = cleanValue.slice(0, -picInfo.decimalPlaces);
      const fractionalPart = cleanValue.slice(-picInfo.decimalPlaces);
      result = new Decimal(`${integerPart}.${fractionalPart}`);
    } else {
      // All digits are fractional
      result = new Decimal(`0.${cleanValue.padStart(picInfo.decimalPlaces, '0')}`);
    }
  } else if (picInfo.decimalPlaces > 0 && !picInfo.impliedDecimal && numericValue.includes('.')) {
    // Explicit decimal point (only when no implied decimal in picture clause)
    result = new Decimal(numericValue);
  } else {
    // No decimal places or integer only
    result = new Decimal(cleanValue);
  }

  return isNegative ? result.times(-1) : result;
}

/**
 * Format JavaScript number to COBOL numeric format based on picture clause
 *
 * @param {Decimal|number|string} value - Value to format
 * @param {string} picClause - COBOL picture clause
 * @param {boolean} impliedDecimal - Whether to show implied decimal (default: false)
 * @returns {string} Formatted string matching COBOL picture clause
 *
 * @example
 * formatCobolNumber(12345.67, 'S9(7)V99') // "001234567" (implied decimal)
 * formatCobolNumber(12345.67, 'S9(7)V99', true) // "+0012345.67"
 * formatCobolNumber(-123, 'S9(5)') // "-00123"
 */
export function formatCobolNumber(value, picClause, impliedDecimal = false) {
  const decimal = new Decimal(value || 0);
  const picInfo = parsePictureClause(picClause);

  // Scale the value to remove decimal places for COBOL storage format
  let scaledValue = decimal;
  if (picInfo.decimalPlaces > 0) {
    scaledValue = decimal.times(new Decimal(10).pow(picInfo.decimalPlaces));
  }

  // Round to integer and keep as string to preserve precision
  const integerValue = scaledValue.abs().toFixed(0, COBOL_ROUNDING_MODES.HALF_UP);

  // Format with leading zeros
  let formatted = integerValue.padStart(picInfo.totalDigits, '0');

  // Truncate if too long (take rightmost digits)
  if (formatted.length > picInfo.totalDigits) {
    formatted = formatted.slice(-picInfo.totalDigits);
  }

  // Add decimal point if showing explicit decimal
  if (impliedDecimal && picInfo.decimalPlaces > 0) {
    const integerPart = formatted.slice(0, -picInfo.decimalPlaces) || '0';
    const fractionalPart = formatted.slice(-picInfo.decimalPlaces);
    formatted = `${integerPart}.${fractionalPart}`;
  }

  // Add sign if signed field
  if (picInfo.signed) {
    const sign = decimal.isNegative() ? '-' : '+';
    formatted = sign + formatted;
  }

  return formatted;
}

/**
 * Convert value to display format for UI presentation
 *
 * @param {Decimal|number|string} value - Value to format for display
 * @param {string} picClause - COBOL picture clause
 * @param {object} options - Display formatting options
 * @param {boolean} options.showSign - Show explicit sign (default: false)
 * @param {boolean} options.showDecimal - Show decimal point (default: true)
 * @param {boolean} options.trimLeadingZeros - Remove leading zeros (default: true)
 * @returns {string} Formatted display string
 *
 * @example
 * displayFormat(12345.67, 'S9(7)V99') // "12345.67"
 * displayFormat(123, 'S9(5)', {showSign: true}) // "+123"
 * displayFormat(0.5, '9V9') // "0.5"
 */
export function displayFormat(value, picClause, options = {}) {
  const {
    showSign = false,
    showDecimal = true,
    trimLeadingZeros = true,
  } = options;

  const decimal = new Decimal(value || 0);
  const picInfo = parsePictureClause(picClause);

  // Format with appropriate decimal places
  let formatted = decimal.toFixed(picInfo.decimalPlaces, COBOL_ROUNDING_MODES.HALF_UP);

  // Remove leading zeros if requested
  if (trimLeadingZeros) {
    const parts = formatted.split('.');
    parts[0] = parts[0].replace(/^0+/, '') || '0';
    formatted = parts.join('.');
  }

  // Hide decimal point for whole numbers if showDecimal is false
  if (!showDecimal && formatted.endsWith('.00')) {
    formatted = formatted.slice(0, -3);
  }

  // Add explicit sign if requested and field is signed
  if (showSign && picInfo.signed && !decimal.isZero()) {
    if (!formatted.startsWith('-') && !formatted.startsWith('+')) {
      formatted = `+${formatted}`;
    }
  }

  return formatted;
}

/**
 * Format decimal value with precise control over scale and rounding
 *
 * @param {Decimal|number|string} value - Value to format
 * @param {number} scale - Number of decimal places
 * @param {number} roundingMode - Rounding mode (default: HALF_UP)
 * @param {boolean} stripTrailingZeros - Remove trailing zeros (default: false)
 * @returns {string} Formatted decimal string
 *
 * @example
 * formatDecimal(123.456789, 2) // "123.46"
 * formatDecimal(123.400, 3, COBOL_ROUNDING_MODES.HALF_UP, true) // "123.4"
 * formatDecimal(123.555, 2, COBOL_ROUNDING_MODES.DOWN) // "123.55"
 */
export function formatDecimal(
  value,
  scale = 2,
  roundingMode = COBOL_ROUNDING_MODES.HALF_UP,
  stripTrailingZeros = false,
) {
  const decimal = new Decimal(value || 0);

  // Apply rounding with specified mode
  let formatted = decimal.toFixed(scale, roundingMode);

  // Strip trailing zeros if requested
  if (stripTrailingZeros && formatted.includes('.')) {
    formatted = formatted.replace(/\.?0+$/, '');
  }

  return formatted;
}

/**
 * Parse COBOL picture clause to extract formatting information
 *
 * @private
 * @param {string} picClause - COBOL picture clause
 * @returns {object} Picture clause information
 */
function parsePictureClause(picClause) {
  if (!picClause) {
    return {
      signed: false,
      totalDigits: 0,
      decimalPlaces: 0,
      impliedDecimal: false,
    };
  }

  const clause = picClause.toUpperCase().trim();

  // Check if signed
  const signed = clause.startsWith('S');
  const workingClause = signed ? clause.slice(1) : clause;

  // Check for implied decimal (V)
  const impliedDecimal = workingClause.includes('V');
  const parts = workingClause.split('V');

  let integerDigits = 0;
  let decimalPlaces = 0;

  if (parts.length === 1) {
    // No decimal part
    integerDigits = parseDigitCount(parts[0]);
  } else {
    // Has decimal part
    integerDigits = parseDigitCount(parts[0]);
    decimalPlaces = parseDigitCount(parts[1]);
  }

  return {
    signed,
    totalDigits: integerDigits + decimalPlaces,
    integerDigits,
    decimalPlaces,
    impliedDecimal,
  };
}

/**
 * Parse digit count from picture clause segment
 *
 * @private
 * @param {string} segment - Picture clause segment
 * @returns {number} Number of digits
 */
function parseDigitCount(segment) {
  if (!segment) {return 0;}

  // Handle formats like "9(5)" or "99999"
  const match = segment.match(/9(\((\d+)\))?/);
  if (match) {
    if (match[2]) {
      // Format: 9(5)
      return parseInt(match[2], 10);
    } else {
      // Count the 9s
      return (segment.match(/9/g) || []).length;
    }
  }

  return 0;
}

// CommonJS exports for Jest testing compatibility
if (typeof module !== 'undefined' && module.exports) {
  module.exports = {
    COBOL_DECIMAL_SEPARATORS,
    COBOL_ROUNDING_MODES,
    fromComp3,
    toComp3,
    formatCurrency,
    padString,
    trimString,
    parseCobolNumber,
    formatCobolNumber,
    displayFormat,
    formatDecimal,
  };
}
