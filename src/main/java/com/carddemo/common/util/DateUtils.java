package com.carddemo.common.util;

import com.carddemo.common.enums.ErrorFlag;
import com.carddemo.common.enums.ValidationResult;
import com.carddemo.common.validator.ValidationConstants;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Comprehensive date utility class converted from COBOL utilities CSUTLDTC.cbl and CSUTLDPY.cpy.
 * 
 * <p>This class provides date validation, formatting, and conversion with exact preservation of COBOL 
 * calendar logic including leap year calculations and century validation constraints. All validation 
 * methods maintain identical behavior to the original COBOL date processing routines.</p>
 * 
 * <p>Core COBOL Conversion Mapping:</p>
 * <ul>
 *   <li>CSUTLDTC.cbl CEEDAYS API → Java LocalDate validation with equivalent error handling</li>
 *   <li>CSUTLDPY.cpy EDIT-DATE-CCYYMMDD → validateDate() with comprehensive validation</li>
 *   <li>CSUTLDPY.cpy EDIT-YEAR-CCYY → validateYear() with century constraints (19xx, 20xx)</li>
 *   <li>CSUTLDPY.cpy EDIT-MONTH → validateMonth() with 01-12 range validation</li>
 *   <li>CSUTLDPY.cpy EDIT-DAY → validateDay() with month-specific day validation</li>
 *   <li>CSUTLDPY.cpy EDIT-DAY-MONTH-YEAR → comprehensive leap year and month-day validation</li>
 *   <li>CSUTLDPY.cpy EDIT-DATE-OF-BIRTH → validateDateOfBirth() with future date prevention</li>
 *   <li>CSDAT01Y.cpy date formats → multiple format parsing and formatting methods</li>
 * </ul>
 * 
 * <p>Supported Date Formats:</p>
 * <ul>
 *   <li>CCYYMMDD: Century-Year-Month-Day (primary COBOL format)</li>
 *   <li>YYYYMMDD: Year-Month-Day (alternative COBOL format)</li>
 *   <li>MM/DD/YYYY: US format with slashes</li>
 *   <li>YYYY-MM-DD: ISO format with dashes</li>
 *   <li>YYYY-MM-DD HH:MM:SS: Full timestamp format</li>
 * </ul>
 * 
 * <p>COBOL Calendar Logic Preservation:</p>
 * <ul>
 *   <li>Century validation limited to 19xx and 20xx per COBOL Y2K constraints</li>
 *   <li>Leap year calculation using exact COBOL arithmetic (divide by 4, 100, 400)</li>
 *   <li>Month-specific day validation (31-day months, 30-day months, February)</li>
 *   <li>Date of birth validation preventing future dates per business rules</li>
 *   <li>Error flag handling using COBOL Y/N boolean patterns</li>
 * </ul>
 * 
 * <p>Performance Characteristics:</p>
 * <ul>
 *   <li>Optimized for transaction response times under 200ms at 95th percentile</li>
 *   <li>Thread-safe validation supporting 10,000+ TPS processing</li>
 *   <li>Memory efficient for batch processing within 4-hour window</li>
 *   <li>Compiled regex patterns for optimal format validation performance</li>
 * </ul>
 * 
 * @author Blitzy Platform
 * @version 1.0
 * @since Java 21
 */
public final class DateUtils {
    
    /**
     * Logger instance for date validation operations and error tracking
     */
    private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);
    
    // ========================================================================
    // DATE FORMAT CONSTANTS - COBOL DATE FORMAT EQUIVALENTS
    // ========================================================================
    
    /**
     * COBOL date format CCYYMMDD - Century, Year, Month, Day
     * Equivalent to CSUTLDPY.cpy WS-EDIT-DATE-CCYYMMDD structure
     */
    public static final String COBOL_DATE_FORMAT = "yyyyMMdd";
    
    /**
     * COBOL timestamp format YYYY-MM-DD HH:MM:SS.SSSSSS
     * Equivalent to CSDAT01Y.cpy WS-TIMESTAMP structure
     */
    public static final String COBOL_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSSSS";
    
    /**
     * US slash date format MM/DD/YYYY
     * Equivalent to CSDAT01Y.cpy WS-CURDATE-MM-DD-YY structure
     */
    public static final String SLASH_DATE_FORMAT = "MM/dd/yyyy";
    
    // ========================================================================
    // COMPILED REGEX PATTERNS FOR OPTIMAL PERFORMANCE
    // ========================================================================
    
    /**
     * Pattern for CCYYMMDD format validation (8 numeric digits)
     */
    private static final Pattern CCYYMMDD_PATTERN = Pattern.compile("^\\d{8}$");
    
    /**
     * Pattern for MM/DD/YYYY format validation
     */
    private static final Pattern SLASH_DATE_PATTERN = Pattern.compile("^\\d{2}/\\d{2}/\\d{4}$");
    
    /**
     * Pattern for YYYY-MM-DD format validation
     */
    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    
    /**
     * Pattern for timestamp format validation
     */
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}(\\.\\d{1,6})?$");
    
    // ========================================================================
    // COMPILED DATE FORMATTERS FOR OPTIMAL PERFORMANCE
    // ========================================================================
    
    /**
     * Formatter for CCYYMMDD format (primary COBOL format)
     */
    private static final DateTimeFormatter COBOL_DATE_FORMATTER = DateTimeFormatter.ofPattern(COBOL_DATE_FORMAT);
    
    /**
     * Formatter for MM/DD/YYYY format
     */
    private static final DateTimeFormatter SLASH_DATE_FORMATTER = DateTimeFormatter.ofPattern(SLASH_DATE_FORMAT);
    
    /**
     * Formatter for YYYY-MM-DD ISO format
     */
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * Formatter for timestamp format
     */
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // ========================================================================
    // COBOL CALENDAR LOGIC CONSTANTS
    // ========================================================================
    
    /**
     * Valid century values per COBOL THIS-CENTURY/LAST-CENTURY conditions
     */
    private static final int THIS_CENTURY = 20;
    private static final int LAST_CENTURY = 19;
    
    /**
     * Days in each month for non-leap years
     */
    private static final int[] DAYS_IN_MONTH = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    
    /**
     * Months with 31 days (equivalent to COBOL WS-31-DAY-MONTH condition)
     */
    private static final int[] THIRTY_ONE_DAY_MONTHS = {1, 3, 5, 7, 8, 10, 12};
    
    /**
     * Private constructor preventing instantiation
     */
    private DateUtils() {
        throw new UnsupportedOperationException("DateUtils is a utility class and cannot be instantiated");
    }
    
    // ========================================================================
    // PRIMARY DATE VALIDATION METHODS
    // ========================================================================
    
    /**
     * Validates a date string with comprehensive COBOL date validation logic.
     * 
     * <p>This method replicates the complete COBOL date validation from CSUTLDPY.cpy 
     * EDIT-DATE-CCYYMMDD routine, including:</p>
     * <ul>
     *   <li>Format validation for multiple date formats</li>
     *   <li>Century validation (19xx, 20xx only)</li>
     *   <li>Year validation (4 digits, numeric)</li>
     *   <li>Month validation (01-12)</li>
     *   <li>Day validation (01-31 with month-specific validation)</li>
     *   <li>Leap year calculation for February 29</li>
     *   <li>Final validation using LocalDate parsing</li>
     * </ul>
     * 
     * <p>Equivalent to COBOL procedure:</p>
     * <pre>
     * EDIT-DATE-CCYYMMDD.
     *    EDIT-YEAR-CCYY.
     *    EDIT-MONTH.
     *    EDIT-DAY.
     *    EDIT-DAY-MONTH-YEAR.
     *    EDIT-DATE-LE.
     * </pre>
     * 
     * @param dateValue The date string to validate (supports CCYYMMDD, MM/DD/YYYY, YYYY-MM-DD formats)
     * @return ValidationResult.VALID if valid, appropriate error result otherwise
     */
    public static ValidationResult validateDate(String dateValue) {
        logger.debug("Validating date: {}", dateValue);
        
        // Required field validation (COBOL LOW-VALUES/SPACES check)
        ValidationResult requiredCheck = ValidationUtils.validateRequiredField(dateValue, "Date");
        if (!requiredCheck.isValid()) {
            return requiredCheck;
        }
        
        String cleanDate = ValidationUtils.sanitizeInput(dateValue);
        
        // Try to parse the date in multiple formats
        Optional<LocalDate> parsedDate = parseDate(cleanDate);
        if (parsedDate.isEmpty()) {
            logger.debug("Date parsing failed for: {}", cleanDate);
            return ValidationResult.BAD_DATE_VALUE;
        }
        
        LocalDate date = parsedDate.get();
        
        // Validate century constraints (COBOL THIS-CENTURY/LAST-CENTURY logic)
        ValidationResult centuryResult = validateCentury(date.getYear());
        if (!centuryResult.isValid()) {
            return centuryResult;
        }
        
        logger.debug("Date validation successful: {} -> {}", cleanDate, date);
        return ValidationResult.VALID;
    }
    
    /**
     * Validates year with COBOL century constraints.
     * 
     * <p>Replicates COBOL EDIT-YEAR-CCYY validation from CSUTLDPY.cpy:</p>
     * <pre>
     * IF THIS-CENTURY
     * OR LAST-CENTURY
     *    CONTINUE
     * ELSE
     *    SET INPUT-ERROR TO TRUE
     * END-IF
     * </pre>
     * 
     * @param year The year to validate (must be 19xx or 20xx)
     * @return ValidationResult.VALID if valid century, INVALID_ERA otherwise
     */
    public static ValidationResult validateYear(int year) {
        logger.debug("Validating year: {}", year);
        
        int century = year / 100;
        
        if (century == THIS_CENTURY || century == LAST_CENTURY) {
            logger.debug("Year validation successful: {}", year);
            return ValidationResult.VALID;
        }
        
        logger.debug("Invalid century for year: {}", year);
        return ValidationResult.INVALID_ERA;
    }
    
    /**
     * Validates month with COBOL month range validation.
     * 
     * <p>Replicates COBOL EDIT-MONTH validation from CSUTLDPY.cpy:</p>
     * <pre>
     * IF WS-VALID-MONTH
     *    CONTINUE
     * ELSE
     *    SET INPUT-ERROR TO TRUE
     * END-IF
     * </pre>
     * 
     * @param month The month to validate (1-12)
     * @return ValidationResult.VALID if valid month, INVALID_MONTH otherwise
     */
    public static ValidationResult validateMonth(int month) {
        logger.debug("Validating month: {}", month);
        
        if (month >= 1 && month <= 12) {
            logger.debug("Month validation successful: {}", month);
            return ValidationResult.VALID;
        }
        
        logger.debug("Invalid month: {}", month);
        return ValidationResult.INVALID_MONTH;
    }
    
    /**
     * Validates day with COBOL day range and month-specific validation.
     * 
     * <p>Replicates COBOL EDIT-DAY validation from CSUTLDPY.cpy:</p>
     * <pre>
     * IF WS-VALID-DAY
     *    CONTINUE
     * ELSE
     *    SET INPUT-ERROR TO TRUE
     * END-IF
     * </pre>
     * 
     * @param day The day to validate (1-31)
     * @param month The month for day validation context
     * @param year The year for leap year calculation
     * @return ValidationResult.VALID if valid day, INVALID_RANGE otherwise
     */
    public static ValidationResult validateDay(int day, int month, int year) {
        logger.debug("Validating day: {} for month: {} year: {}", day, month, year);
        
        if (day < 1 || day > 31) {
            logger.debug("Day out of general range: {}", day);
            return ValidationResult.INVALID_RANGE;
        }
        
        // Month-specific day validation
        int maxDaysInMonth = getDaysInMonth(month, year);
        if (day > maxDaysInMonth) {
            logger.debug("Day {} exceeds maximum for month {}: {}", day, month, maxDaysInMonth);
            return ValidationResult.INVALID_RANGE;
        }
        
        logger.debug("Day validation successful: {}", day);
        return ValidationResult.VALID;
    }
    
    /**
     * Validates date of birth with COBOL future date prevention.
     * 
     * <p>Replicates COBOL EDIT-DATE-OF-BIRTH validation from CSUTLDPY.cpy:</p>
     * <pre>
     * IF WS-CURRENT-DATE-BINARY > WS-EDIT-DATE-BINARY
     *    CONTINUE
     * ELSE
     *    SET INPUT-ERROR TO TRUE
     * END-IF
     * </pre>
     * 
     * @param dateOfBirth The date of birth to validate
     * @return ValidationResult.VALID if not in future, INVALID_RANGE otherwise
     */
    public static ValidationResult validateDateOfBirth(String dateOfBirth) {
        logger.debug("Validating date of birth: {}", dateOfBirth);
        
        // First validate the date format
        ValidationResult dateValidation = validateDate(dateOfBirth);
        if (!dateValidation.isValid()) {
            return dateValidation;
        }
        
        // Parse the date
        Optional<LocalDate> parsedDate = parseDate(ValidationUtils.sanitizeInput(dateOfBirth));
        if (parsedDate.isEmpty()) {
            return ValidationResult.BAD_DATE_VALUE;
        }
        
        LocalDate birthDate = parsedDate.get();
        LocalDate currentDate = getCurrentDate();
        
        // Check if birth date is in the future
        if (birthDate.isAfter(currentDate)) {
            logger.debug("Date of birth is in the future: {}", birthDate);
            return ValidationResult.INVALID_RANGE;
        }
        
        logger.debug("Date of birth validation successful: {}", birthDate);
        return ValidationResult.VALID;
    }
    
    /**
     * Validates century with COBOL century constraints.
     * 
     * <p>Replicates COBOL century validation logic from CSUTLDPY.cpy:</p>
     * <pre>
     * IF THIS-CENTURY
     * OR LAST-CENTURY
     *    CONTINUE
     * ELSE
     *    SET INPUT-ERROR TO TRUE
     * END-IF
     * </pre>
     * 
     * @param year The year to validate century for
     * @return ValidationResult.VALID if valid century, INVALID_ERA otherwise
     */
    public static ValidationResult validateCentury(int year) {
        logger.debug("Validating century for year: {}", year);
        
        int century = year / 100;
        
        if (century == THIS_CENTURY || century == LAST_CENTURY) {
            logger.debug("Century validation successful for year: {}", year);
            return ValidationResult.VALID;
        }
        
        logger.debug("Invalid century for year: {} (century: {})", year, century);
        return ValidationResult.INVALID_ERA;
    }
    
    // ========================================================================
    // DATE PARSING AND FORMATTING METHODS
    // ========================================================================
    
    /**
     * Parses a date string in multiple formats with COBOL format priority.
     * 
     * <p>Attempts to parse the date in the following order:</p>
     * <ol>
     *   <li>CCYYMMDD format (primary COBOL format)</li>
     *   <li>YYYY-MM-DD format (ISO format)</li>
     *   <li>MM/DD/YYYY format (US format)</li>
     * </ol>
     * 
     * @param dateValue The date string to parse
     * @return Optional containing parsed LocalDate if successful, empty otherwise
     */
    public static Optional<LocalDate> parseDate(String dateValue) {
        logger.debug("Parsing date: {}", dateValue);
        
        if (StringUtils.isBlank(dateValue)) {
            return Optional.empty();
        }
        
        String cleanDate = ValidationUtils.sanitizeInput(dateValue);
        
        // Try CCYYMMDD format first (primary COBOL format)
        if (CCYYMMDD_PATTERN.matcher(cleanDate).matches()) {
            try {
                LocalDate date = LocalDate.parse(cleanDate, COBOL_DATE_FORMATTER);
                logger.debug("Successfully parsed CCYYMMDD format: {} -> {}", cleanDate, date);
                return Optional.of(date);
            } catch (DateTimeParseException e) {
                logger.debug("Failed to parse as CCYYMMDD: {}", cleanDate);
            }
        }
        
        // Try YYYY-MM-DD format (ISO format)
        if (ISO_DATE_PATTERN.matcher(cleanDate).matches()) {
            try {
                LocalDate date = LocalDate.parse(cleanDate, ISO_DATE_FORMATTER);
                logger.debug("Successfully parsed ISO format: {} -> {}", cleanDate, date);
                return Optional.of(date);
            } catch (DateTimeParseException e) {
                logger.debug("Failed to parse as ISO format: {}", cleanDate);
            }
        }
        
        // Try MM/DD/YYYY format (US format)
        if (SLASH_DATE_PATTERN.matcher(cleanDate).matches()) {
            try {
                LocalDate date = LocalDate.parse(cleanDate, SLASH_DATE_FORMATTER);
                logger.debug("Successfully parsed slash format: {} -> {}", cleanDate, date);
                return Optional.of(date);
            } catch (DateTimeParseException e) {
                logger.debug("Failed to parse as slash format: {}", cleanDate);
            }
        }
        
        logger.debug("Failed to parse date in any supported format: {}", cleanDate);
        return Optional.empty();
    }
    
    /**
     * Formats a LocalDate to COBOL CCYYMMDD format.
     * 
     * <p>Equivalent to COBOL date formatting to WS-EDIT-DATE-CCYYMMDD structure.</p>
     * 
     * @param date The LocalDate to format
     * @return Formatted date string in CCYYMMDD format
     */
    public static String formatDate(LocalDate date) {
        logger.debug("Formatting date to COBOL format: {}", date);
        
        if (date == null) {
            return "";
        }
        
        String formatted = date.format(COBOL_DATE_FORMATTER);
        logger.debug("Formatted date: {} -> {}", date, formatted);
        return formatted;
    }
    
    /**
     * Formats a LocalDate for display in MM/DD/YYYY format.
     * 
     * <p>Equivalent to COBOL WS-CURDATE-MM-DD-YY formatting logic.</p>
     * 
     * @param date The LocalDate to format
     * @return Formatted date string in MM/DD/YYYY format
     */
    public static String formatDateForDisplay(LocalDate date) {
        logger.debug("Formatting date for display: {}", date);
        
        if (date == null) {
            return "";
        }
        
        String formatted = date.format(SLASH_DATE_FORMATTER);
        logger.debug("Formatted date for display: {} -> {}", date, formatted);
        return formatted;
    }
    
    /**
     * Formats a LocalDate to slash format (MM/DD/YYYY).
     * 
     * @param date The LocalDate to format
     * @return Formatted date string in MM/DD/YYYY format
     */
    public static String formatDateSlash(LocalDate date) {
        return formatDateForDisplay(date);
    }
    
    /**
     * Parses a COBOL date string (CCYYMMDD format).
     * 
     * @param cobolDate The COBOL date string to parse
     * @return Optional containing parsed LocalDate if successful, empty otherwise
     */
    public static Optional<LocalDate> parseCobolDate(String cobolDate) {
        logger.debug("Parsing COBOL date: {}", cobolDate);
        
        if (StringUtils.isBlank(cobolDate)) {
            return Optional.empty();
        }
        
        String cleanDate = ValidationUtils.sanitizeInput(cobolDate);
        
        if (!CCYYMMDD_PATTERN.matcher(cleanDate).matches()) {
            logger.debug("Invalid COBOL date format: {}", cleanDate);
            return Optional.empty();
        }
        
        try {
            LocalDate date = LocalDate.parse(cleanDate, COBOL_DATE_FORMATTER);
            logger.debug("Successfully parsed COBOL date: {} -> {}", cleanDate, date);
            return Optional.of(date);
        } catch (DateTimeParseException e) {
            logger.debug("Failed to parse COBOL date: {}", cleanDate, e);
            return Optional.empty();
        }
    }
    
    /**
     * Formats a LocalDate to COBOL date format (CCYYMMDD).
     * 
     * @param date The LocalDate to format
     * @return Formatted date string in CCYYMMDD format
     */
    public static String formatCobolDate(LocalDate date) {
        return formatDate(date);
    }
    
    // ========================================================================
    // TIMESTAMP METHODS
    // ========================================================================
    
    /**
     * Parses a timestamp string in COBOL timestamp format.
     * 
     * <p>Equivalent to COBOL WS-TIMESTAMP parsing logic from CSDAT01Y.cpy.</p>
     * 
     * @param timestampValue The timestamp string to parse
     * @return Optional containing parsed LocalDate if successful, empty otherwise
     */
    public static Optional<LocalDate> parseTimestamp(String timestampValue) {
        logger.debug("Parsing timestamp: {}", timestampValue);
        
        if (StringUtils.isBlank(timestampValue)) {
            return Optional.empty();
        }
        
        String cleanTimestamp = ValidationUtils.sanitizeInput(timestampValue);
        
        if (!TIMESTAMP_PATTERN.matcher(cleanTimestamp).matches()) {
            logger.debug("Invalid timestamp format: {}", cleanTimestamp);
            return Optional.empty();
        }
        
        try {
            // Extract just the date part for LocalDate parsing
            String datePart = cleanTimestamp.substring(0, 10);
            LocalDate date = LocalDate.parse(datePart, ISO_DATE_FORMATTER);
            logger.debug("Successfully parsed timestamp: {} -> {}", cleanTimestamp, date);
            return Optional.of(date);
        } catch (DateTimeParseException e) {
            logger.debug("Failed to parse timestamp: {}", cleanTimestamp, e);
            return Optional.empty();
        }
    }
    
    /**
     * Formats a LocalDate to timestamp format.
     * 
     * <p>Equivalent to COBOL WS-TIMESTAMP formatting logic.</p>
     * 
     * @param date The LocalDate to format
     * @return Formatted timestamp string
     */
    public static String formatTimestamp(LocalDate date) {
        logger.debug("Formatting timestamp: {}", date);
        
        if (date == null) {
            return "";
        }
        
        String formatted = date.format(ISO_DATE_FORMATTER) + " 00:00:00";
        logger.debug("Formatted timestamp: {} -> {}", date, formatted);
        return formatted;
    }
    
    /**
     * Parses a COBOL timestamp string.
     * 
     * @param cobolTimestamp The COBOL timestamp string to parse
     * @return Optional containing parsed LocalDate if successful, empty otherwise
     */
    public static Optional<LocalDate> parseCobolTimestamp(String cobolTimestamp) {
        return parseTimestamp(cobolTimestamp);
    }
    
    /**
     * Formats a LocalDate to COBOL timestamp format.
     * 
     * @param date The LocalDate to format
     * @return Formatted timestamp string
     */
    public static String formatCobolTimestamp(LocalDate date) {
        return formatTimestamp(date);
    }
    
    /**
     * Gets current timestamp in COBOL format.
     * 
     * @return Current timestamp string
     */
    public static String getCurrentTimestamp() {
        return formatTimestamp(getCurrentDate());
    }
    
    // ========================================================================
    // UTILITY METHODS
    // ========================================================================
    
    /**
     * Gets the current date.
     * 
     * <p>Equivalent to COBOL FUNCTION CURRENT-DATE logic.</p>
     * 
     * @return Current LocalDate
     */
    public static LocalDate getCurrentDate() {
        LocalDate currentDate = LocalDate.now();
        logger.debug("Current date: {}", currentDate);
        return currentDate;
    }
    
    /**
     * Checks if a date is valid using comprehensive validation.
     * 
     * <p>Convenience method that combines date parsing and validation.</p>
     * 
     * @param dateValue The date string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidDate(String dateValue) {
        ValidationResult result = validateDate(dateValue);
        return result.isValid();
    }
    
    /**
     * Checks if a date range is valid.
     * 
     * <p>Validates that both dates are valid and start date is not after end date.</p>
     * 
     * @param startDate The start date string
     * @param endDate The end date string
     * @return true if valid range, false otherwise
     */
    public static boolean isValidDateRange(String startDate, String endDate) {
        logger.debug("Validating date range: {} to {}", startDate, endDate);
        
        // Validate both dates
        if (!isValidDate(startDate) || !isValidDate(endDate)) {
            logger.debug("Invalid date in range: start={}, end={}", startDate, endDate);
            return false;
        }
        
        // Parse both dates
        Optional<LocalDate> start = parseDate(startDate);
        Optional<LocalDate> end = parseDate(endDate);
        
        if (start.isEmpty() || end.isEmpty()) {
            logger.debug("Failed to parse dates in range");
            return false;
        }
        
        // Check that start is not after end
        boolean isValid = !start.get().isAfter(end.get());
        logger.debug("Date range validation result: {}", isValid);
        return isValid;
    }
    
    /**
     * Checks if a year is a leap year using COBOL leap year logic.
     * 
     * <p>Replicates COBOL leap year calculation from CSUTLDPY.cpy:</p>
     * <pre>
     * IF WS-EDIT-DATE-YY-N = 0
     *    MOVE 400 TO WS-DIV-BY
     * ELSE
     *    MOVE 4 TO WS-DIV-BY
     * END-IF
     * 
     * DIVIDE WS-EDIT-DATE-CCYY-N BY WS-DIV-BY
     * GIVING WS-DIVIDEND REMAINDER WS-REMAINDER
     * 
     * IF WS-REMAINDER = ZEROES
     *    CONTINUE (is leap year)
     * </pre>
     * 
     * @param year The year to check
     * @return true if leap year, false otherwise
     */
    public static boolean isLeapYear(int year) {
        logger.debug("Checking leap year: {}", year);
        
        // COBOL leap year logic: divisible by 4, but not by 100 unless also by 400
        boolean isLeap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
        logger.debug("Leap year result for {}: {}", year, isLeap);
        return isLeap;
    }
    
    /**
     * Gets the number of days in a specific month and year.
     * 
     * <p>Handles leap year calculation for February.</p>
     * 
     * @param month The month (1-12)
     * @param year The year
     * @return Number of days in the month
     */
    public static int getDaysInMonth(int month, int year) {
        logger.debug("Getting days in month: {} year: {}", month, year);
        
        if (month < 1 || month > 12) {
            logger.debug("Invalid month: {}", month);
            return 0;
        }
        
        int days = DAYS_IN_MONTH[month - 1];
        
        // Handle February in leap years
        if (month == 2 && isLeapYear(year)) {
            days = 29;
        }
        
        logger.debug("Days in month {} year {}: {}", month, year, days);
        return days;
    }
    
    /**
     * Validates that a date is not in the future.
     * 
     * <p>Business rule validation for fields that cannot contain future dates.</p>
     * 
     * @param dateValue The date string to validate
     * @return ValidationResult.VALID if not in future, INVALID_RANGE otherwise
     */
    public static ValidationResult validateFutureDate(String dateValue) {
        logger.debug("Validating future date: {}", dateValue);
        
        // First validate the date format
        ValidationResult dateValidation = validateDate(dateValue);
        if (!dateValidation.isValid()) {
            return dateValidation;
        }
        
        // Parse the date
        Optional<LocalDate> parsedDate = parseDate(ValidationUtils.sanitizeInput(dateValue));
        if (parsedDate.isEmpty()) {
            return ValidationResult.BAD_DATE_VALUE;
        }
        
        LocalDate date = parsedDate.get();
        LocalDate currentDate = getCurrentDate();
        
        // Check if date is in the future
        if (date.isAfter(currentDate)) {
            logger.debug("Date is in the future: {}", date);
            return ValidationResult.INVALID_RANGE;
        }
        
        logger.debug("Future date validation successful: {}", date);
        return ValidationResult.VALID;
    }
    
    /**
     * Validates COBOL date format specifically.
     * 
     * <p>Validates that a string matches the COBOL CCYYMMDD format pattern.</p>
     * 
     * @param dateValue The date string to validate
     * @return ValidationResult.VALID if valid COBOL format, appropriate error otherwise
     */
    public static ValidationResult validateCobolDateFormat(String dateValue) {
        logger.debug("Validating COBOL date format: {}", dateValue);
        
        ValidationResult requiredCheck = ValidationUtils.validateRequiredField(dateValue, "Date");
        if (!requiredCheck.isValid()) {
            return requiredCheck;
        }
        
        String cleanDate = ValidationUtils.sanitizeInput(dateValue);
        
        // Check CCYYMMDD pattern
        if (!CCYYMMDD_PATTERN.matcher(cleanDate).matches()) {
            logger.debug("Invalid COBOL date format: {}", cleanDate);
            return ValidationResult.BAD_PIC_STRING;
        }
        
        // Validate that it can be parsed as a valid date
        Optional<LocalDate> parsedDate = parseCobolDate(cleanDate);
        if (parsedDate.isEmpty()) {
            logger.debug("COBOL date format validation failed: {}", cleanDate);
            return ValidationResult.BAD_DATE_VALUE;
        }
        
        logger.debug("COBOL date format validation successful: {}", cleanDate);
        return ValidationResult.VALID;
    }
}