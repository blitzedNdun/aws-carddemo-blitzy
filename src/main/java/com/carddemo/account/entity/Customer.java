package com.carddemo.account.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Customer JPA entity representing customer master data converted from COBOL CUSTOMER-RECORD.
 * This entity implements the customers table mapping with normalized address structure,
 * PII field protection, FICO credit scoring, and cascade relationships to accounts and cards
 * supporting comprehensive customer management operations.
 * 
 * Mapped from COBOL copybook: app/cpy/CUSTREC.cpy
 * Database table: customers
 * Record length: 500 bytes (original COBOL structure)
 * 
 * Key Features:
 * - Primary key: 9-digit customer ID (CUST-ID)
 * - Personal information with validation
 * - Address structure with normalized fields
 * - PII protection for SSN and government ID
 * - FICO credit score validation (300-850 range)
 * - Date of birth with past validation
 * - Cascade relationships to Account and Card entities
 * 
 * @version CardDemo_v1.0-15-g27d6c6f-68
 * @since 2022-07-19
 */
@Entity
@Table(name = "customers", schema = "public")
public class Customer {

    /**
     * Primary key: 9-digit customer identifier
     * Mapped from COBOL: CUST-ID PIC 9(09)
     * Database constraint: Primary key, NOT NULL, length=9
     */
    @Id
    @Column(name = "customer_id", length = 9, nullable = false)
    @NotNull(message = "Customer ID is required")
    @Pattern(regexp = "\\d{9}", message = "Customer ID must be exactly 9 digits")
    private String customerId;

    /**
     * Customer first name
     * Mapped from COBOL: CUST-FIRST-NAME PIC X(25)
     * Database constraint: VARCHAR(25), NOT NULL
     */
    @Column(name = "first_name", length = 25, nullable = false)
    @NotNull(message = "First name is required")
    @Size(min = 1, max = 25, message = "First name must be between 1 and 25 characters")
    private String firstName;

    /**
     * Customer middle name
     * Mapped from COBOL: CUST-MIDDLE-NAME PIC X(25)
     * Database constraint: VARCHAR(25), nullable
     */
    @Column(name = "middle_name", length = 25)
    @Size(max = 25, message = "Middle name must not exceed 25 characters")
    private String middleName;

    /**
     * Customer last name
     * Mapped from COBOL: CUST-LAST-NAME PIC X(25)
     * Database constraint: VARCHAR(25), NOT NULL
     */
    @Column(name = "last_name", length = 25, nullable = false)
    @NotNull(message = "Last name is required")
    @Size(min = 1, max = 25, message = "Last name must be between 1 and 25 characters")
    private String lastName;

    /**
     * Primary address line
     * Mapped from COBOL: CUST-ADDR-LINE-1 PIC X(50)
     * Database constraint: VARCHAR(50), NOT NULL
     */
    @Column(name = "address_line_1", length = 50, nullable = false)
    @NotNull(message = "Address line 1 is required")
    @Size(min = 1, max = 50, message = "Address line 1 must be between 1 and 50 characters")
    private String addressLine1;

    /**
     * Secondary address line
     * Mapped from COBOL: CUST-ADDR-LINE-2 PIC X(50)
     * Database constraint: VARCHAR(50), nullable
     */
    @Column(name = "address_line_2", length = 50)
    @Size(max = 50, message = "Address line 2 must not exceed 50 characters")
    private String addressLine2;

    /**
     * Additional address line
     * Mapped from COBOL: CUST-ADDR-LINE-3 PIC X(50)
     * Database constraint: VARCHAR(50), nullable
     */
    @Column(name = "address_line_3", length = 50)
    @Size(max = 50, message = "Address line 3 must not exceed 50 characters")
    private String addressLine3;

    /**
     * State code (2-character abbreviation)
     * Mapped from COBOL: CUST-ADDR-STATE-CD PIC X(02)
     * Database constraint: VARCHAR(2), NOT NULL
     */
    @Column(name = "state_code", length = 2, nullable = false)
    @NotNull(message = "State code is required")
    @Size(min = 2, max = 2, message = "State code must be exactly 2 characters")
    @Pattern(regexp = "[A-Z]{2}", message = "State code must be 2 uppercase letters")
    private String stateCode;

    /**
     * Country code (3-character abbreviation)
     * Mapped from COBOL: CUST-ADDR-COUNTRY-CD PIC X(03)
     * Database constraint: VARCHAR(3), NOT NULL
     */
    @Column(name = "country_code", length = 3, nullable = false)
    @NotNull(message = "Country code is required")
    @Size(min = 3, max = 3, message = "Country code must be exactly 3 characters")
    @Pattern(regexp = "[A-Z]{3}", message = "Country code must be 3 uppercase letters")
    private String countryCode;

    /**
     * ZIP/Postal code
     * Mapped from COBOL: CUST-ADDR-ZIP PIC X(10)
     * Database constraint: VARCHAR(10), NOT NULL
     */
    @Column(name = "zip_code", length = 10, nullable = false)
    @NotNull(message = "ZIP code is required")
    @Size(min = 5, max = 10, message = "ZIP code must be between 5 and 10 characters")
    @Pattern(regexp = "\\d{5}(-\\d{4})?", message = "ZIP code must be in format 12345 or 12345-6789")
    private String zipCode;

    /**
     * Primary phone number
     * Mapped from COBOL: CUST-PHONE-NUM-1 PIC X(15)
     * Database constraint: VARCHAR(15), nullable
     */
    @Column(name = "phone_number_1", length = 15)
    @Size(max = 15, message = "Phone number 1 must not exceed 15 characters")
    @Pattern(regexp = "\\d{10,15}", message = "Phone number must be 10-15 digits")
    private String phoneNumber1;

    /**
     * Secondary phone number
     * Mapped from COBOL: CUST-PHONE-NUM-2 PIC X(15)
     * Database constraint: VARCHAR(15), nullable
     */
    @Column(name = "phone_number_2", length = 15)
    @Size(max = 15, message = "Phone number 2 must not exceed 15 characters")
    @Pattern(regexp = "\\d{10,15}", message = "Phone number must be 10-15 digits")
    private String phoneNumber2;

    /**
     * Social Security Number (PII field with encryption support)
     * Mapped from COBOL: CUST-SSN PIC 9(09)
     * Database constraint: VARCHAR(9), NOT NULL
     * Security: Field-level encryption required per Section 6.2.3.3 privacy controls
     */
    @Column(name = "ssn", length = 9, nullable = false)
    @NotNull(message = "SSN is required")
    @Pattern(regexp = "\\d{9}", message = "SSN must be exactly 9 digits")
    private String ssn;

    /**
     * Government issued ID (PII field with encryption support)
     * Mapped from COBOL: CUST-GOVT-ISSUED-ID PIC X(20)
     * Database constraint: VARCHAR(20), nullable
     * Security: Field-level encryption required per Section 6.2.3.3 privacy controls
     */
    @Column(name = "government_issued_id", length = 20)
    @Size(max = 20, message = "Government issued ID must not exceed 20 characters")
    private String governmentIssuedId;

    /**
     * Date of birth with past validation
     * Mapped from COBOL: CUST-DOB-YYYYMMDD PIC X(10)
     * Database constraint: DATE, NOT NULL
     * Validation: Must be in the past
     */
    @Column(name = "date_of_birth", nullable = false)
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    /**
     * EFT account identifier
     * Mapped from COBOL: CUST-EFT-ACCOUNT-ID PIC X(10)
     * Database constraint: VARCHAR(10), nullable
     */
    @Column(name = "eft_account_id", length = 10)
    @Size(max = 10, message = "EFT account ID must not exceed 10 characters")
    private String eftAccountId;

    /**
     * Primary cardholder indicator
     * Mapped from COBOL: CUST-PRI-CARD-HOLDER-IND PIC X(01)
     * Database constraint: VARCHAR(1), NOT NULL, default 'Y'
     */
    @Column(name = "primary_cardholder_indicator", length = 1, nullable = false)
    @NotNull(message = "Primary cardholder indicator is required")
    @Pattern(regexp = "[YN]", message = "Primary cardholder indicator must be 'Y' or 'N'")
    private String primaryCardHolderIndicator = "Y";

    /**
     * FICO credit score with 300-850 range validation
     * Mapped from COBOL: CUST-FICO-CREDIT-SCORE PIC 9(03)
     * Database constraint: INTEGER, NOT NULL
     * Business rule: Must be between 300 and 850 (standard FICO range)
     */
    @Column(name = "fico_credit_score", nullable = false)
    @NotNull(message = "FICO credit score is required")
    @Min(value = 300, message = "FICO credit score must be at least 300")
    @Max(value = 850, message = "FICO credit score must not exceed 850")
    private Integer ficoCreditScore;

    /**
     * One-to-many relationship to Account entities
     * Cascade operations for account lifecycle management
     * Mapped by customer_id foreign key in accounts table
     */
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Account> accounts = new HashSet<>();

    /**
     * One-to-many relationship to Card entities
     * Cascade operations for card lifecycle management
     * Mapped by customer_id foreign key in cards table
     */
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Card> cards = new HashSet<>();

    /**
     * Default constructor for JPA
     */
    public Customer() {
        // Default constructor required by JPA
    }

    /**
     * Constructor with required fields
     * 
     * @param customerId Customer ID (9 digits)
     * @param firstName Customer first name
     * @param lastName Customer last name
     * @param addressLine1 Primary address
     * @param stateCode State code (2 letters)
     * @param countryCode Country code (3 letters)
     * @param zipCode ZIP code
     * @param ssn Social Security Number (9 digits)
     * @param dateOfBirth Date of birth
     * @param ficoCreditScore FICO credit score (300-850)
     */
    public Customer(String customerId, String firstName, String lastName, 
                   String addressLine1, String stateCode, String countryCode, 
                   String zipCode, String ssn, LocalDate dateOfBirth, 
                   Integer ficoCreditScore) {
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
        this.primaryCardHolderIndicator = "Y";
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

    public Set<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(Set<Account> accounts) {
        this.accounts = accounts != null ? accounts : new HashSet<>();
    }

    public Set<Card> getCards() {
        return cards;
    }

    public void setCards(Set<Card> cards) {
        this.cards = cards != null ? cards : new HashSet<>();
    }

    // Helper methods for managing relationships

    /**
     * Add an account to this customer
     * Maintains bidirectional relationship
     * 
     * @param account Account to add
     */
    public void addAccount(Account account) {
        if (account != null) {
            this.accounts.add(account);
            account.setCustomer(this);
        }
    }

    /**
     * Remove an account from this customer
     * Maintains bidirectional relationship
     * 
     * @param account Account to remove
     */
    public void removeAccount(Account account) {
        if (account != null) {
            this.accounts.remove(account);
            account.setCustomer(null);
        }
    }

    /**
     * Add a card to this customer
     * Maintains bidirectional relationship
     * 
     * @param card Card to add
     */
    public void addCard(Card card) {
        if (card != null) {
            this.cards.add(card);
            card.setCustomer(this);
        }
    }

    /**
     * Remove a card from this customer
     * Maintains bidirectional relationship
     * 
     * @param card Card to remove
     */
    public void removeCard(Card card) {
        if (card != null) {
            this.cards.remove(card);
            card.setCustomer(null);
        }
    }

    // Business methods

    /**
     * Get full name combining first, middle, and last names
     * 
     * @return Full name as concatenated string
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
     * Get formatted address as single string
     * 
     * @return Formatted address with line breaks
     */
    public String getFormattedAddress() {
        StringBuilder address = new StringBuilder();
        
        if (addressLine1 != null) {
            address.append(addressLine1);
        }
        
        if (addressLine2 != null && !addressLine2.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append("\n");
            }
            address.append(addressLine2);
        }
        
        if (addressLine3 != null && !addressLine3.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append("\n");
            }
            address.append(addressLine3);
        }
        
        // Add city, state, zip line
        if (stateCode != null && zipCode != null) {
            if (address.length() > 0) {
                address.append("\n");
            }
            address.append(stateCode).append(" ").append(zipCode);
        }
        
        return address.toString();
    }

    /**
     * Check if customer is a primary cardholder
     * 
     * @return true if primary cardholder indicator is 'Y'
     */
    public boolean isPrimaryCardHolder() {
        return "Y".equals(primaryCardHolderIndicator);
    }

    /**
     * Calculate age based on date of birth
     * 
     * @return Age in years
     */
    public int getAge() {
        if (dateOfBirth != null) {
            return LocalDate.now().getYear() - dateOfBirth.getYear();
        }
        return 0;
    }

    /**
     * Get masked SSN for display purposes
     * Shows only last 4 digits: ***-**-1234
     * 
     * @return Masked SSN string
     */
    public String getMaskedSsn() {
        if (ssn != null && ssn.length() == 9) {
            return "***-**-" + ssn.substring(5);
        }
        return "***-**-****";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Customer customer = (Customer) o;
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
                ", primaryCardHolderIndicator='" + primaryCardHolderIndicator + '\'' +
                '}';
    }
}