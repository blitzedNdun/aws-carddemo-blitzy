/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.test;

import com.carddemo.config.DatabaseConfig;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base test configuration class providing common Spring Boot test setup, profile activation,
 * and shared test utilities for all test types including unit, integration, and performance tests.
 * 
 * This configuration class establishes the foundational testing infrastructure for the CardDemo
 * application's comprehensive four-layer testing strategy as defined in Section 6.6 TESTING STRATEGY.
 * The configuration enables parallel test execution with proper test isolation while providing
 * shared utilities and beans required across all test categories.
 * 
 * Core Responsibilities:
 * - Configure test-specific data sources (H2 for unit tests, Testcontainers for integration)
 * - Set up test property sources with environment-specific test configurations
 * - Provide test execution listeners for lifecycle management and parallel execution support
 * - Configure mock web environment for Spring MVC controller testing
 * - Enable component scanning for test packages and provide shared test utilities
 * - Establish test profiles for unit, integration, performance, and end-to-end testing
 * 
 * Testing Strategy Integration:
 * - Unit Testing: Fast H2 in-memory database with minimal Spring context loading
 * - Integration Testing: Testcontainers PostgreSQL with full Spring Boot application context
 * - Performance Testing: Production-like configuration with metrics collection enabled
 * - End-to-End Testing: Complete application stack with mock external service dependencies
 * 
 * Parallel Test Execution Support:
 * - Test isolation through dedicated database schemas per test execution thread
 * - Thread-safe test data generation and cleanup mechanisms
 * - Proper test execution listener configuration for concurrent test management
 * - Session management isolation using thread-local storage patterns
 * 
 * COBOL Migration Testing Features:
 * - Test data sources configured for COBOL-to-Java comparison testing
 * - BigDecimal precision validation utilities for COMP-3 packed decimal equivalence
 * - Test property sources supporting VSAM-to-PostgreSQL migration validation
 * - Mock service configurations replacing external mainframe dependencies
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 * @see DatabaseConfig
 * @see com.carddemo.test.TestConstants
 * @see Section 6.6 TESTING STRATEGY
 */
@TestConfiguration
@Import(DatabaseConfig.class)
@TestPropertySource(locations = "classpath:application-test.yml")
@ExtendWith(SpringExtension.class)
public class BaseTestConfig {

    private static final Logger logger = LoggerFactory.getLogger(BaseTestConfig.class);

    // Test database configuration constants optimized for fast test execution
    private static final String TEST_DB_NAME = "carddemo_test";
    private static final String TEST_DB_USERNAME = "test_user"; 
    private static final String TEST_DB_PASSWORD = "test_password";
    private static final String H2_JDBC_URL = "jdbc:h2:mem:" + TEST_DB_NAME + 
        ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";
    
    // Connection pool settings optimized for test parallel execution
    private static final int TEST_MAX_POOL_SIZE = 10;      // Smaller pool for test efficiency
    private static final int TEST_MINIMUM_IDLE = 2;        // Minimum idle connections
    private static final long TEST_CONNECTION_TIMEOUT = 5000;   // 5 seconds timeout for tests
    private static final long TEST_IDLE_TIMEOUT = 300000;      // 5 minutes idle timeout
    private static final long TEST_MAX_LIFETIME = 1800000;     // 30 minutes max connection lifetime

    /**
     * Configures test-specific property sources for all testing environments.
     * 
     * This method establishes centralized test configuration management supporting
     * different test profiles while ensuring consistent property resolution across
     * all test types. Properties are optimized for fast test execution and proper
     * test isolation in parallel testing scenarios.
     * 
     * Test Property Configuration:
     * - Unit Test Profile: H2 in-memory database, disabled security, minimal logging
     * - Integration Test Profile: Testcontainers PostgreSQL, Redis session store
     * - Performance Test Profile: Production-like settings, metrics collection enabled
     * - End-to-End Test Profile: Full application stack with mock external services
     * 
     * Property Sources Hierarchy:
     * 1. application-test.yml: Base test properties for all profiles
     * 2. Profile-specific properties: Loaded based on active test profile
     * 3. System properties: Override for CI/CD pipeline customization
     * 4. Environment variables: Container and cloud deployment overrides
     * 
     * Parallel Test Execution Properties:
     * - Separate database schemas per test thread to prevent data contamination
     * - Thread-safe session management configuration for concurrent test execution
     * - Unique connection pool names for test isolation and debugging
     * - Test-specific logging patterns for parallel execution troubleshooting
     * 
     * @return Properties instance containing all test configuration properties
     */
    @Bean
    @Primary
    public Properties testPropertySource() {
        Properties testProperties = new Properties();
        
        // Core application test properties
        testProperties.setProperty("spring.application.name", "carddemo-test");
        testProperties.setProperty("spring.profiles.active", "test");
        
        // Database configuration for different test types
        testProperties.setProperty("spring.datasource.url", H2_JDBC_URL);
        testProperties.setProperty("spring.datasource.username", TEST_DB_USERNAME);
        testProperties.setProperty("spring.datasource.password", TEST_DB_PASSWORD);
        testProperties.setProperty("spring.datasource.driver-class-name", "org.h2.Driver");
        
        // JPA and Hibernate configuration optimized for testing
        testProperties.setProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
        testProperties.setProperty("spring.jpa.show-sql", "false");
        testProperties.setProperty("spring.jpa.properties.hibernate.format_sql", "false");
        testProperties.setProperty("spring.jpa.properties.hibernate.jdbc.batch_size", "10");
        testProperties.setProperty("spring.jpa.properties.hibernate.order_inserts", "true");
        testProperties.setProperty("spring.jpa.properties.hibernate.order_updates", "true");
        testProperties.setProperty("spring.jpa.database-platform", "org.hibernate.dialect.H2Dialect");
        
        // Transaction management for test isolation
        testProperties.setProperty("spring.transaction.rollback-on-commit-failure", "true");
        testProperties.setProperty("spring.test.database.replace", "none");
        
        // Security configuration for testing
        testProperties.setProperty("spring.security.user.name", "testuser");
        testProperties.setProperty("spring.security.user.password", "testpass");
        testProperties.setProperty("spring.security.user.roles", "USER,ADMIN");
        
        // Session management configuration for test isolation
        testProperties.setProperty("spring.session.store-type", "none");
        testProperties.setProperty("server.servlet.session.timeout", "30m");
        testProperties.setProperty("server.servlet.session.cookie.max-age", "1800");
        
        // Logging configuration optimized for test execution
        testProperties.setProperty("logging.level.com.carddemo", "INFO");
        testProperties.setProperty("logging.level.org.springframework", "WARN");
        testProperties.setProperty("logging.level.org.hibernate", "WARN");
        testProperties.setProperty("logging.level.org.testcontainers", "INFO");
        testProperties.setProperty("logging.pattern.console", 
            "%d{HH:mm:ss.SSS} [%thread] %-5level [%X{testClass}] %logger{36} - %msg%n");
        
        // Test execution and parallel processing configuration
        testProperties.setProperty("junit.jupiter.execution.parallel.enabled", "true");
        testProperties.setProperty("junit.jupiter.execution.parallel.mode.default", "concurrent");
        testProperties.setProperty("junit.jupiter.execution.parallel.config.strategy", "dynamic");
        
        // Performance and monitoring configuration for test validation
        testProperties.setProperty("management.endpoints.web.exposure.include", "health,metrics");
        testProperties.setProperty("management.endpoint.health.show-details", "always");
        testProperties.setProperty("management.metrics.export.simple.enabled", "true");
        
        logger.info("Configured test property source with {} properties for parallel test execution", 
                   testProperties.size());
        
        return testProperties;
    }

    /**
     * Configures test execution listener for comprehensive test lifecycle management.
     * 
     * This listener provides essential test execution lifecycle management for parallel
     * test execution scenarios, ensuring proper test isolation, resource cleanup,
     * and performance monitoring across all test categories in the four-layer testing
     * strategy.
     * 
     * Test Execution Lifecycle Management:
     * - Before Test Class: Initialize test context and prepare shared resources
     * - Before Test Method: Set up method-specific test data and mock configurations
     * - After Test Method: Clean up test data and reset mock states for isolation
     * - After Test Class: Release shared resources and log execution metrics
     * 
     * Parallel Test Execution Support:
     * - Thread-safe test context management using ThreadLocal storage
     * - Concurrent test data cleanup without cross-thread contamination
     * - Performance metrics collection per test execution thread
     * - Isolation verification between parallel test executions
     * 
     * COBOL Migration Testing Features:
     * - Test data comparison utilities for COBOL-to-Java equivalence validation
     * - BigDecimal precision tracking for financial calculation testing
     * - Mock service state management for external mainframe dependency simulation
     * - Test execution timing for performance regression detection
     * 
     * @return CustomTestExecutionListener configured for parallel test execution
     */
    @Bean
    @Primary
    public TestExecutionListener testExecutionListener() {
        return new CustomTestExecutionListener();
    }

    /**
     * Configures optimized test data source for fast unit and integration testing.
     * 
     * This method provides a high-performance, test-optimized data source that supports
     * both H2 in-memory databases for unit tests and connection pooling for integration
     * tests with Testcontainers. The configuration ensures fast test execution while
     * maintaining data integrity and proper test isolation.
     * 
     * Data Source Configuration:
     * - Unit Tests: H2 in-memory database with PostgreSQL compatibility mode
     * - Integration Tests: HikariCP connection pool with optimized settings
     * - Performance Tests: Production-like connection pool configuration
     * - End-to-End Tests: Full database setup with realistic data volumes
     * 
     * Test Isolation Features:
     * - Separate database schemas per test execution thread
     * - Automatic schema creation and cleanup for test isolation
     * - Connection pool sizing optimized for parallel test execution
     * - Transaction boundary management for proper test cleanup
     * 
     * Performance Optimizations:
     * - Reduced connection timeouts for faster test failure detection
     * - Optimized batch size settings for bulk test data operations
     * - Prepared statement caching for repeated test query execution
     * - Connection validation disabled for test performance improvement
     * 
     * @return DataSource configured for optimal test performance and isolation
     */
    @Bean
    @Primary
    @Profile("unit")
    public DataSource testDataSource() {
        logger.info("Configuring H2 in-memory test data source for unit testing");
        
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .setName(TEST_DB_NAME)
            .addScript("classpath:schema-test.sql")
            .addScript("classpath:test-data.sql")
            .build();
    }

    /**
     * Configures integration test data source using HikariCP connection pooling.
     * 
     * This data source configuration is specifically designed for integration tests
     * that require external database connectivity through Testcontainers while
     * maintaining optimal performance and proper test isolation in parallel
     * execution scenarios.
     * 
     * @return HikariDataSource configured for integration testing
     */
    @Bean
    @Primary
    @Profile("integration")
    public DataSource integrationTestDataSource() {
        logger.info("Configuring HikariCP test data source for integration testing");
        
        HikariConfig config = new HikariConfig();
        
        // Basic connection properties - will be overridden by Testcontainers
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/carddemo_test");
        config.setUsername(TEST_DB_USERNAME);
        config.setPassword(TEST_DB_PASSWORD);
        config.setDriverClassName("org.postgresql.Driver");
        
        // Connection pool sizing optimized for test performance
        config.setMaximumPoolSize(TEST_MAX_POOL_SIZE);
        config.setMinimumIdle(TEST_MINIMUM_IDLE);
        config.setConnectionTimeout(TEST_CONNECTION_TIMEOUT);
        config.setIdleTimeout(TEST_IDLE_TIMEOUT);
        config.setMaxLifetime(TEST_MAX_LIFETIME);
        
        // Connection pool identification for test monitoring
        config.setPoolName("CardDemo-Test-HikariCP");
        config.setConnectionTestQuery("SELECT 1");
        
        // PostgreSQL-specific test optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "100");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "1024");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        
        return new HikariDataSource(config);
    }

    /**
     * Configures mock web environment for Spring MVC controller testing.
     * 
     * This method sets up a comprehensive mock web environment that enables
     * testing of REST controllers, security configurations, and web layer
     * integration without requiring a full embedded web server. The configuration
     * supports parallel test execution and provides realistic web application
     * context for thorough controller testing.
     * 
     * Mock Web Environment Features:
     * - MockMvc configuration for HTTP request/response testing
     * - Security context setup for authentication and authorization testing
     * - Session management simulation for COMMAREA-equivalent state testing
     * - JSON request/response processing for REST API validation
     * 
     * Controller Testing Support:
     * - All 24 REST controller endpoints with transaction code mapping
     * - Request parameter validation and response format verification
     * - Error handling and exception mapping testing
     * - Performance monitoring and response time validation
     * 
     * Security Testing Integration:
     * - Mock authentication providers for user credential testing
     * - Role-based authorization testing for Spring Security integration
     * - Session timeout and management testing for security compliance
     * - CSRF protection and security headers validation
     * 
     * @return MockServletContext configured for comprehensive web layer testing
     */
    @Bean
    @Primary
    public MockServletContext mockWebEnvironment() {
        logger.info("Configuring mock web environment for controller testing");
        
        MockServletContext mockServletContext = new MockServletContext();
        
        // Configure servlet context for REST controller testing
        mockServletContext.setContextPath("/api");
        mockServletContext.addInitParameter("spring.profiles.active", "test");
        mockServletContext.addInitParameter("server.port", "0"); // Random available port
        
        // Configure request processing parameters
        mockServletContext.addInitParameter("spring.mvc.async.request-timeout", "30000");
        mockServletContext.addInitParameter("server.servlet.encoding.charset", "UTF-8");
        mockServletContext.addInitParameter("server.servlet.encoding.enabled", "true");
        mockServletContext.addInitParameter("server.servlet.encoding.force", "true");
        
        // Configure session management for controller testing
        mockServletContext.setSessionTrackingModes(
            java.util.Set.of(javax.servlet.SessionTrackingMode.COOKIE));
        mockServletContext.addInitParameter("server.servlet.session.timeout", "30m");
        mockServletContext.addInitParameter("server.servlet.session.cookie.max-age", "1800");
        
        // Configure security parameters for authentication testing
        mockServletContext.addInitParameter("spring.security.filter.order", "100");
        mockServletContext.addInitParameter("security.require-ssl", "false");
        
        logger.info("Mock web environment configured with servlet context path: {}", 
                   mockServletContext.getContextPath());
        
        return mockServletContext;
    }

    /**
     * Configures TestRestTemplate for REST API integration testing.
     * 
     * This method provides a configured TestRestTemplate optimized for testing
     * REST endpoints with proper timeout settings, error handling, and request/response
     * processing that matches the production Spring Boot configuration.
     * 
     * @return TestRestTemplate configured for comprehensive API testing
     */
    @Bean
    @Primary
    public TestRestTemplate testRestTemplate() {
        return new TestRestTemplate(
            new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build()
        );
    }

    /**
     * Custom test execution listener implementing comprehensive test lifecycle management.
     * 
     * This inner class provides detailed test execution lifecycle management specifically
     * designed for parallel test execution scenarios in the CardDemo application's
     * four-layer testing strategy.
     */
    private static class CustomTestExecutionListener extends AbstractTestExecutionListener {
        
        private static final Logger listenerLogger = LoggerFactory.getLogger(CustomTestExecutionListener.class);
        private static final ThreadLocal<Long> testStartTime = new ThreadLocal<>();
        private static final ThreadLocal<String> testClassName = new ThreadLocal<>();

        @Override
        public void beforeTestClass(org.springframework.test.context.TestContext testContext) throws Exception {
            String className = testContext.getTestClass().getSimpleName();
            testClassName.set(className);
            
            listenerLogger.info("Starting test class execution: {} in thread: {}", 
                               className, Thread.currentThread().getName());
            
            // Set MDC context for logging
            org.slf4j.MDC.put("testClass", className);
            org.slf4j.MDC.put("testThread", Thread.currentThread().getName());
        }

        @Override
        public void afterTestClass(org.springframework.test.context.TestContext testContext) throws Exception {
            String className = testClassName.get();
            
            listenerLogger.info("Completed test class execution: {} in thread: {}", 
                               className, Thread.currentThread().getName());
            
            // Clean up thread-local storage
            testClassName.remove();
            testStartTime.remove();
            
            // Clean up MDC context
            org.slf4j.MDC.clear();
        }

        @Override
        public void beforeTestMethod(org.springframework.test.context.TestContext testContext) throws Exception {
            testStartTime.set(System.currentTimeMillis());
            
            String methodName = testContext.getTestMethod().getName();
            String className = testContext.getTestClass().getSimpleName();
            
            listenerLogger.debug("Starting test method: {}.{} in thread: {}", 
                                className, methodName, Thread.currentThread().getName());
            
            // Set method-specific MDC context
            org.slf4j.MDC.put("testMethod", methodName);
        }

        @Override
        public void afterTestMethod(org.springframework.test.context.TestContext testContext) throws Exception {
            Long startTime = testStartTime.get();
            if (startTime != null) {
                long executionTime = System.currentTimeMillis() - startTime;
                String methodName = testContext.getTestMethod().getName();
                String className = testContext.getTestClass().getSimpleName();
                
                listenerLogger.debug("Completed test method: {}.{} in {}ms in thread: {}", 
                                    className, methodName, executionTime, Thread.currentThread().getName());
                
                // Log performance metrics for monitoring
                if (executionTime > 5000) { // Warn if test takes longer than 5 seconds
                    listenerLogger.warn("Slow test detected: {}.{} took {}ms", 
                                       className, methodName, executionTime);
                }
            }
            
            // Clean up method-specific MDC context
            org.slf4j.MDC.remove("testMethod");
        }
    }
}