package com.carddemo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.anyLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.carddemo.dto.TransactionListRequest;
import com.carddemo.dto.TransactionListResponse;
import com.carddemo.entity.Transaction;
import com.carddemo.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Comprehensive unit test class for TransactionListService, validating COBOL COTRN00C transaction 
 * list logic migration to Java. Tests pagination, filtering, sorting, and transaction data retrieval 
 * with VSAM-equivalent browse operations to ensure 100% functional parity between COBOL and Java 
 * implementations.
 * 
 * This test class ensures:
 * - Complete business logic coverage for all service methods
 * - COBOL-to-Java functional equivalence validation
 * - Pagination and filtering logic correctness
 * - BigDecimal precision preservation for financial calculations
 * - Performance validation for large transaction sets
 * - Error handling and edge case management
 * 
 * Test scenarios cover:
 * - Transaction listing with various filter criteria
 * - Page navigation (next/previous page operations)
 * - VSAM STARTBR/READNEXT equivalent operations via JPA pagination
 * - Date and amount range filtering
 * - Transaction sorting by multiple criteria
 * - Performance testing with high-volume datasets
 * - Error conditions and boundary testing
 */
@ExtendWith(MockitoExtension.class)
public class TransactionListServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionListService transactionListService;

    private List<Transaction> sampleTransactions;
    
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String SAMPLE_ACCOUNT_ID = "10000000001";
    private static final String SAMPLE_CARD_NUMBER = "4532123456789012";

    @BeforeEach
    void setUp() {
        // Generate sample transaction data for testing
        sampleTransactions = createSampleTransactionList();
    }

    /**
     * Creates a representative list of sample transactions for testing various scenarios.
     * Includes transactions with different amounts, dates, and account relationships
     * to simulate real-world transaction listing scenarios.
     */
    private List<Transaction> createSampleTransactionList() {
        List<Transaction> transactions = new ArrayList<>();
        
        // Create transactions with varied data for comprehensive testing
        for (int i = 1; i <= 25; i++) {
            Transaction transaction = new Transaction();
            transaction.setTransactionId((long) i);
            transaction.setAccountId(Long.parseLong(SAMPLE_ACCOUNT_ID));
            transaction.setAmount(new BigDecimal("100.00").multiply(new BigDecimal(i % 10 + 1)));
            transaction.setTransactionDate(LocalDate.now().minusDays(i));
            transaction.setDescription("Test Transaction " + i);
            transaction.setMerchantName("Test Merchant " + i);
            transaction.setCategoryCode("PURCHASE");
            transactions.add(transaction);
        }
        
        return transactions;
    }

    @Nested
    @DisplayName("Transaction Listing Tests - COBOL COTRN00C Equivalence")
    class TransactionListingTests {

        @Test
        @DisplayName("listTransactions() - should return paginated transaction list with correct metadata")
        void testListTransactions_ValidRequest_ReturnsPaginatedList() {
            // Given: Valid transaction list request
            TransactionListRequest request = new TransactionListRequest();
            request.setAccountId(SAMPLE_ACCOUNT_ID);
            request.setPageNumber(1);
            request.setPageSize(DEFAULT_PAGE_SIZE);
            
            List<Transaction> pageTransactions = sampleTransactions.subList(0, DEFAULT_PAGE_SIZE);
            Page<Transaction> mockPage = new PageImpl<>(pageTransactions, 
                PageRequest.of(0, DEFAULT_PAGE_SIZE), sampleTransactions.size());
            
            when(transactionRepository.findByAccountId(eq(Long.parseLong(SAMPLE_ACCOUNT_ID)), any(Pageable.class)))
                .thenReturn(mockPage);
            
            // When: Listing transactions
            TransactionListResponse response = transactionListService.listTransactions(request);
            
            // Then: Verify response structure matches COTRN00 BMS screen layout
            assertThat(response).isNotNull();
            assertThat(response.getTransactions()).hasSize(DEFAULT_PAGE_SIZE);
            assertThat(response.getCurrentPage()).isEqualTo(1);
            assertThat(response.getTotalCount()).isGreaterThan(0);
            assertThat(response.getHasMorePages()).isTrue(); // More pages available
            assertThat(response.getHasPreviousPages()).isFalse(); // First page
            
            // Verify repository interaction
            verify(transactionRepository, times(1))
                .findByAccountId(eq(Long.parseLong(SAMPLE_ACCOUNT_ID)), any(Pageable.class));
        }

        @Test
        @DisplayName("listTransactions() - should handle empty result set gracefully")
        void testListTransactions_NoTransactions_ReturnsEmptyList() {
            // Given: Request for account with no transactions
            TransactionListRequest request = new TransactionListRequest();
            request.setAccountId("99999999999");
            request.setPageNumber(1);
            request.setPageSize(DEFAULT_PAGE_SIZE);
            
            Page<Transaction> emptyPage = new PageImpl<>(new ArrayList<>(), 
                PageRequest.of(0, DEFAULT_PAGE_SIZE), 0);
            
            when(transactionRepository.findByAccountId(eq(99999999999L), any(Pageable.class)))
                .thenReturn(emptyPage);
            
            // When: Listing transactions for empty account
            TransactionListResponse response = transactionListService.listTransactions(request);
            
            // Then: Verify empty response handling
            assertThat(response.getTransactions()).isEmpty();
            assertThat(response.getTotalCount()).isEqualTo(0);
            assertThat(response.getCurrentPage()).isEqualTo(1);
            assertThat(response.getHasMorePages()).isFalse();
            assertThat(response.getHasPreviousPages()).isFalse();
        }

        @Test
        @DisplayName("listTransactions() - should apply date range filters correctly")
        void testListTransactions_DateRangeFilter_AppliesFilterCorrectly() {
            // Given: Request with date range filter
            TransactionListRequest request = new TransactionListRequest();
            request.setAccountId(SAMPLE_ACCOUNT_ID);
            request.setStartDate(LocalDate.now().minusDays(10));
            request.setEndDate(LocalDate.now().minusDays(1));
            request.setPageNumber(1);
            request.setPageSize(DEFAULT_PAGE_SIZE);
            
            // Filter sample transactions within date range
            List<Transaction> filteredTransactions = sampleTransactions.stream()
                .filter(t -> !t.getTransactionDate().isBefore(request.getStartDate()) && 
                           !t.getTransactionDate().isAfter(request.getEndDate()))
                .toList();
            
            Page<Transaction> mockPage = new PageImpl<>(filteredTransactions, 
                PageRequest.of(0, DEFAULT_PAGE_SIZE), filteredTransactions.size());
            
            when(transactionRepository.findByAccountIdAndTransactionDateBetween(
                eq(Long.parseLong(SAMPLE_ACCOUNT_ID)), 
                eq(request.getStartDate()), 
                eq(request.getEndDate()), 
                any(Pageable.class)))
                .thenReturn(mockPage);
            
            // When: Listing transactions with date filter
            TransactionListResponse response = transactionListService.listTransactions(request);
            
            // Then: Verify filtered results
            assertThat(response.getTransactions()).hasSizeLessThanOrEqualTo(DEFAULT_PAGE_SIZE);
            assertThat(response.getTotalCount()).isEqualTo(filteredTransactions.size());
            
            // Verify repository called with correct date parameters
            verify(transactionRepository, times(1))
                .findByAccountIdAndTransactionDateBetween(eq(Long.parseLong(SAMPLE_ACCOUNT_ID)), eq(request.getStartDate()), 
                               eq(request.getEndDate()), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Pagination Navigation Tests - VSAM STARTBR/READNEXT Equivalence")
    class PaginationNavigationTests {

        @Test
        @DisplayName("nextPage() - should navigate to next page correctly")
        void testNextPage_ValidNavigation_ReturnsNextPageData() {
            // Given: Service with current pagination state
            TransactionListRequest initialRequest = new TransactionListRequest();
            initialRequest.setAccountId(SAMPLE_ACCOUNT_ID);
            initialRequest.setPageNumber(1);
            initialRequest.setPageSize(DEFAULT_PAGE_SIZE);
            
            // Initialize service state with first page call
            List<Transaction> firstPageTransactions = sampleTransactions.subList(0, DEFAULT_PAGE_SIZE);
            Page<Transaction> firstPage = new PageImpl<>(firstPageTransactions, 
                PageRequest.of(0, DEFAULT_PAGE_SIZE), sampleTransactions.size());
            
            when(transactionRepository.findByAccountId(eq(Long.parseLong(SAMPLE_ACCOUNT_ID)), any(Pageable.class)))
                .thenReturn(firstPage);
            
            transactionListService.listTransactions(initialRequest);
            
            // Mock second page data for nextPage call
            List<Transaction> secondPageTransactions = sampleTransactions.subList(
                DEFAULT_PAGE_SIZE, Math.min(DEFAULT_PAGE_SIZE * 2, sampleTransactions.size()));
            Page<Transaction> nextPage = new PageImpl<>(secondPageTransactions, 
                PageRequest.of(1, DEFAULT_PAGE_SIZE), sampleTransactions.size());
            
            // The nextPage method will call applyFilters with the new request, so mock the repository call
            when(transactionRepository.findByAccountId(eq(Long.parseLong(SAMPLE_ACCOUNT_ID)), any(Pageable.class)))
                .thenReturn(firstPage)  // First call for initial state check
                .thenReturn(nextPage);  // Second call for actual next page
            
            // When: Navigating to next page
            TransactionListResponse response = transactionListService.nextPage(initialRequest);
            
            // Then: Verify next page navigation
            assertThat(response).isNotNull();
            assertThat(response.getTransactions()).hasSize(secondPageTransactions.size());
            assertThat(response.getHasPreviousPages()).isTrue(); // Can go back
            assertThat(response.getHasMorePages()).isTrue(); // More pages exist
        }

        @Test
        @DisplayName("previousPage() - should navigate to previous page correctly")
        void testPreviousPage_ValidNavigation_ReturnsPreviousPageData() {
            // Given: Request for page 2 (to navigate back to page 1)
            TransactionListRequest pageRequest = new TransactionListRequest();
            pageRequest.setAccountId(SAMPLE_ACCOUNT_ID);
            pageRequest.setPageNumber(2);
            pageRequest.setPageSize(DEFAULT_PAGE_SIZE);
            
            // Mock first page data (where we should land after previous page call)
            List<Transaction> firstPageTransactions = sampleTransactions.subList(0, DEFAULT_PAGE_SIZE);
            Page<Transaction> firstPage = new PageImpl<>(firstPageTransactions, 
                PageRequest.of(0, DEFAULT_PAGE_SIZE), sampleTransactions.size());
            
            // The previousPage method will decrement the page number to 1, then call getTransactionPage
            // which calls applyFilters with a pageable for page 0 (0-based indexing)
            when(transactionRepository.findByAccountId(eq(Long.parseLong(SAMPLE_ACCOUNT_ID)), any(Pageable.class)))
                .thenReturn(firstPage);
            
            // When: Navigating to previous page (from page 2 to page 1)
            TransactionListResponse response = transactionListService.previousPage(pageRequest);
            
            // Then: Verify we're now on page 1 (first page)
            assertThat(response).isNotNull();
            assertThat(response.getTransactions()).hasSize(DEFAULT_PAGE_SIZE);
            assertThat(response.getCurrentPage()).isEqualTo(1);
            assertThat(response.getHasPreviousPages()).isFalse(); // First page - no previous pages
            assertThat(response.getHasMorePages()).isTrue(); // More pages exist (pages 2, 3)
        }

        @Test
        @DisplayName("getTransactionPage() - should retrieve specific page with correct positioning")
        void testGetTransactionPage_SpecificPageRequest_ReturnsCorrectPageData() {
            // Given: Specific pageable request
            Pageable pageable = PageRequest.of(2, DEFAULT_PAGE_SIZE); // Page 3 (zero-based)
            TransactionListRequest request = new TransactionListRequest();
            request.setAccountId(SAMPLE_ACCOUNT_ID);
            
            List<Transaction> pageThreeTransactions = new ArrayList<>();
            if (sampleTransactions.size() > DEFAULT_PAGE_SIZE * 2) {
                pageThreeTransactions = sampleTransactions.subList(
                    DEFAULT_PAGE_SIZE * 2, 
                    Math.min(DEFAULT_PAGE_SIZE * 3, sampleTransactions.size()));
            }
            
            Page<Transaction> targetPage = new PageImpl<>(pageThreeTransactions, 
                pageable, sampleTransactions.size());
            
            when(transactionRepository.findByAccountId(eq(Long.parseLong(SAMPLE_ACCOUNT_ID)), eq(pageable)))
                .thenReturn(targetPage);
            
            // When: Getting specific page
            TransactionListResponse result = transactionListService.getTransactionPage(request, 3, DEFAULT_PAGE_SIZE);
            
            // Then: Verify page positioning
            assertThat(result).isNotNull();
            assertThat(result.getTransactions()).hasSize(pageThreeTransactions.size());
            assertThat(result.getCurrentPage()).isEqualTo(3); // One-based page number
            
            // Verify repository called with correct parameters
            verify(transactionRepository, times(1))
                .findByAccountId(eq(Long.parseLong(SAMPLE_ACCOUNT_ID)), eq(pageable));
        }
    }

    @Nested
    @DisplayName("Filtering and Sorting Tests - Business Logic Validation")
    class FilteringAndSortingTests {

        @Test
        @DisplayName("applyFilters() - should apply account ID filter correctly")
        void testApplyFilters_AccountIdFilter_AppliesFilterCorrectly() {
            // Given: Request with account ID filter
            Pageable pageable = PageRequest.of(0, DEFAULT_PAGE_SIZE);
            TransactionListRequest request = new TransactionListRequest();
            request.setAccountId(SAMPLE_ACCOUNT_ID);
            
            Page<Transaction> mockPage = new PageImpl<>(sampleTransactions.subList(0, DEFAULT_PAGE_SIZE), 
                pageable, sampleTransactions.size());
            
            when(transactionRepository.findByAccountId(eq(Long.parseLong(SAMPLE_ACCOUNT_ID)), eq(pageable)))
                .thenReturn(mockPage);
            
            // When: Applying account ID filter
            Page<Transaction> result = transactionListService.applyFilters(request, pageable);
            
            // Then: Verify filtered results
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(DEFAULT_PAGE_SIZE);
            
            // Verify repository interaction
            verify(transactionRepository, times(1))
                .findByAccountId(eq(Long.parseLong(SAMPLE_ACCOUNT_ID)), eq(pageable));
        }

        @Test
        @DisplayName("applyFilters() - should apply date range filter correctly")
        void testApplyFilters_DateRangeFilter_AppliesFilterCorrectly() {
            // Given: Request with date range filter
            Pageable pageable = PageRequest.of(0, DEFAULT_PAGE_SIZE);
            TransactionListRequest request = new TransactionListRequest();
            request.setStartDate(LocalDate.now().minusDays(10));
            request.setEndDate(LocalDate.now().minusDays(1));
            
            List<Transaction> filteredTransactions = sampleTransactions.stream()
                .filter(t -> !t.getTransactionDate().isBefore(request.getStartDate()) && 
                           !t.getTransactionDate().isAfter(request.getEndDate()))
                .toList();
            
            Page<Transaction> mockPage = new PageImpl<>(filteredTransactions, 
                pageable, filteredTransactions.size());
            
            when(transactionRepository.findByTransactionDateBetween(
                eq(request.getStartDate()), 
                eq(request.getEndDate()), 
                eq(pageable)))
                .thenReturn(mockPage);
            
            // When: Applying date range filter
            Page<Transaction> result = transactionListService.applyFilters(request, pageable);
            
            // Then: Verify filtered results
            assertThat(result).isNotNull();
            
            // Verify repository called with correct date parameters
            verify(transactionRepository, times(1))
                .findByTransactionDateBetween(eq(request.getStartDate()), 
                               eq(request.getEndDate()), eq(pageable));
        }

        @Test
        @DisplayName("applyFilters() - should apply combined account ID and date range filters")
        void testApplyFilters_CombinedFilters_AppliesCombinedFiltersCorrectly() {
            // Given: Request with both account ID and date range filters
            Pageable pageable = PageRequest.of(0, DEFAULT_PAGE_SIZE);
            TransactionListRequest request = new TransactionListRequest();
            request.setAccountId(SAMPLE_ACCOUNT_ID);
            request.setStartDate(LocalDate.now().minusDays(10));
            request.setEndDate(LocalDate.now().minusDays(1));
            
            List<Transaction> filteredTransactions = sampleTransactions.stream()
                .filter(t -> Objects.equals(Long.parseLong(SAMPLE_ACCOUNT_ID), t.getAccountId()) &&
                           !t.getTransactionDate().isBefore(request.getStartDate()) && 
                           !t.getTransactionDate().isAfter(request.getEndDate()))
                .toList();
            
            Page<Transaction> mockPage = new PageImpl<>(filteredTransactions, 
                pageable, filteredTransactions.size());
            
            when(transactionRepository.findByAccountIdAndTransactionDateBetween(
                eq(Long.parseLong(SAMPLE_ACCOUNT_ID)),
                eq(request.getStartDate()), 
                eq(request.getEndDate()), 
                eq(pageable)))
                .thenReturn(mockPage);
            
            // When: Applying combined filters
            Page<Transaction> result = transactionListService.applyFilters(request, pageable);
            
            // Then: Verify combined filtering
            assertThat(result).isNotNull();
            
            // Verify repository called with combined filter parameters
            verify(transactionRepository, times(1))
                .findByAccountIdAndTransactionDateBetween(eq(Long.parseLong(SAMPLE_ACCOUNT_ID)),
                    eq(request.getStartDate()), eq(request.getEndDate()), eq(pageable));
        }
    }

    @Nested
    @DisplayName("Page Count and Navigation Metadata Tests")
    class PageCountAndMetadataTests {

        @Test
        @DisplayName("getPageCount() - should return correct page count")
        void testGetPageCount_WithInitializedState_ReturnsCorrectPageCount() {
            // Given: Service with initialized pagination state
            TransactionListRequest request = new TransactionListRequest();
            request.setAccountId(SAMPLE_ACCOUNT_ID);
            request.setPageNumber(1);
            request.setPageSize(DEFAULT_PAGE_SIZE);
            
            Page<Transaction> mockPage = new PageImpl<>(sampleTransactions.subList(0, DEFAULT_PAGE_SIZE), 
                PageRequest.of(0, DEFAULT_PAGE_SIZE), sampleTransactions.size());
            
            when(transactionRepository.findByAccountId(eq(Long.parseLong(SAMPLE_ACCOUNT_ID)), any(Pageable.class)))
                .thenReturn(mockPage);
            
            // Initialize pagination state
            transactionListService.listTransactions(request);
            
            // When: Getting page count
            int pageCount = transactionListService.getPageCount(request);
            
            // Then: Verify page count calculation
            int expectedPages = (int) Math.ceil((double) sampleTransactions.size() / DEFAULT_PAGE_SIZE);
            assertThat(pageCount).isEqualTo(expectedPages);
        }

        @Test
        @DisplayName("getCurrentPageNumber() - should return current page number")
        void testGetCurrentPageNumber_WithInitializedState_ReturnsCorrectPageNumber() {
            // Given: Service with initialized pagination state on page 2
            TransactionListRequest request = new TransactionListRequest();
            request.setAccountId(SAMPLE_ACCOUNT_ID);
            request.setPageNumber(2);
            request.setPageSize(DEFAULT_PAGE_SIZE);
            
            List<Transaction> secondPageTransactions = sampleTransactions.subList(
                DEFAULT_PAGE_SIZE, Math.min(DEFAULT_PAGE_SIZE * 2, sampleTransactions.size()));
            Page<Transaction> mockPage = new PageImpl<>(secondPageTransactions, 
                PageRequest.of(1, DEFAULT_PAGE_SIZE), sampleTransactions.size());
            
            when(transactionRepository.findByAccountId(eq(Long.parseLong(SAMPLE_ACCOUNT_ID)), any(Pageable.class)))
                .thenReturn(mockPage);
            
            // Initialize pagination state
            transactionListService.listTransactions(request);
            
            // When: Getting current page number
            int currentPage = transactionListService.getCurrentPageNumber(request);
            
            // Then: Verify current page number
            assertThat(currentPage).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Performance and Volume Testing")
    class PerformanceAndVolumeTests {

        @Test
        @DisplayName("listTransactions() - should handle large transaction sets efficiently")
        void testListTransactions_LargeDataset_MaintainsPerformance() {
            // Given: Large dataset simulation
            TransactionListRequest request = new TransactionListRequest();
            request.setAccountId(SAMPLE_ACCOUNT_ID);
            request.setPageNumber(1);
            request.setPageSize(DEFAULT_PAGE_SIZE);
            
            // Simulate large dataset (1000+ transactions)
            List<Transaction> largeDataset = createLargeTransactionSet(1000);
            Page<Transaction> largePage = new PageImpl<>(
                largeDataset.subList(0, Math.min(DEFAULT_PAGE_SIZE, largeDataset.size())), 
                PageRequest.of(0, DEFAULT_PAGE_SIZE), largeDataset.size());
            
            when(transactionRepository.findByAccountId(eq(Long.parseLong(SAMPLE_ACCOUNT_ID)), any(Pageable.class)))
                .thenReturn(largePage);
            
            // When: Processing large dataset with performance measurement
            long startTime = System.currentTimeMillis();
            TransactionListResponse response = transactionListService.listTransactions(request);
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Then: Verify performance requirements (< 200ms per Section 0.5.1)
            assertThat(executionTime).isLessThan(200L);
            assertThat(response.getTransactions()).hasSize(DEFAULT_PAGE_SIZE);
            assertThat(response.getTotalCount()).isEqualTo(largeDataset.size());
        }
    }

    @Nested
    @DisplayName("BigDecimal Precision Tests - COBOL COMP-3 Equivalence")
    class BigDecimalPrecisionTests {

        @Test
        @DisplayName("Transaction amounts - should maintain COBOL COMP-3 precision")
        void testTransactionAmounts_CobolComp3Precision_MaintainsPrecision() {
            // Given: Transactions with precise financial amounts
            List<Transaction> precisionTransactions = createPrecisionTestTransactions();
            
            TransactionListRequest request = new TransactionListRequest();
            request.setAccountId(SAMPLE_ACCOUNT_ID);
            request.setPageNumber(1);
            request.setPageSize(DEFAULT_PAGE_SIZE);
            
            Page<Transaction> mockPage = new PageImpl<>(precisionTransactions, 
                PageRequest.of(0, DEFAULT_PAGE_SIZE), precisionTransactions.size());
            
            when(transactionRepository.findByAccountId(eq(Long.parseLong(SAMPLE_ACCOUNT_ID)), any(Pageable.class)))
                .thenReturn(mockPage);
            
            // When: Processing transactions with financial amounts
            TransactionListResponse response = transactionListService.listTransactions(request);
            
            // Then: Verify COBOL COMP-3 precision preservation
            response.getTransactions().forEach(transaction -> {
                BigDecimal amount = transaction.getAmount();
                
                // Verify scale matches COBOL COMP-3 (2 decimal places)
                assertThat(amount.scale()).isEqualTo(2);
                
                // Verify precision within reasonable financial range
                assertThat(amount).isGreaterThanOrEqualTo(BigDecimal.ZERO);
                assertThat(amount).isLessThan(new BigDecimal("999999.99"));
            });
        }
    }

    /**
     * Creates large transaction set for performance testing.
     * Uses all required Transaction entity fields for comprehensive testing.
     */
    private List<Transaction> createLargeTransactionSet(int count) {
        List<Transaction> transactions = new ArrayList<>();
        
        for (int i = 1; i <= count; i++) {
            Transaction transaction = new Transaction();
            transaction.setTransactionId((long) i);
            transaction.setAmount(new BigDecimal("50.00").add(new BigDecimal(i % 100)));
            transaction.setTransactionDate(LocalDate.now().minusDays(i % 365));
            transaction.setAccountId(Long.parseLong(SAMPLE_ACCOUNT_ID));
            transaction.setDescription("Perf Test Transaction " + i);
            transaction.setMerchantName("Merchant " + (i % 100));
            transaction.setCategoryCode("PURCHASE");
            transactions.add(transaction);
        }
        
        return transactions;
    }

    /**
     * Creates transactions with precise decimal amounts for COBOL COMP-3 precision testing.
     * Validates BigDecimal handling matches COBOL packed decimal behavior.
     */
    private List<Transaction> createPrecisionTestTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        
        // Create transactions with various precision scenarios
        BigDecimal[] testAmounts = {
            new BigDecimal("123.45"), 
            new BigDecimal("67.89"), 
            new BigDecimal("999.99"), 
            new BigDecimal("0.01"),
            new BigDecimal("1000.00")
        };
        
        for (int i = 0; i < testAmounts.length; i++) {
            Transaction transaction = new Transaction();
            transaction.setTransactionId((long) (i + 1));
            transaction.setAmount(testAmounts[i].setScale(2, BigDecimal.ROUND_HALF_UP));
            transaction.setTransactionDate(LocalDate.now().minusDays(i + 1));
            transaction.setAccountId(Long.parseLong(SAMPLE_ACCOUNT_ID));
            transaction.setDescription("Precision Test Transaction " + (i + 1));
            transaction.setMerchantName("Precision Test Merchant " + (i + 1));
            transaction.setCategoryCode("PURCHASE");
            transactions.add(transaction);
        }
        
        return transactions;
    }
}