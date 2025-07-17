package com.carddemo.common.repository;

import com.carddemo.common.entity.TransactionType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for TransactionType entity providing database access methods
 * for transaction type lookup operations with Redis caching support for reference data validation
 * and business rule processing.
 * 
 * This repository replaces VSAM TRANTYPE file operations with modern JPA query methods,
 * implementing Redis caching per Section 6.2.4.2 caching strategy to optimize frequently
 * accessed reference data and support high-volume transaction processing requirements.
 * 
 * The repository enables transaction type classification and validation for the CardDemo
 * microservices architecture, supporting both debit and credit transaction processing
 * with comprehensive caching integration for sub-200ms response times.
 * 
 * Original VSAM file: TRANTYPE with 2-character transaction type codes
 * Cache integration: Redis TTL-based caching with Spring Cache abstraction
 * Performance target: < 5ms lookup time for cached reference data
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@Repository
public interface TransactionTypeRepository extends JpaRepository<TransactionType, String> {

    /**
     * Finds a transaction type by its unique transaction type code.
     * Replaces VSAM TRANTYPE read operations with JPA query method.
     * 
     * This method provides direct lookup functionality for transaction type validation
     * and business rule processing, with Redis caching enabled for optimal performance.
     * The cache key is generated from the transaction type parameter.
     * 
     * @param transactionType 2-character transaction type code (e.g., "01", "02")
     * @return Optional containing the TransactionType if found, empty otherwise
     * 
     * @since 1.0
     */
    @Cacheable(value = "transactionTypes", key = "#transactionType", cacheManager = "redisCacheManager")
    Optional<TransactionType> findByTransactionType(String transactionType);

    /**
     * Finds all transaction types by their debit/credit indicator classification.
     * Supports filtering transaction types based on transaction direction for
     * business rule processing and validation workflows.
     * 
     * This method enables bulk operations for transaction type classification,
     * supporting both debit (true) and credit (false) transaction processing.
     * Results are cached by indicator value for optimal performance.
     * 
     * @param debitCreditIndicator true for debit transactions, false for credit transactions
     * @return List of TransactionType entities matching the debit/credit classification
     * 
     * @since 1.0
     */
    @Cacheable(value = "transactionTypesByIndicator", key = "#debitCreditIndicator", cacheManager = "redisCacheManager")
    List<TransactionType> findByDebitCreditIndicator(Boolean debitCreditIndicator);

    /**
     * Finds a transaction type by both transaction type code and debit/credit indicator.
     * Provides composite lookup functionality for precise transaction type validation
     * and enhanced business rule processing with dual-criteria matching.
     * 
     * This method supports advanced validation scenarios where both transaction type
     * and direction need to be verified simultaneously for financial processing rules.
     * 
     * @param transactionType 2-character transaction type code (e.g., "01", "02")
     * @param debitCreditIndicator true for debit transactions, false for credit transactions
     * @return Optional containing the TransactionType if found with both criteria, empty otherwise
     * 
     * @since 1.0
     */
    @Cacheable(value = "transactionTypesComposite", key = "#transactionType + '_' + #debitCreditIndicator", cacheManager = "redisCacheManager")
    Optional<TransactionType> findByTransactionTypeAndDebitCreditIndicator(String transactionType, Boolean debitCreditIndicator);

    /**
     * Finds all transaction types ordered by transaction type code.
     * Provides sorted reference data for UI display and administrative operations.
     * 
     * This method returns all transaction types in ascending order by their type code,
     * supporting menu generation and administrative functions with consistent ordering.
     * Results are cached for optimal performance in reference data scenarios.
     * 
     * @return List of all TransactionType entities ordered by transaction type code
     * 
     * @since 1.0
     */
    @Cacheable(value = "allTransactionTypes", cacheManager = "redisCacheManager")
    List<TransactionType> findAllByOrderByTransactionTypeAsc();

    /**
     * Finds all active transaction types based on system configuration.
     * Supports filtering for active transaction types in production environments
     * where certain transaction types may be temporarily disabled.
     * 
     * Note: This method assumes transaction types are inherently active unless
     * explicitly configured otherwise. For future enhancement, a status field
     * could be added to the TransactionType entity.
     * 
     * @return List of all TransactionType entities (currently all types are active)
     * 
     * @since 1.0
     */
    @Cacheable(value = "activeTransactionTypes", cacheManager = "redisCacheManager")
    default List<TransactionType> findActiveTransactionTypes() {
        return findAllByOrderByTransactionTypeAsc();
    }

    /**
     * Checks if a transaction type exists by its code.
     * Provides efficient existence validation for transaction type codes
     * without requiring full entity retrieval.
     * 
     * This method supports validation workflows where only existence confirmation
     * is needed, optimizing performance for validation-only scenarios.
     * 
     * @param transactionType 2-character transaction type code to validate
     * @return true if the transaction type exists, false otherwise
     * 
     * @since 1.0
     */
    @Cacheable(value = "transactionTypeExists", key = "#transactionType", cacheManager = "redisCacheManager")
    boolean existsByTransactionType(String transactionType);

    /**
     * Counts transaction types by debit/credit indicator.
     * Provides statistical information for transaction type distribution
     * and administrative reporting purposes.
     * 
     * This method supports operational metrics and system monitoring
     * by providing counts of debit vs credit transaction types.
     * 
     * @param debitCreditIndicator true for debit transactions, false for credit transactions
     * @return count of transaction types matching the debit/credit classification
     * 
     * @since 1.0
     */
    @Cacheable(value = "transactionTypeCount", key = "#debitCreditIndicator", cacheManager = "redisCacheManager")
    long countByDebitCreditIndicator(Boolean debitCreditIndicator);

    // Note: The following methods are inherited from JpaRepository<TransactionType, String>
    // and provide standard CRUD operations with automatic transaction management:
    // - findById(String id): Find by primary key (transaction type code)
    // - save(TransactionType entity): Save or update transaction type
    // - findAll(): Retrieve all transaction types
    // - delete(TransactionType entity): Delete transaction type
    // - deleteById(String id): Delete by primary key
    // - existsById(String id): Check existence by primary key
    // - count(): Count all transaction types

}