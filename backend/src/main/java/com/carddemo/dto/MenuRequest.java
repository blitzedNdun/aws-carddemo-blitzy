package com.carddemo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request DTO for menu operations, serving as the unified input structure
 * for the MenuService facade that handles COBOL menu program functionality.
 * 
 * This class represents the consolidated menu request structure that supports:
 * - Main menu navigation (COMEN01C logic)
 * - Admin menu access control (COADM01C logic) 
 * - Report menu functionality (CORPT00C logic)
 * - Session state management and navigation tracking
 * - PF-key function mapping for 3270 terminal compatibility
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuRequest {

    /**
     * User ID for the request.
     * Used for user authentication and authorization.
     */
    private String userId;

    /**
     * User type (USER, ADMIN, etc.).
     * Used for role-based access control and menu filtering.
     */
    private String userType;

    /**
     * Type of request (MENU, SELECTION, NAVIGATION, etc.).
     * Determines the processing flow within MenuService.
     */
    private String requestType;

    /**
     * Menu type being requested (MAIN, ADMIN, REPORT).
     * Maps to specific COBOL programs:
     * - MAIN -> COMEN01C (main menu)
     * - ADMIN -> COADM01C (admin menu) 
     * - REPORT -> CORPT00C (report menu)
     */
    private String menuType;

    /**
     * Selected menu option number (1, 2, 3, etc.).
     * Used when user selects a specific menu option.
     */
    private String selectedOption;

    /**
     * PF-key pressed by user (F3, F12, etc.).
     * Maps to 3270 terminal PF-key functionality:
     * - F3 = Exit (navigate back)
     * - F12 = Cancel (refresh current menu)
     */
    private String pfKey;

    /**
     * Session context for state management.
     * Contains user session data, navigation stack, and current state.
     */
    private SessionContext sessionContext;

    /**
     * Transaction code for the current operation.
     * Maps to CICS transaction codes (CM00, CA00, CR00, etc.).
     */
    private String transactionCode;

    /**
     * Current menu context for navigation.
     * Tracks the current menu state for proper navigation handling.
     */
    private String currentMenu;

    /**
     * Previous menu for back navigation.
     * Used for F3 (Exit) functionality to navigate back to previous menu.
     */
    private String previousMenu;

    /**
     * Additional request parameters as key-value pairs.
     * Flexible structure for additional context or configuration data.
     */
    private java.util.Map<String, String> parameters;

    /**
     * Timestamp of the request.
     * Used for session timeout and audit trail purposes.
     */
    private java.time.LocalDateTime timestamp;

    /**
     * Request source (WEB, API, BATCH).
     * Identifies the source of the menu request for proper handling.
     */
    private String requestSource;

    /**
     * Language preference for menu display.
     * Supports internationalization of menu options and messages.
     */
    private String language;

    /**
     * Security token for request validation.
     * Used for additional security validation beyond user authentication.
     */
    private String securityToken;

    /**
     * Current menu level for hierarchical navigation.
     * Used to track the current position in the menu hierarchy.
     * Examples: "MAIN", "SUB_MENU", "ROOT"
     */
    private String currentMenuLevel;

    /**
     * Parent menu identifier for back navigation.
     * Used for PF3 (Exit) functionality to navigate back to parent menu.
     * Examples: "MAIN_MENU", "ADMIN_MENU", null for root level
     */
    private String parentMenu;

    /**
     * Current menu state for state management.
     * Used to track menu state across requests, equivalent to COMMAREA state.
     * Examples: "ACTIVE", "RESET", "PRESERVE"
     */
    private String menuState;

    /**
     * Helper method to check if this is an admin menu request.
     * 
     * @return true if this is an admin menu request
     */
    public boolean isAdminMenuRequest() {
        return "ADMIN".equalsIgnoreCase(this.menuType);
    }

    /**
     * Helper method to check if this is a report menu request.
     * 
     * @return true if this is a report menu request
     */
    public boolean isReportMenuRequest() {
        return "REPORT".equalsIgnoreCase(this.menuType);
    }

    /**
     * Helper method to check if this is a main menu request.
     * 
     * @return true if this is a main menu request
     */
    public boolean isMainMenuRequest() {
        return "MAIN".equalsIgnoreCase(this.menuType) || this.menuType == null;
    }

    /**
     * Helper method to check if a menu option was selected.
     * 
     * @return true if a menu option was selected
     */
    public boolean hasSelectedOption() {
        return this.selectedOption != null && !this.selectedOption.trim().isEmpty();
    }

    /**
     * Helper method to check if a PF-key was pressed.
     * 
     * @return true if a PF-key was pressed
     */
    public boolean hasPfKey() {
        return this.pfKey != null && !this.pfKey.trim().isEmpty();
    }

    /**
     * Helper method to get the selected option as an integer.
     * 
     * @return selected option as integer, or -1 if invalid
     */
    public int getSelectedOptionAsInt() {
        try {
            return hasSelectedOption() ? Integer.parseInt(this.selectedOption.trim()) : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Helper method to check if this is an F3 (Exit) key press.
     * 
     * @return true if F3 key was pressed
     */
    public boolean isF3Exit() {
        return "F3".equalsIgnoreCase(this.pfKey);
    }

    /**
     * Helper method to check if this is an F12 (Cancel) key press.
     * 
     * @return true if F12 key was pressed
     */
    public boolean isF12Cancel() {
        return "F12".equalsIgnoreCase(this.pfKey);
    }

    /**
     * Gets the current menu level.
     * 
     * @return Current menu level string
     */
    public String getCurrentMenuLevel() {
        return currentMenuLevel;
    }

    /**
     * Sets the current menu level.
     * 
     * @param currentMenuLevel Menu level identifier
     */
    public void setCurrentMenuLevel(String currentMenuLevel) {
        this.currentMenuLevel = currentMenuLevel;
    }

    /**
     * Gets the parent menu identifier.
     * 
     * @return Parent menu string
     */
    public String getParentMenu() {
        return parentMenu;
    }

    /**
     * Sets the parent menu identifier.
     * 
     * @param parentMenu Parent menu identifier for back navigation
     */
    public void setParentMenu(String parentMenu) {
        this.parentMenu = parentMenu;
    }

    /**
     * Gets the current menu state.
     * 
     * @return Menu state string
     */
    public String getMenuState() {
        return menuState;
    }

    /**
     * Sets the current menu state.
     * 
     * @param menuState Menu state for state management
     */
    public void setMenuState(String menuState) {
        this.menuState = menuState;
    }
}