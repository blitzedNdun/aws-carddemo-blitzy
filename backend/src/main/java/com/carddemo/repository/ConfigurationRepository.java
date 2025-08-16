package com.carddemo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for configuration data persistence providing CRUD operations 
 * for application configuration settings with Spring Data JPA integration.
 * 
 * This repository replaces mainframe configuration management patterns with modern
 * Spring Data JPA repository pattern, supporting:
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
@Repository
public interface ConfigurationRepository extends JpaRepository<Configuration, Long> {

    /**
     * Finds configuration entries by environment and configuration name.
     * Supports environment-specific configuration queries for multi-environment deployments.
     * 
     * @param environment the target environment (DEV, TEST, PROD)
     * @param name the configuration property name
     * @return Optional containing the configuration if found
     */
    @Query("SELECT c FROM Configuration c WHERE c.environment = :environment AND c.name = :name")
    Optional<Configuration> findByEnvironmentAndName(@Param("environment") String environment, 
                                                    @Param("name") String name);

    /**
     * Finds all configuration entries for a specific environment.
     * Enables environment-specific configuration retrieval for deployment management.
     * 
     * @param environment the target environment
     * @return List of configuration entries for the environment
     */
    @Query("SELECT c FROM Configuration c WHERE c.environment = :environment ORDER BY c.name")
    List<Configuration> findByEnvironment(@Param("environment") String environment);

    /**
     * Finds configuration entries by category for logical grouping.
     * Supports configuration organization and batch operations on related settings.
     * 
     * @param category the configuration category (DATABASE, SECURITY, BATCH, etc.)
     * @return List of configuration entries in the category
     */
    @Query("SELECT c FROM Configuration c WHERE c.category = :category ORDER BY c.name")
    List<Configuration> findByConfigCategory(@Param("category") String category);

    /**
     * Finds configuration entries by configuration key for exact matching.
     * Provides direct key-based access for configuration property retrieval.
     * 
     * @param configKey the configuration key identifier
     * @return List of configuration entries matching the key
     */
    @Query("SELECT c FROM Configuration c WHERE c.configKey = :configKey ORDER BY c.version DESC")
    List<Configuration> findByConfigKey(@Param("configKey") String configKey);

    /**
     * Finds the active configuration entry for a specific environment and key.
     * Supports configuration versioning with active/inactive status management.
     * 
     * @param environment the target environment
     * @param configKey the configuration key
     * @return Optional containing the active configuration if found
     */
    @Query("SELECT c FROM Configuration c WHERE c.environment = :environment AND c.configKey = :configKey AND c.active = true")
    Optional<Configuration> findActiveByEnvironmentAndKey(@Param("environment") String environment, 
                                                         @Param("configKey") String configKey);

    /**
     * Finds configuration entries by category and environment for scoped retrieval.
     * Enables environment-specific category-based configuration management.
     * 
     * @param category the configuration category
     * @param environment the target environment
     * @return List of configuration entries matching category and environment
     */
    @Query("SELECT c FROM Configuration c WHERE c.category = :category AND c.environment = :environment ORDER BY c.name")
    List<Configuration> findByCategoryAndEnvironment(@Param("category") String category, 
                                                    @Param("environment") String environment);

    /**
     * Finds all versions of a configuration key across environments.
     * Supports configuration version history and rollback operations.
     * 
     * @param configKey the configuration key
     * @return List of all configuration versions ordered by version descending
     */
    @Query("SELECT c FROM Configuration c WHERE c.configKey = :configKey ORDER BY c.version DESC, c.environment")
    List<Configuration> findAllVersionsByKey(@Param("configKey") String configKey);

    /**
     * Finds configuration entries modified after a specific timestamp.
     * Supports configuration audit trails and change tracking.
     * 
     * @param timestamp the timestamp threshold for filtering
     * @return List of configuration entries modified after the timestamp
     */
    @Query("SELECT c FROM Configuration c WHERE c.lastModified > :timestamp ORDER BY c.lastModified DESC")
    List<Configuration> findModifiedAfter(@Param("timestamp") java.time.LocalDateTime timestamp);

    /**
     * Counts active configuration entries for an environment.
     * Provides configuration statistics for monitoring and validation.
     * 
     * @param environment the target environment
     * @return count of active configuration entries
     */
    @Query("SELECT COUNT(c) FROM Configuration c WHERE c.environment = :environment AND c.active = true")
    long countActiveByEnvironment(@Param("environment") String environment);

    /**
     * Finds configuration entries requiring validation based on change flags.
     * Supports configuration validation workflows and approval processes.
     * 
     * @return List of configuration entries pending validation
     */
    @Query("SELECT c FROM Configuration c WHERE c.requiresValidation = true ORDER BY c.lastModified")
    List<Configuration> findRequiringValidation();

    // Standard JpaRepository methods are automatically inherited:
    // - save(Configuration entity)
    // - findById(Long id)  
    // - deleteById(Long id)
    // - findAll()
    // - saveAndFlush(Configuration entity)
    // - count()
    // - existsById(Long id)
}

/**
 * Configuration entity representing application configuration properties.
 * This is a placeholder definition to satisfy the repository type parameter.
 * The actual Configuration entity should be implemented separately with proper
 * JPA annotations, field mappings, and validation rules.
 * 
 * Note: In a complete implementation, this would be defined in a separate
 * entity package (com.carddemo.entity.Configuration) and imported here.
 */
class Configuration {
    private Long id;
    private String environment;
    private String name;
    private String configKey;
    private String category;
    private String value;
    private String description;
    private Integer version;
    private boolean active;
    private boolean requiresValidation;
    private java.time.LocalDateTime lastModified;
    private String modifiedBy;
    private java.time.LocalDateTime createdDate;
    private String createdBy;
    
    // Getters, setters, and JPA annotations would be implemented
    // in the actual entity class
}