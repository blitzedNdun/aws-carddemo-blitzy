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
 * This annotation validates SSN fields to ensure they meet the required 9-digit 
 * numeric format while supporting both formatted and unformatted input patterns
 * consistent with COBOL customer record processing.
 * 
 * <p>Validation Rules (based on COBOL patterns from COACTUPC.cbl):</p>
 * <ul>
 *   <li>Must be exactly 9 digits when formatted or unformatted</li>
 *   <li>First 3 digits cannot be 000, 666, or 900-999</li>
 *   <li>Middle 2 digits must be 01-99</li>
 *   <li>Last 4 digits must be 0001-9999</li>
 *   <li>Supports both XXX-XX-XXXX and XXXXXXXXX formats</li>
 * </ul>
 * 
 * <p>Error Message Patterns:</p>
 * <ul>
 *   <li>Matches COBOL validation message patterns for consistency</li>
 *   <li>Provides specific error messages for different validation failures</li>
 *   <li>Supports field-level error display equivalent to DFHRED highlighting</li>
 * </ul>
 * 
 * <p>Usage Examples:</p>
 * <pre>
 * // Basic usage on entity fields
 * &#64;ValidSSN
 * private String socialSecurityNumber;
 * 
 * // Custom error message
 * &#64;ValidSSN(message = "Invalid SSN format for customer record")
 * private String customerSSN;
 * 
 * // Validation groups
 * &#64;ValidSSN(groups = {CustomerUpdate.class})
 * private String ssnField;
 * </pre>
 * 
 * @author Blitzy Agent
 * @since 1.0
 * @see com.carddemo.common.validator.SSNValidator
 */
@Documented
@Constraint(validatedBy = SSNValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSSN {
    
    /**
     * Default error message template for SSN validation failures.
     * This message matches the COBOL validation pattern style for consistency.
     * 
     * @return the error message template
     */
    String message() default "SSN must be a valid 9-digit format (XXX-XX-XXXX or XXXXXXXXX)";
    
    /**
     * Validation groups to which this constraint belongs.
     * Allows for conditional validation based on different contexts.
     * 
     * @return the validation groups
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for carrying additional metadata about the validation constraint.
     * Can be used by validation frameworks for additional processing.
     * 
     * @return the payload classes
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Flag to allow blank/null values to pass validation.
     * When true, null or empty strings are considered valid.
     * When false, null or empty strings fail validation.
     * 
     * @return true if blank values are allowed, false otherwise
     */
    boolean allowBlank() default false;
    
    /**
     * Flag to enforce strict SSN format validation.
     * When true, applies all COBOL-based validation rules including:
     * - First 3 digits cannot be 000, 666, or 900-999
     * - Middle 2 digits must be 01-99
     * - Last 4 digits must be 0001-9999
     * 
     * When false, only validates basic 9-digit numeric format.
     * 
     * @return true for strict validation, false for basic validation
     */
    boolean strict() default true;
    
    /**
     * Custom error message for invalid SSN part 1 (first 3 digits).
     * Matches COBOL error message pattern from COACTUPC.cbl validation.
     * 
     * @return the error message for part 1 validation failures
     */
    String part1ErrorMessage() default "SSN: First 3 chars should not be 000, 666, or between 900 and 999";
    
    /**
     * Custom error message for invalid SSN part 2 (middle 2 digits).
     * Matches COBOL error message pattern from COACTUPC.cbl validation.
     * 
     * @return the error message for part 2 validation failures
     */
    String part2ErrorMessage() default "SSN 4th & 5th chars must be between 01 and 99";
    
    /**
     * Custom error message for invalid SSN part 3 (last 4 digits).
     * Matches COBOL error message pattern from COACTUPC.cbl validation.
     * 
     * @return the error message for part 3 validation failures
     */
    String part3ErrorMessage() default "SSN Last 4 chars must be between 0001 and 9999";
    
    /**
     * Custom error message for invalid SSN format.
     * Used when the input doesn't match expected format patterns.
     * 
     * @return the error message for format validation failures
     */
    String formatErrorMessage() default "SSN must be in format XXX-XX-XXXX or XXXXXXXXX";
}