/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carddemo.menu;

import com.carddemo.common.dto.MenuOptionDTO;
import com.carddemo.common.dto.MenuResponseDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST controller providing HTTP endpoints for menu navigation functionality in the CardDemo application.
 * 
 * This controller exposes role-based menu generation API endpoints that integrate with Spring Security
 * for JWT token validation and delegate business logic to MenuNavigationService. The implementation
 * transforms the original COBOL COMEN01C.cbl menu processing program into modern REST API endpoints
 * while preserving the exact menu navigation logic and user experience.
 * 
 * Original COBOL Program Transformation (COMEN01C.cbl):
 * - MAIN-PARA: Main processing logic → REST endpoint request handling with Spring Security context
 * - PROCESS-ENTER-KEY: Menu selection validation → validateMenuSelection() API endpoint
 * - BUILD-MENU-OPTIONS: Dynamic menu generation → role-based menu filtering via service layer
 * - SEND-MENU-SCREEN: Screen display logic → JSON response construction for React frontend
 * 
 * The controller implements the following REST endpoints as specified in Summary of Changes:
 * - GET /api/menu/options: Retrieve menu options for currently authenticated user with role filtering
 * - GET /api/menu/{userRole}: Get menu options for specific user role (admin access required)
 * - GET /api/menu/validate/{optionNumber}: Validate menu selection and check user access permissions
 * 
 * Spring Security Integration:
 * - JWT token validation through @PreAuthorize annotations for all endpoints
 * - Role-based authorization ensures admin-only access to administrative menu functions
 * - Security context integration enables seamless user role extraction for menu filtering
 * 
 * API Documentation:
 * - OpenAPI 3.0 annotations provide comprehensive REST API documentation
 * - Endpoint descriptions reference original CICS transaction codes for traceability
 * - Response schemas document JSON DTOs equivalent to original COMMAREA structures
 * 
 * Error Handling:
 * - HTTP status codes map to original error conditions from COBOL implementation
 * - Business errors return appropriate status codes with user-friendly messages
 * - Security exceptions handled through Spring Security framework integration
 * 
 * Performance Requirements:
 * - Sub-200ms response times maintained through efficient service layer delegation
 * - Stateless endpoint design supports horizontal scaling via Kubernetes orchestration
 * - Redis-backed session management preserves user context across API calls
 * 
 * @author CardDemo Application - Blitzy agent
 * @version 1.0 - Converted from COBOL COMEN01C.cbl
 * @since Java 21, Spring Boot 3.2.x
 */
@RestController
@RequestMapping("/api/menu")
@Tag(name = "Menu Navigation", description = "REST API endpoints for role-based menu navigation functionality converted from COBOL COMEN01C.cbl program")
public class MenuController {

    private static final Logger logger = LoggerFactory.getLogger(MenuController.class);

    /**
     * MenuNavigationService instance for business logic delegation.
     * Injected by Spring's dependency injection framework to provide
     * role-based menu generation and validation functionality.
     */
    @Autowired
    private MenuNavigationService menuNavigationService;

    // Controller constants from original COBOL program
    private static final String PROGRAM_NAME = "COMEN01C";
    private static final String TRANSACTION_ID = "CM00";

    /**
     * Retrieves menu options for the currently authenticated user with role-based filtering.
     * 
     * This endpoint implements the core functionality of the original COBOL COMEN01C.cbl program's
     * MAIN-PARA and BUILD-MENU-OPTIONS paragraphs, providing dynamic menu generation based on
     * the user's role and access permissions extracted from the Spring Security context.
     * 
     * Original COBOL Logic Transformation:
     * - EIBCALEN validation for initial menu display → Spring Security authentication validation
     * - CDEMO-USRTYP-USER role validation → JWT token role extraction and filtering logic
     * - BUILD-MENU-OPTIONS paragraph execution → MenuNavigationService.getMenuOptions() delegation
     * - SEND-MENU-SCREEN paragraph → JSON response construction with MenuResponseDTO
     * 
     * Spring Security Integration:
     * - @PreAuthorize annotation ensures only authenticated users can access menu options
     * - JWT token validation automatically extracts user role from security context
     * - Role-based filtering applied through service layer business logic
     * 
     * API Response Format:
     * - JSON response contains filtered menu options array matching user access permissions
     * - Response includes user role context for frontend menu state management
     * - Success/error status indicators enable proper React component error handling
     * - Message field provides user feedback equivalent to original ERRMSGO display field
     * 
     * @return ResponseEntity<MenuResponseDTO> containing filtered menu options, user role, and status information
     */
    @GetMapping("/options")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(
        summary = "Get menu options for authenticated user",
        description = "Retrieves role-based filtered menu options for the currently authenticated user. " +
                     "Implements functionality equivalent to COBOL COMEN01C.cbl MAIN-PARA and BUILD-MENU-OPTIONS " +
                     "paragraphs with Spring Security JWT token validation and role-based access control.",
        tags = {"Menu Navigation"}
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Successfully retrieved menu options with role-based filtering applied"
    )
    @ApiResponse(
        responseCode = "401", 
        description = "Authentication required - no valid JWT token provided"
    )
    @ApiResponse(
        responseCode = "403", 
        description = "Access denied - insufficient privileges for menu access"
    )
    @ApiResponse(
        responseCode = "500", 
        description = "Internal server error during menu option retrieval"
    )
    public ResponseEntity<MenuResponseDTO> getMenuOptions() {
        logger.info("Processing menu options request for authenticated user - controller: {}, transaction: {}", 
                   getClass().getSimpleName(), TRANSACTION_ID);

        try {
            // Delegate business logic to service layer
            // Equivalent to COBOL PERFORM BUILD-MENU-OPTIONS paragraph
            MenuResponseDTO response = menuNavigationService.getMenuOptions();

            if (response.isSuccess()) {
                logger.info("Successfully retrieved {} menu options for user role: {}", 
                           response.getMenuOptionCount(), response.getUserRole());
                return ResponseEntity.ok(response);
            } else {
                // Handle business logic errors from service layer
                logger.warn("Menu options retrieval failed: {}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("Unexpected error retrieving menu options: {}", e.getMessage(), e);
            
            // Return standardized error response equivalent to COBOL error handling
            MenuResponseDTO errorResponse = MenuResponseDTO.error(
                "Unable to retrieve menu options. Please try again.");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Retrieves menu options for a specific user role with administrative access control.
     * 
     * This endpoint provides administrative functionality for retrieving menu options
     * filtered for a specific user role, enabling admin users to view role-based menu
     * configurations without impersonating other users. The functionality preserves
     * the administrative oversight capabilities present in the original mainframe system.
     * 
     * Administrative Access Control:
     * - @PreAuthorize("hasRole('ADMIN')") ensures only administrators can access this endpoint
     * - Role-based filtering applied through service layer with specified user type
     * - Audit logging captures administrative menu access for security compliance
     * 
     * Path Parameter Validation:
     * - {userRole} parameter accepts 'U' for regular users or 'A' for administrators
     * - Input validation ensures only valid role codes are processed
     * - Invalid role codes return appropriate error responses with user feedback
     * 
     * Original COBOL Equivalent:
     * This endpoint provides functionality similar to administrative menu viewing
     * capabilities that would have been available through CICS security administration
     * functions, enabling system administrators to verify role-based menu configurations.
     * 
     * @param userRole the user role code ('U' for User, 'A' for Admin) to filter menu options
     * @return ResponseEntity<MenuResponseDTO> containing role-specific menu options and status
     */
    @GetMapping("/{userRole}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get menu options for specific user role (Admin only)",
        description = "Administrative endpoint for retrieving menu options filtered for a specific user role. " +
                     "Enables administrators to view role-based menu configurations without user impersonation. " +
                     "Access restricted to users with administrative privileges only.",
        tags = {"Menu Navigation", "Administration"}
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Successfully retrieved menu options for specified role"
    )
    @ApiResponse(
        responseCode = "400", 
        description = "Invalid user role parameter - must be 'U' for User or 'A' for Admin"
    )
    @ApiResponse(
        responseCode = "401", 
        description = "Authentication required - no valid JWT token provided"
    )
    @ApiResponse(
        responseCode = "403", 
        description = "Access denied - administrative privileges required"
    )
    @ApiResponse(
        responseCode = "500", 
        description = "Internal server error during menu option retrieval"
    )
    public ResponseEntity<MenuResponseDTO> getMenuOptionsByRole(
            @Parameter(
                description = "User role code for menu filtering ('U' for regular users, 'A' for administrators)",
                required = true,
                example = "U"
            )
            @PathVariable 
            @NotBlank(message = "User role parameter cannot be blank")
            String userRole) {
        
        logger.info("Processing administrative menu request for role: {} - controller: {}, transaction: {}", 
                   userRole, getClass().getSimpleName(), TRANSACTION_ID);

        try {
            // Validate user role parameter format
            if (!isValidUserRole(userRole)) {
                logger.warn("Invalid user role parameter received: {}", userRole);
                MenuResponseDTO errorResponse = MenuResponseDTO.error(
                    "Invalid user role. Must be 'U' for User or 'A' for Admin.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Convert role string to appropriate enum value for service layer
            com.carddemo.common.enums.UserType userType = convertRoleStringToUserType(userRole);
            
            if (userType == null) {
                logger.warn("Unable to convert user role string to enum: {}", userRole);
                MenuResponseDTO errorResponse = MenuResponseDTO.error(
                    "Invalid user role specification.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Delegate to service layer for role-specific menu generation
            MenuResponseDTO response = menuNavigationService.getMenuOptionsForRole(userType);

            if (response.isSuccess()) {
                logger.info("Successfully retrieved {} menu options for administrative role query: {}", 
                           response.getMenuOptionCount(), userRole);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Administrative menu retrieval failed for role {}: {}", userRole, response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("Unexpected error retrieving menu options for role {}: {}", userRole, e.getMessage(), e);
            
            MenuResponseDTO errorResponse = MenuResponseDTO.error(
                String.format("Unable to retrieve menu options for role %s. Please try again.", userRole));
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Validates a menu selection and determines if the current user has access to the selected option.
     * 
     * This endpoint implements the PROCESS-ENTER-KEY paragraph from the original COBOL COMEN01C.cbl program,
     * providing comprehensive validation logic for menu option selection processing with identical
     * business rules and error handling patterns.
     * 
     * Original COBOL Validation Logic Transformation (PROCESS-ENTER-KEY paragraph):
     * 1. Option number numeric validation and range checking → Path parameter validation with constraints
     * 2. Role-based access validation (CDEMO-USRTYP-USER vs CDEMO-MENU-OPT-USRTYPE) → Service layer authorization
     * 3. Admin-only option access control for regular users → Spring Security integration with service validation
     * 4. Error message generation for invalid selections → Standardized error response DTOs
     * 
     * Validation Process:
     * - Path parameter validation ensures option number is within valid range (1-14)
     * - Service layer validates user access permissions against selected menu option
     * - Authentication context provides user role for access control evaluation
     * - Comprehensive error handling with user-friendly messages equivalent to original
     * 
     * Response Scenarios:
     * - Success: User has access to selected option with confirmation message
     * - Access Denied: User lacks privileges for admin-only options (equivalent to "No access - Admin Only option")
     * - Invalid Option: Option number outside valid range or not found in available options
     * - Authentication Error: No valid JWT token or session context available
     * 
     * @param optionNumber the menu option number to validate (1-14 based on original COBOL range)
     * @return ResponseEntity<MenuResponseDTO> with validation result and appropriate status/message
     */
    @GetMapping("/validate/{optionNumber}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(
        summary = "Validate menu selection and check user access",
        description = "Validates a menu option selection and verifies user access permissions. " +
                     "Implements PROCESS-ENTER-KEY paragraph logic from COBOL COMEN01C.cbl with " +
                     "comprehensive option validation, range checking, and role-based authorization control.",
        tags = {"Menu Navigation", "Validation"}
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Menu option validation completed - check response status for access result"
    )
    @ApiResponse(
        responseCode = "400", 
        description = "Invalid option number - must be between 1 and 14"
    )
    @ApiResponse(
        responseCode = "401", 
        description = "Authentication required - no valid JWT token provided"
    )
    @ApiResponse(
        responseCode = "403", 
        description = "Access denied - insufficient privileges for menu validation"
    )
    @ApiResponse(
        responseCode = "500", 
        description = "Internal server error during menu option validation"
    )
    public ResponseEntity<MenuResponseDTO> validateMenuSelection(
            @Parameter(
                description = "Menu option number to validate (1-14 based on available menu options)",
                required = true,
                example = "1"
            )
            @PathVariable 
            @Min(value = 1, message = "Option number must be at least 1")
            @Max(value = 14, message = "Option number must not exceed 14")
            int optionNumber) {

        logger.info("Processing menu validation request for option: {} - controller: {}, transaction: {}", 
                   optionNumber, getClass().getSimpleName(), TRANSACTION_ID);

        try {
            // Delegate validation logic to service layer
            // Equivalent to COBOL PERFORM PROCESS-ENTER-KEY paragraph
            MenuResponseDTO response = menuNavigationService.validateMenuSelection(optionNumber);

            // Return validation result with appropriate HTTP status
            if (response.isSuccess()) {
                logger.info("Menu option {} validation successful for user role: {}", 
                           optionNumber, response.getUserRole());
                return ResponseEntity.ok(response);
            } else {
                // Business validation failed - user lacks access or invalid option
                logger.warn("Menu option {} validation failed: {}", optionNumber, response.getMessage());
                return ResponseEntity.ok(response); // Return 200 with business error in response body
            }

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid menu option number provided: {} - {}", optionNumber, e.getMessage());
            MenuResponseDTO errorResponse = MenuResponseDTO.error(
                "Please enter a valid option number...");
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            logger.error("Unexpected error validating menu option {}: {}", optionNumber, e.getMessage(), e);
            
            MenuResponseDTO errorResponse = MenuResponseDTO.error(
                String.format("Unable to validate menu option %d. Please try again.", optionNumber));
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Additional endpoint for retrieving all available menu options with metadata.
     * 
     * This convenience endpoint provides comprehensive menu information including
     * option details, access levels, and target services for administrative
     * purposes and frontend menu construction.
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get all available menu options (Admin only)",
        description = "Administrative endpoint for retrieving complete menu option catalog " +
                     "including option numbers, display text, target services, and access levels. " +
                     "Used for administrative menu management and frontend menu construction.",
        tags = {"Menu Navigation", "Administration"}
    )
    public ResponseEntity<List<MenuOptionDTO>> getAllMenuOptions() {
        logger.info("Processing request for all menu options - controller: {}", getClass().getSimpleName());

        try {
            // Get comprehensive menu response and extract options
            MenuResponseDTO response = menuNavigationService.getMenuOptions();
            
            if (response.isSuccess() && response.hasMenuOptions()) {
                logger.info("Successfully retrieved {} total menu options", response.getMenuOptionCount());
                return ResponseEntity.ok(response.getMenuOptions());
            } else {
                logger.warn("Failed to retrieve menu options: {}", response.getMessage());
                return ResponseEntity.badRequest().build();
            }

        } catch (Exception e) {
            logger.error("Error retrieving all menu options: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Validates if the provided user role string is a valid role code.
     * 
     * @param userRole the user role string to validate
     * @return true if valid ('U' or 'A'), false otherwise
     */
    private boolean isValidUserRole(String userRole) {
        return userRole != null && 
               (userRole.equalsIgnoreCase("U") || userRole.equalsIgnoreCase("A"));
    }

    /**
     * Converts a user role string to the appropriate UserType enum value.
     * 
     * @param userRole the user role string ('U' or 'A')
     * @return corresponding UserType enum value, or null if invalid
     */
    private com.carddemo.common.enums.UserType convertRoleStringToUserType(String userRole) {
        if (userRole == null) {
            return null;
        }
        
        try {
            return com.carddemo.common.enums.UserType.fromCode(userRole.toUpperCase())
                .orElse(null);
        } catch (Exception e) {
            logger.warn("Error converting role string '{}' to UserType: {}", userRole, e.getMessage());
            return null;
        }
    }
}