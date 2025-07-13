package com.carddemo.common.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Jakarta Bean Validation annotation for credit card number validation.
 * 
 * <p>This annotation validates credit card numbers using the Luhn algorithm
 * for checksum verification, ensuring industry-standard card validation
 * equivalent to COBOL card number validation routines.</p>
 * 
 * <p>The validation supports:</p>
 * <ul>
 *   <li>16-digit format validation with exact COBOL precision matching</li>
 *   <li>Luhn algorithm checksum verification for card number integrity</li>
 *   <li>Structured error messages compatible with React frontend</li>
 *   <li>Validation groups for different validation contexts</li>
 * </ul>
 * 
 * <p>Usage examples:</p>
 * <pre>
 * {@code
 * public class CardRequest {
 *     @ValidCardNumber
 *     private String cardNumber;
 *     
 *     @ValidCardNumber(message = "Custom error message")
 *     private String primaryCardNumber;
 *     
 *     @ValidCardNumber(groups = {CreateGroup.class})
 *     private String newCardNumber;
 * }
 * }
 * </pre>
 * 
 * <p>This annotation is designed to maintain compatibility with the original
 * COBOL card validation logic while providing modern Jakarta Bean Validation
 * framework integration for Spring Boot microservices architecture.</p>
 * 
 * @author Blitzy agent
 * @since 1.0.0
 * @see CardNumberValidator
 * @see jakarta.validation.Constraint
 */
@Documented
@Constraint(validatedBy = CardNumberValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCardNumber {
    
    /**
     * Default error message for card number validation failures.
     * 
     * <p>The message template uses Jakarta Bean Validation message interpolation
     * and can be customized via validation message bundles or directly through
     * the message attribute.</p>
     * 
     * @return the error message template
     */
    String message() default "Invalid credit card number. Card number must be 16 digits and pass Luhn algorithm validation.";
    
    /**
     * Validation groups for conditional validation.
     * 
     * <p>Groups allow different validation contexts to apply different rules.
     * For example, card creation might have different requirements than
     * card updates.</p>
     * 
     * @return the validation groups
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for carrying additional metadata.
     * 
     * <p>The payload can be used to carry additional information about the
     * validation constraint, such as severity levels or custom validation
     * metadata for integration with Spring Boot validation framework.</p>
     * 
     * @return the payload classes
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Whether to allow null values.
     * 
     * <p>When set to true, null values will pass validation. When false,
     * null values will be treated as invalid card numbers. This allows
     * for flexible validation where card numbers might be optional in
     * certain contexts.</p>
     * 
     * @return true if null values are allowed, false otherwise
     */
    boolean allowNull() default false;
    
    /**
     * Whether to ignore whitespace and formatting characters.
     * 
     * <p>When set to true, the validator will strip whitespace, hyphens,
     * and spaces from the card number before validation. This provides
     * user-friendly input handling while maintaining strict validation.</p>
     * 
     * @return true if formatting characters should be ignored, false otherwise
     */
    boolean ignoreFormatting() default true;
    
    /**
     * Supported card number length.
     * 
     * <p>Defines the expected length of the card number after formatting
     * characters are removed. Default is 16 digits to match COBOL
     * PIC X(16) field definition.</p>
     * 
     * @return the expected card number length
     */
    int expectedLength() default 16;
    
    /**
     * Whether to perform strict Luhn algorithm validation.
     * 
     * <p>When set to true, the validator will apply the Luhn algorithm
     * checksum verification. When false, only format validation will
     * be performed. This allows for test scenarios or legacy compatibility.</p>
     * 
     * @return true if Luhn validation should be performed, false otherwise
     */
    boolean enableLuhnCheck() default true;
    
    /**
     * Custom error message for null values.
     * 
     * <p>Specific error message to display when the card number is null
     * and allowNull is false. This provides more specific feedback than
     * the general validation message.</p>
     * 
     * @return the null value error message
     */
    String nullMessage() default "Card number cannot be null or empty.";
    
    /**
     * Custom error message for format validation failures.
     * 
     * <p>Specific error message to display when the card number format
     * is invalid (wrong length, non-numeric characters, etc.) but before
     * Luhn algorithm validation.</p>
     * 
     * @return the format error message
     */
    String formatMessage() default "Card number must be exactly {expectedLength} digits.";
    
    /**
     * Custom error message for Luhn algorithm validation failures.
     * 
     * <p>Specific error message to display when the card number passes
     * format validation but fails the Luhn algorithm checksum verification.</p>
     * 
     * @return the Luhn validation error message
     */
    String luhnMessage() default "Card number checksum validation failed. Please verify the card number.";
    
    /**
     * List of defining annotation types for composite constraints.
     * 
     * <p>This inner annotation allows the ValidCardNumber annotation to be
     * used as part of composite constraints where multiple validation rules
     * are combined into a single annotation.</p>
     */
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        ValidCardNumber[] value();
    }
}