package com.carddemo.account;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;

/**
 * Account Balance Data Transfer Object (DTO) for providing current balance information 
 * with before/after comparison for real-time balance updates.
 * 
 * This DTO maintains exact decimal precision equivalent to COBOL COMP-3 arithmetic
 * using BigDecimal with DECIMAL(12,2) precision, ensuring no floating-point errors
 * in critical financial calculations.
 * 
 * Supports balance validation flags and credit limit information for comprehensive
 * account health checks and payment confirmation workflows.
 */
public class AccountBalanceDto {
    
    // Math context for exact COBOL COMP-3 precision equivalent
    private static final MathContext COBOL_MATH_CONTEXT = new MathContext(31, RoundingMode.HALF_UP);
    
    /**
     * Account identifier - 11 digit account number
     */
    @JsonProperty("accountId")
    private String accountId;
    
    /**
     * Current account balance with exact decimal precision
     * Maps to ACCT-CURR-BAL PIC S9(10)V99 COMP-3 from CVACT01Y.cpy
     */
    @JsonProperty("currentBalance")
    @Digits(integer = 10, fraction = 2, message = "Current balance must be in format 9999999999.99")
    private BigDecimal currentBalance;
    
    /**
     * Previous account balance for before/after comparison
     * Used for payment confirmation and audit trail
     */
    @JsonProperty("previousBalance")
    @Digits(integer = 10, fraction = 2, message = "Previous balance must be in format 9999999999.99")
    private BigDecimal previousBalance;
    
    /**
     * Balance difference calculation (current - previous)
     * Computed field showing balance change amount
     */
    @JsonProperty("balanceDifference")
    @Digits(integer = 10, fraction = 2, message = "Balance difference must be in format 9999999999.99")
    private BigDecimal balanceDifference;
    
    /**
     * Account credit limit
     * Maps to ACCT-CREDIT-LIMIT PIC S9(10)V99 COMP-3 from CVACT01Y.cpy
     */
    @JsonProperty("creditLimit")
    @Digits(integer = 10, fraction = 2, message = "Credit limit must be in format 9999999999.99")
    private BigDecimal creditLimit;
    
    /**
     * Available credit calculation (creditLimit - currentBalance)
     * Computed field showing remaining credit availability
     */
    @JsonProperty("availableCredit")
    @Digits(integer = 10, fraction = 2, message = "Available credit must be in format 9999999999.99")
    private BigDecimal availableCredit;
    
    /**
     * Cash credit limit for cash advances
     * Maps to ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99 COMP-3 from CVACT01Y.cpy
     */
    @JsonProperty("cashCreditLimit")
    @Digits(integer = 10, fraction = 2, message = "Cash credit limit must be in format 9999999999.99")
    private BigDecimal cashCreditLimit;
    
    /**
     * Current cycle credit total
     * Maps to ACCT-CURR-CYC-CREDIT PIC S9(10)V99 COMP-3 from CVACT01Y.cpy
     */
    @JsonProperty("currentCycleCredit")
    @Digits(integer = 10, fraction = 2, message = "Current cycle credit must be in format 9999999999.99")
    private BigDecimal currentCycleCredit;
    
    /**
     * Current cycle debit total
     * Maps to ACCT-CURR-CYC-DEBIT PIC S9(10)V99 COMP-3 from CVACT01Y.cpy
     */
    @JsonProperty("currentCycleDebit")
    @Digits(integer = 10, fraction = 2, message = "Current cycle debit must be in format 9999999999.99")
    private BigDecimal currentCycleDebit;
    
    /**
     * Account status indicator for balance validation
     * Y = Active, N = Inactive, S = Suspended
     */
    @JsonProperty("accountStatus")
    private String accountStatus;
    
    /**
     * Balance validation flag indicating account health
     * true = Balance within acceptable limits
     * false = Balance requires attention (overlimit, etc.)
     */
    @JsonProperty("balanceValidation")
    private boolean balanceValidation;
    
    /**
     * Over limit flag
     * true = Current balance exceeds credit limit
     * false = Balance within credit limit
     */
    @JsonProperty("overLimit")
    private boolean overLimit;
    
    /**
     * Default constructor
     */
    public AccountBalanceDto() {
        // Initialize BigDecimal fields with zero values using COBOL precision
        this.currentBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.previousBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.balanceDifference = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.creditLimit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.availableCredit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.cashCreditLimit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.currentCycleCredit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.currentCycleDebit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.accountStatus = "Y";
        this.balanceValidation = true;
        this.overLimit = false;
    }
    
    /**
     * Constructor with primary balance information
     * Automatically calculates derived fields
     */
    public AccountBalanceDto(String accountId, BigDecimal currentBalance, BigDecimal creditLimit) {
        this();
        this.accountId = accountId;
        this.currentBalance = currentBalance != null ? currentBalance.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.creditLimit = creditLimit != null ? creditLimit.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        calculateDerivedFields();
    }
    
    /**
     * Calculates derived fields based on current values
     * Uses COBOL COMP-3 equivalent precision for all calculations
     */
    private void calculateDerivedFields() {
        // Calculate balance difference
        if (this.currentBalance != null && this.previousBalance != null) {
            this.balanceDifference = this.currentBalance.subtract(this.previousBalance, COBOL_MATH_CONTEXT);
        }
        
        // Calculate available credit
        if (this.creditLimit != null && this.currentBalance != null) {
            this.availableCredit = this.creditLimit.subtract(this.currentBalance, COBOL_MATH_CONTEXT);
        }
        
        // Set over limit flag
        if (this.currentBalance != null && this.creditLimit != null) {
            this.overLimit = this.currentBalance.compareTo(this.creditLimit) > 0;
        }
        
        // Set balance validation flag
        this.balanceValidation = !this.overLimit && "Y".equals(this.accountStatus);
    }
    
    // Getter and Setter methods with validation and calculation logic
    
    public String getAccountId() {
        return accountId;
    }
    
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }
    
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance != null ? currentBalance.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        calculateDerivedFields();
    }
    
    public BigDecimal getPreviousBalance() {
        return previousBalance;
    }
    
    public void setPreviousBalance(BigDecimal previousBalance) {
        this.previousBalance = previousBalance != null ? previousBalance.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        calculateDerivedFields();
    }
    
    public BigDecimal getBalanceDifference() {
        return balanceDifference;
    }
    
    public void setBalanceDifference(BigDecimal balanceDifference) {
        this.balanceDifference = balanceDifference != null ? balanceDifference.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    
    public BigDecimal getCreditLimit() {
        return creditLimit;
    }
    
    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit != null ? creditLimit.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        calculateDerivedFields();
    }
    
    public BigDecimal getAvailableCredit() {
        return availableCredit;
    }
    
    public void setAvailableCredit(BigDecimal availableCredit) {
        this.availableCredit = availableCredit != null ? availableCredit.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    
    public BigDecimal getCashCreditLimit() {
        return cashCreditLimit;
    }
    
    public void setCashCreditLimit(BigDecimal cashCreditLimit) {
        this.cashCreditLimit = cashCreditLimit != null ? cashCreditLimit.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    
    public BigDecimal getCurrentCycleCredit() {
        return currentCycleCredit;
    }
    
    public void setCurrentCycleCredit(BigDecimal currentCycleCredit) {
        this.currentCycleCredit = currentCycleCredit != null ? currentCycleCredit.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    
    public BigDecimal getCurrentCycleDebit() {
        return currentCycleDebit;
    }
    
    public void setCurrentCycleDebit(BigDecimal currentCycleDebit) {
        this.currentCycleDebit = currentCycleDebit != null ? currentCycleDebit.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    
    public String getAccountStatus() {
        return accountStatus;
    }
    
    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
        calculateDerivedFields();
    }
    
    public boolean isBalanceValidation() {
        return balanceValidation;
    }
    
    public void setBalanceValidation(boolean balanceValidation) {
        this.balanceValidation = balanceValidation;
    }
    
    public boolean isOverLimit() {
        return overLimit;
    }
    
    public void setOverLimit(boolean overLimit) {
        this.overLimit = overLimit;
    }
    
    /**
     * Validates the account balance against business rules
     * Performs comprehensive balance validation equivalent to COBOL business logic
     * 
     * @return true if all validations pass, false otherwise
     */
    public boolean validateBalance() {
        // Check account status
        if (!"Y".equals(accountStatus)) {
            return false;
        }
        
        // Check for null values
        if (currentBalance == null || creditLimit == null) {
            return false;
        }
        
        // Check for negative balance beyond credit limit
        BigDecimal negativeLimit = creditLimit.negate();
        if (currentBalance.compareTo(negativeLimit) < 0) {
            return false;
        }
        
        // Update validation flags
        this.overLimit = currentBalance.compareTo(creditLimit) > 0;
        this.balanceValidation = !this.overLimit;
        
        return this.balanceValidation;
    }
    
    /**
     * Formats balance for display with proper decimal precision
     * Maintains COBOL COMP-3 display format
     * 
     * @param amount BigDecimal amount to format
     * @return Formatted string with 2 decimal places
     */
    public static String formatBalance(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
    
    /**
     * Performs BigDecimal arithmetic with COBOL COMP-3 precision
     * Ensures exact financial calculations without floating-point errors
     * 
     * @param amount1 First amount
     * @param amount2 Second amount
     * @return Sum with exact precision
     */
    public static BigDecimal addWithPrecision(BigDecimal amount1, BigDecimal amount2) {
        if (amount1 == null) amount1 = BigDecimal.ZERO;
        if (amount2 == null) amount2 = BigDecimal.ZERO;
        
        return amount1.add(amount2, COBOL_MATH_CONTEXT).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Performs BigDecimal subtraction with COBOL COMP-3 precision
     * Ensures exact financial calculations without floating-point errors
     * 
     * @param amount1 First amount
     * @param amount2 Second amount
     * @return Difference with exact precision
     */
    public static BigDecimal subtractWithPrecision(BigDecimal amount1, BigDecimal amount2) {
        if (amount1 == null) amount1 = BigDecimal.ZERO;
        if (amount2 == null) amount2 = BigDecimal.ZERO;
        
        return amount1.subtract(amount2, COBOL_MATH_CONTEXT).setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    public String toString() {
        return String.format("AccountBalanceDto{" +
                "accountId='%s', " +
                "currentBalance=%s, " +
                "previousBalance=%s, " +
                "balanceDifference=%s, " +
                "creditLimit=%s, " +
                "availableCredit=%s, " +
                "accountStatus='%s', " +
                "balanceValidation=%s, " +
                "overLimit=%s" +
                "}",
                accountId,
                formatBalance(currentBalance),
                formatBalance(previousBalance),
                formatBalance(balanceDifference),
                formatBalance(creditLimit),
                formatBalance(availableCredit),
                accountStatus,
                balanceValidation,
                overLimit
        );
    }
}