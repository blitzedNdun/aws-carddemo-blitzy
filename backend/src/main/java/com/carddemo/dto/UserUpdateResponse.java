package com.carddemo.dto;

/**
 * Data Transfer Object for user update responses containing operation result status 
 * and updated user profile data. Maps to COUSR02C.cbl output structure with result 
 * messages and updated user information.
 * 
 * This DTO maintains COBOL-equivalent response codes and message patterns to ensure
 * compatibility with existing CICS transaction processing and external interface contracts.
 * 
 * Based on COUSR02C.cbl program structure and CSUSR01Y.cpy copybook definitions.
 */
public class UserUpdateResponse {
    
    /**
     * Operation success status indicator
     * Maps to COBOL WS-ERR-FLG pattern (Y/N)
     */
    private boolean success;
    
    /**
     * Response message matching COBOL message patterns
     * Maps to WS-MESSAGE field from COUSR02C.cbl
     */
    private String message;
    
    /**
     * Updated user data structure
     * Corresponds to SEC-USER-DATA from CSUSR01Y.cpy
     */
    private UpdatedUserData updatedUser;
    
    /**
     * COBOL-equivalent error code for external system compatibility
     * Maps to CICS response codes: NORMAL, NOTFND, OTHER
     */
    private String errorCode;
    
    /**
     * Default constructor
     */
    public UserUpdateResponse() {
        this.success = false;
        this.message = "";
        this.updatedUser = null;
        this.errorCode = "";
    }
    
    /**
     * Constructor for success response
     * @param success Operation success status
     * @param message Success message
     * @param updatedUser Updated user data
     */
    public UserUpdateResponse(boolean success, String message, UpdatedUserData updatedUser) {
        this.success = success;
        this.message = message;
        this.updatedUser = updatedUser;
        this.errorCode = success ? "NORMAL" : "OTHER";
    }
    
    /**
     * Constructor for error response
     * @param success Operation success status (false)
     * @param message Error message
     * @param errorCode COBOL-equivalent error code
     */
    public UserUpdateResponse(boolean success, String message, String errorCode) {
        this.success = success;
        this.message = message;
        this.updatedUser = null;
        this.errorCode = errorCode;
    }
    
    /**
     * Gets the operation success status
     * @return true if operation was successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Sets the operation success status
     * @param success true if operation was successful, false otherwise
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    /**
     * Gets the response message
     * Messages match COBOL patterns from COUSR02C.cbl:
     * - Success: "User {userid} has been updated ..."
     * - Error: "User ID can NOT be empty...", "User ID NOT found...", etc.
     * @return Response message string
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Sets the response message
     * @param message Response message string
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * Gets the updated user data
     * @return UpdatedUserData object containing user profile information
     */
    public UpdatedUserData getUpdatedUser() {
        return updatedUser;
    }
    
    /**
     * Sets the updated user data
     * @param updatedUser UpdatedUserData object containing user profile information
     */
    public void setUpdatedUser(UpdatedUserData updatedUser) {
        this.updatedUser = updatedUser;
    }
    
    /**
     * Gets the COBOL-equivalent error code
     * Standard CICS response codes:
     * - "NORMAL": Successful operation (DFHRESP(NORMAL))
     * - "NOTFND": User not found (DFHRESP(NOTFND))  
     * - "OTHER": General error condition (DFHRESP(OTHER))
     * - "VALIDATION_ERROR": Field validation failure
     * - "NO_MODIFICATIONS": No changes detected for update
     * @return Error code string
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Sets the COBOL-equivalent error code
     * @param errorCode Error code string
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    /**
     * Inner class representing updated user data structure
     * Maps to SEC-USER-DATA structure from CSUSR01Y.cpy copybook
     */
    public static class UpdatedUserData {
        
        /**
         * User ID (SEC-USR-ID) - 8 characters
         */
        private String userId;
        
        /**
         * First Name (SEC-USR-FNAME) - 20 characters
         */
        private String firstName;
        
        /**
         * Last Name (SEC-USR-LNAME) - 20 characters  
         */
        private String lastName;
        
        /**
         * User Type (SEC-USR-TYPE) - 1 character
         * Typically 'R' for Regular user, 'A' for Administrator
         */
        private String userType;
        
        /**
         * Default constructor
         */
        public UpdatedUserData() {
        }
        
        /**
         * Constructor with all fields
         * @param userId User ID (8 chars max)
         * @param firstName First Name (20 chars max)
         * @param lastName Last Name (20 chars max)
         * @param userType User Type (1 char)
         */
        public UpdatedUserData(String userId, String firstName, String lastName, String userType) {
            this.userId = userId;
            this.firstName = firstName;
            this.lastName = lastName;
            this.userType = userType;
        }
        
        /**
         * Gets the user ID
         * @return User ID string (8 characters max)
         */
        public String getUserId() {
            return userId;
        }
        
        /**
         * Sets the user ID
         * @param userId User ID string (8 characters max)
         */
        public void setUserId(String userId) {
            this.userId = userId;
        }
        
        /**
         * Gets the first name
         * @return First name string (20 characters max)
         */
        public String getFirstName() {
            return firstName;
        }
        
        /**
         * Sets the first name
         * @param firstName First name string (20 characters max)
         */
        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }
        
        /**
         * Gets the last name
         * @return Last name string (20 characters max)
         */
        public String getLastName() {
            return lastName;
        }
        
        /**
         * Sets the last name
         * @param lastName Last name string (20 characters max)
         */
        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
        
        /**
         * Gets the user type
         * @return User type string (1 character)
         */
        public String getUserType() {
            return userType;
        }
        
        /**
         * Sets the user type
         * @param userType User type string (1 character)
         */
        public void setUserType(String userType) {
            this.userType = userType;
        }
        
        @Override
        public String toString() {
            return "UpdatedUserData{" +
                    "userId='" + userId + '\'' +
                    ", firstName='" + firstName + '\'' +
                    ", lastName='" + lastName + '\'' +
                    ", userType='" + userType + '\'' +
                    '}';
        }
    }
    
    @Override
    public String toString() {
        return "UserUpdateResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", updatedUser=" + updatedUser +
                ", errorCode='" + errorCode + '\'' +
                '}';
    }
}