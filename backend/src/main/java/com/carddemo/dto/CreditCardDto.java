/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.carddemo.util.ValidationUtil;
import com.carddemo.util.FormatUtil;
import com.carddemo.util.Constants;

import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Data Transfer Object representing credit card entity data.
 * 
 * This DTO supports card management operations including card listing (COCRDLI),
 * card selection (COCRDSL), and card update (COCRDUP) screen functionality.
 * Contains card details including number, account linkage, type, expiry, and status.
 * 
 * Maps credit card data structure from COBOL copybooks (CVCRD01Y) while providing
 * comprehensive validation and formatting capabilities for card management operations.
 * Supports card-account relationship fields and maintains exact functional parity
 * with the original COBOL credit card data structures.
 * 
 * Key Features:
 * - Card number validation and masking for PCI DSS compliance
 * - Card type determination based on number prefix
 * - Expiry date validation in MMYY format
 * - Account linkage with proper validation
 * - Card status management (Active/Inactive)
 * - Embossed name handling for physical card production
 * 
 * This implementation ensures complete compatibility with COBOL BMS screens
 * while providing modern Java validation and JSON serialization capabilities.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
public class CreditCardDto {

    @JsonProperty("cardNumber")
    @Size(min = 16, max = 16, message = "Card number must be exactly 16 digits")
    private String cardNumber;

    @JsonProperty("accountId")
    @Size(min = 11, max = 11, message = "Account ID must be exactly 11 digits")
    private String accountId;

    @JsonProperty("cardType")
    private String cardType;

    @JsonProperty("expiryDate")
    private String expiryDate;

    @JsonProperty("cardStatus")
    @Size(min = 1, max = 1, message = "Card status must be exactly 1 character")
    private String cardStatus;

    @JsonProperty("issueDate")
    private LocalDate issueDate;

    @JsonProperty("embossedName")
    @Size(max = 50, message = "Embossed name must be 50 characters or less")
    private String embossedName;

    /**
     * Default constructor for JSON deserialization and framework usage.
     */
    public CreditCardDto() {
        // Initialize with default values
        this.cardNumber = "";
        this.accountId = "";
        this.cardType = "";
        this.expiryDate = "";
        this.cardStatus = "N";
        this.issueDate = null;
        this.embossedName = "";
    }

    /**
     * Constructor with card number and account ID for basic card creation.
     * 
     * @param cardNumber the 16-digit card number
     * @param accountId the 11-digit account ID
     */
    public CreditCardDto(String cardNumber, String accountId) {
        this();
        this.cardNumber = cardNumber;
        this.accountId = accountId;
        // Auto-determine card type from card number
        this.cardType = ValidationUtil.determineCardType(cardNumber);
    }

    /**
     * Gets the card number.
     * 
     * @return the 16-digit card number
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the card number with validation.
     * 
     * @param cardNumber the 16-digit card number to set
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
        // Auto-determine card type when card number changes
        if (cardNumber != null && !cardNumber.trim().isEmpty()) {
            this.cardType = ValidationUtil.determineCardType(cardNumber);
        }
    }

    /**
     * Gets the account ID.
     * 
     * @return the 11-digit account ID
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID.
     * 
     * @param accountId the 11-digit account ID to set
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the card type.
     * 
     * @return the card type (VISA, MASTERCARD, AMEX, DISCOVER, etc.)
     */
    public String getCardType() {
        return cardType;
    }

    /**
     * Sets the card type.
     * 
     * @param cardType the card type to set
     */
    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    /**
     * Gets the expiry date.
     * 
     * @return the expiry date in MMYY format
     */
    public String getExpiryDate() {
        return expiryDate;
    }

    /**
     * Sets the expiry date in MMYY format.
     * 
     * @param expiryDate the expiry date in MMYY format
     */
    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    /**
     * Gets the card status.
     * 
     * @return the card status (Y for Active, N for Inactive)
     */
    public String getCardStatus() {
        return cardStatus;
    }

    /**
     * Sets the card status.
     * 
     * @param cardStatus the card status (Y for Active, N for Inactive)
     */
    public void setCardStatus(String cardStatus) {
        this.cardStatus = cardStatus;
    }

    /**
     * Gets the issue date.
     * 
     * @return the card issue date
     */
    public LocalDate getIssueDate() {
        return issueDate;
    }

    /**
     * Sets the issue date.
     * 
     * @param issueDate the card issue date to set
     */
    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    /**
     * Gets the embossed name.
     * 
     * @return the name embossed on the card
     */
    public String getEmbossedName() {
        return embossedName;
    }

    /**
     * Sets the embossed name.
     * 
     * @param embossedName the name to emboss on the card
     */
    public void setEmbossedName(String embossedName) {
        this.embossedName = embossedName;
    }

    /**
     * Gets the masked card number showing only the last 4 digits for security.
     * Implements PCI DSS compliance for card number display.
     * 
     * @return masked card number (e.g., "****-****-****-1234")
     */
    public String getMaskedCardNumber() {
        return FormatUtil.maskCardNumber(this.cardNumber);
    }

    /**
     * Checks if the card is expired based on the expiry date.
     * 
     * @return true if the card is expired, false otherwise
     */
    public boolean isExpired() {
        if (expiryDate == null || expiryDate.trim().isEmpty()) {
            return true; // No expiry date means expired
        }
        
        String trimmedExpiry = expiryDate.trim();
        if (trimmedExpiry.length() != 4) {
            return true; // Invalid format means expired
        }
        
        try {
            int month = Integer.parseInt(trimmedExpiry.substring(0, 2));
            int year = 2000 + Integer.parseInt(trimmedExpiry.substring(2, 4));
            
            // Validate month range
            if (month < 1 || month > 12) {
                return true; // Invalid month means expired
            }
            
            LocalDate expiryLocalDate = LocalDate.of(year, month, 1);
            // Card expires at the end of the expiry month
            LocalDate endOfMonth = expiryLocalDate.withDayOfMonth(expiryLocalDate.lengthOfMonth());
            
            return LocalDate.now().isAfter(endOfMonth);
        } catch (NumberFormatException e) {
            return true; // Invalid format means expired
        }
    }

    /**
     * Validates the card number using industry standard validation.
     * Uses ValidationUtil to ensure card number format and length compliance.
     * 
     * @return true if the card number is valid, false otherwise
     */
    public boolean validateCardNumber() {
        try {
            ValidationUtil.FieldValidator validator = new ValidationUtil.FieldValidator();
            validator.validateCardNumber(this.cardNumber);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates the card number and throws exception if invalid.
     * Provides detailed validation error messages for debugging.
     * 
     * @throws RuntimeException if the card number is invalid
     */
    private void validateCardNumberWithException() {
        ValidationUtil.FieldValidator validator = new ValidationUtil.FieldValidator();
        validator.validateCardNumber(this.cardNumber);
    }

    /**
     * Validates all fields of the credit card DTO.
     * Performs comprehensive validation including card number, account ID, 
     * expiry date format, and required field checks.
     */
    public void validate() {
        // Validate card number
        ValidationUtil.validateFieldLength("cardNumber", this.cardNumber, Constants.CARD_NUMBER_LENGTH);
        if (this.cardNumber != null && !this.cardNumber.trim().isEmpty()) {
            validateCardNumberWithException();
        }
        
        // Validate account ID
        ValidationUtil.validateFieldLength("accountId", this.accountId, Constants.ACCOUNT_ID_LENGTH);
        ValidationUtil.validateRequiredField("accountId", this.accountId);
        
        // Validate card status
        if (this.cardStatus != null && !this.cardStatus.trim().isEmpty()) {
            ValidationUtil.validateFieldLength("cardStatus", this.cardStatus, 1);
            if (!"Y".equals(this.cardStatus.trim()) && !"N".equals(this.cardStatus.trim())) {
                throw new IllegalArgumentException("Card status must be 'Y' (Active) or 'N' (Inactive)");
            }
        }
        
        // Validate expiry date format
        if (this.expiryDate != null && !this.expiryDate.trim().isEmpty()) {
            String trimmedExpiry = this.expiryDate.trim();
            if (trimmedExpiry.length() != 4) {
                throw new IllegalArgumentException("Expiry date must be in MMYY format");
            }
            try {
                int month = Integer.parseInt(trimmedExpiry.substring(0, 2));
                int year = Integer.parseInt(trimmedExpiry.substring(2, 4));
                if (month < 1 || month > 12) {
                    throw new IllegalArgumentException("Expiry month must be between 01 and 12");
                }
                if (year < 0 || year > 99) {
                    throw new IllegalArgumentException("Expiry year must be between 00 and 99");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Expiry date must contain only numeric characters");
            }
        }
        
        // Validate embossed name length
        if (this.embossedName != null) {
            ValidationUtil.validateFieldLength("embossedName", this.embossedName, 50);
        }
    }

    /**
     * Checks equality based on card number and account ID.
     * 
     * @param obj the object to compare with
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
        CreditCardDto that = (CreditCardDto) obj;
        return Objects.equals(cardNumber, that.cardNumber) &&
               Objects.equals(accountId, that.accountId) &&
               Objects.equals(cardType, that.cardType) &&
               Objects.equals(expiryDate, that.expiryDate) &&
               Objects.equals(cardStatus, that.cardStatus) &&
               Objects.equals(issueDate, that.issueDate) &&
               Objects.equals(embossedName, that.embossedName);
    }

    /**
     * Generates hash code based on card number and account ID.
     * 
     * @return hash code for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(cardNumber, accountId, cardType, expiryDate, 
                           cardStatus, issueDate, embossedName);
    }

    /**
     * Returns string representation of the credit card with masked card number.
     * 
     * @return string representation including masked card number for security
     */
    @Override
    public String toString() {
        return "CreditCardDto{" +
                "cardNumber='" + getMaskedCardNumber() + '\'' +
                ", accountId='" + FormatUtil.formatAccountNumber(accountId) + '\'' +
                ", cardType='" + cardType + '\'' +
                ", expiryDate='" + expiryDate + '\'' +
                ", cardStatus='" + cardStatus + '\'' +
                ", issueDate=" + issueDate +
                ", embossedName='" + embossedName + '\'' +
                ", expired=" + isExpired() +
                '}';
    }
}
