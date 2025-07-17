package com.carddemo.account.repository;

import com.carddemo.account.entity.Account;
import com.carddemo.account.entity.Customer;
import com.carddemo.common.enums.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for Account entity providing CRUD operations,
 * custom query methods for account management, balance inquiries, customer cross-references,
 * and optimistic locking support, replacing VSAM ACCTDAT dataset access with PostgreSQL
 * B-tree indexed queries optimized for sub-200ms response times.
 * 
 * This repository interface converts COBOL VSAM file operations to modern JPA queries
 * while maintaining identical business logic and data access patterns. All financial
 * calculations preserve COBOL COMP-3 decimal precision using BigDecimal with exact
 * arithmetic operations.
 * 
 * Original COBOL Programs:
 * - COACTVWC.cbl: Account view operations (9300-GETACCTDATA-BYACCT)
 * - COACTUPC.cbl: Account update operations with balance modifications
 * 
 * Original VSAM Files:
 * - ACCTDAT: Primary account master file (300-byte records)
 * - CXACAIX: Account cross-reference alternate index for customer lookups
 * 
 * Database Table: accounts
 * Primary Key: account_id (VARCHAR(11))
 * Foreign Key: customer_id references customers(customer_id)
 * 
 * Performance Requirements:
 * - Query response time: <200ms at 95th percentile
 * - Concurrent transaction support: 10,000 TPS
 * - Optimistic locking for concurrent updates
 * - B-tree index optimization for key-based lookups
 * 
 * @author CardDemo Migration Team
 * @version CardDemo_v1.0-15-g27d6c6f-68
 * @since 2022-07-19
 */
@Repository
@Transactional(isolation = Isolation.SERIALIZABLE)
public interface AccountRepository extends JpaRepository<Account, String> {

    /**
     * Find account by account ID and active status with validation.
     * 
     * Equivalent to COBOL operation:
     * EXEC CICS READ DATASET(LIT-ACCTFILENAME) RIDFLD(WS-CARD-RID-ACCT-ID-X)
     * Combined with ACCT-ACTIVE-STATUS field validation
     * 
     * Supports account lookup with status filtering per COBOL 88-level conditions:
     * 88 FLG-ACCT-STATUS-ISVALID VALUES 'Y', 'N'
     * 
     * @param accountId 11-digit account identifier
     * @param activeStatus Account active status (ACTIVE or INACTIVE)
     * @return Optional containing account if found and status matches
     */
    @Query("SELECT a FROM Account a WHERE a.accountId = :accountId AND a.activeStatus = :activeStatus")
    Optional<Account> findByAccountIdAndActiveStatus(@Param("accountId") String accountId, 
                                                   @Param("activeStatus") AccountStatus activeStatus);

    /**
     * Find accounts with balance greater than specified amount.
     * 
     * Equivalent to COBOL sequential read operations with balance filtering:
     * PERFORM VARYING loop through ACCTDAT with ACCT-CURR-BAL comparison
     * 
     * Supports account screening operations replacing VSAM sequential reads
     * for credit limit analysis and balance inquiries.
     * 
     * @param balance Minimum balance threshold (BigDecimal with COMP-3 precision)
     * @return List of accounts with balance greater than threshold
     */
    @Query("SELECT a FROM Account a WHERE a.currentBalance > :balance ORDER BY a.currentBalance DESC")
    List<Account> findAccountsWithBalanceGreaterThan(@Param("balance") BigDecimal balance);

    /**
     * Find accounts by customer ID ordered by account ID.
     * 
     * Equivalent to CXACAIX alternate index functionality:
     * EXEC CICS READ DATASET(LIT-CARDXREFNAME-ACCT-PATH) RIDFLD(WS-CARD-RID-CUST-ID-X)
     * 
     * Supports customer cross-reference operations maintaining VSAM alternate index
     * access patterns for customer-to-account relationship queries.
     * 
     * @param customerId 9-digit customer identifier
     * @return List of accounts for customer ordered by account ID
     */
    @Query("SELECT a FROM Account a WHERE a.customer.customerId = :customerId ORDER BY a.accountId")
    List<Account> findByCustomerIdOrderByAccountId(@Param("customerId") String customerId);

    /**
     * Find account by account ID and active status ordered by account ID.
     * 
     * Enhanced version of findByAccountIdAndActiveStatus with consistent ordering
     * for predictable result sets in transaction processing scenarios.
     * 
     * @param accountId 11-digit account identifier
     * @param activeStatus Account active status (ACTIVE or INACTIVE)
     * @return Optional containing account if found and status matches
     */
    @Query("SELECT a FROM Account a WHERE a.accountId = :accountId AND a.activeStatus = :activeStatus ORDER BY a.accountId")
    Optional<Account> findByAccountIdAndActiveStatusOrderByAccountId(@Param("accountId") String accountId, 
                                                                   @Param("activeStatus") AccountStatus activeStatus);

    /**
     * Update account balance with optimistic locking support.
     * 
     * Equivalent to COBOL REWRITE operation:
     * EXEC CICS REWRITE DATASET(LIT-ACCTFILENAME) FROM(ACCOUNT-RECORD)
     * 
     * Implements optimistic locking required by AccountUpdateService.java
     * for concurrent account balance updates with version control.
     * 
     * @param accountId 11-digit account identifier
     * @param newBalance New account balance (BigDecimal with COMP-3 precision)
     * @param version Current version for optimistic locking
     * @return Number of affected records (1 if successful, 0 if version conflict)
     */
    @Modifying
    @Query("UPDATE Account a SET a.currentBalance = :newBalance, a.version = a.version + 1 " +
           "WHERE a.accountId = :accountId AND a.version = :version")
    int updateAccountBalance(@Param("accountId") String accountId, 
                           @Param("newBalance") BigDecimal newBalance,
                           @Param("version") Long version);

    /**
     * Find accounts within specified balance range with pagination.
     * 
     * Supports paginated account listing operations equivalent to COBOL 
     * PERFORM VARYING loops with balance range filtering and record counting.
     * 
     * @param minBalance Minimum balance threshold (inclusive)
     * @param maxBalance Maximum balance threshold (inclusive)
     * @param pageable Pagination parameters for result set navigation
     * @return Page of accounts within balance range
     */
    @Query("SELECT a FROM Account a WHERE a.currentBalance >= :minBalance AND a.currentBalance <= :maxBalance " +
           "ORDER BY a.currentBalance DESC, a.accountId")
    Page<Account> findAccountsByBalanceRange(@Param("minBalance") BigDecimal minBalance, 
                                           @Param("maxBalance") BigDecimal maxBalance,
                                           Pageable pageable);

    /**
     * Find active accounts for specific customer with pagination.
     * 
     * Combines customer cross-reference functionality with active status filtering
     * for comprehensive customer account management operations.
     * 
     * @param customerId 9-digit customer identifier
     * @param pageable Pagination parameters for result set navigation
     * @return Page of active accounts for customer
     */
    @Query("SELECT a FROM Account a WHERE a.customer.customerId = :customerId AND a.activeStatus = 'ACTIVE' " +
           "ORDER BY a.accountId")
    Page<Account> findActiveAccountsForCustomer(@Param("customerId") String customerId, Pageable pageable);

    /**
     * Count accounts by status for reporting operations.
     * 
     * Equivalent to COBOL record counting operations:
     * PERFORM VARYING with counter increment for status-based statistics
     * 
     * @param activeStatus Account active status (ACTIVE or INACTIVE)
     * @return Count of accounts with specified status
     */
    @Query("SELECT COUNT(a) FROM Account a WHERE a.activeStatus = :activeStatus")
    long countAccountsByStatus(@Param("activeStatus") AccountStatus activeStatus);

    /**
     * Find accounts with credit limit greater than specified amount.
     * 
     * Supports credit limit analysis operations for risk management
     * and account screening processes.
     * 
     * @param creditLimit Minimum credit limit threshold
     * @return List of accounts with credit limit greater than threshold
     */
    @Query("SELECT a FROM Account a WHERE a.creditLimit > :creditLimit ORDER BY a.creditLimit DESC")
    List<Account> findAccountsWithCreditLimitGreaterThan(@Param("creditLimit") BigDecimal creditLimit);

    /**
     * Find accounts by customer with balance range filtering.
     * 
     * Combined customer cross-reference and balance filtering for comprehensive
     * customer account analysis operations.
     * 
     * @param customerId 9-digit customer identifier
     * @param minBalance Minimum balance threshold (inclusive)
     * @param maxBalance Maximum balance threshold (inclusive)
     * @return List of customer accounts within balance range
     */
    @Query("SELECT a FROM Account a WHERE a.customer.customerId = :customerId " +
           "AND a.currentBalance >= :minBalance AND a.currentBalance <= :maxBalance " +
           "ORDER BY a.currentBalance DESC")
    List<Account> findCustomerAccountsByBalanceRange(@Param("customerId") String customerId,
                                                   @Param("minBalance") BigDecimal minBalance,
                                                   @Param("maxBalance") BigDecimal maxBalance);

    /**
     * Find accounts over credit limit for risk management.
     * 
     * Supports risk monitoring operations to identify accounts
     * exceeding their credit limits for intervention.
     * 
     * @return List of accounts where current balance exceeds credit limit
     */
    @Query("SELECT a FROM Account a WHERE a.currentBalance > a.creditLimit ORDER BY a.currentBalance DESC")
    List<Account> findAccountsOverCreditLimit();

    /**
     * Update account cycle amounts with optimistic locking.
     * 
     * Equivalent to COBOL cycle amount updates:
     * MOVE new-amount TO ACCT-CURR-CYC-CREDIT/ACCT-CURR-CYC-DEBIT
     * EXEC CICS REWRITE DATASET(LIT-ACCTFILENAME) FROM(ACCOUNT-RECORD)
     * 
     * @param accountId 11-digit account identifier
     * @param cycleCredit New current cycle credit amount
     * @param cycleDebit New current cycle debit amount
     * @param version Current version for optimistic locking
     * @return Number of affected records (1 if successful, 0 if version conflict)
     */
    @Modifying
    @Query("UPDATE Account a SET a.currentCycleCredit = :cycleCredit, a.currentCycleDebit = :cycleDebit, " +
           "a.version = a.version + 1 WHERE a.accountId = :accountId AND a.version = :version")
    int updateAccountCycleAmounts(@Param("accountId") String accountId,
                                @Param("cycleCredit") BigDecimal cycleCredit,
                                @Param("cycleDebit") BigDecimal cycleDebit,
                                @Param("version") Long version);

    /**
     * Find accounts by group ID for disclosure and interest rate management.
     * 
     * Supports group-based account operations for interest rate configuration
     * and disclosure management equivalent to COBOL group processing.
     * 
     * @param groupId Group identifier for disclosure management
     * @return List of accounts in specified group
     */
    @Query("SELECT a FROM Account a WHERE a.groupId = :groupId ORDER BY a.accountId")
    List<Account> findAccountsByGroupId(@Param("groupId") String groupId);

    /**
     * Find accounts with expiration dates within specified range.
     * 
     * Supports account expiration monitoring and renewal processing
     * operations for account lifecycle management.
     * 
     * @param startDate Start date for expiration range (inclusive)
     * @param endDate End date for expiration range (inclusive)
     * @return List of accounts with expiration dates in range
     */
    @Query("SELECT a FROM Account a WHERE a.expirationDate >= :startDate AND a.expirationDate <= :endDate " +
           "ORDER BY a.expirationDate")
    List<Account> findAccountsByExpirationDateRange(@Param("startDate") java.time.LocalDate startDate,
                                                   @Param("endDate") java.time.LocalDate endDate);

    /**
     * Update account status with optimistic locking.
     * 
     * Equivalent to COBOL status update operations:
     * MOVE new-status TO ACCT-ACTIVE-STATUS
     * EXEC CICS REWRITE DATASET(LIT-ACCTFILENAME) FROM(ACCOUNT-RECORD)
     * 
     * @param accountId 11-digit account identifier
     * @param activeStatus New account active status
     * @param version Current version for optimistic locking
     * @return Number of affected records (1 if successful, 0 if version conflict)
     */
    @Modifying
    @Query("UPDATE Account a SET a.activeStatus = :activeStatus, a.version = a.version + 1 " +
           "WHERE a.accountId = :accountId AND a.version = :version")
    int updateAccountStatus(@Param("accountId") String accountId,
                          @Param("activeStatus") AccountStatus activeStatus,
                          @Param("version") Long version);

    /**
     * Find accounts by ZIP code for geographical analysis.
     * 
     * Supports location-based account analysis operations
     * for geographic reporting and risk assessment.
     * 
     * @param zipCode ZIP code for geographical filtering
     * @return List of accounts in specified ZIP code
     */
    @Query("SELECT a FROM Account a WHERE a.addressZip = :zipCode ORDER BY a.accountId")
    List<Account> findAccountsByZipCode(@Param("zipCode") String zipCode);

    /**
     * Get account summary statistics by status.
     * 
     * Provides aggregate data for account status reporting
     * equivalent to COBOL statistical summary operations.
     * 
     * @param activeStatus Account active status filter
     * @return Object array containing [count, total_balance, avg_balance, max_balance, min_balance]
     */
    @Query("SELECT COUNT(a), SUM(a.currentBalance), AVG(a.currentBalance), MAX(a.currentBalance), MIN(a.currentBalance) " +
           "FROM Account a WHERE a.activeStatus = :activeStatus")
    Object[] getAccountStatsByStatus(@Param("activeStatus") AccountStatus activeStatus);

    /**
     * Find accounts with recent activity (based on cycle amounts).
     * 
     * Supports account activity monitoring by identifying accounts
     * with non-zero cycle amounts indicating recent transactions.
     * 
     * @return List of accounts with recent transaction activity
     */
    @Query("SELECT a FROM Account a WHERE a.currentCycleCredit > 0 OR a.currentCycleDebit > 0 " +
           "ORDER BY (a.currentCycleCredit + a.currentCycleDebit) DESC")
    List<Account> findAccountsWithRecentActivity();

    /**
     * Find accounts eligible for credit limit increase.
     * 
     * Supports credit limit management by identifying accounts
     * with positive balances and good utilization patterns.
     * 
     * @param utilizationThreshold Maximum utilization percentage (0.0 to 1.0)
     * @return List of accounts eligible for credit limit increase
     */
    @Query("SELECT a FROM Account a WHERE a.currentBalance > 0 AND " +
           "(a.currentBalance / a.creditLimit) < :utilizationThreshold AND a.activeStatus = 'ACTIVE' " +
           "ORDER BY a.currentBalance DESC")
    List<Account> findAccountsEligibleForCreditIncrease(@Param("utilizationThreshold") BigDecimal utilizationThreshold);
}