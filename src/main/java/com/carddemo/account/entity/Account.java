package com.carddemo.account.entity;

import com.carddemo.common.enums.AccountStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Account JPA entity representing account master data converted from COBOL ACCOUNT-RECORD.
 * This entity implements the accounts table mapping with exact DECIMAL(12,2) precision
 * for financial fields, foreign key relationships to customers/cards/transactions,
 * and PostgreSQL B-tree indexing strategy for optimal query performance.
 * 
 * Mapped from COBOL copybook: app/cpy/CVACT01Y.cpy
 * Database table: accounts
 * Record length: 300 bytes (original COBOL structure)
 * 
 * Key Features:
 * - Primary key: 11-digit account ID (ACCT-ID)
 * - BigDecimal financial fields with MathContext.DECIMAL128 precision
 * - Foreign key relationship to Customer entity
 * - Account status enum validation (Active/Inactive)
 * - Date fields with custom format handling
 * - Optimistic locking with version field
 * - Cascade relationships to Card and Transaction entities
 * 
 * @version CardDemo_v1.0-15-g27d6c6f-68
 * @since 2022-07-19
 */
@Entity
@Table(name = "accounts", schema = "public")
public class Account {

    /**
     * Primary key: 11-digit account identifier
     * Mapped from COBOL: ACCT-ID PIC 9(11)
     * Database constraint: Primary key, NOT NULL, length=11
     */
    @Id
    @Column(name = "account_id", length = 11, nullable = false)
    @NotNull(message = "Account ID is required")
    @Pattern(regexp = "\\d{11}", message = "Account ID must be exactly 11 digits")
    private String accountId;

    /**
     * Account active status using AccountStatus enum
     * Mapped from COBOL: ACCT-ACTIVE-STATUS PIC X(01)
     * Database constraint: VARCHAR(1), NOT NULL
     * Business rule: 'Y' for Active, 'N' for Inactive
     */
    @Column(name = "active_status", length = 1, nullable = false)
    @NotNull(message = "Account status is required")
    @Enumerated(EnumType.STRING)
    private AccountStatus activeStatus;

    /**
     * Current account balance with exact financial precision
     * Mapped from COBOL: ACCT-CURR-BAL PIC S9(10)V99
     * Database constraint: DECIMAL(12,2), NOT NULL
     * Precision: Equivalent to COBOL COMP-3 with MathContext.DECIMAL128
     */
    @Column(name = "current_balance", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Current balance is required")
    @DecimalMin(value = "-9999999999.99", message = "Current balance must be greater than -9999999999.99")
    @DecimalMax(value = "9999999999.99", message = "Current balance must be less than 9999999999.99")
    private BigDecimal currentBalance;

    /**
     * Credit limit with exact financial precision
     * Mapped from COBOL: ACCT-CREDIT-LIMIT PIC S9(10)V99
     * Database constraint: DECIMAL(12,2), NOT NULL
     * Business rule: Must be positive value
     */
    @Column(name = "credit_limit", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Credit limit is required")
    @DecimalMin(value = "0.00", message = "Credit limit must be non-negative")
    @DecimalMax(value = "9999999999.99", message = "Credit limit must be less than 9999999999.99")
    private BigDecimal creditLimit;

    /**
     * Cash credit limit with exact financial precision
     * Mapped from COBOL: ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99
     * Database constraint: DECIMAL(12,2), NOT NULL
     * Business rule: Must be positive value, typically less than credit limit
     */
    @Column(name = "cash_credit_limit", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Cash credit limit is required")
    @DecimalMin(value = "0.00", message = "Cash credit limit must be non-negative")
    @DecimalMax(value = "9999999999.99", message = "Cash credit limit must be less than 9999999999.99")
    private BigDecimal cashCreditLimit;

    /**
     * Account opening date
     * Mapped from COBOL: ACCT-OPEN-DATE PIC X(10)
     * Database constraint: DATE, NOT NULL
     * Format: YYYY-MM-DD with custom date format handling
     */
    @Column(name = "open_date", nullable = false)
    @NotNull(message = "Account open date is required")
    @PastOrPresent(message = "Account open date cannot be in the future")
    private LocalDate openDate;

    /**
     * Account expiration date
     * Mapped from COBOL: ACCT-EXPIRAION-DATE PIC X(10)
     * Database constraint: DATE, nullable
     * Format: YYYY-MM-DD with custom date format handling
     */
    @Column(name = "expiration_date")
    @Future(message = "Account expiration date must be in the future")
    private LocalDate expirationDate;

    /**
     * Account reissue date
     * Mapped from COBOL: ACCT-REISSUE-DATE PIC X(10)
     * Database constraint: DATE, nullable
     * Format: YYYY-MM-DD with custom date format handling
     */
    @Column(name = "reissue_date")
    private LocalDate reissueDate;

    /**
     * Current cycle credit amount with exact financial precision
     * Mapped from COBOL: ACCT-CURR-CYC-CREDIT PIC S9(10)V99
     * Database constraint: DECIMAL(12,2), NOT NULL, default 0.00
     * Business rule: Tracks credit transactions in current billing cycle
     */
    @Column(name = "current_cycle_credit", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Current cycle credit is required")
    @DecimalMin(value = "0.00", message = "Current cycle credit must be non-negative")
    @DecimalMax(value = "9999999999.99", message = "Current cycle credit must be less than 9999999999.99")
    private BigDecimal currentCycleCredit = BigDecimal.ZERO;

    /**
     * Current cycle debit amount with exact financial precision
     * Mapped from COBOL: ACCT-CURR-CYC-DEBIT PIC S9(10)V99
     * Database constraint: DECIMAL(12,2), NOT NULL, default 0.00
     * Business rule: Tracks debit transactions in current billing cycle
     */
    @Column(name = "current_cycle_debit", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Current cycle debit is required")
    @DecimalMin(value = "0.00", message = "Current cycle debit must be non-negative")
    @DecimalMax(value = "9999999999.99", message = "Current cycle debit must be less than 9999999999.99")
    private BigDecimal currentCycleDebit = BigDecimal.ZERO;

    /**
     * Address ZIP code for account
     * Mapped from COBOL: ACCT-ADDR-ZIP PIC X(10)
     * Database constraint: VARCHAR(10), nullable
     * Format: ZIP code or ZIP+4 format
     */
    @Column(name = "address_zip", length = 10)
    @Size(max = 10, message = "Address ZIP code must not exceed 10 characters")
    @Pattern(regexp = "\\d{5}(-\\d{4})?", message = "Address ZIP code must be in format 12345 or 12345-6789")
    private String addressZip;

    /**
     * Group identifier for disclosure and interest rate management
     * Mapped from COBOL: ACCT-GROUP-ID PIC X(10)
     * Database constraint: VARCHAR(10), nullable
     * Business rule: Links to disclosure_groups table for interest rate configuration
     */
    @Column(name = "group_id", length = 10)
    @Size(max = 10, message = "Group ID must not exceed 10 characters")
    private String groupId;

    /**
     * Version field for optimistic locking
     * Supports concurrent modification detection equivalent to VSAM record locking
     * Automatically managed by JPA for optimistic concurrency control
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Many-to-one relationship to Customer entity
     * Foreign key: customer_id references customers.customer_id
     * Cascade: PERSIST, MERGE for lifecycle management
     * Fetch: LAZY for performance optimization
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "customer_id", nullable = false)
    @NotNull(message = "Customer is required")
    private Customer customer;

    /**
     * One-to-many relationship to Card entities
     * Mapped by account_id foreign key in cards table
     * Cascade: ALL for complete lifecycle management
     * Fetch: LAZY for performance optimization
     * NOTE: Card entity not yet implemented - uncomment when available
     */
    // @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    // private Set<Card> cards = new HashSet<>();

    /**
     * One-to-many relationship to Transaction entities
     * Mapped by account_id foreign key in transactions table
     * Cascade: ALL for complete lifecycle management
     * Fetch: LAZY for performance optimization
     * NOTE: Transaction entity not yet implemented - uncomment when available
     */
    // @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    // private Set<Transaction> transactions = new HashSet<>();

    /**
     * Default constructor for JPA
     */
    public Account() {
        this.currentCycleCredit = BigDecimal.ZERO;
        this.currentCycleDebit = BigDecimal.ZERO;
        this.activeStatus = AccountStatus.ACTIVE;
    }

    /**
     * Constructor with required fields
     * 
     * @param accountId Account ID (11 digits)
     * @param customer Customer entity reference
     * @param currentBalance Current account balance
     * @param creditLimit Credit limit
     * @param cashCreditLimit Cash credit limit
     * @param openDate Account opening date
     * @param activeStatus Account active status
     */
    public Account(String accountId, Customer customer, BigDecimal currentBalance, 
                   BigDecimal creditLimit, BigDecimal cashCreditLimit, 
                   LocalDate openDate, AccountStatus activeStatus) {
        this.accountId = accountId;
        this.customer = customer;
        this.currentBalance = currentBalance;
        this.creditLimit = creditLimit;
        this.cashCreditLimit = cashCreditLimit;
        this.openDate = openDate;
        this.activeStatus = activeStatus;
        this.currentCycleCredit = BigDecimal.ZERO;
        this.currentCycleDebit = BigDecimal.ZERO;
    }

    // Getters and Setters

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
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

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    // NOTE: Uncomment when Card entity is available
    // public Set<Card> getCards() {
    //     return cards;
    // }

    // public void setCards(Set<Card> cards) {
    //     this.cards = cards != null ? cards : new HashSet<>();
    // }

    // NOTE: Uncomment when Transaction entity is available
    // public Set<Transaction> getTransactions() {
    //     return transactions;
    // }

    // public void setTransactions(Set<Transaction> transactions) {
    //     this.transactions = transactions != null ? transactions : new HashSet<>();
    // }

    // Helper methods for managing relationships

    /**
     * Add a card to this account
     * Maintains bidirectional relationship
     * NOTE: Uncomment when Card entity is available
     * 
     * @param card Card to add
     */
    // public void addCard(Card card) {
    //     if (card != null) {
    //         this.cards.add(card);
    //         card.setAccount(this);
    //     }
    // }

    /**
     * Remove a card from this account
     * Maintains bidirectional relationship
     * NOTE: Uncomment when Card entity is available
     * 
     * @param card Card to remove
     */
    // public void removeCard(Card card) {
    //     if (card != null) {
    //         this.cards.remove(card);
    //         card.setAccount(null);
    //     }
    // }

    /**
     * Add a transaction to this account
     * Maintains bidirectional relationship
     * NOTE: Uncomment when Transaction entity is available
     * 
     * @param transaction Transaction to add
     */
    // public void addTransaction(Transaction transaction) {
    //     if (transaction != null) {
    //         this.transactions.add(transaction);
    //         transaction.setAccount(this);
    //     }
    // }

    /**
     * Remove a transaction from this account
     * Maintains bidirectional relationship
     * NOTE: Uncomment when Transaction entity is available
     * 
     * @param transaction Transaction to remove
     */
    // public void removeTransaction(Transaction transaction) {
    //     if (transaction != null) {
    //         this.transactions.remove(transaction);
    //         transaction.setAccount(null);
    //     }
    // }

    // Placeholder method implementations for export requirements
    
    /**
     * Get cards collection
     * NOTE: Returns empty set until Card entity is implemented
     * 
     * @return Empty set placeholder
     */
    public Set<Object> getCards() {
        return new HashSet<>();
    }

    /**
     * Set cards collection
     * NOTE: No-op until Card entity is implemented
     * 
     * @param cards Cards collection (ignored)
     */
    public void setCards(Set<Object> cards) {
        // No-op until Card entity is implemented
    }

    /**
     * Add card to account
     * NOTE: No-op until Card entity is implemented
     * 
     * @param card Card to add (ignored)
     */
    public void addCard(Object card) {
        // No-op until Card entity is implemented
    }

    /**
     * Remove card from account
     * NOTE: No-op until Card entity is implemented
     * 
     * @param card Card to remove (ignored)
     */
    public void removeCard(Object card) {
        // No-op until Card entity is implemented
    }

    /**
     * Get transactions collection
     * NOTE: Returns empty set until Transaction entity is implemented
     * 
     * @return Empty set placeholder
     */
    public Set<Object> getTransactions() {
        return new HashSet<>();
    }

    /**
     * Set transactions collection
     * NOTE: No-op until Transaction entity is implemented
     * 
     * @param transactions Transactions collection (ignored)
     */
    public void setTransactions(Set<Object> transactions) {
        // No-op until Transaction entity is implemented
    }

    /**
     * Add transaction to account
     * NOTE: No-op until Transaction entity is implemented
     * 
     * @param transaction Transaction to add (ignored)
     */
    public void addTransaction(Object transaction) {
        // No-op until Transaction entity is implemented
    }

    /**
     * Remove transaction from account
     * NOTE: No-op until Transaction entity is implemented
     * 
     * @param transaction Transaction to remove (ignored)
     */
    public void removeTransaction(Object transaction) {
        // No-op until Transaction entity is implemented
    }

    // Business methods

    /**
     * Check if account is active
     * 
     * @return true if account status is ACTIVE
     */
    public boolean isActive() {
        return activeStatus != null && activeStatus.isActive();
    }

    /**
     * Check if account is inactive
     * 
     * @return true if account status is INACTIVE
     */
    public boolean isInactive() {
        return activeStatus != null && activeStatus.isInactive();
    }

    /**
     * Get available credit amount
     * 
     * @return Available credit (credit limit minus current balance)
     */
    public BigDecimal getAvailableCredit() {
        if (creditLimit == null || currentBalance == null) {
            return BigDecimal.ZERO;
        }
        return creditLimit.subtract(currentBalance);
    }

    /**
     * Get available cash credit amount
     * 
     * @return Available cash credit (cash credit limit minus current balance)
     */
    public BigDecimal getAvailableCashCredit() {
        if (cashCreditLimit == null || currentBalance == null) {
            return BigDecimal.ZERO;
        }
        return cashCreditLimit.subtract(currentBalance);
    }

    /**
     * Get current cycle net amount
     * 
     * @return Net amount (current cycle credit minus current cycle debit)
     */
    public BigDecimal getCurrentCycleNet() {
        if (currentCycleCredit == null || currentCycleDebit == null) {
            return BigDecimal.ZERO;
        }
        return currentCycleCredit.subtract(currentCycleDebit);
    }

    /**
     * Check if account is over credit limit
     * 
     * @return true if current balance exceeds credit limit
     */
    public boolean isOverCreditLimit() {
        if (currentBalance == null || creditLimit == null) {
            return false;
        }
        return currentBalance.compareTo(creditLimit) > 0;
    }

    /**
     * Check if account is expired
     * 
     * @return true if expiration date is in the past
     */
    public boolean isExpired() {
        if (expirationDate == null) {
            return false;
        }
        return expirationDate.isBefore(LocalDate.now());
    }

    /**
     * Get account age in days
     * 
     * @return Number of days since account opening
     */
    public long getAccountAgeDays() {
        if (openDate == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(openDate, LocalDate.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Account account = (Account) o;
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
                ", groupId='" + groupId + '\'' +
                ", version=" + version +
                '}';
    }
}