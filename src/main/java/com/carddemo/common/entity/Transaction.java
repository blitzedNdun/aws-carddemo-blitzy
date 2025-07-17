package com.carddemo.common.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA Transaction entity mapping COBOL transaction record (CVTRA05Y.cpy) to PostgreSQL 
 * transactions table with UUID primary key, BigDecimal financial precision, timestamp 
 * partitioning support, and foreign key relationships for comprehensive transaction management.
 * 
 * This entity supports:
 * - 350-byte transaction record structure with exact financial precision per Section 2.1.5
 * - Monthly partitioning for transaction history management per Section 6.2.1.4
 * - Real-time transaction processing with atomic account updates per Section 5.2.2
 * - Sub-200ms response time for card authorization per Section 2.1.4
 * - PostgreSQL DECIMAL(12,2) mapping for exact financial precision
 * - SERIALIZABLE transaction isolation for data consistency
 * - Foreign key relationships to Account, Card, TransactionType, and TransactionCategory entities
 * 
 * Maps to PostgreSQL table: transactions
 * Original COBOL structure: TRAN-RECORD in CVTRA05Y.cpy (350 bytes)
 * 
 * Key field mappings:
 * - TRAN-ID PIC X(16) → transaction_id VARCHAR(16) primary key with UUID generation
 * - TRAN-AMT PIC S9(09)V99 → transaction_amount DECIMAL(12,2) with BigDecimal precision
 * - TRAN-ORIG-TS PIC X(26) → transaction_timestamp TIMESTAMP for partitioning support
 * - TRAN-TYPE-CD PIC X(02) → transaction_type FK to transaction_types table
 * - TRAN-CAT-CD PIC 9(04) → transaction_category FK to transaction_categories table
 * - TRAN-CARD-NUM PIC X(16) → card_number FK to cards table
 * - Account linkage via card_number relationship for balance management
 * 
 * Performance optimizations:
 * - Monthly RANGE partitioning on transaction_timestamp column
 * - B-tree indexes on (transaction_timestamp, account_id) for date-range queries
 * - Foreign key constraints for referential integrity
 * - Optimistic locking with @Version annotation for concurrent access control
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "transactions")
public class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * MathContext for BigDecimal operations maintaining COBOL COMP-3 precision.
     * Uses DECIMAL128 precision with HALF_UP rounding mode per Section 0.3.2.
     */
    public static final MathContext COBOL_MATH_CONTEXT = new MathContext(31, RoundingMode.HALF_UP);

    /**
     * Default scale for financial calculations (2 decimal places).
     */
    public static final int FINANCIAL_SCALE = 2;

    /**
     * Transaction identifier - Primary key mapping TRAN-ID PIC X(16).
     * Maps to PostgreSQL VARCHAR(16) transaction_id primary key with UUID generation.
     * Generated using UUID.randomUUID() for globally unique transaction identifiers.
     * 
     * Format: 16-character string representation of UUID
     * Example: "1234567890ABCDEF"
     */
    @Id
    @Column(name = "transaction_id", length = 16, nullable = false)
    @NotBlank(message = "Transaction ID cannot be blank")
    @Size(min = 16, max = 16, message = "Transaction ID must be exactly 16 characters")
    private String transactionId;

    /**
     * Transaction amount - Maps TRAN-AMT PIC S9(09)V99.
     * Maps to PostgreSQL DECIMAL(12,2) using BigDecimal for exact precision.
     * Maintains COBOL COMP-3 decimal precision with DECIMAL128 context.
     * 
     * Range: -9999999999.99 to 9999999999.99
     * Precision: 12 total digits, 2 decimal places
     * Validation: Minimum value to prevent inappropriate negative transactions
     */
    @Column(name = "transaction_amount", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Transaction amount cannot be null")
    @DecimalMin(value = "-9999999999.99", message = "Transaction amount cannot be less than -9999999999.99")
    private BigDecimal transactionAmount;

    /**
     * Transaction description - Maps TRAN-DESC PIC X(100).
     * Maps to PostgreSQL VARCHAR(100) for transaction description.
     * Contains descriptive text about the transaction purpose or details.
     * 
     * Examples: "Purchase at MERCHANT", "Cash Advance", "Payment Credit"
     */
    @Column(name = "description", length = 100, nullable = false)
    @NotBlank(message = "Transaction description cannot be blank")
    @Size(min = 1, max = 100, message = "Transaction description must be between 1 and 100 characters")
    private String description;

    /**
     * Transaction timestamp - Maps TRAN-ORIG-TS PIC X(26).
     * Maps to PostgreSQL TIMESTAMP for date-range partitioning support.
     * Used for monthly RANGE partitioning per Section 6.2.1.4.
     * 
     * Supports partition pruning for date-range queries in batch processing.
     * Enables 4-hour batch processing window with optimal query performance.
     */
    @Column(name = "transaction_timestamp", nullable = false)
    @NotNull(message = "Transaction timestamp cannot be null")
    private LocalDateTime transactionTimestamp;

    /**
     * Merchant name - Maps TRAN-MERCHANT-NAME PIC X(50).
     * Maps to PostgreSQL VARCHAR(50) for merchant identification.
     * Contains the name of the merchant where the transaction occurred.
     */
    @Column(name = "merchant_name", length = 50)
    @Size(max = 50, message = "Merchant name cannot exceed 50 characters")
    private String merchantName;

    /**
     * Merchant city - Maps TRAN-MERCHANT-CITY PIC X(50).
     * Maps to PostgreSQL VARCHAR(50) for merchant location.
     * Contains the city where the merchant is located.
     */
    @Column(name = "merchant_city", length = 50)
    @Size(max = 50, message = "Merchant city cannot exceed 50 characters")
    private String merchantCity;

    /**
     * Merchant ZIP code - Maps TRAN-MERCHANT-ZIP PIC X(10).
     * Maps to PostgreSQL VARCHAR(10) for merchant postal code.
     * Contains the postal code of the merchant location.
     */
    @Column(name = "merchant_zip", length = 10)
    @Size(max = 10, message = "Merchant ZIP code cannot exceed 10 characters")
    private String merchantZip;

    /**
     * Transaction source - Maps TRAN-SOURCE PIC X(10).
     * Maps to PostgreSQL VARCHAR(10) for transaction source identification.
     * Identifies the channel or system that initiated the transaction.
     * 
     * Examples: "POS", "ATM", "ONLINE", "PHONE"
     */
    @Column(name = "transaction_source", length = 10)
    @Size(max = 10, message = "Transaction source cannot exceed 10 characters")
    private String transactionSource;

    /**
     * Merchant identifier - Maps TRAN-MERCHANT-ID PIC 9(09).
     * Maps to PostgreSQL VARCHAR(9) for merchant identification.
     * Contains the unique identifier for the merchant.
     */
    @Column(name = "merchant_id", length = 9)
    @Size(max = 9, message = "Merchant ID cannot exceed 9 characters")
    private String merchantId;

    /**
     * Many-to-one relationship with Account entity.
     * Foreign key relationship derived from card_number → account_id mapping.
     * Enables account-based transaction tracking and balance management.
     * Uses lazy loading for optimal performance.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id")
    private Account account;

    /**
     * Many-to-one relationship with Card entity.
     * Foreign key relationship for card-based transaction tracking.
     * Maps to TRAN-CARD-NUM PIC X(16) field for card number validation.
     * Enables credit card transaction validation and card-based reporting.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_number", referencedColumnName = "card_number")
    private Card card;

    /**
     * Many-to-one relationship with TransactionType entity.
     * Foreign key relationship for transaction classification.
     * Maps to TRAN-TYPE-CD PIC X(02) field for transaction type validation.
     * Enables transaction classification and type-based processing.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_type", referencedColumnName = "transaction_type")
    private TransactionType transactionType;

    /**
     * Many-to-one relationship with TransactionCategory entity.
     * Foreign key relationship for transaction categorization.
     * Maps to TRAN-CAT-CD PIC 9(04) field for category-based balance tracking.
     * Enables transaction categorization and category-based financial reporting.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_category", referencedColumnName = "transaction_category")
    private TransactionCategory transactionCategory;

    /**
     * Optimistic locking version for concurrent transaction operations protection.
     * Enables transaction processing with concurrent access control.
     * Prevents lost updates in multi-user transaction processing scenarios.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Default constructor for JPA entity instantiation.
     * Initializes financial fields with zero values and generates transaction ID.
     */
    public Transaction() {
        this.transactionId = generateTransactionId();
        this.transactionAmount = BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
        this.transactionTimestamp = LocalDateTime.now();
        this.version = 0L;
    }

    /**
     * Constructor with essential fields for transaction creation.
     * 
     * @param transactionAmount Transaction amount with BigDecimal precision
     * @param description Transaction description
     * @param account Account entity for transaction linkage
     * @param card Card entity for transaction validation
     * @param transactionType Transaction type for classification
     * @param transactionCategory Transaction category for reporting
     */
    public Transaction(BigDecimal transactionAmount, String description, Account account, 
                      Card card, TransactionType transactionType, TransactionCategory transactionCategory) {
        this();
        this.transactionAmount = transactionAmount != null ? 
            transactionAmount.setScale(FINANCIAL_SCALE, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
        this.description = description;
        this.account = account;
        this.card = card;
        this.transactionType = transactionType;
        this.transactionCategory = transactionCategory;
    }

    /**
     * Generates a unique 16-character transaction ID using UUID.
     * Ensures globally unique transaction identifiers for PostgreSQL primary key.
     * 
     * @return 16-character transaction ID string
     */
    private String generateTransactionId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * Gets the transaction identifier.
     * 
     * @return Transaction ID as 16-character string
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Sets the transaction identifier.
     * 
     * @param transactionId Transaction ID as 16-character string
     */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Gets the associated account entity.
     * Provides access to account balance and credit limit information.
     * 
     * @return Account entity for balance management
     */
    public Account getAccount() {
        return account;
    }

    /**
     * Sets the associated account entity.
     * Links transaction to account for balance management and transaction processing.
     * 
     * @param account Account entity
     */
    public void setAccount(Account account) {
        this.account = account;
    }

    /**
     * Gets the associated card entity.
     * Provides access to card information for transaction validation.
     * 
     * @return Card entity for transaction validation
     */
    public Card getCard() {
        return card;
    }

    /**
     * Sets the associated card entity.
     * Links transaction to card for validation and card-based reporting.
     * 
     * @param card Card entity
     */
    public void setCard(Card card) {
        this.card = card;
    }

    /**
     * Gets the associated transaction type entity.
     * Provides access to transaction type classification information.
     * 
     * @return TransactionType entity for transaction classification
     */
    public TransactionType getTransactionType() {
        return transactionType;
    }

    /**
     * Sets the associated transaction type entity.
     * Links transaction to type for classification and type-based processing.
     * 
     * @param transactionType TransactionType entity
     */
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * Gets the associated transaction category entity.
     * Provides access to transaction category information for reporting.
     * 
     * @return TransactionCategory entity for categorization
     */
    public TransactionCategory getTransactionCategory() {
        return transactionCategory;
    }

    /**
     * Sets the associated transaction category entity.
     * Links transaction to category for categorization and financial reporting.
     * 
     * @param transactionCategory TransactionCategory entity
     */
    public void setTransactionCategory(TransactionCategory transactionCategory) {
        this.transactionCategory = transactionCategory;
    }

    /**
     * Gets the transaction amount with BigDecimal precision.
     * Maintains COBOL COMP-3 precision for financial calculations.
     * 
     * @return Transaction amount as BigDecimal with DECIMAL(12,2) precision
     */
    public BigDecimal getTransactionAmount() {
        return transactionAmount;
    }

    /**
     * Sets the transaction amount with BigDecimal precision.
     * Maintains COBOL COMP-3 precision for financial calculations.
     * 
     * @param transactionAmount Transaction amount as BigDecimal
     */
    public void setTransactionAmount(BigDecimal transactionAmount) {
        this.transactionAmount = transactionAmount != null ? 
            transactionAmount.setScale(FINANCIAL_SCALE, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
    }

    /**
     * Gets the transaction description.
     * 
     * @return Transaction description string
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the transaction description.
     * 
     * @param description Transaction description string
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the transaction timestamp.
     * 
     * @return Transaction timestamp as LocalDateTime
     */
    public LocalDateTime getTransactionTimestamp() {
        return transactionTimestamp;
    }

    /**
     * Sets the transaction timestamp.
     * 
     * @param transactionTimestamp Transaction timestamp as LocalDateTime
     */
    public void setTransactionTimestamp(LocalDateTime transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
    }

    /**
     * Gets the merchant name.
     * 
     * @return Merchant name string
     */
    public String getMerchantName() {
        return merchantName;
    }

    /**
     * Sets the merchant name.
     * 
     * @param merchantName Merchant name string
     */
    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    /**
     * Gets the merchant city.
     * 
     * @return Merchant city string
     */
    public String getMerchantCity() {
        return merchantCity;
    }

    /**
     * Sets the merchant city.
     * 
     * @param merchantCity Merchant city string
     */
    public void setMerchantCity(String merchantCity) {
        this.merchantCity = merchantCity;
    }

    /**
     * Gets the merchant ZIP code.
     * 
     * @return Merchant ZIP code string
     */
    public String getMerchantZip() {
        return merchantZip;
    }

    /**
     * Sets the merchant ZIP code.
     * 
     * @param merchantZip Merchant ZIP code string
     */
    public void setMerchantZip(String merchantZip) {
        this.merchantZip = merchantZip;
    }

    /**
     * Gets the transaction source.
     * 
     * @return Transaction source string
     */
    public String getTransactionSource() {
        return transactionSource;
    }

    /**
     * Sets the transaction source.
     * 
     * @param transactionSource Transaction source string
     */
    public void setTransactionSource(String transactionSource) {
        this.transactionSource = transactionSource;
    }

    /**
     * Gets the merchant identifier.
     * 
     * @return Merchant ID string
     */
    public String getMerchantId() {
        return merchantId;
    }

    /**
     * Sets the merchant identifier.
     * 
     * @param merchantId Merchant ID string
     */
    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    /**
     * Gets the optimistic locking version.
     * 
     * @return Version number for optimistic locking
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Sets the optimistic locking version.
     * 
     * @param version Version number for optimistic locking
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Checks if this is a debit transaction (reduces account balance).
     * Uses transaction type's debit/credit indicator for determination.
     * 
     * @return true if this is a debit transaction
     */
    public boolean isDebitTransaction() {
        return transactionType != null && transactionType.isDebitTransaction();
    }

    /**
     * Checks if this is a credit transaction (increases account balance).
     * Uses transaction type's debit/credit indicator for determination.
     * 
     * @return true if this is a credit transaction
     */
    public boolean isCreditTransaction() {
        return transactionType != null && transactionType.isCreditTransaction();
    }

    /**
     * Gets the absolute transaction amount for display purposes.
     * Returns the absolute value of the transaction amount.
     * 
     * @return Absolute transaction amount as BigDecimal
     */
    public BigDecimal getAbsoluteAmount() {
        return transactionAmount != null ? transactionAmount.abs() : BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
    }

    /**
     * Calculates the impact on account balance based on transaction type.
     * Returns positive value for credits, negative for debits.
     * 
     * @return Balance impact as BigDecimal
     */
    public BigDecimal getBalanceImpact() {
        if (transactionAmount == null) {
            return BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
        }
        
        if (isDebitTransaction()) {
            return transactionAmount.negate();
        } else if (isCreditTransaction()) {
            return transactionAmount;
        }
        
        return BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
    }

    /**
     * Gets formatted transaction amount for display.
     * Returns formatted string with currency symbol and proper decimal places.
     * 
     * @return Formatted transaction amount string
     */
    public String getFormattedAmount() {
        if (transactionAmount == null) {
            return "$0.00";
        }
        return String.format("$%,.2f", transactionAmount);
    }

    /**
     * Checks if transaction occurred today.
     * 
     * @return true if transaction timestamp is today
     */
    public boolean isToday() {
        return transactionTimestamp != null && 
               transactionTimestamp.toLocalDate().equals(LocalDateTime.now().toLocalDate());
    }

    /**
     * Checks if transaction has complete merchant information.
     * 
     * @return true if merchant name, city, and ZIP are all populated
     */
    public boolean hasCompleteMerchantInfo() {
        return merchantName != null && !merchantName.trim().isEmpty() &&
               merchantCity != null && !merchantCity.trim().isEmpty() &&
               merchantZip != null && !merchantZip.trim().isEmpty();
    }

    /**
     * Updates transaction amount with proper financial precision.
     * Maintains COBOL COMP-3 precision using BigDecimal operations.
     * 
     * @param amount New transaction amount
     */
    public void updateAmount(BigDecimal amount) {
        if (amount != null) {
            this.transactionAmount = amount.setScale(FINANCIAL_SCALE, RoundingMode.HALF_UP);
        }
    }

    /**
     * Applies transaction amount to account balance.
     * Updates account balance based on transaction type and amount.
     * 
     * @param targetAccount Account to update
     */
    public void applyToAccount(Account targetAccount) {
        if (targetAccount != null && transactionAmount != null) {
            BigDecimal impact = getBalanceImpact();
            targetAccount.addToBalance(impact);
        }
    }

    /**
     * Validates transaction data completeness.
     * Checks all required fields are populated and valid.
     * 
     * @return true if transaction is valid for processing
     */
    public boolean isValidForProcessing() {
        return transactionId != null && !transactionId.trim().isEmpty() &&
               transactionAmount != null &&
               description != null && !description.trim().isEmpty() &&
               transactionTimestamp != null &&
               account != null &&
               card != null &&
               transactionType != null &&
               transactionCategory != null;
    }

    /**
     * Compares this Transaction with another object for equality.
     * Two Transaction objects are considered equal if they have the same transaction ID.
     * 
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Transaction that = (Transaction) obj;
        return Objects.equals(transactionId, that.transactionId);
    }

    /**
     * Returns the hash code for this Transaction.
     * The hash code is based on the transaction ID.
     * 
     * @return hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    /**
     * Returns a string representation of this Transaction.
     * 
     * @return string representation including key fields
     */
    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId='" + transactionId + '\'' +
                ", transactionAmount=" + transactionAmount +
                ", description='" + description + '\'' +
                ", transactionTimestamp=" + transactionTimestamp +
                ", merchantName='" + merchantName + '\'' +
                ", merchantCity='" + merchantCity + '\'' +
                ", merchantZip='" + merchantZip + '\'' +
                ", transactionSource='" + transactionSource + '\'' +
                ", merchantId='" + merchantId + '\'' +
                ", version=" + version +
                '}';
    }
}