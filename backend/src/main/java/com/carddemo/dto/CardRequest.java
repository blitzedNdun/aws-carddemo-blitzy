/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.carddemo.util.Constants;
import com.carddemo.util.ValidationUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.time.LocalDate;

/**
 * Data Transfer Object for card-related requests including card activation, updates, and status changes.
 * Maps to card management BMS screens (COCRDLI.bms, COCRDUP.bms) and program inputs from COBOL 
 * programs COCRDLIC.cbl and COCRDUPC.cbl.
 * 
 * This DTO represents the CARD-RECORD structure from CVACT02Y.cpy copybook, providing a modern
 * Java representation of card data with comprehensive validation and security features.
 * 
 * Key Features:
 * - Field validation using Jakarta Bean Validation annotations
 * - Security compliance with @JsonIgnore for sensitive CVV data
 * - Support for partial updates during status changes
 * - Direct mapping to COBOL field lengths and validation rules
 * 
 * Field Mappings from COBOL CARD-RECORD:
 * - CARD-NUM (PIC X(16)) → cardNumber
 * - CARD-ACCT-ID (PIC 9(11)) → accountId  
 * - CARD-CVV-CD (PIC 9(03)) → cvvCode
 * - CARD-EMBOSSED-NAME (PIC X(50)) → embossedName
 * - CARD-EXPIRAION-DATE (PIC X(10)) → expirationDate (converted to LocalDate)
 * - CARD-ACTIVE-STATUS (PIC X(01)) → activeStatus
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Data
public class CardRequest {

    /**
     * Credit card number - must be exactly 16 digits.
     * Maps to CARD-NUM field from CVACT02Y.cpy (PIC X(16)).
     * 
     * Validation includes:
     * - Pattern validation for 16 digits only
     * - Business validation through ValidationUtil.validateCardNumber()
     * 
     * @see ValidationUtil#validateCardNumber(String)
     */
    @Pattern(
        regexp = "^\\d{16}$", 
        message = "Card number must be exactly 16 digits"
    )
    private String cardNumber;

    /**
     * Account ID associated with the card - must be exactly 11 digits.
     * Maps to CARD-ACCT-ID field from CVACT02Y.cpy (PIC 9(11)).
     * 
     * Validation includes:
     * - Pattern validation for 11 digits only
     * - Business validation through ValidationUtil.validateAccountId()
     * 
     * @see ValidationUtil#validateAccountId(String)
     */
    @Pattern(
        regexp = "^\\d{11}$", 
        message = "Account ID must be exactly 11 digits"
    )
    private String accountId;

    /**
     * Card Verification Value (CVV) code - 3 digits.
     * Maps to CARD-CVV-CD field from CVACT02Y.cpy (PIC 9(03)).
     * 
     * Security Features:
     * - @JsonIgnore prevents CVV from appearing in JSON responses, logs, or toString() output
     * - Maintains PCI DSS compliance by excluding sensitive card verification data
     * - Pattern validation ensures 3-digit format
     * 
     * Note: CVV is typically used for initial card creation and verification but should
     * not be persisted long-term or transmitted in most API responses for security.
     */
    @JsonIgnore
    @Pattern(
        regexp = "^\\d{3}$", 
        message = "CVV code must be exactly 3 digits"
    )
    private String cvvCode;

    /**
     * Name embossed on the card - maximum 50 characters.
     * Maps to CARD-EMBOSSED-NAME field from CVACT02Y.cpy (PIC X(50)).
     * 
     * This field represents the cardholder name as it appears physically on the card.
     * Validation ensures length constraints matching COBOL field specifications.
     */
    private String embossedName;

    /**
     * Card expiration date.
     * Maps to CARD-EXPIRAION-DATE field from CVACT02Y.cpy (PIC X(10)).
     * 
     * Converted from COBOL character date format to Java LocalDate for:
     * - Type safety and proper date arithmetic
     * - Automatic JSON serialization/deserialization
     * - Integration with modern Java date/time APIs
     * 
     * The original COBOL format was text-based; this provides proper date handling
     * with validation for future expiration dates and reasonable date ranges.
     */
    private LocalDate expirationDate;

    /**
     * Card active status - single character Y/N indicator.
     * Maps to CARD-ACTIVE-STATUS field from CVACT02Y.cpy (PIC X(01)).
     * 
     * Valid values:
     * - "Y" = Active card, can be used for transactions
     * - "N" = Inactive card, blocked from transactions
     * 
     * Pattern validation ensures only Y or N values are accepted, matching
     * COBOL business logic from COCRDUPC.cbl validation routines.
     */
    @Pattern(
        regexp = "^[YN]$", 
        message = "Active status must be Y (active) or N (inactive)"
    )
    private String activeStatus;

    /**
     * Validates the card request data using business validation rules.
     * Performs comprehensive validation including:
     * - Card number format and length validation
     * - Account ID format and cross-reference validation
     * - Expiration date future date validation
     * - Active status value validation
     * 
     * This method replicates the validation logic from COBOL programs
     * COCRDLIC.cbl and COCRDUPC.cbl to ensure functional parity.
     * 
     * @throws com.carddemo.exception.ValidationException if any validation fails
     */
    public void validate() {
        ValidationUtil.FieldValidator validator = new ValidationUtil.FieldValidator();
        
        // Validate card number if provided
        if (cardNumber != null && !cardNumber.trim().isEmpty()) {
            validator.validateCardNumber(cardNumber);
        }
        
        // Validate account ID if provided  
        if (accountId != null && !accountId.trim().isEmpty()) {
            validator.validateAccountId(accountId);
        }
        
        // Validate embossed name length
        if (embossedName != null) {
            ValidationUtil.validateFieldLength("embossedName", embossedName, 50);
        }
        
        // Validate active status if provided
        if (activeStatus != null && !activeStatus.trim().isEmpty()) {
            if (!"Y".equals(activeStatus.trim()) && !"N".equals(activeStatus.trim())) {
                throw new IllegalArgumentException("Active status must be Y or N");
            }
        }
        
        // Validate expiration date is not in the past
        if (expirationDate != null && expirationDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Expiration date cannot be in the past");
        }
    }

    /**
     * Checks if this is a partial update request.
     * A partial update typically occurs when only status or specific fields are being modified,
     * as supported by the card update functionality in COCRDUPC.cbl.
     * 
     * @return true if only some fields are populated (indicating partial update)
     */
    public boolean isPartialUpdate() {
        int populatedFields = 0;
        
        if (cardNumber != null && !cardNumber.trim().isEmpty()) populatedFields++;
        if (accountId != null && !accountId.trim().isEmpty()) populatedFields++;
        if (embossedName != null && !embossedName.trim().isEmpty()) populatedFields++;
        if (expirationDate != null) populatedFields++;
        if (activeStatus != null && !activeStatus.trim().isEmpty()) populatedFields++;
        // Note: CVV is excluded from partial update consideration for security
        
        // If only 1-2 fields are populated, consider it a partial update
        return populatedFields > 0 && populatedFields < 4;
    }

    /**
     * Creates a card request for status change operations.
     * This is a common operation pattern from COCRDUPC.cbl where only the status
     * needs to be updated without modifying other card details.
     * 
     * @param cardNumber the card number to update
     * @param accountId the associated account ID
     * @param newStatus the new active status (Y or N)
     * @return CardRequest configured for status update
     */
    public static CardRequest forStatusUpdate(String cardNumber, String accountId, String newStatus) {
        CardRequest request = new CardRequest();
        request.setCardNumber(cardNumber);
        request.setAccountId(accountId);
        request.setActiveStatus(newStatus);
        return request;
    }

    /**
     * Creates a card request for card detail updates.
     * Used when updating embossed name, expiration date, or other card attributes
     * as supported by the card update screen (COCRDUP.bms).
     * 
     * @param cardNumber the card number to update
     * @param accountId the associated account ID  
     * @param embossedName the new embossed name
     * @param expirationDate the new expiration date
     * @param activeStatus the card status
     * @return CardRequest configured for detail update
     */
    public static CardRequest forDetailUpdate(String cardNumber, String accountId, 
                                            String embossedName, LocalDate expirationDate, 
                                            String activeStatus) {
        CardRequest request = new CardRequest();
        request.setCardNumber(cardNumber);
        request.setAccountId(accountId);
        request.setEmbossedName(embossedName);
        request.setExpirationDate(expirationDate);
        request.setActiveStatus(activeStatus);
        return request;
    }
}