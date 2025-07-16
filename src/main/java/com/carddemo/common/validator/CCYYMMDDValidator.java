package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Jakarta Bean Validation validator implementation for CCYYMMDD date format validation.
 * 
 * This validator implements comprehensive date validation logic equivalent to the
 * COBOL CSUTLDPY.cpy date validation routines, providing exact functional equivalence
 * for date validation in the CardDemo application.
 * 
 * Validation Algorithm:
 * 1. Format validation: Must be exactly 8 numeric characters
 * 2. Century validation: Only 19 and 20 are valid (1900s and 2000s)
 * 3. Month validation: Must be 01-12
 * 4. Day validation: Must be 01-31 with month-specific constraints
 * 5. Leap year validation: February 29th requires leap year
 * 6. Month/day combination validation: Prevents invalid combinations
 * 
 * COBOL Equivalent Mapping:
 * - EDIT-YEAR-CCYY → validateCentury() and validateYear()
 * - EDIT-MONTH → validateMonth()
 * - EDIT-DAY → validateDay()
 * - EDIT-DAY-MONTH-YEAR → validateDayMonthYear()
 * - Leap year logic → isLeapYear()
 * 
 * Error Message Structure:
 * - Provides specific error messages for each validation failure
 * - Matches COBOL error message patterns and terminology
 * - Includes field name context for user-friendly error display
 * 
 * @see ValidCCYYMMDD
 * @since 1.0
 */
public class CCYYMMDDValidator implements ConstraintValidator<ValidCCYYMMDD, String> {
    
    // Pattern for 8-digit numeric string validation
    private static final Pattern CCYYMMDD_PATTERN = Pattern.compile("^\\d{8}$");
    
    // Valid century values (19 and 20 only, matching COBOL logic)
    private static final int VALID_CENTURY_19 = 19;
    private static final int VALID_CENTURY_20 = 20;
    
    // Month constants for validation
    private static final int MIN_MONTH = 1;
    private static final int MAX_MONTH = 12;
    private static final int FEBRUARY = 2;
    
    // Day constants for validation
    private static final int MIN_DAY = 1;
    private static final int MAX_DAY = 31;
    private static final int MAX_DAY_30 = 30;
    private static final int MAX_DAY_FEB = 28;
    private static final int MAX_DAY_FEB_LEAP = 29;
    
    // Months with 31 days (matching COBOL WS-31-DAY-MONTH logic)
    private static final int[] MONTHS_31_DAYS = {1, 3, 5, 7, 8, 10, 12};
    
    // Validator configuration
    private String fieldName;
    private boolean allowNull;
    private boolean allowBlank;
    private boolean strictCentury;
    
    @Override
    public void initialize(ValidCCYYMMDD constraintAnnotation) {
        this.fieldName = constraintAnnotation.fieldName();
        this.allowNull = constraintAnnotation.allowNull();
        this.allowBlank = constraintAnnotation.allowBlank();
        this.strictCentury = constraintAnnotation.strictCentury();
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Disable default constraint violation
        context.disableDefaultConstraintViolation();
        
        // Handle null values (matching COBOL LOW-VALUES check)
        if (value == null) {
            if (allowNull) {
                return true;
            }
            addConstraintViolation(context, fieldName + " must be supplied");
            return false;
        }
        
        // Handle blank/empty values (matching COBOL SPACES check)
        if (value.trim().isEmpty()) {
            if (allowBlank) {
                return true;
            }
            addConstraintViolation(context, fieldName + " must be supplied");
            return false;
        }
        
        // Validate format: must be exactly 8 numeric characters
        if (!CCYYMMDD_PATTERN.matcher(value).matches()) {
            addConstraintViolation(context, fieldName + " must be 8 digit number in CCYYMMDD format");
            return false;
        }
        
        // Extract date components
        int century = Integer.parseInt(value.substring(0, 2));
        int year = Integer.parseInt(value.substring(2, 4));
        int month = Integer.parseInt(value.substring(4, 6));
        int day = Integer.parseInt(value.substring(6, 8));
        int fullYear = (century * 100) + year;
        
        // Validate century (matching COBOL THIS-CENTURY and LAST-CENTURY logic)
        if (!validateCentury(century, context)) {
            return false;
        }
        
        // Validate month (matching COBOL WS-VALID-MONTH logic)
        if (!validateMonth(month, context)) {
            return false;
        }
        
        // Validate day (matching COBOL WS-VALID-DAY logic)
        if (!validateDay(day, context)) {
            return false;
        }
        
        // Validate day-month-year combinations (matching COBOL EDIT-DAY-MONTH-YEAR logic)
        if (!validateDayMonthYear(day, month, fullYear, context)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates century component matching COBOL EDIT-YEAR-CCYY logic.
     * Only centuries 19 and 20 are valid to prevent Y2K-style issues.
     */
    private boolean validateCentury(int century, ConstraintValidatorContext context) {
        if (strictCentury) {
            if (century != VALID_CENTURY_19 && century != VALID_CENTURY_20) {
                addConstraintViolation(context, fieldName + " : Century is not valid");
                return false;
            }
        }
        return true;
    }
    
    /**
     * Validates month component matching COBOL EDIT-MONTH logic.
     * Month must be between 1 and 12 inclusive.
     */
    private boolean validateMonth(int month, ConstraintValidatorContext context) {
        if (month < MIN_MONTH || month > MAX_MONTH) {
            addConstraintViolation(context, fieldName + " : Month must be a number between 1 and 12");
            return false;
        }
        return true;
    }
    
    /**
     * Validates day component matching COBOL EDIT-DAY logic.
     * Day must be between 1 and 31 inclusive.
     */
    private boolean validateDay(int day, ConstraintValidatorContext context) {
        if (day < MIN_DAY || day > MAX_DAY) {
            addConstraintViolation(context, fieldName + " : Day must be a number between 1 and 31");
            return false;
        }
        return true;
    }
    
    /**
     * Validates day-month-year combinations matching COBOL EDIT-DAY-MONTH-YEAR logic.
     * Handles month-specific day limits and leap year validation.
     */
    private boolean validateDayMonthYear(int day, int month, int fullYear, ConstraintValidatorContext context) {
        // Check for 31st day in months that don't have 31 days
        if (day == MAX_DAY && !isMonth31Days(month)) {
            addConstraintViolation(context, fieldName + " : Cannot have 31 days in this month");
            return false;
        }
        
        // Check for February 30th (invalid)
        if (month == FEBRUARY && day == MAX_DAY_30) {
            addConstraintViolation(context, fieldName + " : Cannot have 30 days in this month");
            return false;
        }
        
        // Check for February 29th in non-leap years
        if (month == FEBRUARY && day == MAX_DAY_FEB_LEAP) {
            if (!isLeapYear(fullYear)) {
                addConstraintViolation(context, fieldName + " : Not a leap year. Cannot have 29 days in this month");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Determines if a month has 31 days.
     * Matches COBOL WS-31-DAY-MONTH logic.
     */
    private boolean isMonth31Days(int month) {
        for (int thirtyOneDayMonth : MONTHS_31_DAYS) {
            if (month == thirtyOneDayMonth) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Determines if a year is a leap year.
     * Implements exact COBOL leap year logic from CSUTLDPY.cpy:
     * - If year ends in 00, must be divisible by 400
     * - Otherwise, must be divisible by 4
     */
    private boolean isLeapYear(int year) {
        // Extract last two digits of year
        int lastTwoDigits = year % 100;
        
        // If year ends in 00, check divisibility by 400
        if (lastTwoDigits == 0) {
            return (year % 400) == 0;
        } else {
            // Otherwise, check divisibility by 4
            return (year % 4) == 0;
        }
    }
    
    /**
     * Adds a constraint violation with the specified message.
     */
    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
    }
}