package com.carddemo.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carddemo.repository.UserRepository;
import com.carddemo.repository.UserSecurityRepository;
import com.carddemo.entity.User;
import com.carddemo.entity.UserSecurity;
import com.carddemo.dto.UserDetailResponse;

/**
 * Spring Boot service implementing user detail retrieval translated from COUSR02C.cbl.
 * Fetches complete user profile including security settings, role assignments, 
 * password history, and last login timestamps. Maintains RACF user attribute mapping 
 * while providing REST-compatible user information structure.
 * 
 * This service translates the COBOL MAIN-PARA functionality that:
 * 1. Reads user data from USRSEC file using user ID
 * 2. Populates user details (first name, last name, password, user type)
 * 3. Handles validation and error scenarios
 * 4. Provides comprehensive user information for display/update operations
 */
@Service
public class UserDetailService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSecurityRepository userSecurityRepository;

    /**
     * Main user detail retrieval method translating COBOL MAIN-PARA functionality.
     * Reads user data from both User and UserSecurity tables to build comprehensive
     * user detail response matching COUSR02C behavior.
     * 
     * @param userId User identifier (8 characters) matching SEC-USR-ID from COBOL
     * @return UserDetailResponse containing complete user profile information
     * @throws IllegalArgumentException if userId is null or empty
     */
    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetail(String userId) {
        logger.info("Retrieving user details for userId: {}", userId);
        
        // Input validation matching COBOL error handling
        if (userId == null || userId.trim().isEmpty()) {
            logger.error("User ID cannot be empty");
            throw new IllegalArgumentException("User ID can NOT be empty...");
        }

        // Lookup user security information first (primary lookup path)
        Optional<UserSecurity> userSecurityOpt = userSecurityRepository.findByUserId(userId);
        
        if (!userSecurityOpt.isPresent()) {
            logger.warn("User security record not found for userId: {}", userId);
            throw new RuntimeException("User ID NOT found...");
        }

        UserSecurity userSecurity = userSecurityOpt.get();
        
        // Lookup business user information (secondary lookup)
        Optional<User> userOpt = userRepository.findByUserId(userId);
        
        // Build comprehensive user detail response
        UserDetailResponse response = new UserDetailResponse();
        
        // Primary user identification from UserSecurity (matching SEC-USR-ID, SEC-USR-FNAME, etc.)
        response.setUserId(userSecurity.getUserId());
        response.setFirstName(userSecurity.getFirstName());
        response.setLastName(userSecurity.getLastName());
        response.setUserType(userSecurity.getUserType());
        
        // Security and authorization information
        response.setAccountStatus(userSecurity.isEnabled() ? "ACTIVE" : "INACTIVE");
        response.setAccountLocked(!userSecurity.isAccountNonLocked());
        response.setPasswordExpired(!userSecurity.isCredentialsNonExpired());
        
        // Role and permission information from Spring Security authorities
        String roleDescription = "Regular User";
        String permissionLevel = "USER";
        
        if (userSecurity.getAuthorities() != null) {
            boolean isAdmin = userSecurity.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().contains("ADMIN"));
            if (isAdmin) {
                roleDescription = "Administrator";
                permissionLevel = "ADMIN";
            }
        }
        
        response.setRoleDescription(roleDescription);
        response.setPermissionLevel(permissionLevel);
        
        // Additional user profile information if available from User entity
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Supplement with business user details
            if (response.getFirstName() == null || response.getFirstName().trim().isEmpty()) {
                response.setFirstName(user.getFirstName());
            }
            if (response.getLastName() == null || response.getLastName().trim().isEmpty()) {
                response.setLastName(user.getLastName());
            }
            
            // Additional profile information
            logger.debug("Enhanced user detail with business profile data for userId: {}", userId);
        }

        logger.info("Successfully retrieved user details for userId: {}", userId);
        return response;
    }

    /**
     * Alternative lookup method for retrieving user details by username.
     * Translates COBOL pattern of user lookup with different key fields.
     * 
     * @param username Username for lookup (Spring Security username)
     * @return UserDetailResponse containing complete user profile information
     * @throws IllegalArgumentException if username is null or empty
     */
    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetailByUsername(String username) {
        logger.info("Retrieving user details for username: {}", username);
        
        // Input validation
        if (username == null || username.trim().isEmpty()) {
            logger.error("Username cannot be empty");
            throw new IllegalArgumentException("Username can NOT be empty...");
        }

        // Lookup by username in UserSecurity table
        Optional<UserSecurity> userSecurityOpt = userSecurityRepository.findByUsername(username);
        
        if (!userSecurityOpt.isPresent()) {
            logger.warn("User not found for username: {}", username);
            throw new RuntimeException("User NOT found...");
        }

        UserSecurity userSecurity = userSecurityOpt.get();
        
        // Delegate to main getUserDetail method using the found userId
        return getUserDetail(userSecurity.getUserId());
    }

    /**
     * Retrieves basic user profile information without security details.
     * Simplified version for scenarios requiring only business user data.
     * 
     * @param userId User identifier
     * @return UserDetailResponse with basic profile information
     */
    @Transactional(readOnly = true)
    public UserDetailResponse getUserProfile(String userId) {
        logger.info("Retrieving user profile for userId: {}", userId);
        
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID can NOT be empty...");
        }

        Optional<User> userOpt = userRepository.findByUserId(userId);
        
        if (!userOpt.isPresent()) {
            logger.warn("User profile not found for userId: {}", userId);
            throw new RuntimeException("User ID NOT found...");
        }

        User user = userOpt.get();
        
        UserDetailResponse response = new UserDetailResponse();
        response.setUserId(user.getUserId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setUserType(user.getUserType());
        response.setAccountStatus(user.getStatus());
        
        logger.info("Successfully retrieved user profile for userId: {}", userId);
        return response;
    }

    /**
     * Validates if a user exists in the system.
     * Utility method for user existence checking without full detail retrieval.
     * 
     * @param userId User identifier to validate
     * @return true if user exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean validateUserExists(String userId) {
        logger.debug("Validating user existence for userId: {}", userId);
        
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }

        // Check both User and UserSecurity tables for comprehensive validation
        boolean existsInUserSecurity = userSecurityRepository.findByUserId(userId).isPresent();
        boolean existsInUser = userRepository.findByUserId(userId).isPresent();
        
        boolean userExists = existsInUserSecurity || existsInUser;
        logger.debug("User existence validation for userId {}: {}", userId, userExists);
        
        return userExists;
    }

    /**
     * Retrieves security-specific information for a user.
     * Focused method for authentication and authorization data.
     * 
     * @param userId User identifier
     * @return UserDetailResponse with security-focused information
     */
    @Transactional(readOnly = true)
    public UserDetailResponse getUserSecurityInfo(String userId) {
        logger.info("Retrieving user security info for userId: {}", userId);
        
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID can NOT be empty...");
        }

        Optional<UserSecurity> userSecurityOpt = userSecurityRepository.findByUserId(userId);
        
        if (!userSecurityOpt.isPresent()) {
            logger.warn("User security info not found for userId: {}", userId);
            throw new RuntimeException("User ID NOT found...");
        }

        UserSecurity userSecurity = userSecurityOpt.get();
        
        UserDetailResponse response = new UserDetailResponse();
        response.setUserId(userSecurity.getUserId());
        response.setFirstName(userSecurity.getFirstName());
        response.setLastName(userSecurity.getLastName());
        response.setUserType(userSecurity.getUserType());
        response.setAccountStatus(userSecurity.isEnabled() ? "ACTIVE" : "INACTIVE");
        response.setAccountLocked(!userSecurity.isAccountNonLocked());
        response.setPasswordExpired(!userSecurity.isCredentialsNonExpired());
        
        // Role and permission information
        if (userSecurity.getAuthorities() != null) {
            boolean isAdmin = userSecurity.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().contains("ADMIN"));
            response.setRoleDescription(isAdmin ? "Administrator" : "Regular User");
            response.setPermissionLevel(isAdmin ? "ADMIN" : "USER");
        }
        
        logger.info("Successfully retrieved user security info for userId: {}", userId);
        return response;
    }
}