/*
 * BillPaymentController.java
 * 
 * CardDemo Application
 * 
 * REST controller for bill payment processing implementing COBIL00C.cbl functionality with 
 * real-time balance updates, transaction audit trails, and comprehensive security controls 
 * for payment operations in distributed microservices architecture.
 * 
 * This controller provides HTTP REST endpoints for bill payment processing while maintaining 
 * exact functional equivalence with the original COBOL COBIL00C.cbl program. The implementation 
 * preserves all business logic, validation rules, and processing patterns from the mainframe 
 * application while providing modern REST API capabilities for React frontend integration.
 * 
 * Key Business Logic Preserved from COBIL00C.cbl:
 * - Account ID validation (required, must exist, must be active)
 * - Current balance validation (must be > 0 to have something to pay)
 * - Confirmation flag processing ('Y'/'y' to proceed, 'N'/'n' to cancel)
 * - Full balance payment (not partial - pays entire current balance)
 * - Transaction ID generation (find highest existing ID and increment by 1)
 * - Specific transaction attributes (type '02', category 2, merchant 999999999)
 * - Card number lookup from account cross-reference (CXACAIX equivalent)
 * - Account balance update to zero after successful payment
 * - COBOL message formatting: "Payment successful. Your Transaction ID is {ID}."
 * 
 * Security Features:
 * - JWT-based authentication with Spring Security integration
 * - Role-based access control using @PreAuthorize annotations
 * - Comprehensive audit logging for all payment operations
 * - Request validation with field-level error reporting
 * - CORS configuration for secure React frontend communication
 * 
 * Performance Characteristics:
 * - Sub-200ms response time requirement at 95th percentile
 * - Support for 10,000+ TPS throughput with horizontal scaling
 * - Memory usage within 10% increase limit compared to CICS allocation
 * - Real-time balance updates with ACID transaction compliance
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carddemo.card;

import com.carddemo.card.BillPaymentService;
import com.carddemo.card.BillPaymentRequestDto;
import com.carddemo.card.BillPaymentResponseDto;
import com.carddemo.common.security.SecurityConfig;
import com.carddemo.common.dto.ValidationResult;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.slf4j.LoggerFactory;

import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST controller for bill payment processing with complete COBIL00C.cbl functional equivalence.
 * 
 * <p>This controller implements the complete bill payment workflow from the original COBOL program:
 * <ol>
 *   <li>Validates payment request parameters and business rules</li>
 *   <li>Verifies account exists, is active, and has balance > 0</li>
 *   <li>Processes confirmation flag ('Y' to proceed, 'N' to cancel)</li>
 *   <li>Calculates payment amount (always full current balance)</li>
 *   <li>Generates unique transaction ID by incrementing highest existing ID</li>
 *   <li>Creates transaction record with exact COBOL field mappings</li>
 *   <li>Updates account balance to zero atomically</li>
 *   <li>Returns comprehensive payment response with audit information</li>
 * </ol>
 * 
 * <p>The controller maintains exact error message compatibility with the original COBOL program:
 * <ul>
 *   <li>"Acct ID can NOT be empty..." - when account ID is missing</li>
 *   <li>"Account ID NOT found..." - when account doesn't exist</li>
 *   <li>"You have nothing to pay..." - when account balance is zero or negative</li>
 *   <li>"Invalid value. Valid values are (Y/N)..." - for invalid confirmation flags</li>
 *   <li>"Payment successful. Your Transaction ID is {ID}." - for successful payments</li>
 *   <li>"Confirm to make a bill payment..." - when confirmation is required</li>
 * </ul>
 * 
 * <p>Security implementation includes:
 * <ul>
 *   <li>JWT authentication through Spring Security integration</li>
 *   <li>Role-based authorization with @PreAuthorize annotations</li>
 *   <li>Comprehensive audit logging for compliance requirements</li>
 *   <li>Request validation with field-level error reporting</li>
 *   <li>Protection against common web vulnerabilities (CSRF, XSS)</li>
 * </ul>
 * 
 * <p>Performance optimizations include:
 * <ul>
 *   <li>Stateless operation design for horizontal scalability</li>
 *   <li>Efficient validation with early error returns</li>
 *   <li>Optimized database operations through JPA repositories</li>
 *   <li>Connection pooling and transaction management optimization</li>
 * </ul>
 * 
 * @author Blitzy agent
 * @version CardDemo_v1.0-15-g27d6c6f-68
 * @since 2024-01-01
 * @see BillPaymentService
 * @see BillPaymentRequestDto
 * @see BillPaymentResponseDto
 */
@RestController
@RequestMapping("/api/cards/{cardId}/bill-payment")
@CrossOrigin(origins = {"${carddemo.security.cors.allowed-origins:http://localhost:3000,https://localhost:3000}"})
public class BillPaymentController {

    private static final Logger logger = LoggerFactory.getLogger(BillPaymentController.class);

    /**
     * Business service for bill payment processing with real-time balance updates and audit trails.
     * Implements all COBIL00C.cbl business logic including account validation, transaction creation,
     * and balance updates with BigDecimal precision equivalent to COBOL COMP-3 arithmetic.
     */
    @Autowired
    private BillPaymentService billPaymentService;

    /**
     * Processes bill payment requests with comprehensive validation and transaction management.
     * 
     * <p>This method implements the main payment processing workflow equivalent to the COBOL
     * PROCESS-ENTER-KEY paragraph in COBIL00C.cbl. It handles the complete payment lifecycle:</p>
     * 
     * <p><strong>Request Processing Steps:</strong></p>
     * <ol>
     *   <li>Authentication verification through JWT token validation</li>
     *   <li>Authorization check for CARD_USER or ADMIN roles</li>
     *   <li>Request validation using Jakarta Bean Validation</li>
     *   <li>Business rule validation through BillPaymentService</li>
     *   <li>Payment processing with real-time balance updates</li>
     *   <li>Audit trail creation for compliance tracking</li>
     *   <li>Response formatting with transaction confirmation</li>
     * </ol>
     * 
     * <p><strong>COBOL Business Logic Preserved:</strong></p>
     * <ul>
     *   <li>Account ID validation matching COBOL lines 159-167</li>
     *   <li>Confirmation flag processing matching COBOL lines 173-191</li>
     *   <li>Balance verification matching COBOL lines 198-206</li>
     *   <li>Transaction creation matching COBOL lines 212-233</li>
     *   <li>Account update matching COBOL lines 234-235</li>
     *   <li>Success message formatting matching COBOL lines 527-532</li>
     * </ul>
     * 
     * <p><strong>Error Handling:</strong></p>
     * <ul>
     *   <li>HTTP 400 (Bad Request): Validation errors, business rule violations</li>
     *   <li>HTTP 401 (Unauthorized): Authentication failures</li>
     *   <li>HTTP 403 (Forbidden): Authorization failures</li>
     *   <li>HTTP 404 (Not Found): Account not found errors</li>
     *   <li>HTTP 500 (Internal Server Error): System errors, database failures</li>
     * </ul>
     * 
     * <p><strong>Performance Requirements:</strong></p>
     * <ul>
     *   <li>Response time: < 200ms at 95th percentile</li>
     *   <li>Throughput: Support 10,000+ TPS</li>
     *   <li>Memory: Within 10% of CICS baseline</li>
     *   <li>Database: ACID compliance with optimistic locking</li>
     * </ul>
     * 
     * @param cardId the card identifier from the URL path for account cross-reference validation
     * @param request the bill payment request containing account ID, payment amount, and confirmation
     * @return ResponseEntity containing BillPaymentResponseDto with payment results and audit information
     * @throws IllegalArgumentException when request validation fails with business rule violations
     * @throws SecurityException when authentication or authorization fails
     * @throws RuntimeException when system errors occur during payment processing
     */
    @PostMapping
    @PreAuthorize("hasRole('CARD_USER') or hasRole('ADMIN')")
    public ResponseEntity<BillPaymentResponseDto> processBillPayment(
            @PathVariable("cardId") String cardId,
            @Valid @RequestBody BillPaymentRequestDto request) {
        
        // Generate correlation ID for this payment request if not provided
        String correlationId = request.getCorrelationId() != null ? 
            request.getCorrelationId() : UUID.randomUUID().toString();
        
        logger.info("Processing bill payment request - CorrelationId: {}, AccountId: {}, " +
                   "PaymentAmount: {}, Confirmed: {}", 
                   correlationId, request.getAccountId(), 
                   request.getFormattedPaymentAmount(), request.isConfirmed());
        
        try {
            // Step 1: Validate the payment request with comprehensive business rules
            ValidationResult validationResult = validatePayment(request);
            if (!validationResult.isValid()) {
                logger.warn("Bill payment validation failed - CorrelationId: {}, Errors: {}", 
                           correlationId, validationResult.getErrorSummary());
                return handleValidationErrors(validationResult, correlationId);
            }
            
            // Step 2: Set card ID from path parameter for cross-reference validation
            request.setCardNumber(cardId);
            
            // Step 3: Process payment through business service with transaction management
            BillPaymentResponseDto response = billPaymentService.processBillPayment(request);
            
            // Step 4: Determine appropriate HTTP status based on payment outcome
            HttpStatus httpStatus = determineHttpStatus(response);
            
            // Step 5: Log successful payment processing
            if (response.isPaymentSuccessful()) {
                logger.info("Bill payment processed successfully - CorrelationId: {}, " +
                           "TransactionId: {}, Amount: {}", 
                           correlationId, response.getPaymentConfirmationNumber(), 
                           response.getPaymentAmount());
            } else {
                logger.info("Bill payment requires confirmation - CorrelationId: {}, " +
                           "AccountId: {}, Amount: {}", 
                           correlationId, request.getAccountId(), 
                           response.getPaymentAmount());
            }
            
            return new ResponseEntity<>(response, httpStatus);
            
        } catch (BillPaymentService.AccountNotFoundException e) {
            logger.warn("Account not found during bill payment - CorrelationId: {}, AccountId: {}, Error: {}", 
                       correlationId, request.getAccountId(), e.getMessage());
            return handlePaymentError(e, HttpStatus.BAD_REQUEST, correlationId, request);
            
        } catch (BillPaymentService.InsufficientBalanceException e) {
            logger.warn("Insufficient balance for bill payment - CorrelationId: {}, AccountId: {}, Error: {}", 
                       correlationId, request.getAccountId(), e.getMessage());
            return handlePaymentError(e, HttpStatus.BAD_REQUEST, correlationId, request);
            
        } catch (BillPaymentService.PaymentProcessingException e) {
            logger.error("Payment processing failed - CorrelationId: {}, AccountId: {}, Error: {}", 
                        correlationId, request.getAccountId(), e.getMessage(), e);
            return handlePaymentError(e, HttpStatus.INTERNAL_SERVER_ERROR, correlationId, request);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid payment request - CorrelationId: {}, Error: {}", 
                       correlationId, e.getMessage());
            return handlePaymentError(e, HttpStatus.BAD_REQUEST, correlationId, request);
            
        } catch (Exception e) {
            logger.error("Unexpected error during bill payment processing - CorrelationId: {}, Error: {}", 
                        correlationId, e.getMessage(), e);
            return handlePaymentError(e, HttpStatus.INTERNAL_SERVER_ERROR, correlationId, request);
        }
    }

    /**
     * Validates bill payment request with comprehensive business rule checking.
     * 
     * <p>This method implements validation logic equivalent to the COBOL validation
     * routines in COBIL00C.cbl, ensuring all required fields are present and properly
     * formatted before payment processing can proceed.</p>
     * 
     * <p><strong>Validation Rules Implemented:</strong></p>
     * <ul>
     *   <li>Account ID validation: Must be exactly 11 digits, cannot be empty (COBOL lines 159-167)</li>
     *   <li>Payment amount validation: Must be positive, proper BigDecimal precision</li>
     *   <li>Confirmation flag validation: Must be 'Y', 'y', 'N', 'n', or empty (COBOL lines 173-191)</li>
     *   <li>Card number validation: Must be exactly 16 digits if provided</li>
     *   <li>Request metadata validation: Correlation ID, timestamps, audit information</li>
     * </ul>
     * 
     * <p><strong>Error Message Compatibility:</strong></p>
     * <p>All error messages maintain exact compatibility with the original COBOL program
     * to ensure consistent user experience across the modernized system:</p>
     * <ul>
     *   <li>"Acct ID can NOT be empty..." - matches COBOL line 161</li>
     *   <li>"Invalid value. Valid values are (Y/N)..." - matches COBOL lines 187-188</li>
     *   <li>Custom validation messages for business rule compliance</li>
     * </ul>
     * 
     * <p><strong>Performance Characteristics:</strong></p>
     * <ul>
     *   <li>Early validation returns to minimize processing overhead</li>
     *   <li>Efficient field-level validation with minimal object allocation</li>
     *   <li>Comprehensive error collection for single-pass validation</li>
     *   <li>Thread-safe validation logic for concurrent request processing</li>
     * </ul>
     * 
     * @param request the bill payment request to validate
     * @return ValidationResult containing validation status and detailed error information
     */
    public ValidationResult validatePayment(BillPaymentRequestDto request) {
        logger.debug("Validating bill payment request - AccountId: {}, Amount: {}, Confirmed: {}", 
                    request.getAccountId(), request.getFormattedPaymentAmount(), request.isConfirmed());
        
        ValidationResult result = new ValidationResult();
        result.setValidationContext("BillPaymentController.validatePayment");
        
        // Validate account ID is not empty (from COBOL line 159-167)
        if (request.getAccountId() == null || request.getAccountId().trim().isEmpty()) {
            result.addErrorMessage("accountId", "ACCOUNT_ID_EMPTY", 
                                 "Acct ID can NOT be empty...", 
                                 ValidationResult.Severity.ERROR);
        } else if (!request.getAccountId().matches("^[0-9]{11}$")) {
            // Validate account ID format (exactly 11 digits)
            result.addErrorMessage("accountId", "ACCOUNT_ID_FORMAT", 
                                 "Account ID must be exactly 11 numeric digits", 
                                 ValidationResult.Severity.ERROR);
        }
        
        // Validate payment amount if provided
        if (request.getPaymentAmount() != null) {
            if (request.getPaymentAmount().compareTo(BigDecimal.ZERO) <= 0) {
                result.addErrorMessage("paymentAmount", "PAYMENT_AMOUNT_INVALID", 
                                     "Payment amount must be greater than zero", 
                                     ValidationResult.Severity.ERROR);
            }
            
            // Validate decimal precision (must be exactly 2 decimal places)
            if (request.getPaymentAmountScale() > 2) {
                result.addErrorMessage("paymentAmount", "PAYMENT_AMOUNT_PRECISION", 
                                     "Payment amount must have exactly 2 decimal places", 
                                     ValidationResult.Severity.ERROR);
            }
        }
        
        // Validate confirmation flag if provided (from COBOL lines 173-191)
        String confirmationFlag = request.getConfirmationFlag();
        if (confirmationFlag != null && !confirmationFlag.trim().isEmpty()) {
            if (!confirmationFlag.matches("^[YyNn]$")) {
                result.addErrorMessage("confirmationFlag", "CONFIRMATION_FLAG_INVALID", 
                                     "Invalid value. Valid values are (Y/N)...", 
                                     ValidationResult.Severity.ERROR);
            }
        }
        
        // Validate card number format if provided
        if (request.getCardNumber() != null && !request.getCardNumber().trim().isEmpty()) {
            if (!request.getCardNumber().matches("^[0-9]{16}$")) {
                result.addErrorMessage("cardNumber", "CARD_NUMBER_FORMAT", 
                                     "Card number must be exactly 16 digits", 
                                     ValidationResult.Severity.ERROR);
            }
        }
        
        // Validate base request metadata
        if (!request.isValid()) {
            result.addErrorMessage("request", "REQUEST_METADATA_INVALID", 
                                 "Request metadata validation failed", 
                                 ValidationResult.Severity.ERROR);
        }
        
        logger.debug("Payment validation completed - Valid: {}, ErrorCount: {}", 
                    result.isValid(), result.getErrorCount());
        
        return result;
    }

    /**
     * Handles payment processing errors with comprehensive error response generation.
     * 
     * <p>This method creates standardized error responses for various payment processing
     * failures while maintaining compatibility with the original COBOL error handling
     * patterns from COBIL00C.cbl. It ensures consistent error reporting across all
     * payment processing scenarios.</p>
     * 
     * <p><strong>Error Response Features:</strong></p>
     * <ul>
     *   <li>Consistent error message formatting matching COBOL WS-MESSAGE patterns</li>
     *   <li>Appropriate HTTP status codes for different error categories</li>
     *   <li>Comprehensive audit information for error tracking and compliance</li>
     *   <li>Correlation ID preservation for distributed tracing</li>
     *   <li>Security-aware error messages (no sensitive data exposure)</li>
     * </ul>
     * 
     * <p><strong>Error Categories Handled:</strong></p>
     * <ul>
     *   <li>Validation Errors (400): Business rule violations, format errors</li>
     *   <li>Authentication Errors (401): JWT token issues, expired sessions</li>
     *   <li>Authorization Errors (403): Insufficient permissions</li>
     *   <li>Not Found Errors (404): Account or card not found</li>
     *   <li>System Errors (500): Database failures, service unavailability</li>
     * </ul>
     * 
     * <p><strong>Audit Integration:</strong></p>
     * <p>All errors are logged with appropriate severity levels and include comprehensive
     * context information for debugging and compliance auditing. Error responses include
     * audit information for regulatory tracking requirements.</p>
     * 
     * @param exception the exception that occurred during payment processing
     * @param httpStatus the appropriate HTTP status code for the error category
     * @param correlationId the request correlation identifier for tracing
     * @param request the original payment request for context information
     * @return ResponseEntity containing BillPaymentResponseDto with error details
     */
    public ResponseEntity<BillPaymentResponseDto> handlePaymentError(
            Exception exception, HttpStatus httpStatus, String correlationId, BillPaymentRequestDto request) {
        
        logger.debug("Handling payment error - Type: {}, Status: {}, CorrelationId: {}, Message: {}", 
                    exception.getClass().getSimpleName(), httpStatus, correlationId, exception.getMessage());
        
        // Determine appropriate processing status based on HTTP status and exception type
        String processingStatus = determineProcessingStatus(httpStatus, exception);
        
        // Create error message maintaining COBOL compatibility
        String errorMessage = formatErrorMessage(exception, request);
        
        // Create error response with comprehensive audit information
        BillPaymentResponseDto errorResponse = new BillPaymentResponseDto(
            errorMessage, processingStatus, correlationId);
        
        // Set additional error context information
        errorResponse.setPaymentSuccessful(false);
        errorResponse.setPaymentAmount(BigDecimal.ZERO.setScale(2));
        
        // Create audit info for error tracking
        com.carddemo.common.dto.AuditInfo auditInfo = new com.carddemo.common.dto.AuditInfo();
        auditInfo.setUserId(request != null ? request.getAccountId() : "UNKNOWN");
        auditInfo.setOperationType("BILL_PAYMENT_ERROR");
        auditInfo.setCorrelationId(correlationId);
        auditInfo.setSourceSystem("BillPaymentController");
        errorResponse.setAuditInfo(auditInfo);
        
        logger.debug("Payment error response created - Status: {}, Message: {}", 
                    processingStatus, errorMessage);
        
        return new ResponseEntity<>(errorResponse, httpStatus);
    }

    /**
     * Handles validation errors by converting ValidationResult to appropriate HTTP response.
     * 
     * <p>This method processes field-level validation errors and creates comprehensive
     * error responses that maintain compatibility with the original COBOL field validation
     * patterns while providing modern REST API error reporting.</p>
     * 
     * @param validationResult the validation result containing field-level errors
     * @param correlationId the request correlation identifier for tracing
     * @return ResponseEntity containing validation error details
     */
    private ResponseEntity<BillPaymentResponseDto> handleValidationErrors(
            ValidationResult validationResult, String correlationId) {
        
        logger.debug("Handling validation errors - ErrorCount: {}, CorrelationId: {}", 
                    validationResult.getErrorCount(), correlationId);
        
        // Create consolidated error message from validation errors
        StringBuilder errorMessage = new StringBuilder();
        if (validationResult.hasErrorsOfSeverity(ValidationResult.Severity.ERROR)) {
            // Get the first error message for primary display
            ValidationResult.ValidationError firstError = validationResult.getErrorMessages().get(0);
            errorMessage.append(firstError.getErrorMessage());
            
            // Add additional errors if multiple exist
            if (validationResult.getErrorCount() > 1) {
                errorMessage.append(" (").append(validationResult.getErrorCount() - 1)
                           .append(" additional validation errors)");
            }
        } else {
            errorMessage.append("Request validation failed");
        }
        
        // Create validation error response
        BillPaymentResponseDto errorResponse = new BillPaymentResponseDto(
            errorMessage.toString(), "VALIDATION_FAILED", correlationId);
        
        // Set validation-specific error information
        errorResponse.setPaymentSuccessful(false);
        errorResponse.setPaymentAmount(BigDecimal.ZERO.setScale(2));
        
        // Create audit info for validation error tracking
        com.carddemo.common.dto.AuditInfo auditInfo = new com.carddemo.common.dto.AuditInfo();
        auditInfo.setUserId("UNKNOWN");
        auditInfo.setOperationType("BILL_PAYMENT_VALIDATION_ERROR");
        auditInfo.setCorrelationId(correlationId);
        auditInfo.setSourceSystem("BillPaymentController");
        errorResponse.setAuditInfo(auditInfo);
        
        logger.debug("Validation error response created - Message: {}", errorMessage.toString());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Determines the appropriate HTTP status code based on payment response outcome.
     * 
     * @param response the bill payment response to analyze
     * @return appropriate HttpStatus for the response
     */
    private HttpStatus determineHttpStatus(BillPaymentResponseDto response) {
        if (response.isPaymentSuccessful()) {
            return HttpStatus.OK;
        } else if ("COMPLETED".equals(response.getProcessingStatus())) {
            return HttpStatus.OK;
        } else if (response.getProcessingStatus().contains("CONFIRMATION")) {
            return HttpStatus.OK; // Confirmation required is a normal response
        } else if (response.getProcessingStatus().contains("VALIDATION")) {
            return HttpStatus.BAD_REQUEST;
        } else {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    /**
     * Determines processing status based on HTTP status and exception type.
     * 
     * @param httpStatus the HTTP status code
     * @param exception the exception that occurred
     * @return appropriate processing status string
     */
    private String determineProcessingStatus(HttpStatus httpStatus, Exception exception) {
        if (httpStatus == HttpStatus.BAD_REQUEST) {
            if (exception instanceof BillPaymentService.AccountNotFoundException) {
                return "ACCOUNT_NOT_FOUND";
            } else if (exception instanceof BillPaymentService.InsufficientBalanceException) {
                return "INSUFFICIENT_BALANCE";
            } else if (exception instanceof IllegalArgumentException) {
                return "VALIDATION_FAILED";
            } else {
                return "BAD_REQUEST";
            }
        } else if (httpStatus == HttpStatus.INTERNAL_SERVER_ERROR) {
            if (exception instanceof BillPaymentService.PaymentProcessingException) {
                return "PAYMENT_PROCESSING_ERROR";
            } else {
                return "SYSTEM_ERROR";
            }
        } else {
            return "ERROR";
        }
    }

    /**
     * Formats error messages to maintain COBOL compatibility while providing clear user feedback.
     * 
     * @param exception the exception that occurred
     * @param request the original payment request for context
     * @return formatted error message
     */
    private String formatErrorMessage(Exception exception, BillPaymentRequestDto request) {
        String message = exception.getMessage();
        
        // Ensure COBOL-compatible error messages are preserved
        if (message != null) {
            // Direct COBOL message compatibility
            if (message.contains("Acct ID can NOT be empty") ||
                message.contains("Account ID NOT found") ||
                message.contains("You have nothing to pay") ||
                message.contains("Invalid value. Valid values are (Y/N)")) {
                return message;
            }
        }
        
        // Default error message for unexpected errors
        return message != null ? message : "An error occurred during bill payment processing";
    }

    /**
     * Exception handler for method argument validation errors.
     * 
     * @param ex the MethodArgumentNotValidException
     * @return ResponseEntity with validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BillPaymentResponseDto> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        String correlationId = UUID.randomUUID().toString();
        logger.warn("Method argument validation failed - CorrelationId: {}", correlationId);
        
        // Extract validation errors from binding result
        ValidationResult validationResult = new ValidationResult(false);
        BindingResult bindingResult = ex.getBindingResult();
        
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            validationResult.addErrorMessage(
                fieldError.getField(),
                "VALIDATION_ERROR",
                fieldError.getDefaultMessage(),
                ValidationResult.Severity.ERROR
            );
        }
        
        return handleValidationErrors(validationResult, correlationId);
    }
}