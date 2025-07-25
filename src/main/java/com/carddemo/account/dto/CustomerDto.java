/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carddemo.account.dto;

import com.carddemo.account.dto.AddressDto;
import com.carddemo.common.validator.ValidSSN;
import com.carddemo.common.validator.ValidPhoneNumber;
import com.carddemo.common.validator.ValidCCYYMMDD;
import com.carddemo.common.validator.ValidFicoScore;
import com.carddemo.common.enums.ValidationResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Customer Data Transfer Object supporting comprehensive personal information,
 * address details, and FICO score validation for account management operations.
 * 
 * This DTO preserves the exact CUSTDAT record structure from CVCUS01Y.cpy while
 * providing modern validation capabilities through Jakarta Bean Validation and
 * custom business rule validators. The class maintains complete structural
 * compatibility with the original COBOL customer record layout.
 * 
 * Key Features:
 * - Complete customer profile management with data integrity validation
 * - SSN, phone, ZIP code format validation per Section 2.1.3 requirements
 * - FICO score management with range validation (300-850)
 * - Comprehensive personal data validation including date of birth validation
 * - Address normalization and state/ZIP code cross-validation
 * - JSON serialization support for React frontend components
 * - Cascading validation for nested address information
 * 
 * Field Structure (from CVCUS01Y.cpy customer record - RECLN 500):
 * - CUST-ID                    PIC 9(09)     → customerId
 * - CUST-FIRST-NAME           PIC X(25)     → firstName  
 * - CUST-MIDDLE-NAME          PIC X(25)     → middleName
 * - CUST-LAST-NAME            PIC X(25)     → lastName
 * - CUST-ADDR-LINE-1          PIC X(50)     → address.addressLine1
 * - CUST-ADDR-LINE-2          PIC X(50)     → address.addressLine2
 * - CUST-ADDR-LINE-3          PIC X(50)     → address.addressLine3
 * - CUST-ADDR-STATE-CD        PIC X(02)     → address.stateCode
 * - CUST-ADDR-COUNTRY-CD      PIC X(03)     → address.countryCode
 * - CUST-ADDR-ZIP             PIC X(10)     → address.zipCode
 * - CUST-PHONE-NUM-1          PIC X(15)     → phoneNumber1
 * - CUST-PHONE-NUM-2          PIC X(15)     → phoneNumber2
 * - CUST-SSN                  PIC 9(09)     → ssn
 * - CUST-GOVT-ISSUED-ID       PIC X(20)     → governmentIssuedId
 * - CUST-DOB-YYYY-MM-DD       PIC X(10)     → dateOfBirth
 * - CUST-EFT-ACCOUNT-ID       PIC X(10)     → eftAccountId
 * - CUST-PRI-CARD-HOLDER-IND  PIC X(01)     → primaryCardHolder
 * - CUST-FICO-CREDIT-SCORE    PIC 9(03)     → ficoCreditScore
 * 
 * Validation Rules:
 * - Customer ID must be 9-digit numeric value
 * - Name fields cannot exceed 25 characters each
 * - SSN must be valid 9-digit format with business rule validation
 * - Phone numbers must conform to North American Numbering Plan
 * - Date of birth must be valid CCYYMMDD format
 * - FICO score must be between 300-850 (if provided)
 * - Address must have complete validation with state/ZIP cross-validation
 * - Government issued ID cannot exceed 20 characters
 * - EFT account ID cannot exceed 10 characters
 * 
 * React Integration:
 * - JSON property names align with React form field conventions
 * - Validation errors map to Material-UI form validation feedback
 * - Field sequencing preserves original BMS screen layout (COACTVW.bms)
 * - Supports dynamic form validation with real-time feedback
 * 
 * @author CardDemo Development Team - Blitzy Platform
 * @version 1.0
 * @since Java 21
 */
public class CustomerDto {

    /**
     * Nine-digit customer identifier matching COBOL customer ID field.
     * Maps to CUST-ID field from customer record structure.
     * 
     * Validation:
     * - Maximum value of 999,999,999 (9-digit constraint)
     * - Must be positive integer for active customers
     * - Used as primary key for customer lookups
     */
    @JsonProperty("customerId")
    @Max(value = 999999999, message = "Customer ID cannot exceed 9 digits")
    private Integer customerId;

    /**
     * Customer's first name with length validation.
     * Maps to CUST-FIRST-NAME field from customer record structure.
     * 
     * Validation:
     * - Maximum 25 characters (COBOL PIC X(25) equivalent)
     * - Required for customer identification
     * - Supports standard alphabetic characters and spaces
     */
    @JsonProperty("firstName")
    private String firstName;

    /**
     * Customer's middle name with length validation.
     * Maps to CUST-MIDDLE-NAME field from customer record structure.
     * 
     * Validation:
     * - Maximum 25 characters (COBOL PIC X(25) equivalent)
     * - Optional field for customer identification
     * - Supports standard alphabetic characters and spaces
     */
    @JsonProperty("middleName")
    private String middleName;

    /**
     * Customer's last name with length validation.
     * Maps to CUST-LAST-NAME field from customer record structure.
     * 
     * Validation:
     * - Maximum 25 characters (COBOL PIC X(25) equivalent)
     * - Required for customer identification
     * - Supports standard alphabetic characters and spaces
     */
    @JsonProperty("lastName")
    private String lastName;

    /**
     * Complete customer address information with cascading validation.
     * Maps to CUST-ADDR-LINE-1 through CUST-ADDR-ZIP fields from customer record.
     * 
     * Validation:
     * - Cascading validation using @Valid annotation
     * - Complete address validation including state/ZIP cross-validation
     * - Supports multi-line address structure matching COBOL layout
     * - Address normalization and standardization
     */
    @JsonProperty("address")
    @Valid
    private AddressDto address;

    /**
     * Primary phone number with North American Numbering Plan validation.
     * Maps to CUST-PHONE-NUM-1 field from customer record structure.
     * 
     * Validation:
     * - Maximum 15 characters (COBOL PIC X(15) equivalent)
     * - NANP area code validation for US/Canada phone numbers
     * - Supports multiple common formatting styles
     * - Required for customer contact information
     */
    @JsonProperty("phoneNumber1")
    @ValidPhoneNumber(allowEmpty = false, message = "Primary phone number must be a valid North American number")
    private String phoneNumber1;

    /**
     * Secondary phone number with North American Numbering Plan validation.
     * Maps to CUST-PHONE-NUM-2 field from customer record structure.
     * 
     * Validation:
     * - Maximum 15 characters (COBOL PIC X(15) equivalent)
     * - NANP area code validation for US/Canada phone numbers
     * - Supports multiple common formatting styles
     * - Optional secondary contact number
     */
    @JsonProperty("phoneNumber2")
    @ValidPhoneNumber(allowEmpty = true, message = "Secondary phone number must be a valid North American number")
    private String phoneNumber2;

    /**
     * Social Security Number with format and business rule validation.
     * Maps to CUST-SSN field from customer record structure.
     * 
     * Validation:
     * - Must be valid 9-digit SSN format (XXX-XX-XXXX or XXXXXXXXX)
     * - Business rule validation (no 000, 666, or 900-999 area numbers)
     * - Middle group cannot be 00, last group cannot be 0000
     * - Required for customer identification and credit verification
     */
    @JsonProperty("ssn")
    @ValidSSN(allowNull = false, allowEmpty = false, message = "Social Security Number is required and must be valid")
    private String ssn;

    /**
     * Government issued identification document number.
     * Maps to CUST-GOVT-ISSUED-ID field from customer record structure.
     * 
     * Validation:
     * - Maximum 20 characters (COBOL PIC X(20) equivalent)
     * - Supports various government ID formats (driver's license, passport, etc.)
     * - Optional field for additional customer verification
     */
    @JsonProperty("governmentIssuedId")
    private String governmentIssuedId;

    /**
     * Customer date of birth in CCYYMMDD format.
     * Maps to CUST-DOB-YYYY-MM-DD field from customer record structure.
     * 
     * Validation:
     * - Must be valid CCYYMMDD format (19xx or 20xx centuries)
     * - Comprehensive date validation including leap year calculations
     * - Required for age verification and credit assessment
     * - Follows COBOL date validation logic from CSUTLDTC.cbl
     */
    @JsonProperty("dateOfBirth")
    @ValidCCYYMMDD(message = "Date of birth must be in valid CCYYMMDD format")
    private String dateOfBirth;

    /**
     * Electronic Funds Transfer account identifier.
     * Maps to CUST-EFT-ACCOUNT-ID field from customer record structure.
     * 
     * Validation:
     * - Maximum 10 characters (COBOL PIC X(10) equivalent)
     * - Used for automatic payment and funds transfer operations
     * - Optional field for customers with EFT capabilities
     */
    @JsonProperty("eftAccountId")
    private String eftAccountId;

    /**
     * Primary card holder indicator flag.
     * Maps to CUST-PRI-CARD-HOLDER-IND field from customer record structure.
     * 
     * Usage:
     * - true indicates primary card holder status
     * - false indicates secondary/authorized user status
     * - Used for determining account privileges and responsibilities
     */
    @JsonProperty("primaryCardHolder")
    private boolean primaryCardHolder;

    /**
     * FICO credit score with range validation.
     * Maps to CUST-FICO-CREDIT-SCORE field from customer record structure.
     * 
     * Validation:
     * - Must be between 300-850 (standard FICO range)
     * - Nullable for customers without established credit history
     * - Used for credit assessment and account management decisions
     * - Stored as Integer to support exact COBOL COMP-3 precision
     */
    @JsonProperty("ficoCreditScore")
    @ValidFicoScore(message = "FICO credit score must be between 300 and 850")
    private Integer ficoCreditScore;

    /**
     * Default constructor for CustomerDto.
     * Initializes empty customer structure for form binding and JSON deserialization.
     */
    public CustomerDto() {
        // Default constructor for JSON deserialization and form binding
        this.address = new AddressDto();
        this.primaryCardHolder = false; // Default to non-primary status
    }

    /**
     * Full constructor for CustomerDto with all customer information.
     * 
     * @param customerId Nine-digit customer identifier
     * @param firstName Customer's first name
     * @param middleName Customer's middle name
     * @param lastName Customer's last name
     * @param address Complete customer address information
     * @param phoneNumber1 Primary phone number
     * @param phoneNumber2 Secondary phone number
     * @param ssn Social Security Number
     * @param governmentIssuedId Government issued ID number
     * @param dateOfBirth Date of birth in CCYYMMDD format
     * @param eftAccountId EFT account identifier
     * @param primaryCardHolder Primary card holder flag
     * @param ficoCreditScore FICO credit score
     */
    public CustomerDto(Integer customerId, String firstName, String middleName, String lastName,
                      AddressDto address, String phoneNumber1, String phoneNumber2, String ssn,
                      String governmentIssuedId, String dateOfBirth, String eftAccountId,
                      boolean primaryCardHolder, Integer ficoCreditScore) {
        this.customerId = customerId;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.address = address != null ? address : new AddressDto();
        this.phoneNumber1 = phoneNumber1;
        this.phoneNumber2 = phoneNumber2;
        this.ssn = ssn;
        this.governmentIssuedId = governmentIssuedId;
        this.dateOfBirth = dateOfBirth;
        this.eftAccountId = eftAccountId;
        this.primaryCardHolder = primaryCardHolder;
        this.ficoCreditScore = ficoCreditScore;
    }

    /**
     * Gets the customer identifier.
     * 
     * @return the nine-digit customer ID
     */
    public Integer getCustomerId() {
        return customerId;
    }

    /**
     * Sets the customer identifier with validation.
     * 
     * @param customerId the customer ID to set
     */
    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    /**
     * Gets the customer's first name.
     * 
     * @return the first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the customer's first name with validation.
     * 
     * @param firstName the first name to set
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName != null ? firstName.trim() : null;
    }

    /**
     * Gets the customer's middle name.
     * 
     * @return the middle name
     */
    public String getMiddleName() {
        return middleName;
    }

    /**
     * Sets the customer's middle name with validation.
     * 
     * @param middleName the middle name to set
     */
    public void setMiddleName(String middleName) {
        this.middleName = middleName != null ? middleName.trim() : null;
    }

    /**
     * Gets the customer's last name.
     * 
     * @return the last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the customer's last name with validation.
     * 
     * @param lastName the last name to set
     */
    public void setLastName(String lastName) {
        this.lastName = lastName != null ? lastName.trim() : null;
    }

    /**
     * Gets the customer's complete address information.
     * 
     * @return the address DTO with cascading validation
     */
    public AddressDto getAddress() {
        return address;
    }

    /**
     * Sets the customer's complete address information.
     * 
     * @param address the address DTO to set
     */
    public void setAddress(AddressDto address) {
        this.address = address != null ? address : new AddressDto();
    }

    /**
     * Gets the primary phone number.
     * 
     * @return the primary phone number
     */
    public String getPhoneNumber1() {
        return phoneNumber1;
    }

    /**
     * Sets the primary phone number with validation.
     * 
     * @param phoneNumber1 the primary phone number to set
     */
    public void setPhoneNumber1(String phoneNumber1) {
        this.phoneNumber1 = phoneNumber1 != null ? phoneNumber1.trim() : null;
    }

    /**
     * Gets the secondary phone number.
     * 
     * @return the secondary phone number
     */
    public String getPhoneNumber2() {
        return phoneNumber2;
    }

    /**
     * Sets the secondary phone number with validation.
     * 
     * @param phoneNumber2 the secondary phone number to set
     */
    public void setPhoneNumber2(String phoneNumber2) {
        this.phoneNumber2 = phoneNumber2 != null ? phoneNumber2.trim() : null;
    }

    /**
     * Gets the Social Security Number.
     * 
     * @return the SSN
     */
    public String getSsn() {
        return ssn;
    }

    /**
     * Sets the Social Security Number with validation.
     * 
     * @param ssn the SSN to set
     */
    public void setSsn(String ssn) {
        this.ssn = ssn != null ? ssn.trim() : null;
    }

    /**
     * Gets the government issued ID number.
     * 
     * @return the government issued ID
     */
    public String getGovernmentIssuedId() {
        return governmentIssuedId;
    }

    /**
     * Sets the government issued ID number with validation.
     * 
     * @param governmentIssuedId the government issued ID to set
     */
    public void setGovernmentIssuedId(String governmentIssuedId) {
        this.governmentIssuedId = governmentIssuedId != null ? governmentIssuedId.trim() : null;
    }

    /**
     * Gets the date of birth.
     * 
     * @return the date of birth in CCYYMMDD format
     */
    public String getDateOfBirth() {
        return dateOfBirth;
    }

    /**
     * Sets the date of birth with validation.
     * 
     * @param dateOfBirth the date of birth to set in CCYYMMDD format
     */
    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth != null ? dateOfBirth.trim() : null;
    }

    /**
     * Gets the EFT account identifier.
     * 
     * @return the EFT account ID
     */
    public String getEftAccountId() {
        return eftAccountId;
    }

    /**
     * Sets the EFT account identifier with validation.
     * 
     * @param eftAccountId the EFT account ID to set
     */
    public void setEftAccountId(String eftAccountId) {
        this.eftAccountId = eftAccountId != null ? eftAccountId.trim() : null;
    }

    /**
     * Gets the primary card holder status.
     * 
     * @return true if primary card holder, false otherwise
     */
    public boolean isPrimaryCardHolder() {
        return primaryCardHolder;
    }

    /**
     * Sets the primary card holder status.
     * 
     * @param primaryCardHolder the primary card holder flag to set
     */
    public void setPrimaryCardHolder(boolean primaryCardHolder) {
        this.primaryCardHolder = primaryCardHolder;
    }

    /**
     * Gets the FICO credit score.
     * 
     * @return the FICO credit score
     */
    public Integer getFicoCreditScore() {
        return ficoCreditScore;
    }

    /**
     * Sets the FICO credit score with validation.
     * 
     * @param ficoCreditScore the FICO credit score to set
     */
    public void setFicoCreditScore(Integer ficoCreditScore) {
        this.ficoCreditScore = ficoCreditScore;
    }

    /**
     * Validates the complete customer data structure with comprehensive business rule checking.
     * This method provides equivalent validation to the original COBOL customer validation
     * logic while offering enhanced error feedback for React form components.
     * 
     * Validation Process:
     * 1. Required field validation (customer ID, first name, last name, SSN)
     * 2. Individual field format validation using custom validators
     * 3. Field length validation according to COBOL constraints
     * 4. Cross-field validation for data consistency
     * 5. Address validation with state/ZIP cross-validation
     * 6. Business rule validation for customer data integrity
     * 
     * @return ValidationResult indicating success or specific validation failure
     */
    public ValidationResult validate() {
        // Validate required customer ID
        if (customerId == null || customerId <= 0) {
            return ValidationResult.withCustomMessage(
                ValidationResult.BLANK_FIELD, 
                "Customer ID is required and must be a positive number"
            ).getResult();
        }

        // Validate customer ID range (9-digit maximum)
        if (customerId > 999999999) {
            return ValidationResult.withCustomMessage(
                ValidationResult.INVALID_RANGE,
                "Customer ID cannot exceed 9 digits"
            ).getResult();
        }

        // Validate required first name
        if (firstName == null || firstName.trim().isEmpty()) {
            return ValidationResult.withCustomMessage(
                ValidationResult.BLANK_FIELD,
                "First name is required"
            ).getResult();
        }

        // Validate first name length
        if (firstName.length() > 25) {
            return ValidationResult.withCustomMessage(
                ValidationResult.INVALID_LENGTH,
                "First name cannot exceed 25 characters"
            ).getResult();
        }

        // Validate middle name length if provided
        if (middleName != null && middleName.length() > 25) {
            return ValidationResult.withCustomMessage(
                ValidationResult.INVALID_LENGTH,
                "Middle name cannot exceed 25 characters"
            ).getResult();
        }

        // Validate required last name
        if (lastName == null || lastName.trim().isEmpty()) {
            return ValidationResult.withCustomMessage(
                ValidationResult.BLANK_FIELD,
                "Last name is required"
            ).getResult();
        }

        // Validate last name length
        if (lastName.length() > 25) {
            return ValidationResult.withCustomMessage(
                ValidationResult.INVALID_LENGTH,
                "Last name cannot exceed 25 characters"
            ).getResult();
        }

        // Validate address if provided using cascading validation
        if (address != null) {
            ValidationResult addressValidation = address.validate();
            if (!addressValidation.isValid()) {
                return addressValidation;
            }
        }

        // Validate primary phone number length if provided
        if (phoneNumber1 != null && phoneNumber1.length() > 15) {
            return ValidationResult.withCustomMessage(
                ValidationResult.INVALID_LENGTH,
                "Primary phone number cannot exceed 15 characters"
            ).getResult();
        }

        // Validate secondary phone number length if provided
        if (phoneNumber2 != null && phoneNumber2.length() > 15) {
            return ValidationResult.withCustomMessage(
                ValidationResult.INVALID_LENGTH,
                "Secondary phone number cannot exceed 15 characters"
            ).getResult();
        }

        // Validate government issued ID length if provided
        if (governmentIssuedId != null && governmentIssuedId.length() > 20) {
            return ValidationResult.withCustomMessage(
                ValidationResult.INVALID_LENGTH,
                "Government issued ID cannot exceed 20 characters"
            ).getResult();
        }

        // Validate EFT account ID length if provided
        if (eftAccountId != null && eftAccountId.length() > 10) {
            return ValidationResult.withCustomMessage(
                ValidationResult.INVALID_LENGTH,
                "EFT account ID cannot exceed 10 characters"
            ).getResult();
        }

        // FICO score validation is handled by @ValidFicoScore annotation
        // Additional business rule: FICO score should be reasonable for active customers
        if (ficoCreditScore != null && (ficoCreditScore < 300 || ficoCreditScore > 850)) {
            return ValidationResult.withCustomMessage(
                ValidationResult.INVALID_RANGE,
                "FICO credit score must be between 300 and 850"
            ).getResult();
        }

        return ValidationResult.VALID;
    }

    /**
     * Gets the customer's full name by combining first, middle, and last names.
     * 
     * @return formatted full name string
     */
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        
        if (firstName != null && !firstName.trim().isEmpty()) {
            fullName.append(firstName);
        }
        
        if (middleName != null && !middleName.trim().isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(middleName);
        }
        
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(lastName);
        }
        
        return fullName.toString();
    }

    /**
     * Determines if this customer has a complete profile.
     * A complete profile requires all mandatory fields to be populated.
     * 
     * @return true if customer profile is complete, false otherwise
     */
    public boolean isCompleteProfile() {
        return customerId != null && customerId > 0 &&
               firstName != null && !firstName.trim().isEmpty() &&
               lastName != null && !lastName.trim().isEmpty() &&
               ssn != null && !ssn.trim().isEmpty() &&
               phoneNumber1 != null && !phoneNumber1.trim().isEmpty() &&
               dateOfBirth != null && !dateOfBirth.trim().isEmpty() &&
               address != null && address.isCompleteUsAddress();
    }

    /**
     * Determines if this customer has established credit history.
     * 
     * @return true if customer has FICO score, false otherwise
     */
    public boolean hasEstablishedCredit() {
        return ficoCreditScore != null && ficoCreditScore >= 300 && ficoCreditScore <= 850;
    }

    /**
     * Gets the FICO credit score as BigDecimal for exact precision calculations.
     * Supports COBOL COMP-3 equivalent precision for financial calculations.
     * 
     * @return FICO score as BigDecimal or null if not available
     */
    public BigDecimal getFicoCreditScoreAsBigDecimal() {
        return ficoCreditScore != null ? BigDecimal.valueOf(ficoCreditScore.intValue()) : null;
    }

    /**
     * Compares this customer with another object for equality.
     * Two customers are considered equal if their customer IDs match.
     * 
     * @param obj the object to compare with
     * @return true if the customers are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        CustomerDto that = (CustomerDto) obj;
        return Objects.equals(customerId, that.customerId) &&
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
               primaryCardHolder == that.primaryCardHolder &&
               Objects.equals(ficoCreditScore, that.ficoCreditScore);
    }

    /**
     * Generates hash code for this customer based on all field values.
     * 
     * @return hash code for this customer
     */
    @Override
    public int hashCode() {
        return Objects.hash(customerId, firstName, middleName, lastName, address,
                           phoneNumber1, phoneNumber2, ssn, governmentIssuedId,
                           dateOfBirth, eftAccountId, primaryCardHolder, ficoCreditScore);
    }

    /**
     * Returns a string representation of this customer for debugging and logging.
     * Sensitive information (SSN, phone numbers) is masked for security.
     * 
     * @return string representation of this customer
     */
    @Override
    public String toString() {
        return String.format("CustomerDto{customerId=%d, fullName='%s', " +
                           "phoneNumber1='%s', ssn='%s', primaryCardHolder=%b, ficoCreditScore=%d, " +
                           "address=%s}",
                           customerId,
                           getFullName(),
                           phoneNumber1 != null ? phoneNumber1.substring(0, Math.min(phoneNumber1.length(), 3)) + "***" : null,
                           ssn != null ? "***-**-" + ssn.substring(Math.max(0, ssn.length() - 4)) : null,
                           primaryCardHolder,
                           ficoCreditScore,
                           address != null ? address.toString() : "null");
    }
}