package com.carddemo.controller;

import com.carddemo.dto.AdminMenuResponse;
import com.carddemo.dto.MenuOption;
import com.carddemo.dto.MenuResponse;
import com.carddemo.dto.SessionContext;
import com.carddemo.service.MenuService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test class for MenuController that validates main menu and admin menu endpoints,
 * ensuring functional parity with COBOL COMEN01C and COADM01C programs.
 * 
 * Validates:
 * - CM00 transaction equivalent (main menu for regular users)
 * - CA00 transaction equivalent (admin menu for admin users)
 * - Role-based access control matching COBOL user type validation
 * - PF-key navigation (F3 for exit, Enter for selection)
 * - Session state management equivalent to CICS COMMAREA
 * - XCTL-equivalent controller transitions
 */
@SpringBootTest
@AutoConfigureMockMvc
public class MenuControllerTest extends BaseControllerTest {

    @MockBean
    private MenuService menuService;

    private ObjectMapper objectMapper = new ObjectMapper();
    private TestDataBuilder testDataBuilder = new TestDataBuilder();
    private SessionTestUtils sessionTestUtils = new SessionTestUtils();

    /**
     * Setup method to initialize test environment before each test.
     * Equivalent to COBOL program initialization with COMMAREA setup.
     */
    @Override
    public void setUp() {
        super.setUp();
        // Reset all mocks to ensure clean state for each test
        reset(menuService);
    }

    /**
     * Cleanup method executed after each test.
     * Ensures proper session cleanup equivalent to CICS RETURN.
     */
    @Override
    public void tearDown() {
        super.tearDown();
        // Additional cleanup if needed
    }

    /**
     * Test GET /api/menu/main endpoint for regular users.
     * Maps to COMEN01C COBOL program with transaction ID CM00.
     * Validates successful menu retrieval with proper role filtering.
     */
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testGetMainMenuSuccess() throws Exception {
        // Arrange - Create test data equivalent to COBOL menu structure
        SessionContext sessionContext = sessionTestUtils.createTestSession("testuser", "USER");
        MenuResponse expectedResponse = createTestMenuResponse();
        List<MenuOption> menuOptions = createTestMenuOptions();
        expectedResponse.setMenuOptions(menuOptions);

        // Mock service behavior equivalent to COBOL BUILD-MENU-OPTIONS paragraph
        when(menuService.processMenuRequest(any(SessionContext.class)))
            .thenReturn(expectedResponse);
        when(menuService.generateMainMenu(any(SessionContext.class)))
            .thenReturn(expectedResponse);
        when(menuService.validateMenuAccess(any(SessionContext.class)))
            .thenReturn(true);

        // Act & Assert - Test GET request equivalent to CICS SEND MAP
        mockMvc.perform(MockMvcRequestBuilders.get("/api/menu/main")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("sessionContext", sessionContext))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.menuOptions").isArray())
                .andExpect(jsonPath("$.menuOptions").isNotEmpty())
                .andExpect(jsonPath("$.userName").value("testuser"))
                .andExpect(jsonPath("$.errorMessage").isEmpty());

        // Verify service interactions equivalent to COBOL program flow
        verify(menuService).processMenuRequest(any(SessionContext.class));
        verify(menuService).validateMenuAccess(any(SessionContext.class));
    }

    /**
     * Test GET /api/menu/admin endpoint for admin users.
     * Maps to COADM01C COBOL program with transaction ID CA00.
     * Validates admin-specific menu options and system information.
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGetAdminMenuSuccess() throws Exception {
        // Arrange - Create admin session equivalent to COBOL admin COMMAREA
        SessionContext adminSession = sessionTestUtils.createTestSession("admin", "ADMIN");
        AdminMenuResponse expectedResponse = createTestAdminMenuResponse();
        
        // Mock admin service behavior equivalent to COBOL admin menu processing
        when(menuService.processMenuRequest(any(SessionContext.class)))
            .thenReturn(expectedResponse);
        when(menuService.generateAdminMenu(any(SessionContext.class)))
            .thenReturn(expectedResponse);
        when(menuService.validateMenuAccess(any(SessionContext.class)))
            .thenReturn(true);

        // Act & Assert - Test admin endpoint equivalent to CICS SEND MAP for COADM1A
        mockMvc.perform(MockMvcRequestBuilders.get("/api/menu/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("sessionContext", adminSession))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.adminOptions").isArray())
                .andExpect(jsonPath("$.adminOptions").isNotEmpty())
                .andExpect(jsonPath("$.systemStatus").exists())
                .andExpect(jsonPath("$.activeUsers").exists());

        // Verify admin-specific service interactions
        verify(menuService).processMenuRequest(any(SessionContext.class));
        verify(menuService).generateAdminMenu(any(SessionContext.class));
        verify(menuService).validateMenuAccess(any(SessionContext.class));
    }

    /**
     * Test main menu access with regular user role.
     * Validates role-based filtering equivalent to COBOL user type validation
     * in COMEN01C lines 136-143.
     */
    @Test
    @WithMockUser(username = "regularuser", roles = {"USER"})
    public void testGetMainMenuWithRegularUser() throws Exception {
        // Arrange - Regular user session
        SessionContext userSession = sessionTestUtils.createTestSession("regularuser", "USER");
        MenuResponse filteredResponse = createFilteredMenuResponse();

        // Mock filtered menu response - admin options should be excluded
        when(menuService.processMenuRequest(any(SessionContext.class)))
            .thenReturn(filteredResponse);
        when(menuService.validateMenuAccess(any(SessionContext.class)))
            .thenReturn(true);

        // Act & Assert - Verify only user-accessible options are returned
        mockMvc.perform(MockMvcRequestBuilders.get("/api/menu/main")
                .sessionAttr("sessionContext", userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menuOptions").isArray())
                .andExpect(jsonPath("$.menuOptions[?(@.accessLevel == 'A')]").doesNotExist());

        verify(menuService).processMenuRequest(any(SessionContext.class));
    }

    /**
     * Test admin menu access without admin role.
     * Validates security equivalent to COBOL user type restrictions.
     * Should return 403 Forbidden for non-admin users.
     */
    @Test
    @WithMockUser(username = "regularuser", roles = {"USER"})
    public void testGetAdminMenuWithoutAdminRole() throws Exception {
        // Arrange - Regular user trying to access admin menu
        SessionContext userSession = sessionTestUtils.createTestSession("regularuser", "USER");

        // Act & Assert - Should be denied access (403 Forbidden)
        mockMvc.perform(MockMvcRequestBuilders.get("/api/menu/admin")
                .sessionAttr("sessionContext", userSession))
                .andExpect(status().isForbidden());

        // Verify no service calls are made for unauthorized access
        verify(menuService, never()).generateAdminMenu(any(SessionContext.class));
    }

    /**
     * Test dynamic menu option generation based on user context.
     * Maps to COBOL BUILD-MENU-OPTIONS paragraph functionality.
     * Validates proper menu structure and option formatting.
     */
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testMenuOptionGeneration() throws Exception {
        // Arrange - Create session with specific user context
        SessionContext sessionContext = sessionTestUtils.createTestSession("testuser", "USER");
        MenuResponse menuResponse = createDetailedMenuResponse();

        when(menuService.processMenuRequest(any(SessionContext.class)))
            .thenReturn(menuResponse);

        // Act & Assert - Verify menu option structure matches COBOL format
        mockMvc.perform(MockMvcRequestBuilders.get("/api/menu/main")
                .sessionAttr("sessionContext", sessionContext))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menuOptions[0].optionNumber").value("1"))
                .andExpect(jsonPath("$.menuOptions[0].description").value("View Account Information"))
                .andExpect(jsonPath("$.menuOptions[0].transactionCode").value("COACTVWC"))
                .andExpect(jsonPath("$.menuOptions[0].enabled").value(true))
                .andExpect(jsonPath("$.menuOptions[1].optionNumber").value("2"))
                .andExpect(jsonPath("$.menuOptions[1].description").value("View Card Information"))
                .andExpect(jsonPath("$.menuOptions[1].transactionCode").value("COCRDLIC"));

        verify(menuService).processMenuRequest(any(SessionContext.class));
    }

    /**
     * Test role-based menu filtering functionality.
     * Maps to COBOL user type validation from COMEN01C lines 136-143.
     * Validates admin-only options are filtered for regular users.
     */
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testRoleBasedMenuFiltering() throws Exception {
        // Arrange - Regular user session
        SessionContext userSession = sessionTestUtils.createTestSession("testuser", "USER");
        MenuResponse filteredResponse = createRoleFilteredMenuResponse();

        when(menuService.processMenuRequest(any(SessionContext.class)))
            .thenReturn(filteredResponse);

        // Act & Assert - Verify admin options are excluded
        mockMvc.perform(MockMvcRequestBuilders.get("/api/menu/main")
                .sessionAttr("sessionContext", userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menuOptions[?(@.accessLevel == 'A')]").doesNotExist())
                .andExpect(jsonPath("$.menuOptions[?(@.accessLevel == 'U')]").exists());

        verify(menuService).processMenuRequest(any(SessionContext.class));
    }

    /**
     * Test PF-key navigation functionality.
     * Maps to COBOL DFHENTER and DFHPF3 key handling.
     * Validates navigation patterns equivalent to CICS key processing.
     */
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testPFKeyNavigation() throws Exception {
        // Arrange - Session with navigation context
        SessionContext sessionContext = sessionTestUtils.createTestSession("testuser", "USER");
        sessionContext.addToNavigationStack("COSGN00C"); // Equivalent to CDEMO-FROM-PROGRAM

        MenuResponse menuResponse = createTestMenuResponse();
        when(menuService.processMenuRequest(any(SessionContext.class)))
            .thenReturn(menuResponse);
        when(menuService.processMenuSelection(any(SessionContext.class), eq("3")))
            .thenReturn("COSGN00C"); // F3 key returns to sign-on

        // Act & Assert - Test F3 key equivalent (exit to sign-on)
        mockMvc.perform(MockMvcRequestBuilders.post("/api/menu/navigate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"F3\"}")
                .sessionAttr("sessionContext", sessionContext))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextProgram").value("COSGN00C"));

        // Test ENTER key equivalent (option selection)
        mockMvc.perform(MockMvcRequestBuilders.post("/api/menu/navigate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"ENTER\", \"option\":\"1\"}")
                .sessionAttr("sessionContext", sessionContext))
                .andExpect(status().isOk());

        verify(menuService, times(2)).processMenuSelection(any(SessionContext.class), anyString());
    }

    /**
     * Test session context preservation across menu navigation.
     * Maps to COBOL COMMAREA state management functionality.
     * Validates session state equivalent to CICS COMMAREA preservation.
     */
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testSessionContextPreservation() throws Exception {
        // Arrange - Create session with specific state
        SessionContext originalSession = sessionTestUtils.createTestSession("testuser", "USER");
        originalSession.addToNavigationStack("COMEN01C");
        originalSession.getTransientData().put("selectedOption", "2");

        MenuResponse menuResponse = createTestMenuResponse();
        when(menuService.processMenuRequest(any(SessionContext.class)))
            .thenReturn(menuResponse);

        // Act - Make request that should preserve session state
        mockMvc.perform(MockMvcRequestBuilders.get("/api/menu/main")
                .sessionAttr("sessionContext", originalSession))
                .andExpect(status().isOk());

        // Assert - Verify session state is maintained
        sessionTestUtils.validateSessionState(originalSession, "testuser", "USER");
        verify(menuService).processMenuRequest(any(SessionContext.class));
    }

    /**
     * Test error handling for invalid menu selections and validation failures.
     * Maps to COBOL error handling from PROCESS-ENTER-KEY paragraph.
     * Validates proper error messages equivalent to COBOL validation.
     */
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testErrorHandling() throws Exception {
        // Arrange - Session context for error scenarios
        SessionContext sessionContext = sessionTestUtils.createTestSession("testuser", "USER");

        // Test invalid option number (equivalent to COBOL lines 127-134)
        MenuResponse errorResponse = createErrorMenuResponse("Please enter a valid option number...");
        when(menuService.processMenuRequest(any(SessionContext.class)))
            .thenReturn(errorResponse);

        // Act & Assert - Test invalid option selection
        mockMvc.perform(MockMvcRequestBuilders.post("/api/menu/select")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"option\":\"99\"}")
                .sessionAttr("sessionContext", sessionContext))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Please enter a valid option number..."));

        // Test non-numeric option (equivalent to COBOL WS-OPTION IS NOT NUMERIC)
        mockMvc.perform(MockMvcRequestBuilders.post("/api/menu/select")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"option\":\"ABC\"}")
                .sessionAttr("sessionContext", sessionContext))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").exists());

        // Test admin-only option access by regular user (equivalent to COBOL lines 136-143)
        MenuResponse accessDeniedResponse = createErrorMenuResponse("No access - Admin Only option... ");
        when(menuService.processMenuSelection(any(SessionContext.class), eq("admin-option")))
            .thenReturn(accessDeniedResponse);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/menu/select")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"option\":\"admin-option\"}")
                .sessionAttr("sessionContext", sessionContext))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorMessage").value("No access - Admin Only option... "));

        verify(menuService, atLeastOnce()).processMenuRequest(any(SessionContext.class));
    }

    /**
     * Test menu controller transitions equivalent to COBOL XCTL operations.
     * Maps to CICS XCTL program transfers from PROCESS-ENTER-KEY paragraph.
     * Validates proper program flow and navigation state management.
     */
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testMenuControllerTransitions() throws Exception {
        // Arrange - Session with navigation context
        SessionContext sessionContext = sessionTestUtils.createTestSession("testuser", "USER");
        sessionContext.addToNavigationStack("COMEN01C");

        // Mock successful program transition (equivalent to XCTL)
        when(menuService.processMenuSelection(any(SessionContext.class), eq("1")))
            .thenReturn("COACTVWC"); // Navigate to account view program

        // Act & Assert - Test program transition
        mockMvc.perform(MockMvcRequestBuilders.post("/api/menu/select")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"option\":\"1\"}")
                .sessionAttr("sessionContext", sessionContext))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextProgram").value("COACTVWC"))
                .andExpect(jsonPath("$.sessionContext.navigationStack").isArray())
                .andExpect(jsonPath("$.sessionContext.navigationStack[0]").value("COMEN01C"));

        // Test return to sign-on (equivalent to RETURN-TO-SIGNON-SCREEN)
        when(menuService.processMenuSelection(any(SessionContext.class), eq("F3")))
            .thenReturn("COSGN00C");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/menu/navigate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"F3\"}")
                .sessionAttr("sessionContext", sessionContext))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextProgram").value("COSGN00C"));

        verify(menuService).processMenuSelection(any(SessionContext.class), eq("1"));
        verify(menuService).processMenuSelection(any(SessionContext.class), eq("F3"));
    }

    // Helper Methods for Test Data Creation

    /**
     * Creates test menu options equivalent to COBOL menu structure.
     * Maps to BUILD-MENU-OPTIONS paragraph functionality.
     */
    private List<MenuOption> createTestMenuOptions() {
        return Arrays.asList(
            testDataBuilder.buildMenuOption("1", "View Account Information", "COACTVWC", true, "U"),
            testDataBuilder.buildMenuOption("2", "View Card Information", "COCRDLIC", true, "U"),
            testDataBuilder.buildMenuOption("3", "View Transaction Information", "COTRN00C", true, "U"),
            testDataBuilder.buildMenuOption("4", "Admin Functions", "COADM01C", false, "A") // Admin only
        );
    }

    /**
     * Creates a standard menu response for testing.
     * Equivalent to COBOL SEND MAP operation result.
     */
    private MenuResponse createTestMenuResponse() {
        MenuResponse response = testDataBuilder.createMenuResponse();
        response.setMenuOptions(createTestMenuOptions());
        response.setUserName("testuser");
        response.setErrorMessage("");
        return response;
    }

    /**
     * Creates admin menu response for admin-specific testing.
     * Maps to COADM01C program functionality.
     */
    private AdminMenuResponse createTestAdminMenuResponse() {
        AdminMenuResponse adminResponse = new AdminMenuResponse();
        adminResponse.setAdminOptions(Arrays.asList(
            testDataBuilder.buildMenuOption("1", "User Management", "COUSR00C", true, "A"),
            testDataBuilder.buildMenuOption("2", "System Reports", "CORPT00C", true, "A"),
            testDataBuilder.buildMenuOption("3", "Account Administration", "COACTUPC", true, "A")
        ));
        adminResponse.setSystemStatus("ACTIVE");
        adminResponse.setActiveUsers(5);
        return adminResponse;
    }

    /**
     * Creates filtered menu response for regular users.
     * Implements role-based filtering equivalent to COBOL user type validation.
     */
    private MenuResponse createFilteredMenuResponse() {
        MenuResponse response = createTestMenuResponse();
        // Filter out admin-only options (accessLevel = 'A')
        List<MenuOption> filteredOptions = response.getMenuOptions().stream()
            .filter(option -> !"A".equals(option.getAccessLevel()))
            .toList();
        response.setMenuOptions(filteredOptions);
        return response;
    }

    /**
     * Creates detailed menu response for option generation testing.
     * Validates menu structure matches COBOL format.
     */
    private MenuResponse createDetailedMenuResponse() {
        MenuResponse response = createTestMenuResponse();
        response.getMenuOptions().forEach(option -> {
            option.setEnabled(true);
            option.setDescription(option.getDescription() + " - " + option.getTransactionCode());
        });
        return response;
    }

    /**
     * Creates role-filtered menu response for access control testing.
     * Maps to COBOL user type filtering logic.
     */
    private MenuResponse createRoleFilteredMenuResponse() {
        MenuResponse response = new MenuResponse();
        response.setMenuOptions(Arrays.asList(
            testDataBuilder.buildMenuOption("1", "View Account Information", "COACTVWC", true, "U"),
            testDataBuilder.buildMenuOption("2", "View Card Information", "COCRDLIC", true, "U"),
            testDataBuilder.buildMenuOption("3", "View Transaction Information", "COTRN00C", true, "U")
            // Note: Admin options excluded for regular users
        ));
        response.setUserName("testuser");
        return response;
    }

    /**
     * Creates error menu response for validation testing.
     * Maps to COBOL error message handling.
     */
    private MenuResponse createErrorMenuResponse(String errorMessage) {
        MenuResponse response = createTestMenuResponse();
        response.setErrorMessage(errorMessage);
        return response;
    }

    /**
     * Verifies menu response structure and content.
     * Validates equivalent to COBOL screen validation.
     */
    private void verifyMenuResponse(MenuResponse response) {
        assert response != null : "Menu response should not be null";
        assert response.getMenuOptions() != null : "Menu options should not be null";
        assert !response.getMenuOptions().isEmpty() : "Menu options should not be empty";
        assert response.getUserName() != null : "User name should not be null";
        
        // Validate each menu option structure
        response.getMenuOptions().forEach(option -> {
            assert option.getOptionNumber() != null : "Option number should not be null";
            assert option.getDescription() != null : "Option description should not be null";
            assert option.getTransactionCode() != null : "Transaction code should not be null";
            assert option.getAccessLevel() != null : "Access level should not be null";
        });
    }
}
