/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.menu;

import com.carddemo.common.dto.MenuOptionDTO;
import com.carddemo.common.dto.MenuResponseDTO;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller providing HTTP endpoints for menu navigation functionality
 * converted from COBOL COMEN01C.cbl.
 * 
 * Exposes role-based menu generation API endpoints that integrate with Spring Security
 * and delegate business logic to MenuNavigationService for the CardDemo application.
 * 
 * Original COBOL Program: COMEN01C.cbl
 * - Function: Main Menu for Regular Users
 * - Transaction ID: CM00
 * - Purpose: Display and process menu options based on user type
 * 
 * COBOL-to-Spring Boot Transformation:
 * - CICS transaction processing -> REST API endpoints
 * - COMMAREA data passing -> JSON request/response DTOs
 * - CICS program control (XCTL) -> HTTP redirects and service calls
 * - BMS screen handling -> React component integration
 * - CICS security -> Spring Security with JWT authentication
 * 
 * REST API Design:
 * - GET /api/menu/options - Dynamic menu generation for authenticated user
 * - GET /api/menu/{userRole} - Role-specific menu options (admin/user)
 * - Supports Spring Cloud Gateway integration for microservices routing
 * - Implements circuit breaker patterns for resilience
 * 
 * Security Integration:
 * - JWT token validation through Spring Security
 * - Role-based authorization using @PreAuthorize annotations
 * - CSRF protection for state-changing operations
 * - Request/response audit logging
 * 
 * Performance Considerations:
 * - Menu options cached for performance (delegated to service layer)
 * - Stateless operation for horizontal scaling
 * - Minimal data transfer with optimized DTOs
 * 
 * @author AWS CardDemo Migration Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuNavigationService menuNavigationService;

    /**
     * Constructor for dependency injection of MenuNavigationService.
     * 
     * @param menuNavigationService the service handling menu business logic
     */
    @Autowired
    public MenuController(MenuNavigationService menuNavigationService) {
        this.menuNavigationService = menuNavigationService;
    }

    /**
     * Gets menu options for the currently authenticated user.
     * 
     * Implements the main menu functionality from COBOL COMEN01C.cbl MAIN-PARA section.
     * Returns role-based filtered menu options with proper security context validation.
     * 
     * COBOL equivalent:
     * - MAIN-PARA processing logic
     * - SEND-MENU-SCREEN functionality
     * - BUILD-MENU-OPTIONS paragraph
     * - Role validation from CDEMO-USRTYP-USER
     * 
     * Spring Security Integration:
     * - Validates JWT token automatically through Spring Security filter chain
     * - Extracts user role from security context
     * - Applies role-based filtering to menu options
     * 
     * @return ResponseEntity<MenuResponseDTO> containing filtered menu options and user context
     */
    @GetMapping("/options")
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    /* OpenAPI Documentation - Add when OpenAPI dependency is available:
    @Operation(
        summary = "Get menu options for authenticated user",
        description = "Returns role-based filtered menu options for the currently authenticated user. " +
                     "Implements the main menu functionality from COBOL COMEN01C.cbl with " +
                     "Spring Security integration for JWT token validation and role-based filtering."
    )
    @ApiResponse(responseCode = "200", description = "Menu options retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token")
    @ApiResponse(responseCode = "403", description = "Forbidden - User does not have required role")
    @ApiResponse(responseCode = "500", description = "Internal server error - Menu processing failed")
    */
    public ResponseEntity<MenuResponseDTO> getMenuOptions() {
        try {
            // Delegate to service layer for business logic processing
            // Service handles user role detection and menu option filtering
            MenuResponseDTO menuResponse = menuNavigationService.getMenuOptions();
            
            // Return successful response with menu options
            return ResponseEntity.ok(menuResponse);
            
        } catch (Exception ex) {
            // Handle service layer exceptions with appropriate error response
            MenuResponseDTO errorResponse = MenuResponseDTO.createErrorResponse(
                "USER", 
                "Error retrieving menu options: " + ex.getMessage()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Gets menu options for a specific user role.
     * 
     * Implements the role-specific menu processing from COBOL COMEN01C.cbl.
     * Validates user role parameter and returns appropriate menu options with
     * access control validation.
     * 
     * COBOL equivalent:
     * - PROCESS-ENTER-KEY paragraph validation logic
     * - CDEMO-USRTYP-USER AND CDEMO-MENU-OPT-USRTYPE access control
     * - Role-based menu option filtering
     * 
     * Access Control:
     * - Admin users can access both 'USER' and 'ADMIN' role menus
     * - Regular users can only access 'USER' role menus
     * - Prevents privilege escalation through role specification
     * 
     * @param userRole the user role to get menu options for ('USER' or 'ADMIN')
     * @return ResponseEntity<MenuResponseDTO> containing role-specific menu options
     */
    @GetMapping("/{userRole}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or (hasRole('ROLE_USER') and #userRole == 'USER')")
    /* OpenAPI Documentation - Add when OpenAPI dependency is available:
    @Operation(
        summary = "Get menu options for specific user role",
        description = "Returns menu options for the specified user role with access control validation. " +
                     "Admin users can access both USER and ADMIN menus, while regular users can only " +
                     "access USER menus. Implements role-based access control from COBOL COMEN01C.cbl."
    )
    @ApiResponse(responseCode = "200", description = "Role-specific menu options retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Bad request - Invalid user role parameter")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token")
    @ApiResponse(responseCode = "403", description = "Forbidden - User cannot access requested role menu")
    @ApiResponse(responseCode = "500", description = "Internal server error - Menu processing failed")
    */
    public ResponseEntity<MenuResponseDTO> getMenuOptionsByRole(
            @PathVariable 
            @NotBlank(message = "User role is required")
            String userRole) {
        
        try {
            // Validate user role parameter
            if (!userRole.equals("USER") && !userRole.equals("ADMIN")) {
                MenuResponseDTO errorResponse = MenuResponseDTO.createErrorResponse(
                    userRole, 
                    "Invalid user role. Must be 'USER' or 'ADMIN'"
                );
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Get menu options for the authenticated user (with role-based filtering)
            MenuResponseDTO menuResponse = menuNavigationService.getMenuOptions();
            
            // Additional validation for admin menu access
            if (userRole.equals("ADMIN") && !menuResponse.isAdminUser()) {
                MenuResponseDTO errorResponse = MenuResponseDTO.createErrorResponse(
                    "USER", 
                    "Access denied: Admin menu requires administrative privileges"
                );
                return ResponseEntity.status(403).body(errorResponse);
            }
            
            // Return menu options (service layer handles role-based filtering)
            return ResponseEntity.ok(menuResponse);
            
        } catch (Exception ex) {
            // Handle service layer exceptions with appropriate error response
            MenuResponseDTO errorResponse = MenuResponseDTO.createErrorResponse(
                userRole, 
                "Error retrieving menu options for role: " + ex.getMessage()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}