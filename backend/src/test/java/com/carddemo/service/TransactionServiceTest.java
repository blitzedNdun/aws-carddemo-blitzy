/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

// External imports - JUnit 5 and Mockito testing framework
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.reset;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.InjectMocks;
import org.mockito.Mock;

// Java standard library imports
import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// Spring framework imports  
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

// Internal imports - Business service and repositories
import com.carddemo.service.TransactionService;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardRepository;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.Account;
import com.carddemo.entity.Card;

// Internal imports - Test infrastructure
import com.carddemo.service.BaseServiceTest;
import com.carddemo.service.TestDataBuilder;
import com.carddemo.service.PerformanceTestUtils;
import com.carddemo.service.MockServiceFactory;

/**
 * Comprehensive unit test class for TransactionService that validates transaction processing
 * functionality migrated from COBOL programs COTRN00C, COTRN01C, and COTRN02C.
 * 
 * This test class ensures complete functional parity with the original CICS COBOL transaction
 * processing system while validating modern Spring Boot service operations. Tests cover all
 * three primary transaction operations: listing with pagination, viewing details, and creating
 * new transactions.
 * 
 * Key Testing Areas:
 * - Transaction listing with VSAM-equivalent cursor-based pagination (COTRN00C functionality)
 * - Single transaction detail retrieval (COTRN01C functionality) 
 * - New transaction creation with validation (COTRN02C functionality)
 * - Performance validation against 200ms SLA requirement
 * - BigDecimal precision matching COBOL COMP-3 packed decimal behavior
 * - CVTRA05Y copybook 350-byte record structure mapping validation
 * - Transaction boundary management replicating CICS SYNCPOINT behavior
 * - Concurrent transaction handling and optimistic locking scenarios
 * - Error handling and edge case validation
 * 
 * COBOL Migration Validation:
 * All test methods validate that the Java implementation maintains identical behavior
 * to the original COBOL programs, including:
 * - Exact pagination behavior matching VSAM STARTBR/READNEXT/READPREV operations
 * - Financial calculation precision using BigDecimal with COBOL rounding modes
 * - Transaction validation rules matching original COBOL edit routines
 * - Response time requirements under 200ms to match CICS performance
 * - Data structure compatibility with CVTRA05Y 350-byte transaction records
 * 
 * Performance Requirements:
 * - All service operations must complete within 200ms SLA
 * - Transaction listing must support 10 records per page with sub-100ms response
 * - Concurrent transaction processing must handle optimistic locking correctly
 * - Memory usage must remain stable during high-volume testing
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@ExtendWith(MockitoExtension.class)
@Transactional
public class TransactionServiceTest extends BaseServiceTest {

    // Service under test - injected with mocked dependencies
    @InjectMocks
    private TransactionService transactionService;

    // Repository mocks - configured via MockServiceFactory
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private CardRepository cardRepository;

    // Test utilities and data builders
    private TestDataBuilder testDataBuilder;
    private PerformanceTestUtils performanceTestUtils;
    private MockServiceFactory mockServiceFactory;

    // Test data entities - created once and reused
    private Transaction testTransaction;
    private Account testAccount;
    private Card testCard;
    private List<Transaction> testTransactionList;

    // Constants for test validation
    private static final Long TEST_ACCOUNT_ID = 1000000001L;
    private static final String TEST_CARD_NUMBER = "4532123456789012";
    private static final BigDecimal TEST_AMOUNT = new BigDecimal("125.50").setScale(2);
    private static final int TEST_PAGE_SIZE = 10;
    private static final int TEST_PAGE_NUMBER = 0;

    /**
     * Test setup method executed before each test to initialize test environment.
     * Configures mock repositories, creates test data, and sets up performance monitoring.
     * 
     * Establishes:
     * - Mock repository configurations with realistic responses
     * - Test entity creation with COBOL-compatible data structures
     * - Performance measurement utilities for 200ms SLA validation
     * - Transaction boundary setup for test isolation
     */
    @BeforeEach
    void setUp() {
        // Initialize base test environment
        super.setUp();
        
        // Initialize test utilities
        testDataBuilder = new TestDataBuilder();
        performanceTestUtils = new PerformanceTestUtils();
        mockServiceFactory = new MockServiceFactory();
        
        // Create test entities with realistic COBOL-compatible data
        testAccount = createTestAccount();
        testCard = createTestCard();
        testTransaction = createTestTransaction();
        testTransactionList = createTestTransactionList(10);
        
        // Configure mock repositories with standard success responses
        configureMockRepositories();
        
        // Validate test data meets COBOL precision requirements
        validateTestDataPrecision();
    }

    /**
     * Nested test class for transaction listing functionality (COTRN00C migration).
     * Tests cursor-based pagination, filtering, and performance requirements.
     */
    @Nested
    @DisplayName("Transaction Listing Tests (COTRN00C)")
    class TransactionListingTests {

        @Test
        @DisplayName("Should list transactions with pagination under 200ms SLA")
        void testListTransactionsWithPagination() {
            // Arrange
            Pageable pageable = PageRequest.of(TEST_PAGE_NUMBER, TEST_PAGE_SIZE);
            Page<Transaction> expectedPage = new PageImpl<>(testTransactionList, pageable, testTransactionList.size());
            
            when(transactionRepository.findByAccountIdAndDateRange(anyLong(), any(LocalDate.class), 
                any(LocalDate.class), any(Pageable.class))).thenReturn(expectedPage);

            // Act & Assert Performance
            long executionTime = measurePerformance(() -> {
                Page<Transaction> result = transactionService.listTransactions(TEST_ACCOUNT_ID, 
                    LocalDate.now().minusMonths(1), LocalDate.now(), pageable);
                
                // Validate results
                assertAll("Transaction listing validation",
                    () -> assertNotNull(result, "Result should not be null"),
                    () -> assertEquals(TEST_PAGE_SIZE, result.getContent().size(), 
                        "Should return correct page size"),
                    () -> assertEquals(TEST_PAGE_NUMBER, result.getNumber(), 
                        "Should return correct page number"),
                    () -> assertTrue(result.hasContent(), "Should have transaction content"),
                    () -> validateTransactionListStructure(result.getContent())
                );
                
                return result;
            });

            // Validate performance SLA
            validateResponseTime(executionTime, MAX_RESPONSE_TIME_MS);
            
            // Verify repository interaction
            verify(transactionRepository, times(1)).findByAccountIdAndDateRange(
                anyLong(), any(LocalDate.class), any(LocalDate.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should handle pagination forward like VSAM READNEXT operations")
        void testProcessPageNavigation() {
            // Arrange - simulate VSAM browse forward operation
            Pageable firstPage = PageRequest.of(0, TEST_PAGE_SIZE);
            Pageable secondPage = PageRequest.of(1, TEST_PAGE_SIZE);
            
            List<Transaction> firstPageData = testTransactionList.subList(0, TEST_PAGE_SIZE);
            List<Transaction> secondPageData = createTestTransactionList(5); // Partial second page
            
            Page<Transaction> firstPageResult = new PageImpl<>(firstPageData, firstPage, 15);
            Page<Transaction> secondPageResult = new PageImpl<>(secondPageData, secondPage, 15);
            
            when(transactionRepository.findByAccountIdAndDateRange(anyLong(), any(LocalDate.class), 
                any(LocalDate.class), eq(firstPage))).thenReturn(firstPageResult);
            when(transactionRepository.findByAccountIdAndDateRange(anyLong(), any(LocalDate.class), 
                any(LocalDate.class), eq(secondPage))).thenReturn(secondPageResult);

            // Act & Assert Performance for pagination operations
            long executionTime = measurePerformance(() -> {
                // Test forward pagination like COBOL PROCESS-PF8-KEY
                Page<Transaction> firstResult = transactionService.getTransactionPage(TEST_ACCOUNT_ID, 
                    LocalDate.now().minusMonths(1), LocalDate.now(), firstPage);
                Page<Transaction> secondResult = transactionService.getTransactionPage(TEST_ACCOUNT_ID,
                    LocalDate.now().minusMonths(1), LocalDate.now(), secondPage);
                
                // Validate pagination behavior matches VSAM browse
                assertAll("Forward pagination validation",
                    () -> assertEquals(TEST_PAGE_SIZE, firstResult.getContent().size(),
                        "First page should be full"),
                    () -> assertEquals(5, secondResult.getContent().size(),
                        "Second page should have remaining records"),
                    () -> assertTrue(firstResult.hasNext(), 
                        "First page should indicate more records available"),
                    () -> assertFalse(secondResult.hasNext(),
                        "Second page should indicate no more records")
                );
                
                return secondResult;
            });

            // Validate performance meets CICS response time requirements
            validateResponseTime(executionTime, MAX_RESPONSE_TIME_MS);
        }

        @Test
        @DisplayName("Should validate CVTRA05Y record structure mapping")
        void testCVTRA05YRecordStructureMapping() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 1);
            Page<Transaction> singleTransactionPage = new PageImpl<>(
                List.of(testTransaction), pageable, 1);
            
            when(transactionRepository.findByAccountIdAndDateRange(anyLong(), any(LocalDate.class),
                any(LocalDate.class), any(Pageable.class))).thenReturn(singleTransactionPage);

            // Act
            long executionTime = measurePerformance(() -> {
                Page<Transaction> result = transactionService.listTransactions(TEST_ACCOUNT_ID,
                    LocalDate.now().minusMonths(1), LocalDate.now(), pageable);
                
                Transaction transaction = result.getContent().get(0);
                
                // Assert CVTRA05Y copybook field mappings (350-byte structure)
                assertAll("CVTRA05Y structure validation",
                    () -> assertNotNull(transaction.getTransactionId(), 
                        "TRAN-ID field mapping"),
                    () -> assertNotNull(transaction.getAccountId(),
                        "TRAN-ACCT-ID field mapping"),  
                    () -> assertNotNull(transaction.getCardNumber(),
                        "TRAN-CARD-NUM field mapping"),
                    () -> assertAmountPrecision(transaction.getAmount()),
                        // TRAN-AMT COMP-3 precision validation
                    () -> assertNotNull(transaction.getDescription(),
                        "TRAN-DESC field mapping"),
                    () -> assertNotNull(transaction.getMerchantName(),
                        "TRAN-MERCHANT-NAME field mapping"),
                    () -> assertNotNull(transaction.getTransactionDate(),
                        "TRAN-ORIG-TS field mapping")
                );
                
                return result;
            });

            validateResponseTime(executionTime, MAX_RESPONSE_TIME_MS);
        }
    }

    /**
     * Nested test class for transaction detail retrieval (COTRN01C migration).
     * Tests single transaction lookup, validation, and performance.
     */
    @Nested
    @DisplayName("Transaction Detail Tests (COTRN01C)")
    class TransactionDetailTests {

        @Test
        @DisplayName("Should retrieve transaction detail under 200ms SLA")
        void testGetTransactionDetail() {
            // Arrange
            String transactionId = "0000000000000001";
            when(transactionRepository.findById(anyString())).thenReturn(Optional.of(testTransaction));

            // Act & Assert Performance
            long executionTime = measurePerformance(() -> {
                Optional<Transaction> result = transactionService.getTransactionDetail(transactionId);
                
                assertAll("Transaction detail validation",
                    () -> assertTrue(result.isPresent(), "Transaction should be found"),
                    () -> assertEquals(transactionId, result.get().getTransactionId(),
                        "Transaction ID should match"),
                    () -> assertAmountPrecision(result.get().getAmount()),
                    () -> assertEquals(TEST_ACCOUNT_ID, result.get().getAccountId(),
                        "Account ID should match"),
                    () -> validateTransactionDetailStructure(result.get())
                );
                
                return result;
            });

            // Validate performance SLA
            validateResponseTime(executionTime, MAX_RESPONSE_TIME_MS);
            
            // Verify repository interaction
            verify(transactionRepository, times(1)).findById(transactionId);
        }

        @Test
        @DisplayName("Should handle transaction not found scenarios")
        void testGetTransactionDetailNotFound() {
            // Arrange
            String nonExistentTransactionId = "9999999999999999";
            when(transactionRepository.findById(nonExistentTransactionId))
                .thenReturn(Optional.empty());

            // Act & Assert
            long executionTime = measurePerformance(() -> {
                Optional<Transaction> result = transactionService.getTransactionDetail(nonExistentTransactionId);
                
                assertFalse(result.isPresent(), "Transaction should not be found");
                return result;
            });

            validateResponseTime(executionTime, MAX_RESPONSE_TIME_MS);
        }
    }

    /**
     * Nested test class for transaction creation functionality (COTRN02C migration).
     * Tests validation, persistence, and business rule enforcement.
     */
    @Nested
    @DisplayName("Transaction Creation Tests (COTRN02C)")
    class TransactionCreationTests {

        @Test
        @DisplayName("Should add new transaction with validation under 200ms SLA")
        void testAddTransaction() {
            // Arrange
            Transaction newTransaction = createTestTransaction();
            newTransaction.setTransactionId(null); // New transaction has no ID yet
            
            when(accountRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(cardRepository.findById(TEST_CARD_NUMBER)).thenReturn(Optional.of(testCard));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

            // Act & Assert Performance
            long executionTime = measurePerformance(() -> {
                Transaction result = transactionService.addTransaction(newTransaction);
                
                assertAll("Transaction creation validation",
                    () -> assertNotNull(result, "Created transaction should not be null"),
                    () -> assertNotNull(result.getTransactionId(), 
                        "Transaction ID should be generated"),
                    () -> assertEquals(TEST_ACCOUNT_ID, result.getAccountId(),
                        "Account ID should match"),
                    () -> assertAmountPrecision(result.getAmount()),
                    () -> assertNotNull(result.getTransactionDate(),
                        "Transaction date should be set"),
                    () -> validateBusinessRules(result)
                );
                
                return result;
            });

            // Validate performance meets COBOL requirements
            validateResponseTime(executionTime, MAX_RESPONSE_TIME_MS);
            
            // Verify all validation steps were performed
            verify(accountRepository, times(1)).findById(TEST_ACCOUNT_ID);
            verify(cardRepository, times(1)).findById(anyString());
            verify(transactionRepository, times(1)).save(any(Transaction.class));
        }

        @Test
        @DisplayName("Should validate transaction amounts with COBOL precision")
        void testValidateTransaction() {
            // Arrange - Test various amounts with different precisions
            List<BigDecimal> testAmounts = List.of(
                new BigDecimal("0.01"),      // Minimum amount
                new BigDecimal("999.99"),    // Standard amount  
                new BigDecimal("9999.99"),   // Large amount
                new BigDecimal("0.00")       // Zero amount - should fail validation
            );

            for (BigDecimal amount : testAmounts) {
                Transaction transaction = createTestTransaction();
                transaction.setAmount(amount.setScale(2, java.math.RoundingMode.HALF_UP));
                
                // Act & Assert Performance
                long executionTime = measurePerformance(() -> {
                    boolean isValid = transactionService.validateTransaction(transaction);
                    
                    if (amount.compareTo(BigDecimal.ZERO) > 0) {
                        assertTrue(isValid, "Valid amount should pass validation: " + amount);
                        assertAmountPrecision(transaction.getAmount());
                    } else {
                        assertFalse(isValid, "Zero amount should fail validation");
                    }
                    
                    return isValid;
                });

                validateResponseTime(executionTime, MAX_RESPONSE_TIME_MS);
            }
        }
    }

    /**
     * Nested test class for concurrent transaction handling and optimistic locking.
     * Tests multi-user scenarios and data consistency.
     */
    @Nested
    @DisplayName("Concurrent Transaction Tests")
    class ConcurrentTransactionTests {

        @Test
        @DisplayName("Should handle concurrent transaction processing with optimistic locking")
        void testConcurrentTransactionHandling() throws Exception {
            // Arrange
            ExecutorService executorService = Executors.newFixedThreadPool(5);
            List<CompletableFuture<Transaction>> futures = new ArrayList<>();
            
            // Configure mocks for concurrent access
            when(accountRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(cardRepository.findById(anyString())).thenReturn(Optional.of(testCard));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

            // Act - Submit concurrent transaction creation requests
            long startTime = System.nanoTime();
            for (int i = 0; i < 5; i++) {
                CompletableFuture<Transaction> future = CompletableFuture.supplyAsync(() -> {
                    Transaction newTransaction = createTestTransaction();
                    newTransaction.setTransactionId(null);
                    newTransaction.setAmount(new BigDecimal("100.00").setScale(2));
                    return transactionService.addTransaction(newTransaction);
                }, executorService);
                futures.add(future);
            }

            // Wait for all operations to complete
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
            allFutures.get(5, TimeUnit.SECONDS);
            
            long executionTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            // Assert - All transactions should complete successfully
            for (CompletableFuture<Transaction> future : futures) {
                Transaction result = future.get();
                assertNotNull(result, "Concurrent transaction should complete");
                assertAmountPrecision(result.getAmount());
            }

            // Validate overall performance under concurrent load
            assertTrue(executionTime < 1000, // Allow more time for concurrent operations
                "Concurrent operations should complete within reasonable time");
            
            executorService.shutdown();
        }

        @Test
        @DisplayName("Should handle optimistic locking conflicts")
        void testOptimisticLockingFailure() {
            // Arrange
            Transaction conflictingTransaction = createTestTransaction();
            when(accountRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(cardRepository.findById(anyString())).thenReturn(Optional.of(testCard));
            when(transactionRepository.save(any(Transaction.class)))
                .thenThrow(new OptimisticLockingFailureException("Concurrent modification"));

            // Act & Assert
            assertThrows(OptimisticLockingFailureException.class, () -> {
                transactionService.addTransaction(conflictingTransaction);
            }, "Should throw OptimisticLockingFailureException for concurrent modifications");
        }
    }

    /**
     * Helper method to configure mock repositories with realistic test responses.
     * Sets up standard success scenarios for all repository operations.
     */
    private void configureMockRepositories() {
        // Configure TransactionRepository mock
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        
        // Configure AccountRepository mock  
        when(accountRepository.findById(anyLong())).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        
        // Configure CardRepository mock
        when(cardRepository.findById(anyString())).thenReturn(Optional.of(testCard));
        when(cardRepository.findByAccountId(anyLong())).thenReturn(List.of(testCard));
    }

    /**
     * Creates a list of test transactions for pagination testing.
     * 
     * @param count Number of transactions to create
     * @return List of test Transaction entities
     */
    private List<Transaction> createTestTransactionList(int count) {
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Transaction transaction = createTestTransaction();
            transaction.setTransactionId(String.format("%016d", i + 1));
            transaction.setAmount(new BigDecimal("100.00").add(new BigDecimal(i)).setScale(2));
            transactions.add(transaction);
        }
        return transactions;
    }

    /**
     * Validates test data precision meets COBOL COMP-3 requirements.
     */
    private void validateTestDataPrecision() {
        assertAmountPrecision(testTransaction.getAmount());
        assertAmountPrecision(testAccount.getCurrentBalance());
        assertAmountPrecision(testAccount.getCreditLimit());
    }

    /**
     * Validates transaction list structure and data integrity.
     * 
     * @param transactions List of transactions to validate
     */
    private void validateTransactionListStructure(List<Transaction> transactions) {
        assertNotNull(transactions, "Transaction list should not be null");
        assertFalse(transactions.isEmpty(), "Transaction list should not be empty");
        
        for (Transaction transaction : transactions) {
            assertAll("Individual transaction validation",
                () -> assertNotNull(transaction.getTransactionId(), 
                    "Transaction ID should not be null"),
                () -> assertNotNull(transaction.getAccountId(),
                    "Account ID should not be null"),
                () -> assertAmountPrecision(transaction.getAmount()),
                () -> assertNotNull(transaction.getTransactionDate(),
                    "Transaction date should not be null")
            );
        }
    }

    /**
     * Validates transaction detail structure matches COBOL requirements.
     * 
     * @param transaction Transaction to validate
     */
    private void validateTransactionDetailStructure(Transaction transaction) {
        assertAll("Transaction detail structure validation",
            () -> assertNotNull(transaction.getTransactionId()),
            () -> assertNotNull(transaction.getAccountId()),
            () -> assertNotNull(transaction.getAmount()),
            () -> assertNotNull(transaction.getDescription()),
            () -> assertNotNull(transaction.getMerchantName()),
            () -> assertNotNull(transaction.getTransactionDate()),
            () -> assertAmountPrecision(transaction.getAmount())
        );
    }

    /**
     * Validates business rules for transaction processing.
     * 
     * @param transaction Transaction to validate
     */
    private void validateBusinessRules(Transaction transaction) {
        assertAll("Business rule validation",
            () -> assertTrue(transaction.getAmount().compareTo(BigDecimal.ZERO) > 0,
                "Transaction amount must be positive"),
            () -> assertNotNull(transaction.getAccountId(),
                "Account ID is required"),
            () -> assertNotNull(transaction.getDescription(),
                "Transaction description is required"),
            () -> assertTrue(transaction.getDescription().length() <= 50,
                "Description must not exceed 50 characters")
        );
    }
}