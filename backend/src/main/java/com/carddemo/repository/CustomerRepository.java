/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.repository;

import com.carddemo.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * Spring Data JPA repository interface for Customer entity providing data access operations 
 * for customer_data table. Replaces VSAM CUSTDAT file access patterns with JPA operations.
 * 
 * This repository provides CRUD operations, custom query methods for customer search by various
 * criteria including name, SSN, phone numbers, and supports pagination for large result sets.
 * Extends JpaRepository with Customer entity type and Long primary key type.
 * 
 * Repository Methods:
 * - Standard CRUD operations inherited from JpaRepository
 * - Custom finder methods for customer search operations
 * - Case-insensitive search queries for enhanced user experience
 * - Pagination support through Pageable parameter
 * 
 * VSAM Dataset Mapping:
 * - Replaces CUSTDAT KSDS dataset access patterns
 * - Maintains equivalent search capabilities through JPA query derivation
 * - Supports browse operations via pagination (STARTBR/READNEXT equivalent)
 * 
 * Performance Characteristics:
 * - Primary key access: Sub-millisecond response via customer_id index
 * - Name-based search: Optimized via customer_name_idx composite index
 * - SSN lookup: Direct access via unique constraint validation
 * - Paginated results: Efficient navigation for large customer datasets
 * 
 * Field Mapping from COBOL CUSTDAT record structure:
 * - CUST-ID → customerId (Primary Key)
 * - CUST-FIRST-NAME → firstName
 * - CUST-LAST-NAME → lastName  
 * - CUST-SSN → ssn (encrypted field)
 * - CUST-PHONE-NUM-1 → phoneNumber1
 * - CUST-PHONE-NUM-2 → phoneNumber2
 * - CUST-GOVT-ISSUED-ID → governmentIssuedId
 * - CUST-EFT-ACCOUNT-ID → eftAccountId
 * - CUST-FICO-CREDIT-SCORE → ficoScore
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Finds customers by exact match on last name and first name.
     * Case-sensitive search matching COBOL string comparison behavior.
     * 
     * Equivalent VSAM operation: STARTBR on CUSTDAT with name key
     * 
     * @param lastName Customer last name for exact match
     * @param firstName Customer first name for exact match
     * @return List of customers matching both name criteria
     */
    List<Customer> findByLastNameAndFirstName(String lastName, String firstName);

    /**
     * Finds customers by case-insensitive match on last name and first name.
     * Enhanced search capability not available in original COBOL implementation.
     * 
     * Supports modern user interface requirements for flexible name searching.
     * 
     * @param lastName Customer last name for case-insensitive match
     * @param firstName Customer first name for case-insensitive match
     * @return List of customers matching both name criteria (case-insensitive)
     */
    List<Customer> findByLastNameAndFirstNameIgnoreCase(String lastName, String firstName);

    /**
     * Finds customers by case-insensitive partial match on first name.
     * Supports wildcard-like searching for customer discovery operations.
     * 
     * Equivalent to COBOL pattern matching but with enhanced capabilities.
     * Uses PostgreSQL ILIKE operator for efficient text matching.
     * 
     * @param firstName Partial first name for pattern matching
     * @return List of customers with first names containing the specified text
     */
    List<Customer> findByFirstNameContainingIgnoreCase(String firstName);

    /**
     * Finds customers by case-insensitive partial match on last name.
     * Supports wildcard-like searching for customer discovery operations.
     * 
     * Most commonly used search pattern for customer service representatives.
     * Optimized by customer_name_idx index on (last_name, first_name).
     * 
     * @param lastName Partial last name for pattern matching
     * @return List of customers with last names containing the specified text
     */
    List<Customer> findByLastNameContainingIgnoreCase(String lastName);

    /**
     * Finds customer by Social Security Number (SSN).
     * Returns single customer due to SSN uniqueness constraint.
     * 
     * Equivalent VSAM operation: READ on CUSTDAT with SSN key
     * 
     * Security Note: SSN field is encrypted at rest using PostgreSQL pgcrypto.
     * Search operations use encrypted comparison for data protection.
     * 
     * @param ssn Customer Social Security Number (9 digits)
     * @return Optional Customer matching the SSN, empty if not found
     */
    Optional<Customer> findBySSN(String ssn);

    /**
     * Finds customers by phone number match.
     * Searches both phoneNumber1 and phoneNumber2 fields.
     * 
     * Equivalent COBOL logic: Check both CUST-PHONE-NUM-1 and CUST-PHONE-NUM-2
     * 
     * @param phoneNumber Phone number for exact match
     * @return List of customers with matching phone numbers
     */
    List<Customer> findByPhoneNumber1OrPhoneNumber2(String phoneNumber);

    /**
     * Finds customer by customer ID (Primary Key).
     * Direct primary key access with fastest possible performance.
     * 
     * Equivalent VSAM operation: READ on CUSTDAT primary key
     * 
     * @param customerId Customer ID for direct lookup
     * @return Optional Customer matching the ID, empty if not found
     */
    Optional<Customer> findByCustomerId(Long customerId);

    /**
     * Finds customers with FICO score greater than specified threshold.
     * Used for credit qualification and risk assessment operations.
     * 
     * Equivalent COBOL logic: IF CUST-FICO-CREDIT-SCORE > threshold
     * 
     * @param threshold Minimum FICO score for filtering
     * @return List of customers with FICO scores above the threshold
     */
    List<Customer> findByFicoScoreGreaterThan(Integer threshold);

    /**
     * Finds customer by government issued ID.
     * Alternative customer identification method for verification.
     * 
     * Maps to CUST-GOVT-ISSUED-ID field from COBOL copybook.
     * 
     * @param governmentId Government issued ID for lookup
     * @return Optional Customer matching the government ID
     */
    Optional<Customer> findByGovernmentIssuedId(String governmentId);

    /**
     * Finds customer by EFT account ID.
     * Used for electronic funds transfer account linking operations.
     * 
     * Maps to CUST-EFT-ACCOUNT-ID field from COBOL copybook.
     * 
     * @param eftAccountId EFT account identifier for lookup
     * @return Optional Customer linked to the EFT account
     */
    Optional<Customer> findByEftAccountId(String eftAccountId);

    // Additional convenience methods for phone number searching

    /**
     * Finds customers by primary phone number (phoneNumber1).
     * Direct search on the primary phone number field.
     * 
     * @param phoneNumber1 Primary phone number for exact match
     * @return List of customers with matching primary phone number
     */
    List<Customer> findByPhoneNumber1(String phoneNumber1);

    /**
     * Finds customers by secondary phone number (phoneNumber2).
     * Direct search on the secondary phone number field.
     * 
     * @param phoneNumber2 Secondary phone number for exact match
     * @return List of customers with matching secondary phone number
     */
    List<Customer> findByPhoneNumber2(String phoneNumber2);

    // Paginated search methods for large result sets

    /**
     * Finds customers by last name with pagination support.
     * Essential for handling large customer datasets efficiently.
     * 
     * Equivalent to VSAM STARTBR/READNEXT browse operations with positioning.
     * 
     * @param lastName Customer last name for search
     * @param pageable Pagination and sorting parameters
     * @return Page of customers matching the last name criteria
     */
    org.springframework.data.domain.Page<Customer> findByLastNameContainingIgnoreCase(String lastName, Pageable pageable);

    /**
     * Finds customers by first name with pagination support.
     * Supports efficient navigation through large result sets.
     * 
     * @param firstName Customer first name for search
     * @param pageable Pagination and sorting parameters
     * @return Page of customers matching the first name criteria
     */
    org.springframework.data.domain.Page<Customer> findByFirstNameContainingIgnoreCase(String firstName, Pageable pageable);

    /**
     * Finds customers with FICO score range with pagination support.
     * Used for credit analysis and customer segmentation operations.
     * 
     * @param minScore Minimum FICO score for filtering
     * @param maxScore Maximum FICO score for filtering
     * @param pageable Pagination and sorting parameters
     * @return Page of customers within the FICO score range
     */
    org.springframework.data.domain.Page<Customer> findByFicoScoreBetween(Integer minScore, Integer maxScore, Pageable pageable);

    /**
     * Counts customers by state code.
     * Used for geographic distribution analysis and reporting.
     * 
     * @param stateCode Two-character state code for counting
     * @return Number of customers in the specified state
     */
    long countByStateCode(String stateCode);

    /**
     * Finds customers by state code with pagination.
     * Geographic customer listing for regional operations.
     * 
     * @param stateCode Two-character state code for filtering
     * @param pageable Pagination and sorting parameters
     * @return Page of customers in the specified state
     */
    org.springframework.data.domain.Page<Customer> findByStateCode(String stateCode, Pageable pageable);

    /**
     * Finds customers by primary card holder indicator.
     * Identifies primary account holders for billing and communication.
     * 
     * Maps to CUST-PRI-CARD-HOLDER-IND field from COBOL copybook.
     * 
     * @param indicator Primary card holder indicator ('Y' or 'N')
     * @return List of customers matching the indicator
     */
    List<Customer> findByPrimaryCardHolderIndicator(String indicator);

    /**
     * Alias method for findByPhoneNumber1OrPhoneNumber2 to match export schema.
     * Provides backward compatibility with expected method name.
     * 
     * @param phoneNumber Phone number for search in either phone field
     * @return List of customers with matching phone numbers
     */
    default List<Customer> findByPhoneNumber(String phoneNumber) {
        return findByPhoneNumber1OrPhoneNumber2(phoneNumber);
    }

    /**
     * Alias method for findByGovernmentIssuedId to match export schema.
     * Provides backward compatibility with expected method name.
     * 
     * @param governmentId Government issued ID for lookup
     * @return Optional Customer matching the government ID
     */
    default Optional<Customer> findByGovernmentId(String governmentId) {
        return findByGovernmentIssuedId(governmentId);
    }

    // Note: The following methods are inherited from JpaRepository<Customer, Long>:
    // - save(Customer entity)
    // - saveAll(Iterable<Customer> entities)
    // - findById(Long id)
    // - findAll()
    // - findAll(Pageable pageable)
    // - delete(Customer entity)
    // - deleteById(Long id)
    // - deleteAll()
    // - count()
    // - existsById(Long id)
    // - flush()
}