/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.dto.*;
import com.carddemo.entity.Card;
import com.carddemo.repository.CardRepository;
import com.carddemo.exception.BusinessRuleException;
import com.carddemo.exception.ResourceNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * Credit Card Service implementation for card management operations.
 * 
 * Consolidates functionality from COBOL programs COCRDLIC, COCRDSLC, and COCRDUPC
 * into a unified Spring Boot service class, providing:
 * - Card listing with pagination and filtering (CCLI transaction)
 * - Card details retrieval (CCDL transaction)
 * - Card updates and validation (CCUP transaction)
 * - Account-card cross-reference validation
 * 
 * Business Logic Implementation:
 * - Preserves COBOL edit routines and validation rules
 * - Maintains pagination patterns (7 records per page)
 * - Implements card number masking for security
 * - Enforces business rules for card updates
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Service
@Transactional
public class CreditCardService {

    private static final Logger logger = LoggerFactory.getLogger(CreditCardService.class);

    @Autowired
    private CardRepository cardRepository;

    /**
     * Lists credit cards with pagination and optional filtering.
     * Implements CCLI transaction functionality from COCRDLIC COBOL program.
     * 
     * @param accountId Optional account ID filter (11 digits)
     * @param cardNumber Optional card number filter (16 digits)
     * @param page Page number (0-based)
     * @param size Page size (default 7 to match COBOL screen)
     * @return PageResponse containing CardListDto items
     * @throws BusinessRuleException if validation fails
     */
    @Transactional(readOnly = true)
    public PageResponse<CardListDto> listCards(String accountId, String cardNumber, int page, int size) {
        logger.info("Processing card list request - accountId: {}, cardNumber: {}, page: {}, size: {}", 
                   accountId, maskCardNumber(cardNumber), page, size);

        try {
            // Create pageable request
            Pageable pageable = PageRequest.of(page, size);
            
            // Apply filters based on provided parameters
            Page<Card> cardPage;
            
            if (accountId != null && cardNumber != null) {
                // Both filters provided
                cardPage = cardRepository.findByAccountIdAndCardNumberContaining(Long.parseLong(accountId), cardNumber, pageable);
            } else if (accountId != null) {
                // Account filter only
                cardPage = cardRepository.findByAccountId(Long.parseLong(accountId), pageable);
            } else if (cardNumber != null) {
                // Card number filter only
                cardPage = cardRepository.findByCardNumberContaining(cardNumber, pageable);
            } else {
                // No filters - return all cards
                cardPage = cardRepository.findAllOrderedByCardNumber(pageable);
            }

            // Convert Card entities to CardListDto objects
            List<CardListDto> cardList = cardPage.getContent().stream()
                    .map(this::convertToCardListDto)
                    .collect(Collectors.toList());

            // Create and return PageResponse
            return new PageResponse<>(cardList, page, size, cardPage.getTotalElements());

        } catch (Exception e) {
            logger.error("Error processing card list request", e);
            throw new BusinessRuleException("CARD_LIST_ERROR", "Error retrieving card list: " + e.getMessage());
        }
    }

    /**
     * Retrieves detailed information for a specific credit card.
     * Implements CCDL transaction functionality from COCRDSLC COBOL program.
     * 
     * @param cardNumber 16-digit card number
     * @return CardResponse with complete card details
     * @throws ResourceNotFoundException if card not found
     * @throws BusinessRuleException if validation fails
     */
    @Transactional(readOnly = true)
    public CardResponse getCardDetails(String cardNumber) {
        logger.info("Processing card detail request for cardNumber: {}", maskCardNumber(cardNumber));

        try {
            // Retrieve card from repository
            Optional<Card> cardOpt = cardRepository.findByCardNumber(cardNumber);
            
            if (cardOpt.isEmpty()) {
                throw new ResourceNotFoundException("Card", cardNumber);
            }

            Card card = cardOpt.get();
            
            // Convert to CardResponse DTO
            return convertToCardResponse(card);

        } catch (ResourceNotFoundException e) {
            logger.warn("Card not found: {}", maskCardNumber(cardNumber));
            throw e;
        } catch (Exception e) {
            logger.error("Error retrieving card details for cardNumber: {}", maskCardNumber(cardNumber), e);
            throw new BusinessRuleException("CARD_DETAIL_ERROR", "Error retrieving card details: " + e.getMessage());
        }
    }

    /**
     * Updates credit card information with validation.
     * Implements CCUP transaction functionality from COCRDUPC COBOL program.
     * 
     * @param cardNumber 16-digit card number
     * @param cardRequest Update request with new field values
     * @return Updated CardResponse
     * @throws ResourceNotFoundException if card not found
     * @throws BusinessRuleException if validation fails
     */
    public CardResponse updateCard(String cardNumber, CardRequest cardRequest) {
        logger.info("Processing card update request for cardNumber: {}", maskCardNumber(cardNumber));

        try {
            // Retrieve existing card
            Optional<Card> cardOpt = cardRepository.findByCardNumber(cardNumber);
            
            if (cardOpt.isEmpty()) {
                throw new ResourceNotFoundException("Card", cardNumber);
            }

            Card card = cardOpt.get();
            
            // Apply updates from request
            updateCardFromRequest(card, cardRequest);
            
            // Validate business rules
            validateCardUpdate(card, cardRequest);
            
            // Save updated card
            Card updatedCard = cardRepository.save(card);
            
            logger.info("Successfully updated card: {}", maskCardNumber(cardNumber));
            
            // Return updated card details
            return convertToCardResponse(updatedCard);

        } catch (ResourceNotFoundException e) {
            logger.warn("Card not found for update: {}", maskCardNumber(cardNumber));
            throw e;
        } catch (Exception e) {
            logger.error("Error updating card: {}", maskCardNumber(cardNumber), e);
            throw new BusinessRuleException("CARD_UPDATE_ERROR", "Error updating card: " + e.getMessage());
        }
    }

    /**
     * Validates card-account cross-reference relationship.
     * Ensures the card belongs to the specified account.
     * 
     * @param cardNumber 16-digit card number
     * @param accountId 11-digit account identifier
     * @throws BusinessRuleException if validation fails
     */
    @Transactional(readOnly = true)
    public void validateCardAccountXref(String cardNumber, String accountId) {
        logger.debug("Validating card-account cross-reference - cardNumber: {}, accountId: {}", 
                    maskCardNumber(cardNumber), accountId);

        try {
            boolean exists = cardRepository.existsByCardNumberAndAccountId(cardNumber, Long.parseLong(accountId));
            
            if (!exists) {
                throw new BusinessRuleException("CARD_ACCOUNT_MISMATCH", 
                    "Card does not belong to the specified account");
            }

        } catch (BusinessRuleException e) {
            logger.warn("Card-account validation failed - cardNumber: {}, accountId: {}", 
                       maskCardNumber(cardNumber), accountId);
            throw e;
        } catch (Exception e) {
            logger.error("Error validating card-account cross-reference", e);
            throw new BusinessRuleException("CARD_XREF_ERROR", "Error validating card-account relationship");
        }
    }

    /**
     * Converts Card entity to CardListDto for list operations.
     * 
     * @param card Card entity
     * @return CardListDto for API response
     */
    private CardListDto convertToCardListDto(Card card) {
        // Create masked card number for security
        String maskedCardNumber = maskCardNumber(card.getCardNumber());
        
        return CardListDto.builder()
                .maskedCardNumber(maskedCardNumber)
                .accountId(card.getAccountId().toString())
                .cardType(determineCardType(card.getCardNumber()))
                .expirationDate(card.getExpirationDate())
                .activeStatus(card.getActiveStatus())
                .selectionFlag(false)
                .build();
    }

    /**
     * Converts Card entity to CardResponse for detail operations.
     * 
     * @param card Card entity
     * @return CardResponse for API response
     */
    private CardResponse convertToCardResponse(Card card) {
        CardResponse response = new CardResponse();
        
        // Set card number (will be automatically masked)
        response.setCardNumber(card.getCardNumber());
        response.setAccountId(card.getAccountId().toString());
        response.setEmbossedName(card.getEmbossedName());
        response.setExpirationDate(card.getExpirationDate());
        response.setActiveStatus(card.getActiveStatus());
        response.setCardType(determineCardType(card.getCardNumber()));
        
        return response;
    }

    /**
     * Updates Card entity from CardRequest data.
     * 
     * @param card Card entity to update
     * @param request Update request data
     */
    private void updateCardFromRequest(Card card, CardRequest request) {
        // Update embossed name if provided
        if (request.getEmbossedName() != null) {
            card.setEmbossedName(request.getEmbossedName().trim());
        }
        
        // Update expiration date if provided
        if (request.getExpirationDate() != null) {
            card.setExpirationDate(request.getExpirationDate());
        }
        
        // Update active status if provided
        if (request.getActiveStatus() != null) {
            card.setActiveStatus(request.getActiveStatus().trim().toUpperCase());
        }
    }

    /**
     * Validates business rules for card updates.
     * 
     * @param card Card entity being updated
     * @param request Update request data
     * @throws BusinessRuleException if validation fails
     */
    private void validateCardUpdate(Card card, CardRequest request) {
        // Validate embossed name length
        if (card.getEmbossedName() != null && card.getEmbossedName().length() > 50) {
            throw new BusinessRuleException("NAME_TOO_LONG", "Embossed name cannot exceed 50 characters");
        }
        
        // Validate active status
        if (card.getActiveStatus() != null && 
            !"Y".equals(card.getActiveStatus()) && !"N".equals(card.getActiveStatus())) {
            throw new BusinessRuleException("INVALID_STATUS", "Active status must be 'Y' or 'N'");
        }
        
        // Additional business rule validations can be added here
    }

    /**
     * Determines card type from card number BIN range.
     * 
     * @param cardNumber 16-digit card number
     * @return Card type string (VISA, MASTERCARD, AMEX, DISCOVER, UNKNOWN)
     */
    private String determineCardType(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 2) {
            return "UNKNOWN";
        }
        
        String firstTwo = cardNumber.substring(0, 2);
        int firstTwoInt = Integer.parseInt(firstTwo);
        
        // VISA: 4xxx
        if (cardNumber.startsWith("4")) {
            return "VISA";
        }
        // MASTERCARD: 51-55, 2221-2720
        else if (firstTwoInt >= 51 && firstTwoInt <= 55) {
            return "MASTERCARD";
        }
        // AMEX: 34, 37
        else if (cardNumber.startsWith("34") || cardNumber.startsWith("37")) {
            return "AMEX";
        }
        // DISCOVER: 6011, 622126-622925, 644-649, 65
        else if (cardNumber.startsWith("6011") || cardNumber.startsWith("65")) {
            return "DISCOVER";
        }
        else {
            return "UNKNOWN";
        }
    }

    /**
     * Masks card number for security logging and display.
     * Shows only last 4 digits in ****-****-****-1234 format.
     * 
     * @param cardNumber Card number to mask
     * @return Masked card number string
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****-****-****-****";
        }
        
        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "****-****-****-" + lastFour;
    }
}