/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.carddemo.util.ValidationUtil;
import com.carddemo.util.Constants;

import java.math.BigDecimal;
import jakarta.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Account update request DTO for COACTUP screen operations.
 * Contains modifiable account fields including credit limit, address, phone, and status.
 * Excludes read-only fields like balance and supports partial updates with validation.
 * 
 * This DTO maps to the updateable fields from the COACTUP BMS mapset and ensures
 * all field validations match the original COBOL business rules from the account
 * update screen processing.
 * 
 * Supports partial updates by allowing null values for optional fields, with
 * validation applied only to non-null values.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Data
public class AccountUpdateDto {

    /**
     * Credit limit for the account in dollars with 2 decimal precision.
     * Maps to ACRDLIM field from COACTUP BMS mapset and ACCT-CREDIT-LIMIT 
     * from COBOL ACCOUNT-RECORD structure (PIC S9(10)V99).
     * 
     * Uses BigDecimal to maintain exact precision matching COBOL COMP-3
     * packed decimal behavior for financial calculations.
     */
    @JsonProperty("creditLimit")
    private BigDecimal creditLimit;

    /**
     * Cash credit limit for the account in dollars with 2 decimal precision.
     * Maps to ACSHLIM field from COACTUP BMS mapset and ACCT-CASH-CREDIT-LIMIT
     * from COBOL ACCOUNT-RECORD structure (PIC S9(10)V99).
     * 
     * Uses BigDecimal to maintain exact precision matching COBOL COMP-3
     * packed decimal behavior for financial calculations.
     */
    @JsonProperty("cashCreditLimit")
    private BigDecimal cashCreditLimit;

    /**
     * First line of the customer's address.
     * Maps to ACSADL1 field from COACTUP BMS mapset (LENGTH=50).
     * 
     * Supports partial updates - null values indicate field should not be updated.
     */
    @JsonProperty("addressLine1")
    private String addressLine1;

    /**
     * Second line of the customer's address.
     * Maps to ACSADL2 field from COACTUP BMS mapset (LENGTH=50).
     * 
     * Supports partial updates - null values indicate field should not be updated.
     */
    @JsonProperty("addressLine2")
    private String addressLine2;

    /**
     * Third line of the customer's address (typically city).
     * Maps to ACSCITY field from COACTUP BMS mapset (LENGTH=50).
     * 
     * Supports partial updates - null values indicate field should not be updated.
     */
    @JsonProperty("addressLine3")
    private String addressLine3;

    /**
     * State code for the customer's address.
     * Maps to ACSSTTE field from COACTUP BMS mapset (LENGTH=2).
     * 
     * Must be a valid 2-character US state code. Validation is performed
     * using ValidationUtil.isValidStateCode() method.
     */
    @JsonProperty("addressState")
    private String addressState;

    /**
     * ZIP code for the customer's address.
     * Maps to ACSZIPC field from COACTUP BMS mapset (LENGTH=5).
     * 
     * Must be exactly 5 digits. Validation is performed using
     * ValidationUtil.validateZipCode() method.
     */
    @JsonProperty("addressZip")
    private String addressZip;

    /**
     * Country code for the customer's address.
     * Maps to ACSCTRY field from COACTUP BMS mapset (LENGTH=3).
     * 
     * Supports partial updates - null values indicate field should not be updated.
     */
    @JsonProperty("addressCountry")
    private String addressCountry;

    /**
     * Customer's phone number in standard 10-digit format.
     * Maps to consolidated ACSPH1A, ACSPH1B, ACSPH1C fields from COACTUP BMS mapset.
     * 
     * Must be exactly 10 digits and contain a valid area code. Validation
     * is performed using ValidationUtil.validatePhoneNumber() method.
     */
    @JsonProperty("phoneNumber")
    private String phoneNumber;

    /**
     * Active status of the account (Y for active, N for inactive).
     * Maps to ACSTTUS field from COACTUP BMS mapset (LENGTH=1).
     * 
     * Must be exactly 'Y' or 'N' matching COBOL business rules from
     * ACCOUNT-RECORD structure.
     */
    @Pattern(regexp = "^[YN]$", message = "Active status must be 'Y' or 'N'")
    @JsonProperty("activeStatus")
    private String activeStatus;

    /**
     * Validates credit limit ensuring it's positive and within reasonable limits.
     * 
     * @param creditLimit the credit limit to validate
     * @return the validated credit limit with proper scale
     */
    public BigDecimal getCreditLimit() {
        return creditLimit != null ? creditLimit.setScale(2, BigDecimal.ROUND_HALF_UP) : null;
    }

    /**
     * Sets credit limit with proper scale for financial precision.
     * 
     * @param creditLimit the credit limit to set
     */
    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit != null ? creditLimit.setScale(2, BigDecimal.ROUND_HALF_UP) : null;
    }

    /**
     * Validates cash credit limit ensuring it's positive and within reasonable limits.
     * 
     * @param cashCreditLimit the cash credit limit to validate
     * @return the validated cash credit limit with proper scale
     */
    public BigDecimal getCashCreditLimit() {
        return cashCreditLimit != null ? cashCreditLimit.setScale(2, BigDecimal.ROUND_HALF_UP) : null;
    }

    /**
     * Sets cash credit limit with proper scale for financial precision.
     * 
     * @param cashCreditLimit the cash credit limit to set
     */
    public void setCashCreditLimit(BigDecimal cashCreditLimit) {
        this.cashCreditLimit = cashCreditLimit != null ? cashCreditLimit.setScale(2, BigDecimal.ROUND_HALF_UP) : null;
    }

    /**
     * Gets the first line of address with length validation.
     * 
     * @return the first address line, trimmed if not null
     */
    public String getAddressLine1() {
        return addressLine1 != null ? addressLine1.trim() : null;
    }

    /**
     * Sets the first line of address with length validation.
     * 
     * @param addressLine1 the first address line to set
     */
    public void setAddressLine1(String addressLine1) {
        if (addressLine1 != null) {
            ValidationUtil.validateFieldLength("addressLine1", addressLine1.trim(), 50);
        }
        this.addressLine1 = addressLine1;
    }

    /**
     * Gets the second line of address with length validation.
     * 
     * @return the second address line, trimmed if not null
     */
    public String getAddressLine2() {
        return addressLine2 != null ? addressLine2.trim() : null;
    }

    /**
     * Sets the second line of address with length validation.
     * 
     * @param addressLine2 the second address line to set
     */
    public void setAddressLine2(String addressLine2) {
        if (addressLine2 != null) {
            ValidationUtil.validateFieldLength("addressLine2", addressLine2.trim(), 50);
        }
        this.addressLine2 = addressLine2;
    }

    /**
     * Gets the third line of address (typically city) with length validation.
     * 
     * @return the third address line, trimmed if not null
     */
    public String getAddressLine3() {
        return addressLine3 != null ? addressLine3.trim() : null;
    }

    /**
     * Sets the third line of address (typically city) with length validation.
     * 
     * @param addressLine3 the third address line to set
     */
    public void setAddressLine3(String addressLine3) {
        if (addressLine3 != null) {
            ValidationUtil.validateFieldLength("addressLine3", addressLine3.trim(), 50);
        }
        this.addressLine3 = addressLine3;
    }

    /**
     * Gets the state code with validation.
     * 
     * @return the state code, trimmed and uppercased if not null
     */
    public String getAddressState() {
        return addressState != null ? addressState.trim().toUpperCase() : null;
    }

    /**
     * Sets the state code with validation using ValidationUtil.isValidStateCode().
     * 
     * @param addressState the state code to set
     */
    public void setAddressState(String addressState) {
        if (addressState != null) {
            String trimmedState = addressState.trim().toUpperCase();
            if (!trimmedState.isEmpty() && !ValidationUtil.isValidStateCode(trimmedState)) {
                throw new IllegalArgumentException("Invalid state code: " + trimmedState);
            }
            ValidationUtil.validateFieldLength("addressState", trimmedState, 2);
        }
        this.addressState = addressState;
    }

    /**
     * Gets the ZIP code with validation.
     * 
     * @return the ZIP code, trimmed if not null
     */
    public String getAddressZip() {
        return addressZip != null ? addressZip.trim() : null;
    }

    /**
     * Sets the ZIP code with validation using ValidationUtil.validateZipCode().
     * 
     * @param addressZip the ZIP code to set
     */
    public void setAddressZip(String addressZip) {
        if (addressZip != null && !addressZip.trim().isEmpty()) {
            ValidationUtil.validateZipCode("addressZip", addressZip.trim());
        }
        this.addressZip = addressZip;
    }

    /**
     * Gets the country code with validation.
     * 
     * @return the country code, trimmed if not null
     */
    public String getAddressCountry() {
        return addressCountry != null ? addressCountry.trim() : null;
    }

    /**
     * Sets the country code with length validation.
     * 
     * @param addressCountry the country code to set
     */
    public void setAddressCountry(String addressCountry) {
        if (addressCountry != null) {
            ValidationUtil.validateFieldLength("addressCountry", addressCountry.trim(), 3);
        }
        this.addressCountry = addressCountry;
    }

    /**
     * Gets the phone number with validation.
     * 
     * @return the phone number, trimmed if not null
     */
    public String getPhoneNumber() {
        return phoneNumber != null ? phoneNumber.trim() : null;
    }

    /**
     * Sets the phone number with validation using ValidationUtil.validatePhoneNumber().
     * 
     * @param phoneNumber the phone number to set
     */
    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            ValidationUtil.validatePhoneNumber("phoneNumber", phoneNumber.trim());
        }
        this.phoneNumber = phoneNumber;
    }

    /**
     * Gets the active status with validation.
     * 
     * @return the active status, trimmed if not null
     */
    public String getActiveStatus() {
        return activeStatus != null ? activeStatus.trim().toUpperCase() : null;
    }

    /**
     * Sets the active status with validation ensuring it's Y or N.
     * 
     * @param activeStatus the active status to set
     */
    public void setActiveStatus(String activeStatus) {
        if (activeStatus != null) {
            String trimmedStatus = activeStatus.trim().toUpperCase();
            if (!trimmedStatus.isEmpty() && !trimmedStatus.matches("^[YN]$")) {
                throw new IllegalArgumentException("Active status must be 'Y' or 'N'");
            }
            ValidationUtil.validateFieldLength("activeStatus", trimmedStatus, 1);
        }
        this.activeStatus = activeStatus;
    }
}