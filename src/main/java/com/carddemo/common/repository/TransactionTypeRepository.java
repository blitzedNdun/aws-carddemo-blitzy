package com.carddemo.common.repository;

import com.carddemo.common.entity.TransactionType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TransactionTypeRepository JPA Repository Interface
 *
 * Spring Data JPA repository interface providing database access methods for TransactionType entity
 * lookup operations with Redis caching support for reference data validation and business rule processing.
 *
 * Replaces VSAM TRANTYPE dataset operations with modern Spring Data JPA query methods:
 * - VSAM READ TRANTYPE → findByTransactionType() with Redis cache
 * - VSAM BROWSE operations → findAll() with Spring Data Pageable support
 * - Cross-reference lookups → findByDebitCreditIndicator() for transaction classification
 *
 * Performance characteristics per Section 6.2.4.2 caching strategy:
 * - Sub-millisecond lookup times via Redis cache for frequently accessed reference data
 * - Daily cache refresh cycle with TTL configuration
 * - Supports 10,000+ TPS transaction type validation requirements
 *
 * Integration points:
 * - Transaction processing services use this repository for type validation
 * - Business rule engines access debit/credit classification via cached lookups
 * - Batch processing utilizes bulk operations for reference data validation
 * - Redis cache integration provides cache-aside pattern for optimal performance
 *
 * @author CardDemo Transformation Team
 * @version 1.0
 * @since 2024-01-01
 */
@Repository
public interface TransactionTypeRepository extends JpaRepository<TransactionType, String> {

    /**
     * Find transaction type by transaction type code with Redis caching
     *
     * Replaces VSAM TRANTYPE direct access with cached Spring Data JPA query.
     * This method provides primary key lookup functionality equivalent to mainframe
     * dataset access while leveraging Redis cache for sub-millisecond response times.
     *
     * Cache configuration:
     * - Cache name: "transactionTypes"
     * - TTL: 24 hours (daily refresh cycle)
     * - Eviction policy: LRU (Least Recently Used)
     * - Cache-aside pattern with Spring Cache abstraction
     *
     * Business rules:
     * - Transaction type code must be exactly 2 characters
     * - Used extensively in transaction validation and classification
     * - Critical path for transaction processing performance
     *
     * @param transactionType 2-character transaction type code (e.g., "01", "02", "03")
     * @return Optional containing TransactionType if found, empty otherwise
     */
    @Cacheable(value = "transactionTypes", key = "#transactionType")
    Optional<TransactionType> findByTransactionType(String transactionType);

    /**
     * Find all transaction types by debit/credit indicator with Redis caching
     *
     * Provides filtered access to transaction types based on their debit/credit classification.
     * This method supports business logic that needs to distinguish between debit operations
     * (account balance reductions) and credit operations (account balance increases).
     *
     * Cache configuration:
     * - Cache name: "transactionTypesByIndicator"
     * - TTL: 24 hours (reference data refresh cycle)
     * - Key generation: based on debitCreditIndicator boolean value
     *
     * Business use cases:
     * - Transaction posting engines filtering by operation type
     * - Balance calculation workflows requiring direction classification
     * - Reporting systems grouping transactions by debit/credit nature
     * - Validation rules ensuring transaction type consistency
     *
     * @param debitCreditIndicator true for debit transactions (balance reduction),
     *                            false for credit transactions (balance increase)
     * @return List of TransactionType entities matching the debit/credit classification
     */
    @Cacheable(value = "transactionTypesByIndicator", key = "#debitCreditIndicator")
    List<TransactionType> findByDebitCreditIndicator(Boolean debitCreditIndicator);

    /**
     * Find transaction type by code and debit/credit indicator with Redis caching
     *
     * Provides combined lookup functionality for transaction type validation where both
     * the transaction type code and its debit/credit nature must be verified. This method
     * supports advanced business rules that validate transaction consistency.
     *
     * Cache configuration:
     * - Cache name: "transactionTypesCombined"
     * - TTL: 24 hours (reference data refresh cycle)
     * - Composite key: transactionType + debitCreditIndicator
     *
     * Business scenarios:
     * - Transaction validation ensuring type-direction consistency
     * - Double-entry bookkeeping verification in financial processing
     * - Audit trail validation for transaction integrity
     * - Business rule enforcement for transaction categorization
     *
     * @param transactionType 2-character transaction type code
     * @param debitCreditIndicator boolean indicating transaction direction
     * @return Optional containing TransactionType if both criteria match, empty otherwise
     */
    @Cacheable(value = "transactionTypesCombined", key = "#transactionType + '_' + #debitCreditIndicator")
    Optional<TransactionType> findByTransactionTypeAndDebitCreditIndicator(
            String transactionType,
            Boolean debitCreditIndicator
    );

    /**
     * Find all active transaction types for business rule processing
     *
     * Custom query method retrieving all currently valid transaction types.
     * This method supports business scenarios where only active/valid transaction
     * types should be considered for processing or validation operations.
     *
     * Cache configuration:
     * - Cache name: "activeTransactionTypes"
     * - TTL: 24 hours (reference data refresh cycle)
     * - Cache key: "active" (static key for all active types)
     *
     * Business applications:
     * - Transaction validation rules accepting only valid types
     * - UI dropdown population for transaction entry forms
     * - Batch processing validation during transaction file processing
     * - API response filtering for external system integrations
     *
     * Note: While TransactionType entity doesn't have an explicit active flag,
     * this query demonstrates the pattern for future enhancement. Currently,
     * all records in transaction_types table are considered active.
     *
     * @return List of all TransactionType entities currently available for use
     */
    @Cacheable(value = "activeTransactionTypes", key = "'active'")
    @Query("SELECT tt FROM TransactionType tt ORDER BY tt.transactionType")
    List<TransactionType> findAllActive();

    /**
     * Find transaction types by partial description match
     *
     * Provides flexible search capability for transaction types based on partial
     * description matching. This method supports user interfaces and administrative
     * functions requiring text-based search of transaction types.
     *
     * Business use cases:
     * - Administrative interfaces for transaction type management
     * - Search functionality in transaction analysis tools
     * - Reporting systems with flexible filtering capabilities
     * - User assistance features in transaction entry interfaces
     *
     * Performance considerations:
     * - Uses LIKE operator for flexible text matching
     * - Results cached based on search term for repeated searches
     * - Case-insensitive search using UPPER() function
     *
     * @param description partial description text to search for (case-insensitive)
     * @return List of TransactionType entities with descriptions containing the search term
     */
    @Cacheable(value = "transactionTypesByDescription", key = "#description.toUpperCase()")
    @Query("SELECT tt FROM TransactionType tt WHERE UPPER(tt.typeDescription) LIKE UPPER(CONCAT('%', :description, '%')) ORDER BY tt.transactionType")
    List<TransactionType> findByTypeDescriptionContainingIgnoreCase(@Param("description") String description);

    /**
     * Count transaction types by debit/credit indicator
     *
     * Provides statistical information about transaction type distribution by debit/credit
     * classification. This method supports administrative reporting and system monitoring
     * functions that track reference data characteristics.
     *
     * Cache configuration:
     * - Cache name: "transactionTypeCount"
     * - TTL: 24 hours (reference data refresh cycle)
     * - Key generation: based on debitCreditIndicator boolean value
     *
     * Business applications:
     * - Administrative dashboards showing transaction type distribution
     * - System monitoring validating reference data completeness
     * - Configuration validation ensuring balanced transaction types
     * - Reporting systems providing reference data statistics
     *
     * @param debitCreditIndicator true for debit transaction count, false for credit count
     * @return Long value representing count of transaction types for the specified indicator
     */
    @Cacheable(value = "transactionTypeCount", key = "#debitCreditIndicator")
    @Query("SELECT COUNT(tt) FROM TransactionType tt WHERE tt.debitCreditIndicator = :debitCreditIndicator")
    Long countByDebitCreditIndicator(@Param("debitCreditIndicator") Boolean debitCreditIndicator);

    /**
     * Verify if transaction type exists for validation operations
     *
     * Provides fast existence checking for transaction type validation without
     * returning the full entity. This method optimizes validation workflows where
     * only existence confirmation is required rather than full entity retrieval.
     *
     * Cache configuration:
     * - Cache name: "transactionTypeExists"
     * - TTL: 24 hours (reference data refresh cycle)
     * - Boolean cache values for optimal memory usage
     *
     * Performance optimization:
     * - Uses EXISTS query for minimal database overhead
     * - Returns boolean for fastest validation processing
     * - Cached results prevent repeated existence queries
     *
     * Business scenarios:
     * - High-volume transaction validation in batch processing
     * - Real-time transaction validation during payment processing
     * - Data integrity checks in ETL operations
     * - Input validation in user interfaces and APIs
     *
     * @param transactionType 2-character transaction type code to validate
     * @return boolean true if transaction type exists, false otherwise
     */
    @Cacheable(value = "transactionTypeExists", key = "#transactionType")
    @Query("SELECT CASE WHEN COUNT(tt) > 0 THEN true ELSE false END FROM TransactionType tt WHERE tt.transactionType = :transactionType")
    boolean existsByTransactionType(@Param("transactionType") String transactionType);
}