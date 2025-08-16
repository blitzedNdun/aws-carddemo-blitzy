package com.carddemo.repository;

import com.carddemo.entity.Archive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for Archive entity providing data access operations 
 * for archived records. Includes custom queries for retention policy queries, legal hold 
 * management, and efficient archival data retrieval.
 * 
 * This repository supports the archival policies documented in the database design section,
 * providing PostgreSQL-optimized queries for managing archived transaction data, customer data,
 * account data, and card data with appropriate retention periods and legal hold capabilities.
 * 
 * Key features:
 * - Retention policy management with configurable periods by data type
 * - Legal hold functionality for compliance requirements
 * - Storage location tracking for efficient retrieval
 * - PostgreSQL-specific optimizations for bulk operations
 * - Performance-optimized queries for large archive datasets
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024-01-01
 */
@Repository
public interface ArchiveRepository extends JpaRepository<Archive, Long> {

    /**
     * Save an archive record to the database.
     * Inherited from JpaRepository, provides ACID-compliant persistence.
     * 
     * @param archive the archive entity to save
     * @return the saved archive entity with generated ID
     */
    // Inherited: Archive save(Archive archive);

    /**
     * Find an archive record by its primary key ID.
     * Inherited from JpaRepository, uses PostgreSQL B-tree index for optimal performance.
     * 
     * @param id the primary key of the archive record
     * @return Optional containing the archive if found, empty otherwise
     */
    // Inherited: Optional<Archive> findById(Long id);

    /**
     * Retrieve all archive records from the database.
     * Inherited from JpaRepository, supports pagination for large datasets.
     * 
     * @return List of all archive records
     */
    // Inherited: List<Archive> findAll();

    /**
     * Find archive records by data type for category-specific retrieval.
     * Uses composite index on data_type for efficient filtering.
     * 
     * @param dataType the type of archived data (e.g., "TRANSACTION", "CUSTOMER", "ACCOUNT", "CARD")
     * @return List of archives matching the specified data type
     */
    @Query("SELECT a FROM Archive a WHERE a.dataType = :dataType ORDER BY a.archiveDate DESC")
    List<Archive> findByDataType(@Param("dataType") String dataType);

    /**
     * Find archive records eligible for purging based on retention date.
     * Supports automated retention policy enforcement as documented in archival policies.
     * Uses PostgreSQL date indexing for efficient range queries.
     * 
     * @param retentionDate the cutoff date for retention eligibility
     * @return List of archives that have exceeded their retention period
     */
    @Query("SELECT a FROM Archive a WHERE a.retentionDate < :retentionDate AND a.legalHold = false ORDER BY a.retentionDate ASC")
    List<Archive> findByRetentionDateBefore(@Param("retentionDate") LocalDate retentionDate);

    /**
     * Find all archive records currently under legal hold.
     * Supports compliance requirements for litigation and regulatory holds.
     * 
     * @return List of archives with active legal hold status
     */
    @Query("SELECT a FROM Archive a WHERE a.legalHold = true ORDER BY a.legalHoldDate DESC")
    List<Archive> findByLegalHoldTrue();

    /**
     * Find archive records created within a specific date range.
     * Optimized for reporting and audit trail requirements.
     * Uses PostgreSQL date range indexing for efficient queries.
     * 
     * @param startDate the beginning of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return List of archives created within the specified date range
     */
    @Query("SELECT a FROM Archive a WHERE a.archiveDate BETWEEN :startDate AND :endDate ORDER BY a.archiveDate DESC")
    List<Archive> findByArchiveDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Delete an archive record by its primary key ID.
     * Inherited from JpaRepository, supports cascade deletion if configured.
     * 
     * @param id the primary key of the archive record to delete
     */
    // Inherited: void deleteById(Long id);

    /**
     * Count the total number of archive records in the database.
     * Inherited from JpaRepository, uses PostgreSQL table statistics for performance.
     * 
     * @return the total count of archive records
     */
    // Inherited: long count();

    /**
     * Check if an archive record exists by its primary key ID.
     * Inherited from JpaRepository, uses existence check without loading full entity.
     * 
     * @param id the primary key to check for existence
     * @return true if archive exists, false otherwise
     */
    // Inherited: boolean existsById(Long id);

    // Additional custom query methods for enhanced archive management

    /**
     * Find archives by storage location for efficient retrieval operations.
     * Supports distributed storage scenarios and location-based access patterns.
     * 
     * @param storageLocation the storage location identifier
     * @return List of archives stored at the specified location
     */
    @Query("SELECT a FROM Archive a WHERE a.storageLocation = :storageLocation ORDER BY a.archiveDate DESC")
    List<Archive> findByStorageLocation(@Param("storageLocation") String storageLocation);

    /**
     * Find archives by data type and legal hold status for compliance reporting.
     * Combines data type filtering with legal hold status for audit purposes.
     * 
     * @param dataType the type of archived data
     * @param legalHold the legal hold status (true/false)
     * @return List of archives matching both criteria
     */
    @Query("SELECT a FROM Archive a WHERE a.dataType = :dataType AND a.legalHold = :legalHold ORDER BY a.archiveDate DESC")
    List<Archive> findByDataTypeAndLegalHold(@Param("dataType") String dataType, @Param("legalHold") boolean legalHold);

    /**
     * Count archives by data type for capacity planning and reporting.
     * Provides statistics for archive distribution across data categories.
     * 
     * @param dataType the type of archived data to count
     * @return the count of archives for the specified data type
     */
    @Query("SELECT COUNT(a) FROM Archive a WHERE a.dataType = :dataType")
    long countByDataType(@Param("dataType") String dataType);

    /**
     * Find archives scheduled for retention review within a date range.
     * Supports proactive retention management and compliance workflows.
     * 
     * @param startDate the beginning of the retention review period
     * @param endDate the end of the retention review period
     * @return List of archives requiring retention review
     */
    @Query("SELECT a FROM Archive a WHERE a.retentionDate BETWEEN :startDate AND :endDate ORDER BY a.retentionDate ASC")
    List<Archive> findByRetentionDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // PostgreSQL-specific native queries for bulk operations and performance optimization

    /**
     * Bulk update legal hold status for multiple archives.
     * Uses PostgreSQL native SQL for efficient bulk operations.
     * 
     * @param archiveIds the list of archive IDs to update
     * @param legalHold the new legal hold status
     * @param legalHoldDate the date legal hold was applied/removed
     * @return the number of records updated
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE archive SET legal_hold = :legalHold, legal_hold_date = :legalHoldDate, " +
                   "updated_timestamp = CURRENT_TIMESTAMP WHERE archive_id = ANY(:archiveIds)", nativeQuery = true)
    int updateLegalHoldStatus(@Param("archiveIds") Long[] archiveIds, 
                             @Param("legalHold") boolean legalHold, 
                             @Param("legalHoldDate") LocalDateTime legalHoldDate);

    /**
     * Bulk delete archives that have exceeded retention and are not under legal hold.
     * Uses PostgreSQL native SQL with optimal deletion strategy.
     * 
     * @param retentionDate the cutoff date for retention eligibility
     * @return the number of records deleted
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM archive WHERE retention_date < :retentionDate AND legal_hold = false", nativeQuery = true)
    int deleteExpiredArchives(@Param("retentionDate") LocalDate retentionDate);

    /**
     * Update storage location for archives in bulk migration scenarios.
     * Supports archive storage reorganization and migration operations.
     * 
     * @param oldLocation the current storage location
     * @param newLocation the new storage location
     * @return the number of records updated
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE archive SET storage_location = :newLocation, " +
                   "updated_timestamp = CURRENT_TIMESTAMP WHERE storage_location = :oldLocation", nativeQuery = true)
    int updateStorageLocation(@Param("oldLocation") String oldLocation, @Param("newLocation") String newLocation);

    /**
     * Get archive statistics by data type and date range for reporting.
     * Uses PostgreSQL aggregation functions for efficient statistical queries.
     * 
     * @param dataType the type of archived data
     * @param startDate the beginning of the analysis period
     * @param endDate the end of the analysis period
     * @return statistical summary including count, earliest and latest archive dates
     */
    @Query(value = "SELECT :dataType as data_type, COUNT(*) as archive_count, " +
                   "MIN(archive_date) as earliest_archive, MAX(archive_date) as latest_archive, " +
                   "COUNT(CASE WHEN legal_hold = true THEN 1 END) as legal_hold_count " +
                   "FROM archive WHERE data_type = :dataType AND archive_date BETWEEN :startDate AND :endDate", 
           nativeQuery = true)
    ArchiveStatistics getArchiveStatistics(@Param("dataType") String dataType, 
                                         @Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * Find archives requiring immediate attention for compliance.
     * Combines multiple criteria for compliance dashboard reporting.
     * 
     * @return List of archives needing compliance review
     */
    @Query("SELECT a FROM Archive a WHERE " +
           "(a.retentionDate < CURRENT_DATE AND a.legalHold = false) OR " +
           "(a.legalHold = true AND a.legalHoldDate < :reviewDate) " +
           "ORDER BY a.retentionDate ASC, a.legalHoldDate ASC")
    List<Archive> findArchivesRequiringComplianceReview(@Param("reviewDate") LocalDateTime reviewDate);

    /**
     * Get archive storage utilization summary by location.
     * Supports capacity planning and storage optimization decisions.
     * 
     * @return List of storage utilization statistics by location
     */
    @Query(value = "SELECT storage_location, COUNT(*) as archive_count, " +
                   "COUNT(DISTINCT data_type) as data_type_count, " +
                   "MIN(archive_date) as oldest_archive, MAX(archive_date) as newest_archive " +
                   "FROM archive GROUP BY storage_location ORDER BY archive_count DESC", 
           nativeQuery = true)
    List<StorageUtilization> getStorageUtilization();
}

/**
 * Projection interface for archive statistics reporting.
 * Supports efficient data transfer for statistical queries.
 */
interface ArchiveStatistics {
    String getDataType();
    Long getArchiveCount();
    LocalDateTime getEarliestArchive();
    LocalDateTime getLatestArchive();
    Long getLegalHoldCount();
}

/**
 * Projection interface for storage utilization reporting.
 * Supports capacity planning and storage optimization analysis.
 */
interface StorageUtilization {
    String getStorageLocation();
    Long getArchiveCount();
    Long getDataTypeCount();
    LocalDateTime getOldestArchive();
    LocalDateTime getNewestArchive();
}