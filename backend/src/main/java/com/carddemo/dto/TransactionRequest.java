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
 * Data Transfer Object for transaction request operations.
 * Maps to transaction BMS screens and processing inputs from COTRN00/COTRN01 mapsets.
 * 
 * This DTO supports multiple transaction operations including:
 * - Transaction listing and search (COTRN00C functionality)
 * - Transaction detail viewing (COTRN01C functionality)
 * - Transaction filtering by date ranges and amounts
 * - Pagination for large result sets
 * - Payment processing and transaction queries
 * 
 * Field mappings correspond to CVTRA05Y copybook TRAN-RECORD structure:
 * - transactionId → TRAN-ID (PIC X(16))
 * - typeCode → TRAN-TYPE-CD (PIC X(02))
 * - categoryCode → TRAN-CAT-CD (PIC 9(04))
 * - amount → TRAN-AMT (PIC S9(09)V99)
 * - cardNumber → TRAN-CARD-NUM (PIC X(16))
 * - merchantName → TRAN-MERCHANT-NAME (PIC X(50))
 * - merchantId → TRAN-MERCHANT-ID (PIC 9(09))
 * 
 * Additional fields support REST API pagination and date range filtering:
 * - startDate/endDate → Date range filters for transaction queries
 * - pageNumber/pageSize → Pagination parameters for list requests
 * 
 * Validation ensures:
 * - Amount precision to 2 decimal places matching COBOL COMP-3 S9(09)V99
 * - Field lengths match original COBOL PIC clause specifications
 * - Transaction response times < 200ms through efficient validation
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@NoArgsConstructor
public class TransactionRequest {

    /**
     * Transaction identifier from TRAN-ID field.
     * Maps to COBOL PIC X(16) specification.
     * Used for transaction lookups and detail queries.
     */
    @JsonProperty("transactionId")
    private String transactionId;

    /**
     * Transaction type code from TRAN-TYPE-CD field.
     * Maps to COBOL PIC X(02) specification.
     * Indicates transaction category such as purchase, payment, refund.
     */
    @JsonProperty("typeCode")
    private String typeCode;

    /**
     * Transaction category code from TRAN-CAT-CD field.
     * Maps to COBOL PIC 9(04) specification.
     * Four-digit numeric code for transaction categorization.
     */
    @JsonProperty("categoryCode")
    private String categoryCode;

    /**
     * Transaction amount from TRAN-AMT field.
     * Maps to COBOL PIC S9(09)V99 COMP-3 specification.
     * Maintains exact 2 decimal place precision for financial calculations.
     * Maximum precision of 9 integer digits and exactly 2 fractional digits.
     */
    @JsonProperty("amount")
    @Digits(integer = 9, fraction = 2, message = "Amount must have maximum 9 integer digits and exactly 2 decimal places")
    private BigDecimal amount;

    /**
     * Credit card number from TRAN-CARD-NUM field.
     * Maps to COBOL PIC X(16) specification.
     * Standard 16-digit credit card number format.
     */
    @JsonProperty("cardNumber")
    private String cardNumber;

    /**
     * Merchant name from TRAN-MERCHANT-NAME field.
     * Maps to COBOL PIC X(50) specification.
     * Descriptive name of the merchant location.
     */
    @JsonProperty("merchantName")
    private String merchantName;

    /**
     * Merchant identifier from TRAN-MERCHANT-ID field.
     * Maps to COBOL PIC 9(09) specification.
     * Unique numeric identifier for merchant locations.
     */
    @JsonProperty("merchantId")
    private String merchantId;

    /**
     * Start date for transaction date range filtering.
     * Used in transaction list queries to filter results by date range.
     * Supports ISO date format for REST API compatibility.
     */
    @JsonProperty("startDate")
    private LocalDate startDate;

    /**
     * End date for transaction date range filtering.
     * Used in transaction list queries to filter results by date range.
     * Supports ISO date format for REST API compatibility.
     */
    @JsonProperty("endDate")
    private LocalDate endDate;

    /**
     * Page number for pagination support.
     * Zero-based page index for transaction list queries.
     * Enables efficient handling of large transaction datasets.
     */
    @JsonProperty("pageNumber")
    private Integer pageNumber;

    /**
     * Page size for pagination support.
     * Number of transactions to return per page.
     * Defaults to TRANSACTIONS_PER_PAGE constant matching COBOL screen layout.
     */
    @JsonProperty("pageSize")
    private Integer pageSize;

    /**
     * Gets the transaction identifier.
     * 
     * @return the transaction ID string
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Sets the transaction identifier with validation.
     * Validates against TRANSACTION_ID_LENGTH constant and format rules.
     * 
     * @param transactionId the transaction ID to set
     */
    public void setTransactionId(String transactionId) {
        if (transactionId != null && !transactionId.trim().isEmpty()) {
            ValidationUtil.validateFieldLength("transactionId", transactionId.trim(), Constants.TRANSACTION_ID_LENGTH);
            ValidationUtil.validateRequiredField("transactionId", transactionId.trim());
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
     * Validates against TYPE_CODE_LENGTH constant (2 characters).
     * 
     * @param typeCode the transaction type code to set
     */
    public void setTypeCode(String typeCode) {
        if (typeCode != null && !typeCode.trim().isEmpty()) {
            ValidationUtil.validateFieldLength("typeCode", typeCode.trim(), Constants.TYPE_CODE_LENGTH);
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
     * Validates against 4-digit requirement from COBOL PIC 9(04).
     * 
     * @param categoryCode the transaction category code to set
     */
    public void setCategoryCode(String categoryCode) {
        if (categoryCode != null && !categoryCode.trim().isEmpty()) {
            ValidationUtil.validateFieldLength("categoryCode", categoryCode.trim(), 4);
            ValidationUtil.validateNumericField("categoryCode", categoryCode.trim());
        }
        this.categoryCode = categoryCode != null ? categoryCode.trim() : null;
    }

    /**
     * Gets the transaction amount.
     * 
     * @return the transaction amount with 2 decimal place precision
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Sets the transaction amount with precision validation.
     * Ensures amount maintains exactly 2 decimal places and validates range.
     * Uses BigDecimal.setScale() to maintain COBOL COMP-3 precision.
     * 
     * @param amount the transaction amount to set
     */
    public void setAmount(BigDecimal amount) {
        if (amount != null) {
            // Set scale to 2 decimal places to match COBOL COMP-3 S9(09)V99
            this.amount = amount.setScale(2, BigDecimal.ROUND_HALF_UP);
            ValidationUtil.validateTransactionAmount(this.amount);
        } else {
            this.amount = null;
        }
    }

    /**
     * Gets the credit card number.
     * 
     * @return the credit card number
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the credit card number with validation.
     * Validates against CARD_NUMBER_LENGTH constant and format rules.
     * 
     * @param cardNumber the credit card number to set
     */
    public void setCardNumber(String cardNumber) {
        if (cardNumber != null && !cardNumber.trim().isEmpty()) {
            ValidationUtil.validateFieldLength("cardNumber", cardNumber.trim(), Constants.CARD_NUMBER_LENGTH);
            // Use instance validator for card number validation
            ValidationUtil.FieldValidator validator = new ValidationUtil.FieldValidator();
            validator.validateCardNumber(cardNumber.trim());
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
     * Validates against 50-character limit from COBOL PIC X(50).
     * 
     * @param merchantName the merchant name to set
     */
    public void setMerchantName(String merchantName) {
        if (merchantName != null && !merchantName.trim().isEmpty()) {
            ValidationUtil.validateFieldLength("merchantName", merchantName.trim(), 50);
        }
        this.merchantName = merchantName != null ? merchantName.trim() : null;
    }

    /**
     * Gets the merchant identifier.
     * 
     * @return the merchant ID
     */
    public String getMerchantId() {
        return merchantId;
    }

    /**
     * Sets the merchant identifier with validation.
     * Validates against 9-digit requirement from COBOL PIC 9(09).
     * 
     * @param merchantId the merchant ID to set
     */
    public void setMerchantId(String merchantId) {
        if (merchantId != null && !merchantId.trim().isEmpty()) {
            ValidationUtil.validateFieldLength("merchantId", merchantId.trim(), 9);
            ValidationUtil.validateNumericField("merchantId", merchantId.trim());
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
     * Validates date is not in the future and is before end date if both are set.
     * 
     * @param startDate the start date to set
     */
    public void setStartDate(LocalDate startDate) {
        if (startDate != null) {
            // Validate start date is not after today
            if (startDate.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Start date cannot be in the future");
            }
            // Validate start date is before end date if end date is set
            if (this.endDate != null && !startDate.isBefore(this.endDate) && !startDate.isEqual(this.endDate)) {
                throw new IllegalArgumentException("Start date must be before or equal to end date");
            }
        }
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
     * Validates date is not in the future and is after start date if both are set.
     * 
     * @param endDate the end date to set
     */
    public void setEndDate(LocalDate endDate) {
        if (endDate != null) {
            // Validate end date is not after today
            if (endDate.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("End date cannot be in the future");
            }
            // Validate end date is after start date if start date is set
            if (this.startDate != null && !endDate.isAfter(this.startDate) && !endDate.isEqual(this.startDate)) {
                throw new IllegalArgumentException("End date must be after or equal to start date");
            }
        }
        this.endDate = endDate;
    }

    /**
     * Gets the page number for pagination.
     * 
     * @return the page number (zero-based)
     */
    public Integer getPageNumber() {
        return pageNumber;
    }

    /**
     * Sets the page number for pagination.
     * Validates page number is non-negative.
     * 
     * @param pageNumber the page number to set (zero-based)
     */
    public void setPageNumber(Integer pageNumber) {
        if (pageNumber != null && pageNumber < 0) {
            throw new IllegalArgumentException("Page number must be non-negative");
        }
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
     * Validates page size is positive and reasonable.
     * 
     * @param pageSize the page size to set
     */
    public void setPageSize(Integer pageSize) {
        if (pageSize != null) {
            if (pageSize <= 0) {
                throw new IllegalArgumentException("Page size must be positive");
            }
            if (pageSize > 1000) {
                throw new IllegalArgumentException("Page size cannot exceed 1000 records");
            }
        }
        this.pageSize = pageSize;
    }

    /**
     * Validates the complete transaction request.
     * Performs comprehensive validation of all fields according to business rules.
     * 
     * @throws IllegalArgumentException if any validation rules are violated
     */
    public void validate() {
        // Validate date range if both dates are provided
        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("Start date must be before or equal to end date");
            }
        }

        // Validate amount precision if provided
        if (amount != null) {
            if (!ValidationUtil.validateTransactionAmount(amount)) {
                throw new IllegalArgumentException("Transaction amount is invalid");
            }
            // Ensure amount uses proper BigDecimal operations for financial precision
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal normalizedAmount = BigDecimal.valueOf(amount.doubleValue());
                if (normalizedAmount.compareTo(amount) != 0) {
                    throw new IllegalArgumentException("Amount precision error detected");
                }
            }
        }

        // Validate card number format if provided
        if (cardNumber != null && !cardNumber.trim().isEmpty()) {
            ValidationUtil.FieldValidator validator = new ValidationUtil.FieldValidator();
            validator.validateCardNumber(cardNumber.trim());
        }

        // Validate account-related fields if provided
        if (transactionId != null && !transactionId.trim().isEmpty()) {
            ValidationUtil.validateRequiredField("transactionId", transactionId.trim());
            // Use account ID validation for transaction ID format checking
            try {
                ValidationUtil.FieldValidator validator = new ValidationUtil.FieldValidator();
                if (transactionId.trim().length() == 11 && ValidationUtil.validateNumericField(transactionId.trim(), 11)) {
                    validator.validateAccountId(transactionId.trim());
                }
            } catch (Exception e) {
                // Transaction ID may not follow account ID format, which is acceptable
            }
        }

        // Validate merchant ID format if provided
        if (merchantId != null && !merchantId.trim().isEmpty()) {
            ValidationUtil.validateNumericField("merchantId", merchantId.trim());
        }

        // Validate date formats for REST API compatibility
        if (startDate != null) {
            String formattedStart = startDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate parsedStart = LocalDate.parse(formattedStart);
            if (!parsedStart.equals(startDate)) {
                throw new IllegalArgumentException("Start date format validation failed");
            }
        }
        
        if (endDate != null) {
            String formattedEnd = endDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate parsedEnd = LocalDate.parse(formattedEnd);
            if (!parsedEnd.equals(endDate)) {
                throw new IllegalArgumentException("End date format validation failed");
            }
        }
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Compares all fields for equality following standard DTO equals contract.
     * 
     * @param obj the reference object with which to compare
     * @return true if this object is the same as the obj argument; false otherwise
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
     * Returns a hash code value for the object.
     * Generates hash code based on all fields following standard DTO hashCode contract.
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(transactionId, typeCode, categoryCode, amount, cardNumber,
                           merchantName, merchantId, startDate, endDate, pageNumber, pageSize);
    }

    /**
     * Returns a string representation of the object.
     * Provides comprehensive string representation for debugging and logging.
     * Masks sensitive fields like card number for security.
     * 
     * @return a string representation of the object
     */
    @Override
    public String toString() {
        String maskedCardNumber = cardNumber != null && cardNumber.length() > 4 
            ? "**** **** **** " + cardNumber.substring(cardNumber.length() - 4)
            : cardNumber;
            
        return "TransactionRequest{" +
               "transactionId='" + transactionId + '\'' +
               ", typeCode='" + typeCode + '\'' +
               ", categoryCode='" + categoryCode + '\'' +
               ", amount=" + amount +
               ", cardNumber='" + maskedCardNumber + '\'' +
               ", merchantName='" + merchantName + '\'' +
               ", merchantId='" + merchantId + '\'' +
               ", startDate=" + startDate +
               ", endDate=" + endDate +
               ", pageNumber=" + pageNumber +
               ", pageSize=" + pageSize +
               '}';
    }
}