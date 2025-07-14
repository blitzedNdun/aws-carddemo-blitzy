package com.carddemo.account.entity;

import com.carddemo.common.entity.TransactionType;
import com.carddemo.common.entity.TransactionCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA Entity representing Transaction Category Balance data converted from COBOL TRAN-CAT-BAL-RECORD.
 * 
 * This entity maps the legacy VSAM transaction category balance structure to a modern PostgreSQL
 * table with composite primary key support, exact decimal precision, and foreign key relationships
 * for granular balance management and reporting operations per Section 6.2.1.2.
 * 
 * COBOL Structure Mapping (from CVTRA01Y.cpy):
 * - TRAN-CAT-KEY composite key:
 *   - TRANCAT-ACCT-ID PIC 9(11) → account_id VARCHAR(11) FK
 *   - TRANCAT-TYPE-CD PIC X(02) → transaction_type VARCHAR(2) FK
 *   - TRANCAT-CD PIC 9(04) → transaction_category VARCHAR(4) FK
 * - TRAN-CAT-BAL PIC S9(09)V99 → category_balance DECIMAL(12,2)
 * - Added: last_updated TIMESTAMP for audit tracking
 * 
 * The entity supports materialized view integration for account balance summary queries
 * and maintains BigDecimal precision equivalent to COBOL COMP-3 arithmetic for financial
 * accuracy per Section 0.1.2 data precision mandate.
 * 
 * Performance characteristics:
 * - Composite primary key enables efficient balance lookup operations
 * - Foreign key relationships maintain referential integrity
 * - Optimized for balance aggregation queries via materialized views
 * - Supports 10,000+ TPS transaction volume requirements
 * 
 * Converted from: app/cpy/CVTRA01Y.cpy (COBOL copybook)
 * Database Table: transaction_category_balances (PostgreSQL)
 * Record Length: 50 bytes (original COBOL layout)
 * 
 * @author Blitzy agent
 * @version CardDemo_v1.0-15-g27d6c6f-68
 * @since 2022-07-19
 */
@Entity
@Table(name = "transaction_category_balances", indexes = {
    @Index(name = "idx_account_category_balance", columnList = "account_id, transaction_category, category_balance"),
    @Index(name = "idx_transaction_type_balance", columnList = "transaction_type, category_balance"),
    @Index(name = "idx_last_updated", columnList = "last_updated")
})
public class TransactionCategoryBalance implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * MathContext for exact financial calculations matching COBOL COMP-3 precision.
     * Uses DECIMAL128 precision with HALF_EVEN rounding per Section 0.1.2 requirements.
     */
    public static final MathContext COBOL_MATH_CONTEXT = MathContext.DECIMAL128;

    /**
     * Composite primary key for transaction category balance
     * Maps to COBOL TRAN-CAT-KEY structure with account, type, and category identifiers
     */
    @EmbeddedId
    @NotNull(message = "Transaction category balance ID cannot be null")
    private TransactionCategoryBalanceId id;

    /**
     * Transaction category balance amount
     * Converted from: TRAN-CAT-BAL PIC S9(09)V99
     * Precision: DECIMAL(12,2) with exact financial arithmetic
     * 
     * Supports positive and negative balances for comprehensive balance tracking
     * across all transaction categories per account and transaction type.
     */
    @Column(name = "category_balance", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Category balance cannot be null")
    @DecimalMin(value = "-999999999.99", message = "Category balance cannot be less than minimum allowed")
    private BigDecimal categoryBalance;

    /**
     * Last updated timestamp for audit tracking
     * Added field for database schema compliance per Section 6.2.1.1
     * Automatically updated on balance modifications for audit trail
     */
    @Column(name = "last_updated", nullable = false)
    @NotNull(message = "Last updated timestamp cannot be null")
    private LocalDateTime lastUpdated;

    /**
     * Foreign key relationship to Account entity
     * Maintains referential integrity for account-based balance tracking
     * Supports lazy loading for optimal performance
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id", insertable = false, updatable = false)
    private Account account;

    /**
     * Foreign key relationship to TransactionType entity
     * Provides transaction type classification for balance tracking
     * Supports lazy loading for optimal performance
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_type", referencedColumnName = "transaction_type", insertable = false, updatable = false)
    private TransactionType transactionType;

    /**
     * Foreign key relationship to TransactionCategory entity
     * Provides transaction category classification for balance tracking
     * Supports lazy loading for optimal performance
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_category", referencedColumnName = "transaction_category", insertable = false, updatable = false)
    private TransactionCategory transactionCategory;

    /**
     * Default constructor required by JPA
     */
    public TransactionCategoryBalance() {
        this.categoryBalance = BigDecimal.ZERO;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Constructor with required fields
     * 
     * @param id composite primary key
     * @param categoryBalance balance amount
     */
    public TransactionCategoryBalance(TransactionCategoryBalanceId id, BigDecimal categoryBalance) {
        this.id = id;
        this.categoryBalance = categoryBalance != null ? categoryBalance : BigDecimal.ZERO;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Constructor with all fields
     * 
     * @param id composite primary key
     * @param categoryBalance balance amount
     * @param account associated account entity
     * @param transactionType associated transaction type entity
     * @param transactionCategory associated transaction category entity
     */
    public TransactionCategoryBalance(TransactionCategoryBalanceId id, BigDecimal categoryBalance,
                                    Account account, TransactionType transactionType, 
                                    TransactionCategory transactionCategory) {
        this(id, categoryBalance);
        this.account = account;
        this.transactionType = transactionType;
        this.transactionCategory = transactionCategory;
    }

    /**
     * Gets the composite primary key
     * 
     * @return transaction category balance ID
     */
    public TransactionCategoryBalanceId getId() {
        return id;
    }

    /**
     * Sets the composite primary key
     * 
     * @param id transaction category balance ID
     */
    public void setId(TransactionCategoryBalanceId id) {
        this.id = id;
    }

    /**
     * Gets the category balance amount
     * 
     * @return category balance
     */
    public BigDecimal getCategoryBalance() {
        return categoryBalance;
    }

    /**
     * Sets the category balance amount
     * Updates the last updated timestamp for audit tracking
     * 
     * @param categoryBalance category balance
     */
    public void setCategoryBalance(BigDecimal categoryBalance) {
        this.categoryBalance = categoryBalance;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Gets the last updated timestamp
     * 
     * @return last updated timestamp
     */
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Sets the last updated timestamp
     * 
     * @param lastUpdated last updated timestamp
     */
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * Gets the associated account entity
     * 
     * @return account entity
     */
    public Account getAccount() {
        return account;
    }

    /**
     * Sets the associated account entity
     * 
     * @param account account entity
     */
    public void setAccount(Account account) {
        this.account = account;
    }

    /**
     * Gets the associated transaction type entity
     * 
     * @return transaction type entity
     */
    public TransactionType getTransactionType() {
        return transactionType;
    }

    /**
     * Sets the associated transaction type entity
     * 
     * @param transactionType transaction type entity
     */
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * Gets the associated transaction category entity
     * 
     * @return transaction category entity
     */
    public TransactionCategory getTransactionCategory() {
        return transactionCategory;
    }

    /**
     * Sets the associated transaction category entity
     * 
     * @param transactionCategory transaction category entity
     */
    public void setTransactionCategory(TransactionCategory transactionCategory) {
        this.transactionCategory = transactionCategory;
    }

    /**
     * Adds the specified amount to the current balance
     * Uses COBOL math context for exact financial arithmetic
     * 
     * @param amount amount to add
     */
    public void addToBalance(BigDecimal amount) {
        if (amount != null) {
            this.categoryBalance = this.categoryBalance.add(amount, COBOL_MATH_CONTEXT);
            this.lastUpdated = LocalDateTime.now();
        }
    }

    /**
     * Subtracts the specified amount from the current balance
     * Uses COBOL math context for exact financial arithmetic
     * 
     * @param amount amount to subtract
     */
    public void subtractFromBalance(BigDecimal amount) {
        if (amount != null) {
            this.categoryBalance = this.categoryBalance.subtract(amount, COBOL_MATH_CONTEXT);
            this.lastUpdated = LocalDateTime.now();
        }
    }

    /**
     * Checks if the balance is positive
     * 
     * @return true if balance is greater than zero
     */
    public boolean isPositiveBalance() {
        return categoryBalance != null && categoryBalance.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if the balance is negative
     * 
     * @return true if balance is less than zero
     */
    public boolean isNegativeBalance() {
        return categoryBalance != null && categoryBalance.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Checks if the balance is zero
     * 
     * @return true if balance equals zero
     */
    public boolean isZeroBalance() {
        return categoryBalance != null && categoryBalance.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * JPA pre-persist callback to set timestamps
     */
    @PrePersist
    protected void onCreate() {
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * JPA pre-update callback to update timestamps
     */
    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Equality comparison based on composite primary key
     * Essential for JPA entity operations and collection handling
     * 
     * @param obj object to compare
     * @return true if objects are equal based on composite key
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
     * Hash code based on composite primary key
     * Consistent with equals() for proper hash-based collections behavior
     * 
     * @return hash code based on composite key
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * String representation for debugging and logging
     * Includes key fields for identification and balance information
     * 
     * @return string representation of the entity
     */
    @Override
    public String toString() {
        return String.format(
            "TransactionCategoryBalance{id=%s, categoryBalance=%s, lastUpdated=%s}",
            id, categoryBalance, lastUpdated
        );
    }

    /**
     * Embedded composite primary key class for TransactionCategoryBalance
     * 
     * Represents the composite key structure from COBOL TRAN-CAT-KEY definition
     * with account ID, transaction type, and transaction category components.
     * 
     * This class implements the composite key pattern required for JPA entities
     * with multiple primary key fields, maintaining exact correspondence with
     * the VSAM key structure per Section 6.2.1.2.
     */
    @Embeddable
    public static class TransactionCategoryBalanceId implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Account identifier component
         * Maps to COBOL TRANCAT-ACCT-ID PIC 9(11)
         */
        @Column(name = "account_id", length = 11, nullable = false)
        @NotNull(message = "Account ID cannot be null")
        @Size(min = 11, max = 11, message = "Account ID must be exactly 11 characters")
        private String accountId;

        /**
         * Transaction type component
         * Maps to COBOL TRANCAT-TYPE-CD PIC X(02)
         */
        @Column(name = "transaction_type", length = 2, nullable = false)
        @NotNull(message = "Transaction type cannot be null")
        @Size(min = 2, max = 2, message = "Transaction type must be exactly 2 characters")
        private String transactionType;

        /**
         * Transaction category component
         * Maps to COBOL TRANCAT-CD PIC 9(04)
         */
        @Column(name = "transaction_category", length = 4, nullable = false)
        @NotNull(message = "Transaction category cannot be null")
        @Size(min = 4, max = 4, message = "Transaction category must be exactly 4 characters")
        private String transactionCategory;

        /**
         * Default constructor required by JPA
         */
        public TransactionCategoryBalanceId() {
        }

        /**
         * Constructor with all fields
         * 
         * @param accountId account identifier
         * @param transactionType transaction type code
         * @param transactionCategory transaction category code
         */
        public TransactionCategoryBalanceId(String accountId, String transactionType, String transactionCategory) {
            this.accountId = accountId;
            this.transactionType = transactionType;
            this.transactionCategory = transactionCategory;
        }

        /**
         * Gets the account identifier
         * 
         * @return account ID
         */
        public String getAccountId() {
            return accountId;
        }

        /**
         * Sets the account identifier
         * 
         * @param accountId account ID
         */
        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        /**
         * Gets the transaction type
         * 
         * @return transaction type
         */
        public String getTransactionType() {
            return transactionType;
        }

        /**
         * Sets the transaction type
         * 
         * @param transactionType transaction type
         */
        public void setTransactionType(String transactionType) {
            this.transactionType = transactionType;
        }

        /**
         * Gets the transaction category
         * 
         * @return transaction category
         */
        public String getTransactionCategory() {
            return transactionCategory;
        }

        /**
         * Sets the transaction category
         * 
         * @param transactionCategory transaction category
         */
        public void setTransactionCategory(String transactionCategory) {
            this.transactionCategory = transactionCategory;
        }

        /**
         * Equality comparison for composite key
         * Required for JPA composite key functionality
         * 
         * @param obj object to compare
         * @return true if all key components are equal
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
         * Hash code for composite key
         * Required for JPA composite key functionality
         * 
         * @return hash code based on all key components
         */
        @Override
        public int hashCode() {
            return Objects.hash(accountId, transactionType, transactionCategory);
        }

        /**
         * String representation for debugging
         * 
         * @return string representation of the composite key
         */
        @Override
        public String toString() {
            return String.format(
                "TransactionCategoryBalanceId{accountId='%s', transactionType='%s', transactionCategory='%s'}",
                accountId, transactionType, transactionCategory
            );
        }
    }
}