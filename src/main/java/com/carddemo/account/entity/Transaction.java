package com.carddemo.account.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Minimal Transaction entity stub for Account.java validation testing
 * This is a temporary stub to allow Account.java compilation and testing
 * Uses unique entity name to avoid conflict with main Transaction entity
 */
@Entity(name = "AccountTransaction")
@Table(name = "transactions")
public class Transaction {
    
    @Id
    @Column(name = "transaction_id")
    private String transactionId;
    
    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;
    
    @Column(name = "transaction_amount", precision = 12, scale = 2)
    private BigDecimal transactionAmount;
    
    @Column(name = "transaction_type")
    private String transactionTypeCode;
    
    // Default constructor
    public Transaction() {
        this.transactionAmount = BigDecimal.ZERO;
    }
    
    // Minimal getters and setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    
    public BigDecimal getTransactionAmount() { return transactionAmount; }
    public void setTransactionAmount(BigDecimal transactionAmount) { this.transactionAmount = transactionAmount; }
    
    /**
     * Returns a transaction type object with getTransactionType() method
     * This is a minimal implementation for compilation purposes
     */
    public TransactionType getTransactionType() {
        return new TransactionType(transactionTypeCode);
    }
    
    public void setTransactionTypeCode(String transactionTypeCode) {
        this.transactionTypeCode = transactionTypeCode;
    }
    
    /**
     * Minimal TransactionType class to support getTransactionType() method
     */
    public static class TransactionType {
        private String transactionType;
        
        public TransactionType(String transactionType) {
            this.transactionType = transactionType;
        }
        
        public String getTransactionType() {
            return transactionType;
        }
    }
}