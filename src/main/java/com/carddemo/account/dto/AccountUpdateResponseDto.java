/*
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
 * equivalent to CICS transaction responses.
 * 
 * This class provides structured error responses that React components can display
 * per Section 7.4.4 server-side validation coordination, enabling seamless
 * integration between Spring Boot backend validation and React frontend
 * error handling through Material-UI error states and helper text.
 * 
 * Implements audit trail generation for update operations per Section 6.2.3.4
 * update audit trail requirements, ensuring compliance with financial
 * transaction tracking and regulatory requirements.
 * 
 * Equivalent to CICS transaction response patterns from COACTUPC.cbl:
 * - Success/failure confirmation (WS-RESP-CD/WS-REAS-CD patterns)
 * - Error message handling (WS-RETURN-MSG structure)
 * - Audit trail generation (before/after update tracking)
 * - Session context maintenance (pseudo-conversational state)
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountUpdateResponseDto {

    /**
     * Indicates whether the account update operation was successful.
     * Equivalent to CICS WS-RESP-CD DFHRESP(NORMAL) check in COACTUPC.cbl.
     */
    @NotNull
    private Boolean success;

    /**
     * Primary error message for update failure cases.
     * Equivalent to WS-RETURN-MSG from COACTUPC.cbl error handling.
     */
    private String errorMessage;

    /**
     * Account ID that was updated (or attempted to be updated).
     * Provides context for the update operation equivalent to CDEMO-ACCT-ID.
     */
    private String accountId;

    /**
     * Comprehensive audit trail information for the update operation.
     * Supports compliance requirements per Section 6.2.3.4 update audit trail.
     */
    private AuditTrail auditTrail;

    /**
     * Field-level validation errors for detailed error reporting.
     * Enables React components to display specific field validation errors
     * through Material-UI error states and helper text.
     */
    private Map<String, ValidationResult> validationErrors;

    /**
     * Timestamp when the response was generated.
     * Provides audit trail timestamp equivalent to CICS processing time.
     */
    @NotNull
    private LocalDateTime timestamp;

    /**
     * Session context information for stateless REST API confirmation.
     * Maintains pseudo-conversational state equivalent to CICS COMMAREA.
     */
    private SessionContext sessionContext;

    /**
     * Transaction status information for comprehensive response handling.
     * Provides detailed transaction outcome information for audit and monitoring.
     */
    private TransactionStatus transactionStatus;

    /**
     * List of warning messages for non-critical issues during update.
     * Provides advisory information while allowing successful completion.
     */
    private List<String> warningMessages;

    /**
     * Default constructor initializing response with current timestamp.
     * Sets up basic response structure with default values.
     */
    public AccountUpdateResponseDto() {
        this.success = false;
        this.timestamp = LocalDateTime.now();
        this.validationErrors = new HashMap<>();
        this.warningMessages = new ArrayList<>();
    }

    /**
     * Constructor for successful account update response.
     * 
     * @param success true if update was successful
     * @param accountId the updated account ID
     * @param auditTrail audit information for the update
     */
    public AccountUpdateResponseDto(boolean success, String accountId, AuditTrail auditTrail) {
        this();
        this.success = success;
        this.accountId = accountId;
        this.auditTrail = auditTrail;
    }

    /**
     * Constructor for failed account update response with error message.
     * 
     * @param success false for failed update
     * @param errorMessage primary error message
     * @param validationErrors field-level validation errors
     */
    public AccountUpdateResponseDto(boolean success, String errorMessage, 
                                   Map<String, ValidationResult> validationErrors) {
        this();
        this.success = success;
        this.errorMessage = errorMessage;
        this.validationErrors = validationErrors != null ? validationErrors : new HashMap<>();
    }

    /**
     * Checks if the account update operation was successful.
     * Equivalent to checking WS-RESP-CD EQUAL TO DFHRESP(NORMAL) in COACTUPC.cbl.
     * 
     * @return true if update was successful, false otherwise
     */
    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }

    /**
     * Sets the success status of the account update operation.
     * 
     * @param success true if update was successful, false otherwise
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Gets the primary error message for failed update operations.
     * Equivalent to WS-RETURN-MSG from COACTUPC.cbl error handling.
     * 
     * @return error message or null if no error
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
    }

    /**
     * Gets the account ID that was updated or attempted to be updated.
     * 
     * @return account ID
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID that was updated or attempted to be updated.
     * 
     * @param accountId the account ID to set
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the comprehensive audit trail information for the update operation.
     * 
     * @return audit trail information
     */
    public AuditTrail getAuditTrail() {
        return auditTrail;
    }

    /**
     * Sets the comprehensive audit trail information for the update operation.
     * 
     * @param auditTrail the audit trail information to set
     */
    public void setAuditTrail(AuditTrail auditTrail) {
        this.auditTrail = auditTrail;
    }

    /**
     * Gets the field-level validation errors for detailed error reporting.
     * 
     * @return map of field names to validation results
     */
    public Map<String, ValidationResult> getValidationErrors() {
        return validationErrors;
    }

    /**
     * Sets the field-level validation errors for detailed error reporting.
     * 
     * @param validationErrors map of field names to validation results
     */
    public void setValidationErrors(Map<String, ValidationResult> validationErrors) {
        this.validationErrors = validationErrors != null ? validationErrors : new HashMap<>();
    }

    /**
     * Gets the timestamp when the response was generated.
     * 
     * @return response generation timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp when the response was generated.
     * 
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the session context information for stateless REST API confirmation.
     * 
     * @return session context
     */
    public SessionContext getSessionContext() {
        return sessionContext;
    }

    /**
     * Sets the session context information for stateless REST API confirmation.
     * 
     * @param sessionContext the session context to set
     */
    public void setSessionContext(SessionContext sessionContext) {
        this.sessionContext = sessionContext;
    }

    /**
     * Gets the transaction status information for comprehensive response handling.
     * 
     * @return transaction status
     */
    public TransactionStatus getTransactionStatus() {
        return transactionStatus;
    }

    /**
     * Sets the transaction status information for comprehensive response handling.
     * 
     * @param transactionStatus the transaction status to set
     */
    public void setTransactionStatus(TransactionStatus transactionStatus) {
        this.transactionStatus = transactionStatus;
    }

    /**
     * Gets the list of warning messages for non-critical issues during update.
     * 
     * @return list of warning messages
     */
    public List<String> getWarningMessages() {
        return warningMessages;
    }

    /**
     * Sets the list of warning messages for non-critical issues during update.
     * 
     * @param warningMessages the warning messages to set
     */
    public void setWarningMessages(List<String> warningMessages) {
        this.warningMessages = warningMessages != null ? warningMessages : new ArrayList<>();
    }

    /**
     * Adds a validation error for a specific field.
     * Convenience method for building validation error responses.
     * 
     * @param fieldName the field name with validation error
     * @param validationResult the validation result for the field
     */
    public void addValidationError(String fieldName, ValidationResult validationResult) {
        if (validationErrors == null) {
            validationErrors = new HashMap<>();
        }
        validationErrors.put(fieldName, validationResult);
    }

    /**
     * Adds a warning message to the response.
     * Convenience method for building warning message responses.
     * 
     * @param warningMessage the warning message to add
     */
    public void addWarningMessage(String warningMessage) {
        if (warningMessages == null) {
            warningMessages = new ArrayList<>();
        }
        warningMessages.add(warningMessage);
    }

    /**
     * Checks if the response contains any validation errors.
     * 
     * @return true if validation errors exist, false otherwise
     */
    public boolean hasValidationErrors() {
        return validationErrors != null && !validationErrors.isEmpty();
    }

    /**
     * Checks if the response contains any warning messages.
     * 
     * @return true if warning messages exist, false otherwise
     */
    public boolean hasWarningMessages() {
        return warningMessages != null && !warningMessages.isEmpty();
    }

    /**
     * Gets the error flag status based on success and validation errors.
     * Equivalent to COBOL error flag patterns (ERR-FLG-ON/ERR-FLG-OFF).
     * 
     * @return ErrorFlag.ON if errors exist, ErrorFlag.OFF otherwise
     */
    public ErrorFlag getErrorFlag() {
        return (!isSuccess() || hasValidationErrors()) ? ErrorFlag.ON : ErrorFlag.OFF;
    }

    /**
     * Creates a success response with audit trail information.
     * Convenience factory method for successful update responses.
     * 
     * @param accountId the updated account ID
     * @param auditTrail audit information for the update
     * @return successful account update response
     */
    public static AccountUpdateResponseDto createSuccessResponse(String accountId, AuditTrail auditTrail) {
        AccountUpdateResponseDto response = new AccountUpdateResponseDto();
        response.setSuccess(true);
        response.setAccountId(accountId);
        response.setAuditTrail(auditTrail);
        return response;
    }

    /**
     * Creates an error response with primary error message and validation errors.
     * Convenience factory method for failed update responses.
     * 
     * @param errorMessage primary error message
     * @param validationErrors field-level validation errors
     * @return failed account update response
     */
    public static AccountUpdateResponseDto createErrorResponse(String errorMessage, 
                                                              Map<String, ValidationResult> validationErrors) {
        AccountUpdateResponseDto response = new AccountUpdateResponseDto();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        response.setValidationErrors(validationErrors);
        return response;
    }

    /**
     * Creates an error response with primary error message only.
     * Convenience factory method for simple error responses.
     * 
     * @param errorMessage primary error message
     * @return failed account update response
     */
    public static AccountUpdateResponseDto createErrorResponse(String errorMessage) {
        return createErrorResponse(errorMessage, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountUpdateResponseDto that = (AccountUpdateResponseDto) o;
        return Objects.equals(success, that.success) &&
               Objects.equals(errorMessage, that.errorMessage) &&
               Objects.equals(accountId, that.accountId) &&
               Objects.equals(auditTrail, that.auditTrail) &&
               Objects.equals(validationErrors, that.validationErrors) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(sessionContext, that.sessionContext) &&
               Objects.equals(transactionStatus, that.transactionStatus) &&
               Objects.equals(warningMessages, that.warningMessages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, errorMessage, accountId, auditTrail, validationErrors, 
                           timestamp, sessionContext, transactionStatus, warningMessages);
    }

    @Override
    public String toString() {
        return "AccountUpdateResponseDto{" +
               "success=" + success +
               ", errorMessage='" + errorMessage + '\'' +
               ", accountId='" + accountId + '\'' +
               ", auditTrail=" + auditTrail +
               ", validationErrors=" + validationErrors +
               ", timestamp=" + timestamp +
               ", sessionContext=" + sessionContext +
               ", transactionStatus=" + transactionStatus +
               ", warningMessages=" + warningMessages +
               '}';
    }

    /**
     * Nested class representing comprehensive audit trail information.
     * Supports compliance requirements per Section 6.2.3.4 update audit trail.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AuditTrail {
        
        /**
         * Unique identifier for the audit trail entry.
         */
        private String auditId;

        /**
         * User ID who performed the update operation.
         */
        private String userId;

        /**
         * Timestamp when the update operation was performed.
         */
        private LocalDateTime updateTimestamp;

        /**
         * IP address from which the update was performed.
         */
        private String ipAddress;

        /**
         * Session ID for correlation with user session.
         */
        private String sessionId;

        /**
         * Before image of the account data (for audit purposes).
         */
        private String beforeImage;

        /**
         * After image of the account data (for audit purposes).
         */
        private String afterImage;

        /**
         * List of fields that were modified during the update.
         */
        private List<String> modifiedFields;

        /**
         * Reason or description for the update operation.
         */
        private String updateReason;

        /**
         * Default constructor.
         */
        public AuditTrail() {
            this.modifiedFields = new ArrayList<>();
        }

        /**
         * Constructor with essential audit information.
         * 
         * @param auditId unique audit identifier
         * @param userId user performing the update
         * @param updateTimestamp timestamp of the update
         */
        public AuditTrail(String auditId, String userId, LocalDateTime updateTimestamp) {
            this();
            this.auditId = auditId;
            this.userId = userId;
            this.updateTimestamp = updateTimestamp;
        }

        // Getters and setters for all fields

        public String getAuditId() { return auditId; }
        public void setAuditId(String auditId) { this.auditId = auditId; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public LocalDateTime getUpdateTimestamp() { return updateTimestamp; }
        public void setUpdateTimestamp(LocalDateTime updateTimestamp) { this.updateTimestamp = updateTimestamp; }

        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public String getBeforeImage() { return beforeImage; }
        public void setBeforeImage(String beforeImage) { this.beforeImage = beforeImage; }

        public String getAfterImage() { return afterImage; }
        public void setAfterImage(String afterImage) { this.afterImage = afterImage; }

        public List<String> getModifiedFields() { return modifiedFields; }
        public void setModifiedFields(List<String> modifiedFields) { 
            this.modifiedFields = modifiedFields != null ? modifiedFields : new ArrayList<>();
        }

        public String getUpdateReason() { return updateReason; }
        public void setUpdateReason(String updateReason) { this.updateReason = updateReason; }

        /**
         * Adds a modified field to the audit trail.
         * 
         * @param fieldName the name of the modified field
         */
        public void addModifiedField(String fieldName) {
            if (modifiedFields == null) {
                modifiedFields = new ArrayList<>();
            }
            modifiedFields.add(fieldName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AuditTrail that = (AuditTrail) o;
            return Objects.equals(auditId, that.auditId) &&
                   Objects.equals(userId, that.userId) &&
                   Objects.equals(updateTimestamp, that.updateTimestamp) &&
                   Objects.equals(ipAddress, that.ipAddress) &&
                   Objects.equals(sessionId, that.sessionId) &&
                   Objects.equals(beforeImage, that.beforeImage) &&
                   Objects.equals(afterImage, that.afterImage) &&
                   Objects.equals(modifiedFields, that.modifiedFields) &&
                   Objects.equals(updateReason, that.updateReason);
        }

        @Override
        public int hashCode() {
            return Objects.hash(auditId, userId, updateTimestamp, ipAddress, sessionId, 
                               beforeImage, afterImage, modifiedFields, updateReason);
        }

        @Override
        public String toString() {
            return "AuditTrail{" +
                   "auditId='" + auditId + '\'' +
                   ", userId='" + userId + '\'' +
                   ", updateTimestamp=" + updateTimestamp +
                   ", ipAddress='" + ipAddress + '\'' +
                   ", sessionId='" + sessionId + '\'' +
                   ", beforeImage='" + beforeImage + '\'' +
                   ", afterImage='" + afterImage + '\'' +
                   ", modifiedFields=" + modifiedFields +
                   ", updateReason='" + updateReason + '\'' +
                   '}';
        }
    }

    /**
     * Nested class representing session context information.
     * Maintains pseudo-conversational state equivalent to CICS COMMAREA.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SessionContext {
        
        /**
         * Session ID for correlation with user session.
         */
        private String sessionId;

        /**
         * User ID associated with the session.
         */
        private String userId;

        /**
         * Transaction ID for the current operation.
         */
        private String transactionId;

        /**
         * Program name equivalent to CICS program identifier.
         */
        private String programName;

        /**
         * Screen ID for navigation context.
         */
        private String screenId;

        /**
         * Session start timestamp.
         */
        private LocalDateTime sessionStart;

        /**
         * Last activity timestamp.
         */
        private LocalDateTime lastActivity;

        /**
         * Default constructor.
         */
        public SessionContext() {
            this.sessionStart = LocalDateTime.now();
            this.lastActivity = LocalDateTime.now();
        }

        /**
         * Constructor with session information.
         * 
         * @param sessionId session identifier
         * @param userId user identifier
         * @param transactionId transaction identifier
         */
        public SessionContext(String sessionId, String userId, String transactionId) {
            this();
            this.sessionId = sessionId;
            this.userId = userId;
            this.transactionId = transactionId;
        }

        // Getters and setters for all fields

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

        public String getProgramName() { return programName; }
        public void setProgramName(String programName) { this.programName = programName; }

        public String getScreenId() { return screenId; }
        public void setScreenId(String screenId) { this.screenId = screenId; }

        public LocalDateTime getSessionStart() { return sessionStart; }
        public void setSessionStart(LocalDateTime sessionStart) { this.sessionStart = sessionStart; }

        public LocalDateTime getLastActivity() { return lastActivity; }
        public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SessionContext that = (SessionContext) o;
            return Objects.equals(sessionId, that.sessionId) &&
                   Objects.equals(userId, that.userId) &&
                   Objects.equals(transactionId, that.transactionId) &&
                   Objects.equals(programName, that.programName) &&
                   Objects.equals(screenId, that.screenId) &&
                   Objects.equals(sessionStart, that.sessionStart) &&
                   Objects.equals(lastActivity, that.lastActivity);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sessionId, userId, transactionId, programName, screenId, 
                               sessionStart, lastActivity);
        }

        @Override
        public String toString() {
            return "SessionContext{" +
                   "sessionId='" + sessionId + '\'' +
                   ", userId='" + userId + '\'' +
                   ", transactionId='" + transactionId + '\'' +
                   ", programName='" + programName + '\'' +
                   ", screenId='" + screenId + '\'' +
                   ", sessionStart=" + sessionStart +
                   ", lastActivity=" + lastActivity +
                   '}';
        }
    }

    /**
     * Nested class representing transaction status information.
     * Provides detailed transaction outcome information for audit and monitoring.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TransactionStatus {
        
        /**
         * Transaction completion status.
         */
        private String status;

        /**
         * Response code equivalent to CICS WS-RESP-CD.
         */
        private String responseCode;

        /**
         * Reason code equivalent to CICS WS-REAS-CD.
         */
        private String reasonCode;

        /**
         * Processing time in milliseconds.
         */
        private Long processingTimeMs;

        /**
         * Database operation count.
         */
        private Integer dbOperationCount;

        /**
         * Records affected by the transaction.
         */
        private Integer recordsAffected;

        /**
         * Additional transaction details.
         */
        private Map<String, Object> details;

        /**
         * Default constructor.
         */
        public TransactionStatus() {
            this.details = new HashMap<>();
        }

        /**
         * Constructor with basic transaction information.
         * 
         * @param status transaction status
         * @param responseCode response code
         * @param reasonCode reason code
         */
        public TransactionStatus(String status, String responseCode, String reasonCode) {
            this();
            this.status = status;
            this.responseCode = responseCode;
            this.reasonCode = reasonCode;
        }

        // Getters and setters for all fields

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getResponseCode() { return responseCode; }
        public void setResponseCode(String responseCode) { this.responseCode = responseCode; }

        public String getReasonCode() { return reasonCode; }
        public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }

        public Long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

        public Integer getDbOperationCount() { return dbOperationCount; }
        public void setDbOperationCount(Integer dbOperationCount) { this.dbOperationCount = dbOperationCount; }

        public Integer getRecordsAffected() { return recordsAffected; }
        public void setRecordsAffected(Integer recordsAffected) { this.recordsAffected = recordsAffected; }

        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { 
            this.details = details != null ? details : new HashMap<>();
        }

        /**
         * Adds a detail entry to the transaction status.
         * 
         * @param key detail key
         * @param value detail value
         */
        public void addDetail(String key, Object value) {
            if (details == null) {
                details = new HashMap<>();
            }
            details.put(key, value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TransactionStatus that = (TransactionStatus) o;
            return Objects.equals(status, that.status) &&
                   Objects.equals(responseCode, that.responseCode) &&
                   Objects.equals(reasonCode, that.reasonCode) &&
                   Objects.equals(processingTimeMs, that.processingTimeMs) &&
                   Objects.equals(dbOperationCount, that.dbOperationCount) &&
                   Objects.equals(recordsAffected, that.recordsAffected) &&
                   Objects.equals(details, that.details);
        }

        @Override
        public int hashCode() {
            return Objects.hash(status, responseCode, reasonCode, processingTimeMs, 
                               dbOperationCount, recordsAffected, details);
        }

        @Override
        public String toString() {
            return "TransactionStatus{" +
                   "status='" + status + '\'' +
                   ", responseCode='" + responseCode + '\'' +
                   ", reasonCode='" + reasonCode + '\'' +
                   ", processingTimeMs=" + processingTimeMs +
                   ", dbOperationCount=" + dbOperationCount +
                   ", recordsAffected=" + recordsAffected +
                   ", details=" + details +
                   '}';
        }
    }
}