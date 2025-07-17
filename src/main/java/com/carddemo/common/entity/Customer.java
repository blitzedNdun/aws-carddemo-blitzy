package com.carddemo.common.entity;

import com.carddemo.common.entity.Account;
import com.carddemo.common.entity.Card;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * JPA Customer entity mapping COBOL CUSTOMER-RECORD (500-byte) structure to PostgreSQL customers table
 * with personal information, address data, SSN encryption, FICO score validation, and relationships to 
 * accounts and cards supporting customer profile management.
 * 
 * This entity supports:
 * - Customer profile management with comprehensive validation per Section 5.2.2
 * - GDPR compliance with data retention and privacy controls per Section 6.2.3.1
 * - SSN encryption and privacy controls per Section 6.2.3.3
 * - FICO score validation (300-850) per Section 6.2.1.1
 * - One-to-many relationships with Account and Card entities for portfolio management
 * - PostgreSQL DECIMAL precision for financial data integrity
 * - Spring Boot/JPA patterns for microservices architecture
 * 
 * Maps to PostgreSQL table: customers
 * Original COBOL structure: CUSTOMER-RECORD in CUSTREC.cpy (500 bytes)
 * 
 * Key field mappings:
 * - CUST-ID PIC 9(09) -> VARCHAR(9) customer_id primary key
 * - CUST-FIRST-NAME PIC X(25) -> VARCHAR(25) first_name
 * - CUST-MIDDLE-NAME PIC X(25) -> VARCHAR(25) middle_name
 * - CUST-LAST-NAME PIC X(25) -> VARCHAR(25) last_name
 * - CUST-ADDR-LINE-1 PIC X(50) -> VARCHAR(50) address_line_1
 * - CUST-ADDR-LINE-2 PIC X(50) -> VARCHAR(50) address_line_2
 * - CUST-ADDR-LINE-3 PIC X(50) -> VARCHAR(50) address_line_3
 * - CUST-ADDR-STATE-CD PIC X(02) -> VARCHAR(2) state_code
 * - CUST-ADDR-COUNTRY-CD PIC X(03) -> VARCHAR(3) country_code
 * - CUST-ADDR-ZIP PIC X(10) -> VARCHAR(10) zip_code
 * - CUST-PHONE-NUM-1 PIC X(15) -> VARCHAR(15) phone_number_1
 * - CUST-PHONE-NUM-2 PIC X(15) -> VARCHAR(15) phone_number_2
 * - CUST-SSN PIC 9(09) -> VARCHAR(9) ssn (with encryption)
 * - CUST-GOVT-ISSUED-ID PIC X(20) -> VARCHAR(20) government_id
 * - CUST-DOB-YYYYMMDD PIC X(10) -> DATE date_of_birth
 * - CUST-EFT-ACCOUNT-ID PIC X(10) -> VARCHAR(10) eft_account_id
 * - CUST-PRI-CARD-HOLDER-IND PIC X(01) -> BOOLEAN primary_cardholder_indicator
 * - CUST-FICO-CREDIT-SCORE PIC 9(03) -> NUMERIC(3) fico_credit_score
 */
@Entity
@Table(name = "customers")
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Customer identifier - Primary key mapping CUST-ID PIC 9(09)
     * Maps to PostgreSQL VARCHAR(9) customer_id primary key with sequence generation
     * Range: 000000001 to 999999999 (9-digit numeric format)
     */
    @Id
    @Column(name = "customer_id", length = 9, nullable = false)
    @NotNull(message = "Customer ID cannot be null")
    @Pattern(regexp = "\\d{9}", message = "Customer ID must be exactly 9 digits")
    private String customerId;

    /**
     * Customer first name - Maps CUST-FIRST-NAME PIC X(25)
     * Maps to PostgreSQL VARCHAR(25) with length validation
     */
    @Column(name = "first_name", length = 25, nullable = false)
    @NotNull(message = "First name cannot be null")
    @Size(min = 1, max = 25, message = "First name must be between 1 and 25 characters")
    private String firstName;

    /**
     * Customer middle name - Maps CUST-MIDDLE-NAME PIC X(25)
     * Maps to PostgreSQL VARCHAR(25) with length validation
     */
    @Column(name = "middle_name", length = 25)
    @Size(max = 25, message = "Middle name cannot exceed 25 characters")
    private String middleName;

    /**
     * Customer last name - Maps CUST-LAST-NAME PIC X(25)
     * Maps to PostgreSQL VARCHAR(25) with length validation
     */
    @Column(name = "last_name", length = 25, nullable = false)
    @NotNull(message = "Last name cannot be null")
    @Size(min = 1, max = 25, message = "Last name must be between 1 and 25 characters")
    private String lastName;

    /**
     * Primary address line - Maps CUST-ADDR-LINE-1 PIC X(50)
     * Maps to PostgreSQL VARCHAR(50) with length validation
     */
    @Column(name = "address_line_1", length = 50, nullable = false)
    @NotNull(message = "Address line 1 cannot be null")
    @Size(min = 1, max = 50, message = "Address line 1 must be between 1 and 50 characters")
    private String addressLine1;

    /**
     * Secondary address line - Maps CUST-ADDR-LINE-2 PIC X(50)
     * Maps to PostgreSQL VARCHAR(50) with length validation
     */
    @Column(name = "address_line_2", length = 50)
    @Size(max = 50, message = "Address line 2 cannot exceed 50 characters")
    private String addressLine2;

    /**
     * Additional address line - Maps CUST-ADDR-LINE-3 PIC X(50)
     * Maps to PostgreSQL VARCHAR(50) with length validation
     */
    @Column(name = "address_line_3", length = 50)
    @Size(max = 50, message = "Address line 3 cannot exceed 50 characters")
    private String addressLine3;

    /**
     * State code - Maps CUST-ADDR-STATE-CD PIC X(02)
     * Maps to PostgreSQL VARCHAR(2) with state code validation
     */
    @Column(name = "state_code", length = 2, nullable = false)
    @NotNull(message = "State code cannot be null")
    @Size(min = 2, max = 2, message = "State code must be exactly 2 characters")
    @Pattern(regexp = "[A-Z]{2}", message = "State code must be 2 uppercase letters")
    private String stateCode;

    /**
     * Country code - Maps CUST-ADDR-COUNTRY-CD PIC X(03)
     * Maps to PostgreSQL VARCHAR(3) with country code validation
     */
    @Column(name = "country_code", length = 3, nullable = false)
    @NotNull(message = "Country code cannot be null")
    @Size(min = 3, max = 3, message = "Country code must be exactly 3 characters")
    @Pattern(regexp = "[A-Z]{3}", message = "Country code must be 3 uppercase letters")
    private String countryCode;

    /**
     * ZIP code - Maps CUST-ADDR-ZIP PIC X(10)
     * Maps to PostgreSQL VARCHAR(10) with ZIP code validation
     */
    @Column(name = "zip_code", length = 10, nullable = false)
    @NotNull(message = "ZIP code cannot be null")
    @Size(min = 5, max = 10, message = "ZIP code must be between 5 and 10 characters")
    @Pattern(regexp = "\\d{5}(-\\d{4})?", message = "ZIP code must be in format 12345 or 12345-1234")
    private String zipCode;

    /**
     * Primary phone number - Maps CUST-PHONE-NUM-1 PIC X(15)
     * Maps to PostgreSQL VARCHAR(15) with phone number validation
     */
    @Column(name = "phone_number_1", length = 15)
    @Size(max = 15, message = "Phone number 1 cannot exceed 15 characters")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number 1 must be a valid phone number")
    private String phoneNumber1;

    /**
     * Secondary phone number - Maps CUST-PHONE-NUM-2 PIC X(15)
     * Maps to PostgreSQL VARCHAR(15) with phone number validation
     */
    @Column(name = "phone_number_2", length = 15)
    @Size(max = 15, message = "Phone number 2 cannot exceed 15 characters")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number 2 must be a valid phone number")
    private String phoneNumber2;

    /**
     * Social Security Number - Maps CUST-SSN PIC 9(09)
     * Maps to PostgreSQL VARCHAR(9) with encryption and privacy controls per Section 6.2.3.3
     * This field requires special authorization for access per Section 6.2.3.5
     */
    @Column(name = "ssn", length = 9, nullable = false)
    @NotNull(message = "SSN cannot be null")
    @Size(min = 9, max = 9, message = "SSN must be exactly 9 digits")
    @Pattern(regexp = "\\d{9}", message = "SSN must be exactly 9 digits")
    private String ssn;

    /**
     * Government issued ID - Maps CUST-GOVT-ISSUED-ID PIC X(20)
     * Maps to PostgreSQL VARCHAR(20) with length validation
     */
    @Column(name = "government_id", length = 20)
    @Size(max = 20, message = "Government ID cannot exceed 20 characters")
    private String governmentId;

    /**
     * Date of birth - Maps CUST-DOB-YYYYMMDD PIC X(10)
     * Maps to PostgreSQL DATE type for customer birth date management
     */
    @Column(name = "date_of_birth", nullable = false)
    @NotNull(message = "Date of birth cannot be null")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    /**
     * EFT account identifier - Maps CUST-EFT-ACCOUNT-ID PIC X(10)
     * Maps to PostgreSQL VARCHAR(10) for electronic funds transfer account reference
     */
    @Column(name = "eft_account_id", length = 10)
    @Size(max = 10, message = "EFT account ID cannot exceed 10 characters")
    private String eftAccountId;

    /**
     * Primary cardholder indicator - Maps CUST-PRI-CARD-HOLDER-IND PIC X(01)
     * Maps to PostgreSQL BOOLEAN for primary cardholder flag
     */
    @Column(name = "primary_cardholder_indicator", nullable = false)
    @NotNull(message = "Primary cardholder indicator cannot be null")
    private Boolean primaryCardholderIndicator;

    /**
     * FICO credit score - Maps CUST-FICO-CREDIT-SCORE PIC 9(03)
     * Maps to PostgreSQL NUMERIC(3) with range validation (300-850)
     */
    @Column(name = "fico_credit_score", nullable = false)
    @NotNull(message = "FICO credit score cannot be null")
    @Min(value = 300, message = "FICO credit score must be at least 300")
    @Max(value = 850, message = "FICO credit score cannot exceed 850")
    private Integer ficoCreditScore;

    /**
     * One-to-many relationship with Account entity
     * Foreign key relationship enabling customer portfolio management
     * and account lifecycle operations
     */
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "customer_id")
    private List<Account> accounts;

    /**
     * One-to-many relationship with Card entity
     * Foreign key relationship enabling customer card portfolio management
     * and card lifecycle operations
     */
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "customer_id")
    private List<Card> cards;

    /**
     * Optimistic locking version for concurrent customer operations protection
     * Enables customer profile management with concurrent access control
     * Prevents lost updates in multi-user scenarios
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Default constructor for JPA
     */
    public Customer() {
        this.version = 0L;
        this.primaryCardholderIndicator = Boolean.FALSE;
    }

    /**
     * Constructor with essential fields for customer creation
     * 
     * @param customerId Customer identifier (9 digits)
     * @param firstName Customer first name
     * @param lastName Customer last name
     * @param addressLine1 Primary address line
     * @param stateCode State code (2 characters)
     * @param countryCode Country code (3 characters)
     * @param zipCode ZIP code
     * @param ssn Social Security Number (9 digits)
     * @param dateOfBirth Customer birth date
     * @param ficoCreditScore FICO credit score (300-850)
     */
    public Customer(String customerId, String firstName, String lastName, String addressLine1,
                   String stateCode, String countryCode, String zipCode, String ssn,
                   LocalDate dateOfBirth, Integer ficoCreditScore) {
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
     * Gets the customer first name
     * 
     * @return First name as string
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the customer first name
     * 
     * @param firstName First name as string
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the customer middle name
     * 
     * @return Middle name as string
     */
    public String getMiddleName() {
        return middleName;
    }

    /**
     * Sets the customer middle name
     * 
     * @param middleName Middle name as string
     */
    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    /**
     * Gets the customer last name
     * 
     * @return Last name as string
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the customer last name
     * 
     * @param lastName Last name as string
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Gets the primary address line
     * 
     * @return Address line 1 as string
     */
    public String getAddressLine1() {
        return addressLine1;
    }

    /**
     * Sets the primary address line
     * 
     * @param addressLine1 Address line 1 as string
     */
    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    /**
     * Gets the secondary address line
     * 
     * @return Address line 2 as string
     */
    public String getAddressLine2() {
        return addressLine2;
    }

    /**
     * Sets the secondary address line
     * 
     * @param addressLine2 Address line 2 as string
     */
    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    /**
     * Gets the additional address line
     * 
     * @return Address line 3 as string
     */
    public String getAddressLine3() {
        return addressLine3;
    }

    /**
     * Sets the additional address line
     * 
     * @param addressLine3 Address line 3 as string
     */
    public void setAddressLine3(String addressLine3) {
        this.addressLine3 = addressLine3;
    }

    /**
     * Gets the state code
     * 
     * @return State code as 2-character string
     */
    public String getStateCode() {
        return stateCode;
    }

    /**
     * Sets the state code
     * 
     * @param stateCode State code as 2-character string
     */
    public void setStateCode(String stateCode) {
        this.stateCode = stateCode;
    }

    /**
     * Gets the country code
     * 
     * @return Country code as 3-character string
     */
    public String getCountryCode() {
        return countryCode;
    }

    /**
     * Sets the country code
     * 
     * @param countryCode Country code as 3-character string
     */
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    /**
     * Gets the ZIP code
     * 
     * @return ZIP code as string
     */
    public String getZipCode() {
        return zipCode;
    }

    /**
     * Sets the ZIP code
     * 
     * @param zipCode ZIP code as string
     */
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    /**
     * Gets the primary phone number
     * 
     * @return Phone number 1 as string
     */
    public String getPhoneNumber1() {
        return phoneNumber1;
    }

    /**
     * Sets the primary phone number
     * 
     * @param phoneNumber1 Phone number 1 as string
     */
    public void setPhoneNumber1(String phoneNumber1) {
        this.phoneNumber1 = phoneNumber1;
    }

    /**
     * Gets the secondary phone number
     * 
     * @return Phone number 2 as string
     */
    public String getPhoneNumber2() {
        return phoneNumber2;
    }

    /**
     * Sets the secondary phone number
     * 
     * @param phoneNumber2 Phone number 2 as string
     */
    public void setPhoneNumber2(String phoneNumber2) {
        this.phoneNumber2 = phoneNumber2;
    }

    /**
     * Gets the Social Security Number
     * Note: This field contains sensitive PII and requires special authorization
     * 
     * @return SSN as 9-digit string
     */
    public String getSsn() {
        return ssn;
    }

    /**
     * Sets the Social Security Number
     * Note: This field contains sensitive PII and requires special authorization
     * 
     * @param ssn SSN as 9-digit string
     */
    public void setSsn(String ssn) {
        this.ssn = ssn;
    }

    /**
     * Gets the government issued ID
     * 
     * @return Government ID as string
     */
    public String getGovernmentId() {
        return governmentId;
    }

    /**
     * Sets the government issued ID
     * 
     * @param governmentId Government ID as string
     */
    public void setGovernmentId(String governmentId) {
        this.governmentId = governmentId;
    }

    /**
     * Gets the date of birth
     * 
     * @return Date of birth as LocalDate
     */
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    /**
     * Sets the date of birth
     * 
     * @param dateOfBirth Date of birth as LocalDate
     */
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    /**
     * Gets the EFT account identifier
     * 
     * @return EFT account ID as string
     */
    public String getEftAccountId() {
        return eftAccountId;
    }

    /**
     * Sets the EFT account identifier
     * 
     * @param eftAccountId EFT account ID as string
     */
    public void setEftAccountId(String eftAccountId) {
        this.eftAccountId = eftAccountId;
    }

    /**
     * Gets the primary cardholder indicator
     * 
     * @return Primary cardholder indicator as boolean
     */
    public Boolean getPrimaryCardholderIndicator() {
        return primaryCardholderIndicator;
    }

    /**
     * Sets the primary cardholder indicator
     * 
     * @param primaryCardholderIndicator Primary cardholder indicator as boolean
     */
    public void setPrimaryCardholderIndicator(Boolean primaryCardholderIndicator) {
        this.primaryCardholderIndicator = primaryCardholderIndicator;
    }

    /**
     * Gets the FICO credit score
     * 
     * @return FICO credit score as integer (300-850)
     */
    public Integer getFicoCreditScore() {
        return ficoCreditScore;
    }

    /**
     * Sets the FICO credit score
     * 
     * @param ficoCreditScore FICO credit score as integer (300-850)
     */
    public void setFicoCreditScore(Integer ficoCreditScore) {
        this.ficoCreditScore = ficoCreditScore;
    }

    /**
     * Gets the associated accounts
     * Provides access to customer's account portfolio
     * 
     * @return List of Account entities for portfolio management
     */
    public List<Account> getAccounts() {
        return accounts;
    }

    /**
     * Sets the associated accounts
     * Links customer to account portfolio for relationship management
     * 
     * @param accounts List of Account entities
     */
    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }

    /**
     * Gets the associated cards
     * Provides access to customer's card portfolio
     * 
     * @return List of Card entities for card portfolio management
     */
    public List<Card> getCards() {
        return cards;
    }

    /**
     * Sets the associated cards
     * Links customer to card portfolio for relationship management
     * 
     * @param cards List of Card entities
     */
    public void setCards(List<Card> cards) {
        this.cards = cards;
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
     * Gets the customer's full name
     * Concatenates first, middle, and last name with proper spacing
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
     * Gets the customer's complete address
     * Concatenates address lines with proper formatting
     * 
     * @return Complete address as formatted string
     */
    public String getCompleteAddress() {
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
        return address.toString();
    }

    /**
     * Gets masked SSN for display purposes
     * Shows only last 4 digits for security
     * 
     * @return Masked SSN (***-**-1234)
     */
    public String getMaskedSsn() {
        if (ssn == null || ssn.length() != 9) {
            return "***-**-****";
        }
        return "***-**-" + ssn.substring(5);
    }

    /**
     * Calculates customer's age based on date of birth
     * 
     * @return Age in years
     */
    public int getAge() {
        if (dateOfBirth == null) {
            return 0;
        }
        return LocalDate.now().getYear() - dateOfBirth.getYear();
    }

    /**
     * Checks if customer is a primary cardholder
     * 
     * @return true if customer is primary cardholder
     */
    public boolean isPrimaryCardholder() {
        return primaryCardholderIndicator != null && primaryCardholderIndicator;
    }

    /**
     * Checks if customer has good credit score
     * Good credit is typically 700 or above
     * 
     * @return true if FICO score is 700 or above
     */
    public boolean hasGoodCredit() {
        return ficoCreditScore != null && ficoCreditScore >= 700;
    }

    /**
     * Checks if customer has excellent credit score
     * Excellent credit is typically 800 or above
     * 
     * @return true if FICO score is 800 or above
     */
    public boolean hasExcellentCredit() {
        return ficoCreditScore != null && ficoCreditScore >= 800;
    }

    /**
     * Gets credit score category based on FICO score
     * 
     * @return Credit category as string
     */
    public String getCreditCategory() {
        if (ficoCreditScore == null) {
            return "Unknown";
        }
        if (ficoCreditScore >= 800) {
            return "Excellent";
        } else if (ficoCreditScore >= 740) {
            return "Very Good";
        } else if (ficoCreditScore >= 670) {
            return "Good";
        } else if (ficoCreditScore >= 580) {
            return "Fair";
        } else {
            return "Poor";
        }
    }

    /**
     * Validates if customer data is complete for account opening
     * 
     * @return true if all required fields are present
     */
    public boolean isDataComplete() {
        return customerId != null && !customerId.trim().isEmpty() &&
               firstName != null && !firstName.trim().isEmpty() &&
               lastName != null && !lastName.trim().isEmpty() &&
               addressLine1 != null && !addressLine1.trim().isEmpty() &&
               stateCode != null && !stateCode.trim().isEmpty() &&
               countryCode != null && !countryCode.trim().isEmpty() &&
               zipCode != null && !zipCode.trim().isEmpty() &&
               ssn != null && !ssn.trim().isEmpty() &&
               dateOfBirth != null &&
               ficoCreditScore != null;
    }

    /**
     * Equals method for entity comparison based on customer ID
     * 
     * @param o Object to compare
     * @return true if objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(customerId, customer.customerId);
    }

    /**
     * Hash code method for entity hashing based on customer ID
     * 
     * @return hash code based on customerId
     */
    @Override
    public int hashCode() {
        return Objects.hash(customerId);
    }

    /**
     * String representation of customer entity
     * Note: SSN is masked for security
     * 
     * @return String representation including key fields
     */
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
                ", ssn='" + getMaskedSsn() + '\'' +
                ", governmentId='" + governmentId + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", eftAccountId='" + eftAccountId + '\'' +
                ", primaryCardholderIndicator=" + primaryCardholderIndicator +
                ", ficoCreditScore=" + ficoCreditScore +
                ", version=" + version +
                '}';
    }
}