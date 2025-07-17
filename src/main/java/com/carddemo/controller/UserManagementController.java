/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.controller;

import com.carddemo.common.entity.User;
import com.carddemo.common.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Spring Boot REST controller implementing ADMIN-only user management operations
 * with comprehensive CRUD functionality replacing legacy COUSR00C-COUSR03C CICS transactions.
 * 
 * This controller provides secure user management endpoints with method-level authorization
 * ensuring only users with ADMIN role can access user management operations.
 * 
 * Security Features:
 * - @PreAuthorize method-level security annotations for role-based access control
 * - BCrypt password hashing for secure credential storage
 * - Comprehensive audit logging for all user management operations
 * - Input validation and error handling for security compliance
 * - JWT-based authentication with Spring Security integration
 * 
 * Legacy CICS Transaction Mapping:
 * - COUSR00C (User List) → listUsers() with pagination support
 * - COUSR01C (User Add) → createUser() with validation
 * - COUSR02C (User Update) → updateUser() with optimistic locking
 * - COUSR03C (User Delete) → deleteUser() with safety checks
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private static final Logger logger = LoggerFactory.getLogger(UserManagementController.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /**
     * Lists all users with pagination support, replacing legacy COUSR00C CICS transaction.
     * 
     * This endpoint provides paginated user listing with sorting capabilities,
     * maintaining equivalent functionality to the original CICS user list screen
     * while adding modern pagination and filtering capabilities.
     * 
     * Pagination Parameters:
     * - page: Page number (0-based, default: 0)
     * - size: Page size (default: 10, max: 100)
     * - sort: Sort criteria (default: userId)
     * - direction: Sort direction (ASC/DESC, default: ASC)
     * 
     * Security: Requires ADMIN role via @PreAuthorize annotation
     * Audit: Logs all user list access attempts with admin context
     * 
     * @param page Page number for pagination (0-based)
     * @param size Number of records per page
     * @param sort Field to sort by (userId, firstName, lastName, userType)
     * @param direction Sort direction (ASC or DESC)
     * @return ResponseEntity containing paginated user list or error response
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "userId") String sort,
            @RequestParam(defaultValue = "ASC") String direction) {
        
        try {
            // Get current admin user for audit logging
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String adminUserId = authentication.getName();
            
            logger.info("Admin user '{}' requesting user list - page: {}, size: {}, sort: {}, direction: {}",
                    adminUserId, page, size, sort, direction);
            
            // Validate and sanitize pagination parameters
            if (page < 0) {
                logger.warn("Invalid page number '{}' requested by admin '{}'", page, adminUserId);
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("INVALID_PAGE", "Page number must be non-negative"));
            }
            
            if (size <= 0 || size > 100) {
                logger.warn("Invalid page size '{}' requested by admin '{}'", size, adminUserId);
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("INVALID_SIZE", "Page size must be between 1 and 100"));
            }
            
            // Create pageable with sorting
            Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC") ? 
                    Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
            
            // Retrieve paginated users
            Page<User> userPage = userRepository.findAll(pageable);
            
            // Convert to DTOs for response
            List<UserListDto> userDtos = userPage.getContent().stream()
                    .map(this::convertToListDto)
                    .collect(Collectors.toList());
            
            // Create paginated response
            PagedResponse<UserListDto> response = new PagedResponse<>(
                    userDtos,
                    userPage.getNumber(),
                    userPage.getSize(),
                    userPage.getTotalElements(),
                    userPage.getTotalPages(),
                    userPage.isFirst(),
                    userPage.isLast()
            );
            
            logger.info("Successfully retrieved {} users for admin '{}' - page {}/{}", 
                    userDtos.size(), adminUserId, page + 1, userPage.getTotalPages());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving user list for admin user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("SYSTEM_ERROR", "Unable to retrieve user list"));
        }
    }

    /**
     * Retrieves a specific user by ID, supporting both list selection and direct access.
     * 
     * This endpoint provides detailed user information for administrative operations,
     * supporting both the user list selection functionality from COUSR00C and
     * direct user access for update/delete operations.
     * 
     * Security: Requires ADMIN role via @PreAuthorize annotation
     * Audit: Logs all user access attempts with admin and target user context
     * 
     * @param userId The unique user identifier to retrieve
     * @return ResponseEntity containing user details or error response
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable String userId) {
        try {
            // Get current admin user for audit logging
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String adminUserId = authentication.getName();
            
            logger.info("Admin user '{}' requesting details for user '{}'", adminUserId, userId);
            
            // Validate user ID format
            if (userId == null || userId.trim().isEmpty() || userId.length() > 8) {
                logger.warn("Invalid user ID '{}' requested by admin '{}'", userId, adminUserId);
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("INVALID_USER_ID", "User ID must be 1-8 characters"));
            }
            
            // Retrieve user by ID
            Optional<User> userOptional = userRepository.findByUserId(userId.trim().toUpperCase());
            
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                UserDetailDto userDto = convertToDetailDto(user);
                
                logger.info("Successfully retrieved user '{}' for admin '{}'", userId, adminUserId);
                return ResponseEntity.ok(userDto);
            } else {
                logger.warn("User '{}' not found for admin '{}'", userId, adminUserId);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving user '{}' for admin user: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("SYSTEM_ERROR", "Unable to retrieve user details"));
        }
    }

    /**
     * Creates a new user in the system, replacing legacy COUSR01C CICS transaction.
     * 
     * This endpoint provides comprehensive user creation with validation,
     * maintaining equivalent functionality to the original CICS user add screen
     * while adding modern security features and audit capabilities.
     * 
     * Validation Rules:
     * - User ID: 1-8 characters, alphanumeric, unique
     * - First Name: 1-20 characters, required
     * - Last Name: 1-20 characters, required
     * - Password: 8 characters exactly (maintaining UI compatibility)
     * - User Type: 'A' (Admin) or 'U' (User), required
     * 
     * Security: Requires ADMIN role via @PreAuthorize annotation
     * Audit: Logs all user creation attempts with admin context
     * 
     * @param createRequest The user creation request with validation
     * @return ResponseEntity containing created user details or error response
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody UserCreateRequest createRequest) {
        try {
            // Get current admin user for audit logging
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String adminUserId = authentication.getName();
            
            logger.info("Admin user '{}' creating new user with ID '{}'", 
                    adminUserId, createRequest.getUserId());
            
            // Validate user ID uniqueness
            String normalizedUserId = createRequest.getUserId().trim().toUpperCase();
            if (userRepository.existsByUserId(normalizedUserId)) {
                logger.warn("Attempt to create duplicate user ID '{}' by admin '{}'", 
                        normalizedUserId, adminUserId);
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErrorResponse("DUPLICATE_USER_ID", 
                                "User ID '" + normalizedUserId + "' already exists"));
            }
            
            // Validate user type
            if (!createRequest.getUserType().equals("A") && !createRequest.getUserType().equals("U")) {
                logger.warn("Invalid user type '{}' specified by admin '{}'", 
                        createRequest.getUserType(), adminUserId);
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("INVALID_USER_TYPE", 
                                "User type must be 'A' (Admin) or 'U' (User)"));
            }
            
            // Hash the password using BCrypt
            String hashedPassword = passwordEncoder.encode(createRequest.getPassword());
            
            // Create new user entity
            User newUser = new User(
                    normalizedUserId,
                    createRequest.getFirstName().trim(),
                    createRequest.getLastName().trim(),
                    hashedPassword,
                    createRequest.getUserType()
            );
            
            // Save user to database
            User savedUser = userRepository.save(newUser);
            
            // Convert to response DTO
            UserDetailDto userDto = convertToDetailDto(savedUser);
            
            logger.info("Successfully created user '{}' by admin '{}'", 
                    savedUser.getUserId(), adminUserId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
            
        } catch (Exception e) {
            logger.error("Error creating user for admin user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("SYSTEM_ERROR", "Unable to create user"));
        }
    }

    /**
     * Updates an existing user in the system, replacing legacy COUSR02C CICS transaction.
     * 
     * This endpoint provides comprehensive user update functionality with validation,
     * maintaining equivalent functionality to the original CICS user update screen
     * while adding modern security features and optimistic locking.
     * 
     * Update Rules:
     * - User ID cannot be changed (primary key)
     * - First Name: 1-20 characters, required
     * - Last Name: 1-20 characters, required
     * - Password: Optional, 8 characters if provided
     * - User Type: 'A' (Admin) or 'U' (User), required
     * 
     * Security: Requires ADMIN role via @PreAuthorize annotation
     * Audit: Logs all user update attempts with admin and target user context
     * 
     * @param userId The unique user identifier to update
     * @param updateRequest The user update request with validation
     * @return ResponseEntity containing updated user details or error response
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable String userId, 
                                       @Valid @RequestBody UserUpdateRequest updateRequest) {
        try {
            // Get current admin user for audit logging
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String adminUserId = authentication.getName();
            
            logger.info("Admin user '{}' updating user '{}'", adminUserId, userId);
            
            // Validate user ID format
            if (userId == null || userId.trim().isEmpty() || userId.length() > 8) {
                logger.warn("Invalid user ID '{}' for update by admin '{}'", userId, adminUserId);
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("INVALID_USER_ID", "User ID must be 1-8 characters"));
            }
            
            // Retrieve existing user
            String normalizedUserId = userId.trim().toUpperCase();
            Optional<User> userOptional = userRepository.findByUserId(normalizedUserId);
            
            if (!userOptional.isPresent()) {
                logger.warn("User '{}' not found for update by admin '{}'", normalizedUserId, adminUserId);
                return ResponseEntity.notFound().build();
            }
            
            User existingUser = userOptional.get();
            
            // Validate user type
            if (!updateRequest.getUserType().equals("A") && !updateRequest.getUserType().equals("U")) {
                logger.warn("Invalid user type '{}' specified for user '{}' by admin '{}'", 
                        updateRequest.getUserType(), normalizedUserId, adminUserId);
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("INVALID_USER_TYPE", 
                                "User type must be 'A' (Admin) or 'U' (User)"));
            }
            
            // Update user fields
            existingUser.setFirstName(updateRequest.getFirstName().trim());
            existingUser.setLastName(updateRequest.getLastName().trim());
            existingUser.setUserType(updateRequest.getUserType());
            
            // Update password if provided
            if (updateRequest.getPassword() != null && !updateRequest.getPassword().trim().isEmpty()) {
                String hashedPassword = passwordEncoder.encode(updateRequest.getPassword());
                existingUser.setPasswordHash(hashedPassword);
                logger.info("Password updated for user '{}' by admin '{}'", normalizedUserId, adminUserId);
            }
            
            // Save updated user
            User updatedUser = userRepository.save(existingUser);
            
            // Convert to response DTO
            UserDetailDto userDto = convertToDetailDto(updatedUser);
            
            logger.info("Successfully updated user '{}' by admin '{}'", 
                    updatedUser.getUserId(), adminUserId);
            
            return ResponseEntity.ok(userDto);
            
        } catch (Exception e) {
            logger.error("Error updating user '{}' for admin user: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("SYSTEM_ERROR", "Unable to update user"));
        }
    }

    /**
     * Deletes a user from the system, replacing legacy COUSR03C CICS transaction.
     * 
     * This endpoint provides secure user deletion with safety checks,
     * maintaining equivalent functionality to the original CICS user delete screen
     * while adding modern security features and audit capabilities.
     * 
     * Safety Checks:
     * - Cannot delete own admin account
     * - Confirmation required for all deletions
     * - Comprehensive audit logging
     * 
     * Security: Requires ADMIN role via @PreAuthorize annotation
     * Audit: Logs all user deletion attempts with admin and target user context
     * 
     * @param userId The unique user identifier to delete
     * @param confirm Confirmation parameter (must be "true")
     * @return ResponseEntity with success message or error response
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable String userId, 
                                       @RequestParam(required = false) String confirm) {
        try {
            // Get current admin user for audit logging
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String adminUserId = authentication.getName();
            
            logger.info("Admin user '{}' attempting to delete user '{}'", adminUserId, userId);
            
            // Validate user ID format
            if (userId == null || userId.trim().isEmpty() || userId.length() > 8) {
                logger.warn("Invalid user ID '{}' for deletion by admin '{}'", userId, adminUserId);
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("INVALID_USER_ID", "User ID must be 1-8 characters"));
            }
            
            // Require confirmation for deletion
            if (!"true".equals(confirm)) {
                logger.warn("Deletion attempted without confirmation for user '{}' by admin '{}'", 
                        userId, adminUserId);
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("CONFIRMATION_REQUIRED", 
                                "Deletion confirmation required (confirm=true)"));
            }
            
            // Retrieve user to delete
            String normalizedUserId = userId.trim().toUpperCase();
            Optional<User> userOptional = userRepository.findByUserId(normalizedUserId);
            
            if (!userOptional.isPresent()) {
                logger.warn("User '{}' not found for deletion by admin '{}'", normalizedUserId, adminUserId);
                return ResponseEntity.notFound().build();
            }
            
            User userToDelete = userOptional.get();
            
            // Prevent self-deletion
            if (normalizedUserId.equals(adminUserId.toUpperCase())) {
                logger.warn("Admin '{}' attempted to delete own account", adminUserId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("SELF_DELETE_FORBIDDEN", 
                                "Cannot delete your own admin account"));
            }
            
            // Delete user
            userRepository.deleteByUserId(normalizedUserId);
            
            logger.info("Successfully deleted user '{}' by admin '{}'", normalizedUserId, adminUserId);
            
            return ResponseEntity.ok(new SuccessResponse("USER_DELETED", 
                    "User '" + normalizedUserId + "' has been successfully deleted"));
            
        } catch (Exception e) {
            logger.error("Error deleting user '{}' for admin user: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("SYSTEM_ERROR", "Unable to delete user"));
        }
    }

    /**
     * Converts User entity to UserListDto for list operations.
     * 
     * @param user The User entity to convert
     * @return UserListDto with public user information
     */
    private UserListDto convertToListDto(User user) {
        UserListDto dto = new UserListDto();
        dto.setUserId(user.getUserId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setUserType(user.getUserType());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastLogin(user.getLastLogin());
        return dto;
    }

    /**
     * Converts User entity to UserDetailDto for detailed operations.
     * 
     * @param user The User entity to convert
     * @return UserDetailDto with detailed user information
     */
    private UserDetailDto convertToDetailDto(User user) {
        UserDetailDto dto = new UserDetailDto();
        dto.setUserId(user.getUserId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setUserType(user.getUserType());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastLogin(user.getLastLogin());
        dto.setIsAdmin(user.isAdmin());
        dto.setIsRegularUser(user.isRegularUser());
        return dto;
    }

    // DTOs for request/response handling

    /**
     * DTO for user creation requests with validation annotations.
     */
    public static class UserCreateRequest {
        @NotBlank(message = "User ID is required")
        @Size(min = 1, max = 8, message = "User ID must be 1-8 characters")
        private String userId;

        @NotBlank(message = "First name is required")
        @Size(min = 1, max = 20, message = "First name must be 1-20 characters")
        private String firstName;

        @NotBlank(message = "Last name is required")
        @Size(min = 1, max = 20, message = "Last name must be 1-20 characters")
        private String lastName;

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 8, message = "Password must be exactly 8 characters")
        private String password;

        @NotBlank(message = "User type is required")
        @Size(min = 1, max = 1, message = "User type must be exactly 1 character")
        private String userType;

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getUserType() { return userType; }
        public void setUserType(String userType) { this.userType = userType; }
    }

    /**
     * DTO for user update requests with validation annotations.
     */
    public static class UserUpdateRequest {
        @NotBlank(message = "First name is required")
        @Size(min = 1, max = 20, message = "First name must be 1-20 characters")
        private String firstName;

        @NotBlank(message = "Last name is required")
        @Size(min = 1, max = 20, message = "Last name must be 1-20 characters")
        private String lastName;

        @Size(min = 8, max = 8, message = "Password must be exactly 8 characters")
        private String password;

        @NotBlank(message = "User type is required")
        @Size(min = 1, max = 1, message = "User type must be exactly 1 character")
        private String userType;

        // Getters and setters
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getUserType() { return userType; }
        public void setUserType(String userType) { this.userType = userType; }
    }

    /**
     * DTO for user list responses.
     */
    public static class UserListDto {
        private String userId;
        private String firstName;
        private String lastName;
        private String userType;
        private LocalDateTime createdAt;
        private LocalDateTime lastLogin;

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getUserType() { return userType; }
        public void setUserType(String userType) { this.userType = userType; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getLastLogin() { return lastLogin; }
        public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    }

    /**
     * DTO for detailed user responses.
     */
    public static class UserDetailDto {
        private String userId;
        private String firstName;
        private String lastName;
        private String userType;
        private LocalDateTime createdAt;
        private LocalDateTime lastLogin;
        private boolean isAdmin;
        private boolean isRegularUser;

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getUserType() { return userType; }
        public void setUserType(String userType) { this.userType = userType; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getLastLogin() { return lastLogin; }
        public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
        public boolean getIsAdmin() { return isAdmin; }
        public void setIsAdmin(boolean isAdmin) { this.isAdmin = isAdmin; }
        public boolean getIsRegularUser() { return isRegularUser; }
        public void setIsRegularUser(boolean isRegularUser) { this.isRegularUser = isRegularUser; }
    }

    /**
     * DTO for paginated responses.
     */
    public static class PagedResponse<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;

        public PagedResponse(List<T> content, int page, int size, long totalElements, 
                           int totalPages, boolean first, boolean last) {
            this.content = content;
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
            this.first = first;
            this.last = last;
        }

        // Getters and setters
        public List<T> getContent() { return content; }
        public void setContent(List<T> content) { this.content = content; }
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        public long getTotalElements() { return totalElements; }
        public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
        public boolean isFirst() { return first; }
        public void setFirst(boolean first) { this.first = first; }
        public boolean isLast() { return last; }
        public void setLast(boolean last) { this.last = last; }
    }

    /**
     * DTO for error responses.
     */
    public static class ErrorResponse {
        private String code;
        private String message;
        private LocalDateTime timestamp;

        public ErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
            this.timestamp = LocalDateTime.now();
        }

        // Getters and setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    /**
     * DTO for success responses.
     */
    public static class SuccessResponse {
        private String code;
        private String message;
        private LocalDateTime timestamp;

        public SuccessResponse(String code, String message) {
            this.code = code;
            this.message = message;
            this.timestamp = LocalDateTime.now();
        }

        // Getters and setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}