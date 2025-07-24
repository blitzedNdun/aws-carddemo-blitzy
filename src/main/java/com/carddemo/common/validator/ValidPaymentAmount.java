/*
 * ValidPaymentAmount.java
 * 
 * CardDemo Application
 * 
 * Jakarta Bean Validation annotation for bill payment amount validation with BigDecimal precision
 * and business rule enforcement. Ensures payment amounts meet bill payment specific requirements
 * and financial precision standards equivalent to COBOL COMP-3 arithmetic.
 * 
 * Converted from COBOL program COBIL00C.cbl bill payment validation logic:
 * - ACCT-CURR-BAL <= ZEROS validation (line 198)
 * - TRAN-AMT PIC S9(09)V99 precision requirements (CVTRA05Y.cpy line 10)
 * - Business rule: "You have nothing to pay..." for zero balance
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

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Jakarta Bean Validation annotation for validating bill payment amounts with BigDecimal precision
 * and business rule enforcement. Ensures payment amounts meet CardDemo bill payment specific 
 * requirements and maintain financial precision standards equivalent to COBOL COMP-3 arithmetic.
 * 
 * <p>This annotation validates BigDecimal payment amounts according to the business rules extracted
 * from the original COBOL bill payment program COBIL00C.cbl:</p>
 * 
 * <ul>
 *   <li><strong>Minimum Amount</strong>: Payment amount must be greater than zero (equivalent to COBOL: IF ACCT-CURR-BAL <= ZEROS)</li>
 *   <li><strong>Maximum Amount</strong>: Enforces COBOL COMP-3 field limit of S9(09)V99 = 999,999,999.99</li>
 *   <li><strong>Precision Requirements</strong>: Maintains 2 decimal places precision for financial accuracy</li>
 *   <li><strong>Scale Validation</strong>: Ensures no more than 2 decimal places (matching COBOL V99 specification)</li>
 *   <li><strong>Null Handling</strong>: Considers null values as invalid payment amounts</li>
 * </ul>
 * 
 * <p><strong>COBOL Equivalent Validation Logic:</strong></p>
 * <pre>
 * Original COBOL (COBIL00C.cbl lines 198-205):
 * IF ACCT-CURR-BAL <= ZEROS AND
 *    ACTIDINI OF COBIL0AI NOT = SPACES AND LOW-VALUES
 *     MOVE 'Y'     TO WS-ERR-FLG
 *     MOVE 'You have nothing to pay...' TO WS-MESSAGE
 * 
 * TRAN-AMT Field Definition (CVTRA05Y.cpy line 10):
 * 05  TRAN-AMT  PIC S9(09)V99.
 * </pre>
 * 
 * <p><strong>Usage Examples:</strong></p>
 * <pre>
 * // Basic usage with default messages
 * public class BillPaymentRequest {
 *     &#64;ValidPaymentAmount
 *     private BigDecimal paymentAmount;
 * }
 * 
 * // Custom validation message
 * public class AccountPayment {
 *     &#64;ValidPaymentAmount(message = "Payment amount must be a valid positive amount")
 *     private BigDecimal amount;
 * }
 * 
 * // Minimum amount override for specific payment types
 * public class MinimumPayment {
 *     &#64;ValidPaymentAmount(min = "10.00", message = "Minimum payment is $10.00")
 *     private BigDecimal minimumDue;
 * }
 * </pre>
 * 
 * <p><strong>Validation Behavior:</strong></p>
 * <ul>
 *   <li>Validates that payment amounts are positive (greater than 0.00)</li>
 *   <li>Enforces maximum limit of 999,999,999.99 (COBOL S9(09)V99 precision)</li>
 *   <li>Ensures exactly 2 decimal places for financial precision</li>
 *   <li>Provides contextual error messages based on validation failure type</li>
 *   <li>Supports configurable minimum amounts for different payment contexts</li>
 * </ul>
 * 
 * @since 1.0.0
 * @see PaymentAmountValidator
 */
@Documented
@Constraint(validatedBy = PaymentAmountValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPaymentAmount {
    
    /**
     * Default validation message for invalid payment amounts.
     * This message is displayed when the payment amount fails validation.
     * 
     * <p>Default message references the original COBOL business rule from COBIL00C.cbl
     * that prevents payments when the account balance is zero or negative.</p>
     * 
     * @return the default validation message
     */
    String message() default "Payment amount must be a positive value greater than $0.00";
    
    /**
     * Minimum payment amount allowed. Defaults to "0.01" to enforce positive amounts
     * while allowing the smallest possible monetary unit (1 cent).
     * 
     * <p>This corresponds to the COBOL business rule validation that prevents
     * bill payments when ACCT-CURR-BAL <= ZEROS.</p>
     * 
     * <p>Value must be specified as a string to ensure exact decimal precision
     * without floating-point representation issues.</p>
     * 
     * @return the minimum payment amount as a string representation
     */
    String min() default "0.01";
    
    /**
     * Maximum payment amount allowed. Defaults to "999999999.99" which represents
     * the maximum value that can be stored in the COBOL COMP-3 field S9(09)V99
     * as defined in CVTRA05Y.cpy.
     * 
     * <p>This ensures compatibility with the original COBOL data structure limits
     * and prevents arithmetic overflow when processing transactions.</p>
     * 
     * <p>Value must be specified as a string to ensure exact decimal precision
     * without floating-point representation issues.</p>
     * 
     * @return the maximum payment amount as a string representation
     */
    String max() default "999999999.99";
    
    /**
     * Custom message for amounts that are too small (below minimum).
     * Provides specific feedback when payment amount is zero or negative.
     * 
     * <p>This message corresponds to the COBOL error condition:
     * "You have nothing to pay..." from COBIL00C.cbl line 201-202.</p>
     * 
     * @return the validation message for amounts below minimum
     */
    String minMessage() default "You have nothing to pay - amount must be greater than ${min}";
    
    /**
     * Custom message for amounts that are too large (above maximum).
     * Provides specific feedback when payment amount exceeds COBOL field limits.
     * 
     * <p>This prevents arithmetic overflow and maintains compatibility with
     * the original COBOL COMP-3 field precision limits.</p>
     * 
     * @return the validation message for amounts above maximum
     */
    String maxMessage() default "Payment amount cannot exceed ${max} due to system limits";
    
    /**
     * Custom message for amounts with invalid decimal precision.
     * Ensures payment amounts maintain exactly 2 decimal places as required
     * by COBOL V99 specification and financial accuracy standards.
     * 
     * @return the validation message for invalid decimal precision
     */
    String precisionMessage() default "Payment amount must have exactly 2 decimal places for financial accuracy";
    
    /**
     * Custom message for null payment amounts.
     * Provides specific feedback when payment amount is not provided.
     * 
     * <p>This corresponds to the COBOL validation logic that checks for
     * empty or low-values in payment fields.</p>
     * 
     * @return the validation message for null amounts
     */
    String nullMessage() default "Payment amount is required and cannot be empty";
    
    /**
     * Validation groups allow conditional validation based on processing context.
     * Groups can be used to apply different validation rules for different
     * bill payment scenarios (e.g., full payment vs. minimum payment).
     * 
     * <p>Example usage for different payment contexts:</p>
     * <pre>
     * // Validate as full account balance payment
     * &#64;ValidPaymentAmount(groups = FullPayment.class)
     * private BigDecimal fullPaymentAmount;
     * 
     * // Validate as minimum payment with different rules
     * &#64;ValidPaymentAmount(min = "25.00", groups = MinimumPayment.class)
     * private BigDecimal minimumPaymentAmount;
     * </pre>
     * 
     * @return the validation groups
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for carrying additional metadata about the validation constraint.
     * Can be used by validation frameworks to provide enhanced error handling,
     * logging, or integration with monitoring systems.
     * 
     * <p>This interface allows the validation framework to carry additional
     * information about the constraint and validation failure context.</p>
     * 
     * @return the constraint payload
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Defines several {@code @ValidPaymentAmount} annotations on the same element.
     * This allows multiple payment amount validations with different parameters
     * to be applied to the same field or method.
     * 
     * <p>Example usage for complex payment validation scenarios:</p>
     * <pre>
     * &#64;ValidPaymentAmount.List({
     *     &#64;ValidPaymentAmount(min = "0.01", groups = RegularPayment.class),
     *     &#64;ValidPaymentAmount(min = "25.00", groups = CreditCardPayment.class)
     * })
     * private BigDecimal paymentAmount;
     * </pre>
     * 
     * @since 1.0.0
     */
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        /**
         * Array of ValidPaymentAmount annotations to be applied.
         * 
         * @return array of ValidPaymentAmount constraints
         */
        ValidPaymentAmount[] value();
    }
}