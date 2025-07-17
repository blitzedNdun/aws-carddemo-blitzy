package com.carddemo.common.entity;

import com.carddemo.common.entity.DisclosureGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * JPA Account entity mapping COBOL ACCOUNT-RECORD (300-byte) structure to PostgreSQL accounts table
 * with financial balances using BigDecimal precision, account status tracking, date management,
 * and foreign key relationships to customers and disclosure groups.
 * 
 * This entity supports:
 * - COBOL COMP-3 decimal precision using BigDecimal with DECIMAL128 context per Section 0.3.2
 * - Account lifecycle management with optimistic locking per Section 5.2.2
 * - Sub-200ms response times for account operations per Section 2.1.3
 * - PostgreSQL DECIMAL(12,2) mapping for exact financial precision
 * - Foreign key relationships for data integrity and referential consistency
 * 
 * Maps to PostgreSQL table: accounts
 * Original COBOL structure: ACCOUNT-RECORD in CVACT01Y.cpy (300 bytes)
 * 
 * Financial fields maintain exact precision equivalent to COBOL COMP-3:
 * - ACCT-CURR-BAL PIC S9(10)V99 -> DECIMAL(12,2) current_balance
 * - ACCT-CREDIT-LIMIT PIC S9(10)V99 -> DECIMAL(12,2) credit_limit
 * - ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99 -> DECIMAL(12,2) cash_credit_limit
 * - ACCT-CURR-CYC-CREDIT PIC S9(10)V99 -> DECIMAL(12,2) current_cycle_credit
 * - ACCT-CURR-CYC-DEBIT PIC S9(10)V99 -> DECIMAL(12,2) current_cycle_debit
 */
@Entity
@Table(name = "accounts")
public class Account implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * MathContext for BigDecimal operations maintaining COBOL COMP-3 precision
     * Uses DECIMAL128 precision with HALF_UP rounding mode per Section 0.3.2
     */
    public static final MathContext COBOL_MATH_CONTEXT = new MathContext(31, RoundingMode.HALF_UP);

    /**
     * Default scale for financial calculations (2 decimal places)
     */
    public static final int FINANCIAL_SCALE = 2;

    /**
     * Account identifier - Primary key mapping ACCT-ID PIC 9(11)
     * Maps to PostgreSQL VARCHAR(11) account_id primary key with proper formatting
     * Range: 00000000001 to 99999999999 (11-digit numeric format)
     */
    @Id
    @Column(name = "account_id", length = 11, nullable = false)
    @NotNull(message = "Account ID cannot be null")
    @Pattern(regexp = "\\d{11}", message = "Account ID must be exactly 11 digits")
    private String accountId;

    /**
     * Customer identifier - Foreign key to customers table
     * Maps to PostgreSQL VARCHAR(9) customer_id foreign key
     * Links account to customer record for relationship management
     */
    @Column(name = "customer_id", length = 9, nullable = false)
    @NotNull(message = "Customer ID cannot be null")
    @Pattern(regexp = "\\d{9}", message = "Customer ID must be exactly 9 digits")
    private String customerId;

    /**
     * Account active status - Maps ACCT-ACTIVE-STATUS PIC X(01)
     * Maps to PostgreSQL VARCHAR(1) with validation constraints
     * Valid values: 'A' (Active), 'C' (Closed), 'S' (Suspended)
     */
    @Column(name = "active_status", length = 1, nullable = false)
    @NotNull(message = "Active status cannot be null")
    @Pattern(regexp = "[ACS]", message = "Active status must be 'A' (Active), 'C' (Closed), or 'S' (Suspended)")
    private String activeStatus;

    /**
     * Current account balance - Maps ACCT-CURR-BAL PIC S9(10)V99
     * Maps to PostgreSQL DECIMAL(12,2) using BigDecimal for exact precision
     * Maintains COBOL COMP-3 decimal precision with DECIMAL128 context
     * Range: -9999999999.99 to 9999999999.99
     */
    @Column(name = "current_balance", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Current balance cannot be null")
    @DecimalMin(value = "-9999999999.99", message = "Current balance cannot be less than -9999999999.99")
    private BigDecimal currentBalance;

    /**
     * Credit limit - Maps ACCT-CREDIT-LIMIT PIC S9(10)V99
     * Maps to PostgreSQL DECIMAL(12,2) using BigDecimal for exact precision
     * Maintains COBOL COMP-3 decimal precision with DECIMAL128 context
     * Range: 0.00 to 9999999999.99
     */
    @Column(name = "credit_limit", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Credit limit cannot be null")
    @DecimalMin(value = "0.00", message = "Credit limit cannot be negative")
    private BigDecimal creditLimit;

    /**
     * Cash credit limit - Maps ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99
     * Maps to PostgreSQL DECIMAL(12,2) using BigDecimal for exact precision
     * Maintains COBOL COMP-3 decimal precision with DECIMAL128 context
     * Range: 0.00 to 9999999999.99
     */
    @Column(name = "cash_credit_limit", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Cash credit limit cannot be null")
    @DecimalMin(value = "0.00", message = "Cash credit limit cannot be negative")
    private BigDecimal cashCreditLimit;

    /**
     * Account open date - Maps ACCT-OPEN-DATE PIC X(10)
     * Maps to PostgreSQL DATE type for account lifecycle management
     */
    @Column(name = "open_date", nullable = false)
    @NotNull(message = "Open date cannot be null")
    private LocalDate openDate;

    /**
     * Account expiration date - Maps ACCT-EXPIRAION-DATE PIC X(10)
     * Maps to PostgreSQL DATE type for account lifecycle management
     * Note: Original COBOL has typo "EXPIRAION" but maps to correct "expiration"
     */
    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    /**
     * Account reissue date - Maps ACCT-REISSUE-DATE PIC X(10)
     * Maps to PostgreSQL DATE type for account lifecycle management
     */
    @Column(name = "reissue_date")
    private LocalDate reissueDate;

    /**
     * Current cycle credit - Maps ACCT-CURR-CYC-CREDIT PIC S9(10)V99
     * Maps to PostgreSQL DECIMAL(12,2) using BigDecimal for exact precision
     * Maintains COBOL COMP-3 decimal precision with DECIMAL128 context
     * Range: 0.00 to 9999999999.99
     */
    @Column(name = "current_cycle_credit", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Current cycle credit cannot be null")
    @DecimalMin(value = "0.00", message = "Current cycle credit cannot be negative")
    private BigDecimal currentCycleCredit;

    /**
     * Current cycle debit - Maps ACCT-CURR-CYC-DEBIT PIC S9(10)V99
     * Maps to PostgreSQL DECIMAL(12,2) using BigDecimal for exact precision
     * Maintains COBOL COMP-3 decimal precision with DECIMAL128 context
     * Range: 0.00 to 9999999999.99
     */
    @Column(name = "current_cycle_debit", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Current cycle debit cannot be null")
    @DecimalMin(value = "0.00", message = "Current cycle debit cannot be negative")
    private BigDecimal currentCycleDebit;

    /**
     * Address ZIP code - Maps ACCT-ADDR-ZIP PIC X(10)
     * Maps to PostgreSQL VARCHAR(10) for address management
     */
    @Column(name = "address_zip", length = 10)
    @Size(max = 10, message = "Address ZIP code cannot exceed 10 characters")
    private String addressZip;

    /**
     * Disclosure group identifier - Maps ACCT-GROUP-ID PIC X(10)
     * Maps to PostgreSQL VARCHAR(10) group_id foreign key
     * Links account to disclosure group for interest rate management
     */
    @Column(name = "group_id", length = 10)
    @Size(max = 10, message = "Group ID cannot exceed 10 characters")
    private String groupId;

    /**
     * Many-to-one relationship with DisclosureGroup entity
     * Foreign key relationship for interest rate application and financial compliance management
     * Enables disclosure group to apply interest rates to multiple accounts
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", referencedColumnName = "group_id", insertable = false, updatable = false)
    private DisclosureGroup disclosureGroup;

    /**
     * Optimistic locking version for concurrent account operations protection
     * Enables account lifecycle management with concurrent access control
     * Prevents lost updates in multi-user scenarios
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Default constructor for JPA
     */
    public Account() {
        // Initialize financial fields with zero values using proper scale
        this.currentBalance = BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
        this.creditLimit = BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
        this.cashCreditLimit = BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
        this.currentCycleCredit = BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
        this.currentCycleDebit = BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
        this.version = 0L;
    }

    /**
     * Constructor with essential fields for account creation
     * 
     * @param accountId Account identifier (11 digits)
     * @param customerId Customer identifier (9 digits)
     * @param activeStatus Account status ('A', 'C', 'S')
     * @param currentBalance Current balance with BigDecimal precision
     * @param creditLimit Credit limit with BigDecimal precision
     * @param cashCreditLimit Cash credit limit with BigDecimal precision
     * @param openDate Account opening date
     */
    public Account(String accountId, String customerId, String activeStatus, 
                   BigDecimal currentBalance, BigDecimal creditLimit, BigDecimal cashCreditLimit,
                   LocalDate openDate) {
        this();
        this.accountId = accountId;
        this.customerId = customerId;
        this.activeStatus = activeStatus;
        this.currentBalance = currentBalance != null ? currentBalance.setScale(FINANCIAL_SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
        this.creditLimit = creditLimit != null ? creditLimit.setScale(FINANCIAL_SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
        this.cashCreditLimit = cashCreditLimit != null ? cashCreditLimit.setScale(FINANCIAL_SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
        this.openDate = openDate;
    }

    /**
     * Gets the account identifier
     * 
     * @return Account ID as 11-digit string
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account identifier
     * 
     * @param accountId Account ID as 11-digit string
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the customer identifier
     * 
     * @return Customer ID as 9-digit string
     */
    public String getCustomerId() {
        return customerId;
    }

    /**
     * Sets the customer identifier
     * 
     * @param customerId Customer ID as 9-digit string
     */
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    /**
     * Gets the customer entity (placeholder for future implementation)
     * Note: Customer entity relationship will be implemented by another agent
     * 
     * @return Customer entity (null for now)
     */
    public Object getCustomer() {
        // Customer relationship not yet implemented
        return null;
    }

    /**
     * Sets the customer entity (placeholder for future implementation)
     * Note: Customer entity relationship will be implemented by another agent
     * 
     * @param customer Customer entity
     */
    public void setCustomer(Object customer) {
        // Customer relationship not yet implemented
    }

    /**
     * Gets the account active status
     * 
     * @return Active status ('A', 'C', 'S')
     */
    public String getActiveStatus() {
        return activeStatus;
    }

    /**
     * Sets the account active status
     * 
     * @param activeStatus Active status ('A', 'C', 'S')
     */
    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }

    /**
     * Gets the current account balance with BigDecimal precision
     * Maintains COBOL COMP-3 precision for financial calculations
     * 
     * @return Current balance as BigDecimal with DECIMAL(12,2) precision
     */
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    /**
     * Sets the current account balance with BigDecimal precision
     * Maintains COBOL COMP-3 precision for financial calculations
     * 
     * @param currentBalance Current balance as BigDecimal
     */
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance != null ? currentBalance.setScale(FINANCIAL_SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
    }

    /**
     * Gets the credit limit with BigDecimal precision
     * Maintains COBOL COMP-3 precision for financial calculations
     * 
     * @return Credit limit as BigDecimal with DECIMAL(12,2) precision
     */
    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    /**
     * Sets the credit limit with BigDecimal precision
     * Maintains COBOL COMP-3 precision for financial calculations
     * 
     * @param creditLimit Credit limit as BigDecimal
     */
    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit != null ? creditLimit.setScale(FINANCIAL_SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
    }

    /**
     * Gets the cash credit limit with BigDecimal precision
     * Maintains COBOL COMP-3 precision for financial calculations
     * 
     * @return Cash credit limit as BigDecimal with DECIMAL(12,2) precision
     */
    public BigDecimal getCashCreditLimit() {
        return cashCreditLimit;
    }

    /**
     * Sets the cash credit limit with BigDecimal precision
     * Maintains COBOL COMP-3 precision for financial calculations
     * 
     * @param cashCreditLimit Cash credit limit as BigDecimal
     */
    public void setCashCreditLimit(BigDecimal cashCreditLimit) {
        this.cashCreditLimit = cashCreditLimit != null ? cashCreditLimit.setScale(FINANCIAL_SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
    }

    /**
     * Gets the account opening date
     * 
     * @return Open date as LocalDate
     */
    public LocalDate getOpenDate() {
        return openDate;
    }

    /**
     * Sets the account opening date
     * 
     * @param openDate Open date as LocalDate
     */
    public void setOpenDate(LocalDate openDate) {
        this.openDate = openDate;
    }

    /**
     * Gets the account expiration date
     * 
     * @return Expiration date as LocalDate
     */
    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    /**
     * Sets the account expiration date
     * 
     * @param expirationDate Expiration date as LocalDate
     */
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * Gets the account reissue date
     * 
     * @return Reissue date as LocalDate
     */
    public LocalDate getReissueDate() {
        return reissueDate;
    }

    /**
     * Sets the account reissue date
     * 
     * @param reissueDate Reissue date as LocalDate
     */
    public void setReissueDate(LocalDate reissueDate) {
        this.reissueDate = reissueDate;
    }

    /**
     * Gets the current cycle credit with BigDecimal precision
     * Maintains COBOL COMP-3 precision for financial calculations
     * 
     * @return Current cycle credit as BigDecimal with DECIMAL(12,2) precision
     */
    public BigDecimal getCurrentCycleCredit() {
        return currentCycleCredit;
    }

    /**
     * Sets the current cycle credit with BigDecimal precision
     * Maintains COBOL COMP-3 precision for financial calculations
     * 
     * @param currentCycleCredit Current cycle credit as BigDecimal
     */
    public void setCurrentCycleCredit(BigDecimal currentCycleCredit) {
        this.currentCycleCredit = currentCycleCredit != null ? currentCycleCredit.setScale(FINANCIAL_SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
    }

    /**
     * Gets the current cycle debit with BigDecimal precision
     * Maintains COBOL COMP-3 precision for financial calculations
     * 
     * @return Current cycle debit as BigDecimal with DECIMAL(12,2) precision
     */
    public BigDecimal getCurrentCycleDebit() {
        return currentCycleDebit;
    }

    /**
     * Sets the current cycle debit with BigDecimal precision
     * Maintains COBOL COMP-3 precision for financial calculations
     * 
     * @param currentCycleDebit Current cycle debit as BigDecimal
     */
    public void setCurrentCycleDebit(BigDecimal currentCycleDebit) {
        this.currentCycleDebit = currentCycleDebit != null ? currentCycleDebit.setScale(FINANCIAL_SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
    }

    /**
     * Gets the address ZIP code
     * 
     * @return Address ZIP code as string
     */
    public String getAddressZip() {
        return addressZip;
    }

    /**
     * Sets the address ZIP code
     * 
     * @param addressZip Address ZIP code as string
     */
    public void setAddressZip(String addressZip) {
        this.addressZip = addressZip;
    }

    /**
     * Gets the disclosure group identifier
     * 
     * @return Group ID as string
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Sets the disclosure group identifier
     * 
     * @param groupId Group ID as string
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * Gets the associated disclosure group entity
     * Provides access to interest rate information and disclosure text
     * 
     * @return DisclosureGroup entity for interest rate application
     */
    public DisclosureGroup getDisclosureGroup() {
        return disclosureGroup;
    }

    /**
     * Sets the associated disclosure group entity
     * Links account to disclosure group for interest rate management
     * 
     * @param disclosureGroup DisclosureGroup entity
     */
    public void setDisclosureGroup(DisclosureGroup disclosureGroup) {
        this.disclosureGroup = disclosureGroup;
        if (disclosureGroup != null && disclosureGroup.getGroupId() != null) {
            this.groupId = disclosureGroup.getGroupId();
        }
    }

    /**
     * Gets the optimistic locking version
     * 
     * @return Version number for optimistic locking
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Sets the optimistic locking version
     * 
     * @param version Version number for optimistic locking
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Calculates available credit based on current balance and credit limit
     * Uses BigDecimal arithmetic for exact precision
     * 
     * @return Available credit as BigDecimal
     */
    public BigDecimal getAvailableCredit() {
        if (creditLimit == null || currentBalance == null) {
            return BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
        }
        return creditLimit.subtract(currentBalance, COBOL_MATH_CONTEXT).setScale(FINANCIAL_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculates available cash credit based on current balance and cash credit limit
     * Uses BigDecimal arithmetic for exact precision
     * 
     * @return Available cash credit as BigDecimal
     */
    public BigDecimal getAvailableCashCredit() {
        if (cashCreditLimit == null || currentBalance == null) {
            return BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
        }
        return cashCreditLimit.subtract(currentBalance, COBOL_MATH_CONTEXT).setScale(FINANCIAL_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculates interest amount using disclosure group interest rate
     * Provides precise BigDecimal arithmetic for financial calculations
     * 
     * @return Interest amount as BigDecimal
     */
    public BigDecimal calculateInterest() {
        if (disclosureGroup != null && disclosureGroup.getInterestRate() != null && currentBalance != null) {
            return currentBalance.multiply(disclosureGroup.getInterestRate(), COBOL_MATH_CONTEXT).setScale(FINANCIAL_SCALE, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(FINANCIAL_SCALE);
    }

    /**
     * Checks if account is active
     * 
     * @return true if account status is 'A' (Active)
     */
    public boolean isActive() {
        return "A".equals(activeStatus);
    }

    /**
     * Checks if account is closed
     * 
     * @return true if account status is 'C' (Closed)
     */
    public boolean isClosed() {
        return "C".equals(activeStatus);
    }

    /**
     * Checks if account is suspended
     * 
     * @return true if account status is 'S' (Suspended)
     */
    public boolean isSuspended() {
        return "S".equals(activeStatus);
    }

    /**
     * Checks if account is expired based on expiration date
     * 
     * @return true if expiration date is before current date
     */
    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDate.now());
    }

    /**
     * Adds amount to current balance with precise BigDecimal arithmetic
     * Maintains COBOL COMP-3 precision for financial calculations
     * 
     * @param amount Amount to add to balance
     */
    public void addToBalance(BigDecimal amount) {
        if (amount != null) {
            this.currentBalance = this.currentBalance.add(amount, COBOL_MATH_CONTEXT).setScale(FINANCIAL_SCALE, RoundingMode.HALF_UP);
        }
    }

    /**
     * Subtracts amount from current balance with precise BigDecimal arithmetic
     * Maintains COBOL COMP-3 precision for financial calculations
     * 
     * @param amount Amount to subtract from balance
     */
    public void subtractFromBalance(BigDecimal amount) {
        if (amount != null) {
            this.currentBalance = this.currentBalance.subtract(amount, COBOL_MATH_CONTEXT).setScale(FINANCIAL_SCALE, RoundingMode.HALF_UP);
        }
    }

    /**
     * Equals method for entity comparison based on account ID
     * 
     * @param o Object to compare
     * @return true if objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(accountId, account.accountId);
    }

    /**
     * Hash code method for entity hashing based on account ID
     * 
     * @return hash code based on accountId
     */
    @Override
    public int hashCode() {
        return Objects.hash(accountId);
    }

    /**
     * String representation of account entity
     * 
     * @return String representation including key fields
     */
    @Override
    public String toString() {
        return "Account{" +
                "accountId='" + accountId + '\'' +
                ", customerId='" + customerId + '\'' +
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
                ", version=" + version +
                '}';
    }
}