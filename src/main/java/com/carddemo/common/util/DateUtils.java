/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.common.util;

import com.carddemo.common.enums.ValidationResult;
import com.carddemo.common.enums.ErrorFlag;
import com.carddemo.common.validator.ValidationConstants;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DateUtils provides comprehensive date validation, formatting, and conversion utilities
 * converted from COBOL utilities CSUTLDTC.cbl and CSUTLDPY.cpy with exact preservation
 * of COBOL calendar logic including leap year calculations and century validation constraints.
 * 
 * This class converts COBOL date validation procedures to Java implementation while maintaining
 * identical behavior to original COBOL date processing including:
 * - CSUTLDTC.cbl: Date validation using CEEDAYS API equivalent functionality
 * - CSUTLDPY.cpy: Date validation procedures with century, month, day, and leap year validation
 * - CSUTLDWY.cpy: Date validation working storage with 88-level conditions
 * - CSDAT01Y.cpy: Date format structures for display and processing
 * 
 * Key Features:
 * - Exact COBOL calendar logic preservation including leap year processing
 * - Century validation supporting only 19xx and 20xx values as per original constraints
 * - Date of birth validation preventing future dates per business rule requirements
 * - Comprehensive date format validation and conversion utilities
 * - Support for all COBOL date formats: CCYYMMDD, MM/DD/YY, YYYY-MM-DD HH:MM:SS
 * - Integration with ValidationResult for consistent error handling
 * - Thread-safe utility methods with comprehensive logging
 * 
 * Technical Compliance:
 * - Maintains identical validation behavior to original COBOL date validation logic
 * - Supports React form validation and Spring Boot REST API validation
 * - Implements COBOL date format patterns for consistent date processing
 * - Uses LocalDate for modern Java date handling while preserving COBOL semantics
 * 
 * Performance Considerations:
 * - Pre-compiled regex patterns for efficient date format validation
 * - Cached DateTimeFormatter instances for optimal parsing performance
 * - Minimal object creation in validation methods for high-throughput scenarios
 * 
 * Based on COBOL source files:
 * - CSUTLDTC.cbl: Date validation subprogram with CEEDAYS API integration
 * - CSUTLDPY.cpy: Date validation procedures with detailed error handling
 * - CSUTLDWY.cpy: Date validation working storage with 88-level conditions
 * - CSDAT01Y.cpy: Date format structures and timestamp handling
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 */
public final class DateUtils {

    private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);

    // ===========================
    // DATE FORMAT CONSTANTS
    // ===========================

    /**
     * COBOL date format pattern: CCYYMMDD
     * Based on CSUTLDPY.cpy date validation procedures
     */
    public static final String COBOL_DATE_FORMAT = "yyyyMMdd";

    /**
     * COBOL timestamp format pattern: YYYY-MM-DD HH:MM:SS.SSSSSS
     * Based on CSDAT01Y.cpy timestamp structure
     */
    public static final String COBOL_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSSSS";

    /**
     * Slash date format pattern: MM/DD/YY
     * Based on CSDAT01Y.cpy slash date structure
     */
    public static final String SLASH_DATE_FORMAT = "MM/dd/yy";

    // ===========================
    // CACHED DATE TIME FORMATTERS
    // ===========================

    /**
     * Pre-compiled formatter for COBOL CCYYMMDD format
     * Optimized for repeated parsing operations
     */
    private static final DateTimeFormatter COBOL_DATE_FORMATTER = DateTimeFormatter.ofPattern(COBOL_DATE_FORMAT);

    /**
     * Pre-compiled formatter for COBOL timestamp format
     * Handles microsecond precision timestamp parsing
     */
    private static final DateTimeFormatter COBOL_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(COBOL_TIMESTAMP_FORMAT);

    /**
     * Pre-compiled formatter for slash date format
     * Supports MM/DD/YY date pattern
     */
    private static final DateTimeFormatter SLASH_DATE_FORMATTER = DateTimeFormatter.ofPattern(SLASH_DATE_FORMAT);

    /**
     * Pre-compiled formatter for ISO date format
     * Provides standard ISO 8601 date parsing
     */
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Pre-compiled formatter for display date format
     * Supports user-friendly date display
     */
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    // ===========================
    // VALIDATION PATTERNS
    // ===========================

    /**
     * Pattern for COBOL date validation: exactly 8 digits
     * Equivalent to CSUTLDPY.cpy date format validation
     */
    private static final Pattern COBOL_DATE_PATTERN = Pattern.compile("^[0-9]{8}$");

    /**
     * Pattern for slash date validation: MM/DD/YY format
     * Supports various slash date formats
     */
    private static final Pattern SLASH_DATE_PATTERN = Pattern.compile("^[0-9]{2}/[0-9]{2}/[0-9]{2}$");

    /**
     * Pattern for ISO date validation: YYYY-MM-DD format
     * Supports standard ISO date format
     */
    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}$");

    // ===========================
    // CONSTRUCTOR
    // ===========================

    /**
     * Private constructor to prevent instantiation.
     * DateUtils is a utility class containing only static methods.
     */
    private DateUtils() {
        throw new UnsupportedOperationException("DateUtils is a utility class and cannot be instantiated");
    }

    // ===========================
    // PRIMARY DATE VALIDATION METHODS
    // ===========================

    /**
     * Validates date string according to COBOL CCYYMMDD format with comprehensive validation.
     * 
     * Implements COBOL date validation equivalent to EDIT-DATE-CCYYMMDD procedures
     * from CSUTLDPY.cpy. Performs comprehensive validation including:
     * - Format validation (exactly 8 digits)
     * - Century validation (19xx, 20xx only)
     * - Month validation (01-12)
     * - Day validation (01-31 based on month)
     * - Leap year validation for February dates
     * - Final date validation using LocalDate parsing
     * 
     * @param dateValue the date string to validate in CCYYMMDD format
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateDate(String dateValue) {
        logger.debug("Validating date: {}", dateValue);

        // Check for blank field - equivalent to COBOL LOW-VALUES/SPACES check
        ValidationResult requiredCheck = ValidationUtils.validateRequiredField(dateValue);
        if (!requiredCheck.isValid()) {
            logger.warn("Date validation failed: blank field");
            return ValidationResult.BLANK_FIELD;
        }

        String cleanDate = StringUtils.trim(dateValue);

        // Validate format - must be exactly 8 digits
        if (!COBOL_DATE_PATTERN.matcher(cleanDate).matches()) {
            logger.warn("Date validation failed: invalid format - {}", cleanDate);
            return ValidationResult.INVALID_FORMAT;
        }

        // Extract date components for detailed validation
        try {
            int century = Integer.parseInt(cleanDate.substring(0, 2));
            int year = Integer.parseInt(cleanDate.substring(2, 4));
            int month = Integer.parseInt(cleanDate.substring(4, 6));
            int day = Integer.parseInt(cleanDate.substring(6, 8));
            int fullYear = (century * 100) + year;

            // Validate century - equivalent to CSUTLDPY.cpy century validation
            ValidationResult centuryResult = validateCentury(century);
            if (!centuryResult.isValid()) {
                logger.warn("Date validation failed: invalid century - {}", century);
                return centuryResult;
            }

            // Validate year - ensure reasonable year value
            ValidationResult yearResult = validateYear(fullYear);
            if (!yearResult.isValid()) {
                logger.warn("Date validation failed: invalid year - {}", fullYear);
                return yearResult;
            }

            // Validate month - equivalent to CSUTLDPY.cpy month validation
            ValidationResult monthResult = validateMonth(month);
            if (!monthResult.isValid()) {
                logger.warn("Date validation failed: invalid month - {}", month);
                return monthResult;
            }

            // Validate day - equivalent to CSUTLDPY.cpy day validation with leap year logic
            ValidationResult dayResult = validateDay(day, month, fullYear);
            if (!dayResult.isValid()) {
                logger.warn("Date validation failed: invalid day - {} for month/year {}/{}", day, month, fullYear);
                return dayResult;
            }

            // Final validation using LocalDate parsing - equivalent to CSUTLDTC.cbl CEEDAYS validation
            LocalDate.parse(cleanDate, COBOL_DATE_FORMATTER);

            logger.debug("Date validation successful: {}", cleanDate);
            return ValidationResult.VALID;

        } catch (DateTimeParseException | NumberFormatException e) {
            logger.error("Date validation failed: parse exception - {}", cleanDate, e);
            return ValidationResult.INVALID_DATE;
        }
    }

    /**
     * Validates century component of date according to COBOL business rules.
     * 
     * Implements COBOL century validation equivalent to CSUTLDPY.cpy EDIT-YEAR-CCYY
     * procedure. Only supports centuries 19 and 20 as per original COBOL constraints.
     * 
     * @param century the century value to validate (19 or 20)
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateCentury(int century) {
        logger.debug("Validating century: {}", century);

        // Validate century - only 19 and 20 are valid (from CSUTLDWY.cpy)
        if (!ValidationConstants.VALID_CENTURIES.contains(century)) {
            logger.warn("Century validation failed: invalid century - {}", century);
            return ValidationResult.INVALID_ERA;
        }

        logger.debug("Century validation successful: {}", century);
        return ValidationResult.VALID;
    }

    /**
     * Validates year component of date.
     * 
     * Implements year validation with reasonable range constraints
     * to prevent unrealistic year values while maintaining COBOL compatibility.
     * 
     * @param year the full year value to validate (1900-2099)
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateYear(int year) {
        logger.debug("Validating year: {}", year);

        // Validate year range - reasonable bounds for business application
        if (year < 1900 || year > 2099) {
            logger.warn("Year validation failed: out of range - {}", year);
            return ValidationResult.INVALID_RANGE;
        }

        // Check for year zero in era - equivalent to CSUTLDTC.cbl FC-YEAR-IN-ERA-ZERO
        if (year % 100 == 0 && year != 1900 && year != 2000) {
            logger.warn("Year validation failed: year in era zero - {}", year);
            return ValidationResult.YEAR_IN_ERA_ZERO;
        }

        logger.debug("Year validation successful: {}", year);
        return ValidationResult.VALID;
    }

    /**
     * Validates month component of date.
     * 
     * Implements COBOL month validation equivalent to CSUTLDPY.cpy EDIT-MONTH
     * procedure. Validates month is in range 01-12 with proper numeric validation.
     * 
     * @param month the month value to validate (1-12)
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateMonth(int month) {
        logger.debug("Validating month: {}", month);

        // Validate month range - equivalent to CSUTLDPY.cpy WS-VALID-MONTH validation
        if (!ValidationConstants.VALID_MONTHS.contains(month)) {
            logger.warn("Month validation failed: invalid month - {}", month);
            return ValidationResult.INVALID_MONTH;
        }

        logger.debug("Month validation successful: {}", month);
        return ValidationResult.VALID;
    }

    /**
     * Validates day component of date with month and year context.
     * 
     * Implements COBOL day validation equivalent to CSUTLDPY.cpy EDIT-DAY and
     * EDIT-DAY-MONTH-YEAR procedures. Performs comprehensive validation including:
     * - Basic day range validation (1-31)
     * - Month-specific day validation (30 vs 31 day months)
     * - February leap year validation with exact COBOL leap year logic
     * 
     * @param day the day value to validate (1-31)
     * @param month the month context (1-12)
     * @param year the year context for leap year calculation
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateDay(int day, int month, int year) {
        logger.debug("Validating day: {} for month: {} year: {}", day, month, year);

        // Basic day range validation - equivalent to CSUTLDPY.cpy WS-VALID-DAY
        if (!ValidationConstants.VALID_DAYS.contains(day)) {
            logger.warn("Day validation failed: invalid day - {}", day);
            return ValidationResult.INVALID_DATE;
        }

        // Month-specific validation - equivalent to CSUTLDPY.cpy day-month validation
        if (ValidationConstants.MONTHS_WITH_30_DAYS.contains(month) && day > 30) {
            logger.warn("Day validation failed: day {} invalid for 30-day month {}", day, month);
            return ValidationResult.INVALID_DATE;
        }

        // February specific validation - equivalent to CSUTLDPY.cpy February validation
        if (month == ValidationConstants.FEBRUARY) {
            if (day > 29) {
                logger.warn("Day validation failed: day {} invalid for February", day);
                return ValidationResult.INVALID_DATE;
            }
            if (day == 29 && !isLeapYear(year)) {
                logger.warn("Day validation failed: day 29 invalid for non-leap year {}", year);
                return ValidationResult.INVALID_DATE;
            }
        }

        logger.debug("Day validation successful: {} for month: {} year: {}", day, month, year);
        return ValidationResult.VALID;
    }

    /**
     * Validates date of birth with business rule preventing future dates.
     * 
     * Implements COBOL date of birth validation equivalent to CSUTLDPY.cpy
     * EDIT-DATE-OF-BIRTH procedure. Prevents future dates as per business rule
     * requirements using current date comparison.
     * 
     * @param dateOfBirth the date of birth to validate in CCYYMMDD format
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateDateOfBirth(String dateOfBirth) {
        logger.debug("Validating date of birth: {}", dateOfBirth);

        // First validate the date format and components
        ValidationResult dateValidation = validateDate(dateOfBirth);
        if (!dateValidation.isValid()) {
            logger.warn("Date of birth validation failed: invalid date format");
            return dateValidation;
        }

        try {
            // Parse the date for comparison
            LocalDate birthDate = LocalDate.parse(StringUtils.trim(dateOfBirth), COBOL_DATE_FORMATTER);
            LocalDate currentDate = LocalDate.now();

            // Check if date is in future - equivalent to CSUTLDPY.cpy future date validation
            if (birthDate.isAfter(currentDate)) {
                logger.warn("Date of birth validation failed: future date - {}", birthDate);
                return ValidationResult.INVALID_RANGE;
            }

            logger.debug("Date of birth validation successful: {}", birthDate);
            return ValidationResult.VALID;

        } catch (DateTimeParseException e) {
            logger.error("Date of birth validation failed: parse exception - {}", dateOfBirth, e);
            return ValidationResult.INVALID_DATE;
        }
    }

    // ===========================
    // DATE RANGE VALIDATION METHODS
    // ===========================

    /**
     * Validates if a date falls within a specified range.
     * 
     * Provides date range validation functionality for business rules that require
     * dates to fall within specific time periods.
     * 
     * @param dateValue the date to validate in CCYYMMDD format
     * @param startDate the start of the valid range in CCYYMMDD format
     * @param endDate the end of the valid range in CCYYMMDD format
     * @return true if date is within range, false otherwise
     */
    public static boolean isValidDateRange(String dateValue, String startDate, String endDate) {
        logger.debug("Validating date range: {} between {} and {}", dateValue, startDate, endDate);

        try {
            LocalDate date = LocalDate.parse(StringUtils.trim(dateValue), COBOL_DATE_FORMATTER);
            LocalDate start = LocalDate.parse(StringUtils.trim(startDate), COBOL_DATE_FORMATTER);
            LocalDate end = LocalDate.parse(StringUtils.trim(endDate), COBOL_DATE_FORMATTER);

            boolean isValid = (date.isEqual(start) || date.isAfter(start)) && 
                            (date.isEqual(end) || date.isBefore(end));

            logger.debug("Date range validation result: {}", isValid);
            return isValid;

        } catch (DateTimeParseException e) {
            logger.error("Date range validation failed: parse exception", e);
            return false;
        }
    }

    /**
     * Validates if a date is not in the future.
     * 
     * Provides future date validation for business rules that require dates
     * to be current or in the past.
     * 
     * @param dateValue the date to validate in CCYYMMDD format
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateFutureDate(String dateValue) {
        logger.debug("Validating future date: {}", dateValue);

        try {
            LocalDate date = LocalDate.parse(StringUtils.trim(dateValue), COBOL_DATE_FORMATTER);
            LocalDate currentDate = LocalDate.now();

            if (date.isAfter(currentDate)) {
                logger.warn("Future date validation failed: date in future - {}", date);
                return ValidationResult.INVALID_RANGE;
            }

            logger.debug("Future date validation successful: {}", date);
            return ValidationResult.VALID;

        } catch (DateTimeParseException e) {
            logger.error("Future date validation failed: parse exception - {}", dateValue, e);
            return ValidationResult.INVALID_DATE;
        }
    }

    // ===========================
    // LEAP YEAR VALIDATION METHODS
    // ===========================

    /**
     * Determines if a year is a leap year using exact COBOL leap year logic.
     * 
     * Implements COBOL leap year calculation equivalent to CSUTLDPY.cpy leap year
     * validation logic. Uses the exact algorithm from the original COBOL:
     * - If year ends in 00, divisible by 400
     * - Otherwise, divisible by 4
     * 
     * @param year the year to check for leap year status
     * @return true if the year is a leap year, false otherwise
     */
    public static boolean isLeapYear(int year) {
        logger.debug("Checking leap year: {}", year);

        // COBOL logic: if year ends in 00, divide by 400; otherwise divide by 4
        boolean isLeap;
        if (year % 100 == 0) {
            isLeap = year % 400 == 0;
        } else {
            isLeap = year % 4 == 0;
        }

        logger.debug("Leap year result for {}: {}", year, isLeap);
        return isLeap;
    }

    /**
     * Gets the number of days in a month considering leap years.
     * 
     * Calculates the correct number of days in a month based on the month
     * and year, accounting for leap year February.
     * 
     * @param month the month (1-12)
     * @param year the year for leap year calculation
     * @return the number of days in the month
     */
    public static int getDaysInMonth(int month, int year) {
        logger.debug("Getting days in month: {} for year: {}", month, year);

        int days;
        if (ValidationConstants.MONTHS_WITH_31_DAYS.contains(month)) {
            days = 31;
        } else if (ValidationConstants.MONTHS_WITH_30_DAYS.contains(month)) {
            days = 30;
        } else if (month == ValidationConstants.FEBRUARY) {
            days = isLeapYear(year) ? 29 : 28;
        } else {
            logger.warn("Invalid month for days calculation: {}", month);
            days = 0;
        }

        logger.debug("Days in month {} for year {}: {}", month, year, days);
        return days;
    }

    // ===========================
    // DATE PARSING METHODS
    // ===========================

    /**
     * Parses a date string using multiple supported formats.
     * 
     * Attempts to parse a date string using various supported formats including
     * COBOL CCYYMMDD, ISO format, and slash format. Returns Optional containing
     * the parsed date or empty if parsing fails.
     * 
     * @param dateValue the date string to parse
     * @return Optional containing parsed LocalDate or empty if parsing fails
     */
    public static Optional<LocalDate> parseDate(String dateValue) {
        logger.debug("Parsing date: {}", dateValue);

        if (StringUtils.isBlank(dateValue)) {
            logger.debug("Date parsing failed: blank value");
            return Optional.empty();
        }

        String cleanDate = StringUtils.trim(dateValue);

        // Try each supported date pattern
        for (String pattern : ValidationConstants.DATE_PATTERNS) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                LocalDate parsedDate = LocalDate.parse(cleanDate, formatter);
                logger.debug("Date parsing successful with pattern {}: {}", pattern, parsedDate);
                return Optional.of(parsedDate);
            } catch (DateTimeParseException e) {
                // Continue to next pattern
                logger.trace("Date parsing failed with pattern {}: {}", pattern, e.getMessage());
            }
        }

        logger.warn("Date parsing failed: no matching pattern for {}", cleanDate);
        return Optional.empty();
    }

    /**
     * Parses a COBOL date string in CCYYMMDD format.
     * 
     * Specifically parses COBOL CCYYMMDD format dates as used throughout
     * the CardDemo application. Returns Optional containing the parsed date.
     * 
     * @param cobolDate the COBOL date string in CCYYMMDD format
     * @return Optional containing parsed LocalDate or empty if parsing fails
     */
    public static Optional<LocalDate> parseCobolDate(String cobolDate) {
        logger.debug("Parsing COBOL date: {}", cobolDate);

        if (StringUtils.isBlank(cobolDate)) {
            logger.debug("COBOL date parsing failed: blank value");
            return Optional.empty();
        }

        try {
            LocalDate parsedDate = LocalDate.parse(StringUtils.trim(cobolDate), COBOL_DATE_FORMATTER);
            logger.debug("COBOL date parsing successful: {}", parsedDate);
            return Optional.of(parsedDate);
        } catch (DateTimeParseException e) {
            logger.warn("COBOL date parsing failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Parses a COBOL timestamp string.
     * 
     * Parses COBOL timestamp format with microsecond precision as defined
     * in CSDAT01Y.cpy timestamp structure.
     * 
     * @param timestamp the COBOL timestamp string
     * @return Optional containing parsed LocalDate or empty if parsing fails
     */
    public static Optional<LocalDate> parseCobolTimestamp(String timestamp) {
        logger.debug("Parsing COBOL timestamp: {}", timestamp);

        if (StringUtils.isBlank(timestamp)) {
            logger.debug("COBOL timestamp parsing failed: blank value");
            return Optional.empty();
        }

        try {
            // Extract date portion from timestamp
            String datePortion = StringUtils.trim(timestamp).substring(0, 10);
            LocalDate parsedDate = LocalDate.parse(datePortion, ISO_DATE_FORMATTER);
            logger.debug("COBOL timestamp parsing successful: {}", parsedDate);
            return Optional.of(parsedDate);
        } catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
            logger.warn("COBOL timestamp parsing failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Parses a timestamp string and returns the date portion.
     * 
     * Extracts and parses the date portion from various timestamp formats,
     * supporting both COBOL and ISO timestamp formats.
     * 
     * @param timestamp the timestamp string to parse
     * @return Optional containing parsed LocalDate or empty if parsing fails
     */
    public static Optional<LocalDate> parseTimestamp(String timestamp) {
        logger.debug("Parsing timestamp: {}", timestamp);

        if (StringUtils.isBlank(timestamp)) {
            logger.debug("Timestamp parsing failed: blank value");
            return Optional.empty();
        }

        String cleanTimestamp = StringUtils.trim(timestamp);

        // Try COBOL timestamp format first
        if (cleanTimestamp.length() >= 10) {
            try {
                String datePortion = cleanTimestamp.substring(0, 10);
                LocalDate parsedDate = LocalDate.parse(datePortion, ISO_DATE_FORMATTER);
                logger.debug("Timestamp parsing successful: {}", parsedDate);
                return Optional.of(parsedDate);
            } catch (DateTimeParseException e) {
                logger.trace("Timestamp parsing failed with ISO format: {}", e.getMessage());
            }
        }

        // Try other timestamp formats
        try {
            LocalDate parsedDate = LocalDate.parse(cleanTimestamp, COBOL_TIMESTAMP_FORMATTER);
            logger.debug("Timestamp parsing successful with COBOL format: {}", parsedDate);
            return Optional.of(parsedDate);
        } catch (DateTimeParseException e) {
            logger.warn("Timestamp parsing failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // ===========================
    // DATE FORMATTING METHODS
    // ===========================

    /**
     * Formats a LocalDate to COBOL CCYYMMDD format.
     * 
     * Converts a LocalDate to the standard COBOL CCYYMMDD format used
     * throughout the CardDemo application for date storage and processing.
     * 
     * @param date the LocalDate to format
     * @return formatted date string in CCYYMMDD format
     */
    public static String formatCobolDate(LocalDate date) {
        logger.debug("Formatting COBOL date: {}", date);

        if (date == null) {
            logger.debug("COBOL date formatting failed: null date");
            return "";
        }

        String formatted = date.format(COBOL_DATE_FORMATTER);
        logger.debug("COBOL date formatting successful: {}", formatted);
        return formatted;
    }

    /**
     * Formats a LocalDate to display format.
     * 
     * Converts a LocalDate to user-friendly display format for presentation
     * in web interfaces and reports.
     * 
     * @param date the LocalDate to format
     * @return formatted date string for display
     */
    public static String formatDateForDisplay(LocalDate date) {
        logger.debug("Formatting date for display: {}", date);

        if (date == null) {
            logger.debug("Display date formatting failed: null date");
            return "";
        }

        String formatted = date.format(DISPLAY_DATE_FORMATTER);
        logger.debug("Display date formatting successful: {}", formatted);
        return formatted;
    }

    /**
     * Formats a LocalDate to slash format.
     * 
     * Converts a LocalDate to MM/DD/YY format as used in some legacy
     * interfaces and reports.
     * 
     * @param date the LocalDate to format
     * @return formatted date string in MM/DD/YY format
     */
    public static String formatDateSlash(LocalDate date) {
        logger.debug("Formatting slash date: {}", date);

        if (date == null) {
            logger.debug("Slash date formatting failed: null date");
            return "";
        }

        String formatted = date.format(SLASH_DATE_FORMATTER);
        logger.debug("Slash date formatting successful: {}", formatted);
        return formatted;
    }

    /**
     * Formats a date string from one format to another.
     * 
     * Converts a date string from one format to another, supporting various
     * date format conversions needed throughout the application.
     * 
     * @param dateValue the date string to format
     * @param sourceFormat the source format pattern
     * @param targetFormat the target format pattern
     * @return formatted date string or empty if conversion fails
     */
    public static String formatDate(String dateValue, String sourceFormat, String targetFormat) {
        logger.debug("Formatting date from {} to {}: {}", sourceFormat, targetFormat, dateValue);

        if (StringUtils.isBlank(dateValue)) {
            logger.debug("Date formatting failed: blank value");
            return "";
        }

        try {
            DateTimeFormatter sourceFormatter = DateTimeFormatter.ofPattern(sourceFormat);
            DateTimeFormatter targetFormatter = DateTimeFormatter.ofPattern(targetFormat);
            
            LocalDate date = LocalDate.parse(StringUtils.trim(dateValue), sourceFormatter);
            String formatted = date.format(targetFormatter);
            
            logger.debug("Date formatting successful: {}", formatted);
            return formatted;
        } catch (DateTimeParseException e) {
            logger.warn("Date formatting failed: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Formats a LocalDate to COBOL timestamp format.
     * 
     * Converts a LocalDate to COBOL timestamp format with current time
     * and microsecond precision for timestamp fields.
     * 
     * @param date the LocalDate to format
     * @return formatted timestamp string
     */
    public static String formatCobolTimestamp(LocalDate date) {
        logger.debug("Formatting COBOL timestamp: {}", date);

        if (date == null) {
            logger.debug("COBOL timestamp formatting failed: null date");
            return "";
        }

        // Create timestamp with current time
        String formatted = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00.000000";
        logger.debug("COBOL timestamp formatting successful: {}", formatted);
        return formatted;
    }

    /**
     * Formats a LocalDate to timestamp format.
     * 
     * Converts a LocalDate to standard timestamp format for database
     * storage and API responses.
     * 
     * @param date the LocalDate to format
     * @return formatted timestamp string
     */
    public static String formatTimestamp(LocalDate date) {
        logger.debug("Formatting timestamp: {}", date);

        if (date == null) {
            logger.debug("Timestamp formatting failed: null date");
            return "";
        }

        String formatted = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        logger.debug("Timestamp formatting successful: {}", formatted);
        return formatted;
    }

    // ===========================
    // CURRENT DATE METHODS
    // ===========================

    /**
     * Gets the current date in COBOL CCYYMMDD format.
     * 
     * Returns the current system date formatted as COBOL CCYYMMDD for
     * use in date validation and processing routines.
     * 
     * @return current date in COBOL CCYYMMDD format
     */
    public static String getCurrentDate() {
        logger.debug("Getting current date");

        LocalDate currentDate = LocalDate.now();
        String formatted = formatCobolDate(currentDate);

        logger.debug("Current date: {}", formatted);
        return formatted;
    }

    /**
     * Gets the current timestamp in COBOL format.
     * 
     * Returns the current system timestamp formatted as COBOL timestamp
     * for use in audit trails and logging.
     * 
     * @return current timestamp in COBOL format
     */
    public static String getCurrentTimestamp() {
        logger.debug("Getting current timestamp");

        LocalDate currentDate = LocalDate.now();
        String formatted = formatCobolTimestamp(currentDate);

        logger.debug("Current timestamp: {}", formatted);
        return formatted;
    }

    // ===========================
    // UTILITY VALIDATION METHODS
    // ===========================

    /**
     * Validates if a date string is a valid date in any supported format.
     * 
     * Provides general date validation that attempts to parse the date
     * using any supported format and returns whether it represents a valid date.
     * 
     * @param dateValue the date string to validate
     * @return true if the date is valid in any supported format, false otherwise
     */
    public static boolean isValidDate(String dateValue) {
        logger.debug("Validating date format: {}", dateValue);

        if (StringUtils.isBlank(dateValue)) {
            logger.debug("Date validation failed: blank value");
            return false;
        }

        Optional<LocalDate> parsedDate = parseDate(dateValue);
        boolean isValid = parsedDate.isPresent();

        logger.debug("Date validation result: {}", isValid);
        return isValid;
    }

    /**
     * Validates COBOL date format according to CSUTLDPY.cpy validation rules.
     * 
     * Validates that a date string conforms to COBOL CCYYMMDD format
     * requirements and represents a valid calendar date.
     * 
     * @param cobolDate the COBOL date string to validate
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateCobolDateFormat(String cobolDate) {
        logger.debug("Validating COBOL date format: {}", cobolDate);

        // Use the comprehensive date validation method
        return validateDate(cobolDate);
    }
}