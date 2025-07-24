package com.carddemo.common.repository;

import com.carddemo.common.entity.TransactionCategory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for TransactionCategory entity providing
 * database access methods for transaction category lookup operations with Redis caching support.
 * 
 * This repository replaces VSAM TRANCATG file access patterns from the COBOL system,
 * implementing the caching strategy per Section 6.2.4.2 for frequently accessed reference data.
 * 
 * The repository supports:
 * - Transaction category validation and lookup operations
 * - Business rule processing for financial transaction categorization  
 * - Redis cache integration for performance optimization
 * - Spring Data JPA automatic transaction management
 * 
 * Maps from original COBOL file operations:
 * - VSAM TRANCATG READ operations → findByTransactionCategory methods
 * - Category validation logic → findByActiveStatus filtering
 * - Reference data access → cached lookup operations
 * 
 * Cache Configuration:
 * - Cache name: "transactionCategories" 
 * - TTL: Configured via application properties for reference data optimization
 * - Eviction: Automatic on entity updates via @CacheEvict in entity class
 */
@Repository
public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, String> {

    /**
     * Finds a transaction category by its 4-character category code.
     * Implements primary lookup functionality replacing VSAM TRANCATG key-based access.
     * 
     * This method provides cached access to category data with Redis integration
     * for optimal performance in high-volume transaction processing scenarios.
     * 
     * @param transactionCategory 4-character transaction category code (e.g., "0001", "0002")
     * @return Optional containing the TransactionCategory if found, empty otherwise
     * 
     * Cache Strategy:
     * - Cache key: transaction category code
     * - Cache miss: Database query execution
     * - Cache hit: Direct Redis retrieval
     * 
     * Example usage:
     * Optional<TransactionCategory> category = repository.findByTransactionCategory("0001");
     */
    @Cacheable(value = "transactionCategories", key = "#transactionCategory")
    Optional<TransactionCategory> findByTransactionCategory(String transactionCategory);

    /**
     * Finds all transaction categories filtered by their active status.
     * Supports business rule processing by providing access to only active categories
     * for transaction validation and categorization operations.
     * 
     * This method enables category lifecycle management, allowing the system to
     * work with currently active categories while preserving historical data integrity.
     * 
     * @param activeStatus true to find active categories, false for inactive categories
     * @return List of TransactionCategory entities matching the active status filter
     * 
     * Cache Strategy:
     * - Cache key: active status boolean value
     * - Separate cache entries for active/inactive category lists
     * - Automatic cache eviction when category status changes
     * 
     * Business Rules:
     * - Active categories (true): Available for new transaction assignments
     * - Inactive categories (false): Historical data preserved, no new assignments
     */
    @Cacheable(value = "transactionCategoriesByStatus", key = "#activeStatus")
    List<TransactionCategory> findByActiveStatus(Boolean activeStatus);

    /**
     * Finds a specific transaction category by code and active status.
     * Provides combined lookup functionality for validation scenarios where both
     * category existence and active status must be verified simultaneously.
     * 
     * This method supports transaction processing validation by ensuring that
     * only active, valid categories are used for new transaction assignments.
     * 
     * @param transactionCategory 4-character transaction category code
     * @param activeStatus required active status for the category
     * @return Optional containing the TransactionCategory if found with matching status
     * 
     * Cache Strategy:
     * - Cache key: combination of category code and active status
     * - Optimized for validation operations in transaction processing
     * - Reduces database queries for frequent validation checks
     * 
     * Validation Use Cases:
     * - New transaction category assignment validation
     * - Business rule enforcement for active category requirements
     * - Transaction processing validation workflows
     */
    @Cacheable(value = "transactionCategoriesValidation", key = "#transactionCategory + '_' + #activeStatus")
    Optional<TransactionCategory> findByTransactionCategoryAndActiveStatus(
            String transactionCategory, Boolean activeStatus);

    /**
     * Finds all active transaction categories ordered by category code.
     * Provides a sorted list of currently active categories for UI presentation
     * and business processing scenarios requiring consistent ordering.
     * 
     * This method supports dropdown lists, category selection interfaces,
     * and reporting functions that need alphabetically sorted active categories.
     * 
     * @return List of active TransactionCategory entities ordered by category code
     * 
     * Cache Strategy:
     * - Cache key: "active_ordered" for consistent cache naming
     * - Long TTL appropriate for reference data stability
     * - Cache warming strategy for frequently accessed ordered lists
     */
    @Cacheable(value = "transactionCategoriesActiveOrdered", key = "'active_ordered'")
    @Query("SELECT tc FROM TransactionCategory tc WHERE tc.activeStatus = true ORDER BY tc.transactionCategory ASC")
    List<TransactionCategory> findActiveTransactionCategoriesOrdered();

    /**
     * Finds transaction categories by partial description match (case-insensitive).
     * Supports search functionality for administrative interfaces and category
     * management operations requiring flexible lookup capabilities.
     * 
     * This method enables category discovery by description content, supporting
     * user-friendly category management and reporting scenarios.
     * 
     * @param descriptionPattern partial description text for matching (case-insensitive)
     * @return List of TransactionCategory entities with descriptions containing the pattern
     * 
     * Cache Strategy:
     * - Cache key: normalized search pattern 
     * - Shorter TTL due to dynamic search nature
     * - Cache size limits for search result management
     */
    @Cacheable(value = "transactionCategoriesSearch", key = "#descriptionPattern.toLowerCase()")
    @Query("SELECT tc FROM TransactionCategory tc WHERE LOWER(tc.categoryDescription) LIKE LOWER(CONCAT('%', :pattern, '%'))")
    List<TransactionCategory> findByDescriptionContainingIgnoreCase(@Param("pattern") String descriptionPattern);

    /**
     * Counts the number of active transaction categories.
     * Provides administrative metrics for category management and system monitoring.
     * 
     * This method supports dashboard displays and administrative reporting
     * by providing quick access to active category counts without full entity retrieval.
     * 
     * @return count of active transaction categories
     * 
     * Cache Strategy:
     * - Cache key: "count_active" for metric caching
     * - Medium TTL balancing accuracy and performance
     * - Automatic eviction on category status changes
     */
    @Cacheable(value = "transactionCategoriesCount", key = "'count_active'")
    @Query("SELECT COUNT(tc) FROM TransactionCategory tc WHERE tc.activeStatus = true")
    Long countActiveTransactionCategories();

    /**
     * Checks if a transaction category exists and is active.
     * Provides efficient boolean validation for transaction processing scenarios
     * requiring quick category validity checks without full entity retrieval.
     * 
     * This method optimizes validation workflows by returning only boolean results,
     * reducing memory usage and network overhead in high-volume processing.
     * 
     * @param transactionCategory 4-character transaction category code to validate
     * @return true if category exists and is active, false otherwise
     * 
     * Cache Strategy:
     * - Cache key: category code with "_exists" suffix
     * - High-performance boolean caching for validation operations
     * - Automatic cache eviction on category changes
     * 
     * Performance Benefits:
     * - Reduced database query execution for validation
     * - Minimal memory usage for boolean results
     * - Optimized for high-frequency validation operations
     */
    @Cacheable(value = "transactionCategoriesExists", key = "#transactionCategory + '_exists'")
    @Query("SELECT CASE WHEN COUNT(tc) > 0 THEN true ELSE false END FROM TransactionCategory tc " +
           "WHERE tc.transactionCategory = :category AND tc.activeStatus = true")
    Boolean existsActiveTransactionCategory(@Param("category") String transactionCategory);

    /**
     * Finds all transaction categories for administrative operations without caching.
     * Provides uncached access for administrative interfaces requiring real-time data
     * for category management, bulk operations, and system maintenance tasks.
     * 
     * This method bypasses caching to ensure administrators see the most current
     * category data including recent changes and inactive categories.
     * 
     * @return List of all TransactionCategory entities (active and inactive)
     * 
     * Administrative Use Cases:
     * - Category management interfaces
     * - Bulk category operations
     * - System maintenance and data verification
     * - Audit and compliance reporting
     */
    @Query("SELECT tc FROM TransactionCategory tc ORDER BY tc.transactionCategory ASC")
    List<TransactionCategory> findAllForAdmin();
}