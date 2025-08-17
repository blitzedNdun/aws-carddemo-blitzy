package com.carddemo.dto;

import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for user update requests containing validated user profile data.
 * Maps to COUSR02C.cbl input structure with fields for user ID, names, password, and type.
 * Includes validation annotations for business rules enforcement matching COBOL validation logic.
 * 
 * Based on COBOL copybook CSUSR01Y.cpy:
 * - SEC-USR-ID: PIC X(08) - User identifier 
 * - SEC-USR-FNAME: PIC X(20) - First name
 * - SEC-USR-LNAME: PIC X(20) - Last name  
 * - SEC-USR-PWD: PIC X(08) - Password
 * - SEC-USR-TYPE: PIC X(01) - User type
 * 
 * Supports optional fields for partial updates while maintaining COBOL field constraints.
 */
public class UserUpdateRequest {

    /**
     * User identifier - maps to SEC-USR-ID (PIC X(08))
     * Maximum 8 characters as per COBOL field definition
     */
    @Size(max = 8, message = "User ID must not exceed 8 characters")
    private String userId;

    /**
     * User first name - maps to SEC-USR-FNAME (PIC X(20)) 
     * Maximum 20 characters as per COBOL field definition
     */
    @Size(max = 20, message = "First name must not exceed 20 characters")
    private String firstName;

    /**
     * User last name - maps to SEC-USR-LNAME (PIC X(20))
     * Maximum 20 characters as per COBOL field definition  
     */
    @Size(max = 20, message = "Last name must not exceed 20 characters")
    private String lastName;

    /**
     * User password - maps to SEC-USR-PWD (PIC X(08))
     * Maximum 8 characters as per COBOL field definition
     */
    @Size(max = 8, message = "Password must not exceed 8 characters")
    private String password;

    /**
     * User type - maps to SEC-USR-TYPE (PIC X(01))
     * Maximum 1 character as per COBOL field definition
     */
    @Size(max = 1, message = "User type must not exceed 1 character")
    private String userType;

    /**
     * Default constructor for UserUpdateRequest
     */
    public UserUpdateRequest() {
    }

    /**
     * Constructor with all fields for UserUpdateRequest
     * 
     * @param userId    User identifier (max 8 characters)
     * @param firstName User first name (max 20 characters)
     * @param lastName  User last name (max 20 characters)
     * @param password  User password (max 8 characters)
     * @param userType  User type (max 1 character)
     */
    public UserUpdateRequest(String userId, String firstName, String lastName, String password, String userType) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.userType = userType;
    }

    /**
     * Gets the user identifier
     * 
     * @return userId User identifier
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user identifier
     * 
     * @param userId User identifier (max 8 characters)
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets the user first name
     * 
     * @return firstName User first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the user first name
     * 
     * @param firstName User first name (max 20 characters)
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the user last name
     * 
     * @return lastName User last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the user last name
     * 
     * @param lastName User last name (max 20 characters)
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Gets the user password
     * 
     * @return password User password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the user password
     * 
     * @param password User password (max 8 characters)
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Gets the user type
     * 
     * @return userType User type
     */
    public String getUserType() {
        return userType;
    }

    /**
     * Sets the user type
     * 
     * @param userType User type (max 1 character)
     */
    public void setUserType(String userType) {
        this.userType = userType;
    }

    /**
     * String representation of UserUpdateRequest
     * 
     * @return String representation with all field values (password masked)
     */
    @Override
    public String toString() {
        return "UserUpdateRequest{" +
                "userId='" + userId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", password='[MASKED]'" +
                ", userType='" + userType + '\'' +
                '}';
    }

    /**
     * Checks equality based on userId
     * 
     * @param obj Object to compare
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        UserUpdateRequest that = (UserUpdateRequest) obj;
        return userId != null ? userId.equals(that.userId) : that.userId == null;
    }

    /**
     * Hash code based on userId
     * 
     * @return hash code
     */
    @Override
    public int hashCode() {
        return userId != null ? userId.hashCode() : 0;
    }
}