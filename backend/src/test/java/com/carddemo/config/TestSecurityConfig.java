package com.carddemo.config;

import com.carddemo.entity.UserSecurity;
import com.carddemo.repository.UserSecurityRepository;
import com.carddemo.service.SignOnService;
import com.carddemo.dto.SignOnResponse;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Spring Security test configuration providing mock authentication providers, test user details services,
 * and security context setup for testing authentication and authorization flows.
 * 
 * This configuration replaces production Spring Security beans with test-specific implementations
 * that provide controlled, predictable behavior for unit and integration testing. It creates
 * mock users with ROLE_ADMIN and ROLE_USER authorities matching SEC-USR-TYPE values from the
 * original COBOL USRSEC dataset structure.
 * 
 * Key Testing Features:
 * - Mock UserDetailsService with predefined test users (admin/user roles)
 * - NoOpPasswordEncoder matching production plain-text password handling from COSGN00C.cbl
 * - Mock UserSecurityRepository for testing authentication flows
 * - Mock SignOnService for testing security integration
 * - SecurityMockMvcConfigurer for testing secured REST endpoints
 * 
 * Test Users Provided:
 * - TESTADM1 (password: TESTPASS, role: ROLE_ADMIN) - Admin user for testing administrative functions
 * - TESTUSER1 (password: TESTPASS, role: ROLE_USER) - Regular user for testing standard operations
 * - TESTUSER2 (password: TESTPASS, role: ROLE_USER) - Additional user for multi-user scenarios
 * 
 * Security Configuration:
 * - Plain-text password encoding (NoOpPasswordEncoder) matching COBOL authentication
 * - Spring Security context setup for MockMvc testing
 * - Mock authentication providers for controlled test scenarios
 * - Test-specific security event handling and audit logging
 */
@TestConfiguration
public class TestSecurityConfig {

    /**
     * Provides a test UserDetailsService with predefined users for authentication testing.
     * Creates test users with ROLE_ADMIN and ROLE_USER authorities matching the SEC-USR-TYPE
     * values from the original COBOL implementation ('A' for admin, 'U' for user).
     * 
     * Test users are configured with plain-text passwords using NoOpPasswordEncoder
     * to match the production authentication behavior from COSGN00C.cbl program.
     * 
     * @return UserDetailsService with mock users for testing authentication flows
     */
    @Bean
    @Primary
    public UserDetailsService testUserDetailsService() {
        return username -> {
            switch (username.toUpperCase()) {
                case "TESTADM1":
                    return User.withUsername("TESTADM1")
                            .password("TESTPASS")
                            .roles("ADMIN")
                            .build();
                case "TESTUSER1":
                    return User.withUsername("TESTUSER1")
                            .password("TESTPASS")
                            .roles("USER")
                            .build();
                case "TESTUSER2":
                    return User.withUsername("TESTUSER2")
                            .password("TESTPASS")
                            .roles("USER")
                            .build();
                default:
                    throw new UsernameNotFoundException("Test user not found: " + username);
            }
        };
    }

    /**
     * Provides NoOpPasswordEncoder for test environment matching production plain-text password handling.
     * This encoder performs no password encoding, storing passwords in plain text to match
     * the COBOL authentication logic from COSGN00C.cbl that performs direct string comparison.
     * 
     * @return PasswordEncoder that performs no encoding for test compatibility
     */
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    /**
     * Creates a mock UserSecurityRepository for testing authentication flows.
     * Provides controlled behavior for repository methods used during authentication
     * and user management operations, enabling predictable test scenarios.
     * 
     * The mock repository is configured with test users that match the UserSecurity
     * entity structure and provide realistic authentication test data.
     * 
     * @return Mock UserSecurityRepository with predefined test behavior
     */
    @Bean
    @Primary
    public UserSecurityRepository mockUserSecurityRepository() {
        UserSecurityRepository mockRepository = Mockito.mock(UserSecurityRepository.class);
        
        // Create test admin user matching COBOL CSUSR01Y structure
        UserSecurity testAdmin = new UserSecurity();
        testAdmin.setSecUsrId("TESTADM1");
        testAdmin.setUsername("TESTADM1");
        testAdmin.setPassword("TESTPASS");
        testAdmin.setUserType("A"); // Admin type matching SEC-USR-TYPE
        testAdmin.setEnabled(true);
        testAdmin.setFirstName("Test");
        testAdmin.setLastName("Admin");
        
        // Create test regular user matching COBOL CSUSR01Y structure
        UserSecurity testUser1 = new UserSecurity();
        testUser1.setSecUsrId("TESTUSER1");
        testUser1.setUsername("TESTUSER1");
        testUser1.setPassword("TESTPASS");
        testUser1.setUserType("U"); // User type matching SEC-USR-TYPE
        testUser1.setEnabled(true);
        testUser1.setFirstName("Test");
        testUser1.setLastName("User1");
        
        // Create second test user for multi-user scenarios
        UserSecurity testUser2 = new UserSecurity();
        testUser2.setSecUsrId("TESTUSER2");
        testUser2.setUsername("TESTUSER2");
        testUser2.setPassword("TESTPASS");
        testUser2.setUserType("U"); // User type matching SEC-USR-TYPE
        testUser2.setEnabled(true);
        testUser2.setFirstName("Test");
        testUser2.setLastName("User2");
        
        // Configure mock behavior for authentication operations
        when(mockRepository.findByUsername("TESTADM1")).thenReturn(Optional.of(testAdmin));
        when(mockRepository.findByUsername("TESTUSER1")).thenReturn(Optional.of(testUser1));
        when(mockRepository.findByUsername("TESTUSER2")).thenReturn(Optional.of(testUser2));
        when(mockRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        
        // Configure mock behavior for COBOL-style user ID lookup
        when(mockRepository.findBySecUsrId("TESTADM1")).thenReturn(Optional.of(testAdmin));
        when(mockRepository.findBySecUsrId("TESTUSER1")).thenReturn(Optional.of(testUser1));
        when(mockRepository.findBySecUsrId("TESTUSER2")).thenReturn(Optional.of(testUser2));
        when(mockRepository.findBySecUsrId(anyString())).thenReturn(Optional.empty());
        
        // Configure existence checks for user validation
        when(mockRepository.existsByUsername("TESTADM1")).thenReturn(true);
        when(mockRepository.existsByUsername("TESTUSER1")).thenReturn(true);
        when(mockRepository.existsByUsername("TESTUSER2")).thenReturn(true);
        when(mockRepository.existsByUsername(anyString())).thenReturn(false);
        
        when(mockRepository.existsBySecUsrId("TESTADM1")).thenReturn(true);
        when(mockRepository.existsBySecUsrId("TESTUSER1")).thenReturn(true);
        when(mockRepository.existsBySecUsrId("TESTUSER2")).thenReturn(true);
        when(mockRepository.existsBySecUsrId(anyString())).thenReturn(false);
        
        // Configure save operations for user management testing
        when(mockRepository.save(any(UserSecurity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        return mockRepository;
    }

    /**
     * Creates a mock SignOnService for testing security integration scenarios.
     * Provides controlled behavior for authentication flows that depend on SignOnService
     * for user validation and session management operations.
     * 
     * The mock service simulates successful authentication for test users and
     * provides realistic responses for testing REST controller security integration.
     * 
     * @return Mock SignOnService with predefined test behavior
     */
    @Bean
    @Primary
    public SignOnService mockSignOnService() {
        SignOnService mockService = Mockito.mock(SignOnService.class);
        
        // Configure mock behavior for test users using actual method signatures
        // Mock validateCredentials method that returns SignOnResponse
        com.carddemo.dto.SignOnResponse successAdminResponse = new com.carddemo.dto.SignOnResponse();
        successAdminResponse.setSuccess(true);
        successAdminResponse.setUserType("A");
        successAdminResponse.setUserName("Test Admin");
        
        com.carddemo.dto.SignOnResponse successUserResponse = new com.carddemo.dto.SignOnResponse();
        successUserResponse.setSuccess(true);
        successUserResponse.setUserType("U");
        successUserResponse.setUserName("Test User");
        
        com.carddemo.dto.SignOnResponse failureResponse = new com.carddemo.dto.SignOnResponse();
        failureResponse.setSuccess(false);
        failureResponse.setMessage("Authentication failed");
        
        when(mockService.validateCredentials("TESTADM1", "TESTPASS")).thenReturn(successAdminResponse);
        when(mockService.validateCredentials("TESTUSER1", "TESTPASS")).thenReturn(successUserResponse);
        when(mockService.validateCredentials("TESTUSER2", "TESTPASS")).thenReturn(successUserResponse);
        when(mockService.validateCredentials(anyString(), anyString())).thenReturn(failureResponse);
        
        // Create test user objects for getUserDetails method
        UserSecurity testAdminForSignOn = new UserSecurity();
        testAdminForSignOn.setSecUsrId("TESTADM1");
        testAdminForSignOn.setUsername("TESTADM1");
        testAdminForSignOn.setPassword("TESTPASS");
        testAdminForSignOn.setUserType("A");
        testAdminForSignOn.setEnabled(true);
        
        UserSecurity testUser1ForSignOn = new UserSecurity();
        testUser1ForSignOn.setSecUsrId("TESTUSER1");
        testUser1ForSignOn.setUsername("TESTUSER1");
        testUser1ForSignOn.setPassword("TESTPASS");
        testUser1ForSignOn.setUserType("U");
        testUser1ForSignOn.setEnabled(true);
        
        UserSecurity testUser2ForSignOn = new UserSecurity();
        testUser2ForSignOn.setSecUsrId("TESTUSER2");
        testUser2ForSignOn.setUsername("TESTUSER2");
        testUser2ForSignOn.setPassword("TESTPASS");
        testUser2ForSignOn.setUserType("U");
        testUser2ForSignOn.setEnabled(true);
        
        // Mock getUserDetails method
        when(mockService.getUserDetails("TESTADM1")).thenReturn(testAdminForSignOn);
        when(mockService.getUserDetails("TESTUSER1")).thenReturn(testUser1ForSignOn);
        when(mockService.getUserDetails("TESTUSER2")).thenReturn(testUser2ForSignOn);
        when(mockService.getUserDetails(anyString())).thenReturn(null);
        
        // Mock logout method
        when(mockService.logout("TESTADM1")).thenReturn(true);
        when(mockService.logout("TESTUSER1")).thenReturn(true);
        when(mockService.logout("TESTUSER2")).thenReturn(true);
        when(mockService.logout(anyString())).thenReturn(false);
        
        return mockService;
    }

    /**
     * Provides SecurityMockMvcConfigurer for testing secured REST endpoints.
     * Enables MockMvc testing with Spring Security context, allowing integration tests
     * to verify authentication and authorization behavior of REST controllers.
     * 
     * This configurer integrates with @WithMockUser annotations and provides
     * security context setup for comprehensive endpoint testing scenarios.
     * 
     * @return SecurityMockMvcConfigurer for MockMvc security testing
     */
    @Bean
    public org.springframework.test.web.servlet.setup.MockMvcConfigurer securityMockMvcConfigurer() {
        return SecurityMockMvcConfigurers.springSecurity();
    }
}