/*
 * CardDemo Credit Card Management System
 * 
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carddemo.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Jakarta Bean Validation annotation for CVV code validation.
 * 
 * <p>Validates that CVV codes meet the required 3-digit numeric format
 * as specified in card security standards and the original COBOL 
 * implementation (PIC 9(03) format).
 * 
 * <p>This annotation ensures CVV codes:
 * <ul>
 *   <li>Are exactly 3 digits in length</li>
 *   <li>Contain only numeric characters (0-9)</li>
 *   <li>Handle null values appropriately for optional CVV updates</li>
 *   <li>Provide clear error messages for format violations</li>
 *   <li>Support validation groups for different card operation scenarios</li>
 * </ul>
 * 
 * <p><strong>COBOL Equivalence:</strong>
 * This validator preserves the exact validation behavior from the original
 * COBOL CardDemo application where CVV codes are defined as:
 * <pre>
 * CARD-CVV-CD-X                       PIC X(03).
 * CARD-CVV-CD-N REDEFINES CARD-CVV-CD-X PIC 9(03).
 * </pre>
 * 
 * <p><strong>Usage Examples:</strong>
 * <pre>
 * // Basic CVV validation
 * {@literal @}ValidCvv
 * private String cvvCode;
 * 
 * // CVV validation with custom message
 * {@literal @}ValidCvv(message = "CVV must be exactly 3 digits")
 * private String securityCode;
 * 
 * // CVV validation with validation groups
 * {@literal @}ValidCvv(groups = {CardCreation.class, CardUpdate.class})
 * private String cvv;
 * </pre>
 * 
 * <p><strong>Validation Behavior:</strong>
 * <ul>
 *   <li><code>null</code> values: Considered valid (allows optional CVV updates)</li>
 *   <li>Empty strings: Invalid - CVV cannot be empty if provided</li>
 *   <li>Length validation: Must be exactly 3 characters</li>
 *   <li>Format validation: Must contain only numeric digits (0-9)</li>
 *   <li>Leading zeros: Accepted and preserved (e.g., "007" is valid)</li>
 * </ul>
 * 
 * <p><strong>Security Considerations:</strong>
 * This validator focuses on format validation only. CVV codes should be
 * handled securely in accordance with PCI DSS requirements, including:
 * <ul>
 *   <li>Encryption at rest and in transit</li>
 *   <li>Limited retention policies</li>
 *   <li>Restricted access controls</li>
 *   <li>Secure logging practices (CVV should not appear in logs)</li>
 * </ul>
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 * 
 * @see jakarta.validation.Constraint
 * @see ValidCvvValidator
 */
@Documented
@Constraint(validatedBy = ValidCvvValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCvv {
    
    /**
     * The error message to be used when validation fails.
     * 
     * <p>This message supports Jakarta Bean Validation interpolation and can include:
     * <ul>
     *   <li>Message parameter substitution using {parameter}</li>
     *   <li>Internationalization through ResourceBundle</li>
     *   <li>Custom validation context information</li>
     * </ul>
     * 
     * <p>Default message provides clear guidance about CVV format requirements
     * while maintaining security by not exposing the actual invalid value.
     * 
     * @return the error message template
     */
    String message() default "CVV code must be exactly 3 numeric digits";
    
    /**
     * Validation groups to control when this constraint is evaluated.
     * 
     * <p>Groups allow fine-grained control over validation execution:
     * <ul>
     *   <li><strong>CardCreation</strong>: When creating new cards</li>
     *   <li><strong>CardUpdate</strong>: When updating existing card information</li>
     *   <li><strong>PaymentProcessing</strong>: During payment transactions</li>
     *   <li><strong>Default</strong>: Standard validation group (default behavior)</li>
     * </ul>
     * 
     * <p>Example usage with groups:
     * <pre>
     * // Validate CVV only during card creation and payment processing
     * {@literal @}ValidCvv(groups = {CardCreation.class, PaymentProcessing.class})
     * private String cvvCode;
     * </pre>
     * 
     * @return the validation groups
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for carrying additional metadata about the validation constraint.
     * 
     * <p>The payload mechanism allows association of additional metadata
     * with validation constraints. This can be used for:
     * <ul>
     *   <li>Severity levels (warning vs. error)</li>
     *   <li>Custom validation context information</li>
     *   <li>Integration with external validation frameworks</li>
     *   <li>Audit trail requirements</li>
     * </ul>
     * 
     * <p>Common payload implementations might include:
     * <pre>
     * // Custom severity payload
     * {@literal @}ValidCvv(payload = {Severity.High.class})
     * private String cvvCode;
     * 
     * // Audit requirement payload
     * {@literal @}ValidCvv(payload = {AuditRequired.class})
     * private String securityCode;
     * </pre>
     * 
     * @return the payload classes
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Custom error message for null value handling.
     * 
     * <p>While null values are generally considered valid for optional CVV updates,
     * this attribute allows customization of null handling behavior in specific
     * validation contexts where CVV might be required.
     * 
     * <p>When this message is specified and the CVV is null in a context where
     * it should be required, this message will be used instead of the default.
     * 
     * @return the error message for null values
     */
    String nullMessage() default "";
    
    /**
     * Flag to indicate whether null values should be considered invalid.
     * 
     * <p>By default, null values are considered valid to support optional
     * CVV updates. However, in certain validation contexts (such as payment
     * processing), CVV might be required.
     * 
     * <p>Usage examples:
     * <pre>
     * // CVV required for payment processing
     * {@literal @}ValidCvv(requireNonNull = true, groups = {PaymentProcessing.class})
     * private String cvvCode;
     * 
     * // CVV optional for card updates (default behavior)
     * {@literal @}ValidCvv(groups = {CardUpdate.class})
     * private String cvvCode;
     * </pre>
     * 
     * @return true if null values should be considered invalid, false otherwise
     */
    boolean requireNonNull() default false;
}