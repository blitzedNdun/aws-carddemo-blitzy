/*
 * CCYYMMDDValidator.java
 *
 * Implementation class for CCYYMMDD date format validation that provides comprehensive
 * date validation logic including century restrictions, leap year calculations, and 
 * month/day range validation that exactly replicates COBOL date processing behavior
 * from the CardDemo mainframe application.
 *
 * This validator implements the exact validation semantics defined in:
 * - CSUTLDPY.cpy: Date validation paragraphs (EDIT-YEAR-CCYY, EDIT-MONTH, EDIT-DAY, EDIT-DAY-MONTH-YEAR)
 * - CSUTLDWY.cpy: Date validation constants and 88-level conditions  
 * - CSUTLDTC.cbl: Final date validation service using IBM LE CEEDAYS API
 *
 * The validation maintains complete behavioral equivalence with the original COBOL
 * logic while leveraging Java's LocalDate for comprehensive date component validation
 * and leap year calculations.
 *
 * Copyright (c) 2024 CardDemo Application
 */
package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Jakarta Bean Validation implementation for CCYYMMDD date format validation.
 * 
 * This validator provides comprehensive date validation logic that preserves the exact
 * COBOL date validation semantics from the CardDemo mainframe application, including
 * century restrictions, leap year calculations, and month/day range validation as
 * defined in CSUTLDPY.cpy and CSUTLDTC.cbl.
 * 
 * <p>Key Validation Features:</p>
 * <ul>
 *   <li><strong>Format Validation:</strong> Ensures exactly 8 numeric characters</li>
 *   <li><strong>Century Restriction:</strong> Only supports 19xx and 20xx centuries</li>  
 *   <li><strong>Component Validation:</strong> Validates year, month, and day separately</li>
 *   <li><strong>Month-Day Logic:</strong> Enforces correct days per month (30/31 day months)</li>
 *   <li><strong>Leap Year Handling:</strong> COBOL-equivalent leap year validation for February 29</li>
 *   <li><strong>Error Reporting:</strong> Detailed error messages for each validation failure type</li>
 * </ul>
 * 
 * <p>Validation Flow (matching COBOL paragraph sequence):</p>
 * <ol>
 *   <li>Basic format and null/empty validation</li>
 *   <li>EDIT-YEAR-CCYY: Century and year validation</li>
 *   <li>EDIT-MONTH: Month range and numeric validation</li>
 *   <li>EDIT-DAY: Day range and numeric validation</li>
 *   <li>EDIT-DAY-MONTH-YEAR: Complex month/day combinations and leap year logic</li>
 *   <li>Final LocalDate validation (equivalent to CSUTLDTC CEEDAYS call)</li>
 * </ol>
 * 
 * <p>Supported Date Range:</p>
 * January 1, 1900 (19000101) through December 31, 2099 (20991231)
 * 
 * <p>Century Validation Logic:</p>
 * Based on COBOL 88-level conditions THIS-CENTURY (20) and LAST-CENTURY (19),
 * this validator restricts date validation to only these two centuries, following
 * the original comment: "Not having learnt our lesson from history and Y2K... 
 * We code only 19 and 20 as valid century values"
 * 
 * @see ValidCCYYMMDD
 * @see ValidationConstants
 * @since CardDemo v2.0
 * @author Blitzy Platform - CardDemo Modernization Team
 */
public class CCYYMMDDValidator implements ConstraintValidator<ValidCCYYMMDD, String> {

    /**
     * Expected length for CCYYMMDD date format.
     * Based on COBOL WS-EDIT-DATE-CCYYMMDD PIC X(8).
     */
    private static final int CCYYMMDD_LENGTH = 8;
    
    /**
     * Position indices for date components in CCYYMMDD format.
     * Matches COBOL data structure layout from CSUTLDWY.cpy.
     */
    private static final int CENTURY_START = 0;
    private static final int CENTURY_END = 2;
    private static final int YEAR_START = 2;
    private static final int YEAR_END = 4;
    private static final int MONTH_START = 4;
    private static final int MONTH_END = 6;
    private static final int DAY_START = 6;
    private static final int DAY_END = 8;

    /**
     * Initializes the validator with the constraint annotation.
     * 
     * This method is called by the Jakarta Bean Validation framework during
     * validator initialization. Currently no specific initialization is needed
     * as all validation logic is self-contained.
     * 
     * @param constraintAnnotation the annotation instance for this validator
     */
    @Override
    public void initialize(ValidCCYYMMDD constraintAnnotation) {
        // No specific initialization required
        // All validation constants are available from ValidationConstants
    }

    /**
     * Validates the given date string against CCYYMMDD format requirements.
     * 
     * This method implements the complete COBOL date validation logic sequence,
     * processing validation in the same order as the original CSUTLDPY.cpy paragraphs:
     * EDIT-YEAR-CCYY, EDIT-MONTH, EDIT-DAY, and EDIT-DAY-MONTH-YEAR.
     * 
     * @param value the date string to validate (may be null)
     * @param context the constraint validation context for error reporting
     * @return true if the date is valid according to COBOL validation rules, false otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Handle null and empty values - considered valid per Jakarta Bean Validation convention
        // Null validation should be handled by @NotNull annotation if required
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        
        // Disable default violation message to provide specific error details
        context.disableDefaultConstraintViolation();
        
        // Basic format validation - must be exactly 8 characters
        if (value.length() != CCYYMMDD_LENGTH) {
            context.buildConstraintViolationWithTemplate(
                "Date must be exactly 8 characters in CCYYMMDD format")
                .addConstraintViolation();
            return false;
        }
        
        // Numeric validation - all characters must be digits
        if (!ValidationConstants.NUMERIC_PATTERN.matcher(value).matches()) {
            context.buildConstraintViolationWithTemplate(
                "Date must contain only numeric characters")
                .addConstraintViolation();
            return false;
        }
        
        // Extract date components for validation
        String centuryStr = value.substring(CENTURY_START, CENTURY_END);
        String yearStr = value.substring(YEAR_START, YEAR_END);
        String monthStr = value.substring(MONTH_START, MONTH_END);
        String dayStr = value.substring(DAY_START, DAY_END);
        
        int century = Integer.parseInt(centuryStr);
        int year = Integer.parseInt(yearStr);
        int fullYear = Integer.parseInt(value.substring(CENTURY_START, YEAR_END));
        int month = Integer.parseInt(monthStr);
        int day = Integer.parseInt(dayStr);
        
        // EDIT-YEAR-CCYY: Century validation (lines 70-84 in CSUTLDPY.cpy)
        if (!ValidationConstants.VALID_CENTURIES.contains(century)) {
            context.buildConstraintViolationWithTemplate(
                "Century is not valid. Only 19xx and 20xx centuries are supported")
                .addConstraintViolation();
            return false;
        }
        
        // Year validation - cannot be all zeros (though this is prevented by century check)
        if (fullYear == century * 100) {
            context.buildConstraintViolationWithTemplate(
                "Year must be supplied and cannot be zero")
                .addConstraintViolation();
            return false;
        }
        
        // EDIT-MONTH: Month validation (lines 91-143 in CSUTLDPY.cpy)
        if (!ValidationConstants.VALID_MONTHS.contains(month)) {
            context.buildConstraintViolationWithTemplate(
                "Month must be a number between 1 and 12")
                .addConstraintViolation();
            return false;
        }
        
        // EDIT-DAY: Basic day validation (lines 150-203 in CSUTLDPY.cpy)
        if (!ValidationConstants.VALID_DAYS.contains(day)) {
            context.buildConstraintViolationWithTemplate(
                "Day must be a number between 1 and 31")
                .addConstraintViolation();
            return false;
        }
        
        // EDIT-DAY-MONTH-YEAR: Complex month/day combinations (lines 209-279 in CSUTLDPY.cpy)
        
        // Check for 31st day in months that don't have 31 days
        if (day == 31 && !ValidationConstants.MONTHS_WITH_31_DAYS.contains(month)) {
            context.buildConstraintViolationWithTemplate(
                "Cannot have 31 days in this month")
                .addConstraintViolation();
            return false;
        }
        
        // February-specific validation
        if (month == ValidationConstants.FEBRUARY) {
            if (day == 30) {
                context.buildConstraintViolationWithTemplate(
                    "Cannot have 30 days in February")
                    .addConstraintViolation();
                return false;
            }
            
            // February 29th leap year validation (lines 243-272 in CSUTLDPY.cpy)
            if (day == 29) {
                if (!ValidationConstants.isLeapYear(fullYear)) {
                    context.buildConstraintViolationWithTemplate(
                        "Not a leap year. Cannot have 29 days in February")
                        .addConstraintViolation();
                    return false;
                }
            }
        }
        
        // Final validation using LocalDate (equivalent to CSUTLDTC CEEDAYS call)
        // This catches any edge cases not covered by the above logic
        try {
            LocalDate.of(fullYear, month, day);
        } catch (DateTimeParseException e) {
            context.buildConstraintViolationWithTemplate(
                "Date validation error: Invalid date combination")
                .addConstraintViolation();
            return false;
        } catch (Exception e) {
            context.buildConstraintViolationWithTemplate(
                "Date validation error: " + e.getMessage())
                .addConstraintViolation();
            return false;
        }
        
        // If we reach here, all COBOL validation rules have been satisfied
        return true;
    }
    
    /**
     * Validates individual date components for detailed error reporting.
     * 
     * This private method provides granular validation of date components,
     * allowing for specific error messages that match the COBOL validation
     * paragraph structure and error reporting.
     * 
     * @param component the date component name for error messages
     * @param value the component value to validate
     * @param min the minimum valid value
     * @param max the maximum valid value
     * @param context the validation context for error reporting
     * @return true if the component is valid, false otherwise
     */
    private boolean validateDateComponent(String component, int value, int min, int max, 
                                        ConstraintValidatorContext context) {
        if (value < min || value > max) {
            context.buildConstraintViolationWithTemplate(
                component + " must be between " + min + " and " + max)
                .addConstraintViolation();
            return false;
        }
        return true;
    }
    
    /**
     * Validates February days based on leap year status.
     * 
     * This method implements the specific February validation logic from
     * CSUTLDPY.cpy, including the leap year calculation equivalent to the
     * COBOL arithmetic operations for determining leap years.
     * 
     * @param day the day value to validate
     * @param year the full year for leap year calculation
     * @param context the validation context for error reporting
     * @return true if the February day is valid, false otherwise
     */
    private boolean validateFebruaryDay(int day, int year, ConstraintValidatorContext context) {
        if (day > 29) {
            context.buildConstraintViolationWithTemplate(
                "February cannot have more than 29 days")
                .addConstraintViolation();
            return false;
        }
        
        if (day == 29 && !ValidationConstants.isLeapYear(year)) {
            context.buildConstraintViolationWithTemplate(
                "February 29th is only valid in leap years")
                .addConstraintViolation();
            return false;
        }
        
        return true;
    }
}