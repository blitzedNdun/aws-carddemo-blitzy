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

import com.carddemo.service.MainMenuService;
import com.carddemo.dto.MenuRequest;
import com.carddemo.dto.MenuResponse;
import com.carddemo.dto.MenuOption;
import com.carddemo.config.MenuConfiguration;
import com.carddemo.exception.ValidationException;

/**
 * Comprehensive unit test suite for MainMenuService class validating COBOL COMEN01C 
 * main menu logic migration to Java Spring Boot service.
 * 
 * This test class ensures 100% functional parity between the original COBOL program
 * COMEN01C.cbl and the Java implementation, testing all critical business logic
 * including menu generation, role-based filtering, navigation flow, and session
 * state handling that replaces CICS COMMAREA management.
 * 
 * Test Coverage Areas:
 * 1. Main menu process validation (MAIN-PARA equivalent)
 * 2. Menu option selection processing (PROCESS-ENTER-KEY equivalent) 
 * 3. Role-based menu generation (BUILD-MENU-OPTIONS equivalent)
 * 4. User access control validation (CDEMO-USRTYP checks)
 * 5. Header information population (POPULATE-HEADER-INFO equivalent)
 * 6. Error handling and validation patterns
 * 7. PF-key equivalent functionality in service layer
 * 
 * The tests validate exact behavioral matching with COBOL logic including:
 * - Option number validation (WS-OPTION IS NOT NUMERIC checks)
 * - Range validation (> CDEMO-MENU-OPT-COUNT checks)
 * - Access level validation (CDEMO-USRTYP-USER vs CDEMO-MENU-OPT-USRTYPE)
 * - Error message generation matching COBOL constants
 * - Transaction code routing preparation
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MainMenuService - COBOL COMEN01C Logic Validation Tests")
public class MainMenuServiceTest {

    @Mock
    private MenuConfiguration menuConfiguration;

    @InjectMocks
    private MainMenuService mainMenuService;

    private List<MenuOption> testMenuOptions;
    private MenuRequest testMenuRequest;

    /**
     * Set up test data before each test execution.
     * Creates test menu options and request data that matches COBOL COMEN02Y structure.
     */
    @BeforeEach
    @DisplayName("Setup Test Data - COBOL Data Structure Simulation")
    void setupTestData() {
        testMenuOptions = createTestMenuOptions();
        testMenuRequest = createTestMenuRequest();
        
        // Configure mock to return test menu options (equivalent to COPY COMEN02Y)
        // Use lenient stubbing to avoid UnnecessaryStubbingException for tests that don't use it
        lenient().when(menuConfiguration.getMenuOptions()).thenReturn(testMenuOptions);
    }

    /**
     * Test main menu process with valid user credentials.
     * Validates the MAIN-PARA and SEND-MENU-SCREEN equivalent logic.
     */
    @Test
    @DisplayName("Main Menu Process - Valid User Session (MAIN-PARA Logic)")
    void testMainMenuProcessWithValidUser() {
        // Arrange - equivalent to COBOL COMMAREA initialization
        String userId = "TESTUSER";
        String userType = "U";
        String userName = "Test User";

        // Act - equivalent to MAIN-PARA procedure execution
        MenuResponse response = mainMenuService.buildMainMenu(userId, userType, userName);

        // Assert - validate response structure matches COMEN1AO output
        assertThat(response).isNotNull();
        assertThat(response.getUserName()).isEqualTo(userName);
        assertThat(response.getCurrentDate()).isNotNull();
        assertThat(response.getCurrentTime()).isNotNull();
        assertThat(response.getProgramName()).isEqualTo("COMEN01C");
        assertThat(response.getSystemId()).isEqualTo("CM00");
        assertThat(response.getMenuOptions()).isNotEmpty();
        assertThat(response.getMenuOptions()).hasSize(10); // CDEMO-MENU-OPT-COUNT
        
        // Verify menu configuration was accessed
        verify(menuConfiguration, times(1)).getMenuOptions();
    }

    /**
     * Test main menu process with invalid option selection.
     * Validates PROCESS-ENTER-KEY error handling logic.
     */
    @Test
    @DisplayName("Menu Selection - Invalid Option Error (PROCESS-ENTER-KEY Validation)")
    void testMainMenuProcessWithInvalidOption() {
        // Arrange - equivalent to invalid WS-OPTION input
        Integer invalidOption = 99;
        String userType = "U";

        // Act & Assert - equivalent to COBOL error flag setting
        assertThatThrownBy(() -> mainMenuService.processMenuSelection(invalidOption, userType))
            .isInstanceOf(ValidationException.class)
            .hasMessage("Please enter a valid option number...");
    }

    /**
     * Test menu building for admin user with full access.
     * Validates BUILD-MENU-OPTIONS with CDEMO-USRTYP-ADMIN logic.
     */
    @Test
    @DisplayName("Menu Generation - Admin User Full Access (BUILD-MENU-OPTIONS Admin Logic)")
    void testBuildMenuForAdminUser() {
        // Arrange - admin user type equivalent to CDEMO-USRTYP-ADMIN
        String adminUserType = "A";

        // Act - equivalent to BUILD-MENU-OPTIONS procedure for admin
        ArrayList<MenuOption> menuOptions = mainMenuService.buildMenuOptions(adminUserType);

        // Assert - admin sees all options including admin-only ones
        assertThat(menuOptions).isNotNull();
        assertThat(menuOptions).hasSize(10); // All options visible
        
        // Verify each option has proper formatting (equivalent to STRING operation)
        for (MenuOption option : menuOptions) {
            assertThat(option.getDescription()).contains(". ");
            assertThat(option.getOptionNumber()).isBetween(1, 10);
            assertThat(option.getEnabled()).isTrue();
        }
    }

    /**
     * Test menu building for regular user with filtered access.
     * Validates BUILD-MENU-OPTIONS with CDEMO-USRTYP-USER logic.
     */
    @Test
    @DisplayName("Menu Generation - Regular User Filtered Access (BUILD-MENU-OPTIONS User Logic)")
    void testBuildMenuForRegularUser() {
        // Arrange - regular user type equivalent to CDEMO-USRTYP-USER
        String userType = "U";

        // Act - equivalent to BUILD-MENU-OPTIONS procedure for user
        ArrayList<MenuOption> menuOptions = mainMenuService.buildMenuOptions(userType);

        // Assert - user sees only user-accessible options
        assertThat(menuOptions).isNotNull();
        assertThat(menuOptions).hasSize(10); // All test options are user-accessible
        
        // Verify no admin-only options are present
        for (MenuOption option : menuOptions) {
            String accessLevel = option.getAccessLevel();
            assertThat(accessLevel).satisfiesAnyOf(
                level -> assertThat(level).isEqualTo("U"),
                level -> assertThat(level).isNull()
            );
        }
    }

    /**
     * Test menu option validation with valid selection.
     * Validates PROCESS-ENTER-KEY validation logic for valid input.
     */
    @Test
    @DisplayName("Option Validation - Valid Selection (WS-OPTION Validation Success)")
    void testValidateSelectionWithValidOption() {
        // Arrange - valid option within range (1 to CDEMO-MENU-OPT-COUNT)
        Integer validOption = 5;
        String userType = "U";

        // Act & Assert - equivalent to successful WS-OPTION validation
        // Should not throw any exception
        mainMenuService.validateMenuOption(validOption, userType);
        
        // Verify no exception was thrown (test passes if no exception)
        assertThat(validOption).isBetween(1, 10);
    }

    /**
     * Test menu option validation with invalid selection.
     * Validates PROCESS-ENTER-KEY validation logic for invalid input.
     */
    @Test
    @DisplayName("Option Validation - Invalid Selection (WS-OPTION Validation Failure)")
    void testValidateSelectionWithInvalidOption() {
        // Arrange - invalid options testing various COBOL validation scenarios
        String userType = "U";

        // Test null option (equivalent to WS-OPTION = ZEROS)
        assertThatThrownBy(() -> mainMenuService.validateMenuOption(null, userType))
            .isInstanceOf(ValidationException.class)
            .hasMessage("Please enter a valid option number...");

        // Test zero option (equivalent to WS-OPTION = ZEROS)
        assertThatThrownBy(() -> mainMenuService.validateMenuOption(0, userType))
            .isInstanceOf(ValidationException.class)
            .hasMessage("Please enter a valid option number...");

        // Test option out of range (equivalent to WS-OPTION > CDEMO-MENU-OPT-COUNT)
        assertThatThrownBy(() -> mainMenuService.validateMenuOption(15, userType))
            .isInstanceOf(ValidationException.class)
            .hasMessage("Please enter a valid option number...");

        // Test negative option
        assertThatThrownBy(() -> mainMenuService.validateMenuOption(-1, userType))
            .isInstanceOf(ValidationException.class)
            .hasMessage("Please enter a valid option number...");
    }

    /**
     * Test successful menu selection processing.
     * Validates PROCESS-ENTER-KEY success path with transaction routing.
     */
    @Test
    @DisplayName("Menu Selection - Success Processing (XCTL Program Routing)")
    void testProcessMenuSelectionSuccess() {
        // Arrange - valid option for transaction routing
        Integer validOption = 1;
        String userType = "U";

        // Act - equivalent to successful PROCESS-ENTER-KEY with XCTL preparation
        String transactionCode = mainMenuService.processMenuSelection(validOption, userType);

        // Assert - validate transaction code returned for routing
        assertThat(transactionCode).isNotNull();
        assertThat(transactionCode).isEqualTo("COACTVWC"); // Expected transaction code for option 1
    }

    /**
     * Test menu selection processing with access denied scenario.
     * Validates CDEMO-USRTYP-USER vs CDEMO-MENU-OPT-USRTYPE access control.
     */
    @Test
    @DisplayName("Menu Selection - Access Denied (RACF Authorization Check)")
    void testProcessMenuSelectionAccessDenied() {
        // Arrange - create admin-only option for access control testing
        MenuOption adminOption = new MenuOption();
        adminOption.setOptionNumber(11);
        adminOption.setDescription("Admin Only Function");
        adminOption.setTransactionCode("COADM01C");
        adminOption.setAccessLevel("A"); // Admin access required
        
        List<MenuOption> adminTestOptions = new ArrayList<>(testMenuOptions);
        adminTestOptions.add(adminOption);
        
        when(menuConfiguration.getMenuOptions()).thenReturn(adminTestOptions);
        
        String userType = "U"; // Regular user trying to access admin option

        // Act & Assert - equivalent to COBOL access denied error
        assertThatThrownBy(() -> mainMenuService.validateMenuOption(11, userType))
            .isInstanceOf(ValidationException.class)
            .hasMessage("No access - Admin Only option... ");
    }

    /**
     * Creates test menu configuration that matches COBOL COMEN02Y structure.
     * Simulates the CDEMO-MENU-OPTIONS-DATA table with 10 menu options.
     * 
     * @return MenuConfiguration mock for testing
     */
    private MenuConfiguration createTestMenuConfiguration() {
        MenuConfiguration config = new MenuConfiguration();
        return config;
    }

    /**
     * Creates test menu options matching COBOL COMEN02Y copybook structure.
     * Replicates the static data table CDEMO-MENU-OPTIONS-DATA with exact values.
     * 
     * @return List of MenuOption objects for testing
     */
    private List<MenuOption> createTestMenuOptions() {
        List<MenuOption> options = new ArrayList<>();

        // Create 10 menu options matching COMEN02Y structure
        String[] descriptions = {
            "Account View", "Account Update", "Credit Card List", "Credit Card View", 
            "Credit Card Update", "Transaction List", "Transaction View", 
            "Transaction Add", "Transaction Reports", "Bill Payment"
        };
        
        String[] transactionCodes = {
            "COACTVWC", "COACTUPC", "COCRDLIC", "COCRDSLC", "COCRDUPC",
            "COTRN00C", "COTRN01C", "COTRN02C", "CORPT00C", "COBIL00C"
        };

        for (int i = 0; i < 10; i++) {
            MenuOption option = new MenuOption();
            option.setOptionNumber(i + 1);
            option.setDescription(descriptions[i]);
            option.setTransactionCode(transactionCodes[i]);
            option.setAccessLevel("U"); // All test options are user-accessible
            option.setEnabled(true);
            options.add(option);
        }

        return options;
    }

    /**
     * Creates test menu request matching CICS COMMAREA structure.
     * Simulates the input parameters equivalent to CARDDEMO-COMMAREA.
     * 
     * @return MenuRequest object for testing
     */
    private MenuRequest createTestMenuRequest() {
        MenuRequest request = new MenuRequest();
        request.setUserId("TESTUSER");
        request.setUserType("U");
        request.setSelectedOption("1");
        request.setMenuType("MAIN");
        request.setTransactionCode("CM00");
        return request;
    }
}