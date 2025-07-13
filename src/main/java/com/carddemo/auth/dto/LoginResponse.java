/**
 * LoginResponse DTO
 * 
 * Authentication response containing JWT token, user information, and role-based 
 * routing data for successful login processing and frontend state management.
 * 
 * This DTO implements the transformation of COBOL COSGN00C.cbl authentication flow
 * to Spring Security JWT-based authentication, preserving the original user type
 * validation and role-based menu routing while enabling modern cloud-native
 * authentication patterns.
 * 
 * Original COBOL Implementation Reference:
 * - COSGN00C.cbl: Main authentication program with user type validation
 * - CSUSR01Y.cpy: User data structure with SEC-USR-TYPE role mapping
 * - COMMAREA: Session context preservation through distributed Redis session
 * 
 * Spring Security Integration:
 * - JWT token generation with role claims for stateless authentication
 * - User type 'A' maps to ROLE_ADMIN for administrative access
 * - User type 'U' maps to ROLE_USER for standard transaction processing
 * - Audit timestamp for authentication compliance and session management
 * 
 * @author Blitzy Platform Engineering Team
 * @version 1.0
 * @since CardDemo v1.0-15-g27d6c6f-68
 */
package com.carddemo.auth.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Data Transfer Object for authentication response containing JWT token,
 * user information, and role-based routing data.
 * 
 * Replaces CICS COMMAREA session management with Spring Security JWT
 * authentication and Redis-backed session storage for cloud-native
 * microservices architecture.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    /**
     * JWT authentication token for stateless API authentication.
     * 
     * Contains encoded user identity, role claims, and session correlation ID
     * using HS256 algorithm with configurable secret key rotation.
     * Replaces CICS session management with Spring Security OAuth2 Resource Server.
     */
    @JsonProperty("token")
    private String token;

    /**
     * Unique user identifier from PostgreSQL users table.
     * 
     * Migrated from COBOL SEC-USR-ID field (8 characters) with case-insensitive
     * matching preserved. Used for session correlation and audit trail logging.
     */
    @JsonProperty("userId")
    private String userId;

    /**
     * User type code determining authorization level.
     * 
     * Preserves COBOL SEC-USR-TYPE field mapping:
     * - 'A': Administrative user with full system access
     * - 'U': Regular user with standard transaction processing access
     * 
     * Maps to Spring Security roles for @PreAuthorize method-level authorization.
     */
    @JsonProperty("userType")
    private String userType;

    /**
     * Spring Security role authority for authorization decisions.
     * 
     * Role mapping from COBOL user types:
     * - ROLE_ADMIN: Derived from userType 'A' - full administrative privileges
     * - ROLE_USER: Derived from userType 'U' - standard transaction access
     * 
     * Used by Spring Security @PreAuthorize annotations for method-level security.
     */
    @JsonProperty("role")
    private String role;

    /**
     * Authentication timestamp for audit trail and session management.
     * 
     * Captures exact authentication time using LocalDateTime.now() for:
     * - SOX compliance audit requirements
     * - Session timeout calculation with Redis TTL
     * - Security incident correlation and analysis
     * - Authentication performance monitoring
     */
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    /**
     * User's first name for UI personalization.
     * 
     * Migrated from COBOL SEC-USR-FNAME field (20 characters).
     * Used for welcome messages and audit trail enhancement.
     */
    @JsonProperty("firstName")
    private String firstName;

    /**
     * User's last name for UI personalization.
     * 
     * Migrated from COBOL SEC-USR-LNAME field (20 characters).
     * Used for complete user identification in audit logs.
     */
    @JsonProperty("lastName")
    private String lastName;

    /**
     * Frontend routing information based on user role.
     * 
     * Replaces COBOL XCTL program routing logic:
     * - Admin users (Type 'A'): Route to AdminMenuComponent.jsx
     * - Regular users (Type 'U'): Route to MainMenuComponent.jsx
     * 
     * Enables React Router dynamic navigation based on Spring Security roles.
     */
    @JsonProperty("routeTo")
    private String routeTo;

    /**
     * Session correlation ID for distributed session management.
     * 
     * Links JWT token to Redis session store for:
     * - Pseudo-conversational state preservation
     * - Cross-service session sharing
     * - Session invalidation and cleanup
     * - Audit trail correlation
     */
    @JsonProperty("sessionId")
    private String sessionId;

    /**
     * Default constructor for JSON deserialization.
     */
    public LoginResponse() {
        // Initialize timestamp to current time for audit trail
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Full constructor for authentication response creation.
     * 
     * @param token JWT authentication token with role claims
     * @param userId User identifier from authentication service
     * @param userType COBOL user type ('A' for Admin, 'U' for User)
     * @param role Spring Security role authority
     * @param firstName User's first name for personalization
     * @param lastName User's last name for audit trail
     */
    public LoginResponse(String token, String userId, String userType, String role, 
                        String firstName, String lastName) {
        this.token = token;
        this.userId = userId;
        this.userType = userType;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
        this.timestamp = LocalDateTime.now();
        
        // Set routing information based on user role
        this.routeTo = determineRouting(userType);
        
        // Session ID will be set by authentication service
        this.sessionId = null;
    }

    /**
     * Determines frontend routing path based on user type.
     * 
     * Implements COBOL COSGN00C.cbl routing logic:
     * - CDEMO-USRTYP-ADMIN: XCTL to COADM01C → /admin/menu
     * - Regular users: XCTL to COMEN01C → /main/menu
     * 
     * @param userType COBOL user type indicator
     * @return Frontend route path for React Router navigation
     */
    private String determineRouting(String userType) {
        if ("A".equals(userType)) {
            return "/admin/menu";
        } else if ("U".equals(userType)) {
            return "/main/menu";
        } else {
            // Default to main menu for unknown user types
            return "/main/menu";
        }
    }

    /**
     * Gets the JWT authentication token.
     * 
     * @return JWT token string with encoded claims
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the JWT authentication token.
     * 
     * @param token JWT token string with role claims and session correlation
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Gets the user identifier.
     * 
     * @return User ID from PostgreSQL users table
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user identifier.
     * 
     * @param userId User ID for session correlation and audit trail
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets the user type code.
     * 
     * @return COBOL user type ('A' for Admin, 'U' for User)
     */
    public String getUserType() {
        return userType;
    }

    /**
     * Sets the user type code and updates routing accordingly.
     * 
     * @param userType COBOL user type determining authorization level
     */
    public void setUserType(String userType) {
        this.userType = userType;
        this.routeTo = determineRouting(userType);
    }

    /**
     * Gets the Spring Security role authority.
     * 
     * @return Role string for @PreAuthorize authorization decisions
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the Spring Security role authority.
     * 
     * @param role Spring Security role (ROLE_ADMIN or ROLE_USER)
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Gets the authentication timestamp.
     * 
     * @return LocalDateTime of successful authentication
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the authentication timestamp.
     * 
     * @param timestamp Authentication time for audit trail
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the user's first name.
     * 
     * @return First name from user profile
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the user's first name.
     * 
     * @param firstName User's first name for UI personalization
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the user's last name.
     * 
     * @return Last name from user profile
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the user's last name.
     * 
     * @param lastName User's last name for audit trail
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Gets the frontend routing path.
     * 
     * @return React Router path based on user role
     */
    public String getRouteTo() {
        return routeTo;
    }

    /**
     * Sets the frontend routing path.
     * 
     * @param routeTo React component route for role-based navigation
     */
    public void setRouteTo(String routeTo) {
        this.routeTo = routeTo;
    }

    /**
     * Gets the session correlation ID.
     * 
     * @return Session ID for Redis session store correlation
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the session correlation ID.
     * 
     * @param sessionId Distributed session identifier for Redis store
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Creates a full user display name.
     * 
     * Utility method for UI components requiring complete user identification.
     * 
     * @return Formatted "firstName lastName" or userId if names unavailable
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName.trim() + " " + lastName.trim();
        } else if (firstName != null) {
            return firstName.trim();
        } else if (lastName != null) {
            return lastName.trim();
        } else {
            return userId; // Fallback to user ID
        }
    }

    /**
     * Checks if user has administrative privileges.
     * 
     * Utility method for conditional UI rendering and authorization checks.
     * Replaces COBOL CDEMO-USRTYP-ADMIN condition evaluation.
     * 
     * @return true if user type is 'A' (Administrator)
     */
    public boolean isAdmin() {
        return "A".equals(userType) || "ROLE_ADMIN".equals(role);
    }

    /**
     * Checks if authentication response is valid.
     * 
     * Validates that all required fields are present for successful authentication.
     * Used by authentication service for response validation before JSON serialization.
     * 
     * @return true if token, userId, userType, and role are all non-null
     */
    public boolean isValid() {
        return token != null && !token.trim().isEmpty() &&
               userId != null && !userId.trim().isEmpty() &&
               userType != null && !userType.trim().isEmpty() &&
               role != null && !role.trim().isEmpty();
    }

    /**
     * Returns string representation for debugging and audit logging.
     * 
     * Excludes sensitive token information while preserving audit trail data.
     * 
     * @return Formatted string with user context for logging
     */
    @Override
    public String toString() {
        return String.format(
            "LoginResponse{userId='%s', userType='%s', role='%s', " +
            "routeTo='%s', timestamp=%s, sessionId='%s', hasToken=%s}",
            userId, userType, role, routeTo, timestamp, sessionId, 
            (token != null && !token.isEmpty())
        );
    }

    /**
     * Checks equality based on user ID and session ID.
     * 
     * Used for session management and duplicate authentication detection.
     * 
     * @param obj Object to compare
     * @return true if userId and sessionId match
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        LoginResponse that = (LoginResponse) obj;
        
        if (userId != null ? !userId.equals(that.userId) : that.userId != null) {
            return false;
        }
        return sessionId != null ? sessionId.equals(that.sessionId) : that.sessionId == null;
    }

    /**
     * Generates hash code based on user ID and session ID.
     * 
     * @return Hash code for session management collections
     */
    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (sessionId != null ? sessionId.hashCode() : 0);
        return result;
    }
}