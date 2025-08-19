package com.carddemo.controller;

import com.carddemo.service.UserService;
import com.carddemo.dto.UserDto;
import com.carddemo.dto.UserListDto;
import com.carddemo.dto.UserListResponse;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.exception.ValidationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for user management operations replacing COBOL programs
 * COUSR00C, COUSR01C, COUSR02C, and COUSR03C. Handles complete user CRUD
 * functionality with role-based access control and comprehensive validation.
 * 
 * Transaction code mappings:
 * - CU00: GET /api/users (list users with pagination)
 * - CU01: POST /api/users (create new user) 
 * - CU02: PUT /api/users/{id} (update existing user)
 * - CU03: DELETE /api/users/{id} (delete user)
 * 
 * Additional endpoint:
 * - GET /api/users/{id} (get user details)
 * 
 * Security:
 * - All endpoints require authentication
 * - Admin operations (create, update, delete) require ADMIN role
 * - View operations available to all authenticated users
 * 
 * Error handling replicates COBOL ABEND patterns and VSAM response codes
 * while providing modern REST API response structures.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    /**
     * User service providing business logic operations.
     * Injected to delegate CRUD operations to service layer.
     */
    @Autowired
    private UserService userService;

    /**
     * Lists users with pagination support. Replaces COUSR00C (CU00) transaction.
     * 
     * Provides paginated user listing functionality equivalent to the COBOL 
     * COUSR00 BMS screen. Supports up to 10 users per page matching the original
     * 3270 terminal screen layout. Includes pagination metadata for navigation.
     * 
     * Query parameters:
     * - page: Page number (0-based, default 0)
     * - size: Page size (default 10, max 10)
     * - sort: Sort field and direction (default: userId,asc)
     * - startUserId: Starting user ID for search (optional)
     * 
     * @param page page number for pagination (0-based indexing)
     * @param size number of users per page (maximum 10)
     * @param sortBy field to sort by (default: userId)
     * @param sortDir sort direction (asc or desc)
     * @param startUserId starting user ID for filtered search
     * @return ResponseEntity containing UserListResponse with paginated user data
     */
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserListResponse> listUsers(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "userId") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir,
            @RequestParam(value = "startUserId", required = false) String startUserId) {
        
        try {
            // Validate page size to match BMS screen constraints (max 10 users)
            if (size > 10) {
                size = 10;
            }
            if (size <= 0) {
                size = 10;
            }
            if (page < 0) {
                page = 0;
            }

            // Create sort direction - default to ascending if invalid
            Sort.Direction direction;
            try {
                direction = Sort.Direction.fromString(sortDir);
            } catch (IllegalArgumentException e) {
                direction = Sort.Direction.ASC;
            }

            // Create pageable with sort parameters
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            // Call service to get paginated user list
            UserListResponse response = userService.listUsers(pageable);
            
            return ResponseEntity.status(HttpStatus.OK).body(response);
            
        } catch (ValidationException e) {
            // Handle validation errors from service layer
            throw e;
        } catch (Exception e) {
            // Handle unexpected errors
            ValidationException ve = new ValidationException("Error retrieving user list");
            ve.addFieldError("system", "Unable to process user list request: " + e.getMessage());
            throw ve;
        }
    }

    /**
     * Gets user details by user ID. Additional endpoint for user lookup functionality.
     * 
     * Provides individual user detail retrieval equivalent to the user lookup
     * functionality in COUSR02C and COUSR03C when populating user data for
     * update or delete operations.
     * 
     * @param userId the user ID to retrieve
     * @return ResponseEntity containing UserDto with user details
     * @throws ResourceNotFoundException if user is not found
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserDto> getUserById(@PathVariable String userId) {
        
        try {
            // Validate user ID parameter
            if (userId == null || userId.trim().isEmpty()) {
                ValidationException ve = new ValidationException("User ID is required");
                ve.addFieldError("userId", "User ID cannot be empty");
                throw ve;
            }

            // Call service to get user by ID
            UserDto user = userService.findUserById(userId.trim());
            
            return ResponseEntity.status(HttpStatus.OK).body(user);
            
        } catch (ResourceNotFoundException e) {
            // Re-throw resource not found exceptions
            throw e;
        } catch (ValidationException e) {
            // Re-throw validation exceptions
            throw e;
        } catch (Exception e) {
            // Handle unexpected errors
            throw new ResourceNotFoundException("User", userId, "Error retrieving user: " + e.getMessage());
        }
    }

    /**
     * Creates a new user. Replaces COUSR01C (CU01) transaction.
     * 
     * Provides user creation functionality equivalent to the COBOL COUSR01
     * add user screen. Validates all required fields and handles duplicate
     * user ID detection matching original VSAM DUPKEY/DUPREC handling.
     * 
     * Required fields:
     * - userId: 1-8 alphanumeric characters
     * - firstName: 1-20 characters
     * - lastName: 1-20 characters
     * - userType: 'A' for Admin, 'U' for User
     * - password: 1-8 characters
     * 
     * @param userDto user data for creation
     * @return ResponseEntity containing created UserDto
     * @throws ValidationException if validation fails
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto userDto) {
        
        try {
            // Additional business validation beyond bean validation
            validateUserForCreation(userDto);
            
            // Call service to create user
            UserDto createdUser = userService.createUser(userDto);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
            
        } catch (ValidationException e) {
            // Re-throw validation exceptions with field-level details
            throw e;
        } catch (Exception e) {
            // Handle unexpected errors during creation
            ValidationException ve = new ValidationException("Error creating user");
            ve.addFieldError("system", "Unable to create user: " + e.getMessage());
            throw ve;
        }
    }

    /**
     * Updates an existing user. Replaces COUSR02C (CU02) transaction.
     * 
     * Provides user update functionality equivalent to the COBOL COUSR02
     * update screen. Validates all required fields and handles user not found
     * scenarios matching original VSAM NOTFND response code handling.
     * 
     * Only fields present in the request body will be updated.
     * User ID cannot be changed after creation.
     * 
     * @param userId the user ID to update
     * @param userDto updated user data
     * @return ResponseEntity containing updated UserDto
     * @throws ResourceNotFoundException if user is not found
     * @throws ValidationException if validation fails
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateUser(@PathVariable String userId, 
                                              @Valid @RequestBody UserDto userDto) {
        
        try {
            // Validate path parameter
            if (userId == null || userId.trim().isEmpty()) {
                ValidationException ve = new ValidationException("User ID is required");
                ve.addFieldError("userId", "User ID cannot be empty");
                throw ve;
            }

            // Ensure user ID in path matches user ID in body (if provided)
            if (userDto.getUserId() != null && !userId.trim().equals(userDto.getUserId().trim())) {
                ValidationException ve = new ValidationException("User ID mismatch");
                ve.addFieldError("userId", "User ID in path must match user ID in request body");
                throw ve;
            }

            // Set user ID in DTO to match path parameter
            userDto.setUserId(userId.trim());
            
            // Additional business validation for updates
            validateUserForUpdate(userDto);
            
            // Call service to update user
            UserDto updatedUser = userService.updateUser(userId.trim(), userDto);
            
            return ResponseEntity.status(HttpStatus.OK).body(updatedUser);
            
        } catch (ResourceNotFoundException e) {
            // Re-throw resource not found exceptions
            throw e;
        } catch (ValidationException e) {
            // Re-throw validation exceptions
            throw e;
        } catch (Exception e) {
            // Handle unexpected errors during update
            ValidationException ve = new ValidationException("Error updating user");
            ve.addFieldError("system", "Unable to update user: " + e.getMessage());
            throw ve;
        }
    }

    /**
     * Deletes a user. Replaces COUSR03C (CU03) transaction.
     * 
     * Provides user deletion functionality equivalent to the COBOL COUSR03
     * delete screen. Handles user not found scenarios and validates user
     * can be safely deleted (referential integrity).
     * 
     * @param userId the user ID to delete
     * @return ResponseEntity with no content
     * @throws ResourceNotFoundException if user is not found
     * @throws ValidationException if user cannot be deleted
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        
        try {
            // Validate user ID parameter
            if (userId == null || userId.trim().isEmpty()) {
                ValidationException ve = new ValidationException("User ID is required");
                ve.addFieldError("userId", "User ID cannot be empty");
                throw ve;
            }

            // Call service to delete user
            userService.deleteUser(userId.trim());
            
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            
        } catch (ResourceNotFoundException e) {
            // Re-throw resource not found exceptions
            throw e;
        } catch (ValidationException e) {
            // Re-throw validation exceptions
            throw e;
        } catch (Exception e) {
            // Handle unexpected errors during deletion
            ValidationException ve = new ValidationException("Error deleting user");
            ve.addFieldError("system", "Unable to delete user: " + e.getMessage());
            throw ve;
        }
    }

    /**
     * Validates user data for creation operations.
     * Performs additional business logic validation beyond bean validation
     * to replicate COBOL edit routine validation patterns.
     * 
     * @param userDto user data to validate
     * @throws ValidationException if validation fails
     */
    private void validateUserForCreation(UserDto userDto) {
        ValidationException ve = new ValidationException("User validation failed");
        boolean hasErrors = false;

        // Validate user ID is provided and not empty (COBOL: USRIDINI = SPACES check)
        if (userDto.getUserId() == null || userDto.getUserId().trim().isEmpty()) {
            ve.addFieldError("userId", "User ID can NOT be empty...");
            hasErrors = true;
        }

        // Validate first name is provided (COBOL: FNAMEI = SPACES check)
        if (userDto.getFirstName() == null || userDto.getFirstName().trim().isEmpty()) {
            ve.addFieldError("firstName", "First Name can NOT be empty...");
            hasErrors = true;
        }

        // Validate last name is provided (COBOL: LNAMEI = SPACES check)
        if (userDto.getLastName() == null || userDto.getLastName().trim().isEmpty()) {
            ve.addFieldError("lastName", "Last Name can NOT be empty...");
            hasErrors = true;
        }

        // Validate password is provided (COBOL: PASSWDI = SPACES check)
        if (userDto.getPassword() == null || userDto.getPassword().trim().isEmpty()) {
            ve.addFieldError("password", "Password can NOT be empty...");
            hasErrors = true;
        }

        // Validate user type is provided (COBOL: USRTYPEI = SPACES check)
        if (userDto.getUserType() == null || userDto.getUserType().trim().isEmpty()) {
            ve.addFieldError("userType", "User Type can NOT be empty...");
            hasErrors = true;
        } else {
            // Validate user type is valid value
            String userType = userDto.getUserType().trim().toUpperCase();
            if (!"A".equals(userType) && !"U".equals(userType)) {
                ve.addFieldError("userType", "User Type must be 'A' for Admin or 'U' for User");
                hasErrors = true;
            }
        }

        if (hasErrors) {
            throw ve;
        }
    }

    /**
     * Validates user data for update operations.
     * Performs additional business logic validation for user updates
     * to replicate COBOL edit routine validation patterns.
     * 
     * @param userDto user data to validate
     * @throws ValidationException if validation fails
     */
    private void validateUserForUpdate(UserDto userDto) {
        ValidationException ve = new ValidationException("User validation failed");
        boolean hasErrors = false;

        // Validate first name if provided (COBOL: FNAMEI = SPACES check)
        if (userDto.getFirstName() != null && userDto.getFirstName().trim().isEmpty()) {
            ve.addFieldError("firstName", "First Name can NOT be empty...");
            hasErrors = true;
        }

        // Validate last name if provided (COBOL: LNAMEI = SPACES check)
        if (userDto.getLastName() != null && userDto.getLastName().trim().isEmpty()) {
            ve.addFieldError("lastName", "Last Name can NOT be empty...");
            hasErrors = true;
        }

        // Validate password if provided (COBOL: PASSWDI = SPACES check)
        if (userDto.getPassword() != null && userDto.getPassword().trim().isEmpty()) {
            ve.addFieldError("password", "Password can NOT be empty...");
            hasErrors = true;
        }

        // Validate user type if provided (COBOL: USRTYPEI = SPACES check)
        if (userDto.getUserType() != null) {
            if (userDto.getUserType().trim().isEmpty()) {
                ve.addFieldError("userType", "User Type can NOT be empty...");
                hasErrors = true;
            } else {
                // Validate user type is valid value
                String userType = userDto.getUserType().trim().toUpperCase();
                if (!"A".equals(userType) && !"U".equals(userType)) {
                    ve.addFieldError("userType", "User Type must be 'A' for Admin or 'U' for User");
                    hasErrors = true;
                }
            }
        }

        if (hasErrors) {
            throw ve;
        }
    }
}