/*
 * Copyright 2024 CardDemo Application - Blitzy Platform
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.carddemo.account.controller;

import com.carddemo.account.AccountViewService;
import com.carddemo.account.dto.AccountViewRequestDto;
import com.carddemo.account.dto.AccountViewResponseDto;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * AccountViewController - Spring Boot REST controller converting CICS transaction CAVW to RESTful API.
 * 
 * This controller serves as the primary entry point for account view operations, replacing the 
 * legacy COBOL program COACTVWC.cbl with modern REST API endpoints while maintaining complete
 * functional equivalence and business logic preservation.
 * 
 * COBOL Program Transformation:
 * - Original Program: COACTVWC.cbl (Account View Transaction Processing)
 * - Original Transaction ID: CAVW (Card Account View)
 * - Original BMS Map: COACTVW.bms (Account View Screen Definition)
 * - Original Copybooks: CVACT01Y, CVCUS01Y, COCOM01Y (Data Structures)
 * 
 * REST API Mapping:
 * - CICS Transaction Entry: EXEC CICS XCTL PROGRAM('COACTVWC') → GET /api/cavw/accounts/{id}
 * - COMMAREA Input: WS-CARD-RID-ACCT-ID (PIC 9(11)) → Path parameter {id}
 * - BMS Screen Output: CACTVWAO structure → AccountViewResponseDto JSON
 * - Error Handling: WS-RETURN-MSG → HTTP status codes with error DTOs
 * 
 * Key Features:
 * - JWT authentication validation equivalent to RACF security group authorization
 * - 11-digit account ID validation matching COBOL PIC 9(11) business rules
 * - HTTP status code mapping preserving CICS response code semantics
 * - OpenAPI documentation maintaining transaction code CAVW for traceability
 * - Comprehensive error handling with structured JSON error responses
 * - Service layer delegation pattern for separation of concerns
 * 
 * Business Logic Preservation:
 * - Account ID validation: Exactly 11 digits, non-zero, numeric (COBOL 2210-EDIT-ACCOUNT)
 * - Cross-reference validation: Account-card-customer relationships (COBOL 9200-GETCARDXREF-BYACCT)
 * - Data retrieval patterns: Account master lookup (COBOL 9300-GETACCTDATA-BYACCT)
 * - Customer data integration: Customer master lookup (COBOL 9400-GETCUSTDATA-BYCUST)
 * - Error message mapping: Identical error messages from COBOL WS-RETURN-MSG variables
 * 
 * Performance Requirements:
 * - Response times under 200ms at 95th percentile (Section 0.1.2)
 * - Support for 10,000 TPS transaction volume
 * - Memory usage within 110% of CICS baseline
 * - Horizontal scaling via Kubernetes pod replication
 * 
 * Security Integration:
 * - Spring Security method-level authorization (@PreAuthorize)
 * - JWT token validation replacing RACF authentication
 * - Role-based access control equivalent to CICS transaction security
 * - Audit logging for regulatory compliance
 * 
 * @author CardDemo Migration Team - Blitzy Platform
 * @version 1.0
 * @since Java 21
 */
@RestController
@RequestMapping("/api/cavw")
@Tag(name = "Account View Operations", 
     description = "REST API endpoints for account viewing operations replacing CICS transaction CAVW from COACTVWC.cbl")
@SecurityRequirement(name = "bearerAuth")
public class AccountViewController {

    private static final Logger logger = LoggerFactory.getLogger(AccountViewController.class);

    /**
     * Account view service for business logic delegation.
     * Injected via Spring's dependency injection framework to maintain separation of concerns
     * between HTTP request/response handling and core business logic processing.
     */
    @Autowired
    private AccountViewService accountViewService;

    /**
     * Primary account view endpoint replacing CICS transaction CAVW.
     * 
     * This endpoint implements the complete account view functionality from COACTVWC.cbl,
     * including input validation, business logic execution, and response formatting.
     * 
     * Original COBOL Flow Mapping:
     * 1. Input validation (2210-EDIT-ACCOUNT) → Path variable and request validation
     * 2. Cross-reference lookup (9200-GETCARDXREF-BYACCT) → Service layer delegation
     * 3. Account data retrieval (9300-GETACCTDATA-BYACCT) → Service layer delegation
     * 4. Customer data retrieval (9400-GETCUSTDATA-BYCUST) → Service layer delegation
     * 5. Screen population (1200-SETUP-SCREEN-VARS) → Response DTO construction
     * 6. Error handling (WS-RETURN-MSG) → HTTP status codes and error DTOs
     * 
     * URL Design:
     * - Path preserves original transaction code CAVW for backwards traceability
     * - RESTful resource identification: /api/cavw/accounts/{id}
     * - Supports pagination parameters for future enhancements
     * 
     * Authentication & Authorization:
     * - Requires valid JWT token (replacing RACF authentication)
     * - Role-based access control for account view operations
     * - Supports both user and admin roles with appropriate data filtering
     * 
     * @param accountId The 11-digit account identifier (PIC 9(11) equivalent)
     * @param pageNumber Optional pagination page number (default: 0)
     * @param pageSize Optional pagination page size (default: 20)
     * @return ResponseEntity with AccountViewResponseDto or error response
     */
    @GetMapping("/accounts/{accountId}")
    @Operation(
        summary = "View account details (CAVW transaction equivalent)",
        description = "Retrieves comprehensive account information including customer data and card cross-references. " +
                     "This endpoint replaces the legacy CICS transaction CAVW from program COACTVWC.cbl with " +
                     "identical business logic and validation rules preserved.",
        operationId = "viewAccountDetails"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Account details retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AccountViewResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid account ID format or validation error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required - missing or invalid JWT token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - insufficient permissions for account view operations",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Account not found in master file or cross-reference file",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - system malfunction or database connectivity issue",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDto.class)
            )
        )
    })
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<AccountViewResponseDto> viewAccount(
            @Parameter(
                description = "Eleven-digit account identifier (must be exactly 11 numeric digits, non-zero)",
                required = true,
                example = "00001000001",
                schema = @Schema(
                    type = "string",
                    pattern = "^[0-9]{11}$",
                    minLength = 11,
                    maxLength = 11
                )
            )
            @PathVariable String accountId,
            
            @Parameter(
                description = "Page number for pagination (0-based indexing)",
                required = false,
                example = "0",
                schema = @Schema(type = "integer", minimum = "0", defaultValue = "0")
            )
            @RequestParam(value = "page", defaultValue = "0") Integer pageNumber,
            
            @Parameter(
                description = "Page size for pagination (maximum 100 records)",
                required = false,
                example = "20",
                schema = @Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "20")
            )
            @RequestParam(value = "size", defaultValue = "20") Integer pageSize) {

        logger.info("Account view request received for account ID: {} (transaction CAVW equivalent)", 
                   maskAccountId(accountId));
        logger.debug("Request parameters - accountId: {}, pageNumber: {}, pageSize: {}", 
                    maskAccountId(accountId), pageNumber, pageSize);

        try {
            // Step 1: Create request DTO with validation (equivalent to COBOL input validation)
            AccountViewRequestDto request = new AccountViewRequestDto(accountId, pageNumber, pageSize, null);
            
            // Step 2: Validate request DTO (equivalent to COBOL 2210-EDIT-ACCOUNT paragraph)
            if (!request.validate()) {
                logger.warn("Account view request validation failed for account ID: {}", maskAccountId(accountId));
                return ResponseEntity.badRequest()
                    .body(buildErrorResponse("Account number must be a non zero 11 digit number", 
                                           HttpStatus.BAD_REQUEST));
            }
            
            // Step 3: Delegate to service layer for business logic execution
            // This replaces the complete COBOL business logic flow from COACTVWC.cbl
            AccountViewResponseDto response = accountViewService.viewAccount(request);
            
            // Step 4: Handle service response and map to appropriate HTTP status
            if (response.isSuccess()) {
                logger.info("Account view completed successfully for account ID: {}", maskAccountId(accountId));
                logger.debug("Account view response - success: {}, customerData present: {}, balance: {}", 
                           response.isSuccess(), 
                           response.getCustomerData() != null,
                           response.getCurrentBalance());
                
                return ResponseEntity.ok(response);
            } else {
                // Handle business logic errors (equivalent to WS-RETURN-MSG error handling)
                String errorMessage = response.getErrorMessage();
                logger.warn("Account view failed for account ID: {} - Error: {}", 
                          maskAccountId(accountId), errorMessage);
                
                // Map business errors to appropriate HTTP status codes
                if (errorMessage != null) {
                    if (errorMessage.contains("not found") || errorMessage.contains("Not found")) {
                        return ResponseEntity.notFound().build();
                    } else if (errorMessage.contains("validation") || errorMessage.contains("invalid")) {
                        return ResponseEntity.badRequest()
                            .body(buildErrorResponse(errorMessage, HttpStatus.BAD_REQUEST));
                    }
                }
                
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse(errorMessage != null ? errorMessage : 
                                           "System error occurred while retrieving account information", 
                                           HttpStatus.INTERNAL_SERVER_ERROR));
            }
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid account ID format provided: {} - Error: {}", maskAccountId(accountId), e.getMessage());
            return ResponseEntity.badRequest()
                .body(buildErrorResponse("Account number must be a non zero 11 digit number", 
                                       HttpStatus.BAD_REQUEST));
                                       
        } catch (Exception e) {
            logger.error("Unexpected error during account view operation for account ID: {}", 
                        maskAccountId(accountId), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("System error occurred while retrieving account information", 
                                       HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * Exception handler for validation constraint violations.
     * 
     * This method handles Jakarta Bean Validation errors that occur during request processing,
     * providing consistent error responses for validation failures equivalent to COBOL
     * input validation error handling from the 2210-EDIT-ACCOUNT paragraph.
     * 
     * @param ex The ConstraintViolationException containing validation error details
     * @return ResponseEntity with structured error response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<AccountViewResponseDto> handleValidationException(ConstraintViolationException ex) {
        logger.warn("Validation constraint violation in account view operation: {}", ex.getMessage());
        
        // Extract first constraint violation message for user-friendly error response
        String errorMessage = ex.getConstraintViolations().isEmpty() ? 
            "Account number must be a non zero 11 digit number" :
            ex.getConstraintViolations().iterator().next().getMessage();
            
        logger.debug("Detailed validation errors: {}", ex.getConstraintViolations());
        
        return ResponseEntity.badRequest()
            .body(buildErrorResponse(errorMessage, HttpStatus.BAD_REQUEST));
    }

    /**
     * General exception handler for unexpected system errors.
     * 
     * This method provides a safety net for any unexpected exceptions that occur during
     * account view operations, ensuring consistent error responses and proper logging
     * for system monitoring and debugging purposes.
     * 
     * Equivalent to COBOL ABEND-ROUTINE handling in COACTVWC.cbl for system errors
     * that cannot be handled through normal business logic flow.
     * 
     * @param ex The unexpected exception that occurred
     * @return ResponseEntity with generic error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AccountViewResponseDto> handleGeneralException(Exception ex) {
        logger.error("Unexpected system error in account view controller", ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(buildErrorResponse("System error occurred while retrieving account information", 
                                   HttpStatus.INTERNAL_SERVER_ERROR));
    }

    /**
     * Builds standardized error response DTO for consistent error handling.
     * 
     * This method creates AccountViewResponseDto objects configured for error responses,
     * maintaining consistency with the response structure while providing clear error
     * information for client applications.
     * 
     * Equivalent to COBOL error message construction in WS-RETURN-MSG variables and
     * CCARD-ERROR-MSG population for BMS screen display.
     * 
     * @param errorMessage Descriptive error message for the client
     * @param httpStatus HTTP status code for the error response
     * @return AccountViewResponseDto configured as error response
     */
    private AccountViewResponseDto buildErrorResponse(String errorMessage, HttpStatus httpStatus) {
        logger.debug("Building error response - message: {}, status: {}", errorMessage, httpStatus);
        
        AccountViewResponseDto errorResponse = new AccountViewResponseDto();
        errorResponse.setSuccess(false);
        errorResponse.setErrorMessage(errorMessage);
        
        // Set appropriate default values to prevent null pointer exceptions
        errorResponse.setAccountId("");
        
        return errorResponse;
    }

    /**
     * Masks account ID for secure logging and audit trail purposes.
     * 
     * This method provides data protection for sensitive account information in logs,
     * showing only first 2 and last 2 digits while masking the middle portion.
     * 
     * @param accountId The account ID to mask
     * @return Masked account ID string for secure logging
     */
    private String maskAccountId(String accountId) {
        if (accountId == null || accountId.length() < 4) {
            return "***";
        }
        return accountId.substring(0, 2) + "*******" + accountId.substring(accountId.length() - 2);
    }

    /**
     * Inner class for structured error response DTOs.
     * 
     * This class provides consistent error response structure for API clients,
     * maintaining compatibility with AccountViewResponseDto while focusing on
     * error-specific information.
     */
    public static class ErrorResponseDto {
        private boolean success = false;
        private String errorMessage;
        private String errorCode;
        private long timestamp = System.currentTimeMillis();

        public ErrorResponseDto() {}

        public ErrorResponseDto(String errorMessage, String errorCode) {
            this.errorMessage = errorMessage;
            this.errorCode = errorCode;
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}