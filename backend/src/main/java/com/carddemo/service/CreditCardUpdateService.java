/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.repository.CardRepository;
import com.carddemo.entity.Card;
import com.carddemo.dto.CreditCardUpdateRequest;
import com.carddemo.dto.CreditCardUpdateResponse;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.exception.ConcurrencyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.util.Optional;
import jakarta.persistence.OptimisticLockException;
import java.time.LocalDateTime;

/**
 * Spring Boot service implementing credit card update operations translated from COCRDUPC.cbl.
 * 
 * This service class represents a complete translation of the COBOL program COCRDUPC.CBL,
 * which manages credit card update operations including card status changes, credit limit
 * adjustments, expiration date updates, and cardholder information modifications.
 * 
 * Key Features:
 * - Complete validation rule preservation from COBOL edit routines
 * - Optimistic locking implementation replacing CICS READ UPDATE behavior
 * - Change detection logic matching COBOL comparison routines
 * - Comprehensive error handling for all failure scenarios
 * - Transaction management replacing CICS SYNCPOINT operations
 * 
 * COBOL Program Structure Translation:
 * - 0000-MAIN → Service entry point with transaction coordination
 * - 1000-PROCESS-INPUTS → validateRequest() method
 * - 1200-EDIT-MAP-INPUTS → Input validation with business rules
 * - 2000-DECIDE-ACTION → Business logic flow control
 * - 9000-READ-DATA → readCardWithLocking() method
 * - 9200-WRITE-PROCESSING → updateCard() method with optimistic locking
 * - 9300-CHECK-CHANGE-IN-REC → detectChanges() method
 * 
 * Validation Rules (from COBOL lines 806-947):
 * - Card name: alphabetic characters and spaces only (max 50 chars)
 * - Active status: must be 'Y' or 'N'
 * - Expiry month: must be 1-12
 * - Expiry year: must be 1950-2099
 * - Change detection: prevents unnecessary updates
 * 
 * Error Handling (from COBOL error conditions):
 * - ResourceNotFoundException for VSAM NOTFND conditions
 * - ConcurrencyException for READ UPDATE conflicts
 * - Validation errors for business rule violations
 * - Database operation failures
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Service
@Transactional
public class CreditCardUpdateService {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CreditCardUpdateService.class);
    
    private final CardRepository cardRepository;

    /**
     * Constructor for dependency injection.
     * 
     * @param cardRepository Spring Data JPA repository for Card entity operations
     */
    public CreditCardUpdateService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    /**
     * Updates credit card information based on the provided request.
     * 
     * This method implements the complete business logic from COCRDUPC.CBL MAIN-PARA,
     * including input validation, card retrieval with locking, change detection,
     * and update processing with comprehensive error handling.
     * 
     * Transaction Behavior:
     * - Uses @Transactional to provide ACID properties equivalent to CICS SYNCPOINT
     * - Implements optimistic locking through JPA @Version handling
     * - Automatically rolls back on any exception (equivalent to CICS ROLLBACK)
     * 
     * Business Process Flow:
     * 1. Validate input request data (1000-PROCESS-INPUTS equivalent)
     * 2. Read card with optimistic locking (9000-READ-DATA equivalent)
     * 3. Detect changes between existing and new data (9300-CHECK-CHANGE-IN-REC equivalent)
     * 4. Apply updates and save to database (9200-WRITE-PROCESSING equivalent)
     * 5. Build and return response with status indicators
     * 
     * @param request CreditCardUpdateRequest containing card number and updated field values
     * @return CreditCardUpdateResponse containing update results and status information
     * @throws ResourceNotFoundException if the card is not found (VSAM NOTFND equivalent)
     * @throws ConcurrencyException if optimistic locking conflict occurs (READ UPDATE conflict)
     * @throws IllegalArgumentException if validation fails on input data
     */
    public CreditCardUpdateResponse updateCreditCard(CreditCardUpdateRequest request) {
        logger.info("Starting credit card update for card number: {}", 
                   request != null && request.getCardNumber() != null ? request.getCardNumber().replaceAll("\\d(?=\\d{4})", "*") : "null");

        try {
            // Step 1: Validate input request (1000-PROCESS-INPUTS from COCRDUPC.CBL)
            validateRequest(request);
            logger.debug("Input validation completed successfully");

            // Step 2: Read card with optimistic locking (9000-READ-DATA from COCRDUPC.CBL)
            Card existingCard = readCardWithLocking(request.getCardNumber());
            logger.debug("Successfully retrieved card for update with ID: {}", existingCard.getCardNumber());

            // Step 3: Detect changes between existing and new data (9300-CHECK-CHANGE-IN-REC from COCRDUPC.CBL)
            if (!detectChanges(existingCard, request)) {
                logger.info("No changes detected for card: {}", existingCard.getCardNumber());
                return buildResponse(existingCard, true, "No change detected with respect to values fetched.");
            }

            // Step 4: Apply updates and save to database (9200-WRITE-PROCESSING from COCRDUPC.CBL)
            Card updatedCard = applyUpdates(existingCard, request);
            logger.info("Successfully updated card: {}", updatedCard.getCardNumber());

            // Step 5: Build successful response
            return buildResponse(updatedCard, true, "Changes committed to database");

        } catch (IllegalArgumentException e) {
            // Let validation exceptions bubble up unchanged
            logger.error("Validation error during card update: {}", e.getMessage());
            throw e;
        } catch (ResourceNotFoundException e) {
            logger.error("Card not found during update: {}", e.getMessage());
            throw e;
        } catch (OptimisticLockException e) {
            logger.error("Optimistic locking conflict during card update: {}", e.getMessage());
            throw new ConcurrencyException("Card", request != null ? request.getCardNumber() : "unknown", 
                                         "Record changed by some one else. Please review", e);
        } catch (Exception e) {
            logger.error("Unexpected error during card update: {}", e.getMessage(), e);
            throw new RuntimeException("Changes unsuccessful. Please try again", e);
        }
    }

    /**
     * Validates the credit card update request data according to COBOL business rules.
     * 
     * This method implements the validation logic from COBOL sections:
     * - 1210-EDIT-ACCOUNT (lines 721-760)
     * - 1220-EDIT-CARD (lines 762-804) 
     * - 1230-EDIT-NAME (lines 806-843)
     * - 1240-EDIT-CARDSTATUS (lines 845-876)
     * - 1250-EDIT-EXPIRY-MON (lines 877-912)
     * - 1260-EDIT-EXPIRY-YEAR (lines 913-947)
     * 
     * Validation Rules:
     * - Card number: required, must be exactly 16 digits
     * - Embossed name: required, alphabetic and spaces only, max 50 characters
     * - Active status: required, must be 'Y' or 'N'
     * - Expiration date: required, month 1-12, year 1950-2099
     * 
     * @param request the credit card update request to validate
     * @throws IllegalArgumentException if any validation rule fails
     */
    private void validateRequest(CreditCardUpdateRequest request) {
        logger.debug("Validating credit card update request");

        if (request == null) {
            throw new IllegalArgumentException("No input received");
        }

        // Validate card number (1220-EDIT-CARD from COCRDUPC.CBL lines 762-804)
        if (request.getCardNumber() == null || request.getCardNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Card number not provided");
        }
        
        if (!request.getCardNumber().matches("^[0-9]{16}$")) {
            throw new IllegalArgumentException("Card number if supplied must be a 16 digit number");
        }

        // Validate embossed name (1230-EDIT-NAME from COCRDUPC.CBL lines 806-843)
        if (request.getEmbossedName() == null || request.getEmbossedName().trim().isEmpty()) {
            throw new IllegalArgumentException("Card name not provided");
        }
        
        if (request.getEmbossedName().length() > 50) {
            throw new IllegalArgumentException("Card name cannot exceed 50 characters");
        }
        
        // Check alphabetic only (replicates COBOL INSPECT CONVERTING logic lines 823-837)
        if (!request.getEmbossedName().matches("^[A-Za-z\\s]+$")) {
            throw new IllegalArgumentException("Card name can only contain alphabets and spaces");
        }

        // Validate active status (1240-EDIT-CARDSTATUS from COCRDUPC.CBL lines 845-876)
        if (request.getActiveStatus() == null || request.getActiveStatus().trim().isEmpty()) {
            throw new IllegalArgumentException("Card Active Status must be Y or N");
        }
        
        if (!request.getActiveStatus().matches("^[YN]$")) {
            throw new IllegalArgumentException("Card Active Status must be Y or N");
        }

        // Validate expiration date (1250-EDIT-EXPIRY-MON and 1260-EDIT-EXPIRY-YEAR from COCRDUPC.CBL lines 877-947)
        if (request.getExpirationDate() == null) {
            throw new IllegalArgumentException("Expiration date is required");
        }
        
        int month = request.getExpirationDate().getMonthValue();
        int year = request.getExpirationDate().getYear();
        
        // Month validation (lines 877-912)
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Card expiry month must be between 1 and 12");
        }
        
        // Year validation (lines 913-947)
        if (year < 1950 || year > 2099) {
            throw new IllegalArgumentException("Invalid card expiry year");
        }

        logger.debug("Credit card update request validation completed successfully");
    }

    /**
     * Reads the card from database with optimistic locking for update operations.
     * 
     * This method implements the COBOL logic from 9100-GETCARD-BYACCTCARD (lines 1376-1417)
     * and replicates CICS READ FILE UPDATE behavior through JPA findById operation.
     * The returned Card entity will be managed by JPA and tracked for optimistic locking.
     * 
     * VSAM to JPA Translation:
     * - CICS READ FILE UPDATE → JPA findById() in transaction context
     * - VSAM NOTFND condition → Optional.empty() handling
     * - Record locking → JPA optimistic locking via @Version
     * 
     * @param cardNumber the 16-digit card number to read
     * @return Card entity managed by JPA for update operations
     * @throws ResourceNotFoundException if card is not found (VSAM NOTFND equivalent)
     */
    private Card readCardWithLocking(String cardNumber) {
        logger.debug("Reading card with locking for card number: {}", cardNumber);

        // Execute read operation (equivalent to CICS READ FILE UPDATE)
        Optional<Card> cardOptional = cardRepository.findById(cardNumber);
        
        if (!cardOptional.isPresent()) {
            // VSAM NOTFND condition handling (lines 1395-1401)
            logger.warn("Card not found for update: {}", cardNumber);
            throw new ResourceNotFoundException("Card", cardNumber, "Did not find cards for this search condition");
        }

        Card card = cardOptional.get();
        logger.debug("Successfully read card for update: {}", card.getCardNumber());
        
        return card;
    }

    /**
     * Detects changes between existing card data and update request.
     * 
     * This method implements the COBOL logic from 9300-CHECK-CHANGE-IN-REC (lines 1498-1523)
     * and replicates the change detection logic from lines 680-683 in COCRDUPC.CBL.
     * 
     * Change Detection Rules (from COBOL lines 1503-1509):
     * - Embossed name comparison (case-insensitive, matching COBOL INSPECT CONVERTING)
     * - Expiration date comparison (year and month)
     * - Active status comparison
     * 
     * Note: CVV code changes are not detected as they are not part of the update operation
     * in the original COBOL program.
     * 
     * @param existingCard the current card data from database
     * @param request the update request with new values
     * @return true if changes are detected, false if data is identical
     */
    private boolean detectChanges(Card existingCard, CreditCardUpdateRequest request) {
        logger.debug("Detecting changes between existing card and update request");

        // Compare embossed name (case-insensitive, replicating COBOL INSPECT CONVERTING lines 1499-1501)
        String existingName = existingCard.getEmbossedName() != null ? existingCard.getEmbossedName().toUpperCase() : "";
        String newName = request.getEmbossedName() != null ? request.getEmbossedName().toUpperCase() : "";
        
        boolean nameChanged = !existingName.equals(newName);
        
        // Compare expiration date (replicating COBOL date comparison lines 1505-1507)
        boolean dateChanged = false;
        if (existingCard.getExpirationDate() != null && request.getExpirationDate() != null) {
            dateChanged = !existingCard.getExpirationDate().equals(request.getExpirationDate());
        } else {
            dateChanged = (existingCard.getExpirationDate() != request.getExpirationDate());
        }
        
        // Compare active status (replicating COBOL status comparison line 1508)
        String existingStatus = existingCard.getActiveStatus() != null ? existingCard.getActiveStatus() : "";
        String newStatus = request.getActiveStatus() != null ? request.getActiveStatus() : "";
        
        boolean statusChanged = !existingStatus.equals(newStatus);

        boolean hasChanges = nameChanged || dateChanged || statusChanged;
        
        logger.debug("Change detection results - Name: {}, Date: {}, Status: {}, Overall: {}", 
                    nameChanged, dateChanged, statusChanged, hasChanges);
        
        return hasChanges;
    }

    /**
     * Applies updates to the card entity and saves to database with optimistic locking.
     * 
     * This method implements the COBOL logic from 9200-WRITE-PROCESSING (lines 1420-1496)
     * including update preparation (lines 1459-1476) and CICS REWRITE operation (lines 1477-1492).
     * 
     * Update Process:
     * 1. Apply new values to existing card entity (preserving JPA managed state)
     * 2. Save using JPA repository (equivalent to CICS REWRITE)
     * 3. Handle optimistic locking conflicts
     * 
     * JPA Optimistic Locking:
     * - Version field automatically incremented by JPA
     * - OptimisticLockException thrown if version mismatch detected
     * - Transaction rollback on any exception
     * 
     * @param existingCard the card entity to update (managed by JPA)
     * @param request the update request with new values
     * @return updated Card entity after successful save
     * @throws OptimisticLockException if concurrent modification detected
     */
    private Card applyUpdates(Card existingCard, CreditCardUpdateRequest request) {
        logger.debug("Applying updates to card: {}", existingCard.getCardNumber());

        // Apply updates to managed entity (lines 1461-1475 from COCRDUPC.CBL)
        existingCard.setEmbossedName(request.getEmbossedName());
        existingCard.setExpirationDate(request.getExpirationDate());
        existingCard.setActiveStatus(request.getActiveStatus());
        
        logger.debug("Updated card fields - Name: {}, ExpiryDate: {}, Status: {}", 
                    request.getEmbossedName(), request.getExpirationDate(), request.getActiveStatus());

        try {
            // Save to database (equivalent to CICS REWRITE lines 1477-1483)
            Card savedCard = cardRepository.save(existingCard);
            logger.debug("Successfully saved card updates to database");
            return savedCard;
            
        } catch (OptimisticLockException e) {
            // Handle optimistic locking conflict (equivalent to CICS UPDATE failure)
            logger.error("Optimistic locking conflict detected during card update: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            // Handle other database errors (equivalent to CICS REWRITE failure lines 1490-1492)
            logger.error("Database error during card update: {}", e.getMessage(), e);
            throw new RuntimeException("Update of record failed", e);
        }
    }

    /**
     * Builds the credit card update response with appropriate status and messages.
     * 
     * This method constructs the response DTO matching the COBOL response patterns
     * from WS-INFO-MSG and WS-RETURN-MSG structures (lines 156-214).
     * 
     * Response Messages (from COBOL constants):
     * - Success: "Changes committed to database" (CONFIRM-UPDATE-SUCCESS)
     * - No changes: "No change detected with respect to values fetched" (NO-CHANGES-DETECTED)
     * - Failure: "Changes unsuccessful. Please try again" (INFORM-FAILURE)
     * 
     * @param card the card entity (updated or original)
     * @param success indicator of operation success
     * @param message descriptive message for the operation result
     * @return CreditCardUpdateResponse with complete result information
     */
    private CreditCardUpdateResponse buildResponse(Card card, boolean success, String message) {
        logger.debug("Building response for card update - Success: {}, Message: {}", success, message);

        return CreditCardUpdateResponse.builder()
                .maskedCardNumber(card.getMaskedCardNumber())
                .embossedName(card.getEmbossedName())
                .expirationDate(card.getExpirationDate())
                .activeStatus(card.getActiveStatus())
                .updateTimestamp(LocalDateTime.now())
                .successIndicator(success)
                .responseMessage(message)
                .build();
    }
}
