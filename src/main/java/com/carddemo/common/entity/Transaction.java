package com.carddemo.common.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.io.Serializable;

/**
 * JPA Transaction entity mapping COBOL transaction record to PostgreSQL transactions table
 * with UUID primary key, BigDecimal financial precision, timestamp partitioning support,
 * and foreign key relationships for comprehensive transaction management
 */
@Entity(name = "CommonTransaction")
@Table(name = "transactions")
public class Transaction implements Serializable {

    @Id
    @Column(name = "transaction_id", length = 16)
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_number", referencedColumnName = "card_number")
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_type", referencedColumnName = "transaction_type")
    private TransactionType transactionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_category", referencedColumnName = "transaction_category")
    private TransactionCategory transactionCategory;

    @Column(name = "transaction_amount", precision = 12, scale = 2, nullable = false)
    @DecimalMin(value = "0.00", message = "Transaction amount must be non-negative")
    private BigDecimal amount;

    @Column(name = "description", length = 50)
    @Size(max = 50, message = "Description must not exceed 50 characters")
    private String description;

    @Column(name = "transaction_timestamp", nullable = false)
    private LocalDateTime processingTimestamp;

    @Column(name = "original_timestamp")
    private LocalDateTime originalTimestamp;

    @Column(name = "merchant_name", length = 50)
    @Size(max = 50, message = "Merchant name must not exceed 50 characters")
    private String merchantName;

    @Column(name = "merchant_city", length = 30)
    @Size(max = 30, message = "Merchant city must not exceed 30 characters")
    private String merchantCity;

    @Column(name = "merchant_zip", length = 10)
    @Size(max = 10, message = "Merchant zip must not exceed 10 characters")
    private String merchantZip;

    @Column(name = "merchant_id", length = 20)
    @Size(max = 20, message = "Merchant ID must not exceed 20 characters")
    private String merchantId;

    @Column(name = "card_number", length = 16, insertable = false, updatable = false)
    @Size(max = 16, message = "Card number must not exceed 16 characters")
    private String cardNumber;

    @Column(name = "transaction_source", length = 10)
    @Size(max = 10, message = "Transaction source must not exceed 10 characters")
    private String source;

    @Column(name = "transaction_category", length = 4, insertable = false, updatable = false)
    @Size(max = 4, message = "Category code must not exceed 4 characters")
    private String categoryCode;

    // Default constructor
    public Transaction() {
        this.transactionId = UUID.randomUUID().toString().substring(0, 16);
        this.amount = BigDecimal.ZERO;
        this.processingTimestamp = LocalDateTime.now();
    }

    // Getters and setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public String getAccountId() { 
        return account != null ? account.getAccountId() : null;
    }

    public Card getCard() { return card; }
    public void setCard(Card card) { 
        this.card = card;
        if (card != null) {
            this.cardNumber = card.getCardNumber();
        }
    }

    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { 
        this.transactionType = transactionType; 
    }

    // Method to get transaction type as enum for business logic
    public com.carddemo.common.enums.TransactionType getTransactionTypeEnum() {
        if (transactionType != null && transactionType.getTransactionType() != null) {
            try {
                return com.carddemo.common.enums.TransactionType.valueOf(transactionType.getTransactionType());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    public void setTransactionTypeFromEnum(com.carddemo.common.enums.TransactionType transactionType) {
        // Handle enum-based setter for backward compatibility
        if (transactionType != null) {
            // Find or create TransactionType entity by looking it up
            // For now, create a new entity - in real app this would be a lookup
            TransactionType typeEntity = new TransactionType();
            typeEntity.setTransactionType(transactionType.name());
            this.transactionType = typeEntity;
        }
    }

    public TransactionCategory getTransactionCategory() { return transactionCategory; }
    public void setTransactionCategory(TransactionCategory transactionCategory) { 
        this.transactionCategory = transactionCategory; 
        if (transactionCategory != null) {
            this.categoryCode = transactionCategory.getTransactionCategory();
        }
    }

    // Method to get transaction category as enum for business logic
    public com.carddemo.common.enums.TransactionCategory getTransactionCategoryEnum() {
        if (categoryCode != null) {
            try {
                return com.carddemo.common.enums.TransactionCategory.valueOf(categoryCode);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    public void setCategoryCodeFromEnum(com.carddemo.common.enums.TransactionCategory categoryCode) {
        // Handle enum-based setter for backward compatibility
        if (categoryCode != null) {
            TransactionCategory categoryEntity = new TransactionCategory();
            categoryEntity.setTransactionCategory(categoryCode.name());
            this.transactionCategory = categoryEntity;
            this.categoryCode = categoryCode.name();
        }
    }

    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getProcessingTimestamp() { return processingTimestamp; }
    public void setProcessingTimestamp(LocalDateTime processingTimestamp) { 
        this.processingTimestamp = processingTimestamp; 
    }

    public LocalDateTime getOriginalTimestamp() { return originalTimestamp; }
    public void setOriginalTimestamp(LocalDateTime originalTimestamp) { 
        this.originalTimestamp = originalTimestamp; 
    }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public String getMerchantCity() { return merchantCity; }
    public void setMerchantCity(String merchantCity) { this.merchantCity = merchantCity; }

    public String getMerchantZip() { return merchantZip; }
    public void setMerchantZip(String merchantZip) { this.merchantZip = merchantZip; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId='" + transactionId + '\'' +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", processingTimestamp=" + processingTimestamp +
                '}';
    }


}