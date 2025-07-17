/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.menu;

import com.carddemo.common.dto.MenuOptionDTO;
import com.carddemo.common.dto.MenuResponseDTO;
import com.carddemo.common.enums.UserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core business logic service implementing role-based menu navigation functionality 
 * converted from COBOL COMEN01C.cbl.
 * 
 * Provides dynamic menu option generation, user role validation, and security-filtered 
 * menu access for both regular users and administrators in the CardDemo application.
 * 
 * Original COBOL Program: COMEN01C.cbl
 * - Function: Main Menu for Regular Users
 * - Transaction ID: CM00
 * - Purpose: Display and process menu options based on user type
 * 
 * COBOL-to-Java Mappings:
 * - PROCESS-ENTER-KEY paragraph -> validateMenuSelection() method
 * - BUILD-MENU-OPTIONS paragraph -> getMenuOptionsForRole() method
 * - CDEMO-USRTYP-USER validation -> getCurrentUserRole() method
 * - CDEMO-MENU-OPT-USRTYPE check -> getFilteredMenuOptions() method
 * 
 * Data Structure Mappings:
 * - COMEN02Y.cpy (10 regular user options) -> REGULAR_USER_MENU_OPTIONS
 * - COADM02Y.cpy (4 admin options) -> ADMIN_MENU_OPTIONS
 * - CSUSR01Y.cpy SEC-USR-TYPE field -> UserType enum integration
 * 
 * Spring Security Integration:
 * - JWT token validation for user context
 * - Role-based access control (ROLE_USER, ROLE_ADMIN)
 * - Security context management for menu filtering
 * 
 * @author AWS CardDemo Migration Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
public class MenuNavigationService {

    private static final Logger logger = LoggerFactory.getLogger(MenuNavigationService.class);

    // Constants for menu option counts (from COBOL copybooks)
    private static final int REGULAR_USER_MENU_COUNT = 10; // CDEMO-MENU-OPT-COUNT from COMEN02Y.cpy
    private static final int ADMIN_MENU_COUNT = 4; // CDEMO-ADMIN-OPT-COUNT from COADM02Y.cpy

    // Regular user menu options from COMEN02Y.cpy
    private static final List<MenuOptionDTO> REGULAR_USER_MENU_OPTIONS = List.of(
        new MenuOptionDTO(1, "Account View", "COACTVWC", "U"),
        new MenuOptionDTO(2, "Account Update", "COACTUPC", "U"), 
        new MenuOptionDTO(3, "Credit Card List", "COCRDLIC", "U"),
        new MenuOptionDTO(4, "Credit Card View", "COCRDSLC", "U"),
        new MenuOptionDTO(5, "Credit Card Update", "COCRDUPC", "U"),
        new MenuOptionDTO(6, "Transaction List", "COTRN00C", "U"),
        new MenuOptionDTO(7, "Transaction View", "COTRN01C", "U"),
        new MenuOptionDTO(8, "Transaction Add", "COTRN02C", "U"),
        new MenuOptionDTO(9, "Transaction Reports", "CORPT00C", "U"),
        new MenuOptionDTO(10, "Bill Payment", "COBIL00C", "U")
    );

    // Admin menu options from COADM02Y.cpy
    private static final List<MenuOptionDTO> ADMIN_MENU_OPTIONS = List.of(
        new MenuOptionDTO(1, "User List (Security)", "COUSR00C"),
        new MenuOptionDTO(2, "User Add (Security)", "COUSR01C"),
        new MenuOptionDTO(3, "User Update (Security)", "COUSR02C"),
        new MenuOptionDTO(4, "User Delete (Security)", "COUSR03C")
    );

    /**
     * Gets menu options for the currently authenticated user.
     * 
     * Implements the main menu logic from COBOL COMEN01C.cbl MAIN-PARA section.
     * Determines user role from Spring Security context and returns appropriate
     * menu options with role-based filtering.
     * 
     * COBOL equivalent:
     * - MAIN-PARA processing logic
     * - SEND-MENU-SCREEN functionality
     * - Role validation from CDEMO-USRTYP-USER
     * 
     * @return MenuResponseDTO containing filtered menu options for the user's role
     */
    public MenuResponseDTO getMenuOptions() {
        logger.info("Processing menu options request for authenticated user");
        
        try {
            UserType currentUserType = getCurrentUserRole();
            logger.debug("Current user type determined: {}", currentUserType);

            List<MenuOptionDTO> menuOptions = getMenuOptionsForRole(currentUserType);
            List<MenuOptionDTO> filteredOptions = getFilteredMenuOptions(menuOptions, currentUserType);

            logger.info("Generated {} menu options for user type: {}", filteredOptions.size(), currentUserType);
            
            return buildMenuResponse(filteredOptions, currentUserType, "SUCCESS", "");
            
        } catch (Exception ex) {
            logger.error("Error processing menu options request", ex);
            return MenuResponseDTO.createErrorResponse("USER", "Error retrieving menu options");
        }
    }

    /**
     * Validates menu selection against user permissions and available options.
     * 
     * Implements the PROCESS-ENTER-KEY paragraph from COBOL COMEN01C.cbl.
     * Validates that the selected option number is valid, numeric, and accessible
     * to the current user's role.
     * 
     * COBOL equivalent:
     * - PROCESS-ENTER-KEY paragraph logic
     * - WS-OPTION validation (numeric check, range check)
     * - CDEMO-USRTYP-USER and CDEMO-MENU-OPT-USRTYPE access control
     * 
     * @param optionNumber the menu option number to validate (1-based)
     * @return true if the option is valid and accessible, false otherwise
     */
    public boolean validateMenuSelection(int optionNumber) {
        logger.debug("Validating menu selection: option {}", optionNumber);
        
        try {
            // Validate numeric range (COBOL: WS-OPTION IS NOT NUMERIC OR WS-OPTION > CDEMO-MENU-OPT-COUNT)
            if (optionNumber <= 0) {
                logger.warn("Invalid option number: {} - must be greater than 0", optionNumber);
                return false;
            }

            UserType currentUserType = getCurrentUserRole();
            List<MenuOptionDTO> availableOptions = getMenuOptionsForRole(currentUserType);
            List<MenuOptionDTO> filteredOptions = getFilteredMenuOptions(availableOptions, currentUserType);

            // Check if option exists in filtered menu
            boolean optionExists = filteredOptions.stream()
                .anyMatch(option -> option.getOptionNumber().equals(optionNumber));

            if (!optionExists) {
                logger.warn("Option {} not available for user type {}", optionNumber, currentUserType);
                return false;
            }

            // Additional access level validation for admin-only options
            MenuOptionDTO selectedOption = filteredOptions.stream()
                .filter(option -> option.getOptionNumber().equals(optionNumber))
                .findFirst()
                .orElse(null);

            if (selectedOption != null && selectedOption.isAdminOnly() && !isAdminUser()) {
                logger.warn("Access denied: Option {} requires admin privileges", optionNumber);
                return false;
            }

            logger.debug("Menu selection validated successfully: option {}", optionNumber);
            return true;

        } catch (Exception ex) {
            logger.error("Error validating menu selection for option {}", optionNumber, ex);
            return false;
        }
    }

    /**
     * Gets menu options for a specific user role.
     * 
     * Implements the BUILD-MENU-OPTIONS paragraph from COBOL COMEN01C.cbl.
     * Returns the appropriate menu options based on user type (admin vs regular user).
     * 
     * COBOL equivalent:
     * - BUILD-MENU-OPTIONS paragraph logic
     * - CDEMO-MENU-OPT-COUNT processing
     * - Menu option array population
     * 
     * @param userType the user type to get menu options for
     * @return List of MenuOptionDTO objects for the specified user type
     */
    public List<MenuOptionDTO> getMenuOptionsForRole(UserType userType) {
        logger.debug("Getting menu options for user type: {}", userType);
        
        List<MenuOptionDTO> menuOptions = new ArrayList<>();
        
        if (userType == UserType.ADMIN) {
            // Admin users get both regular and admin options
            menuOptions.addAll(REGULAR_USER_MENU_OPTIONS);
            menuOptions.addAll(ADMIN_MENU_OPTIONS);
            logger.debug("Added {} regular + {} admin options for admin user", 
                        REGULAR_USER_MENU_COUNT, ADMIN_MENU_COUNT);
        } else {
            // Regular users get only regular options
            menuOptions.addAll(REGULAR_USER_MENU_OPTIONS);
            logger.debug("Added {} regular options for regular user", REGULAR_USER_MENU_COUNT);
        }
        
        return menuOptions;
    }

    /**
     * Filters menu options based on user role and access level.
     * 
     * Implements the access control logic from COBOL COMEN01C.cbl PROCESS-ENTER-KEY.
     * Removes admin-only options for regular users and applies role-based filtering.
     * 
     * COBOL equivalent:
     * - CDEMO-USRTYP-USER AND CDEMO-MENU-OPT-USRTYPE(WS-OPTION) = 'A' validation
     * - "No access - Admin Only option" error handling
     * 
     * @param menuOptions the complete list of menu options
     * @param userType the current user's type for filtering
     * @return List of MenuOptionDTO objects filtered by user access level
     */
    public List<MenuOptionDTO> getFilteredMenuOptions(List<MenuOptionDTO> menuOptions, UserType userType) {
        logger.debug("Filtering menu options for user type: {}", userType);
        
        if (userType == UserType.ADMIN) {
            // Admin users have access to all options
            logger.debug("Admin user - no filtering applied");
            return new ArrayList<>(menuOptions);
        }
        
        // Regular users - filter out admin-only options
        List<MenuOptionDTO> filteredOptions = menuOptions.stream()
            .filter(option -> !option.isAdminOnly())
            .collect(Collectors.toList());
        
        logger.debug("Filtered {} admin-only options for regular user", 
                    menuOptions.size() - filteredOptions.size());
        
        return filteredOptions;
    }

    /**
     * Gets the current user's role from Spring Security context.
     * 
     * Implements the user type determination logic from COBOL COMEN01C.cbl.
     * Extracts user role from JWT token claims and maps to UserType enum.
     * 
     * COBOL equivalent:
     * - CDEMO-USER-TYPE field access
     * - SEC-USR-TYPE field mapping
     * - User type validation logic
     * 
     * @return UserType enum representing the current user's role
     */
    public UserType getCurrentUserRole() {
        logger.debug("Determining current user role from security context");
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("No authenticated user found in security context");
                return UserType.USER; // Default to regular user
            }

            // Extract user type from authorities
            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            
            UserType userType = isAdmin ? UserType.ADMIN : UserType.USER;
            logger.debug("Current user role determined: {}", userType);
            
            return userType;
            
        } catch (Exception ex) {
            logger.error("Error determining user role from security context", ex);
            return UserType.USER; // Default to regular user on error
        }
    }

    /**
     * Checks if the current user has administrative privileges.
     * 
     * Implements the CDEMO-USRTYP-ADMIN condition from COBOL.
     * Validates admin access for menu options and operations.
     * 
     * COBOL equivalent:
     * - CDEMO-USRTYP-ADMIN 88-level condition
     * - Administrative access validation
     * 
     * @return true if current user is an admin, false otherwise
     */
    public boolean isAdminUser() {
        UserType currentUserType = getCurrentUserRole();
        boolean isAdmin = currentUserType == UserType.ADMIN;
        logger.debug("Admin user check: {}", isAdmin);
        return isAdmin;
    }

    /**
     * Builds a complete menu response with options, user context, and status.
     * 
     * Implements the response building logic from COBOL COMEN01C.cbl.
     * Constructs MenuResponseDTO with appropriate status and message handling.
     * 
     * COBOL equivalent:
     * - WS-MESSAGE handling
     * - WS-ERR-FLG status management
     * - Response preparation logic
     * 
     * @param menuOptions the filtered menu options to include
     * @param userType the current user's type
     * @param status the response status
     * @param message the response message
     * @return MenuResponseDTO containing the complete menu response
     */
    public MenuResponseDTO buildMenuResponse(List<MenuOptionDTO> menuOptions, UserType userType, 
                                           String status, String message) {
        logger.debug("Building menu response for user type: {} with status: {}", userType, status);
        
        String userRoleString = userType == UserType.ADMIN ? "ADMIN" : "USER";
        
        MenuResponseDTO response = new MenuResponseDTO(menuOptions, userRoleString, status, message);
        
        logger.info("Built menu response with {} options for {} user", 
                   menuOptions.size(), userRoleString);
        
        return response;
    }
}