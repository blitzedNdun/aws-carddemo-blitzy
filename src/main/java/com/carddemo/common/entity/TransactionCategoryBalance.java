package com.carddemo.common.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Minimal JPA Entity for Transaction Category Balance tracking.
 * This is a stub implementation to support TransactionCategory compilation.
 * 
 * This entity will be fully implemented by another agent with complete
 * balance tracking functionality as specified in the database design.
 */
@Entity
@Table(name = "transaction_category_balances")
public class TransactionCategoryBalance implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "balance_id")
    private Long balanceId;

    /**
     * Many-to-one relationship with TransactionCategory.
     * Supports the required relationship for balance tracking per category.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_category", referencedColumnName = "transaction_category")
    @NotNull(message = "Transaction category cannot be null")
    private TransactionCategory transactionCategory;

    /**
     * Balance amount for the category.
     * Placeholder implementation for compilation support.
     */
    @Column(name = "balance_amount", precision = 15, scale = 2)
    private BigDecimal balanceAmount;

    /**
     * Default constructor required by JPA.
     */
    public TransactionCategoryBalance() {
    }

    /**
     * Constructor with transaction category.
     * 
     * @param transactionCategory the associated transaction category
     */
    public TransactionCategoryBalance(TransactionCategory transactionCategory) {
        this.transactionCategory = transactionCategory;
    }

    // Getters and Setters

    public Long getBalanceId() {
        return balanceId;
    }

    public void setBalanceId(Long balanceId) {
        this.balanceId = balanceId;
    }

    public TransactionCategory getTransactionCategory() {
        return transactionCategory;
    }

    public void setTransactionCategory(TransactionCategory transactionCategory) {
        this.transactionCategory = transactionCategory;
    }

    public BigDecimal getBalanceAmount() {
        return balanceAmount;
    }

    public void setBalanceAmount(BigDecimal balanceAmount) {
        this.balanceAmount = balanceAmount;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TransactionCategoryBalance that = (TransactionCategoryBalance) obj;
        return Objects.equals(balanceId, that.balanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(balanceId);
    }

    @Override
    public String toString() {
        return String.format("TransactionCategoryBalance{" +
                "balanceId=%d, " +
                "transactionCategory='%s', " +
                "balanceAmount=%s" +
                "}", 
                balanceId, 
                transactionCategory != null ? transactionCategory.getTransactionCategory() : null,
                balanceAmount);
    }
}