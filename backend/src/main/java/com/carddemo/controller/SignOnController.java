/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.controller;

import com.carddemo.dto.SignOnRequest;
import com.carddemo.dto.SignOnResponse;
import com.carddemo.service.SignOnService;
import com.carddemo.entity.UserSecurity;
import com.carddemo.exception.ValidationException;
import com.carddemo.dto.MenuOption;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for CICS CC00 transaction sign-on operations.
 * 
 * This controller implements the CC00 transaction from COSGN00C COBOL program,
 * handling POST /api/tx/CC00 endpoint for user authentication. Maps exactly to the
 * original CICS transaction code while providing modern REST API functionality.
 * 
 * Maintains exact COBOL logic for password validation and user lookup while
 * implementing Spring Security authentication tokens and session context management.
 * Supports both admin and regular user authentication with role-based menu options.
 * 
 * Transaction Flow (CC00 - COSGN00C equivalent):
 * 1. Receive JSON credentials (userId, password) via POST
 * 2. Validate input format and required fields
 * 3. Convert userId to uppercase (matches COBOL behavior)
 * 4. Authenticate against PostgreSQL user_security table
 * 5. Initialize session context (replaces CICS COMMAREA)
 * 6. Return response with user details and menu options
 * 
 * This controller specifically handles the CICS CC00 transaction mapping as required
 * by the COBOL-to-Java migration specifications, providing exact functional parity
 * with the original COSGN00C.cbl program behavior.
 * 
 * Key COBOL mappings:
 * - EXEC CICS RECEIVE MAP → @PostMapping JSON request processing
 * - EXEC CICS SEND MAP → ResponseEntity JSON response
 * - COMMAREA population → Spring Session management
 * - XCTL to menu programs → MenuOption response objects
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/tx")
public class SignOnController {

    private static final Logger logger = LoggerFactory.getLogger(SignOnController.class);

    @Autowired
    private SignOnService signOnService;

    /**
     * CC00 Transaction - User Sign-On Processing
     * 
     * Implements the exact functionality of COSGN00C COBOL program through REST API.
     * Handles user authentication, session establishment, and menu routing based on
     * user type. Maintains all original COBOL validation rules and error messages.
     * 
     * Original COBOL Flow:
     * - MAIN-PARA: Main processing entry point
     * - PROCESS-ENTER-KEY: Input validation and authentication trigger
     * - READ-USER-SEC-FILE: User lookup and password validation
     * - POPULATE-HEADER-INFO: Session establishment and context population
     * - Menu routing via XCTL based on user type
     * 
     * Modern Implementation:
     * - JSON request parsing replaces CICS RECEIVE MAP
     * - Spring Security integration for authentication
     * - Session token generation replaces COMMAREA
     * - JSON response with menu options replaces XCTL routing
     * 
     * @param signOnRequest User credentials from client (JSON)
     * @param request HTTP servlet request for session management
     * @return ResponseEntity with authentication result and user context
     */
    @PostMapping("/CC00")
    public ResponseEntity<SignOnResponse> processCC00Transaction(
            @Valid @RequestBody SignOnRequest signOnRequest,
            HttpServletRequest request) {
        
        logger.info("Processing CC00 sign-on transaction for user: {}", 
                   signOnRequest.getUserId());
        
        SignOnResponse response = new SignOnResponse();
        
        try {
            // Set transaction context information
            response.setTransactionCode("CC00");
            response.setProgramName("COSGN00C");
            response.setApplicationId("CARDDEMO");
            response.setSystemId("CARDDEMO");
            
            // Input validation - replaces COBOL edit routine logic
            validateSignOnRequest(signOnRequest);
            
            // Convert to uppercase - matches COBOL FUNCTION UPPER-CASE behavior
            String userId = signOnRequest.getUserId().toUpperCase();
            String password = signOnRequest.getPassword();
            
            // Authenticate user - replaces READ USRSEC-FILE logic
            SignOnResponse authResult = signOnService.validateCredentials(userId, password);
            
            // Check if authentication was successful
            if (!"SUCCESS".equals(authResult.getStatus())) {
                // Authentication failed - return the error response from service
                response.setStatus("ERROR");
                response.setErrorMessage(authResult.getErrorMessage());
                response.setTransactionCode("CC00");
                response.setProgramName("COSGN00C");
                
                // Map COBOL error messages to appropriate HTTP status
                if ("User not found. Try again ...".equals(authResult.getErrorMessage())) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                } else if ("Wrong Password. Try again ...".equals(authResult.getErrorMessage())) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
            }
            
            // Get user details from service for successful authentication
            UserSecurity authenticatedUser = signOnService.getUserDetails(userId);
            
            if (authenticatedUser != null) {
                // Successful authentication - replaces COBOL authentication success logic
                logger.info("CC00 authentication successful for user: {}", userId);
                
                // Create session context - replaces EXEC CICS LINK to session setup
                String sessionToken = signOnService.initializeSession(authenticatedUser);
                
                // Get menu options based on user type - replaces COBOL user type checking
                List<MenuOption> menuOptions = signOnService.buildMenuOptions(authenticatedUser.getUserType());
                
                // Populate successful response - replaces SEND MAP logic
                response.setStatus("SUCCESS");
                response.setUserId(userId);
                response.setUserName(authenticatedUser.getDisplayName());
                response.setUserRole(authenticatedUser.getUserType());
                response.setSessionToken(sessionToken);
                response.setMenuOptions(menuOptions);
                response.setTransactionCode("CC00");
                response.setProgramName("COSGN00C");
                response.setApplicationId("CARDDEMO");
                response.setSystemId("CARDDEMO");
                
                logger.info("CC00 transaction completed successfully for user: {} with role: {}", 
                           userId, authenticatedUser.getUserType());
                
                return ResponseEntity.ok(response);
                
            } else {
                // System error - user authentication succeeded but user details not found
                logger.error("System error: User authenticated but details not found for: {}", userId);
                response.setStatus("ERROR");
                response.setErrorMessage("System error. Please try again later.");
                response.setTransactionCode("CC00");
                response.setProgramName("COSGN00C");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (ValidationException e) {
            // Validation error - replaces COBOL field validation error handling
            logger.warn("CC00 validation error for user {}: {}", 
                       signOnRequest.getUserId(), e.getMessage());
            response.setStatus("ERROR");
            response.setErrorMessage(e.getMessage());
            response.setTransactionCode("CC00");
            response.setProgramName("COSGN00C");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            
        } catch (Exception e) {
            // System error - replaces COBOL ABEND handling
            logger.error("CC00 system error for user: " + signOnRequest.getUserId(), e);
            response.setStatus("ERROR");
            response.setErrorMessage("System error during sign-on. Please try again.");
            response.setTransactionCode("CC00");
            response.setProgramName("COSGN00C");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Validates sign-on request input fields.
     * Replicates COBOL input validation logic with appropriate error messages.
     * 
     * @param request SignOnRequest to validate
     * @throws ValidationException if validation fails
     */
    private void validateSignOnRequest(SignOnRequest request) throws ValidationException {
        if (request == null) {
            throw new ValidationException("Invalid request. Please provide credentials.");
        }
        
        // Check for empty user ID (COBOL: USERIDI = SPACES OR LOW-VALUES)
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            throw new ValidationException("Please enter User ID ...");
        }
        
        // Check for empty password (COBOL: PASSWDI = SPACES OR LOW-VALUES)
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new ValidationException("Please enter Password ...");
        }
        
        // Additional validation for field lengths (matches COBOL PIC X(8) constraints)
        if (request.getUserId().length() > 8) {
            throw new ValidationException("User ID cannot exceed 8 characters");
        }
        
        if (request.getPassword().length() > 8) {
            throw new ValidationException("Password cannot exceed 8 characters");
        }
    }

    /**
     * Health check endpoint for CC00 transaction processing.
     * Provides status information for monitoring and debugging.
     * 
     * @return ResponseEntity with transaction status information
     */
    @GetMapping("/CC00/status")
    public ResponseEntity<Map<String, String>> getCC00Status() {
        logger.debug("CC00 status check requested");
        
        Map<String, String> status = new HashMap<>();
        status.put("transaction", "CC00");
        status.put("program", "COSGN00C");
        status.put("status", "ACTIVE");
        status.put("description", "Sign-On Transaction Processing");
        
        return ResponseEntity.ok(status);
    }
}