/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.time.LocalDateTime;

import com.carddemo.dto.MenuResponse;
import com.carddemo.dto.MenuOption;
import com.carddemo.config.MenuConfiguration;
import com.carddemo.security.AuthorizationService;
import com.carddemo.util.Constants;
import com.carddemo.exception.ValidationException;
import com.carddemo.security.SessionAttributes;
import com.carddemo.util.FormatUtil;

/**
 * Spring Boot service implementing main menu navigation logic translated from COMEN01C.cbl.
 * 
 * This service provides the core menu processing functionality that maintains exact functional
 * parity with the original COBOL CICS main menu program. It dynamically builds menu options
 * based on user permissions, handles menu selection validation, and routes to appropriate
 * transaction services while preserving the COBOL paragraph structure through corresponding
 * Java methods.
 * 
 * COBOL Program Translation:
 * - MAIN-PARA → buildMainMenu() method
 * - PROCESS-ENTER-KEY → processMenuSelection() method  
 * - BUILD-MENU-OPTIONS → buildMenuOptions() method
 * - POPULATE-HEADER-INFO → populateHeaderInfo() method
 * - SEND-MENU-SCREEN → MenuResponse DTO construction
 * 
 * The service integrates with Spring Security for role-based menu filtering, replacing
 * RACF authorization checks while maintaining identical access control logic.
 * User type checking (SEC-USR-TYPE "A" vs "U") is preserved through AuthorizationService.
 * 
 * Key Features:
 * - Dynamic menu generation based on user role (Admin vs User)
 * - Menu option validation matching COBOL edit routines
 * - Error handling and message display compatible with BMS screen patterns
 * - Header information population with current date/time
 * - Transaction routing preparation for Spring Boot controllers
 * 
 * This implementation addresses the requirements in Section 0.2.1 for COBOL-to-Java
 * program translation while maintaining paragraph structure and preserving all data
 * validation rules and business calculations without modification.
 */
@Service
public class MainMenuService {

    // Constants matching COBOL program values
    private static final String PROGRAM_NAME = "COMEN01C";
    private static final String TRANSACTION_ID = "CM00";
    private static final int MENU_OPTION_COUNT = 10;
    private static final String INVALID_KEY_MESSAGE = "Please enter a valid option number...";
    private static final String ACCESS_DENIED_MESSAGE = "No access - Admin Only option... ";
    private static final String THANK_YOU_MESSAGE = "Thank you for using CardDemo";
    
    @Autowired
    private MenuConfiguration menuConfiguration;
    
    @Autowired 
    private AuthorizationService authorizationService;

    /**
     * Builds the main menu response with filtered options based on user permissions.
     * 
     * This method implements the core logic from COMEN01C.cbl MAIN-PARA and SEND-MENU-SCREEN
     * procedures, constructing a complete menu response with role-based filtering applied.
     * It replicates the COBOL program flow for menu initialization and display preparation.
     * 
     * The method performs the following COBOL-equivalent operations:
     * 1. Initialize menu response structure (equivalent to MOVE LOW-VALUES TO COMEN1AO)
     * 2. Populate header information with current date/time (POPULATE-HEADER-INFO)
     * 3. Build filtered menu options based on user type (BUILD-MENU-OPTIONS)
     * 4. Apply role-based access control (CDEMO-USRTYP-USER checking)
     * 5. Construct final response DTO (SEND MAP equivalent)
     * 
     * @param userId User identifier from session (SEC-USR-ID)
     * @param userType User type indicator (SEC-USR-TYPE: 'A'=Admin, 'U'=User)
     * @param userName User display name (SEC-USR-NAME)
     * @return MenuResponse DTO containing filtered menu options and header information
     */
    public MenuResponse buildMainMenu(String userId, String userType, String userName) {
        // Initialize menu response (equivalent to MOVE LOW-VALUES TO COMEN1AO)
        MenuResponse menuResponse = new MenuResponse();
        
        // Populate header information with current date/time (POPULATE-HEADER-INFO equivalent)
        populateHeaderInfo(menuResponse);
        
        // Set user context information
        menuResponse.setUserName(userName);
        
        // Build menu options with role-based filtering (BUILD-MENU-OPTIONS equivalent)
        ArrayList<MenuOption> filteredOptions = buildMenuOptions(userType);
        menuResponse.setMenuOptions(filteredOptions);
        
        return menuResponse;
    }

    /**
     * Processes menu selection input and validates user choice.
     * 
     * This method translates the PROCESS-ENTER-KEY procedure from COMEN01C.cbl,
     * implementing the exact validation logic and error handling patterns from
     * the original COBOL program. It performs option number validation, access
     * level checking, and transaction routing preparation.
     * 
     * COBOL Logic Translation:
     * 1. Extract and validate option number from input (WS-OPTION processing)
     * 2. Check numeric validation (IS NOT NUMERIC check)
     * 3. Validate option range (> CDEMO-MENU-OPT-COUNT check)  
     * 4. Apply role-based access control (CDEMO-USRTYP-USER vs CDEMO-MENU-OPT-USRTYPE)
     * 5. Prepare transaction routing (XCTL PROGRAM equivalent setup)
     * 
     * @param selectedOption Selected menu option number from user input
     * @param userType User type for access control ('A'=Admin, 'U'=User)
     * @return Transaction code for routing or null if validation fails
     * @throws ValidationException for validation errors matching COBOL error handling
     */
    public String processMenuSelection(Integer selectedOption, String userType) {
        // Validate menu option (equivalent to PROCESS-ENTER-KEY validation logic)
        validateMenuOption(selectedOption, userType);
        
        // Get menu options to find target transaction
        ArrayList<MenuOption> menuOptions = buildMenuOptions(userType);
        
        // Find the selected option
        for (MenuOption option : menuOptions) {
            if (selectedOption.equals(option.getOptionNumber())) {
                // Check if program is available (NOT = 'DUMMY' equivalent)
                String transactionCode = option.getTransactionCode();
                if (transactionCode != null && !transactionCode.startsWith("DUMMY")) {
                    return transactionCode;
                }
                // If dummy transaction, return success but no routing
                return null;
            }
        }
        
        // Should not reach here due to validation, but safety check
        throw new ValidationException(INVALID_KEY_MESSAGE);
    }

    /**
     * Validates menu option selection against business rules.
     * 
     * This method implements the validation logic from PROCESS-ENTER-KEY procedure
     * in COMEN01C.cbl, ensuring menu option selections are valid and accessible
     * to the current user. It replicates the exact error checking patterns and
     * message generation from the original COBOL implementation.
     * 
     * Validation Rules (from COBOL logic):
     * 1. Option must be numeric (WS-OPTION IS NOT NUMERIC check)
     * 2. Option must be in valid range (1 to CDEMO-MENU-OPT-COUNT)
     * 3. Option must not be zero (WS-OPTION = ZEROS check)
     * 4. User must have appropriate access level (CDEMO-USRTYP-USER access check)
     * 
     * Error Handling:
     * - Invalid option number: "Please enter a valid option number..."
     * - Access denied: "No access - Admin Only option... "
     * 
     * @param selectedOption Menu option number to validate
     * @param userType User type for access control validation
     * @throws ValidationException if validation fails with appropriate error message
     */
    public void validateMenuOption(Integer selectedOption, String userType) {
        // Check for null option (equivalent to WS-OPTION = ZEROS)
        if (selectedOption == null || selectedOption == 0) {
            throw new ValidationException(INVALID_KEY_MESSAGE);
        }
        
        // Validate option range (equivalent to WS-OPTION > CDEMO-MENU-OPT-COUNT)
        if (selectedOption < 1 || selectedOption > MENU_OPTION_COUNT) {
            throw new ValidationException(INVALID_KEY_MESSAGE);
        }
        
        // Get menu options to check access level
        ArrayList<MenuOption> menuOptions = buildMenuOptions(null); // Get all options for validation
        
        // Find the selected option and check access
        for (MenuOption option : menuOptions) {
            if (selectedOption.equals(option.getOptionNumber())) {
                // Check user access level (equivalent to CDEMO-USRTYP-USER AND CDEMO-MENU-OPT-USRTYPE = 'A')
                if (Constants.USER_TYPE_USER.equals(userType) && 
                    Constants.USER_TYPE_ADMIN.equals(option.getAccessLevel())) {
                    throw new ValidationException(ACCESS_DENIED_MESSAGE);
                }
                return; // Valid option found
            }
        }
        
        // Option not found in menu
        throw new ValidationException(INVALID_KEY_MESSAGE);
    }

    /**
     * Builds filtered menu options based on user access level.
     * 
     * This method translates the BUILD-MENU-OPTIONS procedure from COMEN01C.cbl,
     * implementing the dynamic menu generation logic with role-based filtering.
     * It creates menu options from the static configuration and applies access
     * control rules matching the original COBOL logic.
     * 
     * COBOL Logic Translation:
     * 1. Iterate through CDEMO-MENU-OPTIONS-DATA (VARYING WS-IDX FROM 1 BY 1)
     * 2. Build option display text (STRING CDEMO-MENU-OPT-NUM, '. ', CDEMO-MENU-OPT-NAME)
     * 3. Apply user type filtering for option display
     * 4. Map to screen output fields (OPTN001O through OPTN012O equivalent)
     * 
     * Access Control:
     * - Admin users ('A'): Can see all menu options
     * - Regular users ('U'): Can only see options with 'U' or null access level
     * - null userType: Returns all options for validation purposes
     * 
     * @param userType User type for filtering ('A'=Admin, 'U'=User, null=all options)
     * @return ArrayList of MenuOption objects filtered by access level
     */
    public ArrayList<MenuOption> buildMenuOptions(String userType) {
        ArrayList<MenuOption> filteredOptions = new ArrayList<>();
        
        // Get base menu options from configuration (equivalent to COPY COMEN02Y)
        var baseOptions = menuConfiguration.getMenuOptions();
        
        // Filter options based on user type (equivalent to BUILD-MENU-OPTIONS logic)
        for (MenuOption option : baseOptions) {
            boolean includeOption = true;
            
            // Apply role-based filtering unless requesting all options
            if (userType != null) {
                String optionAccessLevel = option.getAccessLevel();
                
                // Admin users can access everything
                if (Constants.USER_TYPE_ADMIN.equals(userType)) {
                    includeOption = true;
                }
                // Regular users can only access User-level options  
                else if (Constants.USER_TYPE_USER.equals(userType)) {
                    // Include if option has User access or no specific access requirement
                    includeOption = (optionAccessLevel == null || 
                                   optionAccessLevel.equals(Constants.USER_TYPE_USER));
                }
            }
            
            if (includeOption) {
                // Create filtered option with display formatting
                MenuOption filteredOption = new MenuOption();
                filteredOption.setOptionNumber(option.getOptionNumber());
                
                // Build display text (equivalent to STRING operation in COBOL)
                String displayText = option.getOptionNumber() + ". " + option.getDescription();
                filteredOption.setDescription(displayText);
                filteredOption.setTransactionCode(option.getTransactionCode());
                filteredOption.setAccessLevel(option.getAccessLevel());
                filteredOption.setEnabled(true);
                
                filteredOptions.add(filteredOption);
            }
        }
        
        return filteredOptions;
    }

    /**
     * Populates header information for menu response with current date and time.
     * 
     * This method implements the POPULATE-HEADER-INFO procedure from COMEN01C.cbl,
     * setting up the screen header fields with system information and current
     * timestamp. It replicates the COBOL date/time handling using Java equivalent
     * operations while maintaining exact display format compatibility.
     * 
     * COBOL Logic Translation:
     * 1. Get current date/time (FUNCTION CURRENT-DATE equivalent)
     * 2. Format date components (WS-CURDATE-MM, WS-CURDATE-DD, WS-CURDATE-YY)
     * 3. Format time components (WS-CURTIME-HH, WS-CURTIME-MM, WS-CURTIME-SS)
     * 4. Set system identification fields (TRNNAMEO, PGMNAMEO)
     * 5. Apply formatted values to screen output (CURDATEO, CURTIMEO)
     * 
     * Header Fields (mapping to BMS screen):
     * - currentDate: Maps to CURDATEO (PIC X(8) mm/dd/yy format)
     * - currentTime: Maps to CURTIMEO (PIC X(8) hh:mm:ss format)  
     * - programName: Maps to PGMNAMEO (PIC X(8) program identifier)
     * - systemId: Maps to TRNNAMEO (PIC X(4) transaction identifier)
     * 
     * @param menuResponse MenuResponse object to populate with header information
     */
    public void populateHeaderInfo(MenuResponse menuResponse) {
        // Get current date/time (equivalent to FUNCTION CURRENT-DATE)
        LocalDateTime currentDateTime = LocalDateTime.now();
        
        // Format date and time for display (equivalent to COBOL date/time formatting)
        String formattedDate = FormatUtil.formatDate(currentDateTime);
        String formattedTime = FormatUtil.formatDateTime(currentDateTime);
        
        // Set header information (equivalent to POPULATE-HEADER-INFO assignments)
        menuResponse.setCurrentDate(formattedDate);
        menuResponse.setCurrentTime(formattedTime);
        menuResponse.setProgramName(PROGRAM_NAME);
        menuResponse.setSystemId(TRANSACTION_ID);
    }
}