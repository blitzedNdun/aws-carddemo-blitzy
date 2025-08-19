/*
 * AccountViewResponse.java
 * 
 * Data Transfer Object for account view response combining account details 
 * with associated customer information. Maps to COACTVW BMS screen structure 
 * containing account fields (balance, limits, dates, status) and customer 
 * profile data (names, address, phone, SSN, FICO score).
 * 
 * Supports the account view functionality from COACTVWC.cbl program which:
 * 1. Accepts account ID from COACTVW BMS map
 * 2. Reads CARDXREF by account to get customer ID  
 * 3. Reads ACCTDAT by account ID for account details
 * 4. Reads CUSTDAT by customer ID for customer details
 * 5. Returns combined account and customer data to the screen
 * 
 * This response DTO provides a complete view of account information needed
 * for the CAVW transaction, maintaining exact field structure and precision 
 * as the original COBOL implementation.
 */

package com.carddemo.dto;

import com.carddemo.dto.AccountDto;
import com.carddemo.dto.CustomerDto;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Account View Response DTO
 * 
 * Comprehensive response object for account view operations that combines
 * account financial data with customer personal information, matching the
 * data structure displayed on COACTVW BMS screen.
 * 
 * Contains:
 * - Complete account information (ID, status, balances, dates, limits)
 * - Complete customer information (personal details, address, contact info)
 * - Response metadata (success indicators, error/info messages)
 * - Convenience methods for accessing nested data
 * 
 * Used by:
 * - CAVW transaction REST endpoint response
 * - Account view service layer operations
 * - Frontend React components for account display
 * 
 * Field access patterns match original COBOL COACTVWC.cbl program flow
 * and preserve exact data types and precision from mainframe implementation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountViewResponse {

    // ============ CORE DATA OBJECTS ============
    
    /**
     * Complete account information including financial data.
     * Contains account ID, status, balances, credit limits, dates, and cycles.
     * Maps to account fields displayed on COACTVW BMS screen.
     */
    private AccountDto accountData;
    
    /**
     * Complete customer information including personal and contact data.
     * Contains customer ID, names, address, phone numbers, SSN, DOB, FICO score.
     * Maps to customer fields displayed on COACTVW BMS screen.
     */
    private CustomerDto customerData;
    
    // ============ RESPONSE METADATA ============
    
    /**
     * Success indicator for the operation.
     * True if account and customer data were successfully retrieved and populated.
     */
    private boolean successful;
    
    /**
     * Error message if operation failed.
     * Contains descriptive error information for troubleshooting.
     * Maps to ERRMSG field on COACTVW BMS screen.
     */
    private String errorMessage;
    
    /**
     * Informational message for user guidance.
     * Contains status or instructional information.
     * Maps to INFOMSG field on COACTVW BMS screen.
     */
    private String infoMessage;
    
    // ============ CONSTRUCTORS ============
    
    /**
     * Default constructor for empty response.
     */
    public AccountViewResponse() {
        this.successful = false;
    }
    
    /**
     * Constructor for successful response with account and customer data.
     * 
     * @param accountData Complete account information
     * @param customerData Complete customer information
     */
    public AccountViewResponse(AccountDto accountData, CustomerDto customerData) {
        this.accountData = accountData;
        this.customerData = customerData;
        this.successful = true;
    }
    
    /**
     * Constructor for error response.
     * 
     * @param errorMessage Error description
     */
    public AccountViewResponse(String errorMessage) {
        this.successful = false;
        this.errorMessage = errorMessage;
    }
    
    // ============ CONVENIENCE ACCESSOR METHODS ============
    
    /**
     * Gets account ID from embedded account data.
     * Convenience method that delegates to AccountDto.getAccountId().
     * 
     * @return Account ID if account data is present, null otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getAccountId() {
        return accountData != null ? accountData.getAccountId() : null;
    }
    
    /**
     * Gets customer ID from embedded customer data.
     * Convenience method that delegates to CustomerDto.getCustomerId().
     * 
     * @return Customer ID if customer data is present, null otherwise  
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getCustomerId() {
        return customerData != null ? customerData.getCustomerId() : null;
    }
    
    // ============ ACCOUNT DATA CONVENIENCE METHODS ============
    
    /**
     * Gets account active status from embedded account data.
     * Convenience method that delegates to AccountDto.getActiveStatus().
     * 
     * @return Account active status ('Y'/'N') if account data is present, null otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getAccountActiveStatus() {
        return accountData != null ? accountData.getActiveStatus() : null;
    }
    
    /**
     * Gets current balance from embedded account data.
     * Convenience method that delegates to AccountDto.getCurrentBalance().
     * 
     * @return Current balance if account data is present, null otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public java.math.BigDecimal getCurrentBalance() {
        return accountData != null ? accountData.getCurrentBalance() : null;
    }
    
    /**
     * Gets credit limit from embedded account data.
     * Convenience method that delegates to AccountDto.getCreditLimit().
     * 
     * @return Credit limit if account data is present, null otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public java.math.BigDecimal getCreditLimit() {
        return accountData != null ? accountData.getCreditLimit() : null;
    }
    
    /**
     * Gets cash credit limit from embedded account data.
     * Convenience method that delegates to AccountDto.getCashCreditLimit().
     * 
     * @return Cash credit limit if account data is present, null otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public java.math.BigDecimal getCashCreditLimit() {
        return accountData != null ? accountData.getCashCreditLimit() : null;
    }
    
    /**
     * Gets account open date from embedded account data.
     * Convenience method that delegates to AccountDto.getOpenDate().
     * 
     * @return Account open date if account data is present, null otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public java.time.LocalDate getOpenDate() {
        return accountData != null ? accountData.getOpenDate() : null;
    }
    
    /**
     * Gets account expiration date from embedded account data.
     * Convenience method that delegates to AccountDto.getExpirationDate().
     * 
     * @return Account expiration date if account data is present, null otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public java.time.LocalDate getExpirationDate() {
        return accountData != null ? accountData.getExpirationDate() : null;
    }
    
    /**
     * Gets account reissue date from embedded account data.
     * Convenience method that delegates to AccountDto.getReissueDate().
     * 
     * @return Account reissue date if account data is present, null otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public java.time.LocalDate getReissueDate() {
        return accountData != null ? accountData.getReissueDate() : null;
    }
    
    /**
     * Gets current cycle credit from embedded account data.
     * Convenience method that delegates to AccountDto.getCurrentCycleCredit().
     * 
     * @return Current cycle credit if account data is present, null otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public java.math.BigDecimal getCurrentCycleCredit() {
        return accountData != null ? accountData.getCurrentCycleCredit() : null;
    }
    
    /**
     * Gets current cycle debit from embedded account data.
     * Convenience method that delegates to AccountDto.getCurrentCycleDebit().
     * 
     * @return Current cycle debit if account data is present, null otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public java.math.BigDecimal getCurrentCycleDebit() {
        return accountData != null ? accountData.getCurrentCycleDebit() : null;
    }
    
    /**
     * Gets account group ID from embedded account data.
     * Convenience method that delegates to AccountDto.getGroupId().
     * Note: AccountDto uses getAccountGroupId() for this field.
     * 
     * @return Account group ID if account data is present, null otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getGroupId() {
        return accountData != null ? accountData.getAccountGroupId() : null;
    }
    
    // ============ CUSTOMER DATA CONVENIENCE METHODS ============
    
    /**
     * Gets customer first name from embedded customer data.
     * Convenience method that delegates to CustomerDto.getFirstName().
     * 
     * @return Customer first name if customer data is present, null otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getFirstName() {
        return customerData != null ? customerData.getFirstName() : null;
    }
    
    /**
     * Gets customer middle name from embedded customer data.
     * Convenience method that delegates to CustomerDto.getMiddleName().
     * 
     * @return Customer middle name if customer data is present, null otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getMiddleName() {
        return customerData != null ? customerData.getMiddleName() : null;
    }
    
    /**
     * Gets customer last name from embedded customer data.
     * Convenience method that delegates to CustomerDto.getLastName().
     * 
     * @return Customer last name if customer data is present, null otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getLastName() {
        return customerData != null ? customerData.getLastName() : null;
    }
    
    /**
     * Gets customer address from embedded customer data.
     * Convenience method that delegates to CustomerDto.getAddress().
     * 
     * @return Customer address DTO if customer data is present, null otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public com.carddemo.dto.AddressDto getAddress() {
        return customerData != null ? customerData.getAddress() : null;
    }
    
    /**
     * Gets customer primary phone number from embedded customer data.
     * Convenience method that delegates to CustomerDto.getPhoneNumber1().
     * 
     * @return Customer phone number 1 if customer data is present, null otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getPhoneNumber1() {
        return customerData != null ? customerData.getPhoneNumber1() : null;
    }
    
    /**
     * Gets customer secondary phone number from embedded customer data.
     * Convenience method that delegates to CustomerDto.getPhoneNumber2().
     * 
     * @return Customer phone number 2 if customer data is present, null otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getPhoneNumber2() {
        return customerData != null ? customerData.getPhoneNumber2() : null;
    }
    
    /**
     * Gets customer SSN from embedded customer data.
     * Convenience method that delegates to CustomerDto.getSsn().
     * 
     * @return Customer SSN if customer data is present, null otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getSsn() {
        return customerData != null ? customerData.getSsn() : null;
    }
    
    /**
     * Gets customer government ID from embedded customer data.
     * Convenience method that delegates to CustomerDto.getGovernmentId().
     * 
     * @return Customer government ID if customer data is present, null otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getGovernmentId() {
        return customerData != null ? customerData.getGovernmentId() : null;
    }
    
    /**
     * Gets customer date of birth from embedded customer data.
     * Convenience method that delegates to CustomerDto.getDateOfBirth().
     * 
     * @return Customer date of birth if customer data is present, null otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public java.time.LocalDate getDateOfBirth() {
        return customerData != null ? customerData.getDateOfBirth() : null;
    }
    
    /**
     * Gets customer EFT account ID from embedded customer data.
     * Convenience method that delegates to CustomerDto.getEftAccountId().
     * 
     * @return Customer EFT account ID if customer data is present, null otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getEftAccountId() {
        return customerData != null ? customerData.getEftAccountId() : null;
    }
    
    /**
     * Gets customer primary cardholder indicator from embedded customer data.
     * Convenience method that delegates to CustomerDto.getPrimaryCardholderIndicator().
     * 
     * @return Customer primary cardholder indicator ('Y'/'N') if customer data is present, null otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getPrimaryCardholderIndicator() {
        return customerData != null ? customerData.getPrimaryCardholderIndicator() : null;
    }
    
    /**
     * Gets customer FICO score from embedded customer data.
     * Convenience method that delegates to CustomerDto.getFicoScore().
     * 
     * @return Customer FICO score if customer data is present, null otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public Integer getFicoScore() {
        return customerData != null ? customerData.getFicoScore() : null;
    }
    
    // ============ RESPONSE METADATA ACCESSORS ============
    
    /**
     * Checks if the operation was successful.
     * 
     * @return true if account view operation completed successfully
     */
    public boolean isSuccessful() {
        return successful;
    }
    
    /**
     * Sets the success status of the operation.
     * 
     * @param successful true if operation completed successfully
     */
    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
    
    /**
     * Gets the error message if operation failed.
     * 
     * @return Error message string or null if no error
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Sets an error message and marks operation as unsuccessful.
     * 
     * @param errorMessage Descriptive error message
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.successful = false;
    }
    
    /**
     * Gets the informational message.
     * 
     * @return Info message string or null if no message
     */
    public String getInfoMessage() {
        return infoMessage;
    }
    
    /**
     * Sets an informational message.
     * 
     * @param infoMessage Descriptive info message
     */
    public void setInfoMessage(String infoMessage) {
        this.infoMessage = infoMessage;
    }
    
    // ============ DATA OBJECT ACCESSORS ============
    
    /**
     * Gets the complete account data object.
     * 
     * @return AccountDto containing all account information
     */
    public AccountDto getAccountData() {
        return accountData;
    }
    
    /**
     * Sets the complete account data object.
     * 
     * @param accountData AccountDto containing account information
     */
    public void setAccountData(AccountDto accountData) {
        this.accountData = accountData;
    }
    
    /**
     * Gets the complete customer data object.
     * 
     * @return CustomerDto containing all customer information
     */
    public CustomerDto getCustomerData() {
        return customerData;
    }
    
    /**
     * Sets the complete customer data object.
     * 
     * @param customerData CustomerDto containing customer information
     */
    public void setCustomerData(CustomerDto customerData) {
        this.customerData = customerData;
    }
    
    // ============ UTILITY METHODS ============
    
    /**
     * Creates a successful response with both account and customer data.
     * 
     * @param accountData Complete account information
     * @param customerData Complete customer information
     * @return AccountViewResponse marked as successful
     */
    public static AccountViewResponse success(AccountDto accountData, CustomerDto customerData) {
        return new AccountViewResponse(accountData, customerData);
    }
    
    /**
     * Creates a successful response with account data only.
     * 
     * @param accountData Complete account information
     * @return AccountViewResponse marked as successful with account data
     */
    public static AccountViewResponse success(AccountDto accountData) {
        return new AccountViewResponse(accountData, null);
    }
    
    /**
     * Creates an error response with error message.
     * 
     * @param errorMessage Descriptive error message
     * @return AccountViewResponse marked as unsuccessful with error
     */
    public static AccountViewResponse error(String errorMessage) {
        return new AccountViewResponse(errorMessage);
    }
    
    /**
     * Creates a response with informational message.
     * 
     * @param infoMessage Descriptive informational message
     * @return AccountViewResponse with info message
     */
    public static AccountViewResponse info(String infoMessage) {
        AccountViewResponse response = new AccountViewResponse();
        response.setInfoMessage(infoMessage);
        return response;
    }
    
    /**
     * Validates that response contains required data for successful operation.
     * 
     * @return true if response has valid account data for display
     */
    public boolean hasValidData() {
        return successful && accountData != null && accountData.getAccountId() != null;
    }
    
    /**
     * Gets a summary description of the response status.
     * 
     * @return Status summary string
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getStatusSummary() {
        if (!successful && errorMessage != null) {
            return "Error: " + errorMessage;
        } else if (successful && hasValidData()) {
            return "Success: Account " + getAccountId() + " retrieved";
        } else if (infoMessage != null) {
            return "Info: " + infoMessage;
        } else {
            return "Unknown status";
        }
    }
}