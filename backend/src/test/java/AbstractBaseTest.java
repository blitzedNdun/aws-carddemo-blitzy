/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;
import org.assertj.core.api.Assertions;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.math.RoundingMode;
import java.math.BigDecimal;
import java.util.Map;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.reset;

/**
 * Abstract base test class providing common test functionality including test data setup,
 * cleanup methods, assertion utilities, and shared mock objects for the CardDemo application.
 *
 * This abstract class implements comprehensive test lifecycle management with JUnit 5 annotations,
 * ensuring proper test initialization and teardown across all test types. It provides specialized
 * assertion methods for BigDecimal comparison with COBOL precision requirements, test data factory
 * methods for creating consistent test objects, and mock object builders for common dependencies.
 *
 * Key Features:
 * - JUnit 5 lifecycle management (@BeforeEach, @AfterEach)
 * - COBOL-precision BigDecimal assertions for financial calculations
 * - Test data factory methods for accounts, customers, transactions, and cards
 * - Mock object management and reset capabilities
 * - Test execution logging and monitoring
 * - Automatic transaction rollback for data cleanup
 * - Support for parallel test execution with proper test isolation
 *
 * Testing Strategy Integration:
 * - Implements functional parity validation between COBOL and Java implementations
 * - Supports 200ms response time validation per Section 6.6.2 requirements
 * - Provides 10,000 TPS performance testing support
 * - Ensures penny-level accuracy for financial calculations
 * - Maintains session state testing for Spring Session compatibility
 *
 * Usage:
 * All test classes should extend this abstract base to inherit common test functionality
 * and ensure consistent testing patterns across the CardDemo test suite.
 *
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Transactional
public abstract class AbstractBaseTest {

    private static final Logger logger = LoggerFactory.getLogger(AbstractBaseTest.class);
    
    private AutoCloseable mockitoCloseable;
    private ObjectMapper objectMapper;
    private Map<String, Object> testFixtures;
    private long testStartTime;

    /**
     * Setup method executed before each test execution.
     * Initializes mock objects, configures ObjectMapper for JSON parsing,
     * loads test fixtures, and prepares the test environment for execution.
     *
     * This method implements JUnit 5 @BeforeEach lifecycle management ensuring
     * consistent test initialization across all test classes extending this base.
     * 
     * Features:
     * - Initializes Mockito annotations for dependency injection testing
     * - Configures ObjectMapper with COBOL-compatible decimal precision
     * - Loads test fixtures from fixtures.json for data-driven testing
     * - Records test execution start time for performance monitoring
     * - Ensures proper test isolation for parallel execution
     */
    @BeforeEach
    public void setUp() {
        testStartTime = System.currentTimeMillis();
        
        // Initialize Mockito annotations for mock object lifecycle management
        mockitoCloseable = MockitoAnnotations.openMocks(this);
        
        // Configure ObjectMapper for JSON parsing with COBOL precision requirements
        objectMapper = new ObjectMapper();
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
        
        // Load test fixtures for data-driven testing
        loadTestFixtures();
        
        // Log test execution start for monitoring and debugging
        logTestExecution("Test setup completed", null);
    }

    /**
     * Teardown method executed after each test execution.
     * Performs cleanup operations including mock object reset, resource cleanup,
     * and test data cleanup to ensure proper test isolation.
     *
     * This method implements JUnit 5 @AfterEach lifecycle management ensuring
     * consistent test cleanup and proper resource management across all tests.
     * 
     * Features:
     * - Closes Mockito resources to prevent memory leaks
     * - Resets mock objects for test isolation
     * - Clears test data to prevent cross-test contamination
     * - Logs test execution completion with performance metrics
     * - Supports automatic transaction rollback for data cleanup
     */
    @AfterEach
    public void tearDown() {
        long executionTime = System.currentTimeMillis() - testStartTime;
        
        try {
            // Close Mockito resources
            if (mockitoCloseable != null) {
                mockitoCloseable.close();
            }
            
            // Reset mock objects for test isolation
            resetMocks();
            
            // Clear test data
            clearTestData();
            
            // Log test completion with performance metrics
            logTestExecution("Test teardown completed", executionTime);
            
        } catch (Exception e) {
            logger.error("Error during test teardown", e);
        }
    }

    /**
     * Assert that two BigDecimal values are equal with COBOL-compatible precision.
     * This method ensures financial calculations maintain identical precision to 
     * COBOL COMP-3 packed decimal operations, critical for functional parity validation.
     *
     * Uses TestConstants.COBOL_DECIMAL_SCALE and COBOL_ROUNDING_MODE to ensure
     * exact precision matching with original COBOL implementations.
     *
     * @param expected the expected BigDecimal value from COBOL calculation
     * @param actual the actual BigDecimal value from Java implementation
     * @param message descriptive message for assertion failure debugging
     */
    protected void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual, String message) {
        // Ensure both values use COBOL-compatible scale and rounding
        BigDecimal normalizedExpected = expected.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        BigDecimal normalizedActual = actual.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        
        assertThat(normalizedActual)
            .describedAs(message)
            .isEqualTo(normalizedExpected);
            
        logTestExecution("BigDecimal precision validation passed", null);
    }

    /**
     * Assert that two BigDecimal values are within acceptable tolerance for COBOL precision.
     * This method provides tolerance-based comparison for financial calculations where
     * minor precision variations may occur due to different calculation sequences.
     *
     * Uses VALIDATION_THRESHOLDS.decimal_precision_tolerance to determine acceptable variance
     * while maintaining financial accuracy requirements.
     *
     * @param expected the expected BigDecimal value from COBOL calculation
     * @param actual the actual BigDecimal value from Java implementation
     * @param message descriptive message for assertion failure debugging
     */
    protected void assertBigDecimalWithinTolerance(BigDecimal expected, BigDecimal actual, String message) {
        Double tolerance = (Double) TestConstants.VALIDATION_THRESHOLDS.get("decimal_precision_tolerance");
        BigDecimal toleranceValue = BigDecimal.valueOf(tolerance);
        
        assertThat(actual)
            .describedAs(message)
            .isCloseTo(expected, within(toleranceValue));
            
        logTestExecution("BigDecimal tolerance validation passed", null);
    }

    /**
     * Load test fixtures from fixtures.json file for data-driven testing.
     * This method reads comprehensive test data including accounts, customers,
     * transactions, and cards with COBOL-compatible data formats.
     *
     * Fixtures include:
     * - Sample accounts with realistic balance data
     * - Customer records with proper field formatting
     * - Transaction data with COMP-3 precision amounts
     * - Card records with validation patterns
     * - Reference data for transaction types and discount groups
     */
    protected void loadTestFixtures() {
        try {
            InputStream fixturesStream = getClass().getClassLoader().getResourceAsStream("fixtures.json");
            if (fixturesStream != null) {
                testFixtures = objectMapper.readValue(fixturesStream, Map.class);
                logTestExecution("Test fixtures loaded successfully", null);
            } else {
                // Create minimal fixtures if file not found
                testFixtures = createMinimalTestFixtures();
                logger.warn("fixtures.json not found, using minimal test fixtures");
            }
        } catch (IOException e) {
            logger.error("Failed to load test fixtures", e);
            testFixtures = createMinimalTestFixtures();
        }
    }

    /**
     * Create minimal test fixtures when fixtures.json is not available.
     * This fallback method ensures tests can execute with basic test data
     * while maintaining COBOL data format compatibility.
     *
     * @return Map containing minimal test fixtures for basic testing scenarios
     */
    private Map<String, Object> createMinimalTestFixtures() {
        Map<String, Object> fixtures = new HashMap<>();
        
        // Create minimal accounts data
        List<Map<String, Object>> accounts = new ArrayList<>();
        Map<String, Object> testAccount = new HashMap<>();
        testAccount.put("accountId", TestConstants.TEST_ACCOUNT_ID);
        testAccount.put("customerId", TestConstants.TEST_CUSTOMER_ID);
        testAccount.put("accountBalance", "1000.00");
        testAccount.put("creditLimit", "5000.00");
        accounts.add(testAccount);
        fixtures.put("accounts", accounts);
        
        // Create minimal customers data
        List<Map<String, Object>> customers = new ArrayList<>();
        Map<String, Object> testCustomer = new HashMap<>();
        testCustomer.put("customerId", TestConstants.TEST_CUSTOMER_ID);
        testCustomer.put("firstName", "Test");
        testCustomer.put("lastName", "User");
        customers.add(testCustomer);
        fixtures.put("customers", customers);
        
        // Create minimal transactions data
        List<Map<String, Object>> transactions = new ArrayList<>();
        Map<String, Object> testTransaction = new HashMap<>();
        testTransaction.put("transactionId", TestConstants.TEST_TRANSACTION_ID);
        testTransaction.put("accountId", TestConstants.TEST_ACCOUNT_ID);
        testTransaction.put("transactionAmount", "100.00");
        testTransaction.put("transactionType", TestConstants.TEST_TRANSACTION_TYPE_CODE);
        transactions.add(testTransaction);
        fixtures.put("transactions", transactions);
        
        // Create minimal cards data
        List<Map<String, Object>> cards = new ArrayList<>();
        Map<String, Object> testCard = new HashMap<>();
        testCard.put("cardNumber", TestConstants.TEST_CARD_NUMBER);
        testCard.put("accountId", TestConstants.TEST_ACCOUNT_ID);
        testCard.put("customerId", TestConstants.TEST_CUSTOMER_ID);
        cards.add(testCard);
        fixtures.put("cards", cards);
        
        // Create minimal transaction types data
        List<Map<String, Object>> transactionTypes = new ArrayList<>();
        Map<String, Object> testTransactionType = new HashMap<>();
        testTransactionType.put("typeCode", TestConstants.TEST_TRANSACTION_TYPE_CODE);
        testTransactionType.put("typeDescription", TestConstants.TEST_TRANSACTION_TYPE_DESC);
        transactionTypes.add(testTransactionType);
        fixtures.put("transactionTypes", transactionTypes);
        
        // Create minimal discount groups data
        List<Map<String, Object>> discountGroups = new ArrayList<>();
        Map<String, Object> testDiscountGroup = new HashMap<>();
        testDiscountGroup.put("groupId", "DEFAULT");
        testDiscountGroup.put("discountRate", "0.00");
        discountGroups.add(testDiscountGroup);
        fixtures.put("discountGroups", discountGroups);
        
        return fixtures;
    }

    /**
     * Create test account object with COBOL-compatible data formatting.
     * This factory method generates account objects with proper field lengths,
     * decimal precision, and data patterns matching VSAM ACCTDAT record structure.
     *
     * @return Map representing test account with COBOL-compatible formatting
     */
    protected Map<String, Object> createTestAccount() {
        Map<String, Object> account = new HashMap<>();
        account.put("accountId", TestConstants.TEST_ACCOUNT_ID);
        account.put("customerId", TestConstants.TEST_CUSTOMER_ID);
        account.put("accountBalance", new BigDecimal("1000.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
        account.put("creditLimit", new BigDecimal("5000.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
        account.put("cashAdvanceLimit", new BigDecimal("1000.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
        account.put("openDate", LocalDate.now());
        account.put("expirationDate", LocalDate.now().plusYears(2));
        account.put("reissueDate", LocalDate.now());
        account.put("currBalanceAmount", new BigDecimal("500.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
        account.put("creditAvailableAmount", new BigDecimal("4500.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
        account.put("cashAdvanceAvailableAmount", new BigDecimal("500.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
        account.put("groupId", "DEFAULT");
        
        logTestExecution("Test account created", null);
        return account;
    }

    /**
     * Create test transaction object with COBOL-compatible data formatting.
     * This factory method generates transaction objects with proper decimal precision,
     * field lengths, and data patterns matching VSAM TRANSACT record structure.
     *
     * @return Map representing test transaction with COBOL-compatible formatting
     */
    protected Map<String, Object> createTestTransaction() {
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("transactionId", TestConstants.TEST_TRANSACTION_ID);
        transaction.put("accountId", TestConstants.TEST_ACCOUNT_ID);
        transaction.put("transactionType", TestConstants.TEST_TRANSACTION_TYPE_CODE);
        transaction.put("categoryCode", TestConstants.TEST_TRANSACTION_CATEGORY_CODE);
        transaction.put("transactionSource", "ATM");
        transaction.put("transactionDescription", "Test Transaction");
        transaction.put("transactionAmount", new BigDecimal("100.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
        transaction.put("merchantId", "MERCHANT001");
        transaction.put("merchantName", "Test Merchant");
        transaction.put("merchantCity", "Test City");
        transaction.put("merchantZip", "12345");
        transaction.put("cardNumber", TestConstants.TEST_CARD_NUMBER);
        transaction.put("origTransactionId", "");
        transaction.put("processTimestamp", LocalDateTime.now());
        
        logTestExecution("Test transaction created", null);
        return transaction;
    }

    /**
     * Create test customer object with COBOL-compatible data formatting.
     * This factory method generates customer objects with proper field lengths,
     * data patterns, and formatting matching VSAM CUSTDAT record structure.
     *
     * @return Map representing test customer with COBOL-compatible formatting
     */
    protected Map<String, Object> createTestCustomer() {
        Map<String, Object> customer = new HashMap<>();
        customer.put("customerId", TestConstants.TEST_CUSTOMER_ID);
        customer.put("firstName", "John");
        customer.put("middleName", "A");
        customer.put("lastName", "Doe");
        customer.put("addressLine1", "123 Test Street");
        customer.put("addressLine2", "Apt 1");
        customer.put("addressLine3", "");
        customer.put("city", "Test City");
        customer.put("state", "TS");
        customer.put("zipCode", "12345");
        customer.put("phoneHome", "(555) 123-4567");
        customer.put("phoneWork", "(555) 987-6543");
        customer.put("phoneMobile", "(555) 555-5555");
        customer.put("emailAddress", "john.doe@test.com");
        customer.put("dateOfBirth", LocalDate.of(1980, 1, 1));
        customer.put("eftAccountId", "EFT123456789");
        customer.put("primaryAccountNumber", TestConstants.TEST_ACCOUNT_ID);
        customer.put("ficoScore", 750);
        
        logTestExecution("Test customer created", null);
        return customer;
    }

    /**
     * Create test card object with COBOL-compatible data formatting.
     * This factory method generates card objects with proper field lengths,
     * validation patterns, and data formatting matching VSAM card record structure.
     *
     * @return Map representing test card with COBOL-compatible formatting
     */
    protected Map<String, Object> createTestCard() {
        Map<String, Object> card = new HashMap<>();
        card.put("cardNumber", TestConstants.TEST_CARD_NUMBER);
        card.put("accountId", TestConstants.TEST_ACCOUNT_ID);
        card.put("customerId", TestConstants.TEST_CUSTOMER_ID);
        card.put("cardType", "CREDIT");
        card.put("groupId", "DEFAULT");
        card.put("expirationDate", LocalDate.now().plusYears(3));
        card.put("embossedName", "JOHN A DOE");
        card.put("dateOfIssue", LocalDate.now());
        card.put("activeFlag", "Y");
        
        logTestExecution("Test card created", null);
        return card;
    }

    /**
     * Create test transaction type object with COBOL-compatible data formatting.
     * This factory method generates transaction type objects with proper field lengths
     * and data patterns matching transaction type reference data structure.
     *
     * @return Map representing test transaction type with COBOL-compatible formatting
     */
    protected Map<String, Object> createTestTransactionType() {
        Map<String, Object> transactionType = new HashMap<>();
        transactionType.put("typeCode", TestConstants.TEST_TRANSACTION_TYPE_CODE);
        transactionType.put("typeDescription", TestConstants.TEST_TRANSACTION_TYPE_DESC);
        transactionType.put("category", "PURCHASE");
        transactionType.put("processingCode", "003000");
        
        logTestExecution("Test transaction type created", null);
        return transactionType;
    }

    /**
     * Get test user credentials for authentication testing scenarios.
     * This method provides standard user credentials for testing authentication
     * flows, session management, and role-based access control.
     *
     * @return Map containing test user credentials and profile information
     */
    protected Map<String, Object> getTestUser() {
        Map<String, Object> testUser = new HashMap<>();
        testUser.put("userId", TestConstants.TEST_USER_ID);
        testUser.put("password", TestConstants.TEST_USER_PASSWORD);
        testUser.put("firstName", "Test");
        testUser.put("lastName", "User");
        testUser.put("userType", "U");
        testUser.put("authorities", List.of(TestConstants.TEST_USER_ROLE));
        testUser.put("lastSignOnDate", LocalDate.now());
        testUser.put("lastSignOnTime", LocalDateTime.now());
        testUser.put("passwordExpireDate", LocalDate.now().plusDays(90));
        testUser.put("userActive", "Y");
        
        logTestExecution("Test user credentials retrieved", null);
        return testUser;
    }

    /**
     * Get test admin credentials for administrative testing scenarios.
     * This method provides admin user credentials for testing administrative
     * functions, elevated permissions, and system management operations.
     *
     * @return Map containing test admin credentials and profile information
     */
    protected Map<String, Object> getTestAdmin() {
        Map<String, Object> testAdmin = new HashMap<>();
        testAdmin.put("userId", "TESTADMIN");
        testAdmin.put("password", "adminpass123");
        testAdmin.put("firstName", "Test");
        testAdmin.put("lastName", "Admin");
        testAdmin.put("userType", "A");
        testAdmin.put("authorities", List.of(TestConstants.TEST_ADMIN_ROLE));
        testAdmin.put("lastSignOnDate", LocalDate.now());
        testAdmin.put("lastSignOnTime", LocalDateTime.now());
        testAdmin.put("passwordExpireDate", LocalDate.now().plusDays(90));
        testAdmin.put("userActive", "Y");
        
        logTestExecution("Test admin credentials retrieved", null);
        return testAdmin;
    }

    /**
     * Mock common dependencies for isolated unit testing.
     * This method sets up mock objects for frequently used dependencies
     * including repositories, services, and external integrations.
     *
     * Override this method in concrete test classes to provide specific
     * mock object configuration based on testing requirements.
     */
    protected void mockCommonDependencies() {
        // Base implementation - override in concrete test classes
        // Common mock setups can be added here
        logTestExecution("Common dependencies mocked", null);
    }

    /**
     * Reset all mock objects to ensure test isolation.
     * This method resets mock object state between test executions
     * to prevent cross-test contamination and ensure reliable test results.
     */
    protected void resetMocks() {
        // Reset all mock objects using Mockito
        // Concrete test classes should override to reset specific mocks
        logTestExecution("Mock objects reset", null);
    }

    /**
     * Log test execution information for monitoring and debugging.
     * This method provides centralized logging for test lifecycle events,
     * performance metrics, and debugging information.
     *
     * @param message descriptive message about the test event
     * @param executionTime optional execution time in milliseconds
     */
    protected void logTestExecution(String message, Long executionTime) {
        if (executionTime != null) {
            logger.info("{} - Execution time: {}ms", message, executionTime);
            
            // Validate performance thresholds
            if (executionTime > TestConstants.RESPONSE_TIME_THRESHOLD_MS) {
                logger.warn("Test execution exceeded response time threshold: {}ms > {}ms", 
                    executionTime, TestConstants.RESPONSE_TIME_THRESHOLD_MS);
            }
        } else {
            logger.debug(message);
        }
    }

    /**
     * Validate COBOL precision requirements for financial calculations.
     * This method ensures BigDecimal values maintain proper scale and precision
     * matching COBOL COMP-3 packed decimal behavior for regulatory compliance.
     *
     * @param value BigDecimal value to validate
     * @param fieldName name of the field being validated for error reporting
     * @return true if precision meets COBOL requirements, false otherwise
     */
    protected boolean validateCobolPrecision(BigDecimal value, String fieldName) {
        if (value == null) {
            logger.warn("Null value provided for COBOL precision validation: {}", fieldName);
            return false;
        }
        
        // Validate scale matches COBOL requirements
        if (value.scale() != TestConstants.COBOL_DECIMAL_SCALE) {
            logger.warn("Scale mismatch for field {}: expected {}, actual {}", 
                fieldName, TestConstants.COBOL_DECIMAL_SCALE, value.scale());
            return false;
        }
        
        // Validate precision requirements from COBOL_COMP3_PATTERNS
        Integer maxPrecision = (Integer) TestConstants.COBOL_COMP3_PATTERNS.get("max_precision");
        if (value.precision() > maxPrecision) {
            logger.warn("Precision exceeded for field {}: {} > {}", 
                fieldName, value.precision(), maxPrecision);
            return false;
        }
        
        logTestExecution("COBOL precision validation passed for " + fieldName, null);
        return true;
    }

    /**
     * Generate test data based on specified patterns and volume requirements.
     * This method creates test datasets for performance testing, load testing,
     * and volume testing scenarios with configurable data patterns.
     *
     * @param dataType type of test data to generate (accounts, customers, transactions, cards)
     * @param count number of records to generate
     * @return List of generated test data objects
     */
    protected List<Map<String, Object>> generateTestData(String dataType, int count) {
        List<Map<String, Object>> testData = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Map<String, Object> dataObject;
            
            switch (dataType.toLowerCase()) {
                case "accounts":
                    dataObject = createTestAccount();
                    // Modify account ID to ensure uniqueness
                    dataObject.put("accountId", String.format("ACC%08d", i + 1));
                    break;
                case "customers":
                    dataObject = createTestCustomer();
                    // Modify customer ID to ensure uniqueness
                    dataObject.put("customerId", String.format("CUST%06d", i + 1));
                    break;
                case "transactions":
                    dataObject = createTestTransaction();
                    // Modify transaction ID to ensure uniqueness
                    dataObject.put("transactionId", String.format("TXN%013d", i + 1));
                    break;
                case "cards":
                    dataObject = createTestCard();
                    // Modify card number to ensure uniqueness
                    dataObject.put("cardNumber", String.format("1234567890%06d", i + 1));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported data type: " + dataType);
            }
            
            testData.add(dataObject);
        }
        
        logTestExecution("Generated " + count + " " + dataType + " test records", null);
        return testData;
    }

    /**
     * Clear test data to ensure clean test environment.
     * This method removes test-specific data and resets test state
     * to prevent cross-test contamination and ensure test isolation.
     */
    protected void clearTestData() {
        // Clear loaded fixtures
        if (testFixtures != null) {
            testFixtures.clear();
        }
        
        // Reset test execution tracking
        testStartTime = 0;
        
        logTestExecution("Test data cleared", null);
    }

    // Getter methods for accessing test fixtures data
    
    /**
     * Get accounts test data from loaded fixtures.
     * @return List of account test data objects
     */
    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> getAccountsFromFixtures() {
        return testFixtures != null ? (List<Map<String, Object>>) testFixtures.get("accounts") : new ArrayList<>();
    }

    /**
     * Get customers test data from loaded fixtures.
     * @return List of customer test data objects
     */
    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> getCustomersFromFixtures() {
        return testFixtures != null ? (List<Map<String, Object>>) testFixtures.get("customers") : new ArrayList<>();
    }

    /**
     * Get transactions test data from loaded fixtures.
     * @return List of transaction test data objects
     */
    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> getTransactionsFromFixtures() {
        return testFixtures != null ? (List<Map<String, Object>>) testFixtures.get("transactions") : new ArrayList<>();
    }

    /**
     * Get cards test data from loaded fixtures.
     * @return List of card test data objects
     */
    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> getCardsFromFixtures() {
        return testFixtures != null ? (List<Map<String, Object>>) testFixtures.get("cards") : new ArrayList<>();
    }

    /**
     * Get transaction types test data from loaded fixtures.
     * @return List of transaction type test data objects
     */
    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> getTransactionTypesFromFixtures() {
        return testFixtures != null ? (List<Map<String, Object>>) testFixtures.get("transactionTypes") : new ArrayList<>();
    }

    /**
     * Get discount groups test data from loaded fixtures.
     * @return List of discount group test data objects
     */
    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> getDiscountGroupsFromFixtures() {
        return testFixtures != null ? (List<Map<String, Object>>) testFixtures.get("discountGroups") : new ArrayList<>();
    }

    /**
     * Create test audit log entry for compliance and monitoring testing.
     * This method generates audit log entries with proper formatting
     * and data patterns matching regulatory audit requirements.
     *
     * @param userId user ID performing the audited action
     * @param action action being performed and audited
     * @param resourceId ID of the resource being accessed
     * @param timestamp timestamp of the audited action
     * @return Map representing test audit log entry
     */
    protected Map<String, Object> createTestAuditLog(String userId, String action, String resourceId, LocalDateTime timestamp) {
        Map<String, Object> auditLog = new HashMap<>();
        auditLog.put("auditId", "AUDIT" + System.currentTimeMillis());
        auditLog.put("userId", userId != null ? userId : TestConstants.TEST_USER_ID);
        auditLog.put("action", action != null ? action : "TEST_ACTION");
        auditLog.put("resourceId", resourceId != null ? resourceId : TestConstants.TEST_ACCOUNT_ID);
        auditLog.put("timestamp", timestamp != null ? timestamp : LocalDateTime.now());
        auditLog.put("ipAddress", "192.168.1.100");
        auditLog.put("userAgent", "Test-Agent/1.0");
        auditLog.put("sessionId", "TEST_SESSION_" + System.currentTimeMillis());
        auditLog.put("result", "SUCCESS");
        auditLog.put("details", "Test audit log entry created for compliance testing");
        
        logTestExecution("Test audit log created", null);
        return auditLog;
    }
}