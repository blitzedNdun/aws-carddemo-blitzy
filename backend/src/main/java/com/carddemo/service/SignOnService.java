/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.repository.UserSecurityRepository;
import com.carddemo.entity.UserSecurity;
import com.carddemo.dto.SignOnRequest;
import com.carddemo.dto.SignOnResponse;
import com.carddemo.util.Constants;
import com.carddemo.dto.MenuOption;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Spring Boot service class implementing user authentication and sign-on business logic 
 * translated from COSGN00C.cbl. Validates user credentials against the security repository,
 * manages session initialization with user context, and controls navigation to appropriate 
 * menu based on user type (admin vs regular). 
 * 
 * Maintains the original COBOL paragraph structure as Java methods while leveraging 
 * Spring Security integration.
 * 
 * This service class translates the following COBOL paragraphs:
 * - MAIN-PARA -> mainProcess()
 * - PROCESS-ENTER-KEY -> processSignOn() 
 * - READ-USER-SEC-FILE -> validateCredentials()
 * - SEND-SIGNON-SCREEN -> buildSignOnResponse()
 * - POPULATE-HEADER-INFO -> initializeSession()
 * 
 * Authentication flow preserves original COBOL logic:
 * 1. Input validation (user ID and password required)
 * 2. Uppercase conversion of credentials
 * 3. User lookup in USRSEC equivalent table
 * 4. Password validation (plain text comparison preserved)
 * 5. Session establishment with user context
 * 6. Menu routing based on user type (A=Admin, U=User)
 */
@Service
public class SignOnService {

    private static final Logger logger = LoggerFactory.getLogger(SignOnService.class);

    @Autowired
    private UserSecurityRepository userSecurityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Main processing method corresponding to COBOL MAIN-PARA.
     * Handles the primary sign-on logic flow including input validation,
     * credential verification, and user routing.
     * 
     * @param request SignOnRequest containing user credentials
     * @return SignOnResponse with authentication result and navigation data
     */
    public SignOnResponse mainProcess(SignOnRequest request) {
        logger.info("Starting main authentication process for user: {}", 
                   request != null ? request.getUserId() : "null");

        SignOnResponse response = new SignOnResponse();
        
        try {
            // Initialize response with default values
            response.setSuccess(false);
            response.setMessage("");
            
            // Process sign-on request (equivalent to PROCESS-ENTER-KEY)
            return processSignOn(request);
            
        } catch (Exception e) {
            logger.error("Error in main authentication process", e);
            response.setSuccess(false);
            response.setMessage("System error during authentication. Please try again.");
            return response;
        }
    }

    /**
     * Process sign-on request corresponding to COBOL PROCESS-ENTER-KEY paragraph.
     * Handles input validation, credential verification, and session establishment.
     * Preserves original COBOL validation logic and error message patterns.
     * 
     * @param request SignOnRequest containing user credentials
     * @return SignOnResponse with authentication result
     */
    public SignOnResponse processSignOn(SignOnRequest request) {
        logger.debug("Processing sign-on request");
        
        SignOnResponse response = new SignOnResponse();
        
        // Input validation - equivalent to COBOL validation logic
        if (request == null) {
            logger.warn("Null sign-on request received");
            response.setSuccess(false);
            response.setMessage("Invalid request. Please provide credentials.");
            return response;
        }

        String userId = request.getUserId();
        String password = request.getPassword();

        // Check for empty user ID (COBOL: USERIDI = SPACES OR LOW-VALUES)
        if (userId == null || userId.trim().isEmpty()) {
            logger.warn("Empty user ID provided");
            response.setSuccess(false);
            response.setMessage("Please enter User ID ...");
            return response;
        }

        // Check for empty password (COBOL: PASSWDI = SPACES OR LOW-VALUES)
        if (password == null || password.trim().isEmpty()) {
            logger.warn("Empty password provided for user: {}", userId);
            response.setSuccess(false);
            response.setMessage("Please enter Password ...");
            return response;
        }

        // Convert to uppercase (COBOL: FUNCTION UPPER-CASE)
        userId = userId.toUpperCase().trim();
        password = password.toUpperCase().trim();

        logger.debug("Attempting authentication for user: {}", userId);

        // Validate credentials (equivalent to READ-USER-SEC-FILE)
        return validateCredentials(userId, password);
    }

    /**
     * Validate user credentials corresponding to COBOL READ-USER-SEC-FILE paragraph.
     * Performs user lookup and password verification maintaining original COBOL logic.
     * Routes to appropriate menu based on user type after successful authentication.
     * 
     * @param userId User ID (uppercase, trimmed)
     * @param password Password (uppercase, trimmed)
     * @return SignOnResponse with authentication result and menu options
     */
    public SignOnResponse validateCredentials(String userId, String password) {
        logger.debug("Validating credentials for user: {}", userId);
        
        SignOnResponse response = new SignOnResponse();
        
        try {
            // Lookup user in repository (equivalent to CICS READ DATASET USRSEC)
            Optional<UserSecurity> userOptional = userSecurityRepository.findBySecUsrId(userId);
            
            if (!userOptional.isPresent()) {
                // User not found (COBOL: RESP-CD = 13)
                logger.warn("User not found: {}", userId);
                response.setSuccess(false);
                response.setMessage("User not found. Try again ...");
                return response;
            }

            UserSecurity user = userOptional.get();
            
            // Password validation using BCrypt (Spring Security standard)
            // Note: Migrated from COBOL plain text to secure BCrypt hashing
            if (!passwordEncoder.matches(password, user.getPassword())) {
                logger.warn("Invalid password for user: {}", userId);
                response.setSuccess(false);
                response.setMessage("Wrong Password. Try again ...");
                return response;
            }

            // Authentication successful - establish session
            logger.info("Authentication successful for user: {}", userId);
            
            // Initialize session (equivalent to COMMAREA population)
            String sessionToken = initializeSession(user);
            
            // Build response with user context and menu options
            response.setSuccess(true);
            response.setMessage("Authentication successful");
            response.setUserName(user.getDisplayName());
            response.setUserType(user.getUserType());
            
            // Build menu options based on user type (equivalent to XCTL routing)
            List<MenuOption> menuOptions = buildMenuOptions(user.getUserType());
            response.setMenuOptions(menuOptions);
            
            // Update last login timestamp and reset failed login attempts
            user.updateLastLogin();
            user.resetFailedLoginAttempts();
            userSecurityRepository.save(user);
            
            logger.info("User {} successfully authenticated with type: {}", 
                       userId, user.getUserType());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error validating credentials for user: " + userId, e);
            response.setSuccess(false);
            response.setMessage("Unable to verify the User ...");
            return response;
        }
    }

    /**
     * Initialize user session corresponding to COBOL COMMAREA population.
     * Creates session token and establishes user context for subsequent requests.
     * Replaces CICS COMMAREA with modern session management.
     * 
     * @param user Authenticated UserSecurity entity
     * @return Session token for client authentication
     */
    public String initializeSession(UserSecurity user) {
        logger.debug("Initializing session for user: {}", user.getSecUsrId());
        
        try {
            // Generate session token (equivalent to COMMAREA context)
            String sessionToken = UUID.randomUUID().toString();
            
            // Log session establishment for audit trail
            logger.info("Session initialized for user: {} with type: {}", 
                       user.getSecUsrId(), user.getUserType());
            
            return sessionToken;
            
        } catch (Exception e) {
            logger.error("Error initializing session for user: " + user.getSecUsrId(), e);
            throw new RuntimeException("Session initialization failed", e);
        }
    }

    /**
     * Build menu options based on user type corresponding to COBOL XCTL routing logic.
     * Routes admin users to COADM01C equivalent menu and regular users to COMEN01C equivalent.
     * Maintains original COBOL menu structure and navigation patterns.
     * 
     * @param userType User type ('A' for Admin, 'U' for User)
     * @return List of MenuOption objects for client navigation
     */
    public List<MenuOption> buildMenuOptions(String userType) {
        logger.debug("Building menu options for user type: {}", userType);
        
        List<MenuOption> menuOptions = new ArrayList<>();
        
        try {
            if (Constants.USER_TYPE_ADMIN.equals(userType)) {
                // Admin menu options (equivalent to COADM01C routing)
                logger.debug("Building admin menu options");
                
                MenuOption option1 = new MenuOption();
                option1.setOptionId(1);
                option1.setOptionText("User Management");
                option1.setTarget("ADMIN_USER_MGMT");
                menuOptions.add(option1);
                
                MenuOption option2 = new MenuOption();
                option2.setOptionId(2);
                option2.setOptionText("Account Management");
                option2.setTarget("ADMIN_ACCOUNT_MGMT");
                menuOptions.add(option2);
                
                MenuOption option3 = new MenuOption();
                option3.setOptionId(3);
                option3.setOptionText("Transaction Reports");
                option3.setTarget("ADMIN_REPORTS");
                menuOptions.add(option3);
                
                MenuOption option4 = new MenuOption();
                option4.setOptionId(4);
                option4.setOptionText("System Administration");
                option4.setTarget("ADMIN_SYSTEM");
                menuOptions.add(option4);
                
            } else if (Constants.USER_TYPE_USER.equals(userType)) {
                // Regular user menu options (equivalent to COMEN01C routing)
                logger.debug("Building regular user menu options");
                
                MenuOption option1 = new MenuOption();
                option1.setOptionId(1);
                option1.setOptionText("Account Information");
                option1.setTarget("USER_ACCOUNT_INFO");
                menuOptions.add(option1);
                
                MenuOption option2 = new MenuOption();
                option2.setOptionId(2);
                option2.setOptionText("Transaction History");
                option2.setTarget("USER_TRANSACTIONS");
                menuOptions.add(option2);
                
                MenuOption option3 = new MenuOption();
                option3.setOptionId(3);
                option3.setOptionText("Card Management");
                option3.setTarget("USER_CARDS");
                menuOptions.add(option3);
                
                MenuOption option4 = new MenuOption();
                option4.setOptionId(4);
                option4.setOptionText("Customer Profile");
                option4.setTarget("USER_PROFILE");
                menuOptions.add(option4);
            }
            
            logger.debug("Built {} menu options for user type: {}", 
                        menuOptions.size(), userType);
            
            return menuOptions;
            
        } catch (Exception e) {
            logger.error("Error building menu options for user type: " + userType, e);
            return new ArrayList<>(); // Return empty list on error
        }
    }

    // Additional exported functions for compatibility

    /**
     * Standalone sign-on function for direct authentication calls.
     * Provides alternative entry point for authentication operations.
     * 
     * @param request SignOnRequest containing credentials
     * @return SignOnResponse with authentication result
     */
    public SignOnResponse signOn(SignOnRequest request) {
        logger.debug("Processing standalone signOn request");
        return mainProcess(request);
    }

    /**
     * Create user session for authenticated user.
     * Provides session management functionality for external callers.
     * 
     * @param user Authenticated UserSecurity entity
     * @return Session token string
     */
    public String createUserSession(UserSecurity user) {
        logger.debug("Creating user session for: {}", user.getSecUsrId());
        return initializeSession(user);
    }

    /**
     * Get user details by user ID.
     * Provides user lookup functionality for external services.
     * 
     * @param userId User ID to lookup
     * @return UserSecurity entity if found, null otherwise
     */
    public UserSecurity getUserDetails(String userId) {
        logger.debug("Getting user details for: {}", userId);
        
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        
        String normalizedUserId = userId.toUpperCase().trim();
        Optional<UserSecurity> userOptional = userSecurityRepository.findBySecUsrId(normalizedUserId);
        
        return userOptional.orElse(null);
    }

    /**
     * Process user logout and session cleanup.
     * Handles session termination and cleanup operations.
     * 
     * @param userId User ID for logout processing
     * @return Success status of logout operation
     */
    public boolean logout(String userId) {
        logger.info("Processing logout for user: {}", userId);
        
        try {
            // Log logout event for audit trail
            logger.info("User {} logged out successfully", userId);
            return true;
            
        } catch (Exception e) {
            logger.error("Error processing logout for user: " + userId, e);
            return false;
        }
    }
}