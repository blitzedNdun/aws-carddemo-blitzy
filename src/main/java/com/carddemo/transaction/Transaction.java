package com.carddemo.transaction;

import com.carddemo.common.enums.TransactionType;
import com.carddemo.common.enums.TransactionCategory;
import com.carddemo.common.validator.ValidCardNumber;
import com.carddemo.common.validator.ValidCurrency;
import com.carddemo.common.entity.Account;
import com.carddemo.common.entity.Card;
import com.carddemo.common.util.BigDecimalUtils;

import jakarta.persistence.*;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * JPA Transaction entity representing transaction records with precise BigDecimal financial 
 * data handling and comprehensive database relationship mapping.
 * 
 * This entity maps PostgreSQL transactions table structure exactly preserving COBOL TRAN-RECORD 
 * field definitions and data types while ensuring BigDecimal precision for financial amounts 
 * maintains exact arithmetic equivalent to COBOL packed decimal calculations.
 * 
 * Key Features:
 * - Exact COBOL TRAN-RECORD structure mapping from CVTRA05Y.cpy (350-byte record)
 * - BigDecimal DECIMAL(12,2) precision for TRAN-AMT field equivalent to COBOL S9(09)V99 COMP-3
 * - JPA entity mapping for complex queries and relationship navigation across normalized database schema
 * - Comprehensive validation using Jakarta Bean Validation with custom validators
 * - Jackson JSON serialization for REST API response formatting with proper date and decimal handling
 * - Audit trail fields for transaction creation and modification timestamps with automatic population
 * - Foreign key relationships to Account and Card entities with lazy loading optimization
 * 
 * Database Mapping:
 * - PostgreSQL table: transactions
 * - Primary key: transaction_id (VARCHAR(16) mapped from TRAN-ID PIC X(16))
 * - Foreign keys: account_id, card_number for entity relationships
 * - Indexed fields: transaction_type, category_code, processing_timestamp for query optimization
 * 
 * COBOL Field Mappings:
 * - TRAN-ID PIC X(16) → transaction_id VARCHAR(16)
 * - TRAN-TYPE-CD PIC X(02) → transaction_type VARCHAR(2) with TransactionType enum
 * - TRAN-CAT-CD PIC 9(04) → category_code VARCHAR(4) with TransactionCategory enum
 * - TRAN-SOURCE PIC X(10) → source VARCHAR(10)
 * - TRAN-DESC PIC X(100) → description VARCHAR(100)
 * - TRAN-AMT PIC S9(09)V99 → amount DECIMAL(12,2) with BigDecimal precision
 * - TRAN-MERCHANT-ID PIC 9(09) → merchant_id VARCHAR(9)
 * - TRAN-MERCHANT-NAME PIC X(50) → merchant_name VARCHAR(50)
 * - TRAN-MERCHANT-CITY PIC X(50) → merchant_city VARCHAR(50)
 * - TRAN-MERCHANT-ZIP PIC X(10) → merchant_zip VARCHAR(10)
 * - TRAN-CARD-NUM PIC X(16) → card_number VARCHAR(16) with Luhn validation
 * - TRAN-ORIG-TS PIC X(26) → original_timestamp TIMESTAMP
 * - TRAN-PROC-TS PIC X(26) → processing_timestamp TIMESTAMP
 * 
 * Performance Optimizations:
 * - Lazy loading for Account and Card relationships
 * - Composite indexes on (card_number, processing_timestamp)
 * - Partition-ready design for large transaction volumes
 * - Optimized query patterns for transaction processing workflows
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since CardDemo v1.0-15-g27d6c6f-68
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transactions_card_number", columnList = "card_number"),
    @Index(name = "idx_transactions_processing_timestamp", columnList = "processing_timestamp"),
    @Index(name = "idx_transactions_type_category", columnList = "transaction_type, category_code")
})
public class Transaction implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Transaction identifier - Primary key mapping TRAN-ID PIC X(16)
     * Maps to PostgreSQL VARCHAR(16) transaction_id primary key
     * Range: 16-character alphanumeric transaction identifier
     */
    @Id
    @Column(name = "transaction_id", length = 16, nullable = false)
    @NotNull(message = "Transaction ID cannot be null")
    @Size(min = 16, max = 16, message = "Transaction ID must be exactly 16 characters")
    @Pattern(regexp = "^[A-Za-z0-9]{16}$", message = "Transaction ID must be 16 alphanumeric characters")
    @JsonProperty("transactionId")
    private String transactionId;
    
    /**
     * Transaction type code - Maps TRAN-TYPE-CD PIC X(02)
     * Maps to PostgreSQL VARCHAR(2) with TransactionType enum validation
     * Valid values defined in TransactionType enum (01-30)
     */
    @Column(name = "transaction_type", length = 2, nullable = false)
    @NotNull(message = "Transaction type cannot be null")
    @Size(min = 2, max = 2, message = "Transaction type must be exactly 2 characters")
    @Pattern(regexp = "\\d{2}", message = "Transaction type must be 2 digits")
    @JsonProperty("transactionType")
    private String transactionType;
    
    /**
     * Transaction category code - Maps TRAN-CAT-CD PIC 9(04)
     * Maps to PostgreSQL VARCHAR(4) with TransactionCategory enum validation
     * Valid values defined in TransactionCategory enum (0001-9999)
     */
    @Column(name = "category_code", length = 4, nullable = false)
    @NotNull(message = "Category code cannot be null")
    @Size(min = 4, max = 4, message = "Category code must be exactly 4 characters")
    @Pattern(regexp = "\\d{4}", message = "Category code must be 4 digits")
    @JsonProperty("categoryCode")
    private String categoryCode;
    
    /**
     * Transaction source - Maps TRAN-SOURCE PIC X(10)
     * Maps to PostgreSQL VARCHAR(10) for transaction source identification
     * Examples: "ONLINE", "ATM", "POS", "PHONE", "MOBILE"
     */
    @Column(name = "source", length = 10, nullable = false)
    @NotNull(message = "Transaction source cannot be null")
    @Size(min = 1, max = 10, message = "Transaction source must be between 1 and 10 characters")
    @JsonProperty("source")
    private String source;
    
    /**
     * Transaction description - Maps TRAN-DESC PIC X(100)
     * Maps to PostgreSQL VARCHAR(100) for transaction description
     * Contains merchant name, transaction details, or system-generated description
     */
    @Column(name = "description", length = 100, nullable = false)
    @NotNull(message = "Transaction description cannot be null")
    @Size(min = 1, max = 100, message = "Transaction description must be between 1 and 100 characters")
    @JsonProperty("description")
    private String description;
    
    /**
     * Transaction amount - Maps TRAN-AMT PIC S9(09)V99
     * Maps to PostgreSQL DECIMAL(12,2) using BigDecimal for exact precision
     * Maintains COBOL COMP-3 decimal precision with DECIMAL128 context
     * Range: -999999999.99 to 999999999.99
     */
    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Transaction amount cannot be null")
    @ValidCurrency(
        min = "-999999999.99", 
        max = "999999999.99",
        message = "Transaction amount must be between -999999999.99 and 999999999.99"
    )
    @JsonProperty("amount")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;
    
    /**
     * Merchant identifier - Maps TRAN-MERCHANT-ID PIC 9(09)
     * Maps to PostgreSQL VARCHAR(9) for merchant identification
     * Range: 9-digit numeric merchant ID
     */
    @Column(name = "merchant_id", length = 9)
    @Size(max = 9, message = "Merchant ID cannot exceed 9 characters")
    @Pattern(regexp = "\\d{0,9}", message = "Merchant ID must be numeric")
    @JsonProperty("merchantId")
    private String merchantId;
    
    /**
     * Merchant name - Maps TRAN-MERCHANT-NAME PIC X(50)
     * Maps to PostgreSQL VARCHAR(50) for merchant name
     * Contains the business name of the merchant
     */
    @Column(name = "merchant_name", length = 50)
    @Size(max = 50, message = "Merchant name cannot exceed 50 characters")
    @JsonProperty("merchantName")
    private String merchantName;
    
    /**
     * Merchant city - Maps TRAN-MERCHANT-CITY PIC X(50)
     * Maps to PostgreSQL VARCHAR(50) for merchant city
     * Contains the city location of the merchant
     */
    @Column(name = "merchant_city", length = 50)
    @Size(max = 50, message = "Merchant city cannot exceed 50 characters")
    @JsonProperty("merchantCity")
    private String merchantCity;
    
    /**
     * Merchant ZIP code - Maps TRAN-MERCHANT-ZIP PIC X(10)
     * Maps to PostgreSQL VARCHAR(10) for merchant ZIP code
     * Contains the ZIP/postal code of the merchant
     */
    @Column(name = "merchant_zip", length = 10)
    @Size(max = 10, message = "Merchant ZIP cannot exceed 10 characters")
    @JsonProperty("merchantZip")
    private String merchantZip;
    
    /**
     * Card number - Maps TRAN-CARD-NUM PIC X(16)
     * Maps to PostgreSQL VARCHAR(16) with Luhn algorithm validation
     * Foreign key reference to cards table
     */
    @Column(name = "card_number", length = 16, nullable = false)
    @NotNull(message = "Card number cannot be null")
    @ValidCardNumber(message = "Card number must be a valid 16-digit credit card number")
    @JsonProperty("cardNumber")
    private String cardNumber;
    
    /**
     * Original timestamp - Maps TRAN-ORIG-TS PIC X(26)
     * Maps to PostgreSQL TIMESTAMP for transaction original timestamp
     * Records when the transaction was originally initiated
     */
    @Column(name = "original_timestamp", nullable = false)
    @NotNull(message = "Original timestamp cannot be null")
    @JsonProperty("originalTimestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime originalTimestamp;
    
    /**
     * Processing timestamp - Maps TRAN-PROC-TS PIC X(26)
     * Maps to PostgreSQL TIMESTAMP for transaction processing timestamp
     * Records when the transaction was processed by the system
     */
    @Column(name = "processing_timestamp", nullable = false)
    @NotNull(message = "Processing timestamp cannot be null")
    @JsonProperty("processingTimestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processingTimestamp;
    
    /**
     * Many-to-one relationship with Account entity
     * Foreign key relationship derived from card-to-account mapping
     * Enables account-based transaction lookups and balance updates
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id")
    private Account account;
    
    /**
     * Many-to-one relationship with Card entity
     * Foreign key relationship for card-based transaction lookups
     * Enables card authorization validation through transaction processing
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_number", referencedColumnName = "card_number", insertable = false, updatable = false)
    private Card card;
    
    /**
     * Audit trail creation timestamp
     * Automatically populated when transaction record is created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty("createdAt")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * Audit trail modification timestamp
     * Automatically updated when transaction record is modified
     */
    @Column(name = "updated_at", nullable = false)
    @JsonProperty("updatedAt")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    /**
     * Optimistic locking version for concurrent transaction operations
     * Prevents lost updates in multi-user transaction processing scenarios
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
    
    /**
     * Default constructor for JPA
     */
    public Transaction() {
        this.version = 0L;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Constructor with essential transaction fields
     * 
     * @param transactionId 16-character transaction identifier
     * @param transactionType 2-character transaction type code
     * @param categoryCode 4-character transaction category code
     * @param source Transaction source identifier
     * @param description Transaction description
     * @param amount Transaction amount with BigDecimal precision
     * @param cardNumber 16-digit card number
     * @param originalTimestamp Original transaction timestamp
     * @param processingTimestamp Processing timestamp
     */
    public Transaction(String transactionId, String transactionType, String categoryCode,
                      String source, String description, BigDecimal amount, String cardNumber,
                      LocalDateTime originalTimestamp, LocalDateTime processingTimestamp) {
        this();
        this.transactionId = transactionId;
        this.transactionType = transactionType;
        this.categoryCode = categoryCode;
        this.source = source;
        this.description = description;
        this.amount = amount;
        this.cardNumber = cardNumber;
        this.originalTimestamp = originalTimestamp;
        this.processingTimestamp = processingTimestamp;
    }
    
    /**
     * JPA lifecycle callback for setting creation timestamp
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
    
    /**
     * JPA lifecycle callback for updating modification timestamp
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Gets the transaction identifier
     * 
     * @return Transaction ID as 16-character string
     */
    public String getTransactionId() {
        return transactionId;
    }
    
    /**
     * Sets the transaction identifier
     * 
     * @param transactionId Transaction ID as 16-character string
     */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
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
     * @return Category code as 4-character string
     */
    public String getCategoryCode() {
        return categoryCode;
    }
    
    /**
     * Sets the transaction category code
     * 
     * @param categoryCode Category code as 4-character string
     */
    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }
    
    /**
     * Gets the transaction source
     * 
     * @return Transaction source identifier
     */
    public String getSource() {
        return source;
    }
    
    /**
     * Sets the transaction source
     * 
     * @param source Transaction source identifier
     */
    public void setSource(String source) {
        this.source = source;
    }
    
    /**
     * Gets the transaction description
     * 
     * @return Transaction description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Sets the transaction description
     * 
     * @param description Transaction description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Gets the transaction amount
     * 
     * @return Transaction amount as BigDecimal with exact precision
     */
    public BigDecimal getAmount() {
        return amount;
    }
    
    /**
     * Sets the transaction amount
     * 
     * @param amount Transaction amount as BigDecimal with exact precision
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    /**
     * Gets the merchant identifier
     * 
     * @return Merchant ID as 9-character string
     */
    public String getMerchantId() {
        return merchantId;
    }
    
    /**
     * Sets the merchant identifier
     * 
     * @param merchantId Merchant ID as 9-character string
     */
    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }
    
    /**
     * Gets the merchant name
     * 
     * @return Merchant name
     */
    public String getMerchantName() {
        return merchantName;
    }
    
    /**
     * Sets the merchant name
     * 
     * @param merchantName Merchant name
     */
    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }
    
    /**
     * Gets the merchant city
     * 
     * @return Merchant city
     */
    public String getMerchantCity() {
        return merchantCity;
    }
    
    /**
     * Sets the merchant city
     * 
     * @param merchantCity Merchant city
     */
    public void setMerchantCity(String merchantCity) {
        this.merchantCity = merchantCity;
    }
    
    /**
     * Gets the merchant ZIP code
     * 
     * @return Merchant ZIP code
     */
    public String getMerchantZip() {
        return merchantZip;
    }
    
    /**
     * Sets the merchant ZIP code
     * 
     * @param merchantZip Merchant ZIP code
     */
    public void setMerchantZip(String merchantZip) {
        this.merchantZip = merchantZip;
    }
    
    /**
     * Gets the card number
     * 
     * @return Card number as 16-character string
     */
    public String getCardNumber() {
        return cardNumber;
    }
    
    /**
     * Sets the card number
     * 
     * @param cardNumber Card number as 16-character string
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
    
    /**
     * Gets the original timestamp
     * 
     * @return Original transaction timestamp
     */
    public LocalDateTime getOriginalTimestamp() {
        return originalTimestamp;
    }
    
    /**
     * Sets the original timestamp
     * 
     * @param originalTimestamp Original transaction timestamp
     */
    public void setOriginalTimestamp(LocalDateTime originalTimestamp) {
        this.originalTimestamp = originalTimestamp;
    }
    
    /**
     * Gets the processing timestamp
     * 
     * @return Processing timestamp
     */
    public LocalDateTime getProcessingTimestamp() {
        return processingTimestamp;
    }
    
    /**
     * Sets the processing timestamp
     * 
     * @param processingTimestamp Processing timestamp
     */
    public void setProcessingTimestamp(LocalDateTime processingTimestamp) {
        this.processingTimestamp = processingTimestamp;
    }
    
    /**
     * Gets the associated account
     * 
     * @return Account entity
     */
    public Account getAccount() {
        return account;
    }
    
    /**
     * Sets the associated account
     * 
     * @param account Account entity
     */
    public void setAccount(Account account) {
        this.account = account;
    }
    
    /**
     * Gets the associated card
     * 
     * @return Card entity
     */
    public Card getCard() {
        return card;
    }
    
    /**
     * Sets the associated card
     * 
     * @param card Card entity
     */
    public void setCard(Card card) {
        this.card = card;
    }
    
    /**
     * Gets the creation timestamp
     * 
     * @return Creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Gets the last update timestamp
     * 
     * @return Last update timestamp
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    /**
     * Gets the version for optimistic locking
     * 
     * @return Version number
     */
    public Long getVersion() {
        return version;
    }
    
    /**
     * Sets the version for optimistic locking
     * 
     * @param version Version number
     */
    public void setVersion(Long version) {
        this.version = version;
    }
    
    /**
     * Gets the TransactionType enum value from the transaction type code
     * 
     * @return TransactionType enum value or null if invalid
     */
    public TransactionType getTransactionTypeEnum() {
        return TransactionType.fromCode(this.transactionType).orElse(null);
    }
    
    /**
     * Gets the TransactionCategory enum value from the category code
     * 
     * @return TransactionCategory enum value or null if invalid
     */
    public TransactionCategory getCategoryEnum() {
        return TransactionCategory.fromCode(this.categoryCode).orElse(null);
    }
    
    /**
     * Checks if this transaction is a debit (increases account balance)
     * 
     * @return true if transaction is a debit, false otherwise
     */
    public boolean isDebit() {
        TransactionType type = getTransactionTypeEnum();
        return type != null && type.isDebit();
    }
    
    /**
     * Checks if this transaction is a credit (decreases account balance)
     * 
     * @return true if transaction is a credit, false otherwise
     */
    public boolean isCredit() {
        TransactionType type = getTransactionTypeEnum();
        return type != null && type.isCredit();
    }
    
    /**
     * Gets the formatted transaction amount as currency string
     * 
     * @return Formatted currency string using BigDecimalUtils
     */
    public String getFormattedAmount() {
        return amount != null ? BigDecimalUtils.formatCurrency(amount) : "$0.00";
    }
    
    /**
     * Calculates the signed amount based on transaction type
     * 
     * @return Positive amount for debits, negative for credits
     */
    public BigDecimal getSignedAmount() {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        
        TransactionType type = getTransactionTypeEnum();
        if (type != null && type.isCredit()) {
            return amount.negate();
        }
        
        return amount;
    }
    
    /**
     * Validates the transaction using business rules
     * 
     * @return true if transaction is valid, false otherwise
     */
    public boolean isValid() {
        return transactionId != null && !transactionId.trim().isEmpty() &&
               TransactionType.isValidCode(transactionType) &&
               TransactionCategory.isValidCode(categoryCode) &&
               amount != null &&
               cardNumber != null && cardNumber.length() == 16 &&
               originalTimestamp != null &&
               processingTimestamp != null;
    }
    
    /**
     * Compares this transaction with another transaction for equality
     * 
     * @param obj Object to compare with
     * @return true if transactions are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Transaction that = (Transaction) obj;
        return Objects.equals(transactionId, that.transactionId);
    }
    
    /**
     * Generates hash code for this transaction
     * 
     * @return Hash code based on transaction ID
     */
    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }
    
    /**
     * Returns string representation of the transaction
     * 
     * @return String representation with key transaction details
     */
    @Override
    public String toString() {
        return String.format(
            "Transaction{id='%s', type='%s', category='%s', amount=%s, cardNumber='%s', processingTimestamp=%s}",
            transactionId, transactionType, categoryCode, 
            getFormattedAmount(), cardNumber, processingTimestamp
        );
    }
}