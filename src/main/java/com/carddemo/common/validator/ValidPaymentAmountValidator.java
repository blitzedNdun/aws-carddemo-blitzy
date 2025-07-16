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
import java.util.logging.Logger;

/**
 * Validator implementation for {@link ValidPaymentAmount} annotation.
 * 
 * This validator enforces bill payment amount validation according to CardDemo business rules,
 * ensuring exact BigDecimal precision equivalent to COBOL COMP-3 arithmetic. The validation
 * logic replicates the original COBOL program COBIL00C.cbl payment amount validation.
 * 
 * <p>Validation Rules Implemented:</p>
 * <ul>
 *   <li>Null amount validation with configurable error messages</li>
 *   <li>Negative amount prevention matching COBOL unsigned arithmetic</li>
 *   <li>Zero amount validation with configurable allowance</li>
 *   <li>Minimum/maximum amount range validation</li>
 *   <li>Decimal precision validation for COBOL COMP-3 compatibility</li>
 *   <li>COBOL format constraint enforcement</li>
 * </ul>
 * 
 * <p>Technical Implementation:</p>
 * <ul>
 *   <li>Uses MathContext.DECIMAL128 for exact BigDecimal precision</li>
 *   <li>Validates against COBOL TRAN-AMT format constraints</li>
 *   <li>Provides detailed error messages for each validation failure</li>
 *   <li>Supports configuration through annotation parameters</li>
 * </ul>
 * 
 * @see ValidPaymentAmount
 * @since 1.0
 */
public class ValidPaymentAmountValidator implements ConstraintValidator<ValidPaymentAmount, BigDecimal> {
    
    private static final Logger logger = Logger.getLogger(ValidPaymentAmountValidator.class.getName());
    
    /**
     * COBOL COMP-3 equivalent precision context for exact decimal arithmetic.
     * Uses DECIMAL128 precision with HALF_UP rounding to match COBOL behavior.
     */
    private static final MathContext COBOL_MATH_CONTEXT = new MathContext(31, RoundingMode.HALF_UP);
    
    /**
     * Maximum integer digits allowed in COBOL TRAN-AMT format (PIC S9(09)V99).
     */
    private static final int MAX_INTEGER_DIGITS = 9;
    
    /**
     * Fixed decimal places for COBOL COMP-3 decimal format.
     */
    private static final int COBOL_DECIMAL_PLACES = 2;
    
    // Annotation configuration fields
    private boolean allowZero;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private int decimalPlaces;
    private boolean strictDecimalPrecision;
    private boolean enforceCobolFormat;
    
    // Error message fields
    private String minAmountMessage;
    private String maxAmountMessage;
    private String zeroAmountMessage;
    private String negativeAmountMessage;
    private String decimalPrecisionMessage;
    private String nullAmountMessage;
    
    /**
     * Initializes the validator with configuration from the annotation.
     * 
     * @param annotation the ValidPaymentAmount annotation instance
     */
    @Override
    public void initialize(ValidPaymentAmount annotation) {
        this.allowZero = annotation.allowZero();
        this.decimalPlaces = annotation.decimalPlaces();
        this.strictDecimalPrecision = annotation.strictDecimalPrecision();
        this.enforceCobolFormat = annotation.enforceCobolFormat();
        
        // Parse min and max amounts with exact precision
        try {
            this.minAmount = new BigDecimal(annotation.minAmount(), COBOL_MATH_CONTEXT);
            this.maxAmount = new BigDecimal(annotation.maxAmount(), COBOL_MATH_CONTEXT);
        } catch (NumberFormatException e) {
            logger.severe("Invalid min/max amount configuration in ValidPaymentAmount annotation: " + e.getMessage());
            throw new IllegalArgumentException("Invalid min/max amount configuration", e);
        }
        
        // Store error messages
        this.minAmountMessage = annotation.minAmountMessage();
        this.maxAmountMessage = annotation.maxAmountMessage();
        this.zeroAmountMessage = annotation.zeroAmountMessage();
        this.negativeAmountMessage = annotation.negativeAmountMessage();
        this.decimalPrecisionMessage = annotation.decimalPrecisionMessage();
        this.nullAmountMessage = annotation.nullAmountMessage();
        
        logger.info("ValidPaymentAmountValidator initialized with minAmount=" + this.minAmount + 
                   ", maxAmount=" + this.maxAmount + ", decimalPlaces=" + this.decimalPlaces);
    }
    
    /**
     * Validates the payment amount against all configured business rules.
     * 
     * @param value the payment amount to validate
     * @param context the validation context for error reporting
     * @return true if the payment amount is valid, false otherwise
     */
    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        // Disable default constraint violation message
        context.disableDefaultConstraintViolation();
        
        // Check for null values
        if (value == null) {
            addConstraintViolation(context, nullAmountMessage);
            return false;
        }
        
        // Validate negative amounts
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            addConstraintViolation(context, negativeAmountMessage);
            return false;
        }
        
        // Validate zero amounts
        if (value.compareTo(BigDecimal.ZERO) == 0 && !allowZero) {
            addConstraintViolation(context, zeroAmountMessage);
            return false;
        }
        
        // Validate minimum amount
        if (value.compareTo(minAmount) < 0) {
            addConstraintViolation(context, minAmountMessage.replace("{minAmount}", minAmount.toPlainString()));
            return false;
        }
        
        // Validate maximum amount
        if (value.compareTo(maxAmount) > 0) {
            addConstraintViolation(context, maxAmountMessage.replace("{maxAmount}", maxAmount.toPlainString()));
            return false;
        }
        
        // Validate decimal precision
        if (!validateDecimalPrecision(value)) {
            addConstraintViolation(context, decimalPrecisionMessage.replace("{decimalPlaces}", String.valueOf(decimalPlaces)));
            return false;
        }
        
        // Validate COBOL format constraints
        if (enforceCobolFormat && !validateCobolFormat(value)) {
            addConstraintViolation(context, "Payment amount exceeds COBOL format constraints (PIC S9(09)V99)");
            return false;
        }
        
        logger.fine("Payment amount validation successful for value: " + value);
        return true;
    }
    
    /**
     * Validates that the amount has the correct number of decimal places.
     * 
     * @param value the amount to validate
     * @return true if decimal precision is valid, false otherwise
     */
    private boolean validateDecimalPrecision(BigDecimal value) {
        int actualDecimalPlaces = value.scale();
        
        if (strictDecimalPrecision) {
            // Strict mode: must have exactly the specified number of decimal places
            return actualDecimalPlaces == decimalPlaces;
        } else {
            // Lenient mode: cannot exceed the specified number of decimal places
            return actualDecimalPlaces <= decimalPlaces;
        }
    }
    
    /**
     * Validates that the amount conforms to COBOL COMP-3 format constraints.
     * Based on TRAN-AMT field format (PIC S9(09)V99) from CVTRA05Y.cpy.
     * 
     * @param value the amount to validate
     * @return true if COBOL format is valid, false otherwise
     */
    private boolean validateCobolFormat(BigDecimal value) {
        // Check if the value fits within COBOL integer digit constraints
        String valueStr = value.toPlainString();
        int decimalIndex = valueStr.indexOf('.');
        
        // Calculate integer part length
        int integerDigits;
        if (decimalIndex >= 0) {
            integerDigits = decimalIndex;
        } else {
            integerDigits = valueStr.length();
        }
        
        // Remove leading zeros for accurate digit count
        String integerPart = valueStr.substring(0, decimalIndex >= 0 ? decimalIndex : valueStr.length());
        integerPart = integerPart.replaceFirst("^0+", "");
        if (integerPart.isEmpty()) {
            integerPart = "0";
        }
        
        // Validate against COBOL PIC S9(09)V99 constraints
        if (integerPart.length() > MAX_INTEGER_DIGITS) {
            logger.warning("Payment amount exceeds COBOL integer digit limit: " + integerPart.length() + " > " + MAX_INTEGER_DIGITS);
            return false;
        }
        
        // Ensure the value maintains COBOL decimal precision
        if (value.scale() > COBOL_DECIMAL_PLACES) {
            logger.warning("Payment amount exceeds COBOL decimal precision: " + value.scale() + " > " + COBOL_DECIMAL_PLACES);
            return false;
        }
        
        return true;
    }
    
    /**
     * Adds a constraint violation with the specified message to the validation context.
     * 
     * @param context the validation context
     * @param message the violation message
     */
    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
    }
}