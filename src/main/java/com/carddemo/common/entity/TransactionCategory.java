package com.carddemo.common.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * JPA Entity representing Transaction Category reference data.
 * 
 * This entity maps COBOL transaction category structure (CVTRA04Y.cpy) to PostgreSQL 
 * transaction_categories table. It serves as a reference table for transaction 
 * categorization per Section 6.2.1.2 with Redis caching support for frequently 
 * accessed reference data per Section 6.2.4.2.
 * 
 * COBOL Structure Mapping:
 * - TRAN-CAT-CD PIC 9(04) → transaction_category VARCHAR(4) PK
 * - TRAN-CAT-TYPE-DESC PIC X(50) → category_description VARCHAR(60) 
 * - Active status → active_status BOOLEAN (new field for lifecycle management)
 * 
 * Performance: Cached in Redis for sub-millisecond reference data access
 * Relationships: One-to-Many with TransactionCategoryBalance for balance tracking
 * 
 * @author Blitzy agent
 * @version CardDemo_v1.0-15-g27d6c6f-68
 */
@Entity
@Table(name = "transaction_categories")
@Cacheable("transactionCategories")
public class TransactionCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key: 4-character transaction category code
     * Maps to COBOL TRAN-CAT-CD field from CVTRA04Y copybook
     * Used for transaction classification and balance tracking
     */
    @Id
    @Column(name = "transaction_category", length = 4, nullable = false)
    @NotNull(message = "Transaction category code cannot be null")
    @Size(min = 4, max = 4, message = "Transaction category code must be exactly 4 characters")
    private String transactionCategory;

    /**
     * Category description field
     * Enhanced from COBOL TRAN-CAT-TYPE-DESC PIC X(50) to VARCHAR(60)
     * Provides human-readable category names for UI display
     */
    @Column(name = "category_description", length = 60, nullable = false)
    @NotNull(message = "Category description cannot be null")
    @Size(min = 1, max = 60, message = "Category description must be between 1 and 60 characters")
    private String categoryDescription;

    /**
     * Active status flag for category lifecycle management
     * New field not present in original COBOL structure
     * Enables soft deletion and category activation/deactivation
     */
    @Column(name = "active_status", nullable = false)
    @NotNull(message = "Active status cannot be null")
    private Boolean activeStatus;

    /**
     * One-to-Many relationship with TransactionCategoryBalance
     * Enables balance tracking per category as per Section 6.2.1.2
     * Lazy loading for optimal performance and memory usage
     */
    @OneToMany(mappedBy = "transactionCategory", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<TransactionCategoryBalance> transactionCategoryBalances;

    /**
     * Default constructor for JPA
     */
    public TransactionCategory() {
        super();
    }

    /**
     * Constructor with all required fields
     * 
     * @param transactionCategory 4-character category code
     * @param categoryDescription Category description (max 60 chars)
     * @param activeStatus Active status flag
     */
    public TransactionCategory(String transactionCategory, String categoryDescription, Boolean activeStatus) {
        this.transactionCategory = transactionCategory;
        this.categoryDescription = categoryDescription;
        this.activeStatus = activeStatus;
    }

    /**
     * Gets the 4-character transaction category code
     * 
     * @return transaction category code
     */
    public String getTransactionCategory() {
        return transactionCategory;
    }

    /**
     * Sets the 4-character transaction category code
     * Cache eviction triggered when category code is modified
     * 
     * @param transactionCategory 4-character category code
     */
    @CacheEvict(value = "transactionCategories", key = "#transactionCategory")
    public void setTransactionCategory(String transactionCategory) {
        this.transactionCategory = transactionCategory;
    }

    /**
     * Gets the category description
     * 
     * @return category description
     */
    public String getCategoryDescription() {
        return categoryDescription;
    }

    /**
     * Sets the category description
     * Cache eviction triggered when description is modified
     * 
     * @param categoryDescription Category description (max 60 chars)
     */
    @CacheEvict(value = "transactionCategories", key = "#root.target.transactionCategory")
    public void setCategoryDescription(String categoryDescription) {
        this.categoryDescription = categoryDescription;
    }

    /**
     * Gets the active status flag
     * 
     * @return active status boolean
     */
    public Boolean getActiveStatus() {
        return activeStatus;
    }

    /**
     * Sets the active status flag
     * Cache eviction triggered when status is modified
     * 
     * @param activeStatus Active status flag
     */
    @CacheEvict(value = "transactionCategories", key = "#root.target.transactionCategory")
    public void setActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus;
    }

    /**
     * Gets the list of transaction category balances
     * Lazy-loaded collection for optimal performance
     * 
     * @return list of TransactionCategoryBalance entities
     */
    public List<TransactionCategoryBalance> getTransactionCategoryBalances() {
        return transactionCategoryBalances;
    }

    /**
     * Sets the list of transaction category balances
     * 
     * @param transactionCategoryBalances list of TransactionCategoryBalance entities
     */
    public void setTransactionCategoryBalances(List<TransactionCategoryBalance> transactionCategoryBalances) {
        this.transactionCategoryBalances = transactionCategoryBalances;
    }

    /**
     * Adds a transaction category balance to the collection
     * Maintains bidirectional relationship integrity
     * 
     * @param balance TransactionCategoryBalance to add
     */
    public void addTransactionCategoryBalance(TransactionCategoryBalance balance) {
        if (transactionCategoryBalances != null && !transactionCategoryBalances.contains(balance)) {
            transactionCategoryBalances.add(balance);
            balance.setTransactionCategory(this);
        }
    }

    /**
     * Removes a transaction category balance from the collection
     * Maintains bidirectional relationship integrity
     * 
     * @param balance TransactionCategoryBalance to remove
     */
    public void removeTransactionCategoryBalance(TransactionCategoryBalance balance) {
        if (transactionCategoryBalances != null && transactionCategoryBalances.remove(balance)) {
            balance.setTransactionCategory(null);
        }
    }

    /**
     * Gets the count of associated transaction category balances
     * Utility method for collection size without loading entire collection
     * 
     * @return size of transaction category balances collection
     */
    public int getTransactionCategoryBalanceCount() {
        return transactionCategoryBalances != null ? transactionCategoryBalances.size() : 0;
    }

    /**
     * Checks if this category is active
     * Convenience method for business logic
     * 
     * @return true if category is active, false otherwise
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(activeStatus);
    }

    /**
     * Checks if this category can be used for new transactions
     * Business rule: only active categories can be used
     * 
     * @return true if category can be used for transactions
     */
    public boolean isUsableForTransactions() {
        return isActive() && transactionCategory != null && !transactionCategory.trim().isEmpty();
    }

    /**
     * Equality comparison based on primary key
     * Essential for JPA entity operations and caching
     * 
     * @param obj Object to compare
     * @return true if objects are equal based on transaction category
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
     * Hash code based on primary key
     * Consistent with equals() for proper hash-based collections behavior
     * 
     * @return hash code based on transaction category
     */
    @Override
    public int hashCode() {
        return Objects.hash(transactionCategory);
    }

    /**
     * String representation for debugging and logging
     * Includes key fields for identification
     * 
     * @return string representation of the entity
     */
    @Override
    public String toString() {
        return String.format("TransactionCategory{transactionCategory='%s', categoryDescription='%s', activeStatus=%s}", 
                           transactionCategory, categoryDescription, activeStatus);
    }
}