package com.carddemo.common.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

/**
 * TransactionCategory JPA Entity
 * 
 * Maps COBOL transaction category reference structure (CVTRA04Y.cpy) to PostgreSQL 
 * transaction_categories table for transaction categorization and balance tracking.
 * 
 * This entity serves as a reference table for transaction categorization per Section 6.2.1.2
 * with Redis caching support for frequently accessed reference data per Section 6.2.4.2.
 * 
 * Database Table: transaction_categories
 * Primary Key: transaction_category (VARCHAR(4))
 * 
 * COBOL Structure Mapping:
 * - TRAN-CAT-CD (PIC 9(04)) → transaction_category (VARCHAR(4))  
 * - TRAN-CAT-TYPE-DESC (PIC X(50)) → category_description (VARCHAR(60))
 * - Active status added for category lifecycle management
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "transaction_categories")
@Cacheable(value = "transactionCategories")
public class TransactionCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Transaction Category Code (Primary Key)
     * 
     * 4-character category code for transaction classification.
     * Maps to COBOL TRAN-CAT-CD field with VARCHAR(4) precision.
     * 
     * Validation: Must be exactly 4 characters
     * Examples: "0001", "0002", "0003", "0004"
     */
    @Id
    @Column(name = "transaction_category", length = 4, nullable = false)
    @Size(min = 4, max = 4, message = "Transaction category must be exactly 4 characters")
    private String transactionCategory;

    /**
     * Category Description
     * 
     * Descriptive name for the transaction category.
     * Maps to COBOL TRAN-CAT-TYPE-DESC field with enhanced VARCHAR(60) precision.
     * 
     * Validation: Maximum 60 characters
     * Examples: "Retail Purchase", "Cash Advance", "Balance Transfer"
     */
    @Column(name = "category_description", length = 60, nullable = false)
    @Size(max = 60, message = "Category description must not exceed 60 characters")
    private String categoryDescription;

    /**
     * Active Status
     * 
     * Boolean flag indicating if the category is active for new transactions.
     * Added for category lifecycle management in modern system.
     * 
     * true = Category is active and available for use
     * false = Category is inactive and should not be used for new transactions
     */
    @Column(name = "active_status", nullable = false)
    private Boolean activeStatus;

    /**
     * Transaction Category Balances
     * 
     * One-to-many relationship with TransactionCategoryBalance entities.
     * Enables balance tracking per category as specified in Section 6.2.1.2.
     * 
     * Uses lazy loading for optimal performance and memory usage.
     * CascadeType.ALL ensures balance records are managed with category lifecycle.
     */
    @OneToMany(mappedBy = "transactionCategory", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TransactionCategoryBalance> transactionCategoryBalances;

    /**
     * Default Constructor
     * 
     * Required by JPA specification for entity instantiation.
     * Initializes active status to true by default.
     */
    public TransactionCategory() {
        this.activeStatus = true;
    }

    /**
     * Constructor with required fields
     * 
     * @param transactionCategory 4-character category code
     * @param categoryDescription descriptive category name
     */
    public TransactionCategory(String transactionCategory, String categoryDescription) {
        this.transactionCategory = transactionCategory;
        this.categoryDescription = categoryDescription;
        this.activeStatus = true;
    }

    /**
     * Constructor with all fields
     * 
     * @param transactionCategory 4-character category code
     * @param categoryDescription descriptive category name
     * @param activeStatus category active status
     */
    public TransactionCategory(String transactionCategory, String categoryDescription, Boolean activeStatus) {
        this.transactionCategory = transactionCategory;
        this.categoryDescription = categoryDescription;
        this.activeStatus = activeStatus;
    }

    /**
     * Get Transaction Category Code
     * 
     * @return 4-character transaction category code
     */
    public String getTransactionCategory() {
        return transactionCategory;
    }

    /**
     * Set Transaction Category Code
     * 
     * Cache eviction annotation ensures Redis cache consistency when category
     * reference data is updated per Section 6.2.4.2.
     * 
     * @param transactionCategory 4-character category code
     */
    @CacheEvict(value = "transactionCategories", key = "#transactionCategory")
    public void setTransactionCategory(String transactionCategory) {
        this.transactionCategory = transactionCategory;
    }

    /**
     * Get Category Description
     * 
     * @return descriptive category name
     */
    public String getCategoryDescription() {
        return categoryDescription;
    }

    /**
     * Set Category Description
     * 
     * Cache eviction annotation ensures Redis cache consistency when category
     * reference data is updated per Section 6.2.4.2.
     * 
     * @param categoryDescription descriptive category name
     */
    @CacheEvict(value = "transactionCategories", key = "#root.target.transactionCategory")
    public void setCategoryDescription(String categoryDescription) {
        this.categoryDescription = categoryDescription;
    }

    /**
     * Get Active Status
     * 
     * @return true if category is active, false if inactive
     */
    public Boolean getActiveStatus() {
        return activeStatus;
    }

    /**
     * Set Active Status
     * 
     * Cache eviction annotation ensures Redis cache consistency when category
     * reference data is updated per Section 6.2.4.2.
     * 
     * @param activeStatus category active status
     */
    @CacheEvict(value = "transactionCategories", key = "#root.target.transactionCategory")
    public void setActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus;
    }

    /**
     * Get Transaction Category Balances
     * 
     * @return list of associated transaction category balance records
     */
    public List<TransactionCategoryBalance> getTransactionCategoryBalances() {
        return transactionCategoryBalances;
    }

    /**
     * Set Transaction Category Balances
     * 
     * @param transactionCategoryBalances list of balance records
     */
    public void setTransactionCategoryBalances(List<TransactionCategoryBalance> transactionCategoryBalances) {
        this.transactionCategoryBalances = transactionCategoryBalances;
    }

    /**
     * Add Transaction Category Balance
     * 
     * Convenience method to add a balance record to the category.
     * Maintains bidirectional relationship integrity.
     * 
     * @param balance transaction category balance to add
     */
    public void addTransactionCategoryBalance(TransactionCategoryBalance balance) {
        if (transactionCategoryBalances != null) {
            transactionCategoryBalances.add(balance);
            balance.setTransactionCategory(this);
        }
    }

    /**
     * Remove Transaction Category Balance
     * 
     * Convenience method to remove a balance record from the category.
     * Maintains bidirectional relationship integrity.
     * 
     * @param balance transaction category balance to remove
     */
    public void removeTransactionCategoryBalance(TransactionCategoryBalance balance) {
        if (transactionCategoryBalances != null) {
            transactionCategoryBalances.remove(balance);
            balance.setTransactionCategory(null);
        }
    }

    /**
     * Get Balance Count
     * 
     * Convenience method to get the number of associated balance records.
     * 
     * @return number of balance records, 0 if none
     */
    public int getBalanceCount() {
        return transactionCategoryBalances != null ? transactionCategoryBalances.size() : 0;
    }

    /**
     * Check if Category is Active
     * 
     * Convenience method for boolean status checking.
     * 
     * @return true if category is active, false otherwise
     */
    public boolean isActive() {
        return activeStatus != null && activeStatus;
    }

    /**
     * Validate Category Code Format
     * 
     * Business logic validation for 4-digit numeric category codes.
     * Ensures compliance with COBOL TRAN-CAT-CD format.
     * 
     * @return true if format is valid, false otherwise
     */
    public boolean isValidCategoryCode() {
        return transactionCategory != null && 
               transactionCategory.length() == 4 && 
               transactionCategory.matches("\\d{4}");
    }

    /**
     * String representation for debugging and logging
     * 
     * @return formatted string with key entity information
     */
    @Override
    public String toString() {
        return String.format("TransactionCategory{category='%s', description='%s', active=%s, balances=%d}",
                transactionCategory, categoryDescription, activeStatus, getBalanceCount());
    }

    /**
     * Equality comparison based on primary key
     * 
     * @param obj object to compare
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TransactionCategory that = (TransactionCategory) obj;
        return transactionCategory != null ? 
               transactionCategory.equals(that.transactionCategory) : 
               that.transactionCategory == null;
    }

    /**
     * Hash code based on primary key
     * 
     * @return hash code value
     */
    @Override
    public int hashCode() {
        return transactionCategory != null ? transactionCategory.hashCode() : 0;
    }
}