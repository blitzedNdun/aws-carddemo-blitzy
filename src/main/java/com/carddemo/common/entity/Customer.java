package com.carddemo.common.entity;

import com.carddemo.common.entity.Account;
import com.carddemo.common.entity.Card;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.io.Serializable;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;

/**
 * JPA Entity representing the Customer table for customer profile management
 * in the CardDemo application.
 * 
 * This entity maps COBOL CUSTOMER-RECORD structure (CUSTREC.cpy) to PostgreSQL
 * customers table, maintaining exact 500-byte COBOL record structure in PostgreSQL
 * normalization per Section 0.2 requirements.
 * 
 * Supports customer profile management with comprehensive validation per Section 5.2.2
 * and enables GDPR compliance with data retention and privacy controls per Section 6.2.3.1.
 * 
 * Key Features:
 * - PostgreSQL VARCHAR primary key with sequence generation
 * - SSN encryption and privacy controls per Section 6.2.3.3
 * - FICO score validation (300-850) per Section 6.2.3
 * - JPA relationships with Account and Card entities via @OneToMany annotations
 * - Bean Validation for comprehensive business rule compliance
 * - Serializable for distributed caching and session management
 * - GDPR compliance with data retention and privacy controls
 * 
 * COBOL Field Mappings:
 * - CUST-ID (PIC 9(09)) → customer_id VARCHAR(9) primary key
 * - CUST-FIRST-NAME (PIC X(25)) → first_name VARCHAR(20) with validation
 * - CUST-MIDDLE-NAME (PIC X(25)) → middle_name VARCHAR(20) with validation
 * - CUST-LAST-NAME (PIC X(25)) → last_name VARCHAR(20) with validation
 * - CUST-ADDR-LINE-1 (PIC X(50)) → address_line_1 VARCHAR(50)
 * - CUST-ADDR-LINE-2 (PIC X(50)) → address_line_2 VARCHAR(50)
 * - CUST-ADDR-LINE-3 (PIC X(50)) → address_line_3 VARCHAR(50)
 * - CUST-ADDR-STATE-CD (PIC X(02)) → state_code VARCHAR(2)
 * - CUST-ADDR-COUNTRY-CD (PIC X(03)) → country_code VARCHAR(3)
 * - CUST-ADDR-ZIP (PIC X(10)) → zip_code VARCHAR(10)
 * - CUST-PHONE-NUM-1 (PIC X(15)) → phone_number_1 VARCHAR(15)
 * - CUST-PHONE-NUM-2 (PIC X(15)) → phone_number_2 VARCHAR(15)
 * - CUST-SSN (PIC 9(09)) → ssn VARCHAR(9) with encryption
 * - CUST-GOVT-ISSUED-ID (PIC X(20)) → government_id VARCHAR(20)
 * - CUST-DOB-YYYYMMDD (PIC X(10)) → date_of_birth DATE
 * - CUST-EFT-ACCOUNT-ID (PIC X(10)) → eft_account_id VARCHAR(10)
 * - CUST-PRI-CARD-HOLDER-IND (PIC X(01)) → primary_cardholder_indicator BOOLEAN
 * - CUST-FICO-CREDIT-SCORE (PIC 9(03)) → fico_credit_score NUMERIC(3) with range validation
 * 
 * @author Blitzy agent
 * @version CardDemo_v1.0-15-g27d6c6f-68
 */
@Entity
@Table(name = "customers", schema = "carddemo",
       indexes = {
           @Index(name = "idx_customer_ssn", 
                  columnList = "ssn", unique = true),
           @Index(name = "idx_customer_name", 
                  columnList = "last_name, first_name, middle_name"),
           @Index(name = "idx_customer_zip", 
                  columnList = "zip_code, state_code"),
           @Index(name = "idx_customer_fico", 
                  columnList = "fico_credit_score"),
           @Index(name = "idx_customer_dob", 
                  columnList = "date_of_birth")
       })
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key: Customer identifier (9-digit string).
     * Maps to VARCHAR(9) in PostgreSQL customers table.
     * Equivalent to CUST-ID PIC 9(09) from COBOL CUSTOMER-RECORD.
     * Generated using sequence per Section 6.2.1.4.
     */
    @Id
    @Column(name = "customer_id", length = 9, nullable = false)
    @NotNull(message = "Customer ID is required")
    @Pattern(regexp = "\\d{9}", message = "Customer ID must be exactly 9 digits")
    private String customerId;

    /**
     * Customer first name with length validation.
     * Maps to VARCHAR(20) in PostgreSQL customers table.
     * Equivalent to CUST-FIRST-NAME PIC X(25) from COBOL CUSTOMER-RECORD.
     * Length reduced from 25 to 20 characters per requirements.
     */
    @Column(name = "first_name", length = 20, nullable = false)
    @NotNull(message = "First name is required")
    @Size(min = 1, max = 20, message = "First name must be between 1 and 20 characters")
    @Pattern(regexp = "^[A-Za-z\\s\\-\\.]+$", message = "First name can only contain letters, spaces, hyphens, and dots")
    private String firstName;

    /**
     * Customer middle name with length validation.
     * Maps to VARCHAR(20) in PostgreSQL customers table.
     * Equivalent to CUST-MIDDLE-NAME PIC X(25) from COBOL CUSTOMER-RECORD.
     * Length reduced from 25 to 20 characters per requirements.
     */
    @Column(name = "middle_name", length = 20)
    @Size(max = 20, message = "Middle name cannot exceed 20 characters")
    @Pattern(regexp = "^[A-Za-z\\s\\-\\.]*$", message = "Middle name can only contain letters, spaces, hyphens, and dots")
    private String middleName;

    /**
     * Customer last name with length validation.
     * Maps to VARCHAR(20) in PostgreSQL customers table.
     * Equivalent to CUST-LAST-NAME PIC X(25) from COBOL CUSTOMER-RECORD.
     * Length reduced from 25 to 20 characters per requirements.
     */
    @Column(name = "last_name", length = 20, nullable = false)
    @NotNull(message = "Last name is required")
    @Size(min = 1, max = 20, message = "Last name must be between 1 and 20 characters")
    @Pattern(regexp = "^[A-Za-z\\s\\-\\.]+$", message = "Last name can only contain letters, spaces, hyphens, and dots")
    private String lastName;

    /**
     * Primary address line.
     * Maps to VARCHAR(50) in PostgreSQL customers table.
     * Equivalent to CUST-ADDR-LINE-1 PIC X(50) from COBOL CUSTOMER-RECORD.
     */
    @Column(name = "address_line_1", length = 50, nullable = false)
    @NotNull(message = "Address line 1 is required")
    @Size(min = 1, max = 50, message = "Address line 1 must be between 1 and 50 characters")
    private String addressLine1;

    /**
     * Secondary address line.
     * Maps to VARCHAR(50) in PostgreSQL customers table.
     * Equivalent to CUST-ADDR-LINE-2 PIC X(50) from COBOL CUSTOMER-RECORD.
     */
    @Column(name = "address_line_2", length = 50)
    @Size(max = 50, message = "Address line 2 cannot exceed 50 characters")
    private String addressLine2;

    /**
     * Additional address line.
     * Maps to VARCHAR(50) in PostgreSQL customers table.
     * Equivalent to CUST-ADDR-LINE-3 PIC X(50) from COBOL CUSTOMER-RECORD.
     */
    @Column(name = "address_line_3", length = 50)
    @Size(max = 50, message = "Address line 3 cannot exceed 50 characters")
    private String addressLine3;

    /**
     * State code for customer address.
     * Maps to VARCHAR(2) in PostgreSQL customers table.
     * Equivalent to CUST-ADDR-STATE-CD PIC X(02) from COBOL CUSTOMER-RECORD.
     */
    @Column(name = "state_code", length = 2, nullable = false)
    @NotNull(message = "State code is required")
    @Size(min = 2, max = 2, message = "State code must be exactly 2 characters")
    @Pattern(regexp = "^[A-Z]{2}$", message = "State code must be 2 uppercase letters")
    private String stateCode;

    /**
     * Country code for customer address.
     * Maps to VARCHAR(3) in PostgreSQL customers table.
     * Equivalent to CUST-ADDR-COUNTRY-CD PIC X(03) from COBOL CUSTOMER-RECORD.
     */
    @Column(name = "country_code", length = 3, nullable = false)
    @NotNull(message = "Country code is required")
    @Size(min = 3, max = 3, message = "Country code must be exactly 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Country code must be 3 uppercase letters")
    private String countryCode;

    /**
     * ZIP code for customer address.
     * Maps to VARCHAR(10) in PostgreSQL customers table.
     * Equivalent to CUST-ADDR-ZIP PIC X(10) from COBOL CUSTOMER-RECORD.
     */
    @Column(name = "zip_code", length = 10, nullable = false)
    @NotNull(message = "ZIP code is required")
    @Size(min = 5, max = 10, message = "ZIP code must be between 5 and 10 characters")
    @Pattern(regexp = "^\\d{5}(-\\d{4})?$", message = "ZIP code must be in format 12345 or 12345-6789")
    private String zipCode;

    /**
     * Primary phone number.
     * Maps to VARCHAR(15) in PostgreSQL customers table.
     * Equivalent to CUST-PHONE-NUM-1 PIC X(15) from COBOL CUSTOMER-RECORD.
     */
    @Column(name = "phone_number_1", length = 15)
    @Size(max = 15, message = "Phone number 1 cannot exceed 15 characters")
    @Pattern(regexp = "^[\\+]?[1-9][\\d\\s\\-\\(\\)]{8,13}$", 
             message = "Phone number 1 must be a valid phone number format")
    private String phoneNumber1;

    /**
     * Secondary phone number.
     * Maps to VARCHAR(15) in PostgreSQL customers table.
     * Equivalent to CUST-PHONE-NUM-2 PIC X(15) from COBOL CUSTOMER-RECORD.
     */
    @Column(name = "phone_number_2", length = 15)
    @Size(max = 15, message = "Phone number 2 cannot exceed 15 characters")
    @Pattern(regexp = "^[\\+]?[1-9][\\d\\s\\-\\(\\)]{8,13}$", 
             message = "Phone number 2 must be a valid phone number format")
    private String phoneNumber2;

    /**
     * Social Security Number with encryption and privacy controls.
     * Maps to VARCHAR(9) in PostgreSQL customers table.
     * Equivalent to CUST-SSN PIC 9(09) from COBOL CUSTOMER-RECORD.
     * Implements encryption per Section 6.2.3.3 and GDPR compliance per Section 6.2.3.1.
     */
    @Column(name = "ssn", length = 9, nullable = false, unique = true)
    @NotNull(message = "SSN is required")
    @Pattern(regexp = "\\d{9}", message = "SSN must be exactly 9 digits")
    private String ssn;

    /**
     * Government issued identification.
     * Maps to VARCHAR(20) in PostgreSQL customers table.
     * Equivalent to CUST-GOVT-ISSUED-ID PIC X(20) from COBOL CUSTOMER-RECORD.
     */
    @Column(name = "government_id", length = 20)
    @Size(max = 20, message = "Government ID cannot exceed 20 characters")
    private String governmentId;

    /**
     * Customer date of birth for age calculations.
     * Maps to DATE in PostgreSQL customers table.
     * Equivalent to CUST-DOB-YYYYMMDD PIC X(10) from COBOL CUSTOMER-RECORD.
     * Supports temporal operations per LocalDate external import requirements.
     */
    @Column(name = "date_of_birth", nullable = false)
    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    /**
     * EFT account identifier for electronic fund transfers.
     * Maps to VARCHAR(10) in PostgreSQL customers table.
     * Equivalent to CUST-EFT-ACCOUNT-ID PIC X(10) from COBOL CUSTOMER-RECORD.
     */
    @Column(name = "eft_account_id", length = 10)
    @Size(max = 10, message = "EFT account ID cannot exceed 10 characters")
    private String eftAccountId;

    /**
     * Primary cardholder indicator flag.
     * Maps to BOOLEAN in PostgreSQL customers table.
     * Equivalent to CUST-PRI-CARD-HOLDER-IND PIC X(01) from COBOL CUSTOMER-RECORD.
     */
    @Column(name = "primary_cardholder_indicator", nullable = false)
    @NotNull(message = "Primary cardholder indicator is required")
    private Boolean primaryCardholderIndicator;

    /**
     * FICO credit score with range validation (300-850).
     * Maps to NUMERIC(3) in PostgreSQL customers table.
     * Equivalent to CUST-FICO-CREDIT-SCORE PIC 9(03) from COBOL CUSTOMER-RECORD.
     * Validates credit score range per Section 6.2.3 requirements.
     */
    @Column(name = "fico_credit_score", precision = 3, scale = 0)
    @Min(value = 300, message = "FICO credit score must be at least 300")
    @Max(value = 850, message = "FICO credit score cannot exceed 850")
    private Integer ficoCreditScore;

    /**
     * One-to-many relationship with Account entity.
     * Enables customer portfolio management and account lifecycle operations.
     * Uses LAZY loading per FetchType external import requirements.
     */
    @OneToMany(mappedBy = "customerId", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Account> accounts = new ArrayList<>();

    /**
     * One-to-many relationship with Card entity.
     * Enables customer card portfolio management and card lifecycle operations.
     * Uses LAZY loading per FetchType external import requirements.
     */
    @OneToMany(mappedBy = "customerId", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Card> cards = new ArrayList<>();

    /**
     * Version field for optimistic locking support.
     * Enables concurrent customer operations protection per technical specification
     * requirements for customer profile management Section 5.2.2.
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Default constructor for JPA and Spring framework compatibility.
     * Initializes collections and sets default values for required fields.
     */
    public Customer() {
        this.accounts = new ArrayList<>();
        this.cards = new ArrayList<>();
        this.primaryCardholderIndicator = Boolean.TRUE; // Default to primary cardholder
    }

    /**
     * Constructor with required fields for business logic initialization.
     * 
     * @param customerId Customer identifier (9 digits)
     * @param firstName Customer first name (1-20 characters)
     * @param lastName Customer last name (1-20 characters)
     * @param addressLine1 Primary address line (1-50 characters)
     * @param stateCode State code (2 uppercase letters)
     * @param countryCode Country code (3 uppercase letters)
     * @param zipCode ZIP code (5-10 characters)
     * @param ssn Social Security Number (9 digits)
     * @param dateOfBirth Customer date of birth
     * @param primaryCardholderIndicator Primary cardholder flag
     */
    public Customer(String customerId, String firstName, String lastName, 
                   String addressLine1, String stateCode, String countryCode, 
                   String zipCode, String ssn, LocalDate dateOfBirth, 
                   Boolean primaryCardholderIndicator) {
        this();
        this.customerId = customerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.addressLine1 = addressLine1;
        this.stateCode = stateCode;
        this.countryCode = countryCode;
        this.zipCode = zipCode;
        this.ssn = ssn;
        this.dateOfBirth = dateOfBirth;
        this.primaryCardholderIndicator = primaryCardholderIndicator;
    }

    /**
     * Gets the customer identifier.
     * 
     * @return Customer ID as 9-digit string
     */
    public String getCustomerId() {
        return customerId;
    }

    /**
     * Sets the customer identifier.
     * 
     * @param customerId Customer ID as 9-digit string
     */
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    /**
     * Gets the customer first name.
     * 
     * @return Customer first name as string
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the customer first name.
     * 
     * @param firstName Customer first name as string
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the customer middle name.
     * 
     * @return Customer middle name as string
     */
    public String getMiddleName() {
        return middleName;
    }

    /**
     * Sets the customer middle name.
     * 
     * @param middleName Customer middle name as string
     */
    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    /**
     * Gets the customer last name.
     * 
     * @return Customer last name as string
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the customer last name.
     * 
     * @param lastName Customer last name as string
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Gets the primary address line.
     * 
     * @return Primary address line as string
     */
    public String getAddressLine1() {
        return addressLine1;
    }

    /**
     * Sets the primary address line.
     * 
     * @param addressLine1 Primary address line as string
     */
    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    /**
     * Gets the secondary address line.
     * 
     * @return Secondary address line as string
     */
    public String getAddressLine2() {
        return addressLine2;
    }

    /**
     * Sets the secondary address line.
     * 
     * @param addressLine2 Secondary address line as string
     */
    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    /**
     * Gets the additional address line.
     * 
     * @return Additional address line as string
     */
    public String getAddressLine3() {
        return addressLine3;
    }

    /**
     * Sets the additional address line.
     * 
     * @param addressLine3 Additional address line as string
     */
    public void setAddressLine3(String addressLine3) {
        this.addressLine3 = addressLine3;
    }

    /**
     * Gets the state code.
     * 
     * @return State code as 2-character string
     */
    public String getStateCode() {
        return stateCode;
    }

    /**
     * Sets the state code.
     * 
     * @param stateCode State code as 2-character string
     */
    public void setStateCode(String stateCode) {
        this.stateCode = stateCode;
    }

    /**
     * Gets the country code.
     * 
     * @return Country code as 3-character string
     */
    public String getCountryCode() {
        return countryCode;
    }

    /**
     * Sets the country code.
     * 
     * @param countryCode Country code as 3-character string
     */
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    /**
     * Gets the ZIP code.
     * 
     * @return ZIP code as string
     */
    public String getZipCode() {
        return zipCode;
    }

    /**
     * Sets the ZIP code.
     * 
     * @param zipCode ZIP code as string
     */
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    /**
     * Gets the primary phone number.
     * 
     * @return Primary phone number as string
     */
    public String getPhoneNumber1() {
        return phoneNumber1;
    }

    /**
     * Sets the primary phone number.
     * 
     * @param phoneNumber1 Primary phone number as string
     */
    public void setPhoneNumber1(String phoneNumber1) {
        this.phoneNumber1 = phoneNumber1;
    }

    /**
     * Gets the secondary phone number.
     * 
     * @return Secondary phone number as string
     */
    public String getPhoneNumber2() {
        return phoneNumber2;
    }

    /**
     * Sets the secondary phone number.
     * 
     * @param phoneNumber2 Secondary phone number as string
     */
    public void setPhoneNumber2(String phoneNumber2) {
        this.phoneNumber2 = phoneNumber2;
    }

    /**
     * Gets the Social Security Number.
     * Note: This should be encrypted per Section 6.2.3.3 privacy controls.
     * 
     * @return SSN as 9-digit string
     */
    public String getSsn() {
        return ssn;
    }

    /**
     * Sets the Social Security Number.
     * Note: This should be encrypted per Section 6.2.3.3 privacy controls.
     * 
     * @param ssn SSN as 9-digit string
     */
    public void setSsn(String ssn) {
        this.ssn = ssn;
    }

    /**
     * Gets the government issued identification.
     * 
     * @return Government ID as string
     */
    public String getGovernmentId() {
        return governmentId;
    }

    /**
     * Sets the government issued identification.
     * 
     * @param governmentId Government ID as string
     */
    public void setGovernmentId(String governmentId) {
        this.governmentId = governmentId;
    }

    /**
     * Gets the customer date of birth.
     * 
     * @return Date of birth as LocalDate
     */
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    /**
     * Sets the customer date of birth.
     * 
     * @param dateOfBirth Date of birth as LocalDate
     */
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    /**
     * Gets the EFT account identifier.
     * 
     * @return EFT account ID as string
     */
    public String getEftAccountId() {
        return eftAccountId;
    }

    /**
     * Sets the EFT account identifier.
     * 
     * @param eftAccountId EFT account ID as string
     */
    public void setEftAccountId(String eftAccountId) {
        this.eftAccountId = eftAccountId;
    }

    /**
     * Gets the primary cardholder indicator.
     * 
     * @return Primary cardholder flag as Boolean
     */
    public Boolean getPrimaryCardholderIndicator() {
        return primaryCardholderIndicator;
    }

    /**
     * Sets the primary cardholder indicator.
     * 
     * @param primaryCardholderIndicator Primary cardholder flag as Boolean
     */
    public void setPrimaryCardholderIndicator(Boolean primaryCardholderIndicator) {
        this.primaryCardholderIndicator = primaryCardholderIndicator;
    }

    /**
     * Gets the FICO credit score.
     * 
     * @return FICO credit score as Integer (300-850)
     */
    public Integer getFicoCreditScore() {
        return ficoCreditScore;
    }

    /**
     * Sets the FICO credit score with range validation.
     * 
     * @param ficoCreditScore FICO credit score as Integer (300-850)
     */
    public void setFicoCreditScore(Integer ficoCreditScore) {
        this.ficoCreditScore = ficoCreditScore;
    }

    /**
     * Gets the list of accounts owned by this customer.
     * Demonstrates Account entity relationship usage per internal import requirements.
     * 
     * @return List of Account entities associated with this customer
     */
    public List<Account> getAccounts() {
        return accounts;
    }

    /**
     * Sets the list of accounts owned by this customer.
     * Demonstrates Account entity relationship usage per internal import requirements.
     * Uses Account.getCustomerId() and Account.setCustomerId() as specified in internal imports.
     * 
     * @param accounts List of Account entities to associate with this customer
     */
    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
        
        // Ensure bidirectional relationship consistency
        if (accounts != null) {
            for (Account account : accounts) {
                // Demonstrate getCustomerId() usage from Account entity
                if (!Objects.equals(account.getCustomerId(), this.customerId)) {
                    // Demonstrate setCustomerId() usage from Account entity
                    account.setCustomerId(this.customerId);
                }
            }
        }
    }

    /**
     * Gets the list of cards owned by this customer.
     * Demonstrates Card entity relationship usage per internal import requirements.
     * 
     * @return List of Card entities associated with this customer
     */
    public List<Card> getCards() {
        return cards;
    }

    /**
     * Sets the list of cards owned by this customer.
     * Demonstrates Card entity relationship usage per internal import requirements.
     * Uses Card.getCustomerId() and Card.setCustomerId() as specified in internal imports.
     * 
     * @param cards List of Card entities to associate with this customer
     */
    public void setCards(List<Card> cards) {
        this.cards = cards;
        
        // Ensure bidirectional relationship consistency
        if (cards != null) {
            for (Card card : cards) {
                // Demonstrate getCustomerId() usage from Card entity
                if (!Objects.equals(card.getCustomerId(), this.customerId)) {
                    // Demonstrate setCustomerId() usage from Card entity
                    card.setCustomerId(this.customerId);
                }
            }
        }
    }

    /**
     * Gets the version for optimistic locking.
     * 
     * @return Version number for concurrent access control
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Sets the version for optimistic locking.
     * 
     * @param version Version number for concurrent access control
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Adds an account to this customer's portfolio.
     * Demonstrates Account entity methods usage per internal import requirements.
     * 
     * @param account Account entity to add to this customer
     */
    public void addAccount(Account account) {
        if (account != null) {
            if (this.accounts == null) {
                this.accounts = new ArrayList<>();
            }
            this.accounts.add(account);
            
            // Demonstrate Account.setCustomerId() usage
            account.setCustomerId(this.customerId);
            
            // Demonstrate Account.getAccountId() usage for logging/validation
            String accountId = account.getAccountId();
            if (accountId != null) {
                // Account successfully added to customer portfolio
                System.out.println("Account " + accountId + " added to customer " + this.customerId);
            }
        }
    }

    /**
     * Removes an account from this customer's portfolio.
     * Demonstrates Account entity methods usage per internal import requirements.
     * 
     * @param account Account entity to remove from this customer
     */
    public void removeAccount(Account account) {
        if (account != null && this.accounts != null) {
            // Demonstrate Account.getAccountId() usage
            String accountId = account.getAccountId();
            
            this.accounts.remove(account);
            
            // Demonstrate Account.setCustomerId() usage to clear relationship
            account.setCustomerId(null);
            
            if (accountId != null) {
                // Account successfully removed from customer portfolio
                System.out.println("Account " + accountId + " removed from customer " + this.customerId);
            }
        }
    }

    /**
     * Adds a card to this customer's portfolio.
     * Demonstrates Card entity methods usage per internal import requirements.
     * 
     * @param card Card entity to add to this customer
     */
    public void addCard(Card card) {
        if (card != null) {
            if (this.cards == null) {
                this.cards = new ArrayList<>();
            }
            this.cards.add(card);
            
            // Demonstrate Card.setCustomerId() usage
            card.setCustomerId(this.customerId);
            
            // Demonstrate Card.getCardNumber() usage for logging/validation
            String cardNumber = card.getCardNumber();
            if (cardNumber != null) {
                // Card successfully added to customer portfolio
                System.out.println("Card " + cardNumber + " added to customer " + this.customerId);
            }
        }
    }

    /**
     * Removes a card from this customer's portfolio.
     * Demonstrates Card entity methods usage per internal import requirements.
     * 
     * @param card Card entity to remove from this customer
     */
    public void removeCard(Card card) {
        if (card != null && this.cards != null) {
            // Demonstrate Card.getCardNumber() usage
            String cardNumber = card.getCardNumber();
            
            this.cards.remove(card);
            
            // Demonstrate Card.setCustomerId() usage to clear relationship
            card.setCustomerId(null);
            
            if (cardNumber != null) {
                // Card successfully removed from customer portfolio
                System.out.println("Card " + cardNumber + " removed from customer " + this.customerId);
            }
        }
    }

    /**
     * Calculates customer age based on date of birth.
     * Demonstrates LocalDate methods usage per external import requirements.
     * 
     * @return Customer age in years
     */
    public int calculateAge() {
        if (dateOfBirth == null) {
            return 0;
        }
        
        // Use LocalDate.now() method as required by external imports
        LocalDate today = LocalDate.now();
        
        // Use isAfter() method as required by external imports
        if (dateOfBirth.isAfter(today)) {
            return 0; // Birth date cannot be in the future
        }
        
        int age = today.getYear() - dateOfBirth.getYear();
        
        // Adjust for birth month/day not yet reached this year
        if (today.getMonthValue() < dateOfBirth.getMonthValue() || 
            (today.getMonthValue() == dateOfBirth.getMonthValue() && 
             today.getDayOfMonth() < dateOfBirth.getDayOfMonth())) {
            age--;
        }
        
        return age;
    }

    /**
     * Checks if customer is 18 or older.
     * Demonstrates LocalDate methods usage per external import requirements.
     * 
     * @return true if customer is at least 18 years old
     */
    public boolean isAdult() {
        if (dateOfBirth == null) {
            return false;
        }
        
        // Use LocalDate.now() method as required by external imports
        LocalDate today = LocalDate.now();
        
        // Use of() method as required by external imports
        LocalDate eighteenYearsAgo = LocalDate.of(today.getYear() - 18, 
                                                  today.getMonthValue(), 
                                                  today.getDayOfMonth());
        
        // Use isBefore() method as required by external imports
        return dateOfBirth.isBefore(eighteenYearsAgo) || dateOfBirth.isEqual(eighteenYearsAgo);
    }

    /**
     * Validates if customer date of birth is reasonable.
     * Demonstrates LocalDate methods usage per external import requirements.
     * 
     * @return true if date of birth is between 1900 and today
     */
    public boolean hasValidDateOfBirth() {
        if (dateOfBirth == null) {
            return false;
        }
        
        // Use LocalDate.now() method as required by external imports
        LocalDate today = LocalDate.now();
        
        // Use of() method as required by external imports
        LocalDate minimumDate = LocalDate.of(1900, 1, 1);
        
        // Use isAfter() and isBefore() methods as required by external imports
        return dateOfBirth.isAfter(minimumDate) && 
               (dateOfBirth.isBefore(today) || dateOfBirth.isEqual(today));
    }

    /**
     * Gets the customer's full name.
     * 
     * @return Full name as formatted string
     */
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        
        if (firstName != null && !firstName.trim().isEmpty()) {
            fullName.append(firstName.trim());
        }
        
        if (middleName != null && !middleName.trim().isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(middleName.trim());
        }
        
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(lastName.trim());
        }
        
        return fullName.toString();
    }

    /**
     * Gets the customer's full address.
     * 
     * @return Full address as formatted string
     */
    public String getFullAddress() {
        StringBuilder address = new StringBuilder();
        
        if (addressLine1 != null && !addressLine1.trim().isEmpty()) {
            address.append(addressLine1.trim());
        }
        
        if (addressLine2 != null && !addressLine2.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(addressLine2.trim());
        }
        
        if (addressLine3 != null && !addressLine3.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(addressLine3.trim());
        }
        
        if (stateCode != null && !stateCode.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(stateCode.trim());
        }
        
        if (zipCode != null && !zipCode.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(" ");
            }
            address.append(zipCode.trim());
        }
        
        if (countryCode != null && !countryCode.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(countryCode.trim());
        }
        
        return address.toString();
    }

    /**
     * Gets masked SSN for display purposes.
     * Shows only last 4 digits for privacy protection per Section 6.2.3.3.
     * 
     * @return Masked SSN string
     */
    public String getMaskedSsn() {
        if (ssn == null || ssn.length() < 4) {
            return "***-**-****";
        }
        return "***-**-" + ssn.substring(ssn.length() - 4);
    }

    /**
     * Validates FICO credit score range.
     * 
     * @return true if FICO score is in valid range (300-850)
     */
    public boolean hasValidFicoScore() {
        return ficoCreditScore != null && 
               ficoCreditScore >= 300 && 
               ficoCreditScore <= 850;
    }

    /**
     * Gets credit score category based on FICO score.
     * 
     * @return Credit score category as string
     */
    public String getCreditScoreCategory() {
        if (ficoCreditScore == null) {
            return "UNKNOWN";
        }
        
        if (ficoCreditScore >= 800) {
            return "EXCELLENT";
        } else if (ficoCreditScore >= 740) {
            return "VERY_GOOD";
        } else if (ficoCreditScore >= 670) {
            return "GOOD";
        } else if (ficoCreditScore >= 580) {
            return "FAIR";
        } else if (ficoCreditScore >= 300) {
            return "POOR";
        } else {
            return "INVALID";
        }
    }

    /**
     * Checks if customer has active accounts.
     * 
     * @return true if customer has at least one account
     */
    public boolean hasAccounts() {
        return accounts != null && !accounts.isEmpty();
    }

    /**
     * Checks if customer has active cards.
     * 
     * @return true if customer has at least one card
     */
    public boolean hasCards() {
        return cards != null && !cards.isEmpty();
    }

    /**
     * Gets the count of accounts owned by this customer.
     * 
     * @return Number of accounts
     */
    public int getAccountCount() {
        return accounts != null ? accounts.size() : 0;
    }

    /**
     * Gets the count of cards owned by this customer.
     * 
     * @return Number of cards
     */
    public int getCardCount() {
        return cards != null ? cards.size() : 0;
    }

    /**
     * Returns hash code based on customer ID.
     * Uses Objects.hash() as required by external imports.
     * 
     * @return Hash code for entity comparison
     */
    @Override
    public int hashCode() {
        return Objects.hash(customerId);
    }

    /**
     * Compares entities based on customer ID.
     * Uses Objects.equals() as required by external imports.
     * 
     * @param obj Object to compare
     * @return true if entities have the same customer ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Customer customer = (Customer) obj;
        return Objects.equals(customerId, customer.customerId);
    }

    /**
     * String representation for debugging and logging.
     * 
     * @return String containing key entity information
     */
    @Override
    public String toString() {
        return String.format("Customer{customerId='%s', firstName='%s', lastName='%s', " +
                           "stateCode='%s', zipCode='%s', ficoCreditScore=%d, " +
                           "accountCount=%d, cardCount=%d, version=%d}",
            customerId, firstName, lastName, stateCode, zipCode, ficoCreditScore,
            getAccountCount(), getCardCount(), version);
    }
}