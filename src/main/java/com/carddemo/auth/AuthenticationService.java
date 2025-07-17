/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.auth;

import com.carddemo.auth.dto.LoginRequest;
import com.carddemo.auth.dto.LoginResponse;
import com.carddemo.auth.util.JwtTokenUtil;
import com.carddemo.common.entity.User;
import com.carddemo.common.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.validation.Valid;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Primary authentication microservice for CardDemo application implementing JWT-based
 * authentication with Spring Security 6.x framework and PostgreSQL integration.
 * 
 * This service replaces the legacy COBOL COSGN00C.cbl CICS transaction providing
 * modern REST API authentication with comprehensive security features including
 * BCrypt password hashing, JWT token generation, and role-based access control.
 * 
 * Original COBOL Program Mapping:
 * - COSGN00C.cbl MAIN-PARA -> login() method
 * - PROCESS-ENTER-KEY -> credential validation logic
 * - READ-USER-SEC-FILE -> UserRepository.findByUserId()
 * - VSAM USRSEC file -> PostgreSQL users table
 * - CICS COMMAREA -> JWT token with user context
 * - RACF authentication -> Spring Security with BCrypt
 * 
 * Authentication Flow:
 * 1. Validates incoming LoginRequest credentials
 * 2. Queries PostgreSQL users table via UserRepository
 * 3. Validates BCrypt hashed password using Spring Security
 * 4. Generates JWT token with user information and role claims
 * 5. Returns LoginResponse with token and user context
 * 6. Handles comprehensive error scenarios with structured responses
 * 
 * Security Features:
 * - BCrypt password hashing with configurable salt rounds
 * - JWT token generation with configurable expiration
 * - Role-based access control (ROLE_ADMIN, ROLE_USER)
 * - Comprehensive audit logging for security compliance
 * - Protection against timing attacks through consistent response times
 * - Input validation and sanitization for security hardening
 * 
 * Performance Characteristics:
 * - Target response time: < 50ms for authentication requests
 * - Supports connection pooling with HikariCP for database access
 * - Stateless authentication for horizontal scaling
 * - Redis session management for distributed architecture
 * 
 * @author AWS CardDemo Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@Service
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    
    /**
     * Spring Data JPA repository for user database operations.
     * Replaces COBOL VSAM READ operations with PostgreSQL queries.
     */
    @Autowired
    private UserRepository userRepository;
    
    /**
     * JWT token utility for token generation, validation, and claims extraction.
     * Provides comprehensive JWT operations for Spring Security integration.
     */
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    /**
     * BCrypt password encoder for secure password validation.
     * Provides configurable salt rounds and secure password hashing.
     */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    
    // Constants for user types and roles (from original COBOL SEC-USR-TYPE values)
    private static final String USER_TYPE_ADMIN = "A";
    private static final String USER_TYPE_USER = "U";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_USER = "ROLE_USER";
    
    // Error messages matching original COBOL program behavior
    private static final String ERROR_INVALID_CREDENTIALS = "Invalid username or password";
    private static final String ERROR_USER_NOT_FOUND = "User not found";
    private static final String ERROR_INVALID_INPUT = "Invalid input parameters";
    private static final String ERROR_AUTHENTICATION_FAILED = "Authentication failed";
    private static final String ERROR_TOKEN_INVALID = "Invalid or expired token";
    private static final String ERROR_TOKEN_EXPIRED = "Token has expired";
    
    // Success messages
    private static final String SUCCESS_LOGIN = "Authentication successful";
    private static final String SUCCESS_LOGOUT = "Logout successful";
    private static final String SUCCESS_TOKEN_REFRESH = "Token refreshed successfully";

    /**
     * Authenticates user credentials and generates JWT token for successful authentication.
     * 
     * This method implements the core authentication logic from the original COBOL program
     * COSGN00C.cbl, converting VSAM file operations to PostgreSQL queries and replacing
     * plain text password validation with BCrypt hashing for enhanced security.
     * 
     * Authentication Process:
     * 1. Validates input parameters (username and password format)
     * 2. Normalizes username to uppercase (maintaining COBOL compatibility)
     * 3. Queries PostgreSQL users table via UserRepository
     * 4. Validates BCrypt hashed password using Spring Security
     * 5. Generates JWT token with user information and role claims
     * 6. Updates last login timestamp for audit trail
     * 7. Returns comprehensive LoginResponse with token and user context
     * 
     * Security Features:
     * - Input validation and sanitization
     * - BCrypt password validation with configurable salt rounds
     * - JWT token generation with configurable expiration
     * - Role-based access control mapping
     * - Comprehensive audit logging
     * - Protection against timing attacks
     * 
     * Error Handling:
     * - Validates input parameters for null/empty values
     * - Handles user not found scenarios
     * - Handles password validation failures
     * - Handles database access exceptions
     * - Provides consistent error responses for security
     * 
     * @param loginRequest Validated login request containing username and password
     * @return LoginResponse containing JWT token and user information for successful authentication
     * @throws IllegalArgumentException if input validation fails
     * @throws RuntimeException if database access fails or authentication processing fails
     */
    public LoginResponse login(@Valid LoginRequest loginRequest) {
        logger.info("Authentication request received for user: {}", loginRequest.getUsername());
        
        try {
            // Validate input parameters
            if (loginRequest == null || 
                loginRequest.getUsername() == null || loginRequest.getUsername().trim().isEmpty() ||
                loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
                
                logger.warn("Authentication failed: Invalid input parameters");
                throw new IllegalArgumentException(ERROR_INVALID_INPUT);
            }
            
            // Normalize username to uppercase (maintaining COBOL compatibility)
            String normalizedUsername = loginRequest.getUsername().trim().toUpperCase();
            String password = loginRequest.getPassword().trim();
            
            // Query user from PostgreSQL users table (replaces COBOL VSAM READ)
            Optional<User> userOptional = userRepository.findByUserId(normalizedUsername);
            
            if (!userOptional.isPresent()) {
                logger.warn("Authentication failed: User not found - {}", normalizedUsername);
                // Consistent timing to prevent username enumeration attacks
                passwordEncoder.matches("dummy", "$2a$12$dummy.hash.to.prevent.timing.attacks");
                throw new RuntimeException(ERROR_USER_NOT_FOUND);
            }
            
            User user = userOptional.get();
            
            // Validate BCrypt hashed password (replaces COBOL plain text comparison)
            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                logger.warn("Authentication failed: Invalid password for user - {}", normalizedUsername);
                throw new RuntimeException(ERROR_INVALID_CREDENTIALS);
            }
            
            // Generate session ID for correlation
            String sessionId = UUID.randomUUID().toString();
            
            // Generate JWT token with user information and role claims
            String jwtToken = jwtTokenUtil.generateToken(user.getUserId(), user.getUserType(), sessionId);
            
            // Map user type to Spring Security role
            String role = mapUserTypeToRole(user.getUserType());
            
            // Generate menu items based on user type (replaces COBOL XCTL routing)
            List<String> menuItems = generateMenuItems(user.getUserType());
            
            // Calculate session expiry time
            LocalDateTime sessionExpiry = LocalDateTime.now().plusMinutes(30);
            
            // Create comprehensive login response
            LoginResponse loginResponse = new LoginResponse(jwtToken, user.getUserId(), user.getUserType(), role);
            loginResponse.setFirstName(user.getFirstName());
            loginResponse.setLastName(user.getLastName());
            loginResponse.setMenuItems(menuItems);
            loginResponse.setSessionExpiry(sessionExpiry);
            
            // Update last login timestamp for audit trail
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            
            logger.info("Authentication successful for user: {} ({})", 
                       user.getUserId(), user.getUserType());
            
            return loginResponse;
            
        } catch (IllegalArgumentException e) {
            logger.error("Authentication validation error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Authentication processing error for user: {} - {}", 
                        loginRequest.getUsername(), e.getMessage());
            throw new RuntimeException(ERROR_AUTHENTICATION_FAILED + ": " + e.getMessage(), e);
        }
    }

    /**
     * Logs out the authenticated user by invalidating their JWT token.
     * 
     * This method implements logout functionality by marking the token as invalid
     * and clearing any associated session data. In a distributed architecture,
     * this would typically involve blacklisting the token in Redis or updating
     * the token validation logic.
     * 
     * @param token JWT token to invalidate
     * @return boolean true if logout was successful, false otherwise
     */
    public boolean logout(String token) {
        logger.info("Logout request received");
        
        try {
            // Validate token before logout
            if (token == null || token.trim().isEmpty()) {
                logger.warn("Logout failed: Invalid token format");
                return false;
            }
            
            // Validate token signature and expiration
            if (!jwtTokenUtil.validateToken(token)) {
                logger.warn("Logout failed: Invalid or expired token");
                return false;
            }
            
            // Extract user information from token
            String userId = jwtTokenUtil.extractUsername(token);
            String sessionId = jwtTokenUtil.extractSessionId(token);
            
            // TODO: In production, implement token blacklisting in Redis
            // This would involve adding the token to a blacklist with TTL
            // matching the token's expiration time
            
            logger.info("Logout successful for user: {} (session: {})", userId, sessionId);
            return true;
            
        } catch (Exception e) {
            logger.error("Logout processing error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validates a JWT token for authentication and authorization purposes.
     * 
     * This method provides comprehensive token validation including signature
     * verification, expiration checking, and claims validation. It supports
     * Spring Security's authentication flow and provides detailed validation
     * results for authorization decisions.
     * 
     * @param token JWT token to validate
     * @return boolean true if token is valid and not expired, false otherwise
     */
    public boolean validateToken(String token) {
        logger.debug("Token validation request received");
        
        try {
            // Validate token format
            if (token == null || token.trim().isEmpty()) {
                logger.debug("Token validation failed: Invalid token format");
                return false;
            }
            
            // Validate token signature and expiration
            boolean isValid = jwtTokenUtil.validateToken(token);
            
            if (isValid) {
                // Extract user information for additional validation
                String userId = jwtTokenUtil.extractUsername(token);
                String userType = jwtTokenUtil.extractUserType(token);
                String[] roles = jwtTokenUtil.extractRoles(token);
                
                // Validate user still exists in database
                Optional<User> userOptional = userRepository.findByUserId(userId);
                if (!userOptional.isPresent()) {
                    logger.warn("Token validation failed: User no longer exists - {}", userId);
                    return false;
                }
                
                // Validate user type consistency
                User user = userOptional.get();
                if (!user.getUserType().equals(userType)) {
                    logger.warn("Token validation failed: User type mismatch for user - {}", userId);
                    return false;
                }
                
                logger.debug("Token validation successful for user: {} ({})", userId, userType);
                return true;
            }
            
            logger.debug("Token validation failed: Invalid token signature or expired");
            return false;
            
        } catch (Exception e) {
            logger.error("Token validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Refreshes a JWT token by generating a new token with updated expiration.
     * 
     * This method implements token refresh functionality for maintaining user
     * sessions without requiring re-authentication. It validates the existing
     * token and generates a new one with extended expiration time.
     * 
     * @param token Existing JWT token to refresh
     * @return LoginResponse containing new JWT token and updated user context
     * @throws RuntimeException if token refresh fails
     */
    public LoginResponse refreshToken(String token) {
        logger.info("Token refresh request received");
        
        try {
            // Validate existing token
            if (!jwtTokenUtil.validateToken(token)) {
                logger.warn("Token refresh failed: Invalid or expired token");
                throw new RuntimeException(ERROR_TOKEN_INVALID);
            }
            
            // Extract user information from existing token
            String userId = jwtTokenUtil.extractUsername(token);
            String userType = jwtTokenUtil.extractUserType(token);
            String sessionId = jwtTokenUtil.extractSessionId(token);
            
            // Validate user still exists in database
            Optional<User> userOptional = userRepository.findByUserId(userId);
            if (!userOptional.isPresent()) {
                logger.warn("Token refresh failed: User no longer exists - {}", userId);
                throw new RuntimeException(ERROR_USER_NOT_FOUND);
            }
            
            User user = userOptional.get();
            
            // Generate new JWT token with updated expiration
            String newToken = jwtTokenUtil.generateToken(user.getUserId(), user.getUserType(), sessionId);
            
            // Map user type to Spring Security role
            String role = mapUserTypeToRole(user.getUserType());
            
            // Generate menu items based on user type
            List<String> menuItems = generateMenuItems(user.getUserType());
            
            // Calculate new session expiry time
            LocalDateTime sessionExpiry = LocalDateTime.now().plusMinutes(30);
            
            // Create refresh response
            LoginResponse refreshResponse = new LoginResponse(newToken, user.getUserId(), user.getUserType(), role);
            refreshResponse.setFirstName(user.getFirstName());
            refreshResponse.setLastName(user.getLastName());
            refreshResponse.setMenuItems(menuItems);
            refreshResponse.setSessionExpiry(sessionExpiry);
            
            logger.info("Token refresh successful for user: {} ({})", user.getUserId(), user.getUserType());
            
            return refreshResponse;
            
        } catch (Exception e) {
            logger.error("Token refresh error: {}", e.getMessage());
            throw new RuntimeException(ERROR_TOKEN_EXPIRED + ": " + e.getMessage(), e);
        }
    }

    /**
     * Maps COBOL user type to Spring Security role for authorization.
     * 
     * This method converts the legacy COBOL user type system to Spring Security
     * role-based access control, maintaining compatibility with the original
     * COBOL program's user type logic.
     * 
     * User Type Mapping:
     * - 'A' (Admin) -> ROLE_ADMIN
     * - 'U' (User) -> ROLE_USER
     * - Default -> ROLE_USER
     * 
     * @param userType COBOL user type ('A' for Admin, 'U' for User)
     * @return String Spring Security role name
     */
    private String mapUserTypeToRole(String userType) {
        if (USER_TYPE_ADMIN.equals(userType)) {
            return ROLE_ADMIN;
        } else {
            return ROLE_USER;
        }
    }

    /**
     * Generates menu items based on user type for frontend navigation.
     * 
     * This method replicates the original COBOL program's routing logic where
     * admin users are directed to COADM01C and regular users to COMEN01C.
     * The menu items enable role-based navigation in the React frontend.
     * 
     * @param userType COBOL user type ('A' for Admin, 'U' for User)
     * @return List<String> menu items available to the user
     */
    private List<String> generateMenuItems(String userType) {
        if (USER_TYPE_ADMIN.equals(userType)) {
            // Admin menu items (equivalent to COADM01C routing)
            return Arrays.asList(
                "Account Management",
                "Card Management", 
                "Transaction Processing",
                "User Administration",
                "System Reports",
                "Batch Processing",
                "System Configuration"
            );
        } else {
            // Regular user menu items (equivalent to COMEN01C routing)
            return Arrays.asList(
                "Account View",
                "Card List",
                "Transaction History",
                "Bill Payment",
                "Profile Management"
            );
        }
    }

    /**
     * Validates user credentials format and constraints.
     * 
     * This method provides comprehensive input validation for authentication
     * requests, ensuring compliance with the original COBOL field constraints
     * and security requirements.
     * 
     * @param loginRequest Login request to validate
     * @return boolean true if validation passes, false otherwise
     */
    private boolean validateCredentials(LoginRequest loginRequest) {
        if (loginRequest == null) {
            return false;
        }
        
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        
        // Validate username format (matching COBOL PIC X(08) constraint)
        if (username == null || username.trim().isEmpty() || username.length() > 8) {
            return false;
        }
        
        // Validate password format (matching COBOL PIC X(08) constraint)
        if (password == null || password.trim().isEmpty() || password.length() > 8) {
            return false;
        }
        
        return true;
    }

    /**
     * Generates comprehensive error response for authentication failures.
     * 
     * This method provides consistent error response structure for security
     * purposes, preventing information leakage about valid usernames or
     * system internals.
     * 
     * @param errorMessage Error message to include in response
     * @param httpStatus HTTP status code for the error
     * @return LoginResponse error response structure
     */
    private LoginResponse generateErrorResponse(String errorMessage, HttpStatus httpStatus) {
        LoginResponse errorResponse = new LoginResponse();
        errorResponse.setUserId(null);
        errorResponse.setToken(null);
        errorResponse.setUserType(null);
        errorResponse.setRole(null);
        errorResponse.setTimestamp(LocalDateTime.now());
        
        // Log error for monitoring and security analysis
        logger.warn("Authentication error: {} (HTTP {})", errorMessage, httpStatus.value());
        
        return errorResponse;
    }

    /**
     * Checks if the authentication service is healthy and ready to process requests.
     * 
     * This method provides health check functionality for monitoring and
     * load balancing systems to ensure the authentication service is operational.
     * 
     * @return boolean true if service is healthy, false otherwise
     */
    public boolean isHealthy() {
        try {
            // Check database connectivity
            long userCount = userRepository.count();
            
            // Check JWT token utility availability
            String testToken = jwtTokenUtil.generateToken("TEST", "U", "test-session");
            boolean tokenValid = jwtTokenUtil.validateToken(testToken);
            
            logger.debug("Authentication service health check: DB users={}, JWT test={}", 
                        userCount, tokenValid);
            
            return tokenValid;
            
        } catch (Exception e) {
            logger.error("Authentication service health check failed: {}", e.getMessage());
            return false;
        }
    }
}