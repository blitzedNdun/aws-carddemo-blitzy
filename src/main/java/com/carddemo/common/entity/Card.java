package com.carddemo.common.entity;

import com.carddemo.common.entity.Account;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * JPA Card entity mapping COBOL card record structure to PostgreSQL cards table
 * with card number validation, security fields, expiration tracking, and composite 
 * foreign key relationships to accounts and customers for credit card lifecycle management.
 * 
 * This entity supports:
 * - Card lifecycle management with status tracking and security validation per Section 2.1.4
 * - 200ms 95th percentile response time for card authorization per Section 2.1.4 
 * - Card-to-account cross-referencing per Section 2.1.4
 * - Optimistic locking with @Version annotation for concurrent access control
 * - Luhn algorithm validation for card number integrity per Section 6.2.6.6
 * - Security field management including CVV code and embossed name validation
 * - Composite foreign key relationships to Account and Customer entities
 * 
 * Maps to PostgreSQL table: cards
 * Original COBOL structure: CVCRD01Y.cpy card record definition
 * 
 * Card number field uses Luhn algorithm validation:
 * - CARD-NUM PIC X(16) -> VARCHAR(16) card_number primary key
 * - CVV-CODE PIC X(3) -> VARCHAR(3) cvv_code for security validation
 * - EMBOSSED-NAME PIC X(50) -> VARCHAR(50) embossed_name for card personalization
 * - EXPIRATION-DATE PIC X(10) -> DATE expiration_date for card validity management
 * - ACTIVE-STATUS PIC X(1) -> VARCHAR(1) active_status for lifecycle tracking
 * 
 * Foreign key relationships:
 * - account_id references accounts.account_id for card-to-account linking
 * - customer_id references customers.customer_id for cardholder identification
 * 
 * Performance optimizations:
 * - B-tree index on (account_id, active_status) for account-based card lookup
 * - Optimistic locking for concurrent card operations
 * - Validation annotations for field-level constraints
 */
@Entity
@Table(name = "cards")
public class Card implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Card number - Primary key mapping CARD-NUM PIC X(16)
     * Maps to PostgreSQL VARCHAR(16) card_number primary key with Luhn algorithm validation
     * Range: 16-digit format following standard credit card numbering
     * 
     * Luhn algorithm validation ensures card number integrity for payment processing
     * Per Section 6.2.6.6, card numbers must follow standard 16-digit format
     */
    @Id
    @Column(name = "card_number", length = 16, nullable = false)
    @NotNull(message = "Card number cannot be null")
    @Size(min = 16, max = 16, message = "Card number must be exactly 16 digits")
    @Pattern(regexp = "\\d{16}", message = "Card number must be exactly 16 digits")
    private String cardNumber;

    /**
     * Account identifier - Foreign key to accounts table
     * Maps to PostgreSQL VARCHAR(11) account_id foreign key
     * Links card to account record for balance management and credit limit enforcement
     */
    @Column(name = "account_id", length = 11, nullable = false)
    @NotNull(message = "Account ID cannot be null")
    @Pattern(regexp = "\\d{11}", message = "Account ID must be exactly 11 digits")
    private String accountId;

    /**
     * Customer identifier - Foreign key to customers table
     * Maps to PostgreSQL VARCHAR(9) customer_id foreign key
     * Links card to customer record for cardholder identification and management
     */
    @Column(name = "customer_id", length = 9, nullable = false)
    @NotNull(message = "Customer ID cannot be null")
    @Pattern(regexp = "\\d{9}", message = "Customer ID must be exactly 9 digits")
    private String customerId;

    /**
     * CVV security code - Maps CVV-CODE PIC X(3)
     * Maps to PostgreSQL VARCHAR(3) for card security validation
     * Range: 3-digit numeric code for transaction authorization
     */
    @Column(name = "cvv_code", length = 3, nullable = false)
    @NotNull(message = "CVV code cannot be null")
    @Size(min = 3, max = 3, message = "CVV code must be exactly 3 digits")
    @Pattern(regexp = "\\d{3}", message = "CVV code must be exactly 3 digits")
    private String cvvCode;

    /**
     * Embossed name - Maps EMBOSSED-NAME PIC X(50)
     * Maps to PostgreSQL VARCHAR(50) for card personalization
     * Contains cardholder name as embossed on physical card
     */
    @Column(name = "embossed_name", length = 50, nullable = false)
    @NotNull(message = "Embossed name cannot be null")
    @Size(min = 1, max = 50, message = "Embossed name must be between 1 and 50 characters")
    private String embossedName;

    /**
     * Card expiration date - Maps EXPIRATION-DATE PIC X(10)
     * Maps to PostgreSQL DATE type for card validity management
     * Used for card expiry validation and renewal processing
     */
    @Column(name = "expiration_date", nullable = false)
    @NotNull(message = "Expiration date cannot be null")
    private LocalDate expirationDate;

    /**
     * Card active status - Maps ACTIVE-STATUS PIC X(1)
     * Maps to PostgreSQL VARCHAR(1) with validation constraints
     * Valid values: 'A' (Active), 'S' (Suspended), 'C' (Closed)
     */
    @Column(name = "active_status", length = 1, nullable = false)
    @NotNull(message = "Active status cannot be null")
    @Pattern(regexp = "[ASC]", message = "Active status must be 'A' (Active), 'S' (Suspended), or 'C' (Closed)")
    private String activeStatus;

    /**
     * Many-to-one relationship with Account entity
     * Foreign key relationship for card-to-account linking enabling balance management
     * and credit limit enforcement for card transactions
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id", insertable = false, updatable = false)
    private Account account;

    /**
     * Many-to-one relationship with Customer entity (placeholder)
     * Foreign key relationship for cardholder identification and management
     * Note: Customer entity relationship implementation depends on Customer entity creation
     */
    @Transient
    private Object customer;

    /**
     * Optimistic locking version for concurrent card operations protection
     * Enables card lifecycle management with concurrent access control
     * Prevents lost updates in multi-user card management scenarios
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Default constructor for JPA
     */
    public Card() {
        this.version = 0L;
    }

    /**
     * Constructor with essential fields for card creation
     * 
     * @param cardNumber 16-digit card number
     * @param accountId Account identifier (11 digits)
     * @param customerId Customer identifier (9 digits)
     * @param cvvCode CVV security code (3 digits)
     * @param embossedName Name embossed on card
     * @param expirationDate Card expiry date
     * @param activeStatus Card status ('A', 'S', 'C')
     */
    public Card(String cardNumber, String accountId, String customerId, String cvvCode,
                String embossedName, LocalDate expirationDate, String activeStatus) {
        this();
        this.cardNumber = cardNumber;
        this.accountId = accountId;
        this.customerId = customerId;
        this.cvvCode = cvvCode;
        this.embossedName = embossedName;
        this.expirationDate = expirationDate;
        this.activeStatus = activeStatus;
    }

    /**
     * Gets the card number
     * 
     * @return Card number as 16-digit string
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the card number
     * 
     * @param cardNumber Card number as 16-digit string
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
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
     * Gets the CVV security code
     * 
     * @return CVV code as 3-digit string
     */
    public String getCvvCode() {
        return cvvCode;
    }

    /**
     * Sets the CVV security code
     * 
     * @param cvvCode CVV code as 3-digit string
     */
    public void setCvvCode(String cvvCode) {
        this.cvvCode = cvvCode;
    }

    /**
     * Gets the embossed name
     * 
     * @return Embossed name as displayed on card
     */
    public String getEmbossedName() {
        return embossedName;
    }

    /**
     * Sets the embossed name
     * 
     * @param embossedName Embossed name as displayed on card
     */
    public void setEmbossedName(String embossedName) {
        this.embossedName = embossedName;
    }

    /**
     * Gets the card expiration date
     * 
     * @return Expiration date as LocalDate
     */
    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    /**
     * Sets the card expiration date
     * 
     * @param expirationDate Expiration date as LocalDate
     */
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * Gets the card active status
     * 
     * @return Active status ('A', 'S', 'C')
     */
    public String getActiveStatus() {
        return activeStatus;
    }

    /**
     * Sets the card active status
     * 
     * @param activeStatus Active status ('A', 'S', 'C')
     */
    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }

    /**
     * Gets the associated account entity
     * Provides access to account balance and credit limit information
     * 
     * @return Account entity for balance management and credit limit enforcement
     */
    public Account getAccount() {
        return account;
    }

    /**
     * Sets the associated account entity
     * Links card to account for balance management and transaction processing
     * 
     * @param account Account entity
     */
    public void setAccount(Account account) {
        this.account = account;
        if (account != null && account.getAccountId() != null) {
            this.accountId = account.getAccountId();
        }
    }

    /**
     * Gets the customer entity (placeholder for future implementation)
     * Note: Customer entity relationship will be implemented by another agent
     * 
     * @return Customer entity (null for now)
     */
    public Object getCustomer() {
        return customer;
    }

    /**
     * Sets the customer entity (placeholder for future implementation)
     * Note: Customer entity relationship will be implemented by another agent
     * 
     * @param customer Customer entity
     */
    public void setCustomer(Object customer) {
        this.customer = customer;
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
     * Validates card number using Luhn algorithm
     * Ensures card number integrity for payment processing
     * 
     * @return true if card number passes Luhn validation
     */
    public boolean isValidCardNumber() {
        if (cardNumber == null || cardNumber.length() != 16) {
            return false;
        }
        
        // Luhn algorithm validation
        int sum = 0;
        boolean alternate = false;
        
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        return sum % 10 == 0;
    }

    /**
     * Checks if card is active
     * 
     * @return true if card status is 'A' (Active)
     */
    public boolean isActive() {
        return "A".equals(activeStatus);
    }

    /**
     * Checks if card is suspended
     * 
     * @return true if card status is 'S' (Suspended)
     */
    public boolean isSuspended() {
        return "S".equals(activeStatus);
    }

    /**
     * Checks if card is closed
     * 
     * @return true if card status is 'C' (Closed)
     */
    public boolean isClosed() {
        return "C".equals(activeStatus);
    }

    /**
     * Checks if card is expired based on expiration date
     * 
     * @return true if expiration date is before current date
     */
    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDate.now());
    }

    /**
     * Checks if card is valid for transactions
     * Card must be active and not expired
     * 
     * @return true if card is valid for use
     */
    public boolean isValidForTransactions() {
        return isActive() && !isExpired() && isValidCardNumber();
    }

    /**
     * Gets masked card number for display purposes
     * Shows only last 4 digits for security
     * 
     * @return Masked card number (****-****-****-1234)
     */
    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.length() != 16) {
            return "****-****-****-****";
        }
        return "****-****-****-" + cardNumber.substring(12);
    }

    /**
     * Calculates days until expiration
     * 
     * @return Number of days until card expires
     */
    public long getDaysUntilExpiration() {
        if (expirationDate == null) {
            return 0;
        }
        return LocalDate.now().until(expirationDate).getDays();
    }

    /**
     * Checks if card expires within specified days
     * 
     * @param days Number of days to check
     * @return true if card expires within specified days
     */
    public boolean expiresWithinDays(int days) {
        return getDaysUntilExpiration() <= days;
    }

    /**
     * Equals method for entity comparison based on card number
     * 
     * @param o Object to compare
     * @return true if objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return Objects.equals(cardNumber, card.cardNumber);
    }

    /**
     * Hash code method for entity hashing based on card number
     * 
     * @return hash code based on cardNumber
     */
    @Override
    public int hashCode() {
        return Objects.hash(cardNumber);
    }

    /**
     * String representation of card entity
     * 
     * @return String representation including key fields (with masked card number)
     */
    @Override
    public String toString() {
        return "Card{" +
                "cardNumber='" + getMaskedCardNumber() + '\'' +
                ", accountId='" + accountId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", embossedName='" + embossedName + '\'' +
                ", expirationDate=" + expirationDate +
                ", activeStatus='" + activeStatus + '\'' +
                ", version=" + version +
                '}';
    }
}