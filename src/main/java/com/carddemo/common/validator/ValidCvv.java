/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
 * Jakarta Bean Validation annotation for CVV (Card Verification Value) code validation.
 * 
 * <p>This annotation validates that CVV codes meet the required 3-digit numeric format
 * for credit card security verification, supporting various card types and validation contexts
 * while maintaining compatibility with legacy COBOL CVV processing patterns.</p>
 * 
 * <p>Validation Rules:</p>
 * <ul>
 *   <li>CVV must be exactly 3 digits</li>
 *   <li>CVV must contain only numeric characters (0-9)</li>
 *   <li>CVV cannot be null for required validation contexts</li>
 *   <li>CVV can be null for optional update scenarios when explicitly allowed</li>
 * </ul>
 * 
 * <p>Usage Examples:</p>
 * <pre>{@code
 * public class CardUpdateRequest {
 *     @ValidCvv
 *     private String cvvCode;
 *     
 *     @ValidCvv(message = "CVV is required for new card creation")
 *     private String newCardCvv;
 *     
 *     @ValidCvv(groups = CardCreation.class, allowNull = false)
 *     private String requiredCvv;
 *     
 *     @ValidCvv(groups = CardUpdate.class, allowNull = true)
 *     private String optionalCvv;
 * }
 * }</pre>
 * 
 * <p>Integration with Legacy COBOL Processing:</p>
 * <p>This validation annotation ensures consistency with the legacy COBOL CVV processing
 * patterns found in COCRDUPC.cbl, where CVV codes are handled as 3-digit numeric fields
 * using PIC 9(03) format. The validation maintains exact format compatibility while
 * providing modern Jakarta Bean Validation capabilities.</p>
 * 
 * @since CardDemo v2.0
 * @see CvvValidator
 */
@Documented
@Constraint(validatedBy = CvvValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCvv {

    /**
     * Default validation message for CVV format violations.
     * 
     * @return the error message template
     */
    String message() default "CVV must be a 3-digit numeric code";

    /**
     * Validation groups for controlling when CVV validation is applied.
     * 
     * <p>Common validation groups:</p>
     * <ul>
     *   <li>CardCreation.class - Strict validation for new card creation</li>
     *   <li>CardUpdate.class - Flexible validation for card updates</li>
     *   <li>CardAuthorization.class - Runtime validation for transaction processing</li>
     * </ul>
     * 
     * @return array of validation groups
     */
    Class<?>[] groups() default {};

    /**
     * Payload for carrying additional metadata about the validation constraint.
     * 
     * <p>Can be used to carry severity information, custom error codes,
     * or other metadata for advanced validation processing.</p>
     * 
     * @return array of payload classes
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * Specifies whether null values should be considered valid.
     * 
     * <p>When set to true, null values pass validation, allowing for optional
     * CVV updates in card modification scenarios. When false, null values
     * are treated as validation failures.</p>
     * 
     * <p>Default is true to support optional CVV updates as indicated in
     * the COBOL source code patterns for card modification scenarios.</p>
     * 
     * @return true if null values are allowed, false otherwise
     */
    boolean allowNull() default true;

    /**
     * Custom error message for null value violations when allowNull is false.
     * 
     * @return the error message for null values
     */
    String nullMessage() default "CVV code is required for this operation";

    /**
     * Custom error message for format violations (non-numeric or incorrect length).
     * 
     * @return the error message for format violations
     */
    String formatMessage() default "CVV must contain exactly 3 numeric digits (0-9)";

    /**
     * Validation group marker interface for card creation scenarios.
     * Used when CVV validation must be strict and non-null.
     */
    interface CardCreation {}

    /**
     * Validation group marker interface for card update scenarios.
     * Used when CVV validation can be more flexible with null values allowed.
     */
    interface CardUpdate {}

    /**
     * Validation group marker interface for card authorization scenarios.
     * Used during transaction processing when CVV must be present and valid.
     */
    interface CardAuthorization {}

    /**
     * Validation group marker interface for administrative operations.
     * Used when CVV validation may have different rules for admin users.
     */
    interface AdminOperation {}
}