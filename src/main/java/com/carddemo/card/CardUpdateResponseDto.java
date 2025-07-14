/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
 * Response DTO for card update operations with optimistic locking metadata, audit trail 
 * information, and validation results providing comprehensive update confirmation for 
 * COCRDUPC.cbl functionality.
 * 
 * This response DTO encapsulates the complete result of a card update operation,
 * including the updated card information, optimistic locking conflict resolution details,
 * comprehensive audit trail metadata, and detailed validation feedback. It preserves
 * the exact functional behavior of the original COBOL COCRDUPC program while providing
 * modern JSON-based API response structure for React frontend components.
 * 
 * Key Features:
 * - Complete updated card entity with all modified fields
 * - Optimistic locking conflict detection and resolution information
 * - Comprehensive audit trail for SOX compliance and security tracking
 * - Detailed validation results for user feedback and error handling
 * - Transaction confirmation details for operation verification
 * - Retry information for optimistic locking conflict resolution
 * - Version number tracking for concurrent modification detection
 * 
 * COBOL Program Mapping:
 * - Source: COCRDUPC.cbl (Credit Card Update Program)
 * - Transaction: CCUP (Card Update Transaction)
 * - Mapset: COCRDUP (Card Update Screen)
 * - Response Fields: Maps to CCUP-NEW-DETAILS and validation flags
 * 
 * Business Rules Preserved:
 * - Optimistic locking equivalent to CICS UPDATE with RESP checking
 * - Validation equivalent to COBOL 1200-EDIT-MAP-INPUTS paragraph
 * - Audit trail equivalent to COBOL transaction logging patterns
 * - Error handling equivalent to COBOL ABEND-ROUTINE procedures
 * 
 * @author Blitzy Agent - CardDemo Transformation Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardUpdateResponseDto extends BaseResponseDto {
    
    /**
     * Updated card entity containing all modified information after successful update.
     * Equivalent to COBOL CCUP-NEW-DETAILS structure with complete card data.
     * 
     * Includes all card fields that may have been modified:
     * - Card number (immutable, for reference)
     * - Account ID (immutable, for reference)
     * - Embossed name (CCUP-NEW-CRDNAME)
     * - Expiration date (CCUP-NEW-EXPIRAION-DATE)
     * - Active status (CCUP-NEW-CRDSTCD)
     * - CVV code (CCUP-NEW-CVV-CD)
     * 
     * Critical for React frontend to display updated card information
     * and maintain state consistency after successful update operations.
     */
    @NotNull(message = "Updated card information is required")
    private Card updatedCard;
    
    /**
     * Comprehensive validation result containing field-level validation messages,
     * business rule compliance status, and detailed error information.
     * 
     * Maps to COBOL validation logic from paragraphs:
     * - 1230-EDIT-NAME (card name validation)
     * - 1240-EDIT-CARDSTATUS (status validation)
     * - 1250-EDIT-EXPIRY-MON (expiry month validation)
     * - 1260-EDIT-EXPIRY-YEAR (expiry year validation)
     * 
     * Provides React frontend with structured validation feedback
     * for user input correction and form state management.
     */
    @NotNull(message = "Validation result is required")
    private ValidationResult validationResult;
    
    /**
     * Audit trail information containing transaction audit details including
     * user context, timestamps, operation type, and correlation information
     * for compliance and tracking purposes in card update operations.
     * 
     * Captures equivalent audit information to COBOL transaction logging:
     * - User ID from CICS EIBCALEN context
     * - Transaction timestamp equivalent to FUNCTION CURRENT-DATE
     * - Operation type (CARD_UPDATE)
     * - Correlation ID for distributed tracing
     * - Session information for security audit
     * 
     * Required for SOX compliance, security audit, and transaction tracing
     * in the microservices architecture.
     */
    @NotNull(message = "Audit information is required")
    private AuditInfo auditInfo;
    
    /**
     * Version number for optimistic locking conflict detection and resolution.
     * Equivalent to COBOL record versioning for concurrent modification detection.
     * 
     * Maps to COBOL optimistic locking pattern implemented in:
     * - 9200-WRITE-PROCESSING paragraph
     * - 9300-CHECK-CHANGE-IN-REC paragraph
     * 
     * Used by React frontend and service layer to:
     * - Detect concurrent modifications
     * - Implement retry logic for optimistic locking conflicts
     * - Provide version-aware update operations
     */
    private Long versionNumber;
    
    /**
     * Optimistic locking success indicator for concurrent modification detection.
     * Equivalent to COBOL DFHRESP(NORMAL) check after CICS REWRITE operation.
     * 
     * Maps to COBOL conditions:
     * - true: Successful update without conflicts
     * - false: Optimistic locking conflict detected
     * 
     * Critical for React frontend to determine if update succeeded
     * or if conflict resolution is required.
     */
    private boolean optimisticLockSuccess;
    
    /**
     * Conflict resolution information for optimistic locking failure scenarios.
     * Provides detailed information about what changed and how to resolve conflicts.
     * 
     * Equivalent to COBOL error messages:
     * - DATA-WAS-CHANGED-BEFORE-UPDATE
     * - COULD-NOT-LOCK-FOR-UPDATE
     * - LOCKED-BUT-UPDATE-FAILED
     * 
     * Structure includes:
     * - Conflict type (version mismatch, concurrent modification, etc.)
     * - Current version number
     * - Suggested resolution actions
     * - Retry recommendations
     */
    private String conflictResolutionInfo;
    
    /**
     * Transaction confirmation details for operation verification and audit.
     * Equivalent to COBOL transaction completion confirmation message.
     * 
     * Maps to COBOL success messages:
     * - CONFIRM-UPDATE-SUCCESS: "Changes committed to database"
     * - CCUP-CHANGES-OKAYED-AND-DONE state
     * 
     * Provides React frontend with confirmation of successful operation
     * and reference information for user feedback.
     */
    private String transactionConfirmation;
    
    /**
     * Update timestamp for precise operation timing and audit trail.
     * Equivalent to COBOL FUNCTION CURRENT-DATE with millisecond precision.
     * 
     * Used for:
     * - Audit trail chronology
     * - Performance monitoring
     * - Transaction correlation
     * - Compliance reporting
     * 
     * Generated using LocalDateTime.now() as specified in external imports.
     */
    private LocalDateTime updateTimestamp;
    
    /**
     * Retry information for optimistic locking conflict resolution guidance.
     * Provides structured guidance for handling concurrent modification conflicts.
     * 
     * Equivalent to COBOL retry logic patterns for:
     * - Automatic retry recommendations
     * - User-initiated retry guidance
     * - Conflict resolution strategies
     * 
     * Structure includes:
     * - Retry count
     * - Recommended retry delay
     * - Maximum retry attempts
     * - Retry strategy recommendations
     */
    private String retryInfo;
    
    /**
     * Default constructor for CardUpdateResponseDto with base initialization.
     * Sets up basic response structure with success status and current timestamp.
     */
    public CardUpdateResponseDto() {
        super();
        this.optimisticLockSuccess = true; // Default to success
        this.updateTimestamp = LocalDateTime.now();
        this.validationResult = ValidationResult.success();
        this.auditInfo = new AuditInfo();
    }
    
    /**
     * Constructor for successful card update response with core information.
     * 
     * @param updatedCard The updated card entity
     * @param auditInfo Audit trail information
     * @param correlationId Correlation ID for distributed tracing
     */
    public CardUpdateResponseDto(Card updatedCard, AuditInfo auditInfo, String correlationId) {
        super(correlationId);
        this.updatedCard = updatedCard;
        this.auditInfo = auditInfo;
        this.optimisticLockSuccess = true;
        this.updateTimestamp = LocalDateTime.now();
        this.validationResult = ValidationResult.success();
        this.versionNumber = updatedCard != null ? updatedCard.getVersion() : null;
        this.transactionConfirmation = "Changes committed to database";
    }
    
    /**
     * Constructor for failed card update response with error information.
     * 
     * @param validationResult Validation result containing error information
     * @param auditInfo Audit trail information
     * @param correlationId Correlation ID for distributed tracing
     */
    public CardUpdateResponseDto(ValidationResult validationResult, AuditInfo auditInfo, String correlationId) {
        super(validationResult.hasErrors() ? 
              String.join("; ", validationResult.getErrorMessages()) : 
              "Card update failed", correlationId);
        this.validationResult = validationResult;
        this.auditInfo = auditInfo;
        this.optimisticLockSuccess = false;
        this.updateTimestamp = LocalDateTime.now();
    }
    
    /**
     * Factory method for creating successful card update response.
     * 
     * @param updatedCard The updated card entity
     * @param auditInfo Audit trail information
     * @param correlationId Correlation ID for distributed tracing
     * @return CardUpdateResponseDto for successful update
     */
    public static CardUpdateResponseDto success(Card updatedCard, AuditInfo auditInfo, String correlationId) {
        return new CardUpdateResponseDto(updatedCard, auditInfo, correlationId);
    }
    
    /**
     * Factory method for creating failed card update response.
     * 
     * @param validationResult Validation result with error information
     * @param auditInfo Audit trail information
     * @param correlationId Correlation ID for distributed tracing
     * @return CardUpdateResponseDto for failed update
     */
    public static CardUpdateResponseDto failure(ValidationResult validationResult, AuditInfo auditInfo, String correlationId) {
        return new CardUpdateResponseDto(validationResult, auditInfo, correlationId);
    }
    
    /**
     * Factory method for creating optimistic locking conflict response.
     * 
     * @param conflictInfo Detailed conflict resolution information
     * @param currentVersion Current version number
     * @param auditInfo Audit trail information
     * @param correlationId Correlation ID for distributed tracing
     * @return CardUpdateResponseDto for optimistic locking conflict
     */
    public static CardUpdateResponseDto optimisticLockConflict(String conflictInfo, Long currentVersion, 
                                                              AuditInfo auditInfo, String correlationId) {
        CardUpdateResponseDto response = new CardUpdateResponseDto();
        response.setCorrelationId(correlationId);
        response.setOptimisticLockSuccess(false);
        response.setConflictResolutionInfo(conflictInfo);
        response.setVersionNumber(currentVersion);
        response.setAuditInfo(auditInfo);
        response.setValidationResult(ValidationResult.failure("Record changed by another user. Please review and retry."));
        response.setErrorMessage("Optimistic locking conflict detected");
        response.setSuccess(false);
        return response;
    }
    
    // Getter and Setter methods as specified in exports schema
    
    /**
     * Gets the updated card entity with all modified information.
     * 
     * @return The updated card entity
     */
    public Card getUpdatedCard() {
        return updatedCard;
    }
    
    /**
     * Sets the updated card entity with all modified information.
     * 
     * @param updatedCard The updated card entity
     */
    public void setUpdatedCard(Card updatedCard) {
        this.updatedCard = updatedCard;
        // Update version number from card entity
        if (updatedCard != null) {
            this.versionNumber = updatedCard.getVersion();
        }
    }
    
    /**
     * Gets the validation result containing field-level validation messages
     * and business rule compliance status.
     * 
     * @return The validation result
     */
    public ValidationResult getValidationResult() {
        return validationResult;
    }
    
    /**
     * Sets the validation result containing field-level validation messages
     * and business rule compliance status.
     * 
     * @param validationResult The validation result
     */
    public void setValidationResult(ValidationResult validationResult) {
        this.validationResult = validationResult;
        // Update overall success status based on validation result
        if (validationResult != null && !validationResult.isValid()) {
            this.setSuccess(false);
        }
    }
    
    /**
     * Gets the audit trail information for compliance and tracking purposes.
     * 
     * @return The audit trail information
     */
    public AuditInfo getAuditInfo() {
        return auditInfo;
    }
    
    /**
     * Sets the audit trail information for compliance and tracking purposes.
     * 
     * @param auditInfo The audit trail information
     */
    public void setAuditInfo(AuditInfo auditInfo) {
        this.auditInfo = auditInfo;
    }
    
    /**
     * Gets the version number for optimistic locking conflict detection.
     * 
     * @return The version number
     */
    public Long getVersionNumber() {
        return versionNumber;
    }
    
    /**
     * Sets the version number for optimistic locking conflict detection.
     * 
     * @param versionNumber The version number
     */
    public void setVersionNumber(Long versionNumber) {
        this.versionNumber = versionNumber;
    }
    
    /**
     * Gets the optimistic locking success indicator.
     * 
     * @return true if optimistic locking succeeded, false if conflicts detected
     */
    public boolean isOptimisticLockSuccess() {
        return optimisticLockSuccess;
    }
    
    /**
     * Sets the optimistic locking success indicator.
     * 
     * @param optimisticLockSuccess true if optimistic locking succeeded, false if conflicts detected
     */
    public void setOptimisticLockSuccess(boolean optimisticLockSuccess) {
        this.optimisticLockSuccess = optimisticLockSuccess;
        // Update overall success status based on optimistic locking result
        if (!optimisticLockSuccess) {
            this.setSuccess(false);
        }
    }
    
    /**
     * Gets the conflict resolution information for optimistic locking failures.
     * 
     * @return The conflict resolution information
     */
    public String getConflictResolutionInfo() {
        return conflictResolutionInfo;
    }
    
    /**
     * Sets the conflict resolution information for optimistic locking failures.
     * 
     * @param conflictResolutionInfo The conflict resolution information
     */
    public void setConflictResolutionInfo(String conflictResolutionInfo) {
        this.conflictResolutionInfo = conflictResolutionInfo;
    }
    
    /**
     * Gets the transaction confirmation details for operation verification.
     * 
     * @return The transaction confirmation details
     */
    public String getTransactionConfirmation() {
        return transactionConfirmation;
    }
    
    /**
     * Sets the transaction confirmation details for operation verification.
     * 
     * @param transactionConfirmation The transaction confirmation details
     */
    public void setTransactionConfirmation(String transactionConfirmation) {
        this.transactionConfirmation = transactionConfirmation;
    }
    
    /**
     * Gets the update timestamp for precise operation timing.
     * 
     * @return The update timestamp
     */
    public LocalDateTime getUpdateTimestamp() {
        return updateTimestamp;
    }
    
    /**
     * Sets the update timestamp for precise operation timing.
     * 
     * @param updateTimestamp The update timestamp
     */
    public void setUpdateTimestamp(LocalDateTime updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }
    
    /**
     * Gets the retry information for optimistic locking conflict resolution.
     * 
     * @return The retry information
     */
    public String getRetryInfo() {
        return retryInfo;
    }
    
    /**
     * Sets the retry information for optimistic locking conflict resolution.
     * 
     * @param retryInfo The retry information
     */
    public void setRetryInfo(String retryInfo) {
        this.retryInfo = retryInfo;
    }
    
    /**
     * Business logic method to check if the update operation can be retried.
     * Based on optimistic locking conflict status and retry information.
     * 
     * @return true if update can be retried, false otherwise
     */
    public boolean canRetry() {
        return !optimisticLockSuccess && 
               conflictResolutionInfo != null && 
               !conflictResolutionInfo.contains("LOCKED-BUT-UPDATE-FAILED");
    }
    
    /**
     * Business logic method to get formatted retry guidance for user display.
     * Provides user-friendly retry instructions based on conflict type.
     * 
     * @return Formatted retry guidance string
     */
    public String getRetryGuidance() {
        if (optimisticLockSuccess) {
            return "Update completed successfully";
        }
        
        if (conflictResolutionInfo != null) {
            if (conflictResolutionInfo.contains("DATA-WAS-CHANGED-BEFORE-UPDATE")) {
                return "Record was modified by another user. Please review the current values and retry your update.";
            }
            if (conflictResolutionInfo.contains("COULD-NOT-LOCK-FOR-UPDATE")) {
                return "Unable to lock record for update. Please wait a moment and try again.";
            }
            if (conflictResolutionInfo.contains("LOCKED-BUT-UPDATE-FAILED")) {
                return "Update failed due to system error. Please contact support if the problem persists.";
            }
        }
        
        return "Update failed. Please review your changes and try again.";
    }
    
    /**
     * Business logic method to get the overall operation status summary.
     * Provides comprehensive status information for monitoring and logging.
     * 
     * @return Operation status summary string
     */
    public String getOperationStatusSummary() {
        StringBuilder summary = new StringBuilder();
        
        if (isSuccess()) {
            summary.append("Card update completed successfully");
            if (updatedCard != null) {
                summary.append(" for card ").append(updatedCard.getMaskedCardNumber());
            }
        } else {
            summary.append("Card update failed");
            if (validationResult != null && validationResult.hasErrors()) {
                summary.append(" due to validation errors: ").append(validationResult.getErrorCount());
            }
            if (!optimisticLockSuccess) {
                summary.append(" due to optimistic locking conflict");
            }
        }
        
        return summary.toString();
    }
    
    /**
     * Provides enhanced string representation for debugging and logging.
     * Includes all key response information for comprehensive troubleshooting.
     * 
     * @return String representation of the card update response
     */
    @Override
    public String toString() {
        return String.format(
            "CardUpdateResponseDto{success=%s, optimisticLockSuccess=%s, " +
            "hasUpdatedCard=%s, hasValidationErrors=%s, versionNumber=%s, " +
            "updateTimestamp=%s, correlationId='%s'}",
            isSuccess(),
            optimisticLockSuccess,
            updatedCard != null,
            validationResult != null && validationResult.hasErrors(),
            versionNumber,
            updateTimestamp,
            getCorrelationId()
        );
    }
}