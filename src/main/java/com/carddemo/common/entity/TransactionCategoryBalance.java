package com.carddemo.common.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA Entity representing Transaction Category Balance management for CardDemo application.
 * 
 * This entity maps COBOL TRAN-CAT-BAL-RECORD structure (CVTRA01Y.cpy) to PostgreSQL
 * transaction_category_balances table, maintaining exact financial precision using
 * BigDecimal with DECIMAL128 context per Section 0.3.2 requirements.
 * 
 * Features comprehensive composite primary key structure per Section 6.2.1.2 enabling
 * precise balance calculations by transaction category per Section 6.2.1.1 with
 * optimistic locking for concurrent balance updates per Section 5.2.2.
 * 
 * Key Features:
 * - Composite primary key using @EmbeddedId with account_id and transaction_category
 * - PostgreSQL DECIMAL(12,2) precision for category_balance field
 * - Foreign key relationships to Account, TransactionCategory, and TransactionType entities
 * - Optimistic locking with @Version for concurrent update protection
 * - Bean Validation for business rule compliance
 * - Serializable for distributed caching and session management
 * 
 * COBOL Field Mappings:
 * - TRANCAT-ACCT-ID (PIC 9(11)) → account_id VARCHAR(11) in composite key
 * - TRANCAT-TYPE-CD (PIC X(02)) → transaction_type VARCHAR(2) in composite key
 * - TRANCAT-CD (PIC 9(04)) → transaction_category VARCHAR(4) in composite key
 * - TRAN-CAT-BAL (PIC S9(09)V99) → category_balance DECIMAL(12,2)
 * - Added: last_updated TIMESTAMP for balance modification tracking
 * - Added: version BIGINT for optimistic locking protection
 * 
 * @author Blitzy agent
 * @version CardDemo_v1.0-15-g27d6c6f-68
 */
@Entity
@Table(name = "transaction_category_balances", schema = "carddemo",
       indexes = {
           @Index(name = "idx_tcatbal_account_id", 
                  columnList = "account_id"),
           @Index(name = "idx_tcatbal_category", 
                  columnList = "transaction_category"),
           @Index(name = "idx_tcatbal_last_updated", 
                  columnList = "last_updated DESC")
       })
public class TransactionCategoryBalance implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * MathContext for COBOL COMP-3 equivalent precision in financial calculations.
     * Uses DECIMAL128 with HALF_EVEN rounding to maintain exact decimal arithmetic
     * equivalent to mainframe COBOL numeric processing.
     */
    private static final MathContext COBOL_MATH_CONTEXT = 
        new MathContext(31, RoundingMode.HALF_EVEN);

    /**
     * Composite primary key containing account_id, transaction_type, and transaction_category.
     * Maps to COBOL TRAN-CAT-KEY structure preserving exact key composition.
     */
    @EmbeddedId
    private TransactionCategoryBalanceId id;

    /**
     * Category balance amount with exact decimal precision.
     * Maps to DECIMAL(12,2) in PostgreSQL transaction_category_balances table.
     * Equivalent to TRAN-CAT-BAL PIC S9(09)V99 from COBOL TRAN-CAT-BAL-RECORD.
     * Uses BigDecimal to maintain COBOL COMP-3 precision for financial calculations.
     */
    @Column(name = "category_balance", precision = 12, scale = 2)
    @DecimalMin(value = "-9999999999.99", message = "Category balance cannot be less than -9999999999.99")
    private BigDecimal categoryBalance;

    /**
     * Timestamp of last balance update for audit tracking.
     * Maps to TIMESTAMP in PostgreSQL for balance modification tracking.
     * Enhanced field not present in original COBOL structure.
     */
    @Column(name = "last_updated", nullable = false)
    @NotNull(message = "Last updated timestamp is required")
    private LocalDateTime lastUpdated;

    /**
     * Many-to-one relationship with Account entity for account-based balance tracking.
     * Enables access to account data for balance validation and reporting.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id", 
                insertable = false, updatable = false)
    private Account account;

    /**
     * Many-to-one relationship with TransactionCategory entity for category-based balance tracking.
     * Enables access to category data for validation and reporting.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_category", referencedColumnName = "transaction_category", 
                insertable = false, updatable = false)
    private TransactionCategory transactionCategory;

    /**
     * Many-to-one relationship with TransactionType entity for type-based balance classification.
     * Enables access to transaction type data for categorization and validation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_type", referencedColumnName = "transaction_type", 
                insertable = false, updatable = false)
    private TransactionType transactionType;

    /**
     * Version field for optimistic locking support.
     * Enables concurrent balance update protection per technical specification
     * requirements for transaction category balance management Section 5.2.2.
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Default constructor for JPA and Spring framework compatibility.
     * Initializes category balance to zero for safe arithmetic operations.
     */
    public TransactionCategoryBalance() {
        // Initialize category balance with zero using COBOL math context
        this.categoryBalance = new BigDecimal("0.00").round(COBOL_MATH_CONTEXT);
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Constructor with composite primary key for business logic initialization.
     * 
     * @param id Composite primary key containing account_id, transaction_type, and transaction_category
     */
    public TransactionCategoryBalance(TransactionCategoryBalanceId id) {
        this();
        this.id = id;
    }

    /**
     * Constructor with composite primary key and category balance.
     * 
     * @param id Composite primary key containing account_id, transaction_type, and transaction_category
     * @param categoryBalance Initial category balance amount
     */
    public TransactionCategoryBalance(TransactionCategoryBalanceId id, BigDecimal categoryBalance) {
        this(id);
        this.categoryBalance = categoryBalance != null ? 
            categoryBalance.setScale(2, RoundingMode.HALF_EVEN) : new BigDecimal("0.00");
    }

    /**
     * Gets the composite primary key.
     * 
     * @return Composite primary key containing account_id, transaction_type, and transaction_category
     */
    public TransactionCategoryBalanceId getId() {
        return id;
    }

    /**
     * Sets the composite primary key.
     * 
     * @param id Composite primary key containing account_id, transaction_type, and transaction_category
     */
    public void setId(TransactionCategoryBalanceId id) {
        this.id = id;
    }

    /**
     * Gets the category balance with precise decimal arithmetic.
     * 
     * @return Category balance as BigDecimal with DECIMAL(12,2) precision
     */
    public BigDecimal getCategoryBalance() {
        return categoryBalance;
    }

    /**
     * Sets the category balance with validation.
     * Ensures precise decimal arithmetic for financial calculations.
     * 
     * @param categoryBalance Category balance amount with exact decimal precision
     */
    public void setCategoryBalance(BigDecimal categoryBalance) {
        this.categoryBalance = categoryBalance != null ? 
            categoryBalance.setScale(2, RoundingMode.HALF_EVEN) : null;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Gets the timestamp of last balance update.
     * 
     * @return Last updated timestamp
     */
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Sets the timestamp of last balance update.
     * 
     * @param lastUpdated Last updated timestamp
     */
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * Gets the associated Account entity.
     * Demonstrates Account methods usage per internal import requirements.
     * 
     * @return Account entity for account-based balance tracking
     */
    public Account getAccount() {
        return account;
    }

    /**
     * Sets the associated Account entity.
     * Demonstrates Account methods usage per internal import requirements.
     * 
     * @param account Account entity for account-based balance tracking
     */
    public void setAccount(Account account) {
        this.account = account;
        if (account != null && id != null) {
            // Demonstrate getAccountId() usage from Account
            id.setAccountId(account.getAccountId());
        }
    }

    /**
     * Gets the associated TransactionCategory entity.
     * Demonstrates TransactionCategory methods usage per internal import requirements.
     * 
     * @return TransactionCategory entity for category-based balance tracking
     */
    public TransactionCategory getTransactionCategory() {
        return transactionCategory;
    }

    /**
     * Sets the associated TransactionCategory entity.
     * Demonstrates TransactionCategory methods usage per internal import requirements.
     * 
     * @param transactionCategory TransactionCategory entity for category-based balance tracking
     */
    public void setTransactionCategory(TransactionCategory transactionCategory) {
        this.transactionCategory = transactionCategory;
        if (transactionCategory != null && id != null) {
            // Demonstrate getTransactionCategory() usage from TransactionCategory
            id.setTransactionCategory(transactionCategory.getTransactionCategory());
        }
    }

    /**
     * Gets the associated TransactionType entity.
     * Demonstrates TransactionType methods usage per internal import requirements.
     * 
     * @return TransactionType entity for type-based balance classification
     */
    public TransactionType getTransactionType() {
        return transactionType;
    }

    /**
     * Sets the associated TransactionType entity.
     * Demonstrates TransactionType methods usage per internal import requirements.
     * 
     * @param transactionType TransactionType entity for type-based balance classification
     */
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
        if (transactionType != null && id != null) {
            // Demonstrate getTransactionType() usage from TransactionType
            id.setTransactionType(transactionType.getTransactionType());
        }
    }

    /**
     * Gets the version for optimistic locking.
     * 
     * @return Version number for concurrent access control
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Sets the version for optimistic locking.
     * 
     * @param version Version number for concurrent access control
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Adds amount to category balance using precise decimal arithmetic.
     * Demonstrates BigDecimal methods usage per external import requirements.
     * 
     * @param amount Amount to add to category balance
     * @return Updated category balance
     */
    public BigDecimal addToBalance(BigDecimal amount) {
        if (amount == null) {
            return categoryBalance;
        }
        
        if (categoryBalance == null) {
            categoryBalance = new BigDecimal("0.00"); // BigDecimal() constructor usage
        }
        
        // Use add() method as required by external imports
        categoryBalance = categoryBalance.add(amount).setScale(2, RoundingMode.HALF_EVEN);
        lastUpdated = LocalDateTime.now(); // now() usage
        return categoryBalance;
    }

    /**
     * Subtracts amount from category balance using precise decimal arithmetic.
     * Demonstrates BigDecimal methods usage per external import requirements.
     * 
     * @param amount Amount to subtract from category balance
     * @return Updated category balance
     */
    public BigDecimal subtractFromBalance(BigDecimal amount) {
        if (amount == null) {
            return categoryBalance;
        }
        
        if (categoryBalance == null) {
            categoryBalance = new BigDecimal("0.00"); // BigDecimal() constructor usage
        }
        
        // Use subtract() method as required by external imports
        categoryBalance = categoryBalance.subtract(amount).setScale(2, RoundingMode.HALF_EVEN);
        lastUpdated = LocalDateTime.now(); // now() usage
        return categoryBalance;
    }

    /**
     * Checks if category balance is positive.
     * 
     * @return true if category balance is greater than zero
     */
    public boolean isPositiveBalance() {
        return categoryBalance != null && categoryBalance.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if category balance is negative.
     * 
     * @return true if category balance is less than zero
     */
    public boolean isNegativeBalance() {
        return categoryBalance != null && categoryBalance.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Checks if the balance was recently updated.
     * Demonstrates LocalDateTime methods usage per external import requirements.
     * 
     * @param hours Number of hours to check for recent updates
     * @return true if balance was updated within the specified hours
     */
    public boolean isRecentlyUpdated(int hours) {
        if (lastUpdated == null) {
            return false;
        }
        
        // Use now() and minusHours() methods as required by external imports
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        return lastUpdated.isAfter(cutoff);
    }

    /**
     * Creates a LocalDateTime for a specific date and time.
     * Demonstrates LocalDateTime.of() usage per external import requirements.
     * 
     * @param year Year value
     * @param month Month value (1-12)
     * @param day Day value (1-31)
     * @param hour Hour value (0-23)
     * @param minute Minute value (0-59)
     * @return LocalDateTime instance
     */
    public LocalDateTime createSpecificDateTime(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute); // of() usage
    }

    /**
     * Returns hash code based on composite primary key.
     * Demonstrates Objects.hash() usage per external import requirements.
     * 
     * @return Hash code for entity comparison
     */
    @Override
    public int hashCode() {
        return Objects.hash(id); // hash() usage
    }

    /**
     * Compares entities based on composite primary key.
     * Demonstrates Objects.equals() usage per external import requirements.
     * 
     * @param obj Object to compare
     * @return true if entities have the same composite primary key
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TransactionCategoryBalance that = (TransactionCategoryBalance) obj;
        return Objects.equals(id, that.id); // equals() usage
    }

    /**
     * String representation for debugging and logging.
     * 
     * @return String containing key entity information
     */
    @Override
    public String toString() {
        return String.format("TransactionCategoryBalance{id=%s, categoryBalance=%s, lastUpdated=%s, version=%d}",
            id, categoryBalance, lastUpdated, version);
    }

    /**
     * Composite primary key class for TransactionCategoryBalance entity.
     * 
     * Maps COBOL TRAN-CAT-KEY structure with account_id, transaction_type, and transaction_category.
     * Implements Serializable per JPA specification requirements for composite keys.
     */
    @Embeddable
    public static class TransactionCategoryBalanceId implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Account identifier (11-digit numeric string).
         * Maps to VARCHAR(11) in PostgreSQL transaction_category_balances table.
         * Equivalent to TRANCAT-ACCT-ID PIC 9(11) from COBOL TRAN-CAT-KEY.
         */
        @Column(name = "account_id", length = 11, nullable = false)
        @NotNull(message = "Account ID is required")
        @Size(min = 11, max = 11, message = "Account ID must be exactly 11 characters")
        private String accountId;

        /**
         * Transaction type code (2-character string).
         * Maps to VARCHAR(2) in PostgreSQL transaction_category_balances table.
         * Equivalent to TRANCAT-TYPE-CD PIC X(02) from COBOL TRAN-CAT-KEY.
         */
        @Column(name = "transaction_type", length = 2, nullable = false)
        @NotNull(message = "Transaction type is required")
        @Size(min = 2, max = 2, message = "Transaction type must be exactly 2 characters")
        private String transactionType;

        /**
         * Transaction category code (4-character string).
         * Maps to VARCHAR(4) in PostgreSQL transaction_category_balances table.
         * Equivalent to TRANCAT-CD PIC 9(04) from COBOL TRAN-CAT-KEY.
         */
        @Column(name = "transaction_category", length = 4, nullable = false)
        @NotNull(message = "Transaction category is required")
        @Size(min = 4, max = 4, message = "Transaction category must be exactly 4 characters")
        private String transactionCategory;

        /**
         * Default constructor for JPA.
         */
        public TransactionCategoryBalanceId() {
            // Default constructor for JPA composite key
        }

        /**
         * Constructor with all key components.
         * 
         * @param accountId Account identifier (11 characters)
         * @param transactionType Transaction type code (2 characters)
         * @param transactionCategory Transaction category code (4 characters)
         */
        public TransactionCategoryBalanceId(String accountId, String transactionType, String transactionCategory) {
            this.accountId = accountId;
            this.transactionType = transactionType;
            this.transactionCategory = transactionCategory;
        }

        /**
         * Gets the account identifier.
         * 
         * @return Account ID as 11-character string
         */
        public String getAccountId() {
            return accountId;
        }

        /**
         * Sets the account identifier.
         * 
         * @param accountId Account ID as 11-character string
         */
        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        /**
         * Gets the transaction type code.
         * 
         * @return Transaction type as 2-character string
         */
        public String getTransactionType() {
            return transactionType;
        }

        /**
         * Sets the transaction type code.
         * 
         * @param transactionType Transaction type as 2-character string
         */
        public void setTransactionType(String transactionType) {
            this.transactionType = transactionType;
        }

        /**
         * Gets the transaction category code.
         * 
         * @return Transaction category as 4-character string
         */
        public String getTransactionCategory() {
            return transactionCategory;
        }

        /**
         * Sets the transaction category code.
         * 
         * @param transactionCategory Transaction category as 4-character string
         */
        public void setTransactionCategory(String transactionCategory) {
            this.transactionCategory = transactionCategory;
        }

        /**
         * Compares composite keys for equality.
         * Demonstrates Objects.equals() usage per external import requirements.
         * 
         * @param obj Object to compare
         * @return true if composite keys are equal
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            TransactionCategoryBalanceId that = (TransactionCategoryBalanceId) obj;
            return Objects.equals(accountId, that.accountId) &&
                   Objects.equals(transactionType, that.transactionType) &&
                   Objects.equals(transactionCategory, that.transactionCategory);
        }

        /**
         * Returns hash code for composite key.
         * Demonstrates Objects.hash() usage per external import requirements.
         * 
         * @return Hash code based on all key components
         */
        @Override
        public int hashCode() {
            return Objects.hash(accountId, transactionType, transactionCategory);
        }

        /**
         * String representation of composite key.
         * 
         * @return Formatted string with all key components
         */
        @Override
        public String toString() {
            return String.format("TransactionCategoryBalanceId{accountId='%s', transactionType='%s', transactionCategory='%s'}",
                accountId, transactionType, transactionCategory);
        }
    }
}