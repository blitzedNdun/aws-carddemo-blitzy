/*
 * CardDemo Application
 * 
 * ValidPaymentAmount - Jakarta Bean Validation annotation for bill payment amount validation
 * 
 * This annotation provides comprehensive validation for bill payment amounts ensuring:
 * - BigDecimal precision validation equivalent to COBOL COMP-3 arithmetic
 * - Business rule enforcement for payment amount ranges and constraints
 * - Configurable validation messages for different payment contexts
 * 
 * Converted from COBOL bill payment validation logic in COBIL00C.cbl
 * Original COBOL validation:
 * - ACCT-CURR-BAL <= ZEROS validation (lines 198-206)
 * - TRAN-AMT PIC S9(09)V99 precision requirements (CVTRA05Y line 10)
 * - Payment amount business rule enforcement
 * 
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
 * Jakarta Bean Validation annotation for validating bill payment amounts.
 * 
 * This annotation ensures that payment amounts meet bill payment specific requirements
 * and maintain financial precision standards equivalent to COBOL COMP-3 arithmetic.
 * 
 * <p>Validation Rules Applied:</p>
 * <ul>
 *   <li>Amount must be positive (greater than zero)</li>
 *   <li>Amount precision must not exceed 9 digits before decimal and 2 digits after</li>
 *   <li>Amount must use BigDecimal for exact financial precision</li>
 *   <li>Null values are considered invalid for payment processing</li>
 *   <li>Scale validation ensures COBOL COMP-3 equivalent precision</li>
 * </ul>
 * 
 * <p>Business Context:</p>
 * Based on COBIL00C.cbl validation logic where payment amounts must be positive
 * and within the precision constraints of TRAN-AMT PIC S9(09)V99 field definition.
 * 
 * <p>Usage Example:</p>
 * <pre>
 * public class BillPaymentRequest {
 *     &#64;ValidPaymentAmount(message = "Payment amount must be valid for bill payment")
 *     private BigDecimal paymentAmount;
 * }
 * </pre>
 * 
 * @see jakarta.validation.Constraint
 * @see java.math.BigDecimal
 * @since 1.0
 */
@Documented
@Constraint(validatedBy = ValidPaymentAmountValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPaymentAmount {

    /**
     * Default validation message.
     * Can be overridden to provide context-specific messages for different payment scenarios.
     * 
     * @return the validation error message
     */
    String message() default "Payment amount is invalid for bill payment processing";

    /**
     * Validation groups for conditional validation.
     * Allows grouping of validation constraints for different scenarios.
     * 
     * @return the groups the constraint belongs to
     */
    Class<?>[] groups() default {};

    /**
     * Payload for carrying additional metadata.
     * Can be used to associate additional information with the constraint.
     * 
     * @return the payload associated with the constraint
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * Minimum allowed payment amount.
     * Defaults to "0.01" to ensure positive amounts greater than zero.
     * Configurable to support different minimum payment requirements.
     * 
     * @return the minimum payment amount as a string representation
     */
    String minAmount() default "0.01";

    /**
     * Maximum allowed payment amount.
     * Defaults to "999999999.99" matching COBOL PIC S9(09)V99 field capacity.
     * Represents the maximum value that can be stored in TRAN-AMT field.
     * 
     * @return the maximum payment amount as a string representation
     */
    String maxAmount() default "999999999.99";

    /**
     * Required decimal scale (number of digits after decimal point).
     * Defaults to 2 to match COBOL V99 specification for currency amounts.
     * Ensures financial precision consistency with original COBOL implementation.
     * 
     * @return the required number of decimal places
     */
    int scale() default 2;

    /**
     * Maximum precision (total number of significant digits).
     * Defaults to 11 to match COBOL S9(09)V99 specification (9 + 2 = 11).
     * Ensures compatibility with original COBOL field definitions.
     * 
     * @return the maximum number of significant digits allowed
     */
    int precision() default 11;

    /**
     * Whether to allow null values.
     * Defaults to false as payment amounts are required for bill payment processing.
     * Set to true if payment amount is optional in specific contexts.
     * 
     * @return true if null values are allowed, false otherwise
     */
    boolean allowNull() default false;

    /**
     * Custom validation message for amounts that are too small.
     * Provides specific feedback when payment amount is below minimum threshold.
     * 
     * @return the validation message for amounts below minimum
     */
    String belowMinimumMessage() default "Payment amount must be at least {minAmount}";

    /**
     * Custom validation message for amounts that are too large.
     * Provides specific feedback when payment amount exceeds maximum threshold.
     * 
     * @return the validation message for amounts above maximum
     */
    String aboveMaximumMessage() default "Payment amount cannot exceed {maxAmount}";

    /**
     * Custom validation message for precision violations.
     * Provides specific feedback when payment amount has too many digits.
     * 
     * @return the validation message for precision violations
     */
    String precisionMessage() default "Payment amount precision cannot exceed {precision} total digits";

    /**
     * Custom validation message for scale violations.
     * Provides specific feedback when payment amount has incorrect decimal places.
     * 
     * @return the validation message for scale violations
     */
    String scaleMessage() default "Payment amount must have exactly {scale} decimal places";

    /**
     * Custom validation message for null values.
     * Provides specific feedback when payment amount is null but required.
     * 
     * @return the validation message for null values
     */
    String nullMessage() default "Payment amount is required for bill payment processing";

    /**
     * Custom validation message for non-BigDecimal types.
     * Provides specific feedback when field type is not BigDecimal.
     * 
     * @return the validation message for invalid types
     */
    String invalidTypeMessage() default "Payment amount must be a BigDecimal for financial precision";

    /**
     * Defines several {@code @ValidPaymentAmount} annotations on the same element.
     * Allows applying multiple payment amount validations with different parameters.
     * 
     * @see ValidPaymentAmount
     */
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        ValidPaymentAmount[] value();
    }
}