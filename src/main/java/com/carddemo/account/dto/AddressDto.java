/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.account.dto;

import com.carddemo.common.validator.ValidStateZip;
import com.carddemo.common.enums.UsStateCode;
import com.carddemo.common.enums.ValidationResult;
import com.carddemo.common.util.ValidationUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * Address Data Transfer Object for customer and account management operations.
 * 
 * This DTO maintains the exact structure and validation patterns from the original
 * COBOL customer record address fields (CVCUS01Y.cpy), providing multi-line address
 * support with comprehensive state and ZIP code validation. The implementation
 * preserves the original COBOL field lengths and validation logic while adapting
 * to modern Java/JSON serialization patterns.
 * 
 * Original COBOL Structure (CVCUS01Y.cpy):
 * - CUST-ADDR-LINE-1    PIC X(50)
 * - CUST-ADDR-LINE-2    PIC X(50)
 * - CUST-ADDR-LINE-3    PIC X(50)
 * - CUST-ADDR-STATE-CD  PIC X(02)
 * - CUST-ADDR-COUNTRY-CD PIC X(03)
 * - CUST-ADDR-ZIP       PIC X(10)
 * 
 * Key Features:
 * - Multi-line address support (address_line_1, address_line_2, address_line_3)
 * - State code validation using US state enumeration
 * - ZIP code format validation and cross-reference with state codes
 * - Country code support for international address standardization
 * - Jakarta Bean Validation integration for Spring Boot REST API validation
 * - JSON serialization support for React frontend components
 * - Comprehensive field validation maintaining COBOL business rules
 * 
 * Validation Rules:
 * - Address lines: Maximum 50 characters each (matching COBOL PIC X(50))
 * - State code: Exactly 2 characters, valid US state abbreviation
 * - Country code: Maximum 3 characters (matching COBOL PIC X(03))
 * - ZIP code: Maximum 10 characters, supports both 5-digit and ZIP+4 formats
 * - Cross-validation: State and ZIP code combinations must be geographically consistent
 * 
 * Integration Points:
 * - Customer management services for address updates
 * - Account management services for billing address management
 * - React frontend components for address form validation
 * - Spring Boot REST API endpoints for address data exchange
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 */
@ValidStateZip(
    stateField = "stateCode",
    zipField = "zipCode",
    message = "Invalid state code or inconsistent state/ZIP combination"
)
public class AddressDto {
    
    /**
     * First line of the address (street address).
     * Maps to CUST-ADDR-LINE-1 PIC X(50) from CVCUS01Y.cpy
     */
    @JsonProperty("address_line_1")
    @Size(max = 50, message = "Address line 1 cannot exceed 50 characters")
    private String addressLine1;
    
    /**
     * Second line of the address (apartment, suite, etc.).
     * Maps to CUST-ADDR-LINE-2 PIC X(50) from CVCUS01Y.cpy
     */
    @JsonProperty("address_line_2")
    @Size(max = 50, message = "Address line 2 cannot exceed 50 characters")
    private String addressLine2;
    
    /**
     * Third line of the address (additional address information).
     * Maps to CUST-ADDR-LINE-3 PIC X(50) from CVCUS01Y.cpy
     */
    @JsonProperty("address_line_3")
    @Size(max = 50, message = "Address line 3 cannot exceed 50 characters")
    private String addressLine3;
    
    /**
     * US state code (2-character abbreviation).
     * Maps to CUST-ADDR-STATE-CD PIC X(02) from CVCUS01Y.cpy
     */
    @JsonProperty("state_code")
    @Size(min = 2, max = 2, message = "State code must be exactly 2 characters")
    private String stateCode;
    
    /**
     * Country code (3-character abbreviation).
     * Maps to CUST-ADDR-COUNTRY-CD PIC X(03) from CVCUS01Y.cpy
     */
    @JsonProperty("country_code")
    @Size(max = 3, message = "Country code cannot exceed 3 characters")
    private String countryCode;
    
    /**
     * ZIP code (postal code).
     * Maps to CUST-ADDR-ZIP PIC X(10) from CVCUS01Y.cpy
     * Supports both 5-digit ZIP codes and ZIP+4 formats
     */
    @JsonProperty("zip_code")
    @Size(max = 10, message = "ZIP code cannot exceed 10 characters")
    private String zipCode;
    
    /**
     * Default constructor for JSON deserialization and framework usage.
     */
    public AddressDto() {
        // Initialize with default values
        this.addressLine1 = "";
        this.addressLine2 = "";
        this.addressLine3 = "";
        this.stateCode = "";
        this.countryCode = "US"; // Default to US
        this.zipCode = "";
    }
    
    /**
     * Constructor with all address fields.
     * 
     * @param addressLine1 First line of the address
     * @param addressLine2 Second line of the address
     * @param addressLine3 Third line of the address
     * @param stateCode US state code
     * @param countryCode Country code
     * @param zipCode ZIP code
     */
    public AddressDto(String addressLine1, String addressLine2, String addressLine3,
                     String stateCode, String countryCode, String zipCode) {
        this.addressLine1 = addressLine1 != null ? addressLine1 : "";
        this.addressLine2 = addressLine2 != null ? addressLine2 : "";
        this.addressLine3 = addressLine3 != null ? addressLine3 : "";
        this.stateCode = stateCode != null ? stateCode : "";
        this.countryCode = countryCode != null ? countryCode : "US";
        this.zipCode = zipCode != null ? zipCode : "";
    }
    
    /**
     * Gets the first line of the address.
     * 
     * @return First line of the address
     */
    public String getAddressLine1() {
        return addressLine1;
    }
    
    /**
     * Sets the first line of the address.
     * 
     * @param addressLine1 First line of the address
     */
    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1 != null ? addressLine1 : "";
    }
    
    /**
     * Gets the second line of the address.
     * 
     * @return Second line of the address
     */
    public String getAddressLine2() {
        return addressLine2;
    }
    
    /**
     * Sets the second line of the address.
     * 
     * @param addressLine2 Second line of the address
     */
    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2 != null ? addressLine2 : "";
    }
    
    /**
     * Gets the third line of the address.
     * 
     * @return Third line of the address
     */
    public String getAddressLine3() {
        return addressLine3;
    }
    
    /**
     * Sets the third line of the address.
     * 
     * @param addressLine3 Third line of the address
     */
    public void setAddressLine3(String addressLine3) {
        this.addressLine3 = addressLine3 != null ? addressLine3 : "";
    }
    
    /**
     * Gets the US state code.
     * 
     * @return US state code
     */
    public String getStateCode() {
        return stateCode;
    }
    
    /**
     * Sets the US state code.
     * 
     * @param stateCode US state code
     */
    public void setStateCode(String stateCode) {
        this.stateCode = stateCode != null ? stateCode : "";
    }
    
    /**
     * Gets the country code.
     * 
     * @return Country code
     */
    public String getCountryCode() {
        return countryCode;
    }
    
    /**
     * Sets the country code.
     * 
     * @param countryCode Country code
     */
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode != null ? countryCode : "US";
    }
    
    /**
     * Gets the ZIP code.
     * 
     * @return ZIP code
     */
    public String getZipCode() {
        return zipCode;
    }
    
    /**
     * Sets the ZIP code.
     * 
     * @param zipCode ZIP code
     */
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode != null ? zipCode : "";
    }
    
    /**
     * Validates the address data using COBOL-equivalent validation rules.
     * 
     * This method performs comprehensive validation of all address fields using the same
     * validation logic as the original COBOL implementation, including:
     * - Required field validation for address line 1
     * - Format validation for all address components
     * - State code validation using US state enumeration
     * - ZIP code format validation
     * - Cross-validation of state and ZIP code combinations
     * 
     * @return ValidationResult indicating the validation outcome
     */
    public ValidationResult validate() {
        // Validate address line 1 (required field)
        ValidationResult addressLine1Result = ValidationUtils.validateRequiredField(this.addressLine1);
        if (!addressLine1Result.isValid()) {
            return addressLine1Result;
        }
        
        // Validate state code if provided
        if (this.stateCode != null && !this.stateCode.trim().isEmpty()) {
            if (!UsStateCode.isValid(this.stateCode)) {
                return ValidationResult.INVALID_CROSS_REFERENCE;
            }
        }
        
        // Validate ZIP code format if provided
        if (this.zipCode != null && !this.zipCode.trim().isEmpty()) {
            ValidationResult zipResult = ValidationUtils.validateZipCode(this.zipCode);
            if (!zipResult.isValid()) {
                return zipResult;
            }
        }
        
        // Validate state and ZIP code combination if both are provided
        if (this.stateCode != null && !this.stateCode.trim().isEmpty() &&
            this.zipCode != null && !this.zipCode.trim().isEmpty()) {
            ValidationResult stateZipResult = UsStateCode.validateStateZipCombination(this.stateCode, this.zipCode);
            if (!stateZipResult.isValid()) {
                return stateZipResult;
            }
        }
        
        return ValidationResult.VALID;
    }
    
    /**
     * Checks if two AddressDto objects are equal.
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
        AddressDto that = (AddressDto) obj;
        return Objects.equals(addressLine1, that.addressLine1) &&
               Objects.equals(addressLine2, that.addressLine2) &&
               Objects.equals(addressLine3, that.addressLine3) &&
               Objects.equals(stateCode, that.stateCode) &&
               Objects.equals(countryCode, that.countryCode) &&
               Objects.equals(zipCode, that.zipCode);
    }
    
    /**
     * Generates hash code for the AddressDto object.
     * 
     * @return Hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(addressLine1, addressLine2, addressLine3, stateCode, countryCode, zipCode);
    }
    
    /**
     * Returns a string representation of the AddressDto object.
     * 
     * @return String representation of the address
     */
    @Override
    public String toString() {
        return "AddressDto{" +
               "addressLine1='" + addressLine1 + '\'' +
               ", addressLine2='" + addressLine2 + '\'' +
               ", addressLine3='" + addressLine3 + '\'' +
               ", stateCode='" + stateCode + '\'' +
               ", countryCode='" + countryCode + '\'' +
               ", zipCode='" + zipCode + '\'' +
               '}';
    }
}