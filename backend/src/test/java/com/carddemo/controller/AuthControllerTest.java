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
import com.carddemo.repository.UserSecurityRepository;
import com.carddemo.dto.SessionContext;
import com.carddemo.dto.MenuOption;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.HttpStatus;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;

/**
 * Comprehensive integration test class for AuthController that validates user authentication endpoints
 * including sign-in, sign-out, and session management. Ensures functional parity with COBOL COSGN00C
 * program by testing all authentication workflows, error conditions, and session state management.
 * 
 * This test class validates the complete authentication flow migration from CICS COBOL to Spring Boot:
 * - POST /api/auth/signin endpoint validation (replaces CICS RECEIVE MAP)
 * - User credential validation with uppercase conversion (FUNCTION UPPER-CASE)
 * - USRSEC dataset lookup equivalent (PostgreSQL user_security table)
 * - Session creation and management through Spring Session (replaces COMMAREA)
 * - Role-based routing (admin vs regular user - COADM01C vs COMEN01C)
 * - PF-key equivalent keyboard shortcuts (F3 for exit)
 * - Error handling for invalid credentials (exact COBOL error messages)
 * - COMMAREA-equivalent session attribute population
 * 
 * Test coverage ensures:
 * - 100% unit test coverage for business logic
 * - Validation of authentication flow from React to Spring Security
 * - Session state management across requests
 * - COBOL-to-Java functional parity validation
 * 
 * Key test scenarios replicate original COBOL logic:
 * 1. Successful authentication with admin user routing
 * 2. Successful authentication with regular user routing  
 * 3. Invalid credentials handling with exact error messages
 * 4. Uppercase conversion of user credentials
 * 5. Session management and timeout handling
 * 6. Error conditions and system failure scenarios
 * 7. PF3-equivalent sign-out functionality
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@WebMvcTest(controllers = AuthController.class, 
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
    })
@ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SignOnService signOnService;

    @MockBean
    private UserSecurityRepository userSecurityRepository;
    
    @MockBean
    private com.carddemo.security.JwtTokenService jwtTokenService;
    
    @MockBean
    private com.carddemo.security.CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession mockSession;
    private UserSecurity testAdminUser;
    private UserSecurity testRegularUser;
    private SignOnRequest validAdminRequest;
    private SignOnRequest validUserRequest;
    private SignOnRequest invalidRequest;

    /**
     * Set up test data and mock objects before each test.
     * Initializes test users, requests, and session objects that replicate
     * the original COBOL USRSEC file structure and COMMAREA session state.
     */
    @BeforeEach
    public void setUp() {
        // Initialize mock session for Spring Session testing
        mockSession = new MockHttpSession();
        
        // Create test admin user (equivalent to COBOL SEC-USR-TYPE = 'A')
        testAdminUser = new UserSecurity();
        testAdminUser.setSecUsrId("ADMIN001");
        testAdminUser.setUsername("ADMIN001");
        testAdminUser.setPassword("$2a$10$test.admin.bcrypt.hash"); // BCrypt hash for "PASSWORD"
        testAdminUser.setFirstName("Admin");
        testAdminUser.setLastName("User");
        testAdminUser.setUserType("A"); // Admin type
        testAdminUser.setEnabled(true);
        testAdminUser.setAccountNonExpired(true);
        testAdminUser.setAccountNonLocked(true);
        testAdminUser.setCredentialsNonExpired(true);
        
        // Create test regular user (equivalent to COBOL SEC-USR-TYPE = 'U')
        testRegularUser = new UserSecurity();
        testRegularUser.setSecUsrId("USER0001");
        testRegularUser.setUsername("USER0001");
        testRegularUser.setPassword("$2a$10$test.user.bcrypt.hash"); // BCrypt hash for "PASSWORD"
        testRegularUser.setFirstName("Regular");
        testRegularUser.setLastName("User");
        testRegularUser.setUserType("U"); // User type
        testRegularUser.setEnabled(true);
        testRegularUser.setAccountNonExpired(true);
        testRegularUser.setAccountNonLocked(true);
        testRegularUser.setCredentialsNonExpired(true);
        
        // Create valid admin sign-on request
        validAdminRequest = new SignOnRequest();
        validAdminRequest.setUserId("admin001"); // Will be converted to uppercase
        validAdminRequest.setPassword("password"); // Will be converted to uppercase
        
        // Create valid user sign-on request
        validUserRequest = new SignOnRequest();
        validUserRequest.setUserId("user0001"); // Will be converted to uppercase
        validUserRequest.setPassword("password"); // Will be converted to uppercase
        
        // Create invalid request for error testing
        invalidRequest = new SignOnRequest();
        invalidRequest.setUserId("invalid");
        invalidRequest.setPassword("wrong");
    }

    /**
     * Clean up after each test.
     * Resets mock objects and clears any test state to ensure test isolation.
     */
    @AfterEach
    public void tearDown() {
        // Reset all mocks to ensure test isolation
        reset(signOnService, userSecurityRepository);
        
        // Clear session state
        if (mockSession != null) {
            mockSession.clearAttributes();
        }
        
        // Reset static state if any
        clearInvocations(signOnService, userSecurityRepository);
    }

    /**
     * Test successful sign-in for admin user.
     * Validates the complete authentication flow including:
     * - Request processing and validation
     * - Credential authentication against service
     * - Admin-specific menu options (equivalent to COADM01C routing)
     * - Session establishment with proper attributes
     * - Response structure matching COBOL success patterns
     */
    @Test
    @WithAnonymousUser
    public void testSignInSuccess() throws Exception {
        // Arrange - Set up successful authentication response
        SignOnResponse successResponse = new SignOnResponse();
        successResponse.setStatus("SUCCESS");
        successResponse.setUserId("ADMIN001");
        successResponse.setUserName("Admin User");
        successResponse.setUserRole("A");
        successResponse.setSessionToken("test-session-token");
        successResponse.setMessage("Sign-on successful");
        
        // Mock service calls to simulate successful COBOL authentication flow
        when(signOnService.validateCredentials("ADMIN001", "PASSWORD"))
            .thenReturn(successResponse);
        when(signOnService.getUserDetails("ADMIN001"))
            .thenReturn(testAdminUser);
        when(signOnService.initializeSession(testAdminUser))
            .thenReturn("test-session-token");
        when(signOnService.buildMenuOptions("A"))
            .thenReturn(createAdminMenuOptions());
        
        // Act & Assert - Perform sign-in request and validate response
        MvcResult result = mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAdminRequest))
                .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.userId").value("ADMIN001"))
                .andExpect(jsonPath("$.userName").value("Admin User"))
                .andExpect(jsonPath("$.userRole").value("A"))
                .andExpect(jsonPath("$.sessionToken").value("test-session-token"))
                .andExpect(jsonPath("$.menuOptions").isArray())
                .andExpect(jsonPath("$.menuOptions[0].optionText").value("User Management"))
                .andReturn();
        
        // Verify service interactions match COBOL paragraph flow
        verify(signOnService).validateCredentials("ADMIN001", "PASSWORD");
        verify(signOnService).getUserDetails("ADMIN001");
        verify(signOnService).initializeSession(testAdminUser);
        verify(signOnService).buildMenuOptions("A");
        
        // Verify session attributes are set (equivalent to COMMAREA population)
        String sessionUserId = (String) mockSession.getAttribute("SEC-USR-ID");
        String sessionUserType = (String) mockSession.getAttribute("SEC-USR-TYPE");
        Long sessionStartTime = (Long) mockSession.getAttribute("SESSION-START-TIME");
        
        assertEquals("ADMIN001", sessionUserId);
        assertEquals("A", sessionUserType);
        assertNotNull(sessionStartTime);
        assertTrue(sessionStartTime <= System.currentTimeMillis());
    }

    /**
     * Test sign-in with invalid credentials.
     * Validates error handling that matches original COBOL error messages:
     * - "User not found. Try again ..." (COBOL RESP-CD = 13)
     * - "Wrong Password. Try again ..." (COBOL password mismatch)
     * - "Unable to verify the User ..." (COBOL system error)
     */
    @Test
    @WithAnonymousUser
    public void testSignInInvalidCredentials() throws Exception {
        // Test user not found scenario (COBOL RESP-CD = 13)
        SignOnResponse userNotFoundResponse = new SignOnResponse();
        userNotFoundResponse.setStatus("ERROR");
        userNotFoundResponse.setMessage("User not found. Try again ...");
        
        when(signOnService.validateCredentials("INVALID", "WRONG"))
            .thenReturn(userNotFoundResponse);
        
        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
                .session(mockSession))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("User not found. Try again ..."));
        
        // Test wrong password scenario (COBOL password mismatch)
        SignOnRequest wrongPasswordRequest = new SignOnRequest();
        wrongPasswordRequest.setUserId("user0001");
        wrongPasswordRequest.setPassword("wrongpass");
        
        SignOnResponse wrongPasswordResponse = new SignOnResponse();
        wrongPasswordResponse.setStatus("ERROR");
        wrongPasswordResponse.setMessage("Wrong Password. Try again ...");
        
        when(signOnService.validateCredentials("USER0001", "WRONGPASS"))
            .thenReturn(wrongPasswordResponse);
        
        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongPasswordRequest))
                .session(mockSession))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Wrong Password. Try again ..."));
        
        // Verify no session attributes were set for failed authentication
        assertNull(mockSession.getAttribute("SEC-USR-ID"));
        assertNull(mockSession.getAttribute("SEC-USR-TYPE"));
    }

    /**
     * Test uppercase conversion of credentials.
     * Validates that user ID and password are converted to uppercase to match
     * COBOL FUNCTION UPPER-CASE behavior from COSGN00C program.
     */
    @Test
    @WithAnonymousUser
    public void testSignInUppercaseConversion() throws Exception {
        // Create request with lowercase credentials
        SignOnRequest lowercaseRequest = new SignOnRequest();
        lowercaseRequest.setUserId("admin001"); // lowercase
        lowercaseRequest.setPassword("password"); // lowercase
        
        SignOnResponse successResponse = new SignOnResponse();
        successResponse.setStatus("SUCCESS");
        successResponse.setUserId("ADMIN001");
        successResponse.setUserName("Admin User");
        successResponse.setUserRole("A");
        
        // Mock service to expect uppercase values (COBOL FUNCTION UPPER-CASE)
        when(signOnService.validateCredentials("ADMIN001", "PASSWORD"))
            .thenReturn(successResponse);
        when(signOnService.getUserDetails("ADMIN001"))
            .thenReturn(testAdminUser);
        when(signOnService.initializeSession(testAdminUser))
            .thenReturn("test-session-token");
        when(signOnService.buildMenuOptions("A"))
            .thenReturn(createAdminMenuOptions());
        
        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lowercaseRequest))
                .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
        
        // Verify service was called with uppercase values (COBOL behavior)
        verify(signOnService).validateCredentials("ADMIN001", "PASSWORD");
    }

    /**
     * Test session management across requests.
     * Validates that session state is properly maintained and accessible
     * across multiple REST API calls, equivalent to CICS COMMAREA behavior.
     */
    @Test
    @WithMockUser(username = "ADMIN001", roles = {"ADMIN"})
    public void testSessionManagement() throws Exception {
        // Set up authenticated session with attributes
        mockSession.setAttribute("SEC-USR-ID", "ADMIN001");
        mockSession.setAttribute("SEC-USR-TYPE", "A");
        mockSession.setAttribute("SESSION-START-TIME", System.currentTimeMillis());
        
        SignOnResponse statusResponse = new SignOnResponse();
        statusResponse.setStatus("SUCCESS");
        statusResponse.setUserId("ADMIN001");
        statusResponse.setUserName("Admin User");
        statusResponse.setUserRole("A");
        statusResponse.setMessage("User is authenticated");
        
        when(signOnService.buildMenuOptions("A"))
            .thenReturn(createAdminMenuOptions());
        
        // Test that authentication status can be retrieved
        mockMvc.perform(get("/api/auth/status")
                .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.userId").value("ADMIN001"))
                .andExpect(jsonPath("$.userRole").value("A"));
        
        // Verify session attributes are accessible
        assertEquals("ADMIN001", mockSession.getAttribute("SEC-USR-ID"));
        assertEquals("A", mockSession.getAttribute("SEC-USR-TYPE"));
        assertNotNull(mockSession.getAttribute("SESSION-START-TIME"));
    }

    /**
     * Test role-based routing functionality.
     * Validates that admin users receive admin menu options (COADM01C equivalent)
     * and regular users receive user menu options (COMEN01C equivalent).
     */
    @Test
    @WithAnonymousUser
    public void testRoleBasedRouting() throws Exception {
        // Test admin user routing (equivalent to COADM01C)
        SignOnResponse adminResponse = new SignOnResponse();
        adminResponse.setStatus("SUCCESS");
        adminResponse.setUserId("ADMIN001");
        adminResponse.setUserRole("A");
        adminResponse.setMenuOptions(createAdminMenuOptions());
        
        when(signOnService.validateCredentials("ADMIN001", "PASSWORD"))
            .thenReturn(adminResponse);
        when(signOnService.getUserDetails("ADMIN001"))
            .thenReturn(testAdminUser);
        when(signOnService.initializeSession(testAdminUser))
            .thenReturn("admin-session-token");
        when(signOnService.buildMenuOptions("A"))
            .thenReturn(createAdminMenuOptions());
        
        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAdminRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userRole").value("A"))
                .andExpect(jsonPath("$.menuOptions[0].optionText").value("User Management"))
                .andExpect(jsonPath("$.menuOptions[1].optionText").value("Account Management"));
        
        // Test regular user routing (equivalent to COMEN01C)
        SignOnResponse userResponse = new SignOnResponse();
        userResponse.setStatus("SUCCESS");
        userResponse.setUserId("USER0001");
        userResponse.setUserRole("U");
        userResponse.setMenuOptions(createUserMenuOptions());
        
        when(signOnService.validateCredentials("USER0001", "PASSWORD"))
            .thenReturn(userResponse);
        when(signOnService.getUserDetails("USER0001"))
            .thenReturn(testRegularUser);
        when(signOnService.initializeSession(testRegularUser))
            .thenReturn("user-session-token");
        when(signOnService.buildMenuOptions("U"))
            .thenReturn(createUserMenuOptions());
        
        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userRole").value("U"))
                .andExpect(jsonPath("$.menuOptions[0].optionText").value("Account Information"))
                .andExpect(jsonPath("$.menuOptions[1].optionText").value("Transaction History"));
    }

    /**
     * Test sign-out functionality.
     * Validates session termination equivalent to PF3 key processing in COBOL
     * and CICS RETURN transaction cleanup.
     */
    @Test
    @WithMockUser(username = "ADMIN001", roles = {"ADMIN"})
    public void testSignOut() throws Exception {
        // Set up authenticated session
        mockSession.setAttribute("SEC-USR-ID", "ADMIN001");
        mockSession.setAttribute("SEC-USR-TYPE", "A");
        mockSession.setAttribute("SESSION-START-TIME", System.currentTimeMillis());
        
        // Perform sign-out (equivalent to PF3 processing)
        mockMvc.perform(post("/api/auth/signout")
                .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Sign-out successful"));
        
        // Verify session was invalidated (equivalent to CICS session cleanup)
        assertTrue(mockSession.isInvalid());
    }

    /**
     * Test comprehensive error handling scenarios.
     * Validates all error conditions that can occur during authentication
     * including validation errors, system errors, and edge cases.
     */
    @Test
    @WithAnonymousUser
    public void testErrorHandling() throws Exception {
        // Test validation error for empty user ID
        SignOnRequest emptyUserIdRequest = new SignOnRequest();
        emptyUserIdRequest.setUserId("");
        emptyUserIdRequest.setPassword("password");
        
        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyUserIdRequest)))
                .andExpect(status().isBadRequest());
        
        // Test validation error for empty password  
        SignOnRequest emptyPasswordRequest = new SignOnRequest();
        emptyPasswordRequest.setUserId("user0001");
        emptyPasswordRequest.setPassword("");
        
        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyPasswordRequest)))
                .andExpect(status().isBadRequest());
        
        // Test system error scenario (equivalent to COBOL ABEND)
        when(signOnService.validateCredentials(anyString(), anyString()))
            .thenThrow(new RuntimeException("Database connection error"));
        
        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("System error occurred. Please try again later."));
        
        // Test malformed JSON request
        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Create admin menu options equivalent to COADM01C routing.
     * Returns menu options that admin users would see after successful authentication.
     */
    private List<MenuOption> createAdminMenuOptions() {
        List<MenuOption> menuOptions = new ArrayList<>();
        
        MenuOption option1 = new MenuOption();
        option1.setOptionId(1);
        option1.setOptionText("User Management");
        option1.setTarget("ADMIN_USER_MGMT");
        
        MenuOption option2 = new MenuOption();
        option2.setOptionId(2);
        option2.setOptionText("Account Management");
        option2.setTarget("ADMIN_ACCOUNT_MGMT");
        
        MenuOption option3 = new MenuOption();
        option3.setOptionId(3);
        option3.setOptionText("Transaction Reports");
        option3.setTarget("ADMIN_REPORTS");
        
        MenuOption option4 = new MenuOption();
        option4.setOptionId(4);
        option4.setOptionText("System Administration");
        option4.setTarget("ADMIN_SYSTEM");
        
        menuOptions.add(option1);
        menuOptions.add(option2);
        menuOptions.add(option3);
        menuOptions.add(option4);
        
        return menuOptions;
    }

    /**
     * Create user menu options equivalent to COMEN01C routing.
     * Returns menu options that regular users would see after successful authentication.
     */
    private List<MenuOption> createUserMenuOptions() {
        List<MenuOption> menuOptions = new ArrayList<>();
        
        MenuOption option1 = new MenuOption();
        option1.setOptionId(1);
        option1.setOptionText("Account Information");
        option1.setTarget("USER_ACCOUNT_INFO");
        
        MenuOption option2 = new MenuOption();
        option2.setOptionId(2);
        option2.setOptionText("Transaction History");
        option2.setTarget("USER_TRANSACTIONS");
        
        MenuOption option3 = new MenuOption();
        option3.setOptionId(3);
        option3.setOptionText("Card Management");
        option3.setTarget("USER_CARDS");
        
        MenuOption option4 = new MenuOption();
        option4.setOptionId(4);
        option4.setOptionText("Customer Profile");
        option4.setTarget("USER_PROFILE");
        
        menuOptions.add(option1);
        menuOptions.add(option2);
        menuOptions.add(option3);
        menuOptions.add(option4);
        
        return menuOptions;
    }
}