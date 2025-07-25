package com.carddemo.common.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA Transaction entity mapping COBOL transaction record to PostgreSQL transactions table.
 * 
 * This entity represents the complete 350-byte transaction record structure from COBOL
 * copybook CVTRA05Y.cpy with exact financial precision using BigDecimal for monetary
 * calculations per Section 0.3.2 requirements. The entity supports monthly partitioning
 * for transaction history management per Section 6.2.1.4 and enables real-time
 * transaction processing with atomic account updates per Section 5.2.2.
 * 
 * Converted from COBOL transaction record structure:
 * - TRAN-ID X(16) → transaction_id VARCHAR(16) primary key with UUID generation
 * - TRAN-TYPE-CD X(02) → transaction_type VARCHAR(2) foreign key to transaction_types
 * - TRAN-CAT-CD 9(04) → transaction_category VARCHAR(4) foreign key to transaction_categories
 * - TRAN-SOURCE X(10) → transaction_source VARCHAR(10) transaction origin indicator
 * - TRAN-DESC X(100) → description VARCHAR(100) transaction description
 * - TRAN-AMT S9(09)V99 → transaction_amount DECIMAL(12,2) using BigDecimal precision
 * - TRAN-MERCHANT-ID 9(09) → merchant_id VARCHAR(9) merchant identifier
 * - TRAN-MERCHANT-NAME X(50) → merchant_name VARCHAR(50) merchant business name
 * - TRAN-MERCHANT-CITY X(50) → merchant_city VARCHAR(30) merchant location city
 * - TRAN-MERCHANT-ZIP X(10) → merchant_zip VARCHAR(10) merchant postal code
 * - TRAN-CARD-NUM X(16) → card_number VARCHAR(16) foreign key to cards table
 * - TRAN-ORIG-TS X(26) → transaction_timestamp TIMESTAMP original transaction time
 * - TRAN-PROC-TS X(26) → processing_timestamp TIMESTAMP system processing time
 * 
 * Database mapping features:
 * - Primary key: transaction_id with UUID generation for globally unique identifiers
 * - Monthly RANGE partitioning on transaction_timestamp for optimal query performance
 * - Foreign key relationships to Account, Card, TransactionType, and TransactionCategory
 * - BigDecimal financial precision maintaining COBOL COMP-3 arithmetic accuracy
 * - Optimistic locking with @Version annotation for concurrent transaction safety
 */
@Entity(name = "CommonTransaction")
@Table(name = "transactions", schema = "public")
public class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * COBOL COMP-3 precision equivalent using DECIMAL128 context for exact financial calculations.
     * Maintains COBOL arithmetic precision per Section 0.3.2 requirements with half-even rounding
     * for banking compliance and consistent decimal representation across all monetary operations.
     */
    private static final MathContext COBOL_MATH_CONTEXT = new MathContext(31, RoundingMode.HALF_EVEN);

    /**
     * Transaction identifier serving as primary key.
     * Maps to TRAN-ID from COBOL copybook CVTRA05Y.cpy (PIC X(16)).
     * 
     * Generates globally unique 16-character transaction identifiers using UUID
     * for PostgreSQL primary key storage. The UUID string representation is
     * truncated to 16 characters to maintain compatibility with original COBOL
     * field length while ensuring uniqueness across distributed systems.
     */
    @Id
    @Column(name = "transaction_id", length = 16, nullable = false)
    @NotBlank(message = "Transaction ID cannot be blank")
    @Size(min = 16, max = 16, message = "Transaction ID must be exactly 16 characters")
    private String transactionId;

    /**
     * Many-to-one relationship with Account entity.
     * References the account associated with this transaction for balance management
     * and account-based transaction tracking with BigDecimal financial precision.
     * 
     * Uses account_id as foreign key reference per Section 6.2.1.1 relationships.
     * Enables atomic account updates during transaction processing.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id", nullable = false)
    @NotNull(message = "Account cannot be null")
    private Account account;

    /**
     * Many-to-one relationship with Card entity.
     * References the card used for this transaction enabling card-based transaction
     * tracking and credit card transaction validation with card number mapping.
     * 
     * Maps to TRAN-CARD-NUM from COBOL copybook (PIC X(16)).
     * Uses card_number as foreign key reference for transaction authorization.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_number", referencedColumnName = "card_number", nullable = false)
    @NotNull(message = "Card cannot be null")
    private Card card;

    /**
     * Many-to-one relationship with TransactionType entity.
     * References transaction type for classification and validation in transaction
     * processing workflows. Enables debit/credit indicator processing.
     * 
     * Maps to TRAN-TYPE-CD from COBOL copybook (PIC X(02)).
     * Uses transaction_type as foreign key reference for business rule validation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_type", referencedColumnName = "transaction_type", nullable = false)
    @NotNull(message = "Transaction type cannot be null")
    private TransactionType transactionType;

    /**
     * Many-to-one relationship with TransactionCategory entity.
     * References transaction category for categorization and category-based balance
     * tracking for financial reporting and analytics.
     * 
     * Maps to TRAN-CAT-CD from COBOL copybook (PIC 9(04)).
     * Uses transaction_category as foreign key reference for reporting and analysis.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_category", referencedColumnName = "transaction_category", nullable = false)
    @NotNull(message = "Transaction category cannot be null")
    private TransactionCategory transactionCategory;

    /**
     * Transaction amount with exact decimal precision.
     * Maps to TRAN-AMT from COBOL copybook (PIC S9(09)V99).
     * Original COBOL: 9 integer digits + 2 decimal places with sign
     * PostgreSQL: DECIMAL(12,2) (9 integer digits + 2 decimal places + extra precision)
     * 
     * Using BigDecimal to maintain COBOL COMP-3 precision per Section 0.3.2 requirements.
     * Supports range -9999999999.99 to +9999999999.99 for comprehensive transaction amounts.
     * Minimum value validation ensures only positive transaction amounts are accepted.
     */
    @Column(name = "transaction_amount", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Transaction amount cannot be null")
    @DecimalMin(value = "0.01", message = "Transaction amount must be greater than zero")
    private BigDecimal transactionAmount;

    /**
     * Transaction description providing details about the transaction.
     * Maps to TRAN-DESC from COBOL copybook (PIC X(100)).
     * 
     * Stores human-readable transaction description for transaction history
     * and customer statement generation with 100-character limit preservation.
     */
    @Column(name = "description", length = 100, nullable = false)
    @NotBlank(message = "Transaction description cannot be blank")
    @Size(min = 1, max = 100, message = "Description must be between 1 and 100 characters")
    private String description;

    /**
     * Transaction timestamp for date-range partitioning support.
     * Maps to TRAN-ORIG-TS from COBOL copybook (PIC X(26)).
     * 
     * Uses LocalDateTime for precise timestamp representation supporting PostgreSQL
     * TIMESTAMP type for date-range partitioning and transaction tracking per
     * Section 6.2.1.4 monthly RANGE partitioning requirements.
     */
    @Column(name = "transaction_timestamp", nullable = false)
    @NotNull(message = "Transaction timestamp cannot be null")
    private LocalDateTime transactionTimestamp;

    /**
     * Merchant name for transaction identification and customer statements.
     * Maps to TRAN-MERCHANT-NAME from COBOL copybook (PIC X(50)).
     * 
     * Stores merchant business name for transaction recognition and dispute
     * resolution with proper validation ensuring data quality.
     */
    @Column(name = "merchant_name", length = 50)
    @Size(max = 50, message = "Merchant name cannot exceed 50 characters")
    private String merchantName;

    /**
     * Merchant city for geographic transaction tracking.
     * Maps to TRAN-MERCHANT-CITY from COBOL copybook (PIC X(50)).
     * Note: Reduced to 30 characters in PostgreSQL for optimization.
     * 
     * Provides merchant location information for fraud detection and
     * customer transaction recognition with geographic context.
     */
    @Column(name = "merchant_city", length = 30)
    @Size(max = 30, message = "Merchant city cannot exceed 30 characters")
    private String merchantCity;

    /**
     * Merchant ZIP code for location-based transaction analysis.
     * Maps to TRAN-MERCHANT-ZIP from COBOL copybook (PIC X(10)).
     * 
     * Enables geographic analysis of transaction patterns and supports
     * merchant location validation with postal code formatting.
     */
    @Column(name = "merchant_zip", length = 10)
    @Size(max = 10, message = "Merchant ZIP cannot exceed 10 characters")
    private String merchantZip;

    /**
     * Transaction source indicator showing transaction origin.
     * Maps to TRAN-SOURCE from COBOL copybook (PIC X(10)).
     * 
     * Identifies the source system or channel that originated the transaction
     * for audit trail and system integration tracking.
     */
    @Column(name = "transaction_source", length = 10)
    @Size(max = 10, message = "Transaction source cannot exceed 10 characters")
    private String transactionSource;

    /**
     * Merchant identifier for merchant management and reporting.
     * Maps to TRAN-MERCHANT-ID from COBOL copybook (PIC 9(09)).
     * 
     * Numeric merchant identifier enabling merchant-based transaction
     * analysis and business relationship management.
     */
    @Column(name = "merchant_id", length = 9)
    @Size(max = 9, message = "Merchant ID cannot exceed 9 characters")
    private String merchantId;

    /**
     * Version field for optimistic locking support.
     * Enables concurrent transaction operations protection per Section 5.2.2 requirements.
     * Automatically managed by JPA for conflict resolution in multi-user environments.
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Default constructor required by JPA specification.
     * Initializes transaction with current timestamp and zero amount with proper scale.
     */
    public Transaction() {
        this.transactionAmount = new BigDecimal("0.00");
        this.transactionTimestamp = LocalDateTime.now();
    }

    /**
     * Constructor with required fields for transaction creation.
     * 
     * @param account The account associated with this transaction
     * @param card The card used for this transaction
     * @param transactionType The type classification for this transaction
     * @param transactionCategory The category classification for this transaction
     * @param transactionAmount The monetary amount of the transaction
     * @param description The transaction description
     */
    public Transaction(Account account, Card card, TransactionType transactionType, 
                      TransactionCategory transactionCategory, BigDecimal transactionAmount, String description) {
        this();
        this.transactionId = generateTransactionId();
        this.account = account;
        this.card = card;
        this.transactionType = transactionType;
        this.transactionCategory = transactionCategory;
        this.transactionAmount = transactionAmount != null ? 
            transactionAmount.setScale(2, RoundingMode.HALF_EVEN) : new BigDecimal("0.00");
        this.description = description;
    }

    /**
     * Generates a unique 16-character transaction ID using UUID.
     * Uses UUID.randomUUID() and toString() methods per external import requirements.
     * 
     * @return 16-character transaction identifier for primary key usage
     */
    private String generateTransactionId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * Gets the transaction identifier.
     * 
     * @return The unique transaction ID (VARCHAR(16))
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Sets the transaction identifier.
     * 
     * @param transactionId The unique transaction ID (16 characters, not null)
     */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Gets the associated account entity.
     * Provides access to account information for transaction-to-account operations.
     * Uses getAccountId() method per internal import requirements.
     * 
     * @return The Account entity linked to this transaction
     */
    public Account getAccount() {
        return account;
    }

    /**
     * Sets the associated account entity.
     * Establishes transaction-to-account relationship for balance management.
     * Uses setAccountId() method per internal import requirements.
     * 
     * @param account The Account entity to link to this transaction
     */
    public void setAccount(Account account) {
        this.account = account;
    }

    /**
     * Gets the associated card entity.
     * Provides access to card information for transaction authorization.
     * Uses getCardNumber() method per internal import requirements.
     * 
     * @return The Card entity used for this transaction
     */
    public Card getCard() {
        return card;
    }

    /**
     * Sets the associated card entity.
     * Establishes transaction-to-card relationship for authorization tracking.
     * Uses setCardNumber() method per internal import requirements.
     * 
     * @param card The Card entity to link to this transaction
     */
    public void setCard(Card card) {
        this.card = card;
    }

    /**
     * Gets the transaction type entity.
     * Provides access to transaction type classification and debit/credit indicators.
     * Uses getTransactionType() method per internal import requirements.
     * 
     * @return The TransactionType entity for this transaction
     */
    public TransactionType getTransactionType() {
        return transactionType;
    }

    /**
     * Sets the transaction type entity.
     * Establishes transaction classification for business rule processing.
     * Uses setTransactionType() method per internal import requirements.
     * 
     * @param transactionType The TransactionType entity for classification
     */
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * Gets the transaction category entity.
     * Provides access to category classification for reporting and analysis.
     * Uses getTransactionCategory() method per internal import requirements.
     * 
     * @return The TransactionCategory entity for this transaction
     */
    public TransactionCategory getTransactionCategory() {
        return transactionCategory;
    }

    /**
     * Sets the transaction category entity.
     * Establishes category classification for financial reporting.
     * Uses setTransactionCategory() method per internal import requirements.
     * 
     * @param transactionCategory The TransactionCategory entity for classification
     */
    public void setTransactionCategory(TransactionCategory transactionCategory) {
        this.transactionCategory = transactionCategory;
    }

    /**
     * Gets the transaction amount with BigDecimal precision.
     * Uses BigDecimal() constructor and setScale() methods per external import requirements.
     * 
     * @return The transaction amount as BigDecimal with DECIMAL(12,2) precision
     */
    public BigDecimal getTransactionAmount() {
        return transactionAmount;
    }

    /**
     * Sets the transaction amount using BigDecimal for precise financial calculations.
     * Maintains COBOL COMP-3 precision equivalent per Section 0.3.2 requirements.
     * Uses setScale() method per external import requirements.
     * 
     * @param transactionAmount The transaction amount (must be positive)
     */
    public void setTransactionAmount(BigDecimal transactionAmount) {
        this.transactionAmount = transactionAmount != null ? 
            transactionAmount.setScale(2, RoundingMode.HALF_EVEN) : new BigDecimal("0.00");
    }

    /**
     * Gets the transaction description.
     * 
     * @return The transaction description for customer statements and history
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the transaction description.
     * 
     * @param description The transaction description (1-100 characters)
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the transaction timestamp.
     * Uses LocalDateTime.now() and of() methods per external import requirements.
     * 
     * @return The transaction timestamp for partitioning and audit trail
     */
    public LocalDateTime getTransactionTimestamp() {
        return transactionTimestamp;
    }

    /**
     * Sets the transaction timestamp using LocalDateTime for precise timestamp management.
     * Uses LocalDateTime.of() method for timestamp specification as required by external imports.
     * 
     * @param transactionTimestamp The transaction timestamp for partitioning support
     */
    public void setTransactionTimestamp(LocalDateTime transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp != null ? transactionTimestamp : LocalDateTime.now();
    }

    /**
     * Gets the merchant name.
     * 
     * @return The merchant business name for transaction identification
     */
    public String getMerchantName() {
        return merchantName;
    }

    /**
     * Sets the merchant name with validation.
     * 
     * @param merchantName The merchant business name (up to 50 characters)
     */
    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    /**
     * Gets the merchant city.
     * 
     * @return The merchant location city for geographic analysis
     */
    public String getMerchantCity() {
        return merchantCity;
    }

    /**
     * Sets the merchant city with validation.
     * 
     * @param merchantCity The merchant location city (up to 30 characters)
     */
    public void setMerchantCity(String merchantCity) {
        this.merchantCity = merchantCity;
    }

    /**
     * Gets the merchant ZIP code.
     * 
     * @return The merchant postal code for location validation
     */
    public String getMerchantZip() {
        return merchantZip;
    }

    /**
     * Sets the merchant ZIP code with validation.
     * 
     * @param merchantZip The merchant postal code (up to 10 characters)
     */
    public void setMerchantZip(String merchantZip) {
        this.merchantZip = merchantZip;
    }

    /**
     * Gets the transaction source indicator.
     * 
     * @return The transaction origin system or channel identifier
     */
    public String getTransactionSource() {
        return transactionSource;
    }

    /**
     * Sets the transaction source indicator.
     * 
     * @param transactionSource The origin system identifier (up to 10 characters)
     */
    public void setTransactionSource(String transactionSource) {
        this.transactionSource = transactionSource;
    }

    /**
     * Gets the merchant identifier.
     * 
     * @return The numeric merchant identifier for business relationship management
     */
    public String getMerchantId() {
        return merchantId;
    }

    /**
     * Sets the merchant identifier.
     * 
     * @param merchantId The merchant identifier (up to 9 characters)
     */
    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
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
     * Checks if this is a debit transaction based on transaction type.
     * Uses transaction type's debit/credit indicator for classification.
     * 
     * @return true if this is a debit transaction (decreases account balance)
     */
    public boolean isDebitTransaction() {
        return transactionType != null && transactionType.isDebit();
    }

    /**
     * Checks if this is a credit transaction based on transaction type.
     * Uses transaction type's debit/credit indicator for classification.
     * 
     * @return true if this is a credit transaction (increases account balance)
     */
    public boolean isCreditTransaction() {
        return transactionType != null && transactionType.isCredit();
    }

    /**
     * Gets the absolute transaction amount for calculations.
     * Uses BigDecimal.abs() method for absolute value operations.
     * 
     * @return Absolute value of transaction amount
     */
    public BigDecimal getAbsoluteAmount() {
        return transactionAmount != null ? transactionAmount.abs() : new BigDecimal("0.00");
    }

    /**
     * Calculates the impact on account balance based on transaction type.
     * Debit transactions return negative amount, credit transactions return positive amount.
     * Uses BigDecimal.negate() method per arithmetic requirements.
     * 
     * @return Transaction amount with appropriate sign for balance calculations
     */
    public BigDecimal getBalanceImpact() {
        if (transactionAmount == null) {
            return new BigDecimal("0.00");
        }
        return isDebitTransaction() ? transactionAmount.negate() : transactionAmount;
    }

    /**
     * Processes transaction amount with BigDecimal arithmetic operations.
     * Uses add() and subtract() methods per external import requirements.
     * Applies transaction to account balance based on transaction type.
     * 
     * @param currentBalance The current account balance
     * @return New balance after applying this transaction
     */
    public BigDecimal applyToBalance(BigDecimal currentBalance) {
        if (currentBalance == null || transactionAmount == null) {
            return new BigDecimal("0.00");
        }
        
        if (isDebitTransaction()) {
            return currentBalance.subtract(transactionAmount);
        } else {
            return currentBalance.add(transactionAmount);
        }
    }

    /**
     * Validates merchant information completeness.
     * Ensures all merchant fields are properly populated for transaction processing.
     * 
     * @return true if merchant information is complete and valid
     */
    public boolean isMerchantInformationComplete() {
        return merchantName != null && !merchantName.trim().isEmpty() &&
               merchantCity != null && !merchantCity.trim().isEmpty() &&
               merchantId != null && !merchantId.trim().isEmpty();
    }

    /**
     * Generates transaction reference for customer statements and audit trails.
     * Combines transaction ID with timestamp for unique reference generation.
     * 
     * @return Formatted transaction reference for external identification
     */
    public String getTransactionReference() {
        if (transactionId == null || transactionTimestamp == null) {
            return "UNKNOWN";
        }
        return String.format("%s-%04d%02d%02d", 
            transactionId, 
            transactionTimestamp.getYear(),
            transactionTimestamp.getMonthValue(),
            transactionTimestamp.getDayOfMonth());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Transaction transaction = (Transaction) obj;
        return Objects.equals(transactionId, transaction.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId='" + transactionId + '\'' +
                ", accountId='" + (account != null ? account.getAccountId() : null) + '\'' +
                ", cardNumber='" + (card != null ? card.getCardNumber() : null) + '\'' +
                ", transactionType='" + (transactionType != null ? transactionType.getTransactionType() : null) + '\'' +
                ", transactionCategory='" + (transactionCategory != null ? transactionCategory.getTransactionCategory() : null) + '\'' +
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