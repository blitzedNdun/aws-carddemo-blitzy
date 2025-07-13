package com.carddemo.account;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;

/**
 * Account Balance Data Transfer Object
 * 
 * Provides current balance information with before/after comparison for real-time 
 * balance updates, including credit limit information and balance validation.
 * 
 * This DTO maintains COBOL COMP-3 precision equivalent using BigDecimal with 
 * DECIMAL(12,2) format ensuring exact financial calculations without floating-point errors.
 * 
 * Based on CVACT01Y.cpy account record structure:
 * - ACCT-CURR-BAL PIC S9(10)V99 -> BigDecimal with precision 12, scale 2
 * - ACCT-CREDIT-LIMIT PIC S9(10)V99 -> BigDecimal with precision 12, scale 2
 * 
 * @author Blitzy agent
 * @version 1.0
 * @since CardDemo v1.0-15-g27d6c6f-68
 */
public class AccountBalanceDto {

    /**
     * Math context for COBOL COMP-3 equivalent precision
     * Uses DECIMAL128 with HALF_EVEN rounding to match mainframe behavior
     */
    private static final MathContext COBOL_MATH_CONTEXT = new MathContext(31, RoundingMode.HALF_EVEN);

    /**
     * Account identifier (11 digits) - ACCT-ID from CVACT01Y.cpy
     */
    @JsonProperty("accountId")
    private String accountId;

    /**
     * Current account balance - ACCT-CURR-BAL from CVACT01Y.cpy
     * DECIMAL(12,2) precision for exact financial calculations
     */
    @JsonProperty("currentBalance")
    @Digits(integer = 10, fraction = 2, message = "Current balance must have max 10 integer and 2 fractional digits")
    private BigDecimal currentBalance;

    /**
     * Previous account balance for before/after comparison
     * Used for payment confirmation and balance change validation
     */
    @JsonProperty("previousBalance")
    @Digits(integer = 10, fraction = 2, message = "Previous balance must have max 10 integer and 2 fractional digits")
    private BigDecimal previousBalance;

    /**
     * Calculated balance difference (currentBalance - previousBalance)
     * Computed dynamically to show transaction impact
     */
    @JsonProperty("balanceDifference")
    @Digits(integer = 10, fraction = 2, message = "Balance difference must have max 10 integer and 2 fractional digits")
    private BigDecimal balanceDifference;

    /**
     * Credit limit - ACCT-CREDIT-LIMIT from CVACT01Y.cpy
     * Maximum credit amount available for the account
     */
    @JsonProperty("creditLimit")
    @Digits(integer = 10, fraction = 2, message = "Credit limit must have max 10 integer and 2 fractional digits")
    private BigDecimal creditLimit;

    /**
     * Available credit calculated as (creditLimit - currentBalance)
     * Shows remaining credit capacity for new transactions
     */
    @JsonProperty("availableCredit")
    @Digits(integer = 10, fraction = 2, message = "Available credit must have max 10 integer and 2 fractional digits")
    private BigDecimal availableCredit;

    /**
     * Default constructor initializing all BigDecimal fields to zero
     * with proper scale for financial calculations
     */
    public AccountBalanceDto() {
        this.currentBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        this.previousBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        this.balanceDifference = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        this.creditLimit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        this.availableCredit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * Constructor with all balance information
     * Automatically calculates derived fields (balanceDifference, availableCredit)
     * 
     * @param accountId Account identifier
     * @param currentBalance Current account balance
     * @param previousBalance Previous balance for comparison
     * @param creditLimit Maximum credit limit
     */
    public AccountBalanceDto(String accountId, BigDecimal currentBalance, 
                           BigDecimal previousBalance, BigDecimal creditLimit) {
        this.accountId = accountId;
        this.currentBalance = currentBalance != null ? 
            currentBalance.setScale(2, RoundingMode.HALF_EVEN) : 
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        this.previousBalance = previousBalance != null ? 
            previousBalance.setScale(2, RoundingMode.HALF_EVEN) : 
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        this.creditLimit = creditLimit != null ? 
            creditLimit.setScale(2, RoundingMode.HALF_EVEN) : 
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        
        // Calculate derived fields using COBOL-equivalent precision
        calculateBalanceDifference();
        calculateAvailableCredit();
    }

    /**
     * Gets the account identifier
     * @return Account ID as 11-digit string
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account identifier
     * @param accountId Account ID as 11-digit string
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the current account balance
     * @return Current balance with DECIMAL(12,2) precision
     */
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    /**
     * Sets the current account balance and recalculates derived fields
     * @param currentBalance New current balance
     */
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance != null ? 
            currentBalance.setScale(2, RoundingMode.HALF_EVEN) : 
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        calculateBalanceDifference();
        calculateAvailableCredit();
    }

    /**
     * Gets the previous account balance
     * @return Previous balance with DECIMAL(12,2) precision
     */
    public BigDecimal getPreviousBalance() {
        return previousBalance;
    }

    /**
     * Sets the previous account balance and recalculates balance difference
     * @param previousBalance Previous balance for comparison
     */
    public void setPreviousBalance(BigDecimal previousBalance) {
        this.previousBalance = previousBalance != null ? 
            previousBalance.setScale(2, RoundingMode.HALF_EVEN) : 
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        calculateBalanceDifference();
    }

    /**
     * Gets the calculated balance difference
     * @return Balance difference (current - previous)
     */
    public BigDecimal getBalanceDifference() {
        return balanceDifference;
    }

    /**
     * Sets the balance difference (normally calculated automatically)
     * @param balanceDifference Balance difference amount
     */
    public void setBalanceDifference(BigDecimal balanceDifference) {
        this.balanceDifference = balanceDifference != null ? 
            balanceDifference.setScale(2, RoundingMode.HALF_EVEN) : 
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * Gets the credit limit
     * @return Credit limit with DECIMAL(12,2) precision
     */
    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    /**
     * Sets the credit limit and recalculates available credit
     * @param creditLimit Maximum credit limit
     */
    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit != null ? 
            creditLimit.setScale(2, RoundingMode.HALF_EVEN) : 
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        calculateAvailableCredit();
    }

    /**
     * Gets the available credit
     * @return Available credit (credit limit - current balance)
     */
    public BigDecimal getAvailableCredit() {
        return availableCredit;
    }

    /**
     * Sets the available credit (normally calculated automatically)
     * @param availableCredit Available credit amount
     */
    public void setAvailableCredit(BigDecimal availableCredit) {
        this.availableCredit = availableCredit != null ? 
            availableCredit.setScale(2, RoundingMode.HALF_EVEN) : 
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * Calculates balance difference using COBOL-equivalent precision
     * balanceDifference = currentBalance - previousBalance
     */
    private void calculateBalanceDifference() {
        if (currentBalance != null && previousBalance != null) {
            this.balanceDifference = currentBalance.subtract(previousBalance, COBOL_MATH_CONTEXT)
                .setScale(2, RoundingMode.HALF_EVEN);
        } else {
            this.balanceDifference = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }
    }

    /**
     * Calculates available credit using COBOL-equivalent precision
     * availableCredit = creditLimit - currentBalance
     * Ensures available credit is never negative
     */
    private void calculateAvailableCredit() {
        if (creditLimit != null && currentBalance != null) {
            BigDecimal calculated = creditLimit.subtract(currentBalance, COBOL_MATH_CONTEXT);
            // Ensure available credit is not negative (over-limit scenarios)
            this.availableCredit = calculated.compareTo(BigDecimal.ZERO) >= 0 ? 
                calculated.setScale(2, RoundingMode.HALF_EVEN) : 
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        } else {
            this.availableCredit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }
    }

    /**
     * Validates if the account is within credit limit
     * @return true if current balance is within credit limit
     */
    public boolean isWithinCreditLimit() {
        if (currentBalance == null || creditLimit == null) {
            return false;
        }
        return currentBalance.compareTo(creditLimit) <= 0;
    }

    /**
     * Validates if there's sufficient available credit for a transaction
     * @param transactionAmount Amount to validate
     * @return true if sufficient credit available
     */
    public boolean hasSufficientCredit(BigDecimal transactionAmount) {
        if (availableCredit == null || transactionAmount == null) {
            return false;
        }
        return availableCredit.compareTo(transactionAmount) >= 0;
    }

    /**
     * Gets the credit utilization percentage
     * @return Credit utilization as BigDecimal (0.00 to 100.00)
     */
    public BigDecimal getCreditUtilization() {
        if (creditLimit == null || creditLimit.compareTo(BigDecimal.ZERO) == 0 || currentBalance == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }
        
        return currentBalance.divide(creditLimit, COBOL_MATH_CONTEXT)
            .multiply(BigDecimal.valueOf(100), COBOL_MATH_CONTEXT)
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * Returns string representation of the balance DTO
     * @return Formatted string with key balance information
     */
    @Override
    public String toString() {
        return String.format(
            "AccountBalanceDto{accountId='%s', currentBalance=%s, previousBalance=%s, " +
            "balanceDifference=%s, creditLimit=%s, availableCredit=%s}",
            accountId, currentBalance, previousBalance, balanceDifference, creditLimit, availableCredit
        );
    }

    /**
     * Checks equality based on account ID and balance values
     * @param obj Object to compare
     * @return true if objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AccountBalanceDto that = (AccountBalanceDto) obj;
        
        if (accountId != null ? !accountId.equals(that.accountId) : that.accountId != null) return false;
        if (currentBalance != null ? currentBalance.compareTo(that.currentBalance) != 0 : that.currentBalance != null) return false;
        if (previousBalance != null ? previousBalance.compareTo(that.previousBalance) != 0 : that.previousBalance != null) return false;
        if (creditLimit != null ? creditLimit.compareTo(that.creditLimit) != 0 : that.creditLimit != null) return false;
        
        return true;
    }

    /**
     * Generates hash code based on account ID and balance values
     * @return Hash code
     */
    @Override
    public int hashCode() {
        int result = accountId != null ? accountId.hashCode() : 0;
        result = 31 * result + (currentBalance != null ? currentBalance.hashCode() : 0);
        result = 31 * result + (previousBalance != null ? previousBalance.hashCode() : 0);
        result = 31 * result + (creditLimit != null ? creditLimit.hashCode() : 0);
        return result;
    }
}