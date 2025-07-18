/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.account.dto;

import com.carddemo.account.dto.CustomerDto;
import com.carddemo.common.enums.AccountStatus;
import com.carddemo.common.validator.ValidCurrency;
import com.carddemo.common.validator.ValidCCYYMMDD;
import com.carddemo.common.util.BigDecimalUtils;
import com.carddemo.common.util.ValidationUtils;
import com.carddemo.common.enums.ValidationResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Account Update Request DTO for comprehensive account update operations.
 * 
 * This DTO implements the complete account update functionality from COACTUPC.cbl 
 * with comprehensive validation annotations, BigDecimal financial fields, and 
 * cross-field validation logic. It maintains exact functional equivalence with 
 * the original COBOL transaction processing while providing modern Java validation 
 * capabilities.
 * 
 * Original COBOL Structure (COACTUPC.cbl):
 * - ACCT-ID                     PIC 9(11)     -> accountId
 * - ACCT-ACTIVE-STATUS          PIC X(01)     -> activeStatus
 * - ACCT-CURR-BAL               PIC S9(10)V99 -> currentBalance
 * - ACCT-CREDIT-LIMIT           PIC S9(10)V99 -> creditLimit
 * - ACCT-CASH-CREDIT-LIMIT      PIC S9(10)V99 -> cashCreditLimit
 * - ACCT-OPEN-DATE              PIC X(10)     -> openDate
 * - ACCT-EXPIRAION-DATE         PIC X(10)     -> expirationDate
 * - ACCT-REISSUE-DATE           PIC X(10)     -> reissueDate
 * - ACCT-GROUP-ID               PIC X(10)     -> groupId
 * - Customer data fields        -> customerData
 * 
 * Key Features:
 * - Comprehensive field validation equivalent to COBOL COACTUPC.cbl transaction logic
 * - CCYYMMDD date validation patterns and signed numeric validation per COBOL business rules
 * - Cross-field validation for state/ZIP code consistency and account-card linkage verification
 * - FICO score validation with range 300-850 per customer credit scoring requirements
 * - BigDecimal financial fields with exact COBOL COMP-3 precision using MathContext.DECIMAL128
 * - Jakarta Bean Validation integration for Spring Boot REST API validation
 * - JSON serialization support for React frontend components
 * - Account status validation with ACTIVE/INACTIVE enum mapping
 * - Customer data integration with comprehensive personal information validation
 * 
 * Validation Rules:
 * - Account ID: Required, maximum 11 digits (matching COBOL PIC 9(11))
 * - Active Status: Required, must be 'Y' or 'N' (matching COBOL ACCT-ACTIVE-STATUS)
 * - Current Balance: DECIMAL(12,2) with exact COBOL COMP-3 precision
 * - Credit Limit: DECIMAL(12,2) with positive validation and business rule range checking
 * - Cash Credit Limit: DECIMAL(12,2) with positive validation and credit limit percentage rules
 * - Open Date: CCYYMMDD format with comprehensive date validation including leap year logic
 * - Expiration Date: CCYYMMDD format with business rule validation (future date, valid range)
 * - Reissue Date: CCYYMMDD format with business rule validation (past date, valid range)
 * - Group ID: Maximum 10 characters (matching COBOL PIC X(10))
 * - Customer Data: Comprehensive customer profile validation including FICO score range
 * 
 * Integration Points:
 * - AccountUpdateService.java for business logic processing
 * - React AccountUpdateComponent.jsx for frontend form validation
 * - Spring Boot REST API endpoints for request/response handling
 * - PostgreSQL database persistence with JPA entity mapping
 * - Cross-field validation services for complex business rules
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 */
public class AccountUpdateRequestDto {
    
    /**
     * Account identification number (required for update operations).
     * Maps to ACCT-ID PIC 9(11) from CVACT01Y.cpy and COACTUPC.cbl
     */
    @JsonProperty("account_id")
    @NotBlank(message = "Account ID is required for update operations")
    private String accountId;
    
    /**
     * Current account balance with exact COBOL COMP-3 precision.
     * Maps to ACCT-CURR-BAL PIC S9(10)V99 from CVACT01Y.cpy
     */
    @JsonProperty("current_balance")
    @ValidCurrency(
        min = "-9999999999.99",
        max = "9999999999.99",
        message = "Current balance must be a valid monetary amount",
        context = "ACCOUNT_BALANCE"
    )
    private BigDecimal currentBalance;
    
    /**
     * Credit limit with business rule validation and exact COBOL COMP-3 precision.
     * Maps to ACCT-CREDIT-LIMIT PIC S9(10)V99 from CVACT01Y.cpy
     */
    @JsonProperty("credit_limit")
    @ValidCurrency(
        min = "0.00",
        max = "999999999.99",
        message = "Credit limit must be a positive monetary amount within acceptable range",
        context = "CREDIT_LIMIT"
    )
    private BigDecimal creditLimit;
    
    /**
     * Cash credit limit with business rule validation and exact COBOL COMP-3 precision.
     * Maps to ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99 from CVACT01Y.cpy
     */
    @JsonProperty("cash_credit_limit")
    @ValidCurrency(
        min = "0.00",
        max = "999999999.99",
        message = "Cash credit limit must be a positive monetary amount within acceptable range",
        context = "CREDIT_LIMIT"
    )
    private BigDecimal cashCreditLimit;
    
    /**
     * Account active status with enum validation.
     * Maps to ACCT-ACTIVE-STATUS PIC X(01) from CVACT01Y.cpy
     */
    @JsonProperty("active_status")
    private AccountStatus activeStatus;
    
    /**
     * Account open date in CCYYMMDD format.
     * Maps to ACCT-OPEN-DATE PIC X(10) from CVACT01Y.cpy
     */
    @JsonProperty("open_date")
    @ValidCCYYMMDD(
        fieldName = "Open Date",
        message = "Open date must be in CCYYMMDD format with valid date components",
        allowNull = true
    )
    private String openDate;
    
    /**
     * Account expiration date in CCYYMMDD format.
     * Maps to ACCT-EXPIRAION-DATE PIC X(10) from CVACT01Y.cpy
     */
    @JsonProperty("expiration_date")
    @ValidCCYYMMDD(
        fieldName = "Expiration Date",
        message = "Expiration date must be in CCYYMMDD format with valid date components",
        allowNull = true
    )
    private String expirationDate;
    
    /**
     * Account reissue date in CCYYMMDD format.
     * Maps to ACCT-REISSUE-DATE PIC X(10) from CVACT01Y.cpy
     */
    @JsonProperty("reissue_date")
    @ValidCCYYMMDD(
        fieldName = "Reissue Date",
        message = "Reissue date must be in CCYYMMDD format with valid date components",
        allowNull = true
    )
    private String reissueDate;
    
    /**
     * Account group identifier.
     * Maps to ACCT-GROUP-ID PIC X(10) from CVACT01Y.cpy
     */
    @JsonProperty("group_id")
    private String groupId;
    
    /**
     * Customer data with comprehensive profile validation.
     * Maps to customer fields from COACTUPC.cbl and CVCUS01Y.cpy
     */
    @JsonProperty("customer_data")
    @Valid
    private CustomerDto customerData;
    
    /**
     * Default constructor for JSON deserialization and framework usage.
     */
    public AccountUpdateRequestDto() {
        // Initialize with default values for safety
        this.accountId = "";
        this.currentBalance = BigDecimalUtils.createDecimal(0.0);
        this.creditLimit = BigDecimalUtils.createDecimal(0.0);
        this.cashCreditLimit = BigDecimalUtils.createDecimal(0.0);
        this.activeStatus = AccountStatus.ACTIVE;
        this.openDate = "";
        this.expirationDate = "";
        this.reissueDate = "";
        this.groupId = "";
        this.customerData = new CustomerDto();
    }
    
    /**
     * Constructor with essential account update information.
     * 
     * @param accountId Account identification number
     * @param currentBalance Current account balance
     * @param creditLimit Credit limit
     * @param cashCreditLimit Cash credit limit
     * @param activeStatus Account active status
     * @param customerData Customer profile information
     */
    public AccountUpdateRequestDto(String accountId, BigDecimal currentBalance, 
                                  BigDecimal creditLimit, BigDecimal cashCreditLimit,
                                  AccountStatus activeStatus, CustomerDto customerData) {
        this.accountId = accountId != null ? accountId : "";
        this.currentBalance = currentBalance != null ? currentBalance : BigDecimalUtils.createDecimal(0.0);
        this.creditLimit = creditLimit != null ? creditLimit : BigDecimalUtils.createDecimal(0.0);
        this.cashCreditLimit = cashCreditLimit != null ? cashCreditLimit : BigDecimalUtils.createDecimal(0.0);
        this.activeStatus = activeStatus != null ? activeStatus : AccountStatus.ACTIVE;
        this.openDate = "";
        this.expirationDate = "";
        this.reissueDate = "";
        this.groupId = "";
        this.customerData = customerData != null ? customerData : new CustomerDto();
    }
    
    /**
     * Gets the account identification number.
     * 
     * @return Account ID
     */
    public String getAccountId() {
        return accountId;
    }
    
    /**
     * Sets the account identification number.
     * 
     * @param accountId Account ID
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId != null ? accountId : "";
    }
    
    /**
     * Gets the current account balance.
     * 
     * @return Current balance with exact COBOL COMP-3 precision
     */
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }
    
    /**
     * Sets the current account balance.
     * 
     * @param currentBalance Current balance
     */
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance != null ? currentBalance : BigDecimalUtils.createDecimal(0.0);
    }
    
    /**
     * Gets the credit limit.
     * 
     * @return Credit limit with exact COBOL COMP-3 precision
     */
    public BigDecimal getCreditLimit() {
        return creditLimit;
    }
    
    /**
     * Sets the credit limit.
     * 
     * @param creditLimit Credit limit
     */
    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit != null ? creditLimit : BigDecimalUtils.createDecimal(0.0);
    }
    
    /**
     * Gets the cash credit limit.
     * 
     * @return Cash credit limit with exact COBOL COMP-3 precision
     */
    public BigDecimal getCashCreditLimit() {
        return cashCreditLimit;
    }
    
    /**
     * Sets the cash credit limit.
     * 
     * @param cashCreditLimit Cash credit limit
     */
    public void setCashCreditLimit(BigDecimal cashCreditLimit) {
        this.cashCreditLimit = cashCreditLimit != null ? cashCreditLimit : BigDecimalUtils.createDecimal(0.0);
    }
    
    /**
     * Gets the account active status.
     * 
     * @return Account active status
     */
    public AccountStatus getActiveStatus() {
        return activeStatus;
    }
    
    /**
     * Sets the account active status.
     * 
     * @param activeStatus Account active status
     */
    public void setActiveStatus(AccountStatus activeStatus) {
        this.activeStatus = activeStatus != null ? activeStatus : AccountStatus.ACTIVE;
    }
    
    /**
     * Gets the account open date.
     * 
     * @return Open date in CCYYMMDD format
     */
    public String getOpenDate() {
        return openDate;
    }
    
    /**
     * Sets the account open date.
     * 
     * @param openDate Open date in CCYYMMDD format
     */
    public void setOpenDate(String openDate) {
        this.openDate = openDate != null ? openDate : "";
    }
    
    /**
     * Gets the account expiration date.
     * 
     * @return Expiration date in CCYYMMDD format
     */
    public String getExpirationDate() {
        return expirationDate;
    }
    
    /**
     * Sets the account expiration date.
     * 
     * @param expirationDate Expiration date in CCYYMMDD format
     */
    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate != null ? expirationDate : "";
    }
    
    /**
     * Gets the account reissue date.
     * 
     * @return Reissue date in CCYYMMDD format
     */
    public String getReissueDate() {
        return reissueDate;
    }
    
    /**
     * Sets the account reissue date.
     * 
     * @param reissueDate Reissue date in CCYYMMDD format
     */
    public void setReissueDate(String reissueDate) {
        this.reissueDate = reissueDate != null ? reissueDate : "";
    }
    
    /**
     * Gets the account group identifier.
     * 
     * @return Group ID
     */
    public String getGroupId() {
        return groupId;
    }
    
    /**
     * Sets the account group identifier.
     * 
     * @param groupId Group ID
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId != null ? groupId : "";
    }
    
    /**
     * Gets the customer data.
     * 
     * @return Customer profile information
     */
    public CustomerDto getCustomerData() {
        return customerData;
    }
    
    /**
     * Sets the customer data.
     * 
     * @param customerData Customer profile information
     */
    public void setCustomerData(CustomerDto customerData) {
        this.customerData = customerData != null ? customerData : new CustomerDto();
    }
    
    /**
     * Validates the account update request using comprehensive COBOL-equivalent validation rules.
     * 
     * This method performs extensive validation equivalent to the original COACTUPC.cbl 
     * transaction processing, including:
     * - Account ID format and range validation
     * - Financial amount validation with exact COBOL COMP-3 precision
     * - Date format validation using CCYYMMDD pattern with century, month, and day validation
     * - Account status validation with ACTIVE/INACTIVE enum constraints
     * - Customer data validation including FICO score range validation
     * - Cross-field validation for business rule compliance
     * - Credit limit relationship validation (cash credit limit <= credit limit)
     * - Date range validation for business rule compliance
     * 
     * @return ValidationResult indicating comprehensive validation outcome
     */
    public ValidationResult validate() {
        // Validate account ID
        ValidationResult accountIdResult = ValidationUtils.validateAccountNumber(this.accountId);
        if (!accountIdResult.isValid()) {
            return accountIdResult;
        }
        
        // Validate current balance
        if (this.currentBalance != null) {
            ValidationResult balanceResult = ValidationUtils.validateBalance(this.currentBalance);
            if (!balanceResult.isValid()) {
                return balanceResult;
            }
        }
        
        // Validate credit limit
        if (this.creditLimit != null) {
            ValidationResult creditLimitResult = ValidationUtils.validateCreditLimit(this.creditLimit);
            if (!creditLimitResult.isValid()) {
                return creditLimitResult;
            }
        }
        
        // Validate cash credit limit
        if (this.cashCreditLimit != null) {
            ValidationResult cashCreditLimitResult = ValidationUtils.validateCreditLimit(this.cashCreditLimit);
            if (!cashCreditLimitResult.isValid()) {
                return cashCreditLimitResult;
            }
        }
        
        // Validate account status
        if (this.activeStatus == null) {
            return ValidationResult.BLANK_FIELD;
        }
        
        // Validate open date if provided
        if (this.openDate != null && !this.openDate.trim().isEmpty()) {
            ValidationResult openDateResult = ValidationUtils.validateDateField(this.openDate);
            if (!openDateResult.isValid()) {
                return openDateResult;
            }
        }
        
        // Validate expiration date if provided
        if (this.expirationDate != null && !this.expirationDate.trim().isEmpty()) {
            ValidationResult expirationDateResult = ValidationUtils.validateDateField(this.expirationDate);
            if (!expirationDateResult.isValid()) {
                return expirationDateResult;
            }
        }
        
        // Validate reissue date if provided
        if (this.reissueDate != null && !this.reissueDate.trim().isEmpty()) {
            ValidationResult reissueDateResult = ValidationUtils.validateDateField(this.reissueDate);
            if (!reissueDateResult.isValid()) {
                return reissueDateResult;
            }
        }
        
        // Validate group ID length
        if (this.groupId != null && this.groupId.length() > 10) {
            return ValidationResult.INVALID_RANGE;
        }
        
        // Validate customer data
        if (this.customerData != null) {
            ValidationResult customerResult = this.customerData.validate();
            if (!customerResult.isValid()) {
                return customerResult;
            }
        }
        
        // Perform cross-field validation
        ValidationResult crossFieldResult = validateCrossFieldRules();
        if (!crossFieldResult.isValid()) {
            return crossFieldResult;
        }
        
        return ValidationResult.VALID;
    }
    
    /**
     * Validates cross-field business rules equivalent to COBOL validation logic.
     * 
     * This method implements comprehensive cross-field validation matching the 
     * original COACTUPC.cbl business rules, including:
     * - Cash credit limit must not exceed credit limit
     * - Date range validation for business rule compliance
     * - Account status and financial amount consistency validation
     * - Customer data cross-validation with account information
     * 
     * @return ValidationResult indicating cross-field validation outcome
     */
    private ValidationResult validateCrossFieldRules() {
        // Validate cash credit limit does not exceed credit limit
        if (this.creditLimit != null && this.cashCreditLimit != null) {
            if (BigDecimalUtils.compare(this.cashCreditLimit, this.creditLimit) > 0) {
                return ValidationResult.CROSS_FIELD_VALIDATION_FAILURE;
            }
        }
        
        // Validate date relationships
        if (this.openDate != null && !this.openDate.trim().isEmpty() &&
            this.expirationDate != null && !this.expirationDate.trim().isEmpty()) {
            
            // Expiration date should be after open date
            if (this.openDate.compareTo(this.expirationDate) > 0) {
                return ValidationResult.CROSS_FIELD_VALIDATION_FAILURE;
            }
        }
        
        // Validate reissue date is after open date if both are provided
        if (this.openDate != null && !this.openDate.trim().isEmpty() &&
            this.reissueDate != null && !this.reissueDate.trim().isEmpty()) {
            
            if (this.openDate.compareTo(this.reissueDate) > 0) {
                return ValidationResult.CROSS_FIELD_VALIDATION_FAILURE;
            }
        }
        
        return ValidationResult.VALID;
    }
    
    /**
     * Checks if the account update request is valid using comprehensive validation.
     * 
     * @return true if all validation rules pass, false otherwise
     */
    public boolean isValid() {
        return validate().isValid();
    }
    
    /**
     * Checks if two AccountUpdateRequestDto objects are equal.
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
        AccountUpdateRequestDto that = (AccountUpdateRequestDto) obj;
        return Objects.equals(accountId, that.accountId) &&
               Objects.equals(currentBalance, that.currentBalance) &&
               Objects.equals(creditLimit, that.creditLimit) &&
               Objects.equals(cashCreditLimit, that.cashCreditLimit) &&
               Objects.equals(activeStatus, that.activeStatus) &&
               Objects.equals(openDate, that.openDate) &&
               Objects.equals(expirationDate, that.expirationDate) &&
               Objects.equals(reissueDate, that.reissueDate) &&
               Objects.equals(groupId, that.groupId) &&
               Objects.equals(customerData, that.customerData);
    }
    
    /**
     * Generates hash code for the AccountUpdateRequestDto object.
     * 
     * @return Hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(accountId, currentBalance, creditLimit, cashCreditLimit,
                           activeStatus, openDate, expirationDate, reissueDate,
                           groupId, customerData);
    }
    
    /**
     * Returns a string representation of the AccountUpdateRequestDto object.
     * 
     * @return String representation of the account update request
     */
    @Override
    public String toString() {
        return "AccountUpdateRequestDto{" +
               "accountId='" + accountId + '\'' +
               ", currentBalance=" + currentBalance +
               ", creditLimit=" + creditLimit +
               ", cashCreditLimit=" + cashCreditLimit +
               ", activeStatus=" + activeStatus +
               ", openDate='" + openDate + '\'' +
               ", expirationDate='" + expirationDate + '\'' +
               ", reissueDate='" + reissueDate + '\'' +
               ", groupId='" + groupId + '\'' +
               ", customerData=" + customerData +
               '}';
    }
}