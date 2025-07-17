package com.carddemo.transaction;

import com.carddemo.common.enums.TransactionType;
import com.carddemo.common.enums.TransactionCategory;
import com.carddemo.common.validator.ValidCardNumber;
import com.carddemo.common.validator.ValidCurrency;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Transaction Data Transfer Object providing consistent transaction representation across
 * all API operations with exact COBOL field correspondence.
 * 
 * This DTO maintains complete functional equivalence with the original COBOL TRAN-RECORD
 * structure from CVTRA05Y.cpy copybook, ensuring seamless migration from mainframe
 * VSAM data structures to modern PostgreSQL-based REST API communication.
 * 
 * Key Features:
 * - Exact field mapping to COBOL transaction record structure (RECLN = 350)
 * - Jackson JSON serialization for consistent REST API communication
 * - Jakarta Bean Validation for comprehensive data integrity
 * - BigDecimal precision matching COBOL COMP-3 financial calculations
 * - Integration with Spring Boot microservices architecture
 * 
 * Field Mapping from COBOL TRAN-RECORD:
 * - TRAN-ID (PIC X(16)) → transactionId
 * - TRAN-TYPE-CD (PIC X(02)) → transactionType
 * - TRAN-CAT-CD (PIC 9(04)) → categoryCode
 * - TRAN-SOURCE (PIC X(10)) → source
 * - TRAN-DESC (PIC X(100)) → description
 * - TRAN-AMT (PIC S9(09)V99) → amount
 * - TRAN-MERCHANT-ID (PIC 9(09)) → merchantId
 * - TRAN-MERCHANT-NAME (PIC X(50)) → merchantName
 * - TRAN-MERCHANT-CITY (PIC X(50)) → merchantCity
 * - TRAN-MERCHANT-ZIP (PIC X(10)) → merchantZip
 * - TRAN-CARD-NUM (PIC X(16)) → cardNumber
 * - TRAN-ORIG-TS (PIC X(26)) → originalTimestamp
 * - TRAN-PROC-TS (PIC X(26)) → processingTimestamp
 * 
 * Performance Requirements:
 * - Supports 10,000+ TPS transaction processing capacity
 * - Maintains sub-200ms response times for authorization operations
 * - Optimized for Spring Boot microservices communication
 * - Memory-efficient serialization for high-volume operations
 * 
 * Technical Implementation:
 * - JSON property naming consistent with React frontend expectations
 * - Date/time fields use ISO-8601 format for API compatibility
 * - BigDecimal amounts preserve exact financial precision
 * - Validation annotations ensure data integrity across service boundaries
 * 
 * Based on COBOL sources:
 * - CVTRA05Y.cpy: Transaction record structure definition
 * - COTRN00C.cbl: Transaction listing program
 * - COTRN01C.cbl: Transaction viewing program
 * - COTRN02C.cbl: Transaction addition program
 * 
 * @author Blitzy Platform
 * @version 1.0
 * @since 2024-01-01
 */
public class TransactionDTO {
    
    /**
     * Transaction ID - unique identifier for each transaction
     * Maps to COBOL TRAN-ID field (PIC X(16))
     * System-generated primary key for transaction record
     */
    @JsonProperty("transactionId")
    private String transactionId;
    
    /**
     * Transaction Type - categorizes the type of transaction
     * Maps to COBOL TRAN-TYPE-CD field (PIC X(02))
     * Must match valid TransactionType enum values
     */
    @JsonProperty("transactionType")
    private TransactionType transactionType;
    
    /**
     * Category Code - specifies the transaction category
     * Maps to COBOL TRAN-CAT-CD field (PIC 9(04))
     * Must match valid TransactionCategory enum values
     */
    @JsonProperty("categoryCode")
    private TransactionCategory categoryCode;
    
    /**
     * Transaction Source - identifies the originating system or channel
     * Maps to COBOL TRAN-SOURCE field (PIC X(10))
     * Examples: "ONLINE", "ATM", "POS", "BATCH"
     */
    @JsonProperty("source")
    private String source;
    
    /**
     * Transaction Description - human-readable description of the transaction
     * Maps to COBOL TRAN-DESC field (PIC X(100))
     * Free-form text describing the transaction details
     */
    @JsonProperty("description")
    private String description;
    
    /**
     * Transaction Amount - monetary value of the transaction
     * Maps to COBOL TRAN-AMT field (PIC S9(09)V99)
     * Uses BigDecimal for exact financial precision matching COBOL COMP-3
     */
    @JsonProperty("amount")
    @ValidCurrency(
        min = "-999999999.99",
        max = "999999999.99",
        precision = 11,
        scale = 2,
        message = "Transaction amount must be a valid currency value with 2 decimal places"
    )
    @DecimalMin(
        value = "-999999999.99",
        message = "Transaction amount cannot be less than -999,999,999.99"
    )
    private BigDecimal amount;
    
    /**
     * Merchant ID - identifies the merchant for purchase transactions
     * Maps to COBOL TRAN-MERCHANT-ID field (PIC 9(09))
     * Numeric identifier for merchant processing
     */
    @JsonProperty("merchantId")
    private String merchantId;
    
    /**
     * Merchant Name - name of the merchant or business
     * Maps to COBOL TRAN-MERCHANT-NAME field (PIC X(50))
     * Text description of the merchant
     */
    @JsonProperty("merchantName")
    private String merchantName;
    
    /**
     * Merchant City - city where the merchant is located
     * Maps to COBOL TRAN-MERCHANT-CITY field (PIC X(50))
     * Geographic location information
     */
    @JsonProperty("merchantCity")
    private String merchantCity;
    
    /**
     * Merchant ZIP Code - postal code for merchant location
     * Maps to COBOL TRAN-MERCHANT-ZIP field (PIC X(10))
     * Postal code for merchant address
     */
    @JsonProperty("merchantZip")
    private String merchantZip;
    
    /**
     * Card Number - credit card number used for the transaction
     * Maps to COBOL TRAN-CARD-NUM field (PIC X(16))
     * Must be a valid 16-digit credit card number
     */
    @JsonProperty("cardNumber")
    @ValidCardNumber(
        message = "Card number must be a valid 16-digit credit card number",
        allowNullOrEmpty = true
    )
    private String cardNumber;
    
    /**
     * Original Timestamp - when the transaction was first initiated
     * Maps to COBOL TRAN-ORIG-TS field (PIC X(26))
     * ISO-8601 formatted timestamp
     */
    @JsonProperty("originalTimestamp")
    private LocalDateTime originalTimestamp;
    
    /**
     * Processing Timestamp - when the transaction was processed by the system
     * Maps to COBOL TRAN-PROC-TS field (PIC X(26))
     * ISO-8601 formatted timestamp
     */
    @JsonProperty("processingTimestamp")
    private LocalDateTime processingTimestamp;
    
    /**
     * Default constructor for TransactionDTO
     */
    public TransactionDTO() {
        // Default constructor for serialization frameworks
    }
    
    /**
     * Constructor with all required fields for transaction creation
     * 
     * @param transactionId unique transaction identifier
     * @param transactionType type of transaction
     * @param categoryCode transaction category
     * @param source originating system or channel
     * @param description transaction description
     * @param amount transaction amount
     * @param cardNumber credit card number
     * @param originalTimestamp original transaction timestamp
     * @param processingTimestamp processing timestamp
     */
    public TransactionDTO(String transactionId, TransactionType transactionType, 
                         TransactionCategory categoryCode, String source, String description, 
                         BigDecimal amount, String cardNumber, LocalDateTime originalTimestamp, 
                         LocalDateTime processingTimestamp) {
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
     * Get the transaction ID
     * 
     * @return transaction ID string
     */
    public String getTransactionId() {
        return transactionId;
    }
    
    /**
     * Set the transaction ID
     * 
     * @param transactionId transaction ID to set
     */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    /**
     * Get the transaction type
     * 
     * @return TransactionType enum value
     */
    public TransactionType getTransactionType() {
        return transactionType;
    }
    
    /**
     * Set the transaction type
     * 
     * @param transactionType TransactionType enum value to set
     */
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }
    
    /**
     * Get the category code
     * 
     * @return TransactionCategory enum value
     */
    public TransactionCategory getCategoryCode() {
        return categoryCode;
    }
    
    /**
     * Set the category code
     * 
     * @param categoryCode TransactionCategory enum value to set
     */
    public void setCategoryCode(TransactionCategory categoryCode) {
        this.categoryCode = categoryCode;
    }
    
    /**
     * Get the transaction source
     * 
     * @return source string
     */
    public String getSource() {
        return source;
    }
    
    /**
     * Set the transaction source
     * 
     * @param source source string to set
     */
    public void setSource(String source) {
        this.source = source;
    }
    
    /**
     * Get the transaction description
     * 
     * @return description string
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Set the transaction description
     * 
     * @param description description string to set
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Get the transaction amount
     * 
     * @return BigDecimal amount with exact financial precision
     */
    public BigDecimal getAmount() {
        return amount;
    }
    
    /**
     * Set the transaction amount
     * 
     * @param amount BigDecimal amount to set
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    /**
     * Get the merchant ID
     * 
     * @return merchant ID string
     */
    public String getMerchantId() {
        return merchantId;
    }
    
    /**
     * Set the merchant ID
     * 
     * @param merchantId merchant ID string to set
     */
    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }
    
    /**
     * Get the merchant name
     * 
     * @return merchant name string
     */
    public String getMerchantName() {
        return merchantName;
    }
    
    /**
     * Set the merchant name
     * 
     * @param merchantName merchant name string to set
     */
    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }
    
    /**
     * Get the merchant city
     * 
     * @return merchant city string
     */
    public String getMerchantCity() {
        return merchantCity;
    }
    
    /**
     * Set the merchant city
     * 
     * @param merchantCity merchant city string to set
     */
    public void setMerchantCity(String merchantCity) {
        this.merchantCity = merchantCity;
    }
    
    /**
     * Get the merchant ZIP code
     * 
     * @return merchant ZIP code string
     */
    public String getMerchantZip() {
        return merchantZip;
    }
    
    /**
     * Set the merchant ZIP code
     * 
     * @param merchantZip merchant ZIP code string to set
     */
    public void setMerchantZip(String merchantZip) {
        this.merchantZip = merchantZip;
    }
    
    /**
     * Get the card number
     * 
     * @return card number string
     */
    public String getCardNumber() {
        return cardNumber;
    }
    
    /**
     * Set the card number
     * 
     * @param cardNumber card number string to set
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
    
    /**
     * Get the original timestamp
     * 
     * @return LocalDateTime of original transaction
     */
    public LocalDateTime getOriginalTimestamp() {
        return originalTimestamp;
    }
    
    /**
     * Set the original timestamp
     * 
     * @param originalTimestamp LocalDateTime to set
     */
    public void setOriginalTimestamp(LocalDateTime originalTimestamp) {
        this.originalTimestamp = originalTimestamp;
    }
    
    /**
     * Get the processing timestamp
     * 
     * @return LocalDateTime of processing time
     */
    public LocalDateTime getProcessingTimestamp() {
        return processingTimestamp;
    }
    
    /**
     * Set the processing timestamp
     * 
     * @param processingTimestamp LocalDateTime to set
     */
    public void setProcessingTimestamp(LocalDateTime processingTimestamp) {
        this.processingTimestamp = processingTimestamp;
    }
    
    /**
     * Check if this transaction is a debit transaction (increases account balance)
     * 
     * @return true if transaction is a debit, false otherwise
     */
    public boolean isDebitTransaction() {
        return transactionType != null && transactionType.isDebit();
    }
    
    /**
     * Check if this transaction is a credit transaction (decreases account balance)
     * 
     * @return true if transaction is a credit, false otherwise
     */
    public boolean isCreditTransaction() {
        return transactionType != null && transactionType.isCredit();
    }
    
    /**
     * Check if this transaction affects the account balance
     * 
     * @return true if transaction affects balance, false otherwise
     */
    public boolean affectsBalance() {
        return categoryCode != null && categoryCode.affectsBalance();
    }
    
    /**
     * Check if this transaction is eligible for interest calculations
     * 
     * @return true if eligible for interest, false otherwise
     */
    public boolean isInterestEligible() {
        return categoryCode != null && categoryCode.isInterestEligible();
    }
    
    /**
     * Get the absolute amount value for calculations
     * 
     * @return BigDecimal absolute value of amount
     */
    public BigDecimal getAbsoluteAmount() {
        return amount != null ? amount.abs() : BigDecimal.ZERO;
    }
    
    /**
     * Get the signed amount based on transaction type
     * For debit transactions, amount is positive
     * For credit transactions, amount is negative
     * 
     * @return BigDecimal signed amount
     */
    public BigDecimal getSignedAmount() {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return isCreditTransaction() ? amount.negate() : amount;
    }
    
    /**
     * Validate the transaction data for completeness
     * 
     * @return true if all required fields are present, false otherwise
     */
    public boolean isValid() {
        return transactionId != null && !transactionId.trim().isEmpty() &&
               transactionType != null &&
               categoryCode != null &&
               amount != null &&
               originalTimestamp != null;
    }
    
    /**
     * Create a formatted string representation of the transaction
     * 
     * @return formatted transaction string
     */
    public String getFormattedTransaction() {
        StringBuilder sb = new StringBuilder();
        sb.append("Transaction ID: ").append(transactionId).append(" | ");
        sb.append("Type: ").append(transactionType != null ? transactionType.getDescription() : "N/A").append(" | ");
        sb.append("Amount: $").append(amount != null ? amount.toString() : "0.00").append(" | ");
        sb.append("Date: ").append(originalTimestamp != null ? originalTimestamp.toString() : "N/A");
        return sb.toString();
    }
    
    /**
     * Equals method for transaction comparison
     * 
     * @param obj object to compare
     * @return true if transactions are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TransactionDTO that = (TransactionDTO) obj;
        return Objects.equals(transactionId, that.transactionId) &&
               Objects.equals(transactionType, that.transactionType) &&
               Objects.equals(categoryCode, that.categoryCode) &&
               Objects.equals(source, that.source) &&
               Objects.equals(description, that.description) &&
               Objects.equals(amount, that.amount) &&
               Objects.equals(merchantId, that.merchantId) &&
               Objects.equals(merchantName, that.merchantName) &&
               Objects.equals(merchantCity, that.merchantCity) &&
               Objects.equals(merchantZip, that.merchantZip) &&
               Objects.equals(cardNumber, that.cardNumber) &&
               Objects.equals(originalTimestamp, that.originalTimestamp) &&
               Objects.equals(processingTimestamp, that.processingTimestamp);
    }
    
    /**
     * Hash code method for transaction hashing
     * 
     * @return hash code based on transaction fields
     */
    @Override
    public int hashCode() {
        return Objects.hash(transactionId, transactionType, categoryCode, source, description,
                           amount, merchantId, merchantName, merchantCity, merchantZip,
                           cardNumber, originalTimestamp, processingTimestamp);
    }
    
    /**
     * String representation of the transaction
     * 
     * @return string representation
     */
    @Override
    public String toString() {
        return "TransactionDTO{" +
                "transactionId='" + transactionId + '\'' +
                ", transactionType=" + transactionType +
                ", categoryCode=" + categoryCode +
                ", source='" + source + '\'' +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                ", merchantId='" + merchantId + '\'' +
                ", merchantName='" + merchantName + '\'' +
                ", merchantCity='" + merchantCity + '\'' +
                ", merchantZip='" + merchantZip + '\'' +
                ", cardNumber='" + (cardNumber != null ? "****" + cardNumber.substring(cardNumber.length() - 4) : null) + '\'' +
                ", originalTimestamp=" + originalTimestamp +
                ", processingTimestamp=" + processingTimestamp +
                '}';
    }
}