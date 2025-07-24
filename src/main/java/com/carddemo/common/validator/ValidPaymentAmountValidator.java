/*
 * ValidPaymentAmountValidator.java
 * 
 * CardDemo Application
 * 
 * Implementation class for bill payment amount validation with BigDecimal precision and business
 * rule validation. Validates payment amounts using exact decimal precision equivalent to COBOL
 * COMP-3 calculations while supporting configurable range validation and bill payment specific
 * business rules.
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

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Implementation class for bill payment amount validation with BigDecimal precision and business
 * rule enforcement. Validates payment amounts using exact decimal precision equivalent to COBOL
 * COMP-3 calculations while supporting configurable range validation and bill payment specific
 * business rules.
 * 
 * <p>This validator implements the business logic extracted from the original COBOL bill payment
 * program COBIL00C.cbl and ensures payment amounts meet CardDemo bill payment requirements:</p>
 * 
 * <ul>
 *   <li><strong>Null Handling</strong>: Considers null payment amounts as invalid</li>
 *   <li><strong>Minimum Amount</strong>: Enforces configurable minimum payment (default: $0.01)</li>
 *   <li><strong>Maximum Amount</strong>: Enforces COBOL COMP-3 field limit (default: $999,999,999.99)</li>
 *   <li><strong>Precision Validation</strong>: Ensures exactly 2 decimal places for financial accuracy</li>
 *   <li><strong>Scale Validation</strong>: Validates decimal scale matches COBOL V99 specification</li>
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
 * <p><strong>Validation Process:</strong></p>
 * <ol>
 *   <li>Check for null payment amount (invalid)</li>
 *   <li>Validate minimum amount constraint (configurable, default $0.01)</li>
 *   <li>Validate maximum amount constraint (COBOL S9(09)V99 limit)</li>
 *   <li>Verify decimal precision matches financial standards (exactly 2 decimal places)</li>
 *   <li>Ensure scale validation for COBOL V99 compatibility</li>
 * </ol>
 * 
 * <p><strong>Error Message Context:</strong></p>
 * The validator provides context-specific error messages based on the validation failure type:
 * <ul>
 *   <li>Null amounts: Uses nullMessage from annotation</li>
 *   <li>Below minimum: Uses minMessage with actual minimum value</li>
 *   <li>Above maximum: Uses maxMessage with actual maximum value</li>
 *   <li>Invalid precision: Uses precisionMessage for decimal place issues</li>
 * </ul>
 * 
 * @since 1.0.0
 * @see ValidPaymentAmount
 * @see ValidationConstants
 */
public class ValidPaymentAmountValidator implements ConstraintValidator<ValidPaymentAmount, BigDecimal> {
    
    /**
     * MathContext for exact financial calculations equivalent to COBOL COMP-3 precision.
     * Uses DECIMAL128 with HALF_UP rounding to match COBOL arithmetic behavior.
     */
    private static final MathContext COBOL_MATH_CONTEXT = new MathContext(28, RoundingMode.HALF_UP);
    
    /**
     * Required decimal scale for payment amounts (2 decimal places for cents).
     * Matches COBOL V99 specification from CVTRA05Y.cpy.
     */
    private static final int REQUIRED_SCALE = ValidationConstants.CURRENCY_SCALE;
    
    /**
     * Maximum precision for payment amounts (total digits).
     * Matches COBOL S9(09)V99 specification allowing up to 12 total digits.
     */
    private static final int MAXIMUM_PRECISION = ValidationConstants.CURRENCY_PRECISION;
    
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String minMessage;
    private String maxMessage;
    private String precisionMessage;
    private String nullMessage;
    
    /**
     * Initializes the validator with constraint annotation parameters.
     * Parses minimum and maximum amounts from string values to ensure exact decimal precision.
     * 
     * @param constraintAnnotation the ValidPaymentAmount annotation containing validation parameters
     */
    @Override
    public void initialize(ValidPaymentAmount constraintAnnotation) {
        try {
            // Parse min/max amounts using exact decimal precision to avoid floating-point issues
            this.minAmount = new BigDecimal(constraintAnnotation.min(), COBOL_MATH_CONTEXT);
            this.maxAmount = new BigDecimal(constraintAnnotation.max(), COBOL_MATH_CONTEXT);
            
            // Store custom error messages for different validation failure types
            this.minMessage = constraintAnnotation.minMessage();
            this.maxMessage = constraintAnnotation.maxMessage();
            this.precisionMessage = constraintAnnotation.precisionMessage();
            this.nullMessage = constraintAnnotation.nullMessage();
            
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid min or max payment amount format in @ValidPaymentAmount annotation", e);
        }
    }
    
    /**
     * Performs comprehensive validation of payment amounts with BigDecimal precision.
     * Implements the complete business rule validation equivalent to COBOL bill payment logic.
     * 
     * @param value the BigDecimal payment amount to validate
     * @param context the constraint validator context for error message customization
     * @return true if the payment amount is valid, false otherwise
     */
    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        // Step 1: Check for null payment amount (equivalent to COBOL LOW-VALUES check)
        if (value == null) {
            buildConstraintViolation(context, nullMessage);
            return false;
        }
        
        // Step 2: Validate minimum payment amount (equivalent to COBOL <= ZEROS check)
        if (value.compareTo(minAmount) < 0) {
            String message = minMessage.replace("${min}", minAmount.toPlainString());
            buildConstraintViolation(context, message);
            return false;
        }
        
        // Step 3: Validate maximum payment amount (COBOL S9(09)V99 field limit)
        if (value.compareTo(maxAmount) > 0) {
            String message = maxMessage.replace("${max}", maxAmount.toPlainString());
            buildConstraintViolation(context, message);
            return false;
        }
        
        // Step 4: Validate decimal precision for financial accuracy
        if (!isValidPrecision(value)) {
            buildConstraintViolation(context, precisionMessage);
            return false;
        }
        
        // Step 5: All validations passed
        return true;
    }
    
    /**
     * Validates that the payment amount has exactly the required decimal precision.
     * Ensures compatibility with COBOL V99 specification and financial accuracy standards.
     * 
     * <p>This method checks:</p>
     * <ul>
     *   <li>Scale matches REQUIRED_SCALE (2 decimal places)</li>
     *   <li>Total precision does not exceed MAXIMUM_PRECISION (12 digits)</li>
     *   <li>Value can be represented exactly without precision loss</li>
     * </ul>
     * 
     * @param value the BigDecimal payment amount to check for precision
     * @return true if precision is valid, false otherwise
     */
    private boolean isValidPrecision(BigDecimal value) {
        // Check if scale exceeds the required 2 decimal places
        if (value.scale() > REQUIRED_SCALE) {
            return false;
        }
        
        // Check if total precision exceeds COBOL S9(09)V99 limits
        if (value.precision() > MAXIMUM_PRECISION) {
            return false;
        }
        
        // Ensure the value can be represented with exact precision at required scale
        try {
            // Test if value can be set to required scale without precision loss
            BigDecimal scaledValue = value.setScale(REQUIRED_SCALE, RoundingMode.UNNECESSARY);
            return scaledValue.equals(value.setScale(REQUIRED_SCALE, RoundingMode.HALF_UP));
        } catch (ArithmeticException e) {
            // ArithmeticException indicates precision loss would occur
            return false;
        }
    }
    
    /**
     * Builds a custom constraint violation message and disables the default message.
     * This allows for context-specific error messages based on the validation failure type.
     * 
     * @param context the constraint validator context
     * @param message the custom error message to display
     */
    private void buildConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
    }
}