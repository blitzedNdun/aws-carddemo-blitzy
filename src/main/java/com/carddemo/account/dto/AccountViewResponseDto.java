/*
 * Copyright 2024 CardDemo Application
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carddemo.account.dto;

import com.carddemo.account.dto.CustomerDto;
import com.carddemo.common.enums.AccountStatus;
import com.carddemo.common.util.BigDecimalUtils;
import com.carddemo.common.enums.ValidationResult;
import com.carddemo.common.enums.CardStatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Digits;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.Objects;
import java.time.LocalDate;

/**
 * Account View Response DTO containing complete account details, customer information, and card data.
 * 
 * <p>This comprehensive response DTO aggregates account data from multiple COBOL sources including
 * ACCOUNT-RECORD (CVACT01Y.cpy), CUSTOMER-RECORD (CVCUS01Y.cpy), and CARD-RECORD (CVACT02Y.cpy)
 * to provide a complete view of account information for the account view operations (COACTVWC.cbl).
 * It maintains exact functional equivalence with the original COBOL screen layout while providing
 * enhanced validation and modern JSON serialization capabilities.</p>
 * 
 * <p>Key Features:</p>
 * <ul>
 *   <li>Complete account details with real-time balance and credit limit information</li>
 *   <li>Integrated customer profile data with comprehensive personal information</li>
 *   <li>Card cross-reference information for account-to-card relationship mapping</li>
 *   <li>BigDecimal precision for financial amounts using DECIMAL128 context per Section 0.1.2</li>
 *   <li>JSON serialization with field-level annotations preserving BMS attribute byte behavior</li>
 *   <li>Comprehensive validation annotations for Spring Boot and React integration</li>
 *   <li>Success/error response handling for robust API communication</li>
 * </ul>
 * 
 * <p>COBOL Pattern Conversion:</p>
 * <pre>
 * Original COBOL Screen Layout (COACTVW.bms):
 *   Account ID        : ACCTSID    (11 characters)
 *   Account Status    : ACSTTUS    (1 character - Y/N)
 *   Current Balance   : ACURBAL    (S9(10)V99 COMP-3)
 *   Credit Limit      : ACRDLIM    (S9(10)V99 COMP-3)
 *   Cash Credit Limit : ACSHLIM    (S9(10)V99 COMP-3)
 *   Current Cyc Credit: ACRCYCR    (S9(10)V99 COMP-3)
 *   Current Cyc Debit : ACRCYDB    (S9(10)V99 COMP-3)
 *   Open Date         : ADTOPEN    (X(10) - CCYYMMDD)
 *   Expiry Date       : AEXPDT     (X(10) - CCYYMMDD)
 *   Reissue Date      : AREISDT    (X(10) - CCYYMMDD)
 *   Group ID          : AADDGRP    (X(10))
 *   Customer Data     : Nested customer information
 *   Card Number       : From XREF-CARD-NUM (X(16))
 * 
 * Java DTO Equivalent:
 *   {@literal @}Digits(integer = 11, fraction = 0) private String accountId;
 *   private AccountStatus activeStatus;
 *   {@literal @}Digits(integer = 10, fraction = 2) private BigDecimal currentBalance;
 *   {@literal @}Digits(integer = 10, fraction = 2) private BigDecimal creditLimit;
 *   {@literal @}Digits(integer = 10, fraction = 2) private BigDecimal cashCreditLimit;
 *   {@literal @}Digits(integer = 10, fraction = 2) private BigDecimal currentCycleCredit;
 *   {@literal @}Digits(integer = 10, fraction = 2) private BigDecimal currentCycleDebit;
 *   private LocalDate openDate;
 *   private LocalDate expirationDate;
 *   private LocalDate reissueDate;
 *   private String groupId;
 *   {@literal @}Valid private CustomerDto customerData;
 *   private String cardNumber;
 * </pre>
 * 
 * <p>Business Logic Integration:</p>
 * <ul>
 *   <li>Account data retrieval with real-time balance per Section 2.1.3 F-003-RQ-001</li>
 *   <li>Field validation for financial amounts and dates per Section 2.1.3 F-003-RQ-002</li>
 *   <li>Account-to-customer relationship integrity per Section 2.1.3 F-003-RQ-004</li>
 *   <li>JSON DTO structure for account management operations per Section 7.4.3</li>
 * </ul>
 * 
 * <p>Performance Requirements:</p>
 * <ul>
 *   <li>Supports sub-200ms response time for account view operations at 95th percentile</li>
 *   <li>Optimized for 10,000+ TPS account query processing</li>
 *   <li>Memory efficient for concurrent account view requests</li>
 *   <li>Thread-safe validation methods for microservices architecture</li>
 * </ul>
 * 
 * <p>Usage Examples:</p>
 * <pre>
 * // Create successful account view response
 * AccountViewResponseDto response = new AccountViewResponseDto();
 * response.setAccountId("00000000001");
 * response.setActiveStatus(AccountStatus.ACTIVE);
 * response.setCurrentBalance(BigDecimalUtils.createDecimal("1234.56"));
 * response.setCreditLimit(BigDecimalUtils.createDecimal("5000.00"));
 * response.setOpenDate(LocalDate.parse("2022-01-15"));
 * 
 * // Set customer data
 * CustomerDto customer = new CustomerDto();
 * customer.setCustomerId(123456789L);
 * customer.setFirstName("John");
 * customer.setLastName("Doe");
 * response.setCustomerData(customer);
 * 
 * // Set card information
 * response.setCardNumber("4111111111111111");
 * response.setSuccess(true);
 * 
 * // JSON serialization for React frontend
 * ObjectMapper mapper = new ObjectMapper();
 * String json = mapper.writeValueAsString(response);
 * </pre>
 * 
 * @author Blitzy agent
 * @version 1.0
 * @since CardDemo v1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountViewResponseDto {

    /**
     * Account unique identifier (11-digit numeric string)
     * Maps to COBOL field: ACCT-ID PIC 9(11)
     */
    @Digits(integer = 11, fraction = 0, message = "Account ID must be exactly 11 digits")
    private String accountId;

    /**
     * Current account balance with exact decimal precision
     * Maps to COBOL field: ACCT-CURR-BAL PIC S9(10)V99 COMP-3
     */
    @Digits(integer = 10, fraction = 2, message = "Current balance must have maximum 10 integer digits and 2 decimal places")
    private BigDecimal currentBalance;

    /**
     * Credit limit for the account with exact decimal precision
     * Maps to COBOL field: ACCT-CREDIT-LIMIT PIC S9(10)V99 COMP-3
     */
    @Digits(integer = 10, fraction = 2, message = "Credit limit must have maximum 10 integer digits and 2 decimal places")
    private BigDecimal creditLimit;

    /**
     * Cash credit limit for the account with exact decimal precision
     * Maps to COBOL field: ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99 COMP-3
     */
    @Digits(integer = 10, fraction = 2, message = "Cash credit limit must have maximum 10 integer digits and 2 decimal places")
    private BigDecimal cashCreditLimit;

    /**
     * Account active status indicating account lifecycle state
     * Maps to COBOL field: ACCT-ACTIVE-STATUS PIC X(01)
     */
    private AccountStatus activeStatus;

    /**
     * Account opening date
     * Maps to COBOL field: ACCT-OPEN-DATE PIC X(10)
     */
    private LocalDate openDate;

    /**
     * Account expiration date
     * Maps to COBOL field: ACCT-EXPIRAION-DATE PIC X(10)
     */
    private LocalDate expirationDate;

    /**
     * Account reissue date
     * Maps to COBOL field: ACCT-REISSUE-DATE PIC X(10)
     */
    private LocalDate reissueDate;

    /**
     * Current cycle credit amount with exact decimal precision
     * Maps to COBOL field: ACCT-CURR-CYC-CREDIT PIC S9(10)V99 COMP-3
     */
    @Digits(integer = 10, fraction = 2, message = "Current cycle credit must have maximum 10 integer digits and 2 decimal places")
    private BigDecimal currentCycleCredit;

    /**
     * Current cycle debit amount with exact decimal precision
     * Maps to COBOL field: ACCT-CURR-CYC-DEBIT PIC S9(10)V99 COMP-3
     */
    @Digits(integer = 10, fraction = 2, message = "Current cycle debit must have maximum 10 integer digits and 2 decimal places")
    private BigDecimal currentCycleDebit;

    /**
     * Account group identifier for account categorization
     * Maps to COBOL field: ACCT-GROUP-ID PIC X(10)
     */
    private String groupId;

    /**
     * Complete customer information associated with the account
     * Aggregated from CUSTOMER-RECORD structure (CVCUS01Y.cpy)
     */
    @Valid
    private CustomerDto customerData;

    /**
     * Primary card number associated with the account
     * Maps to COBOL field: XREF-CARD-NUM PIC X(16)
     */
    private String cardNumber;

    /**
     * Response success indicator for API communication
     * Indicates whether the account view operation was successful
     */
    private boolean success;

    /**
     * Error message for failed operations
     * Provides detailed error information for debugging and user feedback
     */
    private String errorMessage;

    /**
     * Default constructor for AccountViewResponseDto.
     * Initializes all fields to appropriate default values for proper validation handling.
     */
    public AccountViewResponseDto() {
        this.accountId = null;
        this.currentBalance = null;
        this.creditLimit = null;
        this.cashCreditLimit = null;
        this.activeStatus = null;
        this.openDate = null;
        this.expirationDate = null;
        this.reissueDate = null;
        this.currentCycleCredit = null;
        this.currentCycleDebit = null;
        this.groupId = null;
        this.customerData = null;
        this.cardNumber = null;
        this.success = false;
        this.errorMessage = null;
    }

    /**
     * Comprehensive constructor for AccountViewResponseDto with all account fields.
     * 
     * @param accountId Account unique identifier
     * @param currentBalance Current account balance
     * @param creditLimit Credit limit for the account
     * @param cashCreditLimit Cash credit limit for the account
     * @param activeStatus Account active status
     * @param openDate Account opening date
     * @param expirationDate Account expiration date
     * @param reissueDate Account reissue date
     * @param currentCycleCredit Current cycle credit amount
     * @param currentCycleDebit Current cycle debit amount
     * @param groupId Account group identifier
     * @param customerData Customer information
     * @param cardNumber Primary card number
     * @param success Response success indicator
     * @param errorMessage Error message for failed operations
     */
    public AccountViewResponseDto(String accountId, BigDecimal currentBalance, BigDecimal creditLimit,
                                  BigDecimal cashCreditLimit, AccountStatus activeStatus, LocalDate openDate,
                                  LocalDate expirationDate, LocalDate reissueDate, BigDecimal currentCycleCredit,
                                  BigDecimal currentCycleDebit, String groupId, CustomerDto customerData,
                                  String cardNumber, boolean success, String errorMessage) {
        this.accountId = accountId;
        this.currentBalance = currentBalance;
        this.creditLimit = creditLimit;
        this.cashCreditLimit = cashCreditLimit;
        this.activeStatus = activeStatus;
        this.openDate = openDate;
        this.expirationDate = expirationDate;
        this.reissueDate = reissueDate;
        this.currentCycleCredit = currentCycleCredit;
        this.currentCycleDebit = currentCycleDebit;
        this.groupId = groupId;
        this.customerData = customerData;
        this.cardNumber = cardNumber;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    /**
     * Gets the account unique identifier.
     * 
     * @return the account ID
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account unique identifier.
     * 
     * @param accountId the account ID (must be 11 digits)
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the current account balance.
     * 
     * @return the current balance with exact decimal precision
     */
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    /**
     * Sets the current account balance.
     * 
     * @param currentBalance the current balance with DECIMAL128 precision
     */
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    /**
     * Gets the credit limit for the account.
     * 
     * @return the credit limit with exact decimal precision
     */
    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    /**
     * Sets the credit limit for the account.
     * 
     * @param creditLimit the credit limit with DECIMAL128 precision
     */
    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    /**
     * Gets the cash credit limit for the account.
     * 
     * @return the cash credit limit with exact decimal precision
     */
    public BigDecimal getCashCreditLimit() {
        return cashCreditLimit;
    }

    /**
     * Sets the cash credit limit for the account.
     * 
     * @param cashCreditLimit the cash credit limit with DECIMAL128 precision
     */
    public void setCashCreditLimit(BigDecimal cashCreditLimit) {
        this.cashCreditLimit = cashCreditLimit;
    }

    /**
     * Gets the account active status.
     * 
     * @return the account status (ACTIVE/INACTIVE)
     */
    public AccountStatus getActiveStatus() {
        return activeStatus;
    }

    /**
     * Sets the account active status.
     * 
     * @param activeStatus the account status
     */
    public void setActiveStatus(AccountStatus activeStatus) {
        this.activeStatus = activeStatus;
    }

    /**
     * Gets the account opening date.
     * 
     * @return the account opening date
     */
    public LocalDate getOpenDate() {
        return openDate;
    }

    /**
     * Sets the account opening date.
     * 
     * @param openDate the account opening date
     */
    public void setOpenDate(LocalDate openDate) {
        this.openDate = openDate;
    }

    /**
     * Gets the account expiration date.
     * 
     * @return the account expiration date
     */
    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    /**
     * Sets the account expiration date.
     * 
     * @param expirationDate the account expiration date
     */
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * Gets the account reissue date.
     * 
     * @return the account reissue date
     */
    public LocalDate getReissueDate() {
        return reissueDate;
    }

    /**
     * Sets the account reissue date.
     * 
     * @param reissueDate the account reissue date
     */
    public void setReissueDate(LocalDate reissueDate) {
        this.reissueDate = reissueDate;
    }

    /**
     * Gets the current cycle credit amount.
     * 
     * @return the current cycle credit with exact decimal precision
     */
    public BigDecimal getCurrentCycleCredit() {
        return currentCycleCredit;
    }

    /**
     * Sets the current cycle credit amount.
     * 
     * @param currentCycleCredit the current cycle credit with DECIMAL128 precision
     */
    public void setCurrentCycleCredit(BigDecimal currentCycleCredit) {
        this.currentCycleCredit = currentCycleCredit;
    }

    /**
     * Gets the current cycle debit amount.
     * 
     * @return the current cycle debit with exact decimal precision
     */
    public BigDecimal getCurrentCycleDebit() {
        return currentCycleDebit;
    }

    /**
     * Sets the current cycle debit amount.
     * 
     * @param currentCycleDebit the current cycle debit with DECIMAL128 precision
     */
    public void setCurrentCycleDebit(BigDecimal currentCycleDebit) {
        this.currentCycleDebit = currentCycleDebit;
    }

    /**
     * Gets the account group identifier.
     * 
     * @return the group ID
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Sets the account group identifier.
     * 
     * @param groupId the group ID (max 10 characters)
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * Gets the customer information associated with the account.
     * 
     * @return the customer data DTO
     */
    public CustomerDto getCustomerData() {
        return customerData;
    }

    /**
     * Sets the customer information associated with the account.
     * 
     * @param customerData the customer data DTO with validation
     */
    public void setCustomerData(CustomerDto customerData) {
        this.customerData = customerData;
    }

    /**
     * Gets the primary card number associated with the account.
     * 
     * @return the card number
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the primary card number associated with the account.
     * 
     * @param cardNumber the card number (16 digits)
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Gets the response success indicator.
     * 
     * @return true if operation was successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Sets the response success indicator.
     * 
     * @param success true if operation was successful, false otherwise
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Gets the error message for failed operations.
     * 
     * @return the error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message for failed operations.
     * 
     * @param errorMessage the error message
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Validates the complete account view response data with comprehensive business rule validation.
     * 
     * <p>This method provides comprehensive account response validation including:</p>
     * <ul>
     *   <li>Required field validation for account ID and basic account information</li>
     *   <li>Financial amount validation using BigDecimalUtils for exact precision</li>
     *   <li>Account status validation using AccountStatus enum</li>
     *   <li>Date validation for account lifecycle dates</li>
     *   <li>Customer data validation using CustomerDto.validate() method</li>
     *   <li>Card number validation for cross-reference integrity</li>
     *   <li>Cross-field validation for business rule compliance</li>
     * </ul>
     * 
     * <p>The validation maintains exact functional equivalence with the original COBOL
     * account view validation logic while providing enhanced error messaging for modern
     * user interfaces.</p>
     * 
     * @return ValidationResult.VALID if all validation passes, appropriate error result otherwise
     */
    public ValidationResult validate() {
        // Validate required field - account ID is mandatory
        if (accountId == null || accountId.trim().isEmpty()) {
            return ValidationResult.BLANK_FIELD;
        }

        // Validate account ID format (must be 11 digits)
        if (!accountId.matches("\\d{11}")) {
            return ValidationResult.INVALID_FORMAT;
        }

        // Validate account ID range (must be non-zero)
        if (accountId.equals("00000000000")) {
            return ValidationResult.INVALID_RANGE;
        }

        // Validate financial amounts if present
        if (currentBalance != null) {
            try {
                BigDecimalUtils.validateFinancialAmount(currentBalance);
            } catch (IllegalArgumentException e) {
                return ValidationResult.INVALID_RANGE;
            }
        }

        if (creditLimit != null) {
            try {
                BigDecimalUtils.validateFinancialAmount(creditLimit);
            } catch (IllegalArgumentException e) {
                return ValidationResult.INVALID_RANGE;
            }
            
            // Credit limit must be positive
            if (BigDecimalUtils.isNegative(creditLimit)) {
                return ValidationResult.INVALID_RANGE;
            }
        }

        if (cashCreditLimit != null) {
            try {
                BigDecimalUtils.validateFinancialAmount(cashCreditLimit);
            } catch (IllegalArgumentException e) {
                return ValidationResult.INVALID_RANGE;
            }
            
            // Cash credit limit must be positive
            if (BigDecimalUtils.isNegative(cashCreditLimit)) {
                return ValidationResult.INVALID_RANGE;
            }
        }

        if (currentCycleCredit != null) {
            try {
                BigDecimalUtils.validateFinancialAmount(currentCycleCredit);
            } catch (IllegalArgumentException e) {
                return ValidationResult.INVALID_RANGE;
            }
        }

        if (currentCycleDebit != null) {
            try {
                BigDecimalUtils.validateFinancialAmount(currentCycleDebit);
            } catch (IllegalArgumentException e) {
                return ValidationResult.INVALID_RANGE;
            }
        }

        // Validate account status if present
        if (activeStatus == null) {
            return ValidationResult.BLANK_FIELD;
        }

        // Validate group ID length (if provided)
        if (groupId != null && groupId.length() > 10) {
            return ValidationResult.INVALID_FORMAT;
        }

        // Validate customer data if present
        if (customerData != null) {
            ValidationResult customerValidation = customerData.validate();
            if (!customerValidation.isValid()) {
                return customerValidation;
            }
        }

        // Validate card number format (if provided)
        if (cardNumber != null && !cardNumber.trim().isEmpty()) {
            // Basic card number format validation - 16 digits
            if (!cardNumber.matches("\\d{16}")) {
                return ValidationResult.INVALID_FORMAT;
            }
        }

        // Validate date consistency
        if (openDate != null && expirationDate != null) {
            if (openDate.isAfter(expirationDate)) {
                return ValidationResult.BAD_DATE_VALUE;
            }
        }

        if (openDate != null && reissueDate != null) {
            if (openDate.isAfter(reissueDate)) {
                return ValidationResult.BAD_DATE_VALUE;
            }
        }

        // Validate business rules for credit limits
        if (creditLimit != null && cashCreditLimit != null) {
            // Cash credit limit should not exceed credit limit
            if (BigDecimalUtils.compare(cashCreditLimit, creditLimit) > 0) {
                return ValidationResult.INVALID_RANGE;
            }
        }

        // Validate cycle amounts relationship
        if (currentCycleCredit != null && currentCycleDebit != null) {
            // Both amounts should be positive or zero
            if (BigDecimalUtils.isNegative(currentCycleCredit) || BigDecimalUtils.isNegative(currentCycleDebit)) {
                return ValidationResult.INVALID_RANGE;
            }
        }

        // All validations passed
        return ValidationResult.VALID;
    }

    /**
     * Checks if this account view response object is equal to another object.
     * 
     * <p>This method provides deep equality comparison for account view response objects,
     * comparing all account fields for exact matches. It supports null-safe comparison
     * and maintains consistent behavior with hashCode() method.</p>
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

        AccountViewResponseDto that = (AccountViewResponseDto) obj;
        return success == that.success &&
               Objects.equals(accountId, that.accountId) &&
               Objects.equals(currentBalance, that.currentBalance) &&
               Objects.equals(creditLimit, that.creditLimit) &&
               Objects.equals(cashCreditLimit, that.cashCreditLimit) &&
               Objects.equals(activeStatus, that.activeStatus) &&
               Objects.equals(openDate, that.openDate) &&
               Objects.equals(expirationDate, that.expirationDate) &&
               Objects.equals(reissueDate, that.reissueDate) &&
               Objects.equals(currentCycleCredit, that.currentCycleCredit) &&
               Objects.equals(currentCycleDebit, that.currentCycleDebit) &&
               Objects.equals(groupId, that.groupId) &&
               Objects.equals(customerData, that.customerData) &&
               Objects.equals(cardNumber, that.cardNumber) &&
               Objects.equals(errorMessage, that.errorMessage);
    }

    /**
     * Generates a hash code for this account view response object.
     * 
     * <p>This method provides consistent hash code generation based on all account
     * fields, supporting proper behavior in collections and hash-based data structures.</p>
     * 
     * @return hash code value for this account view response object
     */
    @Override
    public int hashCode() {
        return Objects.hash(accountId, currentBalance, creditLimit, cashCreditLimit, activeStatus,
                           openDate, expirationDate, reissueDate, currentCycleCredit, currentCycleDebit,
                           groupId, customerData, cardNumber, success, errorMessage);
    }

    /**
     * Returns a string representation of this account view response object.
     * 
     * <p>This method provides a comprehensive string representation including all
     * account fields for debugging and logging purposes. It maintains consistent
     * formatting and handles null values appropriately.</p>
     * 
     * @return string representation of the account view response
     */
    @Override
    public String toString() {
        return String.format(
            "AccountViewResponseDto{accountId='%s', currentBalance=%s, creditLimit=%s, " +
            "cashCreditLimit=%s, activeStatus=%s, openDate=%s, expirationDate=%s, " +
            "reissueDate=%s, currentCycleCredit=%s, currentCycleDebit=%s, groupId='%s', " +
            "customerData=%s, cardNumber='%s', success=%b, errorMessage='%s'}",
            accountId, currentBalance, creditLimit, cashCreditLimit, activeStatus,
            openDate, expirationDate, reissueDate, currentCycleCredit, currentCycleDebit,
            groupId, customerData, cardNumber, success, errorMessage
        );
    }
}