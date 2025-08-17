package com.carddemo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler for CardDemo Application
 * 
 * This @ControllerAdvice class centralizes error handling for all REST controllers,
 * mapping various exception types to appropriate HTTP responses while maintaining
 * error codes and message formats compatible with legacy COBOL ABEND routines.
 * 
 * The exception handler maintains compatibility with COBOL ABEND-DATA structure:
 * - errorCode (4 chars like ABEND-CODE)  
 * - culprit (8 chars like ABEND-CULPRIT)
 * - reason (50 chars like ABEND-REASON)
 * - message (72 chars like ABEND-MSG)
 * 
 * All exceptions are logged for comprehensive audit trails and compliance tracking.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    // COBOL ABEND-like constants for error classification
    private static final String BUSINESS_ERROR_CODE = "9001";
    private static final String VALIDATION_ERROR_CODE = "9002";
    private static final String NOT_FOUND_ERROR_CODE = "9404";
    private static final String PRECISION_ERROR_CODE = "9003";
    private static final String CONCURRENCY_ERROR_CODE = "9409";
    private static final String ACCESS_DENIED_CODE = "9403";
    private static final String AUTH_ERROR_CODE = "9401";
    private static final String GENERIC_ERROR_CODE = "9999";
    
    private static final String CULPRIT_PROGRAM = "CARDAPI";  // 8 chars max like COBOL program name

    /**
     * Handles business rule violations that replace COBOL ABEND routines.
     * Maps business logic errors to appropriate HTTP status codes with
     * standardized error response format.
     *
     * @param ex the BusinessRuleException thrown
     * @return ResponseEntity with 400 Bad Request status and error details
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleException(BusinessRuleException ex) {
        HttpServletRequest request = getCurrentRequest();
        
        logger.warn("Business rule violation: {} | Request: {} {} | Remote: {}", 
                   ex.getMessage(), 
                   request.getMethod(),
                   request.getRequestURI(),
                   request.getRemoteAddr());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode() != null ? ex.getErrorCode() : BUSINESS_ERROR_CODE)
                .culprit(CULPRIT_PROGRAM)
                .reason(truncateToLength(ex.getMessage(), 50))
                .message(truncateToLength("Business rule violation: " + ex.getMessage(), 72))
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles field-level validation failures that replicate COBOL edit routine
     * error handling. Provides field-specific error details compatible with BMS 
     * screen error highlighting patterns.
     *
     * @param ex the ValidationException thrown
     * @return ResponseEntity with 400 Bad Request status and field-level error details
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        HttpServletRequest request = getCurrentRequest();
        
        logger.warn("Validation failure: {} | Fields with errors: {} | Request: {} {} | Remote: {}", 
                   ex.getMessage(),
                   ex.hasFieldErrors() ? ex.getFieldErrors().keySet() : "none",
                   request.getMethod(),
                   request.getRequestURI(),
                   request.getRemoteAddr());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(VALIDATION_ERROR_CODE)
                .culprit(CULPRIT_PROGRAM)
                .reason(truncateToLength("Field validation failed", 50))
                .message(truncateToLength("Validation errors: " + ex.getMessage(), 72))
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .fieldErrors(ex.hasFieldErrors() ? ex.getFieldErrors() : new HashMap<>())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles resource not found scenarios equivalent to VSAM NOTFND condition.
     * Maps to HTTP 404 responses with detailed resource information.
     *
     * @param ex the ResourceNotFoundException thrown
     * @return ResponseEntity with 404 Not Found status and resource details
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        HttpServletRequest request = getCurrentRequest();
        
        logger.info("Resource not found: {} {} | Request: {} {} | Remote: {}", 
                   ex.getResourceType(),
                   ex.getResourceId(),
                   request.getMethod(),
                   request.getRequestURI(),
                   request.getRemoteAddr());
        
        String searchInfo = ex.getSearchCriteria() != null && !ex.getSearchCriteria().isEmpty() 
                ? " Search criteria: " + ex.getSearchCriteria().toString()
                : "";
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(NOT_FOUND_ERROR_CODE)
                .culprit(CULPRIT_PROGRAM)
                .reason(truncateToLength("Resource not found: " + ex.getResourceType(), 50))
                .message(truncateToLength("Not found: " + ex.getMessage() + searchInfo, 72))
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles BigDecimal precision errors in financial calculations.
     * Critical for preventing precision loss violations in monetary calculations
     * that must maintain COBOL COMP-3 packed decimal accuracy.
     *
     * @param ex the DataPrecisionException thrown
     * @return ResponseEntity with 500 Internal Server Error status and precision details
     */
    @ExceptionHandler(DataPrecisionException.class)
    public ResponseEntity<ErrorResponse> handleDataPrecisionException(DataPrecisionException ex) {
        HttpServletRequest request = getCurrentRequest();
        
        logger.error("Financial precision error: {} | Expected: {} Actual: {} | Value: {} | Request: {} {} | Remote: {}", 
                    ex.getMessage(),
                    ex.getPrecisionExpected(),
                    ex.getPrecisionActual(),
                    ex.getProblematicValue(),
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getRemoteAddr());
        
        String precisionDetails = String.format("Expected precision: %d, Actual: %d", 
                                               ex.getPrecisionExpected(), 
                                               ex.getPrecisionActual());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(PRECISION_ERROR_CODE)
                .culprit(CULPRIT_PROGRAM)
                .reason(truncateToLength("Financial calculation precision error", 50))
                .message(truncateToLength("Precision violation: " + precisionDetails, 72))
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles concurrent update conflicts when optimistic locking fails.
     * Maps to HTTP 409 Conflict responses for CICS READ UPDATE/REWRITE
     * equivalent scenarios.
     *
     * @param ex the ConcurrencyException thrown
     * @return ResponseEntity with 409 Conflict status and retry guidance
     */
    @ExceptionHandler(ConcurrencyException.class)
    public ResponseEntity<ErrorResponse> handleConcurrencyException(ConcurrencyException ex) {
        HttpServletRequest request = getCurrentRequest();
        
        logger.warn("Concurrent update conflict: {} | Entity: {} ID: {} | Request: {} {} | Remote: {}", 
                   ex.getMessage(),
                   ex.getEntityType(),
                   ex.getEntityId(),
                   request.getMethod(),
                   request.getRequestURI(),
                   request.getRemoteAddr());
        
        String conflictDetails = String.format("Entity: %s, ID: %s", 
                                             ex.getEntityType(), 
                                             ex.getEntityId());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(CONCURRENCY_ERROR_CODE)
                .culprit(CULPRIT_PROGRAM)
                .reason(truncateToLength("Concurrent update detected", 50))
                .message(truncateToLength("Conflict: " + conflictDetails + " - retry required", 72))
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles Spring Security access denied exceptions.
     * Provides standardized 403 Forbidden responses that maintain compatibility
     * with RACF authorization error patterns from the mainframe system.
     *
     * @param ex the AccessDeniedException thrown
     * @return ResponseEntity with 403 Forbidden status and access details
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        HttpServletRequest request = getCurrentRequest();
        
        logger.warn("Access denied: {} | Request: {} {} | Remote: {}", 
                   ex.getMessage(),
                   request.getMethod(),
                   request.getRequestURI(),
                   request.getRemoteAddr());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ACCESS_DENIED_CODE)
                .culprit(CULPRIT_PROGRAM)
                .reason(truncateToLength("Access denied - insufficient privileges", 50))
                .message(truncateToLength("Authorization failed: " + ex.getMessage(), 72))
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handles Spring Security authentication exceptions.
     * Provides standardized 401 Unauthorized responses for invalid credentials,
     * session timeouts, and token expiration scenarios that replace CICS SIGNON
     * authentication errors.
     *
     * @param ex the AuthenticationException thrown
     * @return ResponseEntity with 401 Unauthorized status and authentication details
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        HttpServletRequest request = getCurrentRequest();
        
        logger.warn("Authentication failed: {} | Request: {} {} | Remote: {}", 
                   ex.getMessage(),
                   request.getMethod(),
                   request.getRequestURI(),
                   request.getRemoteAddr());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(AUTH_ERROR_CODE)
                .culprit(CULPRIT_PROGRAM)
                .reason(truncateToLength("Authentication required", 50))
                .message(truncateToLength("Authentication failed: " + ex.getMessage(), 72))
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handles Spring validation failures when @Valid annotation validation fails.
     * Extracts field-level validation errors and formats them in ErrorResponse
     * structure compatible with BMS screen field highlighting patterns.
     *
     * @param ex the MethodArgumentNotValidException thrown
     * @return ResponseEntity with 400 Bad Request status and detailed field errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        HttpServletRequest request = getCurrentRequest();
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        });
        
        logger.warn("Request validation failed: {} field errors | Request: {} {} | Remote: {}", 
                   fieldErrors.size(),
                   request.getMethod(),
                   request.getRequestURI(),
                   request.getRemoteAddr());
        
        String fieldList = fieldErrors.keySet().toString();
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(VALIDATION_ERROR_CODE)
                .culprit(CULPRIT_PROGRAM)
                .reason(truncateToLength("Request validation failed", 50))
                .message(truncateToLength("Invalid fields: " + fieldList, 72))
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles all other unhandled exceptions as a fallback.
     * Provides a standardized error response for unexpected exceptions while
     * logging full stack traces for debugging and monitoring.
     *
     * @param ex the generic Exception thrown
     * @return ResponseEntity with 500 Internal Server Error status and generic error details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        HttpServletRequest request = getCurrentRequest();
        
        logger.error("Unexpected error occurred: {} | Request: {} {} | Remote: {}", 
                    ex.getMessage(),
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getRemoteAddr(),
                    ex);  // Include full stack trace for debugging
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(GENERIC_ERROR_CODE)
                .culprit(CULPRIT_PROGRAM)
                .reason(truncateToLength("Internal system error", 50))
                .message(truncateToLength("System error occurred - please contact support", 72))
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Retrieves the current HTTP request from the request context.
     * Used by exception handlers to extract request information for logging
     * and error response construction.
     *
     * @return the current HttpServletRequest
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * Truncates a string to the specified maximum length to maintain compatibility
     * with COBOL field length constraints from the ABEND-DATA structure.
     * 
     * This method ensures error messages fit within the COBOL field limits:
     * - ABEND-REASON: 50 characters
     * - ABEND-MSG: 72 characters
     *
     * @param text the text to truncate
     * @param maxLength the maximum allowed length
     * @return the truncated text with ellipsis if truncation occurred
     */
    private String truncateToLength(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}