/*
 * ValidCurrencyValidator.java
 * 
 * Implementation class for currency amount validation with BigDecimal precision.
 * Validates monetary amounts using exact decimal precision equivalent to COBOL COMP-3 
 * calculations while supporting configurable range validation for different financial contexts.
 * 
 * This validator implements:
 * - DECIMAL(12,2) format validation with exact financial precision matching COBOL S9(10)V99
 * - Configurable minimum/maximum values for different monetary contexts
 * - BigDecimal precision requirements maintaining COBOL COMP-3 arithmetic equivalence
 * - Range validation for account balances, credit limits, and transaction amounts
 * - Null value handling following Jakarta Bean Validation standards
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
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

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Implementation class for currency amount validation with BigDecimal precision.
 * 
 * This validator provides comprehensive monetary validation including:
 * - Exact BigDecimal precision equivalent to COBOL COMP-3 calculations
 * - Configurable range validation for different financial contexts
 * - Precision and scale validation maintaining financial accuracy
 * - Support for zero and negative value handling based on business requirements
 * 
 * <p><strong>Validation Logic:</strong></p>
 * <ul>
 *   <li>Null values are considered valid (use @NotNull for null validation)</li>
 *   <li>Values must be within the specified min/max range if provided</li>
 *   <li>Scale must not exceed the configured maximum decimal places</li>
 *   <li>Precision must not exceed the configured maximum total digits</li>
 *   <li>Zero and negative value handling based on annotation configuration</li>
 * </ul>
 * 
 * <p><strong>COBOL COMP-3 Equivalence:</strong></p>
 * <ul>
 *   <li>Uses MathContext.DECIMAL128 for exact arithmetic precision</li>
 *   <li>Maintains exact decimal place requirements</li>
 *   <li>Preserves financial calculation accuracy</li>
 * </ul>
 *
 * @author AWS CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0-15-g27d6c6f-68
 * @see ValidCurrency
 * @see ValidationConstants
 */
public class ValidCurrencyValidator implements ConstraintValidator<ValidCurrency, BigDecimal> {

    private ValidCurrency constraint;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private int precision;
    private int scale;
    private boolean allowZero;
    private boolean allowNegative;

    /**
     * Initializes the validator with constraint annotation values.
     * 
     * @param constraint the ValidCurrency annotation instance
     */
    @Override
    public void initialize(ValidCurrency constraint) {
        this.constraint = constraint;
        this.precision = constraint.precision();
        this.scale = constraint.scale();
        this.allowZero = constraint.allowZero();
        this.allowNegative = constraint.allowNegative();
        
        // Parse min/max values if provided
        this.minValue = parseDecimalValue(constraint.min());
        this.maxValue = parseDecimalValue(constraint.max());
    }

    /**
     * Validates the currency amount according to configured constraints.
     * 
     * @param value the BigDecimal value to validate
     * @param context the validation context for error message customization
     * @return true if valid, false otherwise
     */
    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        // Null values are valid (use @NotNull for null validation)
        if (value == null) {
            return true;
        }

        // Disable default constraint violation to provide custom messages
        context.disableDefaultConstraintViolation();

        // Validate zero values
        if (!allowZero && value.compareTo(BigDecimal.ZERO) == 0) {
            context.buildConstraintViolationWithTemplate(
                "Currency amount cannot be zero").addConstraintViolation();
            return false;
        }

        // Validate negative values
        if (!allowNegative && value.compareTo(BigDecimal.ZERO) < 0) {
            context.buildConstraintViolationWithTemplate(
                "Currency amount cannot be negative").addConstraintViolation();
            return false;
        }

        // Validate precision (total number of digits)
        if (!validatePrecision(value, context)) {
            return false;
        }

        // Validate scale (decimal places)
        if (!validateScale(value, context)) {
            return false;
        }

        // Validate minimum value
        if (minValue != null && value.compareTo(minValue) < 0) {
            context.buildConstraintViolationWithTemplate(
                String.format("Currency amount must be at least %s", minValue.toPlainString()))
                .addConstraintViolation();
            return false;
        }

        // Validate maximum value
        if (maxValue != null && value.compareTo(maxValue) > 0) {
            context.buildConstraintViolationWithTemplate(
                String.format("Currency amount must not exceed %s", maxValue.toPlainString()))
                .addConstraintViolation();
            return false;
        }

        return true;
    }

    /**
     * Validates the precision (total number of digits) of the currency amount.
     * 
     * @param value the BigDecimal value to validate
     * @param context the validation context for error messages
     * @return true if precision is valid, false otherwise
     */
    private boolean validatePrecision(BigDecimal value, ConstraintValidatorContext context) {
        // Use BigDecimal's built-in precision() method which counts all significant digits
        int actualPrecision = value.precision();

        if (actualPrecision > precision) {
            context.buildConstraintViolationWithTemplate(
                String.format("Currency amount has too many digits (%d) - maximum allowed is %d", 
                    actualPrecision, precision))
                .addConstraintViolation();
            return false;
        }

        return true;
    }

    /**
     * Validates the scale (number of decimal places) of the currency amount.
     * 
     * @param value the BigDecimal value to validate
     * @param context the validation context for error messages
     * @return true if scale is valid, false otherwise
     */
    private boolean validateScale(BigDecimal value, ConstraintValidatorContext context) {
        // Use the actual scale of the BigDecimal value
        // For negative scale (like with very large numbers), use 0
        int actualScale = Math.max(0, value.scale());

        if (actualScale > scale) {
            context.buildConstraintViolationWithTemplate(
                String.format("Currency amount has too many decimal places (%d) - maximum allowed is %d", 
                    actualScale, scale))
                .addConstraintViolation();
            return false;
        }

        return true;
    }

    /**
     * Parses a string value to BigDecimal with COBOL-equivalent precision.
     * 
     * @param value the string value to parse
     * @return the parsed BigDecimal or null if empty/null
     */
    private BigDecimal parseDecimalValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            // Use DECIMAL128 context for COBOL COMP-3 equivalence
            return new BigDecimal(value, MathContext.DECIMAL128);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid decimal value in ValidCurrency constraint: " + value, e);
        }
    }
}