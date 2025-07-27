package com.carddemo.common.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.io.Serializable;

/**
 * JPA Customer entity mapping COBOL CUSTOMER-RECORD to PostgreSQL customers table
 * with personal information, address data, SSN encryption, FICO score validation,
 * and relationships to accounts and cards supporting customer profile management
 */
@Entity
@Table(name = "customers")
public class Customer implements Serializable {

    @Id
    @Column(name = "customer_id", length = 9)
    @Size(min = 9, max = 9, message = "Customer ID must be exactly 9 characters")
    private String customerId;

    @Column(name = "first_name", length = 20)
    @Size(max = 20, message = "First name must not exceed 20 characters")
    @NotBlank(message = "First name is required")
    private String firstName;

    @Column(name = "middle_name", length = 20)
    @Size(max = 20, message = "Middle name must not exceed 20 characters")
    private String middleName;

    @Column(name = "last_name", length = 20)
    @Size(max = 20, message = "Last name must not exceed 20 characters")
    @NotBlank(message = "Last name is required")
    private String lastName;

    @Column(name = "address_line_1", length = 50)
    @Size(max = 50, message = "Address line 1 must not exceed 50 characters")
    private String addressLine1;

    @Column(name = "address_line_2", length = 50)
    @Size(max = 50, message = "Address line 2 must not exceed 50 characters")
    private String addressLine2;

    @Column(name = "address_line_3", length = 50)
    @Size(max = 50, message = "Address line 3 must not exceed 50 characters")
    private String addressLine3;

    @Column(name = "state_code", length = 2)
    @Size(min = 2, max = 2, message = "State code must be exactly 2 characters")
    private String stateCode;

    @Column(name = "country_code", length = 3)
    @Size(min = 3, max = 3, message = "Country code must be exactly 3 characters")
    private String countryCode;

    @Column(name = "zip_code", length = 10)
    @Size(max = 10, message = "Zip code must not exceed 10 characters")
    private String zipCode;

    @Column(name = "phone_number_1", length = 15)
    @Size(max = 15, message = "Phone number 1 must not exceed 15 characters")
    private String phoneNumber1;

    @Column(name = "phone_number_2", length = 15)
    @Size(max = 15, message = "Phone number 2 must not exceed 15 characters")
    private String phoneNumber2;

    @Column(name = "ssn", length = 9)
    @Size(min = 9, max = 9, message = "SSN must be exactly 9 characters")
    private String ssn;

    @Column(name = "government_id", length = 20)
    @Size(max = 20, message = "Government ID must not exceed 20 characters")
    private String governmentId;

    @Column(name = "date_of_birth")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Column(name = "eft_account_id", length = 11)
    @Size(max = 11, message = "EFT account ID must not exceed 11 characters")
    private String eftAccountId;

    @Column(name = "primary_cardholder_indicator", length = 1)
    @Size(min = 1, max = 1, message = "Primary cardholder indicator must be exactly 1 character")
    @Pattern(regexp = "[YN]", message = "Primary cardholder indicator must be Y or N")
    private String primaryCardholderIndicator;

    @Column(name = "fico_credit_score")
    @Min(value = 300, message = "FICO credit score must be at least 300")
    @Max(value = 850, message = "FICO credit score must not exceed 850")
    private Integer ficoCreditScore;

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Account> accounts;

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Card> cards;

    // Default constructor
    public Customer() {
        this.primaryCardholderIndicator = "Y";
    }

    // Constructor with customer ID
    public Customer(String customerId) {
        this();
        this.customerId = customerId;
    }

    // Getters and setters
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }

    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }

    public String getAddressLine3() { return addressLine3; }
    public void setAddressLine3(String addressLine3) { this.addressLine3 = addressLine3; }

    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getPhoneNumber1() { return phoneNumber1; }
    public void setPhoneNumber1(String phoneNumber1) { this.phoneNumber1 = phoneNumber1; }

    public String getPhoneNumber2() { return phoneNumber2; }
    public void setPhoneNumber2(String phoneNumber2) { this.phoneNumber2 = phoneNumber2; }

    public String getSsn() { return ssn; }
    public void setSsn(String ssn) { this.ssn = ssn; }

    public String getGovernmentId() { return governmentId; }
    public void setGovernmentId(String governmentId) { this.governmentId = governmentId; }

    // Alias methods for backward compatibility with different naming conventions
    public String getGovernmentIssuedId() { return governmentId; }
    public void setGovernmentIssuedId(String governmentId) { this.governmentId = governmentId; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getEftAccountId() { return eftAccountId; }
    public void setEftAccountId(String eftAccountId) { this.eftAccountId = eftAccountId; }

    public String getPrimaryCardholderIndicator() { return primaryCardholderIndicator; }
    public void setPrimaryCardholderIndicator(String primaryCardholderIndicator) { 
        this.primaryCardholderIndicator = primaryCardholderIndicator; 
    }

    // Alias methods for backward compatibility with different naming conventions
    public String getPrimaryCardHolderIndicator() { return primaryCardholderIndicator; }
    public void setPrimaryCardHolderIndicator(String primaryCardholderIndicator) { 
        this.primaryCardholderIndicator = primaryCardholderIndicator; 
    }

    public Integer getFicoCreditScore() { return ficoCreditScore; }
    public void setFicoCreditScore(Integer ficoCreditScore) { this.ficoCreditScore = ficoCreditScore; }

    public List<Account> getAccounts() { return accounts; }
    public void setAccounts(List<Account> accounts) { this.accounts = accounts; }

    public List<Card> getCards() { return cards; }
    public void setCards(List<Card> cards) { this.cards = cards; }

    // Utility methods
    public String getFullName() {
        // COBOL-style formatting: "LASTNAME, FIRSTNAME MIDDLENAME"
        StringBuilder fullName = new StringBuilder();
        if (lastName != null) {
            fullName.append(lastName);
        }
        if (firstName != null) {
            if (fullName.length() > 0) fullName.append(", ");
            fullName.append(firstName);
        }
        if (middleName != null && !middleName.trim().isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(middleName);
        }
        return fullName.toString();
    }

    public boolean isPrimaryCardholder() {
        return "Y".equals(primaryCardholderIndicator);
    }

    // Alias method for backward compatibility
    public boolean isPrimaryCardHolder() {
        return isPrimaryCardholder();
    }

    public String getMaskedSsn() {
        if (ssn != null && ssn.length() >= 4) {
            return "XXX-XX-" + ssn.substring(ssn.length() - 4);
        }
        return "XXX-XX-XXXX";
    }

    public String getFormattedAddress() {
        // COBOL-style address formatting combining all address components
        StringBuilder formattedAddress = new StringBuilder();
        
        if (addressLine1 != null && !addressLine1.trim().isEmpty()) {
            formattedAddress.append(addressLine1.trim());
        }
        
        if (addressLine2 != null && !addressLine2.trim().isEmpty()) {
            if (formattedAddress.length() > 0) formattedAddress.append(", ");
            formattedAddress.append(addressLine2.trim());
        }
        
        if (addressLine3 != null && !addressLine3.trim().isEmpty()) {
            if (formattedAddress.length() > 0) formattedAddress.append(", ");
            formattedAddress.append(addressLine3.trim());
        }
        
        // Add state code if available
        if (stateCode != null && !stateCode.trim().isEmpty()) {
            if (formattedAddress.length() > 0) formattedAddress.append(", ");
            formattedAddress.append(stateCode.trim());
        }
        
        // Add ZIP code if available
        if (zipCode != null && !zipCode.trim().isEmpty()) {
            if (formattedAddress.length() > 0) formattedAddress.append(" ");
            formattedAddress.append(zipCode.trim());
        }
        
        // Add country code if available
        if (countryCode != null && !countryCode.trim().isEmpty()) {
            if (formattedAddress.length() > 0) formattedAddress.append(", ");
            formattedAddress.append(countryCode.trim());
        }
        
        return formattedAddress.toString();
    }

    public boolean isValidFicoScore() {
        return ficoCreditScore != null && ficoCreditScore >= 300 && ficoCreditScore <= 850;
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
                ", lastName='" + lastName + '\'' +
                ", primaryCardholderIndicator='" + primaryCardholderIndicator + '\'' +
                ", ficoCreditScore=" + ficoCreditScore +
                '}';
    }
}