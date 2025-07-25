package com.carddemo.card;

import com.carddemo.card.CardListService;
import com.carddemo.card.CardListRequestDto;
import com.carddemo.card.CardListResponseDto;
import com.carddemo.common.security.JwtAuthenticationFilter;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller implementing credit card listing endpoints with role-based access control,
 * pagination support for 7 cards per page, and JWT authentication validation.
 * 
 * This controller replaces the COCRDLIC.cbl transaction functionality from the mainframe system,
 * implementing equivalent business logic through modern Spring Boot REST API patterns while
 * preserving exact functional behavior and validation rules.
 * 
 * Key Features:
 * - Paginated card browsing with 7 cards per page (matching legacy screen size)
 * - Role-based access control supporting both admin and regular users
 * - Account-based filtering for non-admin users
 * - Comprehensive input validation equivalent to COBOL edit paragraphs
 * - JWT authentication validation and error handling
 * - OpenAPI documentation with DTO definitions for API contract management
 * 
 * Original Transaction Code: CCLI (COCRDLIC.cbl)
 * Route Path: /api/cards/list (preserving transaction code traceability)
 */
@RestController
@RequestMapping("/api/cards")
@Tag(name = "Card Management", description = "Credit card listing and management operations")
@SecurityRequirement(name = "bearerAuth")
public class CardListController {
    
    private static final Logger logger = LoggerFactory.getLogger(CardListController.class);
    
    /**
     * Default page size matching the original COBOL screen size.
     * Corresponds to WS-MAX-SCREEN-LINES = 7 in COCRDLIC.cbl line 178.
     */
    private static final int DEFAULT_PAGE_SIZE = 7;
    
    /**
     * Maximum allowable page size to prevent performance issues.
     * Based on original VSAM browse performance characteristics.
     */
    private static final int MAX_PAGE_SIZE = 100;
    
    @Autowired
    private CardListService cardListService;
    
    /**
     * Primary endpoint for listing credit cards with pagination and filtering support.
     * Implements the core functionality of COCRDLIC.cbl with role-based access control.
     * 
     * Admin users can view all cards across all accounts, while regular users are
     * restricted to cards associated with their authorized accounts only.
     * 
     * This endpoint corresponds to the main processing logic in COCRDLIC.cbl starting
     * at line 298 (0000-MAIN paragraph) and pagination logic in 9000-READ-FORWARD.
     * 
     * @param pageNumber Zero-based page number for pagination (default: 0)
     * @param pageSize Number of cards per page (default: 7, max: 100)
     * @param accountId Optional account ID filter (11-digit account number)
     * @param cardNumber Optional card number filter (16-digit card number)
     * @param sortBy Optional sort field (default: cardNumber)
     * @param sortDirection Optional sort direction (ASC/DESC, default: ASC)
     * @return CardListResponseDto containing paginated card list and metadata
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    @Operation(
        summary = "List credit cards with pagination",
        description = "Retrieves paginated list of credit cards with optional filtering. " +
                      "Admin users can view all cards, regular users see only authorized cards. " +
                      "Implements COCRDLIC.cbl transaction functionality with 7 cards per page default.",
        parameters = {
            @Parameter(name = "pageNumber", description = "Zero-based page number", example = "0"),
            @Parameter(name = "pageSize", description = "Number of cards per page (max 100)", example = "7"),
            @Parameter(name = "accountId", description = "Filter by 11-digit account ID", example = "12345678901"),
            @Parameter(name = "cardNumber", description = "Filter by 16-digit card number", example = "1234567890123456"),
            @Parameter(name = "sortBy", description = "Sort field name", example = "cardNumber"),
            @Parameter(name = "sortDirection", description = "Sort direction (ASC/DESC)", example = "ASC")
        }
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Cards retrieved successfully",
            content = @Content(schema = @Schema(implementation = CardListResponseDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<CardListResponseDto> listCards(
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "7") int pageSize,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String cardNumber,
            @RequestParam(defaultValue = "cardNumber") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {
        
        logger.info("Processing card list request - page: {}, size: {}, accountId: {}, cardNumber: {}", 
                   pageNumber, pageSize, accountId, cardNumber);
        
        try {
            // Input validation equivalent to COBOL 2200-EDIT-INPUTS paragraph
            validatePaginationParameters(pageNumber, pageSize);
            validateFilterParameters(accountId, cardNumber);
            
            // Create pageable request with sorting
            Sort sort = createSortSpecification(sortBy, sortDirection);
            Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
            
            // Build request DTO equivalent to COBOL COMMAREA processing
            CardListRequestDto request = buildCardListRequest(pageable, accountId, cardNumber);
            
            // Delegate to service layer for business logic processing
            CardListResponseDto response = cardListService.listCards(request);
            
            logger.info("Card list request completed successfully - returned {} cards", 
                       response.getCards().size());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error processing card list request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Endpoint for listing cards by specific account ID with enhanced access control.
     * Implements account-specific filtering logic from COCRDLIC.cbl lines 1385-1405.
     * 
     * This endpoint provides optimized access for account-specific card browsing,
     * corresponding to the account filter validation in FLG-ACCTFILTER-ISVALID logic.
     * 
     * @param accountId 11-digit account ID for card filtering
     * @param pageNumber Zero-based page number for pagination (default: 0)
     * @param pageSize Number of cards per page (default: 7, max: 100)
     * @param sortBy Optional sort field (default: cardNumber)
     * @param sortDirection Optional sort direction (ASC/DESC, default: ASC)
     * @return CardListResponseDto containing paginated card list for the specified account
     */
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    @Operation(
        summary = "List cards for specific account",
        description = "Retrieves paginated list of credit cards for a specific account. " +
                      "Implements account filtering logic from COCRDLIC.cbl with role-based access control.",
        parameters = {
            @Parameter(name = "accountId", description = "11-digit account ID", required = true),
            @Parameter(name = "pageNumber", description = "Zero-based page number", example = "0"),
            @Parameter(name = "pageSize", description = "Number of cards per page (max 100)", example = "7"),
            @Parameter(name = "sortBy", description = "Sort field name", example = "cardNumber"),
            @Parameter(name = "sortDirection", description = "Sort direction (ASC/DESC)", example = "ASC")
        }
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Cards retrieved successfully for account",
            content = @Content(schema = @Schema(implementation = CardListResponseDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid account ID or parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges for account access"),
        @ApiResponse(responseCode = "404", description = "Account not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<CardListResponseDto> listCardsByAccountId(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "7") int pageSize,
            @RequestParam(defaultValue = "cardNumber") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {
        
        logger.info("Processing cards by account request - accountId: {}, page: {}, size: {}", 
                   accountId, pageNumber, pageSize);
        
        try {
            // Validate account ID format (equivalent to COBOL 2210-EDIT-ACCOUNT paragraph)
            validateAccountId(accountId);
            validatePaginationParameters(pageNumber, pageSize);
            
            // Create pageable request with sorting
            Sort sort = createSortSpecification(sortBy, sortDirection);
            Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
            
            // Build request DTO with account filter
            CardListRequestDto request = buildCardListRequest(pageable, accountId, null);
            
            // Delegate to service layer for business logic processing
            CardListResponseDto response = cardListService.listCards(request);
            
            logger.info("Cards by account request completed - accountId: {}, returned {} cards", 
                       accountId, response.getCards().size());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for account {}: {}", accountId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error processing cards by account request for account: {}", accountId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Advanced card search endpoint with comprehensive filtering capabilities.
     * Implements POST-based search for complex filter combinations from COCRDLIC.cbl.
     * 
     * @param request CardListRequestDto containing search criteria and pagination
     * @return CardListResponseDto containing filtered and paginated results
     */
    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    @Operation(
        summary = "Advanced card search with complex filtering",
        description = "Performs advanced card search with comprehensive filtering options. " +
                      "Supports complex search criteria through POST request body.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Card search criteria with pagination parameters",
            required = true,
            content = @Content(schema = @Schema(implementation = CardListRequestDto.class))
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Card search completed successfully",
            content = @Content(schema = @Schema(implementation = CardListResponseDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid search criteria"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<CardListResponseDto> searchCards(@Valid @RequestBody CardListRequestDto request) {
        
        logger.info("Processing advanced card search request - filters: accountId={}, cardNumber={}", 
                   request.getAccountId(), request.getCardNumber());
        
        try {
            // Validate search request parameters
            validateSearchRequest(request);
            
            // Delegate to service layer for business logic processing
            CardListResponseDto response = cardListService.listCards(request);
            
            logger.info("Advanced card search completed - returned {} cards", 
                       response.getCards().size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing advanced card search", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Exception handler for validation errors.
     * Implements standardized error response equivalent to COBOL error message handling.
     * 
     * @param ex ConstraintViolationException containing validation errors
     * @return ResponseEntity with error details and BAD_REQUEST status
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleValidationException(ConstraintViolationException ex) {
        logger.warn("Validation error in card list request: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "VALIDATION_ERROR");
        errorResponse.put("message", "Request validation failed");
        errorResponse.put("details", ex.getMessage());
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Exception handler for authorization errors.
     * Implements access control error handling equivalent to RACF security violations.
     * 
     * @param ex SecurityException containing authorization error details
     * @return ResponseEntity with error details and UNAUTHORIZED status
     */
    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<Map<String, Object>> handleAuthorizationException(SecurityException ex) {
        logger.warn("Authorization error in card list request: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "AUTHORIZATION_ERROR");
        errorResponse.put("message", "Insufficient privileges for requested operation");
        errorResponse.put("details", ex.getMessage());
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    // ===============================
    // Private Helper Methods
    // ===============================
    
    /**
     * Validates pagination parameters equivalent to COBOL input validation.
     * Implements validation logic similar to WS-MAX-SCREEN-LINES checking.
     */
    private void validatePaginationParameters(int pageNumber, int pageSize) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("Page number must be non-negative");
        }
        
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }
        
        if (pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                String.format("Page size must not exceed %d", MAX_PAGE_SIZE));
        }
    }
    
    /**
     * Validates filter parameters equivalent to COBOL 2210-EDIT-ACCOUNT and 2220-EDIT-CARD.
     */
    private void validateFilterParameters(String accountId, String cardNumber) {
        if (accountId != null && !accountId.trim().isEmpty()) {
            validateAccountId(accountId);
        }
        
        if (cardNumber != null && !cardNumber.trim().isEmpty()) {
            validateCardNumber(cardNumber);
        }
    }
    
    /**
     * Validates account ID format equivalent to COBOL CC-ACCT-ID validation.
     * Implements logic from lines 1017-1029 in COCRDLIC.cbl.
     */
    private void validateAccountId(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return; // Empty is acceptable for optional parameter
        }
        
        String trimmed = accountId.trim();
        if (trimmed.length() != 11) {
            throw new IllegalArgumentException(
                "Account ID filter, if supplied, must be an 11 digit number");
        }
        
        if (!trimmed.matches("\\d{11}")) {
            throw new IllegalArgumentException(
                "Account ID filter, if supplied, must be a 11 digit number");
        }
    }
    
    /**
     * Validates card number format equivalent to COBOL CC-CARD-NUM validation.
     * Implements logic from lines 1052-1066 in COCRDLIC.cbl.
     */
    private void validateCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            return; // Empty is acceptable for optional parameter
        }
        
        String trimmed = cardNumber.trim();
        if (trimmed.length() != 16) {
            throw new IllegalArgumentException(
                "Card ID filter, if supplied, must be a 16 digit number");
        }
        
        if (!trimmed.matches("\\d{16}")) {
            throw new IllegalArgumentException(
                "Card ID filter, if supplied, must be a 16 digit number");
        }
    }
    
    /**
     * Creates sort specification from request parameters.
     */
    private Sort createSortSpecification(String sortBy, String sortDirection) {
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection) ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        
        return Sort.by(direction, sortBy);
    }
    
    /**
     * Builds CardListRequestDto from request parameters.
     * Equivalent to COBOL COMMAREA processing and WS-THIS-PROGCOMMAREA setup.
     */
    private CardListRequestDto buildCardListRequest(Pageable pageable, String accountId, String cardNumber) {
        CardListRequestDto request = new CardListRequestDto();
        
        // Set pagination parameters
        request.setPageNumber(pageable.getPageNumber());
        request.setPageSize(pageable.getPageSize());
        
        // Set sort parameters
        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            request.setSortBy(order.getProperty());
            request.setSortDirection(order.getDirection().name());
        }
        
        // Set filter parameters
        request.setAccountId(accountId);
        request.setCardNumber(cardNumber);
        
        return request;
    }
    
    /**
     * Validates advanced search request parameters.
     */
    private void validateSearchRequest(CardListRequestDto request) {
        // Validate pagination
        validatePaginationParameters(
            Optional.ofNullable(request.getPageNumber()).orElse(0),
            Optional.ofNullable(request.getPageSize()).orElse(DEFAULT_PAGE_SIZE)
        );
        
        // Validate filters
        validateFilterParameters(request.getAccountId(), request.getCardNumber());
    }
}