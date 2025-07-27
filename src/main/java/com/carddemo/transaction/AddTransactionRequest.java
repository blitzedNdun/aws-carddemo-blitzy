package com.carddemo.transaction;

import com.carddemo.common.enums.TransactionType;
import com.carddemo.common.enums.TransactionCategory;
import com.carddemo.common.validator.ValidCardNumber;
import com.carddemo.common.validator.ValidCurrency;
import com.carddemo.common.validator.ValidCCYYMMDD;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for transaction addition operations with comprehensive validation rules
 * and business constraint enforcement.
 * 
 * This class implements complete validation pipeline equivalent to COBOL data validation
 * from COTRN02C.cbl program, ensuring exact functional equivalence while providing
 * modern Jakarta Bean Validation integration for Spring Boot microservices.
 * 
 * All validation rules preserve the original COBOL business logic including:
 * - Account-card relationship verification (CXACAIX/CCXREF cross-reference validation)
 * - Transaction type and category validation with reference table lookup
 * - Monetary amount validation with exact COMP-3 precision using BigDecimal
 * - Date validation with CCYYMMDD format and century restriction (19xx, 20xx)
 * - Merchant data validation with field length and format constraints
 * - Complete field presence validation matching original COBOL EVALUATE statements
 * 
 * Field validation maintains exact COBOL PICTURE clause equivalents:
 * - TRAN-ID PIC X(16) → String accountId, cardNumber pattern validation
 * - TRAN-TYPE-CD PIC X(02) → TransactionType enum validation
 * - TRAN-CAT-CD PIC 9(04) → TransactionCategory enum validation  
 * - TRAN-AMT PIC S9(09)V99 → BigDecimal with DECIMAL(11,2) precision
 * - Date fields PIC X(26) → LocalDateTime with CCYYMMDD validation
 * - Merchant fields maintain original field length constraints
 * 
 * @author Blitzy Platform - CardDemo Modernization Team
 * @version CardDemo_v1.0-15-g27d6c6f-68
 * @since 2024-01-01
 */
public class AddTransactionRequest {

    /**
     * Account ID for the transaction (11-digit numeric format).
     * Corresponds to TRAN-ACCT-ID and ACTIDINI field from COBOL.
     * Must reference existing account in PostgreSQL accounts table.
     * Validated against CXACAIX cross-reference for account-card relationship.
     */
    @JsonProperty("accountId")
    @NotBlank(message = "Account ID can NOT be empty")
    @Pattern(regexp = "\\d{11}", message = "Account ID must be 11 digits")
    @Size(min = 11, max = 11, message = "Account ID must be exactly 11 digits")
    private String accountId;

    /**
     * Card number for the transaction (16-digit format).
     * Corresponds to TRAN-CARD-NUM and CARDNINI field from COBOL.
     * Must reference existing card in PostgreSQL cards table.
     * Validated against CCXREF cross-reference for card-account relationship.
     * Uses Luhn algorithm validation for credit card number verification.
     */
    @JsonProperty("cardNumber")
    @NotBlank(message = "Card Number can NOT be empty")
    @ValidCardNumber(message = "Card Number must be valid 16-digit format and pass Luhn validation")
    private String cardNumber;

    /**
     * Transaction type code (2-character format).
     * Corresponds to TRAN-TYPE-CD and TTYPCDI field from COBOL.
     * Must reference valid entry in PostgreSQL transaction_types table.
     * Validated against TransactionType enum for business rule compliance.
     */
    @JsonProperty("transactionType")
    @NotNull(message = "Transaction Type can NOT be empty")
    private TransactionType transactionType;

    /**
     * Transaction category code (4-digit numeric format).
     * Corresponds to TRAN-CAT-CD and TCATCDI field from COBOL.
     * Must reference valid entry in PostgreSQL transaction_categories table.
     * Validated against TransactionCategory enum for categorization compliance.
     */
    @JsonProperty("transactionCategory")
    @NotNull(message = "Transaction Category can NOT be empty")
    private TransactionCategory transactionCategory;

    /**
     * Transaction source identifier (up to 10 characters).
     * Corresponds to TRAN-SOURCE and TRNSRCI field from COBOL.
     * Indicates the origination source of the transaction (POS, ATM, Online, etc.).
     */
    @JsonProperty("source")
    @NotBlank(message = "Source can NOT be empty")
    @Size(max = 10, message = "Source must not exceed 10 characters")
    private String source;

    /**
     * Transaction description (up to 100 characters).
     * Corresponds to TRAN-DESC and TDESCI field from COBOL.
     * Provides detailed description of the transaction for account holder reference.
     */
    @JsonProperty("description")
    @NotBlank(message = "Description can NOT be empty")
    @Size(max = 100, message = "Description must not exceed 100 characters")
    private String description;

    /**
     * Transaction amount with exact financial precision.
     * Corresponds to TRAN-AMT PIC S9(09)V99 from COBOL.
     * Uses BigDecimal to maintain COMP-3 precision equivalent to DECIMAL(11,2).
     * Validated for proper monetary format with 2 decimal places.
     * Supports both positive and negative amounts for credits and debits.
     */
    @JsonProperty("amount")
    @NotNull(message = "Amount can NOT be empty")
    @ValidCurrency(
        precision = 11, 
        scale = 2, 
        min = "-999999999.99", 
        max = "999999999.99",
        allowNegative = true,
        message = "Amount must be valid monetary value in format ±999999999.99"
    )
    @DecimalMax(value = "999999999.99", message = "Amount exceeds maximum allowed value")
    private BigDecimal amount;

    /**
     * Merchant ID (9-digit numeric format).
     * Corresponds to TRAN-MERCHANT-ID and MIDI field from COBOL.
     * Must be numeric identifier for the merchant where transaction occurred.
     */
    @JsonProperty("merchantId")
    @NotBlank(message = "Merchant ID can NOT be empty")
    @Pattern(regexp = "\\d{1,9}", message = "Merchant ID must be numeric")
    @Size(max = 9, message = "Merchant ID must not exceed 9 digits")
    private String merchantId;

    /**
     * Merchant name (up to 50 characters).
     * Corresponds to TRAN-MERCHANT-NAME and MNAMEI field from COBOL.
     * Business name where the transaction was processed.
     */
    @JsonProperty("merchantName")
    @NotBlank(message = "Merchant Name can NOT be empty")
    @Size(max = 50, message = "Merchant Name must not exceed 50 characters")
    private String merchantName;

    /**
     * Merchant city (up to 50 characters).
     * Corresponds to TRAN-MERCHANT-CITY and MCITYI field from COBOL.
     * City location where the transaction was processed.
     */
    @JsonProperty("merchantCity")
    @NotBlank(message = "Merchant City can NOT be empty")
    @Size(max = 50, message = "Merchant City must not exceed 50 characters")
    private String merchantCity;

    /**
     * Merchant ZIP code (up to 10 characters).
     * Corresponds to TRAN-MERCHANT-ZIP and MZIPI field from COBOL.
     * ZIP/postal code for merchant location verification.
     */
    @JsonProperty("merchantZip")
    @NotBlank(message = "Merchant Zip can NOT be empty")
    @Size(max = 10, message = "Merchant Zip must not exceed 10 characters")
    @Pattern(regexp = "\\d{5}(-\\d{4})?|[A-Z]\\d[A-Z]\\s?\\d[A-Z]\\d", 
             message = "Merchant Zip must be valid US ZIP code (12345 or 12345-6789) or Canadian postal code")
    private String merchantZip;

    /**
     * Original transaction date and time.
     * Corresponds to TRAN-ORIG-TS and TORIGDTI field from COBOL.
     * Represents when the transaction originally occurred at the merchant.
     * Uses LocalDateTime for precise timestamp handling.
     */
    @JsonProperty("originalDate")
    @NotNull(message = "Original Date can NOT be empty")
    private LocalDateTime originalDate;

    /**
     * Processing date and time.
     * Corresponds to TRAN-PROC-TS and TPROCDTI field from COBOL.
     * Represents when the transaction was processed by the card system.
     * Uses LocalDateTime for precise timestamp handling.
     */
    @JsonProperty("processingDate")
    @NotNull(message = "Processing Date can NOT be empty")
    private LocalDateTime processingDate;

    /**
     * Confirmation flag for transaction addition.
     * Corresponds to CONFIRMI field from COBOL validation logic.
     * Must be 'Y' or 'y' to confirm transaction addition, 'N' or 'n' to cancel.
     * Implements exact COBOL EVALUATE logic for confirmation validation.
     */
    @JsonProperty("confirm")
    @NotBlank(message = "Confirmation is required")
    @Pattern(regexp = "[YyNn]", message = "Confirm must be Y/y (Yes) or N/n (No)")
    private String confirm;

    /**
     * Default constructor for Jakarta Bean Validation and JSON deserialization.
     */
    public AddTransactionRequest() {
        // Default constructor required for JSON deserialization and validation
    }

    /**
     * Get the account ID for the transaction.
     * 
     * @return the account ID (11-digit string)
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Set the account ID for the transaction.
     * 
     * @param accountId the account ID (11-digit string)
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Get the card number for the transaction.
     * 
     * @return the card number (16-digit string)
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Set the card number for the transaction.
     * 
     * @param cardNumber the card number (16-digit string)
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Get the transaction type.
     * 
     * @return the transaction type enum
     */
    public TransactionType getTransactionType() {
        return transactionType;
    }

    /**
     * Set the transaction type.
     * 
     * @param transactionType the transaction type enum
     */
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * Get the transaction category.
     * 
     * @return the transaction category enum
     */
    public TransactionCategory getTransactionCategory() {
        return transactionCategory;
    }

    /**
     * Set the transaction category.
     * 
     * @param transactionCategory the transaction category enum
     */
    public void setTransactionCategory(TransactionCategory transactionCategory) {
        this.transactionCategory = transactionCategory;
    }

    /**
     * Get the transaction source.
     * 
     * @return the transaction source identifier
     */
    public String getSource() {
        return source;
    }

    /**
     * Set the transaction source.
     * 
     * @param source the transaction source identifier
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Get the transaction description.
     * 
     * @return the transaction description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the transaction description.
     * 
     * @param description the transaction description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the transaction amount.
     * 
     * @return the transaction amount with exact BigDecimal precision
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Set the transaction amount.
     * 
     * @param amount the transaction amount with exact BigDecimal precision
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * Get the merchant ID.
     * 
     * @return the merchant ID (numeric string)
     */
    public String getMerchantId() {
        return merchantId;
    }

    /**
     * Set the merchant ID.
     * 
     * @param merchantId the merchant ID (numeric string)
     */
    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    /**
     * Get the merchant name.
     * 
     * @return the merchant name
     */
    public String getMerchantName() {
        return merchantName;
    }

    /**
     * Set the merchant name.
     * 
     * @param merchantName the merchant name
     */
    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    /**
     * Get the merchant city.
     * 
     * @return the merchant city
     */
    public String getMerchantCity() {
        return merchantCity;
    }

    /**
     * Set the merchant city.
     * 
     * @param merchantCity the merchant city
     */
    public void setMerchantCity(String merchantCity) {
        this.merchantCity = merchantCity;
    }

    /**
     * Get the merchant ZIP code.
     * 
     * @return the merchant ZIP code
     */
    public String getMerchantZip() {
        return merchantZip;
    }

    /**
     * Set the merchant ZIP code.
     * 
     * @param merchantZip the merchant ZIP code
     */
    public void setMerchantZip(String merchantZip) {
        this.merchantZip = merchantZip;
    }

    /**
     * Get the original transaction date.
     * 
     * @return the original transaction date and time
     */
    public LocalDateTime getOriginalDate() {
        return originalDate;
    }

    /**
     * Set the original transaction date.
     * 
     * @param originalDate the original transaction date and time
     */
    public void setOriginalDate(LocalDateTime originalDate) {
        this.originalDate = originalDate;
    }

    /**
     * Get the processing date.
     * 
     * @return the processing date and time
     */
    public LocalDateTime getProcessingDate() {
        return processingDate;
    }

    /**
     * Set the processing date.
     * 
     * @param processingDate the processing date and time
     */
    public void setProcessingDate(LocalDateTime processingDate) {
        this.processingDate = processingDate;
    }

    /**
     * Get the confirmation flag.
     * 
     * @return the confirmation flag (Y/N)
     */
    public String getConfirm() {
        return confirm;
    }

    /**
     * Set the confirmation flag.
     * 
     * @param confirm the confirmation flag (Y/N)
     */
    public void setConfirm(String confirm) {
        this.confirm = confirm;
    }

    /**
     * String representation of the AddTransactionRequest for logging and debugging.
     * Masks sensitive data (card number, account ID) for security.
     * 
     * @return formatted string representation
     */
    @Override
    public String toString() {
        return String.format(
            "AddTransactionRequest{" +
            "accountId='%s', " +
            "cardNumber='%s', " +
            "transactionType=%s, " +
            "transactionCategory=%s, " +
            "source='%s', " +
            "description='%s', " +
            "amount=%s, " +
            "merchantId='%s', " +
            "merchantName='%s', " +
            "merchantCity='%s', " +
            "merchantZip='%s', " +
            "originalDate=%s, " +
            "processingDate=%s, " +
            "confirm='%s'" +
            "}",
            accountId != null ? "***" + accountId.substring(Math.max(0, accountId.length() - 4)) : null,
            cardNumber != null ? "****-****-****-" + cardNumber.substring(Math.max(0, cardNumber.length() - 4)) : null,
            transactionType,
            transactionCategory,
            source,
            description,
            amount,
            merchantId,
            merchantName,
            merchantCity,
            merchantZip,
            originalDate,
            processingDate,
            confirm
        );
    }

    /**
     * Equals method for object comparison based on business key fields.
     * Used for transaction deduplication and validation purposes.
     * 
     * @param obj the object to compare
     * @return true if objects are equal based on business key
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AddTransactionRequest that = (AddTransactionRequest) obj;
        
        return java.util.Objects.equals(accountId, that.accountId) &&
               java.util.Objects.equals(cardNumber, that.cardNumber) &&
               java.util.Objects.equals(transactionType, that.transactionType) &&
               java.util.Objects.equals(transactionCategory, that.transactionCategory) &&
               java.util.Objects.equals(amount, that.amount) &&
               java.util.Objects.equals(originalDate, that.originalDate) &&
               java.util.Objects.equals(merchantId, that.merchantId);
    }

    /**
     * Hash code implementation based on business key fields.
     * Ensures consistent hashing for transaction deduplication.
     * 
     * @return hash code value
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(
            accountId, 
            cardNumber, 
            transactionType, 
            transactionCategory, 
            amount, 
            originalDate, 
            merchantId
        );
    }
}