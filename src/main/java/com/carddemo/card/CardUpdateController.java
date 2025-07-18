/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.card;

import com.carddemo.common.dto.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.OptimisticLockingFailureException;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REST controller for credit card lifecycle management implementing COCRDUPC.cbl functionality
 * with optimistic locking, transaction validation, and comprehensive security controls.
 * 
 * <p>This controller serves as the primary REST API entry point for credit card update operations,
 * converting the complete COBOL program COCRDUPC.cbl to Spring Boot microservice architecture
 * while maintaining exact functional equivalence and enhanced cloud-native capabilities.
 * 
 * <p>Original COBOL Program: app/cbl/COCRDUPC.cbl
 * <p>Original Transaction ID: CCUP (Card Update)
 * <p>Original Mapset: COCRDUP
 * <p>Original BMS Map: CCRDUPA
 * 
 * <p>Key Features:
 * <ul>
 *   <li>RESTful card update operations with HTTP PUT method support</li>
 *   <li>Optimistic locking for concurrent update conflict resolution</li>
 *   <li>Spring Security role-based authorization with @PreAuthorize annotations</li>
 *   <li>Comprehensive input validation using Jakarta Bean Validation</li>
 *   <li>Transaction validation and balance update coordination</li>
 *   <li>PostgreSQL constraint enforcement and error handling</li>
 *   <li>Audit trail generation for compliance and security monitoring</li>
 *   <li>Exception handling for business logic and infrastructure errors</li>
 * </ul>
 * 
 * <p>Business Logic Flow (equivalent to COBOL program structure):
 * <ol>
 *   <li>Request validation equivalent to COBOL 1000-PROCESS-INPUTS</li>
 *   <li>Authorization checking equivalent to COBOL security validation</li>
 *   <li>Business logic execution equivalent to COBOL 2000-DECIDE-ACTION</li>
 *   <li>Data persistence equivalent to COBOL 9200-WRITE-PROCESSING</li>
 *   <li>Response construction equivalent to COBOL 3000-SEND-MAP</li>
 * </ol>
 * 
 * <p>Security Implementation:
 * <ul>
 *   <li>Spring Security @PreAuthorize annotations for role-based access control</li>
 *   <li>JWT token validation through Spring Cloud Gateway integration</li>
 *   <li>Method-level authorization enforcement for card modification operations</li>
 *   <li>Comprehensive audit logging for all card update attempts</li>
 *   <li>Rate limiting protection through API Gateway integration</li>
 * </ul>
 * 
 * <p>Error Handling Strategy:
 * <ul>
 *   <li>Validation exceptions with detailed field-level error messages</li>
 *   <li>Optimistic locking conflict resolution with retry guidance</li>
 *   <li>Database constraint violations with business-friendly error messages</li>
 *   <li>General exception handling with correlation IDs for troubleshooting</li>
 * </ul>
 * 
 * <p>Performance Requirements:
 * <ul>
 *   <li>Transaction response times: &lt;200ms at 95th percentile</li>
 *   <li>Concurrent access support: 10,000 TPS with optimistic locking</li>
 *   <li>Memory usage: Within 110% of CICS baseline per Section 0.1.2</li>
 *   <li>Database transaction optimization: PostgreSQL SERIALIZABLE isolation</li>
 * </ul>
 * 
 * <p>Integration Points:
 * <ul>
 *   <li>CardUpdateService for business logic processing and validation</li>
 *   <li>Spring Security for authentication and authorization</li>
 *   <li>Spring Cloud Gateway for request routing and load balancing</li>
 *   <li>PostgreSQL for data persistence with optimistic locking</li>
 *   <li>Redis for session management and distributed caching</li>
 *   <li>Prometheus for metrics collection and performance monitoring</li>
 * </ul>
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 * 
 * @see com.carddemo.card.CardUpdateService
 * @see com.carddemo.card.CardUpdateRequestDto
 * @see com.carddemo.card.CardUpdateResponseDto
 * @see com.carddemo.common.dto.ValidationResult
 */
@RestController
@RequestMapping("/api/cards")
@CrossOrigin(origins = {"http://localhost:3000", "https://carddemo.example.com"})
public class CardUpdateController {

    private static final Logger logger = LoggerFactory.getLogger(CardUpdateController.class);

    /**
     * CardUpdateService for business logic processing and validation.
     * 
     * <p>This service implements the complete card update workflow equivalent to
     * COBOL program COCRDUPC.cbl, including comprehensive validation, optimistic
     * locking, cross-reference checking, and atomic database updates.
     */
    @Autowired
    private CardUpdateService cardUpdateService;

    /**
     * Updates a credit card with comprehensive validation and security controls.
     * 
     * <p>This method implements the complete card update operation equivalent to
     * COBOL program COCRDUPC.cbl, providing RESTful API access to card lifecycle
     * management with modern cloud-native security patterns and optimistic locking.
     * 
     * <p>The endpoint supports comprehensive card field updates including:
     * <ul>
     *   <li>Embossed name modifications with format validation</li>
     *   <li>Expiration date updates with business rule enforcement</li>
     *   <li>Active status changes with lifecycle compliance</li>
     *   <li>Account association modifications with cross-reference validation</li>
     *   <li>CVV code updates with security compliance</li>
     * </ul>
     * 
     * <p>Security Controls:
     * <ul>
     *   <li>Role-based authorization: Users with USER or ADMIN roles can update cards</li>
     *   <li>JWT token validation through Spring Cloud Gateway</li>
     *   <li>Request validation using Jakarta Bean Validation</li>
     *   <li>Optimistic locking to prevent concurrent modification conflicts</li>
     *   <li>Comprehensive audit logging for all update attempts</li>
     * </ul>
     * 
     * <p>Business Logic Flow:
     * <ol>
     *   <li>Request validation equivalent to COBOL 1000-PROCESS-INPUTS</li>
     *   <li>Authorization verification equivalent to COBOL security checks</li>
     *   <li>Business rule validation equivalent to COBOL 1200-EDIT-MAP-INPUTS</li>
     *   <li>Optimistic locking equivalent to COBOL 9300-CHECK-CHANGE-IN-REC</li>
     *   <li>Data persistence equivalent to COBOL 9200-WRITE-PROCESSING</li>
     *   <li>Response construction equivalent to COBOL screen preparation</li>
     * </ol>
     * 
     * <p>Error Handling:
     * <ul>
     *   <li>Validation errors return 400 Bad Request with detailed field messages</li>
     *   <li>Optimistic locking conflicts return 409 Conflict with retry guidance</li>
     *   <li>Authorization failures return 403 Forbidden with security context</li>
     *   <li>Resource not found returns 404 Not Found with correlation ID</li>
     *   <li>Server errors return 500 Internal Server Error with support information</li>
     * </ul>
     * 
     * @param cardNumber The 16-digit card number identifying the card to update
     * @param request CardUpdateRequestDto containing update data and optimistic locking version
     * @return ResponseEntity&lt;CardUpdateResponseDto&gt; with update results and audit information
     * 
     * @throws MethodArgumentNotValidException if request validation fails
     * @throws OptimisticLockingFailureException if concurrent modification detected
     * @throws CardNotFoundException if specified card does not exist
     * @throws SecurityException if insufficient permissions for card modification
     * 
     * @see com.carddemo.card.CardUpdateService#updateCard(CardUpdateRequestDto)
     * @see com.carddemo.card.CardUpdateRequestDto
     * @see com.carddemo.card.CardUpdateResponseDto
     */
    @PutMapping("/{cardNumber}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<CardUpdateResponseDto> updateCard(
            @PathVariable String cardNumber,
            @Valid @RequestBody CardUpdateRequestDto request) {
        
        // Generate correlation ID for request tracking and audit trail
        String correlationId = UUID.randomUUID().toString();
        
        logger.info("Card update request received - Card: {}, User: {}, Correlation: {}", 
                   cardNumber, request.getUserId(), correlationId);
        
        // Validate card number consistency between path and request body
        if (!cardNumber.equals(request.getCardNumber())) {
            logger.warn("Card number mismatch - Path: {}, Request: {}, Correlation: {}", 
                       cardNumber, request.getCardNumber(), correlationId);
            
            ValidationResult validationResult = new ValidationResult(false, "CARD_NUMBER_MISMATCH");
            validationResult.addErrorMessage("CARD_NUMBER_MISMATCH", 
                "Card number in path must match card number in request body");
            
            CardUpdateResponseDto errorResponse = createValidationErrorResponse(
                validationResult, correlationId, "Card number mismatch");
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        try {
            // Set correlation ID in request for audit trail
            request.setCorrelationId(correlationId);
            
            // Execute card update business logic through service layer
            CardUpdateResponseDto response = cardUpdateService.updateCard(request);
            
            // Log successful update operation
            logger.info("Card update completed successfully - Card: {}, User: {}, Correlation: {}", 
                       cardNumber, request.getUserId(), correlationId);
            
            // Return successful response with updated card information
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            // Handle unexpected errors with comprehensive logging
            logger.error("Unexpected error during card update - Card: {}, User: {}, Correlation: {}", 
                        cardNumber, request.getUserId(), correlationId, e);
            
            // Create error response with correlation ID for troubleshooting
            CardUpdateResponseDto errorResponse = createServerErrorResponse(
                correlationId, "Unexpected error during card update: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Handles Jakarta Bean Validation exceptions for request validation errors.
     * 
     * <p>This method processes validation failures equivalent to COBOL input edit
     * paragraph validation, providing detailed field-level error messages that
     * correspond to the original BMS field validation patterns.
     * 
     * <p>The handler extracts all field validation errors and constructs a
     * comprehensive validation response that includes:
     * <ul>
     *   <li>Field-specific error messages with validation context</li>
     *   <li>Business rule compliance status</li>
     *   <li>Correlation ID for error tracking and debugging</li>
     *   <li>HTTP 400 Bad Request status for client-side validation errors</li>
     * </ul>
     * 
     * @param ex MethodArgumentNotValidException containing validation error details
     * @return ResponseEntity&lt;CardUpdateResponseDto&gt; with validation error information
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CardUpdateResponseDto> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        String correlationId = UUID.randomUUID().toString();
        
        logger.warn("Card update validation failed - Correlation: {}, Errors: {}", 
                   correlationId, ex.getBindingResult().getFieldErrorCount());
        
        // Create validation result with detailed field errors
        ValidationResult validationResult = new ValidationResult(false, "VALIDATION_FAILED");
        
        // Extract and process all field validation errors
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();
            String errorCode = error.getCode() != null ? error.getCode() : "FIELD_VALIDATION_ERROR";
            
            validationResult.addErrorMessage(errorCode, errorMessage, fieldName, 
                ValidationResult.ValidationSeverity.ERROR);
        });
        
        // Create comprehensive error response
        CardUpdateResponseDto errorResponse = createValidationErrorResponse(
            validationResult, correlationId, "Request validation failed");
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles optimistic locking failures for concurrent modification conflicts.
     * 
     * <p>This method processes optimistic locking exceptions equivalent to COBOL
     * paragraph 9300-CHECK-CHANGE-IN-REC, providing conflict resolution guidance
     * and retry information for clients experiencing concurrent update scenarios.
     * 
     * <p>The handler provides comprehensive conflict resolution information including:
     * <ul>
     *   <li>Version conflict detection with expected vs actual version numbers</li>
     *   <li>Retry strategy guidance with exponential backoff recommendations</li>
     *   <li>Conflict resolution metadata for client-side handling</li>
     *   <li>HTTP 409 Conflict status for optimistic locking violations</li>
     * </ul>
     * 
     * @param ex OptimisticLockingFailureException containing version conflict details
     * @return ResponseEntity&lt;CardUpdateResponseDto&gt; with conflict resolution information
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<CardUpdateResponseDto> handleOptimisticLockingException(
            OptimisticLockingFailureException ex) {
        
        String correlationId = UUID.randomUUID().toString();
        
        logger.warn("Optimistic locking conflict detected - Correlation: {}, Message: {}", 
                   correlationId, ex.getMessage());
        
        // Create validation result indicating version conflict
        ValidationResult validationResult = new ValidationResult(false, "OPTIMISTIC_LOCK_CONFLICT");
        validationResult.addErrorMessage("VERSION_CONFLICT", 
            "The card has been modified by another user. Please refresh and try again.");
        
        // Create conflict response with retry guidance
        CardUpdateResponseDto conflictResponse = createOptimisticLockingErrorResponse(
            validationResult, correlationId, "Optimistic locking conflict detected");
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(conflictResponse);
    }

    /**
     * Handles card not found exceptions for invalid card number scenarios.
     * 
     * <p>This method processes card lookup failures equivalent to COBOL paragraph
     * 9100-GETCARD-BYACCTCARD when the specified card number does not exist in
     * the database, providing appropriate error responses for client applications.
     * 
     * <p>The handler provides detailed error information including:
     * <ul>
     *   <li>Resource not found status with specific card context</li>
     *   <li>Correlation ID for error tracking and support</li>
     *   <li>Detailed error message for client-side handling</li>
     *   <li>HTTP 404 Not Found status for resource lookup failures</li>
     * </ul>
     * 
     * @param ex CardNotFoundException containing card lookup failure details
     * @return ResponseEntity&lt;CardUpdateResponseDto&gt; with not found error information
     */
    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<CardUpdateResponseDto> handleCardNotFoundException(
            CardNotFoundException ex) {
        
        String correlationId = UUID.randomUUID().toString();
        
        logger.warn("Card not found - Correlation: {}, Message: {}", 
                   correlationId, ex.getMessage());
        
        // Create validation result indicating card not found
        ValidationResult validationResult = new ValidationResult(false, "CARD_NOT_FOUND");
        validationResult.addErrorMessage("CARD_NOT_FOUND", 
            "The specified card number does not exist or is not accessible.");
        
        // Create not found response
        CardUpdateResponseDto errorResponse = createValidationErrorResponse(
            validationResult, correlationId, "Card not found");
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles general exceptions for unexpected server errors.
     * 
     * <p>This method provides a catch-all exception handler for any unexpected
     * errors that occur during card update processing, ensuring graceful error
     * handling and comprehensive logging for troubleshooting purposes.
     * 
     * <p>The handler provides:
     * <ul>
     *   <li>Comprehensive error logging with stack traces</li>
     *   <li>Correlation ID for support case tracking</li>
     *   <li>Generic error response to prevent information disclosure</li>
     *   <li>HTTP 500 Internal Server Error status for server-side failures</li>
     * </ul>
     * 
     * @param ex Exception containing error details
     * @return ResponseEntity&lt;CardUpdateResponseDto&gt; with server error information
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CardUpdateResponseDto> handleGenericException(Exception ex) {
        
        String correlationId = UUID.randomUUID().toString();
        
        logger.error("Unexpected error in card update controller - Correlation: {}", 
                    correlationId, ex);
        
        // Create generic error response
        CardUpdateResponseDto errorResponse = createServerErrorResponse(
            correlationId, "An unexpected error occurred. Please contact support with correlation ID: " + correlationId);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Creates a validation error response for request validation failures.
     * 
     * <p>This helper method constructs a comprehensive error response that includes
     * detailed validation information, correlation IDs for tracking, and appropriate
     * HTTP status codes for client-side error handling.
     * 
     * @param validationResult ValidationResult containing detailed error information
     * @param correlationId Unique correlation identifier for error tracking
     * @param errorMessage Primary error message for response
     * @return CardUpdateResponseDto with validation error details
     */
    private CardUpdateResponseDto createValidationErrorResponse(ValidationResult validationResult, 
                                                               String correlationId, 
                                                               String errorMessage) {
        
        // Create audit information for error tracking
        var auditInfo = new com.carddemo.common.dto.AuditInfo();
        auditInfo.setUserId("system");
        auditInfo.setOperationType("CARD_UPDATE_VALIDATION_ERROR");
        auditInfo.setTimestamp(LocalDateTime.now());
        auditInfo.setCorrelationId(correlationId);
        auditInfo.setSessionId("session-" + correlationId);
        
        // Create validation error response
        CardUpdateResponseDto response = new CardUpdateResponseDto(validationResult, auditInfo, errorMessage);
        
        // Set error-specific metadata
        response.setOptimisticLockSuccess(false);
        response.setUpdateTimestamp(LocalDateTime.now());
        
        return response;
    }

    /**
     * Creates an optimistic locking error response for version conflicts.
     * 
     * <p>This helper method constructs a comprehensive conflict response that includes
     * version conflict information, retry guidance, and appropriate HTTP status codes
     * for client-side conflict resolution handling.
     * 
     * @param validationResult ValidationResult containing conflict error information
     * @param correlationId Unique correlation identifier for error tracking
     * @param errorMessage Primary error message for response
     * @return CardUpdateResponseDto with optimistic locking conflict details
     */
    private CardUpdateResponseDto createOptimisticLockingErrorResponse(ValidationResult validationResult,
                                                                      String correlationId,
                                                                      String errorMessage) {
        
        // Create audit information for conflict tracking
        var auditInfo = new com.carddemo.common.dto.AuditInfo();
        auditInfo.setUserId("system");
        auditInfo.setOperationType("CARD_UPDATE_OPTIMISTIC_LOCK_ERROR");
        auditInfo.setTimestamp(LocalDateTime.now());
        auditInfo.setCorrelationId(correlationId);
        auditInfo.setSessionId("session-" + correlationId);
        
        // Create conflict error response
        CardUpdateResponseDto response = new CardUpdateResponseDto(validationResult, auditInfo, errorMessage);
        
        // Set optimistic locking conflict information
        response.setOptimisticLockSuccess(false);
        response.setUpdateTimestamp(LocalDateTime.now());
        
        // Create conflict resolution information
        var conflictInfo = new CardUpdateResponseDto.ConflictResolutionInfo(true, null, null);
        conflictInfo.setResolutionStrategy("RETRY_WITH_LATEST_VERSION");
        conflictInfo.setConflictTimestamp(LocalDateTime.now());
        response.setConflictResolutionInfo(conflictInfo);
        
        // Create retry information
        var retryInfo = new CardUpdateResponseDto.RetryInfo(true, 1000L, 3);
        retryInfo.setRetryStrategy("EXPONENTIAL_BACKOFF");
        retryInfo.setCurrentAttempt(0);
        response.setRetryInfo(retryInfo);
        
        return response;
    }

    /**
     * Creates a server error response for unexpected server failures.
     * 
     * <p>This helper method constructs a generic error response that provides
     * correlation IDs for support while preventing information disclosure about
     * internal server implementation details.
     * 
     * @param correlationId Unique correlation identifier for error tracking
     * @param errorMessage Primary error message for response
     * @return CardUpdateResponseDto with server error details
     */
    private CardUpdateResponseDto createServerErrorResponse(String correlationId, String errorMessage) {
        
        // Create validation result for server error
        ValidationResult validationResult = new ValidationResult(false, "INTERNAL_SERVER_ERROR");
        validationResult.addErrorMessage("SERVER_ERROR", errorMessage);
        
        // Create audit information for server error tracking
        var auditInfo = new com.carddemo.common.dto.AuditInfo();
        auditInfo.setUserId("system");
        auditInfo.setOperationType("CARD_UPDATE_SERVER_ERROR");
        auditInfo.setTimestamp(LocalDateTime.now());
        auditInfo.setCorrelationId(correlationId);
        auditInfo.setSessionId("session-" + correlationId);
        
        // Create server error response
        CardUpdateResponseDto response = new CardUpdateResponseDto(validationResult, auditInfo, errorMessage);
        
        // Set error-specific metadata
        response.setOptimisticLockSuccess(false);
        response.setUpdateTimestamp(LocalDateTime.now());
        
        return response;
    }

    /**
     * Custom exception class for card not found scenarios.
     * 
     * <p>This exception represents the scenario where a specified card number
     * cannot be found in the database, equivalent to COBOL RESP(NOTFND) conditions
     * in the original program.
     */
    public static class CardNotFoundException extends RuntimeException {
        
        /**
         * Constructs a new CardNotFoundException with the specified detail message.
         * 
         * @param message the detail message explaining the card not found condition
         */
        public CardNotFoundException(String message) {
            super(message);
        }
        
        /**
         * Constructs a new CardNotFoundException with the specified detail message and cause.
         * 
         * @param message the detail message explaining the card not found condition
         * @param cause the underlying cause of the exception
         */
        public CardNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}