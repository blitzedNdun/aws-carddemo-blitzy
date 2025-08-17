/*
 * AccountRepository.java
 * 
 * Spring Data JPA repository interface for Account entity providing
 * data access for account_data table. Manages account lookups, balance
 * updates, and fee assessment queries replacing VSAM ACCTDAT file patterns.
 * 
 * Provides comprehensive account operations for customer account management,
 * balance inquiries, credit limit validation, and fee assessment processing.
 * Extends JpaRepository with Account entity type and String primary key type.
 * 
 * This repository replaces VSAM ACCTDAT file I/O operations (READ, WRITE, 
 * REWRITE, DELETE) with modern JPA method implementations for account_data 
 * table access, supporting account management processes and fee assessment 
 * batch operations.
 * 
 * Based on COBOL copybook: app/cpy/CVACT01Y.cpy
 * - ACCT-ID (PIC 9(11)) for account identification and primary key access
 * - ACCT-ACTIVE-STATUS (PIC X(01)) for account status filtering
 * - ACCT-CURR-BAL (PIC S9(10)V99) for balance queries and updates
 * - ACCT-CREDIT-LIMIT (PIC S9(10)V99) for credit limit validation
 * - ACCT-GROUP-ID (PIC X(10)) for fee assessment grouping
 */

package com.carddemo.repository;

import com.carddemo.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for Account entity.
 * 
 * Provides comprehensive data access operations for account_data table,
 * supporting account management, balance tracking, credit limit validation,
 * and fee assessment processing. Replaces VSAM ACCTDAT file patterns with 
 * modern JPA repository methods that maintain identical functionality while 
 * leveraging PostgreSQL relational database capabilities.
 * 
 * Key functionality:
 * - Account ID-based lookups for account management operations
 * - Customer ID filtering for customer account relationships
 * - Active status filtering for operational account queries
 * - Balance range queries for reporting and analysis
 * - Credit utilization calculations for risk management
 * - Fee assessment eligibility determination for batch processing
 * 
 * All methods support the sub-200ms REST API response time requirement
 * through optimized PostgreSQL B-tree index utilization and query planning.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    /**
     * Finds account by account ID (primary key lookup).
     * 
     * Supports direct account lookup operations equivalent to VSAM READ by
     * primary key. Essential for account validation, balance inquiries,
     * and transaction processing workflows.
     * 
     * @param accountId the account ID (11-digit string)
     * @return Optional Account if found
     */
    Optional<Account> findByAccountId(String accountId);

    /**
     * Finds all accounts for a specific customer.
     * 
     * Supports customer-based account lookup operations for customer service
     * and account management functions. Enables retrieval of all accounts
     * associated with a customer for comprehensive account viewing.
     * 
     * @param customerId the customer ID (9-digit string)
     * @return list of accounts for the specified customer
     */
    List<Account> findByCustomerId(String customerId);

    /**
     * Finds accounts by active status.
     * 
     * Supports status-based filtering for operational queries and reporting.
     * Enables separation of active accounts for transaction processing and
     * inactive accounts for archival and historical reporting.
     * 
     * @param activeStatus the active status ('Y' for active, 'N' for inactive)
     * @return list of accounts with the specified status
     */
    List<Account> findByActiveStatus(String activeStatus);

    /**
     * Finds active accounts for a specific customer.
     * 
     * Combines customer and status filtering for precise account retrieval.
     * Essential for customer service operations requiring only active
     * account information for transaction processing and balance inquiries.
     * 
     * @param customerId the customer ID
     * @param activeStatus the active status
     * @return list of active accounts for the specified customer
     */
    List<Account> findByCustomerIdAndActiveStatus(String customerId, String activeStatus);

    /**
     * Finds accounts by account group ID.
     * 
     * Supports group-based account lookup operations for fee assessment,
     * interest calculation, and disclosure group management. Essential for
     * batch processing operations that apply fees or interest rates based
     * on account group classifications.
     * 
     * @param accountGroupId the account group ID (10-character string)
     * @return list of accounts in the specified group
     */
    List<Account> findByAccountGroupId(String accountGroupId);

    /**
     * Finds accounts with current balance greater than specified amount.
     * 
     * Supports balance-based filtering for reporting and analysis operations.
     * Enables identification of high-balance accounts for special handling,
     * credit line management, and risk assessment processes.
     * 
     * @param balance the minimum balance threshold (exclusive)
     * @return list of accounts with balance above the threshold
     */
    List<Account> findByCurrentBalanceGreaterThan(BigDecimal balance);

    /**
     * Finds accounts with current balance less than specified amount.
     * 
     * Supports low-balance account identification for customer notifications,
     * fee assessment eligibility, and account closure processing. Essential
     * for negative balance monitoring and collection workflows.
     * 
     * @param balance the maximum balance threshold (exclusive)
     * @return list of accounts with balance below the threshold
     */
    List<Account> findByCurrentBalanceLessThan(BigDecimal balance);

    /**
     * Finds accounts with current balance between two amounts.
     * 
     * Supports range-based balance queries for targeted customer communications,
     * promotional offers, and risk assessment categorization. Enables precise
     * balance segmentation for marketing and account management strategies.
     * 
     * @param minBalance the minimum balance (inclusive)
     * @param maxBalance the maximum balance (inclusive)
     * @return list of accounts with balance in the specified range
     */
    List<Account> findByCurrentBalanceBetween(BigDecimal minBalance, BigDecimal maxBalance);

    /**
     * Finds accounts requiring fee assessment based on assessment criteria.
     * 
     * Supports fee assessment batch processing by identifying accounts eligible
     * for specific fee types based on assessment date, balance criteria, and
     * account group configurations. Essential for automated fee processing
     * workflows and regulatory compliance.
     * 
     * Custom JPQL query enables complex fee assessment logic including:
     * - Active account status validation
     * - Account group-based fee eligibility
     * - Balance threshold validation for fee applicability
     * 
     * @param feeType the type of fee to assess (used for group filtering)
     * @return list of accounts requiring fee assessment
     */
    @Query("SELECT a FROM Account a WHERE a.activeStatus = 'Y' " +
           "AND a.accountGroupId = :feeType " +
           "AND a.currentBalance > 0.00 " +
           "ORDER BY a.accountId")
    List<Account> findAccountsRequiringFeeAssessment(@Param("feeType") String feeType);

    /**
     * Finds accounts by ZIP code for geographic analysis.
     * 
     * Supports geographic segmentation for regional reporting, marketing
     * campaigns, and risk assessment based on geographic concentration.
     * Enables ZIP code-based account grouping for operational analysis.
     * 
     * @param addressZip the ZIP code (5-digit string)
     * @return list of accounts in the specified ZIP code
     */
    List<Account> findByAddressZip(String addressZip);

    /**
     * Finds accounts with expiration date before specified date.
     * 
     * Supports account expiration monitoring for card reissue processing,
     * customer notifications, and account maintenance workflows. Essential
     * for proactive account lifecycle management and customer service.
     * 
     * @param expirationDate the expiration date threshold
     * @return list of accounts expiring before the specified date
     */
    List<Account> findByExpirationDateBefore(LocalDate expirationDate);

    /**
     * Finds accounts with credit utilization above threshold using custom query.
     * 
     * Calculates credit utilization percentage and filters accounts above
     * specified threshold for risk management, credit line adjustment, and
     * customer outreach programs. Supports proactive account management
     * and risk mitigation strategies.
     * 
     * @param utilizationThreshold the credit utilization threshold (as percentage)
     * @return list of accounts with high credit utilization
     */
    @Query("SELECT a FROM Account a WHERE a.activeStatus = 'Y' " +
           "AND a.creditLimit > 0 " +
           "AND (a.currentBalance / a.creditLimit * 100) > :utilizationThreshold " +
           "ORDER BY (a.currentBalance / a.creditLimit) DESC")
    List<Account> findAccountsWithHighCreditUtilization(
            @Param("utilizationThreshold") BigDecimal utilizationThreshold);

    /**
     * Finds accounts eligible for credit line increase using custom query.
     * 
     * Identifies accounts meeting criteria for automatic credit line increases
     * based on payment history, balance utilization, and account age. Supports
     * customer retention programs and revenue enhancement initiatives.
     * 
     * @param minOpenDays minimum number of days account has been open
     * @param maxUtilization maximum credit utilization percentage
     * @return list of accounts eligible for credit line increase
     */
    @Query("SELECT a FROM Account a WHERE a.activeStatus = 'Y' " +
           "AND a.openDate <= :cutoffDate " +
           "AND a.creditLimit > 0 " +
           "AND (a.currentBalance / a.creditLimit * 100) < :maxUtilization " +
           "AND a.currentCycleDebit > 0 " +
           "ORDER BY a.openDate")
    List<Account> findAccountsEligibleForCreditIncrease(
            @Param("cutoffDate") LocalDate cutoffDate,
            @Param("maxUtilization") BigDecimal maxUtilization);

    /**
     * Inherited JpaRepository methods provide standard CRUD operations:
     * 
     * save(Account entity) - Saves or updates account
     * saveAll(Iterable<Account> entities) - Batch save operations
     * findById(String id) - Retrieves account by primary key
     * findAll() - Retrieves all accounts
     * delete(Account entity) - Deletes account
     * deleteById(String id) - Deletes account by primary key
     * deleteAll() - Deletes all accounts
     * count() - Returns total count of accounts
     * existsById(String id) - Checks existence by primary key
     * flush() - Synchronizes persistence context with database
     * 
     * All inherited methods maintain ACID compliance through Spring @Transactional
     * annotations and PostgreSQL transaction management, ensuring data consistency
     * equivalent to VSAM KSDS file operations while providing enhanced relational
     * database capabilities and performance optimization through B-tree indexing.
     */
}