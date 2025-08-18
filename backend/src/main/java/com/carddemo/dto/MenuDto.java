/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;

import com.carddemo.dto.MenuOption;
import com.carddemo.util.Constants;

/**
 * Menu navigation DTO for COMEN01 and COADM01 screens.
 * 
 * This DTO represents the complete menu structure for both main menu (COMEN01) and 
 * admin menu (COADM01) screens from the original COBOL/CICS application. It supports
 * dynamic menu generation based on user permissions and role-based access control.
 * 
 * The implementation maintains functional parity with the original COBOL menu systems
 * while providing modern REST API data structures for React frontend consumption.
 * 
 * Key Features:
 * - Supports both MAIN and ADMIN menu types
 * - Role-based filtering of menu options
 * - Navigation hints and keyboard shortcuts
 * - System information display
 * - Up to 12 menu options matching original BMS layout
 * 
 * BMS Map Compatibility:
 * - COMEN01.bms: Main Menu (OPTN001-OPTN012 fields)
 * - COADM01.bms: Admin Menu (OPTN001-OPTN012 fields)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
public class MenuDto {

    /**
     * Maximum number of menu options supported by the BMS screen layout.
     * Derived from COMEN01.bms and COADM01.bms with 12 option fields (OPTN001-OPTN012).
     */
    private static final int MENU_OPTION_MAX_COUNT = 12;
    
    /**
     * Maximum length for menu option descriptions matching BMS field specifications.
     * Derived from COMEN01.bms where each OPTN*** field has LENGTH=40.
     */
    private static final int MENU_DESCRIPTION_LENGTH = 40;

    /**
     * Menu type enumeration defining the type of menu being displayed.
     */
    public enum MenuType {
        /**
         * Main menu for regular users (COMEN01).
         */
        MAIN,
        
        /**
         * Administrative menu for admin users (COADM01).
         */
        ADMIN
    }

    /**
     * Type of menu being displayed (MAIN or ADMIN).
     * Determines which BMS map structure is used and which options are available.
     */
    private String menuType;

    /**
     * List of menu options available for display.
     * Limited to 12 options matching the original BMS screen layout capacity.
     * Each option is validated for proper structure and content.
     */
    @Valid
    @Size(min = 1, max = MENU_OPTION_MAX_COUNT, message = "Menu must contain between 1 and " + MENU_OPTION_MAX_COUNT + " options")
    private List<MenuOption> menuOptions;

    /**
     * Current user's role for access control.
     * Used for filtering menu options based on access permissions.
     * Maps to user type from original COBOL user validation.
     */
    private String userRole;

    /**
     * Navigation hints for user guidance.
     * Provides contextual help and instructions for menu navigation.
     * Replaces static text from original BMS function key area.
     */
    private Map<String, String> navigationHints;

    /**
     * Keyboard shortcuts and function key mappings.
     * Maps PF keys and other shortcuts to their descriptions.
     * Maintains compatibility with original 3270 function key behavior.
     */
    private Map<String, String> keyboardShortcuts;

    /**
     * System information for display in menu header.
     * Contains transaction name, program name, date, time, and other system data.
     * Replaces BMS header fields (TRNNAME, PGMNAME, CURDATE, CURTIME).
     */
    private Map<String, String> systemInfo;

    /**
     * Default constructor for framework instantiation.
     * Initializes collections and sets default values.
     */
    public MenuDto() {
        this.menuOptions = new ArrayList<>();
        this.navigationHints = new HashMap<>();
        this.keyboardShortcuts = new HashMap<>();
        this.systemInfo = new HashMap<>();
        initializeDefaultKeyboardShortcuts();
    }

    /**
     * Constructor with menu type.
     * Initializes the menu with the specified type and default collections.
     * 
     * @param menuType The type of menu (MAIN or ADMIN)
     */
    public MenuDto(String menuType) {
        this();
        this.menuType = menuType;
    }

    /**
     * Initializes default keyboard shortcuts matching original function key behavior.
     * Sets up standard PF key mappings used across all menu screens.
     */
    private void initializeDefaultKeyboardShortcuts() {
        keyboardShortcuts.put("ENTER", "Continue");
        keyboardShortcuts.put("PF3", "Exit");
        keyboardShortcuts.put("PF12", "Cancel");
    }

    /**
     * Gets the menu type.
     * 
     * @return The menu type (MAIN or ADMIN)
     */
    public String getMenuType() {
        return menuType;
    }

    /**
     * Sets the menu type.
     * 
     * @param menuType The menu type to set
     */
    public void setMenuType(String menuType) {
        this.menuType = menuType;
    }

    /**
     * Gets the list of menu options.
     * 
     * @return The list of menu options
     */
    public List<MenuOption> getMenuOptions() {
        return menuOptions;
    }

    /**
     * Sets the list of menu options.
     * Validates that the list doesn't exceed the maximum option count.
     * 
     * @param menuOptions The list of menu options to set
     */
    public void setMenuOptions(List<MenuOption> menuOptions) {
        if (menuOptions != null && menuOptions.size() > MENU_OPTION_MAX_COUNT) {
            throw new IllegalArgumentException("Menu cannot contain more than " + MENU_OPTION_MAX_COUNT + " options");
        }
        this.menuOptions = menuOptions != null ? menuOptions : new ArrayList<>();
    }

    /**
     * Gets the user role.
     * 
     * @return The current user's role
     */
    public String getUserRole() {
        return userRole;
    }

    /**
     * Sets the user role.
     * 
     * @param userRole The user role to set
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    /**
     * Gets the navigation hints.
     * 
     * @return Map of navigation hints
     */
    public Map<String, String> getNavigationHints() {
        return navigationHints;
    }

    /**
     * Sets the navigation hints.
     * 
     * @param navigationHints Map of navigation hints to set
     */
    public void setNavigationHints(Map<String, String> navigationHints) {
        this.navigationHints = navigationHints != null ? navigationHints : new HashMap<>();
    }

    /**
     * Gets the keyboard shortcuts.
     * 
     * @return Map of keyboard shortcuts
     */
    public Map<String, String> getKeyboardShortcuts() {
        return keyboardShortcuts;
    }

    /**
     * Sets the keyboard shortcuts.
     * 
     * @param keyboardShortcuts Map of keyboard shortcuts to set
     */
    public void setKeyboardShortcuts(Map<String, String> keyboardShortcuts) {
        this.keyboardShortcuts = keyboardShortcuts != null ? keyboardShortcuts : new HashMap<>();
    }

    /**
     * Gets the system information.
     * 
     * @return Map of system information
     */
    public Map<String, String> getSystemInfo() {
        return systemInfo;
    }

    /**
     * Sets the system information.
     * 
     * @param systemInfo Map of system information to set
     */
    public void setSystemInfo(Map<String, String> systemInfo) {
        this.systemInfo = systemInfo != null ? systemInfo : new HashMap<>();
    }

    /**
     * Filters menu options based on user role.
     * Returns a new list containing only the options that the specified role can access.
     * Implements role-based access control matching original RACF security model.
     * 
     * @param role The user role to filter by
     * @return List of accessible menu options for the specified role
     */
    public List<MenuOption> filterOptionsByRole(String role) {
        if (role == null || menuOptions == null) {
            return new ArrayList<>();
        }
        
        return menuOptions.stream()
                .filter(option -> option.getEnabled() != null && option.getEnabled())
                .filter(option -> isOptionAccessibleByRole(option, role))
                .collect(Collectors.toList());
    }

    /**
     * Adds a menu option to the menu.
     * Validates that the maximum option count is not exceeded and that the option number is unique.
     * 
     * @param menuOption The menu option to add
     * @throws IllegalArgumentException if maximum options exceeded or option number already exists
     */
    public void addMenuOption(MenuOption menuOption) {
        if (menuOption == null) {
            throw new IllegalArgumentException("Menu option cannot be null");
        }
        
        if (menuOptions.size() >= MENU_OPTION_MAX_COUNT) {
            throw new IllegalArgumentException("Cannot add menu option: maximum of " + MENU_OPTION_MAX_COUNT + " options allowed");
        }
        
        // Check for duplicate option numbers
        if (menuOption.getOptionNumber() != null) {
            boolean optionExists = menuOptions.stream()
                    .anyMatch(existing -> Objects.equals(existing.getOptionNumber(), menuOption.getOptionNumber()));
            if (optionExists) {
                throw new IllegalArgumentException("Menu option with number " + menuOption.getOptionNumber() + " already exists");
            }
        }
        
        menuOptions.add(menuOption);
    }

    /**
     * Removes a menu option by option number.
     * 
     * @param optionNumber The option number to remove
     * @return true if an option was removed, false if no option with that number was found
     */
    public boolean removeMenuOption(Integer optionNumber) {
        if (optionNumber == null) {
            return false;
        }
        
        return menuOptions.removeIf(option -> Objects.equals(option.getOptionNumber(), optionNumber));
    }

    /**
     * Checks if the current user has administrative access.
     * Determines if the user can access admin-only menu options and functions.
     * 
     * @return true if user has admin access, false otherwise
     */
    public boolean hasAdminAccess() {
        return Constants.USER_TYPE_ADMIN.equals(userRole);
    }

    /**
     * Gets the count of available (enabled) menu options.
     * 
     * @return Number of enabled menu options
     */
    public int getAvailableOptionsCount() {
        if (menuOptions == null) {
            return 0;
        }
        
        return (int) menuOptions.stream()
                .filter(option -> option.getEnabled() != null && option.getEnabled())
                .count();
    }

    /**
     * Checks if a menu option is accessible by the specified role.
     * Implements the access control logic for role-based menu filtering.
     * 
     * @param option The menu option to check
     * @param role The user role to check against
     * @return true if the option is accessible by the role, false otherwise
     */
    private boolean isOptionAccessibleByRole(MenuOption option, String role) {
        if (option.getAccessLevel() == null) {
            // No access level specified means available to all users
            return true;
        }
        
        String accessLevel = option.getAccessLevel();
        
        // Admin users can access everything
        if (Constants.USER_TYPE_ADMIN.equals(role)) {
            return true;
        }
        
        // Regular users can access options that don't require admin access
        if (Constants.USER_TYPE_USER.equals(role)) {
            return !Constants.USER_TYPE_ADMIN.equals(accessLevel);
        }
        
        // For other roles, check exact match
        return role.equals(accessLevel);
    }

    /**
     * Checks equality based on menu type and menu options.
     * Two MenuDto objects are equal if they have the same menu type and equivalent menu options.
     * 
     * @param obj The object to compare with
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MenuDto menuDto = (MenuDto) obj;
        return Objects.equals(menuType, menuDto.menuType) &&
               Objects.equals(menuOptions, menuDto.menuOptions) &&
               Objects.equals(userRole, menuDto.userRole);
    }

    /**
     * Generates hash code based on menu type, menu options, and user role.
     * 
     * @return Hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(menuType, menuOptions, userRole);
    }

    /**
     * Provides string representation of the menu DTO.
     * Includes menu type, option count, user role, and available options count.
     * 
     * @return String representation
     */
    @Override
    public String toString() {
        return "MenuDto{" +
                "menuType='" + menuType + '\'' +
                ", menuOptionsCount=" + (menuOptions != null ? menuOptions.size() : 0) +
                ", userRole='" + userRole + '\'' +
                ", availableOptionsCount=" + getAvailableOptionsCount() +
                ", hasAdminAccess=" + hasAdminAccess() +
                '}';
    }
}
