/*
 * Copyright (c) 2024 CardDemo Application
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

package com.carddemo.card;

import com.carddemo.common.dto.BaseResponseDto;
import com.carddemo.common.dto.AuditInfo;
import com.carddemo.common.dto.ValidationResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;

/**
 * Response DTO for card update operations with optimistic locking metadata, audit trail information,
 * and validation results providing comprehensive update confirmation for COCRDUPC.cbl functionality.
 * 
 * This DTO represents the complete response structure for card update operations, maintaining
 * compatibility with the original COBOL COCRDUPC program's response patterns while providing
 * modern REST API response capabilities for React frontend integration.
 * 
 * Key Features from COBOL COCRDUPC Conversion:
 * - Updated card information equivalent to CCUP-NEW-DETAILS structure
 * - Optimistic locking conflict resolution matching VSAM record locking behavior
 * - Comprehensive audit trail information for SOX compliance and security monitoring
 * - Detailed validation results preserving BMS field-level validation patterns
 * - Transaction confirmation details equivalent to CICS SYNCPOINT operations
 * - Retry information for handling concurrent update conflicts
 * 
 * COBOL Program Mapping:
 * - Source: COCRDUPC.cbl (Credit Card Update Program)
 * - Transaction ID: CCUP (Credit Card Update)
 * - Screen: COCRDUP.bms (Credit Card Update Screen)
 * - Copybooks: CVCRD01Y.cpy (Card Work Area Structure)
 * 
 * Response Status Mapping from COBOL:
 * - CCUP-CHANGES-OKAYED-AND-DONE -> success=true with confirmation details
 * - CCUP-CHANGES-OKAYED-LOCK-ERROR -> optimisticLockSuccess=false with retry info
 * - CCUP-CHANGES-OKAYED-BUT-FAILED -> success=false with error details
 * - CCUP-CHANGES-NOT-OK -> success=false with validation errors
 * 
 * Performance Requirements:
 * - Sub-200ms response generation for card update confirmations
 * - Support for 10,000+ TPS with optimal JSON serialization
 * - Memory efficient with @JsonInclude for null value exclusion
 * - Correlation ID support for distributed tracing across microservices
 * 
 * Security and Compliance:
 * - Comprehensive audit trail for PCI DSS compliance
 * - Version-based optimistic locking for concurrent access control
 * - Masked card number display for PCI data protection
 * - Detailed validation results for security constraint enforcement
 * 
 * Usage Pattern:
 * This DTO is returned by the CardUpdateService when processing card update requests
 * from the React frontend, providing complete feedback on update success, validation
 * errors, locking conflicts, and audit information for comprehensive user experience.
 * 
 * @author Blitzy Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardUpdateResponseDto extends BaseResponseDto {

    /**
     * Updated card information after successful update operation.
     * 
     * Contains the complete card entity with updated field values, equivalent to
     * the CCUP-NEW-DETAILS structure from the COBOL program. Provides the client
     * with the current state of the card after update processing, including any
     * server-side modifications or normalizations applied during the update.
     * 
     * This field is populated when the update operation succeeds and contains
     * the card information as persisted in the PostgreSQL database, ensuring
     * the client has accurate post-update state information.
     */
    private Card updatedCard;

    /**
     * Comprehensive validation results for the card update operation.
     * 
     * Contains field-level validation messages, business rule compliance status,
     * and detailed error information equivalent to the input validation logic
     * from the COBOL program's 1200-EDIT-MAP-INPUTS section. Provides granular
     * feedback on validation failures to support precise error handling in the
     * React frontend components.
     * 
     * Maps to COBOL validation patterns:
     * - Account ID validation (1210-EDIT-ACCOUNT)
     * - Card number validation (1220-EDIT-CARD)
     * - Card name validation (1230-EDIT-NAME)
     * - Card status validation (1240-EDIT-CARDSTATUS)
     * - Expiration date validation (1250-EDIT-EXPIRY-MON, 1260-EDIT-EXPIRY-YEAR)
     */
    @NotNull
    private ValidationResult validationResult;

    /**
     * Comprehensive audit trail information for the card update operation.
     * 
     * Contains user context, timestamps, operation type, and correlation information
     * equivalent to the audit trail capabilities required for SOX compliance and
     * security monitoring. Provides complete traceability for card update operations
     * supporting regulatory requirements and security incident investigation.
     * 
     * Essential for:
     * - PCI DSS audit trail requirements for cardholder data modifications
     * - SOX compliance for financial data change tracking
     * - Security monitoring and incident response capabilities
     * - Distributed transaction correlation across microservices
     */
    @NotNull
    private AuditInfo auditInfo;

    /**
     * Version number for optimistic locking conflict detection and resolution.
     * 
     * Contains the current version number of the card entity after update,
     * equivalent to the optimistic locking mechanism that replaces VSAM record
     * locking from the original COBOL system. Used by clients to detect concurrent
     * modifications and implement retry logic for update conflicts.
     * 
     * Maps to COBOL locking scenarios:
     * - COULD-NOT-LOCK-FOR-UPDATE: Version conflict detected
     * - DATA-WAS-CHANGED-BEFORE-UPDATE: Concurrent modification detected
     * - Normal update: Version incremented successfully
     */
    private Long versionNumber;

    /**
     * Optimistic locking success status indicator.
     * 
     * Indicates whether the update operation succeeded without version conflicts,
     * equivalent to the COBOL program's ability to successfully lock and update
     * the VSAM record. False indicates a concurrent modification was detected
     * and the update was rejected to maintain data consistency.
     * 
     * Used by React frontend to determine whether to display success confirmation
     * or retry/refresh prompts for the user when concurrent updates occur.
     */
    private boolean optimisticLockSuccess;

    /**
     * Conflict resolution information for optimistic locking failures.
     * 
     * Provides detailed information about version conflicts and suggested resolution
     * strategies when optimistic locking fails, equivalent to the COBOL program's
     * conflict detection in the 9300-CHECK-CHANGE-IN-REC section. Helps users
     * understand what changed and how to proceed with their update attempt.
     * 
     * Contains information such as:
     * - Fields that were modified by concurrent updates
     * - Suggested resolution strategies (refresh and retry, merge changes, etc.)
     * - Current field values for comparison with user's intended changes
     */
    private String conflictResolutionInfo;

    /**
     * Transaction confirmation details for successful update operations.
     * 
     * Contains confirmation information equivalent to the CONFIRM-UPDATE-SUCCESS
     * message from the COBOL program, providing users with detailed feedback about
     * the successful completion of their card update operation. Includes transaction
     * identifiers and completion timestamps for audit and user confirmation purposes.
     * 
     * Populated when update operations complete successfully and includes:
     * - Transaction completion confirmation message
     * - Updated field summary for user verification
     * - Next steps guidance for the user interface
     */
    private String transactionConfirmation;

    /**
     * Update timestamp indicating when the card modification was completed.
     * 
     * Records the precise moment when the card update was successfully committed
     * to the database, equivalent to the timestamp capture that would occur during
     * CICS SYNCPOINT processing in the original COBOL system. Essential for audit
     * trail completeness and providing users with accurate update timing information.
     * 
     * Uses LocalDateTime with high precision to support:
     * - Exact audit trail temporal requirements
     * - User interface display of update completion time
     * - Correlation with database transaction logs
     * - Distributed system timing analysis
     */
    private LocalDateTime updateTimestamp;

    /**
     * Retry information for handling optimistic locking conflicts and failures.
     * 
     * Provides guidance to clients on how to handle update failures, equivalent
     * to the user guidance messages provided by the COBOL program when update
     * operations fail due to locking conflicts or validation errors. Supports
     * intelligent retry logic in the React frontend for improved user experience.
     * 
     * Contains information such as:
     * - Recommended retry delay intervals
     * - Maximum retry attempt suggestions
     * - Alternative resolution strategies
     * - User action recommendations for persistent conflicts
     */
    private String retryInfo;

    /**
     * Default constructor initializing the response for successful card update operations.
     * 
     * Creates a CardUpdateResponseDto with default success status and initializes
     * required nested objects to ensure consistent response structure. Automatically
     * sets the update timestamp to the current system time for accurate audit trails.
     */
    public CardUpdateResponseDto() {
        super();
        this.validationResult = new ValidationResult();
        this.auditInfo = new AuditInfo();
        this.optimisticLockSuccess = true;
        this.updateTimestamp = LocalDateTime.now();
    }

    /**
     * Constructor for successful card update responses with correlation tracking.
     * 
     * Creates a successful response with correlation ID for distributed tracing
     * and initializes all required components for comprehensive update confirmation.
     * Used when card update operations complete successfully without conflicts.
     * 
     * @param correlationId Unique identifier for request correlation and distributed tracing
     */
    public CardUpdateResponseDto(String correlationId) {
        super(correlationId);
        this.validationResult = new ValidationResult();
        this.auditInfo = new AuditInfo();
        this.optimisticLockSuccess = true;
        this.updateTimestamp = LocalDateTime.now();
    }

    /**
     * Constructor for card update error responses with detailed error context.
     * 
     * Creates an error response with failure status, error message, and correlation ID
     * for comprehensive error reporting. Used when validation failures, business rule
     * violations, or system errors prevent successful card update completion.
     * 
     * @param errorMessage Descriptive error message for client consumption
     * @param correlationId Unique identifier for request correlation and error tracking
     */
    public CardUpdateResponseDto(String errorMessage, String correlationId) {
        super(errorMessage, correlationId);
        this.validationResult = new ValidationResult(false);
        this.auditInfo = new AuditInfo();
        this.optimisticLockSuccess = false;
        this.updateTimestamp = LocalDateTime.now();
    }

    // Getter and Setter Methods

    /**
     * Gets the updated card information after successful update operation.
     * 
     * @return Card entity with updated field values, null if update failed
     */
    public Card getUpdatedCard() {
        return updatedCard;
    }

    /**
     * Sets the updated card information after successful update operation.
     * 
     * @param updatedCard Card entity containing the updated field values
     */
    public void setUpdatedCard(Card updatedCard) {
        this.updatedCard = updatedCard;
    }

    /**
     * Gets the comprehensive validation results for the card update operation.
     * 
     * @return ValidationResult containing field-level validation messages and compliance status
     */
    public ValidationResult getValidationResult() {
        return validationResult;
    }

    /**
     * Sets the comprehensive validation results for the card update operation.
     * 
     * @param validationResult ValidationResult containing validation feedback and error details
     */
    public void setValidationResult(ValidationResult validationResult) {
        this.validationResult = validationResult;
    }

    /**
     * Gets the comprehensive audit trail information for the card update operation.
     * 
     * @return AuditInfo containing user context, timestamps, and correlation information
     */
    public AuditInfo getAuditInfo() {
        return auditInfo;
    }

    /**
     * Sets the comprehensive audit trail information for the card update operation.
     * 
     * @param auditInfo AuditInfo containing audit trail details and compliance information
     */
    public void setAuditInfo(AuditInfo auditInfo) {
        this.auditInfo = auditInfo;
    }

    /**
     * Gets the version number for optimistic locking conflict detection.
     * 
     * @return Current version number of the card entity after update
     */
    public Long getVersionNumber() {
        return versionNumber;
    }

    /**
     * Sets the version number for optimistic locking conflict detection.
     * 
     * @param versionNumber Version number for concurrent modification tracking
     */
    public void setVersionNumber(Long versionNumber) {
        this.versionNumber = versionNumber;
    }

    /**
     * Gets the optimistic locking success status indicator.
     * 
     * @return true if update succeeded without version conflicts, false otherwise
     */
    public boolean isOptimisticLockSuccess() {
        return optimisticLockSuccess;
    }

    /**
     * Sets the optimistic locking success status indicator.
     * 
     * @param optimisticLockSuccess Success status for optimistic locking operation
     */
    public void setOptimisticLockSuccess(boolean optimisticLockSuccess) {
        this.optimisticLockSuccess = optimisticLockSuccess;
    }

    /**
     * Gets the conflict resolution information for optimistic locking failures.
     * 
     * @return Detailed information about version conflicts and resolution strategies
     */
    public String getConflictResolutionInfo() {
        return conflictResolutionInfo;
    }

    /**
     * Sets the conflict resolution information for optimistic locking failures.
     * 
     * @param conflictResolutionInfo Information about conflicts and suggested resolutions
     */
    public void setConflictResolutionInfo(String conflictResolutionInfo) {
        this.conflictResolutionInfo = conflictResolutionInfo;
    }

    /**
     * Gets the transaction confirmation details for successful update operations.
     * 
     * @return Confirmation message and transaction details for successful updates
     */
    public String getTransactionConfirmation() {
        return transactionConfirmation;
    }

    /**
     * Sets the transaction confirmation details for successful update operations.
     * 
     * @param transactionConfirmation Confirmation message and completion details
     */
    public void setTransactionConfirmation(String transactionConfirmation) {
        this.transactionConfirmation = transactionConfirmation;
    }

    /**
     * Gets the update timestamp indicating when the card modification was completed.
     * 
     * @return LocalDateTime representing the precise update completion time
     */
    public LocalDateTime getUpdateTimestamp() {
        return updateTimestamp;
    }

    /**
     * Sets the update timestamp indicating when the card modification was completed.
     * 
     * @param updateTimestamp Precise timestamp of update completion for audit trails
     */
    public void setUpdateTimestamp(LocalDateTime updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }

    /**
     * Gets the retry information for handling optimistic locking conflicts and failures.
     * 
     * @return Guidance information for client retry logic and conflict resolution
     */
    public String getRetryInfo() {
        return retryInfo;
    }

    /**
     * Sets the retry information for handling optimistic locking conflicts and failures.
     * 
     * @param retryInfo Guidance for retry attempts and conflict resolution strategies
     */
    public void setRetryInfo(String retryInfo) {
        this.retryInfo = retryInfo;
    }

    // Utility Methods for Response Processing

    /**
     * Utility method to configure the response for successful card update completion.
     * 
     * Sets success status, populates confirmation details, and ensures all required
     * audit information is present. Used by the service layer to create consistent
     * success responses equivalent to CCUP-CHANGES-OKAYED-AND-DONE from COBOL.
     * 
     * @param updatedCard The card entity after successful update
     * @param confirmationMessage Success confirmation message for user display
     */
    public void setUpdateSuccess(Card updatedCard, String confirmationMessage) {
        setSuccess(true);
        this.updatedCard = updatedCard;
        this.transactionConfirmation = confirmationMessage;
        this.optimisticLockSuccess = true;
        this.updateTimestamp = LocalDateTime.now();
        
        if (updatedCard != null) {
            this.versionNumber = updatedCard.getVersion();
        }
    }

    /**
     * Utility method to configure the response for optimistic locking conflict scenarios.
     * 
     * Sets failure status, provides conflict resolution guidance, and includes retry
     * information for client applications. Used when concurrent modifications are
     * detected, equivalent to DATA-WAS-CHANGED-BEFORE-UPDATE from COBOL processing.
     * 
     * @param conflictMessage Detailed message about the version conflict
     * @param retryGuidance Guidance for resolving the conflict and retrying
     */
    public void setOptimisticLockConflict(String conflictMessage, String retryGuidance) {
        setSuccess(false);
        setErrorMessage("Optimistic locking conflict detected: " + conflictMessage);
        this.optimisticLockSuccess = false;
        this.conflictResolutionInfo = conflictMessage;
        this.retryInfo = retryGuidance;
        this.updateTimestamp = LocalDateTime.now();
    }

    /**
     * Utility method to configure the response for validation failure scenarios.
     * 
     * Sets failure status and incorporates validation results for comprehensive
     * error feedback. Used when input validation or business rule validation
     * fails, equivalent to CCUP-CHANGES-NOT-OK from COBOL validation processing.
     * 
     * @param validationResult Comprehensive validation results with field-level errors
     * @param errorMessage Overall error message for the validation failure
     */
    public void setValidationFailure(ValidationResult validationResult, String errorMessage) {
        setSuccess(false);
        setErrorMessage(errorMessage);
        this.validationResult = validationResult;
        this.optimisticLockSuccess = false;
        this.updateTimestamp = LocalDateTime.now();
    }

    /**
     * Utility method to check if the response indicates a retryable operation.
     * 
     * Analyzes the response status to determine if the client should attempt
     * to retry the card update operation. Returns true for optimistic locking
     * conflicts and certain validation failures that may be resolved by user
     * action and retry attempts.
     * 
     * @return true if the operation can be retried, false for permanent failures
     */
    public boolean isRetryable() {
        // Optimistic locking conflicts are always retryable
        if (!optimisticLockSuccess && conflictResolutionInfo != null) {
            return true;
        }
        
        // Validation failures may be retryable if they contain warnings rather than errors
        if (validationResult != null && validationResult.hasErrors()) {
            return validationResult.getErrorCount() == 0 || 
                   validationResult.hasErrorsOfSeverity(ValidationResult.Severity.WARNING);
        }
        
        return false;
    }

    /**
     * Utility method to get a formatted summary of the update operation result.
     * 
     * Provides a human-readable summary of the card update operation including
     * success status, validation results, and any conflicts or errors. Useful
     * for logging, debugging, and user interface display purposes.
     * 
     * @return Formatted string summary of the update operation result
     */
    public String getUpdateSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Card Update Operation - ");
        
        if (isSuccess()) {
            summary.append("SUCCESS");
            if (updatedCard != null) {
                summary.append(" for card ").append(updatedCard.getMaskedCardNumber());
            }
            if (transactionConfirmation != null) {
                summary.append(" - ").append(transactionConfirmation);
            }
        } else {
            summary.append("FAILED");
            if (!optimisticLockSuccess) {
                summary.append(" (Optimistic Lock Conflict)");
            }
            if (validationResult != null && validationResult.hasErrors()) {
                summary.append(" (Validation Errors: ").append(validationResult.getErrorCount()).append(")");
            }
            if (hasError()) {
                summary.append(" - ").append(getErrorMessage());
            }
        }
        
        if (updateTimestamp != null) {
            summary.append(" at ").append(updateTimestamp.toString());
        }
        
        return summary.toString();
    }

    /**
     * Returns string representation of the card update response for logging and debugging.
     * 
     * Provides comprehensive string representation including all response metadata,
     * update status, validation results, and audit information. Formats key information
     * in human-readable format while preserving correlation IDs for distributed tracing.
     * 
     * @return Formatted string representation of the complete response
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CardUpdateResponseDto{");
        sb.append("success=").append(isSuccess());
        
        if (updatedCard != null) {
            sb.append(", updatedCard=").append(updatedCard.getMaskedCardNumber());
            sb.append(", version=").append(versionNumber);
        }
        
        sb.append(", optimisticLockSuccess=").append(optimisticLockSuccess);
        
        if (validationResult != null) {
            sb.append(", validationValid=").append(validationResult.isValid());
            sb.append(", validationErrors=").append(validationResult.getErrorCount());
        }
        
        if (updateTimestamp != null) {
            sb.append(", updateTimestamp=").append(updateTimestamp.toString());
        }
        
        if (getCorrelationId() != null) {
            sb.append(", correlationId='").append(getCorrelationId()).append('\'');
        }
        
        sb.append('}');
        return sb.toString();
    }
}