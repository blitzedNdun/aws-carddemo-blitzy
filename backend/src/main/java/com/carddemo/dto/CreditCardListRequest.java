/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.carddemo.util.Constants;
import com.carddemo.util.ValidationUtil;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request DTO for credit card list query operations (CCLI transaction).
 * 
 * This DTO encapsulates the filter parameters and pagination settings for retrieving
 * credit card lists. Maps directly to the COCRDLI BMS input fields, maintaining
 * compatibility with the original COBOL program COCRDLIC.cbl validation logic.
 * 
 * Key features:
 * - Optional accountId filter for 11-digit account numbers
 * - Optional cardNumber filter for 16-digit card numbers  
 * - Pagination support for STARTBR/READNEXT replacement with Spring Data Pageable
 * - Validation matching COBOL PIC clauses and business rules
 * 
 * Validation behavior matches COBOL logic from COCRDLIC.cbl:
 * - Both accountId and cardNumber are optional filters (can be null/blank)
 * - When provided, accountId must be exactly 11 digits (matching PIC 9(11))
 * - When provided, cardNumber must be exactly 16 digits (matching PIC 9(16))
 * - Invalid formats generate validation errors matching COBOL error messages
 * 
 * This DTO supports cursor-based pagination to replicate VSAM browse operations
 * (STARTBR/READNEXT/READPREV) through Spring Data Pageable interface.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Data
public class CreditCardListRequest {
    
    /**
     * Optional account ID filter for credit card list queries.
     * 
     * Maps to ACCTSID field from COCRDLI.bms (LENGTH=11).
     * Corresponds to CC-ACCT-ID working storage field in COCRDLIC.cbl.
     * 
     * Validation rules from COBOL program 2210-EDIT-ACCOUNT:
     * - Optional field (can be null or blank) 
     * - When provided, must be exactly 11 digits
     * - Matches COBOL validation: "ACCOUNT FILTER,IF SUPPLIED MUST BE A 11 DIGIT NUMBER"
     * - Numeric validation enforced through regex pattern
     * 
     * Field length constant: {@link Constants#ACCOUNT_ID_LENGTH}
     */
    @Pattern(regexp = "^\\d{11}$", 
             message = "Account filter, if supplied must be a 11 digit number")
    private String accountId;
    
    /**
     * Optional card number filter for credit card list queries.
     * 
     * Maps to CARDSID field from COCRDLI.bms (LENGTH=16).
     * Corresponds to CC-CARD-NUM working storage field in COCRDLIC.cbl.
     * 
     * Validation rules from COBOL program 2220-EDIT-CARD:
     * - Optional field (can be null or blank)
     * - When provided, must be exactly 16 digits  
     * - Matches COBOL validation: "CARD ID FILTER,IF SUPPLIED MUST BE A 16 DIGIT NUMBER"
     * - Numeric validation enforced through regex pattern
     * 
     * Field length constant: {@link Constants#CARD_NUMBER_LENGTH}
     */
    @Pattern(regexp = "^\\d{16}$",
             message = "Card ID filter, if supplied must be a 16 digit number") 
    private String cardNumber;
    
    /**
     * Page number for pagination support.
     * 
     * Replaces VSAM STARTBR/READNEXT cursor positioning with Spring Data pagination.
     * Maps to the screen numbering logic in COCRDLIC.cbl (WS-CA-SCREEN-NUM).
     * 
     * Zero-based page numbering:
     * - Page 0 = first page
     * - Page 1 = second page, etc.
     * 
     * Default value: 0 (first page)
     */
    private Integer pageNumber = 0;
    
    /**
     * Number of records per page for pagination.
     * 
     * Replaces COBOL WS-MAX-SCREEN-LINES (value 7) with configurable page size.
     * Supports flexible page sizes while maintaining reasonable limits.
     * 
     * Default value: 10 (matching modern pagination standards)
     * Maximum recommended: 100 (to prevent performance issues)
     */
    private Integer pageSize = 10;
    
    /**
     * Validates the request using ValidationUtil methods.
     * 
     * This method provides programmatic validation that supplements the 
     * Jakarta Bean Validation annotations. Uses the same validation logic
     * as the COBOL program for consistency.
     * 
     * Note: The ValidationUtil methods expect non-null values, so this method
     * only validates when fields are present, matching the COBOL optional logic.
     */
    public void validate() {
        ValidationUtil.FieldValidator validator = new ValidationUtil.FieldValidator();
        
        // Validate accountId if provided (matching COBOL optional logic)
        if (accountId != null && !accountId.trim().isEmpty()) {
            validator.validateAccountId(accountId.trim());
        }
        
        // Validate cardNumber if provided (matching COBOL optional logic)  
        if (cardNumber != null && !cardNumber.trim().isEmpty()) {
            validator.validateCardNumber(cardNumber.trim());
        }
        
        // Validate pagination parameters
        if (pageNumber != null && pageNumber < 0) {
            throw new IllegalArgumentException("Page number must be non-negative");
        }
        
        if (pageSize != null && (pageSize < 1 || pageSize > 100)) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
    }
    
    /**
     * Determines if any filter criteria are specified.
     * 
     * Used by service layer to determine query strategy:
     * - If filters present: execute filtered query  
     * - If no filters: execute unfiltered query (admin users only per COBOL logic)
     * 
     * @return true if either accountId or cardNumber filters are specified
     */
    public boolean hasFilters() {
        return (accountId != null && !accountId.trim().isEmpty()) ||
               (cardNumber != null && !cardNumber.trim().isEmpty());
    }
    
    /**
     * Gets the effective page number, ensuring non-null value.
     * 
     * @return the page number, defaulting to 0 if null
     */
    public int getEffectivePageNumber() {
        return pageNumber != null ? pageNumber : 0;
    }
    
    /**
     * Gets the effective page size, ensuring non-null value.
     * 
     * @return the page size, defaulting to 10 if null
     */
    public int getEffectivePageSize() {
        return pageSize != null ? pageSize : 10;
    }
    
    /**
     * Normalizes the account ID filter by trimming whitespace.
     * 
     * @return trimmed account ID or null if empty
     */
    public String getNormalizedAccountId() {
        if (accountId == null || accountId.trim().isEmpty()) {
            return null;
        }
        return accountId.trim();
    }
    
    /**
     * Normalizes the card number filter by trimming whitespace.
     * 
     * @return trimmed card number or null if empty  
     */
    public String getNormalizedCardNumber() {
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            return null;
        }
        return cardNumber.trim();
    }
}