/*
 * CardDemo Application
 * 
 * PaymentAmountValidator - Jakarta Bean Validation implementation for bill payment amount validation
 * 
 * This validator provides comprehensive validation for bill payment amounts ensuring:
 * - BigDecimal precision validation equivalent to COBOL COMP-3 arithmetic
 * - Business rule enforcement matching COBIL00C.cbl validation logic
 * - Configurable validation messages for different payment contexts
 * - Exact financial precision with scale and precision constraints
 * 
 * Converted from COBOL bill payment validation logic in COBIL00C.cbl
 * Original COBOL validation:
 * - ACCT-CURR-BAL <= ZEROS validation (lines 198-206)
 * - TRAN-AMT PIC S9(09)V99 precision requirements (CVTRA05Y line 10)
 * - Payment amount business rule enforcement for bill payment processing
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
 * Jakarta Bean Validation implementation for ValidPaymentAmount constraint.
 * 
 * This validator ensures that payment amounts meet bill payment specific requirements
 * and maintain financial precision standards equivalent to COBOL COMP-3 arithmetic.
 * 
 * <p>Validation Logic Implementation:</p>
 * <ul>
 *   <li>Validates BigDecimal type requirement for financial precision</li>
 *   <li>Enforces null value handling based on allowNull parameter</li>
 *   <li>Validates minimum and maximum amount constraints</li>
 *   <li>Ensures precision (total digits) meets COBOL S9(09)V99 specification</li>
 *   <li>Validates scale (decimal places) for currency formatting</li>
 *   <li>Provides context-specific error messages with parameter substitution</li>
 * </ul>
 * 
 * <p>Business Rules Based on COBIL00C.cbl:</p>
 * <ul>
 *   <li>Payment amounts must be positive (greater than zero)</li>
 *   <li>Maximum precision of 9 digits before decimal point</li>
 *   <li>Exactly 2 decimal places for currency representation</li>
 *   <li>Range validation equivalent to COBOL COMP-3 field capacity</li>
 * </ul>
 * 
 * <p>Technical Implementation:</p>
 * Based on TRAN-AMT field definition from CVTRA05Y.cpy (PIC S9(09)V99) and
 * validation logic from COBIL00C.cbl ensuring exact functional equivalence
 * with original COBOL bill payment processing.
 * 
 * @see ValidPaymentAmount
 * @see ValidationConstants
 * @since 1.0
 */
public class PaymentAmountValidator implements ConstraintValidator<ValidPaymentAmount, BigDecimal> {

    /**
     * MathContext for COBOL-equivalent decimal arithmetic.
     * Uses DECIMAL128 precision with HALF_UP rounding to match COBOL COMP-3 behavior.
     */
    private static final MathContext COBOL_MATH_CONTEXT = new MathContext(34, RoundingMode.HALF_UP);

    // Validation parameters from annotation
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private int requiredScale;
    private int maxPrecision;
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
     * This method extracts validation parameters from the ValidPaymentAmount annotation
     * and prepares BigDecimal constraints for efficient validation processing.
     * 
     * @param constraintAnnotation the ValidPaymentAmount annotation instance
     */
    @Override
    public void initialize(ValidPaymentAmount constraintAnnotation) {
        // Extract amount constraints with COBOL-equivalent precision
        this.minAmount = new BigDecimal(constraintAnnotation.minAmount(), COBOL_MATH_CONTEXT);
        this.maxAmount = new BigDecimal(constraintAnnotation.maxAmount(), COBOL_MATH_CONTEXT);
        
        // Extract precision and scale constraints
        this.requiredScale = constraintAnnotation.scale();
        this.maxPrecision = constraintAnnotation.precision();
        this.allowNull = constraintAnnotation.allowNull();
        
        // Extract custom error messages for context-specific validation feedback
        this.belowMinimumMessage = constraintAnnotation.belowMinimumMessage();
        this.aboveMaximumMessage = constraintAnnotation.aboveMaximumMessage();
        this.precisionMessage = constraintAnnotation.precisionMessage();
        this.scaleMessage = constraintAnnotation.scaleMessage();
        this.nullMessage = constraintAnnotation.nullMessage();
        this.invalidTypeMessage = constraintAnnotation.invalidTypeMessage();
    }

    /**
     * Validates the payment amount against bill payment business rules.
     * 
     * This method performs comprehensive validation of payment amounts ensuring
     * compliance with COBOL COMP-3 precision requirements and bill payment
     * business rules extracted from COBIL00C.cbl.
     * 
     * <p>Validation Steps:</p>
     * <ol>
     *   <li>Null value validation based on allowNull parameter</li>
     *   <li>Minimum amount validation (must be greater than zero)</li>
     *   <li>Maximum amount validation (COBOL field capacity)</li>
     *   <li>Scale validation (exactly 2 decimal places)</li>
     *   <li>Precision validation (maximum 11 total digits)</li>
     * </ol>
     * 
     * @param value the payment amount to validate
     * @param context the validation context for error message customization
     * @return true if the payment amount is valid, false otherwise
     */
    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        // Disable default constraint violation to provide custom messages
        context.disableDefaultConstraintViolation();
        
        // Handle null value validation
        if (value == null) {
            if (allowNull) {
                return true;
            }
            addConstraintViolation(context, nullMessage);
            return false;
        }
        
        // Validate minimum amount constraint
        // Based on COBIL00C.cbl line 198-206: ACCT-CURR-BAL <= ZEROS validation
        if (value.compareTo(minAmount) < 0) {
            String message = belowMinimumMessage.replace("{minAmount}", minAmount.toString());
            addConstraintViolation(context, message);
            return false;
        }
        
        // Validate maximum amount constraint
        // Based on CVTRA05Y.cpy TRAN-AMT PIC S9(09)V99 field capacity
        if (value.compareTo(maxAmount) > 0) {
            String message = aboveMaximumMessage.replace("{maxAmount}", maxAmount.toString());
            addConstraintViolation(context, message);
            return false;
        }
        
        // Validate scale requirement (decimal places)
        // COBOL V99 specification requires exactly 2 decimal places
        if (value.scale() != requiredScale) {
            String message = scaleMessage.replace("{scale}", String.valueOf(requiredScale));
            addConstraintViolation(context, message);
            return false;
        }
        
        // Validate precision requirement (total digits)
        // COBOL S9(09)V99 specification allows maximum 11 total digits (9 + 2)
        if (getPrecision(value) > maxPrecision) {
            String message = precisionMessage.replace("{precision}", String.valueOf(maxPrecision));
            addConstraintViolation(context, message);
            return false;
        }
        
        // All validations passed - payment amount is valid for bill payment processing
        return true;
    }

    /**
     * Calculates the precision (total number of significant digits) of a BigDecimal.
     * 
     * This method determines the total number of digits in a BigDecimal value,
     * which is used to validate against COBOL COMP-3 field precision constraints.
     * 
     * The precision calculation accounts for:
     * - All digits in the unscaled value
     * - Trailing zeros that are significant in financial calculations
     * - COBOL COMP-3 precision requirements
     * 
     * @param value the BigDecimal value to analyze
     * @return the total number of significant digits
     */
    private int getPrecision(BigDecimal value) {
        // Use BigDecimal's precision method for accurate digit counting
        // This handles all edge cases including scientific notation and trailing zeros
        return value.precision();
    }

    /**
     * Adds a constraint violation with the specified message to the validation context.
     * 
     * This method provides centralized error message handling for all validation
     * failures, ensuring consistent error reporting across different validation scenarios.
     * 
     * @param context the validation context for error message registration
     * @param message the detailed validation error message
     */
    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
    }
}