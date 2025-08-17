/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.util;

import org.apache.commons.lang3.StringUtils;
import java.nio.charset.StandardCharsets;

/**
 * String manipulation utility class providing COBOL-equivalent string operations.
 * 
 * This utility class implements fixed-length field padding/trimming, COBOL MOVE operations
 * with space filling, string justification (left/right), and character set conversions.
 * All methods ensure string operations maintain exact COBOL behavior for data format
 * compatibility during the mainframe-to-cloud migration.
 * 
 * Key Features:
 * - Fixed-length field operations matching COBOL PIC clauses
 * - COBOL MOVE semantics with automatic space filling
 * - Safe string operations with null handling
 * - Character set conversions for EBCDIC/ASCII compatibility
 * - Field boundary validation using system constants
 * 
 * Thread Safety: All methods are static and thread-safe.
 * 
 * @see org.apache.commons.lang3.StringUtils
 * @see Constants
 */
public final class StringUtil {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private StringUtil() {
        throw new IllegalStateException("Utility class - cannot be instantiated");
    }

    /**
     * Pads a string to the right with spaces to reach the specified length.
     * Equivalent to COBOL PIC X(n) field assignment with automatic space padding.
     * 
     * @param input the input string to pad (may be null)
     * @param length the target length for padding
     * @return the right-padded string, truncated if longer than length
     * @throws IllegalArgumentException if length is negative
     */
    public static String padRight(String input, int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }
        
        String safeInput = defaultString(input);
        
        if (safeInput.length() > length) {
            return safeInput.substring(0, length);
        }
        
        return StringUtils.rightPad(safeInput, length);
    }

    /**
     * Pads a string to the left with spaces to reach the specified length.
     * Useful for numeric field formatting and right-justified displays.
     * 
     * @param input the input string to pad (may be null)
     * @param length the target length for padding
     * @return the left-padded string, truncated if longer than length
     * @throws IllegalArgumentException if length is negative
     */
    public static String padLeft(String input, int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }
        
        String safeInput = defaultString(input);
        
        if (safeInput.length() > length) {
            return safeInput.substring(safeInput.length() - length);
        }
        
        return StringUtils.leftPad(safeInput, length);
    }

    /**
     * Trims the input string and then pads it to the specified length.
     * Replicates COBOL MOVE operation behavior with automatic space filling.
     * 
     * @param input the input string to trim and pad (may be null)
     * @param length the target length after trimming and padding
     * @return the trimmed and right-padded string
     * @throws IllegalArgumentException if length is negative
     */
    public static String trimAndPad(String input, int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }
        
        String trimmed = safeTrim(input);
        return padRight(trimmed, length);
    }

    /**
     * Left-justifies text within a field of specified length.
     * Equivalent to left-aligned COBOL field display.
     * 
     * @param input the input string to justify (may be null)
     * @param length the field length for justification
     * @return the left-justified string with trailing spaces
     * @throws IllegalArgumentException if length is negative
     */
    public static String justifyLeft(String input, int length) {
        return padRight(safeTrim(input), length);
    }

    /**
     * Right-justifies text within a field of specified length.
     * Equivalent to right-aligned COBOL field display.
     * 
     * @param input the input string to justify (may be null)
     * @param length the field length for justification
     * @return the right-justified string with leading spaces
     * @throws IllegalArgumentException if length is negative
     */
    public static String justifyRight(String input, int length) {
        return padLeft(safeTrim(input), length);
    }

    /**
     * Centers text within a field of specified length.
     * Distributes padding evenly on both sides.
     * 
     * @param input the input string to center (may be null)
     * @param length the field length for centering
     * @return the centered string with balanced padding
     * @throws IllegalArgumentException if length is negative
     */
    public static String center(String input, int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }
        
        String safeInput = safeTrim(input);
        
        if (safeInput.length() >= length) {
            return safeInput.substring(0, length);
        }
        
        return StringUtils.center(safeInput, length);
    }

    /**
     * Implements COBOL MOVE operation equivalent functionality.
     * Moves source string to target field with proper length handling and space filling.
     * 
     * @param source the source string to move (may be null)
     * @param targetLength the length of the target field
     * @return the moved string formatted to target length
     * @throws IllegalArgumentException if targetLength is negative
     */
    public static String moveString(String source, int targetLength) {
        if (targetLength < 0) {
            throw new IllegalArgumentException("Target length cannot be negative: " + targetLength);
        }
        
        String safeSource = defaultString(source);
        
        if (safeSource.length() > targetLength) {
            // Truncate from the right (COBOL behavior)
            return safeSource.substring(0, targetLength);
        } else {
            // Pad with spaces to the right
            return StringUtils.rightPad(safeSource, targetLength);
        }
    }

    /**
     * Extracts a substring with boundary checking and safe handling.
     * Prevents IndexOutOfBoundsException and handles null inputs gracefully.
     * 
     * @param input the input string (may be null)
     * @param startIndex the starting index (0-based)
     * @param endIndex the ending index (exclusive)
     * @return the substring or empty string if boundaries are invalid
     * @throws IllegalArgumentException if startIndex is negative or greater than endIndex
     */
    public static String substringWithBounds(String input, int startIndex, int endIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("Start index cannot be negative: " + startIndex);
        }
        if (startIndex > endIndex) {
            throw new IllegalArgumentException("Start index cannot be greater than end index: " + startIndex + " > " + endIndex);
        }
        
        if (StringUtils.isEmpty(input)) {
            return "";
        }
        
        int actualStart = Math.min(startIndex, input.length());
        int actualEnd = Math.min(endIndex, input.length());
        
        if (actualStart >= actualEnd) {
            return "";
        }
        
        return input.substring(actualStart, actualEnd);
    }

    /**
     * Converts EBCDIC encoded bytes to ASCII string representation.
     * Used for processing mainframe data during migration.
     * 
     * Note: This is a simplified conversion. For production use with actual EBCDIC data,
     * consider using IBM's EBCDIC character set implementations.
     * 
     * @param ebcdicBytes the EBCDIC encoded byte array
     * @return the ASCII string representation
     * @throws IllegalArgumentException if ebcdicBytes is null
     */
    public static String convertEbcdicToAscii(byte[] ebcdicBytes) {
        if (ebcdicBytes == null) {
            throw new IllegalArgumentException("EBCDIC bytes cannot be null");
        }
        
        // For demonstration purposes, using ISO-8859-1 as a simplified conversion
        // In production, use appropriate EBCDIC codepage (e.g., IBM-1047)
        return new String(ebcdicBytes, StandardCharsets.ISO_8859_1);
    }

    /**
     * Converts ASCII string to EBCDIC encoded bytes.
     * Used for generating mainframe-compatible data during transition.
     * 
     * Note: This is a simplified conversion. For production use with actual EBCDIC data,
     * consider using IBM's EBCDIC character set implementations.
     * 
     * @param asciiString the ASCII string to convert
     * @return the EBCDIC encoded byte array
     * @throws IllegalArgumentException if asciiString is null
     */
    public static byte[] convertAsciiToEbcdic(String asciiString) {
        if (asciiString == null) {
            throw new IllegalArgumentException("ASCII string cannot be null");
        }
        
        // For demonstration purposes, using ISO-8859-1 as a simplified conversion
        // In production, use appropriate EBCDIC codepage (e.g., IBM-1047)
        return asciiString.getBytes(StandardCharsets.ISO_8859_1);
    }

    /**
     * Formats a string to a fixed length with proper truncation or padding.
     * Combines trimming, length validation, and padding in one operation.
     * 
     * @param input the input string to format (may be null)
     * @param fixedLength the required fixed length
     * @return the formatted string at exactly the specified length
     * @throws IllegalArgumentException if fixedLength is negative
     */
    public static String formatFixedLength(String input, int fixedLength) {
        if (fixedLength < 0) {
            throw new IllegalArgumentException("Fixed length cannot be negative: " + fixedLength);
        }
        
        if (fixedLength == 0) {
            return "";
        }
        
        String safeInput = defaultString(input);
        
        if (safeInput.length() > fixedLength) {
            return safeInput.substring(0, fixedLength);
        } else {
            return StringUtils.rightPad(safeInput, fixedLength);
        }
    }

    /**
     * Checks if a string is empty or contains only space characters.
     * Useful for validating COBOL field contents and detecting "blank" fields.
     * 
     * @param input the string to check (may be null)
     * @return true if the string is null, empty, or contains only spaces
     */
    public static boolean isEmptyOrSpaces(String input) {
        return StringUtils.isBlank(input);
    }

    /**
     * Removes trailing spaces from a string while preserving leading spaces.
     * Replicates COBOL trailing space removal behavior.
     * 
     * @param input the input string (may be null)
     * @return the string with trailing spaces removed, or empty string if input is null
     */
    public static String trimTrailing(String input) {
        if (input == null) {
            return "";
        }
        
        int end = input.length();
        while (end > 0 && input.charAt(end - 1) == ' ') {
            end--;
        }
        
        return input.substring(0, end);
    }

    /**
     * Removes leading spaces from a string while preserving trailing spaces.
     * Useful for processing left-padded numeric fields.
     * 
     * @param input the input string (may be null)
     * @return the string with leading spaces removed, or empty string if input is null
     */
    public static String trimLeading(String input) {
        if (input == null) {
            return "";
        }
        
        int start = 0;
        while (start < input.length() && input.charAt(start) == ' ') {
            start++;
        }
        
        return input.substring(start);
    }

    /**
     * Replaces the content of a field with spaces of the specified length.
     * Equivalent to COBOL MOVE SPACES TO field-name operation.
     * 
     * @param length the number of spaces to generate
     * @return a string containing the specified number of spaces
     * @throws IllegalArgumentException if length is negative
     */
    public static String replaceWithSpaces(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }
        
        if (length == 0) {
            return "";
        }
        
        return StringUtils.repeat(" ", length);
    }

    /**
     * Validates that a string meets the length requirements for a specific field type.
     * Uses system constants to enforce COBOL field length constraints.
     * 
     * @param input the string to validate (may be null)
     * @param fieldType the type of field (used to lookup length constraints)
     * @return true if the string length is valid for the field type
     * @throws IllegalArgumentException if fieldType is not recognized
     */
    public static boolean validateFieldLength(String input, String fieldType) {
        if (fieldType == null) {
            throw new IllegalArgumentException("Field type cannot be null");
        }
        
        String safeInput = defaultString(input);
        int maxLength = getMaxLengthForFieldType(fieldType);
        
        return safeInput.length() <= maxLength;
    }

    /**
     * Performs null-safe string trimming.
     * Returns empty string for null input, trimmed string otherwise.
     * 
     * @param input the string to trim (may be null)
     * @return the trimmed string or empty string if input is null
     */
    public static String safeTrim(String input) {
        if (input == null) {
            return "";
        }
        return input.trim();
    }

    /**
     * Performs null-safe substring extraction.
     * Handles boundary conditions and null inputs gracefully.
     * 
     * @param input the input string (may be null)
     * @param startIndex the starting index (0-based)
     * @param length the number of characters to extract
     * @return the substring or empty string if parameters are invalid
     * @throws IllegalArgumentException if startIndex or length is negative
     */
    public static String safeSubstring(String input, int startIndex, int length) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("Start index cannot be negative: " + startIndex);
        }
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }
        
        if (input == null || length == 0) {
            return "";
        }
        
        if (startIndex >= input.length()) {
            return "";
        }
        
        int endIndex = Math.min(startIndex + length, input.length());
        return input.substring(startIndex, endIndex);
    }

    /**
     * Implements exact COBOL MOVE operation behavior.
     * Handles alphanumeric data movement with proper truncation and padding.
     * 
     * @param source the source string to move (may be null)
     * @param targetLength the target field length
     * @return the string formatted according to COBOL MOVE semantics
     * @throws IllegalArgumentException if targetLength is negative
     */
    public static String cobolMove(String source, int targetLength) {
        if (targetLength < 0) {
            throw new IllegalArgumentException("Target length cannot be negative: " + targetLength);
        }
        
        if (targetLength == 0) {
            return "";
        }
        
        String safeSource = defaultString(source);
        
        // COBOL MOVE behavior: left-align and pad/truncate as needed
        if (safeSource.length() >= targetLength) {
            // Truncate from the right
            return safeSource.substring(0, targetLength);
        } else {
            // Pad with spaces on the right
            return StringUtils.rightPad(safeSource, targetLength);
        }
    }

    /**
     * Implements COBOL MOVE SPACES operation.
     * Fills a field with spaces according to COBOL semantics.
     * 
     * @param targetLength the target field length to fill with spaces
     * @return a string of spaces with the specified length
     * @throws IllegalArgumentException if targetLength is negative
     */
    public static String cobolMoveSpaces(int targetLength) {
        if (targetLength < 0) {
            throw new IllegalArgumentException("Target length cannot be negative: " + targetLength);
        }
        
        if (targetLength == 0) {
            return "";
        }
        
        return StringUtils.repeat(" ", targetLength);
    }

    // Helper Methods

    /**
     * Returns a default string if the input is null, otherwise returns the input.
     * Provides null-safe string operations throughout the utility class.
     * 
     * @param input the input string (may be null)
     * @return the input string or empty string if null
     */
    private static String defaultString(String input) {
        return StringUtils.defaultString(input);
    }

    /**
     * Gets the maximum length for a specific field type based on system constants.
     * Maps field type names to their corresponding length constants.
     * 
     * @param fieldType the field type identifier
     * @return the maximum length for the field type
     * @throws IllegalArgumentException if the field type is not recognized
     */
    private static int getMaxLengthForFieldType(String fieldType) {
        if (fieldType == null) {
            throw new IllegalArgumentException("Field type cannot be null");
        }
        
        // Map field types to their corresponding constants
        switch (fieldType.toUpperCase()) {
            case "CARD_NUMBER":
                return Constants.CARD_NUMBER_LENGTH;
            case "TRANSACTION_ID":
                return Constants.TRANSACTION_ID_LENGTH;
            case "ACCOUNT_NUMBER":
                return Constants.ACCOUNT_NUMBER_LENGTH;
            case "USER_ID":
                return Constants.USER_ID_LENGTH;
            case "USER_NAME":
                return Constants.USER_NAME_LENGTH;
            case "USER_TYPE":
                return Constants.USER_TYPE_LENGTH;
            case "TYPE_CODE":
                return Constants.TYPE_CODE_LENGTH;
            case "DESCRIPTION":
                return Constants.DESCRIPTION_LENGTH;
            case "DATE_FORMAT":
                return Constants.DATE_FORMAT_LENGTH;
            default:
                throw new IllegalArgumentException("Unknown field type: " + fieldType);
        }
    }

    /**
     * Validates input parameters for string manipulation operations.
     * Provides centralized parameter validation with consistent error messages.
     * 
     * @param input the input string (may be null)
     * @param length the length parameter to validate
     * @param parameterName the name of the parameter for error messages
     * @throws IllegalArgumentException if length is negative
     */
    private static void validateParameters(String input, int length, String parameterName) {
        if (length < 0) {
            throw new IllegalArgumentException(parameterName + " cannot be negative: " + length);
        }
    }

    /**
     * Checks if a string represents a valid numeric value.
     * Useful for validating COBOL numeric field contents.
     * 
     * @param input the string to check (may be null)
     * @return true if the string represents a valid number
     */
    public static boolean isNumeric(String input) {
        if (StringUtils.isEmpty(input)) {
            return false;
        }
        
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        
        try {
            Double.parseDouble(trimmed);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Normalizes a string for comparison by trimming and converting to uppercase.
     * Useful for case-insensitive field comparisons in business logic.
     * 
     * @param input the string to normalize (may be null)
     * @return the normalized string or empty string if input is null
     */
    public static String normalizeForComparison(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().toUpperCase();
    }
}