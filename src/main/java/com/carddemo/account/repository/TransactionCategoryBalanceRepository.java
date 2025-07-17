package com.carddemo.account.repository;

import com.carddemo.account.entity.TransactionCategoryBalance;
import com.carddemo.account.entity.TransactionCategoryBalance.TransactionCategoryBalanceId;
import com.carddemo.account.entity.Account;
import com.carddemo.common.entity.TransactionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * TransactionCategoryBalanceRepository provides Spring Data JPA repository interface for
 * TransactionCategoryBalance entity operations with composite primary key support.
 *
 * This repository enables granular balance management by transaction category, supporting
 * bulk update operations for batch processing and aggregation queries for financial
 * reporting and account analysis operations as specified in Section 6.2.1.2.
 *
 * Key Features:
 * - Composite primary key operations matching VSAM TRAN-CAT-KEY structure
 * - BigDecimal precision maintenance equivalent to COBOL COMP-3 arithmetic
 * - Bulk update operations for batch processing within 4-hour window requirement
 * - Materialized view integration for account balance summary operations
 * - Transaction category balance validation and constraint checking
 * - Optimized queries for financial reporting and account analysis
 *
 * Original COBOL Structure Mapping:
 * - TRAN-CAT-KEY: account_id + transaction_type + transaction_category
 * - TRAN-CAT-BAL: BigDecimal balance with exact decimal precision
 * - VSAM sequential access patterns via ORDER BY clauses
 * - CICS transaction integrity via @Transactional annotations
 *
 * Database Table: transaction_category_balances
 * Primary Key: Composite (account_id, transaction_category)
 * Foreign Keys: account_id -> accounts.account_id, transaction_category -> transaction_categories.transaction_category
 *
 * @author CardDemo Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@Repository
public interface TransactionCategoryBalanceRepository extends JpaRepository<TransactionCategoryBalance, TransactionCategoryBalanceId> {

    /**
     * Retrieves all transaction category balances for a specific account ordered by transaction category.
     * 
     * Equivalent to COBOL sequential read of TCATBAL file with account key matching.
     * Used for balance inquiry operations in account management services.
     * 
     * Original COBOL Pattern:
     * - PERFORM 1000-TCATBALF-GET-NEXT
     * - IF TRANCAT-ACCT-ID = WS-ACCOUNT-ID
     * - DISPLAY TRAN-CAT-BAL-RECORD
     * 
     * @param accountId The 11-digit account identifier
     * @return List of TransactionCategoryBalance entities ordered by transaction category
     */
    @Query("SELECT tcb FROM TransactionCategoryBalance tcb WHERE tcb.id.accountId = :accountId ORDER BY tcb.id.transactionType, tcb.id.transactionCategory")
    List<TransactionCategoryBalance> findByAccountIdOrderByTransactionCategory(@Param("accountId") String accountId);

    /**
     * Updates the category balance for a specific account, transaction type, and transaction category.
     * 
     * Performs atomic balance update during transaction posting from batch programs.
     * Maintains BigDecimal precision equivalent to COBOL COMP-3 arithmetic operations.
     * 
     * Original COBOL Pattern:
     * - MOVE NEW-BALANCE TO TRAN-CAT-BAL
     * - REWRITE TRAN-CAT-BAL-RECORD
     * - IF TCATBALF-STATUS = '00' (success)
     * 
     * @param accountId The 11-digit account identifier
     * @param transactionType The 2-character transaction type code
     * @param transactionCategory The 4-character transaction category code
     * @param newBalance The new balance amount with exact decimal precision
     * @return Number of records updated (should be 1 for successful update)
     */
    @Modifying
    @Transactional
    @Query("UPDATE TransactionCategoryBalance tcb SET tcb.categoryBalance = :newBalance, tcb.lastUpdated = CURRENT_TIMESTAMP WHERE tcb.id.accountId = :accountId AND tcb.id.transactionType = :transactionType AND tcb.id.transactionCategory = :transactionCategory")
    int updateCategoryBalance(@Param("accountId") String accountId, 
                             @Param("transactionType") String transactionType,
                             @Param("transactionCategory") String transactionCategory, 
                             @Param("newBalance") BigDecimal newBalance);

    /**
     * Retrieves the balance for a specific account, transaction type, and transaction category combination.
     * 
     * Equivalent to COBOL random read with composite key access pattern.
     * Used for specific category balance lookups in transaction processing.
     * 
     * Original COBOL Pattern:
     * - MOVE ACCOUNT-ID TO TRANCAT-ACCT-ID
     * - MOVE TYPE-CODE TO TRANCAT-TYPE-CD
     * - MOVE CATEGORY-CODE TO TRANCAT-CD
     * - READ TCATBAL-FILE KEY IS TRAN-CAT-KEY
     * 
     * @param accountId The 11-digit account identifier
     * @param transactionType The 2-character transaction type code
     * @param transactionCategory The 4-character transaction category code
     * @return Optional containing the balance amount if found, empty otherwise
     */
    @Query("SELECT tcb.categoryBalance FROM TransactionCategoryBalance tcb WHERE tcb.id.accountId = :accountId AND tcb.id.transactionType = :transactionType AND tcb.id.transactionCategory = :transactionCategory")
    Optional<BigDecimal> findBalanceByAccountIdAndCategory(@Param("accountId") String accountId, 
                                                          @Param("transactionType") String transactionType,
                                                          @Param("transactionCategory") String transactionCategory);

    /**
     * Calculates the sum of all category balances for a specific account.
     * 
     * Performs aggregation query for financial reporting and account analysis.
     * Used in balance summary operations and account overview displays.
     * 
     * Original COBOL Pattern:
     * - PERFORM VARYING WS-CATEGORY FROM 1 BY 1 UNTIL WS-CATEGORY > MAX-CATEGORIES
     * - ADD TRAN-CAT-BAL TO WS-TOTAL-BALANCE
     * - END-PERFORM
     * 
     * @param accountId The 11-digit account identifier
     * @return Sum of all category balances for the account, null if no records found
     */
    @Query("SELECT SUM(tcb.categoryBalance) FROM TransactionCategoryBalance tcb WHERE tcb.id.accountId = :accountId")
    BigDecimal sumBalancesByAccountId(@Param("accountId") String accountId);

    /**
     * Retrieves all accounts that have negative balances in any transaction category.
     * 
     * Used for financial reporting and analysis to identify accounts with negative positions.
     * Supports risk management and credit monitoring operations.
     * 
     * Original COBOL Pattern:
     * - PERFORM 1000-TCATBALF-GET-NEXT
     * - IF TRAN-CAT-BAL < 0
     * - MOVE TRANCAT-ACCT-ID TO NEGATIVE-ACCOUNT-TABLE
     * 
     * @return List of account IDs with negative category balances
     */
    @Query("SELECT DISTINCT tcb.id.accountId FROM TransactionCategoryBalance tcb WHERE tcb.categoryBalance < 0")
    List<String> findAccountsWithNegativeBalances();

    /**
     * Performs bulk update of all category balances for a specific account.
     * 
     * Supports batch processing operations including interest calculation and statement generation.
     * Optimized for bulk operations within the 4-hour processing window requirement.
     * 
     * Original COBOL Pattern:
     * - PERFORM 1300-COMPUTE-INTEREST
     * - PERFORM 1400-COMPUTE-FEES
     * - PERFORM 1050-UPDATE-ACCOUNT
     * - REWRITE TRAN-CAT-BAL-RECORD
     * 
     * @param accountId The 11-digit account identifier
     * @param adjustmentAmount The amount to add to all category balances
     * @return Number of records updated
     */
    @Modifying
    @Transactional
    @Query("UPDATE TransactionCategoryBalance tcb SET tcb.categoryBalance = tcb.categoryBalance + :adjustmentAmount, tcb.lastUpdated = CURRENT_TIMESTAMP WHERE tcb.id.accountId = :accountId")
    int updateAllBalancesForAccount(@Param("accountId") String accountId, 
                                   @Param("adjustmentAmount") BigDecimal adjustmentAmount);

    /**
     * Validates balance constraints and business rules for transaction category balances.
     * 
     * Ensures balance integrity and business rule compliance during transaction processing.
     * Performs validation checks equivalent to COBOL 88-level condition validation.
     * 
     * Original COBOL Pattern:
     * - 88 VALID-BALANCE VALUE -9999999999.99 THRU 9999999999.99
     * - IF NOT VALID-BALANCE
     * - PERFORM 9999-ABEND-PROGRAM
     * 
     * @param accountId The 11-digit account identifier
     * @return True if all balances are within valid range, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(tcb) = 0 THEN true ELSE false END FROM TransactionCategoryBalance tcb WHERE tcb.id.accountId = :accountId AND (tcb.categoryBalance < -9999999999.99 OR tcb.categoryBalance > 9999999999.99)")
    boolean validateBalanceConstraints(@Param("accountId") String accountId);

    /**
     * Retrieves transaction category balance with associated account and category details.
     * 
     * Performs JOIN operations to access related Account and TransactionCategory entities.
     * Used for comprehensive balance reporting with account and category information.
     * 
     * @param accountId The 11-digit account identifier
     * @return List of TransactionCategoryBalance entities with eager-loaded relationships
     */
    @Query("SELECT tcb FROM TransactionCategoryBalance tcb " +
           "JOIN FETCH tcb.account a " +
           "JOIN FETCH tcb.transactionCategory tc " +
           "WHERE tcb.id.accountId = :accountId " +
           "ORDER BY tcb.id.transactionType, tcb.id.transactionCategory")
    List<TransactionCategoryBalance> findByAccountIdWithDetails(@Param("accountId") String accountId);

    /**
     * Retrieves all transaction category balances for accounts with specific balance conditions.
     * 
     * Supports financial analysis and reporting operations with flexible balance filtering.
     * Used for account management and risk assessment queries.
     * 
     * @param minBalance Minimum balance threshold for filtering
     * @param maxBalance Maximum balance threshold for filtering
     * @return List of TransactionCategoryBalance entities within the specified range
     */
    @Query("SELECT tcb FROM TransactionCategoryBalance tcb WHERE tcb.categoryBalance BETWEEN :minBalance AND :maxBalance ORDER BY tcb.id.accountId, tcb.id.transactionType, tcb.id.transactionCategory")
    List<TransactionCategoryBalance> findBalancesInRange(@Param("minBalance") BigDecimal minBalance, 
                                                        @Param("maxBalance") BigDecimal maxBalance);

    /**
     * Counts the number of transaction categories with balances for a specific account.
     * 
     * Used for account analysis and balance distribution reporting.
     * Supports account portfolio management and statistics generation.
     * 
     * @param accountId The 11-digit account identifier
     * @return Number of transaction categories with balances for the account
     */
    @Query("SELECT COUNT(tcb) FROM TransactionCategoryBalance tcb WHERE tcb.id.accountId = :accountId")
    Long countCategoriesWithBalances(@Param("accountId") String accountId);

    /**
     * Retrieves transaction category balances for multiple accounts in a single query.
     * 
     * Optimized for batch processing and bulk operations across multiple accounts.
     * Supports portfolio analysis and cross-account balance reporting.
     * 
     * @param accountIds List of 11-digit account identifiers
     * @return List of TransactionCategoryBalance entities for all specified accounts
     */
    @Query("SELECT tcb FROM TransactionCategoryBalance tcb WHERE tcb.id.accountId IN :accountIds ORDER BY tcb.id.accountId, tcb.id.transactionType, tcb.id.transactionCategory")
    List<TransactionCategoryBalance> findByAccountIdList(@Param("accountIds") List<String> accountIds);

    /**
     * Retrieves the most recent balance update timestamp for an account.
     * 
     * Used for audit trail and data currency verification in balance operations.
     * Supports operational monitoring and data freshness validation.
     * 
     * @param accountId The 11-digit account identifier
     * @return The most recent last updated timestamp for the account's balances
     */
    @Query("SELECT MAX(tcb.lastUpdated) FROM TransactionCategoryBalance tcb WHERE tcb.id.accountId = :accountId")
    Optional<java.time.LocalDateTime> findLastUpdateTimestamp(@Param("accountId") String accountId);

    /**
     * Deletes all transaction category balances for a specific account.
     * 
     * Used for account closure and data cleanup operations.
     * Maintains referential integrity during account lifecycle management.
     * 
     * @param accountId The 11-digit account identifier
     * @return Number of records deleted
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM TransactionCategoryBalance tcb WHERE tcb.id.accountId = :accountId")
    int deleteByAccountId(@Param("accountId") String accountId);

    /**
     * Retrieves balance statistics for a specific transaction category across all accounts.
     * 
     * Supports category-level analysis and financial reporting operations.
     * Used for transaction category performance analysis and risk assessment.
     * 
     * @param transactionCategory The 4-character transaction category code
     * @return List of balance statistics including min, max, avg, and count
     */
    @Query("SELECT tcb.id.transactionCategory, MIN(tcb.categoryBalance), MAX(tcb.categoryBalance), AVG(tcb.categoryBalance), COUNT(tcb) FROM TransactionCategoryBalance tcb WHERE tcb.id.transactionCategory = :transactionCategory GROUP BY tcb.id.transactionCategory")
    List<Object[]> findBalanceStatisticsByCategory(@Param("transactionCategory") String transactionCategory);

    /**
     * Performs conditional balance update based on current balance value.
     * 
     * Supports atomic conditional updates in transaction processing scenarios.
     * Maintains data consistency during concurrent balance modifications.
     * 
     * @param accountId The 11-digit account identifier
     * @param transactionType The 2-character transaction type code
     * @param transactionCategory The 4-character transaction category code
     * @param expectedBalance The expected current balance value
     * @param newBalance The new balance amount to set
     * @return Number of records updated (1 if successful, 0 if condition not met)
     */
    @Modifying
    @Transactional
    @Query("UPDATE TransactionCategoryBalance tcb SET tcb.categoryBalance = :newBalance, tcb.lastUpdated = CURRENT_TIMESTAMP WHERE tcb.id.accountId = :accountId AND tcb.id.transactionType = :transactionType AND tcb.id.transactionCategory = :transactionCategory AND tcb.categoryBalance = :expectedBalance")
    int updateCategoryBalanceConditional(@Param("accountId") String accountId,
                                        @Param("transactionType") String transactionType,
                                        @Param("transactionCategory") String transactionCategory,
                                        @Param("expectedBalance") BigDecimal expectedBalance,
                                        @Param("newBalance") BigDecimal newBalance);
}