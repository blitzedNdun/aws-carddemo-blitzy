package com.carddemo.common.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.io.Serializable;

/**
 * JPA Account entity mapping COBOL ACCOUNT-RECORD to PostgreSQL accounts table
 * with financial balances using BigDecimal precision, account status tracking,
 * date management, and foreign key relationships to customers and disclosure groups
 */
@Entity(name = "CommonAccount")
@Table(name = "accounts")
public class Account implements Serializable {

    @Id
    @Column(name = "account_id", length = 11)
    @Size(min = 11, max = 11, message = "Account ID must be exactly 11 characters")
    private String accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "customer_id", length = 9, insertable = false, updatable = false)
    private String customerId;

    @Column(name = "current_balance", precision = 12, scale = 2)
    @DecimalMin(value = "0.00", message = "Current balance must be non-negative")
    private BigDecimal currentBalance;

    @Column(name = "credit_limit", precision = 12, scale = 2)
    @DecimalMin(value = "0.00", message = "Credit limit must be non-negative")
    private BigDecimal creditLimit;

    @Column(name = "cash_credit_limit", precision = 12, scale = 2)
    @DecimalMin(value = "0.00", message = "Cash credit limit must be non-negative")
    private BigDecimal cashCreditLimit;

    @Column(name = "active_status")
    @NotNull(message = "Active status cannot be null")
    private Boolean activeStatus;

    @Column(name = "open_date")
    private LocalDate openDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "reissue_date")
    private LocalDate reissueDate;

    @Column(name = "current_cycle_credit", precision = 12, scale = 2)
    @DecimalMin(value = "0.00", message = "Current cycle credit must be non-negative")
    private BigDecimal currentCycleCredit;

    @Column(name = "current_cycle_debit", precision = 12, scale = 2)
    @DecimalMin(value = "0.00", message = "Current cycle debit must be non-negative")
    private BigDecimal currentCycleDebit;

    @Column(name = "address_zip", length = 10)
    @Size(max = 10, message = "Address zip must not exceed 10 characters")
    private String addressZip;

    @Column(name = "group_id", length = 10)
    @Size(max = 10, message = "Group ID must not exceed 10 characters")
    private String groupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", referencedColumnName = "group_id", insertable = false, updatable = false)
    private DisclosureGroup disclosureGroup;

    @Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Card> cards;

    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Transaction> transactions;

    // Default constructor
    public Account() {
        this.currentBalance = BigDecimal.ZERO;
        this.creditLimit = BigDecimal.ZERO;
        this.cashCreditLimit = BigDecimal.ZERO;
        this.currentCycleCredit = BigDecimal.ZERO;
        this.currentCycleDebit = BigDecimal.ZERO;
        this.activeStatus = true;
        this.openDate = LocalDate.now();
    }

    // Constructor with account ID
    public Account(String accountId) {
        this();
        this.accountId = accountId;
    }

    // Getters and setters
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { 
        this.customer = customer;
        if (customer != null) {
            this.customerId = customer.getCustomerId();
        }
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }

    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }

    public BigDecimal getCashCreditLimit() { return cashCreditLimit; }
    public void setCashCreditLimit(BigDecimal cashCreditLimit) { this.cashCreditLimit = cashCreditLimit; }

    public Boolean getActiveStatus() { return activeStatus; }
    public void setActiveStatus(Boolean activeStatus) { this.activeStatus = activeStatus; }

    public LocalDate getOpenDate() { return openDate; }
    public void setOpenDate(LocalDate openDate) { this.openDate = openDate; }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    public LocalDate getReissueDate() { return reissueDate; }
    public void setReissueDate(LocalDate reissueDate) { this.reissueDate = reissueDate; }

    public BigDecimal getCurrentCycleCredit() { return currentCycleCredit; }
    public void setCurrentCycleCredit(BigDecimal currentCycleCredit) { this.currentCycleCredit = currentCycleCredit; }

    public BigDecimal getCurrentCycleDebit() { return currentCycleDebit; }
    public void setCurrentCycleDebit(BigDecimal currentCycleDebit) { this.currentCycleDebit = currentCycleDebit; }

    public String getAddressZip() { return addressZip; }
    public void setAddressZip(String addressZip) { this.addressZip = addressZip; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public DisclosureGroup getDisclosureGroup() { return disclosureGroup; }
    public void setDisclosureGroup(DisclosureGroup disclosureGroup) { 
        this.disclosureGroup = disclosureGroup;
        if (disclosureGroup != null) {
            this.groupId = disclosureGroup.getGroupId();
        }
    }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public List<Card> getCards() { return cards; }
    public void setCards(List<Card> cards) { this.cards = cards; }

    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }

    // Utility methods
    public BigDecimal getCreditUtilizationPercentage() {
        if (creditLimit == null || creditLimit.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (currentBalance == null) {
            return BigDecimal.ZERO;
        }
        return currentBalance.divide(creditLimit, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    // Utility methods
    public boolean isActive() {
        return Boolean.TRUE.equals(activeStatus);
    }

    public BigDecimal getAvailableCredit() {
        return creditLimit.subtract(currentBalance);
    }

    public BigDecimal getAvailableCashCredit() {
        return cashCreditLimit.subtract(currentBalance);
    }

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
                ", customerId='" + customerId + '\'' +
                ", currentBalance=" + currentBalance +
                ", creditLimit=" + creditLimit +
                ", activeStatus=" + activeStatus +
                '}';
    }
}