/*
 * ValidCurrencyValidator.java
 * 
 * Jakarta Bean Validation implementation for ValidCurrency annotation.
 * Provides comprehensive currency amount validation with BigDecimal precision maintenance
 * equivalent to COBOL COMP-3 calculations for financial data integrity.
 * 
 * This validator implements:
 * - Exact BigDecimal precision validation matching COBOL S9(n)V99 patterns
 * - Configurable range validation with proper mathematical context
 * - Scale and precision checking for different monetary contexts
 * - Business rule validation for zero and negative value handling
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
import java.math.RoundingMode;

/**
 * Jakarta Bean Validation implementation for validating currency amounts with exact BigDecimal precision.
 * 
 * This validator ensures that monetary fields maintain exact financial precision equivalent to 
 * COBOL COMP-3 calculations, preventing floating-point precision errors in critical financial operations.
 * 
 * <p><strong>Mathematical Context Configuration:</strong></p>
 * The validator uses MathContext.DECIMAL128 with RoundingMode.HALF_EVEN to ensure:
 * <ul>
 *   <li>Exact precision equivalent to COBOL COMP-3 packed decimal arithmetic</li>
 *   <li>Consistent rounding behavior matching mainframe financial calculations</li>
 *   <li>128-bit decimal precision supporting the largest financial amounts</li>
 *   <li>IEEE 754 decimal arithmetic compliance for cross-platform consistency</li>
 * </ul>
 * 
 * <p><strong>Validation Algorithm:</strong></p>
 * <ol>
 *   <li>Null value handling (null values pass validation)</li>
 *   <li>Type verification (only BigDecimal values are accepted)</li>
 *   <li>Zero value validation based on allowZero flag</li>
 *   <li>Negative value validation based on allowNegative flag</li>
 *   <li>Scale validation against configured decimal places</li>
 *   <li>Precision validation against configured total digits</li>
 *   <li>Range validation against configured min/max values</li>
 * </ol>
 * 
 * <p><strong>Error Scenarios:</strong></p>
 * <ul>
 *   <li>Value exceeds maximum precision (total digits)</li>
 *   <li>Value exceeds maximum scale (decimal places)</li>
 *   <li>Value below minimum threshold</li>
 *   <li>Value above maximum threshold</li>
 *   <li>Zero value when not allowed</li>
 *   <li>Negative value when not allowed</li>
 *   <li>Non-BigDecimal type provided</li>
 * </ul>
 * 
 * @author AWS CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0-15-g27d6c6f-68
 * @see ValidCurrency
 * @see jakarta.validation.ConstraintValidator
 */
public class ValidCurrencyValidator implements ConstraintValidator<ValidCurrency, BigDecimal> {
    
    /**
     * Mathematical context for exact financial calculations.
     * Uses DECIMAL128 precision with HALF_EVEN rounding to maintain
     * COBOL COMP-3 arithmetic equivalence and prevent precision errors.
     */
    private static final MathContext FINANCIAL_MATH_CONTEXT = MathContext.DECIMAL128;
    
    /**
     * Rounding mode for consistent financial calculations.
     * HALF_EVEN (banker's rounding) provides the most mathematically
     * unbiased rounding behavior for financial applications.
     */
    private static final RoundingMode FINANCIAL_ROUNDING_MODE = RoundingMode.HALF_EVEN;
    
    // Annotation attribute storage
    private int precision;
    private int scale;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private boolean allowZero;
    private boolean allowNegative;
    private boolean hasMinValue;
    private boolean hasMaxValue;
    
    /**
     * Initializes the validator with configuration from the ValidCurrency annotation.
     * 
     * @param constraintAnnotation the ValidCurrency annotation instance with configuration
     */
    @Override
    public void initialize(ValidCurrency constraintAnnotation) {
        this.precision = constraintAnnotation.precision();
        this.scale = constraintAnnotation.scale();
        this.allowZero = constraintAnnotation.allowZero();
        this.allowNegative = constraintAnnotation.allowNegative();
        
        // Parse minimum value with exact precision
        String minStr = constraintAnnotation.min().trim();
        if (!minStr.isEmpty()) {
            try {
                this.minValue = new BigDecimal(minStr, FINANCIAL_MATH_CONTEXT);
                this.hasMinValue = true;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid minimum value format: " + minStr, e);
            }
        } else {
            this.hasMinValue = false;
        }
        
        // Parse maximum value with exact precision
        String maxStr = constraintAnnotation.max().trim();
        if (!maxStr.isEmpty()) {
            try {
                this.maxValue = new BigDecimal(maxStr, FINANCIAL_MATH_CONTEXT);
                this.hasMaxValue = true;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid maximum value format: " + maxStr, e);
            }
        } else {
            this.hasMaxValue = false;
        }
        
        // Validate configuration consistency
        validateConfiguration();
    }
    
    /**
     * Validates configuration parameters for consistency and business logic correctness.
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    private void validateConfiguration() {
        // Validate precision and scale relationship
        if (scale < 0) {
            throw new IllegalArgumentException("Scale cannot be negative: " + scale);
        }
        if (precision < 1) {
            throw new IllegalArgumentException("Precision must be at least 1: " + precision);
        }
        if (scale > precision) {
            throw new IllegalArgumentException("Scale (" + scale + ") cannot exceed precision (" + precision + ")");
        }
        
        // Validate min/max relationship
        if (hasMinValue && hasMaxValue) {
            if (minValue.compareTo(maxValue) > 0) {
                throw new IllegalArgumentException("Minimum value (" + minValue + ") cannot exceed maximum value (" + maxValue + ")");
            }
        }
        
        // Validate zero constraint consistency
        BigDecimal zero = BigDecimal.ZERO;
        if (!allowZero) {
            if (hasMinValue && minValue.compareTo(zero) <= 0 && hasMaxValue && maxValue.compareTo(zero) >= 0) {
                throw new IllegalArgumentException("Zero is not allowed but is within the specified range [" + minValue + ", " + maxValue + "]");
            }
        }
        
        // Validate negative constraint consistency
        if (!allowNegative && hasMinValue && minValue.compareTo(zero) < 0) {
            throw new IllegalArgumentException("Negative values are not allowed but minimum value is negative: " + minValue);
        }
    }
    
    /**
     * Validates a BigDecimal currency amount against all configured constraints.
     * 
     * @param value the BigDecimal value to validate (can be null)
     * @param context the validation context for error message customization
     * @return true if the value is valid, false otherwise
     */
    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        // Null values are considered valid - use @NotNull for null validation
        if (value == null) {
            return true;
        }
        
        // Prepare for custom error messages
        context.disableDefaultConstraintViolation();
        
        // Validate zero constraint
        if (!allowZero && value.compareTo(BigDecimal.ZERO) == 0) {
            addViolation(context, "Zero values are not allowed for this currency field");
            return false;
        }
        
        // Validate negative constraint
        if (!allowNegative && value.compareTo(BigDecimal.ZERO) < 0) {
            addViolation(context, "Negative values are not allowed for this currency field");
            return false;
        }
        
        // Validate scale (decimal places)
        if (value.scale() > scale) {
            addViolation(context, String.format(
                "Currency amount has too many decimal places: %d (maximum allowed: %d)", 
                value.scale(), scale));
            return false;
        }
        
        // Validate precision (total digits)
        if (!validatePrecision(value)) {
            addViolation(context, String.format(
                "Currency amount exceeds maximum precision: %d total digits allowed", 
                precision));
            return false;
        }
        
        // Validate minimum value constraint
        if (hasMinValue && value.compareTo(minValue) < 0) {
            addViolation(context, String.format(
                "Currency amount %s is below minimum allowed value: %s", 
                value.toPlainString(), minValue.toPlainString()));
            return false;
        }
        
        // Validate maximum value constraint
        if (hasMaxValue && value.compareTo(maxValue) > 0) {
            addViolation(context, String.format(
                "Currency amount %s exceeds maximum allowed value: %s", 
                value.toPlainString(), maxValue.toPlainString()));
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates that the BigDecimal value does not exceed the configured precision.
     * 
     * Precision validation considers the total number of significant digits,
     * matching COBOL COMP-3 field precision requirements.
     * 
     * @param value the BigDecimal value to check
     * @return true if the value fits within the precision limit, false otherwise
     */
    private boolean validatePrecision(BigDecimal value) {
        // Convert to plain string to count actual digits
        String plainString = value.abs().toPlainString();
        
        // Remove decimal point for digit counting
        String digitsOnly = plainString.replace(".", "");
        
        // Remove leading zeros to count significant digits only
        String significantDigits = digitsOnly.replaceFirst("^0+", "");
        
        // Handle edge case where value is zero
        if (significantDigits.isEmpty()) {
            significantDigits = "0";
        }
        
        return significantDigits.length() <= precision;
    }
    
    /**
     * Adds a custom validation error message to the constraint validator context.
     * 
     * @param context the validation context
     * @param message the custom error message
     */
    private void addViolation(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
    }
    
    /**
     * Utility method to create a BigDecimal with proper financial precision context.
     * 
     * This method ensures that BigDecimal values are created with the same
     * mathematical context used by the validator, maintaining consistency
     * across the application.
     * 
     * @param value the string representation of the decimal value
     * @return a BigDecimal with proper financial precision context
     * @throws NumberFormatException if the string cannot be parsed as a decimal
     */
    public static BigDecimal createFinancialBigDecimal(String value) {
        return new BigDecimal(value, FINANCIAL_MATH_CONTEXT);
    }
    
    /**
     * Utility method to perform financial calculations with proper rounding.
     * 
     * This method provides a consistent way to perform arithmetic operations
     * on currency amounts while maintaining COBOL COMP-3 equivalent precision.
     * 
     * @param operand1 the first operand
     * @param operand2 the second operand
     * @param operation the arithmetic operation to perform
     * @return the result of the calculation with proper financial precision
     */
    public static BigDecimal performFinancialCalculation(BigDecimal operand1, BigDecimal operand2, ArithmeticOperation operation) {
        if (operand1 == null || operand2 == null) {
            throw new IllegalArgumentException("Operands cannot be null for financial calculations");
        }
        
        return switch (operation) {
            case ADD -> operand1.add(operand2, FINANCIAL_MATH_CONTEXT);
            case SUBTRACT -> operand1.subtract(operand2, FINANCIAL_MATH_CONTEXT);
            case MULTIPLY -> operand1.multiply(operand2, FINANCIAL_MATH_CONTEXT);
            case DIVIDE -> operand1.divide(operand2, FINANCIAL_MATH_CONTEXT);
        };
    }
    
    /**
     * Enumeration of supported arithmetic operations for financial calculations.
     */
    public enum ArithmeticOperation {
        ADD, SUBTRACT, MULTIPLY, DIVIDE
    }
}