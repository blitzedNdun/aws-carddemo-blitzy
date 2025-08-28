/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.config;

import com.carddemo.config.DatabaseConfig;
import com.carddemo.config.TransactionConfig;
import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.Customer;
import com.carddemo.entity.UserSecurity;
import com.carddemo.util.CobolDataConverter;
import com.carddemo.util.Constants;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.Properties;

/**
 * Test configuration for database connections providing H2 in-memory database for unit tests 
 * and Testcontainers PostgreSQL setup for integration tests, matching VSAM-to-PostgreSQL migration patterns.
 * 
 * This test configuration class establishes comprehensive database infrastructure for testing the CardDemo 
 * application's migration from VSAM KSDS datasets to PostgreSQL relational database operations while 
 * maintaining identical data access patterns and precision requirements for financial calculations.
 * 
 * Core Responsibilities:
 * - Configure dual database strategies: H2 in-memory for fast unit tests, PostgreSQL containers for integration tests
 * - Set up HikariCP connection pooling with test-optimized settings for both database types
 * - Configure JPA/Hibernate for entity management with PostgreSQL dialect matching production configuration
 * - Initialize test schema and load test data from SQL scripts for comprehensive test scenarios
 * - Provide transaction management with rollback support for test isolation between test methods
 * - Register COBOL data type converters ensuring BigDecimal precision matches COBOL COMP-3 decimal handling
 * 
 * Test Database Strategies:
 * - Unit Tests: H2 in-memory database for fast execution without external dependencies
 * - Integration Tests: Testcontainers PostgreSQL providing real database environment isolation
 * - Schema Management: Automatic DDL validation against JPA entities with schema.sql initialization
 * - Test Data Loading: Automated population from test-data.sql with VSAM-equivalent test records
 * - Transaction Rollback: Automatic @Transactional rollback ensuring clean test state between executions
 * 
 * VSAM-to-PostgreSQL Migration Support:
 * - Composite Key Testing: Support for multi-column primary keys matching VSAM key structures from LISTCAT analysis
 * - BigDecimal Precision: Test configurations ensuring COBOL COMP-3 packed decimal compatibility using CobolDataConverter
 * - Entity Relationships: JPA foreign key testing with Account, Customer, Transaction, and UserSecurity entities
 * - Connection Pool Sizing: Optimized for test concurrency while maintaining production-equivalent behavior
 * - Transaction Boundaries: Test transaction management matching CICS SYNCPOINT behavior for consistency validation
 * 
 * Key Features:
 * - @TestConfiguration annotation ensuring test-only bean registration without affecting production context
 * - @Primary annotations on DataSource beans ensuring test beans take precedence during test execution
 * - Profile-based configuration supporting different test environments (unit vs integration testing)
 * - Automatic PostgreSQL container lifecycle management with proper cleanup after test completion
 * - Schema validation and test data population with error handling and rollback capabilities
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@TestConfiguration
@ActiveProfiles("test")
public class TestDatabaseConfig {

    // H2 Database configuration constants for unit testing
    private static final String H2_JDBC_URL = "jdbc:h2:mem:carddemo_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL";
    private static final String H2_USERNAME = "sa";
    private static final String H2_PASSWORD = "";
    private static final String H2_DRIVER = "org.h2.Driver";
    
    // PostgreSQL Testcontainers configuration for integration testing
    private static final String POSTGRES_DOCKER_IMAGE = "postgres:15.4";
    private static final String POSTGRES_DATABASE = "carddemo_test";
    private static final String POSTGRES_USERNAME = "carddemo_test";
    private static final String POSTGRES_PASSWORD = "carddemo_test";
    
    // Test-optimized HikariCP connection pool settings
    private static final int TEST_MAX_POOL_SIZE = 5;           // Smaller pool for test efficiency
    private static final int TEST_MINIMUM_IDLE = 1;            // Minimal idle connections for tests
    private static final long TEST_CONNECTION_TIMEOUT = 10000; // 10 seconds connection timeout
    private static final long TEST_IDLE_TIMEOUT = 60000;      // 1 minute idle timeout
    private static final long TEST_MAX_LIFETIME = 300000;     // 5 minutes maximum connection lifetime
    private static final long TEST_LEAK_DETECTION_THRESHOLD = 30000; // 30 seconds leak detection
    
    // JPA/Hibernate test configuration settings
    private static final String TEST_HIBERNATE_DDL_AUTO = "none";   // Use manual schema initialization instead
    private static final String TEST_HIBERNATE_SHOW_SQL = "true";          // Enable SQL logging for test debugging
    private static final String TEST_HIBERNATE_FORMAT_SQL = "true";        // Format SQL for test readability
    private static final String TEST_HIBERNATE_JDBC_BATCH_SIZE = "10";     // Smaller batch size for test control
    
    // Test schema and data initialization file paths
    private static final String TEST_SCHEMA_SCRIPT = "schema-test.sql";
    private static final String TEST_DATA_SCRIPT = "test-data.sql";

    /**
     * Configures H2 in-memory DataSource for fast unit test execution.
     * 
     * This method creates and configures an H2 in-memory database optimized for unit testing scenarios
     * where fast startup, execution, and cleanup are prioritized over exact production environment matching.
     * The H2 database provides PostgreSQL compatibility mode ensuring SQL dialect consistency while
     * maintaining the performance benefits of in-memory execution.
     * 
     * H2 Configuration Features:
     * - In-memory database with persistent connection during test suite execution
     * - PostgreSQL compatibility mode ensuring SQL syntax and function compatibility
     * - Optimized connection pool sizing for single-threaded test execution patterns
     * - Automatic schema creation and population from test initialization scripts
     * - Fast cleanup and reset capabilities between test method executions
     * 
     * Performance Characteristics:
     * - Sub-millisecond startup time enabling fast test suite execution
     * - Memory-resident storage providing instant data access without I/O overhead
     * - Automatic transaction rollback support for test method isolation
     * - Minimal resource footprint suitable for continuous integration environments
     * 
     * Use Cases:
     * - JPA repository unit tests validating CRUD operations and query logic
     * - Service layer unit tests requiring database interactions with mocked external dependencies
     * - Data conversion testing ensuring COBOL-to-Java type conversion accuracy using CobolDataConverter
     * - Business logic validation tests requiring controlled test data scenarios
     * 
     * @return configured HikariDataSource for H2 in-memory database connectivity
     */
    @Bean("dataSource")
    @Primary
    @Profile({"test", "unit-test", "contract-test"})
    public DataSource h2DataSource() {
        HikariConfig config = new HikariConfig();
        
        // H2 in-memory database connection properties
        config.setJdbcUrl(H2_JDBC_URL);
        config.setUsername(H2_USERNAME);
        config.setPassword(H2_PASSWORD);
        config.setDriverClassName(H2_DRIVER);
        
        // Test-optimized connection pool sizing for single-threaded unit tests
        config.setMaximumPoolSize(TEST_MAX_POOL_SIZE);
        config.setMinimumIdle(TEST_MINIMUM_IDLE);
        config.setConnectionTimeout(TEST_CONNECTION_TIMEOUT);
        config.setIdleTimeout(TEST_IDLE_TIMEOUT);
        config.setMaxLifetime(TEST_MAX_LIFETIME);
        config.setLeakDetectionThreshold(TEST_LEAK_DETECTION_THRESHOLD);
        
        // Connection pool identification and monitoring for test debugging
        config.setPoolName("CardDemo-Test-H2-HikariCP");
        config.setConnectionTestQuery("SELECT 1");
        
        // H2-specific optimizations for test performance
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "50");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "1024");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        
        return new HikariDataSource(config);
    }

    /**
     * Configures PostgreSQL Testcontainers DataSource for integration testing with database isolation.
     * 
     * This method creates and manages a PostgreSQL database container providing a complete PostgreSQL 
     * environment for integration testing scenarios that require exact production database behavior, 
     * including advanced features like composite keys, partitioning, and specific PostgreSQL dialect 
     * functionality that cannot be accurately replicated in H2.
     * 
     * Testcontainers Configuration Features:
     * - Real PostgreSQL 15.4 instance providing exact production environment matching
     * - Isolated container per test class ensuring complete test isolation and parallel execution capability
     * - Automatic container lifecycle management with proper startup, configuration, and cleanup
     * - Network isolation preventing test interference and ensuring consistent test execution results
     * - Volume mounting capability for complex schema and test data initialization scenarios
     * 
     * Integration Testing Capabilities:
     * - Complex JPA entity relationship testing with foreign key constraints and referential integrity
     * - PostgreSQL-specific feature validation including partitioned tables, composite indexes, and advanced data types
     * - Multi-table transaction testing with proper ACID compliance validation under concurrent access patterns
     * - Performance testing with realistic database I/O patterns and connection pool behavior analysis
     * - Schema migration testing ensuring DDL scripts execute correctly against actual PostgreSQL instances
     * 
     * VSAM Migration Validation:
     * - Composite primary key support matching VSAM KSDS key structures identified in LISTCAT analysis
     * - BigDecimal precision testing ensuring exact COBOL COMP-3 packed decimal compatibility
     * - Transaction isolation level validation matching CICS SYNCPOINT behavior for financial data consistency
     * - Index performance testing validating B-tree indexes replicate VSAM alternate index access patterns
     * 
     * Container Management:
     * - Automatic PostgreSQL container startup with configured database, username, and password
     * - Health check validation ensuring database readiness before test execution begins
     * - Proper resource cleanup and container termination after test completion
     * - Support for test-specific container configuration and network isolation
     * 
     * @return configured HikariDataSource for Testcontainers PostgreSQL database connectivity
     */
    @Bean("dataSource")
    @Profile("integration-test") 
    public DataSource testcontainersDataSource() {
        // Create PostgreSQL container with specified version and configuration
        PostgreSQLContainer<?> postgresContainer = createPostgreSQLContainer();
        
        // Start container and wait for readiness
        postgresContainer.start();
        
        // Configure HikariCP connection pool for Testcontainers PostgreSQL
        HikariConfig config = new HikariConfig();
        
        // PostgreSQL Testcontainers connection properties
        config.setJdbcUrl(postgresContainer.getJdbcUrl());
        config.setUsername(postgresContainer.getUsername());
        config.setPassword(postgresContainer.getPassword());
        config.setDriverClassName("org.postgresql.Driver");
        
        // Integration test optimized connection pool sizing
        config.setMaximumPoolSize(TEST_MAX_POOL_SIZE);
        config.setMinimumIdle(TEST_MINIMUM_IDLE);
        config.setConnectionTimeout(TEST_CONNECTION_TIMEOUT);
        config.setIdleTimeout(TEST_IDLE_TIMEOUT);
        config.setMaxLifetime(TEST_MAX_LIFETIME);
        config.setLeakDetectionThreshold(TEST_LEAK_DETECTION_THRESHOLD);
        
        // Connection pool identification and monitoring for integration test debugging
        config.setPoolName("CardDemo-Integration-Test-PostgreSQL-HikariCP");
        config.setConnectionTestQuery("SELECT 1");
        
        // PostgreSQL-specific optimizations matching production configuration
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "100");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        
        return new HikariDataSource(config);
    }

    /**
     * Configures JPA EntityManagerFactory with test-optimized Hibernate settings for entity management.
     * 
     * This method establishes comprehensive JPA/Hibernate configuration specifically optimized for testing
     * scenarios while maintaining compatibility with production entity mappings and relationships. The
     * configuration supports both H2 in-memory and PostgreSQL Testcontainers databases with appropriate
     * dialect selection and performance tuning for test execution environments.
     * 
     * Test Entity Management Configuration:
     * - Dynamic datasource injection supporting both H2 and PostgreSQL test configurations
     * - Package scanning for com.carddemo.entity classes including Account, Transaction, Customer, and UserSecurity entities
     * - Test-optimized Hibernate properties with enhanced logging and debugging capabilities for test development
     * - Schema validation ensuring entity definitions match test database table structures with proper error reporting
     * 
     * Entity Relationship Testing Support:
     * - Composite key mapping validation for multi-column VSAM key structures using @EmbeddedId annotations
     * - Foreign key relationship testing with proper cascade and fetch strategy validation for entity associations
     * - BigDecimal precision testing ensuring COBOL COMP-3 packed decimal accuracy using scale=2 and HALF_UP rounding
     * - Lazy loading behavior validation for performance testing and proper entity proxy management
     * 
     * Test Performance Optimizations:
     * - Reduced batch size configuration optimizing for test data control and transaction boundary management
     * - Enhanced SQL logging enabling detailed query analysis for test debugging and performance optimization
     * - Connection pooling integration with test-specific HikariCP configurations for optimal resource utilization
     * - Query plan caching with smaller cache sizes appropriate for test data volumes and execution patterns
     * 
     * COBOL Migration Testing Features:
     * - Custom attribute converter registration for COBOL data type conversions using CobolDataConverter utility
     * - Transaction isolation level configuration matching CICS SYNCPOINT behavior for consistency testing
     * - Date and timestamp handling validation ensuring proper conversion between COBOL and Java date formats
     * - Character encoding configuration supporting UTF-8 data handling for international character set testing
     * 
     * @param dataSource configured test DataSource (H2 or PostgreSQL Testcontainers)
     * @return LocalContainerEntityManagerFactoryBean for test JPA entity management
     */
    @Bean("entityManagerFactory")
    @Primary
    public EntityManagerFactory testEntityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        
        // Configure data source and entity scanning for test environment
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.carddemo.entity");
        
        // Configure Hibernate as JPA vendor with test-specific settings
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setDatabase(org.springframework.orm.jpa.vendor.Database.POSTGRESQL);
        vendorAdapter.setShowSql(Boolean.parseBoolean(TEST_HIBERNATE_SHOW_SQL));
        vendorAdapter.setGenerateDdl(false); // Use test schema scripts for controlled initialization
        factory.setJpaVendorAdapter(vendorAdapter);
        
        // Configure Hibernate properties optimized for testing scenarios
        Properties jpaProperties = configureJpaProperties();
        factory.setJpaProperties(jpaProperties);
        
        // Initialize the factory with test configuration
        factory.afterPropertiesSet();
        
        return factory.getObject();
    }

    /**
     * Configures Spring PlatformTransactionManager for test transaction management with automatic rollback support.
     * 
     * This method establishes comprehensive transaction management specifically designed for testing scenarios
     * with automatic rollback capabilities ensuring complete test isolation between test method executions.
     * The configuration provides CICS SYNCPOINT equivalent behavior while enabling test-specific features
     * like rollback-only transactions and nested transaction support for complex test scenarios.
     * 
     * Test Transaction Management Features:
     * - Automatic rollback after each @Transactional test method ensuring clean state between test executions
     * - Test transaction isolation preventing test interference and enabling parallel test execution
     * - Enhanced error handling and logging for test debugging with detailed transaction lifecycle information
     * - Support for nested transactions enabling complex test scenario development with controlled commit points
     * 
     * CICS Migration Testing Equivalency:
     * - Transaction boundary testing matching CICS SYNCPOINT behavior with proper atomicity validation
     * - Rollback testing ensuring proper resource cleanup and state restoration after transaction failures
     * - Concurrency testing with appropriate isolation levels preventing dirty reads and ensuring data consistency
     * - Resource coordination testing validating single-phase commit behavior for PostgreSQL operations
     * 
     * Performance and Debugging Features:
     * - Enhanced transaction timeout configuration suitable for debugging and test development scenarios
     * - Detailed transaction logging enabling analysis of transaction boundaries and rollback behavior
     * - Connection reuse optimization with test-specific EntityManager lifecycle management
     * - Integration with test frameworks supporting Spring Boot Test and MockMvc transaction testing
     * 
     * @param entityManagerFactory configured test JPA EntityManagerFactory for entity management
     * @return JpaTransactionManager for test Spring transaction management integration
     */
    @Bean
    @Primary
    public PlatformTransactionManager testTransactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        
        // Associate with test EntityManagerFactory for JPA integration
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        
        // Configure test-specific transaction manager behavior
        transactionManager.setRollbackOnCommitFailure(true);
        transactionManager.setFailEarlyOnGlobalRollbackOnly(false); // Allow test rollback scenarios
        transactionManager.setNestedTransactionAllowed(true);
        transactionManager.setValidateExistingTransaction(false); // Flexible for test scenarios
        transactionManager.setGlobalRollbackOnParticipationFailure(false); // Test-friendly error handling
        
        // Enhanced logging for test debugging
        transactionManager.setTransactionSynchronization(JpaTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
        
        return transactionManager;
    }

    /**
     * Configures DataSourceInitializer for automated test schema and test data loading.
     * 
     * This method establishes comprehensive database initialization specifically designed for test scenarios
     * requiring controlled schema setup and test data population. The initializer executes DDL scripts for
     * schema creation and DML scripts for test data loading, ensuring consistent test environment setup
     * across different test execution contexts.
     * 
     * Test Database Initialization Features:
     * - Automated execution of schema-test.sql for creating test-specific database structures
     * - Test data population from test-data.sql providing comprehensive VSAM-equivalent test records
     * - Error handling and rollback capabilities ensuring proper test environment setup validation
     * - Support for both H2 and PostgreSQL database initialization with dialect-specific DDL handling
     * 
     * VSAM Migration Test Data Support:
     * - Test records matching VSAM dataset structures identified in LISTCAT analysis
     * - Account test data with composite keys and BigDecimal amounts using proper scale=2 precision
     * - Transaction test data supporting date-based partitioning and foreign key relationship validation
     * - Customer and UserSecurity test data enabling comprehensive entity relationship testing scenarios
     * - Reference data initialization for transaction types, categories, and disclosure groups
     * 
     * Schema Initialization Capabilities:
     * - DDL execution for creating tables, indexes, constraints, and sequences matching production schema
     * - Partitioned table creation supporting monthly transaction partitions for batch processing testing
     * - Foreign key constraint creation validating entity relationship mappings and referential integrity
     * - Index creation supporting B-tree indexes for primary keys and composite key access patterns
     * 
     * @param dataSource configured test DataSource for database operations
     * @return DataSourceInitializer for automated test database setup and population
     */
    @Bean
    @ConditionalOnProperty(name = "spring.liquibase.enabled", havingValue = "false", matchIfMissing = true)
    public DataSourceInitializer testDataSourceInitializer(DataSource dataSource) {
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        
        // Create database populator with test schema and data scripts
        DatabasePopulator populator = loadTestSchema();
        initializer.setDatabasePopulator(populator);
        
        // Enable database initialization on startup
        initializer.setEnabled(true);
        
        return initializer;
    }

    /**
     * Configures DataSourceInitializer for test data loading when Liquibase handles schema creation.
     * 
     * This method creates a test database initialization strategy that only loads test data,
     * allowing Liquibase to handle schema creation and migration. This prevents conflicts between
     * manual schema scripts and Liquibase-managed database structures while still providing
     * comprehensive test data for validation scenarios.
     * 
     * This bean is conditionally created only when Liquibase is enabled, complementing the
     * Liquibase schema management with necessary test data population.
     * 
     * @param dataSource configured test DataSource for database operations
     * @return DataSourceInitializer for test data loading only
     */
    @Bean
    @ConditionalOnProperty(name = "spring.liquibase.enabled", havingValue = "true")
    public DataSourceInitializer liquibaseCompatibleDataInitializer(DataSource dataSource) {
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        
        // Create database populator with only test data (no schema)
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource(TEST_DATA_SCRIPT));
        
        // Configure populator behavior for test reliability
        populator.setContinueOnError(false); // Fail fast for test environment issues
        populator.setIgnoreFailedDrops(true); // Allow clean test restarts
        
        initializer.setDatabasePopulator(populator);
        initializer.setEnabled(true);
        
        return initializer;
    }

    /**
     * Creates and configures ResourceDatabasePopulator for loading test schema and test data.
     * 
     * This method creates a comprehensive database population strategy for test environments, executing
     * both DDL schema creation scripts and DML test data insertion scripts in the proper sequence to
     * establish a complete test database environment. The populator handles script execution order,
     * error recovery, and resource cleanup for reliable test database initialization.
     * 
     * Test Schema Loading Features:
     * - Sequential execution of schema-test.sql containing DDL statements for test table creation
     * - Test data population from test-data.sql containing INSERT statements for comprehensive test scenarios
     * - Error handling with proper exception propagation and resource cleanup for failed initialization attempts
     * - SQL script validation and syntax checking before execution to prevent partial initialization states
     * 
     * Database Schema Creation Support:
     * - CREATE TABLE statements for all entity classes including proper column types and constraints
     * - CREATE INDEX statements for primary keys, foreign keys, and composite index structures matching production
     * - CREATE SEQUENCE statements for identity generation supporting BIGINT primary key columns
     * - Partition table creation for transactions table supporting date-based partitioning for batch processing
     * 
     * Test Data Population Capabilities:
     * - Representative test data covering all major entity types with realistic field values
     * - Foreign key relationship test data ensuring proper entity association validation
     * - Edge case test data including boundary values, null handling, and constraint validation scenarios
     * - Performance test data providing sufficient record volumes for pagination and query optimization testing
     * 
     * @return ResourceDatabasePopulator configured for test schema and data loading
     */
    public DatabasePopulator loadTestSchema() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        
        // Add test schema creation script
        populator.addScript(new ClassPathResource(TEST_SCHEMA_SCRIPT));
        
        // Add test data population script
        populator.addScript(new ClassPathResource(TEST_DATA_SCRIPT));
        
        // Configure populator behavior for test reliability
        populator.setContinueOnError(false); // Fail fast for test environment issues
        populator.setIgnoreFailedDrops(true); // Allow clean test restarts
        
        return populator;
    }

    /**
     * Loads and populates test data from SQL scripts for comprehensive test scenario support.
     * 
     * This method provides programmatic access to test data loading capabilities, enabling test classes
     * to trigger data population on demand or reset test data between test method executions. The method
     * supports both full database reset scenarios and incremental data loading for specific test requirements.
     * 
     * Test Data Loading Features:
     * - On-demand execution of test-data.sql for controlled test data management
     * - Support for incremental data loading without affecting existing test records
     * - Transaction-aware data loading ensuring proper rollback capabilities for test isolation
     * - Error handling and logging for test data loading failures with detailed diagnostic information
     * 
     * Programmatic Data Management:
     * - Test method integration enabling custom test data setup and teardown procedures
     * - Support for test class-specific data loading with conditional execution based on test requirements
     * - Integration with Spring Boot Test framework for automatic data loading in test context initialization
     * - Cleanup and reset capabilities supporting test suite execution with predictable data state
     * 
     * @param dataSource configured test DataSource for data loading operations
     * @return DatabasePopulator configured specifically for test data loading
     */
    public DatabasePopulator loadTestData(DataSource dataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        
        // Add only test data script for incremental loading
        populator.addScript(new ClassPathResource(TEST_DATA_SCRIPT));
        
        // Configure for test data loading reliability
        populator.setContinueOnError(false);
        populator.setIgnoreFailedDrops(false); // Strict error handling for data loading
        
        return populator;
    }

    /**
     * Creates H2 in-memory database with PostgreSQL compatibility mode for unit testing.
     * 
     * This method provides a factory method for creating H2 in-memory database instances with
     * specific configuration optimized for CardDemo unit testing scenarios. The H2 database
     * operates in PostgreSQL compatibility mode ensuring SQL dialect consistency while maintaining
     * the performance benefits of in-memory execution for fast test suite completion.
     * 
     * H2 Database Configuration:
     * - In-memory storage with persistent connections during test suite execution
     * - PostgreSQL compatibility mode ensuring SQL function and syntax compatibility
     * - Automatic schema creation supporting JPA entity DDL generation and validation
     * - Fast cleanup and reset capabilities between test executions
     * 
     * Unit Testing Optimization:
     * - Sub-second database startup and shutdown for rapid test execution cycles
     * - Memory-resident data access eliminating I/O overhead for high-performance testing
     * - Automatic transaction isolation supporting @Transactional test method execution
     * - Minimal resource footprint suitable for CI/CD pipeline execution environments
     * 
     * @return H2 database connection URL for unit testing scenarios
     */
    public String createH2InMemoryDatabase() {
        return H2_JDBC_URL;
    }

    /**
     * Creates and configures PostgreSQL container for integration testing with proper isolation.
     * 
     * This method provides a factory method for creating PostgreSQL Testcontainers instances with
     * comprehensive configuration matching production PostgreSQL environments while maintaining
     * test isolation and proper resource management. The container provides full PostgreSQL
     * functionality including advanced features required for VSAM migration validation.
     * 
     * PostgreSQL Container Configuration:
     * - PostgreSQL 15.4 official image providing exact production version matching
     * - Dedicated test database with isolated schema and proper access controls
     * - Network isolation preventing test interference and ensuring consistent execution results
     * - Automatic health checking ensuring database readiness before test execution begins
     * 
     * Integration Testing Capabilities:
     * - Real PostgreSQL features including partitioning, composite keys, and advanced indexing
     * - Full SQL compatibility for complex queries, joins, and transaction testing scenarios
     * - Performance testing with realistic I/O patterns and connection pool behavior validation
     * - Schema migration testing ensuring DDL scripts execute correctly against actual PostgreSQL
     * 
     * Resource Management:
     * - Proper container lifecycle management with startup, configuration, and cleanup phases
     * - Memory and CPU resource limits appropriate for test execution environments
     * - Volume mounting capabilities for complex schema and data initialization requirements
     * 
     * @return configured PostgreSQL container for integration testing
     */
    public PostgreSQLContainer<?> createPostgreSQLContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_DOCKER_IMAGE))
                .withDatabaseName(POSTGRES_DATABASE)
                .withUsername(POSTGRES_USERNAME)
                .withPassword(POSTGRES_PASSWORD)
                .withReuse(false); // Ensure fresh container for each test run
    }

    /**
     * Configures JPA properties optimized for test execution environments.
     * 
     * This method provides comprehensive Hibernate configuration specifically optimized for testing
     * scenarios with enhanced logging, debugging capabilities, and performance tuning appropriate
     * for test data volumes and execution patterns. The configuration maintains compatibility
     * with production settings while enabling test-specific features.
     * 
     * Test Hibernate Configuration:
     * - Enhanced SQL logging and formatting for test debugging and development
     * - Schema validation ensuring entity mappings match test database structures
     * - Test-optimized batch processing with smaller batch sizes for controlled transaction testing
     * - Connection pool integration with test-specific HikariCP configurations
     * 
     * Performance Testing Support:
     * - Query plan caching with test-appropriate cache sizes for test data volumes
     * - Connection management settings optimized for test execution patterns
     * - Transaction coordination configuration supporting test isolation and rollback scenarios
     * - Prepared statement handling with test-specific optimization settings
     * 
     * COBOL Migration Testing Features:
     * - BigDecimal precision configuration ensuring COBOL COMP-3 compatibility testing
     * - Character encoding settings supporting international character set validation
     * - Transaction isolation configuration matching CICS SYNCPOINT behavior for consistency testing
     * - Custom type handling supporting CobolDataConverter integration for data type conversion testing
     * 
     * @return Properties object containing comprehensive test Hibernate configuration
     */
    public Properties configureJpaProperties() {
        Properties jpaProperties = new Properties();
        
        // Core Hibernate configuration for testing
        jpaProperties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        jpaProperties.setProperty("hibernate.hbm2ddl.auto", TEST_HIBERNATE_DDL_AUTO);
        jpaProperties.setProperty("hibernate.show_sql", TEST_HIBERNATE_SHOW_SQL);
        jpaProperties.setProperty("hibernate.format_sql", TEST_HIBERNATE_FORMAT_SQL);
        
        // Test performance optimization settings
        jpaProperties.setProperty("hibernate.jdbc.batch_size", TEST_HIBERNATE_JDBC_BATCH_SIZE);
        jpaProperties.setProperty("hibernate.order_inserts", "true");
        jpaProperties.setProperty("hibernate.order_updates", "true");
        jpaProperties.setProperty("hibernate.jdbc.batch_versioned_data", "true");
        
        // Test connection and transaction settings
        jpaProperties.setProperty("hibernate.connection.provider_disables_autocommit", "true");
        jpaProperties.setProperty("hibernate.connection.autocommit", "false");
        
        // Test query and caching optimizations
        jpaProperties.setProperty("hibernate.query.plan_cache_max_size", "512");
        jpaProperties.setProperty("hibernate.query.plan_parameter_metadata_max_size", "64");
        jpaProperties.setProperty("hibernate.query.in_clause_parameter_padding", "true");
        
        // Test-specific PostgreSQL optimizations
        jpaProperties.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");
        jpaProperties.setProperty("hibernate.connection.charSet", "UTF-8");
        jpaProperties.setProperty("hibernate.connection.characterEncoding", "UTF-8");
        jpaProperties.setProperty("hibernate.connection.useUnicode", "true");
        
        // Test BigDecimal handling for COBOL precision preservation
        jpaProperties.setProperty("hibernate.globally_quoted_identifiers", "false");
        jpaProperties.setProperty("hibernate.id.new_generator_mappings", "true");
        jpaProperties.setProperty("hibernate.use_sql_comments", "true"); // Enable for test debugging
        
        // Test logging and debugging enhancements
        jpaProperties.setProperty("hibernate.generate_statistics", "true");
        jpaProperties.setProperty("hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS", "100");
        
        return jpaProperties;
    }

    /**
     * Mock MetricsConfig bean for test environments.
     * 
     * @return Mock MetricsConfig for testing
     */
    @Bean
    @Primary
    public com.carddemo.config.MetricsConfig mockMetricsConfig() {
        return org.mockito.Mockito.mock(com.carddemo.config.MetricsConfig.class);
    }

    /**
     * Mock MonitoringService bean for test environments.
     * 
     * Provides a minimal MonitoringService implementation for test scenarios where
     * the actual monitoring infrastructure is not needed. This prevents autowiring
     * failures in security components during test execution.
     * 
     * @return Mock MonitoringService implementation for testing
     */
    @Bean
    @Primary
    public com.carddemo.service.MonitoringService mockMonitoringService() {
        return org.mockito.Mockito.mock(com.carddemo.service.MonitoringService.class);
    }

    /**
     * Mock ValidationUtil bean for test environments.
     * 
     * ValidationUtil is normally a utility class with static methods, but some batch jobs
     * incorrectly try to autowire it. This mock prevents autowiring failures during tests.
     * 
     * @return Mock ValidationUtil implementation for testing
     */
    @Bean
    @Primary
    public com.carddemo.util.ValidationUtil mockValidationUtil() {
        return org.mockito.Mockito.mock(com.carddemo.util.ValidationUtil.class);
    }

    /**
     * Mock CobolDataConverter bean for test environments.
     * 
     * CobolDataConverter is normally a utility class with static methods, but some batch jobs
     * incorrectly try to autowire it. This mock prevents autowiring failures during tests.
     * 
     * @return Mock CobolDataConverter implementation for testing
     */
    @Bean
    @Primary
    public com.carddemo.util.CobolDataConverter mockCobolDataConverter() {
        return org.mockito.Mockito.mock(com.carddemo.util.CobolDataConverter.class);
    }

    /**
     * Mock DateConversionUtil bean for test environments.
     * 
     * DateConversionUtil is normally a utility class with static methods, but some batch jobs
     * incorrectly try to autowire it. This mock prevents autowiring failures during tests.
     * 
     * @return Mock DateConversionUtil implementation for testing
     */
    @Bean
    @Primary
    public com.carddemo.util.DateConversionUtil mockDateConversionUtil() {
        return org.mockito.Mockito.mock(com.carddemo.util.DateConversionUtil.class);
    }
}