/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.carddemo.dto.CardDto;

import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Simplified card DTO for list display in COCRDLI screen.
 * 
 * This DTO represents a simplified view of card information specifically designed for
 * list display operations in the Card Listing Screen (COCRDLI). It contains only the
 * essential fields needed for card selection and display, with security-compliant
 * masking for sensitive card data.
 * 
 * Based on BMS Map COCRDLI.bms structure:
 * - Displays cards in a 7-row listing format
 * - Supports card selection through selectionFlag
 * - Shows masked card numbers for PCI DSS compliance
 * - Displays card type, expiration date, and active status
 * - Includes account ID for cross-reference operations
 * 
 * Field Mappings from COCRDLI BMS Map:
 * - CRDSEL1-7 → selectionFlag (selection indicator)
 * - ACCTNO1-7 → accountId (11-digit account number)  
 * - CRDNUM1-7 → maskedCardNumber (****-****-****-1234 format)
 * - CRDSTS1-7 → activeStatus (Y/N indicator)
 * 
 * Security Features:
 * - Card number automatically masked using CardDto.getMaskedCardNumber()
 * - CVV and other sensitive data excluded from this simplified view
 * - Only displays data necessary for list operations
 * 
 * This DTO supports the card listing and selection operations defined in the
 * COCRDLI BMS mapset for the credit card management system migration.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Data
public class CardListDto {

    /**
     * Masked card number field showing only last 4 digits.
     * Uses CardDto.getMaskedCardNumber() for security compliance.
     * Format: ****-****-****-1234
     */
    @NotNull
    @JsonProperty("maskedCardNumber")
    private String maskedCardNumber;

    /**
     * Account ID field - 11 digits matching ACCTNO fields in COCRDLI.
     * Links the card to the associated account record for cross-reference.
     */
    @NotNull
    @JsonProperty("accountId")
    private String accountId;

    /**
     * Card type determined from BIN ranges.
     * Uses CardDto.getCardType() for industry standard card type detection.
     * Values: VISA, MASTERCARD, AMEX, DISCOVER, UNKNOWN
     */
    @NotNull
    @JsonProperty("cardType")
    private String cardType;

    /**
     * Card expiration date for validity checking.
     * Used to determine if card is expired in list display.
     */
    @JsonProperty("expirationDate")
    private LocalDate expirationDate;

    /**
     * Active status flag - Y/N indicator matching CRDSTS fields.
     * Indicates whether the card is currently active and usable.
     */
    @NotNull
    @JsonProperty("activeStatus")
    private String activeStatus;

    /**
     * Selection flag for list operations.
     * Indicates whether this card row is selected in the COCRDLI list.
     * Used for card selection and navigation operations.
     */
    @JsonProperty("selectionFlag")
    private boolean selectionFlag;

    /**
     * Default constructor for framework compatibility.
     */
    public CardListDto() {
        this.selectionFlag = false;
    }

    /**
     * Constructor to create CardListDto from a full CardDto object.
     * Uses CardDto methods to populate simplified list view fields.
     * 
     * @param cardDto the full card DTO to convert to list format
     */
    public CardListDto(CardDto cardDto) {
        if (cardDto != null) {
            this.maskedCardNumber = cardDto.getMaskedCardNumber();
            this.accountId = cardDto.getAccountId();
            this.cardType = cardDto.getCardType();
            this.expirationDate = cardDto.getExpirationDate();
            this.activeStatus = cardDto.getActiveStatus();
            this.selectionFlag = false;
        }
    }

    /**
     * Full constructor for complete list item initialization.
     * 
     * @param maskedCardNumber the masked card number (****-****-****-1234)
     * @param accountId the 11-digit account ID
     * @param cardType the card type (VISA, MASTERCARD, etc.)
     * @param expirationDate the card expiration date
     * @param activeStatus the card active status (Y/N)
     * @param selectionFlag the selection state for list operations
     */
    public CardListDto(String maskedCardNumber, String accountId, String cardType,
                       LocalDate expirationDate, String activeStatus, boolean selectionFlag) {
        this.maskedCardNumber = maskedCardNumber;
        this.accountId = accountId;
        this.cardType = cardType;
        this.expirationDate = expirationDate;
        this.activeStatus = activeStatus;
        this.selectionFlag = selectionFlag;
    }

    /**
     * Checks if the card is expired based on the expiration date.
     * Used for display logic in the card listing screen.
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
     * Checks if the card is selectable in the list.
     * A card is selectable if it is active and not expired.
     * Used for enabling/disabling selection in COCRDLI screen.
     * 
     * @return true if the card can be selected, false otherwise
     */
    public boolean isSelectable() {
        return "Y".equalsIgnoreCase(activeStatus) && !isExpired();
    }

    /**
     * Formats the expiration date for display in MM/YY format.
     * Matches the COBOL date format used in the original system.
     * 
     * @return formatted expiration date string (MM/YY) or empty string if null
     */
    public String getFormattedExpirationDate() {
        if (expirationDate == null) {
            return "";
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");
        return expirationDate.format(formatter);
    }

    /**
     * Returns a display-friendly version of the active status.
     * Converts Y/N flags to user-friendly text for UI display.
     * 
     * @return "Active" for Y, "Inactive" for N, "Unknown" for other values
     */
    public String getDisplayActiveStatus() {
        if ("Y".equalsIgnoreCase(activeStatus)) {
            return "Active";
        } else if ("N".equalsIgnoreCase(activeStatus)) {
            return "Inactive";
        } else {
            return "Unknown";
        }
    }

    /**
     * Creates a CardListDto from a CardDto using static factory method.
     * Provides a convenient way to convert full card data to list format.
     * 
     * @param cardDto the full card DTO to convert
     * @return new CardListDto instance or null if input is null
     */
    public static CardListDto fromCardDto(CardDto cardDto) {
        if (cardDto == null) {
            return null;
        }
        
        return new CardListDto(cardDto);
    }

    /**
     * Toggles the selection flag for list operations.
     * Used when user selects/deselects a card in the COCRDLI list.
     */
    public void toggleSelection() {
        this.selectionFlag = !this.selectionFlag;
    }

    /**
     * Clears the selection flag.
     * Used when resetting list selections in the COCRDLI screen.
     */
    public void clearSelection() {
        this.selectionFlag = false;
    }

    /**
     * Sets the selection flag if the card is selectable.
     * Ensures only active, non-expired cards can be selected.
     * 
     * @param selected the desired selection state
     * @return true if selection was set, false if card is not selectable
     */
    public boolean setSelectionIfSelectable(boolean selected) {
        if (isSelectable()) {
            this.selectionFlag = selected;
            return true;
        }
        return false;
    }

    /**
     * Creates a new builder instance for the CardListDto class.
     * Enables fluent API construction pattern for easy object creation.
     * Note: While Lombok @Data doesn't automatically generate builder,
     * this method provides compatibility with the expected schema.
     * 
     * @return new CardListDto builder instance
     */
    public static CardListDtoBuilder builder() {
        return new CardListDtoBuilder();
    }

    /**
     * Builder class for CardListDto to support fluent API construction.
     * Provides a convenient way to build CardListDto instances with optional fields.
     */
    public static class CardListDtoBuilder {
        private String maskedCardNumber;
        private String accountId;
        private String cardType;
        private LocalDate expirationDate;
        private String activeStatus;
        private boolean selectionFlag;

        public CardListDtoBuilder maskedCardNumber(String maskedCardNumber) {
            this.maskedCardNumber = maskedCardNumber;
            return this;
        }

        public CardListDtoBuilder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public CardListDtoBuilder cardType(String cardType) {
            this.cardType = cardType;
            return this;
        }

        public CardListDtoBuilder expirationDate(LocalDate expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        public CardListDtoBuilder activeStatus(String activeStatus) {
            this.activeStatus = activeStatus;
            return this;
        }

        public CardListDtoBuilder selectionFlag(boolean selectionFlag) {
            this.selectionFlag = selectionFlag;
            return this;
        }

        public CardListDto build() {
            return new CardListDto(maskedCardNumber, accountId, cardType, 
                                 expirationDate, activeStatus, selectionFlag);
        }
    }
}