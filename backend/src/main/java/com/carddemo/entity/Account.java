/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * JPA entity representing credit card account data, mapped to account_data PostgreSQL table.
 * Corresponds to ACCOUNT-RECORD structure from CVACT01Y.cpy copybook.
 * 
 * Contains account identification, balance information, credit limits, important dates, 
 * and cycle totals. Uses BigDecimal for monetary fields to preserve COBOL COMP-3 packed 
 * decimal precision. Implements relationships with Customer and DisclosureGroup entities 
 * through foreign keys.
 * 
 * Field mappings from COBOL copybook CVACT01Y.cpy:
 * - ACCT-ID (PIC 9(11)) → accountId (BIGINT)
 * - ACCT-ACTIVE-STATUS (PIC X(01)) → activeStatus (CHAR(1))
 * - ACCT-CURR-BAL (PIC S9(10)V99) → currentBalance (NUMERIC(12,2))
 * - ACCT-CREDIT-LIMIT (PIC S9(10)V99) → creditLimit (NUMERIC(12,2))
 * - ACCT-CASH-CREDIT-LIMIT (PIC S9(10)V99) → cashCreditLimit (NUMERIC(12,2))
 * - ACCT-OPEN-DATE (PIC X(10)) → openDate (DATE)
 * - ACCT-EXPIRAION-DATE (PIC X(10)) → expirationDate (DATE)
 * - ACCT-REISSUE-DATE (PIC X(10)) → reissueDate (DATE)
 * - ACCT-CURR-CYC-CREDIT (PIC S9(10)V99) → currentCycleCredit (NUMERIC(12,2))
 * - ACCT-CURR-CYC-DEBIT (PIC S9(10)V99) → currentCycleDebit (NUMERIC(12,2))
 * - ACCT-ADDR-ZIP (PIC X(10)) → addressZip (VARCHAR(10))
 * - ACCT-GROUP-ID (PIC X(10)) → groupId (VARCHAR(10))
 * 
 * Relationships:
 * - @ManyToOne with Customer entity (customer_id foreign key)
 * - @ManyToOne with DisclosureGroup entity (disclosure_group_id foreign key)
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Entity
@Table(name = "account_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    // Constants for field constraints (matching COBOL PIC clauses)
    private static final int ACCOUNT_ID_MAX_DIGITS = 11;
    private static final String MAX_MONETARY_VALUE = "9999999999.99"; // PIC S9(10)V99 max value
    private static final int ZIP_CODE_LENGTH = 10;
    private static final int GROUP_ID_LENGTH = 10;
    
    /**
     * Account ID - Primary key.
     * Maps to ACCT-ID field from COBOL copybook (PIC 9(11)).
     * Unique identifier for each account in the system.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    
    /**
     * Account active status indicator.
     * Maps to ACCT-ACTIVE-STATUS field from COBOL copybook (PIC X(01)).
     * Indicates whether the account is active ('Y') or inactive ('N').
     */
    @Column(name = "active_status", length = 1, nullable = false)
    @NotNull(message = "Active status is required")
    @Size(max = 1, message = "Active status cannot exceed 1 character")
    private String activeStatus;
    
    /**
     * Current account balance.
     * Maps to ACCT-CURR-BAL field from COBOL copybook (PIC S9(10)V99).
     * Uses BigDecimal with scale=2 to preserve COBOL COMP-3 packed decimal precision.
     * Supports positive and negative balances with exact monetary calculations.
     */
    @Column(name = "current_balance", nullable = false, precision = 12, scale = 2)
    @NotNull(message = "Current balance is required")
    @DecimalMax(value = MAX_MONETARY_VALUE, message = "Current balance cannot exceed " + MAX_MONETARY_VALUE)
    private BigDecimal currentBalance;
    
    /**
     * Account credit limit.
     * Maps to ACCT-CREDIT-LIMIT field from COBOL copybook (PIC S9(10)V99).
     * Maximum credit available for purchases and transactions.
     * Uses BigDecimal with scale=2 for exact precision matching COBOL COMP-3.
     */
    @Column(name = "credit_limit", nullable = false, precision = 12, scale = 2)
    @NotNull(message = "Credit limit is required")  
    @DecimalMax(value = MAX_MONETARY_VALUE, message = "Credit limit cannot exceed " + MAX_MONETARY_VALUE)
    private BigDecimal creditLimit;
    
    /**
     * Cash credit limit.
     * Maps to ACCT-CASH-CREDIT-LIMIT field from COBOL copybook (PIC S9(10)V99).
     * Maximum cash advance available on the account.
     * Uses BigDecimal with scale=2 for exact precision matching COBOL COMP-3.
     */
    @Column(name = "cash_credit_limit", nullable = false, precision = 12, scale = 2)
    @NotNull(message = "Cash credit limit is required")
    @DecimalMax(value = MAX_MONETARY_VALUE, message = "Cash credit limit cannot exceed " + MAX_MONETARY_VALUE)
    private BigDecimal cashCreditLimit;
    
    /**
     * Account open date.
     * Maps to ACCT-OPEN-DATE field from COBOL copybook (PIC X(10)).
     * Date when the account was originally opened.
     */
    @Column(name = "open_date", nullable = false)
    @NotNull(message = "Open date is required")
    private LocalDate openDate;
    
    /**
     * Account expiration date.
     * Maps to ACCT-EXPIRAION-DATE field from COBOL copybook (PIC X(10)).
     * Date when the account expires and becomes inactive.
     */
    @Column(name = "expiration_date")
    private LocalDate expirationDate;
    
    /**
     * Account reissue date.
     * Maps to ACCT-REISSUE-DATE field from COBOL copybook (PIC X(10)).
     * Date when the account was last reissued (e.g., new card issued).
     */
    @Column(name = "reissue_date")
    private LocalDate reissueDate;
    
    /**
     * Last transaction date.
     * Date of the most recent transaction activity on the account.
     * Used for dormancy analysis and account maintenance operations.
     */
    @Column(name = "last_transaction_date")
    private LocalDate lastTransactionDate;
    
    /**
     * Current cycle credit amount.
     * Maps to ACCT-CURR-CYC-CREDIT field from COBOL copybook (PIC S9(10)V99).
     * Total credits applied to the account in the current billing cycle.
     * Uses BigDecimal with scale=2 for exact precision matching COBOL COMP-3.
     */
    @Column(name = "current_cycle_credit", nullable = false, precision = 12, scale = 2)
    @NotNull(message = "Current cycle credit is required")
    @DecimalMax(value = MAX_MONETARY_VALUE, message = "Current cycle credit cannot exceed " + MAX_MONETARY_VALUE)
    private BigDecimal currentCycleCredit;
    
    /**
     * Current cycle debit amount.
     * Maps to ACCT-CURR-CYC-DEBIT field from COBOL copybook (PIC S9(10)V99).
     * Total debits applied to the account in the current billing cycle.
     * Uses BigDecimal with scale=2 for exact precision matching COBOL COMP-3.
     */
    @Column(name = "current_cycle_debit", nullable = false, precision = 12, scale = 2)
    @NotNull(message = "Current cycle debit is required")
    @DecimalMax(value = MAX_MONETARY_VALUE, message = "Current cycle debit cannot exceed " + MAX_MONETARY_VALUE)
    private BigDecimal currentCycleDebit;
    
    /**
     * Account ZIP code.
     * Maps to ACCT-ADDR-ZIP field from COBOL copybook (PIC X(10)).
     * ZIP code associated with the account for billing address purposes.
     */
    @Column(name = "zip_code", length = ZIP_CODE_LENGTH)
    @Size(max = ZIP_CODE_LENGTH, message = "ZIP code cannot exceed " + ZIP_CODE_LENGTH + " characters")
    private String addressZip;
    
    /**
     * Account group ID.
     * Maps to ACCT-GROUP-ID field from COBOL copybook (PIC X(10)).
     * Groups accounts for processing, reporting, and configuration purposes.
     */
    @Column(name = "group_id", length = GROUP_ID_LENGTH)
    @Size(max = GROUP_ID_LENGTH, message = "Group ID cannot exceed " + GROUP_ID_LENGTH + " characters")
    private String groupId;
    
    /**
     * Customer relationship.
     * @ManyToOne relationship with Customer entity using customer_id foreign key.
     * Links account to customer profile for account ownership and management.
     */
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    @NotNull(message = "Customer is required")
    private Customer customer;
    
    /**
     * Disclosure Group relationship.
     * @ManyToOne relationship with DisclosureGroup entity using disclosure_group_id foreign key.
     * Links account to appropriate disclosure group for interest rate determination and compliance.
     */
    @ManyToOne  
    @JoinColumn(name = "disclosure_group_id")
    private DisclosureGroup disclosureGroup;
    
    /**
     * JPA lifecycle callback for validation before persisting a new account.
     * Performs comprehensive field validation using COBOL-equivalent validation rules.
     * Initializes default values and ensures data consistency.
     */
    @PrePersist
    public void validateBeforeInsert() {
        performAccountValidation();
        initializeDefaults();
        formatFields();
    }
    
    /**
     * JPA lifecycle callback for validation before updating an existing account.
     * Performs comprehensive field validation using COBOL-equivalent validation rules.
     * Ensures data consistency and business rule compliance.
     */
    @PreUpdate
    public void validateBeforeUpdate() {
        performAccountValidation();
        formatFields();
    }
    
    /**
     * Performs comprehensive account field validation using business rules.
     * Validates monetary amounts, dates, status codes, and field relationships.
     * Ensures data integrity matching COBOL validation patterns.
     */
    private void performAccountValidation() {
        // Validate active status (must be 'Y' or 'N')
        if (activeStatus != null && !activeStatus.equals("Y") && !activeStatus.equals("N")) {
            throw new IllegalArgumentException("Active status must be 'Y' or 'N'");
        }
        
        // Validate monetary amounts are not negative (except current balance which can be negative)
        if (creditLimit != null && creditLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Credit limit cannot be negative");
        }
        
        if (cashCreditLimit != null && cashCreditLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cash credit limit cannot be negative");
        }
        
        if (currentCycleCredit != null && currentCycleCredit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Current cycle credit cannot be negative");
        }
        
        if (currentCycleDebit != null && currentCycleDebit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Current cycle debit cannot be negative");
        }
        
        // Validate cash credit limit does not exceed credit limit
        if (creditLimit != null && cashCreditLimit != null && 
            cashCreditLimit.compareTo(creditLimit) > 0) {
            throw new IllegalArgumentException("Cash credit limit cannot exceed credit limit");
        }
        
        // Validate dates are logical (open date before expiration and reissue dates)
        if (openDate != null && expirationDate != null && openDate.isAfter(expirationDate)) {
            throw new IllegalArgumentException("Open date cannot be after expiration date");
        }
        
        if (openDate != null && reissueDate != null && openDate.isAfter(reissueDate)) {
            throw new IllegalArgumentException("Open date cannot be after reissue date");
        }
    }
    
    /**
     * Initializes default values for new account records.
     * Sets COBOL-compatible defaults matching mainframe initialization patterns.
     */
    private void initializeDefaults() {
        if (activeStatus == null) {
            activeStatus = "Y"; // Default to active
        }
        
        if (currentBalance == null) {
            currentBalance = BigDecimal.ZERO.setScale(2);
        }
        
        if (creditLimit == null) {
            creditLimit = BigDecimal.ZERO.setScale(2);
        }
        
        if (cashCreditLimit == null) {
            cashCreditLimit = BigDecimal.ZERO.setScale(2);
        }
        
        if (currentCycleCredit == null) {
            currentCycleCredit = BigDecimal.ZERO.setScale(2);
        }
        
        if (currentCycleDebit == null) {
            currentCycleDebit = BigDecimal.ZERO.setScale(2);
        }
        
        if (openDate == null) {
            openDate = LocalDate.now();
        }
    }
    
    /**
     * Formats account fields to ensure COBOL-compatible data formats.
     * Applies proper scaling for BigDecimal amounts and trims string fields.
     */
    private void formatFields() {
        // Ensure proper scale for all monetary BigDecimal fields (scale=2 for COMP-3 precision)
        if (currentBalance != null) {
            currentBalance = currentBalance.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        
        if (creditLimit != null) {
            creditLimit = creditLimit.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        
        if (cashCreditLimit != null) {
            cashCreditLimit = cashCreditLimit.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        
        if (currentCycleCredit != null) {
            currentCycleCredit = currentCycleCredit.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        
        if (currentCycleDebit != null) {
            currentCycleDebit = currentCycleDebit.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        
        // Format string fields to uppercase and trim whitespace
        if (activeStatus != null) {
            activeStatus = activeStatus.trim().toUpperCase();
        }
        
        if (addressZip != null) {
            addressZip = addressZip.trim();
        }
        
        if (groupId != null) {
            groupId = groupId.trim().toUpperCase();
        }
    }
    
    /**
     * Retrieves customer ID from the associated Customer entity.
     * Provides convenient access to customer identification without requiring
     * full Customer object navigation. Used for foreign key access and reporting.
     * 
     * @return customer ID if customer relationship exists, null otherwise
     */
    public Long getCustomerId() {
        return (customer != null) ? customer.getCustomerId() : null;
    }
    
    /**
     * Retrieves account group ID from the groupId field.
     * Provides convenient access to account group identification for processing,
     * reporting, and batch operations. Used by services and batch jobs for 
     * group-based account processing and fee assessment.
     * 
     * @return account group ID (same as groupId field), null if not set
     */
    public String getAccountGroupId() {
        return this.groupId;
    }
    
    /**
     * Sets account group ID by updating the groupId field.
     * Provides convenient access to account group identification for processing,
     * reporting, and batch operations. Used by services and batch jobs for 
     * group-based account processing and fee assessment.
     * 
     * @param accountGroupId the account group ID to set
     */
    public void setAccountGroupId(String accountGroupId) {
        this.groupId = accountGroupId;
    }
    
    /**
     * Retrieves complete account data as a formatted string.
     * Provides COBOL-compatible account data representation for legacy interfaces
     * and reporting systems that expect fixed-format account records.
     * 
     * @return formatted account data string matching COBOL record layout
     */
    public String getAccountData() {
        StringBuilder accountData = new StringBuilder();
        
        // Format account ID (11 digits, zero-padded)
        accountData.append(String.format("%011d", accountId != null ? accountId : 0L));
        
        // Format active status (1 character)
        accountData.append(activeStatus != null ? activeStatus : " ");
        
        // Format monetary amounts (12 characters including 2 decimal places)
        accountData.append(formatMonetaryAmount(currentBalance));
        accountData.append(formatMonetaryAmount(creditLimit));
        accountData.append(formatMonetaryAmount(cashCreditLimit));
        
        // Format dates (10 characters each, YYYY-MM-DD format)
        accountData.append(formatDate(openDate));
        accountData.append(formatDate(expirationDate));
        accountData.append(formatDate(reissueDate));
        
        // Format cycle amounts (12 characters including 2 decimal places)
        accountData.append(formatMonetaryAmount(currentCycleCredit));
        accountData.append(formatMonetaryAmount(currentCycleDebit));
        
        // Format ZIP code (10 characters)
        accountData.append(String.format("%-10s", addressZip != null ? addressZip : ""));
        
        // Format group ID (10 characters)
        accountData.append(String.format("%-10s", groupId != null ? groupId : ""));
        
        return accountData.toString();
    }
    
    /**
     * Sets account data from a formatted string.
     * Parses COBOL-compatible account data string and populates entity fields.
     * Used for data migration and legacy interface compatibility.
     * 
     * @param accountData formatted account data string matching COBOL record layout
     */
    public void setAccountData(String accountData) {
        if (accountData == null || accountData.length() < 89) {
            throw new IllegalArgumentException("Account data string is too short or null");
        }
        
        int pos = 0;
        
        // Parse account ID (11 digits)
        this.accountId = Long.parseLong(accountData.substring(pos, pos + 11));
        pos += 11;
        
        // Parse active status (1 character)
        this.activeStatus = accountData.substring(pos, pos + 1);
        pos += 1;
        
        // Parse monetary amounts (12 characters each including 2 decimal places)
        this.currentBalance = parseMonetaryAmount(accountData.substring(pos, pos + 12));
        pos += 12;
        
        this.creditLimit = parseMonetaryAmount(accountData.substring(pos, pos + 12));
        pos += 12;
        
        this.cashCreditLimit = parseMonetaryAmount(accountData.substring(pos, pos + 12));
        pos += 12;
        
        // Parse dates (10 characters each)
        this.openDate = parseDate(accountData.substring(pos, pos + 10));
        pos += 10;
        
        this.expirationDate = parseDate(accountData.substring(pos, pos + 10));
        pos += 10;
        
        this.reissueDate = parseDate(accountData.substring(pos, pos + 10));
        pos += 10;
        
        // Parse cycle amounts (12 characters each including 2 decimal places)
        this.currentCycleCredit = parseMonetaryAmount(accountData.substring(pos, pos + 12));
        pos += 12;
        
        this.currentCycleDebit = parseMonetaryAmount(accountData.substring(pos, pos + 12));
        pos += 12;
        
        // Parse ZIP code (10 characters)
        this.addressZip = accountData.substring(pos, pos + 10).trim();
        pos += 10;
        
        // Parse group ID (10 characters)
        this.groupId = accountData.substring(pos, pos + 10).trim();
    }
    
    /**
     * Formats a monetary amount for COBOL-compatible string representation.
     * Converts BigDecimal to fixed-width string with proper decimal formatting.
     * 
     * @param amount BigDecimal amount to format
     * @return formatted string (12 characters including 2 decimal places)
     */
    private String formatMonetaryAmount(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        return String.format("%12.2f", amount);
    }
    
    /**
     * Parses a monetary amount from COBOL-compatible string format.
     * Converts fixed-width string to BigDecimal with proper scale preservation.
     * 
     * @param amountStr formatted monetary string
     * @return BigDecimal amount with scale=2
     */
    private BigDecimal parseMonetaryAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        try {
            return new BigDecimal(amountStr.trim()).setScale(2, BigDecimal.ROUND_HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO.setScale(2);
        }
    }
    
    /**
     * Formats a date for COBOL-compatible string representation.
     * Converts LocalDate to YYYY-MM-DD format for legacy compatibility.
     * 
     * @param date LocalDate to format
     * @return formatted date string (10 characters)
     */
    private String formatDate(LocalDate date) {
        if (date == null) {
            return "          "; // 10 spaces
        }
        return date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
    }
    
    /**
     * Parses a date from COBOL-compatible string format.
     * Converts YYYY-MM-DD string to LocalDate for proper date handling.
     * 
     * @param dateStr formatted date string
     * @return LocalDate object, null if invalid or empty
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty() || dateStr.trim().equals("0000-00-00")) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Custom equals method to properly compare Account entities.
     * Uses account ID as the primary comparison field with proper null handling.
     * 
     * @param o the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Account account = (Account) o;
        
        // Primary comparison by account ID
        return Objects.equals(accountId, account.accountId);
    }
    
    /**
     * Custom hash code method using Objects.hash() for consistency with equals().
     * 
     * @return hash code for the Account entity
     */
    @Override
    public int hashCode() {
        return Objects.hash(accountId);
    }
    
    /**
     * Custom toString method providing detailed account information.
     * Includes all key fields while maintaining data security for sensitive information.
     * 
     * @return string representation of the Account entity
     */
    @Override
    public String toString() {
        return "Account{" +
                "accountId=" + accountId +
                ", activeStatus='" + activeStatus + '\'' +
                ", currentBalance=" + currentBalance +
                ", creditLimit=" + creditLimit +
                ", cashCreditLimit=" + cashCreditLimit +
                ", openDate=" + openDate +
                ", expirationDate=" + expirationDate +
                ", reissueDate=" + reissueDate +
                ", currentCycleCredit=" + currentCycleCredit +
                ", currentCycleDebit=" + currentCycleDebit +
                ", addressZip='" + addressZip + '\'' +
                ", groupId='" + groupId + '\'' +
                ", customerId=" + getCustomerId() +
                ", disclosureGroupId=" + (disclosureGroup != null ? disclosureGroup.getDisclosureGroupId() : null) +
                '}';
    }
}
