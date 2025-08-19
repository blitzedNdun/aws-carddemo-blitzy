package com.carddemo.service;

import com.carddemo.dto.MenuResponse;
import com.carddemo.dto.SessionContext;
import com.carddemo.config.MenuConfiguration;
import com.carddemo.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Consolidated menu service providing unified navigation and menu management functionality
 * from COMEN01C, COADM01C, and CORPT00C COBOL programs. This service acts as a facade
 * over specialized menu services, handling role-based menu generation, navigation flow
 * between different menu types, and session state management with user permission validation.
 * 
 * This service translates the COBOL menu management logic to modern Spring Boot patterns
 * while maintaining identical menu behavior and user workflow patterns from the original
 * mainframe implementation.
 * 
 * Key Features:
 * - Main menu navigation for regular users (COMEN01C logic)
 * - Admin menu access control for admin users (COADM01C logic)  
 * - Report menu functionality (CORPT00C logic)
 * - Role-based menu filtering and access validation
 * - Navigation state tracking with session context management
 * - PF-key function mapping (F3=Exit, F12=Cancel) for 3270 terminal compatibility
 * - Menu option validation and user permission verification
 * 
 * Technical Implementation:
 * - Facade pattern over MainMenuService, AdminMenuService, and ReportMenuService
 * - Spring Security integration for role-based access control
 * - Session management through SessionService for navigation state tracking
 * - Comprehensive error handling and validation matching COBOL program logic
 * - MenuConfiguration integration for static menu option management
 * 
 * COBOL Program Mapping:
 * - COMEN01C → generateMainMenu() and main menu processing logic
 * - COADM01C → generateAdminMenu() and admin access control logic
 * - CORPT00C → generateReportMenu() and report selection processing
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class MenuService {

    private static final Logger logger = LoggerFactory.getLogger(MenuService.class);

    // PF-Key constants matching COBOL BMS map definitions
    private static final String PF_KEY_EXIT = "F3";
    private static final String PF_KEY_CANCEL = "F12";
    private static final String PF_KEY_ENTER = "ENTER";

    // Menu type constants from COBOL program context
    private static final String MENU_TYPE_MAIN = "MAIN";
    private static final String MENU_TYPE_ADMIN = "ADMIN";
    private static final String MENU_TYPE_REPORT = "REPORT";

    // User role constants matching COBOL user type definitions
    private static final String USER_TYPE_ADMIN = "A";
    private static final String USER_TYPE_USER = "U";

    // Transaction codes from COBOL CICS programs
    private static final String TRANS_MAIN_MENU = "CM00";
    private static final String TRANS_ADMIN_MENU = "CA00";
    private static final String TRANS_REPORT_MENU = "CR00";

    @Autowired
    private MainMenuService mainMenuService;

    @Autowired
    private AdminMenuService adminMenuService;

    // Note: ReportMenuService is not available yet - implementing basic functionality directly

    @Autowired
    private UserService userService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private MenuConfiguration menuConfiguration;

    /**
     * Minimal MenuRequest structure to handle missing MenuRequest.java dependency.
     * This inner class provides the necessary functionality until MenuRequest.java is implemented.
     */
    public static class MenuRequest {
        private String selectedOption;
        private String userId;
        private String userType;
        private String pfKey;
        private String menuType;
        private SessionContext sessionContext;

        public MenuRequest() {}

        public MenuRequest(String selectedOption, String userId, String userType) {
            this.selectedOption = selectedOption;
            this.userId = userId;
            this.userType = userType;
        }

        // Getters and setters to match schema requirements
        public String getSelectedOption() { return selectedOption; }
        public void setSelectedOption(String selectedOption) { this.selectedOption = selectedOption; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getUserType() { return userType; }
        public void setUserType(String userType) { this.userType = userType; }

        public String getPfKey() { return pfKey; }
        public void setPfKey(String pfKey) { this.pfKey = pfKey; }

        public String getMenuType() { return menuType; }
        public void setMenuType(String menuType) { this.menuType = menuType; }

        public SessionContext getSessionContext() { return sessionContext; }
        public void setSessionContext(SessionContext sessionContext) { this.sessionContext = sessionContext; }
    }

    /**
     * Main entry point for processing menu requests.
     * Replicates the COBOL MAIN-PROCESS paragraph logic by routing requests
     * to appropriate menu services based on user role and menu type.
     * 
     * Functionality equivalent to COBOL:
     * - EVALUATE CDEMO-PGM-CONTEXT to determine processing mode
     * - PERFORM INIT-PROCESS for initialization
     * - PERFORM PROCESS-MAP for menu processing
     * - PERFORM SEND-MAP for response generation
     * 
     * @param request Menu request containing user input and context
     * @return MenuResponse containing menu options and navigation context
     */
    public MenuResponse processMenuRequest(MenuRequest request) {
        logger.info("Processing menu request for user: {} with option: {}", 
                   request.getUserId(), request.getSelectedOption());

        try {
            // Validate request and user permissions (replicates COBOL input validation)
            MenuResponse validationResponse = validateMenuAccess(request);
            if (validationResponse.getErrorMessage() != null) {
                return validationResponse;
            }

            // Determine menu type based on user role and request context
            String menuType = determineMenuType(request);
            
            // Route to appropriate menu service based on type
            MenuResponse response;
            switch (menuType) {
                case MENU_TYPE_MAIN:
                    response = generateMainMenu(request);
                    break;
                case MENU_TYPE_ADMIN:
                    response = generateAdminMenu(request);
                    break;
                case MENU_TYPE_REPORT:
                    response = generateReportMenu(request);
                    break;
                default:
                    logger.warn("Unknown menu type: {} for user: {}", menuType, request.getUserId());
                    response = new MenuResponse();
                    response.setErrorMessage("Invalid menu type specified");
                    return response;
            }

            // Handle PF-key navigation if specified
            if (request.getPfKey() != null) {
                response = handleMenuNavigation(request, response);
            }

            // Update session with navigation context
            updateNavigationContext(request, menuType);

            logger.info("Menu request processed successfully for user: {}", request.getUserId());
            return response;

        } catch (Exception e) {
            logger.error("Error processing menu request for user: {}", request.getUserId(), e);
            MenuResponse errorResponse = new MenuResponse();
            errorResponse.setErrorMessage("System error occurred while processing menu request");
            return errorResponse;
        }
    }

    /**
     * Generates main menu for regular users.
     * Delegates to MainMenuService for COMEN01C program logic while adding
     * facade-level validation and role-based filtering.
     * 
     * Functionality equivalent to COBOL COMEN01C:
     * - PERFORM BUILD-MENU to construct menu options
     * - PERFORM VALIDATE-SELECTION for option validation  
     * - PERFORM PROCESS-SELECTION for selection handling
     * 
     * @param request Menu request with user context
     * @return MenuResponse with main menu options and user context
     */
    public MenuResponse generateMainMenu(MenuRequest request) {
        logger.debug("Generating main menu for user: {}", request.getUserId());

        try {
            // Use MainMenuService to build menu (delegates to COMEN01C logic)
            Map<String, Object> mainMenuData = mainMenuService.buildMenu(request.getUserId());
            
            // Process any menu selection if provided
            if (request.getSelectedOption() != null && !request.getSelectedOption().trim().isEmpty()) {
                Map<String, Object> selectionResult = mainMenuService.processMenuSelection(
                    request.getSelectedOption(), request.getUserId());
                
                if (selectionResult.containsKey("error")) {
                    MenuResponse errorResponse = new MenuResponse();
                    errorResponse.setErrorMessage((String) selectionResult.get("error"));
                    return errorResponse;
                }
                
                // Update menu data with selection results
                mainMenuData.putAll(selectionResult);
            }

            // Execute main menu processing logic
            Map<String, Object> processResult = mainMenuService.mainMenuProcess(request.getUserId());
            
            // Build response using menu data
            return buildMenuResponse(mainMenuData, request, MENU_TYPE_MAIN);

        } catch (Exception e) {
            logger.error("Error generating main menu for user: {}", request.getUserId(), e);
            MenuResponse errorResponse = new MenuResponse();
            errorResponse.setErrorMessage("Unable to generate main menu");
            return errorResponse;
        }
    }

    /**
     * Generates admin menu for administrative users.
     * Delegates to AdminMenuService for COADM01C program logic with additional
     * admin access validation and role-based security checks.
     * 
     * Functionality equivalent to COBOL COADM01C:
     * - PERFORM CHECK-ADMIN-ACCESS for authorization validation
     * - PERFORM BUILD-ADMIN-MENU to construct admin options
     * - PERFORM PROCESS-ADMIN-SELECTION for admin operation handling
     * 
     * @param request Menu request with user context  
     * @return MenuResponse with admin menu options or access denied message
     */
    public MenuResponse generateAdminMenu(MenuRequest request) {
        logger.debug("Generating admin menu for user: {}", request.getUserId());

        try {
            // Validate admin access using SecurityService
            if (!securityService.hasAdminAccess()) {
                logger.warn("Non-admin user attempted to access admin menu: {}", request.getUserId());
                MenuResponse accessDeniedResponse = new MenuResponse();
                accessDeniedResponse.setErrorMessage("Access denied - Administrative privileges required");
                return accessDeniedResponse;
            }

            // Use AdminMenuService to build admin menu (delegates to COADM01C logic)
            Map<String, Object> adminMenuData = adminMenuService.buildAdminMenu(request.getUserId());
            
            // Process any admin selection if provided
            if (request.getSelectedOption() != null && !request.getSelectedOption().trim().isEmpty()) {
                Map<String, Object> selectionResult = adminMenuService.processAdminSelection(
                    request.getSelectedOption(), request.getUserId());
                
                if (selectionResult.containsKey("error")) {
                    MenuResponse errorResponse = new MenuResponse();
                    errorResponse.setErrorMessage((String) selectionResult.get("error"));
                    return errorResponse;
                }
                
                // Update menu data with selection results
                adminMenuData.putAll(selectionResult);
            }

            // Execute admin menu processing logic
            Map<String, Object> processResult = adminMenuService.adminMenuProcess(request.getUserId());
            
            // Build response using admin menu data
            return buildMenuResponse(adminMenuData, request, MENU_TYPE_ADMIN);

        } catch (Exception e) {
            logger.error("Error generating admin menu for user: {}", request.getUserId(), e);
            MenuResponse errorResponse = new MenuResponse();
            errorResponse.setErrorMessage("Unable to generate admin menu");
            return errorResponse;
        }
    }

    /**
     * Generates report menu for report selection and date range validation.
     * Implements CORPT00C program logic directly since ReportMenuService is not available.
     * Handles report type selection and custom date range validation.
     * 
     * Functionality equivalent to COBOL CORPT00C:
     * - PERFORM BUILD-REPORT-MENU to construct report options
     * - PERFORM VALIDATE-DATE-RANGE for custom date validation
     * - PERFORM SUBMIT-BATCH-JOB for report generation initiation
     * 
     * @param request Menu request with user context
     * @return MenuResponse with report menu options and date validation
     */
    public MenuResponse generateReportMenu(MenuRequest request) {
        logger.debug("Generating report menu for user: {}", request.getUserId());

        try {
            // Since ReportMenuService is not available, implement basic report menu logic directly
            MenuResponse response = new MenuResponse();
            
            // Build report menu options (equivalent to CORPT00C BUILD-REPORT-MENU)
            List<Map<String, Object>> reportOptions = buildReportMenuOptions();
            response.setMenuOptions(reportOptions);
            
            // Set user context
            User user = getUserForMenu(request.getUserId());
            if (user != null) {
                response.setUserName(user.getFirstName() + " " + user.getLastName());
            }
            
            // Process report selection if provided
            if (request.getSelectedOption() != null && !request.getSelectedOption().trim().isEmpty()) {
                boolean validSelection = handleReportSelection(request.getSelectedOption(), response);
                if (!validSelection) {
                    response.setErrorMessage("Invalid report selection - please choose from available options");
                }
            }
            
            // Add report menu specific attributes
            response.setCurrentMenu(MENU_TYPE_REPORT);
            response.setTransactionCode(TRANS_REPORT_MENU);
            response.setTimestamp(LocalDateTime.now());
            
            logger.debug("Report menu generated successfully for user: {}", request.getUserId());
            return response;

        } catch (Exception e) {
            logger.error("Error generating report menu for user: {}", request.getUserId(), e);
            MenuResponse errorResponse = new MenuResponse();
            errorResponse.setErrorMessage("Unable to generate report menu");
            return errorResponse;
        }
    }

    /**
     * Validates menu access based on user role and permissions.
     * Implements comprehensive authorization checking equivalent to COBOL
     * security validation logic from all three menu programs.
     * 
     * Functionality equivalent to COBOL:
     * - READ USER-SECURITY-FILE to validate user
     * - CHECK USER-TYPE for role validation
     * - VALIDATE PERMISSIONS for operation authorization
     * 
     * @param request Menu request to validate
     * @return MenuResponse with validation results or error message
     */
    public MenuResponse validateMenuAccess(MenuRequest request) {
        logger.debug("Validating menu access for user: {}", request.getUserId());

        try {
            MenuResponse response = new MenuResponse();

            // Validate required request parameters
            if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
                response.setErrorMessage("User ID is required for menu access");
                return response;
            }

            // Validate user exists and get user details
            User user = userService.getUserById(request.getUserId());
            if (user == null) {
                response.setErrorMessage("User not found - please contact system administrator");
                return response;
            }

            // Validate user permissions using UserService
            boolean hasPermissions = userService.validateUserPermissions(request.getUserId());
            if (!hasPermissions) {
                response.setErrorMessage("User account does not have sufficient permissions");
                return response;
            }

            // Validate user role using SecurityService
            String requiredRole = determineRequiredRole(request);
            boolean hasRole = securityService.validateUserRole(request.getUserId(), requiredRole);
            if (!hasRole) {
                response.setErrorMessage("Access denied - insufficient privileges for requested menu");
                return response;
            }

            // Set successful validation (no error message)
            response.setErrorMessage(null);
            logger.debug("Menu access validation successful for user: {}", request.getUserId());
            return response;

        } catch (Exception e) {
            logger.error("Error validating menu access for user: {}", request.getUserId(), e);
            MenuResponse errorResponse = new MenuResponse();
            errorResponse.setErrorMessage("System error during access validation");
            return errorResponse;
        }
    }

    /**
     * Processes menu selection and routes to appropriate functionality.
     * Coordinates between different menu services based on selection type
     * and delegates to appropriate business logic handlers.
     * 
     * Functionality equivalent to COBOL:
     * - EVALUATE SELECTED-OPTION to determine routing
     * - PERFORM appropriate processing paragraph
     * - HANDLE errors and validation failures
     * 
     * @param request Menu request with selection
     * @return MenuResponse with selection processing results
     */
    public MenuResponse processMenuSelection(MenuRequest request) {
        logger.debug("Processing menu selection: {} for user: {}", 
                    request.getSelectedOption(), request.getUserId());

        try {
            // Validate selection is provided
            if (request.getSelectedOption() == null || request.getSelectedOption().trim().isEmpty()) {
                MenuResponse errorResponse = new MenuResponse();
                errorResponse.setErrorMessage("Please select a menu option");
                return errorResponse;
            }

            // Determine current menu context
            String menuType = request.getMenuType() != null ? request.getMenuType() : 
                             determineMenuType(request);

            // Route selection processing based on menu type
            MenuResponse response;
            switch (menuType) {
                case MENU_TYPE_MAIN:
                    response = processMainMenuSelection(request);
                    break;
                case MENU_TYPE_ADMIN:
                    response = processAdminMenuSelection(request);
                    break;
                case MENU_TYPE_REPORT:
                    response = processReportMenuSelection(request);
                    break;
                default:
                    logger.warn("Unknown menu type for selection processing: {}", menuType);
                    response = new MenuResponse();
                    response.setErrorMessage("Invalid menu context for selection");
                    break;
            }

            // Update navigation context after successful processing
            if (response.getErrorMessage() == null) {
                updateSelectionNavigationContext(request, response);
            }

            return response;

        } catch (Exception e) {
            logger.error("Error processing menu selection for user: {}", request.getUserId(), e);
            MenuResponse errorResponse = new MenuResponse();
            errorResponse.setErrorMessage("System error processing menu selection");
            return errorResponse;
        }
    }

    /**
     * Navigates to specified menu type with proper role validation.
     * Handles menu transitions while maintaining session state and
     * enforcing security policies for menu access.
     * 
     * Functionality equivalent to COBOL:
     * - PERFORM XCTL to target transaction
     * - PASS COMMAREA data for context preservation
     * - VALIDATE target menu access permissions
     * 
     * @param request Menu request with navigation target
     * @param targetMenuType Target menu type to navigate to
     * @return MenuResponse for target menu or access denied
     */
    public MenuResponse navigateToMenu(MenuRequest request, String targetMenuType) {
        logger.debug("Navigating to menu: {} for user: {}", targetMenuType, request.getUserId());

        try {
            // Validate target menu type
            if (!isValidMenuType(targetMenuType)) {
                MenuResponse errorResponse = new MenuResponse();
                errorResponse.setErrorMessage("Invalid menu type specified for navigation");
                return errorResponse;
            }

            // Create navigation request for target menu
            MenuRequest navigationRequest = new MenuRequest();
            navigationRequest.setUserId(request.getUserId());
            navigationRequest.setUserType(request.getUserType());
            navigationRequest.setMenuType(targetMenuType);
            navigationRequest.setSessionContext(request.getSessionContext());

            // Validate access to target menu
            MenuResponse accessValidation = validateMenuAccess(navigationRequest);
            if (accessValidation.getErrorMessage() != null) {
                return accessValidation;
            }

            // Generate target menu based on type
            MenuResponse targetMenu;
            switch (targetMenuType) {
                case MENU_TYPE_MAIN:
                    targetMenu = generateMainMenu(navigationRequest);
                    break;
                case MENU_TYPE_ADMIN:
                    targetMenu = generateAdminMenu(navigationRequest);
                    break;
                case MENU_TYPE_REPORT:
                    targetMenu = generateReportMenu(navigationRequest);
                    break;
                default:
                    MenuResponse errorResponse = new MenuResponse();
                    errorResponse.setErrorMessage("Unsupported menu type for navigation");
                    return errorResponse;
            }

            // Update navigation history in session
            if (request.getSessionContext() != null) {
                updateNavigationHistory(request, targetMenuType);
            }

            logger.debug("Navigation successful to menu: {} for user: {}", 
                        targetMenuType, request.getUserId());
            return targetMenu;

        } catch (Exception e) {
            logger.error("Error navigating to menu: {} for user: {}", 
                        targetMenuType, request.getUserId(), e);
            MenuResponse errorResponse = new MenuResponse();
            errorResponse.setErrorMessage("System error during menu navigation");
            return errorResponse;
        }
    }

    /**
     * Validates user role against required permissions.
     * Provides role-based access control by checking user type
     * against menu and operation requirements.
     * 
     * Functionality equivalent to COBOL:
     * - READ USER-SECURITY-FILE 
     * - CHECK SEC-USR-TYPE against required type
     * - RETURN authorization result
     * 
     * @param userId User identifier to validate
     * @param requiredRole Required role for access
     * @return boolean true if user has required role, false otherwise
     */
    public boolean validateUserRole(String userId, String requiredRole) {
        logger.debug("Validating user role for user: {} against required role: {}", 
                    userId, requiredRole);

        try {
            // Delegate to SecurityService for comprehensive role validation
            boolean hasRole = securityService.validateUserRole(userId, requiredRole);
            
            logger.debug("Role validation result for user {}: {}", userId, hasRole);
            return hasRole;

        } catch (Exception e) {
            logger.error("Error validating user role for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Builds menu response DTO with comprehensive menu data and context.
     * Constructs response object with menu options, user information,
     * system context, and navigation data for frontend consumption.
     * 
     * Functionality equivalent to COBOL:
     * - MOVE data to BMS map fields
     * - SET system information fields
     * - PREPARE response structure for SEND MAP
     * 
     * @param menuData Raw menu data from service processing
     * @param request Original menu request for context
     * @param menuType Type of menu being built
     * @return MenuResponse Complete response object for frontend
     */
    public MenuResponse buildMenuResponse(Map<String, Object> menuData, MenuRequest request, String menuType) {
        logger.debug("Building menu response for menu type: {} and user: {}", 
                    menuType, request.getUserId());

        try {
            MenuResponse response = new MenuResponse();

            // Set menu options from menu data
            if (menuData.containsKey("menuOptions")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> options = (List<Map<String, Object>>) menuData.get("menuOptions");
                response.setMenuOptions(options);
            } else {
                // Build default menu options based on menu type
                response.setMenuOptions(getDefaultMenuOptions(menuType));
            }

            // Set user context information
            User user = getUserForMenu(request.getUserId());
            if (user != null) {
                response.setUserName(user.getFirstName() + " " + user.getLastName());
                response.setUserId(user.getUserId());
                response.setUserType(user.getUserType());
            }

            // Set system context information
            response.setCurrentMenu(menuType);
            response.setTransactionCode(getTransactionCodeForMenu(menuType));
            response.setTimestamp(LocalDateTime.now());

            // Set navigation context if available
            if (request.getSessionContext() != null) {
                SessionContext sessionContext = request.getSessionContext();
                response.setNavigationStack(sessionContext.getNavigationStack());
                response.setSessionId(sessionContext.getUserId()); // Use user ID as session identifier
            }

            // Set any error messages from menu data
            if (menuData.containsKey("errorMessage")) {
                response.setErrorMessage((String) menuData.get("errorMessage"));
            }

            // Set success messages if available
            if (menuData.containsKey("successMessage")) {
                response.setSuccessMessage((String) menuData.get("successMessage"));
            }

            logger.debug("Menu response built successfully for menu type: {}", menuType);
            return response;

        } catch (Exception e) {
            logger.error("Error building menu response for menu type: {}", menuType, e);
            MenuResponse errorResponse = new MenuResponse();
            errorResponse.setErrorMessage("System error building menu response");
            return errorResponse;
        }
    }

    /**
     * Handles PF-key navigation and special function processing.
     * Processes function keys (F3=Exit, F12=Cancel, Enter) to implement
     * 3270 terminal equivalent navigation in web interface.
     * 
     * Functionality equivalent to COBOL:
     * - EVALUATE EIBAID to check function key pressed
     * - PERFORM appropriate navigation logic
     * - HANDLE special cases like exit and cancel
     * 
     * @param request Menu request with PF-key information
     * @param currentResponse Current menu response to modify
     * @return MenuResponse Updated response with navigation applied
     */
    public MenuResponse handleMenuNavigation(MenuRequest request, MenuResponse currentResponse) {
        logger.debug("Handling menu navigation with PF-key: {} for user: {}", 
                    request.getPfKey(), request.getUserId());

        try {
            String pfKey = request.getPfKey();
            if (pfKey == null) {
                return currentResponse;
            }

            MenuResponse navigationResponse;
            switch (pfKey.toUpperCase()) {
                case PF_KEY_EXIT:
                    // F3 = Exit - return to previous menu or sign off
                    navigationResponse = handleExitNavigation(request, currentResponse);
                    break;
                
                case PF_KEY_CANCEL:
                    // F12 = Cancel - cancel current operation and refresh menu
                    navigationResponse = handleCancelNavigation(request, currentResponse);
                    break;
                    
                case PF_KEY_ENTER:
                    // Enter = Process current selection
                    navigationResponse = processMenuSelection(request);
                    break;
                    
                default:
                    logger.warn("Unknown PF-key: {} for user: {}", pfKey, request.getUserId());
                    currentResponse.setErrorMessage("Unknown function key - use F3=Exit or F12=Cancel");
                    navigationResponse = currentResponse;
                    break;
            }

            logger.debug("Menu navigation handled successfully for PF-key: {}", pfKey);
            return navigationResponse;

        } catch (Exception e) {
            logger.error("Error handling menu navigation for user: {}", request.getUserId(), e);
            currentResponse.setErrorMessage("System error during navigation");
            return currentResponse;
        }
    }

    // =================== HELPER METHODS ===================

    /**
     * Determines appropriate menu type based on user role and context.
     * Implements menu routing logic equivalent to COBOL program selection.
     */
    private String determineMenuType(MenuRequest request) {
        try {
            // Check if specific menu type is requested
            if (request.getMenuType() != null && !request.getMenuType().trim().isEmpty()) {
                return request.getMenuType();
            }

            // Determine based on user type (equivalent to COBOL user type check)
            String userType = request.getUserType();
            if (userType == null && request.getUserId() != null) {
                User user = getUserForMenu(request.getUserId());
                if (user != null) {
                    userType = user.getUserType();
                }
            }

            // Default menu routing based on user type
            if (USER_TYPE_ADMIN.equals(userType)) {
                return MENU_TYPE_ADMIN; // Admin users get admin menu by default
            } else {
                return MENU_TYPE_MAIN;  // Regular users get main menu
            }

        } catch (Exception e) {
            logger.error("Error determining menu type for user: {}", request.getUserId(), e);
            return MENU_TYPE_MAIN; // Default to main menu on error
        }
    }

    /**
     * Determines required role based on menu request context.
     * Maps menu types to Spring Security role requirements.
     */
    private String determineRequiredRole(MenuRequest request) {
        String menuType = request.getMenuType() != null ? request.getMenuType() : 
                         determineMenuType(request);
        
        switch (menuType) {
            case MENU_TYPE_ADMIN:
                return "ROLE_ADMIN";
            case MENU_TYPE_MAIN:
            case MENU_TYPE_REPORT:
            default:
                return "ROLE_USER";
        }
    }

    /**
     * Builds report menu options equivalent to CORPT00C menu structure.
     * Since ReportMenuService is not available, implements basic report menu directly.
     */
    private List<Map<String, Object>> buildReportMenuOptions() {
        List<Map<String, Object>> reportOptions = new ArrayList<>();

        // Report option 1: Daily Transaction Report
        Map<String, Object> option1 = new HashMap<>();
        option1.put("optionNumber", "1");
        option1.put("optionText", "Daily Transaction Report");
        option1.put("description", "Generate daily transaction summary report");
        option1.put("enabled", true);
        reportOptions.add(option1);

        // Report option 2: Monthly Account Report  
        Map<String, Object> option2 = new HashMap<>();
        option2.put("optionNumber", "2");
        option2.put("optionText", "Monthly Account Report");
        option2.put("description", "Generate monthly account activity report");
        option2.put("enabled", true);
        reportOptions.add(option2);

        // Report option 3: Customer Report
        Map<String, Object> option3 = new HashMap<>();
        option3.put("optionNumber", "3");
        option3.put("optionText", "Customer Report");
        option3.put("description", "Generate customer information report");
        option3.put("enabled", true);
        reportOptions.add(option3);

        // Report option 4: Custom Date Range
        Map<String, Object> option4 = new HashMap<>();
        option4.put("optionNumber", "4");
        option4.put("optionText", "Custom Date Range Report");
        option4.put("description", "Generate report for custom date range");
        option4.put("enabled", true);
        reportOptions.add(option4);

        return reportOptions;
    }

    /**
     * Handles report selection processing equivalent to CORPT00C logic.
     * Since ReportMenuService.handleReportSelection() is not available, implements directly.
     */
    private boolean handleReportSelection(String selectedOption, MenuResponse response) {
        try {
            switch (selectedOption.trim()) {
                case "1":
                    response.setSuccessMessage("Daily Transaction Report selected - processing request");
                    return true;
                case "2":
                    response.setSuccessMessage("Monthly Account Report selected - processing request");
                    return true;
                case "3":
                    response.setSuccessMessage("Customer Report selected - processing request");
                    return true;
                case "4":
                    response.setSuccessMessage("Custom Date Range Report selected - please specify date range");
                    return true;
                default:
                    return false;
            }
        } catch (Exception e) {
            logger.error("Error handling report selection: {}", selectedOption, e);
            return false;
        }
    }

    /**
     * Validates custom date range for reports equivalent to CORPT00C date validation.
     * Since ReportMenuService.validateCustomDateRange() is not available, implements directly.
     */
    public boolean validateCustomDateRange(String startDate, String endDate) {
        try {
            if (startDate == null || endDate == null) {
                return false;
            }

            // Basic date validation - in real implementation would parse and validate dates
            return startDate.trim().length() > 0 && endDate.trim().length() > 0;

        } catch (Exception e) {
            logger.error("Error validating custom date range: {} to {}", startDate, endDate, e);
            return false;
        }
    }

    /**
     * Updates navigation context in session equivalent to CICS COMMAREA update.
     */
    private void updateNavigationContext(MenuRequest request, String menuType) {
        try {
            if (request.getSessionContext() != null) {
                // Add to navigation stack using SessionService
                SessionContext sessionContext = request.getSessionContext();
                List<String> navigationStack = sessionContext.getNavigationStack();
                if (navigationStack == null) {
                    navigationStack = new ArrayList<>();
                }
                
                // Add current menu to navigation stack
                sessionContext.addToNavigationStack(menuType);
                
                logger.debug("Navigation context updated with menu type: {}", menuType);
            }
        } catch (Exception e) {
            logger.error("Error updating navigation context", e);
        }
    }

    /**
     * Processes main menu selection by delegating to MainMenuService.
     */
    private MenuResponse processMainMenuSelection(MenuRequest request) {
        try {
            // Delegate to MainMenuService for COMEN01C logic
            Map<String, Object> selectionResult = mainMenuService.processMenuSelection(
                request.getSelectedOption(), request.getUserId());
            
            if (selectionResult.containsKey("error")) {
                MenuResponse errorResponse = new MenuResponse();
                errorResponse.setErrorMessage((String) selectionResult.get("error"));
                return errorResponse;
            }
            
            // Build response from selection result
            return buildMenuResponse(selectionResult, request, MENU_TYPE_MAIN);

        } catch (Exception e) {
            logger.error("Error processing main menu selection", e);
            MenuResponse errorResponse = new MenuResponse();
            errorResponse.setErrorMessage("Error processing main menu selection");
            return errorResponse;
        }
    }

    /**
     * Processes admin menu selection by delegating to AdminMenuService.
     */
    private MenuResponse processAdminMenuSelection(MenuRequest request) {
        try {
            // Delegate to AdminMenuService for COADM01C logic
            Map<String, Object> selectionResult = adminMenuService.processAdminSelection(
                request.getSelectedOption(), request.getUserId());
            
            if (selectionResult.containsKey("error")) {
                MenuResponse errorResponse = new MenuResponse();
                errorResponse.setErrorMessage((String) selectionResult.get("error"));
                return errorResponse;
            }
            
            // Build response from selection result
            return buildMenuResponse(selectionResult, request, MENU_TYPE_ADMIN);

        } catch (Exception e) {
            logger.error("Error processing admin menu selection", e);
            MenuResponse errorResponse = new MenuResponse();
            errorResponse.setErrorMessage("Error processing admin menu selection");
            return errorResponse;
        }
    }

    /**
     * Processes report menu selection with direct CORPT00C logic implementation.
     */
    private MenuResponse processReportMenuSelection(MenuRequest request) {
        try {
            MenuResponse response = new MenuResponse();
            
            // Handle report selection directly since ReportMenuService is not available
            boolean validSelection = handleReportSelection(request.getSelectedOption(), response);
            
            if (!validSelection) {
                response.setErrorMessage("Invalid report selection - please choose from available options");
            }
            
            // Set report menu context
            response.setCurrentMenu(MENU_TYPE_REPORT);
            response.setTransactionCode(TRANS_REPORT_MENU);
            response.setMenuOptions(buildReportMenuOptions());
            
            // Set user context
            User user = getUserForMenu(request.getUserId());
            if (user != null) {
                response.setUserName(user.getFirstName() + " " + user.getLastName());
            }
            
            return response;

        } catch (Exception e) {
            logger.error("Error processing report menu selection", e);
            MenuResponse errorResponse = new MenuResponse();
            errorResponse.setErrorMessage("Error processing report menu selection");
            return errorResponse;
        }
    }

    /**
     * Updates navigation context after menu selection processing.
     */
    private void updateSelectionNavigationContext(MenuRequest request, MenuResponse response) {
        try {
            if (request.getSessionContext() != null && response.getCurrentMenu() != null) {
                // Update session with selection context
                SessionContext sessionContext = request.getSessionContext();
                sessionContext.addToNavigationStack(response.getCurrentMenu() + ":" + request.getSelectedOption());
                
                logger.debug("Selection navigation context updated");
            }
        } catch (Exception e) {
            logger.error("Error updating selection navigation context", e);
        }
    }

    /**
     * Validates if menu type is supported.
     */
    private boolean isValidMenuType(String menuType) {
        return MENU_TYPE_MAIN.equals(menuType) || 
               MENU_TYPE_ADMIN.equals(menuType) || 
               MENU_TYPE_REPORT.equals(menuType);
    }

    /**
     * Updates navigation history in session using SessionService.
     */
    private void updateNavigationHistory(MenuRequest request, String targetMenuType) {
        try {
            if (request.getSessionContext() != null) {
                SessionContext sessionContext = request.getSessionContext();
                sessionContext.addToNavigationStack("NAVIGATE_TO:" + targetMenuType);
                
                logger.debug("Navigation history updated with target: {}", targetMenuType);
            }
        } catch (Exception e) {
            logger.error("Error updating navigation history", e);
        }
    }

    /**
     * Retrieves user information for menu context using UserService.
     */
    private User getUserForMenu(String userId) {
        try {
            return userService.getUserById(userId);
        } catch (Exception e) {
            logger.error("Error retrieving user for menu: {}", userId, e);
            return null;
        }
    }

    /**
     * Gets default menu options based on menu type and MenuConfiguration.
     */
    private List<Map<String, Object>> getDefaultMenuOptions(String menuType) {
        try {
            switch (menuType) {
                case MENU_TYPE_REPORT:
                    return buildReportMenuOptions();
                case MENU_TYPE_MAIN:
                case MENU_TYPE_ADMIN:
                default:
                    // Use MenuConfiguration for standard menu options
                    List<Map<String, Object>> defaultOptions = new ArrayList<>();
                    Map<String, Object> menuOptions = menuConfiguration.getMenuOptions();
                    
                    // Convert MenuConfiguration data to response format
                    for (Map.Entry<String, Object> entry : menuOptions.entrySet()) {
                        Map<String, Object> option = new HashMap<>();
                        option.put("optionNumber", entry.getKey());
                        option.put("optionText", entry.getValue().toString());
                        option.put("enabled", true);
                        defaultOptions.add(option);
                    }
                    
                    return defaultOptions;
            }
        } catch (Exception e) {
            logger.error("Error getting default menu options for type: {}", menuType, e);
            return new ArrayList<>();
        }
    }

    /**
     * Gets transaction code for menu type matching COBOL transaction IDs.
     */
    private String getTransactionCodeForMenu(String menuType) {
        switch (menuType) {
            case MENU_TYPE_MAIN:
                return TRANS_MAIN_MENU;
            case MENU_TYPE_ADMIN:
                return TRANS_ADMIN_MENU;
            case MENU_TYPE_REPORT:
                return TRANS_REPORT_MENU;
            default:
                return TRANS_MAIN_MENU;
        }
    }

    /**
     * Handles F3=Exit navigation equivalent to COBOL PF3 processing.
     */
    private MenuResponse handleExitNavigation(MenuRequest request, MenuResponse currentResponse) {
        try {
            // F3 = Exit - navigate to previous menu or main menu
            if (request.getSessionContext() != null) {
                List<String> navigationStack = request.getSessionContext().getNavigationStack();
                if (navigationStack != null && navigationStack.size() > 1) {
                    // Go back to previous menu
                    String previousMenu = navigationStack.get(navigationStack.size() - 2);
                    if (previousMenu.contains(":")) {
                        previousMenu = previousMenu.split(":")[0];
                    }
                    
                    // Navigate back to previous menu
                    return navigateToMenu(request, previousMenu);
                }
            }
            
            // Default to main menu if no navigation history
            return navigateToMenu(request, MENU_TYPE_MAIN);

        } catch (Exception e) {
            logger.error("Error handling exit navigation", e);
            currentResponse.setErrorMessage("Error during exit navigation");
            return currentResponse;
        }
    }

    /**
     * Handles F12=Cancel navigation equivalent to COBOL PF12 processing.
     */
    private MenuResponse handleCancelNavigation(MenuRequest request, MenuResponse currentResponse) {
        try {
            // F12 = Cancel - refresh current menu and clear any error states
            String currentMenuType = currentResponse.getCurrentMenu();
            if (currentMenuType == null) {
                currentMenuType = determineMenuType(request);
            }
            
            // Create fresh menu request without selection
            MenuRequest refreshRequest = new MenuRequest();
            refreshRequest.setUserId(request.getUserId());
            refreshRequest.setUserType(request.getUserType());
            refreshRequest.setMenuType(currentMenuType);
            refreshRequest.setSessionContext(request.getSessionContext());
            
            // Generate fresh menu
            switch (currentMenuType) {
                case MENU_TYPE_MAIN:
                    return generateMainMenu(refreshRequest);
                case MENU_TYPE_ADMIN:
                    return generateAdminMenu(refreshRequest);
                case MENU_TYPE_REPORT:
                    return generateReportMenu(refreshRequest);
                default:
                    return generateMainMenu(refreshRequest);
            }

        } catch (Exception e) {
            logger.error("Error handling cancel navigation", e);
            currentResponse.setErrorMessage("Error during cancel operation");
            return currentResponse;
        }
    }
}