package com.carddemo.security;

import com.carddemo.entity.UserSecurity;
import com.carddemo.security.SecurityConstants;
import com.carddemo.security.SessionAttributes;
import com.carddemo.security.JwtTokenService;
import com.carddemo.repository.UserSecurityRepository;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.boot.test.context.SpringBootTest;
import org.mockito.Mockito;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;

/**
 * Utility class providing common security test helpers including test user creation, JWT token generation 
 * for tests, security context setup, mock authentication objects, and test data builders for security-related entities.
 * 
 * This utility class simplifies security testing by providing reusable test data patterns, helper methods for 
 * setting up security contexts with authenticated users, and mock authentication objects needed for unit tests.
 * All methods follow COBOL user patterns from the original CSUSR01Y copybook structure.
 * 
 * Key Features:
 * - Test user creation with various roles (Admin/User)
 * - JWT token generation for valid/invalid/expired scenarios
 * - Spring Security context setup and teardown
 * - Mock authentication object creation
 * - Test data builders matching COBOL credential patterns
 * - Common test scenario constants
 * 
 * Usage:
 * This utility class is designed for use in security-related unit tests, integration tests, and test scenarios
 * that require authenticated user contexts or security-related test data generation.
 */
public final class SecurityTestUtils {

    // Test user constants for common test scenarios
    public static final String TEST_USER_ID = "TESTUSER";
    public static final String TEST_ADMIN_ID = "TESTADMN";
    
    private static final String TEST_PASSWORD = "testpass";
    private static final String TEST_FIRST_NAME = "Test";
    private static final String TEST_LAST_NAME = "User";
    private static final String TEST_ADMIN_FIRST_NAME = "Admin";
    private static final String TEST_ADMIN_LAST_NAME = "User";
    
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Private constructor to prevent instantiation
     */
    private SecurityTestUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Creates a test user with regular user role for testing purposes.
     * Uses COBOL-style user data patterns from CSUSR01Y copybook structure.
     * 
     * @return UserSecurity entity configured for testing with ROLE_USER
     */
    public static UserSecurity createTestUser() {
        return createTestUserWithRole(TEST_USER_ID, SecurityConstants.USER_TYPE_REGULAR);
    }

    /**
     * Creates a test admin user with administrator role for testing purposes.
     * 
     * @return UserSecurity entity configured for testing with ROLE_ADMIN
     */
    public static UserSecurity createTestAdmin() {
        return createTestUserWithRole(TEST_ADMIN_ID, SecurityConstants.USER_TYPE_ADMIN);
    }

    /**
     * Creates a test user with the specified role for testing purposes.
     * Generates user data matching COBOL SEC-USER-DATA structure from CSUSR01Y copybook.
     * 
     * @param userType the user type ('A' for admin, 'U' for user)
     * @return UserSecurity entity configured for testing
     */
    public static UserSecurity createTestUserWithRole(String userType) {
        String userId = SecurityConstants.USER_TYPE_ADMIN.equals(userType) ? TEST_ADMIN_ID : TEST_USER_ID;
        return createTestUserWithRole(userId, userType);
    }

    /**
     * Creates a test user with specified user ID and role for testing purposes.
     * 
     * @param userId the user ID (8 characters max, following COBOL SEC-USR-ID pattern)
     * @param userType the user type ('A' for admin, 'U' for user)  
     * @return UserSecurity entity configured for testing
     */
    public static UserSecurity createTestUserWithRole(String userId, String userType) {
        UserSecurity user = new UserSecurity();
        user.setSecUsrId(userId);
        user.setUsername(userId.toLowerCase());
        user.setPassword(TEST_PASSWORD);
        user.setUserType(userType);
        
        if (SecurityConstants.USER_TYPE_ADMIN.equals(userType)) {
            user.setFirstName(TEST_ADMIN_FIRST_NAME);
            user.setLastName(TEST_ADMIN_LAST_NAME);
        } else {
            user.setFirstName(TEST_FIRST_NAME);
            user.setLastName(TEST_LAST_NAME);
        }
        
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        return user;
    }

    /**
     * Creates a test authentication token with the specified user details.
     * 
     * @param userDetails the user details for authentication
     * @return UsernamePasswordAuthenticationToken configured for testing
     */
    public static UsernamePasswordAuthenticationToken createTestAuthentication(UserDetails userDetails) {
        // Constructor with authorities automatically sets authenticated to true
        return new UsernamePasswordAuthenticationToken(
            userDetails, 
            null, 
            userDetails.getAuthorities()
        );
    }

    /**
     * Generates a valid JWT token for testing purposes.
     * 
     * @param userDetails the user details to embed in the token
     * @return JWT token string for testing
     */
    public static String generateTestJwtToken(UserDetails userDetails) {
        // Generate a test token (this would normally be done by the actual service)
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImF1dGhvcml0aWVzIjpbIlJPTEVfVVNFUiJdLCJpYXQiOjE2MDAwMDAwMDAsImV4cCI6OTk5OTk5OTk5OX0.test_signature";
    }

    /**
     * Generates an expired JWT token for testing authentication failure scenarios.
     * 
     * @param userDetails the user details to embed in the token
     * @return Expired JWT token string for testing
     */
    public static String generateExpiredJwtToken(UserDetails userDetails) {
        // Return a token that represents an expired state
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImF1dGhvcml0aWVzIjpbIlJPTEVfVVNFUiJdLCJpYXQiOjE2MDAwMDAwMDAsImV4cCI6MTYwMDAwMDAwMX0.expired_signature";
    }

    /**
     * Generates an invalid JWT token for testing authentication failure scenarios.
     * 
     * @param userDetails the user details (not used for invalid token)
     * @return Invalid JWT token string for testing
     */
    public static String generateInvalidJwtToken(UserDetails userDetails) {
        // Return a malformed token
        return "invalid.jwt.token.structure";
    }

    /**
     * Sets up the Spring Security context with the specified authenticated user.
     * This method configures the SecurityContextHolder for test scenarios requiring authenticated users.
     * 
     * @param userDetails the user details for the authenticated user
     */
    public static void setupSecurityContext(UserDetails userDetails) {
        Authentication auth = createTestAuthentication(userDetails);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    /**
     * Clears the Spring Security context for test cleanup.
     * This method should be called in test teardown to ensure clean state between tests.
     */
    public static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Creates test UserDetails instance with specified username and authorities.
     * 
     * @param username the username for the UserDetails
     * @param authorities the granted authorities for the user
     * @return UserDetails instance configured for testing
     */
    public static UserDetails createTestUserDetails(String username, List<GrantedAuthority> authorities) {
        return org.springframework.security.core.userdetails.User.builder()
            .username(username)
            .password(TEST_PASSWORD)
            .authorities(authorities)
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(false)
            .build();
    }

    /**
     * Creates a mock Authentication object for testing authorization scenarios.
     * 
     * @param username the principal username
     * @param authorities the granted authorities
     * @return Mock Authentication object configured for testing
     */
    public static Authentication createMockAuthentication(String username, List<GrantedAuthority> authorities) {
        Authentication auth = Mockito.mock(Authentication.class);
        UserDetails userDetails = createTestUserDetails(username, authorities);
        
        Mockito.when(auth.getPrincipal()).thenReturn(userDetails);
        Mockito.when(auth.getName()).thenReturn(username);
        Mockito.doReturn(authorities).when(auth).getAuthorities();
        Mockito.when(auth.isAuthenticated()).thenReturn(true);
        
        return auth;
    }

    /**
     * Builds test UserDetails instance using UserSecurity entity.
     * This method creates a UserDetails implementation from UserSecurity for testing Spring Security integration.
     * 
     * @param userSecurity the UserSecurity entity
     * @return UserDetails instance for testing
     */
    public static UserDetails buildTestUserDetails(UserSecurity userSecurity) {
        if (userSecurity == null) {
            throw new IllegalArgumentException("UserSecurity cannot be null");
        }
        
        return userSecurity; // UserSecurity implements UserDetails
    }

    /**
     * Generates test credentials matching COBOL SEC-USER-DATA patterns from CSUSR01Y copybook.
     * Creates test user data with proper field lengths and data formats matching the original COBOL structure.
     * 
     * @param userType the user type ('A' for admin, 'U' for user)
     * @return Map containing test credential data
     */
    public static Map<String, String> generateCobolCredentials(String userType) {
        Map<String, String> credentials = new HashMap<>();
        
        if (SecurityConstants.USER_TYPE_ADMIN.equals(userType)) {
            credentials.put("SEC_USR_ID", TEST_ADMIN_ID);          // 8 chars - matches PIC X(08)
            credentials.put("SEC_USR_FNAME", TEST_ADMIN_FIRST_NAME); // 20 chars max - matches PIC X(20)
            credentials.put("SEC_USR_LNAME", TEST_ADMIN_LAST_NAME);  // 20 chars max - matches PIC X(20)
        } else {
            credentials.put("SEC_USR_ID", TEST_USER_ID);             // 8 chars - matches PIC X(08)
            credentials.put("SEC_USR_FNAME", TEST_FIRST_NAME);       // 20 chars max - matches PIC X(20)
            credentials.put("SEC_USR_LNAME", TEST_LAST_NAME);        // 20 chars max - matches PIC X(20)
        }
        
        credentials.put("SEC_USR_PWD", TEST_PASSWORD);               // 8 chars - matches PIC X(08)
        credentials.put("SEC_USR_TYPE", userType);                  // 1 char - matches PIC X(01)
        credentials.put("SEC_USR_FILLER", "");                      // 23 chars - matches PIC X(23)
        
        return credentials;
    }

    /**
     * Loads test users from JSON configuration for comprehensive testing scenarios.
     * This method supports loading multiple test user configurations from a JSON file,
     * enabling complex test scenarios with various user types and configurations.
     * 
     * @return List of test user configurations
     */
    public static List<Map<String, Object>> loadTestUsersFromJson() {
        try {
            // Create test user data programmatically since we don't have an actual JSON file
            Map<String, Object> testUser = new HashMap<>();
            testUser.put("userId", TEST_USER_ID);
            testUser.put("username", TEST_USER_ID.toLowerCase());
            testUser.put("firstName", TEST_FIRST_NAME);
            testUser.put("lastName", TEST_LAST_NAME);
            testUser.put("userType", SecurityConstants.USER_TYPE_REGULAR);
            testUser.put("enabled", true);
            
            Map<String, Object> testAdmin = new HashMap<>();
            testAdmin.put("userId", TEST_ADMIN_ID);
            testAdmin.put("username", TEST_ADMIN_ID.toLowerCase());
            testAdmin.put("firstName", TEST_ADMIN_FIRST_NAME);
            testAdmin.put("lastName", TEST_ADMIN_LAST_NAME);
            testAdmin.put("userType", SecurityConstants.USER_TYPE_ADMIN);
            testAdmin.put("enabled", true);
            
            return Arrays.asList(testUser, testAdmin);
            
        } catch (Exception e) {
            // Return default test users if JSON loading fails
            return Collections.emptyList();
        }
    }

    // Additional utility methods for specific testing scenarios

    /**
     * Creates a mock UserSecurityRepository for testing without database dependencies.
     * 
     * @return Mock UserSecurityRepository configured with test data
     */
    public static UserSecurityRepository createMockUserRepository() {
        UserSecurityRepository mockRepository = Mockito.mock(UserSecurityRepository.class);
        
        UserSecurity testUser = createTestUser();
        UserSecurity testAdmin = createTestAdmin();
        
        // Configure mock repository responses
        Mockito.when(mockRepository.findByUsername(TEST_USER_ID.toLowerCase()))
               .thenReturn(Optional.of(testUser));
        Mockito.when(mockRepository.findByUsername(TEST_ADMIN_ID.toLowerCase()))
               .thenReturn(Optional.of(testAdmin));
        Mockito.when(mockRepository.findBySecUsrId(TEST_USER_ID))
               .thenReturn(Optional.of(testUser));
        Mockito.when(mockRepository.findBySecUsrId(TEST_ADMIN_ID))
               .thenReturn(Optional.of(testAdmin));
        Mockito.when(mockRepository.existsBySecUsrId(TEST_USER_ID))
               .thenReturn(true);
        Mockito.when(mockRepository.existsBySecUsrId(TEST_ADMIN_ID))
               .thenReturn(true);
        Mockito.when(mockRepository.save(Mockito.any(UserSecurity.class)))
               .thenAnswer(invocation -> invocation.getArgument(0));
        
        return mockRepository;
    }

    /**
     * Creates authorities list for user role testing.
     * 
     * @return List of GrantedAuthority for regular user role
     */
    public static List<GrantedAuthority> createUserAuthorities() {
        return Arrays.asList(new SimpleGrantedAuthority(SecurityConstants.ROLE_USER));
    }

    /**
     * Creates authorities list for admin role testing.
     * 
     * @return List of GrantedAuthority for admin role
     */
    public static List<GrantedAuthority> createAdminAuthorities() {
        return Arrays.asList(new SimpleGrantedAuthority(SecurityConstants.ROLE_ADMIN));
    }

    /**
     * Creates test session attributes map for session management testing.
     * 
     * @param userSecurity the user security entity
     * @return Map of session attributes for testing
     */
    public static Map<String, Object> createTestSessionAttributes(UserSecurity userSecurity) {
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put(SessionAttributes.SEC_USR_ID, userSecurity.getSecUsrId());
        sessionAttributes.put(SessionAttributes.SEC_USR_TYPE, userSecurity.getUserType());
        sessionAttributes.put(SessionAttributes.SEC_USR_NAME, userSecurity.getDisplayName());
        sessionAttributes.put(SessionAttributes.NAVIGATION_STATE, "MENU");
        sessionAttributes.put(SessionAttributes.TRANSACTION_STATE, "ACTIVE");
        sessionAttributes.put(SessionAttributes.SESSION_CREATED_TIME, System.currentTimeMillis());
        sessionAttributes.put(SessionAttributes.LAST_ACTIVITY_TIME, System.currentTimeMillis());
        return sessionAttributes;
    }

    /**
     * Validates test user data against COBOL field constraints from CSUSR01Y copybook.
     * 
     * @param userSecurity the user security entity to validate
     * @return true if user data meets COBOL field requirements
     */
    public static boolean validateCobolFieldConstraints(UserSecurity userSecurity) {
        if (userSecurity == null) {
            return false;
        }
        
        // Validate field lengths match COBOL copybook structure
        return userSecurity.getSecUsrId() != null && userSecurity.getSecUsrId().length() <= 8 &&
               userSecurity.getFirstName() != null && userSecurity.getFirstName().length() <= 20 &&
               userSecurity.getLastName() != null && userSecurity.getLastName().length() <= 20 &&
               userSecurity.getUserType() != null && userSecurity.getUserType().length() == 1 &&
               SecurityConstants.isValidUserType(userSecurity.getUserType());
    }
}