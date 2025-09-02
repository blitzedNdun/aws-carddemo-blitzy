/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.carddemo.util.Constants;
import com.carddemo.util.FormatUtil;
import com.carddemo.util.ValidationUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Objects;

/**
 * DTO for card response containing card details with masked sensitive information.
 * Maps CARD-RECORD structure from CVACT02Y.cpy for secure REST API responses.
 * 
 * This DTO implements API-Level Data Masking by:
 * - Masking credit card numbers to show only last 4 digits (****-****-****-1234)
 * - Excluding CVV from response for PCI DSS compliance
 * - Deriving card type from card number BIN range
 * - Formatting dates as ISO-8601 format for REST API compatibility
 * 
 * Corresponds to COBOL CARD-RECORD structure:
 * - CARD-NUM (PIC X(16)) -> cardNumber (masked)
 * - CARD-ACCT-ID (PIC 9(11)) -> accountId
 * - CARD-EMBOSSED-NAME (PIC X(50)) -> embossedName
 * - CARD-EXPIRAION-DATE (PIC X(10)) -> expirationDate (as LocalDate)
 * - CARD-ACTIVE-STATUS (PIC X(01)) -> activeStatus
 * - Derived field -> cardType
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {

    // Private constants for missing values from Constants class
    private static final int ACTIVE_STATUS_LENGTH = 1;
    private static final String CARD_TYPE_VISA = "VISA";
    private static final String CARD_TYPE_MASTERCARD = "MASTERCARD";
    private static final String CARD_TYPE_AMEX = "AMEX";
    
    /**
     * Masked card number showing only last 4 digits.
     * Format: ****-****-****-1234
     * Read-only field for security compliance.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Size(max = Constants.CARD_NUMBER_LENGTH, message = "Card number cannot exceed {max} characters")
    private String cardNumber;

    /**
     * Account ID associated with this card.
     * Maps to CARD-ACCT-ID from COBOL CARD-RECORD.
     */
    @Size(max = Constants.ACCOUNT_ID_LENGTH, message = "Account ID cannot exceed {max} characters")
    private String accountId;

    /**
     * Cardholder name embossed on the card.
     * Maps to CARD-EMBOSSED-NAME from COBOL CARD-RECORD.
     */
    @Size(max = 50, message = "Embossed name cannot exceed {max} characters")
    private String embossedName;

    /**
     * Card expiration date in ISO-8601 format.
     * Converted from COBOL PIC X(10) format to LocalDate.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;

    /**
     * Card active status indicator.
     * Maps to CARD-ACTIVE-STATUS from COBOL CARD-RECORD.
     * Values: 'Y' = Active, 'N' = Inactive
     */
    @Size(min = ACTIVE_STATUS_LENGTH, max = ACTIVE_STATUS_LENGTH, 
          message = "Active status must be exactly {max} character")
    private String activeStatus;

    /**
     * Card type derived from card number BIN range.
     * Possible values: VISA, MASTERCARD, AMEX, DISCOVER, UNKNOWN
     */
    private String cardType;

    /**
     * Constructor with card number masking and type derivation.
     * 
     * @param rawCardNumber the raw card number to be masked
     * @param accountId the account ID
     * @param embossedName the cardholder name
     * @param expirationDate the card expiration date
     * @param activeStatus the card active status
     */
    public CardResponse(String rawCardNumber, String accountId, String embossedName, 
                       LocalDate expirationDate, String activeStatus) {
        this.accountId = accountId;
        this.embossedName = embossedName;
        this.expirationDate = expirationDate;
        this.activeStatus = activeStatus;
        
        // Mask card number and derive card type
        if (rawCardNumber != null && !rawCardNumber.trim().isEmpty()) {
            this.cardNumber = FormatUtil.maskCardNumber(rawCardNumber);
            this.cardType = ValidationUtil.determineCardType(rawCardNumber);
        } else {
            this.cardNumber = null;
            this.cardType = "UNKNOWN";
        }
    }

    /**
     * Gets the masked card number.
     * Returns card number with only last 4 digits visible.
     * 
     * @return masked card number in format ****-****-****-1234
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the card number with automatic masking.
     * Also derives and sets the card type.
     * 
     * @param rawCardNumber the raw card number to mask and set
     */
    public void setCardNumber(String rawCardNumber) {
        if (rawCardNumber != null && !rawCardNumber.trim().isEmpty()) {
            this.cardNumber = FormatUtil.maskCardNumber(rawCardNumber);
            this.cardType = ValidationUtil.determineCardType(rawCardNumber);
        } else {
            this.cardNumber = null;
            this.cardType = "UNKNOWN";
        }
    }

    /**
     * Gets the account ID associated with this card.
     * 
     * @return the account ID
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID for this card.
     * 
     * @param accountId the account ID to set
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the cardholder name embossed on the card.
     * 
     * @return the embossed name
     */
    public String getEmbossedName() {
        return embossedName;
    }

    /**
     * Sets the cardholder name to be embossed on the card.
     * 
     * @param embossedName the embossed name to set
     */
    public void setEmbossedName(String embossedName) {
        this.embossedName = embossedName;
    }

    /**
     * Gets the card expiration date.
     * 
     * @return the expiration date as LocalDate
     */
    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    /**
     * Sets the card expiration date.
     * 
     * @param expirationDate the expiration date to set
     */
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * Gets the card active status.
     * 
     * @return the active status ('Y' or 'N')
     */
    public String getActiveStatus() {
        return activeStatus;
    }

    /**
     * Sets the card active status.
     * 
     * @param activeStatus the active status to set ('Y' or 'N')
     */
    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }

    /**
     * Gets the derived card type.
     * 
     * @return the card type (VISA, MASTERCARD, AMEX, DISCOVER, UNKNOWN)
     */
    public String getCardType() {
        return cardType;
    }

    /**
     * Sets the card type.
     * Note: This is typically derived automatically from the card number.
     * 
     * @param cardType the card type to set
     */
    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    /**
     * Returns a string representation of the CardResponse.
     * Card number is shown masked for security.
     * 
     * @return string representation of the CardResponse
     */
    @Override
    public String toString() {
        return "CardResponse{" +
                "cardNumber='" + cardNumber + '\'' +
                ", accountId='" + accountId + '\'' +
                ", embossedName='" + embossedName + '\'' +
                ", expirationDate=" + expirationDate +
                ", activeStatus='" + activeStatus + '\'' +
                ", cardType='" + cardType + '\'' +
                '}';
    }

    /**
     * Checks equality based on card number and account ID.
     * 
     * @param obj the object to compare with
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CardResponse that = (CardResponse) obj;
        return Objects.equals(cardNumber, that.cardNumber) &&
                Objects.equals(accountId, that.accountId) &&
                Objects.equals(embossedName, that.embossedName) &&
                Objects.equals(expirationDate, that.expirationDate) &&
                Objects.equals(activeStatus, that.activeStatus) &&
                Objects.equals(cardType, that.cardType);
    }

    /**
     * Generates hash code based on card number and account ID.
     * 
     * @return hash code for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(cardNumber, accountId, embossedName, expirationDate, activeStatus, cardType);
    }
}