package com.carddemo.transaction;

import com.carddemo.common.enums.TransactionCategory;
import com.carddemo.common.enums.TransactionType;
import com.carddemo.common.validator.ValidCardNumber;
import com.carddemo.common.validator.ValidCurrency;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Common transaction data transfer object providing consistent transaction representation
 * across all API operations with exact COBOL field correspondence.
 * 
 * This DTO maintains exact field mapping to the COBOL TRAN-RECORD structure defined in
 * CVTRA05Y.cpy copybook, ensuring complete compatibility with legacy data formats while
 * supporting modern Java 21 Spring Boot microservices architecture.
 * 
 * Field Mappings from COBOL TRAN-RECORD (RECLN = 350):
 * - TRAN-ID (PIC X(16)) → transactionId (String)
 * - TRAN-TYPE-CD (PIC X(02)) → transactionType (TransactionType enum)  
 * - TRAN-CAT-CD (PIC 9(04)) → categoryCode (TransactionCategory enum)
 * - TRAN-SOURCE (PIC X(10)) → source (String)
 * - TRAN-DESC (PIC X(100)) → description (String)
 * - TRAN-AMT (PIC S9(09)V99) → amount (BigDecimal with precision=11, scale=2)
 * - TRAN-MERCHANT-ID (PIC 9(09)) → merchantId (String)
 * - TRAN-MERCHANT-NAME (PIC X(50)) → merchantName (String)
 * - TRAN-MERCHANT-CITY (PIC X(50)) → merchantCity (String)
 * - TRAN-MERCHANT-ZIP (PIC X(10)) → merchantZip (String)
 * - TRAN-CARD-NUM (PIC X(16)) → cardNumber (String)
 * - TRAN-ORIG-TS (PIC X(26)) → originalTimestamp (LocalDateTime)
 * - TRAN-PROC-TS (PIC X(26)) → processingTimestamp (LocalDateTime)
 * 
 * The DTO includes comprehensive validation annotations for data integrity,
 * Jackson JSON serialization support for consistent API responses, and
 * maintains exact BigDecimal precision for financial calculations equivalent
 * to COBOL COMP-3 arithmetic operations.
 * 
 * @author Blitzy Agent  
 * @version CardDemo_v1.0-15-g27d6c6f-68
 * @since 1.0
 */
public class TransactionDTO {

    /**
     * Transaction identifier corresponding to TRAN-ID field (PIC X(16))
     * Unique identifier for each transaction record in the system
     */
    @JsonProperty("transaction_id")
    @NotBlank(message = "Transaction ID is required")
    @Size(max = 16, message = "Transaction ID must not exceed 16 characters")
    private String transactionId;

    /**
     * Transaction type code corresponding to TRAN-TYPE-CD field (PIC X(02))
     * Enumerated value representing the type of transaction (purchase, payment, etc.)
     */
    @JsonProperty("transaction_type")
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    /**
     * Transaction category code corresponding to TRAN-CAT-CD field (PIC 9(04))
     * Enumerated value for transaction categorization and balance management
     */
    @JsonProperty("category_code")
    @NotNull(message = "Transaction category is required")
    private TransactionCategory categoryCode;

    /**
     * Transaction source corresponding to TRAN-SOURCE field (PIC X(10))
     * Identifies the source system or channel that initiated the transaction
     */
    @JsonProperty("source")
    @Size(max = 10, message = "Transaction source must not exceed 10 characters")
    private String source;

    /**
     * Transaction description corresponding to TRAN-DESC field (PIC X(100))
     * Human-readable description of the transaction
     */
    @JsonProperty("description")
    @Size(max = 100, message = "Transaction description must not exceed 100 characters")
    private String description;

    /**
     * Transaction amount corresponding to TRAN-AMT field (PIC S9(09)V99)
     * Monetary amount with exact BigDecimal precision matching COBOL COMP-3 arithmetic
     */
    @JsonProperty("amount")
    @NotNull(message = "Transaction amount is required")
    @ValidCurrency(precision = 11, scale = 2, min = "0.01", message = "Transaction amount must be positive with maximum 2 decimal places")
    @DecimalMin(value = "0.01", message = "Transaction amount must be greater than zero")
    private BigDecimal amount;

    /**
     * Merchant identifier corresponding to TRAN-MERCHANT-ID field (PIC 9(09))
     * Numeric identifier for the merchant, stored as string to preserve leading zeros
     */
    @JsonProperty("merchant_id")
    @Size(max = 9, message = "Merchant ID must not exceed 9 characters")
    private String merchantId;

    /**
     * Merchant name corresponding to TRAN-MERCHANT-NAME field (PIC X(50))
     * Business name of the merchant where transaction occurred
     */
    @JsonProperty("merchant_name")
    @Size(max = 50, message = "Merchant name must not exceed 50 characters")
    private String merchantName;

    /**
     * Merchant city corresponding to TRAN-MERCHANT-CITY field (PIC X(50))
     * City location of the merchant
     */
    @JsonProperty("merchant_city")
    @Size(max = 50, message = "Merchant city must not exceed 50 characters")
    private String merchantCity;

    /**
     * Merchant ZIP code corresponding to TRAN-MERCHANT-ZIP field (PIC X(10))
     * Postal code of the merchant location
     */
    @JsonProperty("merchant_zip")
    @Size(max = 10, message = "Merchant ZIP code must not exceed 10 characters")
    private String merchantZip;

    /**
     * Card number corresponding to TRAN-CARD-NUM field (PIC X(16))
     * Credit card number associated with the transaction, validated using Luhn algorithm
     */
    @JsonProperty("card_number")
    @ValidCardNumber(message = "Card number must be valid 16-digit number")
    private String cardNumber;

    /**
     * Original timestamp corresponding to TRAN-ORIG-TS field (PIC X(26))
     * Original date and time when the transaction was initiated
     */
    @JsonProperty("original_timestamp")
    private LocalDateTime originalTimestamp;

    /**
     * Processing timestamp corresponding to TRAN-PROC-TS field (PIC X(26))
     * Date and time when the transaction was processed by the system
     */
    @JsonProperty("processing_timestamp")
    private LocalDateTime processingTimestamp;

    /**
     * Default constructor for TransactionDTO
     * Initializes a new instance with no preset values
     */
    public TransactionDTO() {
        // Default constructor for JSON deserialization and JPA
    }

    /**
     * Full constructor for TransactionDTO with all fields
     * 
     * @param transactionId Unique transaction identifier
     * @param transactionType Type of transaction
     * @param categoryCode Transaction category
     * @param source Transaction source system
     * @param description Transaction description
     * @param amount Transaction monetary amount
     * @param merchantId Merchant identifier
     * @param merchantName Merchant business name
     * @param merchantCity Merchant city location
     * @param merchantZip Merchant postal code
     * @param cardNumber Associated card number
     * @param originalTimestamp Original transaction timestamp
     * @param processingTimestamp Processing timestamp
     */
    public TransactionDTO(String transactionId, TransactionType transactionType, 
                         TransactionCategory categoryCode, String source, String description,
                         BigDecimal amount, String merchantId, String merchantName, 
                         String merchantCity, String merchantZip, String cardNumber,
                         LocalDateTime originalTimestamp, LocalDateTime processingTimestamp) {
        this.transactionId = transactionId;
        this.transactionType = transactionType;
        this.categoryCode = categoryCode;
        this.source = source;
        this.description = description;
        this.amount = amount;
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.merchantCity = merchantCity;
        this.merchantZip = merchantZip;
        this.cardNumber = cardNumber;
        this.originalTimestamp = originalTimestamp;
        this.processingTimestamp = processingTimestamp;
    }

    /**
     * Get transaction identifier
     * 
     * @return String transaction ID (up to 16 characters)
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Set transaction identifier
     * 
     * @param transactionId Unique transaction identifier (max 16 characters)
     */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Get transaction type
     * 
     * @return TransactionType enumerated value
     */
    public TransactionType getTransactionType() {
        return transactionType;
    }

    /**
     * Set transaction type
     * 
     * @param transactionType TransactionType enumerated value
     */
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * Get transaction category code
     * 
     * @return TransactionCategory enumerated value
     */
    public TransactionCategory getCategoryCode() {
        return categoryCode;
    }

    /**
     * Set transaction category code
     * 
     * @param categoryCode TransactionCategory enumerated value
     */
    public void setCategoryCode(TransactionCategory categoryCode) {
        this.categoryCode = categoryCode;
    }

    /**
     * Get transaction source
     * 
     * @return String source identifier (up to 10 characters)
     */
    public String getSource() {
        return source;
    }

    /**
     * Set transaction source
     * 
     * @param source Transaction source identifier (max 10 characters)
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Get transaction description
     * 
     * @return String transaction description (up to 100 characters)
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set transaction description
     * 
     * @param description Human-readable transaction description (max 100 characters)
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get transaction amount
     * 
     * @return BigDecimal amount with exact precision (11 digits total, 2 decimal places)
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Set transaction amount
     * 
     * @param amount BigDecimal monetary amount with COBOL COMP-3 precision
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * Get merchant identifier
     * 
     * @return String merchant ID (up to 9 characters)
     */
    public String getMerchantId() {
        return merchantId;
    }

    /**
     * Set merchant identifier
     * 
     * @param merchantId Merchant identifier (max 9 characters)
     */
    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    /**
     * Get merchant name
     * 
     * @return String merchant business name (up to 50 characters)
     */
    public String getMerchantName() {
        return merchantName;
    }

    /**
     * Set merchant name
     * 
     * @param merchantName Merchant business name (max 50 characters)
     */
    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    /**
     * Get merchant city
     * 
     * @return String merchant city location (up to 50 characters)
     */
    public String getMerchantCity() {
        return merchantCity;
    }

    /**
     * Set merchant city
     * 
     * @param merchantCity Merchant city location (max 50 characters)
     */
    public void setMerchantCity(String merchantCity) {
        this.merchantCity = merchantCity;
    }

    /**
     * Get merchant ZIP code
     * 
     * @return String merchant postal code (up to 10 characters)
     */
    public String getMerchantZip() {
        return merchantZip;
    }

    /**
     * Set merchant ZIP code
     * 
     * @param merchantZip Merchant postal code (max 10 characters)
     */
    public void setMerchantZip(String merchantZip) {
        this.merchantZip = merchantZip;
    }

    /**
     * Get card number
     * 
     * @return String validated 16-digit card number
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Set card number
     * 
     * @param cardNumber Valid 16-digit credit card number
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Get original timestamp
     * 
     * @return LocalDateTime when transaction was originally initiated
     */
    public LocalDateTime getOriginalTimestamp() {
        return originalTimestamp;
    }

    /**
     * Set original timestamp
     * 
     * @param originalTimestamp LocalDateTime when transaction was initiated
     */
    public void setOriginalTimestamp(LocalDateTime originalTimestamp) {
        this.originalTimestamp = originalTimestamp;
    }

    /**
     * Get processing timestamp
     * 
     * @return LocalDateTime when transaction was processed
     */
    public LocalDateTime getProcessingTimestamp() {
        return processingTimestamp;
    }

    /**
     * Set processing timestamp
     * 
     * @param processingTimestamp LocalDateTime when transaction was processed
     */
    public void setProcessingTimestamp(LocalDateTime processingTimestamp) {
        this.processingTimestamp = processingTimestamp;
    }

    /**
     * Check if this transaction represents a debit operation
     * Uses transaction type to determine debit/credit nature
     * 
     * @return true if transaction is a debit (increases balance owed)
     */
    public boolean isDebit() {
        return transactionType != null && transactionType.isDebit();
    }

    /**
     * Check if this transaction represents a credit operation  
     * Uses transaction type to determine debit/credit nature
     * 
     * @return true if transaction is a credit (decreases balance owed)
     */
    public boolean isCredit() {
        return transactionType != null && transactionType.isCredit();
    }

    /**
     * Validate if all required fields are present and valid
     * Provides comprehensive validation beyond individual field constraints
     * 
     * @return true if all required fields are valid
     */
    public boolean isValid() {
        return transactionId != null && !transactionId.trim().isEmpty() &&
               transactionType != null &&
               categoryCode != null &&
               amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * String representation of TransactionDTO for logging and debugging
     * Includes key transaction details while masking sensitive card information
     * 
     * @return String representation with masked card number
     */
    @Override
    public String toString() {
        String maskedCardNumber = cardNumber != null && cardNumber.length() >= 4 ?
            "****" + cardNumber.substring(cardNumber.length() - 4) : "****";
            
        return String.format("TransactionDTO{id='%s', type=%s, category=%s, amount=%s, card='%s', merchant='%s'}",
            transactionId, transactionType, categoryCode, amount, maskedCardNumber, merchantName);
    }

    /**
     * Equals method for TransactionDTO comparison
     * Based on transaction ID as unique identifier
     * 
     * @param obj Object to compare
     * @return true if objects are equal based on transaction ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TransactionDTO that = (TransactionDTO) obj;
        return transactionId != null && transactionId.equals(that.transactionId);
    }

    /**
     * Hash code method for TransactionDTO
     * Based on transaction ID as unique identifier
     * 
     * @return hash code value
     */
    @Override
    public int hashCode() {
        return transactionId != null ? transactionId.hashCode() : 0;
    }
}