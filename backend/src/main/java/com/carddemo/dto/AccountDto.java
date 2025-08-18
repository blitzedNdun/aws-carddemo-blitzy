/*
 * AccountDto.java
 * 
 * Data Transfer Object for account view operations (CAVW transaction).
 * Combines account and customer information for comprehensive account display.
 * 
 * Maps to combined data from COACTVWC.cbl functionality which performs:
 * 1. Read CARDXREF by account ID to get customer ID
 * 2. Read ACCTDAT by account ID for account details  
 * 3. Read CUSTDAT by customer ID for customer details
 * 4. Return combined account and customer information
 * 
 * Maintains identical field structure and precision as COBOL implementation
 * with BigDecimal scale(2) for all monetary amounts matching COMP-3 behavior.
 */

package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Account Data Transfer Object
 * 
 * Provides comprehensive account view combining account and customer information.
 * Used for CAVW transaction response and account update operations response.
 * 
 * Contains account financial data with exact COBOL COMP-3 precision and
 * customer personal information for complete account management view.
 * 
 * Key components:
 * - Account identification and status
 * - Balance and credit limit information with BigDecimal precision  
 * - Customer personal and contact information
 * - Date information for account lifecycle management
 * - Utility fields for business logic (available credit, account status)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {

    // ============ ACCOUNT INFORMATION ============
    
    /**
     * Account ID (11-digit primary key).
     * Maps to ACCT-ID from CVACT01Y.cpy (PIC 9(11)).
     */
    @JsonProperty("accountId")
    private String accountId;

    /**
     * Account active status ('Y' = active, 'N' = inactive).
     * Maps to ACCT-ACTIVE-STATUS from CVACT01Y.cpy (PIC X(01)).
     */
    @JsonProperty("activeStatus")
    private String activeStatus;

    /**
     * Current account balance with 2 decimal places.
     * Maps to ACCT-CURR-BAL from CVACT01Y.cpy (PIC S9(10)V99 COMP-3).
     * Uses BigDecimal to preserve exact COBOL COMP-3 precision.
     */
    @JsonProperty("currentBalance")
    private BigDecimal currentBalance;

    /**
     * Credit limit with 2 decimal places.
     * Maps to ACCT-CREDIT-LIMIT from CVACT01Y.cpy (PIC S9(10)V99 COMP-3).
     */
    @JsonProperty("creditLimit")
    private BigDecimal creditLimit;

    /**
     * Cash advance credit limit with 2 decimal places.
     * Maps to ACCT-CASH-CREDIT-LIMIT from CVACT01Y.cpy (PIC S9(10)V99 COMP-3).
     */
    @JsonProperty("cashCreditLimit")
    private BigDecimal cashCreditLimit;

    /**
     * Account opening date.
     * Maps to ACCT-OPEN-DATE from CVACT01Y.cpy (PIC X(10)).
     */
    @JsonProperty("openDate")
    private LocalDate openDate;

    /**
     * Account expiration date.
     * Maps to ACCT-EXPIRAION-DATE from CVACT01Y.cpy (PIC X(10)).
     */
    @JsonProperty("expirationDate")
    private LocalDate expirationDate;

    /**
     * Account reissue date (date of last card reissue).
     * Maps to ACCT-REISSUE-DATE from CVACT01Y.cpy (PIC X(10)).
     */
    @JsonProperty("reissueDate")
    private LocalDate reissueDate;

    /**
     * Current cycle credit total with 2 decimal places.
     * Maps to ACCT-CURR-CYC-CREDIT from CVACT01Y.cpy (PIC S9(10)V99 COMP-3).
     */
    @JsonProperty("currentCycleCredit")
    private BigDecimal currentCycleCredit;

    /**
     * Current cycle debit total with 2 decimal places.
     * Maps to ACCT-CURR-CYC-DEBIT from CVACT01Y.cpy (PIC S9(10)V99 COMP-3).
     */
    @JsonProperty("currentCycleDebit")
    private BigDecimal currentCycleDebit;

    /**
     * Address ZIP code (5-digit).
     * Maps to ACCT-ADDR-ZIP from CVACT01Y.cpy (PIC 9(05)).
     */
    @JsonProperty("addressZip")
    private String addressZip;

    /**
     * Account group ID for disclosure group assignment.
     * Maps to ACCT-GROUP-ID from CVACT01Y.cpy (PIC X(10)).
     */
    @JsonProperty("accountGroupId")
    private String accountGroupId;

    // ============ CUSTOMER INFORMATION ============

    /**
     * Customer ID (9-digit foreign key).
     * Maps to CUST-ID from CVCUS01Y.cpy (PIC 9(09)).
     */
    @JsonProperty("customerId")
    private String customerId;

    /**
     * Customer first name.
     * Maps to CUST-FIRST-NAME from CVCUS01Y.cpy (PIC X(20)).
     */
    @JsonProperty("customerFirstName")
    private String customerFirstName;

    /**
     * Customer middle name.
     * Maps to CUST-MIDDLE-NAME from CVCUS01Y.cpy (PIC X(20)).
     */
    @JsonProperty("customerMiddleName")
    private String customerMiddleName;

    /**
     * Customer last name.
     * Maps to CUST-LAST-NAME from CVCUS01Y.cpy (PIC X(20)).
     */
    @JsonProperty("customerLastName")
    private String customerLastName;

    /**
     * Customer address line 1.
     * Maps to CUST-ADDR-LINE-1 from CVCUS01Y.cpy (PIC X(50)).
     */
    @JsonProperty("customerAddressLine1")
    private String customerAddressLine1;

    /**
     * Customer address line 2.
     * Maps to CUST-ADDR-LINE-2 from CVCUS01Y.cpy (PIC X(50)).
     */
    @JsonProperty("customerAddressLine2")
    private String customerAddressLine2;

    /**
     * Customer address line 3.
     * Maps to CUST-ADDR-LINE-3 from CVCUS01Y.cpy (PIC X(50)).
     */
    @JsonProperty("customerAddressLine3")
    private String customerAddressLine3;

    /**
     * Customer state code.
     * Maps to CUST-ADDR-STATE-CD from CVCUS01Y.cpy (PIC X(02)).
     */
    @JsonProperty("customerStateCode")
    private String customerStateCode;

    /**
     * Customer country code.
     * Maps to CUST-ADDR-COUNTRY-CD from CVCUS01Y.cpy (PIC X(03)).
     */
    @JsonProperty("customerCountryCode")
    private String customerCountryCode;

    /**
     * Customer ZIP code.
     * Maps to CUST-ADDR-ZIP from CVCUS01Y.cpy (PIC X(10)).
     */
    @JsonProperty("customerZipCode")
    private String customerZipCode;

    /**
     * Customer primary phone number.
     * Maps to CUST-PHONE-NUM-1 from CVCUS01Y.cpy (PIC X(15)).
     */
    @JsonProperty("customerPhoneNumber1")
    private String customerPhoneNumber1;

    /**
     * Customer secondary phone number.
     * Maps to CUST-PHONE-NUM-2 from CVCUS01Y.cpy (PIC X(15)).
     */
    @JsonProperty("customerPhoneNumber2")
    private String customerPhoneNumber2;

    /**
     * Customer date of birth.
     * Maps to CUST-DOB-YYYYMMDD from CVCUS01Y.cpy (PIC X(08)).
     */
    @JsonProperty("customerDateOfBirth")
    private LocalDate customerDateOfBirth;

    /**
     * Customer EFT account ID.
     * Maps to CUST-EFT-ACCOUNT-ID from CVCUS01Y.cpy (PIC X(10)).
     */
    @JsonProperty("customerEftAccountId")
    private String customerEftAccountId;

    /**
     * Customer FICO credit score.
     * Maps to CUST-FICO-CREDIT-SCORE from CVCUS01Y.cpy (PIC 9(03)).
     */
    @JsonProperty("customerFicoScore")
    private Integer customerFicoScore;

    // ============ CALCULATED FIELDS ============

    /**
     * Available credit (calculated field).
     * Credit limit minus current balance, minimum zero.
     */
    @JsonProperty("availableCredit")
    private BigDecimal availableCredit;

    /**
     * Available cash credit (calculated field).
     * Cash credit limit minus current balance, minimum zero.
     */
    @JsonProperty("availableCashCredit")
    private BigDecimal availableCashCredit;

    /**
     * Account active indicator (calculated field).
     * True if activeStatus is 'Y', false otherwise.
     */
    @JsonProperty("isActive")
    private Boolean isActive;

    /**
     * Account expired indicator (calculated field).
     * True if expiration date is before current date.
     */
    @JsonProperty("isExpired")
    private Boolean isExpired;

    /**
     * Customer full name (calculated field).
     * Concatenated first, middle (if present), and last name.
     */
    @JsonProperty("customerFullName")
    private String customerFullName;

    // ============ CONSTRUCTORS ============

    /**
     * Constructor for account-only data (without customer information).
     * Used when customer data is not needed or available.
     */
    public AccountDto(String accountId, String activeStatus, BigDecimal currentBalance,
                     BigDecimal creditLimit, BigDecimal cashCreditLimit, LocalDate openDate,
                     LocalDate expirationDate, String customerId) {
        this.accountId = accountId;
        this.activeStatus = activeStatus;
        this.currentBalance = currentBalance;
        this.creditLimit = creditLimit;
        this.cashCreditLimit = cashCreditLimit;
        this.openDate = openDate;
        this.expirationDate = expirationDate;
        this.customerId = customerId;
        
        // Calculate derived fields
        this.calculateDerivedFields();
    }

    // ============ UTILITY METHODS ============

    /**
     * Calculate derived/computed fields based on base data.
     * Called after setting base fields to ensure consistency.
     */
    public void calculateDerivedFields() {
        // Available credit calculation
        if (creditLimit != null && currentBalance != null) {
            this.availableCredit = creditLimit.subtract(currentBalance).max(BigDecimal.ZERO);
        }
        
        // Available cash credit calculation
        if (cashCreditLimit != null && currentBalance != null) {
            this.availableCashCredit = cashCreditLimit.subtract(currentBalance).max(BigDecimal.ZERO);
        }
        
        // Account status flags
        this.isActive = "Y".equals(activeStatus);
        this.isExpired = expirationDate != null && expirationDate.isBefore(LocalDate.now());
        
        // Customer full name
        if (customerFirstName != null || customerLastName != null) {
            StringBuilder fullName = new StringBuilder();
            if (customerFirstName != null) {
                fullName.append(customerFirstName);
            }
            if (customerMiddleName != null && !customerMiddleName.trim().isEmpty()) {
                if (fullName.length() > 0) fullName.append(" ");
                fullName.append(customerMiddleName);
            }
            if (customerLastName != null) {
                if (fullName.length() > 0) fullName.append(" ");
                fullName.append(customerLastName);
            }
            this.customerFullName = fullName.toString();
        }
    }

    /**
     * Set current balance and recalculate derived fields.
     */
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
        calculateDerivedFields();
    }

    /**
     * Set credit limit and recalculate derived fields.
     */
    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
        calculateDerivedFields();
    }

    /**
     * Set cash credit limit and recalculate derived fields.
     */
    public void setCashCreditLimit(BigDecimal cashCreditLimit) {
        this.cashCreditLimit = cashCreditLimit;
        calculateDerivedFields();
    }

    /**
     * Set active status and recalculate derived fields.
     */
    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
        calculateDerivedFields();
    }

    /**
     * Set expiration date and recalculate derived fields.
     */
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
        calculateDerivedFields();
    }

    /**
     * Set customer name fields and recalculate full name.
     */
    public void setCustomerFirstName(String customerFirstName) {
        this.customerFirstName = customerFirstName;
        calculateDerivedFields();
    }

    public void setCustomerMiddleName(String customerMiddleName) {
        this.customerMiddleName = customerMiddleName;
        calculateDerivedFields();
    }

    public void setCustomerLastName(String customerLastName) {
        this.customerLastName = customerLastName;
        calculateDerivedFields();
    }
}