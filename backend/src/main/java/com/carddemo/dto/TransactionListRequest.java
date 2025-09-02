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
 * Request DTO for transaction list query (CT00 transaction).
 * Contains search criteria including account ID, card number, date range filters, 
 * amount range filters, and pagination parameters.
 * 
 * Maps COTRN00 BMS input fields for transaction search and browsing operations,
 * converting CICS transaction screen input to REST API request structure.
 * Implements cursor-based pagination for VSAM browse operations equivalent
 * to STARTBR/READNEXT/READPREV transaction processing patterns.
 * 
 * Field validation mirrors COBOL PIC clause constraints from the original
 * transaction list program (COTRN00C) and transaction record structure (CVTRA05Y).
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@NoArgsConstructor
public class TransactionListRequest {

    /**
     * Account ID for transaction filtering.
     * Must be exactly 11 digits matching COBOL PIC X(11) specification.
     * Corresponds to ACCT-ID field from account record structure.
     */
    @JsonProperty("accountId")
    private String accountId;

    /**
     * Card number for transaction filtering.
     * Must be exactly 16 digits matching credit card industry standards.
     * Corresponds to TRAN-CARD-NUM field from CVTRA05Y transaction record.
     */
    @JsonProperty("cardNumber")
    private String cardNumber;

    /**
     * Start date for transaction date range filtering.
     * Date format: YYYY-MM-DD (ISO 8601).
     * Converts to COBOL date format internally for database queries.
     */
    @JsonProperty("startDate")
    private LocalDate startDate;

    /**
     * End date for transaction date range filtering.
     * Date format: YYYY-MM-DD (ISO 8601).
     * Must be equal to or after startDate.
     */
    @JsonProperty("endDate")
    private LocalDate endDate;

    /**
     * Page number for pagination control.
     * 1-based page numbering matching COBOL screen pagination logic.
     * Corresponds to CDEMO-CT00-PAGE-NUM from COTRN00C program.
     */
    @JsonProperty("pageNumber")
    private Integer pageNumber;

    /**
     * Number of transactions per page.
     * Default value matches COBOL screen capacity (10 transactions per screen).
     * Maximum value should not exceed database query performance limits.
     */
    @JsonProperty("pageSize")
    private Integer pageSize;

    /**
     * Minimum transaction amount for range filtering.
     * Precision matches COBOL COMP-3 S9(09)V99 format from TRAN-AMT field.
     * Maximum 9 integer digits and exactly 2 fractional digits.
     */
    @JsonProperty("minAmount")
    @Digits(integer = 9, fraction = 2, message = "Minimum amount must have maximum 9 integer digits and exactly 2 fractional digits")
    private BigDecimal minAmount;

    /**
     * Maximum transaction amount for range filtering.
     * Precision matches COBOL COMP-3 S9(09)V99 format from TRAN-AMT field.
     * Maximum 9 integer digits and exactly 2 fractional digits.
     */
    @JsonProperty("maxAmount")
    @Digits(integer = 9, fraction = 2, message = "Maximum amount must have maximum 9 integer digits and exactly 2 fractional digits")
    private BigDecimal maxAmount;

    /**
     * Gets the account ID for transaction filtering.
     * 
     * @return the account ID (11 digits)
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID for transaction filtering.
     * Validates account ID format according to COBOL PIC X(11) specification.
     * 
     * @param accountId the account ID to set (must be exactly 11 digits)
     */
    public void setAccountId(String accountId) {
        // Validate account ID using ValidationUtil
        if (accountId != null && !accountId.trim().isEmpty()) {
            ValidationUtil.FieldValidator validator = new ValidationUtil.FieldValidator();
            validator.validateAccountId(accountId);
        }
        this.accountId = accountId;
    }

    /**
     * Gets the card number for transaction filtering.
     * 
     * @return the card number (16 digits)
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the card number for transaction filtering.
     * Validates card number format according to industry standards.
     * 
     * @param cardNumber the card number to set (must be exactly 16 digits)
     */
    public void setCardNumber(String cardNumber) {
        // Validate card number using ValidationUtil
        if (cardNumber != null && !cardNumber.trim().isEmpty()) {
            ValidationUtil.FieldValidator validator = new ValidationUtil.FieldValidator();
            validator.validateCardNumber(cardNumber);
        }
        this.cardNumber = cardNumber;
    }

    /**
     * Gets the start date for transaction date range filtering.
     * 
     * @return the start date in YYYY-MM-DD format
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Sets the start date for transaction date range filtering.
     * Validates that start date is not after end date if both are provided.
     * 
     * @param startDate the start date to set (YYYY-MM-DD format)
     */
    public void setStartDate(LocalDate startDate) {
        // Validate date range if both dates are provided
        if (startDate != null && this.endDate != null) {
            validateDateRange(startDate, this.endDate);
        }
        this.startDate = startDate;
    }

    /**
     * Gets the end date for transaction date range filtering.
     * 
     * @return the end date in YYYY-MM-DD format
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * Sets the end date for transaction date range filtering.
     * Validates that end date is not before start date if both are provided.
     * 
     * @param endDate the end date to set (YYYY-MM-DD format)
     */
    public void setEndDate(LocalDate endDate) {
        // Validate date range if both dates are provided
        if (this.startDate != null && endDate != null) {
            validateDateRange(this.startDate, endDate);
        }
        this.endDate = endDate;
    }

    /**
     * Gets the page number for pagination control.
     * 
     * @return the page number (1-based)
     */
    public Integer getPageNumber() {
        return pageNumber;
    }

    /**
     * Sets the page number for pagination control.
     * Validates that page number is positive.
     * 
     * @param pageNumber the page number to set (must be positive)
     */
    public void setPageNumber(Integer pageNumber) {
        if (pageNumber != null && pageNumber < 1) {
            throw new IllegalArgumentException("Page number must be 1 or greater");
        }
        this.pageNumber = pageNumber;
    }

    /**
     * Gets the number of transactions per page.
     * 
     * @return the page size
     */
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * Sets the number of transactions per page.
     * Validates that page size is within reasonable limits.
     * 
     * @param pageSize the page size to set (must be positive and not exceed 100)
     */
    public void setPageSize(Integer pageSize) {
        if (pageSize != null && (pageSize < 1 || pageSize > 100)) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
        this.pageSize = pageSize;
    }

    /**
     * Gets the minimum transaction amount for range filtering.
     * 
     * @return the minimum amount
     */
    public BigDecimal getMinAmount() {
        return minAmount;
    }

    /**
     * Sets the minimum transaction amount for range filtering.
     * Validates amount precision and range constraints.
     * 
     * @param minAmount the minimum amount to set
     */
    public void setMinAmount(BigDecimal minAmount) {
        if (minAmount != null) {
            // Validate amount is not negative
            if (minAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Minimum amount cannot be negative");
            }
            
            // Validate precision matches COBOL COMP-3 format
            if (minAmount.scale() > 2) {
                throw new IllegalArgumentException("Minimum amount cannot have more than 2 decimal places");
            }
            
            // Validate range with maxAmount if both provided
            if (this.maxAmount != null && minAmount.compareTo(this.maxAmount) > 0) {
                throw new IllegalArgumentException("Minimum amount cannot be greater than maximum amount");
            }
        }
        this.minAmount = minAmount;
    }

    /**
     * Gets the maximum transaction amount for range filtering.
     * 
     * @return the maximum amount
     */
    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    /**
     * Sets the maximum transaction amount for range filtering.
     * Validates amount precision and range constraints.
     * 
     * @param maxAmount the maximum amount to set
     */
    public void setMaxAmount(BigDecimal maxAmount) {
        if (maxAmount != null) {
            // Validate amount is not negative
            if (maxAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Maximum amount cannot be negative");
            }
            
            // Validate precision matches COBOL COMP-3 format
            if (maxAmount.scale() > 2) {
                throw new IllegalArgumentException("Maximum amount cannot have more than 2 decimal places");
            }
            
            // Validate range with minAmount if both provided
            if (this.minAmount != null && maxAmount.compareTo(this.minAmount) < 0) {
                throw new IllegalArgumentException("Maximum amount cannot be less than minimum amount");
            }
        }
        this.maxAmount = maxAmount;
    }

    /**
     * Validates that start date is before or equal to end date.
     * Replicates COBOL date range validation logic.
     * 
     * @param startDate the start date to validate
     * @param endDate the end date to validate
     * @throws IllegalArgumentException if date range is invalid
     */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("Start date cannot be after end date");
            }
        }
    }

    /**
     * Compares this TransactionListRequest with another object for equality.
     * Two requests are equal if all their fields are equal.
     * 
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TransactionListRequest that = (TransactionListRequest) obj;
        return Objects.equals(accountId, that.accountId) &&
               Objects.equals(cardNumber, that.cardNumber) &&
               Objects.equals(startDate, that.startDate) &&
               Objects.equals(endDate, that.endDate) &&
               Objects.equals(pageNumber, that.pageNumber) &&
               Objects.equals(pageSize, that.pageSize) &&
               Objects.equals(minAmount, that.minAmount) &&
               Objects.equals(maxAmount, that.maxAmount);
    }

    /**
     * Generates a hash code for this TransactionListRequest.
     * Hash code is based on all fields to ensure consistency with equals().
     * 
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(accountId, cardNumber, startDate, endDate, 
                           pageNumber, pageSize, minAmount, maxAmount);
    }

    /**
     * Returns a string representation of this TransactionListRequest.
     * Includes all fields in a readable format for debugging and logging.
     * Card number is masked for security purposes.
     * 
     * @return the string representation
     */
    @Override
    public String toString() {
        String maskedCardNumber = null;
        if (cardNumber != null && cardNumber.length() >= 4) {
            maskedCardNumber = "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
        } else if (cardNumber != null) {
            maskedCardNumber = "****";
        }
        
        return "TransactionListRequest{" +
               "accountId='" + accountId + '\'' +
               ", cardNumber='" + maskedCardNumber + '\'' +
               ", startDate=" + startDate +
               ", endDate=" + endDate +
               ", pageNumber=" + pageNumber +
               ", pageSize=" + pageSize +
               ", minAmount=" + minAmount +
               ", maxAmount=" + maxAmount +
               '}';
    }
}