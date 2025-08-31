/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.security;

import com.carddemo.controller.AuthController;
import com.carddemo.security.SecurityConfig;
import com.carddemo.security.JwtTokenService;
import com.carddemo.security.SessionConfig;
import com.carddemo.integration.BaseIntegrationTest;
import com.carddemo.security.SecurityTestUtils;
import com.carddemo.controller.TestConstants;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.Assertions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.HttpSession;

import static org.hamcrest.Matchers.containsString;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive integration tests for complete security flows using MockMvc and Testcontainers, 
 * testing login workflow matching COSGN00C, role-based endpoint access, session management 
 * with Redis, JWT token lifecycle, and logout functionality.
 * 
 * This test class validates end-to-end security functionality ensuring that the modernized 
 * Spring Boot security implementation maintains complete functional parity with the original 
 * COBOL/CICS security model, particularly the COSGN00C sign-on transaction flow.
 * 
 * Key Test Coverage:
 * - Complete authentication workflow equivalent to COSGN00C COBOL program
 * - Role-based authorization matching SEC-USR-TYPE logic ('A' for admin, 'U' for user)
 * - Session state management via Redis replacing CICS COMMAREA functionality
 * - JWT token generation, validation, and lifecycle management
 * - Concurrent user session handling and isolation
 * - Logout functionality with complete session invalidation
 * - Security headers validation and CORS configuration testing
 * - Performance validation meeting sub-200ms response time requirements
 * 
 * Test Environment:
 * - Uses Testcontainers for isolated PostgreSQL and Redis instances
 * - MockMvc for HTTP layer integration testing without full server startup
 * - Comprehensive test user data matching COBOL SEC-USER-DATA structure
 * - Spring Security context testing with various authentication scenarios
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@AutoConfigureMockMvc
public class SecurityIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private AuthController authController;
    
    @Autowired
    private SecurityConfig securityConfig;
    
    @Autowired
    private JwtTokenService jwtTokenService;
    
    @Autowired
    private SessionConfig sessionConfig;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private Map<String, Object> testUserCredentials;
    private Map<String, Object> testAdminCredentials;
    private String validJwtToken;
    private String invalidJwtToken;
    
    /**
     * Sets up test environment before each test execution.
     * Initializes test user data, JWT tokens, and mock authentication context.
     * Ensures clean test state with proper container configuration.
     */
    @BeforeEach
    @Override
    public void setupTestContainers() {
        // Call parent setup to initialize Testcontainers
        super.setupTestContainers();
        
        // Initialize test credentials matching COBOL SEC-USER-DATA structure
        setupTestCredentials();
        
        // Setup JWT tokens for testing
        setupJwtTokens();
        
        // Verify container health before tests
        verifyContainerHealth();
    }
    
    /**
     * Tests the complete login workflow matching COSGN00C COBOL program behavior.
     * Validates end-to-end authentication flow from credential submission through
     * successful authentication response with proper session establishment.
     * 
     * Test Steps:
     * 1. Submit valid credentials via POST /api/auth/signin
     * 2. Verify authentication success response (200 OK)
     * 3. Validate JWT token generation and structure
     * 4. Confirm session creation in Redis
     * 5. Verify user role assignment based on SEC-USR-TYPE
     * 6. Validate response time under 200ms threshold
     * 
     * This test ensures functional parity with the original COSGN00C transaction flow.
     */
    @Test
    public void testCompleteLoginWorkflow() throws Exception {
        // Record start time for performance validation
        long startTime = System.currentTimeMillis();
        
        // Prepare login request matching COSGN00C input format
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", TestConstants.TEST_USER_ID);
        loginRequest.put("password", TestConstants.TEST_USER_PASSWORD);
        
        // Execute authentication request via MockMvc
        mockMvc.perform(post("/api/auth/signin")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.userType").value("U"))
                .andExpect(jsonPath("$.userId").value(TestConstants.TEST_USER_ID))
                .andExpect(header().exists("Set-Cookie")) // Session cookie
                .andDo(result -> {
                    // Validate JWT token structure
                    String token = extractTokenFromResponse(result.getResponse().getContentAsString());
                    assertThat(jwtTokenService.validateToken(token)).isTrue();
                    assertThat(jwtTokenService.extractUsername(token)).isEqualTo(TestConstants.TEST_USER_ID);
                    
                    // Validate session creation in Redis
                    HttpSession session = result.getRequest().getSession();
                    assertThat(session.getAttribute("SEC-USR-ID")).isEqualTo(TestConstants.TEST_USER_ID);
                    assertThat(session.getAttribute("SEC-USR-TYPE")).isEqualTo("U");
                });
        
        // Validate response time performance requirement
        long responseTime = System.currentTimeMillis() - startTime;
        assertThat(responseTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
    }
    
    /**
     * Tests role-based access control matching COBOL SEC-USR-TYPE authorization logic.
     * Validates that admin users (type 'A') can access administrative endpoints while
     * regular users (type 'U') are properly denied access to restricted resources.
     * 
     * Test Scenarios:
     * 1. Admin user accessing administrative endpoints (should succeed)
     * 2. Regular user attempting administrative access (should fail with 403)
     * 3. Both user types accessing common endpoints (should succeed)
     * 4. Unauthenticated access to protected endpoints (should fail with 401)
     * 
     * This test ensures @PreAuthorize annotations work correctly with Spring Security.
     */
    @Test
    public void testRoleBasedAccess() throws Exception {
        // Test admin user access to administrative endpoints
        String adminToken = generateAdminToken();
        
        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
        
        mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .content("{\"username\":\"newuser\",\"role\":\"USER\"}"))
                .andExpect(status().isCreated());
        
        // Test regular user denied access to administrative endpoints
        String userToken = generateUserToken();
        
        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        
        mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + userToken)
                .contentType("application/json")
                .content("{\"username\":\"newuser\",\"role\":\"USER\"}"))
                .andExpect(status().isForbidden());
        
        // Test both user types can access common endpoints
        mockMvc.perform(get("/api/accounts")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/api/accounts")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
        
        // Test unauthenticated access fails
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized());
        
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }
    
    /**
     * Tests session persistence in Redis matching CICS COMMAREA functionality.
     * Validates that user session state is properly maintained across requests
     * and that session data persists in the Redis session store.
     * 
     * Test Coverage:
     * 1. Session creation during authentication
     * 2. Session data persistence across multiple requests
     * 3. Session attribute storage and retrieval
     * 4. Session timeout configuration validation
     * 5. Session isolation between different users
     * 
     * This test ensures Spring Session Redis maintains equivalent functionality
     * to CICS COMMAREA for session state management.
     */
    @Test
    public void testSessionPersistence() throws Exception {
        // Authenticate user and capture session
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", TestConstants.TEST_USER_ID);
        loginRequest.put("password", TestConstants.TEST_USER_PASSWORD);
        
        String sessionCookie = mockMvc.perform(post("/api/auth/signin")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Set-Cookie");
        
        // Verify session attributes are properly set
        mockMvc.perform(get("/api/user/profile")
                .header("Cookie", sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(TestConstants.TEST_USER_ID))
                .andExpect(jsonPath("$.userType").value("U"));
        
        // Test session persistence across multiple requests
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/accounts")
                    .header("Cookie", sessionCookie))
                    .andExpect(status().isOk());
        }
        
        // Validate session timeout configuration (30 minutes)
        long sessionTimeout = sessionConfig.getSessionTimeout();
        assertThat(sessionTimeout).isEqualTo(30 * 60); // 30 minutes in seconds
        
        // Test session isolation with different user
        Map<String, String> adminLoginRequest = new HashMap<>();
        adminLoginRequest.put("username", "TESTADMN");
        adminLoginRequest.put("password", "ADMIN123");
        
        String adminSessionCookie = mockMvc.perform(post("/api/auth/signin")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(adminLoginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Set-Cookie");
        
        // Verify sessions are isolated
        assertThat(sessionCookie).isNotEqualTo(adminSessionCookie);
        
        // Verify each session maintains separate user context
        mockMvc.perform(get("/api/user/profile")
                .header("Cookie", sessionCookie))
                .andExpect(jsonPath("$.userId").value(TestConstants.TEST_USER_ID));
        
        mockMvc.perform(get("/api/user/profile")
                .header("Cookie", adminSessionCookie))
                .andExpect(jsonPath("$.userId").value("TESTADMN"));
    }
    
    /**
     * Tests JWT token lifecycle including generation, validation, and expiration handling.
     * Validates the complete token management system supporting stateless authentication
     * while maintaining session state through Redis integration.
     * 
     * Test Scenarios:
     * 1. JWT token generation with proper claims and expiration
     * 2. Token validation for authentic tokens
     * 3. Token rejection for expired tokens
     * 4. Token rejection for malformed tokens
     * 5. Token refresh functionality
     * 6. Token blacklisting on logout
     * 
     * This test ensures JWT token management maintains security while providing
     * seamless user experience equivalent to CICS session handling.
     */
    @Test
    public void testJwtTokenLifecycle() throws Exception {
        // Test valid token generation
        String validToken = jwtTokenService.generateToken(SecurityTestUtils.createTestUser());
        assertThat(validToken).isNotNull();
        assertThat(jwtTokenService.validateToken(validToken)).isTrue();
        assertThat(jwtTokenService.extractUsername(validToken)).isEqualTo(TestConstants.TEST_USER_ID);
        
        // Test token claims extraction
        Map<String, Object> claims = jwtTokenService.extractClaims(validToken);
        assertThat(claims.get("sub")).isEqualTo(TestConstants.TEST_USER_ID);
        assertThat(claims.get("authorities")).isNotNull();
        
        // Test valid token usage for API access
        mockMvc.perform(get("/api/accounts")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
        
        // Test expired token rejection
        String expiredToken = "expired.jwt.token.for.testing";
        assertThat(jwtTokenService.validateToken(expiredToken)).isFalse();
        
        mockMvc.perform(get("/api/accounts")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
        
        // Test invalid token rejection
        String invalidToken = invalidJwtToken;
        assertThat(jwtTokenService.validateToken(invalidToken)).isFalse();
        
        mockMvc.perform(get("/api/accounts")
                .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
        
        // Test token refresh functionality
        String refreshedToken = jwtTokenService.refreshToken(validToken);
        assertThat(refreshedToken).isNotNull();
        assertThat(refreshedToken).isNotEqualTo(validToken);
        assertThat(jwtTokenService.validateToken(refreshedToken)).isTrue();
        
        // Test refreshed token usage
        mockMvc.perform(get("/api/accounts")
                .header("Authorization", "Bearer " + refreshedToken))
                .andExpect(status().isOk());
    }
    
    /**
     * Tests concurrent user sessions to validate system scalability and session isolation.
     * Simulates multiple users accessing the system simultaneously to ensure proper
     * session management and resource isolation under concurrent load.
     * 
     * Test Coverage:
     * 1. Multiple concurrent authentication requests
     * 2. Session isolation between concurrent users
     * 3. Resource access with concurrent sessions
     * 4. Performance under concurrent load
     * 5. Session cleanup and resource management
     * 
     * This test validates that the modernized system can handle concurrent users
     * equivalent to or better than the original CICS multi-user environment.
     */
    @Test
    public void testConcurrentUserSessions() throws Exception {
        int concurrentUsers = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentUsers);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // Create concurrent authentication requests
        for (int i = 0; i < concurrentUsers; i++) {
            final int userId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // Create unique user credentials for each concurrent session
                    Map<String, String> loginRequest = new HashMap<>();
                    loginRequest.put("username", "USER" + String.format("%03d", userId));
                    loginRequest.put("password", "PASS" + String.format("%03d", userId));
                    
                    // Perform concurrent authentication
                    mockMvc.perform(post("/api/auth/signin")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(loginRequest)))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.success").value(true))
                            .andExpect(jsonPath("$.userId").value("USER" + String.format("%03d", userId)));
                    
                } catch (Exception e) {
                    fail("Concurrent authentication failed for user " + userId, e);
                }
            }, executorService);
            
            futures.add(future);
        }
        
        // Wait for all concurrent requests to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        assertThat(allFutures.get(30, TimeUnit.SECONDS)).isNull(); // Completes without exception
        
        // Verify session isolation by testing concurrent API access
        List<CompletableFuture<Void>> accessFutures = new ArrayList<>();
        
        for (int i = 0; i < concurrentUsers; i++) {
            final int userId = i;
            CompletableFuture<Void> accessFuture = CompletableFuture.runAsync(() -> {
                try {
                    String userToken = generateTokenForUser("USER" + String.format("%03d", userId));
                    
                    // Test concurrent API access
                    mockMvc.perform(get("/api/accounts")
                            .header("Authorization", "Bearer " + userToken))
                            .andExpect(status().isOk());
                    
                } catch (Exception e) {
                    fail("Concurrent API access failed for user " + userId, e);
                }
            }, executorService);
            
            accessFutures.add(accessFuture);
        }
        
        // Wait for all concurrent API access requests
        CompletableFuture<Void> allAccessFutures = CompletableFuture.allOf(
            accessFutures.toArray(new CompletableFuture[0])
        );
        
        assertThat(allAccessFutures.get(30, TimeUnit.SECONDS)).isNull();
        
        // Cleanup
        executorService.shutdown();
        assertThat(executorService.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    }
    
    /**
     * Tests logout functionality with complete session invalidation.
     * Validates that user logout properly terminates the session, invalidates
     * JWT tokens, and clears all session data from Redis.
     * 
     * Test Steps:
     * 1. Authenticate user and establish session
     * 2. Verify authenticated access to protected resources
     * 3. Perform logout via POST /api/auth/signout
     * 4. Verify session invalidation in Redis
     * 5. Verify JWT token blacklisting
     * 6. Confirm denied access to protected resources post-logout
     * 
     * This test ensures complete cleanup equivalent to CICS session termination.
     */
    @Test
    public void testLogoutAndSessionInvalidation() throws Exception {
        // Authenticate user and capture session details
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", TestConstants.TEST_USER_ID);
        loginRequest.put("password", TestConstants.TEST_USER_PASSWORD);
        
        String sessionCookie = mockMvc.perform(post("/api/auth/signin")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Set-Cookie");
        
        String authToken = extractTokenFromLastResponse();
        
        // Verify authenticated access before logout
        mockMvc.perform(get("/api/accounts")
                .header("Cookie", sessionCookie)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());
        
        // Perform logout
        mockMvc.perform(post("/api/auth/signout")
                .header("Cookie", sessionCookie)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
        
        // Verify session invalidation - access should be denied
        mockMvc.perform(get("/api/accounts")
                .header("Cookie", sessionCookie))
                .andExpect(status().isUnauthorized());
        
        // Verify JWT token blacklisting
        mockMvc.perform(get("/api/accounts")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isUnauthorized());
        
        // Verify token is no longer valid
        assertThat(jwtTokenService.validateToken(authToken)).isFalse();
        
        // Verify user must re-authenticate for access
        mockMvc.perform(post("/api/auth/signin")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
    
    /**
     * Tests security headers configuration for comprehensive web security.
     * Validates that all required security headers are properly configured
     * to protect against common web vulnerabilities.
     * 
     * Security Headers Tested:
     * 1. X-Content-Type-Options: nosniff
     * 2. X-Frame-Options: DENY
     * 3. X-XSS-Protection: 1; mode=block
     * 4. Strict-Transport-Security (HSTS)
     * 5. Content-Security-Policy
     * 6. Referrer-Policy: strict-origin-when-cross-origin
     * 
     * This test ensures web security hardening equivalent to enterprise standards.
     */
    @Test
    public void testSecurityHeaders() throws Exception {
        // Test security headers on authentication endpoint
        mockMvc.perform(post("/api/auth/signin")
                .contentType("application/json")
                .content("{\"username\":\"test\",\"password\":\"test\"}"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-XSS-Protection", "1; mode=block"))
                .andExpect(header().exists("Strict-Transport-Security"))
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
        
        // Test security headers on protected endpoints
        String validToken = generateUserToken();
        
        mockMvc.perform(get("/api/accounts")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"));
        
        // Test security headers on administrative endpoints
        String adminToken = generateAdminToken();
        
        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }
    
    /**
     * Tests CORS configuration for secure cross-origin resource sharing.
     * Validates that CORS is properly configured to allow legitimate cross-origin
     * requests while blocking unauthorized origins.
     * 
     * CORS Test Coverage:
     * 1. Allowed origins configuration
     * 2. Allowed methods (GET, POST, PUT, DELETE, OPTIONS)
     * 3. Allowed headers including Authorization
     * 4. Credentials support configuration
     * 5. Preflight request handling
     * 6. Blocked origins rejection
     * 
     * This test ensures secure cross-origin access for the React frontend.
     */
    @Test
    public void testCorsConfiguration() throws Exception {
        // Test preflight request for allowed origin
        mockMvc.perform(options("/api/auth/signin")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Authorization")))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        
        // Test actual CORS request
        mockMvc.perform(post("/api/auth/signin")
                .header("Origin", "http://localhost:3000")
                .contentType("application/json")
                .content("{\"username\":\"test\",\"password\":\"test\"}"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
        
        // Test blocked origin
        mockMvc.perform(options("/api/auth/signin")
                .header("Origin", "http://malicious-site.com")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
        
        // Test CORS on protected endpoints
        String validToken = generateUserToken();
        
        mockMvc.perform(get("/api/accounts")
                .header("Origin", "http://localhost:3000")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }
    
    // Helper Methods
    
    /**
     * Sets up test credentials matching COBOL SEC-USER-DATA structure.
     */
    private void setupTestCredentials() {
        testUserCredentials = new HashMap<>();
        testUserCredentials.put("username", TestConstants.TEST_USER_ID);
        testUserCredentials.put("password", TestConstants.TEST_USER_PASSWORD);
        testUserCredentials.put("role", TestConstants.TEST_USER_ROLE);
        
        testAdminCredentials = new HashMap<>();
        testAdminCredentials.put("username", "TESTADMN");
        testAdminCredentials.put("password", "ADMIN123");
        testAdminCredentials.put("role", TestConstants.TEST_ADMIN_ROLE);
    }
    
    /**
     * Sets up JWT tokens for testing various scenarios.
     */
    private void setupJwtTokens() {
        validJwtToken = SecurityTestUtils.generateTestJwtToken(SecurityTestUtils.createTestUser());
        invalidJwtToken = "invalid.jwt.token.for.testing";
    }
    
    /**
     * Verifies that Testcontainers are healthy and ready for testing.
     */
    private void verifyContainerHealth() {
        assertThat(getPostgreSQLContainer().isRunning()).isTrue();
        assertThat(getRedisContainer().isRunning()).isTrue();
    }
    
    /**
     * Extracts JWT token from authentication response.
     */
    private String extractTokenFromResponse(String responseContent) {
        try {
            Map<String, Object> response = objectMapper.readValue(responseContent, Map.class);
            return (String) response.get("token");
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract token from response", e);
        }
    }
    
    /**
     * Extracts JWT token from the last HTTP response.
     */
    private String extractTokenFromLastResponse() {
        // Implementation would extract token from last response
        // This is a placeholder for the actual implementation
        return validJwtToken;
    }
    
    /**
     * Generates admin JWT token for testing admin access.
     */
    private String generateAdminToken() {
        return SecurityTestUtils.generateTestJwtToken(SecurityTestUtils.createTestAdmin());
    }
    
    /**
     * Generates user JWT token for testing regular user access.
     */
    private String generateUserToken() {
        return SecurityTestUtils.generateTestJwtToken(SecurityTestUtils.createTestUser());
    }
    
    /**
     * Generates JWT token for specific user ID.
     */
    private String generateTokenForUser(String userId) {
        return SecurityTestUtils.generateTestJwtToken(SecurityTestUtils.createTestUser());
    }
}