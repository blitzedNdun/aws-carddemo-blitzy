package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Implementation class for CCYYMMDD date format validation.
 * <p>
 * This validator provides comprehensive date validation logic including century restrictions,
 * leap year calculations, and month/day range validation that exactly replicates COBOL date
 * processing behavior from CSUTLDPY.cpy and related copybooks.
 * </p>
 * 
 * <h3>Validation Logic:</h3>
 * <ul>
 *   <li><strong>Format Validation:</strong> Ensures input is exactly 8 digits in CCYYMMDD format</li>
 *   <li><strong>Century Validation:</strong> Restricts to 19xx and 20xx centuries (configurable)</li>
 *   <li><strong>Year Validation:</strong> Validates year component for numeric format</li>
 *   <li><strong>Month Validation:</strong> Ensures month is between 1 and 12</li>
 *   <li><strong>Day Validation:</strong> Validates day range considering month-specific limits</li>
 *   <li><strong>Leap Year Validation:</strong> Validates February 29th only in leap years</li>
 *   <li><strong>Month/Day Combination:</strong> Validates day limits per month (30/31 days)</li>
 * </ul>
 * 
 * <h3>COBOL Equivalency:</h3>
 * <p>
 * This implementation mirrors the validation logic from these COBOL paragraphs:
 * </p>
 * <ul>
 *   <li>EDIT-YEAR-CCYY - Century and year validation</li>
 *   <li>EDIT-MONTH - Month range validation (1-12)</li>
 *   <li>EDIT-DAY - Day range validation considering month</li>
 *   <li>EDIT-DAY-MONTH-YEAR - Complex date combination validation</li>
 *   <li>EDIT-DATE-LE - Final date validation using leap year logic</li>
 * </ul>
 * 
 * @see ValidCCYYMMDD
 * @author Blitzy Platform
 * @version 1.0
 * @since 2024-01-01
 */
public class ValidCCYYMMDDValidator implements ConstraintValidator<ValidCCYYMMDD, String> {
    
    private ValidCCYYMMDD annotation;
    
    /**
     * Initialize the validator with annotation parameters.
     * This method is called once when the validator is created.
     * 
     * @param constraintAnnotation the annotation instance for this constraint
     */
    @Override
    public void initialize(ValidCCYYMMDD constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }
    
    /**
     * Validate the date string against CCYYMMDD format and business rules.
     * 
     * @param value the date string to validate
     * @param context the validation context for error reporting
     * @return true if the date is valid, false otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Handle null and blank values
        if (value == null || value.trim().isEmpty()) {
            if (annotation.allowBlank()) {
                return true;
            } else {
                addViolation(context, annotation.formatMessage());
                return false;
            }
        }
        
        // Remove leading/trailing whitespace
        value = value.trim();
        
        // Check format: must be exactly 8 digits
        if (!isValidFormat(value)) {
            addViolation(context, annotation.formatMessage());
            return false;
        }
        
        // Extract date components
        String centuryStr = value.substring(0, 2);
        String yearStr = value.substring(2, 4);
        String monthStr = value.substring(4, 6);
        String dayStr = value.substring(6, 8);
        
        // Validate century (CC)
        if (annotation.strictCentury() && !isValidCentury(centuryStr)) {
            addViolation(context, annotation.centuryMessage());
            return false;
        }
        
        // Validate year (YY)
        if (!isValidYear(yearStr)) {
            addViolation(context, annotation.yearMessage());
            return false;
        }
        
        // Validate month (MM)
        int month = Integer.parseInt(monthStr);
        if (!isValidMonth(month)) {
            addViolation(context, annotation.monthMessage());
            return false;
        }
        
        // Validate day (DD)
        int day = Integer.parseInt(dayStr);
        if (!isValidDay(day)) {
            addViolation(context, annotation.dayMessage());
            return false;
        }
        
        // Validate month/day combination if enabled
        if (annotation.checkMonthDay() && !isValidMonthDayCombo(month, day)) {
            addViolation(context, annotation.monthDayMessage());
            return false;
        }
        
        // Validate leap year if enabled
        if (annotation.checkLeapYear()) {
            int century = Integer.parseInt(centuryStr);
            int year = Integer.parseInt(yearStr);
            int fullYear = (century * 100) + year;
            
            if (!isValidLeapYearCombo(fullYear, month, day)) {
                addViolation(context, annotation.leapYearMessage());
                return false;
            }
        }
        
        // All validations passed
        return true;
    }
    
    /**
     * Validate the format is exactly 8 digits.
     * 
     * @param value the input string
     * @return true if format is valid
     */
    private boolean isValidFormat(String value) {
        return value != null && value.length() == 8 && value.matches("\\d{8}");
    }
    
    /**
     * Validate century component (19xx and 20xx only).
     * 
     * @param centuryStr the century string (CC)
     * @return true if century is valid
     */
    private boolean isValidCentury(String centuryStr) {
        try {
            int century = Integer.parseInt(centuryStr);
            return century == 19 || century == 20;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Validate year component.
     * 
     * @param yearStr the year string (YY)
     * @return true if year is valid
     */
    private boolean isValidYear(String yearStr) {
        try {
            int year = Integer.parseInt(yearStr);
            return year >= 0 && year <= 99;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Validate month component (1-12).
     * 
     * @param month the month value
     * @return true if month is valid
     */
    private boolean isValidMonth(int month) {
        return month >= 1 && month <= 12;
    }
    
    /**
     * Validate day component (1-31).
     * 
     * @param day the day value
     * @return true if day is valid
     */
    private boolean isValidDay(int day) {
        return day >= 1 && day <= 31;
    }
    
    /**
     * Validate month/day combination considering month-specific day limits.
     * 
     * @param month the month value
     * @param day the day value
     * @return true if combination is valid
     */
    private boolean isValidMonthDayCombo(int month, int day) {
        // Months with 31 days: 1, 3, 5, 7, 8, 10, 12
        if (month == 1 || month == 3 || month == 5 || month == 7 || 
            month == 8 || month == 10 || month == 12) {
            return day <= 31;
        }
        
        // Months with 30 days: 4, 6, 9, 11
        if (month == 4 || month == 6 || month == 9 || month == 11) {
            return day <= 30;
        }
        
        // February: handle separately (28/29 days)
        if (month == 2) {
            return day <= 29; // Leap year validation happens separately
        }
        
        return false;
    }
    
    /**
     * Validate leap year combination for February 29th.
     * 
     * @param fullYear the complete year (CCYY)
     * @param month the month value
     * @param day the day value
     * @return true if combination is valid
     */
    private boolean isValidLeapYearCombo(int fullYear, int month, int day) {
        // Only check February 29th
        if (month == 2 && day == 29) {
            return isLeapYear(fullYear);
        }
        
        // February with 28 or fewer days is always valid
        if (month == 2 && day <= 28) {
            return true;
        }
        
        // All other months are handled by month/day combo validation
        return true;
    }
    
    /**
     * Check if a year is a leap year using the standard leap year rules.
     * A year is a leap year if:
     * - It is divisible by 4 AND
     * - If divisible by 100, it must also be divisible by 400
     * 
     * @param year the year to check
     * @return true if the year is a leap year
     */
    private boolean isLeapYear(int year) {
        return (year % 4 == 0) && ((year % 100 != 0) || (year % 400 == 0));
    }
    
    /**
     * Add a constraint violation with custom message.
     * 
     * @param context the validation context
     * @param message the error message
     */
    private void addViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}