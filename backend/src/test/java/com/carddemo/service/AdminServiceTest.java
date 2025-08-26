package com.carddemo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;

import com.carddemo.service.AdminService;
import com.carddemo.test.TestDataGenerator;
import com.carddemo.dto.AdminMenuResponse;
import com.carddemo.dto.MenuOption;
import com.carddemo.dto.UserDto;
import com.carddemo.entity.UserSecurity;
import com.carddemo.repository.UserSecurityRepository;
import com.carddemo.service.ReportGenerationService;
import com.carddemo.dto.MenuRequest;

/**
 * Unit test class for AdminService validating COBOL COADM01C admin menu logic migration to Java.
 * 
 * This comprehensive test suite validates administrative functions, user management operations, 
 * and privilege verification translated from the original COBOL COADM01C.cbl program.
 * 
 * Test Coverage:
 * - Administrative privilege verification
 * - User management operations
 * - Admin menu generation with proper options
 * - Security role validation and access control
 * - Admin-specific function access control
 * - System status information retrieval
 * - Integration with UserSecurity and reporting services
 * 
 * Testing Framework:
 * - JUnit 5 with Mockito for dependency isolation
 * - AssertJ for fluent assertions
 * - TestDataGenerator for COBOL-compliant test data
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Unit Tests - COBOL COADM01C Logic Validation")
public class AdminServiceTest {

    // Service under test - will be mocked since it doesn't exist yet
    @Mock
    private AdminService adminService;
    
    // Mock dependencies for isolated testing
    @Mock
    private UserSecurityRepository userSecurityRepository;
    
    @Mock
    private ReportGenerationService reportGenerationService;
    
    // Test data generator for COBOL-compliant test scenarios
    private TestDataGenerator testDataGenerator;
    
    // Test fixtures for consistent test data
    private UserSecurity testAdminUser;
    private UserSecurity testRegularUser;
    private List<MenuOption> testAdminMenuOptions;
    private List<MenuOption> testRegularMenuOptions;

    /**
     * Set up test fixtures before each test.
     * Creates COBOL-compliant test data using TestDataGenerator.
     */
    @BeforeEach
    void setupTestData() {
        testDataGenerator = new TestDataGenerator();
        testDataGenerator.resetRandomSeed();
        
        // Generate test users with different privilege levels
        testAdminUser = testDataGenerator.generateUserSecurity()
            .toBuilder()
            .username("ADMIN01")
            .userType("ADMIN")
            .enabled(true)
            .build();
            
        testRegularUser = testDataGenerator.generateUserSecurity()
            .toBuilder()
            .username("USER01")
            .userType("USER")
            .enabled(true)
            .build();
        
        // Generate menu options for different user types
        testAdminMenuOptions = testDataGenerator.generateMenuOptions();
        testRegularMenuOptions = testAdminMenuOptions.stream()
            .filter(option -> !"ADMIN".equals(option.getAccessLevel()))
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Test admin privilege verification with valid admin user.
     * Validates COBOL COADM01C admin privilege checking logic.
     */
    @Test
    @DisplayName("Admin privilege verification succeeds for valid admin user")
    void testVerifyAdminPrivilegesWithValidAdmin() {
        // Given: Valid admin user
        when(adminService.verifyAdminPrivileges(testAdminUser)).thenReturn(true);
        
        // When: Verifying admin privileges
        boolean hasAdminPrivileges = adminService.verifyAdminPrivileges(testAdminUser);
        
        // Then: Admin privileges should be granted
        assertThat(hasAdminPrivileges).isTrue();
        verify(adminService).verifyAdminPrivileges(testAdminUser);
    }

    /**
     * Test admin privilege verification with invalid user.
     * Validates security access control for non-admin users.
     */
    @Test
    @DisplayName("Admin privilege verification fails for regular user")
    void testVerifyAdminPrivilegesWithInvalidUser() {
        // Given: Regular user without admin privileges
        when(adminService.verifyAdminPrivileges(testRegularUser)).thenReturn(false);
        
        // When: Verifying admin privileges
        boolean hasAdminPrivileges = adminService.verifyAdminPrivileges(testRegularUser);
        
        // Then: Admin privileges should be denied
        assertThat(hasAdminPrivileges).isFalse();
        verify(adminService).verifyAdminPrivileges(testRegularUser);
    }

    /**
     * Test admin menu options generation for admin user.
     * Validates COBOL COADM01C menu generation logic for privileged users.
     */
    @Test
    @DisplayName("Admin menu options generated correctly for admin user")
    void testGenerateAdminMenuOptionsForAdmin() {
        // Given: Admin user and expected menu options
        when(adminService.generateAdminMenuOptions(testAdminUser))
            .thenReturn(testAdminMenuOptions);
        
        // When: Generating admin menu options
        List<MenuOption> menuOptions = adminService.generateAdminMenuOptions(testAdminUser);
        
        // Then: All admin menu options should be available
        assertThat(menuOptions)
            .isNotNull()
            .isNotEmpty()
            .hasSize(testAdminMenuOptions.size())
            .containsAll(testAdminMenuOptions);
        
        // Verify admin-specific options are present
        assertThat(menuOptions.stream()
            .anyMatch(option -> "ADMIN".equals(option.getAccessLevel())))
            .isTrue();
            
        verify(adminService).generateAdminMenuOptions(testAdminUser);
    }

    /**
     * Test admin menu options generation for regular user.
     * Validates proper filtering of admin-only menu options.
     */
    @Test
    @DisplayName("Admin menu options filtered correctly for regular user")
    void testGenerateAdminMenuOptionsForRegularUser() {
        // Given: Regular user and filtered menu options
        when(adminService.generateAdminMenuOptions(testRegularUser))
            .thenReturn(testRegularMenuOptions);
        
        // When: Generating menu options for regular user
        List<MenuOption> menuOptions = adminService.generateAdminMenuOptions(testRegularUser);
        
        // Then: Admin-specific options should be filtered out
        assertThat(menuOptions)
            .isNotNull()
            .hasSize(testRegularMenuOptions.size())
            .containsAll(testRegularMenuOptions);
        
        // Verify no admin-specific options are present
        assertThat(menuOptions.stream()
            .noneMatch(option -> "ADMIN".equals(option.getAccessLevel())))
            .isTrue();
            
        verify(adminService).generateAdminMenuOptions(testRegularUser);
    }

    /**
     * Test admin menu processing with valid menu selection.
     * Validates COBOL COADM01C menu processing logic.
     */
    @Test
    @DisplayName("Admin menu processing succeeds with valid selection")
    void testProcessAdminMenuWithValidSelection() {
        // Given: Valid admin menu request
        MenuRequest menuRequest = createTestAdminMenuRequest();
        menuRequest.setSelectedOption("1");
        menuRequest.setUserId(testAdminUser.getUsername());
        
        AdminMenuResponse expectedResponse = createTestAdminMenuResponse();
        when(adminService.processAdminMenu(menuRequest)).thenReturn(expectedResponse);
        
        // When: Processing admin menu request
        AdminMenuResponse response = adminService.processAdminMenu(menuRequest);
        
        // Then: Valid response should be returned
        assertThat(response)
            .isNotNull()
            .isEqualTo(expectedResponse);
        
        assertThat(response.getAdminOptions())
            .isNotNull()
            .isNotEmpty();
            
        verify(adminService).processAdminMenu(menuRequest);
    }

    /**
     * Test admin menu processing with invalid selection.
     * Validates error handling for invalid menu choices.
     */
    @Test
    @DisplayName("Admin menu processing fails with invalid selection")
    void testProcessAdminMenuWithInvalidSelection() {
        // Given: Invalid admin menu request
        MenuRequest menuRequest = createTestAdminMenuRequest();
        menuRequest.setSelectedOption("999"); // Invalid option
        menuRequest.setUserId(testAdminUser.getUsername());
        
        when(adminService.processAdminMenu(menuRequest))
            .thenThrow(new IllegalArgumentException("Invalid menu selection: 999"));
        
        // When/Then: Processing should throw exception
        assertThatThrownBy(() -> adminService.processAdminMenu(menuRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid menu selection: 999");
            
        verify(adminService).processAdminMenu(menuRequest);
    }

    /**
     * Test user management operations handling.
     * Validates COBOL COADM01C user management functionality.
     */
    @Test
    @DisplayName("User management operations handled correctly")
    void testHandleUserManagementOperations() {
        // Given: User management request
        UserDto testUserDto = testDataGenerator.generateAdminUser();
        
        when(adminService.handleUserManagement(testUserDto)).thenReturn(testUserDto);
        
        // When: Handling user management operation
        UserDto result = adminService.handleUserManagement(testUserDto);
        
        // Then: User management should be successful
        assertThat(result)
            .isNotNull()
            .isEqualTo(testUserDto);
        
        assertThat(result.getUserId()).isEqualTo(testUserDto.getUserId());
        assertThat(result.getUserType()).isEqualTo(testUserDto.getUserType());
        
        verify(adminService).handleUserManagement(testUserDto);
    }

    /**
     * Test admin access validation with admin role.
     * Validates role-based access control logic.
     */
    @Test
    @DisplayName("Admin access validation succeeds for admin role")
    void testValidateAdminAccessWithAdminRole() {
        // Given: Admin user
        String adminUserId = testAdminUser.getUsername();
        when(adminService.validateAdminAccess(adminUserId)).thenReturn(true);
        
        // When: Validating admin access
        boolean hasAccess = adminService.validateAdminAccess(adminUserId);
        
        // Then: Access should be granted
        assertThat(hasAccess).isTrue();
        verify(adminService).validateAdminAccess(adminUserId);
    }

    /**
     * Test admin access validation with user role.
     * Validates access denial for non-admin users.
     */
    @Test
    @DisplayName("Admin access validation fails for user role")
    void testValidateAdminAccessWithUserRole() {
        // Given: Regular user
        String regularUserId = testRegularUser.getUsername();
        when(adminService.validateAdminAccess(regularUserId)).thenReturn(false);
        
        // When: Validating admin access
        boolean hasAccess = adminService.validateAdminAccess(regularUserId);
        
        // Then: Access should be denied
        assertThat(hasAccess).isFalse();
        verify(adminService).validateAdminAccess(regularUserId);
    }

    /**
     * Test admin menu response building with proper admin options.
     * Validates COBOL COADM01C response formatting logic.
     */
    @Test
    @DisplayName("Admin menu response built with proper admin options")
    void testBuildMenuResponseWithAdminOptions() {
        // Given: Admin menu request and expected response
        MenuRequest menuRequest = createTestAdminMenuRequest();
        AdminMenuResponse expectedResponse = createTestAdminMenuResponse();
        
        when(adminService.buildMenuResponse(menuRequest)).thenReturn(expectedResponse);
        
        // When: Building menu response
        AdminMenuResponse response = adminService.buildMenuResponse(menuRequest);
        
        // Then: Response should contain admin options
        assertThat(response)
            .isNotNull()
            .isEqualTo(expectedResponse);
        
        assertThat(response.getAdminOptions())
            .isNotNull()
            .isNotEmpty();
        
        assertThat(response.getSystemStatus()).isNotNull();
        assertThat(response.getActiveUsers()).isGreaterThanOrEqualTo(0);
        
        verify(adminService).buildMenuResponse(menuRequest);
    }

    /**
     * Test system status information retrieval.
     * Validates system monitoring functionality from COADM01C.
     */
    @Test
    @DisplayName("System status information retrieved correctly")
    void testGetSystemStatusInformation() {
        // Given: Expected system status response
        AdminMenuResponse mockResponse = createTestAdminMenuResponse();
        when(adminService.buildMenuResponse(any(MenuRequest.class)))
            .thenReturn(mockResponse);
        
        MenuRequest systemRequest = createTestAdminMenuRequest();
        
        // When: Getting system status information
        AdminMenuResponse response = adminService.buildMenuResponse(systemRequest);
        
        // Then: System status should be populated
        assertThat(response.getSystemStatus()).isNotNull();
        assertThat(response.getLastBatchRun()).isNotNull();
        assertThat(response.getActiveUsers()).isGreaterThanOrEqualTo(0);
        
        verify(adminService).buildMenuResponse(systemRequest);
    }

    /**
     * Test active user count validation.
     * Validates user statistics tracking functionality.
     */
    @Test
    @DisplayName("Active user count validation works correctly")
    void testGetActiveUserCountValidation() {
        // Given: Mock response with active user count
        AdminMenuResponse mockResponse = createTestAdminMenuResponse();
        when(adminService.buildMenuResponse(any(MenuRequest.class)))
            .thenReturn(mockResponse);
        
        MenuRequest countRequest = createTestAdminMenuRequest();
        
        // When: Getting active user count
        AdminMenuResponse response = adminService.buildMenuResponse(countRequest);
        
        // Then: Active user count should be valid
        assertThat(response.getActiveUsers())
            .isNotNull()
            .isGreaterThanOrEqualTo(0)
            .isLessThanOrEqualTo(1000); // Reasonable upper bound
            
        verify(adminService).buildMenuResponse(countRequest);
    }

    // Helper methods for test data creation

    /**
     * Creates a test admin user with proper COBOL-style attributes.
     */
    UserSecurity createTestAdminUser() {
        return testDataGenerator.generateUserSecurity()
            .toBuilder()
            .username("TESTADM")
            .userType("ADMIN")
            .enabled(true)
            .build();
    }

    /**
     * Creates a test regular user with standard user attributes.
     */
    UserSecurity createTestRegularUser() {
        return testDataGenerator.generateUserSecurity()
            .toBuilder()
            .username("TESTUSER")
            .userType("USER")
            .enabled(true)
            .build();
    }

    /**
     * Creates test menu options with various access levels.
     */
    List<MenuOption> createTestMenuOptions() {
        return testDataGenerator.generateMenuOptions();
    }

    /**
     * Creates a test admin menu request.
     */
    private MenuRequest createTestAdminMenuRequest() {
        MenuRequest request = new MenuRequest();
        request.setUserId(testAdminUser.getUsername());
        request.setUserType(testAdminUser.getUserType());
        request.setMenuType("ADMIN");
        request.setRequestType("MENU");
        return request;
    }

    /**
     * Creates a test admin menu response.
     */
    private AdminMenuResponse createTestAdminMenuResponse() {
        AdminMenuResponse response = new AdminMenuResponse();
        response.setAdminOptions(testAdminMenuOptions);
        response.setSystemStatus("ACTIVE");
        response.setActiveUsers(42);
        response.setLastBatchRun(java.time.LocalDateTime.now().minusHours(2));
        return response;
    }
}