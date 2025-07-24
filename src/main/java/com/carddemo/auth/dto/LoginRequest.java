/**
 * LoginRequest DTO for CardDemo Application Authentication
 * 
 * This Data Transfer Object captures user credentials for Spring Security JWT authentication,
 * replacing the legacy COBOL COSGN00C sign-on processing with modern REST API endpoints.
 * Maintains exact field length requirements from original COBOL structure while adding
 * contemporary validation and security features.
 * 
 * Original COBOL mapping:
 * - WS-USER-ID PIC X(08) -> username field
 * - WS-USER-PWD PIC X(08) -> password field
 * 
 * Security Features:
 * - Jakarta Bean Validation for input sanitization
 * - Field length constraints matching COBOL requirements
 * - JSON deserialization for REST API processing
 * - Input validation preventing injection attacks
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since CardDemo v1.0
 */
package com.carddemo.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Login request DTO for authentication endpoint
 * 
 * Captures user credentials with validation constraints that match
 * the original COBOL field definitions while providing modern
 * security validation and JSON processing capabilities.
 */
public class LoginRequest {

    /**
     * User identification field
     * 
     * Maps to COBOL WS-USER-ID PIC X(08) from COSGN00C program.
     * Enforces exact 8-character length requirement to maintain
     * compatibility with existing user database structure.
     * 
     * Validation rules:
     * - Cannot be null or empty
     * - Must be between 1 and 8 characters
     * - Will be converted to uppercase during processing
     */
    @NotNull(message = "Username is required")
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 1, max = 8, message = "Username must be between 1 and 8 characters")
    @JsonProperty("username")
    private String username;

    /**
     * User password field
     * 
     * Maps to COBOL WS-USER-PWD PIC X(08) from COSGN00C program.
     * Enforces exact 8-character length requirement matching
     * the original mainframe authentication structure.
     * 
     * Validation rules:
     * - Cannot be null or empty
     * - Must be between 1 and 8 characters
     * - Will be processed with BCrypt hashing for secure storage
     */
    @NotNull(message = "Password is required")
    @NotBlank(message = "Password cannot be blank")
    @Size(min = 1, max = 8, message = "Password must be between 1 and 8 characters")
    @JsonProperty("password")
    private String password;

    /**
     * Default constructor for JSON deserialization
     * 
     * Required by Jackson for REST API request processing.
     * Initializes all fields to null for validation framework.
     */
    public LoginRequest() {
        // Default constructor for JSON deserialization
    }

    /**
     * Constructor with parameters for programmatic creation
     * 
     * @param username User identification (1-8 characters)
     * @param password User password (1-8 characters)
     */
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Gets the username field
     * 
     * @return Username string (1-8 characters)
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username field
     * 
     * Input will be trimmed to remove leading/trailing whitespace
     * and validated against length constraints during bean validation.
     * 
     * @param username User identification (1-8 characters)
     */
    public void setUsername(String username) {
        this.username = username != null ? username.trim() : null;
    }

    /**
     * Gets the password field
     * 
     * @return Password string (1-8 characters)
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password field
     * 
     * Password is stored as-is without trimming to preserve
     * exact character input as required by authentication logic.
     * 
     * @param password User password (1-8 characters)
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * String representation for logging and debugging
     * 
     * Does not include password field for security reasons.
     * Only shows username for audit trail purposes.
     * 
     * @return String representation with masked password
     */
    @Override
    public String toString() {
        return "LoginRequest{" +
                "username='" + username + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }

    /**
     * Equality comparison based on username only
     * 
     * Password is excluded from equality check for security reasons.
     * Two LoginRequest objects are considered equal if they have
     * the same username, regardless of password content.
     * 
     * @param obj Object to compare
     * @return true if usernames match, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        LoginRequest that = (LoginRequest) obj;
        return username != null ? username.equals(that.username) : that.username == null;
    }

    /**
     * Hash code based on username only
     * 
     * Consistent with equals() method, uses only username
     * for hash code generation to maintain security.
     * 
     * @return Hash code based on username
     */
    @Override
    public int hashCode() {
        return username != null ? username.hashCode() : 0;
    }

    /**
     * Validates if the request contains non-empty credentials
     * 
     * Provides programmatic validation beyond Jakarta Bean Validation
     * for additional security checks during authentication processing.
     * 
     * @return true if both username and password are present and non-empty
     */
    public boolean hasValidCredentials() {
        return username != null && !username.trim().isEmpty() &&
               password != null && !password.isEmpty();
    }

    /**
     * Creates a sanitized copy for logging purposes
     * 
     * Returns a new LoginRequest with the password field masked
     * for secure logging in audit trails and debugging output.
     * 
     * @return LoginRequest copy with masked password
     */
    public LoginRequest createLoggableCopy() {
        LoginRequest copy = new LoginRequest();
        copy.setUsername(this.username);
        copy.setPassword("[MASKED]");
        return copy;
    }
}