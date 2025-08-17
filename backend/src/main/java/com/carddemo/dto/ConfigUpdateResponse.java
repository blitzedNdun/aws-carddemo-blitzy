package com.carddemo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for system configuration updates in AdminConfigService.
 * 
 * This class provides a structured response format for configuration management 
 * operations, including success status, audit information, validation results,
 * and rollback support. It captures all necessary details for configuration
 * change tracking and provides comprehensive audit trail functionality.
 * 
 * The DTO supports both string-based and BigDecimal-based configuration values
 * to handle various parameter types including financial configurations that
 * require COBOL-compatible precision.
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 */
public class ConfigUpdateResponse {

    /**
     * Indicates whether the configuration update operation was successful
     */
    private boolean success;

    /**
     * The configuration key that was updated
     */
    private String configKey;

    /**
     * The previous value of the configuration parameter
     */
    private Object oldValue;

    /**
     * The new value of the configuration parameter after update
     */
    private Object newValue;

    /**
     * Timestamp when the configuration update was performed
     */
    private LocalDateTime updateTimestamp;

    /**
     * Version number of the configuration after the update
     */
    private Long versionNumber;

    /**
     * List of validation errors encountered during the update operation
     */
    private List<String> validationErrors;

    /**
     * Unique audit identifier for tracking this configuration change
     */
    private String auditId;

    /**
     * Indicates whether rollback is supported for this configuration change
     */
    private boolean rollbackSupported;

    /**
     * Default constructor for ConfigUpdateResponse
     */
    public ConfigUpdateResponse() {
        this.success = false;
        this.rollbackSupported = true; // Default to supporting rollback
        this.updateTimestamp = LocalDateTime.now();
    }

    /**
     * Constructor with basic configuration update information
     * 
     * @param success whether the update was successful
     * @param configKey the configuration key that was updated
     * @param oldValue the previous value
     * @param newValue the new value after update
     */
    public ConfigUpdateResponse(boolean success, String configKey, Object oldValue, Object newValue) {
        this();
        this.success = success;
        this.configKey = configKey;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /**
     * Constructor with complete audit information
     * 
     * @param success whether the update was successful
     * @param configKey the configuration key that was updated
     * @param oldValue the previous value
     * @param newValue the new value after update
     * @param auditId unique audit identifier
     * @param versionNumber version number after update
     */
    public ConfigUpdateResponse(boolean success, String configKey, Object oldValue, 
                               Object newValue, String auditId, Long versionNumber) {
        this(success, configKey, oldValue, newValue);
        this.auditId = auditId;
        this.versionNumber = versionNumber;
    }

    /**
     * Checks if the configuration update operation was successful
     * 
     * @return true if the update was successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Sets the success status of the configuration update operation
     * 
     * @param success true if the update was successful, false otherwise
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Gets the configuration key that was updated
     * 
     * @return the configuration key
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * Sets the configuration key that was updated
     * 
     * @param configKey the configuration key
     */
    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    /**
     * Gets the previous value of the configuration parameter
     * 
     * @return the old configuration value (String, BigDecimal, or other type)
     */
    public Object getOldValue() {
        return oldValue;
    }

    /**
     * Sets the previous value of the configuration parameter
     * 
     * @param oldValue the old configuration value
     */
    public void setOldValue(Object oldValue) {
        this.oldValue = oldValue;
    }

    /**
     * Gets the new value of the configuration parameter after update
     * 
     * @return the new configuration value (String, BigDecimal, or other type)
     */
    public Object getNewValue() {
        return newValue;
    }

    /**
     * Sets the new value of the configuration parameter after update
     * 
     * @param newValue the new configuration value
     */
    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }

    /**
     * Gets the timestamp when the configuration update was performed
     * 
     * @return the update timestamp
     */
    public LocalDateTime getUpdateTimestamp() {
        return updateTimestamp;
    }

    /**
     * Sets the timestamp when the configuration update was performed
     * 
     * @param updateTimestamp the update timestamp
     */
    public void setUpdateTimestamp(LocalDateTime updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }

    /**
     * Gets the version number of the configuration after the update
     * 
     * @return the version number
     */
    public Long getVersionNumber() {
        return versionNumber;
    }

    /**
     * Sets the version number of the configuration after the update
     * 
     * @param versionNumber the version number
     */
    public void setVersionNumber(Long versionNumber) {
        this.versionNumber = versionNumber;
    }

    /**
     * Gets the list of validation errors encountered during the update operation
     * 
     * @return the list of validation error messages
     */
    public List<String> getValidationErrors() {
        return validationErrors;
    }

    /**
     * Sets the list of validation errors encountered during the update operation
     * 
     * @param validationErrors the list of validation error messages
     */
    public void setValidationErrors(List<String> validationErrors) {
        this.validationErrors = validationErrors;
    }

    /**
     * Gets the unique audit identifier for tracking this configuration change
     * 
     * @return the audit identifier
     */
    public String getAuditId() {
        return auditId;
    }

    /**
     * Sets the unique audit identifier for tracking this configuration change
     * 
     * @param auditId the audit identifier
     */
    public void setAuditId(String auditId) {
        this.auditId = auditId;
    }

    /**
     * Checks if rollback is supported for this configuration change
     * 
     * @return true if rollback is supported, false otherwise
     */
    public boolean getRollbackSupported() {
        return rollbackSupported;
    }

    /**
     * Sets whether rollback is supported for this configuration change
     * 
     * @param rollbackSupported true if rollback is supported, false otherwise
     */
    public void setRollbackSupported(boolean rollbackSupported) {
        this.rollbackSupported = rollbackSupported;
    }

    /**
     * Checks if the configuration update has validation errors
     * 
     * @return true if there are validation errors, false otherwise
     */
    public boolean hasValidationErrors() {
        return validationErrors != null && !validationErrors.isEmpty();
    }

    /**
     * Checks if the configuration value involves financial data (BigDecimal)
     * 
     * @return true if either old or new value is a BigDecimal, false otherwise
     */
    public boolean isFinancialConfiguration() {
        return (oldValue instanceof BigDecimal) || (newValue instanceof BigDecimal);
    }

    /**
     * Gets the configuration change summary as a readable string
     * 
     * @return summary of the configuration change
     */
    public String getChangeSummary() {
        if (configKey == null) {
            return "Configuration update - key not specified";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("Configuration '").append(configKey).append("'");
        
        if (success) {
            summary.append(" updated successfully");
        } else {
            summary.append(" update failed");
        }
        
        if (oldValue != null && newValue != null) {
            summary.append(" (").append(oldValue).append(" → ").append(newValue).append(")");
        }
        
        return summary.toString();
    }

    @Override
    public String toString() {
        return "ConfigUpdateResponse{" +
                "success=" + success +
                ", configKey='" + configKey + '\'' +
                ", oldValue=" + oldValue +
                ", newValue=" + newValue +
                ", updateTimestamp=" + updateTimestamp +
                ", versionNumber=" + versionNumber +
                ", validationErrors=" + validationErrors +
                ", auditId='" + auditId + '\'' +
                ", rollbackSupported=" + rollbackSupported +
                '}';
    }
}