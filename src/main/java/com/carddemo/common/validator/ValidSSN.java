package com.carddemo.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Jakarta Bean Validation annotation for Social Security Number format validation.
 * 
 * This validator ensures SSN fields meet the required 9-digit numeric format
 * while supporting both formatted (XXX-XX-XXXX) and unformatted (XXXXXXXXX) 
 * input patterns consistent with COBOL customer record processing.
 * 
 * Validation Rules (derived from COBOL COACTUPC.CBL):
 * - Must be exactly 9 digits when formatted 
 * - Supports both XXX-XX-XXXX and XXXXXXXXX formats
 * - First 3 digits cannot be 000, 666, or 900-999
 * - Middle 2 digits must be 01-99 (cannot be 00)
 * - Last 4 digits must be 0001-9999 (cannot be 0000)
 * 
 * Error messages match COBOL validation message patterns for consistency
 * with the original mainframe application behavior.
 * 
 * Example usage:
 * <pre>
 * public class Customer {
 *     {@code @ValidSSN}
 *     private String socialSecurityNumber;
 * }
 * </pre>
 * 
 * @see SSNValidator for the actual validation implementation logic
 */
@Documented
@Constraint(validatedBy = SSNValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSSN {
    
    /**
     * Default validation error message for invalid SSN format.
     * Message template follows COBOL error message patterns for consistency.
     * 
     * @return the default error message template
     */
    String message() default "Social Security Number must be a valid 9-digit number in format XXX-XX-XXXX or XXXXXXXXX";
    
    /**
     * Validation groups for conditional validation scenarios.
     * Allows grouping validation constraints for different contexts.
     * 
     * @return array of validation group classes
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for carrying additional metadata about the validation constraint.
     * Can be used to attach severity levels or custom error handling information.
     * 
     * @return array of payload classes extending Payload interface
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Custom error message for first part validation failures.
     * Used when the first 3 digits fail business rule validation.
     * 
     * @return error message for invalid first part (XXX cannot be 000, 666, or 900-999)
     */
    String firstPartMessage() default "SSN: First 3 chars should not be 000, 666, or between 900 and 999";
    
    /**
     * Custom error message for second part validation failures.
     * Used when the middle 2 digits are invalid (cannot be 00).
     * 
     * @return error message for invalid second part (XX must be 01-99)
     */
    String secondPartMessage() default "SSN 4th & 5th chars must be between 01 and 99";
    
    /**
     * Custom error message for third part validation failures.
     * Used when the last 4 digits are invalid (cannot be 0000).
     * 
     * @return error message for invalid third part (XXXX must be 0001-9999)
     */
    String thirdPartMessage() default "SSN Last 4 chars must be between 0001 and 9999";
    
    /**
     * Flag to allow null values to pass validation.
     * When true, null SSN values are considered valid (for optional fields).
     * When false, null values trigger validation failure.
     * 
     * @return true if null values should be allowed, false otherwise
     */
    boolean allowNull() default true;
    
    /**
     * Flag to allow empty string values to pass validation.
     * When true, empty or blank SSN values are considered valid (for optional fields).
     * When false, empty values trigger validation failure.
     * 
     * @return true if empty values should be allowed, false otherwise
     */
    boolean allowEmpty() default true;
}