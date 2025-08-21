/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import com.carddemo.entity.Transaction;
import com.carddemo.util.Constants;

/**
 * Data Transfer Object for bill detail response containing comprehensive billing information.
 * 
 * This DTO maps billing statement data from the BillInquiryService to REST API responses,
 * providing complete billing details including current balance, payment history, and 
 * calculated minimum payment amounts. Supports the COBIL01C billing inquiry functionality 
 * converted from COBOL to Java Spring Boot service.
 * 
 * Key Features:
 * - Complete billing statement information with current and previous balances
 * - Calculated minimum payment based on COBOL business rules
 * - Payment history with transaction details for account
 * - Interest charges calculation with precise decimal arithmetic
 * - Due date information for payment scheduling
 * - BigDecimal precision for all financial amounts matching COBOL COMP-3
 * - ISO-8601 date formatting for REST API compatibility
 * - Jakarta Bean Validation for field constraints
 * - Jackson JSON serialization with custom property names
 * - Lombok-generated getters, setters, equals, hashCode, and toString
 * 
 * Billing Information Structure:
 * - Account identification and statement period details
 * - Current balance and previous balance with exact decimal precision
 * - Minimum payment calculation following COBOL payment rules
 * - Interest charges based on previous balance and payment history
 * - Payment due date for billing cycle management
 * - Complete payment transaction history for the account
 * - Statement date for billing cycle identification
 * 
 * Usage in REST Controllers:
 * - Billing inquiry endpoints (GET /api/billing/{accountId})
 * - Statement detail endpoints (GET /api/billing/{accountId}/statements)
 * - Payment history endpoints (GET /api/billing/{accountId}/payments)
 * 
 * Security Considerations:
 * - Account ID validation ensuring 11-digit format
 * - Financial amounts with precise decimal handling
 * - Payment history limited to authorized account access
 * 
 * Performance Considerations:
 * - Lightweight DTO suitable for billing inquiry responses
 * - BigDecimal arithmetic preserves exact financial precision
 * - LocalDate provides efficient date operations for billing cycles
 * 
 * @author CardDemo Migration Team  
 * @version 1.0
 * @since CardDemo v1.0
 */
@Data
public class BillDetailResponse {

    /**
     * Account ID for the billing inquiry - 11-digit account identifier.
     * Maps to request accountId and validates against COBOL PIC 9(11) constraint.
     * Used for account identification and billing statement correlation.
     */
    @JsonProperty("accountId")
    @Size(min = 11, max = 11, message = "Account ID must be exactly 11 digits")
    private String accountId;

    /**
     * Current account balance with exact decimal precision.
     * Maps to statement currentBalance field with BigDecimal for COBOL COMP-3 compatibility.
     * Represents the total outstanding amount on the account as of statement date.
     */
    @JsonProperty("currentBalance")
    private BigDecimal currentBalance;

    /**
     * Previous statement balance for comparison and interest calculation.
     * Maps to statement previousBalance field maintaining COBOL precision.
     * Used for calculating interest charges and payment progress.
     */
    @JsonProperty("previousBalance")
    private BigDecimal previousBalance;

    /**
     * Calculated minimum payment amount based on COBOL business rules.
     * Uses the greater of 2% of current balance or $25.00 fixed minimum.
     * Exact decimal precision matching COBOL minimum payment calculation.
     */
    @JsonProperty("minimumPayment")
    private BigDecimal minimumPayment;

    /**
     * Payment due date for the current billing cycle.
     * Maps to statement dueDate field formatted as ISO-8601 date.
     * Used for payment scheduling and late fee calculations.
     */
    @JsonProperty("paymentDueDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate paymentDueDate;

    /**
     * Interest charges calculated on previous balance.
     * Based on COBOL interest calculation logic with monthly rate application.
     * Uses BigDecimal for exact precision in interest amount calculations.
     */
    @JsonProperty("interestCharges")
    private BigDecimal interestCharges;

    /**
     * Statement date for the current billing period.
     * Maps to statement statementDate field formatted as ISO-8601.
     * Identifies the billing cycle period for this statement.
     */
    @JsonProperty("statementDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate statementDate;

    /**
     * Complete payment history for the account within requested date range.
     * List of Transaction objects containing payment details and amounts.
     * Filtered to show only payment-type transactions (credits to account).
     */
    @JsonProperty("paymentHistory")
    private List<Transaction> paymentHistory;

    /**
     * Sets the current balance with proper scale for financial precision.
     * 
     * Ensures the balance is set with exactly 2 decimal places matching COBOL
     * S9(09)V99 specification. Uses HALF_UP rounding mode to match COBOL
     * ROUNDED clause behavior for consistent financial calculations.
     * 
     * @param currentBalance the current account balance to set with proper scale
     */
    public void setCurrentBalance(BigDecimal currentBalance) {
        if (currentBalance != null) {
            this.currentBalance = currentBalance.setScale(2, BigDecimal.ROUND_HALF_UP);
        } else {
            this.currentBalance = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
    }

    /**
     * Returns the current balance, ensuring proper scale is maintained.
     * 
     * Guarantees that returned balance always has exactly 2 decimal places
     * for consistent financial precision across all billing operations.
     * 
     * @return current balance with 2 decimal places, never null
     */
    public BigDecimal getCurrentBalance() {
        if (this.currentBalance == null) {
            return BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        return this.currentBalance.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Sets the previous balance with proper scale for financial precision.
     * 
     * Ensures the balance is set with exactly 2 decimal places for consistency
     * with current balance and interest calculation requirements.
     * 
     * @param previousBalance the previous statement balance to set with proper scale
     */
    public void setPreviousBalance(BigDecimal previousBalance) {
        if (previousBalance != null) {
            this.previousBalance = previousBalance.setScale(2, BigDecimal.ROUND_HALF_UP);
        } else {
            this.previousBalance = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
    }

    /**
     * Returns the previous balance, ensuring proper scale is maintained.
     * 
     * Guarantees that returned balance always has exactly 2 decimal places
     * for consistent interest calculation and comparison operations.
     * 
     * @return previous balance with 2 decimal places, never null
     */
    public BigDecimal getPreviousBalance() {
        if (this.previousBalance == null) {
            return BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        return this.previousBalance.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Sets the minimum payment with proper scale for financial precision.
     * 
     * Ensures the minimum payment is set with exactly 2 decimal places
     * matching COBOL minimum payment calculation requirements.
     * 
     * @param minimumPayment the calculated minimum payment to set with proper scale
     */
    public void setMinimumPayment(BigDecimal minimumPayment) {
        if (minimumPayment != null) {
            this.minimumPayment = minimumPayment.setScale(2, BigDecimal.ROUND_HALF_UP);
        } else {
            this.minimumPayment = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
    }

    /**
     * Returns the minimum payment, ensuring proper scale is maintained.
     * 
     * Guarantees that returned minimum payment always has exactly 2 decimal places
     * for consistent payment processing and display formatting.
     * 
     * @return minimum payment with 2 decimal places, never null
     */
    public BigDecimal getMinimumPayment() {
        if (this.minimumPayment == null) {
            return BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        return this.minimumPayment.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Sets the interest charges with proper scale for financial precision.
     * 
     * Ensures the interest charges are set with exactly 2 decimal places
     * matching COBOL interest calculation and billing requirements.
     * 
     * @param interestCharges the calculated interest charges to set with proper scale
     */
    public void setInterestCharges(BigDecimal interestCharges) {
        if (interestCharges != null) {
            this.interestCharges = interestCharges.setScale(2, BigDecimal.ROUND_HALF_UP);
        } else {
            this.interestCharges = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
    }

    /**
     * Returns the interest charges, ensuring proper scale is maintained.
     * 
     * Guarantees that returned interest charges always have exactly 2 decimal places
     * for consistent billing calculations and statement display.
     * 
     * @return interest charges with 2 decimal places, never null
     */
    public BigDecimal getInterestCharges() {
        if (this.interestCharges == null) {
            return BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        return this.interestCharges.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Validates that all required billing information is present.
     * 
     * Ensures essential billing data including account ID, current balance,
     * minimum payment, and due date are available for complete billing response.
     * 
     * @return true if all required fields are present, false otherwise
     */
    public boolean isComplete() {
        return accountId != null && !accountId.trim().isEmpty() &&
               currentBalance != null &&
               minimumPayment != null &&
               paymentDueDate != null;
    }

    /**
     * Determines if the account has an outstanding balance requiring payment.
     * 
     * Checks if current balance is greater than zero indicating payment is due.
     * Used for payment reminder logic and billing status determination.
     * 
     * @return true if account has outstanding balance, false otherwise
     */
    public boolean hasOutstandingBalance() {
        return getCurrentBalance().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Determines if interest charges have been applied to this statement.
     * 
     * Checks if interest charges are greater than zero indicating interest accrual.
     * Used for interest notification and billing calculation verification.
     * 
     * @return true if interest charges are present, false otherwise
     */
    public boolean hasInterestCharges() {
        return getInterestCharges().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Calculates the total amount due including balance and interest charges.
     * 
     * Provides the complete amount due for payment processing including
     * current balance plus any accrued interest charges.
     * 
     * @return total amount due with 2 decimal places precision
     */
    public BigDecimal getTotalAmountDue() {
        return getCurrentBalance().add(getInterestCharges()).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Determines if payment history is available for this billing response.
     * 
     * Checks if payment history list is present and contains payment transactions.
     * Used for payment history display and account activity verification.
     * 
     * @return true if payment history is available, false otherwise
     */
    public boolean hasPaymentHistory() {
        return paymentHistory != null && !paymentHistory.isEmpty();
    }
}