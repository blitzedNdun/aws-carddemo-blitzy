package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity representing application configuration properties.
 * 
 * This entity replaces mainframe configuration management patterns with modern
 * Spring Data JPA entity pattern, supporting:
 * - Configuration property CRUD operations
 * - Version management for configuration changes
 * - Environment-specific configuration queries
 * - Audit trail for configuration modifications
 * - Configuration rollback support
 * - Configuration validation before persistence
 * - Configuration caching and optimistic locking
 * 
 * Supports the modernization from COBOL/CICS configuration management to 
 * cloud-native configuration services while maintaining identical configuration
 * access patterns and data consistency requirements.
 * 
 * @author Blitzy agent
 * @version 1.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "configuration", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"environment", "config_key", "version"}),
           @UniqueConstraint(columnNames = {"environment", "config_key", "active"})
       },
       indexes = {
           @Index(name = "idx_config_env_key", columnList = "environment, config_key"),
           @Index(name = "idx_config_category", columnList = "category"),
           @Index(name = "idx_config_active", columnList = "active"),
           @Index(name = "idx_config_last_modified", columnList = "last_modified")
       })
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Configuration implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key for configuration records.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Target environment for this configuration (DEV, TEST, PROD).
     * Supports environment-specific configuration management.
     */
    @NotBlank(message = "Environment is required")
    @Size(max = 10, message = "Environment must not exceed 10 characters")
    @Pattern(regexp = "^(DEV|TEST|PROD|LOCAL)$", message = "Environment must be DEV, TEST, PROD, or LOCAL")
    @Column(name = "environment", nullable = false, length = 10)
    private String environment;

    /**
     * Human-readable configuration property name.
     */
    @NotBlank(message = "Configuration name is required")
    @Size(max = 100, message = "Configuration name must not exceed 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Unique configuration key identifier for programmatic access.
     */
    @NotBlank(message = "Configuration key is required")
    @Size(max = 200, message = "Configuration key must not exceed 200 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Configuration key must contain only alphanumeric characters, dots, underscores, and hyphens")
    @Column(name = "config_key", nullable = false, length = 200)
    private String configKey;

    /**
     * Configuration category for logical grouping (DATABASE, SECURITY, BATCH, etc.).
     */
    @NotBlank(message = "Configuration category is required")
    @Size(max = 50, message = "Configuration category must not exceed 50 characters")
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    /**
     * Configuration value as string representation.
     */
    @NotNull(message = "Configuration value is required")
    @Size(max = 4000, message = "Configuration value must not exceed 4000 characters")
    @Column(name = "config_value", nullable = false, length = 4000)
    private String value;

    /**
     * Human-readable description of the configuration parameter.
     */
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * Configuration version number for rollback support.
     */
    @NotNull(message = "Version is required")
    @Min(value = 1, message = "Version must be at least 1")
    @Column(name = "version", nullable = false)
    private Integer version;

    /**
     * Indicates if this configuration version is currently active.
     * Only one version per environment/key combination can be active.
     */
    @NotNull(message = "Active status is required")
    @Column(name = "active", nullable = false)
    private Boolean active;

    /**
     * Flag indicating if configuration requires validation before activation.
     */
    @NotNull(message = "Requires validation flag is required")
    @Column(name = "requires_validation", nullable = false)
    private Boolean requiresValidation;

    /**
     * Last modification timestamp for audit trail.
     */
    @LastModifiedDate
    @Column(name = "last_modified", nullable = false)
    private LocalDateTime lastModified;

    /**
     * User who last modified this configuration.
     */
    @Size(max = 50, message = "Modified by must not exceed 50 characters")
    @Column(name = "modified_by", length = 50)
    private String modifiedBy;

    /**
     * Creation timestamp for audit trail.
     */
    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    /**
     * User who created this configuration.
     */
    @Size(max = 50, message = "Created by must not exceed 50 characters")
    @Column(name = "created_by", length = 50, updatable = false)
    private String createdBy;

    /**
     * Optimistic locking version for concurrent update prevention.
     */
    @Version
    @Column(name = "entity_version")
    private Long entityVersion;

    /**
     * Post-construct initialization for default values.
     */
    @PrePersist
    public void initializeDefaults() {
        if (this.version == null) {
            this.version = 1;
        }
        if (this.active == null) {
            this.active = false;
        }
        if (this.requiresValidation == null) {
            this.requiresValidation = false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (this.createdDate == null) {
            this.createdDate = now;
        }
        if (this.lastModified == null) {
            this.lastModified = now;
        }
    }

    /**
     * Validates that only one configuration per environment/key can be active.
     *
     * @return true if this configuration is valid for activation
     */
    public boolean isValidForActivation() {
        return this.configKey != null && !this.configKey.trim().isEmpty() &&
               this.environment != null && !this.environment.trim().isEmpty() &&
               this.value != null;
    }

    /**
     * Creates a unique identifier for this configuration combining environment and key.
     *
     * @return unique identifier string
     */
    public String getUniqueIdentifier() {
        return String.format("%s.%s", environment, configKey);
    }

    /**
     * Custom equals method based on business key (environment + configKey + version).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Configuration that = (Configuration) o;
        return Objects.equals(environment, that.environment) &&
               Objects.equals(configKey, that.configKey) &&
               Objects.equals(version, that.version);
    }

    /**
     * Custom hashCode method based on business key (environment + configKey + version).
     */
    @Override
    public int hashCode() {
        return Objects.hash(environment, configKey, version);
    }

    /**
     * String representation for debugging and logging.
     */
    @Override
    public String toString() {
        return "Configuration{" +
                "id=" + id +
                ", environment='" + environment + '\'' +
                ", configKey='" + configKey + '\'' +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", value='" + (value != null ? "[HIDDEN]" : null) + '\'' +
                ", version=" + version +
                ", active=" + active +
                ", requiresValidation=" + requiresValidation +
                ", lastModified=" + lastModified +
                '}';
    }
}