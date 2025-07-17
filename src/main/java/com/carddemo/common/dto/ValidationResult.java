package com.carddemo.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * ValidationResult DTO containing field-level validation messages, business rule compliance status,
 * and comprehensive error information for API response validation feedback.
 * 
 * This class serves as a unified response structure for all validation operations throughout the
 * CardDemo application, providing consistent error handling and user feedback mechanisms aligned
 * with the Spring Boot microservices architecture.
 * 
 * Key Features:
 * - Field-level error messages for comprehensive user feedback
 * - Business rule compliance status with pass/fail information
 * - Standardized error codes for consistent API error handling
 * - Validation severity levels (ERROR, WARNING, INFO) for graduated user feedback
 * - JSON serialization annotations for consistent API response formatting
 * - Helper methods for validation status checking and error aggregation
 * 
 * Usage Example:
 * <pre>
 * ValidationResult result = new ValidationResult();
 * result.addErrorMessage("FIELD_REQUIRED", "Account ID is required", ValidationSeverity.ERROR);
 * result.addErrorMessage("FORMAT_INVALID", "Invalid SSN format", ValidationSeverity.ERROR);
 * result.setValid(false);
 * </pre>
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 2024-01-01
 */
public class ValidationResult {
    
    /**
     * Enumeration defining validation severity levels for graduated user feedback.
     * Aligned with BMS field attribute validation patterns from the original COBOL system.
     */
    public enum ValidationSeverity {
        /** Critical validation errors that prevent operation completion */
        ERROR,
        /** Warning conditions that may impact operation but don't prevent completion */
        WARNING,
        /** Informational messages providing additional context or guidance */
        INFO
    }
    
    /**
     * Inner class representing a single validation error with detailed information.
     * Provides comprehensive error context for field-level validation feedback.
     */
    public static class ValidationError {
        
        @JsonProperty("error_code")
        private String errorCode;
        
        @JsonProperty("error_message")
        private String errorMessage;
        
        @JsonProperty("field_name")
        private String fieldName;
        
        @JsonProperty("severity")
        private ValidationSeverity severity;
        
        /**
         * Default constructor for JSON deserialization.
         */
        public ValidationError() {
            this.severity = ValidationSeverity.ERROR;
        }
        
        /**
         * Constructor for creating validation error with all details.
         * 
         * @param errorCode Standardized error code for consistent API error handling
         * @param errorMessage Descriptive error message for user feedback
         * @param fieldName Name of the field that failed validation (optional)
         * @param severity Validation severity level
         */
        public ValidationError(String errorCode, String errorMessage, String fieldName, ValidationSeverity severity) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.fieldName = fieldName;
            this.severity = severity != null ? severity : ValidationSeverity.ERROR;
        }
        
        /**
         * Constructor for creating validation error with error code and message.
         * 
         * @param errorCode Standardized error code for consistent API error handling
         * @param errorMessage Descriptive error message for user feedback
         * @param severity Validation severity level
         */
        public ValidationError(String errorCode, String errorMessage, ValidationSeverity severity) {
            this(errorCode, errorMessage, null, severity);
        }
        
        // Getters and setters
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName; }
        
        public ValidationSeverity getSeverity() { return severity; }
        public void setSeverity(ValidationSeverity severity) { 
            this.severity = severity != null ? severity : ValidationSeverity.ERROR; 
        }
        
        @Override
        public String toString() {
            return String.format("ValidationError{code='%s', message='%s', field='%s', severity='%s'}", 
                    errorCode, errorMessage, fieldName, severity);
        }
    }
    
    // Main ValidationResult fields
    
    /**
     * Overall validation status indicating if all validation rules passed.
     * Corresponds to business rule compliance status for comprehensive validation feedback.
     */
    @JsonProperty("is_valid")
    private boolean valid;
    
    /**
     * List of validation error messages providing field-level validation feedback.
     * Supports comprehensive error information for user guidance and debugging.
     */
    @JsonProperty("error_messages")
    private List<ValidationError> errorMessages;
    
    /**
     * Overall validation severity level based on the most severe error encountered.
     * Provides graduated user feedback for different types of validation issues.
     */
    @JsonProperty("severity")
    private ValidationSeverity severity;
    
    /**
     * Primary validation code representing the most critical validation failure.
     * Used for standardized error handling and automated error processing.
     */
    @JsonProperty("validation_code")
    private String validationCode;
    
    /**
     * Total count of validation errors for quick assessment.
     * Provides immediate feedback on validation result complexity.
     */
    @JsonProperty("error_count")
    private int errorCount;
    
    /**
     * Additional context information for validation results.
     * Supports extended validation scenarios and debugging information.
     */
    @JsonProperty("validation_context")
    private String validationContext;
    
    /**
     * Default constructor initializing ValidationResult with valid state.
     * Creates an empty validation result indicating successful validation.
     */
    public ValidationResult() {
        this.valid = true;
        this.errorMessages = new ArrayList<>();
        this.severity = ValidationSeverity.INFO;
        this.errorCount = 0;
    }
    
    /**
     * Constructor for creating ValidationResult with initial validation status.
     * 
     * @param valid Initial validation status
     */
    public ValidationResult(boolean valid) {
        this();
        this.valid = valid;
        if (!valid) {
            this.severity = ValidationSeverity.ERROR;
        }
    }
    
    /**
     * Constructor for creating ValidationResult with validation status and primary error code.
     * 
     * @param valid Initial validation status
     * @param validationCode Primary validation code for standardized error handling
     */
    public ValidationResult(boolean valid, String validationCode) {
        this(valid);
        this.validationCode = validationCode;
    }
    
    // Core validation methods
    
    /**
     * Checks if the validation result is valid (no errors).
     * 
     * @return true if validation passed, false otherwise
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Sets the overall validation status.
     * 
     * @param valid Validation status to set
     */
    public void setValid(boolean valid) {
        this.valid = valid;
        if (!valid && this.severity == ValidationSeverity.INFO) {
            this.severity = ValidationSeverity.WARNING;
        }
    }
    
    /**
     * Gets the list of validation error messages.
     * 
     * @return List of validation errors
     */
    public List<ValidationError> getErrorMessages() {
        return errorMessages;
    }
    
    /**
     * Sets the list of validation error messages.
     * Updates error count and overall validation status accordingly.
     * 
     * @param errorMessages List of validation errors to set
     */
    public void setErrorMessages(List<ValidationError> errorMessages) {
        this.errorMessages = errorMessages != null ? errorMessages : new ArrayList<>();
        this.errorCount = this.errorMessages.size();
        
        // Update overall validation status based on errors
        if (this.errorCount > 0) {
            this.valid = false;
            updateOverallSeverity();
        } else {
            this.valid = true;
            this.severity = ValidationSeverity.INFO;
        }
    }
    
    /**
     * Adds a validation error message with detailed information.
     * Automatically updates error count and overall validation status.
     * 
     * @param errorCode Standardized error code for consistent API error handling
     * @param errorMessage Descriptive error message for user feedback
     * @param fieldName Name of the field that failed validation (optional)
     * @param severity Validation severity level
     */
    public void addErrorMessage(String errorCode, String errorMessage, String fieldName, ValidationSeverity severity) {
        ValidationError error = new ValidationError(errorCode, errorMessage, fieldName, severity);
        this.errorMessages.add(error);
        this.errorCount = this.errorMessages.size();
        this.valid = false;
        
        // Update primary validation code if not set or if this is more severe
        if (this.validationCode == null || severity == ValidationSeverity.ERROR) {
            this.validationCode = errorCode;
        }
        
        updateOverallSeverity();
    }
    
    /**
     * Adds a validation error message with error code and message.
     * Uses ERROR severity by default.
     * 
     * @param errorCode Standardized error code for consistent API error handling
     * @param errorMessage Descriptive error message for user feedback
     * @param severity Validation severity level
     */
    public void addErrorMessage(String errorCode, String errorMessage, ValidationSeverity severity) {
        addErrorMessage(errorCode, errorMessage, null, severity);
    }
    
    /**
     * Adds a validation error message with error code and message.
     * Uses ERROR severity by default.
     * 
     * @param errorCode Standardized error code for consistent API error handling
     * @param errorMessage Descriptive error message for user feedback
     */
    public void addErrorMessage(String errorCode, String errorMessage) {
        addErrorMessage(errorCode, errorMessage, null, ValidationSeverity.ERROR);
    }
    
    /**
     * Checks if there are any validation errors.
     * 
     * @return true if there are validation errors, false otherwise
     */
    public boolean hasErrors() {
        return !errorMessages.isEmpty();
    }
    
    /**
     * Gets the total count of validation errors.
     * 
     * @return Number of validation errors
     */
    public int getErrorCount() {
        return errorCount;
    }
    
    /**
     * Gets the overall validation severity.
     * 
     * @return Validation severity level
     */
    public ValidationSeverity getSeverity() {
        return severity;
    }
    
    /**
     * Sets the overall validation severity.
     * 
     * @param severity Validation severity level to set
     */
    public void setSeverity(ValidationSeverity severity) {
        this.severity = severity != null ? severity : ValidationSeverity.INFO;
    }
    
    /**
     * Gets the primary validation code.
     * 
     * @return Primary validation code for standardized error handling
     */
    public String getValidationCode() {
        return validationCode;
    }
    
    /**
     * Sets the primary validation code.
     * 
     * @param validationCode Primary validation code to set
     */
    public void setValidationCode(String validationCode) {
        this.validationCode = validationCode;
    }
    
    /**
     * Gets the validation context information.
     * 
     * @return Validation context description
     */
    public String getValidationContext() {
        return validationContext;
    }
    
    /**
     * Sets the validation context information.
     * 
     * @param validationContext Validation context description
     */
    public void setValidationContext(String validationContext) {
        this.validationContext = validationContext;
    }
    
    // Helper methods for validation status checking and error aggregation
    
    /**
     * Checks if there are any ERROR severity validation messages.
     * 
     * @return true if there are ERROR level validation messages
     */
    public boolean hasErrorLevelMessages() {
        return errorMessages.stream()
                .anyMatch(error -> error.getSeverity() == ValidationSeverity.ERROR);
    }
    
    /**
     * Checks if there are any WARNING severity validation messages.
     * 
     * @return true if there are WARNING level validation messages
     */
    public boolean hasWarnings() {
        return errorMessages.stream()
                .anyMatch(error -> error.getSeverity() == ValidationSeverity.WARNING);
    }
    
    /**
     * Checks if there are any INFO severity validation messages.
     * 
     * @return true if there are INFO level validation messages
     */
    public boolean hasInfoMessages() {
        return errorMessages.stream()
                .anyMatch(error -> error.getSeverity() == ValidationSeverity.INFO);
    }
    
    /**
     * Gets count of errors by severity level.
     * 
     * @param severity Severity level to count
     * @return Number of errors with specified severity
     */
    public long getErrorCountBySeverity(ValidationSeverity severity) {
        return errorMessages.stream()
                .filter(error -> error.getSeverity() == severity)
                .count();
    }
    
    /**
     * Gets all error messages of a specific severity level.
     * 
     * @param severity Severity level to filter by
     * @return List of errors with specified severity
     */
    public List<ValidationError> getErrorsBySeverity(ValidationSeverity severity) {
        return errorMessages.stream()
                .filter(error -> error.getSeverity() == severity)
                .toList();
    }
    
    /**
     * Gets all error messages for a specific field.
     * 
     * @param fieldName Field name to filter by
     * @return List of errors for the specified field
     */
    public List<ValidationError> getErrorsForField(String fieldName) {
        return errorMessages.stream()
                .filter(error -> fieldName != null && fieldName.equals(error.getFieldName()))
                .toList();
    }
    
    /**
     * Merges another ValidationResult into this one.
     * Combines error messages and updates overall validation status.
     * 
     * @param other ValidationResult to merge
     */
    public void mergeWith(ValidationResult other) {
        if (other == null) {
            return;
        }
        
        // Add all error messages from the other result
        this.errorMessages.addAll(other.getErrorMessages());
        this.errorCount = this.errorMessages.size();
        
        // Update validation status
        if (!other.isValid()) {
            this.valid = false;
        }
        
        // Update primary validation code if not set
        if (this.validationCode == null && other.getValidationCode() != null) {
            this.validationCode = other.getValidationCode();
        }
        
        // Update overall severity
        updateOverallSeverity();
        
        // Merge validation context if needed
        if (this.validationContext == null && other.getValidationContext() != null) {
            this.validationContext = other.getValidationContext();
        }
    }
    
    /**
     * Clears all validation errors and resets to valid state.
     */
    public void clear() {
        this.errorMessages.clear();
        this.errorCount = 0;
        this.valid = true;
        this.severity = ValidationSeverity.INFO;
        this.validationCode = null;
        this.validationContext = null;
    }
    
    /**
     * Creates a summary of validation results for logging or display.
     * 
     * @return String summary of validation results
     */
    @JsonIgnore
    public String getSummary() {
        if (valid) {
            return "Validation passed successfully";
        }
        
        long errorCount = getErrorCountBySeverity(ValidationSeverity.ERROR);
        long warningCount = getErrorCountBySeverity(ValidationSeverity.WARNING);
        long infoCount = getErrorCountBySeverity(ValidationSeverity.INFO);
        
        return String.format("Validation failed - Errors: %d, Warnings: %d, Info: %d", 
                errorCount, warningCount, infoCount);
    }
    
    /**
     * Updates the overall severity based on the most severe error encountered.
     * Private helper method for maintaining consistent severity levels.
     */
    private void updateOverallSeverity() {
        if (errorMessages.isEmpty()) {
            this.severity = ValidationSeverity.INFO;
            return;
        }
        
        // Find the most severe error level
        boolean hasError = errorMessages.stream()
                .anyMatch(error -> error.getSeverity() == ValidationSeverity.ERROR);
        if (hasError) {
            this.severity = ValidationSeverity.ERROR;
            return;
        }
        
        boolean hasWarning = errorMessages.stream()
                .anyMatch(error -> error.getSeverity() == ValidationSeverity.WARNING);
        if (hasWarning) {
            this.severity = ValidationSeverity.WARNING;
            return;
        }
        
        this.severity = ValidationSeverity.INFO;
    }
    
    /**
     * Checks if the validation result represents a successful validation.
     * Alias for isValid() method for improved readability.
     * 
     * @return true if validation was successful
     */
    @JsonIgnore
    public boolean isSuccess() {
        return isValid();
    }
    
    /**
     * Checks if the validation result represents a failed validation.
     * Convenience method for improved readability.
     * 
     * @return true if validation failed
     */
    @JsonIgnore
    public boolean isFailure() {
        return !isValid();
    }
    
    @Override
    public String toString() {
        return String.format("ValidationResult{valid=%s, errorCount=%d, severity=%s, validationCode='%s'}", 
                valid, errorCount, severity, validationCode);
    }
}