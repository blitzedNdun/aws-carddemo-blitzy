package com.carddemo.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Jakarta Bean Validation annotation interface for credit card number validation using Luhn algorithm.
 * 
 * This annotation validates credit card numbers to ensure they:
 * - Are exactly 16 digits in length (matching COBOL CARD-NUM PIC X(16) field)
 * - Contain only numeric characters
 * - Pass the Luhn algorithm checksum verification
 * - Meet industry standards for credit card number format
 * 
 * The validation logic implements exact equivalence to COBOL card number validation
 * routines while providing enhanced checksum verification not present in the original
 * mainframe application.
 * 
 * Usage Examples:
 * <pre>
 * public class CardRequest {
 *     &#64;ValidCardNumber
 *     private String cardNumber;
 *     
 *     &#64;ValidCardNumber(message = "Invalid payment card number")
 *     private String paymentCardNumber;
 *     
 *     &#64;ValidCardNumber(groups = {CreateCard.class, UpdateCard.class})
 *     private String cardNumber;
 * }
 * </pre>
 * 
 * Error Messages:
 * The annotation provides structured error messages compatible with React frontend
 * validation displays. Default message: "Card number must be 16 digits and pass Luhn algorithm validation"
 * 
 * Validation Groups:
 * Supports Jakarta Bean Validation groups for context-specific validation scenarios
 * such as card creation, update operations, or payment processing workflows.
 * 
 * Implementation Notes:
 * - Validates against 16-digit format with exact COBOL precision matching
 * - Implements Luhn algorithm for checksum verification
 * - Provides consistent error messages for UI integration
 * - Supports validation groups for different business contexts
 * - Maintains compatibility with existing COBOL validation patterns
 */
@Documented
@Constraint(validatedBy = CardNumberValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ValidCardNumber.List.class)
public @interface ValidCardNumber {
    
    /**
     * Default validation error message.
     * 
     * This message is structured to be compatible with React frontend error display
     * components and follows the same format as other validation messages in the system.
     * 
     * @return the error message template
     */
    String message() default "Card number must be 16 digits and pass Luhn algorithm validation";
    
    /**
     * Validation groups for context-specific validation scenarios.
     * 
     * Allows the same entity to be validated differently based on the operation context:
     * - Card creation workflows
     * - Card update operations  
     * - Payment processing validation
     * - Administrative card management
     * 
     * @return array of validation group classes
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for carrying additional metadata about the validation constraint.
     * 
     * Can be used to associate additional information with the constraint such as:
     * - Severity levels for different validation contexts
     * - Custom error codes for frontend handling
     * - Audit information for compliance tracking
     * 
     * @return array of payload classes
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Container annotation for repeatable {@code @ValidCardNumber} annotations.
     * 
     * Enables the use of multiple {@code @ValidCardNumber} annotations on the same element
     * with different configurations (e.g., different messages or groups).
     * 
     * This follows the standard Jakarta Bean Validation pattern for repeatable annotations
     * and maintains consistency with other validation annotations in the framework.
     */
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        /**
         * Array of {@code @ValidCardNumber} annotations.
         * 
         * @return array of ValidCardNumber annotations
         */
        ValidCardNumber[] value();
    }
}