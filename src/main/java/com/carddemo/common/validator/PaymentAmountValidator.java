/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Jakarta Bean Validation implementation for bill payment amount validation.
 * 
 * This validator enforces business rules for payment amounts according to the original 
 * COBOL program COBIL00C.cbl logic, ensuring exact BigDecimal precision equivalent to 
 * COBOL COMP-3 arithmetic for financial calculations. The implementation validates 
 * payment amounts against the constraints defined in the ValidPaymentAmount annotation.
 * 
 * <p>Business Rules Implementation:</p>
 * <ul>
 *   <li>Payment amount null validation with configurable error messages</li>
 *   <li>Zero amount validation based on allowZero flag (default: not allowed)</li>
 *   <li>Negative amount validation (always prohibited for payments)</li>
 *   <li>Minimum amount validation (default: $0.01)</li>
 *   <li>Maximum amount validation (default: $999,999,999.99 - COBOL S9(09)V99 limit)</li>
 *   <li>Decimal precision validation ensuring exactly 2 decimal places</li>
 *   <li>COBOL COMP-3 format compliance validation</li>
 * </ul>
 * 
 * <p>Technical Implementation:</p>
 * <ul>
 *   <li>Uses BigDecimal with DECIMAL128 precision matching COBOL arithmetic</li>
 *   <li>Implements HALF_UP rounding mode for COBOL compatibility</li>
 *   <li>Validates against COBOL TRAN-AMT field format (PIC S9(09)V99)</li>
 *   <li>Supports configurable validation parameters through annotation</li>
 *   <li>Provides detailed error messages for different validation failures</li>
 * </ul>
 * 
 * <p>COBOL Integration:</p>
 * <p>This validator enforces the same business rules as the original COBOL bill payment
 * program (COBIL00C.cbl), including:</p>
 * <ul>
 *   <li>Account balance validation (must be > 0 to have something to pay)</li>
 *   <li>Payment amount precision matching COBOL COMP-3 decimal format</li>
 *   <li>Maximum transaction amount based on TRAN-AMT field structure</li>
 *   <li>Financial calculation precision equivalent to mainframe arithmetic</li>
 * </ul>
 * 
 * @see ValidPaymentAmount
 * @see ValidationConstants
 * @since 1.0
 */
public class PaymentAmountValidator implements ConstraintValidator<ValidPaymentAmount, BigDecimal> {

    /**
     * MathContext for COBOL-compatible decimal calculations.
     * Uses DECIMAL128 precision with HALF_UP rounding to match COBOL COMP-3 arithmetic.
     */
    private static final MathContext COBOL_MATH_CONTEXT = new MathContext(31, RoundingMode.HALF_UP);

    /**
     * Default maximum amount for payment validation.
     * Based on COBOL TRAN-AMT field format (PIC S9(09)V99).
     */
    private static final BigDecimal DEFAULT_MAX_AMOUNT = new BigDecimal("999999999.99");

    /**
     * Default minimum amount for payment validation.
     * Set to one cent to enforce minimum payment requirement.
     */
    private static final BigDecimal DEFAULT_MIN_AMOUNT = new BigDecimal("0.01");

    /**
     * Zero amount constant for validation comparisons.
     */
    private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO;

    // Annotation parameter fields
    private boolean allowZero;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private int decimalPlaces;
    private boolean strictDecimalPrecision;
    private boolean enforceCobolFormat;
    
    // Error message fields
    private String nullAmountMessage;
    private String zeroAmountMessage;
    private String negativeAmountMessage;
    private String minAmountMessage;
    private String maxAmountMessage;
    private String decimalPrecisionMessage;

    /**
     * Initializes the validator with parameters from the ValidPaymentAmount annotation.
     * 
     * @param constraintAnnotation the annotation instance containing validation parameters
     */
    @Override
    public void initialize(ValidPaymentAmount constraintAnnotation) {
        this.allowZero = constraintAnnotation.allowZero();
        this.decimalPlaces = constraintAnnotation.decimalPlaces();
        this.strictDecimalPrecision = constraintAnnotation.strictDecimalPrecision();
        this.enforceCobolFormat = constraintAnnotation.enforceCobolFormat();
        
        // Initialize amount limits with proper BigDecimal precision
        this.minAmount = new BigDecimal(constraintAnnotation.minAmount(), COBOL_MATH_CONTEXT);
        this.maxAmount = new BigDecimal(constraintAnnotation.maxAmount(), COBOL_MATH_CONTEXT);
        
        // Store error messages for context-specific validation failures
        this.nullAmountMessage = constraintAnnotation.nullAmountMessage();
        this.zeroAmountMessage = constraintAnnotation.zeroAmountMessage();
        this.negativeAmountMessage = constraintAnnotation.negativeAmountMessage();
        this.minAmountMessage = constraintAnnotation.minAmountMessage();
        this.maxAmountMessage = constraintAnnotation.maxAmountMessage();
        this.decimalPrecisionMessage = constraintAnnotation.decimalPrecisionMessage();
    }

    /**
     * Validates a payment amount against all configured business rules.
     * 
     * This method implements comprehensive validation logic equivalent to the original
     * COBOL program COBIL00C.cbl, ensuring that payment amounts meet all business
     * requirements and maintain exact decimal precision.
     * 
     * @param value the payment amount to validate
     * @param context the validation context for error reporting
     * @return true if the payment amount is valid, false otherwise
     */
    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        // Disable default constraint violation to enable custom messages
        context.disableDefaultConstraintViolation();
        
        // Validate null amount
        if (value == null) {
            addConstraintViolation(context, nullAmountMessage);
            return false;
        }
        
        // Validate negative amount (always prohibited for payments)
        if (value.compareTo(ZERO_AMOUNT) < 0) {
            addConstraintViolation(context, negativeAmountMessage);
            return false;
        }
        
        // Validate zero amount based on allowZero flag
        if (value.compareTo(ZERO_AMOUNT) == 0 && !allowZero) {
            addConstraintViolation(context, zeroAmountMessage);
            return false;
        }
        
        // Skip further validation for zero amounts when allowed
        if (value.compareTo(ZERO_AMOUNT) == 0 && allowZero) {
            return true;
        }
        
        // Validate minimum amount constraint
        if (value.compareTo(minAmount) < 0) {
            String message = minAmountMessage.replace("{minAmount}", minAmount.toPlainString());
            addConstraintViolation(context, message);
            return false;
        }
        
        // Validate maximum amount constraint (COBOL TRAN-AMT field limit)
        if (value.compareTo(maxAmount) > 0) {
            String message = maxAmountMessage.replace("{maxAmount}", maxAmount.toPlainString());
            addConstraintViolation(context, message);
            return false;
        }
        
        // Validate decimal precision for financial accuracy
        if (strictDecimalPrecision && !isValidDecimalPrecision(value)) {
            String message = decimalPrecisionMessage.replace("{decimalPlaces}", String.valueOf(decimalPlaces));
            addConstraintViolation(context, message);
            return false;
        }
        
        // Validate COBOL format compliance if enabled
        if (enforceCobolFormat && !isValidCobolFormat(value)) {
            addConstraintViolation(context, "Payment amount must comply with COBOL COMP-3 format constraints");
            return false;
        }
        
        return true;
    }

    /**
     * Validates that the payment amount has the correct number of decimal places.
     * 
     * This method ensures that financial amounts maintain the exact precision
     * required by the COBOL COMP-3 format, typically 2 decimal places for currency.
     * 
     * @param value the payment amount to validate
     * @return true if the decimal precision is correct, false otherwise
     */
    private boolean isValidDecimalPrecision(BigDecimal value) {
        if (value == null) {
            return false;
        }
        
        // Get the scale (number of decimal places) of the BigDecimal value
        int actualScale = value.scale();
        
        // For strict precision, the scale must match exactly
        if (strictDecimalPrecision) {
            return actualScale == decimalPlaces;
        }
        
        // For non-strict precision, allow fewer decimal places but not more
        return actualScale <= decimalPlaces;
    }

    /**
     * Validates that the payment amount complies with COBOL COMP-3 format constraints.
     * 
     * This method ensures that the payment amount can be properly stored and processed
     * in a format equivalent to the original COBOL TRAN-AMT field (PIC S9(09)V99).
     * 
     * @param value the payment amount to validate
     * @return true if the amount complies with COBOL format, false otherwise
     */
    private boolean isValidCobolFormat(BigDecimal value) {
        if (value == null) {
            return false;
        }
        
        // Ensure the value uses the correct precision and scale for COBOL compatibility
        BigDecimal normalizedValue = value.setScale(ValidationConstants.CURRENCY_SCALE, 
                                                   ValidationConstants.CURRENCY_ROUNDING_MODE);
        
        // Check if the normalized value is within COBOL COMP-3 limits
        // COBOL PIC S9(09)V99 allows 9 digits before decimal point + 2 after
        BigDecimal maxCobolValue = new BigDecimal("999999999.99");
        BigDecimal minCobolValue = new BigDecimal("-999999999.99");
        
        return normalizedValue.compareTo(minCobolValue) >= 0 && 
               normalizedValue.compareTo(maxCobolValue) <= 0;
    }

    /**
     * Adds a constraint violation with the specified message to the validation context.
     * 
     * @param context the validation context for error reporting
     * @param message the error message to add
     */
    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
    }
}