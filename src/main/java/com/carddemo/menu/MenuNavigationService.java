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
import java.util.stream.Collectors;

/**
 * Menu Navigation Service for CardDemo application.
 * 
 * This service provides role-based menu navigation functionality converted from COBOL COMEN01C.cbl.
 * It implements dynamic menu option generation, user role validation, and security-filtered menu access
 * for both regular users and administrators while maintaining exact functional equivalence with the
 * original COBOL implementation.
 * 
 * Key Features:
 * - Role-based menu filtering equivalent to COBOL CDEMO-USRTYP-USER validation logic
 * - Dynamic menu option generation from COMEN02Y.cpy and COADM02Y.cpy structures
 * - JWT token validation and user role determination via Spring Security integration
 * - RESTful function dispatch to appropriate microservices replacing CICS XCTL behavior
 * - Comprehensive error handling and audit logging for compliance requirements
 * 
 * Original COBOL Mapping:
 * - COMEN01C.cbl (lines 75-278) → MenuNavigationService methods
 * - CDEMO-MENU-OPT-COUNT processing → getMenuOptionsForRole() logic
 * - CDEMO-USRTYP-USER validation → role-based filtering implementation
 * - WS-OPTION validation → validateMenuSelection() method
 * - BUILD-MENU-OPTIONS (lines 236-277) → buildMenuResponse() method
 * 
 * Security Integration:
 * - Spring Security Authentication context for user role determination
 * - JWT token validation for user session management
 * - Role-based access control equivalent to RACF security patterns
 * - Audit logging for menu access patterns and security events
 * 
 * Performance Requirements:
 * - Menu generation completes within 200ms for 95th percentile responses
 * - Thread-safe implementation supporting 10,000+ concurrent users
 * - Efficient memory usage with ArrayList-based menu option collections
 * - Optimized role filtering to minimize processing overhead
 * 
 * @author Blitzy agent
 * @version 1.0
 * @since 2024-01-01
 */
@Service
public class MenuNavigationService {

    private static final Logger logger = LoggerFactory.getLogger(MenuNavigationService.class);
    
    // Service metadata constants matching COBOL program identification
    private static final String PROGRAM_NAME = "COMEN01C";
    private static final String TRANSACTION_ID = "CM00";
    
    // Regular user menu options from COMEN02Y.cpy (10 options)
    // Preserves exact COBOL structure: CDEMO-MENU-OPT-NUM, CDEMO-MENU-OPT-NAME, CDEMO-MENU-OPT-PGMNAME, CDEMO-MENU-OPT-USRTYPE
    private static final MenuOptionDTO[] REGULAR_USER_MENU_OPTIONS = {
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
    };
    
    // Admin menu options from COADM02Y.cpy (4 options)
    // Preserves exact COBOL structure: CDEMO-ADMIN-OPT-NUM, CDEMO-ADMIN-OPT-NAME, CDEMO-ADMIN-OPT-PGMNAME
    private static final MenuOptionDTO[] ADMIN_MENU_OPTIONS = {
        new MenuOptionDTO(1, "User List (Security)", "COUSR00C"),
        new MenuOptionDTO(2, "User Add (Security)", "COUSR01C"),
        new MenuOptionDTO(3, "User Update (Security)", "COUSR02C"),
        new MenuOptionDTO(4, "User Delete (Security)", "COUSR03C")
    };
    
    // Menu option count constants matching COBOL definitions
    private static final int REGULAR_MENU_OPT_COUNT = 10; // CDEMO-MENU-OPT-COUNT from COMEN02Y.cpy
    private static final int ADMIN_MENU_OPT_COUNT = 4;    // CDEMO-ADMIN-OPT-COUNT from COADM02Y.cpy

    /**
     * Get menu options for the current authenticated user.
     * 
     * This method replicates the main menu processing logic from COMEN01C.cbl (lines 75-111).
     * It determines the current user's role from the Spring Security context and returns
     * appropriate menu options filtered by access permissions.
     * 
     * Original COBOL flow:
     * - MAIN-PARA processing logic
     * - CDEMO-USRTYP-USER validation
     * - Menu option filtering based on user type
     * - Error handling for invalid user sessions
     * 
     * @return MenuResponseDTO containing filtered menu options and user context
     */
    public MenuResponseDTO getMenuOptions() {
        logger.info("Processing menu options request for current user");
        
        try {
            // Get current user role from Spring Security context
            UserType currentUserType = getCurrentUserRole();
            if (currentUserType == null) {
                logger.warn("No authenticated user found in security context");
                return MenuResponseDTO.error("Authentication required to access menu options");
            }
            
            logger.debug("Processing menu options for user type: {}", currentUserType);
            
            // Get filtered menu options based on user role
            ArrayList<MenuOptionDTO> filteredOptions = getMenuOptionsForRole(currentUserType);
            
            // Build successful response
            MenuResponseDTO response = buildMenuResponse(filteredOptions, currentUserType);
            
            logger.info("Successfully processed {} menu options for user type: {}", 
                       filteredOptions.size(), currentUserType);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error processing menu options request", e);
            return MenuResponseDTO.error("Unable to retrieve menu options: " + e.getMessage());
        }
    }

    /**
     * Validate if the current user can access a specific menu option.
     * 
     * This method implements the menu option validation logic from COMEN01C.cbl (lines 127-144).
     * It checks if the selected option number is valid and if the user has sufficient permissions
     * to access the corresponding functionality.
     * 
     * Original COBOL validation logic:
     * - WS-OPTION numeric validation
     * - CDEMO-MENU-OPT-COUNT boundary checking
     * - CDEMO-USRTYP-USER and CDEMO-MENU-OPT-USRTYPE permission validation
     * - Error message generation for invalid selections
     * 
     * @param optionNumber The menu option number to validate (1-based)
     * @return true if the user can access the menu option, false otherwise
     */
    public boolean validateMenuSelection(int optionNumber) {
        logger.debug("Validating menu selection: option number {}", optionNumber);
        
        try {
            // Get current user role
            UserType currentUserType = getCurrentUserRole();
            if (currentUserType == null) {
                logger.warn("No authenticated user found for menu validation");
                return false;
            }
            
            // Get all available options for user role
            ArrayList<MenuOptionDTO> availableOptions = getMenuOptionsForRole(currentUserType);
            
            // Check if option number is within valid range
            if (optionNumber < 1 || optionNumber > availableOptions.size()) {
                logger.warn("Invalid menu option number {} for user type {}", optionNumber, currentUserType);
                return false;
            }
            
            // Find the specific option and validate access
            MenuOptionDTO selectedOption = availableOptions.get(optionNumber - 1);
            boolean hasAccess = selectedOption.isAccessibleTo(currentUserType.getCode());
            
            logger.debug("Menu validation result for option {} ({}): {}", 
                        optionNumber, selectedOption.getTargetService(), hasAccess);
            
            return hasAccess;
            
        } catch (Exception e) {
            logger.error("Error validating menu selection for option {}", optionNumber, e);
            return false;
        }
    }

    /**
     * Get menu options filtered by user role.
     * 
     * This method implements the role-based menu filtering logic equivalent to COBOL
     * CDEMO-USRTYP-USER validation patterns. It returns appropriate menu options
     * based on the user's access level while preserving exact functional equivalence.
     * 
     * Role-based filtering logic:
     * - ADMIN users: Get both admin-specific and regular user options
     * - REGULAR users: Get only regular user options with access level validation
     * - Invalid roles: Return empty menu with error status
     * 
     * @param userType The user's role type for menu filtering
     * @return ArrayList of MenuOptionDTO filtered by user permissions
     */
    public ArrayList<MenuOptionDTO> getMenuOptionsForRole(UserType userType) {
        logger.debug("Getting menu options for user type: {}", userType);
        
        ArrayList<MenuOptionDTO> menuOptions = new ArrayList<>();
        
        try {
            if (userType == null) {
                logger.warn("Null user type provided for menu filtering");
                return menuOptions;
            }
            
            // Admin users get admin-specific options
            if (userType.isAdmin()) {
                // Add admin menu options
                for (MenuOptionDTO option : ADMIN_MENU_OPTIONS) {
                    menuOptions.add(option);
                }
                
                // Admin users also get access to regular user options
                for (MenuOptionDTO option : REGULAR_USER_MENU_OPTIONS) {
                    menuOptions.add(option);
                }
                
                logger.debug("Retrieved {} admin menu options and {} regular options", 
                           ADMIN_MENU_OPT_COUNT, REGULAR_MENU_OPT_COUNT);
            } 
            // Regular users get filtered regular options
            else if (userType.isUser()) {
                // Add regular user menu options with access level validation
                for (MenuOptionDTO option : REGULAR_USER_MENU_OPTIONS) {
                    if (option.isAccessibleTo(userType.getCode())) {
                        menuOptions.add(option);
                    }
                }
                
                logger.debug("Retrieved {} regular user menu options", menuOptions.size());
            }
            else {
                logger.warn("Unsupported user type: {}", userType);
            }
            
        } catch (Exception e) {
            logger.error("Error getting menu options for user type: {}", userType, e);
        }
        
        return menuOptions;
    }

    /**
     * Get filtered menu options based on user access level.
     * 
     * This method provides internal filtering logic that replicates the COBOL
     * access control validation patterns. It ensures that only menu options
     * appropriate for the user's access level are returned.
     * 
     * @param userAccessLevel The user's access level code ('A' for Admin, 'R' for Regular)
     * @return ArrayList of MenuOptionDTO filtered by access level
     */
    public ArrayList<MenuOptionDTO> getFilteredMenuOptions(String userAccessLevel) {
        logger.debug("Filtering menu options by access level: {}", userAccessLevel);
        
        ArrayList<MenuOptionDTO> allOptions = new ArrayList<>();
        
        try {
            // Convert access level to UserType for processing
            UserType userType = UserType.fromCode(userAccessLevel).orElse(null);
            if (userType == null) {
                logger.warn("Invalid access level code provided: {}", userAccessLevel);
                return allOptions;
            }
            
            // Get role-based options
            allOptions = getMenuOptionsForRole(userType);
            
            // Apply additional filtering based on access level
            ArrayList<MenuOptionDTO> filteredOptions = allOptions.stream()
                .filter(option -> option.isAccessibleTo(userAccessLevel))
                .collect(Collectors.toCollection(ArrayList::new));
            
            logger.debug("Filtered {} options to {} accessible options for access level: {}", 
                        allOptions.size(), filteredOptions.size(), userAccessLevel);
            
            return filteredOptions;
            
        } catch (Exception e) {
            logger.error("Error filtering menu options by access level: {}", userAccessLevel, e);
            return new ArrayList<>();
        }
    }

    /**
     * Get the current user's role from Spring Security context.
     * 
     * This method extracts the user's role from the JWT token stored in the Spring Security
     * authentication context. It replaces the COBOL user type determination logic from
     * CSUSR01Y.cpy (SEC-USR-TYPE field processing).
     * 
     * @return UserType enum representing the current user's role, or null if not authenticated
     */
    public UserType getCurrentUserRole() {
        try {
            // Get current authentication from Spring Security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.debug("No authenticated user found in security context");
                return null;
            }
            
            // Extract user role from authentication authorities
            String userRole = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .findFirst()
                .orElse(null);
            
            if (userRole == null) {
                logger.warn("No role found in authentication authorities for user: {}", 
                           authentication.getPrincipal());
                return null;
            }
            
            // Convert Spring Security role to UserType
            if ("ROLE_ADMIN".equals(userRole)) {
                return UserType.ADMIN;
            } else if ("ROLE_USER".equals(userRole)) {
                return UserType.USER;
            } else {
                logger.warn("Unknown user role: {}", userRole);
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Error getting current user role from security context", e);
            return null;
        }
    }

    /**
     * Check if the current user is an administrator.
     * 
     * This method implements the COBOL 88-level condition equivalent to
     * CDEMO-USRTYP-ADMIN validation from the original menu processing logic.
     * 
     * @return true if current user is an administrator, false otherwise
     */
    public boolean isAdminUser() {
        try {
            UserType currentUserType = getCurrentUserRole();
            boolean isAdmin = currentUserType != null && currentUserType.isAdmin();
            
            logger.debug("Admin user check result: {}", isAdmin);
            return isAdmin;
            
        } catch (Exception e) {
            logger.error("Error checking admin user status", e);
            return false;
        }
    }

    /**
     * Build menu response DTO with filtered options and user context.
     * 
     * This method creates a complete menu response structure equivalent to the COBOL
     * screen output generation logic from COMEN01C.cbl (BUILD-MENU-OPTIONS processing).
     * It includes all necessary metadata for React frontend consumption.
     * 
     * Original COBOL mapping:
     * - CDEMO-MENU-OPT-COUNT → menuOptions list size
     * - CDEMO-USRTYP-USER → userRole field
     * - WS-ERR-FLG → status indicator
     * - WS-MESSAGE → message field
     * 
     * @param menuOptions List of filtered menu options for the user
     * @param userType The user's role type for context information
     * @return MenuResponseDTO containing complete menu response data
     */
    public MenuResponseDTO buildMenuResponse(ArrayList<MenuOptionDTO> menuOptions, UserType userType) {
        logger.debug("Building menu response for user type: {} with {} options", 
                    userType, menuOptions.size());
        
        try {
            // Create response DTO with menu options and user context
            MenuResponseDTO response = new MenuResponseDTO();
            response.setMenuOptions(menuOptions);
            response.setUserRole(userType != null ? userType.getCode() : null);
            response.setStatus("SUCCESS");
            
            // Generate appropriate message based on menu content
            String message = "";
            if (menuOptions.isEmpty()) {
                message = "No menu options available for your access level";
                response.setStatus("WARNING");
            } else {
                message = String.format("Retrieved %d menu options for %s user", 
                                      menuOptions.size(), 
                                      userType != null ? userType.getDescription() : "unknown");
            }
            response.setMessage(message);
            
            logger.info("Built menu response: {} options, status: {}, user: {}", 
                       menuOptions.size(), response.getStatus(), 
                       userType != null ? userType.getDescription() : "unknown");
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error building menu response", e);
            return MenuResponseDTO.error("Unable to build menu response: " + e.getMessage());
        }
    }
}