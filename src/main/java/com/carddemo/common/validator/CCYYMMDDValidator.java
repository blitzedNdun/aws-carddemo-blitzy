package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Implementation class for CCYYMMDD date format validation.
 * <p>
 * Provides comprehensive date validation logic including century restrictions, 
 * leap year calculations, and month/day range validation that exactly replicates 
 * COBOL date processing behavior from CSUTLDPY.cpy and CSUTLDTC.cbl.
 * </p>
 * 
 * <h3>COBOL Equivalency Mapping:</h3>
 * <ul>
 *   <li><strong>EDIT-YEAR-CCYY:</strong> {@link #validateCentury(String)} and {@link #validateYear(String)}</li>
 *   <li><strong>EDIT-MONTH:</strong> {@link #validateMonth(String)}</li>
 *   <li><strong>EDIT-DAY:</strong> {@link #validateDay(String)}</li>
 *   <li><strong>EDIT-DAY-MONTH-YEAR:</strong> {@link #validateMonthDayCombination(int, int, int)}</li>
 *   <li><strong>EDIT-DATE-LE:</strong> {@link #validateDateWithLocalDate(int, int, int)}</li>
 * </ul>
 * 
 * <h3>Validation Sequence:</h3>
 * <ol>
 *   <li>Format validation - must be exactly 8 digits</li>
 *   <li>Century validation - only 19xx and 20xx allowed</li>
 *   <li>Year validation - must be numeric</li>
 *   <li>Month validation - must be 1-12</li>
 *   <li>Day validation - must be 1-31</li>
 *   <li>Month/day combination validation - day limits per month</li>
 *   <li>Leap year validation - February 29th only in leap years</li>
 *   <li>Final date validation - using Java LocalDate</li>
 * </ol>
 * 
 * <h3>Century Validation Logic:</h3>
 * <p>
 * Following COBOL comment: "Not having learnt our lesson from history and Y2K
 * And being unable to imagine COBOL in the 2100s
 * We code only 19 and 20 as valid century values"
 * </p>
 * 
 * <h3>Leap Year Calculation:</h3>
 * <p>
 * Implements COBOL leap year logic from CSUTLDPY.cpy:
 * - If year ends in 00, divide by 400
 * - Otherwise, divide by 4
 * - No remainder means leap year
 * </p>
 * 
 * @see ValidCCYYMMDD
 * @see ValidationConstants
 * @author Blitzy Platform
 * @version 1.0
 * @since 2024-01-01
 */
public class CCYYMMDDValidator implements ConstraintValidator<ValidCCYYMMDD, String> {
    
    /**
     * Pattern for CCYYMMDD format validation - exactly 8 digits
     * Equivalent to COBOL numeric validation in CSUTLDPY.cpy
     */
    private static final Pattern CCYYMMDD_PATTERN = Pattern.compile("^\\d{8}$");
    
    /**
     * Set of months with 31 days
     * Source: COBOL 88-level WS-31-DAY-MONTH VALUES 1, 3, 5, 7, 8, 10, 12
     */
    private static final int[] MONTHS_WITH_31_DAYS = {1, 3, 5, 7, 8, 10, 12};
    
    /**
     * February month constant
     * Source: COBOL 88-level WS-FEBRUARY VALUE 2
     */
    private static final int FEBRUARY = 2;
    
    /**
     * Valid century values from COBOL
     * Source: COBOL 88-level THIS-CENTURY VALUE 20, LAST-CENTURY VALUE 19
     */
    private static final int THIS_CENTURY = 20;
    private static final int LAST_CENTURY = 19;
    
    /**
     * Configuration from annotation
     */
    private ValidCCYYMMDD annotation;
    
    /**
     * Initializes the validator with annotation configuration.
     * 
     * @param constraintAnnotation the annotation instance containing configuration
     */
    @Override
    public void initialize(ValidCCYYMMDD constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }
    
    /**
     * Validates a date string in CCYYMMDD format.
     * <p>
     * Implements the complete validation sequence from CSUTLDPY.cpy:
     * 1. Format and null/blank validation
     * 2. Century validation (19xx or 20xx only)
     * 3. Year, month, day component validation
     * 4. Month/day combination validation
     * 5. Leap year validation for February 29th
     * 6. Final date validation using LocalDate
     * </p>
     * 
     * @param value the date string to validate
     * @param context the validation context for error reporting
     * @return true if the date is valid, false otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Handle null and blank values based on allowBlank flag
        if (value == null || value.trim().isEmpty()) {
            if (annotation.allowBlank()) {
                return true;
            } else {
                buildConstraintViolation(context, annotation.formatMessage(), 
                    "Date must be supplied");
                return false;
            }
        }
        
        // Disable default constraint violation to provide custom messages
        context.disableDefaultConstraintViolation();
        
        // Format validation - must be exactly 8 digits
        if (!validateFormat(value, context)) {
            return false;
        }
        
        // Extract date components (equivalent to COBOL REDEFINES)
        int century = Integer.parseInt(value.substring(0, 2));  // CC
        int year = Integer.parseInt(value.substring(2, 4));     // YY  
        int month = Integer.parseInt(value.substring(4, 6));    // MM
        int day = Integer.parseInt(value.substring(6, 8));      // DD
        int fullYear = century * 100 + year;                    // CCYY
        
        // Century validation (equivalent to EDIT-YEAR-CCYY)
        if (annotation.strictCentury() && !validateCentury(century, context)) {
            return false;
        }
        
        // Year validation (numeric validation already done in format check)
        if (!validateYear(fullYear, context)) {
            return false;
        }
        
        // Month validation (equivalent to EDIT-MONTH)
        if (!validateMonth(month, context)) {
            return false;
        }
        
        // Day validation (equivalent to EDIT-DAY)
        if (!validateDay(day, context)) {
            return false;
        }
        
        // Month/day combination validation (equivalent to EDIT-DAY-MONTH-YEAR)
        if (annotation.checkMonthDay() && !validateMonthDayCombination(month, day, fullYear, context)) {
            return false;
        }
        
        // Final date validation using LocalDate (equivalent to EDIT-DATE-LE)
        if (!validateDateWithLocalDate(fullYear, month, day, context)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates the format of the input string.
     * <p>
     * Equivalent to COBOL format validation in CSUTLDPY.cpy where
     * date components must be numeric and properly formatted.
     * </p>
     * 
     * @param value the input string
     * @param context validation context
     * @return true if format is valid
     */
    private boolean validateFormat(String value, ConstraintValidatorContext context) {
        if (!CCYYMMDD_PATTERN.matcher(value).matches()) {
            buildConstraintViolation(context, annotation.formatMessage(),
                "Date must be in CCYYMMDD format (8 digits)");
            return false;
        }
        
        // Additional check for numeric pattern using ValidationConstants
        if (!ValidationConstants.NUMERIC_PATTERN.matcher(value).matches()) {
            buildConstraintViolation(context, annotation.formatMessage(),
                "Date must contain only numeric digits");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates the century component.
     * <p>
     * Implements COBOL logic from CSUTLDPY.cpy:
     * "Century not reasonable - We code only 19 and 20 as valid century values"
     * Checks against THIS-CENTURY (20) and LAST-CENTURY (19) conditions.
     * </p>
     * 
     * @param century the century value (19 or 20)
     * @param context validation context
     * @return true if century is valid
     */
    private boolean validateCentury(int century, ConstraintValidatorContext context) {
        if (century != THIS_CENTURY && century != LAST_CENTURY) {
            buildConstraintViolation(context, annotation.centuryMessage(),
                "Century is not valid (only 19xx and 20xx are supported)");
            return false;
        }
        return true;
    }
    
    /**
     * Validates the year component.
     * <p>
     * Implements basic year validation equivalent to COBOL year checks.
     * Year must be reasonable (not zero in most business contexts).
     * </p>
     * 
     * @param year the full year value
     * @param context validation context
     * @return true if year is valid
     */
    private boolean validateYear(int year, ConstraintValidatorContext context) {
        // Year validation is primarily handled by century validation
        // Additional business rules could be added here if needed
        return true;
    }
    
    /**
     * Validates the month component.
     * <p>
     * Implements COBOL logic from CSUTLDPY.cpy EDIT-MONTH:
     * Checks against WS-VALID-MONTH condition (VALUES 1 THROUGH 12).
     * </p>
     * 
     * @param month the month value (1-12)
     * @param context validation context
     * @return true if month is valid
     */
    private boolean validateMonth(int month, ConstraintValidatorContext context) {
        if (month < 1 || month > 12) {
            buildConstraintViolation(context, annotation.monthMessage(),
                "Month must be a number between 1 and 12");
            return false;
        }
        return true;
    }
    
    /**
     * Validates the day component.
     * <p>
     * Implements COBOL logic from CSUTLDPY.cpy EDIT-DAY:
     * Checks against WS-VALID-DAY condition (VALUES 1 THROUGH 31).
     * </p>
     * 
     * @param day the day value (1-31)
     * @param context validation context
     * @return true if day is valid
     */
    private boolean validateDay(int day, ConstraintValidatorContext context) {
        if (day < 1 || day > 31) {
            buildConstraintViolation(context, annotation.dayMessage(),
                "Day must be a number between 1 and 31");
            return false;
        }
        return true;
    }
    
    /**
     * Validates month/day combinations and leap year rules.
     * <p>
     * Implements COBOL logic from CSUTLDPY.cpy EDIT-DAY-MONTH-YEAR:
     * - Checks 31-day restriction for non-31-day months
     * - Checks February 30-day restriction
     * - Implements leap year calculation for February 29th
     * </p>
     * 
     * @param month the month value
     * @param day the day value
     * @param year the full year value
     * @param context validation context
     * @return true if month/day combination is valid
     */
    private boolean validateMonthDayCombination(int month, int day, int year, ConstraintValidatorContext context) {
        // Check for 31 days in non-31-day months
        // Equivalent to COBOL: IF NOT WS-31-DAY-MONTH AND WS-DAY-31
        if (day == 31 && !isMonth31Days(month)) {
            buildConstraintViolation(context, annotation.monthDayMessage(),
                "Cannot have 31 days in this month");
            return false;
        }
        
        // Check for February 30 or 31
        // Equivalent to COBOL: IF WS-FEBRUARY AND WS-DAY-30
        if (month == FEBRUARY && day >= 30) {
            buildConstraintViolation(context, annotation.monthDayMessage(),
                "Cannot have " + day + " days in February");
            return false;
        }
        
        // Check for February 29 in non-leap years
        // Equivalent to COBOL leap year calculation logic
        if (annotation.checkLeapYear() && month == FEBRUARY && day == 29) {
            if (!isLeapYear(year)) {
                buildConstraintViolation(context, annotation.leapYearMessage(),
                    "Not a leap year. Cannot have 29 days in February");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks if a month has 31 days.
     * <p>
     * Implements COBOL 88-level condition WS-31-DAY-MONTH VALUES 1, 3, 5, 7, 8, 10, 12.
     * </p>
     * 
     * @param month the month to check
     * @return true if the month has 31 days
     */
    private boolean isMonth31Days(int month) {
        for (int thirtyOneDayMonth : MONTHS_WITH_31_DAYS) {
            if (month == thirtyOneDayMonth) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Determines if a year is a leap year.
     * <p>
     * Implements COBOL leap year calculation from CSUTLDPY.cpy:
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
     *    [leap year]
     * </pre>
     * </p>
     * 
     * @param year the year to check
     * @return true if the year is a leap year
     */
    private boolean isLeapYear(int year) {
        // COBOL logic: if year ends in 00, divide by 400; otherwise divide by 4
        int lastTwoDigits = year % 100;
        if (lastTwoDigits == 0) {
            return year % 400 == 0;
        } else {
            return year % 4 == 0;
        }
    }
    
    /**
     * Final date validation using Java LocalDate.
     * <p>
     * Equivalent to COBOL EDIT-DATE-LE which calls CSUTLDTC utility
     * to perform final date validation using Language Environment services.
     * This provides additional validation for edge cases that might pass
     * the individual component validations.
     * </p>
     * 
     * @param year the year value
     * @param month the month value
     * @param day the day value
     * @param context validation context
     * @return true if the date is valid
     */
    private boolean validateDateWithLocalDate(int year, int month, int day, ConstraintValidatorContext context) {
        try {
            // Use LocalDate to perform comprehensive date validation
            // This catches any edge cases that individual validations might miss
            LocalDate.of(year, month, day);
            return true;
        } catch (DateTimeParseException | IllegalArgumentException e) {
            buildConstraintViolation(context, annotation.message(),
                "Date validation error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Builds a constraint violation with custom message.
     * <p>
     * Helper method to create detailed error messages that match
     * the COBOL error message patterns from CSUTLDPY.cpy.
     * </p>
     * 
     * @param context validation context
     * @param message the error message template
     * @param details additional error details
     */
    private void buildConstraintViolation(ConstraintValidatorContext context, String message, String details) {
        context.buildConstraintViolationWithTemplate(details)
               .addConstraintViolation();
    }
}