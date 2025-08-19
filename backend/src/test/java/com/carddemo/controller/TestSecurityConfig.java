package com.carddemo.controller;

import com.carddemo.service.SignOnService;
import com.carddemo.entity.UserSecurity;
import com.carddemo.security.JwtAuthenticationFilter;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Test security configuration class that sets up Spring Security for controller tests.
 * 
 * This configuration provides a comprehensive test environment for Spring Boot controller tests
 * by creating mock security beans and test users with different roles. It configures authentication
 * for test scenarios while maintaining compatibility with the production security configuration.
 * 
 * Key Features:
 * - Provides test users with ADMIN and USER roles for comprehensive authorization testing
 * - Configures JWT token generation utilities for API authentication testing
 * - Sets up in-memory UserDetailsService with predefined test credentials
 * - Configures BCrypt password encoder for secure test credential handling
 * - Provides mock authentication providers for isolated unit testing
 * - Configures session management for COMMAREA-equivalent testing scenarios
 * - Sets up CSRF token handling for form submission testing
 * - Enables Spring Security test integration with MockMvc framework
 * 
 * Test Users Provided:
 * - ADMIN User: "TESTADM" with ROLE_ADMIN authority for administrative function testing
 * - Regular User: "TESTUSER" with ROLE_USER authority for standard user function testing
 * 
 * Usage in Controller Tests:
 * ```java
 * @SpringBootTest
 * @Import(TestSecurityConfig.class)
 * @AutoConfigureTestDatabase
 * class UserControllerTest {
 *     // Test methods using pre-configured security context
 * }
 * ```
 * 
 * JWT Token Generation:
 * The configuration provides utility methods for generating valid JWT tokens for API testing,
 * enabling comprehensive testing of secured endpoints with proper authentication context.
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    private static final String TEST_SECRET = "testSecretKeyForJwtTokenGenerationInTestEnvironment";
    private static final long TEST_TOKEN_VALIDITY = 3600000; // 1 hour in milliseconds
    
    // Test user credentials
    private static final String ADMIN_USER_ID = "TESTADM";
    private static final String ADMIN_USERNAME = "testadmin";
    private static final String ADMIN_PASSWORD = "adminpass";
    private static final String ADMIN_FIRST_NAME = "Test";
    private static final String ADMIN_LAST_NAME = "Administrator";
    private static final String ADMIN_USER_TYPE = "A";
    
    private static final String REGULAR_USER_ID = "TESTUSER";
    private static final String REGULAR_USERNAME = "testuser";
    private static final String REGULAR_PASSWORD = "userpass";
    private static final String REGULAR_FIRST_NAME = "Test";
    private static final String REGULAR_LAST_NAME = "User";
    private static final String REGULAR_USER_TYPE = "U";

    /**
     * Provides a test-specific UserDetailsService with predefined test users.
     * 
     * This bean creates an in-memory UserDetailsService containing test users with
     * different roles for comprehensive authorization testing. The service provides
     * both administrative and regular user accounts with properly configured authorities
     * matching the production user role mapping.
     * 
     * Test Users:
     * - Admin User: TESTADM with ROLE_ADMIN authority
     * - Regular User: TESTUSER with ROLE_USER authority
     * 
     * @return UserDetailsService configured with test users for controller testing
     */
    @Bean
    @Primary
    public UserDetailsService testUserDetailsService() {
        return new UserDetailsService() {
            private final Map<String, UserSecurity> testUsers = createTestUsers();
            
            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                UserSecurity user = testUsers.get(username.toUpperCase());
                if (user == null) {
                    throw new UsernameNotFoundException("Test user not found: " + username);
                }
                return user;
            }
            
            private Map<String, UserSecurity> createTestUsers() {
                Map<String, UserSecurity> users = new HashMap<>();
                
                // Create test admin user
                UserSecurity adminUser = createAdminUserInternal();
                users.put(adminUser.getUsername().toUpperCase(), adminUser);
                users.put(adminUser.getSecUsrId().toUpperCase(), adminUser);
                
                // Create test regular user
                UserSecurity regularUser = createRegularUserInternal();
                users.put(regularUser.getUsername().toUpperCase(), regularUser);
                users.put(regularUser.getSecUsrId().toUpperCase(), regularUser);
                
                return users;
            }
        };
    }

    /**
     * Provides a test-specific BCrypt password encoder for secure credential handling.
     * 
     * This bean provides the same BCrypt password encoder used in production for
     * encoding test user passwords, ensuring consistency between test and production
     * environments while maintaining security best practices in test scenarios.
     * 
     * @return BCryptPasswordEncoder for test credential encoding and validation
     */
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Generates a test JWT token for API authentication testing.
     * 
     * This method creates valid JWT tokens for test users, enabling comprehensive
     * testing of secured API endpoints with proper authentication context. The tokens
     * include user identity, roles, and standard JWT claims for complete authentication
     * simulation in test environments.
     * 
     * @param username Username for token generation
     * @param userType User type ('A' for admin, 'U' for regular user)
     * @return Valid JWT token string for API authentication testing
     */
    public static String generateTestJwtToken(String username, String userType) {
        Date expirationDate = new Date(System.currentTimeMillis() + TEST_TOKEN_VALIDITY);
        String role = "A".equals(userType) ? "ROLE_ADMIN" : "ROLE_USER";
        
        return Jwts.builder()
                .setSubject(username)
                .claim("userType", userType)
                .claim("role", role)
                .claim("userId", username.toUpperCase())
                .setIssuedAt(new Date())
                .setExpiration(expirationDate)
                .setIssuer("carddemo-test")
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Creates a test admin user with ROLE_ADMIN authority.
     * 
     * This method creates a UserSecurity entity configured as an administrative user
     * for testing admin-only functionality. The user includes proper role mapping
     * and authority configuration for comprehensive authorization testing scenarios.
     * 
     * @return UserSecurity entity configured as test admin user
     */
    public UserSecurity createTestAdmin() {
        return createAdminUserInternal();
    }

    /**
     * Creates a test regular user with ROLE_USER authority.
     * 
     * This method creates a UserSecurity entity configured as a regular user
     * for testing standard user functionality. The user includes proper role mapping
     * and authority configuration for testing user-level access restrictions
     * and standard business functionality.
     * 
     * @return UserSecurity entity configured as test regular user
     */
    public UserSecurity createTestUser() {
        return createRegularUserInternal();
    }

    /**
     * Provides a mock authentication provider for isolated unit testing.
     * 
     * This bean creates a mock DaoAuthenticationProvider that can be configured
     * to simulate various authentication scenarios without requiring actual
     * database connectivity or external service dependencies.
     * 
     * @return Mock AuthenticationProvider for unit test isolation
     */
    @Bean
    @Primary
    public AuthenticationProvider mockAuthenticationProvider() {
        DaoAuthenticationProvider provider = mock(DaoAuthenticationProvider.class);
        
        // Configure default mock behavior for successful authentication
        when(provider.supports(any(Class.class))).thenReturn(true);
        
        return provider;
    }

    /**
     * Configures test security filter chain for controller testing.
     * 
     * This method sets up a security filter chain optimized for testing scenarios,
     * including session management configuration for COMMAREA testing, CSRF handling
     * for form submissions, and JWT authentication filter integration for API testing.
     * 
     * The configuration provides:
     * - Stateless session management for API testing
     * - CSRF protection for form-based testing scenarios
     * - JWT authentication filter integration
     * - Proper exception handling for test scenarios
     * 
     * @param http HttpSecurity configuration builder
     * @return SecurityFilterChain configured for test scenarios
     * @throws Exception if security configuration fails
     */
    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for API testing
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(
                mock(JwtAuthenticationFilter.class), 
                UsernamePasswordAuthenticationFilter.class
            )
            .build();
    }

    /**
     * Provides MockMvc configurer for Spring Security test integration.
     * 
     * This method returns the Spring Security MockMvc configurer that enables
     * comprehensive security testing with MockMvc framework, providing authentication
     * context setup and security filter chain integration for controller tests.
     * 
     * @return MockMvcConfigurer for Spring Security test integration
     */
    public static MockMvcConfigurer springSecurity() {
        return SecurityMockMvcConfigurers.springSecurity();
    }

    /**
     * Utility method for generating test JWT tokens with default admin privileges.
     * 
     * This static method provides a convenient way to generate JWT tokens
     * for the default admin user in test scenarios, simplifying test setup and 
     * authentication context configuration for administrative function testing.
     * 
     * @return Valid JWT token string for default admin user authentication
     */
    public static String generateTestJwtToken() {
        return generateTestJwtToken(ADMIN_USERNAME, ADMIN_USER_TYPE);
    }

    /**
     * Internal helper method for creating admin user entities.
     * 
     * @return UserSecurity entity configured as admin user
     */
    private static UserSecurity createAdminUserInternal() {
        UserSecurity adminUser = new UserSecurity(
            ADMIN_USER_ID,
            ADMIN_USERNAME,
            new BCryptPasswordEncoder().encode(ADMIN_PASSWORD),
            ADMIN_FIRST_NAME,
            ADMIN_LAST_NAME,
            ADMIN_USER_TYPE
        );
        
        adminUser.setEnabled(true);
        adminUser.setAccountNonExpired(true);
        adminUser.setAccountNonLocked(true);
        adminUser.setCredentialsNonExpired(true);
        adminUser.setFailedLoginAttempts(0);
        adminUser.setLastLogin(LocalDateTime.now());
        
        return adminUser;
    }

    /**
     * Internal helper method for creating regular user entities.
     * 
     * @return UserSecurity entity configured as regular user
     */
    private static UserSecurity createRegularUserInternal() {
        UserSecurity regularUser = new UserSecurity(
            REGULAR_USER_ID,
            REGULAR_USERNAME,
            new BCryptPasswordEncoder().encode(REGULAR_PASSWORD),
            REGULAR_FIRST_NAME,
            REGULAR_LAST_NAME,
            REGULAR_USER_TYPE
        );
        
        regularUser.setEnabled(true);
        regularUser.setAccountNonExpired(true);
        regularUser.setAccountNonLocked(true);
        regularUser.setCredentialsNonExpired(true);
        regularUser.setFailedLoginAttempts(0);
        regularUser.setLastLogin(LocalDateTime.now());
        
        return regularUser;
    }

    /**
     * Utility method for creating test admin user entities.
     * 
     * This static method provides a convenient way to create admin user
     * entities for test scenarios, enabling easy setup of test data with
     * administrative privileges for comprehensive authorization testing.
     * 
     * @return UserSecurity entity configured as admin user
     */
    public static UserSecurity createTestAdminUser() {
        return createAdminUserInternal();
    }

    /**
     * Utility method for creating test regular user entities.
     * 
     * This static method provides a convenient way to create regular user
     * entities for test scenarios, enabling easy setup of test data with
     * standard user privileges for user-level functionality testing.
     * 
     * @return UserSecurity entity configured as regular user
     */
    public static UserSecurity createTestRegularUser() {
        return createRegularUserInternal();
    }
}