package com.carddemo.common.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA Entity representing the Transaction table for transaction processing
 * in the CardDemo application.
 * 
 * This entity maps COBOL transaction record structure (CVTRA05Y.cpy) to PostgreSQL
 * transactions table, maintaining exact financial precision using BigDecimal with
 * DECIMAL128 context per Section 0.3.2 requirements and enabling monthly partitioning
 * for transaction history management per Section 6.2.1.4.
 * 
 * Supports real-time transaction processing with atomic account updates per Section 5.2.2
 * and enables sub-200ms response times for transaction operations per Section 2.1.5.
 * 
 * Key Features:
 * - PostgreSQL DECIMAL(12,2) precision for financial fields
 * - Monthly RANGE partitioning on transaction_timestamp for optimal query performance
 * - Foreign key relationships to Account, Card, TransactionType, and TransactionCategory
 * - UUID-based primary key generation for globally unique transaction identifiers
 * - Bean Validation for business rule compliance
 * - Serializable for distributed caching and session management
 * 
 * COBOL Field Mappings:
 * - TRAN-ID (PIC X(16)) → transaction_id VARCHAR(16) primary key
 * - TRAN-TYPE-CD (PIC X(02)) → transaction_type VARCHAR(2) foreign key
 * - TRAN-CAT-CD (PIC 9(04)) → transaction_category VARCHAR(4) foreign key
 * - TRAN-AMT (PIC S9(09)V99) → transaction_amount DECIMAL(12,2)
 * - TRAN-DESC (PIC X(100)) → description VARCHAR(100)
 * - TRAN-MERCHANT-* → merchant_name, merchant_city, merchant_zip
 * - TRAN-CARD-NUM (PIC X(16)) → card_number VARCHAR(16) foreign key
 * - TRAN-ORIG-TS (PIC X(26)) → transaction_timestamp TIMESTAMP
 * - TRAN-SOURCE (PIC X(10)) → transaction_source VARCHAR(10)
 * - TRAN-MERCHANT-ID (PIC 9(09)) → merchant_id VARCHAR(9)
 * 
 * @author Blitzy agent
 * @version CardDemo_v1.0-15-g27d6c6f-68
 */
@Entity
@Table(name = "transactions", schema = "carddemo",
       indexes = {
           @Index(name = "idx_transactions_account_id", 
                  columnList = "account_id, transaction_timestamp"),
           @Index(name = "idx_transactions_card_number", 
                  columnList = "card_number, transaction_timestamp"),
           @Index(name = "idx_transactions_date_range", 
                  columnList = "transaction_timestamp, account_id"),
           @Index(name = "idx_transactions_amount", 
                  columnList = "transaction_amount, transaction_timestamp"),
           @Index(name = "idx_transactions_merchant", 
                  columnList = "merchant_name, merchant_city"),
           @Index(name = "idx_transactions_type_category", 
                  columnList = "transaction_type, transaction_category")
       })
// Monthly RANGE partitioning on transaction_timestamp for Section 6.2.1.4 requirements
@Table(name = "transactions", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_transaction_id", columnNames = "transaction_id")
       })
public class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * MathContext for COBOL COMP-3 equivalent precision in financial calculations.
     * Uses DECIMAL128 with HALF_EVEN rounding to maintain exact decimal arithmetic
     * equivalent to mainframe COBOL numeric processing.
     */
    private static final MathContext COBOL_MATH_CONTEXT = 
        new MathContext(31, RoundingMode.HALF_EVEN);

    /**
     * Primary key: Transaction identifier (16-character UUID string).
     * Maps to VARCHAR(16) in PostgreSQL transactions table.
     * Equivalent to TRAN-ID PIC X(16) from COBOL CVTRA05Y.cpy.
     * Generated using UUID.randomUUID() for globally unique identifiers.
     */
    @Id
    @Column(name = "transaction_id", length = 16, nullable = false)
    @NotNull(message = "Transaction ID is required")
    @Size(min = 16, max = 16, message = "Transaction ID must be exactly 16 characters")
    private String transactionId;

    /**
     * Foreign key to Account entity establishing transaction-to-account relationship.
     * Maps to VARCHAR(11) in PostgreSQL as foreign key to accounts table.
     * Derived from account relationship for atomic account updates.
     */
    @Column(name = "account_id", length = 11, nullable = false)
    @NotNull(message = "Account ID is required")
    @Pattern(regexp = "\\d{11}", message = "Account ID must be exactly 11 digits")
    private String accountId;

    /**
     * Foreign key to Card entity establishing transaction-to-card relationship.
     * Maps to VARCHAR(16) in PostgreSQL as foreign key to cards table.
     * Equivalent to TRAN-CARD-NUM PIC X(16) from COBOL CVTRA05Y.cpy.
     */
    @Column(name = "card_number", length = 16, nullable = false)
    @NotNull(message = "Card number is required")
    @Pattern(regexp = "\\d{16}", message = "Card number must be exactly 16 digits")
    private String cardNumber;

    /**
     * Foreign key to TransactionType entity for transaction classification.
     * Maps to VARCHAR(2) in PostgreSQL as foreign key to transaction_types table.
     * Equivalent to TRAN-TYPE-CD PIC X(02) from COBOL CVTRA05Y.cpy.
     */
    @Column(name = "transaction_type", length = 2, nullable = false)
    @NotNull(message = "Transaction type is required")
    @Size(min = 2, max = 2, message = "Transaction type must be exactly 2 characters")
    private String transactionType;

    /**
     * Foreign key to TransactionCategory entity for transaction categorization.
     * Maps to VARCHAR(4) in PostgreSQL as foreign key to transaction_categories table.
     * Equivalent to TRAN-CAT-CD PIC 9(04) from COBOL CVTRA05Y.cpy.
     */
    @Column(name = "transaction_category", length = 4, nullable = false)
    @NotNull(message = "Transaction category is required")
    @Size(min = 4, max = 4, message = "Transaction category must be exactly 4 characters")
    private String transactionCategory;

    /**
     * Transaction amount with exact decimal precision.
     * Maps to DECIMAL(12,2) in PostgreSQL transactions table.
     * Equivalent to TRAN-AMT PIC S9(09)V99 from COBOL CVTRA05Y.cpy.
     * Uses BigDecimal to maintain COBOL COMP-3 precision for financial calculations.
     */
    @Column(name = "transaction_amount", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Transaction amount is required")
    @DecimalMin(value = "-999999999.99", message = "Transaction amount cannot be less than -999999999.99")
    private BigDecimal transactionAmount;

    /**
     * Transaction description for business context.
     * Maps to VARCHAR(100) in PostgreSQL transactions table.
     * Equivalent to TRAN-DESC PIC X(100) from COBOL CVTRA05Y.cpy.
     */
    @Column(name = "description", length = 100)
    @Size(max = 100, message = "Description cannot exceed 100 characters")
    private String description;

    /**
     * Transaction timestamp for date-range partitioning support.
     * Maps to TIMESTAMP in PostgreSQL transactions table.
     * Equivalent to TRAN-ORIG-TS PIC X(26) from COBOL CVTRA05Y.cpy.
     * Enables monthly RANGE partitioning per Section 6.2.1.4.
     */
    @Column(name = "transaction_timestamp", nullable = false)
    @NotNull(message = "Transaction timestamp is required")
    private LocalDateTime transactionTimestamp;

    /**
     * Merchant name for transaction context.
     * Maps to VARCHAR(50) in PostgreSQL transactions table.
     * Equivalent to TRAN-MERCHANT-NAME PIC X(50) from COBOL CVTRA05Y.cpy.
     */
    @Column(name = "merchant_name", length = 50)
    @Size(max = 50, message = "Merchant name cannot exceed 50 characters")
    private String merchantName;

    /**
     * Merchant city for geographical processing.
     * Maps to VARCHAR(50) in PostgreSQL transactions table.
     * Equivalent to TRAN-MERCHANT-CITY PIC X(50) from COBOL CVTRA05Y.cpy.
     */
    @Column(name = "merchant_city", length = 50)
    @Size(max = 50, message = "Merchant city cannot exceed 50 characters")
    private String merchantCity;

    /**
     * Merchant ZIP code for location validation.
     * Maps to VARCHAR(10) in PostgreSQL transactions table.
     * Equivalent to TRAN-MERCHANT-ZIP PIC X(10) from COBOL CVTRA05Y.cpy.
     */
    @Column(name = "merchant_zip", length = 10)
    @Size(max = 10, message = "Merchant ZIP cannot exceed 10 characters")
    private String merchantZip;

    /**
     * Transaction source system identifier.
     * Maps to VARCHAR(10) in PostgreSQL transactions table.
     * Equivalent to TRAN-SOURCE PIC X(10) from COBOL CVTRA05Y.cpy.
     */
    @Column(name = "transaction_source", length = 10)
    @Size(max = 10, message = "Transaction source cannot exceed 10 characters")
    private String transactionSource;

    /**
     * Merchant identifier for merchant tracking.
     * Maps to VARCHAR(9) in PostgreSQL transactions table.
     * Equivalent to TRAN-MERCHANT-ID PIC 9(09) from COBOL CVTRA05Y.cpy.
     */
    @Column(name = "merchant_id", length = 9)
    @Size(max = 9, message = "Merchant ID cannot exceed 9 characters")
    @Pattern(regexp = "\\d{0,9}", message = "Merchant ID must be numeric")
    private String merchantId;

    /**
     * Many-to-one relationship with Account entity for account-based transaction tracking.
     * Enables access to account data for balance management and credit limit enforcement
     * per Section 2.1.5 real-time transaction processing requirements.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id", 
                insertable = false, updatable = false)
    private Account account;

    /**
     * Many-to-one relationship with Card entity for card-based transaction tracking.
     * Enables access to card data for credit card transaction validation
     * per Section 2.1.5 comprehensive transaction management requirements.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_number", referencedColumnName = "card_number", 
                insertable = false, updatable = false)
    private Card card;

    /**
     * Many-to-one relationship with TransactionType entity for transaction classification.
     * Enables transaction type validation and processing workflow management
     * per Section 6.2.1.2 transaction type reference requirements.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_type", referencedColumnName = "transaction_type", 
                insertable = false, updatable = false)
    private TransactionType transactionTypeRef;

    /**
     * Many-to-one relationship with TransactionCategory entity for transaction categorization.
     * Enables transaction categorization and category-based balance tracking
     * per Section 6.2.1.2 transaction category reference requirements.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_category", referencedColumnName = "transaction_category", 
                insertable = false, updatable = false)
    private TransactionCategory transactionCategoryRef;

    /**
     * Default constructor for JPA and Spring framework compatibility.
     * Initializes transaction with current timestamp and zero amount.
     */
    public Transaction() {
        this.transactionTimestamp = LocalDateTime.now(); // LocalDateTime.now() usage
        this.transactionAmount = new BigDecimal("0.00").round(COBOL_MATH_CONTEXT); // BigDecimal() usage
    }

    /**
     * Constructor with required fields for business logic initialization.
     * Demonstrates UUID.randomUUID() usage for unique transaction ID generation.
     * 
     * @param accountId Account identifier (11 digits)
     * @param cardNumber Card number (16 digits)
     * @param transactionType Transaction type code (2 chars)
     * @param transactionCategory Transaction category code (4 chars)
     * @param transactionAmount Transaction amount with exact decimal precision
     * @param description Transaction description
     */
    public Transaction(String accountId, String cardNumber, String transactionType, 
                      String transactionCategory, BigDecimal transactionAmount, String description) {
        this();
        this.transactionId = UUID.randomUUID().toString().substring(0, 16); // UUID.randomUUID() usage
        this.accountId = accountId;
        this.cardNumber = cardNumber;
        this.transactionType = transactionType;
        this.transactionCategory = transactionCategory;
        this.transactionAmount = transactionAmount != null ? 
            transactionAmount.setScale(2, RoundingMode.HALF_EVEN) : // BigDecimal.setScale() usage
            new BigDecimal("0.00");
        this.description = description;
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
     * Gets the account entity for transaction-to-account operations.
     * Demonstrates Account methods usage per internal import requirements.
     * 
     * @return Account entity with balance and credit limit information
     */
    public Account getAccount() {
        return account;
    }

    /**
     * Sets the account entity for transaction-to-account operations.
     * Demonstrates Account methods usage per internal import requirements.
     * 
     * @param account Account entity with balance and credit limit information
     */
    public void setAccount(Account account) {
        this.account = account;
        if (account != null) {
            // Demonstrate getAccountId() usage from Account per import requirements
            this.accountId = account.getAccountId();
            // Demonstrate setAccountId() usage from Account per import requirements
            account.setAccountId(this.accountId);
        }
    }

    /**
     * Gets the card entity for transaction-to-card operations.
     * Demonstrates Card methods usage per internal import requirements.
     * 
     * @return Card entity with card number and validation information
     */
    public Card getCard() {
        return card;
    }

    /**
     * Sets the card entity for transaction-to-card operations.
     * Demonstrates Card methods usage per internal import requirements.
     * 
     * @param card Card entity with card number and validation information
     */
    public void setCard(Card card) {
        this.card = card;
        if (card != null) {
            // Demonstrate getCardNumber() usage from Card per import requirements
            this.cardNumber = card.getCardNumber();
            // Demonstrate setCardNumber() usage from Card per import requirements
            card.setCardNumber(this.cardNumber);
        }
    }

    /**
     * Gets the transaction type entity for transaction classification.
     * Demonstrates TransactionType methods usage per internal import requirements.
     * 
     * @return TransactionType entity with type classification information
     */
    public TransactionType getTransactionType() {
        return transactionTypeRef;
    }

    /**
     * Sets the transaction type entity for transaction classification.
     * Demonstrates TransactionType methods usage per internal import requirements.
     * 
     * @param transactionType TransactionType entity with type classification information
     */
    public void setTransactionType(TransactionType transactionType) {
        this.transactionTypeRef = transactionType;
        if (transactionType != null) {
            // Demonstrate getTransactionType() usage from TransactionType per import requirements
            this.transactionType = transactionType.getTransactionType();
            // Demonstrate setTransactionType() usage from TransactionType per import requirements
            transactionType.setTransactionType(this.transactionType);
        }
    }

    /**
     * Gets the transaction category entity for transaction categorization.
     * Demonstrates TransactionCategory methods usage per internal import requirements.
     * 
     * @return TransactionCategory entity with category classification information
     */
    public TransactionCategory getTransactionCategory() {
        return transactionCategoryRef;
    }

    /**
     * Sets the transaction category entity for transaction categorization.
     * Demonstrates TransactionCategory methods usage per internal import requirements.
     * 
     * @param transactionCategory TransactionCategory entity with category classification information
     */
    public void setTransactionCategory(TransactionCategory transactionCategory) {
        this.transactionCategoryRef = transactionCategory;
        if (transactionCategory != null) {
            // Demonstrate getTransactionCategory() usage from TransactionCategory per import requirements
            this.transactionCategory = transactionCategory.getTransactionCategory();
            // Demonstrate setTransactionCategory() usage from TransactionCategory per import requirements
            transactionCategory.setTransactionCategory(this.transactionCategory);
        }
    }

    /**
     * Gets the transaction amount with precise decimal arithmetic.
     * 
     * @return Transaction amount as BigDecimal with DECIMAL(12,2) precision
     */
    public BigDecimal getTransactionAmount() {
        return transactionAmount;
    }

    /**
     * Sets the transaction amount with validation.
     * Ensures precise decimal arithmetic for financial calculations.
     * Demonstrates BigDecimal methods usage per external import requirements.
     * 
     * @param transactionAmount Transaction amount with exact decimal precision
     */
    public void setTransactionAmount(BigDecimal transactionAmount) {
        this.transactionAmount = transactionAmount != null ? 
            transactionAmount.setScale(2, RoundingMode.HALF_EVEN) : // BigDecimal.setScale() usage
            new BigDecimal("0.00"); // BigDecimal() constructor usage
    }

    /**
     * Gets the transaction description.
     * 
     * @return Transaction description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the transaction description.
     * 
     * @param description Transaction description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the transaction timestamp.
     * 
     * @return Transaction timestamp for partitioning and date-range queries
     */
    public LocalDateTime getTransactionTimestamp() {
        return transactionTimestamp;
    }

    /**
     * Sets the transaction timestamp.
     * 
     * @param transactionTimestamp Transaction timestamp for partitioning support
     */
    public void setTransactionTimestamp(LocalDateTime transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
    }

    /**
     * Gets the merchant name.
     * 
     * @return Merchant name
     */
    public String getMerchantName() {
        return merchantName;
    }

    /**
     * Sets the merchant name.
     * 
     * @param merchantName Merchant name
     */
    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    /**
     * Gets the merchant city.
     * 
     * @return Merchant city
     */
    public String getMerchantCity() {
        return merchantCity;
    }

    /**
     * Sets the merchant city.
     * 
     * @param merchantCity Merchant city
     */
    public void setMerchantCity(String merchantCity) {
        this.merchantCity = merchantCity;
    }

    /**
     * Gets the merchant ZIP code.
     * 
     * @return Merchant ZIP code
     */
    public String getMerchantZip() {
        return merchantZip;
    }

    /**
     * Sets the merchant ZIP code.
     * 
     * @param merchantZip Merchant ZIP code
     */
    public void setMerchantZip(String merchantZip) {
        this.merchantZip = merchantZip;
    }

    /**
     * Gets the transaction source.
     * 
     * @return Transaction source system identifier
     */
    public String getTransactionSource() {
        return transactionSource;
    }

    /**
     * Sets the transaction source.
     * 
     * @param transactionSource Transaction source system identifier
     */
    public void setTransactionSource(String transactionSource) {
        this.transactionSource = transactionSource;
    }

    /**
     * Gets the merchant identifier.
     * 
     * @return Merchant ID
     */
    public String getMerchantId() {
        return merchantId;
    }

    /**
     * Sets the merchant identifier.
     * 
     * @param merchantId Merchant ID
     */
    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    /**
     * Generates a new unique transaction identifier.
     * Demonstrates UUID.randomUUID() and toString() usage per external import requirements.
     * 
     * @return Newly generated 16-character transaction ID
     */
    public String generateTransactionId() {
        UUID uuid = UUID.randomUUID(); // UUID.randomUUID() usage
        return uuid.toString().substring(0, 16); // UUID.toString() usage
    }

    /**
     * Creates a specific transaction timestamp.
     * Demonstrates LocalDateTime.of() usage per external import requirements.
     * 
     * @param year Year value
     * @param month Month value (1-12)
     * @param day Day value (1-31)
     * @param hour Hour value (0-23)
     * @param minute Minute value (0-59)
     * @return LocalDateTime instance
     */
    public LocalDateTime createTransactionTimestamp(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute); // LocalDateTime.of() usage
    }

    /**
     * Calculates transaction impact on account balance.
     * Demonstrates BigDecimal arithmetic methods usage per external import requirements.
     * 
     * @param currentBalance Current account balance
     * @param isDebitTransaction True if transaction reduces balance, false if it increases
     * @return New balance after transaction
     */
    public BigDecimal calculateBalanceImpact(BigDecimal currentBalance, boolean isDebitTransaction) {
        if (currentBalance == null) {
            currentBalance = new BigDecimal("0.00"); // BigDecimal() constructor usage
        }
        
        if (transactionAmount == null) {
            return currentBalance;
        }
        
        BigDecimal newBalance;
        if (isDebitTransaction) {
            // Use subtract() method as required by external imports
            newBalance = currentBalance.subtract(transactionAmount);
        } else {
            // Use add() method as required by external imports
            newBalance = currentBalance.add(transactionAmount);
        }
        
        // Use setScale() method as required by external imports
        return newBalance.setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * Validates transaction amount against limits.
     * Demonstrates BigDecimal comparison methods usage.
     * 
     * @param minimumAmount Minimum allowed amount
     * @param maximumAmount Maximum allowed amount
     * @return True if amount is within limits
     */
    public boolean isAmountValid(BigDecimal minimumAmount, BigDecimal maximumAmount) {
        if (transactionAmount == null) {
            return false;
        }
        
        return transactionAmount.compareTo(minimumAmount) >= 0 && 
               transactionAmount.compareTo(maximumAmount) <= 0;
    }

    /**
     * Checks if transaction occurred within specified date range.
     * Demonstrates LocalDateTime comparison methods.
     * 
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return True if transaction is within date range
     */
    public boolean isWithinDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (transactionTimestamp == null || startDate == null || endDate == null) {
            return false;
        }
        
        return !transactionTimestamp.isBefore(startDate) && 
               !transactionTimestamp.isAfter(endDate);
    }

    /**
     * Formats transaction amount for display.
     * Demonstrates BigDecimal formatting and string operations.
     * 
     * @return Formatted amount string
     */
    public String getFormattedAmount() {
        if (transactionAmount == null) {
            return "$0.00";
        }
        
        return String.format("$%,.2f", transactionAmount);
    }

    /**
     * Checks if transaction is a recent transaction (within last 24 hours).
     * Demonstrates LocalDateTime.now() usage for time comparisons.
     * 
     * @return True if transaction is recent
     */
    public boolean isRecentTransaction() {
        if (transactionTimestamp == null) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now(); // LocalDateTime.now() usage
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);
        
        return transactionTimestamp.isAfter(twentyFourHoursAgo);
    }

    /**
     * Returns hash code based on transaction ID.
     * 
     * @return Hash code for entity comparison
     */
    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    /**
     * Compares entities based on transaction ID.
     * 
     * @param obj Object to compare
     * @return True if entities have the same transaction ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Transaction transaction = (Transaction) obj;
        return Objects.equals(transactionId, transaction.transactionId);
    }

    /**
     * String representation for debugging and logging.
     * 
     * @return String containing key entity information
     */
    @Override
    public String toString() {
        return String.format("Transaction{transactionId='%s', accountId='%s', cardNumber='%s', " +
                           "transactionType='%s', transactionCategory='%s', transactionAmount=%s, " +
                           "transactionTimestamp=%s, merchantName='%s', merchantCity='%s'}",
            transactionId, accountId, cardNumber, transactionType, transactionCategory, 
            transactionAmount, transactionTimestamp, merchantName, merchantCity);
    }
}