/*
 * CardDemo Application
 * 
 * Transaction Category Repository Interface
 * 
 * Spring Data JPA repository interface for TransactionCategory entity providing
 * data access operations for transaction_categories reference table with composite
 * primary key. Manages transaction categorization replacing VSAM TRANCATG reference
 * file. Provides category lookup operations for transaction processing and reporting.
 * Uses @EmbeddedId for composite key of category and subcategory codes. Supports
 * caching for reference data optimization.
 * 
 * This repository replaces VSAM TRANCATG reference file I/O operations with modern
 * JPA method implementations for transaction_categories table access and composite
 * key handling. Enables caching of frequently accessed category lookups to improve
 * performance by avoiding repeated database queries for static reference data.
 */

package com.carddemo.repository;

import com.carddemo.entity.TransactionCategory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for TransactionCategory entity providing
 * data access operations for transaction_categories reference table with composite
 * primary key. Manages transaction categorization replacing VSAM TRANCATG reference
 * file.
 * 
 * This repository provides:
 * - Standard CRUD operations through JpaRepository
 * - Custom finder methods for category lookup operations
 * - Category hierarchy queries for reporting
 * - Caching support for performance optimization
 * - Composite key handling with @EmbeddedId
 * 
 * Key Features:
 * - Replaces VSAM TRANCATG file access patterns
 * - Supports transaction categorization workflows
 * - Enables category lookup for transaction processing
 * - Provides cursor-based pagination for large datasets
 * - Implements caching for frequently accessed reference data
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */
@Repository
@Cacheable("transactionCategories")
public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, Object> {

    /**
     * Retrieve all transaction categories by category code.
     * Supports category hierarchy queries for reporting.
     * 
     * @param categoryCode the category code to search for
     * @return list of matching transaction categories
     */
    @Cacheable("transactionCategoriesByCode")
    List<TransactionCategory> findByCategoryCode(@Param("categoryCode") String categoryCode);

    /**
     * Retrieve all transaction categories by subcategory code.
     * Enables subcategory-based filtering for transaction classification.
     * 
     * @param subcategoryCode the subcategory code to search for
     * @return list of matching transaction categories
     */
    @Cacheable("transactionCategoriesBySubcode")
    List<TransactionCategory> findBySubcategoryCode(@Param("subcategoryCode") String subcategoryCode);

    /**
     * Retrieve all transaction categories by transaction type code.
     * Supports transaction type classification workflows.
     * 
     * @param transactionTypeCode the transaction type code to search for
     * @return list of matching transaction categories
     */
    @Cacheable("transactionCategoriesByType")
    List<TransactionCategory> findByTransactionTypeCode(@Param("transactionTypeCode") String transactionTypeCode);

    /**
     * Retrieve specific transaction category by composite key.
     * Primary lookup method for exact category identification.
     * 
     * @param categoryCode the category code (primary key component)
     * @param subcategoryCode the subcategory code (primary key component)
     * @return optional containing the matching transaction category, if found
     */
    @Cacheable("transactionCategoryByComposite")
    Optional<TransactionCategory> findByCategoryCodeAndSubcategoryCode(
            @Param("categoryCode") String categoryCode,
            @Param("subcategoryCode") String subcategoryCode);

    /**
     * Retrieve all transaction categories by transaction type code, ordered by category code.
     * Provides sorted category listings for transaction type analysis.
     * 
     * @param transactionTypeCode the transaction type code to search for
     * @return list of matching transaction categories sorted by category code
     */
    @Cacheable("transactionCategoriesByTypeOrdered")
    List<TransactionCategory> findByTransactionTypeCodeOrderByCategoryCodeAsc(
            @Param("transactionTypeCode") String transactionTypeCode);

    /**
     * Search transaction categories by description containing the specified text (case-insensitive).
     * Enables flexible category search for user interfaces.
     * 
     * @param description the description text to search for
     * @return list of matching transaction categories
     */
    List<TransactionCategory> findByCategoryDescriptionContainingIgnoreCase(
            @Param("description") String description);

    /**
     * Search transaction categories by category name containing the specified text (case-insensitive).
     * Supports category name-based search operations.
     * 
     * @param categoryName the category name text to search for
     * @return list of matching transaction categories
     */
    List<TransactionCategory> findByCategoryNameContainingIgnoreCase(
            @Param("categoryName") String categoryName);

    /**
     * Count the number of transaction categories with the specified category code.
     * Provides category usage statistics for reporting.
     * 
     * @param categoryCode the category code to count
     * @return count of matching transaction categories
     */
    @Cacheable("transactionCategoryCount")
    long countByCategoryCode(@Param("categoryCode") String categoryCode);

    /**
     * Check if a transaction category exists with the specified composite key.
     * Efficient existence check for category validation.
     * 
     * @param categoryCode the category code (primary key component)
     * @param subcategoryCode the subcategory code (primary key component)
     * @return true if the category exists, false otherwise
     */
    @Cacheable("transactionCategoryExists")
    boolean existsByCategoryCodeAndSubcategoryCode(
            @Param("categoryCode") String categoryCode,
            @Param("subcategoryCode") String subcategoryCode);

    /**
     * Retrieve all transaction categories with pagination support.
     * Enables cursor-based pagination for large datasets replicating VSAM browse operations.
     * 
     * @param pageable pagination information including page size, page number, and sorting criteria
     * @return page of transaction categories with pagination metadata
     */
    @Override
    org.springframework.data.domain.Page<TransactionCategory> findAll(Pageable pageable);

    // Inherited standard CRUD operations from JpaRepository:
    // - save(TransactionCategory entity)
    // - findById(Object id)
    // - findAll()
    // - delete(TransactionCategory entity)
    // - deleteById(Object id)
    // - deleteAll()
    // - count()
    // - existsById(Object id)
    // - flush()
}