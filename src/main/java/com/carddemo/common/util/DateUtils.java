/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.carddemo.common.util;

import com.carddemo.common.util.ValidationUtils;
import com.carddemo.common.enums.ValidationResult;
import com.carddemo.common.enums.ErrorFlag;
import com.carddemo.common.validator.ValidationConstants;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DateUtils - Comprehensive date utility class converted from COBOL utilities CSUTLDTC.cbl and CSUTLDPY.cpy,
 * providing date validation, formatting, and conversion with exact preservation of COBOL calendar logic
 * including leap year calculations and century validation constraints.
 * 
 * <p>This utility class maintains absolute functional equivalence with the original COBOL date processing
 * logic while supporting modern Java 21 date/time APIs and Spring Boot integration patterns. All validation
 * rules, error conditions, and business logic have been preserved exactly as implemented in the source
 * COBOL programs.</p>
 * 
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>Exact COBOL date validation logic preservation from CSUTLDPY.cpy</li>
 *   <li>Century validation supporting only 19xx and 20xx values per COBOL constraints</li>
 *   <li>Comprehensive leap year calculations matching COBOL arithmetic behavior</li>
 *   <li>Date of birth validation preventing future dates per business rules</li>
 *   <li>Multiple date format support including COBOL CCYYMMDD and display formats</li>
 *   <li>Integration with ValidationResult enum for consistent error handling</li>
 *   <li>ErrorFlag enum support for COBOL Y/N error flag patterns</li>
 * </ul>
 * 
 * <h3>COBOL Source Mappings:</h3>
 * <ul>
 *   <li>CSUTLDTC.cbl: Main date validation subprogram using CEEDAYS API</li>
 *   <li>CSUTLDPY.cpy: Date validation procedures (EDIT-DATE-CCYYMMDD, EDIT-YEAR-CCYY, etc.)</li>
 *   <li>CSUTLDWY.cpy: Working storage data structures and 88-level conditions</li>
 *   <li>CSDAT01Y.cpy: Date and timestamp format definitions</li>
 * </ul>
 * 
 * <h3>Performance Characteristics:</h3>
 * <ul>
 *   <li>Optimized for high-frequency validation operations (10,000+ TPS)</li>
 *   <li>Pre-compiled regex patterns for format validation</li>
 *   <li>Efficient LocalDate operations with minimal object allocation</li>
 *   <li>Sub-millisecond validation response times for standard date operations</li>
 * </ul>
 * 
 * @author CardDemo Migration Team - Blitzy Platform
 * @version 1.0
 * @since Java 21
 */
public final class DateUtils {

    /**
     * Logger for date validation operations and error tracking.
     * Supports structured logging for date validation debugging and audit trails.
     */
    private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);

    // =======================================================================
    // DATE FORMAT CONSTANTS (from CSDAT01Y.cpy and CSUTLDWY.cpy)
    // =======================================================================

    /**
     * Primary COBOL date format pattern (CCYYMMDD/YYYYMMDD).
     * Equivalent to COBOL WS-DATE-FORMAT VALUE 'YYYYMMDD' from CSUTLDWY.cpy.
     */
    public static final String COBOL_DATE_FORMAT = "yyyyMMdd";

    /**
     * COBOL timestamp format pattern with millisecond precision.
     * Based on WS-TIMESTAMP structure from CSDAT01Y.cpy (YYYY-MM-DD HH:MM:SS.mmmmmm).
     */
    public static final String COBOL_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSSSS";

    /**
     * Slash-separated date format for display purposes.
     * Based on WS-CURDATE-MM-DD-YY structure from CSDAT01Y.cpy (MM/DD/YY).
     */
    public static final String SLASH_DATE_FORMAT = "MM/dd/yyyy";

    // =======================================================================
    // COMPILED DATE TIME FORMATTERS FOR PERFORMANCE
    // =======================================================================

    /**
     * Pre-compiled DateTimeFormatter for COBOL date format (YYYYMMDD).
     * Optimized for high-frequency parsing operations.
     */
    private static final DateTimeFormatter COBOL_DATE_FORMATTER = DateTimeFormatter.ofPattern(COBOL_DATE_FORMAT);

    /**
     * Pre-compiled DateTimeFormatter for COBOL timestamp format.
     * Handles full timestamp parsing with microsecond precision.
     */
    private static final DateTimeFormatter COBOL_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    /**
     * Pre-compiled DateTimeFormatter for slash date format (MM/dd/yyyy).
     * Used for user-friendly date display and input parsing.
     */
    private static final DateTimeFormatter SLASH_DATE_FORMATTER = DateTimeFormatter.ofPattern(SLASH_DATE_FORMAT);

    /**
     * Pre-compiled DateTimeFormatter for ISO date format (yyyy-MM-dd).
     * Used for database storage and API interactions.
     */
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Pre-compiled DateTimeFormatter for time-only format (HH:mm:ss).
     * Based on WS-CURTIME-HH-MM-SS structure from CSDAT01Y.cpy.
     */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // =======================================================================
    // COMPILED REGEX PATTERNS FOR VALIDATION
    // =======================================================================

    /**
     * Pattern for COBOL date format validation (8 digits: YYYYMMDD).
     * Ensures exact 8-digit format before attempting date parsing.
     */
    private static final Pattern COBOL_DATE_PATTERN = Pattern.compile("^[0-9]{8}$");

    /**
     * Pattern for slash date format validation (MM/dd/yyyy or MM/dd/yy).
     * Supports both 2-digit and 4-digit year formats for flexibility.
     */
    private static final Pattern SLASH_DATE_PATTERN = Pattern.compile("^[0-9]{1,2}/[0-9]{1,2}/[0-9]{2,4}$");

    /**
     * Pattern for ISO date format validation (yyyy-MM-dd).
     * Ensures proper hyphen-separated date format.
     */
    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}$");

    // =======================================================================
    // PRIVATE CONSTRUCTOR
    // =======================================================================

    /**
     * Private constructor to prevent instantiation of utility class.
     * DateUtils is designed as a stateless utility class with only static methods.
     */
    private DateUtils() {
        throw new UnsupportedOperationException("DateUtils is a utility class and cannot be instantiated");
    }

    // =======================================================================
    // PRIMARY DATE VALIDATION METHODS (from CSUTLDPY.cpy)
    // =======================================================================

    /**
     * Validates a date string using comprehensive COBOL date validation logic.
     * Implements the complete EDIT-DATE-CCYYMMDD paragraph from CSUTLDPY.cpy.
     * 
     * <p>Validation Rules (from COBOL EDIT-DATE-CCYYMMDD):</p>
     * <ul>
     *   <li>Date format must be CCYYMMDD (8 digits)</li>
     *   <li>Century must be 19 or 20 (THIS-CENTURY or LAST-CENTURY)</li>
     *   <li>Month must be 1-12 (WS-VALID-MONTH)</li>
     *   <li>Day must be 1-31 with month-specific validation (WS-VALID-DAY)</li>
     *   <li>Leap year calculation for February 29th validation</li>
     *   <li>Cross-validation using CEEDAYS equivalent logic</li>
     * </ul>
     * 
     * @param dateValue The date string to validate in CCYYMMDD format
     * @return ValidationResult indicating success or specific validation failure
     */
    public static ValidationResult validateDate(String dateValue) {
        logger.debug("Validating date: {}", dateValue);

        // COBOL equivalent: Check for null/low-values/spaces
        ValidationResult requiredCheck = ValidationUtils.validateRequiredField(dateValue, "date");
        if (!requiredCheck.isValid()) {
            logger.warn("Date validation failed: {}", requiredCheck.getErrorMessage());
            return requiredCheck;
        }

        String trimmedDate = dateValue.trim();

        // COBOL equivalent: Validate 8-digit format (CCYYMMDD)
        if (!COBOL_DATE_PATTERN.matcher(trimmedDate).matches()) {
            logger.warn("Date validation failed: invalid format (must be CCYYMMDD/YYYYMMDD)");
            return ValidationResult.INVALID_FORMAT;
        }

        try {
            // Extract date components (COBOL field extraction equivalent)
            int century = Integer.parseInt(trimmedDate.substring(0, 2));  // CC (WS-EDIT-DATE-CC)
            int year = Integer.parseInt(trimmedDate.substring(2, 4));     // YY (WS-EDIT-DATE-YY)
            int month = Integer.parseInt(trimmedDate.substring(4, 6));    // MM (WS-EDIT-DATE-MM)
            int day = Integer.parseInt(trimmedDate.substring(6, 8));      // DD (WS-EDIT-DATE-DD)

            int fullYear = century * 100 + year; // WS-EDIT-DATE-CCYY-N equivalent

            // COBOL equivalent: EDIT-YEAR-CCYY validation
            ValidationResult yearResult = validateCentury(century);
            if (!yearResult.isValid()) {
                return yearResult;
            }

            // COBOL equivalent: EDIT-MONTH validation
            ValidationResult monthResult = validateMonth(month);
            if (!monthResult.isValid()) {
                return monthResult;
            }

            // COBOL equivalent: EDIT-DAY validation
            ValidationResult dayResult = validateDay(day);
            if (!dayResult.isValid()) {
                return dayResult;
            }

            // COBOL equivalent: EDIT-DAY-MONTH-YEAR validation
            ValidationResult monthDayResult = validateDayForMonth(day, month, fullYear);
            if (!monthDayResult.isValid()) {
                return monthDayResult;
            }

            // COBOL equivalent: EDIT-DATE-LE (CEEDAYS API validation)
            ValidationResult dateParseResult = validateDateParsing(trimmedDate);
            if (!dateParseResult.isValid()) {
                return dateParseResult;
            }

            logger.debug("Date validation successful: {}", dateValue);
            return ValidationResult.VALID;

        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            logger.warn("Date validation failed: parsing error", e);
            return ValidationResult.INVALID_FORMAT;
        }
    }

    /**
     * Validates century component using COBOL THIS-CENTURY/LAST-CENTURY logic.
     * Implements century validation from EDIT-YEAR-CCYY paragraph in CSUTLDPY.cpy.
     * 
     * <p>COBOL Logic (lines 70-84 in CSUTLDPY.cpy):</p>
     * <pre>
     * IF THIS-CENTURY OR LAST-CENTURY
     *    CONTINUE
     * ELSE
     *    SET INPUT-ERROR TO TRUE
     *    STRING 'Century is not valid.' INTO WS-RETURN-MSG
     * </pre>
     * 
     * @param century The century value (19 or 20)
     * @return ValidationResult indicating century validity
     */
    public static ValidationResult validateCentury(int century) {
        logger.debug("Validating century: {}", century);

        // COBOL equivalent: THIS-CENTURY (20) OR LAST-CENTURY (19)
        if (!ValidationConstants.VALID_CENTURIES.contains(century)) {
            logger.warn("Century validation failed: invalid century {} (must be 19 or 20)", century);
            return ValidationResult.INVALID_DATE;
        }

        logger.debug("Century validation successful: {}", century);
        return ValidationResult.VALID;
    }

    /**
     * Validates year component including century and leap year considerations.
     * Implements comprehensive year validation from EDIT-YEAR-CCYY paragraph.
     * 
     * @param year The full year value (1900-2099)
     * @return ValidationResult indicating year validity
     */
    public static ValidationResult validateYear(int year) {
        logger.debug("Validating year: {}", year);

        // Extract century from full year
        int century = year / 100;
        
        ValidationResult centuryResult = validateCentury(century);
        if (!centuryResult.isValid()) {
            return centuryResult;
        }

        // Additional business rule validation for reasonable year range
        if (year < 1900 || year > 2099) {
            logger.warn("Year validation failed: outside reasonable range {}", year);
            return ValidationResult.INVALID_RANGE;
        }

        logger.debug("Year validation successful: {}", year);
        return ValidationResult.VALID;
    }

    /**
     * Validates month component using COBOL WS-VALID-MONTH logic.
     * Implements month validation from EDIT-MONTH paragraph in CSUTLDPY.cpy.
     * 
     * <p>COBOL Logic (lines 111-124 in CSUTLDPY.cpy):</p>
     * <pre>
     * IF WS-VALID-MONTH
     *    CONTINUE
     * ELSE
     *    SET INPUT-ERROR TO TRUE
     *    STRING 'Month must be a number between 1 and 12.' INTO WS-RETURN-MSG
     * </pre>
     * 
     * @param month The month value (1-12)
     * @return ValidationResult indicating month validity
     */
    public static ValidationResult validateMonth(int month) {
        logger.debug("Validating month: {}", month);

        // COBOL equivalent: WS-VALID-MONTH (VALUES 1 THROUGH 12)
        if (!ValidationConstants.VALID_MONTHS.contains(month)) {
            logger.warn("Month validation failed: invalid month {} (must be 1-12)", month);
            return ValidationResult.INVALID_DATE;
        }

        logger.debug("Month validation successful: {}", month);
        return ValidationResult.VALID;
    }

    /**
     * Validates day component using COBOL WS-VALID-DAY logic.
     * Implements day validation from EDIT-DAY paragraph in CSUTLDPY.cpy.
     * 
     * <p>COBOL Logic (lines 187-200 in CSUTLDPY.cpy):</p>
     * <pre>
     * IF WS-VALID-DAY
     *    CONTINUE
     * ELSE
     *    SET INPUT-ERROR TO TRUE
     *    STRING 'day must be a number between 1 and 31.' INTO WS-RETURN-MSG
     * </pre>
     * 
     * @param day The day value (1-31)
     * @return ValidationResult indicating day validity
     */
    public static ValidationResult validateDay(int day) {
        logger.debug("Validating day: {}", day);

        // COBOL equivalent: WS-VALID-DAY (VALUES 1 THROUGH 31)
        if (!ValidationConstants.VALID_DAYS.contains(day)) {
            logger.warn("Day validation failed: invalid day {} (must be 1-31)", day);
            return ValidationResult.INVALID_DATE;
        }

        logger.debug("Day validation successful: {}", day);
        return ValidationResult.VALID;
    }

    /**
     * Validates day for specific month and year combination including leap year logic.
     * Implements EDIT-DAY-MONTH-YEAR paragraph from CSUTLDPY.cpy with exact COBOL leap year calculations.
     * 
     * <p>COBOL Logic (lines 213-272 in CSUTLDPY.cpy):</p>
     * <ul>
     *   <li>31-day month validation: Cannot have 31 days in months without 31 days</li>
     *   <li>February validation: Cannot have 30 days in February</li>
     *   <li>Leap year calculation: WS-DIV-BY = 400 for century years, 4 for others</li>
     *   <li>February 29th: Only valid in leap years</li>
     * </ul>
     * 
     * @param day The day value (1-31)
     * @param month The month value (1-12)
     * @param year The full year value (1900-2099)
     * @return ValidationResult indicating day/month/year combination validity
     */
    private static ValidationResult validateDayForMonth(int day, int month, int year) {
        logger.debug("Validating day {} for month {} year {}", day, month, year);

        // COBOL equivalent: Check for 31 days in non-31-day months
        if (!ValidationConstants.MONTHS_WITH_31_DAYS.contains(month) && day == 31) {
            logger.warn("Day validation failed: cannot have 31 days in month {}", month);
            return ValidationResult.INVALID_DATE;
        }

        // COBOL equivalent: Check for 30 days in February
        if (month == ValidationConstants.FEBRUARY && day == 30) {
            logger.warn("Day validation failed: cannot have 30 days in February");
            return ValidationResult.INVALID_DATE;
        }

        // COBOL equivalent: February 29th leap year validation (lines 243-272)
        if (month == ValidationConstants.FEBRUARY && day == 29) {
            if (!isLeapYear(year)) {
                logger.warn("Day validation failed: February 29th invalid for non-leap year {}", year);
                return ValidationResult.INVALID_DATE;
            }
        }

        logger.debug("Day/month/year validation successful: {}/{}/{}", day, month, year);
        return ValidationResult.VALID;
    }

    /**
     * Validates date parsing using LocalDate equivalent to COBOL CEEDAYS API.
     * Implements EDIT-DATE-LE paragraph from CSUTLDPY.cpy.
     * 
     * <p>COBOL Logic (lines 290-316 in CSUTLDPY.cpy):</p>
     * <pre>
     * CALL 'CSUTLDTC' USING WS-EDIT-DATE-CCYYMMDD, WS-DATE-FORMAT, WS-DATE-VALIDATION-RESULT
     * IF WS-SEVERITY-N = 0
     *    CONTINUE
     * ELSE
     *    SET INPUT-ERROR TO TRUE
     * </pre>
     * 
     * @param dateString The date string in CCYYMMDD format
     * @return ValidationResult indicating parsing validity
     */
    private static ValidationResult validateDateParsing(String dateString) {
        logger.debug("Validating date parsing: {}", dateString);

        try {
            // Equivalent to COBOL CEEDAYS API call - attempt to parse date
            LocalDate.parse(dateString, COBOL_DATE_FORMATTER);
            logger.debug("Date parsing validation successful: {}", dateString);
            return ValidationResult.VALID;
        } catch (DateTimeParseException e) {
            logger.warn("Date parsing validation failed: {}", e.getMessage());
            return ValidationResult.INVALID_DATE;
        }
    }

    /**
     * Validates date of birth ensuring it's not in the future.
     * Implements EDIT-DATE-OF-BIRTH paragraph from CSUTLDPY.cpy.
     * 
     * <p>COBOL Logic (lines 341-368 in CSUTLDPY.cpy):</p>
     * <pre>
     * MOVE FUNCTION CURRENT-DATE TO WS-CURRENT-DATE-YYYYMMDD
     * COMPUTE WS-EDIT-DATE-BINARY = FUNCTION INTEGER-OF-DATE (WS-EDIT-DATE-CCYYMMDD-N)
     * COMPUTE WS-CURRENT-DATE-BINARY = FUNCTION INTEGER-OF-DATE (WS-CURRENT-DATE-YYYYMMDD-N)
     * IF WS-CURRENT-DATE-BINARY > WS-EDIT-DATE-BINARY
     *    CONTINUE
     * ELSE
     *    SET INPUT-ERROR TO TRUE
     *    STRING 'cannot be in the future' INTO WS-RETURN-MSG
     * </pre>
     * 
     * @param dateOfBirth The date of birth string in CCYYMMDD format
     * @return ValidationResult indicating date of birth validity
     */
    public static ValidationResult validateDateOfBirth(String dateOfBirth) {
        logger.debug("Validating date of birth: {}", dateOfBirth);

        // First validate the date format and structure
        ValidationResult basicValidation = validateDate(dateOfBirth);
        if (!basicValidation.isValid()) {
            return basicValidation;
        }

        try {
            // Parse the date of birth
            LocalDate birthDate = LocalDate.parse(dateOfBirth.trim(), COBOL_DATE_FORMATTER);
            LocalDate currentDate = LocalDate.now();

            // COBOL equivalent: Check if birth date is in the future
            if (birthDate.isAfter(currentDate)) {
                logger.warn("Date of birth validation failed: cannot be in the future");
                return ValidationResult.INVALID_DATE;
            }

            logger.debug("Date of birth validation successful: {}", dateOfBirth);
            return ValidationResult.VALID;

        } catch (DateTimeParseException e) {
            logger.warn("Date of birth validation failed: parsing error", e);
            return ValidationResult.INVALID_FORMAT;
        }
    }

    /**
     * Validates if a date is in a valid range between two dates.
     * Provides range validation for business rule enforcement.
     * 
     * @param dateValue The date to validate
     * @param startDate The start date of the valid range (inclusive)
     * @param endDate The end date of the valid range (inclusive)
     * @return ValidationResult indicating range validity
     */
    public static ValidationResult isValidDateRange(String dateValue, String startDate, String endDate) {
        logger.debug("Validating date range: {} between {} and {}", dateValue, startDate, endDate);

        // Validate all three dates first
        ValidationResult dateResult = validateDate(dateValue);
        if (!dateResult.isValid()) {
            return dateResult;
        }

        ValidationResult startResult = validateDate(startDate);
        if (!startResult.isValid()) {
            return startResult;
        }

        ValidationResult endResult = validateDate(endDate);
        if (!endResult.isValid()) {
            return endResult;
        }

        try {
            LocalDate date = LocalDate.parse(dateValue.trim(), COBOL_DATE_FORMATTER);
            LocalDate start = LocalDate.parse(startDate.trim(), COBOL_DATE_FORMATTER);
            LocalDate end = LocalDate.parse(endDate.trim(), COBOL_DATE_FORMATTER);

            // Check if date is within range (inclusive)
            if (date.isBefore(start) || date.isAfter(end)) {
                logger.warn("Date range validation failed: {} not in range {} to {}", dateValue, startDate, endDate);
                return ValidationResult.INVALID_RANGE;
            }

            logger.debug("Date range validation successful: {} in range {} to {}", dateValue, startDate, endDate);
            return ValidationResult.VALID;

        } catch (DateTimeParseException e) {
            logger.warn("Date range validation failed: parsing error", e);
            return ValidationResult.INVALID_FORMAT;
        }
    }

    /**
     * Validates if a date is not in the future (for business rules).
     * Useful for validating effective dates, transaction dates, etc.
     * 
     * @param dateValue The date to validate in CCYYMMDD format
     * @return ValidationResult indicating future date validity
     */
    public static ValidationResult validateFutureDate(String dateValue) {
        logger.debug("Validating future date restriction: {}", dateValue);

        ValidationResult basicValidation = validateDate(dateValue);
        if (!basicValidation.isValid()) {
            return basicValidation;
        }

        try {
            LocalDate date = LocalDate.parse(dateValue.trim(), COBOL_DATE_FORMATTER);
            LocalDate currentDate = LocalDate.now();

            if (date.isAfter(currentDate)) {
                logger.warn("Future date validation failed: {} is in the future", dateValue);
                return ValidationResult.INVALID_DATE;
            }

            logger.debug("Future date validation successful: {}", dateValue);
            return ValidationResult.VALID;

        } catch (DateTimeParseException e) {
            logger.warn("Future date validation failed: parsing error", e);
            return ValidationResult.INVALID_FORMAT;
        }
    }

    // =======================================================================
    // LEAP YEAR CALCULATION (from CSUTLDPY.cpy lines 245-271)
    // =======================================================================

    /**
     * Determines if a year is a leap year using exact COBOL leap year logic.
     * Implements the leap year calculation from CSUTLDPY.cpy with identical arithmetic.
     * 
     * <p>COBOL Logic (lines 245-271 in CSUTLDPY.cpy):</p>
     * <pre>
     * IF WS-EDIT-DATE-YY-N = 0
     *    MOVE 400 TO WS-DIV-BY
     * ELSE
     *    MOVE 4 TO WS-DIV-BY
     * END-IF
     * DIVIDE WS-EDIT-DATE-CCYY-N BY WS-DIV-BY GIVING WS-DIVIDEND REMAINDER WS-REMAINDER
     * IF WS-REMAINDER = ZEROES
     *    CONTINUE (leap year)
     * ELSE
     *    SET INPUT-ERROR TO TRUE (not a leap year)
     * </pre>
     * 
     * @param year The year to test for leap year (4-digit format)
     * @return true if the year is a leap year, false otherwise
     */
    public static boolean isLeapYear(int year) {
        logger.debug("Checking leap year: {}", year);

        // COBOL equivalent: IF WS-EDIT-DATE-YY-N = 0 (century year check)
        int divBy;
        if (year % 100 == 0) {
            divBy = 400; // Century years must be divisible by 400
        } else {
            divBy = 4;   // Non-century years must be divisible by 4
        }

        // COBOL equivalent: DIVIDE year BY divBy REMAINDER remainder
        boolean isLeap = (year % divBy == 0);
        
        logger.debug("Leap year check result for {}: {}", year, isLeap);
        return isLeap;
    }

    // =======================================================================
    // DATE FORMATTING METHODS (from CSDAT01Y.cpy structures)
    // =======================================================================

    /**
     * Formats a LocalDate to COBOL date format (YYYYMMDD).
     * Equivalent to COBOL date formatting for display and storage.
     * 
     * @param date The LocalDate to format
     * @return Formatted date string in YYYYMMDD format
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            logger.warn("Format date called with null date");
            return "";
        }

        String formatted = date.format(COBOL_DATE_FORMATTER);
        logger.debug("Formatted date {} to COBOL format: {}", date, formatted);
        return formatted;
    }

    /**
     * Formats a LocalDate to user-friendly display format (MM/dd/yyyy).
     * Based on WS-CURDATE-MM-DD-YY structure from CSDAT01Y.cpy.
     * 
     * @param date The LocalDate to format for display
     * @return Formatted date string in MM/dd/yyyy format
     */
    public static String formatDateForDisplay(LocalDate date) {
        if (date == null) {
            logger.warn("Format date for display called with null date");
            return "";
        }

        String formatted = date.format(SLASH_DATE_FORMATTER);
        logger.debug("Formatted date {} for display: {}", date, formatted);
        return formatted;
    }

    /**
     * Formats a LocalDate to slash-separated format (MM/dd/yyyy).
     * Provides explicit slash formatting for UI components.
     * 
     * @param date The LocalDate to format
     * @return Formatted date string in MM/dd/yyyy format
     */
    public static String formatDateSlash(LocalDate date) {
        return formatDateForDisplay(date); // Same implementation
    }

    /**
     * Formats a LocalDate to COBOL date format string.
     * Provides direct COBOL format conversion for data exchange.
     * 
     * @param date The LocalDate to format
     * @return Formatted date string in YYYYMMDD format
     */
    public static String formatCobolDate(LocalDate date) {
        return formatDate(date); // Same implementation
    }

    /**
     * Formats a LocalDateTime to COBOL timestamp format.
     * Based on WS-TIMESTAMP structure from CSDAT01Y.cpy.
     * 
     * @param dateTime The LocalDateTime to format
     * @return Formatted timestamp string in yyyy-MM-dd HH:mm:ss.SSSSSS format
     */
    public static String formatTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            logger.warn("Format timestamp called with null dateTime");
            return "";
        }

        String formatted = dateTime.format(COBOL_TIMESTAMP_FORMATTER);
        logger.debug("Formatted timestamp {} to COBOL format: {}", dateTime, formatted);
        return formatted;
    }

    /**
     * Formats a LocalDateTime to COBOL timestamp format.
     * Provides explicit COBOL timestamp formatting for data exchange.
     * 
     * @param dateTime The LocalDateTime to format
     * @return Formatted timestamp string in yyyy-MM-dd HH:mm:ss.SSSSSS format
     */
    public static String formatCobolTimestamp(LocalDateTime dateTime) {
        return formatTimestamp(dateTime); // Same implementation
    }

    // =======================================================================
    // DATE PARSING METHODS
    // =======================================================================

    /**
     * Parses a date string in COBOL format (YYYYMMDD) to LocalDate.
     * Provides safe parsing with validation for COBOL date strings.
     * 
     * @param dateString The date string in YYYYMMDD format
     * @return Optional<LocalDate> containing parsed date, or empty if invalid
     */
    public static Optional<LocalDate> parseDate(String dateString) {
        logger.debug("Parsing date: {}", dateString);

        if (StringUtils.isBlank(dateString)) {
            logger.warn("Parse date called with blank string");
            return Optional.empty();
        }

        try {
            ValidationResult validation = validateDate(dateString.trim());
            if (!validation.isValid()) {
                logger.warn("Parse date failed validation: {}", validation.getErrorMessage());
                return Optional.empty();
            }

            LocalDate parsed = LocalDate.parse(dateString.trim(), COBOL_DATE_FORMATTER);
            logger.debug("Successfully parsed date: {} -> {}", dateString, parsed);
            return Optional.of(parsed);

        } catch (DateTimeParseException e) {
            logger.warn("Parse date failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Parses a COBOL date string (YYYYMMDD) to LocalDate.
     * Provides explicit COBOL date parsing for data import operations.
     * 
     * @param cobolDateString The COBOL date string in YYYYMMDD format
     * @return Optional<LocalDate> containing parsed date, or empty if invalid
     */
    public static Optional<LocalDate> parseCobolDate(String cobolDateString) {
        return parseDate(cobolDateString); // Same implementation
    }

    /**
     * Parses a timestamp string in COBOL format to LocalDateTime.
     * Based on WS-TIMESTAMP structure parsing from CSDAT01Y.cpy.
     * 
     * @param timestampString The timestamp string in yyyy-MM-dd HH:mm:ss.SSSSSS format
     * @return Optional<LocalDateTime> containing parsed timestamp, or empty if invalid
     */
    public static Optional<LocalDateTime> parseTimestamp(String timestampString) {
        logger.debug("Parsing timestamp: {}", timestampString);

        if (StringUtils.isBlank(timestampString)) {
            logger.warn("Parse timestamp called with blank string");
            return Optional.empty();
        }

        try {
            String trimmed = timestampString.trim();
            
            // Handle various timestamp formats
            LocalDateTime parsed;
            if (trimmed.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{6}$")) {
                // Full microsecond format
                parsed = LocalDateTime.parse(trimmed, COBOL_TIMESTAMP_FORMATTER);
            } else if (trimmed.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$")) {
                // Standard format without microseconds
                parsed = LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } else {
                logger.warn("Parse timestamp failed: unsupported format {}", timestampString);
                return Optional.empty();
            }

            logger.debug("Successfully parsed timestamp: {} -> {}", timestampString, parsed);
            return Optional.of(parsed);

        } catch (DateTimeParseException e) {
            logger.warn("Parse timestamp failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Parses a COBOL timestamp string to LocalDateTime.
     * Provides explicit COBOL timestamp parsing for data import operations.
     * 
     * @param cobolTimestampString The COBOL timestamp string
     * @return Optional<LocalDateTime> containing parsed timestamp, or empty if invalid
     */
    public static Optional<LocalDateTime> parseCobolTimestamp(String cobolTimestampString) {
        return parseTimestamp(cobolTimestampString); // Same implementation
    }

    // =======================================================================
    // CURRENT DATE/TIME METHODS (from CSDAT01Y.cpy)
    // =======================================================================

    /**
     * Gets the current date in LocalDate format.
     * Equivalent to COBOL FUNCTION CURRENT-DATE date portion.
     * 
     * @return Current date as LocalDate
     */
    public static LocalDate getCurrentDate() {
        LocalDate current = LocalDate.now();
        logger.debug("Current date: {}", current);
        return current;
    }

    /**
     * Gets the current timestamp in LocalDateTime format.
     * Equivalent to COBOL FUNCTION CURRENT-DATE with time information.
     * 
     * @return Current timestamp as LocalDateTime
     */
    public static LocalDateTime getCurrentTimestamp() {
        LocalDateTime current = LocalDateTime.now();
        logger.debug("Current timestamp: {}", current);
        return current;
    }

    // =======================================================================
    // FORMAT VALIDATION METHODS
    // =======================================================================

    /**
     * Validates if a date string matches the COBOL date format.
     * Checks format without full date validation for performance.
     * 
     * @param dateString The date string to validate
     * @return ValidationResult indicating format validity
     */
    public static ValidationResult validateCobolDateFormat(String dateString) {
        logger.debug("Validating COBOL date format: {}", dateString);

        ValidationResult requiredCheck = ValidationUtils.validateRequiredField(dateString, "date");
        if (!requiredCheck.isValid()) {
            return requiredCheck;
        }

        String trimmed = dateString.trim();

        // Check exact 8-digit format
        if (!COBOL_DATE_PATTERN.matcher(trimmed).matches()) {
            logger.warn("COBOL date format validation failed: {}", dateString);
            return ValidationResult.INVALID_FORMAT;
        }

        logger.debug("COBOL date format validation successful: {}", dateString);
        return ValidationResult.VALID;
    }

    /**
     * Checks if a date string represents a valid date without throwing exceptions.
     * Provides boolean validation for conditional logic.
     * 
     * @param dateString The date string to validate
     * @return true if the date is valid, false otherwise
     */
    public static boolean isValidDate(String dateString) {
        return validateDate(dateString).isValid();
    }

    // =======================================================================
    // UTILITY METHODS
    // =======================================================================

    /**
     * Gets the number of days in a specific month and year.
     * Handles leap year calculations for February.
     * 
     * @param month The month (1-12)
     * @param year The year (4-digit format)
     * @return Number of days in the month
     */
    public static int getDaysInMonth(int month, int year) {
        logger.debug("Getting days in month {} year {}", month, year);

        ValidationResult monthResult = validateMonth(month);
        if (!monthResult.isValid()) {
            logger.warn("Invalid month for getDaysInMonth: {}", month);
            return 0;
        }

        ValidationResult yearResult = validateYear(year);
        if (!yearResult.isValid()) {
            logger.warn("Invalid year for getDaysInMonth: {}", year);
            return 0;
        }

        int days = ValidationConstants.getMaxDayForMonth(month, year);
        logger.debug("Days in month {} year {}: {}", month, year, days);
        return days;
    }

    /**
     * Converts a date string from one format to another with validation.
     * Provides flexible date format conversion for various UI needs.
     * 
     * @param dateString The source date string
     * @param sourceFormat The source format pattern
     * @param targetFormat The target format pattern
     * @return Optional<String> containing converted date, or empty if conversion fails
     */
    public static Optional<String> convertDateFormat(String dateString, String sourceFormat, String targetFormat) {
        logger.debug("Converting date format: {} from {} to {}", dateString, sourceFormat, targetFormat);

        if (StringUtils.isBlank(dateString) || StringUtils.isBlank(sourceFormat) || StringUtils.isBlank(targetFormat)) {
            logger.warn("Convert date format called with blank parameters");
            return Optional.empty();
        }

        try {
            DateTimeFormatter sourceFormatter = DateTimeFormatter.ofPattern(sourceFormat);
            DateTimeFormatter targetFormatter = DateTimeFormatter.ofPattern(targetFormat);
            
            LocalDate date = LocalDate.parse(dateString.trim(), sourceFormatter);
            String converted = date.format(targetFormatter);
            
            logger.debug("Successfully converted date: {} -> {}", dateString, converted);
            return Optional.of(converted);

        } catch (DateTimeParseException | IllegalArgumentException e) {
            logger.warn("Date format conversion failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Calculates the difference in days between two dates.
     * Provides date arithmetic for business calculations.
     * 
     * @param startDate The start date in YYYYMMDD format
     * @param endDate The end date in YYYYMMDD format
     * @return Optional<Long> containing day difference, or empty if dates are invalid
     */
    public static Optional<Long> daysBetween(String startDate, String endDate) {
        logger.debug("Calculating days between {} and {}", startDate, endDate);

        Optional<LocalDate> start = parseDate(startDate);
        Optional<LocalDate> end = parseDate(endDate);

        if (start.isEmpty() || end.isEmpty()) {
            logger.warn("Days between calculation failed: invalid dates");
            return Optional.empty();
        }

        try {
            long days = java.time.temporal.ChronoUnit.DAYS.between(start.get(), end.get());
            logger.debug("Days between {} and {}: {}", startDate, endDate, days);
            return Optional.of(days);

        } catch (Exception e) {
            logger.warn("Days between calculation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Creates a date validation summary for debugging and logging.
     * Provides comprehensive validation information for troubleshooting.
     * 
     * @param dateString The date string to analyze
     * @return String containing detailed validation information
     */
    public static String getDateValidationSummary(String dateString) {
        if (StringUtils.isBlank(dateString)) {
            return "Date validation summary: [BLANK] - Date string is null or empty";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Date validation summary for '").append(dateString).append("':\n");

        String trimmed = dateString.trim();
        
        // Format check
        boolean formatValid = COBOL_DATE_PATTERN.matcher(trimmed).matches();
        summary.append("  Format (YYYYMMDD): ").append(formatValid ? "VALID" : "INVALID").append("\n");

        if (formatValid && trimmed.length() == 8) {
            try {
                // Component extraction
                int century = Integer.parseInt(trimmed.substring(0, 2));
                int year = Integer.parseInt(trimmed.substring(2, 4));
                int month = Integer.parseInt(trimmed.substring(4, 6));
                int day = Integer.parseInt(trimmed.substring(6, 8));
                int fullYear = century * 100 + year;

                // Individual component validation
                summary.append("  Century (").append(century).append("): ")
                       .append(ValidationConstants.VALID_CENTURIES.contains(century) ? "VALID" : "INVALID").append("\n");
                summary.append("  Month (").append(month).append("): ")
                       .append(ValidationConstants.VALID_MONTHS.contains(month) ? "VALID" : "INVALID").append("\n");
                summary.append("  Day (").append(day).append("): ")
                       .append(ValidationConstants.VALID_DAYS.contains(day) ? "VALID" : "INVALID").append("\n");
                
                // Leap year info
                summary.append("  Leap year (").append(fullYear).append("): ").append(isLeapYear(fullYear)).append("\n");
                
                // Month-specific day validation
                if (ValidationConstants.VALID_MONTHS.contains(month)) {
                    int maxDays = ValidationConstants.getMaxDayForMonth(month, fullYear);
                    summary.append("  Max days for month: ").append(maxDays).append("\n");
                    summary.append("  Day valid for month: ").append(day <= maxDays ? "VALID" : "INVALID").append("\n");
                }

                // Overall validation
                ValidationResult overall = validateDate(dateString);
                summary.append("  Overall validation: ").append(overall.isValid() ? "VALID" : "INVALID");
                if (!overall.isValid()) {
                    summary.append(" (").append(overall.getErrorMessage()).append(")");
                }

            } catch (Exception e) {
                summary.append("  Error during analysis: ").append(e.getMessage());
            }
        }

        return summary.toString();
    }
}
