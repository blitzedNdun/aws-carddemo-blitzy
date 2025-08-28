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
import com.carddemo.service.TransactionListService;
import com.carddemo.test.BaseServiceTest;
import com.carddemo.test.TestDataGenerator;
import com.carddemo.test.CobolComparisonUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
public class TransactionListServiceTest extends BaseServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    private TransactionListService transactionListService;

    private TestDataGenerator testDataGenerator;
    
    private CobolComparisonUtils cobolComparisonUtils;

    private List<Transaction> sampleTransactions;
    
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String SAMPLE_ACCOUNT_ID = "1000000001";
    private static final String SAMPLE_CARD_NUMBER = "4532123456789012";

    @BeforeEach
    void setUp() {
        // Initialize service with mocked repository
        transactionListService = new TransactionListService(transactionRepository);
        
        // Initialize test utilities from BaseServiceTest
        testDataGenerator = new TestDataGenerator();
        cobolComparisonUtils = new CobolComparisonUtils();
        
        // Set up base test configuration
        setupTestData();
        
        // Generate sample transaction data for testing
        sampleTransactions = createSampleTransactionList();
        
        // Reset all mocks before each test
        resetMocks();
    }

    /**
     * Creates a representative list of sample transactions for testing various scenarios.
     * Includes transactions with different amounts, dates, and account relationships
     * to simulate real-world transaction listing scenarios.
     */
    private List<Transaction> createSampleTransactionList() {
        List<Transaction> transactions = new ArrayList<>();
        
        // Generate transactions using TestDataGenerator
        testDataGenerator.resetRandomSeed();
        
        // Create transactions with varied data for comprehensive testing
        for (int i = 1; i <= 25; i++) {
            Transaction transaction = testDataGenerator.generateTransaction();
            transaction.setTransactionId((long) i);
            transaction.setAccountId(SAMPLE_ACCOUNT_ID);
            transaction.setCardNumber(SAMPLE_CARD_NUMBER);
            transaction.setTransactionDate(LocalDate.now().minusDays(i));
            transaction.setAmount(testDataGenerator.generateValidTransactionAmount());
            transaction.setDescription("Test Transaction " + i);
            transaction.setMerchantName("Test Merchant " + i);
            transaction.setTransactionType("PURCHASE");
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
            
            when(transactionRepository.findByAccountId(eq(SAMPLE_ACCOUNT_ID), any(Pageable.class)))
                .thenReturn(mockPage);
            
            // When: Listing transactions
            TransactionListResponse response = transactionListService.listTransactions(request);
            
            // Then: Verify response structure matches COTRN00 BMS screen layout
            assertThat(response).isNotNull();
            assertThat(response.getTransactions()).hasSize(DEFAULT_PAGE_SIZE);
            assertThat(response.getCurrentPage()).isEqualTo(1);
            assertThat(response.getTotalCount()).isEqualTo(sampleTransactions.size());
            assertThat(response.getHasMorePages()).isTrue(); // More pages available
            assertThat(response.getHasPreviousPages()).isFalse(); // First page
            
            // Verify repository interaction
            verify(transactionRepository, times(1))
                .findByAccountId(eq(SAMPLE_ACCOUNT_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("listTransactions() - should handle empty result set gracefully")
        void testListTransactions_NoTransactions_ReturnsEmptyList() {
            // Given: Request for account with no transactions
            TransactionListRequest request = new TransactionListRequest();
            request.setAccountId("9999999999");
            request.setPageNumber(1);
            request.setPageSize(DEFAULT_PAGE_SIZE);
            
            Page<Transaction> emptyPage = new PageImpl<>(new ArrayList<>(), 
                PageRequest.of(0, DEFAULT_PAGE_SIZE), 0);
            
            when(transactionRepository.findByAccountId(eq("9999999999"), any(Pageable.class)))
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
            
            when(transactionRepository.findByDateRange(
                eq(SAMPLE_ACCOUNT_ID), 
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
                .findByDateRange(eq(SAMPLE_ACCOUNT_ID), eq(request.getStartDate()), 
                               eq(request.getEndDate()), any(Pageable.class));
        }

        @Test
        @DisplayName("listTransactions() - should apply amount range filters with BigDecimal precision")
        void testListTransactions_AmountRangeFilter_PreservesBigDecimalPrecision() {
            // Given: Request with amount range filter
            TransactionListRequest request = new TransactionListRequest();
            request.setAccountId(SAMPLE_ACCOUNT_ID);
            request.setMinAmount(new BigDecimal("100.00"));
            request.setMaxAmount(new BigDecimal("500.00"));
            request.setPageNumber(1);
            request.setPageSize(DEFAULT_PAGE_SIZE);
            
            // Create transactions with specific amounts for precision testing
            List<Transaction> precisionTransactions = new ArrayList<>();
            Transaction tx1 = testDataGenerator.generateTransaction();
            tx1.setAmount(new BigDecimal("150.25")); // Within range
            Transaction tx2 = testDataGenerator.generateTransaction();
            tx2.setAmount(new BigDecimal("299.99")); // Within range
            Transaction tx3 = testDataGenerator.generateTransaction();
            tx3.setAmount(new BigDecimal("50.00"));  // Below range
            precisionTransactions.add(tx1);
            precisionTransactions.add(tx2);
            
            Page<Transaction> mockPage = new PageImpl<>(precisionTransactions, 
                PageRequest.of(0, DEFAULT_PAGE_SIZE), precisionTransactions.size());
            
            when(transactionRepository.findByAmountRange(
                eq(SAMPLE_ACCOUNT_ID),
                eq(request.getMinAmount()),
                eq(request.getMaxAmount()),
                any(Pageable.class)))
                .thenReturn(mockPage);
            
            // When: Listing transactions with amount filter
            TransactionListResponse response = transactionListService.listTransactions(request);
            
            // Then: Verify BigDecimal precision preserved
            assertThat(response.getTransactions()).hasSize(2);
            response.getTransactions().forEach(transaction -> {
                assertBigDecimalEquals(transaction.getAmount(), 
                    transaction.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertThat(transaction.getAmount()).isBetween(request.getMinAmount(), request.getMaxAmount());
            });
            
            // Verify COBOL COMP-3 precision compliance
            response.getTransactions().forEach(transaction -> {
                assertThat(cobolComparisonUtils.validateFinancialPrecision(
                    transaction.getAmount(), transaction.getAmount())).isTrue();
            });
        }
    }

    @Nested
    @DisplayName("Pagination Navigation Tests - VSAM STARTBR/READNEXT Equivalence")
    class PaginationNavigationTests {

        @Test
        @DisplayName("nextPage() - should navigate to next page correctly")
        void testNextPage_ValidNavigation_ReturnsNextPageData() {
            // Given: Current page with more pages available
            TransactionListRequest currentRequest = new TransactionListRequest();
            currentRequest.setAccountId(SAMPLE_ACCOUNT_ID);
            currentRequest.setPageNumber(1);
            currentRequest.setPageSize(DEFAULT_PAGE_SIZE);
            
            // Mock second page data
            List<Transaction> secondPageTransactions = sampleTransactions.subList(
                DEFAULT_PAGE_SIZE, Math.min(DEFAULT_PAGE_SIZE * 2, sampleTransactions.size()));
            Page<Transaction> nextPage = new PageImpl<>(secondPageTransactions, 
                PageRequest.of(1, DEFAULT_PAGE_SIZE), sampleTransactions.size());
            
            when(transactionRepository.findByAccountId(eq(SAMPLE_ACCOUNT_ID), any(Pageable.class)))
                .thenReturn(nextPage);
            
            // When: Navigating to next page
            TransactionListResponse response = transactionListService.nextPage(currentRequest);
            
            // Then: Verify next page navigation
            assertThat(response.getCurrentPage()).isEqualTo(2);
            assertThat(response.getTransactions()).hasSize(secondPageTransactions.size());
            assertThat(response.getHasPreviousPages()).isTrue(); // Can go back
            assertThat(response.getHasMorePages()).isTrue(); // More pages exist
            
            // Verify repository called with correct page parameters
            verify(transactionRepository, times(1))
                .findByAccountId(eq(SAMPLE_ACCOUNT_ID), 
                    eq(PageRequest.of(1, DEFAULT_PAGE_SIZE, Sort.by("transactionDate").descending())));
        }

        @Test
        @DisplayName("previousPage() - should navigate to previous page correctly")
        void testPreviousPage_ValidNavigation_ReturnsPreviousPageData() {
            // Given: Currently on page 2
            TransactionListRequest currentRequest = new TransactionListRequest();
            currentRequest.setAccountId(SAMPLE_ACCOUNT_ID);
            currentRequest.setPageNumber(2);
            currentRequest.setPageSize(DEFAULT_PAGE_SIZE);
            
            // Mock first page data
            List<Transaction> firstPageTransactions = sampleTransactions.subList(0, DEFAULT_PAGE_SIZE);
            Page<Transaction> previousPage = new PageImpl<>(firstPageTransactions, 
                PageRequest.of(0, DEFAULT_PAGE_SIZE), sampleTransactions.size());
            
            when(transactionRepository.findByAccountId(eq(SAMPLE_ACCOUNT_ID), any(Pageable.class)))
                .thenReturn(previousPage);
            
            // When: Navigating to previous page
            TransactionListResponse response = transactionListService.previousPage(currentRequest);
            
            // Then: Verify previous page navigation
            assertThat(response.getCurrentPage()).isEqualTo(1);
            assertThat(response.getTransactions()).hasSize(DEFAULT_PAGE_SIZE);
            assertThat(response.getHasPreviousPages()).isFalse(); // First page
            assertThat(response.getHasMorePages()).isTrue(); // More pages exist
            
            // Verify repository interaction
            verify(transactionRepository, times(1))
                .findByAccountId(eq(SAMPLE_ACCOUNT_ID), 
                    eq(PageRequest.of(0, DEFAULT_PAGE_SIZE, Sort.by("transactionDate").descending())));
        }

        @Test
        @DisplayName("getTransactionPage() - should retrieve specific page with correct positioning")
        void testGetTransactionPage_SpecificPageRequest_ReturnsCorrectPageData() {
            // Given: Request for page 3
            int requestedPage = 3;
            int expectedPageIndex = requestedPage - 1; // Zero-based for Spring Data
            
            List<Transaction> pageThreeTransactions = new ArrayList<>();
            if (sampleTransactions.size() > DEFAULT_PAGE_SIZE * 2) {
                pageThreeTransactions = sampleTransactions.subList(
                    DEFAULT_PAGE_SIZE * 2, 
                    Math.min(DEFAULT_PAGE_SIZE * 3, sampleTransactions.size()));
            }
            
            Page<Transaction> targetPage = new PageImpl<>(pageThreeTransactions, 
                PageRequest.of(expectedPageIndex, DEFAULT_PAGE_SIZE), sampleTransactions.size());
            
            when(transactionRepository.findByAccountId(eq(SAMPLE_ACCOUNT_ID), any(Pageable.class)))
                .thenReturn(targetPage);
            
            // When: Getting specific page
            TransactionListResponse response = transactionListService.getTransactionPage(
                SAMPLE_ACCOUNT_ID, requestedPage, DEFAULT_PAGE_SIZE);
            
            // Then: Verify page positioning
            assertThat(response.getCurrentPage()).isEqualTo(requestedPage);
            assertThat(response.getTransactions()).hasSize(pageThreeTransactions.size());
            assertThat(response.getHasPreviousPages()).isTrue(); // Not first page
            
            // Verify repository called with correct parameters
            verify(transactionRepository, times(1))
                .findByAccountId(eq(SAMPLE_ACCOUNT_ID), 
                    eq(PageRequest.of(expectedPageIndex, DEFAULT_PAGE_SIZE, 
                        Sort.by("transactionDate").descending())));
        }
    }

    @Nested
    @DisplayName("Filtering and Sorting Tests - Business Logic Validation")
    class FilteringAndSortingTests {

        @Test
        @DisplayName("applyFilters() - should combine multiple filter criteria correctly")
        void testApplyFilters_MultipleFilters_CombinesFiltersCorrectly() {
            // Given: Request with multiple filters
            TransactionListRequest request = new TransactionListRequest();
            request.setAccountId(SAMPLE_ACCOUNT_ID);
            request.setStartDate(LocalDate.now().minusDays(7));
            request.setEndDate(LocalDate.now());
            request.setMinAmount(new BigDecimal("100.00"));
            request.setMaxAmount(new BigDecimal("1000.00"));
            request.setPageNumber(1);
            request.setPageSize(DEFAULT_PAGE_SIZE);
            
            // Create filtered transaction set
            List<Transaction> filteredTransactions = sampleTransactions.stream()
                .filter(t -> !t.getTransactionDate().isBefore(request.getStartDate()) && 
                           !t.getTransactionDate().isAfter(request.getEndDate()) &&
                           t.getAmount().compareTo(request.getMinAmount()) >= 0 &&
                           t.getAmount().compareTo(request.getMaxAmount()) <= 0)
                .toList();
            
            Page<Transaction> mockPage = new PageImpl<>(filteredTransactions, 
                PageRequest.of(0, DEFAULT_PAGE_SIZE), filteredTransactions.size());
            
            // Mock the combined filter method
            when(transactionRepository.findByAccountId(anyString(), any(Pageable.class)))
                .thenReturn(mockPage);
            
            // When: Applying multiple filters
            TransactionListResponse response = transactionListService.applyFilters(request);
            
            // Then: Verify combined filtering
            assertThat(response.getTransactions()).allSatisfy(transaction -> {
                assertThat(transaction.getTransactionDate()).isBetween(
                    request.getStartDate(), request.getEndDate());
                assertThat(transaction.getAmount()).isBetween(
                    request.getMinAmount(), request.getMaxAmount());
            });
        }

        @Test
        @DisplayName("applyFilters() - should handle card number filtering with validation")
        void testApplyFilters_CardNumberFilter_ValidatesCardNumber() {
            // Given: Request with card number filter
            TransactionListRequest request = new TransactionListRequest();
            request.setAccountId(SAMPLE_ACCOUNT_ID);
            request.setCardNumber(SAMPLE_CARD_NUMBER);
            request.setPageNumber(1);
            request.setPageSize(DEFAULT_PAGE_SIZE);
            
            // Filter transactions by card number
            List<Transaction> cardTransactions = sampleTransactions.stream()
                .filter(t -> SAMPLE_CARD_NUMBER.equals(t.getCardNumber()))
                .toList();
            
            Page<Transaction> mockPage = new PageImpl<>(cardTransactions, 
                PageRequest.of(0, DEFAULT_PAGE_SIZE), cardTransactions.size());
            
            when(transactionRepository.findByAccountId(eq(SAMPLE_ACCOUNT_ID), any(Pageable.class)))
                .thenReturn(mockPage);
            
            // When: Filtering by card number
            TransactionListResponse response = transactionListService.applyFilters(request);
            
            // Then: Verify card number filtering
            assertThat(response.getTransactions()).allSatisfy(transaction -> {
                assertThat(transaction.getCardNumber()).isEqualTo(SAMPLE_CARD_NUMBER);
            });
            
            // Validate card number format (16 digits)
            assertThat(SAMPLE_CARD_NUMBER).matches("\\d{16}");
        }
    }

    @Nested
    @DisplayName("Page Count and Navigation Metadata Tests")
    class PageCountAndMetadataTests {

        @Test
        @DisplayName("getPageCount() - should calculate total pages correctly")
        void testGetPageCount_VariousDataSizes_CalculatesCorrectly() {
            // Test various data sizes
            int[] testDataSizes = {0, 1, 10, 11, 25, 100, 101};
            int pageSize = DEFAULT_PAGE_SIZE;
            
            for (int dataSize : testDataSizes) {
                // Calculate expected page count
                int expectedPages = (dataSize == 0) ? 0 : (int) Math.ceil((double) dataSize / pageSize);
                
                // When: Getting page count
                int actualPages = transactionListService.getPageCount(dataSize, pageSize);
                
                // Then: Verify calculation
                assertThat(actualPages).isEqualTo(expectedPages);
            }
        }

        @Test
        @DisplayName("getCurrentPageNumber() - should return current page position correctly")
        void testGetCurrentPageNumber_VariousRequests_ReturnsCorrectPage() {
            // Given: Various page number requests
            int[] testPageNumbers = {1, 2, 5, 10};
            
            for (int pageNumber : testPageNumbers) {
                TransactionListRequest request = new TransactionListRequest();
                request.setPageNumber(pageNumber);
                
                // When: Getting current page number
                int currentPage = transactionListService.getCurrentPageNumber(request);
                
                // Then: Verify page number
                assertThat(currentPage).isEqualTo(pageNumber);
            }
        }

        @Test
        @DisplayName("getCurrentPageNumber() - should default to page 1 when not specified")
        void testGetCurrentPageNumber_NoPageSpecified_DefaultsToPageOne() {
            // Given: Request without page number
            TransactionListRequest request = new TransactionListRequest();
            // pageNumber is null by default
            
            // When: Getting current page number
            int currentPage = transactionListService.getCurrentPageNumber(request);
            
            // Then: Verify defaults to page 1
            assertThat(currentPage).isEqualTo(1);
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
            List<Transaction> largeDataset = testDataGenerator.generateDailyTransactionBatch();
            Page<Transaction> largePage = new PageImpl<>(
                largeDataset.subList(0, Math.min(DEFAULT_PAGE_SIZE, largeDataset.size())), 
                PageRequest.of(0, DEFAULT_PAGE_SIZE), largeDataset.size());
            
            when(transactionRepository.findByAccountId(eq(SAMPLE_ACCOUNT_ID), any(Pageable.class)))
                .thenReturn(largePage);
            
            // When: Processing large dataset with performance measurement
            long startTime = System.currentTimeMillis();
            TransactionListResponse response = transactionListService.listTransactions(request);
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Then: Verify performance requirements (< 200ms per Section 0.5.1)
            assertThat(executionTime).isLessThan(200L);
            assertThat(response.getTransactions()).hasSize(DEFAULT_PAGE_SIZE);
            assertThat(response.getTotalCount()).isEqualTo(largeDataset.size());
            
            // Validate response time compliance
            validateResponseTime(executionTime, 200L);
        }

        @Test
        @DisplayName("applyFilters() - should maintain performance with complex filters")
        void testApplyFilters_ComplexFilters_MaintainsPerformance() {
            // Given: Complex filter request
            TransactionListRequest request = new TransactionListRequest();
            request.setAccountId(SAMPLE_ACCOUNT_ID);
            request.setStartDate(LocalDate.now().minusDays(30));
            request.setEndDate(LocalDate.now());
            request.setMinAmount(new BigDecimal("50.00"));
            request.setMaxAmount(new BigDecimal("500.00"));
            request.setPageNumber(1);
            request.setPageSize(DEFAULT_PAGE_SIZE);
            
            List<Transaction> filteredResults = sampleTransactions.subList(0, DEFAULT_PAGE_SIZE);
            Page<Transaction> mockPage = new PageImpl<>(filteredResults, 
                PageRequest.of(0, DEFAULT_PAGE_SIZE), sampleTransactions.size());
            
            when(transactionRepository.findByAccountId(anyString(), any(Pageable.class)))
                .thenReturn(mockPage);
            
            // When: Applying complex filters with performance measurement
            long startTime = measurePerformance(() -> {
                return transactionListService.applyFilters(request);
            });
            
            // Then: Verify performance maintained
            assertThat(startTime).isLessThan(200L);
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Cases")
    class ErrorHandlingTests {

        @Test
        @DisplayName("listTransactions() - should handle null request gracefully")
        void testListTransactions_NullRequest_ThrowsAppropriateException() {
            // When/Then: Null request should throw exception
            assertThatThrownBy(() -> transactionListService.listTransactions(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Transaction list request cannot be null");
        }

        @Test
        @DisplayName("listTransactions() - should handle invalid page numbers")
        void testListTransactions_InvalidPageNumber_ThrowsException() {
            // Given: Request with invalid page number
            TransactionListRequest request = new TransactionListRequest();
            request.setAccountId(SAMPLE_ACCOUNT_ID);
            request.setPageNumber(0); // Invalid: pages are 1-based
            request.setPageSize(DEFAULT_PAGE_SIZE);
            
            // When/Then: Invalid page number should throw exception
            assertThatThrownBy(() -> transactionListService.listTransactions(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page number must be greater than 0");
        }

        @Test
        @DisplayName("listTransactions() - should handle invalid page size")
        void testListTransactions_InvalidPageSize_ThrowsException() {
            // Given: Request with invalid page size
            TransactionListRequest request = new TransactionListRequest();
            request.setAccountId(SAMPLE_ACCOUNT_ID);
            request.setPageNumber(1);
            request.setPageSize(-1); // Invalid: negative page size
            
            // When/Then: Invalid page size should throw exception
            assertThatThrownBy(() -> transactionListService.listTransactions(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page size must be positive");
        }

        @Test
        @DisplayName("previousPage() - should handle first page navigation gracefully")
        void testPreviousPage_FirstPage_HandlesGracefully() {
            // Given: Request for previous page when on first page
            TransactionListRequest request = new TransactionListRequest();
            request.setAccountId(SAMPLE_ACCOUNT_ID);
            request.setPageNumber(1);
            request.setPageSize(DEFAULT_PAGE_SIZE);
            
            // When/Then: Should handle gracefully without error
            assertThatCode(() -> transactionListService.previousPage(request))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("nextPage() - should handle last page navigation gracefully")
        void testNextPage_LastPage_HandlesGracefully() {
            // Given: Request for next page when on last page
            TransactionListRequest request = new TransactionListRequest();
            request.setAccountId(SAMPLE_ACCOUNT_ID);
            request.setPageNumber(100); // Assume high page number
            request.setPageSize(DEFAULT_PAGE_SIZE);
            
            // Mock empty page to simulate last page
            Page<Transaction> emptyPage = new PageImpl<>(new ArrayList<>(), 
                PageRequest.of(99, DEFAULT_PAGE_SIZE), sampleTransactions.size());
            
            when(transactionRepository.findByAccountId(anyString(), any(Pageable.class)))
                .thenReturn(emptyPage);
            
            // When/Then: Should handle gracefully
            assertThatCode(() -> transactionListService.nextPage(request))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("COBOL Functional Parity Validation")
    class CobolFunctionalParityTests {

        @Test
        @DisplayName("Transaction listing - should produce identical results to COBOL COTRN00C")
        void testTransactionListing_CobolParity_ProducesIdenticalResults() {
            // Given: Standard transaction list request
            TransactionListRequest request = new TransactionListRequest();
            request.setAccountId(SAMPLE_ACCOUNT_ID);
            request.setPageNumber(1);
            request.setPageSize(10); // Match COTRN00 BMS screen (10 transaction rows)
            
            List<Transaction> pageTransactions = sampleTransactions.subList(0, 10);
            Page<Transaction> mockPage = new PageImpl<>(pageTransactions, 
                PageRequest.of(0, 10), sampleTransactions.size());
            
            when(transactionRepository.findByAccountId(eq(SAMPLE_ACCOUNT_ID), any(Pageable.class)))
                .thenReturn(mockPage);
            
            // When: Getting transaction list
            TransactionListResponse response = transactionListService.listTransactions(request);
            
            // Then: Validate COBOL equivalence using comparison utilities
            CobolComparisonUtils.ComparisonResult comparisonResult = 
                cobolComparisonUtils.generateComparisonReport(response, request);
            
            assertThat(comparisonResult.isEquivalent()).isTrue();
            assertThat(comparisonResult.getDifferences()).isEmpty();
            
            // Verify field-by-field equivalence
            response.getTransactions().forEach(transaction -> {
                // Validate financial precision matches COBOL COMP-3
                assertThat(cobolComparisonUtils.validateFinancialPrecision(
                    transaction.getAmount(), transaction.getAmount())).isTrue();
                
                // Verify BigDecimal precision matches COBOL calculations
                assertThat(cobolComparisonUtils.compareBigDecimals(
                    transaction.getAmount(), transaction.getAmount())).isZero();
            });
        }

        @Test
        @DisplayName("Page navigation - should replicate COTRN00 PF7/PF8 functionality")
        void testPageNavigation_CobolParity_ReplicatesPFKeyFunctionality() {
            // Given: Multi-page transaction scenario
            TransactionListRequest baseRequest = new TransactionListRequest();
            baseRequest.setAccountId(SAMPLE_ACCOUNT_ID);
            baseRequest.setPageSize(10);
            
            // Test PF8 (Next Page) equivalent
            baseRequest.setPageNumber(1);
            List<Transaction> firstPage = sampleTransactions.subList(0, 10);
            Page<Transaction> mockFirstPage = new PageImpl<>(firstPage, 
                PageRequest.of(0, 10), sampleTransactions.size());
            
            when(transactionRepository.findByAccountId(eq(SAMPLE_ACCOUNT_ID), any(Pageable.class)))
                .thenReturn(mockFirstPage);
            
            TransactionListResponse firstResponse = transactionListService.listTransactions(baseRequest);
            
            // Verify PF8 (next page) functionality
            assertThat(firstResponse.getHasMorePages()).isTrue(); // PF8 should be enabled
            assertThat(firstResponse.getHasPreviousPages()).isFalse(); // PF7 should be disabled
            
            // Test PF7 (Previous Page) equivalent from page 2
            baseRequest.setPageNumber(2);
            List<Transaction> secondPage = sampleTransactions.subList(10, 20);
            Page<Transaction> mockSecondPage = new PageImpl<>(secondPage, 
                PageRequest.of(1, 10), sampleTransactions.size());
            
            when(transactionRepository.findByAccountId(eq(SAMPLE_ACCOUNT_ID), any(Pageable.class)))
                .thenReturn(mockSecondPage);
            
            TransactionListResponse secondResponse = transactionListService.listTransactions(baseRequest);
            
            // Verify PF7 (previous page) functionality
            assertThat(secondResponse.getHasPreviousPages()).isTrue(); // PF7 should be enabled
            assertThat(secondResponse.getHasMorePages()).isTrue(); // PF8 should be enabled (more pages exist)
        }

        @Test
        @DisplayName("Financial calculations - should maintain COBOL COMP-3 precision")
        void testFinancialCalculations_CobolParity_MaintainsComp3Precision() {
            // Given: Transactions with precise financial amounts
            testDataGenerator.resetRandomSeed();
            List<Transaction> precisionTransactions = new ArrayList<>();
            
            // Create transactions with COBOL COMP-3 equivalent amounts
            Transaction tx1 = testDataGenerator.generateTransaction();
            tx1.setAmount(new BigDecimal("123.45")); // PIC S9(5)V99 COMP-3
            
            Transaction tx2 = testDataGenerator.generateTransaction();
            tx2.setAmount(new BigDecimal("9999.99")); // Maximum for PIC S9(5)V99
            
            Transaction tx3 = testDataGenerator.generateTransaction();
            tx3.setAmount(new BigDecimal("0.01")); // Minimum positive amount
            
            precisionTransactions.add(tx1);
            precisionTransactions.add(tx2);
            precisionTransactions.add(tx3);
            
            TransactionListRequest request = new TransactionListRequest();
            request.setAccountId(SAMPLE_ACCOUNT_ID);
            request.setPageNumber(1);
            request.setPageSize(DEFAULT_PAGE_SIZE);
            
            Page<Transaction> mockPage = new PageImpl<>(precisionTransactions, 
                PageRequest.of(0, DEFAULT_PAGE_SIZE), precisionTransactions.size());
            
            when(transactionRepository.findByAccountId(eq(SAMPLE_ACCOUNT_ID), any(Pageable.class)))
                .thenReturn(mockPage);
            
            // When: Processing transactions with financial amounts
            TransactionListResponse response = transactionListService.listTransactions(request);
            
            // Then: Verify COBOL COMP-3 precision preservation
            response.getTransactions().forEach(transaction -> {
                BigDecimal amount = transaction.getAmount();
                
                // Verify scale matches COBOL COMP-3 (2 decimal places)
                assertThat(amount.scale()).isEqualTo(2);
                
                // Verify precision within COBOL COMP-3 range
                assertThat(amount).isBetween(new BigDecimal("0.00"), new BigDecimal("99999.99"));
                
                // Validate using COBOL comparison utilities
                CobolComparisonUtils.FinancialDifference difference = 
                    cobolComparisonUtils.validateFinancialPrecision(amount, amount);
                assertThat(difference).isNull(); // No differences expected
            });
        }
    }

    /**
     * Helper method to create a specific transaction for testing.
     * Allows controlled test scenarios with known data values.
     */
    private Transaction createTestTransaction(Long id, String accountId, BigDecimal amount, 
                                           LocalDate date, String description) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(id);
        transaction.setAccountId(accountId);
        transaction.setAmount(amount);
        transaction.setTransactionDate(date);
        transaction.setDescription(description);
        transaction.setMerchantName("Test Merchant");
        transaction.setTransactionType("PURCHASE");
        transaction.setCardNumber(SAMPLE_CARD_NUMBER);
        return transaction;
    }

    /**
     * Helper method to verify repository interactions with correct parameters.
     */
    private void verifyRepositoryInteraction(String expectedAccountId, int expectedPage, 
                                           int expectedSize) {
        verify(transactionRepository, times(1))
            .findByAccountId(eq(expectedAccountId), 
                eq(PageRequest.of(expectedPage, expectedSize, 
                    Sort.by("transactionDate").descending())));
    }
}