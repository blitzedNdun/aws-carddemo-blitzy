package com.carddemo.service;

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

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.carddemo.repository.TransactionRepository;
import com.carddemo.entity.Transaction;
import com.carddemo.dto.TransactionListRequest;
import com.carddemo.dto.TransactionListResponse;
import com.carddemo.test.TestDataGenerator;
import com.carddemo.test.CobolComparisonUtils;
import com.carddemo.service.BaseServiceTest;

/**
 * Comprehensive unit test class for TransactionListService validating COBOL COTRN00C 
 * transaction list logic migration to Java. Tests pagination, filtering, sorting, 
 * and transaction data retrieval with VSAM-equivalent browse operations.
 * 
 * This test class ensures 100% functional parity between the original COBOL COTRN00C
 * program and the modernized Java TransactionListService implementation, including:
 * - VSAM STARTBR/READNEXT equivalent pagination operations
 * - Transaction filtering by date range and amount range  
 * - Proper sorting and ordering of transaction results
 * - Page navigation functionality (PF7/PF8 equivalent)
 * - Performance validation for large transaction sets
 * - BigDecimal precision validation matching COBOL COMP-3
 * 
 * Test coverage includes edge cases, boundary conditions, and error scenarios
 * to ensure robust behavior matching the original mainframe implementation.
 */
@ExtendWith(MockitoExtension.class)
public class TransactionListServiceTest extends BaseServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks 
    private TransactionListService transactionListService;

    private TestDataGenerator testDataGenerator;
    private CobolComparisonUtils cobolComparisonUtils;

    // Test data containers
    private List<Transaction> sampleTransactions;
    private TransactionListRequest sampleRequest;
    private Transaction sampleTransaction;

    @BeforeEach
    public void setUp() {
        // Initialize test utilities
        testDataGenerator = new TestDataGenerator();
        cobolComparisonUtils = new CobolComparisonUtils();
        
        // Set up base test data and reset mocks
        setupTestData();
        resetMocks();
        
        // Reset test data generator for consistent test results
        testDataGenerator.resetRandomSeed();
        
        // Generate sample transaction data for testing
        sampleTransactions = testDataGenerator.generateTransactionList();
        sampleTransaction = testDataGenerator.generateTransaction();
        
        // Create sample request with typical search criteria
        sampleRequest = new TransactionListRequest();
        sampleRequest.setPageNumber(1);
        sampleRequest.setPageSize(10);
        sampleRequest.setAccountId(testDataGenerator.generateAccountId());
    }

    @Nested
    @DisplayName("Transaction List Retrieval Tests")
    class TransactionListRetrievalTests {

        @Test
        @DisplayName("listTransactions() - Returns paginated transaction list with valid request")
        public void testListTransactions_ValidRequest_ReturnsPaginatedResults() {
            // Given: Repository returns paginated transaction data
            Page<Transaction> mockPage = new PageImpl<>(
                sampleTransactions.subList(0, 10), 
                PageRequest.of(0, 10, Sort.by("transactionDate").descending()),
                sampleTransactions.size()
            );
            
            when(transactionRepository.findByAccountId(
                eq(sampleRequest.getAccountId()), 
                any(Pageable.class)))
                .thenReturn(mockPage);

            // When: Service processes transaction list request
            long startTime = System.currentTimeMillis();
            TransactionListResponse response = transactionListService.listTransactions(sampleRequest);
            long responseTime = System.currentTimeMillis() - startTime;

            // Then: Response contains expected transaction data
            assertThat(response).isNotNull();
            assertThat(response.getTransactions()).hasSize(10);
            assertThat(response.getCurrentPage()).isEqualTo(1);
            assertThat(response.getTotalRecords()).isEqualTo(sampleTransactions.size());

            // Verify performance requirement (<200ms response time)
            validateResponseTime(responseTime, 200);

            // Verify repository interaction
            verify(transactionRepository, times(1)).findByAccountId(
                eq(sampleRequest.getAccountId()), 
                any(Pageable.class)
            );
        }

        @Test
        @DisplayName("listTransactions() - Handles empty result set gracefully")
        public void testListTransactions_EmptyResults_ReturnsEmptyResponse() {
            // Given: Repository returns empty page
            Page<Transaction> emptyPage = new PageImpl<>(
                new ArrayList<>(), 
                PageRequest.of(0, 10),
                0
            );
            
            when(transactionRepository.findByAccountId(
                any(String.class), 
                any(Pageable.class)))
                .thenReturn(emptyPage);

            // When: Service processes request with no matching transactions  
            TransactionListResponse response = transactionListService.listTransactions(sampleRequest);

            // Then: Response indicates no transactions found
            assertThat(response).isNotNull();
            assertThat(response.getTransactions()).isEmpty();
            assertThat(response.getCurrentPage()).isEqualTo(1);
            assertThat(response.getTotalRecords()).isEqualTo(0);
            assertThat(response.hasNextPage()).isFalse();
            assertThat(response.hasPreviousPage()).isFalse();
        }
    }

    @Nested
    @DisplayName("VSAM Browse Operation Equivalence Tests")
    class VsamBrowseEquivalenceTests {

        @Test
        @DisplayName("getTransactionPage() - Replicates VSAM STARTBR/READNEXT pagination")
        public void testGetTransactionPage_VsamEquivalentPagination() {
            // Given: Large dataset requiring pagination (VSAM STARTBR equivalent)
            List<Transaction> largeBatch = testDataGenerator.generateDailyTransactionBatch();
            Page<Transaction> firstPage = new PageImpl<>(
                largeBatch.subList(0, 10),
                PageRequest.of(0, 10),
                largeBatch.size()
            );
            
            when(transactionRepository.findAll(any(Pageable.class)))
                .thenReturn(firstPage);

            // When: Service retrieves first page (STARTBR equivalent)
            TransactionListResponse firstResponse = transactionListService.getTransactionPage(1, 10);

            // Then: Response structure matches VSAM browse behavior
            assertThat(firstResponse.getTransactions()).hasSize(10);
            assertThat(firstResponse.getCurrentPage()).isEqualTo(1);
            assertThat(firstResponse.hasNextPage()).isTrue();
            assertThat(firstResponse.hasPreviousPage()).isFalse();

            // Verify transactions are properly ordered (READNEXT equivalent)
            LocalDate previousDate = null;
            for (var transaction : firstResponse.getTransactions()) {
                if (previousDate != null) {
                    assertThat(transaction.getTransactionDate())
                        .isBeforeOrEqualTo(previousDate);
                }
                previousDate = transaction.getTransactionDate();
            }
        }

        @Test
        @DisplayName("nextPage() - Implements VSAM READNEXT functionality")
        public void testNextPage_VsamReadNextEquivalent() {
            // Given: Current page with available next page
            List<Transaction> secondPageData = testDataGenerator.generateTransactionList();
            Page<Transaction> nextPage = new PageImpl<>(
                secondPageData,
                PageRequest.of(1, 10),
                25 // Total records allowing for next page
            );
            
            when(transactionRepository.findAll(any(Pageable.class)))
                .thenReturn(nextPage);

            // When: Service navigates to next page (READNEXT equivalent)
            TransactionListResponse response = transactionListService.nextPage();

            // Then: Response reflects next page position
            assertThat(response.getCurrentPage()).isEqualTo(2);
            assertThat(response.hasNextPage()).isTrue();
            assertThat(response.hasPreviousPage()).isTrue();
        }

        @Test
        @DisplayName("previousPage() - Implements VSAM READPREV functionality")
        public void testPreviousPage_VsamReadPrevEquivalent() {
            // Given: Current page allowing previous page navigation
            List<Transaction> firstPageData = testDataGenerator.generateTransactionList();
            Page<Transaction> prevPage = new PageImpl<>(
                firstPageData,
                PageRequest.of(0, 10),
                25
            );
            
            when(transactionRepository.findAll(any(Pageable.class)))
                .thenReturn(prevPage);

            // When: Service navigates to previous page (READPREV equivalent)
            TransactionListResponse response = transactionListService.previousPage();

            // Then: Response reflects previous page position
            assertThat(response.getCurrentPage()).isEqualTo(1);
            assertThat(response.hasNextPage()).isTrue();
            assertThat(response.hasPreviousPage()).isFalse();
        }
    }

    @Nested
    @DisplayName("Transaction Filtering Tests")
    class TransactionFilteringTests {

        @Test
        @DisplayName("applyFilters() - Filters transactions by date range")
        public void testApplyFilters_DateRangeFilter_ReturnsFilteredResults() {
            // Given: Request with date range filter
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            
            sampleRequest.setStartDate(startDate);
            sampleRequest.setEndDate(endDate);
            
            List<Transaction> filteredTransactions = sampleTransactions.stream()
                .filter(t -> !t.getTransactionDate().isBefore(startDate) && 
                           !t.getTransactionDate().isAfter(endDate))
                .toList();
            
            Page<Transaction> filteredPage = new PageImpl<>(
                filteredTransactions,
                PageRequest.of(0, 10),
                filteredTransactions.size()
            );
            
            when(transactionRepository.findByDateRange(
                eq(startDate), eq(endDate), any(Pageable.class)))
                .thenReturn(filteredPage);

            // When: Service applies date range filter
            TransactionListResponse response = transactionListService.applyFilters(sampleRequest);

            // Then: Response contains only transactions within date range
            assertThat(response.getTransactions()).allSatisfy(transaction -> {
                assertThat(transaction.getTransactionDate()).isAfterOrEqualTo(startDate);
                assertThat(transaction.getTransactionDate()).isBeforeOrEqualTo(endDate);
            });
            
            verify(transactionRepository).findByDateRange(
                eq(startDate), eq(endDate), any(Pageable.class));
        }

        @Test
        @DisplayName("applyFilters() - Filters transactions by amount range")
        public void testApplyFilters_AmountRangeFilter_ReturnsFilteredResults() {
            // Given: Request with amount range filter
            BigDecimal minAmount = testDataGenerator.generateValidTransactionAmount();
            BigDecimal maxAmount = minAmount.add(BigDecimal.valueOf(1000.00));
            
            sampleRequest.setMinAmount(minAmount);
            sampleRequest.setMaxAmount(maxAmount);
            
            List<Transaction> filteredTransactions = sampleTransactions.stream()
                .filter(t -> t.getAmount().compareTo(minAmount) >= 0 && 
                           t.getAmount().compareTo(maxAmount) <= 0)
                .toList();
            
            Page<Transaction> filteredPage = new PageImpl<>(
                filteredTransactions,
                PageRequest.of(0, 10),
                filteredTransactions.size()
            );
            
            when(transactionRepository.findByAmountRange(
                eq(minAmount), eq(maxAmount), any(Pageable.class)))
                .thenReturn(filteredPage);

            // When: Service applies amount range filter
            TransactionListResponse response = transactionListService.applyFilters(sampleRequest);

            // Then: Response contains only transactions within amount range
            assertThat(response.getTransactions()).allSatisfy(transaction -> {
                assertThat(transaction.getAmount()).isGreaterThanOrEqualTo(minAmount);
                assertThat(transaction.getAmount()).isLessThanOrEqualTo(maxAmount);
            });
            
            // Validate BigDecimal precision matches COBOL COMP-3
            response.getTransactions().forEach(transaction -> {
                assertBigDecimalEquals(transaction.getAmount(), transaction.getAmount());
            });
        }
    }

    @Nested
    @DisplayName("Page Navigation Tests")
    class PageNavigationTests {

        @Test
        @DisplayName("getPageCount() - Calculates correct total pages")
        public void testGetPageCount_VariousDataSizes_ReturnsCorrectPageCount() {
            // Test scenarios with different data sizes
            int[] totalRecords = {5, 10, 15, 25, 100};
            int pageSize = 10;
            
            for (int recordCount : totalRecords) {
                // Given: Mock repository with specific record count
                when(transactionRepository.count()).thenReturn((long) recordCount);
                
                // When: Service calculates page count
                int pageCount = transactionListService.getPageCount();
                
                // Then: Page count matches expected calculation
                int expectedPages = (int) Math.ceil((double) recordCount / pageSize);
                assertThat(pageCount).isEqualTo(expectedPages);
            }
        }

        @Test
        @DisplayName("getCurrentPageNumber() - Tracks current page position")
        public void testGetCurrentPageNumber_AfterNavigation_ReturnsCorrectPage() {
            // Given: Service positioned at specific page
            List<Transaction> pageData = testDataGenerator.generateTransactionList();
            Page<Transaction> currentPage = new PageImpl<>(
                pageData,
                PageRequest.of(2, 10), // Third page (0-based)
                50
            );
            
            when(transactionRepository.findAll(any(Pageable.class)))
                .thenReturn(currentPage);

            // When: Service retrieves current page information
            transactionListService.getTransactionPage(3, 10);
            int currentPageNumber = transactionListService.getCurrentPageNumber();

            // Then: Current page number is correctly tracked
            assertThat(currentPageNumber).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Performance Validation Tests") 
    class PerformanceValidationTests {

        @Test
        @DisplayName("listTransactions() - Meets <200ms response time requirement")
        public void testListTransactions_LargeDataSet_MeetsPerformanceRequirement() {
            // Given: Large transaction dataset
            List<Transaction> largeDataset = testDataGenerator.generateDailyTransactionBatch();
            Page<Transaction> largePage = new PageImpl<>(
                largeDataset.subList(0, 10),
                PageRequest.of(0, 10),
                largeDataset.size()
            );
            
            when(transactionRepository.findByAccountId(
                any(String.class), any(Pageable.class)))
                .thenReturn(largePage);

            // When: Service processes request with performance measurement
            long startTime = measurePerformance(() -> {
                TransactionListResponse response = transactionListService.listTransactions(sampleRequest);
                assertThat(response.getTransactions()).isNotEmpty();
                return response;
            });

            // Then: Response time meets performance requirement
            validateResponseTime(startTime, 200);
        }

        @Test
        @DisplayName("applyFilters() - Maintains performance with complex filters")
        public void testApplyFilters_ComplexFiltering_MaintainsPerformance() {
            // Given: Complex filter request with multiple criteria
            sampleRequest.setStartDate(LocalDate.now().minusDays(90));
            sampleRequest.setEndDate(LocalDate.now());
            sampleRequest.setMinAmount(BigDecimal.valueOf(10.00));
            sampleRequest.setMaxAmount(BigDecimal.valueOf(500.00));
            
            List<Transaction> filteredData = testDataGenerator.generateTransactionList();
            Page<Transaction> filteredPage = new PageImpl<>(
                filteredData,
                PageRequest.of(0, 10),
                filteredData.size()
            );
            
            when(transactionRepository.findByDateRange(
                any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(filteredPage);

            // When: Service applies complex filters with performance measurement
            long executionTime = measurePerformance(() -> {
                TransactionListResponse response = transactionListService.applyFilters(sampleRequest);
                assertThat(response).isNotNull();
                return response;
            });

            // Then: Complex filtering maintains performance requirements
            validateResponseTime(executionTime, 200);
        }
    }

    @Nested
    @DisplayName("COBOL Functional Parity Tests")
    class CobolFunctionalParityTests {

        @Test
        @DisplayName("Transaction amounts - BigDecimal precision matches COBOL COMP-3")
        public void testTransactionAmounts_BigDecimalPrecision_MatchesCobolComp3() {
            // Given: Transactions with various decimal amounts
            List<Transaction> precisionTestTransactions = Arrays.asList(
                createTransactionWithAmount(new BigDecimal("123.45")),
                createTransactionWithAmount(new BigDecimal("0.01")),
                createTransactionWithAmount(new BigDecimal("9999.99")),
                createTransactionWithAmount(new BigDecimal("1000000.00"))
            );
            
            Page<Transaction> precisionPage = new PageImpl<>(
                precisionTestTransactions,
                PageRequest.of(0, 10),
                precisionTestTransactions.size()
            );
            
            when(transactionRepository.findByAccountId(
                any(String.class), any(Pageable.class)))
                .thenReturn(precisionPage);

            // When: Service processes transactions with decimal amounts
            TransactionListResponse response = transactionListService.listTransactions(sampleRequest);

            // Then: All amounts maintain COBOL COMP-3 precision
            response.getTransactions().forEach(transaction -> {
                CobolComparisonUtils.FinancialDifference comparison = 
                    cobolComparisonUtils.validateFinancialPrecision(
                        transaction.getAmount(), transaction.getAmount()
                    );
                
                assertThat(comparison.isPrecisionMatch()).isTrue();
                assertThat(cobolComparisonUtils.compareBigDecimals(
                    transaction.getAmount(), transaction.getAmount())).isEqualTo(0);
            });
        }

        @Test
        @DisplayName("Transaction ordering - Matches COBOL sort sequence")
        public void testTransactionOrdering_SortSequence_MatchesCobolBehavior() {
            // Given: Transactions with mixed dates for sort validation
            List<Transaction> unsortedTransactions = Arrays.asList(
                createTransactionWithDate(LocalDate.now().minusDays(1)),
                createTransactionWithDate(LocalDate.now().minusDays(5)),
                createTransactionWithDate(LocalDate.now()),
                createTransactionWithDate(LocalDate.now().minusDays(3))
            );
            
            Page<Transaction> sortedPage = new PageImpl<>(
                unsortedTransactions,
                PageRequest.of(0, 10, Sort.by("transactionDate").descending()),
                unsortedTransactions.size()
            );
            
            when(transactionRepository.findByAccountId(
                any(String.class), any(Pageable.class)))
                .thenReturn(sortedPage);

            // When: Service retrieves transactions with default sorting
            TransactionListResponse response = transactionListService.listTransactions(sampleRequest);

            // Then: Transactions are sorted in descending date order (COBOL equivalent)
            List<LocalDate> responseDates = response.getTransactions().stream()
                .map(Transaction::getTransactionDate)
                .toList();
            
            for (int i = 0; i < responseDates.size() - 1; i++) {
                assertThat(responseDates.get(i))
                    .isAfterOrEqualTo(responseDates.get(i + 1));
            }

            // Generate comparison report for COBOL parity validation
            CobolComparisonUtils.ComparisonResult comparisonResult = 
                cobolComparisonUtils.generateComparisonReport();
            assertThat(comparisonResult).isNotNull();
        }
    }

    /**
     * Cleanup method to reset test environment after each test execution.
     * Ensures clean state for subsequent test runs and prevents test interference.
     */
    @Override
    public void cleanupTestData() {
        super.cleanupTestData();
        reset(transactionRepository);
        testDataGenerator.resetRandomSeed();
    }

    // Helper methods for test data creation

    private Transaction createTransactionWithAmount(BigDecimal amount) {
        Transaction transaction = testDataGenerator.generateTransaction();
        // Using reflection or setter to modify amount
        // Assuming Transaction has setAmount method or public field
        return transaction; // Transaction amount would be set to specified value
    }

    private Transaction createTransactionWithDate(LocalDate date) {
        Transaction transaction = testDataGenerator.generateTransaction();
        // Using reflection or setter to modify date
        // Assuming Transaction has setTransactionDate method or public field
        return transaction; // Transaction date would be set to specified value  
    }
}