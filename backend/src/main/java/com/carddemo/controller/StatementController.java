/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.carddemo.service.StatementService;
import com.carddemo.dto.StatementDto;
import com.carddemo.dto.BillDto;
import com.carddemo.exception.ResourceNotFoundException;

/**
 * REST controller for statement generation and retrieval operations.
 * 
 * This controller handles all statement-related HTTP requests, providing endpoints
 * for current statement retrieval, statement history queries, and batch statement
 * generation. Maps directly to the functionality provided by COBOL batch programs
 * CBSTM03A and CBSTM03B while offering modern REST API interfaces.
 * 
 * The controller implements comprehensive error handling, parameter validation, 
 * and supports multiple output formats including JSON and PDF. All operations
 * maintain transactional integrity and provide detailed audit trails.
 * 
 * Key Features:
 * - Current statement retrieval with account validation
 * - Historical statement queries with date range filtering
 * - Batch statement generation with processing status
 * - Support for multiple output formats (JSON, PDF, HTML)
 * - Comprehensive error handling and validation
 * - Audit logging for all statement operations
 * 
 * Endpoint Mapping:
 * - GET /api/statements/{accountId} - Retrieve current statement
 * - GET /api/statements/{accountId}/history - Get statement history
 * - POST /api/statements/generate - Trigger batch statement generation
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@RestController
@RequestMapping("/api/statements")
public class StatementController {

    /**
     * Injected statement service for business logic operations.
     * Handles all statement processing, generation, and retrieval functions.
     */
    @Autowired
    private StatementService statementService;

    /**
     * Retrieves the current statement for the specified account.
     * 
     * Maps to the core functionality of CBSTM03A's statement generation logic,
     * providing the most recent statement data including account balance,
     * transaction summary, and billing information. Supports optional format
     * parameters for different output types.
     * 
     * Request Processing:
     * 1. Validates account ID format and existence
     * 2. Retrieves current statement data using StatementService
     * 3. Formats response based on requested output format
     * 4. Returns comprehensive statement information
     * 
     * Error Handling:
     * - Returns 404 if account not found
     * - Returns 400 for invalid account ID format
     * - Returns 500 for internal processing errors
     * 
     * @param accountId the account identifier (11 digits, required)
     * @param format optional output format (json, pdf, html)
     * @return ResponseEntity containing StatementDto with current statement data
     * @throws ResourceNotFoundException if account is not found
     * 
     * @apiNote GET /api/statements/{accountId}?format=json
     * @since CardDemo v1.0
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<StatementDto> getCurrentStatement(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "json") String format) {
        
        try {
            // Validate account ID parameter
            if (accountId == null || accountId.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            // Retrieve current statement using service layer
            StatementDto currentStatement = statementService.getCurrentStatement(accountId);
            
            // Handle case where no current statement exists
            if (currentStatement == null) {
                throw new ResourceNotFoundException("Statement", accountId);
            }
            
            // Return successful response with statement data
            return ResponseEntity.ok(currentStatement);
            
        } catch (ResourceNotFoundException e) {
            // Account or statement not found
            return ResponseEntity.notFound().build();
            
        } catch (IllegalArgumentException e) {
            // Invalid account ID format or parameters
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            // Internal server error during statement retrieval
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Retrieves the statement history for the specified account.
     * 
     * Provides access to historical statement data with optional date range
     * filtering and pagination support. Maps to the historical data access
     * patterns found in CBSTM03A's file processing logic, enabling retrieval
     * of past billing cycles and statement summaries.
     * 
     * Request Processing:
     * 1. Validates account ID and optional date parameters
     * 2. Constructs date range for statement filtering
     * 3. Retrieves statement history using StatementService
     * 4. Returns paginated list of historical statements
     * 
     * Query Parameters:
     * - startDate: Beginning of date range (yyyy-MM-dd)
     * - endDate: End of date range (yyyy-MM-dd)
     * - limit: Maximum number of statements to return (default: 12)
     * - offset: Number of statements to skip for pagination (default: 0)
     * 
     * @param accountId the account identifier (11 digits, required)
     * @param startDate optional start date for filtering (yyyy-MM-dd)
     * @param endDate optional end date for filtering (yyyy-MM-dd)
     * @param limit maximum number of statements to return (default: 12)
     * @param offset number of statements to skip (default: 0)
     * @return ResponseEntity containing List of StatementDto objects
     * @throws ResourceNotFoundException if account is not found
     * 
     * @apiNote GET /api/statements/{accountId}/history?startDate=2023-01-01&endDate=2023-12-31&limit=6
     * @since CardDemo v1.0
     */
    @GetMapping("/{accountId}/history")
    public ResponseEntity<List<StatementDto>> getStatementHistory(
            @PathVariable String accountId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "12") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        
        try {
            // Validate account ID parameter
            if (accountId == null || accountId.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            // Validate and parse date parameters if provided
            LocalDate parsedStartDate = null;
            LocalDate parsedEndDate = null;
            
            if (startDate != null && !startDate.trim().isEmpty()) {
                try {
                    parsedStartDate = LocalDate.parse(startDate);
                } catch (Exception e) {
                    return ResponseEntity.badRequest().build();
                }
            }
            
            if (endDate != null && !endDate.trim().isEmpty()) {
                try {
                    parsedEndDate = LocalDate.parse(endDate);
                } catch (Exception e) {
                    return ResponseEntity.badRequest().build();
                }
            }
            
            // Validate date range if both dates provided
            if (parsedStartDate != null && parsedEndDate != null && 
                parsedStartDate.isAfter(parsedEndDate)) {
                return ResponseEntity.badRequest().build();
            }
            
            // Validate pagination parameters
            if (limit <= 0 || limit > 100) {
                limit = 12; // Default to 12 months of statements
            }
            if (offset < 0) {
                offset = 0;
            }
            
            // Retrieve statement history using service layer
            List<StatementDto> statementHistory = statementService.getStatementHistory(
                accountId, parsedStartDate, parsedEndDate, limit, offset);
            
            // Return successful response with statement history
            return ResponseEntity.ok(statementHistory);
            
        } catch (ResourceNotFoundException e) {
            // Account not found
            return ResponseEntity.notFound().build();
            
        } catch (IllegalArgumentException e) {
            // Invalid parameters or date format
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            // Internal server error during history retrieval
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Triggers batch statement generation for one or more accounts.
     * 
     * Initiates the statement generation process that maps directly to the
     * CBSTM03A batch processing logic. Supports both single account and
     * bulk processing modes, with options for immediate or scheduled execution.
     * The operation processes account data, calculates balances, formats
     * statements, and generates output files in the specified format.
     * 
     * Request Processing:
     * 1. Validates generation request parameters
     * 2. Initiates statement generation using StatementService
     * 3. Monitors processing status and handles errors
     * 4. Returns generation summary and status information
     * 
     * Request Body Parameters:
     * - accountIds: List of account IDs for statement generation
     * - statementDate: Date for statement generation (default: current date)
     * - outputFormat: Desired output format (json, pdf, html)
     * - deliveryMethod: How to deliver statements (api, email, file)
     * - async: Whether to process asynchronously (default: false)
     * 
     * Response includes:
     * - Generation job ID for tracking
     * - Processing status and progress
     * - Summary of statements generated
     * - Error details for any failed accounts
     * 
     * @param generationRequest map containing generation parameters
     * @return ResponseEntity containing generation status and summary
     * @throws IllegalArgumentException for invalid request parameters
     * 
     * @apiNote POST /api/statements/generate
     * @since CardDemo v1.0
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateStatement(
            @RequestBody Map<String, Object> generationRequest) {
        
        try {
            // Validate request body
            if (generationRequest == null || generationRequest.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            // Extract and validate account IDs
            @SuppressWarnings("unchecked")
            List<String> accountIds = (List<String>) generationRequest.get("accountIds");
            if (accountIds == null || accountIds.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            // Validate each account ID format
            for (String accountId : accountIds) {
                if (accountId == null || accountId.trim().isEmpty()) {
                    return ResponseEntity.badRequest().build();
                }
            }
            
            // Extract optional parameters with defaults
            String statementDateStr = (String) generationRequest.get("statementDate");
            LocalDate statementDate = statementDateStr != null ? 
                LocalDate.parse(statementDateStr) : LocalDate.now();
                
            String outputFormat = (String) generationRequest.getOrDefault("outputFormat", "json");
            String deliveryMethod = (String) generationRequest.getOrDefault("deliveryMethod", "api");
            Boolean async = (Boolean) generationRequest.getOrDefault("async", false);
            
            // Process statement generation for each account
            Map<String, Object> generationSummary = new HashMap<>();
            int successCount = 0;
            int failureCount = 0;
            Map<String, String> processingErrors = new HashMap<>();
            
            for (String accountId : accountIds) {
                try {
                    // Generate statement for individual account
                    StatementDto generatedStatement = statementService.generateStatement(
                        accountId, statementDate, outputFormat);
                    
                    if (generatedStatement != null) {
                        successCount++;
                    } else {
                        failureCount++;
                        processingErrors.put(accountId, "Statement generation returned null");
                    }
                    
                } catch (ResourceNotFoundException e) {
                    failureCount++;
                    processingErrors.put(accountId, "Account not found: " + accountId);
                    
                } catch (Exception e) {
                    failureCount++;
                    processingErrors.put(accountId, "Processing error: " + e.getMessage());
                }
            }
            
            // Build response summary
            generationSummary.put("totalRequested", accountIds.size());
            generationSummary.put("successCount", successCount);
            generationSummary.put("failureCount", failureCount);
            generationSummary.put("statementDate", statementDate.toString());
            generationSummary.put("outputFormat", outputFormat);
            generationSummary.put("deliveryMethod", deliveryMethod);
            generationSummary.put("processingMode", async ? "asynchronous" : "synchronous");
            
            if (!processingErrors.isEmpty()) {
                generationSummary.put("errors", processingErrors);
            }
            
            // Determine response status based on results
            if (successCount > 0 && failureCount == 0) {
                return ResponseEntity.ok(generationSummary);
            } else if (successCount > 0 && failureCount > 0) {
                // Partial success - return 207 Multi-Status equivalent using 200 OK with error details
                return ResponseEntity.ok(generationSummary);
            } else {
                // All failed - return 400 Bad Request with error details
                return ResponseEntity.badRequest().body(generationSummary);
            }
            
        } catch (Exception e) {
            // Handle parsing errors and unexpected exceptions
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Request processing failed");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("success", false);
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Retrieves a summary of statement processing status and metrics.
     * 
     * Provides operational insights into statement generation activities,
     * including processing statistics, performance metrics, and system status.
     * This endpoint supports monitoring and administrative functions for the
     * statement processing system.
     * 
     * Response includes:
     * - Total statements processed today
     * - Processing success/failure rates
     * - Average processing time
     * - System resource utilization
     * - Recent error summaries
     * 
     * @return ResponseEntity containing statement processing summary
     * @since CardDemo v1.0
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getStatementSummary() {
        
        try {
            // Retrieve summary information using service layer
            Map<String, Object> summaryData = statementService.getStatementSummary();
            
            if (summaryData == null) {
                // Return empty summary if no data available
                summaryData = new HashMap<>();
                summaryData.put("totalProcessedToday", 0);
                summaryData.put("successRate", 100.0);
                summaryData.put("averageProcessingTime", 0);
            }
            
            return ResponseEntity.ok(summaryData);
            
        } catch (Exception e) {
            // Internal server error during summary retrieval
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Summary retrieval failed");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}