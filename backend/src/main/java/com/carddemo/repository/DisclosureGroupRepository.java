/*
 * DisclosureGroupRepository.java
 * 
 * Spring Data JPA repository interface for DisclosureGroup entity providing
 * data access for disclosure_groups table. Manages interest rate configurations
 * and disclosure group settings replacing VSAM reference file patterns.
 * 
 * Provides lookup operations for interest calculation and disclosure assignment.
 * Extends JpaRepository with DisclosureGroup entity type and Long primary key type.
 * 
 * This repository replaces VSAM disclosure group reference file I/O operations
 * (READ, WRITE, REWRITE, DELETE) with modern JPA method implementations for
 * disclosure_groups table access, supporting interest rate retrieval for
 * calculation processes and disclosure group assignment queries.
 * 
 * Based on COBOL copybook: app/cpy/CVTRA02Y.cpy
 * - DIS-GROUP-RECORD structure with composite key access patterns
 * - DIS-ACCT-GROUP-ID (PIC X(10)) for account group identification
 * - DIS-TRAN-TYPE-CD (PIC X(02)) for transaction type classification
 * - DIS-TRAN-CAT-CD (PIC 9(04)) for transaction category codes
 * - DIS-INT-RATE (PIC S9(04)V99) for interest rate calculations
 */

package com.carddemo.repository;

import com.carddemo.entity.DisclosureGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for DisclosureGroup entity.
 * 
 * Provides comprehensive data access operations for disclosure_groups table,
 * supporting interest rate configuration management and disclosure group
 * assignment operations. Replaces VSAM reference file patterns with modern
 * JPA repository methods that maintain identical functionality while leveraging
 * PostgreSQL relational database capabilities.
 * 
 * Key functionality:
 * - Account group ID-based lookups for disclosure assignment
 * - Transaction type and category code filtering for interest calculation
 * - Interest rate range queries for configuration management
 * - Group name-based searches for administrative operations
 * - Active rate retrieval for real-time calculation processes
 * 
 * All methods support the sub-200ms REST API response time requirement
 * through optimized PostgreSQL B-tree index utilization and query planning.
 */
@Repository
public interface DisclosureGroupRepository extends JpaRepository<DisclosureGroup, Long> {

    /**
     * Finds disclosure groups by account group ID.
     * 
     * Supports account-based disclosure group lookup operations equivalent to
     * VSAM READ by account group ID key. Essential for determining applicable
     * disclosure group assignments during account processing and interest
     * calculation workflows.
     * 
     * @param accountGroupId the account group identifier (10 character string)
     * @return list of disclosure groups for the specified account group
     */
    List<DisclosureGroup> findByAccountGroupId(String accountGroupId);

    /**
     * Finds disclosure groups by transaction type code and transaction category code.
     * 
     * Supports composite key lookup operations equivalent to VSAM READ by
     * transaction type and category combination. Critical for interest rate
     * determination during transaction processing and disclosure assignment
     * based on transaction characteristics.
     * 
     * @param transactionTypeCode the transaction type code (2 character string)
     * @param transactionCategoryCode the transaction category code (4 digit numeric)
     * @return list of disclosure groups matching the transaction criteria
     */
    List<DisclosureGroup> findByTransactionTypeCodeAndTransactionCategoryCode(
            String transactionTypeCode, 
            String transactionCategoryCode);

    /**
     * Finds disclosure groups by group name.
     * 
     * Supports name-based lookup operations for administrative and configuration
     * management functions. Enables group management through natural key searches
     * rather than primary key access, supporting user-friendly administrative
     * interfaces and configuration validation processes.
     * 
     * @param groupName the disclosure group name for lookup
     * @return list of disclosure groups with matching group names
     */
    List<DisclosureGroup> findByGroupName(String groupName);

    /**
     * Finds disclosure groups by exact interest rate.
     * 
     * Supports interest rate-based lookup operations for configuration validation
     * and rate management functions. Enables identification of disclosure groups
     * with specific interest rate values for administrative reporting and
     * rate structure analysis.
     * 
     * Uses BigDecimal for precise financial calculations maintaining COBOL
     * COMP-3 packed decimal precision from DIS-INT-RATE field (S9(04)V99).
     * 
     * @param interestRate the exact interest rate for matching (BigDecimal precision)
     * @return list of disclosure groups with the specified interest rate
     */
    List<DisclosureGroup> findByInterestRate(BigDecimal interestRate);

    /**
     * Finds disclosure groups with interest rates greater than specified value.
     * 
     * Supports range-based interest rate queries for configuration analysis
     * and rate structure reporting. Enables identification of disclosure groups
     * above specified rate thresholds for administrative review and rate
     * management operations.
     * 
     * @param interestRate the minimum interest rate threshold (exclusive)
     * @return list of disclosure groups with interest rates above the threshold
     */
    List<DisclosureGroup> findByInterestRateGreaterThan(BigDecimal interestRate);

    /**
     * Finds disclosure groups with interest rates less than specified value.
     * 
     * Supports range-based interest rate queries for configuration analysis
     * and promotional rate identification. Enables identification of disclosure
     * groups below specified rate thresholds for customer benefit analysis
     * and marketing campaign support.
     * 
     * @param interestRate the maximum interest rate threshold (exclusive)
     * @return list of disclosure groups with interest rates below the threshold
     */
    List<DisclosureGroup> findByInterestRateLessThan(BigDecimal interestRate);

    /**
     * Finds active interest rates by group ID using custom query.
     * 
     * Retrieves currently active interest rate configurations for specified
     * disclosure group ID. Supports real-time interest calculation processes
     * by filtering for active rate entries and providing optimized access
     * to current rate information.
     * 
     * Custom JPQL query ensures optimal performance through index utilization
     * and supports complex filtering criteria for active rate determination.
     * 
     * @param groupId the disclosure group ID for active rate lookup
     * @return list of disclosure groups with active interest rates
     */
    @Query("SELECT dg FROM DisclosureGroup dg WHERE dg.disclosureGroupId = :groupId AND dg.active = true")
    List<DisclosureGroup> findActiveRatesByGroupId(@Param("groupId") Long groupId);

    /**
     * Finds disclosure groups by account group ID and transaction type code.
     * 
     * Supports composite lookup operations combining account group and transaction
     * type criteria. Essential for precise disclosure group determination during
     * transaction processing workflows where both account characteristics and
     * transaction type influence applicable disclosure requirements.
     * 
     * @param accountGroupId the account group identifier
     * @param transactionTypeCode the transaction type code
     * @return list of disclosure groups matching both criteria
     */
    List<DisclosureGroup> findByAccountGroupIdAndTransactionTypeCode(
            String accountGroupId, 
            String transactionTypeCode);

    /**
     * Finds disclosure groups by transaction type code.
     * 
     * Supports transaction type-based lookup operations for disclosure group
     * determination. Enables identification of all disclosure groups applicable
     * to specific transaction types, supporting transaction processing workflows
     * and regulatory compliance reporting.
     * 
     * @param transactionTypeCode the transaction type code (2 character string)
     * @return list of disclosure groups for the specified transaction type
     */
    List<DisclosureGroup> findByTransactionTypeCode(String transactionTypeCode);

    /**
     * Finds disclosure groups by transaction category code.
     * 
     * Supports transaction category-based lookup operations for disclosure group
     * determination. Enables identification of all disclosure groups applicable
     * to specific transaction categories, supporting categorized interest
     * calculation and disclosure assignment processes.
     * 
     * @param transactionCategoryCode the transaction category code (4 digit numeric)
     * @return list of disclosure groups for the specified transaction category
     */
    List<DisclosureGroup> findByTransactionCategoryCode(String transactionCategoryCode);

    /**
     * Finds all active disclosure groups using custom query.
     * 
     * Retrieves all currently active disclosure group configurations for
     * system-wide operations and administrative reporting. Supports bulk
     * operations and configuration validation processes by providing access
     * to all active disclosure group entries.
     * 
     * Custom JPQL query with ordering ensures consistent result ordering
     * and optimal query performance through index utilization.
     * 
     * @return list of all active disclosure groups ordered by group name
     */
    @Query("SELECT dg FROM DisclosureGroup dg WHERE dg.active = true ORDER BY dg.groupName")
    List<DisclosureGroup> findActiveDisclosureGroups();

    /**
     * Finds disclosure groups optimized for interest calculation processes.
     * 
     * Retrieves disclosure groups with optimized field selection for interest
     * calculation operations. Custom query includes only essential fields for
     * calculation processes, reducing memory usage and improving performance
     * for high-volume interest calculation batch jobs.
     * 
     * Supports the 4-hour batch processing window requirement through optimized
     * query execution and minimal data transfer for calculation operations.
     * 
     * @param accountGroupId the account group identifier for calculation scope
     * @param transactionTypeCode the transaction type code for rate determination
     * @return list of disclosure groups optimized for interest calculation
     */
    @Query("SELECT dg FROM DisclosureGroup dg WHERE dg.accountGroupId = :accountGroupId " +
           "AND dg.transactionTypeCode = :transactionTypeCode AND dg.active = true " +
           "ORDER BY dg.interestRate DESC")
    List<DisclosureGroup> findForInterestCalculation(
            @Param("accountGroupId") String accountGroupId,
            @Param("transactionTypeCode") String transactionTypeCode);

    /**
     * Inherited JpaRepository methods provide standard CRUD operations:
     * 
     * save(DisclosureGroup entity) - Saves or updates disclosure group
     * saveAll(Iterable<DisclosureGroup> entities) - Batch save operations
     * findById(Long id) - Retrieves disclosure group by primary key
     * findAll() - Retrieves all disclosure groups
     * delete(DisclosureGroup entity) - Deletes disclosure group
     * deleteById(Long id) - Deletes disclosure group by primary key
     * deleteAll() - Deletes all disclosure groups
     * count() - Returns total count of disclosure groups
     * existsById(Long id) - Checks existence by primary key
     * flush() - Synchronizes persistence context with database
     * 
     * All inherited methods maintain ACID compliance through Spring @Transactional
     * annotations and PostgreSQL transaction management, ensuring data consistency
     * equivalent to VSAM KSDS file operations while providing enhanced relational
     * database capabilities and performance optimization through B-tree indexing.
     */
}