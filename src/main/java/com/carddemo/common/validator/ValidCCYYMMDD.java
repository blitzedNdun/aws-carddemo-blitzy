package com.carddemo.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Jakarta Bean Validation annotation for CCYYMMDD date format validation.
 * <p>
 * This annotation validates date strings in CCYYMMDD format with comprehensive
 * validation logic that preserves the exact business rules from the original
 * COBOL date validation routines (CSUTLDPY.cpy, CSUTLDTC.cbl).
 * </p>
 * 
 * <h3>Validation Rules:</h3>
 * <ul>
 *   <li><strong>Century Validation:</strong> Only 19xx and 20xx centuries are valid</li>
 *   <li><strong>Year Validation:</strong> Must be numeric and not blank</li>
 *   <li><strong>Month Validation:</strong> Must be between 1 and 12</li>
 *   <li><strong>Day Validation:</strong> Must be between 1 and 31, considering month-specific rules</li>
 *   <li><strong>Leap Year Validation:</strong> February 29th only allowed in leap years</li>
 *   <li><strong>Month/Day Combination:</strong> Validates day limits per month (30 days in April, June, September, November)</li>
 * </ul>
 * 
 * <h3>Error Messages:</h3>
 * <p>
 * The validator provides structured error messages for each validation component
 * that match the original COBOL error handling patterns:
 * </p>
 * <ul>
 *   <li>Century validation errors</li>
 *   <li>Year format and range errors</li>
 *   <li>Month range errors</li>
 *   <li>Day range errors</li>
 *   <li>Leap year validation errors</li>
 *   <li>Month/day combination errors</li>
 * </ul>
 * 
 * <h3>Usage Examples:</h3>
 * <pre>
 * {@code
 * public class CustomerDto {
 *     @ValidCCYYMMDD
 *     private String dateOfBirth;
 *     
 *     @ValidCCYYMMDD(message = "Account opening date must be in CCYYMMDD format")
 *     private String openingDate;
 * }
 * }
 * </pre>
 * 
 * <h3>COBOL Equivalency:</h3>
 * <p>
 * This annotation implements the same validation logic as the COBOL paragraphs:
 * </p>
 * <ul>
 *   <li>EDIT-YEAR-CCYY - Century and year validation</li>
 *   <li>EDIT-MONTH - Month range validation</li>
 *   <li>EDIT-DAY - Day range validation</li>
 *   <li>EDIT-DAY-MONTH-YEAR - Complex date combination validation</li>
 *   <li>EDIT-DATE-LE - Final date validation using equivalent logic</li>
 * </ul>
 * 
 * @see CCYYMMDDValidator
 * @author Blitzy Platform
 * @version 1.0
 * @since 2024-01-01
 */
@Documented
@Constraint(validatedBy = CCYYMMDDValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCCYYMMDD {
    
    /**
     * Default error message for CCYYMMDD validation failures.
     * This message will be used when no specific error component is identified.
     * 
     * @return the default error message
     */
    String message() default "Date must be in valid CCYYMMDD format";
    
    /**
     * Validation groups for conditional validation.
     * Allows grouping of validations for different scenarios.
     * 
     * @return the validation groups
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for additional metadata.
     * Can be used to provide additional context or metadata about the validation.
     * 
     * @return the payload classes
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Custom error message for century validation failures.
     * Used when the century component (CC) is not 19 or 20.
     * 
     * @return the century error message
     */
    String centuryMessage() default "Century is not valid (only 19xx and 20xx are supported)";
    
    /**
     * Custom error message for year validation failures.
     * Used when the year component is not numeric or is blank.
     * 
     * @return the year error message
     */
    String yearMessage() default "Year must be a 4-digit number";
    
    /**
     * Custom error message for month validation failures.
     * Used when the month component is not between 1 and 12.
     * 
     * @return the month error message
     */
    String monthMessage() default "Month must be a number between 1 and 12";
    
    /**
     * Custom error message for day validation failures.
     * Used when the day component is not between 1 and 31.
     * 
     * @return the day error message
     */
    String dayMessage() default "Day must be a number between 1 and 31";
    
    /**
     * Custom error message for leap year validation failures.
     * Used when February 29th is specified in a non-leap year.
     * 
     * @return the leap year error message
     */
    String leapYearMessage() default "Not a leap year. Cannot have 29 days in February";
    
    /**
     * Custom error message for month/day combination validation failures.
     * Used when the day is invalid for the specified month.
     * 
     * @return the month/day combination error message
     */
    String monthDayMessage() default "Invalid day for the specified month";
    
    /**
     * Custom error message for format validation failures.
     * Used when the input string is not in the expected CCYYMMDD format.
     * 
     * @return the format error message
     */
    String formatMessage() default "Date must be in CCYYMMDD format (8 digits)";
    
    /**
     * Flag to allow blank or null values.
     * When true, blank or null values will pass validation.
     * When false, blank or null values will fail validation.
     * 
     * @return true if blank values are allowed, false otherwise
     */
    boolean allowBlank() default false;
    
    /**
     * Flag to enable strict century validation.
     * When true, enforces century validation as per COBOL logic.
     * When false, allows any century (for testing purposes).
     * 
     * @return true if strict century validation is enabled, false otherwise
     */
    boolean strictCentury() default true;
    
    /**
     * Flag to enable leap year validation.
     * When true, validates February 29th against leap year rules.
     * When false, allows February 29th in any year.
     * 
     * @return true if leap year validation is enabled, false otherwise
     */
    boolean checkLeapYear() default true;
    
    /**
     * Flag to enable month/day combination validation.
     * When true, validates day limits per month (30/31 days).
     * When false, allows any day 1-31 regardless of month.
     * 
     * @return true if month/day validation is enabled, false otherwise
     */
    boolean checkMonthDay() default true;
    
    /**
     * Defines several {@link ValidCCYYMMDD} annotations on the same element.
     * This allows for multiple validation configurations on a single field.
     * 
     * @see ValidCCYYMMDD
     */
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_USE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        ValidCCYYMMDD[] value();
    }
}