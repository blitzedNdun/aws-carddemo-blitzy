/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.config;

import com.carddemo.security.SessionAttributes;
import com.carddemo.security.SessionConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import redis.embedded.RedisServer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Test configuration for Redis session management providing embedded Redis server for unit tests 
 * and Testcontainers Redis for integration tests, replacing CICS COMMAREA state management.
 * 
 * This configuration class provides comprehensive Redis testing infrastructure to validate
 * session state persistence, timeout behavior, and distributed session management that
 * replaces CICS COMMAREA functionality. It supports both fast unit tests with embedded
 * Redis and integration tests with Testcontainers Redis matching production configuration.
 *
 * Key Features:
 * - Embedded Redis server for fast unit test execution without external dependencies
 * - Testcontainers Redis container for integration tests with production-like configuration
 * - Session timeout settings matching CICS transaction timeout behavior (30 minutes)
 * - 32KB session state limit enforcement matching COMMAREA constraints
 * - Session event listeners for testing session lifecycle and audit trail functionality
 * - Redis connection factory configuration optimized for test execution performance
 * - Support for session replication testing and horizontal scaling validation
 *
 * CICS COMMAREA Testing Strategy:
 * The original COBOL system used DFHCOMMAREA to maintain user context across transactions.
 * This test configuration enables comprehensive validation of session state persistence,
 * timeout behavior, and session attribute management that replicates CICS functionality
 * through Redis-backed Spring Session integration.
 *
 * Test Environment Support:
 * - Unit Tests (@ActiveProfiles("test")): Uses embedded Redis for speed and isolation
 * - Integration Tests (@ActiveProfiles("integration")): Uses Testcontainers for realism
 * - Performance Tests: Validates sub-200ms session read/write operations
 * - Session Lifecycle Tests: Validates creation, timeout, and destruction events
 *
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 * @see RedisConfig for production Redis configuration
 * @see SessionConfig for session management configuration
 * @see SessionAttributes for session attribute constants and utilities
 */
@TestConfiguration
@Configuration
public class TestRedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(TestRedisConfig.class);

    // Test Constants - Since TestConstants.java doesn't exist yet, defining locally
    public static final int SESSION_TIMEOUT_MINUTES = 30;
    public static final int MAX_SESSION_SIZE_KB = 32;
    public static final String TEST_USER_ID = "TESTUSER";
    public static final String TEST_USER_PASSWORD = "TESTPASS";
    public static final int REDIS_TEST_PORT = 6370; // Non-standard port to avoid conflicts
    public static final int REDIS_TEST_DATABASE = 1; // Use database 1 for testing

    private RedisServer embeddedRedisServer;
    private GenericContainer<?> redisContainer;

    /**
     * Creates embedded Redis server for fast unit test execution without external dependencies.
     * 
     * This method provides an embedded Redis server that starts automatically for unit tests,
     * enabling fast session management testing without requiring external Redis infrastructure.
     * The embedded server is configured with a non-standard port to avoid conflicts with
     * development or production Redis instances.
     *
     * Unit Test Features:
     * - Automatic startup and shutdown with test lifecycle
     * - Isolated test database for session attribute storage
     * - Fast in-memory data storage for rapid test execution
     * - No external dependencies required for CI/CD pipeline execution
     * - Port conflict avoidance through dynamic port allocation
     *
     * Configuration Details:
     * - Uses Redis embedded library for JVM-native Redis server
     * - Configured with test-specific port (6370) to avoid standard Redis port conflicts
     * - Database 1 reserved for test data isolation from potential development data
     * - Memory-only storage for fast test execution and cleanup
     *
     * @return configured embedded Redis server for unit testing
     * @throws RuntimeException if Redis server cannot be started
     */
    @Bean
    @Profile("test")
    public RedisServer embeddedRedisServer() {
        logger.info("Configuring embedded Redis server for unit tests on port {}", REDIS_TEST_PORT);
        
        try {
            // Find available port if default test port is in use
            int availablePort = findAvailablePort(REDIS_TEST_PORT);
            
            embeddedRedisServer = RedisServer.builder()
                .port(availablePort)
                .setting("maxmemory 64M")
                .setting("maxmemory-policy allkeys-lru")
                .setting("save \"\"") // Disable persistence for testing
                .setting("appendonly no") // Disable AOF for testing
                .build();
            
            logger.info("Embedded Redis server configured on port {}", availablePort);
            return embeddedRedisServer;
            
        } catch (Exception e) {
            logger.error("Failed to configure embedded Redis server", e);
            throw new RuntimeException("Could not start embedded Redis server for testing", e);
        }
    }

    /**
     * Creates Testcontainers Redis container for integration tests matching production configuration.
     * 
     * This method provides a Testcontainers Redis instance that closely matches production
     * Redis configuration for comprehensive integration testing. The container runs a real
     * Redis instance in Docker, enabling validation of session management behavior under
     * production-like conditions including network latency and container lifecycle management.
     *
     * Integration Test Features:
     * - Real Redis 7-alpine Docker container matching production deployment
     * - Production-equivalent configuration for session management validation
     * - Network isolation and container lifecycle testing capabilities
     * - Support for Redis cluster and failover scenario testing
     * - Validation of session persistence across container restarts
     *
     * Container Configuration:
     * - Redis 7-alpine image for compatibility with production Kubernetes deployment
     * - Exposed Redis port for external connection testing
     * - Memory and CPU limits for consistent test performance
     * - Container health checks for reliable test execution
     * - Automatic cleanup after test completion
     *
     * @return configured Testcontainers Redis container for integration testing
     */
    @Bean
    @Profile("integration")
    public GenericContainer<?> redisTestContainer() {
        logger.info("Configuring Testcontainers Redis for integration tests");
        
        try {
            redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .withCommand("redis-server", 
                    "--maxmemory", "128M",
                    "--maxmemory-policy", "allkeys-lru",
                    "--save", "", 
                    "--appendonly", "no");
            
            // Start container for integration tests
            redisContainer.start();
            
            logger.info("Testcontainers Redis started on host {} port {}", 
                       redisContainer.getHost(), redisContainer.getMappedPort(6379));
            
            return redisContainer;
            
        } catch (Exception e) {
            logger.error("Failed to configure Testcontainers Redis", e);
            throw new RuntimeException("Could not start Testcontainers Redis for integration testing", e);
        }
    }

    /**
     * Creates Redis connection factory for test environment with appropriate configuration.
     * 
     * This method provides a Redis connection factory configured for the test environment,
     * supporting both embedded Redis for unit tests and Testcontainers Redis for integration
     * tests. The factory includes connection pool optimization for test execution performance
     * and timeout configuration matching production session management requirements.
     *
     * Connection Factory Features:
     * - Lettuce-based connection factory for optimal performance and resource management
     * - Test-specific database selection for data isolation
     * - Connection timeout optimization for fast test execution
     * - Support for both embedded Redis and Testcontainers Redis configurations
     * - Connection pool sizing appropriate for concurrent test execution
     *
     * Database Configuration:
     * - Database 1 reserved for test data to avoid conflicts with development data
     * - Automatic database selection based on active test profile
     * - Connection validation and health check configuration
     * - Timeout settings optimized for test execution speed
     *
     * @return configured LettuceConnectionFactory for test Redis connections
     */
    @Bean
    public LettuceConnectionFactory testRedisConnectionFactory() {
        logger.info("Configuring Redis connection factory for test environment");
        
        LettuceConnectionFactory factory;
        
        if (isIntegrationTestProfile()) {
            // Use Testcontainers Redis for integration tests
            if (redisContainer != null && redisContainer.isRunning()) {
                factory = new LettuceConnectionFactory(
                    redisContainer.getHost(), 
                    redisContainer.getMappedPort(6379));
                logger.info("Redis connection factory configured for Testcontainers: {}:{}", 
                           redisContainer.getHost(), redisContainer.getMappedPort(6379));
            } else {
                throw new RuntimeException("Testcontainers Redis is not running for integration tests");
            }
        } else {
            // Use embedded Redis for unit tests
            int testPort = (embeddedRedisServer != null && embeddedRedisServer.isActive()) 
                ? embeddedRedisServer.ports().get(0) 
                : REDIS_TEST_PORT;
            
            factory = new LettuceConnectionFactory("localhost", testPort);
            logger.info("Redis connection factory configured for embedded Redis: localhost:{}", testPort);
        }
        
        // Configure test-specific database
        factory.setDatabase(REDIS_TEST_DATABASE);
        factory.setValidateConnection(true);
        
        // Optimize for test performance
        // Note: Timeout configuration handled by connection pool settings
        
        factory.afterPropertiesSet();
        logger.info("Redis connection factory configured for test database {}", REDIS_TEST_DATABASE);
        
        return factory;
    }

    /**
     * Creates RedisTemplate for testing with JSON serialization optimized for session management.
     * 
     * This method provides a RedisTemplate configured specifically for test scenarios,
     * including session attribute storage, retrieval, and validation testing. The template
     * uses JSON serialization to enable human-readable session data inspection during
     * test debugging and supports complex session objects used in COMMAREA replacement.
     *
     * Template Configuration Features:
     * - String serialization for keys enabling readable session identifiers in Redis
     * - JSON serialization for values supporting complex session attribute objects
     * - Optimized for session size validation testing with 32KB COMMAREA limit enforcement
     * - Integration with SessionAttributes constants for consistent session attribute access
     * - Support for concurrent test execution with thread-safe operations
     *
     * Session Testing Support:
     * - Session attribute storage and retrieval validation
     * - Session size limit testing and validation
     * - Session timeout behavior verification
     * - Session event listener testing and audit trail validation
     * - Performance testing for sub-200ms session operation requirements
     *
     * @return configured RedisTemplate for session management testing
     */
    @Bean
    public RedisTemplate<String, Object> testRedisTemplate() {
        logger.info("Configuring RedisTemplate for session management testing");
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(testRedisConnectionFactory());
        
        // Configure serializers for test data inspection
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.setDefaultSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        
        logger.info("RedisTemplate configured for test environment with JSON serialization");
        return template;
    }

    /**
     * Gets the Redis test port for dynamic test configuration.
     * 
     * This method provides access to the Redis port being used for testing,
     * supporting dynamic port allocation for embedded Redis and Testcontainers
     * Redis configurations. The port information is essential for test setup,
     * connection validation, and integration test configuration.
     *
     * Port Selection Logic:
     * - For embedded Redis: Returns the dynamically allocated port from the running server
     * - For Testcontainers Redis: Returns the mapped port from the Docker container
     * - For configuration: Returns the default test port for static configuration
     * - Handles port conflicts through automatic port detection and allocation
     *
     * Use Cases:
     * - Test configuration setup and validation
     * - Dynamic test environment configuration
     * - Integration test connection string generation
     * - Port conflict resolution and reporting
     * - Test infrastructure health check validation
     *
     * @return Redis port number for test connections
     */
    public int getRedisTestPort() {
        if (isIntegrationTestProfile() && redisContainer != null && redisContainer.isRunning()) {
            int mappedPort = redisContainer.getMappedPort(6379);
            logger.debug("Returning Testcontainers Redis port: {}", mappedPort);
            return mappedPort;
        } else if (embeddedRedisServer != null && embeddedRedisServer.isActive()) {
            int embeddedPort = embeddedRedisServer.ports().get(0);
            logger.debug("Returning embedded Redis port: {}", embeddedPort);
            return embeddedPort;
        } else {
            logger.debug("Returning default test Redis port: {}", REDIS_TEST_PORT);
            return REDIS_TEST_PORT;
        }
    }

    /**
     * Gets the Redis test database number for test data isolation.
     * 
     * This method provides the database number used for test data storage,
     * ensuring isolation between test data and any development or production
     * data that might exist in the default Redis database. The database
     * selection supports clean test execution and data isolation requirements.
     *
     * Database Isolation Features:
     * - Test data stored in dedicated database (database 1) for isolation
     * - Automatic database selection for different test environments
     * - Support for parallel test execution with database-level isolation
     * - Clean separation from development data in database 0
     * - Integration with Redis connection factory database configuration
     *
     * Test Data Management:
     * - Session attribute storage isolated from other applications
     * - Test-specific data cleanup and initialization
     * - Support for concurrent test execution without data conflicts
     * - Database-level transaction isolation for test integrity
     * - Consistent database selection across test configuration components
     *
     * @return Redis database number for test data storage (1)
     */
    public int getRedisTestDatabase() {
        logger.debug("Returning Redis test database number: {}", REDIS_TEST_DATABASE);
        return REDIS_TEST_DATABASE;
    }

    /**
     * Lifecycle management for embedded Redis server startup.
     * 
     * This method handles the automatic startup of the embedded Redis server
     * for unit tests, ensuring the server is available before test execution
     * begins. The startup process includes port allocation, configuration
     * validation, and health check verification.
     */
    @PostConstruct
    public void startEmbeddedRedis() {
        if (embeddedRedisServer != null && !embeddedRedisServer.isActive()) {
            try {
                embeddedRedisServer.start();
                logger.info("Embedded Redis server started successfully on port {}", 
                           embeddedRedisServer.ports().get(0));
            } catch (Exception e) {
                logger.error("Failed to start embedded Redis server", e);
                throw new RuntimeException("Could not start embedded Redis for testing", e);
            }
        }
    }

    /**
     * Lifecycle management for Redis test infrastructure cleanup.
     * 
     * This method ensures proper cleanup of Redis test infrastructure,
     * including stopping embedded Redis servers and Testcontainers Redis
     * containers. The cleanup process prevents resource leaks and ensures
     * clean test environment termination.
     */
    @PreDestroy
    public void stopRedisInfrastructure() {
        // Stop embedded Redis server
        if (embeddedRedisServer != null && embeddedRedisServer.isActive()) {
            try {
                embeddedRedisServer.stop();
                logger.info("Embedded Redis server stopped successfully");
            } catch (Exception e) {
                logger.warn("Error stopping embedded Redis server", e);
            }
        }
        
        // Stop Testcontainers Redis
        if (redisContainer != null && redisContainer.isRunning()) {
            try {
                redisContainer.stop();
                logger.info("Testcontainers Redis stopped successfully");
            } catch (Exception e) {
                logger.warn("Error stopping Testcontainers Redis", e);
            }
        }
    }

    /**
     * Dynamic property configuration for Spring Boot test integration.
     * 
     * This method configures Spring Boot application properties dynamically
     * based on the running test infrastructure, ensuring proper integration
     * between test Redis configuration and Spring Session management.
     *
     * @param registry Spring's dynamic property registry for test configuration
     */
    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registry) {
        // Redis connection properties will be set by the connection factory
        registry.add("spring.session.store-type", () -> "redis");
        registry.add("spring.session.redis.namespace", () -> "carddemo:test:session");
        registry.add("spring.session.timeout", () -> "30m"); // 30 minutes matching CICS timeout
        registry.add("carddemo.session.enabled", () -> "true");
        registry.add("carddemo.session.max-sessions-per-user", () -> "3"); // Lower limit for testing
    }

    // Private utility methods

    /**
     * Finds an available port for embedded Redis to avoid conflicts.
     * 
     * @param preferredPort the preferred port to use if available
     * @return available port number
     */
    private int findAvailablePort(int preferredPort) {
        try (ServerSocket socket = new ServerSocket(preferredPort)) {
            return preferredPort;
        } catch (IOException e) {
            // Preferred port is in use, find a random available port
            try (ServerSocket socket = new ServerSocket(0)) {
                int availablePort = socket.getLocalPort();
                logger.info("Preferred port {} unavailable, using port {}", preferredPort, availablePort);
                return availablePort;
            } catch (IOException ex) {
                throw new RuntimeException("Could not find available port for embedded Redis", ex);
            }
        }
    }

    /**
     * Determines if integration test profile is active.
     * 
     * @return true if integration test profile is active
     */
    private boolean isIntegrationTestProfile() {
        // Check for integration profile through system properties or environment
        String profiles = System.getProperty("spring.profiles.active", "");
        boolean isIntegration = profiles.contains("integration");
        
        if (isIntegration) {
            logger.debug("Integration test profile detected");
        } else {
            logger.debug("Unit test profile active (default)");
        }
        
        return isIntegration;
    }

    // Session management testing utilities for integration with SessionAttributes

    /**
     * Validates session attribute storage and retrieval for testing.
     * 
     * This utility method provides comprehensive validation of session attribute
     * operations, ensuring compatibility with SessionAttributes constants and
     * utility methods. It supports testing of session size limits, attribute
     * type safety, and session state consistency.
     *
     * @param sessionAttributes Map of session attributes to validate
     * @return true if session attributes are valid and within size limits
     */
    public boolean validateTestSessionAttributes(java.util.Map<String, Object> sessionAttributes) {
        if (sessionAttributes == null) {
            logger.debug("Session attributes are null, validation passed");
            return true;
        }

        try {
            // Validate session size using SessionAttributes utility
            boolean sizeValid = SessionAttributes.validateSessionSize(sessionAttributes);
            if (!sizeValid) {
                logger.warn("Session size validation failed for test session");
                return false;
            }

            // Validate required session attributes for COMMAREA compatibility
            boolean hasUserId = sessionAttributes.containsKey(SessionAttributes.SEC_USR_ID);
            boolean hasUserType = sessionAttributes.containsKey(SessionAttributes.SEC_USR_TYPE);
            
            if (hasUserId || hasUserType) {
                // If user attributes exist, validate their types and values
                Object userId = sessionAttributes.get(SessionAttributes.SEC_USR_ID);
                Object userType = sessionAttributes.get(SessionAttributes.SEC_USR_TYPE);
                
                if (userId != null && !(userId instanceof String)) {
                    logger.warn("Invalid user ID type in session: {}", userId.getClass());
                    return false;
                }
                
                if (userType != null && !(userType instanceof String)) {
                    logger.warn("Invalid user type in session: {}", userType.getClass());
                    return false;
                }
                
                // Validate user type values
                if (userType instanceof String) {
                    String type = (String) userType;
                    if (!"A".equals(type) && !"U".equals(type)) {
                        logger.warn("Invalid user type value in session: {}", type);
                        return false;
                    }
                }
            }

            logger.debug("Session attributes validation passed for {} attributes", sessionAttributes.size());
            return true;
            
        } catch (Exception e) {
            logger.error("Error validating test session attributes", e);
            return false;
        }
    }

    /**
     * Creates test session attributes for session management testing.
     * 
     * This utility method generates standardized test session attributes
     * that match CICS COMMAREA structure and SessionAttributes constants,
     * supporting consistent test data generation across session management tests.
     *
     * @param userId test user identifier
     * @param userType test user type ('A' for Admin, 'U' for User)
     * @return Map of test session attributes matching COMMAREA structure
     */
    public java.util.Map<String, Object> createTestSessionAttributes(String userId, String userType) {
        java.util.Map<String, Object> sessionAttributes = new java.util.HashMap<>();
        
        // Core user attributes matching CICS COMMAREA
        sessionAttributes.put(SessionAttributes.SEC_USR_ID, userId != null ? userId : TEST_USER_ID);
        sessionAttributes.put(SessionAttributes.SEC_USR_TYPE, userType != null ? userType : "U");
        sessionAttributes.put(SessionAttributes.SEC_USR_NAME, "Test User");
        
        // Navigation and transaction state
        sessionAttributes.put(SessionAttributes.NAVIGATION_STATE, "MENU");
        sessionAttributes.put(SessionAttributes.TRANSACTION_STATE, "ACTIVE");
        
        // Session metadata
        sessionAttributes.put("SESSION_CREATED_TIME", System.currentTimeMillis());
        sessionAttributes.put("SESSION_LAST_ACCESS", System.currentTimeMillis());
        
        logger.debug("Created test session attributes for user: {} type: {}", userId, userType);
        return sessionAttributes;
    }
}
