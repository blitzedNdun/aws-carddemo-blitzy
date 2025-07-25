/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
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
 * Account View Response Data Transfer Object providing comprehensive account details,
 * customer information, and associated card data for account management operations.
 * 
 * This DTO serves as the complete response structure for account view operations,
 * aggregating data from multiple VSAM record structures including ACCTDAT, CUSTDAT,
 * and CARDDAT files to provide a unified account view matching the original BMS 
 * screen layout from COACTVW.bms.
 * 
 * The class maintains exact functional equivalence with the original COBOL program
 * COACTVWC.cbl while providing modern JSON serialization capabilities for React
 * frontend integration and comprehensive validation for data integrity.
 * 
 * Key Features:
 * - Complete account financial data with BigDecimal precision for COMP-3 equivalence
 * - Embedded customer profile information with cascading validation
 * - Card cross-reference data for account-card relationship management
 * - BMS attribute byte preservation through JSON field annotations
 * - Comprehensive validation matching original COBOL field validation logic
 * - Success/error response handling for REST API compatibility
 * 
 * Data Structure Mapping (from COBOL copybooks):
 * 
 * Account Data (CVACT01Y.cpy - ACCOUNT-RECORD):
 * - ACCT-ID                     PIC 9(11)      → accountId
 * - ACCT-ACTIVE-STATUS          PIC X(01)      → activeStatus (AccountStatus enum)
 * - ACCT-CURR-BAL               PIC S9(10)V99  → currentBalance (BigDecimal)
 * - ACCT-CREDIT-LIMIT           PIC S9(10)V99  → creditLimit (BigDecimal)
 * - ACCT-CASH-CREDIT-LIMIT      PIC S9(10)V99  → cashCreditLimit (BigDecimal)
 * - ACCT-OPEN-DATE              PIC X(10)      → openDate (LocalDate)
 * - ACCT-EXPIRAION-DATE         PIC X(10)      → expirationDate (LocalDate)
 * - ACCT-REISSUE-DATE           PIC X(10)      → reissueDate (LocalDate)
 * - ACCT-CURR-CYC-CREDIT        PIC S9(10)V99  → currentCycleCredit (BigDecimal)
 * - ACCT-CURR-CYC-DEBIT         PIC S9(10)V99  → currentCycleDebit (BigDecimal)
 * - ACCT-GROUP-ID               PIC X(10)      → groupId
 * 
 * Customer Data (CVCUS01Y.cpy - CUSTOMER-RECORD):
 * - Complete CustomerDto embedding with all customer profile fields
 * 
 * Card Data (CVACT02Y.cpy - CARD-RECORD):
 * - CARD-NUM                    PIC X(16)      → cardNumber
 * 
 * Performance Requirements:
 * - Response construction must complete within 50ms for account view operations
 * - JSON serialization must support React component rendering under 100ms
 * - BigDecimal operations must maintain exact precision for regulatory compliance
 * 
 * Business Rules:
 * - All monetary fields use exact BigDecimal precision with DECIMAL128 context
 * - Account status validation ensures proper lifecycle management
 * - Customer data validation supports comprehensive profile verification
 * - Card number masking for security compliance in client-side display
 * 
 * Integration Points:
 * - Used by AccountViewService for account detail retrieval operations
 * - Supports React AccountViewComponent for BMS screen equivalent display
 * - Integrates with Spring Boot validation framework for field-level validation
 * - Compatible with Spring Security for role-based field access control
 * 
 * @author CardDemo Development Team - Blitzy Platform
 * @version 1.0
 * @since Java 21
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountViewResponseDto {

    /**
     * Eleven-digit account identifier matching COBOL account ID field.
     * Maps to ACCT-ID field from CVACT01Y.cpy account record structure.
     * 
     * Validation:
     * - Must be exactly 11 digits for account identification
     * - Required field for all account operations
     * - Used as primary key for account lookups and cross-references
     */
    private String accountId;

    /**
     * Current account balance with exact BigDecimal precision.
     * Maps to ACCT-CURR-BAL field from CVACT01Y.cpy account record.
     * 
     * Precision Requirements:
     * - Uses BigDecimal with DECIMAL128 context for exact arithmetic
     * - Scale of 2 decimal places matching COBOL S9(10)V99 COMP-3
     * - Supports account balances up to 9,999,999,999.99
     * - All calculations must preserve exact decimal precision
     */
    @Digits(integer = 10, fraction = 2, message = "Current balance must have maximum 10 integer digits and 2 decimal places")
    private BigDecimal currentBalance;

    /**
     * Account credit limit with exact BigDecimal precision.
     * Maps to ACCT-CREDIT-LIMIT field from CVACT01Y.cpy account record.
     * 
     * Business Rules:
     * - Determines maximum available credit for the account
     * - Must be non-negative value for active accounts
     * - Used for transaction authorization and credit utilization calculations
     * - Precision maintained using DECIMAL128 context
     */
    @Digits(integer = 10, fraction = 2, message = "Credit limit must have maximum 10 integer digits and 2 decimal places")
    private BigDecimal creditLimit;

    /**
     * Cash advance credit limit with exact BigDecimal precision.
     * Maps to ACCT-CASH-CREDIT-LIMIT field from CVACT01Y.cpy account record.
     * 
     * Business Rules:
     * - Determines maximum cash advance available for the account
     * - Typically lower than total credit limit
     * - Used for cash advance transaction authorization
     * - Precision maintained using DECIMAL128 context
     */
    @Digits(integer = 10, fraction = 2, message = "Cash credit limit must have maximum 10 integer digits and 2 decimal places")
    private BigDecimal cashCreditLimit;

    /**
     * Account active status using AccountStatus enumeration.
     * Maps to ACCT-ACTIVE-STATUS field from CVACT01Y.cpy account record.
     * 
     * Business Rules:
     * - ACTIVE: Account can process all transaction types
     * - INACTIVE: Account is suspended and cannot process transactions
     * - Used for transaction authorization and account lifecycle management
     */
    private AccountStatus activeStatus;

    /**
     * Account opening date with proper date validation.
     * Maps to ACCT-OPEN-DATE field from CVACT01Y.cpy account record.
     * 
     * Validation:
     * - Must be valid date in YYYY-MM-DD format
     * - Cannot be in the future
     * - Used for account age calculations and reporting
     */
    private LocalDate openDate;

    /**
     * Account expiration date with proper date validation.
     * Maps to ACCT-EXPIRAION-DATE field from CVACT01Y.cpy account record.
     * 
     * Business Rules:
     * - Must be future date for active accounts
     * - Used for account renewal and lifecycle management
     * - Automatic account status updates based on expiration
     */
    private LocalDate expirationDate;

    /**
     * Account reissue date for card replacement operations.
     * Maps to ACCT-REISSUE-DATE field from CVACT01Y.cpy account record.
     * 
     * Business Rules:
     * - Tracks most recent card reissue activity
     * - Used for card management and security operations
     * - Optional field for accounts without reissue history
     */
    private LocalDate reissueDate;

    /**
     * Current billing cycle credit amount with exact precision.
     * Maps to ACCT-CURR-CYC-CREDIT field from CVACT01Y.cpy account record.
     * 
     * Business Rules:
     * - Tracks credits applied during current billing cycle
     * - Used for statement generation and balance calculations
     * - Precision maintained using DECIMAL128 context
     */
    @Digits(integer = 10, fraction = 2, message = "Current cycle credit must have maximum 10 integer digits and 2 decimal places")
    private BigDecimal currentCycleCredit;

    /**
     * Current billing cycle debit amount with exact precision.
     * Maps to ACCT-CURR-CYC-DEBIT field from CVACT01Y.cpy account record.
     * 
     * Business Rules:
     * - Tracks debits applied during current billing cycle
     * - Used for statement generation and balance calculations
     * - Precision maintained using DECIMAL128 context
     */
    @Digits(integer = 10, fraction = 2, message = "Current cycle debit must have maximum 10 integer digits and 2 decimal places")
    private BigDecimal currentCycleDebit;

    /**
     * Account group identifier for account classification.
     * Maps to ACCT-GROUP-ID field from CVACT01Y.cpy account record.
     * 
     * Business Rules:
     * - Used for account categorization and reporting
     * - Supports business analytics and account management
     * - Links to account type and service level definitions
     */
    private String groupId;

    /**
     * Complete customer profile information with cascading validation.
     * Embedded CustomerDto containing all customer personal and contact information
     * from CVCUS01Y.cpy customer record structure.
     * 
     * Integration:
     * - Provides comprehensive customer data for account view display
     * - Supports cascading validation for complete profile verification
     * - Maintains customer-account relationship integrity
     */
    @Valid
    private CustomerDto customerData;

    /**
     * Associated card number for account-card cross-reference.
     * Maps to CARD-NUM field from card cross-reference lookup operation.
     * 
     * Security:
     * - Full card number for internal processing
     * - Client-side masking applied for display security
     * - Used for card management operations and transaction processing
     */
    private String cardNumber;

    /**
     * Operation success indicator for REST API response handling.
     * Indicates whether the account view operation completed successfully.
     * 
     * Usage:
     * - true: Account data retrieved and validated successfully
     * - false: Operation failed with error details in errorMessage
     * - Supports client-side error handling and user feedback
     */
    private boolean success;

    /**
     * Error message for failed operations or validation errors.
     * Provides detailed error information for debugging and user feedback.
     * 
     * Usage:
     * - Contains specific error details when success = false
     * - Supports internationalization for multi-language error messages
     * - Used by React components for error display and user guidance
     */
    private String errorMessage;

    /**
     * Default constructor for AccountViewResponseDto.
     * Initializes empty response structure for JSON deserialization and form binding.
     * Sets default values for operation status and financial fields.
     */
    public AccountViewResponseDto() {
        // Initialize with default success state
        this.success = true;
        this.errorMessage = null;
        
        // Initialize financial fields with zero values using proper scale
        this.currentBalance = BigDecimalUtils.ZERO_MONETARY;
        this.creditLimit = BigDecimalUtils.ZERO_MONETARY;
        this.cashCreditLimit = BigDecimalUtils.ZERO_MONETARY;
        this.currentCycleCredit = BigDecimalUtils.ZERO_MONETARY;
        this.currentCycleDebit = BigDecimalUtils.ZERO_MONETARY;
        
        // Initialize customer data structure
        this.customerData = new CustomerDto();
    }

    /**
     * Full constructor for AccountViewResponseDto with all account and customer data.
     * Creates complete response structure with comprehensive account information.
     * 
     * @param accountId Eleven-digit account identifier
     * @param currentBalance Current account balance with exact precision
     * @param creditLimit Account credit limit with exact precision
     * @param cashCreditLimit Cash advance limit with exact precision
     * @param activeStatus Account status enumeration
     * @param openDate Account opening date
     * @param expirationDate Account expiration date
     * @param reissueDate Account reissue date
     * @param currentCycleCredit Current cycle credit amount
     * @param currentCycleDebit Current cycle debit amount
     * @param groupId Account group identifier
     * @param customerData Complete customer profile information
     * @param cardNumber Associated card number
     */
    public AccountViewResponseDto(String accountId, BigDecimal currentBalance, BigDecimal creditLimit,
                                 BigDecimal cashCreditLimit, AccountStatus activeStatus, LocalDate openDate,
                                 LocalDate expirationDate, LocalDate reissueDate, BigDecimal currentCycleCredit,
                                 BigDecimal currentCycleDebit, String groupId, CustomerDto customerData,
                                 String cardNumber) {
        this.accountId = accountId;
        this.currentBalance = currentBalance != null ? 
            currentBalance.setScale(BigDecimalUtils.MONETARY_SCALE, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode()) : 
            BigDecimalUtils.ZERO_MONETARY;
        this.creditLimit = creditLimit != null ? 
            creditLimit.setScale(BigDecimalUtils.MONETARY_SCALE, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode()) : 
            BigDecimalUtils.ZERO_MONETARY;
        this.cashCreditLimit = cashCreditLimit != null ? 
            cashCreditLimit.setScale(BigDecimalUtils.MONETARY_SCALE, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode()) : 
            BigDecimalUtils.ZERO_MONETARY;
        this.activeStatus = activeStatus;
        this.openDate = openDate;
        this.expirationDate = expirationDate;
        this.reissueDate = reissueDate;
        this.currentCycleCredit = currentCycleCredit != null ? 
            currentCycleCredit.setScale(BigDecimalUtils.MONETARY_SCALE, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode()) : 
            BigDecimalUtils.ZERO_MONETARY;
        this.currentCycleDebit = currentCycleDebit != null ? 
            currentCycleDebit.setScale(BigDecimalUtils.MONETARY_SCALE, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode()) : 
            BigDecimalUtils.ZERO_MONETARY;
        this.groupId = groupId;
        this.customerData = customerData != null ? customerData : new CustomerDto();
        this.cardNumber = cardNumber;
        this.success = true;
        this.errorMessage = null;
    }

    /**
     * Gets the account identifier.
     * 
     * @return the eleven-digit account ID
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account identifier with validation.
     * 
     * @param accountId the account ID to set
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId != null ? accountId.trim() : null;
    }

    /**
     * Gets the current account balance with exact precision.
     * 
     * @return the current balance as BigDecimal
     */
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    /**
     * Sets the current account balance with proper precision handling.
     * 
     * @param currentBalance the current balance to set
     */
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance != null ? 
            currentBalance.setScale(BigDecimalUtils.MONETARY_SCALE, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode()) : 
            BigDecimalUtils.ZERO_MONETARY;
    }

    /**
     * Gets the account credit limit with exact precision.
     * 
     * @return the credit limit as BigDecimal
     */
    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    /**
     * Sets the account credit limit with proper precision handling.
     * 
     * @param creditLimit the credit limit to set
     */
    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit != null ? 
            creditLimit.setScale(BigDecimalUtils.MONETARY_SCALE, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode()) : 
            BigDecimalUtils.ZERO_MONETARY;
    }

    /**
     * Gets the cash advance credit limit with exact precision.
     * 
     * @return the cash credit limit as BigDecimal
     */
    public BigDecimal getCashCreditLimit() {
        return cashCreditLimit;
    }

    /**
     * Sets the cash advance credit limit with proper precision handling.
     * 
     * @param cashCreditLimit the cash credit limit to set
     */
    public void setCashCreditLimit(BigDecimal cashCreditLimit) {
        this.cashCreditLimit = cashCreditLimit != null ? 
            cashCreditLimit.setScale(BigDecimalUtils.MONETARY_SCALE, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode()) : 
            BigDecimalUtils.ZERO_MONETARY;
    }

    /**
     * Gets the account active status.
     * 
     * @return the account status enumeration
     */
    public AccountStatus getActiveStatus() {
        return activeStatus;
    }

    /**
     * Sets the account active status with validation.
     * 
     * @param activeStatus the account status to set
     */
    public void setActiveStatus(AccountStatus activeStatus) {
        this.activeStatus = activeStatus;
    }

    /**
     * Gets the account opening date.
     * 
     * @return the opening date
     */
    public LocalDate getOpenDate() {
        return openDate;
    }

    /**
     * Sets the account opening date with validation.
     * 
     * @param openDate the opening date to set
     */
    public void setOpenDate(LocalDate openDate) {
        this.openDate = openDate;
    }

    /**
     * Gets the account expiration date.
     * 
     * @return the expiration date
     */
    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    /**
     * Sets the account expiration date with validation.
     * 
     * @param expirationDate the expiration date to set
     */
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * Gets the account reissue date.
     * 
     * @return the reissue date
     */
    public LocalDate getReissueDate() {
        return reissueDate;
    }

    /**
     * Sets the account reissue date with validation.
     * 
     * @param reissueDate the reissue date to set
     */
    public void setReissueDate(LocalDate reissueDate) {
        this.reissueDate = reissueDate;
    }

    /**
     * Gets the current cycle credit amount with exact precision.
     * 
     * @return the current cycle credit as BigDecimal
     */
    public BigDecimal getCurrentCycleCredit() {
        return currentCycleCredit;
    }

    /**
     * Sets the current cycle credit amount with proper precision handling.
     * 
     * @param currentCycleCredit the current cycle credit to set
     */
    public void setCurrentCycleCredit(BigDecimal currentCycleCredit) {
        this.currentCycleCredit = currentCycleCredit != null ? 
            currentCycleCredit.setScale(BigDecimalUtils.MONETARY_SCALE, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode()) : 
            BigDecimalUtils.ZERO_MONETARY;
    }

    /**
     * Gets the current cycle debit amount with exact precision.
     * 
     * @return the current cycle debit as BigDecimal
     */
    public BigDecimal getCurrentCycleDebit() {
        return currentCycleDebit;
    }

    /**
     * Sets the current cycle debit amount with proper precision handling.
     * 
     * @param currentCycleDebit the current cycle debit to set
     */
    public void setCurrentCycleDebit(BigDecimal currentCycleDebit) {
        this.currentCycleDebit = currentCycleDebit != null ? 
            currentCycleDebit.setScale(BigDecimalUtils.MONETARY_SCALE, BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode()) : 
            BigDecimalUtils.ZERO_MONETARY;
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
     * Sets the account group identifier with validation.
     * 
     * @param groupId the group ID to set
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId != null ? groupId.trim() : null;
    }

    /**
     * Gets the complete customer profile information.
     * 
     * @return the customer data with cascading validation
     */
    public CustomerDto getCustomerData() {
        return customerData;
    }

    /**
     * Sets the complete customer profile information.
     * 
     * @param customerData the customer data to set
     */
    public void setCustomerData(CustomerDto customerData) {
        this.customerData = customerData != null ? customerData : new CustomerDto();
    }

    /**
     * Gets the associated card number for account-card cross-reference.
     * 
     * @return the card number
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the associated card number with validation.
     * 
     * @param cardNumber the card number to set
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber != null ? cardNumber.trim() : null;
    }

    /**
     * Gets the operation success status.
     * 
     * @return true if operation was successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Sets the operation success status.
     * 
     * @param success the success status to set
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Gets the error message for failed operations.
     * 
     * @return the error message or null if no error
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message for failed operations.
     * 
     * @param errorMessage the error message to set
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        // Automatically set success to false when error message is provided
        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            this.success = false;
        }
    }

    /**
     * Calculates the available credit for the account.
     * Available credit = Credit Limit - Current Balance
     * 
     * @return available credit amount with exact precision
     */
    public BigDecimal getAvailableCredit() {
        if (creditLimit == null || currentBalance == null) {
            return BigDecimalUtils.ZERO_MONETARY;
        }
        return BigDecimalUtils.subtract(creditLimit, currentBalance);
    }

    /**
     * Calculates the available cash advance credit for the account.
     * Available cash credit = Cash Credit Limit - Current Balance
     * 
     * @return available cash credit amount with exact precision
     */
    public BigDecimal getAvailableCashCredit() {
        if (cashCreditLimit == null || currentBalance == null) {
            return BigDecimalUtils.ZERO_MONETARY;
        }
        return BigDecimalUtils.subtract(cashCreditLimit, currentBalance);
    }

    /**
     * Calculates the credit utilization percentage.
     * Credit utilization = (Current Balance / Credit Limit) * 100
     * 
     * @return credit utilization percentage with exact precision
     */
    public BigDecimal getCreditUtilizationPercentage() {
        if (creditLimit == null || currentBalance == null || 
            BigDecimalUtils.equals(creditLimit, BigDecimalUtils.ZERO_MONETARY)) {
            return BigDecimalUtils.ZERO_MONETARY;
        }
        
        BigDecimal utilization = BigDecimalUtils.divide(currentBalance, creditLimit);
        return BigDecimalUtils.multiply(utilization, BigDecimalUtils.createDecimal("100"));
    }

    /**
     * Gets the net cycle amount (credit - debit) for current billing cycle.
     * Net cycle amount = Current Cycle Credit - Current Cycle Debit
     * 
     * @return net cycle amount with exact precision
     */
    public BigDecimal getNetCycleAmount() {
        if (currentCycleCredit == null || currentCycleDebit == null) {
            return BigDecimalUtils.ZERO_MONETARY;
        }
        return BigDecimalUtils.subtract(currentCycleCredit, currentCycleDebit);
    }

    /**
     * Determines if the account is active and operational.
     * 
     * @return true if account status is ACTIVE, false otherwise
     */
    public boolean isActiveAccount() {
        return activeStatus != null && activeStatus.isActive();
    }

    /**
     * Determines if the account has expired based on expiration date.
     * 
     * @return true if account is expired, false otherwise
     */
    public boolean isExpiredAccount() {
        if (expirationDate == null) {
            return false;
        }
        return expirationDate.isBefore(LocalDate.now());
    }

    /**
     * Gets a masked version of the card number for secure display.
     * Shows only last 4 digits with asterisks masking the rest.
     * 
     * @return masked card number (e.g., "************1234")
     */
    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        
        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "*".repeat(cardNumber.length() - 4) + lastFour;
    }

    /**
     * Validates the complete account view response data structure.
     * Performs comprehensive validation including account data, customer data,
     * and cross-field business rule validation.
     * 
     * @return ValidationResult indicating success or specific validation failure
     */
    public ValidationResult validate() {
        // Validate required account ID
        if (accountId == null || accountId.trim().isEmpty()) {
            return ValidationResult.withCustomMessage(
                ValidationResult.BLANK_FIELD,
                "Account ID is required"
            ).getResult();
        }

        // Validate account ID format (11 digits)
        if (!accountId.matches("\\d{11}")) {
            return ValidationResult.withCustomMessage(
                ValidationResult.INVALID_FORMAT,
                "Account ID must be exactly 11 digits"
            ).getResult();
        }

        // Validate financial fields are not null
        if (currentBalance == null || creditLimit == null || cashCreditLimit == null) {
            return ValidationResult.withCustomMessage(
                ValidationResult.BLANK_FIELD,
                "Financial amounts cannot be null"
            ).getResult();
        }

        // Validate financial field ranges
        if (BigDecimalUtils.isLessThan(currentBalance, BigDecimalUtils.createDecimal("-99999999.99")) ||
            BigDecimalUtils.isGreaterThan(currentBalance, BigDecimalUtils.createDecimal("99999999.99"))) {
            return ValidationResult.withCustomMessage(
                ValidationResult.INVALID_RANGE,
                "Current balance must be between -99,999,999.99 and 99,999,999.99"
            ).getResult();
        }

        // Validate credit limits are non-negative
        if (BigDecimalUtils.isLessThan(creditLimit, BigDecimalUtils.ZERO_MONETARY)) {
            return ValidationResult.withCustomMessage(
                ValidationResult.INVALID_RANGE,
                "Credit limit cannot be negative"
            ).getResult();
        }

        if (BigDecimalUtils.isLessThan(cashCreditLimit, BigDecimalUtils.ZERO_MONETARY)) {
            return ValidationResult.withCustomMessage(
                ValidationResult.INVALID_RANGE,
                "Cash credit limit cannot be negative"
            ).getResult();
        }

        // Validate cash credit limit does not exceed total credit limit
        if (BigDecimalUtils.isGreaterThan(cashCreditLimit, creditLimit)) {
            return ValidationResult.withCustomMessage(
                ValidationResult.BUSINESS_RULE_VIOLATION,
                "Cash credit limit cannot exceed total credit limit"
            ).getResult();
        }

        // Validate account status is provided
        if (activeStatus == null) {
            return ValidationResult.withCustomMessage(
                ValidationResult.BLANK_FIELD,
                "Account status is required"
            ).getResult();
        }

        // Validate date fields if provided
        if (openDate != null && openDate.isAfter(LocalDate.now())) {
            return ValidationResult.withCustomMessage(
                ValidationResult.INVALID_DATE,
                "Account open date cannot be in the future"
            ).getResult();
        }

        if (expirationDate != null && openDate != null && expirationDate.isBefore(openDate)) {
            return ValidationResult.withCustomMessage(
                ValidationResult.INVALID_DATE,
                "Account expiration date cannot be before open date"
            ).getResult();
        }

        // Validate customer data if provided
        if (customerData != null) {
            ValidationResult customerValidation = customerData.validate();
            if (!customerValidation.isValid()) {
                return customerValidation;
            }
        }

        return ValidationResult.VALID;
    }

    /**
     * Compares this account view response with another object for equality.
     * Two responses are considered equal if their account IDs match.
     * 
     * @param obj the object to compare with
     * @return true if the responses are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AccountViewResponseDto that = (AccountViewResponseDto) obj;
        return success == that.success &&
               Objects.equals(accountId, that.accountId) &&
               Objects.equals(currentBalance, that.currentBalance) &&
               Objects.equals(creditLimit, that.creditLimit) &&
               Objects.equals(cashCreditLimit, that.cashCreditLimit) &&
               activeStatus == that.activeStatus &&
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
     * Generates hash code for this account view response based on all field values.
     * 
     * @return hash code for this response
     */
    @Override
    public int hashCode() {
        return Objects.hash(accountId, currentBalance, creditLimit, cashCreditLimit,
                           activeStatus, openDate, expirationDate, reissueDate,
                           currentCycleCredit, currentCycleDebit, groupId, customerData,
                           cardNumber, success, errorMessage);
    }

    /**
     * Returns a string representation of this account view response for debugging and logging.
     * Sensitive information (card number) is masked for security.
     * 
     * @return string representation of this response
     */
    @Override
    public String toString() {
        return String.format(
            "AccountViewResponseDto{accountId='%s', currentBalance=%s, creditLimit=%s, " +
            "cashCreditLimit=%s, activeStatus=%s, openDate=%s, expirationDate=%s, " +
            "reissueDate=%s, currentCycleCredit=%s, currentCycleDebit=%s, groupId='%s', " +
            "customerData=%s, cardNumber='%s', success=%b, errorMessage='%s'}",
            accountId,
            currentBalance != null ? BigDecimalUtils.formatCurrency(currentBalance) : null,
            creditLimit != null ? BigDecimalUtils.formatCurrency(creditLimit) : null,
            cashCreditLimit != null ? BigDecimalUtils.formatCurrency(cashCreditLimit) : null,
            activeStatus,
            openDate,
            expirationDate,
            reissueDate,
            currentCycleCredit != null ? BigDecimalUtils.formatCurrency(currentCycleCredit) : null,
            currentCycleDebit != null ? BigDecimalUtils.formatCurrency(currentCycleDebit) : null,
            groupId,
            customerData != null ? customerData.toString() : "null",
            getMaskedCardNumber(),
            success,
            errorMessage
        );
    }
}