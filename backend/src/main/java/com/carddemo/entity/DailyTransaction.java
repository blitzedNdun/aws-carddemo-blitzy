package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * JPA entity class for daily transaction batch processing records, mapped to a staging table 
 * for daily transaction imports. Mirrors the Transaction entity structure but used for batch 
 * import processing before posting to main transactions table. Contains all transaction fields 
 * for validation and processing workflows.
 * 
 * Maps COBOL structure DAILY-TRANSACTION-RECORD from app/cpy/CVTRA06Y.cpy to PostgreSQL schema.
 * Supports Spring Batch processing for daily transaction file imports with chunk-based processing.
 * Used by DailyTransactionRepository for batch validation and cross-reference operations.
 * 
 * @author Blitzy Agent - CardDemo System Migration
 * @version 1.0.0
 */
@Entity
@Table(name = "daily_transaction", 
       indexes = {
           @Index(name = "idx_daily_transaction_date", columnList = "transaction_date"),
           @Index(name = "idx_daily_transaction_card", columnList = "card_number"),
           @Index(name = "idx_daily_transaction_account", columnList = "account_id"),
           @Index(name = "idx_daily_transaction_status", columnList = "processing_status"),
           @Index(name = "idx_daily_transaction_merchant", columnList = "merchant_name")
       })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class DailyTransaction {

    /**
     * Primary key - auto-generated sequence ID for daily transaction staging records
     * Maps to COBOL DAILY-TRAN-ID field (PIC 9(9) COMP-3)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "daily_transaction_seq")
    @SequenceGenerator(name = "daily_transaction_seq", sequenceName = "daily_transaction_seq", allocationSize = 1)
    @Column(name = "daily_transaction_id", nullable = false)
    private Long dailyTransactionId;

    /**
     * Account ID for transaction relationship validation
     * Maps to COBOL ACCT-ID field (PIC 9(9) COMP-3)
     */
    @Column(name = "account_id", nullable = false, precision = 19)
    @NotNull(message = "Account ID is required")
    private Long accountId;

    /**
     * Credit card number used in transaction
     * Maps to COBOL CARD-NUM field (PIC X(16))
     */
    @Column(name = "card_number", length = 16, nullable = false)
    @NotBlank(message = "Card number is required")
    @Size(min = 16, max = 16, message = "Card number must be exactly 16 digits")
    @Pattern(regexp = "\\d{16}", message = "Card number must contain only digits")
    private String cardNumber;

    /**
     * Transaction processing date 
     * Maps to COBOL TRAN-DATE field (PIC 9(8))
     */
    @Column(name = "transaction_date", nullable = false)
    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    /**
     * Transaction processing time
     * Maps to COBOL TRAN-TIME field (PIC 9(6))
     */
    @Column(name = "transaction_time")
    private LocalTime transactionTime;

    /**
     * Transaction amount with COBOL COMP-3 precision preservation
     * Maps to COBOL TRAN-AMT field (PIC S9(9)V99 COMP-3)
     */
    @Column(name = "transaction_amount", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Transaction amount is required")
    @DecimalMin(value = "0.01", message = "Transaction amount must be positive")
    @DecimalMax(value = "999999999.99", message = "Transaction amount exceeds maximum allowed")
    private BigDecimal transactionAmount;

    /**
     * Transaction type code for classification
     * Maps to COBOL TRAN-TYPE-CD field (PIC X(2))
     */
    @Column(name = "transaction_type_code", length = 2, nullable = false)
    @NotBlank(message = "Transaction type code is required")
    @Size(min = 2, max = 2, message = "Transaction type code must be exactly 2 characters")
    private String transactionTypeCode;

    /**
     * Transaction category code for reporting
     * Maps to COBOL TRAN-CAT-CD field (PIC X(4))
     */
    @Column(name = "category_code", length = 4, nullable = false)
    @NotBlank(message = "Category code is required")
    @Size(min = 4, max = 4, message = "Category code must be exactly 4 characters")
    private String categoryCode;

    /**
     * Transaction description or memo
     * Maps to COBOL TRAN-DESC field (PIC X(100))
     */
    @Column(name = "description", length = 100)
    @Size(max = 100, message = "Description cannot exceed 100 characters")
    private String description;

    /**
     * Merchant name for transaction
     * Maps to COBOL MERCHANT-NAME field (PIC X(50))
     */
    @Column(name = "merchant_name", length = 50)
    @Size(max = 50, message = "Merchant name cannot exceed 50 characters")
    private String merchantName;

    /**
     * Merchant ID for transaction tracking
     * Maps to COBOL MERCHANT-ID field (PIC 9(9) COMP-3)
     */
    @Column(name = "merchant_id")
    private Long merchantId;

    /**
     * Unique transaction ID from external system
     * Maps to COBOL TRAN-ID field (PIC X(20))
     */
    @Column(name = "transaction_id", length = 20, unique = true)
    @Size(max = 20, message = "Transaction ID cannot exceed 20 characters")
    private String transactionId;

    /**
     * Processing status for batch workflow tracking
     * Maps to COBOL PROC-STATUS field (PIC X(10))
     * Values: NEW=Unprocessed, PENDING=Queued, PROCESSING=In Progress, COMPLETED=Success, FAILED=Failed
     */
    @Column(name = "processing_status", length = 10, nullable = false)
    @NotBlank(message = "Processing status is required")
    @Pattern(regexp = "NEW|PENDING|PROCESSING|COMPLETED|FAILED", message = "Processing status must be NEW, PENDING, PROCESSING, COMPLETED, or FAILED")
    private String processingStatus = "NEW"; // Default to NEW

    /**
     * Processing error message for failed transactions
     * Maps to COBOL ERROR-MSG field (PIC X(200))
     */
    @Column(name = "error_message", length = 200)
    @Size(max = 200, message = "Error message cannot exceed 200 characters")
    private String errorMessage;

    /**
     * File batch ID for tracking batch import groups
     * Maps to COBOL BATCH-ID field (PIC X(10))
     */
    @Column(name = "batch_id", length = 10)
    @Size(max = 10, message = "Batch ID cannot exceed 10 characters")
    private String batchId;

    /**
     * Record sequence number within batch file
     * Maps to COBOL REC-SEQ-NUM field (PIC 9(7))
     */
    @Column(name = "record_sequence")
    @Min(value = 1, message = "Record sequence must be positive")
    private Integer recordSequence;

    /**
     * Original timestamp when transaction was first received
     * Maps to COBOL ORIG-TIMESTAMP field 
     */
    @Column(name = "original_timestamp", nullable = false)
    @NotNull
    private LocalDateTime originalTimestamp;

    /**
     * Processing timestamp when transaction was processed
     * Maps to COBOL PROC-TIMESTAMP field
     */
    @Column(name = "processing_timestamp")
    private LocalDateTime processingTimestamp;

    /**
     * Date record was created in staging table
     */
    @Column(name = "created_date", nullable = false)
    @NotNull
    private LocalDateTime createdDate;

    /**
     * Date record was last updated
     */
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    /**
     * Default constructor required by JPA
     */
    public DailyTransaction() {
        this.createdDate = LocalDateTime.now();
        this.originalTimestamp = LocalDateTime.now();
        this.processingStatus = "NEW";
    }

    /**
     * Constructor for creating daily transaction with required fields
     * 
     * @param accountId Account ID for validation
     * @param cardNumber Card number for transaction
     * @param transactionDate Date of transaction
     * @param transactionAmount Transaction amount
     * @param transactionTypeCode Transaction type classification
     * @param categoryCode Transaction category
     */
    public DailyTransaction(Long accountId, String cardNumber, LocalDate transactionDate, 
                           BigDecimal transactionAmount, String transactionTypeCode, String categoryCode) {
        this();
        this.accountId = accountId;
        this.cardNumber = cardNumber;
        this.transactionDate = transactionDate;
        this.transactionAmount = transactionAmount;
        this.transactionTypeCode = transactionTypeCode;
        this.categoryCode = categoryCode;
    }

    // Getters and Setters

    public Long getDailyTransactionId() {
        return dailyTransactionId;
    }

    public void setDailyTransactionId(Long dailyTransactionId) {
        this.dailyTransactionId = dailyTransactionId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public LocalTime getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(LocalTime transactionTime) {
        this.transactionTime = transactionTime;
    }

    public BigDecimal getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(BigDecimal transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    public String getTransactionTypeCode() {
        return transactionTypeCode;
    }

    public void setTransactionTypeCode(String transactionTypeCode) {
        this.transactionTypeCode = transactionTypeCode;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(String processingStatus) {
        this.processingStatus = processingStatus;
        this.updatedDate = LocalDateTime.now();
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.updatedDate = LocalDateTime.now();
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public Integer getRecordSequence() {
        return recordSequence;
    }

    public void setRecordSequence(Integer recordSequence) {
        this.recordSequence = recordSequence;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getOriginalTimestamp() {
        return originalTimestamp;
    }

    public void setOriginalTimestamp(LocalDateTime originalTimestamp) {
        this.originalTimestamp = originalTimestamp;
    }

    public LocalDateTime getProcessingTimestamp() {
        return processingTimestamp;
    }

    public void setProcessingTimestamp(LocalDateTime processingTimestamp) {
        this.processingTimestamp = processingTimestamp;
        this.updatedDate = LocalDateTime.now();
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    // Business Logic Helper Methods

    /**
     * Checks if the daily transaction is unprocessed
     * 
     * @return true if processing status is 'NEW' or 'PENDING'
     */
    public boolean isUnprocessed() {
        return "NEW".equals(this.processingStatus) || "PENDING".equals(this.processingStatus);
    }

    /**
     * Checks if the daily transaction processing was successful
     * 
     * @return true if processing status is 'COMPLETED' (Success)
     */
    public boolean isProcessedSuccessfully() {
        return "COMPLETED".equals(this.processingStatus);
    }

    /**
     * Checks if the daily transaction has processing errors
     * 
     * @return true if processing status is 'FAILED'
     */
    public boolean hasProcessingError() {
        return "FAILED".equals(this.processingStatus);
    }

    /**
     * Marks the transaction as processing
     */
    public void markAsProcessing() {
        setProcessingStatus("PROCESSING");
        setProcessingTimestamp(LocalDateTime.now());
    }

    /**
     * Marks the transaction as successfully processed
     */
    public void markAsSuccess() {
        setProcessingStatus("COMPLETED");
        setProcessingTimestamp(LocalDateTime.now());
        setErrorMessage(null);
    }

    /**
     * Marks the transaction as failed with error message
     * 
     * @param errorMessage Error details for failed processing
     */
    public void markAsFailed(String errorMessage) {
        setProcessingStatus("FAILED");
        setProcessingTimestamp(LocalDateTime.now());
        setErrorMessage(errorMessage);
    }

    /**
     * Marks the transaction as pending in queue
     */
    public void markAsPending() {
        setProcessingStatus("PENDING");
        setProcessingTimestamp(LocalDateTime.now());
    }

    /**
     * Gets the display amount formatted for currency
     * 
     * @return formatted amount string with 2 decimal places
     */
    public String getFormattedAmount() {
        return transactionAmount != null ? String.format("$%.2f", transactionAmount) : "$0.00";
    }

    /**
     * Gets the processing status description
     * 
     * @return human-readable processing status
     */
    public String getProcessingStatusDescription() {
        switch (this.processingStatus) {
            case "NEW": return "New";
            case "PENDING": return "Pending";
            case "PROCESSING": return "Processing";
            case "COMPLETED": return "Completed";
            case "FAILED": return "Failed";
            default: return "Unknown";
        }
    }

    // Object equality and hash methods

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        DailyTransaction that = (DailyTransaction) obj;
        return dailyTransactionId != null && dailyTransactionId.equals(that.dailyTransactionId);
    }

    @Override
    public int hashCode() {
        return dailyTransactionId != null ? dailyTransactionId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return String.format("DailyTransaction{id=%d, accountId=%d, cardNumber=%s, date=%s, amount=%s, status=%s}",
            dailyTransactionId, accountId, 
            cardNumber != null ? "**** **** **** " + (cardNumber.length() > 4 ? cardNumber.substring(12) : cardNumber) : null,
            transactionDate, getFormattedAmount(), getProcessingStatusDescription());
    }
}