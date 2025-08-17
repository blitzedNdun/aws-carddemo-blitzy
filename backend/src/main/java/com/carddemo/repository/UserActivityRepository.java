/*
 * CardDemo - Credit Card Management System
 * 
 * User Activity Repository Interface
 * 
 * This Spring Data JPA repository interface provides comprehensive data access methods
 * for user activity tracking and reporting. It supports administrative reporting
 * capabilities including user login patterns, session duration analytics, and 
 * activity trend analysis with advanced filtering and aggregation functions.
 * 
 * The repository follows the Spring Data JPA repository pattern and provides
 * both standard CRUD operations and custom query methods optimized for
 * PostgreSQL database operations within the CardDemo modernization architecture.
 * 
 * Key Features:
 * - Date range filtering for activity analysis
 * - Session duration analytics and reporting
 * - User behavior pattern analysis
 * - Administrative dashboard support
 * - High-performance query optimization
 * 
 * @since 1.0
 * @version 1.0
 */
package com.carddemo.repository;

import com.carddemo.entity.UserActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

/**
 * Spring Data JPA repository interface for UserActivity entity persistence and querying.
 * 
 * This repository extends JpaRepository to provide standard CRUD operations while also
 * defining custom query methods for complex user activity analysis and reporting.
 * The interface supports the administrative reporting requirements for user login
 * patterns, session duration analytics, and activity trend analysis.
 * 
 * All query methods are optimized for PostgreSQL and support pagination for large
 * datasets. The repository integrates with Spring Security for user context tracking
 * and provides comprehensive audit trail capabilities.
 * 
 * Query Performance Optimizations:
 * - Index utilization for date range queries
 * - Pagination support for large result sets
 * - Native SQL for complex aggregations
 * - Projection interfaces for summary reports
 * 
 * Database Compatibility:
 * - Optimized for PostgreSQL 15.x
 * - Composite primary key support
 * - B-tree index utilization
 * - Transaction isolation compliance
 */
@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    /**
     * Find user activities by user ID within a specific date range.
     * 
     * This method supports administrative reporting by retrieving all activity
     * records for a specific user within a defined time period. Results are
     * ordered by activity timestamp in descending order (most recent first).
     * 
     * Query Performance:
     * - Utilizes composite index on (userId, activityDate)
     * - Supports pagination for large result sets
     * - Optimized for PostgreSQL B-tree index scans
     * 
     * @param userId The unique identifier of the user
     * @param startDate The start date of the activity range (inclusive)
     * @param endDate The end date of the activity range (inclusive)
     * @param pageable Pagination parameters for large result sets
     * @return Page of UserActivity entities matching the criteria
     */
    @Query("SELECT ua FROM UserActivity ua WHERE ua.userId = :userId " +
           "AND ua.activityDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ua.activityTimestamp DESC")
    Page<UserActivity> findByUserIdAndDateBetween(
        @Param("userId") String userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable
    );

    /**
     * Find user activities by activity type and specific date.
     * 
     * This method enables analysis of specific activity types (login, logout,
     * transaction, etc.) for a given date. Supports administrative monitoring
     * of system usage patterns and security audit requirements.
     * 
     * Activity Types:
     * - LOGIN: User authentication events
     * - LOGOUT: User session termination events
     * - TRANSACTION: Financial transaction processing
     * - VIEW: Data access and viewing operations
     * - UPDATE: Data modification operations
     * 
     * @param activityType The type of activity to filter by
     * @param activityDate The specific date to analyze
     * @param pageable Pagination parameters for result set management
     * @return Page of UserActivity entities for the specified type and date
     */
    @Query("SELECT ua FROM UserActivity ua WHERE ua.activityType = :activityType " +
           "AND ua.activityDate = :activityDate " +
           "ORDER BY ua.activityTimestamp ASC")
    Page<UserActivity> findByActivityTypeAndDate(
        @Param("activityType") String activityType,
        @Param("activityDate") LocalDate activityDate,
        Pageable pageable
    );

    /**
     * Find user activities where session duration exceeds specified threshold.
     * 
     * This method supports security monitoring by identifying extended user
     * sessions that may require administrative attention. Used for compliance
     * reporting and session timeout policy enforcement.
     * 
     * Session Duration Analysis:
     * - Identifies long-running sessions for security review
     * - Supports timeout policy compliance monitoring
     * - Enables resource utilization analysis
     * - Facilitates user behavior pattern identification
     * 
     * @param durationMinutes Minimum session duration in minutes
     * @param pageable Pagination parameters for result management
     * @return Page of UserActivity entities with extended session durations
     */
    @Query("SELECT ua FROM UserActivity ua WHERE ua.sessionDurationMinutes > :durationMinutes " +
           "ORDER BY ua.sessionDurationMinutes DESC")
    Page<UserActivity> findBySessionDurationGreaterThan(
        @Param("durationMinutes") Integer durationMinutes,
        Pageable pageable
    );

    /**
     * Count total user activities within a specified date range.
     * 
     * This method provides aggregate statistics for administrative dashboards
     * and capacity planning analysis. Supports trend analysis and system
     * utilization reporting across configurable time periods.
     * 
     * Reporting Applications:
     * - Daily activity volume tracking
     * - Monthly trend analysis
     * - System capacity planning
     * - Performance baseline establishment
     * 
     * @param startDate The start date of the counting range (inclusive)
     * @param endDate The end date of the counting range (inclusive)
     * @return Total count of activities within the specified date range
     */
    @Query("SELECT COUNT(ua) FROM UserActivity ua WHERE ua.activityDate BETWEEN :startDate AND :endDate")
    Long countByDateBetween(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find most active users based on activity frequency within date range.
     * 
     * This method identifies users with the highest activity levels for
     * administrative reporting and user behavior analysis. Results are
     * aggregated by user ID and ordered by activity count in descending order.
     * 
     * Analysis Capabilities:
     * - Top user identification for resource planning
     * - Activity pattern recognition for security monitoring
     * - User engagement metrics for business analysis
     * - System load distribution assessment
     * 
     * Query Optimization:
     * - Native SQL for optimal aggregation performance
     * - PostgreSQL-specific GROUP BY and ORDER BY optimization
     * - Configurable result limit for dashboard display
     * 
     * @param startDate The start date for activity analysis (inclusive)
     * @param endDate The end date for activity analysis (inclusive)
     * @param limit Maximum number of users to return
     * @return List of UserActivitySummary projections with user ID and activity count
     */
    @Query(value = "SELECT ua.user_id as userId, COUNT(*) as activityCount, " +
                   "MAX(ua.activity_timestamp) as lastActivityTimestamp " +
                   "FROM user_activity ua " +
                   "WHERE ua.activity_date BETWEEN :startDate AND :endDate " +
                   "GROUP BY ua.user_id " +
                   "ORDER BY COUNT(*) DESC " +
                   "LIMIT :limit",
           nativeQuery = true)
    List<UserActivitySummary> findMostActiveUsers(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("limit") Integer limit
    );

    /**
     * Calculate average session duration for all users within date range.
     * 
     * This method computes aggregate session duration statistics for system
     * performance analysis and user behavior reporting. Supports administrative
     * dashboard metrics and session timeout policy optimization.
     * 
     * Calculation Method:
     * - Aggregates all session durations within specified date range
     * - Excludes null or zero duration sessions from calculation
     * - Returns average duration in minutes with decimal precision
     * - Optimized for PostgreSQL aggregate function performance
     * 
     * Business Applications:
     * - Session timeout policy optimization
     * - User engagement measurement
     * - System resource planning
     * - Performance baseline establishment
     * 
     * @param startDate The start date for duration calculation (inclusive)
     * @param endDate The end date for duration calculation (inclusive)
     * @return Average session duration in minutes as BigDecimal for precision
     */
    @Query("SELECT AVG(ua.sessionDurationMinutes) FROM UserActivity ua " +
           "WHERE ua.activityDate BETWEEN :startDate AND :endDate " +
           "AND ua.sessionDurationMinutes IS NOT NULL " +
           "AND ua.sessionDurationMinutes > 0")
    BigDecimal calculateAverageSessionDuration(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find user activities by user ID and activity type within date range.
     * 
     * This method provides detailed activity filtering combining user identity,
     * activity type, and temporal constraints. Supports comprehensive audit
     * trail analysis and user-specific behavior monitoring.
     * 
     * Security Applications:
     * - User-specific audit trail generation
     * - Activity type compliance monitoring
     * - Behavioral pattern analysis
     * - Security incident investigation
     * 
     * @param userId The unique identifier of the user
     * @param activityType The type of activity to filter by
     * @param startDate The start date of the activity range (inclusive)
     * @param endDate The end date of the activity range (inclusive)
     * @param pageable Pagination parameters for result management
     * @return Page of UserActivity entities matching all specified criteria
     */
    @Query("SELECT ua FROM UserActivity ua WHERE ua.userId = :userId " +
           "AND ua.activityType = :activityType " +
           "AND ua.activityDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ua.activityTimestamp DESC")
    Page<UserActivity> findByUserIdAndActivityTypeAndDateBetween(
        @Param("userId") String userId,
        @Param("activityType") String activityType,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable
    );

    /**
     * Find daily activity statistics grouped by date within range.
     * 
     * This method generates daily activity summaries for trend analysis
     * and administrative reporting. Results include activity counts,
     * unique user counts, and average session durations by date.
     * 
     * Statistical Output:
     * - Daily activity volume metrics
     * - Unique user engagement tracking
     * - Session duration trend analysis
     * - System utilization patterns
     * 
     * @param startDate The start date for statistical analysis (inclusive)
     * @param endDate The end date for statistical analysis (inclusive)
     * @return List of DailyActivityStats projections with aggregated metrics
     */
    @Query(value = "SELECT ua.activity_date as activityDate, " +
                   "COUNT(*) as totalActivities, " +
                   "COUNT(DISTINCT ua.user_id) as uniqueUsers, " +
                   "AVG(ua.session_duration_minutes) as avgSessionDuration " +
                   "FROM user_activity ua " +
                   "WHERE ua.activity_date BETWEEN :startDate AND :endDate " +
                   "GROUP BY ua.activity_date " +
                   "ORDER BY ua.activity_date ASC",
           nativeQuery = true)
    List<DailyActivityStats> findDailyActivityStatistics(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Projection interface for user activity summary reporting.
     * 
     * This interface defines the contract for user activity aggregation
     * results returned by the findMostActiveUsers query method. Provides
     * read-only access to calculated metrics for administrative dashboards.
     */
    interface UserActivitySummary {
        String getUserId();
        Long getActivityCount();
        LocalDateTime getLastActivityTimestamp();
    }

    /**
     * Projection interface for daily activity statistics reporting.
     * 
     * This interface defines the contract for daily aggregated activity
     * metrics returned by the findDailyActivityStatistics query method.
     * Supports trend analysis and system utilization reporting.
     */
    interface DailyActivityStats {
        LocalDate getActivityDate();
        Long getTotalActivities();
        Long getUniqueUsers();
        BigDecimal getAvgSessionDuration();
    }
}

