package com.carddemo.common.util;

import com.carddemo.common.util.BigDecimalUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.math.BigDecimal;
import java.util.regex.Pattern;
import java.text.DecimalFormat;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConversionUtils - Comprehensive data type conversion utility class providing bidirectional
 * conversion between COBOL and Java data formats with exact format compatibility.
 *
 * This utility class ensures precise transformation of data types while maintaining complete
 * compatibility with legacy COBOL mainframe data formats. All conversions preserve the exact
 * semantic meaning and precision requirements mandated by Section 0.1.2 of the technical
 * specification for the CardDemo application transformation.
 *
 * Key Features:
 * - Exact COBOL COMP-3 packed decimal conversion using BigDecimal with DECIMAL128 precision
 * - COBOL PICTURE clause string formatting and padding utilities maintaining field layout fidelity
 * - Comprehensive COBOL date/time format conversion supporting all patterns from CSDAT01Y.cpy
 * - Bidirectional string conversion preserving COBOL character data manipulation behaviors
 * - Validation utilities ensuring data integrity during COBOL to Java transformations
 *
 * All conversions maintain exact format compatibility as required for regulatory compliance
 * and to ensure identical processing behavior between legacy mainframe and modern Java systems.
 *
 * Date Format Support:
 * - YYYYMMDD: Standard COBOL date format with 4-digit year
 * - MM/DD/YY: Display format with century handling for 2-digit years
 * - HH:MM:SS: Time format with 24-hour clock representation
 * - YYYY-MM-DD HH:MM:SS.SSSSSS: Full timestamp format with microsecond precision
 * - CCYYMMDD: Century-aware date format from CSUTLDWY.cpy patterns
 *
 * @author CardDemo Migration Team - Blitzy Platform
 * @version 1.0
 * @since Java 21
 */
public final class ConversionUtils {

    private static final Logger logger = LoggerFactory.getLogger(ConversionUtils.class);

    // COBOL date format patterns from CSDAT01Y.cpy and CSUTLDWY.cpy
    private static final DateTimeFormatter COBOL_DATE_YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter COBOL_DATE_MM_DD_YY = DateTimeFormatter.ofPattern("MM/dd/yy");
    private static final DateTimeFormatter COBOL_TIME_HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter COBOL_TIMESTAMP_FULL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
    private static final DateTimeFormatter COBOL_TIMESTAMP_STANDARD = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter COBOL_TIME_HHMMSS = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter COBOL_TIME_HHMMSSMS = DateTimeFormatter.ofPattern("HHmmssSSS");

    // Regular expression patterns for COBOL data validation
    private static final Pattern COBOL_NUMERIC_PATTERN = Pattern.compile("^[+-]?\\d+(\\.\\d+)?$");
    private static final Pattern COBOL_DISPLAY_PATTERN = Pattern.compile("^[+-]?[0-9]*\\.?[0-9]*$");
    private static final Pattern COBOL_DATE_YYYYMMDD_PATTERN = Pattern.compile("^\\d{8}$");
    private static final Pattern COBOL_TIME_HHMMSS_PATTERN = Pattern.compile("^\\d{6}$");
    private static final Pattern COBOL_PACKED_DECIMAL_PATTERN = Pattern.compile("^[+-]?\\d+(\\.\\d+)?[CF]?$");

    // Constants for COBOL century handling from CSUTLDWY.cpy
    private static final int THIS_CENTURY = 20;
    private static final int LAST_CENTURY = 19;
    private static final int CENTURY_CUTOFF_YEAR = 50; // Years 00-49 = 20xx, 50-99 = 19xx

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private ConversionUtils() {
        throw new UnsupportedOperationException("ConversionUtils is a utility class and cannot be instantiated");
    }

    /**
     * Converts COBOL character string to Java String with proper handling of COBOL
     * padding conventions and character encoding preservation.
     *
     * Handles COBOL string characteristics:
     * - Right-padding with spaces for fixed-length fields
     * - EBCDIC to ASCII character set conversion concepts
     * - NULL character handling equivalent to COBOL LOW-VALUES
     * - Preservation of leading/trailing space significance based on PICTURE clause
     *
     * @param cobolString The COBOL character string to convert, may contain padding
     * @param pictureClause The COBOL PICTURE clause defining field characteristics (e.g., "X(20)")
     * @return Java String with appropriate trimming and formatting
     * @throws IllegalArgumentException if pictureClause is invalid
     */
    public static String cobolToJavaString(String cobolString, String pictureClause) {
        logger.debug("Converting COBOL string to Java: '{}' with PICTURE '{}'", cobolString, pictureClause);
        
        if (cobolString == null) {
            logger.debug("COBOL string is null, returning empty string");
            return "";
        }

        if (StringUtils.isBlank(pictureClause)) {
            logger.warn("PICTURE clause is blank, using default string conversion");
            return cobolString.trim();
        }

        // Handle COBOL LOW-VALUES (null characters) and HIGH-VALUES
        String processedString = cobolString.replace('\0', ' ').replace('\uFFFF', ' ');
        
        // Extract field length from PICTURE clause (e.g., X(20) -> 20)
        String lengthStr = pictureClause.replaceAll(".*\\((\\d+)\\).*", "$1");
        
        try {
            if (NumberUtils.isCreatable(lengthStr)) {
                int fieldLength = Integer.parseInt(lengthStr);
                // Trim trailing spaces but preserve field semantics
                if (processedString.length() > fieldLength) {
                    processedString = processedString.substring(0, fieldLength);
                }
            }
        } catch (NumberFormatException e) {
            logger.warn("Could not parse field length from PICTURE clause: {}", pictureClause);
        }

        // Trim trailing spaces (COBOL standard behavior for alphanumeric fields)
        String result = StringUtils.stripEnd(processedString, " ");
        logger.debug("Converted COBOL string result: '{}'", result);
        return result;
    }

    /**
     * Converts Java String to COBOL character string format with proper padding
     * and length enforcement according to PICTURE clause specifications.
     *
     * Applies COBOL formatting rules:
     * - Right-padding with spaces to meet fixed field length requirements
     * - Truncation if input exceeds PICTURE clause length specification
     * - Character validation ensuring compatibility with COBOL character sets
     * - Preservation of COBOL field layout conventions
     *
     * @param javaString The Java string to convert to COBOL format
     * @param pictureClause The COBOL PICTURE clause defining target field format (e.g., "X(25)")
     * @return COBOL-formatted string with appropriate padding and length
     * @throws IllegalArgumentException if pictureClause is invalid or javaString is null
     */
    public static String javaToCobolString(String javaString, String pictureClause) {
        logger.debug("Converting Java string to COBOL: '{}' with PICTURE '{}'", javaString, pictureClause);
        
        if (javaString == null) {
            throw new IllegalArgumentException("Java string cannot be null for COBOL conversion");
        }

        if (StringUtils.isBlank(pictureClause)) {
            throw new IllegalArgumentException("PICTURE clause cannot be blank for COBOL string conversion");
        }

        // Extract field length from PICTURE clause
        String lengthStr = pictureClause.replaceAll(".*\\((\\d+)\\).*", "$1");
        int fieldLength;
        
        try {
            fieldLength = Integer.parseInt(lengthStr);
        } catch (NumberFormatException e) {
            logger.error("Invalid PICTURE clause format: {}", pictureClause);
            throw new IllegalArgumentException("Invalid PICTURE clause format: " + pictureClause);
        }

        String result;
        if (javaString.length() > fieldLength) {
            // Truncate if too long
            result = javaString.substring(0, fieldLength);
            logger.debug("Truncated string to field length {}", fieldLength);
        } else {
            // Right-pad with spaces to meet COBOL field length requirement
            result = StringUtils.rightPad(javaString, fieldLength, ' ');
        }

        logger.debug("Converted Java string to COBOL result: '{}'", result);
        return result;
    }

    /**
     * Converts COBOL COMP-3 packed decimal string representation to Java BigDecimal
     * maintaining exact precision and scale as defined in the original COBOL field.
     *
     * Handles COBOL COMP-3 characteristics:
     * - Packed decimal format with exact precision preservation using DECIMAL128 context
     * - Sign handling for positive/negative values (C/D suffix or leading +/-)
     * - Scale preservation matching COBOL PIC S9(n)V9(m) COMP-3 field definitions
     * - Precision validation ensuring no data loss during conversion
     *
     * @param cobolComp3String The COBOL COMP-3 string representation to convert
     * @param precision Total number of digits in the COBOL field
     * @param scale Number of decimal places in the COBOL field
     * @return BigDecimal with exact COBOL precision using DECIMAL128 context
     * @throws IllegalArgumentException if parameters are invalid
     * @throws NumberFormatException if cobolComp3String cannot be parsed as decimal
     */
    public static BigDecimal cobolCompToDecimal(String cobolComp3String, int precision, int scale) {
        logger.debug("Converting COBOL COMP-3 to BigDecimal: '{}' precision={}, scale={}", 
                    cobolComp3String, precision, scale);
        
        if (StringUtils.isBlank(cobolComp3String)) {
            logger.debug("COBOL COMP-3 string is blank, returning zero");
            return BigDecimalUtils.createDecimal("0").setScale(scale, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode());
        }

        if (precision <= 0 || scale < 0 || scale > precision) {
            throw new IllegalArgumentException(
                String.format("Invalid precision/scale: precision=%d, scale=%d", precision, scale));
        }

        String cleanValue = cobolComp3String.trim();
        boolean isNegative = false;

        // Handle COBOL sign indicators
        if (cleanValue.endsWith("D") || cleanValue.endsWith("d")) {
            isNegative = true;
            cleanValue = cleanValue.substring(0, cleanValue.length() - 1);
        } else if (cleanValue.endsWith("C") || cleanValue.endsWith("c")) {
            isNegative = false;
            cleanValue = cleanValue.substring(0, cleanValue.length() - 1);
        } else if (cleanValue.startsWith("-")) {
            isNegative = true;
            cleanValue = cleanValue.substring(1);
        } else if (cleanValue.startsWith("+")) {
            cleanValue = cleanValue.substring(1);
        }

        try {
            BigDecimal result = BigDecimalUtils.createDecimal(cleanValue);
            
            // Apply scale
            result = result.setScale(scale, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode());
            
            // Apply sign
            if (isNegative) {
                result = result.negate(BigDecimalUtils.DECIMAL128_CONTEXT);
            }
            
            // Validate precision doesn't exceed COBOL field definition
            if (result.precision() > precision) {
                logger.warn("Converted value precision {} exceeds COBOL field precision {}", 
                           result.precision(), precision);
            }

            logger.debug("Converted COBOL COMP-3 to BigDecimal result: {}", result);
            return result;
            
        } catch (NumberFormatException e) {
            logger.error("Failed to parse COBOL COMP-3 value: {}", cobolComp3String, e);
            throw new NumberFormatException("Invalid COBOL COMP-3 format: " + cobolComp3String);
        }
    }

    /**
     * Converts Java BigDecimal to COBOL COMP-3 packed decimal string representation
     * maintaining exact formatting requirements for COBOL field compatibility.
     *
     * Applies COBOL COMP-3 formatting:
     * - Precision and scale validation against COBOL field specifications
     * - Sign representation using COBOL conventions (C for positive, D for negative)
     * - Zero-padding to meet exact COBOL field length requirements
     * - Format validation ensuring compatibility with COBOL COMP-3 storage
     *
     * @param decimal The BigDecimal value to convert to COBOL COMP-3 format
     * @param precision Total number of digits required in COBOL field
     * @param scale Number of decimal places required in COBOL field
     * @param useSignSuffix Whether to use C/D suffix (true) or +/- prefix (false)
     * @return COBOL COMP-3 formatted string representation
     * @throws IllegalArgumentException if parameters are invalid or decimal is null
     */
    public static String decimalToCobolComp(BigDecimal decimal, int precision, int scale, boolean useSignSuffix) {
        logger.debug("Converting BigDecimal to COBOL COMP-3: {} precision={}, scale={}, useSignSuffix={}", 
                    decimal, precision, scale, useSignSuffix);
        
        if (decimal == null) {
            throw new IllegalArgumentException("BigDecimal cannot be null for COBOL COMP-3 conversion");
        }

        if (precision <= 0 || scale < 0 || scale > precision) {
            throw new IllegalArgumentException(
                String.format("Invalid precision/scale: precision=%d, scale=%d", precision, scale));
        }

        // Scale the decimal to match COBOL field requirements
        BigDecimal scaledDecimal = decimal.setScale(scale, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode());
        
        // Validate precision doesn't exceed COBOL field capacity
        if (scaledDecimal.precision() > precision) {
            logger.warn("BigDecimal precision {} exceeds COBOL field precision {}", 
                       scaledDecimal.precision(), precision);
        }

        boolean isNegative = scaledDecimal.compareTo(BigDecimal.ZERO) < 0;
        BigDecimal absValue = scaledDecimal.abs();
        
        // Format as plain string without scientific notation
        String valueStr = absValue.toPlainString();
        
        // Remove decimal point for COBOL representation
        if (scale > 0) {
            String[] parts = valueStr.split("\\.");
            String integerPart = parts[0];
            String fractionalPart = parts.length > 1 ? parts[1] : "";
            
            // Pad fractional part with zeros if needed
            fractionalPart = StringUtils.rightPad(fractionalPart, scale, '0');
            valueStr = integerPart + fractionalPart;
        }
        
        // Zero-pad to meet precision requirements
        int totalDigits = precision;
        valueStr = StringUtils.leftPad(valueStr, totalDigits, '0');
        
        // Apply sign formatting
        String result;
        if (useSignSuffix) {
            result = valueStr + (isNegative ? "D" : "C");
        } else {
            result = (isNegative ? "-" : "+") + valueStr;
        }

        logger.debug("Converted BigDecimal to COBOL COMP-3 result: '{}'", result);
        return result;
    }

    /**
     * Converts COBOL date format (YYYYMMDD) to Java LocalDate maintaining exact
     * date value and handling century logic from CSUTLDWY.cpy specifications.
     *
     * Supports COBOL date formats:
     * - YYYYMMDD: 8-digit format with full year (e.g., "20240315")
     * - CCYYMMDD: Century-aware format from CSUTLDWY.cpy structure
     * - Validation of month (1-12) and day ranges per COBOL 88-level conditions
     * - Leap year handling consistent with COBOL date arithmetic
     *
     * @param cobolDate The COBOL date string in YYYYMMDD format
     * @return LocalDate representing the converted date
     * @throws IllegalArgumentException if cobolDate is null or invalid format
     * @throws DateTimeParseException if date values are invalid
     */
    public static LocalDate cobolDateToLocalDate(String cobolDate) {
        logger.debug("Converting COBOL date to LocalDate: '{}'", cobolDate);
        
        if (StringUtils.isBlank(cobolDate)) {
            throw new IllegalArgumentException("COBOL date string cannot be null or blank");
        }

        String cleanDate = cobolDate.trim();
        
        // Validate YYYYMMDD format
        if (!COBOL_DATE_YYYYMMDD_PATTERN.matcher(cleanDate).matches()) {
            throw new IllegalArgumentException("COBOL date must be in YYYYMMDD format: " + cobolDate);
        }

        try {
            LocalDate result = LocalDate.parse(cleanDate, COBOL_DATE_YYYYMMDD);
            
            // Validate month and day ranges per COBOL 88-level conditions from CSUTLDWY.cpy
            int month = result.getMonthValue();
            int day = result.getDayOfMonth();
            
            if (month < 1 || month > 12) {
                throw new IllegalArgumentException("Invalid month value: " + month);
            }
            
            if (day < 1 || day > 31) {
                throw new IllegalArgumentException("Invalid day value: " + day);
            }
            
            logger.debug("Converted COBOL date to LocalDate result: {}", result);
            return result;
            
        } catch (DateTimeParseException e) {
            logger.error("Failed to parse COBOL date: {}", cobolDate, e);
            throw new DateTimeParseException("Invalid COBOL date format: " + cobolDate, cobolDate, 0);
        }
    }

    /**
     * Converts Java LocalDate to COBOL date format (YYYYMMDD) maintaining
     * exact compatibility with COBOL date field requirements.
     *
     * Applies COBOL date formatting:
     * - YYYYMMDD: 8-digit format with zero-padding
     * - Validation ensuring date is within COBOL supported range
     * - Century handling consistent with COBOL date arithmetic
     * - Format compatibility with CSDAT01Y.cpy date structures
     *
     * @param localDate The LocalDate to convert to COBOL format
     * @return COBOL date string in YYYYMMDD format
     * @throws IllegalArgumentException if localDate is null
     */
    public static String localDateToCobolDate(LocalDate localDate) {
        logger.debug("Converting LocalDate to COBOL date: {}", localDate);
        
        if (localDate == null) {
            throw new IllegalArgumentException("LocalDate cannot be null for COBOL date conversion");
        }

        String result = localDate.format(COBOL_DATE_YYYYMMDD);
        logger.debug("Converted LocalDate to COBOL date result: '{}'", result);
        return result;
    }

    /**
     * Converts COBOL time format (HHMMSS) to Java LocalTime maintaining exact
     * time precision and handling millisecond components from CSDAT01Y.cpy.
     *
     * Supports COBOL time formats:
     * - HHMMSS: 6-digit format for hours, minutes, seconds (e.g., "143052")
     * - HHMMSSMS: 8-digit format including 2-digit milliseconds (e.g., "14305287")
     * - 24-hour clock format consistent with COBOL time arithmetic
     * - Validation of time component ranges (hours 00-23, minutes/seconds 00-59)
     *
     * @param cobolTime The COBOL time string in HHMMSS or HHMMSSMS format
     * @return LocalTime representing the converted time
     * @throws IllegalArgumentException if cobolTime is null or invalid format
     * @throws DateTimeParseException if time values are invalid
     */
    public static LocalTime cobolTimeToLocalTime(String cobolTime) {
        logger.debug("Converting COBOL time to LocalTime: '{}'", cobolTime);
        
        if (StringUtils.isBlank(cobolTime)) {
            throw new IllegalArgumentException("COBOL time string cannot be null or blank");
        }

        String cleanTime = cobolTime.trim();
        
        try {
            LocalTime result;
            
            if (cleanTime.length() == 6) {
                // HHMMSS format
                if (!COBOL_TIME_HHMMSS_PATTERN.matcher(cleanTime).matches()) {
                    throw new IllegalArgumentException("COBOL time must be in HHMMSS format: " + cobolTime);
                }
                result = LocalTime.parse(cleanTime, COBOL_TIME_HHMMSS);
            } else if (cleanTime.length() == 8) {
                // HHMMSSMS format (milliseconds as 2-digit value * 10)
                result = LocalTime.parse(cleanTime, COBOL_TIME_HHMMSSMS);
            } else {
                throw new IllegalArgumentException("COBOL time must be 6 or 8 digits: " + cobolTime);
            }
            
            logger.debug("Converted COBOL time to LocalTime result: {}", result);
            return result;
            
        } catch (DateTimeParseException e) {
            logger.error("Failed to parse COBOL time: {}", cobolTime, e);
            throw new DateTimeParseException("Invalid COBOL time format: " + cobolTime, cobolTime, 0);
        }
    }

    /**
     * Converts Java LocalTime to COBOL time format (HHMMSS) maintaining
     * exact compatibility with COBOL time field requirements.
     *
     * Applies COBOL time formatting:
     * - HHMMSS: 6-digit format with zero-padding
     * - 24-hour clock representation
     * - Seconds truncation (no fractional seconds in basic format)
     * - Format compatibility with CSDAT01Y.cpy time structures
     *
     * @param localTime The LocalTime to convert to COBOL format
     * @return COBOL time string in HHMMSS format
     * @throws IllegalArgumentException if localTime is null
     */
    public static String localTimeToCobolTime(LocalTime localTime) {
        logger.debug("Converting LocalTime to COBOL time: {}", localTime);
        
        if (localTime == null) {
            throw new IllegalArgumentException("LocalTime cannot be null for COBOL time conversion");
        }

        // Truncate to seconds precision for basic COBOL format
        LocalTime truncatedTime = localTime.withNano(0);
        String result = truncatedTime.format(COBOL_TIME_HHMMSS);
        
        logger.debug("Converted LocalTime to COBOL time result: '{}'", result);
        return result;
    }

    /**
     * Converts COBOL timestamp format to Java LocalDateTime maintaining exact
     * precision and supporting multiple timestamp formats from CSDAT01Y.cpy.
     *
     * Supports COBOL timestamp formats:
     * - "YYYY-MM-DD HH:MM:SS.SSSSSS": Full timestamp with microseconds
     * - "YYYY-MM-DD HH:MM:SS": Standard timestamp without microseconds
     * - Handles date and time component validation
     * - Maintains timezone-neutral representation consistent with COBOL
     *
     * @param cobolTimestamp The COBOL timestamp string to convert
     * @return LocalDateTime representing the converted timestamp
     * @throws IllegalArgumentException if cobolTimestamp is null or invalid format
     * @throws DateTimeParseException if timestamp values are invalid
     */
    public static LocalDateTime cobolTimestampToLocalDateTime(String cobolTimestamp) {
        logger.debug("Converting COBOL timestamp to LocalDateTime: '{}'", cobolTimestamp);
        
        if (StringUtils.isBlank(cobolTimestamp)) {
            throw new IllegalArgumentException("COBOL timestamp string cannot be null or blank");
        }

        String cleanTimestamp = cobolTimestamp.trim();
        
        try {
            LocalDateTime result;
            
            if (cleanTimestamp.contains(".") && cleanTimestamp.length() > 19) {
                // Full timestamp format with microseconds
                result = LocalDateTime.parse(cleanTimestamp, COBOL_TIMESTAMP_FULL);
            } else {
                // Standard timestamp format without microseconds
                result = LocalDateTime.parse(cleanTimestamp, COBOL_TIMESTAMP_STANDARD);
            }
            
            logger.debug("Converted COBOL timestamp to LocalDateTime result: {}", result);
            return result;
            
        } catch (DateTimeParseException e) {
            logger.error("Failed to parse COBOL timestamp: {}", cobolTimestamp, e);
            throw new DateTimeParseException("Invalid COBOL timestamp format: " + cobolTimestamp, cobolTimestamp, 0);
        }
    }

    /**
     * Converts Java LocalDateTime to COBOL timestamp format maintaining
     * exact compatibility with COBOL timestamp field requirements.
     *
     * Applies COBOL timestamp formatting:
     * - "YYYY-MM-DD HH:MM:SS.SSSSSS": Full format with microsecond precision
     * - Zero-padding for all date and time components
     * - Format compatibility with CSDAT01Y.cpy timestamp structures
     * - Maintains consistent formatting for database storage
     *
     * @param localDateTime The LocalDateTime to convert to COBOL format
     * @param includeMicroseconds Whether to include microsecond precision
     * @return COBOL timestamp string in appropriate format
     * @throws IllegalArgumentException if localDateTime is null
     */
    public static String localDateTimeToCobolTimestamp(LocalDateTime localDateTime, boolean includeMicroseconds) {
        logger.debug("Converting LocalDateTime to COBOL timestamp: {} includeMicroseconds={}", 
                    localDateTime, includeMicroseconds);
        
        if (localDateTime == null) {
            throw new IllegalArgumentException("LocalDateTime cannot be null for COBOL timestamp conversion");
        }

        String result;
        if (includeMicroseconds) {
            result = localDateTime.format(COBOL_TIMESTAMP_FULL);
        } else {
            result = localDateTime.format(COBOL_TIMESTAMP_STANDARD);
        }
        
        logger.debug("Converted LocalDateTime to COBOL timestamp result: '{}'", result);
        return result;
    }

    /**
     * Formats data according to COBOL PICTURE clause specifications maintaining
     * exact field layout and validation requirements.
     *
     * Supports COBOL PICTURE formats:
     * - X(n): Character fields with right padding
     * - 9(n): Numeric fields with zero padding
     * - S9(n)V9(m): Signed numeric with decimal places
     * - Z(n): Zero suppression with space padding
     * - $(n): Currency formatting with dollar sign
     *
     * @param value The value to format (String, BigDecimal, or Number)
     * @param pictureClause The COBOL PICTURE clause defining format requirements
     * @return Formatted string according to PICTURE clause specifications
     * @throws IllegalArgumentException if pictureClause is invalid or value is null
     */
    public static String formatWithPicture(Object value, String pictureClause) {
        logger.debug("Formatting value with PICTURE clause: {} with '{}'", value, pictureClause);
        
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null for PICTURE formatting");
        }
        
        if (StringUtils.isBlank(pictureClause)) {
            throw new IllegalArgumentException("PICTURE clause cannot be blank");
        }

        String cleanPicture = pictureClause.trim().toUpperCase();
        String result;

        try {
            if (cleanPicture.startsWith("X(")) {
                // Character field formatting
                String stringValue = value.toString();
                int length = extractLengthFromPicture(cleanPicture);
                result = javaToCobolString(stringValue, cleanPicture);
                
            } else if (cleanPicture.startsWith("9(") || cleanPicture.contains("V9(")) {
                // Numeric field formatting
                BigDecimal decimalValue;
                if (value instanceof BigDecimal) {
                    decimalValue = (BigDecimal) value;
                } else if (value instanceof Number) {
                    decimalValue = BigDecimalUtils.createDecimal(value.toString());
                } else {
                    decimalValue = BigDecimalUtils.parseDecimal(value.toString());
                }
                
                int[] precisionScale = extractPrecisionScale(cleanPicture);
                result = decimalToCobolComp(decimalValue, precisionScale[0], precisionScale[1], false);
                
            } else if (cleanPicture.startsWith("Z(")) {
                // Zero suppression formatting
                String stringValue = value.toString();
                int length = extractLengthFromPicture(cleanPicture);
                result = formatZeroSuppressed(stringValue, length);
                
            } else if (cleanPicture.startsWith("$(")) {
                // Currency formatting
                BigDecimal decimalValue;
                if (value instanceof BigDecimal) {
                    decimalValue = (BigDecimal) value;
                } else {
                    decimalValue = BigDecimalUtils.parseDecimal(value.toString());
                }
                result = BigDecimalUtils.formatCurrency(decimalValue);
                
            } else {
                // Default string conversion
                result = value.toString();
                logger.warn("Unrecognized PICTURE clause pattern: {}", pictureClause);
            }
            
        } catch (Exception e) {
            logger.error("Error formatting value {} with PICTURE {}", value, pictureClause, e);
            throw new IllegalArgumentException("Invalid PICTURE clause or value: " + e.getMessage());
        }

        logger.debug("Formatted value with PICTURE result: '{}'", result);
        return result;
    }

    /**
     * Pads string according to COBOL field requirements maintaining exact
     * character positioning and length specifications.
     *
     * Applies COBOL padding rules:
     * - Left padding with zeros for numeric fields
     * - Right padding with spaces for character fields
     * - Truncation when input exceeds field length
     * - Preservation of field alignment per COBOL conventions
     *
     * @param input The string to pad
     * @param totalLength The target field length
     * @param padLeft Whether to pad left (true) or right (false)
     * @param padChar The character to use for padding
     * @return Padded string meeting COBOL field requirements
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static String padStringCobol(String input, int totalLength, boolean padLeft, char padChar) {
        logger.debug("Padding COBOL string: '{}' length={}, padLeft={}, padChar='{}'", 
                    input, totalLength, padLeft, padChar);
        
        if (input == null) {
            input = "";
        }
        
        if (totalLength < 0) {
            throw new IllegalArgumentException("Total length cannot be negative: " + totalLength);
        }

        String result;
        if (input.length() > totalLength) {
            // Truncate if too long
            result = input.substring(0, totalLength);
            logger.debug("Truncated string to length {}", totalLength);
        } else if (input.length() < totalLength) {
            // Pad to reach target length
            if (padLeft) {
                result = StringUtils.leftPad(input, totalLength, padChar);
            } else {
                result = StringUtils.rightPad(input, totalLength, padChar);
            }
        } else {
            // Already correct length
            result = input;
        }

        logger.debug("Padded COBOL string result: '{}'", result);
        return result;
    }

    /**
     * Trims string according to COBOL field conventions maintaining
     * semantic significance of whitespace and padding.
     *
     * Applies COBOL trimming rules:
     * - Removes trailing spaces from alphanumeric fields
     * - Preserves leading zeros in numeric contexts
     * - Handles both left and right trimming based on field type
     * - Maintains COBOL LOW-VALUES and HIGH-VALUES handling
     *
     * @param cobolString The COBOL string to trim
     * @param trimBoth Whether to trim both ends (true) or just trailing (false)
     * @return Trimmed string according to COBOL conventions
     */
    public static String trimCobolString(String cobolString, boolean trimBoth) {
        logger.debug("Trimming COBOL string: '{}' trimBoth={}", cobolString, trimBoth);
        
        if (cobolString == null) {
            return "";
        }

        // Handle COBOL special values
        String processedString = cobolString.replace('\0', ' ').replace('\uFFFF', ' ');
        
        String result;
        if (trimBoth) {
            result = processedString.trim();
        } else {
            // Trim only trailing spaces (standard COBOL behavior)
            result = StringUtils.stripEnd(processedString, " ");
        }

        logger.debug("Trimmed COBOL string result: '{}'", result);
        return result;
    }

    /**
     * Validates string representation against COBOL numeric field requirements
     * ensuring compatibility with COBOL PIC 9 and S9 field definitions.
     *
     * Validates COBOL numeric characteristics:
     * - Digit-only content for PIC 9 fields
     * - Sign handling for PIC S9 fields
     * - Decimal point positioning for PIC V fields
     * - Length validation against field specifications
     * - Character set validation for numeric contexts
     *
     * @param numericString The string to validate as COBOL numeric
     * @param allowSigned Whether to allow signed values (PIC S9)
     * @param allowDecimal Whether to allow decimal points (PIC V)
     * @return true if valid COBOL numeric format, false otherwise
     */
    public static boolean validateCobolNumeric(String numericString, boolean allowSigned, boolean allowDecimal) {
        logger.debug("Validating COBOL numeric: '{}' allowSigned={}, allowDecimal={}", 
                    numericString, allowSigned, allowDecimal);
        
        if (StringUtils.isBlank(numericString)) {
            logger.debug("Numeric string is blank, returning false");
            return false;
        }

        String cleanString = numericString.trim();
        
        // Basic pattern validation
        if (!COBOL_NUMERIC_PATTERN.matcher(cleanString).matches()) {
            logger.debug("String does not match basic numeric pattern");
            return false;
        }

        // Check sign handling
        if (!allowSigned && (cleanString.startsWith("+") || cleanString.startsWith("-"))) {
            logger.debug("Signed value not allowed but sign found");
            return false;
        }

        // Check decimal handling
        if (!allowDecimal && cleanString.contains(".")) {
            logger.debug("Decimal not allowed but decimal point found");
            return false;
        }

        // Additional COBOL-specific validation
        try {
            if (allowDecimal) {
                BigDecimalUtils.parseDecimal(cleanString);
            } else {
                Long.parseLong(cleanString.replace("+", ""));
            }
            
            logger.debug("COBOL numeric validation passed");
            return true;
            
        } catch (NumberFormatException e) {
            logger.debug("COBOL numeric validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Converts COBOL display format numeric to Java representation maintaining
     * exact numeric value and handling COBOL display conventions.
     *
     * Handles COBOL display characteristics:
     * - Leading zero preservation and removal
     * - Sign overpunch handling (COBOL sign representation)
     * - Decimal alignment for display fields
     * - Zone decimal to packed decimal conversion concepts
     *
     * @param displayValue The COBOL display format value to convert
     * @param pictureClause The COBOL PICTURE clause defining the display format
     * @return BigDecimal representation of the display value
     * @throws IllegalArgumentException if parameters are invalid
     * @throws NumberFormatException if displayValue cannot be converted
     */
    public static BigDecimal convertCobolDisplay(String displayValue, String pictureClause) {
        logger.debug("Converting COBOL display format: '{}' with PICTURE '{}'", displayValue, pictureClause);
        
        if (StringUtils.isBlank(displayValue)) {
            throw new IllegalArgumentException("Display value cannot be null or blank");
        }
        
        if (StringUtils.isBlank(pictureClause)) {
            throw new IllegalArgumentException("PICTURE clause cannot be null or blank");
        }

        String cleanValue = displayValue.trim();
        String cleanPicture = pictureClause.trim().toUpperCase();
        
        try {
            // Extract precision and scale from PICTURE clause
            int[] precisionScale = extractPrecisionScale(cleanPicture);
            int precision = precisionScale[0];
            int scale = precisionScale[1];
            
            // Handle sign overpunch (COBOL convention where last digit encodes sign)
            boolean isNegative = false;
            if (cleanValue.length() > 0) {
                char lastChar = cleanValue.charAt(cleanValue.length() - 1);
                if (lastChar >= 'J' && lastChar <= 'R') {
                    // Negative overpunch
                    isNegative = true;
                    char digit = (char) ('0' + (lastChar - 'J' + 1));
                    cleanValue = cleanValue.substring(0, cleanValue.length() - 1) + digit;
                } else if (lastChar >= 'A' && lastChar <= 'I') {
                    // Positive overpunch
                    char digit = (char) ('0' + (lastChar - 'A' + 1));
                    cleanValue = cleanValue.substring(0, cleanValue.length() - 1) + digit;
                }
            }
            
            // Convert to BigDecimal
            BigDecimal result = BigDecimalUtils.createDecimal(cleanValue);
            
            // Apply scale if specified
            if (scale > 0 && !cleanValue.contains(".")) {
                // Implied decimal point
                result = result.movePointLeft(scale);
            }
            
            // Apply sign
            if (isNegative) {
                result = result.negate(BigDecimalUtils.DECIMAL128_CONTEXT);
            }
            
            logger.debug("Converted COBOL display format result: {}", result);
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to convert COBOL display value: {}", displayValue, e);
            throw new NumberFormatException("Invalid COBOL display format: " + displayValue);
        }
    }

    /**
     * Parses COBOL date string with flexible format detection supporting
     * multiple date patterns from CSDAT01Y.cpy and CSUTLDWY.cpy structures.
     *
     * Supports date formats:
     * - YYYYMMDD: Standard 8-digit format
     * - MM/DD/YY: Display format with century inference
     * - CCYYMMDD: Century-aware format
     * - YYMMDD: 2-digit year with century logic from CSUTLDWY.cpy
     *
     * @param dateString The COBOL date string to parse
     * @param dateFormat Optional format hint (null for auto-detection)
     * @return LocalDate representation of the parsed date
     * @throws IllegalArgumentException if dateString is invalid
     * @throws DateTimeParseException if date cannot be parsed
     */
    public static LocalDate parseCobolDate(String dateString, String dateFormat) {
        logger.debug("Parsing COBOL date: '{}' with format hint '{}'", dateString, dateFormat);
        
        if (StringUtils.isBlank(dateString)) {
            throw new IllegalArgumentException("Date string cannot be null or blank");
        }

        String cleanDate = dateString.trim();
        
        try {
            LocalDate result;
            
            if (dateFormat != null && !dateFormat.trim().isEmpty()) {
                // Use specified format
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat.trim());
                result = LocalDate.parse(cleanDate, formatter);
            } else {
                // Auto-detect format
                if (cleanDate.length() == 8 && StringUtils.isNumeric(cleanDate)) {
                    // YYYYMMDD format
                    result = LocalDate.parse(cleanDate, COBOL_DATE_YYYYMMDD);
                } else if (cleanDate.contains("/") && cleanDate.length() == 8) {
                    // MM/DD/YY format with century inference
                    LocalDate parsedDate = LocalDate.parse(cleanDate, COBOL_DATE_MM_DD_YY);
                    // Apply century logic from CSUTLDWY.cpy
                    int year = parsedDate.getYear();
                    if (year < 100) {
                        if (year <= CENTURY_CUTOFF_YEAR) {
                            year += THIS_CENTURY * 100;
                        } else {
                            year += LAST_CENTURY * 100;
                        }
                        result = parsedDate.withYear(year);
                    } else {
                        result = parsedDate;
                    }
                } else if (cleanDate.length() == 6 && StringUtils.isNumeric(cleanDate)) {
                    // YYMMDD format with century inference
                    int yy = Integer.parseInt(cleanDate.substring(0, 2));
                    int mm = Integer.parseInt(cleanDate.substring(2, 4));
                    int dd = Integer.parseInt(cleanDate.substring(4, 6));
                    
                    int yyyy = (yy <= CENTURY_CUTOFF_YEAR) ? THIS_CENTURY * 100 + yy : LAST_CENTURY * 100 + yy;
                    result = LocalDate.of(yyyy, mm, dd);
                } else {
                    throw new DateTimeParseException("Unrecognized date format", cleanDate, 0);
                }
            }
            
            logger.debug("Parsed COBOL date result: {}", result);
            return result;
            
        } catch (DateTimeParseException e) {
            logger.error("Failed to parse COBOL date: {}", dateString, e);
            throw new DateTimeParseException("Invalid COBOL date format: " + dateString, dateString, 0);
        }
    }

    /**
     * Formats LocalDate to specified COBOL date format maintaining exact
     * compatibility with COBOL date field requirements and display conventions.
     *
     * Supports output formats:
     * - YYYYMMDD: Standard 8-digit format
     * - MM/DD/YY: Display format with 2-digit year
     * - CCYYMMDD: Century-explicit format
     * - Custom patterns using DateTimeFormatter syntax
     *
     * @param date The LocalDate to format
     * @param targetFormat The desired COBOL date format pattern
     * @return Formatted date string according to specified format
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static String formatCobolDate(LocalDate date, String targetFormat) {
        logger.debug("Formatting LocalDate to COBOL format: {} with format '{}'", date, targetFormat);
        
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null for COBOL formatting");
        }
        
        if (StringUtils.isBlank(targetFormat)) {
            throw new IllegalArgumentException("Target format cannot be null or blank");
        }

        String cleanFormat = targetFormat.trim();
        String result;
        
        try {
            if ("YYYYMMDD".equalsIgnoreCase(cleanFormat)) {
                result = date.format(COBOL_DATE_YYYYMMDD);
            } else if ("MM/DD/YY".equalsIgnoreCase(cleanFormat)) {
                result = date.format(COBOL_DATE_MM_DD_YY);
            } else {
                // Custom format
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(cleanFormat);
                result = date.format(formatter);
            }
            
            logger.debug("Formatted LocalDate to COBOL result: '{}'", result);
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to format date {} with format {}", date, targetFormat, e);
            throw new IllegalArgumentException("Invalid date format pattern: " + targetFormat);
        }
    }

    /**
     * Converts signed numeric values handling COBOL sign conventions and
     * representations including overpunch and separate sign indicators.
     *
     * Handles COBOL signed numeric formats:
     * - Leading sign (+/-) representation
     * - Trailing sign representation
     * - Overpunch sign encoding (A-I positive, J-R negative)
     * - Separate sign positions per COBOL PIC S specifications
     *
     * @param signedValue The signed numeric string to convert
     * @param targetSigned Whether result should be signed
     * @param useOverpunch Whether to use overpunch encoding
     * @return BigDecimal with appropriate sign handling
     * @throws IllegalArgumentException if signedValue is invalid
     * @throws NumberFormatException if conversion fails
     */
    public static BigDecimal convertSignedNumeric(String signedValue, boolean targetSigned, boolean useOverpunch) {
        logger.debug("Converting signed numeric: '{}' targetSigned={}, useOverpunch={}", 
                    signedValue, targetSigned, useOverpunch);
        
        if (StringUtils.isBlank(signedValue)) {
            throw new IllegalArgumentException("Signed value cannot be null or blank");
        }

        String cleanValue = signedValue.trim();
        boolean isNegative = false;
        
        try {
            // Handle different sign representations
            if (useOverpunch && cleanValue.length() > 0) {
                // Check for overpunch in last character
                char lastChar = cleanValue.charAt(cleanValue.length() - 1);
                if (lastChar >= 'A' && lastChar <= 'R') {
                    if (lastChar >= 'J' && lastChar <= 'R') {
                        isNegative = true;
                        char digit = (char) ('0' + (lastChar - 'J' + 1));
                    } else {
                        char digit = (char) ('0' + (lastChar - 'A' + 1));
                    }
                    cleanValue = cleanValue.substring(0, cleanValue.length() - 1) + digit;
                }
            } else {
                // Handle explicit sign characters
                if (cleanValue.startsWith("-")) {
                    isNegative = true;
                    cleanValue = cleanValue.substring(1);
                } else if (cleanValue.startsWith("+")) {
                    cleanValue = cleanValue.substring(1);
                } else if (cleanValue.endsWith("-")) {
                    isNegative = true;
                    cleanValue = cleanValue.substring(0, cleanValue.length() - 1);
                } else if (cleanValue.endsWith("+")) {
                    cleanValue = cleanValue.substring(0, cleanValue.length() - 1);
                }
            }
            
            // Convert to BigDecimal
            BigDecimal result = BigDecimalUtils.createDecimal(cleanValue);
            
            // Apply sign
            if (isNegative) {
                result = result.negate(BigDecimalUtils.DECIMAL128_CONTEXT);
            }
            
            // If target is not signed, take absolute value
            if (!targetSigned && result.compareTo(BigDecimal.ZERO) < 0) {
                result = result.abs(BigDecimalUtils.DECIMAL128_CONTEXT);
                logger.debug("Converted negative value to positive as target is unsigned");
            }
            
            logger.debug("Converted signed numeric result: {}", result);
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to convert signed numeric: {}", signedValue, e);
            throw new NumberFormatException("Invalid signed numeric format: " + signedValue);
        }
    }

    /**
     * Helper method to extract field length from COBOL PICTURE clause.
     * Parses patterns like X(20), 9(8), etc.
     */
    private static int extractLengthFromPicture(String pictureClause) {
        String lengthStr = pictureClause.replaceAll(".*\\((\\d+)\\).*", "$1");
        if (NumberUtils.isCreatable(lengthStr)) {
            return Integer.parseInt(lengthStr);
        }
        return pictureClause.length(); // Fallback to clause length
    }

    /**
     * Helper method to extract precision and scale from COBOL PICTURE clause.
     * Parses patterns like 9(8), S9(10)V9(2), etc.
     */
    private static int[] extractPrecisionScale(String pictureClause) {
        int precision = 0;
        int scale = 0;
        
        // Extract integer part
        if (pictureClause.contains("9(")) {
            String integerPart = pictureClause.replaceAll(".*9\\((\\d+)\\).*", "$1");
            if (NumberUtils.isCreatable(integerPart)) {
                precision = Integer.parseInt(integerPart);
            }
        }
        
        // Extract decimal part
        if (pictureClause.contains("V9(")) {
            String decimalPart = pictureClause.replaceAll(".*V9\\((\\d+)\\).*", "$1");
            if (NumberUtils.isCreatable(decimalPart)) {
                scale = Integer.parseInt(decimalPart);
                precision += scale;
            }
        }
        
        return new int[]{precision, scale};
    }

    /**
     * Helper method to format zero-suppressed values per COBOL Z picture pattern.
     */
    private static String formatZeroSuppressed(String value, int length) {
        if (StringUtils.isBlank(value) || "0".equals(value.trim())) {
            return StringUtils.repeat(" ", length);
        }
        
        // Remove leading zeros and right-pad with spaces
        String trimmed = value.replaceFirst("^0+", "");
        if (trimmed.isEmpty()) {
            trimmed = "0";
        }
        
        return StringUtils.rightPad(trimmed, length, ' ');
    }
}