/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Date conversion and validation utility class that translates the CSUTLDTC COBOL subprogram functionality.
 * Provides methods for validating dates in CCYYMMDD format, converting between date formats, 
 * and performing date arithmetic operations. Replaces CEEDAYS API calls with Java 8 time API 
 * equivalents while maintaining identical validation logic.
 * 
 * This utility maintains the exact validation rules from the original COBOL implementation:
 * - Year validation for centuries 19 and 20 only (1900-2099)
 * - Month validation (1-12)
 * - Day validation considering leap years and month lengths
 * - Date format validation in CCYYMMDD format
 * 
 * The class preserves all business logic from CSUTLDTC.cbl, CSUTLDPY.cpy, and related copybooks
 * while leveraging modern Java date/time APIs for enhanced precision and maintainability.
 */
public final class DateConversionUtil {



    /**
     * Date format pattern for CCYYMMDD format.
     */
    private static final DateTimeFormatter CCYYMMDD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Pattern for validating numeric-only strings.
     */
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^\\d+$");

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private DateConversionUtil() {
        throw new IllegalStateException("Utility class - cannot be instantiated");
    }

    /**
     * Validates a year value ensuring it falls within acceptable centuries (19 or 20).
     * Replicates the EDIT-YEAR-CCYY paragraph logic from CSUTLDPY.cpy.
     * 
     * @param year the year to validate (CCYY format, e.g., 1995, 2023)
     * @return true if year is valid (between 1900-2099), false otherwise
     */
    public static boolean validateYear(int year) {
        // Century validation: only 19 and 20 are valid (1900-2099)
        int century = year / 100;
        return century == 19 || century == 20;
    }

    /**
     * Validates a month value ensuring it's between 1 and 12.
     * Replicates the EDIT-MONTH paragraph logic from CSUTLDPY.cpy.
     * 
     * @param month the month to validate (1-12)
     * @return true if month is valid (1-12), false otherwise
     */
    public static boolean validateMonth(int month) {
        return month >= 1 && month <= 12;
    }

    /**
     * Validates a day value considering the specific month and year (for leap year handling).
     * Replicates the EDIT-DAY and EDIT-DAY-MONTH-YEAR paragraph logic from CSUTLDPY.cpy.
     * 
     * @param year the year (for leap year calculation)
     * @param month the month (for determining days in month)
     * @param day the day to validate
     * @return true if day is valid for the given month and year, false otherwise
     */
    public static boolean validateDay(int year, int month, int day) {
        if (day < 1 || day > 31) {
            return false;
        }

        // Check basic day range for the month
        LocalDate tempDate;
        try {
            tempDate = LocalDate.of(year, month, 1);
        } catch (Exception e) {
            return false;
        }

        int daysInMonth = tempDate.lengthOfMonth();
        return day <= daysInMonth;
    }

    /**
     * Checks if the given date is not in the future compared to the current date.
     * Replicates the EDIT-DATE-OF-BIRTH paragraph logic from CSUTLDPY.cpy.
     * 
     * @param date the date to check
     * @return true if the date is not in the future, false otherwise
     */
    public static boolean isNotFutureDate(LocalDate date) {
        return !date.isAfter(LocalDate.now());
    }

    /**
     * Calculates a retention date by adding a specified number of years to the given date.
     * Used for determining data archival dates and retention periods.
     * 
     * @param baseDate the base date to calculate from
     * @param years the number of years to add
     * @return the calculated retention date
     */
    public static LocalDate calculateRetentionDate(LocalDate baseDate, int years) {
        return baseDate.plusYears(years);
    }

    /**
     * Determines if a record is eligible for archival based on the retention date.
     * 
     * @param createdDate the date the record was created
     * @param retentionYears the number of years to retain the record
     * @return true if the record is eligible for archival, false otherwise
     */
    public static boolean isEligibleForArchival(LocalDate createdDate, int retentionYears) {
        LocalDate retentionDate = calculateRetentionDate(createdDate, retentionYears);
        return LocalDate.now().isAfter(retentionDate);
    }

    /**
     * Adds a specified number of years to the given date.
     * 
     * @param date the base date
     * @param years the number of years to add
     * @return the date with added years
     */
    public static LocalDate addYears(LocalDate date, int years) {
        return date.plusYears(years);
    }

    /**
     * Parses a date string in CCYYMMDD format into a LocalDate object.
     * Replicates COBOL date parsing logic with comprehensive validation.
     * 
     * @param dateString the date string in CCYYMMDD format
     * @return the parsed LocalDate object
     * @throws IllegalArgumentException if the date string is invalid
     */
    public static LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            throw new IllegalArgumentException("Date string cannot be null or empty");
        }

        String trimmedDate = dateString.trim();
        
        if (trimmedDate.length() != Constants.DATE_FORMAT_LENGTH) {
            throw new IllegalArgumentException("Date string must be exactly " + Constants.DATE_FORMAT_LENGTH + " characters long (CCYYMMDD format)");
        }

        if (!NUMERIC_PATTERN.matcher(trimmedDate).matches()) {
            throw new IllegalArgumentException("Date string must contain only numeric characters");
        }

        try {
            return LocalDate.parse(trimmedDate, CCYYMMDD_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + dateString, e);
        }
    }

    /**
     * Validates a date string in CCYYMMDD format using the comprehensive validation logic
     * from the original COBOL implementation. Replicates the EDIT-DATE-CCYYMMDD paragraph
     * logic from CSUTLDPY.cpy and the CEEDAYS API validation from CSUTLDTC.cbl.
     * 
     * @param dateString the date string to validate in CCYYMMDD format
     * @return true if the date is valid, false otherwise
     */
    public static boolean validateDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return false;
        }

        String trimmedDate = dateString.trim();
        
        // Check length
        if (trimmedDate.length() != Constants.DATE_FORMAT_LENGTH) {
            return false;
        }

        // Check if all characters are numeric
        if (!NUMERIC_PATTERN.matcher(trimmedDate).matches()) {
            return false;
        }

        try {
            // Extract year, month, day components
            int year = Integer.parseInt(trimmedDate.substring(0, 4));
            int month = Integer.parseInt(trimmedDate.substring(4, 6));
            int day = Integer.parseInt(trimmedDate.substring(6, 8));

            // Validate year (century check)
            if (!validateYear(year)) {
                return false;
            }

            // Validate month
            if (!validateMonth(month)) {
                return false;
            }

            // Validate day
            if (!validateDay(year, month, day)) {
                return false;
            }

            // Final validation using LocalDate parsing
            LocalDate.of(year, month, day);
            return true;

        } catch (NumberFormatException | java.time.DateTimeException e) {
            return false;
        }
    }

    /**
     * Converts a date string from one format to another.
     * Currently supports conversion to and from CCYYMMDD format.
     * 
     * @param dateString the input date string
     * @param fromFormat the source format pattern
     * @param toFormat the target format pattern
     * @return the converted date string
     * @throws IllegalArgumentException if conversion fails
     */
    public static String convertDateFormat(String dateString, String fromFormat, String toFormat) {
        if (dateString == null || dateString.trim().isEmpty()) {
            throw new IllegalArgumentException("Date string cannot be null or empty");
        }

        try {
            DateTimeFormatter fromFormatter = DateTimeFormatter.ofPattern(fromFormat);
            DateTimeFormatter toFormatter = DateTimeFormatter.ofPattern(toFormat);
            
            LocalDate date = LocalDate.parse(dateString.trim(), fromFormatter);
            return date.format(toFormatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Unable to convert date from format '" + fromFormat + "' to '" + toFormat + "': " + dateString, e);
        }
    }

    /**
     * Converts a COBOL date string (CCYYMMDD) to a LocalDate object.
     * Replicates the functionality of the CSUTLDTC COBOL subprogram.
     * 
     * @param cobolDate the COBOL date string in CCYYMMDD format
     * @return the corresponding LocalDate object
     * @throws IllegalArgumentException if the COBOL date is invalid
     */
    public static LocalDate convertCobolDate(String cobolDate) {
        return parseDate(cobolDate);
    }

    /**
     * Formats a LocalDate object to COBOL date string format (CCYYMMDD).
     * 
     * @param date the LocalDate to format
     * @return the formatted date string in CCYYMMDD format
     */
    public static String formatToCobol(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        return date.format(CCYYMMDD_FORMATTER);
    }

    /**
     * Gets the current date in CCYYMMDD format.
     * Replicates COBOL FUNCTION CURRENT-DATE behavior for date portion.
     * 
     * @return the current date as a string in CCYYMMDD format
     */
    public static String getCurrentDate() {
        return LocalDate.now().format(CCYYMMDD_FORMATTER);
    }

    /**
     * Adds a specified number of days to the given date.
     * Provides functionality equivalent to COBOL date arithmetic operations.
     * 
     * @param date the base date
     * @param days the number of days to add (can be negative to subtract)
     * @return the date with added days
     */
    public static LocalDate addDays(LocalDate date, long days) {
        return date.plusDays(days);
    }

    /**
     * Subtracts a specified number of days from the given date.
     * Provides functionality equivalent to COBOL date arithmetic operations.
     * 
     * @param date the base date
     * @param days the number of days to subtract
     * @return the date with subtracted days
     */
    public static LocalDate subtractDays(LocalDate date, long days) {
        return date.minusDays(days);
    }

    /**
     * Checks if the given date is before another date.
     * Used for date comparison operations similar to COBOL date comparisons.
     * 
     * @param date1 the first date
     * @param date2 the second date to compare against
     * @return true if date1 is before date2, false otherwise
     */
    public static boolean isBefore(LocalDate date1, LocalDate date2) {
        return date1.isBefore(date2);
    }

    /**
     * Extracts the year component from a date.
     * Replicates COBOL date component extraction functionality.
     * 
     * @param date the date to extract year from
     * @return the year as an integer
     */
    public static int getYear(LocalDate date) {
        return date.getYear();
    }

    /**
     * Extracts the month component from a date.
     * Replicates COBOL date component extraction functionality.
     * 
     * @param date the date to extract month from
     * @return the month as an integer (1-12)
     */
    public static int getMonth(LocalDate date) {
        return date.getMonthValue();
    }

    /**
     * Extracts the day component from a date.
     * Replicates COBOL date component extraction functionality.
     * 
     * @param date the date to extract day from
     * @return the day as an integer (1-31)
     */
    public static int getDay(LocalDate date) {
        return date.getDayOfMonth();
    }

    /**
     * Checks if a given year is a leap year.
     * Replicates COBOL leap year calculation logic used in CSUTLDPY.cpy.
     * 
     * @param year the year to check
     * @return true if the year is a leap year, false otherwise
     */
    public static boolean isLeapYear(int year) {
        return LocalDate.of(year, 1, 1).isLeapYear();
    }

    /**
     * Validates that a date string matches the expected format.
     * Provides validation for COBOL date format compatibility in batch processing.
     *
     * @param dateString the date string to validate
     * @param expectedFormat the expected date format pattern
     * @return true if the date string matches the expected format
     */
    public static boolean validateDateFormat(String dateString, String expectedFormat) {
        if (dateString == null || expectedFormat == null) {
            return false;
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(expectedFormat);
            LocalDate.parse(dateString, formatter);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Formats a date for use in batch report headers.
     * Provides header date formatting matching COBOL batch report layouts.
     *
     * @param date the date to format
     * @return formatted date string suitable for report headers
     */
    public static String formatHeaderDate(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        return formatter.format(date);
    }

    /**
     * Formats a date for use in batch reports with COBOL-compatible format.
     * Provides report date formatting matching COBOL date display patterns.
     *
     * @param date the date to format
     * @return formatted date string for batch reports
     */
    public static String formatReportDate(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return formatter.format(date);
    }

    /**
     * Formats a LocalDateTime as a timestamp string for batch processing.
     * Provides timestamp formatting for batch report generation and logging.
     *
     * @param dateTime the LocalDateTime to format
     * @return formatted timestamp string
     */
    public static String formatTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            dateTime = LocalDateTime.now();
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return formatter.format(dateTime);
    }

    /**
     * Formats a LocalDateTime as a timestamp string with custom pattern.
     * Provides flexible timestamp formatting for various batch processing needs.
     *
     * @param dateTime the LocalDateTime to format
     * @param pattern the custom format pattern
     * @return formatted timestamp string using the specified pattern
     */
    public static String formatTimestamp(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            dateTime = LocalDateTime.now();
        }
        
        if (pattern == null || pattern.trim().isEmpty()) {
            pattern = "yyyy-MM-dd HH:mm:ss";
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return formatter.format(dateTime);
    }
}