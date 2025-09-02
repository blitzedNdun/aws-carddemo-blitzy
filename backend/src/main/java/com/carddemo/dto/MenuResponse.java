/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.carddemo.dto.MenuOption;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO for main menu display containing filtered menu options, user context, 
 * system information, and error messages.
 * 
 * Maps to COMEN01 BMS output screen with role-based menu option filtering and system 
 * status display, maintaining functional parity with the original COBOL/CICS 
 * main menu implementation.
 * 
 * This DTO corresponds to the COMEN1AO output structure from COMEN01.CPY copybook:
 * - menuOptions: Maps to OPTN001O through OPTN012O fields (up to 12 options)
 * - userName: Maps to user identification from COMMAREA
 * - currentDate: Maps to CURDATEO field (PIC X(8))
 * - currentTime: Maps to CURTIMEO field (PIC X(8)) 
 * - programName: Maps to PGMNAMEO field (PIC X(8))
 * - systemId: Maps to TRNNAMEO field (transaction ID, PIC X(4))
 * - errorMessage: Maps to ERRMSGO field (PIC X(78))
 * 
 * The class implements role-based menu filtering logic equivalent to the COBOL
 * program's user type checking and menu option access control.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MenuResponse {

    /**
     * Maximum number of menu options supported by COMEN01 screen layout.
     * Corresponds to OPTN001-OPTN012 fields in the BMS definition.
     */
    private static final int MAX_MENU_OPTIONS = 12;
    
    /**
     * Maximum length for error message field.
     * Derived from COMEN01.bms ERRMSG field LENGTH=78.
     */
    private static final int ERROR_MESSAGE_MAX_LENGTH = 78;
    
    /**
     * Maximum length for program name field.
     * Derived from COMEN01.bms PGMNAME field LENGTH=8.
     */
    private static final int PROGRAM_NAME_MAX_LENGTH = 8;
    
    /**
     * Maximum length for system ID field.
     * Derived from COMEN01.bms TRNNAME field LENGTH=4.
     */
    private static final int SYSTEM_ID_MAX_LENGTH = 4;
    
    /**
     * Maximum length for date field.
     * Derived from COMEN01.bms CURDATE field LENGTH=8 (mm/dd/yy format).
     */
    private static final int DATE_MAX_LENGTH = 8;
    
    /**
     * Maximum length for time field.
     * Derived from COMEN01.bms CURTIME field LENGTH=8 (hh:mm:ss format).
     */
    private static final int TIME_MAX_LENGTH = 8;

    /**
     * List of menu options to display, filtered based on user access level.
     * Maps to OPTN001O through OPTN012O fields in COMEN01 BMS layout.
     * 
     * The list is populated with role-based filtering equivalent to the COBOL
     * logic that checks CDEMO-MENU-OPT-USRTYPE against CDEMO-USRTYP-USER/ADMIN.
     * 
     * Maximum of 12 options matching the screen layout constraints.
     */
    @Valid
    @Size(max = MAX_MENU_OPTIONS, message = "Cannot exceed " + MAX_MENU_OPTIONS + " menu options")
    private List<MenuOption> menuOptions;

    /**
     * Current user name for display.
     * Maps to user identification from CARDDEMO-COMMAREA (CDEMO-USER-ID).
     * Used for personalizing the menu display and security context.
     */
    private String userName;

    /**
     * Current date in mm/dd/yy format.
     * Maps to CURDATEO field (PIC X(8)) from COMEN01 copybook.
     * Populated using FUNCTION CURRENT-DATE equivalent logic.
     */
    @Size(max = DATE_MAX_LENGTH, message = "Date cannot exceed " + DATE_MAX_LENGTH + " characters")
    private String currentDate;

    /**
     * Current time in hh:mm:ss format.
     * Maps to CURTIMEO field (PIC X(8)) from COMEN01 copybook.
     * Populated using FUNCTION CURRENT-DATE equivalent logic.
     */
    @Size(max = TIME_MAX_LENGTH, message = "Time cannot exceed " + TIME_MAX_LENGTH + " characters")
    private String currentTime;

    /**
     * Program name identifier.
     * Maps to PGMNAMEO field (PIC X(8)) from COMEN01 copybook.
     * Equivalent to WS-PGMNAME in the COBOL program (VALUE 'COMEN01C').
     */
    @Size(max = PROGRAM_NAME_MAX_LENGTH, message = "Program name cannot exceed " + PROGRAM_NAME_MAX_LENGTH + " characters")
    private String programName;

    /**
     * System/Transaction identifier.
     * Maps to TRNNAMEO field (PIC X(4)) from COMEN01 copybook.
     * Equivalent to WS-TRANID in the COBOL program (VALUE 'CM00').
     */
    @Size(max = SYSTEM_ID_MAX_LENGTH, message = "System ID cannot exceed " + SYSTEM_ID_MAX_LENGTH + " characters")
    private String systemId;

    /**
     * Error or status message to display.
     * Maps to ERRMSGO field (PIC X(78)) from COMEN01 copybook.
     * Used for validation errors, access denial messages, or system notifications.
     */
    @Size(max = ERROR_MESSAGE_MAX_LENGTH, message = "Error message cannot exceed " + ERROR_MESSAGE_MAX_LENGTH + " characters")
    private String errorMessage;

    /**
     * Current menu type identifier.
     * Used for navigation context and session state management.
     */
    private String currentMenu;

    /**
     * Transaction code for the current operation.
     * Maps to COBOL transaction ID logic.
     */
    private String transactionCode;

    /**
     * Timestamp for the response generation.
     * Used for session management and audit logging.
     */
    private LocalDateTime timestamp;

    /**
     * User ID for the current session.
     * Used for session context and security validation.
     */
    private String userId;

    /**
     * User type/role for access control.
     * Used for role-based menu filtering and security.
     */
    private String userType;

    /**
     * Session ID for state management.
     * Used for tracking user session across requests.
     */
    private String sessionId;

    /**
     * Navigation stack for tracking user navigation history.
     * Used for breadcrumb navigation and back navigation functionality.
     */
    private List<String> navigationStack;

    /**
     * Success message for positive feedback.
     * Used for confirmation messages and user notifications.
     */
    private String successMessage;

    /**
     * Default constructor initializing empty menu options list.
     */
    public MenuResponse() {
        this.menuOptions = new ArrayList<>();
    }

    /**
     * Constructor with core menu data.
     * 
     * @param menuOptions List of menu options to display
     * @param userName Current user name
     * @param currentDate Current date in mm/dd/yy format
     * @param currentTime Current time in hh:mm:ss format
     */
    public MenuResponse(List<MenuOption> menuOptions, String userName, String currentDate, String currentTime) {
        this.menuOptions = menuOptions != null ? menuOptions : new ArrayList<>();
        this.userName = userName;
        this.currentDate = currentDate;
        this.currentTime = currentTime;
    }

    /**
     * Gets the filtered list of menu options.
     * 
     * @return List of MenuOption objects for display
     */
    public List<MenuOption> getMenuOptions() {
        return menuOptions;
    }

    /**
     * Sets the menu options list with validation.
     * Ensures the list doesn't exceed the maximum supported by the screen layout.
     * 
     * @param menuOptions List of menu options, truncated if exceeds maximum
     */
    public void setMenuOptions(List<MenuOption> menuOptions) {
        if (menuOptions != null && menuOptions.size() > MAX_MENU_OPTIONS) {
            this.menuOptions = menuOptions.subList(0, MAX_MENU_OPTIONS);
        } else {
            this.menuOptions = menuOptions != null ? menuOptions : new ArrayList<>();
        }
    }

    /**
     * Gets the current user name.
     * 
     * @return User name string
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the current user name.
     * 
     * @param userName User name for display
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Gets the current date.
     * 
     * @return Date string in mm/dd/yy format
     */
    public String getCurrentDate() {
        return currentDate;
    }

    /**
     * Sets the current date with length validation.
     * 
     * @param currentDate Date string, truncated if exceeds 8 characters
     */
    public void setCurrentDate(String currentDate) {
        if (currentDate != null && currentDate.length() > DATE_MAX_LENGTH) {
            this.currentDate = currentDate.substring(0, DATE_MAX_LENGTH);
        } else {
            this.currentDate = currentDate;
        }
    }

    /**
     * Gets the current time.
     * 
     * @return Time string in hh:mm:ss format
     */
    public String getCurrentTime() {
        return currentTime;
    }

    /**
     * Sets the current time with length validation.
     * 
     * @param currentTime Time string, truncated if exceeds 8 characters
     */
    public void setCurrentTime(String currentTime) {
        if (currentTime != null && currentTime.length() > TIME_MAX_LENGTH) {
            this.currentTime = currentTime.substring(0, TIME_MAX_LENGTH);
        } else {
            this.currentTime = currentTime;
        }
    }

    /**
     * Gets the program name.
     * 
     * @return Program name identifier
     */
    public String getProgramName() {
        return programName;
    }

    /**
     * Sets the program name with length validation.
     * 
     * @param programName Program name, truncated if exceeds 8 characters
     */
    public void setProgramName(String programName) {
        if (programName != null && programName.length() > PROGRAM_NAME_MAX_LENGTH) {
            this.programName = programName.substring(0, PROGRAM_NAME_MAX_LENGTH);
        } else {
            this.programName = programName;
        }
    }

    /**
     * Gets the system/transaction identifier.
     * 
     * @return System ID string
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * Sets the system/transaction identifier with length validation.
     * 
     * @param systemId System ID, truncated if exceeds 4 characters
     */
    public void setSystemId(String systemId) {
        if (systemId != null && systemId.length() > SYSTEM_ID_MAX_LENGTH) {
            this.systemId = systemId.substring(0, SYSTEM_ID_MAX_LENGTH);
        } else {
            this.systemId = systemId;
        }
    }

    /**
     * Gets the error or status message.
     * 
     * @return Error message string
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error or status message with length validation.
     * 
     * @param errorMessage Error message, truncated if exceeds 78 characters
     */
    public void setErrorMessage(String errorMessage) {
        if (errorMessage != null && errorMessage.length() > ERROR_MESSAGE_MAX_LENGTH) {
            this.errorMessage = errorMessage.substring(0, ERROR_MESSAGE_MAX_LENGTH);
        } else {
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Filters menu options based on user access level.
     * Implements role-based access control equivalent to COBOL logic that checks
     * CDEMO-MENU-OPT-USRTYPE against user type.
     * 
     * @param userAccessLevel User's access level ("ADMIN", "USER", etc.)
     * @return Filtered list of menu options the user can access
     */
    public List<MenuOption> getFilteredMenuOptions(String userAccessLevel) {
        if (menuOptions == null || userAccessLevel == null) {
            return new ArrayList<>();
        }
        
        return menuOptions.stream()
                .filter(option -> option.getEnabled() != null && option.getEnabled())
                .filter(option -> isOptionAccessible(option, userAccessLevel))
                .collect(Collectors.toList());
    }

    /**
     * Determines if a menu option is accessible to the user.
     * Implements the access control logic equivalent to the COBOL program's
     * check for CDEMO-USRTYP-USER vs CDEMO-MENU-OPT-USRTYPE.
     * 
     * @param option The menu option to check
     * @param userAccessLevel User's access level
     * @return True if user can access the option, false otherwise
     */
    private boolean isOptionAccessible(MenuOption option, String userAccessLevel) {
        String optionAccessLevel = option.getAccessLevel();
        
        // If no access level specified on option, allow access
        if (optionAccessLevel == null || optionAccessLevel.trim().isEmpty()) {
            return true;
        }
        
        // Admin users can access everything
        if ("ADMIN".equalsIgnoreCase(userAccessLevel)) {
            return true;
        }
        
        // For other users, check if their access level matches option requirement
        return userAccessLevel.equalsIgnoreCase(optionAccessLevel);
    }

    /**
     * Adds a menu option to the list with validation.
     * Ensures the option is valid and doesn't exceed maximum count.
     * 
     * @param option MenuOption to add
     * @return True if option was added, false if rejected
     */
    public boolean addMenuOption(MenuOption option) {
        if (option == null || menuOptions.size() >= MAX_MENU_OPTIONS) {
            return false;
        }
        
        // Validate option number is within valid range
        Integer optionNumber = option.getOptionNumber();
        if (optionNumber == null || optionNumber < 1 || optionNumber > MAX_MENU_OPTIONS) {
            return false;
        }
        
        // Check if option number already exists
        boolean exists = menuOptions.stream()
                .anyMatch(existing -> optionNumber.equals(existing.getOptionNumber()));
        
        if (exists) {
            return false;
        }
        
        return menuOptions.add(option);
    }

    /**
     * Removes a menu option by option number.
     * 
     * @param optionNumber The option number to remove
     * @return True if option was removed, false if not found
     */
    public boolean removeMenuOption(Integer optionNumber) {
        if (optionNumber == null) {
            return false;
        }
        
        return menuOptions.removeIf(option -> optionNumber.equals(option.getOptionNumber()));
    }

    /**
     * Gets a menu option by option number.
     * 
     * @param optionNumber The option number to find
     * @return MenuOption if found, null otherwise
     */
    public MenuOption getMenuOption(Integer optionNumber) {
        if (optionNumber == null || menuOptions == null) {
            return null;
        }
        
        return menuOptions.stream()
                .filter(option -> optionNumber.equals(option.getOptionNumber()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Clears all menu options.
     */
    public void clearMenuOptions() {
        if (menuOptions != null) {
            menuOptions.clear();
        }
    }

    /**
     * Gets the count of currently loaded menu options.
     * 
     * @return Number of menu options
     */
    public int getMenuOptionCount() {
        return menuOptions != null ? menuOptions.size() : 0;
    }

    /**
     * Checks if there are any error messages.
     * 
     * @return True if error message is present and not empty
     */
    public boolean hasErrors() {
        return errorMessage != null && !errorMessage.trim().isEmpty();
    }

    /**
     * Validates all menu options using their internal validation.
     * 
     * @return True if all options are valid, false otherwise
     */
    public boolean validateMenuOptions() {
        if (menuOptions == null) {
            return true;
        }
        
        for (MenuOption option : menuOptions) {
            // Basic validation - check required fields
            if (option.getOptionNumber() == null || 
                option.getDescription() == null || option.getDescription().trim().isEmpty()) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Gets the current menu type.
     * 
     * @return Current menu type identifier
     */
    public String getCurrentMenu() {
        return currentMenu;
    }

    /**
     * Sets the current menu type.
     * 
     * @param currentMenu Menu type identifier
     */
    public void setCurrentMenu(String currentMenu) {
        this.currentMenu = currentMenu;
    }

    /**
     * Gets the transaction code.
     * 
     * @return Transaction code string
     */
    public String getTransactionCode() {
        return transactionCode;
    }

    /**
     * Sets the transaction code.
     * 
     * @param transactionCode Transaction code for the operation
     */
    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }

    /**
     * Gets the response timestamp.
     * 
     * @return Timestamp of response generation
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the response timestamp.
     * 
     * @param timestamp Timestamp for the response
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the user ID.
     * 
     * @return User ID string
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user ID.
     * 
     * @param userId User ID for session context
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets the user type.
     * 
     * @return User type/role string
     */
    public String getUserType() {
        return userType;
    }

    /**
     * Sets the user type.
     * 
     * @param userType User type/role for access control
     */
    public void setUserType(String userType) {
        this.userType = userType;
    }

    /**
     * Gets the session ID.
     * 
     * @return Session ID string
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the session ID.
     * 
     * @param sessionId Session ID for state management
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Gets the success message.
     * 
     * @return Success message string
     */
    public String getSuccessMessage() {
        return successMessage;
    }

    /**
     * Sets the success message.
     * 
     * @param successMessage Success message for user feedback
     */
    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
    }

    /**
     * Gets the navigation stack.
     * 
     * @return Navigation stack list
     */
    public List<String> getNavigationStack() {
        return navigationStack;
    }

    /**
     * Sets the navigation stack.
     * 
     * @param navigationStack Navigation stack for breadcrumb functionality
     */
    public void setNavigationStack(List<String> navigationStack) {
        this.navigationStack = navigationStack;
    }

    /**
     * Provides string representation of the menu response.
     * Includes key fields and menu option count for debugging.
     * 
     * @return String representation
     */
    @Override
    public String toString() {
        return "MenuResponse{" +
                "menuOptions count=" + (menuOptions != null ? menuOptions.size() : 0) +
                ", userName='" + userName + '\'' +
                ", currentDate='" + currentDate + '\'' +
                ", currentTime='" + currentTime + '\'' +
                ", programName='" + programName + '\'' +
                ", systemId='" + systemId + '\'' +
                ", currentMenu='" + currentMenu + '\'' +
                ", transactionCode='" + transactionCode + '\'' +
                ", hasError=" + hasErrors() +
                '}';
    }
}