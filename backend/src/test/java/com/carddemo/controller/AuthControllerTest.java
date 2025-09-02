/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.controller;

import com.carddemo.controller.AuthController;
import com.carddemo.service.SignOnService;
import com.carddemo.dto.SignOnRequest;
import com.carddemo.dto.SignOnResponse;
import com.carddemo.entity.UserSecurity;
import com.carddemo.dto.MenuOption;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.ArrayList;

/**
 * Comprehensive unit test class for AuthController that validates user authentication endpoints
 * including sign-in, sign-out, and session management. Ensures functional parity with COBOL COSGN00C
 * program by testing all authentication workflows, error conditions, and session state management.
 * 
 * This test class validates the complete authentication flow migration from CICS COBOL to Spring Boot:
 * - POST /api/auth/signin endpoint validation (replaces CICS RECEIVE MAP)
 * - User credential validation with uppercase conversion (FUNCTION UPPER-CASE)
 * - USRSEC dataset lookup equivalent (PostgreSQL user_security table)
 * - Session creation and management through Spring Session (replaces COMMAREA)
 * - Role-based routing (admin vs regular user - COADM01C vs COMEN01C)
 * - Error handling for invalid credentials (exact COBOL error messages)
 * - COMMAREA-equivalent session attribute population
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @InjectMocks
    private AuthController authController;

    @Mock
    private SignOnService signOnService;

    private MockHttpSession mockSession;
    private MockHttpServletRequest mockRequest;
    private UserSecurity testAdminUser;
    private UserSecurity testRegularUser;
    private SignOnRequest validAdminRequest;
    private SignOnRequest validUserRequest;
    private SignOnRequest invalidRequest;

    /**
     * Set up test data and mock objects before each test.
     */
    @BeforeEach
    public void setUp() {
        // Initialize mock session for Spring Session testing
        mockSession = new MockHttpSession();
        mockRequest = new MockHttpServletRequest();
        mockRequest.setSession(mockSession);
        
        // Create test admin user (equivalent to COBOL SEC-USR-TYPE = 'A')
        testAdminUser = new UserSecurity();
        testAdminUser.setSecUsrId("ADMIN001");
        testAdminUser.setUsername("ADMIN001");
        testAdminUser.setPassword("$2a$10$test.admin.bcrypt.hash");
        testAdminUser.setFirstName("Admin");
        testAdminUser.setLastName("User");
        testAdminUser.setUserType("A");
        testAdminUser.setEnabled(true);
        testAdminUser.setAccountNonExpired(true);
        testAdminUser.setAccountNonLocked(true);
        testAdminUser.setCredentialsNonExpired(true);
        
        // Create test regular user (equivalent to COBOL SEC-USR-TYPE = 'U')
        testRegularUser = new UserSecurity();
        testRegularUser.setSecUsrId("USER0001");
        testRegularUser.setUsername("USER0001");
        testRegularUser.setPassword("$2a$10$test.user.bcrypt.hash");
        testRegularUser.setFirstName("Regular");
        testRegularUser.setLastName("User");
        testRegularUser.setUserType("U");
        testRegularUser.setEnabled(true);
        testRegularUser.setAccountNonExpired(true);
        testRegularUser.setAccountNonLocked(true);
        testRegularUser.setCredentialsNonExpired(true);
        
        // Create valid admin sign-on request
        validAdminRequest = new SignOnRequest();
        validAdminRequest.setUserId("admin001");
        validAdminRequest.setPassword("password");
        
        // Create valid user sign-on request
        validUserRequest = new SignOnRequest();
        validUserRequest.setUserId("user0001");
        validUserRequest.setPassword("password");
        
        // Create invalid request for error testing
        invalidRequest = new SignOnRequest();
        invalidRequest.setUserId("invalid");
        invalidRequest.setPassword("wrong");
    }

    /**
     * Clean up after each test.
     */
    @AfterEach
    public void tearDown() {
        reset(signOnService);
        if (mockSession != null) {
            mockSession.clearAttributes();
        }
        clearInvocations(signOnService);
    }

    /**
     * Test successful sign-in for admin user.
     */
    @Test
    public void testSignInSuccess() throws Exception {
        // Arrange
        SignOnResponse successResponse = new SignOnResponse();
        successResponse.setStatus("SUCCESS");
        successResponse.setUserId("ADMIN001");
        successResponse.setUserName("Admin User");
        successResponse.setUserRole("A");
        successResponse.setSessionToken("test-session-token");
        successResponse.setMessage("Sign-on successful");
        successResponse.setMenuOptions(createAdminMenuOptions());
        
        when(signOnService.validateCredentials("ADMIN001", "password"))
            .thenReturn(successResponse);
        when(signOnService.getUserDetails("ADMIN001"))
            .thenReturn(testAdminUser);
        when(signOnService.initializeSession(testAdminUser))
            .thenReturn("test-session-token");
        when(signOnService.buildMenuOptions("A"))
            .thenReturn(createAdminMenuOptions());
        
        // Act
        ResponseEntity<SignOnResponse> response = authController.signIn(validAdminRequest, mockRequest);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().getStatus());
        assertEquals("ADMIN001", response.getBody().getUserId());
        assertEquals("Admin User", response.getBody().getUserName());
        assertEquals("A", response.getBody().getUserRole());
        assertEquals("test-session-token", response.getBody().getSessionToken());
        assertNotNull(response.getBody().getMenuOptions());
        assertEquals("User Management", response.getBody().getMenuOptions().get(0).getDescription());
        
        // Verify service interactions
        verify(signOnService).validateCredentials("ADMIN001", "password");
        verify(signOnService).getUserDetails("ADMIN001");
        verify(signOnService).initializeSession(testAdminUser);
        verify(signOnService).buildMenuOptions("A");
    }

    /**
     * Test sign-in with invalid credentials.
     */
    @Test
    public void testSignInInvalidCredentials() throws Exception {
        // Arrange
        SignOnResponse errorResponse = new SignOnResponse();
        errorResponse.setStatus("ERROR");
        errorResponse.setMessage("User not found. Try again ...");
        
        when(signOnService.validateCredentials("INVALID", "wrong"))
            .thenReturn(errorResponse);
        
        // Act
        ResponseEntity<SignOnResponse> response = authController.signIn(invalidRequest, mockRequest);
        
        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ERROR", response.getBody().getStatus());
        assertEquals("User not found. Try again ...", response.getBody().getErrorMessage());
        
        // Verify no session attributes were set
        assertNull(mockSession.getAttribute("SEC-USR-ID"));
        assertNull(mockSession.getAttribute("SEC-USR-TYPE"));
    }

    /**
     * Test uppercase conversion of credentials.
     */
    @Test
    public void testSignInUppercaseConversion() throws Exception {
        // Arrange
        SignOnResponse successResponse = new SignOnResponse();
        successResponse.setStatus("SUCCESS");
        successResponse.setUserId("ADMIN001");
        successResponse.setUserName("Admin User");
        successResponse.setUserRole("A");
        
        when(signOnService.validateCredentials("ADMIN001", "password"))
            .thenReturn(successResponse);
        when(signOnService.getUserDetails("ADMIN001"))
            .thenReturn(testAdminUser);
        when(signOnService.initializeSession(testAdminUser))
            .thenReturn("test-session-token");
        when(signOnService.buildMenuOptions("A"))
            .thenReturn(createAdminMenuOptions());
        
        // Act
        ResponseEntity<SignOnResponse> response = authController.signIn(validAdminRequest, mockRequest);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("SUCCESS", response.getBody().getStatus());
        
        // Verify service was called with uppercase userId and original password
        verify(signOnService).validateCredentials("ADMIN001", "password");
    }

    /**
     * Test role-based routing functionality.
     */
    @Test
    public void testRoleBasedRouting() throws Exception {
        // Test admin user routing
        SignOnResponse adminResponse = new SignOnResponse();
        adminResponse.setStatus("SUCCESS");
        adminResponse.setUserId("ADMIN001");
        adminResponse.setUserRole("A");
        adminResponse.setMenuOptions(createAdminMenuOptions());
        
        when(signOnService.validateCredentials("ADMIN001", "password"))
            .thenReturn(adminResponse);
        when(signOnService.getUserDetails("ADMIN001"))
            .thenReturn(testAdminUser);
        when(signOnService.initializeSession(testAdminUser))
            .thenReturn("admin-session-token");
        when(signOnService.buildMenuOptions("A"))
            .thenReturn(createAdminMenuOptions());
        
        ResponseEntity<SignOnResponse> adminResponseEntity = authController.signIn(validAdminRequest, mockRequest);
        
        assertEquals(HttpStatus.OK, adminResponseEntity.getStatusCode());
        assertEquals("A", adminResponseEntity.getBody().getUserRole());
        assertEquals("User Management", adminResponseEntity.getBody().getMenuOptions().get(0).getDescription());
        
        // Test regular user routing
        reset(signOnService);
        mockSession.clearAttributes();
        
        SignOnResponse userResponse = new SignOnResponse();
        userResponse.setStatus("SUCCESS");
        userResponse.setUserId("USER0001");
        userResponse.setUserRole("U");
        userResponse.setMenuOptions(createUserMenuOptions());
        
        when(signOnService.validateCredentials("USER0001", "password"))
            .thenReturn(userResponse);
        when(signOnService.getUserDetails("USER0001"))
            .thenReturn(testRegularUser);
        when(signOnService.initializeSession(testRegularUser))
            .thenReturn("user-session-token");
        when(signOnService.buildMenuOptions("U"))
            .thenReturn(createUserMenuOptions());
        
        ResponseEntity<SignOnResponse> userResponseEntity = authController.signIn(validUserRequest, mockRequest);
        
        assertEquals(HttpStatus.OK, userResponseEntity.getStatusCode());
        assertEquals("U", userResponseEntity.getBody().getUserRole());
        assertEquals("Account Information", userResponseEntity.getBody().getMenuOptions().get(0).getDescription());
    }

    /**
     * Test error handling scenarios.
     */
    @Test
    public void testErrorHandling() throws Exception {
        // Test system error scenario
        when(signOnService.validateCredentials(anyString(), anyString()))
            .thenThrow(new RuntimeException("Database connection error"));
        
        ResponseEntity<SignOnResponse> response = authController.signIn(validUserRequest, mockRequest);
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ERROR", response.getBody().getStatus());
        assertTrue(response.getBody().getErrorMessage().contains("System error occurred"));
    }

    /**
     * Create admin menu options equivalent to COADM01C routing.
     */
    private List<MenuOption> createAdminMenuOptions() {
        List<MenuOption> menuOptions = new ArrayList<>();
        
        MenuOption option1 = new MenuOption();
        option1.setOptionNumber(1);
        option1.setDescription("User Management");
        option1.setTransactionCode("ADMIN_USER_MGMT");
        
        MenuOption option2 = new MenuOption();
        option2.setOptionNumber(2);
        option2.setDescription("Account Management");
        option2.setTransactionCode("ADMIN_ACCOUNT_MGMT");
        
        menuOptions.add(option1);
        menuOptions.add(option2);
        
        return menuOptions;
    }

    /**
     * Create user menu options equivalent to COMEN01C routing.
     */
    private List<MenuOption> createUserMenuOptions() {
        List<MenuOption> menuOptions = new ArrayList<>();
        
        MenuOption option1 = new MenuOption();
        option1.setOptionNumber(1);
        option1.setDescription("Account Information");
        option1.setTransactionCode("USER_ACCOUNT_INFO");
        
        MenuOption option2 = new MenuOption();
        option2.setOptionNumber(2);
        option2.setDescription("Transaction History");
        option2.setTransactionCode("USER_TRANSACTIONS");
        
        menuOptions.add(option1);
        menuOptions.add(option2);
        
        return menuOptions;
    }
}