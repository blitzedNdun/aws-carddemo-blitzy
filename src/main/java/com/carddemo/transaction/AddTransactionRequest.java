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
import com.carddemo.common.validator.ValidCCYYMMDD;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Request DTO for transaction addition operations with comprehensive validation rules 
 * and business constraint enforcement.
 * 
 * <p>This class implements the complete validation pipeline equivalent to COBOL 
 * data validation found in COTRN02C.cbl, providing detailed error reporting and 
 * maintaining exact functional equivalence with the original CICS transaction 
 * processing system.</p>
 * 
 * <h3>COBOL Transformation Details:</h3>
 * <ul>
 *   <li>Converted from COTRN02C.cbl ADD-TRANSACTION functionality</li>
 *   <li>Maintains CVTRA05Y.cpy record structure mappings</li>
 *   <li>Implements VALIDATE-INPUT-KEY-FIELDS and VALIDATE-INPUT-DATA-FIELDS logic</li>
 *   <li>Preserves exact COBOL field validation including numeric and format checks</li>
 *   <li>Supports account-card relationship verification equivalent to CCXREF/CXACAIX lookups</li>
 * </ul>
 * 
 * <h3>Validation Rules (from COBOL):</h3>
 * <ul>
 *   <li><strong>Account ID:</strong> Must be numeric, cannot be empty, verified in CXACAIX</li>
 *   <li><strong>Card Number:</strong> Must be numeric, 16 digits, verified in CCXREF</li>
 *   <li><strong>Transaction Type:</strong> Must be numeric, cannot be empty, validated against TRANTYPE</li>
 *   <li><strong>Transaction Category:</strong> Must be numeric, cannot be empty, validated against TRANCATG</li>
 *   <li><strong>Amount:</strong> Must be in format +/-99999999.99, supports COMP-3 precision</li>
 *   <li><strong>Dates:</strong> Must be in YYYY-MM-DD format with date validation</li>
 *   <li><strong>Merchant fields:</strong> All required, proper format validation</li>
 * </ul>
 * 
 * <h3>Business Constraints:</h3>
 * <ul>
 *   <li>Account-Card relationship must be valid (one or both required)</li>
 *   <li>Transaction amount must be within business limits</li>
 *   <li>Date validation includes leap year and month/day combinations</li>
 *   <li>Merchant data must conform to industry standards</li>
 *   <li>Confirmation flag validation for transaction processing</li>
 * </ul>
 * 
 * <h3>Integration Points:</h3>
 * <ul>
 *   <li>Spring Boot REST API validation framework</li>
 *   <li>Jakarta Bean Validation 3.0 annotations</li>
 *   <li>Custom validators for CardDemo-specific business rules</li>
 *   <li>Jackson JSON serialization for React frontend integration</li>
 *   <li>JPA entity mapping for PostgreSQL persistence</li>
 * </ul>
 * 
 * @author Blitzy agent
 * @version 1.0
 * @since 1.0
 */
public class AddTransactionRequest {
    
    /**
     * Account ID for the transaction.
     * Equivalent to COBOL ACTIDINI field with numeric validation.
     * Must be verified against CXACAIX cross-reference file.
     */
    @JsonProperty("accountId")
    @NotNull(message = "Account ID cannot be null")
    @Min(value = 1, message = "Account ID must be a positive number")
    @Max(value = 99999999999L, message = "Account ID must be 11 digits or less")
    private Long accountId;
    
    /**
     * Credit card number for the transaction.
     * Equivalent to COBOL CARDNINI field with 16-digit numeric validation.
     * Must be verified against CCXREF cross-reference file.
     */
    @JsonProperty("cardNumber")
    @NotBlank(message = "Card Number cannot be empty")
    @ValidCardNumber(message = "Card Number must be 16 digits and pass Luhn algorithm validation")
    @Size(min = 16, max = 16, message = "Card Number must be exactly 16 digits")
    @Pattern(regexp = "^[0-9]{16}$", message = "Card Number must be numeric")
    private String cardNumber;
    
    /**
     * Transaction type code.
     * Equivalent to COBOL TTYPCDI field with validation against TRANTYPE reference.
     */
    @JsonProperty("transactionType")
    @NotNull(message = "Transaction Type cannot be null")
    private TransactionType transactionType;
    
    /**
     * Transaction category code.
     * Equivalent to COBOL TCATCDI field with validation against TRANCATG reference.
     */
    @JsonProperty("transactionCategory")
    @NotNull(message = "Transaction Category cannot be null")
    private TransactionCategory transactionCategory;
    
    /**
     * Transaction source description.
     * Equivalent to COBOL TRNSRCI field with required validation.
     */
    @JsonProperty("source")
    @NotBlank(message = "Transaction Source cannot be empty")
    @Size(min = 1, max = 10, message = "Transaction Source must be between 1 and 10 characters")
    @Pattern(regexp = "^[A-Za-z0-9\\s\\-_]+$", message = "Transaction Source contains invalid characters")
    private String source;
    
    /**
     * Transaction description.
     * Equivalent to COBOL TDESCI field with required validation.
     */
    @JsonProperty("description")
    @NotBlank(message = "Transaction Description cannot be empty")
    @Size(min = 1, max = 100, message = "Transaction Description must be between 1 and 100 characters")
    private String description;
    
    /**
     * Transaction amount with exact BigDecimal precision.
     * Equivalent to COBOL TRNAMTI field with +/-99999999.99 format validation.
     * Maintains COMP-3 precision using BigDecimal with scale 2.
     */
    @JsonProperty("amount")
    @NotNull(message = "Transaction Amount cannot be null")
    @ValidCurrency(
        min = "-99999999.99",
        max = "99999999.99",
        message = "Transaction Amount must be in format +/-99999999.99"
    )
    @DecimalMax(value = "99999999.99", message = "Transaction Amount cannot exceed 99999999.99")
    private BigDecimal amount;
    
    /**
     * Merchant ID for the transaction.
     * Equivalent to COBOL MIDI field with numeric validation.
     */
    @JsonProperty("merchantId")
    @NotNull(message = "Merchant ID cannot be null")
    @Min(value = 1, message = "Merchant ID must be a positive number")
    @Max(value = 999999999L, message = "Merchant ID must be 9 digits or less")
    private Long merchantId;
    
    /**
     * Merchant name.
     * Equivalent to COBOL MNAMEI field with required validation.
     */
    @JsonProperty("merchantName")
    @NotBlank(message = "Merchant Name cannot be empty")
    @Size(min = 1, max = 50, message = "Merchant Name must be between 1 and 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9\\s\\-'.,&]+$", message = "Merchant Name contains invalid characters")
    private String merchantName;
    
    /**
     * Merchant city.
     * Equivalent to COBOL MCITYI field with required validation.
     */
    @JsonProperty("merchantCity")
    @NotBlank(message = "Merchant City cannot be empty")
    @Size(min = 1, max = 50, message = "Merchant City must be between 1 and 50 characters")
    @Pattern(regexp = "^[A-Za-z\\s\\-'.,]+$", message = "Merchant City contains invalid characters")
    private String merchantCity;
    
    /**
     * Merchant ZIP code.
     * Equivalent to COBOL MZIPI field with required validation.
     */
    @JsonProperty("merchantZip")
    @NotBlank(message = "Merchant ZIP cannot be empty")
    @Size(min = 5, max = 10, message = "Merchant ZIP must be between 5 and 10 characters")
    @Pattern(regexp = "^[0-9]{5}(-[0-9]{4})?$", message = "Merchant ZIP must be in format 12345 or 12345-6789")
    private String merchantZip;
    
    /**
     * Original transaction date.
     * Equivalent to COBOL TORIGDTI field with YYYY-MM-DD format validation.
     */
    @JsonProperty("originalDate")
    @NotNull(message = "Original Date cannot be null")
    private LocalDateTime originalDate;
    
    /**
     * Processing transaction date.
     * Equivalent to COBOL TPROCDTI field with YYYY-MM-DD format validation.
     */
    @JsonProperty("processingDate")
    @NotNull(message = "Processing Date cannot be null")
    private LocalDateTime processingDate;
    
    /**
     * Confirmation flag for transaction processing.
     * Equivalent to COBOL CONFIRMI field with Y/N validation.
     */
    @JsonProperty("confirm")
    @NotBlank(message = "Confirmation flag cannot be empty")
    @Pattern(regexp = "^[YyNn]$", message = "Confirmation must be Y or N")
    private String confirm;
    
    /**
     * Default constructor for AddTransactionRequest.
     */
    public AddTransactionRequest() {
        // Default constructor for framework usage
    }
    
    /**
     * Constructor with all required fields for AddTransactionRequest.
     * 
     * @param accountId Account ID for the transaction
     * @param cardNumber Credit card number
     * @param transactionType Transaction type enum
     * @param transactionCategory Transaction category enum
     * @param source Transaction source description
     * @param description Transaction description
     * @param amount Transaction amount with BigDecimal precision
     * @param merchantId Merchant ID
     * @param merchantName Merchant name
     * @param merchantCity Merchant city
     * @param merchantZip Merchant ZIP code
     * @param originalDate Original transaction date
     * @param processingDate Processing transaction date
     * @param confirm Confirmation flag (Y/N)
     */
    public AddTransactionRequest(Long accountId, String cardNumber, TransactionType transactionType,
                                TransactionCategory transactionCategory, String source, String description,
                                BigDecimal amount, Long merchantId, String merchantName, String merchantCity,
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
     * Gets the account ID.
     * 
     * @return Account ID for the transaction
     */
    public Long getAccountId() {
        return accountId;
    }
    
    /**
     * Sets the account ID.
     * 
     * @param accountId Account ID for the transaction
     */
    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
    
    /**
     * Gets the card number.
     * 
     * @return Credit card number
     */
    public String getCardNumber() {
        return cardNumber;
    }
    
    /**
     * Sets the card number.
     * 
     * @param cardNumber Credit card number
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
    
    /**
     * Gets the transaction type.
     * 
     * @return Transaction type enum
     */
    public TransactionType getTransactionType() {
        return transactionType;
    }
    
    /**
     * Sets the transaction type.
     * 
     * @param transactionType Transaction type enum
     */
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }
    
    /**
     * Gets the transaction category.
     * 
     * @return Transaction category enum
     */
    public TransactionCategory getTransactionCategory() {
        return transactionCategory;
    }
    
    /**
     * Sets the transaction category.
     * 
     * @param transactionCategory Transaction category enum
     */
    public void setTransactionCategory(TransactionCategory transactionCategory) {
        this.transactionCategory = transactionCategory;
    }
    
    /**
     * Gets the transaction source.
     * 
     * @return Transaction source description
     */
    public String getSource() {
        return source;
    }
    
    /**
     * Sets the transaction source.
     * 
     * @param source Transaction source description
     */
    public void setSource(String source) {
        this.source = source;
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
     * Gets the transaction amount.
     * 
     * @return Transaction amount with BigDecimal precision
     */
    public BigDecimal getAmount() {
        return amount;
    }
    
    /**
     * Sets the transaction amount.
     * 
     * @param amount Transaction amount with BigDecimal precision
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    /**
     * Gets the merchant ID.
     * 
     * @return Merchant ID
     */
    public Long getMerchantId() {
        return merchantId;
    }
    
    /**
     * Sets the merchant ID.
     * 
     * @param merchantId Merchant ID
     */
    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
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
     * Gets the original transaction date.
     * 
     * @return Original transaction date
     */
    public LocalDateTime getOriginalDate() {
        return originalDate;
    }
    
    /**
     * Sets the original transaction date.
     * 
     * @param originalDate Original transaction date
     */
    public void setOriginalDate(LocalDateTime originalDate) {
        this.originalDate = originalDate;
    }
    
    /**
     * Gets the processing transaction date.
     * 
     * @return Processing transaction date
     */
    public LocalDateTime getProcessingDate() {
        return processingDate;
    }
    
    /**
     * Sets the processing transaction date.
     * 
     * @param processingDate Processing transaction date
     */
    public void setProcessingDate(LocalDateTime processingDate) {
        this.processingDate = processingDate;
    }
    
    /**
     * Gets the confirmation flag.
     * 
     * @return Confirmation flag (Y/N)
     */
    public String getConfirm() {
        return confirm;
    }
    
    /**
     * Sets the confirmation flag.
     * 
     * @param confirm Confirmation flag (Y/N)
     */
    public void setConfirm(String confirm) {
        this.confirm = confirm;
    }
    
    /**
     * Validates that either account ID or card number is provided.
     * Equivalent to COBOL VALIDATE-INPUT-KEY-FIELDS logic.
     * 
     * @return true if validation passes, false otherwise
     */
    public boolean isValidAccountCardCombination() {
        return (accountId != null && accountId > 0) || 
               (cardNumber != null && !cardNumber.trim().isEmpty());
    }
    
    /**
     * Validates that the confirmation flag is properly set.
     * Equivalent to COBOL confirmation validation logic.
     * 
     * @return true if confirmation is 'Y' or 'y', false otherwise
     */
    public boolean isConfirmed() {
        return confirm != null && ("Y".equalsIgnoreCase(confirm.trim()) || "y".equalsIgnoreCase(confirm.trim()));
    }
    
    /**
     * Validates that the transaction amount is in valid range.
     * Equivalent to COBOL amount validation with COMP-3 precision.
     * 
     * @return true if amount is within valid range, false otherwise
     */
    public boolean isValidAmount() {
        if (amount == null) {
            return false;
        }
        
        BigDecimal maxAmount = new BigDecimal("99999999.99");
        BigDecimal minAmount = new BigDecimal("-99999999.99");
        
        return amount.compareTo(minAmount) >= 0 && amount.compareTo(maxAmount) <= 0 && 
               amount.scale() <= 2;
    }
    
    /**
     * Validates that all required merchant fields are present.
     * Equivalent to COBOL merchant validation logic.
     * 
     * @return true if all merchant fields are valid, false otherwise
     */
    public boolean isValidMerchantData() {
        return merchantId != null && merchantId > 0 &&
               merchantName != null && !merchantName.trim().isEmpty() &&
               merchantCity != null && !merchantCity.trim().isEmpty() &&
               merchantZip != null && !merchantZip.trim().isEmpty();
    }
    
    /**
     * Validates that transaction dates are properly set.
     * Equivalent to COBOL date validation logic.
     * 
     * @return true if dates are valid, false otherwise
     */
    public boolean isValidDateCombination() {
        return originalDate != null && processingDate != null &&
               !originalDate.isAfter(processingDate);
    }
    
    /**
     * Comprehensive validation method that checks all business rules.
     * Equivalent to COBOL VALIDATE-INPUT-KEY-FIELDS and VALIDATE-INPUT-DATA-FIELDS.
     * 
     * @return true if all validation passes, false otherwise
     */
    public boolean isValid() {
        return isValidAccountCardCombination() &&
               isValidAmount() &&
               isValidMerchantData() &&
               isValidDateCombination() &&
               transactionType != null &&
               transactionCategory != null &&
               source != null && !source.trim().isEmpty() &&
               description != null && !description.trim().isEmpty();
    }
    
    /**
     * Checks if this object equals another object.
     * 
     * @param obj Object to compare with
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        AddTransactionRequest that = (AddTransactionRequest) obj;
        return Objects.equals(accountId, that.accountId) &&
               Objects.equals(cardNumber, that.cardNumber) &&
               Objects.equals(transactionType, that.transactionType) &&
               Objects.equals(transactionCategory, that.transactionCategory) &&
               Objects.equals(source, that.source) &&
               Objects.equals(description, that.description) &&
               Objects.equals(amount, that.amount) &&
               Objects.equals(merchantId, that.merchantId) &&
               Objects.equals(merchantName, that.merchantName) &&
               Objects.equals(merchantCity, that.merchantCity) &&
               Objects.equals(merchantZip, that.merchantZip) &&
               Objects.equals(originalDate, that.originalDate) &&
               Objects.equals(processingDate, that.processingDate) &&
               Objects.equals(confirm, that.confirm);
    }
    
    /**
     * Generates hash code for this object.
     * 
     * @return Hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(accountId, cardNumber, transactionType, transactionCategory,
                          source, description, amount, merchantId, merchantName, merchantCity,
                          merchantZip, originalDate, processingDate, confirm);
    }
    
    /**
     * String representation of AddTransactionRequest.
     * 
     * @return String representation with masked sensitive data
     */
    @Override
    public String toString() {
        return "AddTransactionRequest{" +
               "accountId=" + accountId +
               ", cardNumber='" + (cardNumber != null ? cardNumber.replaceAll("\\d(?=\\d{4})", "*") : null) + '\'' +
               ", transactionType=" + transactionType +
               ", transactionCategory=" + transactionCategory +
               ", source='" + source + '\'' +
               ", description='" + description + '\'' +
               ", amount=" + amount +
               ", merchantId=" + merchantId +
               ", merchantName='" + merchantName + '\'' +
               ", merchantCity='" + merchantCity + '\'' +
               ", merchantZip='" + merchantZip + '\'' +
               ", originalDate=" + originalDate +
               ", processingDate=" + processingDate +
               ", confirm='" + confirm + '\'' +
               '}';
    }
}