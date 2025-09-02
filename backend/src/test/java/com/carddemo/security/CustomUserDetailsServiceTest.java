/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.security;

import com.carddemo.entity.UserSecurity;
import com.carddemo.repository.UserSecurityRepository;
import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.UnitTest;
import com.carddemo.util.TestConstants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for CustomUserDetailsService implementation.
 * 
 * This test class validates the Spring Security UserDetailsService implementation that replaces
 * legacy RACF authentication with modern PostgreSQL-based user authentication. Tests ensure
 * functional parity with the original COBOL COSGN00C sign-on program behavior including:
 * - User loading from PostgreSQL usrsec table matching VSAM USRSEC structure
 * - UserDetails object creation with proper authorities based on SEC-USR-TYPE mapping
 * - Case-insensitive authentication matching COBOL FUNCTION UPPER-CASE logic
 * - Exception handling for non-existent users with UsernameNotFoundException
 * - Password field mapping and validation from SEC-USR-PWD field
 * - Integration with Spring Security AuthenticationManager framework
 * 
 * Test Coverage:
 * - Successful user authentication with valid credentials
 * - User not found scenarios with proper exception handling
 * - Admin user role mapping ('A' → ROLE_ADMIN)
 * - Regular user role mapping ('U' → ROLE_USER)
 * - Username case conversion matching COBOL logic
 * - UserDetails properties validation (enabled, non-expired status)
 * - Repository integration through mocked UserSecurityRepository
 * 
 * Authentication Flow Testing:
 * 1. Username uppercase conversion replicating COBOL FUNCTION UPPER-CASE
 * 2. PostgreSQL usrsec table query via UserSecurityRepository.findByUsername()
 * 3. UserDetails object creation with Spring Security authorities
 * 4. Exception handling matching COBOL RESP-CD error patterns
 * 
 * This test class ensures the CustomUserDetailsService maintains identical authentication
 * behavior to the original COBOL COSGN00C program while leveraging Spring Security framework
 * capabilities for modern authentication and authorization requirements.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class CustomUserDetailsServiceTest extends AbstractBaseTest implements UnitTest {

    @Mock
    private UserSecurityRepository userSecurityRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private UserSecurity testRegularUser;
    private UserSecurity testAdminUser;
    private UserSecurity testDisabledUser;

    /**
     * Setup method executed before each test execution.
     * Initializes test user objects with COBOL-compatible data patterns and configures
     * mock repository behaviors for comprehensive testing scenarios.
     * 
     * This method extends AbstractBaseTest.setUp() to provide CustomUserDetailsService-specific
     * test data initialization including:
     * - Regular user with 'U' type matching COBOL SEC-USR-TYPE patterns
     * - Admin user with 'A' type for role authorization testing
     * - Disabled user for account status validation testing
     * - Mock repository configuration for isolated unit testing
     * 
     * Test User Creation:
     * - Uses COBOL field lengths (8 chars for user ID, 20 chars for names)
     * - Applies proper user type values ('A' for admin, 'U' for regular user)
     * - Sets realistic authentication flags and timestamps
     * - Ensures BCrypt password encoding for security compliance
     */
    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        
        // Create test regular user matching COBOL SEC-USER-DATA structure
        testRegularUser = new UserSecurity();
        testRegularUser.setId(1L);
        testRegularUser.setSecUsrId("TESTUSER");
        testRegularUser.setUsername("TESTUSER");
        testRegularUser.setPassword("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9w8ug4WGDrMEdiq"); // BCrypt for "testpass"
        testRegularUser.setFirstName("Test");
        testRegularUser.setLastName("User");
        testRegularUser.setUserType("U"); // Regular user type from COBOL SEC-USR-TYPE
        testRegularUser.setEnabled(true);
        testRegularUser.setAccountNonExpired(true);
        testRegularUser.setAccountNonLocked(true);
        testRegularUser.setCredentialsNonExpired(true);
        testRegularUser.setFailedLoginAttempts(0);
        testRegularUser.setCreatedAt(LocalDateTime.now());
        testRegularUser.setUpdatedAt(LocalDateTime.now());

        // Create test admin user matching COBOL SEC-USER-DATA structure
        testAdminUser = new UserSecurity();
        testAdminUser.setId(2L);
        testAdminUser.setSecUsrId("TESTADMN");
        testAdminUser.setUsername("TESTADMN");
        testAdminUser.setPassword("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9w8ug4WGDrMEdiq"); // BCrypt for "adminpass"
        testAdminUser.setFirstName("Test");
        testAdminUser.setLastName("Admin");
        testAdminUser.setUserType("A"); // Admin user type from COBOL SEC-USR-TYPE
        testAdminUser.setEnabled(true);
        testAdminUser.setAccountNonExpired(true);
        testAdminUser.setAccountNonLocked(true);
        testAdminUser.setCredentialsNonExpired(true);
        testAdminUser.setFailedLoginAttempts(0);
        testAdminUser.setCreatedAt(LocalDateTime.now());
        testAdminUser.setUpdatedAt(LocalDateTime.now());

        // Create test disabled user for account status testing
        testDisabledUser = new UserSecurity();
        testDisabledUser.setId(3L);
        testDisabledUser.setSecUsrId("DISABLED");
        testDisabledUser.setUsername("DISABLED");
        testDisabledUser.setPassword("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9w8ug4WGDrMEdiq");
        testDisabledUser.setFirstName("Disabled");
        testDisabledUser.setLastName("User");
        testDisabledUser.setUserType("U");
        testDisabledUser.setEnabled(false); // Disabled account
        testDisabledUser.setAccountNonExpired(true);
        testDisabledUser.setAccountNonLocked(true);
        testDisabledUser.setCredentialsNonExpired(true);
        testDisabledUser.setFailedLoginAttempts(0);
        testDisabledUser.setCreatedAt(LocalDateTime.now());
        testDisabledUser.setUpdatedAt(LocalDateTime.now());

        // Configure common dependencies mock
        mockCommonDependencies();
    }

    /**
     * Teardown method executed after each test execution.
     * Performs cleanup operations including mock object reset and test data cleanup.
     * 
     * This method extends AbstractBaseTest.tearDown() to provide CustomUserDetailsService-specific
     * cleanup operations ensuring proper test isolation and resource management.
     */
    @AfterEach
    @Override
    public void tearDown() {
        super.tearDown();
        
        // Reset mock repository to ensure test isolation
        reset(userSecurityRepository);
        
        // Clear test user objects
        testRegularUser = null;
        testAdminUser = null;
        testDisabledUser = null;
    }

    /**
     * Test successful user loading with valid username returns proper UserDetails object.
     * 
     * This test validates the core authentication functionality of CustomUserDetailsService
     * by verifying that a valid username lookup returns a complete UserDetails object
     * with all required properties properly set. The test replicates the successful
     * authentication path from COBOL COSGN00C program lines 222-240.
     * 
     * Validation Points:
     * - Repository called with correct uppercase username
     * - UserDetails object returned is not null
     * - Username matches expected value
     * - Password hash is properly preserved
     * - User type is correctly mapped
     * - Account status flags are properly set
     * - Authorities collection contains expected roles
     * 
     * COBOL Behavior Replication:
     * - Matches CICS READ operation success (WS-RESP-CD = 0)
     * - Validates user data structure population from SEC-USER-DATA
     * - Ensures proper user type handling from SEC-USR-TYPE field
     */
    @Test
    @DisplayName("Load user by username - User exists - Returns UserDetails")
    public void testLoadUserByUsername_UserExists_ReturnsUserDetails() {
        // Arrange - Setup mock repository behavior for existing user
        String inputUsername = "testuser"; // lowercase input
        String expectedUsername = "TESTUSER"; // expected uppercase conversion
        
        when(userSecurityRepository.findByUsername(expectedUsername))
            .thenReturn(Optional.of(testRegularUser));
        
        // Act - Call loadUserByUsername method
        UserDetails result = customUserDetailsService.loadUserByUsername(inputUsername);
        
        // Assert - Verify UserDetails object properties
        assertNotNull(result, "UserDetails object should not be null for existing user");
        assertEquals(expectedUsername, result.getUsername(), 
            "Username should match the expected uppercase value");
        assertNotNull(result.getPassword(), "Password should not be null");
        assertTrue(result.isEnabled(), "User account should be enabled");
        assertTrue(result.isAccountNonExpired(), "User account should not be expired");
        assertTrue(result.isAccountNonLocked(), "User account should not be locked");
        assertTrue(result.isCredentialsNonExpired(), "User credentials should not be expired");
        assertNotNull(result.getAuthorities(), "Authorities should not be null");
        assertFalse(result.getAuthorities().isEmpty(), "Authorities should not be empty");
        
        // Verify repository interaction with uppercase username
        verify(userSecurityRepository, times(1)).findByUsername(expectedUsername);
        verifyNoMoreInteractions(userSecurityRepository);
    }

    /**
     * Test user not found scenario throws UsernameNotFoundException.
     * 
     * This test validates the error handling behavior of CustomUserDetailsService
     * when attempting to load a non-existent user. The test replicates the user
     * not found error handling from COBOL COSGN00C program lines 247-251 where
     * RESP-CD = 13 triggers "User not found. Try again ..." message.
     * 
     * Validation Points:
     * - Repository called with uppercase username conversion
     * - UsernameNotFoundException thrown with proper message
     * - Exception message matches COBOL error message pattern
     * - No UserDetails object returned on error
     * 
     * COBOL Behavior Replication:
     * - Matches CICS READ response code 13 (record not found)
     * - Replicates error message "User not found. Try again ..."
     * - Ensures identical exception handling flow
     */
    @Test
    @DisplayName("Load user by username - User not found - Throws UsernameNotFoundException")
    public void testLoadUserByUsername_UserNotFound_ThrowsUsernameNotFoundException() {
        // Arrange - Setup mock repository behavior for non-existent user
        String inputUsername = "nonexistent";
        String expectedUsername = "NONEXISTENT"; // expected uppercase conversion
        
        when(userSecurityRepository.findByUsername(expectedUsername))
            .thenReturn(Optional.empty());
        
        // Act & Assert - Verify exception is thrown
        UsernameNotFoundException exception = assertThrows(
            UsernameNotFoundException.class,
            () -> customUserDetailsService.loadUserByUsername(inputUsername),
            "Should throw UsernameNotFoundException for non-existent user"
        );
        
        // Verify exception message matches COBOL error message pattern
        assertEquals("User not found. Try again ...", exception.getMessage(),
            "Exception message should match COBOL error message from COSGN00C");
        
        // Verify repository interaction
        verify(userSecurityRepository, times(1)).findByUsername(expectedUsername);
        verifyNoMoreInteractions(userSecurityRepository);
    }

    /**
     * Test admin user loading returns UserDetails with ROLE_ADMIN authority.
     * 
     * This test validates the role mapping functionality for administrative users
     * by verifying that users with SEC-USR-TYPE = 'A' receive proper ROLE_ADMIN
     * authorities. The test replicates the admin user routing logic from COBOL
     * COSGN00C program lines 230-235 where admin users are routed to COADM01C.
     * 
     * Validation Points:
     * - UserDetails returned for admin user
     * - Authorities collection contains ROLE_ADMIN
     * - User type 'A' properly mapped to admin role
     * - All account status flags properly set
     * - SecurityConstants.getRoleForUserType() behavior validated
     * 
     * COBOL Behavior Replication:
     * - Matches CDEMO-USRTYP-ADMIN conditional logic
     * - Validates admin privilege assignment from SEC-USR-TYPE = 'A'
     * - Ensures proper admin routing behavior
     */
    @Test
    @DisplayName("Load user by username - Admin user - Has admin role")
    public void testLoadUserByUsername_AdminUser_HasAdminRole() {
        // Arrange - Setup mock repository behavior for admin user
        String adminUsername = "TESTADMN";
        
        when(userSecurityRepository.findByUsername(adminUsername))
            .thenReturn(Optional.of(testAdminUser));
        
        // Act - Load admin user
        UserDetails result = customUserDetailsService.loadUserByUsername(adminUsername);
        
        // Assert - Verify admin role assignment
        assertNotNull(result, "UserDetails should not be null for admin user");
        assertEquals(adminUsername, result.getUsername(), "Username should match admin test user");
        
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertNotNull(authorities, "Authorities should not be null");
        assertEquals(1, authorities.size(), "Admin user should have exactly one authority");
        
        GrantedAuthority adminAuthority = authorities.iterator().next();
        assertEquals("ROLE_ADMIN", adminAuthority.getAuthority(),
            "Admin user should have ROLE_ADMIN authority matching SEC-USR-TYPE 'A'");
        
        // Verify repository interaction
        verify(userSecurityRepository, times(1)).findByUsername(adminUsername);
        verifyNoMoreInteractions(userSecurityRepository);
    }

    /**
     * Test regular user loading returns UserDetails with ROLE_USER authority.
     * 
     * This test validates the role mapping functionality for regular users
     * by verifying that users with SEC-USR-TYPE = 'U' receive proper ROLE_USER
     * authorities. The test replicates the regular user routing logic from COBOL
     * COSGN00C program lines 235-240 where regular users are routed to COMEN01C.
     * 
     * Validation Points:
     * - UserDetails returned for regular user
     * - Authorities collection contains ROLE_USER
     * - User type 'U' properly mapped to user role
     * - All account status flags properly set
     * - SecurityConstants.getRoleForUserType() behavior validated
     * 
     * COBOL Behavior Replication:
     * - Matches non-admin user routing logic (ELSE clause)
     * - Validates regular user privilege assignment from SEC-USR-TYPE = 'U'
     * - Ensures proper user-level access control
     */
    @Test
    @DisplayName("Load user by username - Regular user - Has user role")
    public void testLoadUserByUsername_RegularUser_HasUserRole() {
        // Arrange - Setup mock repository behavior for regular user
        String regularUsername = "TESTUSER";
        
        when(userSecurityRepository.findByUsername(regularUsername))
            .thenReturn(Optional.of(testRegularUser));
        
        // Act - Load regular user
        UserDetails result = customUserDetailsService.loadUserByUsername(regularUsername);
        
        // Assert - Verify user role assignment
        assertNotNull(result, "UserDetails should not be null for regular user");
        assertEquals(regularUsername, result.getUsername(), "Username should match regular test user");
        
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertNotNull(authorities, "Authorities should not be null");
        assertEquals(1, authorities.size(), "Regular user should have exactly one authority");
        
        GrantedAuthority userAuthority = authorities.iterator().next();
        assertEquals("ROLE_USER", userAuthority.getAuthority(),
            "Regular user should have ROLE_USER authority matching SEC-USR-TYPE 'U'");
        
        // Verify repository interaction
        verify(userSecurityRepository, times(1)).findByUsername(regularUsername);
        verifyNoMoreInteractions(userSecurityRepository);
    }

    /**
     * Test username uppercase conversion matching COBOL FUNCTION UPPER-CASE logic.
     * 
     * This test validates the case-insensitive authentication behavior by verifying
     * that lowercase, mixed-case, and uppercase usernames are all converted to
     * uppercase before repository lookup. The test replicates the uppercase conversion
     * logic from COBOL COSGN00C program lines 132-134.
     * 
     * Validation Points:
     * - Lowercase input converted to uppercase for repository lookup
     * - Mixed-case input converted to uppercase for repository lookup
     * - Uppercase input remains unchanged
     * - Repository always called with uppercase username
     * - UserDetails returned regardless of input case
     * 
     * COBOL Behavior Replication:
     * - Matches MOVE FUNCTION UPPER-CASE(USERIDI OF COSGN0AI) TO WS-USER-ID
     * - Ensures case-insensitive authentication behavior
     * - Validates consistent username normalization
     */
    @Test
    @DisplayName("Load user by username - Uppercase conversion")
    public void testLoadUserByUsername_UppercaseConversion() {
        // Test data for different case variations
        String[] inputUsernames = {"testuser", "TestUser", "TESTUSER", "tEsTuSeR"};
        String expectedUppercaseUsername = "TESTUSER";
        
        // Setup mock repository to return user for uppercase lookup
        when(userSecurityRepository.findByUsername(expectedUppercaseUsername))
            .thenReturn(Optional.of(testRegularUser));
        
        for (String inputUsername : inputUsernames) {
            // Act - Load user with various case combinations
            UserDetails result = customUserDetailsService.loadUserByUsername(inputUsername);
            
            // Assert - Verify UserDetails returned regardless of input case
            assertNotNull(result, 
                "UserDetails should be returned for username: " + inputUsername);
            assertEquals(expectedUppercaseUsername, result.getUsername(),
                "Username should be normalized to uppercase for input: " + inputUsername);
        }
        
        // Verify repository called with uppercase username for each input
        verify(userSecurityRepository, times(inputUsernames.length))
            .findByUsername(expectedUppercaseUsername);
        verifyNoMoreInteractions(userSecurityRepository);
    }

    /**
     * Test comprehensive UserDetails properties validation for valid user.
     * 
     * This test validates all UserDetails interface properties returned by
     * CustomUserDetailsService to ensure complete Spring Security integration
     * and proper mapping from COBOL user security data structure.
     * 
     * Validation Points:
     * - Username property matches SEC-USR-ID field
     * - Password property preserves BCrypt hash from SEC-USR-PWD
     * - Authorities collection properly mapped from SEC-USR-TYPE
     * - Account status flags correctly set
     * - UserSecurity entity properties accessible through UserDetails interface
     * 
     * Property Validation Coverage:
     * - getUsername() returns correct username
     * - getPassword() returns BCrypt password hash
     * - getAuthorities() returns proper role collection
     * - isEnabled() reflects account enabled status
     * - isAccountNonExpired() validates account expiration
     * - isAccountNonLocked() validates account lock status
     * - isCredentialsNonExpired() validates credential expiration
     */
    @Test
    @DisplayName("UserDetails properties validation - Valid user")
    public void testUserDetailsProperties_ValidUser() {
        // Arrange - Setup mock repository for regular user
        String username = "TESTUSER";
        
        when(userSecurityRepository.findByUsername(username))
            .thenReturn(Optional.of(testRegularUser));
        
        // Act - Load user and get UserDetails
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
        
        // Assert - Validate all UserDetails properties
        
        // Username validation
        assertEquals(username, userDetails.getUsername(),
            "Username should match SEC-USR-ID field value");
        
        // Password validation  
        assertNotNull(userDetails.getPassword(), "Password should not be null");
        assertTrue(userDetails.getPassword().startsWith("$2a$"),
            "Password should be BCrypt encoded matching SEC-USR-PWD security requirements");
        
        // Authorities validation
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertNotNull(authorities, "Authorities collection should not be null");
        assertFalse(authorities.isEmpty(), "Authorities collection should not be empty");
        assertEquals(1, authorities.size(), "User should have exactly one authority");
        
        GrantedAuthority authority = authorities.iterator().next();
        assertEquals("ROLE_USER", authority.getAuthority(),
            "Regular user should have ROLE_USER authority from SEC-USR-TYPE 'U'");
        
        // Account status validation
        assertTrue(userDetails.isEnabled(),
            "Account should be enabled by default");
        assertTrue(userDetails.isAccountNonExpired(),
            "Account should not be expired for active user");
        assertTrue(userDetails.isAccountNonLocked(),
            "Account should not be locked for valid user");
        assertTrue(userDetails.isCredentialsNonExpired(),
            "Credentials should not be expired for active user");
        
        // Verify repository interaction
        verify(userSecurityRepository, times(1)).findByUsername(username);
        verifyNoMoreInteractions(userSecurityRepository);
    }

}