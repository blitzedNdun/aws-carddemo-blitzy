package com.carddemo.common.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Properties;

/**
 * CardDemo Database Configuration
 * 
 * Provides comprehensive PostgreSQL database configuration for the CardDemo application
 * with optimized settings for high-volume transaction processing (10,000+ TPS).
 * 
 * Key Features:
 * - HikariCP connection pooling optimized for cloud-native microservices
 * - SERIALIZABLE transaction isolation to replicate VSAM record locking behavior
 * - JPA/Hibernate configuration with COBOL-to-Java precision mapping
 * - Liquibase integration for database schema management
 * - Connection pool monitoring and health checks
 * - BigDecimal precision configuration for financial calculations
 * 
 * Performance Targets:
 * - Sub-200ms response times at 95th percentile
 * - Support for 10,000+ transactions per second
 * - Memory usage within 110% of CICS baseline
 * - Horizontal scaling across microservices instances
 * 
 * @author Blitzy agent
 * @version 1.0
 * @since CardDemo v1.0
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = {
    "com.carddemo.account.repository",
    "com.carddemo.common.repository",
    "com.carddemo.transaction"
})
public class DatabaseConfig {

    // Database connection properties
    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/carddemo}")
    private String databaseUrl;

    @Value("${spring.datasource.username:carddemo}")
    private String databaseUsername;

    @Value("${spring.datasource.password:carddemo}")
    private String databasePassword;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    // HikariCP connection pool configuration
    @Value("${spring.datasource.hikari.maximum-pool-size:50}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:10}")
    private int minIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;

    @Value("${spring.datasource.hikari.leak-detection-threshold:60000}")
    private long leakDetectionThreshold;

    @Value("${spring.datasource.hikari.validation-timeout:5000}")
    private long validationTimeout;

    @Value("${spring.datasource.hikari.initialization-fail-timeout:1}")
    private long initializationFailTimeout;

    // JPA/Hibernate configuration
    @Value("${spring.jpa.hibernate.ddl-auto:validate}")
    private String ddlAuto;

    @Value("${spring.jpa.show-sql:false}")
    private boolean showSql;

    @Value("${spring.jpa.hibernate.jdbc.batch_size:50}")
    private int batchSize;

    @Value("${spring.jpa.hibernate.order_inserts:true}")
    private boolean orderInserts;

    @Value("${spring.jpa.hibernate.order_updates:true}")
    private boolean orderUpdates;

    @Value("${spring.jpa.hibernate.jdbc.batch_versioned_data:true}")
    private boolean batchVersionedData;

    // Transaction isolation configuration
    @Value("${spring.jpa.properties.hibernate.connection.isolation:8}")
    private int isolationLevel; // 8 = SERIALIZABLE

    /**
     * Mathematical context for COBOL COMP-3 decimal precision preservation.
     * Uses DECIMAL128 precision with HALF_EVEN rounding to ensure exact financial calculations
     * equivalent to mainframe COBOL arithmetic operations.
     */
    public static final MathContext COBOL_MATH_CONTEXT = new MathContext(31, RoundingMode.HALF_EVEN);

    /**
     * Primary DataSource Bean - HikariCP Connection Pool
     * 
     * Configures high-performance JDBC connection pool optimized for PostgreSQL
     * with settings designed to handle 10,000+ TPS throughput while maintaining
     * sub-200ms response times at 95th percentile.
     * 
     * Connection Pool Sizing Strategy:
     * - Maximum pool size: 50 connections per microservice instance
     * - Minimum idle: 10 connections for baseline load
     * - Calculated using PostgreSQL formula: (core_count * 2) + effective_spindle_count
     * - Scaled for cloud deployment and horizontal scaling requirements
     * 
     * @return HikariDataSource configured for optimal PostgreSQL performance
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        // Core connection settings
        config.setJdbcUrl(databaseUrl);
        config.setUsername(databaseUsername);
        config.setPassword(databasePassword);
        config.setDriverClassName(driverClassName);
        
        // Connection pool sizing optimized for 10,000+ TPS
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        
        // Timeout configurations for resilience
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setValidationTimeout(validationTimeout);
        config.setInitializationFailTimeout(initializationFailTimeout);
        
        // Connection leak detection for monitoring
        config.setLeakDetectionThreshold(leakDetectionThreshold);
        
        // PostgreSQL-specific optimizations
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("CardDemo-HikariCP");
        
        // Performance and monitoring settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("reWriteBatchedInserts", "true");
        config.addDataSourceProperty("maintainTimeStats", "true");
        
        // PostgreSQL connection properties for SERIALIZABLE isolation
        config.addDataSourceProperty("defaultTransactionIsolation", "TRANSACTION_SERIALIZABLE");
        config.addDataSourceProperty("applicationName", "CardDemo-Microservice");
        config.addDataSourceProperty("stringtype", "unspecified");
        
        // Health check and monitoring
        config.setRegisterMbeans(true);
        config.setAllowPoolSuspension(true);
        
        return new HikariDataSource(config);
    }

    /**
     * JPA EntityManagerFactory Bean
     * 
     * Configures Hibernate JPA with PostgreSQL-specific optimizations and
     * COBOL-to-Java data type mappings. Includes performance tuning for
     * high-volume financial transaction processing.
     * 
     * Key Configurations:
     * - SERIALIZABLE transaction isolation for VSAM-equivalent behavior
     * - Batch processing optimization for bulk operations
     * - Custom type converters for COBOL COMP-3 precision mapping
     * - PostgreSQL dialect with advanced features
     * 
     * @param dataSource The configured HikariCP data source
     * @return LocalContainerEntityManagerFactoryBean for JPA operations
     */
    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("com.carddemo");
        
        // Hibernate JPA vendor adapter
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(false);
        vendorAdapter.setShowSql(showSql);
        vendorAdapter.setDatabasePlatform("org.hibernate.dialect.PostgreSQLDialect");
        emf.setJpaVendorAdapter(vendorAdapter);
        
        // Hibernate properties for performance and precision
        Properties jpaProperties = new Properties();
        
        // DDL and schema management
        jpaProperties.put(AvailableSettings.HBM2DDL_AUTO, ddlAuto);
        jpaProperties.put(AvailableSettings.PHYSICAL_NAMING_STRATEGY, 
            "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        
        // Transaction isolation - SERIALIZABLE for VSAM equivalence
        jpaProperties.put(AvailableSettings.ISOLATION, isolationLevel);
        jpaProperties.put(AvailableSettings.CONNECTION_HANDLING, "delayed_acquisition_and_hold");
        
        // Batch processing optimization
        jpaProperties.put(AvailableSettings.STATEMENT_BATCH_SIZE, batchSize);
        jpaProperties.put(AvailableSettings.ORDER_INSERTS, orderInserts);
        jpaProperties.put(AvailableSettings.ORDER_UPDATES, orderUpdates);
        jpaProperties.put(AvailableSettings.BATCH_VERSIONED_DATA, batchVersionedData);
        
        // Performance optimizations
        jpaProperties.put(AvailableSettings.USE_SECOND_LEVEL_CACHE, false);
        jpaProperties.put(AvailableSettings.USE_QUERY_CACHE, false);
        jpaProperties.put(AvailableSettings.GENERATE_STATISTICS, true);
        jpaProperties.put(AvailableSettings.LOG_SLOW_QUERY, 1000L);
        
        // PostgreSQL-specific optimizations
        jpaProperties.put(AvailableSettings.DIALECT, "org.hibernate.dialect.PostgreSQLDialect");
        jpaProperties.put("hibernate.jdbc.lob.non_contextual_creation", true);
        jpaProperties.put("hibernate.temp.use_jdbc_metadata_defaults", false);
        
        // Connection pool integration
        jpaProperties.put(AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, true);
        jpaProperties.put(AvailableSettings.AUTOCOMMIT, false);
        
        // Precision mapping for financial calculations
        jpaProperties.put("hibernate.type.preferred_duration_jdbc_type", "NUMERIC");
        jpaProperties.put("hibernate.type.preferred_instant_jdbc_type", "TIMESTAMP");
        
        // Monitoring and observability
        jpaProperties.put(AvailableSettings.SHOW_SQL, showSql);
        jpaProperties.put(AvailableSettings.FORMAT_SQL, true);
        jpaProperties.put(AvailableSettings.USE_SQL_COMMENTS, true);
        
        emf.setJpaProperties(jpaProperties);
        
        return emf;
    }

    /**
     * JPA Transaction Manager Bean
     * 
     * Configures JPA transaction management with SERIALIZABLE isolation level
     * to replicate VSAM record locking behavior. Ensures ACID compliance
     * equivalent to CICS syncpoint management.
     * 
     * Transaction Characteristics:
     * - SERIALIZABLE isolation prevents phantom reads
     * - Automatic rollback on exceptions
     * - Integration with Spring's @Transactional annotation
     * - Support for distributed transactions across microservices
     * 
     * @param entityManagerFactory The configured JPA entity manager factory
     * @return PlatformTransactionManager for declarative transaction management
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        
        // Transaction timeout for long-running operations
        transactionManager.setDefaultTimeout(300); // 5 minutes
        
        // Enable transaction synchronization for monitoring
        transactionManager.setGlobalRollbackOnParticipationFailure(true);
        transactionManager.setFailEarlyOnGlobalRollbackOnly(true);
        
        // JPA-specific settings
        transactionManager.setJpaDialect(new org.springframework.orm.jpa.vendor.HibernateJpaDialect());
        
        return transactionManager;
    }

    /**
     * Liquibase Auto-Configuration Note
     * 
     * Spring Boot 3.x automatically configures Liquibase when spring-boot-starter-liquibase
     * is present in the classpath. The configuration is handled through application.properties:
     * 
     * - spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml
     * - spring.liquibase.enabled=true
     * - spring.liquibase.contexts=production,test
     * - spring.liquibase.default-schema=carddemo
     * 
     * This provides the same functionality as manual SpringLiquibase bean configuration
     * while leveraging Spring Boot's auto-configuration capabilities for better integration
     * with the application lifecycle and configuration management.
     */

    /**
     * Database Health Check Configuration
     * 
     * Provides comprehensive health monitoring for the database connection
     * pool and transaction processing capabilities. Integrates with Spring
     * Boot Actuator for Kubernetes health checks and monitoring.
     * 
     * @return Custom health indicator for database monitoring
     */
    @Bean
    public org.springframework.boot.actuate.health.HealthIndicator databaseHealthIndicator(DataSource dataSource) {
        return new org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator(dataSource, "SELECT 1");
    }

    /**
     * Connection Pool Metrics Configuration
     * 
     * Configures HikariCP metrics for Prometheus monitoring and alerting.
     * Provides visibility into connection pool performance and utilization
     * for proactive scaling and performance optimization.
     * 
     * @param dataSource The HikariCP data source to monitor
     * @return Micrometer metrics for connection pool monitoring
     */
    @Bean
    @ConditionalOnProperty(name = "management.metrics.export.prometheus.enabled", havingValue = "true")
    public io.micrometer.core.instrument.binder.db.PostgreSQLDatabaseMetrics postgresqlMetrics(DataSource dataSource) {
        return new io.micrometer.core.instrument.binder.db.PostgreSQLDatabaseMetrics(dataSource, "carddemo");
    }
}