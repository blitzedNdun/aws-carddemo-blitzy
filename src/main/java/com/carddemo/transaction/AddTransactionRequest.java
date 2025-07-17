/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
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
import com.carddemo.common.validator.ValidCCYYMMDD;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for transaction addition operations with comprehensive validation rules 
 * and business constraint enforcement.
 * 
 * This class provides a complete validation pipeline equivalent to COBOL data validation 
 * from COTRN02C.cbl with detailed error reporting and business rule enforcement.
 * 
 * Key Features:
 * - Comprehensive Jakarta Bean Validation annotations for all transaction fields
 * - Amount limits validation with BigDecimal precision matching COBOL COMP-3 format
 * - Date validation with CCYYMMDD format enforcement and century validation
 * - Text field constraints equivalent to COBOL field validation
 * - Account-card relationship verification and transaction type/category validation
 * - Merchant data validation including name, location, and ZIP code format checking
 * 
 * Field Mapping from COBOL COTRN02C.cbl:
 * - ACTIDINI → accountId (11-digit account identifier)
 * - CARDNINI → cardNumber (16-digit card number with Luhn validation)
 * - TTYPCDI → transactionType (2-character transaction type code)
 * - TCATCDI → transactionCategory (4-digit category code)
 * - TRNSRCI → source (10-character transaction source identifier)
 * - TDESCI → description (100-character transaction description)
 * - TRNAMTI → amount (decimal amount with -99999999.99 format)
 * - MIDI → merchantId (9-digit merchant identifier)
 * - MNAMEI → merchantName (50-character merchant name)
 * - MCITYI → merchantCity (50-character merchant city)
 * - MZIPI → merchantZip (10-character ZIP code)
 * - TORIGDTI → originalDate (YYYY-MM-DD format)
 * - TPROCDTI → processingDate (YYYY-MM-DD format)
 * - CONFIRMI → confirm (Y/N confirmation flag)
 * 
 * Business Rule Validation:
 * - All required fields must be populated (equivalent to COBOL "can NOT be empty" checks)
 * - Numeric fields must be properly formatted and within business limits
 * - Date fields must be valid calendar dates with century validation
 * - Amount field must follow exact decimal format with sign indication
 * - Transaction type and category must exist in reference tables
 * - Merchant information must be complete and properly formatted
 * 
 * Performance Requirements:
 * - Validation processing must complete within sub-200ms response time constraints
 * - BigDecimal operations maintain exact financial precision equivalent to COBOL COMP-3
 * - Field validation supports 10,000+ TPS throughput requirements
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since CardDemo v1.0-15-g27d6c6f-68
 */
public class AddTransactionRequest {

    /**
     * Account ID for the transaction.
     * 
     * Corresponds to COBOL ACTIDINI field (11-digit account identifier).
     * Must be numeric and exactly 11 digits to maintain account numbering consistency.
     * Cross-reference validation ensures account exists in ACCTDAT table.
     */
    @NotNull(message = "Account ID cannot be null")
    @NotBlank(message = "Account ID can NOT be empty")
    @Pattern(regexp = "^\\d{11}$", message = "Account ID must be numeric and exactly 11 digits")
    @JsonProperty("accountId")
    private String accountId;

    /**
     * Credit card number for the transaction.
     * 
     * Corresponds to COBOL CARDNINI field (16-digit card number).
     * Must pass Luhn algorithm validation and cross-reference with CARDDAT table.
     * Account-card relationship verification ensures card belongs to specified account.
     */
    @NotNull(message = "Card Number cannot be null")
    @NotBlank(message = "Card Number can NOT be empty")
    @ValidCardNumber(message = "Card Number must be a valid 16-digit credit card number")
    @JsonProperty("cardNumber")
    private String cardNumber;

    /**
     * Transaction type classification.
     * 
     * Corresponds to COBOL TTYPCDI field (2-character transaction type code).
     * Must be numeric and exist in TRANTYPE reference table.
     * Used for business logic routing and balance calculation rules.
     */
    @NotNull(message = "Transaction Type cannot be null")
    @JsonProperty("transactionType")
    private TransactionType transactionType;

    /**
     * Transaction category classification.
     * 
     * Corresponds to COBOL TCATCDI field (4-digit category code).
     * Must be numeric and exist in TRANCATG reference table.
     * Used for reporting and balance management categorization.
     */
    @NotNull(message = "Transaction Category cannot be null")
    @JsonProperty("transactionCategory")
    private TransactionCategory transactionCategory;

    /**
     * Transaction source identifier.
     * 
     * Corresponds to COBOL TRNSRCI field (10-character source identifier).
     * Identifies the originating system or channel for the transaction.
     */
    @NotNull(message = "Source cannot be null")
    @NotBlank(message = "Source can NOT be empty")
    @Size(max = 10, message = "Source must not exceed 10 characters")
    @JsonProperty("source")
    private String source;

    /**
     * Transaction description.
     * 
     * Corresponds to COBOL TDESCI field (100-character description).
     * Free-form text describing the nature of the transaction.
     */
    @NotNull(message = "Description cannot be null")
    @NotBlank(message = "Description can NOT be empty")
    @Size(max = 100, message = "Description must not exceed 100 characters")
    @JsonProperty("description")
    private String description;

    /**
     * Transaction amount with exact decimal precision.
     * 
     * Corresponds to COBOL TRNAMTI field (decimal amount with -99999999.99 format).
     * Must maintain exact financial precision equivalent to COBOL COMP-3 calculations.
     * Positive amounts represent debits, negative amounts represent credits.
     */
    @NotNull(message = "Amount cannot be null")
    @ValidCurrency(
        min = "-99999999.99",
        max = "99999999.99",
        precision = 11,
        scale = 2,
        message = "Amount must be in format -99999999.99 with exact decimal precision"
    )
    @DecimalMax(value = "99999999.99", message = "Amount exceeds maximum allowed value")
    @JsonProperty("amount")
    private BigDecimal amount;

    /**
     * Merchant identifier for the transaction.
     * 
     * Corresponds to COBOL MIDI field (9-digit merchant identifier).
     * Must be numeric and identify a valid merchant in the system.
     */
    @NotNull(message = "Merchant ID cannot be null")
    @NotBlank(message = "Merchant ID can NOT be empty")
    @Pattern(regexp = "^\\d{9}$", message = "Merchant ID must be numeric and exactly 9 digits")
    @JsonProperty("merchantId")
    private String merchantId;

    /**
     * Merchant name for the transaction.
     * 
     * Corresponds to COBOL MNAMEI field (50-character merchant name).
     * Business name where the transaction occurred.
     */
    @NotNull(message = "Merchant Name cannot be null")
    @NotBlank(message = "Merchant Name can NOT be empty")
    @Size(max = 50, message = "Merchant Name must not exceed 50 characters")
    @JsonProperty("merchantName")
    private String merchantName;

    /**
     * Merchant city for the transaction.
     * 
     * Corresponds to COBOL MCITYI field (50-character merchant city).
     * City where the merchant is located.
     */
    @NotNull(message = "Merchant City cannot be null")
    @NotBlank(message = "Merchant City can NOT be empty")
    @Size(max = 50, message = "Merchant City must not exceed 50 characters")
    @JsonProperty("merchantCity")
    private String merchantCity;

    /**
     * Merchant ZIP code for the transaction.
     * 
     * Corresponds to COBOL MZIPI field (10-character ZIP code).
     * ZIP code where the merchant is located.
     */
    @NotNull(message = "Merchant Zip cannot be null")
    @NotBlank(message = "Merchant Zip can NOT be empty")
    @Size(max = 10, message = "Merchant Zip must not exceed 10 characters")
    @Pattern(regexp = "^\\d{5}(-\\d{4})?$", message = "Merchant Zip must be in format 12345 or 12345-6789")
    @JsonProperty("merchantZip")
    private String merchantZip;

    /**
     * Original transaction date.
     * 
     * Corresponds to COBOL TORIGDTI field (YYYY-MM-DD format).
     * The date when the transaction originally occurred.
     * Must be a valid calendar date with century validation (19xx or 20xx).
     */
    @NotNull(message = "Original Date cannot be null")
    @ValidCCYYMMDD(
        fieldName = "Original Date",
        message = "Original Date must be in CCYYMMDD format with valid century (19xx or 20xx), month (01-12), and day (01-31)"
    )
    @JsonProperty("originalDate")
    private LocalDateTime originalDate;

    /**
     * Processing date for the transaction.
     * 
     * Corresponds to COBOL TPROCDTI field (YYYY-MM-DD format).
     * The date when the transaction is being processed in the system.
     * Must be a valid calendar date with century validation (19xx or 20xx).
     */
    @NotNull(message = "Processing Date cannot be null")
    @ValidCCYYMMDD(
        fieldName = "Processing Date",
        message = "Processing Date must be in CCYYMMDD format with valid century (19xx or 20xx), month (01-12), and day (01-31)"
    )
    @JsonProperty("processingDate")
    private LocalDateTime processingDate;

    /**
     * Confirmation flag for transaction processing.
     * 
     * Corresponds to COBOL CONFIRMI field (Y/N confirmation flag).
     * Must be 'Y' or 'y' to proceed with transaction addition.
     * 'N', 'n', or blank values require user confirmation before processing.
     */
    @NotNull(message = "Confirm cannot be null")
    @Pattern(regexp = "^[YyNn]$", message = "Confirm must be Y or N")
    @JsonProperty("confirm")
    private String confirm;

    /**
     * Default constructor for AddTransactionRequest.
     * 
     * Creates an empty request object for population by Jackson deserialization
     * or manual field setting in service layer processing.
     */
    public AddTransactionRequest() {
        // Default constructor for Jackson deserialization
    }

    /**
     * Full constructor for AddTransactionRequest.
     * 
     * Creates a complete request object with all required fields populated.
     * Used for programmatic request creation in service layer or testing.
     * 
     * @param accountId 11-digit account identifier
     * @param cardNumber 16-digit card number
     * @param transactionType transaction type enumeration
     * @param transactionCategory transaction category enumeration
     * @param source transaction source identifier
     * @param description transaction description
     * @param amount transaction amount with exact decimal precision
     * @param merchantId 9-digit merchant identifier
     * @param merchantName merchant name
     * @param merchantCity merchant city
     * @param merchantZip merchant ZIP code
     * @param originalDate original transaction date
     * @param processingDate processing date
     * @param confirm confirmation flag
     */
    public AddTransactionRequest(String accountId, String cardNumber, TransactionType transactionType,
                                TransactionCategory transactionCategory, String source, String description,
                                BigDecimal amount, String merchantId, String merchantName, String merchantCity,
                                String merchantZip, LocalDateTime originalDate, LocalDateTime processingDate,
                                String confirm) {
        this.accountId = accountId;
        this.cardNumber = cardNumber;
        this.transactionType = transactionType;
        this.transactionCategory = transactionCategory;
        this.source = source;
        this.description = description;
        this.amount = amount;
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.merchantCity = merchantCity;
        this.merchantZip = merchantZip;
        this.originalDate = originalDate;
        this.processingDate = processingDate;
        this.confirm = confirm;
    }

    /**
     * Gets the account ID for the transaction.
     * 
     * @return 11-digit account identifier
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID for the transaction.
     * 
     * @param accountId 11-digit account identifier
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the credit card number for the transaction.
     * 
     * @return 16-digit card number
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the credit card number for the transaction.
     * 
     * @param cardNumber 16-digit card number
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Gets the transaction type classification.
     * 
     * @return transaction type enumeration
     */
    public TransactionType getTransactionType() {
        return transactionType;
    }

    /**
     * Sets the transaction type classification.
     * 
     * @param transactionType transaction type enumeration
     */
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * Gets the transaction category classification.
     * 
     * @return transaction category enumeration
     */
    public TransactionCategory getTransactionCategory() {
        return transactionCategory;
    }

    /**
     * Sets the transaction category classification.
     * 
     * @param transactionCategory transaction category enumeration
     */
    public void setTransactionCategory(TransactionCategory transactionCategory) {
        this.transactionCategory = transactionCategory;
    }

    /**
     * Gets the transaction source identifier.
     * 
     * @return transaction source identifier
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets the transaction source identifier.
     * 
     * @param source transaction source identifier
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Gets the transaction description.
     * 
     * @return transaction description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the transaction description.
     * 
     * @param description transaction description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the transaction amount with exact decimal precision.
     * 
     * @return transaction amount as BigDecimal
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Sets the transaction amount with exact decimal precision.
     * 
     * @param amount transaction amount as BigDecimal
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * Gets the merchant identifier for the transaction.
     * 
     * @return 9-digit merchant identifier
     */
    public String getMerchantId() {
        return merchantId;
    }

    /**
     * Sets the merchant identifier for the transaction.
     * 
     * @param merchantId 9-digit merchant identifier
     */
    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    /**
     * Gets the merchant name for the transaction.
     * 
     * @return merchant name
     */
    public String getMerchantName() {
        return merchantName;
    }

    /**
     * Sets the merchant name for the transaction.
     * 
     * @param merchantName merchant name
     */
    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    /**
     * Gets the merchant city for the transaction.
     * 
     * @return merchant city
     */
    public String getMerchantCity() {
        return merchantCity;
    }

    /**
     * Sets the merchant city for the transaction.
     * 
     * @param merchantCity merchant city
     */
    public void setMerchantCity(String merchantCity) {
        this.merchantCity = merchantCity;
    }

    /**
     * Gets the merchant ZIP code for the transaction.
     * 
     * @return merchant ZIP code
     */
    public String getMerchantZip() {
        return merchantZip;
    }

    /**
     * Sets the merchant ZIP code for the transaction.
     * 
     * @param merchantZip merchant ZIP code
     */
    public void setMerchantZip(String merchantZip) {
        this.merchantZip = merchantZip;
    }

    /**
     * Gets the original transaction date.
     * 
     * @return original transaction date
     */
    public LocalDateTime getOriginalDate() {
        return originalDate;
    }

    /**
     * Sets the original transaction date.
     * 
     * @param originalDate original transaction date
     */
    public void setOriginalDate(LocalDateTime originalDate) {
        this.originalDate = originalDate;
    }

    /**
     * Gets the processing date for the transaction.
     * 
     * @return processing date
     */
    public LocalDateTime getProcessingDate() {
        return processingDate;
    }

    /**
     * Sets the processing date for the transaction.
     * 
     * @param processingDate processing date
     */
    public void setProcessingDate(LocalDateTime processingDate) {
        this.processingDate = processingDate;
    }

    /**
     * Gets the confirmation flag for transaction processing.
     * 
     * @return confirmation flag (Y/N)
     */
    public String getConfirm() {
        return confirm;
    }

    /**
     * Sets the confirmation flag for transaction processing.
     * 
     * @param confirm confirmation flag (Y/N)
     */
    public void setConfirm(String confirm) {
        this.confirm = confirm;
    }

    /**
     * Validates that the confirmation flag is set to proceed with transaction.
     * 
     * Equivalent to COBOL CONFIRMI validation logic in COTRN02C.cbl PROCESS-ENTER-KEY paragraph.
     * Only 'Y' or 'y' values allow transaction processing to continue.
     * 
     * @return true if confirmed for processing, false otherwise
     */
    public boolean isConfirmed() {
        return "Y".equals(confirm) || "y".equals(confirm);
    }

    /**
     * Validates that all required fields are populated.
     * 
     * Provides programmatic validation check equivalent to COBOL field validation
     * in VALIDATE-INPUT-DATA-FIELDS paragraph of COTRN02C.cbl.
     * 
     * @return true if all required fields are populated, false otherwise
     */
    public boolean isValid() {
        return accountId != null && !accountId.trim().isEmpty() &&
               cardNumber != null && !cardNumber.trim().isEmpty() &&
               transactionType != null &&
               transactionCategory != null &&
               source != null && !source.trim().isEmpty() &&
               description != null && !description.trim().isEmpty() &&
               amount != null &&
               merchantId != null && !merchantId.trim().isEmpty() &&
               merchantName != null && !merchantName.trim().isEmpty() &&
               merchantCity != null && !merchantCity.trim().isEmpty() &&
               merchantZip != null && !merchantZip.trim().isEmpty() &&
               originalDate != null &&
               processingDate != null &&
               confirm != null && !confirm.trim().isEmpty();
    }

    /**
     * Returns a string representation of the AddTransactionRequest.
     * 
     * Provides formatted output for debugging and logging purposes.
     * Sensitive card number information is masked for security.
     * 
     * @return formatted string representation
     */
    @Override
    public String toString() {
        return String.format(
            "AddTransactionRequest{accountId='%s', cardNumber='%s', transactionType=%s, " +
            "transactionCategory=%s, source='%s', description='%s', amount=%s, " +
            "merchantId='%s', merchantName='%s', merchantCity='%s', merchantZip='%s', " +
            "originalDate=%s, processingDate=%s, confirm='%s'}",
            accountId,
            cardNumber != null ? cardNumber.replaceAll("\\d(?=\\d{4})", "*") : null,
            transactionType,
            transactionCategory,
            source,
            description != null ? (description.length() > 50 ? description.substring(0, 50) + "..." : description) : null,
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
     * Checks equality of AddTransactionRequest objects.
     * 
     * Compares all fields for equality to determine if two requests are identical.
     * Used for testing and validation purposes.
     * 
     * @param obj object to compare with
     * @return true if objects are equal, false otherwise
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
               java.util.Objects.equals(source, that.source) &&
               java.util.Objects.equals(description, that.description) &&
               java.util.Objects.equals(amount, that.amount) &&
               java.util.Objects.equals(merchantId, that.merchantId) &&
               java.util.Objects.equals(merchantName, that.merchantName) &&
               java.util.Objects.equals(merchantCity, that.merchantCity) &&
               java.util.Objects.equals(merchantZip, that.merchantZip) &&
               java.util.Objects.equals(originalDate, that.originalDate) &&
               java.util.Objects.equals(processingDate, that.processingDate) &&
               java.util.Objects.equals(confirm, that.confirm);
    }

    /**
     * Generates hash code for AddTransactionRequest objects.
     * 
     * Uses all fields to generate a consistent hash code for use in collections
     * and caching scenarios.
     * 
     * @return hash code value
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(accountId, cardNumber, transactionType, transactionCategory,
                                     source, description, amount, merchantId, merchantName,
                                     merchantCity, merchantZip, originalDate, processingDate, confirm);
    }
}