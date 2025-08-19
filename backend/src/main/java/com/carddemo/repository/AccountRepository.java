/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.repository;

import com.carddemo.entity.Account;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for Account entity providing data access operations for account_data table.
 * Implements repository pattern to replace VSAM ACCTDAT file access with modern JPA operations.
 * 
 * This repository replaces COBOL VSAM file operations from COACTVWC.cbl program:
 * - EXEC CICS READ DATASET(LIT-ACCTFILENAME) → findById() method
 * - VSAM STARTBR/READNEXT operations → findAll(Pageable) for cursor-based pagination
 * - Alternate index access by customer ID → findByCustomerId() method
 * 
 * Key VSAM-to-JPA mappings:
 * - VSAM KSDS primary key (ACCT-ID) → JPA @Id Long accountId
 * - VSAM alternate index (customer ID) → JPA custom query methods
 * - VSAM browse operations → JPA Pageable interface for pagination
 * - VSAM READ/WRITE/REWRITE/DELETE → JPA save(), delete(), findById() operations
 * 
 * Transaction boundaries are managed through Spring's @Transactional support,
 * replicating CICS SYNCPOINT behavior for data consistency.
 * 
 * Provides CRUD operations, custom query methods for account retrieval by customer ID,
 * and cursor-based pagination support replicating VSAM browse operations.
 * Extends JpaRepository with Account entity type and Long primary key type.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Finds all accounts associated with a specific customer ID.
     * 
     * Replaces VSAM alternate index access pattern from COBOL program COACTVWC.cbl
     * where customer ID is retrieved through CARDXREF file and used to locate accounts.
     * 
     * This method supports customers with multiple credit card accounts by returning
     * a List<Account> collection. Maps to the COBOL access pattern:
     * - Read CARDXREF by account ID to get customer ID (line 727-735 in COACTVWC.cbl)
     * - Use customer ID to locate related accounts
     * 
     * Uses custom JPQL query to access the customer_id foreign key relationship.
     * 
     * @param customerId the customer ID to search for
     * @return List of Account entities associated with the customer, empty list if none found
     */
    @Query("SELECT a FROM Account a WHERE a.customer.customerId = :customerId")
    List<Account> findByCustomerId(@Param("customerId") Long customerId);

    /**
     * Finds accounts by multiple account IDs in a single query.
     * 
     * Supports batch operations where multiple account IDs need to be retrieved
     * efficiently. Used for bulk processing operations and cross-reference lookups.
     * 
     * @param accountIds collection of account IDs to retrieve
     * @return List of Account entities matching the provided IDs
     */
    List<Account> findByAccountIdIn(List<Long> accountIds);

    /**
     * Finds all accounts belonging to a specific group ID.
     * 
     * Groups accounts for processing, reporting, and configuration purposes.
     * Maps to ACCT-GROUP-ID field from COBOL copybook (PIC X(10)).
     * Used for batch operations and group-based account management.
     * 
     * @param groupId the account group ID to search for
     * @return List of Account entities in the specified group
     */
    List<Account> findByGroupId(String groupId);

    /**
     * Finds account by associated card number.
     * 
     * Provides account lookup via card number relationship, supporting
     * card-to-account mapping similar to COBOL CARDXREF file access patterns.
     * 
     * This method signature is defined per the export schema requirements.
     * Implementation requires card-to-account relationship entities (Card, CardXref)
     * that replicate the COBOL CARDXREF file structure from COACTVWC.cbl.
     * 
     * The actual query implementation will be provided once the complete
     * entity relationship model is available.
     * 
     * @param cardNumber the card number to search for
     * @return Account entity associated with the card number, null if not found
     */
    // Implementation note: This method requires Card and CardXref entities
    // to be implemented to provide the proper join relationships
    // TODO: This method will be properly implemented once Card and CardXref entities are created
    // For now, using a placeholder query to prevent Spring Data JPA mapping errors
    @Query("SELECT null FROM Account a WHERE 1=0")
    Account findByCardNumber(@Param("cardNumber") String cardNumber);

    /**
     * Finds accounts by active status and accounts opened before a specific date.
     * 
     * Supports account lifecycle management and reporting operations.
     * Used for identifying accounts that meet specific criteria for
     * renewal, closure, or special processing.
     * 
     * @param activeStatus the active status ('Y' or 'N')
     * @param openDate the cutoff date for account opening
     * @return List of Account entities matching the criteria
     */
    List<Account> findByActiveStatusAndOpenDateBefore(String activeStatus, LocalDate openDate);

    /**
     * Finds account by ID with pessimistic write lock for update operations.
     * 
     * Replicates COBOL "READ FOR UPDATE" functionality from COACTUPC.cbl program
     * by acquiring a database row lock to prevent concurrent modifications.
     * This method implements the same locking behavior as EXEC CICS READ UPDATE
     * commands to ensure data consistency during account update operations.
     * 
     * Uses JPA @Lock annotation with LockModeType.PESSIMISTIC_WRITE to acquire
     * an exclusive lock on the account row, preventing other transactions from
     * reading or writing the same account until the current transaction commits.
     * 
     * This method is essential for:
     * - Account balance updates that must be atomic
     * - Credit limit modifications requiring consistency
     * - Status changes that need transaction isolation
     * - Any update operation requiring read-then-write atomicity
     * 
     * Example usage:
     * Optional<Account> account = accountRepository.findByIdForUpdate(accountId);
     * if (account.isPresent()) {
     *     account.get().setCurrentBalance(newBalance);
     *     accountRepository.save(account.get());
     * }
     * 
     * @param accountId the account ID to find and lock
     * @return Optional<Account> containing the locked account if found, empty otherwise
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountId = :accountId")
    Optional<Account> findByIdForUpdate(@Param("accountId") Long accountId);

    /**
     * Finds accounts with account numbers within a specified range.
     * 
     * Supports range-based queries for account processing and reporting.
     * Replicates COBOL STARTBR/READNEXT patterns for sequential processing
     * of accounts within specific ID ranges.
     * 
     * Note: "AccountNumber" in this context refers to the accountId field
     * in the Account entity, which corresponds to ACCT-ID from COBOL copybook (PIC 9(11)).
     * This method name follows the export schema specification.
     * 
     * @param startAccountNumber the starting account number (inclusive)
     * @param endAccountNumber the ending account number (inclusive)  
     * @return List of Account entities within the specified range
     */
    @Query("SELECT a FROM Account a WHERE a.accountId BETWEEN :startAccountNumber AND :endAccountNumber")
    List<Account> findByAccountNumberBetween(@Param("startAccountNumber") Long startAccountNumber, 
                                           @Param("endAccountNumber") Long endAccountNumber);

    /**
     * Finds all accounts with pagination support.
     * 
     * Replicates VSAM STARTBR/READNEXT/READPREV browse operations with consistent ordering.
     * Provides cursor-based pagination that maintains VSAM-equivalent sequential access patterns.
     * 
     * The Pageable parameter defines:
     * - Page size (equivalent to VSAM browse buffer size)
     * - Page number (for sequential navigation)
     * - Sorting criteria (typically by account ID for consistent ordering)
     * 
     * Example usage for VSAM-equivalent browsing:
     * Pageable pageable = PageRequest.of(0, 100, Sort.by("accountId"));
     * Page<Account> accounts = findAll(pageable);
     * 
     * This method is inherited from JpaRepository but documented here for
     * clarity regarding VSAM browse operation replacement.
     * 
     * @param pageable pagination and sorting information
     * @return Page<Account> containing the requested page of accounts
     */
    // Inherited from JpaRepository: Page<Account> findAll(Pageable pageable);

    /**
     * Checks if an account exists by account ID.
     * 
     * Provides efficient existence checking without loading full entity data.
     * Replicates COBOL READ operations that only verify record existence.
     * 
     * This method is inherited from JpaRepository but documented here for
     * clarity regarding VSAM record existence checking.
     * 
     * @param accountId the account ID to check
     * @return true if account exists, false otherwise
     */
    // Inherited from JpaRepository: boolean existsById(Long accountId);

    /**
     * Counts the total number of accounts in the repository.
     * 
     * Provides record count functionality similar to COBOL file record counting.
     * Used for reporting and batch processing size estimation.
     * 
     * This method is inherited from JpaRepository but documented here for
     * clarity regarding VSAM record counting operations.
     * 
     * @return total count of Account entities
     */
    // Inherited from JpaRepository: long count();

    /**
     * Deletes all accounts from the repository.
     * 
     * Provides bulk delete functionality for data reset operations.
     * Should be used with extreme caution in production environments.
     * 
     * This method is inherited from JpaRepository but documented here for
     * clarity regarding bulk delete operations.
     */
    // Inherited from JpaRepository: void deleteAll();

    // Additional inherited CRUD methods from JpaRepository<Account, Long>:
    // - Optional<Account> findById(Long id)
    // - List<Account> findAll()
    // - <S extends Account> S save(S entity)
    // - <S extends Account> List<S> saveAll(Iterable<S> entities)
    // - void delete(Account entity)
    // - void deleteById(Long id)
    // - void deleteAll(Iterable<? extends Account> entities)
}