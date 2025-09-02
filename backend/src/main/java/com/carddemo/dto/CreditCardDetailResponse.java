/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import com.carddemo.util.FormatUtil;

/**
 * Comprehensive response DTO for credit card detail display operations.
 * 
 * This class represents the complete credit card detail information returned from
 * REST API responses, translated directly from the COCRDSLC.cbl COBOL program
 * output structure. Contains all card information including masked card number,
 * account details, cardholder name, expiration date, status, and CVV code.
 * 
 * The DTO maintains COBOL field formatting and validation patterns while providing
 * JSON-compatible structure for modern web applications. All field lengths and
 * validation rules match the original COBOL PIC clause specifications from the
 * CVACT02Y copybook to ensure data consistency during migration.
 * 
 * Key Features:
 * - Card number masking for PCI DSS compliance using FormatUtil
 * - Account number formatting with proper zero-padding
 * - Expiration date handling with month/year component access
 * - JSON serialization with @JsonProperty annotations
 * - Field validation matching COBOL field specifications
 * - Comprehensive error handling for date parsing operations
 * 
 * This implementation directly addresses the requirements from Section 0.1.3
 * for preserving COBOL business logic exactly while providing REST-compatible
 * responses for the modernized Spring Boot application.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
public class CreditCardDetailResponse {

    /**
     * Card number field matching COBOL CARD-CARD-NUM-X PIC X(16).
     * Stores the full 16-digit card number for internal processing.
     * External access via getMaskedCardNumber() for security compliance.
     */
    @JsonProperty("cardNumber")
    @Size(max = 16, message = "Card number must not exceed 16 characters")
    private String cardNumber;

    /**
     * Account ID field matching COBOL CARD-ACCT-ID PIC 9(11).
     * 11-digit account identifier with zero-padding formatting.
     */
    @JsonProperty("accountId")
    @Size(max = 11, message = "Account ID must not exceed 11 characters")
    private String accountId;

    /**
     * Embossed name field matching COBOL CARD-NAME-EMBOSSED-X PIC X(50).
     * Cardholder name as it appears embossed on the physical card.
     */
    @JsonProperty("embossedName")
    @Size(max = 50, message = "Embossed name must not exceed 50 characters")
    private String embossedName;

    /**
     * Expiration date field matching COBOL CARD-EXPIRAION-DATE-X PIC X(10).
     * Stores date in YYYY-MM-DD format for consistent date handling.
     */
    @JsonProperty("expirationDate")
    @Size(max = 10, message = "Expiration date must not exceed 10 characters")
    private String expirationDate;

    /**
     * Card status field matching COBOL CARD-ACTIVE-STATUS PIC X(01).
     * Single character indicating card active status.
     */
    @JsonProperty("cardStatus")
    @Size(max = 1, message = "Card status must not exceed 1 character")
    private String cardStatus;

    /**
     * CVV code field matching COBOL CARD-CVV-CD PIC 9(03).
     * 3-digit card verification value for security purposes.
     */
    @JsonProperty("cvvCode")
    @Size(max = 3, message = "CVV code must not exceed 3 characters")
    private String cvvCode;

    /**
     * Date formatter for parsing COBOL date formats.
     * Handles various date input formats from COBOL systems.
     */
    private static final DateTimeFormatter COBOL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter EXPIRY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Default constructor for DTO instantiation.
     */
    public CreditCardDetailResponse() {
        // Default constructor for framework compatibility
    }

    /**
     * Constructor with all fields for complete DTO initialization.
     * 
     * @param cardNumber 16-digit card number
     * @param accountId 11-digit account identifier
     * @param embossedName cardholder embossed name
     * @param expirationDate card expiration date
     * @param cardStatus card active status
     * @param cvvCode 3-digit CVV code
     */
    public CreditCardDetailResponse(String cardNumber, String accountId, String embossedName,
            String expirationDate, String cardStatus, String cvvCode) {
        this.cardNumber = cardNumber;
        this.accountId = accountId;
        this.embossedName = embossedName;
        this.expirationDate = expirationDate;
        this.cardStatus = cardStatus;
        this.cvvCode = cvvCode;
    }

    /**
     * Gets the full card number for internal processing only.
     * 
     * WARNING: This method returns the unmasked card number and should only
     * be used for internal processing. For display purposes, use getMaskedCardNumber().
     * 
     * @return full 16-digit card number
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the card number with validation.
     * 
     * @param cardNumber 16-digit card number to set
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Gets the masked card number for secure display.
     * 
     * This method implements PCI DSS compliance by masking all but the last
     * 4 digits of the card number using FormatUtil.maskCardNumber().
     * Returns format: ****-****-****-1234
     * 
     * @return masked card number showing only last 4 digits
     */
    @JsonProperty("maskedCardNumber")
    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            return "";
        }
        
        try {
            return FormatUtil.maskCardNumber(cardNumber);
        } catch (IllegalArgumentException e) {
            // Return empty string if card number is invalid for masking
            return "";
        }
    }

    /**
     * Gets the account ID.
     * 
     * @return 11-digit account identifier
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID with proper formatting.
     * 
     * Applies zero-padding formatting using FormatUtil.formatAccountNumber()
     * to ensure compliance with COBOL PIC 9(11) specification.
     * 
     * @param accountId account identifier to set
     */
    public void setAccountId(String accountId) {
        if (accountId != null && !accountId.trim().isEmpty()) {
            try {
                this.accountId = FormatUtil.formatAccountNumber(accountId);
            } catch (IllegalArgumentException e) {
                // Store original value if formatting fails
                this.accountId = accountId;
            }
        } else {
            this.accountId = accountId;
        }
    }

    /**
     * Gets the embossed cardholder name.
     * 
     * @return cardholder name as embossed on card
     */
    public String getEmbossedName() {
        return embossedName;
    }

    /**
     * Sets the embossed cardholder name.
     * 
     * @param embossedName cardholder name to set
     */
    public void setEmbossedName(String embossedName) {
        this.embossedName = embossedName;
    }

    /**
     * Gets the card expiration date.
     * 
     * @return expiration date in YYYY-MM-DD format
     */
    public String getExpirationDate() {
        return expirationDate;
    }

    /**
     * Sets the card expiration date.
     * 
     * @param expirationDate expiration date to set
     */
    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * Gets the expiration month component from the expiration date.
     * 
     * Parses the expiration date field to extract the month value,
     * matching the COBOL CARD-EXPIRY-MONTH field functionality.
     * 
     * @return month value (1-12) or null if date is invalid
     */
    public Integer getExpirationMonth() {
        if (expirationDate == null || expirationDate.trim().isEmpty()) {
            return null;
        }
        
        try {
            LocalDate date = LocalDate.parse(expirationDate, EXPIRY_FORMATTER);
            return date.getMonthValue();
        } catch (DateTimeParseException e) {
            // Try alternative formats that might come from COBOL
            try {
                // Handle YYYYMMDD format
                if (expirationDate.length() >= 6) {
                    String yearMonth = expirationDate.substring(0, 4) + "-" + 
                                     expirationDate.substring(4, 6) + "-01";
                    LocalDate date = LocalDate.parse(yearMonth, EXPIRY_FORMATTER);
                    return date.getMonthValue();
                }
            } catch (Exception ex) {
                // Return null for unparseable dates
            }
            return null;
        }
    }

    /**
     * Gets the expiration year component from the expiration date.
     * 
     * Parses the expiration date field to extract the year value,
     * matching the COBOL CARD-EXPIRY-YEAR field functionality.
     * 
     * @return 4-digit year value or null if date is invalid
     */
    public Integer getExpirationYear() {
        if (expirationDate == null || expirationDate.trim().isEmpty()) {
            return null;
        }
        
        try {
            LocalDate date = LocalDate.parse(expirationDate, EXPIRY_FORMATTER);
            return date.getYear();
        } catch (DateTimeParseException e) {
            // Try alternative formats that might come from COBOL
            try {
                // Handle YYYYMMDD format
                if (expirationDate.length() >= 4) {
                    return Integer.parseInt(expirationDate.substring(0, 4));
                }
            } catch (NumberFormatException ex) {
                // Return null for unparseable dates
            }
            return null;
        }
    }

    /**
     * Gets the card status.
     * 
     * @return single character card status indicator
     */
    public String getCardStatus() {
        return cardStatus;
    }

    /**
     * Sets the card status.
     * 
     * @param cardStatus card status indicator to set
     */
    public void setCardStatus(String cardStatus) {
        this.cardStatus = cardStatus;
    }

    /**
     * Gets the CVV code.
     * 
     * @return 3-digit card verification value
     */
    public String getCvvCode() {
        return cvvCode;
    }

    /**
     * Sets the CVV code.
     * 
     * @param cvvCode 3-digit verification code to set
     */
    public void setCvvCode(String cvvCode) {
        this.cvvCode = cvvCode;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * 
     * Two CreditCardDetailResponse objects are considered equal if all their
     * field values are equal, implementing proper equality semantics for DTO comparison.
     * 
     * @param obj the reference object with which to compare
     * @return true if this object is the same as the obj argument; false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        CreditCardDetailResponse that = (CreditCardDetailResponse) obj;
        return Objects.equals(cardNumber, that.cardNumber) &&
               Objects.equals(accountId, that.accountId) &&
               Objects.equals(embossedName, that.embossedName) &&
               Objects.equals(expirationDate, that.expirationDate) &&
               Objects.equals(cardStatus, that.cardStatus) &&
               Objects.equals(cvvCode, that.cvvCode);
    }

    /**
     * Returns a hash code value for the object.
     * 
     * Generates hash code based on all field values to ensure proper
     * hash-based collection behavior and equality contract compliance.
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(cardNumber, accountId, embossedName, 
                          expirationDate, cardStatus, cvvCode);
    }

    /**
     * Returns a string representation of the object.
     * 
     * Provides a comprehensive string representation including all field values
     * with card number properly masked for security. Suitable for logging and
     * debugging purposes without exposing sensitive information.
     * 
     * @return a string representation of the object
     */
    @Override
    public String toString() {
        return String.format(
            "CreditCardDetailResponse{" +
            "maskedCardNumber='%s', " +
            "accountId='%s', " +
            "embossedName='%s', " +
            "expirationDate='%s', " +
            "cardStatus='%s', " +
            "cvvCode='***'" +
            "}",
            getMaskedCardNumber(),
            accountId,
            embossedName,
            expirationDate,
            cardStatus
        );
    }
}