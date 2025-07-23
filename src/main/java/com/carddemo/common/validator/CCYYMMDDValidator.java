/*
 * CCYYMMDDValidator.java
 *
 * Implementation class for CCYYMMDD date format validation. Provides comprehensive date 
 * validation logic including century restrictions, leap year calculations, and month/day 
 * range validation that exactly replicates COBOL date processing behavior.
 *
 * This validator implements the business logic for the @ValidCCYYMMDD annotation,
 * ensuring dates conform to the exact validation rules defined in CSUTLDPY.cpy and
 * CSUTLDTC.cbl from the original CardDemo mainframe application.
 *
 * Copyright (c) 2024 CardDemo Application
 */
package com.carddemo.common.validator;

import com.carddemo.common.validator.ValidCCYYMMDD;
import com.carddemo.common.validator.ValidationConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Implementation class for CCYYMMDD date format validation.
 * 
 * This validator provides comprehensive date validation logic that preserves
 * the exact COBOL date validation semantics from the CardDemo mainframe application,
 * including century restrictions, leap year calculations, and month/day range validation
 * as defined in CSUTLDPY.cpy and CSUTLDTC.cbl.
 * 
 * <p>Validation Process:</p>
 * <ol>
 *   <li><strong>Format Check:</strong> Validates exactly 8 characters, all numeric</li>
 *   <li><strong>Century Validation:</strong> Ensures CC is 19 or 20 only</li>
 *   <li><strong>Year Validation:</strong> Validates YY is numeric and not 00</li>
 *   <li><strong>Month Validation:</strong> Ensures MM is between 01-12</li>
 *   <li><strong>Day Validation:</strong> Validates DD is appropriate for month/year</li>
 *   <li><strong>Leap Year Logic:</strong> February 29th validated with COBOL rules</li>
 * </ol>
 * 
 * <p>Century Restrictions:</p>
 * The validator restricts dates to 19xx and 20xx centuries (1900-2099) to maintain
 * compatibility with the original COBOL application's century handling logic.
 * 
 * <p>Leap Year Calculation:</p>
 * Uses exact COBOL leap year logic:
 * - Century years (e.g., 1900, 2000) must be divisible by 400
 * - Non-century years must be divisible by 4
 * 
 * @author Blitzy Platform - CardDemo Modernization Team
 * @see ValidCCYYMMDD
 * @see ValidationConstants
 */
public class CCYYMMDDValidator implements ConstraintValidator<ValidCCYYMMDD, String> {
    
    /**
     * Initialize the validator.
     * 
     * @param annotation the ValidCCYYMMDD annotation instance
     */
    @Override
    public void initialize(ValidCCYYMMDD annotation) {
        // No initialization parameters needed for this validator
    }
    
    /**
     * Validates the CCYYMMDD date string according to COBOL date validation logic.
     * 
     * This method performs comprehensive validation including format checking,
     * century restrictions, leap year calculations, and month/day range validation
     * exactly as implemented in the original COBOL date processing routines.
     * 
     * @param value the date string to validate (may be null)
     * @param context the validation context for custom error messages
     * @return true if the date is valid, false otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null and empty values are considered valid (use @NotNull/@NotEmpty for required fields)
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        
        // Disable default constraint violation to provide custom error messages
        context.disableDefaultConstraintViolation();
        
        // Step 1: Format validation - must be exactly 8 characters
        if (value.length() != 8) {
            context.buildConstraintViolationWithTemplate(
                "Date must be exactly 8 characters in CCYYMMDD format, got " + value.length() + " characters"
            ).addConstraintViolation();
            return false;
        }
        
        // Step 2: Numeric validation - all characters must be digits
        if (!ValidationConstants.NUMERIC_PATTERN.matcher(value).matches()) {
            context.buildConstraintViolationWithTemplate(
                "Date must contain only numeric characters (0-9) in CCYYMMDD format"
            ).addConstraintViolation();
            return false;
        }
        
        // Step 3: Parse date components
        try {
            int century = Integer.parseInt(value.substring(0, 2));
            int year = Integer.parseInt(value.substring(2, 4));
            int month = Integer.parseInt(value.substring(4, 6));
            int day = Integer.parseInt(value.substring(6, 8));
            
            // Step 4: Century validation - only 19xx and 20xx supported
            if (!ValidationConstants.VALID_CENTURIES.contains(century)) {
                context.buildConstraintViolationWithTemplate(
                    "Century must be 19 or 20 (1900-2099), got " + century
                ).addConstraintViolation();
                return false;
            }
            
            // Step 5: Year validation - cannot be 00 (COBOL business rule)
            if (year == 0) {
                context.buildConstraintViolationWithTemplate(
                    "Year cannot be 00 in CCYYMMDD format"
                ).addConstraintViolation();
                return false;
            }
            
            // Step 6: Month validation - must be 01-12
            if (!ValidationConstants.VALID_MONTHS.contains(month)) {
                context.buildConstraintViolationWithTemplate(
                    "Month must be between 01-12, got " + String.format("%02d", month)
                ).addConstraintViolation();
                return false;
            }
            
            // Step 7: Day validation - must be valid for the specific month/year
            int fullYear = (century * 100) + year;
            int maxDayForMonth = ValidationConstants.getMaxDayForMonth(month, fullYear);
            
            if (day < 1 || day > maxDayForMonth) {
                String monthName = getMonthName(month);
                if (month == ValidationConstants.FEBRUARY && day == 29) {
                    context.buildConstraintViolationWithTemplate(
                        "February 29 is not valid for year " + fullYear + " (not a leap year)"
                    ).addConstraintViolation();
                } else {
                    context.buildConstraintViolationWithTemplate(
                        "Day must be between 01-" + String.format("%02d", maxDayForMonth) + 
                        " for " + monthName + " " + fullYear + ", got " + String.format("%02d", day)
                    ).addConstraintViolation();
                }
                return false;
            }
            
            // Step 8: Additional validation using Java LocalDate for edge cases
            try {
                LocalDate.of(fullYear, month, day);
            } catch (DateTimeParseException e) {
                context.buildConstraintViolationWithTemplate(
                    "Invalid date combination: " + value + " does not represent a valid calendar date"
                ).addConstraintViolation();
                return false;
            }
            
            return true;
            
        } catch (NumberFormatException e) {
            // This should not happen due to numeric pattern check, but handle gracefully
            context.buildConstraintViolationWithTemplate(
                "Date components must be numeric in CCYYMMDD format"
            ).addConstraintViolation();
            return false;
        }
    }
    
    /**
     * Gets the month name for error messages.
     * 
     * @param month the month number (1-12)
     * @return the month name for display in error messages
     */
    private String getMonthName(int month) {
        switch (month) {
            case 1: return "January";
            case 2: return "February";
            case 3: return "March";
            case 4: return "April";
            case 5: return "May";
            case 6: return "June";
            case 7: return "July";
            case 8: return "August";
            case 9: return "September";
            case 10: return "October";
            case 11: return "November";
            case 12: return "December";
            default: return "Month " + month;
        }
    }
}