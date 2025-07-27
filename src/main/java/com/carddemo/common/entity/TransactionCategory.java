package com.carddemo.common.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JPA Entity for Transaction Category reference table mapping COBOL CVTRA04Y structure
 * to PostgreSQL transaction_categories table with Redis caching support.
 * 
 * This entity serves as a reference table for transaction categorization per Section 6.2.1.2,
 * implementing caching per Section 6.2.4.2 for frequently accessed reference data.
 * 
 * Converted from COBOL transaction category reference structure:
 * - TRAN-CAT-CD PIC 9(04) -> transaction_category VARCHAR(4) primary key
 * - TRAN-CAT-TYPE-DESC PIC X(50) -> category_description VARCHAR(60)
 * - Added active_status BOOLEAN for category lifecycle management
 * 
 * Supports relationship with TransactionCategoryBalance for balance tracking per category
 * as specified in the database design requirements.
 */
@Entity
@Table(name = "transaction_categories")
@Cacheable(value = "transactionCategories", key = "#transactionCategory")
public class TransactionCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Transaction category code - 4-character primary key for category classification.
     * Maps to COBOL TRAN-CAT-CD PIC 9(04) with VARCHAR storage for PostgreSQL compatibility.
     * 
     * Example values: "0001", "0002", "0003", etc.
     */
    @Id
    @Column(name = "transaction_category", length = 4, nullable = false, unique = true)
    @NotBlank(message = "Transaction category code cannot be blank")
    @Size(min = 4, max = 4, message = "Transaction category must be exactly 4 characters")
    private String transactionCategory;

    /**
     * Category description providing human-readable category name.
     * Maps to COBOL TRAN-CAT-TYPE-DESC PIC X(50) with expanded 60-character limit
     * to accommodate enhanced category descriptions in modern system.
     * 
     * Example values: "Retail Purchases", "Cash Advances", "Balance Transfers"
     */
    @Column(name = "category_description", length = 60, nullable = false)
    @NotBlank(message = "Category description cannot be blank")
    @Size(min = 1, max = 60, message = "Category description must be between 1 and 60 characters")
    private String categoryDescription;

    /**
     * Active status flag for category lifecycle management.
     * Added to support category activation/deactivation without data deletion,
     * enabling business continuity while managing category evolution.
     * 
     * true = Category is active and available for new transactions
     * false = Category is inactive, existing transactions preserved but no new assignments
     */
    @Column(name = "active_status", nullable = false)
    @NotNull(message = "Active status cannot be null")
    private Boolean activeStatus = Boolean.TRUE;

    /**
     * One-to-many relationship with TransactionCategoryBalance entities.
     * Enables balance tracking per category as required by the database design.
     * 
     * Uses LAZY fetching to optimize performance for reference table access patterns.
     * CascadeType.PERSIST and MERGE support balance creation and updates.
     */
    @OneToMany(mappedBy = "transactionCategory", 
               fetch = FetchType.LAZY, 
               cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<TransactionCategoryBalance> transactionCategoryBalances = new ArrayList<>();

    /**
     * Default constructor required by JPA specification.
     * Initializes active status to true by default for new categories.
     */
    public TransactionCategory() {
        this.activeStatus = Boolean.TRUE;
        this.transactionCategoryBalances = new ArrayList<>();
    }

    /**
     * Constructor with required fields for category creation.
     * 
     * @param transactionCategory 4-character category code
     * @param categoryDescription descriptive category name
     */
    public TransactionCategory(String transactionCategory, String categoryDescription) {
        this();
        this.transactionCategory = transactionCategory;
        this.categoryDescription = categoryDescription;
    }

    /**
     * Full constructor with all fields.
     * 
     * @param transactionCategory 4-character category code
     * @param categoryDescription descriptive category name
     * @param activeStatus category active status flag
     */
    public TransactionCategory(String transactionCategory, String categoryDescription, Boolean activeStatus) {
        this(transactionCategory, categoryDescription);
        this.activeStatus = activeStatus != null ? activeStatus : Boolean.TRUE;
    }

    // Getter and Setter methods

    /**
     * Gets the transaction category code.
     * 
     * @return 4-character transaction category code
     */
    public String getTransactionCategory() {
        return transactionCategory;
    }

    /**
     * Sets the transaction category code.
     * Cache eviction is triggered when category code is modified to ensure Redis consistency.
     * 
     * @param transactionCategory 4-character category code
     */
    @CacheEvict(value = "transactionCategories", key = "#transactionCategory")
    public void setTransactionCategory(String transactionCategory) {
        this.transactionCategory = transactionCategory;
    }

    /**
     * Gets the category description.
     * 
     * @return descriptive category name
     */
    public String getCategoryDescription() {
        return categoryDescription;
    }

    /**
     * Sets the category description.
     * Cache eviction is triggered for reference data consistency.
     * 
     * @param categoryDescription descriptive category name
     */
    @CacheEvict(value = "transactionCategories", key = "#root.target.transactionCategory")
    public void setCategoryDescription(String categoryDescription) {
        this.categoryDescription = categoryDescription;
    }

    /**
     * Gets the active status of the category.
     * 
     * @return true if category is active, false if inactive
     */
    public Boolean getActiveStatus() {
        return activeStatus;
    }

    /**
     * Sets the active status of the category.
     * Cache eviction is triggered to ensure status changes are reflected in Redis.
     * 
     * @param activeStatus category active status flag
     */
    @CacheEvict(value = "transactionCategories", key = "#root.target.transactionCategory")
    public void setActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus != null ? activeStatus : Boolean.TRUE;
    }

    /**
     * Gets the list of transaction category balances associated with this category.
     * 
     * @return list of TransactionCategoryBalance entities
     */
    public List<TransactionCategoryBalance> getTransactionCategoryBalances() {
        if (transactionCategoryBalances == null) {
            transactionCategoryBalances = new ArrayList<>();
        }
        return transactionCategoryBalances;
    }

    /**
     * Sets the list of transaction category balances.
     * Ensures bidirectional relationship consistency by setting the category reference
     * in each balance entity.
     * 
     * @param transactionCategoryBalances list of TransactionCategoryBalance entities
     */
    public void setTransactionCategoryBalances(List<TransactionCategoryBalance> transactionCategoryBalances) {
        this.transactionCategoryBalances = transactionCategoryBalances != null ? 
            transactionCategoryBalances : new ArrayList<>();
        
        // Ensure bidirectional relationship consistency
        for (TransactionCategoryBalance balance : this.transactionCategoryBalances) {
            if (balance != null && !this.equals(balance.getTransactionCategory())) {
                balance.setTransactionCategory(this);
            }
        }
    }

    /**
     * Adds a transaction category balance to this category.
     * Maintains bidirectional relationship consistency.
     * 
     * @param balance TransactionCategoryBalance to add
     */
    public void addTransactionCategoryBalance(TransactionCategoryBalance balance) {
        if (balance != null) {
            getTransactionCategoryBalances().add(balance);
            balance.setTransactionCategory(this);
        }
    }

    /**
     * Removes a transaction category balance from this category.
     * Maintains bidirectional relationship consistency.
     * 
     * @param balance TransactionCategoryBalance to remove
     */
    public void removeTransactionCategoryBalance(TransactionCategoryBalance balance) {
        if (balance != null && getTransactionCategoryBalances().remove(balance)) {
            balance.setTransactionCategory(null);
        }
    }

    /**
     * Utility method to check if the category is currently active.
     * 
     * @return true if category is active and available for use
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(activeStatus);
    }

    /**
     * Utility method to activate the category.
     * Triggers cache eviction to ensure status change is reflected in Redis.
     */
    @CacheEvict(value = "transactionCategories", key = "#root.target.transactionCategory")
    public void activate() {
        this.activeStatus = Boolean.TRUE;
    }

    /**
     * Utility method to deactivate the category.
     * Triggers cache eviction to ensure status change is reflected in Redis.
     */
    @CacheEvict(value = "transactionCategories", key = "#root.target.transactionCategory")
    public void deactivate() {
        this.activeStatus = Boolean.FALSE;
    }

    /**
     * Equals method for entity comparison based on primary key.
     * Essential for JPA entity management and collection operations.
     * 
     * @param obj object to compare
     * @return true if objects are equal based on transaction category code
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TransactionCategory that = (TransactionCategory) obj;
        return Objects.equals(transactionCategory, that.transactionCategory);
    }

    /**
     * Hash code method based on primary key for proper collection behavior.
     * 
     * @return hash code based on transaction category code
     */
    @Override
    public int hashCode() {
        return Objects.hash(transactionCategory);
    }

    /**
     * String representation for debugging and logging purposes.
     * Includes key fields without exposing sensitive data.
     * 
     * @return string representation of the transaction category
     */
    @Override
    public String toString() {
        return String.format("TransactionCategory{" +
                "transactionCategory='%s', " +
                "categoryDescription='%s', " +
                "activeStatus=%s, " +
                "balanceCount=%d" +
                "}", 
                transactionCategory, 
                categoryDescription, 
                activeStatus,
                transactionCategoryBalances != null ? transactionCategoryBalances.size() : 0);
    }
}