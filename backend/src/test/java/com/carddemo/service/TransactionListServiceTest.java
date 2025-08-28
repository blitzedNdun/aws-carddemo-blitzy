/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.repository.TransactionRepository;
import com.carddemo.entity.Transaction;
import com.carddemo.dto.TransactionListRequest;
import com.carddemo.dto.TransactionListResponse;
import com.carddemo.dto.TransactionSummaryDto;
import com.carddemo.test.TestDataGenerator;
import com.carddemo.test.CobolComparisonUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Comprehensive unit test suite for TransactionListService
 * 
 * Tests the complete COBOL COTRN00C to Java service migration,
 * validating VSAM STARTBR/READNEXT equivalent pagination operations,
 * transaction filtering, and COBOL COMP-3 precision handling.
 * 
 * Test Coverage:
 * - Transaction pagination and VSAM-equivalent operations
 * - Date and amount range filtering
 * - Page navigation (PF7/PF8 key equivalents)  
 * - Performance validation (<200ms response time)
 * - BigDecimal precision matching COBOL COMP-3
 * - Error scenarios and edge cases
 * - COBOL business logic parity validation
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@ExtendWith(MockitoExtension.class)
public class TransactionListServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionListService transactionListService;

    private TestDataGenerator testDataGenerator;
    private TransactionListRequest sampleRequest;
    private Transaction sampleTransaction;
    
    @BeforeEach
    public void setUp() {
        testDataGenerator = new TestDataGenerator();
        
        // Create sample request with valid 11-digit account ID
        sampleRequest = new TransactionListRequest();
        sampleRequest.setAccountId("12345678901");
        sampleRequest.setPageNumber(1);
        sampleRequest.setPageSize(10);
        
        // Create sample transaction
        sampleTransaction = createSampleTransaction(1L, "12345678901", new BigDecimal("100.00"));
    }
    
    /**
     * Helper method to create sample transactions
     */
    private Transaction createSampleTransaction(Long transactionId, String accountId, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setAccountId(Long.parseLong(accountId));
        transaction.setAmount(amount);
        transaction.setTransactionDate(LocalDate.now());
        transaction.setDescription("Test Transaction " + transactionId);
        return transaction;
    }
    
    /**
     * Helper method to generate a list of test transactions
     */
    private List<Transaction> generateTransactionList(int count, String accountId) {
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            BigDecimal amount = new BigDecimal("100.00").multiply(new BigDecimal(i));
            transactions.add(createSampleTransaction((long) i, accountId, amount));
        }
        return transactions;
    }

    @Nested
    @DisplayName("Basic Transaction Listing Tests")
    class BasicTransactionListingTests {

        @Test
        @DisplayName("listTransactions() - Should return paginated transaction list")
        public void testListTransactions_BasicFunctionality() {
            // Given: Mock repository returns sample data
            List<Transaction> transactions = generateTransactionList(5, "12345678901");
            Page<Transaction> page = new PageImpl<>(
                transactions, 
                PageRequest.of(0, 10),
                transactions.size()
            );
            
            when(transactionRepository.findByAccountId(any(Long.class), any(Pageable.class)))
                .thenReturn(page);
            
            // When: Service processes request
            long startTime = System.currentTimeMillis();
            TransactionListResponse response = transactionListService.listTransactions(sampleRequest);
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Then: Response contains expected data
            assertThat(response).isNotNull();
            assertThat(response.getTransactions()).hasSize(5);
            assertThat(response.getCurrentPage()).isEqualTo(1);
            assertThat(response.getTotalCount()).isEqualTo(5);
            assertThat(response.getHasMorePages()).isFalse();
            assertThat(response.getHasPreviousPages()).isFalse();
            
            // Performance validation (<200ms)
            assertThat(responseTime).isLessThan(200L);
            
            // Verify repository interaction
            verify(transactionRepository).findByAccountId(eq(12345678901L), any(Pageable.class));
        }

        @Test
        @DisplayName("listTransactions() - Should handle null request gracefully")
        public void testListTransactions_NullRequest() {
            // When/Then: Should throw IllegalArgumentException
            assertThatThrownBy(() -> transactionListService.listTransactions(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("TransactionListRequest cannot be null");
        }


    }

    @Nested
    @DisplayName("Pagination Tests - VSAM STARTBR/READNEXT Equivalence")
    class PaginationTests {

        @Test
        @DisplayName("nextPage() - Should navigate to next page when available")
        public void testNextPage_NavigateForward() {
            // Given: Current page has more pages available
            List<Transaction> transactions = generateTransactionList(10, "12345678901");
            Page<Transaction> currentPage = new PageImpl<>(
                transactions, 
                PageRequest.of(0, 10),
                20 // Total elements > current page size
            );
            
            List<Transaction> nextPageTransactions = generateTransactionList(10, "12345678901");
            Page<Transaction> nextPage = new PageImpl<>(
                nextPageTransactions, 
                PageRequest.of(1, 10),
                20
            );
            
            when(transactionRepository.findByAccountId(eq(12345678901L), any(Pageable.class)))
                .thenReturn(currentPage, nextPage);
            
            // When: Navigate to next page
            TransactionListResponse response = transactionListService.nextPage(sampleRequest);
            
            // Then: Should return next page
            assertThat(response).isNotNull();
            assertThat(response.getCurrentPage()).isEqualTo(2);
            assertThat(response.getHasPreviousPages()).isTrue();
        }

        @Test
        @DisplayName("previousPage() - Should navigate to previous page when available")  
        public void testPreviousPage_NavigateBackward() {
            // Given: Current page is page 2
            sampleRequest.setPageNumber(2);
            List<Transaction> transactions = generateTransactionList(10, "12345678901");
            Page<Transaction> previousPage = new PageImpl<>(
                transactions, 
                PageRequest.of(0, 10),
                20
            );
            
            when(transactionRepository.findByAccountId(eq(12345678901L), any(Pageable.class)))
                .thenReturn(previousPage);
            
            // When: Navigate to previous page
            TransactionListResponse response = transactionListService.previousPage(sampleRequest);
            
            // Then: Should return previous page
            assertThat(response).isNotNull();
            assertThat(response.getCurrentPage()).isEqualTo(1);
            assertThat(response.getHasPreviousPages()).isFalse();
        }

        @Test
        @DisplayName("previousPage() - Should handle first page boundary")
        public void testPreviousPage_AtFirstPage() {
            // Given: Currently at first page
            List<Transaction> transactions = generateTransactionList(5, "12345678901");
            Page<Transaction> page = new PageImpl<>(
                transactions, 
                PageRequest.of(0, 10),
                5
            );
            
            when(transactionRepository.findByAccountId(eq(12345678901L), any(Pageable.class)))
                .thenReturn(page);
            
            // When: Try to go to previous page
            TransactionListResponse response = transactionListService.previousPage(sampleRequest);
            
            // Then: Should remain at first page
            assertThat(response.getCurrentPage()).isEqualTo(1);
            assertThat(response.getHasPreviousPages()).isFalse();
        }
    }

    @Nested
    @DisplayName("Filtering Tests")
    class FilteringTests {

        @Test
        @DisplayName("applyFilters() - Should filter by account ID and date range")
        public void testApplyFilters_AccountIdAndDateRange() {
            // Given: Request with account ID and date range
            sampleRequest.setStartDate(LocalDate.now().minusDays(30));
            sampleRequest.setEndDate(LocalDate.now());
            Pageable pageable = PageRequest.of(0, 10);
            
            List<Transaction> filteredTransactions = generateTransactionList(5, "12345678901");
            Page<Transaction> page = new PageImpl<>(filteredTransactions, pageable, 5);
            
            when(transactionRepository.findByAccountIdAndTransactionDateBetween(
                eq(12345678901L), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(page);
            
            // When: Apply filters
            Page<Transaction> result = transactionListService.applyFilters(sampleRequest, pageable);
            
            // Then: Should use account ID and date range filtering
            assertThat(result.getContent()).hasSize(5);
            verify(transactionRepository).findByAccountIdAndTransactionDateBetween(
                eq(12345678901L), any(LocalDate.class), any(LocalDate.class), eq(pageable));
        }

        @Test
        @DisplayName("applyFilters() - Should filter by date range only")
        public void testApplyFilters_DateRangeOnly() {
            // Given: Request with only date range (no account ID)
            TransactionListRequest dateOnlyRequest = new TransactionListRequest();
            dateOnlyRequest.setStartDate(LocalDate.now().minusDays(30));
            dateOnlyRequest.setEndDate(LocalDate.now());
            Pageable pageable = PageRequest.of(0, 10);
            
            List<Transaction> filteredTransactions = generateTransactionList(8, "12345678901");
            Page<Transaction> page = new PageImpl<>(filteredTransactions, pageable, 8);
            
            when(transactionRepository.findByTransactionDateBetween(
                any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(page);
            
            // When: Apply filters
            Page<Transaction> result = transactionListService.applyFilters(dateOnlyRequest, pageable);
            
            // Then: Should use date range filtering only
            assertThat(result.getContent()).hasSize(8);
            verify(transactionRepository).findByTransactionDateBetween(
                any(LocalDate.class), any(LocalDate.class), eq(pageable));
        }
    }

    @Nested
    @DisplayName("Performance and Precision Tests")
    class PerformanceAndPrecisionTests {

        @Test
        @DisplayName("listTransactions() - Should complete within 200ms for large datasets")
        public void testListTransactions_PerformanceValidation() {
            // Given: Large dataset (simulate performance test)
            List<Transaction> largeDataset = generateTransactionList(100, "12345678901");
            Page<Transaction> page = new PageImpl<>(
                largeDataset.subList(0, 10),
                PageRequest.of(0, 10),
                largeDataset.size()
            );
            
            when(transactionRepository.findByAccountId(any(Long.class), any(Pageable.class)))
                .thenReturn(page);
            
            // When: Process request
            long startTime = System.currentTimeMillis();
            TransactionListResponse response = transactionListService.listTransactions(sampleRequest);
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Then: Should complete within performance threshold
            assertThat(responseTime).isLessThan(200L);
            assertThat(response.getTransactions()).hasSize(10);
        }

        @Test
        @DisplayName("mapTransactionToSummaryDto() - Should preserve COBOL COMP-3 precision")
        public void testBigDecimalPrecision_CobolCompat() {
            // Given: Transaction with specific decimal precision
            BigDecimal originalAmount = new BigDecimal("123.456"); // More than 2 decimal places
            sampleTransaction.setAmount(originalAmount);
            
            List<Transaction> transactions = Arrays.asList(sampleTransaction);
            Page<Transaction> page = new PageImpl<>(transactions, PageRequest.of(0, 10), 1);
            
            when(transactionRepository.findByAccountId(any(Long.class), any(Pageable.class)))
                .thenReturn(page);
            
            // When: Process transaction
            TransactionListResponse response = transactionListService.listTransactions(sampleRequest);
            
            // Then: Amount should be rounded to 2 decimal places (COBOL COMP-3 behavior)
            TransactionSummaryDto dto = response.getTransactions().get(0);
            assertThat(dto.getAmount()).isEqualTo(new BigDecimal("123.46"));
            assertThat(dto.getAmount().scale()).isEqualTo(2);
            
            // Validate COBOL precision comparison using utility
            boolean precisionMatch = CobolComparisonUtils.compareBigDecimals(
                dto.getAmount(), new BigDecimal("123.46"), "COMP-3");
            assertThat(precisionMatch).isTrue();
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("listTransactions() - Should handle empty result set")
        public void testListTransactions_EmptyResults() {
            // Given: Repository returns empty page
            Page<Transaction> emptyPage = new PageImpl<>(
                new ArrayList<>(),
                PageRequest.of(0, 10),
                0
            );
            
            when(transactionRepository.findByAccountId(any(Long.class), any(Pageable.class)))
                .thenReturn(emptyPage);
            
            // When: Process request
            TransactionListResponse response = transactionListService.listTransactions(sampleRequest);
            
            // Then: Should handle empty results gracefully
            assertThat(response.getTransactions()).isEmpty();
            assertThat(response.getTotalCount()).isEqualTo(0);
            assertThat(response.getHasMorePages()).isFalse();
            assertThat(response.getHasPreviousPages()).isFalse();
        }

        @Test
        @DisplayName("getPageCount() - Should calculate correct page count")
        public void testGetPageCount_CorrectCalculation() {
            // Given: Total of 25 transactions with page size 10
            Page<Transaction> page = new PageImpl<>(
                generateTransactionList(10, "12345678901"),
                PageRequest.of(0, 10),
                25 // Total elements
            );
            
            when(transactionRepository.findByAccountId(any(Long.class), any(Pageable.class)))
                .thenReturn(page);
            
            // When: Get page count
            Integer pageCount = transactionListService.getPageCount(sampleRequest);
            
            // Then: Should return 3 pages (25 / 10 = 2.5, rounded up to 3)
            assertThat(pageCount).isEqualTo(3);
        }

        @Test
        @DisplayName("getCurrentPageNumber() - Should return correct current page")
        public void testGetCurrentPageNumber() {
            // Given: Request with page number 5
            sampleRequest.setPageNumber(5);
            
            // When: Get current page number
            Integer currentPage = transactionListService.getCurrentPageNumber(sampleRequest);
            
            // Then: Should return 5
            assertThat(currentPage).isEqualTo(5);
        }

        @Test
        @DisplayName("getCurrentPageNumber() - Should handle null request")
        public void testGetCurrentPageNumber_NullRequest() {
            // When: Get current page number with null request
            Integer currentPage = transactionListService.getCurrentPageNumber(null);
            
            // Then: Should return default value 1
            assertThat(currentPage).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("COBOL Integration and Data Mapping Tests")
    class CobolIntegrationTests {

        @Test
        @DisplayName("TransactionSummaryDto mapping - Should match COBOL field structure")
        public void testTransactionSummaryDtoMapping() {
            // Given: Transaction with all fields populated
            sampleTransaction.setTransactionId(12345L);
            sampleTransaction.setDescription("AMAZON PURCHASE");
            sampleTransaction.setAmount(new BigDecimal("87.99"));
            sampleTransaction.setTransactionDate(LocalDate.of(2024, 1, 15));
            
            List<Transaction> transactions = Arrays.asList(sampleTransaction);
            Page<Transaction> page = new PageImpl<>(transactions, PageRequest.of(0, 10), 1);
            
            when(transactionRepository.findByAccountId(any(Long.class), any(Pageable.class)))
                .thenReturn(page);
            
            // When: Process transaction
            TransactionListResponse response = transactionListService.listTransactions(sampleRequest);
            
            // Then: DTO should have correct field mappings
            TransactionSummaryDto dto = response.getTransactions().get(0);
            assertThat(dto.getTransactionId()).isEqualTo("12345");
            assertThat(dto.getDescription()).isEqualTo("AMAZON PURCHASE");
            assertThat(dto.getAmount()).isEqualTo(new BigDecimal("87.99"));
            assertThat(dto.getDate()).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(dto.getSelected()).isFalse(); // Default selection state
        }

        @Test
        @DisplayName("Date handling - Should preserve COBOL date format compatibility")
        public void testDateHandling_CobolCompatibility() {
            // Given: Transactions with various dates
            LocalDate testDate1 = LocalDate.of(2024, 3, 15);
            LocalDate testDate2 = LocalDate.of(2024, 12, 31);
            
            Transaction trans1 = createSampleTransaction(1L, "12345678901", new BigDecimal("100.00"));
            trans1.setTransactionDate(testDate1);
            
            Transaction trans2 = createSampleTransaction(2L, "12345678901", new BigDecimal("200.00"));
            trans2.setTransactionDate(testDate2);
            
            List<Transaction> transactions = Arrays.asList(trans1, trans2);
            Page<Transaction> page = new PageImpl<>(transactions, PageRequest.of(0, 10), 2);
            
            when(transactionRepository.findByAccountId(any(Long.class), any(Pageable.class)))
                .thenReturn(page);
            
            // When: Process transactions
            TransactionListResponse response = transactionListService.listTransactions(sampleRequest);
            
            // Then: Dates should be preserved correctly
            List<TransactionSummaryDto> dtos = response.getTransactions();
            assertThat(dtos.get(0).getDate()).isEqualTo(testDate1);
            assertThat(dtos.get(1).getDate()).isEqualTo(testDate2);
        }
    }
}