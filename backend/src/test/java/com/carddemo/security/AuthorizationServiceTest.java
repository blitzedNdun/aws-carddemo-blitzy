package com.carddemo.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.util.Arrays;

/**
 * Unit tests for AuthorizationService testing role-based authorization logic
 * matching COBOL SEC-USR-TYPE permissions and Spring Security integration.
 * 
 * Tests validate:
 * - Role-based access control for ROLE_ADMIN and ROLE_USER
 * - Method-level security with @PreAuthorize annotations
 * - Resource access permissions matching COBOL user type restrictions
 * - Authorization decision logic for various user-resource combinations
 * - Spring Security context propagation and authentication state
 * 
 * COBOL Mapping:
 * - SEC-USR-TYPE "A" -> ROLE_ADMIN (full access)
 * - SEC-USR-TYPE "U" -> ROLE_USER (limited access)
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.batch.job.enabled=false",
        "spring.cloud.config.enabled=false"
    },
    classes = {
        AuthorizationService.class,
        AuthorizationServiceTest.TestSecurityConfig.class
    }
)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "logging.level.com.carddemo.security=DEBUG",
    "spring.security.debug=true",
    "spring.batch.job.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;REFERENTIAL_INTEGRITY=FALSE"
})
public class AuthorizationServiceTest {

    @Autowired
    private AuthorizationService authorizationService;

    /**
     * Test setup method to initialize security context and test environment.
     * Configures Spring Security test infrastructure for authorization testing.
     */
    @BeforeEach
    public void setUp() {
        // Clear any existing security context
        SecurityContextHolder.clearContext();
        
        // Verify test environment is properly configured
        Assertions.assertNotNull(authorizationService, 
            "AuthorizationService should be autowired for testing");
    }

    /**
     * Test admin role access permissions.
     * Validates that users with ROLE_ADMIN can access administrative functions
     * matching COBOL SEC-USR-TYPE "A" permissions.
     */
    @Test
    public void testAdminRoleAccess() {
        // Manually set up admin user authentication
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken("TESTADM", "password", 
                Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        try {
            // Test basic admin role detection
            Assertions.assertTrue(authorizationService.hasAdminRole(),
                "User with ROLE_ADMIN should be detected as admin");
            
            // Test admin-specific permissions
            Assertions.assertTrue(authorizationService.canAccessAllAccounts(),
                "Admin users should have access to all accounts");
            
            Assertions.assertTrue(authorizationService.canManageUsers(),
                "Admin users should be able to manage other users");
            
            Assertions.assertTrue(authorizationService.canAccessReports(),
                "Admin users should have access to system reports");
            
            // Test account modification permissions for admin
            Assertions.assertTrue(authorizationService.canModifyAccount("12345678901"),
                "Admin users should be able to modify any account");
            
            // Test transaction access for admin
            Assertions.assertTrue(authorizationService.canAccessTransaction("TXN001"),
                "Admin users should have access to all transactions");
        } finally {
            // Clean up security context
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Test user role access permissions.
     * Validates that users with ROLE_USER have limited access
     * matching COBOL SEC-USR-TYPE "U" restrictions.
     */
    @Test
    public void testUserRoleAccess() {
        // Manually set up regular user authentication
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken("TESTUSR", "password", 
                Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        try {
            // Test basic user role detection
            Assertions.assertTrue(authorizationService.hasUserRole(),
                "User with ROLE_USER should be detected as regular user");
            
            Assertions.assertFalse(authorizationService.hasAdminRole(),
                "User with ROLE_USER should not be detected as admin");
            
            // Test user access restrictions
            Assertions.assertFalse(authorizationService.canAccessAllAccounts(),
                "Regular users should not have access to all accounts");
            
            Assertions.assertFalse(authorizationService.canManageUsers(),
                "Regular users should not be able to manage other users");
            
            Assertions.assertFalse(authorizationService.canAccessReports(),
                "Regular users should not have access to system reports");
            
            // Test limited account access for regular users
            Assertions.assertFalse(authorizationService.canModifyAccount("12345678901"),
                "Regular users should not be able to modify accounts they don't own");
        } finally {
            // Clean up security context
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Test @PreAuthorize annotation enforcement.
     * Validates method-level security annotations are properly enforced
     * and authorization decisions are correctly made.
     */
    @Test
    public void testPreAuthorizeAnnotations() {
        // Test with admin user
        setSecurityContext("TESTADM", "ROLE_ADMIN");
        
        // Verify admin can access protected methods
        Assertions.assertTrue(authorizationService.checkPermission("ADMIN_OPERATION"),
            "Admin should have permission for admin operations");
        
        // Test with regular user
        setSecurityContext("TESTUSR", "ROLE_USER");
        
        // Verify user cannot access admin-only methods
        Assertions.assertFalse(authorizationService.checkPermission("ADMIN_OPERATION"),
            "Regular user should not have permission for admin operations");
        
        // Verify user can access general operations
        Assertions.assertTrue(authorizationService.checkPermission("USER_OPERATION"),
            "Regular user should have permission for user operations");
    }

    /**
     * Test account access permissions based on ownership and role.
     * Validates account access control logic matches COBOL business rules.
     */
    @Test
    public void testAccountAccessPermissions() {
        String testAccountId = "12345678901";
        String userOwnedAccount = "98765432109";
        
        // Test admin access to any account
        setSecurityContext("TESTADM", "ROLE_ADMIN");
        Assertions.assertTrue(authorizationService.canAccessAccount(testAccountId),
            "Admin should be able to access any account");
        
        // Test user access to owned account
        setSecurityContext("TESTUSR", "ROLE_USER");
        
        // Mock isResourceOwner to return true for user-owned account
        // In real implementation, this would check database ownership
        Assertions.assertTrue(authorizationService.isResourceOwner("TESTUSR", userOwnedAccount),
            "User should be recognized as owner of their account");
        
        // User should be able to access their own account
        Assertions.assertTrue(authorizationService.canAccessAccount(userOwnedAccount),
            "User should be able to access their own account");
        
        // User should not be able to access other accounts
        Assertions.assertFalse(authorizationService.canAccessAccount(testAccountId),
            "User should not be able to access accounts they don't own");
    }

    /**
     * Test transaction access permissions.
     * Validates transaction-level access control based on user role and ownership.
     */
    @Test
    public void testTransactionAccessPermissions() {
        String testTransactionId = "TXN12345";
        String userTransactionId = "TXN67890";
        
        // Test admin access to all transactions
        setSecurityContext("TESTADM", "ROLE_ADMIN");
        Assertions.assertTrue(authorizationService.canAccessTransaction(testTransactionId),
            "Admin should be able to access any transaction");
        
        // Test user access to their own transactions
        setSecurityContext("TESTUSR", "ROLE_USER");
        
        // Mock ownership for user transaction
        Assertions.assertTrue(authorizationService.isResourceOwner("TESTUSR", userTransactionId),
            "User should be recognized as owner of their transaction");
        
        Assertions.assertTrue(authorizationService.canAccessTransaction(userTransactionId),
            "User should be able to access their own transactions");
        
        // User should not access other users' transactions
        Assertions.assertFalse(authorizationService.canAccessTransaction(testTransactionId),
            "User should not be able to access transactions they don't own");
    }

    /**
     * Test resource ownership validation.
     * Validates ownership determination logic for accounts and transactions.
     */
    @Test
    public void testResourceOwnershipValidation() {
        String userId = "TESTUSR";
        String ownedResourceId = "RESOURCE123";
        String notOwnedResourceId = "RESOURCE456";
        
        setSecurityContext(userId, "ROLE_USER");
        
        // Test ownership validation (mocked in real scenario would query database)
        Assertions.assertTrue(authorizationService.isResourceOwner(userId, ownedResourceId),
            "User should be identified as owner of their resources");
        
        Assertions.assertFalse(authorizationService.isResourceOwner(userId, notOwnedResourceId),
            "User should not be identified as owner of other users' resources");
        
        // Test null and empty parameter handling
        Assertions.assertFalse(authorizationService.isResourceOwner(null, ownedResourceId),
            "Null user ID should return false for ownership");
        
        Assertions.assertFalse(authorizationService.isResourceOwner(userId, null),
            "Null resource ID should return false for ownership");
        
        Assertions.assertFalse(authorizationService.isResourceOwner("", ownedResourceId),
            "Empty user ID should return false for ownership");
    }

    /**
     * Test unauthorized access scenarios.
     * Validates proper denial of access for unauthorized operations.
     */
    @Test
    public void testUnauthorizedAccess() {
        // Test unauthenticated access (no security context)
        SecurityContextHolder.clearContext();
        
        Assertions.assertFalse(authorizationService.hasAdminRole(),
            "Unauthenticated user should not have admin role");
        
        Assertions.assertFalse(authorizationService.hasUserRole(),
            "Unauthenticated user should not have user role");
        
        Assertions.assertFalse(authorizationService.canAccessAllAccounts(),
            "Unauthenticated user should not access all accounts");
        
        // Test user attempting admin operations
        setSecurityContext("TESTUSR", "ROLE_USER");
        
        Assertions.assertFalse(authorizationService.canManageUsers(),
            "Regular user should not be able to manage users");
        
        Assertions.assertFalse(authorizationService.canAccessReports(),
            "Regular user should not access system reports");
        
        // Test invalid role scenarios
        setSecurityContext("INVALIDUSER", "ROLE_INVALID");
        
        Assertions.assertFalse(authorizationService.hasAdminRole(),
            "User with invalid role should not have admin access");
        
        Assertions.assertFalse(authorizationService.hasUserRole(),
            "User with invalid role should not have user access");
    }

    /**
     * Test role hierarchy and permission inheritance.
     * Validates that admin roles inherit user permissions where applicable.
     */
    @Test
    public void testRoleHierarchy() {
        // Test that admin role includes user permissions
        setSecurityContext("TESTADM", "ROLE_ADMIN");
        
        // Admin should have both admin and user capabilities
        Assertions.assertTrue(authorizationService.hasAdminRole(),
            "Admin user should have admin role");
        
        // In this system, admin role should also grant user permissions
        Assertions.assertTrue(authorizationService.checkPermission("USER_OPERATION"),
            "Admin should be able to perform user operations");
        
        Assertions.assertTrue(authorizationService.checkPermission("ADMIN_OPERATION"),
            "Admin should be able to perform admin operations");
        
        // Test user role isolation
        setSecurityContext("TESTUSR", "ROLE_USER");
        
        Assertions.assertTrue(authorizationService.checkPermission("USER_OPERATION"),
            "User should be able to perform user operations");
        
        Assertions.assertFalse(authorizationService.checkPermission("ADMIN_OPERATION"),
            "User should not be able to perform admin operations");
    }

    /**
     * Test Spring Security context propagation.
     * Validates that security context is properly maintained across method calls.
     */
    @Test
    public void testSecurityContextPropagation() {
        // Set initial security context
        setSecurityContext("TESTADM", "ROLE_ADMIN");
        
        // Verify context is accessible
        Assertions.assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
            "Security context should be set");
        
        Assertions.assertEquals("TESTADM",
            SecurityContextHolder.getContext().getAuthentication().getName(),
            "Security context should contain correct username");
        
        // Test context maintains state across authorization checks
        Assertions.assertTrue(authorizationService.hasAdminRole(),
            "Security context should persist for authorization checks");
        
        // Test context clearing
        SecurityContextHolder.clearContext();
        Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication(),
            "Security context should be cleared when requested");
        
        // Test context restoration
        setSecurityContext("TESTUSR", "ROLE_USER");
        Assertions.assertEquals("TESTUSR",
            SecurityContextHolder.getContext().getAuthentication().getName(),
            "New security context should be properly set");
    }

    /**
     * Parameterized test for different user types and their permissions.
     * Tests multiple scenarios with different user IDs and role combinations.
     */
    @ParameterizedTest
    @ValueSource(strings = {"ADMIN001", "ADMIN002", "TESTADM", "AUDIT001"})
    public void testAdminUserPermissions(String adminUserId) {
        setSecurityContext(adminUserId, "ROLE_ADMIN");
        
        Assertions.assertTrue(authorizationService.hasAdminRole(),
            "Admin user " + adminUserId + " should have admin role");
        
        Assertions.assertTrue(authorizationService.canAccessAllAccounts(),
            "Admin user " + adminUserId + " should access all accounts");
        
        Assertions.assertTrue(authorizationService.canManageUsers(),
            "Admin user " + adminUserId + " should manage users");
    }

    /**
     * Parameterized test for regular user permissions.
     * Validates consistent behavior across different regular users.
     */
    @ParameterizedTest
    @ValueSource(strings = {"USER001", "USER002", "USER003", "TESTUSR"})
    public void testRegularUserPermissions(String userId) {
        setSecurityContext(userId, "ROLE_USER");
        
        Assertions.assertTrue(authorizationService.hasUserRole(),
            "User " + userId + " should have user role");
        
        Assertions.assertFalse(authorizationService.hasAdminRole(),
            "User " + userId + " should not have admin role");
        
        Assertions.assertFalse(authorizationService.canManageUsers(),
            "User " + userId + " should not manage users");
        
        Assertions.assertFalse(authorizationService.canAccessAllAccounts(),
            "User " + userId + " should not access all accounts");
    }

    /**
     * Helper method to set up Spring Security context for testing.
     * Creates authentication token with specified user and role.
     * 
     * @param username The username for the security context
     * @param role The role to assign (e.g., "ROLE_ADMIN", "ROLE_USER")
     */
    private void setSecurityContext(String username, String role) {
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(
                username, 
                "password", 
                Arrays.asList(new SimpleGrantedAuthority(role))
            );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Test configuration for enabling method security in test context.
     */
    @Configuration
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestSecurityConfig {
        // Empty configuration class to enable method security
    }
}