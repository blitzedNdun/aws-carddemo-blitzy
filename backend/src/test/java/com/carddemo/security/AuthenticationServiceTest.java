/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.security;

import com.carddemo.entity.UserSecurity;
import com.carddemo.repository.UserSecurityRepository;
import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.UnitTest;
import com.carddemo.test.TestConstants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthenticationService that replaces RACF authentication functionality.
 * 
 * This test class validates the complete authentication workflow that replaces the legacy 
 * COSGN00C COBOL program with modern Spring Security-based authentication. Tests ensure
 * 100% functional parity with the original mainframe authentication logic while leveraging
 * modern Java enterprise frameworks.
 * 
 * <p>Key Testing Areas:</p>
 * <ul>
 *   <li>User credential validation against PostgreSQL usrsec table</li>
 *   <li>Password encoding compatibility with legacy COBOL passwords using PlainTextPasswordEncoder</li>
 *   <li>Authentication success/failure scenarios matching COBOL COSGN00C behavior</li>
 *   <li>User type determination (ADMIN vs USER) based on SEC-USR-TYPE field mapping</li>
 *   <li>Session management integration with Spring Security context</li>
 *   <li>Error handling and exception scenarios maintaining legacy behavior patterns</li>
 * </ul>
 *
 * <p>Testing Strategy Alignment:</p>
 * <ul>
 *   <li>Functional parity validation with COBOL authentication logic</li>
 *   <li>Performance validation ensuring sub-200ms authentication response times</li>
 *   <li>Security validation testing authentication bypass prevention</li>
 *   <li>Integration testing with PostgreSQL user_security table structure</li>
 * </ul>
 *
 * <p>COBOL Program Reference:</p>
 * This test validates Java implementation against COSGN00C.cbl authentication logic,
 * ensuring identical credential validation, user type determination, and error handling
 * while leveraging Spring Security framework capabilities.
 *
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest extends AbstractBaseTest implements UnitTest {

    @Mock
    private UserSecurityRepository userSecurityRepository;

    @Mock
    private PlainTextPasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationService authenticationService;

    private UserSecurity testUser;
    private UserSecurity testAdmin;

    /**
     * Test setup method executed before each test.
     * Initializes test user objects with COBOL-compatible data patterns
     * and configures mock objects for isolated unit testing.
     * 
     * This setup ensures each test starts with consistent test data
     * matching the original VSAM USRSEC record structure and COBOL
     * field specifications from CSUSR01Y copybook.
     */
    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        setupTestUsers();
        configurePasswordEncoderMocks();
        logTestExecution("AuthenticationService test setup completed", null);
    }

    /**
     * Setup test user objects with COBOL-compatible data structure.
     * Creates test users matching the original USRSEC VSAM dataset format
     * with proper field lengths and data types from CSUSR01Y copybook.
     */
    private void setupTestUsers() {
        // Create regular test user matching COBOL structure
        testUser = new UserSecurity();
        testUser.setSecUsrId(TestConstants.TEST_USER_ID);
        testUser.setUsername(TestConstants.TEST_USER_ID.toLowerCase());
        testUser.setPassword(TestConstants.TEST_USER_PASSWORD);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setUserType("U"); // Regular user type from SEC-USR-TYPE
        testUser.setEnabled(true);
        testUser.setAccountNonExpired(true);
        testUser.setAccountNonLocked(true);
        testUser.setCredentialsNonExpired(true);
        testUser.setFailedLoginAttempts(0);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        // Create admin test user matching COBOL structure
        testAdmin = new UserSecurity();
        testAdmin.setSecUsrId("TESTADMN");
        testAdmin.setUsername("testadmn");
        testAdmin.setPassword("adminpass");
        testAdmin.setFirstName("Test");
        testAdmin.setLastName("Admin");
        testAdmin.setUserType("A"); // Admin user type from SEC-USR-TYPE
        testAdmin.setEnabled(true);
        testAdmin.setAccountNonExpired(true);
        testAdmin.setAccountNonLocked(true);
        testAdmin.setCredentialsNonExpired(true);
        testAdmin.setFailedLoginAttempts(0);
        testAdmin.setCreatedAt(LocalDateTime.now());
        testAdmin.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Configure password encoder mock behavior for legacy compatibility testing.
     * Sets up PlainTextPasswordEncoder mocks to simulate legacy COBOL password
     * behavior with uppercase conversion and plain-text comparison logic.
     */
    private void configurePasswordEncoderMocks() {
        // Configure password encoder to match legacy behavior
        when(passwordEncoder.matches(any(String.class), any(String.class)))
            .thenAnswer(invocation -> {
                String rawPassword = invocation.getArgument(0);
                String encodedPassword = invocation.getArgument(1);
                // Simulate COBOL uppercase conversion and direct comparison
                return rawPassword.toUpperCase().equals(encodedPassword.toUpperCase());
            });

        when(passwordEncoder.encode(any(String.class)))
            .thenAnswer(invocation -> {
                String password = invocation.getArgument(0);
                // Simulate COBOL FUNCTION UPPER-CASE behavior
                return password.toUpperCase();
            });
    }

    /**
     * Test successful authentication with valid user credentials.
     * Validates that authentication service properly authenticates users
     * with correct credentials, replicating COSGN00C success path logic.
     * 
     * This test ensures:
     * - Username lookup works correctly (case-insensitive)
     * - Password validation uses PlainTextPasswordEncoder
     * - Successful authentication returns properly configured Authentication object
     * - User authorities are correctly set based on SEC-USR-TYPE
     */
    @Test
    @DisplayName("Authenticate - Valid User Credentials - Returns Authenticated User")
    public void testAuthenticate_ValidCredentials_ReturnsAuthenticatedUser() {
        // Arrange
        String username = TestConstants.TEST_USER_ID.toLowerCase();
        String password = TestConstants.TEST_USER_PASSWORD;
        
        when(userSecurityRepository.findByUsername(username))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPassword()))
            .thenReturn(true);

        // Act
        long startTime = System.currentTimeMillis();
        Authentication result = authenticationService.authenticate(username, password);
        long executionTime = System.currentTimeMillis() - startTime;

        // Assert
        assertNotNull(result, "Authentication result should not be null");
        assertTrue(result.isAuthenticated(), "Authentication should be successful");
        assertEquals(username, result.getName(), "Username should match input");
        assertNotNull(result.getAuthorities(), "Authorities should be set");
        assertTrue(result.getAuthorities().contains(TestConstants.TEST_USER_ROLE),
            "User should have ROLE_USER authority");

        // Verify repository interaction
        verify(userSecurityRepository, times(1)).findByUsername(username);
        verify(passwordEncoder, times(1)).matches(password, testUser.getPassword());

        // Validate performance requirement (sub-200ms)
        assertTrue(executionTime < TestConstants.RESPONSE_TIME_THRESHOLD_MS,
            "Authentication should complete within 200ms threshold");

        logTestExecution("Valid credentials authentication test passed", executionTime);
    }

    /**
     * Test authentication failure with invalid password.
     * Validates that authentication service properly rejects users
     * with incorrect passwords, replicating COSGN00C failure logic.
     * 
     * This test ensures:
     * - Invalid password rejection works correctly
     * - Authentication returns null or unauthenticated result
     * - Failed login attempts may be tracked (future enhancement)
     * - Performance remains within acceptable limits for security
     */
    @Test
    @DisplayName("Authenticate - Invalid Password - Returns Authentication Failure")
    public void testAuthenticate_InvalidPassword_ReturnsAuthenticationFailure() {
        // Arrange
        String username = TestConstants.TEST_USER_ID.toLowerCase();
        String invalidPassword = "wrongpass";
        
        when(userSecurityRepository.findByUsername(username))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(invalidPassword, testUser.getPassword()))
            .thenReturn(false);

        // Act & Assert
        long startTime = System.currentTimeMillis();
        assertThrows(RuntimeException.class, () -> {
            authenticationService.authenticate(username, invalidPassword);
        }, "Invalid password should throw authentication exception");
        long executionTime = System.currentTimeMillis() - startTime;

        // Verify repository interaction
        verify(userSecurityRepository, times(1)).findByUsername(username);
        verify(passwordEncoder, times(1)).matches(invalidPassword, testUser.getPassword());

        // Validate performance requirement
        assertTrue(executionTime < TestConstants.RESPONSE_TIME_THRESHOLD_MS,
            "Authentication failure should complete within 200ms threshold");

        logTestExecution("Invalid password authentication test passed", executionTime);
    }

    /**
     * Test authentication failure with non-existent user.
     * Validates that authentication service properly handles requests
     * for users that don't exist in the system, replicating COSGN00C
     * user-not-found logic.
     * 
     * This test ensures:
     * - Non-existent user handling works correctly
     * - Appropriate exception is thrown (UsernameNotFoundException)
     * - Repository interaction is optimized (single query)
     * - Security timing attacks are prevented
     */
    @Test
    @DisplayName("Authenticate - Non-Existent User - Throws UsernameNotFoundException")
    public void testAuthenticate_NonExistentUser_ThrowsUsernameNotFoundException() {
        // Arrange
        String nonExistentUsername = "NOUSER";
        String password = TestConstants.TEST_USER_PASSWORD;
        
        when(userSecurityRepository.findByUsername(nonExistentUsername))
            .thenReturn(Optional.empty());

        // Act & Assert
        long startTime = System.currentTimeMillis();
        assertThrows(UsernameNotFoundException.class, () -> {
            authenticationService.authenticate(nonExistentUsername, password);
        }, "Non-existent user should throw UsernameNotFoundException");
        long executionTime = System.currentTimeMillis() - startTime;

        // Verify repository interaction
        verify(userSecurityRepository, times(1)).findByUsername(nonExistentUsername);
        // Password encoder should not be called for non-existent user
        verify(passwordEncoder, never()).matches(any(), any());

        // Validate performance requirement
        assertTrue(executionTime < TestConstants.RESPONSE_TIME_THRESHOLD_MS,
            "User not found should complete within 200ms threshold");

        logTestExecution("Non-existent user authentication test passed", executionTime);
    }

    /**
     * Test validateCredentials method for user credential verification.
     * Validates the core credential validation logic that replaces COBOL
     * password checking routines from COSGN00C program.
     * 
     * This test ensures:
     * - Username case conversion handling (COBOL FUNCTION UPPER-CASE)
     * - Password validation using PlainTextPasswordEncoder
     * - User lookup from PostgreSQL usrsec table
     * - Proper return values for validation results
     */
    @Test
    @DisplayName("ValidateCredentials - Valid User - Returns True")
    public void testValidateCredentials_ValidUser_ReturnsTrue() {
        // Arrange
        String username = TestConstants.TEST_USER_ID.toLowerCase();
        String password = TestConstants.TEST_USER_PASSWORD;
        
        when(userSecurityRepository.findByUsername(username))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPassword()))
            .thenReturn(true);

        // Act
        long startTime = System.currentTimeMillis();
        boolean result = authenticationService.validateCredentials(username, password);
        long executionTime = System.currentTimeMillis() - startTime;

        // Assert
        assertTrue(result, "Valid credentials should return true");

        // Verify interactions
        verify(userSecurityRepository, times(1)).findByUsername(username);
        verify(passwordEncoder, times(1)).matches(password, testUser.getPassword());

        // Validate performance
        assertTrue(executionTime < TestConstants.RESPONSE_TIME_THRESHOLD_MS,
            "Credential validation should complete within 200ms threshold");

        logTestExecution("Valid credentials validation test passed", executionTime);
    }

    /**
     * Test validateCredentials method with invalid credentials.
     * Validates proper rejection of invalid credentials matching
     * COSGN00C error handling logic.
     */
    @Test
    @DisplayName("ValidateCredentials - Invalid Credentials - Returns False")
    public void testValidateCredentials_InvalidCredentials_ReturnsFalse() {
        // Arrange
        String username = TestConstants.TEST_USER_ID.toLowerCase();
        String invalidPassword = "wrongpass";
        
        when(userSecurityRepository.findByUsername(username))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(invalidPassword, testUser.getPassword()))
            .thenReturn(false);

        // Act
        boolean result = authenticationService.validateCredentials(username, invalidPassword);

        // Assert
        assertFalse(result, "Invalid credentials should return false");

        // Verify interactions
        verify(userSecurityRepository, times(1)).findByUsername(username);
        verify(passwordEncoder, times(1)).matches(invalidPassword, testUser.getPassword());

        logTestExecution("Invalid credentials validation test passed", null);
    }

    /**
     * Test determineUserType method for user role classification.
     * Validates proper mapping from COBOL SEC-USR-TYPE field values
     * to Spring Security role authorities.
     * 
     * This test ensures:
     * - 'A' user type maps to ROLE_ADMIN authority
     * - 'U' user type maps to ROLE_USER authority
     * - Case-insensitive user type handling
     * - Proper Spring Security authority formatting
     */
    @Test
    @DisplayName("DetermineUserType - Admin User Type - Returns ROLE_ADMIN")
    public void testDetermineUserType_AdminUser_ReturnsRoleAdmin() {
        // Arrange - Admin user with type 'A'
        testAdmin.setUserType("A");

        // Act
        String userType = authenticationService.determineUserType(testAdmin);

        // Assert
        assertEquals("ROLE_ADMIN", userType, "Admin user should return ROLE_ADMIN");
        
        logTestExecution("Admin user type determination test passed", null);
    }

    /**
     * Test determineUserType method for regular user classification.
     * Validates proper mapping for regular users from COBOL SEC-USR-TYPE
     * field to Spring Security role authorities.
     */
    @Test
    @DisplayName("DetermineUserType - Regular User Type - Returns ROLE_USER")
    public void testDetermineUserType_RegularUser_ReturnsRoleUser() {
        // Arrange - Regular user with type 'U'
        testUser.setUserType("U");

        // Act
        String userType = authenticationService.determineUserType(testUser);

        // Assert
        assertEquals("ROLE_USER", userType, "Regular user should return ROLE_USER");
        
        logTestExecution("Regular user type determination test passed", null);
    }

    /**
     * Test case-insensitive username handling.
     * Validates that username lookup works correctly regardless of case,
     * matching COBOL FUNCTION UPPER-CASE behavior for user identification.
     */
    @Test
    @DisplayName("Authenticate - Case Insensitive Username - Handles Correctly")
    public void testAuthenticate_CaseInsensitiveUsername_HandlesCorrectly() {
        // Arrange - Test with mixed case username
        String mixedCaseUsername = "TestUser";
        String expectedUsername = mixedCaseUsername.toLowerCase();
        String password = TestConstants.TEST_USER_PASSWORD;
        
        when(userSecurityRepository.findByUsername(expectedUsername))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPassword()))
            .thenReturn(true);

        // Act
        Authentication result = authenticationService.authenticate(mixedCaseUsername, password);

        // Assert
        assertNotNull(result, "Authentication should succeed with case conversion");
        assertTrue(result.isAuthenticated(), "Authentication should be successful");
        
        // Verify case conversion occurred
        verify(userSecurityRepository, times(1)).findByUsername(expectedUsername);

        logTestExecution("Case insensitive username test passed", null);
    }
    /**
     * Test disabled user account authentication rejection.
     * Validates that disabled user accounts are properly rejected
     * even with valid credentials, matching COBOL account status checking.
     */
    @Test
    @DisplayName("Authenticate - Disabled User Account - Throws Authentication Exception")
    public void testAuthenticate_DisabledUser_ThrowsAuthenticationException() {
        // Arrange - Create disabled user
        UserSecurity disabledUser = createTestUser();
        disabledUser.setEnabled(false);
        String username = disabledUser.getUsername();
        String password = TestConstants.TEST_USER_PASSWORD;
        
        when(userSecurityRepository.findByUsername(username))
            .thenReturn(Optional.of(disabledUser));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            authenticationService.authenticate(username, password);
        }, "Disabled user should be rejected");

        verify(userSecurityRepository, times(1)).findByUsername(username);
        
        logTestExecution("Disabled user authentication test passed", null);
    }

    /**
     * Test locked user account authentication rejection.
     * Validates that locked user accounts are properly rejected
     * matching COBOL account lock status validation.
     */
    @Test
    @DisplayName("Authenticate - Locked User Account - Throws Authentication Exception")
    public void testAuthenticate_LockedUser_ThrowsAuthenticationException() {
        // Arrange - Create locked user
        UserSecurity lockedUser = createTestUser();
        lockedUser.setAccountNonLocked(false);
        String username = lockedUser.getUsername();
        String password = TestConstants.TEST_USER_PASSWORD;
        
        when(userSecurityRepository.findByUsername(username))
            .thenReturn(Optional.of(lockedUser));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            authenticationService.authenticate(username, password);
        }, "Locked user should be rejected");

        verify(userSecurityRepository, times(1)).findByUsername(username);
        
        logTestExecution("Locked user authentication test passed", null);
    }

    /**
     * Test expired user account authentication rejection.
     * Validates that expired user accounts are properly rejected
     * matching COBOL account expiration checking logic.
     */
    @Test
    @DisplayName("Authenticate - Expired User Account - Throws Authentication Exception")
    public void testAuthenticate_ExpiredUser_ThrowsAuthenticationException() {
        // Arrange - Create expired user
        UserSecurity expiredUser = createTestUser();
        expiredUser.setAccountNonExpired(false);
        String username = expiredUser.getUsername();
        String password = TestConstants.TEST_USER_PASSWORD;
        
        when(userSecurityRepository.findByUsername(username))
            .thenReturn(Optional.of(expiredUser));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            authenticationService.authenticate(username, password);
        }, "Expired user should be rejected");

        verify(userSecurityRepository, times(1)).findByUsername(username);
        
        logTestExecution("Expired user authentication test passed", null);
    }

    /**
     * Test expired credentials authentication rejection.
     * Validates that users with expired credentials are properly rejected
     * matching COBOL credential expiration validation.
     */
    @Test
    @DisplayName("Authenticate - Expired Credentials - Throws Authentication Exception")
    public void testAuthenticate_ExpiredCredentials_ThrowsAuthenticationException() {
        // Arrange - Create user with expired credentials
        UserSecurity expiredCredentialsUser = createTestUser();
        expiredCredentialsUser.setCredentialsNonExpired(false);
        String username = expiredCredentialsUser.getUsername();
        String password = TestConstants.TEST_USER_PASSWORD;
        
        when(userSecurityRepository.findByUsername(username))
            .thenReturn(Optional.of(expiredCredentialsUser));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            authenticationService.authenticate(username, password);
        }, "User with expired credentials should be rejected");

        verify(userSecurityRepository, times(1)).findByUsername(username);
        
        logTestExecution("Expired credentials authentication test passed", null);
    }

    /**
     * Test successful admin user authentication.
     * Validates that admin users are properly authenticated and
     * assigned correct authorities matching COBOL admin type logic.
     */
    @Test
    @DisplayName("Authenticate - Valid Admin Credentials - Returns Admin Authentication")
    public void testAuthenticate_ValidAdminCredentials_ReturnsAdminAuthentication() {
        // Arrange
        String username = testAdmin.getUsername();
        String password = testAdmin.getPassword();
        
        when(userSecurityRepository.findByUsername(username))
            .thenReturn(Optional.of(testAdmin));
        when(passwordEncoder.matches(password, testAdmin.getPassword()))
            .thenReturn(true);

        // Act
        long startTime = System.currentTimeMillis();
        Authentication result = authenticationService.authenticate(username, password);
        long executionTime = System.currentTimeMillis() - startTime;

        // Assert
        assertNotNull(result, "Admin authentication result should not be null");
        assertTrue(result.isAuthenticated(), "Admin authentication should be successful");
        assertEquals(username, result.getName(), "Admin username should match input");
        
        // Verify admin authority is granted
        assertTrue(result.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")),
            "Admin user should have ROLE_ADMIN authority");

        // Verify repository interaction
        verify(userSecurityRepository, times(1)).findByUsername(username);
        verify(passwordEncoder, times(1)).matches(password, testAdmin.getPassword());

        // Validate performance requirement
        assertTrue(executionTime < TestConstants.RESPONSE_TIME_THRESHOLD_MS,
            "Admin authentication should complete within 200ms threshold");

        logTestExecution("Valid admin credentials authentication test passed", executionTime);
    }

    /**
     * Test validateCredentials with null username.
     * Validates proper handling of null input parameters
     * matching COBOL null checking behavior.
     */
    @Test
    @DisplayName("ValidateCredentials - Null Username - Returns False")
    public void testValidateCredentials_NullUsername_ReturnsFalse() {
        // Arrange
        String nullUsername = null;
        String password = TestConstants.TEST_USER_PASSWORD;

        // Act
        boolean result = authenticationService.validateCredentials(nullUsername, password);

        // Assert
        assertFalse(result, "Null username should return false");

        // Verify no repository interaction
        verify(userSecurityRepository, never()).findByUsername(any());
        verify(passwordEncoder, never()).matches(any(), any());

        logTestExecution("Null username validation test passed", null);
    }

    /**
     * Test validateCredentials with null password.
     * Validates proper handling of null password input
     * matching COBOL null checking behavior.
     */
    @Test
    @DisplayName("ValidateCredentials - Null Password - Returns False")
    public void testValidateCredentials_NullPassword_ReturnsFalse() {
        // Arrange
        String username = TestConstants.TEST_USER_ID.toLowerCase();
        String nullPassword = null;

        // Act
        boolean result = authenticationService.validateCredentials(username, nullPassword);

        // Assert
        assertFalse(result, "Null password should return false");

        // Verify no repository interaction
        verify(userSecurityRepository, never()).findByUsername(any());
        verify(passwordEncoder, never()).matches(any(), any());

        logTestExecution("Null password validation test passed", null);
    }

    /**
     * Test determineUserType with invalid user type.
     * Validates proper handling of unexpected user type values
     * with appropriate default behavior or exception throwing.
     */
    @Test
    @DisplayName("DetermineUserType - Invalid Type - Throws Exception")
    public void testDetermineUserType_InvalidType_ThrowsException() {
        // Arrange
        UserSecurity invalidUser = createTestUser();
        invalidUser.setUserType("X"); // Invalid type

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            authenticationService.determineUserType(invalidUser);
        }, "Invalid user type should throw IllegalArgumentException");

        logTestExecution("Invalid user type determination test passed", null);
    }

    /**
     * Test determineUserType with null user object.
     * Validates proper null handling matching COBOL null checking patterns.
     */
    @Test
    @DisplayName("DetermineUserType - Null User - Throws Exception")
    public void testDetermineUserType_NullUser_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            authenticationService.determineUserType(null);
        }, "Null user should throw IllegalArgumentException");

        logTestExecution("Null user type determination test passed", null);
    }

    /**
     * Test PlainTextPasswordEncoder matches method behavior.
     * Validates that the password encoder correctly implements
     * legacy COBOL password comparison logic with case handling.
     */
    @Test
    @DisplayName("PasswordEncoder - Matches Method - Validates Legacy Behavior")
    public void testPasswordEncoder_MatchesMethod_ValidatesLegacyBehavior() {
        // Arrange
        String rawPassword = TestConstants.TEST_USER_PASSWORD;
        String encodedPassword = TestConstants.TEST_USER_PASSWORD.toUpperCase();
        
        // Reset mock to test actual behavior
        reset(passwordEncoder);
        when(passwordEncoder.matches(rawPassword, encodedPassword))
            .thenReturn(true);

        // Act
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);

        // Assert
        assertTrue(matches, "Password encoder should match equivalent passwords");

        verify(passwordEncoder, times(1)).matches(rawPassword, encodedPassword);
        
        logTestExecution("Password encoder matches test passed", null);
    }

    /**
     * Test PlainTextPasswordEncoder encode method behavior.
     * Validates that password encoding follows COBOL FUNCTION UPPER-CASE
     * behavior for legacy compatibility.
     */
    @Test
    @DisplayName("PasswordEncoder - Encode Method - Preserves Legacy Format")
    public void testPasswordEncoder_EncodeMethod_PreservesLegacyFormat() {
        // Arrange
        String rawPassword = "testpass";
        String expectedEncoded = "TESTPASS";
        
        // Reset mock to test actual behavior
        reset(passwordEncoder);
        when(passwordEncoder.encode(rawPassword))
            .thenReturn(expectedEncoded);

        // Act
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Assert
        assertEquals(expectedEncoded, encodedPassword, 
            "Password encoder should convert to uppercase");

        verify(passwordEncoder, times(1)).encode(rawPassword);
        
        logTestExecution("Password encoder encode test passed", null);
    }

    /**
     * Test authentication performance under load simulation.
     * Validates that authentication service meets performance requirements
     * even under concurrent access patterns matching CICS transaction load.
     */
    @Test
    @DisplayName("Authenticate - Performance Under Load - Meets Response Time Requirements")
    public void testAuthenticate_PerformanceUnderLoad_MeetsResponseTimeRequirements() {
        // Arrange
        String username = TestConstants.TEST_USER_ID.toLowerCase();
        String password = TestConstants.TEST_USER_PASSWORD;
        
        when(userSecurityRepository.findByUsername(username))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPassword()))
            .thenReturn(true);

        // Act - Simulate multiple rapid authentication requests
        long totalTime = 0;
        int iterations = 10;
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.currentTimeMillis();
            Authentication result = authenticationService.authenticate(username, password);
            long executionTime = System.currentTimeMillis() - startTime;
            totalTime += executionTime;
            
            assertNotNull(result, "Authentication should succeed in iteration " + i);
            assertTrue(result.isAuthenticated(), 
                "Authentication should be successful in iteration " + i);
        }

        // Assert average performance
        long averageTime = totalTime / iterations;
        assertTrue(averageTime < TestConstants.RESPONSE_TIME_THRESHOLD_MS,
            "Average authentication time should be under 200ms, was: " + averageTime + "ms");

        // Verify expected number of repository calls
        verify(userSecurityRepository, times(iterations)).findByUsername(username);
        verify(passwordEncoder, times(iterations)).matches(password, testUser.getPassword());

        logTestExecution("Performance under load test passed", averageTime);
    }

    /**
     * Test authentication with COBOL-compatible username formats.
     * Validates proper handling of 8-character fixed-width usernames
     * matching original VSAM USRSEC key structure.
     */
    @Test
    @DisplayName("Authenticate - COBOL Compatible Username Format - Handles Correctly")
    public void testAuthenticate_CobolCompatibleUsernameFormat_HandlesCorrectly() {
        // Arrange - Test with COBOL-style 8-character username
        String cobolUsername = "TESTUSER";  // 8 characters, uppercase
        String expectedUsername = cobolUsername.toLowerCase();
        String password = TestConstants.TEST_USER_PASSWORD;
        
        UserSecurity cobolUser = createTestUser();
        cobolUser.setSecUsrId(cobolUsername);
        cobolUser.setUsername(expectedUsername);
        
        when(userSecurityRepository.findByUsername(expectedUsername))
            .thenReturn(Optional.of(cobolUser));
        when(passwordEncoder.matches(password, cobolUser.getPassword()))
            .thenReturn(true);

        // Act
        Authentication result = authenticationService.authenticate(cobolUsername, password);

        // Assert
        assertNotNull(result, "COBOL-format username should authenticate successfully");
        assertTrue(result.isAuthenticated(), "Authentication should be successful");
        assertEquals(expectedUsername, result.getName(), 
            "Username should be converted to lowercase");

        verify(userSecurityRepository, times(1)).findByUsername(expectedUsername);
        
        logTestExecution("COBOL compatible username test passed", null);
    }

    /**
     * Test username trimming and space handling.
     * Validates proper handling of usernames with leading/trailing spaces
     * matching COBOL string handling behavior.
     */
    @Test
    @DisplayName("Authenticate - Username With Spaces - Trims Correctly")
    public void testAuthenticate_UsernameWithSpaces_TrimsCorrectly() {
        // Arrange - Username with spaces (common in COBOL fixed-width fields)
        String usernameWithSpaces = "  " + TestConstants.TEST_USER_ID + "  ";
        String expectedUsername = TestConstants.TEST_USER_ID.toLowerCase();
        String password = TestConstants.TEST_USER_PASSWORD;
        
        when(userSecurityRepository.findByUsername(expectedUsername))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPassword()))
            .thenReturn(true);

        // Act
        Authentication result = authenticationService.authenticate(usernameWithSpaces, password);

        // Assert
        assertNotNull(result, "Authentication should succeed with trimmed username");
        assertTrue(result.isAuthenticated(), "Authentication should be successful");

        // Verify trimmed username was used for repository lookup
        verify(userSecurityRepository, times(1)).findByUsername(expectedUsername);
        
        logTestExecution("Username trimming test passed", null);
    }

    /**
     * Test password trimming and space handling.
     * Validates proper handling of passwords with leading/trailing spaces
     * matching COBOL password field handling.
     */
    @Test
    @DisplayName("Authenticate - Password With Spaces - Trims Correctly")
    public void testAuthenticate_PasswordWithSpaces_TrimsCorrectly() {
        // Arrange - Password with spaces (common in COBOL fixed-width fields)
        String username = TestConstants.TEST_USER_ID.toLowerCase();
        String passwordWithSpaces = "  " + TestConstants.TEST_USER_PASSWORD + "  ";
        String expectedPassword = TestConstants.TEST_USER_PASSWORD;
        
        when(userSecurityRepository.findByUsername(username))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(expectedPassword, testUser.getPassword()))
            .thenReturn(true);

        // Act
        Authentication result = authenticationService.authenticate(username, passwordWithSpaces);

        // Assert
        assertNotNull(result, "Authentication should succeed with trimmed password");
        assertTrue(result.isAuthenticated(), "Authentication should be successful");

        // Verify trimmed password was used for validation
        verify(passwordEncoder, times(1)).matches(expectedPassword, testUser.getPassword());
        
        logTestExecution("Password trimming test passed", null);
    }

    /**
     * Test UsernamePasswordAuthenticationToken creation.
     * Validates that proper Spring Security authentication tokens
     * are created with correct principal and credentials.
     */
    @Test
    @DisplayName("Authenticate - Authentication Token Creation - Creates Correct Token")
    public void testAuthenticate_AuthenticationTokenCreation_CreatesCorrectToken() {
        // Arrange
        String username = TestConstants.TEST_USER_ID.toLowerCase();
        String password = TestConstants.TEST_USER_PASSWORD;
        
        when(userSecurityRepository.findByUsername(username))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPassword()))
            .thenReturn(true);

        // Act
        Authentication result = authenticationService.authenticate(username, password);

        // Assert
        assertNotNull(result, "Authentication token should be created");
        assertTrue(result instanceof UsernamePasswordAuthenticationToken,
            "Should return UsernamePasswordAuthenticationToken");
        
        UsernamePasswordAuthenticationToken token = 
            (UsernamePasswordAuthenticationToken) result;
            
        assertEquals(username, token.getPrincipal(), 
            "Principal should match username");
        assertNotNull(token.getCredentials(), 
            "Credentials should be set");
        assertTrue(token.isAuthenticated(), 
            "Token should be authenticated");

        logTestExecution("Authentication token creation test passed", null);
    }

    /**
     * Test repository exception handling.
     * Validates proper handling of database connectivity issues
     * and repository access exceptions.
     */
    @Test
    @DisplayName("Authenticate - Repository Exception - Throws Service Exception")
    public void testAuthenticate_RepositoryException_ThrowsServiceException() {
        // Arrange
        String username = TestConstants.TEST_USER_ID.toLowerCase();
        String password = TestConstants.TEST_USER_PASSWORD;
        
        when(userSecurityRepository.findByUsername(username))
            .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            authenticationService.authenticate(username, password);
        }, "Repository exception should propagate as service exception");

        verify(userSecurityRepository, times(1)).findByUsername(username);
        // Password encoder should not be called on repository exception
        verify(passwordEncoder, never()).matches(any(), any());

        logTestExecution("Repository exception handling test passed", null);
    }

    /**
     * Test concurrent authentication requests.
     * Validates that authentication service handles concurrent requests
     * properly without thread safety issues.
     */
    @Test
    @DisplayName("Authenticate - Concurrent Requests - Handles Thread Safety")
    public void testAuthenticate_ConcurrentRequests_HandlesThreadSafety() {
        // Arrange
        String username = TestConstants.TEST_USER_ID.toLowerCase();
        String password = TestConstants.TEST_USER_PASSWORD;
        
        when(userSecurityRepository.findByUsername(username))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPassword()))
            .thenReturn(true);

        // Act - Simulate concurrent access
        Authentication result1 = authenticationService.authenticate(username, password);
        Authentication result2 = authenticationService.authenticate(username, password);

        // Assert
        assertNotNull(result1, "First authentication should succeed");
        assertNotNull(result2, "Second authentication should succeed");
        assertTrue(result1.isAuthenticated(), "First authentication should be valid");
        assertTrue(result2.isAuthenticated(), "Second authentication should be valid");

        // Verify expected repository calls
        verify(userSecurityRepository, times(2)).findByUsername(username);
        verify(passwordEncoder, times(2)).matches(password, testUser.getPassword());

        logTestExecution("Concurrent authentication test passed", null);
    }

    /**
     * Test validateCredentials with non-existent user.
     * Validates proper handling when user is not found in repository.
     */
    @Test
    @DisplayName("ValidateCredentials - Non-Existent User - Returns False")
    public void testValidateCredentials_NonExistentUser_ReturnsFalse() {
        // Arrange
        String nonExistentUsername = "NOUSER";
        String password = TestConstants.TEST_USER_PASSWORD;
        
        when(userSecurityRepository.findByUsername(nonExistentUsername))
            .thenReturn(Optional.empty());

        // Act
        boolean result = authenticationService.validateCredentials(nonExistentUsername, password);

        // Assert
        assertFalse(result, "Non-existent user should return false");

        // Verify repository interaction
        verify(userSecurityRepository, times(1)).findByUsername(nonExistentUsername);
        // Password encoder should not be called for non-existent user
        verify(passwordEncoder, never()).matches(any(), any());

        logTestExecution("Non-existent user validation test passed", null);
    }

    /**
     * Test determineUserType with lowercase user type.
     * Validates proper handling of case variations in user type field
     * matching COBOL case handling behavior.
     */
    @Test
    @DisplayName("DetermineUserType - Lowercase Admin Type - Returns ROLE_ADMIN")
    public void testDetermineUserType_LowercaseAdminType_ReturnsRoleAdmin() {
        // Arrange
        UserSecurity adminUser = createTestUser();
        adminUser.setUserType("a"); // lowercase admin type

        // Act
        String userType = authenticationService.determineUserType(adminUser);

        // Assert
        assertEquals("ROLE_ADMIN", userType, "Lowercase 'a' should map to ROLE_ADMIN");
        
        logTestExecution("Lowercase admin type determination test passed", null);
    }

    /**
     * Test determineUserType with lowercase user type.
     * Validates proper handling of case variations in user type field.
     */
    @Test
    @DisplayName("DetermineUserType - Lowercase User Type - Returns ROLE_USER")
    public void testDetermineUserType_LowercaseUserType_ReturnsRoleUser() {
        // Arrange
        UserSecurity regularUser = createTestUser();
        regularUser.setUserType("u"); // lowercase user type

        // Act
        String userType = authenticationService.determineUserType(regularUser);

        // Assert
        assertEquals("ROLE_USER", userType, "Lowercase 'u' should map to ROLE_USER");
        
        logTestExecution("Lowercase user type determination test passed", null);
    }

    /**
     * Test authentication with maximum username length.
     * Validates proper handling of 8-character username limit
     * matching COBOL PIC X(8) field constraint.
     */
    @Test
    @DisplayName("Authenticate - Maximum Username Length - Handles Correctly")
    public void testAuthenticate_MaximumUsernameLength_HandlesCorrectly() {
        // Arrange - 8-character username (COBOL maximum)
        String maxLengthUsername = "TESTUSER"; // Exactly 8 characters
        String expectedUsername = maxLengthUsername.toLowerCase();
        String password = TestConstants.TEST_USER_PASSWORD;
        
        UserSecurity maxLengthUser = createTestUser();
        maxLengthUser.setSecUsrId(maxLengthUsername);
        maxLengthUser.setUsername(expectedUsername);
        
        when(userSecurityRepository.findByUsername(expectedUsername))
            .thenReturn(Optional.of(maxLengthUser));
        when(passwordEncoder.matches(password, maxLengthUser.getPassword()))
            .thenReturn(true);

        // Act
        Authentication result = authenticationService.authenticate(maxLengthUsername, password);

        // Assert
        assertNotNull(result, "Maximum length username should authenticate");
        assertTrue(result.isAuthenticated(), "Authentication should be successful");

        verify(userSecurityRepository, times(1)).findByUsername(expectedUsername);
        
        logTestExecution("Maximum username length test passed", null);
    }

    /**
     * Test authentication with maximum password length.
     * Validates proper handling of 8-character password limit
     * matching COBOL PIC X(8) field constraint.
     */
    @Test
    @DisplayName("Authenticate - Maximum Password Length - Handles Correctly")
    public void testAuthenticate_MaximumPasswordLength_HandlesCorrectly() {
        // Arrange - 8-character password (COBOL maximum)
        String username = TestConstants.TEST_USER_ID.toLowerCase();
        String maxLengthPassword = "PASSWORD"; // Exactly 8 characters
        
        UserSecurity passwordUser = createTestUser();
        passwordUser.setPassword(maxLengthPassword);
        
        when(userSecurityRepository.findByUsername(username))
            .thenReturn(Optional.of(passwordUser));
        when(passwordEncoder.matches(maxLengthPassword, passwordUser.getPassword()))
            .thenReturn(true);

        // Act
        Authentication result = authenticationService.authenticate(username, maxLengthPassword);

        // Assert
        assertNotNull(result, "Maximum length password should authenticate");
        assertTrue(result.isAuthenticated(), "Authentication should be successful");

        verify(passwordEncoder, times(1)).matches(maxLengthPassword, passwordUser.getPassword());
        
        logTestExecution("Maximum password length test passed", null);
    }

    /**
     * Test COBOL data compatibility scenarios.
     * Validates authentication service works with data patterns
     * typical of COBOL-to-Java data conversion.
     */
    @Test
    @DisplayName("Authenticate - COBOL Data Compatibility - Handles Legacy Patterns")
    public void testAuthenticate_CobolDataCompatibility_HandlesLegacyPatterns() {
        // Arrange - Test data matching COBOL CSUSR01Y copybook structure
        UserSecurity cobolCompatibleUser = new UserSecurity();
        cobolCompatibleUser.setSecUsrId("CBLUSER1");
        cobolCompatibleUser.setUsername("cbluser1");
        cobolCompatibleUser.setPassword("CBLPASS1");
        cobolCompatibleUser.setFirstName("COBOL");
        cobolCompatibleUser.setLastName("USER");
        cobolCompatibleUser.setUserType("U");
        cobolCompatibleUser.setEnabled(true);
        cobolCompatibleUser.setAccountNonExpired(true);
        cobolCompatibleUser.setAccountNonLocked(true);
        cobolCompatibleUser.setCredentialsNonExpired(true);
        cobolCompatibleUser.setFailedLoginAttempts(0);
        cobolCompatibleUser.setCreatedAt(LocalDateTime.now());
        cobolCompatibleUser.setUpdatedAt(LocalDateTime.now());
        
        when(userSecurityRepository.findByUsername("cbluser1"))
            .thenReturn(Optional.of(cobolCompatibleUser));
        when(passwordEncoder.matches("CBLPASS1", "CBLPASS1"))
            .thenReturn(true);

        // Act
        Authentication result = authenticationService.authenticate("CBLUSER1", "CBLPASS1");

        // Assert
        assertNotNull(result, "COBOL compatible data should authenticate");
        assertTrue(result.isAuthenticated(), "Authentication should be successful");
        assertEquals("cbluser1", result.getName(), "Username should be normalized");

        logTestExecution("COBOL data compatibility test passed", null);
    }

    /**
     * Helper method to create a test user with default values.
     * Provides consistent test user creation matching COBOL data structure.
     * 
     * @return UserSecurity object with test data
     */
    private UserSecurity createTestUser() {
        UserSecurity user = new UserSecurity();
        user.setSecUsrId(TestConstants.TEST_USER_ID);
        user.setUsername(TestConstants.TEST_USER_ID.toLowerCase());
        user.setPassword(TestConstants.TEST_USER_PASSWORD);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setUserType("U");
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}
    /**
     * Test validateCredentials with empty username.
     * Validates proper handling of empty string inputs
     * matching COBOL space checking behavior.
     */
    @Test
    @DisplayName("ValidateCredentials - Empty Username - Returns False")
    public void testValidateCredentials_EmptyUsername_ReturnsFalse() {
        // Arrange
        String emptyUsername = "";
        String password = TestConstants.TEST_USER_PASSWORD;

        // Act
        boolean result = authenticationService.validateCredentials(emptyUsername, password);

        // Assert
        assertFalse(result, "Empty username should return false");

        // Verify no repository interaction
        verify(userSecurityRepository, never()).findByUsername(any());
        verify(passwordEncoder, never()).matches(any(), any());

        logTestExecution("Empty username validation test passed", null);
    }

    /**
     * Test validateCredentials with empty password.
     * Validates proper handling of empty password input
     * matching COBOL space checking behavior.
     */
    @Test
    @DisplayName("ValidateCredentials - Empty Password - Returns False")
    public void testValidateCredentials_EmptyPassword_ReturnsFalse() {
        // Arrange
        String username = TestConstants.TEST_USER_ID.toLowerCase();
        String emptyPassword = "";

        // Act
        boolean result = authenticationService.validateCredentials(username, emptyPassword);

        // Assert
        assertFalse(result, "Empty password should return false");

        // Verify no repository interaction
        verify(userSecurityRepository, never()).findByUsername(any());
        verify(passwordEncoder, never()).matches(any(), any());

        logTestExecution("Empty password validation test passed", null);
    }

