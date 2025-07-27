/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.carddemo.common.enums;

import jakarta.validation.ConstraintViolation;
import org.springframework.validation.Errors;
import java.util.Optional;

/**
 * Enumeration defining comprehensive validation result types for field validation
 * feedback in the CardDemo application. This enum supports integration with both
 * Jakarta Bean Validation framework and Spring Boot validation, while providing
 * equivalent validation feedback granularity as the original COBOL field validation
 * logic found in CSUTLDTC.cbl and CSLKPCDY.cpy.
 * 
 * The validation results maintain identical user experience as original BMS field
 * validation and support React Hook Form error handling through structured error
 * messages and validation state indicators.
 */
public enum ValidationResult {
    
    /**
     * Indicates successful field validation with no errors detected.
     * Equivalent to COBOL FC-INVALID-DATE "Date is valid" success condition.
     */
    VALID("Field validation successful", "success"),
    
    /**
     * Indicates field format validation failure - data does not match expected
     * pattern or structure. Maps to COBOL validation patterns like invalid date
     * format, malformed SSN, or incorrect phone number structure.
     */
    INVALID_FORMAT("Field format is invalid", "format_error"),
    
    /**
     * Indicates field value is outside acceptable range or bounds.
     * Equivalent to COBOL numeric range validation (e.g., FICO scores 300-850,
     * non-negative account balances, future date requirements for card expiry).
     */
    INVALID_RANGE("Field value is outside acceptable range", "range_error"),
    
    /**
     * Indicates cross-reference validation failure - field value does not exist
     * in lookup tables or related data structures. Maps to COBOL 88-level
     * condition failures like invalid phone area codes (VALID-PHONE-AREA-CODE),
     * invalid state codes (VALID-US-STATE-CODE), or invalid state-zip combinations.
     */
    INVALID_CROSS_REFERENCE("Field value not found in reference data", "reference_error"),
    
    /**
     * Indicates required field is empty or contains only whitespace.
     * Equivalent to COBOL INSUFFICIENT-DATA condition from CSUTLDTC validation.
     */
    BLANK_FIELD("Required field cannot be blank", "required_error"),
    
    /**
     * Indicates date validation failure with detailed error categorization.
     * Maps to specific COBOL CEEDAYS API error conditions for comprehensive
     * date validation feedback.
     */
    INVALID_DATE("Date validation failed", "date_error"),
    
    /**
     * Indicates numeric data validation failure for non-numeric content
     * in numeric fields. Equivalent to COBOL FC-NON-NUMERIC-DATA condition.
     */
    NON_NUMERIC_DATA("Field contains non-numeric data", "numeric_error"),
    
    /**
     * Indicates data length validation failure - field content exceeds
     * maximum allowed length or is shorter than minimum required length.
     */
    INVALID_LENGTH("Field length is invalid", "length_error"),
    
    /**
     * Indicates business rule validation failure that doesn't fit other
     * categories. Used for complex validation logic specific to business
     * requirements.
     */
    BUSINESS_RULE_VIOLATION("Business rule validation failed", "business_error");
    
    private final String defaultMessage;
    private final String errorCode;
    
    /**
     * Constructs a ValidationResult enum value with default message and error code.
     *
     * @param defaultMessage The default error message for this validation result
     * @param errorCode The error code for client-side error handling
     */
    ValidationResult(String defaultMessage, String errorCode) {
        this.defaultMessage = defaultMessage;
        this.errorCode = errorCode;
    }
    
    /**
     * Determines if this validation result represents a successful validation.
     * Only VALID enum value returns true.
     *
     * @return true if validation was successful, false otherwise
     */
    public boolean isValid() {
        return this == VALID;
    }
    
    /**
     * Gets the default error message for this validation result.
     * This message maintains compatibility with original BMS field validation
     * error messages and provides appropriate feedback for React form components.
     *
     * @return the default error message
     */
    public String getErrorMessage() {
        return defaultMessage;
    }
    
    /**
     * Gets the error code for this validation result.
     * Error codes support client-side error handling and internationalization
     * in React components.
     *
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Creates a ValidationResult from a Jakarta Bean Validation ConstraintViolation.
     * This method integrates with Spring Boot's validation framework to provide
     * consistent validation result handling across the application.
     *
     * @param violation the constraint violation from Jakarta validation
     * @return appropriate ValidationResult based on the violation
     */
    public static ValidationResult fromConstraintViolation(ConstraintViolation<?> violation) {
        if (violation == null) {
            return VALID;
        }
        
        String message = violation.getMessage();
        String propertyPath = violation.getPropertyPath().toString();
        
        // Map common Jakarta validation messages to ValidationResult types
        if (message.toLowerCase().contains("blank") || message.toLowerCase().contains("empty")) {
            return BLANK_FIELD;
        } else if (message.toLowerCase().contains("size") || message.toLowerCase().contains("length")) {
            return INVALID_LENGTH;
        } else if (message.toLowerCase().contains("range") || message.toLowerCase().contains("between")) {
            return INVALID_RANGE;
        } else if (message.toLowerCase().contains("pattern") || message.toLowerCase().contains("format")) {
            return INVALID_FORMAT;
        } else if (message.toLowerCase().contains("date")) {
            return INVALID_DATE;
        } else if (message.toLowerCase().contains("numeric") || message.toLowerCase().contains("number")) {
            return NON_NUMERIC_DATA;
        } else {
            return BUSINESS_RULE_VIOLATION;
        }
    }
    
    /**
     * Creates a ValidationResult from Spring Validation Errors object.
     * This method provides integration with Spring Boot's validation framework
     * and supports comprehensive error handling for REST API endpoints.
     *
     * @param errors the Spring validation errors object
     * @param fieldName the name of the field being validated
     * @return appropriate ValidationResult based on the field errors
     */
    public static ValidationResult fromSpringErrors(Errors errors, String fieldName) {
        if (errors == null || !errors.hasErrors()) {
            return VALID;
        }
        
        if (errors.hasFieldErrors(fieldName)) {
            String errorMessage = Optional.ofNullable(errors.getFieldError(fieldName))
                    .map(fieldError -> fieldError.getDefaultMessage())
                    .orElse("");
                    
            return fromErrorMessage(errorMessage);
        } else if (errors.hasGlobalErrors()) {
            String errorMessage = Optional.ofNullable(errors.getGlobalError())
                    .map(globalError -> globalError.getDefaultMessage())
                    .orElse("");
                    
            return fromErrorMessage(errorMessage);
        }
        
        return VALID;
    }
    
    /**
     * Creates a ValidationResult based on error message content analysis.
     * This method provides consistent mapping from various validation frameworks
     * to our standardized ValidationResult enumeration.
     *
     * @param errorMessage the error message to analyze
     * @return appropriate ValidationResult based on message content
     */
    public static ValidationResult fromErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            return VALID;
        }
        
        String lowerMessage = errorMessage.toLowerCase();
        
        if (lowerMessage.contains("blank") || lowerMessage.contains("required") || 
            lowerMessage.contains("empty") || lowerMessage.contains("null")) {
            return BLANK_FIELD;
        } else if (lowerMessage.contains("format") || lowerMessage.contains("pattern") ||
                   lowerMessage.contains("invalid format") || lowerMessage.contains("malformed")) {
            return INVALID_FORMAT;
        } else if (lowerMessage.contains("range") || lowerMessage.contains("between") ||
                   lowerMessage.contains("minimum") || lowerMessage.contains("maximum") ||
                   lowerMessage.contains("too large") || lowerMessage.contains("too small")) {
            return INVALID_RANGE;
        } else if (lowerMessage.contains("reference") || lowerMessage.contains("lookup") ||
                   lowerMessage.contains("not found") || lowerMessage.contains("invalid code")) {
            return INVALID_CROSS_REFERENCE;
        } else if (lowerMessage.contains("date") || lowerMessage.contains("invalid era") ||
                   lowerMessage.contains("invalid month") || lowerMessage.contains("year")) {
            return INVALID_DATE;
        } else if (lowerMessage.contains("numeric") || lowerMessage.contains("number") ||
                   lowerMessage.contains("digit") || lowerMessage.contains("nonnumeric")) {
            return NON_NUMERIC_DATA;
        } else if (lowerMessage.contains("length") || lowerMessage.contains("size") ||
                   lowerMessage.contains("too long") || lowerMessage.contains("too short")) {
            return INVALID_LENGTH;
        } else {
            return BUSINESS_RULE_VIOLATION;
        }
    }
    
    /**
     * Creates a custom ValidationResult with a specific error message.
     * This method supports dynamic error message generation while maintaining
     * the appropriate validation result type classification.
     *
     * @param resultType the type of validation result
     * @param customMessage the custom error message
     * @return ValidationResult wrapper with custom message
     */
    public static ValidationResultWithMessage withCustomMessage(ValidationResult resultType, String customMessage) {
        return new ValidationResultWithMessage(resultType, customMessage);
    }
    
    /**
     * Wrapper class for ValidationResult with custom error message.
     * Supports dynamic error message handling while preserving validation
     * result type information for client-side error handling.
     */
    public static class ValidationResultWithMessage {
        private final ValidationResult result;
        private final String customMessage;
        
        private ValidationResultWithMessage(ValidationResult result, String customMessage) {
            this.result = result;
            this.customMessage = customMessage;
        }
        
        /**
         * Gets the validation result type.
         *
         * @return the validation result
         */
        public ValidationResult getResult() {
            return result;
        }
        
        /**
         * Gets the custom error message.
         *
         * @return the custom message
         */
        public String getMessage() {
            return customMessage != null ? customMessage : result.getErrorMessage();
        }
        
        /**
         * Gets the error code from the underlying validation result.
         *
         * @return the error code
         */
        public String getErrorCode() {
            return result.getErrorCode();
        }
        
        /**
         * Determines if the validation was successful.
         *
         * @return true if validation was successful
         */
        public boolean isValid() {
            return result.isValid();
        }
    }
}