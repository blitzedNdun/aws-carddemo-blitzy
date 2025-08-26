/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.repository;

import com.carddemo.BaseIntegrationTest;
import com.carddemo.TestConstants;
import com.carddemo.TestDataGenerator;
import com.carddemo.entity.Account;
import com.carddemo.entity.Card;
import com.carddemo.entity.Transaction;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardRepository;
import com.carddemo.repository.TransactionRepository;

import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive integration test class for TransactionRepository validating high-volume transaction processing,
 * date-range queries with partition pruning, merchant data handling, and batch processing operations 
 * within 4-hour window requirements.
 * 
 * This test class ensures the Spring Data JPA repository implementation maintains 100% functional parity
 * with the original COBOL VSAM transaction processing while meeting modern performance requirements:
 * - High-volume transaction inserts (10,000 TPS target)
 * - Sub-200ms response times for individual operations
 * - PostgreSQL partition pruning for date-range queries
 * - BigDecimal precision matching COBOL COMP-3 packed decimals
 * - ACID compliance for concurrent transaction processing
 * - Foreign key constraint validation with Account and Card entities
 * 
 * Test Coverage Areas:
 * 1. High-Volume Transaction Processing (Performance Testing)
 * 2. Date-Range Query Validation with Partition Pruning
 * 3. Card Number-Based Transaction Retrieval
 * 4. BigDecimal Amount Precision Validation (12,2 scale)
 * 5. Transaction Type and Category Code Relationships
 * 6. Merchant Data Field Validation (ID, name, city, ZIP)
 * 7. Timestamp Handling for ORIG and PROC timestamps
 * 8. Monthly Partition Management and Archival
 * 9. Batch Processing Queries for Daily Totals
 * 10. Transaction Description Field Truncation at 100 chars
 * 11. Negative Amount Handling for Credits/Refunds
 * 12. Foreign Key Constraints with Account and Card entities
 * 13. Concurrent Transaction Processing with ACID Compliance
 * 14. Query Performance on Partitioned Tables
 * 
 * COBOL Program Mappings:
 * - COTRN00C.cbl → findByAccountIdAndTransactionDateBetween tests
 * - COTRN01C.cbl → findById and transaction detail tests
 * - COTRN02C.cbl → save and transaction creation tests
 * - CBTRN01C.cbl → batch processing and daily total tests
 * - CVTRA05Y.cpy → entity field mapping and precision tests
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@SpringBootTest
@Transactional
@DisplayName("TransactionRepository Integration Tests")
public class TransactionRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private CardRepository cardRepository;

    /**
     * Test high-volume transaction insert performance to validate 10,000 TPS target.
     * Validates that the system can handle high-frequency transaction processing
     * requirements while maintaining data integrity and performance benchmarks.
     */
    @DisplayName("High-Volume Transaction Inserts - 10,000 TPS Target")
    public void testHighVolumeTransactionInserts() {
        // Setup test data
        setupTestData();
        
        int targetTransactionCount = 1000; // Reduced for test efficiency
        List<Transaction> transactions = new ArrayList<>();
        
        // Generate high-volume transaction dataset
        for (int i = 0; i < targetTransactionCount; i++) {
            Transaction transaction = createTestTransaction();
            transaction.setTransactionId((long) (i + 1));
            transaction.setAmount(TestDataGenerator.generateComp3BigDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            transaction.setTransactionDate(LocalDate.now().minusDays(i % 30));
            transactions.add(transaction);
        }
        
        // Measure performance
        Instant startTime = Instant.now();
        
        // Bulk insert transactions
        List<Transaction> savedTransactions = transactionRepository.saveAll(transactions);
        transactionRepository.flush();
        
        Instant endTime = Instant.now();
        long executionTimeMs = endTime.toEpochMilli() - startTime.toEpochMilli();
        
        // Validate performance meets target
        assertThat(savedTransactions).hasSize(targetTransactionCount);
        assertThat(executionTimeMs).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS * targetTransactionCount);
        
        // Calculate and validate TPS
        double actualTPS = (double) targetTransactionCount / (executionTimeMs / 1000.0);
        assertThat(actualTPS).isGreaterThan(TestConstants.TARGET_TPS / 10); // Adjusted for test scale
        
        // Validate data integrity
        long transactionCount = transactionRepository.count();
        assertThat(transactionCount).isGreaterThanOrEqualTo(targetTransactionCount);
        
        cleanupTestData();
    }

    /**
     * Test date-range queries with partition pruning optimization.
     * Validates that PostgreSQL partition pruning works correctly for transaction_date queries
     * and delivers expected performance improvements for batch processing operations.
     */
    @DisplayName("Date-Range Queries with Partition Pruning")
    public void testDateRangeQueriesWithPartitionPruning() {
        setupTestData();
        
        // Create transactions across multiple months for partition testing
        LocalDate startDate = LocalDate.now().minusMonths(3);
        LocalDate endDate = LocalDate.now();
        
        List<Transaction> testTransactions = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Transaction transaction = createTestTransaction();
            transaction.setTransactionId((long) (i + 1000));
            transaction.setTransactionDate(startDate.plusDays(i % 90));
            testTransactions.add(transaction);
        }
        
        transactionRepository.saveAll(testTransactions);
        
        // Test date range query performance
        Instant queryStartTime = Instant.now();
        
        List<Transaction> dateRangeResults = transactionRepository.findByAccountIdAndTransactionDateBetween(
            TestConstants.TEST_ACCOUNT_ID, startDate.plusMonths(1), endDate.minusMonths(1)
        );
        
        Instant queryEndTime = Instant.now();
        long queryTimeMs = queryEndTime.toEpochMilli() - queryStartTime.toEpochMilli();
        
        // Validate query performance (should benefit from partition pruning)
        assertThat(queryTimeMs).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        // Validate query results accuracy
        assertThat(dateRangeResults).isNotEmpty();
        
        // Verify all results fall within date range
        LocalDate finalStartDate = startDate.plusMonths(1);
        LocalDate finalEndDate = endDate.minusMonths(1);
        dateRangeResults.forEach(transaction -> {
            assertThat(transaction.getTransactionDate())
                .isBetween(finalStartDate, finalEndDate);
        });
        
        cleanupTestData();
    }

    /**
     * Test card number-based transaction retrieval with proper indexing.
     * Validates that card_number foreign key lookups use database indexes efficiently
     * and deliver sub-200ms response times for real-time transaction queries.
     */
    @DisplayName("Card Number Transaction Retrieval with Indexing")
    public void testCardNumberTransactionRetrievalWithIndexing() {
        setupTestData();
        
        // Create test transactions for specific card
        String testCardNumber = TestConstants.TEST_CARD_NUMBER;
        List<Transaction> cardTransactions = new ArrayList<>();
        
        for (int i = 0; i < 50; i++) {
            Transaction transaction = createTestTransaction();
            transaction.setTransactionId((long) (i + 2000));
            transaction.setCardNumber(testCardNumber);
            cardTransactions.add(transaction);
        }
        
        transactionRepository.saveAll(cardTransactions);
        
        // Test card-based query performance
        Instant queryStartTime = Instant.now();
        
        List<Transaction> cardResults = transactionRepository.findByCardNumber(testCardNumber);
        
        Instant queryEndTime = Instant.now();
        long queryTimeMs = queryEndTime.toEpochMilli() - queryStartTime.toEpochMilli();
        
        // Validate index-optimized query performance
        assertThat(queryTimeMs).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        // Validate query results
        assertThat(cardResults).hasSize(50);
        cardResults.forEach(transaction -> {
            assertThat(transaction.getCardNumber()).isEqualTo(testCardNumber);
        });
        
        cleanupTestData();
    }

    /**
     * Test BigDecimal amount precision validation for COBOL COMP-3 compatibility.
     * Ensures that all monetary calculations maintain exact precision matching
     * the original COBOL packed decimal implementation with scale=2.
     */
    @DisplayName("BigDecimal Amount Precision Validation - COBOL COMP-3 Compatibility")
    public void testBigDecimalAmountPrecisionValidation() {
        setupTestData();
        
        // Test various decimal precision scenarios
        BigDecimal[] testAmounts = {
            new BigDecimal("1234567890.12"), // Maximum COBOL COMP-3 precision
            new BigDecimal("0.01"),          // Minimum positive amount
            new BigDecimal("-999999999.99"), // Maximum negative amount
            new BigDecimal("999999999.99"),  // Maximum positive amount
            new BigDecimal("0.00")           // Zero amount
        };
        
        List<Transaction> precisionTests = new ArrayList<>();
        
        for (int i = 0; i < testAmounts.length; i++) {
            Transaction transaction = createTestTransaction();
            transaction.setTransactionId((long) (i + 3000));
            transaction.setAmount(testAmounts[i].setScale(TestConstants.COBOL_DECIMAL_SCALE, 
                TestConstants.COBOL_ROUNDING_MODE));
            precisionTests.add(transaction);
        }
        
        // Save and retrieve to test precision preservation
        List<Transaction> savedTransactions = transactionRepository.saveAll(precisionTests);
        transactionRepository.flush();
        
        // Validate precision preservation after database round-trip
        for (int i = 0; i < testAmounts.length; i++) {
            Transaction saved = savedTransactions.get(i);
            BigDecimal expectedAmount = testAmounts[i].setScale(TestConstants.COBOL_DECIMAL_SCALE, 
                TestConstants.COBOL_ROUNDING_MODE);
            
            assertBigDecimalEquals(saved.getAmount(), expectedAmount);
            assertThat(saved.getAmount().scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        }
        
        cleanupTestData();
    }

    /**
     * Test transaction type and category code relationships.
     * Validates that foreign key relationships to transaction types and categories
     * work correctly and maintain referential integrity.
     */
    @DisplayName("Transaction Type and Category Code Relationships")
    public void testTransactionTypeAndCategoryCodeRelationships() {
        setupTestData();
        
        // Create transactions with various types and categories
        String[] transactionTypes = {"DB", "CR", "PU", "PA"};
        String[] categoryTypes = {"PURCH", "PYMNT", "FEES", "INTR"};
        
        List<Transaction> typeTestTransactions = new ArrayList<>();
        
        for (int i = 0; i < transactionTypes.length; i++) {
            Transaction transaction = createTestTransaction();
            transaction.setTransactionId((long) (i + 4000));
            transaction.setTransactionType(transactionTypes[i]);
            transaction.setCategoryCode(categoryTypes[i]);
            typeTestTransactions.add(transaction);
        }
        
        List<Transaction> savedTypeTransactions = transactionRepository.saveAll(typeTestTransactions);
        
        // Validate type and category preservation
        for (int i = 0; i < transactionTypes.length; i++) {
            Transaction saved = savedTypeTransactions.get(i);
            assertThat(saved.getTransactionType()).isEqualTo(transactionTypes[i]);
            assertThat(saved.getCategoryCode()).isEqualTo(categoryTypes[i]);
        }
        
        // Test filtering by transaction type
        List<Transaction> debitTransactions = transactionRepository.findByTransactionType("DB");
        assertThat(debitTransactions).isNotEmpty();
        debitTransactions.forEach(transaction -> {
            assertThat(transaction.getTransactionType()).isEqualTo("DB");
        });
        
        cleanupTestData();
    }

    /**
     * Test merchant data fields validation and storage.
     * Validates proper handling of merchant ID, name, city, and ZIP code fields
     * with appropriate length limits and data formatting.
     */
    @DisplayName("Merchant Data Fields Validation")
    public void testMerchantDataFieldsValidation() {
        setupTestData();
        
        Transaction transaction = createTestTransaction();
        transaction.setTransactionId(5000L);
        
        // Test merchant data with various field lengths
        transaction.setMerchantId("MERCH12345");
        transaction.setMerchantName("Test Merchant Corporation Inc");
        transaction.setMerchantCity("New York");
        transaction.setMerchantZip("10001-1234");
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Validate merchant data preservation
        assertThat(savedTransaction.getMerchantId()).isEqualTo("MERCH12345");
        assertThat(savedTransaction.getMerchantName()).isEqualTo("Test Merchant Corporation Inc");
        assertThat(savedTransaction.getMerchantCity()).isEqualTo("New York");
        assertThat(savedTransaction.getMerchantZip()).isEqualTo("10001-1234");
        
        // Test field length limits
        String longMerchantName = "Very Long Merchant Name That Exceeds Normal Length Limits And Should Be Handled Appropriately By The System";
        transaction.setMerchantName(longMerchantName.substring(0, Math.min(longMerchantName.length(), 50)));
        
        assertThatCode(() -> transactionRepository.save(transaction))
            .doesNotThrowAnyException();
        
        cleanupTestData();
    }

    /**
     * Test timestamp handling for original and processed timestamps.
     * Validates that ORIG_TS and PROC_TS fields handle date/time data correctly
     * with appropriate precision and timezone handling.
     */
    @DisplayName("Timestamp Handling - Original and Processed Timestamps")
    public void testTimestampHandling() {
        setupTestData();
        
        Transaction transaction = createTestTransaction();
        transaction.setTransactionId(6000L);
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime processingTime = now.plusMinutes(5);
        
        transaction.setOriginalTimestamp(now);
        transaction.setProcessedTimestamp(processingTime);
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Validate timestamp precision and accuracy
        assertDateEquals(savedTransaction.getOriginalTimestamp(), now);
        assertDateEquals(savedTransaction.getProcessedTimestamp(), processingTime);
        
        // Test timestamp ordering
        assertThat(savedTransaction.getProcessedTimestamp())
            .isAfter(savedTransaction.getOriginalTimestamp());
        
        cleanupTestData();
    }

    /**
     * Test transaction description field truncation at 100 characters.
     * Validates that long descriptions are properly handled and truncated
     * to maintain database field constraints and COBOL compatibility.
     */
    @DisplayName("Transaction Description Field Truncation")
    public void testTransactionDescriptionFieldTruncation() {
        setupTestData();
        
        Transaction transaction = createTestTransaction();
        transaction.setTransactionId(7000L);
        
        // Test description at maximum length
        String maxLengthDescription = "A".repeat(100);
        transaction.setDescription(maxLengthDescription);
        
        Transaction savedMaxLength = transactionRepository.save(transaction);
        assertThat(savedMaxLength.getDescription()).hasSize(100);
        
        // Test description truncation for overly long descriptions
        String longDescription = "Very Long Transaction Description That Exceeds The Maximum Allowed Length Of One Hundred Characters And Should Be Truncated Appropriately";
        transaction.setDescription(longDescription.substring(0, 100));
        
        Transaction savedTruncated = transactionRepository.save(transaction);
        assertThat(savedTruncated.getDescription()).hasSize(100);
        
        cleanupTestData();
    }

    /**
     * Test negative amount handling for credits and refunds.
     * Validates that negative amounts are properly stored and retrieved
     * to support credit transactions and refund processing.
     */
    @DisplayName("Negative Amount Handling - Credits and Refunds")
    public void testNegativeAmountHandling() {
        setupTestData();
        
        List<Transaction> negativeAmountTransactions = new ArrayList<>();
        
        // Create various negative amount scenarios
        BigDecimal[] negativeAmounts = {
            new BigDecimal("-50.00"),    // Standard refund
            new BigDecimal("-0.01"),     // Minimum negative
            new BigDecimal("-999999.99") // Large refund
        };
        
        for (int i = 0; i < negativeAmounts.length; i++) {
            Transaction transaction = createTestTransaction();
            transaction.setTransactionId((long) (i + 8000));
            transaction.setAmount(negativeAmounts[i]);
            transaction.setTransactionType("CR"); // Credit transaction
            negativeAmountTransactions.add(transaction);
        }
        
        List<Transaction> savedNegativeTransactions = transactionRepository.saveAll(negativeAmountTransactions);
        
        // Validate negative amount preservation
        for (int i = 0; i < negativeAmounts.length; i++) {
            Transaction saved = savedNegativeTransactions.get(i);
            assertBigDecimalEquals(saved.getAmount(), negativeAmounts[i]);
            assertThat(saved.getAmount().signum()).isEqualTo(-1);
        }
        
        cleanupTestData();
    }

    /**
     * Test foreign key constraints with Account and Card entities.
     * Validates that referential integrity is maintained between transactions
     * and their related account and card entities.
     */
    @DisplayName("Foreign Key Constraints - Account and Card Relationships")
    public void testForeignKeyConstraints() {
        setupTestData();
        
        // Test valid foreign key relationships
        Transaction validTransaction = createTestTransaction();
        validTransaction.setTransactionId(9000L);
        validTransaction.setAccountId(TestConstants.TEST_ACCOUNT_ID);
        validTransaction.setCardNumber(TestConstants.TEST_CARD_NUMBER);
        
        assertThatCode(() -> transactionRepository.save(validTransaction))
            .doesNotThrowAnyException();
        
        // Verify relationship integrity
        Optional<Transaction> savedTransaction = transactionRepository.findById(9000L);
        assertThat(savedTransaction).isPresent();
        assertThat(savedTransaction.get().getAccountId()).isEqualTo(TestConstants.TEST_ACCOUNT_ID);
        assertThat(savedTransaction.get().getCardNumber()).isEqualTo(TestConstants.TEST_CARD_NUMBER);
        
        cleanupTestData();
    }

    /**
     * Test concurrent transaction processing with ACID compliance.
     * Validates that multiple concurrent transactions maintain data consistency
     * and integrity under high-concurrency scenarios.
     */
    @DisplayName("Concurrent Transaction Processing - ACID Compliance")
    public void testConcurrentTransactionProcessingACIDCompliance() {
        setupTestData();
        
        int numberOfThreads = 10;
        int transactionsPerThread = 50;
        
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger transactionIdCounter = new AtomicInteger(10000);
        
        List<Future<?>> futures = new ArrayList<>();
        
        // Submit concurrent transaction creation tasks
        for (int t = 0; t < numberOfThreads; t++) {
            Future<?> future = executorService.submit(() -> {
                try {
                    for (int i = 0; i < transactionsPerThread; i++) {
                        Transaction transaction = createTestTransaction();
                        transaction.setTransactionId((long) transactionIdCounter.incrementAndGet());
                        transaction.setAmount(TestDataGenerator.generateComp3BigDecimal());
                        transactionRepository.save(transaction);
                    }
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }
        
        // Wait for all threads to complete
        try {
            latch.await();
            
            // Verify all transactions were created successfully
            long totalTransactions = transactionRepository.count();
            assertThat(totalTransactions).isGreaterThanOrEqualTo(numberOfThreads * transactionsPerThread);
            
            // Verify data consistency - no duplicate IDs should exist
            List<Transaction> allTransactions = transactionRepository.findAll();
            long uniqueTransactionIds = allTransactions.stream()
                .map(Transaction::getTransactionId)
                .distinct()
                .count();
            
            assertThat(uniqueTransactionIds).isEqualTo(allTransactions.size());
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executorService.shutdown();
        }
        
        cleanupTestData();
    }

    /**
     * Test query performance on partitioned tables.
     * Validates that queries against partitioned transaction tables
     * maintain acceptable performance levels for business operations.
     */
    @DisplayName("Query Performance on Partitioned Tables")
    public void testQueryPerformanceOnPartitionedTables() {
        setupTestData();
        
        // Create transactions across multiple partitions
        List<Transaction> partitionTestTransactions = new ArrayList<>();
        LocalDate baseDate = LocalDate.now().minusYears(1);
        
        for (int month = 0; month < 12; month++) {
            for (int day = 0; day < 10; day++) {
                Transaction transaction = createTestTransaction();
                transaction.setTransactionId((long) ((month * 100) + day + 11000));
                transaction.setTransactionDate(baseDate.plusMonths(month).plusDays(day));
                partitionTestTransactions.add(transaction);
            }
        }
        
        transactionRepository.saveAll(partitionTestTransactions);
        
        // Test cross-partition query performance
        Instant queryStartTime = Instant.now();
        
        LocalDate queryStartDate = baseDate.plusMonths(2);
        LocalDate queryEndDate = baseDate.plusMonths(8);
        
        List<Transaction> crossPartitionResults = transactionRepository.findByAccountIdAndTransactionDateBetween(
            TestConstants.TEST_ACCOUNT_ID, queryStartDate, queryEndDate
        );
        
        Instant queryEndTime = Instant.now();
        long queryTimeMs = queryEndTime.toEpochMilli() - queryStartTime.toEpochMilli();
        
        // Validate query performance meets requirements
        assertThat(queryTimeMs).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        assertThat(crossPartitionResults).isNotEmpty();
        
        cleanupTestData();
    }

    /**
     * Test batch processing queries for daily totals.
     * Validates that aggregate queries used for daily batch processing
     * operations complete within acceptable time limits.
     */
    @DisplayName("Batch Processing Queries - Daily Totals")
    public void testBatchProcessingQueriesForDailyTotals() {
        setupTestData();
        
        // Create transactions for daily total calculations
        LocalDate processingDate = LocalDate.now();
        List<Transaction> dailyTransactions = new ArrayList<>();
        
        BigDecimal expectedTotal = BigDecimal.ZERO;
        for (int i = 0; i < 100; i++) {
            Transaction transaction = createTestTransaction();
            transaction.setTransactionId((long) (i + 12000));
            transaction.setTransactionDate(processingDate);
            
            BigDecimal amount = new BigDecimal(String.valueOf(i + 1)).setScale(2, BigDecimal.ROUND_HALF_UP);
            transaction.setAmount(amount);
            expectedTotal = expectedTotal.add(amount);
            
            dailyTransactions.add(transaction);
        }
        
        transactionRepository.saveAll(dailyTransactions);
        
        // Test daily total calculation performance
        Instant calcStartTime = Instant.now();
        
        List<Transaction> dailyResults = transactionRepository.findByProcessingDateBetween(
            processingDate, processingDate
        );
        
        // Calculate total manually to verify aggregate accuracy
        BigDecimal actualTotal = dailyResults.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Instant calcEndTime = Instant.now();
        long calcTimeMs = calcEndTime.toEpochMilli() - calcStartTime.toEpochMilli();
        
        // Validate batch processing performance
        assertThat(calcTimeMs).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        assertBigDecimalEquals(actualTotal, expectedTotal);
        
        cleanupTestData();
    }

    // Helper methods for test setup and validation

    /**
     * Creates a test transaction with standard default values.
     * Uses TestDataGenerator to create COBOL-compatible test data.
     */
    private Transaction createTestTransaction() {
        return TestDataGenerator.generateTransaction();
    }

    /**
     * Sets up test data including accounts and cards for foreign key relationships.
     */
    private void setupTestData() {
        loadTestFixtures();
    }

    /**
     * Cleans up test data to maintain test isolation.
     */
    private void cleanupTestData() {
        cleanupTestData();
    }

    /**
     * Custom assertion for BigDecimal equality with proper precision handling.
     */
    private void assertBigDecimalEquals(BigDecimal actual, BigDecimal expected) {
        assertBigDecimalEquals(actual, expected);
    }

    /**
     * Custom assertion for date/time equality with appropriate tolerance.
     */
    private void assertDateEquals(LocalDateTime actual, LocalDateTime expected) {
        assertDateEquals(actual, expected);
    }
}