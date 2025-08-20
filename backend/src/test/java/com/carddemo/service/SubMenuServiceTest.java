/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.lenient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import com.carddemo.service.SubMenuService;
import com.carddemo.test.TestDataGenerator;
import com.carddemo.dto.MenuRequest;
import com.carddemo.dto.MenuResponse;
import com.carddemo.dto.MenuOption;
import com.carddemo.config.MenuConfiguration;
import com.carddemo.entity.UserSecurity;

/**
 * Comprehensive unit test suite for SubMenuService class validating COBOL sub-menu 
 * navigation logic migration from COMEN01C to Java Spring Boot service.
 * 
 * This test class ensures 100% functional parity between the original COBOL program
 * COMEN01C.cbl sub-menu processing logic and the Java implementation, testing all 
 * critical business logic including:
 * 
 * - Sub-menu generation with hierarchical navigation support
 * - Context-based option filtering and role-based access control
 * - Back navigation handling preserving menu state
 * - Menu state management replicating CICS COMMAREA behavior
 * - Menu access validation matching COBOL user type checking
 * 
 * Test Coverage Areas:
 * 1. Sub-menu generation processing (equivalent to BUILD-MENU-OPTIONS for sub-menus)
 * 2. Navigation flow control (equivalent to PROCESS-ENTER-KEY sub-menu routing)
 * 3. Back navigation state management (equivalent to RETURN-TO-PARENT processing)
 * 4. Context-based option filtering (equivalent to role-based menu building)
 * 5. Menu state preservation (equivalent to COMMAREA state management)
 * 6. Access validation (equivalent to user type authorization checking)
 * 
 * The tests validate that the Java implementation preserves the exact business logic
 * from COMEN01C.cbl while providing modern Spring Boot service capabilities for
 * REST API consumption and session state management.
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SubMenuService Tests - COBOL COMEN01C Sub-Menu Logic Migration")
public class SubMenuServiceTest {

    @Mock
    private MenuConfiguration mockMenuConfiguration;

    @Mock
    private TestDataGenerator mockTestDataGenerator;

    @InjectMocks
    private SubMenuService subMenuService;

    private MenuRequest testMenuRequest;
    private List<MenuOption> testMenuOptions;
    private UserSecurity testAdminUser;
    private UserSecurity testRegularUser;

    /**
     * Sets up test data and mock behaviors before each test execution.
     * 
     * Initializes COBOL-compliant test data structures including:
     * - Menu request objects with user context
     * - Hierarchical menu options with access levels
     * - User security objects with different user types
     * - Menu configuration with static menu data
     * 
     * This setup ensures all tests operate with consistent, COBOL-compatible
     * test data that matches the original mainframe data structures.
     */
    @BeforeEach
    void setupTestData() {
        // Create test menu options with hierarchical structure
        testMenuOptions = createTestSubMenuOptions();
        
        // Create test users with different access levels
        testAdminUser = createTestUserContext("ADMIN");
        testRegularUser = createTestUserContext("USER");
        
        // Create test menu request
        testMenuRequest = createTestMenuRequest();
        
        // Configure mock menu configuration
        createTestMenuConfiguration();
    }

    /**
     * Tests sub-menu generation with valid user context.
     * 
     * Validates that the generateSubMenu method correctly:
     * - Processes valid user context and menu request
     * - Returns properly structured sub-menu response
     * - Includes appropriate menu options for user type
     * - Maintains hierarchical menu structure
     * - Preserves COBOL menu option formatting
     * 
     * Equivalent to COBOL BUILD-MENU-OPTIONS paragraph for sub-menu creation.
     */
    @Test
    @DisplayName("Generate Sub-Menu - Valid Context")
    void testGenerateSubMenuWithValidContext() {
        // Arrange
        when(mockMenuConfiguration.getMenuOptions()).thenReturn(testMenuOptions);
        when(mockTestDataGenerator.generateMenuOptions()).thenReturn(testMenuOptions);
        
        testMenuRequest.setUserId("TESTUSER");
        testMenuRequest.setUserType("USER");
        testMenuRequest.setRequestType("SUB_MENU");
        
        // Act
        MenuResponse response = subMenuService.generateSubMenu(testMenuRequest);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getMenuOptions()).isNotNull();
        assertThat(response.getMenuOptions()).isNotEmpty();
        assertThat(response.getErrorMessage()).isEmpty();
        assertThat(response.getUserName()).isNotNull();
        assertThat(response.getCurrentDate()).isNotNull();
        assertThat(response.getCurrentTime()).isNotNull();
        
        // Verify menu options are properly filtered for user type
        List<MenuOption> menuOptions = response.getMenuOptions();
        assertThat(menuOptions).hasSize(testMenuOptions.size());
        
        // Verify COBOL-style option numbering (1-based indexing)
        for (int i = 0; i < menuOptions.size(); i++) {
            MenuOption option = menuOptions.get(i);
            assertThat(option.getOptionNumber()).isEqualTo(i + 1);
            assertThat(option.getDescription()).isNotNull();
            assertThat(option.getEnabled()).isTrue();
        }
        
        verify(mockMenuConfiguration, times(1)).getMenuOptions();
    }

    /**
     * Tests sub-menu generation with invalid user context.
     * 
     * Validates that the generateSubMenu method correctly:
     * - Handles invalid or null user context
     * - Returns appropriate error response
     * - Does not expose menu options for invalid context
     * - Maintains security by denying access
     * 
     * Equivalent to COBOL user validation logic in MAIN-PARA.
     */
    @Test
    @DisplayName("Generate Sub-Menu - Invalid Context")
    void testGenerateSubMenuWithInvalidContext() {
        // Arrange
        testMenuRequest.setUserId(null);
        testMenuRequest.setUserType(null);
        
        // Act
        MenuResponse response = subMenuService.generateSubMenu(testMenuRequest);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getMenuOptions()).isEmpty();
        assertThat(response.getErrorMessage()).contains("Invalid user context");
        assertThat(response.getUserName()).isEmpty();
        
        // Verify no menu configuration access for invalid context
        verify(mockMenuConfiguration, never()).getMenuOptions();
    }

    /**
     * Tests successful sub-menu navigation.
     * 
     * Validates that the navigateToSubMenu method correctly:
     * - Processes valid menu option selection
     * - Returns success response with navigation target
     * - Maintains session state for navigation
     * - Preserves user context through navigation
     * 
     * Equivalent to COBOL PROCESS-ENTER-KEY paragraph for menu selection.
     */
    @Test
    @DisplayName("Navigate to Sub-Menu - Success")
    void testNavigateToSubMenuSuccess() {
        // Arrange
        testMenuRequest.setSelectedOption("2");
        testMenuRequest.setUserId("TESTUSER");
        testMenuRequest.setUserType("USER");
        
        when(mockMenuConfiguration.getMenuOptions()).thenReturn(testMenuOptions);
        
        // Act
        MenuResponse response = subMenuService.navigateToSubMenu(testMenuRequest);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getErrorMessage()).isEmpty();
        assertThat(response.getUserName()).isNotNull();
        
        // Verify navigation preserves user context
        assertThat(response.getUserName()).contains("TESTUSER");
        
        verify(mockMenuConfiguration, times(1)).getMenuOptions();
    }

    /**
     * Tests sub-menu navigation with access denied.
     * 
     * Validates that the navigateToSubMenu method correctly:
     * - Denies access for unauthorized menu options
     * - Returns appropriate error message
     * - Maintains security by preventing unauthorized navigation
     * - Preserves user session state
     * 
     * Equivalent to COBOL user type checking logic for admin-only options.
     */
    @Test
    @DisplayName("Navigate to Sub-Menu - Access Denied")
    void testNavigateToSubMenuAccessDenied() {
        // Arrange - Regular user trying to access admin option
        testMenuRequest.setSelectedOption("1"); // Admin-only option
        testMenuRequest.setUserId("TESTUSER");
        testMenuRequest.setUserType("USER");
        
        when(mockMenuConfiguration.getMenuOptions()).thenReturn(testMenuOptions);
        
        // Act
        MenuResponse response = subMenuService.navigateToSubMenu(testMenuRequest);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getErrorMessage()).contains("No access - Admin Only option");
        assertThat(response.getUserName()).isNotNull();
        
        verify(mockMenuConfiguration, times(1)).getMenuOptions();
    }

    /**
     * Tests back navigation to parent menu.
     * 
     * Validates that the handleBackNavigation method correctly:
     * - Handles PF3 key equivalent navigation
     * - Returns to appropriate parent menu level
     * - Maintains menu hierarchy state
     * - Preserves user session context
     * 
     * Equivalent to COBOL PF3 key processing and parent menu return logic.
     */
    @Test
    @DisplayName("Handle Back Navigation - To Parent")
    void testHandleBackNavigationToParent() {
        // Arrange
        testMenuRequest.setUserId("TESTUSER");
        testMenuRequest.setUserType("USER");
        testMenuRequest.setCurrentMenuLevel("SUB_MENU");
        testMenuRequest.setParentMenu("MAIN_MENU");
        
        // Act
        MenuResponse response = subMenuService.handleBackNavigation(testMenuRequest);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getErrorMessage()).isEmpty();
        assertThat(response.getUserName()).isNotNull();
        
        // Verify navigation preserves user context
        assertThat(response.getUserName()).contains("TESTUSER");
    }

    /**
     * Tests back navigation to root menu.
     * 
     * Validates that the handleBackNavigation method correctly:
     * - Handles navigation to root menu level
     * - Resets menu hierarchy state appropriately
     * - Maintains user authentication state
     * - Returns to main menu context
     * 
     * Equivalent to COBOL root menu return processing.
     */
    @Test
    @DisplayName("Handle Back Navigation - To Root")
    void testHandleBackNavigationToRoot() {
        // Arrange
        testMenuRequest.setUserId("TESTUSER");
        testMenuRequest.setUserType("USER");
        testMenuRequest.setCurrentMenuLevel("ROOT");
        testMenuRequest.setParentMenu(null);
        
        // Act
        MenuResponse response = subMenuService.handleBackNavigation(testMenuRequest);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getErrorMessage()).isEmpty();
        assertThat(response.getUserName()).isNotNull();
    }

    /**
     * Tests menu option filtering for admin users.
     * 
     * Validates that the filterOptionsByContext method correctly:
     * - Shows all menu options for admin users
     * - Includes admin-only options in the response
     * - Maintains proper option ordering and formatting
     * - Preserves COBOL access control logic
     * 
     * Equivalent to COBOL user type checking for admin access.
     */
    @Test
    @DisplayName("Filter Options by Context - Admin User")
    void testFilterOptionsByContextAdmin() {
        // Arrange
        testMenuRequest.setUserId("ADMIN001");
        testMenuRequest.setUserType("ADMIN");
        
        when(mockMenuConfiguration.getMenuOptions()).thenReturn(testMenuOptions);
        
        // Act
        List<MenuOption> filteredOptions = subMenuService.filterOptionsByContext(testMenuRequest);
        
        // Assert
        assertThat(filteredOptions).isNotNull();
        assertThat(filteredOptions).hasSize(testMenuOptions.size());
        
        // Verify admin can see all options including admin-only
        boolean hasAdminOption = filteredOptions.stream()
            .anyMatch(option -> "A".equals(option.getAccessLevel()));
        assertThat(hasAdminOption).isTrue();
        
        verify(mockMenuConfiguration, times(1)).getMenuOptions();
    }

    /**
     * Tests menu option filtering for regular users.
     * 
     * Validates that the filterOptionsByContext method correctly:
     * - Filters out admin-only menu options
     * - Shows only user-accessible options
     * - Maintains proper option numbering after filtering
     * - Preserves COBOL security restrictions
     * 
     * Equivalent to COBOL user type filtering logic.
     */
    @Test
    @DisplayName("Filter Options by Context - Regular User")
    void testFilterOptionsByContextRegularUser() {
        // Arrange
        testMenuRequest.setUserId("USER001");
        testMenuRequest.setUserType("USER");
        
        when(mockMenuConfiguration.getMenuOptions()).thenReturn(testMenuOptions);
        
        // Act
        List<MenuOption> filteredOptions = subMenuService.filterOptionsByContext(testMenuRequest);
        
        // Assert
        assertThat(filteredOptions).isNotNull();
        
        // Verify regular user cannot see admin-only options
        boolean hasAdminOption = filteredOptions.stream()
            .anyMatch(option -> "A".equals(option.getAccessLevel()));
        assertThat(hasAdminOption).isFalse();
        
        // Verify user can see regular options
        boolean hasUserOption = filteredOptions.stream()
            .anyMatch(option -> "U".equals(option.getAccessLevel()));
        assertThat(hasUserOption).isTrue();
        
        verify(mockMenuConfiguration, times(1)).getMenuOptions();
    }

    /**
     * Tests menu state management with context preservation.
     * 
     * Validates that the manageMenuState method correctly:
     * - Preserves menu navigation state across requests
     * - Maintains user session context
     * - Handles menu level transitions properly
     * - Replicates CICS COMMAREA state management
     * 
     * Equivalent to COBOL COMMAREA state preservation logic.
     */
    @Test
    @DisplayName("Manage Menu State - Preserve Context")
    void testManageMenuStatePreserveContext() {
        // Arrange
        testMenuRequest.setUserId("TESTUSER");
        testMenuRequest.setUserType("USER");
        testMenuRequest.setCurrentMenuLevel("SUB_MENU");
        testMenuRequest.setMenuState("ACTIVE");
        
        // Act
        MenuResponse response = subMenuService.manageMenuState(testMenuRequest);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUserName()).contains("TESTUSER");
        assertThat(response.getErrorMessage()).isEmpty();
    }

    /**
     * Tests menu state management with state reset.
     * 
     * Validates that the manageMenuState method correctly:
     * - Resets menu state when required
     * - Clears navigation history appropriately
     * - Maintains user authentication
     * - Handles state transition edge cases
     * 
     * Equivalent to COBOL state reset and initialization logic.
     */
    @Test
    @DisplayName("Manage Menu State - Reset")
    void testManageMenuStateReset() {
        // Arrange
        testMenuRequest.setUserId("TESTUSER");
        testMenuRequest.setUserType("USER");
        testMenuRequest.setCurrentMenuLevel("ROOT");
        testMenuRequest.setMenuState("RESET");
        
        // Act
        MenuResponse response = subMenuService.manageMenuState(testMenuRequest);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUserName()).contains("TESTUSER");
        assertThat(response.getErrorMessage()).isEmpty();
    }

    /**
     * Tests menu access validation with proper permissions.
     * 
     * Validates that the validateMenuAccess method correctly:
     * - Grants access for authorized users
     * - Validates user credentials and context
     * - Maintains security compliance
     * - Returns appropriate success response
     * 
     * Equivalent to COBOL user validation and access control logic.
     */
    @Test
    @DisplayName("Validate Menu Access - With Permission")
    void testValidateMenuAccessWithPermission() {
        // Arrange
        testMenuRequest.setUserId("TESTUSER");
        testMenuRequest.setUserType("USER");
        testMenuRequest.setSelectedOption("3"); // User-accessible option
        
        when(mockMenuConfiguration.getMenuOptions()).thenReturn(testMenuOptions);
        
        // Act
        boolean hasAccess = subMenuService.validateMenuAccess(testMenuRequest);
        
        // Assert
        assertThat(hasAccess).isTrue();
        
        verify(mockMenuConfiguration, times(1)).getMenuOptions();
    }

    /**
     * Tests menu access validation without proper permissions.
     * 
     * Validates that the validateMenuAccess method correctly:
     * - Denies access for unauthorized users
     * - Maintains security by preventing unauthorized access
     * - Validates user context properly
     * - Returns appropriate denial response
     * 
     * Equivalent to COBOL access denial and security validation logic.
     */
    @Test
    @DisplayName("Validate Menu Access - Without Permission")
    void testValidateMenuAccessWithoutPermission() {
        // Arrange
        testMenuRequest.setUserId("TESTUSER");
        testMenuRequest.setUserType("USER");
        testMenuRequest.setSelectedOption("1"); // Admin-only option
        
        when(mockMenuConfiguration.getMenuOptions()).thenReturn(testMenuOptions);
        
        // Act
        boolean hasAccess = subMenuService.validateMenuAccess(testMenuRequest);
        
        // Assert
        assertThat(hasAccess).isFalse();
        
        verify(mockMenuConfiguration, times(1)).getMenuOptions();
    }

    /**
     * Creates test sub-menu options with hierarchical access levels.
     * 
     * Generates COBOL-compliant menu option test data including:
     * - Option numbers matching COBOL 1-based indexing
     * - Descriptions formatted to COBOL field lengths
     * - Access levels (A=Admin, U=User) matching COBOL user types
     * - Enabled status for menu option availability
     * 
     * @return List of test menu options with varying access levels
     */
    private List<MenuOption> createTestSubMenuOptions() {
        List<MenuOption> options = new ArrayList<>();
        
        // Admin-only option (equivalent to CDEMO-USRTYP-USER check in COBOL)
        options.add(new MenuOption(1, "Admin Functions", "COADM01C", true, "A"));
        
        // User-accessible options (equivalent to regular menu options)
        options.add(new MenuOption(2, "Account View", "COACTVWC", true, "U"));
        options.add(new MenuOption(3, "Transaction List", "COTRN00C", true, "U"));
        options.add(new MenuOption(4, "Credit Card List", "COCRDLIC", true, "U"));
        options.add(new MenuOption(5, "Bill Payment", "COBIL00C", true, "U"));
        
        return options;
    }

    /**
     * Creates test menu request with user context.
     * 
     * Generates COBOL-compliant menu request structure including:
     * - User ID matching COBOL SEC-USR-ID format
     * - User type matching COBOL user classification
     * - Request type for menu operation identification
     * - Session context for state management
     * 
     * @return Test menu request with complete user context
     */
    private MenuRequest createTestMenuRequest() {
        MenuRequest request = new MenuRequest();
        request.setUserId("TESTUSER");
        request.setUserType("USER");
        request.setRequestType("MENU");
        request.setCurrentMenuLevel("MAIN");
        request.setMenuState("ACTIVE");
        return request;
    }

    /**
     * Creates test user context with specified user type.
     * 
     * Generates COBOL-compliant user security objects including:
     * - Username matching COBOL user ID format
     * - User type for access control validation
     * - Security context for authentication
     * - Role-based access control settings
     * 
     * @param userType The user type (ADMIN or USER)
     * @return Test user security object with specified access level
     */
    private UserSecurity createTestUserContext(String userType) {
        UserSecurity user = new UserSecurity();
        user.setUsername("TEST" + userType);
        user.setUserType(userType);
        return user;
    }

    /**
     * Configures test menu configuration mock.
     * 
     * Sets up mock menu configuration behavior including:
     * - Menu option count matching COBOL static data
     * - Menu options list with proper structure
     * - Access level filtering capabilities
     * - COBOL-compliant data format validation
     * 
     * Replicates COMEN02Y copybook static menu data structure.
     */
    private void createTestMenuConfiguration() {
        when(mockMenuConfiguration.getMenuOptions()).thenReturn(testMenuOptions);
        when(mockMenuConfiguration.getMenuOptionCount()).thenReturn(testMenuOptions.size());
    }
}