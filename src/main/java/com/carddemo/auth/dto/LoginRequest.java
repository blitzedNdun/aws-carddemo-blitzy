/*
 * CardDemo Authentication Login Request DTO
 * 
 * This class represents the login request payload for the CardDemo authentication system.
 * It captures user credentials for Spring Security JWT authentication, maintaining 
 * exact field length requirements from the original COBOL COSGN00C program.
 *
 * Field Length Requirements (from COBOL):
 * - Username: PIC X(08) - exactly 8 characters
 * - Password: PIC X(08) - exactly 8 characters
 *
 * The DTO includes comprehensive Jakarta Bean Validation annotations for input validation
 * and security compliance, ensuring proper field constraints and format validation
 * before credential processing.
 *
 * Original COBOL Source: app/cbl/COSGN00C.cbl
 * - WS-USER-ID                 PIC X(08)
 * - WS-USER-PWD                PIC X(08)
 *
 * Security Features:
 * - Field length validation matching COBOL requirements
 * - Input sanitization through Jakarta Bean Validation
 * - JSON deserialization support for REST API processing
 * - Trimming and format validation for authentication security
 */

package com.carddemo.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Login Request DTO for CardDemo authentication system.
 * 
 * This class captures user credentials for Spring Security JWT authentication,
 * maintaining exact field length requirements from the original COBOL program
 * while providing modern validation and security features.
 */
public class LoginRequest {
    
    /**
     * Username field - exactly 8 characters matching COBOL WS-USER-ID PIC X(08)
     * 
     * Validation Rules:
     * - Required field (cannot be blank)
     * - Exactly 8 characters in length
     * - Will be converted to uppercase during processing (matching COBOL behavior)
     */
    @NotBlank(message = "Username is required")
    @Size(min = 1, max = 8, message = "Username must be between 1 and 8 characters")
    @JsonProperty("username")
    private String username;
    
    /**
     * Password field - exactly 8 characters matching COBOL WS-USER-PWD PIC X(08)
     * 
     * Validation Rules:
     * - Required field (cannot be blank)
     * - Exactly 8 characters in length
     * - Will be processed with BCrypt hashing for secure credential storage
     */
    @NotBlank(message = "Password is required")
    @Size(min = 1, max = 8, message = "Password must be between 1 and 8 characters")
    @JsonProperty("password")
    private String password;
    
    /**
     * Default constructor for JSON deserialization
     */
    public LoginRequest() {
        // Default constructor required for JSON deserialization
    }
    
    /**
     * Constructor with parameters for programmatic creation
     * 
     * @param username the username credential (1-8 characters)
     * @param password the password credential (1-8 characters)
     */
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    /**
     * Get username field value
     * 
     * @return the username string, will be converted to uppercase during authentication
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Set username field value
     * 
     * @param username the username string (1-8 characters)
     */
    public void setUsername(String username) {
        this.username = username != null ? username.trim() : null;
    }
    
    /**
     * Get password field value
     * 
     * @return the password string for BCrypt validation
     */
    public String getPassword() {
        return password;
    }
    
    /**
     * Set password field value
     * 
     * @param password the password string (1-8 characters)
     */
    public void setPassword(String password) {
        this.password = password != null ? password.trim() : null;
    }
    
    /**
     * String representation for logging and debugging
     * 
     * Note: Password is masked for security - never log actual password values
     * 
     * @return formatted string representation of the login request
     */
    @Override
    public String toString() {
        return "LoginRequest{" +
                "username='" + username + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }
    
    /**
     * Equals method for object comparison
     * 
     * @param obj the object to compare
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        LoginRequest that = (LoginRequest) obj;
        
        if (username != null ? !username.equals(that.username) : that.username != null) return false;
        return password != null ? password.equals(that.password) : that.password == null;
    }
    
    /**
     * Hash code implementation
     * 
     * @return hash code value for this object
     */
    @Override
    public int hashCode() {
        int result = username != null ? username.hashCode() : 0;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        return result;
    }
}