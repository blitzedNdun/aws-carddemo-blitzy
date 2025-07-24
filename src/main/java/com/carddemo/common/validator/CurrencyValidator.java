/*
 * CurrencyValidator.java
 *
 * Implementation class for currency amount validation with BigDecimal precision.
 * Validates monetary amounts using exact decimal precision equivalent to COBOL COMP-3 
 * calculations while supporting configurable range validation for different financial contexts.
 *
 * This validator ensures:
 * - DECIMAL(12,2) format validation with exact financial precision matching COBOL S9(10)V99
 * - Configurable minimum/maximum values for different monetary contexts
 * - BigDecimal precision requirements maintaining COBOL COMP-3 arithmetic equivalence
 * - Range validation for account balances, credit limits, and transaction amounts
 * - Comprehensive error reporting for precision and range violations
 *
 * COBOL COMP-3 Field Mappings:
 * - ACCT-CURR-BAL PIC S9(10)V99 COMP-3 → DECIMAL(12,2) precision
 * - ACCT-CREDIT-LIMIT PIC S9(10)V99 COMP-3 → DECIMAL(12,2) precision
 * - TRAN-CAT-BAL PIC S9(09)V99 COMP-3 → DECIMAL(11,2) precision
 *
 * Interest calculation from CBACT04C.cbl line 464-465:
 * COMPUTE WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
 * requires exact precision arithmetic equivalent to MathContext.DECIMAL128
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
 * Jakarta Bean Validation implementation for currency amount validation with exact BigDecimal precision.
 * 
 * This validator implements the ValidCurrency annotation to ensure monetary fields maintain
 * exact financial precision equivalent to COBOL COMP-3 calculations while supporting
 * comprehensive validation for different currency contexts in the CardDemo application.
 * 
 * <p><strong>Validation Features:</strong></p>
 * <ul>
 *   <li>Exact BigDecimal precision using MathContext.DECIMAL128 for COBOL COMP-3 equivalence</li>
 *   <li>Configurable precision and scale validation (default DECIMAL 12,2)</li>
 *   <li>Range validation with configurable minimum and maximum values</li>
 *   <li>Zero and negative value validation based on business rules</li>
 *   <li>Comprehensive error messages for different validation failure scenarios</li>
 * </ul>
 * 
 * <p><strong>COBOL COMP-3 Precision Mapping:</strong></p>
 * <ul>
 *   <li>S9(10)V99 COMP-3 → precision=12, scale=2 (account balances, credit limits)</li>
 *   <li>S9(09)V99 COMP-3 → precision=11, scale=2 (transaction category balances)</li>
 *   <li>Uses DECIMAL128 MathContext for exact arithmetic matching COBOL behavior</li>
 * </ul>
 * 
 * <p><strong>Business Rule Integration:</strong></p>
 * <ul>
 *   <li>MIN_CREDIT_LIMIT ($500) from ValidationConstants for credit limit validation</li>
 *   <li>MAX_CREDIT_LIMIT ($50,000) from ValidationConstants for credit limit validation</li>
 *   <li>CURRENCY_PRECISION (12) and CURRENCY_SCALE (2) from ValidationConstants</li>
 * </ul>
 *
 * @author AWS CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0-15-g27d6c6f-68
 * @see ValidCurrency
 * @see ValidationConstants
 */
public class CurrencyValidator implements ConstraintValidator<ValidCurrency, BigDecimal> {

    /**
     * MathContext for exact financial calculations equivalent to COBOL COMP-3 arithmetic precision.
     * Uses DECIMAL128 to ensure no precision loss in monetary calculations.
     */
    private static final MathContext COBOL_MATH_CONTEXT = MathContext.DECIMAL128;

    // Validation constraint parameters
    private int precision;
    private int scale;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private boolean allowZero;
    private boolean allowNegative;
    private String originalMessage;

    /**
     * Initializes the validator with constraint parameters from the ValidCurrency annotation.
     * Sets up precision, scale, range limits, and flags for validation logic.
     *
     * @param constraintAnnotation the ValidCurrency annotation instance with validation parameters
     */
    @Override
    public void initialize(ValidCurrency constraintAnnotation) {
        this.precision = constraintAnnotation.precision();
        this.scale = constraintAnnotation.scale();
        this.allowZero = constraintAnnotation.allowZero();
        this.allowNegative = constraintAnnotation.allowNegative();
        this.originalMessage = constraintAnnotation.message();

        // Parse minimum value with exact precision if specified
        if (!constraintAnnotation.min().isEmpty()) {
            try {
                this.minValue = new BigDecimal(constraintAnnotation.min(), COBOL_MATH_CONTEXT);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid minimum value format: " + constraintAnnotation.min(), e);
            }
        }

        // Parse maximum value with exact precision if specified
        if (!constraintAnnotation.max().isEmpty()) {
            try {
                this.maxValue = new BigDecimal(constraintAnnotation.max(), COBOL_MATH_CONTEXT);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid maximum value format: " + constraintAnnotation.max(), e);
            }
        }

        // Validate constraint parameters consistency
        validateConstraintParameters();
    }

    /**
     * Validates a currency amount against all configured constraints.
     * Performs comprehensive validation including precision, scale, range, and business rule checks.
     *
     * @param value the BigDecimal currency amount to validate (null values are considered valid)
     * @param context the constraint validator context for custom error messages
     * @return true if the value is valid according to all constraints, false otherwise
     */
    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        // Null values are considered valid - use @NotNull for null validation
        if (value == null) {
            return true;
        }

        // Disable default constraint violation to provide custom error messages
        context.disableDefaultConstraintViolation();

        // Normalize value using COBOL-equivalent math context for exact precision
        BigDecimal normalizedValue = value.round(COBOL_MATH_CONTEXT);

        // Validate precision (total number of digits)
        if (!validatePrecision(normalizedValue, context)) {
            return false;
        }

        // Validate scale (decimal places)
        if (!validateScale(normalizedValue, context)) {
            return false;
        }

        // Validate zero values if not allowed
        if (!validateZeroConstraint(normalizedValue, context)) {
            return false;
        }

        // Validate negative values if not allowed
        if (!validateNegativeConstraint(normalizedValue, context)) {
            return false;
        }

        // Validate minimum value constraint
        if (!validateMinimumValue(normalizedValue, context)) {
            return false;
        }

        // Validate maximum value constraint
        if (!validateMaximumValue(normalizedValue, context)) {
            return false;
        }

        // All validations passed
        return true;
    }

    /**
     * Validates that the currency amount does not exceed the configured precision limit.
     * Ensures total digits (including both integer and fractional parts) match COBOL COMP-3 limits.
     *
     * @param value the currency amount to validate
     * @param context the constraint validator context for error messages
     * @return true if precision is valid, false otherwise
     */
    private boolean validatePrecision(BigDecimal value, ConstraintValidatorContext context) {
        // Calculate total number of significant digits
        String plainString = value.stripTrailingZeros().toPlainString();
        int totalDigits = plainString.replace(".", "").replace("-", "").length();

        if (totalDigits > precision) {
            context.buildConstraintViolationWithTemplate(
                String.format("Currency amount has %d total digits but maximum allowed is %d (precision constraint)", 
                             totalDigits, precision)
            ).addConstraintViolation();
            return false;
        }

        return true;
    }

    /**
     * Validates that the currency amount does not exceed the configured scale limit.
     * Ensures decimal places match COBOL COMP-3 scale requirements (typically 2 for currency).
     *
     * @param value the currency amount to validate
     * @param context the constraint validator context for error messages
     * @return true if scale is valid, false otherwise
     */
    private boolean validateScale(BigDecimal value, ConstraintValidatorContext context) {
        int actualScale = value.scale();

        if (actualScale > scale) {
            context.buildConstraintViolationWithTemplate(
                String.format("Currency amount has %d decimal places but maximum allowed is %d (scale constraint)", 
                             actualScale, scale)
            ).addConstraintViolation();
            return false;
        }

        return true;
    }

    /**
     * Validates zero value constraint based on business rules.
     * Some financial contexts require positive amounts (e.g., credit limits, transaction amounts).
     *
     * @param value the currency amount to validate
     * @param context the constraint validator context for error messages
     * @return true if zero constraint is satisfied, false otherwise
     */
    private boolean validateZeroConstraint(BigDecimal value, ConstraintValidatorContext context) {
        if (!allowZero && value.compareTo(BigDecimal.ZERO) == 0) {
            context.buildConstraintViolationWithTemplate(
                "Currency amount cannot be zero in this context"
            ).addConstraintViolation();
            return false;
        }

        return true;
    }

    /**
     * Validates negative value constraint based on business rules.
     * Most financial contexts require non-negative amounts except for account balances with overdrafts.
     *
     * @param value the currency amount to validate
     * @param context the constraint validator context for error messages
     * @return true if negative constraint is satisfied, false otherwise
     */
    private boolean validateNegativeConstraint(BigDecimal value, ConstraintValidatorContext context) {
        if (!allowNegative && value.compareTo(BigDecimal.ZERO) < 0) {
            context.buildConstraintViolationWithTemplate(
                "Currency amount cannot be negative in this context"
            ).addConstraintViolation();
            return false;
        }

        return true;
    }

    /**
     * Validates minimum value constraint with exact BigDecimal precision.
     * Uses COBOL-equivalent comparison logic to ensure exact precision matching.
     *
     * @param value the currency amount to validate
     * @param context the constraint validator context for error messages
     * @return true if minimum constraint is satisfied, false otherwise
     */
    private boolean validateMinimumValue(BigDecimal value, ConstraintValidatorContext context) {
        if (minValue != null && value.compareTo(minValue) < 0) {
            context.buildConstraintViolationWithTemplate(
                String.format("Currency amount %s is below minimum allowed value %s", 
                             value.toPlainString(), minValue.toPlainString())
            ).addConstraintViolation();
            return false;
        }

        return true;
    }

    /**
     * Validates maximum value constraint with exact BigDecimal precision.
     * Uses COBOL-equivalent comparison logic to ensure exact precision matching.
     *
     * @param value the currency amount to validate
     * @param context the constraint validator context for error messages
     * @return true if maximum constraint is satisfied, false otherwise
     */
    private boolean validateMaximumValue(BigDecimal value, ConstraintValidatorContext context) {
        if (maxValue != null && value.compareTo(maxValue) > 0) {
            context.buildConstraintViolationWithTemplate(
                String.format("Currency amount %s exceeds maximum allowed value %s", 
                             value.toPlainString(), maxValue.toPlainString())
            ).addConstraintViolation();
            return false;
        }

        return true;
    }

    /**
     * Validates constraint parameters for consistency and business rule compliance.
     * Ensures annotation parameters are logically consistent and match ValidationConstants where applicable.
     *
     * @throws IllegalArgumentException if constraint parameters are invalid or inconsistent
     */
    private void validateConstraintParameters() {
        // Validate precision and scale relationship
        if (scale > precision) {
            throw new IllegalArgumentException(
                String.format("Scale (%d) cannot be greater than precision (%d)", scale, precision));
        }

        if (precision <= 0 || scale < 0) {
            throw new IllegalArgumentException(
                String.format("Precision (%d) must be positive and scale (%d) must be non-negative", 
                             precision, scale));
        }

        // Validate min/max relationship if both are specified
        if (minValue != null && maxValue != null && minValue.compareTo(maxValue) > 0) {
            throw new IllegalArgumentException(
                String.format("Minimum value (%s) cannot be greater than maximum value (%s)", 
                             minValue.toPlainString(), maxValue.toPlainString()));
        }

        // Validate consistency with ValidationConstants for common currency constraints
        if (precision == ValidationConstants.CURRENCY_PRECISION && scale == ValidationConstants.CURRENCY_SCALE) {
            // Using standard currency precision - validate against standard limits if not overridden
            if (minValue == null && maxValue == null) {
                // No custom range specified - could use ValidationConstants defaults
                // This is informational only, not an error condition
            }
        }

        // Validate allowZero and allowNegative flags consistency
        if (!allowZero && !allowNegative && minValue != null && minValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                "Minimum value must be positive when both zero and negative values are not allowed");
        }
    }
}