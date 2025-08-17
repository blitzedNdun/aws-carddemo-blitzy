package com.carddemo.repository;

import com.carddemo.entity.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for ConfigurationRepository validating all query methods,
 * entity persistence, and business logic for configuration management functionality.
 * 
 * Tests cover:
 * - CRUD operations through JpaRepository
 * - Custom query methods for environment-specific configuration
 * - Configuration versioning and rollback support
 * - Active/inactive configuration management
 * - Configuration validation workflows
 * - Audit trail functionality
 * - Configuration category filtering
 * - Performance and data integrity
 * 
 * This test suite ensures the ConfigurationRepository correctly replaces 
 * mainframe configuration patterns while maintaining data consistency.
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.show-sql=true"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringJUnitConfig
class ConfigurationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ConfigurationRepository configurationRepository;

    private Configuration devConfig;
    private Configuration prodConfig;
    private Configuration testConfig;
    private Configuration inactiveConfig;

    @BeforeEach
    void setUp() {
        // Clear all existing data
        configurationRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // Create test configurations for different environments
        devConfig = Configuration.builder()
                .environment("DEV")
                .configKey("database.connection.pool.size")
                .name("Database Connection Pool Size")
                .category("DATABASE")
                .value("10")
                .description("Maximum number of database connections in pool")
                .version(2)
                .active(true)
                .requiresValidation(false)
                .lastModified(LocalDateTime.now().minusDays(1))
                .modifiedBy("admin")
                .createdDate(LocalDateTime.now().minusDays(2))
                .createdBy("admin")
                .build();

        prodConfig = Configuration.builder()
                .environment("PROD")
                .configKey("database.connection.pool.size")
                .name("Database Connection Pool Size")
                .category("DATABASE")
                .value("50")
                .description("Maximum number of database connections in pool")
                .version(1)
                .active(true)
                .requiresValidation(true)
                .lastModified(LocalDateTime.now().minusHours(2))
                .modifiedBy("prod-admin")
                .createdDate(LocalDateTime.now().minusDays(5))
                .createdBy("prod-admin")
                .build();

        testConfig = Configuration.builder()
                .environment("TEST")
                .configKey("security.jwt.expiration")
                .name("JWT Token Expiration Time")
                .category("SECURITY")
                .value("3600")
                .description("JWT token expiration time in seconds")
                .version(2)
                .active(true)
                .requiresValidation(false)
                .lastModified(LocalDateTime.now().minusHours(3))
                .modifiedBy("test-admin")
                .createdDate(LocalDateTime.now().minusDays(3))
                .createdBy("test-admin")
                .build();

        inactiveConfig = Configuration.builder()
                .environment("DEV")
                .configKey("database.connection.pool.size")
                .name("Database Connection Pool Size (Previous)")
                .category("DATABASE")
                .value("5")
                .description("Previous version of pool size configuration")
                .version(1)
                .active(false)
                .requiresValidation(false)
                .lastModified(LocalDateTime.now().minusDays(3))
                .modifiedBy("admin")
                .createdDate(LocalDateTime.now().minusDays(4))
                .createdBy("admin")
                .build();

        // Persist test data
        entityManager.persist(devConfig);
        entityManager.persist(prodConfig);
        entityManager.persist(testConfig);
        entityManager.persist(inactiveConfig);
        entityManager.flush();
    }

    // ====================
    // Standard JPA Repository Tests
    // ====================

    @Test
    void testSaveConfiguration() {
        Configuration newConfig = Configuration.builder()
                .environment("LOCAL")
                .configKey("batch.processing.chunk.size")
                .name("Batch Processing Chunk Size")
                .category("BATCH")
                .value("1000")
                .description("Number of records processed in each batch chunk")
                .version(1)
                .active(true)
                .requiresValidation(false)
                .modifiedBy("developer")
                .createdBy("developer")
                .build();

        Configuration saved = configurationRepository.save(newConfig);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEnvironment()).isEqualTo("LOCAL");
        assertThat(saved.getConfigKey()).isEqualTo("batch.processing.chunk.size");
        assertThat(saved.getValue()).isEqualTo("1000");
        assertThat(saved.getCategory()).isEqualTo("BATCH");
    }

    @Test
    void testFindByIdConfiguration() {
        Optional<Configuration> found = configurationRepository.findById(devConfig.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEnvironment()).isEqualTo("DEV");
        assertThat(found.get().getConfigKey()).isEqualTo("database.connection.pool.size");
        assertThat(found.get().getValue()).isEqualTo("10");
    }

    @Test
    void testFindAllConfigurations() {
        List<Configuration> allConfigs = configurationRepository.findAll();

        assertThat(allConfigs).hasSize(4);
        assertThat(allConfigs).extracting(Configuration::getEnvironment)
                .containsExactlyInAnyOrder("DEV", "PROD", "TEST", "DEV");
    }

    @Test
    void testDeleteConfiguration() {
        Long configId = devConfig.getId();
        configurationRepository.deleteById(configId);
        
        Optional<Configuration> deleted = configurationRepository.findById(configId);
        assertThat(deleted).isEmpty();

        List<Configuration> remaining = configurationRepository.findAll();
        assertThat(remaining).hasSize(3);
    }

    @Test
    void testCountConfigurations() {
        long count = configurationRepository.count();
        assertThat(count).isEqualTo(4);
    }

    @Test
    void testExistsById() {
        boolean exists = configurationRepository.existsById(devConfig.getId());
        assertThat(exists).isTrue();

        boolean notExists = configurationRepository.existsById(99999L);
        assertThat(notExists).isFalse();
    }

    // ====================
    // Custom Query Method Tests
    // ====================

    @Test
    void testFindByEnvironmentAndName() {
        Optional<Configuration> found = configurationRepository.findByEnvironmentAndName(
                "DEV", "Database Connection Pool Size");

        assertThat(found).isPresent();
        assertThat(found.get().getEnvironment()).isEqualTo("DEV");
        assertThat(found.get().getName()).isEqualTo("Database Connection Pool Size");
        assertThat(found.get().getValue()).isEqualTo("10");
    }

    @Test
    void testFindByEnvironmentAndNameNotFound() {
        Optional<Configuration> found = configurationRepository.findByEnvironmentAndName(
                "STAGING", "Non-existent Config");

        assertThat(found).isEmpty();
    }

    @Test
    void testFindByEnvironment() {
        List<Configuration> devConfigs = configurationRepository.findByEnvironment("DEV");

        assertThat(devConfigs).hasSize(2);
        assertThat(devConfigs).allMatch(config -> "DEV".equals(config.getEnvironment()));
        assertThat(devConfigs).extracting(Configuration::getConfigKey)
                .containsExactly("database.connection.pool.size", "database.connection.pool.size");
    }

    @Test
    void testFindByEnvironmentOrderedByName() {
        List<Configuration> prodConfigs = configurationRepository.findByEnvironment("PROD");

        assertThat(prodConfigs).hasSize(1);
        assertThat(prodConfigs.get(0).getEnvironment()).isEqualTo("PROD");
        assertThat(prodConfigs.get(0).getName()).isEqualTo("Database Connection Pool Size");
    }

    @Test
    void testFindByConfigCategory() {
        List<Configuration> databaseConfigs = configurationRepository.findByConfigCategory("DATABASE");

        assertThat(databaseConfigs).hasSize(3);
        assertThat(databaseConfigs).allMatch(config -> "DATABASE".equals(config.getCategory()));
        assertThat(databaseConfigs).extracting(Configuration::getValue)
                .containsExactlyInAnyOrder("10", "50", "5");
    }

    @Test
    void testFindByConfigCategoryOrderedByName() {
        List<Configuration> securityConfigs = configurationRepository.findByConfigCategory("SECURITY");

        assertThat(securityConfigs).hasSize(1);
        assertThat(securityConfigs.get(0).getCategory()).isEqualTo("SECURITY");
        assertThat(securityConfigs.get(0).getName()).isEqualTo("JWT Token Expiration Time");
    }

    @Test
    void testFindByConfigKey() {
        List<Configuration> poolSizeConfigs = configurationRepository.findByConfigKey(
                "database.connection.pool.size");

        assertThat(poolSizeConfigs).hasSize(3);
        assertThat(poolSizeConfigs).allMatch(config -> 
                "database.connection.pool.size".equals(config.getConfigKey()));
        // Should be ordered by version descending
        assertThat(poolSizeConfigs).extracting(Configuration::getVersion)
                .containsExactly(2, 1, 1);
    }

    @Test
    void testFindActiveByEnvironmentAndKey() {
        Optional<Configuration> activeConfig = configurationRepository.findActiveByEnvironmentAndKey(
                "DEV", "database.connection.pool.size");

        assertThat(activeConfig).isPresent();
        assertThat(activeConfig.get().getActive()).isTrue();
        assertThat(activeConfig.get().getValue()).isEqualTo("10");
        assertThat(activeConfig.get().getVersion()).isEqualTo(2);
    }

    @Test
    void testFindActiveByEnvironmentAndKeyNotFound() {
        Optional<Configuration> activeConfig = configurationRepository.findActiveByEnvironmentAndKey(
                "STAGING", "non.existent.key");

        assertThat(activeConfig).isEmpty();
    }

    @Test
    void testFindByCategoryAndEnvironment() {
        List<Configuration> devDatabaseConfigs = configurationRepository.findByCategoryAndEnvironment(
                "DATABASE", "DEV");

        assertThat(devDatabaseConfigs).hasSize(2);
        assertThat(devDatabaseConfigs).allMatch(config -> 
                "DATABASE".equals(config.getCategory()) && "DEV".equals(config.getEnvironment()));
    }

    @Test
    void testFindAllVersionsByKey() {
        List<Configuration> allVersions = configurationRepository.findAllVersionsByKey(
                "database.connection.pool.size");

        assertThat(allVersions).hasSize(3);
        assertThat(allVersions).extracting(Configuration::getVersion)
                .containsExactly(2, 1, 1); // Ordered by version descending
        assertThat(allVersions).extracting(Configuration::getEnvironment)
                .containsExactly("DEV", "DEV", "PROD"); // Then by environment (alphabetical)
    }

    @Test
    void testFindModifiedAfter() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(4);
        List<Configuration> recentlyModified = configurationRepository.findModifiedAfter(cutoffTime);

        assertThat(recentlyModified).hasSize(2);
        assertThat(recentlyModified).allMatch(config -> 
                config.getLastModified().isAfter(cutoffTime));
        // Should be ordered by lastModified descending
        assertThat(recentlyModified.get(0).getLastModified())
                .isAfter(recentlyModified.get(1).getLastModified());
    }

    @Test
    void testCountActiveByEnvironment() {
        long devActiveCount = configurationRepository.countActiveByEnvironment("DEV");
        assertThat(devActiveCount).isEqualTo(1);

        long prodActiveCount = configurationRepository.countActiveByEnvironment("PROD");
        assertThat(prodActiveCount).isEqualTo(1);

        long testActiveCount = configurationRepository.countActiveByEnvironment("TEST");
        assertThat(testActiveCount).isEqualTo(1);

        long stagingActiveCount = configurationRepository.countActiveByEnvironment("STAGING");
        assertThat(stagingActiveCount).isEqualTo(0);
    }

    @Test
    void testFindRequiringValidation() {
        List<Configuration> requiresValidation = configurationRepository.findRequiringValidation();

        assertThat(requiresValidation).hasSize(1);
        assertThat(requiresValidation.get(0).getRequiresValidation()).isTrue();
        assertThat(requiresValidation.get(0).getEnvironment()).isEqualTo("PROD");
    }

    // ====================
    // Configuration Entity Tests
    // ====================

    @Test
    void testConfigurationEntityValidation() {
        Configuration config = Configuration.builder()
                .environment("TEST")
                .configKey("test.validation.key")
                .name("Test Configuration")
                .category("TESTING")
                .value("test-value")
                .version(1)
                .active(true)
                .requiresValidation(false)
                .build();

        assertThat(config.isValidForActivation()).isTrue();
        assertThat(config.getUniqueIdentifier()).isEqualTo("TEST.test.validation.key");
    }

    @Test
    void testConfigurationEqualsAndHashCode() {
        Configuration config1 = Configuration.builder()
                .environment("DEV")
                .configKey("test.key")
                .version(1)
                .build();

        Configuration config2 = Configuration.builder()
                .environment("DEV")
                .configKey("test.key")
                .version(1)
                .build();

        Configuration config3 = Configuration.builder()
                .environment("DEV")
                .configKey("test.key")
                .version(2)
                .build();

        assertThat(config1).isEqualTo(config2);
        assertThat(config1).isNotEqualTo(config3);
        assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
        assertThat(config1.hashCode()).isNotEqualTo(config3.hashCode());
    }

    @Test
    void testConfigurationToString() {
        String configString = devConfig.toString();

        assertThat(configString).contains("Configuration{");
        assertThat(configString).contains("environment='DEV'");
        assertThat(configString).contains("configKey='database.connection.pool.size'");
        assertThat(configString).contains("version=2");
        assertThat(configString).contains("active=true");
        assertThat(configString).contains("value='[HIDDEN]'"); // Value should be hidden
    }

    // ====================
    // Business Logic and Integration Tests
    // ====================

    @Test
    void testConfigurationVersioningWorkflow() {
        // Create new version of existing configuration
        Configuration newVersion = Configuration.builder()
                .environment("DEV")
                .configKey("database.connection.pool.size.v2")
                .name("Database Connection Pool Size V2")
                .category("DATABASE")
                .value("15")
                .description("Updated pool size for better performance")
                .version(1)
                .active(false)
                .requiresValidation(true)
                .modifiedBy("developer")
                .createdBy("developer")
                .build();

        Configuration saved = configurationRepository.save(newVersion);

        // Verify versioning
        List<Configuration> allVersions = configurationRepository.findAllVersionsByKey(
                "database.connection.pool.size.v2");
        assertThat(allVersions).hasSize(1);

        Optional<Configuration> activeVersion = configurationRepository.findActiveByEnvironmentAndKey(
                "DEV", "database.connection.pool.size.v2");
        assertThat(activeVersion).isEmpty(); // No active version since we created it as inactive

        // Activate new version
        saved.setActive(true);
        saved.setRequiresValidation(false);
        configurationRepository.save(saved);

        // Verify activation
        Optional<Configuration> newActiveVersion = configurationRepository.findActiveByEnvironmentAndKey(
                "DEV", "database.connection.pool.size.v2");
        assertThat(newActiveVersion).isPresent();
        assertThat(newActiveVersion.get().getVersion()).isEqualTo(1);
        assertThat(newActiveVersion.get().getValue()).isEqualTo("15");
    }

    @Test
    void testConfigurationCategoryFiltering() {
        // Add more configurations for testing
        Configuration securityConfig1 = Configuration.builder()
                .environment("DEV")
                .configKey("security.encryption.algorithm")
                .name("Encryption Algorithm")
                .category("SECURITY")
                .value("AES-256")
                .version(1)
                .active(true)
                .requiresValidation(false)
                .build();

        Configuration batchConfig = Configuration.builder()
                .environment("DEV")
                .configKey("batch.job.timeout")
                .name("Batch Job Timeout")
                .category("BATCH")
                .value("3600")
                .version(1)
                .active(true)
                .requiresValidation(false)
                .build();

        configurationRepository.save(securityConfig1);
        configurationRepository.save(batchConfig);

        // Test category filtering
        List<Configuration> securityConfigs = configurationRepository.findByConfigCategory("SECURITY");
        assertThat(securityConfigs).hasSize(2);

        List<Configuration> batchConfigs = configurationRepository.findByConfigCategory("BATCH");
        assertThat(batchConfigs).hasSize(1);

        List<Configuration> databaseConfigs = configurationRepository.findByConfigCategory("DATABASE");
        assertThat(databaseConfigs).hasSize(3);
    }

    @Test
    void testEnvironmentSpecificQueries() {
        // Test environment filtering with categories
        List<Configuration> devDatabaseConfigs = configurationRepository.findByCategoryAndEnvironment(
                "DATABASE", "DEV");
        assertThat(devDatabaseConfigs).hasSize(2);

        List<Configuration> prodDatabaseConfigs = configurationRepository.findByCategoryAndEnvironment(
                "DATABASE", "PROD");
        assertThat(prodDatabaseConfigs).hasSize(1);

        List<Configuration> testSecurityConfigs = configurationRepository.findByCategoryAndEnvironment(
                "SECURITY", "TEST");
        assertThat(testSecurityConfigs).hasSize(1);
    }

    @Test
    void testAuditTrailQueries() {
        LocalDateTime recentTime = LocalDateTime.now().minusHours(5);
        List<Configuration> recentChanges = configurationRepository.findModifiedAfter(recentTime);

        assertThat(recentChanges).hasSize(2);
        assertThat(recentChanges).allMatch(config -> 
                config.getLastModified().isAfter(recentTime));

        // Test ordering by modification time
        if (recentChanges.size() > 1) {
            LocalDateTime firstModTime = recentChanges.get(0).getLastModified();
            LocalDateTime secondModTime = recentChanges.get(1).getLastModified();
            assertThat(firstModTime).isAfterOrEqualTo(secondModTime);
        }
    }

    // ====================
    // Edge Cases and Error Handling Tests
    // ====================

    @Test
    void testEmptyResultQueries() {
        List<Configuration> nonExistentCategory = configurationRepository.findByConfigCategory("NONEXISTENT");
        assertThat(nonExistentCategory).isEmpty();

        List<Configuration> nonExistentEnvironment = configurationRepository.findByEnvironment("NONEXISTENT");
        assertThat(nonExistentEnvironment).isEmpty();

        Optional<Configuration> nonExistentConfig = configurationRepository.findByEnvironmentAndName(
                "DEV", "Non-existent Configuration");
        assertThat(nonExistentConfig).isEmpty();
    }

    @Test
    void testNullAndEmptyParameterHandling() {
        // These should return empty results, not throw exceptions
        List<Configuration> nullCategory = configurationRepository.findByConfigCategory(null);
        assertThat(nullCategory).isEmpty();

        List<Configuration> emptyEnvironment = configurationRepository.findByEnvironment("");
        assertThat(emptyEnvironment).isEmpty();
    }

    @Test
    void testConfigurationBuilderAndDefaults() {
        Configuration config = Configuration.builder()
                .environment("TEST")
                .configKey("test.key")
                .name("Test Config")
                .category("TEST")
                .value("test-value")
                .build();

        // Test builder pattern works
        assertThat(config.getEnvironment()).isEqualTo("TEST");
        assertThat(config.getConfigKey()).isEqualTo("test.key");
        assertThat(config.getName()).isEqualTo("Test Config");
        assertThat(config.getCategory()).isEqualTo("TEST");
        assertThat(config.getValue()).isEqualTo("test-value");
    }

    @Test 
    void testConfigurationValidationBoundaries() {
        Configuration invalidConfig = Configuration.builder()
                .environment("")
                .configKey("")
                .name("")
                .category("")
                .value("")
                .build();

        assertThat(invalidConfig.isValidForActivation()).isFalse();

        Configuration validConfig = Configuration.builder()
                .environment("DEV")
                .configKey("valid.key")
                .name("Valid Config")
                .category("TESTING")
                .value("valid-value")
                .build();

        assertThat(validConfig.isValidForActivation()).isTrue();
    }
}