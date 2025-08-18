package com.carddemo.security;

import com.carddemo.entity.UserSecurity;
import com.carddemo.repository.UserSecurityRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive ad-hoc unit tests for CustomUserDetailsService
 * Testing COBOL-to-Java authentication logic conversion and Spring Security integration
 */
class CustomUserDetailsServiceAdHocTest {

    @Mock
    private UserSecurityRepository userSecurityRepository;

    private CustomUserDetailsService customUserDetailsService;

    private UserSecurity testAdminUser;
    private UserSecurity testRegularUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        customUserDetailsService = new CustomUserDetailsService();
        
        // Use reflection to inject the mock repository
        try {
            java.lang.reflect.Field field = CustomUserDetailsService.class.getDeclaredField("userSecurityRepository");
            field.setAccessible(true);
            field.set(customUserDetailsService, userSecurityRepository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock repository", e);
        }

        // Create test users matching COBOL patterns
        testAdminUser = new UserSecurity();
        testAdminUser.setSecUsrId("ADMIN001");
        testAdminUser.setUsername("ADMIN001");
        testAdminUser.setPassword("$2a$10$encrypted.password.hash");
        testAdminUser.setFirstName("Test");
        testAdminUser.setLastName("Admin");
        testAdminUser.setUserType("A"); // Admin type from COBOL SEC-USR-TYPE
        testAdminUser.setEnabled(true);
        testAdminUser.setAccountNonExpired(true);
        testAdminUser.setAccountNonLocked(true);
        testAdminUser.setCredentialsNonExpired(true);

        testRegularUser = new UserSecurity();
        testRegularUser.setSecUsrId("USER0001");
        testRegularUser.setUsername("USER0001");
        testRegularUser.setPassword("$2a$10$encrypted.user.password");
        testRegularUser.setFirstName("Test");
        testRegularUser.setLastName("User");
        testRegularUser.setUserType("U"); // Regular user type from COBOL SEC-USR-TYPE
        testRegularUser.setEnabled(true);
        testRegularUser.setAccountNonExpired(true);
        testRegularUser.setAccountNonLocked(true);
        testRegularUser.setCredentialsNonExpired(true);
    }

    @Test
    @DisplayName("Test loadUserByUsername with valid admin user")
    void testLoadUserByUsername_ValidAdminUser() {
        // Given
        String inputUsername = "admin001"; // lowercase input
        String expectedUsername = "ADMIN001"; // should be converted to uppercase
        when(userSecurityRepository.findByUsername(expectedUsername))
            .thenReturn(Optional.of(testAdminUser));

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername(inputUsername);

        // Then
        assertNotNull(result, "UserDetails should not be null");
        assertEquals(expectedUsername, result.getUsername(), "Username should match expected value");
        assertEquals(testAdminUser.getPassword(), result.getPassword(), "Password should match");
        assertTrue(result.isEnabled(), "User should be enabled");
        assertTrue(result.isAccountNonExpired(), "Account should not be expired");
        assertTrue(result.isAccountNonLocked(), "Account should not be locked");
        assertTrue(result.isCredentialsNonExpired(), "Credentials should not be expired");

        // Verify COBOL SEC-USR-TYPE 'A' -> ROLE_ADMIN conversion
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertNotNull(authorities, "Authorities should not be null");
        assertEquals(1, authorities.size(), "Should have exactly one authority");
        assertTrue(authorities.stream()
                .anyMatch(auth -> SecurityConstants.ROLE_ADMIN.equals(auth.getAuthority())),
                "Admin user should have ROLE_ADMIN authority");

        // Verify uppercase conversion was applied (COBOL logic)
        verify(userSecurityRepository).findByUsername(expectedUsername);
        verify(userSecurityRepository, never()).findByUsername(inputUsername);
    }

    @Test
    @DisplayName("Test loadUserByUsername with valid regular user")
    void testLoadUserByUsername_ValidRegularUser() {
        // Given
        String inputUsername = "user0001"; // lowercase input
        String expectedUsername = "USER0001"; // should be converted to uppercase
        when(userSecurityRepository.findByUsername(expectedUsername))
            .thenReturn(Optional.of(testRegularUser));

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername(inputUsername);

        // Then
        assertNotNull(result, "UserDetails should not be null");
        assertEquals(expectedUsername, result.getUsername(), "Username should match expected value");
        assertEquals(testRegularUser.getPassword(), result.getPassword(), "Password should match");

        // Verify COBOL SEC-USR-TYPE 'U' -> ROLE_USER conversion
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertNotNull(authorities, "Authorities should not be null");
        assertEquals(1, authorities.size(), "Should have exactly one authority");
        assertTrue(authorities.stream()
                .anyMatch(auth -> SecurityConstants.ROLE_USER.equals(auth.getAuthority())),
                "Regular user should have ROLE_USER authority");

        // Verify uppercase conversion was applied (COBOL logic)
        verify(userSecurityRepository).findByUsername(expectedUsername);
    }

    @Test
    @DisplayName("Test loadUserByUsername with user not found")
    void testLoadUserByUsername_UserNotFound() {
        // Given
        String inputUsername = "nonexistent";
        String expectedUsername = "NONEXISTENT";
        when(userSecurityRepository.findByUsername(expectedUsername))
            .thenReturn(Optional.empty());

        // When & Then
        UsernameNotFoundException exception = assertThrows(
            UsernameNotFoundException.class,
            () -> customUserDetailsService.loadUserByUsername(inputUsername),
            "Should throw UsernameNotFoundException for non-existent user"
        );

        assertEquals("User not found. Try again ...", exception.getMessage(),
                "Exception message should match COBOL error message");

        // Verify uppercase conversion was applied even for non-existent users
        verify(userSecurityRepository).findByUsername(expectedUsername);
    }

    @Test
    @DisplayName("Test case-insensitive authentication matching COBOL logic")
    void testCaseInsensitiveAuthentication() {
        // Given - various case combinations
        String[] inputVariations = {"admin001", "ADMIN001", "Admin001", "aDmIn001"};
        String expectedUsername = "ADMIN001";
        when(userSecurityRepository.findByUsername(expectedUsername))
            .thenReturn(Optional.of(testAdminUser));

        // When & Then - all variations should result in uppercase conversion
        for (String input : inputVariations) {
            UserDetails result = customUserDetailsService.loadUserByUsername(input);
            assertNotNull(result, "UserDetails should not be null for input: " + input);
            assertEquals(expectedUsername, result.getUsername(),
                    "Username should be uppercase for input: " + input);
        }

        // Verify all calls were made with uppercase username
        verify(userSecurityRepository, times(inputVariations.length))
            .findByUsername(expectedUsername);
    }

    @Test
    @DisplayName("Test null username handling")
    void testNullUsernameHandling() {
        // When & Then
        assertThrows(NullPointerException.class,
            () -> customUserDetailsService.loadUserByUsername(null),
            "Should throw exception for null username");
    }

    @Test
    @DisplayName("Test empty username handling")
    void testEmptyUsernameHandling() {
        // Given
        String emptyUsername = "";
        when(userSecurityRepository.findByUsername(""))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class,
            () -> customUserDetailsService.loadUserByUsername(emptyUsername),
            "Should throw UsernameNotFoundException for empty username");
    }

    @Test
    @DisplayName("Test repository integration")
    void testRepositoryIntegration() {
        // Given
        String username = "testuser";
        String expectedUsername = "TESTUSER";
        when(userSecurityRepository.findByUsername(expectedUsername))
            .thenReturn(Optional.of(testRegularUser));

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername(username);

        // Then
        assertNotNull(result, "Result should not be null");
        verify(userSecurityRepository, times(1)).findByUsername(expectedUsername);
        verifyNoMoreInteractions(userSecurityRepository);
    }

    @Test
    @DisplayName("Test disabled user handling")
    void testDisabledUserHandling() {
        // Given
        testRegularUser.setEnabled(false);
        String username = "user0001";
        String expectedUsername = "USER0001";
        when(userSecurityRepository.findByUsername(expectedUsername))
            .thenReturn(Optional.of(testRegularUser));

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername(username);

        // Then
        assertNotNull(result, "UserDetails should not be null");
        assertFalse(result.isEnabled(), "User should be disabled");
    }

    @Test
    @DisplayName("Test locked user handling")
    void testLockedUserHandling() {
        // Given
        testRegularUser.setAccountNonLocked(false);
        String username = "user0001";
        String expectedUsername = "USER0001";
        when(userSecurityRepository.findByUsername(expectedUsername))
            .thenReturn(Optional.of(testRegularUser));

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername(username);

        // Then
        assertNotNull(result, "UserDetails should not be null");
        assertFalse(result.isAccountNonLocked(), "Account should be locked");
    }

    @Test
    @DisplayName("Test expired user handling")
    void testExpiredUserHandling() {
        // Given
        testRegularUser.setAccountNonExpired(false);
        testRegularUser.setCredentialsNonExpired(false);
        String username = "user0001";
        String expectedUsername = "USER0001";
        when(userSecurityRepository.findByUsername(expectedUsername))
            .thenReturn(Optional.of(testRegularUser));

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername(username);

        // Then
        assertNotNull(result, "UserDetails should not be null");
        assertFalse(result.isAccountNonExpired(), "Account should be expired");
        assertFalse(result.isCredentialsNonExpired(), "Credentials should be expired");
    }

    @Test
    @DisplayName("Test COBOL user type validation")
    void testCobolUserTypeValidation() {
        // Test invalid user type handling
        UserSecurity invalidTypeUser = new UserSecurity();
        invalidTypeUser.setSecUsrId("INVALID1");
        invalidTypeUser.setUsername("INVALID1");
        invalidTypeUser.setPassword("$2a$10$test.hash");
        invalidTypeUser.setFirstName("Invalid");
        invalidTypeUser.setLastName("User");
        invalidTypeUser.setUserType("X"); // Invalid type
        invalidTypeUser.setEnabled(true);
        invalidTypeUser.setAccountNonExpired(true);
        invalidTypeUser.setAccountNonLocked(true);
        invalidTypeUser.setCredentialsNonExpired(true);

        String username = "invalid1";
        String expectedUsername = "INVALID1";
        when(userSecurityRepository.findByUsername(expectedUsername))
            .thenReturn(Optional.of(invalidTypeUser));

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername(username);

        // Then - should default to ROLE_USER for invalid types
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertTrue(authorities.stream()
                .anyMatch(auth -> SecurityConstants.ROLE_USER.equals(auth.getAuthority())),
                "Invalid user type should default to ROLE_USER");
    }

    @Test
    @DisplayName("Test COBOL field constraints integration")
    void testCobolFieldConstraints() {
        // Given - user with COBOL-compliant field lengths
        String username = "12345678"; // 8 characters max (COBOL SEC-USR-ID)
        String expectedUsername = "12345678";
        
        UserSecurity cobolConstraintUser = new UserSecurity();
        cobolConstraintUser.setSecUsrId("12345678");
        cobolConstraintUser.setUsername("12345678");
        cobolConstraintUser.setPassword("$2a$10$test.hash");
        cobolConstraintUser.setFirstName("Test");
        cobolConstraintUser.setLastName("COBOL");
        cobolConstraintUser.setUserType("A");
        cobolConstraintUser.setEnabled(true);
        cobolConstraintUser.setAccountNonExpired(true);
        cobolConstraintUser.setAccountNonLocked(true);
        cobolConstraintUser.setCredentialsNonExpired(true);
        
        when(userSecurityRepository.findByUsername(expectedUsername))
            .thenReturn(Optional.of(cobolConstraintUser));

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername(username);

        // Then
        assertNotNull(result, "Should handle COBOL-compliant field lengths");
        assertEquals(expectedUsername, result.getUsername());
    }

    @Test
    @DisplayName("Test Spring Security integration compatibility")
    void testSpringSecurityIntegration() {
        // Given
        String username = "springtest";
        String expectedUsername = "SPRINGTEST";
        when(userSecurityRepository.findByUsername(expectedUsername))
            .thenReturn(Optional.of(testAdminUser));

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername(username);

        // Then - verify all UserDetails interface methods work
        assertNotNull(result.getUsername(), "getUsername() should work");
        assertNotNull(result.getPassword(), "getPassword() should work");
        assertNotNull(result.getAuthorities(), "getAuthorities() should work");
        assertNotNull(result.isEnabled(), "isEnabled() should work");
        assertNotNull(result.isAccountNonExpired(), "isAccountNonExpired() should work");
        assertNotNull(result.isAccountNonLocked(), "isAccountNonLocked() should work");
        assertNotNull(result.isCredentialsNonExpired(), "isCredentialsNonExpired() should work");
    }
}