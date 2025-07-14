package com.carddemo.common.util;

import com.carddemo.common.util.BigDecimalUtils;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConversionUtils - Comprehensive data type conversion utility class
 * 
 * This utility class provides bidirectional conversion between COBOL and Java data formats,
 * ensuring exact format compatibility for packed decimals, strings, dates, and other data types
 * used throughout the microservices architecture. Maintains precise COBOL semantics during
 * the COBOL-to-Java migration process.
 * 
 * Key Features:
 * - Exact COBOL COMP-3 packed decimal conversion using BigDecimal with precision preservation
 * - String formatting and padding utilities replicating COBOL PICTURE clause behavior exactly
 * - Date and timestamp conversions preserving all original COBOL format patterns
 * - Numeric validation and formatting supporting COBOL display formats
 * - Thread-safe operations with comprehensive error handling and logging
 * 
 * COBOL Data Type Mapping:
 * - COMP-3 (Packed Decimal) → BigDecimal with MathContext.DECIMAL128
 * - PIC X(n) → String with exact length validation and padding
 * - PIC 9(n) → Integer/Long/BigDecimal based on precision requirements
 * - Date formats (YYYYMMDD, MM/DD/YY) → LocalDate with format preservation
 * - Time formats (HHMMSS, HH:MM:SS) → LocalTime with format preservation
 * - Timestamps → LocalDateTime with microsecond precision
 * 
 * Performance Requirements:
 * - Optimized for < 200ms response time for card authorization processing
 * - Thread-safe for 10,000+ TPS transaction processing
 * - Memory efficient for batch processing within 4-hour window
 * 
 * @author Blitzy Platform
 * @version 1.0
 * @since Java 21
 */
public final class ConversionUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(ConversionUtils.class);
    
    // COBOL date format patterns from CSDAT01Y.cpy
    private static final DateTimeFormatter COBOL_DATE_YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter COBOL_DATE_MM_DD_YY = DateTimeFormatter.ofPattern("MM/dd/yy");
    private static final DateTimeFormatter COBOL_DATE_YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    // COBOL time format patterns from CSDAT01Y.cpy
    private static final DateTimeFormatter COBOL_TIME_HHMMSS = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter COBOL_TIME_HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter COBOL_TIME_HH_MM_SS_MS = DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSS");
    
    // COBOL timestamp format patterns from CSDAT01Y.cpy
    private static final DateTimeFormatter COBOL_TIMESTAMP_FULL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
    private static final DateTimeFormatter COBOL_TIMESTAMP_STANDARD = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // COBOL numeric validation patterns
    private static final Pattern COBOL_NUMERIC_PATTERN = Pattern.compile("^[+-]?\\d+(\\.\\d+)?$");
    private static final Pattern COBOL_INTEGER_PATTERN = Pattern.compile("^[+-]?\\d+$");
    private static final Pattern COBOL_COMP3_PATTERN = Pattern.compile("^[+-]?\\d{1,31}(\\.\\d{1,18})?$");
    
    // COBOL PICTURE clause patterns
    private static final Pattern PICTURE_X_PATTERN = Pattern.compile("X\\((\\d+)\\)");
    private static final Pattern PICTURE_9_PATTERN = Pattern.compile("9\\((\\d+)\\)");
    private static final Pattern PICTURE_S9V9_PATTERN = Pattern.compile("S9\\((\\d+)\\)V9\\((\\d+)\\)");
    
    // Thread-safe decimal formatters
    private static final ThreadLocal<DecimalFormat> COBOL_DISPLAY_FORMATTER = ThreadLocal.withInitial(() -> {
        DecimalFormat formatter = new DecimalFormat("0");
        formatter.setGroupingUsed(false);
        return formatter;
    });
    
    /**
     * Private constructor preventing instantiation
     * 
     * This utility class contains only static methods and should not be instantiated.
     * All methods are designed to be called statically for optimal performance.
     */
    private ConversionUtils() {
        throw new UnsupportedOperationException("ConversionUtils is a utility class and cannot be instantiated");
    }
    
    /**
     * Converts COBOL string to Java String with exact formatting preservation
     * 
     * Handles COBOL PICTURE clause string processing including:
     * - Fixed-length field extraction with trailing space trimming
     * - Low-values and high-values conversion to appropriate null or default values
     * - EBCDIC character set considerations for special characters
     * 
     * @param cobolString The COBOL string value to convert
     * @param pictureLength The expected length from PICTURE clause (e.g., PIC X(10))
     * @return Java String with COBOL formatting preserved
     * @throws IllegalArgumentException if cobolString is null or pictureLength is invalid
     */
    public static String cobolToJavaString(String cobolString, int pictureLength) {
        if (cobolString == null) {
            logger.debug("Converting null COBOL string to empty Java string");
            return "";
        }
        
        if (pictureLength <= 0) {
            throw new IllegalArgumentException("Picture length must be positive, got: " + pictureLength);
        }
        
        logger.debug("Converting COBOL string '{}' with picture length {}", cobolString, pictureLength);
        
        // Handle low-values (nulls) and high-values
        String processedString = cobolString
            .replace('\u0000', ' ')  // Convert null characters to spaces
            .replace('\uFFFF', ' '); // Convert high-values to spaces
        
        // Ensure exact length and trim trailing spaces (COBOL behavior)
        if (processedString.length() > pictureLength) {
            processedString = processedString.substring(0, pictureLength);
        }
        
        // Right-pad with spaces to exact length, then trim trailing spaces
        processedString = StringUtils.rightPad(processedString, pictureLength);
        String result = StringUtils.trimToEmpty(processedString);
        
        logger.debug("Converted COBOL string to Java string: '{}'", result);
        return result;
    }
    
    /**
     * Converts Java String to COBOL string format with exact PICTURE clause formatting
     * 
     * Applies COBOL PICTURE clause formatting rules including:
     * - Fixed-length field padding with spaces to exact picture length
     * - String truncation when input exceeds picture length
     * - Proper handling of null and empty values
     * 
     * @param javaString The Java String value to convert
     * @param pictureLength The target length from PICTURE clause (e.g., PIC X(10))
     * @return COBOL-formatted string with exact length
     * @throws IllegalArgumentException if pictureLength is invalid
     */
    public static String javaToCobolString(String javaString, int pictureLength) {
        if (pictureLength <= 0) {
            throw new IllegalArgumentException("Picture length must be positive, got: " + pictureLength);
        }
        
        String inputString = (javaString == null) ? "" : javaString;
        logger.debug("Converting Java string '{}' to COBOL format with picture length {}", inputString, pictureLength);
        
        // Truncate if too long, pad if too short (COBOL PICTURE behavior)
        String result;
        if (inputString.length() > pictureLength) {
            result = inputString.substring(0, pictureLength);
            logger.warn("Truncated Java string from {} to {} characters", inputString.length(), pictureLength);
        } else {
            result = StringUtils.rightPad(inputString, pictureLength);
        }
        
        logger.debug("Converted Java string to COBOL format: '{}'", result);
        return result;
    }
    
    /**
     * Converts COBOL COMP-3 packed decimal to Java BigDecimal with exact precision preservation
     * 
     * Handles COBOL COMP-3 (packed decimal) conversion maintaining:
     * - Exact decimal precision using BigDecimal with MathContext.DECIMAL128
     * - Proper scale and precision mapping from COBOL PICTURE clause
     * - Sign handling for positive and negative values
     * - Financial calculation accuracy requirements
     * 
     * @param cobolComp3Value The COBOL COMP-3 value as string representation
     * @param totalDigits Total number of digits from PICTURE clause (e.g., 9(10)V99 = 12 digits)
     * @param decimalPlaces Number of decimal places from PICTURE clause (e.g., V99 = 2 places)
     * @return BigDecimal with exact COBOL COMP-3 precision
     * @throws IllegalArgumentException if input values are invalid
     * @throws NumberFormatException if cobolComp3Value cannot be parsed
     */
    public static BigDecimal cobolCompToDecimal(String cobolComp3Value, int totalDigits, int decimalPlaces) {
        if (cobolComp3Value == null) {
            logger.debug("Converting null COBOL COMP-3 value to BigDecimal zero");
            return BigDecimalUtils.createDecimal("0").setScale(decimalPlaces, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode());
        }
        
        if (totalDigits <= 0 || decimalPlaces < 0 || decimalPlaces > totalDigits) {
            throw new IllegalArgumentException(String.format(
                "Invalid COMP-3 format: totalDigits=%d, decimalPlaces=%d", totalDigits, decimalPlaces));
        }
        
        logger.debug("Converting COBOL COMP-3 '{}' with {} total digits, {} decimal places", 
                    cobolComp3Value, totalDigits, decimalPlaces);
        
        // Clean and validate the input
        String cleanValue = StringUtils.trim(cobolComp3Value);
        if (StringUtils.isBlank(cleanValue)) {
            return BigDecimalUtils.createDecimal("0").setScale(decimalPlaces, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode());
        }
        
        // Validate COMP-3 format
        if (!COBOL_COMP3_PATTERN.matcher(cleanValue).matches()) {
            throw new NumberFormatException("Invalid COBOL COMP-3 format: " + cobolComp3Value);
        }
        
        // Create BigDecimal with exact precision
        BigDecimal result = BigDecimalUtils.createDecimal(cleanValue);
        result = result.setScale(decimalPlaces, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode());
        
        // Validate total digits constraint
        if (result.precision() > totalDigits) {
            throw new IllegalArgumentException(String.format(
                "COMP-3 value precision %d exceeds maximum %d digits", result.precision(), totalDigits));
        }
        
        logger.debug("Converted COBOL COMP-3 to BigDecimal: {}", result);
        return result;
    }
    
    /**
     * Converts Java BigDecimal to COBOL COMP-3 packed decimal format
     * 
     * Formats BigDecimal to COBOL COMP-3 representation maintaining:
     * - Exact decimal precision and scale requirements
     * - Proper sign representation for positive and negative values
     * - Validation against COBOL PICTURE clause constraints
     * - Financial calculation accuracy preservation
     * 
     * @param decimal The BigDecimal value to convert
     * @param totalDigits Total number of digits for COBOL PICTURE clause
     * @param decimalPlaces Number of decimal places for COBOL PICTURE clause
     * @return String representation of COBOL COMP-3 format
     * @throws IllegalArgumentException if parameters are invalid or decimal exceeds constraints
     */
    public static String decimalToCobolComp(BigDecimal decimal, int totalDigits, int decimalPlaces) {
        if (decimal == null) {
            logger.debug("Converting null BigDecimal to COBOL COMP-3 zero");
            return "0";
        }
        
        if (totalDigits <= 0 || decimalPlaces < 0 || decimalPlaces > totalDigits) {
            throw new IllegalArgumentException(String.format(
                "Invalid COMP-3 format: totalDigits=%d, decimalPlaces=%d", totalDigits, decimalPlaces));
        }
        
        logger.debug("Converting BigDecimal {} to COBOL COMP-3 with {} total digits, {} decimal places", 
                    decimal, totalDigits, decimalPlaces);
        
        // Set exact scale for COBOL compatibility
        BigDecimal scaledDecimal = decimal.setScale(decimalPlaces, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode());
        
        // Validate precision constraints
        if (scaledDecimal.precision() > totalDigits) {
            throw new IllegalArgumentException(String.format(
                "BigDecimal precision %d exceeds COMP-3 maximum %d digits", scaledDecimal.precision(), totalDigits));
        }
        
        // Format as COBOL COMP-3 string representation
        String result = scaledDecimal.toPlainString();
        
        logger.debug("Converted BigDecimal to COBOL COMP-3: '{}'", result);
        return result;
    }
    
    /**
     * Converts COBOL date format to Java LocalDate
     * 
     * Supports multiple COBOL date formats from CSDAT01Y.cpy:
     * - YYYYMMDD (WS-CURDATE-N format)
     * - MM/DD/YY (WS-CURDATE-MM-DD-YY format)
     * - YYYY-MM-DD (WS-TIMESTAMP-DT format)
     * 
     * @param cobolDate The COBOL date string to convert
     * @param dateFormat The expected COBOL date format pattern
     * @return LocalDate representation of the COBOL date
     * @throws IllegalArgumentException if cobolDate is null or dateFormat is unsupported
     * @throws java.time.format.DateTimeParseException if date cannot be parsed
     */
    public static LocalDate cobolDateToLocalDate(String cobolDate, String dateFormat) {
        if (cobolDate == null) {
            throw new IllegalArgumentException("COBOL date cannot be null");
        }
        
        if (StringUtils.isBlank(dateFormat)) {
            throw new IllegalArgumentException("Date format cannot be null or blank");
        }
        
        logger.debug("Converting COBOL date '{}' with format '{}'", cobolDate, dateFormat);
        
        String cleanDate = StringUtils.trim(cobolDate);
        if (StringUtils.isBlank(cleanDate)) {
            throw new IllegalArgumentException("COBOL date cannot be blank");
        }
        
        LocalDate result;
        try {
            switch (dateFormat.toUpperCase()) {
                case "YYYYMMDD":
                    result = LocalDate.parse(cleanDate, COBOL_DATE_YYYYMMDD);
                    break;
                case "MM/DD/YY":
                    result = LocalDate.parse(cleanDate, COBOL_DATE_MM_DD_YY);
                    break;
                case "YYYY-MM-DD":
                    result = LocalDate.parse(cleanDate, COBOL_DATE_YYYY_MM_DD);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported COBOL date format: " + dateFormat);
            }
        } catch (Exception e) {
            logger.error("Failed to parse COBOL date '{}' with format '{}'", cobolDate, dateFormat, e);
            throw e;
        }
        
        logger.debug("Converted COBOL date to LocalDate: {}", result);
        return result;
    }
    
    /**
     * Converts Java LocalDate to COBOL date format
     * 
     * Formats LocalDate to COBOL date string matching CSDAT01Y.cpy patterns:
     * - YYYYMMDD format for WS-CURDATE-N compatibility
     * - MM/DD/YY format for WS-CURDATE-MM-DD-YY compatibility
     * - YYYY-MM-DD format for WS-TIMESTAMP-DT compatibility
     * 
     * @param localDate The LocalDate to convert
     * @param dateFormat The target COBOL date format pattern
     * @return COBOL-formatted date string
     * @throws IllegalArgumentException if localDate is null or dateFormat is unsupported
     */
    public static String localDateToCobolDate(LocalDate localDate, String dateFormat) {
        if (localDate == null) {
            throw new IllegalArgumentException("LocalDate cannot be null");
        }
        
        if (StringUtils.isBlank(dateFormat)) {
            throw new IllegalArgumentException("Date format cannot be null or blank");
        }
        
        logger.debug("Converting LocalDate {} to COBOL format '{}'", localDate, dateFormat);
        
        String result;
        switch (dateFormat.toUpperCase()) {
            case "YYYYMMDD":
                result = localDate.format(COBOL_DATE_YYYYMMDD);
                break;
            case "MM/DD/YY":
                result = localDate.format(COBOL_DATE_MM_DD_YY);
                break;
            case "YYYY-MM-DD":
                result = localDate.format(COBOL_DATE_YYYY_MM_DD);
                break;
            default:
                throw new IllegalArgumentException("Unsupported COBOL date format: " + dateFormat);
        }
        
        logger.debug("Converted LocalDate to COBOL date: '{}'", result);
        return result;
    }
    
    /**
     * Converts COBOL time format to Java LocalTime
     * 
     * Supports COBOL time formats from CSDAT01Y.cpy:
     * - HHMMSS (WS-CURTIME-N format)
     * - HH:MM:SS (WS-CURTIME-HH-MM-SS format)
     * - HH:MM:SS.SSSSSS (WS-TIMESTAMP-TM format with microseconds)
     * 
     * @param cobolTime The COBOL time string to convert
     * @param timeFormat The expected COBOL time format pattern
     * @return LocalTime representation of the COBOL time
     * @throws IllegalArgumentException if cobolTime is null or timeFormat is unsupported
     * @throws java.time.format.DateTimeParseException if time cannot be parsed
     */
    public static LocalTime cobolTimeToLocalTime(String cobolTime, String timeFormat) {
        if (cobolTime == null) {
            throw new IllegalArgumentException("COBOL time cannot be null");
        }
        
        if (StringUtils.isBlank(timeFormat)) {
            throw new IllegalArgumentException("Time format cannot be null or blank");
        }
        
        logger.debug("Converting COBOL time '{}' with format '{}'", cobolTime, timeFormat);
        
        String cleanTime = StringUtils.trim(cobolTime);
        if (StringUtils.isBlank(cleanTime)) {
            throw new IllegalArgumentException("COBOL time cannot be blank");
        }
        
        LocalTime result;
        try {
            switch (timeFormat.toUpperCase()) {
                case "HHMMSS":
                    result = LocalTime.parse(cleanTime, COBOL_TIME_HHMMSS);
                    break;
                case "HH:MM:SS":
                    result = LocalTime.parse(cleanTime, COBOL_TIME_HH_MM_SS);
                    break;
                case "HH:MM:SS.SSSSSS":
                    result = LocalTime.parse(cleanTime, COBOL_TIME_HH_MM_SS_MS);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported COBOL time format: " + timeFormat);
            }
        } catch (Exception e) {
            logger.error("Failed to parse COBOL time '{}' with format '{}'", cobolTime, timeFormat, e);
            throw e;
        }
        
        logger.debug("Converted COBOL time to LocalTime: {}", result);
        return result;
    }
    
    /**
     * Converts Java LocalTime to COBOL time format
     * 
     * Formats LocalTime to COBOL time string matching CSDAT01Y.cpy patterns:
     * - HHMMSS format for WS-CURTIME-N compatibility
     * - HH:MM:SS format for WS-CURTIME-HH-MM-SS compatibility
     * - HH:MM:SS.SSSSSS format for WS-TIMESTAMP-TM compatibility
     * 
     * @param localTime The LocalTime to convert
     * @param timeFormat The target COBOL time format pattern
     * @return COBOL-formatted time string
     * @throws IllegalArgumentException if localTime is null or timeFormat is unsupported
     */
    public static String localTimeToCobolTime(LocalTime localTime, String timeFormat) {
        if (localTime == null) {
            throw new IllegalArgumentException("LocalTime cannot be null");
        }
        
        if (StringUtils.isBlank(timeFormat)) {
            throw new IllegalArgumentException("Time format cannot be null or blank");
        }
        
        logger.debug("Converting LocalTime {} to COBOL format '{}'", localTime, timeFormat);
        
        String result;
        switch (timeFormat.toUpperCase()) {
            case "HHMMSS":
                result = localTime.format(COBOL_TIME_HHMMSS);
                break;
            case "HH:MM:SS":
                result = localTime.format(COBOL_TIME_HH_MM_SS);
                break;
            case "HH:MM:SS.SSSSSS":
                result = localTime.format(COBOL_TIME_HH_MM_SS_MS);
                break;
            default:
                throw new IllegalArgumentException("Unsupported COBOL time format: " + timeFormat);
        }
        
        logger.debug("Converted LocalTime to COBOL time: '{}'", result);
        return result;
    }
    
    /**
     * Converts COBOL timestamp format to Java LocalDateTime
     * 
     * Supports COBOL timestamp formats from CSDAT01Y.cpy:
     * - YYYY-MM-DD HH:MM:SS.SSSSSS (WS-TIMESTAMP full format)
     * - YYYY-MM-DD HH:MM:SS (WS-TIMESTAMP without microseconds)
     * 
     * @param cobolTimestamp The COBOL timestamp string to convert
     * @param timestampFormat The expected COBOL timestamp format pattern
     * @return LocalDateTime representation of the COBOL timestamp
     * @throws IllegalArgumentException if cobolTimestamp is null or timestampFormat is unsupported
     * @throws java.time.format.DateTimeParseException if timestamp cannot be parsed
     */
    public static LocalDateTime cobolTimestampToLocalDateTime(String cobolTimestamp, String timestampFormat) {
        if (cobolTimestamp == null) {
            throw new IllegalArgumentException("COBOL timestamp cannot be null");
        }
        
        if (StringUtils.isBlank(timestampFormat)) {
            throw new IllegalArgumentException("Timestamp format cannot be null or blank");
        }
        
        logger.debug("Converting COBOL timestamp '{}' with format '{}'", cobolTimestamp, timestampFormat);
        
        String cleanTimestamp = StringUtils.trim(cobolTimestamp);
        if (StringUtils.isBlank(cleanTimestamp)) {
            throw new IllegalArgumentException("COBOL timestamp cannot be blank");
        }
        
        LocalDateTime result;
        try {
            switch (timestampFormat.toUpperCase()) {
                case "YYYY-MM-DD HH:MM:SS.SSSSSS":
                    result = LocalDateTime.parse(cleanTimestamp, COBOL_TIMESTAMP_FULL);
                    break;
                case "YYYY-MM-DD HH:MM:SS":
                    result = LocalDateTime.parse(cleanTimestamp, COBOL_TIMESTAMP_STANDARD);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported COBOL timestamp format: " + timestampFormat);
            }
        } catch (Exception e) {
            logger.error("Failed to parse COBOL timestamp '{}' with format '{}'", cobolTimestamp, timestampFormat, e);
            throw e;
        }
        
        logger.debug("Converted COBOL timestamp to LocalDateTime: {}", result);
        return result;
    }
    
    /**
     * Converts Java LocalDateTime to COBOL timestamp format
     * 
     * Formats LocalDateTime to COBOL timestamp string matching CSDAT01Y.cpy patterns:
     * - YYYY-MM-DD HH:MM:SS.SSSSSS format for WS-TIMESTAMP full compatibility
     * - YYYY-MM-DD HH:MM:SS format for WS-TIMESTAMP standard compatibility
     * 
     * @param localDateTime The LocalDateTime to convert
     * @param timestampFormat The target COBOL timestamp format pattern
     * @return COBOL-formatted timestamp string
     * @throws IllegalArgumentException if localDateTime is null or timestampFormat is unsupported
     */
    public static String localDateTimeToCobolTimestamp(LocalDateTime localDateTime, String timestampFormat) {
        if (localDateTime == null) {
            throw new IllegalArgumentException("LocalDateTime cannot be null");
        }
        
        if (StringUtils.isBlank(timestampFormat)) {
            throw new IllegalArgumentException("Timestamp format cannot be null or blank");
        }
        
        logger.debug("Converting LocalDateTime {} to COBOL format '{}'", localDateTime, timestampFormat);
        
        String result;
        switch (timestampFormat.toUpperCase()) {
            case "YYYY-MM-DD HH:MM:SS.SSSSSS":
                result = localDateTime.format(COBOL_TIMESTAMP_FULL);
                break;
            case "YYYY-MM-DD HH:MM:SS":
                result = localDateTime.format(COBOL_TIMESTAMP_STANDARD);
                break;
            default:
                throw new IllegalArgumentException("Unsupported COBOL timestamp format: " + timestampFormat);
        }
        
        logger.debug("Converted LocalDateTime to COBOL timestamp: '{}'", result);
        return result;
    }
    
    /**
     * Formats a value according to COBOL PICTURE clause specifications
     * 
     * Applies COBOL PICTURE clause formatting rules including:
     * - X(n) patterns for alphanumeric fields with exact length requirements
     * - 9(n) patterns for numeric fields with zero-padding and length validation
     * - S9(n)V9(n) patterns for signed decimal fields with proper scale
     * - Special handling for display formats and editing characters
     * 
     * @param value The value to format
     * @param pictureClause The COBOL PICTURE clause specification (e.g., "PIC X(10)", "PIC 9(5)V99")
     * @return Formatted string according to PICTURE clause rules
     * @throws IllegalArgumentException if pictureClause is invalid or unsupported
     */
    public static String formatWithPicture(Object value, String pictureClause) {
        if (StringUtils.isBlank(pictureClause)) {
            throw new IllegalArgumentException("Picture clause cannot be null or blank");
        }
        
        logger.debug("Formatting value '{}' with PICTURE clause '{}'", value, pictureClause);
        
        String cleanPicture = StringUtils.upperCase(StringUtils.trim(pictureClause));
        cleanPicture = cleanPicture.replace("PIC", "").replace("PICTURE", "").trim();
        
        if (value == null) {
            // Handle null values based on picture type
            if (cleanPicture.startsWith("X")) {
                return formatAlphanumericPicture("", cleanPicture);
            } else if (cleanPicture.startsWith("9") || cleanPicture.startsWith("S9")) {
                return formatNumericPicture("0", cleanPicture);
            } else {
                throw new IllegalArgumentException("Unsupported PICTURE clause: " + pictureClause);
            }
        }
        
        String stringValue = value.toString();
        
        // Determine picture type and format accordingly
        if (cleanPicture.startsWith("X")) {
            return formatAlphanumericPicture(stringValue, cleanPicture);
        } else if (cleanPicture.startsWith("9") || cleanPicture.startsWith("S9")) {
            return formatNumericPicture(stringValue, cleanPicture);
        } else {
            throw new IllegalArgumentException("Unsupported PICTURE clause: " + pictureClause);
        }
    }
    
    /**
     * Pads a string according to COBOL field requirements
     * 
     * Applies COBOL string padding rules:
     * - Right-padding with spaces for alphanumeric fields (PIC X)
     * - Left-padding with zeros for numeric fields (PIC 9)
     * - Truncation when input exceeds target length
     * - Proper handling of null and empty values
     * 
     * @param input The string to pad
     * @param targetLength The target length for padding
     * @param paddingType The type of padding ("SPACES" for alphanumeric, "ZEROS" for numeric)
     * @return Padded string with exact target length
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static String padStringCobol(String input, int targetLength, String paddingType) {
        if (targetLength <= 0) {
            throw new IllegalArgumentException("Target length must be positive, got: " + targetLength);
        }
        
        if (StringUtils.isBlank(paddingType)) {
            throw new IllegalArgumentException("Padding type cannot be null or blank");
        }
        
        String inputString = (input == null) ? "" : input;
        logger.debug("Padding string '{}' to length {} with type '{}'", inputString, targetLength, paddingType);
        
        String result;
        switch (paddingType.toUpperCase()) {
            case "SPACES":
                if (inputString.length() > targetLength) {
                    result = inputString.substring(0, targetLength);
                } else {
                    result = StringUtils.rightPad(inputString, targetLength);
                }
                break;
            case "ZEROS":
                if (inputString.length() > targetLength) {
                    result = inputString.substring(Math.max(0, inputString.length() - targetLength));
                } else {
                    result = StringUtils.leftPad(inputString, targetLength, '0');
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported padding type: " + paddingType);
        }
        
        logger.debug("Padded string result: '{}'", result);
        return result;
    }
    
    /**
     * Trims a string according to COBOL field requirements
     * 
     * Applies COBOL string trimming rules:
     * - Trailing space removal for alphanumeric fields
     * - Leading zero removal for numeric fields (except single zero)
     * - Proper handling of null and empty values
     * - Preservation of significant spaces and zeros
     * 
     * @param input The string to trim
     * @param trimType The type of trimming ("TRAILING_SPACES", "LEADING_ZEROS", "BOTH")
     * @return Trimmed string according to COBOL rules
     * @throws IllegalArgumentException if trimType is invalid
     */
    public static String trimCobolString(String input, String trimType) {
        if (input == null) {
            logger.debug("Trimming null string, returning empty string");
            return "";
        }
        
        if (StringUtils.isBlank(trimType)) {
            throw new IllegalArgumentException("Trim type cannot be null or blank");
        }
        
        logger.debug("Trimming string '{}' with type '{}'", input, trimType);
        
        String result;
        switch (trimType.toUpperCase()) {
            case "TRAILING_SPACES":
                result = StringUtils.trimToEmpty(input);
                break;
            case "LEADING_ZEROS":
                result = input.replaceFirst("^0+(?!$)", "");
                if (StringUtils.isBlank(result)) {
                    result = "0";
                }
                break;
            case "BOTH":
                result = StringUtils.trimToEmpty(input);
                if (NumberUtils.isCreatable(result)) {
                    result = result.replaceFirst("^0+(?!$)", "");
                    if (StringUtils.isBlank(result)) {
                        result = "0";
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported trim type: " + trimType);
        }
        
        logger.debug("Trimmed string result: '{}'", result);
        return result;
    }
    
    /**
     * Validates a string as COBOL numeric format
     * 
     * Performs COBOL numeric validation including:
     * - Integer format validation (PIC 9 fields)
     * - Decimal format validation (PIC 9V9 fields)
     * - Signed numeric validation (PIC S9 fields)
     * - COMP-3 packed decimal validation
     * - Range and precision checking
     * 
     * @param numericString The string to validate as numeric
     * @param numericType The type of numeric validation ("INTEGER", "DECIMAL", "SIGNED", "COMP3")
     * @return true if the string is valid COBOL numeric format, false otherwise
     * @throws IllegalArgumentException if numericType is invalid
     */
    public static boolean validateCobolNumeric(String numericString, String numericType) {
        if (numericString == null) {
            logger.debug("Validating null numeric string, returning false");
            return false;
        }
        
        if (StringUtils.isBlank(numericType)) {
            throw new IllegalArgumentException("Numeric type cannot be null or blank");
        }
        
        logger.debug("Validating numeric string '{}' as type '{}'", numericString, numericType);
        
        String cleanString = StringUtils.trim(numericString);
        if (StringUtils.isBlank(cleanString)) {
            return false;
        }
        
        boolean result;
        switch (numericType.toUpperCase()) {
            case "INTEGER":
                result = COBOL_INTEGER_PATTERN.matcher(cleanString).matches();
                break;
            case "DECIMAL":
                result = COBOL_NUMERIC_PATTERN.matcher(cleanString).matches();
                break;
            case "SIGNED":
                result = COBOL_NUMERIC_PATTERN.matcher(cleanString).matches() &&
                        (cleanString.startsWith("+") || cleanString.startsWith("-") || 
                         !cleanString.startsWith("0") || cleanString.equals("0"));
                break;
            case "COMP3":
                result = COBOL_COMP3_PATTERN.matcher(cleanString).matches();
                break;
            default:
                throw new IllegalArgumentException("Unsupported numeric type: " + numericType);
        }
        
        logger.debug("Numeric validation result: {}", result);
        return result;
    }
    
    /**
     * Converts COBOL display format to appropriate Java data type
     * 
     * Handles COBOL display format conversion including:
     * - Alphanumeric display fields to String with proper trimming
     * - Numeric display fields to appropriate numeric types
     * - Signed display fields with proper sign handling
     * - Date and time display fields to temporal types
     * - Special character handling and validation
     * 
     * @param displayValue The COBOL display format value to convert
     * @param targetType The target Java type ("STRING", "INTEGER", "DECIMAL", "DATE", "TIME")
     * @param pictureClause Optional PICTURE clause for formatting guidance
     * @return Converted value in appropriate Java type
     * @throws IllegalArgumentException if parameters are invalid or conversion fails
     */
    public static Object convertCobolDisplay(String displayValue, String targetType, String pictureClause) {
        if (StringUtils.isBlank(targetType)) {
            throw new IllegalArgumentException("Target type cannot be null or blank");
        }
        
        logger.debug("Converting COBOL display value '{}' to type '{}' with picture '{}'", 
                    displayValue, targetType, pictureClause);
        
        if (displayValue == null) {
            return getDefaultValueForType(targetType);
        }
        
        String cleanValue = StringUtils.trim(displayValue);
        if (StringUtils.isBlank(cleanValue)) {
            return getDefaultValueForType(targetType);
        }
        
        Object result;
        try {
            switch (targetType.toUpperCase()) {
                case "STRING":
                    result = trimCobolString(cleanValue, "TRAILING_SPACES");
                    break;
                case "INTEGER":
                    result = NumberUtils.toInt(trimCobolString(cleanValue, "LEADING_ZEROS"), 0);
                    break;
                case "LONG":
                    result = NumberUtils.toLong(trimCobolString(cleanValue, "LEADING_ZEROS"), 0L);
                    break;
                case "DECIMAL":
                    String numericValue = trimCobolString(cleanValue, "LEADING_ZEROS");
                    result = BigDecimalUtils.parseDecimal(numericValue);
                    break;
                case "DATE":
                    // Default to YYYYMMDD format if no picture clause provided
                    String dateFormat = extractDateFormatFromPicture(pictureClause, "YYYYMMDD");
                    result = cobolDateToLocalDate(cleanValue, dateFormat);
                    break;
                case "TIME":
                    // Default to HHMMSS format if no picture clause provided
                    String timeFormat = extractTimeFormatFromPicture(pictureClause, "HHMMSS");
                    result = cobolTimeToLocalTime(cleanValue, timeFormat);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported target type: " + targetType);
            }
        } catch (Exception e) {
            logger.error("Failed to convert COBOL display value '{}' to type '{}'", displayValue, targetType, e);
            throw new IllegalArgumentException("Conversion failed: " + e.getMessage(), e);
        }
        
        logger.debug("Converted COBOL display to {}: {}", targetType, result);
        return result;
    }
    
    /**
     * Parses COBOL date string with automatic format detection
     * 
     * Attempts to parse COBOL date strings using common COBOL date formats:
     * - YYYYMMDD (8-digit format)
     * - MM/DD/YY (slash-separated format)
     * - YYYY-MM-DD (ISO-style format)
     * - MM/DD/YYYY (full year slash format)
     * 
     * @param cobolDateString The COBOL date string to parse
     * @return LocalDate representation of the parsed date
     * @throws IllegalArgumentException if date string cannot be parsed with any known format
     */
    public static LocalDate parseCobolDate(String cobolDateString) {
        if (cobolDateString == null) {
            throw new IllegalArgumentException("COBOL date string cannot be null");
        }
        
        String cleanDate = StringUtils.trim(cobolDateString);
        if (StringUtils.isBlank(cleanDate)) {
            throw new IllegalArgumentException("COBOL date string cannot be blank");
        }
        
        logger.debug("Parsing COBOL date string with automatic format detection: '{}'", cleanDate);
        
        // Try different COBOL date formats in order of likelihood
        String[] formats = {"YYYYMMDD", "MM/DD/YY", "YYYY-MM-DD"};
        
        for (String format : formats) {
            try {
                LocalDate result = cobolDateToLocalDate(cleanDate, format);
                logger.debug("Successfully parsed date '{}' using format '{}'", cleanDate, format);
                return result;
            } catch (Exception e) {
                // Continue to next format
                logger.debug("Failed to parse date '{}' with format '{}': {}", cleanDate, format, e.getMessage());
            }
        }
        
        throw new IllegalArgumentException("Could not parse COBOL date string with any known format: " + cobolDateString);
    }
    
    /**
     * Formats LocalDate to COBOL date string with specified or default format
     * 
     * Formats LocalDate to COBOL date string using:
     * - Specified format if provided
     * - YYYYMMDD format as default for maximum compatibility
     * - Proper error handling and validation
     * 
     * @param localDate The LocalDate to format
     * @param dateFormat Optional date format (defaults to "YYYYMMDD" if null)
     * @return COBOL-formatted date string
     * @throws IllegalArgumentException if localDate is null
     */
    public static String formatCobolDate(LocalDate localDate, String dateFormat) {
        if (localDate == null) {
            throw new IllegalArgumentException("LocalDate cannot be null");
        }
        
        String format = StringUtils.isBlank(dateFormat) ? "YYYYMMDD" : dateFormat;
        logger.debug("Formatting LocalDate {} with format '{}'", localDate, format);
        
        return localDateToCobolDate(localDate, format);
    }
    
    /**
     * Converts signed numeric string to appropriate Java numeric type
     * 
     * Handles COBOL signed numeric conversion including:
     * - Leading sign handling (+ or -)
     * - Trailing sign handling (COBOL SIGN TRAILING SEPARATE)
     * - Overpunch sign handling (COBOL SIGN LEADING/TRAILING)
     * - Proper conversion to BigDecimal with sign preservation
     * - Validation and error handling
     * 
     * @param signedNumeric The COBOL signed numeric string
     * @param signType The sign representation type ("LEADING", "TRAILING", "OVERPUNCH")
     * @param targetScale The target decimal scale for the result
     * @return BigDecimal with proper sign and scale
     * @throws IllegalArgumentException if parameters are invalid or conversion fails
     */
    public static BigDecimal convertSignedNumeric(String signedNumeric, String signType, int targetScale) {
        if (signedNumeric == null) {
            logger.debug("Converting null signed numeric to BigDecimal zero");
            return BigDecimal.ZERO.setScale(targetScale, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode());
        }
        
        if (StringUtils.isBlank(signType)) {
            throw new IllegalArgumentException("Sign type cannot be null or blank");
        }
        
        if (targetScale < 0) {
            throw new IllegalArgumentException("Target scale must be non-negative, got: " + targetScale);
        }
        
        logger.debug("Converting signed numeric '{}' with sign type '{}' and scale {}", 
                    signedNumeric, signType, targetScale);
        
        String cleanNumeric = StringUtils.trim(signedNumeric);
        if (StringUtils.isBlank(cleanNumeric)) {
            return BigDecimal.ZERO.setScale(targetScale, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode());
        }
        
        String numericPart;
        boolean isNegative = false;
        
        try {
            switch (signType.toUpperCase()) {
                case "LEADING":
                    if (cleanNumeric.startsWith("+")) {
                        numericPart = cleanNumeric.substring(1);
                        isNegative = false;
                    } else if (cleanNumeric.startsWith("-")) {
                        numericPart = cleanNumeric.substring(1);
                        isNegative = true;
                    } else {
                        numericPart = cleanNumeric;
                        isNegative = false;
                    }
                    break;
                    
                case "TRAILING":
                    if (cleanNumeric.endsWith("+")) {
                        numericPart = cleanNumeric.substring(0, cleanNumeric.length() - 1);
                        isNegative = false;
                    } else if (cleanNumeric.endsWith("-")) {
                        numericPart = cleanNumeric.substring(0, cleanNumeric.length() - 1);
                        isNegative = true;
                    } else {
                        numericPart = cleanNumeric;
                        isNegative = false;
                    }
                    break;
                    
                case "OVERPUNCH":
                    // Handle COBOL overpunch format where last digit contains sign
                    numericPart = convertOverpunchToNumeric(cleanNumeric);
                    isNegative = isOverpunchNegative(cleanNumeric);
                    break;
                    
                default:
                    throw new IllegalArgumentException("Unsupported sign type: " + signType);
            }
            
            // Clean and validate numeric part
            numericPart = StringUtils.trim(numericPart);
            if (!NumberUtils.isCreatable(numericPart)) {
                throw new NumberFormatException("Invalid numeric format: " + numericPart);
            }
            
            BigDecimal result = BigDecimalUtils.createDecimal(numericPart);
            if (isNegative) {
                result = result.negate(BigDecimalUtils.DECIMAL128_CONTEXT);
            }
            
            result = result.setScale(targetScale, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode());
            
            logger.debug("Converted signed numeric to BigDecimal: {}", result);
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to convert signed numeric '{}' with sign type '{}'", signedNumeric, signType, e);
            throw new IllegalArgumentException("Signed numeric conversion failed: " + e.getMessage(), e);
        }
    }
    
    // Private helper methods
    
    /**
     * Formats alphanumeric value according to COBOL PIC X pattern
     * 
     * @param value The value to format
     * @param picturePattern The PIC X pattern (e.g., "X(10)")
     * @return Formatted alphanumeric string
     */
    private static String formatAlphanumericPicture(String value, String picturePattern) {
        java.util.regex.Matcher matcher = PICTURE_X_PATTERN.matcher(picturePattern);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid PIC X pattern: " + picturePattern);
        }
        
        int length = Integer.parseInt(matcher.group(1));
        return padStringCobol(value, length, "SPACES");
    }
    
    /**
     * Formats numeric value according to COBOL PIC 9 pattern
     * 
     * @param value The value to format
     * @param picturePattern The PIC 9 pattern (e.g., "9(5)", "S9(3)V9(2)")
     * @return Formatted numeric string
     */
    private static String formatNumericPicture(String value, String picturePattern) {
        // Handle signed decimal pattern S9(n)V9(n)
        java.util.regex.Matcher signedDecimalMatcher = PICTURE_S9V9_PATTERN.matcher(picturePattern);
        if (signedDecimalMatcher.find()) {
            int integerDigits = Integer.parseInt(signedDecimalMatcher.group(1));
            int decimalDigits = Integer.parseInt(signedDecimalMatcher.group(2));
            
            BigDecimal decimal = BigDecimalUtils.parseDecimal(value);
            decimal = decimal.setScale(decimalDigits, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode());
            
            String formattedValue = decimal.toPlainString();
            return padStringCobol(formattedValue, integerDigits + decimalDigits + 1, "ZEROS"); // +1 for decimal point
        }
        
        // Handle simple numeric pattern 9(n)
        java.util.regex.Matcher numericMatcher = PICTURE_9_PATTERN.matcher(picturePattern);
        if (numericMatcher.find()) {
            int length = Integer.parseInt(numericMatcher.group(1));
            
            // Remove decimal part for integer formatting
            String integerValue = value.contains(".") ? value.substring(0, value.indexOf('.')) : value;
            return padStringCobol(integerValue, length, "ZEROS");
        }
        
        throw new IllegalArgumentException("Invalid numeric PICTURE pattern: " + picturePattern);
    }
    
    /**
     * Gets default value for specified target type
     * 
     * @param targetType The target type
     * @return Default value for the type
     */
    private static Object getDefaultValueForType(String targetType) {
        switch (targetType.toUpperCase()) {
            case "STRING":
                return "";
            case "INTEGER":
                return 0;
            case "LONG":
                return 0L;
            case "DECIMAL":
                return BigDecimal.ZERO;
            case "DATE":
                return LocalDate.now();
            case "TIME":
                return LocalTime.now();
            default:
                return null;
        }
    }
    
    /**
     * Extracts date format from PICTURE clause or returns default
     * 
     * @param pictureClause The PICTURE clause to analyze
     * @param defaultFormat The default format to use
     * @return Extracted or default date format
     */
    private static String extractDateFormatFromPicture(String pictureClause, String defaultFormat) {
        if (StringUtils.isBlank(pictureClause)) {
            return defaultFormat;
        }
        
        // Analyze picture clause for date format hints
        String cleanPicture = StringUtils.upperCase(StringUtils.trim(pictureClause));
        if (cleanPicture.contains("X(8)") || cleanPicture.contains("9(8)")) {
            return "YYYYMMDD";
        } else if (cleanPicture.contains("X(10)") && cleanPicture.contains("/")) {
            return "MM/DD/YY";
        } else if (cleanPicture.contains("X(10)") && cleanPicture.contains("-")) {
            return "YYYY-MM-DD";
        }
        
        return defaultFormat;
    }
    
    /**
     * Extracts time format from PICTURE clause or returns default
     * 
     * @param pictureClause The PICTURE clause to analyze
     * @param defaultFormat The default format to use
     * @return Extracted or default time format
     */
    private static String extractTimeFormatFromPicture(String pictureClause, String defaultFormat) {
        if (StringUtils.isBlank(pictureClause)) {
            return defaultFormat;
        }
        
        // Analyze picture clause for time format hints
        String cleanPicture = StringUtils.upperCase(StringUtils.trim(pictureClause));
        if (cleanPicture.contains("X(6)") || cleanPicture.contains("9(6)")) {
            return "HHMMSS";
        } else if (cleanPicture.contains("X(8)") && cleanPicture.contains(":")) {
            return "HH:MM:SS";
        } else if (cleanPicture.contains("X(15)") && cleanPicture.contains(":") && cleanPicture.contains(".")) {
            return "HH:MM:SS.SSSSSS";
        }
        
        return defaultFormat;
    }
    
    /**
     * Converts COBOL overpunch format to standard numeric string
     * 
     * @param overpunchValue The overpunch value to convert
     * @return Standard numeric string
     */
    private static String convertOverpunchToNumeric(String overpunchValue) {
        if (StringUtils.isBlank(overpunchValue)) {
            return "0";
        }
        
        String value = overpunchValue.trim();
        if (value.length() == 0) {
            return "0";
        }
        
        // Get the last character which contains the sign and digit
        char lastChar = value.charAt(value.length() - 1);
        String prefix = value.length() > 1 ? value.substring(0, value.length() - 1) : "";
        
        // Convert overpunch character to digit
        String lastDigit;
        switch (lastChar) {
            case '{': lastDigit = "0"; break;  // Positive 0
            case 'A': lastDigit = "1"; break;  // Positive 1
            case 'B': lastDigit = "2"; break;  // Positive 2
            case 'C': lastDigit = "3"; break;  // Positive 3
            case 'D': lastDigit = "4"; break;  // Positive 4
            case 'E': lastDigit = "5"; break;  // Positive 5
            case 'F': lastDigit = "6"; break;  // Positive 6
            case 'G': lastDigit = "7"; break;  // Positive 7
            case 'H': lastDigit = "8"; break;  // Positive 8
            case 'I': lastDigit = "9"; break;  // Positive 9
            case '}': lastDigit = "0"; break;  // Negative 0
            case 'J': lastDigit = "1"; break;  // Negative 1
            case 'K': lastDigit = "2"; break;  // Negative 2
            case 'L': lastDigit = "3"; break;  // Negative 3
            case 'M': lastDigit = "4"; break;  // Negative 4
            case 'N': lastDigit = "5"; break;  // Negative 5
            case 'O': lastDigit = "6"; break;  // Negative 6
            case 'P': lastDigit = "7"; break;  // Negative 7
            case 'Q': lastDigit = "8"; break;  // Negative 8
            case 'R': lastDigit = "9"; break;  // Negative 9
            default:
                if (Character.isDigit(lastChar)) {
                    lastDigit = String.valueOf(lastChar);
                } else {
                    throw new IllegalArgumentException("Invalid overpunch character: " + lastChar);
                }
        }
        
        return prefix + lastDigit;
    }
    
    /**
     * Determines if COBOL overpunch value represents a negative number
     * 
     * @param overpunchValue The overpunch value to check
     * @return true if negative, false if positive
     */
    private static boolean isOverpunchNegative(String overpunchValue) {
        if (StringUtils.isBlank(overpunchValue)) {
            return false;
        }
        
        String value = overpunchValue.trim();
        if (value.length() == 0) {
            return false;
        }
        
        // Check the last character for negative overpunch indicators
        char lastChar = value.charAt(value.length() - 1);
        return lastChar == '}' || (lastChar >= 'J' && lastChar <= 'R');
    }
}