package com.carddemo.repository;

import com.carddemo.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for Settlement entity providing comprehensive
 * data access operations for settlement transaction processing and reconciliation.
 * 
 * This repository supports the modernized settlement processing system that replaces
 * the legacy COBOL batch program CBTRN02C for transaction posting, reconciliation,
 * and balance management operations. All query methods are optimized for PostgreSQL
 * performance with appropriate indexing strategies.
 * 
 * Key functionalities:
 * - Settlement-to-authorization matching for transaction reconciliation
 * - Date range queries supporting daily batch reconciliation processing
 * - Merchant-specific settlement reporting and analysis
 * - Status-based settlement tracking and workflow management
 * - Unmatched settlement identification for exception processing
 * 
 * @author Blitzy agent
 * @version 1.0
 * @since Spring Boot 3.2.x migration from COBOL batch processing
 */
@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    /**
     * Saves a settlement entity to the database.
     * Inherited from JpaRepository, provides comprehensive settlement persistence
     * with automatic ID generation and optimistic locking support.
     * 
     * @param settlement the Settlement entity to save
     * @return the saved Settlement entity with generated ID
     */
    @Override
    <S extends Settlement> S save(S settlement);

    /**
     * Retrieves a settlement by its unique identifier.
     * Inherited from JpaRepository, provides efficient primary key-based lookup
     * utilizing PostgreSQL B-tree index optimization.
     * 
     * @param id the settlement ID (primary key)
     * @return Optional containing the Settlement if found, empty otherwise
     */
    @Override
    Optional<Settlement> findById(Long id);

    /**
     * Finds settlements associated with a specific transaction ID.
     * Supports settlement-to-transaction matching operations essential for
     * the TransactionReconBatchService reconciliation processing.
     * 
     * This method replaces the COBOL logic that validates transactions against
     * the TRANSACT file and enables automated matching of settlements to
     * original authorization transactions.
     * 
     * @param transactionId the transaction ID to search for
     * @return List of settlements associated with the transaction
     */
    List<Settlement> findByTransactionId(Long transactionId);

    /**
     * Finds settlements by authorization ID for payment authorization matching.
     * Critical for matching settlement records to original payment authorizations,
     * supporting the reconciliation process that ensures all authorized
     * transactions are properly settled.
     * 
     * This functionality replaces the COBOL cross-reference file (XREF-FILE)
     * validation logic from the legacy batch processing system.
     * 
     * @param authorizationId the authorization ID to match against
     * @return List of settlements for the specified authorization
     */
    List<Settlement> findByAuthorizationId(String authorizationId);

    /**
     * Retrieves settlements within a specific date range for reconciliation processing.
     * Essential for daily batch reconciliation operations, supporting the 4-hour
     * processing window requirement by enabling efficient date-based settlement queries.
     * 
     * Leverages PostgreSQL range partitioning and date indexing for optimal
     * performance during high-volume batch processing operations.
     * 
     * @param startDate the beginning of the settlement date range (inclusive)
     * @param endDate the end of the settlement date range (inclusive)
     * @return List of settlements within the specified date range
     */
    List<Settlement> findBySettlementDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Finds settlements for a specific merchant within a date range.
     * Supports merchant-specific settlement reporting and reconciliation analysis,
     * enabling targeted settlement processing and merchant account management.
     * 
     * This method enables the generation of merchant settlement reports that
     * replace manual mainframe reporting processes with automated Spring Boot
     * batch job capabilities.
     * 
     * @param merchantId the merchant identifier
     * @param startDate the beginning of the settlement date range (inclusive)
     * @param endDate the end of the settlement date range (inclusive)
     * @return List of settlements for the merchant within the date range
     */
    List<Settlement> findByMerchantIdAndSettlementDateBetween(
            String merchantId, 
            LocalDate startDate, 
            LocalDate endDate
    );

    /**
     * Retrieves settlements by their processing status.
     * Supports settlement workflow management and exception processing by
     * enabling status-based settlement queries for operational oversight
     * and automated processing workflows.
     * 
     * Status values typically include: PENDING, PROCESSED, REJECTED, MATCHED, UNMATCHED
     * 
     * @param settlementStatus the settlement processing status
     * @return List of settlements with the specified status
     */
    List<Settlement> findBySettlementStatus(String settlementStatus);

    /**
     * Identifies unmatched settlements requiring manual intervention or reprocessing.
     * Complex query that finds settlements without corresponding authorization records,
     * supporting exception processing and settlement reconciliation quality assurance.
     * 
     * This method replaces the COBOL logic that writes rejected transactions to
     * the DALYREJS file, providing real-time identification of settlement
     * discrepancies for operational teams.
     * 
     * Utilizes a custom JPQL query to find settlements that lack authorization
     * matching, supporting automated exception processing workflows.
     * 
     * @return List of settlements that do not have corresponding authorization matches
     */
    @Query("SELECT s FROM Settlement s WHERE s.authorizationId IS NULL " +
           "OR s.settlementStatus = 'UNMATCHED'")
    List<Settlement> findUnmatchedSettlements();

    /**
     * Finds settlements by amount range for financial analysis and reconciliation.
     * Supports amount-based settlement analysis and exception processing for
     * large-value transactions requiring additional oversight.
     * 
     * @param minAmount minimum settlement amount (inclusive)
     * @param maxAmount maximum settlement amount (inclusive)
     * @return List of settlements within the specified amount range
     */
    List<Settlement> findByAmountBetween(BigDecimal minAmount, BigDecimal maxAmount);

    /**
     * Retrieves settlements for a specific merchant and status combination.
     * Supports targeted merchant settlement processing and status-based
     * workflow management for operational efficiency.
     * 
     * @param merchantId the merchant identifier
     * @param settlementStatus the settlement processing status
     * @return List of settlements for the merchant with the specified status
     */
    List<Settlement> findByMerchantIdAndSettlementStatus(String merchantId, String settlementStatus);

    /**
     * Counts the number of settlements by status for operational metrics.
     * Provides real-time settlement processing statistics for dashboard
     * reporting and operational monitoring.
     * 
     * @param settlementStatus the settlement status to count
     * @return number of settlements with the specified status
     */
    Long countBySettlementStatus(String settlementStatus);

    /**
     * Calculates total settlement amount by merchant and date range.
     * Supports financial reporting and merchant account reconciliation
     * with PostgreSQL aggregate function optimization.
     * 
     * @param merchantId the merchant identifier
     * @param startDate the beginning of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return total settlement amount for the merchant and date range
     */
    @Query("SELECT COALESCE(SUM(s.amount), 0) FROM Settlement s " +
           "WHERE s.merchantId = :merchantId " +
           "AND s.settlementDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByMerchantIdAndSettlementDateBetween(
            @Param("merchantId") String merchantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}