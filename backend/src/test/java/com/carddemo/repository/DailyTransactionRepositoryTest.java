/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.repository;

import com.carddemo.repository.DailyTransactionRepository;
import com.carddemo.entity.DailyTransaction;
import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.TestDataGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive integration test class for DailyTransactionRepository validating batch processing operations,
 * daily aggregations, report generation, and data archival within the 4-hour processing window requirement.
 *
 * This test class implements complete integration testing for the DailyTransactionRepository, ensuring
 * functional parity with the original COBOL CBTRN01C batch program. Tests validate bulk insert operations,
 * daily aggregation queries, partition-aware queries, and performance characteristics required for
 * Spring Batch job processing within the mandated 4-hour processing window.
 *
 * Key Testing Areas:
 * - Bulk data loading and transaction processing 
 * - Daily aggregation calculations (sum, count, avg)
 * - Date-based reporting and filtering operations
 * - Partition-aware query performance validation
 * - Data archival and retention management
 * - Transaction categorization and merchant analysis
 * - Checkpoint and restart capability validation
 * - Concurrent batch job execution scenarios
 * - Memory-optimized streaming query performance
 * - Financial precision validation for COBOL parity
 *
 * Performance Requirements:
 * - Complete batch processing within 4-hour window
 * - Support concurrent batch job execution
 * - Memory-efficient streaming for large datasets
 * - Sub-200ms response times for daily queries
 * - Rollback capability on batch failures
 *
 * COBOL Program Equivalence:
 * This test validates the Java/Spring implementation against the original COBOL CBTRN01C program
 * which processes daily transaction files (DALYTRAN-FILE) by reading records, performing lookups
 * against XREF and ACCOUNT files, and posting transactions. The Spring Batch implementation
 * must maintain identical processing logic and data accuracy.
 *
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@DataJpaTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({TestDataGenerator.class})
@DisplayName("Daily Transaction Repository Integration Tests")
public class DailyTransactionRepositoryTest extends AbstractBaseTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("carddemo_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @Autowired
    private DailyTransactionRepository dailyTransactionRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private TestDataGenerator testDataGenerator;

    private static final int BULK_INSERT_SIZE = 1000;
    private static final int LARGE_DATASET_SIZE = 10000;
    private static final BigDecimal PRECISION_TOLERANCE = new BigDecimal("0.01");

    /**
     * Dynamic property configuration for Testcontainers database connection.
     * Configures Spring DataSource properties from the PostgreSQL container instance.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("logging.level.org.hibernate.SQL", () -> "DEBUG");
    }

    /**
     * Test setup method preparing test data and repository for each test execution.
     * Implements comprehensive test data initialization with COBOL-compatible data patterns.
     */
    @BeforeEach
    public void setupTest() {
        super.setUp();
        // Clear any existing test data
        dailyTransactionRepository.deleteAll();
        testEntityManager.flush();
        testEntityManager.clear();
    }

    // ========================================
    // BULK INSERT OPERATIONS TESTING
    // ========================================

    /**
     * Test bulk insert operations for daily batch loads.
     * Validates repository's ability to handle large-volume data insertion
     * efficiently within batch processing window constraints.
     */
    @Test
    @DisplayName("Bulk Insert Operations - Daily Batch Load Validation")
    public void testBulkInsertOperations_DailyBatchLoad() {
        // Given: Generate bulk test data matching COBOL DALYTRAN-RECORD structure
        List<DailyTransaction> transactions = testDataGenerator.generateDailyTransactionBatch(BULK_INSERT_SIZE);
        
        // When: Perform bulk insert operation
        long startTime = System.currentTimeMillis();
        List<DailyTransaction> savedTransactions = dailyTransactionRepository.saveAll(transactions);
        dailyTransactionRepository.flush();
        long endTime = System.currentTimeMillis();
        
        // Then: Validate bulk insert performance and data integrity
        assertThat(savedTransactions).hasSize(BULK_INSERT_SIZE);
        assertThat(endTime - startTime).isLessThan(5000); // 5 second max for 1000 records
        assertThat(dailyTransactionRepository.count()).isEqualTo(BULK_INSERT_SIZE);
        
        // Verify all records have valid IDs assigned
        savedTransactions.forEach(transaction -> {
            assertThat(transaction.getTransactionId()).isNotNull();
            assertThat(transaction.getAmount()).isNotNull();
            assertThat(transaction.getTransactionType()).isNotNull();
        });
    }

    /**
     * Test saveAll() method with transaction batch processing.
     * Validates ACID compliance and proper transaction boundary handling.
     */
    @Test
    @DisplayName("Save All Method - Transaction Batch Processing")
    public void testSaveAllMethod_TransactionBatchProcessing() {
        // Given: Create transaction batch with mixed processing status
        List<DailyTransaction> batch = testDataGenerator.generateDailyTransactionBatch(100);
        batch.forEach(tx -> tx.setProcessingStatus("PENDING"));
        
        // When: Save batch and verify transaction boundaries
        List<DailyTransaction> saved = dailyTransactionRepository.saveAll(batch);
        testEntityManager.flush();
        
        // Then: Verify all transactions saved with consistent state
        assertThat(saved).hasSize(100);
        saved.forEach(tx -> {
            assertThat(tx.getProcessingStatus()).isEqualTo("PENDING");
            assertThat(tx.getTransactionId()).isNotNull();
        });
        
        // Verify database consistency
        long count = dailyTransactionRepository.count();
        assertThat(count).isEqualTo(100);
    }

    // ========================================
    // DAILY AGGREGATION QUERIES TESTING
    // ========================================

    /**
     * Test daily aggregation queries (sum, count, avg).
     * Validates aggregation calculations match COBOL COMP-3 precision requirements.
     */
    @Test
    @DisplayName("Daily Aggregation Queries - Sum, Count, Average Validation")
    public void testDailyAggregationQueries_SumCountAverage() {
        // Given: Create transactions for specific date with known amounts
        LocalDate testDate = LocalDate.now().minusDays(1);
        List<DailyTransaction> transactions = createTestTransactionsForDate(testDate, 50);
        dailyTransactionRepository.saveAll(transactions);
        testEntityManager.flush();
        
        // Calculate expected aggregations
        BigDecimal expectedSum = transactions.stream()
                .map(DailyTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int expectedCount = transactions.size();
        BigDecimal expectedAvg = expectedSum.divide(BigDecimal.valueOf(expectedCount), 2, BigDecimal.ROUND_HALF_UP);
        
        // When: Execute aggregation queries
        List<DailyTransaction> dateTransactions = dailyTransactionRepository
                .findByTransactionDateBetween(testDate, testDate);
        
        BigDecimal actualSum = dateTransactions.stream()
                .map(DailyTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Then: Validate aggregation results with COBOL precision
        assertThat(dateTransactions).hasSize(expectedCount);
        assertBigDecimalEquals(actualSum, expectedSum, PRECISION_TOLERANCE);
        
        // Verify average calculation precision
        BigDecimal actualAvg = actualSum.divide(BigDecimal.valueOf(expectedCount), 2, BigDecimal.ROUND_HALF_UP);
        assertBigDecimalEquals(actualAvg, expectedAvg, PRECISION_TOLERANCE);
    }

    /**
     * Test findByTransactionDate for daily report generation.
     * Validates date-based filtering and reporting functionality.
     */
    @Test
    @DisplayName("Find By Transaction Date - Daily Report Generation")
    public void testFindByTransactionDate_DailyReportGeneration() {
        // Given: Create transactions across multiple dates
        LocalDate reportDate = LocalDate.now().minusDays(2);
        LocalDate excludeDate = LocalDate.now().minusDays(5);
        
        List<DailyTransaction> reportTransactions = createTestTransactionsForDate(reportDate, 25);
        List<DailyTransaction> excludedTransactions = createTestTransactionsForDate(excludeDate, 15);
        
        dailyTransactionRepository.saveAll(reportTransactions);
        dailyTransactionRepository.saveAll(excludedTransactions);
        testEntityManager.flush();
        
        // When: Query transactions for specific report date
        List<DailyTransaction> result = dailyTransactionRepository
                .findByTransactionDateBetween(reportDate, reportDate);
        
        // Then: Verify only report date transactions returned
        assertThat(result).hasSize(25);
        result.forEach(tx -> {
            assertThat(tx.getTransactionDate()).isEqualTo(reportDate);
            assertThat(tx.getAmount()).isNotNull();
            assertThat(tx.getTransactionType()).isNotNull();
        });
        
        // Verify excluded date transactions not included
        List<DailyTransaction> allTransactions = dailyTransactionRepository.findAll();
        assertThat(allTransactions).hasSize(40); // 25 + 15 total
    }

    // ========================================
    // PARTITION-AWARE QUERIES TESTING
    // ========================================

    /**
     * Test partition-aware queries for performance optimization.
     * Validates query performance across date partitions.
     */
    @Test
    @DisplayName("Partition-Aware Queries - Performance Optimization")
    public void testPartitionAwareQueries_PerformanceOptimization() {
        // Given: Create transactions across multiple months (simulating partitions)
        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate previousMonth = currentMonth.minusMonths(1);
        LocalDate futureMonth = currentMonth.plusMonths(1);
        
        List<DailyTransaction> currentMonthTx = createTestTransactionsForDate(currentMonth, 100);
        List<DailyTransaction> previousMonthTx = createTestTransactionsForDate(previousMonth, 100);
        List<DailyTransaction> futureMonthTx = createTestTransactionsForDate(futureMonth, 100);
        
        dailyTransactionRepository.saveAll(currentMonthTx);
        dailyTransactionRepository.saveAll(previousMonthTx);
        dailyTransactionRepository.saveAll(futureMonthTx);
        testEntityManager.flush();
        
        // When: Execute partition-aware query for current month
        long startTime = System.nanoTime();
        List<DailyTransaction> currentMonthResults = dailyTransactionRepository
                .findByTransactionDateBetween(currentMonth, currentMonth.plusMonths(1).minusDays(1));
        long endTime = System.nanoTime();
        
        // Then: Validate query performance and result accuracy
        long queryTimeMs = (endTime - startTime) / 1_000_000;
        assertThat(queryTimeMs).isLessThan(100); // Sub-100ms for partition query
        assertThat(currentMonthResults).hasSize(100);
        
        // Verify partition isolation
        currentMonthResults.forEach(tx -> {
            assertThat(tx.getTransactionDate()).isBetween(
                currentMonth, 
                currentMonth.plusMonths(1).minusDays(1)
            );
        });
    }

    // ========================================
    // DATA ARCHIVAL TESTING
    // ========================================

    /**
     * Test data archival after processing window.
     * Validates data retention and archival functionality.
     */
    @Test
    @DisplayName("Data Archival - Processing Window Management")
    public void testDataArchival_ProcessingWindowManagement() {
        // Given: Create transactions with different processing dates
        LocalDate recentDate = LocalDate.now().minusDays(1);
        LocalDate archivalDate = LocalDate.now().minusDays(90); // 90 days retention
        
        List<DailyTransaction> recentTransactions = createTestTransactionsForDate(recentDate, 20);
        List<DailyTransaction> archivalTransactions = createTestTransactionsForDate(archivalDate, 30);
        
        // Mark archival transactions as completed
        archivalTransactions.forEach(tx -> {
            tx.setProcessingStatus("COMPLETED");
            tx.setProcessedTimestamp(LocalDateTime.now().minusDays(90));
        });
        
        dailyTransactionRepository.saveAll(recentTransactions);
        dailyTransactionRepository.saveAll(archivalTransactions);
        testEntityManager.flush();
        
        // When: Query for archival candidates
        LocalDate archivalCutoff = LocalDate.now().minusDays(30);
        List<DailyTransaction> archivalCandidates = dailyTransactionRepository
                .findByTransactionDateBetween(LocalDate.MIN, archivalCutoff);
        
        // Then: Validate archival candidate identification
        assertThat(archivalCandidates).hasSize(30);
        archivalCandidates.forEach(tx -> {
            assertThat(tx.getTransactionDate()).isBefore(archivalCutoff);
            assertThat(tx.getProcessingStatus()).isEqualTo("COMPLETED");
        });
        
        // Verify recent data not included in archival
        List<DailyTransaction> allTransactions = dailyTransactionRepository.findAll();
        assertThat(allTransactions).hasSize(50); // 20 + 30 total
    }

    // ========================================
    // TRANSACTION CATEGORIZATION TESTING
    // ========================================

    /**
     * Test transaction categorization for daily summaries.
     * Validates categorization logic and summary calculations.
     */
    @Test
    @DisplayName("Transaction Categorization - Daily Summary Generation")
    public void testTransactionCategorization_DailySummaryGeneration() {
        // Given: Create transactions with different categories
        LocalDate summaryDate = LocalDate.now().minusDays(1);
        List<DailyTransaction> purchases = createCategorizedTransactions(summaryDate, "PURCHASE", 15);
        List<DailyTransaction> payments = createCategorizedTransactions(summaryDate, "PAYMENT", 10);
        List<DailyTransaction> refunds = createCategorizedTransactions(summaryDate, "REFUND", 5);
        
        dailyTransactionRepository.saveAll(purchases);
        dailyTransactionRepository.saveAll(payments);
        dailyTransactionRepository.saveAll(refunds);
        testEntityManager.flush();
        
        // When: Query transactions by category for summary
        List<DailyTransaction> allDayTransactions = dailyTransactionRepository
                .findByTransactionDateBetween(summaryDate, summaryDate);
        
        // Then: Validate categorization and counts
        assertThat(allDayTransactions).hasSize(30); // 15 + 10 + 5
        
        // Group by transaction type and validate counts
        long purchaseCount = allDayTransactions.stream()
                .filter(tx -> "PURCHASE".equals(tx.getTransactionType()))
                .count();
        long paymentCount = allDayTransactions.stream()
                .filter(tx -> "PAYMENT".equals(tx.getTransactionType()))
                .count();
        long refundCount = allDayTransactions.stream()
                .filter(tx -> "REFUND".equals(tx.getTransactionType()))
                .count();
        
        assertThat(purchaseCount).isEqualTo(15);
        assertThat(paymentCount).isEqualTo(10);
        assertThat(refundCount).isEqualTo(5);
    }

    /**
     * Test merchant totals aggregation.
     * Validates merchant-based reporting and aggregation functionality.
     */
    @Test
    @DisplayName("Merchant Totals Aggregation - Merchant Analysis")
    public void testMerchantTotalsAggregation_MerchantAnalysis() {
        // Given: Create transactions for different merchants
        LocalDate analysisDate = LocalDate.now().minusDays(1);
        String merchant1 = testDataGenerator.generateMerchantId();
        String merchant2 = testDataGenerator.generateMerchantId();
        String merchant3 = testDataGenerator.generateMerchantId();
        
        List<DailyTransaction> merchant1Tx = createMerchantTransactions(analysisDate, merchant1, 10);
        List<DailyTransaction> merchant2Tx = createMerchantTransactions(analysisDate, merchant2, 15);
        List<DailyTransaction> merchant3Tx = createMerchantTransactions(analysisDate, merchant3, 5);
        
        dailyTransactionRepository.saveAll(merchant1Tx);
        dailyTransactionRepository.saveAll(merchant2Tx);
        dailyTransactionRepository.saveAll(merchant3Tx);
        testEntityManager.flush();
        
        // When: Query transactions for merchant analysis
        List<DailyTransaction> allTransactions = dailyTransactionRepository
                .findByTransactionDateBetween(analysisDate, analysisDate);
        
        // Then: Validate merchant transaction distribution
        assertThat(allTransactions).hasSize(30); // 10 + 15 + 5
        
        // Group by merchant and validate counts
        long merchant1Count = allTransactions.stream()
                .filter(tx -> merchant1.equals(tx.getMerchantId()))
                .count();
        long merchant2Count = allTransactions.stream()
                .filter(tx -> merchant2.equals(tx.getMerchantId()))
                .count();
        long merchant3Count = allTransactions.stream()
                .filter(tx -> merchant3.equals(tx.getMerchantId()))
                .count();
        
        assertThat(merchant1Count).isEqualTo(10);
        assertThat(merchant2Count).isEqualTo(15);
        assertThat(merchant3Count).isEqualTo(5);
    }

    // ========================================
    // CHECKPOINT AND RESTART TESTING
    // ========================================

    /**
     * Test checkpoint and restart capabilities.
     * Validates batch job restart functionality and data consistency.
     */
    @Test
    @DisplayName("Checkpoint and Restart Capabilities - Batch Job Recovery")
    public void testCheckpointAndRestartCapabilities_BatchJobRecovery() {
        // Given: Create partial batch processing scenario
        List<DailyTransaction> batch1 = testDataGenerator.generateDailyTransactionBatch(50);
        List<DailyTransaction> batch2 = testDataGenerator.generateDailyTransactionBatch(50);
        
        // Mark batch1 as processed, batch2 as pending (simulating restart scenario)
        batch1.forEach(tx -> tx.setProcessingStatus("COMPLETED"));
        batch2.forEach(tx -> tx.setProcessingStatus("PENDING"));
        
        // When: Save both batches and simulate restart
        dailyTransactionRepository.saveAll(batch1);
        dailyTransactionRepository.saveAll(batch2);
        testEntityManager.flush();
        
        // Query for restart candidates (pending transactions)
        List<DailyTransaction> pendingTransactions = dailyTransactionRepository
                .findByProcessingStatus("PENDING");
        
        // Then: Validate restart scenario handling
        assertThat(pendingTransactions).hasSize(50);
        pendingTransactions.forEach(tx -> {
            assertThat(tx.getProcessingStatus()).isEqualTo("PENDING");
            assertThat(tx.getTransactionId()).isNotNull();
        });
        
        // Verify completed transactions remain unchanged
        List<DailyTransaction> completedTransactions = dailyTransactionRepository
                .findByProcessingStatus("COMPLETED");
        assertThat(completedTransactions).hasSize(50);
        
        // Total count validation
        assertThat(dailyTransactionRepository.count()).isEqualTo(100);
    }

    // ========================================
    // CONCURRENT EXECUTION TESTING
    // ========================================

    /**
     * Test concurrent batch job execution.
     * Validates thread safety and concurrent data access scenarios.
     */
    @Test
    @DisplayName("Concurrent Batch Job Execution - Thread Safety Validation")
    public void testConcurrentBatchJobExecution_ThreadSafetyValidation() throws InterruptedException {
        // Given: Create concurrent execution scenario
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // When: Execute concurrent batch insertions
        for (int i = 0; i < 4; i++) {
            final int batchId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                List<DailyTransaction> batch = testDataGenerator.generateDailyTransactionBatch(25);
                batch.forEach(tx -> tx.setCategoryCode("BATCH_" + batchId));
                dailyTransactionRepository.saveAll(batch);
            }, executor);
            futures.add(future);
        }
        
        // Wait for all concurrent operations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        testEntityManager.flush();
        
        // Then: Validate concurrent execution results
        long totalCount = dailyTransactionRepository.count();
        assertThat(totalCount).isEqualTo(100); // 4 batches × 25 records
        
        // Verify data integrity across concurrent batches
        for (int i = 0; i < 4; i++) {
            List<DailyTransaction> batchTransactions = dailyTransactionRepository
                    .findByCategoryCode("BATCH_" + i);
            assertThat(batchTransactions).hasSize(25);
        }
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    // ========================================
    // PERFORMANCE TESTING
    // ========================================

    /**
     * Test memory-optimized streaming queries.
     * Validates memory efficiency for large dataset processing.
     */
    @Test
    @DisplayName("Memory-Optimized Streaming Queries - Large Dataset Processing")
    public void testMemoryOptimizedStreamingQueries_LargeDatasetProcessing() {
        // Given: Create large dataset for streaming test
        List<DailyTransaction> largeDataset = testDataGenerator.generateDailyTransactionBatch(LARGE_DATASET_SIZE);
        
        // When: Process large dataset with memory constraints
        long startTime = System.currentTimeMillis();
        List<DailyTransaction> saved = dailyTransactionRepository.saveAll(largeDataset);
        testEntityManager.flush();
        long endTime = System.currentTimeMillis();
        
        // Then: Validate performance within acceptable limits
        long processingTime = endTime - startTime;
        assertThat(processingTime).isLessThan(30000); // 30 second max for 10k records
        assertThat(saved).hasSize(LARGE_DATASET_SIZE);
        
        // Verify streaming query performance
        long queryStartTime = System.currentTimeMillis();
        long count = dailyTransactionRepository.count();
        long queryEndTime = System.currentTimeMillis();
        
        assertThat(count).isEqualTo(LARGE_DATASET_SIZE);
        assertThat(queryEndTime - queryStartTime).isLessThan(1000); // 1 second max for count query
    }

    /**
     * Test batch performance within 4-hour window.
     * Validates processing performance meets batch window requirements.
     */
    @Test
    @DisplayName("Batch Performance Within 4-Hour Window - Processing Window Validation")
    public void testBatchPerformanceWithin4HourWindow_ProcessingWindowValidation() {
        // Given: Simulate realistic daily batch volume
        int dailyTransactionVolume = 50000; // Realistic daily volume
        List<DailyTransaction> dailyBatch = testDataGenerator.generateDailyTransactionBatch(dailyTransactionVolume);
        
        // When: Process full daily batch
        long startTime = System.currentTimeMillis();
        
        // Simulate batch processing in chunks (typical Spring Batch pattern)
        int chunkSize = 1000;
        for (int i = 0; i < dailyBatch.size(); i += chunkSize) {
            int endIndex = Math.min(i + chunkSize, dailyBatch.size());
            List<DailyTransaction> chunk = dailyBatch.subList(i, endIndex);
            dailyTransactionRepository.saveAll(chunk);
            
            if (i % (chunkSize * 10) == 0) { // Periodic flush
                testEntityManager.flush();
                testEntityManager.clear();
            }
        }
        
        testEntityManager.flush();
        long endTime = System.currentTimeMillis();
        
        // Then: Validate processing time within 4-hour window
        long processingTimeMs = endTime - startTime;
        long processingTimeHours = processingTimeMs / (1000 * 60 * 60);
        
        assertThat(processingTimeHours).isLessThan(4); // Must complete within 4 hours
        assertThat(dailyTransactionRepository.count()).isEqualTo(dailyTransactionVolume);
        
        logger.info("Processed {} transactions in {} ms ({} hours)", 
                    dailyTransactionVolume, processingTimeMs, 
                    processingTimeMs / (1000.0 * 60.0 * 60.0));
    }

    // ========================================
    // ROLLBACK AND FAILURE TESTING
    // ========================================

    /**
     * Test rollback on batch failures.
     * Validates transaction rollback and error handling capabilities.
     */
    @Test
    @DisplayName("Rollback on Batch Failures - Error Handling Validation")
    public void testRollbackOnBatchFailures_ErrorHandlingValidation() {
        // Given: Create batch with valid and invalid data
        List<DailyTransaction> validBatch = testDataGenerator.generateDailyTransactionBatch(25);
        
        // Insert valid data first
        dailyTransactionRepository.saveAll(validBatch);
        testEntityManager.flush();
        long initialCount = dailyTransactionRepository.count();
        assertThat(initialCount).isEqualTo(25);
        
        // When: Attempt to save invalid data that should cause constraint violation
        try {
            DailyTransaction invalidTransaction = testDataGenerator.generateDailyTransaction();
            // Create constraint violation scenario (e.g., null required field)
            invalidTransaction.setTransactionId(null); // This should cause violation
            dailyTransactionRepository.save(invalidTransaction);
            testEntityManager.flush();
            
            fail("Expected constraint violation exception");
        } catch (Exception e) {
            // Then: Verify rollback behavior
            testEntityManager.clear(); // Clear persistence context
            long countAfterFailure = dailyTransactionRepository.count();
            assertThat(countAfterFailure).isEqualTo(initialCount); // Should remain unchanged
        }
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Create test transactions for a specific date.
     */
    private List<DailyTransaction> createTestTransactionsForDate(LocalDate date, int count) {
        List<DailyTransaction> transactions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            DailyTransaction tx = testDataGenerator.generateDailyTransaction();
            tx.setTransactionDate(date);
            tx.setOriginalTimestamp(date.atStartOfDay());
            transactions.add(tx);
        }
        return transactions;
    }

    /**
     * Create categorized transactions for testing.
     */
    private List<DailyTransaction> createCategorizedTransactions(LocalDate date, String category, int count) {
        List<DailyTransaction> transactions = createTestTransactionsForDate(date, count);
        transactions.forEach(tx -> {
            tx.setTransactionType(category);
            tx.setCategoryCode(category);
        });
        return transactions;
    }

    /**
     * Create merchant-specific transactions for testing.
     */
    private List<DailyTransaction> createMerchantTransactions(LocalDate date, String merchantId, int count) {
        List<DailyTransaction> transactions = createTestTransactionsForDate(date, count);
        transactions.forEach(tx -> tx.setMerchantId(merchantId));
        return transactions;
    }

    /**
     * Assert BigDecimal values are equal within tolerance.
     */
    private void assertBigDecimalEquals(BigDecimal actual, BigDecimal expected, BigDecimal tolerance) {
        assertThat(actual).isCloseTo(expected, within(tolerance));
    }
}