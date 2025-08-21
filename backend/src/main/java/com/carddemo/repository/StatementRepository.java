/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.repository;

import com.carddemo.entity.Statement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for Statement entity providing data access operations 
 * for statements table. Implements repository pattern to replace VSAM statement file access 
 * with modern JPA operations.
 * 
 * This repository replaces COBOL VSAM file operations from CBSTM03A/CBSTM03B programs:
 * - EXEC CICS READ DATASET(LIT-STMTFILENAME) → findById() method
 * - VSAM STARTBR/READNEXT operations → findAll(Pageable) for cursor-based pagination
 * - Sequential statement processing → findByAccountIdOrderByStatementDateDesc() method
 * 
 * Key VSAM-to-JPA mappings:
 * - VSAM KSDS primary key (STMT-ID) → JPA @Id Long statementId
 * - VSAM alternate index (account ID + date) → JPA custom query methods
 * - VSAM browse operations → JPA Pageable interface for pagination
 * - VSAM READ/WRITE/REWRITE/DELETE → JPA save(), delete(), findById() operations
 * 
 * Statement processing boundaries are managed through Spring's @Transactional support,
 * replicating CICS SYNCPOINT behavior for data consistency during batch statement generation.
 * 
 * Provides CRUD operations, custom query methods for statement retrieval by account ID and date,
 * and cursor-based pagination support replicating VSAM browse operations for statement history.
 * Extends JpaRepository with Statement entity type and Long primary key type.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Repository
public interface StatementRepository extends JpaRepository<Statement, Long> {

    /**
     * Find all statements for a specific account ordered by statement date descending
     * Replaces COBOL statement history retrieval logic
     * 
     * @param accountId the account identifier
     * @return list of statements for the account
     */
    List<Statement> findByAccountId(Long accountId);

    /**
     * Find statements for a specific account ordered by statement date descending
     * with pagination support for large statement histories
     * 
     * @param accountId the account identifier
     * @param pageable pagination parameters
     * @return page of statements for the account
     */
    Page<Statement> findByAccountIdOrderByStatementDateDesc(Long accountId, Pageable pageable);

    /**
     * Find the most recent statement for a specific account
     * Replaces COBOL logic to get current statement information
     * 
     * @param accountId the account identifier
     * @return optional containing the latest statement if found
     */
    @Query("SELECT s FROM Statement s WHERE s.accountId = :accountId " +
           "ORDER BY s.statementDate DESC LIMIT 1")
    Optional<Statement> findLatestStatementByAccountId(@Param("accountId") Long accountId);

    /**
     * Find statements by account ID and statement date range
     * Used for statement history queries and date-based filtering
     * 
     * @param accountId the account identifier
     * @param startDate start of date range (inclusive)
     * @param endDate end of date range (inclusive)
     * @return list of statements within the date range
     */
    List<Statement> findByAccountIdAndStatementDateBetween(
        Long accountId, LocalDate startDate, LocalDate endDate);

    /**
     * Find statements by statement date range across all accounts
     * Used for batch processing and statement generation reporting
     * 
     * @param startDate start of date range (inclusive)
     * @param endDate end of date range (inclusive)
     * @return list of statements within the date range
     */
    List<Statement> findByStatementDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find statements by status for batch processing operations
     * Used to identify statements needing archival or further processing
     * 
     * @param status statement status code (G=Generated, A=Archived, P=Paid)
     * @return list of statements with matching status
     */
    List<Statement> findByStatementStatus(String status);

    /**
     * Find statements requiring minimum payment
     * Replicates COBOL logic for identifying accounts with outstanding balances
     * 
     * @return list of statements with minimum payment due
     */
    @Query("SELECT s FROM Statement s WHERE s.currentBalance > 0 " +
           "AND s.minimumPaymentAmount > 0 AND s.statementStatus = 'G'")
    List<Statement> findStatementsWithMinimumPaymentDue();

    /**
     * Check if statement already exists for account and date
     * Prevents duplicate statement generation in batch processing
     * 
     * @param accountId the account identifier
     * @param statementDate the statement date
     * @return true if statement exists, false otherwise
     */
    boolean existsByAccountIdAndStatementDate(Long accountId, LocalDate statementDate);

    /**
     * Find statements by account ID and payment due date for payment processing
     * Used in payment batch processing to identify overdue accounts
     * 
     * @param accountId the account identifier
     * @param dueDate payment due date
     * @return list of statements with matching due date
     */
    List<Statement> findByAccountIdAndPaymentDueDate(Long accountId, LocalDate dueDate);

    /**
     * Get count of statements for an account within a date range
     * Used for validation and reporting purposes
     * 
     * @param accountId the account identifier
     * @param startDate start of date range
     * @param endDate end of date range
     * @return count of statements in date range
     */
    @Query("SELECT COUNT(s) FROM Statement s WHERE s.accountId = :accountId " +
           "AND s.statementDate BETWEEN :startDate AND :endDate")
    long countByAccountIdAndStatementDateBetween(
        @Param("accountId") Long accountId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    /**
     * Find statements with outstanding balances for aged receivables reporting
     * Replicates COBOL aging logic for account management
     * 
     * @param asOfDate date to calculate aging
     * @return list of statements with outstanding balances
     */
    @Query("SELECT s FROM Statement s WHERE s.currentBalance > 0 " +
           "AND s.statementDate <= :asOfDate ORDER BY s.accountId, s.statementDate")
    List<Statement> findStatementsWithOutstandingBalances(@Param("asOfDate") LocalDate asOfDate);

    /**
     * Update statement status for batch operations
     * Used during statement archival and processing workflows
     * 
     * @param statementId the statement identifier
     * @param newStatus new status code
     * @param updatedBy user updating the status
     * @return number of records updated
     */
    @Query("UPDATE Statement s SET s.statementStatus = :newStatus, " +
           "s.lastUpdatedDate = CURRENT_TIMESTAMP WHERE s.statementId = :statementId")
    int updateStatementStatus(
        @Param("statementId") Long statementId,
        @Param("newStatus") String newStatus);

    /**
     * Find statements requiring archival based on retention policy
     * Used in automated archival batch processing
     * 
     * @param cutoffDate statements older than this date are candidates for archival
     * @return list of statements ready for archival
     */
    @Query("SELECT s FROM Statement s WHERE s.statementDate < :cutoffDate " +
           "AND s.statementStatus = 'G' ORDER BY s.statementDate")
    List<Statement> findStatementsForArchival(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Obtain pessimistic lock on statement record for concurrent processing
     * Replicates CICS exclusive record locking during statement updates
     * 
     * @param statementId the statement identifier
     * @return optional containing locked statement if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Statement s WHERE s.statementId = :statementId")
    Optional<Statement> findByIdWithLock(@Param("statementId") Long statementId);
}