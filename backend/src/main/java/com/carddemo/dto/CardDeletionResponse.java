package com.carddemo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Data Transfer Object for card deletion responses in the CardDemo system.
 * 
 * This DTO provides comprehensive feedback on card cancellation operations including
 * deletion status, validation results, archive confirmation, and account update results.
 * Designed to support the COBOL-to-Java migration while maintaining functional parity
 * with original mainframe card deletion processing.
 * 
 * Key Features:
 * - Financial data precision using BigDecimal for COBOL COMP-3 compatibility
 * - Comprehensive audit trail with timestamps and user tracking
 * - Security through card number masking (last 4 digits only)
 * - Detailed error reporting and warning messages
 * - Builder pattern for flexible object construction
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 1.0
 */
public class CardDeletionResponse {

    /**
     * Enumeration for card deletion status values.
     * Maps to COBOL success/failure indicators while providing type safety.
     */
    public enum DeletionStatus {
        SUCCESS("SUCCESS"),
        FAILED("FAILED");
        
        private final String value;
        
        DeletionStatus(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * Status of the card deletion operation (SUCCESS/FAILED).
     * Required field that indicates the overall outcome of the deletion request.
     */
    private DeletionStatus deletionStatus;

    /**
     * Masked card number showing only the last 4 digits for security.
     * Format: "****-****-****-1234" preserving original card number structure
     * while protecting sensitive information in responses.
     */
    private String maskedCardNumber;

    /**
     * Reference identifier for archived card records.
     * Provides traceability to archived data for compliance and audit requirements.
     * Generated during the archival process when card data is moved to historical storage.
     */
    private String archiveId;

    /**
     * Boolean flag indicating whether the associated account was updated.
     * True when account balance, status, or related fields were modified during deletion.
     * False when only card-specific data was affected.
     */
    private Boolean accountUpdated;

    /**
     * Outstanding balance on the card at time of deletion.
     * Uses BigDecimal to maintain COBOL COMP-3 packed decimal precision.
     * Scale and rounding configured to match original mainframe behavior.
     */
    private BigDecimal outstandingBalance;

    /**
     * List of warning messages encountered during card deletion processing.
     * Provides detailed feedback on non-critical issues or validation warnings.
     * Empty list when no warnings are present.
     */
    private List<String> warningMessages;

    /**
     * User ID of the individual who processed the card deletion request.
     * Required for audit trail and compliance tracking.
     * Maps to authenticated user context from Spring Security.
     */
    private String processedBy;

    /**
     * Timestamp when the card deletion operation was completed.
     * Provides precise audit trail for regulatory compliance and troubleshooting.
     * Uses LocalDateTime for timezone-independent processing.
     */
    private LocalDateTime timestamp;

    /**
     * Default constructor for framework compatibility.
     * Initializes timestamp to current system time.
     */
    public CardDeletionResponse() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Full constructor for complete object initialization.
     *
     * @param deletionStatus Status of the deletion operation
     * @param maskedCardNumber Masked card number (last 4 digits only)
     * @param archiveId Reference to archived records
     * @param accountUpdated Whether account was updated
     * @param outstandingBalance Outstanding balance at deletion time
     * @param warningMessages List of warning messages
     * @param processedBy User who processed the deletion
     * @param timestamp When the operation was completed
     */
    public CardDeletionResponse(DeletionStatus deletionStatus, String maskedCardNumber, 
                              String archiveId, Boolean accountUpdated, 
                              BigDecimal outstandingBalance, List<String> warningMessages,
                              String processedBy, LocalDateTime timestamp) {
        this.deletionStatus = deletionStatus;
        this.maskedCardNumber = maskedCardNumber;
        this.archiveId = archiveId;
        this.accountUpdated = accountUpdated;
        this.outstandingBalance = outstandingBalance;
        this.warningMessages = warningMessages;
        this.processedBy = processedBy;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    /**
     * Creates a new builder instance for flexible object construction.
     *
     * @return New CardDeletionResponseBuilder instance
     */
    public static CardDeletionResponseBuilder builder() {
        return new CardDeletionResponseBuilder();
    }

    // Getter and Setter methods

    /**
     * Gets the deletion status.
     *
     * @return The deletion status (SUCCESS/FAILED)
     */
    public DeletionStatus getDeletionStatus() {
        return deletionStatus;
    }

    /**
     * Sets the deletion status.
     *
     * @param deletionStatus The deletion status to set
     */
    public void setDeletionStatus(DeletionStatus deletionStatus) {
        this.deletionStatus = deletionStatus;
    }

    /**
     * Gets the masked card number.
     *
     * @return The masked card number showing only last 4 digits
     */
    public String getMaskedCardNumber() {
        return maskedCardNumber;
    }

    /**
     * Sets the masked card number.
     *
     * @param maskedCardNumber The masked card number to set
     */
    public void setMaskedCardNumber(String maskedCardNumber) {
        this.maskedCardNumber = maskedCardNumber;
    }

    /**
     * Gets the archive ID.
     *
     * @return The archive reference ID
     */
    public String getArchiveId() {
        return archiveId;
    }

    /**
     * Sets the archive ID.
     *
     * @param archiveId The archive ID to set
     */
    public void setArchiveId(String archiveId) {
        this.archiveId = archiveId;
    }

    /**
     * Gets the account updated flag.
     *
     * @return True if account was updated, false otherwise
     */
    public Boolean getAccountUpdated() {
        return accountUpdated;
    }

    /**
     * Sets the account updated flag.
     *
     * @param accountUpdated The account updated flag to set
     */
    public void setAccountUpdated(Boolean accountUpdated) {
        this.accountUpdated = accountUpdated;
    }

    /**
     * Gets the outstanding balance.
     *
     * @return The outstanding balance as BigDecimal
     */
    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    /**
     * Sets the outstanding balance.
     *
     * @param outstandingBalance The outstanding balance to set
     */
    public void setOutstandingBalance(BigDecimal outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    /**
     * Gets the warning messages.
     *
     * @return List of warning messages
     */
    public List<String> getWarningMessages() {
        return warningMessages;
    }

    /**
     * Sets the warning messages.
     *
     * @param warningMessages The warning messages to set
     */
    public void setWarningMessages(List<String> warningMessages) {
        this.warningMessages = warningMessages;
    }

    /**
     * Gets the processed by user ID.
     *
     * @return The user ID who processed the deletion
     */
    public String getProcessedBy() {
        return processedBy;
    }

    /**
     * Sets the processed by user ID.
     *
     * @param processedBy The user ID to set
     */
    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    /**
     * Gets the operation timestamp.
     *
     * @return The timestamp when operation was completed
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the operation timestamp.
     *
     * @param timestamp The timestamp to set
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns a string representation of the CardDeletionResponse.
     * Includes all fields while masking sensitive information.
     *
     * @return String representation of the object
     */
    @Override
    public String toString() {
        return "CardDeletionResponse{" +
                "deletionStatus=" + deletionStatus +
                ", maskedCardNumber='" + maskedCardNumber + '\'' +
                ", archiveId='" + archiveId + '\'' +
                ", accountUpdated=" + accountUpdated +
                ", outstandingBalance=" + outstandingBalance +
                ", warningMessages=" + warningMessages +
                ", processedBy='" + processedBy + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Compares all fields for equality including null safety.
     *
     * @param obj the reference object with which to compare
     * @return true if this object is the same as the obj argument; false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        CardDeletionResponse that = (CardDeletionResponse) obj;
        
        return deletionStatus == that.deletionStatus &&
               Objects.equals(maskedCardNumber, that.maskedCardNumber) &&
               Objects.equals(archiveId, that.archiveId) &&
               Objects.equals(accountUpdated, that.accountUpdated) &&
               Objects.equals(outstandingBalance, that.outstandingBalance) &&
               Objects.equals(warningMessages, that.warningMessages) &&
               Objects.equals(processedBy, that.processedBy) &&
               Objects.equals(timestamp, that.timestamp);
    }

    /**
     * Returns a hash code value for the object.
     * Includes all fields in hash calculation for consistency with equals().
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(deletionStatus, maskedCardNumber, archiveId, 
                          accountUpdated, outstandingBalance, warningMessages, 
                          processedBy, timestamp);
    }

    /**
     * Builder class for CardDeletionResponse objects.
     * Provides fluent API for constructing instances with optional fields.
     */
    public static class CardDeletionResponseBuilder {
        private DeletionStatus deletionStatus;
        private String maskedCardNumber;
        private String archiveId;
        private Boolean accountUpdated;
        private BigDecimal outstandingBalance;
        private List<String> warningMessages;
        private String processedBy;
        private LocalDateTime timestamp;

        /**
         * Private constructor to enforce use of static builder() method.
         */
        private CardDeletionResponseBuilder() {
        }

        /**
         * Sets the deletion status.
         *
         * @param deletionStatus The deletion status
         * @return This builder instance
         */
        public CardDeletionResponseBuilder deletionStatus(DeletionStatus deletionStatus) {
            this.deletionStatus = deletionStatus;
            return this;
        }

        /**
         * Sets the masked card number.
         *
         * @param maskedCardNumber The masked card number
         * @return This builder instance
         */
        public CardDeletionResponseBuilder maskedCardNumber(String maskedCardNumber) {
            this.maskedCardNumber = maskedCardNumber;
            return this;
        }

        /**
         * Sets the archive ID.
         *
         * @param archiveId The archive ID
         * @return This builder instance
         */
        public CardDeletionResponseBuilder archiveId(String archiveId) {
            this.archiveId = archiveId;
            return this;
        }

        /**
         * Sets the account updated flag.
         *
         * @param accountUpdated The account updated flag
         * @return This builder instance
         */
        public CardDeletionResponseBuilder accountUpdated(Boolean accountUpdated) {
            this.accountUpdated = accountUpdated;
            return this;
        }

        /**
         * Sets the outstanding balance.
         *
         * @param outstandingBalance The outstanding balance
         * @return This builder instance
         */
        public CardDeletionResponseBuilder outstandingBalance(BigDecimal outstandingBalance) {
            this.outstandingBalance = outstandingBalance;
            return this;
        }

        /**
         * Sets the warning messages.
         *
         * @param warningMessages The warning messages
         * @return This builder instance
         */
        public CardDeletionResponseBuilder warningMessages(List<String> warningMessages) {
            this.warningMessages = warningMessages;
            return this;
        }

        /**
         * Sets the processed by user ID.
         *
         * @param processedBy The user ID
         * @return This builder instance
         */
        public CardDeletionResponseBuilder processedBy(String processedBy) {
            this.processedBy = processedBy;
            return this;
        }

        /**
         * Sets the operation timestamp.
         *
         * @param timestamp The timestamp
         * @return This builder instance
         */
        public CardDeletionResponseBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Builds the CardDeletionResponse instance.
         *
         * @return New CardDeletionResponse instance with configured values
         */
        public CardDeletionResponse build() {
            return new CardDeletionResponse(deletionStatus, maskedCardNumber, archiveId,
                                          accountUpdated, outstandingBalance, warningMessages,
                                          processedBy, timestamp);
        }
    }
}