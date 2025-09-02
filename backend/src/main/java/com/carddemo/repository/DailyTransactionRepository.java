package com.carddemo.repository;

import com.carddemo.entity.DailyTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository interface for DailyTransaction entity providing data access operations 
 * for daily transaction batch import processing. Supports Spring Batch reader integration for 
 * processing daily transaction files, pagination for chunk-based processing, and validation 
 * queries for cross-reference checking during batch processing workflows.
 * 
 * This repository replaces VSAM DALYTRAN file access patterns from CBTRN01C and CBTRN02C batch 
 * programs with modern JPA operations supporting PostgreSQL database persistence and Spring Batch 
 * chunk-oriented processing.
 * 
 * Key Features:
 * - Chunk-based processing support with pagination for Spring Batch readers
 * - Processing status tracking for batch workflow management
 * - Date-range queries for daily file processing windows
 * - Cross-reference validation queries during import processing
 * - Merchant and transaction validation for data integrity
 * 
 * @see com.carddemo.entity.DailyTransaction
 * @author Blitzy Agent
 * @version 1.0
 */
@Repository
public interface DailyTransactionRepository extends JpaRepository<DailyTransaction, Long> {

    /**
     * Finds daily transactions by processing status for batch workflow tracking.
     * Supports Spring Batch job restart and recovery scenarios by identifying
     * transactions in various processing states (NEW, PROCESSING, COMPLETED, FAILED).
     * 
     * @param processingStatus the processing status to filter by
     * @return list of daily transactions with the specified processing status
     */
    List<DailyTransaction> findByProcessingStatus(String processingStatus);

    /**
     * Finds daily transactions by processing status with pagination support.
     * Essential for Spring Batch chunk-based processing with configurable chunk sizes
     * and page-based iteration through large daily transaction files.
     * 
     * @param processingStatus the processing status to filter by
     * @param pageable pagination information including page size and sort order
     * @return page of daily transactions with the specified processing status
     */
    Page<DailyTransaction> findByProcessingStatus(String processingStatus, Pageable pageable);

    /**
     * Finds daily transactions within a specific date range for daily file processing.
     * Supports batch processing of transactions for specific processing dates and
     * enables reprocessing of failed transaction dates.
     * 
     * @param startDate the start date of the transaction date range (inclusive)
     * @param endDate the end date of the transaction date range (inclusive)
     * @return list of daily transactions within the specified date range
     */
    List<DailyTransaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Finds daily transactions by transaction date range with pagination support.
     * Optimized for large daily transaction files with date-based partitioning
     * and chunk processing in Spring Batch workflows.
     * 
     * @param startDate the start date of the transaction date range (inclusive)
     * @param endDate the end date of the transaction date range (inclusive)
     * @param pageable pagination information for chunk-based processing
     * @return page of daily transactions within the specified date range
     */
    Page<DailyTransaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Finds daily transactions by card number for cross-reference validation.
     * Supports the card number lookup logic from CBTRN01C batch program for
     * validating card numbers against card cross-reference (XREF) data.
     * 
     * @param cardNumber the card number to search for (16-character string)
     * @return list of daily transactions for the specified card number
     */
    List<DailyTransaction> findByCardNumber(String cardNumber);

    /**
     * Finds daily transactions by merchant ID for merchant validation processing.
     * Enables merchant-specific processing and validation during batch import
     * workflows as performed in CBTRN02C merchant validation logic.
     * 
     * @param merchantId the merchant identifier to filter by
     * @return list of daily transactions for the specified merchant
     */
    List<DailyTransaction> findByMerchantId(Long merchantId);

    /**
     * Finds daily transactions by transaction type code for categorization processing.
     * Supports transaction type validation and categorization logic during batch
     * processing workflows.
     * 
     * @param transactionTypeCode the transaction type code (2-character string)
     * @return list of daily transactions with the specified transaction type
     */
    List<DailyTransaction> findByTransactionTypeCode(String transactionTypeCode);

    /**
     * Counts daily transactions by processing status for batch job monitoring.
     * Provides metrics for batch job progress tracking and completion validation.
     * 
     * @param processingStatus the processing status to count
     * @return count of daily transactions with the specified processing status
     */
    long countByProcessingStatus(String processingStatus);

    /**
     * Finds unprocessed daily transactions for batch job initialization.
     * Returns transactions that have not yet been processed by batch jobs,
     * supporting job restart scenarios and incremental processing.
     * 
     * @return list of daily transactions with NEW or PENDING processing status
     */
    @Query("SELECT dt FROM DailyTransaction dt WHERE dt.processingStatus IN ('NEW', 'PENDING') ORDER BY dt.originalTimestamp ASC")
    List<DailyTransaction> findUnprocessedTransactions();

    /**
     * Finds unprocessed daily transactions with pagination for chunk processing.
     * Optimized for Spring Batch readers that process large volumes of daily
     * transactions in configurable chunk sizes.
     * 
     * @param pageable pagination information for chunk-based processing
     * @return page of unprocessed daily transactions ordered by original timestamp
     */
    @Query("SELECT dt FROM DailyTransaction dt WHERE dt.processingStatus IN ('NEW', 'PENDING') ORDER BY dt.originalTimestamp ASC")
    Page<DailyTransaction> findUnprocessedTransactions(Pageable pageable);

    /**
     * Finds daily transactions by processing timestamp range for batch job monitoring.
     * Supports batch job performance analysis and processing window validation.
     * 
     * @param startTimestamp the start of the processing timestamp range
     * @param endTimestamp the end of the processing timestamp range
     * @return list of daily transactions processed within the timestamp range
     */
    List<DailyTransaction> findByProcessingTimestampBetween(LocalDateTime startTimestamp, LocalDateTime endTimestamp);

    /**
     * Finds daily transactions with failed validation for error processing.
     * Returns transactions that failed cross-reference validation or other
     * business rule validation during batch processing.
     * 
     * @return list of daily transactions with FAILED processing status
     */
    @Query("SELECT dt FROM DailyTransaction dt WHERE dt.processingStatus = 'FAILED' ORDER BY dt.processingTimestamp DESC")
    List<DailyTransaction> findFailedTransactions();

    /**
     * Updates processing status for a daily transaction by transaction ID.
     * Supports batch processing workflow state management and error handling.
     * 
     * @param transactionId the unique transaction identifier
     * @param processingStatus the new processing status
     * @param processingTimestamp the processing timestamp to set
     * @return number of records updated (should be 1 for successful update)
     */
    @Query("UPDATE DailyTransaction dt SET dt.processingStatus = :processingStatus, dt.processingTimestamp = :processingTimestamp WHERE dt.transactionId = :transactionId")
    int updateProcessingStatus(@Param("transactionId") String transactionId, 
                               @Param("processingStatus") String processingStatus, 
                               @Param("processingTimestamp") LocalDateTime processingTimestamp);

    /**
     * Finds daily transactions by transaction amount range for validation processing.
     * Supports amount-based validation rules and large transaction detection
     * during batch processing workflows.
     * 
     * @param minAmount the minimum transaction amount (inclusive)
     * @param maxAmount the maximum transaction amount (inclusive)
     * @return list of daily transactions within the specified amount range
     */
    @Query("SELECT dt FROM DailyTransaction dt WHERE dt.transactionAmount BETWEEN :minAmount AND :maxAmount")
    List<DailyTransaction> findByTransactionAmountBetween(@Param("minAmount") java.math.BigDecimal minAmount, 
                                                          @Param("maxAmount") java.math.BigDecimal maxAmount);

    /**
     * Checks if a daily transaction exists by transaction ID for duplicate detection.
     * Prevents duplicate processing of transactions during batch import workflows
     * and supports idempotent batch job execution.
     * 
     * @param transactionId the unique transaction identifier to check
     * @return true if a transaction with the specified ID exists, false otherwise
     */
    boolean existsByTransactionId(String transactionId);

    /**
     * Deletes daily transactions older than a specified date for data retention.
     * Supports automated cleanup of processed daily transaction records based
     * on retention policies and archival requirements.
     * 
     * @param cutoffDate the date before which transactions should be deleted
     * @return number of records deleted
     */
    @Query("DELETE FROM DailyTransaction dt WHERE dt.transactionDate < :cutoffDate AND dt.processingStatus = 'COMPLETED'")
    int deleteByTransactionDateBeforeAndProcessingStatus(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Finds the latest processing timestamp for completed transactions.
     * Supports batch job restart logic by identifying the last successfully
     * processed transaction timestamp for incremental processing.
     * 
     * @return the latest processing timestamp for completed transactions, or null if none exist
     */
    @Query("SELECT MAX(dt.processingTimestamp) FROM DailyTransaction dt WHERE dt.processingStatus = 'COMPLETED'")
    LocalDateTime findLatestCompletedProcessingTimestamp();
}