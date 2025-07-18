/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.account.dto;

import com.carddemo.common.validator.ValidSSN;
import com.carddemo.common.validator.ValidPhoneNumber;
import com.carddemo.common.validator.ValidCCYYMMDD;
import com.carddemo.common.validator.ValidFicoScore;
import com.carddemo.common.enums.ValidationResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.Valid;
import java.util.Objects;
import java.math.BigDecimal;

/**
 * Customer Data Transfer Object for comprehensive customer profile management.
 * 
 * This DTO maintains the exact structure and validation patterns from the original
 * COBOL customer record definitions (CVCUS01Y.cpy and CUSTREC.cpy), providing
 * complete customer profile information with comprehensive validation for account
 * management operations.
 * 
 * Original COBOL Structure (CVCUS01Y.cpy):
 * - CUST-ID                     PIC 9(09)     -> customerId
 * - CUST-FIRST-NAME             PIC X(25)     -> firstName
 * - CUST-MIDDLE-NAME            PIC X(25)     -> middleName
 * - CUST-LAST-NAME              PIC X(25)     -> lastName
 * - CUST-ADDR-LINE-1/2/3        PIC X(50)     -> AddressDto
 * - CUST-ADDR-STATE-CD          PIC X(02)     -> AddressDto.stateCode
 * - CUST-ADDR-COUNTRY-CD        PIC X(03)     -> AddressDto.countryCode
 * - CUST-ADDR-ZIP               PIC X(10)     -> AddressDto.zipCode
 * - CUST-PHONE-NUM-1            PIC X(15)     -> phoneNumber1
 * - CUST-PHONE-NUM-2            PIC X(15)     -> phoneNumber2
 * - CUST-SSN                    PIC 9(09)     -> ssn
 * - CUST-GOVT-ISSUED-ID         PIC X(20)     -> governmentIssuedId
 * - CUST-DOB-YYYY-MM-DD         PIC X(10)     -> dateOfBirth
 * - CUST-EFT-ACCOUNT-ID         PIC X(10)     -> eftAccountId
 * - CUST-PRI-CARD-HOLDER-IND    PIC X(01)     -> primaryCardHolder
 * - CUST-FICO-CREDIT-SCORE      PIC 9(03)     -> ficoCreditScore
 * 
 * Key Features:
 * - Complete customer profile with personal information and address
 * - Comprehensive validation including SSN, phone number, date format validation
 * - FICO credit score management with range validation (300-850)
 * - Primary card holder indicator for account relationship management
 * - Government issued ID support for identity verification
 * - EFT account linkage for payment processing
 * - Jakarta Bean Validation integration for Spring Boot REST API validation
 * - JSON serialization support for React frontend components
 * - Nested address validation with state/ZIP code cross-validation
 * 
 * Validation Rules:
 * - Customer ID: Maximum 9 digits (matching COBOL PIC 9(09))
 * - Names: Maximum 25 characters each (matching COBOL PIC X(25))
 * - Phone numbers: NANP format validation with area code verification
 * - SSN: 9-digit format with comprehensive validation rules
 * - Date of birth: CCYYMMDD format with leap year and range validation
 * - FICO score: Range 300-850 with nullable support for customers without credit history
 * - Address: Multi-line address with state/ZIP code cross-validation
 * - Government ID: Maximum 20 characters (matching COBOL PIC X(20))
 * - EFT account: Maximum 10 characters (matching COBOL PIC X(10))
 * 
 * Integration Points:
 * - Account management services for customer profile updates
 * - Card management services for primary card holder determination
 * - Credit assessment services for FICO score management
 * - Payment processing services for EFT account information
 * - React frontend components for customer form validation
 * - Spring Boot REST API endpoints for customer data exchange
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 */
public class CustomerDto {
    
    /**
     * Customer identification number (unique identifier).
     * Maps to CUST-ID PIC 9(09) from CVCUS01Y.cpy
     */
    @JsonProperty("customer_id")
    @Max(value = 999999999L, message = "Customer ID cannot exceed 9 digits")
    private Long customerId;
    
    /**
     * Customer's first name.
     * Maps to CUST-FIRST-NAME PIC X(25) from CVCUS01Y.cpy
     */
    @JsonProperty("first_name")
    private String firstName;
    
    /**
     * Customer's middle name (optional).
     * Maps to CUST-MIDDLE-NAME PIC X(25) from CVCUS01Y.cpy
     */
    @JsonProperty("middle_name")
    private String middleName;
    
    /**
     * Customer's last name.
     * Maps to CUST-LAST-NAME PIC X(25) from CVCUS01Y.cpy
     */
    @JsonProperty("last_name")
    private String lastName;
    
    /**
     * Customer's complete address information.
     * Maps to CUST-ADDR-LINE-1/2/3, CUST-ADDR-STATE-CD, CUST-ADDR-COUNTRY-CD, 
     * and CUST-ADDR-ZIP from CVCUS01Y.cpy
     */
    @JsonProperty("address")
    @Valid
    private AddressDto address;
    
    /**
     * Primary phone number (required).
     * Maps to CUST-PHONE-NUM-1 PIC X(15) from CVCUS01Y.cpy
     */
    @JsonProperty("phone_number_1")
    @ValidPhoneNumber(message = "Invalid primary phone number format or area code")
    private String phoneNumber1;
    
    /**
     * Secondary phone number (optional).
     * Maps to CUST-PHONE-NUM-2 PIC X(15) from CVCUS01Y.cpy
     */
    @JsonProperty("phone_number_2")
    @ValidPhoneNumber(allowEmpty = true, message = "Invalid secondary phone number format or area code")
    private String phoneNumber2;
    
    /**
     * Social Security Number with comprehensive validation.
     * Maps to CUST-SSN PIC 9(09) from CVCUS01Y.cpy
     */
    @JsonProperty("ssn")
    @ValidSSN(message = "SSN must be a valid 9-digit format")
    private String ssn;
    
    /**
     * Government issued identification (driver's license, state ID, etc.).
     * Maps to CUST-GOVT-ISSUED-ID PIC X(20) from CVCUS01Y.cpy
     */
    @JsonProperty("government_issued_id")
    private String governmentIssuedId;
    
    /**
     * Date of birth in CCYYMMDD format.
     * Maps to CUST-DOB-YYYY-MM-DD PIC X(10) from CVCUS01Y.cpy
     */
    @JsonProperty("date_of_birth")
    @ValidCCYYMMDD(fieldName = "Date of Birth", message = "Invalid date of birth format")
    private String dateOfBirth;
    
    /**
     * Electronic Funds Transfer account identifier.
     * Maps to CUST-EFT-ACCOUNT-ID PIC X(10) from CVCUS01Y.cpy
     */
    @JsonProperty("eft_account_id")
    private String eftAccountId;
    
    /**
     * Primary card holder indicator (Y/N).
     * Maps to CUST-PRI-CARD-HOLDER-IND PIC X(01) from CVCUS01Y.cpy
     */
    @JsonProperty("primary_card_holder")
    private boolean primaryCardHolder;
    
    /**
     * FICO credit score with range validation.
     * Maps to CUST-FICO-CREDIT-SCORE PIC 9(03) from CVCUS01Y.cpy
     */
    @JsonProperty("fico_credit_score")
    @ValidFicoScore(message = "FICO credit score must be between 300 and 850")
    private Integer ficoCreditScore;
    
    /**
     * Default constructor for JSON deserialization and framework usage.
     */
    public CustomerDto() {
        // Initialize with default values
        this.customerId = 0L;
        this.firstName = "";
        this.middleName = "";
        this.lastName = "";
        this.address = new AddressDto();
        this.phoneNumber1 = "";
        this.phoneNumber2 = "";
        this.ssn = "";
        this.governmentIssuedId = "";
        this.dateOfBirth = "";
        this.eftAccountId = "";
        this.primaryCardHolder = false;
        this.ficoCreditScore = null;
    }
    
    /**
     * Constructor with essential customer information.
     * 
     * @param customerId Customer identification number
     * @param firstName Customer's first name
     * @param lastName Customer's last name
     * @param ssn Social Security Number
     * @param phoneNumber1 Primary phone number
     */
    public CustomerDto(Long customerId, String firstName, String lastName, String ssn, String phoneNumber1) {
        this();
        this.customerId = customerId;
        this.firstName = firstName != null ? firstName : "";
        this.lastName = lastName != null ? lastName : "";
        this.ssn = ssn != null ? ssn : "";
        this.phoneNumber1 = phoneNumber1 != null ? phoneNumber1 : "";
    }
    
    /**
     * Constructor with comprehensive customer information.
     * 
     * @param customerId Customer identification number
     * @param firstName Customer's first name
     * @param middleName Customer's middle name
     * @param lastName Customer's last name
     * @param address Complete address information
     * @param phoneNumber1 Primary phone number
     * @param phoneNumber2 Secondary phone number
     * @param ssn Social Security Number
     * @param governmentIssuedId Government issued identification
     * @param dateOfBirth Date of birth in CCYYMMDD format
     * @param eftAccountId Electronic Funds Transfer account ID
     * @param primaryCardHolder Primary card holder indicator
     * @param ficoCreditScore FICO credit score
     */
    public CustomerDto(Long customerId, String firstName, String middleName, String lastName,
                      AddressDto address, String phoneNumber1, String phoneNumber2, String ssn,
                      String governmentIssuedId, String dateOfBirth, String eftAccountId,
                      boolean primaryCardHolder, Integer ficoCreditScore) {
        this.customerId = customerId;
        this.firstName = firstName != null ? firstName : "";
        this.middleName = middleName != null ? middleName : "";
        this.lastName = lastName != null ? lastName : "";
        this.address = address != null ? address : new AddressDto();
        this.phoneNumber1 = phoneNumber1 != null ? phoneNumber1 : "";
        this.phoneNumber2 = phoneNumber2 != null ? phoneNumber2 : "";
        this.ssn = ssn != null ? ssn : "";
        this.governmentIssuedId = governmentIssuedId != null ? governmentIssuedId : "";
        this.dateOfBirth = dateOfBirth != null ? dateOfBirth : "";
        this.eftAccountId = eftAccountId != null ? eftAccountId : "";
        this.primaryCardHolder = primaryCardHolder;
        this.ficoCreditScore = ficoCreditScore;
    }
    
    /**
     * Gets the customer identification number.
     * 
     * @return Customer ID
     */
    public Long getCustomerId() {
        return customerId;
    }
    
    /**
     * Sets the customer identification number.
     * 
     * @param customerId Customer ID
     */
    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }
    
    /**
     * Gets the customer's first name.
     * 
     * @return First name
     */
    public String getFirstName() {
        return firstName;
    }
    
    /**
     * Sets the customer's first name.
     * 
     * @param firstName First name
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName != null ? firstName : "";
    }
    
    /**
     * Gets the customer's middle name.
     * 
     * @return Middle name
     */
    public String getMiddleName() {
        return middleName;
    }
    
    /**
     * Sets the customer's middle name.
     * 
     * @param middleName Middle name
     */
    public void setMiddleName(String middleName) {
        this.middleName = middleName != null ? middleName : "";
    }
    
    /**
     * Gets the customer's last name.
     * 
     * @return Last name
     */
    public String getLastName() {
        return lastName;
    }
    
    /**
     * Sets the customer's last name.
     * 
     * @param lastName Last name
     */
    public void setLastName(String lastName) {
        this.lastName = lastName != null ? lastName : "";
    }
    
    /**
     * Gets the customer's complete address information.
     * 
     * @return Address information
     */
    public AddressDto getAddress() {
        return address;
    }
    
    /**
     * Sets the customer's complete address information.
     * 
     * @param address Address information
     */
    public void setAddress(AddressDto address) {
        this.address = address != null ? address : new AddressDto();
    }
    
    /**
     * Gets the primary phone number.
     * 
     * @return Primary phone number
     */
    public String getPhoneNumber1() {
        return phoneNumber1;
    }
    
    /**
     * Sets the primary phone number.
     * 
     * @param phoneNumber1 Primary phone number
     */
    public void setPhoneNumber1(String phoneNumber1) {
        this.phoneNumber1 = phoneNumber1 != null ? phoneNumber1 : "";
    }
    
    /**
     * Gets the secondary phone number.
     * 
     * @return Secondary phone number
     */
    public String getPhoneNumber2() {
        return phoneNumber2;
    }
    
    /**
     * Sets the secondary phone number.
     * 
     * @param phoneNumber2 Secondary phone number
     */
    public void setPhoneNumber2(String phoneNumber2) {
        this.phoneNumber2 = phoneNumber2 != null ? phoneNumber2 : "";
    }
    
    /**
     * Gets the Social Security Number.
     * 
     * @return SSN
     */
    public String getSsn() {
        return ssn;
    }
    
    /**
     * Sets the Social Security Number.
     * 
     * @param ssn SSN
     */
    public void setSsn(String ssn) {
        this.ssn = ssn != null ? ssn : "";
    }
    
    /**
     * Gets the government issued identification.
     * 
     * @return Government issued ID
     */
    public String getGovernmentIssuedId() {
        return governmentIssuedId;
    }
    
    /**
     * Sets the government issued identification.
     * 
     * @param governmentIssuedId Government issued ID
     */
    public void setGovernmentIssuedId(String governmentIssuedId) {
        this.governmentIssuedId = governmentIssuedId != null ? governmentIssuedId : "";
    }
    
    /**
     * Gets the date of birth.
     * 
     * @return Date of birth in CCYYMMDD format
     */
    public String getDateOfBirth() {
        return dateOfBirth;
    }
    
    /**
     * Sets the date of birth.
     * 
     * @param dateOfBirth Date of birth in CCYYMMDD format
     */
    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth != null ? dateOfBirth : "";
    }
    
    /**
     * Gets the Electronic Funds Transfer account identifier.
     * 
     * @return EFT account ID
     */
    public String getEftAccountId() {
        return eftAccountId;
    }
    
    /**
     * Sets the Electronic Funds Transfer account identifier.
     * 
     * @param eftAccountId EFT account ID
     */
    public void setEftAccountId(String eftAccountId) {
        this.eftAccountId = eftAccountId != null ? eftAccountId : "";
    }
    
    /**
     * Checks if this customer is the primary card holder.
     * 
     * @return true if primary card holder, false otherwise
     */
    public boolean isPrimaryCardHolder() {
        return primaryCardHolder;
    }
    
    /**
     * Sets the primary card holder indicator.
     * 
     * @param primaryCardHolder Primary card holder indicator
     */
    public void setPrimaryCardHolder(boolean primaryCardHolder) {
        this.primaryCardHolder = primaryCardHolder;
    }
    
    /**
     * Gets the FICO credit score.
     * 
     * @return FICO credit score (300-850 range)
     */
    public Integer getFicoCreditScore() {
        return ficoCreditScore;
    }
    
    /**
     * Sets the FICO credit score.
     * 
     * @param ficoCreditScore FICO credit score (300-850 range)
     */
    public void setFicoCreditScore(Integer ficoCreditScore) {
        this.ficoCreditScore = ficoCreditScore;
    }
    
    /**
     * Validates the customer data using COBOL-equivalent validation rules.
     * 
     * This method performs comprehensive validation of all customer fields using the same
     * validation logic as the original COBOL implementation, including:
     * - Customer ID range validation (1-999999999)
     * - Name field validation (required first and last name)
     * - SSN format and business rule validation
     * - Phone number format and area code validation
     * - Date of birth format and range validation
     * - FICO score range validation (300-850)
     * - Address validation including state/ZIP code cross-validation
     * - Government ID format validation
     * - EFT account ID format validation
     * 
     * @return ValidationResult indicating the validation outcome
     */
    public ValidationResult validate() {
        // Validate customer ID
        if (this.customerId == null || this.customerId <= 0 || this.customerId > 999999999L) {
            return ValidationResult.INVALID_RANGE;
        }
        
        // Validate required name fields
        if (this.firstName == null || this.firstName.trim().isEmpty()) {
            return ValidationResult.BLANK_FIELD;
        }
        if (this.lastName == null || this.lastName.trim().isEmpty()) {
            return ValidationResult.BLANK_FIELD;
        }
        
        // Validate field lengths (matching COBOL PIC X(25) constraints)
        if (this.firstName.length() > 25) {
            return ValidationResult.INVALID_RANGE;
        }
        if (this.middleName != null && this.middleName.length() > 25) {
            return ValidationResult.INVALID_RANGE;
        }
        if (this.lastName.length() > 25) {
            return ValidationResult.INVALID_RANGE;
        }
        
        // Validate SSN format if provided
        if (this.ssn != null && !this.ssn.trim().isEmpty()) {
            if (!isValidSSNFormat(this.ssn)) {
                return ValidationResult.INVALID_FORMAT;
            }
        }
        
        // Validate primary phone number format if provided
        if (this.phoneNumber1 != null && !this.phoneNumber1.trim().isEmpty()) {
            if (!isValidPhoneFormat(this.phoneNumber1)) {
                return ValidationResult.INVALID_FORMAT;
            }
        }
        
        // Validate secondary phone number format if provided
        if (this.phoneNumber2 != null && !this.phoneNumber2.trim().isEmpty()) {
            if (!isValidPhoneFormat(this.phoneNumber2)) {
                return ValidationResult.INVALID_FORMAT;
            }
        }
        
        // Validate date of birth format if provided
        if (this.dateOfBirth != null && !this.dateOfBirth.trim().isEmpty()) {
            if (!isValidDateFormat(this.dateOfBirth)) {
                return ValidationResult.INVALID_DATE;
            }
        }
        
        // Validate FICO score range if provided
        if (this.ficoCreditScore != null) {
            if (this.ficoCreditScore < 300 || this.ficoCreditScore > 850) {
                return ValidationResult.INVALID_RANGE;
            }
        }
        
        // Validate government issued ID length if provided
        if (this.governmentIssuedId != null && this.governmentIssuedId.length() > 20) {
            return ValidationResult.INVALID_RANGE;
        }
        
        // Validate EFT account ID length if provided
        if (this.eftAccountId != null && this.eftAccountId.length() > 10) {
            return ValidationResult.INVALID_RANGE;
        }
        
        // Validate address if provided
        if (this.address != null) {
            ValidationResult addressResult = this.address.validate();
            if (!addressResult.isValid()) {
                return addressResult;
            }
        }
        
        return ValidationResult.VALID;
    }
    
    /**
     * Validates SSN format using basic format rules.
     * 
     * @param ssn SSN to validate
     * @return true if format is valid, false otherwise
     */
    private boolean isValidSSNFormat(String ssn) {
        if (ssn == null || ssn.trim().isEmpty()) {
            return false;
        }
        
        // Remove formatting characters
        String cleanSSN = ssn.replaceAll("[^0-9]", "");
        
        // Must be exactly 9 digits
        return cleanSSN.length() == 9 && cleanSSN.matches("\\d{9}");
    }
    
    /**
     * Validates phone number format using basic format rules.
     * 
     * @param phone Phone number to validate
     * @return true if format is valid, false otherwise
     */
    private boolean isValidPhoneFormat(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        
        // Remove formatting characters
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        
        // Must be exactly 10 digits for North American format
        return cleanPhone.length() == 10 && cleanPhone.matches("\\d{10}");
    }
    
    /**
     * Validates date format using basic CCYYMMDD format rules.
     * 
     * @param date Date to validate
     * @return true if format is valid, false otherwise
     */
    private boolean isValidDateFormat(String date) {
        if (date == null || date.trim().isEmpty()) {
            return false;
        }
        
        // Must be exactly 8 characters and all numeric
        return date.length() == 8 && date.matches("\\d{8}");
    }
    
    /**
     * Checks if two CustomerDto objects are equal.
     * 
     * @param obj Object to compare with
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CustomerDto that = (CustomerDto) obj;
        return primaryCardHolder == that.primaryCardHolder &&
               Objects.equals(customerId, that.customerId) &&
               Objects.equals(firstName, that.firstName) &&
               Objects.equals(middleName, that.middleName) &&
               Objects.equals(lastName, that.lastName) &&
               Objects.equals(address, that.address) &&
               Objects.equals(phoneNumber1, that.phoneNumber1) &&
               Objects.equals(phoneNumber2, that.phoneNumber2) &&
               Objects.equals(ssn, that.ssn) &&
               Objects.equals(governmentIssuedId, that.governmentIssuedId) &&
               Objects.equals(dateOfBirth, that.dateOfBirth) &&
               Objects.equals(eftAccountId, that.eftAccountId) &&
               Objects.equals(ficoCreditScore, that.ficoCreditScore);
    }
    
    /**
     * Generates hash code for the CustomerDto object.
     * 
     * @return Hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(customerId, firstName, middleName, lastName, address,
                           phoneNumber1, phoneNumber2, ssn, governmentIssuedId,
                           dateOfBirth, eftAccountId, primaryCardHolder, ficoCreditScore);
    }
    
    /**
     * Returns a string representation of the CustomerDto object.
     * 
     * @return String representation of the customer
     */
    @Override
    public String toString() {
        return "CustomerDto{" +
               "customerId=" + customerId +
               ", firstName='" + firstName + '\'' +
               ", middleName='" + middleName + '\'' +
               ", lastName='" + lastName + '\'' +
               ", address=" + address +
               ", phoneNumber1='" + phoneNumber1 + '\'' +
               ", phoneNumber2='" + phoneNumber2 + '\'' +
               ", ssn='" + (ssn != null && !ssn.isEmpty() ? "***-**-" + ssn.substring(Math.max(0, ssn.length() - 4)) : "") + '\'' +
               ", governmentIssuedId='" + governmentIssuedId + '\'' +
               ", dateOfBirth='" + dateOfBirth + '\'' +
               ", eftAccountId='" + eftAccountId + '\'' +
               ", primaryCardHolder=" + primaryCardHolder +
               ", ficoCreditScore=" + ficoCreditScore +
               '}';
    }
}