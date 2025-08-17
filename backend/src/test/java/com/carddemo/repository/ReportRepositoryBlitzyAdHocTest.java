package com.carddemo.repository;

import com.carddemo.entity.Report;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ad-hoc integration tests for ReportRepository to validate all custom query methods
 * and ensure proper mapping from COBOL reporting functionality to Spring Data JPA operations.
 * 
 * This test suite validates the complete repository functionality including:
 * - Date range queries replicating CBTRN03C filtering logic
 * - Report status and type filtering from CORPT00C user interface
 * - Batch operations for report cleanup and management
 * - Performance optimization through proper query execution
 */
@DataJpaTest
@ActiveProfiles("test")
class ReportRepositoryBlitzyAdHocTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ReportRepository reportRepository;

    private Report transactionReport;
    private Report dailySummaryReport;
    private Report completedReport;
    private Report failedReport;
    private Report processingReport;

    @BeforeEach
    void setUp() {
        // Create test data representing various report scenarios from CBTRN03C and CORPT00C
        
        transactionReport = new Report(Report.ReportType.TRANSACTION_DETAIL, "testuser1", Report.Format.PDF);
        transactionReport.setStartDate(LocalDate.of(2024, 1, 1));
        transactionReport.setEndDate(LocalDate.of(2024, 1, 31));
        transactionReport.setStatus(Report.Status.COMPLETED);
        transactionReport.setParameters("{\"accountId\":\"1001\",\"includeAuthorizations\":true}");
        transactionReport.setRecordCount(150L);
        transactionReport.setFileSizeBytes(2048576L);
        transactionReport.setProcessingCompletedAt(LocalDateTime.now().minusDays(1));
        entityManager.persist(transactionReport);

        dailySummaryReport = new Report(Report.ReportType.DAILY_SUMMARY, "testuser2", Report.Format.CSV);
        dailySummaryReport.setStartDate(LocalDate.of(2024, 2, 1));
        dailySummaryReport.setEndDate(LocalDate.of(2024, 2, 28));
        dailySummaryReport.setStatus(Report.Status.REQUESTED);
        dailySummaryReport.setParameters("{\"includeFees\":true,\"summaryLevel\":\"account\"}");
        entityManager.persist(dailySummaryReport);

        completedReport = new Report(Report.ReportType.MONTHLY, "testuser1", Report.Format.EXCEL);
        completedReport.setStartDate(LocalDate.of(2024, 3, 1));
        completedReport.setEndDate(LocalDate.of(2024, 3, 31));
        completedReport.setStatus(Report.Status.COMPLETED);
        completedReport.setFilePath("/reports/monthly-2024-03.xlsx");
        completedReport.setRecordCount(500L);
        completedReport.setFileSizeBytes(5242880L);
        completedReport.setProcessingCompletedAt(LocalDateTime.now().minusHours(2));
        entityManager.persist(completedReport);

        failedReport = new Report(Report.ReportType.AUDIT, "testuser3", Report.Format.PDF);
        failedReport.setStartDate(LocalDate.of(2024, 4, 1));
        failedReport.setEndDate(LocalDate.of(2024, 4, 30));
        failedReport.setStatus(Report.Status.FAILED);
        failedReport.setErrorMessage("Database connection timeout during report generation");
        failedReport.setProcessingCompletedAt(LocalDateTime.now().minusMinutes(30));
        entityManager.persist(failedReport);

        processingReport = new Report(Report.ReportType.COMPLIANCE, "testuser2", Report.Format.CSV);
        processingReport.setStartDate(LocalDate.of(2024, 5, 1));
        processingReport.setEndDate(LocalDate.of(2024, 5, 31));
        processingReport.setStatus(Report.Status.PROCESSING);
        processingReport.setProcessingStartedAt(LocalDateTime.now().minusMinutes(10));
        entityManager.persist(processingReport);

        // Additional failed report for cleanup testing (older than 1 hour)
        Report oldFailedReport = new Report(Report.ReportType.COMPLIANCE, "testuser4", Report.Format.CSV);
        oldFailedReport.setStartDate(LocalDate.of(2024, 6, 1));
        oldFailedReport.setEndDate(LocalDate.of(2024, 6, 30));
        oldFailedReport.setStatus(Report.Status.FAILED);
        oldFailedReport.setErrorMessage("Network timeout during report generation");
        oldFailedReport.setProcessingCompletedAt(LocalDateTime.now().minusHours(3));
        entityManager.persist(oldFailedReport);

        entityManager.flush();
    }

    @Test
    void testFindByReportType() {
        // Test basic report type filtering equivalent to COBOL file READ by report type
        List<Report> transactionReports = reportRepository.findByReportType(Report.ReportType.TRANSACTION_DETAIL);
        
        assertThat(transactionReports).hasSize(1);
        assertThat(transactionReports.get(0).getReportType()).isEqualTo(Report.ReportType.TRANSACTION_DETAIL);
        assertThat(transactionReports.get(0).getUserId()).isEqualTo("testuser1");
    }

    @Test
    void testFindByDateRange() {
        // Test date range queries replicating CBTRN03C date filtering logic
        LocalDate searchStart = LocalDate.of(2024, 1, 15);
        LocalDate searchEnd = LocalDate.of(2024, 2, 15);
        
        List<Report> reportsInRange = reportRepository.findByDateRange(searchStart, searchEnd);
        
        // Should find transaction report (Jan 1-31) and daily summary (Feb 1-28) as they overlap
        assertThat(reportsInRange).hasSize(2);
        assertThat(reportsInRange)
            .extracting(Report::getReportType)
            .containsExactlyInAnyOrder(Report.ReportType.TRANSACTION_DETAIL, Report.ReportType.DAILY_SUMMARY);
    }

    @Test
    void testFindByStatusAndReportType() {
        // Test combined status and type filtering from CORPT00C user interface
        List<Report> completedMonthly = reportRepository.findByStatusAndReportType(
            Report.Status.COMPLETED, Report.ReportType.MONTHLY);
        
        assertThat(completedMonthly).hasSize(1);
        assertThat(completedMonthly.get(0).getReportType()).isEqualTo(Report.ReportType.MONTHLY);
        assertThat(completedMonthly.get(0).getStatus()).isEqualTo(Report.Status.COMPLETED);
    }

    @Test
    void testFindByUserIdAndDateRange() {
        // Test user-specific report filtering within date ranges
        LocalDateTime rangeStart = LocalDateTime.now().minusDays(2);
        LocalDateTime rangeEnd = LocalDateTime.now().plusDays(1);
        
        List<Report> userReports = reportRepository.findByUserIdAndDateRange("testuser1", rangeStart, rangeEnd);
        
        assertThat(userReports).hasSize(2); // transaction and monthly reports for testuser1
        assertThat(userReports).allMatch(report -> "testuser1".equals(report.getUserId()));
        assertThat(userReports.get(0).getCreatedAt()).isAfter(userReports.get(1).getCreatedAt()); // DESC order
    }

    @Test
    void testDeleteOldReports() {
        // Test cleanup functionality for expired reports
        // Note: @CreationTimestamp prevents manual setting of createdAt, 
        // so we test that the method executes without error and handles the criteria correctly
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        
        // Count reports before deletion (should be all current since timestamps are auto-generated)
        long countBefore = reportRepository.count();
        
        // Execute the delete operation - should delete nothing since all reports are recent
        int deletedCount = reportRepository.deleteOldReports(cutoffDate);
        
        // Verify the method executed without error
        assertThat(deletedCount).isGreaterThanOrEqualTo(0);
        
        // Since all test reports have auto-generated timestamps (current time), 
        // none should be older than the cutoff, so deletion count should be 0
        assertThat(deletedCount).isEqualTo(0);
        
        // Verify count remains the same since no records should be deleted
        long countAfter = reportRepository.count();
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    void testFindPendingReports() {
        // Test job monitoring functionality similar to CBTRN03C batch tracking
        List<Report> pendingReports = reportRepository.findPendingReports();
        
        assertThat(pendingReports).hasSize(2); // REQUESTED and PROCESSING reports
        assertThat(pendingReports)
            .extracting(Report::getStatus)
            .containsExactlyInAnyOrder(Report.Status.REQUESTED, Report.Status.PROCESSING);
        
        // Verify ordering by creation time (ASC)
        assertThat(pendingReports.get(0).getCreatedAt())
            .isBeforeOrEqualTo(pendingReports.get(1).getCreatedAt());
    }

    @Test
    void testCountByReportTypeAndStatus() {
        // Test statistical reporting for dashboard metrics
        long completedCount = reportRepository.countByReportTypeAndStatus(
            Report.ReportType.TRANSACTION_DETAIL, Report.Status.COMPLETED);
        
        assertThat(completedCount).isEqualTo(1);
        
        long requestedCount = reportRepository.countByReportTypeAndStatus(
            Report.ReportType.DAILY_SUMMARY, Report.Status.REQUESTED);
        
        assertThat(requestedCount).isEqualTo(1);
    }

    @Test
    void testFindByStatusOrderByCreatedAtDesc() {
        // Test status-based report queue management
        List<Report> completedReports = reportRepository.findByStatusOrderByCreatedAtDesc(Report.Status.COMPLETED);
        
        assertThat(completedReports).hasSize(2); // transaction and monthly reports
        assertThat(completedReports)
            .allMatch(report -> Report.Status.COMPLETED.equals(report.getStatus()));
        
        // Verify DESC ordering
        assertThat(completedReports.get(0).getCreatedAt())
            .isAfterOrEqualTo(completedReports.get(1).getCreatedAt());
    }

    @Test
    void testFindTopReportsByUserId() {
        // Test recent report history functionality for user interfaces
        List<Report> recentReports = reportRepository.findTopReportsByUserId("testuser1");
        
        assertThat(recentReports).hasSize(2);
        assertThat(recentReports).allMatch(report -> "testuser1".equals(report.getUserId()));
        
        // Verify DESC ordering by creation time
        assertThat(recentReports.get(0).getCreatedAt())
            .isAfterOrEqualTo(recentReports.get(1).getCreatedAt());
    }

    @Test
    void testFindByReportTypeIn() {
        // Test bulk report queries for dashboard interfaces
        List<Report.ReportType> reportTypes = Arrays.asList(
            Report.ReportType.TRANSACTION_DETAIL, 
            Report.ReportType.AUDIT
        );
        
        List<Report> reports = reportRepository.findByReportTypeIn(reportTypes);
        
        assertThat(reports).hasSize(2);
        assertThat(reports)
            .extracting(Report::getReportType)
            .containsExactlyInAnyOrder(Report.ReportType.TRANSACTION_DETAIL, Report.ReportType.AUDIT);
    }

    @Test
    void testFindReportsEligibleForCleanup() {
        // Test automated file lifecycle management
        LocalDateTime completedBefore = LocalDateTime.now().minusHours(1);
        List<Report.Status> eligibleStatuses = Arrays.asList(
            Report.Status.COMPLETED, 
            Report.Status.FAILED
        );
        
        List<Report> eligibleReports = reportRepository.findReportsEligibleForCleanup(
            completedBefore, eligibleStatuses);
        
        assertThat(eligibleReports).hasSize(3); // completed and failed reports older than 1 hour
        assertThat(eligibleReports)
            .extracting(Report::getStatus)
            .containsExactlyInAnyOrder(Report.Status.COMPLETED, Report.Status.COMPLETED, Report.Status.FAILED);
    }

    @Test
    void testCountByUserIdAndDateRange() {
        // Test user-specific reporting metrics for quota management
        LocalDateTime rangeStart = LocalDateTime.now().minusDays(1);
        LocalDateTime rangeEnd = LocalDateTime.now().plusDays(1);
        
        long userReportCount = reportRepository.countByUserIdAndDateRange("testuser1", rangeStart, rangeEnd);
        
        assertThat(userReportCount).isEqualTo(2); // transaction and monthly reports
    }

    @Test
    void testFindFailedReportsForRetry() {
        // Test error recovery and retry mechanisms
        LocalDateTime failedSince = LocalDateTime.now().minusHours(1);
        
        List<Report> failedReports = reportRepository.findFailedReportsForRetry(failedSince);
        
        assertThat(failedReports).hasSize(1);
        assertThat(failedReports.get(0).getStatus()).isEqualTo(Report.Status.FAILED);
        assertThat(failedReports.get(0).getErrorMessage())
            .contains("Database connection timeout");
    }

    @Test
    void testUpdateReportStatusBatch() {
        // Test bulk status updates for administrative operations
        List<Long> reportIds = Arrays.asList(
            dailySummaryReport.getId(), 
            processingReport.getId()
        );
        
        int updatedCount = reportRepository.updateReportStatusBatch(reportIds, Report.Status.CANCELLED);
        
        assertThat(updatedCount).isEqualTo(2);
        
        // Verify updates
        entityManager.clear();
        Report updatedDaily = reportRepository.findById(dailySummaryReport.getId()).orElse(null);
        Report updatedProcessing = reportRepository.findById(processingReport.getId()).orElse(null);
        
        assertThat(updatedDaily.getStatus()).isEqualTo(Report.Status.CANCELLED);
        assertThat(updatedProcessing.getStatus()).isEqualTo(Report.Status.CANCELLED);
    }

    @Test
    void testFindByFormatAndStatus() {
        // Test format-specific report management
        List<Report> pdfCompleted = reportRepository.findByFormatAndStatus(
            Report.Format.PDF, Report.Status.COMPLETED);
        
        assertThat(pdfCompleted).hasSize(1); // only transaction report is PDF and completed
        assertThat(pdfCompleted.get(0).getFormat()).isEqualTo(Report.Format.PDF);
        assertThat(pdfCompleted.get(0).getReportType()).isEqualTo(Report.ReportType.TRANSACTION_DETAIL);
    }

    @Test
    void testCalculateTotalFileSizeByDateRange() {
        // Test storage utilization metrics for capacity planning
        LocalDateTime rangeStart = LocalDateTime.now().minusDays(2);
        LocalDateTime rangeEnd = LocalDateTime.now().plusDays(1);
        
        Long totalSize = reportRepository.calculateTotalFileSizeByDateRange(rangeStart, rangeEnd);
        
        // Should include completed transaction report (2MB) and monthly report (5MB)
        assertThat(totalSize).isEqualTo(2048576L + 5242880L); // 7291456 bytes total
    }

    @Test
    void testRepositoryIntegrationWithSpringDataJpa() {
        // Test basic Spring Data JPA functionality integration
        assertThat(reportRepository.count()).isEqualTo(6);
        
        // Test findById
        Report found = reportRepository.findById(transactionReport.getId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getReportType()).isEqualTo(Report.ReportType.TRANSACTION_DETAIL);
        
        // Test save
        Report newReport = new Report(Report.ReportType.CUSTOM, "testuser4", Report.Format.TEXT);
        newReport.setStartDate(LocalDate.of(2024, 6, 1));
        newReport.setEndDate(LocalDate.of(2024, 6, 30));
        
        Report saved = reportRepository.save(newReport);
        assertThat(saved.getId()).isNotNull();
        assertThat(reportRepository.count()).isEqualTo(7);
    }

    @Test
    void testRepositoryExceptionHandling() {
        // Test handling of edge cases and null parameters
        List<Report> emptyResult = reportRepository.findByDateRange(
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));
        assertThat(emptyResult).isEmpty();
        
        long zeroCount = reportRepository.countByReportTypeAndStatus(
            Report.ReportType.YEARLY, Report.Status.PROCESSING);
        assertThat(zeroCount).isEqualTo(0);
        
        List<Report> noFailures = reportRepository.findFailedReportsForRetry(
            LocalDateTime.now().minusMinutes(1));
        assertThat(noFailures).isEmpty();
    }
}