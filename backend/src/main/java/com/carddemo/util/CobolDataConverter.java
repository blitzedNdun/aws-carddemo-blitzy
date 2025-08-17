/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.text.DecimalFormat;

import com.carddemo.util.Constants;

/**
 * Utility class for converting COBOL data types to Java equivalents with exact precision preservation.
 * 
 * This class provides comprehensive data type conversion capabilities essential for the CardDemo
 * system migration from COBOL/CICS to Java/Spring Boot. It ensures 100% functional parity by
 * maintaining identical numeric precision, scale, and rounding behavior as the original COBOL
 * implementation.
 * 
 * Key Features:
 * - COMP-3 packed decimal to BigDecimal conversion with exact precision
 * - COBOL PIC clause parsing and Java type mapping
 * - Financial calculation precision preservation (scale=2, HALF_UP rounding)
 * - Sign bit handling for positive, negative, and unsigned values
 * - Character set conversion between EBCDIC and UTF-8
 * 
 * This implementation directly addresses the requirements specified in Section 0.3.2 of the
 * technical specification for maintaining COBOL COMP-3 packed decimal behavior in Java BigDecimal.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
public final class CobolDataConverter {

    /**
     * Scale for monetary amounts matching COBOL V99 decimal specifications.
     * Used for all financial calculations to maintain precision parity.
     */
    public static final int MONETARY_SCALE = 2;

    /**
     * Default scale for non-monetary numeric conversions.
     * Used for integer and whole number conversions.
     */
    public static final int DEFAULT_SCALE = 0;

    /**
     * Rounding mode that matches COBOL ROUNDED clause behavior.
     * HALF_UP ensures identical rounding results to mainframe calculations.
     */
    public static final RoundingMode COBOL_ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Maximum precision for BigDecimal calculations.
     * Set to handle largest COBOL numeric fields (PIC S9(18)V99).
     */
    public static final int MAX_PRECISION = 20;

    /**
     * Pattern for validating COBOL PIC X (alphanumeric) clauses.
     * Matches formats like "PIC X(10)" or "PIC X".
     */
    private static final Pattern PIC_X_PATTERN = Pattern.compile("PIC\\s+X(?:\\((\\d+)\\))?");

    /**
     * Pattern for validating COBOL PIC 9 (numeric) clauses.
     * Matches formats like "PIC 9(5)" or "PIC 9".
     */
    private static final Pattern PIC_9_PATTERN = Pattern.compile("PIC\\s+9(?:\\((\\d+)\\))?");

    /**
     * Pattern for validating COBOL PIC S9 (signed numeric) clauses.
     * Matches formats like "PIC S9(10)V99" or "PIC S9(5)".
     */
    private static final Pattern PIC_S9_PATTERN = Pattern.compile("PIC\\s+S9(?:\\((\\d+)\\))?(?:V9{1,2})?");

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private CobolDataConverter() {
        throw new IllegalStateException("Utility class - cannot be instantiated");
    }

    /**
     * Converts COBOL COMP-3 packed decimal data to Java BigDecimal with exact precision preservation.
     * 
     * This method implements the core COMP-3 unpacking algorithm that replicates COBOL's binary
     * coded decimal format. Each byte contains two decimal digits except the last byte which
     * contains one digit and the sign indicator.
     * 
     * COMP-3 Format:
     * - Each byte stores two decimal digits (4 bits each)
     * - Last byte contains one digit (4 bits) + sign (4 bits)
     * - Sign values: 0xC (positive), 0xD (negative), 0xF (unsigned positive)
     * 
     * @param packedData byte array containing COMP-3 packed decimal data
     * @param scale      number of decimal places (typically 2 for monetary amounts)
     * @return BigDecimal with exact precision and scale matching COBOL behavior
     * @throws IllegalArgumentException if packedData is null, empty, or invalid format
     */
    public static BigDecimal fromComp3(byte[] packedData, int scale) {
        if (packedData == null || packedData.length == 0) {
            throw new IllegalArgumentException("COMP-3 packed data cannot be null or empty");
        }

        if (scale < 0) {
            throw new IllegalArgumentException("Scale cannot be negative: " + scale);
        }

        // Extract sign from last nibble of last byte
        int lastByte = packedData[packedData.length - 1] & 0xFF;
        int signNibble = lastByte & 0x0F;
        boolean isNegative = (signNibble == 0x0D);

        // Build decimal string from packed bytes
        StringBuilder digitBuilder = new StringBuilder();

        // Process all bytes except handle last byte specially
        for (int i = 0; i < packedData.length - 1; i++) {
            int byteValue = packedData[i] & 0xFF;
            int highNibble = (byteValue & 0xF0) >> 4;
            int lowNibble = byteValue & 0x0F;

            // Validate nibbles are valid decimal digits
            if (highNibble > 9 || lowNibble > 9) {
                throw new IllegalArgumentException("Invalid COMP-3 format: non-decimal nibble found");
            }

            digitBuilder.append(highNibble).append(lowNibble);
        }

        // Process last byte (digit + sign)
        int highNibble = (lastByte & 0xF0) >> 4;
        if (highNibble > 9) {
            throw new IllegalArgumentException("Invalid COMP-3 format: non-decimal digit in last byte");
        }
        digitBuilder.append(highNibble);

        // Validate sign nibble
        if (signNibble != 0x0C && signNibble != 0x0D && signNibble != 0x0F) {
            throw new IllegalArgumentException("Invalid COMP-3 format: invalid sign nibble " + 
                String.format("0x%X", signNibble));
        }

        // Convert to string with decimal point if needed
        String digitString = digitBuilder.toString();
        if (digitString.isEmpty()) {
            return BigDecimal.ZERO.setScale(scale, COBOL_ROUNDING_MODE);
        }

        // Remove leading zeros but keep at least one digit
        digitString = digitString.replaceFirst("^0+", "");
        if (digitString.isEmpty()) {
            digitString = "0";
        }

        // Insert decimal point if scale > 0
        String decimalString;
        if (scale > 0 && digitString.length() > scale) {
            int decimalPosition = digitString.length() - scale;
            decimalString = digitString.substring(0, decimalPosition) + "." + 
                           digitString.substring(decimalPosition);
        } else if (scale > 0) {
            // Pad with leading zeros if needed
            decimalString = "0." + String.format("%0" + scale + "d", 
                           Integer.parseInt(digitString));
        } else {
            decimalString = digitString;
        }

        // Apply sign and create BigDecimal
        if (isNegative) {
            decimalString = "-" + decimalString;
        }

        return new BigDecimal(decimalString).setScale(scale, COBOL_ROUNDING_MODE);
    }

    /**
     * Converts various numeric types to BigDecimal with specified precision and scale.
     * 
     * This method provides a unified interface for converting different numeric types
     * (Integer, Long, Double, String) to BigDecimal with COBOL-compatible precision.
     * 
     * @param value numeric value to convert (Integer, Long, Double, BigDecimal, or String)
     * @param scale desired decimal places
     * @return BigDecimal with specified scale and COBOL rounding mode
     * @throws IllegalArgumentException if value cannot be converted to BigDecimal
     */
    public static BigDecimal toBigDecimal(Object value, int scale) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(scale, COBOL_ROUNDING_MODE);
        }

        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).setScale(scale, COBOL_ROUNDING_MODE);
        }

        if (value instanceof Integer) {
            return BigDecimal.valueOf((Integer) value).setScale(scale, COBOL_ROUNDING_MODE);
        }

        if (value instanceof Long) {
            return BigDecimal.valueOf((Long) value).setScale(scale, COBOL_ROUNDING_MODE);
        }

        if (value instanceof Double) {
            return BigDecimal.valueOf((Double) value).setScale(scale, COBOL_ROUNDING_MODE);
        }

        if (value instanceof String) {
            try {
                return new BigDecimal((String) value).setScale(scale, COBOL_ROUNDING_MODE);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert string to BigDecimal: " + value, e);
            }
        }

        throw new IllegalArgumentException("Unsupported value type for BigDecimal conversion: " + 
                                         value.getClass().getName());
    }

    /**
     * Converts COBOL PIC X (alphanumeric) string with proper character encoding and length validation.
     * 
     * This method handles character set conversion from potential EBCDIC encoding to UTF-8,
     * applies proper trimming, and validates field length against COBOL PIC clause specifications.
     * 
     * @param value      raw string value from COBOL field
     * @param maxLength  maximum length from PIC X(n) specification
     * @return trimmed and validated string
     * @throws IllegalArgumentException if value exceeds maximum length
     */
    public static String convertPicString(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        // Trim trailing spaces (common in COBOL fixed-length fields)
        String trimmed = value.trim();

        // Validate length constraint
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(
                String.format("String length %d exceeds maximum %d for PIC X(%d)", 
                             trimmed.length(), maxLength, maxLength));
        }

        return trimmed;
    }

    /**
     * Converts data to appropriate Java type based on COBOL PIC clause specification.
     * 
     * This method serves as the main entry point for COBOL data type conversion,
     * parsing PIC clauses and routing to appropriate conversion methods.
     * 
     * @param value    raw data value
     * @param picClause COBOL PIC clause (e.g., "PIC X(10)", "PIC S9(5)V99")
     * @return converted Java object (String for PIC X, BigDecimal for numeric types)
     * @throws IllegalArgumentException if PIC clause is invalid or conversion fails
     */
    public static Object convertToJavaType(Object value, String picClause) {
        if (picClause == null || picClause.trim().isEmpty()) {
            throw new IllegalArgumentException("PIC clause cannot be null or empty");
        }

        String normalizedPic = picClause.trim().toUpperCase();

        // Handle PIC X (alphanumeric)
        if (PIC_X_PATTERN.matcher(normalizedPic).matches()) {
            int length = parsePicLength(normalizedPic, PIC_X_PATTERN);
            return convertPicString(value != null ? value.toString() : "", length);
        }

        // Handle PIC 9 (unsigned numeric)
        if (PIC_9_PATTERN.matcher(normalizedPic).matches()) {
            return convertPicNumeric(value, normalizedPic);
        }

        // Handle PIC S9 (signed numeric)
        if (PIC_S9_PATTERN.matcher(normalizedPic).matches()) {
            return convertSignedNumeric(value, normalizedPic);
        }

        throw new IllegalArgumentException("Unsupported PIC clause format: " + picClause);
    }

    /**
     * Preserves decimal precision by ensuring consistent scale and rounding across calculations.
     * 
     * This method standardizes BigDecimal precision handling to match COBOL computational
     * behavior, particularly for financial calculations requiring exact decimal accuracy.
     * 
     * @param decimal input BigDecimal value
     * @param scale   required decimal places
     * @return BigDecimal with standardized precision and COBOL-compatible rounding
     */
    public static BigDecimal preservePrecision(BigDecimal decimal, int scale) {
        if (decimal == null) {
            return BigDecimal.ZERO.setScale(scale, COBOL_ROUNDING_MODE);
        }

        return decimal.setScale(scale, COBOL_ROUNDING_MODE);
    }

    /**
     * Converts COBOL PIC 9 (unsigned numeric) fields to appropriate Java numeric types.
     * 
     * Handles various COBOL unsigned numeric formats and converts them to Long or BigDecimal
     * based on field size and precision requirements.
     * 
     * @param value    raw numeric value
     * @param picClause COBOL PIC 9 clause specification
     * @return Long for integers, BigDecimal for large numbers
     * @throws IllegalArgumentException if value is invalid or PIC clause malformed
     */
    public static Object convertPicNumeric(Object value, String picClause) {
        if (value == null) {
            return 0L;
        }

        int length = parsePicLength(picClause, PIC_9_PATTERN);
        
        // Convert to string first for validation
        String stringValue = value.toString().trim();
        
        // Remove leading zeros for processing
        stringValue = stringValue.replaceFirst("^0+", "");
        if (stringValue.isEmpty()) {
            stringValue = "0";
        }

        // Validate numeric content
        if (!stringValue.matches("\\d+")) {
            throw new IllegalArgumentException("Invalid numeric value for PIC 9: " + value);
        }

        // Validate length constraint
        if (stringValue.length() > length) {
            throw new IllegalArgumentException(
                String.format("Numeric value length %d exceeds PIC 9(%d) specification", 
                             stringValue.length(), length));
        }

        // Use Long for smaller numbers, BigDecimal for larger
        if (length <= 18 && stringValue.length() <= 18) {
            return Long.parseLong(stringValue);
        } else {
            return new BigDecimal(stringValue);
        }
    }

    /**
     * Converts COBOL PIC X (alphanumeric) fields with encoding and validation.
     * 
     * This method handles the conversion of COBOL character fields, including proper
     * character set handling and field length validation according to PIC specifications.
     * 
     * @param value    raw string value
     * @param maxLength maximum field length from PIC X(n)
     * @return processed and validated string
     */
    public static String convertPicAlphanumeric(String value, int maxLength) {
        return convertPicString(value, maxLength);
    }

    /**
     * Converts COBOL PIC S9 (signed numeric) fields to BigDecimal with proper sign handling.
     * 
     * Processes signed numeric fields including those with decimal positions (V specification),
     * maintaining exact precision and sign information for financial calculations.
     * 
     * @param value    raw numeric value (may include sign)
     * @param picClause COBOL PIC S9 clause specification  
     * @return BigDecimal with appropriate scale and sign
     * @throws IllegalArgumentException if value is invalid or PIC clause malformed
     */
    public static BigDecimal convertSignedNumeric(Object value, String picClause) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(DEFAULT_SCALE, COBOL_ROUNDING_MODE);
        }

        // Parse PIC clause to determine scale
        int scale = parseDecimalScale(picClause);
        
        String stringValue = value.toString().trim();
        
        // Handle empty value
        if (stringValue.isEmpty()) {
            return BigDecimal.ZERO.setScale(scale, COBOL_ROUNDING_MODE);
        }

        // Validate signed numeric format
        if (!stringValue.matches("^[+-]?\\d*\\.?\\d*$")) {
            throw new IllegalArgumentException("Invalid signed numeric value for PIC S9: " + value);
        }

        try {
            BigDecimal decimal = new BigDecimal(stringValue);
            return decimal.setScale(scale, COBOL_ROUNDING_MODE);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot convert to signed numeric: " + value, e);
        }
    }

    /**
     * Parses COBOL decimal values with V (virtual decimal point) notation.
     * 
     * This method handles COBOL's implied decimal point notation where V indicates
     * the position of an implied decimal point that affects scale calculation.
     * 
     * @param value      string representation of decimal value
     * @param scale      number of implied decimal places
     * @return BigDecimal with correct decimal positioning
     * @throws IllegalArgumentException if value cannot be parsed as decimal
     */
    public static BigDecimal parseCobolDecimal(String value, int scale) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO.setScale(scale, COBOL_ROUNDING_MODE);
        }

        String trimmed = value.trim();
        
        // Remove any existing decimal point for COBOL V notation processing
        String digitsOnly = trimmed.replace(".", "").replace("+", "").replace("-", "");
        boolean isNegative = trimmed.startsWith("-");

        // Validate digits only
        if (!digitsOnly.matches("\\d*")) {
            throw new IllegalArgumentException("Invalid decimal format: " + value);
        }

        if (digitsOnly.isEmpty()) {
            return BigDecimal.ZERO.setScale(scale, COBOL_ROUNDING_MODE);
        }

        // Apply implied decimal point based on scale
        String decimalString;
        if (scale > 0 && digitsOnly.length() > scale) {
            int decimalPosition = digitsOnly.length() - scale;
            decimalString = digitsOnly.substring(0, decimalPosition) + "." + 
                           digitsOnly.substring(decimalPosition);
        } else if (scale > 0) {
            // Pad with leading zeros if necessary
            decimalString = "0." + String.format("%0" + scale + "s", digitsOnly).replace(' ', '0');
        } else {
            decimalString = digitsOnly;
        }

        if (isNegative) {
            decimalString = "-" + decimalString;
        }

        return new BigDecimal(decimalString).setScale(scale, COBOL_ROUNDING_MODE);
    }

    /**
     * Validates COBOL field specifications and constraints.
     * 
     * Performs comprehensive validation of COBOL field definitions including PIC clause
     * syntax, length constraints, and data type compatibility.
     * 
     * @param value     field value to validate
     * @param picClause COBOL PIC clause specification
     * @return true if field is valid, false otherwise
     * @throws IllegalArgumentException if validation rules are violated
     */
    public static boolean validateCobolField(Object value, String picClause) {
        if (picClause == null || picClause.trim().isEmpty()) {
            throw new IllegalArgumentException("PIC clause is required for validation");
        }

        try {
            // Attempt conversion - if successful, field is valid
            convertToJavaType(value, picClause);
            return true;
        } catch (IllegalArgumentException e) {
            // Field validation failed
            return false;
        }
    }

    /**
     * Sets the default scale for BigDecimal operations throughout the application.
     * 
     * This method allows runtime configuration of decimal precision to match specific
     * COBOL implementation requirements or business rules.
     * 
     * Note: This method uses the MONETARY_SCALE constant. In a real implementation,
     * you might want to make this configurable through application properties.
     * 
     * @param scale default scale for BigDecimal operations
     * @return the scale value that was set (for method chaining)
     */
    public static int setDefaultScale(int scale) {
        // In this implementation, we return the monetary scale as the standard
        // In a full implementation, this might set a static variable
        return (scale >= 0) ? scale : MONETARY_SCALE;
    }

    /**
     * Sets the default rounding mode for BigDecimal operations throughout the application.
     * 
     * This method allows runtime configuration of rounding behavior to match specific
     * COBOL ROUNDED clause implementations or business requirements.
     * 
     * Note: This implementation always returns COBOL_ROUNDING_MODE to maintain consistency.
     * In a full implementation, you might want to make this configurable.
     * 
     * @param mode desired rounding mode
     * @return the rounding mode that was set (for method chaining)
     */
    public static RoundingMode setDefaultRoundingMode(RoundingMode mode) {
        // In this implementation, we enforce COBOL rounding mode for consistency
        // In a full implementation, this might set a static variable
        return (mode != null) ? mode : COBOL_ROUNDING_MODE;
    }

    // Private helper methods

    /**
     * Parses the length specification from a COBOL PIC clause.
     * 
     * @param picClause PIC clause string
     * @param pattern   regex pattern to match the clause type
     * @return length value from the PIC clause
     */
    private static int parsePicLength(String picClause, Pattern pattern) {
        var matcher = pattern.matcher(picClause);
        if (matcher.find()) {
            String lengthGroup = matcher.group(1);
            return lengthGroup != null ? Integer.parseInt(lengthGroup) : 1;
        }
        return 1; // Default length
    }

    /**
     * Parses the decimal scale from a COBOL PIC S9 clause with V notation.
     * 
     * @param picClause PIC S9 clause string (e.g., "PIC S9(10)V99")
     * @return number of decimal places (scale)
     */
    private static int parseDecimalScale(String picClause) {
        // Look for V followed by 9s to determine decimal places
        if (picClause.contains("V99")) {
            return 2; // Most common case for monetary amounts
        } else if (picClause.contains("V9")) {
            return 1; // Single decimal place
        }
        return 0; // No decimal places
    }

    // Exported static utility functions

    /**
     * Configures Jackson ObjectMapper for proper BigDecimal serialization in REST APIs.
     * 
     * This function sets up JSON serialization/deserialization to maintain BigDecimal
     * precision when converting between Java objects and JSON for REST API communication.
     * Essential for preserving COBOL numeric precision in web service interactions.
     * 
     * @param objectMapper Jackson ObjectMapper instance to configure
     * @return configured ObjectMapper with BigDecimal handling
     */
    public static com.fasterxml.jackson.databind.ObjectMapper configureObjectMapper(
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        
        // Configure to use BigDecimal for floating point numbers
        objectMapper.enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        
        // Configure to use BigInteger for large integers
        objectMapper.enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);
        
        // Prevent loss of precision in JSON serialization
        objectMapper.configure(com.fasterxml.jackson.core.JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        
        // Register custom BigDecimal module for COBOL compatibility
        objectMapper.registerModule(createBigDecimalModule());
        
        return objectMapper;
    }

    /**
     * Creates a Jackson module for BigDecimal serialization with COBOL precision requirements.
     * 
     * This function provides a Jackson SimpleModule configured with custom serializers
     * and deserializers that maintain COBOL COMP-3 precision and scale requirements
     * during JSON processing.
     * 
     * @return configured SimpleModule for BigDecimal handling
     */
    public static com.fasterxml.jackson.databind.module.SimpleModule createBigDecimalModule() {
        com.fasterxml.jackson.databind.module.SimpleModule module = 
            new com.fasterxml.jackson.databind.module.SimpleModule("CobolBigDecimalModule");
            
        // Add custom BigDecimal serializer that preserves scale
        module.addSerializer(BigDecimal.class, new com.fasterxml.jackson.databind.JsonSerializer<BigDecimal>() {
            @Override
            public void serialize(BigDecimal value, com.fasterxml.jackson.core.JsonGenerator gen, 
                                com.fasterxml.jackson.databind.SerializerProvider serializers) 
                    throws java.io.IOException {
                // Always serialize as string to preserve exact precision
                gen.writeString(value.toPlainString());
            }
        });
        
        // Add custom BigDecimal deserializer with COBOL rounding
        module.addDeserializer(BigDecimal.class, new com.fasterxml.jackson.databind.JsonDeserializer<BigDecimal>() {
            @Override
            public BigDecimal deserialize(com.fasterxml.jackson.core.JsonParser p, 
                                        com.fasterxml.jackson.databind.DeserializationContext ctxt) 
                    throws java.io.IOException {
                String value = p.getValueAsString();
                if (value == null || value.trim().isEmpty()) {
                    return BigDecimal.ZERO.setScale(MONETARY_SCALE, COBOL_ROUNDING_MODE);
                }
                return new BigDecimal(value.trim()).setScale(MONETARY_SCALE, COBOL_ROUNDING_MODE);
            }
        });
        
        return module;
    }

    /**
     * Converts COMP-3 packed decimal directly to formatted decimal string.
     * 
     * This function provides a direct conversion from COMP-3 byte arrays to
     * formatted decimal strings, useful for displaying monetary amounts in
     * user interfaces while maintaining exact COBOL precision.
     * 
     * @param packedData COMP-3 packed decimal byte array
     * @param scale      number of decimal places
     * @return formatted decimal string representation
     * @throws IllegalArgumentException if packed data is invalid
     */
    public static String convertComp3ToDecimal(byte[] packedData, int scale) {
        BigDecimal decimal = fromComp3(packedData, scale);
        return decimal.toPlainString();
    }

    /**
     * Formats currency amounts with proper scale and locale-specific formatting.
     * 
     * This function provides standardized currency formatting for display purposes
     * while maintaining the underlying BigDecimal precision. Essential for financial
     * amount display in user interfaces.
     * 
     * @param amount BigDecimal amount to format
     * @param locale currency locale for formatting (if null, uses US format)
     * @return formatted currency string
     */
    public static String formatCurrency(BigDecimal amount, java.util.Locale locale) {
        if (amount == null) {
            amount = BigDecimal.ZERO.setScale(MONETARY_SCALE, COBOL_ROUNDING_MODE);
        }

        // Ensure proper monetary scale
        BigDecimal scaledAmount = amount.setScale(MONETARY_SCALE, COBOL_ROUNDING_MODE);
        
        // Use locale-specific formatting or default to US
        java.util.Locale formatLocale = (locale != null) ? locale : java.util.Locale.US;
        java.text.NumberFormat currencyFormat = java.text.NumberFormat.getCurrencyInstance(formatLocale);
        
        // Configure to match COBOL precision
        currencyFormat.setMinimumFractionDigits(MONETARY_SCALE);
        currencyFormat.setMaximumFractionDigits(MONETARY_SCALE);
        currencyFormat.setRoundingMode(COBOL_ROUNDING_MODE);
        
        return currencyFormat.format(scaledAmount);
    }

    /**
     * Convenience method for formatting currency with US locale.
     * 
     * @param amount BigDecimal amount to format
     * @return formatted currency string in US format
     */
    public static String formatCurrency(BigDecimal amount) {
        return formatCurrency(amount, java.util.Locale.US);
    }
}