package com.carddemo.account.entity;

import com.carddemo.common.enums.AccountStatus;
import com.carddemo.common.converter.AccountStatusConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA Entity representing account master data converted from COBOL ACCOUNT-RECORD.
 * Maintains exact field mappings and financial precision from the original VSAM ACCTDAT file
 * while providing modern JPA capabilities, PostgreSQL performance optimization, and microservices integration.
 * 
 * Original COBOL Structure: ACCOUNT-RECORD from CVACT01Y.cpy (RECLN 300)
 * Target Table: accounts
 * 
 * Key Features:
 * - BigDecimal financial precision with MathContext.DECIMAL128 for COBOL COMP-3 equivalence
 * - Foreign key relationships maintaining VSAM cross-reference functionality
 * - PostgreSQL B-tree indexing strategy for optimal query performance
 * - Jakarta Bean Validation for business rule enforcement
 * - Optimistic locking for concurrent modification detection
 * - Cascade relationships with cards and transactions entities
 * 
 * Performance Requirements:
 * - Sub-200ms response times for account lookup operations at 95th percentile
 * - Support for 10,000+ TPS throughput with horizontal scaling
 * - Memory usage within 10% increase limit compared to CICS allocation
 * - Transaction isolation level SERIALIZABLE for VSAM-equivalent locking
 * 
 * Business Rules Enforced:
 * - Account ID must be exactly 11 digits (COBOL PIC 9(11) constraint)
 * - Financial amounts maintain exact decimal precision equivalent to COBOL COMP-3
 * - Active status validation through AccountStatus enumeration
 * - Date fields support COBOL date format conversion with validation
 * - Credit limits must be non-negative with business rule validation
 * 
 * @author Blitzy Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "account_data", 
       indexes = {
           @Index(name = "idx_customer_account_xref", columnList = "customer_id, account_id"),
           @Index(name = "idx_account_balance", columnList = "account_id, current_balance"),
           @Index(name = "idx_account_status", columnList = "active_status"),
           @Index(name = "idx_account_group", columnList = "group_id"),
           @Index(name = "idx_account_zip", columnList = "address_zip"),
           @Index(name = "idx_account_dates", columnList = "open_date, expiration_date")
       })
public class Account {

    /**
     * MathContext for financial calculations maintaining COBOL COMP-3 precision
     * Uses DECIMAL128 precision with HALF_EVEN rounding to ensure exact financial arithmetic
     */
    public static final MathContext COBOL_DECIMAL_CONTEXT = MathContext.DECIMAL128;

    /**
     * Date formatter for COBOL date format conversion (YYYY-MM-DD)
     * Supports conversion from COBOL PIC X(10) date fields
     */
    private static final DateTimeFormatter COBOL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Account ID - Primary Key
     * Mapped from COBOL: ACCT-ID PIC 9(11)
     * Constraint: Exactly 11 digits as per mainframe format
     */
    @Id
    @Column(name = "account_id", length = 11, nullable = false)
    @NotNull(message = "Account ID is required")
    @Pattern(regexp = "^[0-9]{11}$", message = "Account ID must be exactly 11 digits")
    private String accountId;

    /**
     * Customer Reference - Foreign Key to customers table
     * Establishes @ManyToOne relationship with Customer entity
     * Mapped from business requirement for customer-account association
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, 
                foreignKey = @ForeignKey(name = "fk_account_customer"))
    @NotNull(message = "Customer reference is required")
    private Customer customer;

    /**
     * Account Active Status
     * Mapped from COBOL: ACCT-ACTIVE-STATUS PIC X(01)
     * Uses AccountStatus enumeration for 'Y'/'N' validation
     */
    @Convert(converter = AccountStatusConverter.class)
    @Column(name = "active_status", length = 1, nullable = false)
    @NotNull(message = "Account status is required")
    private AccountStatus activeStatus;

    /**
     * Current Account Balance
     * Mapped from COBOL: ACCT-CURR-BAL PIC S9(10)V99 COMP-3
     * Uses BigDecimal with DECIMAL(12,2) precision for exact financial calculations
     */
    @Column(name = "current_balance", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Current balance is required")
    @DecimalMin(value = "-9999999999.99", message = "Current balance cannot be less than -9999999999.99")
    @DecimalMax(value = "9999999999.99", message = "Current balance cannot exceed 9999999999.99")
    private BigDecimal currentBalance;

    /**
     * Credit Limit
     * Mapped from COBOL: ACCT-CREDIT-LIMIT PIC S9(10)V99 COMP-3
     * Uses BigDecimal with DECIMAL(12,2) precision for exact credit limit management
     */
    @Column(name = "credit_limit", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Credit limit is required")
    @DecimalMin(value = "0.00", message = "Credit limit must be non-negative")
    @DecimalMax(value = "9999999999.99", message = "Credit limit cannot exceed 9999999999.99")
    private BigDecimal creditLimit;

    /**
     * Cash Credit Limit
     * Mapped from COBOL: ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99 COMP-3
     * Uses BigDecimal with DECIMAL(12,2) precision for exact cash advance limit management
     */
    @Column(name = "cash_credit_limit", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Cash credit limit is required")
    @DecimalMin(value = "0.00", message = "Cash credit limit must be non-negative")
    @DecimalMax(value = "9999999999.99", message = "Cash credit limit cannot exceed 9999999999.99")
    private BigDecimal cashCreditLimit;

    /**
     * Account Open Date
     * Mapped from COBOL: ACCT-OPEN-DATE PIC X(10)
     * Converted from string format to LocalDate for proper date handling
     */
    @Column(name = "open_date", nullable = false)
    @NotNull(message = "Account open date is required")
    @PastOrPresent(message = "Account open date cannot be in the future")
    private LocalDate openDate;

    /**
     * Account Expiration Date
     * Mapped from COBOL: ACCT-EXPIRAION-DATE PIC X(10)
     * Converted from string format to LocalDate for proper date handling
     */
    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    /**
     * Account Reissue Date
     * Mapped from COBOL: ACCT-REISSUE-DATE PIC X(10)
     * Converted from string format to LocalDate for proper date handling
     */
    @Column(name = "reissue_date")
    private LocalDate reissueDate;

    /**
     * Current Cycle Credit
     * Mapped from COBOL: ACCT-CURR-CYC-CREDIT PIC S9(10)V99 COMP-3
     * Uses BigDecimal with DECIMAL(12,2) precision for exact cycle credit tracking
     */
    @Column(name = "current_cycle_credit", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Current cycle credit is required")
    @DecimalMin(value = "0.00", message = "Current cycle credit must be non-negative")
    @DecimalMax(value = "9999999999.99", message = "Current cycle credit cannot exceed 9999999999.99")
    private BigDecimal currentCycleCredit;

    /**
     * Current Cycle Debit
     * Mapped from COBOL: ACCT-CURR-CYC-DEBIT PIC S9(10)V99 COMP-3
     * Uses BigDecimal with DECIMAL(12,2) precision for exact cycle debit tracking
     */
    @Column(name = "current_cycle_debit", precision = 12, scale = 2, nullable = false)
    @NotNull(message = "Current cycle debit is required")
    @DecimalMin(value = "0.00", message = "Current cycle debit must be non-negative")
    @DecimalMax(value = "9999999999.99", message = "Current cycle debit cannot exceed 9999999999.99")
    private BigDecimal currentCycleDebit;

    /**
     * Account Address ZIP Code
     * Mapped from COBOL: ACCT-ADDR-ZIP PIC X(10)
     */
    @Column(name = "address_zip", length = 10)
    @Size(max = 10, message = "Address ZIP code cannot exceed 10 characters")
    @Pattern(regexp = "^[0-9]{5}(-[0-9]{4})?$|^$", message = "ZIP code must be in format 12345 or 12345-6789")
    private String addressZip;

    /**
     * Account Group ID for disclosure group association
     * Mapped from COBOL: ACCT-GROUP-ID PIC X(10)
     */
    @Column(name = "group_id", length = 10)
    @Size(max = 10, message = "Group ID cannot exceed 10 characters")
    private String groupId;

    /**
     * Version field for optimistic locking
     * Enables concurrent modification detection equivalent to VSAM record locking
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Account-Card Relationship
     * @OneToMany with cascade operations for card lifecycle management
     * Implements bidirectional relationship with lazy loading for performance
     */
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Card> cards = new HashSet<>();

    /**
     * Account-Transaction Relationship
     * @OneToMany with cascade operations for transaction lifecycle management
     * Implements bidirectional relationship with lazy loading for performance
     */
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Transaction> transactions = new HashSet<>();

    // Default Constructor
    public Account() {
        this.cards = new HashSet<>();
        this.transactions = new HashSet<>();
        this.currentBalance = BigDecimal.ZERO;
        this.creditLimit = BigDecimal.ZERO;
        this.cashCreditLimit = BigDecimal.ZERO;
        this.currentCycleCredit = BigDecimal.ZERO;
        this.currentCycleDebit = BigDecimal.ZERO;
        this.activeStatus = AccountStatus.ACTIVE;
    }

    /**
     * Parameterized Constructor for essential account fields
     * Supports account creation with required business data
     */
    public Account(String accountId, Customer customer, BigDecimal currentBalance, 
                   BigDecimal creditLimit, BigDecimal cashCreditLimit, LocalDate openDate) {
        this();
        this.accountId = accountId;
        this.customer = customer;
        this.currentBalance = currentBalance;
        this.creditLimit = creditLimit;
        this.cashCreditLimit = cashCreditLimit;
        this.openDate = openDate;
        this.activeStatus = AccountStatus.ACTIVE;
    }

    // Getter and Setter Methods

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

    // Relationship Management Methods

    public Set<Card> getCards() {
        return cards;
    }

    public void setCards(Set<Card> cards) {
        this.cards = cards != null ? cards : new HashSet<>();
    }

    /**
     * Add a card to this account with bidirectional relationship management
     * Maintains referential integrity and cascade operations
     * 
     * @param card Card entity to associate with this account
     */
    public void addCard(Card card) {
        if (card != null) {
            this.cards.add(card);
            card.setAccount(this);
        }
    }

    /**
     * Remove a card from this account with bidirectional relationship management
     * Maintains referential integrity and cascade operations
     * 
     * @param card Card entity to dissociate from this account
     */
    public void removeCard(Card card) {
        if (card != null) {
            this.cards.remove(card);
            card.setAccount(null);
        }
    }

    public Set<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(Set<Transaction> transactions) {
        this.transactions = transactions != null ? transactions : new HashSet<>();
    }

    /**
     * Add a transaction to this account with bidirectional relationship management
     * Maintains referential integrity and cascade operations
     * 
     * @param transaction Transaction entity to associate with this account
     */
    public void addTransaction(Transaction transaction) {
        if (transaction != null) {
            this.transactions.add(transaction);
            transaction.setAccount(this);
        }
    }

    /**
     * Remove a transaction from this account with bidirectional relationship management
     * Maintains referential integrity and cascade operations
     * 
     * @param transaction Transaction entity to dissociate from this account
     */
    public void removeTransaction(Transaction transaction) {
        if (transaction != null) {
            this.transactions.remove(transaction);
            transaction.setAccount(null);
        }
    }

    // Business Logic Methods

    /**
     * Calculate available credit based on current balance and credit limit
     * Replicates COBOL financial calculation logic with exact precision
     * 
     * @return Available credit amount as BigDecimal
     */
    public BigDecimal getAvailableCredit() {
        if (creditLimit == null || currentBalance == null) {
            return BigDecimal.ZERO;
        }
        return creditLimit.subtract(currentBalance, COBOL_DECIMAL_CONTEXT);
    }

    /**
     * Calculate available cash credit based on current balance and cash credit limit
     * Replicates COBOL financial calculation logic with exact precision
     * 
     * @return Available cash credit amount as BigDecimal
     */
    public BigDecimal getAvailableCashCredit() {
        if (cashCreditLimit == null || currentBalance == null) {
            return BigDecimal.ZERO;
        }
        return cashCreditLimit.subtract(currentBalance, COBOL_DECIMAL_CONTEXT);
    }

    /**
     * Check if account is currently active
     * Replicates COBOL 88-level condition logic for account status validation
     * 
     * @return true if account status is ACTIVE, false otherwise
     */
    public boolean isActive() {
        return activeStatus != null && activeStatus.isActive();
    }

    /**
     * Check if account is currently inactive
     * Replicates COBOL 88-level condition logic for account status validation
     * 
     * @return true if account status is INACTIVE, false otherwise
     */
    public boolean isInactive() {
        return activeStatus != null && activeStatus.isInactive();
    }

    /**
     * Check if account has expired based on expiration date
     * Provides business rule validation for account lifecycle management
     * 
     * @return true if account has expired, false otherwise
     */
    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDate.now());
    }

    /**
     * Check if credit limit has been exceeded
     * Provides business rule validation for credit management
     * 
     * @return true if current balance exceeds credit limit, false otherwise
     */
    public boolean isCreditLimitExceeded() {
        if (creditLimit == null || currentBalance == null) {
            return false;
        }
        return currentBalance.compareTo(creditLimit) > 0;
    }

    /**
     * Calculate credit utilization percentage (0-100%)
     * Provides business intelligence for credit management
     * 
     * @return Credit utilization percentage as BigDecimal
     */
    public BigDecimal getCreditUtilizationPercentage() {
        if (creditLimit == null || creditLimit.compareTo(BigDecimal.ZERO) == 0 || currentBalance == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal utilizationRatio = currentBalance.divide(creditLimit, COBOL_DECIMAL_CONTEXT);
        return utilizationRatio.multiply(BigDecimal.valueOf(100), COBOL_DECIMAL_CONTEXT);
    }

    /**
     * Format date for COBOL compatibility
     * Converts LocalDate to COBOL PIC X(10) format (YYYY-MM-DD)
     * 
     * @param date LocalDate to format
     * @return Formatted date string or null if date is null
     */
    public static String formatDateForCobol(LocalDate date) {
        return date != null ? date.format(COBOL_DATE_FORMAT) : null;
    }

    /**
     * Parse date from COBOL format
     * Converts COBOL PIC X(10) format (YYYY-MM-DD) to LocalDate
     * 
     * @param dateString Date string in COBOL format
     * @return Parsed LocalDate or null if string is null/empty
     * @throws DateTimeParseException if date string is invalid
     */
    public static LocalDate parseDateFromCobol(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(dateString.trim(), COBOL_DATE_FORMAT);
    }

    // Standard Object Methods

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Account account = (Account) obj;
        return accountId != null && accountId.equals(account.accountId);
    }

    @Override
    public int hashCode() {
        return accountId != null ? accountId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountId='" + accountId + '\'' +
                ", customer=" + (customer != null ? customer.getCustomerId() : "null") +
                ", activeStatus=" + activeStatus +
                ", currentBalance=" + currentBalance +
                ", creditLimit=" + creditLimit +
                ", cashCreditLimit=" + cashCreditLimit +
                ", openDate=" + openDate +
                ", expirationDate=" + expirationDate +
                ", version=" + version +
                ", cardCount=" + (cards != null ? cards.size() : 0) +
                ", transactionCount=" + (transactions != null ? transactions.size() : 0) +
                '}';
    }
}