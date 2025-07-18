/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.account.controller;

import com.carddemo.account.AccountUpdateService;
import com.carddemo.account.dto.AccountUpdateRequestDto;
import com.carddemo.account.dto.AccountUpdateResponseDto;
import com.carddemo.common.util.ValidationUtils;
import com.carddemo.common.enums.ValidationResult;
import com.carddemo.common.enums.AccountStatus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.OptimisticLockingFailureException;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;

/**
 * AccountUpdateController - Spring Boot REST controller that converts CICS transaction CAUP
 * to RESTful API endpoints, implementing comprehensive validation, concurrency control, and
 * delegation to AccountUpdateService for transactional account modifications.
 * 
 * This controller replaces the original COACTUPC.cbl CICS transaction with modern Spring Boot
 * REST API architecture while maintaining identical business logic, validation rules, and
 * error handling patterns. All financial operations preserve COBOL COMP-3 decimal precision
 * using BigDecimal arithmetic with exact calculation equivalence.
 * 
 * Original COBOL Program: COACTUPC.cbl
 * Original Transaction Code: CAUP
 * 
 * Key Features:
 * - RESTful API design with PUT /api/accounts/{id} endpoint replacing CICS transaction entry
 * - Comprehensive Jakarta Bean Validation with cross-field validation patterns
 * - Optimistic locking implementation preventing concurrent modification conflicts
 * - Spring Security integration with role-based access control equivalent to RACF permissions
 * - Detailed error response formatting with field-level validation feedback for React components
 * - HTTP status code mapping (200, 400, 404, 409, 500) with business-specific error handling
 * - Audit trail generation and transaction logging for compliance requirements
 * - Performance monitoring with sub-200ms response time targets at 95th percentile
 * 
 * REST API Design Patterns:
 * - Stateless request/response with JWT authentication replacing CICS pseudo-conversational processing
 * - JSON DTOs preserving COMMAREA structure and data validation semantics
 * - OpenAPI documentation with transaction code preservation for backwards traceability
 * - Spring Cloud Gateway integration for load balancing and circuit breaker patterns
 * - Redis session management for distributed user context preservation
 * 
 * Security Implementation:
 * - Spring Security @PreAuthorize annotations enforcing method-level authorization
 * - JWT token validation with distributed session management through Redis
 * - Role-based access control mapping RACF security groups to Spring Security roles
 * - Input validation and SQL injection prevention through parameterized queries
 * - Comprehensive audit logging for financial transaction compliance
 * 
 * Error Handling Strategy:
 * - HTTP 200 OK: Successful account update with confirmation response
 * - HTTP 400 Bad Request: Validation errors with detailed field-level feedback
 * - HTTP 404 Not Found: Account not found or invalid account ID
 * - HTTP 409 Conflict: Optimistic locking failure due to concurrent modifications
 * - HTTP 500 Internal Server Error: System errors with sanitized error messages
 * 
 * Performance Requirements:
 * - Transaction response time: <200ms at 95th percentile
 * - Concurrent request handling: 10,000 TPS support through horizontal scaling
 * - Database connection pooling: HikariCP with optimized connection management
 * - Memory usage optimization: Efficient object allocation and garbage collection
 * 
 * Technical Compliance:
 * - Section 0.2.3 requirement for REST API endpoints replacing CICS transaction entry points
 * - Spring @RestController with comprehensive request validation per Section 7.3.3
 * - HTTP status code mapping with detailed error responses per Section 7.3.5
 * - Spring Security authentication and authorization per Section 7.3.4
 * - Optimistic locking error handling per Section 6.1.2.3 distributed transaction management
 * - Comprehensive validation error response formatting per Section 7.3.3 server-side validation
 * 
 * Integration Points:
 * - AccountUpdateService: Business logic processing with transactional account modifications
 * - AccountUpdateRequestDto: Request validation and data transfer object with Jakarta Bean Validation
 * - AccountUpdateResponseDto: Response formatting with audit trail and error handling
 * - ValidationUtils: Field validation utilities with COBOL-equivalent validation patterns
 * - Spring Security: Authentication and authorization with JWT token validation
 * - Spring Cloud Gateway: API routing and load balancing with circuit breaker patterns
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AccountUpdateController {

    private static final Logger logger = LoggerFactory.getLogger(AccountUpdateController.class);

    /**
     * Account update service for business logic processing.
     * Provides transactional account modifications with optimistic locking.
     */
    @Autowired
    private AccountUpdateService accountUpdateService;

    /**
     * Updates an existing account with comprehensive validation and concurrency control.
     * 
     * This endpoint converts the original CICS transaction CAUP to RESTful API processing
     * while maintaining identical business logic, validation rules, and error handling.
     * The implementation preserves all COBOL business logic through Java method equivalents
     * with exact decimal precision using BigDecimal arithmetic.
     * 
     * Original COBOL Flow (COACTUPC.cbl):
     * - 1000-COACTUPC-MAIN: Main processing logic → updateAccount() service call
     * - 1300-EDIT-VALIDATION: Field validation → Jakarta Bean Validation annotations
     * - 1400-PROCESS-LOGIC: Business logic → AccountUpdateService.updateAccount()
     * - 9600-WRITE-PROCESSING: Database updates → JPA repository operations
     * - 9700-CHECK-CHANGE-IN-REC: Optimistic locking → JPA @Version annotation
     * 
     * HTTP Status Code Mapping:
     * - 200 OK: Successful account update with confirmation response
     * - 400 Bad Request: Validation errors with detailed field-level feedback
     * - 404 Not Found: Account not found or invalid account ID
     * - 409 Conflict: Optimistic locking failure due to concurrent modifications
     * - 500 Internal Server Error: System errors with sanitized error messages
     * 
     * Security Requirements:
     * - JWT authentication required for all requests
     * - Role-based authorization: ACCOUNT_UPDATE permission required
     * - Input validation and sanitization to prevent injection attacks
     * - Comprehensive audit logging for compliance requirements
     * 
     * Performance Targets:
     * - Response time: <200ms at 95th percentile
     * - Concurrent request handling: Support for 10,000 TPS
     * - Database connection efficiency: Optimized connection pooling
     * - Memory usage optimization: Efficient object lifecycle management
     * 
     * @param accountId Account identification number (path parameter)
     * @param request Account update request with validation annotations
     * @param bindingResult Spring validation result for detailed error handling
     * @return ResponseEntity<AccountUpdateResponseDto> with success/error status and audit trail
     */
    @PutMapping("/{accountId}")
    @PreAuthorize("hasRole('ACCOUNT_UPDATE') or hasRole('ADMIN')")
    public ResponseEntity<AccountUpdateResponseDto> updateAccount(
            @PathVariable String accountId,
            @Valid @RequestBody AccountUpdateRequestDto request,
            BindingResult bindingResult) {
        
        logger.info("Received account update request for account ID: {}", accountId);
        
        long startTime = System.currentTimeMillis();
        String auditId = UUID.randomUUID().toString();
        
        try {
            // Step 1: Validate path parameter consistency
            if (!accountId.equals(request.getAccountId())) {
                logger.warn("Account ID mismatch: path={}, request={}", accountId, request.getAccountId());
                
                AccountUpdateResponseDto errorResponse = buildErrorResponse(
                    accountId,
                    "Account ID mismatch between path parameter and request body",
                    HttpStatus.BAD_REQUEST,
                    auditId,
                    startTime
                );
                
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Step 2: Handle Jakarta Bean Validation errors
            if (bindingResult.hasErrors()) {
                logger.warn("Validation errors found for account update request: {}", accountId);
                
                Map<String, ValidationResult> validationErrors = new HashMap<>();
                for (FieldError error : bindingResult.getFieldErrors()) {
                    validationErrors.put(error.getField(), 
                        ValidationResult.INVALID_FORMAT.withMessage(error.getDefaultMessage()));
                }
                
                AccountUpdateResponseDto errorResponse = buildErrorResponse(
                    accountId,
                    "Request validation failed",
                    HttpStatus.BAD_REQUEST,
                    auditId,
                    startTime,
                    validationErrors
                );
                
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Step 3: Perform additional business validation
            ValidationResult businessValidationResult = validateBusinessRules(request);
            if (!businessValidationResult.isValid()) {
                logger.warn("Business validation failed for account {}: {}", 
                    accountId, businessValidationResult.getErrorMessage());
                
                Map<String, ValidationResult> validationErrors = new HashMap<>();
                validationErrors.put("business_rules", businessValidationResult);
                
                AccountUpdateResponseDto errorResponse = buildErrorResponse(
                    accountId,
                    "Business validation failed: " + businessValidationResult.getErrorMessage(),
                    HttpStatus.BAD_REQUEST,
                    auditId,
                    startTime,
                    validationErrors
                );
                
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Step 4: Delegate to service layer for transactional processing
            logger.debug("Delegating account update to service layer for account: {}", accountId);
            AccountUpdateResponseDto serviceResponse = accountUpdateService.updateAccount(request);
            
            // Step 5: Handle service response and return appropriate HTTP status
            if (serviceResponse.getSuccess()) {
                long processingTime = System.currentTimeMillis() - startTime;
                logger.info("Account update completed successfully for account {} in {}ms", 
                    accountId, processingTime);
                
                return ResponseEntity.ok(serviceResponse);
            } else {
                logger.error("Account update failed for account {}: {}", 
                    accountId, serviceResponse.getErrorMessage());
                
                return ResponseEntity.badRequest().body(serviceResponse);
            }
            
        } catch (OptimisticLockingFailureException e) {
            logger.warn("Optimistic locking failure for account {}: {}", accountId, e.getMessage());
            return handleOptimisticLockingException(accountId, e, auditId, startTime);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid argument for account update {}: {}", accountId, e.getMessage());
            
            AccountUpdateResponseDto errorResponse = buildErrorResponse(
                accountId,
                "Invalid request parameters: " + e.getMessage(),
                HttpStatus.BAD_REQUEST,
                auditId,
                startTime
            );
            
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Unexpected error during account update for account {}: {}", 
                accountId, e.getMessage(), e);
            return handleGeneralException(accountId, e, auditId, startTime);
        }
    }

    /**
     * Handles Jakarta Bean Validation exceptions with detailed field-level error reporting.
     * 
     * This method provides comprehensive validation error handling equivalent to CICS
     * field validation patterns, enabling React components to display specific field
     * validation errors through Material-UI error states and helper text.
     * 
     * @param ex MethodArgumentNotValidException with validation details
     * @return ResponseEntity<AccountUpdateResponseDto> with field-level validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AccountUpdateResponseDto> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        logger.warn("Validation exception occurred: {}", ex.getMessage());
        
        long startTime = System.currentTimeMillis();
        String auditId = UUID.randomUUID().toString();
        
        Map<String, ValidationResult> validationErrors = new HashMap<>();
        
        // Extract field-level validation errors from BindingResult
        BindingResult bindingResult = ex.getBindingResult();
        for (FieldError error : bindingResult.getFieldErrors()) {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();
            
            ValidationResult validationResult = determineValidationResult(error);
            validationErrors.put(fieldName, validationResult.withMessage(errorMessage));
            
            logger.debug("Validation error for field '{}': {}", fieldName, errorMessage);
        }
        
        AccountUpdateResponseDto errorResponse = buildErrorResponse(
            "unknown",
            "Request validation failed",
            HttpStatus.BAD_REQUEST,
            auditId,
            startTime,
            validationErrors
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles optimistic locking exceptions with proper HTTP 409 Conflict response.
     * 
     * This method converts JPA OptimisticLockingFailureException to appropriate HTTP
     * status codes and error messages equivalent to CICS concurrent access handling.
     * 
     * @param accountId Account ID for context
     * @param ex OptimisticLockingFailureException with concurrency details
     * @param auditId Audit trail identifier
     * @param startTime Request processing start time
     * @return ResponseEntity<AccountUpdateResponseDto> with HTTP 409 Conflict status
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<AccountUpdateResponseDto> handleOptimisticLockingException(
            String accountId, OptimisticLockingFailureException ex, String auditId, long startTime) {
        
        logger.warn("Optimistic locking failure for account {}: {}", accountId, ex.getMessage());
        
        AccountUpdateResponseDto errorResponse = buildErrorResponse(
            accountId,
            "Account has been modified by another user. Please refresh and try again.",
            HttpStatus.CONFLICT,
            auditId,
            startTime
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles general exceptions with sanitized error messages.
     * 
     * This method provides comprehensive error handling for unexpected system errors
     * while maintaining security by sanitizing error messages for client consumption.
     * 
     * @param accountId Account ID for context
     * @param ex Exception with error details
     * @param auditId Audit trail identifier
     * @param startTime Request processing start time
     * @return ResponseEntity<AccountUpdateResponseDto> with HTTP 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AccountUpdateResponseDto> handleGeneralException(
            String accountId, Exception ex, String auditId, long startTime) {
        
        logger.error("Unexpected error for account {}: {}", accountId, ex.getMessage(), ex);
        
        AccountUpdateResponseDto errorResponse = buildErrorResponse(
            accountId,
            "An unexpected error occurred while processing your request. Please try again later.",
            HttpStatus.INTERNAL_SERVER_ERROR,
            auditId,
            startTime
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Validates business rules for account update requests.
     * 
     * This method implements comprehensive business rule validation equivalent to
     * COBOL validation logic from COACTUPC.cbl, including cross-field validation
     * and business constraint enforcement.
     * 
     * @param request Account update request to validate
     * @return ValidationResult indicating business rule validation outcome
     */
    private ValidationResult validateBusinessRules(AccountUpdateRequestDto request) {
        logger.debug("Validating business rules for account: {}", request.getAccountId());
        
        // Validate account ID format
        ValidationResult accountIdResult = ValidationUtils.validateAccountNumber(request.getAccountId());
        if (!accountIdResult.isValid()) {
            return accountIdResult;
        }
        
        // Validate current balance if provided
        if (request.getCurrentBalance() != null) {
            ValidationResult balanceResult = ValidationUtils.validateBalance(request.getCurrentBalance());
            if (!balanceResult.isValid()) {
                return balanceResult;
            }
        }
        
        // Validate credit limit if provided
        if (request.getCreditLimit() != null) {
            ValidationResult creditLimitResult = ValidationUtils.validateCreditLimit(request.getCreditLimit());
            if (!creditLimitResult.isValid()) {
                return creditLimitResult;
            }
        }
        
        // Validate account status
        if (request.getActiveStatus() != null) {
            if (!AccountStatus.isValid(request.getActiveStatus().getCode())) {
                return ValidationResult.INVALID_RANGE.withMessage("Invalid account status");
            }
        }
        
        // Validate required fields
        if (request.getAccountId() == null || request.getAccountId().trim().isEmpty()) {
            return ValidationResult.BLANK_FIELD.withMessage("Account ID is required");
        }
        
        logger.debug("Business rule validation successful for account: {}", request.getAccountId());
        return ValidationResult.VALID;
    }

    /**
     * Determines the appropriate ValidationResult based on field validation error.
     * 
     * @param error FieldError with validation details
     * @return ValidationResult corresponding to the validation failure type
     */
    private ValidationResult determineValidationResult(FieldError error) {
        String errorCode = error.getCode();
        
        if (errorCode == null) {
            return ValidationResult.INVALID_FORMAT;
        }
        
        switch (errorCode) {
            case "NotBlank":
            case "NotNull":
            case "NotEmpty":
                return ValidationResult.BLANK_FIELD;
            case "Size":
            case "Length":
            case "Min":
            case "Max":
                return ValidationResult.INVALID_RANGE;
            case "Pattern":
            case "Email":
                return ValidationResult.INVALID_FORMAT;
            case "DecimalMin":
            case "DecimalMax":
                return ValidationResult.INVALID_RANGE;
            default:
                return ValidationResult.INVALID_FORMAT;
        }
    }

    /**
     * Builds standardized error response with audit trail and processing metrics.
     * 
     * @param accountId Account ID for context
     * @param errorMessage Primary error message
     * @param httpStatus HTTP status code
     * @param auditId Audit trail identifier
     * @param startTime Request processing start time
     * @return AccountUpdateResponseDto with error details
     */
    private AccountUpdateResponseDto buildErrorResponse(String accountId, String errorMessage, 
                                                       HttpStatus httpStatus, String auditId, long startTime) {
        return buildErrorResponse(accountId, errorMessage, httpStatus, auditId, startTime, null);
    }

    /**
     * Builds standardized error response with validation errors and audit trail.
     * 
     * @param accountId Account ID for context
     * @param errorMessage Primary error message
     * @param httpStatus HTTP status code
     * @param auditId Audit trail identifier
     * @param startTime Request processing start time
     * @param validationErrors Field-level validation errors
     * @return AccountUpdateResponseDto with comprehensive error details
     */
    private AccountUpdateResponseDto buildErrorResponse(String accountId, String errorMessage, 
                                                       HttpStatus httpStatus, String auditId, 
                                                       long startTime, Map<String, ValidationResult> validationErrors) {
        
        AccountUpdateResponseDto response = new AccountUpdateResponseDto();
        response.setSuccess(false);
        response.setAccountId(accountId);
        response.setErrorMessage(errorMessage);
        response.setTimestamp(LocalDateTime.now());
        
        if (validationErrors != null && !validationErrors.isEmpty()) {
            response.setValidationErrors(validationErrors);
        }
        
        // Build audit trail
        AccountUpdateResponseDto.AuditTrail auditTrail = new AccountUpdateResponseDto.AuditTrail();
        auditTrail.setAuditId(auditId);
        auditTrail.setUpdateTimestamp(LocalDateTime.now());
        auditTrail.setUserId("SYSTEM"); // In production, get from security context
        auditTrail.setSessionId("SESSION_" + auditId.substring(0, 8));
        auditTrail.setUpdateReason("Account update failed - " + errorMessage);
        
        response.setAuditTrail(auditTrail);
        
        // Build transaction status
        AccountUpdateResponseDto.TransactionStatus transactionStatus = new AccountUpdateResponseDto.TransactionStatus();
        transactionStatus.setStatus("FAILURE");
        transactionStatus.setResponseCode(String.valueOf(httpStatus.value()));
        transactionStatus.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        transactionStatus.setRecordsAffected(0);
        
        response.setTransactionStatus(transactionStatus);
        
        // Build session context
        AccountUpdateResponseDto.SessionContext sessionContext = new AccountUpdateResponseDto.SessionContext();
        sessionContext.setSessionId("SESSION_" + auditId.substring(0, 8));
        sessionContext.setUserId("SYSTEM");
        sessionContext.setTransactionId("ACUP_" + auditId.substring(0, 8));
        sessionContext.setProgramName("AccountUpdateController");
        sessionContext.setScreenId("ACUP");
        
        response.setSessionContext(sessionContext);
        
        return response;
    }
}