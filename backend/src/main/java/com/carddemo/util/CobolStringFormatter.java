/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.util;

/**
 * COBOL string formatting utility class that handles conversion between COBOL PIC clause 
 * formatted strings and Java strings.
 * 
 * This class provides comprehensive string formatting capabilities essential for the CardDemo
 * system migration from COBOL/CICS to Java/Spring Boot. It maintains exact COBOL behavior
 * for string operations including alphanumeric padding (PIC X), numeric formatting (PIC 9),
 * FILLER field handling, fixed-length field formatting, and COBOL JUSTIFIED RIGHT clause behavior.
 * 
 * Key Features:
 * - PIC X clause formatting with space padding
 * - PIC 9 clause formatting with zero padding  
 * - JUSTIFIED RIGHT clause handling
 * - Fixed-length field boundary maintenance
 * - FILLER placeholder processing
 * - COBOL MOVE semantics replication
 * - Trailing space preservation for display compatibility
 * 
 * This implementation directly addresses the requirements for maintaining exact string
 * formatting compatibility with COBOL MOVE semantics and display patterns.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
public final class CobolStringFormatter {

    /**
     * Default space character for padding operations.
     */
    private static final char SPACE_CHAR = ' ';
    
    /**
     * Default zero character for numeric padding.
     */
    private static final char ZERO_CHAR = '0';
    
    /**
     * Maximum field length for COBOL string operations.
     */
    private static final int MAX_FIELD_LENGTH = 32767;

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private CobolStringFormatter() {
        throw new IllegalStateException("Utility class - cannot be instantiated");
    }

    /**
     * Formats alphanumeric field according to COBOL PIC X clause specifications.
     * 
     * This method replicates COBOL PIC X field formatting by padding or truncating
     * the input string to the specified length with space characters. Maintains
     * exact COBOL MOVE semantics for alphanumeric data.
     * 
     * @param value     the string value to format
     * @param length    the target field length
     * @return formatted string with proper space padding or truncation
     * @throws IllegalArgumentException if length is invalid
     */
    public static String formatAlphanumericField(String value, int length) {
        if (length < 0 || length > MAX_FIELD_LENGTH) {
            throw new IllegalArgumentException("Invalid field length: " + length);
        }
        
        if (value == null) {
            value = "";
        }
        
        if (value.length() == length) {
            return value;
        } else if (value.length() < length) {
            // Pad with spaces on the right (COBOL default)
            return padRight(value, length, SPACE_CHAR);
        } else {
            // Truncate to specified length
            return truncateString(value, length);
        }
    }

    /**
     * Formats numeric field according to COBOL PIC 9 clause specifications.
     * 
     * This method replicates COBOL PIC 9 field formatting by padding or truncating
     * numeric strings with zero characters on the left. Maintains exact COBOL
     * numeric display behavior.
     * 
     * @param value     the numeric string value to format
     * @param length    the target field length
     * @return formatted string with proper zero padding or truncation
     * @throws IllegalArgumentException if length is invalid
     */
    public static String formatNumericField(String value, int length) {
        if (length < 0 || length > MAX_FIELD_LENGTH) {
            throw new IllegalArgumentException("Invalid field length: " + length);
        }
        
        if (value == null || value.trim().isEmpty()) {
            // Default to zeros for empty numeric fields
            return padLeft("", length, ZERO_CHAR);
        }
        
        // Remove any non-numeric characters except digits
        String cleanValue = value.replaceAll("[^0-9]", "");
        
        if (cleanValue.length() == length) {
            return cleanValue;
        } else if (cleanValue.length() < length) {
            // Pad with zeros on the left
            return padLeft(cleanValue, length, ZERO_CHAR);
        } else {
            // Truncate from the left (preserve rightmost digits)
            return cleanValue.substring(cleanValue.length() - length);
        }
    }

    /**
     * Handles COBOL JUSTIFIED RIGHT clause formatting.
     * 
     * This method implements COBOL JUSTIFIED RIGHT behavior by right-aligning
     * the value within the specified field length and padding with spaces on the left.
     * 
     * @param value     the string value to justify
     * @param length    the target field length
     * @return right-justified string with left space padding
     * @throws IllegalArgumentException if length is invalid
     */
    public static String handleJustifiedRight(String value, int length) {
        if (length < 0 || length > MAX_FIELD_LENGTH) {
            throw new IllegalArgumentException("Invalid field length: " + length);
        }
        
        if (value == null) {
            value = "";
        }
        
        if (value.length() >= length) {
            // Truncate from the left if too long
            return value.substring(value.length() - length);
        } else {
            // Pad with spaces on the left for right justification
            return padLeft(value, length, SPACE_CHAR);
        }
    }

    /**
     * Formats field to maintain COBOL record layout field boundaries.
     * 
     * This method ensures that string fields maintain their exact boundaries
     * as defined in COBOL record layouts, preserving fixed-width record structure.
     * 
     * @param value     the string value to format
     * @param length    the fixed field length
     * @param padChar   the character to use for padding
     * @return formatted string maintaining exact field boundaries
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static String formatFixedLength(String value, int length, char padChar) {
        if (length < 0 || length > MAX_FIELD_LENGTH) {
            throw new IllegalArgumentException("Invalid field length: " + length);
        }
        
        if (value == null) {
            value = "";
        }
        
        if (value.length() == length) {
            return value;
        } else if (value.length() < length) {
            // Pad to required length
            return padRight(value, length, padChar);
        } else {
            // Truncate to required length
            return value.substring(0, length);
        }
    }

    /**
     * Handles FILLER field placeholders in COBOL record processing.
     * 
     * This method processes FILLER fields by generating appropriate placeholder
     * content or empty space based on the field definition and processing requirements.
     * 
     * @param fieldLength   the length of the FILLER field
     * @param fillChar      the character to use for filling
     * @return FILLER field content as a string
     * @throws IllegalArgumentException if field length is invalid
     */
    public static String handleFillerFields(int fieldLength, char fillChar) {
        if (fieldLength < 0 || fieldLength > MAX_FIELD_LENGTH) {
            throw new IllegalArgumentException("Invalid FILLER field length: " + fieldLength);
        }
        
        if (fieldLength == 0) {
            return "";
        }
        
        StringBuilder filler = new StringBuilder(fieldLength);
        for (int i = 0; i < fieldLength; i++) {
            filler.append(fillChar);
        }
        
        return filler.toString();
    }

    /**
     * Pads string with specified character on the right side.
     * 
     * @param value     the string to pad
     * @param length    the target length
     * @param padChar   the padding character
     * @return right-padded string
     */
    public static String padString(String value, int length, char padChar) {
        return padRight(value, length, padChar);
    }

    /**
     * Truncates string to specified length.
     * 
     * @param value     the string to truncate
     * @param length    the maximum length
     * @return truncated string
     * @throws IllegalArgumentException if length is negative
     */
    public static String truncateString(String value, int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }
        
        if (value == null) {
            return "";
        }
        
        if (value.length() <= length) {
            return value;
        }
        
        return value.substring(0, length);
    }

    /**
     * Formats edited numeric fields with decimal points and commas.
     * 
     * This method handles COBOL edited numeric fields that include formatting
     * characters like decimal points, commas, and currency symbols.
     * 
     * @param value         the numeric value to format
     * @param editPattern   the COBOL edit pattern
     * @return formatted numeric string with edit characters
     */
    public static String formatEditedNumeric(String value, String editPattern) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        
        if (editPattern == null || editPattern.trim().isEmpty()) {
            return value;
        }
        
        // Basic implementation for common edit patterns
        if (editPattern.contains("Z") && editPattern.contains(".")) {
            // Zero suppression with decimal point
            return formatZeroSuppressed(value, editPattern);
        } else if (editPattern.contains(",")) {
            // Thousands separator formatting
            return formatWithCommas(value);
        }
        
        return value;
    }

    /**
     * Preserves trailing spaces to maintain COBOL display semantics.
     * 
     * This method ensures that trailing spaces are preserved in string fields
     * to maintain exact compatibility with COBOL display behavior.
     * 
     * @param value         the string value
     * @param fieldLength   the original field length
     * @return string with preserved trailing spaces
     */
    public static String preserveTrailingSpaces(String value, int fieldLength) {
        if (value == null) {
            return formatFixedLength("", fieldLength, SPACE_CHAR);
        }
        
        if (value.length() >= fieldLength) {
            return value.substring(0, fieldLength);
        }
        
        // Preserve original trailing spaces if any, then pad to field length
        return formatFixedLength(value, fieldLength, SPACE_CHAR);
    }

    /**
     * Converts Java string to COBOL-compatible string format.
     * 
     * @param value     the Java string to convert
     * @param length    the target COBOL field length
     * @return COBOL-compatible string
     */
    public static String convertToCobolString(String value, int length) {
        return formatFixedLength(value, length, SPACE_CHAR);
    }

    /**
     * Converts COBOL string to Java string format.
     * 
     * @param value     the COBOL string to convert
     * @return Java-compatible string (typically trimmed)
     */
    public static String convertToJavaString(String value) {
        return value != null ? value.trim() : "";
    }

    /**
     * Parses COBOL PIC clause to extract formatting information.
     * 
     * @param picClause     the COBOL PIC clause
     * @return map containing PIC clause components
     */
    public static java.util.Map<String, Object> parsePicClause(String picClause) {
        java.util.Map<String, Object> picInfo = new java.util.HashMap<>();
        
        if (picClause == null || picClause.trim().isEmpty()) {
            picInfo.put("type", "unknown");
            picInfo.put("length", 0);
            return picInfo;
        }
        
        String upperClause = picClause.toUpperCase().trim();
        
        if (upperClause.contains("PIC X")) {
            picInfo.put("type", "alphanumeric");
            picInfo.put("length", extractLength(upperClause, "X"));
        } else if (upperClause.contains("PIC 9")) {
            picInfo.put("type", "numeric");
            picInfo.put("length", extractLength(upperClause, "9"));
        } else if (upperClause.contains("PIC S9")) {
            picInfo.put("type", "signed_numeric");
            picInfo.put("length", extractLength(upperClause, "S9"));
        } else {
            picInfo.put("type", "unknown");
            picInfo.put("length", 0);
        }
        
        return picInfo;
    }

    // Private helper methods

    /**
     * Pads string on the right with specified character.
     */
    private static String padRight(String value, int length, char padChar) {
        if (value == null) {
            value = "";
        }
        
        if (value.length() >= length) {
            return value.substring(0, length);
        }
        
        StringBuilder padded = new StringBuilder(value);
        while (padded.length() < length) {
            padded.append(padChar);
        }
        
        return padded.toString();
    }

    /**
     * Pads string on the left with specified character.
     */
    private static String padLeft(String value, int length, char padChar) {
        if (value == null) {
            value = "";
        }
        
        if (value.length() >= length) {
            return value.substring(value.length() - length);
        }
        
        StringBuilder padded = new StringBuilder();
        int paddingNeeded = length - value.length();
        
        for (int i = 0; i < paddingNeeded; i++) {
            padded.append(padChar);
        }
        
        padded.append(value);
        return padded.toString();
    }

    /**
     * Extracts length from PIC clause.
     */
    private static int extractLength(String picClause, String picType) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            picType + "\\((\\d+)\\)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(picClause);
        
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        
        // Count repeated characters
        long count = picClause.chars()
            .filter(ch -> ch == picType.charAt(picType.length() - 1))
            .count();
        
        return (int) count;
    }

    /**
     * Formats numeric value with zero suppression.
     */
    private static String formatZeroSuppressed(String value, String pattern) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        
        // Basic zero suppression - replace leading zeros with spaces
        String trimmed = value.trim();
        StringBuilder result = new StringBuilder();
        boolean foundNonZero = false;
        
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (ch != '0' || foundNonZero || i == trimmed.length() - 1) {
                foundNonZero = true;
                result.append(ch);
            } else {
                result.append(' ');
            }
        }
        
        return result.toString();
    }

    /**
     * Formats numeric value with thousands separators.
     */
    private static String formatWithCommas(String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }
        
        try {
            long number = Long.parseLong(value.trim());
            return String.format("%,d", number);
        } catch (NumberFormatException e) {
            return value;
        }
    }
}