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
 * This annotation implements comprehensive date validation logic that preserves
 * the exact COBOL date validation semantics from the CardDemo mainframe application,
 * including century restrictions, leap year calculations, and month/day range validation
 * as defined in CSUTLDPY.cpy and CSUTLDTC.cbl.
 * 
 * <p>Validation Rules:</p>
 * <ul>
 *   <li><strong>Format:</strong> Exactly 8 characters in CCYYMMDD format</li>
 *   <li><strong>Century:</strong> Only 19xx and 20xx centuries supported (19 or 20)</li>
 *   <li><strong>Year:</strong> Must be numeric, cannot be blank or zeros</li>
 *   <li><strong>Month:</strong> Must be between 01-12</li>
 *   <li><strong>Day:</strong> Must be valid for the specified month and year</li>
 *   <li><strong>Leap Year:</strong> February 29th validated with COBOL-equivalent logic</li>
 *   <li><strong>Month-Day Combinations:</strong> Enforces correct days per month</li>
 * </ul>
 * 
 * <p>Supported Date Ranges:</p>
 * <ul>
 *   <li>January 1, 1900 (19000101) through December 31, 2099 (20991231)</li>
 *   <li>Leap year validation follows Gregorian calendar rules</li>
 *   <li>Century years (e.g., 1900, 2000) validated for divisibility by 400</li>
 *   <li>Non-century years validated for divisibility by 4</li>
 * </ul>
 * 
 * <p>Error Message Structure:</p>
 * The validator provides detailed error messages indicating the specific validation
 * failure component (century, year, month, day, or leap year), matching the
 * structured error reporting from the original COBOL validation routines.
 * 
 * <p>Example Usage:</p>
 * <pre>
 * public class CustomerRequest {
 *     &#64;ValidCCYYMMDD
 *     private String dateOfBirth;
 *     
 *     &#64;ValidCCYYMMDD(message = "Account open date must be valid CCYYMMDD format")
 *     private String accountOpenDate;
 * }
 * </pre>
 * 
 * <p>Valid Examples:</p>
 * <ul>
 *   <li>19850315 - March 15, 1985</li>
 *   <li>20000229 - February 29, 2000 (leap year)</li>
 *   <li>19991231 - December 31, 1999</li>
 *   <li>20240101 - January 1, 2024</li>
 * </ul>
 * 
 * <p>Invalid Examples:</p>
 * <ul>
 *   <li>18501231 - Century 18 not supported</li>
 *   <li>21001231 - Century 21 not supported</li>
 *   <li>19991301 - Month 13 invalid</li>
 *   <li>19990230 - February 30 invalid</li>
 *   <li>19990229 - February 29 invalid (1999 not leap year)</li>
 *   <li>19991131 - November 31 invalid (November has 30 days)</li>
 * </ul>
 * 
 * @see CCYYMMDDValidator
 * @since CardDemo v2.0
 * @author Blitzy Platform - CardDemo Modernization Team
 */
@Documented
@Constraint(validatedBy = CCYYMMDDValidator.class)
@Target({
    ElementType.METHOD,
    ElementType.FIELD,
    ElementType.ANNOTATION_TYPE,
    ElementType.CONSTRUCTOR,
    ElementType.PARAMETER,
    ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCCYYMMDD {
    
    /**
     * The default validation error message.
     * 
     * This message can be overridden by specifying a custom message when using
     * the annotation. The validator will provide specific error details based on
     * the validation failure type (invalid century, month, day, etc.).
     * 
     * @return the validation error message
     */
    String message() default "Date must be in valid CCYYMMDD format with supported century (19xx or 20xx)";
    
    /**
     * Specifies the validation groups to which this constraint belongs.
     * 
     * Groups allow for selective validation of different constraint sets.
     * By default, this constraint belongs to the default validation group.
     * 
     * @return the validation groups
     */
    Class<?>[] groups() default {};
    
    /**
     * Custom payload that can be associated with the constraint.
     * 
     * Payloads can be used by validation clients to assign custom payload
     * objects to a constraint. This is not used by the Bean Validation API itself.
     * 
     * @return the custom payload classes
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Defines several {@code @ValidCCYYMMDD} constraints on the same element.
     * 
     * This allows for multiple CCYYMMDD validations with different parameters
     * to be applied to the same field, method, or class.
     * 
     * @see ValidCCYYMMDD
     */
    @Target({
        ElementType.METHOD,
        ElementType.FIELD,
        ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR,
        ElementType.PARAMETER,
        ElementType.TYPE_USE
    })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface List {
        ValidCCYYMMDD[] value();
    }
}