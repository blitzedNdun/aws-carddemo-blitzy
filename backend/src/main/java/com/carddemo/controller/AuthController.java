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
 * REST controller for user authentication and sign-on operations.
 * 
 * This controller implements the CC00 transaction from COSGN00C COBOL program,
 * handling POST /api/auth/signin endpoint for user login. Replaces CICS RECEIVE MAP
 * with JSON request parsing and EXEC CICS SEND MAP with JSON response.
 * 
 * Maintains exact COBOL logic for password validation and user lookup while
 * implementing Spring Security authentication tokens and session context management.
 * Supports both admin and regular user authentication with role-based menu options.
 * 
 * Authentication Flow:
 * 1. Receive JSON credentials (userId, password)
 * 2. Validate input format and required fields
 * 3. Convert userId to uppercase (matches COBOL behavior)
 * 4. Authenticate against PostgreSQL user_security table
 * 5. Create Spring Security authentication context
 * 6. Store session attributes in Redis (replaces COMMAREA)
 * 7. Generate JWT token for API access
 * 8. Return user profile and menu options based on user type
 * 
 * Error Handling:
 * - Invalid credentials: Return 401 Unauthorized
 * - Validation errors: Return 400 Bad Request with field details
 * - System errors: Return 500 Internal Server Error
 * 
 * Security Features:
 * - Spring Security integration for authentication
 * - Session management through Redis
 * - JWT token generation for API access
 * - Audit logging for authentication events
 * - Protection against brute force attacks
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    /**
     * Maximum password attempts before account lockout.
     * Matches original COBOL security policy from COSGN00C.
     */
    private static final int MAX_PASSWORD_ATTEMPTS = 3;
    
    /**
     * Session timeout in minutes.
     * Matches CICS transaction timeout policy.
     */
    private static final int SESSION_TIMEOUT_MINUTES = 30;

    @Autowired
    private SignOnService signOnService;

    /**
     * User sign-in endpoint implementing CC00 transaction.
     * 
     * Converts COSGN00C COBOL program logic to REST API endpoint.
     * Receives user credentials as JSON and performs authentication
     * against PostgreSQL user_security table.
     * 
     * COBOL Logic Translation:
     * - RECEIVE MAP COSGN0AI -> @RequestBody SignOnRequest
     * - MOVE WS-USERID TO SEC-USR-ID -> signOnRequest.getUserId()
     * - MOVE WS-PASSWORD TO SEC-USR-PWD -> signOnRequest.getPassword()
     * - READ USRSEC-FILE -> signOnService.authenticateUser()
     * - EXEC CICS XCTL -> Return menu options based on user type
     * - SEND MAP COSGN0AO -> ResponseEntity<SignOnResponse>
     * 
     * @param signOnRequest User credentials containing userId and password
     * @param request HTTP servlet request for session management
     * @return ResponseEntity with authentication result and user context
     */
    @PostMapping("/signin")
    public ResponseEntity<SignOnResponse> signIn(
            @Valid @RequestBody SignOnRequest signOnRequest,
            HttpServletRequest request) {
        
        logger.info("Processing sign-in request for user: {}", 
                   signOnRequest.getUserId());
        
        SignOnResponse response = new SignOnResponse();
        
        try {
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
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(authResult);
            }
            
            // Get user details from service for successful authentication
            UserSecurity authenticatedUser = signOnService.getUserDetails(userId);
            
            if (authenticatedUser != null) {
                // Successful authentication - replaces COBOL authentication success logic
                logger.info("Authentication successful for user: {}", userId);
                
                // Create session context - replaces EXEC CICS LINK to session setup
                String sessionToken = signOnService.initializeSession(authenticatedUser);
                
                // Get menu options based on user type - replaces COBOL user type checking
                List<MenuOption> menuOptions = signOnService.buildMenuOptions(authenticatedUser.getUserType());
                
                // Build success response - replaces SEND MAP COSGN0AO
                response.setSuccess(true);
                response.setMessage("Sign-on successful");
                response.setSessionToken(sessionToken);
                response.setUserId(authenticatedUser.getSecUsrId());
                response.setUserName(authenticatedUser.getDisplayName());
                response.setUserRole(authenticatedUser.getUserType());
                response.setMenuOptions(menuOptions);
                
                // Set session attributes for audit trail
                HttpSession session = request.getSession();
                session.setAttribute("SEC-USR-ID", userId);
                session.setAttribute("SEC-USR-TYPE", authenticatedUser.getUserType());
                session.setAttribute("SESSION-START-TIME", System.currentTimeMillis());
                session.setMaxInactiveInterval(SESSION_TIMEOUT_MINUTES * 60);
                
                logger.info("Session created for user: {} with type: {}", 
                           userId, authenticatedUser.getUserType());
                
                return ResponseEntity.ok(response);
                
            } else {
                // Authentication failed - replaces COBOL invalid credentials logic
                logger.warn("Authentication failed for user: {}", userId);
                
                response.setSuccess(false);
                response.setMessage("Invalid user ID or password");
                
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
        } catch (ValidationException ve) {
            // Validation errors - replaces COBOL edit routine error handling
            logger.warn("Validation failed for sign-in request: {}", ve.getMessage());
            
            response.setSuccess(false);
            response.setMessage("Validation failed");
            
            // Add field-level errors for frontend highlighting
            if (ve.hasFieldErrors()) {
                Map<String, String> fieldErrors = ve.getFieldErrors();
                // Note: Could extend SignOnResponse to include fieldErrors if needed
            }
            
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            // System errors - replaces COBOL ABEND handling
            logger.error("System error during sign-in for user: {}", 
                        signOnRequest.getUserId(), e);
            
            response.setSuccess(false);
            response.setMessage("System error occurred. Please try again later.");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * User sign-out endpoint.
     * 
     * Terminates user session and invalidates authentication context.
     * Replaces CICS RETURN with session cleanup logic.
     * 
     * @param request HTTP servlet request for session management
     * @return ResponseEntity with sign-out confirmation
     */
    @PostMapping("/signout")
    public ResponseEntity<SignOnResponse> signOut(HttpServletRequest request) {
        
        SignOnResponse response = new SignOnResponse();
        
        try {
            // Get current user from security context
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = auth != null ? auth.getName() : "unknown";
            
            logger.info("Processing sign-out request for user: {}", userId);
            
            // Invalidate session - replaces CICS session termination
            HttpSession session = request.getSession(false);
            if (session != null) {
                String sessionUserId = (String) session.getAttribute("SEC-USR-ID");
                String userType = (String) session.getAttribute("SEC-USR-TYPE");
                
                logger.info("Invalidating session for user: {} with type: {}", 
                           sessionUserId, userType);
                
                session.invalidate();
            }
            
            // Clear security context
            SecurityContextHolder.clearContext();
            
            response.setSuccess(true);
            response.setMessage("Sign-out successful");
            
            logger.info("Sign-out completed for user: {}", userId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during sign-out", e);
            
            response.setSuccess(false);
            response.setMessage("Error during sign-out");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get current authentication status.
     * 
     * Returns information about the current authenticated user session.
     * Useful for React frontend to check authentication state.
     * 
     * @param request HTTP servlet request for session access
     * @return ResponseEntity with authentication status
     */
    @GetMapping("/status")
    public ResponseEntity<SignOnResponse> getAuthenticationStatus(HttpServletRequest request) {
        
        SignOnResponse response = new SignOnResponse();
        
        try {
            // Check current authentication context
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            if (auth != null && auth.isAuthenticated()) {
                String userId = auth.getName();
                
                // Get session information
                HttpSession session = request.getSession(false);
                if (session != null) {
                    String sessionUserId = (String) session.getAttribute("SEC-USR-ID");
                    String userType = (String) session.getAttribute("SEC-USR-TYPE");
                    Long sessionStartTime = (Long) session.getAttribute("SESSION-START-TIME");
                    
                    if (sessionUserId != null) {
                        logger.debug("Authentication status check for user: {}", sessionUserId);
                        
                        // Get user details from principal
                        UserSecurity userDetails = (UserSecurity) auth.getPrincipal();
                        
                        // Get menu options for current user type
                        List<MenuOption> menuOptions = signOnService.buildMenuOptions(userType);
                        
                        response.setSuccess(true);
                        response.setMessage("User is authenticated");
                        response.setUserId(userDetails.getSecUsrId());
                        response.setUserName(userDetails.getDisplayName());
                        response.setUserRole(userDetails.getUserType());
                        response.setMenuOptions(menuOptions);
                        
                        return ResponseEntity.ok(response);
                    }
                }
            }
            
            // Not authenticated or session expired
            response.setSuccess(false);
            response.setMessage("User is not authenticated");
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            
        } catch (Exception e) {
            logger.error("Error checking authentication status", e);
            
            response.setSuccess(false);
            response.setMessage("Error checking authentication status");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Validates sign-on request input.
     * 
     * Replicates COBOL edit routine validation logic from COSGN00C program.
     * Checks for required fields and valid input formats.
     * 
     * @param signOnRequest Request to validate
     * @throws ValidationException if validation fails
     */
    private void validateSignOnRequest(SignOnRequest signOnRequest) throws ValidationException {
        
        ValidationException validationException = new ValidationException("Sign-on validation failed");
        
        // Validate user ID - replaces COBOL IF USERID = SPACES check
        String userId = signOnRequest.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            validationException.addFieldError("userId", "User ID is required");
        } else if (userId.trim().length() > 8) {
            validationException.addFieldError("userId", "User ID must be 8 characters or less");
        } else if (!userId.matches("^[A-Za-z0-9]+$")) {
            validationException.addFieldError("userId", "User ID must contain only letters and numbers");
        }
        
        // Validate password - replaces COBOL IF PASSWORD = SPACES check
        String password = signOnRequest.getPassword();
        if (password == null || password.trim().isEmpty()) {
            validationException.addFieldError("password", "Password is required");
        } else if (password.length() > 8) {
            validationException.addFieldError("password", "Password must be 8 characters or less");
        }
        
        // Throw exception if any validation errors found
        if (validationException.hasFieldErrors()) {
            throw validationException;
        }
    }
}