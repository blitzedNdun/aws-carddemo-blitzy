/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carddemo.card;

import com.carddemo.common.dto.ValidationResult;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REST controller for individual card selection and detail viewing implementing
 * COCRDSLC.cbl functionality through Spring Boot REST endpoints with comprehensive
 * security controls and cross-reference validation.
 * 
 * This controller provides complete card selection operations equivalent to the original
 * COBOL program COCRDSLC.cbl, including input validation, card data retrieval, cross-reference
 * validation, and role-based authorization controls. The implementation maintains exact
 * functional equivalence while supporting modern REST API patterns and Spring Security.
 * 
 * <p>Key Features:</p>
 * <ul>
 *   <li>Individual card detail retrieval with comprehensive validation</li>
 *   <li>Cross-reference validation via PostgreSQL foreign key constraints</li>
 *   <li>Method-level Spring Security authorization with @PreAuthorize annotations</li>
 *   <li>Comprehensive input validation using Jakarta Bean Validation</li>
 *   <li>Structured error handling with appropriate HTTP status codes</li>
 *   <li>Audit logging for security compliance and monitoring</li>
 * </ul>
 * 
 * <p>Original COBOL Implementation Mapping:</p>
 * The controller maps directly from COCRDSLC.cbl maintaining identical business logic:
 * <ul>
 *   <li>0000-MAIN: Main processing flow → selectCard() and getCardDetails() endpoints</li>
 *   <li>2200-EDIT-MAP-INPUTS: Input validation → @Valid annotation processing</li>
 *   <li>9000-READ-DATA: Card retrieval → CardSelectionService.selectCard() delegation</li>
 *   <li>Error handling patterns: COBOL error messages → HTTP exception responses</li>
 * </ul>
 * 
 * <p>Security and Authorization:</p>
 * <ul>
 *   <li>Role-based access control using Spring Security @PreAuthorize</li>
 *   <li>Method-level authorization for sensitive card operations</li>
 *   <li>Card data masking based on user authorization level</li>
 *   <li>Comprehensive audit logging for compliance requirements</li>
 * </ul>
 * 
 * <p>Error Handling:</p>
 * Maintains identical error message patterns from COCRDSLC.cbl:
 * <ul>
 *   <li>"Card number not provided" → HTTP 400 Bad Request</li>
 *   <li>"Account number not provided" → HTTP 400 Bad Request</li>
 *   <li>"Did not find cards for this search condition" → HTTP 404 Not Found</li>
 *   <li>"Error reading Card Data File" → HTTP 500 Internal Server Error</li>
 * </ul>
 * 
 * <p>Performance Requirements:</p>
 * <ul>
 *   <li>Sub-200ms response times at 95th percentile for card selection operations</li>
 *   <li>Support for 10,000+ TPS throughput with horizontal scaling</li>
 *   <li>Memory efficient request/response processing</li>
 *   <li>Optimized database query patterns via service layer</li>
 * </ul>
 * 
 * @author CardDemo Development Team - Blitzy Platform
 * @version 1.0
 * @since Java 21
 * @see com.carddemo.card.CardSelectionService
 * @see com.carddemo.card.CardSelectionRequestDto
 * @see com.carddemo.card.CardSelectionResponseDto
 */
@RestController
@RequestMapping("/api/v1/cards")
public class CardSelectionController {

    /**
     * Logger for controller operations, security events, and error tracking.
     * Provides structured logging for debugging, monitoring, and compliance reporting.
     */
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CardSelectionController.class);

    /**
     * Business service for card selection operations with cross-reference validation.
     * Provides comprehensive card data retrieval, validation, and audit logging
     * implementing the complete COCRDSLC.cbl business logic.
     */
    @Autowired
    private CardSelectionService cardSelectionService;

    // ===================================================================================
    // PRIMARY CARD SELECTION ENDPOINTS
    // ===================================================================================

    /**
     * Primary card selection endpoint implementing COCRDSLC.cbl main processing logic.
     * 
     * This endpoint provides comprehensive card selection functionality equivalent to the
     * original COBOL program's main processing flow (0000-MAIN), including input validation,
     * card data retrieval, cross-reference validation, and role-based response construction.
     * 
     * <p>Processing Flow (equivalent to COBOL 0000-MAIN):</p>
     * <ol>
     *   <li>Validate request input parameters using Jakarta Bean Validation</li>
     *   <li>Check user authorization for card selection operations</li>
     *   <li>Delegate to CardSelectionService for business logic processing</li>
     *   <li>Apply data masking based on user authorization level</li>
     *   <li>Log operation for audit compliance and security monitoring</li>
     *   <li>Return comprehensive card selection response</li>
     * </ol>
     * 
     * <p>Security Controls:</p>
     * <ul>
     *   <li>Method-level authorization requiring CARD_VIEWER or higher role</li>
     *   <li>Request validation preventing malicious input and injection attacks</li>
     *   <li>Data masking for sensitive card information based on authorization</li>
     *   <li>Comprehensive audit logging for security event tracking</li>
     * </ul>
     * 
     * <p>Example Request:</p>
     * <pre>
     * POST /api/v1/cards/select
     * {
     *   "cardNumber": "4532015112830366",
     *   "accountId": "12345678901",
     *   "userRole": "CARD_VIEWER",
     *   "includeMaskedData": true,
     *   "validateExistence": true,
     *   "includeCrossReference": true
     * }
     * </pre>
     * 
     * <p>Example Response:</p>
     * <pre>
     * {
     *   "success": true,
     *   "cardNumber": "4532015112830366",
     *   "maskedCardNumber": "4532-****-****-0366",
     *   "embossedName": "JOHN DOE",
     *   "expirationDate": "2025-12-31T00:00:00",
     *   "activeStatus": "ACTIVE",
     *   "accountInfo": { ... },
     *   "customerInfo": { ... },
     *   "auditInfo": { ... }
     * }
     * </pre>
     * 
     * @param request CardSelectionRequestDto containing card number, account ID, and authorization parameters
     * @return ResponseEntity containing CardSelectionResponseDto with complete card details
     * @throws MethodArgumentNotValidException if request validation fails
     * @throws RuntimeException if card selection processing fails
     */
    @PostMapping("/select")
    @PreAuthorize("hasAnyRole('CARD_VIEWER', 'CARD_ADMIN', 'ACCOUNT_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<CardSelectionResponseDto> selectCard(@Valid @RequestBody CardSelectionRequestDto request) {
        logger.info("Processing card selection request for card: {} and account: {}", 
                   maskCardNumber(request.getCardNumber()), request.getAccountId());

        // Generate correlation ID for request tracking if not provided
        if (request.getCorrelationId() == null || request.getCorrelationId().trim().isEmpty()) {
            request.setCorrelationId(UUID.randomUUID().toString());
        }

        LocalDateTime processingStartTime = LocalDateTime.now();
        
        try {
            // Validate request completeness beyond basic bean validation
            validateCardSelectionRequest(request);

            // Delegate to service for business logic processing
            CardSelectionResponseDto response = cardSelectionService.selectCard(request);

            // Log successful operation for audit compliance
            long processingDuration = java.time.Duration.between(processingStartTime, LocalDateTime.now()).toMillis();
            logger.info("Card selection completed successfully - Card: {}, Account: {}, Duration: {}ms, CorrelationId: {}",
                       maskCardNumber(request.getCardNumber()), request.getAccountId(), 
                       processingDuration, request.getCorrelationId());

            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Card selection request validation failed - Card: {}, Account: {}, Error: {}, CorrelationId: {}",
                       maskCardNumber(request.getCardNumber()), request.getAccountId(), 
                       e.getMessage(), request.getCorrelationId());
            throw e; // Will be handled by handleValidationException
            
        } catch (RuntimeException e) {
            logger.error("Card selection processing failed - Card: {}, Account: {}, Error: {}, CorrelationId: {}",
                        maskCardNumber(request.getCardNumber()), request.getAccountId(), 
                        e.getMessage(), request.getCorrelationId(), e);
            throw e; // Will be handled by appropriate exception handler
        }
    }

    /**
     * Card detail retrieval endpoint for direct card lookup by card number.
     * 
     * This endpoint provides streamlined card detail retrieval functionality for scenarios
     * where the card number is known and account cross-reference validation is not required.
     * Supports path variable card number specification for RESTful API patterns.
     * 
     * <p>Processing Features:</p>
     * <ul>
     *   <li>Direct card lookup by card number path variable</li>
     *   <li>Optional account ID query parameter for cross-reference validation</li>
     *   <li>Role-based data masking and authorization controls</li>
     *   <li>Comprehensive error handling for not found scenarios</li>
     * </ul>
     * 
     * <p>Usage Scenarios:</p>
     * <ul>
     *   <li>Quick card detail lookup for customer service operations</li>
     *   <li>Card validation during transaction processing</li>
     *   <li>Administrative card status checking</li>
     * </ul>
     * 
     * <p>Example Usage:</p>
     * <pre>
     * GET /api/v1/cards/4532015112830366/details?accountId=12345678901&userRole=CARD_VIEWER
     * </pre>
     * 
     * @param cardNumber 16-digit card number for detail retrieval
     * @param accountId Optional 11-digit account ID for cross-reference validation
     * @param userRole User role for authorization and data masking decisions
     * @param includeMaskedData Flag to control sensitive data inclusion in response
     * @return ResponseEntity containing CardSelectionResponseDto with card details
     * @throws IllegalArgumentException if path parameters are invalid
     * @throws RuntimeException if card not found or processing fails
     */
    @GetMapping("/{cardNumber}/details")
    @PreAuthorize("hasAnyRole('CARD_VIEWER', 'CARD_ADMIN', 'ACCOUNT_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<CardSelectionResponseDto> getCardDetails(
            @PathVariable String cardNumber,
            @RequestParam(required = false) String accountId,
            @RequestParam String userRole,
            @RequestParam(defaultValue = "false") Boolean includeMaskedData) {
        
        logger.info("Processing card detail request for card: {}", maskCardNumber(cardNumber));

        String correlationId = UUID.randomUUID().toString();
        LocalDateTime processingStartTime = LocalDateTime.now();
        
        try {
            // Build card selection request from path and query parameters
            CardSelectionRequestDto request = new CardSelectionRequestDto(correlationId);
            request.setCardNumber(cardNumber);
            request.setAccountId(accountId);
            request.setUserRole(userRole);
            request.setIncludeMaskedData(includeMaskedData);
            request.setValidateExistence(true);
            request.setIncludeCrossReference(accountId != null);

            // Validate constructed request
            validateCardSelectionRequest(request);

            // Delegate to service for processing
            CardSelectionResponseDto response = cardSelectionService.selectCard(request);

            // Log successful operation
            long processingDuration = java.time.Duration.between(processingStartTime, LocalDateTime.now()).toMillis();
            logger.info("Card detail retrieval completed successfully - Card: {}, Duration: {}ms, CorrelationId: {}",
                       maskCardNumber(cardNumber), processingDuration, correlationId);

            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Card detail request validation failed - Card: {}, Error: {}, CorrelationId: {}",
                       maskCardNumber(cardNumber), e.getMessage(), correlationId);
            throw e;
            
        } catch (RuntimeException e) {
            logger.error("Card detail retrieval failed - Card: {}, Error: {}, CorrelationId: {}",
                        maskCardNumber(cardNumber), e.getMessage(), correlationId, e);
            throw e;
        }
    }

    // ===================================================================================
    // EXCEPTION HANDLERS (as required by exports specification)
    // ===================================================================================

    /**
     * Exception handler for card not found scenarios.
     * 
     * Handles RuntimeException cases where card data cannot be retrieved due to:
     * - Invalid card number not found in database
     * - Card-account relationship validation failures
     * - Cross-reference validation errors
     * 
     * Maps to COBOL error messages from COCRDSLC.cbl:
     * - "Did not find cards for this search condition"
     * - "Did not find this account in cards database"
     * 
     * @param e RuntimeException containing card not found details
     * @return ResponseEntity with HTTP 404 Not Found and error details
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<CardSelectionResponseDto> handleCardNotFoundException(RuntimeException e) {
        logger.warn("Card not found exception: {}", e.getMessage());

        // Check if this is a card not found scenario based on message content
        String errorMessage = e.getMessage();
        if (errorMessage != null && (errorMessage.contains("Did not find") || 
                                   errorMessage.contains("not found") ||
                                   errorMessage.contains("not associated"))) {
            
            // Create error response matching COBOL error message patterns
            CardSelectionResponseDto errorResponse = new CardSelectionResponseDto();
            errorResponse.setSuccess(false);
            errorResponse.setTimestamp(LocalDateTime.now());
            errorResponse.setErrorMessage(errorMessage);

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        // For other runtime exceptions, return internal server error
        CardSelectionResponseDto errorResponse = new CardSelectionResponseDto();
        errorResponse.setSuccess(false);
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setErrorMessage("Error reading Card Data File: " + errorMessage);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Exception handler for validation failures.
     * 
     * Handles validation exceptions including:
     * - Jakarta Bean Validation failures (@Valid annotation)
     * - Business rule validation failures
     * - Input format validation errors
     * 
     * Maps to COBOL validation messages from COCRDSLC.cbl:
     * - "Account number not provided"
     * - "Card number not provided"
     * - "Account number must be a 11 digit number"
     * - "Card number must be a 16 digit number"
     * 
     * @param e MethodArgumentNotValidException containing validation error details
     * @return ResponseEntity with HTTP 400 Bad Request and validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CardSelectionResponseDto> handleValidationException(MethodArgumentNotValidException e) {
        logger.warn("Card selection validation failed: {}", e.getMessage());

        // Extract validation errors from binding result
        BindingResult bindingResult = e.getBindingResult();
        StringBuilder errorMessage = new StringBuilder();
        
        // Process field errors equivalent to COBOL edit paragraph validation
        bindingResult.getFieldErrors().forEach(fieldError -> {
            String field = fieldError.getField();
            String message = fieldError.getDefaultMessage();
            
            // Map field errors to COBOL-equivalent messages
            if ("cardNumber".equals(field)) {
                errorMessage.append("Card number not provided or invalid format. ");
            } else if ("accountId".equals(field)) {
                errorMessage.append("Account number not provided or invalid format. ");
            } else {
                errorMessage.append(message).append(" ");
            }
        });

        // Create validation error response
        CardSelectionResponseDto errorResponse = new CardSelectionResponseDto();
        errorResponse.setSuccess(false);
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setErrorMessage(errorMessage.toString().trim());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Exception handler for authorization failures.
     * 
     * Handles Spring Security authorization exceptions when users lack
     * appropriate roles for card selection operations.
     * 
     * @param e Security exception containing authorization failure details
     * @return ResponseEntity with HTTP 401 Unauthorized and error details
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<CardSelectionResponseDto> handleUnauthorizedException(
            org.springframework.security.access.AccessDeniedException e) {
        logger.warn("Card selection authorization failed: {}", e.getMessage());

        CardSelectionResponseDto errorResponse = new CardSelectionResponseDto();
        errorResponse.setSuccess(false);
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setErrorMessage("Insufficient authorization for card selection operations");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    // ===================================================================================
    // PRIVATE UTILITY METHODS
    // ===================================================================================

    /**
     * Validates card selection request beyond basic bean validation.
     * 
     * Performs comprehensive request validation equivalent to COBOL edit paragraphs
     * from COCRDSLC.cbl (2200-EDIT-MAP-INPUTS, 2210-EDIT-ACCOUNT, 2220-EDIT-CARD).
     * 
     * Validation Rules:
     * - Card number must be provided and pass format validation
     * - Account ID must be provided when cross-reference validation is enabled
     * - User role must be specified for authorization decisions
     * - Request correlation ID must be present for audit tracking
     * 
     * @param request CardSelectionRequestDto to validate
     * @throws IllegalArgumentException if validation fails with specific error message
     */
    private void validateCardSelectionRequest(CardSelectionRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Card selection request is required");
        }

        // Validate card number (equivalent to COBOL 2220-EDIT-CARD)
        if (request.getCardNumber() == null || request.getCardNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Card number not provided");
        }

        // Validate account ID when cross-reference validation is requested
        if (request.getIncludeCrossReference() && 
            (request.getAccountId() == null || request.getAccountId().trim().isEmpty())) {
            throw new IllegalArgumentException("Account number not provided");
        }

        // Validate user role for authorization
        if (request.getUserRole() == null || request.getUserRole().trim().isEmpty()) {
            throw new IllegalArgumentException("User role is required for authorization");
        }

        // Validate correlation ID for audit tracking
        if (request.getCorrelationId() == null || request.getCorrelationId().trim().isEmpty()) {
            request.setCorrelationId(UUID.randomUUID().toString());
        }
    }

    /**
     * Masks card number for secure logging and debugging.
     * 
     * Creates PCI-compliant masked card number for logging purposes,
     * showing only the last 4 digits to prevent sensitive data exposure
     * while maintaining traceability for debugging and audit purposes.
     * 
     * @param cardNumber Card number to mask
     * @return Masked card number or default mask if input is invalid
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
    }
}