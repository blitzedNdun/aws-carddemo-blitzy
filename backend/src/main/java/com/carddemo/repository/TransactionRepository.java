/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.repository;

import com.carddemo.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository interface for Transaction entity providing comprehensive data access operations
 * for transactions table with partitioned structure. This repository replaces VSAM TRANSACT file access
 * with modern JPA operations while preserving equivalent functionality and performance characteristics.
 * 
 * <p>This repository interface supports:
 * <ul>
 * <li>Date-range queries with automatic partition pruning for efficient batch processing</li>
 * <li>Transaction history retrieval with pagination support for large result sets</li>
 * <li>Account-based filtering for customer transaction management</li>
 * <li>Card-based transaction lookup for payment processing</li>
 * <li>Amount-based filtering for fraud detection and reporting</li>
 * <li>Merchant-based searches for transaction categorization</li>
 * <li>Transaction posting and reversal operations with ACID compliance</li>
 * </ul>
 * 
 * <p>Key Features:
 * <ul>
 * <li>Replaces VSAM STARTBR/READNEXT/READPREV operations with JPA pagination</li>
 * <li>Supports PostgreSQL table partitioning for optimal performance</li>
 * <li>Provides cursor-based pagination equivalent to COBOL browse operations</li>
 * <li>Maintains transaction isolation and consistency through Spring @Transactional</li>
 * <li>Enables complex filtering and sorting operations through JPQL queries</li>
 * </ul>
 * 
 * <p>Migration Notes:
 * <ul>
 * <li>Converts COBOL COMP-3 packed decimal handling to BigDecimal precision</li>
 * <li>Maps VSAM key access patterns to PostgreSQL B-tree index utilization</li>
 * <li>Preserves original transaction processing logic and business rules</li>
 * <li>Supports equivalent response times to original CICS transaction processing</li>
 * </ul>
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Account-based transaction queries
    
    /**
     * Finds transactions for a specific account within a date range.
     * Replaces VSAM STARTBR operation with account ID and date filtering.
     * Supports partition pruning for efficient query execution.
     * 
     * @param accountId the account ID to filter by
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return List of transactions within the specified date range for the account
     */
    List<Transaction> findByAccountIdAndTransactionDateBetween(
        Long accountId, 
        LocalDate startDate, 
        LocalDate endDate
    );

    /**
     * Finds transactions for a specific account within a date range with pagination.
     * Equivalent to COBOL pagination logic with 10 records per page.
     * 
     * @param accountId the account ID to filter by
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @param pageable pagination information
     * @return Page of transactions with metadata for navigation
     */
    Page<Transaction> findByAccountIdAndTransactionDateBetween(
        Long accountId, 
        LocalDate startDate, 
        LocalDate endDate, 
        Pageable pageable
    );

    /**
     * Finds the most recent transaction for a specific account.
     * Replaces VSAM reverse browse operation (READPREV).
     * 
     * @param accountId the account ID to search
     * @return Optional containing the most recent transaction, or empty if none found
     */
    Optional<Transaction> findTopByAccountIdOrderByTransactionDateDesc(Long accountId);

    /**
     * Finds all transactions for a specific account ordered by transaction date (newest first).
     * Supports comprehensive transaction history display.
     * 
     * @param accountId the account ID to filter by
     * @return List of transactions ordered by date descending
     */
    List<Transaction> findByAccountIdOrderByTransactionDateDesc(Long accountId);

    /**
     * Finds transactions for a specific account before a specific date.
     * Supports historical data analysis and archival operations.
     * 
     * @param accountId the account ID to filter by
     * @param cutoffDate the cutoff date (exclusive)
     * @return List of transactions before the specified date
     */
    List<Transaction> findByAccountIdAndTransactionDateBefore(Long accountId, LocalDate cutoffDate);

    /**
     * Counts transactions for a specific account within a date range.
     * Supports batch processing statistics and performance monitoring.
     * 
     * @param accountId the account ID to count transactions for
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return count of transactions in the specified range
     */
    Long countByAccountIdAndTransactionDateBetween(
        Long accountId, 
        LocalDate startDate, 
        LocalDate endDate
    );

    // Card-based transaction queries
    
    /**
     * Finds transactions for a specific card within a date range.
     * Supports card-specific transaction analysis and fraud detection.
     * 
     * @param cardNumber the card number to filter by
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return List of transactions for the specified card and date range
     */
    List<Transaction> findByCardNumberAndTransactionDateBetween(
        String cardNumber, 
        LocalDate startDate, 
        LocalDate endDate
    );

    /**
     * Finds transactions for a specific card within a date range with pagination.
     * Supports large result set navigation for detailed card transaction history.
     * 
     * @param cardNumber the card number to filter by
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @param pageable pagination information
     * @return Page of transactions with navigation metadata
     */
    Page<Transaction> findByCardNumberAndTransactionDateBetween(
        String cardNumber, 
        LocalDate startDate, 
        LocalDate endDate, 
        Pageable pageable
    );

    // Date-based transaction queries for batch processing
    
    /**
     * Finds all transactions within a specific date range.
     * Supports batch processing operations and daily transaction reporting.
     * Utilizes partition pruning for optimal performance on large datasets.
     * 
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return List of all transactions within the specified date range
     */
    List<Transaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Finds transactions within a date range with pagination.
     * Supports large-scale batch processing with memory-efficient pagination.
     * 
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @param pageable pagination information
     * @return Page of transactions within the date range
     */
    Page<Transaction> findByTransactionDateBetween(
        LocalDate startDate, 
        LocalDate endDate, 
        Pageable pageable
    );

    /**
     * Finds transactions processed between specific dates.
     * Supports batch processing and settlement operations.
     * 
     * @param startDate the start processing date (inclusive)
     * @param endDate the end processing date (inclusive)
     * @return List of transactions processed within the specified date range
     */
    @Query("SELECT t FROM Transaction t WHERE t.processedTimestamp >= :startDate AND t.processedTimestamp <= :endDate")
    List<Transaction> findByProcessingDateBetween(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate
    );

    // Transaction type and category-based queries
    
    /**
     * Finds transactions by transaction type within a date range.
     * Supports transaction categorization and reporting operations.
     * 
     * @param transactionTypeCode the transaction type code to filter by
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return List of transactions matching the type and date range
     */
    @Query("SELECT t FROM Transaction t WHERE t.transactionTypeCode = :transactionTypeCode " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<Transaction> findByTransactionTypeAndTransactionDateBetween(
        @Param("transactionTypeCode") String transactionTypeCode,
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate
    );

    /**
     * Finds transactions by category code within a date range.
     * Supports spending analysis and financial reporting.
     * 
     * @param categoryCode the category code to filter by
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return List of transactions matching the category and date range
     */
    @Query("SELECT t FROM Transaction t WHERE t.categoryCode = :categoryCode " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<Transaction> findByCategoryCodeAndTransactionDateBetween(
        @Param("categoryCode") String categoryCode,
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate
    );

    // Amount-based transaction queries
    
    /**
     * Finds transactions with amount greater than specified value within a date range.
     * Supports fraud detection and high-value transaction monitoring.
     * 
     * @param minAmount the minimum amount threshold (exclusive)
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return List of transactions exceeding the amount threshold
     */
    List<Transaction> findByAmountGreaterThanAndTransactionDateBetween(
        BigDecimal minAmount, 
        LocalDate startDate, 
        LocalDate endDate
    );

    /**
     * Finds transactions with amount greater than specified value within a date range with pagination.
     * Supports large-scale fraud analysis with memory-efficient processing.
     * 
     * @param minAmount the minimum amount threshold (exclusive)
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @param pageable pagination information
     * @return Page of transactions exceeding the amount threshold
     */
    Page<Transaction> findByAmountGreaterThanAndTransactionDateBetween(
        BigDecimal minAmount, 
        LocalDate startDate, 
        LocalDate endDate, 
        Pageable pageable
    );

    // Merchant-based transaction queries
    
    /**
     * Finds transactions by merchant name containing specified text (case-insensitive).
     * Supports merchant analysis and dispute resolution.
     * 
     * @param merchantName the merchant name pattern to search for
     * @return List of transactions with matching merchant names
     */
    List<Transaction> findByMerchantNameContainingIgnoreCase(String merchantName);

    /**
     * Finds transactions by merchant name containing specified text with pagination.
     * Supports comprehensive merchant transaction analysis.
     * 
     * @param merchantName the merchant name pattern to search for
     * @param pageable pagination information
     * @return Page of transactions with matching merchant names
     */
    Page<Transaction> findByMerchantNameContainingIgnoreCase(String merchantName, Pageable pageable);

    // Utility and administrative queries
    
    /**
     * Finds transactions by simple account ID without date restrictions.
     * Provides complete transaction history for account analysis.
     * 
     * @param accountId the account ID to filter by
     * @return List of all transactions for the specified account
     */
    List<Transaction> findByAccountId(Long accountId);

    /**
     * Finds transactions by account ID with pagination.
     * Supports efficient browsing of large transaction histories.
     * 
     * @param accountId the account ID to filter by
     * @param pageable pagination information
     * @return Page of transactions for the specified account
     */
    Page<Transaction> findByAccountId(Long accountId, Pageable pageable);

    /**
     * Checks if a transaction exists by transaction ID.
     * Supports transaction validation and duplicate detection.
     * 
     * @param transactionId the transaction ID to check
     * @return true if transaction exists, false otherwise
     */
    boolean existsByTransactionId(Long transactionId);

    /**
     * Finds the transaction with the highest ID (most recently generated).
     * Supports transaction ID sequence validation and monitoring.
     * 
     * @return Optional containing the transaction with highest ID, or empty if no transactions exist
     */
    Optional<Transaction> findTopByOrderByTransactionIdDesc();

    // Advanced statistical and analytical queries
    
    /**
     * Calculates the total transaction amount for an account within a date range.
     * Supports account balance calculations and financial reporting.
     * 
     * @param accountId the account ID to calculate totals for
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return the sum of transaction amounts, or null if no transactions found
     */
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.accountId = :accountId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalAmountByAccountIdAndDateRange(
        @Param("accountId") Long accountId,
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate
    );

    /**
     * Finds transactions for an account with specific transaction type within date range.
     * Supports detailed transaction categorization and analysis.
     * 
     * @param accountId the account ID to filter by
     * @param transactionTypeCode the transaction type code
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return List of matching transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.accountId = :accountId " +
           "AND t.transactionTypeCode = :transactionTypeCode " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findByAccountIdAndTransactionTypeAndDateRange(
        @Param("accountId") Long accountId,
        @Param("transactionTypeCode") String transactionTypeCode,
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate
    );

    /**
     * Finds pending transactions (not yet processed) for an account.
     * Supports transaction processing and settlement operations.
     * 
     * @param accountId the account ID to search
     * @return List of pending transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.accountId = :accountId " +
           "AND t.processedTimestamp IS NULL " +
           "ORDER BY t.transactionDate ASC")
    List<Transaction> findPendingTransactionsByAccountId(@Param("accountId") Long accountId);

    /**
     * Finds duplicate transactions based on account, amount, and merchant within time window.
     * Supports fraud detection and duplicate prevention.
     * 
     * @param accountId the account ID to check
     * @param amount the transaction amount
     * @param merchantName the merchant name
     * @param cutoffDate the cutoff date for duplicate detection (time window calculated by caller)
     * @return List of potential duplicate transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.accountId = :accountId " +
           "AND t.amount = :amount " +
           "AND t.merchantName = :merchantName " +
           "AND t.transactionDate >= :cutoffDate " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findPotentialDuplicates(
        @Param("accountId") Long accountId,
        @Param("amount") BigDecimal amount,
        @Param("merchantName") String merchantName,
        @Param("cutoffDate") LocalDate cutoffDate
    );

    /**
     * Finds transactions requiring manual review based on business rules.
     * Supports automated transaction processing and exception handling.
     * 
     * @return List of transactions flagged for manual review
     */
    @Query("SELECT t FROM Transaction t WHERE " +
           "(t.amount > 1000.00 AND t.transactionTypeCode = 'PU') " +
           "OR (t.amount > 500.00 AND t.transactionTypeCode = 'CA') " +
           "OR t.description LIKE '%SUSPICIOUS%' " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findTransactionsRequiringReview();

    // Batch processing support methods
    
    /**
     * Finds unprocessed transactions for batch processing.
     * Supports nightly batch operations and settlement processing.
     * 
     * @param batchDate the date to process transactions for
     * @return List of transactions ready for batch processing
     */
    @Query("SELECT t FROM Transaction t WHERE t.transactionDate = :batchDate " +
           "AND t.processedTimestamp IS NULL " +
           "ORDER BY t.transactionId ASC")
    List<Transaction> findUnprocessedTransactionsForDate(@Param("batchDate") LocalDate batchDate);

    /**
     * Marks transactions as processed by updating the processed timestamp.
     * Supports batch processing completion tracking.
     * 
     * @param transactionIds the list of transaction IDs to mark as processed
     * @param processedTimestamp the timestamp to set
     * @return the number of transactions updated
     */
    @Query("UPDATE Transaction t SET t.processedTimestamp = :processedTimestamp " +
           "WHERE t.transactionId IN :transactionIds")
    int markTransactionsAsProcessed(
        @Param("transactionIds") List<Long> transactionIds,
        @Param("processedTimestamp") LocalDate processedTimestamp
    );

    /**
     * Counts all transactions for a specific account.
     * Used for pagination calculations and account activity analysis.
     * 
     * @param accountId the account ID to count transactions for
     * @return the total count of transactions for the account
     */
    Long countByAccountId(Long accountId);

    /**
     * Counts transactions for a specific account after a given date.
     * 
     * Used for account maintenance operations, dormancy analysis, and activity validation.
     * Supports transaction count validation for account closure and status update operations.
     * 
     * @param accountId the account ID to count transactions for
     * @param cutoffDate the cutoff date (transactions after this date are counted)
     * @return the count of transactions after the specified date
     */
    Long countByAccountIdAndTransactionDateAfter(Long accountId, LocalDate cutoffDate);

    /**
     * Finds transactions for a specific account within a LocalDateTime date range.
     * Provides flexibility for timestamp-based filtering and supports legacy COBOL conversion needs.
     * 
     * @param accountId the account ID to filter by
     * @param startDate the start date and time (inclusive)
     * @param endDate the end date and time (inclusive)
     * @param pageable pagination information
     * @return Page of transactions within the specified date range
     */
    @Query("SELECT t FROM Transaction t WHERE t.accountId = :accountId " +
           "AND t.originalTimestamp >= :startDate AND t.originalTimestamp <= :endDate " +
           "ORDER BY t.transactionId ASC")
    Page<Transaction> findByAccountIdAndDateRange(
        @Param("accountId") Long accountId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

}
