package com.carddemo.account.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA Entity representing customer master data converted from COBOL CUSTOMER-RECORD.
 * Maintains exact field mappings and data precision from the original VSAM CUSTDAT file
 * while providing modern JPA capabilities, PII field protection, and cascade relationships.
 * 
 * Original COBOL Structure: CUSTOMER-RECORD (RECLN 500)
 * Target Table: customers
 * 
 * Key Features:
 * - PII field protection for SSN and government-issued ID
 * - FICO credit score validation (300-850 range)
 * - Cascade relationships with accounts and cards
 * - Jakarta Bean Validation for business rule enforcement
 * - PostgreSQL table mapping with normalized address structure
 */
@Entity
@Table(name = "customers", 
       indexes = {
           @Index(name = "idx_customer_ssn", columnList = "ssn"),
           @Index(name = "idx_customer_name", columnList = "lastName, firstName"),
           @Index(name = "idx_customer_fico", columnList = "ficoCreditScore")
       })
public class Customer {

    /**
     * Customer ID - Primary Key
     * Mapped from COBOL: CUST-ID PIC 9(09)
     * Constraint: Exactly 9 digits as per mainframe format
     */
    @Id
    @Column(name = "customer_id", length = 9, nullable = false)
    @NotNull(message = "Customer ID is required")
    @Pattern(regexp = "^[0-9]{9}$", message = "Customer ID must be exactly 9 digits")
    private String customerId;

    /**
     * Customer First Name
     * Mapped from COBOL: CUST-FIRST-NAME PIC X(25)
     */
    @Column(name = "first_name", length = 25, nullable = false)
    @NotBlank(message = "First name is required")
    @Size(max = 25, message = "First name cannot exceed 25 characters")
    private String firstName;

    /**
     * Customer Middle Name
     * Mapped from COBOL: CUST-MIDDLE-NAME PIC X(25)
     */
    @Column(name = "middle_name", length = 25)
    @Size(max = 25, message = "Middle name cannot exceed 25 characters")
    private String middleName;

    /**
     * Customer Last Name
     * Mapped from COBOL: CUST-LAST-NAME PIC X(25)
     */
    @Column(name = "last_name", length = 25, nullable = false)
    @NotBlank(message = "Last name is required")
    @Size(max = 25, message = "Last name cannot exceed 25 characters")
    private String lastName;

    /**
     * Customer Address Line 1
     * Mapped from COBOL: CUST-ADDR-LINE-1 PIC X(50)
     */
    @Column(name = "address_line1", length = 50, nullable = false)
    @NotBlank(message = "Address line 1 is required")
    @Size(max = 50, message = "Address line 1 cannot exceed 50 characters")
    private String addressLine1;

    /**
     * Customer Address Line 2
     * Mapped from COBOL: CUST-ADDR-LINE-2 PIC X(50)
     */
    @Column(name = "address_line2", length = 50)
    @Size(max = 50, message = "Address line 2 cannot exceed 50 characters")
    private String addressLine2;

    /**
     * Customer Address Line 3
     * Mapped from COBOL: CUST-ADDR-LINE-3 PIC X(50)
     */
    @Column(name = "address_line3", length = 50)
    @Size(max = 50, message = "Address line 3 cannot exceed 50 characters")
    private String addressLine3;

    /**
     * State Code
     * Mapped from COBOL: CUST-ADDR-STATE-CD PIC X(02)
     */
    @Column(name = "state_code", length = 2, nullable = false)
    @NotBlank(message = "State code is required")
    @Size(min = 2, max = 2, message = "State code must be exactly 2 characters")
    @Pattern(regexp = "^[A-Z]{2}$", message = "State code must be 2 uppercase letters")
    private String stateCode;

    /**
     * Country Code
     * Mapped from COBOL: CUST-ADDR-COUNTRY-CD PIC X(03)
     */
    @Column(name = "country_code", length = 3, nullable = false)
    @NotBlank(message = "Country code is required")
    @Size(min = 3, max = 3, message = "Country code must be exactly 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Country code must be 3 uppercase letters")
    private String countryCode;

    /**
     * ZIP Code
     * Mapped from COBOL: CUST-ADDR-ZIP PIC X(10)
     */
    @Column(name = "zip_code", length = 10, nullable = false)
    @NotBlank(message = "ZIP code is required")
    @Size(max = 10, message = "ZIP code cannot exceed 10 characters")
    @Pattern(regexp = "^[0-9]{5}(-[0-9]{4})?$", message = "ZIP code must be in format 12345 or 12345-6789")
    private String zipCode;

    /**
     * Primary Phone Number
     * Mapped from COBOL: CUST-PHONE-NUM-1 PIC X(15)
     */
    @Column(name = "phone_number1", length = 15)
    @Size(max = 15, message = "Phone number 1 cannot exceed 15 characters")
    private String phoneNumber1;

    /**
     * Secondary Phone Number
     * Mapped from COBOL: CUST-PHONE-NUM-2 PIC X(15)
     */
    @Column(name = "phone_number2", length = 15)
    @Size(max = 15, message = "Phone number 2 cannot exceed 15 characters")
    private String phoneNumber2;

    /**
     * Social Security Number - PII Field with Encryption Support
     * Mapped from COBOL: CUST-SSN PIC 9(09)
     * CRITICAL: This field contains PII and must be encrypted at rest per Section 6.2.3.3
     */
    @Column(name = "ssn", length = 9, nullable = false)
    @NotNull(message = "SSN is required")
    @Pattern(regexp = "^[0-9]{9}$", message = "SSN must be exactly 9 digits")
    // NOTE: Actual encryption implementation will be handled by PostgreSQL pgcrypto
    // and Spring Security data protection utilities as per security architecture
    private String ssn;

    /**
     * Government Issued ID - PII Field with Encryption Support
     * Mapped from COBOL: CUST-GOVT-ISSUED-ID PIC X(20)
     * CRITICAL: This field contains PII and must be encrypted at rest per Section 6.2.3.3
     */
    @Column(name = "government_issued_id", length = 20)
    @Size(max = 20, message = "Government issued ID cannot exceed 20 characters")
    // NOTE: Actual encryption implementation will be handled by PostgreSQL pgcrypto
    // and Spring Security data protection utilities as per security architecture
    private String governmentIssuedId;

    /**
     * Date of Birth
     * Mapped from COBOL: CUST-DOB-YYYYMMDD PIC X(10)
     * Converted from string format to LocalDate for proper date handling
     */
    @Column(name = "date_of_birth", nullable = false)
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    /**
     * EFT Account ID
     * Mapped from COBOL: CUST-EFT-ACCOUNT-ID PIC X(10)
     */
    @Column(name = "eft_account_id", length = 10)
    @Size(max = 10, message = "EFT account ID cannot exceed 10 characters")
    private String eftAccountId;

    /**
     * Primary Card Holder Indicator
     * Mapped from COBOL: CUST-PRI-CARD-HOLDER-IND PIC X(01)
     */
    @Column(name = "primary_card_holder_indicator", length = 1)
    @Size(max = 1, message = "Primary card holder indicator must be 1 character")
    @Pattern(regexp = "^[YN]$", message = "Primary card holder indicator must be Y or N")
    private String primaryCardHolderIndicator;

    /**
     * FICO Credit Score with Business Rule Validation
     * Mapped from COBOL: CUST-FICO-CREDIT-SCORE PIC 9(03)
     * CRITICAL: Must enforce 300-850 range validation per business requirements
     */
    @Column(name = "fico_credit_score", nullable = false)
    @NotNull(message = "FICO credit score is required")
    @Min(value = 300, message = "FICO credit score must be at least 300")
    @Max(value = 850, message = "FICO credit score cannot exceed 850")
    private Integer ficoCreditScore;

    /**
     * Customer-Account Relationship
     * @OneToMany with cascade operations for account lifecycle management
     * Implements cascade operations per Section 6.2.1.1 schema design
     */
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Account> accounts = new HashSet<>();

    /**
     * Customer-Card Relationship  
     * @OneToMany with cascade operations for card lifecycle management
     * Supports direct customer-card relationships for card management operations
     */
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Card> cards = new HashSet<>();

    // Default Constructor
    public Customer() {
        this.accounts = new HashSet<>();
        this.cards = new HashSet<>();
    }

    // Parameterized Constructor for essential fields
    public Customer(String customerId, String firstName, String lastName, 
                   String addressLine1, String stateCode, String countryCode, 
                   String zipCode, String ssn, LocalDate dateOfBirth, Integer ficoCreditScore) {
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
        this.ficoCreditScore = ficoCreditScore;
    }

    // Getters and Setters

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

    // Relationship Management Methods

    public Set<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(Set<Account> accounts) {
        this.accounts = accounts != null ? accounts : new HashSet<>();
    }

    /**
     * Add an account to this customer with bidirectional relationship management
     * Maintains referential integrity and cascade operations
     */
    public void addAccount(Account account) {
        if (account != null) {
            this.accounts.add(account);
            account.setCustomer(this);
        }
    }

    /**
     * Remove an account from this customer with bidirectional relationship management
     * Maintains referential integrity and cascade operations
     */
    public void removeAccount(Account account) {
        if (account != null) {
            this.accounts.remove(account);
            account.setCustomer(null);
        }
    }

    public Set<Card> getCards() {
        return cards;
    }

    public void setCards(Set<Card> cards) {
        this.cards = cards != null ? cards : new HashSet<>();
    }

    /**
     * Add a card to this customer with bidirectional relationship management
     * Maintains referential integrity and cascade operations
     */
    public void addCard(Card card) {
        if (card != null) {
            this.cards.add(card);
            card.setCustomer(this);
        }
    }

    /**
     * Remove a card from this customer with bidirectional relationship management
     * Maintains referential integrity and cascade operations
     */
    public void removeCard(Card card) {
        if (card != null) {
            this.cards.remove(card);
            card.setCustomer(null);
        }
    }

    // Business Logic Methods

    /**
     * Get full customer name in "Last, First Middle" format
     * Replicates COBOL name formatting logic
     */
    public String getFullName() {
        StringBuilder fullName = new StringBuilder(lastName);
        fullName.append(", ").append(firstName);
        if (middleName != null && !middleName.trim().isEmpty()) {
            fullName.append(" ").append(middleName);
        }
        return fullName.toString();
    }

    /**
     * Get complete address as a single formatted string
     * Replicates COBOL address formatting logic
     */
    public String getFormattedAddress() {
        StringBuilder address = new StringBuilder(addressLine1);
        if (addressLine2 != null && !addressLine2.trim().isEmpty()) {
            address.append(", ").append(addressLine2);
        }
        if (addressLine3 != null && !addressLine3.trim().isEmpty()) {
            address.append(", ").append(addressLine3);
        }
        address.append(", ").append(stateCode).append(" ").append(zipCode);
        address.append(", ").append(countryCode);
        return address.toString();
    }

    /**
     * Validate FICO credit score is within acceptable business range
     * Implements business rule validation matching COBOL field validation
     */
    public boolean isValidFicoScore() {
        return ficoCreditScore != null && ficoCreditScore >= 300 && ficoCreditScore <= 850;
    }

    /**
     * Check if customer is a primary card holder
     * Replicates COBOL 88-level condition logic
     */
    public boolean isPrimaryCardHolder() {
        return "Y".equalsIgnoreCase(primaryCardHolderIndicator);
    }

    /**
     * Get masked SSN for display purposes (PII protection)
     * Shows only last 4 digits: XXX-XX-1234
     */
    public String getMaskedSsn() {
        if (ssn == null || ssn.length() != 9) {
            return "XXX-XX-XXXX";
        }
        return "XXX-XX-" + ssn.substring(5);
    }

    // Standard Object Methods

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Customer customer = (Customer) obj;
        return customerId != null && customerId.equals(customer.customerId);
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
                ", ficoCreditScore=" + ficoCreditScore +
                ", accountCount=" + (accounts != null ? accounts.size() : 0) +
                ", cardCount=" + (cards != null ? cards.size() : 0) +
                '}';
    }
}