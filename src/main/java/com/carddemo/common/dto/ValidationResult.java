package com.carddemo.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Validation result DTO containing field-level validation messages, business rule compliance status,
 * and comprehensive error information for API response validation feedback.
 * 
 * This class provides structured validation results that maintain compatibility with the original
 * COBOL/BMS field validation patterns while supporting modern REST API error response standards.
 * 
 * Key Features:
 * - Field-level error message collection with severity categorization
 * - Business rule compliance status tracking
 * - Standardized error codes for consistent API error handling
 * - JSON serialization support for React frontend integration
 * - Helper methods for validation status checking and error aggregation
 */
public class ValidationResult {
    
    /**
     * Enumeration defining validation severity levels for graduated user feedback.
     * Matches the original BMS attribute patterns for error display.
     */
    public enum Severity {
        /** Critical errors that prevent processing - equivalent to DFHRED highlighting */
        ERROR,
        /** Warning conditions that allow processing but require attention */
        WARNING,
        /** Informational messages providing guidance or status updates */
        INFO
    }
    
    /**
     * Individual validation error message containing field-specific error details.
     * Maintains field-level granularity equivalent to BMS field attribute validation.
     */
    public static class ValidationError {
        @JsonProperty("field_name")
        private String fieldName;
        
        @JsonProperty("error_code")
        private String errorCode;
        
        @JsonProperty("error_message")
        private String errorMessage;
        
        @JsonProperty("severity")
        private Severity severity;
        
        // Default constructor for JSON deserialization
        public ValidationError() {}
        
        /**
         * Constructs a validation error with all required details.
         * 
         * @param fieldName The name of the field that failed validation
         * @param errorCode Standardized error code for programmatic handling
         * @param errorMessage Human-readable error description
         * @param severity Severity level for UI display prioritization
         */
        public ValidationError(String fieldName, String errorCode, String errorMessage, Severity severity) {
            this.fieldName = fieldName;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.severity = severity;
        }
        
        // Getters and setters
        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName; }
        
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public Severity getSeverity() { return severity; }
        public void setSeverity(Severity severity) { this.severity = severity; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ValidationError that = (ValidationError) o;
            return Objects.equals(fieldName, that.fieldName) &&
                   Objects.equals(errorCode, that.errorCode) &&
                   Objects.equals(errorMessage, that.errorMessage) &&
                   severity == that.severity;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(fieldName, errorCode, errorMessage, severity);
        }
        
        @Override
        public String toString() {
            return String.format("ValidationError{fieldName='%s', errorCode='%s', message='%s', severity=%s}",
                    fieldName, errorCode, errorMessage, severity);
        }
    }
    
    // Main ValidationResult properties
    
    /** Overall validation status - true if all validations passed */
    @JsonProperty("is_valid")
    private boolean valid;
    
    /** Collection of field-level validation errors */
    @JsonProperty("error_messages")
    private List<ValidationError> errorMessages;
    
    /** Overall validation severity level - highest severity among all errors */
    @JsonProperty("severity")
    private Severity severity;
    
    /** Standardized validation result code for programmatic handling */
    @JsonProperty("validation_code")
    private String validationCode;
    
    /** Business rule compliance status indicator */
    @JsonProperty("business_rule_compliance")
    private boolean businessRuleCompliance;
    
    /** Additional context information for validation results */
    @JsonProperty("validation_context")
    private String validationContext;
    
    /**
     * Default constructor initializing validation result as valid with empty error list.
     * Follows the "valid until proven invalid" pattern from COBOL validation logic.
     */
    public ValidationResult() {
        this.valid = true;
        this.errorMessages = new ArrayList<>();
        this.severity = Severity.INFO;
        this.businessRuleCompliance = true;
        this.validationCode = "VALIDATION_SUCCESS";
    }
    
    /**
     * Constructor for creating validation result with specific status.
     * 
     * @param valid Initial validation status
     */
    public ValidationResult(boolean valid) {
        this();
        this.valid = valid;
        if (!valid) {
            this.validationCode = "VALIDATION_FAILED";
            this.businessRuleCompliance = false;
        }
    }
    
    // Core validation status methods
    
    /**
     * Returns the overall validation status.
     * 
     * @return true if all validations passed, false otherwise
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Sets the overall validation status.
     * 
     * @param valid The validation status to set
     */
    public void setValid(boolean valid) {
        this.valid = valid;
        if (!valid && "VALIDATION_SUCCESS".equals(this.validationCode)) {
            this.validationCode = "VALIDATION_FAILED";
        }
    }
    
    /**
     * Returns the list of validation error messages.
     * 
     * @return List of ValidationError objects containing field-level errors
     */
    public List<ValidationError> getErrorMessages() {
        return errorMessages;
    }
    
    /**
     * Sets the list of validation error messages.
     * 
     * @param errorMessages List of validation errors to set
     */
    public void setErrorMessages(List<ValidationError> errorMessages) {
        this.errorMessages = errorMessages != null ? errorMessages : new ArrayList<>();
        updateValidationStatus();
    }
    
    /**
     * Adds a single error message to the validation result.
     * Automatically updates validation status and severity levels.
     * 
     * @param fieldName The name of the field that failed validation
     * @param errorCode Standardized error code
     * @param errorMessage Human-readable error description
     * @param severity Severity level for the error
     */
    public void addErrorMessage(String fieldName, String errorCode, String errorMessage, Severity severity) {
        ValidationError error = new ValidationError(fieldName, errorCode, errorMessage, severity);
        this.errorMessages.add(error);
        updateValidationStatus();
    }
    
    /**
     * Adds a validation error object to the result.
     * 
     * @param error The ValidationError to add
     */
    public void addErrorMessage(ValidationError error) {
        if (error != null) {
            this.errorMessages.add(error);
            updateValidationStatus();
        }
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
     * Returns the total number of validation errors.
     * 
     * @return Count of validation errors
     */
    public int getErrorCount() {
        return errorMessages.size();
    }
    
    /**
     * Returns the overall validation severity level.
     * This represents the highest severity among all validation errors.
     * 
     * @return The overall severity level
     */
    public Severity getSeverity() {
        return severity;
    }
    
    /**
     * Sets the overall validation severity level.
     * 
     * @param severity The severity level to set
     */
    public void setSeverity(Severity severity) {
        this.severity = severity;
    }
    
    /**
     * Returns the standardized validation code.
     * 
     * @return Validation code for programmatic handling
     */
    public String getValidationCode() {
        return validationCode;
    }
    
    /**
     * Sets the standardized validation code.
     * 
     * @param validationCode The validation code to set
     */
    public void setValidationCode(String validationCode) {
        this.validationCode = validationCode;
    }
    
    /**
     * Returns the business rule compliance status.
     * 
     * @return true if business rules are satisfied, false otherwise
     */
    public boolean isBusinessRuleCompliance() {
        return businessRuleCompliance;
    }
    
    /**
     * Sets the business rule compliance status.
     * 
     * @param businessRuleCompliance The compliance status to set
     */
    public void setBusinessRuleCompliance(boolean businessRuleCompliance) {
        this.businessRuleCompliance = businessRuleCompliance;
    }
    
    /**
     * Returns the validation context information.
     * 
     * @return Context information for the validation
     */
    public String getValidationContext() {
        return validationContext;
    }
    
    /**
     * Sets the validation context information.
     * 
     * @param validationContext Context information to set
     */
    public void setValidationContext(String validationContext) {
        this.validationContext = validationContext;
    }
    
    // Helper methods for validation status checking and error aggregation
    
    /**
     * Checks if there are any errors of the specified severity level.
     * 
     * @param severity The severity level to check for
     * @return true if errors of the specified severity exist
     */
    public boolean hasErrorsOfSeverity(Severity severity) {
        return errorMessages.stream()
                .anyMatch(error -> error.getSeverity() == severity);
    }
    
    /**
     * Returns the count of errors for a specific severity level.
     * 
     * @param severity The severity level to count
     * @return Number of errors with the specified severity
     */
    public long getErrorCountBySeverity(Severity severity) {
        return errorMessages.stream()
                .filter(error -> error.getSeverity() == severity)
                .count();
    }
    
    /**
     * Returns all error messages for a specific field.
     * 
     * @param fieldName The field name to filter by
     * @return List of ValidationError objects for the specified field
     */
    public List<ValidationError> getErrorsForField(String fieldName) {
        return errorMessages.stream()
                .filter(error -> Objects.equals(error.getFieldName(), fieldName))
                .toList();
    }
    
    /**
     * Checks if a specific field has validation errors.
     * 
     * @param fieldName The field name to check
     * @return true if the field has validation errors
     */
    public boolean hasFieldErrors(String fieldName) {
        return errorMessages.stream()
                .anyMatch(error -> Objects.equals(error.getFieldName(), fieldName));
    }
    
    /**
     * Merges validation results from another ValidationResult into this one.
     * Useful for aggregating validation results from multiple validation steps.
     * 
     * @param other Another ValidationResult to merge
     */
    public void merge(ValidationResult other) {
        if (other != null && other.hasErrors()) {
            this.errorMessages.addAll(other.getErrorMessages());
            updateValidationStatus();
            
            // Update business rule compliance if the other result failed compliance
            if (!other.isBusinessRuleCompliance()) {
                this.businessRuleCompliance = false;
            }
        }
    }
    
    /**
     * Clears all validation errors and resets the result to valid state.
     */
    public void clearErrors() {
        this.errorMessages.clear();
        this.valid = true;
        this.severity = Severity.INFO;
        this.validationCode = "VALIDATION_SUCCESS";
        this.businessRuleCompliance = true;
    }
    
    /**
     * Creates a summary string of all validation errors.
     * Useful for logging and debugging purposes.
     * 
     * @return String summary of validation errors
     */
    public String getErrorSummary() {
        if (errorMessages.isEmpty()) {
            return "No validation errors";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Validation failed with %d error(s):\n", errorMessages.size()));
        
        for (ValidationError error : errorMessages) {
            summary.append(String.format("- [%s] %s: %s (%s)\n", 
                    error.getSeverity(), 
                    error.getFieldName(), 
                    error.getErrorMessage(), 
                    error.getErrorCode()));
        }
        
        return summary.toString();
    }
    
    /**
     * Updates the overall validation status based on current error messages.
     * This method is called automatically when errors are added or modified.
     */
    private void updateValidationStatus() {
        // Update valid status
        this.valid = errorMessages.isEmpty();
        
        // Update overall severity to the highest severity among all errors
        this.severity = errorMessages.stream()
                .map(ValidationError::getSeverity)
                .max((s1, s2) -> s1.ordinal() - s2.ordinal())
                .orElse(Severity.INFO);
        
        // Update business rule compliance - false if any ERROR severity exists
        this.businessRuleCompliance = !hasErrorsOfSeverity(Severity.ERROR);
        
        // Update validation code based on error presence and severity
        if (errorMessages.isEmpty()) {
            this.validationCode = "VALIDATION_SUCCESS";
        } else if (hasErrorsOfSeverity(Severity.ERROR)) {
            this.validationCode = "VALIDATION_FAILED_ERRORS";
        } else if (hasErrorsOfSeverity(Severity.WARNING)) {
            this.validationCode = "VALIDATION_PASSED_WITH_WARNINGS";
        } else {
            this.validationCode = "VALIDATION_PASSED_WITH_INFO";
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationResult result = (ValidationResult) o;
        return valid == result.valid &&
               businessRuleCompliance == result.businessRuleCompliance &&
               Objects.equals(errorMessages, result.errorMessages) &&
               severity == result.severity &&
               Objects.equals(validationCode, result.validationCode) &&
               Objects.equals(validationContext, result.validationContext);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(valid, errorMessages, severity, validationCode, businessRuleCompliance, validationContext);
    }
    
    @Override
    public String toString() {
        return String.format("ValidationResult{valid=%s, errorCount=%d, severity=%s, code='%s', businessCompliance=%s}",
                valid, errorMessages.size(), severity, validationCode, businessRuleCompliance);
    }
}