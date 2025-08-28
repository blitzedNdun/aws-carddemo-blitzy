/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;

/**
 * Response DTO for user sign-on/authentication result.
 * 
 * Maps the COSGN00 BMS screen output fields to a modern REST API response structure,
 * preserving all display information from the original CICS sign-on transaction while 
 * providing additional authentication context for session management and menu navigation.
 * 
 * This DTO maintains functional parity with the original COBOL/CICS COSGN00C program
 * response patterns while enabling JSON-based communication for the React frontend.
 * 
 * Key field mappings from COSGN00.bms:
 * - TRNNAME/TRNNAMEI → transactionCode
 * - PGMNAME/PGMNAMEI → programName  
 * - CURDATE/CURTIME → timestamp (combined)
 * - APPLID/APPLIDI → applicationId
 * - SYSID/SYSIDI → systemId
 * - ERRMSG/ERRMSGI → errorMessage
 * - USERID input → userId (from authentication)
 * 
 * Additional fields for modern authentication:
 * - status: Authentication result (SUCCESS/ERROR)
 * - userName: Display name for authenticated user
 * - userRole: Role-based access control information  
 * - sessionToken: Session management token
 * - menuOptions: Available menu options after successful authentication
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SignOnResponse {

    /**
     * Authentication result status.
     * Maps to the success/failure logic from COSGN00C program flow.
     */
    private String status;

    /**
     * User identifier from sign-on input.
     * Maps to USERID field from COSGN00 copybook (PIC X(8)).
     * Validated to match original COBOL field constraints.
     */
    @Size(max = 8, message = "User ID cannot exceed 8 characters")
    private String userId;

    /**
     * Display name for the authenticated user.
     * Retrieved from user security tables after successful authentication.
     * Used for personalized greeting in the UI.
     */
    private String userName;

    /**
     * User role for access control.
     * Retrieved from user security tables, used for menu option filtering.
     * Maps to RACF role concepts from the original mainframe security.
     */
    @Size(max = 8, message = "User role cannot exceed 8 characters") 
    private String userRole;

    /**
     * Session token for maintaining authenticated state.
     * Replaces CICS COMMAREA session management with modern token-based approach.
     * Used by subsequent REST API calls for authentication.
     */
    private String sessionToken;

    /**
     * List of available menu options for the authenticated user.
     * Populated based on user role and access permissions.
     * Enables immediate navigation after successful sign-on.
     */
    private List<MenuOption> menuOptions;

    /**
     * Error message for failed authentication attempts.
     * Maps to ERRMSG field from COSGN00 copybook (PIC X(78)).
     * Displayed in red on the original 3270 screen, shown as error in UI.
     */
    @Size(max = 78, message = "Error message cannot exceed 78 characters")
    private String errorMessage;

    /**
     * Current date and time of the authentication attempt.
     * Maps to CURDATE and CURTIME fields from COSGN00 BMS screen.
     * Provides temporal context matching original screen display.
     */
    private LocalDateTime timestamp;

    /**
     * Transaction code that processed the sign-on request.
     * Maps to TRNNAME field from COSGN00 copybook (PIC X(4)).
     * Typically "CC00" for the sign-on transaction.
     */
    @Size(max = 4, message = "Transaction code cannot exceed 4 characters")
    private String transactionCode;

    /**
     * Program name that handled the authentication.
     * Maps to PGMNAME field from COSGN00 copybook (PIC X(8)).
     * Typically "COSGN00C" for the sign-on program.
     */
    @Size(max = 8, message = "Program name cannot exceed 8 characters")
    private String programName;

    /**
     * Application identifier for the CardDemo system.
     * Maps to APPLID field from COSGN00 copybook (PIC X(8)).
     * Identifies the CICS application region.
     */
    @Size(max = 8, message = "Application ID cannot exceed 8 characters")
    private String applicationId;

    /**
     * System identifier for the target environment.
     * Maps to SYSID field from COSGN00 copybook (PIC X(8)).
     * Identifies the system instance (DEV, TEST, PROD).
     */
    @Size(max = 8, message = "System ID cannot exceed 8 characters")
    private String systemId;

    /**
     * Constructor with timestamp initialization.
     * Sets the current timestamp using LocalDateTime.now() for immediate temporal context.
     */
    public SignOnResponse() {
        this.timestamp = LocalDateTime.now();
        this.menuOptions = new ArrayList<>();
    }

    /**
     * Gets the authentication status.
     * 
     * @return The status string ("SUCCESS" or "ERROR")
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the authentication status.
     * 
     * @param status The authentication result status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the user identifier.
     * 
     * @return The user ID (max 8 characters)
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user identifier.
     * 
     * @param userId The user ID, validated to 8 characters max
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets the user display name.
     * 
     * @return The user's full name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the user display name.
     * 
     * @param userName The user's full name for display
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Gets the user role.
     * 
     * @return The user's role for access control
     */
    public String getUserRole() {
        return userRole;
    }

    /**
     * Sets the user role.
     * 
     * @param userRole The user's role for access control
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    /**
     * Gets the session token.
     * 
     * @return The session token for API authentication
     */
    public String getSessionToken() {
        return sessionToken;
    }

    /**
     * Sets the session token.
     * 
     * @param sessionToken The session token for maintaining authentication state
     */
    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    /**
     * Gets the list of available menu options.
     * 
     * @return The list of MenuOption objects for navigation
     */
    public List<MenuOption> getMenuOptions() {
        return menuOptions;
    }

    /**
     * Sets the list of available menu options.
     * 
     * @param menuOptions The list of MenuOption objects based on user role
     */
    public void setMenuOptions(List<MenuOption> menuOptions) {
        this.menuOptions = menuOptions;
    }

    /**
     * Gets the error message.
     * 
     * @return The error message for failed authentication (max 78 characters)
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message.
     * 
     * @param errorMessage The error message for display to user
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Gets the authentication timestamp.
     * 
     * @return The LocalDateTime when authentication was processed
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the authentication timestamp.
     * 
     * @param timestamp The timestamp for this authentication attempt
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the transaction code.
     * 
     * @return The transaction code that processed this request
     */
    public String getTransactionCode() {
        return transactionCode;
    }

    /**
     * Sets the transaction code.
     * 
     * @param transactionCode The CICS transaction code (typically "CC00")
     */
    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }

    /**
     * Gets the program name.
     * 
     * @return The COBOL program name that handled authentication
     */
    public String getProgramName() {
        return programName;
    }

    /**
     * Sets the program name.
     * 
     * @param programName The COBOL program name (typically "COSGN00C")
     */
    public void setProgramName(String programName) {
        this.programName = programName;
    }

    /**
     * Gets the application identifier.
     * 
     * @return The CICS application ID
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * Sets the application identifier.
     * 
     * @param applicationId The CICS application ID
     */
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * Gets the system identifier.
     * 
     * @return The system ID (DEV, TEST, PROD)
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * Sets the system identifier.
     * 
     * @param systemId The system ID for environment identification
     */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    // Convenience methods for SignOnService compatibility

    /**
     * Sets the authentication success status.
     * Convenience method that maps boolean success to status string.
     * 
     * @param success True for successful authentication, false for failure
     */
    public void setSuccess(boolean success) {
        this.status = success ? "SUCCESS" : "ERROR";
    }

    /**
     * Sets the response message.
     * Convenience method that maps to errorMessage field.
     * 
     * @param message The message to set (typically error message)
     */
    public void setMessage(String message) {
        this.errorMessage = message;
    }

    /**
     * Sets the user type.
     * Convenience method that maps to userRole field for compatibility.
     * 
     * @param userType The user type/role to set
     */
    public void setUserType(String userType) {
        this.userRole = userType;
    }

    /**
     * Checks equality based on userId and timestamp.
     * Two SignOnResponse objects are equal if they have the same userId and timestamp.
     * 
     * @param obj The object to compare with
     * @return True if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SignOnResponse that = (SignOnResponse) obj;
        return (userId != null ? userId.equals(that.userId) : that.userId == null) &&
               (timestamp != null ? timestamp.equals(that.timestamp) : that.timestamp == null);
    }

    /**
     * Generates hash code based on userId and timestamp.
     * 
     * @return Hash code value
     */
    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }

    /**
     * Provides string representation of the sign-on response.
     * Includes key fields for debugging and logging purposes.
     * 
     * @return String representation
     */
    @Override
    public String toString() {
        return "SignOnResponse{" +
                "status='" + status + '\'' +
                ", userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", userRole='" + userRole + '\'' +
                ", sessionToken='" + (sessionToken != null ? "[PROTECTED]" : null) + '\'' +
                ", menuOptions=" + (menuOptions != null ? menuOptions.size() + " options" : null) +
                ", errorMessage='" + errorMessage + '\'' +
                ", timestamp=" + timestamp +
                ", transactionCode='" + transactionCode + '\'' +
                ", programName='" + programName + '\'' +
                ", applicationId='" + applicationId + '\'' +
                ", systemId='" + systemId + '\'' +
                '}';
    }

    /**
     * Enumeration for authentication status values.
     * Provides type-safe constants for sign-on result states.
     */
    public enum SignOnStatus {
        /**
         * Successful authentication status.
         * Indicates user credentials were validated and session established.
         */
        SUCCESS,
        
        /**
         * Failed authentication status.
         * Indicates authentication failed due to invalid credentials or system error.
         */
        ERROR;

        /**
         * Returns all available status values.
         * Note: This method is automatically generated by Java for enums.
         * 
         * @return Array of all SignOnStatus enum values
         */
        // values() method is automatically provided by Java enum

        /**
         * Returns the SignOnStatus enum value for the given string.
         * Note: This method is automatically generated by Java for enums.
         * 
         * @param name The string name of the status
         * @return The corresponding SignOnStatus enum value
         * @throws IllegalArgumentException if the name doesn't match any enum value
         */
        // valueOf(String) method is automatically provided by Java enum
    }
}