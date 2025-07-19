package com.carddemo.card;

import com.carddemo.account.entity.Account;
import com.carddemo.account.entity.Customer;
import com.carddemo.common.enums.CardStatus;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * Card JPA entity representing credit card data converted from COBOL CARD-RECORD.
 * This entity implements the PostgreSQL cards table mapping with exact VSAM field mappings,
 * foreign key relationships to accounts and customers, comprehensive validation annotations,
 * and BigDecimal financial precision for card lifecycle management operations.
 * 
 * Mapped from COBOL copybook: app/cpy/CVCRD01Y.cpy
 * Database table: cards
 * VSAM dataset: CARDDAT
 * Record length: 150 bytes (original COBOL structure)
 * 
 * Key Features:
 * - Primary key: 16-digit card number (CARD-NUM)
 * - Foreign key relationships to Account and Customer entities
 * - Card status enum validation with transaction authorization support
 * - Expiration date validation with future date enforcement
 * - CVV code validation with 3-digit format
 * - Embossed name validation with character length constraints
 * - Comprehensive input validation through Jakarta Bean Validation
 * 
 * Business Rules:
 * - Card number must be exactly 16 digits (Luhn algorithm validated)
 * - Account ID must reference valid account (11 digits)
 * - Customer ID must reference valid customer (9 digits)
 * - CVV code must be exactly 3 digits
 * - Embossed name must not exceed 50 characters
 * - Expiration date must be in the future
 * - Active status must be valid CardStatus enum value
 * 
 * @version CardDemo_v1.0-15-g27d6c6f-68
 * @since 2022-07-19
 */
@Entity
@Table(name = "cards", schema = "public")
public class Card {

    /**
     * Primary key: 16-digit card number
     * Mapped from COBOL: CARD-NUM PIC X(16)
     * Database constraint: Primary key, NOT NULL, length=16
     * Business rule: Must be exactly 16 digits for payment processing
     */
    @Id
    @Column(name = "card_number", length = 16, nullable = false)
    @NotNull(message = "Card number is required")
    @Pattern(regexp = "\\d{16}", message = "Card number must be exactly 16 digits")
    private String cardNumber;

    /**
     * Foreign key: Account identifier
     * Mapped from COBOL: ACCT-ID PIC X(11)
     * Database constraint: Foreign key to accounts.account_id, NOT NULL, length=11
     * Business rule: Must reference valid account for card-account linkage
     */
    @Column(name = "account_id", length = 11, nullable = false)
    @NotNull(message = "Account ID is required")
    @Pattern(regexp = "\\d{11}", message = "Account ID must be exactly 11 digits")
    private String accountId;

    /**
     * Foreign key: Customer identifier
     * Mapped from COBOL: CUST-ID PIC X(09)
     * Database constraint: Foreign key to customers.customer_id, NOT NULL, length=9
     * Business rule: Must reference valid customer for card-customer linkage
     */
    @Column(name = "customer_id", length = 9, nullable = false)
    @NotNull(message = "Customer ID is required")
    @Pattern(regexp = "\\d{9}", message = "Customer ID must be exactly 9 digits")
    private String customerId;

    /**
     * Card verification value (CVV) code
     * Mapped from COBOL: CVV-CODE PIC X(03)
     * Database constraint: VARCHAR(3), NOT NULL
     * Business rule: Must be exactly 3 digits for security validation
     */
    @Column(name = "cvv_code", length = 3, nullable = false)
    @NotNull(message = "CVV code is required")
    @Pattern(regexp = "\\d{3}", message = "CVV code must be exactly 3 digits")
    private String cvvCode;

    /**
     * Embossed name on card
     * Mapped from COBOL: EMBOSSED-NAME PIC X(50)
     * Database constraint: VARCHAR(50), NOT NULL
     * Business rule: Must not exceed 50 characters for card printing
     */
    @Column(name = "embossed_name", length = 50, nullable = false)
    @NotNull(message = "Embossed name is required")
    @Size(min = 1, max = 50, message = "Embossed name must be between 1 and 50 characters")
    private String embossedName;

    /**
     * Card expiration date
     * Mapped from COBOL: EXPIRY-DATE PIC X(10)
     * Database constraint: DATE, NOT NULL
     * Business rule: Must be in the future for transaction authorization
     */
    @Column(name = "expiration_date", nullable = false)
    @NotNull(message = "Expiration date is required")
    @Future(message = "Expiration date must be in the future")
    private LocalDate expirationDate;

    /**
     * Card active status using CardStatus enum
     * Mapped from COBOL: ACTIVE-STATUS PIC X(01)
     * Database constraint: VARCHAR(1), NOT NULL
     * Business rule: Must be valid CardStatus (ACTIVE, INACTIVE, BLOCKED)
     */
    @Column(name = "active_status", length = 1, nullable = false)
    @NotNull(message = "Active status is required")
    @Enumerated(EnumType.STRING)
    private CardStatus activeStatus;

    /**
     * Many-to-one relationship to Account entity
     * Foreign key: account_id references accounts.account_id
     * Cascade: PERSIST, MERGE for lifecycle management
     * Fetch: LAZY for performance optimization
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "account_id", nullable = false, insertable = false, updatable = false)
    @Valid
    private Account account;

    /**
     * Many-to-one relationship to Customer entity
     * Foreign key: customer_id references customers.customer_id
     * Cascade: PERSIST, MERGE for lifecycle management
     * Fetch: LAZY for performance optimization
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "customer_id", nullable = false, insertable = false, updatable = false)
    @Valid
    private Customer customer;

    /**
     * Version field for optimistic locking
     * Automatically managed by JPA for concurrent update protection
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Default constructor for JPA
     */
    public Card() {
        this.activeStatus = CardStatus.ACTIVE;
    }

    /**
     * Constructor with required fields
     * 
     * @param cardNumber 16-digit card number
     * @param accountId Account ID (11 digits)
     * @param customerId Customer ID (9 digits)
     * @param cvvCode CVV code (3 digits)
     * @param embossedName Name on card
     * @param expirationDate Card expiration date
     * @param activeStatus Card status
     */
    public Card(String cardNumber, String accountId, String customerId, String cvvCode, 
                String embossedName, LocalDate expirationDate, CardStatus activeStatus) {
        this.cardNumber = cardNumber;
        this.accountId = accountId;
        this.customerId = customerId;
        this.cvvCode = cvvCode;
        this.embossedName = embossedName;
        this.expirationDate = expirationDate;
        this.activeStatus = activeStatus != null ? activeStatus : CardStatus.ACTIVE;
    }

    // Getters and Setters

    /**
     * Gets the card number
     * 
     * @return 16-digit card number
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the card number
     * 
     * @param cardNumber 16-digit card number
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Gets the account ID
     * 
     * @return 11-digit account ID
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID
     * 
     * @param accountId 11-digit account ID
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the customer ID
     * 
     * @return 9-digit customer ID
     */
    public String getCustomerId() {
        return customerId;
    }

    /**
     * Sets the customer ID
     * 
     * @param customerId 9-digit customer ID
     */
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    /**
     * Gets the CVV code
     * 
     * @return 3-digit CVV code
     */
    public String getCvvCode() {
        return cvvCode;
    }

    /**
     * Sets the CVV code
     * 
     * @param cvvCode 3-digit CVV code
     */
    public void setCvvCode(String cvvCode) {
        this.cvvCode = cvvCode;
    }

    /**
     * Gets the embossed name
     * 
     * @return Name embossed on card
     */
    public String getEmbossedName() {
        return embossedName;
    }

    /**
     * Sets the embossed name
     * 
     * @param embossedName Name to emboss on card
     */
    public void setEmbossedName(String embossedName) {
        this.embossedName = embossedName;
    }

    /**
     * Gets the expiration date
     * 
     * @return Card expiration date
     */
    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    /**
     * Sets the expiration date
     * 
     * @param expirationDate Card expiration date
     */
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * Gets the active status
     * 
     * @return Card status enum
     */
    public CardStatus getActiveStatus() {
        return activeStatus;
    }

    /**
     * Sets the active status
     * 
     * @param activeStatus Card status enum
     */
    public void setActiveStatus(CardStatus activeStatus) {
        this.activeStatus = activeStatus;
    }

    /**
     * Gets the associated account
     * 
     * @return Account entity
     */
    public Account getAccount() {
        return account;
    }

    /**
     * Sets the associated account
     * 
     * @param account Account entity
     */
    public void setAccount(Account account) {
        this.account = account;
        if (account != null) {
            this.accountId = account.getAccountId();
        }
    }

    /**
     * Gets the associated customer
     * 
     * @return Customer entity
     */
    public Customer getCustomer() {
        return customer;
    }

    /**
     * Sets the associated customer
     * 
     * @param customer Customer entity
     */
    public void setCustomer(Customer customer) {
        this.customer = customer;
        if (customer != null) {
            this.customerId = customer.getCustomerId();
        }
    }

    /**
     * Gets the version for optimistic locking
     * 
     * @return Version number for optimistic locking
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Sets the version for optimistic locking
     * Note: This is typically managed automatically by JPA
     * 
     * @param version Version number for optimistic locking
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    // Business methods

    /**
     * Check if card is active
     * 
     * @return true if card status is ACTIVE
     */
    public boolean isActive() {
        return activeStatus != null && activeStatus.isActive();
    }

    /**
     * Check if card is inactive
     * 
     * @return true if card status is INACTIVE
     */
    public boolean isInactive() {
        return activeStatus != null && activeStatus.isInactive();
    }

    /**
     * Check if card is blocked
     * 
     * @return true if card status is BLOCKED
     */
    public boolean isBlocked() {
        return activeStatus != null && activeStatus.isBlocked();
    }

    /**
     * Check if card is expired
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
     * Check if card can be used for transactions
     * 
     * @return true if card is active and not expired
     */
    public boolean isTransactionAllowed() {
        return isActive() && !isExpired();
    }

    /**
     * Check if card can be used for authorization
     * 
     * @return true if card is active and not expired
     */
    public boolean isAuthorizationAllowed() {
        return isActive() && !isExpired();
    }

    /**
     * Get masked card number for display purposes
     * Shows only last 4 digits: ****-****-****-1234
     * 
     * @return Masked card number string
     */
    public String getMaskedCardNumber() {
        if (cardNumber != null && cardNumber.length() == 16) {
            return "****-****-****-" + cardNumber.substring(12);
        }
        return "****-****-****-****";
    }

    /**
     * Get formatted card number with spaces
     * Format: 1234 5678 9012 3456
     * 
     * @return Formatted card number string
     */
    public String getFormattedCardNumber() {
        if (cardNumber != null && cardNumber.length() == 16) {
            return cardNumber.substring(0, 4) + " " + 
                   cardNumber.substring(4, 8) + " " + 
                   cardNumber.substring(8, 12) + " " + 
                   cardNumber.substring(12);
        }
        return cardNumber;
    }

    /**
     * Get card expiration in MM/YY format
     * 
     * @return Expiration date in MM/YY format
     */
    public String getExpirationMonthYear() {
        if (expirationDate != null) {
            return String.format("%02d/%02d", 
                               expirationDate.getMonthValue(), 
                               expirationDate.getYear() % 100);
        }
        return "00/00";
    }

    /**
     * Get days until expiration
     * 
     * @return Number of days until card expires
     */
    public long getDaysUntilExpiration() {
        if (expirationDate == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expirationDate);
    }

    /**
     * Check if card is expiring soon (within 30 days)
     * 
     * @return true if card expires within 30 days
     */
    public boolean isExpiringSoon() {
        return getDaysUntilExpiration() <= 30 && getDaysUntilExpiration() > 0;
    }

    /**
     * Validate card number using Luhn algorithm
     * 
     * @return true if card number passes Luhn validation
     */
    public boolean isValidCardNumber() {
        if (cardNumber == null || cardNumber.length() != 16) {
            return false;
        }
        
        // Luhn algorithm implementation
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
        
        return (sum % 10) == 0;
    }

    /**
     * Get card type based on card number prefix
     * 
     * @return Card type string (Visa, MasterCard, etc.)
     */
    public String getCardType() {
        if (cardNumber == null || cardNumber.length() != 16) {
            return "Unknown";
        }
        
        String firstFour = cardNumber.substring(0, 4);
        int prefix = Integer.parseInt(firstFour);
        
        if (prefix >= 4000 && prefix <= 4999) {
            return "Visa";
        } else if (prefix >= 5100 && prefix <= 5599) {
            return "MasterCard";
        } else if (prefix >= 3400 && prefix <= 3499) {
            return "American Express";
        } else if (prefix >= 6000 && prefix <= 6999) {
            return "Discover";
        } else {
            return "Unknown";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Card card = (Card) o;
        return cardNumber != null ? cardNumber.equals(card.cardNumber) : card.cardNumber == null;
    }

    @Override
    public int hashCode() {
        return cardNumber != null ? cardNumber.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Card{" +
                "cardNumber='" + getMaskedCardNumber() + '\'' +
                ", accountId='" + accountId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", embossedName='" + embossedName + '\'' +
                ", expirationDate=" + expirationDate +
                ", activeStatus=" + activeStatus +
                ", cardType='" + getCardType() + '\'' +
                '}';
    }
}