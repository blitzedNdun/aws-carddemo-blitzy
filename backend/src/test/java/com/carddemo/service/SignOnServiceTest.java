package com.carddemo.service;

import com.carddemo.dto.SignOnRequest;
import com.carddemo.dto.SignOnResponse;
import com.carddemo.entity.UserSecurity;
import com.carddemo.repository.UserSecurityRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit test class for SignOnService validating COBOL COSGN00C 
 * sign-on logic migration to Java Spring Boot implementation.
 * 
 * Tests validate:
 * - User authentication and credential validation
 * - Role-based access control (Admin vs Regular User)  
 * - Session management using Spring Session
 * - Invalid login scenario handling
 * - Performance requirements (sub-200ms response times)
 * - COBOL-equivalent test data and functional parity
 * 
 * Testing Framework: JUnit 5, Mockito, AssertJ
 * Coverage Requirement: 100% business logic coverage
 * Performance Target: Authentication response within 200ms
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SignOnService - COBOL COSGN00C Equivalent Testing")
class SignOnServiceTest extends BaseServiceTest {

    @Mock
    private UserSecurityRepository userSecurityRepository;

    @InjectMocks
    private SignOnService signOnService;

    // Test data constants matching COBOL program values
    private static final String VALID_ADMIN_USER = "TESTADM1";
    private static final String VALID_REGULAR_USER = "TESTUSER";
    private static final String VALID_PASSWORD = "PASSWORD";
    private static final String INVALID_USER = "BADUSER1";
    private static final String INVALID_PASSWORD = "BADPASS1";
    private static final String EMPTY_USER = "";
    private static final String EMPTY_PASSWORD = "";
    private static final String ADMIN_USER_TYPE = "A";
    private static final String REGULAR_USER_TYPE = "R";
    
    // Performance measurement constants
    private static final long MAX_RESPONSE_TIME_MS = 200L;

    private UserSecurity mockAdminUser;
    private UserSecurity mockRegularUser;
    private SignOnRequest validAdminRequest;
    private SignOnRequest validUserRequest;
    private SignOnRequest invalidUserRequest;
    private SignOnRequest invalidPasswordRequest;
    private SignOnRequest emptyUserRequest;
    private SignOnRequest emptyPasswordRequest;

    @BeforeEach
    void setUp() {
        setupTestData();
        createMockUsers();
    }

    /**
     * Initialize test data using BaseServiceTest utilities and TestDataGenerator
     */
    private void setupTestData() {
        // Create admin sign-on request
        validAdminRequest = new SignOnRequest();
        validAdminRequest.setUserId(VALID_ADMIN_USER);
        validAdminRequest.setPassword(VALID_PASSWORD);
        
        // Create regular user sign-on request  
        validUserRequest = new SignOnRequest();
        validUserRequest.setUserId(VALID_REGULAR_USER);
        validUserRequest.setPassword(VALID_PASSWORD);
        
        // Create invalid user request
        invalidUserRequest = new SignOnRequest();
        invalidUserRequest.setUserId(INVALID_USER);
        invalidUserRequest.setPassword(VALID_PASSWORD);
        
        // Create invalid password request
        invalidPasswordRequest = new SignOnRequest();
        invalidPasswordRequest.setUserId(VALID_REGULAR_USER);
        invalidPasswordRequest.setPassword(INVALID_PASSWORD);
        
        // Create empty user request
        emptyUserRequest = new SignOnRequest();
        emptyUserRequest.setUserId(EMPTY_USER);
        emptyUserRequest.setPassword(VALID_PASSWORD);
        
        // Create empty password request
        emptyPasswordRequest = new SignOnRequest();
        emptyPasswordRequest.setUserId(VALID_REGULAR_USER);
        emptyPasswordRequest.setPassword(EMPTY_PASSWORD);
    }

    /**
     * Create mock UserSecurity objects matching COBOL USRSEC file structure
     */
    private void createMockUsers() {
        // Mock admin user matching COBOL USRSEC record structure
        mockAdminUser = createMockAdmin();
        mockAdminUser.setUsername(VALID_ADMIN_USER);
        mockAdminUser.setPassword(VALID_PASSWORD); // In real implementation would be encrypted
        mockAdminUser.setUserType(ADMIN_USER_TYPE);
        mockAdminUser.setEnabled(true);
        mockAdminUser.setAuthorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        // Mock regular user matching COBOL USRSEC record structure
        mockRegularUser = createMockUser();
        mockRegularUser.setUsername(VALID_REGULAR_USER);
        mockRegularUser.setPassword(VALID_PASSWORD); // In real implementation would be encrypted
        mockRegularUser.setUserType(REGULAR_USER_TYPE);
        mockRegularUser.setEnabled(true);
        mockRegularUser.setAuthorities(List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Nested
    @DisplayName("Authentication Validation Tests - COBOL PROCESS-ENTER-KEY Equivalent")
    class AuthenticationValidationTests {

        @Test
        @DisplayName("Valid admin user authentication - should return success with admin menu")
        void testValidAdminAuthentication_ReturnsSuccessWithAdminMenu() {
            // Given: Mock repository returns admin user (simulates COBOL READ USRSEC success)
            when(userSecurityRepository.findByUsername(VALID_ADMIN_USER))
                .thenReturn(Optional.of(mockAdminUser));
            
            // When: Processing sign-on request (equivalent to COBOL PROCESS-ENTER-KEY)
            Instant startTime = Instant.now();
            SignOnResponse response = signOnService.signOn(validAdminRequest);
            long responseTime = Duration.between(startTime, Instant.now()).toMillis();
            
            // Then: Verify successful authentication matching COBOL behavior
            assertThat(response).isNotNull();
            assertThat(response.isAuthenticated()).isTrue();
            assertThat(response.getUserDetails()).isEqualTo(VALID_ADMIN_USER);
            assertThat(response.getNextScreen()).isEqualTo("COADM01C"); // Admin menu screen
            assertThat(response.getErrorMessage()).isNullOrEmpty();
            assertThat(response.getSessionToken()).isNotNull();
            
            // Verify performance requirement (sub-200ms response time)
            validateResponseTime(responseTime);
            
            // Verify repository interaction
            verify(userSecurityRepository, times(1)).findByUsername(VALID_ADMIN_USER);
        }

        @Test
        @DisplayName("Valid regular user authentication - should return success with user menu")
        void testValidRegularUserAuthentication_ReturnsSuccessWithUserMenu() {
            // Given: Mock repository returns regular user (simulates COBOL READ USRSEC success)
            when(userSecurityRepository.findByUsername(VALID_REGULAR_USER))
                .thenReturn(Optional.of(mockRegularUser));
            
            // When: Processing sign-on request
            Instant startTime = Instant.now();
            SignOnResponse response = signOnService.signOn(validUserRequest);
            long responseTime = Duration.between(startTime, Instant.now()).toMillis();
            
            // Then: Verify successful authentication with user menu
            assertThat(response).isNotNull();
            assertThat(response.isAuthenticated()).isTrue();
            assertThat(response.getUserDetails()).isEqualTo(VALID_REGULAR_USER);
            assertThat(response.getNextScreen()).isEqualTo("COMEN01C"); // User menu screen
            assertThat(response.getErrorMessage()).isNullOrEmpty();
            assertThat(response.getSessionToken()).isNotNull();
            
            // Verify performance requirement
            validateResponseTime(responseTime);
            
            // Verify repository interaction  
            verify(userSecurityRepository, times(1)).findByUsername(VALID_REGULAR_USER);
        }

        @Test
        @DisplayName("Invalid user ID - should return failure with user not found message")
        void testInvalidUserId_ReturnsFailureWithUserNotFoundMessage() {
            // Given: Mock repository returns empty (simulates COBOL RESP-CD = 13)
            when(userSecurityRepository.findByUsername(INVALID_USER))
                .thenReturn(Optional.empty());
            
            // When: Processing sign-on with invalid user
            Instant startTime = Instant.now();
            SignOnResponse response = signOnService.signOn(invalidUserRequest);
            long responseTime = Duration.between(startTime, Instant.now()).toMillis();
            
            // Then: Verify failure response matching COBOL error handling
            assertThat(response).isNotNull();
            assertThat(response.isAuthenticated()).isFalse();
            assertThat(response.getUserDetails()).isNull();
            assertThat(response.getNextScreen()).isNull();
            assertThat(response.getErrorMessage()).isEqualTo("User not found. Try again ...");
            assertThat(response.getSessionToken()).isNull();
            
            // Verify performance requirement maintained even for failures
            validateResponseTime(responseTime);
            
            // Verify repository interaction
            verify(userSecurityRepository, times(1)).findByUsername(INVALID_USER);
        }

        @Test  
        @DisplayName("Invalid password - should return failure with wrong password message")
        void testInvalidPassword_ReturnsFailureWithWrongPasswordMessage() {
            // Given: Mock user exists but password validation will fail
            UserSecurity userWithDifferentPassword = createMockUser();
            userWithDifferentPassword.setUsername(VALID_REGULAR_USER);
            userWithDifferentPassword.setPassword("DIFFERENT_PASSWORD");
            userWithDifferentPassword.setUserType(REGULAR_USER_TYPE);
            
            when(userSecurityRepository.findByUsername(VALID_REGULAR_USER))
                .thenReturn(Optional.of(userWithDifferentPassword));
            
            // When: Processing sign-on with invalid password  
            Instant startTime = Instant.now();
            SignOnResponse response = signOnService.signOn(invalidPasswordRequest);
            long responseTime = Duration.between(startTime, Instant.now()).toMillis();
            
            // Then: Verify failure response matching COBOL password validation
            assertThat(response).isNotNull();
            assertThat(response.isAuthenticated()).isFalse();
            assertThat(response.getUserDetails()).isNull();
            assertThat(response.getNextScreen()).isNull();
            assertThat(response.getErrorMessage()).isEqualTo("Wrong Password. Try again ...");
            assertThat(response.getSessionToken()).isNull();
            
            // Verify performance requirement
            validateResponseTime(responseTime);
            
            // Verify repository interaction
            verify(userSecurityRepository, times(1)).findByUsername(VALID_REGULAR_USER);
        }
    }

    @Nested
    @DisplayName("Input Validation Tests - COBOL Field Validation Equivalent")
    class InputValidationTests {

        @Test
        @DisplayName("Empty user ID - should return validation error")
        void testEmptyUserId_ReturnsValidationError() {
            // When: Processing request with empty user ID (COBOL: USERIDI = SPACES)
            Instant startTime = Instant.now();
            SignOnResponse response = signOnService.signOn(emptyUserRequest);
            long responseTime = Duration.between(startTime, Instant.now()).toMillis();
            
            // Then: Verify validation error matching COBOL behavior
            assertThat(response).isNotNull();
            assertThat(response.isAuthenticated()).isFalse();
            assertThat(response.getUserDetails()).isNull();
            assertThat(response.getNextScreen()).isNull();
            assertThat(response.getErrorMessage()).isEqualTo("Please enter User ID ...");
            assertThat(response.getSessionToken()).isNull();
            
            // Verify performance requirement
            validateResponseTime(responseTime);
            
            // Verify no repository interaction for validation failures
            verify(userSecurityRepository, never()).findByUsername(any());
        }

        @Test
        @DisplayName("Empty password - should return validation error")
        void testEmptyPassword_ReturnsValidationError() {
            // When: Processing request with empty password (COBOL: PASSWDI = SPACES)
            Instant startTime = Instant.now();
            SignOnResponse response = signOnService.signOn(emptyPasswordRequest);
            long responseTime = Duration.between(startTime, Instant.now()).toMillis();
            
            // Then: Verify validation error matching COBOL behavior
            assertThat(response).isNotNull();
            assertThat(response.isAuthenticated()).isFalse();
            assertThat(response.getUserDetails()).isNull();
            assertThat(response.getNextScreen()).isNull();
            assertThat(response.getErrorMessage()).isEqualTo("Please enter Password ...");
            assertThat(response.getSessionToken()).isNull();
            
            // Verify performance requirement
            validateResponseTime(responseTime);
            
            // Verify no repository interaction for validation failures
            verify(userSecurityRepository, never()).findByUsername(any());
        }

        @Test
        @DisplayName("Null request - should handle gracefully")
        void testNullRequest_HandledGracefully() {
            // When: Processing null request
            Instant startTime = Instant.now();
            SignOnResponse response = signOnService.signOn(null);
            long responseTime = Duration.between(startTime, Instant.now()).toMillis();
            
            // Then: Verify graceful handling
            assertThat(response).isNotNull();
            assertThat(response.isAuthenticated()).isFalse();
            assertThat(response.getUserDetails()).isNull();
            assertThat(response.getNextScreen()).isNull();
            assertThat(response.getErrorMessage()).isNotEmpty();
            assertThat(response.getSessionToken()).isNull();
            
            // Verify performance requirement maintained
            validateResponseTime(responseTime);
            
            // Verify no repository interaction
            verify(userSecurityRepository, never()).findByUsername(any());
        }
    }

    @Nested
    @DisplayName("Session Management Tests - Spring Session Integration")
    class SessionManagementTests {

        @Test
        @DisplayName("Successful authentication creates user session")
        void testSuccessfulAuthentication_CreatesUserSession() {
            // Given: Valid user authentication setup
            when(userSecurityRepository.findByUsername(VALID_REGULAR_USER))
                .thenReturn(Optional.of(mockRegularUser));
            
            // When: Processing successful sign-on
            SignOnResponse response = signOnService.signOn(validUserRequest);
            
            // Then: Verify session creation
            assertThat(response.getSessionToken()).isNotNull();
            assertThat(response.isAuthenticated()).isTrue();
            
            // Verify session contains user context matching COBOL COMMAREA
            assertThat(response.getUserDetails()).isNotNull();
            assertThat(response.getUserDetails()).isEqualTo(VALID_REGULAR_USER);
        }

        @Test
        @DisplayName("Failed authentication does not create session")
        void testFailedAuthentication_DoesNotCreateSession() {
            // Given: Invalid user setup  
            when(userSecurityRepository.findByUsername(INVALID_USER))
                .thenReturn(Optional.empty());
            
            // When: Processing failed sign-on
            SignOnResponse response = signOnService.signOn(invalidUserRequest);
            
            // Then: Verify no session creation
            assertThat(response.getSessionToken()).isNull();
            assertThat(response.isAuthenticated()).isFalse();
            assertThat(response.getUserDetails()).isNull();
        }

        @Test
        @DisplayName("User details populated correctly in session")
        void testUserDetailsPopulatedCorrectlyInSession() {
            // Given: Admin user authentication
            when(userSecurityRepository.findByUsername(VALID_ADMIN_USER))
                .thenReturn(Optional.of(mockAdminUser));
            
            // When: Processing admin sign-on
            SignOnResponse response = signOnService.signOn(validAdminRequest);
            
            // Then: Verify admin user details in session
            assertThat(response.getUserDetails()).isEqualTo(VALID_ADMIN_USER);
            assertThat(response.getNextScreen()).isEqualTo("COADM01C");
            assertThat(response.isAuthenticated()).isTrue();
            assertThat(response.getSessionToken()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Role-Based Access Control Tests - COBOL User Type Handling")
    class RoleBasedAccessControlTests {

        @Test
        @DisplayName("Admin user gets admin menu screen")
        void testAdminUser_GetsAdminMenuScreen() {
            // Given: Admin user in repository
            when(userSecurityRepository.findByUsername(VALID_ADMIN_USER))
                .thenReturn(Optional.of(mockAdminUser));
            
            // When: Admin user signs on
            SignOnResponse response = signOnService.signOn(validAdminRequest);
            
            // Then: Verify admin routing (COBOL: IF CDEMO-USRTYP-ADMIN EXEC CICS XCTL PROGRAM('COADM01C'))
            assertThat(response.isAuthenticated()).isTrue();
            assertThat(response.getNextScreen()).isEqualTo("COADM01C");
            assertThat(response.getUserDetails()).isEqualTo(VALID_ADMIN_USER);
        }

        @Test
        @DisplayName("Regular user gets user menu screen")  
        void testRegularUser_GetsUserMenuScreen() {
            // Given: Regular user in repository
            when(userSecurityRepository.findByUsername(VALID_REGULAR_USER))
                .thenReturn(Optional.of(mockRegularUser));
            
            // When: Regular user signs on
            SignOnResponse response = signOnService.signOn(validUserRequest);
            
            // Then: Verify user routing (COBOL: ELSE EXEC CICS XCTL PROGRAM('COMEN01C'))
            assertThat(response.isAuthenticated()).isTrue();
            assertThat(response.getNextScreen()).isEqualTo("COMEN01C");
            assertThat(response.getUserDetails()).isEqualTo(VALID_REGULAR_USER);
        }

        @Test
        @DisplayName("User type determines screen navigation")
        void testUserType_DeterminesScreenNavigation() {
            // Test both user types for comprehensive coverage
            
            // Admin user test
            when(userSecurityRepository.findByUsername(VALID_ADMIN_USER))
                .thenReturn(Optional.of(mockAdminUser));
            SignOnResponse adminResponse = signOnService.signOn(validAdminRequest);
            
            // Regular user test
            when(userSecurityRepository.findByUsername(VALID_REGULAR_USER))
                .thenReturn(Optional.of(mockRegularUser));
            SignOnResponse userResponse = signOnService.signOn(validUserRequest);
            
            // Verify different navigation paths
            assertThat(adminResponse.getNextScreen()).isEqualTo("COADM01C");
            assertThat(userResponse.getNextScreen()).isEqualTo("COMEN01C");
            assertThat(adminResponse.getNextScreen()).isNotEqualTo(userResponse.getNextScreen());
        }
    }

    @Nested
    @DisplayName("Performance Validation Tests - Sub-200ms Response Requirement")
    class PerformanceValidationTests {

        @Test
        @DisplayName("Authentication response time under 200ms - successful case")
        void testAuthenticationResponseTime_SuccessfulCase_Under200ms() {
            // Given: Valid user setup
            when(userSecurityRepository.findByUsername(VALID_REGULAR_USER))
                .thenReturn(Optional.of(mockRegularUser));
            
            // When: Measuring authentication performance
            long responseTime = measurePerformance(() -> signOnService.signOn(validUserRequest));
            
            // Then: Verify performance requirement
            assertThat(responseTime).isLessThan(MAX_RESPONSE_TIME_MS);
        }

        @Test
        @DisplayName("Authentication response time under 200ms - failure case")
        void testAuthenticationResponseTime_FailureCase_Under200ms() {
            // Given: Invalid user setup
            when(userSecurityRepository.findByUsername(INVALID_USER))
                .thenReturn(Optional.empty());
            
            // When: Measuring authentication performance for failure case
            long responseTime = measurePerformance(() -> signOnService.signOn(invalidUserRequest));
            
            // Then: Verify performance requirement maintained even for failures
            assertThat(responseTime).isLessThan(MAX_RESPONSE_TIME_MS);
        }

        @Test
        @DisplayName("Bulk authentication performance test")
        void testBulkAuthentication_MaintainsPerformance() {
            // Given: User setup for bulk testing
            when(userSecurityRepository.findByUsername(VALID_REGULAR_USER))
                .thenReturn(Optional.of(mockRegularUser));
            
            // When: Performing multiple authentications
            int testRuns = 10;
            long totalTime = 0;
            
            for (int i = 0; i < testRuns; i++) {
                long individualTime = measurePerformance(() -> signOnService.signOn(validUserRequest));
                totalTime += individualTime;
                assertThat(individualTime).isLessThan(MAX_RESPONSE_TIME_MS);
            }
            
            // Then: Verify average performance
            long averageTime = totalTime / testRuns;
            assertThat(averageTime).isLessThan(MAX_RESPONSE_TIME_MS);
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Cases - COBOL Exception Equivalent")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Repository exception handling")
        void testRepositoryException_HandledGracefully() {
            // Given: Repository throws exception (simulates VSAM file error)
            when(userSecurityRepository.findByUsername(VALID_REGULAR_USER))
                .thenThrow(new RuntimeException("Database connection error"));
            
            // When: Processing sign-on with repository error
            SignOnResponse response = signOnService.signOn(validUserRequest);
            
            // Then: Verify graceful error handling
            assertThat(response).isNotNull();
            assertThat(response.isAuthenticated()).isFalse();
            assertThat(response.getErrorMessage()).isEqualTo("Unable to verify the User ...");
            assertThat(response.getSessionToken()).isNull();
        }

        @Test
        @DisplayName("Disabled user account handling")
        void testDisabledUser_HandledCorrectly() {
            // Given: Disabled user account
            UserSecurity disabledUser = createMockUser();
            disabledUser.setUsername(VALID_REGULAR_USER);
            disabledUser.setPassword(VALID_PASSWORD);
            disabledUser.setEnabled(false); // Account disabled
            
            when(userSecurityRepository.findByUsername(VALID_REGULAR_USER))
                .thenReturn(Optional.of(disabledUser));
            
            // When: Processing sign-on for disabled user
            SignOnResponse response = signOnService.signOn(validUserRequest);
            
            // Then: Verify disabled account handling
            assertThat(response).isNotNull();
            assertThat(response.isAuthenticated()).isFalse();
            assertThat(response.getErrorMessage()).contains("account");
            assertThat(response.getSessionToken()).isNull();
        }
    }

    @Nested
    @DisplayName("COBOL Functional Parity Tests - Direct Logic Comparison")
    class CobolFunctionalParityTests {

        @Test
        @DisplayName("COBOL paragraph 0000-MAIN-PROCESSING equivalent validation")
        void testMainProcessing_CobolEquivalent() {
            // This test validates the main processing flow equivalent to COBOL MAIN-PARA
            
            // Given: Valid user setup matching COBOL test scenario
            when(userSecurityRepository.findByUsername(VALID_REGULAR_USER))
                .thenReturn(Optional.of(mockRegularUser));
            
            // When: Processing main sign-on logic  
            SignOnResponse response = signOnService.signOn(validUserRequest);
            
            // Then: Verify equivalent COBOL behavior
            assertThat(response).isNotNull();
            assertThat(response.isAuthenticated()).isTrue();
            
            // Verify COBOL equivalent variables populated
            assertThat(response.getUserDetails()).isEqualTo(VALID_REGULAR_USER); // CDEMO-USER-ID
            assertThat(response.getNextScreen()).isEqualTo("COMEN01C"); // CICS XCTL target
        }

        @Test
        @DisplayName("COBOL READ-user-sec-file equivalent validation")
        void testReadUserSecFile_CobolEquivalent() {
            // This test validates the user security file read equivalent to READ-USER-SEC-FILE paragraph
            
            // Given: User exists in repository (simulates successful VSAM read)
            when(userSecurityRepository.findByUsername(VALID_ADMIN_USER))
                .thenReturn(Optional.of(mockAdminUser));
            
            // When: Processing user lookup
            SignOnResponse response = signOnService.signOn(validAdminRequest);
            
            // Then: Verify successful read equivalent (COBOL RESP-CD = 0)
            assertThat(response.isAuthenticated()).isTrue();
            assertThat(response.getUserDetails()).isEqualTo(VALID_ADMIN_USER);
            
            // Verify repository interaction equivalent to VSAM READ
            verify(userSecurityRepository, times(1)).findByUsername(VALID_ADMIN_USER);
        }
    }

    @Nested
    @DisplayName("BigDecimal and Precision Tests - COBOL COMP-3 Equivalent") 
    class BigDecimalPrecisionTests {

        @Test
        @DisplayName("Numeric field validation maintains COBOL precision")
        void testNumericFieldValidation_CobolPrecision() {
            // While sign-on doesn't directly use BigDecimal, test framework validation
            
            // Test BigDecimal equality assertion helper from BaseServiceTest
            BigDecimal testValue1 = new BigDecimal("123.45");
            BigDecimal testValue2 = new BigDecimal("123.45");
            BigDecimal testValue3 = new BigDecimal("123.46");
            
            // Should pass - equal values
            assertBigDecimalEquals(testValue1, testValue2);
            
            // Verify precision handling
            assertThat(testValue1).isEqualByComparingTo(testValue2);
            assertThat(testValue1).isNotEqualByComparingTo(testValue3);
        }
    }

    @Nested
    @DisplayName("Individual Service Method Tests - Complete Coverage")
    class IndividualServiceMethodTests {

        @Test
        @DisplayName("validateCredentials method - successful validation")
        void testValidateCredentials_SuccessfulValidation() {
            // Given: Valid user credentials
            when(userSecurityRepository.findByUsername(VALID_REGULAR_USER))
                .thenReturn(Optional.of(mockRegularUser));
            
            // When: Validating credentials directly
            boolean isValid = signOnService.validateCredentials(VALID_REGULAR_USER, VALID_PASSWORD);
            
            // Then: Verify validation success
            assertThat(isValid).isTrue();
            verify(userSecurityRepository, times(1)).findByUsername(VALID_REGULAR_USER);
        }

        @Test
        @DisplayName("validateCredentials method - failed validation")
        void testValidateCredentials_FailedValidation() {
            // Given: User not found
            when(userSecurityRepository.findByUsername(INVALID_USER))
                .thenReturn(Optional.empty());
            
            // When: Validating invalid credentials
            boolean isValid = signOnService.validateCredentials(INVALID_USER, VALID_PASSWORD);
            
            // Then: Verify validation failure
            assertThat(isValid).isFalse();
            verify(userSecurityRepository, times(1)).findByUsername(INVALID_USER);
        }

        @Test
        @DisplayName("createUserSession method - admin user session creation")
        void testCreateUserSession_AdminUser() {
            // When: Creating session for admin user
            String sessionToken = signOnService.createUserSession(mockAdminUser);
            
            // Then: Verify session creation
            assertThat(sessionToken).isNotNull();
            assertThat(sessionToken).isNotEmpty();
            assertThat(sessionToken.length()).isGreaterThan(10); // Reasonable token length
        }

        @Test
        @DisplayName("createUserSession method - regular user session creation")
        void testCreateUserSession_RegularUser() {
            // When: Creating session for regular user
            String sessionToken = signOnService.createUserSession(mockRegularUser);
            
            // Then: Verify session creation
            assertThat(sessionToken).isNotNull();
            assertThat(sessionToken).isNotEmpty();
            assertThat(sessionToken.length()).isGreaterThan(10);
        }

        @Test
        @DisplayName("getUserDetails method - admin user details retrieval")
        void testGetUserDetails_AdminUser() {
            // Given: Valid session token for admin user
            String sessionToken = signOnService.createUserSession(mockAdminUser);
            
            // When: Getting user details from session
            UserSecurity retrievedUser = signOnService.getUserDetails(sessionToken);
            
            // Then: Verify user details retrieval
            assertThat(retrievedUser).isNotNull();
            assertThat(retrievedUser.getUsername()).isEqualTo(VALID_ADMIN_USER);
            assertThat(retrievedUser.getUserType()).isEqualTo(ADMIN_USER_TYPE);
            assertThat(retrievedUser.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("getUserDetails method - regular user details retrieval")
        void testGetUserDetails_RegularUser() {
            // Given: Valid session token for regular user
            String sessionToken = signOnService.createUserSession(mockRegularUser);
            
            // When: Getting user details from session
            UserSecurity retrievedUser = signOnService.getUserDetails(sessionToken);
            
            // Then: Verify user details retrieval
            assertThat(retrievedUser).isNotNull();
            assertThat(retrievedUser.getUsername()).isEqualTo(VALID_REGULAR_USER);
            assertThat(retrievedUser.getUserType()).isEqualTo(REGULAR_USER_TYPE);
            assertThat(retrievedUser.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("getUserDetails method - invalid session token")
        void testGetUserDetails_InvalidSessionToken() {
            // When: Getting user details with invalid token
            UserSecurity retrievedUser = signOnService.getUserDetails("INVALID_TOKEN_123");
            
            // Then: Verify graceful handling
            assertThat(retrievedUser).isNull();
        }

        @Test
        @DisplayName("logout method - successful logout")
        void testLogout_SuccessfulLogout() {
            // Given: Valid session token
            String sessionToken = signOnService.createUserSession(mockRegularUser);
            
            // When: Performing logout
            boolean logoutResult = signOnService.logout(sessionToken);
            
            // Then: Verify successful logout
            assertThat(logoutResult).isTrue();
            
            // Verify session is invalidated
            UserSecurity retrievedUser = signOnService.getUserDetails(sessionToken);
            assertThat(retrievedUser).isNull();
        }

        @Test
        @DisplayName("logout method - invalid session token")
        void testLogout_InvalidSessionToken() {
            // When: Performing logout with invalid token
            boolean logoutResult = signOnService.logout("INVALID_TOKEN_456");
            
            // Then: Verify graceful handling
            assertThat(logoutResult).isFalse();
        }

        @Test
        @DisplayName("logout method - performance validation")
        void testLogout_PerformanceValidation() {
            // Given: Valid session
            String sessionToken = signOnService.createUserSession(mockRegularUser);
            
            // When: Measuring logout performance
            long responseTime = measurePerformance(() -> signOnService.logout(sessionToken));
            
            // Then: Verify performance requirement
            validateResponseTime(responseTime);
        }
    }

    @Nested
    @DisplayName("Integration with Spring Security - Authentication Framework")
    class SpringSecurityIntegrationTests {

        @Test
        @DisplayName("UserDetails interface implementation validation")
        void testUserDetailsImplementation_ProperIntegration() {
            // Given: Mock user with Spring Security integration
            assertThat(mockRegularUser).isInstanceOf(org.springframework.security.core.userdetails.UserDetails.class);
            
            // When: Accessing Spring Security methods
            String username = mockRegularUser.getUsername();
            String password = mockRegularUser.getPassword();
            boolean isEnabled = mockRegularUser.isEnabled();
            var authorities = mockRegularUser.getAuthorities();
            
            // Then: Verify proper implementation
            assertThat(username).isEqualTo(VALID_REGULAR_USER);
            assertThat(password).isEqualTo(VALID_PASSWORD);
            assertThat(isEnabled).isTrue();
            assertThat(authorities).isNotEmpty();
            assertThat(authorities).extracting("authority").contains("ROLE_USER");
        }

        @Test
        @DisplayName("Admin user authorities validation")
        void testAdminUserAuthorities_ProperRoleAssignment() {
            // When: Checking admin authorities
            var authorities = mockAdminUser.getAuthorities();
            
            // Then: Verify admin role assignment
            assertThat(authorities).isNotEmpty();
            assertThat(authorities).extracting("authority").contains("ROLE_ADMIN");
        }
    }

    @Nested
    @DisplayName("Complete End-to-End Workflow Tests")
    class EndToEndWorkflowTests {

        @Test
        @DisplayName("Complete admin user workflow - sign on to logout")
        void testCompleteAdminWorkflow_SignOnToLogout() {
            // Given: Admin user setup
            when(userSecurityRepository.findByUsername(VALID_ADMIN_USER))
                .thenReturn(Optional.of(mockAdminUser));
            
            // When: Complete workflow execution
            // Step 1: Sign on
            SignOnResponse signOnResponse = signOnService.signOn(validAdminRequest);
            
            // Step 2: Validate credentials separately
            boolean credentialsValid = signOnService.validateCredentials(VALID_ADMIN_USER, VALID_PASSWORD);
            
            // Step 3: Create session
            String sessionToken = signOnService.createUserSession(mockAdminUser);
            
            // Step 4: Get user details
            UserSecurity userDetails = signOnService.getUserDetails(sessionToken);
            
            // Step 5: Logout
            boolean logoutResult = signOnService.logout(sessionToken);
            
            // Then: Verify complete workflow
            assertThat(signOnResponse.isAuthenticated()).isTrue();
            assertThat(signOnResponse.getNextScreen()).isEqualTo("COADM01C");
            assertThat(credentialsValid).isTrue();
            assertThat(sessionToken).isNotNull();
            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo(VALID_ADMIN_USER);
            assertThat(logoutResult).isTrue();
        }

        @Test
        @DisplayName("Complete regular user workflow - sign on to logout")
        void testCompleteRegularUserWorkflow_SignOnToLogout() {
            // Given: Regular user setup
            when(userSecurityRepository.findByUsername(VALID_REGULAR_USER))
                .thenReturn(Optional.of(mockRegularUser));
            
            // When: Complete workflow execution
            SignOnResponse signOnResponse = signOnService.signOn(validUserRequest);
            boolean credentialsValid = signOnService.validateCredentials(VALID_REGULAR_USER, VALID_PASSWORD);
            String sessionToken = signOnService.createUserSession(mockRegularUser);
            UserSecurity userDetails = signOnService.getUserDetails(sessionToken);
            boolean logoutResult = signOnService.logout(sessionToken);
            
            // Then: Verify complete workflow
            assertThat(signOnResponse.isAuthenticated()).isTrue();
            assertThat(signOnResponse.getNextScreen()).isEqualTo("COMEN01C");
            assertThat(credentialsValid).isTrue();
            assertThat(sessionToken).isNotNull();
            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo(VALID_REGULAR_USER);
            assertThat(logoutResult).isTrue();
        }
    }

    @Nested
    @DisplayName("Test Data Generator Integration Tests")
    class TestDataGeneratorIntegrationTests {

        @Test
        @DisplayName("Generated test user validation")
        void testGeneratedTestUser_ProperValidation() {
            // When: Using TestDataGenerator to create user
            UserSecurity generatedUser = TestDataGenerator.generateUserSecurity();
            
            // Then: Verify generated user meets COBOL requirements
            assertThat(generatedUser).isNotNull();
            assertThat(generatedUser.getUsername()).isNotNull();
            assertThat(generatedUser.getUsername().length()).isLessThanOrEqualTo(8); // COBOL USER-ID limit
            assertThat(generatedUser.getPassword()).isNotNull();
            assertThat(generatedUser.getUserType()).isIn(ADMIN_USER_TYPE, REGULAR_USER_TYPE);
        }

        @Test
        @DisplayName("COBOL comparison utils integration")
        void testCobolComparisonUtils_Integration() {
            // Given: Generated test data
            UserSecurity testUser = TestDataGenerator.generateUserSecurity();
            
            // When: Using comparison utilities
            String comparisonReport = CobolComparisonUtils.generateComparisonReport();
            
            // Then: Verify utilities work correctly
            assertThat(comparisonReport).isNotNull();
            assertThat(comparisonReport).isNotEmpty();
        }
    }

    /**
     * Helper method to validate response time against performance requirements
     */
    private void validateResponseTime(long responseTimeMs) {
        assertThat(responseTimeMs)
            .as("Response time should be under %d ms for authentication operations", MAX_RESPONSE_TIME_MS)
            .isLessThan(MAX_RESPONSE_TIME_MS);
    }
}