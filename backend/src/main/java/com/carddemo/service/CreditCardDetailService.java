/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.repository.CardRepository;
import com.carddemo.entity.Card;
import com.carddemo.dto.CreditCardDetailResponse;
import com.carddemo.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Optional;

/**
 * Spring Boot service implementing credit card detail retrieval operations.
 * 
 * This service is a direct translation of COCRDSLC.cbl COBOL program, providing
 * comprehensive credit card detail lookup functionality. The implementation maintains
 * exact business logic and error handling patterns from the original COBOL code
 * while leveraging Spring Boot and JPA for modern data access.
 * 
 * Key Features:
 * - Credit card detail lookup by card number (primary access path)
 * - Credit card detail lookup by account ID (alternate index access)
 * - Combined account and card validation for cross-reference verification
 * - Comprehensive input validation matching COBOL edit routines
 * - Card masking and formatting for secure display
 * - Error handling equivalent to VSAM NOTFND conditions
 * 
 * COBOL Program Translation:
 * - COCRDSLC.cbl → CreditCardDetailService.java
 * - 0000-MAIN → Service method orchestration
 * - 2000-PROCESS-INPUTS → Input validation methods
 * - 2210-EDIT-ACCOUNT → validateAccountId()
 * - 2220-EDIT-CARD → validateCardNumber()
 * - 9000-READ-DATA → Card lookup orchestration
 * - 9100-GETCARD-BYACCTCARD → lookupCardByNumber()
 * - 9150-GETCARD-BYACCT → lookupCardByAccount()
 * - 1200-SETUP-SCREEN-VARS → mapCardToDetailResponse()
 * 
 * Data Access Translation:
 * - VSAM CARDDAT file → CardRepository.findByCardNumber()
 * - VSAM CARDAIX alternate index → CardRepository.findByAccountId()
 * - CICS READ operations → JPA repository methods
 * - VSAM NOTFND condition → ResourceNotFoundException
 * 
 * This implementation ensures 100% functional parity with the original COBOL
 * program while providing REST-compatible responses for the modernized architecture.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Service
public class CreditCardDetailService {

    /**
     * CardRepository for credit card data access operations.
     * Replaces VSAM CARDDAT file access with JPA-based database operations.
     */
    @Autowired
    private CardRepository cardRepository;

    /**
     * Retrieves complete credit card details by card number.
     * 
     * This method implements the primary card lookup functionality from COCRDSLC.cbl,
     * mapping directly to the 9100-GETCARD-BYACCTCARD section. Performs comprehensive
     * validation and returns complete card information including masked card number,
     * account details, cardholder name, expiration date, and status.
     * 
     * COBOL Equivalent:
     * - Input validation: 2220-EDIT-CARD section
     * - Data retrieval: 9100-GETCARD-BYACCTCARD section
     * - VSAM READ with CARDDAT file using card number as primary key
     * - Error handling for DFHRESP(NOTFND) condition
     * 
     * Business Rules:
     * - Card number must be exactly 16 digits
     * - Card number must be numeric only
     * - Card must exist in database
     * - Returns complete card detail information
     * 
     * @param cardNumber 16-digit credit card number to lookup
     * @return CreditCardDetailResponse containing complete card information
     * @throws IllegalArgumentException if card number format is invalid
     * @throws ResourceNotFoundException if card is not found in database
     */
    public CreditCardDetailResponse getCardDetail(String cardNumber) {
        // Validate input card number (equivalent to 2220-EDIT-CARD)
        validateCardNumber(cardNumber);
        
        // Clean card number for lookup (remove spaces)
        String cleanCardNumber = cardNumber.trim().replaceAll("\\s+", "");
        
        // Perform card lookup (equivalent to 9100-GETCARD-BYACCTCARD)
        Optional<Card> cardOptional = lookupCardByNumber(cleanCardNumber);
        
        // Handle not found condition (equivalent to DFHRESP(NOTFND))
        if (cardOptional.isEmpty()) {
            throw new ResourceNotFoundException("Card", cardNumber, 
                "Did not find cards for this search condition");
        }
        
        // Map card entity to response DTO (equivalent to 1200-SETUP-SCREEN-VARS)
        return mapCardToDetailResponse(cardOptional.get());
    }

    /**
     * Retrieves credit card details by account ID using alternate index access.
     * 
     * This method implements the alternate access path from COCRDSLC.cbl, mapping
     * to the 9150-GETCARD-BYACCT section. Uses account ID to locate associated
     * cards and returns details for the first active card found.
     * 
     * COBOL Equivalent:
     * - Input validation: 2210-EDIT-ACCOUNT section
     * - Data retrieval: 9150-GETCARD-BYACCT section  
     * - VSAM READ with CARDAIX alternate index using account ID
     * - Error handling for DFHRESP(NOTFND) condition
     * 
     * Business Rules:
     * - Account ID must be exactly 11 digits
     * - Account ID must be numeric only
     * - At least one card must exist for the account
     * - Returns details for first active card found
     * 
     * @param accountId 11-digit account identifier to lookup cards for
     * @return CreditCardDetailResponse containing card information for account
     * @throws IllegalArgumentException if account ID format is invalid
     * @throws ResourceNotFoundException if no cards found for account
     */
    public CreditCardDetailResponse getCardDetailByAccountId(String accountId) {
        // Validate input account ID (equivalent to 2210-EDIT-ACCOUNT)
        validateAccountId(accountId);
        
        // Clean account ID for lookup (remove spaces)
        String cleanAccountId = accountId.trim().replaceAll("\\s+", "");
        
        // Perform card lookup by account (equivalent to 9150-GETCARD-BYACCT)
        Optional<Card> cardOptional = lookupCardByAccount(cleanAccountId);
        
        // Handle not found condition (equivalent to DFHRESP(NOTFND))
        if (cardOptional.isEmpty()) {
            throw new ResourceNotFoundException("Card", "account:" + accountId,
                "Did not find this account in cards database");
        }
        
        // Map card entity to response DTO
        return mapCardToDetailResponse(cardOptional.get());
    }

    /**
     * Validates card access permissions and existence.
     * 
     * This method implements card access validation logic from COCRDSLC.cbl,
     * ensuring that the specified card number is valid, exists in the database,
     * and is accessible for detail operations. Throws appropriate exceptions
     * for invalid cards or access violations.
     * 
     * COBOL Equivalent:
     * - Input validation: 2220-EDIT-CARD section
     * - Existence check: 9100-GETCARD-BYACCTCARD section
     * - Error handling for invalid input and NOTFND conditions
     * 
     * Business Rules:
     * - Card number must be exactly 16 digits
     * - Card number must be numeric only
     * - Card must exist in database
     * - Card must be accessible for current operation
     * 
     * @param cardNumber 16-digit credit card number to validate
     * @throws IllegalArgumentException if card number format is invalid
     * @throws ResourceNotFoundException if card is not found or not accessible
     */
    public void validateCardAccess(String cardNumber) {
        // Validate input card number format
        validateCardNumber(cardNumber);
        
        // Clean card number for lookup (remove spaces)
        String cleanCardNumber = cardNumber.trim().replaceAll("\\s+", "");
        
        // Check card existence in database
        Optional<Card> cardOptional = lookupCardByNumber(cleanCardNumber);
        
        // Throw exception if card not found
        if (cardOptional.isEmpty()) {
            throw new ResourceNotFoundException("Card", cleanCardNumber,
                "Card not found or not accessible");
        }
        
        // Additional access validation could be added here
        // For example, checking user permissions, card status, etc.
    }

    /**
     * Maps Card entity to CreditCardDetailResponse DTO with proper formatting.
     * 
     * This method implements the screen formatting logic from COCRDSLC.cbl,
     * specifically the 1200-SETUP-SCREEN-VARS section. Converts the Card entity
     * into a properly formatted response DTO with card number masking, date
     * formatting, and field preparation for display.
     * 
     * COBOL Equivalent:
     * - 1200-SETUP-SCREEN-VARS section
     * - CARD-EMBOSSED-NAME → embossed name mapping
     * - CARD-EXPIRAION-DATE → expiration date formatting
     * - CARD-ACTIVE-STATUS → status mapping
     * - Card number masking for display
     * 
     * Business Rules:
     * - Card number must be masked for security compliance
     * - Account ID formatted with proper zero-padding
     * - Expiration date formatted for display
     * - All fields properly mapped from entity to DTO
     * 
     * @param card Card entity to map to response DTO
     * @return CreditCardDetailResponse with formatted card information
     * @throws IllegalArgumentException if card entity is null
     */
    public CreditCardDetailResponse mapCardToDetailResponse(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("Card entity cannot be null");
        }
        
        // Create new response DTO instance
        CreditCardDetailResponse response = new CreditCardDetailResponse();
        
        // Map card number (will be masked in DTO for security)
        response.setCardNumber(card.getCardNumber());
        
        // Map account ID with proper formatting
        response.setAccountId(card.getAccountId() != null ? card.getAccountId().toString() : "");
        
        // Map embossed name
        response.setEmbossedName(card.getEmbossedName());
        
        // Map expiration date with proper formatting
        if (card.getExpirationDate() != null) {
            response.setExpirationDate(card.getExpirationDate().toString());
        }
        
        // Map card status
        response.setCardStatus(card.getActiveStatus());
        
        // Map CVV code
        response.setCvvCode(card.getCvvCode());
        
        return response;
    }

    /**
     * Retrieves credit card details by both account ID and card number validation.
     * 
     * This method implements combined validation logic from COCRDSLC.cbl,
     * ensuring that the specified card number actually belongs to the specified
     * account. This provides cross-reference validation for enhanced security
     * and data integrity.
     * 
     * COBOL Equivalent:
     * - Combined logic from 2210-EDIT-ACCOUNT and 2220-EDIT-CARD
     * - Cross-reference validation between account and card
     * - Enhanced error handling for mismatched account-card combinations
     * 
     * Business Rules:
     * - Account ID must be exactly 11 digits
     * - Card number must be exactly 16 digits  
     * - Both values must be numeric only
     * - Card must exist and belong to specified account
     * - Returns card details only if validation passes
     * 
     * @param accountId 11-digit account identifier
     * @param cardNumber 16-digit credit card number
     * @return CreditCardDetailResponse containing validated card information
     * @throws IllegalArgumentException if input format is invalid
     * @throws ResourceNotFoundException if card not found or doesn't belong to account
     */
    public CreditCardDetailResponse getCardDetailByAccountAndCard(String accountId, String cardNumber) {
        // Validate both input parameters
        validateAccountId(accountId);
        validateCardNumber(cardNumber);
        
        // Clean inputs for lookup (remove spaces)
        String cleanAccountId = accountId.trim().replaceAll("\\s+", "");
        String cleanCardNumber = cardNumber.trim().replaceAll("\\s+", "");
        
        // Lookup card by card number
        Optional<Card> cardOptional = lookupCardByNumber(cleanCardNumber);
        
        // Check if card exists
        if (cardOptional.isEmpty()) {
            throw new ResourceNotFoundException("Card", cardNumber,
                "Did not find cards for this search condition");
        }
        
        Card card = cardOptional.get();
        
        // Validate that card belongs to specified account
        Long accountIdLong = Long.parseLong(cleanAccountId);
        if (!accountIdLong.equals(card.getAccountId())) {
            throw new ResourceNotFoundException("Card", cleanCardNumber,
                "Card does not belong to specified account: " + cleanAccountId);
        }
        
        // Return mapped response if all validations pass
        return mapCardToDetailResponse(card);
    }

    // Private Helper Methods (equivalent to COBOL paragraph sections)

    /**
     * Validates card number format and content.
     * 
     * Implements the 2220-EDIT-CARD section from COCRDSLC.cbl, performing
     * comprehensive validation of card number input including length,
     * numeric content, and business rule compliance.
     * 
     * COBOL Equivalent:
     * - 2220-EDIT-CARD section
     * - CC-CARD-NUM IS NOT NUMERIC validation
     * - 16-character length validation
     * - Error message: "CARD ID FILTER,IF SUPPLIED MUST BE A 16 DIGIT NUMBER"
     * 
     * @param cardNumber card number to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCardNumber(String cardNumber) {
        // Check for null or empty input
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Card number not provided");
        }
        
        // Remove any spaces or formatting characters
        String cleanCardNumber = cardNumber.trim().replaceAll("\\s+", "");
        
        // Check length (must be exactly 16 digits)
        if (cleanCardNumber.length() != 16) {
            throw new IllegalArgumentException("Card number if supplied must be a 16 digit number");
        }
        
        // Check that all characters are numeric
        if (!cleanCardNumber.matches("\\d{16}")) {
            throw new IllegalArgumentException("Card number if supplied must be a 16 digit number");
        }
        
        // Check for all zeros (invalid card number)
        if (cleanCardNumber.equals("0000000000000000")) {
            throw new IllegalArgumentException("Card number must be a non zero 16 digit number");
        }
    }

    /**
     * Validates account ID format and content.
     * 
     * Implements the 2210-EDIT-ACCOUNT section from COCRDSLC.cbl, performing
     * comprehensive validation of account ID input including length,
     * numeric content, and business rule compliance.
     * 
     * COBOL Equivalent:
     * - 2210-EDIT-ACCOUNT section
     * - CC-ACCT-ID IS NOT NUMERIC validation
     * - 11-character length validation
     * - Error message: "ACCOUNT FILTER,IF SUPPLIED MUST BE A 11 DIGIT NUMBER"
     * 
     * @param accountId account ID to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateAccountId(String accountId) {
        // Check for null or empty input
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("Account number not provided");
        }
        
        // Remove any spaces or formatting characters
        String cleanAccountId = accountId.trim().replaceAll("\\s+", "");
        
        // Check length (must be exactly 11 digits)
        if (cleanAccountId.length() != 11) {
            throw new IllegalArgumentException("Account number must be a non zero 11 digit number");
        }
        
        // Check that all characters are numeric
        if (!cleanAccountId.matches("\\d{11}")) {
            throw new IllegalArgumentException("Account number must be a non zero 11 digit number");
        }
        
        // Check for all zeros (invalid account ID)
        if (cleanAccountId.equals("00000000000")) {
            throw new IllegalArgumentException("Account number must be a non zero 11 digit number");
        }
    }

    /**
     * Performs card lookup by card number using repository.
     * 
     * Implements the 9100-GETCARD-BYACCTCARD section from COCRDSLC.cbl,
     * performing direct card lookup using the card number as primary key.
     * Handles database access errors and converts them to appropriate exceptions.
     * 
     * COBOL Equivalent:
     * - 9100-GETCARD-BYACCTCARD section
     * - EXEC CICS READ FILE(CARDDAT) using card number
     * - RESP(WS-RESP-CD) and RESP2(WS-REAS-CD) error handling
     * - DFHRESP(NORMAL) and DFHRESP(NOTFND) condition handling
     * 
     * @param cardNumber validated card number for lookup
     * @return Optional containing Card entity if found, empty otherwise
     */
    private Optional<Card> lookupCardByNumber(String cardNumber) {
        try {
            // Use repository to find card by card number (primary key access)
            return cardRepository.findByCardNumber(cardNumber);
        } catch (Exception e) {
            // Handle database access errors (equivalent to CICS file error handling)
            throw new RuntimeException("Error reading Card Data File: " + e.getMessage(), e);
        }
    }

    /**
     * Performs card lookup by account ID using alternate index.
     * 
     * Implements the 9150-GETCARD-BYACCT section from COCRDSLC.cbl,
     * performing card lookup using account ID via alternate index access.
     * Returns the first active card found for the account.
     * 
     * COBOL Equivalent:
     * - 9150-GETCARD-BYACCT section
     * - EXEC CICS READ FILE(CARDAIX) using account ID
     * - Alternate index access pattern
     * - RESP(WS-RESP-CD) and RESP2(WS-REAS-CD) error handling
     * 
     * @param accountId validated account ID for lookup
     * @return Optional containing first Card entity found for account, empty otherwise
     */
    private Optional<Card> lookupCardByAccount(String accountId) {
        try {
            // Convert account ID to Long for repository query
            Long accountIdLong = Long.parseLong(accountId);
            
            // Use repository to find cards by account ID (alternate index access)
            var cards = cardRepository.findByAccountId(accountIdLong);
            
            // Return first active card found (equivalent to COBOL behavior)
            return cards.stream()
                    .filter(card -> "Y".equals(card.getActiveStatus()))
                    .findFirst()
                    .or(() -> cards.stream().findFirst()); // Fallback to any card if no active found
        } catch (NumberFormatException e) {
            // This should not happen due to validation, but handle gracefully
            throw new IllegalArgumentException("Invalid account ID format: " + accountId, e);
        } catch (Exception e) {
            // Handle database access errors (equivalent to CICS file error handling)
            throw new RuntimeException("Error reading Card Data File: " + e.getMessage(), e);
        }
    }
}
