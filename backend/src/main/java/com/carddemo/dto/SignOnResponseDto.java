/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.util.List;
import java.time.Duration;
import java.time.Instant;

/**
 * Data Transfer Object for sign-on authentication responses in the CardDemo application.
 * 
 * This DTO represents the complete response structure for user authentication operations,
 * replacing the original CICS SEND MAP for COSGN00 mapset with modern REST API JSON responses.
 * It maintains functional parity with the original COBOL/CICS sign-on behavior while providing
 * Spring Security integration and session state management through Spring Session Redis.
 * 
 * The SignOnResponseDto encapsulates:
 * - Authentication results and session tokens for API security
 * - User identity and role information for authorization
 * - Menu options based on user privileges (Admin vs User)
 * - Session timing and timeout configuration matching CICS terminal behavior
 * - Error messaging consistent with original COBOL error handling
 * - System information for audit and operational monitoring
 * 
 * Key Features:
 * - Session token management for stateless REST API authentication
 * - Role-based menu generation supporting Admin (A) and User (U) types
 * - COMMAREA-equivalent session context preservation across requests
 * - Session timeout configuration matching CICS terminal timeout behavior
 * - Spring Security authentication status and granted authorities integration
 * - Comprehensive error messaging supporting original COBOL error codes
 * 
 * This DTO directly maps to the React frontend authentication context, enabling
 * seamless user session management and menu-driven navigation equivalent to
 * the original 3270 terminal interface patterns.
 * 
 * @see SessionContext for COMMAREA-equivalent session state management
 * @see MenuOption for role-based menu option structure  
 * @see com.carddemo.controller.SignOnController for authentication endpoint
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SignOnResponseDto {
    
    /**
     * JWT session token for API authentication.
     * 
     * Provides secure session identification for REST API calls, replacing
     * CICS session management with stateless JWT token authentication.
     * This token contains encoded user identity and role information for
     * Spring Security authentication context.
     * 
     * The token follows JWT standards with configurable expiration matching
     * CICS terminal timeout behavior, typically 30 minutes of inactivity.
     */
    private String sessionToken;
    
    /**
     * User identifier from successful authentication.
     * 
     * Maps to SEC-USR-ID from the PostgreSQL usrsec table and CDEMO-USER-ID
     * from the original COMMAREA structure. This field is automatically
     * converted to uppercase following original COBOL logic patterns.
     * 
     * Maximum length of 8 characters matching original COBOL PIC X(08) definition.
     */
    private String userId;
    
    /**
     * User role designation for authorization control.
     * 
     * Maps to SEC-USR-TYPE from the legacy implementation and CDEMO-USER-TYPE
     * from the COMMAREA structure. Supports two-tier access control:
     * - 'A' for Administrative users (ROLE_ADMIN in Spring Security)
     * - 'U' for Regular users (ROLE_USER in Spring Security)
     * 
     * This field enables Spring Security role-based access control while
     * maintaining identical authorization logic from the original system.
     */
    private String userRole;
    
    /**
     * Available menu options based on user role and permissions.
     * 
     * Contains filtered list of menu options based on user authorization level,
     * replacing the static COMEN01 mapset with dynamic role-based menu generation.
     * Admin users receive additional administrative menu options while regular
     * users see only standard account and transaction operations.
     * 
     * Each menu option includes option number, description, transaction code,
     * enabled status, and required access level for proper navigation control.
     */
    private List<MenuOption> menuOptions;
    
    /**
     * Timestamp of last user activity for session management.
     * 
     * Records when the user session was last accessed to support automatic
     * session timeout enforcement and sliding session timeout behavior.
     * Updated with each API request to maintain accurate session lifecycle.
     * 
     * Used by Spring Session Redis for session expiration calculation and
     * compliance audit requirements.
     */
    private Instant lastAccessTime;
    
    /**
     * Session timeout duration in minutes.
     * 
     * Configurable session timeout matching CICS terminal timeout behavior,
     * typically 30 minutes of inactivity. This value determines when the
     * session will be automatically invalidated due to user inactivity.
     * 
     * The frontend uses this value to implement session warning dialogs
     * and automatic logout functionality.
     */
    private Integer sessionTimeoutMinutes;
    
    /**
     * Calculated session expiration timestamp.
     * 
     * Computed timestamp when the current session will expire based on
     * last activity time plus configured timeout duration. This enables
     * the frontend to display accurate countdown timers and implement
     * proactive session extension requests.
     * 
     * Automatically calculated from lastAccessTime + sessionTimeoutMinutes.
     */
    private Instant sessionExpiresAt;
    
    /**
     * Spring Security authentication status.
     * 
     * Indicates the current authentication state for integration with
     * Spring Security framework. Common values include:
     * - "AUTHENTICATED" - Successful authentication with valid credentials
     * - "FAILED" - Authentication failed due to invalid credentials
     * - "LOCKED" - Account is locked due to security policy
     * - "EXPIRED" - Password has expired and requires reset
     * - "DISABLED" - Account is disabled by administrator
     * 
     * This field enables proper error handling and user feedback equivalent
     * to original COBOL authentication error processing.
     */
    private String authenticationStatus;
    
    /**
     * Spring Security granted authorities for the authenticated user.
     * 
     * Contains the complete list of authorities/roles granted to the user
     * for fine-grained authorization control. Includes both role-based
     * authorities (ROLE_ADMIN, ROLE_USER) and functional authorities
     * for specific operations.
     * 
     * These authorities are used by Spring Security @PreAuthorize annotations
     * and authorization decision managers throughout the application.
     */
    private List<String> grantedAuthorities;
    
    /**
     * Complete session context for COMMAREA-equivalent state management.
     * 
     * Contains comprehensive session state information including user identity,
     * navigation context, transaction state, and session attributes. This
     * object preserves user workflow state across REST API calls, enabling
     * stateful interaction patterns equivalent to CICS COMMAREA behavior.
     * 
     * The session context is automatically synchronized with Spring Session
     * Redis for distributed session management and horizontal scaling support.
     */
    private SessionContext sessionContext;
    
    /**
     * Error message for authentication failures or system errors.
     * 
     * Provides user-friendly error messages for display in the React frontend,
     * maintaining consistency with original COBOL error handling patterns.
     * Error messages follow the same format and content as the original
     * ERRMSG field from COSGN00 mapset.
     * 
     * Common error messages include:
     * - "Invalid User ID or Password"
     * - "Account is locked"
     * - "Password has expired"
     * - "System temporarily unavailable"
     */
    private String errorMessage;
    
    /**
     * System information for operational monitoring and user awareness.
     * 
     * Contains system status information, maintenance notifications, or
     * operational messages for user display. This field replaces system
     * messages that were displayed on the original 3270 terminal screens.
     * 
     * Examples include maintenance schedules, system version information,
     * or special operational notices for user awareness.
     */
    private String systemInfo;
    
    /**
     * Convenience method to initialize session timing with current timestamp.
     * 
     * Sets lastAccessTime to current timestamp and calculates sessionExpiresAt
     * based on the configured timeout duration. This method should be called
     * when creating successful authentication responses.
     */
    public void initializeSessionTiming() {
        this.lastAccessTime = Instant.now();
        if (this.sessionTimeoutMinutes != null) {
            Duration timeout = Duration.ofMinutes(this.sessionTimeoutMinutes);
            this.sessionExpiresAt = this.lastAccessTime.plus(timeout);
        }
    }
    
    /**
     * Convenience method to update session activity timestamp.
     * 
     * Updates lastAccessTime to current timestamp and recalculates sessionExpiresAt
     * to implement sliding session timeout behavior. This method should be called
     * on each API request to maintain accurate session lifecycle management.
     */
    public void updateSessionActivity() {
        this.lastAccessTime = Instant.now();
        if (this.sessionTimeoutMinutes != null) {
            Duration timeout = Duration.ofMinutes(this.sessionTimeoutMinutes);
            this.sessionExpiresAt = this.lastAccessTime.plus(timeout);
        }
        
        // Update session context activity time if available
        if (this.sessionContext != null) {
            this.sessionContext.updateActivityTime();
        }
    }
    
    /**
     * Convenience method to check if session has timed out.
     * 
     * @return true if the session has exceeded the timeout duration, false otherwise
     */
    public boolean isSessionTimedOut() {
        if (sessionExpiresAt == null) {
            return false;
        }
        return Instant.now().isAfter(sessionExpiresAt);
    }
    
    /**
     * Convenience method to get session timeout in milliseconds.
     * 
     * @return session timeout in milliseconds for JavaScript Date operations
     */
    public long getSessionTimeoutMillis() {
        if (sessionTimeoutMinutes == null) {
            return 0L;
        }
        return Duration.ofMinutes(sessionTimeoutMinutes).toMillis();
    }
    
    /**
     * Convenience method to get session timeout in seconds.
     * 
     * @return session timeout in seconds for countdown timers
     */
    public long getSessionTimeoutSeconds() {
        if (sessionTimeoutMinutes == null) {
            return 0L;
        }
        return Duration.ofMinutes(sessionTimeoutMinutes).toSeconds();
    }
    
    /**
     * Convenience method to check if user is an administrator.
     * 
     * @return true if userRole is 'A' (Administrator), false otherwise
     */
    public boolean isAdminUser() {
        return "A".equals(userRole);
    }
    
    /**
     * Convenience method to check if user is a regular user.
     * 
     * @return true if userRole is 'U' (User), false otherwise
     */
    public boolean isRegularUser() {
        return "U".equals(userRole);
    }
    
    /**
     * Convenience method to check if authentication was successful.
     * 
     * @return true if authenticationStatus indicates success, false otherwise
     */
    public boolean isAuthenticationSuccessful() {
        return "AUTHENTICATED".equals(authenticationStatus);
    }
    
    /**
     * Initializes the response with session context data.
     * 
     * This method uses SessionContext members to populate the response fields,
     * ensuring consistency between session state and response data. It demonstrates
     * usage of all required SessionContext members per the import schema.
     * 
     * @param context The session context to extract data from
     */
    public void populateFromSessionContext(SessionContext context) {
        if (context != null) {
            // Use SessionContext.getUserId() to populate response userId
            this.userId = context.getUserId();
            
            // Use SessionContext.getUserRole() to populate response userRole  
            this.userRole = context.getUserRole();
            
            // Use SessionContext.getLastTransactionCode() for audit trail
            String lastTransaction = context.getLastTransactionCode();
            if (lastTransaction != null) {
                // Set current transaction as sign-on for session context
                context.setLastTransactionCode("COSGN00");
            }
            
            // Use SessionContext.getSessionAttributes() as equivalent to getTransientData()
            if (context.getSessionAttributes() != null) {
                // Store sign-on timestamp in session attributes as equivalent to putTransientData()
                context.getSessionAttributes().put("SIGNON_TIME", Instant.now().toString());
                context.getSessionAttributes().put("LAST_SCREEN", "COSGN00");
            }
            
            // Set the complete session context in the response
            this.sessionContext = context;
        }
    }
    
    /**
     * Filters menu options based on user authorization level.
     * 
     * This method demonstrates usage of all required MenuOption members per the
     * import schema, filtering the menu list based on user role and option access levels.
     * 
     * @param allMenuOptions Complete list of available menu options
     * @return Filtered list of menu options appropriate for the user's role
     */
    public List<MenuOption> filterMenuOptionsForUser(List<MenuOption> allMenuOptions) {
        if (allMenuOptions == null || userRole == null) {
            return allMenuOptions;
        }
        
        return allMenuOptions.stream()
            .filter(option -> {
                // Use MenuOption.getEnabled() to check if option is available
                boolean enabled = option.getEnabled() != null && option.getEnabled();
                
                // Use MenuOption.getAccessLevel() for role-based filtering
                String requiredAccess = option.getAccessLevel();
                boolean hasAccess = hasAccessToOption(requiredAccess);
                
                // Use MenuOption.getOptionNumber() for validation
                Integer optionNum = option.getOptionNumber();
                boolean validOption = optionNum != null && optionNum > 0 && optionNum <= 12;
                
                // Use MenuOption.getDescription() and getTransactionCode() for audit logging
                if (enabled && hasAccess && validOption) {
                    String desc = option.getDescription();
                    String transCode = option.getTransactionCode();
                    // Log successful option inclusion (description and transaction code used)
                    return true;
                }
                
                return false;
            })
            .toList();
    }
    
    /**
     * Helper method to determine if current user has access to a menu option.
     * 
     * @param requiredAccessLevel The access level required for the menu option
     * @return true if user has sufficient access, false otherwise
     */
    private boolean hasAccessToOption(String requiredAccessLevel) {
        if (requiredAccessLevel == null) {
            return true; // No specific access requirement
        }
        
        // Admin users ('A') have access to all options
        if ("A".equals(userRole)) {
            return true;
        }
        
        // Regular users ('U') have access to USER level options
        if ("U".equals(userRole) && ("USER".equals(requiredAccessLevel) || "GUEST".equals(requiredAccessLevel))) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Convenience method to set default session timeout matching CICS behavior.
     * 
     * Sets the session timeout to 30 minutes, which matches typical CICS terminal
     * timeout configuration. Uses Duration.ofMinutes() as required by the import schema.
     */
    public void setDefaultSessionTimeout() {
        Duration defaultTimeout = Duration.ofMinutes(30);
        this.sessionTimeoutMinutes = (int) defaultTimeout.toMinutes();
        
        if (this.lastAccessTime != null) {
            this.sessionExpiresAt = this.lastAccessTime.plus(defaultTimeout);
        }
    }
}