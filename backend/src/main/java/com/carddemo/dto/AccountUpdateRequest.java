/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.carddemo.util.Constants;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for account update operations (CAUP transaction).
 * Maps to COACTUP BMS input fields for account modification operations.
 * 
 * This DTO handles account update requests from the React frontend and validates
 * all modifiable account fields according to COBOL field specifications from
 * the original COACTUP.bms mapset.
 * 
 * Key validations:
 * - Account ID: 11-digit format matching COBOL PIC X(11)
 * - Active Status: Y/N validation matching COBOL single character field
 * - Credit Limits: Monetary amounts with 2 decimal precision matching COMP-3 fields
 * - Expiration Date: ISO date format for proper date handling
 */
@Data
public class AccountUpdateRequest {

    /**
     * Account identifier for the account to be updated.
     * Maps to ACCTSID field in COACTUP.bms with LENGTH=11.
     * Must be exactly 11 characters to match COBOL PIC X(11) specification.
     */
    @JsonProperty("accountId")
    @NotBlank(message = "Account ID is required")
    @Size(min = Constants.ACCOUNT_NUMBER_LENGTH, max = Constants.ACCOUNT_NUMBER_LENGTH, 
          message = "Account ID must be exactly " + Constants.ACCOUNT_NUMBER_LENGTH + " characters")
    @Pattern(regexp = "^[0-9]{11}$", message = "Account ID must be 11 digits")
    private String accountId;

    /**
     * Active status indicator for the account.
     * Maps to ACSTTUS field in COACTUP.bms with LENGTH=1.
     * Must be either 'Y' (active) or 'N' (inactive) to match COBOL logic.
     */
    @JsonProperty("activeStatus")
    @NotBlank(message = "Active status is required")
    @Size(min = 1, max = 1, message = "Active status must be exactly 1 character")
    @Pattern(regexp = "^[YN]$", message = "Active status must be Y or N")
    private String activeStatus;

    /**
     * Credit limit amount for the account.
     * Maps to ACRDLIM field in COACTUP.bms with LENGTH=15.
     * Uses BigDecimal with scale 2 to exactly replicate COBOL COMP-3 S9(13)V99 precision
     * and prevent floating-point rounding errors in financial calculations.
     */
    @JsonProperty("creditLimit")
    @NotNull(message = "Credit limit is required")
    @DecimalMin(value = "0.00", message = "Credit limit must be non-negative")
    @Digits(integer = 13, fraction = 2, message = "Credit limit must have at most 13 integer digits and 2 decimal places")
    private BigDecimal creditLimit;

    /**
     * Cash credit limit amount for the account.
     * Maps to ACSHLIM field in COACTUP.bms with LENGTH=15.
     * Uses BigDecimal with scale 2 to exactly replicate COBOL COMP-3 S9(13)V99 precision
     * and prevent floating-point rounding errors in financial calculations.
     */
    @JsonProperty("cashCreditLimit")
    @NotNull(message = "Cash credit limit is required")
    @DecimalMin(value = "0.00", message = "Cash credit limit must be non-negative")
    @Digits(integer = 13, fraction = 2, message = "Cash credit limit must have at most 13 integer digits and 2 decimal places")
    private BigDecimal cashCreditLimit;

    /**
     * Account expiration date.
     * Maps to EXPYEAR/EXPMON/EXPDAY fields in COACTUP.bms.
     * Uses LocalDate for proper date validation and formatting,
     * replacing the separate year/month/day COBOL fields with a single date object.
     */
    @JsonProperty("expirationDate")
    @NotNull(message = "Expiration date is required")
    private LocalDate expirationDate;

    /**
     * Default constructor for JSON deserialization and Spring framework.
     */
    public AccountUpdateRequest() {
        super();
    }

    /**
     * Full constructor for creating AccountUpdateRequest with all required fields.
     * 
     * @param accountId The 11-digit account identifier
     * @param activeStatus The account active status (Y/N)
     * @param creditLimit The account credit limit amount
     * @param cashCreditLimit The account cash credit limit amount
     * @param expirationDate The account expiration date
     */
    public AccountUpdateRequest(String accountId, String activeStatus, BigDecimal creditLimit, 
                               BigDecimal cashCreditLimit, LocalDate expirationDate) {
        this.accountId = accountId;
        this.activeStatus = activeStatus;
        this.creditLimit = creditLimit;
        this.cashCreditLimit = cashCreditLimit;
        this.expirationDate = expirationDate;
    }
}