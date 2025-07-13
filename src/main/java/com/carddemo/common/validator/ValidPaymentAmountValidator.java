/*
 * CardDemo Application
 * 
 * ValidPaymentAmountValidator - Implementation of ValidPaymentAmount constraint validation
 * 
 * This validator implements the business logic for bill payment amount validation,
 * ensuring BigDecimal precision and COBOL COMP-3 arithmetic compatibility.
 * 
 * Based on COBIL00C.cbl payment validation logic:
 * - Positive amount validation (ACCT-CURR-BAL > ZEROS check)
 * - TRAN-AMT precision enforcement (PIC S9(09)V99)
 * - Financial accuracy requirements
 * 
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
 * Validator implementation for {@link ValidPaymentAmount} annotation.
 * 
 * This validator performs comprehensive validation of payment amounts ensuring:
 * - Financial precision equivalent to COBOL COMP-3 arithmetic
 * - Business rule compliance for bill payment processing
 * - Proper error messaging for different validation failures
 * 
 * <p>Validation Logic:</p>
 * <ol>
 *   <li>Type validation - ensures BigDecimal type for financial precision</li>
 *   <li>Null validation - based on allowNull configuration</li>
 *   <li>Range validation - amount within min/max bounds</li>
 *   <li>Precision validation - total digits not exceeding COBOL limits</li>
 *   <li>Scale validation - decimal places matching currency requirements</li>
 * </ol>
 * 
 * <p>COBOL Compatibility:</p>
 * Maintains exact compatibility with original COBOL validation logic while
 * leveraging Java BigDecimal for precise financial calculations.
 * 
 * @see ValidPaymentAmount
 * @see ConstraintValidator
 * @since 1.0
 */
public class ValidPaymentAmountValidator implements ConstraintValidator<ValidPaymentAmount, Object> {

    /**
     * Math context for COBOL-equivalent precision calculations.
     * Uses DECIMAL128 precision with HALF_UP rounding to match COBOL COMP-3 behavior.
     */
    private static final MathContext COBOL_MATH_CONTEXT = new MathContext(31, RoundingMode.HALF_UP);

    // Constraint annotation parameters
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private int scale;
    private int precision;
    private boolean allowNull;
    
    // Custom error messages
    private String belowMinimumMessage;
    private String aboveMaximumMessage;
    private String precisionMessage;
    private String scaleMessage;
    private String nullMessage;
    private String invalidTypeMessage;

    /**
     * Initializes the validator with constraint annotation parameters.
     * 
     * @param constraintAnnotation the ValidPaymentAmount annotation instance
     */
    @Override
    public void initialize(ValidPaymentAmount constraintAnnotation) {
        // Parse min/max amounts using COBOL-compatible math context
        this.minAmount = new BigDecimal(constraintAnnotation.minAmount(), COBOL_MATH_CONTEXT);
        this.maxAmount = new BigDecimal(constraintAnnotation.maxAmount(), COBOL_MATH_CONTEXT);
        this.scale = constraintAnnotation.scale();
        this.precision = constraintAnnotation.precision();
        this.allowNull = constraintAnnotation.allowNull();
        
        // Store custom error messages
        this.belowMinimumMessage = constraintAnnotation.belowMinimumMessage();
        this.aboveMaximumMessage = constraintAnnotation.aboveMaximumMessage();
        this.precisionMessage = constraintAnnotation.precisionMessage();
        this.scaleMessage = constraintAnnotation.scaleMessage();
        this.nullMessage = constraintAnnotation.nullMessage();
        this.invalidTypeMessage = constraintAnnotation.invalidTypeMessage();
    }

    /**
     * Validates the payment amount according to business rules and precision requirements.
     * 
     * @param value the value to validate
     * @param context the constraint validator context
     * @return true if the value is valid, false otherwise
     */
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // Disable default constraint violation to provide custom messages
        context.disableDefaultConstraintViolation();

        // Handle null values
        if (value == null) {
            if (allowNull) {
                return true;
            } else {
                addConstraintViolation(context, nullMessage);
                return false;
            }
        }

        // Validate that value is BigDecimal for financial precision
        if (!(value instanceof BigDecimal)) {
            addConstraintViolation(context, invalidTypeMessage);
            return false;
        }

        BigDecimal amount = (BigDecimal) value;

        // Validate minimum amount (must be positive for bill payments)
        if (amount.compareTo(minAmount) < 0) {
            String message = belowMinimumMessage.replace("{minAmount}", minAmount.toPlainString());
            addConstraintViolation(context, message);
            return false;
        }

        // Validate maximum amount (COBOL field capacity)
        if (amount.compareTo(maxAmount) > 0) {
            String message = aboveMaximumMessage.replace("{maxAmount}", maxAmount.toPlainString());
            addConstraintViolation(context, message);
            return false;
        }

        // Validate scale (decimal places) - must match COBOL V99 specification
        if (amount.scale() != scale) {
            String message = scaleMessage.replace("{scale}", String.valueOf(scale));
            addConstraintViolation(context, message);
            return false;
        }

        // Validate precision (total digits) - must not exceed COBOL S9(09)V99 capacity
        if (amount.precision() > precision) {
            String message = precisionMessage.replace("{precision}", String.valueOf(precision));
            addConstraintViolation(context, message);
            return false;
        }

        // Additional business rule validation specific to bill payments
        if (!isValidForBillPayment(amount, context)) {
            return false;
        }

        return true;
    }

    /**
     * Performs additional business rule validation specific to bill payment processing.
     * 
     * @param amount the payment amount to validate
     * @param context the constraint validator context
     * @return true if amount is valid for bill payment, false otherwise
     */
    private boolean isValidForBillPayment(BigDecimal amount, ConstraintValidatorContext context) {
        // Ensure amount is not zero (based on COBIL00C.cbl line 198 logic)
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            addConstraintViolation(context, "Payment amount cannot be zero for bill payment processing");
            return false;
        }

        // Validate that amount has valid monetary precision (no fractional cents)
        // This ensures compatibility with COBOL currency handling
        BigDecimal scaledAmount = amount.setScale(scale, RoundingMode.HALF_UP);
        if (amount.compareTo(scaledAmount) != 0) {
            addConstraintViolation(context, "Payment amount contains invalid fractional currency units");
            return false;
        }

        // Validate that amount can be represented in COBOL COMP-3 format
        // This ensures no loss of precision during data conversion
        try {
            // Test conversion to COBOL-equivalent format and back
            BigDecimal cobolEquivalent = new BigDecimal(amount.toPlainString(), COBOL_MATH_CONTEXT);
            if (cobolEquivalent.compareTo(amount) != 0) {
                addConstraintViolation(context, "Payment amount exceeds COBOL COMP-3 precision limits");
                return false;
            }
        } catch (NumberFormatException e) {
            addConstraintViolation(context, "Payment amount format is incompatible with financial processing");
            return false;
        }

        return true;
    }

    /**
     * Adds a constraint violation with the specified message.
     * 
     * @param context the constraint validator context
     * @param message the error message to add
     */
    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}