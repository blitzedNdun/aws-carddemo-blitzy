/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.repository;

import com.carddemo.entity.TransactionCategoryBalance;
import com.carddemo.entity.TransactionCategoryBalance.TransactionCategoryBalanceKey;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for TransactionCategoryBalance entity providing data access 
 * for transaction_category_balance table with composite primary key support.
 * 
 * Manages category-wise balance tracking operations replacing VSAM indexed file patterns from
 * the original COBOL TRAN-CAT-BAL-RECORD structure. Provides comprehensive balance management
 * including account-category queries, date-range balance retrievals, and batch balance updates
 * for end-of-day processing.
 * 
 * Features:
 * - Composite primary key handling using @EmbeddedId annotation
 * - Custom query methods for balance retrieval by account, category, and date combinations
 * - Support for batch balance updates in end-of-day processing scenarios
 * - Balance history queries for statement generation and reporting
 * - Cursor-based pagination replicating VSAM STARTBR/READNEXT/READPREV operations
 * - Aggregation methods for balance summation and financial calculations
 * 
 * Key Methods:
 * - Standard JPA CRUD operations with composite key support
 * - Account-category balance queries with date filtering
 * - Date range balance history retrieval
 * - Balance threshold filtering for limit management
 * - Batch update operations for daily balance processing
 * - Balance summary and aggregation operations
 * 
 * Database Mapping:
 * - Table: transaction_category_balance
 * - Primary Key: Composite (account_id, category_code, balance_date)
 * - Foreign Keys: account_id → account_data.account_id
 * 
 * Performance Considerations:
 * - Uses PostgreSQL B-tree indexes on composite primary key
 * - Pageable support for large result sets with LIMIT/OFFSET optimization
 * - Custom queries optimized for balance reporting and batch processing patterns
 * - Support for balance date range queries with partition pruning
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Repository
public interface TransactionCategoryBalanceRepository extends JpaRepository<TransactionCategoryBalance, TransactionCategoryBalanceKey> {

    // ========================================
    // ACCOUNT-CATEGORY BALANCE QUERIES
    // ========================================

    /**
     * Find all balance records for a specific account and category code.
     * Retrieves balance history across all dates for the account-category combination.
     * 
     * @param accountId the account ID to search for
     * @param categoryCode the category code to search for (4-character code)
     * @return list of balance records ordered by balance date descending
     */
    @Query("SELECT tcb FROM TransactionCategoryBalance tcb WHERE tcb.id.accountId = :accountId AND tcb.id.categoryCode = :categoryCode ORDER BY tcb.id.balanceDate DESC")
    List<TransactionCategoryBalance> findByAccountIdAndCategoryCode(@Param("accountId") Long accountId, 
                                                                   @Param("categoryCode") String categoryCode);

    /**
     * Find balance record for a specific account, category, and date combination.
     * Returns the exact balance for the specified account-category-date key.
     * 
     * @param accountId the account ID to search for
     * @param categoryCode the category code to search for (4-character code)
     * @param balanceDate the specific balance date
     * @return optional balance record for the exact key combination
     */
    @Query("SELECT tcb FROM TransactionCategoryBalance tcb WHERE tcb.id.accountId = :accountId AND tcb.id.categoryCode = :categoryCode AND tcb.id.balanceDate = :balanceDate")
    Optional<TransactionCategoryBalance> findByAccountIdAndCategoryCodeAndBalanceDate(@Param("accountId") Long accountId,
                                                                                     @Param("categoryCode") String categoryCode,
                                                                                     @Param("balanceDate") LocalDate balanceDate);

    // ========================================
    // DATE RANGE BALANCE QUERIES
    // ========================================

    /**
     * Find all balance records for an account within a date range.
     * Supports balance history queries for statement generation and reporting.
     * 
     * @param accountId the account ID to search for
     * @param startDate the start date of the range (inclusive)
     * @param endDate the end date of the range (inclusive)
     * @return list of balance records within the date range ordered by balance date
     */
    @Query("SELECT tcb FROM TransactionCategoryBalance tcb WHERE tcb.id.accountId = :accountId AND tcb.id.balanceDate >= :startDate AND tcb.id.balanceDate <= :endDate ORDER BY tcb.id.balanceDate DESC, tcb.id.categoryCode")
    List<TransactionCategoryBalance> findByAccountIdAndBalanceDateBetween(@Param("accountId") Long accountId,
                                                                          @Param("startDate") LocalDate startDate,
                                                                          @Param("endDate") LocalDate endDate);

    /**
     * Find balance records for a specific category and date across all accounts.
     * Useful for category-wide balance analysis and reporting.
     * 
     * @param categoryCode the category code to search for
     * @param balanceDate the specific balance date
     * @return list of balance records for all accounts in the category
     */
    @Query("SELECT tcb FROM TransactionCategoryBalance tcb WHERE tcb.id.categoryCode = :categoryCode AND tcb.id.balanceDate = :balanceDate ORDER BY tcb.id.accountId")
    List<TransactionCategoryBalance> findByCategoryCodeAndBalanceDate(@Param("categoryCode") String categoryCode,
                                                                      @Param("balanceDate") LocalDate balanceDate);

    /**
     * Find all balance records for an account ordered by balance date descending.
     * Provides complete balance history for account analysis with cursor pagination support.
     * 
     * @param accountId the account ID to search for
     * @param pageable pagination information for large result sets
     * @return page of balance records ordered by date descending
     */
    @Query("SELECT tcb FROM TransactionCategoryBalance tcb WHERE tcb.id.accountId = :accountId ORDER BY tcb.id.balanceDate DESC, tcb.id.categoryCode")
    Page<TransactionCategoryBalance> findByAccountIdOrderByBalanceDateDesc(@Param("accountId") Long accountId,
                                                                           Pageable pageable);

    // ========================================
    // BALANCE THRESHOLD AND FILTERING QUERIES
    // ========================================

    /**
     * Find balance records with balance greater than the specified threshold.
     * Supports credit limit monitoring and high-balance account identification.
     * 
     * @param threshold the balance threshold to compare against
     * @return list of balance records exceeding the threshold
     */
    @Query("SELECT tcb FROM TransactionCategoryBalance tcb WHERE tcb.balance > :threshold ORDER BY tcb.balance DESC, tcb.id.accountId")
    List<TransactionCategoryBalance> findByBalanceGreaterThan(@Param("threshold") BigDecimal threshold);

    /**
     * Find balance records with balance less than the specified threshold.
     * Supports negative balance monitoring and overdraft detection.
     * 
     * @param threshold the balance threshold to compare against
     * @return list of balance records below the threshold
     */
    @Query("SELECT tcb FROM TransactionCategoryBalance tcb WHERE tcb.balance < :threshold ORDER BY tcb.balance ASC, tcb.id.accountId")
    List<TransactionCategoryBalance> findByBalanceLessThan(@Param("threshold") BigDecimal threshold);

    // ========================================
    // BALANCE AGGREGATION AND SUMMARY QUERIES
    // ========================================

    /**
     * Calculate the sum of balances for a specific account and category across all dates.
     * Provides category balance totals for account summary reporting.
     * 
     * @param accountId the account ID to sum balances for
     * @param categoryCode the category code to sum balances for
     * @return the total balance sum, or zero if no records found
     */
    @Query("SELECT COALESCE(SUM(tcb.balance), 0) FROM TransactionCategoryBalance tcb WHERE tcb.id.accountId = :accountId AND tcb.id.categoryCode = :categoryCode")
    BigDecimal sumBalanceByAccountIdAndCategoryCode(@Param("accountId") Long accountId,
                                                   @Param("categoryCode") String categoryCode);

    /**
     * Find the most recent balance record for a specific account and category.
     * Returns the latest balance entry for current balance determination.
     * 
     * @param accountId the account ID to search for
     * @param categoryCode the category code to search for
     * @return optional latest balance record
     */
    @Query("SELECT tcb FROM TransactionCategoryBalance tcb WHERE tcb.id.accountId = :accountId AND tcb.id.categoryCode = :categoryCode ORDER BY tcb.id.balanceDate DESC LIMIT 1")
    Optional<TransactionCategoryBalance> findLatestBalanceByAccountIdAndCategoryCode(@Param("accountId") Long accountId,
                                                                                    @Param("categoryCode") String categoryCode);

    // ========================================
    // DATA MAINTENANCE AND CLEANUP QUERIES
    // ========================================

    /**
     * Delete balance records for an account before a specified date.
     * Supports data archival and retention policy implementation.
     * 
     * @param accountId the account ID to delete records for
     * @param cutoffDate the cutoff date (records before this date will be deleted)
     * @return the number of records deleted
     */
    @Modifying
    @Query("DELETE FROM TransactionCategoryBalance tcb WHERE tcb.id.accountId = :accountId AND tcb.id.balanceDate < :cutoffDate")
    int deleteByAccountIdAndBalanceDateBefore(@Param("accountId") Long accountId,
                                             @Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Count balance records for a specific account and category combination.
     * Provides data volume metrics for balance tracking analysis.
     * 
     * @param accountId the account ID to count records for
     * @param categoryCode the category code to count records for
     * @return the count of balance records
     */
    @Query("SELECT COUNT(tcb) FROM TransactionCategoryBalance tcb WHERE tcb.id.accountId = :accountId AND tcb.id.categoryCode = :categoryCode")
    long countByAccountIdAndCategoryCode(@Param("accountId") Long accountId,
                                        @Param("categoryCode") String categoryCode);

    // ========================================
    // BALANCE HISTORY AND REPORTING QUERIES
    // ========================================

    /**
     * Find complete balance history for an account with pagination support.
     * Supports comprehensive balance reporting and statement generation.
     * Replicates VSAM browse operations with STARTBR/READNEXT patterns.
     * 
     * @param accountId the account ID to retrieve balance history for
     * @param pageable pagination information including page size and sorting
     * @return page of balance records with complete history
     */
    @Query("SELECT tcb FROM TransactionCategoryBalance tcb WHERE tcb.id.accountId = :accountId ORDER BY tcb.id.balanceDate DESC, tcb.id.categoryCode")
    Page<TransactionCategoryBalance> findBalanceHistoryByAccountId(@Param("accountId") Long accountId,
                                                                  Pageable pageable);

    // ========================================
    // BATCH PROCESSING AND END-OF-DAY OPERATIONS
    // ========================================

    /**
     * Update balance amounts for end-of-day processing in batch operations.
     * Supports bulk balance updates for daily balance processing scenarios.
     * 
     * This method provides a framework for batch balance updates but requires
     * specific business logic implementation in the service layer due to the
     * complexity of balance calculations and the composite key structure.
     * 
     * @param accountId the account ID to update balances for
     * @param categoryCode the category code to update balances for
     * @param balanceDate the balance date to update
     * @param newBalance the new balance amount
     * @return the number of records updated
     */
    @Modifying
    @Query("UPDATE TransactionCategoryBalance tcb SET tcb.balance = :newBalance WHERE tcb.id.accountId = :accountId AND tcb.id.categoryCode = :categoryCode AND tcb.id.balanceDate = :balanceDate")
    int updateBalanceForEndOfDay(@Param("accountId") Long accountId,
                                @Param("categoryCode") String categoryCode,
                                @Param("balanceDate") LocalDate balanceDate,
                                @Param("newBalance") BigDecimal newBalance);

    // ========================================
    // STANDARD JPA REPOSITORY METHODS
    // (Inherited from JpaRepository<TransactionCategoryBalance, TransactionCategoryBalanceKey>)
    // ========================================

    /*
     * The following methods are automatically provided by Spring Data JPA through inheritance:
     * 
     * - save(TransactionCategoryBalance entity) : TransactionCategoryBalance
     * - saveAll(Iterable<TransactionCategoryBalance> entities) : List<TransactionCategoryBalance>
     * - findById(TransactionCategoryBalanceKey id) : Optional<TransactionCategoryBalance>
     * - findAll() : List<TransactionCategoryBalance>
     * - findAll(Pageable pageable) : Page<TransactionCategoryBalance>
     * - delete(TransactionCategoryBalance entity) : void
     * - deleteById(TransactionCategoryBalanceKey id) : void
     * - count() : long
     * - existsById(TransactionCategoryBalanceKey id) : boolean
     * - flush() : void
     * 
     * These methods provide complete CRUD operations for TransactionCategoryBalance entities
     * with automatic composite key handling through the TransactionCategoryBalanceKey embedded ID.
     * 
     * All methods support ACID transactions through Spring's @Transactional management
     * and leverage PostgreSQL's B-tree index optimization for efficient composite key operations.
     */
}