/*
 * Customer.java
 * 
 * JPA entity class representing customer profile data, mapped to customer_data 
 * PostgreSQL table. Contains personal information, contact details, addresses, 
 * and financial indicators for credit card account holders. Uses proper 
 * validation and data types to preserve COBOL field structure compatibility.
 * 
 * Implements one-to-many relationships with Account and Card entities through 
 * customer_id foreign key, enabling customer-to-account navigation and 
 * comprehensive customer profile management operations.
 * 
 * Based on COBOL copybook: app/cpy/CVCUS01Y.cpy
 * - CUST-ID (PIC 9(09)) for customer identification
 * - CUST-FIRST-NAME (PIC X(20)) for first name
 * - CUST-MIDDLE-NAME (PIC X(20)) for middle name
 * - CUST-LAST-NAME (PIC X(20)) for last name
 * - CUST-ADDR-LINE-1 (PIC X(50)) for address line 1
 * - CUST-ADDR-LINE-2 (PIC X(50)) for address line 2
 * - CUST-ADDR-LINE-3 (PIC X(50)) for address line 3
 * - CUST-ADDR-STATE-CD (PIC X(02)) for state code
 * - CUST-ADDR-COUNTRY-CD (PIC X(03)) for country code
 * - CUST-ADDR-ZIP (PIC X(10)) for ZIP code
 * - CUST-PHONE-NUM-1 (PIC X(15)) for primary phone
 * - CUST-PHONE-NUM-2 (PIC X(15)) for secondary phone
 * - CUST-SSN (PIC X(09)) for social security number
 * - CUST-GOVT-ISSUED-ID (PIC X(20)) for government ID
 * - CUST-DOB-YYYYMMDD (PIC X(08)) for date of birth
 * - CUST-EFT-ACCOUNT-ID (PIC X(10)) for EFT account
 * - CUST-PRI-CARD-HOLDER-IND (PIC X(01)) for primary cardholder indicator
 * - CUST-FICO-CREDIT-SCORE (PIC 9(03)) for FICO score
 */

package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * JPA entity class representing customer profile data.
 * 
 * Maps to the customer_data PostgreSQL table, providing comprehensive customer
 * information including personal details, contact information, addresses,
 * and financial indicators. Maintains relationships with Account and Card
 * entities for complete customer management functionality.
 * 
 * Key functionality:
 * - Customer identification and personal information management
 * - Contact and address information with proper validation
 * - Financial indicators including FICO score tracking
 * - Government ID and SSN management with encryption support
 * - Primary cardholder designation for account management
 * - EFT account linkage for payment processing
 * 
 * All fields use appropriate validation to preserve COBOL field constraints
 * and ensure data integrity equivalent to mainframe data validation rules.
 */
@Entity
@Table(name = "customer_data", indexes = {
    @Index(name = "idx_customer_name", columnList = "last_name, first_name"),
    @Index(name = "idx_customer_ssn", columnList = "ssn"),
    @Index(name = "idx_customer_phone", columnList = "phone_number_1"),
    @Index(name = "idx_customer_eft", columnList = "eft_account_id")
})
public class Customer {

    /**
     * Primary key: Customer ID (9-digit numeric customer identifier).
     * Maps to CUST-ID from CVCUS01Y.cpy (PIC 9(09)).
     */
    @Id
    @Column(name = "customer_id", length = 9, nullable = false)
    @NotNull(message = "Customer ID is required")
    @Size(min = 9, max = 9, message = "Customer ID must be exactly 9 digits")
    @Pattern(regexp = "\\d{9}", message = "Customer ID must contain only digits")
    private String customerId;

    /**
     * Customer first name.
     * Maps to CUST-FIRST-NAME from CVCUS01Y.cpy (PIC X(20)).
     */
    @Column(name = "first_name", length = 20, nullable = false)
    @NotNull(message = "First name is required")
    @NotBlank(message = "First name cannot be blank")
    @Size(min = 1, max = 20, message = "First name must be 1-20 characters")
    private String firstName;

    /**
     * Customer middle name (optional).
     * Maps to CUST-MIDDLE-NAME from CVCUS01Y.cpy (PIC X(20)).
     */
    @Column(name = "middle_name", length = 20)
    @Size(max = 20, message = "Middle name cannot exceed 20 characters")
    private String middleName;

    /**
     * Customer last name.
     * Maps to CUST-LAST-NAME from CVCUS01Y.cpy (PIC X(20)).
     */
    @Column(name = "last_name", length = 20, nullable = false)
    @NotNull(message = "Last name is required")
    @NotBlank(message = "Last name cannot be blank")
    @Size(min = 1, max = 20, message = "Last name must be 1-20 characters")
    private String lastName;

    /**
     * Address line 1 (primary address).
     * Maps to CUST-ADDR-LINE-1 from CVCUS01Y.cpy (PIC X(50)).
     */
    @Column(name = "address_line_1", length = 50, nullable = false)
    @NotNull(message = "Address line 1 is required")
    @NotBlank(message = "Address line 1 cannot be blank")
    @Size(min = 1, max = 50, message = "Address line 1 must be 1-50 characters")
    private String addressLine1;

    /**
     * Address line 2 (optional).
     * Maps to CUST-ADDR-LINE-2 from CVCUS01Y.cpy (PIC X(50)).
     */
    @Column(name = "address_line_2", length = 50)
    @Size(max = 50, message = "Address line 2 cannot exceed 50 characters")
    private String addressLine2;

    /**
     * Address line 3 (optional).
     * Maps to CUST-ADDR-LINE-3 from CVCUS01Y.cpy (PIC X(50)).
     */
    @Column(name = "address_line_3", length = 50)
    @Size(max = 50, message = "Address line 3 cannot exceed 50 characters")
    private String addressLine3;

    /**
     * State code (2-character state abbreviation).
     * Maps to CUST-ADDR-STATE-CD from CVCUS01Y.cpy (PIC X(02)).
     */
    @Column(name = "state_code", length = 2, nullable = false)
    @NotNull(message = "State code is required")
    @Size(min = 2, max = 2, message = "State code must be exactly 2 characters")
    @Pattern(regexp = "[A-Z]{2}", message = "State code must be 2 uppercase letters")
    private String stateCode;

    /**
     * Country code (3-character country abbreviation).
     * Maps to CUST-ADDR-COUNTRY-CD from CVCUS01Y.cpy (PIC X(03)).
     */
    @Column(name = "country_code", length = 3, nullable = false)
    @NotNull(message = "Country code is required")
    @Size(min = 3, max = 3, message = "Country code must be exactly 3 characters")
    @Pattern(regexp = "[A-Z]{3}", message = "Country code must be 3 uppercase letters")
    private String countryCode;

    /**
     * ZIP code (up to 10 characters for international support).
     * Maps to CUST-ADDR-ZIP from CVCUS01Y.cpy (PIC X(10)).
     */
    @Column(name = "zip_code", length = 10, nullable = false)
    @NotNull(message = "ZIP code is required")
    @Size(min = 5, max = 10, message = "ZIP code must be 5-10 characters")
    private String zipCode;

    /**
     * Primary phone number.
     * Maps to CUST-PHONE-NUM-1 from CVCUS01Y.cpy (PIC X(15)).
     */
    @Column(name = "phone_number_1", length = 15, nullable = false)
    @NotNull(message = "Primary phone number is required")
    @Size(min = 10, max = 15, message = "Phone number must be 10-15 characters")
    @Pattern(regexp = "[0-9\\-\\(\\)\\+\\s]+", message = "Phone number contains invalid characters")
    private String phoneNumber1;

    /**
     * Secondary phone number (optional).
     * Maps to CUST-PHONE-NUM-2 from CVCUS01Y.cpy (PIC X(15)).
     */
    @Column(name = "phone_number_2", length = 15)
    @Size(max = 15, message = "Phone number cannot exceed 15 characters")
    @Pattern(regexp = "[0-9\\-\\(\\)\\+\\s]*", message = "Phone number contains invalid characters")
    private String phoneNumber2;

    /**
     * Social Security Number (9 digits, encrypted).
     * Maps to CUST-SSN from CVCUS01Y.cpy (PIC X(09)).
     */
    @Column(name = "ssn", length = 9, nullable = false)
    @NotNull(message = "SSN is required")
    @Size(min = 9, max = 9, message = "SSN must be exactly 9 digits")
    @Pattern(regexp = "\\d{9}", message = "SSN must contain only digits")
    private String ssn;

    /**
     * Government issued ID (driver's license, passport, etc.).
     * Maps to CUST-GOVT-ISSUED-ID from CVCUS01Y.cpy (PIC X(20)).
     */
    @Column(name = "government_id", length = 20)
    @Size(max = 20, message = "Government ID cannot exceed 20 characters")
    private String governmentId;

    /**
     * Date of birth.
     * Maps to CUST-DOB-YYYYMMDD from CVCUS01Y.cpy (PIC X(08)).
     */
    @Column(name = "date_of_birth", nullable = false)
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    /**
     * EFT account ID for electronic funds transfer.
     * Maps to CUST-EFT-ACCOUNT-ID from CVCUS01Y.cpy (PIC X(10)).
     */
    @Column(name = "eft_account_id", length = 10)
    @Size(max = 10, message = "EFT account ID cannot exceed 10 characters")
    private String eftAccountId;

    /**
     * Primary cardholder indicator ('Y' = primary, 'N' = not primary).
     * Maps to CUST-PRI-CARD-HOLDER-IND from CVCUS01Y.cpy (PIC X(01)).
     */
    @Column(name = "primary_card_holder", length = 1, nullable = false)
    @NotNull(message = "Primary cardholder indicator is required")
    @Pattern(regexp = "[YN]", message = "Primary cardholder indicator must be 'Y' or 'N'")
    private String primaryCardHolder;

    /**
     * FICO credit score (300-850 range).
     * Maps to CUST-FICO-CREDIT-SCORE from CVCUS01Y.cpy (PIC 9(03)).
     */
    @Column(name = "fico_score")
    @Min(value = 300, message = "FICO score must be at least 300")
    @Max(value = 850, message = "FICO score cannot exceed 850")
    private Integer ficoScore;

    /**
     * One-to-many relationship with Account entities.
     * Enables navigation from customer to associated accounts.
     */
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Account> accounts;

    /**
     * Default constructor for JPA.
     */
    public Customer() {
        // Default constructor for JPA entity requirements
    }

    /**
     * Constructor with required fields.
     */
    public Customer(String customerId, String firstName, String lastName, 
                    String addressLine1, String stateCode, String countryCode, 
                    String zipCode, String phoneNumber1, String ssn, 
                    LocalDate dateOfBirth, String primaryCardHolder) {
        this.customerId = customerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.addressLine1 = addressLine1;
        this.stateCode = stateCode;
        this.countryCode = countryCode;
        this.zipCode = zipCode;
        this.phoneNumber1 = phoneNumber1;
        this.ssn = ssn;
        this.dateOfBirth = dateOfBirth;
        this.primaryCardHolder = primaryCardHolder;
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

    public String getGovernmentId() {
        return governmentId;
    }

    public void setGovernmentId(String governmentId) {
        this.governmentId = governmentId;
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

    public String getPrimaryCardHolder() {
        return primaryCardHolder;
    }

    public void setPrimaryCardHolder(String primaryCardHolder) {
        this.primaryCardHolder = primaryCardHolder;
    }

    public Integer getFicoScore() {
        return ficoScore;
    }

    public void setFicoScore(Integer ficoScore) {
        this.ficoScore = ficoScore;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }

    /**
     * Utility method to check if customer is primary cardholder.
     */
    public boolean isPrimaryCardHolder() {
        return "Y".equals(primaryCardHolder);
    }

    /**
     * Utility method to get full name.
     */
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        fullName.append(firstName);
        if (middleName != null && !middleName.trim().isEmpty()) {
            fullName.append(" ").append(middleName);
        }
        fullName.append(" ").append(lastName);
        return fullName.toString();
    }

    /**
     * Utility method to get formatted address.
     */
    public String getFormattedAddress() {
        StringBuilder address = new StringBuilder();
        address.append(addressLine1);
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
     * Utility method to get masked SSN for display.
     */
    public String getMaskedSsn() {
        if (ssn == null || ssn.length() != 9) {
            return "***-**-****";
        }
        return "***-**-" + ssn.substring(5);
    }

    /**
     * Utility method to check if customer has good credit.
     */
    public boolean hasGoodCredit() {
        return ficoScore != null && ficoScore >= 650;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
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
                ", ssn='" + getMaskedSsn() + '\'' +
                ", governmentId='" + governmentId + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", eftAccountId='" + eftAccountId + '\'' +
                ", primaryCardHolder='" + primaryCardHolder + '\'' +
                ", ficoScore=" + ficoScore +
                '}';
    }
}