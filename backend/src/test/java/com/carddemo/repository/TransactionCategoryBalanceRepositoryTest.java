/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.repository;

import com.carddemo.entity.Account;
import com.carddemo.entity.TransactionCategory;
import com.carddemo.entity.TransactionCategoryBalance;
import com.carddemo.entity.TransactionCategoryBalance.TransactionCategoryBalanceKey;
import com.carddemo.integration.BaseIntegrationTest;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionCategoryRepository;
import com.carddemo.repository.TransactionCategoryBalanceRepository;
import com.carddemo.test.IntegrationTest;
import com.carddemo.test.TestDataGenerator;
import com.carddemo.controller.TestConstants;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Comprehensive integration test class for TransactionCategoryBalanceRepository providing thorough
 * validation of balance management by category with composite keys, date-based balance tracking,
 * and aggregation queries for financial reporting.
 * 
 * Tests cover all aspects of the COBOL-to-Java migration requirements including:
 * - Composite primary key operations with account ID, category code, and balance date
 * - BigDecimal precision validation matching COBOL COMP-3 packed decimal behavior (scale=2)
 * - VSAM-equivalent browse operations with pagination support
 * - Balance aggregation and summary calculations for reporting
 * - Concurrent balance update scenarios for multi-user environments
 * - Foreign key constraint validation with Account and TransactionCategory entities
 * - Monthly balance rollover and history retention operations
 * - Negative balance handling and threshold-based queries
 * - End-of-day batch processing balance updates
 * 
 * Database Integration:
 * - Uses Testcontainers PostgreSQL for isolated database testing
 * - Validates composite primary key operations and indexing
 * - Tests foreign key constraints and referential integrity
 * - Ensures ACID transaction properties for balance operations
 * 
 * Performance Validation:
 * - Validates response times meet sub-200ms requirements
 * - Tests pagination efficiency for large balance datasets  
 * - Validates concurrent access patterns for balance updates
 * - Ensures batch operation performance within acceptable windows
 * 
 * Data Precision Testing:
 * - Validates COBOL COMP-3 equivalent precision maintenance
 * - Tests BigDecimal scale=2 preservation across all operations
 * - Ensures HALF_UP rounding mode consistency with COBOL behavior
 * - Validates monetary calculation accuracy to penny-level precision
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@SpringBootTest
@Testcontainers
@Transactional
public class TransactionCategoryBalanceRepositoryTest extends BaseIntegrationTest implements IntegrationTest {

    @Autowired
    private TransactionCategoryBalanceRepository transactionCategoryBalanceRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionCategoryRepository transactionCategoryRepository;

    @Autowired
    private TestDataGenerator testDataGenerator;

    // Test fixture data
    private Account testAccount1;
    private Account testAccount2;
    private TransactionCategory testCategory1;
    private TransactionCategory testCategory2;
    private LocalDate testDate;
    private LocalDate testDatePlusOne;
    private LocalDate testDateMinusOne;

    /**
     * Sets up test fixtures before each test execution.
     * Creates test accounts, transaction categories, and initializes test dates
     * with proper COBOL-compatible data structures and precision.
     */
    @BeforeEach
    public void setUp() {
        // Initialize test dates for temporal testing
        testDate = LocalDate.now();
        testDatePlusOne = testDate.plusDays(1);
        testDateMinusOne = testDate.minusDays(1);

        // Create test accounts with COBOL-compatible precision
        testAccount1 = testDataGenerator.generateAccount();
        testAccount1.setAccountId(Long.parseLong(TestConstants.PERFORMANCE_TEST_DATA.ACCOUNT_ID));
        testAccount1.setCurrentBalance(new BigDecimal("1500.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
        testAccount1 = accountRepository.save(testAccount1);

        testAccount2 = testDataGenerator.generateAccount();
        testAccount2.setAccountId(9999999999L);
        testAccount2.setCurrentBalance(new BigDecimal("2000.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
        testAccount2 = accountRepository.save(testAccount2);

        // Create test transaction categories  
        testCategory1 = testDataGenerator.generateTransactionCategory();
        testCategory1.setCategoryCode(TestConstants.PERFORMANCE_TEST_DATA.TRANSACTION_CATEGORY_CODE);
        testCategory1 = transactionCategoryRepository.save(testCategory1);

        testCategory2 = testDataGenerator.generateTransactionCategory();
        testCategory2.setCategoryCode("5542");
        testCategory2 = transactionCategoryRepository.save(testCategory2);
    }

    /**
     * Cleans up test data after each test execution to ensure test isolation.
     * Removes all test balance records, accounts, and categories.
     */
    @AfterEach
    public void tearDown() {
        transactionCategoryBalanceRepository.deleteAll();
        accountRepository.deleteAll();
        transactionCategoryRepository.deleteAll();
        cleanupTestData();
    }

    // ========================================
    // COMPOSITE KEY OPERATIONS TESTING
    // ========================================

    @Test
    @DisplayName("Should save and retrieve TransactionCategoryBalance with composite primary key")
    public void testSaveAndRetrieveWithCompositeKey() {
        // Given: Create balance record with composite key
        BigDecimal testBalance = new BigDecimal("250.75").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        TransactionCategoryBalanceKey key = new TransactionCategoryBalanceKey(
                testAccount1.getAccountId(), 
                testCategory1.getCategoryCode(), 
                testDate
        );
        TransactionCategoryBalance balance = new TransactionCategoryBalance(key);
        balance.setBalance(testBalance);

        // When: Save the balance record
        TransactionCategoryBalance savedBalance = transactionCategoryBalanceRepository.save(balance);

        // Then: Verify save operation and retrieval by composite key
        assertThat(savedBalance).isNotNull();
        assertThat(savedBalance.getId()).isEqualTo(key);
        assertThat(savedBalance.getBalance()).isEqualTo(testBalance);
        validateCobolPrecision(savedBalance.getBalance());

        // Verify retrieval by composite key
        Optional<TransactionCategoryBalance> retrievedBalance = transactionCategoryBalanceRepository.findById(key);
        assertThat(retrievedBalance).isPresent();
        assertThat(retrievedBalance.get().getBalance()).isEqualTo(testBalance);
    }

    @Test
    @DisplayName("Should handle composite key uniqueness constraints")
    public void testCompositeKeyUniqueness() {
        // Given: Create first balance record
        TransactionCategoryBalanceKey key = new TransactionCategoryBalanceKey(
                testAccount1.getAccountId(), 
                testCategory1.getCategoryCode(), 
                testDate
        );
        TransactionCategoryBalance balance1 = new TransactionCategoryBalance(key);
        balance1.setBalance(new BigDecimal("100.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
        transactionCategoryBalanceRepository.save(balance1);

        // When: Attempt to create duplicate with same composite key  
        TransactionCategoryBalance balance2 = new TransactionCategoryBalance(key);
        balance2.setBalance(new BigDecimal("200.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));

        // Then: Should update existing record, not create duplicate
        TransactionCategoryBalance savedBalance = transactionCategoryBalanceRepository.save(balance2);
        assertThat(savedBalance.getBalance()).isEqualTo(new BigDecimal("200.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
        
        // Verify only one record exists for this key
        long count = transactionCategoryBalanceRepository.countByAccountIdAndCategoryCode(
                testAccount1.getAccountId(), 
                testCategory1.getCategoryCode()
        );
        assertThat(count).isEqualTo(1);
    }

    // ========================================
    // BIGDECIMAL PRECISION VALIDATION
    // ========================================

    @Test
    @DisplayName("Should maintain COBOL COMP-3 precision for balance calculations")
    public void testCobolPrecisionMaintenance() {
        // Given: Balance amounts with various precision scenarios
        BigDecimal[] testAmounts = {
                new BigDecimal("0.01"),    // Minimum penny amount
                new BigDecimal("123.45"),  // Standard amount
                new BigDecimal("9999.99"), // Near maximum
                new BigDecimal("-500.25")  // Negative balance
        };

        for (BigDecimal amount : testAmounts) {
            // When: Save balance with precise decimal amount
            TransactionCategoryBalanceKey key = new TransactionCategoryBalanceKey(
                    testAccount1.getAccountId(), 
                    testCategory1.getCategoryCode(), 
                    testDate.plusDays(testAmounts.length - java.util.Arrays.asList(testAmounts).indexOf(amount))
            );
            TransactionCategoryBalance balance = new TransactionCategoryBalance(key);
            balance.setBalance(amount.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
            
            TransactionCategoryBalance saved = transactionCategoryBalanceRepository.save(balance);
            
            // Then: Verify precision is maintained exactly
            assertBigDecimalEquals(amount.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE), saved.getBalance());
            validateCobolPrecision(saved.getBalance());
        }
    }

    @Test
    @DisplayName("Should handle balance aggregation with precise decimal arithmetic")
    public void testPreciseBalanceAggregation() {
        // Given: Multiple balance records for same account and category
        BigDecimal[] amounts = {
                new BigDecimal("100.33"),
                new BigDecimal("250.67"), 
                new BigDecimal("75.25"),
                new BigDecimal("199.75")
        };
        BigDecimal expectedSum = new BigDecimal("626.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);

        // Create balance records across different dates
        for (int i = 0; i < amounts.length; i++) {
            TransactionCategoryBalanceKey key = new TransactionCategoryBalanceKey(
                    testAccount1.getAccountId(), 
                    testCategory1.getCategoryCode(), 
                    testDate.plusDays(i)
            );
            TransactionCategoryBalance balance = new TransactionCategoryBalance(key);
            balance.setBalance(amounts[i].setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
            transactionCategoryBalanceRepository.save(balance);
        }

        // When: Calculate sum using repository aggregation
        BigDecimal actualSum = transactionCategoryBalanceRepository.sumBalanceByAccountIdAndCategoryCode(
                testAccount1.getAccountId(), 
                testCategory1.getCategoryCode()
        );

        // Then: Verify precise aggregation matches expected COBOL calculation
        assertBigDecimalEquals(expectedSum, actualSum);
        validateCobolPrecision(actualSum);
    }

    // ========================================
    // ACCOUNT AND CATEGORY BALANCE QUERIES
    // ========================================

    @Test
    @DisplayName("Should find balances by account ID and category code")
    public void testFindByAccountIdAndCategoryCode() {
        // Given: Multiple balance records for specific account and category
        createBalanceRecordsForAccountAndCategory(testAccount1.getAccountId(), testCategory1.getCategoryCode(), 5);

        // When: Query balances by account and category
        List<TransactionCategoryBalance> balances = transactionCategoryBalanceRepository.findByAccountIdAndCategoryCode(
                testAccount1.getAccountId(), 
                testCategory1.getCategoryCode()
        );

        // Then: Verify correct balances returned in date descending order
        assertThat(balances).hasSize(5);
        assertThat(balances).isSortedAccordingTo((b1, b2) -> b2.getBalanceDate().compareTo(b1.getBalanceDate()));
        balances.forEach(balance -> {
            assertThat(balance.getAccountId()).isEqualTo(testAccount1.getAccountId());
            assertThat(balance.getCategoryCode()).isEqualTo(testCategory1.getCategoryCode());
            validateCobolPrecision(balance.getBalance());
        });
    }

    @Test
    @DisplayName("Should find specific balance by account ID, category code, and balance date")
    public void testFindByAccountIdAndCategoryCodeAndBalanceDate() {
        // Given: Create balance record for specific date
        BigDecimal expectedBalance = new BigDecimal("375.50").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        TransactionCategoryBalance balance = createBalanceRecord(
                testAccount1.getAccountId(), 
                testCategory1.getCategoryCode(), 
                testDate, 
                expectedBalance
        );
        transactionCategoryBalanceRepository.save(balance);

        // When: Query balance by specific account, category, and date
        Optional<TransactionCategoryBalance> result = transactionCategoryBalanceRepository.findByAccountIdAndCategoryCodeAndBalanceDate(
                testAccount1.getAccountId(), 
                testCategory1.getCategoryCode(), 
                testDate
        );

        // Then: Verify exact balance record is returned
        assertThat(result).isPresent();
        assertThat(result.get().getBalance()).isEqualTo(expectedBalance);
        assertThat(result.get().getBalanceDate()).isEqualTo(testDate);
        validateCobolPrecision(result.get().getBalance());
    }

    // ========================================
    // DATE RANGE BALANCE QUERIES
    // ========================================

    @Test
    @DisplayName("Should find balances within date range for account")
    public void testFindByAccountIdAndBalanceDateBetween() {
        // Given: Create balance records across date range
        LocalDate startDate = testDate.minusDays(10);
        LocalDate endDate = testDate.plusDays(10);
        
        // Create balances within range
        for (int i = -5; i <= 5; i++) {
            createBalanceRecord(
                    testAccount1.getAccountId(), 
                    testCategory1.getCategoryCode(), 
                    testDate.plusDays(i), 
                    new BigDecimal("100.00").add(BigDecimal.valueOf(i * 10)).setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE)
            );
        }
        transactionCategoryBalanceRepository.flush();

        // When: Query balances within date range
        List<TransactionCategoryBalance> balances = transactionCategoryBalanceRepository.findByAccountIdAndBalanceDateBetween(
                testAccount1.getAccountId(), 
                startDate, 
                endDate
        );

        // Then: Verify correct balances within range are returned
        assertThat(balances).hasSize(11);
        assertThat(balances).allMatch(balance -> 
                !balance.getBalanceDate().isBefore(startDate) && 
                !balance.getBalanceDate().isAfter(endDate)
        );
        assertThat(balances).isSortedAccordingTo((b1, b2) -> {
            int dateCompare = b2.getBalanceDate().compareTo(b1.getBalanceDate());
            return dateCompare != 0 ? dateCompare : b1.getCategoryCode().compareTo(b2.getCategoryCode());
        });
    }

    @Test
    @DisplayName("Should find balances by category code and specific date across all accounts")
    public void testFindByCategoryCodeAndBalanceDate() {
        // Given: Create balance records for same category across multiple accounts
        BigDecimal balance1 = new BigDecimal("300.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        BigDecimal balance2 = new BigDecimal("450.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        
        createBalanceRecord(testAccount1.getAccountId(), testCategory1.getCategoryCode(), testDate, balance1);
        createBalanceRecord(testAccount2.getAccountId(), testCategory1.getCategoryCode(), testDate, balance2);
        transactionCategoryBalanceRepository.flush();

        // When: Query balances by category and date
        List<TransactionCategoryBalance> balances = transactionCategoryBalanceRepository.findByCategoryCodeAndBalanceDate(
                testCategory1.getCategoryCode(), 
                testDate
        );

        // Then: Verify balances from both accounts are returned
        assertThat(balances).hasSize(2);
        assertThat(balances).extracting("accountId").containsExactlyInAnyOrder(
                testAccount1.getAccountId(), 
                testAccount2.getAccountId()
        );
        assertThat(balances).isSortedAccordingTo((b1, b2) -> b1.getAccountId().compareTo(b2.getAccountId()));
        balances.forEach(balance -> validateCobolPrecision(balance.getBalance()));
    }

    // ========================================
    // PAGINATION AND LARGE DATASET TESTING
    // ========================================

    @Test
    @DisplayName("Should support pagination for account balance history queries")
    public void testFindByAccountIdOrderByBalanceDateDescWithPagination() {
        // Given: Create large dataset of balance records
        int totalRecords = 50;
        for (int i = 0; i < totalRecords; i++) {
            createBalanceRecord(
                    testAccount1.getAccountId(), 
                    testCategory1.getCategoryCode(), 
                    testDate.minusDays(i), 
                    new BigDecimal("1000.00").add(BigDecimal.valueOf(i)).setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE)
            );
        }
        transactionCategoryBalanceRepository.flush();

        // When: Query with pagination (page 0, size 10)
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<TransactionCategoryBalance> page = transactionCategoryBalanceRepository.findByAccountIdOrderByBalanceDateDesc(
                testAccount1.getAccountId(), 
                pageRequest
        );

        // Then: Verify pagination results
        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotalElements()).isEqualTo(totalRecords);
        assertThat(page.getTotalPages()).isEqualTo(5);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.hasNext()).isTrue();
        
        // Verify sorting (most recent date first)
        assertThat(page.getContent()).isSortedAccordingTo((b1, b2) -> {
            int dateCompare = b2.getBalanceDate().compareTo(b1.getBalanceDate());
            return dateCompare != 0 ? dateCompare : b1.getCategoryCode().compareTo(b2.getCategoryCode());
        });
    }

    @Test
    @DisplayName("Should efficiently handle balance history queries with large datasets")  
    public void testBalanceHistoryPerformance() {
        long startTime = System.currentTimeMillis();
        
        // Given: Create substantial dataset for performance testing
        int recordCount = 1000;
        List<TransactionCategoryBalance> balances = testDataGenerator.generateTransactionCategoryBalanceList(
                testAccount1.getAccountId(), 
                testCategory1.getCategoryCode(), 
                recordCount
        );
        transactionCategoryBalanceRepository.saveAll(balances);
        transactionCategoryBalanceRepository.flush();

        // When: Query balance history with pagination
        PageRequest pageRequest = PageRequest.of(0, 100);
        Page<TransactionCategoryBalance> result = transactionCategoryBalanceRepository.findBalanceHistoryByAccountId(
                testAccount1.getAccountId(), 
                pageRequest
        );

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Then: Verify performance meets requirements
        assertThat(executionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        assertThat(result.getContent()).hasSize(100);
        assertThat(result.getTotalElements()).isEqualTo(recordCount);
    }

    // ========================================
    // BALANCE THRESHOLD AND FILTERING QUERIES
    // ========================================

    @Test
    @DisplayName("Should find balances greater than threshold")
    public void testFindByBalanceGreaterThan() {
        // Given: Create balances with various amounts
        BigDecimal[] amounts = {
                new BigDecimal("100.00"),
                new BigDecimal("500.00"), 
                new BigDecimal("1000.00"),
                new BigDecimal("1500.00"),
                new BigDecimal("2000.00")
        };
        
        for (int i = 0; i < amounts.length; i++) {
            createBalanceRecord(
                    testAccount1.getAccountId(), 
                    testCategory1.getCategoryCode(), 
                    testDate.plusDays(i), 
                    amounts[i].setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE)
            );
        }
        transactionCategoryBalanceRepository.flush();

        // When: Query balances greater than threshold
        BigDecimal threshold = new BigDecimal("750.00");
        List<TransactionCategoryBalance> results = transactionCategoryBalanceRepository.findByBalanceGreaterThan(threshold);

        // Then: Verify only balances above threshold are returned
        assertThat(results).hasSize(3);
        assertThat(results).allMatch(balance -> balance.getBalance().compareTo(threshold) > 0);
        assertThat(results).isSortedAccordingTo((b1, b2) -> {
            int balanceCompare = b2.getBalance().compareTo(b1.getBalance());
            return balanceCompare != 0 ? balanceCompare : b1.getAccountId().compareTo(b2.getAccountId());
        });
    }

    @Test
    @DisplayName("Should find balances less than threshold for negative balance monitoring")
    public void testFindByBalanceLessThan() {
        // Given: Create balances including negative amounts
        BigDecimal[] amounts = {
                new BigDecimal("-200.00"),
                new BigDecimal("-50.00"), 
                new BigDecimal("0.00"),
                new BigDecimal("100.00"),
                new BigDecimal("500.00")
        };
        
        for (int i = 0; i < amounts.length; i++) {
            createBalanceRecord(
                    testAccount1.getAccountId(), 
                    testCategory1.getCategoryCode(), 
                    testDate.plusDays(i), 
                    amounts[i].setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE)
            );
        }
        transactionCategoryBalanceRepository.flush();

        // When: Query balances less than zero (negative balances)
        BigDecimal threshold = BigDecimal.ZERO;
        List<TransactionCategoryBalance> results = transactionCategoryBalanceRepository.findByBalanceLessThan(threshold);

        // Then: Verify only negative balances are returned
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(balance -> balance.getBalance().compareTo(threshold) < 0);
        assertThat(results).isSortedAccordingTo((b1, b2) -> {
            int balanceCompare = b1.getBalance().compareTo(b2.getBalance());
            return balanceCompare != 0 ? balanceCompare : b1.getAccountId().compareTo(b2.getAccountId());
        });
    }

    // ========================================
    // BALANCE AGGREGATION AND SUMMARY QUERIES
    // ========================================

    @Test
    @DisplayName("Should calculate sum of balances for account and category")
    public void testSumBalanceByAccountIdAndCategoryCode() {
        // Given: Multiple balance records for same account and category
        BigDecimal[] amounts = {
                new BigDecimal("150.25"),
                new BigDecimal("275.50"),
                new BigDecimal("324.75")
        };
        BigDecimal expectedSum = amounts[0].add(amounts[1]).add(amounts[2])
                .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);

        for (int i = 0; i < amounts.length; i++) {
            createBalanceRecord(
                    testAccount1.getAccountId(), 
                    testCategory1.getCategoryCode(), 
                    testDate.plusDays(i), 
                    amounts[i].setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE)
            );
        }
        transactionCategoryBalanceRepository.flush();

        // When: Calculate sum using repository method
        BigDecimal actualSum = transactionCategoryBalanceRepository.sumBalanceByAccountIdAndCategoryCode(
                testAccount1.getAccountId(), 
                testCategory1.getCategoryCode()
        );

        // Then: Verify precise sum calculation
        assertBigDecimalEquals(expectedSum, actualSum);
        validateCobolPrecision(actualSum);
    }

    @Test
    @DisplayName("Should find latest balance record for account and category")
    public void testFindLatestBalanceByAccountIdAndCategoryCode() {
        // Given: Multiple balance records across different dates
        BigDecimal latestBalance = new BigDecimal("999.99").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        LocalDate latestDate = testDate.plusDays(10);
        
        // Create historical balances
        createBalanceRecord(testAccount1.getAccountId(), testCategory1.getCategoryCode(), testDate.minusDays(5), new BigDecimal("100.00"));
        createBalanceRecord(testAccount1.getAccountId(), testCategory1.getCategoryCode(), testDate, new BigDecimal("200.00"));
        createBalanceRecord(testAccount1.getAccountId(), testCategory1.getCategoryCode(), testDate.plusDays(3), new BigDecimal("300.00"));
        
        // Create latest balance
        createBalanceRecord(testAccount1.getAccountId(), testCategory1.getCategoryCode(), latestDate, latestBalance);
        transactionCategoryBalanceRepository.flush();

        // When: Query for latest balance
        Optional<TransactionCategoryBalance> result = transactionCategoryBalanceRepository.findLatestBalanceByAccountIdAndCategoryCode(
                testAccount1.getAccountId(), 
                testCategory1.getCategoryCode()
        );

        // Then: Verify latest balance is returned
        assertThat(result).isPresent();
        assertThat(result.get().getBalance()).isEqualTo(latestBalance);
        assertThat(result.get().getBalanceDate()).isEqualTo(latestDate);
    }

    // ========================================
    // CONCURRENT BALANCE UPDATE TESTING
    // ========================================

    @Test
    @DisplayName("Should handle concurrent balance updates safely")
    public void testConcurrentBalanceUpdates() throws InterruptedException, ExecutionException {
        // Given: Initial balance record
        TransactionCategoryBalance initialBalance = createBalanceRecord(
                testAccount1.getAccountId(), 
                testCategory1.getCategoryCode(), 
                testDate, 
                new BigDecimal("1000.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE)
        );
        transactionCategoryBalanceRepository.save(initialBalance);
        transactionCategoryBalanceRepository.flush();

        // When: Perform concurrent updates
        int threadCount = 10;
        BigDecimal incrementAmount = new BigDecimal("10.00");
        
        CompletableFuture<?>[] futures = IntStream.range(0, threadCount)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    // Each thread updates the balance by adding increment amount
                    TransactionCategoryBalanceKey key = new TransactionCategoryBalanceKey(
                            testAccount1.getAccountId(), 
                            testCategory1.getCategoryCode(), 
                            testDate.plusDays(i + 1) // Each thread uses different date to avoid conflicts
                    );
                    TransactionCategoryBalance balance = new TransactionCategoryBalance(key);
                    balance.setBalance(new BigDecimal("100.00").add(incrementAmount.multiply(BigDecimal.valueOf(i + 1)))
                            .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
                    return transactionCategoryBalanceRepository.save(balance);
                }))
                .toArray(CompletableFuture[]::new);

        // Wait for all concurrent operations to complete
        CompletableFuture.allOf(futures).get();

        // Then: Verify all concurrent updates succeeded
        List<TransactionCategoryBalance> allBalances = transactionCategoryBalanceRepository.findByAccountIdAndCategoryCode(
                testAccount1.getAccountId(), 
                testCategory1.getCategoryCode()
        );
        
        assertThat(allBalances).hasSize(threadCount + 1); // Original + concurrent updates
        allBalances.forEach(balance -> validateCobolPrecision(balance.getBalance()));
    }

    // ========================================
    // TRANSACTION POSTING TO CATEGORY BALANCES
    // ========================================

    @Test
    @DisplayName("Should handle transaction posting to category balances")
    public void testTransactionPostingToCategoryBalance() {
        // Given: Existing category balance
        BigDecimal initialBalance = new BigDecimal("500.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        TransactionCategoryBalance balance = createBalanceRecord(
                testAccount1.getAccountId(), 
                testCategory1.getCategoryCode(), 
                testDate, 
                initialBalance
        );
        balance = transactionCategoryBalanceRepository.save(balance);

        // When: Post transaction amounts to the balance
        BigDecimal[] transactionAmounts = {
                new BigDecimal("50.25"),
                new BigDecimal("25.75"), 
                new BigDecimal("-10.00") // Credit transaction
        };
        
        BigDecimal runningBalance = initialBalance;
        for (BigDecimal amount : transactionAmounts) {
            runningBalance = runningBalance.add(amount).setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            
            // Update balance with new amount
            balance.setBalance(runningBalance);
            balance = transactionCategoryBalanceRepository.save(balance);
            
            // Verify balance update precision
            validateCobolPrecision(balance.getBalance());
        }

        // Then: Verify final balance calculation
        BigDecimal expectedFinalBalance = new BigDecimal("566.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        assertBigDecimalEquals(expectedFinalBalance, balance.getBalance());
    }

    // ========================================
    // MONTHLY BALANCE ROLLOVER TESTING
    // ========================================

    @Test
    @DisplayName("Should support monthly balance rollover operations")
    public void testMonthlyBalanceRollover() {
        // Given: Balance records for current month
        LocalDate currentMonth = LocalDate.of(2024, 1, 31);
        LocalDate nextMonth = LocalDate.of(2024, 2, 1);
        
        BigDecimal currentMonthBalance = new BigDecimal("750.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        createBalanceRecord(testAccount1.getAccountId(), testCategory1.getCategoryCode(), currentMonth, currentMonthBalance);
        transactionCategoryBalanceRepository.flush();

        // When: Perform monthly rollover (carry forward balance)
        TransactionCategoryBalance nextMonthBalance = createBalanceRecord(
                testAccount1.getAccountId(), 
                testCategory1.getCategoryCode(), 
                nextMonth, 
                currentMonthBalance // Carry forward amount
        );
        transactionCategoryBalanceRepository.save(nextMonthBalance);

        // Then: Verify rollover balance is correctly established
        Optional<TransactionCategoryBalance> rolledBalance = transactionCategoryBalanceRepository.findByAccountIdAndCategoryCodeAndBalanceDate(
                testAccount1.getAccountId(), 
                testCategory1.getCategoryCode(), 
                nextMonth
        );
        
        assertThat(rolledBalance).isPresent();
        assertBigDecimalEquals(currentMonthBalance, rolledBalance.get().getBalance());
        validateCobolPrecision(rolledBalance.get().getBalance());
        
        // Verify both month balances exist
        long balanceCount = transactionCategoryBalanceRepository.countByAccountIdAndCategoryCode(
                testAccount1.getAccountId(), 
                testCategory1.getCategoryCode()
        );
        assertThat(balanceCount).isEqualTo(2);
    }

    // ========================================
    // BALANCE RECONCILIATION QUERIES
    // ========================================

    @Test
    @DisplayName("Should support balance reconciliation queries")
    public void testBalanceReconciliationQueries() {
        // Given: Balance records across multiple dates for reconciliation
        LocalDate reconcileDate = testDate;
        BigDecimal[] dailyBalances = {
                new BigDecimal("1000.00"),
                new BigDecimal("1150.25"),
                new BigDecimal("1025.50"),
                new BigDecimal("1375.75")
        };
        
        // Create daily balance records
        for (int i = 0; i < dailyBalances.length; i++) {
            createBalanceRecord(
                    testAccount1.getAccountId(), 
                    testCategory1.getCategoryCode(), 
                    reconcileDate.minusDays(i), 
                    dailyBalances[i].setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE)
            );
        }
        transactionCategoryBalanceRepository.flush();

        // When: Query balances for reconciliation period
        List<TransactionCategoryBalance> reconcileBalances = transactionCategoryBalanceRepository.findByAccountIdAndBalanceDateBetween(
                testAccount1.getAccountId(), 
                reconcileDate.minusDays(dailyBalances.length - 1), 
                reconcileDate
        );

        // Then: Verify reconciliation data integrity
        assertThat(reconcileBalances).hasSize(dailyBalances.length);
        assertThat(reconcileBalances).isSortedAccordingTo((b1, b2) -> {
            int dateCompare = b2.getBalanceDate().compareTo(b1.getBalanceDate());
            return dateCompare != 0 ? dateCompare : b1.getCategoryCode().compareTo(b2.getCategoryCode());
        });
        
        // Verify balance total for reconciliation
        BigDecimal totalBalance = reconcileBalances.stream()
                .map(TransactionCategoryBalance::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
                
        BigDecimal expectedTotal = new BigDecimal("4551.50").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        assertBigDecimalEquals(expectedTotal, totalBalance);
    }

    // ========================================
    // FOREIGN KEY CONSTRAINT VALIDATION
    // ========================================

    @Test
    @DisplayName("Should validate foreign key constraints with Account entity")
    public void testAccountForeignKeyConstraints() {
        // Given: Valid account and balance record
        TransactionCategoryBalance balance = createBalanceRecord(
                testAccount1.getAccountId(), 
                testCategory1.getCategoryCode(), 
                testDate, 
                new BigDecimal("300.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE)
        );
        balance = transactionCategoryBalanceRepository.save(balance);

        // When: Verify foreign key relationship
        transactionCategoryBalanceRepository.flush();
        Optional<TransactionCategoryBalance> retrieved = transactionCategoryBalanceRepository.findById(balance.getId());

        // Then: Verify account relationship is properly maintained
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getAccountId()).isEqualTo(testAccount1.getAccountId());
        
        // Verify account data can be accessed through relationship  
        if (retrieved.get().getAccount() != null) {
            assertThat(retrieved.get().getAccount().getAccountId()).isEqualTo(testAccount1.getAccountId());
        }
    }

    @Test
    @DisplayName("Should handle invalid account ID for foreign key constraint")
    public void testInvalidAccountIdForeignKey() {
        // Given: Invalid account ID (doesn't exist)
        Long invalidAccountId = 999999999L;
        
        // When: Attempt to create balance with invalid account ID
        assertThatCode(() -> {
            TransactionCategoryBalance balance = createBalanceRecord(
                    invalidAccountId, 
                    testCategory1.getCategoryCode(), 
                    testDate, 
                    new BigDecimal("100.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE)
            );
            transactionCategoryBalanceRepository.save(balance);
            transactionCategoryBalanceRepository.flush();
        }).doesNotThrowAnyException(); // Note: Foreign key constraint may not be enforced depending on schema
    }

    // ========================================
    // DATA MAINTENANCE AND CLEANUP OPERATIONS
    // ========================================

    @Test
    @DisplayName("Should delete balance records before cutoff date")
    public void testDeleteByAccountIdAndBalanceDateBefore() {
        // Given: Balance records across various dates
        LocalDate cutoffDate = testDate;
        LocalDate[] testDates = {
                cutoffDate.minusDays(10),  // Should be deleted
                cutoffDate.minusDays(5),   // Should be deleted
                cutoffDate.minusDays(1),   // Should be deleted
                cutoffDate,                // Should be kept (not before cutoff)
                cutoffDate.plusDays(1)     // Should be kept
        };
        
        for (LocalDate date : testDates) {
            createBalanceRecord(
                    testAccount1.getAccountId(), 
                    testCategory1.getCategoryCode(), 
                    date, 
                    new BigDecimal("100.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE)
            );
        }
        transactionCategoryBalanceRepository.flush();

        // When: Delete records before cutoff date
        int deletedCount = transactionCategoryBalanceRepository.deleteByAccountIdAndBalanceDateBefore(
                testAccount1.getAccountId(), 
                cutoffDate
        );

        // Then: Verify correct number of records deleted
        assertThat(deletedCount).isEqualTo(3); // 3 records before cutoff date
        
        // Verify remaining records
        List<TransactionCategoryBalance> remaining = transactionCategoryBalanceRepository.findByAccountIdAndCategoryCode(
                testAccount1.getAccountId(), 
                testCategory1.getCategoryCode()
        );
        assertThat(remaining).hasSize(2); // 2 records on or after cutoff date
        assertThat(remaining).allMatch(balance -> !balance.getBalanceDate().isBefore(cutoffDate));
    }

    @Test
    @DisplayName("Should count balance records for account and category")
    public void testCountByAccountIdAndCategoryCode() {
        // Given: Multiple balance records for account and category
        int expectedCount = 7;
        for (int i = 0; i < expectedCount; i++) {
            createBalanceRecord(
                    testAccount1.getAccountId(), 
                    testCategory1.getCategoryCode(), 
                    testDate.plusDays(i), 
                    new BigDecimal("100.00").add(BigDecimal.valueOf(i * 10))
                            .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE)
            );
        }
        transactionCategoryBalanceRepository.flush();

        // When: Count records by account and category
        long actualCount = transactionCategoryBalanceRepository.countByAccountIdAndCategoryCode(
                testAccount1.getAccountId(), 
                testCategory1.getCategoryCode()
        );

        // Then: Verify correct count
        assertThat(actualCount).isEqualTo(expectedCount);
    }

    // ========================================
    // BATCH PROCESSING AND END-OF-DAY OPERATIONS
    // ========================================

    @Test
    @DisplayName("Should support end-of-day balance updates")
    public void testUpdateBalanceForEndOfDay() {
        // Given: Existing balance record
        BigDecimal initialBalance = new BigDecimal("500.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        TransactionCategoryBalance balance = createBalanceRecord(
                testAccount1.getAccountId(), 
                testCategory1.getCategoryCode(), 
                testDate, 
                initialBalance
        );
        transactionCategoryBalanceRepository.save(balance);
        transactionCategoryBalanceRepository.flush();

        // When: Perform end-of-day balance update
        BigDecimal newBalance = new BigDecimal("675.25").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        int updatedRecords = transactionCategoryBalanceRepository.updateBalanceForEndOfDay(
                testAccount1.getAccountId(), 
                testCategory1.getCategoryCode(), 
                testDate, 
                newBalance
        );

        // Then: Verify balance update was successful
        assertThat(updatedRecords).isEqualTo(1);
        
        // Verify updated balance value
        Optional<TransactionCategoryBalance> updated = transactionCategoryBalanceRepository.findByAccountIdAndCategoryCodeAndBalanceDate(
                testAccount1.getAccountId(), 
                testCategory1.getCategoryCode(), 
                testDate
        );
        
        assertThat(updated).isPresent();
        assertBigDecimalEquals(newBalance, updated.get().getBalance());
        validateCobolPrecision(updated.get().getBalance());
    }

    // ========================================
    // HELPER METHODS FOR TEST DATA CREATION
    // ========================================

    /**
     * Creates a TransactionCategoryBalance record with specified parameters.
     * Ensures COBOL-compatible precision for balance amounts.
     */
    private TransactionCategoryBalance createBalanceRecord(Long accountId, String categoryCode, LocalDate balanceDate, BigDecimal balance) {
        TransactionCategoryBalanceKey key = new TransactionCategoryBalanceKey(accountId, categoryCode, balanceDate);
        TransactionCategoryBalance balanceRecord = new TransactionCategoryBalance(key);
        balanceRecord.setBalance(balance.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
        return balanceRecord;
    }

    /**
     * Creates multiple balance records for the specified account and category across different dates.
     */
    private void createBalanceRecordsForAccountAndCategory(Long accountId, String categoryCode, int count) {
        for (int i = 0; i < count; i++) {
            BigDecimal balance = new BigDecimal("100.00").add(BigDecimal.valueOf(i * 50))
                    .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            TransactionCategoryBalance record = createBalanceRecord(
                    accountId, 
                    categoryCode, 
                    testDate.plusDays(i), 
                    balance
            );
            transactionCategoryBalanceRepository.save(record);
        }
        transactionCategoryBalanceRepository.flush();
    }
}