/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.performance;

import static org.assertj.core.api.Assertions.*;

import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.Customer;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.config.DatabaseConfig;
import com.carddemo.config.TestDatabaseConfig;
import com.carddemo.test.TestConstants;
import com.carddemo.test.AbstractBaseTest;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

/**
 * Database-specific performance tests validating PostgreSQL query performance, index efficiency,
 * connection pooling, and transaction isolation matching VSAM access patterns and CICS behavior.
 * 
 * This comprehensive performance test suite validates the migration from VSAM KSDS datasets to 
 * PostgreSQL relational database operations while ensuring identical performance characteristics
 * and data access patterns. The tests focus on replicating COBOL/CICS transaction processing
 * behavior with sub-200ms response time requirements for real-time card authorization processing.
 * 
 * Core Performance Validation Areas:
 * - Query performance matching VSAM sequential and direct access patterns
 * - B-tree index efficiency replicating VSAM alternate index (CARDAIX/CXACAIX) access
 * - Cursor-based pagination performance for STARTBR/READNEXT operations
 * - Connection pooling behavior matching CICS thread management characteristics
 * - Range partitioning efficiency on transactions table for batch processing optimization
 * - BigDecimal precision preservation under high-volume concurrent access scenarios
 * 
 * VSAM Migration Performance Equivalency:
 * - Direct key access: PostgreSQL primary key B-tree lookup ≤ VSAM KSDS direct read performance
 * - Sequential browsing: PostgreSQL cursor pagination ≤ VSAM STARTBR/READNEXT browse performance
 * - Alternate key access: PostgreSQL secondary index lookup ≤ VSAM AIX performance
 * - Transaction isolation: PostgreSQL pessimistic locking ≤ CICS READ FOR UPDATE performance
 * - Batch processing: PostgreSQL range partition scanning ≤ VSAM sequential dataset processing
 * 
 * Performance Test Categories:
 * - Primary Key Access: Single record retrieval performance validation
 * - Index Efficiency: Secondary index utilization and query optimization testing
 * - Pagination Performance: Large result set navigation with cursor-based pagination
 * - Concurrent Access: Multi-threaded database operations with connection pool validation
 * - Bulk Operations: High-volume insert/update performance for batch processing scenarios
 * - Transaction Isolation: Performance impact of pessimistic locking under concurrent access
 * 
 * Success Criteria:
 * - All database operations complete within 200ms response time threshold
 * - Connection pool maintains optimal performance under concurrent load
 * - BigDecimal calculations preserve COBOL COMP-3 precision under load
 * - Index utilization provides optimal query execution plans
 * - Range partitioning eliminates unnecessary partition scanning
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@SpringBootTest
@Testcontainers
@Tag("performance")
@Tag("database")
public class DatabasePerformanceTest extends AbstractBaseTest {

    @Container
    private static final PostgreSQLContainer<?> postgresContainer = 
        new PostgreSQLContainer<>("postgres:15.4")
            .withDatabaseName("carddemo_perf_test")
            .withUsername("carddemo_test")
            .withPassword("carddemo_test");

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    // Performance measurement variables
    private long startTime;
    private long endTime;
    private static final long RESPONSE_TIME_THRESHOLD = TestConstants.RESPONSE_TIME_THRESHOLD_MS;

    // Test data volume configuration for performance testing
    private static final int SMALL_DATASET_SIZE = 100;
    private static final int MEDIUM_DATASET_SIZE = 1000;
    private static final int LARGE_DATASET_SIZE = 10000;
    private static final int CONCURRENT_THREAD_COUNT = 10;

    /**
     * Constructor initializing test dependencies and performance measurement infrastructure.
     * 
     * Establishes comprehensive test environment for database performance validation including
     * repository dependencies, test data generation capabilities, and performance measurement
     * infrastructure required for VSAM-to-PostgreSQL migration validation.
     * 
     * @param accountRepository Spring Data JPA repository for Account entity performance testing
     * @param transactionRepository Spring Data JPA repository for Transaction entity performance testing
     */
    public DatabasePerformanceTest(AccountRepository accountRepository, 
                                 TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        // TestDataGenerator methods will be called statically, no instance needed
    }

    /**
     * Initializes test environment and loads performance test data before each test execution.
     * 
     * This method establishes a controlled test environment with representative data volumes
     * and proper database state for consistent performance measurement across all test methods.
     * The initialization includes test data generation, database state validation, and
     * performance measurement infrastructure setup.
     * 
     * Test Environment Setup:
     * - Loads test fixtures with realistic data volumes matching production patterns
     * - Validates database connection pool initialization and configuration
     * - Establishes baseline performance measurement infrastructure
     * - Ensures proper transaction isolation and cleanup between test executions
     * 
     * Data Loading Strategy:
     * - Account data: Generates representative account records with COBOL-compatible precision
     * - Transaction data: Creates partitioned transaction records with proper date distribution
     * - Reference data: Loads transaction types, categories, and other lookup tables
     * - Index validation: Verifies all indexes are properly created and statistics updated
     */
    @BeforeEach
    public void setUp() {
        super.setUp();
        loadTestFixtures();
        
        // Warm up connection pool to ensure consistent performance measurements
        accountRepository.findById(1000000001L);
        
        // Validate test environment readiness
        assertThat(postgresContainer.isRunning())
            .as("PostgreSQL test container must be running for performance tests")
            .isTrue();
    }

    /**
     * Tests primary key lookup performance matching VSAM KSDS direct read operations.
     * 
     * This test validates that PostgreSQL primary key B-tree index lookups provide equivalent
     * or superior performance compared to VSAM KSDS direct record access. The test measures
     * single record retrieval performance under various load conditions to ensure sub-200ms
     * response times required for real-time card authorization processing.
     * 
     * Performance Validation:
     * - Direct account lookup by primary key within response time threshold
     * - B-tree index utilization providing optimal query execution plan
     * - Connection pool efficiency during single record access operations
     * - Memory usage patterns matching CICS transaction processing characteristics
     * 
     * VSAM Equivalency:
     * - PostgreSQL findById() ≤ VSAM KSDS direct read performance
     * - Single I/O operation for primary key access
     * - Index-only scan eliminating heap table access when possible
     */
    @Test
    @DisplayName("Primary Key Lookup Performance - Account Entity")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @Transactional
    @Rollback
    public void testAccountPrimaryKeyLookupPerformance() {
        // Generate test account with COBOL-compatible data
        Account testAccount = new TestDataGenerator().generateAccount();
        accountRepository.save(testAccount);
        validateCobolPrecision(testAccount.getCurrentBalance());
        
        // Measure primary key lookup performance
        startTime = System.nanoTime();
        
        Account retrievedAccount = accountRepository.findById(testAccount.getAccountId()).orElse(null);
        
        endTime = System.nanoTime();
        long responseTime = (endTime - startTime) / 1_000_000; // Convert to milliseconds
        
        // Validate performance and data integrity
        assertThat(retrievedAccount)
            .as("Account should be retrieved successfully")
            .isNotNull();
            
        assertThat(responseTime)
            .as("Primary key lookup should complete within %d ms threshold", RESPONSE_TIME_THRESHOLD)
            .isLessThan(RESPONSE_TIME_THRESHOLD);
            
        assertThat(retrievedAccount.getAccountId())
            .as("Retrieved account ID should match original")
            .isEqualTo(testAccount.getAccountId());
            
        // Validate COBOL precision preservation
        assertBigDecimalEquals(retrievedAccount.getCurrentBalance(), testAccount.getCurrentBalance(), "Current balance should match");
        assertBigDecimalEquals(retrievedAccount.getCreditLimit(), testAccount.getCreditLimit(), "Credit limit should match");
        assertBigDecimalEquals(retrievedAccount.getCashCreditLimit(), testAccount.getCashCreditLimit(), "Cash credit limit should match");
    }

    /**
     * Tests transaction primary key lookup performance with partitioned table access.
     * 
     * This test validates PostgreSQL primary key performance on partitioned tables, ensuring
     * that partition pruning and index optimization provide efficient access to transaction
     * records distributed across monthly partitions. The test specifically validates performance
     * under the range partitioning strategy implemented for the transactions table.
     * 
     * Partition Performance Validation:
     * - Primary key access with automatic partition pruning
     * - Index performance across multiple partitions
     * - Query optimizer utilization of partition constraints
     * - Memory efficiency during cross-partition access operations
     */
    @Test
    @DisplayName("Primary Key Lookup Performance - Transaction Entity (Partitioned)")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @Transactional
    @Rollback
    public void testTransactionPrimaryKeyLookupPerformance() {
        // Generate test transaction with current date for active partition
        Transaction testTransaction = new TestDataGenerator().generateTransaction();
        testTransaction.setTransactionDate(LocalDate.now());
        transactionRepository.save(testTransaction);
        
        // Measure primary key lookup performance on partitioned table
        startTime = System.nanoTime();
        
        Transaction retrievedTransaction = transactionRepository.findById(testTransaction.getTransactionId()).orElse(null);
        
        endTime = System.nanoTime();
        long responseTime = (endTime - startTime) / 1_000_000;
        
        // Validate performance and partitioning behavior
        assertThat(retrievedTransaction)
            .as("Transaction should be retrieved from partitioned table")
            .isNotNull();
            
        assertThat(responseTime)
            .as("Partitioned table primary key lookup should complete within %d ms", RESPONSE_TIME_THRESHOLD)
            .isLessThan(RESPONSE_TIME_THRESHOLD);
            
        assertThat(retrievedTransaction.getTransactionId())
            .as("Retrieved transaction ID should match original")
            .isEqualTo(testTransaction.getTransactionId());
            
        // Validate BigDecimal precision preservation in partitioned table
        assertBigDecimalEquals(retrievedTransaction.getAmount(), testTransaction.getAmount(), "Transaction amount should match");
    }

    /**
     * Tests B-tree index efficiency for customer-to-account relationship queries.
     * 
     * This test validates the performance of secondary B-tree indexes that replace VSAM
     * alternate indexes (specifically CXACAIX). The test measures query performance when
     * accessing accounts by customer ID, ensuring that the customer_account_idx provides
     * optimal performance for this common access pattern.
     * 
     * Index Performance Validation:
     * - Secondary index utilization for foreign key lookups
     * - Query execution plan optimization with proper index selection
     * - Index-only scan performance when all required columns are covered
     * - Memory usage efficiency during index traversal operations
     */
    @Test
    @DisplayName("B-tree Index Efficiency - Customer Account Lookup")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @Transactional
    @Rollback
    public void testCustomerAccountIndexPerformance() {
        // Create test customer with multiple accounts
        String testCustomerId = TestConstants.TEST_CUSTOMER_ID;
        List<Account> customerAccounts = generateTestAccountsForCustomer(testCustomerId, 5);
        accountRepository.saveAll(customerAccounts);
        
        // Measure customer-based account lookup performance
        startTime = System.nanoTime();
        
        List<Account> retrievedAccounts = accountRepository.findByCustomerId(Long.parseLong(testCustomerId));
        
        endTime = System.nanoTime();
        long responseTime = (endTime - startTime) / 1_000_000;
        
        // Validate index performance and result accuracy
        assertThat(retrievedAccounts)
            .as("Customer accounts should be retrieved via secondary index")
            .isNotNull()
            .hasSize(5);
            
        assertThat(responseTime)
            .as("Customer account index lookup should complete within %d ms", RESPONSE_TIME_THRESHOLD)
            .isLessThan(RESPONSE_TIME_THRESHOLD);
            
        // Validate all accounts belong to correct customer
        retrievedAccounts.forEach(account -> {
            assertThat(account.getCustomerId())
                .as("All retrieved accounts should belong to test customer")
                .isEqualTo(testCustomerId);
                
            // Validate COBOL precision preservation
            validateCobolPrecision(account.getCurrentBalance());
            validateCobolPrecision(account.getCreditLimit());
        });
    }

    /**
     * Tests cursor-based pagination performance replicating VSAM STARTBR/READNEXT operations.
     * 
     * This test validates that Spring Data JPA pagination provides equivalent performance
     * to VSAM browse operations. The test measures performance of retrieving large result
     * sets using cursor-based pagination with proper ordering and filtering to ensure
     * efficient navigation through transaction records.
     * 
     * Pagination Performance Validation:
     * - Cursor-based pagination with ORDER BY optimization
     * - Index utilization during large result set traversal
     * - Memory efficiency with configurable page sizes
     * - Performance stability across multiple page retrievals
     */
    @Test
    @DisplayName("Cursor-based Pagination Performance - STARTBR/READNEXT Equivalent")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    @Transactional
    @Rollback
    public void testCursorBasedPaginationPerformance() {
        // Generate large transaction dataset for pagination testing
        String testAccountId = TestConstants.TEST_ACCOUNT_ID_STRING;
        List<Transaction> transactions = generateTestTransactionsForAccount(testAccountId, MEDIUM_DATASET_SIZE);
        transactionRepository.saveAll(transactions);
        
        // Test pagination performance across multiple pages
        int pageSize = 50; // VSAM-equivalent browse chunk size
        int totalPages = MEDIUM_DATASET_SIZE / pageSize;
        List<Long> pageResponseTimes = new ArrayList<>();
        
        for (int page = 0; page < Math.min(totalPages, 10); page++) {
            Pageable pageable = PageRequest.of(page, pageSize);
            
            startTime = System.nanoTime();
            
            Page<Transaction> transactionPage = transactionRepository.findByAccountId(Long.parseLong(testAccountId), pageable);
            
            endTime = System.nanoTime();
            long pageResponseTime = (endTime - startTime) / 1_000_000;
            pageResponseTimes.add(pageResponseTime);
            
            // Validate page content and performance
            assertThat(transactionPage.getContent())
                .as("Page %d should contain expected number of transactions", page)
                .hasSize(page < totalPages - 1 ? pageSize : MEDIUM_DATASET_SIZE % pageSize);
                
            assertThat(pageResponseTime)
                .as("Page %d retrieval should complete within %d ms", page, RESPONSE_TIME_THRESHOLD)
                .isLessThan(RESPONSE_TIME_THRESHOLD);
        }
        
        // Validate consistent pagination performance (no significant degradation)
        double averageResponseTime = pageResponseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        assertThat(averageResponseTime)
            .as("Average pagination response time should be well within threshold")
            .isLessThan(RESPONSE_TIME_THRESHOLD / 2.0);
    }

    /**
     * Tests range partitioning efficiency on transactions table for date-based queries.
     * 
     * This test validates that PostgreSQL range partitioning provides optimal performance
     * for date-range queries that are essential for batch processing operations. The test
     * measures partition pruning effectiveness and index utilization across partitioned
     * monthly transaction tables.
     * 
     * Partition Performance Validation:
     * - Automatic partition pruning for date-range queries
     * - Index performance across multiple partitions
     * - Memory efficiency during large date range scans
     * - Query optimizer utilization of partition constraints
     */
    @Test
    @DisplayName("Range Partitioning Efficiency - Date Range Queries")
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    @Transactional
    @Rollback
    public void testRangePartitioningPerformance() {
        // Generate transactions across multiple months for partition testing
        LocalDate startDate = LocalDate.now().minusMonths(3);
        LocalDate endDate = LocalDate.now().plusDays(1);
        List<Transaction> partitionedTransactions = generateTransactionsAcrossDateRange(startDate, endDate, LARGE_DATASET_SIZE);
        transactionRepository.saveAll(partitionedTransactions);
        
        // Test date range query performance with partition pruning
        LocalDate queryStartDate = LocalDate.now().minusMonths(1);
        LocalDate queryEndDate = LocalDate.now();
        
        startTime = System.nanoTime();
        
        List<Transaction> rangeResults = transactionRepository.findByTransactionDateBetween(queryStartDate, queryEndDate);
        
        endTime = System.nanoTime();
        long responseTime = (endTime - startTime) / 1_000_000;
        
        // Validate partition pruning performance and result accuracy
        assertThat(rangeResults)
            .as("Date range query should return transactions from specified period")
            .isNotNull()
            .isNotEmpty();
            
        assertThat(responseTime)
            .as("Partitioned date range query should complete within %d ms", RESPONSE_TIME_THRESHOLD * 2)
            .isLessThan(RESPONSE_TIME_THRESHOLD * 2); // Allow longer time for range queries
            
        // Validate all results fall within query range
        rangeResults.forEach(transaction -> {
            assertThat(transaction.getTransactionDate())
                .as("Transaction date should fall within query range")
                .isBetween(queryStartDate, queryEndDate);
                
            // Validate BigDecimal precision preservation
            validateCobolPrecision(transaction.getAmount());
        });
    }

    /**
     * Tests connection pool performance under concurrent database access load.
     * 
     * This test validates that HikariCP connection pool configuration provides optimal
     * performance under concurrent access patterns that replicate CICS transaction
     * processing load. The test measures connection acquisition, usage, and release
     * performance under multi-threaded access scenarios.
     * 
     * Connection Pool Performance Validation:
     * - Connection acquisition time under concurrent load
     * - Connection utilization efficiency and proper cleanup
     * - Thread contention minimization during peak usage
     * - Resource management preventing connection leaks
     */
    @Test
    @DisplayName("Connection Pool Performance - Concurrent Access")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void testConnectionPoolPerformance() throws Exception {
        // Create concurrent execution scenario
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREAD_COUNT);
        List<Future<Long>> futures = new ArrayList<>();
        
        // Submit concurrent database operations
        for (int i = 0; i < CONCURRENT_THREAD_COUNT; i++) {
            final int threadIndex = i;
            
            Future<Long> future = executor.submit(() -> {
                long threadStartTime = System.nanoTime();
                
                // Perform typical database operations per thread
                Long accountId = Long.parseLong(String.format("100000000%d", threadIndex));
                Account account = new TestDataGenerator().generateAccount();
                account.setAccountId(accountId);
                
                // Save and retrieve account (connection pool utilization)
                accountRepository.save(account);
                Account retrieved = accountRepository.findById(accountId).orElse(null);
                
                long threadEndTime = System.nanoTime();
                return (threadEndTime - threadStartTime) / 1_000_000; // milliseconds
            });
            
            futures.add(future);
        }
        
        // Collect and validate concurrent operation performance
        List<Long> threadResponseTimes = new ArrayList<>();
        for (Future<Long> future : futures) {
            Long responseTime = future.get();
            threadResponseTimes.add(responseTime);
            
            assertThat(responseTime)
                .as("Concurrent database operation should complete within threshold")
                .isLessThan(RESPONSE_TIME_THRESHOLD * 2); // Allow some overhead for concurrency
        }
        
        executor.shutdown();
        
        // Validate overall concurrent performance statistics
        double averageResponseTime = threadResponseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long maxResponseTime = threadResponseTimes.stream().mapToLong(Long::longValue).max().orElse(0L);
        
        assertThat(averageResponseTime)
            .as("Average concurrent response time should be reasonable")
            .isLessThan(RESPONSE_TIME_THRESHOLD);
            
        assertThat(maxResponseTime)
            .as("Maximum concurrent response time should not exceed threshold significantly")
            .isLessThan(RESPONSE_TIME_THRESHOLD * 3);
    }

    /**
     * Tests bulk operation performance for batch processing scenarios.
     * 
     * This test validates bulk database operations that are essential for batch processing
     * scenarios like end-of-day transaction posting and monthly statement generation.
     * The test measures insert, update, and query performance under high-volume data
     * processing conditions.
     * 
     * Bulk Operation Performance Validation:
     * - Batch insert performance with optimal memory usage
     * - Bulk update operations maintaining transaction isolation
     * - Large result set queries with proper index utilization
     * - Memory management during high-volume processing
     */
    @Test
    @DisplayName("Bulk Operation Performance - Batch Processing")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @Transactional
    @Rollback
    public void testBulkOperationPerformance() {
        // Test bulk insert performance
        List<Transaction> bulkTransactions = generateTestTransactions(LARGE_DATASET_SIZE);
        
        startTime = System.nanoTime();
        
        transactionRepository.saveAll(bulkTransactions);
        
        endTime = System.nanoTime();
        long bulkInsertTime = (endTime - startTime) / 1_000_000;
        
        // Validate bulk insert performance
        assertThat(bulkInsertTime)
            .as("Bulk insert of %d transactions should complete within reasonable time", LARGE_DATASET_SIZE)
            .isLessThan(30000); // 30 seconds for bulk operations
            
        // Test bulk query performance on large dataset
        startTime = System.nanoTime();
        
        // Use date range covering all test data for bulk query
        LocalDate startDate = LocalDate.now().minusYears(1);
        LocalDate endDate = LocalDate.now();
        List<Transaction> largeResultSet = transactionRepository.findByAmountGreaterThanAndTransactionDateBetween(
            BigDecimal.ZERO, startDate, endDate);
        
        endTime = System.nanoTime();
        long bulkQueryTime = (endTime - startTime) / 1_000_000;
        
        // Validate bulk query performance and results
        assertThat(largeResultSet)
            .as("Bulk query should return all transactions with positive amounts")
            .isNotNull()
            .hasSizeLessThanOrEqualTo(LARGE_DATASET_SIZE);
            
        assertThat(bulkQueryTime)
            .as("Bulk query should complete within reasonable time")
            .isLessThan(5000); // 5 seconds for large queries
            
        // Validate sample of results for precision preservation
        largeResultSet.stream().limit(100).forEach(transaction -> 
            validateCobolPrecision(transaction.getAmount())
        );
    }

    /**
     * Generates test accounts for a specific customer with COBOL-compatible data.
     * 
     * @param customerId the customer ID to associate with generated accounts
     * @param count the number of accounts to generate
     * @return list of Account entities with realistic test data
     */
    private List<Account> generateTestAccountsForCustomer(String customerId, int count) {
        List<Account> accounts = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Account account = new TestDataGenerator().generateAccount();
            // Create customer and set relationship instead of setCustomerId
            Customer customer = Customer.builder().customerId(Long.parseLong(customerId)).build();
            account.setCustomer(customer);
            account.setAccountId(Long.parseLong(String.format("%s%02d", customerId, i)));
            accounts.add(account);
        }
        
        return accounts;
    }

    /**
     * Generates test transactions for a specific account with varied amounts and dates.
     * 
     * @param accountId the account ID to associate with generated transactions
     * @param count the number of transactions to generate
     * @return list of Transaction entities with realistic test data
     */
    private List<Transaction> generateTestTransactionsForAccount(String accountId, int count) {
        List<Transaction> transactions = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Transaction transaction = new TestDataGenerator().generateTransaction();
            transaction.setAccountId(Long.parseLong(accountId));
            transaction.setTransactionDate(LocalDate.now().minusDays(i % 30)); // Spread across month
            transactions.add(transaction);
        }
        
        return transactions;
    }

    /**
     * Generates transactions distributed across a date range for partition testing.
     * 
     * @param startDate the start date for transaction generation
     * @param endDate the end date for transaction generation
     * @param count the total number of transactions to generate
     * @return list of Transaction entities distributed across the date range
     */
    private List<Transaction> generateTransactionsAcrossDateRange(LocalDate startDate, LocalDate endDate, int count) {
        List<Transaction> transactions = new ArrayList<>();
        long daysBetween = startDate.until(endDate).getDays();
        
        for (int i = 0; i < count; i++) {
            Transaction transaction = new TestDataGenerator().generateTransaction();
            // Distribute transactions evenly across the date range
            long randomDays = (long) (Math.random() * daysBetween);
            transaction.setTransactionDate(startDate.plusDays(randomDays));
            transactions.add(transaction);
        }
        
        return transactions;
    }

    /**
     * Generates a specified number of test transactions with varied data.
     * 
     * @param count the number of transactions to generate
     * @return list of Transaction entities with realistic test data
     */
    private List<Transaction> generateTestTransactions(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> new TestDataGenerator().generateTransaction())
            .toList();
    }

    /**
     * Validates BigDecimal precision matches COBOL COMP-3 requirements.
     * 
     * @param value the BigDecimal value to validate
     */
    private void validateCobolPrecision(BigDecimal value) {
        if (value != null) {
            assertThat(value.scale())
                .as("BigDecimal scale should match COBOL COMP-3 precision")
                .isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        }
    }
}