/*
 * UserAddResponse.java
 * 
 * DTO for user creation responses containing operation status, created user details,
 * and generated credentials. Returns confirmation data from COUSR03C-equivalent 
 * service logic with success/error indicators and audit information.
 * 
 * This class replaces the COBOL COMMAREA response structure for user creation
 * operations, providing REST API-compatible JSON response format while maintaining
 * identical business logic and data validation patterns from the original mainframe
 * implementation.
 * 
 * Implements comprehensive error handling and audit trail requirements for
 * regulatory compliance and operational monitoring in the modernized Spring Boot
 * architecture.
 */
package com.carddemo.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for user creation operations in the Card Demo application.
 * 
 * This DTO encapsulates the complete response information for user creation
 * requests, including operation status, created user details, generated
 * credentials, error information, and audit metadata.
 * 
 * The structure maps to the original BMS screen outputs and COBOL program
 * response patterns while providing modern JSON serialization capabilities
 * for REST API integration.
 * 
 * Key Features:
 * - Operation success/failure indication
 * - Complete user details for created accounts
 * - Auto-generated secure password information
 * - Comprehensive error reporting
 * - Audit trail with precise timestamps
 * - Builder pattern for flexible object construction
 * 
 * Usage Example:
 * <pre>
 * UserAddResponse response = UserAddResponse.builder()
 *     .success(true)
 *     .userId("ADMIN001")
 *     .generatedPassword("TempPwd123")
 *     .message("User created successfully")
 *     .timestamp(LocalDateTime.now())
 *     .build();
 * </pre>
 * 
 * @author CardDemo Application
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddResponse {
    
    /**
     * Indicates whether the user creation operation was successful.
     * 
     * This field provides a boolean flag for quick determination of operation
     * outcome, enabling client applications to branch logic based on success
     * or failure without parsing detailed error messages.
     * 
     * @return true if user was created successfully, false otherwise
     */
    @JsonProperty("success")
    private boolean success;
    
    /**
     * The unique identifier for the newly created user.
     * 
     * Contains the 8-character user ID that was assigned to the new user account.
     * This corresponds to the SEC-USR-ID field from the original COBOL structure
     * (CSUSR01Y.cpy) and serves as the primary key for user authentication and
     * authorization operations.
     * 
     * @return the user ID string, or null if creation failed
     */
    @JsonProperty("userId")
    private String userId;
    
    /**
     * The auto-generated temporary password for the new user.
     * 
     * Contains the system-generated initial password that the user must use
     * for first login. This password should be changed during the initial
     * login session for security compliance. Maps to the SEC-USR-PWD field
     * from the original COBOL structure.
     * 
     * @return the generated password string, or null if creation failed
     */
    @JsonProperty("generatedPassword")
    private String generatedPassword;
    
    /**
     * Human-readable message describing the operation result.
     * 
     * Provides detailed information about the operation outcome, including
     * success confirmations or specific error descriptions. This message
     * is suitable for display to end users and corresponds to the message
     * fields used in the original BMS screen responses.
     * 
     * @return descriptive message about the operation result
     */
    @JsonProperty("message")
    private String message;
    
    /**
     * Specific error code for failed operations.
     * 
     * When the operation fails, this field contains a standardized error code
     * that can be used for programmatic error handling and logging. Error codes
     * follow the same classification system used in the original COBOL
     * implementation for consistency with existing error handling procedures.
     * 
     * @return error code string, or null for successful operations
     */
    @JsonProperty("errorCode")
    private String errorCode;
    
    /**
     * Precise timestamp of the operation execution.
     * 
     * Records the exact date and time when the user creation operation was
     * processed, providing audit trail information for compliance and
     * monitoring purposes. This timestamp corresponds to the date/time
     * fields used in the original COBOL program header information.
     * 
     * @return LocalDateTime object representing the operation timestamp
     */
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    /**
     * Indicates whether the operation completed successfully.
     * 
     * This method provides a convenient way to check operation success status
     * and is equivalent to checking the success field directly. Included for
     * compatibility with standard success checking patterns.
     * 
     * @return true if the user creation was successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Sets the success status of the operation.
     * 
     * @param success true if the operation was successful, false otherwise
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    /**
     * Gets the unique identifier for the created user.
     * 
     * @return the user ID string, or null if creation failed
     */
    public String getUserId() {
        return userId;
    }
    
    /**
     * Sets the user identifier for the created user.
     * 
     * @param userId the unique user identifier (typically 8 characters)
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    /**
     * Gets the auto-generated password for the new user.
     * 
     * @return the generated password string, or null if creation failed
     */
    public String getGeneratedPassword() {
        return generatedPassword;
    }
    
    /**
     * Sets the auto-generated password for the new user.
     * 
     * @param generatedPassword the system-generated temporary password
     */
    public void setGeneratedPassword(String generatedPassword) {
        this.generatedPassword = generatedPassword;
    }
    
    /**
     * Gets the descriptive message about the operation result.
     * 
     * @return human-readable message describing the operation outcome
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Sets the descriptive message about the operation result.
     * 
     * @param message human-readable description of the operation outcome
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * Gets the error code for failed operations.
     * 
     * @return error code string, or null for successful operations
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Sets the error code for failed operations.
     * 
     * @param errorCode standardized error code for programmatic handling
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    /**
     * Gets the timestamp of the operation execution.
     * 
     * @return LocalDateTime object representing when the operation was processed
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Sets the timestamp of the operation execution.
     * 
     * @param timestamp the date and time when the operation was processed
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Creates a new UserAddResponse builder instance.
     * 
     * This static method provides access to the Lombok-generated builder pattern
     * for flexible and readable object construction. The builder pattern is
     * particularly useful when not all fields need to be set or when conditional
     * field setting is required based on operation outcomes.
     * 
     * @return a new UserAddResponseBuilder instance
     */
    public static UserAddResponseBuilder builder() {
        return new UserAddResponseBuilder();
    }
    
    /**
     * Compares this UserAddResponse with another object for equality.
     * 
     * Two UserAddResponse objects are considered equal if all their fields
     * have the same values. This method is generated by Lombok and provides
     * comprehensive equality checking for all fields.
     * 
     * @param obj the object to compare with this UserAddResponse
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        UserAddResponse that = (UserAddResponse) obj;
        
        if (success != that.success) return false;
        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        if (generatedPassword != null ? !generatedPassword.equals(that.generatedPassword) : that.generatedPassword != null) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        if (errorCode != null ? !errorCode.equals(that.errorCode) : that.errorCode != null) return false;
        return timestamp != null ? timestamp.equals(that.timestamp) : that.timestamp == null;
    }
    
    /**
     * Generates a hash code for this UserAddResponse.
     * 
     * The hash code is computed based on all fields in the object and provides
     * consistent hash values for objects that are equal according to the equals()
     * method. This method is generated by Lombok.
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        int result = (success ? 1 : 0);
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (generatedPassword != null ? generatedPassword.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (errorCode != null ? errorCode.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }
    
    /**
     * Returns a string representation of this UserAddResponse.
     * 
     * The string includes all field values in a readable format, useful for
     * debugging and logging purposes. This method is generated by Lombok and
     * provides comprehensive object state representation.
     * 
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "UserAddResponse{" +
                "success=" + success +
                ", userId='" + userId + '\'' +
                ", generatedPassword='" + (generatedPassword != null ? "[PROTECTED]" : null) + '\'' +
                ", message='" + message + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}