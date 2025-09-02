package com.carddemo.repository;

import com.carddemo.entity.Fee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for Fee entity providing CRUD operations and custom queries 
 * for fee management, assessment, and retrieval operations.
 * 
 * This repository follows the Spring Data JPA repository pattern used throughout the CardDemo application
 * and provides comprehensive fee management capabilities including:
 * - Standard CRUD operations for Fee entities
 * - Custom query methods for finding fees by account ID, type, and status
 * - Date range queries for fee assessment periods
 * - Aggregate queries for fee totals and summaries
 * - Pagination support for large fee datasets
 * 
 * The repository integrates seamlessly with the Spring Boot transaction management and
 * PostgreSQL database architecture, maintaining ACID compliance for all fee operations.
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @see Fee
 * @see JpaRepository
 */
@Repository
public interface FeeRepository extends JpaRepository<Fee, Long> {

    /**
     * Finds all fees associated with a specific account ID.
     * This method supports fee history and audit queries by retrieving all fees 
     * ever assessed against a particular account.
     * 
     * @param accountId the account ID to search for fees
     * @return List of Fee entities associated with the account
     */
    List<Fee> findByAccountId(Long accountId);

    /**
     * Finds fees by fee type with pagination support.
     * This method enables filtering fees by specific types (e.g., late fees, overlimit fees, etc.)
     * and supports pagination for large datasets.
     * 
     * @param feeType the type of fee to search for
     * @param pageable pagination information
     * @return Page of Fee entities matching the fee type
     */
    Page<Fee> findByFeeType(String feeType, Pageable pageable);

    /**
     * Finds fees by fee status with pagination support.
     * This method supports fee management operations by filtering fees based on their
     * current status (e.g., pending, assessed, waived, etc.).
     * 
     * @param feeStatus the status of fees to search for
     * @param pageable pagination information
     * @return Page of Fee entities matching the fee status
     */
    Page<Fee> findByFeeStatus(String feeStatus, Pageable pageable);

    /**
     * Finds fees assessed within a specific date range.
     * This method supports fee history queries and regulatory reporting by enabling
     * date-range based fee retrieval for specific assessment periods.
     * 
     * @param startDate the start date of the assessment period (inclusive)
     * @param endDate the end date of the assessment period (inclusive)
     * @return List of Fee entities assessed within the date range
     */
    List<Fee> findByAssessmentDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Finds fees for a specific account and fee type combination.
     * This method supports targeted fee queries for specific account and fee type
     * combinations, useful for fee assessment validation and duplicate checking.
     * 
     * @param accountId the account ID to search for
     * @param feeType the fee type to filter by
     * @return List of Fee entities matching both account and fee type criteria
     */
    List<Fee> findByAccountIdAndFeeType(Long accountId, String feeType);

    /**
     * Calculates the total fee amount for a specific account.
     * This method provides aggregate fee calculation functionality using a custom JPQL query
     * to sum all fee amounts for a given account, supporting account balance calculations
     * and fee summary operations.
     * 
     * @param accountId the account ID to calculate total fees for
     * @return Optional containing the total fee amount, or empty if no fees exist
     */
    @Query("SELECT SUM(f.feeAmount) FROM Fee f WHERE f.accountId = :accountId AND f.feeStatus = 'ASSESSED'")
    Optional<BigDecimal> getTotalFeesByAccount(@Param("accountId") Long accountId);

    /**
     * Finds all pending fees for a specific account.
     * This method supports fee assessment and posting operations by retrieving
     * all fees in pending status for a given account.
     * 
     * @param accountId the account ID to search for pending fees
     * @return List of Fee entities in pending status for the account
     */
    @Query("SELECT f FROM Fee f WHERE f.accountId = :accountId AND f.feeStatus = 'PENDING' ORDER BY f.assessmentDate ASC")
    List<Fee> findPendingFeesByAccount(@Param("accountId") Long accountId);

    /**
     * Finds fees by account ID and assessment date range with pagination.
     * This method combines account filtering with date range queries and pagination support,
     * enabling efficient retrieval of account-specific fee history for large datasets.
     * 
     * @param accountId the account ID to search for
     * @param startDate the start date of the assessment period (inclusive)
     * @param endDate the end date of the assessment period (inclusive)
     * @param pageable pagination information
     * @return Page of Fee entities matching the criteria
     */
    @Query("SELECT f FROM Fee f WHERE f.accountId = :accountId AND f.assessmentDate BETWEEN :startDate AND :endDate ORDER BY f.assessmentDate DESC")
    Page<Fee> findByAccountIdAndAssessmentDateBetween(
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    /**
     * Calculates total fees by fee type for reporting purposes.
     * This method provides aggregate reporting functionality by calculating
     * total fee amounts grouped by fee type, supporting management reporting
     * and fee analysis operations.
     * 
     * @param feeType the fee type to calculate totals for
     * @return Optional containing the total amount for the fee type
     */
    @Query("SELECT SUM(f.feeAmount) FROM Fee f WHERE f.feeType = :feeType AND f.feeStatus = 'ASSESSED'")
    Optional<BigDecimal> getTotalFeesByType(@Param("feeType") String feeType);

    /**
     * Finds the most recent fee assessment for a specific account and fee type.
     * This method supports fee assessment logic by retrieving the latest fee
     * of a specific type for an account, useful for preventing duplicate assessments
     * and fee policy enforcement.
     * 
     * @param accountId the account ID to search for
     * @param feeType the fee type to filter by
     * @return Optional containing the most recent fee, or empty if none exists
     */
    @Query("SELECT f FROM Fee f WHERE f.accountId = :accountId AND f.feeType = :feeType ORDER BY f.assessmentDate DESC LIMIT 1")
    Optional<Fee> findMostRecentFeeByAccountAndType(@Param("accountId") Long accountId, @Param("feeType") String feeType);

    /**
     * Counts the number of fees for a specific account and status.
     * This method supports fee management operations by providing count-based
     * queries for specific account and status combinations, useful for
     * dashboard displays and fee summary statistics.
     * 
     * @param accountId the account ID to count fees for
     * @param feeStatus the fee status to filter by
     * @return the count of fees matching the criteria
     */
    long countByAccountIdAndFeeStatus(Long accountId, String feeStatus);

    /**
     * Finds all fees that are due for assessment based on a cutoff date.
     * This method supports automated fee assessment processes by identifying
     * fees that are scheduled for assessment before or on a specified date.
     * 
     * @param cutoffDate the cutoff date for fee assessment
     * @return List of Fee entities due for assessment
     */
    @Query("SELECT f FROM Fee f WHERE f.feeStatus = 'SCHEDULED' AND f.assessmentDate <= :cutoffDate ORDER BY f.assessmentDate ASC")
    List<Fee> findFeesForAssessment(@Param("cutoffDate") LocalDate cutoffDate);

}