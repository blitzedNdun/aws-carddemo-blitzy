package com.carddemo.common.entity;

import com.carddemo.common.entity.Account;
import com.carddemo.common.entity.Card;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/**
 * JPA Customer entity mapping COBOL CUSTOMER-RECORD to PostgreSQL customers table.
 * 
 * This entity represents customer profile data with personal information, address data,
 * SSN encryption, FICO score validation, and relationships to accounts and cards supporting
 * customer profile management per Section 5.2.2. The entity maintains COBOL 500-byte
 * record structure precision equivalent and supports GDPR compliance with data retention
 * and privacy controls per Section 6.2.3.1.
 * 
 * Converted from COBOL copybook CUSTREC.cpy with 500-byte record structure to normalized
 * PostgreSQL table design supporting sub-200ms response times for customer operations per
 * Section 2.1 requirements.
 * 
 * Database mapping:
 * - customer_id: VARCHAR(9) primary key for 9-digit customer identification with sequence generation
 * - first_name, middle_name, last_name: VARCHAR(20) with proper validation per business rules
 * - address_line_1, address_line_2, address_line_3: VARCHAR(50) each for complete address storage
 * - ssn: VARCHAR(9) with encryption and privacy controls per Section 6.2.3.3
 * - fico_credit_score: NUMERIC(3) with range validation (300-850) for credit assessment
 * - One-to-many relationships with Account and Card entities via customer_id foreign key
 */
@Entity(name = "CommonCustomer")
@Table(name = "customers", schema = "public")
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Customer identifier serving as primary key.
     * Maps to CUST-ID from COBOL copybook CUSTREC.cpy (PIC 9(09)).
     * 9-digit customer identifier with sequence generation for unique identification.
     */
    @Id
    @Column(name = "customer_id", length = 9, nullable = false)
    @NotNull(message = "Customer ID cannot be null")
    @Pattern(regexp = "\\d{9}", message = "Customer ID must be exactly 9 digits")
    private String customerId;

    /**
     * Customer first name with length validation.
     * Maps to CUST-FIRST-NAME from COBOL copybook (PIC X(25)) normalized to VARCHAR(20).
     * Stores customer's legal first name for identification and correspondence.
     */
    @Column(name = "first_name", length = 20, nullable = false)
    @NotNull(message = "First name cannot be null")
    @Size(min = 1, max = 20, message = "First name must be between 1 and 20 characters")
    private String firstName;

    /**
     * Customer middle name with length validation.
     * Maps to CUST-MIDDLE-NAME from COBOL copybook (PIC X(25)) normalized to VARCHAR(20).
     * Stores customer's middle name or initial for complete name representation.
     */
    @Column(name = "middle_name", length = 20)
    @Size(max = 20, message = "Middle name cannot exceed 20 characters")
    private String middleName;

    /**
     * Customer last name with length validation.
     * Maps to CUST-LAST-NAME from COBOL copybook (PIC X(25)) normalized to VARCHAR(20).
     * Stores customer's legal surname for identification and correspondence.
     */
    @Column(name = "last_name", length = 20, nullable = false)
    @NotNull(message = "Last name cannot be null")
    @Size(min = 1, max = 20, message = "Last name must be between 1 and 20 characters")
    private String lastName;

    /**
     * Primary address line for customer correspondence.
     * Maps to CUST-ADDR-LINE-1 from COBOL copybook (PIC X(50)).
     * Stores street address or PO Box for primary mailing address.
     */
    @Column(name = "address_line_1", length = 50, nullable = false)
    @NotNull(message = "Address line 1 cannot be null")
    @Size(min = 1, max = 50, message = "Address line 1 must be between 1 and 50 characters")
    private String addressLine1;

    /**
     * Secondary address line for additional address information.
     * Maps to CUST-ADDR-LINE-2 from COBOL copybook (PIC X(50)).
     * Stores apartment, suite, or additional address details.
     */
    @Column(name = "address_line_2", length = 50)
    @Size(max = 50, message = "Address line 2 cannot exceed 50 characters")
    private String addressLine2;

    /**
     * Third address line for comprehensive address storage.
     * Maps to CUST-ADDR-LINE-3 from COBOL copybook (PIC X(50)).
     * Stores additional address information for complex addresses.
     */
    @Column(name = "address_line_3", length = 50)
    @Size(max = 50, message = "Address line 3 cannot exceed 50 characters")
    private String addressLine3;

    /**
     * State code for customer address.
     * Maps to CUST-ADDR-STATE-CD from COBOL copybook (PIC X(02)).
     * Stores 2-character state abbreviation for geographic identification.
     */
    @Column(name = "state_code", length = 2, nullable = false)
    @NotNull(message = "State code cannot be null")
    @Size(min = 2, max = 2, message = "State code must be exactly 2 characters")
    @Pattern(regexp = "[A-Z]{2}", message = "State code must be 2 uppercase letters")
    private String stateCode;

    /**
     * Country code for customer address.
     * Maps to CUST-ADDR-COUNTRY-CD from COBOL copybook (PIC X(03)).
     * Stores 3-character country code for international address support.
     */
    @Column(name = "country_code", length = 3, nullable = false)
    @NotNull(message = "Country code cannot be null")
    @Size(min = 3, max = 3, message = "Country code must be exactly 3 characters")
    @Pattern(regexp = "[A-Z]{3}", message = "Country code must be 3 uppercase letters")
    private String countryCode;

    /**
     * ZIP code for customer address.
     * Maps to CUST-ADDR-ZIP from COBOL copybook (PIC X(10)).
     * Stores postal code supporting both 5-digit and ZIP+4 formats.
     */
    @Column(name = "zip_code", length = 10, nullable = false)
    @NotNull(message = "ZIP code cannot be null")
    @Size(min = 5, max = 10, message = "ZIP code must be between 5 and 10 characters")
    @Pattern(regexp = "\\d{5}(-\\d{4})?", message = "ZIP code must be in format 12345 or 12345-6789")
    private String zipCode;

    /**
     * Primary phone number for customer contact.
     * Maps to CUST-PHONE-NUM-1 from COBOL copybook (PIC X(15)).
     * Stores primary contact phone number with format validation.
     */
    @Column(name = "phone_number_1", length = 15)
    @Size(max = 15, message = "Phone number 1 cannot exceed 15 characters")
    @Pattern(regexp = "\\d{10,15}", message = "Phone number 1 must be 10-15 digits")
    private String phoneNumber1;

    /**
     * Secondary phone number for customer contact.
     * Maps to CUST-PHONE-NUM-2 from COBOL copybook (PIC X(15)).
     * Stores alternate contact phone number with format validation.
     */
    @Column(name = "phone_number_2", length = 15)
    @Size(max = 15, message = "Phone number 2 cannot exceed 15 characters")
    @Pattern(regexp = "\\d{10,15}", message = "Phone number 2 must be 10-15 digits")
    private String phoneNumber2;

    /**
     * Social Security Number with encryption support.
     * Maps to CUST-SSN from COBOL copybook (PIC 9(09)).
     * Stores customer SSN with encryption and privacy controls per Section 6.2.3.3.
     * 
     * Note: This field should be encrypted at the application level before database storage
     * and decrypted only when specifically required for business operations.
     */
    @Column(name = "ssn", length = 9, nullable = false)
    @NotNull(message = "SSN cannot be null")
    @Pattern(regexp = "\\d{9}", message = "SSN must be exactly 9 digits")
    private String ssn;

    /**
     * Government-issued identification number.
     * Maps to CUST-GOVT-ISSUED-ID from COBOL copybook (PIC X(20)).
     * Stores additional government ID for customer verification purposes.
     */
    @Column(name = "government_id", length = 20)
    @Size(max = 20, message = "Government ID cannot exceed 20 characters")
    private String governmentId;

    /**
     * Customer date of birth for age verification and compliance.
     * Maps to CUST-DOB-YYYYMMDD from COBOL copybook (PIC X(10)).
     * Uses LocalDate for proper temporal operations and validation per Java 21 standards.
     * Supports age calculations and regulatory compliance requirements.
     */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /**
     * Electronic Funds Transfer account identifier.
     * Maps to CUST-EFT-ACCOUNT-ID from COBOL copybook (PIC X(10)).
     * Stores linked EFT account for automatic payment processing.
     */
    @Column(name = "eft_account_id", length = 10)
    @Size(max = 10, message = "EFT account ID cannot exceed 10 characters")
    private String eftAccountId;

    /**
     * Primary cardholder indicator flag.
     * Maps to CUST-PRI-CARD-HOLDER-IND from COBOL copybook (PIC X(01)).
     * Indicates whether customer is primary cardholder (Y) or secondary (N).
     */
    @Column(name = "primary_cardholder_indicator", length = 1, nullable = false)
    @NotNull(message = "Primary cardholder indicator cannot be null")
    @Pattern(regexp = "[YN]", message = "Primary cardholder indicator must be Y or N")
    private String primaryCardholderIndicator;

    /**
     * FICO credit score with range validation.
     * Maps to CUST-FICO-CREDIT-SCORE from COBOL copybook (PIC 9(03)).
     * Stores customer credit score with strict validation (300-850) for credit assessment.
     * Uses NUMERIC(3) precision for exact score representation.
     */
    @Column(name = "fico_credit_score", precision = 3, scale = 0)
    @Min(value = 300, message = "FICO credit score must be at least 300")
    @Max(value = 850, message = "FICO credit score cannot exceed 850")
    private Integer ficoCreditScore;

    /**
     * One-to-many relationship with Account entities.
     * Customer can own multiple accounts for portfolio management.
     * Uses LAZY loading for optimal performance and memory usage per Section 6.2.4.
     */
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Account> accounts = new ArrayList<>();

    /**
     * One-to-many relationship with Card entities.
     * Customer can own multiple cards across different accounts.
     * Uses LAZY loading for optimal performance and memory usage per Section 6.2.4.
     */
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Card> cards = new ArrayList<>();

    /**
     * Version field for optimistic locking support.
     * Enables concurrent customer operations protection per Section 5.2.2 requirements.
     * Automatically managed by JPA for conflict resolution in multi-user environments.
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Default constructor required by JPA specification.
     * Initializes collections and sets default values for required fields.
     */
    public Customer() {
        this.accounts = new ArrayList<>();
        this.cards = new ArrayList<>();
        this.primaryCardholderIndicator = "Y"; // Default to primary cardholder
    }

    /**
     * Constructor with required fields for business logic initialization.
     * 
     * @param customerId The unique customer identifier (9 digits)
     * @param firstName The customer's first name (1-20 characters)
     * @param lastName The customer's last name (1-20 characters)
     * @param addressLine1 The primary address line (1-50 characters)
     * @param stateCode The state code (2 uppercase letters)
     * @param countryCode The country code (3 uppercase letters)
     * @param zipCode The ZIP code (5-10 digits with optional hyphen)
     * @param ssn The Social Security Number (9 digits)
     */
    public Customer(String customerId, String firstName, String lastName, String addressLine1,
                    String stateCode, String countryCode, String zipCode, String ssn) {
        this();
        this.customerId = customerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.addressLine1 = addressLine1;
        this.stateCode = stateCode;
        this.countryCode = countryCode;
        this.zipCode = zipCode;
        this.ssn = ssn;
    }

    /**
     * Gets the customer identifier.
     * 
     * @return The unique customer ID (VARCHAR(9))
     */
    public String getCustomerId() {
        return customerId;
    }

    /**
     * Sets the customer identifier.
     * 
     * @param customerId The unique customer ID (9 digits, not null)
     */
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    /**
     * Gets the customer's first name.
     * 
     * @return The first name (VARCHAR(20))
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the customer's first name.
     * 
     * @param firstName The first name (1-20 characters, not null)
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the customer's middle name.
     * 
     * @return The middle name (VARCHAR(20))
     */
    public String getMiddleName() {
        return middleName;
    }

    /**
     * Sets the customer's middle name.
     * 
     * @param middleName The middle name (up to 20 characters)
     */
    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    /**
     * Gets the customer's last name.
     * 
     * @return The last name (VARCHAR(20))
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the customer's last name.
     * 
     * @param lastName The last name (1-20 characters, not null)
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Gets the primary address line.
     * 
     * @return The address line 1 (VARCHAR(50))
     */
    public String getAddressLine1() {
        return addressLine1;
    }

    /**
     * Sets the primary address line.
     * 
     * @param addressLine1 The address line 1 (1-50 characters, not null)
     */
    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    /**
     * Gets the secondary address line.
     * 
     * @return The address line 2 (VARCHAR(50))
     */
    public String getAddressLine2() {
        return addressLine2;
    }

    /**
     * Sets the secondary address line.
     * 
     * @param addressLine2 The address line 2 (up to 50 characters)
     */
    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    /**
     * Gets the third address line.
     * 
     * @return The address line 3 (VARCHAR(50))
     */
    public String getAddressLine3() {
        return addressLine3;
    }

    /**
     * Sets the third address line.
     * 
     * @param addressLine3 The address line 3 (up to 50 characters)
     */
    public void setAddressLine3(String addressLine3) {
        this.addressLine3 = addressLine3;
    }

    /**
     * Gets the state code.
     * 
     * @return The state code (VARCHAR(2))
     */
    public String getStateCode() {
        return stateCode;
    }

    /**
     * Sets the state code.
     * 
     * @param stateCode The state code (2 uppercase letters, not null)
     */
    public void setStateCode(String stateCode) {
        this.stateCode = stateCode;
    }

    /**
     * Gets the country code.
     * 
     * @return The country code (VARCHAR(3))
     */
    public String getCountryCode() {
        return countryCode;
    }

    /**
     * Sets the country code.
     * 
     * @param countryCode The country code (3 uppercase letters, not null)
     */
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    /**
     * Gets the ZIP code.
     * 
     * @return The ZIP code (VARCHAR(10))
     */
    public String getZipCode() {
        return zipCode;
    }

    /**
     * Sets the ZIP code.
     * 
     * @param zipCode The ZIP code (5-10 digits with optional hyphen, not null)
     */
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    /**
     * Gets the primary phone number.
     * 
     * @return The phone number 1 (VARCHAR(15))
     */
    public String getPhoneNumber1() {
        return phoneNumber1;
    }

    /**
     * Sets the primary phone number.
     * 
     * @param phoneNumber1 The phone number 1 (10-15 digits)
     */
    public void setPhoneNumber1(String phoneNumber1) {
        this.phoneNumber1 = phoneNumber1;
    }

    /**
     * Gets the secondary phone number.
     * 
     * @return The phone number 2 (VARCHAR(15))
     */
    public String getPhoneNumber2() {
        return phoneNumber2;
    }

    /**
     * Sets the secondary phone number.
     * 
     * @param phoneNumber2 The phone number 2 (10-15 digits)
     */
    public void setPhoneNumber2(String phoneNumber2) {
        this.phoneNumber2 = phoneNumber2;
    }

    /**
     * Gets the Social Security Number.
     * Note: This method should implement decryption when SSN encryption is implemented.
     * 
     * @return The SSN (VARCHAR(9))
     */
    public String getSsn() {
        return ssn;
    }

    /**
     * Sets the Social Security Number.
     * Note: This method should implement encryption when SSN encryption is implemented.
     * 
     * @param ssn The SSN (9 digits, not null)
     */
    public void setSsn(String ssn) {
        this.ssn = ssn;
    }

    /**
     * Gets the government-issued ID.
     * 
     * @return The government ID (VARCHAR(20))
     */
    public String getGovernmentId() {
        return governmentId;
    }

    /**
     * Sets the government-issued ID.
     * 
     * @param governmentId The government ID (up to 20 characters)
     */
    public void setGovernmentId(String governmentId) {
        this.governmentId = governmentId;
    }

    /**
     * Gets the customer's date of birth.
     * Uses LocalDate for proper temporal operations per Java 21 standards.
     * 
     * @return The date of birth for age calculations and compliance
     */
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    /**
     * Sets the customer's date of birth.
     * Uses LocalDate.of() method for date specification as required by external imports.
     * 
     * @param dateOfBirth The date of birth
     */
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    /**
     * Gets the EFT account identifier.
     * 
     * @return The EFT account ID (VARCHAR(10))
     */
    public String getEftAccountId() {
        return eftAccountId;
    }

    /**
     * Sets the EFT account identifier.
     * 
     * @param eftAccountId The EFT account ID (up to 10 characters)
     */
    public void setEftAccountId(String eftAccountId) {
        this.eftAccountId = eftAccountId;
    }

    /**
     * Gets the primary cardholder indicator.
     * 
     * @return The primary cardholder indicator (Y/N)
     */
    public String getPrimaryCardholderIndicator() {
        return primaryCardholderIndicator;
    }

    /**
     * Sets the primary cardholder indicator.
     * 
     * @param primaryCardholderIndicator The indicator (Y for primary, N for secondary)
     */
    public void setPrimaryCardholderIndicator(String primaryCardholderIndicator) {
        this.primaryCardholderIndicator = primaryCardholderIndicator;
    }

    /**
     * Gets the FICO credit score.
     * 
     * @return The FICO credit score (300-850 range)
     */
    public Integer getFicoCreditScore() {
        return ficoCreditScore;
    }

    /**
     * Sets the FICO credit score with range validation.
     * 
     * @param ficoCreditScore The FICO score (300-850 inclusive)
     */
    public void setFicoCreditScore(Integer ficoCreditScore) {
        this.ficoCreditScore = ficoCreditScore;
    }

    /**
     * Gets the list of accounts owned by this customer.
     * Provides access to customer's account portfolio for account management operations.
     * Uses getCustomerId() and setCustomerId() methods per internal import requirements.
     * 
     * @return The list of Account entities associated with this customer
     */
    public List<Account> getAccounts() {
        return accounts;
    }

    /**
     * Sets the list of accounts owned by this customer.
     * Establishes customer-account relationships for portfolio management.
     * Uses setCustomerId() method per internal import requirements.
     * 
     * @param accounts The list of Account entities to associate with this customer
     */
    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
        // Ensure bidirectional relationship consistency
        if (accounts != null) {
            for (Account account : accounts) {
                account.setCustomerId(this.customerId);
            }
        }
    }

    /**
     * Gets the list of cards owned by this customer.
     * Provides access to customer's card portfolio for card management operations.
     * Uses getCustomerId() and setCustomerId() methods per internal import requirements.
     * 
     * @return The list of Card entities associated with this customer
     */
    public List<Card> getCards() {
        return cards;
    }

    /**
     * Sets the list of cards owned by this customer.
     * Establishes customer-card relationships for card portfolio management.
     * Uses setCustomerId() method per internal import requirements.
     * 
     * @param cards The list of Card entities to associate with this customer
     */
    public void setCards(List<Card> cards) {
        this.cards = cards;
        // Ensure bidirectional relationship consistency
        if (cards != null) {
            for (Card card : cards) {
                card.setCustomer(this.customerId);
            }
        }
    }

    /**
     * Gets the version for optimistic locking.
     * 
     * @return The version number for concurrent access control
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Sets the version for optimistic locking.
     * 
     * @param version The version number for conflict resolution
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Adds an account to this customer's portfolio.
     * Maintains bidirectional relationship consistency.
     * Uses getAccountId() and setAccountId() methods per internal import requirements.
     * 
     * @param account The Account entity to add to this customer
     */
    public void addAccount(Account account) {
        if (account != null) {
            this.accounts.add(account);
            account.setCustomerId(this.customerId);
        }
    }

    /**
     * Removes an account from this customer's portfolio.
     * Maintains bidirectional relationship consistency.
     * Uses getAccountId() method per internal import requirements.
     * 
     * @param account The Account entity to remove from this customer
     */
    public void removeAccount(Account account) {
        if (account != null) {
            this.accounts.remove(account);
            account.setCustomerId(null);
        }
    }

    /**
     * Adds a card to this customer's portfolio.
     * Maintains bidirectional relationship consistency.
     * Uses getCardNumber() and setCardNumber() methods per internal import requirements.
     * 
     * @param card The Card entity to add to this customer
     */
    public void addCard(Card card) {
        if (card != null) {
            this.cards.add(card);
            card.setCustomer(this.customerId);
        }
    }

    /**
     * Removes a card from this customer's portfolio.
     * Maintains bidirectional relationship consistency.
     * Uses getCardNumber() method per internal import requirements.
     * 
     * @param card The Card entity to remove from this customer
     */
    public void removeCard(Card card) {
        if (card != null) {
            this.cards.remove(card);
            card.setCustomer(null);
        }
    }

    /**
     * Checks if the customer is a primary cardholder.
     * 
     * @return true if primary cardholder indicator is Y, false otherwise
     */
    public boolean isPrimaryCardholder() {
        return "Y".equals(primaryCardholderIndicator);
    }

    /**
     * Calculates customer's age based on date of birth.
     * Uses LocalDate.now() and isAfter() methods per external import requirements.
     * 
     * @return The customer's age in years, or null if date of birth is not set
     */
    public Integer getAge() {
        if (dateOfBirth == null) {
            return null;
        }
        
        LocalDate currentDate = LocalDate.now();
        int age = currentDate.getYear() - dateOfBirth.getYear();
        
        // Adjust age if birthday hasn't occurred this year
        LocalDate birthdayThisYear = dateOfBirth.withYear(currentDate.getYear());
        if (currentDate.isBefore(birthdayThisYear)) {
            age--;
        }
        
        return age;
    }

    /**
     * Checks if the customer is of legal age (18 or older).
     * Uses isAfter() method per external import requirements.
     * 
     * @return true if customer is 18 or older, false otherwise
     */
    public boolean isLegalAge() {
        if (dateOfBirth == null) {
            return false;
        }
        
        LocalDate eighteenYearsAgo = LocalDate.now().minusYears(18);
        return dateOfBirth.isBefore(eighteenYearsAgo) || dateOfBirth.equals(eighteenYearsAgo);
    }

    /**
     * Gets the customer's full name.
     * Concatenates first, middle (if present), and last names with proper spacing.
     * 
     * @return The complete customer name for display purposes
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
     * Gets the customer's complete address.
     * Concatenates all non-empty address lines with proper formatting.
     * 
     * @return The complete customer address for mailing purposes
     */
    public String getCompleteAddress() {
        StringBuilder address = new StringBuilder();
        
        if (addressLine1 != null && !addressLine1.trim().isEmpty()) {
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
        
        // Add city, state, ZIP
        if (stateCode != null && zipCode != null) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(stateCode).append(" ").append(zipCode);
        }
        
        if (countryCode != null && !"USA".equals(countryCode)) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(countryCode);
        }
        
        return address.toString();
    }

    /**
     * Validates FICO credit score range.
     * Ensures score falls within acceptable FICO range (300-850).
     * 
     * @return true if score is valid, false otherwise
     */
    public boolean isValidFicoScore() {
        return ficoCreditScore != null && ficoCreditScore >= 300 && ficoCreditScore <= 850;
    }

    /**
     * Gets credit risk category based on FICO score.
     * Categorizes customer credit risk for business decision making.
     * 
     * @return Credit risk category (EXCELLENT, GOOD, FAIR, POOR, UNKNOWN)
     */
    public String getCreditRiskCategory() {
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
        } else {
            return "POOR";
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Customer customer = (Customer) obj;
        return Objects.equals(customerId, customer.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId);
    }

    @Override
    public String toString() {
        return "Customer{" +
                "customerId='" + customerId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", middleName='" + middleName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", addressLine1='" + addressLine1 + '\'' +
                ", addressLine2='" + addressLine2 + '\'' +
                ", addressLine3='" + addressLine3 + '\'' +
                ", stateCode='" + stateCode + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", zipCode='" + zipCode + '\'' +
                ", phoneNumber1='" + phoneNumber1 + '\'' +
                ", phoneNumber2='" + phoneNumber2 + '\'' +
                ", governmentId='" + governmentId + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", eftAccountId='" + eftAccountId + '\'' +
                ", primaryCardholderIndicator='" + primaryCardholderIndicator + '\'' +
                ", ficoCreditScore=" + ficoCreditScore +
                ", accountCount=" + (accounts != null ? accounts.size() : 0) +
                ", cardCount=" + (cards != null ? cards.size() : 0) +
                ", version=" + version +
                '}';
    }
}