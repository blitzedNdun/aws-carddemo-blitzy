package com.carddemo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity class representing settlement transaction records for payment card processing.
 * Contains settlement identification, transaction references, merchant information, 
 * settlement amounts, and processing timestamps. Supports settlement-to-authorization 
 * matching for reconciliation and chargeback processing workflows.
 * 
 * This entity maps to the settlements table in PostgreSQL and maintains COBOL COMP-3 
 * precision for financial calculations using BigDecimal with scale=2 and HALF_UP rounding.
 */
@Entity
@Table(name = "settlements", indexes = {
    @Index(name = "settlement_date_idx", columnList = "settlementDate, merchantId"),
    @Index(name = "settlement_batch_idx", columnList = "batchId"),
    @Index(name = "settlement_transaction_idx", columnList = "transactionId"),
    @Index(name = "settlement_authorization_idx", columnList = "authorizationId"),
    @Index(name = "settlement_merchant_idx", columnList = "merchantId")
})
public class Settlement {

    /**
     * Primary key - Settlement transaction identifier
     */
    @Id
    @Column(name = "settlement_id", nullable = false, precision = 16)
    private Long settlementId;

    /**
     * Reference to the original transaction ID for settlement-transaction matching
     */
    @Column(name = "transaction_id", precision = 16)
    private Long transactionId;

    /**
     * Reference to the authorization ID for settlement-authorization matching
     */
    @Column(name = "authorization_id", precision = 16)
    private Long authorizationId;

    /**
     * Merchant identifier for settlement grouping and reconciliation
     */
    @Column(name = "merchant_id", length = 20)
    private String merchantId;

    /**
     * Merchant name for settlement reporting and identification
     */
    @Column(name = "merchant_name", length = 50)
    private String merchantName;

    /**
     * Settlement amount with COBOL COMP-3 precision (scale=2, HALF_UP rounding)
     * Maintains exact financial precision for payment processing
     */
    @Column(name = "settlement_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal settlementAmount;

    /**
     * Date when the settlement was initiated
     */
    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    /**
     * Timestamp when the settlement was processed
     */
    @Column(name = "processed_date")
    private LocalDateTime processedDate;

    /**
     * Settlement processing status (PENDING, COMPLETED, FAILED, CANCELLED)
     */
    @Column(name = "settlement_status", length = 10, nullable = false)
    private String settlementStatus;

    /**
     * Batch identifier for grouping settlements in batch processing
     */
    @Column(name = "batch_id", length = 20)
    private String batchId;

    /**
     * Acquirer reference number for external reconciliation
     */
    @Column(name = "acquirer_reference_number", length = 30)
    private String acquirerReferenceNumber;

    /**
     * Default constructor required by JPA
     */
    public Settlement() {
    }

    /**
     * Constructor for creating new settlement records
     */
    public Settlement(Long settlementId, Long transactionId, Long authorizationId, 
                     String merchantId, String merchantName, BigDecimal settlementAmount, 
                     LocalDate settlementDate, String settlementStatus) {
        this.settlementId = settlementId;
        this.transactionId = transactionId;
        this.authorizationId = authorizationId;
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.settlementAmount = settlementAmount != null ? 
            settlementAmount.setScale(2, RoundingMode.HALF_UP) : null;
        this.settlementDate = settlementDate;
        this.settlementStatus = settlementStatus;
    }

    // Getter and Setter methods

    public Long getSettlementId() {
        return settlementId;
    }

    public void setSettlementId(Long settlementId) {
        this.settlementId = settlementId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Long getAuthorizationId() {
        return authorizationId;
    }

    public void setAuthorizationId(Long authorizationId) {
        this.authorizationId = authorizationId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public BigDecimal getSettlementAmount() {
        return settlementAmount;
    }

    /**
     * Sets the settlement amount with COBOL COMP-3 precision preservation
     * Automatically applies scale=2 and HALF_UP rounding to match COBOL behavior
     */
    public void setSettlementAmount(BigDecimal settlementAmount) {
        if (settlementAmount != null) {
            // Ensure COBOL COMP-3 precision with scale=2 and HALF_UP rounding
            this.settlementAmount = settlementAmount.setScale(2, RoundingMode.HALF_UP);
        } else {
            this.settlementAmount = null;
        }
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public LocalDateTime getProcessedDate() {
        return processedDate;
    }

    public void setProcessedDate(LocalDateTime processedDate) {
        this.processedDate = processedDate;
    }

    public String getSettlementStatus() {
        return settlementStatus;
    }

    public void setSettlementStatus(String settlementStatus) {
        this.settlementStatus = settlementStatus;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getAcquirerReferenceNumber() {
        return acquirerReferenceNumber;
    }

    public void setAcquirerReferenceNumber(String acquirerReferenceNumber) {
        this.acquirerReferenceNumber = acquirerReferenceNumber;
    }

    // Note: Transaction and Authorization relationships are not implemented 
    // as those entities are not available in depends_on_files
    // These methods return null as placeholders for future integration
    
    /**
     * Placeholder for Transaction entity relationship
     * Returns null until Transaction entity is available
     */
    public Object getTransaction() {
        return null;
    }

    /**
     * Placeholder for Transaction entity relationship
     * No operation until Transaction entity is available
     */
    public void setTransaction(Object transaction) {
        // No operation - Transaction entity not available
    }

    /**
     * Placeholder for Authorization entity relationship
     * Returns null until Authorization entity is available
     */
    public Object getAuthorization() {
        return null;
    }

    /**
     * Placeholder for Authorization entity relationship
     * No operation until Authorization entity is available
     */
    public void setAuthorization(Object authorization) {
        // No operation - Authorization entity not available
    }

    /**
     * Business method for settlement amount validation
     * Ensures amount is positive and within valid range
     */
    public boolean isValidSettlementAmount() {
        return settlementAmount != null && 
               settlementAmount.compareTo(BigDecimal.ZERO) > 0 &&
               settlementAmount.compareTo(new BigDecimal("999999999.99")) <= 0;
    }

    /**
     * Business method to check if settlement is complete
     */
    public boolean isSettlementComplete() {
        return "COMPLETED".equals(settlementStatus) && processedDate != null;
    }

    /**
     * Business method to create a settlement amount using BigDecimal factory methods
     * with proper COBOL COMP-3 precision
     */
    public static BigDecimal createSettlementAmount(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Business method to create a settlement amount from string with validation
     */
    public static BigDecimal createSettlementAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(amountStr).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid settlement amount format: " + amountStr, e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Settlement that = (Settlement) o;
        return Objects.equals(settlementId, that.settlementId) &&
               Objects.equals(transactionId, that.transactionId) &&
               Objects.equals(authorizationId, that.authorizationId) &&
               Objects.equals(merchantId, that.merchantId) &&
               Objects.equals(merchantName, that.merchantName) &&
               Objects.equals(settlementAmount, that.settlementAmount) &&
               Objects.equals(settlementDate, that.settlementDate) &&
               Objects.equals(processedDate, that.processedDate) &&
               Objects.equals(settlementStatus, that.settlementStatus) &&
               Objects.equals(batchId, that.batchId) &&
               Objects.equals(acquirerReferenceNumber, that.acquirerReferenceNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(settlementId, transactionId, authorizationId, merchantId, 
                           merchantName, settlementAmount, settlementDate, processedDate, 
                           settlementStatus, batchId, acquirerReferenceNumber);
    }

    @Override
    public String toString() {
        return "Settlement{" +
               "settlementId=" + settlementId +
               ", transactionId=" + transactionId +
               ", authorizationId=" + authorizationId +
               ", merchantId='" + merchantId + '\'' +
               ", merchantName='" + merchantName + '\'' +
               ", settlementAmount=" + settlementAmount +
               ", settlementDate=" + settlementDate +
               ", processedDate=" + processedDate +
               ", settlementStatus='" + settlementStatus + '\'' +
               ", batchId='" + batchId + '\'' +
               ", acquirerReferenceNumber='" + acquirerReferenceNumber + '\'' +
               '}';
    }
}