/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.repository;

import com.carddemo.entity.Authorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for Authorization entity providing CRUD operations 
 * and custom query methods for authorization processing.
 * 
 * This repository interface supports real-time credit card authorization operations including
 * authorization lookups, velocity checking, transaction history retrieval, and fraud detection
 * queries. It implements the repository pattern for the Authorization entity with optimized
 * query methods designed for high-throughput authorization processing.
 * 
 * Key Features:
 * - Real-time authorization lookups by card number and time ranges
 * - Velocity limit checking for fraud prevention  
 * - Transaction history retrieval with sorting and pagination
 * - Fraud detection queries by merchant and approval patterns
 * - Pessimistic locking support for concurrent authorization processing
 * - Sub-200ms query performance for authorization decisions
 * 
 * Database Integration:
 * - Leverages PostgreSQL authorizations table with optimized indexes
 * - Uses composite indexes for efficient multi-column queries
 * - Supports concurrent access with proper locking mechanisms
 * - Implements cursor-based pagination for large result sets
 * 
 * Performance Optimization:
 * - Query methods utilize indexed columns for optimal performance
 * - Supports pagination and sorting for large datasets
 * - Implements efficient counting queries for velocity checks
 * - Uses read-only queries where appropriate for better performance
 * 
 * Security and Compliance:
 * - Supports audit trail requirements for regulatory compliance
 * - Implements secure query patterns for sensitive authorization data
 * - Provides fraud detection and velocity checking capabilities
 * - Maintains transaction integrity through proper locking
 * 
 * This repository supports the migration from COBOL authorization processing
 * while maintaining identical business logic and performance requirements
 * for real-time credit card authorization workflows.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Repository
public interface AuthorizationRepository extends JpaRepository<Authorization, Long> {

    /**
     * Retrieves authorizations by card number within a specified time range.
     * This method supports real-time authorization lookups and transaction history
     * queries for a specific card within a given time period.
     * 
     * Used for:
     * - Recent transaction validation
     * - Authorization history analysis
     * - Duplicate transaction detection
     * - Time-based authorization patterns
     * 
     * Performance: Utilizes auth_card_idx and auth_timestamp_idx for optimal query performance.
     * 
     * @param cardNumber the 16-digit card number to search for
     * @param startTime the start of the time range (inclusive)
     * @param endTime the end of the time range (inclusive)
     * @return list of authorizations for the card within the specified time range, ordered by request timestamp descending
     */
    @Query("SELECT a FROM Authorization a WHERE a.cardNumber = :cardNumber " +
           "AND a.requestTimestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY a.requestTimestamp DESC")
    List<Authorization> findByCardNumberAndTimestampBetween(
            @Param("cardNumber") String cardNumber,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Counts the number of authorizations for a card number after a specified timestamp.
     * This method is primarily used for velocity limit checking to prevent fraud
     * by detecting rapid-fire authorization attempts.
     * 
     * Used for:
     * - Velocity limit enforcement (e.g., max 5 transactions per minute)
     * - Fraud detection based on transaction frequency
     * - Real-time authorization decision making
     * - Risk assessment for authorization approval
     * 
     * Performance: Uses auth_card_idx and auth_timestamp_idx for efficient counting.
     * 
     * @param cardNumber the 16-digit card number to check
     * @param timestamp the timestamp after which to count authorizations
     * @return the count of authorizations for the card after the specified timestamp
     */
    @Query("SELECT COUNT(a) FROM Authorization a WHERE a.cardNumber = :cardNumber " +
           "AND a.requestTimestamp > :timestamp")
    long countByCardNumberAndTimestampAfter(
            @Param("cardNumber") String cardNumber,
            @Param("timestamp") LocalDateTime timestamp);

    /**
     * Retrieves authorizations for a specific account ordered by timestamp in descending order.
     * This method supports transaction history retrieval and account-based authorization
     * analysis for customer service and account management operations.
     * 
     * Used for:
     * - Account transaction history display
     * - Customer service inquiries
     * - Account-level authorization analysis
     * - Recent activity summaries
     * 
     * Performance: Utilizes auth_account_idx and auth_timestamp_idx for optimal performance.
     * 
     * @param accountId the account ID to retrieve authorizations for
     * @return list of authorizations for the account, ordered by request timestamp descending
     */
    @Query("SELECT a FROM Authorization a WHERE a.accountId = :accountId " +
           "ORDER BY a.requestTimestamp DESC")
    List<Authorization> findByAccountIdOrderByTimestampDesc(@Param("accountId") Long accountId);

    /**
     * Retrieves authorizations for a specific merchant with a given approval status.
     * This method supports fraud detection by analyzing merchant-specific approval
     * patterns and identifying potentially fraudulent merchant activity.
     * 
     * Used for:
     * - Merchant fraud detection and analysis
     * - Approval rate monitoring by merchant
     * - Risk assessment for merchant relationships
     * - Compliance reporting and audit trails
     * 
     * Performance: Uses auth_merchant_idx and auth_status_idx for efficient filtering.
     * 
     * @param merchantId the merchant ID to analyze
     * @param approvalStatus the approval status to filter by (APPROVED or DECLINED)
     * @return list of authorizations for the merchant with the specified approval status
     */
    @Query("SELECT a FROM Authorization a WHERE a.merchantId = :merchantId " +
           "AND a.approvalStatus = :approvalStatus " +
           "ORDER BY a.requestTimestamp DESC")
    List<Authorization> findByMerchantIdAndApprovalStatus(
            @Param("merchantId") String merchantId,
            @Param("approvalStatus") String approvalStatus);

    /**
     * Retrieves authorizations with high fraud scores above a specified threshold.
     * This method supports fraud detection and risk management by identifying
     * authorizations that were flagged with elevated fraud risk scores.
     * 
     * Used for:
     * - Fraud pattern analysis and investigation
     * - Risk assessment and monitoring
     * - Compliance reporting for high-risk transactions
     * - Machine learning model validation
     * 
     * Performance: Query is optimized for fraud score analysis and monitoring.
     * 
     * @param fraudScoreThreshold the minimum fraud score threshold (typically 750+ for high risk)
     * @return list of authorizations with fraud scores above the threshold, ordered by fraud score descending
     */
    @Query("SELECT a FROM Authorization a WHERE a.fraudScore >= :fraudScoreThreshold " +
           "ORDER BY a.fraudScore DESC, a.requestTimestamp DESC")
    List<Authorization> findByFraudScoreGreaterThanEqual(@Param("fraudScoreThreshold") Integer fraudScoreThreshold);

    /**
     * Retrieves the most recent authorization for a specific card number.
     * This method supports real-time authorization processing by providing
     * the latest authorization record for comparison and validation.
     * 
     * Used for:
     * - Recent authorization validation
     * - Duplicate transaction detection
     * - Real-time decision making support
     * - Last authorization status checking
     * 
     * Performance: Uses auth_card_idx and leverages database ordering for efficiency.
     * 
     * @param cardNumber the 16-digit card number
     * @return the most recent authorization for the card, or empty if none found
     */
    @Query("SELECT a FROM Authorization a WHERE a.cardNumber = :cardNumber " +
           "ORDER BY a.requestTimestamp DESC LIMIT 1")
    Optional<Authorization> findMostRecentByCardNumber(@Param("cardNumber") String cardNumber);

    /**
     * Retrieves authorizations that failed velocity checks within a time range.
     * This method supports fraud analysis by identifying patterns of velocity
     * check failures that may indicate coordinated fraud attempts.
     * 
     * Used for:
     * - Velocity-based fraud pattern analysis
     * - Authorization failure investigation
     * - Risk monitoring and reporting
     * - Fraud detection system validation
     * 
     * @param startTime the start of the time range to analyze
     * @param endTime the end of the time range to analyze
     * @return list of authorizations with failed velocity checks in the time range
     */
    @Query("SELECT a FROM Authorization a WHERE a.velocityCheckResult = 'FAIL' " +
           "AND a.requestTimestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY a.requestTimestamp DESC")
    List<Authorization> findVelocityFailuresByTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Counts authorizations by approval status within a time range.
     * This method supports operational reporting and monitoring by providing
     * approval/decline statistics for specified time periods.
     * 
     * Used for:
     * - Operational dashboard metrics
     * - Approval rate monitoring
     * - Performance analysis and reporting
     * - SLA compliance tracking
     * 
     * @param approvalStatus the approval status to count (APPROVED or DECLINED)
     * @param startTime the start of the time range
     * @param endTime the end of the time range
     * @return count of authorizations with the specified status in the time range
     */
    @Query("SELECT COUNT(a) FROM Authorization a WHERE a.approvalStatus = :approvalStatus " +
           "AND a.requestTimestamp BETWEEN :startTime AND :endTime")
    long countByApprovalStatusAndTimeRange(
            @Param("approvalStatus") String approvalStatus,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Finds an authorization by ID with pessimistic write lock for concurrent processing.
     * This method supports real-time authorization updates where concurrent access
     * must be controlled to prevent race conditions during authorization processing.
     * 
     * Used for:
     * - Concurrent authorization processing
     * - Status updates requiring exclusive access
     * - Critical section protection during authorization flows
     * - Database consistency maintenance
     * 
     * Performance: Uses PESSIMISTIC_WRITE lock to ensure data consistency.
     * Warning: This method should be used sparingly due to locking overhead.
     * 
     * @param authorizationId the authorization ID to retrieve and lock
     * @return the authorization with exclusive lock, or empty if not found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Authorization a WHERE a.authorizationId = :authorizationId")
    Optional<Authorization> findByIdWithLock(@Param("authorizationId") Long authorizationId);

    /**
     * Retrieves authorizations with processing times exceeding the SLA threshold.
     * This method supports performance monitoring by identifying authorizations
     * that took longer than the 200ms SLA requirement to process.
     * 
     * Used for:
     * - SLA compliance monitoring
     * - Performance analysis and optimization
     * - System health monitoring
     * - Operational alerting and dashboards
     * 
     * @param slaThresholdMs the SLA threshold in milliseconds (typically 200)
     * @param startTime the start of the analysis time range
     * @param endTime the end of the analysis time range
     * @return list of authorizations exceeding the SLA threshold
     */
    @Query("SELECT a FROM Authorization a WHERE a.processingTime > :slaThresholdMs " +
           "AND a.requestTimestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY a.processingTime DESC")
    List<Authorization> findSLAViolationsByTimeRange(
            @Param("slaThresholdMs") Integer slaThresholdMs,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Retrieves distinct merchant IDs from authorizations within a time range.
     * This method supports merchant analysis and fraud detection by providing
     * a list of merchants that processed authorizations in a given time period.
     * 
     * Used for:
     * - Merchant activity analysis
     * - Fraud detection and investigation
     * - Business intelligence and reporting
     * - Merchant relationship management
     * 
     * @param startTime the start of the time range
     * @param endTime the end of the time range
     * @return list of distinct merchant IDs active in the time range
     */
    @Query("SELECT DISTINCT a.merchantId FROM Authorization a " +
           "WHERE a.requestTimestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY a.merchantId")
    List<String> findDistinctMerchantsByTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}