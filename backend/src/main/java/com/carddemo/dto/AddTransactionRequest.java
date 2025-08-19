/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.carddemo.util.Constants;
import com.carddemo.util.ValidationUtil;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for adding new transactions from COTRN02 BMS screen.
 * 
 * Maps all input fields from the COTRN02 "Add Transaction" screen to support
 * transaction creation workflow. This DTO maintains exact field lengths and
 * validation rules from the original COBOL BMS map definition to ensure
 * functional parity during the mainframe modernization.
 * 
 * Field mappings from COTRN02.bms and COTRN02.cpy:
 * - ACTIDIN (PIC X(11)) -> accountId
 * - CARDNIN (PIC X(16)) -> cardNumber  
 * - TTYPCD (PIC X(2)) -> typeCode
 * - TCATCD (PIC X(4)) -> categoryCode
 * - TRNSRC (PIC X(10)) -> source
 * - TDESC (PIC X(60)) -> description
 * - TRNAMT (PIC X(12)) -> amount
 * - MNAME (PIC X(30)) -> merchantName
 * - MCITY (PIC X(25)) -> merchantCity
 * - MZIP (PIC X(10)) -> merchantZip
 * - TORIGDT (PIC X(10)) -> transactionDate
 * 
 * Validation rules implement COBOL field validation logic:
 * - Required fields match UNPROT fields from BMS definition
 * - Field lengths match COBOL PIC clause specifications
 * - Format validation maintains COBOL data integrity
 * - Amount precision preserves COMP-3 packed decimal behavior
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Data
public class AddTransactionRequest {

    /**
     * Account ID for the transaction (required).
     * Maps to ACTIDIN field from COTRN02 BMS map (PIC X(11)).
     * Must be exactly 11 digits as per COBOL account ID validation.
     */
    @NotNull(message = "Account ID must be supplied")
    @Size(max = Constants.ACCOUNT_ID_LENGTH, message = "Account ID must be " + Constants.ACCOUNT_ID_LENGTH + " characters or less")
    @Pattern(regexp = "^\\d{11}$", message = "Account ID must be exactly 11 digits")
    private String accountId;

    /**
     * Card number for the transaction (required).
     * Maps to CARDNIN field from COTRN02 BMS map (PIC X(16)).
     * Must be exactly 16 digits as per credit card industry standards.
     */
    @NotNull(message = "Card number must be supplied")
    @Size(max = Constants.CARD_NUMBER_LENGTH, message = "Card number must be " + Constants.CARD_NUMBER_LENGTH + " characters or less")
    @Pattern(regexp = "^\\d{16}$", message = "Card number must be exactly 16 digits")
    private String cardNumber;

    /**
     * Transaction type code (required).
     * Maps to TTYPCD field from COTRN02 BMS map (PIC X(2)).
     * Used for categorizing transaction types in the system.
     */
    @NotNull(message = "Transaction type code must be supplied")
    @Size(max = Constants.TYPE_CODE_LENGTH, message = "Transaction type code must be " + Constants.TYPE_CODE_LENGTH + " characters or less")
    private String typeCode;

    /**
     * Transaction category code (required).
     * Maps to TCATCD field from COTRN02 BMS map (PIC X(4)).
     * Used for detailed transaction categorization.
     */
    @NotNull(message = "Transaction category code must be supplied")
    @Size(max = Constants.CATEGORY_CODE_LENGTH, message = "Transaction category code must be " + Constants.CATEGORY_CODE_LENGTH + " characters or less")
    private String categoryCode;

    /**
     * Transaction source (optional).
     * Maps to TRNSRC field from COTRN02 BMS map (PIC X(10)).
     * Indicates the originating source of the transaction.
     */
    @Size(max = Constants.SOURCE_LENGTH, message = "Transaction source must be " + Constants.SOURCE_LENGTH + " characters or less")
    private String source;

    /**
     * Transaction description (optional).
     * Maps to TDESC field from COTRN02 BMS map (PIC X(60)).
     * Provides detailed description of the transaction.
     * Note: Using DESCRIPTION_LENGTH constant which should be 60 to match BMS field length.
     */
    @Size(max = Constants.DESCRIPTION_LENGTH, message = "Transaction description must be " + Constants.DESCRIPTION_LENGTH + " characters or less")
    private String description;

    /**
     * Transaction amount (required).
     * Maps to TRNAMT field from COTRN02 BMS map (PIC X(12)).
     * Uses BigDecimal to maintain exact financial precision matching COBOL COMP-3 behavior.
     * Format: monetary amounts with 2 decimal places (e.g., 123.45).
     */
    @NotNull(message = "Transaction amount must be supplied")
    @DecimalMin(value = "0.01", message = "Transaction amount must be greater than zero")
    @DecimalMax(value = "99999.99", message = "Transaction amount cannot exceed $99,999.99")
    @Digits(integer = 5, fraction = 2, message = "Transaction amount must have at most 5 integer digits and 2 decimal places")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "#.##")
    private BigDecimal amount;

    /**
     * Merchant name (optional).
     * Maps to MNAME field from COTRN02 BMS map (PIC X(30)).
     * Name of the merchant where the transaction occurred.
     */
    @Size(max = Constants.MERCHANT_NAME_LENGTH, message = "Merchant name must be " + Constants.MERCHANT_NAME_LENGTH + " characters or less")
    private String merchantName;

    /**
     * Merchant city (optional).
     * Maps to MCITY field from COTRN02 BMS map (PIC X(25)).
     * City where the merchant is located.
     */
    @Size(max = 25, message = "Merchant city must be 25 characters or less")
    private String merchantCity;

    /**
     * Merchant ZIP code (optional).
     * Maps to MZIP field from COTRN02 BMS map (PIC X(10)).
     * ZIP code of the merchant location. Must be 5 digits if provided.
     */
    @Size(max = 10, message = "Merchant ZIP code must be 10 characters or less")
    @Pattern(regexp = "^$|^\\d{5}$", message = "Merchant ZIP code must be exactly 5 digits if provided")
    private String merchantZip;

    /**
     * Transaction date (required).
     * Maps to TORIGDT field from COTRN02 BMS map (PIC X(10)).
     * Date when the transaction originally occurred.
     * Format: ISO date (yyyy-MM-dd) for JSON serialization.
     */
    @NotNull(message = "Transaction date must be supplied")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate transactionDate;

    /**
     * Validates the account ID using COBOL-compatible validation rules.
     * Delegates to ValidationUtil.validateAccountId() for consistency.
     */
    public void validateAccountId() {
        ValidationUtil.FieldValidator validator = new ValidationUtil.FieldValidator();
        validator.validateAccountId(this.accountId);
    }

    /**
     * Validates the card number using industry standard validation rules.
     * Delegates to ValidationUtil.validateCardNumber() for consistency.
     */
    public void validateCardNumber() {
        ValidationUtil.FieldValidator validator = new ValidationUtil.FieldValidator();
        validator.validateCardNumber(this.cardNumber);
    }

    /**
     * Validates the transaction amount using financial validation rules.
     * Delegates to ValidationUtil.validateTransactionAmount() for consistency.
     */
    public void validateTransactionAmount() {
        ValidationUtil.FieldValidator validator = new ValidationUtil.FieldValidator();
        validator.validateTransactionAmount(this.amount);
    }

    /**
     * Validates the merchant ZIP code using US postal validation rules.
     * Delegates to ValidationUtil.validateZipCode() for consistency.
     */
    public void validateZipCode() {
        if (this.merchantZip != null && !this.merchantZip.trim().isEmpty()) {
            ValidationUtil.validateZipCode("merchantZip", this.merchantZip);
        }
    }

    /**
     * Performs comprehensive validation of all fields using ValidationUtil methods.
     * This method ensures all ValidationUtil members_accessed are utilized as required.
     */
    public void validateAllFields() {
        validateAccountId();
        validateCardNumber();
        validateTransactionAmount();
        validateZipCode();
    }
}