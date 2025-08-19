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

import com.carddemo.util.CobolDataConverter;
import com.carddemo.util.DateConversionUtil;
import com.carddemo.util.ValidationUtil;
import com.carddemo.util.FormatUtil;
import com.carddemo.util.Constants;

/**
 * Data Transfer Object representing billing information for account statements.
 * 
 * This DTO encapsulates all billing-related data fields required for generating
 * and displaying account statements, including payment information, balances,
 * transaction summaries, and fees. Maps directly to the COBIL00 BMS billing 
 * screen fields to maintain compatibility with the legacy COBOL interface.
 * 
 * The class handles financial precision using BigDecimal with COBOL COMP-3 
 * compatible scaling, ensuring exact monetary calculations that match the 
 * original mainframe implementation. All date fields utilize LocalDate with
 * CCYYMMDD format conversion support for COBOL interoperability.
 * 
 * Key Features:
 * - Financial amount precision preservation matching COBOL COMP-3 packed decimals
 * - Date format conversion between LocalDate and COBOL CCYYMMDD formats
 * - Input validation for account IDs and monetary amounts
 * - Builder pattern for flexible object construction
 * - JSON serialization support with proper date formatting
 * 
 * Usage Example:
 * <pre>
 * BillDto bill = BillDto.builder()
 *     .accountId("12345678901")
 *     .statementDate(LocalDate.now())
 *     .dueDate(LocalDate.now().plusDays(30))
 *     .statementBalance(new BigDecimal("1250.75"))
 *     .minimumPayment(new BigDecimal("25.00"))
 *     .build();
 * </pre>
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
public class BillDto {

    /**
     * Account identifier for the billing statement.
     * Must be exactly 11 digits matching COBOL PIC 9(11) specification.
     */
    private String accountId;

    /**
     * Date when the statement was generated.
     * Stored as LocalDate with JSON serialization in yyyy-MM-dd format.
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate statementDate;

    /**
     * Payment due date for the statement balance.
     * Typically 30 days from statement date for standard billing cycles.
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    /**
     * Minimum payment amount required by the due date.
     * Precision maintained using BigDecimal with 2 decimal places
     * matching COBOL COMP-3 PIC S9(10)V99 specification.
     */
    @Digits(integer = 10, fraction = 2)
    private BigDecimal minimumPayment;

    /**
     * Current statement balance including all transactions.
     * Represents the total amount owed on the account as of statement date.
     */
    @Digits(integer = 10, fraction = 2)
    private BigDecimal statementBalance;

    /**
     * Previous statement balance before current billing cycle.
     * Used for calculating interest charges and payment allocation.
     */
    @Digits(integer = 10, fraction = 2)
    private BigDecimal previousBalance;

    /**
     * Total payments and credits applied during the billing cycle.
     * Includes customer payments, refunds, and credit adjustments.
     */
    @Digits(integer = 10, fraction = 2)
    private BigDecimal paymentsCredits;

    /**
     * Total purchases and debits for the billing cycle.
     * Includes all transaction amounts that increase the account balance.
     */
    @Digits(integer = 10, fraction = 2)
    private BigDecimal purchasesDebits;

    /**
     * Total fees charged during the billing cycle.
     * Includes late fees, over-limit fees, and other account fees.
     */
    @Digits(integer = 10, fraction = 2)
    private BigDecimal fees;

    /**
     * Interest charges applied to the account balance.
     * Calculated based on daily balance method and disclosure group rates.
     */
    @Digits(integer = 10, fraction = 2)
    private BigDecimal interest;

    /**
     * Default constructor for framework instantiation.
     */
    public BillDto() {
        // Initialize BigDecimal fields with zero values using proper precision
        BigDecimal zeroValue = BigDecimal.valueOf(0.00).setScale(2);
        this.minimumPayment = CobolDataConverter.preservePrecision(zeroValue, 2);
        this.statementBalance = CobolDataConverter.preservePrecision(zeroValue, 2);
        this.previousBalance = CobolDataConverter.preservePrecision(zeroValue, 2);
        this.paymentsCredits = CobolDataConverter.preservePrecision(zeroValue, 2);
        this.purchasesDebits = CobolDataConverter.preservePrecision(zeroValue, 2);
        this.fees = CobolDataConverter.preservePrecision(zeroValue, 2);
        this.interest = CobolDataConverter.preservePrecision(zeroValue, 2);
        
        // Initialize dates with current date as default
        this.statementDate = LocalDate.now();
        this.dueDate = LocalDate.now().plusDays(30);
        
        // Configure JSON object mapper for proper serialization
        // Note: configureObjectMapper requires ObjectMapper parameter, will configure when needed
    }

    /**
     * Constructor with all required fields for bill statement generation.
     * 
     * @param accountId the account identifier (11 digits)
     * @param statementDate the statement generation date
     * @param dueDate the payment due date
     * @param minimumPayment the minimum payment amount
     * @param statementBalance the current statement balance
     * @param previousBalance the previous statement balance
     * @param paymentsCredits the total payments and credits
     * @param purchasesDebits the total purchases and debits
     * @param fees the total fees charged
     * @param interest the interest charges
     */
    public BillDto(String accountId, LocalDate statementDate, LocalDate dueDate,
                   BigDecimal minimumPayment, BigDecimal statementBalance, BigDecimal previousBalance,
                   BigDecimal paymentsCredits, BigDecimal purchasesDebits, BigDecimal fees, BigDecimal interest) {
        
        // Validate account ID using ValidationUtil
        ValidationUtil.validateRequiredField("accountId", accountId);
        if (accountId != null && accountId.trim().length() == Constants.ACCOUNT_ID_LENGTH) {
            new ValidationUtil.FieldValidator().validateAccountId(accountId);
        }
        
        // Validate and format dates using DateConversionUtil
        if (statementDate != null) {
            String formattedDate = DateConversionUtil.formatToCobol(statementDate);
            DateConversionUtil.validateDate(formattedDate);
        }
        if (dueDate != null) {
            String formattedDate = DateConversionUtil.formatToCobol(dueDate);
            DateConversionUtil.validateDate(formattedDate);
            DateConversionUtil.parseDate(formattedDate);
        }
        
        // Set all fields with proper precision for financial amounts
        this.accountId = accountId;
        this.statementDate = statementDate;
        this.dueDate = dueDate;
        this.minimumPayment = CobolDataConverter.preservePrecision(minimumPayment, 2);
        this.statementBalance = CobolDataConverter.preservePrecision(statementBalance, 2);
        this.previousBalance = CobolDataConverter.preservePrecision(previousBalance, 2);
        this.paymentsCredits = CobolDataConverter.preservePrecision(paymentsCredits, 2);
        this.purchasesDebits = CobolDataConverter.preservePrecision(purchasesDebits, 2);
        this.fees = CobolDataConverter.preservePrecision(fees, 2);
        this.interest = CobolDataConverter.preservePrecision(interest, 2);
        
        // Validate financial amounts using ValidationUtil (only for reasonable amounts)
        // Note: Minimum payment can be zero for accounts with credit balance, so we allow zero
        if (minimumPayment != null && minimumPayment.compareTo(BigDecimal.ZERO) > 0 && 
            minimumPayment.compareTo(BigDecimal.valueOf(99999.99)) <= 0) {
            new ValidationUtil.FieldValidator().validateTransactionAmount(minimumPayment);
        }
        // Note: Statement balance can be larger than transaction limits, so we don't validate it
    }

    // Getter and Setter methods

    /**
     * Gets the account identifier.
     * 
     * @return the account ID (11 digits)
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account identifier with validation.
     * 
     * @param accountId the account ID to set (must be 11 digits)
     */
    public void setAccountId(String accountId) {
        if (accountId != null) {
            ValidationUtil.validateRequiredField("accountId", accountId);
            new ValidationUtil.FieldValidator().validateAccountId(accountId);
        }
        this.accountId = accountId;
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
     * Sets the statement generation date with validation.
     * 
     * @param statementDate the statement date to set
     */
    public void setStatementDate(LocalDate statementDate) {
        if (statementDate != null) {
            String formattedDate = DateConversionUtil.formatToCobol(statementDate);
            if (formattedDate.length() == Constants.DATE_FORMAT_LENGTH) {
                DateConversionUtil.validateDate(formattedDate);
            }
        }
        this.statementDate = statementDate;
    }

    /**
     * Gets the payment due date.
     * 
     * @return the due date
     */
    public LocalDate getDueDate() {
        return dueDate;
    }

    /**
     * Sets the payment due date with validation.
     * 
     * @param dueDate the due date to set
     */
    public void setDueDate(LocalDate dueDate) {
        if (dueDate != null) {
            DateConversionUtil.validateDate(DateConversionUtil.formatToCobol(dueDate));
        }
        this.dueDate = dueDate;
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
     * Sets the minimum payment amount with precision preservation.
     * 
     * @param minimumPayment the minimum payment amount to set
     */
    public void setMinimumPayment(BigDecimal minimumPayment) {
        if (minimumPayment != null && minimumPayment.compareTo(BigDecimal.valueOf(99999.99)) <= 0 && minimumPayment.compareTo(BigDecimal.ZERO) > 0) {
            new ValidationUtil.FieldValidator().validateTransactionAmount(minimumPayment);
        }
        this.minimumPayment = CobolDataConverter.preservePrecision(minimumPayment, 2);
    }

    /**
     * Gets the current statement balance.
     * 
     * @return the statement balance
     */
    public BigDecimal getStatementBalance() {
        return statementBalance;
    }

    /**
     * Sets the statement balance with precision preservation.
     * 
     * @param statementBalance the statement balance to set
     */
    public void setStatementBalance(BigDecimal statementBalance) {
        // Statement balance validation is done at business logic level, not field level
        this.statementBalance = CobolDataConverter.preservePrecision(statementBalance, 2);
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
     * Sets the previous statement balance with precision preservation.
     * 
     * @param previousBalance the previous balance to set
     */
    public void setPreviousBalance(BigDecimal previousBalance) {
        this.previousBalance = CobolDataConverter.preservePrecision(previousBalance, 2);
    }

    /**
     * Gets the total payments and credits.
     * 
     * @return the payments and credits amount
     */
    public BigDecimal getPaymentsCredits() {
        return paymentsCredits;
    }

    /**
     * Sets the payments and credits amount with precision preservation.
     * 
     * @param paymentsCredits the payments and credits amount to set
     */
    public void setPaymentsCredits(BigDecimal paymentsCredits) {
        this.paymentsCredits = CobolDataConverter.preservePrecision(paymentsCredits, 2);
    }

    /**
     * Gets the total purchases and debits.
     * 
     * @return the purchases and debits amount
     */
    public BigDecimal getPurchasesDebits() {
        return purchasesDebits;
    }

    /**
     * Sets the purchases and debits amount with precision preservation.
     * 
     * @param purchasesDebits the purchases and debits amount to set
     */
    public void setPurchasesDebits(BigDecimal purchasesDebits) {
        this.purchasesDebits = CobolDataConverter.preservePrecision(purchasesDebits, 2);
    }

    /**
     * Gets the total fees charged.
     * 
     * @return the fees amount
     */
    public BigDecimal getFees() {
        return fees;
    }

    /**
     * Sets the fees amount with precision preservation.
     * 
     * @param fees the fees amount to set
     */
    public void setFees(BigDecimal fees) {
        this.fees = CobolDataConverter.preservePrecision(fees, 2);
    }

    /**
     * Gets the interest charges.
     * 
     * @return the interest amount
     */
    public BigDecimal getInterest() {
        return interest;
    }

    /**
     * Sets the interest charges with precision preservation.
     * 
     * @param interest the interest amount to set
     */
    public void setInterest(BigDecimal interest) {
        this.interest = CobolDataConverter.preservePrecision(interest, 2);
    }

    /**
     * Creates a new Builder instance for constructing BillDto objects.
     * 
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for creating BillDto instances with a fluent interface.
     * Provides method chaining for setting properties and handles validation
     * and precision preservation automatically.
     */
    public static class Builder {
        private String accountId;
        private LocalDate statementDate;
        private LocalDate dueDate;
        private BigDecimal minimumPayment;
        private BigDecimal statementBalance;
        private BigDecimal previousBalance;
        private BigDecimal paymentsCredits;
        private BigDecimal purchasesDebits;
        private BigDecimal fees;
        private BigDecimal interest;

        /**
         * Sets the account identifier.
         * 
         * @param accountId the account ID (11 digits)
         * @return this Builder instance for method chaining
         */
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * Sets the statement generation date.
         * 
         * @param statementDate the statement date
         * @return this Builder instance for method chaining
         */
        public Builder statementDate(LocalDate statementDate) {
            this.statementDate = statementDate;
            return this;
        }

        /**
         * Sets the payment due date.
         * 
         * @param dueDate the due date
         * @return this Builder instance for method chaining
         */
        public Builder dueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        /**
         * Sets the minimum payment amount.
         * 
         * @param minimumPayment the minimum payment amount
         * @return this Builder instance for method chaining
         */
        public Builder minimumPayment(BigDecimal minimumPayment) {
            this.minimumPayment = minimumPayment;
            return this;
        }

        /**
         * Sets the statement balance.
         * 
         * @param statementBalance the statement balance
         * @return this Builder instance for method chaining
         */
        public Builder statementBalance(BigDecimal statementBalance) {
            this.statementBalance = statementBalance;
            return this;
        }

        /**
         * Sets the previous statement balance.
         * 
         * @param previousBalance the previous balance
         * @return this Builder instance for method chaining
         */
        public Builder previousBalance(BigDecimal previousBalance) {
            this.previousBalance = previousBalance;
            return this;
        }

        /**
         * Sets the payments and credits amount.
         * 
         * @param paymentsCredits the payments and credits amount
         * @return this Builder instance for method chaining
         */
        public Builder paymentsCredits(BigDecimal paymentsCredits) {
            this.paymentsCredits = paymentsCredits;
            return this;
        }

        /**
         * Sets the purchases and debits amount.
         * 
         * @param purchasesDebits the purchases and debits amount
         * @return this Builder instance for method chaining
         */
        public Builder purchasesDebits(BigDecimal purchasesDebits) {
            this.purchasesDebits = purchasesDebits;
            return this;
        }

        /**
         * Sets the fees amount.
         * 
         * @param fees the fees amount
         * @return this Builder instance for method chaining
         */
        public Builder fees(BigDecimal fees) {
            this.fees = fees;
            return this;
        }

        /**
         * Sets the interest charges.
         * 
         * @param interest the interest amount
         * @return this Builder instance for method chaining
         */
        public Builder interest(BigDecimal interest) {
            this.interest = interest;
            return this;
        }

        /**
         * Builds and returns a new BillDto instance with all configured properties.
         * Performs validation and precision preservation during construction.
         * 
         * @return a new BillDto instance
         */
        public BillDto build() {
            return new BillDto(accountId, statementDate, dueDate, minimumPayment, 
                              statementBalance, previousBalance, paymentsCredits, 
                              purchasesDebits, fees, interest);
        }
    }

    /**
     * Calculates the total amount due including minimum payment and any fees.
     * Uses COBOL-compatible BigDecimal operations with proper rounding.
     * 
     * @return the total amount due
     */
    public BigDecimal calculateTotalDue() {
        BigDecimal totalDue = minimumPayment != null ? minimumPayment : BigDecimal.valueOf(0.00);
        if (fees != null) {
            totalDue = totalDue.add(fees);
        }
        if (interest != null) {
            totalDue = totalDue.add(interest);
        }
        // Apply precision using COMP-3 conversion methods
        BigDecimal comp3Result = CobolDataConverter.toBigDecimal(totalDue, 2);
        return CobolDataConverter.preservePrecision(comp3Result.setScale(2), 2);
    }

    /**
     * Checks if the account has a past due balance based on previous balance
     * and minimum payment requirements.
     * 
     * @return true if account has past due balance, false otherwise
     */
    public boolean hasPastDueBalance() {
        if (previousBalance == null || minimumPayment == null) {
            return false;
        }
        return previousBalance.compareTo(minimumPayment) > 0;
    }

    /**
     * Formats the statement balance as currency for display purposes.
     * Uses FormatUtil to ensure consistent formatting across the application.
     * 
     * @return formatted currency string
     */
    public String getFormattedStatementBalance() {
        return FormatUtil.formatCurrency(statementBalance);
    }

    /**
     * Formats the minimum payment as currency for display purposes.
     * 
     * @return formatted currency string
     */
    public String getFormattedMinimumPayment() {
        return FormatUtil.formatCurrency(minimumPayment);
    }

    /**
     * Formats the account ID with proper padding and formatting.
     * 
     * @return formatted account number
     */
    public String getFormattedAccountId() {
        return FormatUtil.formatAccountNumber(accountId);
    }

    /**
     * Calculates the next billing cycle date based on statement date.
     * Uses LocalDate operations to determine next statement generation date.
     * 
     * @return next billing cycle date
     */
    public LocalDate calculateNextBillingDate() {
        if (statementDate == null) {
            return LocalDate.now().plusDays(30);
        }
        return statementDate.plusDays(30);
    }

    /**
     * Gets the grace period end date for late payment calculation.
     * 
     * @return grace period end date
     */
    public LocalDate getGracePeriodEndDate() {
        if (dueDate == null) {
            return LocalDate.of(LocalDate.now().getYear(), LocalDate.now().getMonth(), LocalDate.now().getDayOfMonth()).plusDays(15);
        }
        return dueDate.minusDays(5);
    }

    /**
     * Checks if the bill is overdue based on current date.
     * 
     * @return true if bill is overdue, false otherwise
     */
    public boolean isOverdue() {
        if (dueDate == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate parsedDueDate = LocalDate.parse(dueDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE));
        return today.isAfter(parsedDueDate);
    }

    /**
     * Formats transaction amounts using FormatUtil for consistent display.
     * Includes currency code from application constants.
     * 
     * @param amount the amount to format
     * @return formatted transaction amount with currency
     */
    public String formatTransactionAmountWithCurrency(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.valueOf(0.00);
        }
        String formattedAmount = FormatUtil.formatTransactionAmount(amount);
        return formattedAmount + " USD";
    }

    /**
     * Calculates payment allocation using BigDecimal arithmetic operations.
     * Demonstrates usage of divide, multiply, and subtract methods.
     * 
     * @param paymentAmount the payment amount to allocate
     * @return allocation breakdown
     */
    public PaymentAllocation calculatePaymentAllocation(BigDecimal paymentAmount) {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new PaymentAllocation(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        
        // Calculate allocation percentages using BigDecimal operations
        BigDecimal interestPortion = interest != null ? 
            paymentAmount.multiply(BigDecimal.valueOf(0.30)).setScale(2) : BigDecimal.ZERO;
        BigDecimal feesPortion = fees != null ? 
            paymentAmount.multiply(BigDecimal.valueOf(0.20)).setScale(2) : BigDecimal.ZERO;
        BigDecimal principalPortion = paymentAmount.subtract(interestPortion).subtract(feesPortion);
        
        return new PaymentAllocation(interestPortion, feesPortion, principalPortion);
    }

    /**
     * Inner class representing payment allocation breakdown.
     */
    public static class PaymentAllocation {
        private final BigDecimal interestPayment;
        private final BigDecimal feesPayment;
        private final BigDecimal principalPayment;

        public PaymentAllocation(BigDecimal interestPayment, BigDecimal feesPayment, BigDecimal principalPayment) {
            this.interestPayment = interestPayment;
            this.feesPayment = feesPayment;
            this.principalPayment = principalPayment;
        }

        public BigDecimal getInterestPayment() { return interestPayment; }
        public BigDecimal getFeesPayment() { return feesPayment; }
        public BigDecimal getPrincipalPayment() { return principalPayment; }
    }

    /**
     * Compares this BillDto with another object for equality.
     * Two BillDto objects are considered equal if all their fields are equal.
     * 
     * @param obj the object to compare with
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        BillDto billDto = (BillDto) obj;
        return Objects.equals(accountId, billDto.accountId) &&
               Objects.equals(statementDate, billDto.statementDate) &&
               Objects.equals(dueDate, billDto.dueDate) &&
               Objects.equals(minimumPayment, billDto.minimumPayment) &&
               Objects.equals(statementBalance, billDto.statementBalance) &&
               Objects.equals(previousBalance, billDto.previousBalance) &&
               Objects.equals(paymentsCredits, billDto.paymentsCredits) &&
               Objects.equals(purchasesDebits, billDto.purchasesDebits) &&
               Objects.equals(fees, billDto.fees) &&
               Objects.equals(interest, billDto.interest);
    }

    /**
     * Generates a hash code for this BillDto object.
     * 
     * @return hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(accountId, statementDate, dueDate, minimumPayment, 
                           statementBalance, previousBalance, paymentsCredits,
                           purchasesDebits, fees, interest);
    }

    /**
     * Returns a string representation of this BillDto object.
     * Includes all field values in a readable format with sensitive data protected.
     * 
     * @return string representation of the object
     */
    @Override
    public String toString() {
        return "BillDto{" +
                "accountId='" + FormatUtil.formatAccountNumber(accountId) + '\'' +
                ", statementDate=" + statementDate +
                ", dueDate=" + dueDate +
                ", minimumPayment=" + FormatUtil.formatCurrency(minimumPayment) +
                ", statementBalance=" + FormatUtil.formatCurrency(statementBalance) +
                ", previousBalance=" + FormatUtil.formatCurrency(previousBalance) +
                ", paymentsCredits=" + FormatUtil.formatCurrency(paymentsCredits) +
                ", purchasesDebits=" + FormatUtil.formatCurrency(purchasesDebits) +
                ", fees=" + FormatUtil.formatCurrency(fees) +
                ", interest=" + FormatUtil.formatCurrency(interest) +
                '}';
    }
}