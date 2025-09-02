/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.commons.lang3.StringUtils;

import com.carddemo.util.Constants;
import com.carddemo.util.StringUtil;
import com.carddemo.util.DateConversionUtil;
import com.carddemo.util.CobolDataConverter;

/**
 * Formatting utility class for converting between display formats and internal representations.
 * 
 * This class handles all formatting operations required for maintaining exact compatibility
 * with COBOL display patterns and screen formats. It provides comprehensive formatting 
 * capabilities for currency amounts, account numbers, card number masking, date/time 
 * formatting, and numeric display formatting that matches COBOL PIC clause specifications.
 * 
 * Key Features:
 * - Currency formatting matching COBOL PIC S9(9)V99 display patterns
 * - Card number masking for security compliance (shows only last 4 digits)
 * - Account number formatting with proper zero-padding
 * - Date/time formatting for various COBOL timestamp formats
 * - Numeric display formatting with sign indicators and decimal places
 * - Fixed-width field formatting matching COBOL fixed-length records
 * 
 * All formatted output ensures byte-for-byte compatibility with original COBOL system 
 * displays for seamless screen and report compatibility during the migration.
 * 
 * This implementation directly addresses the requirements specified in Section 0.5.1
 * for preserving exact display format compatibility with COBOL screens and maintaining
 * identical formatting patterns for user interface consistency.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
public final class FormatUtil {

    // COBOL display format patterns matching original BMS mapsets
    private static final String CURRENCY_PATTERN_POSITIVE = "#,##0.00";
    private static final String CURRENCY_PATTERN_NEGATIVE = "-#,##0.00";
    private static final String CURRENCY_PATTERN_SIGNED = "+#,##0.00;-#,##0.00";
    private static final String ZERO_SUPPRESSED_PATTERN = "###,###,##0.00";
    private static final String REPORT_AMOUNT_PATTERN = "ZZZ,ZZZ,ZZ9.99";
    
    // Date format patterns matching COBOL CSDAT01Y copybook
    private static final DateTimeFormatter CCYYMMDD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MMDDYY_FORMATTER = DateTimeFormatter.ofPattern("MMddyy");
    private static final DateTimeFormatter YYYYMMDDHHMMSS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter DISPLAY_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
    
    // Card masking pattern - shows only last 4 digits for security
    private static final String CARD_MASK_PATTERN = "****-****-****-";
    private static final int CARD_VISIBLE_DIGITS = 4;
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private FormatUtil() {
        throw new IllegalStateException("Utility class - cannot be instantiated");
    }

    /**
     * Formats currency amounts matching COBOL display patterns with proper decimal places.
     * 
     * This method implements COBOL PIC S9(9)V99 display formatting with standard currency
     * symbols and thousand separators. Handles positive amounts with standard formatting.
     * 
     * @param amount BigDecimal amount to format
     * @return formatted currency string (e.g., "$1,234.56")
     * @throws IllegalArgumentException if amount is null
     */
    public static String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        
        BigDecimal scaledAmount = CobolDataConverter.preservePrecision(amount, CobolDataConverter.MONETARY_SCALE);
        DecimalFormat formatter = new DecimalFormat(CURRENCY_PATTERN_POSITIVE);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        
        return "$" + formatter.format(scaledAmount);
    }

    /**
     * Formats currency amounts with explicit sign indicators matching COBOL signed displays.
     * 
     * This method implements COBOL PIC +ZZZ,ZZZ,ZZZ.ZZ and -ZZZ,ZZZ,ZZZ.ZZ display patterns
     * with proper sign placement and zero suppression for financial reports.
     * 
     * @param amount BigDecimal amount to format with sign
     * @return formatted currency string with sign (e.g., "+1,234.56" or "-1,234.56")
     * @throws IllegalArgumentException if amount is null
     */
    public static String formatCurrencyWithSign(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        
        BigDecimal scaledAmount = CobolDataConverter.preservePrecision(amount, CobolDataConverter.MONETARY_SCALE);
        DecimalFormat formatter = new DecimalFormat(CURRENCY_PATTERN_SIGNED);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        
        return formatter.format(scaledAmount);
    }

    /**
     * Formats dates in display format matching COBOL screen patterns.
     * 
     * Converts LocalDateTime to MM/dd/yyyy format for user interface display,
     * matching the original COBOL BMS screen date formatting.
     * 
     * @param date LocalDateTime to format
     * @return formatted date string (e.g., "12/31/2023")
     */
    public static String formatDate(LocalDateTime date) {
        if (date == null) {
            return "";
        }
        
        return date.format(DISPLAY_DATE_FORMATTER);
    }

    /**
     * Formats date and time for display matching COBOL timestamp patterns.
     * 
     * Converts LocalDateTime to standard display format for user interfaces
     * and logging, maintaining consistency with COBOL datetime displays.
     * 
     * @param dateTime LocalDateTime to format
     * @return formatted datetime string (e.g., "2023-12-31 14:30:45")
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        
        return dateTime.format(DISPLAY_DATETIME_FORMATTER);
    }

    /**
     * Formats timestamps with microsecond precision matching COBOL WS-TIMESTAMP.
     * 
     * Converts LocalDateTime to full timestamp format including microseconds
     * for audit trails and transaction logging that matches COBOL precision.
     * 
     * @param timestamp LocalDateTime to format
     * @return formatted timestamp string (e.g., "2023-12-31 14:30:45.123456")
     */
    public static String formatTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "";
        }
        
        return timestamp.format(TIMESTAMP_FORMATTER);
    }

    /**
     * Formats numeric amounts for display with proper decimal places and separators.
     * 
     * This method provides general numeric formatting for non-currency amounts
     * while maintaining COBOL numeric display patterns and precision.
     * 
     * @param number BigDecimal number to format
     * @param scale decimal places to display
     * @return formatted number string
     */
    public static String formatNumber(BigDecimal number, int scale) {
        if (number == null) {
            return "0" + (scale > 0 ? "." + "0".repeat(scale) : "");
        }
        
        BigDecimal scaledNumber = CobolDataConverter.preservePrecision(number, scale);
        String pattern = "#,##0" + (scale > 0 ? "." + "0".repeat(scale) : "");
        DecimalFormat formatter = new DecimalFormat(pattern);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        
        return formatter.format(scaledNumber);
    }

    /**
     * Formats account numbers with proper zero-padding matching COBOL PIC 9(11).
     * 
     * This method ensures account numbers are displayed with leading zeros
     * to maintain the fixed 11-digit format specified in CVACT01Y copybook.
     * 
     * @param accountNumber String or numeric account number
     * @return formatted account number (e.g., "00001234567")
     * @throws IllegalArgumentException if account number is invalid
     */
    public static String formatAccountNumber(Object accountNumber) {
        if (accountNumber == null) {
            return StringUtils.leftPad("0", Constants.ACCOUNT_NUMBER_LENGTH, '0');
        }
        
        String accountStr = accountNumber.toString().trim();
        
        // Remove any non-numeric characters
        accountStr = accountStr.replaceAll("[^0-9]", "");
        
        // Validate length
        if (accountStr.length() > Constants.ACCOUNT_NUMBER_LENGTH) {
            throw new IllegalArgumentException("Account number exceeds maximum length: " + accountStr);
        }
        
        // Pad with leading zeros to match COBOL PIC 9(11)
        return StringUtils.leftPad(accountStr, Constants.ACCOUNT_NUMBER_LENGTH, '0');
    }

    /**
     * Formats transaction amounts for display matching COBOL transaction records.
     * 
     * Formats amounts from TRAN-AMT fields with proper sign handling and decimal
     * places matching the CVTRA05Y copybook PIC S9(09)V99 specification.
     * 
     * @param amount transaction amount to format
     * @return formatted transaction amount
     */
    public static String formatTransactionAmount(BigDecimal amount) {
        if (amount == null) {
            return formatCurrency(BigDecimal.ZERO);
        }
        
        return formatCurrencyWithSign(amount);
    }

    /**
     * Formats transaction IDs with proper padding matching COBOL TRAN-ID fields.
     * 
     * Ensures transaction IDs conform to the 16-character format specified
     * in CVTRA05Y copybook PIC X(16) specification.
     * 
     * @param transactionId transaction ID to format
     * @return formatted transaction ID
     */
    public static String formatTransactionId(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            return StringUtil.formatFixedLength("", Constants.TRANSACTION_ID_LENGTH);
        }
        
        return StringUtil.formatFixedLength(transactionId.trim(), Constants.TRANSACTION_ID_LENGTH);
    }

    /**
     * Formats dates in CCYYMMDD format matching COBOL date fields.
     * 
     * Converts LocalDateTime to COBOL-compatible CCYYMMDD string format
     * for database storage and file interfaces.
     * 
     * @param date LocalDateTime to format
     * @return CCYYMMDD formatted string (e.g., "20231231")
     */
    public static String formatCCYYMMDD(LocalDateTime date) {
        if (date == null) {
            return "";
        }
        
        return DateConversionUtil.formatToCobol(date.toLocalDate());
    }

    /**
     * Formats dates in MMDDYY format for legacy interface compatibility.
     * 
     * Converts LocalDateTime to 6-character MMDDYY format for interfaces
     * that require compressed date representation.
     * 
     * @param date LocalDateTime to format
     * @return MMDDYY formatted string (e.g., "123123" for Dec 31, 2023)
     */
    public static String formatMMDDYY(LocalDateTime date) {
        if (date == null) {
            return "";
        }
        
        return date.format(MMDDYY_FORMATTER);
    }

    /**
     * Formats timestamps in YYYYMMDDHHMMSS format for file interfaces.
     * 
     * Converts LocalDateTime to 14-character timestamp format for
     * batch processing and file naming conventions.
     * 
     * @param dateTime LocalDateTime to format
     * @return YYYYMMDDHHMMSS formatted string (e.g., "20231231143045")
     */
    public static String formatYYYYMMDDHHMMSS(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        
        return dateTime.format(YYYYMMDDHHMMSS_FORMATTER);
    }

    /**
     * Pads string to specified width with left alignment.
     * 
     * Uses StringUtils.leftPad() for consistent string padding
     * operations throughout the application.
     * 
     * @param text string to pad
     * @param width target width
     * @param padChar character to use for padding
     * @return left-padded string
     */
    public static String padLeft(String text, int width, char padChar) {
        return StringUtils.leftPad(text, width, padChar);
    }

    /**
     * Pads string to specified width with right alignment.
     * 
     * Uses StringUtils.rightPad() for consistent string padding
     * operations throughout the application.
     * 
     * @param text string to pad
     * @param width target width
     * @param padChar character to use for padding
     * @return right-padded string
     */
    public static String padRight(String text, int width, char padChar) {
        return StringUtils.rightPad(text, width, padChar);
    }

    /**
     * Truncates text to specified maximum length with ellipsis indicator.
     * 
     * Safely truncates text for display purposes while indicating
     * truncation occurred for user awareness.
     * 
     * @param text string to truncate
     * @param maxLength maximum allowed length
     * @return truncated string with ellipsis if needed
     */
    public static String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        
        if (maxLength <= 3) {
            return text.substring(0, maxLength);
        }
        
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Masks card numbers showing only last 4 digits for security.
     * 
     * Implements PCI DSS compliance by masking all but the last 4 digits
     * of card numbers for display and logging purposes.
     * 
     * @param cardNumber full card number to mask
     * @return masked card number (e.g., "****-****-****-1234")
     * @throws IllegalArgumentException if card number is invalid
     */
    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            return "";
        }
        
        String cleanCard = cardNumber.replaceAll("[^0-9]", "");
        
        if (cleanCard.length() < CARD_VISIBLE_DIGITS) {
            throw new IllegalArgumentException("Card number too short to mask safely");
        }
        
        if (cleanCard.length() != Constants.CARD_NUMBER_LENGTH) {
            throw new IllegalArgumentException("Invalid card number length: " + cleanCard.length());
        }
        
        String lastFour = cleanCard.substring(cleanCard.length() - CARD_VISIBLE_DIGITS);
        return CARD_MASK_PATTERN + lastFour;
    }

    /**
     * Masks sensitive data with configurable visible character count.
     * 
     * Provides generic masking functionality for various types of
     * sensitive data beyond card numbers.
     * 
     * @param sensitiveData data to mask
     * @param visibleChars number of characters to leave visible
     * @return masked data string
     */
    public static String maskSensitiveData(String sensitiveData, int visibleChars) {
        if (sensitiveData == null || sensitiveData.isEmpty()) {
            return "";
        }
        
        if (visibleChars >= sensitiveData.length()) {
            return sensitiveData;
        }
        
        String visible = sensitiveData.substring(sensitiveData.length() - visibleChars);
        String masked = "*".repeat(sensitiveData.length() - visibleChars);
        
        return masked + visible;
    }

    /**
     * Formats strings to fixed length matching COBOL field specifications.
     * 
     * Ensures strings conform to COBOL fixed-length field requirements
     * by truncating or padding as necessary.
     * 
     * @param text string to format
     * @param length target fixed length
     * @return fixed-length formatted string
     */
    public static String formatFixedLength(String text, int length) {
        return StringUtil.formatFixedLength(text, length);
    }

    /**
     * Formats numbers with zero suppression matching COBOL Z patterns.
     * 
     * Implements COBOL zero suppression display patterns where leading
     * zeros are replaced with spaces for improved readability.
     * 
     * @param number numeric value to format
     * @param totalWidth total field width including decimals
     * @param decimalPlaces number of decimal places
     * @return zero-suppressed formatted string
     */
    public static String formatZeroSuppressed(BigDecimal number, int totalWidth, int decimalPlaces) {
        if (number == null) {
            return " ".repeat(totalWidth);
        }
        
        BigDecimal scaledNumber = CobolDataConverter.preservePrecision(number, decimalPlaces);
        
        // Create pattern with leading spaces for zero suppression
        StringBuilder patternBuilder = new StringBuilder();
        int integerWidth = totalWidth - decimalPlaces - (decimalPlaces > 0 ? 1 : 0);
        
        // Add leading suppression characters
        for (int i = 0; i < integerWidth - 1; i++) {
            patternBuilder.append("#");
        }
        patternBuilder.append("0");
        
        // Add decimal part if needed
        if (decimalPlaces > 0) {
            patternBuilder.append(".");
            patternBuilder.append("0".repeat(decimalPlaces));
        }
        
        DecimalFormat formatter = new DecimalFormat(patternBuilder.toString());
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        
        String formatted = formatter.format(scaledNumber);
        return StringUtils.leftPad(formatted, totalWidth, ' ');
    }

    /**
     * Formats numbers with thousands separators for display clarity.
     * 
     * Adds comma separators to large numbers while maintaining
     * precision and COBOL-compatible rounding behavior.
     * 
     * @param number numeric value to format
     * @param decimalPlaces number of decimal places to show
     * @return formatted number with thousands separators
     */
    public static String formatWithThousandsSeparator(BigDecimal number, int decimalPlaces) {
        if (number == null) {
            return "0" + (decimalPlaces > 0 ? "." + "0".repeat(decimalPlaces) : "");
        }
        
        return formatNumber(number, decimalPlaces);
    }

    /**
     * Formats decimal amounts with exact precision preservation.
     * 
     * Ensures decimal amounts maintain exact precision matching
     * COBOL V99 specifications for financial calculations.
     * 
     * @param amount decimal amount to format
     * @return precisely formatted decimal amount
     */
    public static String formatDecimalAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        
        BigDecimal scaledAmount = CobolDataConverter.preservePrecision(amount, CobolDataConverter.MONETARY_SCALE);
        DecimalFormat formatter = new DecimalFormat("0.00");
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        
        return formatter.format(scaledAmount);
    }

    /**
     * Formats signed amounts with explicit positive/negative indicators.
     * 
     * Provides clear sign indication for amounts in financial reports
     * and audit trails matching COBOL signed field displays.
     * 
     * @param amount signed amount to format
     * @return formatted amount with explicit sign
     */
    public static String formatSignedAmount(BigDecimal amount) {
        return formatCurrencyWithSign(amount);
    }

    /**
     * Formats amounts for reports matching COBOL report layouts.
     * 
     * Implements COBOL report formatting patterns with proper alignment
     * and zero suppression for financial statement generation.
     * 
     * @param amount report amount to format
     * @param fieldWidth total field width for alignment
     * @return report-formatted amount string
     */
    public static String formatReportAmount(BigDecimal amount, int fieldWidth) {
        if (amount == null) {
            return StringUtils.leftPad("", fieldWidth, ' ');
        }
        
        String formatted = formatDecimalAmount(amount);
        return StringUtils.leftPad(formatted, fieldWidth, ' ');
    }

    /**
     * Formats amounts for display with standard currency presentation.
     * 
     * Provides consistent amount display formatting for user interfaces
     * and screen presentations matching original COBOL displays.
     * 
     * @param amount display amount to format
     * @return display-formatted amount string
     */
    public static String formatDisplayAmount(BigDecimal amount) {
        return formatCurrency(amount);
    }

    // Static utility functions for standalone use

    /**
     * Formats data according to COBOL PIC pattern specifications.
     * 
     * Provides COBOL pattern-based formatting using PIC clause specifications
     * to ensure exact compatibility with legacy data formatting.
     * 
     * @param value data value to format
     * @param picPattern COBOL PIC pattern (e.g., "PIC X(10)", "PIC S9(5)V99")
     * @return formatted data according to PIC pattern
     */
    public static String formatCobolPattern(Object value, String picPattern) {
        if (value == null) {
            return "";
        }
        
        Object converted = CobolDataConverter.convertToJavaType(value, picPattern);
        
        if (converted instanceof String) {
            return (String) converted;
        } else if (converted instanceof BigDecimal) {
            return formatDecimalAmount((BigDecimal) converted);
        } else {
            return converted.toString();
        }
    }

    /**
     * Validates format specifications and data compatibility.
     * 
     * Performs validation of format specifications and data values
     * to ensure compatibility with COBOL field requirements.
     * 
     * @param value data value to validate
     * @param formatSpec format specification or PIC clause
     * @return true if value is compatible with format specification
     */
    public static boolean validateFormat(Object value, String formatSpec) {
        try {
            if (formatSpec.toUpperCase().startsWith("PIC")) {
                return CobolDataConverter.validateCobolField(value, formatSpec);
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Formats data to specified fixed width with alignment control.
     * 
     * Provides precise fixed-width formatting with left or right alignment
     * for maintaining COBOL fixed-length record compatibility.
     * 
     * @param data data to format
     * @param width target field width
     * @param rightAlign true for right alignment, false for left
     * @return fixed-width formatted string
     */
    public static String formatFixedWidth(String data, int width, boolean rightAlign) {
        if (data == null) {
            data = "";
        }
        
        if (data.length() > width) {
            return data.substring(0, width);
        }
        
        if (rightAlign) {
            return StringUtils.leftPad(data, width, ' ');
        } else {
            return StringUtils.rightPad(data, width, ' ');
        }
    }

    /**
     * Aligns field data within specified boundaries.
     * 
     * Provides field alignment functionality for report generation
     * and display formatting with precise positioning control.
     * 
     * @param fieldData data to align
     * @param fieldWidth target field width
     * @param alignment alignment specification ("LEFT", "RIGHT", "CENTER")
     * @return aligned field data
     */
    public static String alignField(String fieldData, int fieldWidth, String alignment) {
        if (fieldData == null) {
            fieldData = "";
        }
        
        if (fieldData.length() >= fieldWidth) {
            return fieldData.substring(0, fieldWidth);
        }
        
        switch (alignment.toUpperCase()) {
            case "LEFT":
                return StringUtils.rightPad(fieldData, fieldWidth, ' ');
            case "RIGHT":
                return StringUtils.leftPad(fieldData, fieldWidth, ' ');
            case "CENTER":
                int leftPad = (fieldWidth - fieldData.length()) / 2;
                int rightPad = fieldWidth - fieldData.length() - leftPad;
                return " ".repeat(leftPad) + fieldData + " ".repeat(rightPad);
            default:
                return StringUtils.rightPad(fieldData, fieldWidth, ' ');
        }
    }

    /**
     * Formats numeric values with proper scale and zero-padding for batch reports.
     * Provides numeric formatting matching COBOL PIC clause specifications.
     *
     * @param value the numeric value to format (BigDecimal, Integer, Long, etc.)
     * @param totalDigits total number of digits including decimal places
     * @param decimalPlaces number of decimal places to display
     * @return formatted numeric string with proper padding and alignment
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static String formatNumeric(Object value, int totalDigits, int decimalPlaces) {
        if (totalDigits <= 0) {
            throw new IllegalArgumentException("Total digits must be positive: " + totalDigits);
        }
        
        if (decimalPlaces < 0) {
            throw new IllegalArgumentException("Decimal places cannot be negative: " + decimalPlaces);
        }
        
        if (decimalPlaces >= totalDigits) {
            throw new IllegalArgumentException("Decimal places must be less than total digits");
        }
        
        BigDecimal decimalValue;
        if (value == null) {
            decimalValue = BigDecimal.ZERO;
        } else if (value instanceof BigDecimal) {
            decimalValue = (BigDecimal) value;
        } else if (value instanceof Number) {
            decimalValue = new BigDecimal(value.toString());
        } else {
            decimalValue = new BigDecimal(value.toString());
        }
        
        // Set scale with HALF_UP rounding to match COBOL behavior
        decimalValue = decimalValue.setScale(decimalPlaces, RoundingMode.HALF_UP);
        
        // Create format pattern with leading zeros
        StringBuilder pattern = new StringBuilder();
        int integerDigits = totalDigits - decimalPlaces;
        pattern.append("0".repeat(integerDigits));
        if (decimalPlaces > 0) {
            pattern.append(".");
            pattern.append("0".repeat(decimalPlaces));
        }
        
        DecimalFormat formatter = new DecimalFormat(pattern.toString());
        return formatter.format(decimalValue);
    }

    /**
     * Formats a value according to a COBOL PIC clause specification.
     * Provides COBOL PIC clause compatible formatting for batch report generation.
     *
     * @param value the value to format
     * @param picClause the COBOL PIC clause specification (e.g., "PIC 9(5)V99", "PIC X(20)")
     * @return formatted string matching the PIC clause specification
     * @throws IllegalArgumentException if PIC clause is invalid
     */
    public static String formatWithPicture(Object value, String picClause) {
        if (picClause == null || picClause.trim().isEmpty()) {
            throw new IllegalArgumentException("PIC clause cannot be null or empty");
        }
        
        String normalizedPic = picClause.trim().toUpperCase();
        if (!normalizedPic.startsWith("PIC")) {
            normalizedPic = "PIC " + normalizedPic;
        }
        
        // Handle numeric PIC clauses (9 patterns)
        if (normalizedPic.contains("9")) {
            return formatNumericWithPic(value, normalizedPic);
        }
        
        // Handle alphanumeric PIC clauses (X patterns)
        if (normalizedPic.contains("X")) {
            return formatAlphanumericWithPic(value, normalizedPic);
        }
        
        // Default formatting for unrecognized patterns
        return value != null ? value.toString() : "";
    }

    /**
     * Aligns decimal points in numeric values for columnar display.
     * Provides decimal alignment for financial reports and columnar data display.
     *
     * @param value the numeric value to align
     * @param fieldWidth the total field width for alignment
     * @param decimalPlaces the number of decimal places to maintain
     * @return decimal-aligned string suitable for columnar display
     */
    public static String alignDecimal(Object value, int fieldWidth, int decimalPlaces) {
        if (fieldWidth <= 0) {
            throw new IllegalArgumentException("Field width must be positive: " + fieldWidth);
        }
        
        if (decimalPlaces < 0) {
            throw new IllegalArgumentException("Decimal places cannot be negative: " + decimalPlaces);
        }
        
        BigDecimal decimalValue;
        if (value == null) {
            decimalValue = BigDecimal.ZERO;
        } else if (value instanceof BigDecimal) {
            decimalValue = (BigDecimal) value;
        } else if (value instanceof Number) {
            decimalValue = new BigDecimal(value.toString());
        } else {
            try {
                decimalValue = new BigDecimal(value.toString());
            } catch (NumberFormatException e) {
                // Non-numeric value, return right-aligned string
                return StringUtils.leftPad(value.toString(), fieldWidth, ' ');
            }
        }
        
        // Set scale with HALF_UP rounding
        decimalValue = decimalValue.setScale(decimalPlaces, RoundingMode.HALF_UP);
        
        // Format with proper decimal places
        DecimalFormat formatter = new DecimalFormat();
        formatter.setMinimumFractionDigits(decimalPlaces);
        formatter.setMaximumFractionDigits(decimalPlaces);
        formatter.setGroupingUsed(false);
        
        String formattedValue = formatter.format(decimalValue);
        
        // Right-align within the field width
        return StringUtils.leftPad(formattedValue, fieldWidth, ' ');
    }

    /**
     * Helper method for formatting numeric values with COBOL PIC 9 patterns.
     */
    private static String formatNumericWithPic(Object value, String picClause) {
        // Parse PIC clause to extract digit counts and decimal places
        // Simplified implementation for common patterns like PIC 9(5)V99
        String pattern = picClause.replaceAll("PIC\\s+", "");
        
        int totalDigits = 0;
        int decimalPlaces = 0;
        
        // Count 9s before V
        if (pattern.contains("V")) {
            String[] parts = pattern.split("V");
            totalDigits += countDigits(parts[0]);
            if (parts.length > 1) {
                decimalPlaces = countDigits(parts[1]);
                totalDigits += decimalPlaces;
            }
        } else {
            totalDigits = countDigits(pattern);
        }
        
        return formatNumeric(value, totalDigits, decimalPlaces);
    }

    /**
     * Helper method for formatting alphanumeric values with COBOL PIC X patterns.
     */
    private static String formatAlphanumericWithPic(Object value, String picClause) {
        // Parse PIC clause to extract character count
        String pattern = picClause.replaceAll("PIC\\s+", "");
        int fieldWidth = countCharacters(pattern);
        
        String stringValue = value != null ? value.toString() : "";
        return StringUtils.rightPad(stringValue, fieldWidth, ' ').substring(0, fieldWidth);
    }

    /**
     * Helper method to count digits in a PIC pattern.
     */
    private static int countDigits(String pattern) {
        int count = 0;
        // Count explicit 9s
        for (char c : pattern.toCharArray()) {
            if (c == '9') {
                count++;
            }
        }
        
        // Handle 9(n) notation
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("9\\((\\d+)\\)");
        java.util.regex.Matcher m = p.matcher(pattern);
        while (m.find()) {
            count += Integer.parseInt(m.group(1)) - 1; // -1 because we already counted the 9
        }
        
        return count;
    }

    /**
     * Helper method to count characters in a PIC X pattern.
     */
    private static int countCharacters(String pattern) {
        int count = 0;
        // Count explicit Xs
        for (char c : pattern.toCharArray()) {
            if (c == 'X') {
                count++;
            }
        }
        
        // Handle X(n) notation
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("X\\((\\d+)\\)");
        java.util.regex.Matcher m = p.matcher(pattern);
        while (m.find()) {
            count += Integer.parseInt(m.group(1)) - 1; // -1 because we already counted the X
        }
        
        return count;
    }
}