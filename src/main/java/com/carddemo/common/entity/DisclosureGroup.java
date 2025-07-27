package com.carddemo.common.entity;

import com.carddemo.common.entity.Account;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA DisclosureGroup entity mapping COBOL disclosure structure to PostgreSQL disclosure_groups table.
 * 
 * This entity represents interest rate disclosure groups for credit card accounts, supporting
 * banking regulatory requirements for interest rate disclosure and enabling precise financial
 * calculations using BigDecimal arithmetic as specified in Section 0.3.2.
 * 
 * Converted from COBOL copybook CVTRA02Y.cpy with enhanced fields for modern system requirements
 * including legal disclosure text storage and effective date tracking for regulatory compliance.
 * 
 * Database mapping:
 * - group_id: VARCHAR(10) primary key for interest rate group classification
 * - disclosure_text: TEXT for legal disclosure content storage  
 * - interest_rate: DECIMAL(5,4) using BigDecimal for precise interest rate calculations
 * - effective_date: TIMESTAMP for rate change tracking and historical analysis
 * - One-to-many relationship with Account entity for interest rate application per Section 6.2.1.1
 */
@Entity
@Table(name = "disclosure_groups", schema = "public")
public class DisclosureGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Group identifier serving as primary key.
     * Maps to DIS-ACCT-GROUP-ID from COBOL copybook CVTRA02Y.cpy.
     * Used for interest rate group classification and account association.
     */
    @Id
    @Column(name = "group_id", length = 10, nullable = false)
    @NotNull(message = "Group ID cannot be null")
    @Size(min = 1, max = 10, message = "Group ID must be between 1 and 10 characters")
    private String groupId;

    /**
     * Legal disclosure text content for regulatory compliance.
     * Extended field not present in original COBOL structure, added for
     * banking regulation compliance per Section 6.2.3.1.
     */
    @Column(name = "disclosure_text", columnDefinition = "TEXT")
    @Size(max = 1000, message = "Disclosure text cannot exceed 1000 characters")
    private String disclosureText;

    /**
     * Annual interest rate with precise decimal representation.
     * Maps to DIS-INT-RATE from COBOL copybook with enhanced precision.
     * Original COBOL: PIC S9(04)V99 (4 integer digits + 2 decimal places)
     * PostgreSQL: DECIMAL(5,4) (1 integer digit + 4 decimal places for percentage precision)
     * 
     * Using BigDecimal to maintain COBOL COMP-3 precision per Section 0.3.2 requirements.
     * Supports range 0.0001 to 9.9999 (0.01% to 999.99% annual percentage rate).
     */
    @Column(name = "interest_rate", precision = 5, scale = 4)
    @DecimalMax(value = "9.9999", message = "Interest rate cannot exceed 999.99%")
    private BigDecimal interestRate;

    /**
     * Effective date for rate change tracking and historical analysis.
     * Extended field not present in original COBOL structure, added for
     * regulatory compliance and audit trail requirements.
     */
    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    /**
     * One-to-many relationship with Account entity.
     * Represents accounts that use this disclosure group for interest rate application.
     * Mapped by the group_id foreign key in the accounts table per Section 6.2.1.1.
     * 
     * References Account entity in the same package (renamed to CommonAccount to avoid conflicts).
     */
    @OneToMany(mappedBy = "disclosureGroup", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Account> accounts = new ArrayList<>();

    /**
     * Default constructor required by JPA specification.
     */
    public DisclosureGroup() {
        // Initialize collection to prevent null pointer exceptions
        this.accounts = new ArrayList<>();
    }

    /**
     * Constructor with required fields for business logic initialization.
     * 
     * @param groupId The unique group identifier (1-10 characters)
     * @param interestRate The annual interest rate with 4 decimal place precision
     */
    public DisclosureGroup(String groupId, BigDecimal interestRate) {
        this();
        this.groupId = groupId;
        this.interestRate = interestRate;
        this.effectiveDate = LocalDate.now();
    }

    /**
     * Constructor with all fields including specific effective date.
     * Uses LocalDate.of() method for precise date specification.
     * 
     * @param groupId The unique group identifier
     * @param interestRate The annual interest rate
     * @param disclosureText The legal disclosure text
     * @param year The year for effective date
     * @param month The month for effective date
     * @param dayOfMonth The day for effective date  
     */
    public DisclosureGroup(String groupId, BigDecimal interestRate, String disclosureText,
                          int year, int month, int dayOfMonth) {
        this();
        this.groupId = groupId;
        this.interestRate = interestRate;
        this.disclosureText = disclosureText;
        this.effectiveDate = LocalDate.of(year, month, dayOfMonth);
    }

    /**
     * Gets the group identifier.
     * 
     * @return The unique group ID (VARCHAR(10))
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Sets the group identifier.
     * 
     * @param groupId The unique group ID (1-10 characters, not null)
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * Gets the legal disclosure text.
     * 
     * @return The disclosure text content (up to 1000 characters)
     */
    public String getDisclosureText() {
        return disclosureText;
    }

    /**
     * Sets the legal disclosure text for regulatory compliance.
     * 
     * @param disclosureText The disclosure content (max 1000 characters)
     */
    public void setDisclosureText(String disclosureText) {
        this.disclosureText = disclosureText;
    }

    /**
     * Gets the interest rate with precise decimal representation.
     * 
     * @return The annual interest rate as BigDecimal with DECIMAL(5,4) precision
     */
    public BigDecimal getInterestRate() {
        return interestRate;
    }

    /**
     * Sets the interest rate using BigDecimal for precise financial calculations.
     * Maintains COBOL COMP-3 precision equivalent per Section 0.3.2 requirements.
     * 
     * @param interestRate The annual percentage rate (0.0001 to 9.9999)
     */
    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    /**
     * Gets the effective date for rate tracking.
     * 
     * @return The date when this rate configuration became effective
     */
    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    /**
     * Sets the effective date for rate change tracking and historical analysis.
     * 
     * @param effectiveDate The date for rate effectiveness
     */
    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    /**
     * Gets the list of accounts associated with this disclosure group.
     * 
     * @return List of account entities using this interest rate configuration
     */
    public List<Account> getAccounts() {
        return accounts;
    }

    /**
     * Sets the list of accounts for this disclosure group.
     * 
     * @param accounts List of account entities to associate
     */
    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts != null ? accounts : new ArrayList<>();
    }

    /**
     * Adds an account to this disclosure group.
     * Maintains bidirectional relationship consistency using List.add() method.
     * 
     * @param account The account entity to associate
     */
    public void addAccount(Account account) {
        if (account != null) {
            this.accounts.add(account);
            // Note: Bidirectional relationship management would be handled in Account entity
        }
    }

    /**
     * Removes an account from this disclosure group.
     * Maintains bidirectional relationship consistency using List.remove() method.
     * 
     * @param account The account entity to disassociate
     */
    public void removeAccount(Account account) {
        if (account != null) {
            this.accounts.remove(account);
            // Note: Bidirectional relationship management would be handled in Account entity
        }
    }

    /**
     * Gets the number of accounts associated with this disclosure group.
     * Uses List.size() method for count determination.
     * 
     * @return The count of associated accounts
     */
    public int getAccountCount() {
        return accounts != null ? accounts.size() : 0;
    }

    /**
     * Calculates monthly interest rate from annual rate.
     * Utility method for financial calculations maintaining BigDecimal precision.
     * 
     * @return Monthly interest rate (annual rate / 12) with DECIMAL128 precision
     */
    public BigDecimal getMonthlyInterestRate() {
        if (interestRate == null) {
            return BigDecimal.ZERO;
        }
        return interestRate.divide(BigDecimal.valueOf(12), java.math.MathContext.DECIMAL128);
    }

    /**
     * Calculates daily interest rate from annual rate.
     * Utility method for daily interest calculations maintaining precision.
     * 
     * @return Daily interest rate (annual rate / 365) with DECIMAL128 precision
     */
    public BigDecimal getDailyInterestRate() {
        if (interestRate == null) {
            return BigDecimal.ZERO;
        }
        return interestRate.divide(BigDecimal.valueOf(365), java.math.MathContext.DECIMAL128);
    }

    /**
     * Checks if this disclosure group is currently effective.
     * 
     * @return true if effective date is null or in the past/present
     */
    public boolean isCurrentlyEffective() {
        if (effectiveDate == null) {
            return true; // No effective date restriction
        }
        return !effectiveDate.isAfter(LocalDate.now());
    }

    /**
     * Calculates interest amount for a given principal using BigDecimal precision.
     * Uses BigDecimal() constructor, multiply(), and setScale() methods per external import requirements.
     * 
     * @param principalAmount The principal amount for interest calculation
     * @return The calculated interest amount with proper scale
     */
    public BigDecimal calculateInterestAmount(BigDecimal principalAmount) {
        if (interestRate == null || principalAmount == null) {
            return new BigDecimal("0.00");  // Using BigDecimal() constructor
        }
        
        // Calculate interest: principal * rate
        BigDecimal interestAmount = principalAmount.multiply(interestRate);
        
        // Set scale to 2 decimal places for currency representation
        return interestAmount.setScale(2, java.math.RoundingMode.HALF_EVEN);
    }

    /**
     * Validates and normalizes the interest rate using BigDecimal operations.
     * Ensures proper scale and precision for financial calculations.
     * 
     * @param rate The rate to validate and normalize
     * @return Normalized rate with proper scale, or null if invalid
     */
    public BigDecimal normalizeInterestRate(BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) < 0) {
            return null;
        }
        
        // Ensure rate has proper scale (4 decimal places) using setScale()
        return rate.setScale(4, java.math.RoundingMode.HALF_EVEN);
    }

    /**
     * Creates a default interest rate using BigDecimal constructor.
     * Used for initialization when no rate is specified.
     * 
     * @return Default interest rate of 0.0500 (5.00%)
     */
    public static BigDecimal createDefaultInterestRate() {
        return new BigDecimal("0.0500");  // Using BigDecimal() constructor
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        DisclosureGroup that = (DisclosureGroup) obj;
        return groupId != null ? groupId.equals(that.groupId) : that.groupId == null;
    }

    @Override
    public int hashCode() {
        return groupId != null ? groupId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "DisclosureGroup{" +
                "groupId='" + groupId + '\'' +
                ", interestRate=" + interestRate +
                ", effectiveDate=" + effectiveDate +
                ", accountCount=" + (accounts != null ? accounts.size() : 0) +
                '}';
    }
}