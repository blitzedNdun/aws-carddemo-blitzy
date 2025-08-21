package com.carddemo.util;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Utility class providing file formatting and validation operations
 * including COBOL-style field padding, numeric format conversions,
 * date formatting, and record layout validation.
 * 
 * This class replicates COBOL utility functions to maintain
 * 100% functional parity with the mainframe implementation.
 */
@Component
public class FileUtils {
    
    // Constants for COBOL-style formatting
    private static final char SPACE = ' ';
    private static final char ZERO = '0';
    private static final String COBOL_NUMERIC_PATTERN = "^[+-]?\\d*\\.?\\d*$";
    private static final Pattern NUMERIC_PATTERN = Pattern.compile(COBOL_NUMERIC_PATTERN);
    
    // Date format patterns used in COBOL
    private static final DateTimeFormatter CCYYMMDD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MMDDCCYY_FORMATTER = DateTimeFormatter.ofPattern("MMddyyyy");
    private static final DateTimeFormatter DDMMCCYY_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy");
    
    /**
     * Formats a field to fixed width using COBOL-style padding rules.
     * Replicates COBOL PIC X(n) behavior with proper truncation and padding.
     * 
     * @param value The value to format
     * @param width The target field width
     * @param padChar The character to use for padding
     * @param leftJustified Whether to left-justify (true) or right-justify (false)
     * @return Formatted fixed-width field
     */
    public static String formatFixedWidthField(String value, int width, char padChar, boolean leftJustified) {
        if (value == null) {
            value = "";
        }
        
        // Handle truncation if value is longer than width
        if (value.length() > width) {
            return value.substring(0, width);
        }
        
        // Calculate padding needed
        int paddingNeeded = width - value.length();
        if (paddingNeeded <= 0) {
            return value;
        }
        
        // Create padding string
        StringBuilder padding = new StringBuilder();
        for (int i = 0; i < paddingNeeded; i++) {
            padding.append(padChar);
        }
        
        // Apply padding based on justification
        if (leftJustified) {
            return value + padding.toString();
        } else {
            return padding.toString() + value;
        }
    }
    
    /**
     * Formats decimal amounts with proper precision matching COBOL COMP-3 behavior.
     * Ensures monetary values maintain exact precision for financial calculations.
     * 
     * @param amount The decimal amount to format
     * @param totalDigits Total number of digits (including decimals)
     * @param decimalPlaces Number of decimal places
     * @param signDisplay Whether to display sign explicitly
     * @return Formatted decimal amount string
     */
    public static String formatDecimalAmount(BigDecimal amount, int totalDigits, int decimalPlaces, boolean signDisplay) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        
        // Set scale to match COBOL COMP-3 precision
        amount = amount.setScale(decimalPlaces, RoundingMode.HALF_UP);
        
        // Get absolute value for formatting, remember original sign
        boolean isNegative = amount.compareTo(BigDecimal.ZERO) < 0;
        BigDecimal absAmount = amount.abs();
        
        // Convert to string without scientific notation
        String amountStr = absAmount.toPlainString();
        
        // Calculate integer portion digits needed
        int integerDigits = totalDigits - decimalPlaces;
        int currentIntegerDigits = amountStr.indexOf('.') == -1 ? 
            amountStr.length() : amountStr.indexOf('.');
        
        // Format the result
        StringBuilder result = new StringBuilder();
        
        // Handle sign
        if (signDisplay) {
            result.append(isNegative ? '-' : '+');
        } else if (isNegative) {
            result.append('-');
        }
        
        // Add leading zeros for integer part
        int paddingNeeded = integerDigits - currentIntegerDigits;
        for (int i = 0; i < paddingNeeded; i++) {
            result.append(ZERO);
        }
        
        // Add the formatted amount
        result.append(amountStr);
        
        return result.toString();
    }
    
    /**
     * Formats date fields according to various COBOL date formats.
     * Supports CCYYMMDD, MMDDCCYY, and DDMMCCYY formats.
     * 
     * @param date The date to format
     * @param format The target format ("CCYYMMDD", "MMDDCCYY", "DDMMCCYY")
     * @return Formatted date string
     */
    public static String formatDateField(LocalDate date, String format) {
        if (date == null) {
            return formatFixedWidthField("", 8, ZERO, false);
        }
        
        try {
            switch (format.toUpperCase()) {
                case "CCYYMMDD":
                    return date.format(CCYYMMDD_FORMATTER);
                case "MMDDCCYY":
                    return date.format(MMDDCCYY_FORMATTER);
                case "DDMMCCYY":
                    return date.format(DDMMCCYY_FORMATTER);
                default:
                    // Default to CCYYMMDD format
                    return date.format(CCYYMMDD_FORMATTER);
            }
        } catch (Exception e) {
            // Return zeros if formatting fails
            return formatFixedWidthField("", 8, ZERO, false);
        }
    }
    
    /**
     * Validates record structure for layout compliance.
     * Checks that record fields conform to expected COBOL copybook layouts.
     * 
     * @param record The record data to validate
     * @param expectedLength The expected total record length
     * @param fieldDefinitions Array of field definitions [start_pos, length, type]
     * @return true if record structure is valid, false otherwise
     */
    public static boolean validateRecordStructure(String record, int expectedLength, int[][] fieldDefinitions) {
        if (record == null) {
            return false;
        }
        
        // Check overall record length
        if (record.length() != expectedLength) {
            return false;
        }
        
        // Validate each field definition
        for (int[] fieldDef : fieldDefinitions) {
            if (fieldDef.length < 3) {
                continue; // Skip invalid field definitions
            }
            
            int startPos = fieldDef[0];
            int length = fieldDef[1];
            int type = fieldDef[2]; // 0=character, 1=numeric, 2=alphanumeric
            
            // Check bounds
            if (startPos < 0 || startPos + length > record.length()) {
                return false;
            }
            
            // Extract field value
            String fieldValue = record.substring(startPos, startPos + length);
            
            // Validate based on type
            switch (type) {
                case 1: // Numeric field
                    if (!isValidCobolNumeric(fieldValue)) {
                        return false;
                    }
                    break;
                case 0: // Character field
                case 2: // Alphanumeric field
                default:
                    // Character fields are always valid
                    break;
            }
        }
        
        return true;
    }
    
    /**
     * Pads field to the left with specified character.
     * Replicates COBOL right-justification behavior.
     * 
     * @param value The value to pad
     * @param length The target length
     * @param padChar The padding character
     * @return Left-padded string
     */
    public static String padFieldLeft(String value, int length, char padChar) {
        return formatFixedWidthField(value, length, padChar, false);
    }
    
    /**
     * Pads field to the right with specified character.
     * Replicates COBOL left-justification behavior.
     * 
     * @param value The value to pad
     * @param length The target length
     * @param padChar The padding character
     * @return Right-padded string
     */
    public static String padFieldRight(String value, int length, char padChar) {
        return formatFixedWidthField(value, length, padChar, true);
    }
    
    /**
     * Calculates field checksum for data integrity validation.
     * Implements simple checksum algorithm used in COBOL file processing.
     * 
     * @param data The data to calculate checksum for
     * @return Calculated checksum value
     */
    public static int calculateFieldChecksum(String data) {
        if (data == null || data.isEmpty()) {
            return 0;
        }
        
        int checksum = 0;
        for (int i = 0; i < data.length(); i++) {
            checksum += (int) data.charAt(i);
            // Prevent overflow by using modulo
            checksum = checksum % 65536;
        }
        
        return checksum;
    }
    
    /**
     * Parses COBOL numeric formats and converts to Java BigDecimal.
     * Handles COBOL PIC 9(n)V9(m) and COMP-3 numeric representations.
     * 
     * @param cobolNumeric The COBOL numeric string
     * @param impliedDecimalPlaces Number of implied decimal places
     * @return Parsed BigDecimal value
     */
    public static BigDecimal parseCobolNumeric(String cobolNumeric, int impliedDecimalPlaces) {
        if (cobolNumeric == null || cobolNumeric.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        String cleaned = cobolNumeric.trim();
        
        // Handle COBOL spaces and low-values as zero
        if (cleaned.isEmpty() || cleaned.equals("000000000000000") || 
            cleaned.chars().allMatch(c -> c == ' ' || c == 0)) {
            return BigDecimal.ZERO;
        }
        
        // Remove leading zeros but keep at least one digit
        cleaned = cleaned.replaceFirst("^0+(?!$)", "");
        if (cleaned.isEmpty()) {
            cleaned = "0";
        }
        
        // Validate numeric format
        if (!NUMERIC_PATTERN.matcher(cleaned).matches()) {
            // If not valid numeric, try to extract digits only
            cleaned = cleaned.replaceAll("[^\\d-+.]", "");
            if (cleaned.isEmpty()) {
                return BigDecimal.ZERO;
            }
        }
        
        try {
            BigDecimal result = new BigDecimal(cleaned);
            
            // Apply implied decimal places
            if (impliedDecimalPlaces > 0) {
                result = result.movePointLeft(impliedDecimalPlaces);
            }
            
            return result;
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Helper method to validate if a string represents a valid COBOL numeric field.
     * 
     * @param value The value to validate
     * @return true if valid numeric, false otherwise
     */
    private static boolean isValidCobolNumeric(String value) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Empty/spaces are valid in COBOL numeric fields
        }
        
        String trimmed = value.trim();
        
        // Check for all spaces or zeros (valid in COBOL)
        if (trimmed.isEmpty() || trimmed.matches("^[0\\s]+$")) {
            return true;
        }
        
        // Check for digits only (most common COBOL numeric format)
        if (trimmed.matches("^\\d+$")) {
            return true;
        }
        
        // Check for valid numeric pattern with optional decimal and sign
        return NUMERIC_PATTERN.matcher(trimmed).matches();
    }
}