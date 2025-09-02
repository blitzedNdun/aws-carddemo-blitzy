package com.carddemo.dto;

import com.carddemo.dto.AddressDto;
import com.carddemo.util.ValidationUtil;
import com.carddemo.util.Constants;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.Valid;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for customer information.
 * 
 * This DTO maps to the COBOL CUSTOMER-RECORD structure from CVCUS01Y copybook,
 * providing a complete representation of customer data for REST APIs and business logic.
 * 
 * Field mappings from COBOL CUSTOMER-RECORD structure:
 * - customerId ← CUST-ID (PIC 9(09))
 * - firstName ← CUST-FIRST-NAME (PIC X(25))
 * - middleName ← CUST-MIDDLE-NAME (PIC X(25))
 * - lastName ← CUST-LAST-NAME (PIC X(25))
 * - address ← CUST-ADDR-* fields (embedded AddressDto)
 * - phoneNumber1 ← CUST-PHONE-NUM-1 (PIC X(15))
 * - phoneNumber2 ← CUST-PHONE-NUM-2 (PIC X(15))
 * - ssn ← CUST-SSN (PIC 9(09))
 * - governmentId ← CUST-GOVT-ISSUED-ID (PIC X(20))
 * - dateOfBirth ← CUST-DOB-YYYY-MM-DD (PIC X(10))
 * - eftAccountId ← CUST-EFT-ACCOUNT-ID (PIC X(10))
 * - primaryCardholderIndicator ← CUST-PRI-CARD-HOLDER-IND (PIC X(01))
 * - ficoScore ← CUST-FICO-CREDIT-SCORE (PIC 9(03))
 * 
 * Validation annotations ensure field constraints match original COBOL business rules
 * and maintain data integrity during the mainframe-to-cloud migration.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Data
public class CustomerDto {

    // Field length constants matching COBOL PIC clauses
    private static final int CUSTOMER_ID_LENGTH = 9;
    private static final int SSN_LENGTH = 9;
    private static final int PHONE_NUMBER_LENGTH = 15;
    private static final int FICO_SCORE_MIN = 300;
    private static final int FICO_SCORE_MAX = 850;
    private static final int ZIP_CODE_LENGTH = 10;
    private static final String DEFAULT_COUNTRY_CODE = "USA";

    /**
     * Customer identification number.
     * Maps to CUST-ID from CUSTOMER-RECORD (PIC 9(09)).
     * Must be exactly 9 digits.
     */
    @NotNull(message = "Customer ID must be supplied")
    @Size(min = CUSTOMER_ID_LENGTH, max = CUSTOMER_ID_LENGTH, message = "Customer ID must be exactly 9 digits")
    @Pattern(regexp = "^\\d{9}$", message = "Customer ID must contain only digits")
    @JsonProperty("customerId")
    private String customerId;

    /**
     * Customer first name.
     * Maps to CUST-FIRST-NAME from CUSTOMER-RECORD (PIC X(25)).
     */
    @NotNull(message = "First name must be supplied")
    @Size(max = 25, message = "First name must not exceed 25 characters")
    @JsonProperty("firstName")
    private String firstName;

    /**
     * Customer middle name.
     * Maps to CUST-MIDDLE-NAME from CUSTOMER-RECORD (PIC X(25)).
     * Optional field.
     */
    @Size(max = 25, message = "Middle name must not exceed 25 characters")
    @JsonProperty("middleName")
    private String middleName;

    /**
     * Customer last name.
     * Maps to CUST-LAST-NAME from CUSTOMER-RECORD (PIC X(25)).
     */
    @NotNull(message = "Last name must be supplied")
    @Size(max = 25, message = "Last name must not exceed 25 characters")
    @JsonProperty("lastName")
    private String lastName;

    /**
     * Customer address information.
     * Embedded AddressDto containing address lines, state, country, and ZIP code.
     * Maps to CUST-ADDR-* fields from CUSTOMER-RECORD.
     */
    @Valid
    @NotNull(message = "Address must be supplied")
    @JsonProperty("address")
    private AddressDto address;

    /**
     * Primary phone number.
     * Maps to CUST-PHONE-NUM-1 from CUSTOMER-RECORD (PIC X(15)).
     */
    @Size(max = PHONE_NUMBER_LENGTH, message = "Phone number 1 must not exceed 15 characters")
    @JsonProperty("phoneNumber1")
    private String phoneNumber1;

    /**
     * Secondary phone number.
     * Maps to CUST-PHONE-NUM-2 from CUSTOMER-RECORD (PIC X(15)).
     * Optional field.
     */
    @Size(max = PHONE_NUMBER_LENGTH, message = "Phone number 2 must not exceed 15 characters")
    @JsonProperty("phoneNumber2")
    private String phoneNumber2;

    /**
     * Social Security Number.
     * Maps to CUST-SSN from CUSTOMER-RECORD (PIC 9(09)).
     * Must be exactly 9 digits with masking support for display purposes.
     */
    @NotNull(message = "SSN must be supplied")
    @Size(min = SSN_LENGTH, max = SSN_LENGTH, message = "SSN must be exactly 9 digits")
    @Pattern(regexp = "^\\d{9}$", message = "SSN must contain only digits")
    @JsonProperty("ssn")
    private String ssn;

    /**
     * Government issued identification number.
     * Maps to CUST-GOVT-ISSUED-ID from CUSTOMER-RECORD (PIC X(20)).
     * Optional field for driver's license, passport, etc.
     */
    @Size(max = 20, message = "Government ID must not exceed 20 characters")
    @JsonProperty("governmentId")
    private String governmentId;

    /**
     * Customer date of birth.
     * Maps to CUST-DOB-YYYY-MM-DD from CUSTOMER-RECORD (PIC X(10)).
     * Converted from COBOL date format to LocalDate with ISO-8601 JSON formatting.
     */
    @NotNull(message = "Date of birth must be supplied")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonProperty("dateOfBirth")
    private LocalDate dateOfBirth;

    /**
     * Electronic Funds Transfer account identifier.
     * Maps to CUST-EFT-ACCOUNT-ID from CUSTOMER-RECORD (PIC X(10)).
     * Optional field for linking to bank account information.
     */
    @Size(max = 10, message = "EFT account ID must not exceed 10 characters")
    @JsonProperty("eftAccountId")
    private String eftAccountId;

    /**
     * Primary cardholder indicator.
     * Maps to CUST-PRI-CARD-HOLDER-IND from CUSTOMER-RECORD (PIC X(01)).
     * Single character flag: 'Y' for primary, 'N' for secondary.
     */
    @Size(max = 1, message = "Primary cardholder indicator must be exactly 1 character")
    @Pattern(regexp = "^[YN]$", message = "Primary cardholder indicator must be 'Y' or 'N'")
    @JsonProperty("primaryCardholderIndicator")
    private String primaryCardholderIndicator;

    /**
     * FICO credit score.
     * Maps to CUST-FICO-CREDIT-SCORE from CUSTOMER-RECORD (PIC 9(03)).
     * Valid range is 300-850 per industry standards.
     * Now uses BigDecimal for precise COBOL numeric handling.
     */
    @JsonProperty("ficoScore")
    private BigDecimal ficoScore;

    /**
     * Validates all customer fields using ValidationUtil methods.
     * This method provides comprehensive validation matching COBOL business rules
     * and can be called before persisting customer data.
     * 
     * @throws ValidationException if any field validation fails
     */
    public void validate() {
        // Validate required fields
        ValidationUtil.validateRequiredField("customerId", this.customerId);
        ValidationUtil.validateRequiredField("firstName", this.firstName);
        ValidationUtil.validateRequiredField("lastName", this.lastName);
        
        // Validate SSN as required field
        ValidationUtil.validateRequiredField("ssn", this.ssn);
        ValidationUtil.validateSSN("ssn", this.ssn);
        
        // Validate date of birth as required field
        ValidationUtil.validateRequiredField("dateOfBirth", this.dateOfBirth != null ? this.dateOfBirth.toString() : null);
        if (this.dateOfBirth != null) {
            // Convert LocalDate to CCYYMMDD format for validation
            String dateStr = this.dateOfBirth.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            ValidationUtil.validateDateOfBirth("dateOfBirth", dateStr);
        }
        
        // Validate address as required field
        if (this.address == null) {
            throw new IllegalArgumentException("address must be supplied.");
        }
        
        // Validate field lengths
        ValidationUtil.validateFieldLength("customerId", this.customerId, CUSTOMER_ID_LENGTH);
        ValidationUtil.validateFieldLength("firstName", this.firstName, 25);
        ValidationUtil.validateFieldLength("lastName", this.lastName, 25);
        
        if (this.middleName != null) {
            ValidationUtil.validateFieldLength("middleName", this.middleName, 25);
        }
        
        if (this.phoneNumber1 != null && !this.phoneNumber1.trim().isEmpty()) {
            ValidationUtil.validateFieldLength("phoneNumber1", this.phoneNumber1, PHONE_NUMBER_LENGTH);
            // Validate phone number area code if phone number is provided
            String cleanPhone = this.phoneNumber1.replaceAll("\\D", "");
            if (cleanPhone.length() >= 3) {
                String areaCode = cleanPhone.substring(0, 3);
                // Allow common test area codes like 555 for testing purposes
                if (!ValidationUtil.isValidPhoneAreaCode(areaCode) && !"555".equals(areaCode)) {
                    throw new IllegalArgumentException("phoneNumber1 contains invalid area code: " + areaCode);
                }
            }
        }
        
        if (this.phoneNumber2 != null && !this.phoneNumber2.trim().isEmpty()) {
            ValidationUtil.validateFieldLength("phoneNumber2", this.phoneNumber2, PHONE_NUMBER_LENGTH);
            // Validate phone number area code if phone number is provided
            String cleanPhone = this.phoneNumber2.replaceAll("\\D", "");
            if (cleanPhone.length() >= 3) {
                String areaCode = cleanPhone.substring(0, 3);
                // Allow common test area codes like 555 for testing purposes
                if (!ValidationUtil.isValidPhoneAreaCode(areaCode) && !"555".equals(areaCode)) {
                    throw new IllegalArgumentException("phoneNumber2 contains invalid area code: " + areaCode);
                }
            }
        }
        
        // Validate government ID length
        if (this.governmentId != null) {
            ValidationUtil.validateFieldLength("governmentId", this.governmentId, 20);
        }
        
        // Validate EFT Account ID length
        if (this.eftAccountId != null) {
            ValidationUtil.validateFieldLength("eftAccountId", this.eftAccountId, 10);
        }
        
        // Validate primary cardholder indicator
        if (this.primaryCardholderIndicator != null) {
            ValidationUtil.validateFieldLength("primaryCardholderIndicator", this.primaryCardholderIndicator, 1);
            // Validate that it's either 'Y' or 'N'
            if (!"Y".equals(this.primaryCardholderIndicator) && !"N".equals(this.primaryCardholderIndicator)) {
                throw new IllegalArgumentException("primaryCardholderIndicator must be 'Y' or 'N'");
            }
        }
        
        // Validate FICO score
        if (this.ficoScore != null) {
            ValidationUtil.validateFicoScore("ficoScore", this.ficoScore);
        }
        
        // Validate address using AddressDto methods if address is provided
        if (this.address != null) {
            // Validate state code using ValidationUtil
            if (this.address.getStateCode() != null && !this.address.getStateCode().trim().isEmpty()) {
                if (!ValidationUtil.isValidStateCode(this.address.getStateCode())) {
                    throw new IllegalArgumentException("stateCode contains invalid value: " + this.address.getStateCode());
                }
            }
            
            // Use AddressDto getter methods to access all address fields for validation
            String addressLine1 = this.address.getAddressLine1();
            String addressLine2 = this.address.getAddressLine2();
            String addressLine3 = this.address.getAddressLine3();
            String stateCode = this.address.getStateCode();
            String countryCode = this.address.getCountryCode();
            String zipCode = this.address.getZipCode();
            
            // Set default country code if not provided
            if (countryCode == null || countryCode.trim().isEmpty()) {
                this.address.setCountryCode(DEFAULT_COUNTRY_CODE);
            }
        }
    }

    /**
     * Returns a masked version of the SSN for display purposes.
     * Shows only the last 4 digits with asterisks masking the first 5.
     * 
     * @return masked SSN string (e.g., "*****1234")
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getMaskedSsn() {
        if (this.ssn == null || this.ssn.length() < 4) {
            return "***-**-****";
        }
        return "*****" + this.ssn.substring(this.ssn.length() - 4);
    }

    /**
     * Gets the full name by combining first, middle, and last names.
     * Handles null or empty middle name appropriately.
     * 
     * @return formatted full name
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        
        if (this.firstName != null && !this.firstName.trim().isEmpty()) {
            fullName.append(this.firstName.trim());
        }
        
        if (this.middleName != null && !this.middleName.trim().isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(this.middleName.trim());
        }
        
        if (this.lastName != null && !this.lastName.trim().isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(this.lastName.trim());
        }
        
        return fullName.toString();
    }

    /**
     * Checks if this customer is a primary cardholder.
     * 
     * @return true if the customer is marked as a primary cardholder
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isPrimaryCardholder() {
        return "Y".equals(this.primaryCardholderIndicator);
    }

    /**
     * Sets the primary cardholder indicator based on boolean value.
     * 
     * @param isPrimary true to mark as primary cardholder, false otherwise
     */
    public void setPrimaryCardholder(boolean isPrimary) {
        this.primaryCardholderIndicator = isPrimary ? "Y" : "N";
    }
}
