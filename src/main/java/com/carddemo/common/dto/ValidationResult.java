package com.carddemo.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Validation result DTO containing field-level validation messages, business rule 
 * compliance status, and comprehensive error information for API response validation feedback.
 * 
 * This class maintains exact functional equivalence with COBOL validation patterns 
 * while providing modern JSON-based error reporting for React frontend components.
 * Follows enterprise-grade validation patterns required for CardDemo microservices architecture.
 *
 * @version 1.0
 * @since CardDemo Spring Boot Migration
 */
public class ValidationResult {

    /**
     * Enumeration defining validation severity levels for graduated user feedback.
     * Maps to Material-UI alert severity levels for consistent frontend rendering.
     */
    public enum Severity {
        /**
         * Critical validation failures preventing operation completion.
         * Equivalent to COBOL DFHRED error highlighting.
         */
        ERROR,
        
        /**
         * Warning conditions requiring user attention but allowing continuation.
         * Equivalent to COBOL DFHYELLOW field highlighting.
         */
        WARNING,
        
        /**
         * Informational messages providing user guidance.
         * Equivalent to COBOL DFHGREEN informational display.
         */
        INFO
    }

    /**
     * Overall validation status indicating pass/fail for business rule compliance.
     * Preserves COBOL boolean validation semantics with explicit true/false states.
     */
    @JsonProperty("valid")
    private boolean valid;

    /**
     * Collection of field-level error messages for comprehensive user feedback.
     * Maintains insertion order to preserve original COBOL validation sequence.
     */
    @JsonProperty("errorMessages")
    private List<String> errorMessages;

    /**
     * Validation severity level for graduated user feedback display.
     * Enables React components to render appropriate alert styles.
     */
    @JsonProperty("severity")
    private Severity severity;

    /**
     * Standardized error code for consistent API error handling across microservices.
     * Follows COBOL transaction code patterns (e.g., COSGN00, COACTVW) for familiarity.
     */
    @JsonProperty("validationCode")
    private String validationCode;

    /**
     * Default constructor initializing empty validation result.
     * Creates valid state with empty error collection by default.
     */
    public ValidationResult() {
        this.valid = true;
        this.errorMessages = new ArrayList<>();
        this.severity = Severity.INFO;
        this.validationCode = "";
    }

    /**
     * Constructor for creating validation result with initial status and severity.
     * Commonly used for immediate validation failure scenarios.
     *
     * @param valid Initial validation status
     * @param severity Validation severity level
     */
    public ValidationResult(boolean valid, Severity severity) {
        this();
        this.valid = valid;
        this.severity = severity;
    }

    /**
     * Constructor for creating validation result with status, severity, and error code.
     * Provides comprehensive initialization for complex validation scenarios.
     *
     * @param valid Initial validation status
     * @param severity Validation severity level
     * @param validationCode Standardized error code
     */
    public ValidationResult(boolean valid, Severity severity, String validationCode) {
        this(valid, severity);
        this.validationCode = validationCode != null ? validationCode : "";
    }

    /**
     * Gets the overall validation status indicating business rule compliance.
     * 
     * @return true if all validations passed, false if any validation failed
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Sets the overall validation status for business rule compliance tracking.
     * 
     * @param valid Validation status to set
     */
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /**
     * Gets the collection of field-level error messages for user feedback.
     * 
     * @return List of error messages in insertion order
     */
    public List<String> getErrorMessages() {
        return errorMessages;
    }

    /**
     * Sets the collection of field-level error messages.
     * Replaces existing error messages with provided collection.
     * 
     * @param errorMessages New error message collection
     */
    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages != null ? errorMessages : new ArrayList<>();
    }

    /**
     * Adds a single error message to the validation result.
     * Automatically sets validation status to false when error is added.
     * Maintains insertion order for consistent error message sequencing.
     * 
     * @param errorMessage Error message to add
     */
    public void addErrorMessage(String errorMessage) {
        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            this.errorMessages.add(errorMessage.trim());
            this.valid = false;
            
            // Auto-upgrade severity to ERROR when error messages are present
            if (this.severity == Severity.INFO) {
                this.severity = Severity.ERROR;
            }
        }
    }

    /**
     * Checks if validation result contains any error messages.
     * Useful for conditional error handling in service layers.
     * 
     * @return true if error messages exist, false otherwise
     */
    @JsonIgnore
    public boolean hasErrors() {
        return !errorMessages.isEmpty();
    }

    /**
     * Gets the count of error messages for error aggregation statistics.
     * Enables quantitative error reporting for monitoring and logging.
     * 
     * @return Number of error messages in the result
     */
    @JsonIgnore
    public int getErrorCount() {
        return errorMessages.size();
    }

    /**
     * Gets the validation severity level for graduated user feedback.
     * 
     * @return Current validation severity
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Sets the validation severity level for user feedback control.
     * 
     * @param severity Validation severity to set
     */
    public void setSeverity(Severity severity) {
        this.severity = severity != null ? severity : Severity.INFO;
    }

    /**
     * Gets the standardized error code for consistent API error handling.
     * 
     * @return Current validation error code
     */
    public String getValidationCode() {
        return validationCode;
    }

    /**
     * Sets the standardized error code for API error handling consistency.
     * 
     * @param validationCode Validation error code to set
     */
    public void setValidationCode(String validationCode) {
        this.validationCode = validationCode != null ? validationCode : "";
    }

    /**
     * Merges another validation result into this one, combining error messages
     * and updating status appropriately. Maintains error message ordering.
     * Useful for aggregating validation results from multiple business rules.
     * 
     * @param other ValidationResult to merge into this one
     */
    public void merge(ValidationResult other) {
        if (other != null) {
            // Merge error messages while preserving order
            this.errorMessages.addAll(other.getErrorMessages());
            
            // Update validation status - fails if either result fails
            this.valid = this.valid && other.isValid();
            
            // Escalate severity to highest level
            if (other.getSeverity() == Severity.ERROR || this.severity == Severity.ERROR) {
                this.severity = Severity.ERROR;
            } else if (other.getSeverity() == Severity.WARNING || this.severity == Severity.WARNING) {
                this.severity = Severity.WARNING;
            }
            
            // Preserve validation code from first non-empty result
            if (this.validationCode.isEmpty() && !other.getValidationCode().isEmpty()) {
                this.validationCode = other.getValidationCode();
            }
        }
    }

    /**
     * Creates a successful validation result with INFO severity.
     * Utility method for creating positive validation outcomes.
     * 
     * @return ValidationResult indicating successful validation
     */
    public static ValidationResult success() {
        return new ValidationResult(true, Severity.INFO);
    }

    /**
     * Creates a failed validation result with ERROR severity and error message.
     * Utility method for creating validation failures with immediate feedback.
     * 
     * @param errorMessage Error message describing validation failure
     * @return ValidationResult indicating validation failure
     */
    public static ValidationResult failure(String errorMessage) {
        ValidationResult result = new ValidationResult(false, Severity.ERROR);
        result.addErrorMessage(errorMessage);
        return result;
    }

    /**
     * Creates a failed validation result with ERROR severity, error message, and code.
     * Utility method for creating comprehensive validation failures.
     * 
     * @param errorMessage Error message describing validation failure
     * @param validationCode Standardized error code
     * @return ValidationResult indicating validation failure with code
     */
    public static ValidationResult failure(String errorMessage, String validationCode) {
        ValidationResult result = new ValidationResult(false, Severity.ERROR, validationCode);
        result.addErrorMessage(errorMessage);
        return result;
    }

    /**
     * Creates a warning validation result that passes but requires user attention.
     * Utility method for creating warning conditions allowing operation continuation.
     * 
     * @param warningMessage Warning message for user attention
     * @return ValidationResult indicating warning condition
     */
    public static ValidationResult warning(String warningMessage) {
        ValidationResult result = new ValidationResult(true, Severity.WARNING);
        if (warningMessage != null && !warningMessage.trim().isEmpty()) {
            result.getErrorMessages().add(warningMessage.trim());
        }
        return result;
    }

    /**
     * Provides string representation of validation result for logging and debugging.
     * Includes validation status, error count, severity, and validation code.
     * 
     * @return String representation of validation state
     */
    @Override
    public String toString() {
        return String.format("ValidationResult{valid=%s, errorCount=%d, severity=%s, code='%s'}", 
                           valid, getErrorCount(), severity, validationCode);
    }

    /**
     * Checks equality based on validation status, error messages, severity, and code.
     * Enables proper validation result comparison for testing and business logic.
     * 
     * @param obj Object to compare with
     * @return true if validation results are equivalent
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ValidationResult that = (ValidationResult) obj;
        return valid == that.valid &&
               errorMessages.equals(that.errorMessages) &&
               severity == that.severity &&
               validationCode.equals(that.validationCode);
    }

    /**
     * Generates hash code based on validation components for proper collection behavior.
     * 
     * @return Hash code for validation result
     */
    @Override
    public int hashCode() {
        int result = Boolean.hashCode(valid);
        result = 31 * result + errorMessages.hashCode();
        result = 31 * result + severity.hashCode();
        result = 31 * result + validationCode.hashCode();
        return result;
    }
}