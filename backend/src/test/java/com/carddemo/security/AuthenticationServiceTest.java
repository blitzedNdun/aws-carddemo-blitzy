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

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        setupTestUsers();
    }

    private void setupTestUsers() {
        // Setup test user
        testUser = new UserSecurity();
        testUser.setId(1L);
        testUser.setSecUsrId(TestConstants.TEST_USER_ID);
        testUser.setUsername(TestConstants.TEST_USER_ID);
        testUser.setPassword(TestConstants.TEST_USER_PASSWORD);
        testUser.setUserType("U");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEnabled(true);
        testUser.setAccountNonLocked(true);
        testUser.setAccountNonExpired(true);
        testUser.setCredentialsNonExpired(true);

        // Setup test admin
        testAdmin = new UserSecurity();
        testAdmin.setId(2L);
        testAdmin.setSecUsrId("TESTADMIN");
        testAdmin.setUsername("TESTADMIN");
        testAdmin.setPassword("adminpass123");
        testAdmin.setUserType("A");
        testAdmin.setFirstName("Test");
        testAdmin.setLastName("Admin");
        testAdmin.setEnabled(true);
        testAdmin.setAccountNonLocked(true);
        testAdmin.setAccountNonExpired(true);
        testAdmin.setCredentialsNonExpired(true);
    }

    @Test
    @DisplayName("Authenticate - Valid Credentials - Returns Authenticated User")
    public void testAuthenticate_ValidCredentials_ReturnsAuthenticatedUser() {
        // Arrange
        String username = TestConstants.TEST_USER_ID;
        String password = TestConstants.TEST_USER_PASSWORD;
        
        when(userSecurityRepository.findByUsername(username))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPassword()))
            .thenReturn(true);

        // Act
        Authentication result = authenticationService.authenticate(username, password);

        // Assert
        assertNotNull(result, "Authentication result should not be null");
        assertTrue(result.isAuthenticated(), "User should be authenticated");
        assertEquals(testUser.getUsername(), result.getName(), "Username should match");
        assertNotNull(result.getAuthorities(), "Authorities should not be null");
        assertTrue(result.getAuthorities().size() > 0, "Should have at least one authority");
        
        // Verify repository and encoder interactions
        verify(userSecurityRepository).findByUsername(username);
        verify(passwordEncoder).matches(password, testUser.getPassword());
        
        logTestExecution("Valid credentials authentication test passed", null);
    }

    @Test
    @DisplayName("Authenticate - Invalid Password - Returns Authentication Failure")
    public void testAuthenticate_InvalidPassword_ReturnsAuthenticationFailure() {
        // Arrange
        String username = TestConstants.TEST_USER_ID;
        String invalidPassword = "wrongpassword";
        
        when(userSecurityRepository.findByUsername(username))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(invalidPassword, testUser.getPassword()))
            .thenReturn(false);

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> {
            authenticationService.authenticate(username, invalidPassword);
        }, "Should throw UsernameNotFoundException for invalid password");
        
        // Verify interactions
        verify(userSecurityRepository).findByUsername(username);
        verify(passwordEncoder).matches(invalidPassword, testUser.getPassword());
        
        logTestExecution("Invalid password authentication test passed", null);
    }

    @Test
    @DisplayName("Authenticate - Non-Existent User - Throws UsernameNotFoundException")
    public void testAuthenticate_NonExistentUser_ThrowsUsernameNotFoundException() {
        // Arrange
        String nonExistentUsername = "NONEXIST";
        String password = TestConstants.TEST_USER_PASSWORD;
        
        when(userSecurityRepository.findByUsername(nonExistentUsername))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> {
            authenticationService.authenticate(nonExistentUsername, password);
        }, "Should throw UsernameNotFoundException for non-existent user");
        
        // Verify only repository interaction (no password check for non-existent user)
        verify(userSecurityRepository).findByUsername(nonExistentUsername);
        verify(passwordEncoder, never()).matches(any(), any());
        
        logTestExecution("Non-existent user authentication test passed", null);
    }

    @Test
    @DisplayName("ValidateCredentials - Valid User - Returns True")
    public void testValidateCredentials_ValidUser_ReturnsTrue() {
        // Arrange
        String username = TestConstants.TEST_USER_ID;
        String password = TestConstants.TEST_USER_PASSWORD;
        
        when(userSecurityRepository.findByUsername(username))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPassword()))
            .thenReturn(true);

        // Act
        boolean result = authenticationService.validateCredentials(username, password);

        // Assert
        assertTrue(result, "Validation should return true for valid credentials");
        
        // Verify interactions
        verify(userSecurityRepository).findByUsername(username);
        verify(passwordEncoder).matches(password, testUser.getPassword());
        
        logTestExecution("Valid credentials validation test passed", null);
    }

    @Test
    @DisplayName("ValidateCredentials - Invalid Credentials - Returns False")
    public void testValidateCredentials_InvalidCredentials_ReturnsFalse() {
        // Arrange
        String username = TestConstants.TEST_USER_ID;
        String invalidPassword = "wrongpassword";
        
        when(userSecurityRepository.findByUsername(username))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(invalidPassword, testUser.getPassword()))
            .thenReturn(false);

        // Act
        boolean result = authenticationService.validateCredentials(username, invalidPassword);

        // Assert
        assertFalse(result, "Validation should return false for invalid credentials");
        
        // Verify interactions
        verify(userSecurityRepository).findByUsername(username);
        verify(passwordEncoder).matches(invalidPassword, testUser.getPassword());
        
        logTestExecution("Invalid credentials validation test passed", null);
    }

    @Test
    @DisplayName("DetermineUserType - Admin User - Returns ADMIN")
    public void testDetermineUserType_AdminUser_ReturnsAdmin() {
        // Act
        String userType = authenticationService.determineUserType(testAdmin);

        // Assert
        assertEquals("ADMIN", userType, "Admin user should return ADMIN type");
        
        logTestExecution("Admin user type determination test passed", null);
    }

    @Test
    @DisplayName("DetermineUserType - Regular User - Returns USER")
    public void testDetermineUserType_RegularUser_ReturnsUser() {
        // Act
        String userType = authenticationService.determineUserType(testUser);

        // Assert
        assertEquals("USER", userType, "Regular user should return USER type");
        
        logTestExecution("Regular user type determination test passed", null);
    }

    @Test
    @DisplayName("Authenticate - Case Insensitive Username - Returns Authenticated User")
    public void testAuthenticate_CaseInsensitiveUsername_ReturnsAuthenticatedUser() {
        // Arrange
        String mixedCaseUsername = TestConstants.TEST_USER_ID.toLowerCase();
        String password = TestConstants.TEST_USER_PASSWORD;
        String expectedUsername = TestConstants.TEST_USER_ID.toUpperCase();
        
        when(userSecurityRepository.findByUsername(expectedUsername))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPassword()))
            .thenReturn(true);

        // Act
        Authentication result = authenticationService.authenticate(mixedCaseUsername, password);

        // Assert
        assertNotNull(result, "Authentication result should not be null");
        assertTrue(result.isAuthenticated(), "User should be authenticated");
        
        // Verify repository called with normalized username
        verify(userSecurityRepository).findByUsername(expectedUsername);
        
        logTestExecution("Case insensitive username test passed", null);
    }

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

    @Test
    @DisplayName("ValidateCredentials - Null Password - Returns False")
    public void testValidateCredentials_NullPassword_ReturnsFalse() {
        // Arrange
        String username = TestConstants.TEST_USER_ID;
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
}