/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Jakarta Bean Validation implementation for CCYYMMDD date format validation.
 * 
 * This validator provides comprehensive date validation logic that exactly replicates
 * COBOL date processing behavior from CSUTLDPY.cpy, including century restrictions,
 * leap year calculations, and month/day range validation.
 * 
 * Validation Rules (from COBOL CSUTLDPY.cpy):
 * 1. Format: Exactly 8 numeric characters (CCYYMMDD)
 * 2. Century: Only 19 and 20 are valid (1900s and 2000s) - THIS-CENTURY/LAST-CENTURY
 * 3. Year: Must be 4-digit numeric within century constraints
 * 4. Month: Must be 01-12 (WS-VALID-MONTH condition)
 * 5. Day: Must be 01-31 with proper month/year constraints (WS-VALID-DAY condition)
 * 6. Month/Day Cross-validation:
 *    - 31-day months: January, March, May, July, August, October, December
 *    - 30-day months: April, June, September, November  
 *    - February: 28 days normally, 29 days in leap years
 * 7. Leap Year: Uses exact COBOL algorithm from EDIT-DAY-MONTH-YEAR paragraph
 * 
 * COBOL Paragraph Mapping:
 * - EDIT-YEAR-CCYY → validateCenturyAndYear()
 * - EDIT-MONTH → validateMonth()
 * - EDIT-DAY → validateDay() 
 * - EDIT-DAY-MONTH-YEAR → validateDayMonthYear()
 * 
 * Error Messages:
 * Provides detailed validation error messages for each component failure,
 * matching COBOL validation error message style and content.
 * 
 * @see ValidCCYYMMDD
 * @see ValidationConstants
 * @since 1.0
 */
public class CCYYMMDDValidator implements ConstraintValidator<ValidCCYYMMDD, String> {
    
    private ValidCCYYMMDD annotation;
    
    /**
     * Initializes the validator with constraint annotation configuration.
     * Stores annotation parameters for use during validation.
     * 
     * @param constraintAnnotation The ValidCCYYMMDD annotation with configuration
     */
    @Override
    public void initialize(ValidCCYYMMDD constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }
    
    /**
     * Validates a date string in CCYYMMDD format using COBOL-equivalent validation logic.
     * 
     * Implements the complete validation flow from CSUTLDPY.cpy:
     * 1. Null/blank validation based on annotation settings
     * 2. Format validation (8 numeric characters)
     * 3. Century validation (19xx and 20xx only)
     * 4. Month validation (01-12)
     * 5. Day validation (01-31)
     * 6. Cross-validation for month/day combinations and leap year logic
     * 
     * @param value The date string to validate
     * @param context The constraint validator context for custom error messages
     * @return true if the date is valid, false otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Handle null values based on annotation configuration
        // Equivalent to COBOL LOW-VALUES check in EDIT-YEAR-CCYY
        if (value == null) {
            if (annotation.allowNull()) {
                return true;
            } else {
                buildCustomMessage(context, annotation.fieldName() + " must be supplied.");
                return false;
            }
        }
        
        // Handle blank/empty values based on annotation configuration
        // Equivalent to COBOL SPACES check in EDIT-YEAR-CCYY
        if (value.trim().isEmpty()) {
            if (annotation.allowBlank()) {
                return true;
            } else {
                buildCustomMessage(context, annotation.fieldName() + " must be supplied.");
                return false;
            }
        }
        
        // Validate format: exactly 8 characters
        if (value.length() != 8) {
            buildCustomMessage(context, annotation.fieldName() + " must be 8 characters in CCYYMMDD format.");
            return false;
        }
        
        // Validate numeric format - equivalent to COBOL NUMERIC check
        if (!ValidationConstants.NUMERIC_PATTERN.matcher(value).matches()) {
            buildCustomMessage(context, annotation.fieldName() + " must be 8 digit number.");
            return false;
        }
        
        // Extract date components
        String centuryStr = value.substring(0, 2);
        String yearStr = value.substring(2, 4);
        String monthStr = value.substring(4, 6);
        String dayStr = value.substring(6, 8);
        
        try {
            int century = Integer.parseInt(centuryStr);
            int year = Integer.parseInt(yearStr);
            int month = Integer.parseInt(monthStr);
            int day = Integer.parseInt(dayStr);
            
            // Validate century - equivalent to THIS-CENTURY/LAST-CENTURY check
            if (!validateCenturyAndYear(century, context)) {
                return false;
            }
            
            // Validate month - equivalent to WS-VALID-MONTH check
            if (!validateMonth(month, context)) {
                return false;
            }
            
            // Validate day - equivalent to WS-VALID-DAY check
            if (!validateDay(day, context)) {
                return false;
            }
            
            // Calculate full year for cross-validation
            int fullYear = (century * 100) + year;
            
            // Cross-validate day/month/year - equivalent to EDIT-DAY-MONTH-YEAR
            if (!validateDayMonthYear(day, month, fullYear, context)) {
                return false;
            }
            
            // Final validation using Java LocalDate to catch any edge cases
            // Equivalent to COBOL CEEDAYS API call in EDIT-DATE-LE
            try {
                LocalDate.of(fullYear, month, day);
            } catch (DateTimeParseException e) {
                buildCustomMessage(context, annotation.fieldName() + " validation error: Invalid date.");
                return false;
            }
            
            return true;
            
        } catch (NumberFormatException e) {
            buildCustomMessage(context, annotation.fieldName() + " must be numeric.");
            return false;
        }
    }
    
    /**
     * Validates century and year components.
     * Implements EDIT-YEAR-CCYY paragraph logic from CSUTLDPY.cpy.
     * 
     * Century validation rules:
     * - Only 19 (1900s) and 20 (2000s) are valid centuries
     * - Matches COBOL 88-level conditions THIS-CENTURY and LAST-CENTURY
     * 
     * @param century The century value (19 or 20)
     * @param context The constraint validator context for error messages
     * @return true if century is valid, false otherwise
     */
    private boolean validateCenturyAndYear(int century, ConstraintValidatorContext context) {
        // Century validation - equivalent to THIS-CENTURY OR LAST-CENTURY check
        // From CSUTLDWY.cpy: 88 THIS-CENTURY VALUE 20, 88 LAST-CENTURY VALUE 19
        if (annotation.strictCentury() && century != 19 && century != 20) {
            buildCustomMessage(context, annotation.fieldName() + " : Century is not valid.");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates month component.
     * Implements EDIT-MONTH paragraph logic from CSUTLDPY.cpy.
     * 
     * Month validation rules:
     * - Must be between 1 and 12 inclusive
     * - Matches COBOL WS-VALID-MONTH condition (VALUES 1 THROUGH 12)
     * 
     * @param month The month value (1-12)
     * @param context The constraint validator context for error messages
     * @return true if month is valid, false otherwise
     */
    private boolean validateMonth(int month, ConstraintValidatorContext context) {
        // Month range validation - equivalent to WS-VALID-MONTH check
        // From CSUTLDWY.cpy: 88 WS-VALID-MONTH VALUES 1 THROUGH 12
        if (month < 1 || month > 12) {
            buildCustomMessage(context, annotation.fieldName() + ": Month must be a number between 1 and 12.");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates day component.
     * Implements EDIT-DAY paragraph logic from CSUTLDPY.cpy.
     * 
     * Day validation rules:
     * - Must be between 1 and 31 inclusive
     * - Matches COBOL WS-VALID-DAY condition (VALUES 1 THROUGH 31)
     * 
     * @param day The day value (1-31)
     * @param context The constraint validator context for error messages
     * @return true if day is valid, false otherwise
     */
    private boolean validateDay(int day, ConstraintValidatorContext context) {
        // Day range validation - equivalent to WS-VALID-DAY check
        // From CSUTLDWY.cpy: 88 WS-VALID-DAY VALUES 1 THROUGH 31
        if (day < 1 || day > 31) {
            buildCustomMessage(context, annotation.fieldName() + ":day must be a number between 1 and 31.");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates day/month/year combinations and leap year logic.
     * Implements EDIT-DAY-MONTH-YEAR paragraph logic from CSUTLDPY.cpy.
     * 
     * Cross-validation rules:
     * 1. 31-day validation: Only certain months can have 31 days
     * 2. February validation: Cannot have 30 or 31 days
     * 3. Leap year validation: February 29th only valid in leap years
     * 4. Leap year algorithm: Exact COBOL logic from CSUTLDPY.cpy
     * 
     * @param day The day value
     * @param month The month value  
     * @param year The full year value (CCYY)
     * @param context The constraint validator context for error messages
     * @return true if the day/month/year combination is valid, false otherwise
     */
    private boolean validateDayMonthYear(int day, int month, int year, ConstraintValidatorContext context) {
        // Check for 31-day months - equivalent to WS-31-DAY-MONTH check
        // From CSUTLDWY.cpy: 88 WS-31-DAY-MONTH VALUES 1, 3, 5, 7, 8, 10, 12
        boolean is31DayMonth = ValidationConstants.MONTHS_WITH_31_DAYS.contains(month);
        
        // Validate 31st day for non-31-day months
        // Equivalent to: IF NOT WS-31-DAY-MONTH AND WS-DAY-31
        if (!is31DayMonth && day == 31) {
            buildCustomMessage(context, annotation.fieldName() + ":Cannot have 31 days in this month.");
            return false;
        }
        
        // February-specific validation
        // Equivalent to: IF WS-FEBRUARY
        if (month == ValidationConstants.FEBRUARY) {
            // February cannot have 30 days
            // Equivalent to: IF WS-FEBRUARY AND WS-DAY-30
            if (day == 30) {
                buildCustomMessage(context, annotation.fieldName() + ":Cannot have 30 days in this month.");
                return false;
            }
            
            // February 29th leap year validation
            // Equivalent to: IF WS-FEBRUARY AND WS-DAY-29
            if (day == 29) {
                if (!isLeapYearCobolLogic(year)) {
                    buildCustomMessage(context, annotation.fieldName() + ":Not a leap year.Cannot have 29 days in this month.");
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Implements exact COBOL leap year calculation logic from CSUTLDPY.cpy.
     * 
     * COBOL Algorithm (from EDIT-DAY-MONTH-YEAR paragraph):
     * 1. If year ends in 00 (year % 100 == 0), divide by 400
     * 2. Otherwise, divide by 4  
     * 3. If remainder is 0, it's a leap year
     * 
     * This preserves the exact COBOL logic:
     * - IF WS-EDIT-DATE-YY-N = 0 MOVE 400 TO WS-DIV-BY
     * - ELSE MOVE 4 TO WS-DIV-BY
     * - DIVIDE WS-EDIT-DATE-CCYY-N BY WS-DIV-BY GIVING WS-DIVIDEND REMAINDER WS-REMAINDER
     * - IF WS-REMAINDER = ZEROES (leap year)
     * 
     * @param year The full year (CCYY format)
     * @return true if the year is a leap year, false otherwise
     */
    private boolean isLeapYearCobolLogic(int year) {
        int divisor;
        
        // Equivalent to: IF WS-EDIT-DATE-YY-N = 0
        if ((year % 100) == 0) {
            divisor = 400;  // MOVE 400 TO WS-DIV-BY
        } else {
            divisor = 4;    // MOVE 4 TO WS-DIV-BY
        }
        
        // Equivalent to: DIVIDE ... REMAINDER WS-REMAINDER
        // IF WS-REMAINDER = ZEROES
        return (year % divisor) == 0;
    }
    
    /**
     * Builds a custom validation error message and disables the default message.
     * 
     * This method follows Jakarta Bean Validation best practices for custom
     * error messages while preserving COBOL-style error message formatting.
     * 
     * @param context The constraint validator context
     * @param message The custom error message to display
     */
    private void buildCustomMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
    }
}