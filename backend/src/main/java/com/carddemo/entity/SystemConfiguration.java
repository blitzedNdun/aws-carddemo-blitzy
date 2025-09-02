package com.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity representing system configuration parameters including business rules,
 * interest rates, fee schedules, and processing limits. Maps VSAM parameter file 
 * to PostgreSQL table with audit trail support and version control for configuration
 * rollback capabilities.
 * 
 * This entity supports the modernized Spring Boot configuration management system,
 * replacing mainframe VSAM configuration datasets with PostgreSQL-backed storage
 * while preserving identical configuration versioning and audit trail functionality.
 * 
 * Configuration categories include:
 * - INTEREST_RATES: Interest rate parameters for account calculations
 * - FEE_SCHEDULE: Fee amounts and calculation parameters  
 * - PROCESSING_LIMITS: Transaction and batch processing thresholds
 * - BUSINESS_RULES: Business logic configuration parameters
 * - VALIDATION_RULES: Data validation patterns and constraints
 * 
 * Each configuration parameter supports versioning through sequence numbers,
 * enabling rollback to previous values while maintaining a complete audit trail
 * of all configuration changes for regulatory compliance.
 */
@Entity
@Table(
    name = "system_configuration",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_system_config_category_key_version",
            columnNames = {"config_category", "config_key", "version_number"}
        )
    }
)
@SequenceGenerator(
    name = "system_config_seq",
    sequenceName = "system_configuration_seq",
    allocationSize = 1
)
public class SystemConfiguration {

    /**
     * Primary key identifier for system configuration records.
     * Generated using PostgreSQL sequence for optimal performance.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "system_config_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * Configuration category for logical grouping of related parameters.
     * Categories align with business functional areas and system components.
     * 
     * Standard categories:
     * - INTEREST_RATES: Interest calculation parameters
     * - FEE_SCHEDULE: Fee amounts and calculation rules
     * - PROCESSING_LIMITS: System processing thresholds
     * - BUSINESS_RULES: Business logic configuration
     * - VALIDATION_RULES: Data validation patterns
     */
    @NotBlank(message = "Configuration category is required")
    @Size(max = 50, message = "Configuration category must not exceed 50 characters")
    @Column(name = "config_category", length = 50, nullable = false)
    private String configCategory;

    /**
     * Unique configuration parameter key within the category.
     * Follows naming convention: CATEGORY_SPECIFIC_PARAMETER_NAME
     * 
     * Examples:
     * - INTEREST_RATE_PURCHASE_APR
     * - FEE_LATE_PAYMENT_AMOUNT
     * - LIMIT_DAILY_TRANSACTION_COUNT
     * - RULE_MINIMUM_PAYMENT_PERCENTAGE
     */
    @NotBlank(message = "Configuration key is required")
    @Size(max = 100, message = "Configuration key must not exceed 100 characters")
    @Column(name = "config_key", length = 100, nullable = false)
    private String configKey;

    /**
     * Configuration parameter value stored as string representation.
     * Actual data type interpretation based on dataType field.
     * 
     * For financial values, stores string representation of BigDecimal
     * to preserve exact precision matching COBOL COMP-3 packed decimal behavior.
     * 
     * Examples:
     * - "18.99" for interest rates (DECIMAL)
     * - "35.00" for fee amounts (DECIMAL)  
     * - "10000" for transaction limits (INTEGER)
     * - "true" for boolean flags (BOOLEAN)
     * - "2024-12-31" for date values (DATE)
     */
    @NotBlank(message = "Configuration value is required")
    @Size(max = 500, message = "Configuration value must not exceed 500 characters")
    @Column(name = "config_value", length = 500, nullable = false)
    private String configValue;

    /**
     * Data type indicator for proper value interpretation and validation.
     * Supports COBOL data type mappings for mainframe compatibility.
     * 
     * Supported types:
     * - STRING: Text values and descriptions
     * - DECIMAL: Financial amounts with precision (maps to BigDecimal)
     * - INTEGER: Whole number values (maps to Long)
     * - BOOLEAN: True/false flags (maps to Boolean)
     * - DATE: Date values (maps to LocalDate)
     * - PERCENTAGE: Percentage values with decimal precision
     */
    @NotBlank(message = "Data type is required")
    @Size(max = 20, message = "Data type must not exceed 20 characters")
    @Column(name = "data_type", length = 20, nullable = false)
    private String dataType;

    /**
     * Human-readable description of the configuration parameter.
     * Provides context for system administrators and developers.
     * Includes business purpose and impact of parameter changes.
     */
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * Default value for configuration parameter rollback scenarios.
     * Stores the original or system default value for recovery purposes.
     * Used when rolling back to previous configuration versions.
     */
    @Size(max = 500, message = "Default value must not exceed 500 characters")
    @Column(name = "default_value", length = 500)
    private String defaultValue;

    /**
     * Validation rule or pattern for configuration value validation.
     * Supports regular expressions, range constraints, and business rules.
     * 
     * Examples:
     * - "^\\d{1,2}\\.\\d{2}$" for percentage rates
     * - "MIN:0,MAX:999999.99" for monetary amounts
     * - "ENUM:Y,N" for flag values
     * - "DATE_FORMAT:YYYY-MM-DD" for date values
     */
    @Size(max = 500, message = "Validation rule must not exceed 500 characters")
    @Column(name = "validation_rule", length = 500)
    private String validationRule;

    /**
     * Version number for configuration parameter rollback capabilities.
     * Incremented on each configuration change to maintain history.
     * Enables rollback to specific versions for operational recovery.
     * 
     * Version 1 represents the initial configuration value.
     * Higher versions represent subsequent changes with audit trail.
     */
    @NotNull(message = "Version number is required")
    @Column(name = "version_number", nullable = false)
    private Long versionNumber = 1L;

    /**
     * Audit trail timestamp for configuration creation.
     * Automatically set when configuration record is first created.
     * Supports regulatory compliance and change tracking requirements.
     */
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    /**
     * Audit trail timestamp for configuration modifications.
     * Automatically updated when configuration record is modified.
     * Enables tracking of configuration change frequency and timing.
     */
    @Column(name = "last_modified_date", nullable = false)
    private LocalDateTime lastModifiedDate;

    /**
     * Default constructor for JPA entity instantiation.
     * Initializes version number to 1 for new configuration parameters.
     */
    public SystemConfiguration() {
        this.versionNumber = 1L;
    }

    /**
     * Constructor for creating new configuration parameters with all required fields.
     * 
     * @param configCategory the configuration category for logical grouping
     * @param configKey the unique configuration parameter key
     * @param configValue the configuration parameter value
     * @param dataType the data type for value interpretation
     * @param description the human-readable description
     */
    public SystemConfiguration(String configCategory, String configKey, String configValue, 
                             String dataType, String description) {
        this.configCategory = configCategory;
        this.configKey = configKey;
        this.configValue = configValue;
        this.dataType = dataType;
        this.description = description;
        this.defaultValue = configValue; // Set initial value as default
        this.versionNumber = 1L;
    }

    /**
     * JPA lifecycle method to set creation timestamp before entity persistence.
     * Automatically called by JPA provider during INSERT operations.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdDate = now;
        this.lastModifiedDate = now;
    }

    /**
     * JPA lifecycle method to update modification timestamp before entity updates.
     * Automatically called by JPA provider during UPDATE operations.
     */
    @PreUpdate
    protected void onUpdate() {
        this.lastModifiedDate = LocalDateTime.now();
    }

    // Getter methods for all fields

    /**
     * Gets the primary key identifier for this configuration record.
     * 
     * @return the unique identifier
     */
    public Long getId() {
        return id;
    }

    /**
     * Gets the configuration category for logical parameter grouping.
     * 
     * @return the configuration category
     */
    public String getConfigCategory() {
        return configCategory;
    }

    /**
     * Gets the unique configuration parameter key within the category.
     * 
     * @return the configuration key
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * Gets the configuration parameter value as string representation.
     * 
     * @return the configuration value
     */
    public String getConfigValue() {
        return configValue;
    }

    /**
     * Gets the data type indicator for value interpretation.
     * 
     * @return the data type
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * Gets the human-readable description of the configuration parameter.
     * 
     * @return the parameter description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the default value for rollback scenarios.
     * 
     * @return the default value
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Gets the validation rule or pattern for value validation.
     * 
     * @return the validation rule
     */
    public String getValidationRule() {
        return validationRule;
    }

    /**
     * Gets the version number for rollback capabilities.
     * 
     * @return the version number
     */
    public Long getVersionNumber() {
        return versionNumber;
    }

    /**
     * Gets the creation timestamp for audit trail.
     * 
     * @return the creation date and time
     */
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Gets the last modification timestamp for audit trail.
     * 
     * @return the last modification date and time
     */
    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    // Setter methods for mutable fields

    /**
     * Sets the configuration parameter value with automatic version incrementing.
     * Increments version number to support rollback capabilities.
     * 
     * @param configValue the new configuration value
     */
    public void setConfigValue(String configValue) {
        if (this.configValue != null && !this.configValue.equals(configValue)) {
            this.versionNumber++;
        }
        this.configValue = configValue;
    }

    /**
     * Sets the human-readable description of the configuration parameter.
     * 
     * @param description the parameter description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the validation rule or pattern for value validation.
     * 
     * @param validationRule the validation rule
     */
    public void setValidationRule(String validationRule) {
        this.validationRule = validationRule;
    }

    /**
     * Sets the default value for rollback scenarios.
     * 
     * @param defaultValue the default value
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Sets the version number for rollback capabilities.
     * 
     * @param versionNumber the version number
     */
    public void setVersionNumber(Long versionNumber) {
        this.versionNumber = versionNumber;
    }

    // Utility methods for common operations

    /**
     * Parses the configuration value as a BigDecimal for financial calculations.
     * Preserves exact precision matching COBOL COMP-3 packed decimal behavior.
     * 
     * @return the configuration value as BigDecimal
     * @throws NumberFormatException if value cannot be parsed as decimal
     */
    public BigDecimal getValueAsDecimal() {
        if (configValue == null || configValue.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(configValue.trim());
    }

    /**
     * Parses the configuration value as a Long for integer calculations.
     * 
     * @return the configuration value as Long
     * @throws NumberFormatException if value cannot be parsed as integer
     */
    public Long getValueAsLong() {
        if (configValue == null || configValue.trim().isEmpty()) {
            return 0L;
        }
        return Long.valueOf(configValue.trim());
    }

    /**
     * Parses the configuration value as a Boolean for flag operations.
     * Supports multiple boolean representations: true/false, Y/N, 1/0.
     * 
     * @return the configuration value as Boolean
     */
    public Boolean getValueAsBoolean() {
        if (configValue == null || configValue.trim().isEmpty()) {
            return false;
        }
        String value = configValue.trim().toUpperCase();
        return "TRUE".equals(value) || "Y".equals(value) || "1".equals(value);
    }

    /**
     * Validates the configuration value against the validation rule.
     * Supports regex patterns, range constraints, and enumeration validation.
     * 
     * @return true if value is valid, false otherwise
     */
    public boolean isValueValid() {
        if (validationRule == null || validationRule.trim().isEmpty()) {
            return true; // No validation rule specified
        }
        
        if (configValue == null) {
            return false; // Value required when validation rule exists
        }
        
        String rule = validationRule.trim();
        String value = configValue.trim();
        
        // Handle different validation rule types
        if (rule.startsWith("REGEX:")) {
            String pattern = rule.substring(6);
            return value.matches(pattern);
        } else if (rule.startsWith("RANGE:")) {
            return validateRange(value, rule.substring(6));
        } else if (rule.startsWith("ENUM:")) {
            String[] allowedValues = rule.substring(5).split(",");
            for (String allowedValue : allowedValues) {
                if (allowedValue.trim().equals(value)) {
                    return true;
                }
            }
            return false;
        } else {
            // Default to regex pattern matching
            return value.matches(rule);
        }
    }

    /**
     * Validates numeric value against range constraints.
     * Supports MIN and MAX range validation for numeric values.
     * 
     * @param value the value to validate
     * @param rangeRule the range constraint rule
     * @return true if value is within range, false otherwise
     */
    private boolean validateRange(String value, String rangeRule) {
        try {
            BigDecimal numericValue = new BigDecimal(value);
            String[] rangeParts = rangeRule.split(",");
            
            for (String part : rangeParts) {
                String constraint = part.trim();
                if (constraint.startsWith("MIN:")) {
                    BigDecimal minValue = new BigDecimal(constraint.substring(4));
                    if (numericValue.compareTo(minValue) < 0) {
                        return false;
                    }
                } else if (constraint.startsWith("MAX:")) {
                    BigDecimal maxValue = new BigDecimal(constraint.substring(4));
                    if (numericValue.compareTo(maxValue) > 0) {
                        return false;
                    }
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false; // Value is not numeric
        }
    }

    /**
     * Creates a new version of this configuration with updated value.
     * Preserves all metadata while incrementing version number.
     * 
     * @param newValue the new configuration value
     * @return new SystemConfiguration instance with incremented version
     */
    public SystemConfiguration createNewVersion(String newValue) {
        SystemConfiguration newVersion = new SystemConfiguration();
        newVersion.configCategory = this.configCategory;
        newVersion.configKey = this.configKey;
        newVersion.configValue = newValue;
        newVersion.dataType = this.dataType;
        newVersion.description = this.description;
        newVersion.defaultValue = this.defaultValue;
        newVersion.validationRule = this.validationRule;
        newVersion.versionNumber = this.versionNumber + 1;
        return newVersion;
    }

    /**
     * Resets configuration value to default for rollback scenarios.
     * Increments version number to maintain audit trail.
     */
    public void resetToDefault() {
        if (defaultValue != null) {
            setConfigValue(defaultValue);
        }
    }

    @Override
    public String toString() {
        return String.format("SystemConfiguration{id=%d, category='%s', key='%s', value='%s', version=%d}", 
                           id, configCategory, configKey, configValue, versionNumber);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SystemConfiguration)) return false;
        
        SystemConfiguration that = (SystemConfiguration) o;
        
        if (id != null) {
            return id.equals(that.id);
        }
        
        return configCategory != null && configCategory.equals(that.configCategory) &&
               configKey != null && configKey.equals(that.configKey) &&
               versionNumber != null && versionNumber.equals(that.versionNumber);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return id.hashCode();
        }
        
        int result = configCategory != null ? configCategory.hashCode() : 0;
        result = 31 * result + (configKey != null ? configKey.hashCode() : 0);
        result = 31 * result + (versionNumber != null ? versionNumber.hashCode() : 0);
        return result;
    }
}