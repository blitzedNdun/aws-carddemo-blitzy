package com.carddemo.controller;

import com.carddemo.dto.ApiResponse;
import com.carddemo.dto.CardListDto;
import com.carddemo.dto.CardRequest;
import com.carddemo.dto.CardResponse;
import com.carddemo.dto.PageResponse;
import com.carddemo.dto.ResponseStatus;
import com.carddemo.exception.BusinessRuleException;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.service.CreditCardService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;

/**
 * REST controller for credit card operations handling CCLI, CCDL, and CCUP transaction codes.
 * Manages card listing, details, and updates replacing COCRDLIC, COCRDSLC, and COCRDUPC COBOL programs.
 * Provides GET /api/cards for listing, GET /api/cards/{cardNumber} for details, PUT /api/cards/{cardNumber} for updates.
 * Implements card number masking for security.
 * 
 * This controller consolidates functionality from three COBOL programs:
 * - COCRDLIC (CCLI transaction): Card listing with pagination and filtering
 * - COCRDSLC (CCDL transaction): Individual card detail viewing
 * - COCRDUPC (CCUP transaction): Card field updates and validation
 */
@RestController
@RequestMapping("/api/cards")
public class CardController {

    private static final Logger logger = LoggerFactory.getLogger(CardController.class);

    @Autowired
    private CreditCardService creditCardService;

    /**
     * List credit cards with pagination and filtering (replaces COCRDLIC COBOL program).
     * Maps to CCLI transaction code with support for account and card number filtering.
     * Implements pagination matching COBOL screen navigation (F7/F8 keys).
     * 
     * @param accountId Optional account ID filter (equivalent to COMMAREA account filter)
     * @param cardNumber Optional card number filter for exact match searches  
     * @param page Page number (0-based, defaults to 0)
     * @param size Page size (defaults to 7 to match COBOL screen lines)
     * @return PageResponse containing CardListDto items with masked card numbers
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CardListDto>>> listCards(
            @RequestParam(value = "accountId", required = false) String accountId,
            @RequestParam(value = "cardNumber", required = false) String cardNumber,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "7") int size) {
        
        logger.info("Processing card list request - accountId: {}, cardNumber: {}, page: {}, size: {}", 
                   accountId, 
                   cardNumber != null ? maskCardNumber(cardNumber) : null, 
                   page, 
                   size);

        try {
            // Validate input parameters matching COBOL edit routines
            validateListCardParameters(accountId, cardNumber, page, size);

            // Call service layer (replaces COBOL 9000-READ-FORWARD section)
            PageResponse<CardListDto> cardPage = creditCardService.listCards(accountId, cardNumber, page, size);

            // Create API response matching CICS SEND MAP pattern
            ApiResponse<PageResponse<CardListDto>> response = new ApiResponse<>();
            response.setStatus(ResponseStatus.SUCCESS);
            response.setTransactionCode("CCLI");
            response.setResponseData(cardPage);

            logger.info("Successfully retrieved {} cards for page {}", 
                       cardPage.getData().size(), page);

            return ResponseEntity.ok(response);

        } catch (BusinessRuleException e) {
            logger.error("Business rule violation in card listing: {}", e.getMessage());
            
            ApiResponse<PageResponse<CardListDto>> errorResponse = new ApiResponse<>();
            errorResponse.setStatus(ResponseStatus.ERROR);
            errorResponse.setTransactionCode("CCLI");
            
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            logger.error("Unexpected error in card listing", e);
            
            ApiResponse<PageResponse<CardListDto>> errorResponse = new ApiResponse<>();
            errorResponse.setStatus(ResponseStatus.ERROR);
            errorResponse.setTransactionCode("CCLI");
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get detailed information for a specific card (replaces COCRDSLC COBOL program).
     * Maps to CCDL transaction code with card number lookup and account cross-reference validation.
     * Returns complete card details with security-compliant field masking.
     * 
     * @param cardNumber The card number to retrieve (16 digits)
     * @param accountId Optional account ID for cross-reference validation
     * @return CardResponse with complete card details and masked sensitive data
     */
    @GetMapping("/{cardNumber}")
    public ResponseEntity<ApiResponse<CardResponse>> getCardDetails(
            @PathVariable("cardNumber") String cardNumber,
            @RequestParam(value = "accountId", required = false) String accountId) {
        
        logger.info("Processing card detail request - cardNumber: {}, accountId: {}", 
                   maskCardNumber(cardNumber), accountId);

        try {
            // Validate card number format (replaces COBOL edit routines)
            validateCardNumber(cardNumber);
            
            // Validate account cross-reference if provided
            if (accountId != null) {
                creditCardService.validateCardAccountXref(cardNumber, accountId);
            }

            // Retrieve card details (replaces COBOL READ operations)
            CardResponse cardDetails = creditCardService.getCardDetails(cardNumber);

            // Create API response
            ApiResponse<CardResponse> response = new ApiResponse<>();
            response.setStatus(ResponseStatus.SUCCESS);
            response.setTransactionCode("CCDL");
            response.setResponseData(cardDetails);

            logger.info("Successfully retrieved card details for cardNumber: {}", 
                       maskCardNumber(cardNumber));

            return ResponseEntity.ok(response);

        } catch (BusinessRuleException e) {
            logger.error("Business rule violation in card detail retrieval: {}", e.getMessage());
            
            ApiResponse<CardResponse> errorResponse = new ApiResponse<>();
            errorResponse.setStatus(ResponseStatus.ERROR);
            errorResponse.setTransactionCode("CCDL");
            
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (ResourceNotFoundException e) {
            logger.error("Card not found: {}", e.getMessage());
            
            ApiResponse<CardResponse> errorResponse = new ApiResponse<>();
            errorResponse.setStatus(ResponseStatus.ERROR);
            errorResponse.setTransactionCode("CCDL");
            
            return ResponseEntity.status(404).body(errorResponse);

        } catch (Exception e) {
            logger.error("Unexpected error retrieving card details for cardNumber: {}", 
                        maskCardNumber(cardNumber), e);
            
            ApiResponse<CardResponse> errorResponse = new ApiResponse<>();
            errorResponse.setStatus(ResponseStatus.ERROR);
            errorResponse.setTransactionCode("CCDL");
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Update card information (replaces COCRDUPC COBOL program).
     * Maps to CCUP transaction code with comprehensive field validation and business rule enforcement.
     * Supports updates to embossed name, expiration date, and active status.
     * 
     * @param cardNumber The card number to update (16 digits)
     * @param cardRequest Request object containing fields to update
     * @return Updated CardResponse with new field values
     */
    @PutMapping("/{cardNumber}")
    public ResponseEntity<ApiResponse<CardResponse>> updateCard(
            @PathVariable("cardNumber") String cardNumber,
            @Valid @RequestBody CardRequest cardRequest) {
        
        logger.info("Processing card update request - cardNumber: {}", maskCardNumber(cardNumber));

        try {
            // Validate card number format
            validateCardNumber(cardNumber);
            
            // Validate request data consistency (replaces COBOL edit routines)
            validateCardUpdateRequest(cardNumber, cardRequest);

            // Perform card update with business rule validation
            CardResponse updatedCard = creditCardService.updateCard(cardNumber, cardRequest);

            // Create success response
            ApiResponse<CardResponse> response = new ApiResponse<>();
            response.setStatus(ResponseStatus.SUCCESS);
            response.setTransactionCode("CCUP");
            response.setResponseData(updatedCard);

            logger.info("Successfully updated card: {}", maskCardNumber(cardNumber));

            return ResponseEntity.ok(response);

        } catch (BusinessRuleException e) {
            logger.error("Business rule violation in card update: {}", e.getMessage());
            
            ApiResponse<CardResponse> errorResponse = new ApiResponse<>();
            errorResponse.setStatus(ResponseStatus.ERROR);
            errorResponse.setTransactionCode("CCUP");
            
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            logger.error("Unexpected error updating card: {}", maskCardNumber(cardNumber), e);
            
            ApiResponse<CardResponse> errorResponse = new ApiResponse<>();
            errorResponse.setStatus(ResponseStatus.ERROR);
            errorResponse.setTransactionCode("CCUP");
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get cards by account ID with pagination support.
     * Alternative endpoint for account-specific card listing matching COBOL account filtering logic.
     * 
     * @param accountId The account ID to filter by (11 digits)
     * @param page Page number (0-based, defaults to 0)
     * @param size Page size (defaults to 7)
     * @return PageResponse containing CardListDto items for the specified account
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse<PageResponse<CardListDto>>> getCardsByAccount(
            @PathVariable String accountId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "7") int size) {
        
        logger.info("Processing cards by account request - accountId: {}, page: {}, size: {}", 
                   accountId, page, size);

        try {
            // Validate account ID format (11 digits numeric)
            validateAccountId(accountId);

            // Delegate to main listing method with account filter
            return listCards(accountId, null, page, size);

        } catch (BusinessRuleException e) {
            logger.error("Business rule violation in cards by account: {}", e.getMessage());
            
            ApiResponse<PageResponse<CardListDto>> errorResponse = new ApiResponse<>();
            errorResponse.setStatus(ResponseStatus.ERROR);
            errorResponse.setTransactionCode("CCLI");
            
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            logger.error("Unexpected error retrieving cards for account: {}", accountId, e);
            
            ApiResponse<PageResponse<CardListDto>> errorResponse = new ApiResponse<>();
            errorResponse.setStatus(ResponseStatus.ERROR);
            errorResponse.setTransactionCode("CCLI");
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Validates parameters for card listing operations.
     * Replicates COBOL input validation logic from COCRDLIC program.
     * 
     * @param accountId Account ID filter (optional)
     * @param cardNumber Card number filter (optional)
     * @param page Page number
     * @param size Page size
     * @throws BusinessRuleException if validation fails
     */
    private void validateListCardParameters(String accountId, String cardNumber, int page, int size) {
        // Validate account ID if provided (matches COBOL account edit routine)
        if (accountId != null && !accountId.trim().isEmpty()) {
            validateAccountId(accountId);
        }

        // Validate card number if provided (matches COBOL card edit routine)
        if (cardNumber != null && !cardNumber.trim().isEmpty()) {
            validateCardNumber(cardNumber);
        }

        // Validate pagination parameters
        if (page < 0) {
            throw new BusinessRuleException("INVALID_PAGE", "Page number must be non-negative");
        }

        if (size <= 0 || size > 50) {
            throw new BusinessRuleException("INVALID_PAGE_SIZE", "Page size must be between 1 and 50");
        }
    }

    /**
     * Validates account ID format (11 digit numeric).
     * Replicates COBOL account validation from 2210-EDIT-ACCOUNT section.
     * 
     * @param accountId The account ID to validate
     * @throws BusinessRuleException if validation fails
     */
    private void validateAccountId(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new BusinessRuleException("ACCOUNT_REQUIRED", "Account ID is required");
        }

        // Remove any leading/trailing whitespace
        String trimmedAccountId = accountId.trim();

        // Must be exactly 11 digits (matches COBOL PIC 9(11))
        if (!trimmedAccountId.matches("\\d{11}")) {
            throw new BusinessRuleException("INVALID_ACCOUNT_FORMAT", 
                "Account ID must be exactly 11 digits");
        }

        // Must not be all zeros
        if ("00000000000".equals(trimmedAccountId)) {
            throw new BusinessRuleException("INVALID_ACCOUNT_VALUE", 
                "Account ID must be a non-zero 11 digit number");
        }
    }

    /**
     * Validates card number format (16 digit numeric).
     * Replicates COBOL card validation from 2220-EDIT-CARD section.
     * 
     * @param cardNumber The card number to validate
     * @throws BusinessRuleException if validation fails
     */
    private void validateCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            throw new BusinessRuleException("CARD_NUMBER_REQUIRED", "Card number is required");
        }

        // Remove any leading/trailing whitespace and formatting characters
        String cleanCardNumber = cardNumber.trim().replaceAll("[\\s\\-]", "");

        // Must be 15-16 digits (15 for Amex, 16 for Visa/MC/Discover)
        if (!cleanCardNumber.matches("\\d{15,16}")) {
            throw new BusinessRuleException("INVALID_CARD_FORMAT", 
                "Card number must be exactly 15-16 digits");
        }

        // Must not be all zeros
        if ("000000000000000".equals(cleanCardNumber) || "0000000000000000".equals(cleanCardNumber)) {
            throw new BusinessRuleException("INVALID_CARD_VALUE", 
                "Card number must be a non-zero number");
        }
    }

    /**
     * Validates card update request data consistency.
     * Replicates COBOL field validation logic from COCRDUPC program.
     * 
     * @param cardNumber The card number being updated
     * @param cardRequest The update request data
     * @throws BusinessRuleException if validation fails
     */
    private void validateCardUpdateRequest(String cardNumber, CardRequest cardRequest) {
        // Validate card number consistency
        if (cardRequest.getCardNumber() != null && 
            !cardNumber.equals(cardRequest.getCardNumber())) {
            throw new BusinessRuleException("CARD_NUMBER_MISMATCH", 
                "Card number in URL must match card number in request body");
        }

        // Validate account ID if provided
        if (cardRequest.getAccountId() != null) {
            validateAccountId(cardRequest.getAccountId());
        }

        // Validate active status if provided (must be 'Y' or 'N')
        if (cardRequest.getActiveStatus() != null) {
            String status = cardRequest.getActiveStatus().trim().toUpperCase();
            if (!"Y".equals(status) && !"N".equals(status)) {
                throw new BusinessRuleException("INVALID_STATUS", 
                    "Active status must be 'Y' or 'N'");
            }
        }

        // Validate embossed name if provided (max 50 characters)
        if (cardRequest.getEmbossedName() != null) {
            String name = cardRequest.getEmbossedName().trim();
            if (name.length() > 50) {
                throw new BusinessRuleException("NAME_TOO_LONG", 
                    "Embossed name cannot exceed 50 characters");
            }
            if (name.isEmpty()) {
                throw new BusinessRuleException("NAME_REQUIRED", 
                    "Embossed name cannot be empty");
            }
        }

        // Validate expiration date if provided (YYYY-MM-DD format)
        if (cardRequest.getExpirationDate() != null) {
            validateExpirationDate(cardRequest.getExpirationDate());
        }
    }

    /**
     * Validates expiration date format and business rules.
     * Replicates COBOL date validation logic.
     * 
     * @param expirationDate The expiration date to validate
     * @throws BusinessRuleException if validation fails
     */
    private void validateExpirationDate(LocalDate expirationDate) {
        if (expirationDate == null) {
            throw new BusinessRuleException("EXPIRATION_DATE_REQUIRED", 
                "Expiration date is required");
        }

        // Get date components
        int year = expirationDate.getYear();
        int month = expirationDate.getMonthValue();
        int day = expirationDate.getDayOfMonth();

        // Validate year range (1950-2099, matching COBOL VALID-YEAR)
        if (year < 1950 || year > 2099) {
            throw new BusinessRuleException("INVALID_YEAR", 
                "Year must be between 1950 and 2099");
        }

        // Validate month range (1-12, matching COBOL VALID-MONTH)
        if (month < 1 || month > 12) {
            throw new BusinessRuleException("INVALID_MONTH", 
                "Month must be between 1 and 12");
        }

        // Validate day range (basic validation)
        if (day < 1 || day > 31) {
            throw new BusinessRuleException("INVALID_DAY", 
                "Day must be between 1 and 31");
        }

        // Validate that the expiration date is in the future
        LocalDate today = LocalDate.now();
        if (expirationDate.isBefore(today)) {
            throw new BusinessRuleException("EXPIRED_CARD", 
                "Card expiration date cannot be in the past");
        }
    }

    /**
     * Masks card number for security logging (shows only last 4 digits).
     * Implements card number masking as specified in requirements.
     * 
     * @param cardNumber The card number to mask
     * @return Masked card number (****-****-****-1234 format)
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****-****-****-****";
        }
        
        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "****-****-****-" + lastFour;
    }
}