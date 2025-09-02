/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.controller;

import com.carddemo.dto.DisclosureGroupDto;
import com.carddemo.service.InterestCalculationService;
import com.carddemo.service.InterestRateService;
import com.carddemo.dto.InterestCalculationRequest;
import com.carddemo.dto.InterestCalculationResponse;
import com.carddemo.dto.InterestRateResponse;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * REST controller for interest calculation and management operations.
 * 
 * This controller handles interest rate queries, interest calculation for accounts,
 * and disclosure group management. It maintains BigDecimal precision for interest
 * calculations matching COBOL COMP-3 behavior from the original CBACT04C.cbl program.
 * 
 * The controller translates the COBOL interest calculation logic:
 * WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
 * 
 * into modern REST API endpoints while preserving exact calculation precision
 * and business logic from the mainframe system.
 * 
 * Endpoints provided:
 * - GET /api/interest/rates - Returns current interest rates by category
 * - POST /api/interest/calculate - Calculates monthly interest for account
 * - GET /api/interest/disclosure-groups - Returns disclosure group information
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/interest")
@Slf4j
public class InterestController {

    private final InterestRateService interestRateService;
    private final InterestCalculationService interestCalculationService;

    /**
     * Constructor-based dependency injection for Spring Boot services.
     * Ensures proper dependency management and testability.
     * 
     * @param interestRateService Service for interest rate management operations
     * @param interestCalculationService Service for interest calculation operations
     */
    @Autowired
    public InterestController(InterestRateService interestRateService,
                            InterestCalculationService interestCalculationService) {
        this.interestRateService = interestRateService;
        this.interestCalculationService = interestCalculationService;
        log.info("InterestController initialized successfully");
    }

    /**
     * Retrieves current interest rates by category and disclosure group.
     * 
     * This endpoint replaces the COBOL disclosure group lookup functionality
     * from CBACT04C.cbl (1200-GET-INTEREST-RATE paragraph). It provides
     * current effective interest rates for different transaction categories
     * and account groups.
     * 
     * Supports optional category filtering via query parameter to get rates
     * for specific transaction categories matching COBOL TRAN-CAT-CD processing.
     * 
     * @param categoryCode Optional query parameter to filter rates by category
     * @return ResponseEntity containing list of current interest rates
     */
    @GetMapping("/rates")
    public ResponseEntity<List<InterestRateResponse>> getCurrentRates(
            @RequestParam(value = "categoryCode", required = false) String categoryCode) {
        
        log.debug("Processing request for current interest rates with category filter: {}", categoryCode);
        
        try {
            List<InterestRateResponse> rates;
            
            if (categoryCode != null && !categoryCode.trim().isEmpty()) {
                // Get rates for specific category using InterestRateService
                log.debug("Filtering rates by category code: {}", categoryCode);
                rates = interestRateService.getRatesByCategory(categoryCode.trim());
                
                if (rates.isEmpty()) {
                    log.warn("No interest rates found for category code: {}", categoryCode);
                    return ResponseEntity.notFound().build();
                }
                
                log.info("Successfully retrieved {} interest rates for category {}", 
                        rates.size(), categoryCode);
            } else {
                // Get all current rates using InterestRateService
                log.debug("Retrieving all current interest rates");
                rates = interestRateService.getCurrentRates();
                log.info("Successfully retrieved {} interest rates (all categories)", rates.size());
            }
            
            return ResponseEntity.ok(rates);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid category code in rates request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            log.error("Error retrieving current interest rates: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve current interest rates", e);
        }
    }

    /**
     * Calculates monthly interest for a given account balance and transaction category.
     * 
     * This endpoint implements the core COBOL interest calculation logic from
     * CBACT04C.cbl paragraph 1300-COMPUTE-INTEREST. It uses the exact formula:
     * WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
     * 
     * The calculation maintains BigDecimal precision matching COBOL COMP-3
     * packed decimal behavior to ensure exact financial calculations.
     * 
     * @param request InterestCalculationRequest containing balance and transaction details
     * @return ResponseEntity containing calculated monthly interest amount
     */
    @PostMapping("/calculate")
    public ResponseEntity<InterestCalculationResponse> calculateInterest(
            @Valid @RequestBody InterestCalculationRequest request) {
        
        log.debug("Processing interest calculation request: {}", request);
        
        try {
            // Input validation logging
            log.debug("Calculating interest for balance: {}, type: {}, category: {}, group: {}",
                     request.getBalance(), request.getTransactionTypeCode(),
                     request.getCategoryCode(), request.getAccountGroupId());
            
            // Get effective rate first using InterestCalculationService
            BigDecimal effectiveRate = interestCalculationService.getEffectiveRate(
                request.getAccountGroupId(), 
                request.getTransactionTypeCode(),
                request.getCategoryCode()
            );
            
            log.debug("Effective rate determined: {}", effectiveRate);
            
            // Calculate monthly interest using InterestCalculationService
            // This implements the COBOL formula: WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
            BigDecimal monthlyInterest = interestCalculationService.calculateMonthlyInterest(
                request.getBalance(),
                effectiveRate
            );
            
            // Also calculate daily interest for comparison/validation purposes
            BigDecimal dailyInterest = interestCalculationService.calculateDailyInterest(
                request.getBalance(),
                effectiveRate
            );
            
            log.debug("Calculated monthly interest: {}, daily interest: {}", 
                     monthlyInterest, dailyInterest);
            
            // Build response object
            InterestCalculationResponse response = new InterestCalculationResponse(
                monthlyInterest,
                effectiveRate,
                request.getCategoryCode(),
                request.getBalance()
            );
            
            log.info("Interest calculation completed: monthly interest = {}, effective rate = {}",
                    response.getMonthlyInterest(), response.getEffectiveRate());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid interest calculation request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            log.error("Error calculating interest: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate interest", e);
        }
    }

    /**
     * Retrieves disclosure group information for compliance and rate schedules.
     * 
     * This endpoint provides disclosure group data equivalent to the COBOL
     * DISCGRP file processing from CBACT04C.cbl. It returns structured
     * information about account groups, transaction types, and associated
     * interest rates for regulatory compliance purposes.
     * 
     * @return ResponseEntity containing list of disclosure groups
     */
    @GetMapping("/disclosure-groups")
    public ResponseEntity<List<DisclosureGroupDto>> getDisclosureGroups() {
        log.debug("Processing request for disclosure groups");
        
        try {
            // Delegate to InterestRateService for disclosure group retrieval
            List<DisclosureGroupDto> disclosureGroups = interestRateService.getAllDisclosureGroups();
            
            log.info("Successfully retrieved {} disclosure groups", disclosureGroups.size());
            return ResponseEntity.ok(disclosureGroups);
            
        } catch (Exception e) {
            log.error("Error retrieving disclosure groups: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve disclosure groups", e);
        }
    }

    /**
     * Handles validation exceptions for request parameter validation errors.
     * 
     * This exception handler processes Bean Validation errors that occur
     * when request DTOs fail validation constraints. It provides structured
     * error responses for client applications.
     * 
     * @param ex ValidationException containing validation error details
     * @return ResponseEntity with HTTP 400 Bad Request and error details
     */
    @ExceptionHandler(jakarta.validation.ValidationException.class)
    public ResponseEntity<String> handleValidationException(jakarta.validation.ValidationException ex) {
        log.warn("Validation error in interest calculation: {}", ex.getMessage());
        return ResponseEntity.badRequest()
            .body("Validation error: " + ex.getMessage());
    }

    /**
     * Handles general exceptions that occur during interest operations.
     * 
     * This exception handler provides a safety net for unexpected errors
     * during interest calculation and rate retrieval operations. It ensures
     * graceful error handling and proper HTTP response codes.
     * 
     * @param ex Exception containing error details
     * @return ResponseEntity with HTTP 500 Internal Server Error and error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception ex) {
        log.error("Unexpected error in interest controller: {}", ex.getMessage(), ex);
        return ResponseEntity.status(500)
            .body("Internal server error occurred during interest operation");
    }
}