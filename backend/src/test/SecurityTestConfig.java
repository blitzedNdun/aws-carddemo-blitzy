/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.test;

import com.carddemo.entity.UserSecurity;
import com.carddemo.security.CustomAuthenticationProvider;
import com.carddemo.security.CustomUserDetailsService;
import com.carddemo.security.JwtTokenService;
import com.carddemo.security.SecurityConstants;
import com.carddemo.security.SessionAttributes;
import com.carddemo.test.TestConstants;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Security Test Configuration providing mock authentication providers, test user details services,
 * and security context setup for testing authentication and authorization flows that replace RACF functionality.
 * 
 * This configuration enables comprehensive testing of the COBOL-to-Spring Security migration by providing:
 * - Mock user details service with predefined test users
 * - Test authentication providers replicating COSGN00C COBOL logic
 * - JWT token generation for API endpoint testing
 * - Security context management for role-based authorization testing
 * - Session state setup matching CICS COMMAREA behavior
 */
@TestConfiguration
public class SecurityTestConfig {
    
    /**
     * Creates a mock UserDetailsService for testing authentication flows without database dependencies.
     * Provides predefined test users with different roles to validate authorization logic.
     * 
     * @return UserDetailsService configured with test users matching COBOL user types
     */
    @Bean
    @Primary
    public UserDetailsService mockUserDetailsService() {
        UserDetailsService mockService = Mockito.mock(UserDetailsService.class);
        
        // Create test user (regular user type 'U' from COBOL)
        UserDetails testUser = User.withUsername(TestConstants.TEST_USER_ID)
                .password(TestConstants.TEST_USER_PASSWORD)
                .authorities(new SimpleGrantedAuthority(SecurityConstants.ROLE_USER))
                .build();
        
        // Create test admin user (admin user type 'A' from COBOL)
        UserDetails testAdmin = User.withUsername("ADMIN01")
                .password("ADMIN123")
                .authorities(new SimpleGrantedAuthority(SecurityConstants.ROLE_ADMIN))
                .build();
        
        // Configure mock behaviors
        Mockito.when(mockService.loadUserByUsername(TestConstants.TEST_USER_ID))
               .thenReturn(testUser);
        Mockito.when(mockService.loadUserByUsername("ADMIN01"))
               .thenReturn(testAdmin);
        Mockito.when(mockService.loadUserByUsername("INVALID"))
               .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));
        
        return mockService;
    }
    
    /**
     * Creates a test authentication provider that replicates COSGN00C COBOL authentication logic.
     * Performs case-insensitive username matching and plain-text password comparison as per legacy system.
     * 
     * @return CustomAuthenticationProvider configured for testing scenarios
     */
    @Bean
    @Primary 
    public CustomAuthenticationProvider testAuthenticationProvider() {
        CustomAuthenticationProvider mockProvider = Mockito.mock(CustomAuthenticationProvider.class);
        
        // Mock successful authentication for test user
        Authentication successAuth = new UsernamePasswordAuthenticationToken(
            TestConstants.TEST_USER_ID,
            TestConstants.TEST_USER_PASSWORD,
            Arrays.asList(new SimpleGrantedAuthority(SecurityConstants.ROLE_USER))
        );
        
        // Mock successful authentication for admin user
        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
            "ADMIN01",
            "ADMIN123", 
            Arrays.asList(new SimpleGrantedAuthority(SecurityConstants.ROLE_ADMIN))
        );
        
        Mockito.when(mockProvider.authenticate(Mockito.any(Authentication.class)))
               .thenAnswer(invocation -> {
                   Authentication auth = invocation.getArgument(0);
                   String username = auth.getName().toUpperCase();
                   String password = auth.getCredentials().toString().toUpperCase();
                   
                   if (TestConstants.TEST_USER_ID.equals(username) && 
                       TestConstants.TEST_USER_PASSWORD.equals(password)) {
                       return successAuth;
                   } else if ("ADMIN01".equals(username) && "ADMIN123".equals(password)) {
                       return adminAuth;
                   } else {
                       throw new org.springframework.security.authentication.BadCredentialsException("Invalid credentials");
                   }
               });
        
        return mockProvider;
    }
    
    /**
     * Creates a test JWT token service for API testing scenarios.
     * Generates valid tokens for authenticated test scenarios without Redis dependencies.
     * 
     * @return JwtTokenService configured for testing
     */
    @Bean
    @Primary
    public JwtTokenService testJwtTokenService() {
        JwtTokenService mockService = Mockito.mock(JwtTokenService.class);
        
        String testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJURVNUVVNSIiwiaWF0IjoxNjAwMDAwMDAwLCJleHAiOjE2MDAwMDM2MDB9.test";
        
        Mockito.when(mockService.generateToken(TestConstants.TEST_USER_ID))
               .thenReturn(testToken);
        Mockito.when(mockService.validateToken(testToken))
               .thenReturn(true);
        Mockito.when(mockService.extractUsername(testToken))
               .thenReturn(TestConstants.TEST_USER_ID);
        
        return mockService;
    }
    
    /**
     * Creates a test password encoder that performs plain-text comparison to match COBOL legacy behavior.
     * Used exclusively in testing environments to validate authentication logic.
     * 
     * @return PasswordEncoder that performs plain-text encoding
     */
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return rawPassword.toString().toUpperCase();
            }
            
            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return rawPassword.toString().toUpperCase().equals(encodedPassword);
            }
        };
    }
    
    /**
     * Creates a test user object with predefined credentials for testing authentication flows.
     * Replicates the user record structure from VSAM USRSEC dataset.
     * 
     * @return UserSecurity entity configured for testing
     */
    public UserSecurity createTestUser() {
        UserSecurity testUser = new UserSecurity();
        testUser.setUserId(TestConstants.TEST_USER_ID);
        testUser.setPassword(TestConstants.TEST_USER_PASSWORD);
        testUser.setUserType("U"); // Regular user type from COBOL
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setStatus("A"); // Active status
        return testUser;
    }
    
    /**
     * Creates a test admin user object with admin credentials for testing administrative functions.
     * 
     * @return UserSecurity entity configured as admin for testing
     */
    public UserSecurity createTestAdminUser() {
        UserSecurity adminUser = new UserSecurity();
        adminUser.setUserId("ADMIN01");
        adminUser.setPassword("ADMIN123");
        adminUser.setUserType("A"); // Admin user type from COBOL
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setStatus("A"); // Active status
        return adminUser;
    }
    
    /**
     * Creates a test Authentication object for security context testing.
     * 
     * @param username The username for the authentication
     * @param authorities The granted authorities
     * @return Authentication object for testing
     */
    public Authentication createTestAuthentication(String username, Collection<SimpleGrantedAuthority> authorities) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            username, 
            "password", 
            authorities
        );
        auth.setAuthenticated(true);
        return auth;
    }
    
    /**
     * Creates a mock security context with authenticated user for testing authorization logic.
     * 
     * @param authentication The authentication to set in the context
     * @return SecurityContext configured for testing
     */
    public SecurityContext createMockSecurityContext(Authentication authentication) {
        SecurityContext context = Mockito.mock(SecurityContext.class);
        Mockito.when(context.getAuthentication()).thenReturn(authentication);
        return context;
    }
    
    /**
     * Generates a test JWT token for API endpoint testing scenarios.
     * 
     * @param username The username to generate token for
     * @return JWT token string for testing
     */
    public String generateTestJwtToken(String username) {
        // Simple test token for testing purposes
        return "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
               Base64.getEncoder().encodeToString(
                   ("{\"sub\":\"" + username + "\",\"iat\":1600000000,\"exp\":1600003600}").getBytes()
               ) + ".test-signature";
    }
    
    /**
     * Sets up test session attributes matching CICS COMMAREA functionality.
     * Creates session state that replicates the legacy mainframe session management.
     * 
     * @param userId The user ID to set in session
     * @param userType The user type ('A' for admin, 'U' for regular user)
     * @return Map of session attributes for testing
     */
    public Map<String, Object> setupTestSession(String userId, String userType) {
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put(SessionAttributes.SEC_USR_ID, userId);
        sessionAttributes.put(SessionAttributes.SEC_USR_TYPE, userType);
        sessionAttributes.put(SessionAttributes.NAVIGATION_STATE, "MAIN_MENU");
        sessionAttributes.put("LAST_ACCESS_TIME", LocalDateTime.now());
        sessionAttributes.put("SESSION_TIMEOUT", TestConstants.SESSION_TIMEOUT_MINUTES);
        return sessionAttributes;
    }
}

/**
 * Builder class for creating test Authentication objects with various configurations.
 * Provides fluent API for building authentication scenarios matching different user roles and states.
 */
class TestAuthenticationBuilder {
    private String username = TestConstants.TEST_USER_ID;
    private String password = TestConstants.TEST_USER_PASSWORD;
    private List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    
    /**
     * Sets the username for the authentication being built.
     * 
     * @param username The username to set
     * @return this builder for method chaining
     */
    public TestAuthenticationBuilder withUsername(String username) {
        this.username = username;
        return this;
    }
    
    /**
     * Sets the password for the authentication being built.
     * 
     * @param password The password to set
     * @return this builder for method chaining
     */
    public TestAuthenticationBuilder withPassword(String password) {
        this.password = password;
        return this;
    }
    
    /**
     * Adds a role to the authentication being built.
     * Automatically prefixes with "ROLE_" if not present.
     * 
     * @param role The role to add
     * @return this builder for method chaining
     */
    public TestAuthenticationBuilder withRole(String role) {
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        this.authorities.add(new SimpleGrantedAuthority(roleWithPrefix));
        return this;
    }
    
    /**
     * Adds a specific authority to the authentication being built.
     * 
     * @param authority The authority string to add
     * @return this builder for method chaining
     */
    public TestAuthenticationBuilder withAuthority(String authority) {
        this.authorities.add(new SimpleGrantedAuthority(authority));
        return this;
    }
    
    /**
     * Builds the Authentication object with the configured parameters.
     * 
     * @return Configured UsernamePasswordAuthenticationToken
     */
    public UsernamePasswordAuthenticationToken build() {
        // Default to USER role if no authorities specified
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority(SecurityConstants.ROLE_USER));
        }
        
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            username, password, authorities);
        auth.setAuthenticated(true);
        return auth;
    }
    
    /**
     * Creates and returns the Authentication object.
     * Alias for build() method for backwards compatibility.
     * 
     * @return Configured Authentication object
     */
    public Authentication createAuthentication() {
        return build();
    }
}

/**
 * Manager class for handling security context operations in tests.
 * Provides convenient methods for setting up and managing security contexts that replicate
 * the CICS security model during testing scenarios.
 */
class TestSecurityContextManager {
    
    /**
     * Sets up security context with a regular user authentication.
     * Replicates COBOL user type 'U' authentication state.
     */
    public void setupUserContext() {
        Authentication auth = new TestAuthenticationBuilder()
            .withUsername(TestConstants.TEST_USER_ID)
            .withRole(TestConstants.TEST_USER_ROLE)
            .build();
        
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }
    
    /**
     * Sets up security context with admin user authentication.
     * Replicates COBOL user type 'A' authentication state.
     */
    public void setupAdminContext() {
        Authentication auth = new TestAuthenticationBuilder()
            .withUsername("ADMIN01")
            .withRole(TestConstants.TEST_ADMIN_ROLE)
            .build();
        
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }
    
    /**
     * Sets up security context with no authentication (anonymous user).
     * Used for testing unauthorized access scenarios.
     */
    public void setupUnauthenticatedContext() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(null);
        SecurityContextHolder.setContext(context);
    }
    
    /**
     * Clears the current security context.
     * Used for cleanup between test methods.
     */
    public void clearContext() {
        SecurityContextHolder.clearContext();
    }
    
    /**
     * Gets the current authenticated user's username.
     * 
     * @return Username of current authenticated user, or null if not authenticated
     */
    public String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
    
    /**
     * Checks if the current user has the specified role.
     * 
     * @param role The role to check for (without ROLE_ prefix)
     * @return true if user has the role, false otherwise
     */
    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth.getAuthorities().stream()
                   .anyMatch(authority -> authority.getAuthority().equals(roleWithPrefix));
    }
    
    /**
     * Checks if there is currently an authenticated user in the security context.
     * 
     * @return true if user is authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName());
    }
}