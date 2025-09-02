package com.carddemo.repository;

import com.carddemo.entity.Dispute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for Dispute entity providing CRUD operations 
 * and custom query methods for dispute management functionality.
 * 
 * This repository replaces VSAM data access patterns with PostgreSQL-based operations,
 * supporting the credit card dispute processing system as part of the COBOL-to-Java migration.
 * 
 * Key Features:
 * - Standard CRUD operations through JpaRepository inheritance
 * - Custom query methods for dispute filtering and search
 * - Pagination support for large result sets
 * - Complex query operations for dispute analytics
 * - Integration with Spring Security for audit tracking
 * 
 * Performance Considerations:
 * - Utilizes PostgreSQL B-tree indexes for optimal query performance
 * - Supports cursor-based pagination for VSAM STARTBR/READNEXT pattern replication
 * - Designed for sub-200ms response time requirements
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024
 */
@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    /**
     * Standard JPA save operation for creating or updating dispute records.
     * Inherits from JpaRepository to provide ACID-compliant persistence operations.
     * 
     * @param dispute The dispute entity to save
     * @return The saved dispute entity with generated ID
     */
    // save() method inherited from JpaRepository<Dispute, Long>

    /**
     * Standard JPA findById operation for retrieving dispute by primary key.
     * Utilizes PostgreSQL B-tree index for optimal performance.
     * 
     * @param id The dispute ID (primary key)
     * @return Optional containing the dispute if found
     */
    // findById() method inherited from JpaRepository<Dispute, Long>

    /**
     * Standard JPA findAll operation for retrieving all dispute records.
     * Supports pagination through Pageable parameter when needed.
     * 
     * @return List of all dispute entities
     */
    // findAll() method inherited from JpaRepository<Dispute, Long>

    /**
     * Standard JPA delete operation for removing dispute records.
     * Maintains referential integrity through PostgreSQL constraints.
     * 
     * @param dispute The dispute entity to delete
     */
    // delete() method inherited from JpaRepository<Dispute, Long>

    /**
     * Finds all disputes associated with a specific transaction ID.
     * Supports credit card dispute lookup by transaction reference.
     * 
     * @param transactionId The transaction ID to search for
     * @return List of disputes linked to the transaction
     */
    @Query("SELECT d FROM Dispute d WHERE d.transactionId = :transactionId ORDER BY d.createdDate DESC")
    List<Dispute> findByTransactionId(@Param("transactionId") Long transactionId);

    /**
     * Finds all disputes for a specific account ID with pagination support.
     * Enables account-level dispute management and history tracking.
     * 
     * @param accountId The account ID to search for
     * @param pageable Pagination parameters for large result sets
     * @return Page of disputes for the account
     */
    @Query("SELECT d FROM Dispute d WHERE d.accountId = :accountId ORDER BY d.createdDate DESC")
    Page<Dispute> findByAccountId(@Param("accountId") Long accountId, Pageable pageable);

    /**
     * Overloaded method for finding disputes by account ID without pagination.
     * 
     * @param accountId The account ID to search for
     * @return List of disputes for the account
     */
    @Query("SELECT d FROM Dispute d WHERE d.accountId = :accountId ORDER BY d.createdDate DESC")
    List<Dispute> findByAccountId(@Param("accountId") Long accountId);

    /**
     * Finds disputes by status for workflow management and reporting.
     * Supports dispute processing workflow automation.
     * 
     * @param status The dispute status to filter by
     * @return List of disputes with matching status
     */
    @Query("SELECT d FROM Dispute d WHERE d.status = :status ORDER BY d.createdDate ASC")
    List<Dispute> findByStatus(@Param("status") String status);

    /**
     * Finds disputes by dispute type for categorization and analytics.
     * Enables dispute type-specific processing and reporting.
     * 
     * @param disputeType The type of dispute to search for
     * @return List of disputes matching the type
     */
    @Query("SELECT d FROM Dispute d WHERE d.disputeType = :disputeType ORDER BY d.createdDate DESC")
    List<Dispute> findByDisputeType(@Param("disputeType") String disputeType);

    /**
     * Finds disputes within a specific date range for time-based analysis.
     * Supports regulatory reporting and dispute aging analysis.
     * 
     * @param startDate The start date of the range (inclusive)
     * @param endDate The end date of the range (inclusive)
     * @return List of disputes within the date range
     */
    @Query("SELECT d FROM Dispute d WHERE d.createdDate BETWEEN :startDate AND :endDate ORDER BY d.createdDate DESC")
    List<Dispute> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Finds disputes by status and account ID for targeted account management.
     * Combines status filtering with account-specific queries for efficient processing.
     * 
     * @param status The dispute status to filter by
     * @param accountId The account ID to filter by
     * @return List of disputes matching both criteria
     */
    @Query("SELECT d FROM Dispute d WHERE d.status = :status AND d.accountId = :accountId ORDER BY d.createdDate DESC")
    List<Dispute> findByStatusAndAccountId(@Param("status") String status, @Param("accountId") Long accountId);

    /**
     * Counts disputes by status for dashboard metrics and reporting.
     * Provides aggregate data for dispute management analytics.
     * 
     * @param status The dispute status to count
     * @return Count of disputes with the specified status
     */
    @Query("SELECT COUNT(d) FROM Dispute d WHERE d.status = :status")
    Long countByStatus(@Param("status") String status);

    /**
     * Checks if any disputes exist for a specific transaction ID.
     * Supports duplicate dispute prevention and transaction dispute validation.
     * 
     * @param transactionId The transaction ID to check
     * @return True if disputes exist for the transaction, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN TRUE ELSE FALSE END FROM Dispute d WHERE d.transactionId = :transactionId")
    boolean existsByTransactionId(@Param("transactionId") Long transactionId);

    /**
     * Finds disputes by status with pagination support for large datasets.
     * Enables efficient processing of dispute queues and status-based workflows.
     * 
     * @param status The dispute status to filter by
     * @param pageable Pagination parameters
     * @return Page of disputes with matching status
     */
    @Query("SELECT d FROM Dispute d WHERE d.status = :status ORDER BY d.createdDate ASC")
    Page<Dispute> findByStatus(@Param("status") String status, Pageable pageable);

    /**
     * Finds active disputes (non-closed status) for operational management.
     * Supports active dispute queue management and processing prioritization.
     * 
     * @return List of active disputes ordered by creation date
     */
    @Query("SELECT d FROM Dispute d WHERE d.status NOT IN ('CLOSED', 'RESOLVED', 'REJECTED', 'RESOLVED_CUSTOMER', 'RESOLVED_MERCHANT') ORDER BY d.createdDate ASC")
    List<Dispute> findActiveDisputes();

    /**
     * Finds disputes by account ID and status with date range filtering.
     * Comprehensive query for detailed dispute analysis and reporting.
     * 
     * @param accountId The account ID to filter by
     * @param status The dispute status to filter by
     * @param startDate The start date of the range (inclusive)
     * @param endDate The end date of the range (inclusive)
     * @return List of disputes matching all criteria
     */
    @Query("SELECT d FROM Dispute d WHERE d.accountId = :accountId AND d.status = :status AND d.createdDate BETWEEN :startDate AND :endDate ORDER BY d.createdDate DESC")
    List<Dispute> findByAccountIdAndStatusAndDateRange(
        @Param("accountId") Long accountId,
        @Param("status") String status,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );



    /**
     * Finds disputes by dispute type and status for workflow automation.
     * Enables type-specific and status-specific dispute processing.
     * 
     * @param disputeType The type of dispute
     * @param status The dispute status
     * @return List of disputes matching both type and status
     */
    @Query("SELECT d FROM Dispute d WHERE d.disputeType = :disputeType AND d.status = :status ORDER BY d.createdDate DESC")
    List<Dispute> findByDisputeTypeAndStatus(@Param("disputeType") String disputeType, @Param("status") String status);

    /**
     * Counts total disputes for an account within a date range.
     * Supports account-level dispute metrics and trend analysis.
     * 
     * @param accountId The account ID
     * @param startDate The start date of the range (inclusive)
     * @param endDate The end date of the range (inclusive)
     * @return Count of disputes for the account in the date range
     */
    @Query("SELECT COUNT(d) FROM Dispute d WHERE d.accountId = :accountId AND d.createdDate BETWEEN :startDate AND :endDate")
    Long countByAccountIdAndDateRange(
        @Param("accountId") Long accountId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}