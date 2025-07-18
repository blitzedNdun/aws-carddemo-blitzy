package com.carddemo.transaction;

import com.carddemo.common.enums.TransactionType;
import com.carddemo.common.enums.TransactionCategory;
import com.carddemo.common.validator.ValidCardNumber;
import com.carddemo.common.validator.ValidCurrency;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.DecimalMin;

/**
 * Common transaction data transfer object providing consistent transaction representation 
 * across all API operations with exact COBOL field correspondence.
 * 
 * <p>This DTO maintains complete structural fidelity with the original COBOL TRAN-RECORD
 * from CVTRA05Y.cpy, ensuring all transaction fields are accurately represented with
 * appropriate Java data types and BigDecimal precision for financial calculations.</p>
 * 
 * <p><strong>COBOL Field Mappings:</strong></p>
 * <ul>
 *   <li>TRAN-ID PIC X(16) → transactionId String</li>
 *   <li>TRAN-TYPE-CD PIC X(02) → transactionType TransactionType enum</li>
 *   <li>TRAN-CAT-CD PIC 9(04) → categoryCode String</li>
 *   <li>TRAN-SOURCE PIC X(10) → source String</li>
 *   <li>TRAN-DESC PIC X(100) → description String</li>
 *   <li>TRAN-AMT PIC S9(09)V99 → amount BigDecimal</li>
 *   <li>TRAN-MERCHANT-ID PIC 9(09) → merchantId String</li>
 *   <li>TRAN-MERCHANT-NAME PIC X(50) → merchantName String</li>
 *   <li>TRAN-MERCHANT-CITY PIC X(50) → merchantCity String</li>
 *   <li>TRAN-MERCHANT-ZIP PIC X(10) → merchantZip String</li>
 *   <li>TRAN-CARD-NUM PIC X(16) → cardNumber String</li>
 *   <li>TRAN-ORIG-TS PIC X(26) → originalTimestamp LocalDateTime</li>
 *   <li>TRAN-PROC-TS PIC X(26) → processingTimestamp LocalDateTime</li>
 * </ul>
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 */
public class TransactionDTO {
    
    /**
     * Transaction identifier - 16-character unique identifier.
     * Maps to TRAN-ID PIC X(16) from CVTRA05Y.cpy
     */
    @JsonProperty("transactionId")
    private String transactionId;
    
    /**
     * Transaction type enumeration value.
     * Maps to TRAN-TYPE-CD PIC X(02) from CVTRA05Y.cpy
     */
    @JsonProperty("transactionType")
    private TransactionType transactionType;
    
    /**
     * Transaction category code.
     * Maps to TRAN-CAT-CD PIC 9(04) from CVTRA05Y.cpy
     */
    @JsonProperty("categoryCode")
    private String categoryCode;
    
    /**
     * Transaction source identifier.
     * Maps to TRAN-SOURCE PIC X(10) from CVTRA05Y.cpy
     */
    @JsonProperty("source")
    private String source;
    
    /**
     * Transaction description.
     * Maps to TRAN-DESC PIC X(100) from CVTRA05Y.cpy
     */
    @JsonProperty("description")
    private String description;
    
    /**
     * Transaction amount with exact financial precision.
     * Maps to TRAN-AMT PIC S9(09)V99 from CVTRA05Y.cpy
     */
    @JsonProperty("amount")
    @ValidCurrency(min = "-999999999.99", max = "999999999.99")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal amount;
    
    /**
     * Merchant identifier.
     * Maps to TRAN-MERCHANT-ID PIC 9(09) from CVTRA05Y.cpy
     */
    @JsonProperty("merchantId")
    private String merchantId;
    
    /**
     * Merchant name.
     * Maps to TRAN-MERCHANT-NAME PIC X(50) from CVTRA05Y.cpy
     */
    @JsonProperty("merchantName")
    private String merchantName;
    
    /**
     * Merchant city.
     * Maps to TRAN-MERCHANT-CITY PIC X(50) from CVTRA05Y.cpy
     */
    @JsonProperty("merchantCity")
    private String merchantCity;
    
    /**
     * Merchant ZIP code.
     * Maps to TRAN-MERCHANT-ZIP PIC X(10) from CVTRA05Y.cpy
     */
    @JsonProperty("merchantZip")
    private String merchantZip;
    
    /**
     * Card number associated with transaction.
     * Maps to TRAN-CARD-NUM PIC X(16) from CVTRA05Y.cpy
     */
    @JsonProperty("cardNumber")
    @ValidCardNumber
    private String cardNumber;
    
    /**
     * Original transaction timestamp.
     * Maps to TRAN-ORIG-TS PIC X(26) from CVTRA05Y.cpy
     */
    @JsonProperty("originalTimestamp")
    private LocalDateTime originalTimestamp;
    
    /**
     * Processing timestamp.
     * Maps to TRAN-PROC-TS PIC X(26) from CVTRA05Y.cpy
     */
    @JsonProperty("processingTimestamp")
    private LocalDateTime processingTimestamp;
    
    /**
     * Default constructor for TransactionDTO.
     */
    public TransactionDTO() {
        // Default constructor for serialization
    }
    
    /**
     * Gets the transaction identifier.
     * 
     * @return transaction ID
     */
    public String getTransactionId() {
        return transactionId;
    }
    
    /**
     * Sets the transaction identifier.
     * 
     * @param transactionId transaction ID
     */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    /**
     * Gets the transaction type.
     * 
     * @return transaction type enum
     */
    public TransactionType getTransactionType() {
        return transactionType;
    }
    
    /**
     * Sets the transaction type.
     * 
     * @param transactionType transaction type enum
     */
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }
    
    /**
     * Gets the category code.
     * 
     * @return category code
     */
    public String getCategoryCode() {
        return categoryCode;
    }
    
    /**
     * Sets the category code.
     * 
     * @param categoryCode category code
     */
    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }
    
    /**
     * Gets the source.
     * 
     * @return source
     */
    public String getSource() {
        return source;
    }
    
    /**
     * Sets the source.
     * 
     * @param source source
     */
    public void setSource(String source) {
        this.source = source;
    }
    
    /**
     * Gets the description.
     * 
     * @return description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Sets the description.
     * 
     * @param description description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Gets the amount.
     * 
     * @return amount
     */
    public BigDecimal getAmount() {
        return amount;
    }
    
    /**
     * Sets the amount.
     * 
     * @param amount amount
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    /**
     * Gets the merchant identifier.
     * 
     * @return merchant ID
     */
    public String getMerchantId() {
        return merchantId;
    }
    
    /**
     * Sets the merchant identifier.
     * 
     * @param merchantId merchant ID
     */
    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }
    
    /**
     * Gets the merchant name.
     * 
     * @return merchant name
     */
    public String getMerchantName() {
        return merchantName;
    }
    
    /**
     * Sets the merchant name.
     * 
     * @param merchantName merchant name
     */
    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }
    
    /**
     * Gets the merchant city.
     * 
     * @return merchant city
     */
    public String getMerchantCity() {
        return merchantCity;
    }
    
    /**
     * Sets the merchant city.
     * 
     * @param merchantCity merchant city
     */
    public void setMerchantCity(String merchantCity) {
        this.merchantCity = merchantCity;
    }
    
    /**
     * Gets the merchant ZIP code.
     * 
     * @return merchant ZIP
     */
    public String getMerchantZip() {
        return merchantZip;
    }
    
    /**
     * Sets the merchant ZIP code.
     * 
     * @param merchantZip merchant ZIP
     */
    public void setMerchantZip(String merchantZip) {
        this.merchantZip = merchantZip;
    }
    
    /**
     * Gets the card number.
     * 
     * @return card number
     */
    public String getCardNumber() {
        return cardNumber;
    }
    
    /**
     * Sets the card number.
     * 
     * @param cardNumber card number
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
    
    /**
     * Gets the original timestamp.
     * 
     * @return original timestamp
     */
    public LocalDateTime getOriginalTimestamp() {
        return originalTimestamp;
    }
    
    /**
     * Sets the original timestamp.
     * 
     * @param originalTimestamp original timestamp
     */
    public void setOriginalTimestamp(LocalDateTime originalTimestamp) {
        this.originalTimestamp = originalTimestamp;
    }
    
    /**
     * Gets the processing timestamp.
     * 
     * @return processing timestamp
     */
    public LocalDateTime getProcessingTimestamp() {
        return processingTimestamp;
    }
    
    /**
     * Sets the processing timestamp.
     * 
     * @param processingTimestamp processing timestamp
     */
    public void setProcessingTimestamp(LocalDateTime processingTimestamp) {
        this.processingTimestamp = processingTimestamp;
    }
}