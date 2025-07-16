package com.carddemo.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Jakarta Bean Validation annotation for credit card number validation using Luhn algorithm.
 * 
 * <p>This annotation validates credit card numbers according to the following rules:
 * <ul>
 *   <li>Must be exactly 16 digits in length (matching COBOL CARD-NUM PIC X(16))</li>
 *   <li>Must contain only numeric characters (0-9)</li>
 *   <li>Must pass Luhn algorithm checksum verification</li>
 *   <li>Preserves exact COBOL validation logic equivalent to COCRDLIC.CBL validation</li>
 * </ul>
 * 
 * <p>The Luhn algorithm implementation follows the standard specification:
 * <ul>
 *   <li>Starting from the rightmost digit, double every second digit</li>
 *   <li>If doubling results in a number greater than 9, subtract 9</li>
 *   <li>Sum all the digits</li>
 *   <li>Card number is valid if the total is divisible by 10</li>
 * </ul>
 * 
 * <p>Error messages are structured for compatibility with React frontend components
 * and maintain consistency with original COBOL error handling patterns.
 * 
 * <p>Example usage:
 * <pre>
 * public class CardDto {
 *     &#64;ValidCardNumber(message = "Invalid credit card number")
 *     private String cardNumber;
 *     
 *     &#64;ValidCardNumber(groups = {UpdateValidation.class})
 *     private String updatedCardNumber;
 * }
 * </pre>
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 1.0
 */
@Documented
@Constraint(validatedBy = CardNumberValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCardNumber {
    
    /**
     * Default error message for card number validation failures.
     * 
     * <p>This message is compatible with React frontend error handling
     * and maintains consistency with COBOL error messages from COCRDLIC.CBL.
     * The message supports internationalization through Spring's MessageSource.
     * 
     * @return the default error message
     */
    String message() default "Card number must be a valid 16-digit credit card number";
    
    /**
     * Validation groups that this constraint belongs to.
     * 
     * <p>Allows for different validation contexts such as:
     * <ul>
     *   <li>Create operations - full validation including Luhn check</li>
     *   <li>Update operations - may have different validation rules</li>
     *   <li>Search operations - format validation only</li>
     * </ul>
     * 
     * <p>This supports the original COBOL validation patterns where different
     * transaction types (COCRDLIC, COCRDUPC) may have varying validation requirements.
     * 
     * @return the validation groups this constraint belongs to
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for clients to associate metadata with the constraint.
     * 
     * <p>This can be used to carry additional information about the validation
     * such as error codes, severity levels, or custom metadata that can be
     * processed by the React frontend for enhanced user experience.
     * 
     * <p>Example payload usage:
     * <pre>
     * &#64;ValidCardNumber(payload = {CriticalValidation.class})
     * private String cardNumber;
     * </pre>
     * 
     * @return the payload classes
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Flag to enable or disable Luhn algorithm validation.
     * 
     * <p>When set to false, only format validation (16 digits, numeric) is performed.
     * This is useful for testing scenarios or when integrating with systems that
     * may not strictly enforce Luhn validation.
     * 
     * <p>Default is true to maintain full credit card validation compliance
     * as expected in production credit card processing systems.
     * 
     * @return true if Luhn validation should be performed, false otherwise
     */
    boolean enableLuhnValidation() default true;
    
    /**
     * Flag to allow null or empty values to pass validation.
     * 
     * <p>When set to true, null and empty string values are considered valid
     * and will not trigger validation errors. This is useful for optional
     * fields or when combined with @NotNull/@NotEmpty annotations.
     * 
     * <p>When set to false, null and empty values will fail validation.
     * This matches the COBOL behavior where card number fields are typically
     * required in transaction processing.
     * 
     * @return true if null/empty values should be allowed, false otherwise
     */
    boolean allowNullOrEmpty() default false;
}