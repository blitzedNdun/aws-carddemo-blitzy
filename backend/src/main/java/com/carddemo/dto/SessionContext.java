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

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Session context DTO maintaining user state across REST API calls, equivalent to CICS COMMAREA.
 * 
 * This class provides comprehensive session state management for the CardDemo application,
 * replacing the original CICS COMMAREA structure with modern Spring Session capabilities
 * while preserving identical session lifecycle and state management patterns.
 * 
 * The SessionContext maintains user identity, authorization level, navigation state, and
 * transaction context through Spring Session Redis with JSON serialization, enabling
 * stateful interaction patterns across REST API calls while supporting horizontal scaling
 * through distributed session clustering.
 * 
 * Key Features:
 * - User identity preservation through SEC-USR-ID and SEC-USR-TYPE equivalents
 * - Navigation state management for maintaining user workflow context
 * - Transaction state tracking for operation status and error handling
 * - Spring Security integration for authentication and authorization context
 * - 32KB storage limit compliance matching original CICS COMMAREA constraints
 * 
 * @see ApiRequest Generic API request wrapper containing SessionContext
 * @see com.carddemo.security.SessionAttributes Spring Session attribute management
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionContext {
    
    /**
     * User identifier equivalent to SEC-USR-ID from the original COSGN00C implementation.
     * 
     * Maintains the primary user identification value used for authentication and
     * authorization decisions throughout the application. This field directly maps
     * to the SEC-USR-ID field from the PostgreSQL usrsec table, ensuring consistent
     * user identification across all REST API operations.
     * 
     * The user ID is automatically converted to uppercase following the original
     * COBOL logic patterns and used for Spring Security authentication context.
     */
    @JsonProperty("userId")
    @NotNull(message = "User ID is required for session context")
    @Size(max = 8, message = "User ID must not exceed 8 characters")
    private String userId;
    
    /**
     * User role designation equivalent to SEC-USR-TYPE from the legacy implementation.
     * 
     * Defines the authorization level for the authenticated user, supporting the
     * two-tier access control model:
     * - 'A' for Administrative users (ROLE_ADMIN in Spring Security)
     * - 'U' for Regular users (ROLE_USER in Spring Security)
     * 
     * This field enables Spring Security role-based access control through
     * @PreAuthorize annotations and authorization decision managers while
     * maintaining identical authorization logic from the original CICS implementation.
     */
    @JsonProperty("userRole")
    @NotNull(message = "User role is required for authorization")
    @Size(min = 1, max = 1, message = "User role must be exactly 1 character")
    private String userRole;
    
    /**
     * Current menu context preserving navigation state across REST API calls.
     * 
     * Maintains the active menu or program context to support proper navigation
     * flow and user interface state management. This field enables the React
     * frontend to maintain consistent navigation patterns equivalent to the
     * original 3270 terminal screen flow.
     * 
     * Values correspond to original CICS transaction codes and menu identifiers
     * such as "MAIN", "ADMIN", "ACCOUNT", "TRANSACTION", etc.
     */
    @JsonProperty("currentMenu")
    @Size(max = 20, message = "Current menu identifier must not exceed 20 characters")
    private String currentMenu;
    
    /**
     * Last transaction code executed, supporting audit trail and navigation history.
     * 
     * Tracks the most recent business operation for audit logging and user workflow
     * management. This field enables proper error handling context and supports
     * navigation back to previous operations when required.
     * 
     * Transaction codes follow the original CICS pattern (e.g., "COSGN00", "COTRN01").
     */
    @JsonProperty("lastTransactionCode")
    @Size(max = 8, message = "Transaction code must not exceed 8 characters")
    private String lastTransactionCode;
    
    /**
     * Session creation timestamp for timeout management and audit purposes.
     * 
     * Records when the user session was established to support automatic session
     * timeout enforcement and compliance audit requirements. This timestamp enables
     * Spring Session Redis to manage session lifecycle according to configured
     * timeout policies.
     */
    @JsonProperty("sessionStartTime")
    private LocalDateTime sessionStartTime;
    
    /**
     * Last activity timestamp for session timeout calculation.
     * 
     * Updated with each API request to support sliding session timeout behavior
     * equivalent to CICS session management. This field enables automatic session
     * extension based on user activity while maintaining security timeout policies.
     */
    @JsonProperty("lastActivityTime")
    private LocalDateTime lastActivityTime;
    
    /**
     * Current operation status for transaction state management.
     * 
     * Maintains the status of ongoing business operations to support proper error
     * handling and transaction recovery. Values include:
     * - "ACTIVE" - Normal operation in progress
     * - "ERROR" - Error state requiring user attention
     * - "COMPLETE" - Operation completed successfully
     * - "TIMEOUT" - Session timeout condition
     */
    @JsonProperty("operationStatus")
    @Size(max = 10, message = "Operation status must not exceed 10 characters")
    private String operationStatus;
    
    /**
     * Error message context for user feedback and error handling.
     * 
     * Preserves error messages across REST API calls to support proper error
     * display and user guidance. This field enables consistent error handling
     * patterns equivalent to the original COBOL program error processing.
     */
    @JsonProperty("errorMessage")
    @Size(max = 100, message = "Error message must not exceed 100 characters")
    private String errorMessage;
    
    /**
     * Additional session attributes for extensible state management.
     * 
     * Provides flexible storage for operation-specific state data that doesn't
     * fit into the standard session context fields. This map enables preservation
     * of complex business state across REST API calls while maintaining the
     * 32KB total session size limit.
     * 
     * Common usage includes temporary data for multi-step operations, user
     * preferences, and workflow state preservation.
     */
    @JsonProperty("sessionAttributes")
    private Map<String, Object> sessionAttributes;
    
    /**
     * Provides a string representation of the session context for logging and debugging.
     * 
     * Includes essential session information while protecting sensitive data from
     * exposure in log files. User role and session timing information are included
     * for debugging purposes while excluding detailed session attributes.
     * 
     * @return A formatted string representation suitable for logging
     */
    @Override
    public String toString() {
        return "SessionContext{" +
                "userId='" + userId + '\'' +
                ", userRole='" + userRole + '\'' +
                ", currentMenu='" + currentMenu + '\'' +
                ", lastTransactionCode='" + lastTransactionCode + '\'' +
                ", sessionStartTime=" + sessionStartTime +
                ", lastActivityTime=" + lastActivityTime +
                ", operationStatus='" + operationStatus + '\'' +
                ", attributeCount=" + (sessionAttributes != null ? sessionAttributes.size() : 0) +
                '}';
    }
    
    /**
     * Determines equality based on user identity and session timing.
     * 
     * Two SessionContext instances are considered equal if they represent the same
     * user session with identical user ID, role, and session start time. This
     * implementation supports proper session comparison in collections and caching
     * scenarios while excluding mutable fields like last activity time.
     * 
     * @param obj The object to compare with this SessionContext
     * @return true if the objects represent the same user session, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SessionContext that = (SessionContext) obj;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(userRole, that.userRole) &&
               Objects.equals(sessionStartTime, that.sessionStartTime);
    }
    
    /**
     * Generates hash code based on user identity and session start time.
     * 
     * Provides consistent hash code generation for use in hash-based collections
     * such as HashMap and HashSet. The implementation ensures that equal session
     * contexts have equal hash codes as required by the Object contract.
     * 
     * @return The computed hash code for this SessionContext instance
     */
    @Override
    public int hashCode() {
        return Objects.hash(userId, userRole, sessionStartTime);
    }
    
    /**
     * Convenience method to check if the session represents an administrative user.
     * 
     * @return true if the user role is 'A' (Administrator), false otherwise
     */
    public boolean isAdminUser() {
        return "A".equals(userRole);
    }
    
    /**
     * Convenience method to check if the session represents a regular user.
     * 
     * @return true if the user role is 'U' (User), false otherwise
     */
    public boolean isRegularUser() {
        return "U".equals(userRole);
    }
    
    /**
     * Updates the last activity time to the current timestamp.
     * 
     * This method should be called on each API request to maintain accurate
     * session timeout calculation and support sliding session timeout behavior.
     */
    public void updateActivityTime() {
        this.lastActivityTime = LocalDateTime.now();
    }
    
    /**
     * Checks if the session has exceeded the specified timeout period.
     * 
     * @param timeoutMinutes The session timeout in minutes
     * @return true if the session has timed out, false otherwise
     */
    public boolean isTimedOut(int timeoutMinutes) {
        if (lastActivityTime == null) {
            return false;
        }
        return lastActivityTime.isBefore(LocalDateTime.now().minusMinutes(timeoutMinutes));
    }
}