package com.carddemo.repository;

import com.carddemo.entity.SystemConfiguration;
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
public interface ConfigurationRepository extends JpaRepository<SystemConfiguration, Long> {

    /**
     * Finds configuration entries by configuration category and key.
     * Supports category-specific configuration queries for logical grouping.
     * 
     * @param category the configuration category (INTEREST_RATES, FEE_SCHEDULE, etc.)
     * @param configKey the configuration key identifier
     * @return Optional containing the configuration if found
     */
    @Query("SELECT c FROM SystemConfiguration c WHERE c.configCategory = :category AND c.configKey = :configKey")
    Optional<SystemConfiguration> findByCategoryAndKey(@Param("category") String category, 
                                                      @Param("configKey") String configKey);

    /**
     * Finds all configuration entries for a specific category.
     * Enables category-specific configuration retrieval for logical grouping.
     * 
     * @param category the configuration category
     * @return List of configuration entries for the category
     */
    @Query("SELECT c FROM SystemConfiguration c WHERE c.configCategory = :category ORDER BY c.configKey")
    List<SystemConfiguration> findByCategory(@Param("category") String category);

    /**
     * Finds configuration entries by category for logical grouping.
     * Supports configuration organization and batch operations on related settings.
     * 
     * @param category the configuration category (INTEREST_RATES, FEE_SCHEDULE, PROCESSING_LIMITS, etc.)
     * @return List of configuration entries in the category
     */
    @Query("SELECT c FROM SystemConfiguration c WHERE c.configCategory = :category ORDER BY c.configKey")
    List<SystemConfiguration> findByConfigCategory(@Param("category") String category);

    /**
     * Finds configuration entries by configuration key for exact matching.
     * Provides direct key-based access for configuration property retrieval.
     * 
     * @param configKey the configuration key identifier
     * @return List of configuration entries matching the key
     */
    @Query("SELECT c FROM SystemConfiguration c WHERE c.configKey = :configKey ORDER BY c.versionNumber DESC")
    List<SystemConfiguration> findByConfigKey(@Param("configKey") String configKey);

    /**
     * Finds the latest version of a configuration entry by key.
     * Supports configuration versioning with latest version retrieval.
     * 
     * @param configKey the configuration key
     * @return Optional containing the latest configuration version if found
     */
    @Query("SELECT c FROM SystemConfiguration c WHERE c.configKey = :configKey ORDER BY c.versionNumber DESC")
    Optional<SystemConfiguration> findLatestByConfigKey(@Param("configKey") String configKey);

    /**
     * Finds configuration entries by category with latest versions.
     * Enables category-based configuration management with version control.
     * 
     * @param category the configuration category
     * @return List of configuration entries matching category with latest versions
     */
    @Query("SELECT c FROM SystemConfiguration c WHERE c.configCategory = :category AND c.versionNumber = (SELECT MAX(c2.versionNumber) FROM SystemConfiguration c2 WHERE c2.configKey = c.configKey) ORDER BY c.configKey")
    List<SystemConfiguration> findLatestByCategoryAndKey(@Param("category") String category);

    /**
     * Finds all versions of a configuration key for version history.
     * Supports configuration version history and rollback operations.
     * 
     * @param configKey the configuration key
     * @return List of all configuration versions ordered by version descending
     */
    @Query("SELECT c FROM SystemConfiguration c WHERE c.configKey = :configKey ORDER BY c.versionNumber DESC")
    List<SystemConfiguration> findAllVersionsByKey(@Param("configKey") String configKey);

    /**
     * Finds configuration entries modified after a specific timestamp.
     * Supports configuration audit trails and change tracking.
     * 
     * @param timestamp the timestamp threshold for filtering
     * @return List of configuration entries modified after the timestamp
     */
    @Query("SELECT c FROM SystemConfiguration c WHERE c.lastModifiedDate > :timestamp ORDER BY c.lastModifiedDate DESC")
    List<SystemConfiguration> findModifiedAfter(@Param("timestamp") java.time.LocalDateTime timestamp);

    /**
     * Counts configuration entries by category.
     * Provides configuration statistics for monitoring and validation.
     * 
     * @param category the configuration category
     * @return count of configuration entries in the category
     */
    @Query("SELECT COUNT(c) FROM SystemConfiguration c WHERE c.configCategory = :category")
    long countByCategory(@Param("category") String category);

    /**
     * Finds configuration entries created recently for audit review.
     * Supports configuration validation workflows and approval processes.
     * 
     * @param hours number of hours back to search
     * @return List of configuration entries created in the specified timeframe
     */
    @Query("SELECT c FROM SystemConfiguration c WHERE c.createdDate > :timestamp ORDER BY c.createdDate DESC")
    List<SystemConfiguration> findRecentlyCreated(@Param("timestamp") java.time.LocalDateTime timestamp);

    // Standard JpaRepository methods are automatically inherited:
    // - save(SystemConfiguration entity)
    // - findById(Long id)  
    // - deleteById(Long id)
    // - findAll()
    // - saveAndFlush(SystemConfiguration entity)
    // - count()
    // - existsById(Long id)
}