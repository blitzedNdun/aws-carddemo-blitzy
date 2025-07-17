package com.carddemo.common.entity;

import com.carddemo.common.entity.Account;
import com.carddemo.common.entity.TransactionCategory;
import com.carddemo.common.entity.TransactionType;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA TransactionCategoryBalance entity mapping COBOL transaction category balance 
 * structure (CVTRA01Y.cpy) to PostgreSQL transaction_category_balances table with 
 * composite primary key, BigDecimal balance precision, timestamp tracking, and 
 * foreign key relationships for category-based balance management.
 * 
 * This entity supports:
 * - Composite primary key structure per Section 6.2.1.2
 * - Precise balance calculations by transaction category per Section 6.2.1.1
 * - Concurrent balance updates with optimistic locking per Section 5.2.2
 * - COBOL COMP-3 decimal precision using BigDecimal per Section 0.3.2
 * - Sub-200ms response times for balance operations per Section 2.1.3
 * 
 * Original COBOL structure: TRAN-CAT-BAL-RECORD in CVTRA01Y.cpy (50 bytes)
 * PostgreSQL table: transaction_category_balances
 * 
 * Composite Key Structure:
 * - TRANCAT-ACCT-ID PIC 9(11) → account_id VARCHAR(11)
 * - TRANCAT-TYPE-CD PIC X(02) → transaction_type VARCHAR(2)
 * - TRANCAT-CD PIC 9(04) → transaction_category VARCHAR(4)
 * 
 * Balance Field Mapping:
 * - TRAN-CAT-BAL PIC S9(09)V99 → category_balance DECIMAL(12,2)
 * - Added last_updated TIMESTAMP for balance modification tracking
 * - Added version BIGINT for optimistic locking protection
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
     * MathContext for BigDecimal operations maintaining COBOL COMP-3 precision
     * Uses DECIMAL128 precision with HALF_UP rounding mode per Section 0.3.2
     */
    public static final MathContext COBOL_MATH_CONTEXT = new MathContext(31, RoundingMode.HALF_UP);

    /**
     * Default scale for financial calculations (2 decimal places)
     */
    public static final int FINANCIAL_SCALE = 2;

    /**
     * Composite primary key containing account_id, transaction_type, and transaction_category
     * Maps to TRAN-CAT-KEY structure from COBOL copybook CVTRA01Y.cpy
     */
    @EmbeddedId
    private TransactionCategoryBalanceId id;

    /**
     * Category balance amount - Maps TRAN-CAT-BAL PIC S9(09)V99
     * Maps to PostgreSQL DECIMAL(12,2) using BigDecimal for exact precision
     * Maintains COBOL COMP-3 decimal precision with DECIMAL128 context
     * Range: -9999999999.99 to 9999999999.99
     */
    @Column(name = "category_balance", precision = 12, scale = 2, nullable = false)
    private BigDecimal categoryBalance;

    /**
     * Last updated timestamp for balance modification tracking
     * Maps to PostgreSQL TIMESTAMP type for balance change auditing
     * Automatically updated on balance modifications
     */
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    /**
     * Many-to-one relationship with Account entity
     * Foreign key relationship via account_id component of composite key
     * Enables account-based balance management and reporting
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id", insertable = false, updatable = false)
    private Account account;

    /**
     * Many-to-one relationship with TransactionCategory entity
     * Foreign key relationship via transaction_category component of composite key
     * Enables category-based balance tracking and validation
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_category", referencedColumnName = "transaction_category", insertable = false, updatable = false)
    private TransactionCategory transactionCategory;

    /**
     * Many-to-one relationship with TransactionType entity
     * Foreign key relationship via transaction_type component of composite key
     * Enables type-based categorization and balance classification
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_type", referencedColumnName = "transaction_type", insertable = false, updatable = false)
    private TransactionType transactionType;

    /**
     * Optimistic locking version for concurrent balance update protection
     * Enables category balance management with concurrent access control
     * Prevents lost updates in multi-user scenarios
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Default constructor for JPA
     */
    public TransactionCategoryBalance() {
        // Initialize balance with zero value using proper scale
        this.categoryBalance = BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
        this.lastUpdated = LocalDateTime.now();
        this.version = 0L;
    }

    /**
     * Constructor with composite key and balance
     * 
     * @param id Composite primary key containing account_id, transaction_type, and transaction_category
     * @param categoryBalance Category balance with BigDecimal precision
     */
    public TransactionCategoryBalance(TransactionCategoryBalanceId id, BigDecimal categoryBalance) {
        this();
        this.id = id;
        this.categoryBalance = categoryBalance != null ? categoryBalance.setScale(FINANCIAL_SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
    }

    /**
     * Constructor with individual key components and balance
     * 
     * @param accountId Account identifier (11 digits)
     * @param transactionType Transaction type code (2 characters)
     * @param transactionCategory Transaction category code (4 characters)
     * @param categoryBalance Category balance with BigDecimal precision
     */
    public TransactionCategoryBalance(String accountId, String transactionType, String transactionCategory, BigDecimal categoryBalance) {
        this();
        this.id = new TransactionCategoryBalanceId(accountId, transactionType, transactionCategory);
        this.categoryBalance = categoryBalance != null ? categoryBalance.setScale(FINANCIAL_SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
    }

    /**
     * Gets the composite primary key
     * 
     * @return Composite key containing account_id, transaction_type, and transaction_category
     */
    public TransactionCategoryBalanceId getId() {
        return id;
    }

    /**
     * Sets the composite primary key
     * 
     * @param id Composite key containing account_id, transaction_type, and transaction_category
     */
    public void setId(TransactionCategoryBalanceId id) {
        this.id = id;
    }

    /**
     * Gets the category balance with BigDecimal precision
     * Maintains COBOL COMP-3 precision for financial calculations
     * 
     * @return Category balance as BigDecimal with DECIMAL(12,2) precision
     */
    public BigDecimal getCategoryBalance() {
        return categoryBalance;
    }

    /**
     * Sets the category balance with BigDecimal precision
     * Maintains COBOL COMP-3 precision for financial calculations
     * Automatically updates last_updated timestamp
     * 
     * @param categoryBalance Category balance as BigDecimal
     */
    public void setCategoryBalance(BigDecimal categoryBalance) {
        this.categoryBalance = categoryBalance != null ? categoryBalance.setScale(FINANCIAL_SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Gets the last updated timestamp
     * 
     * @return Last updated timestamp as LocalDateTime
     */
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Sets the last updated timestamp
     * 
     * @param lastUpdated Last updated timestamp as LocalDateTime
     */
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * Gets the associated account entity
     * Provides access to account information for balance management
     * 
     * @return Account entity for balance relationship
     */
    public Account getAccount() {
        return account;
    }

    /**
     * Sets the associated account entity
     * Links balance to account for management and reporting
     * 
     * @param account Account entity
     */
    public void setAccount(Account account) {
        this.account = account;
    }

    /**
     * Gets the associated transaction category entity
     * Provides access to category information for balance classification
     * 
     * @return TransactionCategory entity for balance categorization
     */
    public TransactionCategory getTransactionCategory() {
        return transactionCategory;
    }

    /**
     * Sets the associated transaction category entity
     * Links balance to category for classification and validation
     * 
     * @param transactionCategory TransactionCategory entity
     */
    public void setTransactionCategory(TransactionCategory transactionCategory) {
        this.transactionCategory = transactionCategory;
    }

    /**
     * Gets the associated transaction type entity
     * Provides access to type information for balance classification
     * 
     * @return TransactionType entity for balance type classification
     */
    public TransactionType getTransactionType() {
        return transactionType;
    }

    /**
     * Sets the associated transaction type entity
     * Links balance to type for classification and validation
     * 
     * @param transactionType TransactionType entity
     */
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * Gets the optimistic locking version
     * 
     * @return Version number for optimistic locking
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Sets the optimistic locking version
     * 
     * @param version Version number for optimistic locking
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Adds amount to category balance with precise BigDecimal arithmetic
     * Maintains COBOL COMP-3 precision for financial calculations
     * Automatically updates last_updated timestamp
     * 
     * @param amount Amount to add to balance
     */
    public void addToBalance(BigDecimal amount) {
        if (amount != null) {
            this.categoryBalance = this.categoryBalance.add(amount, COBOL_MATH_CONTEXT).setScale(FINANCIAL_SCALE, RoundingMode.HALF_UP);
            this.lastUpdated = LocalDateTime.now();
        }
    }

    /**
     * Subtracts amount from category balance with precise BigDecimal arithmetic
     * Maintains COBOL COMP-3 precision for financial calculations
     * Automatically updates last_updated timestamp
     * 
     * @param amount Amount to subtract from balance
     */
    public void subtractFromBalance(BigDecimal amount) {
        if (amount != null) {
            this.categoryBalance = this.categoryBalance.subtract(amount, COBOL_MATH_CONTEXT).setScale(FINANCIAL_SCALE, RoundingMode.HALF_UP);
            this.lastUpdated = LocalDateTime.now();
        }
    }

    /**
     * Checks if category balance is positive
     * 
     * @return true if balance is greater than zero
     */
    public boolean isPositiveBalance() {
        return categoryBalance != null && categoryBalance.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if category balance is negative
     * 
     * @return true if balance is less than zero
     */
    public boolean isNegativeBalance() {
        return categoryBalance != null && categoryBalance.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Checks if category balance is zero
     * 
     * @return true if balance equals zero
     */
    public boolean isZeroBalance() {
        return categoryBalance != null && categoryBalance.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Equals method for entity comparison based on composite primary key
     * 
     * @param o Object to compare
     * @return true if objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionCategoryBalance that = (TransactionCategoryBalance) o;
        return Objects.equals(id, that.id);
    }

    /**
     * Hash code method for entity hashing based on composite primary key
     * 
     * @return hash code based on composite key
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * String representation of transaction category balance entity
     * 
     * @return String representation including key fields
     */
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
     * Composite primary key class for TransactionCategoryBalance entity
     * Contains account_id, transaction_type, and transaction_category fields
     * Maps to TRAN-CAT-KEY structure from COBOL copybook CVTRA01Y.cpy
     */
    @Embeddable
    public static class TransactionCategoryBalanceId implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Account identifier - Part of composite key
         * Maps to TRANCAT-ACCT-ID PIC 9(11) from COBOL structure
         */
        @Column(name = "account_id", length = 11, nullable = false)
        private String accountId;

        /**
         * Transaction type code - Part of composite key
         * Maps to TRANCAT-TYPE-CD PIC X(02) from COBOL structure
         */
        @Column(name = "transaction_type", length = 2, nullable = false)
        private String transactionType;

        /**
         * Transaction category code - Part of composite key
         * Maps to TRANCAT-CD PIC 9(04) from COBOL structure
         */
        @Column(name = "transaction_category", length = 4, nullable = false)
        private String transactionCategory;

        /**
         * Default constructor for JPA
         */
        public TransactionCategoryBalanceId() {
        }

        /**
         * Constructor with all key components
         * 
         * @param accountId Account identifier (11 digits)
         * @param transactionType Transaction type code (2 characters)
         * @param transactionCategory Transaction category code (4 characters)
         */
        public TransactionCategoryBalanceId(String accountId, String transactionType, String transactionCategory) {
            this.accountId = accountId;
            this.transactionType = transactionType;
            this.transactionCategory = transactionCategory;
        }

        /**
         * Gets the account identifier
         * 
         * @return Account ID as 11-digit string
         */
        public String getAccountId() {
            return accountId;
        }

        /**
         * Sets the account identifier
         * 
         * @param accountId Account ID as 11-digit string
         */
        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        /**
         * Gets the transaction type code
         * 
         * @return Transaction type as 2-character string
         */
        public String getTransactionType() {
            return transactionType;
        }

        /**
         * Sets the transaction type code
         * 
         * @param transactionType Transaction type as 2-character string
         */
        public void setTransactionType(String transactionType) {
            this.transactionType = transactionType;
        }

        /**
         * Gets the transaction category code
         * 
         * @return Transaction category as 4-character string
         */
        public String getTransactionCategory() {
            return transactionCategory;
        }

        /**
         * Sets the transaction category code
         * 
         * @param transactionCategory Transaction category as 4-character string
         */
        public void setTransactionCategory(String transactionCategory) {
            this.transactionCategory = transactionCategory;
        }

        /**
         * Equals method for composite key comparison
         * 
         * @param o Object to compare
         * @return true if objects are equal
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TransactionCategoryBalanceId that = (TransactionCategoryBalanceId) o;
            return Objects.equals(accountId, that.accountId) &&
                   Objects.equals(transactionType, that.transactionType) &&
                   Objects.equals(transactionCategory, that.transactionCategory);
        }

        /**
         * Hash code method for composite key hashing
         * 
         * @return hash code based on all key components
         */
        @Override
        public int hashCode() {
            return Objects.hash(accountId, transactionType, transactionCategory);
        }

        /**
         * String representation of composite key
         * 
         * @return String representation including all key components
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