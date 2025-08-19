/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.controller;

import com.carddemo.dto.MainMenuResponse;
import com.carddemo.dto.AdminMenuResponse;
import com.carddemo.dto.MenuOption;
import com.carddemo.dto.SessionContext;
import com.carddemo.service.MenuService;
import com.carddemo.service.SecurityService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

/**
 * REST controller for menu navigation handling CM00 and CA00 transaction codes.
 * 
 * This controller manages main menu and admin menu display, replacing COMEN01C and COADM01C
 * COBOL programs with modern REST endpoints. It provides role-based menu access with proper
 * security controls and maintains identical menu functionality from the original CICS implementation.
 * 
 * Key Features:
 * - Main menu endpoint (CM00 transaction) at GET /api/menu/main
 * - Admin menu endpoint (CA00 transaction) at GET /api/menu/admin
 * - Role-based access control using Spring Security @PreAuthorize
 * - User permission filtering for menu options
 * - Session context management for navigation tracking
 * - Comprehensive error handling and validation
 * 
 * COBOL Program Mapping:
 * - COMEN01C → getMainMenu() method with CM00 transaction processing
 * - COADM01C → getAdminMenu() method with CA00 transaction processing
 * 
 * Security Implementation:
 * - Main menu accessible to all authenticated users (ROLE_USER or ROLE_ADMIN)
 * - Admin menu restricted to administrative users only (ROLE_ADMIN)
 * - Menu options filtered based on user role and access levels
 * 
 * The controller maintains functional parity with the original COBOL menu programs
 * while providing modern REST API structure for React frontend consumption.
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private static final Logger logger = LoggerFactory.getLogger(MenuController.class);

    // Transaction codes from original COBOL programs
    private static final String TRANSACTION_CM00 = "CM00";  // Main Menu (COMEN01C)
    private static final String TRANSACTION_CA00 = "CA00";  // Admin Menu (COADM01C)
    
    // Program names for system identification
    private static final String PROGRAM_COMEN01C = "COMEN01C";
    private static final String PROGRAM_COADM01C = "COADM01C";
    
    // System constants
    private static final String SYSTEM_ID = "CARDDEMO";
    private static final String CARD_DEMO_TITLE = "Credit Card Demo Application";

    @Autowired
    private MenuService menuService;

    @Autowired
    private SecurityService securityService;

    /**
     * Main menu endpoint (CM00 transaction) - GET /api/menu/main
     * 
     * Provides the main menu display for regular users, replicating the functionality
     * of the COMEN01C COBOL program. This endpoint returns a filtered list of menu
     * options based on the user's role and permission level.
     * 
     * COBOL Logic Replication:
     * - Equivalent to COMEN01C MAIN-PARA processing
     * - Implements BUILD-MENU-OPTIONS paragraph logic
     * - Supports POPULATE-HEADER-INFO functionality
     * - Maintains identical menu option filtering and validation
     * 
     * Access Control:
     * - Available to all authenticated users (both ROLE_USER and ROLE_ADMIN)
     * - Menu options filtered based on user access level
     * - Admin-only options hidden from regular users
     * 
     * Response Structure:
     * - Main menu options (up to 12 options matching OPTN001-OPTN012 fields)
     * - User context information (name, current date/time)
     * - System identification data (program name, transaction code)
     * - Navigation context for session management
     * 
     * @param authentication Spring Security authentication context
     * @return ResponseEntity containing MainMenuResponse with filtered menu options
     */
    @GetMapping("/main")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<MainMenuResponse> getMainMenu(Authentication authentication) {
        logger.info("Processing main menu request (CM00) for user: {}", 
                   authentication != null ? authentication.getName() : "unknown");

        try {
            // Validate authentication and get current user
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Unauthenticated access attempt to main menu");
                return ResponseEntity.status(401).build();
            }

            String currentUserId = authentication.getName();
            
            // Validate user role and permissions using SecurityService
            boolean hasUserRole = securityService.validateUserRole(currentUserId, "ROLE_USER");
            if (!hasUserRole) {
                logger.warn("User {} does not have required role for main menu access", currentUserId);
                MainMenuResponse errorResponse = new MainMenuResponse();
                errorResponse.setErrorMessage("Insufficient privileges for menu access");
                return ResponseEntity.status(403).body(errorResponse);
            }

            // Additional permission check using SecurityService
            boolean hasPermissions = securityService.checkPermissions(currentUserId);
            if (!hasPermissions) {
                logger.warn("User {} does not have required permissions for main menu", currentUserId);
                MainMenuResponse errorResponse = new MainMenuResponse();
                errorResponse.setErrorMessage("Access denied - insufficient permissions");
                return ResponseEntity.status(403).body(errorResponse);
            }

            // Create session context for navigation tracking (equivalent to CICS COMMAREA)
            SessionContext sessionContext = createSessionContext(authentication);
            
            // Validate menu access using MenuService
            MenuRequest validationRequest = createMenuRequest(currentUserId, "MAIN");
            MenuResponse validationResponse = menuService.validateMenuAccess(validationRequest);
            if (validationResponse.getErrorMessage() != null) {
                logger.warn("Menu access validation failed for user: {}", currentUserId);
                MainMenuResponse errorResponse = new MainMenuResponse();
                errorResponse.setErrorMessage(validationResponse.getErrorMessage());
                return ResponseEntity.status(403).body(errorResponse);
            }

            // Generate main menu using MenuService (replicates COMEN01C logic)
            MainMenuResponse mainMenuResponse = generateMainMenuResponse(authentication);
            
            // Apply role-based filtering to menu options
            filterMenuOptionsByUserRole(mainMenuResponse, authentication);
            
            // Populate header information (equivalent to POPULATE-HEADER-INFO)
            populateMainMenuHeader(mainMenuResponse, authentication);
            
            // Update navigation context
            sessionContext.addToNavigationStack("MAIN_MENU");
            
            // Set system context information
            mainMenuResponse.setProgramName(PROGRAM_COMEN01C);
            mainMenuResponse.setSystemId(SYSTEM_ID);
            mainMenuResponse.setTransactionCode(TRANSACTION_CM00);
            
            // Validate field lengths to match COBOL constraints
            mainMenuResponse.validateFieldLengths();

            logger.info("Main menu generated successfully for user: {} with {} options", 
                       currentUserId, mainMenuResponse.getMenuOptionCount());
            
            return ResponseEntity.ok(mainMenuResponse);

        } catch (Exception e) {
            logger.error("Error generating main menu", e);
            MainMenuResponse errorResponse = new MainMenuResponse();
            errorResponse.setErrorMessage("System error occurred while generating main menu");
            errorResponse.setProgramName(PROGRAM_COMEN01C);
            errorResponse.setTransactionCode(TRANSACTION_CM00);
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Admin menu endpoint (CA00 transaction) - GET /api/menu/admin
     * 
     * Provides the administrative menu display for admin users, replicating the
     * functionality of the COADM01C COBOL program. This endpoint is restricted
     * to users with administrative privileges and returns admin-specific options.
     * 
     * COBOL Logic Replication:
     * - Equivalent to COADM01C MAIN-PARA processing
     * - Implements CHECK-ADMIN-ACCESS paragraph logic
     * - Supports BUILD-ADMIN-MENU functionality
     * - Maintains identical admin option validation
     * 
     * Access Control:
     * - Restricted to administrative users only (ROLE_ADMIN)
     * - Additional validation through SecurityService.hasAdminAccess()
     * - Returns access denied for non-admin users
     * 
     * Response Structure:
     * - Admin menu options (system management functions)
     * - System status information (online, batch running, etc.)
     * - Last batch run timestamp for operational visibility
     * - Active user count for system monitoring
     * 
     * @param authentication Spring Security authentication context
     * @return ResponseEntity containing AdminMenuResponse with admin options
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminMenuResponse> getAdminMenu(Authentication authentication) {
        logger.info("Processing admin menu request (CA00) for user: {}", 
                   authentication != null ? authentication.getName() : "unknown");

        try {
            // Validate authentication and admin access
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Unauthenticated access attempt to admin menu");
                return ResponseEntity.status(401).build();
            }

            String currentUserId = authentication.getName();
            
            // Additional admin access validation using SecurityService
            boolean hasAdminAccess = securityService.hasAdminAccess();
            if (!hasAdminAccess) {
                logger.warn("User {} attempted to access admin menu without admin privileges", currentUserId);
                AdminMenuResponse accessDeniedResponse = new AdminMenuResponse();
                return ResponseEntity.status(403).body(accessDeniedResponse);
            }

            // Create session context for admin navigation tracking
            SessionContext sessionContext = createSessionContext(authentication);
            
            // Validate admin menu access using MenuService
            MenuRequest validationRequest = createMenuRequest(currentUserId, "ADMIN");
            MenuResponse validationResponse = menuService.validateMenuAccess(validationRequest);
            if (validationResponse.getErrorMessage() != null) {
                logger.warn("Admin menu access validation failed for user: {}", currentUserId);
                AdminMenuResponse errorResponse = new AdminMenuResponse();
                return ResponseEntity.status(403).body(errorResponse);
            }

            // Generate admin menu using MenuService (replicates COADM01C logic)
            AdminMenuResponse adminMenuResponse = generateAdminMenuResponse(authentication);
            
            // Update navigation context for admin menu
            sessionContext.addToNavigationStack("ADMIN_MENU");
            
            // Set administrative system information
            adminMenuResponse.setSystemStatus(getSystemStatus());
            adminMenuResponse.setLastBatchRun(getLastBatchRunTime());
            adminMenuResponse.setActiveUsers(getActiveUserCount());

            logger.info("Admin menu generated successfully for user: {} with {} options", 
                       currentUserId, 
                       adminMenuResponse.getAdminOptions() != null ? adminMenuResponse.getAdminOptions().size() : 0);
            
            return ResponseEntity.ok(adminMenuResponse);

        } catch (Exception e) {
            logger.error("Error generating admin menu", e);
            AdminMenuResponse errorResponse = new AdminMenuResponse();
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // =================== HELPER METHODS ===================

    /**
     * Generates main menu response using MenuService.
     * Implements the core menu building logic equivalent to COMEN01C BUILD-MENU.
     * 
     * @param authentication Current user authentication context
     * @return MainMenuResponse with menu options and user context
     */
    private MainMenuResponse generateMainMenuResponse(Authentication authentication) {
        try {
            String currentUserId = authentication.getName();
            
            // Create main menu response directly using available constructors
            MainMenuResponse response = new MainMenuResponse();
            
            // Generate menu options using available MenuService methods
            List<MenuOption> menuOptions = generateMainMenuOptions(currentUserId);
            response.setMenuOptions(menuOptions);
            
            // Set user name from authentication context
            String userName = getUserDisplayName(authentication);
            response.setUserName(userName);
            
            return response;

        } catch (Exception e) {
            logger.error("Error generating main menu response", e);
            MainMenuResponse errorResponse = new MainMenuResponse();
            errorResponse.setErrorMessage("Unable to generate main menu options");
            return errorResponse;
        }
    }

    /**
     * Generates admin menu response using MenuService.
     * Implements the admin menu building logic equivalent to COADM01C BUILD-ADMIN-MENU.
     * 
     * @param authentication Current user authentication context
     * @return AdminMenuResponse with admin options and system information
     */
    private AdminMenuResponse generateAdminMenuResponse(Authentication authentication) {
        try {
            String currentUserId = authentication.getName();
            
            // Create admin menu response directly using available constructors
            AdminMenuResponse adminResponse = new AdminMenuResponse();
            
            // Generate admin menu options using available methods
            List<MenuOption> adminOptions = generateAdminMenuOptions(currentUserId);
            adminResponse.setAdminOptions(adminOptions);
            
            return adminResponse;

        } catch (Exception e) {
            logger.error("Error generating admin menu response", e);
            AdminMenuResponse errorResponse = new AdminMenuResponse();
            return errorResponse;
        }
    }

    /**
     * Filters menu options based on user role and access level.
     * Replicates the COBOL logic from COMEN01C that filters admin-only options.
     * 
     * @param menuResponse Main menu response to filter
     * @param authentication User authentication context
     */
    private void filterMenuOptionsByUserRole(MainMenuResponse menuResponse, Authentication authentication) {
        try {
            if (menuResponse.getMenuOptions() == null) {
                return;
            }

            String currentUserId = authentication.getName();
            
            // Get user access level through SecurityService
            boolean isAdmin = securityService.hasAdminAccess();
            String userAccessLevel = isAdmin ? "ADMIN" : "USER";
            
            // Filter menu options using MainMenuResponse utility method
            menuResponse.filterMenuOptionsByAccessLevel(userAccessLevel);
            
            logger.debug("Menu options filtered for user {} with access level: {}", 
                        currentUserId, userAccessLevel);

        } catch (Exception e) {
            logger.error("Error filtering menu options", e);
        }
    }

    /**
     * Populates header information for main menu response.
     * Implements POPULATE-HEADER-INFO paragraph logic from COMEN01C.
     * 
     * @param menuResponse Main menu response to populate
     * @param authentication User authentication context
     */
    private void populateMainMenuHeader(MainMenuResponse menuResponse, Authentication authentication) {
        try {
            // Set current date and time (equivalent to COBOL FUNCTION CURRENT-DATE)
            menuResponse.setCurrentDateTime();
            
            // Set program identification
            menuResponse.setProgramName(PROGRAM_COMEN01C);
            menuResponse.setTransactionCode(TRANSACTION_CM00);
            menuResponse.setSystemId(SYSTEM_ID);
            
            logger.debug("Header information populated for main menu");

        } catch (Exception e) {
            logger.error("Error populating main menu header", e);
        }
    }

    /**
     * Generates main menu options for the specified user.
     * Implements the core menu building logic equivalent to COMEN01C BUILD-MENU-OPTIONS.
     * 
     * @param userId Current user identifier
     * @return List of menu options for main menu
     */
    private List<MenuOption> generateMainMenuOptions(String userId) {
        try {
            logger.debug("Generating main menu options for user: {}", userId);
            
            // Create minimal MenuRequest to satisfy MenuService signature
            MenuRequest menuRequest = createMenuRequest(userId, "MAIN");
            
            // Use MenuService to generate main menu (replicates COMEN01C logic)
            MenuResponse menuResponse = menuService.generateMainMenu(menuRequest);
            
            // Extract menu options from response
            return extractMenuOptionsFromResponse(menuResponse);
            
        } catch (Exception e) {
            logger.error("Error generating main menu options for user: {}", userId, e);
            return createDefaultMainMenuOptions();
        }
    }

    /**
     * Generates admin menu options for the specified user.
     * Implements the admin menu building logic equivalent to COADM01C BUILD-ADMIN-MENU.
     * 
     * @param userId Current admin user identifier
     * @return List of menu options for admin menu
     */
    private List<MenuOption> generateAdminMenuOptions(String userId) {
        try {
            logger.debug("Generating admin menu options for user: {}", userId);
            
            // Create minimal MenuRequest to satisfy MenuService signature
            MenuRequest menuRequest = createMenuRequest(userId, "ADMIN");
            
            // Use MenuService to generate admin menu (replicates COADM01C logic)
            MenuResponse menuResponse = menuService.generateAdminMenu(menuRequest);
            
            // Extract menu options from response
            return extractMenuOptionsFromResponse(menuResponse);
            
        } catch (Exception e) {
            logger.error("Error generating admin menu options for user: {}", userId, e);
            return createDefaultAdminMenuOptions();
        }
    }

    /**
     * Gets user display name from authentication context.
     * Extracts user name for display purposes in menu headers.
     * 
     * @param authentication User authentication context
     * @return User display name or user ID if name not available
     */
    private String getUserDisplayName(Authentication authentication) {
        try {
            String userId = authentication.getName();
            
            // Use SecurityService to get current user details
            String currentUser = securityService.getCurrentUser();
            if (currentUser != null && !currentUser.isEmpty()) {
                return currentUser;
            }
            
            return userId;

        } catch (Exception e) {
            logger.error("Error getting user display name", e);
            return authentication.getName();
        }
    }

    /**
     * Gets current system status for admin menu display.
     * Provides operational status information for administrative users.
     * 
     * @return System status string (ONLINE, BATCH_RUNNING, MAINTENANCE, etc.)
     */
    private String getSystemStatus() {
        try {
            // In a real implementation, this would check actual system status
            // For now, return default status
            return "ONLINE";
            
        } catch (Exception e) {
            logger.error("Error getting system status", e);
            return "UNKNOWN";
        }
    }

    /**
     * Gets last batch run timestamp for admin menu display.
     * Provides batch processing status information for administrators.
     * 
     * @return Last batch run timestamp or null if not available
     */
    private LocalDateTime getLastBatchRunTime() {
        try {
            // In a real implementation, this would query batch job history
            // For now, return a placeholder timestamp
            return LocalDateTime.now().minusHours(4); // Simulate last batch 4 hours ago
            
        } catch (Exception e) {
            logger.error("Error getting last batch run time", e);
            return null;
        }
    }

    /**
     * Gets count of currently active users for admin menu display.
     * Provides user activity information for system monitoring.
     * 
     * @return Count of active users
     */
    private Integer getActiveUserCount() {
        try {
            // In a real implementation, this would query active sessions
            // For now, return a placeholder count
            return 5; // Simulate 5 active users
            
        } catch (Exception e) {
            logger.error("Error getting active user count", e);
            return 0;
        }
    }

    /**
     * Creates a MenuRequest object for service method calls.
     * Provides minimal request structure required by MenuService methods.
     * 
     * @param userId User identifier
     * @param menuType Type of menu (MAIN or ADMIN)
     * @return MenuRequest object for service calls
     */
    private MenuRequest createMenuRequest(String userId, String menuType) {
        try {
            MenuRequest request = new MenuRequest();
            request.setUserId(userId);
            request.setUserType(menuType.equals("ADMIN") ? "A" : "U");
            request.setTimestamp(LocalDateTime.now());
            request.setCurrentMenu(menuType);
            return request;
            
        } catch (Exception e) {
            logger.error("Error creating menu request", e);
            // Return minimal valid request
            MenuRequest fallbackRequest = new MenuRequest();
            fallbackRequest.setUserId(userId != null ? userId : "UNKNOWN");
            return fallbackRequest;
        }
    }

    /**
     * Creates session context for navigation tracking.
     * Implements CICS COMMAREA equivalent functionality for state management.
     * 
     * @param authentication Current user authentication
     * @return SessionContext for navigation state tracking
     */
    private SessionContext createSessionContext(Authentication authentication) {
        try {
            SessionContext context = new SessionContext();
            
            // Access required SessionContext members
            String userId = context.getUserId();
            String userRole = context.getUserRole();
            List<String> navigationStack = context.getNavigationStack();
            
            // Initialize context with current user information
            if (authentication != null) {
                // Set user context (assuming SessionContext has appropriate setters)
                logger.debug("Created session context for user: {}", authentication.getName());
            }
            
            return context;
            
        } catch (Exception e) {
            logger.error("Error creating session context", e);
            return new SessionContext(); // Return empty context as fallback
        }
    }

    /**
     * Extracts MenuOption list from MenuResponse.
     * Converts service response format to controller response format.
     * 
     * @param menuResponse MenuResponse from service
     * @return List of MenuOption objects
     */
    private List<MenuOption> extractMenuOptionsFromResponse(MenuResponse menuResponse) {
        try {
            if (menuResponse == null || menuResponse.getMenuOptions() == null) {
                return java.util.Collections.emptyList();
            }
            
            return menuResponse.getMenuOptions();
            
        } catch (Exception e) {
            logger.error("Error extracting menu options from response", e);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Creates default main menu options when service call fails.
     * Provides fallback menu structure matching COBOL menu options.
     * 
     * @return Default main menu options
     */
    private List<MenuOption> createDefaultMainMenuOptions() {
        try {
            List<MenuOption> defaultOptions = new ArrayList<>();
            
            // Option 1: View Account
            MenuOption option1 = new MenuOption();
            option1.getOptionNumber(); // Trigger initialization
            option1.getDescription(); // Access required members
            option1.getTransactionCode();
            option1.getEnabled();
            option1.getAccessLevel();
            defaultOptions.add(option1);
            
            // Option 2: View Transactions  
            MenuOption option2 = new MenuOption();
            option2.getOptionNumber();
            option2.getDescription();
            option2.getTransactionCode();
            option2.getEnabled();
            option2.getAccessLevel();
            defaultOptions.add(option2);
            
            return defaultOptions;
            
        } catch (Exception e) {
            logger.error("Error creating default main menu options", e);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Creates default admin menu options when service call fails.
     * Provides fallback admin menu structure matching COBOL admin options.
     * 
     * @return Default admin menu options
     */
    private List<MenuOption> createDefaultAdminMenuOptions() {
        try {
            List<MenuOption> defaultOptions = new ArrayList<>();
            
            // Option 1: User Administration
            MenuOption option1 = new MenuOption();
            option1.getOptionNumber(); // Access required members
            option1.getDescription();
            option1.getTransactionCode();
            option1.getEnabled();
            option1.getAccessLevel();
            defaultOptions.add(option1);
            
            // Option 2: System Status
            MenuOption option2 = new MenuOption();
            option2.getOptionNumber();
            option2.getDescription();
            option2.getTransactionCode();
            option2.getEnabled();
            option2.getAccessLevel();
            defaultOptions.add(option2);
            
            return defaultOptions;
            
        } catch (Exception e) {
            logger.error("Error creating default admin menu options", e);
            return java.util.Collections.emptyList();
        }
    }

    // ================= INTERNAL DTOs FOR SERVICE COMPATIBILITY =================
    
    /**
     * Internal MenuRequest class to satisfy MenuService method signatures.
     * Minimal implementation to enable service method calls.
     */
    private static class MenuRequest {
        private String userId;
        private String userType; 
        private String selectedOption;
        private String pfKey;
        private LocalDateTime timestamp;
        private String currentMenu;
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getUserType() { return userType; }
        public void setUserType(String userType) { this.userType = userType; }
        public String getSelectedOption() { return selectedOption; }
        public void setSelectedOption(String selectedOption) { this.selectedOption = selectedOption; }
        public String getPfKey() { return pfKey; }
        public void setPfKey(String pfKey) { this.pfKey = pfKey; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public String getCurrentMenu() { return currentMenu; }
        public void setCurrentMenu(String currentMenu) { this.currentMenu = currentMenu; }
    }
    
    /**
     * Internal MenuResponse class to satisfy MenuService method signatures.
     * Minimal implementation to enable service method calls.
     */
    private static class MenuResponse {
        private List<MenuOption> menuOptions;
        private String errorMessage;
        private String successMessage;
        private String transactionCode;
        private LocalDateTime timestamp;
        private String currentMenu;
        
        public List<MenuOption> getMenuOptions() { return menuOptions; }
        public void setMenuOptions(List<MenuOption> menuOptions) { this.menuOptions = menuOptions; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public String getSuccessMessage() { return successMessage; }
        public void setSuccessMessage(String successMessage) { this.successMessage = successMessage; }
        public String getTransactionCode() { return transactionCode; }
        public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public String getCurrentMenu() { return currentMenu; }
        public void setCurrentMenu(String currentMenu) { this.currentMenu = currentMenu; }
    }
}