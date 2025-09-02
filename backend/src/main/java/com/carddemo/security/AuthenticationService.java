/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.security;

import com.carddemo.entity.UserSecurity;
import com.carddemo.repository.UserSecurityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Authentication service that replaces RACF authentication functionality from COBOL COSGN00C program.
 * 
 * This service implements the core authentication logic that was originally handled by the mainframe
 * COSGN00C COBOL program, providing modern Spring Security-based authentication while maintaining
 * 100% functional parity with the original mainframe behavior.
 * 
 * <p>Key Features:</p>
 * <ul>
 *   <li>User credential validation against PostgreSQL usrsec table (replaces VSAM USRSEC file)</li>
 *   <li>Password validation using PlainTextPasswordEncoder for legacy COBOL password compatibility</li>
 *   <li>User type determination (ADMIN vs USER) based on SEC-USR-TYPE field mapping</li>
 *   <li>Spring Security Authentication object creation for session management</li>
 *   <li>Case-insensitive username handling matching COBOL behavior</li>
 *   <li>Comprehensive error handling and validation matching legacy patterns</li>
 * </ul>
 *
 * <p>COBOL COSGN00C Replacement:</p>
 * This service directly replaces the functionality of the COSGN00C COBOL program that handled
 * user sign-on authentication in the mainframe environment. The authentication flow maintains
 * identical business logic and user experience while leveraging modern Java/Spring technologies.
 * 
 * <p>Performance Requirements:</p>
 * Authentication operations must complete within 200ms to meet system performance requirements
 * and maintain user experience parity with the original mainframe implementation.
 *
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Service
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    @Autowired
    private UserSecurityRepository userSecurityRepository;

    @Autowired
    private PlainTextPasswordEncoder passwordEncoder;

    /**
     * Authenticates a user with username and password, returning Spring Security Authentication object.
     * 
     * This method replicates the core authentication logic from COBOL COSGN00C program while
     * integrating with Spring Security framework. It performs credential validation, user lookup,
     * and creates an authenticated Authentication object for session management.
     * 
     * <p>Authentication Process:</p>
     * <ol>
     *   <li>Validate input parameters (null/empty checks)</li>
     *   <li>Perform case-insensitive username lookup in PostgreSQL</li>
     *   <li>Validate password using PlainTextPasswordEncoder for COBOL compatibility</li>
     *   <li>Check user account status (enabled, not locked, not expired)</li>
     *   <li>Determine user authorities based on user type (ADMIN/USER)</li>
     *   <li>Create and return Spring Security Authentication object</li>
     * </ol>
     *
     * <p>Error Handling:</p>
     * - Throws UsernameNotFoundException for invalid username or password
     * - Throws IllegalArgumentException for null/empty parameters
     * - Handles account status violations (disabled, locked, expired)
     * 
     * @param username the username for authentication (case-insensitive, max 8 characters)
     * @param password the password for authentication (max 8 characters for COBOL compatibility)
     * @return Authentication object containing user details and authorities
     * @throws UsernameNotFoundException if username not found or password invalid
     * @throws IllegalArgumentException if username or password is null/empty
     */
    public Authentication authenticate(String username, String password) {
        logger.debug("Attempting authentication for username: {}", username);
        long startTime = System.currentTimeMillis();

        try {
            // Validate input parameters
            validateAuthenticationInputs(username, password);

            // Trim and convert to uppercase for COBOL compatibility
            String normalizedUsername = normalizeUsername(username);
            
            // Find user in PostgreSQL usrsec table
            Optional<UserSecurity> userOpt = userSecurityRepository.findByUsername(normalizedUsername);
            if (userOpt.isEmpty()) {
                logger.warn("Authentication failed - user not found: {}", normalizedUsername);
                throw new UsernameNotFoundException("Invalid username or password");
            }

            UserSecurity user = userOpt.get();

            // Validate account status
            validateAccountStatus(user);

            // Validate password using PlainTextPasswordEncoder for COBOL compatibility
            if (!passwordEncoder.matches(password.trim(), user.getPassword())) {
                logger.warn("Authentication failed - invalid password for user: {}", normalizedUsername);
                throw new UsernameNotFoundException("Invalid username or password");
            }

            // Determine user authorities based on user type
            List<GrantedAuthority> authorities = createUserAuthorities(user);

            // Create Spring Security Authentication object
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(), 
                null, // Don't store password in authentication object
                authorities
            );

            long executionTime = System.currentTimeMillis() - startTime;
            logger.info("Authentication successful for user: {} in {}ms", normalizedUsername, executionTime);

            return authentication;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Authentication failed for user: {} in {}ms - {}", username, executionTime, e.getMessage());
            throw e;
        }
    }

    /**
     * Validates user credentials without creating full Authentication object.
     * 
     * This method provides credential validation functionality for scenarios where
     * full authentication is not required but credential verification is needed.
     * It replicates COBOL password validation logic while integrating with modern
     * Spring Security password encoding.
     *
     * @param username the username to validate (case-insensitive)
     * @param password the password to validate
     * @return true if credentials are valid, false otherwise
     */
    public boolean validateCredentials(String username, String password) {
        logger.debug("Validating credentials for username: {}", username);

        try {
            // Validate input parameters
            if (username == null || password == null || 
                username.trim().isEmpty() || password.trim().isEmpty()) {
                logger.debug("Credential validation failed - null or empty inputs");
                return false;
            }

            // Normalize username for COBOL compatibility
            String normalizedUsername = normalizeUsername(username);
            
            // Find user in PostgreSQL usrsec table
            Optional<UserSecurity> userOpt = userSecurityRepository.findByUsername(normalizedUsername);
            if (userOpt.isEmpty()) {
                logger.debug("Credential validation failed - user not found: {}", normalizedUsername);
                return false;
            }

            UserSecurity user = userOpt.get();

            // Check if account is enabled
            if (!user.isEnabled()) {
                logger.debug("Credential validation failed - account disabled: {}", normalizedUsername);
                return false;
            }

            // Validate password using PlainTextPasswordEncoder
            boolean isValid = passwordEncoder.matches(password.trim(), user.getPassword());
            
            logger.debug("Credential validation result for user {}: {}", normalizedUsername, isValid);
            return isValid;

        } catch (Exception e) {
            logger.error("Error during credential validation for user: {} - {}", username, e.getMessage());
            return false;
        }
    }

    /**
     * Determines user type (ADMIN or USER) based on SEC-USR-TYPE field from COBOL.
     * 
     * This method replicates the user type determination logic from the COBOL COSGN00C
     * program, mapping the SEC-USR-TYPE field values to Spring Security role names.
     * 
     * <p>User Type Mapping:</p>
     * <ul>
     *   <li>'A' (Admin) -> "ADMIN"</li>
     *   <li>'U' (User) -> "USER"</li>
     *   <li>Any other value -> "USER" (default)</li>
     * </ul>
     *
     * @param user the UserSecurity entity to determine type for
     * @return "ADMIN" if user type is 'A', "USER" otherwise
     * @throws IllegalArgumentException if user is null
     */
    public String determineUserType(UserSecurity user) {
        if (user == null) {
            logger.error("Cannot determine user type - user is null");
            throw new IllegalArgumentException("User cannot be null");
        }

        String userType = user.getUserType();
        if (userType == null) {
            logger.warn("User type is null for user: {}, defaulting to USER", user.getUsername());
            return "USER";
        }

        // Map COBOL SEC-USR-TYPE field values to role names
        String normalizedUserType = userType.trim().toUpperCase();
        switch (normalizedUserType) {
            case "A":
                logger.debug("User {} determined as ADMIN type", user.getUsername());
                return "ADMIN";
            case "U":
                logger.debug("User {} determined as USER type", user.getUsername());
                return "USER";
            default:
                logger.warn("Unknown user type '{}' for user {}, defaulting to USER", 
                    userType, user.getUsername());
                return "USER";
        }
    }

    /**
     * Validates authentication input parameters for null/empty values.
     * 
     * @param username the username to validate
     * @param password the password to validate
     * @throws IllegalArgumentException if either parameter is null or empty
     */
    private void validateAuthenticationInputs(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
    }

    /**
     * Normalizes username for COBOL compatibility (trim and uppercase).
     * 
     * This method replicates the COBOL username handling logic where usernames
     * are stored and compared in uppercase format with trailing spaces trimmed.
     *
     * @param username the username to normalize
     * @return normalized username (trimmed and uppercase)
     */
    private String normalizeUsername(String username) {
        return username.trim().toUpperCase();
    }

    /**
     * Validates user account status (enabled, not locked, not expired).
     * 
     * This method checks various account status flags to ensure the user account
     * is in a valid state for authentication, matching COBOL account validation logic.
     *
     * @param user the UserSecurity entity to validate
     * @throws UsernameNotFoundException if account status is invalid
     */
    private void validateAccountStatus(UserSecurity user) {
        if (!user.isEnabled()) {
            logger.warn("Authentication failed - account disabled: {}", user.getUsername());
            throw new UsernameNotFoundException("Account is disabled");
        }

        if (!user.isAccountNonLocked()) {
            logger.warn("Authentication failed - account locked: {}", user.getUsername());
            throw new UsernameNotFoundException("Account is locked");
        }

        if (!user.isAccountNonExpired()) {
            logger.warn("Authentication failed - account expired: {}", user.getUsername());
            throw new UsernameNotFoundException("Account is expired");
        }

        if (!user.isCredentialsNonExpired()) {
            logger.warn("Authentication failed - credentials expired: {}", user.getUsername());
            throw new UsernameNotFoundException("Credentials are expired");
        }
    }

    /**
     * Creates Spring Security authorities based on user type.
     * 
     * This method converts the COBOL user type field to Spring Security GrantedAuthority
     * objects, enabling role-based access control in the modernized application.
     *
     * @param user the UserSecurity entity to create authorities for
     * @return List of GrantedAuthority objects based on user type
     */
    private List<GrantedAuthority> createUserAuthorities(UserSecurity user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        String userType = determineUserType(user);
        
        // Add role-based authority
        authorities.add(new SimpleGrantedAuthority("ROLE_" + userType));
        
        // Add user type specific permissions
        if ("ADMIN".equals(userType)) {
            authorities.add(new SimpleGrantedAuthority("ADMIN_ACCESS"));
            authorities.add(new SimpleGrantedAuthority("USER_MANAGEMENT"));
        } else {
            authorities.add(new SimpleGrantedAuthority("USER_ACCESS"));
        }

        logger.debug("Created authorities for user {}: {}", user.getUsername(), authorities);
        return authorities;
    }
}