/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.repository;

import com.carddemo.entity.Card;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Spring Data JPA repository interface for Card entity providing data access operations 
 * for card_data table. Replaces VSAM CARDDAT file access and CARDAIX alternate index 
 * with JPA operations.
 * 
 * This repository interface provides comprehensive card data access functionality that 
 * replicates the behavior of the original VSAM CARDDAT dataset and CARDAIX alternate index.
 * The implementation supports all card management operations required by the credit card 
 * processing system while leveraging Spring Data JPA's advanced query capabilities.
 * 
 * VSAM Migration Details:
 * - CARDDAT Dataset → card_data PostgreSQL table
 * - CARDAIX Alternate Index → findByAccountId and findByCustomerId methods
 * - VSAM STARTBR/READNEXT → Pageable-based pagination
 * - VSAM READ/WRITE/REWRITE/DELETE → Standard JPA CRUD operations
 * 
 * Key Features:
 * - Primary key access via card number (replacing VSAM primary key access)
 * - Account-based card lookups (replacing CARDAIX alternate index)
 * - Customer-based card lookups for comprehensive relationship queries
 * - Status-based filtering for active/inactive card management
 * - Existence checks for card number validation
 * - Counting operations for business reporting
 * - Pagination support for large result sets
 * 
 * Security Considerations:
 * - All card data access is secured through Spring Security
 * - CVV codes are excluded from query results via Card entity @JsonIgnore
 * - Card numbers are masked in display operations through Card.getMaskedCardNumber()
 * 
 * Performance Optimizations:
 * - Database indexes on account_id and customer_id for efficient lookups
 * - Lazy loading relationships to prevent unnecessary data fetching
 * - Pageable support for large result sets to prevent memory issues
 * 
 * This implementation preserves all business logic from original COBOL programs:
 * - COCRDLIC.cbl (card list/inquiry operations)
 * - COCRDSLC.cbl (card selection operations)  
 * - COCRDUPC.cbl (card update operations)
 * - COACTVWC.cbl (account view with card details)
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Repository
public interface CardRepository extends JpaRepository<Card, String> {

    /**
     * Finds all cards associated with a specific account ID.
     * 
     * This method replaces the CARDAIX alternate index access pattern from the
     * original VSAM implementation. The CARDAIX provided secondary access to
     * card records based on account ID, enabling efficient lookup of all cards
     * belonging to a specific account.
     * 
     * Database Query: SELECT * FROM card_data WHERE account_id = ?
     * VSAM Equivalent: STARTBR on CARDAIX with account ID as key
     * 
     * @param accountId the account ID to search for
     * @return List of Card entities associated with the account
     */
    List<Card> findByAccountId(Long accountId);

    /**
     * Finds all cards with a specific active status.
     * 
     * This method supports card validation queries by status, enabling the system
     * to efficiently identify active or inactive cards for business operations
     * such as card activation/deactivation processing and reporting.
     * 
     * Database Query: SELECT * FROM card_data WHERE active_status = ?
     * 
     * @param activeStatus the active status ('Y' for active, 'N' for inactive)
     * @return List of Card entities with the specified status
     */
    List<Card> findByActiveStatus(String activeStatus);

    /**
     * Finds a card by its card number (primary key lookup).
     * 
     * This method provides explicit card number lookup functionality, though
     * the same result can be achieved using findById(). This method exists
     * for clarity in business logic where card number is the search criteria.
     * 
     * Database Query: SELECT * FROM card_data WHERE card_number = ?
     * VSAM Equivalent: Direct READ on CARDDAT using primary key
     * 
     * @param cardNumber the 16-digit card number
     * @return Card entity if found, null otherwise
     */
    Card findByCardNumber(String cardNumber);

    /**
     * Finds all cards associated with a specific customer ID.
     * 
     * This method enables customer-centric card lookups, supporting business
     * operations that require accessing all cards belonging to a specific
     * customer across multiple accounts.
     * 
     * Database Query: SELECT * FROM card_data WHERE customer_id = ?
     * 
     * @param customerId the customer ID to search for
     * @return List of Card entities associated with the customer
     */
    List<Card> findByCustomerId(Long customerId);

    /**
     * Finds all cards for a specific account with a specific active status.
     * 
     * This compound query supports business operations that need to filter
     * cards by both account association and active status, such as displaying
     * only active cards for an account or identifying inactive cards for
     * cleanup operations.
     * 
     * Database Query: SELECT * FROM card_data WHERE account_id = ? AND active_status = ?
     * 
     * @param accountId the account ID to search for
     * @param activeStatus the active status ('Y' for active, 'N' for inactive)
     * @return List of Card entities matching both criteria
     */
    List<Card> findByAccountIdAndActiveStatus(Long accountId, String activeStatus);

    /**
     * Checks if a card exists with the specified card number.
     * 
     * This method provides efficient existence checking without retrieving
     * the full Card entity, supporting card number validation operations
     * during card creation and validation processes.
     * 
     * Database Query: SELECT COUNT(*) > 0 FROM card_data WHERE card_number = ?
     * 
     * @param cardNumber the 16-digit card number to check
     * @return true if card exists, false otherwise
     */
    boolean existsByCardNumber(String cardNumber);

    /**
     * Counts the number of cards associated with a specific account.
     * 
     * This method supports business reporting and validation operations
     * that need to determine how many cards are associated with an account
     * without retrieving the actual card data.
     * 
     * Database Query: SELECT COUNT(*) FROM card_data WHERE account_id = ?
     * 
     * @param accountId the account ID to count cards for
     * @return number of cards associated with the account
     */
    long countByAccountId(Long accountId);

    /**
     * Counts the number of cards with a specific active status.
     * 
     * This method supports business reporting operations that need statistics
     * on active vs inactive cards in the system for monitoring and
     * administrative purposes.
     * 
     * Database Query: SELECT COUNT(*) FROM card_data WHERE active_status = ?
     * 
     * @param activeStatus the active status ('Y' for active, 'N' for inactive)
     * @return number of cards with the specified status
     */
    long countByActiveStatus(String activeStatus);
}