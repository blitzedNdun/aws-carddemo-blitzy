package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity class representing settlement transaction records for credit card processing.
 * This entity supports the modernized settlement processing system that replaces
 * the legacy COBOL batch program CBTRN02C for transaction posting, reconciliation,
 * and balance management operations.
 * 
 * Settlement processing involves matching authorization transactions with actual
 * payment settlements from merchants, supporting real-time reconciliation and
 * exception processing that was previously handled by mainframe batch jobs.
 * 
 * The entity maps to the PostgreSQL settlements table with appropriate indexing
 * for high-performance queries during batch reconciliation processing.
 * 
 * @author Blitzy agent
 * @version 1.0
 * @since Spring Boot 3.2.x migration from COBOL batch processing
 */
@Entity
@Table(name = "settlements", indexes = {
    @Index(name = "idx_settlement_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_settlement_authorization_id", columnList = "authorization_id"),
    @Index(name = "idx_settlement_date", columnList = "settlement_date"),
    @Index(name = "idx_settlement_merchant_date", columnList = "merchant_id, settlement_date"),
    @Index(name = "idx_settlement_status", columnList = "settlement_status")
})
public class Settlement {

    /**
     * Enumeration for settlement status tracking through the settlement lifecycle.
     * Supports settlement workflow management and exception processing.
     */
    public enum SettlementStatus {
        PENDING,
        PROCESSED,
        MATCHED,
        UNMATCHED,
        REJECTED,
        REVERSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long id;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Size(max = 50, message = "Authorization ID must not exceed 50 characters")
    @Column(name = "authorization_id", length = 50)
    private String authorizationId;

    @NotNull(message = "Settlement date is required")
    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @NotNull(message = "Merchant ID is required")
    @Size(max = 20, message = "Merchant ID must not exceed 20 characters")
    @Column(name = "merchant_id", nullable = false, length = 20)
    private String merchantId;

    @NotNull(message = "Settlement amount is required")
    @DecimalMin(value = "0.00", message = "Settlement amount must be non-negative")
    @Digits(integer = 12, fraction = 2, message = "Settlement amount must have at most 12 integer digits and 2 fractional digits")
    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Settlement status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", nullable = false, length = 20)
    private SettlementStatus settlementStatus;

    @Size(max = 255, message = "Settlement description must not exceed 255 characters")
    @Column(name = "settlement_description")
    private String settlementDescription;

    @Size(max = 50, message = "Settlement reference must not exceed 50 characters")
    @Column(name = "settlement_reference", length = 50)
    private String settlementReference;

    @Column(name = "processing_date")
    private LocalDate processingDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Size(max = 100, message = "Created by must not exceed 100 characters")
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Size(max = 100, message = "Updated by must not exceed 100 characters")
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /**
     * Default constructor for JPA.
     */
    public Settlement() {
        this.createdAt = LocalDateTime.now();
        this.settlementStatus = SettlementStatus.PENDING;
    }

    /**
     * Constructor with required fields for settlement creation.
     * 
     * @param merchantId the merchant identifier
     * @param amount the settlement amount
     * @param settlementDate the settlement date
     */
    public Settlement(String merchantId, BigDecimal amount, LocalDate settlementDate) {
        this();
        this.merchantId = merchantId;
        this.amount = amount;
        this.settlementDate = settlementDate;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getAuthorizationId() {
        return authorizationId;
    }

    public void setAuthorizationId(String authorizationId) {
        this.authorizationId = authorizationId;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public SettlementStatus getSettlementStatus() {
        return settlementStatus;
    }

    public void setSettlementStatus(SettlementStatus settlementStatus) {
        this.settlementStatus = settlementStatus;
    }

    public String getSettlementDescription() {
        return settlementDescription;
    }

    public void setSettlementDescription(String settlementDescription) {
        this.settlementDescription = settlementDescription;
    }

    public String getSettlementReference() {
        return settlementReference;
    }

    public void setSettlementReference(String settlementReference) {
        this.settlementReference = settlementReference;
    }

    public LocalDate getProcessingDate() {
        return processingDate;
    }

    public void setProcessingDate(LocalDate processingDate) {
        this.processingDate = processingDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Settlement)) return false;
        Settlement that = (Settlement) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(authorizationId, that.authorizationId) &&
               Objects.equals(merchantId, that.merchantId) &&
               Objects.equals(settlementDate, that.settlementDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, authorizationId, merchantId, settlementDate);
    }

    @Override
    public String toString() {
        return "Settlement{" +
               "id=" + id +
               ", transactionId=" + transactionId +
               ", authorizationId='" + authorizationId + '\'' +
               ", settlementDate=" + settlementDate +
               ", merchantId='" + merchantId + '\'' +
               ", amount=" + amount +
               ", settlementStatus=" + settlementStatus +
               ", createdAt=" + createdAt +
               '}';
    }
}