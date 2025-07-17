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

package com.carddemo.card;

import com.carddemo.common.dto.BaseResponseDto;
import com.carddemo.common.dto.AuditInfo;
import com.carddemo.common.dto.ValidationResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for card update operations providing comprehensive update confirmation.
 * 
 * This class extends BaseResponseDto to provide specific card update response information
 * including optimistic locking metadata, audit trail information, and validation results.
 * It supports the complete card update workflow from COCRDUPC.cbl with enhanced
 * cloud-native features for distributed transaction management.
 * 
 * Derived from COBOL program: app/cbl/COCRDUPC.cbl
 * Maps to BMS screen: app/bms/COCRDUP.bms
 * Transaction ID: CCUP (Card Update)
 * 
 * Key Features:
 * - Updated card information with complete field mappings
 * - Optimistic locking conflict resolution with version tracking
 * - Comprehensive audit trail with user context and timestamps
 * - Detailed validation results with business rule compliance
 * - Transaction confirmation details for update tracking
 * - Retry information for handling concurrent update conflicts
 * - Enhanced error handling with specific failure categorization
 * 
 * Optimistic Locking Implementation:
 * The response includes version numbers and conflict resolution metadata to handle
 * concurrent updates, replacing the COBOL READ FOR UPDATE mechanism with modern
 * distributed locking patterns using version-based optimistic concurrency control.
 * 
 * Audit Trail Compliance:
 * Comprehensive audit information supports SOX 404, PCI DSS, and GDPR requirements
 * with complete user context, operation tracking, and compliance metadata for
 * card data modification operations.
 * 
 * Validation Result Integration:
 * Detailed validation feedback supports React frontend components with field-level
 * error messages, business rule compliance status, and progressive validation
 * patterns for enhanced user experience.
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since Spring Boot 3.2.x
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardUpdateResponseDto extends BaseResponseDto {

    /**
     * Updated card information after successful operation.
     * Contains complete card details with all fields populated after update,
     * including account ID, card number, embossed name, expiration date, and status.
     * Maps to COBOL CARD-UPDATE-RECORD structure from COCRDUPC.cbl.
     */
    @NotNull(message = "Updated card information is required")
    private Card updatedCard;

    /**
     * Validation result containing comprehensive field-level validation feedback.
     * Provides detailed validation messages, business rule compliance status,
     * and error categorization for React frontend integration.
     * Maps to COBOL validation flags and messages from COCRDUPC.cbl validation logic.
     */
    @NotNull(message = "Validation result is required")
    private ValidationResult validationResult;

    /**
     * Comprehensive audit trail information for compliance and tracking.
     * Includes user context, operation timestamps, correlation IDs, and source system
     * information for complete audit trail maintenance and regulatory compliance.
     * Enhances COBOL transaction logging with cloud-native audit patterns.
     */
    @NotNull(message = "Audit information is required")
    private AuditInfo auditInfo;

    /**
     * Current version number for optimistic locking conflict resolution.
     * Tracks the record version after successful update for subsequent operations.
     * Replaces COBOL READ FOR UPDATE mechanism with distributed concurrency control.
     */
    private Long versionNumber;

    /**
     * Optimistic locking success status indicating if version-based update succeeded.
     * true = update succeeded without conflicts, false = version conflict detected.
     * Provides immediate feedback on concurrent update collision handling.
     */
    private boolean optimisticLockSuccess;

    /**
     * Conflict resolution information for handling concurrent update scenarios.
     * Provides detailed information about version conflicts, affected fields,
     * and recommended resolution strategies for distributed transaction management.
     * Enhances COBOL "DATA-WAS-CHANGED-BEFORE-UPDATE" handling with structured metadata.
     */
    private ConflictResolutionInfo conflictResolutionInfo;

    /**
     * Transaction confirmation details for update operation tracking.
     * Provides comprehensive confirmation information including transaction ID,
     * processing timestamp, affected records, and operation completion status.
     * Maps to COBOL transaction completion messages with enhanced traceability.
     */
    private TransactionConfirmation transactionConfirmation;

    /**
     * Timestamp when the card update operation was completed.
     * Provides precise timing information for audit trail and performance monitoring.
     * Uses LocalDateTime for timezone-independent timestamp representation.
     */
    private LocalDateTime updateTimestamp;

    /**
     * Retry information for handling optimistic locking conflicts.
     * Provides guidance on retry strategies, backoff timing, and maximum retry attempts
     * for clients experiencing concurrent update conflicts.
     * Supports resilient distributed transaction patterns.
     */
    private RetryInfo retryInfo;

    /**
     * Inner class for conflict resolution information.
     * Provides structured metadata for handling optimistic locking conflicts
     * and concurrent update scenarios in distributed card management operations.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ConflictResolutionInfo {
        
        /**
         * Indicates if a version conflict was detected during update.
         * true = version conflict detected, false = no conflict encountered.
         */
        private boolean conflictDetected;
        
        /**
         * Original version number that was expected during update.
         * Used for conflict analysis and resolution strategy determination.
         */
        private Long expectedVersion;
        
        /**
         * Actual version number found in the database during update.
         * Indicates the current version that caused the conflict.
         */
        private Long actualVersion;
        
        /**
         * Timestamp when the conflicting update occurred.
         * Provides timing information for conflict resolution analysis.
         */
        private LocalDateTime conflictTimestamp;
        
        /**
         * User ID who made the conflicting update.
         * Provides context for conflict resolution and audit trail.
         */
        private String conflictingUserId;
        
        /**
         * Detailed description of the conflict resolution strategy.
         * Provides guidance on how to handle the version conflict.
         */
        private String resolutionStrategy;
        
        /**
         * List of fields that were modified in the conflicting update.
         * Enables fine-grained conflict analysis and selective resolution.
         */
        private java.util.List<String> conflictingFields;

        // Constructors
        public ConflictResolutionInfo() {}

        public ConflictResolutionInfo(boolean conflictDetected, Long expectedVersion, Long actualVersion) {
            this.conflictDetected = conflictDetected;
            this.expectedVersion = expectedVersion;
            this.actualVersion = actualVersion;
            this.conflictTimestamp = LocalDateTime.now();
        }

        // Getters and setters
        public boolean isConflictDetected() { return conflictDetected; }
        public void setConflictDetected(boolean conflictDetected) { this.conflictDetected = conflictDetected; }

        public Long getExpectedVersion() { return expectedVersion; }
        public void setExpectedVersion(Long expectedVersion) { this.expectedVersion = expectedVersion; }

        public Long getActualVersion() { return actualVersion; }
        public void setActualVersion(Long actualVersion) { this.actualVersion = actualVersion; }

        public LocalDateTime getConflictTimestamp() { return conflictTimestamp; }
        public void setConflictTimestamp(LocalDateTime conflictTimestamp) { this.conflictTimestamp = conflictTimestamp; }

        public String getConflictingUserId() { return conflictingUserId; }
        public void setConflictingUserId(String conflictingUserId) { this.conflictingUserId = conflictingUserId; }

        public String getResolutionStrategy() { return resolutionStrategy; }
        public void setResolutionStrategy(String resolutionStrategy) { this.resolutionStrategy = resolutionStrategy; }

        public java.util.List<String> getConflictingFields() { return conflictingFields; }
        public void setConflictingFields(java.util.List<String> conflictingFields) { this.conflictingFields = conflictingFields; }
    }

    /**
     * Inner class for transaction confirmation information.
     * Provides comprehensive confirmation details for card update operations
     * including transaction metadata, processing status, and audit information.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TransactionConfirmation {
        
        /**
         * Unique transaction identifier for the update operation.
         * Provides traceability and correlation for audit and monitoring.
         */
        private String transactionId;
        
        /**
         * Processing timestamp when the transaction was completed.
         * Provides precise timing for audit trail and performance monitoring.
         */
        private LocalDateTime processingTimestamp;
        
        /**
         * Transaction status indicating the final operation result.
         * Values: SUCCESS, FAILURE, PARTIAL, ROLLBACK
         */
        private String transactionStatus;
        
        /**
         * Number of records affected by the update operation.
         * Provides confirmation of the operation scope and impact.
         */
        private int affectedRecords;
        
        /**
         * Detailed processing message for the transaction.
         * Provides specific information about the transaction outcome.
         */
        private String processingMessage;
        
        /**
         * Transaction processing duration in milliseconds.
         * Supports performance monitoring and SLA compliance tracking.
         */
        private Long processingDurationMs;

        // Constructors
        public TransactionConfirmation() {}

        public TransactionConfirmation(String transactionId, String transactionStatus) {
            this.transactionId = transactionId;
            this.transactionStatus = transactionStatus;
            this.processingTimestamp = LocalDateTime.now();
            this.affectedRecords = 1; // Default for single card update
        }

        // Getters and setters
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

        public LocalDateTime getProcessingTimestamp() { return processingTimestamp; }
        public void setProcessingTimestamp(LocalDateTime processingTimestamp) { this.processingTimestamp = processingTimestamp; }

        public String getTransactionStatus() { return transactionStatus; }
        public void setTransactionStatus(String transactionStatus) { this.transactionStatus = transactionStatus; }

        public int getAffectedRecords() { return affectedRecords; }
        public void setAffectedRecords(int affectedRecords) { this.affectedRecords = affectedRecords; }

        public String getProcessingMessage() { return processingMessage; }
        public void setProcessingMessage(String processingMessage) { this.processingMessage = processingMessage; }

        public Long getProcessingDurationMs() { return processingDurationMs; }
        public void setProcessingDurationMs(Long processingDurationMs) { this.processingDurationMs = processingDurationMs; }
    }

    /**
     * Inner class for retry information.
     * Provides structured guidance for handling optimistic locking conflicts
     * and implementing resilient retry strategies in distributed environments.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RetryInfo {
        
        /**
         * Indicates if the operation should be retried.
         * true = retry recommended, false = retry not recommended or not applicable.
         */
        private boolean shouldRetry;
        
        /**
         * Recommended retry delay in milliseconds.
         * Provides exponential backoff timing for conflict resolution.
         */
        private Long retryDelayMs;
        
        /**
         * Maximum number of retry attempts recommended.
         * Prevents infinite retry loops and provides circuit breaker behavior.
         */
        private int maxRetryAttempts;
        
        /**
         * Current retry attempt number.
         * Tracks the retry sequence for escalation and monitoring.
         */
        private int currentAttempt;
        
        /**
         * Retry strategy description for client guidance.
         * Provides specific instructions for handling the retry scenario.
         */
        private String retryStrategy;
        
        /**
         * Exponential backoff multiplier for retry delay calculation.
         * Supports adaptive retry timing based on conflict frequency.
         */
        private BigDecimal backoffMultiplier;

        // Constructors
        public RetryInfo() {}

        public RetryInfo(boolean shouldRetry, Long retryDelayMs, int maxRetryAttempts) {
            this.shouldRetry = shouldRetry;
            this.retryDelayMs = retryDelayMs;
            this.maxRetryAttempts = maxRetryAttempts;
            this.currentAttempt = 0;
            this.backoffMultiplier = BigDecimal.valueOf(1.5); // Default exponential backoff
        }

        // Getters and setters
        public boolean isShouldRetry() { return shouldRetry; }
        public void setShouldRetry(boolean shouldRetry) { this.shouldRetry = shouldRetry; }

        public Long getRetryDelayMs() { return retryDelayMs; }
        public void setRetryDelayMs(Long retryDelayMs) { this.retryDelayMs = retryDelayMs; }

        public int getMaxRetryAttempts() { return maxRetryAttempts; }
        public void setMaxRetryAttempts(int maxRetryAttempts) { this.maxRetryAttempts = maxRetryAttempts; }

        public int getCurrentAttempt() { return currentAttempt; }
        public void setCurrentAttempt(int currentAttempt) { this.currentAttempt = currentAttempt; }

        public String getRetryStrategy() { return retryStrategy; }
        public void setRetryStrategy(String retryStrategy) { this.retryStrategy = retryStrategy; }

        public BigDecimal getBackoffMultiplier() { return backoffMultiplier; }
        public void setBackoffMultiplier(BigDecimal backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }
    }

    /**
     * Default constructor for JSON deserialization and Spring framework integration.
     * Initializes the response with current timestamp and default successful status.
     */
    public CardUpdateResponseDto() {
        super();
        this.updateTimestamp = LocalDateTime.now();
        this.optimisticLockSuccess = true;
        this.validationResult = new ValidationResult();
        this.auditInfo = new AuditInfo();
    }

    /**
     * Constructor for successful card update response.
     * 
     * @param updatedCard Updated card information after successful operation
     * @param validationResult Validation result with comprehensive feedback
     * @param auditInfo Audit trail information for compliance tracking
     */
    public CardUpdateResponseDto(Card updatedCard, ValidationResult validationResult, AuditInfo auditInfo) {
        this();
        this.updatedCard = updatedCard;
        this.validationResult = validationResult;
        this.auditInfo = auditInfo;
        this.setSuccess(true);
        this.setMessage("Card update completed successfully");
    }

    /**
     * Constructor for failed card update response with error information.
     * 
     * @param validationResult Validation result with error details
     * @param auditInfo Audit trail information for compliance tracking
     * @param errorMessage Error message describing the failure
     */
    public CardUpdateResponseDto(ValidationResult validationResult, AuditInfo auditInfo, String errorMessage) {
        this();
        this.validationResult = validationResult;
        this.auditInfo = auditInfo;
        this.setSuccess(false);
        this.setErrorMessage(errorMessage);
        this.optimisticLockSuccess = false;
    }

    // Getters and setters for all fields

    /**
     * Gets the updated card information after successful operation.
     * 
     * @return Updated card details with complete field mappings
     */
    public Card getUpdatedCard() {
        return updatedCard;
    }

    /**
     * Sets the updated card information after successful operation.
     * 
     * @param updatedCard Updated card details to set
     */
    public void setUpdatedCard(Card updatedCard) {
        this.updatedCard = updatedCard;
    }

    /**
     * Gets the validation result containing comprehensive field-level validation feedback.
     * 
     * @return Validation result with detailed error messages and business rule compliance
     */
    public ValidationResult getValidationResult() {
        return validationResult;
    }

    /**
     * Sets the validation result containing comprehensive field-level validation feedback.
     * 
     * @param validationResult Validation result to set
     */
    public void setValidationResult(ValidationResult validationResult) {
        this.validationResult = validationResult;
    }

    /**
     * Gets the comprehensive audit trail information for compliance and tracking.
     * 
     * @return Audit information with user context and operation details
     */
    public AuditInfo getAuditInfo() {
        return auditInfo;
    }

    /**
     * Sets the comprehensive audit trail information for compliance and tracking.
     * 
     * @param auditInfo Audit information to set
     */
    public void setAuditInfo(AuditInfo auditInfo) {
        this.auditInfo = auditInfo;
    }

    /**
     * Gets the current version number for optimistic locking conflict resolution.
     * 
     * @return Current version number after successful update
     */
    public Long getVersionNumber() {
        return versionNumber;
    }

    /**
     * Sets the current version number for optimistic locking conflict resolution.
     * 
     * @param versionNumber Version number to set
     */
    public void setVersionNumber(Long versionNumber) {
        this.versionNumber = versionNumber;
    }

    /**
     * Gets the optimistic locking success status.
     * 
     * @return true if update succeeded without conflicts, false if version conflict detected
     */
    public boolean isOptimisticLockSuccess() {
        return optimisticLockSuccess;
    }

    /**
     * Sets the optimistic locking success status.
     * 
     * @param optimisticLockSuccess Optimistic locking success status to set
     */
    public void setOptimisticLockSuccess(boolean optimisticLockSuccess) {
        this.optimisticLockSuccess = optimisticLockSuccess;
    }

    /**
     * Gets the conflict resolution information for handling concurrent update scenarios.
     * 
     * @return Conflict resolution metadata for distributed transaction management
     */
    public ConflictResolutionInfo getConflictResolutionInfo() {
        return conflictResolutionInfo;
    }

    /**
     * Sets the conflict resolution information for handling concurrent update scenarios.
     * 
     * @param conflictResolutionInfo Conflict resolution information to set
     */
    public void setConflictResolutionInfo(ConflictResolutionInfo conflictResolutionInfo) {
        this.conflictResolutionInfo = conflictResolutionInfo;
    }

    /**
     * Gets the transaction confirmation details for update operation tracking.
     * 
     * @return Transaction confirmation with processing metadata and completion status
     */
    public TransactionConfirmation getTransactionConfirmation() {
        return transactionConfirmation;
    }

    /**
     * Sets the transaction confirmation details for update operation tracking.
     * 
     * @param transactionConfirmation Transaction confirmation to set
     */
    public void setTransactionConfirmation(TransactionConfirmation transactionConfirmation) {
        this.transactionConfirmation = transactionConfirmation;
    }

    /**
     * Gets the timestamp when the card update operation was completed.
     * 
     * @return Update operation completion timestamp
     */
    public LocalDateTime getUpdateTimestamp() {
        return updateTimestamp;
    }

    /**
     * Sets the timestamp when the card update operation was completed.
     * 
     * @param updateTimestamp Update completion timestamp to set
     */
    public void setUpdateTimestamp(LocalDateTime updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }

    /**
     * Gets the retry information for handling optimistic locking conflicts.
     * 
     * @return Retry guidance for conflict resolution and resilient transaction patterns
     */
    public RetryInfo getRetryInfo() {
        return retryInfo;
    }

    /**
     * Sets the retry information for handling optimistic locking conflicts.
     * 
     * @param retryInfo Retry information to set
     */
    public void setRetryInfo(RetryInfo retryInfo) {
        this.retryInfo = retryInfo;
    }

    // Helper methods for response construction and validation

    /**
     * Creates a successful card update response with complete information.
     * 
     * @param updatedCard Updated card information
     * @param validationResult Validation result
     * @param auditInfo Audit trail information
     * @param versionNumber Current version number
     * @param transactionId Transaction identifier
     * @return Configured successful response
     */
    public static CardUpdateResponseDto createSuccessResponse(Card updatedCard, ValidationResult validationResult, 
                                                             AuditInfo auditInfo, Long versionNumber, String transactionId) {
        CardUpdateResponseDto response = new CardUpdateResponseDto(updatedCard, validationResult, auditInfo);
        response.setVersionNumber(versionNumber);
        response.setOptimisticLockSuccess(true);
        
        TransactionConfirmation confirmation = new TransactionConfirmation(transactionId, "SUCCESS");
        confirmation.setProcessingMessage("Card update completed successfully");
        response.setTransactionConfirmation(confirmation);
        
        return response;
    }

    /**
     * Creates a failed card update response with error information.
     * 
     * @param validationResult Validation result with error details
     * @param auditInfo Audit trail information
     * @param errorMessage Error message
     * @param transactionId Transaction identifier
     * @return Configured error response
     */
    public static CardUpdateResponseDto createErrorResponse(ValidationResult validationResult, AuditInfo auditInfo, 
                                                          String errorMessage, String transactionId) {
        CardUpdateResponseDto response = new CardUpdateResponseDto(validationResult, auditInfo, errorMessage);
        
        TransactionConfirmation confirmation = new TransactionConfirmation(transactionId, "FAILURE");
        confirmation.setProcessingMessage(errorMessage);
        response.setTransactionConfirmation(confirmation);
        
        return response;
    }

    /**
     * Creates a card update response with optimistic locking conflict information.
     * 
     * @param expectedVersion Expected version number
     * @param actualVersion Actual version number found
     * @param conflictingUserId User who made the conflicting update
     * @param validationResult Validation result
     * @param auditInfo Audit trail information
     * @return Configured conflict response
     */
    public static CardUpdateResponseDto createConflictResponse(Long expectedVersion, Long actualVersion, 
                                                              String conflictingUserId, ValidationResult validationResult, 
                                                              AuditInfo auditInfo) {
        CardUpdateResponseDto response = new CardUpdateResponseDto(validationResult, auditInfo, 
                                                                  "Optimistic locking conflict detected");
        response.setOptimisticLockSuccess(false);
        
        ConflictResolutionInfo conflictInfo = new ConflictResolutionInfo(true, expectedVersion, actualVersion);
        conflictInfo.setConflictingUserId(conflictingUserId);
        conflictInfo.setResolutionStrategy("RETRY_WITH_LATEST_VERSION");
        response.setConflictResolutionInfo(conflictInfo);
        
        RetryInfo retryInfo = new RetryInfo(true, 1000L, 3);
        retryInfo.setRetryStrategy("EXPONENTIAL_BACKOFF");
        response.setRetryInfo(retryInfo);
        
        return response;
    }

    /**
     * Checks if the card update operation was successful.
     * 
     * @return true if update was successful and validation passed
     */
    public boolean isUpdateSuccessful() {
        return isSuccess() && optimisticLockSuccess && 
               validationResult != null && validationResult.isValid();
    }

    /**
     * Checks if the response indicates a validation failure.
     * 
     * @return true if validation failed
     */
    public boolean hasValidationErrors() {
        return validationResult != null && !validationResult.isValid();
    }

    /**
     * Checks if the response indicates an optimistic locking conflict.
     * 
     * @return true if optimistic locking conflict was detected
     */
    public boolean hasOptimisticLockingConflict() {
        return !optimisticLockSuccess && conflictResolutionInfo != null && 
               conflictResolutionInfo.isConflictDetected();
    }

    /**
     * Gets a summary of the card update operation result.
     * 
     * @return Formatted summary string for logging and monitoring
     */
    public String getOperationSummary() {
        if (isUpdateSuccessful()) {
            return String.format("Card update successful - Card: %s, Version: %d", 
                                updatedCard != null ? updatedCard.getCardNumber() : "N/A", 
                                versionNumber != null ? versionNumber : 0);
        } else if (hasOptimisticLockingConflict()) {
            return String.format("Card update failed - Optimistic locking conflict detected");
        } else if (hasValidationErrors()) {
            return String.format("Card update failed - Validation errors: %d", 
                                validationResult.getErrorCount());
        } else {
            return "Card update failed - Unknown error";
        }
    }

    @Override
    public String toString() {
        return "CardUpdateResponseDto{" +
                "success=" + isSuccess() +
                ", optimisticLockSuccess=" + optimisticLockSuccess +
                ", versionNumber=" + versionNumber +
                ", updateTimestamp=" + updateTimestamp +
                ", hasValidationErrors=" + hasValidationErrors() +
                ", hasConflictInfo=" + (conflictResolutionInfo != null) +
                ", transactionId=" + (transactionConfirmation != null ? transactionConfirmation.getTransactionId() : "N/A") +
                '}';
    }
}