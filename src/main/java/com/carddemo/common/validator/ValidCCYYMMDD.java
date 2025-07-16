package com.carddemo.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Jakarta Bean Validation annotation for CCYYMMDD date format validation.
 * 
 * This annotation validates date strings in CCYYMMDD format (8 characters) with
 * comprehensive validation logic equivalent to COBOL CSUTLDPY.cpy date validation routines.
 * 
 * Validation Rules:
 * - Century: Only 19 and 20 are valid (1900s and 2000s)
 * - Year: Must be 4-digit numeric (CCYY format)
 * - Month: Must be 01-12
 * - Day: Must be 01-31 with proper month/year constraints
 * - Leap Year: February 29th validation with proper leap year logic
 * - Month/Day Constraints: Validates days per month (30/31 day months, February limitations)
 * 
 * Error Messages:
 * - Provides structured validation error messages for each component
 * - Century validation error for invalid century values
 * - Month range validation error for invalid months
 * - Day range validation error for invalid days
 * - Leap year validation error for February 29th in non-leap years
 * - Month/day combination validation errors
 * 
 * Usage Example:
 * <pre>
 * public class PersonDTO {
 *     {@literal @}ValidCCYYMMDD
 *     private String birthDate;
 * }
 * </pre>
 * 
 * Conversion from COBOL CSUTLDPY.cpy:
 * - EDIT-YEAR-CCYY paragraph → Century and year validation
 * - EDIT-MONTH paragraph → Month range validation  
 * - EDIT-DAY paragraph → Day range validation
 * - EDIT-DAY-MONTH-YEAR paragraph → Cross-validation logic
 * - Leap year logic → Exact COBOL algorithm preservation
 * 
 * @see CCYYMMDDValidator
 * @since 1.0
 */
@Documented
@Constraint(validatedBy = CCYYMMDDValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCCYYMMDD {
    
    /**
     * Default validation error message.
     * Can be overridden in specific validation contexts.
     */
    String message() default "Invalid date format. Date must be in CCYYMMDD format with valid century (19xx or 20xx), month (01-12), and day (01-31)";
    
    /**
     * Validation groups for conditional validation.
     * Allows grouping of validation constraints for different scenarios.
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for metadata about the validation.
     * Can be used to associate additional information with the constraint.
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Variable name for error message context.
     * Used to provide field-specific error messages matching COBOL validation style.
     * Defaults to "Date" but can be customized per field.
     */
    String fieldName() default "Date";
    
    /**
     * Allow null values flag.
     * When true, null values pass validation (useful for optional fields).
     * When false, null values fail validation.
     * Defaults to true to match typical Jakarta Bean Validation behavior.
     */
    boolean allowNull() default true;
    
    /**
     * Allow blank/empty values flag.
     * When true, empty strings or whitespace-only strings pass validation.
     * When false, empty/blank values fail validation.
     * Defaults to true to match COBOL LOW-VALUES/SPACES handling.
     */
    boolean allowBlank() default true;
    
    /**
     * Strict century validation flag.
     * When true, only centuries 19 and 20 are valid (matching COBOL logic).
     * When false, allows additional centuries (for future extensibility).
     * Defaults to true to preserve COBOL date validation behavior.
     */
    boolean strictCentury() default true;
}