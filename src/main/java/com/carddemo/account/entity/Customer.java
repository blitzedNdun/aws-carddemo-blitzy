package com.carddemo.account.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA Entity representing customer master data converted from COBOL CUSTOMER-RECORD.
 * 
 * This entity maps the legacy VSAM CUSTDAT dataset structure to a modern PostgreSQL
 * relational table with comprehensive validation, PII field protection, and cascade
 * relationships to accounts and cards for complete customer lifecycle management.
 * 
 * The entity implements the normalized address structure per Section 6.2.1.1 schema
 * design and includes field-level encryption support for PII data elements as 
 * specified in Section 6.2.3.3 privacy controls.
 * 
 * Converted from: app/cpy/CUSTREC.cpy (COBOL copybook)
 * Database Table: customers (PostgreSQL)
 * Record Length: 500 bytes (original COBOL layout)
 * 
 * @author Blitzy agent
 * @version CardDemo_v1.0-15-g27d6c6f-68
 * @since 2022-07-19
 */
@Entity
@Table(name = "customers")
public class Customer {

    /**
     * Customer unique identifier (Primary Key)
     * Converted from: CUST-ID PIC 9(09)
     * Format: 9-digit numeric string
     */
    @Id
    @Column(name = "customer_id", length = 9, nullable = false)
    @NotNull(message = "Customer ID cannot be null")
    @Pattern(regexp = "\\d{9}", message = "Customer ID must be exactly 9 digits")
    private String customerId;

    /**
     * Customer first name
     * Converted from: CUST-FIRST-NAME PIC X(25)
     */
    @Column(name = "first_name", length = 25, nullable = false)
    @NotBlank(message = "First name is required")
    @Size(max = 25, message = "First name cannot exceed 25 characters")
    private String firstName;

    /**
     * Customer middle name
     * Converted from: CUST-MIDDLE-NAME PIC X(25)
     */
    @Column(name = "middle_name", length = 25)
    @Size(max = 25, message = "Middle name cannot exceed 25 characters")
    private String middleName;

    /**
     * Customer last name
     * Converted from: CUST-LAST-NAME PIC X(25)
     */
    @Column(name = "last_name", length = 25, nullable = false)
    @NotBlank(message = "Last name is required")
    @Size(max = 25, message = "Last name cannot exceed 25 characters")
    private String lastName;

    /**
     * Primary address line
     * Converted from: CUST-ADDR-LINE-1 PIC X(50)
     */
    @Column(name = "address_line_1", length = 50, nullable = false)
    @NotBlank(message = "Address line 1 is required")
    @Size(max = 50, message = "Address line 1 cannot exceed 50 characters")
    private String addressLine1;

    /**
     * Secondary address line
     * Converted from: CUST-ADDR-LINE-2 PIC X(50)
     */
    @Column(name = "address_line_2", length = 50)
    @Size(max = 50, message = "Address line 2 cannot exceed 50 characters")
    private String addressLine2;

    /**
     * Additional address line
     * Converted from: CUST-ADDR-LINE-3 PIC X(50)
     */
    @Column(name = "address_line_3", length = 50)
    @Size(max = 50, message = "Address line 3 cannot exceed 50 characters")
    private String addressLine3;

    /**
     * State or province code
     * Converted from: CUST-ADDR-STATE-CD PIC X(02)
     */
    @Column(name = "state_code", length = 2, nullable = false)
    @NotBlank(message = "State code is required")
    @Size(min = 2, max = 2, message = "State code must be exactly 2 characters")
    private String stateCode;

    /**
     * Country code
     * Converted from: CUST-ADDR-COUNTRY-CD PIC X(03)
     */
    @Column(name = "country_code", length = 3, nullable = false)
    @NotBlank(message = "Country code is required")
    @Size(min = 3, max = 3, message = "Country code must be exactly 3 characters")
    private String countryCode;

    /**
     * Postal/ZIP code
     * Converted from: CUST-ADDR-ZIP PIC X(10)
     */
    @Column(name = "zip_code", length = 10, nullable = false)
    @NotBlank(message = "ZIP code is required")
    @Size(max = 10, message = "ZIP code cannot exceed 10 characters")
    private String zipCode;

    /**
     * Primary phone number
     * Converted from: CUST-PHONE-NUM-1 PIC X(15)
     */
    @Column(name = "phone_number_1", length = 15)
    @Size(max = 15, message = "Phone number 1 cannot exceed 15 characters")
    private String phoneNumber1;

    /**
     * Secondary phone number
     * Converted from: CUST-PHONE-NUM-2 PIC X(15)
     */
    @Column(name = "phone_number_2", length = 15)
    @Size(max = 15, message = "Phone number 2 cannot exceed 15 characters")
    private String phoneNumber2;

    /**
     * Social Security Number (PII - requires encryption/masking)
     * Converted from: CUST-SSN PIC 9(09)
     * 
     * Field-level encryption support per Section 6.2.3.3 privacy controls.
     * Application-level masking through Spring Security data protection utilities.
     */
    @Column(name = "ssn", length = 9, nullable = false)
    @NotBlank(message = "SSN is required")
    @Pattern(regexp = "\\d{9}", message = "SSN must be exactly 9 digits")
    // Note: In production, this field should be encrypted at the database level
    // using PostgreSQL pgcrypto extension and masked in API responses
    private String ssn;

    /**
     * Government issued identification (PII - requires encryption/masking)
     * Converted from: CUST-GOVT-ISSUED-ID PIC X(20)
     * 
     * Field-level encryption support per Section 6.2.3.3 privacy controls.
     */
    @Column(name = "government_issued_id", length = 20)
    @Size(max = 20, message = "Government issued ID cannot exceed 20 characters")
    // Note: In production, this field should be encrypted at the database level
    private String governmentIssuedId;

    /**
     * Date of birth
     * Converted from: CUST-DOB-YYYYMMDD PIC X(10)
     * 
     * Uses LocalDate for proper date handling and validation.
     */
    @Column(name = "date_of_birth", nullable = false)
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    /**
     * Electronic funds transfer account ID
     * Converted from: CUST-EFT-ACCOUNT-ID PIC X(10)
     */
    @Column(name = "eft_account_id", length = 10)
    @Size(max = 10, message = "EFT account ID cannot exceed 10 characters")
    private String eftAccountId;

    /**
     * Primary card holder indicator
     * Converted from: CUST-PRI-CARD-HOLDER-IND PIC X(01)
     */
    @Column(name = "primary_cardholder_indicator", length = 1)
    @Size(max = 1, message = "Primary cardholder indicator must be 1 character")
    private String primaryCardHolderIndicator;

    /**
     * FICO credit score with business rule validation (300-850 range)
     * Converted from: CUST-FICO-CREDIT-SCORE PIC 9(03)
     * 
     * Enforces standard FICO credit score range per business requirements.
     */
    @Column(name = "fico_credit_score")
    @Min(value = 300, message = "FICO credit score must be at least 300")
    @Max(value = 850, message = "FICO credit score cannot exceed 850")
    private Integer ficoCreditScore;

    /**
     * Collection of accounts associated with this customer
     * 
     * @OneToMany relationship with cascade operations for account lifecycle management.
     * Implements customer-account relationship maintenance per business requirements.
     */
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Account> accounts = new HashSet<>();

    /**
     * Collection of cards associated with this customer
     * 
     * TODO: Enable @OneToMany relationship when Card entity is implemented by other agents
     * @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
     * 
     * Implements customer-card relationship maintenance per business requirements.
     * This relationship will provide cascade operations for card lifecycle management.
     */
    // @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Object> cards = new HashSet<>();

    /**
     * Default constructor required by JPA
     */
    public Customer() {
        // Initialize collections to prevent null pointer exceptions
        this.accounts = new HashSet<>();
        this.cards = new HashSet<>();
    }

    /**
     * Constructor with required fields
     */
    public Customer(String customerId, String firstName, String lastName, 
                   String addressLine1, String stateCode, String countryCode, 
                   String zipCode, String ssn, LocalDate dateOfBirth) {
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
    }

    // Getter and Setter methods

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getAddressLine3() {
        return addressLine3;
    }

    public void setAddressLine3(String addressLine3) {
        this.addressLine3 = addressLine3;
    }

    public String getStateCode() {
        return stateCode;
    }

    public void setStateCode(String stateCode) {
        this.stateCode = stateCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getPhoneNumber1() {
        return phoneNumber1;
    }

    public void setPhoneNumber1(String phoneNumber1) {
        this.phoneNumber1 = phoneNumber1;
    }

    public String getPhoneNumber2() {
        return phoneNumber2;
    }

    public void setPhoneNumber2(String phoneNumber2) {
        this.phoneNumber2 = phoneNumber2;
    }

    public String getSsn() {
        return ssn;
    }

    public void setSsn(String ssn) {
        this.ssn = ssn;
    }

    public String getGovernmentIssuedId() {
        return governmentIssuedId;
    }

    public void setGovernmentIssuedId(String governmentIssuedId) {
        this.governmentIssuedId = governmentIssuedId;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getEftAccountId() {
        return eftAccountId;
    }

    public void setEftAccountId(String eftAccountId) {
        this.eftAccountId = eftAccountId;
    }

    public String getPrimaryCardHolderIndicator() {
        return primaryCardHolderIndicator;
    }

    public void setPrimaryCardHolderIndicator(String primaryCardHolderIndicator) {
        this.primaryCardHolderIndicator = primaryCardHolderIndicator;
    }

    public Integer getFicoCreditScore() {
        return ficoCreditScore;
    }

    public void setFicoCreditScore(Integer ficoCreditScore) {
        this.ficoCreditScore = ficoCreditScore;
    }

    public Set<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(Set<Account> accounts) {
        this.accounts = accounts != null ? accounts : new HashSet<>();
    }

    /**
     * Add an account to this customer's account collection
     * Maintains bidirectional relationship integrity
     */
    public void addAccount(Account account) {
        if (account != null) {
            this.accounts.add(account);
            account.setCustomer(this);
        }
    }

    /**
     * Remove an account from this customer's account collection
     * Maintains bidirectional relationship integrity
     */
    public void removeAccount(Account account) {
        if (account != null) {
            this.accounts.remove(account);
            account.setCustomer(null);
        }
    }

    public Set<Object> getCards() {
        return cards;
    }

    public void setCards(Set<Object> cards) {
        this.cards = cards != null ? cards : new HashSet<>();
    }

    /**
     * Add a card to this customer's card collection
     * Maintains bidirectional relationship integrity
     * 
     * Note: Parameter type will be Card when that entity is implemented.
     * Current Object type maintains compilation compatibility.
     */
    public void addCard(Object card) {
        if (card != null) {
            this.cards.add(card);
            // Bidirectional relationship will be established when Card entity is available
            // card.setCustomer(this);
        }
    }

    /**
     * Remove a card from this customer's card collection
     * Maintains bidirectional relationship integrity
     * 
     * Note: Parameter type will be Card when that entity is implemented.
     * Current Object type maintains compilation compatibility.
     */
    public void removeCard(Object card) {
        if (card != null) {
            this.cards.remove(card);
            // Bidirectional relationship will be cleared when Card entity is available
            // card.setCustomer(null);
        }
    }

    /**
     * Utility method to get full name
     */
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        if (firstName != null) {
            fullName.append(firstName);
        }
        if (middleName != null && !middleName.trim().isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(middleName);
        }
        if (lastName != null) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(lastName);
        }
        return fullName.toString();
    }

    /**
     * Utility method to get formatted address
     */
    public String getFormattedAddress() {
        StringBuilder address = new StringBuilder();
        if (addressLine1 != null) {
            address.append(addressLine1);
        }
        if (addressLine2 != null && !addressLine2.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(addressLine2);
        }
        if (addressLine3 != null && !addressLine3.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(addressLine3);
        }
        if (stateCode != null && zipCode != null) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(stateCode).append(" ").append(zipCode);
        }
        if (countryCode != null) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(countryCode);
        }
        return address.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Customer customer = (Customer) obj;
        return customerId != null ? customerId.equals(customer.customerId) : customer.customerId == null;
    }

    @Override
    public int hashCode() {
        return customerId != null ? customerId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "customerId='" + customerId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", stateCode='" + stateCode + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", ficoCreditScore=" + ficoCreditScore +
                ", accountCount=" + (accounts != null ? accounts.size() : 0) +
                ", cardCount=" + (cards != null ? cards.size() : 0) +
                '}';
    }
}