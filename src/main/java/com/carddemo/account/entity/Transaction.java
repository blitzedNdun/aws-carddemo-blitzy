package com.carddemo.account.entity;

import jakarta.persistence.*;

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
    
    // Default constructor
    public Transaction() {}
    
    // Minimal getters and setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
}