/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import java.util.List;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.Data;

/**
 * Response DTO for main menu display (CM00 transaction).
 * 
 * Maps the COMEN01 BMS main menu screen output to a modern REST API response structure.
 * This DTO provides the complete data structure needed for React frontend to render
 * the main menu interface with up to 12 menu options, user context, and system information.
 * 
 * The structure maintains 1:1 mapping with the original COBOL/CICS screen layout:
 * - TRNNAME field → transactionCode
 * - PGMNAME field → programName  
 * - CURDATE field → currentDate
 * - CURTIME field → currentTime
 * - OPTN001-OPTN012 fields → menuOptions list
 * - ERRMSG field → errorMessage
 * 
 * Supports role-based menu filtering where menuOptions list contains only
 * the options the current user has permission to access, maintaining the
 * security model from the original RACF-based mainframe system.
 * 
 * This DTO ensures functional parity with the original COMEN01 screen while
 * providing modern JSON-based API structure for React frontend consumption.
 */
@Data
public class MainMenuResponse {

    /**
     * Maximum number of menu options supported by COMEN01 screen layout.
     * Corresponds to OPTN001-OPTN012 fields in the original BMS definition.
     */
    private static final int MAX_MENU_OPTIONS = 12;
    
    /**
     * Maximum length for transaction code matching TRNNAME field.
     * Derived from COMEN01.bms TRNNAME field LENGTH=4.
     */
    private static final int TRANSACTION_CODE_LENGTH = 4;
    
    /**
     * Maximum length for program name matching PGMNAME field.
     * Derived from COMEN01.bms PGMNAME field LENGTH=8.
     */
    private static final int PROGRAM_NAME_LENGTH = 8;
    
    /**
     * Maximum length for error message matching ERRMSG field.
     * Derived from COMEN01.bms ERRMSG field LENGTH=78.
     */
    private static final int ERROR_MESSAGE_LENGTH = 78;

    /**
     * List of available menu options filtered by user role.
     * 
     * Maps to OPTN001-OPTN012 fields from COMEN01.bms where each option
     * displays a 40-character description. The list is dynamically filtered
     * based on the user's access level, supporting role-based menu display.
     * 
     * Validated to contain between 1-12 options matching the original screen
     * capacity. Each MenuOption object undergoes cascade validation to ensure
     * proper structure and data integrity.
     */
    @JsonProperty("menuOptions")
    @Size(min = 1, max = MAX_MENU_OPTIONS, message = "Menu options must contain between 1 and " + MAX_MENU_OPTIONS + " items")
    @Valid
    private List<MenuOption> menuOptions;

    /**
     * Current user name for personalization.
     * 
     * Provides user context for the main menu display, supporting the
     * personalized experience that was implied in the original COBOL
     * system through CICS user identification.
     */
    @JsonProperty("userName")
    private String userName;

    /**
     * Current system date for display.
     * 
     * Maps to CURDATE field from COMEN01.bms (PIC X(8)) which displays
     * the current date in mm/dd/yy format. Provides temporal context
     * matching the original screen layout positioning.
     */
    @JsonProperty("currentDate")
    private String currentDate;

    /**
     * Current system time for display.
     * 
     * Maps to CURTIME field from COMEN01.bms (PIC X(8)) which displays
     * the current time in hh:mm:ss format. Provides temporal context
     * matching the original screen layout positioning.
     */
    @JsonProperty("currentTime")
    private String currentTime;

    /**
     * Program name for system identification.
     * 
     * Maps to PGMNAME field from COMEN01.bms (PIC X(8)) which displays
     * the executing program identifier. Maintains system context and
     * debugging information from the original COBOL implementation.
     */
    @JsonProperty("programName")
    private String programName;

    /**
     * System identifier for environment context.
     * 
     * Provides system identification information to help users understand
     * which environment (development, test, production) they are accessing.
     * Extends the original system context beyond the single-system mainframe.
     */
    @JsonProperty("systemId")
    private String systemId;

    /**
     * Transaction code for the current screen.
     * 
     * Maps to TRNNAME field from COMEN01.bms (PIC X(4)) which identifies
     * the current CICS transaction code. Maintains the transaction context
     * from the original COBOL/CICS system for proper navigation and auditing.
     */
    @JsonProperty("transactionCode")
    private String transactionCode;

    /**
     * Error message for display when validation or processing errors occur.
     * 
     * Maps to ERRMSG field from COMEN01.bms (PIC X(78)) which provides
     * user feedback for errors and validation failures. Maintains the
     * error handling approach from the original COBOL system.
     */
    @JsonProperty("errorMessage")
    private String errorMessage;

    /**
     * Default constructor for framework instantiation.
     * Initializes empty menuOptions list to prevent null pointer exceptions.
     */
    public MainMenuResponse() {
        this.menuOptions = new java.util.ArrayList<>();
    }

    /**
     * Convenience constructor for creating a complete main menu response.
     * 
     * @param menuOptions List of menu options for the user
     * @param userName Current user name
     * @param currentDate Current system date
     * @param currentTime Current system time
     * @param programName Program identifier
     * @param systemId System identifier
     * @param transactionCode Transaction code
     */
    public MainMenuResponse(List<MenuOption> menuOptions, String userName, 
                           String currentDate, String currentTime,
                           String programName, String systemId, 
                           String transactionCode) {
        this.menuOptions = menuOptions != null ? menuOptions : new java.util.ArrayList<>();
        this.userName = userName;
        this.currentDate = currentDate;
        this.currentTime = currentTime;
        this.programName = programName;
        this.systemId = systemId;
        this.transactionCode = transactionCode;
    }

    /**
     * Utility method to format current date and time using LocalDateTime.
     * 
     * Sets currentDate and currentTime fields using the current system time,
     * formatted to match the original COBOL screen layout expectations.
     * Uses LocalDateTime.now() and format() methods as specified in external imports.
     */
    public void setCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        this.currentDate = now.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd/yy"));
        this.currentTime = now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    /**
     * Utility method to add a menu option to the list.
     * 
     * Validates that the maximum number of options is not exceeded before adding.
     * Provides a convenient way to build the menu options list while maintaining
     * the 12-option limit from the original COMEN01 screen layout.
     * 
     * @param menuOption The menu option to add
     * @return true if added successfully, false if limit would be exceeded
     */
    public boolean addMenuOption(MenuOption menuOption) {
        if (this.menuOptions.size() >= MAX_MENU_OPTIONS) {
            return false;
        }
        return this.menuOptions.add(menuOption);
    }

    /**
     * Utility method to filter menu options by access level.
     * 
     * Removes menu options that the user doesn't have permission to access,
     * supporting the role-based security model from the original RACF system.
     * Uses the MenuOption.getAccessLevel() method to determine permission.
     * 
     * @param userAccessLevel The user's access level
     */
    public void filterMenuOptionsByAccessLevel(String userAccessLevel) {
        if (userAccessLevel == null || this.menuOptions == null) {
            return;
        }
        
        this.menuOptions.removeIf(option -> {
            if (option.getAccessLevel() == null) {
                return false; // Keep options with no access level requirement
            }
            return !userAccessLevel.equalsIgnoreCase(option.getAccessLevel()) &&
                   !"ADMIN".equalsIgnoreCase(userAccessLevel); // ADMIN can access everything
        });
    }

    /**
     * Utility method to validate and truncate string fields to match COBOL limits.
     * 
     * Ensures that string fields don't exceed the original COBOL field lengths,
     * maintaining data integrity with the original system constraints.
     */
    public void validateFieldLengths() {
        if (this.transactionCode != null && this.transactionCode.length() > TRANSACTION_CODE_LENGTH) {
            this.transactionCode = this.transactionCode.substring(0, TRANSACTION_CODE_LENGTH);
        }
        
        if (this.programName != null && this.programName.length() > PROGRAM_NAME_LENGTH) {
            this.programName = this.programName.substring(0, PROGRAM_NAME_LENGTH);
        }
        
        if (this.errorMessage != null && this.errorMessage.length() > ERROR_MESSAGE_LENGTH) {
            this.errorMessage = this.errorMessage.substring(0, ERROR_MESSAGE_LENGTH);
        }
    }

    /**
     * Checks if there are any error messages present.
     * 
     * @return true if error message is not null and not empty
     */
    public boolean hasError() {
        return this.errorMessage != null && !this.errorMessage.trim().isEmpty();
    }

    /**
     * Gets the number of menu options currently in the list.
     * 
     * @return The count of menu options
     */
    public int getMenuOptionCount() {
        return this.menuOptions != null ? this.menuOptions.size() : 0;
    }
}