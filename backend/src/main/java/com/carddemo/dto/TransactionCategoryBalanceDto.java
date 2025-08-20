/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.carddemo.util.Constants;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Transaction Category Balance Data Transfer Object.
 * 
 * This DTO represents transaction category balance information mapping to the COBOL
 * CVTRA01Y copybook TRAN-CAT-BAL-RECORD structure. It provides category balance
 * data for account analysis, statement generation, and balance reporting
 * during the mainframe-to-cloud migration.
 * 
 * COBOL Mapping:
 * - TRANCAT-ACCT-ID (PIC 9(11)) -> accountId
 * - TRANCAT-CD (PIC 9(04)) -> categoryCode
 * - TRAN-CAT-BAL (PIC S9(09)V99) -> balance
 * - Balance date added for temporal tracking
 * 
 * This class supports:
 * - Category balance retrieval for account analysis
 * - Balance aggregation across categories and date ranges
 * - Balance history tracking for reporting
 * - JSON serialization for REST API responses
 */
@Data
@NoArgsConstructor
public class TransactionCategoryBalanceDto {

    /**
     * Account ID for balance tracking.
     * 
     * Maps to COBOL TRANCAT-ACCT-ID field (PIC 9(11)).
     * Represents the 11-digit account identifier for which the category balance is maintained.
     */
    @JsonProperty("accountId")
    @NotNull(message = "Account ID is required")
    private Long accountId;

    /**
     * Transaction type code.
     * 
     * Optional field representing the transaction type associated with the balance.
     * Used for filtering and categorization but not part of the core balance key.
     */
    @JsonProperty("typeCode")
    @Size(max = 2, message = "Transaction type code cannot exceed 2 characters")
    private String typeCode;

    /**
     * Category code for balance categorization.
     * 
     * Maps to COBOL TRANCAT-CD field (PIC 9(04)).
     * Represents the 4-character category identifier used for transaction categorization.
     */
    @JsonProperty("categoryCode")
    @NotNull(message = "Category code is required")
    @Size(min = 4, max = 4, message = "Category code must be exactly 4 characters")
    private String categoryCode;

    /**
     * Category balance amount.
     * 
     * Maps to COBOL TRAN-CAT-BAL field (PIC S9(09)V99).
     * Uses BigDecimal with scale=2 to preserve COBOL COMP-3 packed decimal precision.
     * Supports positive and negative balances for debit/credit operations.
     */
    @JsonProperty("balance")
    @NotNull(message = "Balance is required")
    @DecimalMax(value = "9999999999.99", message = "Balance cannot exceed 9999999999.99")
    private BigDecimal balance;

    /**
     * Balance date for temporal tracking.
     * 
     * Optional field representing the date for which the balance is valid.
     * Used for balance history tracking and date-range queries.
     */
    @JsonProperty("balanceDate")
    private LocalDate balanceDate;

    /**
     * Creates a new TransactionCategoryBalanceDto with required fields.
     * 
     * This constructor ensures that core balance fields are properly initialized
     * for category balance operations and reporting.
     * 
     * @param accountId the 11-digit account identifier
     * @param typeCode the transaction type code (optional)
     * @param categoryCode the 4-character category code
     * @param balance the balance amount with 2 decimal precision
     */
    public TransactionCategoryBalanceDto(Long accountId, String typeCode, String categoryCode, BigDecimal balance) {
        this.accountId = accountId;
        this.typeCode = typeCode;
        this.categoryCode = categoryCode;
        this.balance = balance;
    }

    /**
     * Creates a new TransactionCategoryBalanceDto with all fields.
     * 
     * @param accountId the 11-digit account identifier
     * @param typeCode the transaction type code (optional)
     * @param categoryCode the 4-character category code
     * @param balance the balance amount with 2 decimal precision
     * @param balanceDate the balance date for temporal tracking
     */
    public TransactionCategoryBalanceDto(Long accountId, String typeCode, String categoryCode, 
                                       BigDecimal balance, LocalDate balanceDate) {
        this.accountId = accountId;
        this.typeCode = typeCode;
        this.categoryCode = categoryCode;
        this.balance = balance;
        this.balanceDate = balanceDate;
    }

    /**
     * Gets the account ID.
     * 
     * @return the 11-digit account identifier
     */
    public Long getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID.
     * 
     * @param accountId the 11-digit account identifier to set
     */
    public void setAccountId(Long accountId) {
        this.accountId = accountId;
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
     * Sets the transaction type code.
     * 
     * @param typeCode the transaction type code to set
     */
    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    /**
     * Gets the category code.
     * 
     * @return the 4-character category code
     */
    public String getCategoryCode() {
        return categoryCode;
    }

    /**
     * Sets the category code.
     * 
     * @param categoryCode the 4-character category code to set
     */
    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    /**
     * Gets the balance amount.
     * 
     * @return the balance amount with 2 decimal precision
     */
    public BigDecimal getBalance() {
        return balance;
    }

    /**
     * Sets the balance amount.
     * 
     * @param balance the balance amount to set
     */
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    /**
     * Gets the balance date.
     * 
     * @return the balance date for temporal tracking
     */
    public LocalDate getBalanceDate() {
        return balanceDate;
    }

    /**
     * Sets the balance date.
     * 
     * @param balanceDate the balance date to set
     */
    public void setBalanceDate(LocalDate balanceDate) {
        this.balanceDate = balanceDate;
    }

    /**
     * Checks if this TransactionCategoryBalanceDto is equal to another object.
     * 
     * Two instances are considered equal if they have the same accountId,
     * categoryCode, and balance values.
     * 
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TransactionCategoryBalanceDto that = (TransactionCategoryBalanceDto) obj;
        
        if (accountId != null ? !accountId.equals(that.accountId) : that.accountId != null) return false;
        if (categoryCode != null ? !categoryCode.equals(that.categoryCode) : that.categoryCode != null) return false;
        if (balance != null ? balance.compareTo(that.balance) != 0 : that.balance != null) return false;
        return typeCode != null ? typeCode.equals(that.typeCode) : that.typeCode == null;
    }

    /**
     * Returns the hash code for this TransactionCategoryBalanceDto.
     * 
     * The hash code is computed based on accountId, categoryCode, and balance
     * to ensure proper behavior in hash-based collections.
     * 
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        int result = accountId != null ? accountId.hashCode() : 0;
        result = 31 * result + (categoryCode != null ? categoryCode.hashCode() : 0);
        result = 31 * result + (balance != null ? balance.hashCode() : 0);
        result = 31 * result + (typeCode != null ? typeCode.hashCode() : 0);
        return result;
    }

    /**
     * Returns a string representation of this TransactionCategoryBalanceDto.
     * 
     * The string includes all fields for debugging and logging purposes.
     * 
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "TransactionCategoryBalanceDto{" +
                "accountId=" + accountId +
                ", typeCode='" + typeCode + '\'' +
                ", categoryCode='" + categoryCode + '\'' +
                ", balance=" + balance +
                ", balanceDate=" + balanceDate +
                '}';
    }
}