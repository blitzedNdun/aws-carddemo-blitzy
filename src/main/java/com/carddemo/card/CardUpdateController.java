package com.carddemo.card;

import com.carddemo.common.dto.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

/**
 * REST controller for credit card lifecycle management implementing COCRDUPC.cbl functionality
 * with optimistic locking, transaction validation, and comprehensive security controls for 
 * card data modifications.
 * 
 * This controller serves as the REST API gateway for card update operations, replacing the 
 * legacy CICS transaction CCUP with modern Spring Boot REST endpoints. It maintains full
 * compatibility with the original COBOL business logic while providing JSON-based API access.
 * 
 * Key Features:
 * - RESTful card update operations with PUT mapping
 * - Optimistic locking support for concurrent update protection
 * - Comprehensive input validation using Jakarta Bean Validation
 * - Role-based security authorization with Spring Security
 * - Detailed error handling for validation and business rule violations
 * - Transaction management coordinating with account balance updates
 * - Audit logging for all card modification operations
 * - Exception handling for optimistic locking conflicts and constraint violations
 * 
 * Security Model:
 * - Role-based access control ensuring only authorized users can modify card data
 * - Method-level authorization with @PreAuthorize annotations
 * - Comprehensive audit trail for all update operations
 * 
 * Error Handling:
 * - ValidationException for input validation failures
 * - OptimisticLockingException for concurrent modification conflicts  
 * - CardNotFoundException for invalid card lookup attempts
 * - Standardized error response format with ValidationResult integration
 * 
 * COBOL Program Mapping:
 * - Replaces COCRDUPC.cbl CICS transaction processing
 * - Maintains equivalent validation logic from COBOL paragraphs 1200-1260
 * - Preserves business rule enforcement from sections 9200-9300
 * 
 * @author Blitzy Platform - Card Management Team
 * @version 1.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/cards")
@CrossOrigin(origins = {"http://localhost:3000", "https://carddemo.blitzy.com"})
public class CardUpdateController {

    private static final Logger logger = LoggerFactory.getLogger(CardUpdateController.class);

    private final CardUpdateService cardUpdateService;

    /**
     * Constructs CardUpdateController with required dependencies.
     * 
     * @param cardUpdateService Business service for card update operations
     */
    @Autowired
    public CardUpdateController(CardUpdateService cardUpdateService) {
        this.cardUpdateService = cardUpdateService;
        logger.info("CardUpdateController initialized with CardUpdateService dependency");
    }

    /**
     * Updates credit card information with optimistic locking and comprehensive validation.
     * 
     * This endpoint implements the complete card update workflow from the original COCRDUPC.cbl
     * COBOL program, including all validation steps, business rule checks, and error handling.
     * The operation is atomic and includes coordination with account balance updates.
     * 
     * Request Processing Flow:
     * 1. Input validation using Jakarta Bean Validation annotations
     * 2. Authorization check ensuring user has card modification privileges
     * 3. Business validation through CardUpdateService.validateUpdateRequest()
     * 4. Optimistic locking verification using version numbers
     * 5. Card data update with transaction boundary management
     * 6. Account association validation and balance coordination
     * 7. Audit logging and response generation
     * 
     * Security Requirements:
     * - User must have 'CARD_UPDATE' authority or be an administrator
     * - All operations are logged for audit compliance
     * - Sensitive card data is protected during transmission
     * 
     * Validation Rules (mapped from COBOL paragraphs 1200-1260):
     * - Card number must be 16 digits and exist in system
     * - Account ID must be 11 digits and associated with card
     * - Card name must contain only alphabetic characters and spaces
     * - Card status must be 'Y' (active) or 'N' (inactive)
     * - Expiration month must be 1-12
     * - Expiration year must be valid future year (1950-2099 range from COBOL)
     * - Version number required for optimistic locking
     * 
     * Error Scenarios:
     * - 400 Bad Request: Input validation failures or business rule violations
     * - 404 Not Found: Card or account not found in system
     * - 409 Conflict: Optimistic locking failure due to concurrent modifications
     * - 500 Internal Server Error: System errors or database constraints
     * 
     * @param cardNumber The 16-digit card number to update (path parameter)
     * @param request CardUpdateRequestDto containing update data and version for optimistic locking
     * @return ResponseEntity containing CardUpdateResponseDto with update results and validation status
     */
    @PutMapping("/{cardNumber}")
    @PreAuthorize("hasAuthority('CARD_UPDATE') or hasRole('ADMIN')")
    public ResponseEntity<CardUpdateResponseDto> updateCard(
            @PathVariable String cardNumber,
            @Valid @RequestBody CardUpdateRequestDto request) {
        
        logger.info("Processing card update request for card number: {} by user with authorities", 
                   cardNumber != null ? cardNumber.substring(0, 4) + "****" : "null");
        
        try {
            // Ensure path parameter matches request body card number for consistency
            if (!cardNumber.equals(request.getCardNumber())) {
                logger.warn("Card number mismatch: path parameter {} does not match request body {}", 
                           cardNumber, request.getCardNumber());
                
                CardUpdateResponseDto errorResponse = new CardUpdateResponseDto();
                ValidationResult validationResult = new ValidationResult(false);
                validationResult.addErrorMessage("cardNumber", "CARD_NUMBER_MISMATCH", 
                    "Card number in URL path must match card number in request body", 
                    ValidationResult.Severity.ERROR);
                errorResponse.setValidationResult(validationResult);
                
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Perform comprehensive business validation
            ValidationResult validationResult = cardUpdateService.validateUpdateRequest(request);
            if (!validationResult.isValid()) {
                logger.warn("Card update validation failed for card {}: {}", 
                          cardNumber.substring(0, 4) + "****", validationResult.getErrorSummary());
                
                CardUpdateResponseDto errorResponse = new CardUpdateResponseDto();
                errorResponse.setValidationResult(validationResult);
                
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Execute card update with optimistic locking
            CardUpdateResponseDto response = cardUpdateService.updateCard(request);
            
            if (response.isOptimisticLockSuccess() && response.getValidationResult().isValid()) {
                logger.info("Card update completed successfully for card number: {}", 
                          cardNumber.substring(0, 4) + "****");
                return ResponseEntity.ok(response);
            } else if (!response.isOptimisticLockSuccess()) {
                logger.warn("Optimistic locking conflict detected for card {}", 
                          cardNumber.substring(0, 4) + "****");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            } else {
                logger.warn("Card update failed validation for card {}: {}", 
                          cardNumber.substring(0, 4) + "****", 
                          response.getValidationResult().getErrorSummary());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (CardNotFoundException e) {
            logger.error("Card not found during update operation: {}", e.getMessage());
            return handleCardNotFoundException(e);
        } catch (OptimisticLockingException e) {
            logger.error("Optimistic locking exception during card update: {}", e.getMessage());
            return handleOptimisticLockingException(e);
        } catch (ValidationException e) {
            logger.error("Validation exception during card update: {}", e.getMessage());
            return handleValidationException(e);
        } catch (Exception e) {
            logger.error("Unexpected error during card update for card {}: {}", 
                        cardNumber != null ? cardNumber.substring(0, 4) + "****" : "null", 
                        e.getMessage(), e);
            
            CardUpdateResponseDto errorResponse = new CardUpdateResponseDto();
            ValidationResult validationResult = new ValidationResult(false);
            validationResult.addErrorMessage("system", "INTERNAL_ERROR", 
                "An unexpected error occurred during card update processing", 
                ValidationResult.Severity.ERROR);
            errorResponse.setValidationResult(validationResult);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Handles ValidationException by converting to appropriate HTTP response.
     * 
     * This method processes validation failures that occur during card update operations,
     * converting Java exceptions into structured JSON error responses that maintain
     * compatibility with the original COBOL error message patterns.
     * 
     * @param e ValidationException containing field-level validation errors
     * @return ResponseEntity with 400 Bad Request status and detailed validation errors
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<CardUpdateResponseDto> handleValidationException(ValidationException e) {
        logger.debug("Handling validation exception: {}", e.getMessage());
        
        CardUpdateResponseDto errorResponse = new CardUpdateResponseDto();
        ValidationResult validationResult = new ValidationResult(false);
        
        // Convert exception details to ValidationResult format
        if (e.getFieldName() != null) {
            validationResult.addErrorMessage(e.getFieldName(), "VALIDATION_ERROR", 
                e.getMessage(), ValidationResult.Severity.ERROR);
        } else {
            validationResult.addErrorMessage("request", "VALIDATION_ERROR", 
                e.getMessage(), ValidationResult.Severity.ERROR);
        }
        
        errorResponse.setValidationResult(validationResult);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles OptimisticLockingException for concurrent modification conflicts.
     * 
     * This method processes optimistic locking failures that occur when multiple users
     * attempt to modify the same card record simultaneously. It returns a 409 Conflict
     * status with detailed information about the conflict and current record state.
     * 
     * The response includes conflict resolution metadata to help clients handle the
     * concurrent modification scenario, similar to the COBOL logic in paragraph 9300.
     * 
     * @param e OptimisticLockingException containing conflict details
     * @return ResponseEntity with 409 Conflict status and conflict resolution information
     */
    @ExceptionHandler(OptimisticLockingException.class)
    public ResponseEntity<CardUpdateResponseDto> handleOptimisticLockingException(OptimisticLockingException e) {
        logger.debug("Handling optimistic locking exception: {}", e.getMessage());
        
        CardUpdateResponseDto errorResponse = new CardUpdateResponseDto();
        ValidationResult validationResult = new ValidationResult(false);
        
        validationResult.addErrorMessage("version", "OPTIMISTIC_LOCK_CONFLICT", 
            "Record was modified by another user. Please refresh and try again.", 
            ValidationResult.Severity.ERROR);
        
        errorResponse.setValidationResult(validationResult);
        errorResponse.setOptimisticLockSuccess(false);
        
        // Include conflict resolution information if available
        if (e.getCurrentVersion() != null) {
            errorResponse.setVersionNumber(e.getCurrentVersion());
            errorResponse.setConflictResolutionInfo(
                String.format("Current record version is %d. Please refresh your data and retry the update.", 
                             e.getCurrentVersion()));
        }
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles CardNotFoundException when requested card does not exist.
     * 
     * This method processes card lookup failures, returning a 404 Not Found status
     * with clear error messaging. It corresponds to the COBOL logic that handles
     * the DFHRESP(NOTFND) condition in paragraph 9100.
     * 
     * @param e CardNotFoundException containing card lookup failure details
     * @return ResponseEntity with 404 Not Found status and error details
     */
    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<CardUpdateResponseDto> handleCardNotFoundException(CardNotFoundException e) {
        logger.debug("Handling card not found exception: {}", e.getMessage());
        
        CardUpdateResponseDto errorResponse = new CardUpdateResponseDto();
        ValidationResult validationResult = new ValidationResult(false);
        
        validationResult.addErrorMessage("cardNumber", "CARD_NOT_FOUND", 
            "The specified card was not found in the system", 
            ValidationResult.Severity.ERROR);
        
        errorResponse.setValidationResult(validationResult);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // Custom exception classes for specific error scenarios
    
    /**
     * Exception thrown when input validation fails during card update operations.
     * Maps to the COBOL input validation logic in paragraphs 1200-1260.
     */
    public static class ValidationException extends RuntimeException {
        private final String fieldName;
        
        public ValidationException(String message) {
            super(message);
            this.fieldName = null;
        }
        
        public ValidationException(String fieldName, String message) {
            super(message);
            this.fieldName = fieldName;
        }
        
        public String getFieldName() {
            return fieldName;
        }
    }
    
    /**
     * Exception thrown when optimistic locking conflicts occur during card updates.
     * Maps to the COBOL optimistic locking logic in paragraph 9300.
     */
    public static class OptimisticLockingException extends RuntimeException {
        private final Long currentVersion;
        
        public OptimisticLockingException(String message) {
            super(message);
            this.currentVersion = null;
        }
        
        public OptimisticLockingException(String message, Long currentVersion) {
            super(message);
            this.currentVersion = currentVersion;
        }
        
        public Long getCurrentVersion() {
            return currentVersion;
        }
    }
    
    /**
     * Exception thrown when requested card is not found in the system.
     * Maps to the COBOL NOTFND response handling in paragraph 9100.
     */
    public static class CardNotFoundException extends RuntimeException {
        public CardNotFoundException(String message) {
            super(message);
        }
    }
}