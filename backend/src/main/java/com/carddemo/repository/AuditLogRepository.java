/*
 * AuditLogRepository.java
 * 
 * Spring Data JPA repository interface for audit log persistence and retrieval operations.
 * Provides optimized query methods for security audit trail management, compliance reporting,
 * and audit log analysis with support for date range filtering, user-based queries, and
 * event type categorization.
 * 
 * This repository supports the comprehensive audit logging framework requirements outlined
 * in the Security Architecture documentation (Section 6.4), providing enterprise-grade
 * audit trail capabilities for regulatory compliance and security monitoring.
 */
package com.carddemo.repository;

import com.carddemo.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for AuditLog entity persistence and retrieval.
 * 
 * Provides comprehensive audit log management capabilities including:
 * - Basic CRUD operations through JpaRepository extension
 * - Optimized query methods for audit log filtering and retrieval
 * - Date range filtering for compliance reporting
 * - User-based audit trail queries for security analysis
 * - Event type categorization for monitoring dashboards
 * - Pagination support for efficient handling of large audit datasets
 * - Native SQL queries for complex audit reporting requirements
 * 
 * All query methods are optimized for PostgreSQL performance with appropriate
 * indexing strategies on frequently accessed audit fields as documented in
 * the Database Design section (6.2).
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Retrieves audit logs for a specific username within a date range.
     * Optimized for user-specific audit trail analysis and compliance reporting.
     * 
     * @param username The username to filter audit logs by
     * @param startTime The start of the date range (inclusive)
     * @param endTime The end of the date range (inclusive)
     * @param pageable Pagination parameters for large result sets
     * @return Page of audit logs matching the criteria, sorted by timestamp descending
     */
    Page<AuditLog> findByUsernameAndTimestampBetween(
            @Param("username") String username,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    /**
     * Retrieves audit logs by event type within a specified time range.
     * Essential for monitoring specific types of security events and generating
     * event-specific compliance reports.
     * 
     * @param eventType The type of audit event to filter by
     * @param startTime The start of the date range (inclusive)
     * @param endTime The end of the date range (inclusive)
     * @param pageable Pagination parameters for large result sets
     * @return Page of audit logs matching the event type and time criteria
     */
    Page<AuditLog> findByEventTypeAndTimestampBetween(
            @Param("eventType") String eventType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    /**
     * Retrieves audit logs from a specific source IP address within a time range.
     * Critical for security incident investigation and IP-based access analysis.
     * 
     * @param sourceIp The source IP address to filter by
     * @param startTime The start of the date range (inclusive)
     * @param endTime The end of the date range (inclusive)
     * @param pageable Pagination parameters for large result sets
     * @return Page of audit logs from the specified IP address
     */
    Page<AuditLog> findBySourceIpAndTimestampBetween(
            @Param("sourceIp") String sourceIp,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    /**
     * Finds audit logs by correlation ID for tracking related audit events.
     * Enables correlation analysis of multi-step transactions and security workflows.
     * 
     * @param correlationId The correlation ID to search for
     * @return List of audit logs with the specified correlation ID
     */
    List<AuditLog> findByCorrelationId(@Param("correlationId") String correlationId);

    /**
     * Retrieves audit logs by outcome status for analyzing security event results.
     * Useful for monitoring authentication failures, authorization denials, and
     * other security-relevant outcomes.
     * 
     * @param outcome The outcome status to filter by (e.g., "SUCCESS", "FAILURE", "DENIED")
     * @param pageable Pagination parameters for large result sets
     * @return Page of audit logs with the specified outcome
     */
    Page<AuditLog> findByOutcome(@Param("outcome") String outcome, Pageable pageable);

    /**
     * Custom query for comprehensive audit log retrieval within a date range.
     * Optimized native SQL query for high-performance audit log analysis with
     * proper indexing utilization on timestamp columns.
     * 
     * @param startDate The start date for the query range
     * @param endDate The end date for the query range
     * @param pageable Pagination parameters for large result sets
     * @return Page of audit logs within the specified date range
     */
    @Query(value = "SELECT a.* FROM audit_log a " +
                   "WHERE a.timestamp >= :startDate AND a.timestamp <= :endDate " +
                   "ORDER BY a.timestamp DESC",
           countQuery = "SELECT COUNT(*) FROM audit_log a " +
                       "WHERE a.timestamp >= :startDate AND a.timestamp <= :endDate",
           nativeQuery = true)
    Page<AuditLog> findAuditLogsByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * Custom query for user-specific audit log analysis with advanced filtering.
     * Combines username filtering with optional event type and outcome filtering
     * for comprehensive user activity analysis.
     * 
     * @param username The username to analyze
     * @param eventType Optional event type filter (can be null)
     * @param outcome Optional outcome filter (can be null)
     * @param startDate The start date for analysis
     * @param endDate The end date for analysis
     * @param pageable Pagination parameters for large result sets
     * @return Page of filtered audit logs for the specified user
     */
    @Query("SELECT a FROM AuditLog a " +
           "WHERE a.username = :username " +
           "AND (:eventType IS NULL OR a.eventType = :eventType) " +
           "AND (:outcome IS NULL OR a.outcome = :outcome) " +
           "AND a.timestamp BETWEEN :startDate AND :endDate " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> findAuditLogsByUser(
            @Param("username") String username,
            @Param("eventType") String eventType,
            @Param("outcome") String outcome,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * Deletes audit logs older than the specified timestamp for retention management.
     * Critical for implementing audit log retention policies and managing storage costs.
     * Uses batch deletion to efficiently remove large numbers of old audit records.
     * 
     * @param cutoffDate The cutoff date - audit logs older than this will be deleted
     * @return The number of audit log records deleted
     */
    @Query("DELETE FROM AuditLog a WHERE a.timestamp < :cutoffDate")
    int deleteByTimestampBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Finds audit logs with failed authentication attempts for security monitoring.
     * Specifically targets authentication events with failure outcomes for
     * security incident detection and brute force attack monitoring.
     * 
     * @param startTime The start time for the monitoring period
     * @param endTime The end time for the monitoring period
     * @param pageable Pagination parameters for large result sets
     * @return Page of failed authentication audit logs
     */
    @Query("SELECT a FROM AuditLog a " +
           "WHERE a.eventType = 'AUTHENTICATION' " +
           "AND a.outcome = 'FAILURE' " +
           "AND a.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> findFailedAuthenticationAttempts(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    /**
     * Retrieves authorization denial events for security policy analysis.
     * Identifies patterns in access denials for security policy refinement
     * and compliance reporting.
     * 
     * @param startTime The start time for the analysis period
     * @param endTime The end time for the analysis period
     * @param pageable Pagination parameters for large result sets
     * @return Page of authorization denial audit logs
     */
    @Query("SELECT a FROM AuditLog a " +
           "WHERE a.eventType = 'AUTHORIZATION' " +
           "AND a.outcome = 'DENIED' " +
           "AND a.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> findAuthorizationDenials(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    /**
     * Finds audit logs by resource accessed for resource-specific security analysis.
     * Enables monitoring of access patterns to sensitive resources and compliance
     * reporting for specific data or functionality access.
     * 
     * @param resourceAccessed The resource identifier to filter by
     * @param startTime The start time for the analysis period
     * @param endTime The end time for the analysis period
     * @param pageable Pagination parameters for large result sets
     * @return Page of audit logs for the specified resource
     */
    Page<AuditLog> findByResourceAccessedAndTimestampBetween(
            @Param("resourceAccessed") String resourceAccessed,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    /**
     * Counts audit events by type within a time period for dashboard metrics.
     * Provides aggregated counts of different audit event types for security
     * dashboards and monitoring systems.
     * 
     * @param startTime The start time for the counting period
     * @param endTime The end time for the counting period
     * @return List of Object arrays containing [eventType, count] pairs
     */
    @Query("SELECT a.eventType, COUNT(a) FROM AuditLog a " +
           "WHERE a.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY a.eventType " +
           "ORDER BY COUNT(a) DESC")
    List<Object[]> countAuditEventsByType(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Finds audit logs with integrity hash mismatches for forensic analysis.
     * Critical for detecting potential audit log tampering and maintaining
     * audit trail integrity for regulatory compliance.
     * 
     * @param pageable Pagination parameters for investigation results
     * @return Page of audit logs with potential integrity issues
     */
    @Query("SELECT a FROM AuditLog a " +
           "WHERE a.integrityHash IS NULL " +
           "OR a.integrityHash = '' " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> findAuditLogsWithIntegrityIssues(Pageable pageable);

    /**
     * Retrieves the most recent audit log entry for a specific user.
     * Useful for tracking last activity and session management.
     * 
     * @param username The username to find the last activity for
     * @return Optional containing the most recent audit log entry for the user
     */
    Optional<AuditLog> findFirstByUsernameOrderByTimestampDesc(@Param("username") String username);

    /**
     * Finds audit logs for compliance reporting with advanced filtering.
     * Supports regulatory compliance requirements by providing comprehensive
     * audit trail queries with multiple filter criteria.
     * 
     * @param eventTypes List of event types to include in the report
     * @param startDate The start date for the compliance period
     * @param endDate The end date for the compliance period
     * @param pageable Pagination parameters for large compliance reports
     * @return Page of audit logs matching compliance criteria
     */
    @Query("SELECT a FROM AuditLog a " +
           "WHERE a.eventType IN :eventTypes " +
           "AND a.timestamp BETWEEN :startDate AND :endDate " +
           "ORDER BY a.timestamp ASC")
    Page<AuditLog> findComplianceAuditLogs(
            @Param("eventTypes") List<String> eventTypes,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * Advanced audit log search with multiple criteria filtering.
     * Supports comprehensive audit log searching with multiple optional filter criteria
     * for security incident investigation, compliance audits, and operational monitoring.
     * 
     * @param username Optional username filter (can be null)
     * @param eventType Optional event type filter (can be null)
     * @param outcome Optional outcome filter (can be null)
     * @param sourceIp Optional source IP filter (can be null)
     * @param startDate Optional start date filter (can be null)
     * @param endDate Optional end date filter (can be null)
     * @param pageable Pagination parameters for large result sets
     * @return Page of audit logs matching the specified criteria
     */
    @Query("SELECT a FROM AuditLog a " +
           "WHERE (:username IS NULL OR a.username = :username) " +
           "AND (:eventType IS NULL OR a.eventType = :eventType) " +
           "AND (:outcome IS NULL OR a.outcome = :outcome) " +
           "AND (:sourceIp IS NULL OR a.sourceIp = :sourceIp) " +
           "AND (:startDate IS NULL OR a.timestamp >= :startDate) " +
           "AND (:endDate IS NULL OR a.timestamp <= :endDate) " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> findAuditLogsByAdvancedCriteria(
            @Param("username") String username,
            @Param("eventType") String eventType,
            @Param("outcome") String outcome,
            @Param("sourceIp") String sourceIp,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}