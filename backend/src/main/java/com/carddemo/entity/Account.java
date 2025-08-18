/*
 * Account.java
 * 
 * JPA entity class representing credit card account data, mapped to account_data 
 * PostgreSQL table. Contains account identification, balance information, credit 
 * limits, important dates, and cycle totals. Uses BigDecimal for monetary fields 
 * to preserve COBOL COMP-3 packed decimal precision.
 * 
 * Implements relationship with Customer entity through foreign key.
 * DisclosureGroup relationship is handled programmatically via accountGroupId
 * to avoid foreign key constraint issues (account_group_id is not unique).
 * 
 * Based on COBOL copybook: app/cpy/CVACT01Y.cpy
 * - ACCT-ID (PIC 9(11)) for account identification
 * - ACCT-ACTIVE-STATUS (PIC X(01)) for account status ('Y'=active, 'N'=inactive)
 * - ACCT-CURR-BAL (PIC S9(10)V99) for current balance (COMP-3)
 * - ACCT-CREDIT-LIMIT (PIC S9(10)V99) for credit limit (COMP-3)
 * - ACCT-CASH-CREDIT-LIMIT (PIC S9(10)V99) for cash advance limit (COMP-3)
 * - ACCT-OPEN-DATE (PIC X(10)) for account opening date
 * - ACCT-EXPIRAION-DATE (PIC X(10)) for account expiration date
 * - ACCT-REISSUE-DATE (PIC X(10)) for reissue date
 * - ACCT-CURR-CYC-CREDIT (PIC S9(10)V99) for current cycle credits (COMP-3)
 * - ACCT-CURR-CYC-DEBIT (PIC S9(10)V99) for current cycle debits (COMP-3)
 * - ACCT-ADDR-ZIP (PIC 9(05)) for address ZIP code
 * - ACCT-GROUP-ID (PIC X(10)) for account group ID (links to DisclosureGroup)
 */

package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * JPA entity class representing credit card account data.
 * 
 * Maps to the account_data PostgreSQL table, providing comprehensive account
 * information including balance tracking, credit limits, important dates,
 * and cycle totals. Maintains relationship with Customer entity for complete 
 * account management functionality. DisclosureGroup access via repository.
 * 
 * Key functionality:
 * - Account identification and status management
 * - Balance and credit limit tracking with COBOL precision
 * - Cycle-based credit and debit totals for billing operations
 * - Date tracking for account lifecycle management
 * - Customer relationship through foreign key linkage
 * - Interest rate determination through accountGroupId lookup
 * 
 * All monetary fields use BigDecimal with scale=2 to preserve exact precision
 * equivalent to COBOL COMP-3 packed decimal fields, ensuring financial
 * calculation accuracy and regulatory compliance.
 */
@Entity
@Table(name = "account_data", indexes = {
    @Index(name = "idx_account_customer_id", columnList = "customer_id"),
    @Index(name = "idx_account_group_id", columnList = "account_group_id"),
    @Index(name = "idx_account_status", columnList = "active_status"),
    @Index(name = "idx_account_open_date", columnList = "open_date")
})
public class Account {

    /**
     * Primary key: Account ID (11-digit numeric account identifier).
     * Maps to ACCT-ID from CVACT01Y.cpy (PIC 9(11)).
     */
    @Id
    @Column(name = "account_id", length = 11, nullable = false)
    @NotNull(message = "Account ID is required")
    @Size(min = 11, max = 11, message = "Account ID must be exactly 11 digits")
    @Pattern(regexp = "\\d{11}", message = "Account ID must contain only digits")
    private String accountId;

    /**
     * Account active status flag ('Y' = active, 'N' = inactive).
     * Maps to ACCT-ACTIVE-STATUS from CVACT01Y.cpy (PIC X(01)).
     */
    @Column(name = "active_status", length = 1, nullable = false)
    @NotNull(message = "Active status is required")
    @Pattern(regexp = "[YN]", message = "Active status must be 'Y' or 'N'")
    private String activeStatus;

    /**
     * Current account balance with 2 decimal places.
     * Maps to ACCT-CURR-BAL from CVACT01Y.cpy (PIC S9(10)V99 COMP-3).
     * Uses BigDecimal to preserve exact COBOL COMP-3 precision.
     */
    @Column(name = "current_balance", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Current balance is required")
    @DecimalMax(value = "9999999999.99", message = "Current balance cannot exceed 9,999,999,999.99")
    @DecimalMin(value = "-9999999999.99", message = "Current balance cannot be less than -9,999,999,999.99")
    private BigDecimal currentBalance;

    /**
     * Credit limit with 2 decimal places.
     * Maps to ACCT-CREDIT-LIMIT from CVACT01Y.cpy (PIC S9(10)V99 COMP-3).
     */
    @Column(name = "credit_limit", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Credit limit is required")
    @DecimalMax(value = "9999999999.99", message = "Credit limit cannot exceed 9,999,999,999.99")
    @DecimalMin(value = "0.00", message = "Credit limit must be non-negative")
    private BigDecimal creditLimit;

    /**
     * Cash advance credit limit with 2 decimal places.
     * Maps to ACCT-CASH-CREDIT-LIMIT from CVACT01Y.cpy (PIC S9(10)V99 COMP-3).
     */
    @Column(name = "cash_credit_limit", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Cash credit limit is required")
    @DecimalMax(value = "9999999999.99", message = "Cash credit limit cannot exceed 9,999,999,999.99")
    @DecimalMin(value = "0.00", message = "Cash credit limit must be non-negative")
    private BigDecimal cashCreditLimit;

    /**
     * Account opening date.
     * Maps to ACCT-OPEN-DATE from CVACT01Y.cpy (PIC X(10)).
     */
    @Column(name = "open_date", nullable = false)
    @NotNull(message = "Open date is required")
    @PastOrPresent(message = "Open date cannot be in the future")
    private LocalDate openDate;

    /**
     * Account expiration date.
     * Maps to ACCT-EXPIRAION-DATE from CVACT01Y.cpy (PIC X(10)).
     */
    @Column(name = "expiration_date", nullable = false)
    @NotNull(message = "Expiration date is required")
    @Future(message = "Expiration date must be in the future")
    private LocalDate expirationDate;

    /**
     * Account reissue date (date of last card reissue).
     * Maps to ACCT-REISSUE-DATE from CVACT01Y.cpy (PIC X(10)).
     */
    @Column(name = "reissue_date")
    private LocalDate reissueDate;

    /**
     * Current cycle credit total with 2 decimal places.
     * Maps to ACCT-CURR-CYC-CREDIT from CVACT01Y.cpy (PIC S9(10)V99 COMP-3).
     */
    @Column(name = "current_cycle_credit", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Current cycle credit is required")
    @DecimalMax(value = "9999999999.99", message = "Current cycle credit cannot exceed 9,999,999,999.99")
    @DecimalMin(value = "0.00", message = "Current cycle credit must be non-negative")
    private BigDecimal currentCycleCredit;

    /**
     * Current cycle debit total with 2 decimal places.
     * Maps to ACCT-CURR-CYC-DEBIT from CVACT01Y.cpy (PIC S9(10)V99 COMP-3).
     */
    @Column(name = "current_cycle_debit", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Current cycle debit is required")
    @DecimalMax(value = "9999999999.99", message = "Current cycle debit cannot exceed 9,999,999,999.99")
    @DecimalMin(value = "0.00", message = "Current cycle debit must be non-negative")
    private BigDecimal currentCycleDebit;

    /**
     * Address ZIP code (5-digit).
     * Maps to ACCT-ADDR-ZIP from CVACT01Y.cpy (PIC 9(05)).
     */
    @Column(name = "address_zip", length = 5, nullable = false)
    @NotNull(message = "Address ZIP code is required")
    @Size(min = 5, max = 5, message = "ZIP code must be exactly 5 digits")
    @Pattern(regexp = "\\d{5}", message = "ZIP code must contain only digits")
    private String addressZip;

    /**
     * Account group ID for disclosure group assignment.
     * Maps to ACCT-GROUP-ID from CVACT01Y.cpy (PIC X(10)).
     * Links to DisclosureGroup entity for interest rate determination.
     */
    @Column(name = "account_group_id", length = 10, nullable = false)
    @NotNull(message = "Account group ID is required")
    @Size(min = 1, max = 10, message = "Account group ID must be 1-10 characters")
    private String accountGroupId;

    /**
     * Customer ID for customer relationship (foreign key).
     * Links to Customer entity for account-to-customer navigation.
     */
    @Column(name = "customer_id", length = 9, nullable = false)
    @NotNull(message = "Customer ID is required")
    @Size(min = 9, max = 9, message = "Customer ID must be exactly 9 digits")
    @Pattern(regexp = "\\d{9}", message = "Customer ID must contain only digits")
    private String customerId;

    /**
     * Many-to-one relationship with Customer entity.
     * Enables navigation from account to customer information.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "customer_id", 
                insertable = false, updatable = false)
    private Customer customer;

    /**
     * Note: DisclosureGroup relationship is handled programmatically rather than
     * through JPA foreign key due to non-unique account_group_id in disclosure_groups table.
     * Use DisclosureGroupRepository to query by accountGroupId when needed.
     */
    // Removed @ManyToOne relationship to prevent foreign key constraint issues
    // private DisclosureGroup disclosureGroup;

    /**
     * Default constructor for JPA.
     */
    public Account() {
        // Initialize BigDecimal fields to zero to prevent null pointer exceptions
        this.currentBalance = BigDecimal.ZERO.setScale(2);
        this.creditLimit = BigDecimal.ZERO.setScale(2);
        this.cashCreditLimit = BigDecimal.ZERO.setScale(2);
        this.currentCycleCredit = BigDecimal.ZERO.setScale(2);
        this.currentCycleDebit = BigDecimal.ZERO.setScale(2);
    }

    /**
     * Constructor with required fields.
     */
    public Account(String accountId, String activeStatus, String customerId, String accountGroupId) {
        this();
        this.accountId = accountId;
        this.activeStatus = activeStatus;
        this.customerId = customerId;
        this.accountGroupId = accountGroupId;
    }

    // Getter and Setter methods

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getActiveStatus() {
        return activeStatus;
    }

    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance != null ? currentBalance.setScale(2) : BigDecimal.ZERO.setScale(2);
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit != null ? creditLimit.setScale(2) : BigDecimal.ZERO.setScale(2);
    }

    public BigDecimal getCashCreditLimit() {
        return cashCreditLimit;
    }

    public void setCashCreditLimit(BigDecimal cashCreditLimit) {
        this.cashCreditLimit = cashCreditLimit != null ? cashCreditLimit.setScale(2) : BigDecimal.ZERO.setScale(2);
    }

    public LocalDate getOpenDate() {
        return openDate;
    }

    public void setOpenDate(LocalDate openDate) {
        this.openDate = openDate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public LocalDate getReissueDate() {
        return reissueDate;
    }

    public void setReissueDate(LocalDate reissueDate) {
        this.reissueDate = reissueDate;
    }

    public BigDecimal getCurrentCycleCredit() {
        return currentCycleCredit;
    }

    public void setCurrentCycleCredit(BigDecimal currentCycleCredit) {
        this.currentCycleCredit = currentCycleCredit != null ? currentCycleCredit.setScale(2) : BigDecimal.ZERO.setScale(2);
    }

    public BigDecimal getCurrentCycleDebit() {
        return currentCycleDebit;
    }

    public void setCurrentCycleDebit(BigDecimal currentCycleDebit) {
        this.currentCycleDebit = currentCycleDebit != null ? currentCycleDebit.setScale(2) : BigDecimal.ZERO.setScale(2);
    }

    public String getAddressZip() {
        return addressZip;
    }

    public void setAddressZip(String addressZip) {
        this.addressZip = addressZip;
    }

    public String getAccountGroupId() {
        return accountGroupId;
    }

    public void setAccountGroupId(String accountGroupId) {
        this.accountGroupId = accountGroupId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    /**
     * Note: DisclosureGroup access removed due to JPA foreign key constraint issues.
     * Use DisclosureGroupRepository.findByAccountGroupId(this.accountGroupId) to 
     * retrieve related disclosure groups programmatically.
     */
    // public DisclosureGroup getDisclosureGroup() { return disclosureGroup; }
    // public void setDisclosureGroup(DisclosureGroup disclosureGroup) { this.disclosureGroup = disclosureGroup; }

    /**
     * Utility method to check if account is active.
     */
    public boolean isActive() {
        return "Y".equals(activeStatus);
    }

    /**
     * Utility method to get available credit.
     */
    public BigDecimal getAvailableCredit() {
        return creditLimit.subtract(currentBalance).max(BigDecimal.ZERO);
    }

    /**
     * Utility method to get available cash advance credit.
     */
    public BigDecimal getAvailableCashCredit() {
        return cashCreditLimit.subtract(currentBalance).max(BigDecimal.ZERO);
    }

    /**
     * Utility method to check if account is expired.
     */
    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDate.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(accountId, account.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId);
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountId='" + accountId + '\'' +
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
                ", accountGroupId='" + accountGroupId + '\'' +
                ", customerId='" + customerId + '\'' +
                '}';
    }
}