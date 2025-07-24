package com.carddemo.common.entity;

import com.carddemo.common.entity.Account;
import com.carddemo.common.entity.TransactionCategory;
import com.carddemo.common.entity.TransactionType;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * JPA TransactionCategoryBalance entity mapping COBOL transaction category balance structure
 * to PostgreSQL transaction_category_balances table with composite primary key, BigDecimal 
 * balance precision, timestamp tracking, and foreign key relationships for category-based 
 * balance management.
 * 
 * This entity maintains category-specific balance tracking per account as specified in Section 6.2.1.2,
 * supporting precise balance calculations by transaction category per Section 6.2.1.1 with 
 * concurrent balance updates protected by optimistic locking per Section 5.2.2.
 * 
 * Converted from COBOL copybook CVTRA01Y.cpy with composite key structure:
 * - TRAN-CAT-KEY composite key structure mapped to @EmbeddedId TransactionCategoryBalanceId
 * - TRANCAT-ACCT-ID PIC 9(11) → account_id VARCHAR(11) foreign key to accounts table
 * - TRANCAT-TYPE-CD PIC X(02) → transaction_type VARCHAR(2) foreign key to transaction_types table  
 * - TRANCAT-CD PIC 9(04) → transaction_category VARCHAR(4) foreign key to transaction_categories table
 * - TRAN-CAT-BAL PIC S9(09)V99 → category_balance DECIMAL(12,2) using BigDecimal precision
 * - Added last_updated TIMESTAMP for balance modification tracking
 * - Added version field for JPA optimistic locking support
 * 
 * Database Mapping:
 * - Table: transaction_category_balances with composite primary key constraint
 * - Precision: DECIMAL(12,2) for exact financial calculations maintaining COBOL COMP-3 equivalence
 * - Foreign Keys: account_id → accounts.account_id, transaction_category → transaction_categories.transaction_category,
 *   transaction_type → transaction_types.transaction_type
 * - Indexing: Composite B-tree index on primary key components for optimal query performance
 */
@Entity(name = "CommonTransactionCategoryBalance")
@Table(name = "transaction_category_balances", schema = "public")
public class TransactionCategoryBalance implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Composite primary key identifier combining account ID, transaction type, and transaction category.
     * Maps to COBOL TRAN-CAT-KEY structure with three-part composite key supporting
     * category-based balance tracking per account and transaction type classification.
     */
    @EmbeddedId
    @NotNull(message = "Transaction category balance ID cannot be null")
    private TransactionCategoryBalanceId id;

    /**
     * Category balance amount with exact decimal precision for financial calculations.
     * Maps to COBOL TRAN-CAT-BAL PIC S9(09)V99 with PostgreSQL DECIMAL(12,2) storage.
     * Original COBOL: 9 integer digits + 2 decimal places with sign support
     * PostgreSQL: DECIMAL(12,2) for 10 integer digits + 2 decimal places + sign
     * 
     * Using BigDecimal to maintain COBOL COMP-3 precision per Section 0.3.2 requirements.
     * Supports range -9999999999.99 to +9999999999.99 for comprehensive balance tracking.
     */
    @Column(name = "category_balance", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Category balance cannot be null")
    @DecimalMin(value = "-9999999999.99", message = "Category balance cannot be less than -9999999999.99")
    private BigDecimal categoryBalance;

    /**
     * Last updated timestamp for balance modification tracking.
     * Provides audit trail for balance changes supporting financial compliance requirements.
     * Uses LocalDateTime for precise timestamp representation and operations per Java 21 standards.
     */
    @Column(name = "last_updated", nullable = false)
    @NotNull(message = "Last updated timestamp cannot be null")
    private LocalDateTime lastUpdated;

    /**
     * Many-to-one relationship with Account entity for account reference.
     * Uses account_id from the composite primary key as the foreign key reference.
     * Enables navigation from balance to account details for comprehensive financial operations.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id", insertable = false, updatable = false)
    private Account account;

    /**
     * Many-to-one relationship with TransactionCategory entity for category reference.
     * Uses transaction_category from the composite primary key as the foreign key reference.
     * Enables category-based balance classification and reporting operations.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_category", referencedColumnName = "transaction_category", insertable = false, updatable = false)
    private TransactionCategory transactionCategory;

    /**
     * Many-to-one relationship with TransactionType entity for type reference.
     * Uses transaction_type from the composite primary key as the foreign key reference.
     * Enables type-based balance classification supporting debit/credit categorization.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_type", referencedColumnName = "transaction_type", insertable = false, updatable = false)
    private TransactionType transactionType;

    /**
     * Version field for optimistic locking support.
     * Enables concurrent balance update protection per Section 5.2.2 requirements.
     * Automatically managed by JPA for conflict resolution in multi-user environments.
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Default constructor required by JPA specification.
     * Initializes balance to zero with proper scale and sets last updated to current time.
     */
    public TransactionCategoryBalance() {
        this.categoryBalance = new BigDecimal("0.00");
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Constructor with composite primary key for entity initialization.
     * 
     * @param id The composite primary key containing account_id, transaction_type, and transaction_category
     */
    public TransactionCategoryBalance(TransactionCategoryBalanceId id) {
        this();
        this.id = id;
    }

    /**
     * Constructor with composite key and balance for comprehensive initialization.
     * Uses BigDecimal() constructor and setScale() methods per external import requirements.
     * 
     * @param id The composite primary key
     * @param categoryBalance The initial category balance amount
     */
    public TransactionCategoryBalance(TransactionCategoryBalanceId id, BigDecimal categoryBalance) {
        this.id = id;
        this.categoryBalance = categoryBalance != null ? 
            categoryBalance.setScale(2, java.math.RoundingMode.HALF_EVEN) : new BigDecimal("0.00");
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Full constructor with all key fields for complete entity initialization.
     * Uses LocalDateTime.of() method for timestamp specification as required by external imports.
     * 
     * @param id The composite primary key
     * @param categoryBalance The category balance amount
     * @param lastUpdated The last updated timestamp
     */
    public TransactionCategoryBalance(TransactionCategoryBalanceId id, BigDecimal categoryBalance, LocalDateTime lastUpdated) {
        this.id = id;
        this.categoryBalance = categoryBalance != null ? 
            categoryBalance.setScale(2, java.math.RoundingMode.HALF_EVEN) : new BigDecimal("0.00");
        this.lastUpdated = lastUpdated != null ? lastUpdated : LocalDateTime.now();
    }

    /**
     * Gets the composite primary key identifier.
     * 
     * @return The composite primary key containing account_id, transaction_type, and transaction_category
     */
    public TransactionCategoryBalanceId getId() {
        return id;
    }

    /**
     * Sets the composite primary key identifier.
     * 
     * @param id The composite primary key (not null)
     */
    public void setId(TransactionCategoryBalanceId id) {
        this.id = id;
    }

    /**
     * Gets the category balance with BigDecimal precision.
     * 
     * @return The category balance as BigDecimal with DECIMAL(12,2) precision
     */
    public BigDecimal getCategoryBalance() {
        return categoryBalance;
    }

    /**
     * Sets the category balance using BigDecimal for precise financial calculations.
     * Maintains COBOL COMP-3 precision equivalent per Section 0.3.2 requirements.
     * Uses setScale() method per external import requirements.
     * 
     * @param categoryBalance The category balance (-9999999999.99 to 9999999999.99)
     */
    public void setCategoryBalance(BigDecimal categoryBalance) {
        this.categoryBalance = categoryBalance != null ? 
            categoryBalance.setScale(2, java.math.RoundingMode.HALF_EVEN) : new BigDecimal("0.00");
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Gets the last updated timestamp.
     * 
     * @return The timestamp when the balance was last modified
     */
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Sets the last updated timestamp.
     * Uses LocalDateTime.of() method for timestamp specification as required by external imports.
     * 
     * @param lastUpdated The last updated timestamp
     */
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated != null ? lastUpdated : LocalDateTime.now();
    }

    /**
     * Gets the associated Account entity.
     * 
     * @return The Account entity for this balance record
     */
    public Account getAccount() {
        return account;
    }

    /**
     * Sets the associated Account entity.
     * Synchronizes the account_id in the composite primary key with the account entity.
     * Uses Account.getAccountId() method per internal import requirements.
     * 
     * @param account The Account entity
     */
    public void setAccount(Account account) {
        this.account = account;
        if (account != null && this.id != null) {
            this.id.setAccountId(account.getAccountId());
        }
    }

    /**
     * Gets the associated TransactionCategory entity.
     * 
     * @return The TransactionCategory entity for this balance record
     */
    public TransactionCategory getTransactionCategory() {
        return transactionCategory;
    }

    /**
     * Sets the associated TransactionCategory entity.
     * Synchronizes the transaction_category in the composite primary key with the category entity.
     * Uses TransactionCategory.getTransactionCategory() method per internal import requirements.
     * 
     * @param transactionCategory The TransactionCategory entity
     */
    public void setTransactionCategory(TransactionCategory transactionCategory) {
        this.transactionCategory = transactionCategory;
        if (transactionCategory != null && this.id != null) {
            this.id.setTransactionCategory(transactionCategory.getTransactionCategory());
        }
    }

    /**
     * Gets the associated TransactionType entity.
     * 
     * @return The TransactionType entity for this balance record
     */
    public TransactionType getTransactionType() {
        return transactionType;
    }

    /**
     * Sets the associated TransactionType entity.
     * Synchronizes the transaction_type in the composite primary key with the type entity.
     * Uses TransactionType.getTransactionType() method per internal import requirements.
     * 
     * @param transactionType The TransactionType entity
     */
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
        if (transactionType != null && this.id != null) {
            this.id.setTransactionType(transactionType.getTransactionType());
        }
    }

    /**
     * Gets the version for optimistic locking.
     * 
     * @return The version number for concurrent access control
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Sets the version for optimistic locking.
     * 
     * @param version The version number for conflict resolution
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Adds an amount to the category balance using BigDecimal precision.
     * Uses add() method per external import requirements.
     * Automatically updates the last_updated timestamp.
     * 
     * @param amount The amount to add to the category balance
     */
    public void addToCategoryBalance(BigDecimal amount) {
        if (amount != null && categoryBalance != null) {
            this.categoryBalance = categoryBalance.add(amount);
            this.lastUpdated = LocalDateTime.now();
        }
    }

    /**
     * Subtracts an amount from the category balance using BigDecimal precision.
     * Uses subtract() method per external import requirements.
     * Automatically updates the last_updated timestamp.
     * 
     * @param amount The amount to subtract from the category balance
     */
    public void subtractFromCategoryBalance(BigDecimal amount) {
        if (amount != null && categoryBalance != null) {
            this.categoryBalance = categoryBalance.subtract(amount);
            this.lastUpdated = LocalDateTime.now();
        }
    }

    /**
     * Checks if the category balance is positive.
     * 
     * @return true if the balance is greater than zero
     */
    public boolean hasPositiveBalance() {
        return categoryBalance != null && categoryBalance.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if the category balance is zero.
     * 
     * @return true if the balance equals zero
     */
    public boolean hasZeroBalance() {
        return categoryBalance != null && categoryBalance.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Checks if the category balance is negative.
     * 
     * @return true if the balance is less than zero
     */
    public boolean hasNegativeBalance() {
        return categoryBalance != null && categoryBalance.compareTo(BigDecimal.ZERO) < 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TransactionCategoryBalance that = (TransactionCategoryBalance) obj;
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
                ", categoryBalance=" + categoryBalance +
                ", lastUpdated=" + lastUpdated +
                ", version=" + version +
                '}';
    }

    /**
     * Composite primary key class for TransactionCategoryBalance entity.
     * Maps to COBOL TRAN-CAT-KEY structure with three-part composite key
     * supporting account-based category balance tracking per Section 6.2.1.2.
     * 
     * Composite Key Structure:
     * - account_id: VARCHAR(11) referencing accounts.account_id
     * - transaction_type: VARCHAR(2) referencing transaction_types.transaction_type  
     * - transaction_category: VARCHAR(4) referencing transaction_categories.transaction_category
     */
    @Embeddable
    public static class TransactionCategoryBalanceId implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Account identifier for foreign key relationship.
         * Maps to TRANCAT-ACCT-ID from COBOL copybook (PIC 9(11)).
         * References accounts.account_id for account-based balance tracking.
         */
        @Column(name = "account_id", length = 11, nullable = false)
        @NotNull(message = "Account ID cannot be null")
        private String accountId;

        /**
         * Transaction type code for classification.
         * Maps to TRANCAT-TYPE-CD from COBOL copybook (PIC X(02)).
         * References transaction_types.transaction_type for type-based categorization.
         */
        @Column(name = "transaction_type", length = 2, nullable = false)
        @NotNull(message = "Transaction type cannot be null")
        private String transactionType;

        /**
         * Transaction category code for balance classification.
         * Maps to TRANCAT-CD from COBOL copybook (PIC 9(04)).
         * References transaction_categories.transaction_category for category-based tracking.
         */
        @Column(name = "transaction_category", length = 4, nullable = false)
        @NotNull(message = "Transaction category cannot be null")
        private String transactionCategory;

        /**
         * Default constructor required by JPA specification.
         */
        public TransactionCategoryBalanceId() {
        }

        /**
         * Constructor with all composite key fields.
         * 
         * @param accountId The account identifier
         * @param transactionType The transaction type code
         * @param transactionCategory The transaction category code
         */
        public TransactionCategoryBalanceId(String accountId, String transactionType, String transactionCategory) {
            this.accountId = accountId;
            this.transactionType = transactionType;
            this.transactionCategory = transactionCategory;
        }

        /**
         * Gets the account identifier.
         * 
         * @return The account ID (11 digits)
         */
        public String getAccountId() {
            return accountId;
        }

        /**
         * Sets the account identifier.
         * 
         * @param accountId The account ID (11 digits, not null)
         */
        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        /**
         * Gets the transaction type code.
         * 
         * @return The transaction type code (2 characters)
         */
        public String getTransactionType() {
            return transactionType;
        }

        /**
         * Sets the transaction type code.
         * 
         * @param transactionType The transaction type code (2 characters, not null)
         */
        public void setTransactionType(String transactionType) {
            this.transactionType = transactionType;
        }

        /**
         * Gets the transaction category code.
         * 
         * @return The transaction category code (4 characters)
         */
        public String getTransactionCategory() {
            return transactionCategory;
        }

        /**
         * Sets the transaction category code.
         * 
         * @param transactionCategory The transaction category code (4 characters, not null)
         */
        public void setTransactionCategory(String transactionCategory) {
            this.transactionCategory = transactionCategory;
        }

        /**
         * Compares this composite key with another object for equality.
         * Essential for JPA entity identity operations and collection behavior.
         * Uses Objects.equals() method per external import requirements.
         * 
         * @param obj The object to compare with
         * @return true if the objects are equal based on all key components
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
         * Returns a hash code value for this composite key.
         * Uses Objects.hash() method per external import requirements.
         * Essential for proper HashMap/HashSet operations and JPA entity management.
         * 
         * @return hash code value based on all key components
         */
        @Override
        public int hashCode() {
            return Objects.hash(accountId, transactionType, transactionCategory);
        }

        @Override
        public String toString() {
            return "TransactionCategoryBalanceId{" +
                    "accountId='" + accountId + '\'' +
                    ", transactionType='" + transactionType + '\'' +
                    ", transactionCategory='" + transactionCategory + '\'' +
                    '}';
        }
    }
}