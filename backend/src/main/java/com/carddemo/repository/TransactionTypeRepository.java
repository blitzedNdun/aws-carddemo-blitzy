package com.carddemo.repository;

import com.carddemo.entity.TransactionType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository interface for TransactionType entity providing data access 
 * for transaction_types reference table. Manages transaction type classifications replacing 
 * VSAM TRANTYPE reference file. Provides lookup operations for transaction validation and 
 * categorization. Supports caching annotations for frequently accessed reference data.
 * 
 * This repository extends JpaRepository with TransactionType entity type and String primary 
 * key type, providing standard CRUD operations and custom query methods for transaction 
 * type management.
 * 
 * Key Features:
 * - VSAM TRANTYPE reference file replacement
 * - Transaction type code validation and lookup
 * - Debit/credit flag determination for transaction processing
 * - Cached reference data for optimal performance
 * - Spring Data JPA automatic query generation
 * 
 * @author Blitzy CardDemo Migration Team
 * @version 1.0
 * @since 2024-01-01
 */
@Repository
@Cacheable("transactionTypes")
public interface TransactionTypeRepository extends JpaRepository<TransactionType, String> {

    /**
     * Finds a transaction type by its unique transaction type code.
     * This method provides direct lookup functionality replacing VSAM TRANTYPE 
     * key-based access patterns. Used for transaction validation and classification.
     * 
     * @param transactionTypeCode the 2-character transaction type code (primary key)
     * @return TransactionType entity if found, null otherwise
     * @throws IllegalArgumentException if transactionTypeCode is null or empty
     */
    @Cacheable(value = "transactionTypes", key = "#transactionTypeCode")
    TransactionType findByTransactionTypeCode(String transactionTypeCode);

    /**
     * Finds all transaction types with a specific debit/credit flag.
     * Used for filtering transaction types by their debit ('D') or credit ('C') 
     * classification for accounting and reporting purposes.
     * 
     * @param debitCreditFlag the single character flag ('D' for debit, 'C' for credit)
     * @return List of TransactionType entities matching the debit/credit flag
     * @throws IllegalArgumentException if debitCreditFlag is null
     */
    @Cacheable(value = "transactionTypesByFlag", key = "#debitCreditFlag")
    List<TransactionType> findByDebitCreditFlag(String debitCreditFlag);

    /**
     * Searches for transaction types by partial description match (case-insensitive).
     * Provides flexible search capability for transaction type lookup by description
     * text, supporting administrative and customer service operations.
     * 
     * @param description partial or full description text to search for
     * @return List of TransactionType entities with descriptions containing the search text
     * @throws IllegalArgumentException if description is null or empty
     */
    @Cacheable(value = "transactionTypesByDescription", key = "#description.toLowerCase()")
    List<TransactionType> findByTypeDescriptionContainingIgnoreCase(String description);

    /**
     * Retrieves all transaction types with caching support.
     * Overrides the default findAll() method to add caching for the complete
     * reference data set, optimizing performance for bulk operations.
     * 
     * @return List of all TransactionType entities
     */
    @Override
    @Cacheable(value = "allTransactionTypes")
    List<TransactionType> findAll();

    /**
     * Checks if a transaction type exists by its type code.
     * Provides efficient existence checking for transaction validation
     * without loading the full entity.
     * 
     * @param transactionTypeCode the transaction type code to check
     * @return true if the transaction type exists, false otherwise
     */
    @Cacheable(value = "transactionTypeExists", key = "#transactionTypeCode")
    boolean existsByTransactionTypeCode(String transactionTypeCode);

    /**
     * Counts the total number of transaction types.
     * Useful for administrative reporting and system monitoring.
     * 
     * @return the total count of transaction type records
     */
    @Override
    @Cacheable(value = "transactionTypeCount")
    long count();
}