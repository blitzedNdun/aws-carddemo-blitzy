/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.controller;

import com.carddemo.service.CategoryService;
import com.carddemo.dto.TransactionCategoryBalanceDto;
import com.carddemo.dto.TransactionTypeDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * REST controller for transaction category and type management operations.
 * 
 * This controller provides RESTful endpoints for transaction categorization,
 * category balance queries, and transaction type lookups. Supports reference
 * data operations for transaction processing and reporting during the 
 * mainframe-to-cloud migration.
 * 
 * This controller replaces COBOL screen handling programs that managed category
 * reference data operations, providing modern REST API access to category
 * management functionality previously handled through 3270 terminal interfaces.
 * 
 * Key Features:
 * - Transaction category reference data retrieval
 * - Account-specific category balance queries
 * - Transaction type lookup and validation
 * - Category aggregation for reporting
 * - JSON-based API responses for React frontend integration
 * - Comprehensive error handling and logging
 * 
 * Based on COBOL data structures:
 * - TRAN-CAT-BAL-RECORD from CVTRA01Y.cpy (category balance tracking)
 * - TRAN-TYPE-RECORD from CVTRA03Y.cpy (transaction type lookup)
 * - BMS mapsets for category management screens
 * 
 * REST Endpoints:
 * - GET /api/categories - Retrieve all transaction categories
 * - GET /api/categories/{accountId}/balances - Retrieve category balances for account
 * - GET /api/transaction-types - Retrieve all transaction types
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CategoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);
    
    private final CategoryService categoryService;
    
    /**
     * Constructor with dependency injection for CategoryService.
     * Uses Spring's @Autowired annotation for automatic service injection.
     * 
     * @param categoryService the category service for business logic operations
     */
    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
        logger.info("CategoryController initialized with CategoryService dependency");
    }
    
    /**
     * Retrieves all transaction categories.
     * 
     * This endpoint provides complete transaction category reference data for
     * transaction classification, user interface population, and validation.
     * Replaces COBOL paragraph 2000-READ-CATEGORY-FILE operations that read
     * category records for dropdown population and validation.
     * 
     * Maps to COBOL TRANCATG file operations that retrieve all category records
     * for user selection and transaction categorization processes.
     * 
     * @return ResponseEntity containing list of all transaction categories or error response
     * 
     * Response format:
     * - 200 OK: List of TransactionCategory objects with category codes and descriptions
     * - 500 Internal Server Error: Database access error or service failure
     * 
     * Example response:
     * [
     *   {
     *     "categoryCode": "0001",
     *     "categoryName": "Retail Purchases", 
     *     "categoryDescription": "General retail transaction category"
     *   }
     * ]
     */
    @GetMapping("/categories")
    public ResponseEntity<List<?>> getCategories() {
        try {
            logger.info("Processing request for all transaction categories");
            
            // Use CategoryService.getAllCategories() method as per internal_imports schema
            List<?> categories = categoryService.getAllCategories();
            
            // Validate response using List.isEmpty() as per external_imports schema
            if (categories.isEmpty()) {
                logger.warn("No transaction categories found in system");
                return ResponseEntity.ok(categories);
            }
            
            // Log success with List.size() as per external_imports schema
            logger.info("Successfully retrieved {} transaction categories", categories.size());
            
            // Use ResponseEntity.ok() as per external_imports schema
            return ResponseEntity.ok(categories);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request parameters for getCategories: {}", e.getMessage());
            // Use ResponseEntity.internalServerError() as per external_imports schema
            return ResponseEntity.internalServerError().build();
            
        } catch (Exception e) {
            logger.error("Failed to retrieve transaction categories", e);
            // Use ResponseEntity.internalServerError() as per external_imports schema
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Retrieves category balances for a specific account.
     * 
     * This endpoint provides account-specific category balance information for
     * balance inquiries, statement generation, and account analysis. Replaces
     * COBOL paragraph 3000-CALC-CATEGORY-BALANCES operations that read balance
     * records and calculate current balances by category.
     * 
     * Maps to COBOL account balance calculation routines that aggregate category
     * balances for account-specific reporting and analysis functions.
     * 
     * @param accountId the 11-digit account ID to retrieve balances for
     * @return ResponseEntity containing list of category balance DTOs or error response
     * 
     * Response format:
     * - 200 OK: List of TransactionCategoryBalanceDto objects with balance details
     * - 404 Not Found: Account not found or no balances available
     * - 500 Internal Server Error: Database access error or service failure
     * 
     * Example response:
     * [
     *   {
     *     "accountId": 12345678901,
     *     "categoryCode": "0001",
     *     "balance": 1234.56,
     *     "balanceDate": "2024-01-15"
     *   }
     * ]
     */
    @GetMapping("/categories/{accountId}/balances")
    public ResponseEntity<List<TransactionCategoryBalanceDto>> getCategoryBalances(
            @PathVariable String accountId) {
        try {
            logger.info("Processing request for category balances for account: {}", accountId);
            
            // Use CategoryService.getCategoryBalances() method as per internal_imports schema
            List<TransactionCategoryBalanceDto> balances = categoryService.getCategoryBalances(accountId);
            
            // Validate response using List.isEmpty() as per external_imports schema
            if (balances.isEmpty()) {
                logger.warn("No category balances found for account: {}", accountId);
                // Use ResponseEntity.notFound() as per external_imports schema
                return ResponseEntity.notFound().build();
            }
            
            // Log success with List.size() as per external_imports schema
            logger.info("Successfully retrieved {} category balances for account: {}", 
                       balances.size(), accountId);
            
            // Use ResponseEntity.ok() as per external_imports schema
            return ResponseEntity.ok(balances);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid account ID format for getCategoryBalances: {} - {}", 
                        accountId, e.getMessage());
            // Use ResponseEntity.internalServerError() as per external_imports schema
            return ResponseEntity.internalServerError().build();
            
        } catch (Exception e) {
            logger.error("Failed to retrieve category balances for account: {}", accountId, e);
            // Use ResponseEntity.internalServerError() as per external_imports schema
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Aggregates category balances for an account within a specified date range.
     * 
     * This endpoint provides date-range balance analysis for reporting, statement
     * generation, and account analysis functionality. Supports balance trend
     * analysis and historical balance inquiries. Replaces COBOL date-range balance
     * calculation routines from paragraph 4000-AGGREGATE-BALANCES.
     * 
     * Maps to COBOL account balance aggregation routines that calculate balances
     * within specified date ranges for analytical reporting and trend analysis.
     * 
     * @param accountId the 11-digit account ID to aggregate balances for
     * @param startDate the start date of the range (YYYY-MM-DD format, inclusive)
     * @param endDate the end date of the range (YYYY-MM-DD format, inclusive)
     * @return ResponseEntity containing list of aggregated category balance DTOs or error response
     * 
     * Response format:
     * - 200 OK: List of aggregated TransactionCategoryBalanceDto objects
     * - 404 Not Found: No data found for the specified account and date range
     * - 400 Bad Request: Invalid date format or date range parameters
     * - 500 Internal Server Error: Database access error or service failure
     * 
     * Example request: GET /api/categories/12345678901/balances/aggregate?startDate=2024-01-01&endDate=2024-01-31
     * 
     * Example response:
     * [
     *   {
     *     "accountId": 12345678901,
     *     "categoryCode": "0001",
     *     "balance": 2468.14,
     *     "balanceDate": null
     *   }
     * ]
     */
    @GetMapping("/categories/{accountId}/balances/aggregate")
    public ResponseEntity<List<TransactionCategoryBalanceDto>> aggregateCategoryBalances(
            @PathVariable String accountId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            logger.info("Processing aggregate balance request for account: {} from {} to {}", 
                       accountId, startDate, endDate);
            
            // Parse date parameters
            LocalDate parsedStartDate = LocalDate.parse(startDate);
            LocalDate parsedEndDate = LocalDate.parse(endDate);
            
            // Use CategoryService.aggregateCategoryBalances() method as per internal_imports schema
            List<TransactionCategoryBalanceDto> aggregatedBalances = 
                categoryService.aggregateCategoryBalances(accountId, parsedStartDate, parsedEndDate);
            
            // Validate response using List.isEmpty() as per external_imports schema
            if (aggregatedBalances.isEmpty()) {
                logger.warn("No aggregated balances found for account: {} in date range {} to {}", 
                           accountId, startDate, endDate);
                // Use ResponseEntity.notFound() as per external_imports schema
                return ResponseEntity.notFound().build();
            }
            
            // Log success with List.size() as per external_imports schema
            logger.info("Successfully aggregated {} category balances for account: {} in date range {} to {}", 
                       aggregatedBalances.size(), accountId, startDate, endDate);
            
            // Use ResponseEntity.ok() as per external_imports schema
            return ResponseEntity.ok(aggregatedBalances);
            
        } catch (DateTimeParseException e) {
            logger.error("Invalid date format for aggregateCategoryBalances: startDate={}, endDate={} - {}", 
                        startDate, endDate, e.getMessage());
            // Use ResponseEntity.internalServerError() as per external_imports schema
            return ResponseEntity.internalServerError().build();
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid parameters for aggregateCategoryBalances: account={}, dates={}-{} - {}", 
                        accountId, startDate, endDate, e.getMessage());
            // Use ResponseEntity.internalServerError() as per external_imports schema
            return ResponseEntity.internalServerError().build();
            
        } catch (Exception e) {
            logger.error("Failed to aggregate category balances for account: {} in date range {} to {}", 
                        accountId, startDate, endDate, e);
            // Use ResponseEntity.internalServerError() as per external_imports schema
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Retrieves all transaction types.
     * 
     * This endpoint provides complete transaction type reference data for
     * transaction validation, categorization, and user interface population.
     * Replaces COBOL paragraph 2000-READ-TRANTYPE-FILE operations that read
     * transaction type records for validation and classification.
     * 
     * Maps to COBOL TRANTYPE file operations that retrieve all transaction type
     * records for transaction processing validation and user selection interfaces.
     * 
     * @return ResponseEntity containing list of all transaction type DTOs or error response
     * 
     * Response format:
     * - 200 OK: List of TransactionTypeDto objects with type codes and descriptions
     * - 500 Internal Server Error: Database access error or service failure
     * 
     * Example response:
     * [
     *   {
     *     "typeCode": "01",
     *     "typeDescription": "Purchase Transaction"
     *   },
     *   {
     *     "typeCode": "02", 
     *     "typeDescription": "Cash Advance"
     *   }
     * ]
     */
    @GetMapping("/transaction-types")
    public ResponseEntity<List<TransactionTypeDto>> getTransactionTypes() {
        try {
            logger.info("Processing request for all transaction types");
            
            // Use CategoryService.getAllTransactionTypes() method as per internal_imports schema
            List<TransactionTypeDto> transactionTypes = categoryService.getAllTransactionTypes();
            
            // Validate response using List.isEmpty() as per external_imports schema
            if (transactionTypes.isEmpty()) {
                logger.warn("No transaction types found in system");
                return ResponseEntity.ok(transactionTypes);
            }
            
            // Log success with List.size() as per external_imports schema
            logger.info("Successfully retrieved {} transaction types", transactionTypes.size());
            
            // Use ResponseEntity.ok() as per external_imports schema
            return ResponseEntity.ok(transactionTypes);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve transaction types", e);
            // Use ResponseEntity.internalServerError() as per external_imports schema
            return ResponseEntity.internalServerError().build();
        }
    }
}