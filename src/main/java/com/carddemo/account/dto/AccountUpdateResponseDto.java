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

package com.carddemo.account.dto;

import com.carddemo.common.enums.ValidationResult;
import com.carddemo.common.enums.ErrorFlag;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Response Data Transfer Object for account update operations in the CardDemo application.
 * This DTO provides comprehensive confirmation, validation error handling, and audit trail
 * information with structured message handling equivalent to CICS transaction responses.
 * 
 * <p>The response structure transforms legacy COBOL CICS transaction patterns to modern
 * REST API responses while maintaining identical business logic and validation behavior.
 * All response elements support React Hook Form error handling and Material-UI component
 * integration for seamless frontend user experience.</p>
 * 
 * <p>COBOL Program Equivalence:</p>
 * <ul>
 *   <li>Maps to COACTUPC.cbl return message handling (WS-RETURN-MSG)</li>
 *   <li>Preserves CICS response code patterns (WS-RESP-CD, WS-REAS-CD)</li>
 *   <li>Maintains CSMSG01Y and CSMSG02Y message structure compatibility</li>
 *   <li>Provides equivalent validation feedback as BMS field attribute processing</li>
 * </ul>
 * 
 * <p>Frontend Integration:</p>
 * <ul>
 *   <li>Structured error responses support React component error state management</li>
 *   <li>Field-level validation errors enable Material-UI TextField error highlighting</li>
 *   <li>Success confirmation provides immediate user feedback for completed operations</li>
 *   <li>Audit trail information supports compliance requirements and user transparency</li>
 * </ul>
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountUpdateResponseDto {

    /**
     * Indicates whether the account update operation completed successfully.
     * Equivalent to COBOL condition checking for successful CICS transaction completion.
     * 
     * <p>Success criteria:</p>
     * <ul>
     *   <li>All field validations passed (ValidationResult.VALID)</li>
     *   <li>Database transaction committed successfully</li>
     *   <li>No concurrent update conflicts detected</li>
     *   <li>Both account and customer records updated without errors</li>
     * </ul>
     */
    @NotNull
    private boolean success;

    /**
     * Primary error message for display to end users when update operation fails.
     * Equivalent to COBOL WS-RETURN-MSG from COACTUPC.cbl error handling logic.
     * 
     * <p>Message types include:</p>
     * <ul>
     *   <li>Record locking failures (equivalent to COULD-NOT-LOCK-ACCT-FOR-UPDATE)</li>
     *   <li>Data validation errors (equivalent to INPUT-ERROR conditions)</li>
     *   <li>Concurrent update conflicts (equivalent to DATA-WAS-CHANGED-BEFORE-UPDATE)</li>
     *   <li>Database constraint violations from PostgreSQL foreign key enforcement</li>
     * </ul>
     */
    private String errorMessage;

    /**
     * Account identifier for the updated account record.
     * Provides confirmation of which account was processed and supports audit trail correlation.
     * Format matches COBOL ACCT-ID field structure (11-digit numeric string).
     */
    @NotNull
    private String accountId;

    /**
     * Comprehensive audit trail information capturing all update operation details.
     * Supports compliance requirements from Section 6.2.3.4 update audit trail requirements
     * and provides transparency for user verification of completed changes.
     */
    private AuditTrailInfo auditTrail;

    /**
     * Field-level validation errors mapped by field name to ValidationResult.
     * Enables React Hook Form integration with precise error highlighting for
     * individual form fields that failed validation.
     * 
     * <p>Field mappings include:</p>
     * <ul>
     *   <li>Account fields: activeStatus, creditLimit, openDate, etc.</li>
     *   <li>Customer fields: firstName, lastName, address, phone, etc.</li>
     *   <li>Cross-reference validation: state-zip consistency, account-customer linkage</li>
     * </ul>
     */
    private Map<String, ValidationResult> validationErrors;

    /**
     * Response generation timestamp for audit trail and client-side cache management.
     * Provides precise timing information for update operation completion.
     */
    @NotNull
    private LocalDateTime timestamp;

    /**
     * List of informational messages for successful operations or warnings.
     * Equivalent to COBOL informational message handling from CSMSG02Y structures.
     * Supports user notification of partial success scenarios or important updates.
     */
    private List<String> informationalMessages;

    /**
     * Session context identifier for stateless REST API session correlation.
     * Links the response to the user session in Redis for pseudo-conversational processing
     * equivalent to CICS terminal session management.
     */
    private String sessionId;

    /**
     * Transaction status information for Spring Boot transaction management correlation.
     * Provides visibility into database transaction completion status and supports
     * debugging of complex multi-table update scenarios.
     */
    private TransactionStatusInfo transactionStatus;

    /**
     * Error flag indicating presence of any error conditions.
     * Implements ErrorFlag enum pattern for consistent error state management
     * equivalent to COBOL Y/N error flag processing.
     */
    private ErrorFlag errorFlag;

    /**
     * Default constructor initializing response with timestamp and empty collections.
     * Establishes baseline state for successful response building.
     */
    public AccountUpdateResponseDto() {
        this.timestamp = LocalDateTime.now();
        this.validationErrors = new HashMap<>();
        this.informationalMessages = new ArrayList<>();
        this.errorFlag = ErrorFlag.OFF;
    }

    /**
     * Constructor for successful update response with account ID confirmation.
     * 
     * @param accountId the account identifier that was successfully updated
     */
    public AccountUpdateResponseDto(String accountId) {
        this();
        this.success = true;
        this.accountId = accountId;
        this.errorFlag = ErrorFlag.OFF;
    }

    /**
     * Constructor for failed update response with error message and account ID.
     * 
     * @param accountId the account identifier that failed to update
     * @param errorMessage the primary error message for user display
     */
    public AccountUpdateResponseDto(String accountId, String errorMessage) {
        this();
        this.success = false;
        this.accountId = accountId;
        this.errorMessage = errorMessage;
        this.errorFlag = ErrorFlag.ON;
    }

    /**
     * Determines if the account update operation was successful.
     * 
     * @return true if the update completed successfully, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Sets the success status of the account update operation.
     * Automatically updates the error flag based on success status.
     * 
     * @param success the success status to set
     */
    public void setSuccess(boolean success) {
        this.success = success;
        this.errorFlag = success ? ErrorFlag.OFF : ErrorFlag.ON;
    }

    /**
     * Gets the primary error message for user display.
     * 
     * @return the error message, or null if no error occurred
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the primary error message for failed update operations.
     * 
     * @param errorMessage the error message to set
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            this.success = false;
            this.errorFlag = ErrorFlag.ON;
        }
    }

    /**
     * Gets the account identifier for the update operation.
     * 
     * @return the account ID
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account identifier for the update operation.
     * 
     * @param accountId the account ID to set
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the comprehensive audit trail information.
     * 
     * @return the audit trail information, or null if not available
     */
    public AuditTrailInfo getAuditTrail() {
        return auditTrail;
    }

    /**
     * Sets the audit trail information for the update operation.
     * 
     * @param auditTrail the audit trail information to set
     */
    public void setAuditTrail(AuditTrailInfo auditTrail) {
        this.auditTrail = auditTrail;
    }

    /**
     * Gets the field-level validation errors map.
     * 
     * @return map of field names to validation results
     */
    public Map<String, ValidationResult> getValidationErrors() {
        return validationErrors;
    }

    /**
     * Sets the field-level validation errors.
     * Automatically updates success status and error flag if validation errors are present.
     * 
     * @param validationErrors the validation errors map to set
     */
    public void setValidationErrors(Map<String, ValidationResult> validationErrors) {
        this.validationErrors = validationErrors != null ? validationErrors : new HashMap<>();
        
        // Update success status based on validation errors
        if (!this.validationErrors.isEmpty()) {
            boolean hasErrors = this.validationErrors.values().stream()
                    .anyMatch(result -> !result.isValid());
            if (hasErrors) {
                this.success = false;
                this.errorFlag = ErrorFlag.ON;
            }
        }
    }

    /**
     * Gets the response generation timestamp.
     * 
     * @return the timestamp when this response was created
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the response generation timestamp.
     * 
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the list of informational messages.
     * 
     * @return list of informational messages
     */
    public List<String> getInformationalMessages() {
        return informationalMessages;
    }

    /**
     * Sets the list of informational messages.
     * 
     * @param informationalMessages the informational messages to set
     */
    public void setInformationalMessages(List<String> informationalMessages) {
        this.informationalMessages = informationalMessages != null ? informationalMessages : new ArrayList<>();
    }

    /**
     * Gets the session context identifier.
     * 
     * @return the session ID for Redis session correlation
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the session context identifier.
     * 
     * @param sessionId the session ID to set
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Gets the transaction status information.
     * 
     * @return the transaction status information
     */
    public TransactionStatusInfo getTransactionStatus() {
        return transactionStatus;
    }

    /**
     * Sets the transaction status information.
     * 
     * @param transactionStatus the transaction status to set
     */
    public void setTransactionStatus(TransactionStatusInfo transactionStatus) {
        this.transactionStatus = transactionStatus;
    }

    /**
     * Gets the error flag indicating presence of error conditions.
     * 
     * @return the error flag state
     */
    public ErrorFlag getErrorFlag() {
        return errorFlag;
    }

    /**
     * Sets the error flag state.
     * 
     * @param errorFlag the error flag to set
     */
    public void setErrorFlag(ErrorFlag errorFlag) {
        this.errorFlag = errorFlag;
    }

    /**
     * Adds a field-level validation error to the response.
     * Convenience method for building validation error collection during processing.
     * 
     * @param fieldName the name of the field that failed validation
     * @param validationResult the validation result for the field
     */
    public void addValidationError(String fieldName, ValidationResult validationResult) {
        if (this.validationErrors == null) {
            this.validationErrors = new HashMap<>();
        }
        this.validationErrors.put(fieldName, validationResult);
        
        if (!validationResult.isValid()) {
            this.success = false;
            this.errorFlag = ErrorFlag.ON;
        }
    }

    /**
     * Adds an informational message to the response.
     * Supports building collections of informational feedback during processing.
     * 
     * @param message the informational message to add
     */
    public void addInformationalMessage(String message) {
        if (this.informationalMessages == null) {
            this.informationalMessages = new ArrayList<>();
        }
        this.informationalMessages.add(message);
    }

    /**
     * Determines if the response contains any validation errors.
     * 
     * @return true if validation errors are present, false otherwise
     */
    public boolean hasValidationErrors() {
        return validationErrors != null && !validationErrors.isEmpty() &&
               validationErrors.values().stream().anyMatch(result -> !result.isValid());
    }

    /**
     * Determines if the response contains informational messages.
     * 
     * @return true if informational messages are present, false otherwise
     */
    public boolean hasInformationalMessages() {
        return informationalMessages != null && !informationalMessages.isEmpty();
    }

    /**
     * Gets the total count of validation errors.
     * 
     * @return the number of fields with validation errors
     */
    public int getValidationErrorCount() {
        if (validationErrors == null) {
            return 0;
        }
        return (int) validationErrors.values().stream()
                .filter(result -> !result.isValid())
                .count();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AccountUpdateResponseDto that = (AccountUpdateResponseDto) obj;
        return success == that.success &&
               Objects.equals(accountId, that.accountId) &&
               Objects.equals(errorMessage, that.errorMessage) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(validationErrors, that.validationErrors) &&
               Objects.equals(auditTrail, that.auditTrail) &&
               Objects.equals(informationalMessages, that.informationalMessages) &&
               Objects.equals(sessionId, that.sessionId) &&
               Objects.equals(transactionStatus, that.transactionStatus) &&
               errorFlag == that.errorFlag;
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, accountId, errorMessage, timestamp, validationErrors,
                           auditTrail, informationalMessages, sessionId, transactionStatus, errorFlag);
    }

    @Override
    public String toString() {
        return Objects.toString(this, 
            "AccountUpdateResponseDto{" +
            "success=" + success +
            ", accountId='" + accountId + "'" +
            ", errorMessage='" + errorMessage + "'" +
            ", timestamp=" + timestamp +
            ", validationErrorCount=" + getValidationErrorCount() +
            ", informationalMessageCount=" + (informationalMessages != null ? informationalMessages.size() : 0) +
            ", sessionId='" + sessionId + "'" +
            ", errorFlag=" + errorFlag +
            "}");
    }

    /**
     * Audit trail information for update operations supporting compliance requirements
     * and providing detailed tracking of all changes made during the update process.
     */
    public static class AuditTrailInfo {
        
        /**
         * Username of the user who performed the update operation.
         * Correlates with Spring Security authentication context.
         */
        private String updatedBy;
        
        /**
         * Timestamp when the update operation was initiated.
         */
        private LocalDateTime updateTimestamp;
        
        /**
         * List of specific fields that were modified during the update.
         * Provides granular change tracking for audit compliance.
         */
        private List<String> modifiedFields;
        
        /**
         * Before and after values for critical fields.
         * Supports audit trail requirements and change verification.
         */
        private Map<String, String> changeDetails;
        
        /**
         * Database transaction identifier for correlation with database logs.
         */
        private String transactionId;
        
        /**
         * IP address of the client that initiated the update request.
         */
        private String clientIpAddress;
        
        /**
         * Additional metadata for compliance and debugging purposes.
         */
        private Map<String, String> metadata;

        /**
         * Default constructor initializing collections.
         */
        public AuditTrailInfo() {
            this.updateTimestamp = LocalDateTime.now();
            this.modifiedFields = new ArrayList<>();
            this.changeDetails = new HashMap<>();
            this.metadata = new HashMap<>();
        }

        // Getters and setters for all fields
        
        public String getUpdatedBy() {
            return updatedBy;
        }

        public void setUpdatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
        }

        public LocalDateTime getUpdateTimestamp() {
            return updateTimestamp;
        }

        public void setUpdateTimestamp(LocalDateTime updateTimestamp) {
            this.updateTimestamp = updateTimestamp;
        }

        public List<String> getModifiedFields() {
            return modifiedFields;
        }

        public void setModifiedFields(List<String> modifiedFields) {
            this.modifiedFields = modifiedFields != null ? modifiedFields : new ArrayList<>();
        }

        public Map<String, String> getChangeDetails() {
            return changeDetails;
        }

        public void setChangeDetails(Map<String, String> changeDetails) {
            this.changeDetails = changeDetails != null ? changeDetails : new HashMap<>();
        }

        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }

        public String getClientIpAddress() {
            return clientIpAddress;
        }

        public void setClientIpAddress(String clientIpAddress) {
            this.clientIpAddress = clientIpAddress;
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, String> metadata) {
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }

        /**
         * Adds a modified field to the audit trail.
         * 
         * @param fieldName the name of the field that was modified
         */
        public void addModifiedField(String fieldName) {
            if (this.modifiedFields == null) {
                this.modifiedFields = new ArrayList<>();
            }
            if (!this.modifiedFields.contains(fieldName)) {
                this.modifiedFields.add(fieldName);
            }
        }

        /**
         * Adds change detail information for a specific field.
         * 
         * @param fieldName the name of the field
         * @param changeInfo the before/after change information
         */
        public void addChangeDetail(String fieldName, String changeInfo) {
            if (this.changeDetails == null) {
                this.changeDetails = new HashMap<>();
            }
            this.changeDetails.put(fieldName, changeInfo);
        }

        /**
         * Adds metadata information to the audit trail.
         * 
         * @param key the metadata key
         * @param value the metadata value
         */
        public void addMetadata(String key, String value) {
            if (this.metadata == null) {
                this.metadata = new HashMap<>();
            }
            this.metadata.put(key, value);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            AuditTrailInfo that = (AuditTrailInfo) obj;
            return Objects.equals(updatedBy, that.updatedBy) &&
                   Objects.equals(updateTimestamp, that.updateTimestamp) &&
                   Objects.equals(modifiedFields, that.modifiedFields) &&
                   Objects.equals(changeDetails, that.changeDetails) &&
                   Objects.equals(transactionId, that.transactionId) &&
                   Objects.equals(clientIpAddress, that.clientIpAddress) &&
                   Objects.equals(metadata, that.metadata);
        }

        @Override
        public int hashCode() {
            return Objects.hash(updatedBy, updateTimestamp, modifiedFields, changeDetails,
                               transactionId, clientIpAddress, metadata);
        }

        @Override
        public String toString() {
            return Objects.toString(this,
                "AuditTrailInfo{" +
                "updatedBy='" + updatedBy + "'" +
                ", updateTimestamp=" + updateTimestamp +
                ", modifiedFieldCount=" + (modifiedFields != null ? modifiedFields.size() : 0) +
                ", changeDetailCount=" + (changeDetails != null ? changeDetails.size() : 0) +
                ", transactionId='" + transactionId + "'" +
                ", clientIpAddress='" + clientIpAddress + "'" +
                ", metadataCount=" + (metadata != null ? metadata.size() : 0) +
                "}");
        }
    }

    /**
     * Transaction status information for Spring Boot transaction management correlation.
     * Provides detailed information about database transaction processing and completion.
     */
    public static class TransactionStatusInfo {
        
        /**
         * Spring transaction identifier for correlation with transaction logs.
         */
        private String springTransactionId;
        
        /**
         * Database transaction status (COMMITTED, ROLLED_BACK, ACTIVE, etc.).
         */
        private String transactionStatus;
        
        /**
         * Transaction isolation level used for the update operation.
         */
        private String isolationLevel;
        
        /**
         * Indicates whether the transaction was read-only.
         */
        private boolean readOnly;
        
        /**
         * Transaction timeout value in seconds.
         */
        private int timeoutSeconds;
        
        /**
         * Number of database operations performed within the transaction.
         */
        private int operationCount;
        
        /**
         * Transaction processing duration in milliseconds.
         */
        private long processingTimeMs;

        /**
         * Default constructor with default values.
         */
        public TransactionStatusInfo() {
            this.readOnly = false;
            this.timeoutSeconds = 300; // 5 minutes default
            this.operationCount = 0;
            this.processingTimeMs = 0L;
        }

        // Getters and setters for all fields
        
        public String getSpringTransactionId() {
            return springTransactionId;
        }

        public void setSpringTransactionId(String springTransactionId) {
            this.springTransactionId = springTransactionId;
        }

        public String getTransactionStatus() {
            return transactionStatus;
        }

        public void setTransactionStatus(String transactionStatus) {
            this.transactionStatus = transactionStatus;
        }

        public String getIsolationLevel() {
            return isolationLevel;
        }

        public void setIsolationLevel(String isolationLevel) {
            this.isolationLevel = isolationLevel;
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        public void setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public int getOperationCount() {
            return operationCount;
        }

        public void setOperationCount(int operationCount) {
            this.operationCount = operationCount;
        }

        public long getProcessingTimeMs() {
            return processingTimeMs;
        }

        public void setProcessingTimeMs(long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            TransactionStatusInfo that = (TransactionStatusInfo) obj;
            return readOnly == that.readOnly &&
                   timeoutSeconds == that.timeoutSeconds &&
                   operationCount == that.operationCount &&
                   processingTimeMs == that.processingTimeMs &&
                   Objects.equals(springTransactionId, that.springTransactionId) &&
                   Objects.equals(transactionStatus, that.transactionStatus) &&
                   Objects.equals(isolationLevel, that.isolationLevel);
        }

        @Override
        public int hashCode() {
            return Objects.hash(springTransactionId, transactionStatus, isolationLevel,
                               readOnly, timeoutSeconds, operationCount, processingTimeMs);
        }

        @Override
        public String toString() {
            return Objects.toString(this,
                "TransactionStatusInfo{" +
                "springTransactionId='" + springTransactionId + "'" +
                ", transactionStatus='" + transactionStatus + "'" +
                ", isolationLevel='" + isolationLevel + "'" +
                ", readOnly=" + readOnly +
                ", timeoutSeconds=" + timeoutSeconds +
                ", operationCount=" + operationCount +
                ", processingTimeMs=" + processingTimeMs +
                "}");
        }
    }
}