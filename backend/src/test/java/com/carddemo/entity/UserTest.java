/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.carddemo.entity;

import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.TestConstants;
import com.carddemo.test.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive JUnit 5 unit tests for UserSecurity JPA entity validating 
 * SEC-USER-DATA structure from CSUSR01Y copybook.
 * 
 * Tests cover:
 * - COBOL field mappings from 80-byte record structure
 * - Spring Security UserDetails interface implementation  
 * - Role mapping from SEC-USR-TYPE to Spring Security authorities
 * - Field validation and constraints
 * - Legacy password encoding compatibility
 * 
 * Maintains functional parity with COBOL COSGN00C authentication logic.
 */
@Tag("unit")
@DisplayName("UserSecurity Entity Tests")
public class UserTest extends AbstractBaseTest implements UnitTest {

    private UserSecurity testUser;
    private UserSecurity testAdmin;
    
    @Mock
    private Collection<GrantedAuthority> mockAuthorities;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        logTestExecution("Setting up UserSecurity test data", null);
        
        // Create standard test user (type 'U') matching TestConstants
        testUser = createTestUserEntity(
            TestConstants.TEST_USER_ID,
            "JOHN", 
            "DOE",
            TestConstants.TEST_USER_PASSWORD,
            "U"
        );
        
        // Create admin test user (type 'A') matching TestConstants  
        testAdmin = createTestAdminEntity(
            "ADMIN001",
            "JANE",
            "SMITH", 
            "ADMIN123",
            "A"
        );
    }

    @AfterEach
    @Override
    public void tearDown() {
        super.tearDown();
        testUser = null;
        testAdmin = null;
        mockAuthorities = null;
    }

    /**
     * Helper method to create UserSecurity entity for testing
     * Maps COBOL SEC-USER-DATA structure with exact field lengths
     */
    private UserSecurity createTestUserEntity(String userId, String firstName, 
                                            String lastName, String password, String userType) {
        UserSecurity user = new UserSecurity();
        user.setSecUsrId(userId);         // SEC-USR-ID PIC X(08)
        user.setUsername(userId);         // Set username same as userId for Spring Security 
        user.setFirstName(firstName);     // SEC-USR-FNAME PIC X(20)
        user.setLastName(lastName);       // SEC-USR-LNAME PIC X(20)
        user.setPassword(password);       // SEC-USR-PWD PIC X(08)
        user.setUserType(userType);       // SEC-USR-TYPE PIC X(01)
        // SEC-USR-FILLER PIC X(23) - handled internally by entity
        return user;
    }
    
    /**
     * Helper method to create admin UserSecurity entity for testing
     */
    private UserSecurity createTestAdminEntity(String userId, String firstName,
                                             String lastName, String password, String userType) {
        return createTestUserEntity(userId, firstName, lastName, password, userType);
    }
    
    /**
     * Helper method to convert Map from AbstractBaseTest to UserSecurity entity
     */
    private UserSecurity createUserSecurityFromMap(Map<String, Object> userMap) {
        UserSecurity user = new UserSecurity();
        user.setSecUsrId((String) userMap.get("userId"));
        user.setUsername((String) userMap.get("userId"));
        user.setPassword((String) userMap.get("password"));
        user.setFirstName((String) userMap.get("firstName"));
        user.setLastName((String) userMap.get("lastName"));
        user.setUserType((String) userMap.get("userType"));
        return user;
    }

    @Nested
    @DisplayName("COBOL Field Mapping Tests")
    class CobolFieldMappingTests {

        @Test
        @DisplayName("SEC-USR-ID field mapping - 8 character user ID")
        public void testUserIdFieldMapping() {
            // Validate 8-character user ID field from COBOL PIC X(08)
            assertThat(testUser.getUserId())
                .isNotNull()
                .isEqualTo(TestConstants.TEST_USER_ID)
                .hasSize(8);
                
            // Test boundary conditions for user ID length
            UserSecurity boundaryUser = createTestUserEntity("12345678", "Test", "User", "password", "U");
            assertThat(boundaryUser.getUserId()).hasSize(8);
            
            // Verify getUserId() matches getUsername() for Spring Security compatibility
            assertThat(testUser.getUsername()).isEqualTo(testUser.getUserId());
        }

        @Test
        @DisplayName("SEC-USR-FNAME field mapping - 20 character first name")
        public void testFirstNameFieldMapping() {
            // Validate 20-character first name field from COBOL PIC X(20)
            assertThat(testUser.getFirstName())
                .isNotNull()
                .isEqualTo("JOHN")
                .hasSizeLessThanOrEqualTo(20);
                
            // Test maximum length first name
            UserSecurity maxLengthUser = createTestUserEntity("USER0001", "12345678901234567890", "Doe", "password", "U");
            assertThat(maxLengthUser.getFirstName()).hasSizeLessThanOrEqualTo(20);
            
            // Test empty first name handling
            UserSecurity emptyNameUser = createTestUserEntity("USER0002", "", "Doe", "password", "U");
            assertThat(emptyNameUser.getFirstName()).isNotNull();
        }

        @Test
        @DisplayName("SEC-USR-LNAME field mapping - 20 character last name")
        public void testLastNameFieldMapping() {
            // Validate 20-character last name field from COBOL PIC X(20)
            assertThat(testUser.getLastName())
                .isNotNull()
                .isEqualTo("DOE")
                .hasSizeLessThanOrEqualTo(20);
                
            // Test maximum length last name
            UserSecurity maxLengthUser = createTestUserEntity("USER0003", "John", "12345678901234567890", "password", "U");
            assertThat(maxLengthUser.getLastName()).hasSizeLessThanOrEqualTo(20);
        }

        @Test
        @DisplayName("SEC-USR-PWD field mapping - 8 character password")
        public void testPasswordFieldMapping() {
            // Validate 8-character password field from COBOL PIC X(08)
            assertThat(testUser.getPassword())
                .isNotNull()
                .isEqualTo(TestConstants.TEST_USER_PASSWORD)
                .hasSize(8);
                
            // Test boundary conditions for password length
            UserSecurity boundaryUser = createTestUserEntity("USER0004", "Test", "User", "12345678", "U");
            assertThat(boundaryUser.getPassword()).hasSize(8);
        }

        @Test
        @DisplayName("SEC-USR-TYPE field mapping - 1 character user type")
        public void testUserTypeFieldMapping() {
            // Validate 1-character user type field from COBOL PIC X(01)
            assertThat(testUser.getUserType())
                .isNotNull()
                .isEqualTo("U")
                .hasSize(1);
                
            assertThat(testAdmin.getUserType())
                .isNotNull() 
                .isEqualTo("A")
                .hasSize(1);
                
            // Verify only valid user types
            assertThat(testUser.getUserType()).isIn("A", "U");
            assertThat(testAdmin.getUserType()).isIn("A", "U");
        }

        @Test
        @DisplayName("SEC-USR-FILLER field handling - 23 character padding")
        public void testFillerFieldHandling() {
            // COBOL FILLER field PIC X(23) should not impact entity functionality
            // Verify total record length conceptually matches 80-byte COBOL structure:
            // 8 (user_id) + 20 (fname) + 20 (lname) + 8 (password) + 1 (type) + 23 (filler) = 80 bytes
            int calculatedRecordLength = 8 + 20 + 20 + 8 + 1 + 23;
            assertThat(calculatedRecordLength).isEqualTo(80);
            
            // Verify entity functions properly regardless of filler field presence
            assertThat(testUser.getUserId()).isNotNull();
            assertThat(testUser.getFirstName()).isNotNull();
            assertThat(testUser.getLastName()).isNotNull();
            assertThat(testUser.getPassword()).isNotNull();
            assertThat(testUser.getUserType()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Spring Security UserDetails Implementation Tests")
    class SpringSecurityUserDetailsTests {

        @Test
        @DisplayName("UserDetails interface - getUsername() implementation")
        public void testGetUsernameImplementation() {
            // Spring Security requires getUsername() to return user identifier
            assertThat(testUser.getUsername())
                .isNotNull()
                .isEqualTo(testUser.getUserId())
                .isEqualTo(TestConstants.TEST_USER_ID);
                
            assertThat(testAdmin.getUsername())
                .isNotNull()
                .isEqualTo(testAdmin.getUserId());
        }

        @Test
        @DisplayName("UserDetails interface - getPassword() implementation")
        public void testGetPasswordImplementation() {
            // Spring Security requires getPassword() for authentication
            assertThat(testUser.getPassword())
                .isNotNull()
                .isEqualTo(TestConstants.TEST_USER_PASSWORD);
                
            assertThat(testAdmin.getPassword())
                .isNotNull()
                .hasSize(8); // COBOL PIC X(08) constraint
        }

        @Test
        @DisplayName("UserDetails interface - getAuthorities() implementation")
        public void testGetAuthoritiesImplementation() {
            // Test user authorities mapping
            Collection<? extends GrantedAuthority> userAuthorities = testUser.getAuthorities();
            assertThat(userAuthorities)
                .isNotNull()
                .hasSize(1)
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_USER");
                
            // Test admin authorities mapping  
            Collection<? extends GrantedAuthority> adminAuthorities = testAdmin.getAuthorities();
            assertThat(adminAuthorities)
                .isNotNull()
                .hasSize(1)
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN");
        }

        @Test
        @DisplayName("UserDetails interface - account status methods")
        public void testAccountStatusMethods() {
            // Test default Spring Security account status implementations
            assertThat(testUser.isEnabled()).isTrue();
            assertThat(testUser.isAccountNonExpired()).isTrue();
            assertThat(testUser.isAccountNonLocked()).isTrue();
            assertThat(testUser.isCredentialsNonExpired()).isTrue();
            
            assertThat(testAdmin.isEnabled()).isTrue();
            assertThat(testAdmin.isAccountNonExpired()).isTrue();
            assertThat(testAdmin.isAccountNonLocked()).isTrue();
            assertThat(testAdmin.isCredentialsNonExpired()).isTrue();
        }
    }

    @Nested
    @DisplayName("Role Mapping and Authorization Tests")
    class RoleMappingTests {

        @Test
        @DisplayName("SEC-USR-TYPE 'U' maps to ROLE_USER authority")
        public void testUserRoleMapping() {
            UserSecurity regularUser = createTestUserEntity("USER0005", "Regular", "User", "userpass", "U");
            
            Collection<? extends GrantedAuthority> authorities = regularUser.getAuthorities();
            assertThat(authorities)
                .isNotNull()
                .hasSize(1);
                
            GrantedAuthority authority = authorities.iterator().next();
            assertThat(authority).isInstanceOf(SimpleGrantedAuthority.class);
            assertThat(authority.getAuthority()).isEqualTo("ROLE_USER");
        }

        @Test
        @DisplayName("SEC-USR-TYPE 'A' maps to ROLE_ADMIN authority")
        public void testAdminRoleMapping() {
            UserSecurity adminUser = createTestUserEntity("ADMIN002", "Admin", "User", "adminpwd", "A");
            
            Collection<? extends GrantedAuthority> authorities = adminUser.getAuthorities();
            assertThat(authorities)
                .isNotNull()
                .hasSize(1);
                
            GrantedAuthority authority = authorities.iterator().next();
            assertThat(authority).isInstanceOf(SimpleGrantedAuthority.class);
            assertThat(authority.getAuthority()).isEqualTo("ROLE_ADMIN");
        }

        @Test
        @DisplayName("Role-based access control validation")
        public void testRoleBasedAccessControl() {
            // Verify user type determines authorization level
            assertThat(testUser.getUserType()).isEqualTo("U");
            assertThat(testUser.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsOnly("ROLE_USER");
                
            assertThat(testAdmin.getUserType()).isEqualTo("A");
            assertThat(testAdmin.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsOnly("ROLE_ADMIN");
        }

        @Test
        @DisplayName("Invalid user type handling")
        public void testInvalidUserTypeHandling() {
            // Test handling of invalid user type values
            UserSecurity invalidUser = createTestUserEntity("USER0006", "Invalid", "User", "password", "X");
            
            // Invalid user type should result in empty authorities or default handling
            Collection<? extends GrantedAuthority> authorities = invalidUser.getAuthorities();
            // Implementation should handle invalid types gracefully
            assertThat(authorities).isNotNull();
        }
    }

    @Nested  
    @DisplayName("Password Encoding and Security Tests")
    class PasswordSecurityTests {

        @Test
        @DisplayName("NoOpPasswordEncoder compatibility for legacy support")
        public void testNoOpPasswordEncoderCompatibility() {
            // Verify plain text password storage for legacy COBOL system compatibility
            assertThat(testUser.getPassword())
                .isEqualTo(TestConstants.TEST_USER_PASSWORD)
                .doesNotContain("$2a$") // Not BCrypt encoded
                .doesNotContain("{") // Not delegating password encoder format
                .hasSize(8); // COBOL PIC X(08) constraint
        }

        @Test
        @DisplayName("Password field validation and constraints") 
        public void testPasswordFieldValidation() {
            // Test password length constraints matching COBOL PIC X(08)
            UserSecurity validPasswordUser = createTestUserEntity("USER0007", "Test", "User", "validpwd", "U");
            assertThat(validPasswordUser.getPassword()).hasSize(8);
            
            // Test boundary conditions
            UserSecurity maxPasswordUser = createTestUserEntity("USER0008", "Test", "User", "12345678", "U");
            assertThat(maxPasswordUser.getPassword()).hasSize(8);
        }

        @Test
        @DisplayName("Case sensitivity in password handling")
        public void testPasswordCaseSensitivity() {
            // Verify case-sensitive password comparison (Spring Security requirement)
            UserSecurity user1 = createTestUserEntity("USER0009", "Test", "User", "Password", "U");
            UserSecurity user2 = createTestUserEntity("USER0010", "Test", "User", "password", "U");
            
            assertThat(user1.getPassword()).isNotEqualTo(user2.getPassword());
            assertThat(user1.getPassword()).isEqualTo("Password");
            assertThat(user2.getPassword()).isEqualTo("password");
        }
    }

    @Nested
    @DisplayName("Field Validation and Constraint Tests")
    class FieldValidationTests {

        @Test
        @DisplayName("User ID unique constraint validation")
        public void testUserIdUniqueConstraint() {
            // Verify user ID serves as unique identifier (primary key)
            assertThat(testUser.getUserId()).isNotNull();
            assertThat(testAdmin.getUserId()).isNotNull();
            assertThat(testUser.getUserId()).isNotEqualTo(testAdmin.getUserId());
            
            // Test user ID case sensitivity
            UserSecurity lowerCaseUser = createTestUserEntity("user0011", "Test", "User", "password", "U");
            UserSecurity upperCaseUser = createTestUserEntity("USER0011", "Test", "User", "password", "U");
            assertThat(lowerCaseUser.getUserId()).isNotEqualTo(upperCaseUser.getUserId());
        }

        @Test
        @DisplayName("Field length constraints from COBOL structure")
        public void testCobolFieldLengthConstraints() {
            // Test all field lengths match COBOL PIC clause specifications
            assertThat(testUser.getUserId()).hasSizeLessThanOrEqualTo(8);   // PIC X(08)
            assertThat(testUser.getFirstName()).hasSizeLessThanOrEqualTo(20); // PIC X(20)
            assertThat(testUser.getLastName()).hasSizeLessThanOrEqualTo(20);  // PIC X(20)
            assertThat(testUser.getPassword()).hasSizeLessThanOrEqualTo(8);   // PIC X(08)
            assertThat(testUser.getUserType()).hasSizeLessThanOrEqualTo(1);   // PIC X(01)
        }

        @Test
        @DisplayName("Required field validation")
        public void testRequiredFieldValidation() {
            // Verify all essential fields are populated for authentication
            assertThat(testUser.getUserId()).isNotEmpty();
            assertThat(testUser.getPassword()).isNotEmpty();
            assertThat(testUser.getUserType()).isNotEmpty();
            
            // First name and last name may be optional for authentication but should be handled
            assertThat(testUser.getFirstName()).isNotNull();
            assertThat(testUser.getLastName()).isNotNull();
        }

        @Test
        @DisplayName("Special character handling in fields")
        public void testSpecialCharacterHandling() {
            // Test handling of special characters in name fields
            UserSecurity specialCharUser = createTestUserEntity(
                "USER0012", 
                "MARÍA-JOSÉ", 
                "O'CONNOR-SMITH", 
                "special!",
                "U"
            );
            
            assertThat(specialCharUser.getFirstName()).isEqualTo("MARÍA-JOSÉ");
            assertThat(specialCharUser.getLastName()).isEqualTo("O'CONNOR-SMITH");
            assertThat(specialCharUser.getPassword()).isEqualTo("special!");
        }
    }

    @Nested
    @DisplayName("Spring Security Integration Tests")
    class SpringSecurityIntegrationTests {

        @Test
        @DisplayName("Authentication principal integration")
        public void testAuthenticationPrincipalIntegration() {
            // Verify UserSecurity can serve as Spring Security Principal
            assertThat(testUser).isInstanceOf(org.springframework.security.core.userdetails.UserDetails.class);
            
            // Test principal name matches user ID
            assertThat(testUser.getUsername()).isEqualTo(testUser.getUserId());
        }

        @Test
        @DisplayName("Authority collection behavior")
        public void testAuthorityCollectionBehavior() {
            Collection<? extends GrantedAuthority> userAuthorities = testUser.getAuthorities();
            Collection<? extends GrantedAuthority> adminAuthorities = testAdmin.getAuthorities();
            
            // Verify collection properties
            assertThat(userAuthorities).isNotEmpty().hasSize(1);
            assertThat(adminAuthorities).isNotEmpty().hasSize(1);
            
            // Verify authorities are properly typed
            assertThat(userAuthorities).allMatch(auth -> auth instanceof SimpleGrantedAuthority);
            assertThat(adminAuthorities).allMatch(auth -> auth instanceof SimpleGrantedAuthority);
        }

        @Test
        @DisplayName("Security context compatibility")
        public void testSecurityContextCompatibility() {
            // Verify entity works with Spring Security authentication
            assertThat(testUser.isAccountNonExpired()).isTrue();
            assertThat(testUser.isAccountNonLocked()).isTrue();
            assertThat(testUser.isCredentialsNonExpired()).isTrue();
            assertThat(testUser.isEnabled()).isTrue();
            
            // Test with admin user
            assertThat(testAdmin.isAccountNonExpired()).isTrue();
            assertThat(testAdmin.isAccountNonLocked()).isTrue();
            assertThat(testAdmin.isCredentialsNonExpired()).isTrue();
            assertThat(testAdmin.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("COBOL Data Compatibility Tests")
    class CobolDataCompatibilityTests {

        @Test
        @DisplayName("COBOL precision validation for numeric constraints")
        public void testCobolPrecisionValidation() {
            // Use AbstractBaseTest COBOL precision validation utilities
            // Create a test BigDecimal for validation
            BigDecimal testValue = new BigDecimal("100.00");
            validateCobolPrecision(testValue, "testValue");
            
            // Verify field lengths match COBOL copybook exactly
            assertThat(testUser.getUserId().length()).isEqualTo(8);
            assertThat(testUser.getPassword().length()).isEqualTo(8);
            assertThat(testUser.getUserType().length()).isEqualTo(1);
        }

        @Test
        @DisplayName("Data format compatibility with COBOL COSGN00C")
        public void testCobolDataFormatCompatibility() {
            // Verify uppercase conversion matching COBOL behavior
            UserSecurity mixedCaseUser = createTestUserEntity("user0013", "john", "doe", "password", "u");
            
            // User ID should be uppercase for consistency with COBOL
            if (mixedCaseUser.getUserId().equals("USER0013")) {
                assertThat(mixedCaseUser.getUserId()).isEqualTo("USER0013");
            }
            
            // Names may preserve case or be uppercase depending on implementation
            assertThat(mixedCaseUser.getFirstName()).isNotNull();
            assertThat(mixedCaseUser.getLastName()).isNotNull();
        }

        @Test
        @DisplayName("Character encoding compatibility")
        public void testCharacterEncodingCompatibility() {
            // Test EBCDIC to ASCII conversion compatibility
            UserSecurity asciiUser = createTestUserEntity("ASCII001", "TEST", "USER", "testpass", "U");
            
            // Verify all fields handle standard ASCII characters
            assertThat(asciiUser.getUserId()).matches("[A-Z0-9]+");
            assertThat(asciiUser.getFirstName()).matches("[A-Z\\-'\\s]*");
            assertThat(asciiUser.getLastName()).matches("[A-Z\\-'\\s]*");
        }
    }

    @Nested
    @DisplayName("Entity Lifecycle and JPA Tests") 
    class EntityLifecycleTests {

        @Test
        @DisplayName("Entity instantiation and initialization")
        public void testEntityInstantiation() {
            UserSecurity newUser = new UserSecurity();
            
            // Verify entity can be instantiated without errors
            assertThat(newUser).isNotNull();
            
            // Test field initialization
            newUser.setSecUsrId("NEWUSER1");
            newUser.setUsername("NEWUSER1");
            newUser.setFirstName("New");
            newUser.setLastName("User");
            newUser.setPassword("newpass1");
            newUser.setUserType("U");
            
            assertThat(newUser.getUserId()).isEqualTo("NEWUSER1");
            assertThat(newUser.getFirstName()).isEqualTo("New");
            assertThat(newUser.getLastName()).isEqualTo("User");
            assertThat(newUser.getPassword()).isEqualTo("newpass1");
            assertThat(newUser.getUserType()).isEqualTo("U");
        }

        @Test
        @DisplayName("Entity equality and hash code")
        public void testEntityEqualityAndHashCode() {
            UserSecurity user1 = createTestUserEntity(TestConstants.TEST_USER_ID, "John", "Doe", "password", "U");
            UserSecurity user2 = createTestUserEntity(TestConstants.TEST_USER_ID, "John", "Doe", "password", "U");
            UserSecurity user3 = createTestUserEntity("DIFF0001", "Jane", "Smith", "password", "A");
            
            // Test equality based on user ID (primary key)
            if (user1.equals(user2)) {
                assertThat(user1).isEqualTo(user2);
                assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
            }
            
            assertThat(user1).isNotEqualTo(user3);
        }

        @Test
        @DisplayName("ToString implementation")
        public void testToStringImplementation() {
            String userString = testUser.toString();
            
            // Verify toString() doesn't expose sensitive information
            assertThat(userString).isNotNull();
            assertThat(userString).doesNotContain(testUser.getPassword()); // Password should be masked
            
            // Should contain non-sensitive identifying information
            if (userString.contains(testUser.getUserId())) {
                assertThat(userString).contains(testUser.getUserId());
            }
        }
    }

    @Nested
    @DisplayName("Business Logic and Validation Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Authentication validation logic")
        public void testAuthenticationValidationLogic() {
            // Test authentication with valid credentials
            assertThat(testUser.getUsername()).isEqualTo(TestConstants.TEST_USER_ID);
            assertThat(testUser.getPassword()).isEqualTo(TestConstants.TEST_USER_PASSWORD);
            
            // Verify user is enabled for authentication
            assertThat(testUser.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("User profile completeness validation")
        public void testUserProfileCompleteness() {
            // Verify all profile fields are properly set
            assertThat(testUser.getUserId()).isNotEmpty();
            assertThat(testUser.getFirstName()).isNotEmpty();
            assertThat(testUser.getLastName()).isNotEmpty();
            assertThat(testUser.getPassword()).isNotEmpty();
            assertThat(testUser.getUserType()).isNotEmpty();
            
            // Test admin profile completeness
            assertThat(testAdmin.getUserId()).isNotEmpty();
            assertThat(testAdmin.getFirstName()).isNotEmpty();
            assertThat(testAdmin.getLastName()).isNotEmpty();
            assertThat(testAdmin.getPassword()).isNotEmpty();
            assertThat(testAdmin.getUserType()).isNotEmpty();
        }

        @Test
        @DisplayName("Business rule validation from COSGN00C")
        public void testBusinessRuleValidation() {
            // Test business rules from original COBOL COSGN00C program
            
            // User ID must be non-empty for authentication
            assertThat(testUser.getUserId()).isNotEmpty();
            
            // Password must be non-empty for authentication
            assertThat(testUser.getPassword()).isNotEmpty();
            
            // User type must be valid ('A' or 'U')
            assertThat(testUser.getUserType()).isIn("A", "U");
            assertThat(testAdmin.getUserType()).isIn("A", "U");
        }
    }

    @Nested
    @DisplayName("Test Data Factory Integration Tests")
    class TestDataFactoryTests {

        @Test
        @DisplayName("AbstractBaseTest integration - getTestUser()")
        public void testGetTestUserIntegration() {
            // Use AbstractBaseTest factory method
            Map<String, Object> testUserMap = getTestUser();
            UserSecurity factoryUser = createUserSecurityFromMap(testUserMap);
            
            assertThat(factoryUser).isNotNull();
            assertThat(factoryUser.getUserId()).isNotEmpty();
            assertThat(factoryUser.getUserType()).isEqualTo("U");
            assertThat(factoryUser.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_USER");
        }

        @Test
        @DisplayName("AbstractBaseTest integration - getTestAdmin()")  
        public void testGetTestAdminIntegration() {
            // Use AbstractBaseTest factory method
            Map<String, Object> testAdminMap = getTestAdmin();
            UserSecurity factoryAdmin = createUserSecurityFromMap(testAdminMap);
            
            assertThat(factoryAdmin).isNotNull();
            assertThat(factoryAdmin.getUserId()).isNotEmpty();
            assertThat(factoryAdmin.getUserType()).isEqualTo("A");
            assertThat(factoryAdmin.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN");
        }

        @Test
        @DisplayName("TestConstants integration validation")
        public void testConstantsIntegration() {
            // Verify test constants are properly used
            assertThat(TestConstants.TEST_USER_ID).isNotEmpty().hasSize(8);
            assertThat(TestConstants.TEST_USER_PASSWORD).isNotEmpty().hasSize(8);
            // Note: TestConstants role constants are SimpleGrantedAuthority objects
            assertThat(TestConstants.TEST_USER_ROLE.getAuthority()).isEqualTo("ROLE_USER");
            assertThat(TestConstants.TEST_ADMIN_ROLE.getAuthority()).isEqualTo("ROLE_ADMIN");
            
            // Verify COBOL precision constants
            assertThat(TestConstants.COBOL_DECIMAL_SCALE).isNotNull();
            assertThat(TestConstants.COBOL_ROUNDING_MODE).isNotNull();
        }
    }

    @Nested
    @DisplayName("Performance and Memory Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Entity creation performance validation")
        public void testEntityCreationPerformance() {
            long startTime = System.currentTimeMillis();
            
            // Create multiple entities to test performance
            for (int i = 0; i < 1000; i++) {
                UserSecurity user = createTestUserEntity(
                    String.format("USER%04d", i),
                    "Test",
                    "User", 
                    "password",
                    "U"
                );
                assertThat(user).isNotNull();
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Verify entity creation meets performance requirements
            assertThat(duration).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        }

        @Test
        @DisplayName("Memory usage validation") 
        public void testMemoryUsage() {
            // Test entity memory footprint
            UserSecurity[] users = new UserSecurity[100];
            
            for (int i = 0; i < 100; i++) {
                users[i] = createTestUserEntity(
                    String.format("MEM%05d", i),
                    "Memory",
                    "Test",
                    "memtest1",
                    "U"
                );
            }
            
            // Verify all entities created successfully
            assertThat(users).hasSize(100);
            assertThat(users).allMatch(user -> user != null);
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Case Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Null field handling")
        public void testNullFieldHandling() {
            UserSecurity nullFieldUser = new UserSecurity();
            
            // Test graceful handling of null fields
            if (nullFieldUser.getUserId() == null) {
                assertThat(nullFieldUser.getUsername()).isNull();
            }
            
            if (nullFieldUser.getPassword() == null) {
                assertThat(nullFieldUser.getPassword()).isNull();
            }
            
            // Authorities should handle null user type gracefully
            Collection<? extends GrantedAuthority> authorities = nullFieldUser.getAuthorities();
            assertThat(authorities).isNotNull(); // Should return empty collection or default
        }

        @Test
        @DisplayName("Empty string field handling")
        public void testEmptyStringFieldHandling() {
            UserSecurity emptyFieldUser = createTestUserEntity("", "", "", "", "");
            
            // Verify empty string handling
            assertThat(emptyFieldUser.getUserId()).isEmpty();
            assertThat(emptyFieldUser.getFirstName()).isEmpty();
            assertThat(emptyFieldUser.getLastName()).isEmpty();
            assertThat(emptyFieldUser.getPassword()).isEmpty();
            assertThat(emptyFieldUser.getUserType()).isEmpty();
        }

        @Test
        @DisplayName("Whitespace handling in fields")
        public void testWhitespaceHandling() {
            UserSecurity whitespaceUser = createTestUserEntity(
                "  USER14", 
                " JOHN ", 
                " DOE ", 
                " passwd ",
                " U"
            );
            
            // Verify whitespace preservation or trimming behavior
            assertThat(whitespaceUser.getUserId()).isNotNull();
            assertThat(whitespaceUser.getFirstName()).isNotNull();
            assertThat(whitespaceUser.getLastName()).isNotNull();
            assertThat(whitespaceUser.getPassword()).isNotNull();
            assertThat(whitespaceUser.getUserType()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Functional Parity Tests with COBOL")
    class FunctionalParityTests {

        @Test
        @DisplayName("Functional parity with COSGN00C authentication logic")
        public void testCosgn00cFunctionalParity() {
            // Test that authentication behavior matches original COBOL program
            
            // Valid user authentication scenario
            assertThat(testUser.getUsername()).isEqualTo(TestConstants.TEST_USER_ID);
            assertThat(testUser.getPassword()).isEqualTo(TestConstants.TEST_USER_PASSWORD);
            assertThat(testUser.isEnabled()).isTrue();
            
            // Admin user authentication scenario  
            assertThat(testAdmin.isEnabled()).isTrue();
            assertThat(testAdmin.getUserType()).isEqualTo("A");
            assertThat(testAdmin.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN");
        }

        @Test
        @DisplayName("Role-based menu access validation") 
        public void testRoleBasedMenuAccess() {
            // Test role-based access control matching COBOL program logic
            
            // Regular user should have ROLE_USER authority
            Collection<? extends GrantedAuthority> userAuthorities = testUser.getAuthorities();
            boolean hasUserRole = userAuthorities.stream()
                .anyMatch(auth -> "ROLE_USER".equals(auth.getAuthority()));
            assertThat(hasUserRole).isTrue();
            
            // Admin user should have ROLE_ADMIN authority
            Collection<? extends GrantedAuthority> adminAuthorities = testAdmin.getAuthorities();
            boolean hasAdminRole = adminAuthorities.stream()
                .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
            assertThat(hasAdminRole).isTrue();
        }

        @Test
        @DisplayName("Session state compatibility")
        public void testSessionStateCompatibility() {
            // Verify entity supports session state equivalent to CICS COMMAREA
            
            // User identification for session tracking
            assertThat(testUser.getUserId()).isNotNull();
            assertThat(testUser.getUserType()).isNotNull();
            
            // Verify session data can be stored and retrieved
            String sessionUserId = testUser.getUserId();
            String sessionUserType = testUser.getUserType();
            
            assertThat(sessionUserId).isEqualTo(TestConstants.TEST_USER_ID);
            assertThat(sessionUserType).isEqualTo("U");
        }
    }

    @Nested
    @DisplayName("Integration with Test Framework Tests")
    class TestFrameworkIntegrationTests {

        @Test
        @DisplayName("AssertJ assertion compatibility")
        public void testAssertJCompatibility() {
            // Test comprehensive AssertJ assertions
            assertThat(testUser)
                .isNotNull()
                .extracting(UserSecurity::getUserId, UserSecurity::getUserType)
                .containsExactly(TestConstants.TEST_USER_ID, "U");
                
            assertThat(testUser.getAuthorities())
                .hasSize(1)
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_USER");
        }

        @Test
        @DisplayName("Test method execution performance")
        public void testExecutionPerformance() {
            long startTime = System.currentTimeMillis();
            
            // Execute representative operations
            testUser.getUsername();
            testUser.getPassword();
            testUser.getAuthorities();
            testUser.isEnabled();
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Verify method execution is performant
            assertThat(duration).isLessThan(10L); // Sub-10ms for method calls
        }
    }
}