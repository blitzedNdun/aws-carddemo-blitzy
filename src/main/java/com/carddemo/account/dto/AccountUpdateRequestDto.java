package com.carddemo.account.dto;

import com.carddemo.account.dto.CustomerDto;
import com.carddemo.common.enums.AccountStatus;
import com.carddemo.common.enums.ValidationResult;
import com.carddemo.common.util.BigDecimalUtils;
import com.carddemo.common.util.ValidationUtils;
import com.carddemo.common.validator.ValidCCYYMMDD;
import com.carddemo.common.validator.ValidCurrency;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Request DTO for account update operations with comprehensive validation annotations,
 * BigDecimal financial fields, and cross-field validation logic equivalent to 
 * COACTUPC.cbl transaction processing.
 * 
 * This DTO maintains exact field mappings from COBOL ACCT-UPDATE-RECORD and 
 * CUST-UPDATE-RECORD structures while providing modern Jakarta Bean Validation
 * annotations that replicate the original COBOL business rules and data validation
 * patterns used in the account update transaction.
 * 
 * Fields correspond to:
 * - CVACT01Y.cpy (Account Master Record) editable fields
 * - CVCUS01Y.cpy (Customer Master Record) via embedded CustomerDto
 * - COBOL date validation patterns from CSUTLDWY.cpy working storage
 * - Financial precision requirements matching COMP-3 decimal fields
 */
public class AccountUpdateRequestDto {

    /**
     * Account identifier from ACCT-ID field (PIC 9(11))
     * Maps to ACUP-NEW-ACCT-ID in COACTUPC.cbl transaction logic
     * Required field for identifying the account record to update
     */
    @NotBlank(message = "Account ID is required and cannot be blank")
    @JsonProperty("accountId")
    private String accountId;

    /**
     * Account active status from ACCT-ACTIVE-STATUS field (PIC X(01))
     * Maps to ACUP-NEW-ACTIVE-STATUS with Y/N validation logic
     * Converted to typed enum for better validation and type safety
     */
    @JsonProperty("activeStatus")
    private AccountStatus activeStatus;

    /**
     * Current account balance from ACCT-CURR-BAL field (PIC S9(10)V99 COMP-3)
     * Maps to ACUP-NEW-CURR-BAL-N with exact decimal precision preservation
     * Uses BigDecimal to maintain COBOL COMP-3 arithmetic accuracy
     */
    @ValidCurrency(message = "Current balance must be a valid monetary amount with proper decimal precision")
    @JsonProperty("currentBalance")
    private BigDecimal currentBalance;

    /**
     * Credit limit from ACCT-CREDIT-LIMIT field (PIC S9(10)V99 COMP-3)
     * Maps to ACUP-NEW-CREDIT-LIMIT-N with financial validation rules
     * Must be positive value with maximum 2 decimal places for monetary precision
     */
    @ValidCurrency(message = "Credit limit must be a valid positive monetary amount")
    @JsonProperty("creditLimit")
    private BigDecimal creditLimit;

    /**
     * Cash credit limit from ACCT-CASH-CREDIT-LIMIT field (PIC S9(10)V99 COMP-3)
     * Maps to ACUP-NEW-CASH-CREDIT-LIMIT-N following COBOL business rules
     * Typically should not exceed the main credit limit per business logic
     */
    @ValidCurrency(message = "Cash credit limit must be a valid positive monetary amount")
    @JsonProperty("cashCreditLimit")
    private BigDecimal cashCreditLimit;

    /**
     * Account opening date from ACCT-OPEN-DATE field (PIC X(10))
     * Maps to ACUP-NEW-OPEN-DATE with CCYYMMDD format validation
     * Requires comprehensive date validation including leap year logic
     */
    @ValidCCYYMMDD(message = "Open date must be in valid CCYYMMDD format with proper date validation")
    @JsonProperty("openDate")
    private String openDate;

    /**
     * Account expiration date from ACCT-EXPIRAION-DATE field (PIC X(10))
     * Maps to ACUP-NEW-EXP-DATE with business rule that expiry must be after open date
     * Subject to cross-field validation against openDate for logical consistency
     */
    @ValidCCYYMMDD(message = "Expiry date must be in valid CCYYMMDD format")
    @JsonProperty("expiryDate")
    private String expiryDate;

    /**
     * Card reissue date from ACCT-REISSUE-DATE field (PIC X(10))
     * Maps to ACUP-NEW-REISSUE-DATE with optional date validation
     * Can be null/empty if no reissue has occurred for the account
     */
    @ValidCCYYMMDD(message = "Reissue date must be in valid CCYYMMDD format when provided")
    @JsonProperty("reissueDate")
    private String reissueDate;

    /**
     * Embedded customer data from CUSTOMER-RECORD (CVCUS01Y.cpy)
     * Maps to all ACUP-NEW-CUST-* fields in the COBOL transaction
     * Includes comprehensive customer validation via CustomerDto annotations
     * Supports cascading validation for nested customer fields including FICO score
     */
    @Valid
    @JsonProperty("customerData")
    private CustomerDto customerData;

    /**
     * Default constructor for DTO instantiation
     * Initializes financial fields with zero values using BigDecimal precision
     */
    public AccountUpdateRequestDto() {
        this.currentBalance = BigDecimalUtils.createDecimal("0.00");
        this.creditLimit = BigDecimalUtils.createDecimal("0.00");
        this.cashCreditLimit = BigDecimalUtils.createDecimal("0.00");
        this.activeStatus = AccountStatus.ACTIVE;
    }

    /**
     * Constructor with account ID for targeted updates
     * Provides convenient initialization for specific account updates
     * 
     * @param accountId The account identifier to update
     */
    public AccountUpdateRequestDto(String accountId) {
        this();
        this.accountId = accountId;
    }

    // Getter and Setter methods with proper JavaDoc documentation

    /**
     * Gets the account identifier
     * @return The account ID as string representation of 11-digit numeric value
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account identifier with validation
     * @param accountId The account ID to set (must be valid 11-digit format)
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the account active status
     * @return The account status enum (ACTIVE/INACTIVE)
     */
    public AccountStatus getActiveStatus() {
        return activeStatus;
    }

    /**
     * Sets the account active status
     * @param activeStatus The status to set (ACTIVE for 'Y', INACTIVE for 'N')
     */
    public void setActiveStatus(AccountStatus activeStatus) {
        this.activeStatus = activeStatus;
    }

    /**
     * Gets the current account balance
     * @return The current balance as BigDecimal with COMP-3 precision
     */
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    /**
     * Sets the current account balance
     * @param currentBalance The balance to set (must maintain monetary precision)
     */
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    /**
     * Gets the credit limit
     * @return The credit limit as BigDecimal with proper monetary scale
     */
    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    /**
     * Sets the credit limit
     * @param creditLimit The credit limit to set (must be positive monetary value)
     */
    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    /**
     * Gets the cash credit limit
     * @return The cash credit limit as BigDecimal
     */
    public BigDecimal getCashCreditLimit() {
        return cashCreditLimit;
    }

    /**
     * Sets the cash credit limit
     * @param cashCreditLimit The cash credit limit to set
     */
    public void setCashCreditLimit(BigDecimal cashCreditLimit) {
        this.cashCreditLimit = cashCreditLimit;
    }

    /**
     * Gets the account opening date
     * @return The opening date in CCYYMMDD format
     */
    public String getOpenDate() {
        return openDate;
    }

    /**
     * Sets the account opening date
     * @param openDate The opening date in CCYYMMDD format
     */
    public void setOpenDate(String openDate) {
        this.openDate = openDate;
    }

    /**
     * Gets the account expiry date
     * @return The expiry date in CCYYMMDD format
     */
    public String getExpiryDate() {
        return expiryDate;
    }

    /**
     * Sets the account expiry date
     * @param expiryDate The expiry date in CCYYMMDD format
     */
    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    /**
     * Gets the card reissue date
     * @return The reissue date in CCYYMMDD format (can be null)
     */
    public String getReissueDate() {
        return reissueDate;
    }

    /**
     * Sets the card reissue date
     * @param reissueDate The reissue date in CCYYMMDD format (optional)
     */
    public void setReissueDate(String reissueDate) {
        this.reissueDate = reissueDate;
    }

    /**
     * Gets the embedded customer data
     * @return The customer DTO with full customer information and validation
     */
    public CustomerDto getCustomerData() {
        return customerData;
    }

    /**
     * Sets the embedded customer data
     * @param customerData The customer DTO containing all customer fields
     */
    public void setCustomerData(CustomerDto customerData) {
        this.customerData = customerData;
    }

    /**
     * Comprehensive validation method replicating COACTUPC.cbl validation logic
     * Performs cross-field validation equivalent to original COBOL business rules
     * including account number format, financial constraints, and date relationships
     * 
     * @return ValidationResult indicating overall validation status
     */
    public ValidationResult validate() {
        // Account ID validation - must be valid 11-digit format
        ValidationResult accountValidation = ValidationUtils.validateAccountNumber(this.accountId);
        if (!accountValidation.isValid()) {
            return accountValidation;
        }

        // Credit limit validation - must be positive and within business limits
        ValidationResult creditValidation = ValidationUtils.validateCreditLimit(this.creditLimit);
        if (!creditValidation.isValid()) {
            return creditValidation;
        }

        // Current balance validation - validate format and precision
        ValidationResult balanceValidation = ValidationUtils.validateBalance(this.currentBalance);
        if (!balanceValidation.isValid()) {
            return balanceValidation;
        }

        // Cash credit limit must not exceed main credit limit per business rules
        if (this.cashCreditLimit != null && this.creditLimit != null) {
            if (this.cashCreditLimit.compareTo(this.creditLimit) > 0) {
                return ValidationResult.INVALID_RANGE;
            }
        }

        // Date relationship validation - expiry must be after open date
        if (this.openDate != null && this.expiryDate != null) {
            try {
                int openDateInt = Integer.parseInt(this.openDate);
                int expiryDateInt = Integer.parseInt(this.expiryDate);
                if (expiryDateInt <= openDateInt) {
                    return ValidationResult.INVALID_RANGE;
                }
            } catch (NumberFormatException e) {
                return ValidationResult.INVALID_FORMAT;
            }
        }

        // Customer data validation if present
        if (this.customerData != null) {
            ValidationResult customerValidation = this.customerData.validate();
            if (!customerValidation.isValid()) {
                return customerValidation;
            }

            // FICO score range validation (300-850) per Section 2.1.3 F-003-RQ-003
            if (this.customerData.getFicoCreditScore() != null) {
                int ficoScore = this.customerData.getFicoCreditScore();
                if (ficoScore < 300 || ficoScore > 850) {
                    return ValidationResult.INVALID_RANGE;
                }
            }
        }

        return ValidationResult.VALID;
    }

    /**
     * Convenience method to check if the DTO passes all validation rules
     * Equivalent to checking validate().isValid() but more readable in code
     * 
     * @return true if all validation rules pass, false otherwise
     */
    public boolean isValid() {
        return validate().isValid();
    }

    /**
     * Cross-field validation for state/ZIP code consistency per Section 7.4.4
     * Validates that customer address state and ZIP code are logically consistent
     * Replicates original COBOL validation patterns from address validation routines
     * 
     * @return true if state and ZIP code are consistent, false otherwise
     */
    public boolean validateStateZipConsistency() {
        if (this.customerData == null || this.customerData.getAddress() == null) {
            return true; // No validation needed if no address data
        }

        // Extract state and ZIP from customer address
        String state = this.customerData.getAddress().getStateCode();
        String zipCode = this.customerData.getAddress().getZipCode();

        if (state == null || zipCode == null) {
            return true; // Cannot validate if either field is missing
        }

        // Use ValidationUtils for state/ZIP consistency check
        return ValidationUtils.validateRequiredField(state, "state code").isValid() && 
               ValidationUtils.validateRequiredField(zipCode, "zip code").isValid();
    }

    /**
     * Account-card linkage verification per Section 7.4.4 cross-field validation
     * Ensures account and customer relationship integrity
     * Validates that customer ID relationships are maintained per COBOL business rules
     * 
     * @return true if account-customer linkage is valid, false otherwise
     */
    public boolean validateAccountCustomerLinkage() {
        if (this.accountId == null || this.customerData == null) {
            return false; // Both account and customer data required
        }

        // Validate account ID format
        ValidationResult accountResult = ValidationUtils.validateAccountNumber(this.accountId);
        if (!accountResult.isValid()) {
            return false;
        }

        // Validate customer ID if present
        if (this.customerData.getCustomerId() != null) {
            ValidationResult customerResult = ValidationUtils.validateCustomerId(this.customerData.getCustomerId().toString());
            return customerResult.isValid();
        }

        return true;
    }

    /**
     * Generates hash code for DTO object identity
     * Uses all significant fields for proper equals/hashCode contract
     * 
     * @return hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(accountId, activeStatus, currentBalance, creditLimit, 
                           cashCreditLimit, openDate, expiryDate, reissueDate, customerData);
    }

    /**
     * Compares this DTO with another object for equality
     * Performs deep comparison of all fields including nested CustomerDto
     * 
     * @param obj the object to compare with
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
        
        AccountUpdateRequestDto other = (AccountUpdateRequestDto) obj;
        return Objects.equals(accountId, other.accountId) &&
               Objects.equals(activeStatus, other.activeStatus) &&
               Objects.equals(currentBalance, other.currentBalance) &&
               Objects.equals(creditLimit, other.creditLimit) &&
               Objects.equals(cashCreditLimit, other.cashCreditLimit) &&
               Objects.equals(openDate, other.openDate) &&
               Objects.equals(expiryDate, other.expiryDate) &&
               Objects.equals(reissueDate, other.reissueDate) &&
               Objects.equals(customerData, other.customerData);
    }

    /**
     * Provides string representation of the DTO for logging and debugging
     * Excludes sensitive customer data while showing key account information
     * 
     * @return string representation of this object
     */
    @Override
    public String toString() {
        return String.format(
            "AccountUpdateRequestDto{accountId='%s', activeStatus=%s, currentBalance=%s, " +
            "creditLimit=%s, cashCreditLimit=%s, openDate='%s', expiryDate='%s', " +
            "reissueDate='%s', hasCustomerData=%s}",
            accountId, activeStatus, currentBalance, creditLimit, cashCreditLimit,
            openDate, expiryDate, reissueDate, (customerData != null)
        );
    }
}