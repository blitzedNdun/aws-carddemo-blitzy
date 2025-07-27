/*
 * PaymentAmountValidator.java
 * 
 * CardDemo Application
 * 
 * Jakarta Bean Validation implementation for bill payment amount validation with BigDecimal 
 * precision and COBOL-equivalent business rule enforcement. Validates payment amounts using 
 * exact decimal precision with configurable range validation and bill payment specific 
 * business rules converted from COBOL program COBIL00C.cbl.
 * 
 * Implements validation logic equivalent to COBOL business rules:
 * - ACCT-CURR-BAL <= ZEROS validation preventing payments on zero/negative balances
 * - TRAN-AMT PIC S9(09)V99 precision requirements for financial accuracy
 * - "You have nothing to pay..." business rule messaging for invalid payment scenarios
 * 
 * Converted from COBOL program COBIL00C.cbl bill payment validation logic:
 * - Line 198-205: Account balance validation preventing zero/negative balance payments
 * - Line 224: Transaction amount assignment (MOVE ACCT-CURR-BAL TO TRAN-AMT)
 * - CVTRA05Y.cpy line 10: TRAN-AMT PIC S9(09)V99 precision specification
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
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

import com.carddemo.common.validator.ValidPaymentAmount;
import com.carddemo.common.validator.ValidationConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Jakarta Bean Validation implementation for validating bill payment amounts with BigDecimal 
 * precision and business rule enforcement. Ensures payment amounts meet CardDemo bill payment 
 * specific requirements and maintain financial precision standards equivalent to COBOL COMP-3 
 * arithmetic operations.
 * 
 * <p>This validator implements the business logic extracted from the original COBOL bill payment 
 * program COBIL00C.cbl, specifically the validation rules that prevent bill payments when account 
 * balances are zero or negative, while maintaining exact financial precision through BigDecimal 
 * arithmetic operations.</p>
 * 
 * <h3>COBOL Business Rule Implementation</h3>
 * <p>The validator enforces the following business rules converted from COBOL:</p>
 * 
 * <ul>
 *   <li><strong>Minimum Payment Validation</strong>: Equivalent to COBOL condition 
 *       {@code IF ACCT-CURR-BAL <= ZEROS} from COBIL00C.cbl line 198-205</li>
 *   <li><strong>Maximum Amount Constraint</strong>: Enforces COBOL COMP-3 field limit 
 *       {@code PIC S9(09)V99} = 999,999,999.99 from CVTRA05Y.cpy line 10</li>
 *   <li><strong>Precision Requirements</strong>: Maintains exactly 2 decimal places for 
 *       financial accuracy matching COBOL V99 specification</li>
 *   <li><strong>Error Messaging</strong>: Replicates COBOL error message 
 *       "You have nothing to pay..." for invalid payment scenarios</li>
 * </ul>
 * 
 * <h3>BigDecimal Precision Management</h3>
 * <p>All arithmetic operations utilize BigDecimal with ValidationConstants.COBOL_ROUNDING_MODE 
 * to ensure exact precision compatibility with original COBOL COMP-3 calculations. The 
 * validator prevents floating-point precision errors that could occur with double or float 
 * arithmetic operations.</p>
 * 
 * <h3>Validation Process Flow</h3>
 * <p>The validation follows a structured approach:</p>
 * <ol>
 *   <li>Null value validation - ensures payment amount is provided</li>
 *   <li>Minimum amount validation - enforces positive payment amounts</li>
 *   <li>Maximum amount validation - prevents COBOL field overflow</li>
 *   <li>Decimal precision validation - ensures exactly 2 decimal places</li>
 *   <li>Scale verification - confirms financial accuracy requirements</li>
 * </ol>
 * 
 * <h3>Integration with Spring Boot Microservices</h3>
 * <p>This validator integrates seamlessly with Spring Boot's validation framework, providing 
 * automatic validation for REST API endpoints that process bill payment requests. The 
 * {@code @Valid} annotation triggers this validator for any field annotated with 
 * {@code @ValidPaymentAmount}.</p>
 * 
 * <h3>Usage Examples</h3>
 * <pre>
 * // Basic usage with default validation rules
 * public class BillPaymentRequest {
 *     &#64;ValidPaymentAmount
 *     private BigDecimal paymentAmount;
 * }
 * 
 * // Custom minimum amount for specific payment types
 * public class MinimumPaymentRequest {
 *     &#64;ValidPaymentAmount(min = "25.00", minMessage = "Minimum payment is $25.00")
 *     private BigDecimal minimumDue;
 * }
 * 
 * // Enhanced error messaging for user experience
 * public class AccountPayment {
 *     &#64;ValidPaymentAmount(
 *         min = "0.01", 
 *         max = "50000.00",
 *         message = "Payment amount must be between $0.01 and $50,000.00"
 *     )
 *     private BigDecimal amount;
 * }
 * </pre>
 * 
 * <h3>Error Message Customization</h3>
 * <p>The validator supports multiple customizable error messages:</p>
 * <ul>
 *   <li>{@code message} - General validation failure message</li>
 *   <li>{@code minMessage} - Specific message for amounts below minimum</li>
 *   <li>{@code maxMessage} - Specific message for amounts above maximum</li>
 *   <li>{@code precisionMessage} - Message for invalid decimal precision</li>
 *   <li>{@code nullMessage} - Message for null payment amounts</li>
 * </ul>
 * 
 * <h3>Performance Characteristics</h3>
 * <p>The validator is optimized for high-throughput validation scenarios:</p>
 * <ul>
 *   <li>Cached BigDecimal constants for minimum/maximum values</li>
 *   <li>Efficient short-circuit validation logic</li>
 *   <li>Minimal object allocation during validation operations</li>
 *   <li>Thread-safe implementation for concurrent request processing</li>
 * </ul>
 * 
 * @since 1.0.0
 * @see ValidPaymentAmount
 * @see ValidationConstants
 * @see jakarta.validation.ConstraintValidator
 */
public class PaymentAmountValidator implements ConstraintValidator<ValidPaymentAmount, BigDecimal> {

    /**
     * Minimum allowed payment amount as BigDecimal for efficient comparison operations.
     * Initialized during validator initialization from annotation parameters.
     * Uses COBOL-equivalent precision with ValidationConstants.CURRENCY_SCALE decimal places.
     */
    private BigDecimal minAmount;
    
    /**
     * Maximum allowed payment amount as BigDecimal for efficient comparison operations.
     * Initialized during validator initialization from annotation parameters.
     * Defaults to COBOL COMP-3 field limit of 999,999,999.99 (PIC S9(09)V99).
     */
    private BigDecimal maxAmount;
    
    /**
     * Cached ValidPaymentAmount annotation instance for accessing custom error messages
     * during validation failure scenarios. Provides context-specific error messaging
     * based on validation failure type (null, minimum, maximum, precision).
     */
    private ValidPaymentAmount annotation;
    
    /**
     * MathContext for BigDecimal operations ensuring COBOL-equivalent precision.
     * Uses ValidationConstants.COBOL_ROUNDING_MODE to maintain exact compatibility
     * with original COBOL COMP-3 arithmetic behavior and prevent precision loss.
     */
    private static final MathContext COBOL_MATH_CONTEXT = new MathContext(
        ValidationConstants.CURRENCY_PRECISION, 
        ValidationConstants.COBOL_ROUNDING_MODE
    );

    /**
     * Initializes the validator with configuration parameters from the ValidPaymentAmount 
     * annotation. Converts string-based minimum and maximum values to BigDecimal instances 
     * for efficient runtime comparison operations.
     * 
     * <p>This method is called once per validator instance by the Jakarta Bean Validation 
     * framework during constraint validator initialization. It pre-computes BigDecimal 
     * values to avoid repeated string parsing during validation operations.</p>
     * 
     * <p><strong>COBOL Precision Conversion</strong>: String values are converted to 
     * BigDecimal using the COBOL-equivalent MathContext to ensure exact precision 
     * matching. This prevents floating-point representation issues that could occur 
     * with direct double-to-BigDecimal conversion.</p>
     * 
     * <p><strong>Validation Rule Configuration</strong>:</p>
     * <ul>
     *   <li>Minimum amount defaults to "0.01" enforcing positive payment amounts</li>
     *   <li>Maximum amount defaults to "999999999.99" matching COBOL S9(09)V99 limit</li>
     *   <li>BigDecimal scale verification ensures financial precision requirements</li>
     *   <li>Annotation reference cached for custom error message access</li>
     * </ul>
     * 
     * <p><strong>Error Handling</strong>: Invalid annotation parameter values (non-numeric 
     * min/max strings) will cause NumberFormatException during initialization, preventing 
     * validator instantiation and ensuring fail-fast behavior for configuration errors.</p>
     * 
     * @param constraintAnnotation the ValidPaymentAmount annotation instance containing 
     *                           validation configuration parameters including min/max values 
     *                           and custom error messages
     * 
     * @throws NumberFormatException if annotation min or max parameters contain invalid 
     *                              numeric values that cannot be converted to BigDecimal
     * 
     * @see ValidPaymentAmount#min()
     * @see ValidPaymentAmount#max()
     * @see ValidationConstants#COBOL_ROUNDING_MODE
     */
    @Override
    public void initialize(ValidPaymentAmount constraintAnnotation) {
        this.annotation = constraintAnnotation;
        
        // Convert string-based min/max values to BigDecimal with COBOL-equivalent precision
        // This ensures exact financial arithmetic without floating-point representation errors
        this.minAmount = new BigDecimal(constraintAnnotation.min(), COBOL_MATH_CONTEXT);
        this.maxAmount = new BigDecimal(constraintAnnotation.max(), COBOL_MATH_CONTEXT);
        
        // Validate that configured min/max values have correct decimal scale
        // This prevents configuration errors that could compromise financial precision
        if (this.minAmount.scale() > ValidationConstants.CURRENCY_SCALE) {
            throw new IllegalArgumentException(
                String.format("Minimum amount scale (%d) exceeds CURRENCY_SCALE (%d): %s", 
                    this.minAmount.scale(), ValidationConstants.CURRENCY_SCALE, constraintAnnotation.min())
            );
        }
        
        if (this.maxAmount.scale() > ValidationConstants.CURRENCY_SCALE) {
            throw new IllegalArgumentException(
                String.format("Maximum amount scale (%d) exceeds CURRENCY_SCALE (%d): %s", 
                    this.maxAmount.scale(), ValidationConstants.CURRENCY_SCALE, constraintAnnotation.max())
            );
        }
        
        // Validate logical consistency of min/max range
        if (this.minAmount.compareTo(this.maxAmount) > 0) {
            throw new IllegalArgumentException(
                String.format("Minimum amount (%s) cannot be greater than maximum amount (%s)", 
                    constraintAnnotation.min(), constraintAnnotation.max())
            );
        }
    }

    /**
     * Validates a BigDecimal payment amount according to COBOL-equivalent business rules 
     * and financial precision requirements. Implements comprehensive validation logic 
     * that replicates the original COBOL bill payment validation from COBIL00C.cbl.
     * 
     * <p>This method performs structured validation following the original COBOL program 
     * logic, ensuring that payment amounts meet all business rule requirements while 
     * maintaining exact financial precision through BigDecimal arithmetic operations.</p>
     * 
     * <p><strong>COBOL Business Rule Implementation</strong>:</p>
     * <p>The validation logic directly implements the COBOL condition from COBIL00C.cbl 
     * lines 198-205:</p>
     * <pre>
     * Original COBOL:
     * IF ACCT-CURR-BAL <= ZEROS AND
     *    ACTIDINI OF COBIL0AI NOT = SPACES AND LOW-VALUES
     *     MOVE 'Y'     TO WS-ERR-FLG
     *     MOVE 'You have nothing to pay...' TO WS-MESSAGE
     * </pre>
     * 
     * <p><strong>Validation Process Flow</strong>:</p>
     * <ol>
     *   <li><strong>Null Validation</strong>: Ensures payment amount is provided 
     *       (equivalent to COBOL SPACES/LOW-VALUES check)</li>
     *   <li><strong>Minimum Amount Validation</strong>: Enforces positive payment amounts 
     *       (equivalent to COBOL ACCT-CURR-BAL <= ZEROS check)</li>
     *   <li><strong>Maximum Amount Validation</strong>: Prevents COBOL field overflow 
     *       (enforces PIC S9(09)V99 field limit)</li>
     *   <li><strong>Decimal Precision Validation</strong>: Ensures exactly 2 decimal places 
     *       (maintains COBOL V99 specification)</li>
     *   <li><strong>Scale Verification</strong>: Confirms financial accuracy requirements 
     *       (prevents precision loss in calculations)</li>
     * </ol>
     * 
     * <p><strong>BigDecimal Precision Management</strong>:</p>
     * <p>All comparison operations use BigDecimal.compareTo() to ensure exact precision 
     * matching. The validator maintains COBOL COMP-3 equivalent precision by validating:</p>
     * <ul>
     *   <li>Scale does not exceed ValidationConstants.CURRENCY_SCALE (2 decimal places)</li>
     *   <li>Total precision aligns with ValidationConstants.CURRENCY_PRECISION (12 digits)</li>
     *   <li>Arithmetic operations use ValidationConstants.COBOL_ROUNDING_MODE</li>
     * </ul>
     * 
     * <p><strong>Error Message Generation</strong>:</p>
     * <p>The method generates context-specific error messages based on validation failure 
     * type, replicating COBOL error messaging patterns:</p>
     * <ul>
     *   <li>Null amounts: Uses nullMessage from annotation</li>
     *   <li>Below minimum: Uses minMessage with "You have nothing to pay..." pattern</li>
     *   <li>Above maximum: Uses maxMessage with system limit explanation</li>
     *   <li>Invalid precision: Uses precisionMessage for financial accuracy requirements</li>
     * </ul>
     * 
     * <p><strong>Performance Optimizations</strong>:</p>
     * <ul>
     *   <li>Short-circuit evaluation terminates on first validation failure</li>
     *   <li>Cached BigDecimal instances avoid repeated object allocation</li>
     *   <li>Efficient BigDecimal.compareTo() operations for range checking</li>
     *   <li>Minimal string manipulation during error message generation</li>
     * </ul>
     * 
     * @param value the BigDecimal payment amount to validate, may be null
     * @param context the constraint validator context for error message customization 
     *               and validation state management
     * 
     * @return {@code true} if the payment amount is valid according to all business rules 
     *         and precision requirements, {@code false} if any validation rule fails
     * 
     * @see ValidPaymentAmount#min()
     * @see ValidPaymentAmount#max()
     * @see ValidationConstants#CURRENCY_SCALE
     * @see ValidationConstants#CURRENCY_PRECISION
     */
    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        
        // Disable default constraint violation to provide custom error messages
        context.disableDefaultConstraintViolation();
        
        // COBOL Equivalent: Check for SPACES or LOW-VALUES (null validation)
        // Original COBOL: IF ACTIDINI OF COBIL0AI = SPACES OR LOW-VALUES
        if (value == null) {
            context.buildConstraintViolationWithTemplate(annotation.nullMessage())
                   .addConstraintViolation();
            return false;
        }
        
        // COBOL Equivalent: IF ACCT-CURR-BAL <= ZEROS (minimum amount validation)
        // Original COBOL: Lines 198-205 in COBIL00C.cbl
        // Business Rule: "You have nothing to pay..." for zero/negative amounts
        if (value.compareTo(minAmount) < 0) {
            String errorMessage = annotation.minMessage().replace("${min}", minAmount.toString());
            context.buildConstraintViolationWithTemplate(errorMessage)
                   .addConstraintViolation();
            return false;
        }
        
        // COBOL Equivalent: PIC S9(09)V99 field limit validation (maximum amount)
        // Original COBOL: TRAN-AMT field definition in CVTRA05Y.cpy line 10
        // Prevents arithmetic overflow in COBOL COMP-3 calculations
        if (value.compareTo(maxAmount) > 0) {
            String errorMessage = annotation.maxMessage().replace("${max}", maxAmount.toString());
            context.buildConstraintViolationWithTemplate(errorMessage)
                   .addConstraintViolation();
            return false;
        }
        
        // COBOL Equivalent: V99 precision specification (decimal scale validation)
        // Ensures exactly 2 decimal places for financial accuracy
        // Prevents precision loss in monetary calculations
        if (value.scale() > ValidationConstants.CURRENCY_SCALE) {
            context.buildConstraintViolationWithTemplate(annotation.precisionMessage())
                   .addConstraintViolation();
            return false;
        }
        
        // Additional validation: Ensure the value can be represented within COBOL precision limits
        // This prevents issues when converting between BigDecimal and COBOL COMP-3 fields
        if (value.precision() > ValidationConstants.CURRENCY_PRECISION) {
            String errorMessage = String.format(
                "Payment amount precision (%d digits) exceeds COBOL COMP-3 field limit (%d digits)",
                value.precision(), ValidationConstants.CURRENCY_PRECISION
            );
            context.buildConstraintViolationWithTemplate(errorMessage)
                   .addConstraintViolation();
            return false;
        }
        
        // Validation successful: payment amount meets all COBOL-equivalent business rules
        // and financial precision requirements
        return true;
    }
    
    /**
     * Utility method for validating BigDecimal payment amounts programmatically without 
     * Jakarta Bean Validation framework integration. Provides direct access to validation 
     * logic for custom validation scenarios and testing purposes.
     * 
     * <p>This method enables validation of payment amounts in contexts where annotation-based 
     * validation is not available or appropriate, such as:</p>
     * <ul>
     *   <li>Unit testing with specific validation parameters</li>
     *   <li>Programmatic validation in service layer methods</li>
     *   <li>Custom validation workflows requiring explicit control</li>
     *   <li>Integration with non-Spring validation frameworks</li>
     * </ul>
     * 
     * <p><strong>Usage Example</strong>:</p>
     * <pre>
     * PaymentAmountValidator validator = new PaymentAmountValidator();
     * BigDecimal amount = new BigDecimal("100.00");
     * BigDecimal minAmount = new BigDecimal("0.01");
     * BigDecimal maxAmount = new BigDecimal("999999999.99");
     * 
     * boolean isValid = validator.validateAmount(amount, minAmount, maxAmount);
     * if (!isValid) {
     *     // Handle validation failure
     * }
     * </pre>
     * 
     * @param amount the BigDecimal payment amount to validate
     * @param minAmount the minimum allowed payment amount
     * @param maxAmount the maximum allowed payment amount
     * 
     * @return {@code true} if the payment amount meets all validation requirements, 
     *         {@code false} otherwise
     * 
     * @since 1.0.0
     */
    public static boolean validateAmount(BigDecimal amount, BigDecimal minAmount, BigDecimal maxAmount) {
        if (amount == null) {
            return false;
        }
        
        if (amount.compareTo(minAmount) < 0) {
            return false;
        }
        
        if (amount.compareTo(maxAmount) > 0) {
            return false;
        }
        
        if (amount.scale() > ValidationConstants.CURRENCY_SCALE) {
            return false;
        }
        
        if (amount.precision() > ValidationConstants.CURRENCY_PRECISION) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Utility method for formatting payment amounts with COBOL-equivalent precision for 
     * display purposes. Ensures consistent formatting across all payment amount displays 
     * in the application, maintaining financial accuracy and user experience consistency.
     * 
     * <p>This method applies ValidationConstants.CURRENCY_SCALE formatting to ensure all 
     * displayed payment amounts show exactly 2 decimal places, matching COBOL V99 
     * display format and financial industry standards.</p>
     * 
     * <p><strong>Formatting Rules</strong>:</p>
     * <ul>
     *   <li>Always displays exactly 2 decimal places (e.g., "100.00", not "100")</li>
     *   <li>Uses ValidationConstants.COBOL_ROUNDING_MODE for consistent rounding</li>
     *   <li>Handles null values gracefully by returning "0.00"</li>
     *   <li>Maintains BigDecimal precision without loss of financial accuracy</li>
     * </ul>
     * 
     * <p><strong>Usage Example</strong>:</p>
     * <pre>
     * BigDecimal amount = new BigDecimal("123.5");
     * String formatted = PaymentAmountValidator.formatPaymentAmount(amount);
     * // Returns "123.50"
     * 
     * BigDecimal nullAmount = null;
     * String formattedNull = PaymentAmountValidator.formatPaymentAmount(nullAmount);
     * // Returns "0.00"
     * </pre>
     * 
     * @param amount the BigDecimal payment amount to format, may be null
     * 
     * @return formatted payment amount string with exactly 2 decimal places, 
     *         or "0.00" if amount is null
     * 
     * @since 1.0.0
     */
    public static String formatPaymentAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        
        return amount.setScale(ValidationConstants.CURRENCY_SCALE, ValidationConstants.COBOL_ROUNDING_MODE)
                    .toString();
    }
}