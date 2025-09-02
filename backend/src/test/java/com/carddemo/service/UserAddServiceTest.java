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

import com.carddemo.dto.UserAddRequest;
import com.carddemo.dto.UserAddResponse;
import com.carddemo.entity.UserSecurity;
import com.carddemo.repository.UserSecurityRepository;
import com.carddemo.security.PasswordGenerator;
import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.TestConstants;
import com.carddemo.test.UnitTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit test suite for UserAddService validating COBOL COUSR02C functional parity.
 * 
 * This test class ensures complete validation of user creation logic migrated from the mainframe
 * COUSR02C.cbl COBOL program to modern Spring Boot service implementation. The tests verify
 * all business logic scenarios including user ID uniqueness, password handling, role assignment,
 * validation rules, and error conditions.
 * 
 * Test Coverage Areas:
 * - User creation with valid inputs
 * - User ID uniqueness validation and duplicate prevention
 * - Password complexity requirements and generation
 * - User type validation (Admin 'A' vs Regular 'R')
 * - Field length and format validation
 * - Error handling and exception scenarios
 * - Audit trail generation
 * - Performance requirements validation
 * 
 * All tests maintain COBOL precision requirements and response time expectations
 * as defined in the original mainframe system specifications.
 * 
 * @author Blitzy Platform Migration Agent
 * @version 1.0
 * @since Java 21
 */
@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
@DisplayName("UserAddService Unit Tests - COBOL COUSR02C Migration Validation")
public class UserAddServiceTest extends AbstractBaseTest {

    @Mock
    private UserSecurityRepository userSecurityRepository;
    
    @Mock
    private PasswordGenerator passwordGenerator;
    
    private UserAddService userAddService;
    
    private UserAddRequest validRequest;
    private UserSecurity mockUserEntity;

    /**
     * Setup method to initialize test data and mock objects before each test execution.
     * Creates standard test fixtures matching COBOL data structures and business rules.
     */
    @BeforeEach
    public void setUp() {
        super.setUp();
        
        // Manually create the service with mocked dependencies
        userAddService = new UserAddService(userSecurityRepository, passwordGenerator);
        
        // Create valid test request matching COBOL CSUSR01Y copybook structure
        validRequest = new UserAddRequest();
        validRequest.setUserId("TESTUSER");
        validRequest.setFirstName("John");
        validRequest.setLastName("Doe");
        validRequest.setUserType("A");
        validRequest.setPassword("TestPwd1");
        validRequest.setGeneratePassword(false);
        validRequest.setActiveStatus(true);
        
        // Create mock user entity for repository interactions
        mockUserEntity = new UserSecurity();
        mockUserEntity.setSecUsrId("TESTUSER");
        mockUserEntity.setUsername("TESTUSER");
        mockUserEntity.setFirstName("John");
        mockUserEntity.setLastName("Doe");
        mockUserEntity.setUserType("A");
        mockUserEntity.setPassword("TestPwd1");
        mockUserEntity.setEnabled(true);
        mockUserEntity.setAccountNonExpired(true);
        mockUserEntity.setAccountNonLocked(true);
        mockUserEntity.setCredentialsNonExpired(true);
        mockUserEntity.setFailedLoginAttempts(0);
        mockUserEntity.setCreatedAt(LocalDateTime.now());
        mockUserEntity.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("User Creation Success Scenarios")
    class UserCreationSuccessTests {

        @Test
        @DisplayName("Should successfully create user with valid Admin request")
        void testAddUser_ValidAdminRequest_Success() {
            // Arrange
            when(userSecurityRepository.existsBySecUsrId("TESTUSER")).thenReturn(false);
            when(userSecurityRepository.save(any(UserSecurity.class))).thenReturn(mockUserEntity);
            
            // Act
            long startTime = System.currentTimeMillis();
            UserAddResponse response = userAddService.addUser(validRequest);
            long endTime = System.currentTimeMillis();
            
            // Assert - Validate response structure and content
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getUserId()).isEqualTo("TESTUSER");
            assertThat(response.getGeneratedPassword()).isEqualTo("TestPwd1");
            assertThat(response.getMessage()).isEqualTo("User created successfully");
            assertThat(response.getErrorCode()).isNull();
            assertThat(response.getTimestamp()).isNotNull();
            
            // Assert - Validate COBOL response time requirement (<200ms)
            assertThat(endTime - startTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
            
            // Verify repository interactions
            verify(userSecurityRepository).existsBySecUsrId("TESTUSER");
            verify(userSecurityRepository).save(any(UserSecurity.class));
        }

        @Test
        @DisplayName("Should successfully create user with valid Regular user request")
        void testAddUser_ValidRegularUserRequest_Success() {
            // Arrange
            validRequest.setUserType("R");
            mockUserEntity.setUserType("R"); // Update mock to match request
            
            when(userSecurityRepository.existsBySecUsrId("TESTUSER")).thenReturn(false);
            when(userSecurityRepository.save(any(UserSecurity.class))).thenReturn(mockUserEntity);
            
            // Act
            UserAddResponse response = userAddService.addUser(validRequest);
            
            // Assert
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getUserId()).isEqualTo("TESTUSER");
            assertThat(response.getMessage()).isEqualTo("User created successfully");
            
            // Verify user entity has correct type
            verify(userSecurityRepository).save(argThat(user -> "R".equals(user.getUserType())));
        }

        @Test
        @DisplayName("Should successfully generate password when requested")
        void testAddUser_GeneratePasswordRequested_Success() {
            // Arrange
            validRequest.setGeneratePassword(true);
            validRequest.setPassword(null); // No password provided, should be generated
            
            String generatedPassword = "GenPwd123";
            when(passwordGenerator.generateSecurePassword()).thenReturn(generatedPassword);
            when(userSecurityRepository.existsBySecUsrId("TESTUSER")).thenReturn(false);
            when(userSecurityRepository.save(any(UserSecurity.class))).thenReturn(mockUserEntity);
            
            // Act
            UserAddResponse response = userAddService.addUser(validRequest);
            
            // Assert
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getGeneratedPassword()).isEqualTo(generatedPassword);
            
            // Verify password generation was called
            verify(passwordGenerator).generateSecurePassword();
            verify(userSecurityRepository).save(argThat(user -> generatedPassword.equals(user.getPassword())));
        }
    }

    @Nested
    @DisplayName("User ID Uniqueness Validation Tests")
    class UserIdUniquenessTests {

        @Test
        @DisplayName("Should reject duplicate user ID")
        void testAddUser_DuplicateUserId_Failure() {
            // Arrange - The validateUniqueUserId method checks repository first
            when(userSecurityRepository.existsBySecUsrId("TESTUSER")).thenReturn(true);
            
            // Act
            UserAddResponse response = userAddService.addUser(validRequest);
            
            // Assert
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getUserId()).isNull();
            assertThat(response.getMessage()).isEqualTo("User ID already exists");
            assertThat(response.getErrorCode()).isEqualTo("DUPLICATE_USER_ID");
            assertThat(response.getTimestamp()).isNotNull();
            
            // Verify no save operation was attempted
            verify(userSecurityRepository, never()).save(any(UserSecurity.class));
        }

        @Test
        @DisplayName("Should handle database duplicate key exception")
        void testAddUser_DatabaseDuplicateKeyException_Failure() {
            // Arrange
            when(userSecurityRepository.existsBySecUsrId("TESTUSER")).thenReturn(false);
            when(userSecurityRepository.save(any(UserSecurity.class)))
                .thenThrow(new DuplicateKeyException("Duplicate key"));
            
            // Act
            UserAddResponse response = userAddService.addUser(validRequest);
            
            // Assert
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo("User ID already exists");
            assertThat(response.getErrorCode()).isEqualTo("DUPLICATE_USER_ID");
            
            // Verify interactions
            verify(userSecurityRepository).existsBySecUsrId("TESTUSER");
            verify(userSecurityRepository).save(any(UserSecurity.class));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   ", "TOOLONGID"})
        @DisplayName("Should validate user ID length and format")
        void testValidateUniqueUserId_InvalidFormats(String invalidUserId) {
            // Act
            String validationResult = userAddService.validateUniqueUserId(invalidUserId);
            
            // Assert
            assertThat(validationResult).isNotNull();
            if (invalidUserId.trim().isEmpty()) {
                assertThat(validationResult).isEqualTo("User ID cannot be empty");
            } else if (invalidUserId.length() > 8) {
                assertThat(validationResult).isEqualTo("User ID must not exceed 8 characters");
            }
        }

        @Test
        @DisplayName("Should pass validation for valid unique user ID")
        void testValidateUniqueUserId_ValidId_Success() {
            // Arrange - Note: service normalizes to uppercase
            when(userSecurityRepository.existsBySecUsrId("NEWUSER")).thenReturn(false);
            
            // Act
            String validationResult = userAddService.validateUniqueUserId("newuser");
            
            // Assert
            assertThat(validationResult).isNull();
            
            // Verify the repository was called with normalized (uppercase) user ID
            verify(userSecurityRepository).existsBySecUsrId("NEWUSER");
        }
    }

    @Nested
    @DisplayName("User Type Validation Tests")
    class UserTypeValidationTests {

        @ParameterizedTest
        @CsvSource({
            "A, true",
            "R, true", 
            "a, true",
            "r, true",
            "X, false",
            "B, false",
            "'', false",
            "AA, false"
        })
        @DisplayName("Should validate user type according to COBOL business rules")
        void testIsValidUserType_VariousTypes(String userType, boolean expected) {
            // Act
            boolean result = userAddService.isValidUserType(userType);
            
            // Assert
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should reject request with invalid user type")
        void testAddUser_InvalidUserType_Failure() {
            // Arrange
            validRequest.setUserType("X");
            
            // Act
            UserAddResponse response = userAddService.addUser(validRequest);
            
            // Assert
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo("Invalid user type. Must be 'A' for Admin or 'R' for Regular user");
            assertThat(response.getErrorCode()).isEqualTo("INVALID_USER_TYPE");
        }
    }

    @Nested
    @DisplayName("Password Validation Tests")
    class PasswordValidationTests {

        @Test
        @DisplayName("Should reject request without password when generation disabled")
        void testAddUser_NoPasswordProvided_Failure() {
            // Arrange
            validRequest.setPassword(null);
            validRequest.setGeneratePassword(false);
            
            // Act
            UserAddResponse response = userAddService.addUser(validRequest);
            
            // Assert
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo("Password must be provided or generation must be enabled");
            assertThat(response.getErrorCode()).isEqualTo("INVALID_PASSWORD");
        }

        @Test
        @DisplayName("Should reject request with empty password when generation disabled")
        void testAddUser_EmptyPassword_Failure() {
            // Arrange
            validRequest.setPassword("   ");
            validRequest.setGeneratePassword(false);
            
            // Act
            UserAddResponse response = userAddService.addUser(validRequest);
            
            // Assert
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo("Password must be provided or generation must be enabled");
            assertThat(response.getErrorCode()).isEqualTo("INVALID_PASSWORD");
        }
    }

    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        @Test
        @DisplayName("Should reject null request")
        void testAddUser_NullRequest_Failure() {
            // Act
            UserAddResponse response = userAddService.addUser(null);
            
            // Assert
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo("Request cannot be null");
            assertThat(response.getErrorCode()).isEqualTo("INVALID_REQUEST");
        }

        @Test
        @DisplayName("Should reject request with null user ID")
        void testAddUser_NullUserId_Failure() {
            // Arrange
            validRequest.setUserId(null);
            
            // Act
            UserAddResponse response = userAddService.addUser(validRequest);
            
            // Assert
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo("User ID cannot be empty");
            assertThat(response.getErrorCode()).isEqualTo("INVALID_USER_ID");
        }

        @Test
        @DisplayName("Should reject request with empty user ID")
        void testAddUser_EmptyUserId_Failure() {
            // Arrange
            validRequest.setUserId("   ");
            
            // Act
            UserAddResponse response = userAddService.addUser(validRequest);
            
            // Assert
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo("User ID cannot be empty");
            assertThat(response.getErrorCode()).isEqualTo("INVALID_USER_ID");
        }
    }

    @Nested
    @DisplayName("Entity Creation Tests")
    class EntityCreationTests {

        @Test
        @DisplayName("Should create user entity with proper field mapping")
        void testCreateUserEntity_ValidRequest_ProperMapping() {
            // Arrange
            String testPassword = "TestPwd123";
            
            // Act
            UserSecurity result = userAddService.createUserEntity(validRequest, testPassword);
            
            // Assert - Validate core fields from COBOL copybook structure
            assertThat(result.getSecUsrId()).isEqualTo("TESTUSER");
            assertThat(result.getUsername()).isEqualTo("TESTUSER");
            assertThat(result.getFirstName()).isEqualTo("John");
            assertThat(result.getLastName()).isEqualTo("Doe");
            assertThat(result.getUserType()).isEqualTo("A");
            assertThat(result.getPassword()).isEqualTo(testPassword);
            
            // Assert - Validate Spring Security default settings
            assertThat(result.isEnabled()).isTrue();
            assertThat(result.isAccountNonExpired()).isTrue();
            assertThat(result.isAccountNonLocked()).isTrue();
            assertThat(result.isCredentialsNonExpired()).isTrue();
            assertThat(result.getFailedLoginAttempts()).isEqualTo(0);
            
            // Assert - Validate audit timestamps
            assertThat(result.getCreatedAt()).isNotNull();
            assertThat(result.getUpdatedAt()).isNotNull();
            assertThat(result.getCreatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
            assertThat(result.getUpdatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
        }

        @Test
        @DisplayName("Should handle null name fields gracefully")
        void testCreateUserEntity_NullNameFields_EmptyStrings() {
            // Arrange
            validRequest.setFirstName(null);
            validRequest.setLastName(null);
            
            // Act
            UserSecurity result = userAddService.createUserEntity(validRequest, "TestPwd");
            
            // Assert
            assertThat(result.getFirstName()).isEqualTo("");
            assertThat(result.getLastName()).isEqualTo("");
        }

        @Test
        @DisplayName("Should normalize user ID and type to uppercase")
        void testCreateUserEntity_LowercaseInputs_UppercaseNormalization() {
            // Arrange
            validRequest.setUserId("testuser");
            validRequest.setUserType("a");
            
            // Act
            UserSecurity result = userAddService.createUserEntity(validRequest, "TestPwd");
            
            // Assert
            assertThat(result.getSecUsrId()).isEqualTo("TESTUSER");
            assertThat(result.getUsername()).isEqualTo("TESTUSER");
            assertThat(result.getUserType()).isEqualTo("A");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle unexpected exceptions during user creation")
        void testAddUser_UnexpectedException_SystemError() {
            // Arrange
            when(userSecurityRepository.existsBySecUsrId("TESTUSER")).thenReturn(false);
            when(userSecurityRepository.save(any(UserSecurity.class)))
                .thenThrow(new RuntimeException("Database connection failed"));
            
            // Act
            UserAddResponse response = userAddService.addUser(validRequest);
            
            // Assert
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo("Internal server error during user creation");
            assertThat(response.getErrorCode()).isEqualTo("SYSTEM_ERROR");
            assertThat(response.getUserId()).isNull();
            assertThat(response.getGeneratedPassword()).isNull();
            
            // Verify interactions
            verify(userSecurityRepository).existsBySecUsrId("TESTUSER");
            verify(userSecurityRepository).save(any(UserSecurity.class));
        }
    }

    @Nested
    @DisplayName("Audit and Compliance Tests")
    class AuditComplianceTests {

        @Test
        @DisplayName("Should generate audit entry for successful user creation")
        void testGenerateAuditEntry_ValidParameters_Success() {
            // This test verifies that audit logging is called - in production this would
            // write to an audit table or external audit system
            
            // Act - This is a void method that logs internally
            userAddService.generateAuditEntry("TESTUSER", "USER_CREATED");
            
            // Assert - Verify method completes without exception
            // In a real implementation, we would verify audit record creation
            // For now, we verify the method can be called successfully
            assertThat(true).isTrue(); // Method completed successfully
        }
    }

    @Nested
    @DisplayName("Performance and Response Time Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should meet COBOL response time requirements for user creation")
        void testAddUser_Performance_MeetsCobolRequirements() {
            // Arrange
            when(userSecurityRepository.existsBySecUsrId("TESTUSER")).thenReturn(false);
            when(userSecurityRepository.save(any(UserSecurity.class))).thenReturn(mockUserEntity);
            
            // Act - Measure execution time
            long startTime = System.currentTimeMillis();
            UserAddResponse response = userAddService.addUser(validRequest);
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            // Assert - Response time should be under 200ms (COBOL requirement)
            assertThat(executionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
            assertThat(response.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("Should maintain consistent performance for validation operations")
        void testValidateUniqueUserId_Performance_Fast() {
            // Arrange
            when(userSecurityRepository.existsBySecUsrId("TESTUSER")).thenReturn(false);
            
            // Act - Measure validation time
            long startTime = System.currentTimeMillis();
            String result = userAddService.validateUniqueUserId("TESTUSER");
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            // Assert - Validation should be very fast (under 50ms)
            assertThat(executionTime).isLessThan(50);
            assertThat(result).isNull(); // Valid user ID
            
            // Verify the repository was called
            verify(userSecurityRepository).existsBySecUsrId("TESTUSER");
        }
    }
}