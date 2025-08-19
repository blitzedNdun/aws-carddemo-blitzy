/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * JPA entity representing transaction category balance data, mapped to transaction_category_balance PostgreSQL table.
 * Corresponds to TRAN-CAT-BAL-RECORD structure from CVTRA01Y.cpy copybook.
 * 
 * Maintains running balances by transaction category for each account, enabling category-wise
 * balance tracking, reporting, and limits management. Uses composite primary key consisting
 * of account ID, category code, and balance date.
 * 
 * Field mappings from COBOL copybook CVTRA01Y.cpy:
 * - TRANCAT-ACCT-ID (PIC 9(11)) → accountId (BIGINT) 
 * - TRANCAT-CD (PIC 9(04)) → categoryCode (VARCHAR(4))
 * - TRAN-CAT-BAL (PIC S9(09)V99) → balance (NUMERIC(12,2))
 * - Balance date added for temporal balance tracking
 * 
 * Features:
 * - Composite primary key using @EmbeddedId for account ID, category code, and balance date
 * - @ManyToOne relationships with Account and TransactionCategory entities
 * - BigDecimal balance field with scale=2 for exact monetary precision
 * - JPA validation annotations for data integrity
 * - Support for date-range balance queries and balance history tracking
 * 
 * Relationships:
 * - @ManyToOne with Account entity (account_id foreign key)
 * - @ManyToOne with TransactionCategory entity (category_code foreign key)
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Entity
@Table(name = "transaction_category_balance")
public class TransactionCategoryBalance implements Serializable {

    private static final long serialVersionUID = 1L;

    // Constants for field constraints
    private static final String MAX_BALANCE_VALUE = "9999999999.99"; // PIC S9(09)V99 equivalent
    
    /**
     * Composite primary key containing account ID, category code, and balance date.
     * Maps to COBOL fields TRANCAT-ACCT-ID, TRANCAT-CD and balance date.
     */
    @EmbeddedId
    @NotNull(message = "Transaction category balance ID is required")
    private TransactionCategoryBalanceKey id;
    
    /**
     * Account relationship for balance tracking.
     * @ManyToOne relationship with Account entity using account_id foreign key.
     * Links balance record to account for account-specific balance verification.
     */
    @ManyToOne
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private Account account;
    
    /**
     * Transaction category relationship for balance categorization.
     * @ManyToOne relationship with TransactionCategory entity using category_code foreign key.
     * Links balance record to category for category-specific balance grouping.
     */
    @ManyToOne
    @JoinColumn(name = "category_code", insertable = false, updatable = false)
    private TransactionCategory transactionCategory;
    
    /**
     * Category balance amount.
     * Maps to TRAN-CAT-BAL field from COBOL copybook (PIC S9(09)V99).
     * Uses BigDecimal with scale=2 to preserve COBOL COMP-3 packed decimal precision.
     * Supports positive and negative balances for debit/credit operations.
     */
    @Column(name = "balance", nullable = false, precision = 12, scale = 2)
    @NotNull(message = "Balance is required")
    @DecimalMax(value = MAX_BALANCE_VALUE, message = "Balance cannot exceed " + MAX_BALANCE_VALUE)
    private BigDecimal balance;
    
    /**
     * Default constructor for JPA.
     */
    public TransactionCategoryBalance() {
    }
    
    /**
     * Constructor with composite key.
     * 
     * @param id the composite primary key
     */
    public TransactionCategoryBalance(TransactionCategoryBalanceKey id) {
        this.id = id;
    }
    
    /**
     * Constructor with all required fields.
     * 
     * @param accountId the account ID (11 digits)
     * @param categoryCode the category code (4 characters)
     * @param balanceDate the balance date
     * @param balance the balance amount
     */
    public TransactionCategoryBalance(Long accountId, String categoryCode, 
                                    LocalDate balanceDate, BigDecimal balance) {
        this.id = new TransactionCategoryBalanceKey(accountId, categoryCode, balanceDate);
        this.balance = balance;
    }
    
    /**
     * JPA lifecycle callback for validation before persisting a new balance record.
     * Performs comprehensive field validation using COBOL-equivalent validation rules.
     * Initializes default values and ensures data consistency.
     */
    @PrePersist
    public void validateBeforeInsert() {
        initializeDefaults();
        formatFields();
        performBalanceValidation();
    }
    
    /**
     * JPA lifecycle callback for validation before updating an existing balance record.
     * Performs comprehensive field validation using COBOL-equivalent validation rules.
     * Ensures data consistency and business rule compliance.
     */
    @PreUpdate
    public void validateBeforeUpdate() {
        formatFields();
        performBalanceValidation();
    }
    
    /**
     * Performs comprehensive balance field validation using business rules.
     * Validates balance amounts, dates, and key field relationships.
     * Ensures data integrity matching COBOL validation patterns.
     */
    private void performBalanceValidation() {
        // Validate composite key is present
        if (id == null) {
            throw new IllegalArgumentException("Transaction category balance ID cannot be null");
        }
        
        // Validate account ID is present
        if (id.getAccountId() == null || id.getAccountId() <= 0) {
            throw new IllegalArgumentException("Account ID must be a positive number");
        }
        
        // Validate category code is present and properly formatted
        if (id.getCategoryCode() == null || id.getCategoryCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Category code is required");
        }
        
        if (!id.getCategoryCode().matches("^[A-Z0-9]{4}$")) {
            throw new IllegalArgumentException("Category code must be 4 alphanumeric characters");
        }
        
        // Validate balance date is present and not in the future
        if (id.getBalanceDate() == null) {
            throw new IllegalArgumentException("Balance date is required");
        }
        
        if (id.getBalanceDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Balance date cannot be in the future");
        }
        
        // Validate balance amount is within acceptable range
        if (balance != null) {
            BigDecimal maxValue = new BigDecimal(MAX_BALANCE_VALUE);
            BigDecimal minValue = maxValue.negate();
            
            if (balance.compareTo(maxValue) > 0) {
                throw new IllegalArgumentException("Balance exceeds maximum allowed value");
            }
            
            if (balance.compareTo(minValue) < 0) {
                throw new IllegalArgumentException("Balance exceeds minimum allowed value");
            }
        }
    }
    
    /**
     * Initializes default values for new balance records.
     * Sets COBOL-compatible defaults matching mainframe initialization patterns.
     */
    private void initializeDefaults() {
        if (balance == null) {
            balance = BigDecimal.ZERO.setScale(2);
        }
        
        if (id != null && id.getBalanceDate() == null) {
            id.setBalanceDate(LocalDate.now());
        }
    }
    
    /**
     * Formats balance fields to ensure COBOL-compatible data formats.
     * Applies proper scaling for BigDecimal amounts and normalizes key fields.
     */
    private void formatFields() {
        // Ensure proper scale for balance amount (scale=2 for COMP-3 precision)
        if (balance != null) {
            balance = balance.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        
        // Format category code to uppercase
        if (id != null && id.getCategoryCode() != null) {
            id.setCategoryCode(id.getCategoryCode().trim().toUpperCase());
        }
    }

    // Getters and Setters

    public TransactionCategoryBalanceKey getId() {
        return id;
    }

    public void setId(TransactionCategoryBalanceKey id) {
        this.id = id;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public TransactionCategory getTransactionCategory() {
        return transactionCategory;
    }

    public void setTransactionCategory(TransactionCategory transactionCategory) {
        this.transactionCategory = transactionCategory;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    // Convenience methods for accessing composite key fields

    /**
     * Get account ID from the composite key.
     * 
     * @return the account ID
     */
    public Long getAccountId() {
        return id != null ? id.getAccountId() : null;
    }

    /**
     * Get category code from the composite key.
     * 
     * @return the category code
     */
    public String getCategoryCode() {
        return id != null ? id.getCategoryCode() : null;
    }

    /**
     * Get balance date from the composite key.
     * 
     * @return the balance date
     */
    public LocalDate getBalanceDate() {
        return id != null ? id.getBalanceDate() : null;
    }

    // Object Methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionCategoryBalance that = (TransactionCategoryBalance) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TransactionCategoryBalance{" +
                "id=" + id +
                ", balance=" + balance +
                ", accountId=" + getAccountId() +
                ", categoryCode=" + getCategoryCode() +
                ", balanceDate=" + getBalanceDate() +
                '}';
    }

    /**
     * Embedded composite primary key class for TransactionCategoryBalance entity.
     * 
     * Represents the composite key structure with account_id, category_code, and balance_date.
     * Maps to COBOL fields TRANCAT-ACCT-ID, TRANCAT-CD from CVTRA01Y.cpy plus balance_date
     * for temporal balance tracking support.
     * 
     * Features:
     * - Account ID (BIGINT) for account identification
     * - Category code (VARCHAR(4)) for transaction categorization  
     * - Balance date (DATE) for temporal balance tracking
     * - Proper equals() and hashCode() implementation for JPA
     * - Validation constraints for data integrity
     */
    @Embeddable
    public static class TransactionCategoryBalanceKey implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Account ID for balance tracking.
         * Maps to COBOL field TRANCAT-ACCT-ID (PIC 9(11)).
         */
        @Column(name = "account_id", nullable = false)
        @NotNull(message = "Account ID is required")
        private Long accountId;

        /**
         * Category code for balance categorization.
         * Maps to COBOL field TRANCAT-CD (PIC 9(04)).
         */
        @Column(name = "category_code", length = 4, nullable = false)
        @NotNull(message = "Category code is required")
        private String categoryCode;

        /**
         * Balance date for temporal tracking.
         * Not present in original COBOL but required for balance history management.
         */
        @Column(name = "balance_date", nullable = false)
        @NotNull(message = "Balance date is required")
        private LocalDate balanceDate;

        /**
         * Default constructor for JPA.
         */
        public TransactionCategoryBalanceKey() {
        }

        /**
         * Constructor with all key fields.
         * 
         * @param accountId the account ID
         * @param categoryCode the category code (4 characters)
         * @param balanceDate the balance date
         */
        public TransactionCategoryBalanceKey(Long accountId, String categoryCode, LocalDate balanceDate) {
            this.accountId = accountId;
            this.categoryCode = categoryCode;
            this.balanceDate = balanceDate;
        }

        // Getters and Setters

        public Long getAccountId() {
            return accountId;
        }

        public void setAccountId(Long accountId) {
            this.accountId = accountId;
        }

        public String getCategoryCode() {
            return categoryCode;
        }

        public void setCategoryCode(String categoryCode) {
            this.categoryCode = categoryCode;
        }

        public LocalDate getBalanceDate() {
            return balanceDate;
        }

        public void setBalanceDate(LocalDate balanceDate) {
            this.balanceDate = balanceDate;
        }

        // Object Methods

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TransactionCategoryBalanceKey that = (TransactionCategoryBalanceKey) o;
            return Objects.equals(accountId, that.accountId) &&
                   Objects.equals(categoryCode, that.categoryCode) &&
                   Objects.equals(balanceDate, that.balanceDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(accountId, categoryCode, balanceDate);
        }

        @Override
        public String toString() {
            return "TransactionCategoryBalanceKey{" +
                    "accountId=" + accountId +
                    ", categoryCode='" + categoryCode + '\'' +
                    ", balanceDate=" + balanceDate +
                    '}';
        }
    }
}