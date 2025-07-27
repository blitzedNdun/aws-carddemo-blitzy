package com.carddemo.account;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;

/**
 * Account Balance Data Transfer Object
 * 
 * Provides comprehensive account balance information with before/after comparison
 * capabilities for real-time balance updates and payment confirmation workflows.
 * Maintains exact decimal precision equivalent to COBOL COMP-3 arithmetic
 * operations ensuring financial data integrity.
 * 
 * This DTO supports:
 * - Current and previous balance comparison for audit trails
 * - Credit limit information with available credit calculations
 * - Balance validation flags for account health monitoring
 * - JSON serialization with proper decimal formatting for frontend integration
 * 
 * Data precision follows COBOL PIC S9(10)V99 format mapped to DECIMAL(12,2)
 * in PostgreSQL, ensuring identical financial calculation results.
 */
public class AccountBalanceDto {

    /**
     * MathContext for all BigDecimal operations ensuring COBOL COMP-3 arithmetic equivalence
     * Uses DECIMAL128 precision with HALF_EVEN rounding (banker's rounding)
     */
    private static final MathContext COBOL_MATH_CONTEXT = new MathContext(31, RoundingMode.HALF_EVEN);

    /**
     * Account identifier - maps to ACCT-ID PIC 9(11) from COBOL copybook
     * 11-digit numeric account identifier with leading zeros preserved
     */
    @JsonProperty("account_id")
    private String accountId;

    /**
     * Current account balance - maps to ACCT-CURR-BAL PIC S9(10)V99 COMP-3
     * Precision: 12 total digits, 2 decimal places
     * Range: -9999999999.99 to 9999999999.99
     */
    @JsonProperty("current_balance")
    @Digits(integer = 10, fraction = 2, message = "Current balance must have maximum 10 integer digits and 2 decimal places")
    private BigDecimal currentBalance;

    /**
     * Previous account balance for before/after comparison
     * Used in payment confirmation and audit trail generation
     * Same precision as current balance ensuring calculation accuracy
     */
    @JsonProperty("previous_balance")
    @Digits(integer = 10, fraction = 2, message = "Previous balance must have maximum 10 integer digits and 2 decimal places")
    private BigDecimal previousBalance;

    /**
     * Balance difference calculation (current - previous)
     * Computed field showing net change for transaction confirmation
     * Positive values indicate credits, negative values indicate debits
     */
    @JsonProperty("balance_difference")
    @Digits(integer = 10, fraction = 2, message = "Balance difference must have maximum 10 integer digits and 2 decimal places")
    private BigDecimal balanceDifference;

    /**
     * Credit limit - maps to ACCT-CREDIT-LIMIT PIC S9(10)V99 COMP-3
     * Maximum credit amount authorized for this account
     * Zero or positive values only, negative indicates suspended credit
     */
    @JsonProperty("credit_limit")
    @Digits(integer = 10, fraction = 2, message = "Credit limit must have maximum 10 integer digits and 2 decimal places")
    private BigDecimal creditLimit;

    /**
     * Available credit calculation (credit_limit + current_balance for negative balances)
     * Computed field showing remaining credit capacity
     * For positive balances: credit_limit - current_balance
     * For negative balances: credit_limit + absolute(current_balance)
     */
    @JsonProperty("available_credit")
    @Digits(integer = 10, fraction = 2, message = "Available credit must have maximum 10 integer digits and 2 decimal places")
    private BigDecimal availableCredit;

    /**
     * Cash credit limit - maps to ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99 COMP-3
     * Maximum cash advance amount authorized, typically lower than credit limit
     */
    @JsonProperty("cash_credit_limit")
    @Digits(integer = 10, fraction = 2, message = "Cash credit limit must have maximum 10 integer digits and 2 decimal places")
    private BigDecimal cashCreditLimit;

    /**
     * Balance validation status indicating account health
     * Values: NORMAL, OVERLIMIT, SUSPENDED, CLOSED
     */
    @JsonProperty("balance_status")
    private String balanceStatus;

    /**
     * Account active status flag - maps to ACCT-ACTIVE-STATUS from COBOL
     * 'Y' = Active, 'N' = Inactive, 'S' = Suspended
     */
    @JsonProperty("account_status")
    private String accountStatus;

    /**
     * Over-limit indicator flag for risk management
     * True when current balance exceeds credit limit
     */
    @JsonProperty("over_limit_flag")
    private Boolean overLimitFlag;

    /**
     * Default constructor initializing BigDecimal fields with zero precision
     */
    public AccountBalanceDto() {
        this.currentBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        this.previousBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        this.balanceDifference = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        this.creditLimit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        this.availableCredit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        this.cashCreditLimit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        this.balanceStatus = "NORMAL";
        this.accountStatus = "Y";
        this.overLimitFlag = false;
    }

    /**
     * Constructor with essential balance fields
     * Automatically calculates derived fields and performs validation
     * 
     * @param accountId Account identifier
     * @param currentBalance Current account balance
     * @param creditLimit Maximum credit limit
     */
    public AccountBalanceDto(String accountId, BigDecimal currentBalance, BigDecimal creditLimit) {
        this();
        this.accountId = accountId;
        this.currentBalance = ensurePrecision(currentBalance);
        this.creditLimit = ensurePrecision(creditLimit);
        this.previousBalance = ensurePrecision(currentBalance); // Initialize to current for new instances
        calculateDerivedFields();
    }

    /**
     * Constructor for payment confirmation scenarios with before/after comparison
     * 
     * @param accountId Account identifier
     * @param currentBalance Current balance after transaction
     * @param previousBalance Balance before transaction
     * @param creditLimit Account credit limit
     */
    public AccountBalanceDto(String accountId, BigDecimal currentBalance, 
                            BigDecimal previousBalance, BigDecimal creditLimit) {
        this();
        this.accountId = accountId;
        this.currentBalance = ensurePrecision(currentBalance);
        this.previousBalance = ensurePrecision(previousBalance);
        this.creditLimit = ensurePrecision(creditLimit);
        calculateDerivedFields();
    }

    /**
     * Ensures BigDecimal precision matches COBOL COMP-3 requirements
     * Sets scale to 2 decimal places with HALF_EVEN rounding
     * 
     * @param value BigDecimal value to process
     * @return BigDecimal with proper scale and precision
     */
    private BigDecimal ensurePrecision(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * Calculates all derived fields based on base balance and credit information
     * Implements business logic for available credit and validation status
     */
    private void calculateDerivedFields() {
        // Calculate balance difference using COBOL-equivalent arithmetic
        this.balanceDifference = this.currentBalance.subtract(this.previousBalance, COBOL_MATH_CONTEXT);
        
        // Calculate available credit based on current balance
        if (this.currentBalance.compareTo(BigDecimal.ZERO) >= 0) {
            // Positive balance: available credit = credit limit - current balance
            this.availableCredit = this.creditLimit.subtract(this.currentBalance, COBOL_MATH_CONTEXT);
        } else {
            // Negative balance: available credit = credit limit + absolute(current balance)
            this.availableCredit = this.creditLimit.add(this.currentBalance.abs(), COBOL_MATH_CONTEXT);
        }
        
        // Ensure available credit is not negative
        if (this.availableCredit.compareTo(BigDecimal.ZERO) < 0) {
            this.availableCredit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }
        
        // Determine over-limit status
        this.overLimitFlag = this.currentBalance.compareTo(this.creditLimit) > 0;
        
        // Set balance status based on account conditions
        if (this.overLimitFlag) {
            this.balanceStatus = "OVERLIMIT";
        } else if ("N".equals(this.accountStatus)) {
            this.balanceStatus = "CLOSED";
        } else if ("S".equals(this.accountStatus)) {
            this.balanceStatus = "SUSPENDED";
        } else {
            this.balanceStatus = "NORMAL";
        }
    }

    // Getter and Setter methods implementing required export interface

    /**
     * Gets the account identifier
     * 
     * @return Account ID string
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account identifier
     * 
     * @param accountId Account ID to set
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the current account balance with exact decimal precision
     * 
     * @return Current balance as BigDecimal
     */
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    /**
     * Sets the current account balance and recalculates derived fields
     * Ensures proper scale and precision for financial calculations
     * 
     * @param currentBalance New current balance
     */
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = ensurePrecision(currentBalance);
        calculateDerivedFields();
    }

    /**
     * Gets the previous account balance for comparison purposes
     * 
     * @return Previous balance as BigDecimal
     */
    public BigDecimal getPreviousBalance() {
        return previousBalance;
    }

    /**
     * Sets the previous account balance and recalculates balance difference
     * Used in payment confirmation and audit workflows
     * 
     * @param previousBalance Previous balance value
     */
    public void setPreviousBalance(BigDecimal previousBalance) {
        this.previousBalance = ensurePrecision(previousBalance);
        calculateDerivedFields();
    }

    /**
     * Gets the calculated balance difference (current - previous)
     * 
     * @return Balance difference as BigDecimal
     */
    public BigDecimal getBalanceDifference() {
        return balanceDifference;
    }

    /**
     * Sets the balance difference directly (typically calculated automatically)
     * 
     * @param balanceDifference Balance difference to set
     */
    public void setBalanceDifference(BigDecimal balanceDifference) {
        this.balanceDifference = ensurePrecision(balanceDifference);
    }

    /**
     * Gets the account credit limit
     * 
     * @return Credit limit as BigDecimal
     */
    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    /**
     * Sets the account credit limit and recalculates available credit
     * 
     * @param creditLimit New credit limit value
     */
    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = ensurePrecision(creditLimit);
        calculateDerivedFields();
    }

    /**
     * Gets the calculated available credit amount
     * 
     * @return Available credit as BigDecimal
     */
    public BigDecimal getAvailableCredit() {
        return availableCredit;
    }

    /**
     * Sets the available credit directly (typically calculated automatically)
     * 
     * @param availableCredit Available credit amount to set
     */
    public void setAvailableCredit(BigDecimal availableCredit) {
        this.availableCredit = ensurePrecision(availableCredit);
    }

    /**
     * Gets the cash credit limit for cash advance operations
     * 
     * @return Cash credit limit as BigDecimal
     */
    public BigDecimal getCashCreditLimit() {
        return cashCreditLimit;
    }

    /**
     * Sets the cash credit limit
     * 
     * @param cashCreditLimit Cash credit limit to set
     */
    public void setCashCreditLimit(BigDecimal cashCreditLimit) {
        this.cashCreditLimit = ensurePrecision(cashCreditLimit);
    }

    /**
     * Gets the balance validation status
     * 
     * @return Balance status string (NORMAL, OVERLIMIT, SUSPENDED, CLOSED)
     */
    public String getBalanceStatus() {
        return balanceStatus;
    }

    /**
     * Sets the balance validation status
     * 
     * @param balanceStatus Status to set
     */
    public void setBalanceStatus(String balanceStatus) {
        this.balanceStatus = balanceStatus;
    }

    /**
     * Gets the account active status
     * 
     * @return Account status ('Y', 'N', 'S')
     */
    public String getAccountStatus() {
        return accountStatus;
    }

    /**
     * Sets the account active status and recalculates derived fields
     * 
     * @param accountStatus Account status to set
     */
    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
        calculateDerivedFields();
    }

    /**
     * Gets the over-limit indicator flag
     * 
     * @return True if balance exceeds credit limit
     */
    public Boolean getOverLimitFlag() {
        return overLimitFlag;
    }

    /**
     * Sets the over-limit indicator flag
     * 
     * @param overLimitFlag Over-limit flag to set
     */
    public void setOverLimitFlag(Boolean overLimitFlag) {
        this.overLimitFlag = overLimitFlag;
    }

    /**
     * Utility method to compare two balances with proper BigDecimal precision
     * Uses COBOL-equivalent comparison logic
     * 
     * @param balance1 First balance to compare
     * @param balance2 Second balance to compare
     * @return -1 if balance1 < balance2, 0 if equal, 1 if balance1 > balance2
     */
    public static int compareBalances(BigDecimal balance1, BigDecimal balance2) {
        if (balance1 == null || balance2 == null) {
            throw new IllegalArgumentException("Balance values cannot be null for comparison");
        }
        return balance1.compareTo(balance2);
    }

    /**
     * Utility method to add two monetary amounts with proper precision
     * Maintains COBOL COMP-3 arithmetic behavior
     * 
     * @param amount1 First amount
     * @param amount2 Second amount
     * @return Sum with proper scale and precision
     */
    public static BigDecimal addAmounts(BigDecimal amount1, BigDecimal amount2) {
        if (amount1 == null) amount1 = BigDecimal.ZERO;
        if (amount2 == null) amount2 = BigDecimal.ZERO;
        
        return amount1.add(amount2, COBOL_MATH_CONTEXT).setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * Utility method to subtract two monetary amounts with proper precision
     * Maintains COBOL COMP-3 arithmetic behavior
     * 
     * @param amount1 Amount to subtract from
     * @param amount2 Amount to subtract
     * @return Difference with proper scale and precision
     */
    public static BigDecimal subtractAmounts(BigDecimal amount1, BigDecimal amount2) {
        if (amount1 == null) amount1 = BigDecimal.ZERO;
        if (amount2 == null) amount2 = BigDecimal.ZERO;
        
        return amount1.subtract(amount2, COBOL_MATH_CONTEXT).setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * Validates that a BigDecimal value conforms to COBOL COMP-3 precision requirements
     * 
     * @param value Value to validate
     * @param fieldName Name of field for error messages
     * @return True if valid precision
     * @throws IllegalArgumentException if precision is invalid
     */
    public static boolean validatePrecision(BigDecimal value, String fieldName) {
        if (value == null) {
            return true; // Null values handled by Jakarta validation
        }
        
        int scale = value.scale();
        int precision = value.precision();
        
        if (scale > 2) {
            throw new IllegalArgumentException(
                String.format("%s has invalid scale %d, maximum 2 decimal places allowed", fieldName, scale));
        }
        
        if (precision - scale > 10) {
            throw new IllegalArgumentException(
                String.format("%s has invalid precision %d, maximum 10 integer digits allowed", fieldName, precision - scale));
        }
        
        return true;
    }

    /**
     * String representation for debugging and logging
     * Excludes sensitive information but shows key balance data
     * 
     * @return String representation of balance information
     */
    @Override
    public String toString() {
        return String.format(
            "AccountBalanceDto{accountId='%s', currentBalance=%s, creditLimit=%s, availableCredit=%s, status='%s'}",
            accountId, currentBalance, creditLimit, availableCredit, balanceStatus
        );
    }

    /**
     * Equals implementation for balance comparison in tests and business logic
     * Compares all balance-related fields with BigDecimal precision
     * 
     * @param obj Object to compare
     * @return True if objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AccountBalanceDto that = (AccountBalanceDto) obj;
        
        return java.util.Objects.equals(accountId, that.accountId) &&
               compareBigDecimals(currentBalance, that.currentBalance) &&
               compareBigDecimals(previousBalance, that.previousBalance) &&
               compareBigDecimals(creditLimit, that.creditLimit) &&
               java.util.Objects.equals(balanceStatus, that.balanceStatus);
    }

    /**
     * Hash code implementation consistent with equals
     * 
     * @return Hash code for this object
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(accountId, currentBalance, previousBalance, creditLimit, balanceStatus);
    }

    /**
     * Helper method for BigDecimal comparison in equals method
     * 
     * @param bd1 First BigDecimal
     * @param bd2 Second BigDecimal
     * @return True if values are equal
     */
    private boolean compareBigDecimals(BigDecimal bd1, BigDecimal bd2) {
        if (bd1 == null && bd2 == null) return true;
        if (bd1 == null || bd2 == null) return false;
        return bd1.compareTo(bd2) == 0;
    }
}