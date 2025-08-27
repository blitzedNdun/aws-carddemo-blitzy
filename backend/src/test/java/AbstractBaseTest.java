package com.carddemo.test;

import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base test class providing common test functionality including test data setup,
 * cleanup methods, assertion utilities for BigDecimal precision matching COBOL behavior,
 * shared mock objects, and lifecycle management for consistent test execution.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractBaseTest {

    // Test data management
    protected TestDataGenerator testDataGenerator;
    protected List<Account> createdAccounts;
    protected List<Transaction> createdTransactions;
    
    // Mockito initialization
    private AutoCloseable mockCloseable;

    /**
     * Setup method called before each test
     */
    @BeforeEach
    public void setUp() {
        // Initialize Mockito annotations
        mockCloseable = MockitoAnnotations.openMocks(this);
        
        // Initialize test data generator
        testDataGenerator = new TestDataGenerator();
        
        // Initialize collections for cleanup
        createdAccounts = new ArrayList<>();
        createdTransactions = new ArrayList<>();
        
        // Load test fixtures
        loadTestFixtures();
        
        // Setup common mocks
        mockCommonDependencies();
    }

    /**
     * Cleanup method called after each test
     */
    @AfterEach
    public void tearDown() throws Exception {
        // Cleanup created test data
        cleanupTestData();
        
        // Close Mockito mocks
        if (mockCloseable != null) {
            mockCloseable.close();
        }
    }

    /**
     * Assert BigDecimal equality with COBOL precision semantics
     */
    protected void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertBigDecimalEquals(expected, actual, "BigDecimal values should be equal");
    }

    /**
     * Assert BigDecimal equality with custom message
     */
    protected void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual, String message) {
        if (expected == null && actual == null) {
            return;
        }
        
        if (expected == null || actual == null) {
            throw new AssertionError(message + " - One value is null: expected=" + expected + ", actual=" + actual);
        }
        
        // Apply COBOL rounding for comparison
        BigDecimal expectedRounded = expected.setScale(2, RoundingMode.HALF_UP);
        BigDecimal actualRounded = actual.setScale(2, RoundingMode.HALF_UP);
        
        if (expectedRounded.compareTo(actualRounded) != 0) {
            throw new AssertionError(message + " - Expected: " + expectedRounded + ", but was: " + actualRounded);
        }
    }

    /**
     * Assert BigDecimal within tolerance for floating point precision
     */
    protected void assertBigDecimalWithinTolerance(BigDecimal expected, BigDecimal actual, BigDecimal tolerance) {
        if (expected == null && actual == null) {
            return;
        }
        
        if (expected == null || actual == null) {
            throw new AssertionError("One value is null: expected=" + expected + ", actual=" + actual);
        }
        
        BigDecimal difference = expected.subtract(actual).abs();
        if (difference.compareTo(tolerance) > 0) {
            throw new AssertionError("Values differ by more than tolerance: expected=" + expected + 
                ", actual=" + actual + ", difference=" + difference + ", tolerance=" + tolerance);
        }
    }

    /**
     * Load test fixtures for consistent test setup
     */
    protected void loadTestFixtures() {
        // Override in subclasses to load specific test data
    }

    /**
     * Create a test account with default values
     */
    protected Account createTestAccount() {
        Account account = testDataGenerator.generateValidAccount();
        createdAccounts.add(account);
        return account;
    }

    /**
     * Create a test account with specific balance and credit limit
     */
    protected Account createTestAccount(BigDecimal balance, BigDecimal creditLimit) {
        Account account = testDataGenerator.generateValidAccount(balance, creditLimit);
        createdAccounts.add(account);
        return account;
    }

    /**
     * Create a test transaction with default values
     */
    protected Transaction createTestTransaction() {
        Transaction transaction = testDataGenerator.generateTransactionData();
        createdTransactions.add(transaction);
        return transaction;
    }

    /**
     * Create a test transaction with specific amount
     */
    protected Transaction createTestTransaction(BigDecimal amount) {
        Transaction transaction = testDataGenerator.generateTransactionData(amount);
        createdTransactions.add(transaction);
        return transaction;
    }

    /**
     * Validate COBOL precision for financial calculations
     */
    protected void validateCobolPrecision(BigDecimal value) {
        if (value == null) {
            throw new AssertionError("Value cannot be null for precision validation");
        }
        
        // Check that scale matches COBOL COMP-3 expectations (2 decimal places for currency)
        if (value.scale() > 2) {
            throw new AssertionError("Value scale exceeds COBOL COMP-3 precision: " + value.scale());
        }
        
        // Ensure value can be represented in COBOL COMP-3 format
        String stringValue = value.toPlainString();
        if (stringValue.contains("E") || stringValue.contains("e")) {
            throw new AssertionError("Value contains scientific notation, not compatible with COBOL COMP-3: " + stringValue);
        }
    }

    /**
     * Setup common mock dependencies used across multiple tests
     */
    protected void mockCommonDependencies() {
        // Override in subclasses to setup specific mocks
    }

    /**
     * Cleanup test data created during test execution
     */
    private void cleanupTestData() {
        // Clear collections
        createdAccounts.clear();
        createdTransactions.clear();
    }

    /**
     * Helper method to create COBOL-equivalent date
     */
    protected LocalDateTime createCobolDate(int year, int month, int day, int hour, int minute, int second) {
        return LocalDateTime.of(year, month, day, hour, minute, second);
    }

    /**
     * Helper method to create BigDecimal with COBOL precision
     */
    protected BigDecimal createCobolDecimal(String value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Helper method to create BigDecimal with specific scale
     */
    protected BigDecimal createCobolDecimal(String value, int scale) {
        return new BigDecimal(value).setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * Validate that a transaction follows COBOL business rules
     */
    protected void validateCobolTransactionRules(Transaction transaction) {
        if (transaction == null) {
            throw new AssertionError("Transaction cannot be null");
        }
        
        // Validate amount precision
        validateCobolPrecision(transaction.getAmount());
        
        // Validate required fields are present
        if (transaction.getAccountId() == null || transaction.getAccountId().trim().isEmpty()) {
            throw new AssertionError("Transaction account ID is required");
        }
        
        if (transaction.getTransactionDate() == null) {
            throw new AssertionError("Transaction date is required");
        }
        
        // Validate amount is positive for purchases
        if ("PURCHASE".equals(transaction.getTransactionType()) && 
            transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AssertionError("Purchase transaction amount must be positive");
        }
    }

    /**
     * Validate that an account follows COBOL business rules
     */
    protected void validateCobolAccountRules(Account account) {
        if (account == null) {
            throw new AssertionError("Account cannot be null");
        }
        
        // Validate balance precision
        if (account.getCurrentBalance() != null) {
            validateCobolPrecision(account.getCurrentBalance());
        }
        
        // Validate credit limit precision
        if (account.getCreditLimit() != null) {
            validateCobolPrecision(account.getCreditLimit());
        }
        
        // Validate required fields
        if (account.getAccountId() == null || account.getAccountId().trim().isEmpty()) {
            throw new AssertionError("Account ID is required");
        }
        
        // Validate credit limit is not exceeded
        if (account.getCurrentBalance() != null && account.getCreditLimit() != null &&
            account.getCurrentBalance().compareTo(account.getCreditLimit()) > 0) {
            throw new AssertionError("Account balance cannot exceed credit limit");
        }
    }

    /**
     * Create a valid account scenario for testing
     */
    protected Account createValidAccountScenario() {
        return createTestAccount(
            createCobolDecimal("500.00"), 
            createCobolDecimal("2000.00")
        );
    }

    /**
     * Create an account near credit limit for testing
     */
    protected Account createNearLimitAccountScenario() {
        return createTestAccount(
            createCobolDecimal("1950.00"), 
            createCobolDecimal("2000.00")
        );
    }

    /**
     * Create an account at credit limit for testing
     */
    protected Account createAtLimitAccountScenario() {
        return createTestAccount(
            createCobolDecimal("2000.00"), 
            createCobolDecimal("2000.00")
        );
    }
}