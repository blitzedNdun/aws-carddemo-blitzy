/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.repository;

import com.carddemo.entity.AccountClosure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository interface for AccountClosure entity providing comprehensive data access operations 
 * for account closure requests and workflow management. This repository implements the repository pattern to 
 * manage the complete closure request lifecycle including finding pending closure requests, updating closure 
 * status, and tracking closure workflow through various query methods.
 * 
 * Extends JpaRepository to provide standard CRUD operations (findById, findAll, save, delete) along with 
 * custom query methods specifically designed for account closure batch processing and closure workflow 
 * management. Integrates seamlessly with Spring transaction management through @Repository annotation 
 * for component scanning and exception translation.
 * 
 * The repository supports account closure operations by providing:
 * - Account-specific closure request retrieval
 * - Status-based closure request filtering  
 * - Pending closure request identification for batch processing
 * - Date range queries for closure request reporting and analytics
 * - Full CRUD operations for closure request lifecycle management
 * 
 * Custom query methods leverage Spring Data JPA query derivation and @Query annotations to provide
 * efficient data access patterns that support both real-time closure request processing and batch
 * closure operations within the 4-hour processing window requirement.
 * 
 * Repository Pattern Benefits:
 * - Clean separation between business logic and data access
 * - Consistent data access interface for closure operations
 * - Spring Data JPA automatic query implementation
 * - Integration with Spring transaction boundaries
 * - Exception translation for database-specific errors
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Repository
public interface ClosureRequestRepository extends JpaRepository<AccountClosure, Long> {

    /**
     * Finds all closure requests associated with a specific account ID.
     * This method supports account closure operations by retrieving all closure requests
     * for a given account, enabling account closure history tracking and status verification.
     * 
     * Uses Spring Data JPA query derivation to automatically generate the appropriate
     * SQL query based on the method name pattern 'findBy{PropertyName}'.
     * 
     * @param accountId the account ID to search for closure requests
     * @return List of AccountClosure entities associated with the specified account ID.
     *         Returns empty list if no closure requests exist for the account.
     * 
     * Example usage in service layer:
     * List&lt;AccountClosure&gt; accountClosures = repository.findByAccountId(123456L);
     * 
     * Generated SQL equivalent:
     * SELECT * FROM account_closure WHERE account_id = ?
     */
    List<AccountClosure> findByAccountId(Long accountId);

    /**
     * Finds all closure requests with a specific closure status.
     * This method enables closure workflow management by filtering requests based on their
     * current processing status ('P' for Pending, 'C' for Complete, 'R' for Rejected).
     * 
     * Critical for batch processing operations that need to identify closure requests
     * in specific workflow states for processing, reporting, or audit purposes.
     * 
     * @param closureStatus the closure status to filter by:
     *                     'P' = Pending closure requests
     *                     'C' = Completed closure requests  
     *                     'R' = Rejected closure requests
     * @return List of AccountClosure entities with the specified closure status.
     *         Returns empty list if no requests exist with the given status.
     * 
     * Example usage for finding completed closures:
     * List&lt;AccountClosure&gt; completedClosures = repository.findByClosureStatus("C");
     * 
     * Generated SQL equivalent:
     * SELECT * FROM account_closure WHERE closure_status = ?
     */
    List<AccountClosure> findByClosureStatus(String closureStatus);

    /**
     * Finds all pending closure requests requiring processing.
     * This specialized method identifies closure requests in 'Pending' status for batch processing
     * operations. Essential for account closure batch jobs that need to process all outstanding
     * closure requests within the 4-hour processing window.
     * 
     * Uses @Query annotation with JPQL to provide explicit query logic for pending status filtering.
     * The query specifically targets closure_status = 'P' to identify all requests awaiting processing.
     * 
     * This method is critical for:
     * - Batch closure processing operations
     * - Closure request queue management
     * - Workflow status monitoring and reporting
     * - Ensuring all pending requests are processed timely
     * 
     * @return List of AccountClosure entities with closure status 'P' (Pending).
     *         Returns empty list if no pending closure requests exist.
     *         Results are ordered by requested_date to process oldest requests first.
     * 
     * Example usage in batch processing:
     * List&lt;AccountClosure&gt; pendingClosures = repository.findPendingClosureRequests();
     * 
     * Generated SQL equivalent:
     * SELECT * FROM account_closure WHERE closure_status = 'P' ORDER BY requested_date ASC
     */
    @Query("SELECT ac FROM AccountClosure ac WHERE ac.closureStatus = 'P' ORDER BY ac.requestedDate ASC")
    List<AccountClosure> findPendingClosureRequests();

    /**
     * Finds closure requests within a specific date range based on requested date.
     * This method supports closure request reporting, analytics, and audit operations by
     * enabling date-range queries for closure request analysis and historical reporting.
     * 
     * Particularly useful for:
     * - Monthly closure request reporting
     * - Closure request trend analysis
     * - Regulatory reporting requirements
     * - Audit trail generation for specific time periods
     * - Performance monitoring of closure processing times
     * 
     * Uses Spring Data JPA Between query derivation to generate appropriate SQL with
     * date range filtering on the requested_date field.
     * 
     * @param startDate the beginning date of the range (inclusive)
     * @param endDate the ending date of the range (inclusive)
     * @return List of AccountClosure entities with requested dates between startDate and endDate (inclusive).
     *         Returns empty list if no closure requests exist within the specified date range.
     *         Results are ordered by requested_date for chronological analysis.
     * 
     * Example usage for monthly reporting:
     * LocalDate monthStart = LocalDate.of(2024, 1, 1);
     * LocalDate monthEnd = LocalDate.of(2024, 1, 31);
     * List&lt;AccountClosure&gt; monthlyClosures = repository.findByRequestedDateBetween(monthStart, monthEnd);
     * 
     * Generated SQL equivalent:
     * SELECT * FROM account_closure WHERE requested_date BETWEEN ? AND ? ORDER BY requested_date ASC
     */
    @Query("SELECT ac FROM AccountClosure ac WHERE ac.requestedDate BETWEEN :startDate AND :endDate ORDER BY ac.requestedDate ASC")
    List<AccountClosure> findByRequestedDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

}