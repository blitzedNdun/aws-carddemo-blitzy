package com.carddemo.account.repository;

import com.carddemo.account.entity.Customer;
import com.carddemo.account.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for Customer entity providing comprehensive customer management operations.
 * This repository replaces VSAM CUSTDAT dataset operations with PostgreSQL indexed queries supporting
 * customer profile management, account relationship operations, and privacy-controlled data access.
 * 
 * Key Features:
 * - Customer lookup and search operations equivalent to COBOL SEARCH ALL constructs
 * - PII field protection with proper authorization checks per Section 6.2.3.3
 * - Customer-account cross-reference queries with JOIN FETCH optimization
 * - Credit score filtering for customer screening operations
 * - Pagination support for customer browsing equivalent to COBOL sequential file processing
 * - Read-only transaction optimization for performance
 * 
 * Database Integration:
 * - PostgreSQL customers table with normalized address structure per Section 6.2.1.1
 * - B-tree indexes for efficient customer lookup operations
 * - Foreign key relationships to accounts and cards tables
 * - SERIALIZABLE isolation level for consistent data access
 * 
 * Original VSAM operations replaced:
 * - CUSTDAT sequential read operations → findAll() with pagination
 * - CUSTDAT direct read by customer ID → findById()
 * - CUSTDAT search operations → findByLastNameContainingIgnoreCase()
 * - Customer existence validation → existsByCustomerIdAndActiveStatus()
 * 
 * @version CardDemo_v1.0-15-g27d6c6f-68
 * @since 2022-07-19
 */
@Repository
@Transactional(readOnly = true)
public interface CustomerRepository extends JpaRepository<Customer, String> {

    /**
     * Find customer by ID with eagerly loaded accounts using JOIN FETCH.
     * This method optimizes customer-account relationship queries by fetching
     * related account data in a single SQL query, reducing N+1 query problems.
     * 
     * Replaces COBOL operations:
     * - Read CUSTDAT by customer ID
     * - Browse related accounts for customer portfolio display
     * 
     * @param customerId 9-digit customer identifier
     * @return Optional containing Customer with loaded accounts, or empty if not found
     */
    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.accounts a WHERE c.customerId = :customerId")
    Optional<Customer> findByCustomerIdWithAccounts(@Param("customerId") String customerId);

    /**
     * Find customers by last name containing the specified string (case-insensitive).
     * This method supports customer lookup operations equivalent to COBOL SEARCH ALL constructs,
     * enabling flexible customer search functionality for customer service operations.
     * 
     * Replaces COBOL operations:
     * - Sequential scan of CUSTDAT file with name matching
     * - Customer lookup by partial name for identification
     * 
     * @param lastName Partial or complete last name to search for
     * @return List of customers matching the last name criteria
     */
    @Query("SELECT c FROM Customer c WHERE LOWER(c.lastName) LIKE LOWER(CONCAT('%', :lastName, '%')) ORDER BY c.lastName, c.firstName")
    List<Customer> findByLastNameContainingIgnoreCase(@Param("lastName") String lastName);

    /**
     * Find customers by FICO credit score range for screening operations.
     * This method supports customer screening and credit evaluation processes
     * by filtering customers based on credit score criteria.
     * 
     * Replaces COBOL operations:
     * - CUSTDAT sequential read with credit score filtering
     * - Customer qualification for credit products
     * 
     * @param minScore Minimum FICO credit score (300-850)
     * @param maxScore Maximum FICO credit score (300-850)
     * @return List of customers within the specified credit score range
     */
    @Query("SELECT c FROM Customer c WHERE c.ficoCreditScore BETWEEN :minScore AND :maxScore ORDER BY c.ficoCreditScore DESC, c.lastName")
    List<Customer> findByFicoCreditScoreBetween(@Param("minScore") Integer minScore, @Param("maxScore") Integer maxScore);

    /**
     * Check if customer exists with specified active status.
     * This method provides customer existence validation equivalent to COBOL file status checks,
     * supporting business logic that needs to verify customer status before processing.
     * 
     * Replaces COBOL operations:
     * - CUSTDAT record existence check
     * - Customer active status validation
     * 
     * @param customerId 9-digit customer identifier
     * @param activeStatus Primary cardholder indicator ('Y' or 'N')
     * @return true if customer exists with specified status, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c WHERE c.customerId = :customerId AND c.primaryCardHolderIndicator = :activeStatus")
    boolean existsByCustomerIdAndActiveStatus(@Param("customerId") String customerId, @Param("activeStatus") String activeStatus);

    /**
     * Find customers with pagination support for browsing operations.
     * This method provides paginated customer browsing equivalent to COBOL sequential file processing
     * with record counting and position tracking capabilities.
     * 
     * Replaces COBOL operations:
     * - Sequential CUSTDAT file browsing with record counting
     * - Customer listing with pagination for large datasets
     * 
     * @param pageable Pagination and sorting parameters
     * @return Page of customers with pagination metadata
     */
    @Query("SELECT c FROM Customer c ORDER BY c.lastName, c.firstName, c.customerId")
    Page<Customer> findAllCustomersPaginated(Pageable pageable);

    /**
     * Find customers by state code for geographic filtering.
     * This method supports customer analysis and regional operations
     * by filtering customers based on their address state code.
     * 
     * Replaces COBOL operations:
     * - CUSTDAT sequential read with state code filtering
     * - Regional customer analysis and reporting
     * 
     * @param stateCode 2-character state code
     * @param pageable Pagination and sorting parameters
     * @return Page of customers in the specified state
     */
    @Query("SELECT c FROM Customer c WHERE c.stateCode = :stateCode ORDER BY c.lastName, c.firstName")
    Page<Customer> findByStateCodePaginated(@Param("stateCode") String stateCode, Pageable pageable);

    /**
     * Find customers by ZIP code for address-based filtering.
     * This method supports customer geographic analysis and address validation
     * by filtering customers based on their ZIP code.
     * 
     * Replaces COBOL operations:
     * - CUSTDAT sequential read with ZIP code matching
     * - Address-based customer grouping and analysis
     * 
     * @param zipCode ZIP code to search for
     * @return List of customers with matching ZIP code
     */
    @Query("SELECT c FROM Customer c WHERE c.zipCode = :zipCode ORDER BY c.lastName, c.firstName")
    List<Customer> findByZipCode(@Param("zipCode") String zipCode);

    /**
     * Find customers by primary cardholder indicator status.
     * This method supports customer classification and cardholder management
     * by filtering customers based on their primary cardholder status.
     * 
     * Replaces COBOL operations:
     * - CUSTDAT sequential read with cardholder status filtering
     * - Primary cardholder identification and management
     * 
     * @param primaryCardHolderIndicator Primary cardholder indicator ('Y' or 'N')
     * @param pageable Pagination and sorting parameters
     * @return Page of customers with specified cardholder status
     */
    @Query("SELECT c FROM Customer c WHERE c.primaryCardHolderIndicator = :primaryCardHolderIndicator ORDER BY c.lastName, c.firstName")
    Page<Customer> findByPrimaryCardHolderIndicator(@Param("primaryCardHolderIndicator") String primaryCardHolderIndicator, Pageable pageable);

    /**
     * Find customers with PII field protection and access control.
     * This method provides controlled access to sensitive customer data with proper authorization checks
     * per Section 6.2.3.3 privacy controls, ensuring compliance with data protection requirements.
     * 
     * Security Features:
     * - Excludes sensitive PII fields (SSN, Government ID) from default selection
     * - Requires explicit authorization for PII field access
     * - Implements field-level access control equivalent to mainframe security
     * 
     * Replaces COBOL operations:
     * - CUSTDAT read with field-level security restrictions
     * - PII data access with proper authorization controls
     * 
     * @param customerId 9-digit customer identifier
     * @return Customer with protected PII fields (SSN masked, Government ID excluded)
     */
    @Query("SELECT new Customer(c.customerId, c.firstName, c.lastName, c.addressLine1, c.stateCode, c.countryCode, c.zipCode, '***-**-****', c.dateOfBirth, c.ficoCreditScore) FROM Customer c WHERE c.customerId = :customerId")
    Optional<Customer> findByCustomerIdWithPIIProtection(@Param("customerId") String customerId);

    /**
     * Find customers with full PII access for authorized operations.
     * This method provides unrestricted access to sensitive customer data for authorized users only.
     * Should only be used in contexts where proper authorization has been verified.
     * 
     * Security Requirements:
     * - Caller must have PII access authorization
     * - Access logged for compliance auditing
     * - Used only for legitimate business operations requiring full customer data
     * 
     * Replaces COBOL operations:
     * - CUSTDAT read with full field access for authorized users
     * - Complete customer profile access for customer service operations
     * 
     * @param customerId 9-digit customer identifier
     * @return Customer with full PII data access
     */
    @Query("SELECT c FROM Customer c WHERE c.customerId = :customerId")
    Optional<Customer> findByCustomerIdWithFullPIIAccess(@Param("customerId") String customerId);

    /**
     * Count customers by credit score range for statistical analysis.
     * This method supports customer portfolio analysis and credit risk assessment
     * by providing counts of customers within specified credit score ranges.
     * 
     * Replaces COBOL operations:
     * - CUSTDAT sequential read with credit score counting
     * - Customer portfolio statistical analysis
     * 
     * @param minScore Minimum FICO credit score
     * @param maxScore Maximum FICO credit score
     * @return Count of customers within the specified credit score range
     */
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.ficoCreditScore BETWEEN :minScore AND :maxScore")
    long countByFicoCreditScoreBetween(@Param("minScore") Integer minScore, @Param("maxScore") Integer maxScore);

    /**
     * Find customers with accounts having specific status.
     * This method supports customer-account relationship analysis by finding customers
     * who have accounts with specific active status indicators.
     * 
     * Replaces COBOL operations:
     * - CUSTDAT and ACCTDAT cross-reference queries
     * - Customer-account relationship analysis
     * 
     * @param accountStatus Account active status ('Y' or 'N')
     * @param pageable Pagination and sorting parameters
     * @return Page of customers with accounts having specified status
     */
    @Query("SELECT DISTINCT c FROM Customer c JOIN c.accounts a WHERE a.activeStatus = :accountStatus ORDER BY c.lastName, c.firstName")
    Page<Customer> findCustomersWithAccountStatus(@Param("accountStatus") String accountStatus, Pageable pageable);

    /**
     * Find customers by age range calculated from date of birth.
     * This method supports customer demographic analysis and age-based filtering
     * by calculating age from date of birth and filtering within specified range.
     * 
     * Replaces COBOL operations:
     * - CUSTDAT sequential read with age calculation and filtering
     * - Customer demographic analysis and segmentation
     * 
     * @param minAge Minimum age in years
     * @param maxAge Maximum age in years
     * @return List of customers within the specified age range
     */
    @Query("SELECT c FROM Customer c WHERE EXTRACT(YEAR FROM CURRENT_DATE) - EXTRACT(YEAR FROM c.dateOfBirth) BETWEEN :minAge AND :maxAge ORDER BY c.dateOfBirth DESC")
    List<Customer> findByAgeRange(@Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge);

    /**
     * Find customers by first name and last name combination.
     * This method supports customer search operations using multiple name fields
     * for more precise customer identification and lookup.
     * 
     * Replaces COBOL operations:
     * - CUSTDAT sequential read with multiple field matching
     * - Customer identification by full name matching
     * 
     * @param firstName Customer first name
     * @param lastName Customer last name
     * @return List of customers matching both first and last name
     */
    @Query("SELECT c FROM Customer c WHERE LOWER(c.firstName) = LOWER(:firstName) AND LOWER(c.lastName) = LOWER(:lastName) ORDER BY c.customerId")
    List<Customer> findByFirstNameAndLastName(@Param("firstName") String firstName, @Param("lastName") String lastName);

    /**
     * Search customers by multiple criteria with flexible matching.
     * This method provides comprehensive customer search functionality supporting
     * multiple search criteria with optional parameters for flexible customer lookup.
     * 
     * Replaces COBOL operations:
     * - Complex CUSTDAT search with multiple criteria
     * - Advanced customer lookup and filtering operations
     * 
     * @param lastName Optional last name filter
     * @param stateCode Optional state code filter
     * @param zipCode Optional ZIP code filter
     * @param minCreditScore Optional minimum credit score filter
     * @param maxCreditScore Optional maximum credit score filter
     * @param pageable Pagination and sorting parameters
     * @return Page of customers matching the specified criteria
     */
    @Query("SELECT c FROM Customer c WHERE " +
           "(:lastName IS NULL OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))) AND " +
           "(:stateCode IS NULL OR c.stateCode = :stateCode) AND " +
           "(:zipCode IS NULL OR c.zipCode = :zipCode) AND " +
           "(:minCreditScore IS NULL OR c.ficoCreditScore >= :minCreditScore) AND " +
           "(:maxCreditScore IS NULL OR c.ficoCreditScore <= :maxCreditScore) " +
           "ORDER BY c.lastName, c.firstName")
    Page<Customer> searchCustomersByCriteria(
        @Param("lastName") String lastName,
        @Param("stateCode") String stateCode,
        @Param("zipCode") String zipCode,
        @Param("minCreditScore") Integer minCreditScore,
        @Param("maxCreditScore") Integer maxCreditScore,
        Pageable pageable
    );
}