/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.carddemo.util.Constants;
import com.carddemo.util.ValidationUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Data Transfer Object for transaction requests including payment processing and transaction queries.
 * Maps to transaction BMS screens (COTRN00.bms, COTRN01.bms) and processing inputs from COBOL programs
 * COTRN00C and COTRN01C. This DTO replicates the TRAN-RECORD structure from CVTRA05Y.cpy copybook
 * while providing modern Java REST API capabilities.
 * 
 * Key field mappings from COBOL:
 * - TRAN-ID (PIC X(16)) -> transactionId
 * - TRAN-TYPE-CD (PIC X(02)) -> typeCode  
 * - TRAN-CAT-CD (PIC 9(04)) -> categoryCode
 * - TRAN-AMT (PIC S9(09)V99) -> amount (BigDecimal with 2 decimal precision)
 * - TRAN-MERCHANT-ID (PIC 9(09)) -> merchantId
 * - TRAN-MERCHANT-NAME (PIC X(50)) -> merchantName
 * - TRAN-CARD-NUM (PIC X(16)) -> cardNumber
 * 
 * Additional fields for REST API functionality:
 * - Date range filters (startDate, endDate) for transaction queries
 * - Pagination parameters (pageNumber, pageSize) for list requests
 * 
 * All validation rules maintain exact compatibility with COBOL business logic to ensure
 * functional parity during the modernization process. Amount precision matches COBOL 
 * COMP-3 packed decimal behavior using BigDecimal with scale of 2.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@NoArgsConstructor
public class TransactionRequest {

    /**
     * Transaction identifier matching TRAN-ID field.
     * COBOL: PIC X(16), allows alphanumeric transaction IDs.
     */
    @JsonProperty("transactionId")
    private String transactionId;

    /**
     * Transaction type code matching TRAN-TYPE-CD field.
     * COBOL: PIC X(02), exactly 2 characters for transaction classification.
     */
    @JsonProperty("typeCode")
    private String typeCode;

    /**
     * Transaction category code matching TRAN-CAT-CD field.
     * COBOL: PIC 9(04), exactly 4 digits for transaction categorization.
     */
    @JsonProperty("categoryCode")
    private String categoryCode;

    /**
     * Transaction amount matching TRAN-AMT field.
     * COBOL: PIC S9(09)V99, exactly 2 decimal places to match COMP-3 precision.
     * Uses BigDecimal to prevent floating-point rounding errors in financial calculations.
     */
    @JsonProperty("amount")
    @Digits(integer = 9, fraction = 2, message = "Amount must have maximum 9 integer digits and exactly 2 decimal places")
    private BigDecimal amount;

    /**
     * Card number matching TRAN-CARD-NUM field.
     * COBOL: PIC X(16), standard 16-digit credit card number.
     */
    @JsonProperty("cardNumber")
    private String cardNumber;

    /**
     * Merchant name matching TRAN-MERCHANT-NAME field.
     * COBOL: PIC X(50), merchant business name for transaction display.
     */
    @JsonProperty("merchantName")
    private String merchantName;

    /**
     * Merchant identifier matching TRAN-MERCHANT-ID field.
     * COBOL: PIC 9(09), exactly 9 digits for merchant identification.
     */
    @JsonProperty("merchantId")
    private String merchantId;

    /**
     * Start date for transaction query date range filtering.
     * Used in transaction list screens for limiting results to specific date ranges.
     */
    @JsonProperty("startDate")
    private LocalDate startDate;

    /**
     * End date for transaction query date range filtering.
     * Used in transaction list screens for limiting results to specific date ranges.
     */
    @JsonProperty("endDate")
    private LocalDate endDate;

    /**
     * Page number for pagination support in transaction list requests.
     * Maps to page navigation functionality from COTRN00.bms screen layout.
     */
    @JsonProperty("pageNumber")
    private Integer pageNumber;

    /**
     * Page size for pagination support in transaction list requests.
     * Defaults to TRANSACTIONS_PER_PAGE constant matching COBOL screen layout.
     */
    @JsonProperty("pageSize")
    private Integer pageSize;

    // Getter and Setter methods with comprehensive validation

    /**
     * Gets the transaction ID.
     * 
     * @return the transaction ID
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Sets the transaction ID with validation.
     * Validates field length and format according to COBOL specifications.
     * 
     * @param transactionId the transaction ID to set
     */
    public void setTransactionId(String transactionId) {
        if (transactionId != null && !transactionId.trim().isEmpty()) {
            ValidationUtil.validateFieldLength("transactionId", transactionId.trim(), Constants.TRANSACTION_ID_LENGTH);
        }
        this.transactionId = transactionId != null ? transactionId.trim() : null;
    }

    /**
     * Gets the transaction type code.
     * 
     * @return the transaction type code
     */
    public String getTypeCode() {
        return typeCode;
    }

    /**
     * Sets the transaction type code with validation.
     * Validates exactly 2 characters as per COBOL PIC X(02) specification.
     * 
     * @param typeCode the transaction type code to set
     */
    public void setTypeCode(String typeCode) {
        if (typeCode != null && !typeCode.trim().isEmpty()) {
            ValidationUtil.validateFieldLength("typeCode", typeCode.trim(), Constants.TYPE_CODE_LENGTH);
            if (typeCode.trim().length() != Constants.TYPE_CODE_LENGTH) {
                throw new IllegalArgumentException("Type code must be exactly " + Constants.TYPE_CODE_LENGTH + " characters");
            }
        }
        this.typeCode = typeCode != null ? typeCode.trim() : null;
    }

    /**
     * Gets the transaction category code.
     * 
     * @return the transaction category code
     */
    public String getCategoryCode() {
        return categoryCode;
    }

    /**
     * Sets the transaction category code with validation.
     * Validates exactly 4 digits as per COBOL PIC 9(04) specification.
     * 
     * @param categoryCode the transaction category code to set
     */
    public void setCategoryCode(String categoryCode) {
        if (categoryCode != null && !categoryCode.trim().isEmpty()) {
            String trimmedCode = categoryCode.trim();
            // Category code length is 4 based on COBOL PIC 9(04) from CVTRA05Y.cpy
            ValidationUtil.validateFieldLength("categoryCode", trimmedCode, 4);
            if (trimmedCode.length() != 4) {
                throw new IllegalArgumentException("Category code must be exactly 4 digits");
            }
            ValidationUtil.validateNumericField("categoryCode", trimmedCode);
        }
        this.categoryCode = categoryCode != null ? categoryCode.trim() : null;
    }

    /**
     * Gets the transaction amount.
     * 
     * @return the transaction amount
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Sets the transaction amount with validation.
     * Validates amount precision and business rules to match COBOL COMP-3 behavior.
     * 
     * @param amount the transaction amount to set
     */
    public void setAmount(BigDecimal amount) {
        if (amount != null) {
            // Validate using static method for amount validation
            if (!ValidationUtil.validateTransactionAmount(amount)) {
                throw new IllegalArgumentException("Invalid transaction amount");
            }
            // Ensure exactly 2 decimal places to match COBOL COMP-3 S9(09)V99
            this.amount = amount.setScale(2, BigDecimal.ROUND_HALF_UP);
        } else {
            this.amount = null;
        }
    }

    /**
     * Gets the card number.
     * 
     * @return the card number
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the card number with validation.
     * Validates card number format and length according to industry standards.
     * 
     * @param cardNumber the card number to set
     */
    public void setCardNumber(String cardNumber) {
        if (cardNumber != null && !cardNumber.trim().isEmpty()) {
            // Use ValidationUtil FieldValidator for card number validation
            ValidationUtil.FieldValidator validator = new ValidationUtil.FieldValidator();
            validator.validateCardNumber(cardNumber.trim());
            ValidationUtil.validateFieldLength("cardNumber", cardNumber.trim(), Constants.CARD_NUMBER_LENGTH);
        }
        this.cardNumber = cardNumber != null ? cardNumber.trim() : null;
    }

    /**
     * Gets the merchant name.
     * 
     * @return the merchant name
     */
    public String getMerchantName() {
        return merchantName;
    }

    /**
     * Sets the merchant name with validation.
     * Validates field length according to COBOL specifications.
     * 
     * @param merchantName the merchant name to set
     */
    public void setMerchantName(String merchantName) {
        if (merchantName != null && !merchantName.trim().isEmpty()) {
            // Merchant name length is 50 based on COBOL PIC X(50) from CVTRA05Y.cpy
            ValidationUtil.validateFieldLength("merchantName", merchantName.trim(), 50);
        }
        this.merchantName = merchantName != null ? merchantName.trim() : null;
    }

    /**
     * Gets the merchant ID.
     * 
     * @return the merchant ID
     */
    public String getMerchantId() {
        return merchantId;
    }

    /**
     * Sets the merchant ID with validation.
     * Validates exactly 9 digits as per COBOL PIC 9(09) specification.
     * 
     * @param merchantId the merchant ID to set
     */
    public void setMerchantId(String merchantId) {
        if (merchantId != null && !merchantId.trim().isEmpty()) {
            String trimmedId = merchantId.trim();
            // Merchant ID length is 9 based on COBOL PIC 9(09) from CVTRA05Y.cpy
            ValidationUtil.validateFieldLength("merchantId", trimmedId, 9);
            if (trimmedId.length() != 9) {
                throw new IllegalArgumentException("Merchant ID must be exactly 9 digits");
            }
            ValidationUtil.validateNumericField("merchantId", trimmedId);
        }
        this.merchantId = merchantId != null ? merchantId.trim() : null;
    }

    /**
     * Gets the start date for date range filtering.
     * 
     * @return the start date
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Sets the start date for date range filtering.
     * 
     * @param startDate the start date to set
     */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /**
     * Gets the end date for date range filtering.
     * 
     * @return the end date
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * Sets the end date for date range filtering.
     * 
     * @param endDate the end date to set
     */
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /**
     * Gets the page number for pagination.
     * 
     * @return the page number
     */
    public Integer getPageNumber() {
        return pageNumber;
    }

    /**
     * Sets the page number for pagination.
     * 
     * @param pageNumber the page number to set
     */
    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    /**
     * Gets the page size for pagination.
     * 
     * @return the page size
     */
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * Sets the page size for pagination.
     * 
     * @param pageSize the page size to set
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Validates the complete transaction request ensuring all business rules are met.
     * This method provides comprehensive validation for transaction processing requests
     * maintaining compatibility with COBOL validation logic.
     */
    public void validateTransactionRequest() {
        ValidationUtil.FieldValidator validator = new ValidationUtil.FieldValidator();
        
        // Validate required fields for transaction processing
        if (transactionId != null && !transactionId.trim().isEmpty()) {
            // Transaction ID validation - use generic field validation since specific method doesn't exist
            ValidationUtil.validateFieldLength("transactionId", transactionId, Constants.TRANSACTION_ID_LENGTH);
        }
        
        if (cardNumber != null && !cardNumber.trim().isEmpty()) {
            validator.validateCardNumber(cardNumber);
        }
        
        if (amount != null) {
            validator.validateTransactionAmount(amount);
        }
        
        if (merchantId != null && !merchantId.trim().isEmpty()) {
            // Merchant ID validation using numeric field validation
            ValidationUtil.validateNumericField("merchantId", merchantId);
            if (merchantId.length() != 9) {
                throw new IllegalArgumentException("Merchant ID must be exactly 9 digits");
            }
        }
        
        // Validate date range if both dates are provided
        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("Start date must be before or equal to end date");
            }
        }
        
        // Validate pagination parameters
        if (pageNumber != null && pageNumber < 0) {
            throw new IllegalArgumentException("Page number must be non-negative");
        }
        
        if (pageSize != null && pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }
    }

    /**
     * Checks equality based on all fields.
     * Required for proper DTO comparison and testing.
     * 
     * @param obj the object to compare with
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
        TransactionRequest that = (TransactionRequest) obj;
        return Objects.equals(transactionId, that.transactionId) &&
               Objects.equals(typeCode, that.typeCode) &&
               Objects.equals(categoryCode, that.categoryCode) &&
               Objects.equals(amount, that.amount) &&
               Objects.equals(cardNumber, that.cardNumber) &&
               Objects.equals(merchantName, that.merchantName) &&
               Objects.equals(merchantId, that.merchantId) &&
               Objects.equals(startDate, that.startDate) &&
               Objects.equals(endDate, that.endDate) &&
               Objects.equals(pageNumber, that.pageNumber) &&
               Objects.equals(pageSize, that.pageSize);
    }

    /**
     * Generates hash code based on all fields.
     * Required for proper DTO hashing and collection operations.
     * 
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(transactionId, typeCode, categoryCode, amount, cardNumber, 
                          merchantName, merchantId, startDate, endDate, pageNumber, pageSize);
    }

    /**
     * Provides string representation of the transaction request.
     * Useful for debugging and logging while maintaining data privacy.
     * 
     * @return string representation of the object
     */
    @Override
    public String toString() {
        return "TransactionRequest{" +
               "transactionId='" + transactionId + '\'' +
               ", typeCode='" + typeCode + '\'' +
               ", categoryCode='" + categoryCode + '\'' +
               ", amount=" + amount +
               ", cardNumber='" + (cardNumber != null ? "****" + cardNumber.substring(Math.max(0, cardNumber.length() - 4)) : null) + '\'' +
               ", merchantName='" + merchantName + '\'' +
               ", merchantId='" + merchantId + '\'' +
               ", startDate=" + startDate +
               ", endDate=" + endDate +
               ", pageNumber=" + pageNumber +
               ", pageSize=" + pageSize +
               '}';
    }
}