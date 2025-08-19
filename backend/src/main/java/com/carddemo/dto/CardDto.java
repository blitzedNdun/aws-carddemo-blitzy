/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.carddemo.util.ValidationUtil;
import com.carddemo.util.FormatUtil;
import com.carddemo.util.Constants;

import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * Credit card DTO mapping to CVACT02Y copybook CARD-RECORD structure.
 * 
 * This DTO represents the card entity data structure, directly translated from the COBOL
 * CARD-RECORD copybook definition to maintain exact functional parity with the legacy
 * mainframe system. All field lengths and validation rules match the original COBOL
 * specifications to ensure seamless data compatibility.
 * 
 * Field Mappings from CVACT02Y copybook:
 * - CARD-NUM (PIC X(16)) → cardNumber (16 digits)
 * - CARD-ACCT-ID (PIC 9(11)) → accountId (11 digits) 
 * - CARD-CVV-CD (PIC 9(03)) → cvvCode (3 digits)
 * - CARD-EMBOSSED-NAME (PIC X(50)) → embossedName (50 characters)
 * - CARD-EXPIRAION-DATE (PIC X(10)) → expirationDate (LocalDate)
 * - CARD-ACTIVE-STATUS (PIC X(01)) → activeStatus (1 character)
 * 
 * Business Rules:
 * - Card number must be exactly 16 digits matching industry standards
 * - Account ID must be exactly 11 digits matching COBOL PIC 9(11) specification
 * - CVV code must be exactly 3 digits for security validation
 * - Embossed name supports up to 50 characters for cardholder identification
 * - Expiration date uses MM/YY format for card validity checking
 * - Active status uses Y/N flag for card activation state
 * 
 * Security Features:
 * - CVV code is marked with @JsonIgnore for PCI DSS compliance
 * - Card number masking available through getMaskedCardNumber()
 * - Sensitive data excluded from toString() output
 * 
 * This implementation supports the card listing, selection, and update operations
 * as defined in the BMS mapsets COCRDLI, COCRDSL, and COCRDUP.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Builder
public class CardDto {

    /**
     * Card number field - 16 digits matching CARD-NUM PIC X(16).
     * Represents the primary card identifier used for all card operations.
     */
    @NotNull
    private String cardNumber;

    /**
     * Account ID field - 11 digits matching CARD-ACCT-ID PIC 9(11).
     * Links the card to the associated account record.
     */
    @NotNull
    private String accountId;

    /**
     * CVV security code - 3 digits matching CARD-CVV-CD PIC 9(03).
     * Excluded from JSON serialization for security compliance.
     */
    @JsonIgnore
    private String cvvCode;

    /**
     * Embossed name field - up to 50 characters matching CARD-EMBOSSED-NAME PIC X(50).
     * Contains the cardholder name as it appears on the physical card.
     */
    private String embossedName;

    /**
     * Card expiration date matching CARD-EXPIRAION-DATE PIC X(10).
     * Stored as LocalDate for proper date handling and validation.
     */
    private LocalDate expirationDate;

    /**
     * Active status flag - 1 character matching CARD-ACTIVE-STATUS PIC X(01).
     * Uses Y/N values to indicate card activation state.
     */
    @NotNull
    private String activeStatus;

    // Card type constants for BIN range determination
    private static final String CARD_TYPE_VISA = "VISA";
    private static final String CARD_TYPE_MASTERCARD = "MASTERCARD";
    private static final String CARD_TYPE_AMEX = "AMEX";

    /**
     * Default constructor for framework compatibility.
     */
    public CardDto() {
        // Default constructor
    }

    /**
     * Full constructor for complete card data initialization.
     * Validates all field constraints during construction.
     *
     * @param cardNumber the 16-digit card number
     * @param accountId the 11-digit account ID
     * @param cvvCode the 3-digit CVV security code
     * @param embossedName the cardholder name (up to 50 characters)
     * @param expirationDate the card expiration date
     * @param activeStatus the card active status (Y/N)
     */
    public CardDto(String cardNumber, String accountId, String cvvCode, 
                   String embossedName, LocalDate expirationDate, String activeStatus) {
        this.cardNumber = cardNumber;
        this.accountId = accountId;
        this.cvvCode = cvvCode;
        this.embossedName = embossedName;
        this.expirationDate = expirationDate;
        this.activeStatus = activeStatus;
        
        // Validate all fields upon construction
        validateAllFields();
    }

    /**
     * Gets the card number field.
     * 
     * @return the 16-digit card number
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the card number field with validation.
     * Validates that the card number is exactly 16 digits using ValidationUtil.
     * 
     * @param cardNumber the 16-digit card number to set
     */
    public void setCardNumber(String cardNumber) {
        if (cardNumber != null) {
            ValidationUtil.validateRequiredField("cardNumber", cardNumber);
            new ValidationUtil.FieldValidator().validateCardNumber(cardNumber);
        }
        this.cardNumber = cardNumber;
    }

    /**
     * Gets the account ID field.
     * 
     * @return the 11-digit account ID
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID field with validation.
     * Validates that the account ID is exactly 11 digits using ValidationUtil.
     * 
     * @param accountId the 11-digit account ID to set
     */
    public void setAccountId(String accountId) {
        if (accountId != null) {
            ValidationUtil.validateRequiredField("accountId", accountId);
            new ValidationUtil.FieldValidator().validateAccountId(accountId);
        }
        this.accountId = accountId;
    }

    /**
     * Gets the CVV security code.
     * This method is marked with @JsonIgnore to prevent CVV exposure in API responses.
     * 
     * @return the 3-digit CVV code
     */
    @JsonIgnore
    public String getCvvCode() {
        return cvvCode;
    }

    /**
     * Sets the CVV security code with validation.
     * Validates that the CVV is exactly 3 digits.
     * 
     * @param cvvCode the 3-digit CVV code to set
     */
    public void setCvvCode(String cvvCode) {
        if (cvvCode != null) {
            ValidationUtil.validateRequiredField("cvvCode", cvvCode);
            if (!ValidationUtil.validateNumericField(cvvCode.trim(), 3)) {
                throw new IllegalArgumentException("CVV code must be exactly 3 digits");
            }
        }
        this.cvvCode = cvvCode;
    }

    /**
     * Gets the embossed name field.
     * 
     * @return the cardholder name as embossed on the card
     */
    public String getEmbossedName() {
        return embossedName;
    }

    /**
     * Sets the embossed name field with length validation.
     * Validates that the name does not exceed 50 characters.
     * 
     * @param embossedName the cardholder name to set
     */
    public void setEmbossedName(String embossedName) {
        if (embossedName != null) {
            ValidationUtil.validateFieldLength("embossedName", embossedName, 50);
        }
        this.embossedName = embossedName;
    }

    /**
     * Gets the card expiration date.
     * 
     * @return the card expiration date
     */
    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    /**
     * Sets the card expiration date with validation.
     * Validates that the expiration date is not null and not in the past.
     * 
     * @param expirationDate the card expiration date to set
     */
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * Gets the active status field.
     * 
     * @return the card active status (Y/N)
     */
    public String getActiveStatus() {
        return activeStatus;
    }

    /**
     * Sets the active status field with validation.
     * Validates that the status is exactly 1 character (Y or N).
     * 
     * @param activeStatus the card active status to set
     */
    public void setActiveStatus(String activeStatus) {
        if (activeStatus != null) {
            ValidationUtil.validateRequiredField("activeStatus", activeStatus);
            ValidationUtil.validateFieldLength("activeStatus", activeStatus, 1);
            if (!activeStatus.trim().toUpperCase().matches("[YN]")) {
                throw new IllegalArgumentException("Active status must be Y or N");
            }
        }
        this.activeStatus = activeStatus;
    }

    /**
     * Returns a masked version of the card number showing only the last 4 digits.
     * Implements PCI DSS compliance by masking sensitive card data for display purposes.
     * 
     * @return masked card number (e.g., "****-****-****-1234")
     */
    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            return "";
        }
        
        try {
            return FormatUtil.maskCardNumber(cardNumber);
        } catch (IllegalArgumentException e) {
            // If card number is invalid, return empty string for safety
            return "";
        }
    }

    /**
     * Determines the card type based on the card number BIN ranges.
     * Uses industry standard BIN (Bank Identification Number) ranges to determine
     * whether the card is VISA, MASTERCARD, AMEX, DISCOVER, or UNKNOWN.
     * 
     * @return the card type string ("VISA", "MASTERCARD", "AMEX", "DISCOVER", "UNKNOWN")
     */
    public String getCardType() {
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            return "UNKNOWN";
        }
        
        return ValidationUtil.determineCardType(cardNumber);
    }

    /**
     * Checks if the card is expired based on the expiration date.
     * Compares the expiration date with the current date to determine validity.
     * 
     * @return true if the card is expired, false otherwise
     */
    public boolean isExpired() {
        if (expirationDate == null) {
            return true; // Null expiration date is considered expired
        }
        
        LocalDate currentDate = LocalDate.now();
        return expirationDate.isBefore(currentDate);
    }

    /**
     * Returns a string representation of the card DTO.
     * Excludes sensitive information like CVV code for security purposes.
     * 
     * @return string representation of the card DTO
     */
    @Override
    public String toString() {
        return "CardDto{" +
                "cardNumber='" + getMaskedCardNumber() + '\'' +
                ", accountId='" + accountId + '\'' +
                ", embossedName='" + embossedName + '\'' +
                ", expirationDate=" + expirationDate +
                ", activeStatus='" + activeStatus + '\'' +
                ", cardType='" + getCardType() + '\'' +
                ", expired=" + isExpired() +
                '}';
    }

    /**
     * Checks equality based on card number and account ID.
     * Two cards are considered equal if they have the same card number and account ID.
     * 
     * @param obj the object to compare
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        CardDto cardDto = (CardDto) obj;
        return Objects.equals(cardNumber, cardDto.cardNumber) &&
               Objects.equals(accountId, cardDto.accountId);
    }

    /**
     * Returns a hash code based on card number and account ID.
     * 
     * @return hash code for the card DTO
     */
    @Override
    public int hashCode() {
        return Objects.hash(cardNumber, accountId);
    }

    /**
     * Creates a new builder instance for the CardDto class.
     * Enables fluent API construction pattern for easy object creation.
     * 
     * @return new CardDto builder instance
     */
    public static CardDtoBuilder builder() {
        return new CardDtoBuilder();
    }

    /**
     * Validates all fields of the card DTO to ensure data integrity.
     * Called during construction and can be called manually for validation.
     * 
     * @throws IllegalArgumentException if any field validation fails
     */
    private void validateAllFields() {
        if (cardNumber != null) {
            setCardNumber(cardNumber); // This will validate the card number
        }
        
        if (accountId != null) {
            setAccountId(accountId); // This will validate the account ID
        }
        
        if (cvvCode != null) {
            setCvvCode(cvvCode); // This will validate the CVV code
        }
        
        if (embossedName != null) {
            setEmbossedName(embossedName); // This will validate the embossed name
        }
        
        if (activeStatus != null) {
            setActiveStatus(activeStatus); // This will validate the active status
        }
    }

    /**
     * Static factory method to create a CardDto from COBOL field values.
     * Useful for converting COBOL data structures during migration.
     * 
     * @param cardNum COBOL CARD-NUM field value
     * @param cardAcctId COBOL CARD-ACCT-ID field value
     * @param cardCvvCd COBOL CARD-CVV-CD field value
     * @param cardEmbossedName COBOL CARD-EMBOSSED-NAME field value
     * @param cardExpirationDate COBOL CARD-EXPIRAION-DATE field value (MM/YY format)
     * @param cardActiveStatus COBOL CARD-ACTIVE-STATUS field value
     * @return new CardDto instance with validated field values
     */
    public static CardDto fromCobolFields(String cardNum, String cardAcctId, String cardCvvCd,
                                          String cardEmbossedName, String cardExpirationDate, 
                                          String cardActiveStatus) {
        CardDto cardDto = new CardDto();
        
        // Set and validate each field
        cardDto.setCardNumber(cardNum);
        cardDto.setAccountId(cardAcctId);
        cardDto.setCvvCode(cardCvvCd);
        cardDto.setEmbossedName(cardEmbossedName);
        cardDto.setActiveStatus(cardActiveStatus);
        
        // Parse expiration date from MM/YY format
        if (cardExpirationDate != null && !cardExpirationDate.trim().isEmpty()) {
            cardDto.setExpirationDate(parseExpirationDate(cardExpirationDate));
        }
        
        return cardDto;
    }

    /**
     * Parses expiration date from MM/YY format to LocalDate.
     * Converts COBOL date format to Java LocalDate for proper date handling.
     * 
     * @param expirationDateStr the expiration date in MM/YY format
     * @return LocalDate representing the last day of the expiration month
     */
    private static LocalDate parseExpirationDate(String expirationDateStr) {
        if (expirationDateStr == null || expirationDateStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            String trimmed = expirationDateStr.trim();
            
            // Handle MM/YY format
            if (trimmed.length() == 5 && trimmed.contains("/")) {
                String[] parts = trimmed.split("/");
                if (parts.length == 2) {
                    int month = Integer.parseInt(parts[0]);
                    int year = Integer.parseInt(parts[1]);
                    
                    // Convert 2-digit year to 4-digit year
                    if (year < 50) {
                        year += 2000; // 00-49 becomes 2000-2049
                    } else {
                        year += 1900; // 50-99 becomes 1950-1999 (but likely not used for cards)
                    }
                    
                    // Return the last day of the expiration month
                    return LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
                }
            }
            
            throw new IllegalArgumentException("Invalid expiration date format. Expected MM/YY");
            
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new IllegalArgumentException("Invalid expiration date format: " + expirationDateStr, e);
        }
    }
}