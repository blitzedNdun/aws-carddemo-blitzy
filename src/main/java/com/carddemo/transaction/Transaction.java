package com.carddemo.transaction;

import com.carddemo.common.enums.TransactionType;
import com.carddemo.common.enums.TransactionCategory;
import com.carddemo.common.validator.ValidCardNumber;
import com.carddemo.common.validator.ValidCurrency;
import com.carddemo.account.entity.Account;
import com.carddemo.card.Card;
import com.carddemo.common.util.BigDecimalUtils;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA Transaction entity representing transaction records with precise BigDecimal financial data handling
 * and comprehensive database relationship mapping.
 * 
 * This entity maps the PostgreSQL transactions table structure exactly preserving COBOL TRAN-RECORD 
 * field definitions and data types from CVTRA05Y.cpy copybook. The implementation ensures BigDecimal 
 * precision for financial amounts maintains exact arithmetic equivalent to COBOL packed decimal 
 * operations as mandated by Section 0.1.2 of the technical specification.
 * 
 * Key Features:
 * - Exact field correspondence to COBOL TRAN-RECORD structure (350-byte record layout)
 * - BigDecimal mapping for transaction amounts with DECIMAL(11,2) precision equivalent to COBOL S9(09)V99 COMP-3
 * - JPA annotations for primary key, foreign key relationships, and database constraints equivalent to VSAM data validation
 * - Audit trail fields for transaction creation and modification timestamps with automatic population
 * - Jackson JSON serialization annotations for REST API response formatting with proper date and decimal handling
 * - JPA entity relationships for account, card, and transaction category associations with lazy loading optimization
 * 
 * Database Mapping:
 * - transaction_id: VARCHAR(16) primary key matching COBOL TRAN-ID field
 * - transaction_type: VARCHAR(2) with TransactionType enum validation
 * - category_code: VARCHAR(4) with TransactionCategory enum validation  
 * - amount: DECIMAL(11,2) using BigDecimal for exact financial precision
 * - Foreign key relationships to accounts and cards tables
 * - Timestamp fields for audit trail and transaction processing
 * 
 * Performance Considerations:
 * - Lazy loading for entity relationships to optimize query performance
 * - Composite indexes on frequently queried fields (account_id, card_number, processing_timestamp)
 * - BigDecimal operations using DECIMAL128 context for maintaining COBOL precision
 * 
 * @author CardDemo Migration Team - Blitzy Platform
 * @version 1.0
 * @since Java 21
 */
@Entity
@Table(name = "transactions", schema = "public", indexes = {
    @Index(name = "idx_transactions_account_id", columnList = "account_id"),
    @Index(name = "idx_transactions_card_number", columnList = "card_number"),
    @Index(name = "idx_transactions_processing_timestamp", columnList = "processing_timestamp"),
    @Index(name = "idx_transactions_original_timestamp", columnList = "original_timestamp"),
    @Index(name = "idx_transactions_merchant_id", columnList = "merchant_id")
})
public class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Transaction identifier serving as primary key.
     * Maps to TRAN-ID from COBOL copybook CVTRA05Y.cpy (PIC X(16)).
     * 16-character unique transaction identifier for transaction tracking and audit purposes.
     */
    @Id
    @Column(name = "transaction_id", length = 16, nullable = false)
    @NotNull(message = "Transaction ID cannot be null")
    @Size(min = 16, max = 16, message = "Transaction ID must be exactly 16 characters")
    @JsonProperty("transactionId")
    private String transactionId;

    /**
     * Transaction type code with enum validation.
     * Maps to TRAN-TYPE-CD from COBOL copybook (PIC X(02)).
     * Uses TransactionType enum for validation and business logic processing.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 2, nullable = false)
    @NotNull(message = "Transaction type cannot be null")
    @JsonProperty("transactionType")
    private TransactionType transactionType;

    /**
     * Transaction category code with enum validation.
     * Maps to TRAN-CAT-CD from COBOL copybook (PIC 9(04)).
     * Uses TransactionCategory enum for categorization and balance management.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category_code", length = 4, nullable = false)
    @NotNull(message = "Category code cannot be null")
    @JsonProperty("categoryCode")
    private TransactionCategory categoryCode;

    /**
     * Transaction amount with exact BigDecimal precision.
     * Maps to TRAN-AMT from COBOL copybook (PIC S9(09)V99).
     * Original COBOL: 9 integer digits + 2 decimal places with sign
     * PostgreSQL: DECIMAL(11,2) (9 integer digits + 2 decimal places + sign)
     * 
     * Using BigDecimal with DECIMAL128 context to maintain COBOL COMP-3 precision 
     * as required by Section 0.3.2. Supports range -999999999.99 to +999999999.99.
     */
    @Column(name = "amount", precision = 11, scale = 2, nullable = false)
    @NotNull(message = "Transaction amount cannot be null")
    @ValidCurrency(precision = 11, scale = 2, min = "-999999999.99", max = "999999999.99", 
                   allowNegative = true, message = "Transaction amount must be valid monetary value")
    @JsonProperty("amount")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;

    /**
     * Transaction description for audit and display purposes.
     * Maps to TRAN-DESC from COBOL copybook (PIC X(100)).
     * 100-character description field for transaction details.
     */
    @Column(name = "description", length = 100)
    @Size(max = 100, message = "Description cannot exceed 100 characters")
    @JsonProperty("description")
    private String description;

    /**
     * Credit card number with validation.
     * Maps to TRAN-CARD-NUM from COBOL copybook (PIC X(16)).
     * 16-digit card number with Luhn algorithm validation for transaction authorization.
     */
    @Column(name = "card_number", length = 16, nullable = false)
    @NotNull(message = "Card number cannot be null")
    @ValidCardNumber(message = "Card number must be valid 16-digit number")
    @JsonProperty("cardNumber")
    private String cardNumber;

    /**
     * Merchant identifier for transaction processing.
     * Maps to TRAN-MERCHANT-ID from COBOL copybook (PIC 9(09)).
     * 9-digit merchant identifier for merchant-based transaction operations.
     */
    @Column(name = "merchant_id", length = 9)
    @Size(max = 9, message = "Merchant ID cannot exceed 9 characters")
    @Pattern(regexp = "\\d{0,9}", message = "Merchant ID must contain only digits")
    @JsonProperty("merchantId")
    private String merchantId;

    /**
     * Merchant name for transaction display.
     * Maps to TRAN-MERCHANT-NAME from COBOL copybook (PIC X(50)).
     * 50-character merchant name for transaction identification and reporting.
     */
    @Column(name = "merchant_name", length = 50)
    @Size(max = 50, message = "Merchant name cannot exceed 50 characters")
    @JsonProperty("merchantName")
    private String merchantName;

    /**
     * Merchant city for transaction processing.
     * Maps to TRAN-MERCHANT-CITY from COBOL copybook (PIC X(50)).
     * 50-character merchant city for geographic transaction tracking.
     */
    @Column(name = "merchant_city", length = 50)
    @Size(max = 50, message = "Merchant city cannot exceed 50 characters")
    @JsonProperty("merchantCity")
    private String merchantCity;

    /**
     * Merchant ZIP code for transaction processing.
     * Maps to TRAN-MERCHANT-ZIP from COBOL copybook (PIC X(10)).
     * 10-character merchant ZIP code for geographic transaction validation.
     */
    @Column(name = "merchant_zip", length = 10)
    @Size(max = 10, message = "Merchant ZIP cannot exceed 10 characters")
    @JsonProperty("merchantZip")
    private String merchantZip;

    /**
     * Original transaction timestamp for audit trail.
     * Maps to TRAN-ORIG-TS from COBOL copybook (PIC X(26)).
     * Original timestamp when transaction was initiated, using LocalDateTime for precise temporal operations.
     */
    @Column(name = "original_timestamp", nullable = false)
    @NotNull(message = "Original timestamp cannot be null")
    @JsonProperty("originalTimestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime originalTimestamp;

    /**
     * Processing timestamp for transaction lifecycle tracking.
     * Maps to TRAN-PROC-TS from COBOL copybook (PIC X(26)).
     * Processing timestamp when transaction was processed by the system.
     */
    @Column(name = "processing_timestamp")
    @JsonProperty("processingTimestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime processingTimestamp;

    /**
     * Transaction source for origin tracking.
     * Maps to TRAN-SOURCE from COBOL copybook (PIC X(10)).
     * 10-character source field indicating transaction origin (POS, ATM, WEB, etc.).
     */
    @Column(name = "source", length = 10)
    @Size(max = 10, message = "Source cannot exceed 10 characters")
    @JsonProperty("source")
    private String source;

    /**
     * Many-to-one relationship with Account entity.
     * Represents the account associated with this transaction for balance management.
     * Uses lazy loading for performance optimization.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id")
    private Account account;

    /**
     * Many-to-one relationship with Card entity.
     * Represents the card used for this transaction, enabling card-based transaction lookups.
     * Uses lazy loading for performance optimization.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_number", referencedColumnName = "card_number", insertable = false, updatable = false)
    private Card card;

    /**
     * Version field for optimistic locking support.
     * Enables concurrent transaction operations protection for multi-user environments.
     * Automatically managed by JPA for conflict resolution.
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Default constructor required by JPA specification.
     * Initializes timestamps for audit trail management.
     */
    public Transaction() {
        this.originalTimestamp = LocalDateTime.now();
    }

    /**
     * Constructor with required fields for business logic initialization.
     * 
     * @param transactionId The unique transaction identifier (16 characters)
     * @param transactionType The transaction type enum
     * @param categoryCode The transaction category enum
     * @param amount The transaction amount with BigDecimal precision
     * @param cardNumber The credit card number (16 digits)
     */
    public Transaction(String transactionId, TransactionType transactionType, 
                      TransactionCategory categoryCode, BigDecimal amount, String cardNumber) {
        this();
        this.transactionId = transactionId;
        this.transactionType = transactionType;
        this.categoryCode = categoryCode;
        this.amount = amount != null ? BigDecimalUtils.roundToMonetary(amount) : null;
        this.cardNumber = cardNumber;
    }

    /**
     * Comprehensive constructor with all transaction details.
     * 
     * @param transactionId The unique transaction identifier
     * @param transactionType The transaction type enum
     * @param categoryCode The transaction category enum
     * @param amount The transaction amount
     * @param description The transaction description
     * @param cardNumber The credit card number
     * @param merchantId The merchant identifier
     * @param merchantName The merchant name
     * @param merchantCity The merchant city
     * @param merchantZip The merchant ZIP code
     * @param source The transaction source
     */
    public Transaction(String transactionId, TransactionType transactionType, 
                      TransactionCategory categoryCode, BigDecimal amount, String description,
                      String cardNumber, String merchantId, String merchantName, 
                      String merchantCity, String merchantZip, String source) {
        this(transactionId, transactionType, categoryCode, amount, cardNumber);
        this.description = description;
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.merchantCity = merchantCity;
        this.merchantZip = merchantZip;
        this.source = source;
    }

    /**
     * Pre-update callback for automatic timestamp management.
     * Updates processing timestamp when transaction is modified.
     */
    @PreUpdate
    protected void onUpdate() {
        this.processingTimestamp = LocalDateTime.now();
    }

    /**
     * Pre-persist callback for automatic timestamp management.
     * Sets processing timestamp when transaction is first saved.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.originalTimestamp == null) {
            this.originalTimestamp = now;
        }
        this.processingTimestamp = now;
    }

    // Getter and Setter methods as required by the exports schema

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
     * Gets the transaction type enum.
     * 
     * @return The TransactionType enum value
     */
    public TransactionType getTransactionType() {
        return transactionType;
    }

    /**
     * Sets the transaction type enum.
     * 
     * @param transactionType The TransactionType enum (not null)
     */
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * Gets the transaction category enum.
     * 
     * @return The TransactionCategory enum value
     */
    public TransactionCategory getCategoryCode() {
        return categoryCode;
    }

    /**
     * Sets the transaction category enum.
     * 
     * @param categoryCode The TransactionCategory enum (not null)
     */
    public void setCategoryCode(TransactionCategory categoryCode) {
        this.categoryCode = categoryCode;
    }

    /**
     * Gets the transaction amount with BigDecimal precision.
     * 
     * @return The transaction amount as BigDecimal with DECIMAL(11,2) precision
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Sets the transaction amount using BigDecimal for precise financial calculations.
     * Maintains COBOL COMP-3 precision equivalent using BigDecimalUtils.
     * 
     * @param amount The transaction amount (-999999999.99 to +999999999.99)
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount != null ? BigDecimalUtils.roundToMonetary(amount) : null;
    }

    /**
     * Gets the transaction description.
     * 
     * @return The transaction description (up to 100 characters)
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the transaction description.
     * 
     * @param description The transaction description (up to 100 characters)
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the credit card number.
     * 
     * @return The 16-digit card number
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the credit card number with validation.
     * 
     * @param cardNumber The 16-digit card number (not null, Luhn algorithm validated)
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Gets the merchant identifier.
     * 
     * @return The merchant ID (up to 9 digits)
     */
    public String getMerchantId() {
        return merchantId;
    }

    /**
     * Sets the merchant identifier.
     * 
     * @param merchantId The merchant ID (up to 9 digits)
     */
    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    /**
     * Gets the merchant name.
     * 
     * @return The merchant name (up to 50 characters)
     */
    public String getMerchantName() {
        return merchantName;
    }

    /**
     * Sets the merchant name.
     * 
     * @param merchantName The merchant name (up to 50 characters)
     */
    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    /**
     * Gets the merchant city.
     * 
     * @return The merchant city (up to 50 characters)
     */
    public String getMerchantCity() {
        return merchantCity;
    }

    /**
     * Sets the merchant city.
     * 
     * @param merchantCity The merchant city (up to 50 characters)
     */
    public void setMerchantCity(String merchantCity) {
        this.merchantCity = merchantCity;
    }

    /**
     * Gets the merchant ZIP code.
     * 
     * @return The merchant ZIP code (up to 10 characters)
     */
    public String getMerchantZip() {
        return merchantZip;
    }

    /**
     * Sets the merchant ZIP code.
     * 
     * @param merchantZip The merchant ZIP code (up to 10 characters)
     */
    public void setMerchantZip(String merchantZip) {
        this.merchantZip = merchantZip;
    }

    /**
     * Gets the original transaction timestamp.
     * 
     * @return The original timestamp when transaction was initiated
     */
    public LocalDateTime getOriginalTimestamp() {
        return originalTimestamp;
    }

    /**
     * Sets the original transaction timestamp.
     * 
     * @param originalTimestamp The original timestamp (not null)
     */
    public void setOriginalTimestamp(LocalDateTime originalTimestamp) {
        this.originalTimestamp = originalTimestamp;
    }

    /**
     * Gets the processing timestamp.
     * 
     * @return The processing timestamp when transaction was processed
     */
    public LocalDateTime getProcessingTimestamp() {
        return processingTimestamp;
    }

    /**
     * Sets the processing timestamp.
     * 
     * @param processingTimestamp The processing timestamp
     */
    public void setProcessingTimestamp(LocalDateTime processingTimestamp) {
        this.processingTimestamp = processingTimestamp;
    }

    /**
     * Gets the transaction source.
     * 
     * @return The transaction source (up to 10 characters)
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets the transaction source.
     * 
     * @param source The transaction source (up to 10 characters)
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Gets the associated account entity.
     * 
     * @return The Account entity for this transaction
     */
    public Account getAccount() {
        return account;
    }

    /**
     * Sets the associated account entity.
     * 
     * @param account The Account entity for balance management
     */
    public void setAccount(Account account) {
        this.account = account;
    }

    /**
     * Gets the associated card entity.
     * 
     * @return The Card entity for this transaction
     */
    public Card getCard() {
        return card;
    }

    /**
     * Sets the associated card entity.
     * 
     * @param card The Card entity for transaction authorization
     */
    public void setCard(Card card) {
        this.card = card;
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

    // Business logic methods

    /**
     * Checks if this transaction is a debit transaction.
     * Uses TransactionType enum to determine debit/credit nature.
     * 
     * @return true if transaction is a debit (increases balance owed)
     */
    public boolean isDebit() {
        return transactionType != null && transactionType.isDebit();
    }

    /**
     * Checks if this transaction is a credit transaction.
     * Uses TransactionType enum to determine debit/credit nature.
     * 
     * @return true if transaction is a credit (decreases balance owed)
     */
    public boolean isCredit() {
        return transactionType != null && transactionType.isCredit();
    }

    /**
     * Gets the absolute amount of the transaction.
     * Uses BigDecimalUtils for exact precision operations.
     * 
     * @return The absolute value of the transaction amount
     */
    public BigDecimal getAbsoluteAmount() {
        return amount != null ? BigDecimalUtils.abs(amount) : BigDecimalUtils.ZERO_MONETARY;
    }

    /**
     * Formats the transaction amount as currency string.
     * Uses BigDecimalUtils for consistent currency formatting.
     * 
     * @return Formatted currency string (e.g., "$1,234.56")
     */
    public String getFormattedAmount() {
        return amount != null ? BigDecimalUtils.formatCurrency(amount) : "$0.00";
    }

    /**
     * Checks if the transaction is processed (has processing timestamp).
     * 
     * @return true if transaction has been processed
     */
    public boolean isProcessed() {
        return processingTimestamp != null;
    }

    /**
     * Gets the account ID from the associated account.
     * Provides direct access to account identifier for performance optimization.
     * 
     * @return The account ID if account is set, null otherwise
     */
    public String getAccountId() {
        return account != null ? account.getAccountId() : null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Transaction that = (Transaction) obj;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId='" + transactionId + '\'' +
                ", transactionType=" + transactionType +
                ", categoryCode=" + categoryCode +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", cardNumber='" + cardNumber + '\'' +
                ", merchantId='" + merchantId + '\'' +
                ", merchantName='" + merchantName + '\'' +
                ", merchantCity='" + merchantCity + '\'' +
                ", merchantZip='" + merchantZip + '\'' +
                ", originalTimestamp=" + originalTimestamp +
                ", processingTimestamp=" + processingTimestamp +
                ", source='" + source + '\'' +
                ", accountId='" + getAccountId() + '\'' +
                ", version=" + version +
                '}';
    }
}