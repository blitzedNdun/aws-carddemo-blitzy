/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.repository;

import com.carddemo.entity.Notification;
import com.carddemo.entity.Notification.DeliveryStatus;
import com.carddemo.entity.Notification.NotificationType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for Notification entity providing CRUD operations,
 * custom query methods, and batch operations for notification data access including
 * delivery status tracking, template queries, and notification archival.
 * 
 * This repository supports multi-channel notification management across EMAIL, SMS, 
 * IN_APP, and PUSH notification types with comprehensive delivery tracking, retry
 * mechanisms, and template-based content generation. Provides efficient query methods
 * for notification filtering, reporting, and bulk operations.
 * 
 * Key Features:
 * - Customer-specific notification queries with pagination support
 * - Delivery status filtering and metrics collection
 * - Template usage tracking and reporting
 * - Channel-specific query operations
 * - Retry queue management for failed notifications
 * - Archival and cleanup operations for data retention
 * - Bulk update operations for status management
 * - Native SQL queries for complex reporting requirements
 * 
 * Performance Considerations:
 * - Optimized for high-volume notification processing
 * - Efficient retry queue management with priority ordering
 * - Template caching support through query optimization
 * - Bulk processing capabilities for batch notification operations
 * - Index-aware query design for sub-200ms response times
 * 
 * Database Integration:
 * - Maps to PostgreSQL notifications table with comprehensive indexing
 * - Supports partitioned tables for efficient archival
 * - Utilizes foreign key relationships with Customer entity
 * - Implements transaction management for ACID compliance
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // =============================================================================
    // CUSTOMER-SPECIFIC NOTIFICATION QUERIES
    // =============================================================================

    /**
     * Find all notifications for a specific customer with pagination support.
     * Retrieves notifications ordered by creation date (newest first) for
     * customer notification history and management.
     * 
     * @param customerId the customer ID to search for
     * @param pageable pagination and sorting parameters
     * @return page of notifications for the specified customer
     */
    @Query("SELECT n FROM Notification n WHERE n.customer.customerId = :customerId ORDER BY n.createdAt DESC")
    Page<Notification> findByCustomerId(@Param("customerId") Long customerId, Pageable pageable);

    /**
     * Find all notifications for a specific customer without pagination.
     * Used for bulk operations and complete customer notification retrieval.
     * 
     * @param customerId the customer ID to search for
     * @return list of all notifications for the specified customer
     */
    @Query("SELECT n FROM Notification n WHERE n.customer.customerId = :customerId ORDER BY n.createdAt DESC")
    List<Notification> findByCustomerId(@Param("customerId") Long customerId);

    // =============================================================================
    // DELIVERY STATUS FILTERING AND TRACKING
    // =============================================================================

    /**
     * Find notifications by delivery status with pagination support.
     * Essential for monitoring notification delivery performance and
     * identifying delivery issues across the system.
     * 
     * @param status the delivery status to filter by
     * @param pageable pagination and sorting parameters
     * @return page of notifications with the specified delivery status
     */
    @Query("SELECT n FROM Notification n WHERE n.deliveryStatus = :status ORDER BY n.createdAt DESC")
    Page<Notification> findByStatus(@Param("status") DeliveryStatus status, Pageable pageable);

    /**
     * Find notifications by delivery status without pagination.
     * Used for batch processing and bulk status updates.
     * 
     * @param status the delivery status to filter by
     * @return list of notifications with the specified delivery status
     */
    @Query("SELECT n FROM Notification n WHERE n.deliveryStatus = :status ORDER BY n.createdAt DESC")
    List<Notification> findByStatus(@Param("status") DeliveryStatus status);

    /**
     * Find notifications by customer ID and delivery status.
     * Combines customer filtering with status tracking for targeted
     * customer notification management and support operations.
     * 
     * @param customerId the customer ID to search for
     * @param status the delivery status to filter by
     * @param pageable pagination and sorting parameters
     * @return page of notifications matching both criteria
     */
    @Query("SELECT n FROM Notification n WHERE n.customer.customerId = :customerId AND n.deliveryStatus = :status ORDER BY n.createdAt DESC")
    Page<Notification> findByCustomerIdAndStatus(@Param("customerId") Long customerId, 
                                                @Param("status") DeliveryStatus status, 
                                                Pageable pageable);

    // =============================================================================
    // TEMPLATE USAGE TRACKING AND REPORTING
    // =============================================================================

    /**
     * Find notifications by template ID with pagination support.
     * Enables template usage analysis, performance tracking, and
     * template effectiveness measurement across notification campaigns.
     * 
     * @param templateId the template ID to search for
     * @param pageable pagination and sorting parameters
     * @return page of notifications using the specified template
     */
    @Query("SELECT n FROM Notification n WHERE n.templateId = :templateId ORDER BY n.createdAt DESC")
    Page<Notification> findByTemplateId(@Param("templateId") String templateId, Pageable pageable);

    /**
     * Find notifications by template ID without pagination.
     * Used for template usage analytics and bulk template operations.
     * 
     * @param templateId the template ID to search for
     * @return list of notifications using the specified template
     */
    @Query("SELECT n FROM Notification n WHERE n.templateId = :templateId ORDER BY n.createdAt DESC")
    List<Notification> findByTemplateId(@Param("templateId") String templateId);

    /**
     * Count notifications by template ID and status for template performance analysis.
     * Provides metrics on template effectiveness including delivery success rates
     * and failure patterns for optimization and troubleshooting.
     * 
     * @param templateId the template ID to analyze
     * @param status the delivery status to count
     * @return count of notifications matching template and status criteria
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.templateId = :templateId AND n.deliveryStatus = :status")
    long countByTemplateIdAndStatus(@Param("templateId") String templateId, @Param("status") DeliveryStatus status);

    // =============================================================================
    // CHANNEL-SPECIFIC QUERY OPERATIONS
    // =============================================================================

    /**
     * Find notifications by delivery channel (notification type) with pagination.
     * Supports channel-specific performance analysis, delivery optimization,
     * and channel effectiveness measurement for multi-channel strategies.
     * 
     * @param notificationType the notification channel/type to filter by
     * @param pageable pagination and sorting parameters
     * @return page of notifications for the specified delivery channel
     */
    @Query("SELECT n FROM Notification n WHERE n.notificationType = :notificationType ORDER BY n.createdAt DESC")
    Page<Notification> findByDeliveryChannel(@Param("notificationType") NotificationType notificationType, Pageable pageable);

    /**
     * Find notifications by delivery channel without pagination.
     * Used for channel analytics and bulk channel operations.
     * 
     * @param notificationType the notification channel/type to filter by
     * @return list of notifications for the specified delivery channel
     */
    @Query("SELECT n FROM Notification n WHERE n.notificationType = :notificationType ORDER BY n.createdAt DESC")
    List<Notification> findByDeliveryChannel(@Param("notificationType") NotificationType notificationType);

    /**
     * Find notifications by customer ID and delivery channel.
     * Enables customer-specific channel preference analysis and
     * personalized notification delivery optimization.
     * 
     * @param customerId the customer ID to search for
     * @param notificationType the notification channel/type to filter by
     * @param pageable pagination and sorting parameters
     * @return page of notifications matching customer and channel criteria
     */
    @Query("SELECT n FROM Notification n WHERE n.customer.customerId = :customerId AND n.notificationType = :notificationType ORDER BY n.createdAt DESC")
    Page<Notification> findByCustomerIdAndDeliveryChannel(@Param("customerId") Long customerId,
                                                         @Param("notificationType") NotificationType notificationType,
                                                         Pageable pageable);

    // =============================================================================
    // RETRY QUEUE MANAGEMENT AND FAILED NOTIFICATION PROCESSING
    // =============================================================================

    /**
     * Find notifications pending retry based on retry eligibility criteria.
     * Identifies failed notifications that haven't exceeded retry limits and
     * are ready for retry processing based on exponential backoff timing.
     * 
     * @return list of notifications eligible for retry processing
     */
    @Query("SELECT n FROM Notification n WHERE n.deliveryStatus = 'FAILED' AND n.retryCount < n.maxRetries AND n.sentAt <= :retryTime ORDER BY n.priority DESC, n.createdAt ASC")
    List<Notification> findPendingRetries(@Param("retryTime") LocalDateTime retryTime);

    /**
     * Find notifications pending retry with pagination support.
     * Supports batch retry processing with configurable batch sizes
     * for efficient retry queue management and system resource optimization.
     * 
     * @param retryTime the current time minus appropriate backoff interval
     * @param pageable pagination parameters for batch processing
     * @return page of notifications eligible for retry processing
     */
    @Query("SELECT n FROM Notification n WHERE n.deliveryStatus = 'FAILED' AND n.retryCount < n.maxRetries AND n.sentAt <= :retryTime ORDER BY n.priority DESC, n.createdAt ASC")
    Page<Notification> findPendingRetries(@Param("retryTime") LocalDateTime retryTime, Pageable pageable);

    /**
     * Find failed notifications by priority for intelligent retry processing.
     * Enables priority-based retry queue management ensuring high-priority
     * notifications are retried first for optimal user experience.
     * 
     * @param priority the priority level to filter by
     * @return list of failed notifications with specified priority
     */
    @Query("SELECT n FROM Notification n WHERE n.deliveryStatus = 'FAILED' AND n.priority = :priority AND n.retryCount < n.maxRetries ORDER BY n.createdAt ASC")
    List<Notification> findFailedNotificationsByPriority(@Param("priority") Integer priority);

    /**
     * Find notifications that have exceeded maximum retry attempts.
     * Identifies permanently failed notifications for administrative
     * review, escalation, or alternative delivery mechanisms.
     * 
     * @param pageable pagination parameters for result management
     * @return page of notifications that have exceeded retry limits
     */
    @Query("SELECT n FROM Notification n WHERE n.deliveryStatus = 'FAILED' AND n.retryCount >= n.maxRetries ORDER BY n.createdAt DESC")
    Page<Notification> findExhaustedRetries(Pageable pageable);

    // =============================================================================
    // BULK UPDATE OPERATIONS FOR STATUS MANAGEMENT
    // =============================================================================

    /**
     * Update delivery status for a specific notification.
     * Provides atomic status updates with timestamp tracking for
     * accurate delivery state management and audit trail maintenance.
     * 
     * @param notificationId the notification ID to update
     * @param status the new delivery status
     * @param updateTime the timestamp of the status update
     * @return number of notifications updated (should be 1 if successful)
     */
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.deliveryStatus = :status, n.sentAt = :updateTime WHERE n.id = :notificationId")
    int updateDeliveryStatus(@Param("notificationId") Long notificationId, 
                           @Param("status") DeliveryStatus status, 
                           @Param("updateTime") LocalDateTime updateTime);

    /**
     * Update delivery status and increment retry count for failed notifications.
     * Supports retry mechanism implementation with atomic retry count
     * management and status tracking for failed delivery attempts.
     * 
     * @param notificationId the notification ID to update
     * @param status the new delivery status (typically RETRY or FAILED)
     * @param retryCount the new retry count
     * @param failureReason the reason for delivery failure
     * @return number of notifications updated (should be 1 if successful)
     */
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.deliveryStatus = :status, n.retryCount = :retryCount, n.failureReason = :failureReason WHERE n.id = :notificationId")
    int updateDeliveryStatusWithRetry(@Param("notificationId") Long notificationId,
                                    @Param("status") DeliveryStatus status,
                                    @Param("retryCount") Integer retryCount,
                                    @Param("failureReason") String failureReason);

    /**
     * Mark notification as delivered with delivery timestamp.
     * Atomic operation for successful delivery confirmation including
     * timestamp recording for delivery metrics and SLA tracking.
     * 
     * @param notificationId the notification ID to mark as delivered
     * @param deliveredAt the delivery confirmation timestamp
     * @return number of notifications updated (should be 1 if successful)
     */
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.deliveryStatus = 'DELIVERED', n.deliveredAt = :deliveredAt WHERE n.id = :notificationId")
    int markAsDelivered(@Param("notificationId") Long notificationId, @Param("deliveredAt") LocalDateTime deliveredAt);

    /**
     * Bulk update delivery status for multiple notifications.
     * Efficient batch operation for status updates across notification
     * sets, supporting bulk processing and system maintenance operations.
     * 
     * @param notificationIds list of notification IDs to update
     * @param status the new delivery status for all notifications
     * @param updateTime the timestamp of the bulk update
     * @return number of notifications updated
     */
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.deliveryStatus = :status, n.sentAt = :updateTime WHERE n.id IN :notificationIds")
    int bulkUpdateDeliveryStatus(@Param("notificationIds") List<Long> notificationIds,
                               @Param("status") DeliveryStatus status,
                               @Param("updateTime") LocalDateTime updateTime);

    // =============================================================================
    // METRICS AND COUNTING OPERATIONS
    // =============================================================================

    /**
     * Count notifications by customer ID and delivery status.
     * Provides customer-specific notification metrics for dashboard
     * displays, customer service operations, and delivery analytics.
     * 
     * @param customerId the customer ID to count for
     * @param status the delivery status to count
     * @return count of notifications matching the criteria
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.customer.customerId = :customerId AND n.deliveryStatus = :status")
    long countByCustomerIdAndStatus(@Param("customerId") Long customerId, @Param("status") DeliveryStatus status);

    /**
     * Count notifications by delivery channel and status.
     * Enables channel performance analysis and delivery success
     * rate measurement across different notification types.
     * 
     * @param notificationType the notification channel/type to count
     * @param status the delivery status to count
     * @return count of notifications matching the criteria
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.notificationType = :notificationType AND n.deliveryStatus = :status")
    long countByDeliveryChannelAndStatus(@Param("notificationType") NotificationType notificationType, 
                                       @Param("status") DeliveryStatus status);

    /**
     * Count notifications created within a date range.
     * Supports time-based analytics, volume trending, and
     * capacity planning for notification system scaling.
     * 
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return count of notifications created within the specified range
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.createdAt BETWEEN :startDate AND :endDate")
    long countByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Count pending notifications (not yet sent) by priority.
     * Provides queue depth metrics for priority-based notification
     * processing and system load balancing operations.
     * 
     * @param priority the priority level to count
     * @return count of pending notifications with specified priority
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.deliveryStatus = 'PENDING' AND n.priority = :priority")
    long countPendingByPriority(@Param("priority") Integer priority);

    // =============================================================================
    // ARCHIVAL AND CLEANUP OPERATIONS FOR DATA RETENTION
    // =============================================================================

    /**
     * Find old notifications for archival based on age criteria.
     * Identifies notifications created before the specified cutoff date
     * for archival processing and long-term storage management.
     * 
     * @param cutoffDate the date before which notifications are considered old
     * @param pageable pagination parameters for batch archival processing
     * @return page of notifications eligible for archival
     */
    @Query("SELECT n FROM Notification n WHERE n.createdAt < :cutoffDate ORDER BY n.createdAt ASC")
    Page<Notification> findOldNotifications(@Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);

    /**
     * Find old notifications by status for selective archival.
     * Enables status-specific archival policies where only certain
     * notification states are archived while others are retained.
     * 
     * @param cutoffDate the date before which notifications are considered old
     * @param status the delivery status to filter for archival
     * @param pageable pagination parameters for batch processing
     * @return page of old notifications with specified status
     */
    @Query("SELECT n FROM Notification n WHERE n.createdAt < :cutoffDate AND n.deliveryStatus = :status ORDER BY n.createdAt ASC")
    Page<Notification> findOldNotificationsByStatus(@Param("cutoffDate") LocalDateTime cutoffDate,
                                                   @Param("status") DeliveryStatus status,
                                                   Pageable pageable);

    /**
     * Count old notifications for archival planning.
     * Provides metrics for archival capacity planning and
     * storage management without loading notification data.
     * 
     * @param cutoffDate the date before which notifications are considered old
     * @return count of notifications eligible for archival
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.createdAt < :cutoffDate")
    long countOldNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Delete old notifications based on retention policy.
     * Permanent deletion operation for notifications that have
     * exceeded the system retention period and are no longer needed.
     * 
     * @param cutoffDate the date before which notifications should be deleted
     * @return number of notifications deleted
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    int deleteOldNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Delete old notifications by status for selective cleanup.
     * Enables status-specific retention policies where certain
     * notification states are cleaned up while others are preserved.
     * 
     * @param cutoffDate the date before which notifications should be deleted
     * @param status the delivery status to delete
     * @return number of notifications deleted
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate AND n.deliveryStatus = :status")
    int deleteOldNotificationsByStatus(@Param("cutoffDate") LocalDateTime cutoffDate, @Param("status") DeliveryStatus status);

    /**
     * Delete delivered notifications older than specified date.
     * Cleanup operation specifically for successfully delivered notifications
     * that can be safely removed after delivery confirmation retention period.
     * 
     * @param cutoffDate the date before which delivered notifications should be deleted
     * @return number of delivered notifications deleted
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.deliveredAt IS NOT NULL AND n.deliveredAt < :cutoffDate AND n.deliveryStatus = 'DELIVERED'")
    int deleteDeliveredNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    // =============================================================================
    // ADVANCED QUERY OPERATIONS AND REPORTING
    // =============================================================================

    /**
     * Find notifications requiring attention (failed with retries exhausted).
     * Identifies notifications that require manual intervention or
     * alternative delivery mechanisms due to persistent delivery failures.
     * 
     * @param pageable pagination parameters for result management
     * @return page of notifications requiring attention
     */
    @Query("SELECT n FROM Notification n WHERE n.deliveryStatus = 'FAILED' AND n.retryCount >= n.maxRetries AND n.optOutChecked = true ORDER BY n.priority DESC, n.createdAt ASC")
    Page<Notification> findNotificationsRequiringAttention(Pageable pageable);

    /**
     * Find the most recent notification for a customer and template combination.
     * Supports duplicate detection and notification frequency management
     * to prevent notification spam and improve customer experience.
     * 
     * @param customerId the customer ID to search for
     * @param templateId the template ID to search for
     * @return the most recent notification matching the criteria, if any
     */
    @Query("SELECT n FROM Notification n WHERE n.customer.customerId = :customerId AND n.templateId = :templateId ORDER BY n.createdAt DESC")
    Optional<Notification> findMostRecentByCustomerAndTemplate(@Param("customerId") Long customerId, 
                                                              @Param("templateId") String templateId);

    /**
     * Find notifications by customer and date range for customer service.
     * Enables customer service representatives to review recent
     * notification history for customer support and issue resolution.
     * 
     * @param customerId the customer ID to search for
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @param pageable pagination parameters for result management
     * @return page of notifications for the customer within the date range
     */
    @Query("SELECT n FROM Notification n WHERE n.customer.customerId = :customerId AND n.createdAt BETWEEN :startDate AND :endDate ORDER BY n.createdAt DESC")
    Page<Notification> findByCustomerIdAndDateRange(@Param("customerId") Long customerId,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate,
                                                   Pageable pageable);

    /**
     * Native SQL query for complex notification delivery statistics.
     * Provides comprehensive delivery metrics aggregated by channel
     * and status for management reporting and system performance analysis.
     * 
     * @param startDate the start of the reporting period
     * @param endDate the end of the reporting period
     * @return list of objects containing delivery statistics
     */
    @Query(value = """
        SELECT 
            notification_type as channel,
            delivery_status as status,
            COUNT(*) as notification_count,
            AVG(EXTRACT(EPOCH FROM (delivered_at - created_at))/60) as avg_delivery_time_minutes,
            MAX(retry_count) as max_retries_used
        FROM notifications 
        WHERE created_at BETWEEN :startDate AND :endDate 
        GROUP BY notification_type, delivery_status 
        ORDER BY notification_type, delivery_status
        """, nativeQuery = true)
    List<Object[]> getDeliveryStatistics(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Native SQL query for notification volume trends analysis.
     * Provides daily notification volume metrics for capacity planning
     * and trend analysis across the specified time period.
     * 
     * @param startDate the start of the analysis period
     * @param endDate the end of the analysis period
     * @return list of objects containing daily volume data
     */
    @Query(value = """
        SELECT 
            CAST(created_at AS DATE) as notification_date,
            notification_type as channel,
            COUNT(*) as daily_volume,
            COUNT(CASE WHEN delivery_status = 'DELIVERED' THEN 1 END) as successful_deliveries,
            COUNT(CASE WHEN delivery_status = 'FAILED' THEN 1 END) as failed_deliveries
        FROM notifications 
        WHERE created_at BETWEEN :startDate AND :endDate 
        GROUP BY CAST(created_at AS DATE), notification_type 
        ORDER BY notification_date DESC, notification_type
        """, nativeQuery = true)
    List<Object[]> getVolumeStatistics(@Param("startDate") LocalDateTime startDate, 
                                     @Param("endDate") LocalDateTime endDate);

    // =============================================================================
    // UTILITY AND CONVENIENCE METHODS
    // =============================================================================

    /**
     * Check if notifications exist for a specific customer.
     * Quick existence check without loading notification data,
     * useful for customer onboarding and account management operations.
     * 
     * @param customerId the customer ID to check
     * @return true if any notifications exist for the customer
     */
    @Query("SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END FROM Notification n WHERE n.customer.customerId = :customerId")
    boolean existsByCustomerId(@Param("customerId") Long customerId);

    /**
     * Find distinct template IDs used within a date range.
     * Supports template usage analysis and content management
     * for identifying active and inactive notification templates.
     * 
     * @param startDate the start of the date range
     * @param endDate the end of the date range
     * @return list of distinct template IDs used in the period
     */
    @Query("SELECT DISTINCT n.templateId FROM Notification n WHERE n.templateId IS NOT NULL AND n.createdAt BETWEEN :startDate AND :endDate ORDER BY n.templateId")
    List<String> findDistinctTemplateIds(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Find notifications with null or empty channel addresses.
     * Data quality check for identifying notifications with
     * missing delivery addresses that require attention or cleanup.
     * 
     * @param pageable pagination parameters for result management
     * @return page of notifications with missing channel addresses
     */
    @Query("SELECT n FROM Notification n WHERE n.channelAddress IS NULL OR TRIM(n.channelAddress) = '' ORDER BY n.createdAt DESC")
    Page<Notification> findNotificationsWithMissingChannelAddress(Pageable pageable);
}