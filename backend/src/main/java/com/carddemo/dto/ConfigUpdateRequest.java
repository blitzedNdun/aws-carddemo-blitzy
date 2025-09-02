package com.carddemo.dto;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request DTO for system configuration updates in AdminConfigService.
 * 
 * Contains configuration parameters, validation rules, and audit information 
 * for admin-initiated configuration changes. Supports structured validation 
 * and error handling for configuration management operations.
 * 
 * This DTO preserves configuration versioning and rollback logic from the 
 * original COBOL parameter validation routines while mapping to Spring 
 * validation framework.
 * 
 * @author Blitzy agent
 * @version 1.0
 */
public class ConfigUpdateRequest {

    /**
     * Configuration key identifier - maps to COBOL parameter name field.
     * Constrained to match original COBOL field definition length.
     */
    @Size(min = 1, max = 50, message = "Configuration key must be between 1 and 50 characters")
    private String configKey;

    /**
     * Configuration value - supports both string and numeric values.
     * Uses Object type to accommodate different value types including BigDecimal
     * for financial parameters requiring COBOL-compatible precision.
     */
    private Object configValue;

    /**
     * Description of the configuration parameter.
     * Maps to COBOL parameter description field with length validation.
     */
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    /**
     * Validation rule specification for the configuration value.
     * Contains business rules and constraints that must be applied.
     */
    @Size(max = 500, message = "Validation rule cannot exceed 500 characters")
    private String validationRule;

    /**
     * Reason for the configuration change - required for audit trail.
     * Maps to COBOL change reason field for compliance tracking.
     */
    @Size(min = 1, max = 255, message = "Change reason must be between 1 and 255 characters")
    private String changeReason;

    /**
     * Admin user ID who initiated the configuration change.
     * Required for audit entries and security tracking.
     */
    @Size(min = 1, max = 50, message = "Admin user ID must be between 1 and 50 characters")
    private String adminUserId;

    /**
     * Default constructor required for Spring framework instantiation.
     */
    public ConfigUpdateRequest() {
        // Initialize for framework compatibility
    }

    /**
     * Constructor for creating configuration update requests with all required fields.
     * 
     * @param configKey Configuration key identifier
     * @param configValue Configuration value (String, BigDecimal, or other types)
     * @param description Description of the configuration parameter
     * @param validationRule Validation rule specification
     * @param changeReason Reason for the configuration change
     * @param adminUserId Admin user ID initiating the change
     */
    public ConfigUpdateRequest(String configKey, Object configValue, String description, 
                             String validationRule, String changeReason, String adminUserId) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.description = description;
        this.validationRule = validationRule;
        this.changeReason = changeReason;
        this.adminUserId = adminUserId;
    }

    /**
     * Gets the configuration key identifier.
     * 
     * @return Configuration key string, matching COBOL parameter name format
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * Sets the configuration key identifier.
     * Validates key format matches COBOL parameter naming conventions.
     * 
     * @param configKey Configuration key to set
     */
    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    /**
     * Gets the configuration value.
     * Returns Object type to support various value types including:
     * - String values for text configurations
     * - BigDecimal values for financial parameters with COBOL precision
     * - Integer values for numeric settings
     * 
     * @return Configuration value object
     */
    public Object getConfigValue() {
        return configValue;
    }

    /**
     * Sets the configuration value.
     * Supports multiple value types to accommodate different configuration needs.
     * Financial values should use BigDecimal for COBOL COMP-3 precision compatibility.
     * 
     * @param configValue Configuration value to set
     */
    public void setConfigValue(Object configValue) {
        this.configValue = configValue;
    }

    /**
     * Gets the configuration parameter description.
     * 
     * @return Description string explaining the configuration parameter
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the configuration parameter description.
     * Used for documentation and user interface display.
     * 
     * @param description Description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the validation rule specification.
     * Contains business rules that must be applied to the configuration value.
     * 
     * @return Validation rule string specification
     */
    public String getValidationRule() {
        return validationRule;
    }

    /**
     * Sets the validation rule specification.
     * Defines constraints and business rules for value validation.
     * Maps to COBOL parameter validation routines.
     * 
     * @param validationRule Validation rule to set
     */
    public void setValidationRule(String validationRule) {
        this.validationRule = validationRule;
    }

    /**
     * Gets the reason for the configuration change.
     * Required for audit trail and compliance tracking.
     * 
     * @return Change reason string
     */
    public String getChangeReason() {
        return changeReason;
    }

    /**
     * Sets the reason for the configuration change.
     * Mandatory field for creating audit entries and tracking changes.
     * 
     * @param changeReason Change reason to set
     */
    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    /**
     * Gets the admin user ID who initiated the change.
     * Used for security tracking and audit purposes.
     * 
     * @return Admin user ID string
     */
    public String getAdminUserId() {
        return adminUserId;
    }

    /**
     * Sets the admin user ID who initiated the change.
     * Required for audit entries and security compliance.
     * 
     * @param adminUserId Admin user ID to set
     */
    public void setAdminUserId(String adminUserId) {
        this.adminUserId = adminUserId;
    }

    /**
     * Validates if this configuration update request contains a financial value.
     * Financial values should be represented as BigDecimal for COBOL precision compatibility.
     * 
     * @return true if configValue is a BigDecimal (financial value)
     */
    public boolean isFinancialValue() {
        return configValue instanceof BigDecimal;
    }

    /**
     * Gets the configuration value as BigDecimal for financial calculations.
     * Ensures COBOL COMP-3 precision compatibility for monetary values.
     * 
     * @return BigDecimal value if configValue is numeric, null otherwise
     * @throws ClassCastException if configValue is not a BigDecimal
     */
    public BigDecimal getFinancialValue() {
        if (configValue instanceof BigDecimal) {
            return (BigDecimal) configValue;
        }
        return null;
    }

    /**
     * Sets a financial configuration value with proper BigDecimal precision.
     * Ensures COBOL COMP-3 compatibility for monetary calculations.
     * 
     * @param financialValue BigDecimal value with appropriate scale and precision
     */
    public void setFinancialValue(BigDecimal financialValue) {
        this.configValue = financialValue;
    }

    /**
     * Gets the configuration value as String for text-based configurations.
     * 
     * @return String representation of the configuration value
     */
    public String getStringValue() {
        return configValue != null ? configValue.toString() : null;
    }

    /**
     * Validates if all required fields for audit tracking are present.
     * Used by AdminConfigService to ensure complete audit trail.
     * 
     * @return true if all audit fields (changeReason, adminUserId) are populated
     */
    public boolean hasCompleteAuditInfo() {
        return changeReason != null && !changeReason.trim().isEmpty() &&
               adminUserId != null && !adminUserId.trim().isEmpty();
    }

    /**
     * Validates if the configuration request has all required core fields.
     * 
     * @return true if configKey and configValue are both present
     */
    public boolean isValid() {
        return configKey != null && !configKey.trim().isEmpty() && 
               configValue != null;
    }

    /**
     * Creates a copy of this configuration update request for versioning support.
     * Supports configuration rollback scenarios by preserving previous values.
     * 
     * @return New ConfigUpdateRequest instance with identical field values
     */
    public ConfigUpdateRequest createCopy() {
        ConfigUpdateRequest copy = new ConfigUpdateRequest();
        copy.configKey = this.configKey;
        copy.configValue = this.configValue;
        copy.description = this.description;
        copy.validationRule = this.validationRule;
        copy.changeReason = this.changeReason;
        copy.adminUserId = this.adminUserId;
        return copy;
    }

    /**
     * String representation for debugging and logging purposes.
     * Excludes sensitive configuration values from output.
     * 
     * @return String representation with key configuration details
     */
    @Override
    public String toString() {
        return "ConfigUpdateRequest{" +
                "configKey='" + configKey + '\'' +
                ", description='" + description + '\'' +
                ", validationRule='" + validationRule + '\'' +
                ", changeReason='" + changeReason + '\'' +
                ", adminUserId='" + adminUserId + '\'' +
                ", hasValue=" + (configValue != null) +
                ", isFinancial=" + isFinancialValue() +
                '}';
    }

    /**
     * Equality comparison based on configuration key and value.
     * Used for detecting duplicate configuration updates.
     * 
     * @param obj Object to compare with
     * @return true if objects represent the same configuration update
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ConfigUpdateRequest that = (ConfigUpdateRequest) obj;
        
        if (configKey != null ? !configKey.equals(that.configKey) : that.configKey != null) {
            return false;
        }
        return configValue != null ? configValue.equals(that.configValue) : that.configValue == null;
    }

    /**
     * Hash code based on configuration key and value.
     * Ensures consistent behavior in collections and maps.
     * 
     * @return Hash code for this configuration update request
     */
    @Override
    public int hashCode() {
        int result = configKey != null ? configKey.hashCode() : 0;
        result = 31 * result + (configValue != null ? configValue.hashCode() : 0);
        return result;
    }
}