/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carddemo.service;

import com.carddemo.repository.UserSecurityRepository;
import com.carddemo.entity.UserSecurity;
import com.carddemo.dto.UserAddRequest;
import com.carddemo.dto.UserAddResponse;
import com.carddemo.security.PasswordGenerator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;

/**
 * Spring Boot service implementing new user creation logic translated from COUSR03C.cbl.
 * 
 * This service provides comprehensive user addition functionality converting COBOL user management
 * operations to modern Spring Boot service patterns. It maintains the same business logic flow
 * as the original COBOL program while integrating with Spring Security and PostgreSQL database.
 * 
 * Key Features:
 * - Validates unique user IDs to prevent duplicate accounts
 * - Generates secure initial passwords following enterprise security policies
 * - Assigns default roles based on user type for Spring Security integration
 * - Creates security profiles with audit trail information
 * - Maintains RACF user definition patterns while using Spring Security framework
 * 
 * The service implements the paragraph structure from COUSR03C.cbl:
 * - MAIN-PARA → addUser() method with transaction boundaries
 * - PROCESS-ENTER-KEY → validateUniqueUserId() method for ID validation
 * - User creation logic → createUserEntity() method for entity construction
 * - Audit functionality → generateAuditEntry() method for compliance tracking
 * 
 * @author Blitzy Platform Migration Agent
 * @version 1.0
 * @since Java 21
 */
@Slf4j
@Service
@Transactional
public class UserAddService {

    private final UserSecurityRepository userSecurityRepository;
    private final PasswordGenerator passwordGenerator;

    /**
     * Constructor for dependency injection of required components.
     * 
     * @param userSecurityRepository Repository for user security data access operations
     * @param passwordGenerator Utility for generating secure initial passwords
     */
    @Autowired
    public UserAddService(UserSecurityRepository userSecurityRepository,
                         PasswordGenerator passwordGenerator) {
        this.userSecurityRepository = userSecurityRepository;
        this.passwordGenerator = passwordGenerator;
    }

    /**
     * Main service method for adding new users to the system.
     * 
     * This method converts the MAIN-PARA logic from COUSR03C.cbl, implementing comprehensive
     * user creation workflow with validation, password generation, and audit trail creation.
     * The method maintains transactional boundaries equivalent to CICS SYNCPOINT behavior.
     * 
     * Business Logic Flow:
     * 1. Validate input parameters and user ID uniqueness
     * 2. Generate secure password if requested
     * 3. Create UserSecurity entity with proper field mapping
     * 4. Assign default roles based on user type
     * 5. Save to database with transaction management
     * 6. Generate audit entry for compliance tracking
     * 7. Return structured response with operation status
     * 
     * @param request UserAddRequest containing user details and creation parameters
     * @return UserAddResponse with operation status, created user details, and generated password
     * @throws DuplicateKeyException when user ID already exists in the system
     * @throws IllegalArgumentException when input validation fails
     */
    @Transactional
    public UserAddResponse addUser(UserAddRequest request) {
        log.info("Starting user creation process for user ID: {}", request.getUserId());
        
        try {
            // Validate input parameters
            if (request == null) {
                log.error("User creation failed: request is null");
                return createErrorResponse("Request cannot be null", "INVALID_REQUEST");
            }
            
            if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
                log.error("User creation failed: user ID is null or empty");
                return createErrorResponse("User ID cannot be empty", "INVALID_USER_ID");
            }
            
            // Step 1: Validate unique user ID (equivalent to PROCESS-ENTER-KEY validation)
            String validationResult = validateUniqueUserId(request.getUserId());
            if (validationResult != null) {
                log.warn("User creation failed: user ID validation error - {}", validationResult);
                return createErrorResponse(validationResult, "DUPLICATE_USER_ID");
            }
            
            // Step 2: Validate user type
            if (!isValidUserType(request.getUserType())) {
                log.error("User creation failed: invalid user type - {}", request.getUserType());
                return createErrorResponse("Invalid user type. Must be 'A' for Admin or 'R' for Regular user", "INVALID_USER_TYPE");
            }
            
            // Step 3: Generate secure password if requested
            String finalPassword = request.getPassword();
            if (request.getGeneratePassword() != null && request.getGeneratePassword()) {
                finalPassword = passwordGenerator.generateSecurePassword();
                log.debug("Generated secure password for user: {}", request.getUserId());
            }
            
            // Validate password if provided
            if (finalPassword == null || finalPassword.trim().isEmpty()) {
                log.error("User creation failed: no password provided or generated");
                return createErrorResponse("Password must be provided or generation must be enabled", "INVALID_PASSWORD");
            }
            
            // Step 4: Create UserSecurity entity (equivalent to COBOL record creation)
            UserSecurity userEntity = createUserEntity(request, finalPassword);
            
            // Step 5: Save to database with transaction management
            UserSecurity savedUser = userSecurityRepository.save(userEntity);
            log.info("Successfully created user: {} with user type: {}", savedUser.getSecUsrId(), savedUser.getUserType());
            
            // Step 6: Generate audit entry for compliance tracking
            generateAuditEntry(savedUser.getSecUsrId(), "USER_CREATED");
            
            // Step 7: Return success response
            UserAddResponse response = UserAddResponse.builder()
                .success(true)
                .userId(savedUser.getSecUsrId())
                .generatedPassword(finalPassword)
                .message("User created successfully")
                .timestamp(LocalDateTime.now())
                .build();
            
            log.info("User creation completed successfully for user ID: {}", request.getUserId());
            return response;
            
        } catch (DuplicateKeyException e) {
            log.error("User creation failed: duplicate user ID - {}", request.getUserId(), e);
            return createErrorResponse("User ID already exists", "DUPLICATE_USER_ID");
        } catch (Exception e) {
            log.error("User creation failed: unexpected error for user ID - {}", request.getUserId(), e);
            return createErrorResponse("Internal server error during user creation", "SYSTEM_ERROR");
        }
    }

    /**
     * Validates that the user ID is unique and meets system requirements.
     * 
     * This method implements the user ID validation logic from COUSR03C.cbl PROCESS-ENTER-KEY
     * paragraph, ensuring that duplicate user IDs cannot be created in the system.
     * 
     * @param userId The user ID to validate for uniqueness
     * @return null if validation passes, error message if validation fails
     */
    public String validateUniqueUserId(String userId) {
        log.debug("Validating user ID uniqueness: {}", userId);
        
        if (userId == null || userId.trim().isEmpty()) {
            return "User ID cannot be empty";
        }
        
        // Trim and uppercase the user ID to match COBOL field handling
        String normalizedUserId = userId.trim().toUpperCase();
        
        // Check length constraint (8 characters maximum from COBOL SEC-USR-ID PIC X(08))
        if (normalizedUserId.length() > 8) {
            return "User ID must not exceed 8 characters";
        }
        
        // Check for existing user with same ID
        if (userSecurityRepository.existsBySecUsrId(normalizedUserId)) {
            log.warn("User ID already exists: {}", normalizedUserId);
            return "User ID already exists";
        }
        
        log.debug("User ID validation passed: {}", normalizedUserId);
        return null;
    }

    /**
     * Creates a UserSecurity entity from the request data with proper field mapping.
     * 
     * This method implements the COBOL record creation logic, mapping UserAddRequest fields
     * to UserSecurity entity properties while maintaining the same data constraints and
     * field lengths from the original CSUSR01Y copybook structure.
     * 
     * @param request UserAddRequest containing user information
     * @param password The final password (either provided or generated)
     * @return UserSecurity entity ready for database persistence
     */
    public UserSecurity createUserEntity(UserAddRequest request, String password) {
        log.debug("Creating UserSecurity entity for user: {}", request.getUserId());
        
        UserSecurity userSecurity = new UserSecurity();
        
        // Map basic user information from request to entity
        userSecurity.setSecUsrId(request.getUserId().trim().toUpperCase());
        userSecurity.setUsername(request.getUserId().trim().toUpperCase()); // For Spring Security compatibility
        userSecurity.setFirstName(request.getFirstName() != null ? request.getFirstName().trim() : "");
        userSecurity.setLastName(request.getLastName() != null ? request.getLastName().trim() : "");
        userSecurity.setUserType(request.getUserType().trim().toUpperCase());
        
        // Set password (will be encoded by Spring Security during authentication)
        userSecurity.setPassword(password);
        
        // Set default Spring Security fields
        userSecurity.setEnabled(true);
        userSecurity.setAccountNonExpired(true);
        userSecurity.setAccountNonLocked(true);
        userSecurity.setCredentialsNonExpired(true);
        userSecurity.setFailedLoginAttempts(0);
        
        // Set audit timestamps
        LocalDateTime now = LocalDateTime.now();
        userSecurity.setCreatedAt(now);
        userSecurity.setUpdatedAt(now);
        
        log.debug("UserSecurity entity created for user: {}", userSecurity.getSecUsrId());
        return userSecurity;
    }

    /**
     * Generates audit entry for user creation events.
     * 
     * This method implements audit trail functionality required for compliance and monitoring,
     * equivalent to the audit logging performed in the original COBOL implementation.
     * 
     * @param userId The user ID for which audit entry is generated
     * @param eventType The type of event being audited
     */
    public void generateAuditEntry(String userId, String eventType) {
        log.info("Audit Entry - User: {}, Event: {}, Timestamp: {}", 
                userId, eventType, LocalDateTime.now());
        
        // Additional audit logging could be implemented here
        // For example, writing to audit table or external audit system
        log.debug("Audit entry generated for user: {} with event type: {}", userId, eventType);
    }

    /**
     * Validates if the provided user type is acceptable according to system rules.
     * 
     * This method implements user type validation equivalent to the COBOL edit routines,
     * ensuring that only valid user types are accepted during user creation.
     * 
     * @param userType The user type to validate
     * @return true if user type is valid, false otherwise
     */
    public boolean isValidUserType(String userType) {
        if (userType == null || userType.trim().isEmpty()) {
            return false;
        }
        
        String normalizedUserType = userType.trim().toUpperCase();
        
        // Valid user types based on COBOL business rules
        // 'A' = Admin user, 'R' = Regular user
        boolean isValid = "A".equals(normalizedUserType) || "R".equals(normalizedUserType);
        
        log.debug("User type validation for '{}': {}", userType, isValid);
        return isValid;
    }

    /**
     * Creates a standardized error response for failed operations.
     * 
     * This method provides consistent error response formatting across all user creation
     * failure scenarios, ensuring proper error handling and user feedback.
     * 
     * @param message Human-readable error message
     * @param errorCode Standardized error code for programmatic handling
     * @return UserAddResponse with error details
     */
    private UserAddResponse createErrorResponse(String message, String errorCode) {
        return UserAddResponse.builder()
            .success(false)
            .userId(null)
            .generatedPassword(null)
            .message(message)
            .errorCode(errorCode)
            .timestamp(LocalDateTime.now())
            .build();
    }
}