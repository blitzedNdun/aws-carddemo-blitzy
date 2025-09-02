package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Request DTO for interest calculation containing account balance, transaction type code,
 * and category code for interest computation. Maps to COBOL interest calculation parameters
 * from CBACT04C.cbl with BigDecimal precision for COMP-3 values.
 * 
 * Supports the COBOL formula: WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
 */
public class InterestCalculationRequest {

    /**
     * Account balance for interest calculation (maps to TRAN-CAT-BAL in COBOL)
     * Uses BigDecimal to maintain COBOL COMP-3 precision for financial calculations
     */
    @JsonProperty("balance")
    @NotNull(message = "Balance is required")
    @DecimalMin(value = "0.00", message = "Balance must be non-negative")
    private BigDecimal balance;

    /**
     * Transaction type code (maps to DIS-TRAN-TYPE-CD PIC X(02) in COBOL)
     * Must be exactly 2 characters as defined in COBOL copybook
     */
    @JsonProperty("transactionTypeCode")
    @NotNull(message = "Transaction type code is required")
    @Size(min = 2, max = 2, message = "Transaction type code must be exactly 2 characters")
    @Pattern(regexp = "^[A-Z0-9]{2}$", message = "Transaction type code must contain only alphanumeric characters")
    private String transactionTypeCode;

    /**
     * Category code (maps to DIS-TRAN-CAT-CD PIC 9(04) in COBOL)
     * Must be exactly 4 digits as defined in COBOL copybook
     */
    @JsonProperty("categoryCode")
    @NotNull(message = "Category code is required")
    @Size(min = 4, max = 4, message = "Category code must be exactly 4 digits")
    @Pattern(regexp = "^[0-9]{4}$", message = "Category code must contain only numeric digits")
    private String categoryCode;

    /**
     * Account group ID (maps to DIS-ACCT-GROUP-ID PIC X(10) in COBOL)
     * Must be exactly 10 characters as defined in COBOL copybook
     */
    @JsonProperty("accountGroupId")
    @NotNull(message = "Account group ID is required")
    @Size(min = 10, max = 10, message = "Account group ID must be exactly 10 characters")
    private String accountGroupId;

    /**
     * Default constructor required for JSON deserialization
     */
    public InterestCalculationRequest() {
    }

    /**
     * Constructor with all required fields for interest calculation
     * 
     * @param balance The account balance for interest calculation
     * @param transactionTypeCode The 2-character transaction type code
     * @param categoryCode The 4-digit category code
     * @param accountGroupId The 10-character account group ID
     */
    public InterestCalculationRequest(BigDecimal balance, String transactionTypeCode, 
                                    String categoryCode, String accountGroupId) {
        this.balance = balance;
        this.transactionTypeCode = transactionTypeCode;
        this.categoryCode = categoryCode;
        this.accountGroupId = accountGroupId;
    }

    /**
     * Gets the account balance for interest calculation
     * 
     * @return The balance as BigDecimal maintaining COBOL COMP-3 precision
     */
    public BigDecimal getBalance() {
        return balance;
    }

    /**
     * Sets the account balance for interest calculation
     * 
     * @param balance The balance as BigDecimal maintaining COBOL COMP-3 precision
     */
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    /**
     * Gets the transaction type code
     * 
     * @return The 2-character transaction type code
     */
    public String getTransactionTypeCode() {
        return transactionTypeCode;
    }

    /**
     * Sets the transaction type code
     * 
     * @param transactionTypeCode The 2-character transaction type code
     */
    public void setTransactionTypeCode(String transactionTypeCode) {
        this.transactionTypeCode = transactionTypeCode;
    }

    /**
     * Gets the category code
     * 
     * @return The 4-digit category code
     */
    public String getCategoryCode() {
        return categoryCode;
    }

    /**
     * Sets the category code
     * 
     * @param categoryCode The 4-digit category code
     */
    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    /**
     * Gets the account group ID
     * 
     * @return The 10-character account group ID
     */
    public String getAccountGroupId() {
        return accountGroupId;
    }

    /**
     * Sets the account group ID
     * 
     * @param accountGroupId The 10-character account group ID
     */
    public void setAccountGroupId(String accountGroupId) {
        this.accountGroupId = accountGroupId;
    }

    /**
     * Returns string representation of the interest calculation request
     * 
     * @return String representation including all fields for debugging
     */
    @Override
    public String toString() {
        return "InterestCalculationRequest{" +
                "balance=" + balance +
                ", transactionTypeCode='" + transactionTypeCode + '\'' +
                ", categoryCode='" + categoryCode + '\'' +
                ", accountGroupId='" + accountGroupId + '\'' +
                '}';
    }

    /**
     * Checks equality based on all fields
     * 
     * @param obj The object to compare with
     * @return true if all fields are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        InterestCalculationRequest that = (InterestCalculationRequest) obj;
        return Objects.equals(balance, that.balance) &&
               Objects.equals(transactionTypeCode, that.transactionTypeCode) &&
               Objects.equals(categoryCode, that.categoryCode) &&
               Objects.equals(accountGroupId, that.accountGroupId);
    }

    /**
     * Generates hash code based on all fields
     * 
     * @return Hash code for the object
     */
    @Override
    public int hashCode() {
        return Objects.hash(balance, transactionTypeCode, categoryCode, accountGroupId);
    }
}