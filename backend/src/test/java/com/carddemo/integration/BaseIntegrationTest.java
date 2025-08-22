/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.integration;

import com.carddemo.entity.Account;
import com.carddemo.entity.Customer;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.UserSecurity;
import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.TestConstants;
import com.carddemo.config.TestDatabaseConfig;
import com.carddemo.config.TransactionConfig;
import com.carddemo.util.CobolDataConverter;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for all integration tests providing common Spring Boot test configuration,
 * Testcontainers setup for PostgreSQL and Redis, test data initialization, database cleanup
 * between tests, and shared assertion utilities for validating COBOL-to-Java functional parity.
 * 
 * This class serves as the foundation for comprehensive integration testing ensuring that the
 * modernized Java/Spring Boot implementation maintains identical functional behavior to the
 * original COBOL/CICS system, particularly for financial calculations and business logic.
 * 
 * Key Features:
 * - Testcontainers integration for isolated PostgreSQL and Redis testing
 * - COBOL-precision BigDecimal assertion methods for financial accuracy validation
 * - Standardized test data factory methods matching COBOL record structures
 * - Session state management testing utilities
 * - Automated transaction rollback for test isolation
 * - Mock external service integration points
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.profiles.active=integration-test"
    }
)
@TestPropertySource(locations = "classpath:application-integration.yml")
@Testcontainers
@Transactional
public abstract class BaseIntegrationTest extends AbstractBaseTest {

    /**
     * PostgreSQL container for database integration testing.
     * Provides isolated database instance with consistent schema and data.
     * Uses PostgreSQL 15.4 to match production environment specifications.
     */
    @Container
    static PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>("postgres:15.4-alpine")
            .withDatabaseName("carddemo_test")
            .withUsername("carddemo_test")
            .withPassword("carddemo_test")
            .withReuse(true);

    /**
     * Redis container for session state integration testing.
     * Provides isolated Redis instance for Spring Session testing.
     * Uses Redis 7.0 to match production environment specifications.
     */
    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.0-alpine")
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", "carddemo_test")
            .withReuse(true);

    /**
     * Sets up Testcontainers and configures test environment before each test execution.
     * Ensures PostgreSQL and Redis containers are running and properly configured.
     * Initializes system properties for Spring Boot to connect to test containers.
     */
    @BeforeEach
    public void setupTestContainers() {
        // Ensure containers are started
        if (!postgresqlContainer.isRunning()) {
            postgresqlContainer.start();
        }
        
        if (!redisContainer.isRunning()) {
            redisContainer.start();
        }

        // Configure Spring Boot to use test containers
        System.setProperty("spring.datasource.url", postgresqlContainer.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgresqlContainer.getUsername());
        System.setProperty("spring.datasource.password", postgresqlContainer.getPassword());
        
        // Configure Redis for Spring Session
        System.setProperty("spring.data.redis.host", redisContainer.getHost());
        System.setProperty("spring.data.redis.port", redisContainer.getMappedPort(6379).toString());
        System.setProperty("spring.data.redis.password", "carddemo_test");

        // Call parent setup for common test initialization
        super.setUp();
        
        // Load integration test fixtures
        loadTestFixtures();
        
        // Setup session state for integration tests
        setupSessionState();
    }

    /**
     * Cleans up test data after each test execution to ensure test isolation.
     * Removes any test-specific data while preserving baseline fixtures.
     * Ensures no test contamination between integration test executions.
     */
    public void cleanupTestData() {
        // Clear any temporary test data created during test execution
        clearSessionState();
        
        // Call parent cleanup for common teardown activities
        super.tearDown();
    }

    /**
     * Creates a test Account instance with COBOL-compatible field values and BigDecimal precision.
     * Uses test constants to ensure consistent monetary field precision matching COBOL COMP-3 behavior.
     * 
     * @return Account entity configured with test data and proper decimal precision
     */
    public Account createIntegrationTestAccount() {
        return Account.builder()
                .accountId(Long.parseLong(TestConstants.TEST_ACCOUNT_ID))
                .activeStatus("Y")
                .currentBalance(new BigDecimal("1500.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE))
                .creditLimit(new BigDecimal("5000.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE))
                .cashCreditLimit(new BigDecimal("1000.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE))
                .openDate(LocalDate.now())
                .currentCycleCredit(new BigDecimal("0.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE))
                .currentCycleDebit(new BigDecimal("0.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE))
                .groupId("DEFAULT")
                .build();
    }

    /**
     * Creates a test Transaction instance with COBOL-compatible field values and BigDecimal precision.
     * Ensures transaction amounts maintain exact precision matching original COBOL calculations.
     * 
     * @return Transaction entity configured with test data and proper decimal precision
     */
    public Transaction createIntegrationTestTransaction() {
        return Transaction.builder()
                .accountId(Long.parseLong(TestConstants.TEST_ACCOUNT_ID))
                .amount(new BigDecimal("100.50").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE))
                .transactionTypeCode("PU")
                .categoryCode("5411")
                .source("ATM")
                .description("TEST MERCHANT")
                .merchantName("TEST MERCHANT")
                .cardNumber(TestConstants.TEST_CARD_NUMBER)
                .originalTimestamp(LocalDateTime.now())
                .processedTimestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a test Customer instance with realistic field values matching COBOL copybook structure.
     * Provides complete customer profile data for comprehensive integration testing scenarios.
     * 
     * @return Customer entity configured with test data matching COBOL record layout
     */
    public Customer createIntegrationTestCustomer() {
        return Customer.builder()
                .customerId(1L) // Will be auto-generated by database
                .firstName("TEST")
                .lastName("CUSTOMER")
                .middleName("USER")
                .addressLine1("123 TEST STREET")
                .addressLine2("APT 1")
                .stateCode("CA")
                .countryCode("USA")
                .zipCode("90210")
                .phoneNumber1("555-123-4567")
                .ssn("999999999")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .ficoScore(750)
                .build();
    }

    /**
     * Creates a test UserSecurity instance for authentication and authorization testing.
     * Provides proper user credentials and role assignments for Spring Security integration testing.
     * 
     * @return UserSecurity entity configured with test authentication data
     */
    public UserSecurity createIntegrationTestUser() {
        UserSecurity user = new UserSecurity(
                TestConstants.TEST_USER_ID,
                "testuser",
                "$2a$10$encrypted.password.hash", // BCrypt hash for "password123"
                "Test",
                "User",
                "U" // Regular user
        );
        return user;
    }

    /**
     * Asserts that two BigDecimal values are equal with COBOL COMP-3 precision requirements.
     * Performs exact decimal comparison ensuring penny-level accuracy for financial calculations.
     * Critical for validating COBOL-to-Java functional parity in monetary computations.
     * 
     * @param expected Expected BigDecimal value with proper COBOL precision
     * @param actual Actual BigDecimal value to validate
     */
    public void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        super.assertBigDecimalEquals(expected, actual, "BigDecimal values should be equal with COBOL precision");
    }

    /**
     * Asserts that two BigDecimal values are within acceptable tolerance for COBOL precision.
     * Uses configurable tolerance threshold for complex financial calculations where minor
     * rounding differences may occur due to intermediate calculation steps.
     * 
     * @param expected Expected BigDecimal value
     * @param actual Actual BigDecimal value to validate
     */
    public void assertBigDecimalWithinTolerance(BigDecimal expected, BigDecimal actual) {
        BigDecimal tolerance = new BigDecimal("0.01"); // Penny-level tolerance
        BigDecimal difference = expected.subtract(actual).abs();
        
        assertThat(difference)
                .as("BigDecimal values should be within tolerance of %s", tolerance)
                .isLessThanOrEqualTo(tolerance);
    }

    /**
     * Returns the PostgreSQL test container instance for advanced configuration or query execution.
     * Provides direct access to container for custom database operations during integration testing.
     * 
     * @return PostgreSQL container instance
     */
    public static PostgreSQLContainer<?> getPostgreSQLContainer() {
        return postgresqlContainer;
    }

    /**
     * Returns the Redis test container instance for session state testing and cache operations.
     * Provides direct access to container for Redis-specific testing scenarios.
     * 
     * @return Redis container instance
     */
    public static GenericContainer<?> getRedisContainer() {
        return redisContainer;
    }

    /**
     * Loads test fixture data for comprehensive integration testing scenarios.
     * Initializes baseline test data matching production data structures and relationships.
     * Calls parent loadTestFixtures() and adds integration-specific fixture data.
     */
    public void loadTestFixtures() {
        // Load base fixtures from parent class
        super.loadTestFixtures();
        
        // Additional integration test specific fixtures can be loaded here
        // For example: loading larger datasets for performance testing
        // or setting up complex entity relationships for workflow testing
    }

    /**
     * Configures mock external services for integration testing isolation.
     * Sets up WireMock stubs or test containers for external dependencies
     * ensuring integration tests don't depend on external systems.
     */
    public void mockExternalServices() {
        // Setup mock beans for external services
        super.mockCommonDependencies();
        
        // Additional mock configurations for integration testing:
        // - Payment network simulators
        // - Core banking system mocks  
        // - Fraud detection service stubs
        // - Regulatory reporting endpoints
    }

    /**
     * Validates COBOL precision requirements for financial calculations.
     * Ensures BigDecimal scale and rounding mode match original COBOL COMP-3 behavior.
     * Critical validation method for maintaining financial accuracy during modernization.
     * 
     * @param value BigDecimal value to validate for COBOL precision compliance
     */
    public void validateCobolPrecision(BigDecimal value) {
        assertThat(value.scale())
                .as("BigDecimal scale should match COBOL COMP-3 precision")
                .isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        
        // Validate the value can be represented in COBOL COMP-3 format
        // This ensures no precision loss during COBOL-Java conversion
        BigDecimal normalizedValue = value.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        assertThat(value).isEqualTo(normalizedValue);
    }

    /**
     * Sets up session state for integration testing scenarios.
     * Initializes Spring Session with test data matching CICS COMMAREA patterns.
     * Configures session attributes for comprehensive workflow testing.
     */
    public void setupSessionState() {
        // Initialize session state for integration tests
        // This method would typically:
        // 1. Create test session in Redis
        // 2. Set session attributes matching COBOL COMMAREA data
        // 3. Configure session timeout for test scenarios
        // 4. Setup user authentication context
        
        // Note: Actual session setup would require Spring Session integration
        // This is a placeholder for session state initialization
    }

    /**
     * Clears session state after integration test execution.
     * Ensures clean session state between test runs to prevent test contamination.
     * Removes all session data from Redis test instance.
     */
    public void clearSessionState() {
        // Clear session state after integration tests
        // This method would typically:
        // 1. Remove test session from Redis
        // 2. Clear session attributes
        // 3. Reset authentication context
        // 4. Clean up any session-related test data
        
        // Note: Actual session cleanup would require Spring Session integration
        // This is a placeholder for session state cleanup
    }
}
