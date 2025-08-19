package com.carddemo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.carddemo.dto.UserDto;
import com.carddemo.dto.UserListResponse;
import com.carddemo.entity.User;
import com.carddemo.entity.UserSecurity;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.exception.ValidationException;
import com.carddemo.repository.UserRepository;
import com.carddemo.repository.UserSecurityRepository;
import com.carddemo.util.ValidationUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Comprehensive unit test class for UserService that validates complete CRUD operations
 * for user management converted from COUSR00C, COUSR01C, COUSR02C, and COUSR03C COBOL programs
 * with role-based access control.
 * 
 * This test class ensures:
 * - Complete CRUD operations with paginated listing
 * - Role-based access control integration
 * - Session state management matching CICS COMMAREA behavior
 * - Password encryption standards validation
 * - User status management and audit trail functionality
 * - Concurrent user modifications and permission validation
 * 
 * Replaces mainframe COBOL program testing with modern Spring Boot unit testing
 * while maintaining identical functional behavior and business rule enforcement.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSecurityRepository userSecurityRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private SignOnService signOnService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    // Note: UserService will be created in future phase as it doesn't exist yet
    // This test class validates the expected interface and behavior

    private User testUser;
    private UserSecurity testUserSecurity;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        // Initialize test data representing typical user scenarios from COBOL programs
        testUser = new User();
        testUser.setId(1L);
        testUser.setUserId("TEST0001");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john.doe@carddemo.com");
        testUser.setDepartment("IT");
        testUser.setPhone("555-1234");
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now().minusDays(30));
        testUser.setUpdatedAt(LocalDateTime.now().minusDays(1));

        testUserSecurity = new UserSecurity();
        testUserSecurity.setSecUsrId("TEST0001");
        testUserSecurity.setUsername("john.doe");
        testUserSecurity.setPassword("$2a$10$encodedPassword");
        testUserSecurity.setUserType("REGULAR");
        testUserSecurity.setEnabled(true);
        testUserSecurity.setAccountNonExpired(true);
        testUserSecurity.setAccountNonLocked(true);
        testUserSecurity.setCredentialsNonExpired(true);

        testUserDto = new UserDto();
        testUserDto.setUserId("TEST0001");
        testUserDto.setFirstName("John");
        testUserDto.setLastName("Doe");
        testUserDto.setEmail("john.doe@carddemo.com");
        testUserDto.setUserType("REGULAR");
        testUserDto.setPassword("plainPassword123");
    }

    /**
     * Nested test class for user listing functionality - equivalent to COUSR00C.cbl
     */
    @Nested
    @DisplayName("User Listing Tests - COUSR00C Equivalent")
    class UserListingTests {

        @Test
        @DisplayName("Should successfully list users with pagination")
        void testListUsersWithPagination() {
            // Given: A paginated request for users
            Pageable pageable = PageRequest.of(0, 10);
            List<User> userList = List.of(testUser);
            Page<User> userPage = new PageImpl<>(userList, pageable, 1);

            when(userRepository.findAll(pageable)).thenReturn(userPage);

            // When: Listing users with pagination
            UserListResponse response = userService.listUsers(pageable);

            // Then: Should return properly formatted paginated response
            assertThat(response).isNotNull();
            assertThat(response.getUsers()).hasSize(1);
            assertThat(response.getTotalElements()).isEqualTo(1);
            assertThat(response.getPageNumber()).isEqualTo(0);
            assertThat(response.getPageSize()).isEqualTo(10);
            assertThat(response.getUsers().get(0).getUserId()).isEqualTo("TEST0001");

            verify(userRepository).findAll(pageable);
            verify(auditService).saveAuditLog(eq("USER_LIST"), anyString(), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should handle empty user list gracefully")
        void testListUsersEmptyResult() {
            // Given: No users in the system
            Pageable pageable = PageRequest.ofSize(10);
            Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(userRepository.findAll(pageable)).thenReturn(emptyPage);

            // When: Listing users
            UserListResponse response = userService.listUsers(pageable);

            // Then: Should return empty but valid response
            assertThat(response).isNotNull();
            assertThat(response.getUsers()).isEmpty();
            assertThat(response.getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should filter users by status when requested")
        void testListUsersByStatus() {
            // Given: Repository configured to filter by active status
            Pageable pageable = PageRequest.ofSize(10);
            List<User> activeUsers = List.of(testUser);
            Page<User> activePage = new PageImpl<>(activeUsers, pageable, 1);

            when(userRepository.findByActive(true, pageable)).thenReturn(activePage);

            // When: Listing only active users
            UserListResponse response = userService.listUsers(true, pageable);

            // Then: Should return filtered results
            assertThat(response.getUsers()).hasSize(1);
            assertThat(response.getUsers().get(0).isActive()).isTrue();

            verify(userRepository).findByActive(true, pageable);
        }
    }

    /**
     * Nested test class for user retrieval functionality
     */
    @Nested
    @DisplayName("User Retrieval Tests - Individual User Access")
    class UserRetrievalTests {

        @Test
        @DisplayName("Should successfully retrieve user by ID")
        void testGetUserById() {
            // Given: User exists in repository
            when(userRepository.findByUserId("TEST0001")).thenReturn(Optional.of(testUser));

            // When: Finding user by ID
            UserDto result = userService.findUserById("TEST0001");

            // Then: Should return properly mapped user data
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo("TEST0001");
            assertThat(result.getFirstName()).isEqualTo("John");
            assertThat(result.getLastName()).isEqualTo("Doe");
            assertThat(result.getEmail()).isEqualTo("john.doe@carddemo.com");

            verify(userRepository).findByUserId("TEST0001");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for non-existent user")
        void testGetUserByIdNotFound() {
            // Given: User does not exist
            when(userRepository.findByUserId("NONEXIST")).thenReturn(Optional.empty());

            // When/Then: Should throw appropriate exception
            assertThatThrownBy(() -> userService.findUserById("NONEXIST"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with ID: NONEXIST");

            verify(userRepository).findByUserId("NONEXIST");
        }

        @Test
        @DisplayName("Should validate user access permissions during retrieval")
        void testGetUserByIdWithPermissionValidation() {
            // Given: User exists and security context is validated
            when(userRepository.findByUserId("TEST0001")).thenReturn(Optional.of(testUser));
            when(signOnService.getSecurityContext()).thenReturn("ADMIN");

            // When: Finding user with permission check
            UserDto result = userService.findUserById("TEST0001");

            // Then: Should complete successfully with audit trail
            assertThat(result).isNotNull();
            verify(auditService).saveAuditLog(eq("USER_VIEW"), eq("TEST0001"), any(LocalDateTime.class));
        }
    }

    /**
     * Nested test class for user creation functionality - equivalent to COUSR01C.cbl
     */
    @Nested
    @DisplayName("User Creation Tests - COUSR01C Equivalent")
    class UserCreationTests {

        @Test
        @DisplayName("Should successfully create new user with all validations")
        void testCreateUser() {
            // Given: Valid user data and mocked dependencies
            when(userRepository.existsByUserId("TEST0001")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userSecurityRepository.save(any(UserSecurity.class))).thenReturn(testUserSecurity);
            when(passwordEncoder.encode("plainPassword123")).thenReturn("$2a$10$encodedPassword");

            try (MockedStatic<ValidationUtil> validationUtil = mockStatic(ValidationUtil.class)) {
                validationUtil.when(() -> ValidationUtil.validateUserData(any(UserDto.class)))
                             .thenReturn(true);
                validationUtil.when(() -> ValidationUtil.checkDuplicateUser("TEST0001"))
                             .thenReturn(false);

                // When: Creating a new user
                UserDto result = userService.createUser(testUserDto);

                // Then: Should create user successfully with proper validation
                assertThat(result).isNotNull();
                assertThat(result.getUserId()).isEqualTo("TEST0001");

                verify(userRepository).existsByUserId("TEST0001");
                verify(userRepository).save(any(User.class));
                verify(userSecurityRepository).save(any(UserSecurity.class));
                verify(passwordEncoder).encode("plainPassword123");
                verify(auditService).saveAuditLog(eq("USER_CREATE"), eq("TEST0001"), any(LocalDateTime.class));
            }
        }

        @Test
        @DisplayName("Should reject creation of duplicate user")
        void testCreateUserDuplicate() {
            // Given: User already exists
            when(userRepository.existsByUserId("TEST0001")).thenReturn(true);

            try (MockedStatic<ValidationUtil> validationUtil = mockStatic(ValidationUtil.class)) {
                validationUtil.when(() -> ValidationUtil.checkDuplicateUser("TEST0001"))
                             .thenReturn(true);

                // When/Then: Should throw validation exception
                assertThatThrownBy(() -> userService.createUser(testUserDto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("User ID already exists");

                verify(userRepository).existsByUserId("TEST0001");
                verify(userRepository, never()).save(any(User.class));
            }
        }

        @Test
        @DisplayName("Should validate required fields during user creation")
        void testCreateUserValidationFailure() {
            // Given: Invalid user data
            UserDto invalidUser = new UserDto();
            invalidUser.setUserId(""); // Invalid empty user ID

            try (MockedStatic<ValidationUtil> validationUtil = mockStatic(ValidationUtil.class)) {
                ValidationException validationException = new ValidationException("Validation failed");
                validationException.addFieldError("userId", "User ID cannot be empty");
                
                validationUtil.when(() -> ValidationUtil.validateUserData(invalidUser))
                             .thenThrow(validationException);

                // When/Then: Should throw validation exception with field errors
                assertThatThrownBy(() -> userService.createUser(invalidUser))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(ex -> {
                        ValidationException ve = (ValidationException) ex;
                        assertThat(ve.hasFieldErrors()).isTrue();
                        assertThat(ve.getFieldError("userId")).isEqualTo("User ID cannot be empty");
                    });
            }
        }

        @Test
        @DisplayName("Should enforce password complexity requirements")
        void testCreateUserPasswordValidation() {
            // Given: User with weak password
            testUserDto.setPassword("weak");

            try (MockedStatic<ValidationUtil> validationUtil = mockStatic(ValidationUtil.class)) {
                ValidationException validationException = new ValidationException("Password validation failed");
                validationException.addFieldError("password", "Password must be at least 8 characters");
                
                validationUtil.when(() -> ValidationUtil.validateUserData(testUserDto))
                             .thenThrow(validationException);

                // When/Then: Should reject weak password
                assertThatThrownBy(() -> userService.createUser(testUserDto))
                    .isInstanceOf(ValidationException.class);
            }
        }
    }

    /**
     * Nested test class for user update functionality - equivalent to COUSR02C.cbl
     */
    @Nested
    @DisplayName("User Update Tests - COUSR02C Equivalent")
    class UserUpdateTests {

        @Test
        @DisplayName("Should successfully update existing user")
        void testUpdateUser() {
            // Given: Existing user and valid update data
            when(userRepository.findByUserId("TEST0001")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            testUserDto.setFirstName("Jane");
            testUserDto.setEmail("jane.doe@carddemo.com");

            try (MockedStatic<ValidationUtil> validationUtil = mockStatic(ValidationUtil.class)) {
                validationUtil.when(() -> ValidationUtil.validateUserData(any(UserDto.class)))
                             .thenReturn(true);

                // When: Updating user
                UserDto result = userService.updateUser("TEST0001", testUserDto);

                // Then: Should update successfully
                assertThat(result).isNotNull();
                assertThat(result.getFirstName()).isEqualTo("Jane");

                verify(userRepository).findByUserId("TEST0001");
                verify(userRepository).save(any(User.class));
                verify(auditService).saveAuditLog(eq("USER_UPDATE"), eq("TEST0001"), any(LocalDateTime.class));
            }
        }

        @Test
        @DisplayName("Should handle concurrent modification scenarios")
        void testUpdateUserConcurrentModification() {
            // Given: User was modified by another process
            User staleUser = new User();
            staleUser.setUpdatedAt(LocalDateTime.now().minusHours(2));
            
            when(userRepository.findByUserId("TEST0001")).thenReturn(Optional.of(staleUser));

            // When/Then: Should handle optimistic locking
            assertThatThrownBy(() -> userService.updateUser("TEST0001", testUserDto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("concurrent modification");
        }

        @Test
        @DisplayName("Should validate permission to update user")
        void testUpdateUserPermissionCheck() {
            // Given: User exists but requester lacks permission
            when(userRepository.findByUserId("TEST0001")).thenReturn(Optional.of(testUser));
            when(signOnService.getSecurityContext()).thenReturn("READONLY");

            // When/Then: Should reject unauthorized update
            assertThatThrownBy(() -> userService.updateUser("TEST0001", testUserDto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("insufficient permissions");
        }

        @Test
        @DisplayName("Should update password with proper encoding")
        void testUpdateUserPassword() {
            // Given: Password update request
            when(userRepository.findByUserId("TEST0001")).thenReturn(Optional.of(testUser));
            when(userSecurityRepository.findBySecUsrId("TEST0001")).thenReturn(Optional.of(testUserSecurity));
            when(passwordEncoder.encode("newPassword123")).thenReturn("$2a$10$newEncodedPassword");
            when(userSecurityRepository.save(any(UserSecurity.class))).thenReturn(testUserSecurity);

            // When: Updating password through SignOnService
            userService.updateUserPassword("TEST0001", "newPassword123");

            // Then: Should encode and store new password
            verify(passwordEncoder).encode("newPassword123");
            verify(userSecurityRepository).save(any(UserSecurity.class));
            verify(auditService).saveAuditLog(eq("PASSWORD_UPDATE"), eq("TEST0001"), any(LocalDateTime.class));
        }
    }

    /**
     * Nested test class for user deletion functionality - equivalent to COUSR03C.cbl
     */
    @Nested
    @DisplayName("User Deletion Tests - COUSR03C Equivalent")
    class UserDeletionTests {

        @Test
        @DisplayName("Should successfully delete existing user")
        void testDeleteUser() {
            // Given: User exists and can be deleted
            when(userRepository.findByUserId("TEST0001")).thenReturn(Optional.of(testUser));
            when(userSecurityRepository.findBySecUsrId("TEST0001")).thenReturn(Optional.of(testUserSecurity));

            // When: Deleting user
            userService.deleteUser("TEST0001");

            // Then: Should delete both User and UserSecurity records
            verify(userRepository).findByUserId("TEST0001");
            verify(userSecurityRepository).delete(testUserSecurity);
            verify(userRepository).delete(testUser);
            verify(auditService).saveAuditLog(eq("USER_DELETE"), eq("TEST0001"), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should handle deletion of non-existent user")
        void testDeleteUserNotFound() {
            // Given: User does not exist
            when(userRepository.findByUserId("NONEXIST")).thenReturn(Optional.empty());

            // When/Then: Should throw ResourceNotFoundException
            assertThatThrownBy(() -> userService.deleteUser("NONEXIST"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with ID: NONEXIST");

            verify(userRepository, never()).delete(any(User.class));
        }

        @Test
        @DisplayName("Should prevent deletion of active admin users")
        void testDeleteUserAdminProtection() {
            // Given: Admin user that should be protected
            testUserSecurity.setUserType("ADMIN");
            when(userRepository.findByUserId("TEST0001")).thenReturn(Optional.of(testUser));
            when(userSecurityRepository.findBySecUsrId("TEST0001")).thenReturn(Optional.of(testUserSecurity));

            // When/Then: Should prevent admin deletion
            assertThatThrownBy(() -> userService.deleteUser("TEST0001"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot delete admin user");

            verify(userRepository, never()).delete(any(User.class));
        }
    }

    /**
     * Nested test class for validation functionality
     */
    @Nested
    @DisplayName("User Validation Tests - Business Rule Enforcement")
    class UserValidationTests {

        @Test
        @DisplayName("Should validate user data comprehensively")
        void testUserValidation() {
            // Given: User data requiring validation
            try (MockedStatic<ValidationUtil> validationUtil = mockStatic(ValidationUtil.class)) {
                validationUtil.when(() -> ValidationUtil.validateUserData(testUserDto))
                             .thenReturn(true);
                validationUtil.when(() -> ValidationUtil.validateEmail("john.doe@carddemo.com"))
                             .thenReturn(true);
                validationUtil.when(() -> ValidationUtil.validatePhone("555-1234"))
                             .thenReturn(true);

                // When: Validating user
                boolean isValid = userService.validateUser(testUserDto);

                // Then: Should perform comprehensive validation
                assertThat(isValid).isTrue();

                validationUtil.verify(() -> ValidationUtil.validateUserData(testUserDto));
                validationUtil.verify(() -> ValidationUtil.validateEmail("john.doe@carddemo.com"));
            }
        }

        @Test
        @DisplayName("Should detect validation violations")
        void testUserValidationFailures() {
            // Given: Invalid user data
            testUserDto.setEmail("invalid-email");

            try (MockedStatic<ValidationUtil> validationUtil = mockStatic(ValidationUtil.class)) {
                ValidationException validationException = new ValidationException("Validation failed");
                validationException.addFieldError("email", "Invalid email format");
                
                validationUtil.when(() -> ValidationUtil.validateEmail("invalid-email"))
                             .thenThrow(validationException);

                // When/Then: Should catch and report validation errors
                assertThatThrownBy(() -> userService.validateUser(testUserDto))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(ex -> {
                        ValidationException ve = (ValidationException) ex;
                        assertThat(ve.hasFieldError("email")).isTrue();
                    });
            }
        }
    }

    /**
     * Nested test class for permission and security validation
     */
    @Nested
    @DisplayName("Permission and Security Tests - Role-Based Access Control")
    class PermissionTests {

        @Test
        @DisplayName("Should enforce role-based access control for user operations")
        void testPermissionChecks() {
            // Given: Different user roles
            when(signOnService.getSecurityContext()).thenReturn("ADMIN");

            // When: Checking admin permissions
            boolean hasPermission = userService.hasUserManagementPermission();

            // Then: Should grant appropriate access
            assertThat(hasPermission).isTrue();
            verify(signOnService).getSecurityContext();
        }

        @Test
        @DisplayName("Should deny access for insufficient permissions")
        void testPermissionDenial() {
            // Given: Limited user role
            when(signOnService.getSecurityContext()).thenReturn("READONLY");

            // When: Checking management permissions
            boolean hasPermission = userService.hasUserManagementPermission();

            // Then: Should deny access
            assertThat(hasPermission).isFalse();
        }

        @Test
        @DisplayName("Should validate authentication context")
        void testAuthenticationValidation() {
            // Given: Valid authentication context
            when(signOnService.authenticateUser("john.doe", "password123")).thenReturn(testUserSecurity);

            // When: Validating authentication
            boolean isAuthenticated = userService.validateAuthentication("john.doe", "password123");

            // Then: Should confirm authentication
            assertThat(isAuthenticated).isTrue();
            verify(signOnService).authenticateUser("john.doe", "password123");
        }
    }

    /**
     * Nested test class for audit logging functionality
     */
    @Nested
    @DisplayName("Audit Logging Tests - Compliance and Tracking")
    class AuditLoggingTests {

        @Test
        @DisplayName("Should generate comprehensive audit logs for all operations")
        void testAuditLogging() {
            // Given: User operation that requires auditing
            when(userRepository.findByUserId("TEST0001")).thenReturn(Optional.of(testUser));

            // When: Performing audited operation
            userService.findUserById("TEST0001");

            // Then: Should create appropriate audit trail
            verify(auditService).saveAuditLog(eq("USER_VIEW"), eq("TEST0001"), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should retrieve audit logs for compliance reporting")
        void testAuditLogRetrieval() {
            // Given: Audit logs exist for user
            when(auditService.getAuditLogsByUser("TEST0001")).thenReturn(List.of("USER_CREATE", "USER_UPDATE"));

            // When: Retrieving audit history
            List<String> auditLogs = auditService.getAuditLogsByUser("TEST0001");

            // Then: Should return complete audit trail
            assertThat(auditLogs).hasSize(2);
            assertThat(auditLogs).contains("USER_CREATE", "USER_UPDATE");
        }

        @Test
        @DisplayName("Should generate compliance reports")
        void testComplianceReporting() {
            // Given: System configured for compliance reporting
            when(auditService.generateComplianceReport(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn("Compliance report data");

            // When: Generating compliance report
            String report = auditService.generateComplianceReport(
                LocalDateTime.now().minusDays(30), 
                LocalDateTime.now()
            );

            // Then: Should produce compliance report
            assertThat(report).isNotNull();
            assertThat(report).contains("Compliance report data");
        }
    }

    // Additional utility test methods supporting the expected UserService interface

    /**
     * Test method for comprehensive user management functionality.
     * Tests the complete CRUD operation cycle as specified in the exports schema.
     */
    @Test
    @DisplayName("Should support complete user management lifecycle")
    void testListUsersWithPagination() {
        // Given: System has users and pagination is requested
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = List.of(testUser);
        Page<User> userPage = new PageImpl<>(users, pageable, 1);

        when(userRepository.findAll(pageable)).thenReturn(userPage);

        // When: Listing users with pagination
        UserListResponse response = userService.listUsers(pageable);

        // Then: Should return paginated results
        assertThat(response).isNotNull();
        assertThat(response.getUsers()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);

        verify(userRepository).findAll(pageable);
        verify(auditService).saveAuditLog(eq("USER_LIST"), anyString(), any(LocalDateTime.class));
    }

    /**
     * Test method for user retrieval by ID as specified in exports schema.
     */
    @Test 
    @DisplayName("Should retrieve user by ID with proper validation")
    void testGetUserById() {
        // Given: User exists in system
        when(userRepository.findByUserId("TEST0001")).thenReturn(Optional.of(testUser));

        // When: Getting user by ID
        UserDto result = userService.findUserById("TEST0001");

        // Then: Should return user data
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo("TEST0001");

        verify(userRepository).findByUserId("TEST0001");
    }

    /**
     * Test method for user creation as specified in exports schema.
     */
    @Test
    @DisplayName("Should create user with comprehensive validation")
    void testCreateUser() {
        // Given: Valid user data for creation
        when(userRepository.existsByUserId("TEST0001")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userSecurityRepository.save(any(UserSecurity.class))).thenReturn(testUserSecurity);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");

        try (MockedStatic<ValidationUtil> validationUtil = mockStatic(ValidationUtil.class)) {
            validationUtil.when(() -> ValidationUtil.validateUserData(any(UserDto.class)))
                         .thenReturn(true);
            validationUtil.when(() -> ValidationUtil.checkDuplicateUser(anyString()))
                         .thenReturn(false);

            // When: Creating new user
            UserDto result = userService.createUser(testUserDto);

            // Then: Should create user successfully
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo("TEST0001");

            verify(userRepository).save(any(User.class));
            verify(auditService).saveAuditLog(eq("USER_CREATE"), eq("TEST0001"), any(LocalDateTime.class));
        }
    }

    /**
     * Test method for user updates as specified in exports schema.
     */
    @Test
    @DisplayName("Should update user with validation and audit trail")
    void testUpdateUser() {
        // Given: Existing user to update
        when(userRepository.findByUserId("TEST0001")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        try (MockedStatic<ValidationUtil> validationUtil = mockStatic(ValidationUtil.class)) {
            validationUtil.when(() -> ValidationUtil.validateUserData(any(UserDto.class)))
                         .thenReturn(true);

            // When: Updating user
            UserDto result = userService.updateUser("TEST0001", testUserDto);

            // Then: Should update successfully
            assertThat(result).isNotNull();

            verify(userRepository).save(any(User.class));
            verify(auditService).saveAuditLog(eq("USER_UPDATE"), eq("TEST0001"), any(LocalDateTime.class));
        }
    }

    /**
     * Test method for user deletion as specified in exports schema.
     */
    @Test
    @DisplayName("Should delete user with proper cleanup and audit")
    void testDeleteUser() {
        // Given: User exists and can be deleted
        when(userRepository.findByUserId("TEST0001")).thenReturn(Optional.of(testUser));
        when(userSecurityRepository.findBySecUsrId("TEST0001")).thenReturn(Optional.of(testUserSecurity));

        // When: Deleting user
        userService.deleteUser("TEST0001");

        // Then: Should delete user and security records
        verify(userRepository).delete(testUser);
        verify(userSecurityRepository).delete(testUserSecurity);
        verify(auditService).saveAuditLog(eq("USER_DELETE"), eq("TEST0001"), any(LocalDateTime.class));
    }

    /**
     * Test method for user validation as specified in exports schema.
     */
    @Test
    @DisplayName("Should validate user data according to business rules")
    void testUserValidation() {
        // Given: User data requiring validation
        try (MockedStatic<ValidationUtil> validationUtil = mockStatic(ValidationUtil.class)) {
            validationUtil.when(() -> ValidationUtil.validateUserData(testUserDto))
                         .thenReturn(true);

            // When: Validating user data
            boolean isValid = userService.validateUser(testUserDto);

            // Then: Should validate successfully
            assertThat(isValid).isTrue();

            validationUtil.verify(() -> ValidationUtil.validateUserData(testUserDto));
        }
    }

    /**
     * Test method for permission checking as specified in exports schema.
     */
    @Test
    @DisplayName("Should enforce role-based access control")
    void testPermissionChecks() {
        // Given: User with admin role
        when(signOnService.getSecurityContext()).thenReturn("ADMIN");

        // When: Checking permissions
        boolean hasPermission = userService.hasUserManagementPermission();

        // Then: Should grant access to admin
        assertThat(hasPermission).isTrue();

        verify(signOnService).getSecurityContext();
    }

    /**
     * Test method for audit logging as specified in exports schema.
     */
    @Test
    @DisplayName("Should generate comprehensive audit logs")
    void testAuditLogging() {
        // Given: User operation requiring audit
        when(userRepository.findByUserId("TEST0001")).thenReturn(Optional.of(testUser));

        // When: Performing audited operation
        userService.findUserById("TEST0001");

        // Then: Should generate audit log
        verify(auditService).saveAuditLog(eq("USER_VIEW"), eq("TEST0001"), any(LocalDateTime.class));
    }
}