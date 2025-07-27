package com.carddemo.account.entity;

import com.carddemo.account.entity.Account;
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
 * JPA Entity representing transaction category balance data converted from COBOL TRAN-CAT-BAL-RECORD.
 * Maintains composite primary key structure matching VSAM TRAN-CAT-KEY definition and exact BigDecimal
 * precision equivalent to COBOL COMP-3 arithmetic for financial accuracy per Section 6.2.1.2.
 * 
 * Original COBOL Structure: TRAN-CAT-BAL-RECORD from CVTRA01Y.cpy (RECLN = 50)
 * Target Table: transaction_category_balances
 * 
 * Key Features:
 * - Composite primary key with account_id, transaction_type, and transaction_category
 * - BigDecimal balance field with DECIMAL(12,2) precision for COBOL COMP-3 equivalence
 * - Foreign key relationships to Account, TransactionType, and TransactionCategory entities
 * - Audit timestamp tracking for balance modification history
 * - Support for materialized view integration per Section 6.2.6.4
 * - Jakarta Bean Validation for financial data integrity
 * 
 * Performance Requirements:
 * - Efficient balance lookup operations per account and transaction category
 * - Support for PostgreSQL composite index strategies on key components
 * - Memory usage optimization through lazy loading of relationships
 * - Transaction isolation level SERIALIZABLE for concurrent balance updates
 * 
 * Business Rules Enforced:
 * - Balance field maintains exact decimal precision equivalent to COBOL PIC S9(09)V99
 * - Composite key ensures unique balance per account-type-category combination
 * - Foreign key constraints maintain referential integrity with related entities
 * - Audit timestamp automatically tracks last modification for balance changes
 * 
 * @author Blitzy Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "transaction_category_balances",
       indexes = {
           @Index(name = "idx_tran_cat_bal_account", columnList = "account_id"),
           @Index(name = "idx_tran_cat_bal_type", columnList = "transaction_type"),
           @Index(name = "idx_tran_cat_bal_category", columnList = "transaction_category"),
           @Index(name = "idx_tran_cat_bal_updated", columnList = "last_updated")
       })
public class TransactionCategoryBalance implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * MathContext for financial calculations maintaining COBOL COMP-3 precision
     * Uses DECIMAL128 precision with HALF_EVEN rounding to ensure exact financial arithmetic
     */
    public static final MathContext COBOL_DECIMAL_CONTEXT = MathContext.DECIMAL128;

    /**
     * Composite primary key embedding account_id, transaction_type, and transaction_category
     * Maps to COBOL TRAN-CAT-KEY structure for exact functional equivalence
     */
    @EmbeddedId
    @NotNull(message = "Transaction category balance ID is required")
    private TransactionCategoryBalanceId id;

    /**
     * Transaction category balance amount
     * Mapped from COBOL: TRAN-CAT-BAL PIC S9(09)V99
     * Uses BigDecimal with DECIMAL(12,2) precision for exact financial calculations
     */
    @Column(name = "category_balance", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Category balance is required")
    @DecimalMin(value = "-999999999.99", message = "Category balance cannot be less than -999999999.99")
    private BigDecimal categoryBalance;

    /**
     * Last updated timestamp for audit tracking
     * Enables balance modification history per database schema Section 6.2.1.1
     */
    @Column(name = "last_updated", nullable = false)
    @NotNull(message = "Last updated timestamp is required")
    private LocalDateTime lastUpdated;

    /**
     * Many-to-one relationship to Account entity via account_id foreign key
     * Provides navigation from balance to account for reporting and validation
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_tran_cat_bal_account"))
    private Account account;

    /**
     * Many-to-one relationship to TransactionType entity via transaction_type foreign key
     * Enables transaction type classification and validation for balance tracking
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_type", nullable = false, insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_tran_cat_bal_trans_type"))
    private TransactionType transactionType;

    /**
     * Many-to-one relationship to TransactionCategory entity via transaction_category foreign key
     * Provides transaction category classification for granular balance tracking and reporting
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_category", nullable = false, insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_tran_cat_bal_trans_category"))
    private TransactionCategory transactionCategory;

    // Constructors

    /**
     * Default constructor required by JPA specification.
     * Initializes entity with current timestamp for audit tracking.
     */
    public TransactionCategoryBalance() {
        this.lastUpdated = LocalDateTime.now();
        this.categoryBalance = BigDecimal.ZERO;
    }

    /**
     * Constructor with composite key for entity creation.
     * 
     * @param id composite primary key containing account_id, transaction_type, and transaction_category
     */
    public TransactionCategoryBalance(TransactionCategoryBalanceId id) {
        this();
        this.id = id;
    }

    /**
     * Constructor with composite key and balance amount.
     * 
     * @param id composite primary key
     * @param categoryBalance balance amount for the category
     */
    public TransactionCategoryBalance(TransactionCategoryBalanceId id, BigDecimal categoryBalance) {
        this(id);
        this.categoryBalance = categoryBalance != null ? categoryBalance : BigDecimal.ZERO;
    }

    /**
     * Full constructor with all fields for complete entity initialization.
     * 
     * @param id composite primary key
     * @param categoryBalance balance amount for the category
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

    // Getter and Setter Methods

    /**
     * Gets the composite primary key.
     * 
     * @return TransactionCategoryBalanceId containing account_id, transaction_type, and transaction_category
     */
    public TransactionCategoryBalanceId getId() {
        return id;
    }

    /**
     * Sets the composite primary key.
     * Updates last modified timestamp automatically.
     * 
     * @param id composite primary key
     */
    public void setId(TransactionCategoryBalanceId id) {
        this.id = id;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Gets the transaction category balance amount.
     * 
     * @return category balance as BigDecimal with exact precision
     */
    public BigDecimal getCategoryBalance() {
        return categoryBalance;
    }

    /**
     * Sets the transaction category balance amount.
     * Updates last modified timestamp automatically for audit tracking.
     * 
     * @param categoryBalance balance amount with COBOL COMP-3 precision
     */
    public void setCategoryBalance(BigDecimal categoryBalance) {
        this.categoryBalance = categoryBalance != null ? categoryBalance : BigDecimal.ZERO;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Gets the last updated timestamp.
     * 
     * @return timestamp of last modification
     */
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Sets the last updated timestamp.
     * 
     * @param lastUpdated timestamp of last modification
     */
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated != null ? lastUpdated : LocalDateTime.now();
    }

    /**
     * Gets the associated account entity.
     * 
     * @return Account entity for balance tracking
     */
    public Account getAccount() {
        return account;
    }

    /**
     * Sets the associated account entity.
     * 
     * @param account Account entity for balance tracking
     */
    public void setAccount(Account account) {
        this.account = account;
    }

    /**
     * Gets the associated transaction type entity.
     * 
     * @return TransactionType entity for classification
     */
    public TransactionType getTransactionType() {
        return transactionType;
    }

    /**
     * Sets the associated transaction type entity.
     * 
     * @param transactionType TransactionType entity for classification
     */
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * Gets the associated transaction category entity.
     * 
     * @return TransactionCategory entity for granular classification
     */
    public TransactionCategory getTransactionCategory() {
        return transactionCategory;
    }

    /**
     * Sets the associated transaction category entity.
     * 
     * @param transactionCategory TransactionCategory entity for granular classification
     */
    public void setTransactionCategory(TransactionCategory transactionCategory) {
        this.transactionCategory = transactionCategory;
    }

    // Business Logic Methods

    /**
     * Updates the balance by adding the specified amount using COBOL-equivalent decimal arithmetic.
     * Maintains exact precision for financial calculations and updates audit timestamp.
     * 
     * @param amount amount to add to the current balance (can be negative for subtractions)
     * @return updated balance amount
     */
    public BigDecimal updateBalance(BigDecimal amount) {
        if (amount != null) {
            this.categoryBalance = this.categoryBalance.add(amount, COBOL_DECIMAL_CONTEXT);
            this.lastUpdated = LocalDateTime.now();
        }
        return this.categoryBalance;
    }

    /**
     * Checks if the balance is positive (credit balance).
     * 
     * @return true if balance is greater than zero
     */
    public boolean hasPositiveBalance() {
        return categoryBalance != null && categoryBalance.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if the balance is negative (debit balance).
     * 
     * @return true if balance is less than zero
     */
    public boolean hasNegativeBalance() {
        return categoryBalance != null && categoryBalance.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Checks if the balance is zero.
     * 
     * @return true if balance equals zero
     */
    public boolean hasZeroBalance() {
        return categoryBalance != null && categoryBalance.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Gets the absolute value of the balance for reporting purposes.
     * 
     * @return absolute balance amount
     */
    public BigDecimal getAbsoluteBalance() {
        return categoryBalance != null ? categoryBalance.abs(COBOL_DECIMAL_CONTEXT) : BigDecimal.ZERO;
    }

    // Standard Object Methods

    /**
     * Compares this TransactionCategoryBalance with another object for equality.
     * Two TransactionCategoryBalance entities are considered equal if they have the same
     * composite primary key (id).
     * 
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TransactionCategoryBalance that = (TransactionCategoryBalance) obj;
        return Objects.equals(id, that.id);
    }

    /**
     * Returns a hash code value for this TransactionCategoryBalance entity.
     * The hash code is based on the composite primary key (id) to ensure
     * consistency with the equals() method.
     * 
     * @return hash code value for this entity
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Returns a string representation of this TransactionCategoryBalance entity.
     * Includes key fields for debugging and logging purposes.
     * 
     * @return string representation of the entity
     */
    @Override
    public String toString() {
        return String.format("TransactionCategoryBalance{" +
                "id=%s, " +
                "categoryBalance=%s, " +
                "lastUpdated=%s" +
                "}", 
                id, 
                categoryBalance, 
                lastUpdated);
    }

    // Embedded Composite Primary Key Class

    /**
     * Embeddable composite primary key class for TransactionCategoryBalance entity.
     * Maps to COBOL TRAN-CAT-KEY structure with account_id, transaction_type, and transaction_category.
     * 
     * Implements Serializable for proper JPA composite key handling and provides
     * equals/hashCode methods for correct entity identity operations.
     */
    @Embeddable
    public static class TransactionCategoryBalanceId implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Account ID component of composite key
         * Maps to COBOL: TRANCAT-ACCT-ID PIC 9(11)
         */
        @Column(name = "account_id", length = 11, nullable = false)
        @NotNull(message = "Account ID is required")
        @Size(min = 11, max = 11, message = "Account ID must be exactly 11 characters")
        private String accountId;

        /**
         * Transaction type component of composite key
         * Maps to COBOL: TRANCAT-TYPE-CD PIC X(02)
         */
        @Column(name = "transaction_type", length = 2, nullable = false)
        @NotNull(message = "Transaction type is required")
        @Size(min = 2, max = 2, message = "Transaction type must be exactly 2 characters")
        private String transactionType;

        /**
         * Transaction category component of composite key
         * Maps to COBOL: TRANCAT-CD PIC 9(04)
         */
        @Column(name = "transaction_category", length = 4, nullable = false)
        @NotNull(message = "Transaction category is required")
        @Size(min = 4, max = 4, message = "Transaction category must be exactly 4 characters")
        private String transactionCategory;

        // Constructors

        /**
         * Default constructor required by JPA specification.
         */
        public TransactionCategoryBalanceId() {
        }

        /**
         * Constructor with all composite key components.
         * 
         * @param accountId 11-character account identifier
         * @param transactionType 2-character transaction type code
         * @param transactionCategory 4-character transaction category code
         */
        public TransactionCategoryBalanceId(String accountId, String transactionType, String transactionCategory) {
            this.accountId = accountId;
            this.transactionType = transactionType;
            this.transactionCategory = transactionCategory;
        }

        // Getter and Setter Methods

        /**
         * Gets the account ID component.
         * 
         * @return 11-character account identifier
         */
        public String getAccountId() {
            return accountId;
        }

        /**
         * Sets the account ID component.
         * 
         * @param accountId 11-character account identifier
         */
        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        /**
         * Gets the transaction type component.
         * 
         * @return 2-character transaction type code
         */
        public String getTransactionType() {
            return transactionType;
        }

        /**
         * Sets the transaction type component.
         * 
         * @param transactionType 2-character transaction type code
         */
        public void setTransactionType(String transactionType) {
            this.transactionType = transactionType;
        }

        /**
         * Gets the transaction category component.
         * 
         * @return 4-character transaction category code
         */
        public String getTransactionCategory() {
            return transactionCategory;
        }

        /**
         * Sets the transaction category component.
         * 
         * @param transactionCategory 4-character transaction category code
         */
        public void setTransactionCategory(String transactionCategory) {
            this.transactionCategory = transactionCategory;
        }

        // Standard Object Methods

        /**
         * Compares this composite key with another object for equality.
         * Two composite keys are equal if all components match exactly.
         * 
         * @param obj the object to compare with
         * @return true if the objects are equal, false otherwise
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
         * Hash code is based on all key components for proper collection behavior.
         * 
         * @return hash code value for this composite key
         */
        @Override
        public int hashCode() {
            return Objects.hash(accountId, transactionType, transactionCategory);
        }

        /**
         * Returns a string representation of this composite key.
         * 
         * @return string representation showing all key components
         */
        @Override
        public String toString() {
            return String.format("TransactionCategoryBalanceId{" +
                    "accountId='%s', " +
                    "transactionType='%s', " +
                    "transactionCategory='%s'" +
                    "}", 
                    accountId, 
                    transactionType, 
                    transactionCategory);
        }
    }
}