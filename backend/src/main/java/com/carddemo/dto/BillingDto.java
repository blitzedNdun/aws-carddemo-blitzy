/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Digits;

/**
 * Data Transfer Object representing simplified billing information for REST API responses.
 * 
 * This DTO provides essential billing data for account statements and billing operations,
 * optimized for REST controller responses and JSON serialization. Designed as a simplified
 * alternative to BillDto for API endpoints requiring basic billing information.
 * 
 * The class maintains financial precision using BigDecimal with COBOL COMP-3 compatible
 * scaling, ensuring monetary calculations match the original mainframe implementation.
 * All fields are designed for JSON serialization with proper date formatting.
 * 
 * Used primarily by BillingController for REST API responses providing account billing
 * status, current balances, minimum payments, and transaction summaries.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
public class BillingDto {

    /**
     * Account identifier for the billing information.
     * Must be exactly 11 digits matching COBOL PIC 9(11) specification.
     */
    private String accountId;

    /**
     * Current account balance including all posted transactions.
     * Represents the total amount owed on the account.
     */
    @Digits(integer = 10, fraction = 2)
    private BigDecimal currentBalance;

    /**
     * Minimum payment amount required by the due date.
     * Calculated using COBOL-equivalent financial logic with BigDecimal precision.
     */
    @Digits(integer = 10, fraction = 2)
    private BigDecimal minimumPayment;

    /**
     * Payment due date for the minimum payment amount.
     * Typically 30 days from statement date for standard billing cycles.
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate paymentDueDate;

    /**
     * Date when the statement was generated.
     * Used for billing cycle calculations and payment processing.
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate statementDate;

    /**
     * Previous statement balance before current billing cycle transactions.
     * Used for interest calculations and payment allocation.
     */
    @Digits(integer = 10, fraction = 2)
    private BigDecimal previousBalance;

    /**
     * Total purchase amounts for the billing cycle.
     * Includes all debits that increase the account balance.
     */
    @Digits(integer = 10, fraction = 2)
    private BigDecimal totalPurchases;

    /**
     * Total payment amounts for the billing cycle.
     * Includes all credits that reduce the account balance.
     */
    @Digits(integer = 10, fraction = 2)
    private BigDecimal totalPayments;

    /**
     * Default constructor for framework instantiation.
     */
    public BillingDto() {
        // Initialize BigDecimal fields with zero values using proper monetary precision
        this.currentBalance = BigDecimal.ZERO.setScale(2);
        this.minimumPayment = BigDecimal.ZERO.setScale(2);
        this.previousBalance = BigDecimal.ZERO.setScale(2);
        this.totalPurchases = BigDecimal.ZERO.setScale(2);
        this.totalPayments = BigDecimal.ZERO.setScale(2);
        
        // Initialize dates with current date as default
        this.statementDate = LocalDate.now();
        this.paymentDueDate = LocalDate.now().plusDays(30);
    }

    /**
     * Constructor with all required fields for billing information.
     * 
     * @param accountId the account identifier
     * @param currentBalance the current account balance
     * @param minimumPayment the minimum payment amount
     * @param paymentDueDate the payment due date
     * @param statementDate the statement generation date
     * @param previousBalance the previous statement balance
     * @param totalPurchases the total purchases for the cycle
     * @param totalPayments the total payments for the cycle
     */
    public BillingDto(String accountId, BigDecimal currentBalance, BigDecimal minimumPayment,
                     LocalDate paymentDueDate, LocalDate statementDate, BigDecimal previousBalance,
                     BigDecimal totalPurchases, BigDecimal totalPayments) {
        this.accountId = accountId;
        this.currentBalance = ensureMonetaryPrecision(currentBalance);
        this.minimumPayment = ensureMonetaryPrecision(minimumPayment);
        this.paymentDueDate = paymentDueDate;
        this.statementDate = statementDate;
        this.previousBalance = ensureMonetaryPrecision(previousBalance);
        this.totalPurchases = ensureMonetaryPrecision(totalPurchases);
        this.totalPayments = ensureMonetaryPrecision(totalPayments);
    }

    /**
     * Gets the account identifier.
     * 
     * @return the account ID
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account identifier.
     * 
     * @param accountId the account ID to set
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the current account balance.
     * 
     * @return the current balance
     */
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    /**
     * Sets the current account balance.
     * 
     * @param currentBalance the current balance to set
     */
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = ensureMonetaryPrecision(currentBalance);
    }

    /**
     * Gets the minimum payment amount.
     * 
     * @return the minimum payment amount
     */
    public BigDecimal getMinimumPayment() {
        return minimumPayment;
    }

    /**
     * Sets the minimum payment amount.
     * 
     * @param minimumPayment the minimum payment amount to set
     */
    public void setMinimumPayment(BigDecimal minimumPayment) {
        this.minimumPayment = ensureMonetaryPrecision(minimumPayment);
    }

    /**
     * Gets the payment due date.
     * 
     * @return the payment due date
     */
    public LocalDate getPaymentDueDate() {
        return paymentDueDate;
    }

    /**
     * Sets the payment due date.
     * 
     * @param paymentDueDate the payment due date to set
     */
    public void setPaymentDueDate(LocalDate paymentDueDate) {
        this.paymentDueDate = paymentDueDate;
    }

    /**
     * Gets the statement generation date.
     * 
     * @return the statement date
     */
    public LocalDate getStatementDate() {
        return statementDate;
    }

    /**
     * Sets the statement generation date.
     * 
     * @param statementDate the statement date to set
     */
    public void setStatementDate(LocalDate statementDate) {
        this.statementDate = statementDate;
    }

    /**
     * Gets the previous statement balance.
     * 
     * @return the previous balance
     */
    public BigDecimal getPreviousBalance() {
        return previousBalance;
    }

    /**
     * Sets the previous statement balance.
     * 
     * @param previousBalance the previous balance to set
     */
    public void setPreviousBalance(BigDecimal previousBalance) {
        this.previousBalance = ensureMonetaryPrecision(previousBalance);
    }

    /**
     * Gets the total purchases for the billing cycle.
     * 
     * @return the total purchases amount
     */
    public BigDecimal getTotalPurchases() {
        return totalPurchases;
    }

    /**
     * Sets the total purchases for the billing cycle.
     * 
     * @param totalPurchases the total purchases amount to set
     */
    public void setTotalPurchases(BigDecimal totalPurchases) {
        this.totalPurchases = ensureMonetaryPrecision(totalPurchases);
    }

    /**
     * Gets the total payments for the billing cycle.
     * 
     * @return the total payments amount
     */
    public BigDecimal getTotalPayments() {
        return totalPayments;
    }

    /**
     * Sets the total payments for the billing cycle.
     * 
     * @param totalPayments the total payments amount to set
     */
    public void setTotalPayments(BigDecimal totalPayments) {
        this.totalPayments = ensureMonetaryPrecision(totalPayments);
    }

    /**
     * Ensures BigDecimal values have proper monetary precision (2 decimal places).
     * Helper method to maintain COBOL COMP-3 equivalent precision.
     * 
     * @param amount the amount to format
     * @return amount with proper monetary precision
     */
    private BigDecimal ensureMonetaryPrecision(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return amount.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Compares this BillingDto with another object for equality.
     * 
     * @param obj the object to compare with
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        BillingDto that = (BillingDto) obj;
        return Objects.equals(accountId, that.accountId) &&
               Objects.equals(currentBalance, that.currentBalance) &&
               Objects.equals(minimumPayment, that.minimumPayment) &&
               Objects.equals(paymentDueDate, that.paymentDueDate) &&
               Objects.equals(statementDate, that.statementDate) &&
               Objects.equals(previousBalance, that.previousBalance) &&
               Objects.equals(totalPurchases, that.totalPurchases) &&
               Objects.equals(totalPayments, that.totalPayments);
    }

    /**
     * Generates a hash code for this BillingDto object.
     * 
     * @return hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(accountId, currentBalance, minimumPayment, paymentDueDate,
                           statementDate, previousBalance, totalPurchases, totalPayments);
    }

    /**
     * Returns a string representation of this BillingDto object.
     * 
     * @return string representation of the object
     */
    @Override
    public String toString() {
        return "BillingDto{" +
                "accountId='" + accountId + '\'' +
                ", currentBalance=" + currentBalance +
                ", minimumPayment=" + minimumPayment +
                ", paymentDueDate=" + paymentDueDate +
                ", statementDate=" + statementDate +
                ", previousBalance=" + previousBalance +
                ", totalPurchases=" + totalPurchases +
                ", totalPayments=" + totalPayments +
                '}';
    }
}