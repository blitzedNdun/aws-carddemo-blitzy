/*
 * Copyright (c) 2024 CardDemo Application
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

package com.carddemo.transaction;

import com.carddemo.transaction.TransactionService;
import com.carddemo.transaction.TransactionViewService;
import com.carddemo.transaction.AddTransactionService;
import com.carddemo.transaction.TransactionListRequest;
import com.carddemo.transaction.TransactionListResponse;
import com.carddemo.transaction.TransactionViewResponse;
import com.carddemo.transaction.AddTransactionRequest;
import com.carddemo.transaction.AddTransactionResponse;
import com.carddemo.common.dto.BaseResponseDto;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * TransactionController - Spring Boot REST API controller providing comprehensive HTTP/JSON endpoints
 * for all transaction operations with enterprise-grade security, validation, and error handling capabilities.
 * 
 * This controller converts the original COBOL CICS transaction programs (COTRN00C, COTRN01C, COTRN02C)
 * to modern REST API endpoints while preserving complete functional equivalence and business logic.
 * Implements stateless API design with JWT authentication and comprehensive OpenAPI documentation
 * that maintains backwards traceability to original CICS transaction codes.
 * 
 * <h3>COBOL Program Equivalence:</h3>
 * <ul>
 *   <li><b>COTRN00C.cbl (CT00)</b> → GET /api/ct00/transactions - Transaction listing with pagination</li>
 *   <li><b>COTRN01C.cbl (CT01)</b> → GET /api/ct01/transactions/{id} - Transaction detail viewing</li>
 *   <li><b>COTRN02C.cbl (CT02)</b> → POST /api/ct02/transactions - Transaction addition</li>
 * </ul>
 * 
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>RESTful HTTP endpoints replacing CICS transaction entry points with identical business logic</li>
 *   <li>JWT authentication and role-based authorization using Spring Security 6.x</li>
 *   <li>Stateless API design with Redis-backed session management for pseudo-conversational behavior</li>
 *   <li>Comprehensive request validation using Jakarta Bean Validation with custom business rule validators</li>
 *   <li>OpenAPI documentation preserving original transaction semantics with CICS transaction code traceability</li>
 *   <li>Structured error handling with HTTP status codes equivalent to COMMAREA status fields</li>
 *   <li>Spring Cloud Gateway integration with transaction code preservation in URL paths</li>
 * </ul>
 * 
 * <h3>API Endpoint Patterns:</h3>
 * <ul>
 *   <li><b>/api/ct00/transactions</b> - Transaction listing endpoint (COTRN00C equivalent)</li>
 *   <li><b>/api/ct01/transactions/{id}</b> - Transaction viewing endpoint (COTRN01C equivalent)</li>
 *   <li><b>/api/ct02/transactions</b> - Transaction addition endpoint (COTRN02C equivalent)</li>
 * </ul>
 * 
 * <h3>Security Implementation:</h3>
 * <ul>
 *   <li>JWT token validation for all endpoints with automatic security context propagation</li>
 *   <li>Role-based access control using @PreAuthorize annotations (USER, ADMIN, TELLER roles)</li>
 *   <li>Cross-service authorization validation for transaction ownership and access rights</li>
 *   <li>Comprehensive audit logging for all transaction operations with correlation ID tracking</li>
 * </ul>
 * 
 * <h3>Performance Requirements:</h3>
 * <ul>
 *   <li>Sub-200ms response times at 95th percentile matching original CICS performance</li>
 *   <li>Support for 10,000+ TPS throughput with horizontal scaling capabilities</li>
 *   <li>Memory usage within 110% of original CICS baseline allocation</li>
 *   <li>Circuit breaker integration for resilience and fault tolerance</li>
 * </ul>
 * 
 * @author Blitzy Agent - CardDemo Migration Team
 * @version 1.0
 * @since Java 21
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Transaction Management", description = "Complete transaction processing API with CICS transaction equivalence (CT00, CT01, CT02)")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('TELLER')")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    // Service dependencies for transaction operations
    private final TransactionService transactionService;
    private final TransactionViewService transactionViewService;
    private final AddTransactionService addTransactionService;

    /**
     * Constructor-based dependency injection for transaction services.
     * Uses Spring @Autowired annotation for automatic dependency resolution.
     * 
     * @param transactionService Core transaction processing service for CT00 operations
     * @param transactionViewService Transaction detail viewing service for CT01 operations
     * @param addTransactionService Transaction addition service for CT02 operations
     */
    @Autowired
    public TransactionController(TransactionService transactionService,
                                TransactionViewService transactionViewService,
                                AddTransactionService addTransactionService) {
        this.transactionService = transactionService;
        this.transactionViewService = transactionViewService;
        this.addTransactionService = addTransactionService;
        
        logger.info("TransactionController initialized with all transaction service dependencies");
    }

    /**
     * GET /api/ct00/transactions - Retrieves paginated list of transactions with comprehensive filtering.
     * 
     * This endpoint provides the primary transaction listing functionality equivalent to the COBOL
     * COTRN00C.cbl program, supporting pagination, filtering, and sorting capabilities that preserve
     * the original CICS pseudo-conversational processing patterns while providing modern REST API features.
     * 
     * <p><b>COBOL Program Equivalence (COTRN00C.cbl):</b></p>
     * <ul>
     *   <li>MAIN-PARA → HTTP GET request processing with parameter validation</li>
     *   <li>PROCESS-ENTER-KEY → Request parameter processing and validation logic</li>
     *   <li>PROCESS-PAGE-FORWARD/BACKWARD → Pagination control via page number parameters</li>
     *   <li>POPULATE-TRAN-DATA → Response DTO population with transaction details</li>
     *   <li>STARTBR/READNEXT/ENDBR → JPA repository queries with Spring Data pagination</li>
     * </ul>
     * 
     * <p><b>Request Parameters:</b></p>
     * <ul>
     *   <li>pageNumber - Zero-based page index for pagination (default: 0)</li>
     *   <li>pageSize - Number of records per page (default: 10, max: 100)</li>
     *   <li>sortBy - Field name for result ordering (default: "transactionDate")</li>
     *   <li>sortDirection - Sort direction ASC/DESC (default: "DESC")</li>
     *   <li>transactionId - Filter by specific transaction ID (16-character numeric)</li>
     *   <li>cardNumber - Filter by card number (16-digit with Luhn validation)</li>
     *   <li>accountId - Filter by account ID (11-digit numeric)</li>
     *   <li>fromDate - Start date for date range filter (CCYYMMDD format)</li>
     *   <li>toDate - End date for date range filter (CCYYMMDD format)</li>
     * </ul>
     * 
     * @param pageNumber Zero-based page number for pagination (default: 0)
     * @param pageSize Number of records per page (default: 10, max: 100) 
     * @param sortBy Field name for sorting (default: "transactionDate")
     * @param sortDirection Sort direction ASC/DESC (default: "DESC")
     * @param transactionId Optional transaction ID filter (16-character numeric)
     * @param cardNumber Optional card number filter (16-digit with validation)
     * @param accountId Optional account ID filter (11-digit numeric)
     * @param fromDate Optional start date for range filter (CCYYMMDD format)
     * @param toDate Optional end date for range filter (CCYYMMDD format)
     * @param authentication Spring Security authentication context for user validation
     * @return ResponseEntity containing TransactionListResponse with paginated transaction data
     */
    @GetMapping("/ct00/transactions")
    @Operation(
        summary = "List transactions with pagination and filtering",
        description = "Retrieves paginated list of transactions equivalent to COBOL COTRN00C.cbl program (CT00 transaction). " +
                     "Supports comprehensive filtering, sorting, and pagination with identical business logic to original CICS implementation.",
        tags = {"Transaction Listing (CT00)"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved transaction list with pagination metadata",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionListResponse.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid request parameters or validation failure",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required - invalid or missing JWT token",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - insufficient permissions for transaction listing",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during transaction processing",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponseDto.class))
        )
    })
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('TELLER')")
    public ResponseEntity<TransactionListResponse> getTransactions(
            @Parameter(description = "Zero-based page number for pagination", example = "0")
            @RequestParam(value = "pageNumber", defaultValue = "0") Integer pageNumber,
            
            @Parameter(description = "Number of records per page (max 100)", example = "10")
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            
            @Parameter(description = "Field name for result sorting", example = "transactionDate")
            @RequestParam(value = "sortBy", defaultValue = "transactionDate") String sortBy,
            
            @Parameter(description = "Sort direction (ASC or DESC)", example = "DESC")
            @RequestParam(value = "sortDirection", defaultValue = "DESC") String sortDirection,
            
            @Parameter(description = "Filter by transaction ID (16-character numeric)", example = "1234567890123456")
            @RequestParam(value = "transactionId", required = false) String transactionId,
            
            @Parameter(description = "Filter by card number (16-digit)", example = "4111111111111111")
            @RequestParam(value = "cardNumber", required = false) String cardNumber,
            
            @Parameter(description = "Filter by account ID (11-digit numeric)", example = "12345678901")
            @RequestParam(value = "accountId", required = false) String accountId,
            
            @Parameter(description = "Start date for range filter (CCYYMMDD format)", example = "20240101")
            @RequestParam(value = "fromDate", required = false) String fromDate,
            
            @Parameter(description = "End date for range filter (CCYYMMDD format)", example = "20241231")
            @RequestParam(value = "toDate", required = false) String toDate,
            
            Authentication authentication) {
        
        logger.info("Processing transaction listing request for user: {}, page: {}, size: {}", 
                   authentication.getName(), pageNumber, pageSize);
        
        try {
            // Create transaction list request with all parameters
            TransactionListRequest request = new TransactionListRequest(UUID.randomUUID().toString());
            request.setPageNumber(pageNumber);
            request.setPageSize(pageSize);
            request.setSortBy(sortBy);
            request.setSortDirection(sortDirection);
            request.setTransactionId(transactionId);
            request.setCardNumber(cardNumber);
            request.setAccountId(accountId);
            request.setFromDate(fromDate);
            request.setToDate(toDate);
            
            // Process transaction listing through service layer
            TransactionListResponse response = transactionService.findTransactions(request);
            
            if (response.isSuccess()) {
                logger.info("Successfully processed transaction listing request. Returned {} transactions", 
                           response.getTransactions() != null ? response.getTransactions().size() : 0);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Transaction listing request failed: {}", response.getErrorMessage());
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error processing transaction listing request", e);
            
            TransactionListResponse errorResponse = new TransactionListResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage("Unable to retrieve transactions: " + e.getMessage());
            errorResponse.setCorrelationId(UUID.randomUUID().toString());
            
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }

    /**
     * GET /api/ct01/transactions/{id} - Retrieves detailed information for a specific transaction.
     * 
     * This endpoint provides transaction detail viewing functionality equivalent to the COBOL
     * COTRN01C.cbl program, including comprehensive security validation and authorization checks
     * to ensure users can only access transactions they are authorized to view.
     * 
     * <p><b>COBOL Program Equivalence (COTRN01C.cbl):</b></p>
     * <ul>
     *   <li>MAIN-PARA → HTTP GET request processing with ID parameter validation</li>
     *   <li>PROCESS-ENTER-KEY → Transaction ID validation and format checking</li>
     *   <li>READ-TRANSACT-FILE → Database lookup via JPA repository findById operation</li>
     *   <li>Field population → Response DTO creation with complete transaction details</li>
     *   <li>Authorization validation → User permission checking and ownership validation</li>
     * </ul>
     * 
     * <p><b>Security Authorization:</b></p>
     * <ul>
     *   <li>ADMIN users: Full access to all transaction details</li>
     *   <li>TELLER users: Access based on branch/region assignments</li>
     *   <li>USER/CUSTOMER roles: Access only to their own account transactions</li>
     *   <li>Comprehensive audit logging for all access attempts</li>
     * </ul>
     * 
     * @param transactionId The 16-character transaction identifier to retrieve
     * @param authentication Spring Security authentication context for authorization
     * @return ResponseEntity containing TransactionViewResponse with complete transaction details
     */
    @GetMapping("/ct01/transactions/{id}")
    @Operation(
        summary = "Get transaction details by ID",
        description = "Retrieves complete transaction details equivalent to COBOL COTRN01C.cbl program (CT01 transaction). " +
                     "Includes comprehensive security validation and authorization checks with complete audit trail.",
        tags = {"Transaction Viewing (CT01)"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved transaction details",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionViewResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid transaction ID format or validation failure",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required - invalid or missing JWT token",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - insufficient permissions for transaction viewing",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Transaction not found or user not authorized to view",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during transaction retrieval",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponseDto.class))
        )
    })
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('TELLER')")
    public ResponseEntity<TransactionViewResponse> getTransactionById(
            @Parameter(description = "16-character transaction identifier", example = "1234567890123456", required = true)
            @PathVariable("id") String transactionId,
            
            Authentication authentication) {
        
        logger.info("Processing transaction view request for ID: {}, user: {}", 
                   transactionId != null ? transactionId.substring(0, Math.min(transactionId.length(), 4)) + "***" : "null",
                   authentication.getName());
        
        try {
            // Process transaction viewing through service layer with security validation
            TransactionViewResponse response = transactionViewService.getTransactionDetails(transactionId, authentication);
            
            if (response.isSuccess()) {
                logger.info("Successfully retrieved transaction details for ID: {}", 
                           transactionId.substring(0, Math.min(transactionId.length(), 4)) + "***");
                return ResponseEntity.ok(response);
            } else {
                // Determine appropriate HTTP status code based on error type
                HttpStatus httpStatus = determineHttpStatus(response.getErrorMessage());
                
                logger.warn("Transaction view request failed with status {}: {}", httpStatus, response.getErrorMessage());
                return ResponseEntity
                    .status(httpStatus)
                    .body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error processing transaction view request for ID: {}", 
                        transactionId != null ? transactionId.substring(0, Math.min(transactionId.length(), 4)) + "***" : "null", e);
            
            TransactionViewResponse errorResponse = new TransactionViewResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage("Unable to retrieve transaction details: " + e.getMessage());
            errorResponse.setCorrelationId(UUID.randomUUID().toString());
            
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }

    /**
     * POST /api/ct02/transactions - Adds a new transaction with comprehensive validation.
     * 
     * This endpoint provides transaction addition functionality equivalent to the COBOL
     * COTRN02C.cbl program, including comprehensive validation pipeline, cross-reference checking,
     * and atomic processing with automatic rollback on validation failures or system errors.
     * 
     * <p><b>COBOL Program Equivalence (COTRN02C.cbl):</b></p>
     * <ul>
     *   <li>MAIN-PARA → HTTP POST request processing with comprehensive validation</li>
     *   <li>PROCESS-ENTER-KEY → Request validation and confirmation logic</li>
     *   <li>VALIDATE-INPUT-KEY-FIELDS → Account-card cross-reference validation</li>
     *   <li>VALIDATE-INPUT-DATA-FIELDS → Field validation and business rule checking</li>
     *   <li>ADD-TRANSACTION → Core transaction creation with UUID ID generation</li>
     *   <li>WRITE-TRANSACT-FILE → Database persistence via JPA repository save operation</li>
     * </ul>
     * 
     * <p><b>Validation Pipeline:</b></p>
     * <ul>
     *   <li>Jakarta Bean Validation for request field validation</li>
     *   <li>Account ID validation (11-digit numeric, must exist in accounts table)</li>
     *   <li>Card Number validation (16-digit numeric with Luhn algorithm)</li>
     *   <li>Account-Card cross-reference validation (ensures card linked to account)</li>
     *   <li>Transaction type and category validation against reference tables</li>
     *   <li>Amount validation with exact BigDecimal precision matching COBOL COMP-3</li>
     *   <li>Merchant data validation with proper field formatting</li>
     * </ul>
     * 
     * @param request Validated transaction addition request with all required fields
     * @param authentication Spring Security authentication context for authorization
     * @return ResponseEntity containing AddTransactionResponse with success status and transaction details
     */
    @PostMapping("/ct02/transactions")
    @Operation(
        summary = "Add new transaction",
        description = "Adds a new transaction equivalent to COBOL COTRN02C.cbl program (CT02 transaction). " +
                     "Includes comprehensive validation pipeline, cross-reference checking, and atomic processing " +
                     "with automatic rollback on validation failures.",
        tags = {"Transaction Addition (CT02)"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Transaction successfully created",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AddTransactionResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Validation failure or invalid request data",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AddTransactionResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required - invalid or missing JWT token",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - insufficient permissions for transaction creation",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Conflict - duplicate transaction or business rule violation",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AddTransactionResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during transaction processing",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AddTransactionResponse.class))
        )
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('TELLER')")
    public ResponseEntity<AddTransactionResponse> addTransaction(
            @Parameter(description = "Transaction addition request with complete validation", required = true)
            @Valid @RequestBody AddTransactionRequest request,
            
            Authentication authentication) {
        
        logger.info("Processing transaction addition request for account: {}, card: {}, user: {}", 
                   request.getAccountId(), 
                   request.getCardNumber() != null ? request.getCardNumber().substring(0, 4) + "****" + request.getCardNumber().substring(12) : "null",
                   authentication.getName());
        
        try {
            // Process transaction addition through service layer with comprehensive validation
            AddTransactionResponse response = addTransactionService.addTransaction(request);
            
            if (response.isSuccess()) {
                logger.info("Successfully added transaction with ID: {}", response.getTransactionId());
                return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);
            } else {
                // Determine appropriate HTTP status code based on error type and business rules
                HttpStatus httpStatus = determineHttpStatusForAddition(response);
                
                logger.warn("Transaction addition failed with status {}: {}", httpStatus, response.getMessage());
                return ResponseEntity
                    .status(httpStatus)
                    .body(response);
            }
            
        } catch (RuntimeException e) {
            logger.error("Runtime error during transaction addition", e);
            
            AddTransactionResponse errorResponse = new AddTransactionResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Unable to add transaction: " + e.getMessage());
            errorResponse.setHttpStatus(500);
            errorResponse.setErrorCode("SYSTEM_ERROR");
            
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
                
        } catch (Exception e) {
            logger.error("Unexpected error during transaction addition", e);
            
            AddTransactionResponse errorResponse = new AddTransactionResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("System error processing transaction request");
            errorResponse.setHttpStatus(500);
            errorResponse.setErrorCode("UNEXPECTED_ERROR");
            
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }

    /**
     * GET /api/ct00/transactions/search - Advanced transaction search with multiple criteria.
     * 
     * This endpoint provides enhanced search functionality building upon the base transaction
     * listing capabilities, supporting complex filter combinations for comprehensive transaction
     * discovery and analysis operations.
     * 
     * @param request TransactionListRequest with comprehensive search criteria
     * @param authentication Spring Security authentication context for authorization
     * @return ResponseEntity containing TransactionListResponse with filtered search results
     */
    @GetMapping("/ct00/transactions/search")
    @Operation(
        summary = "Search transactions with advanced filtering",
        description = "Advanced transaction search with multiple filter criteria, building upon CT00 transaction listing functionality. " +
                     "Supports complex filter combinations for comprehensive transaction discovery and analysis.",
        tags = {"Transaction Search (CT00)"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully executed transaction search",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionListResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid search criteria or validation failure",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required - invalid or missing JWT token",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - insufficient permissions for transaction search",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponseDto.class))
        )
    })
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('TELLER')")
    public ResponseEntity<TransactionListResponse> searchTransactions(
            @Valid TransactionListRequest request,
            Authentication authentication) {
        
        logger.info("Processing advanced transaction search request for user: {}", authentication.getName());
        
        try {
            // Ensure correlation ID is set for tracking
            if (request.getCorrelationId() == null) {
                request.setCorrelationId(UUID.randomUUID().toString());
            }
            
            // Process search through service layer
            TransactionListResponse response = transactionService.findTransactions(request);
            
            if (response.isSuccess()) {
                logger.info("Successfully processed transaction search request. Found {} results", 
                           response.getTransactions() != null ? response.getTransactions().size() : 0);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Transaction search request failed: {}", response.getErrorMessage());
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error processing transaction search request", e);
            
            TransactionListResponse errorResponse = new TransactionListResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage("Unable to execute transaction search: " + e.getMessage());
            errorResponse.setCorrelationId(request.getCorrelationId());
            
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }

    /**
     * GET /api/ct00/transactions/history - Retrieves transaction history with temporal filtering.
     * 
     * This endpoint provides specialized transaction history retrieval functionality with
     * enhanced temporal filtering capabilities for audit trail and compliance reporting purposes.
     * 
     * @param pageNumber Page number for pagination (default: 0)
     * @param pageSize Number of records per page (default: 10)
     * @param startDate Start date for history range (CCYYMMDD format)
     * @param endDate End date for history range (CCYYMMDD format)
     * @param accountId Optional account ID filter
     * @param authentication Spring Security authentication context for authorization
     * @return ResponseEntity containing TransactionListResponse with historical transaction data
     */
    @GetMapping("/ct00/transactions/history")
    @Operation(
        summary = "Get transaction history with temporal filtering",
        description = "Retrieves transaction history with enhanced temporal filtering capabilities for audit trail " +
                     "and compliance reporting. Based on CT00 transaction listing functionality with specialized " +
                     "historical data access patterns.",
        tags = {"Transaction History (CT00)"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved transaction history",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionListResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid date range or parameter validation failure",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required - invalid or missing JWT token",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - insufficient permissions for transaction history",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponseDto.class))
        )
    })
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('TELLER')")
    public ResponseEntity<TransactionListResponse> getTransactionHistory(
            @Parameter(description = "Page number for pagination", example = "0")
            @RequestParam(value = "pageNumber", defaultValue = "0") Integer pageNumber,
            
            @Parameter(description = "Number of records per page", example = "10") 
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            
            @Parameter(description = "Start date for history range (CCYYMMDD)", example = "20240101", required = true)
            @RequestParam(value = "startDate") String startDate,
            
            @Parameter(description = "End date for history range (CCYYMMDD)", example = "20241231", required = true) 
            @RequestParam(value = "endDate") String endDate,
            
            @Parameter(description = "Optional account ID filter", example = "12345678901")
            @RequestParam(value = "accountId", required = false) String accountId,
            
            Authentication authentication) {
        
        logger.info("Processing transaction history request for user: {}, date range: {} to {}", 
                   authentication.getName(), startDate, endDate);
        
        try {
            // Create specialized history request
            String correlationId = UUID.randomUUID().toString();
            TransactionListResponse response = transactionService.findTransactionsByDateRange(
                startDate, endDate, 
                org.springframework.data.domain.PageRequest.of(pageNumber, pageSize,
                    org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "processingTimestamp")),
                correlationId);
            
            if (response.isSuccess()) {
                logger.info("Successfully retrieved transaction history. Found {} records", 
                           response.getTransactions() != null ? response.getTransactions().size() : 0);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Transaction history request failed: {}", response.getErrorMessage());
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error processing transaction history request", e);
            
            TransactionListResponse errorResponse = new TransactionListResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage("Unable to retrieve transaction history: " + e.getMessage());
            errorResponse.setCorrelationId(UUID.randomUUID().toString());
            
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }

    /**
     * Determines appropriate HTTP status code based on error message content for transaction viewing.
     * 
     * @param errorMessage The error message from the service layer
     * @return HttpStatus appropriate for the error condition
     */
    private HttpStatus determineHttpStatus(String errorMessage) {
        if (errorMessage == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        
        String message = errorMessage.toLowerCase();
        
        if (message.contains("not found") || message.contains("id not found")) {
            return HttpStatus.NOT_FOUND;
        } else if (message.contains("unauthorized") || message.contains("access denied")) {
            return HttpStatus.FORBIDDEN;
        } else if (message.contains("invalid") || message.contains("empty") || message.contains("format")) {
            return HttpStatus.BAD_REQUEST;
        } else {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    /**
     * Determines appropriate HTTP status code for transaction addition based on response content.
     * 
     * @param response The AddTransactionResponse from the service layer
     * @return HttpStatus appropriate for the error condition
     */
    private HttpStatus determineHttpStatusForAddition(AddTransactionResponse response) {
        if (response.getHttpStatus() != null && response.getHttpStatus() > 0) {
            return HttpStatus.valueOf(response.getHttpStatus());
        }
        
        String errorCode = response.getErrorCode();
        if (errorCode != null) {
            switch (errorCode) {
                case "VALIDATION_FAILED":
                case "INVALID_CONFIRMATION":
                case "CONFIRMATION_REQUIRED":
                    return HttpStatus.BAD_REQUEST;
                case "UNAUTHORIZED_ACCESS":
                    return HttpStatus.FORBIDDEN;
                case "DUPLICATE_TRANSACTION":
                case "BUSINESS_RULE_VIOLATION":
                    return HttpStatus.CONFLICT;
                case "SYSTEM_ERROR":
                default:
                    return HttpStatus.INTERNAL_SERVER_ERROR;
            }
        }
        
        return HttpStatus.BAD_REQUEST;
    }
}