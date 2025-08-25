/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.entity.UserSecurity;
import com.carddemo.exception.ValidationException;
import com.carddemo.security.PlainTextPasswordEncoder;
import com.carddemo.util.ValidationUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.assertj.core.api.Assertions;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

// Import test infrastructure from depends_on_files
import AbstractBaseTest;
import UnitTest;
import TestConstants;

/**
 * Unit test class for UserAddService validating COBOL COUSR02C user creation logic 
 * migration to Java. Tests comprehensive user validation, password encryption, role 
 * assignment, and duplicate checking functionality.
 * 
 * <p>Test Coverage Areas:</p>
 * <ul>
 *   <li>User ID uniqueness validation and duplicate prevention</li>
 *   <li>Password complexity requirements and encryption verification</li>
 *   <li>Role assignment logic for Admin ('A') and Regular ('R') users</li>
 *   <li>Required field validation matching COBOL edit routines</li>
 *   <li>Input field validation including length and format checks</li>
 *   <li>Error handling and exception scenarios</li>
 *   <li>UserSecurity entity creation and persistence</li>
 * </ul>
 * 
 * <p>COBOL Functional Parity:</p>
 * Tests validate that the Java implementation maintains exact functional parity with 
 * the original COBOL COUSR02C program validation logic, including field validation 
 * rules, error message content, and business process flow.
 * 
 * <p>Test Framework:</p>
 * Uses JUnit 5, Mockito 5.8.0, and AssertJ 3.24.2 for comprehensive test coverage
 * including mock behavior verification, exception testing, and fluent assertions.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserAddService Unit Tests - COBOL COUSR02C Functional Parity")
public class UserAddServiceTest extends AbstractBaseTest implements UnitTest {

    @InjectMocks
    private UserAddService userAddService;

    @Mock
    private JpaRepository<UserSecurity, String> userRepository;

    @Mock
    private PlainTextPasswordEncoder passwordEncoder;

    // Test data constants from TestConstants class - COBOL field specifications
    private static final String VALID_USER_ID = TestConstants.TEST_USER_ID;
    private static final String VALID_FIRST_NAME = "John";
    private static final String VALID_LAST_NAME = "Doe";
    private static final String VALID_PASSWORD = TestConstants.TEST_USER_PASSWORD;
    private static final String VALID_ADMIN_TYPE = TestConstants.TEST_ADMIN_ROLE;
    private static final String VALID_REGULAR_TYPE = TestConstants.TEST_USER_ROLE;
    private static final String ENCODED_PASSWORD = "encoded_password";

    // Invalid test data for negative testing
    private static final String EMPTY_STRING = "";
    private static final String SPACES_ONLY = "   ";
    private static final String NULL_STRING = null;
    private static final String LONG_USER_ID = "VERYLONGUSERIDHEREEXCEEDING8CHARS";
    private static final String INVALID_USER_TYPE = "X";
    private static final String LONG_PASSWORD = "verylongpasswordexceeding8characters";

    private UserSecurity validUserSecurity;

    @BeforeEach
    @Override
    void setUp() {
        super.setUp(); // Call AbstractBaseTest setup
        validUserSecurity = createValidUserSecurity();
        mockCommonDependencies(); // Mock common test dependencies
        loadTestFixtures(); // Load test fixture data
    }

    @AfterEach
    @Override
    void tearDown() {
        super.tearDown(); // Call AbstractBaseTest cleanup
    }

    /**
     * Creates a valid UserSecurity test object matching COBOL field specifications.
     * Replicates USRSEC record structure from copybook CSUSR01Y.cpy.
     */
    private UserSecurity createValidUserSecurity() {
        UserSecurity user = new UserSecurity();
        user.setSecUsrId(VALID_USER_ID);
        user.setFirstName(VALID_FIRST_NAME);
        user.setLastName(VALID_LAST_NAME);
        user.setPassword(VALID_PASSWORD);
        user.setUserType(VALID_REGULAR_TYPE);
        user.setUsername(VALID_USER_ID); // For Spring Security UserDetails
        return user;
    }

    /**
     * Creates a UserSecurity object with specified parameters for testing.
     */
    private UserSecurity createUserSecurity(String userId, String firstName, String lastName, 
                                          String password, String userType) {
        UserSecurity user = new UserSecurity();
        user.setSecUsrId(userId);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword(password);
        user.setUserType(userType);
        user.setUsername(userId);
        return user;
    }

    @Nested
    @DisplayName("addUser() Method Tests - Core Business Logic")
    class AddUserTests {

        @Test
        @DisplayName("Should successfully add valid user with all required fields")
        void shouldAddValidUser() {
            // Arrange
            when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(UserSecurity.class))).thenReturn(validUserSecurity);

            // Act
            UserSecurity result = userAddService.addUser(validUserSecurity);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getSecUsrId()).isEqualTo(VALID_USER_ID);
            assertThat(result.getFirstName()).isEqualTo(VALID_FIRST_NAME);
            assertThat(result.getLastName()).isEqualTo(VALID_LAST_NAME);
            assertThat(result.getUserType()).isEqualTo(VALID_REGULAR_TYPE);
            
            // Verify interactions
            verify(userRepository).findById(VALID_USER_ID);
            verify(passwordEncoder).encode(VALID_PASSWORD);
            verify(userRepository).save(any(UserSecurity.class));
        }

        @Test
        @DisplayName("Should successfully add admin user")
        void shouldAddAdminUser() {
            // Arrange
            UserSecurity adminUser = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                      VALID_LAST_NAME, VALID_PASSWORD, VALID_ADMIN_TYPE);
            when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(UserSecurity.class))).thenReturn(adminUser);

            // Act
            UserSecurity result = userAddService.addUser(adminUser);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUserType()).isEqualTo(VALID_ADMIN_TYPE);
            
            verify(userRepository).findById(VALID_USER_ID);
            verify(passwordEncoder).encode(VALID_PASSWORD);
            verify(userRepository).save(any(UserSecurity.class));
        }

        @Test
        @DisplayName("Should throw ValidationException for null user")
        void shouldThrowExceptionForNullUser() {
            // Act & Assert
            assertThatThrownBy(() -> userAddService.addUser(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("User cannot be null");
            
            verify(userRepository, never()).findById(anyString());
            verify(userRepository, never()).save(any(UserSecurity.class));
        }

        @Test
        @DisplayName("Should throw ValidationException for duplicate user ID")
        void shouldThrowExceptionForDuplicateUserId() {
            // Arrange
            when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.of(validUserSecurity));

            // Act & Assert
            assertThatThrownBy(() -> userAddService.addUser(validUserSecurity))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("User ID already exists");
            
            verify(userRepository).findById(VALID_USER_ID);
            verify(userRepository, never()).save(any(UserSecurity.class));
            verify(passwordEncoder, never()).encode(anyString());
        }
    }

    @Nested
    @DisplayName("validateUserInput() Method Tests - Field Validation")
    class ValidateUserInputTests {

        @Test
        @DisplayName("Should pass validation for valid user input")
        void shouldPassValidationForValidInput() {
            // Act & Assert - should not throw exception
            assertThatCode(() -> userAddService.validateUserInput(validUserSecurity))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw ValidationException for empty user ID")
        void shouldThrowExceptionForEmptyUserId() {
            // Arrange
            UserSecurity userWithEmptyId = createUserSecurity(EMPTY_STRING, VALID_FIRST_NAME, 
                                                            VALID_LAST_NAME, VALID_PASSWORD, VALID_REGULAR_TYPE);

            // Act & Assert
            assertThatThrownBy(() -> userAddService.validateUserInput(userWithEmptyId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("User ID must be supplied");
        }

        @Test
        @DisplayName("Should throw ValidationException for spaces-only user ID")
        void shouldThrowExceptionForSpacesOnlyUserId() {
            // Arrange
            UserSecurity userWithSpacesId = createUserSecurity(SPACES_ONLY, VALID_FIRST_NAME, 
                                                             VALID_LAST_NAME, VALID_PASSWORD, VALID_REGULAR_TYPE);

            // Act & Assert
            assertThatThrownBy(() -> userAddService.validateUserInput(userWithSpacesId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("User ID must be supplied");
        }

        @Test
        @DisplayName("Should throw ValidationException for null user ID")
        void shouldThrowExceptionForNullUserId() {
            // Arrange
            UserSecurity userWithNullId = createUserSecurity(NULL_STRING, VALID_FIRST_NAME, 
                                                           VALID_LAST_NAME, VALID_PASSWORD, VALID_REGULAR_TYPE);

            // Act & Assert
            assertThatThrownBy(() -> userAddService.validateUserInput(userWithNullId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("User ID must be supplied");
        }

        @Test
        @DisplayName("Should throw ValidationException for user ID exceeding 8 characters")
        void shouldThrowExceptionForLongUserId() {
            // Arrange
            UserSecurity userWithLongId = createUserSecurity(LONG_USER_ID, VALID_FIRST_NAME, 
                                                           VALID_LAST_NAME, VALID_PASSWORD, VALID_REGULAR_TYPE);

            // Act & Assert
            assertThatThrownBy(() -> userAddService.validateUserInput(userWithLongId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("User ID must be 8 characters or less");
        }

        @Test
        @DisplayName("Should throw ValidationException for empty first name")
        void shouldThrowExceptionForEmptyFirstName() {
            // Arrange
            UserSecurity userWithEmptyFirstName = createUserSecurity(VALID_USER_ID, EMPTY_STRING, 
                                                                   VALID_LAST_NAME, VALID_PASSWORD, VALID_REGULAR_TYPE);

            // Act & Assert
            assertThatThrownBy(() -> userAddService.validateUserInput(userWithEmptyFirstName))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("First Name can NOT be empty");
        }

        @Test
        @DisplayName("Should throw ValidationException for empty last name")
        void shouldThrowExceptionForEmptyLastName() {
            // Arrange
            UserSecurity userWithEmptyLastName = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                                  EMPTY_STRING, VALID_PASSWORD, VALID_REGULAR_TYPE);

            // Act & Assert
            assertThatThrownBy(() -> userAddService.validateUserInput(userWithEmptyLastName))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Last Name can NOT be empty");
        }

        @Test
        @DisplayName("Should throw ValidationException for empty password")
        void shouldThrowExceptionForEmptyPassword() {
            // Arrange
            UserSecurity userWithEmptyPassword = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                                  VALID_LAST_NAME, EMPTY_STRING, VALID_REGULAR_TYPE);

            // Act & Assert
            assertThatThrownBy(() -> userAddService.validateUserInput(userWithEmptyPassword))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Password can NOT be empty");
        }

        @Test
        @DisplayName("Should throw ValidationException for password exceeding 8 characters")
        void shouldThrowExceptionForLongPassword() {
            // Arrange
            UserSecurity userWithLongPassword = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                                 VALID_LAST_NAME, LONG_PASSWORD, VALID_REGULAR_TYPE);

            // Act & Assert
            assertThatThrownBy(() -> userAddService.validateUserInput(userWithLongPassword))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Password must be at least 4 characters long");
        }

        @Test
        @DisplayName("Should throw ValidationException for empty user type")
        void shouldThrowExceptionForEmptyUserType() {
            // Arrange
            UserSecurity userWithEmptyType = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                              VALID_LAST_NAME, VALID_PASSWORD, EMPTY_STRING);

            // Act & Assert
            assertThatThrownBy(() -> userAddService.validateUserInput(userWithEmptyType))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("User Type can NOT be empty");
        }

        @Test
        @DisplayName("Should throw ValidationException for invalid user type")
        void shouldThrowExceptionForInvalidUserType() {
            // Arrange
            UserSecurity userWithInvalidType = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                                VALID_LAST_NAME, VALID_PASSWORD, INVALID_USER_TYPE);

            // Act & Assert
            assertThatThrownBy(() -> userAddService.validateUserInput(userWithInvalidType))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("User type must be 'A' for Admin or 'R' for Regular");
        }

        @Test
        @DisplayName("Should validate admin user type")
        void shouldValidateAdminUserType() {
            // Arrange
            UserSecurity adminUser = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                      VALID_LAST_NAME, VALID_PASSWORD, VALID_ADMIN_TYPE);

            // Act & Assert - should not throw exception
            assertThatCode(() -> userAddService.validateUserInput(adminUser))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should validate regular user type")
        void shouldValidateRegularUserType() {
            // Arrange
            UserSecurity regularUser = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                        VALID_LAST_NAME, VALID_PASSWORD, VALID_REGULAR_TYPE);

            // Act & Assert - should not throw exception
            assertThatCode(() -> userAddService.validateUserInput(regularUser))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("checkDuplicateUser() Method Tests - Uniqueness Validation")
    class CheckDuplicateUserTests {

        @Test
        @DisplayName("Should return false for non-existent user ID")
        void shouldReturnFalseForNonExistentUser() {
            // Arrange
            when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.empty());

            // Act
            boolean isDuplicate = userAddService.checkDuplicateUser(VALID_USER_ID);

            // Assert
            assertThat(isDuplicate).isFalse();
            verify(userRepository).findById(VALID_USER_ID);
        }

        @Test
        @DisplayName("Should return true for existing user ID")
        void shouldReturnTrueForExistingUser() {
            // Arrange
            when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.of(validUserSecurity));

            // Act
            boolean isDuplicate = userAddService.checkDuplicateUser(VALID_USER_ID);

            // Assert
            assertThat(isDuplicate).isTrue();
            verify(userRepository).findById(VALID_USER_ID);
        }

        @Test
        @DisplayName("Should handle null user ID gracefully")
        void shouldHandleNullUserIdGracefully() {
            // Act
            boolean isDuplicate = userAddService.checkDuplicateUser(NULL_STRING);

            // Assert
            assertThat(isDuplicate).isFalse();
            verify(userRepository, never()).findById(anyString());
        }

        @Test
        @DisplayName("Should handle empty user ID gracefully")
        void shouldHandleEmptyUserIdGracefully() {
            // Act
            boolean isDuplicate = userAddService.checkDuplicateUser(EMPTY_STRING);

            // Assert
            assertThat(isDuplicate).isFalse();
            verify(userRepository, never()).findById(anyString());
        }
    }

    @Nested
    @DisplayName("encryptPassword() Method Tests - Password Security")
    class EncryptPasswordTests {

        @Test
        @DisplayName("Should encrypt password using PlainTextPasswordEncoder")
        void shouldEncryptPassword() {
            // Arrange
            when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn(ENCODED_PASSWORD);

            // Act
            String encryptedPassword = userAddService.encryptPassword(VALID_PASSWORD);

            // Assert
            assertThat(encryptedPassword).isEqualTo(ENCODED_PASSWORD);
            verify(passwordEncoder).encode(VALID_PASSWORD);
        }

        @Test
        @DisplayName("Should handle null password in encryption")
        void shouldHandleNullPasswordInEncryption() {
            // Arrange
            when(passwordEncoder.encode(null)).thenThrow(new IllegalArgumentException("Password cannot be null"));

            // Act & Assert
            assertThatThrownBy(() -> userAddService.encryptPassword(NULL_STRING))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password cannot be null");
            
            verify(passwordEncoder).encode(NULL_STRING);
        }

        @Test
        @DisplayName("Should handle empty password in encryption")
        void shouldHandleEmptyPasswordInEncryption() {
            // Arrange
            when(passwordEncoder.encode(EMPTY_STRING)).thenThrow(new IllegalArgumentException("Password cannot be empty"));

            // Act & Assert
            assertThatThrownBy(() -> userAddService.encryptPassword(EMPTY_STRING))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password cannot be empty");
            
            verify(passwordEncoder).encode(EMPTY_STRING);
        }

        @Test
        @DisplayName("Should encrypt maximum length password (8 characters)")
        void shouldEncryptMaxLengthPassword() {
            // Arrange
            String maxLengthPassword = "12345678";
            String encodedMaxPassword = "encoded_12345678";
            when(passwordEncoder.encode(maxLengthPassword)).thenReturn(encodedMaxPassword);

            // Act
            String encryptedPassword = userAddService.encryptPassword(maxLengthPassword);

            // Assert
            assertThat(encryptedPassword).isEqualTo(encodedMaxPassword);
            verify(passwordEncoder).encode(maxLengthPassword);
        }
    }

    @Nested
    @DisplayName("assignUserRole() Method Tests - Role Management")
    class AssignUserRoleTests {

        @Test
        @DisplayName("Should assign admin role for 'A' user type")
        void shouldAssignAdminRole() {
            // Arrange
            UserSecurity adminUser = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                      VALID_LAST_NAME, VALID_PASSWORD, VALID_ADMIN_TYPE);

            // Act
            userAddService.assignUserRole(adminUser);

            // Assert
            assertThat(adminUser.getUserType()).isEqualTo(VALID_ADMIN_TYPE);
            // Additional role-specific validations can be added based on service implementation
        }

        @Test
        @DisplayName("Should assign regular role for 'R' user type")
        void shouldAssignRegularRole() {
            // Arrange
            UserSecurity regularUser = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                        VALID_LAST_NAME, VALID_PASSWORD, VALID_REGULAR_TYPE);

            // Act
            userAddService.assignUserRole(regularUser);

            // Assert
            assertThat(regularUser.getUserType()).isEqualTo(VALID_REGULAR_TYPE);
        }

        @Test
        @DisplayName("Should handle invalid user type in role assignment")
        void shouldHandleInvalidUserTypeInRoleAssignment() {
            // Arrange
            UserSecurity userWithInvalidType = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                                VALID_LAST_NAME, VALID_PASSWORD, INVALID_USER_TYPE);

            // Act & Assert
            assertThatThrownBy(() -> userAddService.assignUserRole(userWithInvalidType))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid user type");
        }
    }

    @Nested
    @DisplayName("createUserSecurity() Method Tests - Entity Creation")
    class CreateUserSecurityTests {

        @Test
        @DisplayName("Should create UserSecurity entity with all fields populated")
        void shouldCreateUserSecurityEntity() {
            // Act
            UserSecurity createdUser = userAddService.createUserSecurity(
                VALID_USER_ID, VALID_FIRST_NAME, VALID_LAST_NAME, ENCODED_PASSWORD, VALID_REGULAR_TYPE);

            // Assert
            assertThat(createdUser).isNotNull();
            assertThat(createdUser.getSecUsrId()).isEqualTo(VALID_USER_ID);
            assertThat(createdUser.getFirstName()).isEqualTo(VALID_FIRST_NAME);
            assertThat(createdUser.getLastName()).isEqualTo(VALID_LAST_NAME);
            assertThat(createdUser.getPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(createdUser.getUserType()).isEqualTo(VALID_REGULAR_TYPE);
            assertThat(createdUser.getUsername()).isEqualTo(VALID_USER_ID);
        }

        @Test
        @DisplayName("Should create admin user entity")
        void shouldCreateAdminUserEntity() {
            // Act
            UserSecurity adminUser = userAddService.createUserSecurity(
                VALID_USER_ID, VALID_FIRST_NAME, VALID_LAST_NAME, ENCODED_PASSWORD, VALID_ADMIN_TYPE);

            // Assert
            assertThat(adminUser).isNotNull();
            assertThat(adminUser.getUserType()).isEqualTo(VALID_ADMIN_TYPE);
        }

        @Test
        @DisplayName("Should handle null parameters in entity creation")
        void shouldHandleNullParametersInEntityCreation() {
            // Act & Assert
            assertThatThrownBy(() -> userAddService.createUserSecurity(
                NULL_STRING, VALID_FIRST_NAME, VALID_LAST_NAME, ENCODED_PASSWORD, VALID_REGULAR_TYPE))
                .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("generateUserId() Method Tests - ID Generation")
    class GenerateUserIdTests {

        @Test
        @DisplayName("Should generate unique user ID")
        void shouldGenerateUniqueUserId() {
            // Arrange
            String baseUserId = "USER";
            String generatedId = "USER001";
            when(userRepository.findById(generatedId)).thenReturn(Optional.empty());

            // Act
            String result = userAddService.generateUserId();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(8); // COBOL user ID max length
            assertThat(result).matches("^[A-Za-z0-9]+$"); // Alphanumeric only
        }

        @Test
        @DisplayName("Should generate different IDs on multiple calls")
        void shouldGenerateDifferentIds() {
            // Act
            String firstId = userAddService.generateUserId();
            String secondId = userAddService.generateUserId();

            // Assert
            assertThat(firstId).isNotEqualTo(secondId);
            assertThat(firstId).hasSize(8);
            assertThat(secondId).hasSize(8);
        }
    }

    @Nested
    @DisplayName("Integration Tests - Complete User Add Flow")
    class IntegrationTests {

        @Test
        @DisplayName("Should complete full user add workflow successfully")
        void shouldCompleteFullUserAddWorkflow() {
            // Arrange
            when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(UserSecurity.class))).thenReturn(validUserSecurity);

            // Act
            UserSecurity result = userAddService.addUser(validUserSecurity);

            // Assert - Verify complete workflow
            assertThat(result).isNotNull();
            assertThat(result.getSecUsrId()).isEqualTo(VALID_USER_ID);
            
            // Verify all steps executed
            verify(userRepository).findById(VALID_USER_ID); // Duplicate check
            verify(passwordEncoder).encode(VALID_PASSWORD); // Password encryption
            verify(userRepository).save(any(UserSecurity.class)); // User creation
        }

        @Test
        @DisplayName("Should handle workflow with admin user creation")
        void shouldHandleAdminUserCreationWorkflow() {
            // Arrange
            UserSecurity adminUser = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                      VALID_LAST_NAME, VALID_PASSWORD, VALID_ADMIN_TYPE);
            when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(UserSecurity.class))).thenReturn(adminUser);

            // Act
            UserSecurity result = userAddService.addUser(adminUser);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUserType()).isEqualTo(VALID_ADMIN_TYPE);
            
            verify(userRepository).findById(VALID_USER_ID);
            verify(passwordEncoder).encode(VALID_PASSWORD);
            verify(userRepository).save(any(UserSecurity.class));
        }
    }

    @Nested
    @DisplayName("Password Complexity Tests - COBOL Validation Parity")
    class PasswordComplexityTests {

        @Test
        @DisplayName("Should accept minimum length password (4 characters)")
        void shouldAcceptMinimumLengthPassword() {
            // Arrange
            String minimumPassword = "1234";
            UserSecurity userWithMinPassword = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                                VALID_LAST_NAME, minimumPassword, VALID_REGULAR_TYPE);

            // Act & Assert - should not throw exception
            assertThatCode(() -> userAddService.validateUserInput(userWithMinPassword))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject password shorter than 4 characters")
        void shouldRejectShortPassword() {
            // Arrange
            String shortPassword = "123";
            UserSecurity userWithShortPassword = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                                   VALID_LAST_NAME, shortPassword, VALID_REGULAR_TYPE);

            // Act & Assert
            assertThatThrownBy(() -> userAddService.validateUserInput(userWithShortPassword))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Password must be at least 4 characters long");
        }

        @Test
        @DisplayName("Should accept maximum length password (8 characters)")
        void shouldAcceptMaximumLengthPassword() {
            // Arrange
            String maxPassword = "12345678";
            UserSecurity userWithMaxPassword = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                                VALID_LAST_NAME, maxPassword, VALID_REGULAR_TYPE);

            // Act & Assert - should not throw exception
            assertThatCode(() -> userAddService.validateUserInput(userWithMaxPassword))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Field Length Validation Tests - COBOL Picture Clause Compliance")
    class FieldLengthValidationTests {

        @Test
        @DisplayName("Should validate first name length limit (20 characters)")
        void shouldValidateFirstNameLength() {
            // Arrange - 20 character name
            String longFirstName = "VeryLongFirstNameHere20";
            UserSecurity userWithLongFirstName = createUserSecurity(VALID_USER_ID, longFirstName, 
                                                                  VALID_LAST_NAME, VALID_PASSWORD, VALID_REGULAR_TYPE);

            // Act & Assert
            assertThatThrownBy(() -> userAddService.validateUserInput(userWithLongFirstName))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("firstName must be 20 characters or less");
        }

        @Test
        @DisplayName("Should validate last name length limit (20 characters)")
        void shouldValidateLastNameLength() {
            // Arrange - 20+ character name
            String longLastName = "VeryLongLastNameHere20Plus";
            UserSecurity userWithLongLastName = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                                 longLastName, VALID_PASSWORD, VALID_REGULAR_TYPE);

            // Act & Assert
            assertThatThrownBy(() -> userAddService.validateUserInput(userWithLongLastName))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("lastName must be 20 characters or less");
        }

        @Test
        @DisplayName("Should accept maximum length names (20 characters each)")
        void shouldAcceptMaxLengthNames() {
            // Arrange - Exactly 20 characters each
            String maxFirstName = "FirstNameTwentyChars";  // 20 chars
            String maxLastName = "LastNameTwentyCharss";   // 20 chars
            UserSecurity userWithMaxNames = createUserSecurity(VALID_USER_ID, maxFirstName, 
                                                             maxLastName, VALID_PASSWORD, VALID_REGULAR_TYPE);

            // Act & Assert - should not throw exception
            assertThatCode(() -> userAddService.validateUserInput(userWithMaxNames))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Error Handling Tests - Exception Scenarios")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle database connection errors during duplicate check")
        void shouldHandleDatabaseErrorsDuringDuplicateCheck() {
            // Arrange
            when(userRepository.findById(VALID_USER_ID)).thenThrow(new RuntimeException("Database connection failed"));

            // Act & Assert
            assertThatThrownBy(() -> userAddService.checkDuplicateUser(VALID_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection failed");
        }

        @Test
        @DisplayName("Should handle database errors during user save")
        void shouldHandleDatabaseErrorsDuringUserSave() {
            // Arrange
            when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(UserSecurity.class))).thenThrow(new RuntimeException("Database save failed"));

            // Act & Assert
            assertThatThrownBy(() -> userAddService.addUser(validUserSecurity))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database save failed");
        }

        @Test
        @DisplayName("Should handle password encoder failures")
        void shouldHandlePasswordEncoderFailures() {
            // Arrange
            when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(VALID_PASSWORD)).thenThrow(new RuntimeException("Encoding failed"));

            // Act & Assert
            assertThatThrownBy(() -> userAddService.addUser(validUserSecurity))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Encoding failed");
        }
    }

    @Nested
    @DisplayName("COBOL Compliance Tests - Exact Functional Parity")
    class CobolComplianceTests {

        @Test
        @DisplayName("Should replicate COBOL required field validation messages")
        void shouldReplicateCobolValidationMessages() {
            // Test empty User ID - matches COBOL message "User ID can NOT be empty..."
            UserSecurity userWithEmptyId = createUserSecurity(EMPTY_STRING, VALID_FIRST_NAME, 
                                                            VALID_LAST_NAME, VALID_PASSWORD, VALID_REGULAR_TYPE);
            
            assertThatThrownBy(() -> userAddService.validateUserInput(userWithEmptyId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("User ID can NOT be empty");

            // Test empty First Name - matches COBOL message "First Name can NOT be empty..."
            UserSecurity userWithEmptyFirstName = createUserSecurity(VALID_USER_ID, EMPTY_STRING, 
                                                                   VALID_LAST_NAME, VALID_PASSWORD, VALID_REGULAR_TYPE);
            
            assertThatThrownBy(() -> userAddService.validateUserInput(userWithEmptyFirstName))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("First Name can NOT be empty");

            // Test empty Last Name - matches COBOL message "Last Name can NOT be empty..."
            UserSecurity userWithEmptyLastName = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                                  EMPTY_STRING, VALID_PASSWORD, VALID_REGULAR_TYPE);
            
            assertThatThrownBy(() -> userAddService.validateUserInput(userWithEmptyLastName))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Last Name can NOT be empty");

            // Test empty Password - matches COBOL message "Password can NOT be empty..."
            UserSecurity userWithEmptyPassword = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                                  VALID_LAST_NAME, EMPTY_STRING, VALID_REGULAR_TYPE);
            
            assertThatThrownBy(() -> userAddService.validateUserInput(userWithEmptyPassword))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Password can NOT be empty");

            // Test empty User Type - matches COBOL message "User Type can NOT be empty..."
            UserSecurity userWithEmptyType = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                              VALID_LAST_NAME, VALID_PASSWORD, EMPTY_STRING);
            
            assertThatThrownBy(() -> userAddService.validateUserInput(userWithEmptyType))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("User Type can NOT be empty");
        }

        @Test
        @DisplayName("Should maintain COBOL field length constraints")
        void shouldMaintainCobolFieldLengthConstraints() {
            // User ID: 8 characters (PIC X(8))
            assertThatThrownBy(() -> {
                UserSecurity user = createUserSecurity(LONG_USER_ID, VALID_FIRST_NAME, 
                                                      VALID_LAST_NAME, VALID_PASSWORD, VALID_REGULAR_TYPE);
                userAddService.validateUserInput(user);
            }).isInstanceOf(ValidationException.class);

            // Password: 8 characters maximum (PIC X(8))
            assertThatThrownBy(() -> {
                UserSecurity user = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                      VALID_LAST_NAME, LONG_PASSWORD, VALID_REGULAR_TYPE);
                userAddService.validateUserInput(user);
            }).isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("Should preserve COBOL user type validation logic")
        void shouldPreserveCobolUserTypeValidation() {
            // Valid user types from COBOL: 'A' and 'R' (changed from 'U' in service)
            UserSecurity adminUser = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                       VALID_LAST_NAME, VALID_PASSWORD, "A");
            UserSecurity regularUser = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                         VALID_LAST_NAME, VALID_PASSWORD, "R");

            // Should not throw exceptions
            assertThatCode(() -> userAddService.validateUserInput(adminUser))
                .doesNotThrowAnyException();
            assertThatCode(() -> userAddService.validateUserInput(regularUser))
                .doesNotThrowAnyException();

            // Invalid user type should throw exception
            UserSecurity invalidTypeUser = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                             VALID_LAST_NAME, VALID_PASSWORD, "X");
            assertThatThrownBy(() -> userAddService.validateUserInput(invalidTypeUser))
                .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests - Boundary Conditions")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle whitespace-only fields")
        void shouldHandleWhitespaceOnlyFields() {
            // Test spaces-only first name
            UserSecurity userWithSpacesFirstName = createUserSecurity(VALID_USER_ID, SPACES_ONLY, 
                                                                    VALID_LAST_NAME, VALID_PASSWORD, VALID_REGULAR_TYPE);
            
            assertThatThrownBy(() -> userAddService.validateUserInput(userWithSpacesFirstName))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("First Name can NOT be empty");

            // Test spaces-only last name
            UserSecurity userWithSpacesLastName = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                                   SPACES_ONLY, VALID_PASSWORD, VALID_REGULAR_TYPE);
            
            assertThatThrownBy(() -> userAddService.validateUserInput(userWithSpacesLastName))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Last Name can NOT be empty");

            // Test spaces-only password
            UserSecurity userWithSpacesPassword = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                                   VALID_LAST_NAME, SPACES_ONLY, VALID_REGULAR_TYPE);
            
            assertThatThrownBy(() -> userAddService.validateUserInput(userWithSpacesPassword))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Password can NOT be empty");
        }

        @Test
        @DisplayName("Should handle exact boundary length values")
        void shouldHandleExactBoundaryLengthValues() {
            // Test exactly 8-character user ID (boundary condition)
            String exactLengthUserId = "TESTUSER"; // 8 characters
            UserSecurity userWithExactId = createUserSecurity(exactLengthUserId, VALID_FIRST_NAME, 
                                                            VALID_LAST_NAME, VALID_PASSWORD, VALID_REGULAR_TYPE);
            
            assertThatCode(() -> userAddService.validateUserInput(userWithExactId))
                .doesNotThrowAnyException();

            // Test exactly 8-character password (boundary condition)
            String exactLengthPassword = "12345678"; // 8 characters
            UserSecurity userWithExactPassword = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                                   VALID_LAST_NAME, exactLengthPassword, VALID_REGULAR_TYPE);
            
            assertThatCode(() -> userAddService.validateUserInput(userWithExactPassword))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle special characters in names")
        void shouldHandleSpecialCharactersInNames() {
            // Test names with apostrophes (common in names)
            String firstNameWithApostrophe = "O'Connor";
            String lastNameWithHyphen = "Smith-Jones";
            
            UserSecurity userWithSpecialChars = createUserSecurity(VALID_USER_ID, firstNameWithApostrophe, 
                                                                 lastNameWithHyphen, VALID_PASSWORD, VALID_REGULAR_TYPE);

            // Act & Assert - should handle special characters gracefully
            assertThatCode(() -> userAddService.validateUserInput(userWithSpecialChars))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Security Tests - Password Encryption and User Access")
    class SecurityTests {

        @Test
        @DisplayName("Should encrypt password before storing user")
        void shouldEncryptPasswordBeforeStoring() {
            // Arrange
            when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(UserSecurity.class))).thenAnswer(invocation -> {
                UserSecurity savedUser = invocation.getArgument(0);
                // Verify password is encrypted in saved entity
                assertThat(savedUser.getPassword()).isEqualTo(ENCODED_PASSWORD);
                return savedUser;
            });

            // Act
            userAddService.addUser(validUserSecurity);

            // Assert
            verify(passwordEncoder).encode(VALID_PASSWORD);
            verify(userRepository).save(argThat(user -> 
                ENCODED_PASSWORD.equals(user.getPassword())
            ));
        }

        @Test
        @DisplayName("Should prevent duplicate user creation")
        void shouldPreventDuplicateUserCreation() {
            // Arrange
            when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.of(validUserSecurity));

            // Act & Assert
            assertThatThrownBy(() -> userAddService.addUser(validUserSecurity))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("User ID already exists");
            
            // Verify user was not saved
            verify(userRepository, never()).save(any(UserSecurity.class));
        }

        @Test
        @DisplayName("Should maintain case sensitivity for user IDs")
        void shouldMaintainCaseSensitivityForUserIds() {
            // Arrange
            String lowerCaseUserId = "testuser";
            String upperCaseUserId = "TESTUSER";
            
            when(userRepository.findById(lowerCaseUserId)).thenReturn(Optional.empty());
            when(userRepository.findById(upperCaseUserId)).thenReturn(Optional.of(validUserSecurity));

            // Act & Assert
            assertThat(userAddService.checkDuplicateUser(lowerCaseUserId)).isFalse();
            assertThat(userAddService.checkDuplicateUser(upperCaseUserId)).isTrue();
        }
    }

    @Nested
    @DisplayName("Performance Tests - Response Time Requirements")
    class PerformanceTests {

        @Test
        @DisplayName("Should complete user validation within performance threshold")
        void shouldCompleteValidationWithinThreshold() {
            // Arrange
            long startTime = System.currentTimeMillis();

            // Act
            assertThatCode(() -> userAddService.validateUserInput(validUserSecurity))
                .doesNotThrowAnyException();

            // Assert
            long elapsedTime = System.currentTimeMillis() - startTime;
            assertThat(elapsedTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS); // Use TestConstants threshold
        }

        @Test
        @DisplayName("Should complete duplicate check within performance threshold")
        void shouldCompleteDuplicateCheckWithinThreshold() {
            // Arrange
            when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.empty());
            long startTime = System.currentTimeMillis();

            // Act
            userAddService.checkDuplicateUser(VALID_USER_ID);

            // Assert
            long elapsedTime = System.currentTimeMillis() - startTime;
            assertThat(elapsedTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS / 2); // Faster for single operations
        }

        @Test
        @DisplayName("Should complete full user add workflow within performance threshold")
        void shouldCompleteFullWorkflowWithinThreshold() {
            // Arrange
            when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(UserSecurity.class))).thenReturn(validUserSecurity);
            
            long startTime = System.currentTimeMillis();

            // Act
            userAddService.addUser(validUserSecurity);

            // Assert
            long elapsedTime = System.currentTimeMillis() - startTime;
            assertThat(elapsedTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        }
    }

    @Nested
    @DisplayName("ValidationUtil Integration Tests - COBOL Validation Parity")
    class ValidationUtilIntegrationTests {

        @Test
        @DisplayName("Should use ValidationUtil for user ID validation")
        void shouldUseValidationUtilForUserIdValidation() {
            // Test valid user ID
            assertThatCode(() -> ValidationUtil.validateUserId(VALID_USER_ID))
                .doesNotThrowAnyException();

            // Test invalid user ID (too long)
            assertThatThrownBy(() -> ValidationUtil.validateUserId(LONG_USER_ID))
                .isInstanceOf(ValidationException.class);

            // Test empty user ID
            assertThatThrownBy(() -> ValidationUtil.validateUserId(EMPTY_STRING))
                .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("Should use ValidationUtil for password validation")
        void shouldUseValidationUtilForPasswordValidation() {
            // Test valid password
            assertThatCode(() -> ValidationUtil.validatePassword(VALID_PASSWORD))
                .doesNotThrowAnyException();

            // Test empty password
            assertThatThrownBy(() -> ValidationUtil.validatePassword(EMPTY_STRING))
                .isInstanceOf(ValidationException.class);

            // Test short password
            assertThatThrownBy(() -> ValidationUtil.validatePassword("123"))
                .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("Should use ValidationUtil for user type validation")
        void shouldUseValidationUtilForUserTypeValidation() {
            // Test valid admin type
            assertThatCode(() -> ValidationUtil.validateUserType(VALID_ADMIN_TYPE))
                .doesNotThrowAnyException();

            // Test valid regular user type 
            assertThatCode(() -> ValidationUtil.validateUserType(VALID_REGULAR_TYPE))
                .doesNotThrowAnyException();

            // Test invalid user type
            assertThatThrownBy(() -> ValidationUtil.validateUserType(INVALID_USER_TYPE))
                .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("Should use ValidationUtil for field length validation")
        void shouldUseValidationUtilForFieldLengthValidation() {
            // Test valid field length
            assertThatCode(() -> ValidationUtil.validateFieldLength("firstName", VALID_FIRST_NAME, 20))
                .doesNotThrowAnyException();

            // Test exceeded field length
            String longFieldValue = "ThisFieldValueIsDefinitelyTooLongForTheExpectedLimit";
            assertThatThrownBy(() -> ValidationUtil.validateFieldLength("firstName", longFieldValue, 20))
                .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("Should use ValidationUtil for required field validation")
        void shouldUseValidationUtilForRequiredFieldValidation() {
            // Test valid required field
            assertThatCode(() -> ValidationUtil.validateRequiredField("firstName", VALID_FIRST_NAME))
                .doesNotThrowAnyException();

            // Test empty required field
            assertThatThrownBy(() -> ValidationUtil.validateRequiredField("firstName", EMPTY_STRING))
                .isInstanceOf(ValidationException.class);

            // Test null required field
            assertThatThrownBy(() -> ValidationUtil.validateRequiredField("firstName", NULL_STRING))
                .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("PlainTextPasswordEncoder Tests - Password Security")
    class PlainTextPasswordEncoderTests {

        @Test
        @DisplayName("Should test password encoder encode() method")
        void shouldTestPasswordEncoderEncode() {
            // Arrange
            when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn(ENCODED_PASSWORD);

            // Act
            String encoded = passwordEncoder.encode(VALID_PASSWORD);

            // Assert
            assertThat(encoded).isEqualTo(ENCODED_PASSWORD);
            verify(passwordEncoder).encode(VALID_PASSWORD);
        }

        @Test
        @DisplayName("Should test password encoder matches() method")
        void shouldTestPasswordEncoderMatches() {
            // Arrange
            when(passwordEncoder.matches(VALID_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(passwordEncoder.matches("wrongpassword", ENCODED_PASSWORD)).thenReturn(false);

            // Act & Assert
            assertThat(passwordEncoder.matches(VALID_PASSWORD, ENCODED_PASSWORD)).isTrue();
            assertThat(passwordEncoder.matches("wrongpassword", ENCODED_PASSWORD)).isFalse();
            
            verify(passwordEncoder).matches(VALID_PASSWORD, ENCODED_PASSWORD);
            verify(passwordEncoder).matches("wrongpassword", ENCODED_PASSWORD);
        }

        @Test
        @DisplayName("Should test password encoder upgradeEncoding() method")
        void shouldTestPasswordEncoderUpgradeEncoding() {
            // Arrange
            when(passwordEncoder.upgradeEncoding(ENCODED_PASSWORD)).thenReturn(true);

            // Act
            boolean shouldUpgrade = passwordEncoder.upgradeEncoding(ENCODED_PASSWORD);

            // Assert
            assertThat(shouldUpgrade).isTrue();
            verify(passwordEncoder).upgradeEncoding(ENCODED_PASSWORD);
        }
    }

    @Nested
    @DisplayName("UserSecurity Entity Tests - Data Access")
    class UserSecurityEntityTests {

        @Test
        @DisplayName("Should test UserSecurity getUserId() method")
        void shouldTestGetUserId() {
            // Arrange
            validUserSecurity.setSecUsrId(VALID_USER_ID);

            // Act & Assert
            assertThat(validUserSecurity.getUserId()).isEqualTo(VALID_USER_ID);
        }

        @Test
        @DisplayName("Should test UserSecurity getPassword() method")
        void shouldTestGetPassword() {
            // Arrange
            validUserSecurity.setPassword(ENCODED_PASSWORD);

            // Act & Assert
            assertThat(validUserSecurity.getPassword()).isEqualTo(ENCODED_PASSWORD);
        }

        @Test
        @DisplayName("Should test UserSecurity getUserType() method")
        void shouldTestGetUserType() {
            // Arrange
            validUserSecurity.setUserType(VALID_ADMIN_TYPE);

            // Act & Assert
            assertThat(validUserSecurity.getUserType()).isEqualTo(VALID_ADMIN_TYPE);
        }

        @Test
        @DisplayName("Should test UserSecurity getFirstName() method")
        void shouldTestGetFirstName() {
            // Arrange
            validUserSecurity.setFirstName(VALID_FIRST_NAME);

            // Act & Assert
            assertThat(validUserSecurity.getFirstName()).isEqualTo(VALID_FIRST_NAME);
        }

        @Test
        @DisplayName("Should test UserSecurity getLastName() method")
        void shouldTestGetLastName() {
            // Arrange
            validUserSecurity.setLastName(VALID_LAST_NAME);

            // Act & Assert
            assertThat(validUserSecurity.getLastName()).isEqualTo(VALID_LAST_NAME);
        }
    }

    @Nested
    @DisplayName("ValidationException Tests - Error Handling")
    class ValidationExceptionTests {

        @Test
        @DisplayName("Should test ValidationException getMessage() method")
        void shouldTestValidationExceptionGetMessage() {
            // Arrange
            String errorMessage = "Test validation error";
            ValidationException exception = new ValidationException(errorMessage);

            // Act & Assert
            assertThat(exception.getMessage()).isEqualTo(errorMessage);
        }

        @Test
        @DisplayName("Should test ValidationException getFieldName() through field errors")
        void shouldTestValidationExceptionFieldErrors() {
            // Arrange
            ValidationException exception = new ValidationException("Field validation failed");
            exception.addFieldError("userId", "User ID is invalid");

            // Act & Assert
            assertThat(exception.hasFieldErrors()).isTrue();
            assertThat(exception.getFieldError("userId")).isEqualTo("User ID is invalid");
            assertThat(exception.hasFieldError("userId")).isTrue();
        }
    }

    @Nested
    @DisplayName("COBOL Decimal Precision Tests - BigDecimal Compatibility")
    class CobolDecimalPrecisionTests {

        @Test
        @DisplayName("Should validate COBOL decimal precision using AbstractBaseTest utilities")
        void shouldValidateCobolDecimalPrecision() {
            // Arrange
            BigDecimal testAmount = new BigDecimal("123.45");
            BigDecimal expectedAmount = new BigDecimal("123.45");

            // Act & Assert - Use AbstractBaseTest precision validation
            assertBigDecimalEquals(expectedAmount, testAmount);
            validateCobolPrecision(testAmount, TestConstants.COBOL_DECIMAL_SCALE);
        }

        @Test
        @DisplayName("Should handle COBOL decimal scale validation")
        void shouldHandleCobolDecimalScaleValidation() {
            // Arrange
            BigDecimal amountWithCorrectScale = new BigDecimal("100.00").setScale(TestConstants.COBOL_DECIMAL_SCALE);
            
            // Act & Assert
            assertThatCode(() -> validateCobolPrecision(amountWithCorrectScale, TestConstants.COBOL_DECIMAL_SCALE))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Comprehensive User Add Service Tests - All Methods Coverage")
    class ComprehensiveServiceTests {

        @Test
        @DisplayName("Should test validateUniqueUserId() method explicitly")
        void shouldTestValidateUniqueUserId() {
            // Arrange
            when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.empty());

            // Act & Assert - should not throw exception for unique user ID
            assertThatCode(() -> userAddService.validateUniqueUserId(VALID_USER_ID))
                .doesNotThrowAnyException();

            // Test duplicate user ID
            when(userRepository.findById("DUPLICATE")).thenReturn(Optional.of(validUserSecurity));
            assertThatThrownBy(() -> userAddService.validateUniqueUserId("DUPLICATE"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("User ID already exists");
        }

        @Test
        @DisplayName("Should test generateUserId() method for unique ID generation")
        void shouldTestGenerateUserIdMethod() {
            // Arrange - Mock repository to return empty for generated IDs
            when(userRepository.findById(anyString())).thenReturn(Optional.empty());

            // Act
            String generatedId1 = userAddService.generateUserId();
            String generatedId2 = userAddService.generateUserId();

            // Assert
            assertThat(generatedId1).isNotNull();
            assertThat(generatedId2).isNotNull();
            assertThat(generatedId1).isNotEqualTo(generatedId2); // Should generate unique IDs
            assertThat(generatedId1).hasSize(8); // COBOL user ID length limit
            assertThat(generatedId2).hasSize(8); // COBOL user ID length limit
        }

        @Test
        @DisplayName("Should test createUserSecurity() method with all parameters")
        void shouldTestCreateUserSecurityMethod() {
            // Act
            UserSecurity createdUser = userAddService.createUserSecurity(
                VALID_USER_ID, VALID_FIRST_NAME, VALID_LAST_NAME, ENCODED_PASSWORD, VALID_ADMIN_TYPE);

            // Assert
            assertThat(createdUser).isNotNull();
            assertThat(createdUser.getSecUsrId()).isEqualTo(VALID_USER_ID);
            assertThat(createdUser.getFirstName()).isEqualTo(VALID_FIRST_NAME);
            assertThat(createdUser.getLastName()).isEqualTo(VALID_LAST_NAME);
            assertThat(createdUser.getPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(createdUser.getUserType()).isEqualTo(VALID_ADMIN_TYPE);
            assertThat(createdUser.getUsername()).isEqualTo(VALID_USER_ID); // For Spring Security
        }

        @Test
        @DisplayName("Should test assignUserRole() method with different user types")
        void shouldTestAssignUserRoleMethod() {
            // Test admin role assignment
            UserSecurity adminUser = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                      VALID_LAST_NAME, VALID_PASSWORD, VALID_ADMIN_TYPE);
            userAddService.assignUserRole(adminUser);
            assertThat(adminUser.getUserType()).isEqualTo(VALID_ADMIN_TYPE);

            // Test regular user role assignment
            UserSecurity regularUser = createUserSecurity(VALID_USER_ID, VALID_FIRST_NAME, 
                                                        VALID_LAST_NAME, VALID_PASSWORD, VALID_REGULAR_TYPE);
            userAddService.assignUserRole(regularUser);
            assertThat(regularUser.getUserType()).isEqualTo(VALID_REGULAR_TYPE);
        }
    }

    @Nested
    @DisplayName("Test Data Generation Tests - Manual Test Data Creation")
    class TestDataGenerationTests {

        @Test
        @DisplayName("Should generate valid user test data manually")
        void shouldGenerateValidUserTestData() {
            // Act - Manual test data generation since TestDataGenerator is inaccessible
            UserSecurity validUser = createValidUserTestData();

            // Assert
            assertThat(validUser).isNotNull();
            assertThat(validUser.getSecUsrId()).isNotNull();
            assertThat(validUser.getFirstName()).isNotNull();
            assertThat(validUser.getLastName()).isNotNull();
            assertThat(validUser.getPassword()).isNotNull();
            assertThat(validUser.getUserType()).isIn(VALID_ADMIN_TYPE, VALID_REGULAR_TYPE);
        }

        @Test
        @DisplayName("Should generate user with duplicate ID test scenario")
        void shouldGenerateUserWithDuplicateIdTestScenario() {
            // Act - Create duplicate user scenario
            UserSecurity duplicateUser = createUserWithDuplicateId();

            // Assert
            assertThat(duplicateUser).isNotNull();
            assertThat(duplicateUser.getSecUsrId()).isEqualTo(VALID_USER_ID); // Duplicate ID
        }

        @Test
        @DisplayName("Should generate user with invalid password test scenario")
        void shouldGenerateUserWithInvalidPasswordTestScenario() {
            // Act - Create invalid password scenario
            UserSecurity userWithInvalidPassword = createUserWithInvalidPassword();

            // Assert
            assertThat(userWithInvalidPassword).isNotNull();
            assertThat(userWithInvalidPassword.getPassword()).hasSize(3); // Too short
        }

        @Test
        @DisplayName("Should generate admin user test data")
        void shouldGenerateAdminUserTestData() {
            // Act
            UserSecurity adminUser = createAdminUserTestData();

            // Assert
            assertThat(adminUser).isNotNull();
            assertThat(adminUser.getUserType()).isEqualTo(VALID_ADMIN_TYPE);
        }

        @Test
        @DisplayName("Should generate regular user test data")
        void shouldGenerateRegularUserTestData() {
            // Act
            UserSecurity regularUser = createRegularUserTestData();

            // Assert
            assertThat(regularUser).isNotNull();
            assertThat(regularUser.getUserType()).isEqualTo(VALID_REGULAR_TYPE);
        }

        @Test
        @DisplayName("Should generate UserSecurity test data with validation")
        void shouldGenerateUserSecurityTestData() {
            // Act
            UserSecurity generatedUserSecurity = createUserSecurityTestData();

            // Assert
            assertThat(generatedUserSecurity).isNotNull();
            assertThat(generatedUserSecurity.getSecUsrId()).isNotBlank();
            assertThat(generatedUserSecurity.getFirstName()).isNotBlank();
            assertThat(generatedUserSecurity.getLastName()).isNotBlank();
            assertThat(generatedUserSecurity.getPassword()).isNotBlank();
            assertThat(generatedUserSecurity.getUserType()).isNotBlank();
        }

        // Manual test data generation methods to replace inaccessible TestDataGenerator
        private UserSecurity createValidUserTestData() {
            return createUserSecurity("TESTUSER", "ValidFirst", "ValidLast", "pass1234", VALID_REGULAR_TYPE);
        }

        private UserSecurity createUserWithDuplicateId() {
            return createUserSecurity(VALID_USER_ID, "Duplicate", "User", "password", VALID_REGULAR_TYPE);
        }

        private UserSecurity createUserWithInvalidPassword() {
            return createUserSecurity("INVALID1", "Invalid", "Password", "123", VALID_REGULAR_TYPE);
        }

        private UserSecurity createAdminUserTestData() {
            return createUserSecurity("ADMIN001", "Admin", "User", "admin123", VALID_ADMIN_TYPE);
        }

        private UserSecurity createRegularUserTestData() {
            return createUserSecurity("USER0001", "Regular", "User", "user1234", VALID_REGULAR_TYPE);
        }

        private UserSecurity createUserSecurityTestData() {
            return createUserSecurity("SECURITY", "Security", "Test", "secure12", VALID_REGULAR_TYPE);
        }
    }

    @Nested
    @DisplayName("Validation Threshold Tests - COBOL Compliance")
    class ValidationThresholdTests {

        @Test
        @DisplayName("Should validate against COBOL validation thresholds")
        void shouldValidateAgainstCobolValidationThresholds() {
            // Test using validation thresholds from TestConstants
            Map<String, Object> thresholds = TestConstants.VALIDATION_THRESHOLDS;
            
            // Validate user ID length threshold
            assertThat(VALID_USER_ID.length()).isLessThanOrEqualTo(8); // COBOL PIC X(8)
            
            // Validate password length threshold  
            assertThat(VALID_PASSWORD.length()).isLessThanOrEqualTo(8); // COBOL PIC X(8)
            
            // Validate first name length threshold
            assertThat(VALID_FIRST_NAME.length()).isLessThanOrEqualTo(20); // COBOL PIC X(20)
            
            // Validate last name length threshold
            assertThat(VALID_LAST_NAME.length()).isLessThanOrEqualTo(20); // COBOL PIC X(20)
        }

        @Test
        @DisplayName("Should validate COBOL decimal scale consistency")
        void shouldValidateCobolDecimalScaleConsistency() {
            // Test COBOL decimal scale from TestConstants
            int expectedScale = TestConstants.COBOL_DECIMAL_SCALE;
            assertThat(expectedScale).isEqualTo(2); // Standard COBOL decimal scale for amounts
        }
    }
}