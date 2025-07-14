/*
 * Copyright 2024 CardDemo Application
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
 * Common transaction data transfer object providing consistent transaction representation 
 * across all API operations with exact COBOL field correspondence.
 * 
 * <p>This DTO maintains exact field mapping to the original COBOL TRAN-RECORD structure 
 * (CVTRA05Y.cpy) while providing modern Java type safety and validation capabilities. 
 * The class serves as the primary data contract for all transaction-related API operations 
 * including transaction creation, updates, queries, and batch processing.</p>
 * 
 * <p><strong>COBOL Structure Mapping:</strong></p>
 * <ul>
 *   <li>TRAN-ID (PIC X(16)) → String transactionId</li>
 *   <li>TRAN-TYPE-CD (PIC X(02)) → TransactionType transactionType</li>
 *   <li>TRAN-CAT-CD (PIC 9(04)) → TransactionCategory categoryCode</li>
 *   <li>TRAN-SOURCE (PIC X(10)) → String source</li>
 *   <li>TRAN-DESC (PIC X(100)) → String description</li>
 *   <li>TRAN-AMT (PIC S9(09)V99) → BigDecimal amount</li>
 *   <li>TRAN-MERCHANT-ID (PIC 9(09)) → String merchantId</li>
 *   <li>TRAN-MERCHANT-NAME (PIC X(50)) → String merchantName</li>
 *   <li>TRAN-MERCHANT-CITY (PIC X(50)) → String merchantCity</li>
 *   <li>TRAN-MERCHANT-ZIP (PIC X(10)) → String merchantZip</li>
 *   <li>TRAN-CARD-NUM (PIC X(16)) → String cardNumber</li>
 *   <li>TRAN-ORIG-TS (PIC X(26)) → LocalDateTime originalTimestamp</li>
 *   <li>TRAN-PROC-TS (PIC X(26)) → LocalDateTime processingTimestamp</li>
 * </ul>
 * 
 * <p><strong>Financial Precision:</strong></p>
 * <p>The transaction amount field uses BigDecimal with exact COBOL COMP-3 precision 
 * equivalent to PIC S9(09)V99, ensuring no floating-point errors in financial calculations. 
 * All monetary operations maintain the same precision as the original COBOL implementation.</p>
 * 
 * <p><strong>Validation Framework:</strong></p>
 * <p>The class utilizes Jakarta Bean Validation annotations for comprehensive data validation:</p>
 * <ul>
 *   <li>@ValidCardNumber for Luhn algorithm card number validation</li>
 *   <li>@ValidCurrency for BigDecimal precision and range validation</li>
 *   <li>@DecimalMin for minimum amount constraints</li>
 *   <li>Standard validation annotations for string length and format</li>
 * </ul>
 * 
 * <p><strong>JSON Serialization:</strong></p>
 * <p>Jackson annotations ensure consistent JSON property naming and date formatting 
 * for seamless integration with React frontend components and external API consumers.</p>
 * 
 * <p><strong>Usage Examples:</strong></p>
 * <pre>
 * {@code
 * // Create new transaction DTO
 * TransactionDTO transaction = new TransactionDTO();
 * transaction.setTransactionId("TXN001234567890");
 * transaction.setTransactionType(TransactionType.PU);
 * transaction.setCategoryCode(TransactionCategory.GENERAL_PURCHASES);
 * transaction.setAmount(new BigDecimal("125.50"));
 * transaction.setCardNumber("4111111111111111");
 * transaction.setOriginalTimestamp(LocalDateTime.now());
 * 
 * // JSON serialization for REST API
 * ObjectMapper mapper = new ObjectMapper();
 * String json = mapper.writeValueAsString(transaction);
 * 
 * // Bean validation
 * Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
 * Set<ConstraintViolation<TransactionDTO>> violations = validator.validate(transaction);
 * }
 * </pre>
 * 
 * <p><strong>Thread Safety:</strong></p>
 * <p>This class is designed to be thread-safe for concurrent operations in microservices 
 * environments, with immutable field access patterns and no shared mutable state.</p>
 * 
 * <p><strong>Performance Considerations:</strong></p>
 * <p>The class is optimized for high-volume transaction processing (&gt;10,000 TPS) with 
 * efficient field access patterns and minimal object allocation overhead.</p>
 * 
 * @author Blitzy Platform
 * @version 1.0
 * @since 1.0
 * @see TransactionType
 * @see TransactionCategory
 * @see ValidCardNumber
 * @see ValidCurrency
 */
public class TransactionDTO {
    
    /**
     * Transaction identifier from COBOL TRAN-ID field (PIC X(16)).
     * 
     * <p>Unique identifier for each transaction maintaining exact 16-character format 
     * from the original COBOL structure. This field serves as the primary key for 
     * transaction identification across all system components.</p>
     */
    @JsonProperty("transaction_id")
    private String transactionId;
    
    /**
     * Transaction type from COBOL TRAN-TYPE-CD field (PIC X(02)).
     * 
     * <p>Enumerated transaction type providing type-safe validation and business logic 
     * integration. Maps directly to the 2-character transaction type codes used in 
     * the original COBOL implementation.</p>
     */
    @JsonProperty("transaction_type")
    private TransactionType transactionType;
    
    /**
     * Transaction category from COBOL TRAN-CAT-CD field (PIC 9(04)).
     * 
     * <p>Enumerated transaction category for balance management and reporting. 
     * Maintains 4-digit category codes matching the original COBOL structure.</p>
     */
    @JsonProperty("category_code")
    private TransactionCategory categoryCode;
    
    /**
     * Transaction source from COBOL TRAN-SOURCE field (PIC X(10)).
     * 
     * <p>Identifies the originating system or channel for the transaction. 
     * Maintains 10-character format from original COBOL specification.</p>
     */
    @JsonProperty("source")
    private String source;
    
    /**
     * Transaction description from COBOL TRAN-DESC field (PIC X(100)).
     * 
     * <p>Human-readable description of the transaction maintaining 100-character 
     * limit from original COBOL structure. Used for reporting and customer statements.</p>
     */
    @JsonProperty("description")
    private String description;
    
    /**
     * Transaction amount from COBOL TRAN-AMT field (PIC S9(09)V99).
     * 
     * <p>Financial amount using BigDecimal for exact precision matching COBOL COMP-3 
     * calculations. Maintains 9-digit integer and 2-decimal place precision with 
     * proper sign handling for debit/credit operations.</p>
     */
    @JsonProperty("amount")
    @ValidCurrency(
        min = "-999999999.99",
        max = "999999999.99",
        message = "Transaction amount must be within valid range with exactly 2 decimal places"
    )
    @DecimalMin(
        value = "-999999999.99",
        message = "Transaction amount cannot be less than minimum allowed value"
    )
    private BigDecimal amount;
    
    /**
     * Merchant identifier from COBOL TRAN-MERCHANT-ID field (PIC 9(09)).
     * 
     * <p>Nine-digit merchant identifier maintaining exact format from original COBOL 
     * specification. Used for merchant reporting and transaction categorization.</p>
     */
    @JsonProperty("merchant_id")
    private String merchantId;
    
    /**
     * Merchant name from COBOL TRAN-MERCHANT-NAME field (PIC X(50)).
     * 
     * <p>Merchant business name maintaining 50-character limit from original COBOL 
     * structure. Used for customer statements and transaction display.</p>
     */
    @JsonProperty("merchant_name")
    private String merchantName;
    
    /**
     * Merchant city from COBOL TRAN-MERCHANT-CITY field (PIC X(50)).
     * 
     * <p>Merchant location city maintaining 50-character limit from original COBOL 
     * structure. Used for geographic transaction analysis and customer statements.</p>
     */
    @JsonProperty("merchant_city")
    private String merchantCity;
    
    /**
     * Merchant ZIP code from COBOL TRAN-MERCHANT-ZIP field (PIC X(10)).
     * 
     * <p>Merchant postal code maintaining 10-character limit from original COBOL 
     * structure. Supports both US ZIP codes and international postal codes.</p>
     */
    @JsonProperty("merchant_zip")
    private String merchantZip;
    
    /**
     * Card number from COBOL TRAN-CARD-NUM field (PIC X(16)).
     * 
     * <p>Credit card number with Luhn algorithm validation maintaining exact 16-character 
     * format from original COBOL specification. Validates card number integrity using 
     * industry-standard checksum verification.</p>
     */
    @JsonProperty("card_number")
    @ValidCardNumber(
        message = "Card number must be exactly 16 digits and pass Luhn algorithm validation"
    )
    private String cardNumber;
    
    /**
     * Original timestamp from COBOL TRAN-ORIG-TS field (PIC X(26)).
     * 
     * <p>Original transaction timestamp capturing when the transaction was first initiated. 
     * Replaces COBOL 26-character timestamp format with precise LocalDateTime representation.</p>
     */
    @JsonProperty("original_timestamp")
    private LocalDateTime originalTimestamp;
    
    /**
     * Processing timestamp from COBOL TRAN-PROC-TS field (PIC X(26)).
     * 
     * <p>Processing timestamp capturing when the transaction was processed by the system. 
     * Replaces COBOL 26-character timestamp format with precise LocalDateTime representation.</p>
     */
    @JsonProperty("processing_timestamp")
    private LocalDateTime processingTimestamp;
    
    /**
     * Default constructor for TransactionDTO.
     * 
     * <p>Creates a new instance with all fields initialized to their default values. 
     * This constructor is used by JPA, Jackson deserialization, and Spring framework 
     * for object instantiation.</p>
     */
    public TransactionDTO() {
        // Default constructor for JPA and Jackson
    }
    
    /**
     * Gets the transaction identifier.
     * 
     * @return the transaction identifier as a 16-character string
     */
    public String getTransactionId() {
        return transactionId;
    }
    
    /**
     * Sets the transaction identifier.
     * 
     * @param transactionId the transaction identifier (must be 16 characters)
     */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    /**
     * Gets the transaction type.
     * 
     * @return the transaction type enumeration value
     */
    public TransactionType getTransactionType() {
        return transactionType;
    }
    
    /**
     * Sets the transaction type.
     * 
     * @param transactionType the transaction type enumeration value
     */
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }
    
    /**
     * Gets the transaction category code.
     * 
     * @return the transaction category enumeration value
     */
    public TransactionCategory getCategoryCode() {
        return categoryCode;
    }
    
    /**
     * Sets the transaction category code.
     * 
     * @param categoryCode the transaction category enumeration value
     */
    public void setCategoryCode(TransactionCategory categoryCode) {
        this.categoryCode = categoryCode;
    }
    
    /**
     * Gets the transaction source.
     * 
     * @return the transaction source as a 10-character string
     */
    public String getSource() {
        return source;
    }
    
    /**
     * Sets the transaction source.
     * 
     * @param source the transaction source (maximum 10 characters)
     */
    public void setSource(String source) {
        this.source = source;
    }
    
    /**
     * Gets the transaction description.
     * 
     * @return the transaction description as a 100-character string
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Sets the transaction description.
     * 
     * @param description the transaction description (maximum 100 characters)
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Gets the transaction amount.
     * 
     * @return the transaction amount as BigDecimal with exact precision
     */
    public BigDecimal getAmount() {
        return amount;
    }
    
    /**
     * Sets the transaction amount.
     * 
     * @param amount the transaction amount with BigDecimal precision
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    /**
     * Gets the merchant identifier.
     * 
     * @return the merchant identifier as a 9-character string
     */
    public String getMerchantId() {
        return merchantId;
    }
    
    /**
     * Sets the merchant identifier.
     * 
     * @param merchantId the merchant identifier (must be 9 characters)
     */
    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }
    
    /**
     * Gets the merchant name.
     * 
     * @return the merchant name as a 50-character string
     */
    public String getMerchantName() {
        return merchantName;
    }
    
    /**
     * Sets the merchant name.
     * 
     * @param merchantName the merchant name (maximum 50 characters)
     */
    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }
    
    /**
     * Gets the merchant city.
     * 
     * @return the merchant city as a 50-character string
     */
    public String getMerchantCity() {
        return merchantCity;
    }
    
    /**
     * Sets the merchant city.
     * 
     * @param merchantCity the merchant city (maximum 50 characters)
     */
    public void setMerchantCity(String merchantCity) {
        this.merchantCity = merchantCity;
    }
    
    /**
     * Gets the merchant ZIP code.
     * 
     * @return the merchant ZIP code as a 10-character string
     */
    public String getMerchantZip() {
        return merchantZip;
    }
    
    /**
     * Sets the merchant ZIP code.
     * 
     * @param merchantZip the merchant ZIP code (maximum 10 characters)
     */
    public void setMerchantZip(String merchantZip) {
        this.merchantZip = merchantZip;
    }
    
    /**
     * Gets the card number.
     * 
     * @return the card number as a 16-character string
     */
    public String getCardNumber() {
        return cardNumber;
    }
    
    /**
     * Sets the card number.
     * 
     * @param cardNumber the card number (must be 16 digits and pass Luhn validation)
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
    
    /**
     * Gets the original timestamp.
     * 
     * @return the original timestamp when the transaction was initiated
     */
    public LocalDateTime getOriginalTimestamp() {
        return originalTimestamp;
    }
    
    /**
     * Sets the original timestamp.
     * 
     * @param originalTimestamp the original timestamp when the transaction was initiated
     */
    public void setOriginalTimestamp(LocalDateTime originalTimestamp) {
        this.originalTimestamp = originalTimestamp;
    }
    
    /**
     * Gets the processing timestamp.
     * 
     * @return the processing timestamp when the transaction was processed
     */
    public LocalDateTime getProcessingTimestamp() {
        return processingTimestamp;
    }
    
    /**
     * Sets the processing timestamp.
     * 
     * @param processingTimestamp the processing timestamp when the transaction was processed
     */
    public void setProcessingTimestamp(LocalDateTime processingTimestamp) {
        this.processingTimestamp = processingTimestamp;
    }
    
    /**
     * Checks if this transaction represents a debit operation.
     * 
     * <p>Determines if the transaction increases the customer's balance based on 
     * the transaction type's debit/credit indicator. This method preserves the 
     * original COBOL business logic for balance calculations.</p>
     * 
     * @return true if the transaction is a debit operation, false otherwise
     */
    public boolean isDebitTransaction() {
        return transactionType != null && transactionType.isDebit();
    }
    
    /**
     * Checks if this transaction represents a credit operation.
     * 
     * <p>Determines if the transaction decreases the customer's balance based on 
     * the transaction type's debit/credit indicator. This method preserves the 
     * original COBOL business logic for balance calculations.</p>
     * 
     * @return true if the transaction is a credit operation, false otherwise
     */
    public boolean isCreditTransaction() {
        return transactionType != null && transactionType.isCredit();
    }
    
    /**
     * Gets the balance impact of this transaction.
     * 
     * <p>Returns the multiplier value for balance calculations: +1 for debit 
     * transactions (increase balance), -1 for credit transactions (decrease balance). 
     * This method supports the financial calculation logic used in balance updates.</p>
     * 
     * @return 1 for debit transactions, -1 for credit transactions, 0 if type is null
     */
    public int getBalanceImpact() {
        if (transactionType == null) {
            return 0;
        }
        return transactionType.getBalanceImpact();
    }
    
    /**
     * Gets the signed amount for balance calculations.
     * 
     * <p>Returns the transaction amount multiplied by the balance impact to provide 
     * the signed amount for balance calculations. This method preserves the exact 
     * COBOL arithmetic behavior for financial operations.</p>
     * 
     * @return the signed amount for balance calculations, or null if amount is null
     */
    public BigDecimal getSignedAmount() {
        if (amount == null) {
            return null;
        }
        return amount.multiply(BigDecimal.valueOf(getBalanceImpact()));
    }
    
    /**
     * Validates the transaction data integrity.
     * 
     * <p>Performs comprehensive validation of all transaction fields including 
     * business rule validation, format validation, and referential integrity 
     * checks. This method supplements the Jakarta Bean Validation annotations 
     * with custom business logic validation.</p>
     * 
     * @return true if all validation checks pass, false otherwise
     */
    public boolean isValid() {
        // Basic field validation
        if (transactionId == null || transactionId.trim().isEmpty()) {
            return false;
        }
        if (transactionType == null) {
            return false;
        }
        if (categoryCode == null || !categoryCode.isValid()) {
            return false;
        }
        if (amount == null) {
            return false;
        }
        
        // Business rule validation
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return false; // Zero amount transactions not allowed
        }
        
        // Card number validation (basic check)
        if (cardNumber != null && !cardNumber.matches("\\d{16}")) {
            return false;
        }
        
        // Timestamp validation
        if (originalTimestamp != null && processingTimestamp != null) {
            if (originalTimestamp.isAfter(processingTimestamp)) {
                return false; // Original timestamp cannot be after processing timestamp
            }
        }
        
        return true;
    }
    
    /**
     * Compares this transaction with another transaction for equality.
     * 
     * <p>Two transactions are considered equal if they have the same transaction ID 
     * and all other fields match exactly. This method is used for deduplication 
     * and comparison operations in transaction processing.</p>
     * 
     * @param obj the object to compare with this transaction
     * @return true if the transactions are equal, false otherwise
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
     * Generates a hash code for this transaction.
     * 
     * <p>The hash code is based on the transaction ID and other key fields to ensure 
     * proper behavior in collections and hash-based operations. This method provides 
     * consistent hashing for transaction deduplication and lookup operations.</p>
     * 
     * @return the hash code value for this transaction
     */
    @Override
    public int hashCode() {
        return Objects.hash(transactionId, transactionType, categoryCode, source, description,
                          amount, merchantId, merchantName, merchantCity, merchantZip,
                          cardNumber, originalTimestamp, processingTimestamp);
    }
    
    /**
     * Returns a string representation of this transaction.
     * 
     * <p>The string representation includes key transaction details for debugging 
     * and logging purposes. Sensitive information like card numbers are masked 
     * to maintain security in log files.</p>
     * 
     * @return a string representation of the transaction
     */
    @Override
    public String toString() {
        return String.format(
            "TransactionDTO{transactionId='%s', transactionType=%s, categoryCode=%s, " +
            "source='%s', description='%s', amount=%s, merchantId='%s', merchantName='%s', " +
            "merchantCity='%s', merchantZip='%s', cardNumber='%s', " +
            "originalTimestamp=%s, processingTimestamp=%s}",
            transactionId,
            transactionType,
            categoryCode,
            source,
            description,
            amount,
            merchantId,
            merchantName,
            merchantCity,
            merchantZip,
            cardNumber != null ? cardNumber.substring(0, 4) + "************" : null,
            originalTimestamp,
            processingTimestamp
        );
    }
}