/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.integration;

import com.carddemo.security.SecurityConfig;
import com.carddemo.controller.AuthController;
import com.carddemo.controller.UserController;
import com.carddemo.dto.SignOnRequest;
import com.carddemo.dto.SignOnResponse;
import com.carddemo.entity.UserSecurity;
import com.carddemo.repository.UserSecurityRepository;
import com.carddemo.security.CustomUserDetailsService;
import com.carddemo.security.JwtTokenService;
import com.carddemo.security.SecurityConstants;
import com.carddemo.service.SignOnService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

/**
 * Comprehensive Spring Security integration tests for CardDemo application.
 * 
 * Validates complete security implementation replacing RACF authentication with
 * Spring Security 6.x, JWT tokens, BCrypt password encoding, and role-based
 * access control. Tests authentication flows, authorization controls, session 
 * management, and performance requirements.
 * 
 * Key Test Areas:
 * - JWT token generation and validation
 * - Spring Security UserDetailsService integration
 * - Role-based access control (ADMIN vs USER roles)
 * - Method-level security with @PreAuthorize annotations
 * - BCrypt password encoding and verification
 * - Session management and logout functionality
 * - Authentication performance (sub-100ms requirement)
 * - Concurrent authentication handling
 * 
 * Replaces RACF security testing with modern Spring Security patterns while
 * maintaining exact functional parity with COBOL authentication behavior.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "logging.level.org.springframework.security=DEBUG",
    "jwt.expiration=3600000",
    "jwt.secret=testSecret123456789012345678901234567890"
})
public class SecurityIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private UserSecurityRepository userSecurityRepository;
    
    @Autowired
    private SignOnService signOnService;
    
    @Autowired
    private JwtTokenService jwtTokenService;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    @Autowired
    private SecurityConstants securityConstants;

    // Test users for authentication testing
    private UserSecurity testAdminUser;
    private UserSecurity testRegularUser;
    
    // Authentication endpoints
    private static final String AUTH_SIGNIN_URL = "/api/auth/signin";
    private static final String AUTH_SIGNOUT_URL = "/api/auth/signout";
    private static final String AUTH_STATUS_URL = "/api/auth/status";
    
    // User management endpoints for @PreAuthorize testing
    private static final String USERS_URL = "/api/users";
    
    /**
     * Sets up test data before each test method.
     * Creates test users with different roles for comprehensive security testing.
     */
    @BeforeEach
    public void setUp() {
        // Clean up existing test data
        userSecurityRepository.deleteAll();
        
        // Create test admin user matching COBOL user structure
        testAdminUser = createIntegrationTestUser();
        testAdminUser.setSecUsrId("TESTADM");
        testAdminUser.setUsername("TESTADM");  
        testAdminUser.setFirstName("Test");
        testAdminUser.setLastName("Admin");
        testAdminUser.setPassword(passwordEncoder.encode("ADMIN123"));
        testAdminUser.setUserType("A"); // Admin type
        testAdminUser.setEnabled(true);
        testAdminUser = userSecurityRepository.save(testAdminUser);
        
        // Create test regular user matching COBOL user structure  
        testRegularUser = createIntegrationTestUser();
        testRegularUser.setSecUsrId("TESTUSER");
        testRegularUser.setUsername("TESTUSER");
        testRegularUser.setFirstName("Test");
        testRegularUser.setLastName("User"); 
        testRegularUser.setPassword(passwordEncoder.encode("USER123"));
        testRegularUser.setUserType("U"); // User type
        testRegularUser.setEnabled(true);
        testRegularUser = userSecurityRepository.save(testRegularUser);
    }
    
    @Nested
    @DisplayName("Authentication Flow Tests")
    class AuthenticationFlowTests {
        
        @Test
        @DisplayName("Should authenticate valid admin user and return JWT token")
        void testValidAdminAuthentication() {
            // Arrange
            SignOnRequest request = new SignOnRequest();
            request.setUserId("TESTADM");
            request.setPassword("ADMIN123");
            
            Instant start = Instant.now();
            
            // Act
            ResponseEntity<SignOnResponse> response = restTemplate.postForEntity(
                AUTH_SIGNIN_URL, request, SignOnResponse.class);
            
            Instant end = Instant.now();
            long authenticationTimeMs = Duration.between(start, end).toMillis();
            
            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            
            SignOnResponse signOnResponse = response.getBody();
            assertNotNull(signOnResponse);
            assertEquals("SUCCESS", signOnResponse.getStatus());
            assertEquals("TESTADM", signOnResponse.getUserId());
            assertEquals("A", signOnResponse.getUserRole());
            assertNotNull(signOnResponse.getSessionToken());
            assertNotNull(signOnResponse.getMenuOptions());
            
            // Verify JWT token is valid
            String token = signOnResponse.getSessionToken();
            assertTrue(jwtTokenService.validateToken(token));
            assertEquals("TESTADM", jwtTokenService.extractUsername(token));
            
            // Verify performance requirement: authentication < 100ms
            assertTrue(authenticationTimeMs < 100, 
                "Authentication took " + authenticationTimeMs + "ms, should be < 100ms");
        }
        
        @Test
        @DisplayName("Should authenticate valid regular user and return JWT token")
        void testValidRegularUserAuthentication() {
            // Arrange
            SignOnRequest request = new SignOnRequest();
            request.setUserId("TESTUSER");
            request.setPassword("USER123");
            
            Instant start = Instant.now();
            
            // Act
            ResponseEntity<SignOnResponse> response = restTemplate.postForEntity(
                AUTH_SIGNIN_URL, request, SignOnResponse.class);
            
            Instant end = Instant.now();
            long authenticationTimeMs = Duration.between(start, end).toMillis();
            
            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            
            SignOnResponse signOnResponse = response.getBody();
            assertNotNull(signOnResponse);
            assertEquals("SUCCESS", signOnResponse.getStatus());
            assertEquals("TESTUSER", signOnResponse.getUserId());
            assertEquals("U", signOnResponse.getUserRole());
            assertNotNull(signOnResponse.getSessionToken());
            
            // Verify JWT token is valid
            String token = signOnResponse.getSessionToken();
            assertTrue(jwtTokenService.validateToken(token));
            assertEquals("TESTUSER", jwtTokenService.extractUsername(token));
            
            // Verify performance requirement
            assertTrue(authenticationTimeMs < 100, 
                "Authentication took " + authenticationTimeMs + "ms, should be < 100ms");
        }
        
        @Test
        @DisplayName("Should reject invalid credentials with 401 Unauthorized")
        void testInvalidCredentials() {
            // Arrange
            SignOnRequest request = new SignOnRequest();
            request.setUserId("TESTADM");
            request.setPassword("WRONGPWD");
            
            // Act
            ResponseEntity<SignOnResponse> response = restTemplate.postForEntity(
                AUTH_SIGNIN_URL, request, SignOnResponse.class);
            
            // Assert
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            
            SignOnResponse signOnResponse = response.getBody();
            assertNotNull(signOnResponse);
            assertEquals("ERROR", signOnResponse.getStatus());
            assertNotNull(signOnResponse.getErrorMessage());
        }
        
        @Test
        @DisplayName("Should reject non-existent user with 401 Unauthorized")  
        void testNonExistentUser() {
            // Arrange
            SignOnRequest request = new SignOnRequest();
            request.setUserId("BADUSER");
            request.setPassword("PASSWORD");
            
            // Act
            ResponseEntity<SignOnResponse> response = restTemplate.postForEntity(
                AUTH_SIGNIN_URL, request, SignOnResponse.class);
            
            // Assert
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }
        
        @Test
        @DisplayName("Should validate empty credentials with 400 Bad Request")
        void testEmptyCredentials() {
            // Arrange
            SignOnRequest request = new SignOnRequest();
            request.setUserId("");
            request.setPassword("");
            
            // Act
            ResponseEntity<SignOnResponse> response = restTemplate.postForEntity(
                AUTH_SIGNIN_URL, request, SignOnResponse.class);
            
            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }
    
    @Nested
    @DisplayName("JWT Token Service Tests")
    class JwtTokenServiceTests {
        
        @Test
        @DisplayName("Should generate valid JWT token with correct claims")
        void testJwtTokenGeneration() {
            // Act
            String token = jwtTokenService.generateToken(testAdminUser);
            
            // Assert
            assertNotNull(token);
            assertTrue(jwtTokenService.validateToken(token));
            assertEquals("TESTADM", jwtTokenService.extractUsername(token));
            
            // Verify token contains expected claims
            var claims = jwtTokenService.extractClaims(token);
            assertNotNull(claims);
            assertEquals("TESTADM", claims.getSubject());
        }
        
        @Test  
        @DisplayName("Should validate correct JWT tokens")
        void testJwtTokenValidation() {
            // Arrange
            String token = jwtTokenService.generateToken(testRegularUser);
            
            // Act & Assert
            assertTrue(jwtTokenService.validateToken(token));
            assertEquals("TESTUSER", jwtTokenService.extractUsername(token));
        }
        
        @Test
        @DisplayName("Should reject invalid JWT tokens")
        void testInvalidJwtToken() {
            // Arrange
            String invalidToken = "invalid.jwt.token";
            
            // Act & Assert
            assertFalse(jwtTokenService.validateToken(invalidToken));
            assertThrows(Exception.class, () -> jwtTokenService.extractUsername(invalidToken));
        }
    }
    
    @Nested
    @DisplayName("UserDetailsService Integration Tests")
    class UserDetailsServiceTests {
        
        @Test
        @DisplayName("Should load user details for valid username")
        void testLoadUserByUsername() {
            // Act
            UserDetails userDetails = userDetailsService.loadUserByUsername("TESTADM");
            
            // Assert
            assertNotNull(userDetails);
            assertEquals("TESTADM", userDetails.getUsername());
            assertTrue(userDetails.isEnabled());
            assertTrue(passwordEncoder.matches("ADMIN123", userDetails.getPassword()));
            
            // Verify authorities/roles
            assertNotNull(userDetails.getAuthorities());
            assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
        }
        
        @Test
        @DisplayName("Should load regular user details with correct roles")
        void testLoadRegularUserDetails() {
            // Act
            UserDetails userDetails = userDetailsService.loadUserByUsername("TESTUSER");
            
            // Assert
            assertNotNull(userDetails);
            assertEquals("TESTUSER", userDetails.getUsername());
            assertTrue(userDetails.isEnabled());
            
            // Verify user role
            assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
        }
        
        @Test
        @DisplayName("Should throw exception for non-existent username")
        void testLoadNonExistentUser() {
            // Act & Assert
            assertThrows(Exception.class, () -> 
                userDetailsService.loadUserByUsername("NONEXISTENT"));
        }
    }
    
    @Nested
    @DisplayName("Role-Based Access Control Tests")
    class RoleBasedAccessControlTests {
        
        @Test
        @WithMockUser(username = "TESTADM", roles = {"ADMIN"})
        @DisplayName("Should allow admin user to access admin endpoints")
        void testAdminAccessToAdminEndpoints() {
            // Act
            ResponseEntity<String> response = restTemplate.exchange(
                USERS_URL, HttpMethod.GET, null, String.class);
            
            // Assert - Admin should have access to user management endpoints
            assertNotEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
        
        @Test
        @WithMockUser(username = "TESTUSER", roles = {"USER"})
        @DisplayName("Should allow regular user to access user endpoints") 
        void testUserAccessToUserEndpoints() {
            // Act
            ResponseEntity<String> response = restTemplate.exchange(
                USERS_URL, HttpMethod.GET, null, String.class);
            
            // Assert - Regular users should have access to read operations
            assertNotEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
        
        @Test
        @WithMockUser(username = "TESTUSER", roles = {"USER"})
        @DisplayName("Should deny regular user access to admin operations")
        void testUserDeniedAdminOperations() {
            // Arrange
            String createUserPayload = "{\n" +
                "  \"userId\": \"NEWUSER\",\n" +
                "  \"firstName\": \"New\",\n" + 
                "  \"lastName\": \"User\",\n" +
                "  \"userType\": \"U\",\n" +
                "  \"password\": \"PASSWORD\"\n" +
                "}";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(createUserPayload, headers);
            
            // Act - Try to create user (admin operation)
            ResponseEntity<String> response = restTemplate.exchange(
                USERS_URL, HttpMethod.POST, entity, String.class);
            
            // Assert - Should be forbidden for regular user
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
        
        @Test
        @DisplayName("Should deny unauthenticated access to protected endpoints")
        void testUnauthenticatedAccess() {
            // Act
            ResponseEntity<String> response = restTemplate.exchange(
                USERS_URL, HttpMethod.GET, null, String.class);
            
            // Assert - Should require authentication
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }
    }
    
    @Nested
    @DisplayName("Method-Level Security Tests")  
    class MethodLevelSecurityTests {
        
        @Test
        @WithMockUser(username = "TESTADM", roles = {"ADMIN"})
        @DisplayName("Should enforce @PreAuthorize('hasRole('ADMIN')') for admin operations")
        void testPreAuthorizeAdminAnnotation() {
            // Arrange
            String updateUserPayload = "{\n" +
                "  \"firstName\": \"Updated\",\n" +
                "  \"lastName\": \"Name\"\n" +
                "}";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(updateUserPayload, headers);
            
            // Act - Try to update user (requires admin role)
            ResponseEntity<String> response = restTemplate.exchange(
                USERS_URL + "/TESTUSER", HttpMethod.PUT, entity, String.class);
            
            // Assert - Admin should have access
            assertNotEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
        
        @Test
        @WithMockUser(username = "TESTUSER", roles = {"USER"})
        @DisplayName("Should enforce @PreAuthorize('hasRole('USER')') for user operations")
        void testPreAuthorizeUserAnnotation() {
            // Act - Try to get user list (requires user role)
            ResponseEntity<String> response = restTemplate.exchange(
                USERS_URL, HttpMethod.GET, null, String.class);
            
            // Assert - User should have access to read operations
            assertNotEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
        
        @Test
        @WithMockUser(username = "TESTUSER", roles = {"USER"})
        @DisplayName("Should deny user role access to admin-only operations")
        void testPreAuthorizeAdminDeniedToUser() {
            // Act - Try to delete user (admin only operation)
            ResponseEntity<String> response = restTemplate.exchange(
                USERS_URL + "/TESTADM", HttpMethod.DELETE, null, String.class);
            
            // Assert - Should be forbidden for user role
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }
    
    @Nested
    @DisplayName("BCrypt Password Encoding Tests")
    class BCryptPasswordEncodingTests {
        
        @Test
        @DisplayName("Should encode passwords with BCrypt")
        void testBCryptPasswordEncoding() {
            // Arrange
            String plainPassword = "TEST123";
            
            // Act
            String encodedPassword = passwordEncoder.encode(plainPassword);
            
            // Assert
            assertNotNull(encodedPassword);
            assertNotEquals(plainPassword, encodedPassword);
            assertTrue(encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$"));
            assertTrue(passwordEncoder.matches(plainPassword, encodedPassword));
        }
        
        @Test
        @DisplayName("Should validate correct passwords using BCrypt")
        void testBCryptPasswordValidation() {
            // Arrange
            String plainPassword = "ADMIN123";
            String encodedPassword = testAdminUser.getPassword();
            
            // Act & Assert
            assertTrue(passwordEncoder.matches(plainPassword, encodedPassword));
            assertFalse(passwordEncoder.matches("WRONGPWD", encodedPassword));
        }
        
        @Test
        @DisplayName("Should handle password encoding for different user types")
        void testPasswordEncodingForDifferentUsers() {
            // Test admin user password
            assertTrue(passwordEncoder.matches("ADMIN123", testAdminUser.getPassword()));
            
            // Test regular user password  
            assertTrue(passwordEncoder.matches("USER123", testRegularUser.getPassword()));
            
            // Verify passwords are different when encoded
            assertNotEquals(testAdminUser.getPassword(), testRegularUser.getPassword());
        }
    }
    
    @Nested
    @DisplayName("Session Management Tests")
    class SessionManagementTests {
        
        @Test
        @DisplayName("Should create session on successful authentication")
        void testSessionCreationOnAuthentication() {
            // Arrange
            SignOnRequest request = new SignOnRequest();
            request.setUserId("TESTADM");
            request.setPassword("ADMIN123");
            
            // Act
            ResponseEntity<SignOnResponse> response = restTemplate.postForEntity(
                AUTH_SIGNIN_URL, request, SignOnResponse.class);
            
            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            
            SignOnResponse signOnResponse = response.getBody();
            assertNotNull(signOnResponse);
            assertNotNull(signOnResponse.getSessionToken());
            
            // Verify JWT token represents the session
            String token = signOnResponse.getSessionToken();
            assertTrue(jwtTokenService.validateToken(token));
        }
        
        @Test
        @DisplayName("Should invalidate session on logout")
        void testSessionInvalidationOnLogout() {
            // Arrange - First authenticate to create session
            SignOnRequest signInRequest = new SignOnRequest();
            signInRequest.setUserId("TESTADM");  
            signInRequest.setPassword("ADMIN123");
            
            ResponseEntity<SignOnResponse> signInResponse = restTemplate.postForEntity(
                AUTH_SIGNIN_URL, signInRequest, SignOnResponse.class);
            
            assertEquals(HttpStatus.OK, signInResponse.getStatusCode());
            String sessionToken = signInResponse.getBody().getSessionToken();
            assertNotNull(sessionToken);
            
            // Act - Sign out to invalidate session
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(sessionToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<SignOnResponse> signOutResponse = restTemplate.exchange(
                AUTH_SIGNOUT_URL, HttpMethod.POST, entity, SignOnResponse.class);
            
            // Assert
            assertEquals(HttpStatus.OK, signOutResponse.getStatusCode());
            assertEquals("SUCCESS", signOutResponse.getBody().getStatus());
        }
        
        @Test
        @DisplayName("Should validate session status correctly")
        void testSessionStatusValidation() {
            // Arrange - First authenticate
            SignOnRequest request = new SignOnRequest();
            request.setUserId("TESTUSER");
            request.setPassword("USER123");
            
            ResponseEntity<SignOnResponse> authResponse = restTemplate.postForEntity(
                AUTH_SIGNIN_URL, request, SignOnResponse.class);
            
            String sessionToken = authResponse.getBody().getSessionToken();
            
            // Act - Check authentication status
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(sessionToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<SignOnResponse> statusResponse = restTemplate.exchange(
                AUTH_STATUS_URL, HttpMethod.GET, entity, SignOnResponse.class);
            
            // Assert
            assertEquals(HttpStatus.OK, statusResponse.getStatusCode());
            SignOnResponse statusResult = statusResponse.getBody();
            assertNotNull(statusResult);
            assertEquals("TESTUSER", statusResult.getUserId());
        }
    }
    
    @Nested
    @DisplayName("Performance and Concurrent Authentication Tests")
    class PerformanceAndConcurrencyTests {
        
        @RepeatedTest(5)
        @DisplayName("Should complete authentication within 100ms performance requirement")
        void testAuthenticationPerformance() {
            // Arrange
            SignOnRequest request = new SignOnRequest();
            request.setUserId("TESTADM");
            request.setPassword("ADMIN123");
            
            // Act
            Instant start = Instant.now();
            ResponseEntity<SignOnResponse> response = restTemplate.postForEntity(
                AUTH_SIGNIN_URL, request, SignOnResponse.class);
            Instant end = Instant.now();
            
            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            
            long authenticationTimeMs = Duration.between(start, end).toMillis();
            assertTrue(authenticationTimeMs < 100, 
                "Authentication took " + authenticationTimeMs + "ms, requirement is < 100ms");
        }
        
        @Test
        @DisplayName("Should handle concurrent authentication attempts safely")
        void testConcurrentAuthentication() throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException {
            // Arrange
            int numberOfThreads = 10;
            ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
            CountDownLatch latch = new CountDownLatch(numberOfThreads);
            List<CompletableFuture<ResponseEntity<SignOnResponse>>> futures = new ArrayList<>();
            
            // Act - Submit concurrent authentication requests
            for (int i = 0; i < numberOfThreads; i++) {
                final String userId = (i % 2 == 0) ? "TESTADM" : "TESTUSER";
                final String password = (i % 2 == 0) ? "ADMIN123" : "USER123";
                
                CompletableFuture<ResponseEntity<SignOnResponse>> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        SignOnRequest request = new SignOnRequest();
                        request.setUserId(userId);
                        request.setPassword(password);
                        
                        return restTemplate.postForEntity(AUTH_SIGNIN_URL, request, SignOnResponse.class);
                    } finally {
                        latch.countDown();
                    }
                }, executorService);
                
                futures.add(future);
            }
            
            // Wait for all requests to complete
            assertTrue(latch.await(5, TimeUnit.SECONDS), "All authentication requests should complete within 5 seconds");
            
            // Assert - All authentication attempts should succeed
            for (CompletableFuture<ResponseEntity<SignOnResponse>> future : futures) {
                ResponseEntity<SignOnResponse> response = future.get(1, TimeUnit.SECONDS);
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals("SUCCESS", response.getBody().getStatus());
                assertNotNull(response.getBody().getSessionToken());
            }
            
            executorService.shutdown();
        }
        
        @Test
        @DisplayName("Should handle concurrent token validation efficiently")
        void testConcurrentTokenValidation() throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException {
            // Arrange - Generate test tokens
            String adminToken = jwtTokenService.generateToken(testAdminUser);
            String userToken = jwtTokenService.generateToken(testRegularUser);
            
            int numberOfValidations = 20;
            ExecutorService executorService = Executors.newFixedThreadPool(10);
            CountDownLatch latch = new CountDownLatch(numberOfValidations);
            List<CompletableFuture<Boolean>> futures = new ArrayList<>();
            
            // Act - Submit concurrent token validation requests
            for (int i = 0; i < numberOfValidations; i++) {
                final String token = (i % 2 == 0) ? adminToken : userToken;
                
                CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return jwtTokenService.validateToken(token);
                    } finally {
                        latch.countDown();
                    }
                }, executorService);
                
                futures.add(future);
            }
            
            // Wait for all validations to complete
            assertTrue(latch.await(3, TimeUnit.SECONDS), "All token validations should complete within 3 seconds");
            
            // Assert - All token validations should succeed
            for (CompletableFuture<Boolean> future : futures) {
                Boolean result = future.get(1, TimeUnit.SECONDS);
                assertTrue(result, "Token validation should succeed");
            }
            
            executorService.shutdown();
        }
    }
}