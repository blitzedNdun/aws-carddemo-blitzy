package com.carddemo.common.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing the disclosure_groups table that maps COBOL disclosure 
 * group structure to PostgreSQL for interest rate configuration and disclosure management.
 * 
 * This entity supports:
 * - Interest rate configuration and disclosure management per Section 6.2.1.2
 * - Precise interest rate calculations using BigDecimal arithmetic per Section 0.3.2
 * - Compliance with banking regulations for interest rate disclosure per Section 6.2.3.1
 * 
 * Maps to PostgreSQL table: disclosure_groups
 * Original COBOL structure: DIS-GROUP-RECORD in CVTRA02Y.cpy
 */
@Entity
@Table(name = "disclosure_groups")
public class DisclosureGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key - Group identifier for interest rate group classification
     * Maps to DIS-ACCT-GROUP-ID PIC X(10) from COBOL copybook
     * PostgreSQL: VARCHAR(10)
     */
    @Id
    @Column(name = "group_id", length = 10, nullable = false)
    @NotNull(message = "Group ID cannot be null")
    @Size(min = 1, max = 10, message = "Group ID must be between 1 and 10 characters")
    private String groupId;

    /**
     * Legal disclosure text for compliance with banking regulations
     * Stores comprehensive disclosure content for interest rate terms
     * PostgreSQL: TEXT
     */
    @Column(name = "disclosure_text", columnDefinition = "TEXT")
    @NotNull(message = "Disclosure text cannot be null")
    @Size(min = 1, max = 1000, message = "Disclosure text must be between 1 and 1000 characters")
    private String disclosureText;

    /**
     * Interest rate with precise decimal arithmetic for financial calculations
     * Maps to DIS-INT-RATE PIC S9(04)V99 from COBOL copybook
     * PostgreSQL: DECIMAL(5,4) - supports rates from 0.0001 to 9.9999 (0.01% to 999.99%)
     * Uses BigDecimal to maintain COBOL COMP-3 precision per Section 0.3.2
     */
    @Column(name = "interest_rate", precision = 5, scale = 4, nullable = false)
    @NotNull(message = "Interest rate cannot be null")
    @DecimalMin(value = "0.0001", message = "Interest rate must be at least 0.0001 (0.01%)")
    @DecimalMax(value = "9.9999", message = "Interest rate must not exceed 9.9999 (999.99%)")
    private BigDecimal interestRate;

    /**
     * Effective date for rate change tracking and historical analysis
     * Supports temporal tracking of interest rate modifications
     * PostgreSQL: TIMESTAMP
     */
    @Column(name = "effective_date", nullable = false)
    @NotNull(message = "Effective date cannot be null")
    private LocalDateTime effectiveDate;

    /**
     * One-to-many relationship with Account entity for interest rate application
     * Enables one disclosure group to apply to multiple accounts per Section 6.2.1.1
     * Bidirectional relationship mapped by "disclosureGroup" field in Account entity
     */
    @OneToMany(mappedBy = "disclosureGroup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Account> accounts = new ArrayList<>();

    /**
     * Default constructor for JPA
     */
    public DisclosureGroup() {
        // Default constructor required by JPA
    }

    /**
     * Constructor with essential fields for disclosure group creation
     * 
     * @param groupId Group identifier for classification
     * @param disclosureText Legal disclosure content
     * @param interestRate Interest rate with BigDecimal precision
     * @param effectiveDate Effective date for rate application
     */
    public DisclosureGroup(String groupId, String disclosureText, BigDecimal interestRate, LocalDateTime effectiveDate) {
        this.groupId = groupId;
        this.disclosureText = disclosureText;
        this.interestRate = interestRate;
        this.effectiveDate = effectiveDate;
        this.accounts = new ArrayList<>();
    }

    /**
     * Gets the group identifier
     * 
     * @return Group ID string (max 10 characters)
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Sets the group identifier
     * 
     * @param groupId Group ID string (max 10 characters)
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * Gets the legal disclosure text
     * 
     * @return Disclosure text content
     */
    public String getDisclosureText() {
        return disclosureText;
    }

    /**
     * Sets the legal disclosure text
     * 
     * @param disclosureText Disclosure text content
     */
    public void setDisclosureText(String disclosureText) {
        this.disclosureText = disclosureText;
    }

    /**
     * Gets the interest rate with BigDecimal precision
     * Supports exact decimal arithmetic for financial calculations
     * 
     * @return Interest rate as BigDecimal with DECIMAL(5,4) precision
     */
    public BigDecimal getInterestRate() {
        return interestRate;
    }

    /**
     * Sets the interest rate with BigDecimal precision
     * Maintains COBOL COMP-3 precision for financial calculations
     * 
     * @param interestRate Interest rate as BigDecimal
     */
    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    /**
     * Gets the effective date for rate change tracking
     * 
     * @return Effective date as LocalDateTime
     */
    public LocalDateTime getEffectiveDate() {
        return effectiveDate;
    }

    /**
     * Sets the effective date for rate change tracking
     * 
     * @param effectiveDate Effective date as LocalDateTime
     */
    public void setEffectiveDate(LocalDateTime effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    /**
     * Gets the list of accounts associated with this disclosure group
     * Enables one-to-many relationship management
     * 
     * @return List of Account entities
     */
    public List<Account> getAccounts() {
        return accounts;
    }

    /**
     * Sets the list of accounts associated with this disclosure group
     * 
     * @param accounts List of Account entities
     */
    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }

    /**
     * Adds an account to this disclosure group
     * Maintains bidirectional relationship consistency
     * 
     * @param account Account to add to the group
     */
    public void addAccount(Account account) {
        if (account != null) {
            accounts.add(account);
            account.setDisclosureGroup(this);
        }
    }

    /**
     * Removes an account from this disclosure group
     * Maintains bidirectional relationship consistency
     * 
     * @param account Account to remove from the group
     */
    public void removeAccount(Account account) {
        if (account != null) {
            accounts.remove(account);
            account.setDisclosureGroup(null);
        }
    }

    /**
     * Calculates interest amount for a given principal using this group's rate
     * Provides precise BigDecimal arithmetic for financial calculations
     * 
     * @param principal Principal amount for interest calculation
     * @return Interest amount as BigDecimal
     */
    public BigDecimal calculateInterest(BigDecimal principal) {
        if (principal == null || interestRate == null) {
            return BigDecimal.ZERO;
        }
        return principal.multiply(interestRate).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Checks if this disclosure group is currently effective
     * 
     * @return true if effective date is before or equal to current time
     */
    public boolean isEffective() {
        return effectiveDate != null && effectiveDate.isBefore(LocalDateTime.now()) || effectiveDate.isEqual(LocalDateTime.now());
    }

    /**
     * Returns string representation of disclosure group
     * 
     * @return String representation including key fields
     */
    @Override
    public String toString() {
        return "DisclosureGroup{" +
                "groupId='" + groupId + '\'' +
                ", interestRate=" + interestRate +
                ", effectiveDate=" + effectiveDate +
                ", accountCount=" + (accounts != null ? accounts.size() : 0) +
                '}';
    }

    /**
     * Equals method for entity comparison
     * 
     * @param o Object to compare
     * @return true if objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DisclosureGroup that = (DisclosureGroup) o;
        return groupId != null && groupId.equals(that.groupId);
    }

    /**
     * Hash code method for entity hashing
     * 
     * @return hash code based on groupId
     */
    @Override
    public int hashCode() {
        return groupId != null ? groupId.hashCode() : 0;
    }
}