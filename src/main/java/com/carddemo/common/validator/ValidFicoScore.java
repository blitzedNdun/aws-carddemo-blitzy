/*
 * ValidFicoScore.java
 * 
 * Jakarta Bean Validation annotation for FICO credit score range validation.
 * Ensures credit scores fall within the standard 300-850 range while supporting
 * nullable values for customers without established credit history.
 * 
 * This annotation enforces the business rule validation requirements specified
 * in the CardDemo system's functional requirements (F-003-RQ-003) which mandate
 * FICO score validation with range 300-850 and numeric range checking.
 * 
 * The validation supports:
 * - Standard FICO credit score range validation (300-850)
 * - Nullable score support for customers without established credit history
 * - Business-appropriate validation error messages
 * - Jakarta Bean Validation framework integration
 * 
 * Usage:
 * @ValidFicoScore
 * private Integer ficoScore;
 * 
 * @ValidFicoScore(message = "Custom FICO score validation message")
 * private Integer customerCreditScore;
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
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Jakarta Bean Validation annotation for FICO credit score range validation.
 * 
 * Validates that credit scores fall within the standard FICO range of 300-850.
 * Supports nullable values for customers without established credit history.
 * 
 * This annotation implements the business rule validation requirements as
 * specified in the CardDemo functional requirements (F-003-RQ-003) which
 * mandate FICO score validation with numeric range checking.
 * 
 * The validation logic:
 * - Null values are considered valid (optional credit scores)
 * - Non-null values must be between 300 and 850 inclusive
 * - Provides clear, business-appropriate error messages
 * 
 * Supported data types:
 * - Integer (recommended for database integer fields)
 * - Short (for compact storage scenarios)
 * 
 * @since CardDemo v1.0
 * @author Blitzy Platform
 */
@Documented
@Constraint(validatedBy = FicoScoreValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFicoScore {
    
    /**
     * Default validation error message.
     * Provides a clear, business-appropriate message for FICO score validation failures.
     * 
     * @return the default error message
     */
    String message() default "FICO credit score must be between 300 and 850";
    
    /**
     * Validation groups for conditional validation scenarios.
     * Allows grouping of validation constraints for different business contexts.
     * 
     * @return the validation groups
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for additional metadata or custom validation processing.
     * Can be used to pass additional information to validation frameworks.
     * 
     * @return the payload classes
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Minimum valid FICO score value.
     * Standard FICO score range starts at 300.
     * 
     * @return the minimum FICO score (300)
     */
    int min() default 300;
    
    /**
     * Maximum valid FICO score value.
     * Standard FICO score range ends at 850.
     * 
     * @return the maximum FICO score (850)
     */
    int max() default 850;
    
    /**
     * FICO Score Validator implementation.
     * 
     * Note: The actual validation logic is implemented in the standalone
     * FicoScoreValidator class, not this inner class.
     * 
     * The validator:
     * - Treats null values as valid (supports optional credit scores)
     * - Validates non-null Integer and Short values against the 300-850 range
     * - Provides detailed error messages for out-of-range values
     * - Handles edge cases and type safety
     * 
     * This implementation follows the business rules established in the
     * original COBOL system where FICO scores were stored as PIC 9(03)
     * fields with range validation performed during account updates.
     */
    class FicoScoreValidator implements ConstraintValidator<ValidFicoScore, Number> {
        
        private int minValue;
        private int maxValue;
        private String messageTemplate;
        
        /**
         * Initialize the validator with annotation parameter values.
         * 
         * @param constraintAnnotation the ValidFicoScore annotation instance
         */
        @Override
        public void initialize(ValidFicoScore constraintAnnotation) {
            this.minValue = constraintAnnotation.min();
            this.maxValue = constraintAnnotation.max();
            this.messageTemplate = constraintAnnotation.message();
        }
        
        /**
         * Validate the FICO credit score value.
         * 
         * Implements the core validation logic:
         * 1. Null values are considered valid (supports customers without credit history)
         * 2. Non-null values must be within the specified range (default 300-850)
         * 3. Supports Integer and Short numeric types
         * 4. Provides clear validation failure messages
         * 
         * @param value the FICO score value to validate (can be null)
         * @param context the validation context for error reporting
         * @return true if the value is valid, false otherwise
         */
        @Override
        public boolean isValid(Number value, ConstraintValidatorContext context) {
            // Null values are valid - supports customers without established credit history
            if (value == null) {
                return true;
            }
            
            // Convert to integer for range checking
            int ficoScore;
            if (value instanceof Integer) {
                ficoScore = (Integer) value;
            } else if (value instanceof Short) {
                ficoScore = ((Short) value).intValue();
            } else {
                // Unsupported number type - provide helpful error message
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "FICO score must be an Integer or Short value")
                    .addConstraintViolation();
                return false;
            }
            
            // Validate the FICO score is within the acceptable range
            boolean isValidRange = ficoScore >= minValue && ficoScore <= maxValue;
            
            if (!isValidRange) {
                // Provide specific error message with actual value and expected range
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    String.format("FICO credit score %d is out of range. Valid range is %d-%d", 
                        ficoScore, minValue, maxValue))
                    .addConstraintViolation();
                return false;
            }
            
            return true;
        }
    }
}