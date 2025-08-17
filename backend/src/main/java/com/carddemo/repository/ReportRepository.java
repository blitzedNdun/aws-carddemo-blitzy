package com.carddemo.repository;

import com.carddemo.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository interface for Report entity providing data access operations
 * for report metadata, custom queries for report filtering by date ranges and types,
 * and batch report management operations.
 * 
 * This repository supports the reporting functionality converted from CBTRN03C and CORPT00C 
 * COBOL programs, providing comprehensive report lifecycle management with date ranges, 
 * report types, and generation status tracking. It replaces VSAM file access patterns
 * from the original mainframe implementation with modern JPA repository operations.
 * 
 * Key Features:
 * - Report filtering by type, status, user, and date ranges
 * - Batch operations for report cleanup and management
 * - Optimized queries for reporting performance
 * - Support for concurrent report generation and retrieval
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * Finds all reports of a specific report type.
     * Equivalent to COBOL file READ operations filtered by report type from CBTRN03C.
     * 
     * @param reportType the type of reports to find
     * @return list of reports matching the specified type
     */
    List<Report> findByReportType(Report.ReportType reportType);

    /**
     * Finds all reports within a specified date range based on the report's start and end dates.
     * Replicates the date filtering logic from CBTRN03C that compared TRAN-PROC-TS with 
     * WS-START-DATE and WS-END-DATE parameters.
     * 
     * @param startDate the earliest start date for the report range
     * @param endDate the latest end date for the report range
     * @return list of reports with date ranges overlapping the specified period
     */
    @Query("SELECT r FROM Report r WHERE " +
           "(r.startDate IS NULL OR r.startDate <= :endDate) AND " +
           "(r.endDate IS NULL OR r.endDate >= :startDate)")
    List<Report> findByDateRange(@Param("startDate") LocalDate startDate, 
                                @Param("endDate") LocalDate endDate);

    /**
     * Finds reports matching both status and report type criteria.
     * Supports the report management functionality from CORPT00C where users could 
     * view reports by specific types and their processing status.
     * 
     * @param status the status of reports to find
     * @param reportType the type of reports to find
     * @return list of reports matching both criteria
     */
    List<Report> findByStatusAndType(Report.Status status, Report.ReportType reportType);

    /**
     * Finds reports for a specific user within a date range.
     * Combines user-specific filtering with date range queries to support
     * personalized report management as required by the CORPT00C user interface.
     * 
     * @param userId the ID of the user who requested the reports
     * @param startDate the start of the date range for report creation
     * @param endDate the end of the date range for report creation
     * @return list of reports for the user within the specified date range
     */
    @Query("SELECT r FROM Report r WHERE r.userId = :userId AND " +
           "r.createdAt >= :startDate AND r.createdAt <= :endDate " +
           "ORDER BY r.createdAt DESC")
    List<Report> findByUserIdAndDateRange(@Param("userId") String userId,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * Deletes old reports that have exceeded their retention period.
     * Implements cleanup functionality similar to the file management operations
     * in the original COBOL batch processing system. This supports automatic
     * purging of expired reports to manage storage space.
     * 
     * @param cutoffDate reports created before this date will be deleted
     * @return number of reports deleted
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Report r WHERE r.createdAt < :cutoffDate AND " +
           "(r.status = 'COMPLETED' OR r.status = 'FAILED' OR r.status = 'EXPIRED')")
    int deleteOldReports(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Finds all reports that are currently pending processing.
     * Returns reports in REQUESTED, QUEUED, or PROCESSING status to support
     * job monitoring functionality similar to the batch job tracking in CBTRN03C.
     * 
     * @return list of reports that are pending or currently being processed
     */
    @Query("SELECT r FROM Report r WHERE r.status IN ('REQUESTED', 'QUEUED', 'PROCESSING') " +
           "ORDER BY r.createdAt ASC")
    List<Report> findPendingReports();

    /**
     * Counts reports by type and status for reporting dashboard metrics.
     * Provides statistical information for system monitoring and capacity planning,
     * replacing the record counting logic from the original COBOL programs.
     * 
     * @param reportType the type of reports to count
     * @param status the status of reports to count
     * @return count of reports matching the criteria
     */
    long countByTypeAndStatus(Report.ReportType reportType, Report.Status status);

    /**
     * Finds reports by status, ordered by creation date.
     * Supports report queue management and status monitoring functionality
     * equivalent to the job status tracking in the original CICS environment.
     * 
     * @param status the status to filter by
     * @return list of reports with the specified status, ordered by creation time
     */
    List<Report> findByStatusOrderByCreatedAtDesc(Report.Status status);

    /**
     * Finds the most recent reports for a specific user.
     * Supports user interface requirements for displaying recent report history,
     * similar to the report listing functionality in CORPT00C.
     * 
     * @param userId the user ID to search for
     * @param limit the maximum number of reports to return
     * @return list of most recent reports for the user
     */
    @Query("SELECT r FROM Report r WHERE r.userId = :userId " +
           "ORDER BY r.createdAt DESC LIMIT :limit")
    List<Report> findTopReportsByUserId(@Param("userId") String userId, 
                                       @Param("limit") int limit);

    /**
     * Finds reports by multiple report types.
     * Supports bulk report queries for dashboard and management interfaces,
     * replacing multiple VSAM READ operations with a single efficient query.
     * 
     * @param reportTypes list of report types to search for
     * @return list of reports matching any of the specified types
     */
    List<Report> findByReportTypeIn(List<Report.ReportType> reportTypes);

    /**
     * Finds reports that are ready for cleanup based on completion time and status.
     * Identifies reports that have been completed for a specified duration and 
     * are eligible for archival or deletion, supporting automated file lifecycle management.
     * 
     * @param completedBefore timestamp indicating the cutoff for completed reports
     * @param eligibleStatuses list of statuses that are eligible for cleanup
     * @return list of reports eligible for cleanup
     */
    @Query("SELECT r FROM Report r WHERE r.processingCompletedAt < :completedBefore AND " +
           "r.status IN :eligibleStatuses")
    List<Report> findReportsEligibleForCleanup(@Param("completedBefore") LocalDateTime completedBefore,
                                              @Param("eligibleStatuses") List<Report.Status> eligibleStatuses);

    /**
     * Counts total reports for a specific user within a date range.
     * Provides user-specific reporting metrics for usage analysis and
     * quota management, supporting administrative functionality.
     * 
     * @param userId the user ID to count reports for
     * @param startDate the start of the counting period
     * @param endDate the end of the counting period
     * @return count of reports for the user in the specified period
     */
    @Query("SELECT COUNT(r) FROM Report r WHERE r.userId = :userId AND " +
           "r.createdAt >= :startDate AND r.createdAt <= :endDate")
    long countByUserIdAndDateRange(@Param("userId") String userId,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    /**
     * Finds failed reports for retry processing.
     * Supports error recovery and retry mechanisms for failed report generation,
     * similar to the error handling and restart capabilities in the original batch system.
     * 
     * @param failedSince timestamp indicating when to start looking for failed reports
     * @return list of failed reports that may be eligible for retry
     */
    @Query("SELECT r FROM Report r WHERE r.status = 'FAILED' AND " +
           "r.processingCompletedAt >= :failedSince " +
           "ORDER BY r.createdAt ASC")
    List<Report> findFailedReportsForRetry(@Param("failedSince") LocalDateTime failedSince);

    /**
     * Updates report status in batch for multiple reports.
     * Supports bulk status updates for administrative operations and
     * batch processing, improving performance for mass operations.
     * 
     * @param reportIds list of report IDs to update
     * @param newStatus the new status to set for all specified reports
     * @return number of reports updated
     */
    @Modifying
    @Transactional
    @Query("UPDATE Report r SET r.status = :newStatus WHERE r.id IN :reportIds")
    int updateReportStatusBatch(@Param("reportIds") List<Long> reportIds,
                               @Param("newStatus") Report.Status newStatus);

    /**
     * Finds reports by format type for format-specific processing.
     * Supports format-based report management and processing workflows,
     * enabling different handling strategies for different output formats.
     * 
     * @param format the report format to search for
     * @param status optional status filter
     * @return list of reports matching the format and status criteria
     */
    List<Report> findByFormatAndStatus(Report.Format format, Report.Status status);

    /**
     * Calculates total file size for reports within a date range.
     * Provides storage utilization metrics for capacity planning and
     * resource management, supporting operational monitoring requirements.
     * 
     * @param startDate the start of the calculation period
     * @param endDate the end of the calculation period
     * @return sum of file sizes for reports in the specified period
     */
    @Query("SELECT COALESCE(SUM(r.fileSizeBytes), 0) FROM Report r WHERE " +
           "r.createdAt >= :startDate AND r.createdAt <= :endDate AND " +
           "r.status = 'COMPLETED'")
    Long calculateTotalFileSizeByDateRange(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);
}