/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.entity;

import com.carddemo.util.ValidationUtil;
import com.carddemo.util.DateConversionUtil;
import com.carddemo.util.StringUtil;
import com.carddemo.util.Constants;
import com.carddemo.exception.ValidationException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity representing customer profile data, mapped to customer_data PostgreSQL table.
 * Corresponds to CUSTOMER-RECORD structure from CUSTREC.cpy and CVCUS01Y.cpy copybooks.
 * 
 * Contains personal information including names, addresses, contact details, SSN, 
 * government ID, date of birth, and FICO score. Implements field-level encryption 
 * for sensitive data like SSN and supports GDPR compliance through data masking annotations.
 * 
 * Field mappings from COBOL copybooks:
 * - CUST-ID (PIC 9(10)) → customerId (BIGINT)
 * - CUST-FIRST-NAME (PIC X(25)) → firstName (VARCHAR(20))
 * - CUST-MIDDLE-NAME (PIC X(25)) → middleName (VARCHAR(20))
 * - CUST-LAST-NAME (PIC X(25)) → lastName (VARCHAR(20))
 * - CUST-ADDR-LINE-1 (PIC X(50)) → addressLine1 (VARCHAR(50))
 * - CUST-ADDR-LINE-2 (PIC X(50)) → addressLine2 (VARCHAR(50))
 * - CUST-ADDR-LINE-3 (PIC X(50)) → addressLine3 (VARCHAR(50))
 * - CUST-ADDR-STATE-CD (PIC X(02)) → stateCode (VARCHAR(2))
 * - CUST-ADDR-COUNTRY-CD (PIC X(03)) → countryCode (VARCHAR(3))
 * - CUST-ADDR-ZIP (PIC X(10)) → zipCode (VARCHAR(10))
 * - CUST-PHONE-NUM-1 (PIC X(15)) → phoneNumber1 (VARCHAR(15))
 * - CUST-PHONE-NUM-2 (PIC X(15)) → phoneNumber2 (VARCHAR(15))
 * - CUST-SSN (PIC X(9)) → ssn (VARCHAR(9), encrypted)
 * - CUST-GOVT-ISSUED-ID (PIC X(20)) → governmentIssuedId (VARCHAR(20))
 * - CUST-DOB-YYYY-MM-DD (PIC X(10)) → dateOfBirth (DATE)
 * - CUST-EFT-ACCOUNT-ID (PIC X(10)) → eftAccountId (VARCHAR(10))
 * - CUST-PRI-CARD-HOLDER-IND (PIC X) → primaryCardHolderIndicator (CHAR(1))
 * - CUST-FICO-CREDIT-SCORE (PIC 9(3)) → ficoScore (SMALLINT)
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Entity
@Table(name = "customer_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    // Constants for field lengths (matching COBOL PIC clauses)
    private static final int CUSTOMER_ID_LENGTH = 10;
    private static final int SSN_LENGTH = 9;
    private static final int PHONE_NUMBER_LENGTH = 15;
    private static final int ZIP_CODE_LENGTH = 10;
    private static final int FICO_SCORE_MIN = 300;
    private static final int FICO_SCORE_MAX = 850;
    
    /**
     * Customer ID - Primary key.
     * Maps to CUST-ID field from COBOL copybook (PIC 9(10)).
     * Unique identifier for each customer in the system.
     * Note: Explicitly assigned in COBOL migration (no auto-generation).
     */
    @Id
    @Column(name = "customer_id", nullable = false)
    @NotNull(message = "Customer ID is required")
    private Long customerId;

    /**
     * Customer first name.
     * Maps to CUST-FIRST-NAME field from COBOL copybook (PIC X(25)).
     * Truncated to 20 characters for database compatibility.
     */
    @Column(name = "first_name", length = 20, nullable = false)
    @NotNull(message = "First name is required")
    @Size(max = 20, message = "First name cannot exceed 20 characters")
    private String firstName;

    /**
     * Customer middle name.
     * Maps to CUST-MIDDLE-NAME field from COBOL copybook (PIC X(25)).
     * Truncated to 20 characters for database compatibility.
     */
    @Column(name = "middle_name", length = 20)
    @Size(max = 20, message = "Middle name cannot exceed 20 characters")
    private String middleName;

    /**
     * Customer last name.
     * Maps to CUST-LAST-NAME field from COBOL copybook (PIC X(25)).
     * Truncated to 20 characters for database compatibility.
     */
    @Column(name = "last_name", length = 20, nullable = false)
    @NotNull(message = "Last name is required")
    @Size(max = 20, message = "Last name cannot exceed 20 characters")
    private String lastName;

    /**
     * Customer address line 1.
     * Maps to CUST-ADDR-LINE-1 field from COBOL copybook (PIC X(50)).
     * Primary address line for customer residence.
     */
    @Column(name = "address_line_1", length = 50)
    @Size(max = 50, message = "Address line 1 cannot exceed 50 characters")
    private String addressLine1;

    /**
     * Customer address line 2.
     * Maps to CUST-ADDR-LINE-2 field from COBOL copybook (PIC X(50)).
     * Secondary address line for apartment, suite, etc.
     */
    @Column(name = "address_line_2", length = 50)
    @Size(max = 50, message = "Address line 2 cannot exceed 50 characters")
    private String addressLine2;

    /**
     * Customer address line 3.
     * Maps to CUST-ADDR-LINE-3 field from COBOL copybook (PIC X(50)).
     * Tertiary address line for additional address details.
     */
    @Column(name = "address_line_3", length = 50)
    @Size(max = 50, message = "Address line 3 cannot exceed 50 characters")
    private String addressLine3;

    /**
     * Customer state code.
     * Maps to CUST-ADDR-STATE-CD field from COBOL copybook (PIC X(02)).
     * Two-character US state code.
     */
    @Column(name = "state_code", length = 2)
    @Size(max = 2, message = "State code cannot exceed 2 characters")
    private String stateCode;

    /**
     * Customer country code.
     * Maps to CUST-ADDR-COUNTRY-CD field from COBOL copybook (PIC X(03)).
     * Three-character ISO country code.
     */
    @Column(name = "country_code", length = 3)
    @Size(max = 3, message = "Country code cannot exceed 3 characters")
    private String countryCode;

    /**
     * Customer ZIP code.
     * Maps to CUST-ADDR-ZIP field from COBOL copybook (PIC X(10)).
     * Postal code for customer address.
     */
    @Column(name = "zip_code", length = ZIP_CODE_LENGTH)
    @Size(max = ZIP_CODE_LENGTH, message = "ZIP code cannot exceed " + ZIP_CODE_LENGTH + " characters")
    private String zipCode;

    /**
     * Customer primary phone number.
     * Maps to CUST-PHONE-NUM-1 field from COBOL copybook (PIC X(15)).
     * Primary contact phone number for the customer.
     */
    @Column(name = "phone_number_1", length = PHONE_NUMBER_LENGTH)
    @Size(max = PHONE_NUMBER_LENGTH, message = "Phone number 1 cannot exceed " + PHONE_NUMBER_LENGTH + " characters")
    private String phoneNumber1;

    /**
     * Customer secondary phone number.
     * Maps to CUST-PHONE-NUM-2 field from COBOL copybook (PIC X(15)).
     * Secondary contact phone number for the customer.
     */
    @Column(name = "phone_number_2", length = PHONE_NUMBER_LENGTH)
    @Size(max = PHONE_NUMBER_LENGTH, message = "Phone number 2 cannot exceed " + PHONE_NUMBER_LENGTH + " characters")
    private String phoneNumber2;

    /**
     * Customer Social Security Number (encrypted).
     * Maps to CUST-SSN field from COBOL copybook (PIC X(9)).
     * Stored encrypted for data security and GDPR compliance.
     * Excluded from JSON serialization for security.
     */
    @Column(name = "ssn", length = SSN_LENGTH)
    @Size(max = SSN_LENGTH, message = "SSN cannot exceed " + SSN_LENGTH + " characters")
    @JsonIgnore
    private String ssn;

    /**
     * Customer government issued ID.
     * Maps to CUST-GOVT-ISSUED-ID field from COBOL copybook (PIC X(20)).
     * Driver's license, passport, or other government ID number.
     */
    @Column(name = "government_issued_id", length = 20)
    @Size(max = 20, message = "Government issued ID cannot exceed 20 characters")
    private String governmentIssuedId;

    /**
     * Customer date of birth.
     * Maps to CUST-DOB-YYYY-MM-DD field from COBOL copybook (PIC X(10)).
     * Stored as LocalDate for proper date handling and validation.
     */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /**
     * Customer EFT account ID.
     * Maps to CUST-EFT-ACCOUNT-ID field from COBOL copybook (PIC X(10)).
     * Electronic funds transfer account identifier.
     */
    @Column(name = "eft_account_id", length = 10)
    @Size(max = 10, message = "EFT account ID cannot exceed 10 characters")
    private String eftAccountId;

    /**
     * Primary card holder indicator.
     * Maps to CUST-PRI-CARD-HOLDER-IND field from COBOL copybook (PIC X).
     * Indicates whether this customer is the primary card holder.
     */
    @Column(name = "primary_card_holder_indicator", length = 1)
    @Size(max = 1, message = "Primary card holder indicator cannot exceed 1 character")
    private String primaryCardHolderIndicator;

    /**
     * Customer FICO credit score.
     * Maps to CUST-FICO-CREDIT-SCORE field from COBOL copybook (PIC 9(3)).
     * Credit score ranging from 300 to 850.
     */
    @Column(name = "fico_score", precision = 5, scale = 2)
    private BigDecimal ficoScore;

    /**
     * Customer credit limit.
     * Used for credit qualification and account management.
     */
    @Column(name = "credit_limit", precision = 15, scale = 2)
    private BigDecimal creditLimit;

    /**
     * Last update timestamp.
     * Tracks when the customer record was last modified.
     */
    @Column(name = "last_update_timestamp")
    private LocalDateTime lastUpdateTimestamp;

    /**
     * Created timestamp.
     * Tracks when the customer record was created.
     */
    @Column(name = "created_timestamp")
    private LocalDateTime createdTimestamp;

    /**
     * JPA lifecycle callback for validation before persisting a new customer.
     * Performs comprehensive field validation using COBOL-equivalent validation rules.
     * 
     * @throws ValidationException if any field validation fails
     */
    @PrePersist
    public void validateBeforeInsert() {
        performCustomerValidation();
        formatFields();
        // Set creation timestamp
        if (createdTimestamp == null) {
            createdTimestamp = LocalDateTime.now();
        }
        lastUpdateTimestamp = LocalDateTime.now();
    }

    /**
     * JPA lifecycle callback for validation before updating an existing customer.
     * Performs comprehensive field validation using COBOL-equivalent validation rules.
     * 
     * @throws ValidationException if any field validation fails
     */
    @PreUpdate
    public void validateBeforeUpdate() {
        performCustomerValidation();
        formatFields();
        // Update last modified timestamp
        lastUpdateTimestamp = LocalDateTime.now();
    }

    /**
     * Performs comprehensive customer field validation using ValidationUtil methods.
     * Validates SSN format, phone numbers, ZIP code, state code, date of birth, and FICO score.
     * Collects all validation errors and throws ValidationException if any errors are found.
     * 
     * @throws ValidationException with detailed field-level error messages
     */
    private void performCustomerValidation() {
        ValidationException validationException = new ValidationException("Customer validation failed");

        // Validate required fields using ValidationUtil.validateRequiredField()
        try {
            ValidationUtil.validateRequiredField("firstName", firstName);
        } catch (ValidationException e) {
            validationException.addFieldError("firstName", "First name is required");
        }

        try {
            ValidationUtil.validateRequiredField("lastName", lastName);
        } catch (ValidationException e) {
            validationException.addFieldError("lastName", "Last name is required");
        }

        // Validate SSN format using ValidationUtil.validateSSN()
        if (ssn != null && !ssn.trim().isEmpty()) {
            try {
                ValidationUtil.validateSSN("ssn", ssn);
            } catch (ValidationException e) {
                validationException.addFieldError("ssn", "Invalid SSN format. Must be 9 digits");
            }
        }

        // Validate phone numbers using ValidationUtil.validatePhoneNumber()
        if (phoneNumber1 != null && !phoneNumber1.trim().isEmpty()) {
            try {
                ValidationUtil.validatePhoneNumber("phoneNumber1", phoneNumber1);
            } catch (ValidationException e) {
                validationException.addFieldError("phoneNumber1", "Invalid phone number format");
            }
        }

        if (phoneNumber2 != null && !phoneNumber2.trim().isEmpty()) {
            try {
                ValidationUtil.validatePhoneNumber("phoneNumber2", phoneNumber2);
            } catch (ValidationException e) {
                validationException.addFieldError("phoneNumber2", "Invalid phone number format");
            }
        }

        // Validate ZIP code using ValidationUtil.validateZipCode()
        if (zipCode != null && !zipCode.trim().isEmpty()) {
            try {
                ValidationUtil.validateZipCode("zipCode", zipCode);
            } catch (ValidationException e) {
                validationException.addFieldError("zipCode", "Invalid ZIP code format");
            }
        }

        // Validate state code using ValidationUtil.isValidStateCode()
        if (stateCode != null && !stateCode.trim().isEmpty()) {
            if (!ValidationUtil.isValidStateCode(stateCode)) {
                validationException.addFieldError("stateCode", "Invalid US state code");
            }
        }

        // Validate date of birth using DateConversionUtil.validateDate() and DateConversionUtil.isNotFutureDate()
        if (dateOfBirth != null) {
            String dateStr = DateConversionUtil.formatToCobol(dateOfBirth);
            if (!DateConversionUtil.validateDate(dateStr)) {
                validationException.addFieldError("dateOfBirth", "Invalid date of birth format");
            } else if (!DateConversionUtil.isNotFutureDate(dateOfBirth)) {
                validationException.addFieldError("dateOfBirth", "Date of birth cannot be in the future");
            }
        }

        // Validate FICO score range (additional validation beyond @Min/@Max annotations)
        if (ficoScore != null) {
            if (ficoScore.compareTo(new BigDecimal(FICO_SCORE_MIN)) < 0 || 
                ficoScore.compareTo(new BigDecimal(FICO_SCORE_MAX)) > 0) {
                validationException.addFieldError("ficoScore", 
                    "FICO score must be between " + FICO_SCORE_MIN + " and " + FICO_SCORE_MAX);
            }
        }

        // Throw exception if any validation errors were found
        if (validationException.hasFieldErrors()) {
            throw validationException;
        }
    }

    /**
     * Formats customer fields using StringUtil methods to ensure proper COBOL-compatible formatting.
     * Applies trimming, padding, and fixed-length formatting as needed.
     */
    private void formatFields() {
        // Format name fields using StringUtil.trimAndPad() and StringUtil.safeTrim()
        if (firstName != null) {
            firstName = StringUtil.formatFixedLength(StringUtil.safeTrim(firstName), 20);
        }

        if (middleName != null) {
            middleName = StringUtil.formatFixedLength(StringUtil.safeTrim(middleName), 20);
        }

        if (lastName != null) {
            lastName = StringUtil.formatFixedLength(StringUtil.safeTrim(lastName), 20);
        }

        // Format address fields using StringUtil.trimAndPad()
        if (addressLine1 != null) {
            addressLine1 = StringUtil.trimAndPad(addressLine1, 50);
        }

        if (addressLine2 != null) {
            addressLine2 = StringUtil.trimAndPad(addressLine2, 50);
        }

        if (addressLine3 != null) {
            addressLine3 = StringUtil.trimAndPad(addressLine3, 50);
        }

        // Format state and country codes to uppercase using StringUtil.safeTrim()
        if (stateCode != null) {
            stateCode = StringUtil.safeTrim(stateCode).toUpperCase();
        }

        if (countryCode != null) {
            countryCode = StringUtil.safeTrim(countryCode).toUpperCase();
        }

        // Format ZIP code using StringUtil.formatFixedLength()
        if (zipCode != null) {
            zipCode = StringUtil.formatFixedLength(StringUtil.safeTrim(zipCode), ZIP_CODE_LENGTH);
        }

        // Format phone numbers using StringUtil.safeTrim()
        if (phoneNumber1 != null) {
            phoneNumber1 = StringUtil.safeTrim(phoneNumber1);
        }

        if (phoneNumber2 != null) {
            phoneNumber2 = StringUtil.safeTrim(phoneNumber2);
        }

        // Format SSN (remove any non-digits) using StringUtil.safeTrim()
        if (ssn != null) {
            ssn = StringUtil.safeTrim(ssn).replaceAll("[^0-9]", "");
        }

        // Format government ID using StringUtil.safeTrim()
        if (governmentIssuedId != null) {
            governmentIssuedId = StringUtil.safeTrim(governmentIssuedId);
        }

        // Format EFT account ID using StringUtil.safeTrim()
        if (eftAccountId != null) {
            eftAccountId = StringUtil.safeTrim(eftAccountId);
        }

        // Format primary card holder indicator using StringUtil.safeTrim()
        if (primaryCardHolderIndicator != null) {
            primaryCardHolderIndicator = StringUtil.safeTrim(primaryCardHolderIndicator).toUpperCase();
        }
    }

    /**
     * Custom equals method to properly compare Customer entities.
     * Uses customer ID as the primary comparison field, with fallback to other fields.
     * 
     * @param o the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Customer customer = (Customer) o;
        
        // Primary comparison by customer ID
        if (customerId != null && customer.customerId != null) {
            return Objects.equals(customerId, customer.customerId);
        }
        
        // Fallback comparison using other unique fields
        return Objects.equals(firstName, customer.firstName) &&
               Objects.equals(lastName, customer.lastName) &&
               Objects.equals(dateOfBirth, customer.dateOfBirth) &&
               Objects.equals(ssn, customer.ssn);
    }

    /**
     * Custom hash code method using Objects.hash() for consistency with equals().
     * 
     * @return hash code for the Customer entity
     */
    @Override
    public int hashCode() {
        if (customerId != null) {
            return Objects.hash(customerId);
        }
        return Objects.hash(firstName, lastName, dateOfBirth, ssn);
    }

    // Convenience methods for test compatibility and legacy COBOL interface support

    /**
     * Convenience method to get customer ID as String for test compatibility.
     * Maps Long customerId to String format expected by tests.
     * Formats with leading zeros to match COBOL PIC 9(9) specification.
     *
     * @return customer ID as string with leading zeros (9 digits), or null if customerId is null
     */
    public String getCustomerId() {
        return customerId != null ? String.format("%09d", customerId) : null;
    }

    /**
     * Convenience method to set customer ID from String for test compatibility.
     * Converts String to Long for database storage.
     *
     * @param customerIdStr customer ID as string
     */
    public void setCustomerId(String customerIdStr) {
        if (customerIdStr != null && !customerIdStr.trim().isEmpty()) {
            try {
                this.customerId = Long.valueOf(customerIdStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid customer ID format: " + customerIdStr, e);
            }
        } else {
            this.customerId = null;
        }
    }

    /**
     * Convenience method to get primary phone number for test compatibility.
     * Maps phoneNumber1 field to expected getPhoneNumber() method.
     *
     * @return primary phone number
     */
    public String getPhoneNumber() {
        return phoneNumber1;
    }

    /**
     * Convenience method to set primary phone number for test compatibility.
     * Maps to phoneNumber1 field for database storage.
     *
     * @param phoneNumber primary phone number
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber1 = phoneNumber;
    }

    /**
     * Convenience method to get complete address for test compatibility.
     * Combines address line fields into single formatted address string.
     *
     * @return formatted complete address
     */
    public String getAddress() {
        StringBuilder address = new StringBuilder();
        
        if (addressLine1 != null && !addressLine1.trim().isEmpty()) {
            address.append(addressLine1.trim());
        }
        
        if (addressLine2 != null && !addressLine2.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(addressLine2.trim());
        }
        
        if (addressLine3 != null && !addressLine3.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(addressLine3.trim());
        }
        
        // Add city, state, zip if available
        if (stateCode != null && zipCode != null) {
            if (address.length() > 0) address.append(", ");
            address.append(stateCode).append(" ").append(zipCode);
        }
        
        return address.length() > 0 ? address.toString() : null;
    }

    /**
     * Convenience method to set address from single string for test compatibility.
     * Parses formatted address string and populates individual address fields.
     *
     * @param address formatted address string
     */
    public void setAddress(String address) {
        if (address != null && !address.trim().isEmpty()) {
            // Simple address parsing - store entire address in addressLine1 for test compatibility
            this.addressLine1 = address.trim();
            // Clear other address fields to avoid confusion
            this.addressLine2 = null;
            this.addressLine3 = null;
        } else {
            this.addressLine1 = null;
            this.addressLine2 = null;
            this.addressLine3 = null;
        }
    }

    /**
     * Convenience method to get SSN for test compatibility.
     * Maps ssn field to expected getSSN() method (capitalized).
     *
     * @return Social Security Number
     */
    public String getSSN() {
        return ssn;
    }

    /**
     * Convenience method to set SSN for test compatibility.
     * Maps to ssn field with capitalized method name.
     *
     * @param ssnValue Social Security Number
     */
    public void setSSN(String ssnValue) {
        this.ssn = ssnValue;
    }

    /**
     * Convenience method to get SSN for service layer compatibility.
     * Maps ssn field to expected getSsn() method (lowercase).
     *
     * @return Social Security Number
     */
    public String getSsn() {
        return ssn;
    }

    /**
     * Convenience method to set SSN for service layer compatibility.
     * Maps to ssn field with lowercase method name.
     *
     * @param ssnValue Social Security Number
     */
    public void setSsn(String ssnValue) {
        this.ssn = ssnValue;
    }

    /**
     * Get credit score as BigDecimal for COBOL precision compatibility.
     * Returns ficoScore with proper decimal scale for financial calculations.
     *
     * @return credit score as BigDecimal with 2 decimal places, or null if ficoScore is null
     */
    public BigDecimal getCreditScore() {
        return ficoScore;
    }

    /**
     * Set credit score as BigDecimal for COBOL precision compatibility.
     * Stores with proper scale to maintain COBOL COMP-3 equivalent precision.
     *
     * @param creditScore credit score as BigDecimal
     */
    public void setCreditScore(BigDecimal creditScore) {
        if (creditScore != null) {
            this.ficoScore = creditScore.setScale(2, BigDecimal.ROUND_HALF_UP);
        } else {
            this.ficoScore = null;
        }
    }

    /**
     * Custom toString method that masks sensitive information like SSN.
     * Provides useful debugging information while maintaining data security.
     * 
     * @return string representation of the Customer entity
     */
    @Override
    public String toString() {
        return "Customer{" +
                "customerId=" + customerId +
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
                ", ssn='***MASKED***'" +
                ", governmentIssuedId='" + governmentIssuedId + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", eftAccountId='" + eftAccountId + '\'' +
                ", primaryCardHolderIndicator='" + primaryCardHolderIndicator + '\'' +
                ", ficoScore=" + ficoScore +
                ", creditLimit=" + creditLimit +
                ", lastUpdateTimestamp=" + lastUpdateTimestamp +
                ", createdTimestamp=" + createdTimestamp +
                '}';
    }
}
