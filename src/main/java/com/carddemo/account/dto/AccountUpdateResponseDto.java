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

package com.carddemo.account.dto;

import com.carddemo.common.enums.ErrorFlag;
import com.carddemo.common.enums.ValidationResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Response DTO for account update operations providing comprehensive confirmation,
 * validation errors, and audit information with structured message handling
 * equivalent to CICS transaction responses from COACTUPC.cbl.
 * 
 * <p>This response DTO maintains complete functional equivalence with COBOL
 * account update transaction processing while providing modern REST API
 * response structure for React frontend consumption and Spring Boot
 * microservices integration.</p>
 * 
 * <p>Key Features:</p>
 * <ul>
 *   <li>Success/failure confirmation with detailed error reporting</li>
 *   <li>Field-level validation error details for React form handling</li>
 *   <li>Audit trail information for compliance and transaction tracking</li>
 *   <li>Session context preservation for stateless REST API patterns</li>
 *   <li>Structured message handling equivalent to CSMSG01Y/CSMSG02Y copybooks</li>
 * </ul>
 * 
 * <p>COBOL Transaction Equivalent:</p>
 * This DTO represents the response structure for COACTUPC transaction which
 * handles account and customer data updates with comprehensive validation,
 * lock management, and audit trail generation as documented in the original
 * COBOL source.
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountUpdateResponseDto {

    /**
     * Primary success indicator for the account update operation.
     * Corresponds to successful completion of COACTUPC transaction
     * without INPUT-ERROR or other failure conditions.
     */
    @NotNull
    private Boolean success;

    /**
     * Primary error message for operation-level failures.
     * Equivalent to WS-RETURN-MSG from COACTUPC COBOL program
     * providing user-friendly error descriptions.
     */
    private String errorMessage;

    /**
     * Account identifier that was updated or attempted to be updated.
     * Preserves the account context for client-side error handling
     * and audit trail correlation.
     */
    private String accountId;

    /**
     * Comprehensive audit trail information for the update operation.
     * Supports compliance requirements per Section 6.2.3.4 and
     * provides transaction tracking equivalent to CICS audit facilities.
     */
    private AuditTrail auditTrail;

    /**
     * Field-level validation errors for detailed error reporting.
     * Maps field names to their corresponding validation results
     * enabling React components to display specific error messages
     * per form field.
     */
    private Map<String, ValidationResult> validationErrors;

    /**
     * Timestamp when the response was generated.
     * Provides precise timing information for audit and debugging
     * purposes with LocalDateTime precision.
     */
    @NotNull
    private LocalDateTime timestamp;

    /**
     * Overall error flag status using COBOL Y/N pattern.
     * Provides boolean-equivalent error state management
     * compatible with original mainframe error handling patterns.
     */
    private ErrorFlag errorFlag;

    /**
     * Session context identifier for stateless API processing.
     * Enables correlation with Redis session storage and
     * supports pseudo-conversational processing patterns.
     */
    private String sessionId;

    /**
     * Transaction status code providing detailed operation outcome.
     * Maps to CICS response codes and Spring transaction status
     * for comprehensive transaction state reporting.
     */
    private String transactionStatus;

    /**
     * List of informational messages for successful operations.
     * Provides user feedback equivalent to CICS information
     * display patterns for successful transaction completion.
     */
    private List<String> informationMessages;

    /**
     * List of warning messages for operations with non-critical issues.
     * Supports partial success scenarios where updates complete
     * but with warnings about data conditions or business rules.
     */
    private List<String> warningMessages;

    /**
     * Additional metadata for the response.
     * Provides extensibility for future requirements while
     * maintaining backward compatibility with existing clients.
     */
    private Map<String, Object> metadata;

    /**
     * Default constructor initializing response with current timestamp
     * and empty collections for validation errors and messages.
     */
    public AccountUpdateResponseDto() {
        this.timestamp = LocalDateTime.now();
        this.validationErrors = new HashMap<>();
        this.informationMessages = new ArrayList<>();
        this.warningMessages = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.errorFlag = ErrorFlag.OFF;
        this.success = false;
    }

    /**
     * Constructor for successful account update response.
     * 
     * @param accountId The account identifier that was successfully updated
     * @param auditTrail Audit trail information for the successful update
     */
    public AccountUpdateResponseDto(String accountId, AuditTrail auditTrail) {
        this();
        this.success = true;
        this.accountId = accountId;
        this.auditTrail = auditTrail;
        this.errorFlag = ErrorFlag.OFF;
        this.transactionStatus = "COMPLETED";
        addInformationMessage("Account update completed successfully");
    }

    /**
     * Constructor for failed account update response.
     * 
     * @param accountId The account identifier that failed to update
     * @param errorMessage Primary error message describing the failure
     * @param validationErrors Map of field-level validation errors
     */
    public AccountUpdateResponseDto(String accountId, String errorMessage, 
                                  Map<String, ValidationResult> validationErrors) {
        this();
        this.success = false;
        this.accountId = accountId;
        this.errorMessage = errorMessage;
        this.validationErrors = validationErrors != null ? validationErrors : new HashMap<>();
        this.errorFlag = ErrorFlag.ON;
        this.transactionStatus = "FAILED";
    }

    /**
     * Checks if the account update operation was successful.
     * 
     * @return true if operation succeeded, false otherwise
     */
    public Boolean isSuccess() {
        return success;
    }

    /**
     * Sets the success status of the account update operation.
     * 
     * @param success true for successful operation, false for failure
     */
    public void setSuccess(Boolean success) {
        this.success = success;
        // Automatically update error flag based on success status
        this.errorFlag = success ? ErrorFlag.OFF : ErrorFlag.ON;
    }

    /**
     * Gets the primary error message for the operation.
     * 
     * @return error message or null if operation was successful
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the primary error message for the operation.
     * 
     * @param errorMessage descriptive error message for user display
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        // Set error flag if error message is provided
        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            this.errorFlag = ErrorFlag.ON;
            this.success = false;
        }
    }

    /**
     * Gets the account identifier associated with this update operation.
     * 
     * @return account ID that was updated or attempted to be updated
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account identifier for this update operation.
     * 
     * @param accountId account ID being updated
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the audit trail information for this update operation.
     * 
     * @return audit trail details or null if not available
     */
    public AuditTrail getAuditTrail() {
        return auditTrail;
    }

    /**
     * Sets the audit trail information for this update operation.
     * 
     * @param auditTrail comprehensive audit trail for compliance tracking
     */
    public void setAuditTrail(AuditTrail auditTrail) {
        this.auditTrail = auditTrail;
    }

    /**
     * Gets the field-level validation errors for this operation.
     * 
     * @return map of field names to validation results
     */
    public Map<String, ValidationResult> getValidationErrors() {
        return validationErrors;
    }

    /**
     * Sets the field-level validation errors for this operation.
     * 
     * @param validationErrors map of field names to validation errors
     */
    public void setValidationErrors(Map<String, ValidationResult> validationErrors) {
        this.validationErrors = validationErrors != null ? validationErrors : new HashMap<>();
        // Set error flag if validation errors exist
        if (!this.validationErrors.isEmpty()) {
            this.errorFlag = ErrorFlag.ON;
            this.success = false;
        }
    }

    /**
     * Gets the timestamp when this response was generated.
     * 
     * @return response generation timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp for this response.
     * 
     * @param timestamp response generation timestamp
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the error flag status for this operation.
     * 
     * @return ErrorFlag indicating presence or absence of errors
     */
    public ErrorFlag getErrorFlag() {
        return errorFlag;
    }

    /**
     * Sets the error flag status for this operation.
     * 
     * @param errorFlag ErrorFlag indicating error state
     */
    public void setErrorFlag(ErrorFlag errorFlag) {
        this.errorFlag = errorFlag;
        // Synchronize success status with error flag
        if (errorFlag != null && errorFlag.isOn()) {
            this.success = false;
        }
    }

    /**
     * Gets the session identifier for this operation.
     * 
     * @return session ID for correlation with Redis session storage
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the session identifier for this operation.
     * 
     * @param sessionId session ID for stateless API processing
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Gets the transaction status code for this operation.
     * 
     * @return transaction status indicating detailed operation outcome
     */
    public String getTransactionStatus() {
        return transactionStatus;
    }

    /**
     * Sets the transaction status code for this operation.
     * 
     * @param transactionStatus detailed transaction outcome status
     */
    public void setTransactionStatus(String transactionStatus) {
        this.transactionStatus = transactionStatus;
    }

    /**
     * Gets the list of informational messages for this operation.
     * 
     * @return list of informational messages for user display
     */
    public List<String> getInformationMessages() {
        return informationMessages;
    }

    /**
     * Sets the list of informational messages for this operation.
     * 
     * @param informationMessages list of information messages
     */
    public void setInformationMessages(List<String> informationMessages) {
        this.informationMessages = informationMessages != null ? informationMessages : new ArrayList<>();
    }

    /**
     * Gets the list of warning messages for this operation.
     * 
     * @return list of warning messages for user display
     */
    public List<String> getWarningMessages() {
        return warningMessages;
    }

    /**
     * Sets the list of warning messages for this operation.
     * 
     * @param warningMessages list of warning messages
     */
    public void setWarningMessages(List<String> warningMessages) {
        this.warningMessages = warningMessages != null ? warningMessages : new ArrayList<>();
    }

    /**
     * Gets the additional metadata for this response.
     * 
     * @return map of additional metadata properties
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Sets the additional metadata for this response.
     * 
     * @param metadata map of additional metadata properties
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    // Utility methods for common operations

    /**
     * Adds a field-level validation error to the response.
     * 
     * @param fieldName name of the field with validation error
     * @param validationResult validation result indicating the specific error
     */
    public void addValidationError(String fieldName, ValidationResult validationResult) {
        if (fieldName != null && validationResult != null && !validationResult.isValid()) {
            this.validationErrors.put(fieldName, validationResult);
            this.errorFlag = ErrorFlag.ON;
            this.success = false;
        }
    }

    /**
     * Adds an informational message to the response.
     * 
     * @param message informational message for user display
     */
    public void addInformationMessage(String message) {
        if (message != null && !message.trim().isEmpty()) {
            this.informationMessages.add(message);
        }
    }

    /**
     * Adds a warning message to the response.
     * 
     * @param message warning message for user display
     */
    public void addWarningMessage(String message) {
        if (message != null && !message.trim().isEmpty()) {
            this.warningMessages.add(message);
        }
    }

    /**
     * Adds metadata property to the response.
     * 
     * @param key metadata property key
     * @param value metadata property value
     */
    public void addMetadata(String key, Object value) {
        if (key != null) {
            this.metadata.put(key, value);
        }
    }

    /**
     * Checks if the response has any validation errors.
     * 
     * @return true if validation errors exist, false otherwise
     */
    public boolean hasValidationErrors() {
        return validationErrors != null && !validationErrors.isEmpty();
    }

    /**
     * Checks if the response has any error conditions.
     * 
     * @return true if errors exist (error flag ON, error message, or validation errors)
     */
    public boolean hasErrors() {
        return errorFlag.isOn() || 
               (errorMessage != null && !errorMessage.trim().isEmpty()) ||
               hasValidationErrors();
    }

    /**
     * Checks if the response has any warning messages.
     * 
     * @return true if warning messages exist, false otherwise
     */
    public boolean hasWarnings() {
        return warningMessages != null && !warningMessages.isEmpty();
    }

    /**
     * Gets the count of validation errors.
     * 
     * @return number of field-level validation errors
     */
    public int getValidationErrorCount() {
        return validationErrors != null ? validationErrors.size() : 0;
    }

    /**
     * Creates a summary message for the response status.
     * 
     * @return summary message describing the overall operation result
     */
    public String getStatusSummary() {
        if (success) {
            if (hasWarnings()) {
                return String.format("Account %s updated successfully with %d warnings", 
                                    accountId != null ? accountId : "N/A", warningMessages.size());
            }
            return String.format("Account %s updated successfully", 
                                accountId != null ? accountId : "N/A");
        } else {
            if (hasValidationErrors()) {
                return String.format("Account update failed with %d validation errors", 
                                    getValidationErrorCount());
            }
            return "Account update failed: " + (errorMessage != null ? errorMessage : "Unknown error");
        }
    }

    /**
     * Static factory method for creating successful response.
     * 
     * @param accountId account identifier that was updated
     * @param userId user who performed the update
     * @param operationType type of update operation performed
     * @return successful account update response
     */
    public static AccountUpdateResponseDto success(String accountId, String userId, String operationType) {
        AuditTrail auditTrail = new AuditTrail(userId, operationType, LocalDateTime.now());
        AccountUpdateResponseDto response = new AccountUpdateResponseDto(accountId, auditTrail);
        response.setTransactionStatus("COMPLETED");
        return response;
    }

    /**
     * Static factory method for creating failure response.
     * 
     * @param accountId account identifier that failed to update
     * @param errorMessage primary error message
     * @return failed account update response
     */
    public static AccountUpdateResponseDto failure(String accountId, String errorMessage) {
        return new AccountUpdateResponseDto(accountId, errorMessage, null);
    }

    /**
     * Static factory method for creating validation error response.
     * 
     * @param accountId account identifier with validation errors
     * @param validationErrors map of field validation errors
     * @return validation error response
     */
    public static AccountUpdateResponseDto validationError(String accountId, 
                                                         Map<String, ValidationResult> validationErrors) {
        return new AccountUpdateResponseDto(accountId, "Validation errors found", validationErrors);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AccountUpdateResponseDto that = (AccountUpdateResponseDto) obj;
        return Objects.equals(success, that.success) &&
               Objects.equals(errorMessage, that.errorMessage) &&
               Objects.equals(accountId, that.accountId) &&
               Objects.equals(auditTrail, that.auditTrail) &&
               Objects.equals(validationErrors, that.validationErrors) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(errorFlag, that.errorFlag) &&
               Objects.equals(sessionId, that.sessionId) &&
               Objects.equals(transactionStatus, that.transactionStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, errorMessage, accountId, auditTrail, validationErrors, 
                           timestamp, errorFlag, sessionId, transactionStatus);
    }

    @Override
    public String toString() {
        return String.format(
            "AccountUpdateResponseDto{" +
            "success=%s, " +
            "accountId='%s', " +
            "errorFlag=%s, " +
            "transactionStatus='%s', " +
            "validationErrorCount=%d, " +
            "timestamp=%s" +
            "}",
            success, accountId, errorFlag, transactionStatus, 
            getValidationErrorCount(), timestamp
        );
    }

    /**
     * Nested class representing comprehensive audit trail information
     * for account update operations supporting compliance requirements.
     */
    public static class AuditTrail {
        
        /**
         * User identifier who performed the update operation.
         */
        private String userId;
        
        /**
         * Type of operation performed (CREATE, UPDATE, DELETE, etc.).
         */
        private String operationType;
        
        /**
         * Timestamp when the operation was performed.
         */
        private LocalDateTime operationTimestamp;
        
        /**
         * Session identifier for correlation with user session.
         */
        private String sessionId;
        
        /**
         * Source IP address of the update request.
         */
        private String sourceIpAddress;
        
        /**
         * User agent information from the request.
         */
        private String userAgent;
        
        /**
         * Transaction identifier for correlation with system logs.
         */
        private String transactionId;
        
        /**
         * Previous values before the update (for audit comparison).
         */
        private Map<String, Object> previousValues;
        
        /**
         * New values after the update (for audit tracking).
         */
        private Map<String, Object> newValues;
        
        /**
         * Additional audit metadata.
         */
        private Map<String, Object> auditMetadata;

        /**
         * Default constructor.
         */
        public AuditTrail() {
            this.previousValues = new HashMap<>();
            this.newValues = new HashMap<>();
            this.auditMetadata = new HashMap<>();
        }

        /**
         * Constructor with essential audit information.
         * 
         * @param userId user performing the operation
         * @param operationType type of operation
         * @param operationTimestamp timestamp of operation
         */
        public AuditTrail(String userId, String operationType, LocalDateTime operationTimestamp) {
            this();
            this.userId = userId;
            this.operationType = operationType;
            this.operationTimestamp = operationTimestamp;
        }

        // Getters and setters for audit trail fields

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getOperationType() {
            return operationType;
        }

        public void setOperationType(String operationType) {
            this.operationType = operationType;
        }

        public LocalDateTime getOperationTimestamp() {
            return operationTimestamp;
        }

        public void setOperationTimestamp(LocalDateTime operationTimestamp) {
            this.operationTimestamp = operationTimestamp;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getSourceIpAddress() {
            return sourceIpAddress;
        }

        public void setSourceIpAddress(String sourceIpAddress) {
            this.sourceIpAddress = sourceIpAddress;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }

        public Map<String, Object> getPreviousValues() {
            return previousValues;
        }

        public void setPreviousValues(Map<String, Object> previousValues) {
            this.previousValues = previousValues != null ? previousValues : new HashMap<>();
        }

        public Map<String, Object> getNewValues() {
            return newValues;
        }

        public void setNewValues(Map<String, Object> newValues) {
            this.newValues = newValues != null ? newValues : new HashMap<>();
        }

        public Map<String, Object> getAuditMetadata() {
            return auditMetadata;
        }

        public void setAuditMetadata(Map<String, Object> auditMetadata) {
            this.auditMetadata = auditMetadata != null ? auditMetadata : new HashMap<>();
        }

        /**
         * Adds a previous value to the audit trail.
         * 
         * @param fieldName name of the field
         * @param value previous value before update
         */
        public void addPreviousValue(String fieldName, Object value) {
            if (fieldName != null) {
                this.previousValues.put(fieldName, value);
            }
        }

        /**
         * Adds a new value to the audit trail.
         * 
         * @param fieldName name of the field
         * @param value new value after update
         */
        public void addNewValue(String fieldName, Object value) {
            if (fieldName != null) {
                this.newValues.put(fieldName, value);
            }
        }

        /**
         * Adds audit metadata.
         * 
         * @param key metadata key
         * @param value metadata value
         */
        public void addAuditMetadata(String key, Object value) {
            if (key != null) {
                this.auditMetadata.put(key, value);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            AuditTrail that = (AuditTrail) obj;
            return Objects.equals(userId, that.userId) &&
                   Objects.equals(operationType, that.operationType) &&
                   Objects.equals(operationTimestamp, that.operationTimestamp) &&
                   Objects.equals(sessionId, that.sessionId) &&
                   Objects.equals(transactionId, that.transactionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, operationType, operationTimestamp, sessionId, transactionId);
        }

        @Override
        public String toString() {
            return String.format(
                "AuditTrail{" +
                "userId='%s', " +
                "operationType='%s', " +
                "operationTimestamp=%s, " +
                "transactionId='%s'" +
                "}",
                userId, operationType, operationTimestamp, transactionId
            );
        }
    }
}