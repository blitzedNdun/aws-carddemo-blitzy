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
 * Jakarta Bean Validation implementation for currency amount validation with BigDecimal precision.
 * 
 * <p>This validator ensures monetary fields maintain exact financial precision equivalent to 
 * COBOL COMP-3 calculations while supporting configurable range validation for different 
 * financial contexts. The implementation uses BigDecimal arithmetic with MathContext.DECIMAL128 
 * to provide exact decimal precision without floating-point errors.</p>
 * 
 * <p>Key validation features:</p>
 * <ul>
 *   <li>DECIMAL(12,2) precision matching COBOL COMP-3 specifications</li>
 *   <li>Configurable minimum and maximum value ranges</li>
 *   <li>Exact financial calculations using MathContext.DECIMAL128</li>
 *   <li>Support for various currency contexts (account balances, credit limits, etc.)</li>
 *   <li>COBOL-compatible arithmetic behavior and rounding modes</li>
 * </ul>
 * 
 * <p>The validator handles the following validation scenarios:</p>
 * <ul>
 *   <li>Null value validation based on allowNull configuration</li>
 *   <li>Precision and scale validation according to COBOL COMP-3 standards</li>
 *   <li>Range validation with configurable minimum and maximum values</li>
 *   <li>Context-specific validation rules for different financial field types</li>
 * </ul>
 * 
 * <p>This implementation directly corresponds to the COBOL validation logic found in 
 * the original CardDemo mainframe application, ensuring identical business rule enforcement 
 * during the technology stack migration.</p>
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since CardDemo v1.0-15-g27d6c6f-68
 * @see ValidCurrency
 * @see ValidationConstants
 * @see java.math.BigDecimal
 * @see java.math.MathContext
 */
public class CurrencyValidator implements ConstraintValidator<ValidCurrency, BigDecimal> {

    /**
     * MathContext for COBOL COMP-3 equivalent calculations.
     * Uses DECIMAL128 precision for exact financial arithmetic.
     */
    private static final MathContext COBOL_MATH_CONTEXT = MathContext.DECIMAL128;

    /**
     * The minimum allowed value for this currency validator instance.
     * Parsed from the annotation's min attribute.
     */
    private BigDecimal minValue;

    /**
     * The maximum allowed value for this currency validator instance.
     * Parsed from the annotation's max attribute.
     */
    private BigDecimal maxValue;

    /**
     * The required precision (total number of digits) for the currency amount.
     * Corresponds to COBOL COMP-3 field precision specifications.
     */
    private int precision;

    /**
     * The required scale (number of decimal places) for the currency amount.
     * Typically 2 for standard currency representation.
     */
    private int scale;

    /**
     * Whether null values are allowed for this field.
     * When false, null values trigger validation errors.
     */
    private boolean allowNull;

    /**
     * Whether to enforce strict precision matching.
     * When true, the BigDecimal must have exactly the specified scale.
     */
    private boolean strictPrecision;

    /**
     * The rounding mode to use for precision adjustments.
     * Defaults to HALF_EVEN for financial calculations.
     */
    private RoundingMode roundingMode;

    /**
     * The validation context for specialized currency types.
     * Allows for different validation rules based on the currency context.
     */
    private String context;

    /**
     * Whether to validate against COBOL COMP-3 equivalent behavior.
     * When true, ensures exact equivalence with COBOL arithmetic.
     */
    private boolean cobolCompatible;

    /**
     * Initializes the validator with the constraint annotation parameters.
     * 
     * <p>This method parses the annotation parameters and prepares the validator
     * for validating currency amounts. It converts string-based min/max values
     * to BigDecimal instances using the COBOL-compatible math context.</p>
     * 
     * @param constraintAnnotation the ValidCurrency annotation with validation parameters
     * @throws IllegalArgumentException if min/max values cannot be parsed as BigDecimal
     */
    @Override
    public void initialize(ValidCurrency constraintAnnotation) {
        try {
            // Parse min and max values using COBOL-compatible math context
            this.minValue = new BigDecimal(constraintAnnotation.min(), COBOL_MATH_CONTEXT);
            this.maxValue = new BigDecimal(constraintAnnotation.max(), COBOL_MATH_CONTEXT);
            
            // Validate that min <= max
            if (this.minValue.compareTo(this.maxValue) > 0) {
                throw new IllegalArgumentException(
                    "Currency validation: minimum value (" + constraintAnnotation.min() + 
                    ") cannot be greater than maximum value (" + constraintAnnotation.max() + ")");
            }
            
            // Set precision and scale parameters
            this.precision = constraintAnnotation.precision();
            this.scale = constraintAnnotation.scale();
            
            // Validate precision and scale consistency
            if (this.scale < 0 || this.precision < 0) {
                throw new IllegalArgumentException(
                    "Currency validation: precision and scale must be non-negative values");
            }
            
            if (this.scale > this.precision) {
                throw new IllegalArgumentException(
                    "Currency validation: scale (" + this.scale + 
                    ") cannot be greater than precision (" + this.precision + ")");
            }
            
            // Set additional validation parameters
            this.allowNull = constraintAnnotation.allowNull();
            this.strictPrecision = constraintAnnotation.strictPrecision();
            this.context = constraintAnnotation.context();
            this.cobolCompatible = constraintAnnotation.cobolCompatible();
            
            // Parse and validate rounding mode
            try {
                this.roundingMode = RoundingMode.valueOf(constraintAnnotation.roundingMode());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    "Currency validation: invalid rounding mode '" + constraintAnnotation.roundingMode() + "'");
            }
            
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "Currency validation: invalid numeric format in min/max values", e);
        }
    }

    /**
     * Validates a BigDecimal currency amount according to the configured constraints.
     * 
     * <p>This method performs comprehensive validation including:</p>
     * <ul>
     *   <li>Null value validation based on allowNull setting</li>
     *   <li>Precision and scale validation for COBOL COMP-3 compatibility</li>
     *   <li>Range validation against configured minimum and maximum values</li>
     *   <li>Context-specific validation rules for different currency types</li>
     *   <li>COBOL-compatible arithmetic behavior verification</li>
     * </ul>
     * 
     * @param value the BigDecimal value to validate
     * @param context the validation context for error message customization
     * @return true if the value is valid, false otherwise
     */
    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        // Handle null values based on allowNull setting
        if (value == null) {
            if (allowNull) {
                return true;
            } else {
                addConstraintViolation(context, "Currency amount cannot be null");
                return false;
            }
        }

        // Validate precision and scale
        if (!validatePrecisionAndScale(value, context)) {
            return false;
        }

        // Validate range constraints
        if (!validateRange(value, context)) {
            return false;
        }

        // Perform context-specific validation
        if (!validateByContext(value, context)) {
            return false;
        }

        // Validate COBOL COMP-3 compatibility if required
        if (cobolCompatible && !validateCobolCompatibility(value, context)) {
            return false;
        }

        return true;
    }

    /**
     * Validates that the BigDecimal value has the correct precision and scale.
     * 
     * <p>This method ensures that the currency amount conforms to the specified
     * precision and scale requirements, which correspond to COBOL COMP-3 field
     * specifications. It supports both strict and lenient precision matching.</p>
     * 
     * @param value the BigDecimal value to validate
     * @param context the validation context for error messages
     * @return true if precision and scale are valid, false otherwise
     */
    private boolean validatePrecisionAndScale(BigDecimal value, ConstraintValidatorContext context) {
        // Check scale (number of decimal places)
        int valueScale = value.scale();
        
        if (strictPrecision) {
            // Strict mode: scale must match exactly
            if (valueScale != scale) {
                addConstraintViolation(context, 
                    "Currency amount must have exactly " + scale + " decimal places, but has " + valueScale);
                return false;
            }
        } else {
            // Lenient mode: scale can be less than or equal to required scale
            if (valueScale > scale) {
                addConstraintViolation(context, 
                    "Currency amount cannot have more than " + scale + " decimal places, but has " + valueScale);
                return false;
            }
        }

        // Check precision (total number of digits)
        int valuePrecision = value.precision();
        if (valuePrecision > precision) {
            addConstraintViolation(context, 
                "Currency amount cannot have more than " + precision + " total digits, but has " + valuePrecision);
            return false;
        }

        return true;
    }

    /**
     * Validates that the BigDecimal value falls within the configured range.
     * 
     * <p>This method checks that the currency amount is between the minimum and
     * maximum values specified in the annotation. The comparison uses BigDecimal's
     * compareTo method to ensure exact precision matching.</p>
     * 
     * @param value the BigDecimal value to validate
     * @param context the validation context for error messages
     * @return true if the value is within the valid range, false otherwise
     */
    private boolean validateRange(BigDecimal value, ConstraintValidatorContext context) {
        // Check minimum value constraint
        if (value.compareTo(minValue) < 0) {
            addConstraintViolation(context, 
                "Currency amount " + value + " is below minimum allowed value " + minValue);
            return false;
        }

        // Check maximum value constraint
        if (value.compareTo(maxValue) > 0) {
            addConstraintViolation(context, 
                "Currency amount " + value + " exceeds maximum allowed value " + maxValue);
            return false;
        }

        return true;
    }

    /**
     * Performs context-specific validation based on the currency type.
     * 
     * <p>This method applies specialized validation rules based on the currency
     * context (e.g., account balance, credit limit, transaction amount). It uses
     * the constants from ValidationConstants to enforce business rules specific
     * to different financial contexts.</p>
     * 
     * @param value the BigDecimal value to validate
     * @param context the validation context for error messages
     * @return true if context-specific validation passes, false otherwise
     */
    private boolean validateByContext(BigDecimal value, ConstraintValidatorContext context) {
        switch (this.context.toUpperCase()) {
            case "ACCOUNT_BALANCE":
                return validateAccountBalance(value, context);
            
            case "CREDIT_LIMIT":
                return validateCreditLimit(value, context);
            
            case "TRANSACTION_AMOUNT":
                return validateTransactionAmount(value, context);
            
            case "INTEREST_RATE":
                return validateInterestRate(value, context);
            
            case "GENERAL":
            default:
                // General validation - no additional context-specific rules
                return true;
        }
    }

    /**
     * Validates currency amounts in the context of account balances.
     * 
     * <p>Account balances can be negative (overdraft) but must maintain proper
     * precision for financial calculations. This method enforces business rules
     * specific to account balance management.</p>
     * 
     * @param value the BigDecimal value to validate
     * @param context the validation context for error messages
     * @return true if valid for account balance context, false otherwise
     */
    private boolean validateAccountBalance(BigDecimal value, ConstraintValidatorContext context) {
        // Account balances are allowed to be negative (overdraft scenarios)
        // but we ensure they maintain proper scale for financial calculations
        if (value.scale() > ValidationConstants.CURRENCY_SCALE) {
            addConstraintViolation(context, 
                "Account balance must not exceed " + ValidationConstants.CURRENCY_SCALE + " decimal places");
            return false;
        }
        
        return true;
    }

    /**
     * Validates currency amounts in the context of credit limits.
     * 
     * <p>Credit limits must be positive and within the range defined by business
     * rules. This method uses ValidationConstants to enforce minimum and maximum
     * credit limit constraints.</p>
     * 
     * @param value the BigDecimal value to validate
     * @param context the validation context for error messages
     * @return true if valid for credit limit context, false otherwise
     */
    private boolean validateCreditLimit(BigDecimal value, ConstraintValidatorContext context) {
        // Credit limits must be positive
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            addConstraintViolation(context, 
                "Credit limit must be greater than zero");
            return false;
        }
        
        // Check against business rule constraints
        BigDecimal minCreditLimit = new BigDecimal(ValidationConstants.MIN_CREDIT_LIMIT, COBOL_MATH_CONTEXT);
        BigDecimal maxCreditLimit = new BigDecimal(ValidationConstants.MAX_CREDIT_LIMIT, COBOL_MATH_CONTEXT);
        
        if (value.compareTo(minCreditLimit) < 0) {
            addConstraintViolation(context, 
                "Credit limit " + value + " is below minimum business rule limit " + minCreditLimit);
            return false;
        }
        
        if (value.compareTo(maxCreditLimit) > 0) {
            addConstraintViolation(context, 
                "Credit limit " + value + " exceeds maximum business rule limit " + maxCreditLimit);
            return false;
        }
        
        return true;
    }

    /**
     * Validates currency amounts in the context of transaction amounts.
     * 
     * <p>Transaction amounts can be positive or negative (credits/debits) and
     * must maintain exact precision for financial processing. This method
     * enforces transaction-specific business rules.</p>
     * 
     * @param value the BigDecimal value to validate
     * @param context the validation context for error messages
     * @return true if valid for transaction amount context, false otherwise
     */
    private boolean validateTransactionAmount(BigDecimal value, ConstraintValidatorContext context) {
        // Transaction amounts cannot be zero (no-op transactions)
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            addConstraintViolation(context, 
                "Transaction amount cannot be zero");
            return false;
        }
        
        // Ensure proper currency scale for transaction processing
        if (value.scale() > ValidationConstants.CURRENCY_SCALE) {
            addConstraintViolation(context, 
                "Transaction amount must not exceed " + ValidationConstants.CURRENCY_SCALE + " decimal places");
            return false;
        }
        
        return true;
    }

    /**
     * Validates currency amounts in the context of interest rates.
     * 
     * <p>Interest rates require higher precision than standard currency amounts
     * and must be positive. This method enforces interest rate-specific
     * validation rules.</p>
     * 
     * @param value the BigDecimal value to validate
     * @param context the validation context for error messages
     * @return true if valid for interest rate context, false otherwise
     */
    private boolean validateInterestRate(BigDecimal value, ConstraintValidatorContext context) {
        // Interest rates must be positive
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            addConstraintViolation(context, 
                "Interest rate must be greater than zero");
            return false;
        }
        
        // Interest rates typically don't exceed 100% (1.0000)
        BigDecimal maxRate = new BigDecimal("1.0000", COBOL_MATH_CONTEXT);
        if (value.compareTo(maxRate) > 0) {
            addConstraintViolation(context, 
                "Interest rate " + value + " exceeds maximum reasonable rate " + maxRate);
            return false;
        }
        
        return true;
    }

    /**
     * Validates COBOL COMP-3 compatibility for exact arithmetic equivalence.
     * 
     * <p>This method ensures that the BigDecimal value maintains exact
     * equivalence with COBOL COMP-3 packed decimal calculations. It validates
     * that the arithmetic behavior matches the original COBOL implementation.</p>
     * 
     * @param value the BigDecimal value to validate
     * @param context the validation context for error messages
     * @return true if COBOL COMP-3 compatible, false otherwise
     */
    private boolean validateCobolCompatibility(BigDecimal value, ConstraintValidatorContext context) {
        // Ensure the value can be represented exactly in COBOL COMP-3 format
        try {
            // Test arithmetic operations with COBOL math context
            BigDecimal testResult = value.add(BigDecimal.ZERO, COBOL_MATH_CONTEXT);
            
            // Verify the result maintains precision
            if (testResult.compareTo(value) != 0) {
                addConstraintViolation(context, 
                    "Currency amount cannot be represented exactly in COBOL COMP-3 format");
                return false;
            }
            
            // Verify scale is compatible with COBOL COMP-3
            if (value.scale() > ValidationConstants.CURRENCY_SCALE && 
                !this.context.equals("INTEREST_RATE")) {
                addConstraintViolation(context, 
                    "Currency amount scale exceeds COBOL COMP-3 compatibility limit");
                return false;
            }
            
            return true;
            
        } catch (ArithmeticException e) {
            addConstraintViolation(context, 
                "Currency amount causes arithmetic overflow in COBOL COMP-3 calculations");
            return false;
        }
    }

    /**
     * Adds a custom constraint violation message to the validation context.
     * 
     * <p>This method disables the default constraint violation message and
     * adds a custom message that provides more specific information about
     * the validation failure.</p>
     * 
     * @param context the validation context
     * @param message the custom error message to add
     */
    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
    }
}