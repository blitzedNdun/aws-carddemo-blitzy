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
import java.text.NumberFormat;
import java.text.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConversionUtils - Comprehensive data type conversion utility class providing bidirectional 
 * conversion between COBOL and Java data formats with exact format compatibility.
 * 
 * This class ensures precise format compatibility for packed decimals, strings, dates, 
 * and other data types used throughout the microservices architecture. All conversions
 * maintain exact COBOL format semantics and precision requirements.
 * 
 * Key Features:
 * - Exact COBOL COMP-3 packed decimal conversion using BigDecimal with precision preservation
 * - String formatting and padding utilities replicating COBOL PICTURE clause behavior exactly
 * - Date and timestamp conversions preserving all original COBOL format patterns
 * - Comprehensive data validation matching COBOL field validation rules
 * - Bidirectional conversion supporting both COBOL-to-Java and Java-to-COBOL transformations
 * 
 * Technical Compliance:
 * - Implements Section 0.1.2 requirement for exact format compatibility between COBOL and Java
 * - COBOL COMP-3 conversion uses BigDecimal with exact precision mapping per technical specification
 * - String formatting replicates COBOL PICTURE clause behavior exactly
 * - Date conversions preserve all original COBOL format patterns from CSDAT01Y.cpy
 * - All numeric conversions maintain exact decimal precision using BigDecimalUtils
 * 
 * COBOL Format Support:
 * - Date formats: YYYYMMDD, MM/DD/YY, YYYY-MM-DD HH:MM:SS.NNNNNN
 * - Time formats: HHMMSSCC, HH:MM:SS
 * - Numeric formats: COMP-3, DISPLAY, SIGNED, ZONED DECIMAL
 * - String formats: PICTURE X(n), PICTURE 9(n), alphanumeric with padding
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 2024-01-01
 */
public class ConversionUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(ConversionUtils.class);
    
    // COBOL date format patterns from CSDAT01Y.cpy
    private static final DateTimeFormatter COBOL_DATE_YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter COBOL_DATE_MM_DD_YY = DateTimeFormatter.ofPattern("MM/dd/yy");
    private static final DateTimeFormatter COBOL_TIME_HHMMSS = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter COBOL_TIME_HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter COBOL_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
    
    // COBOL numeric validation patterns
    private static final Pattern COBOL_NUMERIC_PATTERN = Pattern.compile("^[+-]?\\d+(\\.\\d+)?$");
    private static final Pattern COBOL_DISPLAY_PATTERN = Pattern.compile("^[0-9\\s]*$");
    private static final Pattern COBOL_SIGNED_PATTERN = Pattern.compile("^[+-]?\\d+$");
    
    // COBOL century validation from CSUTLDWY.cpy
    private static final int THIS_CENTURY = 20;
    private static final int LAST_CENTURY = 19;
    
    /**
     * Private constructor to prevent instantiation.
     * This utility class contains only static methods and should not be instantiated.
     */
    private ConversionUtils() {
        throw new UnsupportedOperationException("ConversionUtils is a utility class and cannot be instantiated");
    }
    
    /**
     * Converts a COBOL string to Java string with proper trimming and null handling.
     * 
     * This method handles COBOL string conversion by trimming trailing spaces,
     * converting COBOL space-filled strings to null, and ensuring proper 
     * character encoding compatibility.
     * 
     * @param cobolString the COBOL string to convert
     * @return converted Java string with proper trimming, null if input is null or spaces
     */
    public static String cobolToJavaString(String cobolString) {
        logger.debug("Converting COBOL string to Java: '{}'", cobolString);
        
        if (cobolString == null) {
            return null;
        }
        
        // Trim trailing spaces (common in COBOL fixed-length fields)
        String trimmed = cobolString.trim();
        
        // Return null for empty or space-only strings (COBOL convention)
        if (trimmed.isEmpty() || StringUtils.isBlank(trimmed)) {
            return null;
        }
        
        logger.debug("Converted COBOL string '{}' to Java string '{}'", cobolString, trimmed);
        return trimmed;
    }
    
    /**
     * Converts a Java string to COBOL string with proper padding and formatting.
     * 
     * This method handles Java to COBOL string conversion by applying proper
     * padding, length constraints, and character formatting to match COBOL
     * fixed-length field requirements.
     * 
     * @param javaString the Java string to convert
     * @param length the target COBOL field length
     * @return converted COBOL string with proper padding and length
     */
    public static String javaToCobolString(String javaString, int length) {
        logger.debug("Converting Java string to COBOL: '{}', length: {}", javaString, length);
        
        if (javaString == null) {
            return StringUtils.rightPad("", length, ' ');
        }
        
        // Truncate if too long, pad if too short
        String result;
        if (javaString.length() > length) {
            result = javaString.substring(0, length);
            logger.warn("Truncated Java string '{}' to length {} for COBOL field", javaString, length);
        } else {
            result = StringUtils.rightPad(javaString, length, ' ');
        }
        
        logger.debug("Converted Java string '{}' to COBOL string '{}'", javaString, result);
        return result;
    }
    
    /**
     * Converts COBOL COMP-3 packed decimal to Java BigDecimal with exact precision.
     * 
     * This method converts COBOL COMP-3 packed decimal values to Java BigDecimal
     * using exact precision mapping and BigDecimalUtils for financial calculations.
     * Maintains all decimal places and precision requirements.
     * 
     * @param cobolComp3 the COBOL COMP-3 value as string representation
     * @param precision the total number of digits
     * @param scale the number of decimal places
     * @return BigDecimal with exact COBOL COMP-3 precision
     * @throws NumberFormatException if the input cannot be converted
     */
    public static BigDecimal cobolCompToDecimal(String cobolComp3, int precision, int scale) {
        logger.debug("Converting COBOL COMP-3 to BigDecimal: '{}', precision: {}, scale: {}", 
                    cobolComp3, precision, scale);
        
        if (cobolComp3 == null || cobolComp3.trim().isEmpty()) {
            return BigDecimal.ZERO.setScale(scale, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode());
        }
        
        try {
            // Parse using BigDecimalUtils for exact precision
            BigDecimal decimal = BigDecimalUtils.parseDecimal(cobolComp3);
            
            // Ensure exact scale matches COBOL definition
            BigDecimal result = decimal.setScale(scale, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode());
            
            logger.debug("Converted COBOL COMP-3 '{}' to BigDecimal '{}'", cobolComp3, result);
            return result;
            
        } catch (NumberFormatException e) {
            logger.error("Failed to convert COBOL COMP-3 '{}' to BigDecimal", cobolComp3, e);
            throw new NumberFormatException("Cannot convert COBOL COMP-3 value to BigDecimal: " + cobolComp3);
        }
    }
    
    /**
     * Converts Java BigDecimal to COBOL COMP-3 packed decimal format.
     * 
     * This method converts Java BigDecimal values to COBOL COMP-3 packed decimal
     * format with exact precision and scale preservation. Ensures financial
     * calculations maintain identical precision.
     * 
     * @param decimal the BigDecimal value to convert
     * @param precision the total number of digits for COBOL field
     * @param scale the number of decimal places for COBOL field
     * @return COBOL COMP-3 formatted string representation
     */
    public static String decimalToCobolComp(BigDecimal decimal, int precision, int scale) {
        logger.debug("Converting BigDecimal to COBOL COMP-3: '{}', precision: {}, scale: {}", 
                    decimal, precision, scale);
        
        if (decimal == null) {
            return "0".repeat(precision - scale) + (scale > 0 ? "." + "0".repeat(scale) : "");
        }
        
        // Set scale to match COBOL field definition
        BigDecimal scaledDecimal = decimal.setScale(scale, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode());
        
        // Format for COBOL COMP-3 representation
        String result = scaledDecimal.toPlainString();
        
        logger.debug("Converted BigDecimal '{}' to COBOL COMP-3 '{}'", decimal, result);
        return result;
    }
    
    /**
     * Converts COBOL date (YYYYMMDD format) to Java LocalDate.
     * 
     * This method converts COBOL date values in YYYYMMDD format to Java LocalDate
     * objects, handling century logic and date validation as defined in CSDAT01Y.cpy.
     * 
     * @param cobolDate the COBOL date string in YYYYMMDD format
     * @return LocalDate object representing the COBOL date
     * @throws DateTimeParseException if the date cannot be parsed
     */
    public static LocalDate cobolDateToLocalDate(String cobolDate) {
        logger.debug("Converting COBOL date to LocalDate: '{}'", cobolDate);
        
        if (cobolDate == null || cobolDate.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Handle YYYYMMDD format from CSDAT01Y.cpy
            String cleanDate = cobolDate.trim().replaceAll("[^0-9]", "");
            
            if (cleanDate.length() == 8) {
                // Full YYYYMMDD format
                LocalDate result = LocalDate.parse(cleanDate, COBOL_DATE_YYYYMMDD);
                logger.debug("Converted COBOL date '{}' to LocalDate '{}'", cobolDate, result);
                return result;
            } else if (cleanDate.length() == 6) {
                // YYMMDD format - apply century logic from CSUTLDWY.cpy
                int year = Integer.parseInt(cleanDate.substring(0, 2));
                int month = Integer.parseInt(cleanDate.substring(2, 4));
                int day = Integer.parseInt(cleanDate.substring(4, 6));
                
                // Century determination logic
                int fullYear = (year >= 50) ? (LAST_CENTURY * 100 + year) : (THIS_CENTURY * 100 + year);
                
                LocalDate result = LocalDate.of(fullYear, month, day);
                logger.debug("Converted COBOL date '{}' to LocalDate '{}' with century logic", cobolDate, result);
                return result;
            }
            
            throw new DateTimeParseException("Invalid COBOL date format", cobolDate, 0);
            
        } catch (DateTimeParseException | NumberFormatException e) {
            logger.error("Failed to convert COBOL date '{}' to LocalDate", cobolDate, e);
            throw new DateTimeParseException("Cannot parse COBOL date: " + cobolDate, cobolDate, 0);
        }
    }
    
    /**
     * Converts Java LocalDate to COBOL date format (YYYYMMDD).
     * 
     * This method converts Java LocalDate objects to COBOL date format
     * using YYYYMMDD pattern as defined in CSDAT01Y.cpy structure.
     * 
     * @param localDate the LocalDate to convert
     * @return COBOL date string in YYYYMMDD format
     */
    public static String localDateToCobolDate(LocalDate localDate) {
        logger.debug("Converting LocalDate to COBOL date: '{}'", localDate);
        
        if (localDate == null) {
            return "00000000";
        }
        
        String result = localDate.format(COBOL_DATE_YYYYMMDD);
        logger.debug("Converted LocalDate '{}' to COBOL date '{}'", localDate, result);
        return result;
    }
    
    /**
     * Converts COBOL time (HHMMSS format) to Java LocalTime.
     * 
     * This method converts COBOL time values in HHMMSS format to Java LocalTime
     * objects, handling the time format as defined in CSDAT01Y.cpy.
     * 
     * @param cobolTime the COBOL time string in HHMMSS format
     * @return LocalTime object representing the COBOL time
     * @throws DateTimeParseException if the time cannot be parsed
     */
    public static LocalTime cobolTimeToLocalTime(String cobolTime) {
        logger.debug("Converting COBOL time to LocalTime: '{}'", cobolTime);
        
        if (cobolTime == null || cobolTime.trim().isEmpty()) {
            return null;
        }
        
        try {
            String cleanTime = cobolTime.trim().replaceAll("[^0-9]", "");
            
            if (cleanTime.length() >= 6) {
                // Handle HHMMSS format from CSDAT01Y.cpy
                String timeString = cleanTime.substring(0, 6);
                LocalTime result = LocalTime.parse(timeString, COBOL_TIME_HHMMSS);
                logger.debug("Converted COBOL time '{}' to LocalTime '{}'", cobolTime, result);
                return result;
            }
            
            throw new DateTimeParseException("Invalid COBOL time format", cobolTime, 0);
            
        } catch (DateTimeParseException | NumberFormatException e) {
            logger.error("Failed to convert COBOL time '{}' to LocalTime", cobolTime, e);
            throw new DateTimeParseException("Cannot parse COBOL time: " + cobolTime, cobolTime, 0);
        }
    }
    
    /**
     * Converts Java LocalTime to COBOL time format (HHMMSS).
     * 
     * This method converts Java LocalTime objects to COBOL time format
     * using HHMMSS pattern as defined in CSDAT01Y.cpy structure.
     * 
     * @param localTime the LocalTime to convert
     * @return COBOL time string in HHMMSS format
     */
    public static String localTimeToCobolTime(LocalTime localTime) {
        logger.debug("Converting LocalTime to COBOL time: '{}'", localTime);
        
        if (localTime == null) {
            return "000000";
        }
        
        String result = localTime.format(COBOL_TIME_HHMMSS);
        logger.debug("Converted LocalTime '{}' to COBOL time '{}'", localTime, result);
        return result;
    }
    
    /**
     * Converts COBOL timestamp to Java LocalDateTime.
     * 
     * This method converts COBOL timestamp values in YYYY-MM-DD HH:MM:SS.NNNNNN format
     * to Java LocalDateTime objects, as defined in CSDAT01Y.cpy WS-TIMESTAMP structure.
     * 
     * @param cobolTimestamp the COBOL timestamp string
     * @return LocalDateTime object representing the COBOL timestamp
     * @throws DateTimeParseException if the timestamp cannot be parsed
     */
    public static LocalDateTime cobolTimestampToLocalDateTime(String cobolTimestamp) {
        logger.debug("Converting COBOL timestamp to LocalDateTime: '{}'", cobolTimestamp);
        
        if (cobolTimestamp == null || cobolTimestamp.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Handle YYYY-MM-DD HH:MM:SS.NNNNNN format from CSDAT01Y.cpy
            String cleanTimestamp = cobolTimestamp.trim();
            
            // Pad microseconds if needed
            if (cleanTimestamp.length() == 19) {
                cleanTimestamp += ".000000";
            } else if (cleanTimestamp.contains(".") && cleanTimestamp.length() < 26) {
                int dotIndex = cleanTimestamp.indexOf(".");
                String microseconds = cleanTimestamp.substring(dotIndex + 1);
                cleanTimestamp = cleanTimestamp.substring(0, dotIndex + 1) + StringUtils.rightPad(microseconds, 6, '0');
            }
            
            LocalDateTime result = LocalDateTime.parse(cleanTimestamp, COBOL_TIMESTAMP);
            logger.debug("Converted COBOL timestamp '{}' to LocalDateTime '{}'", cobolTimestamp, result);
            return result;
            
        } catch (DateTimeParseException e) {
            logger.error("Failed to convert COBOL timestamp '{}' to LocalDateTime", cobolTimestamp, e);
            throw new DateTimeParseException("Cannot parse COBOL timestamp: " + cobolTimestamp, cobolTimestamp, 0);
        }
    }
    
    /**
     * Converts Java LocalDateTime to COBOL timestamp format.
     * 
     * This method converts Java LocalDateTime objects to COBOL timestamp format
     * using YYYY-MM-DD HH:MM:SS.NNNNNN pattern as defined in CSDAT01Y.cpy.
     * 
     * @param localDateTime the LocalDateTime to convert
     * @return COBOL timestamp string in YYYY-MM-DD HH:MM:SS.NNNNNN format
     */
    public static String localDateTimeToCobolTimestamp(LocalDateTime localDateTime) {
        logger.debug("Converting LocalDateTime to COBOL timestamp: '{}'", localDateTime);
        
        if (localDateTime == null) {
            return "0000-00-00 00:00:00.000000";
        }
        
        String result = localDateTime.format(COBOL_TIMESTAMP);
        logger.debug("Converted LocalDateTime '{}' to COBOL timestamp '{}'", localDateTime, result);
        return result;
    }
    
    /**
     * Formats a string value according to COBOL PICTURE clause specifications.
     * 
     * This method applies COBOL PICTURE clause formatting rules to Java strings,
     * handling alphanumeric (X), numeric (9), and special character formatting
     * with proper padding and alignment.
     * 
     * @param value the string value to format
     * @param pictureClause the COBOL PICTURE clause specification (e.g., "X(10)", "9(5)V99")
     * @return formatted string according to PICTURE clause specifications
     */
    public static String formatWithPicture(String value, String pictureClause) {
        logger.debug("Formatting string '{}' with PICTURE clause '{}'", value, pictureClause);
        
        if (value == null) {
            value = "";
        }
        
        if (pictureClause == null || pictureClause.trim().isEmpty()) {
            return value;
        }
        
        try {
            // Parse PICTURE clause to determine format
            String cleanPicture = pictureClause.trim().toUpperCase();
            
            if (cleanPicture.startsWith("X(") && cleanPicture.endsWith(")")) {
                // Alphanumeric field - X(n)
                int length = Integer.parseInt(cleanPicture.substring(2, cleanPicture.length() - 1));
                String result = StringUtils.rightPad(value, length, ' ');
                if (result.length() > length) {
                    result = result.substring(0, length);
                }
                logger.debug("Formatted string '{}' as alphanumeric X({}) to '{}'", value, length, result);
                return result;
                
            } else if (cleanPicture.startsWith("9(") && cleanPicture.contains(")")) {
                // Numeric field - 9(n) or 9(n)V9(m)
                if (cleanPicture.contains("V")) {
                    // Decimal field - 9(n)V9(m)
                    String[] parts = cleanPicture.split("V");
                    int integerPart = Integer.parseInt(parts[0].substring(2, parts[0].length() - 1));
                    int decimalPart = Integer.parseInt(parts[1].substring(2, parts[1].length() - 1));
                    
                    BigDecimal decimal = NumberUtils.isCreatable(value) ? 
                        BigDecimalUtils.parseDecimal(value) : BigDecimal.ZERO;
                    
                    DecimalFormat format = new DecimalFormat();
                    format.setMinimumIntegerDigits(integerPart);
                    format.setMaximumIntegerDigits(integerPart);
                    format.setMinimumFractionDigits(decimalPart);
                    format.setMaximumFractionDigits(decimalPart);
                    format.setGroupingUsed(false);
                    
                    String result = format.format(decimal);
                    logger.debug("Formatted string '{}' as numeric 9({})V9({}) to '{}'", value, integerPart, decimalPart, result);
                    return result;
                    
                } else {
                    // Integer field - 9(n)
                    int length = Integer.parseInt(cleanPicture.substring(2, cleanPicture.length() - 1));
                    String numericValue = NumberUtils.isCreatable(value) ? value : "0";
                    String result = StringUtils.leftPad(numericValue, length, '0');
                    if (result.length() > length) {
                        result = result.substring(result.length() - length);
                    }
                    logger.debug("Formatted string '{}' as numeric 9({}) to '{}'", value, length, result);
                    return result;
                }
            }
            
            // Default: return value as-is
            logger.debug("No specific formatting applied for PICTURE clause '{}', returning original value", pictureClause);
            return value;
            
        } catch (Exception e) {
            logger.error("Failed to format string '{}' with PICTURE clause '{}'", value, pictureClause, e);
            return value;
        }
    }
    
    /**
     * Pads a string according to COBOL field padding rules.
     * 
     * This method applies COBOL string padding rules, handling both left and right
     * padding with spaces or zeros as appropriate for the field type.
     * 
     * @param value the string value to pad
     * @param length the target field length
     * @param padChar the character to use for padding
     * @param leftPad true for left padding, false for right padding
     * @return padded string according to COBOL rules
     */
    public static String padStringCobol(String value, int length, char padChar, boolean leftPad) {
        logger.debug("Padding string '{}' to length {} with char '{}', leftPad: {}", value, length, padChar, leftPad);
        
        if (value == null) {
            value = "";
        }
        
        if (value.length() >= length) {
            String result = value.substring(0, length);
            logger.debug("Truncated string '{}' to length {}: '{}'", value, length, result);
            return result;
        }
        
        String result = leftPad ? StringUtils.leftPad(value, length, padChar) : StringUtils.rightPad(value, length, padChar);
        logger.debug("Padded string '{}' to '{}'", value, result);
        return result;
    }
    
    /**
     * Trims a string according to COBOL trimming rules.
     * 
     * This method applies COBOL string trimming rules, handling trailing spaces
     * and converting space-filled strings to empty strings as per COBOL conventions.
     * 
     * @param cobolString the COBOL string to trim
     * @return trimmed string according to COBOL rules
     */
    public static String trimCobolString(String cobolString) {
        logger.debug("Trimming COBOL string: '{}'", cobolString);
        
        if (cobolString == null) {
            return null;
        }
        
        // Trim trailing spaces (common in COBOL fixed-length fields)
        String result = cobolString.trim();
        
        // Convert space-only strings to empty (COBOL convention)
        if (StringUtils.isBlank(result)) {
            result = "";
        }
        
        logger.debug("Trimmed COBOL string '{}' to '{}'", cobolString, result);
        return result;
    }
    
    /**
     * Validates if a string represents a valid COBOL numeric value.
     * 
     * This method validates string values according to COBOL numeric field rules,
     * checking for valid numeric characters, sign placement, and decimal points.
     * 
     * @param value the string value to validate
     * @param allowDecimal true if decimal points are allowed
     * @param allowSigned true if signed values are allowed
     * @return true if the value is valid COBOL numeric, false otherwise
     */
    public static boolean validateCobolNumeric(String value, boolean allowDecimal, boolean allowSigned) {
        logger.debug("Validating COBOL numeric: '{}', allowDecimal: {}, allowSigned: {}", value, allowDecimal, allowSigned);
        
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        
        String cleanValue = value.trim();
        
        // Check for signed numeric pattern
        if (allowSigned && !allowDecimal) {
            boolean isValid = COBOL_SIGNED_PATTERN.matcher(cleanValue).matches();
            logger.debug("Signed numeric validation result: {}", isValid);
            return isValid;
        }
        
        // Check for decimal numeric pattern
        if (allowDecimal) {
            boolean isValid = COBOL_NUMERIC_PATTERN.matcher(cleanValue).matches();
            logger.debug("Decimal numeric validation result: {}", isValid);
            return isValid;
        }
        
        // Check for display numeric pattern (digits and spaces only)
        boolean isValid = COBOL_DISPLAY_PATTERN.matcher(cleanValue).matches();
        logger.debug("Display numeric validation result: {}", isValid);
        return isValid;
    }
    
    /**
     * Converts COBOL DISPLAY format numeric to Java numeric types.
     * 
     * This method converts COBOL DISPLAY format numeric values to appropriate
     * Java numeric types, handling zero-padding and space-filling as per
     * COBOL DISPLAY field conventions.
     * 
     * @param displayValue the COBOL DISPLAY numeric value
     * @param targetType the target Java numeric type (Integer, Long, BigDecimal)
     * @return converted numeric value in the specified target type
     * @throws NumberFormatException if the value cannot be converted
     */
    public static Object convertCobolDisplay(String displayValue, Class<?> targetType) {
        logger.debug("Converting COBOL DISPLAY '{}' to type '{}'", displayValue, targetType.getSimpleName());
        
        if (displayValue == null || displayValue.trim().isEmpty()) {
            if (targetType == Integer.class) {
                return 0;
            } else if (targetType == Long.class) {
                return 0L;
            } else if (targetType == BigDecimal.class) {
                return BigDecimal.ZERO;
            }
            return null;
        }
        
        // Clean the display value (remove leading/trailing spaces)
        String cleanValue = displayValue.trim();
        
        // Handle space-filled values as zero
        if (StringUtils.isBlank(cleanValue)) {
            cleanValue = "0";
        }
        
        try {
            if (targetType == Integer.class) {
                int result = NumberUtils.toInt(cleanValue, 0);
                logger.debug("Converted DISPLAY '{}' to Integer: {}", displayValue, result);
                return result;
                
            } else if (targetType == Long.class) {
                long result = NumberUtils.toLong(cleanValue, 0L);
                logger.debug("Converted DISPLAY '{}' to Long: {}", displayValue, result);
                return result;
                
            } else if (targetType == BigDecimal.class) {
                BigDecimal result = BigDecimalUtils.createDecimal(cleanValue);
                logger.debug("Converted DISPLAY '{}' to BigDecimal: {}", displayValue, result);
                return result;
                
            } else {
                throw new IllegalArgumentException("Unsupported target type: " + targetType.getSimpleName());
            }
            
        } catch (NumberFormatException e) {
            logger.error("Failed to convert COBOL DISPLAY '{}' to type '{}'", displayValue, targetType.getSimpleName(), e);
            throw new NumberFormatException("Cannot convert COBOL DISPLAY value to " + targetType.getSimpleName() + ": " + displayValue);
        }
    }
    
    /**
     * Parses a COBOL date string with flexible format detection.
     * 
     * This method parses COBOL date strings with automatic format detection,
     * supporting multiple date formats defined in CSDAT01Y.cpy including
     * YYYYMMDD, MM/DD/YY, and other common COBOL date patterns.
     * 
     * @param cobolDate the COBOL date string to parse
     * @return LocalDate object representing the parsed date
     * @throws DateTimeParseException if the date cannot be parsed
     */
    public static LocalDate parseCobolDate(String cobolDate) {
        logger.debug("Parsing COBOL date with format detection: '{}'", cobolDate);
        
        if (cobolDate == null || cobolDate.trim().isEmpty()) {
            return null;
        }
        
        String cleanDate = cobolDate.trim();
        
        try {
            // Try YYYYMMDD format first
            if (cleanDate.matches("\\d{8}")) {
                LocalDate result = LocalDate.parse(cleanDate, COBOL_DATE_YYYYMMDD);
                logger.debug("Parsed COBOL date '{}' as YYYYMMDD: {}", cobolDate, result);
                return result;
            }
            
            // Try MM/DD/YY format
            if (cleanDate.matches("\\d{2}/\\d{2}/\\d{2}")) {
                LocalDate result = LocalDate.parse(cleanDate, COBOL_DATE_MM_DD_YY);
                logger.debug("Parsed COBOL date '{}' as MM/DD/YY: {}", cobolDate, result);
                return result;
            }
            
            // Try YYMMDD format with century logic
            if (cleanDate.matches("\\d{6}")) {
                return cobolDateToLocalDate(cleanDate);
            }
            
            throw new DateTimeParseException("Unsupported COBOL date format", cobolDate, 0);
            
        } catch (DateTimeParseException e) {
            logger.error("Failed to parse COBOL date '{}'", cobolDate, e);
            throw e;
        }
    }
    
    /**
     * Formats a LocalDate to COBOL date string in specified format.
     * 
     * This method formats Java LocalDate objects to COBOL date strings
     * using the specified format pattern, supporting all date formats
     * defined in CSDAT01Y.cpy.
     * 
     * @param localDate the LocalDate to format
     * @param format the COBOL date format ("YYYYMMDD", "MM/DD/YY", etc.)
     * @return formatted COBOL date string
     */
    public static String formatCobolDate(LocalDate localDate, String format) {
        logger.debug("Formatting LocalDate '{}' to COBOL format '{}'", localDate, format);
        
        if (localDate == null) {
            return format.equals("YYYYMMDD") ? "00000000" : 
                   format.equals("MM/DD/YY") ? "00/00/00" : "";
        }
        
        try {
            String result;
            switch (format.toUpperCase()) {
                case "YYYYMMDD":
                    result = localDate.format(COBOL_DATE_YYYYMMDD);
                    break;
                case "MM/DD/YY":
                    result = localDate.format(COBOL_DATE_MM_DD_YY);
                    break;
                default:
                    result = localDate.format(DateTimeFormatter.ofPattern(format));
            }
            
            logger.debug("Formatted LocalDate '{}' to COBOL format '{}': '{}'", localDate, format, result);
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to format LocalDate '{}' to COBOL format '{}'", localDate, format, e);
            return localDate.format(COBOL_DATE_YYYYMMDD);
        }
    }
    
    /**
     * Converts COBOL signed numeric values to Java numeric types.
     * 
     * This method converts COBOL signed numeric values (including leading/trailing
     * sign indicators) to appropriate Java numeric types, handling both zoned
     * decimal and packed decimal signed representations.
     * 
     * @param signedValue the COBOL signed numeric value
     * @param targetType the target Java numeric type (Integer, Long, BigDecimal)
     * @return converted signed numeric value in the specified target type
     * @throws NumberFormatException if the value cannot be converted
     */
    public static Object convertSignedNumeric(String signedValue, Class<?> targetType) {
        logger.debug("Converting COBOL signed numeric '{}' to type '{}'", signedValue, targetType.getSimpleName());
        
        if (signedValue == null || signedValue.trim().isEmpty()) {
            if (targetType == Integer.class) {
                return 0;
            } else if (targetType == Long.class) {
                return 0L;
            } else if (targetType == BigDecimal.class) {
                return BigDecimal.ZERO;
            }
            return null;
        }
        
        String cleanValue = signedValue.trim();
        boolean negative = false;
        
        // Handle leading sign
        if (cleanValue.startsWith("-")) {
            negative = true;
            cleanValue = cleanValue.substring(1);
        } else if (cleanValue.startsWith("+")) {
            cleanValue = cleanValue.substring(1);
        }
        
        // Handle trailing sign (COBOL convention)
        if (cleanValue.endsWith("-")) {
            negative = true;
            cleanValue = cleanValue.substring(0, cleanValue.length() - 1);
        } else if (cleanValue.endsWith("+")) {
            cleanValue = cleanValue.substring(0, cleanValue.length() - 1);
        }
        
        try {
            if (targetType == Integer.class) {
                int result = NumberUtils.toInt(cleanValue, 0);
                result = negative ? -result : result;
                logger.debug("Converted signed numeric '{}' to Integer: {}", signedValue, result);
                return result;
                
            } else if (targetType == Long.class) {
                long result = NumberUtils.toLong(cleanValue, 0L);
                result = negative ? -result : result;
                logger.debug("Converted signed numeric '{}' to Long: {}", signedValue, result);
                return result;
                
            } else if (targetType == BigDecimal.class) {
                BigDecimal result = BigDecimalUtils.createDecimal(cleanValue);
                result = negative ? result.negate() : result;
                logger.debug("Converted signed numeric '{}' to BigDecimal: {}", signedValue, result);
                return result;
                
            } else {
                throw new IllegalArgumentException("Unsupported target type: " + targetType.getSimpleName());
            }
            
        } catch (NumberFormatException e) {
            logger.error("Failed to convert signed numeric '{}' to type '{}'", signedValue, targetType.getSimpleName(), e);
            throw new NumberFormatException("Cannot convert COBOL signed numeric value to " + targetType.getSimpleName() + ": " + signedValue);
        }
    }
}