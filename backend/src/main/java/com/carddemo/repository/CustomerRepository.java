/*
 * CustomerRepository.java
 * 
 * Spring Data JPA repository interface for Customer entity providing
 * data access for customer_data table. Manages customer lookups, profile
 * updates, and personal information queries replacing VSAM CUSTDAT file patterns.
 * 
 * Provides comprehensive customer operations for customer profile management,
 * personal information retrieval, contact details validation, and customer
 * relationship management processes. Extends JpaRepository with Customer 
 * entity type and String primary key type.
 * 
 * This repository replaces VSAM CUSTDAT file I/O operations (READ, WRITE, 
 * REWRITE, DELETE) with modern JPA method implementations for customer_data 
 * table access, supporting customer management processes and account-to-customer
 * relationship navigation.
 * 
 * Based on COBOL copybook: app/cpy/CVCUS01Y.cpy
 * - CUST-ID (PIC 9(09)) for customer identification and primary key access
 * - CUST-FIRST-NAME (PIC X(20)) for name-based searches
 * - CUST-LAST-NAME (PIC X(20)) for name-based searches  
 * - CUST-SSN (PIC X(09)) for SSN-based validation
 * - CUST-PHONE-NUM-1 (PIC X(15)) for contact information retrieval
 */

package com.carddemo.repository;

import com.carddemo.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for Customer entity.
 * 
 * Provides comprehensive data access operations for customer_data table,
 * supporting customer profile management, personal information retrieval,
 * contact details validation, and customer relationship operations.
 * Replaces VSAM CUSTDAT file patterns with modern JPA repository methods
 * that maintain identical functionality while leveraging PostgreSQL
 * relational database capabilities.
 * 
 * Key functionality:
 * - Customer ID-based lookups for customer management operations
 * - Name-based searches for customer service and account linking
 * - SSN-based validation for identity verification
 * - Phone number searches for contact validation
 * - Date-based queries for customer demographics and analytics
 * - Address-based searches for geographic analysis
 * 
 * All methods support the sub-200ms REST API response time requirement
 * through optimized PostgreSQL B-tree index utilization and query planning.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    /**
     * Finds customer by customer ID (primary key lookup).
     * 
     * Supports direct customer lookup operations equivalent to VSAM READ by
     * primary key. Essential for customer validation, profile retrieval,
     * and account-to-customer relationship navigation.
     * 
     * @param customerId the customer ID (9-digit string)
     * @return Optional Customer if found
     */
    Optional<Customer> findByCustomerId(String customerId);

    /**
     * Finds customers by first and last name.
     * 
     * Supports name-based customer lookup operations for customer service
     * and account management functions. Enables customer identification
     * when only name information is available.
     * 
     * @param firstName the customer's first name
     * @param lastName the customer's last name
     * @return list of customers matching the specified names
     */
    List<Customer> findByFirstNameAndLastName(String firstName, String lastName);

    /**
     * Finds customers by last name only.
     * 
     * Supports partial name-based customer lookup for customer service
     * operations. Useful for customer identification when complete name
     * information is not available.
     * 
     * @param lastName the customer's last name
     * @return list of customers with the specified last name
     */
    List<Customer> findByLastName(String lastName);

    /**
     * Finds customer by Social Security Number.
     * 
     * Supports SSN-based customer identification for identity verification
     * and fraud prevention. Essential for customer authentication and
     * account security operations.
     * 
     * @param ssn the customer's SSN (9-digit string)
     * @return Optional Customer if found
     */
    Optional<Customer> findBySsn(String ssn);

    /**
     * Finds customers by primary phone number.
     * 
     * Supports phone-based customer lookup for contact validation and
     * customer service operations. Enables customer identification
     * when phone number is the primary identifier available.
     * 
     * @param phoneNumber1 the customer's primary phone number
     * @return list of customers with the specified phone number
     */
    List<Customer> findByPhoneNumber1(String phoneNumber1);

    /**
     * Finds customers by state code.
     * 
     * Supports geographic customer analysis and regional reporting.
     * Enables state-based customer segmentation for marketing and
     * operational analysis purposes.
     * 
     * @param stateCode the customer's state code (2-character string)
     * @return list of customers in the specified state
     */
    List<Customer> findByStateCode(String stateCode);

    /**
     * Finds customers by ZIP code.
     * 
     * Supports geographic customer analysis and regional reporting.
     * Enables ZIP code-based customer segmentation for targeted
     * marketing campaigns and operational analysis.
     * 
     * @param zipCode the customer's ZIP code
     * @return list of customers in the specified ZIP code
     */
    List<Customer> findByZipCode(String zipCode);

    /**
     * Finds customers by date of birth.
     * 
     * Supports customer demographics analysis and age-based segmentation.
     * Useful for compliance reporting and customer profile analytics.
     * 
     * @param dateOfBirth the customer's date of birth
     * @return list of customers with the specified birth date
     */
    List<Customer> findByDateOfBirth(LocalDate dateOfBirth);

    /**
     * Finds customers by FICO score range.
     * 
     * Supports credit risk analysis and customer segmentation based on
     * credit worthiness. Essential for credit line management and
     * risk assessment processes.
     * 
     * @param minScore minimum FICO score (inclusive)
     * @param maxScore maximum FICO score (inclusive)
     * @return list of customers with FICO scores in the specified range
     */
    List<Customer> findByFicoScoreBetween(Integer minScore, Integer maxScore);

    /**
     * Finds customers by EFT account ID.
     * 
     * Supports payment processing operations and EFT account validation.
     * Enables customer identification for automated payment processing
     * and electronic funds transfer operations.
     * 
     * @param eftAccountId the customer's EFT account ID
     * @return Optional Customer if found
     */
    Optional<Customer> findByEftAccountId(String eftAccountId);

    /**
     * Finds customers by address line 1 for address validation.
     * 
     * Supports address-based customer lookup and duplicate detection.
     * Useful for customer service operations and address validation
     * during customer profile updates.
     * 
     * @param addressLine1 the customer's primary address line
     * @return list of customers with the specified address
     */
    List<Customer> findByAddressLine1(String addressLine1);

    /**
     * Custom query to find customers with complete address information.
     * 
     * Supports comprehensive address validation and customer identification
     * when complete address information is available. Essential for
     * identity verification and fraud prevention operations.
     * 
     * @param addressLine1 primary address line
     * @param city city name
     * @param stateCode state code
     * @param zipCode ZIP code
     * @return list of customers matching the complete address
     */
    @Query("SELECT c FROM Customer c WHERE c.addressLine1 = :addressLine1 " +
           "AND c.city = :city " +
           "AND c.stateCode = :stateCode " +
           "AND c.zipCode = :zipCode")
    List<Customer> findByCompleteAddress(
            @Param("addressLine1") String addressLine1,
            @Param("city") String city,
            @Param("stateCode") String stateCode,
            @Param("zipCode") String zipCode);

    /**
     * Custom query to find customers eligible for promotional offers.
     * 
     * Identifies customers meeting criteria for promotional campaigns
     * based on FICO score, account relationships, and demographic factors.
     * Supports marketing automation and customer retention programs.
     * 
     * @param minFicoScore minimum FICO score for eligibility
     * @param stateCode target state for geographic promotion
     * @return list of customers eligible for promotional offers
     */
    @Query("SELECT c FROM Customer c WHERE c.ficoScore >= :minFicoScore " +
           "AND c.stateCode = :stateCode " +
           "AND c.customerId IN (SELECT a.customerId FROM Account a WHERE a.activeStatus = 'Y') " +
           "ORDER BY c.ficoScore DESC")
    List<Customer> findCustomersEligibleForPromotion(
            @Param("minFicoScore") Integer minFicoScore,
            @Param("stateCode") String stateCode);

    /**
     * Inherited JpaRepository methods provide standard CRUD operations:
     * 
     * save(Customer entity) - Saves or updates customer
     * saveAll(Iterable<Customer> entities) - Batch save operations
     * findById(String id) - Retrieves customer by primary key
     * findAll() - Retrieves all customers
     * delete(Customer entity) - Deletes customer
     * deleteById(String id) - Deletes customer by primary key
     * deleteAll() - Deletes all customers
     * count() - Returns total count of customers
     * existsById(String id) - Checks existence by primary key
     * flush() - Synchronizes persistence context with database
     * 
     * All inherited methods maintain ACID compliance through Spring @Transactional
     * annotations and PostgreSQL transaction management, ensuring data consistency
     * equivalent to VSAM KSDS file operations while providing enhanced relational
     * database capabilities and performance optimization through B-tree indexing.
     */
}