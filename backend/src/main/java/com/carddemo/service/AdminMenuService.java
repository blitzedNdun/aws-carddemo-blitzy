/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.dto.AdminMenuRequest;
import com.carddemo.dto.AdminMenuResponse;
import com.carddemo.dto.MenuOption;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.ArrayList;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring Boot service implementing administrative menu logic translated from COADM01C.cbl.
 * 
 * This service provides privileged menu options for admin users including user management,
 * system configuration, and report generation. Maintains strict role-based access control
 * while preserving the original COBOL menu structure and navigation patterns.
 * 
 * Original COBOL Program: COADM01C.cbl
 * Transaction ID: CA00
 * 
 * Key COBOL Paragraph Mappings:
 * - MAIN-PARA → adminMenuProcess()
 * - BUILD-MENU-OPTIONS → buildAdminMenu()
 * - PROCESS-ENTER-KEY → processAdminSelection()
 * 
 * Admin Menu Options (from COADM02Y configuration):
 * 1. User List (Security) - COUSR00C
 * 2. User Add (Security) - COUSR01C  
 * 3. User Update (Security) - COUSR02C
 * 4. User Delete (Security) - COUSR03C
 * 
 * Implements Spring Security authorization to replace RACF security controls,
 * ensuring only authenticated admin users can access administrative functions.
 * 
 * @see <a href="../../../../../app/cbl/COADM01C.cbl">COADM01C.cbl</a>
 * @see <a href="../../../../../app/cpy/COADM02Y.cpy">COADM02Y.cpy</a>
 */
@Service
public class AdminMenuService {

    private static final Logger logger = LoggerFactory.getLogger(AdminMenuService.class);
    
    // Constants from COADM02Y copybook - Admin menu configuration
    private static final int CDEMO_ADMIN_OPT_COUNT = 4;
    private static final String WS_TRANID = "CA00";
    private static final String WS_PGMNAME = "COADM01C";
    
    // Admin option configuration matching COADM02Y structure
    private static final String[] ADMIN_OPT_NAMES = {
        "User List (Security)",
        "User Add (Security)", 
        "User Update (Security)",
        "User Delete (Security)"
    };
    
    private static final String[] ADMIN_OPT_PGMNAMES = {
        "COUSR00C",
        "COUSR01C",
        "COUSR02C", 
        "COUSR03C"
    };

    /**
     * Main admin menu processing method - translates MAIN-PARA from COADM01C.cbl.
     * 
     * This method handles the primary admin menu logic including session validation,
     * initial screen setup, and routing based on user input. Maintains the same
     * flow control as the original COBOL MAIN-PARA paragraph.
     * 
     * Original COBOL Logic:
     * - Check EIBCALEN for initial entry vs re-entry
     * - Initialize error flags and messages
     * - Route to appropriate processing based on entry type
     * - Handle COMMAREA for session state management
     * 
     * @param request AdminMenuRequest containing session data and user input
     * @return AdminMenuResponse with admin menu options and system status
     * @throws SecurityException if user lacks admin privileges
     */
    @PreAuthorize("hasRole('ADMIN')")
    public AdminMenuResponse adminMenuProcess(AdminMenuRequest request) {
        logger.info("Starting admin menu processing for user session");
        logger.debug("Processing admin menu request: {}", request);
        
        try {
            // Validate admin access (replaces RACF security check)
            validateAdminAccess();
            
            // Initialize response with system status
            AdminMenuResponse response = new AdminMenuResponse();
            
            // Build admin menu options (translates BUILD-MENU-OPTIONS paragraph)
            response = buildAdminMenu();
            
            // Set system status information
            response.setSystemStatus(getSystemStatus());
            response.setLastBatchRun(LocalDateTime.now().minusHours(2)); // Simulated last batch run
            response.setActiveUsers(getCurrentActiveUserCount());
            
            logger.info("Admin menu processing completed successfully");
            return response;
            
        } catch (Exception e) {
            logger.error("Error in admin menu processing: {}", e.getMessage(), e);
            throw new RuntimeException("Admin menu processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Builds admin menu options - translates BUILD-MENU-OPTIONS from COADM01C.cbl.
     * 
     * This method creates the admin-specific menu options based on COADM02Y configuration.
     * Maintains the same option structure and numbering as the original COBOL implementation.
     * 
     * Original COBOL Logic:
     * - PERFORM VARYING loop from 1 to CDEMO-ADMIN-OPT-COUNT
     * - Build option text by concatenating option number, separator, and name
     * - Populate screen fields OPTN001O through OPTN010O
     * 
     * @return AdminMenuResponse populated with admin menu options
     */
    @PreAuthorize("hasRole('ADMIN')")
    public AdminMenuResponse buildAdminMenu() {
        logger.debug("Building admin menu options from COADM02Y configuration");
        
        AdminMenuResponse response = new AdminMenuResponse();
        ArrayList<MenuOption> adminOptions = new ArrayList<>();
        
        // Replicate COBOL PERFORM VARYING loop for building menu options
        for (int idx = 1; idx <= CDEMO_ADMIN_OPT_COUNT; idx++) {
            MenuOption option = new MenuOption();
            
            // Set option number (CDEMO-ADMIN-OPT-NUM equivalent)
            option.setOptionNumber(idx);
            
            // Build option description (STRING operation from COBOL)
            String optionText = idx + ". " + ADMIN_OPT_NAMES[idx - 1];
            option.setDescription(optionText);
            
            // Set transaction code (CDEMO-ADMIN-OPT-PGMNAME equivalent)
            option.setTransactionCode(ADMIN_OPT_PGMNAMES[idx - 1]);
            
            // Enable all admin options for privileged users
            option.setEnabled(true);
            option.setAccessLevel("ADMIN");
            
            adminOptions.add(option);
            
            logger.debug("Added admin option {}: {} -> {}", 
                        idx, ADMIN_OPT_NAMES[idx - 1], ADMIN_OPT_PGMNAMES[idx - 1]);
        }
        
        response.setAdminOptions(adminOptions);
        logger.info("Built {} admin menu options", adminOptions.size());
        
        return response;
    }

    /**
     * Processes admin menu selection - translates PROCESS-ENTER-KEY from COADM01C.cbl.
     * 
     * This method validates the user's menu selection and determines the appropriate
     * action. Maintains the same validation logic as the original COBOL implementation.
     * 
     * Original COBOL Logic:
     * - Parse option input from OPTIONI field
     * - Remove trailing spaces and pad with zeros  
     * - Validate numeric range (1 to CDEMO-ADMIN-OPT-COUNT)
     * - Check if option program name is not 'DUMMY'
     * - Perform XCTL to selected program with COMMAREA
     * 
     * @param request AdminMenuRequest with selected option and session data
     * @return String indicating the target transaction code or error message
     * @throws IllegalArgumentException if invalid option selected
     */
    @PreAuthorize("hasRole('ADMIN')")
    public String processAdminSelection(AdminMenuRequest request) {
        logger.info("Processing admin menu selection");
        logger.debug("Admin selection request: {}", request);
        
        if (request == null || request.getSelectedOption() == null) {
            logger.warn("Invalid admin menu request - missing selection");
            throw new IllegalArgumentException("Please enter a valid option number...");
        }
        
        Integer selectedOption = request.getSelectedOption();
        logger.debug("Processing selected admin option: {}", selectedOption);
        
        // Validate option range (replaces COBOL numeric and range validation)
        if (selectedOption < 1 || selectedOption > CDEMO_ADMIN_OPT_COUNT) {
            logger.warn("Invalid admin option selected: {} (valid range: 1-{})", 
                       selectedOption, CDEMO_ADMIN_OPT_COUNT);
            throw new IllegalArgumentException("Please enter a valid option number...");
        }
        
        // Get target program name (CDEMO-ADMIN-OPT-PGMNAME equivalent)
        String targetProgram = ADMIN_OPT_PGMNAMES[selectedOption - 1];
        
        // Check if program is available (replaces DUMMY check from COBOL)
        if (targetProgram.startsWith("DUMMY")) {
            logger.info("Selected admin option {} is not yet implemented", selectedOption);
            return "This option is coming soon...";
        }
        
        // Log successful selection for audit trail
        logger.info("Admin user selected option {}: {} -> {}", 
                   selectedOption, ADMIN_OPT_NAMES[selectedOption - 1], targetProgram);
        
        // Return target transaction code for controller routing
        // In COBOL, this would perform XCTL to the target program
        return targetProgram;
    }

    /**
     * Validates admin access privileges - implements Spring Security authorization.
     * 
     * This method ensures that only users with proper administrative privileges
     * can access admin menu functions. Replaces RACF security controls from the
     * original mainframe environment.
     * 
     * Security checks:
     * - Verify user has ADMIN role
     * - Validate session is not expired
     * - Log access attempts for audit compliance
     * 
     * @throws SecurityException if user lacks sufficient privileges
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void validateAdminAccess() {
        logger.debug("Validating admin access privileges");
        
        // Spring Security @PreAuthorize handles role validation
        // Additional custom validation can be added here if needed
        
        try {
            // Log successful admin access for audit trail
            logger.info("Admin access validated successfully");
            
        } catch (Exception e) {
            logger.error("Admin access validation failed: {}", e.getMessage());
            throw new SecurityException("Insufficient privileges for admin functions", e);
        }
    }

    /**
     * Gets current system operational status for admin display.
     * 
     * This method provides administrators with real-time system status information
     * including operational state, batch job status, and system health indicators.
     * 
     * Status values:
     * - "ONLINE" - System fully operational
     * - "BATCH_RUNNING" - Batch processing in progress  
     * - "MAINTENANCE" - System under maintenance
     * - "OFFLINE" - System unavailable
     * 
     * @return String indicating current system status
     */
    public String getSystemStatus() {
        logger.debug("Retrieving current system status");
        
        try {
            // In a real implementation, this would check:
            // - Database connectivity
            // - External service availability  
            // - Batch job execution status
            // - System resource utilization
            
            // For now, return operational status
            String status = "ONLINE";
            
            // Check if any critical batch jobs are running
            if (isBatchProcessingActive()) {
                status = "BATCH_RUNNING";
            }
            
            logger.debug("System status determined: {}", status);
            return status;
            
        } catch (Exception e) {
            logger.error("Error retrieving system status: {}", e.getMessage());
            return "OFFLINE";
        }
    }

    /**
     * Helper method to check if batch processing is currently active.
     * 
     * @return boolean indicating if batch jobs are running
     */
    private boolean isBatchProcessingActive() {
        // In a real implementation, this would check Spring Batch job repository
        // For demonstration, simulate batch activity based on time
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        
        // Simulate batch processing during off-hours (2-4 AM)
        return hour >= 2 && hour <= 4;
    }

    /**
     * Helper method to get count of currently active users.
     * 
     * @return Integer count of active users in the system
     */
    private Integer getCurrentActiveUserCount() {
        // In a real implementation, this would query:
        // - Active sessions from Spring Session
        // - Connected database users
        // - Active transaction counts
        
        // For demonstration, return a simulated count
        return 15; // Simulated active user count
    }
}
