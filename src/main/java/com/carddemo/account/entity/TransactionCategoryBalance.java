package com.carddemo.account.entity;

import com.carddemo.common.entity.TransactionType;
import com.carddemo.common.entity.TransactionCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * TransactionCategoryBalance JPA entity representing account balances by transaction category
 * converted from COBOL TRAN-CAT-BAL-RECORD structure with BigDecimal precision.
 * 
 * This entity maintains granular balance tracking per account and transaction category,
 * supporting materialized view integration for account balance summary queries and
 * efficient balance lookup operations through composite primary key structure.
 * 
 * Original COBOL structure from CVTRA01Y.cpy:
 * - TRAN-CAT-KEY (composite key): TRANCAT-ACCT-ID, TRANCAT-TYPE-CD, TRANCAT-CD
 * - TRAN-CAT-BAL (PIC S9(09)V99): Balance amount with exact decimal precision
 * 
 * Database table: transaction_category_balances
 * Primary key: Composite (account_id, transaction_type, transaction_category)
 * 
 * Key Features:
 * - Composite primary key ensuring unique balance per account/type/category combination
 * - BigDecimal balance field with DECIMAL(12,2) precision for exact financial calculations
 * - Foreign key relationships to Account, TransactionType, and TransactionCategory entities
 * - Audit timestamp tracking for balance modification history
 * - Support for materialized view integration per Section 6.2.6.4
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "transaction_category_balances")
public class TransactionCategoryBalance implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Composite primary key containing account_id, transaction_type, and transaction_category.
     * Implements @EmbeddedId pattern for multi-column primary key structure
     * matching VSAM TRAN-CAT-KEY definition per Section 6.2.1.2.
     */
    @EmbeddedId
    @NotNull(message = "Transaction category balance ID is required")
    private TransactionCategoryBalanceId id;

    /**
     * Category balance amount with exact BigDecimal precision.
     * Maps from COBOL TRAN-CAT-BAL PIC S9(09)V99 to PostgreSQL DECIMAL(12,2).
     * 
     * Precision: 12 total digits with 2 decimal places
     * Range: -9999999999.99 to 9999999999.99
     * Uses BigDecimal for exact financial arithmetic operations
     */
    @Column(name = "category_balance", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Category balance is required")
    @DecimalMin(value = "-9999999999.99", message = "Category balance must be greater than -9999999999.99")
    private BigDecimal categoryBalance;

    /**
     * Last updated timestamp for audit tracking.
     * Automatically managed for balance modification history
     * per database schema Section 6.2.1.1.
     */
    @Column(name = "last_updated", nullable = false)
    @NotNull(message = "Last updated timestamp is required")
    private LocalDateTime lastUpdated;

    /**
     * Many-to-one relationship to Account entity.
     * Foreign key: account_id references accounts.account_id
     * Enables efficient navigation from balance to account details.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private Account account;

    /**
     * Many-to-one relationship to TransactionType entity.
     * Foreign key: transaction_type references transaction_types.transaction_type
     * Provides transaction type classification for balance categorization.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_type", insertable = false, updatable = false)
    private TransactionType transactionType;

    /**
     * Many-to-one relationship to TransactionCategory entity.
     * Foreign key: transaction_category references transaction_categories.transaction_category
     * Enables category-based balance aggregation and reporting.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_category", insertable = false, updatable = false)
    private TransactionCategory transactionCategory;

    /**
     * Default constructor for JPA entity instantiation.
     * Initializes last updated timestamp to current time.
     */
    public TransactionCategoryBalance() {
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Constructor with composite ID and balance amount.
     * 
     * @param id Composite primary key
     * @param categoryBalance Balance amount for the category
     */
    public TransactionCategoryBalance(TransactionCategoryBalanceId id, BigDecimal categoryBalance) {
        this.id = id;
        this.categoryBalance = categoryBalance;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Constructor with all required fields.
     * 
     * @param id Composite primary key
     * @param categoryBalance Balance amount for the category
     * @param lastUpdated Last modification timestamp
     */
    public TransactionCategoryBalance(TransactionCategoryBalanceId id, BigDecimal categoryBalance, LocalDateTime lastUpdated) {
        this.id = id;
        this.categoryBalance = categoryBalance;
        this.lastUpdated = lastUpdated;
    }

    /**
     * Gets the composite primary key.
     * 
     * @return Transaction category balance ID
     */
    public TransactionCategoryBalanceId getId() {
        return id;
    }

    /**
     * Sets the composite primary key.
     * 
     * @param id Transaction category balance ID
     */
    public void setId(TransactionCategoryBalanceId id) {
        this.id = id;
    }

    /**
     * Gets the category balance amount.
     * 
     * @return Category balance with exact decimal precision
     */
    public BigDecimal getCategoryBalance() {
        return categoryBalance;
    }

    /**
     * Sets the category balance amount.
     * Updates the last modified timestamp automatically.
     * 
     * @param categoryBalance Category balance amount
     */
    public void setCategoryBalance(BigDecimal categoryBalance) {
        this.categoryBalance = categoryBalance;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Gets the last updated timestamp.
     * 
     * @return Last modification timestamp
     */
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Sets the last updated timestamp.
     * 
     * @param lastUpdated Last modification timestamp
     */
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * Gets the associated Account entity.
     * 
     * @return Account entity reference
     */
    public Account getAccount() {
        return account;
    }

    /**
     * Sets the associated Account entity.
     * 
     * @param account Account entity reference
     */
    public void setAccount(Account account) {
        this.account = account;
    }

    /**
     * Gets the associated TransactionType entity.
     * 
     * @return TransactionType entity reference
     */
    public TransactionType getTransactionType() {
        return transactionType;
    }

    /**
     * Sets the associated TransactionType entity.
     * 
     * @param transactionType TransactionType entity reference
     */
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * Gets the associated TransactionCategory entity.
     * 
     * @return TransactionCategory entity reference
     */
    public TransactionCategory getTransactionCategory() {
        return transactionCategory;
    }

    /**
     * Sets the associated TransactionCategory entity.
     * 
     * @param transactionCategory TransactionCategory entity reference
     */
    public void setTransactionCategory(TransactionCategory transactionCategory) {
        this.transactionCategory = transactionCategory;
    }

    /**
     * Updates the last modified timestamp to current time.
     * Convenience method for audit tracking.
     */
    public void updateLastModified() {
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Checks if the balance is positive.
     * 
     * @return true if balance is greater than zero
     */
    public boolean isPositiveBalance() {
        return categoryBalance != null && categoryBalance.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if the balance is negative.
     * 
     * @return true if balance is less than zero
     */
    public boolean isNegativeBalance() {
        return categoryBalance != null && categoryBalance.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Checks if the balance is zero.
     * 
     * @return true if balance equals zero
     */
    public boolean isZeroBalance() {
        return categoryBalance != null && categoryBalance.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Adds an amount to the current balance.
     * 
     * @param amount Amount to add
     * @return New balance after addition
     */
    public BigDecimal addToBalance(BigDecimal amount) {
        if (amount != null && categoryBalance != null) {
            this.categoryBalance = this.categoryBalance.add(amount);
            this.lastUpdated = LocalDateTime.now();
        }
        return this.categoryBalance;
    }

    /**
     * Subtracts an amount from the current balance.
     * 
     * @param amount Amount to subtract
     * @return New balance after subtraction
     */
    public BigDecimal subtractFromBalance(BigDecimal amount) {
        if (amount != null && categoryBalance != null) {
            this.categoryBalance = this.categoryBalance.subtract(amount);
            this.lastUpdated = LocalDateTime.now();
        }
        return this.categoryBalance;
    }

    /**
     * Compares this TransactionCategoryBalance with another object for equality.
     * Two TransactionCategoryBalance objects are equal if they have the same composite ID.
     * 
     * @param obj Object to compare with
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TransactionCategoryBalance that = (TransactionCategoryBalance) obj;
        return Objects.equals(id, that.id);
    }

    /**
     * Returns hash code for this TransactionCategoryBalance.
     * Hash code is based on the composite primary key.
     * 
     * @return Hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Returns string representation of this TransactionCategoryBalance.
     * 
     * @return String representation with key fields
     */
    @Override
    public String toString() {
        return "TransactionCategoryBalance{" +
                "id=" + id +
                ", categoryBalance=" + categoryBalance +
                ", lastUpdated=" + lastUpdated +
                '}';
    }

    /**
     * Composite primary key class for TransactionCategoryBalance entity.
     * Implements @Embeddable pattern for multi-column primary key structure.
     * 
     * Contains account_id, transaction_type, and transaction_category fields
     * matching VSAM TRAN-CAT-KEY definition.
     */
    @Embeddable
    public static class TransactionCategoryBalanceId implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Account identifier - 11-digit account ID.
         * Maps to COBOL TRANCAT-ACCT-ID PIC 9(11).
         */
        @Column(name = "account_id", length = 11, nullable = false)
        @NotNull(message = "Account ID is required")
        private String accountId;

        /**
         * Transaction type code - 2-character type classification.
         * Maps to COBOL TRANCAT-TYPE-CD PIC X(02).
         */
        @Column(name = "transaction_type", length = 2, nullable = false)
        @NotNull(message = "Transaction type is required")
        private String transactionType;

        /**
         * Transaction category code - 4-character category classification.
         * Maps to COBOL TRANCAT-CD PIC 9(04).
         */
        @Column(name = "transaction_category", length = 4, nullable = false)
        @NotNull(message = "Transaction category is required")
        private String transactionCategory;

        /**
         * Default constructor for JPA.
         */
        public TransactionCategoryBalanceId() {
            // Default constructor required by JPA
        }

        /**
         * Constructor with all required fields.
         * 
         * @param accountId Account identifier
         * @param transactionType Transaction type code
         * @param transactionCategory Transaction category code
         */
        public TransactionCategoryBalanceId(String accountId, String transactionType, String transactionCategory) {
            this.accountId = accountId;
            this.transactionType = transactionType;
            this.transactionCategory = transactionCategory;
        }

        /**
         * Gets the account identifier.
         * 
         * @return Account ID
         */
        public String getAccountId() {
            return accountId;
        }

        /**
         * Sets the account identifier.
         * 
         * @param accountId Account ID
         */
        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        /**
         * Gets the transaction type code.
         * 
         * @return Transaction type code
         */
        public String getTransactionType() {
            return transactionType;
        }

        /**
         * Sets the transaction type code.
         * 
         * @param transactionType Transaction type code
         */
        public void setTransactionType(String transactionType) {
            this.transactionType = transactionType;
        }

        /**
         * Gets the transaction category code.
         * 
         * @return Transaction category code
         */
        public String getTransactionCategory() {
            return transactionCategory;
        }

        /**
         * Sets the transaction category code.
         * 
         * @param transactionCategory Transaction category code
         */
        public void setTransactionCategory(String transactionCategory) {
            this.transactionCategory = transactionCategory;
        }

        /**
         * Compares this TransactionCategoryBalanceId with another object for equality.
         * Two IDs are equal if all their fields match.
         * 
         * @param obj Object to compare with
         * @return true if objects are equal, false otherwise
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            TransactionCategoryBalanceId that = (TransactionCategoryBalanceId) obj;
            return Objects.equals(accountId, that.accountId) &&
                   Objects.equals(transactionType, that.transactionType) &&
                   Objects.equals(transactionCategory, that.transactionCategory);
        }

        /**
         * Returns hash code for this TransactionCategoryBalanceId.
         * Hash code is based on all key fields.
         * 
         * @return Hash code value
         */
        @Override
        public int hashCode() {
            return Objects.hash(accountId, transactionType, transactionCategory);
        }

        /**
         * Returns string representation of this TransactionCategoryBalanceId.
         * 
         * @return String representation with all key fields
         */
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