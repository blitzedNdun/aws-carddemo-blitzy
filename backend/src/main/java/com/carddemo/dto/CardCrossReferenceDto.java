/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.carddemo.util.ValidationUtil;
import com.carddemo.util.Constants;

/**
 * Data Transfer Object representing card cross-reference information
 * mapping to CVACT03Y CARD-XREF-RECORD copybook structure.
 * 
 * This DTO facilitates card-to-account linkage operations and cross-reference 
 * validation, maintaining referential integrity between cards, customers, and accounts.
 * 
 * The class maps directly to the COBOL CARD-XREF-RECORD structure:
 * - XREF-CARD-NUM (PIC X(16)) -> cardNumber
 * - XREF-CUST-ID (PIC 9(09)) -> customerId  
 * - XREF-ACCT-ID (PIC 9(11)) -> accountId
 * 
 * Field lengths and validation rules preserve exact COBOL business logic
 * to ensure functional parity during the mainframe modernization.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardCrossReferenceDto {

    // Customer ID length constant for COBOL PIC 9(09) specification
    private static final int CUSTOMER_ID_LENGTH = 9;

    /**
     * Card number field mapping to XREF-CARD-NUM PIC X(16).
     * Must be exactly 16 characters representing a valid credit card number.
     */
    @JsonProperty("cardNumber")
    @Size(min = Constants.CARD_NUMBER_LENGTH, max = Constants.CARD_NUMBER_LENGTH, 
          message = "Card number must be exactly 16 characters")
    private String cardNumber;

    /**
     * Customer ID field mapping to XREF-CUST-ID PIC 9(09).
     * Must be exactly 9 digits representing a valid customer identifier.
     */
    @JsonProperty("customerId")
    @Size(min = CUSTOMER_ID_LENGTH, max = CUSTOMER_ID_LENGTH, 
          message = "Customer ID must be exactly 9 digits")
    private String customerId;

    /**
     * Account ID field mapping to XREF-ACCT-ID PIC 9(11).
     * Must be exactly 11 digits representing a valid account identifier.
     */
    @JsonProperty("accountId")
    @Size(min = Constants.ACCOUNT_ID_LENGTH, max = Constants.ACCOUNT_ID_LENGTH, 
          message = "Account ID must be exactly 11 digits")
    private String accountId;

    /**
     * Validates all fields in the cross-reference record using COBOL validation rules.
     * This method replicates the validation logic from the original COBOL implementation
     * to ensure data integrity and referential constraints are maintained.
     * 
     * @throws ValidationException if any field validation fails
     */
    public void validate() {
        // Validate required fields are present
        ValidationUtil.validateRequiredField("cardNumber", this.cardNumber);
        ValidationUtil.validateRequiredField("customerId", this.customerId);
        ValidationUtil.validateRequiredField("accountId", this.accountId);
        
        // Use ValidationUtil field validator for detailed validation
        ValidationUtil.FieldValidator validator = new ValidationUtil.FieldValidator();
        
        // Validate card number format and length
        validator.validateCardNumber(this.cardNumber);
        
        // Validate account ID format and length
        validator.validateAccountId(this.accountId);
        
        // Validate customer ID format (must be 9 digits)
        if (this.customerId != null && !this.customerId.trim().isEmpty()) {
            String cleanCustomerId = this.customerId.trim();
            if (!cleanCustomerId.matches("^\\d{9}$")) {
                throw new RuntimeException("Customer ID must be exactly 9 digits");
            }
        }
    }

    /**
     * Checks if this cross-reference record represents a valid card-to-account linkage.
     * 
     * @return true if all required fields are present and valid
     */
    public boolean isValidCrossReference() {
        try {
            validate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Creates a cross-reference key for bidirectional lookups.
     * Used in service layer operations for efficient cross-reference queries.
     * 
     * @return formatted key string for cross-reference operations
     */
    public String getCrossReferenceKey() {
        if (cardNumber == null || customerId == null || accountId == null) {
            return null;
        }
        return String.format("%s-%s-%s", cardNumber.trim(), customerId.trim(), accountId.trim());
    }

    /**
     * Determines if this cross-reference matches the specified card number.
     * Used for card-to-account lookup operations.
     * 
     * @param searchCardNumber the card number to match against
     * @return true if the card numbers match
     */
    public boolean matchesCardNumber(String searchCardNumber) {
        if (this.cardNumber == null || searchCardNumber == null) {
            return false;
        }
        return this.cardNumber.trim().equals(searchCardNumber.trim());
    }

    /**
     * Determines if this cross-reference matches the specified customer ID.
     * Used for customer-to-card lookup operations.
     * 
     * @param searchCustomerId the customer ID to match against
     * @return true if the customer IDs match
     */
    public boolean matchesCustomerId(String searchCustomerId) {
        if (this.customerId == null || searchCustomerId == null) {
            return false;
        }
        return this.customerId.trim().equals(searchCustomerId.trim());
    }

    /**
     * Determines if this cross-reference matches the specified account ID.
     * Used for account-to-card lookup operations.
     * 
     * @param searchAccountId the account ID to match against
     * @return true if the account IDs match
     */
    public boolean matchesAccountId(String searchAccountId) {
        if (this.accountId == null || searchAccountId == null) {
            return false;
        }
        return this.accountId.trim().equals(searchAccountId.trim());
    }
}