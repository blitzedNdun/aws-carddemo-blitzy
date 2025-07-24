/*
 * ValidCurrency.java
 * 
 * Jakarta Bean Validation annotation for currency amount validation with BigDecimal precision.
 * Ensures monetary fields maintain exact financial precision equivalent to COBOL COMP-3 calculations
 * while supporting configurable range validation for different currency contexts.
 * 
 * This annotation validates:
 * - DECIMAL(12,2) format validation with exact financial precision matching COBOL S9(10)V99
 * - Configurable minimum/maximum values for different monetary contexts
 * - BigDecimal precision requirements maintaining COBOL COMP-3 arithmetic equivalence
 * - Range validation for account balances, credit limits, and transaction amounts
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

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Jakarta Bean Validation annotation for validating currency amounts with exact BigDecimal precision.
 * 
 * This annotation ensures that monetary fields maintain exact financial precision equivalent to 
 * COBOL COMP-3 calculations while providing comprehensive validation for different currency contexts.
 * 
 * <p><strong>Precision Requirements:</strong></p>
 * <ul>
 *   <li>Supports DECIMAL(12,2) format by default matching COBOL S9(10)V99 pattern</li>
 *   <li>Configurable precision and scale for different monetary contexts</li>
 *   <li>Maintains BigDecimal arithmetic precision equivalent to COBOL COMP-3 fields</li>
 *   <li>Supports exact financial calculations without floating-point precision errors</li>
 * </ul>
 * 
 * <p><strong>Usage Examples:</strong></p>
 * <pre>
 * // Default account balance validation (DECIMAL 12,2 precision)
 * &#64;ValidCurrency
 * private BigDecimal accountBalance;
 * 
 * // Credit limit validation with custom range
 * &#64;ValidCurrency(min = "0.00", max = "999999.99", message = "Credit limit must be between $0.00 and $999,999.99")
 * private BigDecimal creditLimit;
 * 
 * // Transaction amount validation with specific precision
 * &#64;ValidCurrency(precision = 11, scale = 2, min = "0.01", max = "99999999.99")
 * private BigDecimal transactionAmount;
 * 
 * // Interest rate validation with high precision
 * &#64;ValidCurrency(precision = 5, scale = 4, min = "0.0001", max = "9.9999")
 * private BigDecimal interestRate;
 * </pre>
 * 
 * <p><strong>COBOL COMP-3 Mapping:</strong></p>
 * <ul>
 *   <li>COBOL S9(10)V99 COMP-3 → DECIMAL(12,2) → precision=12, scale=2</li>
 *   <li>COBOL S9(09)V99 COMP-3 → DECIMAL(11,2) → precision=11, scale=2</li>
 *   <li>COBOL S9(03)V9999 COMP-3 → DECIMAL(5,4) → precision=5, scale=4</li>
 * </ul>
 * 
 * <p><strong>Validation Rules:</strong></p>
 * <ul>
 *   <li>Null values are considered valid (use @NotNull for null validation)</li>
 *   <li>Values must be within the specified min/max range if provided</li>
 *   <li>Scale must not exceed the configured maximum decimal places</li>
 *   <li>Precision must not exceed the configured maximum total digits</li>
 *   <li>Only BigDecimal types are supported for exact precision arithmetic</li>
 * </ul>
 *
 * @author AWS CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0-15-g27d6c6f-68
 * @see CurrencyValidator
 * @see jakarta.validation.constraints.DecimalMin
 * @see jakarta.validation.constraints.DecimalMax
 * @see jakarta.validation.constraints.Digits
 */
@Documented
@Constraint(validatedBy = CurrencyValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCurrency {
    
    /**
     * The error message to display when validation fails.
     * Supports parameterized messages with validation attribute values.
     * 
     * @return the error message template
     */
    String message() default "Currency amount must be a valid monetary value with proper precision";
    
    /**
     * Groups for validation group support in Jakarta Bean Validation.
     * Allows different validation contexts (e.g., account creation vs. transaction processing).
     * 
     * @return the validation groups
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for carrying additional metadata about the validation constraint.
     * Can be used for severity levels, error codes, or business rule references.
     * 
     * @return the payload classes
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Maximum number of total digits allowed in the currency amount.
     * Defaults to 12 digits to support DECIMAL(12,2) format matching COBOL S9(10)V99.
     * 
     * <p><strong>Common Precision Values:</strong></p>
     * <ul>
     *   <li>12 - Account balances and credit limits (up to $9,999,999,999.99)</li>
     *   <li>11 - Transaction amounts (up to $999,999,999.99)</li>
     *   <li>5 - Interest rates as percentages (up to 9.9999%)</li>
     * </ul>
     * 
     * @return the maximum total number of digits
     */
    int precision() default 12;
    
    /**
     * Number of decimal places allowed after the decimal point.
     * Defaults to 2 decimal places for standard currency formatting.
     * 
     * <p><strong>Common Scale Values:</strong></p>
     * <ul>
     *   <li>2 - Standard currency amounts (dollars and cents)</li>
     *   <li>4 - Interest rates and percentage calculations</li>
     *   <li>0 - Whole number amounts (rare in financial applications)</li>
     * </ul>
     * 
     * @return the number of decimal places
     */
    int scale() default 2;
    
    /**
     * Minimum allowed value for the currency amount (inclusive).
     * Must be specified as a string to maintain exact precision.
     * Empty string means no minimum constraint.
     * 
     * <p><strong>Example Values:</strong></p>
     * <ul>
     *   <li>"0.00" - Non-negative amounts only</li>
     *   <li>"0.01" - Positive amounts only (no zero)</li>
     *   <li>"-9999999999.99" - Allow negative balances</li>
     * </ul>
     * 
     * @return the minimum value as a string
     */
    String min() default "";
    
    /**
     * Maximum allowed value for the currency amount (inclusive).
     * Must be specified as a string to maintain exact precision.
     * Empty string means no maximum constraint.
     * 
     * <p><strong>Example Values:</strong></p>
     * <ul>
     *   <li>"9999999999.99" - Standard account balance limit</li>
     *   <li>"999999999.99" - Transaction amount limit</li>
     *   <li>"9.9999" - Interest rate percentage limit</li>
     * </ul>
     * 
     * @return the maximum value as a string
     */
    String max() default "";
    
    /**
     * Flag to indicate if zero values should be allowed.
     * Defaults to true to allow zero balances and amounts.
     * Set to false for contexts where zero is not a valid business value.
     * 
     * <p><strong>Business Use Cases:</strong></p>
     * <ul>
     *   <li>true - Account balances (can be zero)</li>
     *   <li>false - Credit limits (must be positive)</li>
     *   <li>false - Transaction amounts (must be positive)</li>
     * </ul>
     * 
     * @return true if zero values are allowed, false otherwise
     */
    boolean allowZero() default true;
    
    /**
     * Flag to indicate if negative values should be allowed.
     * Defaults to false for standard currency validation.
     * Set to true for contexts where negative values represent debts or refunds.
     * 
     * <p><strong>Business Use Cases:</strong></p>
     * <ul>
     *   <li>true - Account balances (can be negative for overdrafts)</li>
     *   <li>false - Credit limits (must be non-negative)</li>
     *   <li>false - Transaction amounts (typically positive)</li>
     * </ul>
     * 
     * @return true if negative values are allowed, false otherwise
     */
    boolean allowNegative() default false;
}