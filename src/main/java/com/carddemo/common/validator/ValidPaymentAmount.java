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
 * Jakarta Bean Validation annotation for bill payment amount validation.
 * 
 * This annotation validates payment amounts according to CardDemo bill payment business rules,
 * ensuring exact BigDecimal precision equivalent to COBOL COMP-3 arithmetic for financial
 * calculations. The validation enforces the bill payment constraint that payments must equal
 * the full account balance, as per the original COBOL program COBIL00C.cbl logic.
 * 
 * <p>Key Business Rules Enforced:</p>
 * <ul>
 *   <li>Payment amount must be greater than zero</li>
 *   <li>Payment amount must not exceed maximum transaction limit (S9(09)V99 = $999,999,999.99)</li>
 *   <li>Payment amount precision must match COBOL COMP-3 format (2 decimal places)</li>
 *   <li>Payment amount must be positive and within valid range for bill payment processing</li>
 * </ul>
 * 
 * <p>Technical Implementation:</p>
 * <ul>
 *   <li>Uses BigDecimal with DECIMAL128 precision for exact financial calculations</li>
 *   <li>Validates against COBOL TRAN-AMT format (PIC S9(09)V99)</li>
 *   <li>Supports configurable validation messages for different payment contexts</li>
 *   <li>Ensures compatibility with PostgreSQL DECIMAL storage and retrieval</li>
 * </ul>
 * 
 * <p>Usage Example:</p>
 * <pre>
 * public class BillPaymentRequest {
 *     &#64;ValidPaymentAmount(
 *         message = "Payment amount must be valid for bill payment processing",
 *         allowZero = false,
 *         maxAmount = "999999999.99"
 *     )
 *     private BigDecimal paymentAmount;
 * }
 * </pre>
 * 
 * @see PaymentAmountValidator
 * @since 1.0
 */
@Documented
@Constraint(validatedBy = PaymentAmountValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPaymentAmount {
    
    /**
     * Default validation message for payment amount validation failures.
     * Can be overridden to provide context-specific error messages.
     */
    String message() default "Payment amount must be valid for bill payment processing";
    
    /**
     * Validation groups for conditional validation scenarios.
     * Allows grouping of validation constraints for different use cases.
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for carrying additional metadata about the validation constraint.
     * Used by validation frameworks for constraint metadata processing.
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Whether to allow zero as a valid payment amount.
     * Default is false to match COBOL bill payment business rules.
     * 
     * @return true if zero amounts are allowed, false otherwise
     */
    boolean allowZero() default false;
    
    /**
     * Minimum payment amount allowed for bill payment processing.
     * Must be specified as a string to preserve exact decimal precision.
     * Default is "0.01" to enforce minimum payment requirement.
     * 
     * @return minimum payment amount as string representation
     */
    String minAmount() default "0.01";
    
    /**
     * Maximum payment amount allowed for bill payment processing.
     * Based on COBOL TRAN-AMT field format (PIC S9(09)V99).
     * Default is "999999999.99" matching COBOL maximum value.
     * 
     * @return maximum payment amount as string representation
     */
    String maxAmount() default "999999999.99";
    
    /**
     * Number of decimal places required for payment amount precision.
     * Must match COBOL COMP-3 decimal format for financial accuracy.
     * Default is 2 decimal places for currency amounts.
     * 
     * @return required number of decimal places
     */
    int decimalPlaces() default 2;
    
    /**
     * Custom validation message for amounts that are too small.
     * Provides specific error messaging for minimum amount violations.
     * 
     * @return error message for amounts below minimum
     */
    String minAmountMessage() default "Payment amount must be at least {minAmount}";
    
    /**
     * Custom validation message for amounts that are too large.
     * Provides specific error messaging for maximum amount violations.
     * 
     * @return error message for amounts above maximum
     */
    String maxAmountMessage() default "Payment amount cannot exceed {maxAmount}";
    
    /**
     * Custom validation message for zero amounts when not allowed.
     * Provides specific error messaging for zero amount violations.
     * 
     * @return error message for zero amounts
     */
    String zeroAmountMessage() default "Payment amount cannot be zero";
    
    /**
     * Custom validation message for negative amounts.
     * Provides specific error messaging for negative amount violations.
     * 
     * @return error message for negative amounts
     */
    String negativeAmountMessage() default "Payment amount cannot be negative";
    
    /**
     * Custom validation message for incorrect decimal precision.
     * Provides specific error messaging for decimal precision violations.
     * 
     * @return error message for incorrect decimal places
     */
    String decimalPrecisionMessage() default "Payment amount must have exactly {decimalPlaces} decimal places";
    
    /**
     * Custom validation message for null amounts.
     * Provides specific error messaging for null amount violations.
     * 
     * @return error message for null amounts
     */
    String nullAmountMessage() default "Payment amount is required";
    
    /**
     * Whether to enforce strict decimal precision validation.
     * When true, validates that the amount has exactly the specified number of decimal places.
     * When false, allows amounts with fewer decimal places (e.g., 10.5 instead of 10.50).
     * Default is true to match COBOL COMP-3 precision requirements.
     * 
     * @return true if strict decimal precision is required, false otherwise
     */
    boolean strictDecimalPrecision() default true;
    
    /**
     * Whether to validate against COBOL COMP-3 format constraints.
     * When true, enforces additional validation rules specific to COBOL numeric format.
     * Default is true to maintain compatibility with legacy COBOL business rules.
     * 
     * @return true if COBOL format validation is required, false otherwise
     */
    boolean enforceCobolFormat() default true;
}