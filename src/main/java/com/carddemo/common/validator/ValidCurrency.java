/*
 * Copyright 2024 CardDemo Application
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;

/**
 * Jakarta Bean Validation annotation for currency amount validation with BigDecimal precision.
 * 
 * <p>This annotation ensures monetary fields maintain exact financial precision equivalent to 
 * COBOL COMP-3 calculations while supporting configurable range validation. It validates that
 * BigDecimal values conform to DECIMAL(12,2) format specifications with precise financial 
 * arithmetic capabilities.</p>
 *
 * <p><strong>COBOL COMP-3 Equivalence:</strong></p>
 * <ul>
 *   <li>COBOL PIC S9(10)V99 COMP-3 → PostgreSQL DECIMAL(12,2) → Java BigDecimal</li>
 *   <li>Maintains exact precision for financial calculations without floating-point errors</li>
 *   <li>Supports arithmetic operations with MathContext.DECIMAL128 precision</li>
 *   <li>Handles signed monetary values with proper range validation</li>
 * </ul>
 *
 * <p><strong>Usage Examples:</strong></p>
 * <pre>
 * {@code
 * public class Account {
 *     @ValidCurrency
 *     private BigDecimal currentBalance;
 *     
 *     @ValidCurrency(min = "0.00", max = "999999.99")
 *     private BigDecimal creditLimit;
 *     
 *     @ValidCurrency(min = "-9999999999.99", max = "9999999999.99", 
 *                   message = "Transaction amount must be within valid range")
 *     private BigDecimal transactionAmount;
 * }
 * }
 * </pre>
 *
 * <p><strong>Validation Rules:</strong></p>
 * <ul>
 *   <li><strong>Precision:</strong> Maximum 12 total digits with exactly 2 decimal places</li>
 *   <li><strong>Range:</strong> Configurable minimum and maximum values with BigDecimal precision</li>
 *   <li><strong>Scale:</strong> Exactly 2 decimal places for monetary amounts</li>
 *   <li><strong>Null Handling:</strong> Null values are considered valid (use @NotNull for required fields)</li>
 * </ul>
 *
 * <p><strong>Integration Points:</strong></p>
 * <ul>
 *   <li>Spring Boot validation framework integration</li>
 *   <li>PostgreSQL DECIMAL(12,2) column mapping</li>
 *   <li>JPA entity field validation</li>
 *   <li>REST API request/response validation</li>
 *   <li>Form input validation in React frontend</li>
 * </ul>
 *
 * <p><strong>Error Message Customization:</strong></p>
 * <p>The default validation message can be overridden using the {@code message} attribute 
 * or by defining custom messages in ValidationMessages.properties:</p>
 * <pre>
 * com.carddemo.common.validator.ValidCurrency.message = Custom currency validation message
 * </pre>
 *
 * <p><strong>Performance Considerations:</strong></p>
 * <ul>
 *   <li>Validation performed using BigDecimal arithmetic for exact precision</li>
 *   <li>Efficient range checking with configurable bounds</li>
 *   <li>Thread-safe validation logic suitable for concurrent operations</li>
 *   <li>Optimized for high-volume transaction processing (&gt;10,000 TPS)</li>
 * </ul>
 *
 * @author CardDemo Development Team
 * @since 1.0
 * @see BigDecimal
 * @see jakarta.validation.constraints.Digits
 * @see jakarta.validation.constraints.DecimalMin
 * @see jakarta.validation.constraints.DecimalMax
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CurrencyValidator.class)
@Documented
@ReportAsSingleViolation
public @interface ValidCurrency {
    
    /**
     * Default validation error message.
     * 
     * <p>This message supports parameter substitution for dynamic error messages:</p>
     * <ul>
     *   <li>{min} - Minimum allowed value</li>
     *   <li>{max} - Maximum allowed value</li>
     *   <li>{value} - Actual value being validated</li>
     * </ul>
     * 
     * @return the error message template
     */
    String message() default "{com.carddemo.common.validator.ValidCurrency.message}";
    
    /**
     * Validation groups for conditional validation scenarios.
     * 
     * <p>Groups allow for context-specific validation rules:</p>
     * <ul>
     *   <li>Creation group for new entity validation</li>
     *   <li>Update group for existing entity modifications</li>
     *   <li>Transaction group for payment processing validation</li>
     * </ul>
     * 
     * @return the validation groups
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for carrying additional metadata about the validation constraint.
     * 
     * <p>Payload can be used for:</p>
     * <ul>
     *   <li>Severity levels (WARNING, ERROR, CRITICAL)</li>
     *   <li>Business rule identifiers</li>
     *   <li>Audit trail information</li>
     *   <li>Error recovery suggestions</li>
     * </ul>
     * 
     * @return the payload classes
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Minimum allowed currency value.
     * 
     * <p>String representation of BigDecimal value supporting:</p>
     * <ul>
     *   <li>Negative values for debit transactions: "-9999999999.99"</li>
     *   <li>Zero values for balance validation: "0.00"</li>
     *   <li>Positive values for credit limits: "999999.99"</li>
     *   <li>Scientific notation: "1.23E+4" (converts to 12300.00)</li>
     * </ul>
     * 
     * <p><strong>COBOL Equivalence:</strong> Maps to COBOL PIC S9(10)V99 COMP-3 minimum value</p>
     * 
     * @return the minimum value as string representation
     */
    String min() default "-9999999999.99";
    
    /**
     * Maximum allowed currency value.
     * 
     * <p>String representation of BigDecimal value supporting:</p>
     * <ul>
     *   <li>Maximum credit limit: "999999.99"</li>
     *   <li>Transaction amount ceiling: "99999.99"</li>
     *   <li>Balance upper bound: "9999999999.99"</li>
     *   <li>Scientific notation: "9.99E+9" (converts to 9990000000.00)</li>
     * </ul>
     * 
     * <p><strong>COBOL Equivalence:</strong> Maps to COBOL PIC S9(10)V99 COMP-3 maximum value</p>
     * 
     * @return the maximum value as string representation
     */
    String max() default "9999999999.99";
    
    /**
     * Whether to allow null values in validation.
     * 
     * <p>Null value handling strategies:</p>
     * <ul>
     *   <li>{@code true} - Null values pass validation (default)</li>
     *   <li>{@code false} - Null values fail validation</li>
     * </ul>
     * 
     * <p><strong>Best Practice:</strong> Use @NotNull annotation separately for required 
     * field validation to maintain clear validation semantics.</p>
     * 
     * @return true if null values are allowed, false otherwise
     */
    boolean allowNull() default true;
    
    /**
     * Whether to enforce exactly 2 decimal places for monetary precision.
     * 
     * <p>Decimal precision enforcement:</p>
     * <ul>
     *   <li>{@code true} - Requires exactly 2 decimal places (default)</li>
     *   <li>{@code false} - Allows 0-2 decimal places</li>
     * </ul>
     * 
     * <p><strong>COBOL Equivalence:</strong> COMP-3 fields always maintain 2 decimal places</p>
     * 
     * @return true if exactly 2 decimal places are required, false otherwise
     */
    boolean requireExactScale() default true;
    
    /**
     * Custom validation context for business-specific validation rules.
     * 
     * <p>Context-specific validation scenarios:</p>
     * <ul>
     *   <li>"ACCOUNT_BALANCE" - Account balance validation rules</li>
     *   <li>"CREDIT_LIMIT" - Credit limit validation rules</li>
     *   <li>"TRANSACTION_AMOUNT" - Transaction amount validation rules</li>
     *   <li>"INTEREST_RATE" - Interest rate validation (typically DECIMAL(5,4))</li>
     * </ul>
     * 
     * <p>The validation context allows for customized business rules and error messages
     * based on the specific use case of the currency field.</p>
     * 
     * @return the validation context identifier
     */
    String context() default "DEFAULT";
    
    /**
     * List of acceptable currencies for multi-currency validation.
     * 
     * <p>Currency code validation:</p>
     * <ul>
     *   <li>ISO 4217 currency codes: "USD", "EUR", "GBP"</li>
     *   <li>Empty array allows all currencies (default)</li>
     *   <li>Single currency for dedicated currency systems</li>
     * </ul>
     * 
     * <p><strong>CardDemo Context:</strong> Currently focused on USD transactions only</p>
     * 
     * @return the array of acceptable currency codes
     */
    String[] acceptableCurrencies() default {};
    
    /**
     * Whether to apply strict COBOL COMP-3 validation rules.
     * 
     * <p>COBOL COMP-3 strict validation includes:</p>
     * <ul>
     *   <li>Exact precision matching: 12 total digits, 2 decimal places</li>
     *   <li>Range validation: -9999999999.99 to +9999999999.99</li>
     *   <li>Scale enforcement: Exactly 2 decimal places required</li>
     *   <li>Arithmetic validation: MathContext.DECIMAL128 precision</li>
     * </ul>
     * 
     * <p><strong>Performance Impact:</strong> Strict validation may have slight performance
     * overhead but ensures complete COBOL equivalence.</p>
     * 
     * @return true if strict COBOL COMP-3 validation is required, false otherwise
     */
    boolean strictCobolCompliance() default true;
}