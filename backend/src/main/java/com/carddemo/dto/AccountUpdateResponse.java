package com.carddemo.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Response DTO for account update operations containing success status, validation errors, 
 * updated account data, and audit information. Maps to COBOL COMMAREA response structure 
 * from COACTUPC processing results.
 * 
 * This class provides a comprehensive response structure for account update operations,
 * mirroring the COBOL COACTUPC program's response patterns while providing modern
 * structured error reporting and audit trail capabilities.
 * 
 * The response includes:
 * - Success/failure status matching COBOL program execution results
 * - Detailed validation errors with field-level error reporting
 * - Updated account data reflecting successful changes
 * - Comprehensive audit information for change tracking
 * - Error messages matching original COBOL error handling patterns
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 */
public class AccountUpdateResponse {

    /**
     * Indicates whether the account update operation was successful.
     * Maps to successful execution of COACTUPC COBOL program logic.
     */
    private boolean success;

    /**
     * List of field-level validation errors encountered during the update operation.
     * Each error contains field name, error code, message, and suggested correction
     * to support detailed error reporting matching BMS field highlighting patterns.
     */
    private List<ValidationError> validationErrors;

    /**
     * Updated account data returned after successful processing.
     * Contains the complete account information as it exists after the update,
     * matching the structure returned by COACTUPC COBOL program.
     */
    private Map<String, Object> updatedAccount;

    /**
     * Audit information tracking the update operation.
     * Includes timestamps, user identification, and change tracking data
     * to maintain comprehensive audit trail for account modifications.
     */
    private Map<String, Object> auditInfo;

    /**
     * General error message for operation-level failures.
     * Provides high-level error description when the operation fails,
     * complementing the detailed field-level validation errors.
     */
    private String errorMessage;

    /**
     * Response timestamp indicating when the response was generated
     */
    private LocalDateTime responseTimestamp;

    /**
     * Transaction identifier for tracking this specific update operation
     */
    private String transactionId;

    /**
     * Default constructor initializing collections and timestamp
     */
    public AccountUpdateResponse() {
        this.success = false;
        this.validationErrors = new ArrayList<>();
        this.updatedAccount = new HashMap<>();
        this.auditInfo = new HashMap<>();
        this.responseTimestamp = LocalDateTime.now();
    }

    /**
     * Constructor for successful account update response
     * 
     * @param updatedAccount the updated account data
     * @param auditInfo the audit information for the update
     */
    public AccountUpdateResponse(Map<String, Object> updatedAccount, Map<String, Object> auditInfo) {
        this();
        this.success = true;
        this.updatedAccount = updatedAccount != null ? new HashMap<>(updatedAccount) : new HashMap<>();
        this.auditInfo = auditInfo != null ? new HashMap<>(auditInfo) : new HashMap<>();
    }

    /**
     * Constructor for failed account update response
     * 
     * @param errorMessage the general error message
     * @param validationErrors the list of validation errors
     */
    public AccountUpdateResponse(String errorMessage, List<ValidationError> validationErrors) {
        this();
        this.success = false;
        this.errorMessage = errorMessage;
        this.validationErrors = validationErrors != null ? new ArrayList<>(validationErrors) : new ArrayList<>();
        // Set updatedAccount and auditInfo to null for error responses
        this.updatedAccount = null;
        this.auditInfo = null;
    }

    /**
     * Constructor for failed account update response with single error message
     * 
     * @param errorMessage the general error message
     */
    public AccountUpdateResponse(String errorMessage) {
        this(errorMessage, null);
    }

    /**
     * Checks if the account update operation was successful.
     * Returns true if the update completed without errors, false otherwise.
     * 
     * @return true if the update was successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Sets the success status of the account update operation
     * 
     * @param success true if the update was successful, false otherwise
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Gets the list of field-level validation errors encountered during the update operation.
     * Each ValidationError object contains detailed information about field-specific
     * validation failures, including field name, error code, message, and suggested correction.
     * 
     * @return the list of validation errors, empty list if no validation errors occurred
     */
    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

    /**
     * Sets the list of validation errors for this response
     * 
     * @param validationErrors the list of validation errors
     */
    public void setValidationErrors(List<ValidationError> validationErrors) {
        this.validationErrors = validationErrors != null ? validationErrors : new ArrayList<>();
    }

    /**
     * Gets the updated account data returned after successful processing.
     * Contains the complete account information as it exists after the update,
     * including account details, customer information, and related data.
     * 
     * The structure matches the data returned by the COACTUPC COBOL program
     * after successful account update processing.
     * 
     * @return the updated account data as a map, empty map if update failed
     */
    public Map<String, Object> getUpdatedAccount() {
        return updatedAccount;
    }

    /**
     * Sets the updated account data for this response
     * 
     * @param updatedAccount the updated account data
     */
    public void setUpdatedAccount(Map<String, Object> updatedAccount) {
        this.updatedAccount = updatedAccount != null ? updatedAccount : new HashMap<>();
    }

    /**
     * Gets the audit information tracking the update operation.
     * Contains comprehensive audit trail data including:
     * - Update timestamp
     * - User identification (who performed the update)
     * - Before and after values for changed fields
     * - Transaction identifiers
     * - System information (hostname, session details)
     * 
     * This information supports compliance requirements and change tracking
     * equivalent to the audit capabilities of the original COBOL system.
     * 
     * @return the audit information as a map, empty map if no audit data available
     */
    public Map<String, Object> getAuditInfo() {
        return auditInfo;
    }

    /**
     * Sets the audit information for this response
     * 
     * @param auditInfo the audit information
     */
    public void setAuditInfo(Map<String, Object> auditInfo) {
        this.auditInfo = auditInfo != null ? auditInfo : new HashMap<>();
    }

    /**
     * Gets the general error message for operation-level failures.
     * Provides a high-level description of what went wrong when the operation fails,
     * complementing the detailed field-level validation errors.
     * 
     * This message corresponds to the error handling patterns used in the
     * COACTUPC COBOL program for system-level or business rule failures.
     * 
     * @return the error message, null if no general error occurred
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the general error message for this response
     * 
     * @param errorMessage the error message
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Gets the response timestamp
     * 
     * @return the timestamp when this response was generated
     */
    public LocalDateTime getResponseTimestamp() {
        return responseTimestamp;
    }

    /**
     * Sets the response timestamp
     * 
     * @param responseTimestamp the response timestamp
     */
    public void setResponseTimestamp(LocalDateTime responseTimestamp) {
        this.responseTimestamp = responseTimestamp;
    }

    /**
     * Gets the transaction identifier for this update operation
     * 
     * @return the transaction identifier
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Sets the transaction identifier for this update operation
     * 
     * @param transactionId the transaction identifier
     */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Checks if the response has validation errors
     * 
     * @return true if there are validation errors, false otherwise
     */
    public boolean hasValidationErrors() {
        return validationErrors != null && !validationErrors.isEmpty();
    }

    /**
     * Checks if the response has audit information
     * 
     * @return true if audit information is available, false otherwise
     */
    public boolean hasAuditInfo() {
        return auditInfo != null && !auditInfo.isEmpty();
    }

    /**
     * Checks if the response has updated account data
     * 
     * @return true if updated account data is available, false otherwise
     */
    public boolean hasUpdatedAccount() {
        return updatedAccount != null && !updatedAccount.isEmpty();
    }

    /**
     * Adds a validation error to the response
     * 
     * @param error the validation error to add
     */
    public void addValidationError(ValidationError error) {
        if (error != null) {
            if (this.validationErrors == null) {
                this.validationErrors = new ArrayList<>();
            }
            this.validationErrors.add(error);
            // Adding validation errors indicates operation failure
            this.success = false;
        }
    }

    /**
     * Adds audit information to the response
     * 
     * @param key the audit information key
     * @param value the audit information value
     */
    public void addAuditInfo(String key, Object value) {
        if (key != null && value != null) {
            if (this.auditInfo == null) {
                this.auditInfo = new HashMap<>();
            }
            this.auditInfo.put(key, value);
        }
    }

    /**
     * Adds updated account information to the response
     * 
     * @param key the account data key
     * @param value the account data value
     */
    public void addUpdatedAccountInfo(String key, Object value) {
        if (key != null && value != null) {
            if (this.updatedAccount == null) {
                this.updatedAccount = new HashMap<>();
            }
            this.updatedAccount.put(key, value);
        }
    }

    /**
     * Gets a summary of the response for logging purposes
     * 
     * @return a string summary of the response
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("AccountUpdateResponse{");
        summary.append("success=").append(success);
        
        if (hasValidationErrors()) {
            summary.append(", validationErrors=").append(validationErrors.size()).append(" errors");
        }
        
        if (hasUpdatedAccount()) {
            summary.append(", updatedAccount=").append(updatedAccount.size()).append(" fields");
        }
        
        if (errorMessage != null) {
            summary.append(", errorMessage='").append(errorMessage).append("'");
        }
        
        if (transactionId != null) {
            summary.append(", transactionId='").append(transactionId).append("'");
        }
        
        summary.append(", timestamp=").append(responseTimestamp);
        summary.append("}");
        
        return summary.toString();
    }

    @Override
    public String toString() {
        return "AccountUpdateResponse{" +
                "success=" + success +
                ", validationErrors=" + validationErrors +
                ", updatedAccount=" + updatedAccount +
                ", auditInfo=" + auditInfo +
                ", errorMessage='" + errorMessage + '\'' +
                ", responseTimestamp=" + responseTimestamp +
                ", transactionId='" + transactionId + '\'' +
                '}';
    }
}