/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.repository;

import com.carddemo.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for Card entity operations.
 * 
 * Provides database access methods for credit card management operations,
 * supporting the functionality migrated from COBOL programs COCRDLIC, 
 * COCRDSLC, and COCRDUPC.
 * 
 * Query Methods:
 * - Card listing with pagination and filtering
 * - Card details retrieval by card number
 * - Account-based card filtering
 * - Card existence and validation checks
 * 
 * This repository replaces VSAM KSDS access patterns with modern
 * Spring Data JPA query methods while maintaining identical
 * business logic and data access patterns.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Repository
public interface CardRepository extends JpaRepository<Card, String> {

    /**
     * Find card by card number.
     * Primary lookup method matching COBOL READ operations.
     * 
     * @param cardNumber 16-digit card number
     * @return Optional Card entity
     */
    Optional<Card> findByCardNumber(String cardNumber);

    /**
     * Find cards by account ID with pagination.
     * Supports card listing operations for specific accounts.
     * 
     * @param accountId 11-digit account identifier
     * @param pageable Pagination parameters
     * @return Page of Card entities
     */
    Page<Card> findByAccountId(Long accountId, Pageable pageable);

    /**
     * Find cards by card number pattern with pagination.
     * Supports partial card number searches for listing operations.
     * 
     * @param cardNumber Partial card number for filtering
     * @param pageable Pagination parameters
     * @return Page of Card entities
     */
    Page<Card> findByCardNumberContaining(String cardNumber, Pageable pageable);

    /**
     * Find cards by account ID and card number pattern.
     * Combined filtering for both account and card number.
     * 
     * @param accountId Account identifier filter
     * @param cardNumber Card number pattern filter
     * @param pageable Pagination parameters
     * @return Page of Card entities
     */
    Page<Card> findByAccountIdAndCardNumberContaining(
            Long accountId, String cardNumber, Pageable pageable);

    /**
     * Check if card exists for given account.
     * Validation method for account-card relationship.
     * 
     * @param cardNumber 16-digit card number
     * @param accountId 11-digit account identifier
     * @return true if card exists for account, false otherwise
     */
    boolean existsByCardNumberAndAccountId(String cardNumber, Long accountId);

    /**
     * Count total cards for account.
     * Used for pagination metadata calculation.
     * 
     * @param accountId Account identifier
     * @return Total number of cards for account
     */
    long countByAccountId(Long accountId);

    /**
     * Find active cards by account ID.
     * Filters cards by active status (Y/N).
     * 
     * @param accountId Account identifier
     * @param activeStatus Active status filter (Y/N)
     * @param pageable Pagination parameters
     * @return Page of active Card entities
     */
    Page<Card> findByAccountIdAndActiveStatus(
            Long accountId, String activeStatus, Pageable pageable);

    /**
     * Custom query to find cards with complex filtering.
     * Supports multiple optional filters for card listing.
     * 
     * @param accountId Optional account ID filter
     * @param cardNumber Optional card number filter
     * @param activeStatus Optional active status filter
     * @param pageable Pagination parameters
     * @return Page of filtered Card entities
     */
    @Query("SELECT c FROM Card c WHERE " +
           "(:accountId IS NULL OR c.accountId = :accountId) AND " +
           "(:cardNumber IS NULL OR c.cardNumber LIKE CONCAT('%', :cardNumber, '%')) AND " +
           "(:activeStatus IS NULL OR c.activeStatus = :activeStatus)")
    Page<Card> findCardsWithFilters(
            @Param("accountId") Long accountId,
            @Param("cardNumber") String cardNumber,
            @Param("activeStatus") String activeStatus,
            Pageable pageable);

    /**
     * Find cards ordered by card number for consistent pagination.
     * Ensures stable pagination order matching COBOL key sequence.
     * 
     * @param pageable Pagination parameters
     * @return Page of Card entities ordered by card number
     */
    @Query("SELECT c FROM Card c ORDER BY c.cardNumber")
    Page<Card> findAllOrderedByCardNumber(Pageable pageable);
}