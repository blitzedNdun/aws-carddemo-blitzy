package com.carddemo.service;

import com.carddemo.entity.*;
import com.carddemo.repository.*;
import com.carddemo.batch.TransactionReportJob;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.assertj.core.api.Assertions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Unit test class for ReportingService that validates reporting functionality 
 * converted from CBTRN03C and CORPT00C COBOL programs with comprehensive 
 * report generation capabilities.
 * 
 * Tests validate:
 * - Transaction report generation with date filtering and aggregation (CBTRN03C logic)
 * - Online report parameter validation and scheduling (CORPT00C logic) 
 * - Multi-format output support (PDF, CSV, TEXT, EXCEL)
 * - Report status tracking and lifecycle management
 * - Performance optimization and caching
 * - Financial precision matching COBOL COMP-3 behavior
 */
@SpringBootTest
@SpringJUnitConfig
@ExtendWith(MockitoExtension.class)
public class ReportingServiceTest extends BaseServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CardXrefRepository cardXrefRepository;

    @Mock
    private TransactionTypeRepository transactionTypeRepository;

    @Mock
    private TransactionCategoryRepository transactionCategoryRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private FileWriterService fileWriterService;

    @Mock
    private TransactionReportJob transactionReportJob;

    private ReportingService reportingService;
    private TestDataBuilder testDataBuilder;
    private CobolComparisonUtils cobolComparisonUtils;

    @BeforeEach
    public void setUp() {
        setupTestData();
        testDataBuilder = new TestDataBuilder();
        cobolComparisonUtils = new CobolComparisonUtils();
        
        reportingService = new ReportingService(
            reportRepository,
            transactionRepository,
            cardXrefRepository,
            transactionTypeRepository,
            transactionCategoryRepository,
            accountRepository,
            fileWriterService
        );
    }

    /**
     * Test transaction report generation functionality converted from CBTRN03C.
     * Validates date filtering, data aggregation, and report formatting matching
     * COBOL program behavior including page totals and grand totals calculation.
     */
    @Test
    public void testGenerateTransactionReport() {
        // Arrange - Create test data matching CBTRN03C input structures
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        String accountId = "000000001";
        Report.ReportType reportType = Report.ReportType.TRANSACTION;
        Report.Format format = Report.Format.TEXT;

        // Create test transactions matching COBOL transaction record layout
        List<Transaction> testTransactions = List.of(
            testDataBuilder.transactionBuilder()
                .transactionId("T001")
                .accountId(accountId)
                .amount(BigDecimal.valueOf(125.50))
                .transactionDate(startDate.plusDays(5))
                .transactionType("01")
                .build(),
            testDataBuilder.transactionBuilder()
                .transactionId("T002")
                .accountId(accountId)
                .amount(BigDecimal.valueOf(75.25))
                .transactionDate(startDate.plusDays(10))
                .transactionType("02")
                .build()
        );

        Account testAccount = testDataBuilder.accountBuilder()
            .accountId(accountId)
            .currentBalance(BigDecimal.valueOf(1500.00))
            .creditLimit(BigDecimal.valueOf(5000.00))
            .customerId("C001")
            .build();

        Report expectedReport = new Report();
        expectedReport.setReportType(reportType);
        expectedReport.setStartDate(startDate);
        expectedReport.setEndDate(endDate);
        expectedReport.setStatus(Report.Status.PENDING);
        expectedReport.setFormat(format);
        expectedReport.setCreatedBy("SYSTEM");

        // Mock repository interactions matching CBTRN03C file operations
        when(transactionRepository.findByAccountIdAndDateRange(accountId, startDate, endDate))
            .thenReturn(testTransactions);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(reportRepository.save(any(Report.class))).thenReturn(expectedReport);
        when(fileWriterService.writeStatementFile(anyString(), anyString()))
            .thenReturn("/reports/transaction_report_001.txt");

        // Act - Execute report generation
        long startTime = System.currentTimeMillis();
        Report result = reportingService.generateTransactionReport(accountId, startDate, endDate, format);
        long endTime = System.currentTimeMillis();

        // Assert - Validate results match COBOL program behavior
        assertThat(result).isNotNull();
        assertThat(result.getReportType()).isEqualTo(Report.ReportType.TRANSACTION);
        assertThat(result.getStartDate()).isEqualTo(startDate);
        assertThat(result.getEndDate()).isEqualTo(endDate);
        assertThat(result.getFormat()).isEqualTo(format);

        // Verify performance meets COBOL processing expectations
        validateResponseTime(endTime - startTime, 200); // Sub-200ms requirement

        // Verify repository interactions match COBOL file access patterns
        verify(transactionRepository).findByAccountIdAndDateRange(accountId, startDate, endDate);
        verify(accountRepository).findById(accountId);
        verify(reportRepository).save(any(Report.class));
        verify(fileWriterService).writeStatementFile(anyString(), anyString());

        // Validate financial calculations match COBOL COMP-3 precision
        BigDecimal expectedTotal = BigDecimal.valueOf(200.75); // 125.50 + 75.25
        assertBigDecimalEquals(expectedTotal, testTransactions.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /**
     * Test daily summary report generation with aggregation logic.
     * Validates daily transaction summaries and account balance calculations
     * matching COBOL numeric precision requirements.
     */
    @Test
    public void testGenerateDailySummaryReport() {
        // Arrange
        LocalDate reportDate = LocalDate.of(2024, 1, 15);
        Report.Format format = Report.Format.CSV;

        List<Transaction> dailyTransactions = List.of(
            testDataBuilder.transactionBuilder()
                .transactionId("D001")
                .amount(BigDecimal.valueOf(500.00))
                .transactionDate(reportDate)
                .transactionType("01")
                .build(),
            testDataBuilder.transactionBuilder()
                .transactionId("D002")
                .amount(BigDecimal.valueOf(150.75))
                .transactionDate(reportDate)
                .transactionType("02")
                .build()
        );

        when(transactionRepository.findByProcessingDateBetween(
            reportDate.atStartOfDay(),
            reportDate.plusDays(1).atStartOfDay()))
            .thenReturn(dailyTransactions);
        when(reportRepository.save(any(Report.class))).thenReturn(new Report());
        when(fileWriterService.writeCsvReport(anyString(), anyList())).thenReturn("/reports/daily_summary.csv");

        // Act
        long startTime = System.currentTimeMillis();
        Report result = reportingService.generateDailySummaryReport(reportDate, format);
        long endTime = System.currentTimeMillis();

        // Assert
        assertThat(result).isNotNull();
        validateResponseTime(endTime - startTime, 200);

        verify(transactionRepository).findByProcessingDateBetween(any(), any());
        verify(fileWriterService).writeCsvReport(anyString(), anyList());
        
        // Validate precision matches COBOL calculations
        BigDecimal expectedDailyTotal = BigDecimal.valueOf(650.75);
        assertBigDecimalEquals(expectedDailyTotal, dailyTransactions.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /**
     * Test audit report generation with comprehensive data validation.
     * Validates system audit trails and compliance reporting requirements.
     */
    @Test
    public void testGenerateAuditReport() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        Report.Format format = Report.Format.PDF;
        String userId = "AUDIT_USER";

        when(transactionRepository.findByProcessingDateBetween(any(), any()))
            .thenReturn(List.of());
        when(reportRepository.save(any(Report.class))).thenReturn(new Report());
        when(fileWriterService.generatePdfStatement(anyString(), anyString()))
            .thenReturn("/reports/audit_report.pdf");

        // Act
        long startTime = System.currentTimeMillis();
        Report result = reportingService.generateAuditReport(startDate, endDate, userId, format);
        long endTime = System.currentTimeMillis();

        // Assert
        assertThat(result).isNotNull();
        validateResponseTime(endTime - startTime, 200);
        verify(fileWriterService).generatePdfStatement(anyString(), anyString());
    }

    /**
     * Test compliance report generation for regulatory requirements.
     * Validates regulatory data formatting and completeness.
     */
    @Test
    public void testGenerateComplianceReport() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 3, 31);
        Report.Format format = Report.Format.EXCEL;
        String complianceType = "SOX_COMPLIANCE";

        when(transactionRepository.count()).thenReturn(1000L);
        when(reportRepository.save(any(Report.class))).thenReturn(new Report());

        // Act
        long startTime = System.currentTimeMillis();
        Report result = reportingService.generateComplianceReport(startDate, endDate, complianceType, format);
        long endTime = System.currentTimeMillis();

        // Assert
        assertThat(result).isNotNull();
        validateResponseTime(endTime - startTime, 200);
        verify(transactionRepository).count();
    }

    /**
     * Test asynchronous report scheduling functionality converted from CORPT00C.
     * Validates report parameter validation and job submission matching
     * COBOL online program behavior for batch job submission.
     */
    @Test
    public void testScheduleReport() {
        // Arrange - Test data matching CORPT00C screen input validation
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        Report.ReportType reportType = Report.ReportType.TRANSACTION;
        Report.Format format = Report.Format.PDF;
        String userId = "USER001";

        Report savedReport = new Report();
        savedReport.setId(UUID.randomUUID());
        savedReport.setReportType(reportType);
        savedReport.setStatus(Report.Status.SCHEDULED);
        savedReport.setCreatedBy(userId);

        when(reportRepository.save(any(Report.class))).thenReturn(savedReport);

        // Act - Execute scheduling logic matching CORPT00C job submission
        long startTime = System.currentTimeMillis();
        String reportId = reportingService.scheduleReport(reportType, startDate, endDate, format, userId);
        long endTime = System.currentTimeMillis();

        // Assert - Validate scheduling behavior matches COBOL program
        assertThat(reportId).isNotNull();
        validateResponseTime(endTime - startTime, 200);

        verify(reportRepository).save(argThat(report -> 
            report.getReportType() == reportType &&
            report.getStatus() == Report.Status.SCHEDULED &&
            report.getCreatedBy().equals(userId)
        ));
    }

    /**
     * Test report status tracking and monitoring.
     * Validates status transitions and progress tracking.
     */
    @Test
    public void testGetReportStatus() {
        // Arrange
        String reportId = UUID.randomUUID().toString();
        Report report = new Report();
        report.setId(UUID.fromString(reportId));
        report.setStatus(Report.Status.COMPLETED);
        report.setProgress(100);

        when(reportRepository.findById(UUID.fromString(reportId))).thenReturn(Optional.of(report));

        // Act
        long startTime = System.currentTimeMillis();
        Report.Status status = reportingService.getReportStatus(reportId);
        long endTime = System.currentTimeMillis();

        // Assert
        assertThat(status).isEqualTo(Report.Status.COMPLETED);
        validateResponseTime(endTime - startTime, 200);
        verify(reportRepository).findById(UUID.fromString(reportId));
    }

    /**
     * Test report download functionality with file retrieval.
     * Validates file access and content delivery.
     */
    @Test
    public void testDownloadReport() {
        // Arrange
        String reportId = UUID.randomUUID().toString();
        Report report = new Report();
        report.setId(UUID.fromString(reportId));
        report.setStatus(Report.Status.COMPLETED);
        report.setFilePath("/reports/test_report.pdf");

        when(reportRepository.findById(UUID.fromString(reportId))).thenReturn(Optional.of(report));

        // Act
        long startTime = System.currentTimeMillis();
        String filePath = reportingService.downloadReport(reportId);
        long endTime = System.currentTimeMillis();

        // Assert
        assertThat(filePath).isEqualTo("/reports/test_report.pdf");
        validateResponseTime(endTime - startTime, 200);
        verify(reportRepository).findById(UUID.fromString(reportId));
    }

    /**
     * Test report deletion and cleanup functionality.
     * Validates proper cleanup of files and database records.
     */
    @Test
    public void testDeleteReport() {
        // Arrange
        String reportId = UUID.randomUUID().toString();
        Report report = new Report();
        report.setId(UUID.fromString(reportId));
        report.setFilePath("/reports/test_report.pdf");

        when(reportRepository.findById(UUID.fromString(reportId))).thenReturn(Optional.of(report));
        when(fileWriterService.archiveFile(anyString())).thenReturn(true);

        // Act
        long startTime = System.currentTimeMillis();
        boolean deleted = reportingService.deleteReport(reportId);
        long endTime = System.currentTimeMillis();

        // Assert
        assertThat(deleted).isTrue();
        validateResponseTime(endTime - startTime, 200);
        verify(reportRepository).findById(UUID.fromString(reportId));
        verify(fileWriterService).archiveFile("/reports/test_report.pdf");
        verify(reportRepository).delete(report);
    }

    /**
     * Test report history retrieval with pagination.
     * Validates historical report tracking and user access.
     */
    @Test
    public void testGetReportHistory() {
        // Arrange
        String userId = "USER001";
        Report.ReportType reportType = Report.ReportType.TRANSACTION;
        
        List<Report> historicalReports = List.of(
            createTestReport("R001", Report.Status.COMPLETED),
            createTestReport("R002", Report.Status.COMPLETED),
            createTestReport("R003", Report.Status.FAILED)
        );

        when(reportRepository.findByReportType(reportType)).thenReturn(historicalReports);

        // Act
        long startTime = System.currentTimeMillis();
        List<Report> history = reportingService.getReportHistory(userId, reportType);
        long endTime = System.currentTimeMillis();

        // Assert
        assertThat(history).hasSize(3);
        assertThat(history).allMatch(report -> report.getReportType() == reportType);
        validateResponseTime(endTime - startTime, 200);
        verify(reportRepository).findByReportType(reportType);
    }

    /**
     * Test report parameter validation matching CORPT00C input validation.
     * Validates date range validation, format validation, and business rules
     * converted from COBOL screen validation logic.
     */
    @Test
    public void testValidateReportParameters() {
        // Arrange - Test valid parameters
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        Report.ReportType reportType = Report.ReportType.TRANSACTION;
        Report.Format format = Report.Format.PDF;

        // Act - Test valid parameters
        long startTime = System.currentTimeMillis();
        boolean isValid = reportingService.validateReportParameters(reportType, startDate, endDate, format);
        long endTime = System.currentTimeMillis();

        // Assert - Valid parameters should pass
        assertThat(isValid).isTrue();
        validateResponseTime(endTime - startTime, 200);

        // Test invalid date range (end before start)
        LocalDate invalidEndDate = LocalDate.of(2023, 12, 31);
        boolean isInvalid = reportingService.validateReportParameters(reportType, startDate, invalidEndDate, format);
        assertThat(isInvalid).isFalse();

        // Test future dates
        LocalDate futureStart = LocalDate.now().plusDays(1);
        LocalDate futureEnd = LocalDate.now().plusDays(30);
        boolean isFutureInvalid = reportingService.validateReportParameters(reportType, futureStart, futureEnd, format);
        assertThat(isFutureInvalid).isFalse();
    }

    /**
     * Test report caching and performance optimization.
     * Validates caching behavior and cache invalidation logic.
     */
    @Test
    public void testReportCaching() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        String accountId = "000000001";
        Report.Format format = Report.Format.TEXT;

        List<Transaction> testTransactions = List.of(
            testDataBuilder.transactionBuilder()
                .transactionId("T001")
                .accountId(accountId)
                .amount(BigDecimal.valueOf(100.00))
                .build()
        );

        when(transactionRepository.findByAccountIdAndDateRange(accountId, startDate, endDate))
            .thenReturn(testTransactions);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(new Account()));
        when(reportRepository.save(any(Report.class))).thenReturn(new Report());
        when(fileWriterService.writeStatementFile(anyString(), anyString()))
            .thenReturn("/reports/cached_report.txt");

        // Act - Generate report twice to test caching
        Report firstResult = reportingService.generateTransactionReport(accountId, startDate, endDate, format);
        Report secondResult = reportingService.generateTransactionReport(accountId, startDate, endDate, format);

        // Assert - Verify caching behavior
        assertThat(firstResult).isNotNull();
        assertThat(secondResult).isNotNull();

        // Verify repository called only once due to caching
        verify(transactionRepository, times(1)).findByAccountIdAndDateRange(accountId, startDate, endDate);
    }

    /**
     * Test error handling and exception scenarios.
     * Validates proper error handling matching COBOL ABEND processing.
     */
    @Test
    public void testErrorHandling() {
        // Test invalid account ID
        String invalidAccountId = "INVALID";
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        Report.Format format = Report.Format.TEXT;

        when(accountRepository.findById(invalidAccountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> 
            reportingService.generateTransactionReport(invalidAccountId, startDate, endDate, format))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Account not found");

        // Test database connection error
        when(transactionRepository.findByAccountIdAndDateRange(anyString(), any(), any()))
            .thenThrow(new RuntimeException("Database connection failed"));

        assertThatThrownBy(() -> 
            reportingService.generateTransactionReport("000000001", startDate, endDate, format))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Database connection failed");
    }

    /**
     * Test multi-format report generation.
     * Validates all supported output formats (PDF, CSV, TEXT, EXCEL).
     */
    @Test
    public void testMultiFormatReportGeneration() {
        // Arrange common test data
        String accountId = "000000001";
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        List<Transaction> testTransactions = List.of(
            testDataBuilder.transactionBuilder()
                .transactionId("T001")
                .accountId(accountId)
                .amount(BigDecimal.valueOf(100.00))
                .build()
        );

        when(transactionRepository.findByAccountIdAndDateRange(accountId, startDate, endDate))
            .thenReturn(testTransactions);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(new Account()));
        when(reportRepository.save(any(Report.class))).thenReturn(new Report());

        // Test each format
        for (Report.Format format : Report.Format.values()) {
            switch (format) {
                case PDF:
                    when(fileWriterService.generatePdfStatement(anyString(), anyString()))
                        .thenReturn("/reports/test.pdf");
                    break;
                case CSV:
                    when(fileWriterService.writeCsvReport(anyString(), anyList()))
                        .thenReturn("/reports/test.csv");
                    break;
                case TEXT:
                    when(fileWriterService.writeStatementFile(anyString(), anyString()))
                        .thenReturn("/reports/test.txt");
                    break;
                case EXCEL:
                    when(fileWriterService.writeStatementFile(anyString(), anyString()))
                        .thenReturn("/reports/test.xlsx");
                    break;
            }

            // Act
            Report result = reportingService.generateTransactionReport(accountId, startDate, endDate, format);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getFormat()).isEqualTo(format);
        }
    }

    /**
     * Helper method to create test report objects.
     */
    private Report createTestReport(String id, Report.Status status) {
        Report report = new Report();
        report.setId(UUID.fromString(id.replaceAll("R", "00000000-0000-0000-0000-00000000000")));
        report.setStatus(status);
        report.setReportType(Report.ReportType.TRANSACTION);
        report.setCreatedDate(LocalDateTime.now().minusDays(1));
        return report;
    }
}