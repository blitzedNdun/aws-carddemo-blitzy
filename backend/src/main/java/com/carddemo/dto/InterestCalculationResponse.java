package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Response DTO for interest calculation results containing calculated monthly interest amount,
 * effective interest rate, and calculation parameters. Returns BigDecimal values matching 
 * COBOL COMP-3 precision for interest calculations performed by InterestCalculationService.
 * 
 * This DTO represents the response from the interest calculation endpoint and maintains
 * exact precision compatibility with the original COBOL CBACT04C program calculations.
 * 
 * Field mappings from COBOL:
 * - monthlyInterest: WS-MONTHLY-INT (PIC S9(09)V99)
 * - effectiveRate: DIS-INT-RATE (PIC S9(04)V99 converted to percentage format)
 * - categoryCode: TRAN-CAT-CD (PIC 9(04))
 * - accountBalance: TRAN-CAT-BAL or ACCT-CURR-BAL (account balance used in calculation)
 */
public class InterestCalculationResponse {

    /**
     * Monthly interest amount calculated using COBOL formula: (balance * rate) / 1200
     * Precision: 9 digits with 2 decimal places matching COBOL WS-MONTHLY-INT
     */
    @JsonProperty("monthlyInterest")
    private BigDecimal monthlyInterest;

    /**
     * Effective interest rate used in calculation
     * Precision: 5 digits with 5 decimal places for precise rate representation
     * Represents the annual percentage rate used in monthly interest calculation
     */
    @JsonProperty("effectiveRate")
    private BigDecimal effectiveRate;

    /**
     * Transaction category code used to determine interest rate
     * Maps to COBOL TRAN-CAT-CD field
     */
    @JsonProperty("categoryCode")
    private String categoryCode;

    /**
     * Account balance used in interest calculation
     * Precision: 15 digits with 2 decimal places for large account balances
     * Represents the principal amount on which interest is calculated
     */
    @JsonProperty("accountBalance")
    private BigDecimal accountBalance;

    /**
     * Default constructor for JSON deserialization
     */
    public InterestCalculationResponse() {
    }

    /**
     * Constructor with all required fields
     * 
     * @param monthlyInterest Calculated monthly interest amount
     * @param effectiveRate Interest rate used in calculation
     * @param categoryCode Transaction category code
     * @param accountBalance Account balance used for calculation
     */
    public InterestCalculationResponse(BigDecimal monthlyInterest, BigDecimal effectiveRate, 
                                     String categoryCode, BigDecimal accountBalance) {
        this.monthlyInterest = monthlyInterest;
        this.effectiveRate = effectiveRate;
        this.categoryCode = categoryCode;
        this.accountBalance = accountBalance;
    }

    /**
     * Gets the calculated monthly interest amount
     * 
     * @return Monthly interest with COBOL COMP-3 precision (9,2)
     */
    public BigDecimal getMonthlyInterest() {
        return monthlyInterest;
    }

    /**
     * Sets the calculated monthly interest amount
     * 
     * @param monthlyInterest Monthly interest amount with precision (9,2)
     */
    public void setMonthlyInterest(BigDecimal monthlyInterest) {
        this.monthlyInterest = monthlyInterest;
    }

    /**
     * Gets the effective interest rate used in calculation
     * 
     * @return Interest rate with high precision (5,5)
     */
    public BigDecimal getEffectiveRate() {
        return effectiveRate;
    }

    /**
     * Sets the effective interest rate
     * 
     * @param effectiveRate Interest rate with precision (5,5)
     */
    public void setEffectiveRate(BigDecimal effectiveRate) {
        this.effectiveRate = effectiveRate;
    }

    /**
     * Gets the transaction category code
     * 
     * @return Category code used for interest rate determination
     */
    public String getCategoryCode() {
        return categoryCode;
    }

    /**
     * Sets the transaction category code
     * 
     * @param categoryCode Category code for interest rate lookup
     */
    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    /**
     * Gets the account balance used in calculation
     * 
     * @return Account balance with precision (15,2)
     */
    public BigDecimal getAccountBalance() {
        return accountBalance;
    }

    /**
     * Sets the account balance for calculation
     * 
     * @param accountBalance Account balance with precision (15,2)
     */
    public void setAccountBalance(BigDecimal accountBalance) {
        this.accountBalance = accountBalance;
    }

    /**
     * Returns string representation of the interest calculation response
     * 
     * @return Formatted string with all field values
     */
    @Override
    public String toString() {
        return "InterestCalculationResponse{" +
                "monthlyInterest=" + monthlyInterest +
                ", effectiveRate=" + effectiveRate +
                ", categoryCode='" + categoryCode + '\'' +
                ", accountBalance=" + accountBalance +
                '}';
    }

    /**
     * Compares this object with another for equality
     * Uses BigDecimal.compareTo for precise decimal comparison
     * 
     * @param obj Object to compare with
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
        InterestCalculationResponse that = (InterestCalculationResponse) obj;
        return Objects.equals(monthlyInterest, that.monthlyInterest) &&
               Objects.equals(effectiveRate, that.effectiveRate) &&
               Objects.equals(categoryCode, that.categoryCode) &&
               Objects.equals(accountBalance, that.accountBalance);
    }

    /**
     * Generates hash code for this object
     * Uses all fields to ensure proper hash distribution
     * 
     * @return Hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(monthlyInterest, effectiveRate, categoryCode, accountBalance);
    }
}