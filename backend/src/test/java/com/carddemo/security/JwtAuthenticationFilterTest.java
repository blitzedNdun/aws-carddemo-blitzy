package com.carddemo.security;

import com.carddemo.security.JwtAuthenticationFilter;
import com.carddemo.security.JwtTokenService;
import com.carddemo.security.CustomUserDetailsService;
import com.carddemo.security.SecurityTestUtils;
import com.carddemo.test.UnitTest;
import com.carddemo.test.AbstractBaseTest;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InjectMocks;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import javax.servlet.FilterChain;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.assertj.core.api.Assertions;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Unit test implementation for JwtAuthenticationFilter using JUnit 5, Mockito, and MockHttpServletRequest/Response.
 * Tests JWT token extraction from Authorization Bearer header, validates token verification process, tests security
 * context population with authenticated user, verifies filter chain continuation for valid tokens, tests rejection
 * of invalid/expired tokens, validates handling of missing Authorization header, and ensures stateless authentication
 * without session creation.
 * 
 * This comprehensive test suite ensures the JWT authentication filter correctly integrates with Spring Security
 * to provide stateless authentication for the CardDemo application while maintaining compatibility with the
 * original COBOL/CICS authentication patterns.
 * 
 * Key Test Scenarios:
 * - JWT token extraction from Authorization Bearer headers
 * - Token validation through JwtTokenService integration
 * - Security context population with authenticated UserDetails
 * - Filter chain continuation for valid and invalid tokens
 * - Proper handling of missing or malformed Authorization headers
 * - Stateless authentication without HTTP session creation
 * - Error handling and security context cleanup on failures
 * 
 * Testing Strategy:
 * - Uses Mockito for dependency mocking and behavior verification
 * - MockHttpServletRequest/Response for servlet testing
 * - AssertJ for fluent assertions and comprehensive validation
 * - SecurityTestUtils for consistent test data creation
 * - AbstractBaseTest for common testing infrastructure
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("JWT Authentication Filter Tests")
public class JwtAuthenticationFilterTest extends AbstractBaseTest implements UnitTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private UserDetails testUser;
    private UserDetails testAdmin;
    private String validJwtToken;
    private String invalidJwtToken;
    private String expiredJwtToken;

    /**
     * Setup method executed before each test execution.
     * Initializes mock HTTP servlet request/response objects, creates test user data,
     * and prepares JWT tokens for various testing scenarios.
     * 
     * This method extends AbstractBaseTest.setUp() to provide JWT-specific test setup
     * including mock authentication objects, test tokens, and security context preparation.
     */
    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        
        // Initialize mock HTTP servlet objects
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        
        // Create test user data using SecurityTestUtils
        testUser = SecurityTestUtils.createTestUser();
        testAdmin = SecurityTestUtils.createTestAdmin();
        
        // Generate test JWT tokens for various scenarios
        validJwtToken = SecurityTestUtils.generateTestJwtToken(testUser);
        invalidJwtToken = SecurityTestUtils.generateInvalidJwtToken(testUser);
        expiredJwtToken = SecurityTestUtils.generateExpiredJwtToken(testUser);
        
        // Clear security context before each test
        SecurityContextHolder.clearContext();
        
        logTestExecution("JWT Authentication Filter test setup completed", null);
    }

    /**
     * Teardown method executed after each test execution.
     * Performs cleanup operations specific to JWT authentication testing
     * including security context cleanup and mock object reset.
     */
    @AfterEach
    @Override
    public void tearDown() {
        // Clear security context after each test
        SecurityContextHolder.clearContext();
        
        // Reset mock objects
        resetMocks();
        
        super.tearDown();
        
        logTestExecution("JWT Authentication Filter test teardown completed", null);
    }

    /**
     * Reset mock objects to ensure test isolation.
     * Extends AbstractBaseTest.resetMocks() to include JWT-specific mock resets.
     */
    @Override
    protected void resetMocks() {
        super.resetMocks();
        
        // Reset JWT-specific mocks
        reset(jwtTokenService);
        reset(customUserDetailsService);
        reset(filterChain);
    }

    /**
     * Test JWT token extraction from Authorization Bearer header.
     * Verifies that the filter correctly extracts JWT tokens from properly formatted
     * Authorization headers and handles various header format scenarios.
     * 
     * Test scenarios:
     * - Valid Bearer token extraction
     * - Authorization header with Bearer prefix
     * - Token extraction without Bearer prefix (should fail)
     * - Authorization header case sensitivity
     * - Token trimming and whitespace handling
     */
    @Test
    @DisplayName("Extract JWT Token from Authorization Bearer Header")
    public void testExtractTokenFromAuthorizationHeader() throws ServletException, IOException {
        // Test Case 1: Valid Bearer token extraction
        String expectedToken = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + expectedToken);
        
        // Configure mock behavior for valid token
        when(jwtTokenService.validateToken(expectedToken)).thenReturn(true);
        when(jwtTokenService.isTokenBlacklisted(expectedToken)).thenReturn(false);
        when(jwtTokenService.extractUsername(expectedToken)).thenReturn("testuser");
        when(customUserDetailsService.loadUserByUsername("testuser")).thenReturn(testUser);
        
        // Execute filter
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Verify token validation was called with correct token
        verify(jwtTokenService).validateToken(expectedToken);
        verify(jwtTokenService).extractUsername(expectedToken);
        verify(customUserDetailsService).loadUserByUsername("testuser");
        verify(filterChain).doFilter(request, response);
        
        // Verify security context was set
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().isAuthenticated()).isTrue();
        
        logTestExecution("Valid Bearer token extraction test passed", null);
        
        // Test Case 2: Authorization header without Bearer prefix (should not authenticate)
        SecurityContextHolder.clearContext();
        MockHttpServletRequest noBearerRequest = new MockHttpServletRequest();
        noBearerRequest.addHeader("Authorization", "InvalidPrefix " + expectedToken);
        
        jwtAuthenticationFilter.doFilterInternal(noBearerRequest, response, filterChain);
        
        // Verify no authentication was set
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        
        logTestExecution("Non-Bearer token rejection test passed", null);
        
        // Test Case 3: Bearer token with extra whitespace (should handle properly)
        SecurityContextHolder.clearContext();
        MockHttpServletRequest whitespaceRequest = new MockHttpServletRequest();
        whitespaceRequest.addHeader("Authorization", "Bearer   " + expectedToken + "   ");
        
        // Reset mocks for whitespace test
        reset(jwtTokenService, customUserDetailsService);
        when(jwtTokenService.validateToken(expectedToken)).thenReturn(true);
        when(jwtTokenService.isTokenBlacklisted(expectedToken)).thenReturn(false);
        when(jwtTokenService.extractUsername(expectedToken)).thenReturn("testuser");
        when(customUserDetailsService.loadUserByUsername("testuser")).thenReturn(testUser);
        
        jwtAuthenticationFilter.doFilterInternal(whitespaceRequest, response, filterChain);
        
        // Verify whitespace was properly trimmed
        verify(jwtTokenService).validateToken(expectedToken);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        
        logTestExecution("Bearer token whitespace handling test passed", null);
    }

    /**
     * Test valid JWT token authentication flow.
     * Verifies that valid JWT tokens result in proper authentication setup
     * including security context population and filter chain continuation.
     * 
     * Test scenarios:
     * - Complete valid token authentication flow
     * - Security context population with UserDetails
     * - UsernamePasswordAuthenticationToken creation
     * - Proper authorities assignment from UserDetails
     * - Filter chain continuation after authentication
     */
    @Test
    @DisplayName("Valid JWT Token Authentication Flow")
    public void testValidTokenAuthentication() throws ServletException, IOException {
        // Setup valid token in Authorization header
        request.addHeader("Authorization", "Bearer " + validJwtToken);
        
        // Configure mock behavior for successful authentication
        when(jwtTokenService.validateToken(validJwtToken)).thenReturn(true);
        when(jwtTokenService.isTokenBlacklisted(validJwtToken)).thenReturn(false);
        when(jwtTokenService.extractUsername(validJwtToken)).thenReturn(testUser.getUsername());
        when(customUserDetailsService.loadUserByUsername(testUser.getUsername())).thenReturn(testUser);
        
        // Execute filter
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Verify all authentication steps were executed
        verify(jwtTokenService).validateToken(validJwtToken);
        verify(jwtTokenService).isTokenBlacklisted(validJwtToken);
        verify(jwtTokenService).extractUsername(validJwtToken);
        verify(customUserDetailsService).loadUserByUsername(testUser.getUsername());
        verify(filterChain).doFilter(request, response);
        
        // Verify security context was properly populated
        UsernamePasswordAuthenticationToken authentication = 
            (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isEqualTo(testUser);
        assertThat(authentication.getCredentials()).isNull(); // Should be null for stateless auth
        assertThat(authentication.getAuthorities()).isEqualTo(testUser.getAuthorities());
        
        logTestExecution("Valid token authentication test passed", null);
        
        // Test with admin user for role verification
        SecurityContextHolder.clearContext();
        reset(jwtTokenService, customUserDetailsService);
        
        String adminToken = SecurityTestUtils.generateTestJwtToken(testAdmin);
        MockHttpServletRequest adminRequest = new MockHttpServletRequest();
        adminRequest.addHeader("Authorization", "Bearer " + adminToken);
        
        when(jwtTokenService.validateToken(adminToken)).thenReturn(true);
        when(jwtTokenService.isTokenBlacklisted(adminToken)).thenReturn(false);
        when(jwtTokenService.extractUsername(adminToken)).thenReturn(testAdmin.getUsername());
        when(customUserDetailsService.loadUserByUsername(testAdmin.getUsername())).thenReturn(testAdmin);
        
        jwtAuthenticationFilter.doFilterInternal(adminRequest, response, filterChain);
        
        // Verify admin authentication
        UsernamePasswordAuthenticationToken adminAuth = 
            (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        
        assertThat(adminAuth).isNotNull();
        assertThat(adminAuth.getPrincipal()).isEqualTo(testAdmin);
        assertThat(adminAuth.getAuthorities()).containsAll(testAdmin.getAuthorities());
        
        logTestExecution("Admin token authentication test passed", null);
    }

    /**
     * Test handling of invalid JWT tokens.
     * Verifies that invalid, expired, or blacklisted tokens are properly rejected
     * and do not result in authentication or security context population.
     * 
     * Test scenarios:
     * - Invalid token signature rejection
     * - Expired token handling
     * - Blacklisted token rejection
     * - Malformed token handling
     * - Security context remains empty for invalid tokens
     * - Filter chain continues despite authentication failure
     */
    @Test
    @DisplayName("Invalid JWT Token Handling")
    public void testInvalidTokenHandling() throws ServletException, IOException {
        // Test Case 1: Invalid token signature
        request.addHeader("Authorization", "Bearer " + invalidJwtToken);
        
        when(jwtTokenService.validateToken(invalidJwtToken)).thenReturn(false);
        
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Verify token validation was attempted
        verify(jwtTokenService).validateToken(invalidJwtToken);
        
        // Verify no further authentication steps were performed
        verify(jwtTokenService, never()).extractUsername(anyString());
        verify(customUserDetailsService, never()).loadUserByUsername(anyString());
        
        // Verify security context remains empty
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        
        // Verify filter chain continues
        verify(filterChain).doFilter(request, response);
        
        logTestExecution("Invalid token signature handling test passed", null);
        
        // Test Case 2: Expired token handling
        SecurityContextHolder.clearContext();
        reset(jwtTokenService, customUserDetailsService, filterChain);
        
        MockHttpServletRequest expiredRequest = new MockHttpServletRequest();
        expiredRequest.addHeader("Authorization", "Bearer " + expiredJwtToken);
        
        when(jwtTokenService.validateToken(expiredJwtToken)).thenReturn(false);
        
        jwtAuthenticationFilter.doFilterInternal(expiredRequest, response, filterChain);
        
        verify(jwtTokenService).validateToken(expiredJwtToken);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(expiredRequest, response);
        
        logTestExecution("Expired token handling test passed", null);
        
        // Test Case 3: Blacklisted token handling
        SecurityContextHolder.clearContext();
        reset(jwtTokenService, customUserDetailsService, filterChain);
        
        MockHttpServletRequest blacklistedRequest = new MockHttpServletRequest();
        String blacklistedToken = "blacklisted.jwt.token";
        blacklistedRequest.addHeader("Authorization", "Bearer " + blacklistedToken);
        
        when(jwtTokenService.validateToken(blacklistedToken)).thenReturn(true);
        when(jwtTokenService.isTokenBlacklisted(blacklistedToken)).thenReturn(true);
        
        jwtAuthenticationFilter.doFilterInternal(blacklistedRequest, response, filterChain);
        
        verify(jwtTokenService).validateToken(blacklistedToken);
        verify(jwtTokenService).isTokenBlacklisted(blacklistedToken);
        verify(jwtTokenService, never()).extractUsername(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(blacklistedRequest, response);
        
        logTestExecution("Blacklisted token handling test passed", null);
        
        // Test Case 4: Token with invalid username
        SecurityContextHolder.clearContext();
        reset(jwtTokenService, customUserDetailsService, filterChain);
        
        MockHttpServletRequest invalidUserRequest = new MockHttpServletRequest();
        String tokenWithInvalidUser = "token.with.invalid.user";
        invalidUserRequest.addHeader("Authorization", "Bearer " + tokenWithInvalidUser);
        
        when(jwtTokenService.validateToken(tokenWithInvalidUser)).thenReturn(true);
        when(jwtTokenService.isTokenBlacklisted(tokenWithInvalidUser)).thenReturn(false);
        when(jwtTokenService.extractUsername(tokenWithInvalidUser)).thenReturn("");
        
        jwtAuthenticationFilter.doFilterInternal(invalidUserRequest, response, filterChain);
        
        verify(jwtTokenService).validateToken(tokenWithInvalidUser);
        verify(jwtTokenService).extractUsername(tokenWithInvalidUser);
        verify(customUserDetailsService, never()).loadUserByUsername(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(invalidUserRequest, response);
        
        logTestExecution("Invalid username in token handling test passed", null);
    }

    /**
     * Test handling of missing Authorization header.
     * Verifies that requests without Authorization headers are properly handled
     * without authentication and continue through the filter chain.
     * 
     * Test scenarios:
     * - Request with no Authorization header
     * - Request with empty Authorization header
     * - Request with null Authorization header
     * - Proper filter chain continuation
     * - Security context remains empty
     */
    @Test
    @DisplayName("Missing Authorization Header Handling")
    public void testMissingAuthorizationHeader() throws ServletException, IOException {
        // Test Case 1: No Authorization header at all
        // request already initialized without headers
        
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Verify no JWT service methods were called
        verify(jwtTokenService, never()).validateToken(anyString());
        verify(jwtTokenService, never()).extractUsername(anyString());
        verify(customUserDetailsService, never()).loadUserByUsername(anyString());
        
        // Verify security context remains empty
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        
        // Verify filter chain continues
        verify(filterChain).doFilter(request, response);
        
        logTestExecution("Missing Authorization header test passed", null);
        
        // Test Case 2: Empty Authorization header
        reset(filterChain);
        SecurityContextHolder.clearContext();
        
        MockHttpServletRequest emptyHeaderRequest = new MockHttpServletRequest();
        emptyHeaderRequest.addHeader("Authorization", "");
        
        jwtAuthenticationFilter.doFilterInternal(emptyHeaderRequest, response, filterChain);
        
        verify(jwtTokenService, never()).validateToken(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(emptyHeaderRequest, response);
        
        logTestExecution("Empty Authorization header test passed", null);
        
        // Test Case 3: Authorization header with only whitespace
        reset(filterChain);
        SecurityContextHolder.clearContext();
        
        MockHttpServletRequest whitespaceHeaderRequest = new MockHttpServletRequest();
        whitespaceHeaderRequest.addHeader("Authorization", "   ");
        
        jwtAuthenticationFilter.doFilterInternal(whitespaceHeaderRequest, response, filterChain);
        
        verify(jwtTokenService, never()).validateToken(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(whitespaceHeaderRequest, response);
        
        logTestExecution("Whitespace-only Authorization header test passed", null);
        
        // Test Case 4: Authorization header without Bearer prefix
        reset(filterChain);
        SecurityContextHolder.clearContext();
        
        MockHttpServletRequest noBearerRequest = new MockHttpServletRequest();
        noBearerRequest.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        
        jwtAuthenticationFilter.doFilterInternal(noBearerRequest, response, filterChain);
        
        verify(jwtTokenService, never()).validateToken(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(noBearerRequest, response);
        
        logTestExecution("Non-Bearer Authorization header test passed", null);
    }

    /**
     * Test filter chain continuation behavior.
     * Verifies that the filter properly continues the filter chain in all scenarios
     * regardless of authentication success or failure, maintaining stateless behavior.
     * 
     * Test scenarios:
     * - Filter chain continuation with valid authentication
     * - Filter chain continuation with invalid authentication
     * - Filter chain continuation with missing headers
     * - Filter chain continuation with exceptions
     * - No HTTP session creation (stateless authentication)
     */
    @Test
    @DisplayName("Filter Chain Continuation Behavior")
    public void testFilterChainContinuation() throws ServletException, IOException {
        // Test Case 1: Filter chain continuation with successful authentication
        request.addHeader("Authorization", "Bearer " + validJwtToken);
        
        when(jwtTokenService.validateToken(validJwtToken)).thenReturn(true);
        when(jwtTokenService.isTokenBlacklisted(validJwtToken)).thenReturn(false);
        when(jwtTokenService.extractUsername(validJwtToken)).thenReturn(testUser.getUsername());
        when(customUserDetailsService.loadUserByUsername(testUser.getUsername())).thenReturn(testUser);
        
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Verify filter chain was called exactly once
        verify(filterChain, times(1)).doFilter(request, response);
        
        logTestExecution("Filter chain continuation with authentication test passed", null);
        
        // Test Case 2: Filter chain continuation with failed authentication
        reset(filterChain);
        SecurityContextHolder.clearContext();
        
        MockHttpServletRequest failedAuthRequest = new MockHttpServletRequest();
        failedAuthRequest.addHeader("Authorization", "Bearer " + invalidJwtToken);
        
        when(jwtTokenService.validateToken(invalidJwtToken)).thenReturn(false);
        
        jwtAuthenticationFilter.doFilterInternal(failedAuthRequest, response, filterChain);
        
        verify(filterChain, times(1)).doFilter(failedAuthRequest, response);
        
        logTestExecution("Filter chain continuation with failed authentication test passed", null);
        
        // Test Case 3: Filter chain continuation with existing security context
        reset(filterChain);
        
        // Set up existing authentication in security context
        UsernamePasswordAuthenticationToken existingAuth = 
            SecurityTestUtils.createTestAuthentication(testUser);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);
        
        MockHttpServletRequest existingAuthRequest = new MockHttpServletRequest();
        existingAuthRequest.addHeader("Authorization", "Bearer " + validJwtToken);
        
        jwtAuthenticationFilter.doFilterInternal(existingAuthRequest, response, filterChain);
        
        // Verify JWT services were not called when authentication already exists
        verify(jwtTokenService, never()).validateToken(anyString());
        verify(filterChain, times(1)).doFilter(existingAuthRequest, response);
        
        // Verify existing authentication was preserved
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(existingAuth);
        
        logTestExecution("Filter chain continuation with existing authentication test passed", null);
        
        // Test Case 4: Stateless behavior verification (no session creation)
        SecurityContextHolder.clearContext();
        reset(filterChain);
        
        MockHttpServletRequest statelessRequest = new MockHttpServletRequest();
        statelessRequest.addHeader("Authorization", "Bearer " + validJwtToken);
        
        when(jwtTokenService.validateToken(validJwtToken)).thenReturn(true);
        when(jwtTokenService.isTokenBlacklisted(validJwtToken)).thenReturn(false);
        when(jwtTokenService.extractUsername(validJwtToken)).thenReturn(testUser.getUsername());
        when(customUserDetailsService.loadUserByUsername(testUser.getUsername())).thenReturn(testUser);
        
        jwtAuthenticationFilter.doFilterInternal(statelessRequest, response, filterChain);
        
        // Verify no session was created (stateless authentication)
        assertThat(statelessRequest.getSession(false)).isNull();
        
        // Verify authentication was set in SecurityContextHolder, not session
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        
        logTestExecution("Stateless authentication behavior test passed", null);
        
        // Test Case 5: Exception handling with filter chain continuation
        SecurityContextHolder.clearContext();
        reset(filterChain);
        
        MockHttpServletRequest exceptionRequest = new MockHttpServletRequest();
        exceptionRequest.addHeader("Authorization", "Bearer " + validJwtToken);
        
        // Simulate service exception
        when(jwtTokenService.validateToken(validJwtToken)).thenThrow(new RuntimeException("Service exception"));
        
        // Should not throw exception, but continue filter chain
        assertThatCode(() -> {
            jwtAuthenticationFilter.doFilterInternal(exceptionRequest, response, filterChain);
        }).doesNotThrowAnyException();
        
        // Verify filter chain still continued despite exception
        verify(filterChain, times(1)).doFilter(exceptionRequest, response);
        
        // Verify security context was cleared on exception
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        
        logTestExecution("Exception handling with filter chain continuation test passed", null);
    }

    /**
     * Test comprehensive authentication scenarios covering edge cases.
     * This method tests additional scenarios to ensure complete coverage
     * of the JWT authentication filter behavior.
     * 
     * Test scenarios:
     * - User not found in UserDetailsService
     * - Multiple Authentication headers
     * - Case-insensitive Bearer prefix handling
     * - Token extraction with various formats
     * - Security context cleanup on authentication failure
     */
    @Test
    @DisplayName("Comprehensive Authentication Scenarios")
    public void testComprehensiveAuthenticationScenarios() throws ServletException, IOException {
        // Test Case 1: User not found in UserDetailsService
        String userNotFoundToken = "user.not.found.token";
        request.addHeader("Authorization", "Bearer " + userNotFoundToken);
        
        when(jwtTokenService.validateToken(userNotFoundToken)).thenReturn(true);
        when(jwtTokenService.isTokenBlacklisted(userNotFoundToken)).thenReturn(false);
        when(jwtTokenService.extractUsername(userNotFoundToken)).thenReturn("nonexistentuser");
        when(customUserDetailsService.loadUserByUsername("nonexistentuser")).thenReturn(null);
        
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Verify authentication was attempted but failed
        verify(jwtTokenService).validateToken(userNotFoundToken);
        verify(customUserDetailsService).loadUserByUsername("nonexistentuser");
        
        // Verify no authentication was set
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        
        logTestExecution("User not found handling test passed", null);
        
        // Test Case 2: Case-insensitive Bearer prefix
        SecurityContextHolder.clearContext();
        reset(jwtTokenService, customUserDetailsService, filterChain);
        
        MockHttpServletRequest caseInsensitiveRequest = new MockHttpServletRequest();
        caseInsensitiveRequest.addHeader("Authorization", "bearer " + validJwtToken);
        
        // Configure mocks for valid authentication
        when(jwtTokenService.validateToken(validJwtToken)).thenReturn(true);
        when(jwtTokenService.isTokenBlacklisted(validJwtToken)).thenReturn(false);
        when(jwtTokenService.extractUsername(validJwtToken)).thenReturn(testUser.getUsername());
        when(customUserDetailsService.loadUserByUsername(testUser.getUsername())).thenReturn(testUser);
        
        jwtAuthenticationFilter.doFilterInternal(caseInsensitiveRequest, response, filterChain);
        
        // Verify case-insensitive Bearer prefix was handled
        verify(jwtTokenService).validateToken(validJwtToken);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        
        logTestExecution("Case-insensitive Bearer prefix test passed", null);
        
        // Test Case 3: Bearer prefix with different casing
        SecurityContextHolder.clearContext();
        reset(jwtTokenService, customUserDetailsService, filterChain);
        
        MockHttpServletRequest mixedCaseRequest = new MockHttpServletRequest();
        mixedCaseRequest.addHeader("Authorization", "BeArEr " + validJwtToken);
        
        when(jwtTokenService.validateToken(validJwtToken)).thenReturn(true);
        when(jwtTokenService.isTokenBlacklisted(validJwtToken)).thenReturn(false);
        when(jwtTokenService.extractUsername(validJwtToken)).thenReturn(testUser.getUsername());
        when(customUserDetailsService.loadUserByUsername(testUser.getUsername())).thenReturn(testUser);
        
        jwtAuthenticationFilter.doFilterInternal(mixedCaseRequest, response, filterChain);
        
        verify(jwtTokenService).validateToken(validJwtToken);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        
        logTestExecution("Mixed case Bearer prefix test passed", null);
        
        // Test Case 4: Empty token after Bearer prefix
        SecurityContextHolder.clearContext();
        reset(jwtTokenService, customUserDetailsService, filterChain);
        
        MockHttpServletRequest emptyTokenRequest = new MockHttpServletRequest();
        emptyTokenRequest.addHeader("Authorization", "Bearer ");
        
        jwtAuthenticationFilter.doFilterInternal(emptyTokenRequest, response, filterChain);
        
        // Verify no JWT service calls were made for empty token
        verify(jwtTokenService, never()).validateToken(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(emptyTokenRequest, response);
        
        logTestExecution("Empty token after Bearer prefix test passed", null);
        
        // Test Case 5: Token validation throws exception
        SecurityContextHolder.clearContext();
        reset(jwtTokenService, customUserDetailsService, filterChain);
        
        MockHttpServletRequest exceptionRequest = new MockHttpServletRequest();
        String exceptionToken = "exception.token";
        exceptionRequest.addHeader("Authorization", "Bearer " + exceptionToken);
        
        when(jwtTokenService.validateToken(exceptionToken))
            .thenThrow(new RuntimeException("Token validation failed"));
        
        // Should handle exception gracefully
        assertThatCode(() -> {
            jwtAuthenticationFilter.doFilterInternal(exceptionRequest, response, filterChain);
        }).doesNotThrowAnyException();
        
        // Verify security context was cleared after exception
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(exceptionRequest, response);
        
        logTestExecution("Token validation exception handling test passed", null);
    }
}