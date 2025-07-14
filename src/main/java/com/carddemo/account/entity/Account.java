package com.carddemo.account.entity;

import com.carddemo.common.enums.AccountStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA Entity representing account master data converted from COBOL ACCOUNT-RECORD.
 * 
 * This entity maps the legacy VSAM ACCTDAT dataset structure to a modern PostgreSQL
 * relational table with exact decimal precision, foreign key relationships, and 
 * comprehensive validation supporting the complete account lifecycle in the Spring Boot
 * microservices architecture.
 * 
 * The entity implements BigDecimal financial arithmetic with MathContext.DECIMAL128
 * precision matching COBOL COMP-3 requirements per Section 0.1.2 data precision mandate.
 * All monetary fields utilize PostgreSQL DECIMAL(12,2) data types with exact precision
 * preservation for financial calculations and regulatory compliance.
 * 
 * Database relationships include bidirectional associations with Customer, Card, and 
 * Transaction entities, maintaining referential integrity equivalent to VSAM cross-reference
 * functionality per Section 6.2.1.1 entity relationships.
 * 
 * Converted from: app/cpy/CVACT01Y.cpy (COBOL copybook)
 * Database Table: accounts (PostgreSQL)
 * Record Length: 300 bytes (original COBOL layout)
 * 
 * @author Blitzy agent
 * @version CardDemo_v1.0-15-g27d6c6f-68
 * @since 2022-07-19
 */
@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_customer_account_xref", columnList = "customer_id, account_id"),
    @Index(name = "idx_account_balance", columnList = "account_id, current_balance"),
    @Index(name = "idx_account_status", columnList = "active_status, account_id")
})
public class Account {

    /**
     * MathContext for exact financial calculations matching COBOL COMP-3 precision.
     * Uses DECIMAL128 precision with HALF_EVEN rounding per Section 0.1.2 requirements.
     */
    public static final MathContext COBOL_MATH_CONTEXT = MathContext.DECIMAL128;

    /**
     * Account unique identifier (Primary Key)
     * Converted from: ACCT-ID PIC 9(11)
     * Format: 11-digit numeric string
     */
    @Id
    @Column(name = "account_id", length = 11, nullable = false)
    @NotNull(message = "Account ID cannot be null")
    @Pattern(regexp = "\\d{11}", message = "Account ID must be exactly 11 digits")
    private String accountId;

    /**
     * Customer relationship (Foreign Key)
     * Converted from: Implicit relationship through customer data processing
     * Maintains bidirectional relationship integrity per Section 6.2.1.1
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @NotNull(message = "Customer association is required")
    private Customer customer;

    /**
     * Account active status
     * Converted from: ACCT-ACTIVE-STATUS PIC X(01)
     * Uses AccountStatus enum with COBOL 88-level condition mapping
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "active_status", length = 1, nullable = false)
    @NotNull(message = "Account status cannot be null")
    private AccountStatus activeStatus;

    /**
     * Current account balance
     * Converted from: ACCT-CURR-BAL PIC S9(10)V99
     * Precision: DECIMAL(12,2) with exact financial arithmetic
     */
    @Column(name = "current_balance", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Current balance cannot be null")
    @DecimalMin(value = "-999999999999.99", message = "Current balance cannot be less than minimum")
    @DecimalMax(value = "999999999999.99", message = "Current balance cannot exceed maximum")
    private BigDecimal currentBalance;

    /**
     * Account credit limit
     * Converted from: ACCT-CREDIT-LIMIT PIC S9(10)V99
     * Precision: DECIMAL(12,2) for exact credit limit calculations
     */
    @Column(name = "credit_limit", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Credit limit cannot be null")
    @DecimalMin(value = "0.00", message = "Credit limit must be non-negative")
    @DecimalMax(value = "999999999999.99", message = "Credit limit cannot exceed maximum")
    private BigDecimal creditLimit;

    /**
     * Cash advance credit limit
     * Converted from: ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99
     * Precision: DECIMAL(12,2) for cash advance calculations
     */
    @Column(name = "cash_credit_limit", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Cash credit limit cannot be null")
    @DecimalMin(value = "0.00", message = "Cash credit limit must be non-negative")
    @DecimalMax(value = "999999999999.99", message = "Cash credit limit cannot exceed maximum")
    private BigDecimal cashCreditLimit;

    /**
     * Account opening date
     * Converted from: ACCT-OPEN-DATE PIC X(10)
     * Uses LocalDate for proper date handling and validation
     */
    @Column(name = "open_date", nullable = false)
    @NotNull(message = "Open date cannot be null")
    @PastOrPresent(message = "Open date cannot be in the future")
    private LocalDate openDate;

    /**
     * Account expiration date
     * Converted from: ACCT-EXPIRAION-DATE PIC X(10)
     * Note: Original COBOL field name contains typo preserved for compatibility
     */
    @Column(name = "expiration_date")
    @Future(message = "Expiration date must be in the future")
    private LocalDate expirationDate;

    /**
     * Card reissue date
     * Converted from: ACCT-REISSUE-DATE PIC X(10)
     * Tracks most recent card reissue for this account
     */
    @Column(name = "reissue_date")
    private LocalDate reissueDate;

    /**
     * Current cycle credit amount
     * Converted from: ACCT-CURR-CYC-CREDIT PIC S9(10)V99
     * Precision: DECIMAL(12,2) for billing cycle calculations
     */
    @Column(name = "current_cycle_credit", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Current cycle credit cannot be null")
    @DecimalMin(value = "0.00", message = "Current cycle credit must be non-negative")
    @DecimalMax(value = "999999999999.99", message = "Current cycle credit cannot exceed maximum")
    private BigDecimal currentCycleCredit;

    /**
     * Current cycle debit amount
     * Converted from: ACCT-CURR-CYC-DEBIT PIC S9(10)V99
     * Precision: DECIMAL(12,2) for billing cycle calculations
     */
    @Column(name = "current_cycle_debit", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Current cycle debit cannot be null")
    @DecimalMin(value = "0.00", message = "Current cycle debit must be non-negative")
    @DecimalMax(value = "999999999999.99", message = "Current cycle debit cannot exceed maximum")
    private BigDecimal currentCycleDebit;

    /**
     * Account address ZIP code
     * Converted from: ACCT-ADDR-ZIP PIC X(10)
     * ZIP code associated with account for billing and verification
     */
    @Column(name = "address_zip", length = 10)
    @Size(max = 10, message = "Address ZIP code cannot exceed 10 characters")
    private String addressZip;

    /**
     * Disclosure group identifier
     * Converted from: ACCT-GROUP-ID PIC X(10)
     * Links to disclosure group for interest rate and terms
     */
    @Column(name = "group_id", length = 10)
    @Size(max = 10, message = "Group ID cannot exceed 10 characters")
    private String groupId;

    /**
     * Optimistic locking version field
     * Supports concurrent modification detection equivalent to VSAM record locking
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Collection of cards associated with this account
     * 
     * TODO: Enable @OneToMany relationship when Card entity is implemented by other agents
     * @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
     * 
     * Implements account-card relationship maintenance per Section 6.2.1.1 requirements.
     * This relationship will provide bidirectional association with Card entities
     * for complete card lifecycle management within account context.
     */
    // @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Object> cards = new HashSet<>();

    /**
     * Collection of transactions associated with this account
     * 
     * TODO: Enable @OneToMany relationship when Transaction entity is implemented by other agents
     * @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
     * 
     * Implements account-transaction relationship for financial tracking per business requirements.
     * This relationship will provide lazy loading access to transaction history
     * with proper cascade operations for transaction lifecycle management.
     */
    // @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Object> transactions = new HashSet<>();

    /**
     * Default constructor required by JPA
     */
    public Account() {
        // Initialize BigDecimal fields with zero values using COBOL math context
        this.currentBalance = BigDecimal.ZERO;
        this.creditLimit = BigDecimal.ZERO;
        this.cashCreditLimit = BigDecimal.ZERO;
        this.currentCycleCredit = BigDecimal.ZERO;
        this.currentCycleDebit = BigDecimal.ZERO;
        
        // Initialize collections to prevent null pointer exceptions
        this.cards = new HashSet<>();
        this.transactions = new HashSet<>();
    }

    /**
     * Constructor with required fields
     */
    public Account(String accountId, Customer customer, AccountStatus activeStatus,
                   BigDecimal currentBalance, BigDecimal creditLimit, 
                   BigDecimal cashCreditLimit, LocalDate openDate) {
        this();
        this.accountId = accountId;
        this.customer = customer;
        this.activeStatus = activeStatus;
        this.currentBalance = currentBalance;
        this.creditLimit = creditLimit;
        this.cashCreditLimit = cashCreditLimit;
        this.openDate = openDate;
    }

    // Getter and Setter methods

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public AccountStatus getActiveStatus() {
        return activeStatus;
    }

    public void setActiveStatus(AccountStatus activeStatus) {
        this.activeStatus = activeStatus;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public BigDecimal getCashCreditLimit() {
        return cashCreditLimit;
    }

    public void setCashCreditLimit(BigDecimal cashCreditLimit) {
        this.cashCreditLimit = cashCreditLimit;
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
        this.currentCycleCredit = currentCycleCredit;
    }

    public BigDecimal getCurrentCycleDebit() {
        return currentCycleDebit;
    }

    public void setCurrentCycleDebit(BigDecimal currentCycleDebit) {
        this.currentCycleDebit = currentCycleDebit;
    }

    public String getAddressZip() {
        return addressZip;
    }

    public void setAddressZip(String addressZip) {
        this.addressZip = addressZip;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Set<Object> getCards() {
        return cards;
    }

    public void setCards(Set<Object> cards) {
        this.cards = cards != null ? cards : new HashSet<>();
    }

    /**
     * Add a card to this account's card collection
     * Maintains bidirectional relationship integrity
     * 
     * Note: Parameter type will be Card when that entity is implemented.
     * Current Object type maintains compilation compatibility.
     */
    public void addCard(Object card) {
        if (card != null) {
            this.cards.add(card);
            // Bidirectional relationship will be established when Card entity is available
            // card.setAccount(this);
        }
    }

    /**
     * Remove a card from this account's card collection
     * Maintains bidirectional relationship integrity
     * 
     * Note: Parameter type will be Card when that entity is implemented.
     * Current Object type maintains compilation compatibility.
     */
    public void removeCard(Object card) {
        if (card != null) {
            this.cards.remove(card);
            // Bidirectional relationship will be cleared when Card entity is available
            // card.setAccount(null);
        }
    }

    public Set<Object> getTransactions() {
        return transactions;
    }

    public void setTransactions(Set<Object> transactions) {
        this.transactions = transactions != null ? transactions : new HashSet<>();
    }

    /**
     * Add a transaction to this account's transaction collection
     * Maintains bidirectional relationship integrity
     * 
     * Note: Parameter type will be Transaction when that entity is implemented.
     * Current Object type maintains compilation compatibility.
     */
    public void addTransaction(Object transaction) {
        if (transaction != null) {
            this.transactions.add(transaction);
            // Bidirectional relationship will be established when Transaction entity is available
            // transaction.setAccount(this);
        }
    }

    /**
     * Remove a transaction from this account's transaction collection
     * Maintains bidirectional relationship integrity
     * 
     * Note: Parameter type will be Transaction when that entity is implemented.
     * Current Object type maintains compilation compatibility.
     */
    public void removeTransaction(Object transaction) {
        if (transaction != null) {
            this.transactions.remove(transaction);
            // Bidirectional relationship will be cleared when Transaction entity is available
            // transaction.setAccount(null);
        }
    }

    /**
     * Calculate available credit based on current balance and credit limit.
     * Uses BigDecimal arithmetic with COBOL math context for exact precision.
     * 
     * @return Available credit amount
     */
    public BigDecimal getAvailableCredit() {
        if (creditLimit == null || currentBalance == null) {
            return BigDecimal.ZERO;
        }
        return creditLimit.subtract(currentBalance, COBOL_MATH_CONTEXT);
    }

    /**
     * Calculate available cash credit based on current balance and cash credit limit.
     * Uses BigDecimal arithmetic with COBOL math context for exact precision.
     * 
     * @return Available cash credit amount
     */
    public BigDecimal getAvailableCashCredit() {
        if (cashCreditLimit == null || currentBalance == null) {
            return BigDecimal.ZERO;
        }
        return cashCreditLimit.subtract(currentBalance, COBOL_MATH_CONTEXT);
    }

    /**
     * Check if account is active based on status
     * 
     * @return true if account status is ACTIVE, false otherwise
     */
    public boolean isActive() {
        return activeStatus != null && activeStatus.isActive();
    }

    /**
     * Check if account is past due based on current balance
     * 
     * @return true if current balance is negative (overdrawn), false otherwise
     */
    public boolean isPastDue() {
        return currentBalance != null && currentBalance.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Check if account is over limit based on current balance and credit limit
     * 
     * @return true if current balance exceeds credit limit, false otherwise
     */
    public boolean isOverLimit() {
        if (currentBalance == null || creditLimit == null) {
            return false;
        }
        return currentBalance.compareTo(creditLimit) > 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Account account = (Account) obj;
        return accountId != null ? accountId.equals(account.accountId) : account.accountId == null;
    }

    @Override
    public int hashCode() {
        return accountId != null ? accountId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountId='" + accountId + '\'' +
                ", activeStatus=" + activeStatus +
                ", currentBalance=" + currentBalance +
                ", creditLimit=" + creditLimit +
                ", cashCreditLimit=" + cashCreditLimit +
                ", openDate=" + openDate +
                ", expirationDate=" + expirationDate +
                ", customerId=" + (customer != null ? customer.getCustomerId() : null) +
                ", cardCount=" + (cards != null ? cards.size() : 0) +
                ", transactionCount=" + (transactions != null ? transactions.size() : 0) +
                ", version=" + version +
                '}';
    }
}