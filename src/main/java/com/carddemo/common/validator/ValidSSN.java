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
 * <p>
 * This annotation validates SSN fields to ensure they meet the required 9-digit numeric format 
 * consistent with COBOL customer record processing patterns. The validation enforces business 
 * rules equivalent to those implemented in the original COBOL programs COACTVWC and COACTUPC.
 * </p>
 * 
 * <h3>Validation Rules:</h3>
 * <ul>
 *   <li><strong>Format:</strong> Accepts both formatted (XXX-XX-XXXX) and unformatted (XXXXXXXXX) input</li>
 *   <li><strong>First 3 digits:</strong> Must not be 000, 666, or between 900-999 (as per COBOL INVALID-SSN-PART1 validation)</li>
 *   <li><strong>Middle 2 digits:</strong> Must be 01-99 (cannot be 00)</li>
 *   <li><strong>Last 4 digits:</strong> Must be 0001-9999 (cannot be 0000)</li>
 *   <li><strong>Length:</strong> Must be exactly 9 digits after removing formatting characters</li>
 * </ul>
 * 
 * <h3>COBOL Compatibility:</h3>
 * <p>
 * This validation replicates the SSN validation logic from the COBOL program COACTUPC.cbl
 * (lines 2431-2491) which implements the 1265-EDIT-US-SSN paragraph. The validation maintains
 * exact compatibility with the COBOL business rules including:
 * </p>
 * <ul>
 *   <li>INVALID-SSN-PART1 validation (VALUES 0, 666, 900 THRU 999)</li>
 *   <li>Non-zero validation for parts 2 and 3</li>
 *   <li>Exact 9-digit numeric format requirement matching CUST-SSN PIC 9(09)</li>
 * </ul>
 * 
 * <h3>Usage Examples:</h3>
 * <pre>
 * {@code
 * public class CustomerDTO {
 *     @ValidSSN
 *     private String socialSecurityNumber;
 * 
 *     @ValidSSN(message = "Customer SSN format is invalid")
 *     private String customerSSN;
 * 
 *     @ValidSSN(groups = {CreateCustomer.class, UpdateCustomer.class})
 *     private String ssn;
 * }
 * }
 * </pre>
 * 
 * <h3>Error Messages:</h3>
 * <p>
 * The default error message matches COBOL validation message patterns for consistency with
 * the original application's user experience. Custom messages can be provided using the
 * message attribute.
 * </p>
 * 
 * @author Blitzy CardDemo Platform
 * @version 1.0
 * @since 1.0
 * 
 * @see com.carddemo.common.validator.SSNValidator
 */
@Documented
@Constraint(validatedBy = SSNValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSSN {
    
    /**
     * Default validation error message.
     * <p>
     * This message format matches the COBOL validation message patterns used in the original
     * CardDemo application for consistency in user experience. The message follows the format
     * used in COACTUPC.cbl for SSN validation errors.
     * </p>
     * 
     * @return the error message when validation fails
     */
    String message() default "SSN must be a valid 9-digit number in format XXX-XX-XXXX or XXXXXXXXX";
    
    /**
     * Validation groups for conditional validation.
     * <p>
     * Allows the SSN validation to be applied conditionally based on validation groups,
     * supporting different validation scenarios such as customer creation versus updates.
     * </p>
     * 
     * @return the validation groups
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for carrying additional metadata.
     * <p>
     * Provides a way to carry additional information about the validation constraint
     * that can be used by validation clients for enhanced error handling or context.
     * </p>
     * 
     * @return the payload classes
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Indicates whether formatted input (XXX-XX-XXXX) is required.
     * <p>
     * When set to true, the validator will require the input to be in the formatted
     * XXX-XX-XXXX pattern. When false (default), both formatted and unformatted
     * inputs are accepted, matching the flexible input handling in COBOL programs.
     * </p>
     * 
     * @return true if formatted input is required, false to accept both formats
     */
    boolean requireFormatted() default false;
    
    /**
     * Indicates whether null values should be considered valid.
     * <p>
     * When set to true, null values will pass validation. When false (default),
     * null values will fail validation. This allows for optional SSN fields
     * while maintaining strict validation when a value is provided.
     * </p>
     * 
     * @return true if null values are valid, false otherwise
     */
    boolean allowNull() default false;
    
    /**
     * Indicates whether empty strings should be considered valid.
     * <p>
     * When set to true, empty strings will pass validation. When false (default),
     * empty strings will fail validation. This provides flexibility for optional
     * fields while maintaining validation integrity.
     * </p>
     * 
     * @return true if empty strings are valid, false otherwise
     */
    boolean allowEmpty() default false;
}