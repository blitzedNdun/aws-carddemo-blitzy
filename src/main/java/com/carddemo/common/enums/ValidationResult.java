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

package com.carddemo.common.enums;

import jakarta.validation.ConstraintViolation;
import org.springframework.validation.Errors;
import java.util.Optional;

/**
 * Enumeration defining comprehensive validation result types for field validation feedback.
 * This enum provides equivalent feedback granularity as the original COBOL field validation logic
 * from CSLKPCDY.cpy and CSUTLDTC.cbl, supporting both Jakarta Bean Validation integration and
 * React Hook Form error handling.
 * 
 * <p>The validation results maintain identical user experience as original BMS field validation
 * while providing enhanced error messaging for modern web applications.</p>
 * 
 * <p>Integration points:</p>
 * <ul>
 *   <li>Jakarta Bean Validation: Supports ConstraintViolation processing</li>
 *   <li>Spring Boot Validation: Integrates with Spring's Errors interface</li>
 *   <li>React Hook Form: Provides structured error messages for frontend validation</li>
 * </ul>
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 2024-01-01
 */
public enum ValidationResult {
    
    /**
     * Validation passed successfully.
     * Equivalent to successful COBOL field validation with no errors.
     */
    VALID("VALID", "Field validation passed", ""),
    
    /**
     * Field format is invalid.
     * Corresponds to COBOL format validation failures for data types, patterns, and length.
     * Examples: Invalid date format, non-numeric data in numeric field, incorrect phone format.
     */
    INVALID_FORMAT("INVALID_FORMAT", "Invalid field format", "Field format does not match required pattern"),
    
    /**
     * Field value is outside acceptable range.
     * Corresponds to COBOL range validation failures for numeric and date fields.
     * Examples: FICO score not between 300-850, negative account balance, future date in past context.
     */
    INVALID_RANGE("INVALID_RANGE", "Value out of range", "Field value is outside acceptable range"),
    
    /**
     * Cross-reference validation failed.
     * Corresponds to COBOL lookup validation failures from CSLKPCDY.cpy reference tables.
     * Examples: Invalid phone area code, non-existent state code, invalid state-zip combination.
     */
    INVALID_CROSS_REFERENCE("INVALID_CROSS_REFERENCE", "Cross-reference validation failed", "Referenced value does not exist in lookup table"),
    
    /**
     * Required field is blank or empty.
     * Corresponds to COBOL required field validation failures for mandatory data elements.
     */
    BLANK_FIELD("BLANK_FIELD", "Required field is blank", "This field is required and cannot be empty"),
    
    /**
     * Date validation failed - insufficient data.
     * Corresponds to COBOL CEEDAYS API FC-INSUFFICIENT-DATA error condition.
     */
    INSUFFICIENT_DATE_DATA("INSUFFICIENT_DATE_DATA", "Insufficient date data", "Date field does not contain sufficient data for validation"),
    
    /**
     * Date validation failed - invalid date value.
     * Corresponds to COBOL CEEDAYS API FC-BAD-DATE-VALUE error condition.
     */
    BAD_DATE_VALUE("BAD_DATE_VALUE", "Invalid date value", "Date field contains invalid date value"),
    
    /**
     * Date validation failed - invalid era.
     * Corresponds to COBOL CEEDAYS API FC-INVALID-ERA error condition.
     */
    INVALID_ERA("INVALID_ERA", "Invalid date era", "Date field contains invalid era specification"),
    
    /**
     * Date validation failed - unsupported range.
     * Corresponds to COBOL CEEDAYS API FC-UNSUPP-RANGE error condition.
     */
    UNSUPPORTED_RANGE("UNSUPPORTED_RANGE", "Unsupported date range", "Date field value is outside supported range"),
    
    /**
     * Date validation failed - invalid month.
     * Corresponds to COBOL CEEDAYS API FC-INVALID-MONTH error condition.
     */
    INVALID_MONTH("INVALID_MONTH", "Invalid month", "Date field contains invalid month value"),
    
    /**
     * Date validation failed - bad picture string.
     * Corresponds to COBOL CEEDAYS API FC-BAD-PIC-STRING error condition.
     */
    BAD_PIC_STRING("BAD_PIC_STRING", "Bad picture string", "Date field format specification is invalid"),
    
    /**
     * Date validation failed - non-numeric data.
     * Corresponds to COBOL CEEDAYS API FC-NON-NUMERIC-DATA error condition.
     */
    NON_NUMERIC_DATA("NON_NUMERIC_DATA", "Non-numeric data", "Date field contains non-numeric data"),
    
    /**
     * Date validation failed - year in era is zero.
     * Corresponds to COBOL CEEDAYS API FC-YEAR-IN-ERA-ZERO error condition.
     */
    YEAR_IN_ERA_ZERO("YEAR_IN_ERA_ZERO", "Year in era is zero", "Date field year value cannot be zero"),
    
    /**
     * Phone area code validation failed.
     * Corresponds to COBOL CSLKPCDY.cpy VALID-PHONE-AREA-CODE validation failure.
     */
    INVALID_PHONE_AREA_CODE("INVALID_PHONE_AREA_CODE", "Invalid phone area code", "Phone area code is not valid for North American numbering plan"),
    
    /**
     * US state code validation failed.
     * Corresponds to COBOL CSLKPCDY.cpy VALID-US-STATE-CODE validation failure.
     */
    INVALID_STATE_CODE("INVALID_STATE_CODE", "Invalid state code", "State code is not a valid US state or territory"),
    
    /**
     * State-ZIP code combination validation failed.
     * Corresponds to COBOL CSLKPCDY.cpy VALID-US-STATE-ZIP-CD2-COMBO validation failure.
     */
    INVALID_STATE_ZIP_COMBO("INVALID_STATE_ZIP_COMBO", "Invalid state-ZIP combination", "State and ZIP code combination is not valid"),
    
    /**
     * Account balance validation failed.
     * Corresponds to COBOL business rule validation for negative balances.
     */
    INVALID_ACCOUNT_BALANCE("INVALID_ACCOUNT_BALANCE", "Invalid account balance", "Account balance cannot be negative"),
    
    /**
     * FICO score validation failed.
     * Corresponds to COBOL business rule validation for FICO score range 300-850.
     */
    INVALID_FICO_SCORE("INVALID_FICO_SCORE", "Invalid FICO score", "FICO score must be between 300 and 850"),
    
    /**
     * Card expiry date validation failed.
     * Corresponds to COBOL business rule validation for future date requirement.
     */
    INVALID_CARD_EXPIRY("INVALID_CARD_EXPIRY", "Invalid card expiry", "Card expiry date must be in the future"),
    
    /**
     * SSN format validation failed.
     * Corresponds to COBOL business rule validation for 9-digit numeric SSN format.
     */
    INVALID_SSN_FORMAT("INVALID_SSN_FORMAT", "Invalid SSN format", "SSN must be 9 digits in XXX-XX-XXXX format");
    
    // Enum instance fields
    private final String code;
    private final String shortMessage;
    private final String detailedMessage;
    
    /**
     * Constructor for ValidationResult enum instances.
     * 
     * @param code The validation result code for programmatic identification
     * @param shortMessage Brief error message for display
     * @param detailedMessage Detailed error message for comprehensive feedback
     */
    ValidationResult(String code, String shortMessage, String detailedMessage) {
        this.code = code;
        this.shortMessage = shortMessage;
        this.detailedMessage = detailedMessage;
    }
    
    /**
     * Checks if the validation result represents a successful validation.
     * 
     * @return true if validation was successful, false otherwise
     */
    public boolean isValid() {
        return this == VALID;
    }
    
    /**
     * Gets the validation result code for programmatic identification.
     * 
     * @return The validation result code
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Gets the short error message for display purposes.
     * 
     * @return The short error message, or empty string for valid results
     */
    public String getErrorMessage() {
        return shortMessage;
    }
    
    /**
     * Gets the detailed error message for comprehensive feedback.
     * 
     * @return The detailed error message, or empty string for valid results
     */
    public String getDetailedMessage() {
        return detailedMessage;
    }
    
    /**
     * Creates a ValidationResult from a Jakarta Bean Validation ConstraintViolation.
     * This method supports integration with Spring Boot validation framework.
     * 
     * @param violation The constraint violation to convert
     * @return ValidationResult representing the violation type
     */
    public static ValidationResult fromConstraintViolation(ConstraintViolation<?> violation) {
        if (violation == null) {
            return VALID;
        }
        
        String message = violation.getMessage();
        String propertyPath = violation.getPropertyPath().toString();
        
        // Map common constraint violation messages to appropriate ValidationResult
        if (message.contains("must not be null") || message.contains("must not be blank")) {
            return BLANK_FIELD;
        } else if (message.contains("must match") || message.contains("invalid format")) {
            return INVALID_FORMAT;
        } else if (message.contains("must be between") || message.contains("range")) {
            return INVALID_RANGE;
        } else if (message.contains("phone") && message.contains("area code")) {
            return INVALID_PHONE_AREA_CODE;
        } else if (message.contains("state") && message.contains("code")) {
            return INVALID_STATE_CODE;
        } else if (message.contains("FICO") || message.contains("score")) {
            return INVALID_FICO_SCORE;
        } else if (message.contains("SSN") || message.contains("social security")) {
            return INVALID_SSN_FORMAT;
        } else if (message.contains("date") && message.contains("format")) {
            return BAD_DATE_VALUE;
        } else if (message.contains("expiry") || message.contains("expiration")) {
            return INVALID_CARD_EXPIRY;
        } else if (message.contains("balance") && message.contains("negative")) {
            return INVALID_ACCOUNT_BALANCE;
        } else {
            return INVALID_FORMAT; // Default fallback
        }
    }
    
    /**
     * Creates a ValidationResult from Spring Boot validation Errors.
     * This method supports integration with Spring MVC validation framework.
     * 
     * @param errors The Spring validation errors
     * @param fieldName The specific field name to check
     * @return ValidationResult representing the first error found, or VALID if no errors
     */
    public static ValidationResult fromSpringErrors(Errors errors, String fieldName) {
        if (errors == null || !errors.hasErrors()) {
            return VALID;
        }
        
        // Check for field-specific errors first
        if (errors.hasFieldErrors(fieldName)) {
            var fieldError = errors.getFieldError(fieldName);
            if (fieldError != null) {
                String errorMessage = fieldError.getDefaultMessage();
                return mapErrorMessageToValidationResult(errorMessage);
            }
        }
        
        // Check for global errors
        if (errors.hasGlobalErrors()) {
            var globalError = errors.getGlobalError();
            if (globalError != null) {
                String errorMessage = globalError.getDefaultMessage();
                return mapErrorMessageToValidationResult(errorMessage);
            }
        }
        
        return VALID;
    }
    
    /**
     * Maps an error message to the appropriate ValidationResult.
     * 
     * @param errorMessage The error message to map
     * @return The corresponding ValidationResult
     */
    private static ValidationResult mapErrorMessageToValidationResult(String errorMessage) {
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            return VALID;
        }
        
        String message = errorMessage.toLowerCase();
        
        if (message.contains("required") || message.contains("blank") || message.contains("empty")) {
            return BLANK_FIELD;
        } else if (message.contains("format") || message.contains("pattern")) {
            return INVALID_FORMAT;
        } else if (message.contains("range") || message.contains("between")) {
            return INVALID_RANGE;
        } else if (message.contains("cross-reference") || message.contains("lookup")) {
            return INVALID_CROSS_REFERENCE;
        } else if (message.contains("phone") && message.contains("area")) {
            return INVALID_PHONE_AREA_CODE;
        } else if (message.contains("state") && message.contains("code")) {
            return INVALID_STATE_CODE;
        } else if (message.contains("zip") && message.contains("state")) {
            return INVALID_STATE_ZIP_COMBO;
        } else if (message.contains("fico") || message.contains("score")) {
            return INVALID_FICO_SCORE;
        } else if (message.contains("ssn") || message.contains("social security")) {
            return INVALID_SSN_FORMAT;
        } else if (message.contains("balance") && message.contains("negative")) {
            return INVALID_ACCOUNT_BALANCE;
        } else if (message.contains("expiry") || message.contains("expiration")) {
            return INVALID_CARD_EXPIRY;
        } else if (message.contains("date")) {
            return BAD_DATE_VALUE;
        } else {
            return INVALID_FORMAT;
        }
    }
    
    /**
     * Creates an Optional ValidationResult for null-safe validation result processing.
     * 
     * @param success Whether the validation was successful
     * @param errorResult The ValidationResult to return if validation failed
     * @return Optional containing the result if validation failed, or empty Optional if successful
     */
    public static Optional<ValidationResult> ofError(boolean success, ValidationResult errorResult) {
        if (success) {
            return Optional.empty();
        }
        return Optional.of(errorResult);
    }
    
    /**
     * Creates an Optional ValidationResult for successful validation.
     * 
     * @return Optional containing VALID result
     */
    public static Optional<ValidationResult> ofValid() {
        return Optional.of(VALID);
    }
    
    /**
     * Creates an Optional ValidationResult for failed validation.
     * 
     * @param errorResult The ValidationResult representing the error
     * @return Optional containing the error result
     */
    public static Optional<ValidationResult> ofError(ValidationResult errorResult) {
        return Optional.of(errorResult);
    }
    
    /**
     * Checks if this ValidationResult represents an error condition.
     * 
     * @return true if this is an error result, false if valid
     */
    public boolean isError() {
        return this != VALID;
    }
    
    /**
     * Checks if this ValidationResult represents a format-related error.
     * 
     * @return true if this is a format error, false otherwise
     */
    public boolean isFormatError() {
        return this == INVALID_FORMAT || this == BAD_DATE_VALUE || this == BAD_PIC_STRING 
               || this == INVALID_SSN_FORMAT || this == NON_NUMERIC_DATA;
    }
    
    /**
     * Checks if this ValidationResult represents a range-related error.
     * 
     * @return true if this is a range error, false otherwise
     */
    public boolean isRangeError() {
        return this == INVALID_RANGE || this == INVALID_FICO_SCORE || this == INVALID_ACCOUNT_BALANCE
               || this == UNSUPPORTED_RANGE || this == INVALID_CARD_EXPIRY;
    }
    
    /**
     * Checks if this ValidationResult represents a cross-reference-related error.
     * 
     * @return true if this is a cross-reference error, false otherwise
     */
    public boolean isCrossReferenceError() {
        return this == INVALID_CROSS_REFERENCE || this == INVALID_PHONE_AREA_CODE 
               || this == INVALID_STATE_CODE || this == INVALID_STATE_ZIP_COMBO;
    }
    
    /**
     * Checks if this ValidationResult represents a date-related error.
     * 
     * @return true if this is a date error, false otherwise
     */
    public boolean isDateError() {
        return this == INSUFFICIENT_DATE_DATA || this == BAD_DATE_VALUE || this == INVALID_ERA
               || this == UNSUPPORTED_RANGE || this == INVALID_MONTH || this == BAD_PIC_STRING
               || this == NON_NUMERIC_DATA || this == YEAR_IN_ERA_ZERO || this == INVALID_CARD_EXPIRY;
    }
    
    /**
     * Returns a string representation of the ValidationResult suitable for logging.
     * 
     * @return String representation including code and error message
     */
    @Override
    public String toString() {
        return String.format("ValidationResult{code='%s', message='%s'}", code, shortMessage);
    }
}