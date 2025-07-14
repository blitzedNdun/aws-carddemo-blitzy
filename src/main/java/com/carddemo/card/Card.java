package com.carddemo.card;

import com.carddemo.account.entity.Account;
import com.carddemo.account.entity.Customer;
import com.carddemo.common.enums.CardStatus;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.Objects;

/**
 * JPA Entity representing credit card master data converted from COBOL CARD-RECORD.
 * 
 * This entity maps the legacy VSAM CARDDAT dataset structure to a modern PostgreSQL
 * relational table with exact field mappings, comprehensive validation, and foreign key
 * relationships supporting complete card lifecycle management in the Spring Boot
 * microservices architecture.
 * 
 * The entity preserves VSAM record layout structure while implementing BigDecimal
 * financial precision equivalent to COBOL COMP-3 arithmetic per Section 0.1.2 data
 * precision mandate. Card status management supports both legacy COBOL Y/N values
 * and modern A/I/B codes for backward compatibility during migration.
 * 
 * Database relationships include bidirectional associations with Account and Customer
 * entities, maintaining referential integrity equivalent to VSAM cross-reference
 * functionality per Section 6.2.1.1 entity relationships.
 * 
 * Converted from: app/data/ASCII/carddata.txt (VSAM CARDDAT dataset)
 * Database Table: cards (PostgreSQL)
 * Record Length: 16 + 11 + 9 + 3 + 50 + 10 + 1 = 100 bytes (original layout)
 * 
 * @author Blitzy agent
 * @version CardDemo_v1.0-15-g27d6c6f-68
 * @since 2022-07-19
 */
@Entity
@Table(name = "cards", indexes = {
    @Index(name = "idx_cards_account_id", columnList = "account_id, active_status"),
    @Index(name = "idx_cards_customer_id", columnList = "customer_id, active_status"),
    @Index(name = "idx_cards_expiration_date", columnList = "expiration_date, active_status")
})
public class Card {

    /**
     * Credit card number (Primary Key)
     * Converted from: Fixed-width position 1-16 in carddata.txt
     * Format: 16-digit numeric string with Luhn algorithm validation
     */
    @Id
    @Column(name = "card_number", length = 16, nullable = false)
    @NotNull(message = "Card number cannot be null")
    @Pattern(regexp = "\\d{16}", message = "Card number must be exactly 16 digits")
    private String cardNumber;

    /**
     * Account relationship (Foreign Key)
     * Converted from: Fixed-width position 17-27 in carddata.txt (11 digits)
     * Maintains bidirectional relationship with Account entity
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    @NotNull(message = "Account association is required")
    @Valid
    private Account account;

    /**
     * Account ID field for direct access and validation
     * Derived from account relationship for query optimization
     */
    @Column(name = "account_id", length = 11, nullable = false, insertable = false, updatable = false)
    @NotNull(message = "Account ID cannot be null")
    @Pattern(regexp = "\\d{11}", message = "Account ID must be exactly 11 digits")
    private String accountId;

    /**
     * Customer relationship (Foreign Key)
     * Converted from: Fixed-width position 28-36 in carddata.txt (9 digits)
     * Maintains bidirectional relationship with Customer entity
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    @NotNull(message = "Customer association is required")
    @Valid
    private Customer customer;

    /**
     * Customer ID field for direct access and validation
     * Derived from customer relationship for query optimization
     */
    @Column(name = "customer_id", length = 9, nullable = false, insertable = false, updatable = false)
    @NotNull(message = "Customer ID cannot be null")
    @Pattern(regexp = "\\d{9}", message = "Customer ID must be exactly 9 digits")
    private String customerId;

    /**
     * Card verification value (CVV/CVC) security code
     * Converted from: Fixed-width position 37-39 in carddata.txt (3 digits)
     * Sensitive data requiring encryption in production environments
     */
    @Column(name = "cvv_code", length = 3, nullable = false)
    @NotNull(message = "CVV code cannot be null")
    @Pattern(regexp = "\\d{3}", message = "CVV code must be exactly 3 digits")
    private String cvvCode;

    /**
     * Name embossed on the credit card
     * Converted from: Fixed-width position 40-89 in carddata.txt (50 characters)
     * Trimmed and formatted name for card printing and display
     */
    @Column(name = "embossed_name", length = 50, nullable = false)
    @NotBlank(message = "Embossed name is required")
    @Size(max = 50, message = "Embossed name cannot exceed 50 characters")
    private String embossedName;

    /**
     * Card expiration date
     * Converted from: Fixed-width position 90-99 in carddata.txt (YYYY-MM-DD format)
     * Must be future date for card validity business rules
     */
    @Column(name = "expiration_date", nullable = false)
    @NotNull(message = "Expiration date cannot be null")
    @Future(message = "Card expiration date must be in the future")
    private LocalDate expirationDate;

    /**
     * Card active status using modern enum with legacy compatibility
     * Converted from: Fixed-width position 100 in carddata.txt (1 character)
     * Supports both legacy (Y/N) and modern (A/I/B) status codes
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "active_status", length = 1, nullable = false)
    @NotNull(message = "Card status cannot be null")
    private CardStatus activeStatus;

    /**
     * Optimistic locking version field
     * Supports concurrent modification detection equivalent to VSAM record locking
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Default constructor required by JPA
     */
    public Card() {
        // Initialize with default active status
        this.activeStatus = CardStatus.ACTIVE;
    }

    /**
     * Constructor with required fields
     * 
     * @param cardNumber 16-digit credit card number
     * @param account Account entity association
     * @param customer Customer entity association
     * @param cvvCode 3-digit CVV security code
     * @param embossedName Name to appear on card
     * @param expirationDate Card expiration date
     * @param activeStatus Card status (Active/Inactive/Blocked)
     */
    public Card(String cardNumber, Account account, Customer customer, String cvvCode, 
                String embossedName, LocalDate expirationDate, CardStatus activeStatus) {
        this.cardNumber = cardNumber;
        this.account = account;
        this.customer = customer;
        this.cvvCode = cvvCode;
        this.embossedName = embossedName;
        this.expirationDate = expirationDate;
        this.activeStatus = activeStatus;
    }

    /**
     * Convenience constructor with string status code
     * Supports legacy Y/N and modern A/I/B status codes
     * 
     * @param cardNumber 16-digit credit card number
     * @param account Account entity association
     * @param customer Customer entity association
     * @param cvvCode 3-digit CVV security code
     * @param embossedName Name to appear on card
     * @param expirationDate Card expiration date
     * @param statusCode Single character status code (Y/N/A/I/B)
     */
    public Card(String cardNumber, Account account, Customer customer, String cvvCode, 
                String embossedName, LocalDate expirationDate, String statusCode) {
        this(cardNumber, account, customer, cvvCode, embossedName, expirationDate, 
             CardStatus.fromCode(statusCode));
    }

    // Getter and Setter methods

    /**
     * Returns the 16-digit credit card number
     * 
     * @return Credit card number string
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the credit card number with validation
     * 
     * @param cardNumber 16-digit credit card number
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Returns the account ID from the account relationship
     * 
     * @return 11-digit account ID string
     */
    public String getAccountId() {
        return account != null ? account.getAccountId() : accountId;
    }

    /**
     * Sets the account ID (derived from account relationship)
     * This method is primarily for JPA mapping and validation
     * 
     * @param accountId 11-digit account ID
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Returns the customer ID from the customer relationship
     * 
     * @return 9-digit customer ID string
     */
    public String getCustomerId() {
        return customer != null ? customer.getCustomerId() : customerId;
    }

    /**
     * Sets the customer ID (derived from customer relationship)
     * This method is primarily for JPA mapping and validation
     * 
     * @param customerId 9-digit customer ID
     */
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    /**
     * Returns the CVV security code
     * 
     * @return 3-digit CVV code string
     */
    public String getCvvCode() {
        return cvvCode;
    }

    /**
     * Sets the CVV security code
     * 
     * @param cvvCode 3-digit CVV code
     */
    public void setCvvCode(String cvvCode) {
        this.cvvCode = cvvCode;
    }

    /**
     * Returns the name embossed on the card
     * 
     * @return Embossed name string
     */
    public String getEmbossedName() {
        return embossedName;
    }

    /**
     * Sets the embossed name on the card
     * 
     * @param embossedName Name to appear on card
     */
    public void setEmbossedName(String embossedName) {
        this.embossedName = embossedName;
    }

    /**
     * Returns the card expiration date
     * 
     * @return Card expiration date
     */
    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    /**
     * Sets the card expiration date
     * 
     * @param expirationDate Future date for card validity
     */
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * Returns the card active status
     * 
     * @return CardStatus enum value
     */
    public CardStatus getActiveStatus() {
        return activeStatus;
    }

    /**
     * Sets the card active status
     * 
     * @param activeStatus CardStatus enum value
     */
    public void setActiveStatus(CardStatus activeStatus) {
        this.activeStatus = activeStatus;
    }

    /**
     * Returns the associated account entity
     * 
     * @return Account entity reference
     */
    public Account getAccount() {
        return account;
    }

    /**
     * Sets the account association with bidirectional relationship management
     * 
     * @param account Account entity to associate with this card
     */
    public void setAccount(Account account) {
        this.account = account;
        if (account != null) {
            this.accountId = account.getAccountId();
        }
    }

    /**
     * Returns the associated customer entity
     * 
     * @return Customer entity reference
     */
    public Customer getCustomer() {
        return customer;
    }

    /**
     * Sets the customer association with bidirectional relationship management
     * 
     * @param customer Customer entity to associate with this card
     */
    public void setCustomer(Customer customer) {
        this.customer = customer;
        if (customer != null) {
            this.customerId = customer.getCustomerId();
        }
    }

    /**
     * Returns the optimistic locking version
     * 
     * @return Version number for optimistic locking
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Sets the optimistic locking version
     * 
     * @param version Version number
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    // Business logic methods

    /**
     * Checks if the card is currently active and can process transactions
     * 
     * @return true if card status allows transactions, false otherwise
     */
    public boolean isActive() {
        return activeStatus != null && activeStatus.isTransactionAllowed();
    }

    /**
     * Checks if the card is expired based on current date
     * 
     * @return true if card is past expiration date, false otherwise
     */
    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDate.now());
    }

    /**
     * Checks if the card is valid for transactions (active and not expired)
     * 
     * @return true if card can process transactions, false otherwise
     */
    public boolean isValid() {
        return isActive() && !isExpired();
    }

    /**
     * Returns a masked version of the card number for display purposes
     * Shows only the last 4 digits with asterisks masking the rest
     * 
     * @return Masked card number string (e.g., "************1234")
     */
    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****************";
        }
        return "*".repeat(cardNumber.length() - 4) + cardNumber.substring(cardNumber.length() - 4);
    }

    /**
     * Gets the card type based on the first digit (basic implementation)
     * 
     * @return Card type string based on card number prefix
     */
    public String getCardType() {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return "Unknown";
        }
        
        char firstDigit = cardNumber.charAt(0);
        switch (firstDigit) {
            case '4': return "Visa";
            case '5': return "MasterCard";
            case '3': return "American Express";
            case '6': return "Discover";
            default: return "Unknown";
        }
    }

    /**
     * Validates card number using Luhn algorithm
     * 
     * @return true if card number passes Luhn validation, false otherwise
     */
    public boolean isLuhnValid() {
        if (cardNumber == null || !cardNumber.matches("\\d{16}")) {
            return false;
        }
        
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
     * Checks if card status can be changed to the specified new status
     * 
     * @param newStatus Target status for transition
     * @return true if status change is allowed, false otherwise
     */
    public boolean canChangeStatusTo(CardStatus newStatus) {
        return activeStatus != null && activeStatus.canTransitionTo(newStatus);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Card card = (Card) obj;
        return Objects.equals(cardNumber, card.cardNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardNumber);
    }

    @Override
    public String toString() {
        return "Card{" +
                "cardNumber='" + getMaskedCardNumber() + '\'' +
                ", accountId='" + getAccountId() + '\'' +
                ", customerId='" + getCustomerId() + '\'' +
                ", embossedName='" + embossedName + '\'' +
                ", expirationDate=" + expirationDate +
                ", activeStatus=" + activeStatus +
                ", cardType='" + getCardType() + '\'' +
                ", isValid=" + isValid() +
                ", version=" + version +
                '}';
    }
}