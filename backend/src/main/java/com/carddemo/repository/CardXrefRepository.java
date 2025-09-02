/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.repository;

import com.carddemo.entity.CardXref;
import com.carddemo.entity.CardXrefId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for CardXref entity providing cross-reference data access operations
 * for card_xref junction table. Supports card-to-customer-to-account relationship queries essential for 
 * CBTRN01C cross-reference validation logic during batch transaction processing.
 * 
 * This repository enables complex multi-entity lookups and relationship validation, replicating the
 * functionality of the COBOL program CBTRN01C which performs cross-reference lookups to validate
 * card numbers and retrieve associated customer and account information.
 * 
 * Key COBOL Operations Replicated:
 * - 2000-LOOKUP-XREF paragraph from CBTRN01C.cbl
 * - Card number validation and cross-reference retrieval
 * - Support for finding customer and account relationships for cards
 * 
 * The repository methods support the following business logic patterns:
 * 1. Card number lookup to find associated customer and account (primary use case)
 * 2. Customer-based queries to find all associated cards and accounts
 * 3. Account-based queries to find all associated cards and customers
 * 4. Composite queries for validating specific card-account relationships
 * 5. Existence checks for relationship validation without full entity retrieval
 * 
 * Database Operations:
 * - Primary key operations using CardXrefId composite key
 * - Index-optimized queries on individual ID components
 * - Complex relationship queries across multiple entities
 * - Batch processing support for high-volume operations
 * 
 * Performance Considerations:
 * - Uses database indexes on xref_card_num for fast card lookups
 * - Supports batch operations for processing multiple cross-references
 * - Lazy loading relationships to optimize memory usage
 * - Query optimization for high-frequency lookup operations
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Repository
public interface CardXrefRepository extends JpaRepository<CardXref, CardXrefId> {

    /**
     * Find all cross-reference records for a specific card number.
     * 
     * Replicates the primary lookup functionality from CBTRN01C.cbl paragraph 2000-LOOKUP-XREF.
     * This method supports the core business logic where a card number is used to find the
     * associated customer and account information for transaction validation.
     * 
     * COBOL Equivalent:
     * MOVE XREF-CARD-NUM TO FD-XREF-CARD-NUM
     * READ XREF-FILE RECORD INTO CARD-XREF-RECORD KEY IS FD-XREF-CARD-NUM
     * 
     * @param cardNumber the card number to search for (16-character string)
     * @return list of CardXref entities matching the card number
     */
    List<CardXref> findByXrefCardNum(String cardNumber);

    /**
     * Find the first cross-reference record for a specific card number.
     * 
     * Optimized version for cases where only one cross-reference per card is expected.
     * Returns Optional to handle cases where no cross-reference exists for the card.
     * 
     * @param cardNumber the card number to search for
     * @return Optional containing the first CardXref entity, or empty if not found
     */
    Optional<CardXref> findFirstByXrefCardNum(String cardNumber);

    /**
     * Find all cross-reference records for a specific customer ID.
     * 
     * Enables customer-centric queries to find all cards and accounts associated with a customer.
     * Useful for customer service operations and account management functions.
     * 
     * @param customerId the customer ID to search for
     * @return list of CardXref entities for the customer
     */
    List<CardXref> findByXrefCustId(Long customerId);

    /**
     * Find all cross-reference records for a specific account ID.
     * 
     * Enables account-centric queries to find all cards and customers associated with an account.
     * Useful for account management and reporting functions.
     * 
     * @param accountId the account ID to search for
     * @return list of CardXref entities for the account
     */
    List<CardXref> findByXrefAcctId(Long accountId);

    /**
     * Find cross-reference record by card number and account ID.
     * 
     * Enables validation of specific card-account relationships. This method supports
     * business logic that needs to verify if a particular card is authorized for a
     * specific account before processing transactions.
     * 
     * @param cardNumber the card number
     * @param accountId the account ID
     * @return Optional containing the CardXref entity if the relationship exists
     */
    Optional<CardXref> findByXrefCardNumAndXrefAcctId(String cardNumber, Long accountId);

    /**
     * Check if a cross-reference exists for a card number and account ID combination.
     * 
     * Optimized existence check that doesn't require loading the full entity.
     * Useful for validation logic where only the existence of the relationship matters.
     * 
     * @param cardNumber the card number to check
     * @param accountId the account ID to check
     * @return true if the cross-reference exists, false otherwise
     */
    boolean existsByXrefCardNumAndXrefAcctId(String cardNumber, Long accountId);

    /**
     * Find cross-reference records by customer ID and account ID.
     * 
     * Supports queries that need to find cards linking a specific customer to a specific account.
     * Useful for customer service and account verification operations.
     * 
     * @param customerId the customer ID
     * @param accountId the account ID
     * @return list of CardXref entities linking the customer and account
     */
    List<CardXref> findByXrefCustIdAndXrefAcctId(Long customerId, Long accountId);

    /**
     * Find cross-reference records by card number and customer ID.
     * 
     * Validates that a specific card belongs to a specific customer.
     * Used for authorization and fraud prevention logic.
     * 
     * @param cardNumber the card number
     * @param customerId the customer ID
     * @return list of CardXref entities for the card-customer relationship
     */
    List<CardXref> findByXrefCardNumAndXrefCustId(String cardNumber, Long customerId);

    /**
     * Count cross-reference records for a specific card number.
     * 
     * Provides statistics on how many accounts/customers are associated with a card.
     * Useful for reporting and analytics functions.
     * 
     * @param cardNumber the card number to count relationships for
     * @return count of cross-reference records
     */
    long countByXrefCardNum(String cardNumber);

    /**
     * Count cross-reference records for a specific customer ID.
     * 
     * Provides statistics on how many cards/accounts are associated with a customer.
     * 
     * @param customerId the customer ID to count relationships for
     * @return count of cross-reference records
     */
    long countByXrefCustId(Long customerId);

    /**
     * Count cross-reference records for a specific account ID.
     * 
     * Provides statistics on how many cards/customers are associated with an account.
     * 
     * @param accountId the account ID to count relationships for
     * @return count of cross-reference records
     */
    long countByXrefAcctId(Long accountId);

    /**
     * Find all card numbers for a specific customer ID.
     * 
     * Projection query that returns only card numbers, optimized for cases where
     * only the card number values are needed without loading full entities.
     * 
     * @param customerId the customer ID
     * @return list of card numbers associated with the customer
     */
    @Query("SELECT cx.xrefCardNum FROM CardXref cx WHERE cx.xrefCustId = :customerId")
    List<String> findCardNumbersByCustomerId(@Param("customerId") Long customerId);

    /**
     * Find all account IDs for a specific card number.
     * 
     * Projection query that returns only account IDs, optimized for cases where
     * only the account ID values are needed without loading full entities.
     * 
     * @param cardNumber the card number
     * @return list of account IDs associated with the card
     */
    @Query("SELECT cx.xrefAcctId FROM CardXref cx WHERE cx.xrefCardNum = :cardNumber")
    List<Long> findAccountIdsByCardNumber(@Param("cardNumber") String cardNumber);

    /**
     * Find all customer IDs for a specific account ID.
     * 
     * Projection query that returns only customer IDs, optimized for cases where
     * only the customer ID values are needed without loading full entities.
     * 
     * @param accountId the account ID
     * @return list of customer IDs associated with the account
     */
    @Query("SELECT cx.xrefCustId FROM CardXref cx WHERE cx.xrefAcctId = :accountId")
    List<Long> findCustomerIdsByAccountId(@Param("accountId") Long accountId);

    /**
     * Delete all cross-reference records for a specific card number.
     * 
     * Supports card deactivation scenarios where all relationships for a card
     * need to be removed from the system.
     * 
     * @param cardNumber the card number to remove relationships for
     * @return number of records deleted
     */
    long deleteByXrefCardNum(String cardNumber);

    /**
     * Delete all cross-reference records for a specific customer ID.
     * 
     * Supports customer account closure scenarios where all card relationships
     * for a customer need to be removed.
     * 
     * @param customerId the customer ID to remove relationships for
     * @return number of records deleted
     */
    long deleteByXrefCustId(Long customerId);

    /**
     * Delete all cross-reference records for a specific account ID.
     * 
     * Supports account closure scenarios where all card relationships
     * for an account need to be removed.
     * 
     * @param accountId the account ID to remove relationships for
     * @return number of records deleted
     */
    long deleteByXrefAcctId(Long accountId);
}