package com.carddemo.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for authentication login request.
 * 
 * This DTO captures user credentials for Spring Security authentication
 * and maintains functional equivalence with the original COBOL COSGN00C
 * program's credential handling. Field length constraints match the 
 * original COBOL working storage definitions:
 * - WS-USER-ID   PIC X(08)
 * - WS-USER-PWD  PIC X(08)
 * 
 * The DTO supports JSON deserialization for REST API authentication
 * endpoint processing and includes comprehensive input validation
 * through Jakarta Bean Validation annotations.
 * 
 * @see com.carddemo.auth.AuthenticationService
 * @author CardDemo Development Team
 */
public class LoginRequest {

    /**
     * User identification field matching COBOL SEC-USR-ID structure.
     * Exactly 8 characters as defined in CSUSR01Y copybook and COSGN00C program.
     * 
     * Validation constraints:
     * - Required field (cannot be null or blank)
     * - Exactly 8 characters matching COBOL PIC X(08) definition
     * - Alphanumeric characters only for security compliance
     * - Whitespace trimmed during processing
     */
    @NotNull(message = "Username is required")
    @NotBlank(message = "Username cannot be empty")
    @Size(min = 1, max = 8, message = "Username must be between 1 and 8 characters")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "Username must contain only alphanumeric characters")
    private String username;

    /**
     * User password field matching COBOL SEC-USR-PWD structure.
     * Exactly 8 characters as defined in CSUSR01Y copybook and COSGN00C program.
     * 
     * Validation constraints:
     * - Required field (cannot be null or blank)
     * - Exactly 8 characters matching COBOL PIC X(08) definition
     * - No character restrictions (allows special characters for password complexity)
     * - Will be processed with BCrypt hashing for secure storage
     */
    @NotNull(message = "Password is required")
    @NotBlank(message = "Password cannot be empty")
    @Size(min = 1, max = 8, message = "Password must be between 1 and 8 characters")
    private String password;

    /**
     * Default constructor for JSON deserialization and framework instantiation.
     */
    public LoginRequest() {
        // Default constructor for JSON binding
    }

    /**
     * Constructor for creating login request with credentials.
     * 
     * @param username User identification (max 8 characters)
     * @param password User password (max 8 characters)
     */
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Gets the username field.
     * 
     * @return username for authentication (max 8 characters)
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username field with input sanitization.
     * Trims whitespace and converts to uppercase to match COBOL processing.
     * 
     * @param username User identification (max 8 characters)
     */
    public void setUsername(String username) {
        this.username = username != null ? username.trim().toUpperCase() : null;
    }

    /**
     * Gets the password field.
     * 
     * @return password for authentication (max 8 characters)
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password field with basic sanitization.
     * Preserves case sensitivity for password security.
     * 
     * @param password User password (max 8 characters)
     */
    public void setPassword(String password) {
        this.password = password != null ? password.trim() : null;
    }

    /**
     * Validates that both username and password are provided.
     * 
     * @return true if both fields contain non-blank values
     */
    public boolean isValid() {
        return username != null && !username.trim().isEmpty() &&
               password != null && !password.trim().isEmpty();
    }

    /**
     * Returns string representation for logging (excludes password for security).
     * 
     * @return string representation with masked password
     */
    @Override
    public String toString() {
        return "LoginRequest{" +
                "username='" + username + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }

    /**
     * Equals method comparing only username for security (excludes password).
     * 
     * @param obj object to compare
     * @return true if usernames match
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        LoginRequest that = (LoginRequest) obj;
        return username != null ? username.equals(that.username) : that.username == null;
    }

    /**
     * Hash code method using only username for security (excludes password).
     * 
     * @return hash code based on username
     */
    @Override
    public int hashCode() {
        return username != null ? username.hashCode() : 0;
    }
}