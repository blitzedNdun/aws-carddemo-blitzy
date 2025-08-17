package com.carddemo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository interface for archived account data.
 * Provides data access operations for archived account records in the PostgreSQL archive schema.
 * Implements repository pattern with specialized query methods for archival date ranges,
 * retention policy queries, and retrieval operations for compliance and audit purposes.
 * 
 * This repository manages archived account data that has been moved from the primary
 * account_data table to the archive schema for long-term retention and compliance reporting.
 * 
 * Key Features:
 * - Archival date range queries for compliance reporting
 * - Retention policy management and expiration tracking
 * - Legal hold query support for litigation requirements
 * - Data type filtering for selective archival operations
 * - Performance-optimized queries for large archive datasets
 */
@Repository
public interface AccountArchiveRepository extends JpaRepository<Object, Long> {

    /**
     * Standard save operation for archiving account data.
     * Persists account records to the archive schema with archival metadata.
     * 
     * @param entity The account archive entity to save
     * @return The saved account archive entity
     */
    @Override
    <S> S save(S entity);

    /**
     * Retrieves all archived account records.
     * Uses pagination to handle large archive datasets efficiently.
     * 
     * @return List of all archived account records
     */
    @Override
    List<Object> findAll();

    /**
     * Standard delete operation for archive records.
     * Used for purging expired archive records per retention policies.
     * 
     * @param entity The account archive entity to delete
     */
    @Override
    void delete(Object entity);

    /**
     * Finds archived account records within the specified date range.
     * Essential for compliance reporting and audit trail generation.
     * Uses database indexes on archive_date for optimal performance.
     * 
     * @param startDate The beginning of the archive date range (inclusive)
     * @param endDate The end of the archive date range (inclusive)
     * @return List of archived accounts within the specified date range
     */
    @Query("SELECT a FROM AccountArchive a WHERE a.archiveDate BETWEEN :startDate AND :endDate ORDER BY a.archiveDate DESC")
    List<Object> findByArchiveDateBetween(@Param("startDate") LocalDate startDate, 
                                         @Param("endDate") LocalDate endDate);

    /**
     * Finds archived account records based on retention period criteria.
     * Supports retention policy enforcement and automated purging operations.
     * Calculates expiration dates based on archive date and retention rules.
     * 
     * @param retentionMonths The retention period in months
     * @return List of archived accounts eligible for retention processing
     */
    @Query("SELECT a FROM AccountArchive a WHERE FUNCTION('MONTHS_BETWEEN', CURRENT_DATE, a.archiveDate) >= :retentionMonths AND a.legalHold = false ORDER BY a.archiveDate ASC")
    List<Object> findByRetentionPeriod(@Param("retentionMonths") Integer retentionMonths);

    /**
     * Finds archived account records by data type and archive date criteria.
     * Supports selective retrieval for specific data classifications.
     * Enables efficient compliance reporting by data category.
     * 
     * @param dataType The classification of archived data (e.g., 'CUSTOMER_DATA', 'ACCOUNT_DATA')
     * @param archiveDate The minimum archive date for filtering
     * @return List of archived accounts matching data type and date criteria
     */
    @Query("SELECT a FROM AccountArchive a WHERE a.dataType = :dataType AND a.archiveDate >= :archiveDate ORDER BY a.archiveDate DESC")
    List<Object> findByDataTypeAndArchiveDateAfter(@Param("dataType") String dataType, 
                                                   @Param("archiveDate") LocalDate archiveDate);

    /**
     * Finds archived account records that are subject to legal hold.
     * Critical for litigation support and regulatory compliance.
     * These records cannot be purged regardless of retention policies.
     * 
     * @return List of archived accounts with active legal holds
     */
    @Query("SELECT a FROM AccountArchive a WHERE a.legalHold = true ORDER BY a.archiveDate ASC")
    List<Object> findByLegalHoldTrue();

    /**
     * Finds archived account records for a specific account ID.
     * Enables account-specific compliance reporting and audit trails.
     * 
     * @param accountId The account identifier to search for
     * @return List of archived records for the specified account
     */
    @Query("SELECT a FROM AccountArchive a WHERE a.accountId = :accountId ORDER BY a.archiveDate DESC")
    List<Object> findByAccountId(@Param("accountId") Long accountId);

    /**
     * Finds archived account records approaching retention expiration.
     * Supports automated notification and purging workflows.
     * 
     * @param warningDays Number of days before expiration to include
     * @return List of archived accounts approaching retention expiration
     */
    @Query("SELECT a FROM AccountArchive a WHERE a.legalHold = false AND " +
           "FUNCTION('DATEDIFF', a.expirationDate, CURRENT_DATE) <= :warningDays AND " +
           "a.expirationDate > CURRENT_DATE ORDER BY a.expirationDate ASC")
    List<Object> findByExpirationWarning(@Param("warningDays") Integer warningDays);

    /**
     * Counts archived account records by data type for reporting.
     * Provides metrics for archive storage management and compliance reporting.
     * 
     * @param dataType The data type to count
     * @return Number of archived records of the specified type
     */
    @Query("SELECT COUNT(a) FROM AccountArchive a WHERE a.dataType = :dataType")
    Long countByDataType(@Param("dataType") String dataType);

    /**
     * Finds archived account records with pagination support.
     * Optimized for large archive datasets with efficient page navigation.
     * 
     * @param pageable Pagination and sorting criteria
     * @return Page of archived account records
     */
    @Query("SELECT a FROM AccountArchive a ORDER BY a.archiveDate DESC")
    Page<Object> findAllWithPagination(Pageable pageable);

    /**
     * Finds archived account records by customer ID for customer-specific reporting.
     * Supports customer data subject requests and privacy compliance.
     * 
     * @param customerId The customer identifier to search for
     * @return List of archived records for the specified customer
     */
    @Query("SELECT a FROM AccountArchive a WHERE a.customerId = :customerId ORDER BY a.archiveDate DESC")
    List<Object> findByCustomerId(@Param("customerId") Long customerId);

    /**
     * Finds archived account records with expired retention periods.
     * Identifies records eligible for permanent deletion per retention policies.
     * Excludes records with active legal holds from deletion eligibility.
     * 
     * @return List of archived accounts eligible for permanent deletion
     */
    @Query("SELECT a FROM AccountArchive a WHERE a.expirationDate < CURRENT_DATE AND a.legalHold = false ORDER BY a.expirationDate ASC")
    List<Object> findExpiredRecords();

    /**
     * Updates legal hold status for archived account records.
     * Critical for litigation management and regulatory compliance.
     * 
     * @param accountId The account ID to update
     * @param legalHold The new legal hold status
     * @return Number of records updated
     */
    @Query("UPDATE AccountArchive a SET a.legalHold = :legalHold, a.lastModified = CURRENT_TIMESTAMP WHERE a.accountId = :accountId")
    Integer updateLegalHoldStatus(@Param("accountId") Long accountId, @Param("legalHold") Boolean legalHold);

    /**
     * Finds archived account records by archive location for storage management.
     * Supports distributed archive storage and data center management.
     * 
     * @param storageLocation The physical or logical storage location
     * @return List of archived records in the specified location
     */
    @Query("SELECT a FROM AccountArchive a WHERE a.storageLocation = :storageLocation ORDER BY a.archiveDate DESC")
    List<Object> findByStorageLocation(@Param("storageLocation") String storageLocation);

}