package com.carddemo.account.service;

import com.carddemo.account.entity.Customer;
import com.carddemo.account.repository.CustomerRepository;
import com.carddemo.account.entity.Account;
import com.carddemo.common.util.ValidationUtils;
import com.carddemo.common.enums.ValidationResult;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * CustomerService provides comprehensive customer management operations including customer profile retrieval,
 * search functionality, FICO credit score validation, and PII-protected data access, supporting customer-account
 * relationship operations and privacy compliance testing scenarios equivalent to VSAM CUSTDAT access patterns.
 * 
 * This service implements the customer management operations layer that replaces COBOL CUSTDAT file processing
 * with modern Spring Boot transactional services, providing secure customer data access with field-level
 * protection per Section 6.2.3.3 privacy controls and efficient customer-account cross-reference queries
 * per Section 6.2.1.1 database design requirements.
 * 
 * Key Features:
 * - Customer profile management with PII field protection and access control
 * - Customer search functionality equivalent to COBOL SEARCH ALL constructs
 * - FICO credit score validation and range enforcement (300-850)
 * - Customer-account relationship management supporting portfolio operations
 * - Spring @Transactional support with SERIALIZABLE isolation level
 * - Comprehensive validation and error handling with audit trail
 * - Integration with CustomerRepository for PostgreSQL customer data access
 * 
 * Security Features:
 * - PII field protection per Section 6.2.3.3 privacy controls
 * - Field-level access control for sensitive customer data
 * - Audit logging for all customer data access operations
 * - Role-based authorization for customer profile operations
 * 
 * Performance Optimizations:
 * - Efficient customer-account cross-reference queries with JOIN FETCH
 * - Paginated search results for large customer datasets
 * - Read-only transaction optimization for query operations
 * - Connection pooling integration with HikariCP
 * 
 * Original COBOL operations replaced:
 * - CUSTDAT sequential read operations → findAllCustomers() with pagination
 * - CUSTDAT direct read by customer ID → getCustomerById()
 * - CUSTDAT search operations → searchCustomersByLastName()
 * - Customer existence validation → existsByCustomerId()
 * - Customer profile validation → validateCustomer()
 * 
 * @version CardDemo_v1.0-15-g27d6c6f-68
 * @since 2022-07-19
 */
@Service
@Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
public class CustomerService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    /**
     * CustomerRepository for PostgreSQL customer data access replacing VSAM CUSTDAT operations
     */
    @Autowired
    private CustomerRepository customerRepository;

    /**
     * Default page size for customer search operations
     */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Maximum page size allowed for customer search operations
     */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * FICO credit score minimum valid value
     */
    private static final int MIN_FICO_SCORE = 300;

    /**
     * FICO credit score maximum valid value
     */
    private static final int MAX_FICO_SCORE = 850;

    /**
     * Retrieves customer by ID with basic information (PII-protected).
     * This method provides customer profile access with PII field protection per Section 6.2.3.3,
     * ensuring sensitive information is masked or excluded from the response.
     * 
     * Replaces COBOL operations:
     * - READ CUSTDAT file with customer ID key
     * - Customer profile display with field-level security
     * 
     * @param customerId 9-digit customer identifier
     * @return Optional containing Customer with PII protection, or empty if not found
     * @throws IllegalArgumentException if customer ID is null or invalid format
     */
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public Optional<Customer> getCustomerById(String customerId) {
        logger.debug("Retrieving customer by ID: {}", customerId);
        
        // Validate customer ID format
        ValidationResult validationResult = ValidationUtils.validateCustomerId(customerId);
        if (!validationResult.isValid()) {
            logger.warn("Invalid customer ID format: {} - {}", customerId, validationResult.getErrorMessage());
            throw new IllegalArgumentException("Invalid customer ID format: " + validationResult.getErrorMessage());
        }
        
        try {
            // Retrieve customer with PII protection
            Optional<Customer> customerOptional = customerRepository.findByCustomerIdWithPIIProtection(customerId);
            
            if (customerOptional.isPresent()) {
                logger.info("Customer retrieved successfully: {}", customerId);
                return customerOptional;
            } else {
                logger.warn("Customer not found: {}", customerId);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving customer by ID: {}", customerId, e);
            throw new RuntimeException("Failed to retrieve customer: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves customer by ID with eagerly loaded accounts using JOIN FETCH.
     * This method optimizes customer-account relationship queries by fetching related account data
     * in a single SQL query, supporting customer portfolio operations and account cross-references.
     * 
     * Replaces COBOL operations:
     * - READ CUSTDAT by customer ID with account cross-reference
     * - Browse related accounts for customer portfolio display
     * 
     * @param customerId 9-digit customer identifier
     * @return Optional containing Customer with loaded accounts, or empty if not found
     * @throws IllegalArgumentException if customer ID is null or invalid format
     */
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public Optional<Customer> getCustomerWithAccounts(String customerId) {
        logger.debug("Retrieving customer with accounts: {}", customerId);
        
        // Validate customer ID format
        ValidationResult validationResult = ValidationUtils.validateCustomerId(customerId);
        if (!validationResult.isValid()) {
            logger.warn("Invalid customer ID format: {} - {}", customerId, validationResult.getErrorMessage());
            throw new IllegalArgumentException("Invalid customer ID format: " + validationResult.getErrorMessage());
        }
        
        try {
            // Retrieve customer with accounts using JOIN FETCH
            Optional<Customer> customerOptional = customerRepository.findByCustomerIdWithAccounts(customerId);
            
            if (customerOptional.isPresent()) {
                Customer customer = customerOptional.get();
                logger.info("Customer with accounts retrieved successfully: {} (accounts: {})", 
                    customerId, customer.getAccounts().size());
                return Optional.of(customer);
            } else {
                logger.warn("Customer not found: {}", customerId);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving customer with accounts: {}", customerId, e);
            throw new RuntimeException("Failed to retrieve customer with accounts: " + e.getMessage(), e);
        }
    }

    /**
     * Searches customers by last name with case-insensitive matching.
     * This method supports customer lookup operations equivalent to COBOL SEARCH ALL constructs,
     * enabling flexible customer search functionality for customer service operations.
     * 
     * Replaces COBOL operations:
     * - Sequential scan of CUSTDAT file with name matching
     * - Customer lookup by partial name for identification
     * 
     * @param lastName Partial or complete last name to search for
     * @return List of customers matching the last name criteria
     * @throws IllegalArgumentException if lastName is null or empty
     */
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public List<Customer> searchCustomersByLastName(String lastName) {
        logger.debug("Searching customers by last name: {}", lastName);
        
        // Validate last name parameter
        ValidationResult validationResult = ValidationUtils.validateRequiredField(lastName);
        if (!validationResult.isValid()) {
            logger.warn("Invalid last name parameter: {} - {}", lastName, validationResult.getErrorMessage());
            throw new IllegalArgumentException("Last name is required for search");
        }
        
        try {
            // Search customers by last name with case-insensitive matching
            List<Customer> customers = customerRepository.findByLastNameContainingIgnoreCase(lastName.trim());
            
            logger.info("Found {} customers matching last name: {}", customers.size(), lastName);
            return customers;
            
        } catch (Exception e) {
            logger.error("Error searching customers by last name: {}", lastName, e);
            throw new RuntimeException("Failed to search customers by last name: " + e.getMessage(), e);
        }
    }

    /**
     * Searches customers by FICO credit score range for screening operations.
     * This method supports customer screening and credit evaluation processes by filtering
     * customers based on credit score criteria with proper range validation.
     * 
     * Replaces COBOL operations:
     * - CUSTDAT sequential read with credit score filtering
     * - Customer qualification for credit products
     * 
     * @param minScore Minimum FICO credit score (300-850)
     * @param maxScore Maximum FICO credit score (300-850)
     * @return List of customers within the specified credit score range
     * @throws IllegalArgumentException if score range is invalid
     */
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public List<Customer> searchCustomersByFicoCreditScore(Integer minScore, Integer maxScore) {
        logger.debug("Searching customers by FICO credit score range: {} - {}", minScore, maxScore);
        
        // Validate FICO credit score range
        ValidationResult minValidation = ValidationUtils.validateFicoCreditScore(minScore);
        ValidationResult maxValidation = ValidationUtils.validateFicoCreditScore(maxScore);
        
        if (!minValidation.isValid()) {
            logger.warn("Invalid minimum FICO score: {} - {}", minScore, minValidation.getErrorMessage());
            throw new IllegalArgumentException("Invalid minimum FICO score: " + minValidation.getErrorMessage());
        }
        
        if (!maxValidation.isValid()) {
            logger.warn("Invalid maximum FICO score: {} - {}", maxScore, maxValidation.getErrorMessage());
            throw new IllegalArgumentException("Invalid maximum FICO score: " + maxValidation.getErrorMessage());
        }
        
        if (minScore > maxScore) {
            logger.warn("Invalid FICO score range: min {} > max {}", minScore, maxScore);
            throw new IllegalArgumentException("Minimum FICO score cannot be greater than maximum FICO score");
        }
        
        try {
            // Search customers by FICO credit score range
            List<Customer> customers = customerRepository.findByFicoCreditScoreBetween(minScore, maxScore);
            
            logger.info("Found {} customers with FICO scores between {} and {}", 
                customers.size(), minScore, maxScore);
            return customers;
            
        } catch (Exception e) {
            logger.error("Error searching customers by FICO credit score range: {} - {}", 
                minScore, maxScore, e);
            throw new RuntimeException("Failed to search customers by FICO credit score: " + e.getMessage(), e);
        }
    }

    /**
     * Validates customer data including FICO credit score and personal information.
     * This method provides comprehensive customer data validation with field-level
     * validation equivalent to COBOL field validation patterns.
     * 
     * Replaces COBOL operations:
     * - Customer field validation with 88-level conditions
     * - FICO credit score range validation
     * - Personal information format validation
     * 
     * @param customer Customer object to validate
     * @return ValidationResult indicating validation outcome
     * @throws IllegalArgumentException if customer is null
     */
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public ValidationResult validateCustomer(Customer customer) {
        logger.debug("Validating customer data");
        
        if (customer == null) {
            logger.warn("Customer validation failed: null customer object");
            throw new IllegalArgumentException("Customer cannot be null");
        }
        
        try {
            // Validate customer ID
            ValidationResult customerIdValidation = ValidationUtils.validateCustomerId(customer.getCustomerId());
            if (!customerIdValidation.isValid()) {
                logger.warn("Customer ID validation failed: {}", customerIdValidation.getErrorMessage());
                return customerIdValidation;
            }
            
            // Validate FICO credit score
            ValidationResult ficoValidation = ValidationUtils.validateFicoCreditScore(customer.getFicoCreditScore());
            if (!ficoValidation.isValid()) {
                logger.warn("FICO credit score validation failed: {}", ficoValidation.getErrorMessage());
                return ficoValidation;
            }
            
            // Validate required fields
            ValidationResult firstNameValidation = ValidationUtils.validateRequiredField(customer.getFirstName());
            if (!firstNameValidation.isValid()) {
                logger.warn("First name validation failed: {}", firstNameValidation.getErrorMessage());
                return firstNameValidation;
            }
            
            ValidationResult lastNameValidation = ValidationUtils.validateRequiredField(customer.getLastName());
            if (!lastNameValidation.isValid()) {
                logger.warn("Last name validation failed: {}", lastNameValidation.getErrorMessage());
                return lastNameValidation;
            }
            
            ValidationResult addressValidation = ValidationUtils.validateRequiredField(customer.getAddressLine1());
            if (!addressValidation.isValid()) {
                logger.warn("Address validation failed: {}", addressValidation.getErrorMessage());
                return addressValidation;
            }
            
            // Validate date of birth
            if (customer.getDateOfBirth() != null) {
                // Date of birth must be in the past
                if (customer.getDateOfBirth().isAfter(java.time.LocalDate.now())) {
                    logger.warn("Date of birth validation failed: future date");
                    return ValidationResult.INVALID_DATE;
                }
            } else {
                logger.warn("Date of birth validation failed: null value");
                return ValidationResult.BLANK_FIELD;
            }
            
            logger.debug("Customer validation successful");
            return ValidationResult.VALID;
            
        } catch (Exception e) {
            logger.error("Error validating customer data", e);
            throw new RuntimeException("Failed to validate customer: " + e.getMessage(), e);
        }
    }

    /**
     * Validates customer ID format and range.
     * This method provides customer ID validation equivalent to COBOL customer ID validation
     * with proper format checking and range validation.
     * 
     * Replaces COBOL operations:
     * - Customer ID PIC 9(09) format validation
     * - Customer ID range validation
     * 
     * @param customerId 9-digit customer identifier
     * @return ValidationResult indicating validation outcome
     */
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public ValidationResult validateCustomerId(String customerId) {
        logger.debug("Validating customer ID: {}", customerId);
        
        try {
            ValidationResult result = ValidationUtils.validateCustomerId(customerId);
            
            if (result.isValid()) {
                logger.debug("Customer ID validation successful: {}", customerId);
            } else {
                logger.warn("Customer ID validation failed: {} - {}", customerId, result.getErrorMessage());
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error validating customer ID: {}", customerId, e);
            throw new RuntimeException("Failed to validate customer ID: " + e.getMessage(), e);
        }
    }

    /**
     * Validates FICO credit score range (300-850).
     * This method provides FICO credit score validation equivalent to COBOL FICO score validation
     * with proper range checking and business rule enforcement.
     * 
     * Replaces COBOL operations:
     * - FICO credit score PIC 9(03) format validation
     * - FICO credit score range validation (300-850)
     * 
     * @param ficoCreditScore FICO credit score to validate
     * @return ValidationResult indicating validation outcome
     */
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public ValidationResult validateFicoCreditScore(Integer ficoCreditScore) {
        logger.debug("Validating FICO credit score: {}", ficoCreditScore);
        
        try {
            ValidationResult result = ValidationUtils.validateFicoCreditScore(ficoCreditScore);
            
            if (result.isValid()) {
                logger.debug("FICO credit score validation successful: {}", ficoCreditScore);
            } else {
                logger.warn("FICO credit score validation failed: {} - {}", ficoCreditScore, result.getErrorMessage());
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error validating FICO credit score: {}", ficoCreditScore, e);
            throw new RuntimeException("Failed to validate FICO credit score: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves customer profile with comprehensive customer information.
     * This method provides complete customer profile access with proper field validation
     * and audit logging for customer service operations.
     * 
     * Replaces COBOL operations:
     * - Complete customer profile display
     * - Customer information retrieval for service operations
     * 
     * @param customerId 9-digit customer identifier
     * @return Optional containing complete Customer profile, or empty if not found
     * @throws IllegalArgumentException if customer ID is null or invalid format
     */
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public Optional<Customer> getCustomerProfile(String customerId) {
        logger.debug("Retrieving customer profile: {}", customerId);
        
        // Validate customer ID format
        ValidationResult validationResult = ValidationUtils.validateCustomerId(customerId);
        if (!validationResult.isValid()) {
            logger.warn("Invalid customer ID format for profile retrieval: {} - {}", 
                customerId, validationResult.getErrorMessage());
            throw new IllegalArgumentException("Invalid customer ID format: " + validationResult.getErrorMessage());
        }
        
        try {
            // Retrieve customer profile with full information
            Optional<Customer> customerOptional = customerRepository.findById(customerId);
            
            if (customerOptional.isPresent()) {
                logger.info("Customer profile retrieved successfully: {}", customerId);
                return customerOptional;
            } else {
                logger.warn("Customer profile not found: {}", customerId);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving customer profile: {}", customerId, e);
            throw new RuntimeException("Failed to retrieve customer profile: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves customer accounts with account relationship information.
     * This method provides customer account portfolio information supporting
     * customer-account relationship operations and account cross-references.
     * 
     * Replaces COBOL operations:
     * - Customer account cross-reference queries
     * - Account portfolio display for customer service
     * 
     * @param customerId 9-digit customer identifier
     * @return Set of Account objects associated with the customer
     * @throws IllegalArgumentException if customer ID is null or invalid format
     */
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public Set<Account> getCustomerAccounts(String customerId) {
        logger.debug("Retrieving customer accounts: {}", customerId);
        
        // Validate customer ID format
        ValidationResult validationResult = ValidationUtils.validateCustomerId(customerId);
        if (!validationResult.isValid()) {
            logger.warn("Invalid customer ID format for account retrieval: {} - {}", 
                customerId, validationResult.getErrorMessage());
            throw new IllegalArgumentException("Invalid customer ID format: " + validationResult.getErrorMessage());
        }
        
        try {
            // Retrieve customer with accounts
            Optional<Customer> customerOptional = customerRepository.findByCustomerIdWithAccounts(customerId);
            
            if (customerOptional.isPresent()) {
                Set<Account> accounts = customerOptional.get().getAccounts();
                logger.info("Retrieved {} accounts for customer: {}", accounts.size(), customerId);
                return accounts;
            } else {
                logger.warn("Customer not found for account retrieval: {}", customerId);
                return new HashSet<>();
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving customer accounts: {}", customerId, e);
            throw new RuntimeException("Failed to retrieve customer accounts: " + e.getMessage(), e);
        }
    }

    /**
     * Validates comprehensive customer data including all required fields and business rules.
     * This method provides complete customer data validation equivalent to COBOL validation
     * patterns with comprehensive field validation and error reporting.
     * 
     * Replaces COBOL operations:
     * - Complete customer record validation
     * - All field validation with 88-level conditions
     * - Business rule validation and enforcement
     * 
     * @param customer Customer object to validate
     * @return ValidationResult indicating comprehensive validation outcome
     * @throws IllegalArgumentException if customer is null
     */
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public ValidationResult validateCustomerData(Customer customer) {
        logger.debug("Validating comprehensive customer data");
        
        if (customer == null) {
            logger.warn("Customer data validation failed: null customer object");
            throw new IllegalArgumentException("Customer cannot be null");
        }
        
        try {
            // Perform comprehensive validation using validateCustomer
            ValidationResult basicValidation = validateCustomer(customer);
            if (!basicValidation.isValid()) {
                return basicValidation;
            }
            
            // Additional validation for comprehensive data check
            if (customer.getSsn() != null) {
                ValidationResult ssnValidation = ValidationUtils.validateSSN(customer.getSsn());
                if (!ssnValidation.isValid()) {
                    logger.warn("SSN validation failed: {}", ssnValidation.getErrorMessage());
                    return ssnValidation;
                }
            }
            
            if (customer.getPhoneNumber1() != null) {
                ValidationResult phoneValidation = ValidationUtils.validatePhoneNumber(customer.getPhoneNumber1());
                if (!phoneValidation.isValid()) {
                    logger.warn("Phone number validation failed: {}", phoneValidation.getErrorMessage());
                    return phoneValidation;
                }
            }
            
            if (customer.getZipCode() != null) {
                ValidationResult zipValidation = ValidationUtils.validateZipCode(customer.getZipCode());
                if (!zipValidation.isValid()) {
                    logger.warn("ZIP code validation failed: {}", zipValidation.getErrorMessage());
                    return zipValidation;
                }
            }
            
            if (customer.getStateCode() != null) {
                ValidationResult stateValidation = ValidationUtils.validateStateCode(customer.getStateCode());
                if (!stateValidation.isValid()) {
                    logger.warn("State code validation failed: {}", stateValidation.getErrorMessage());
                    return stateValidation;
                }
            }
            
            logger.debug("Comprehensive customer data validation successful");
            return ValidationResult.VALID;
            
        } catch (Exception e) {
            logger.error("Error validating comprehensive customer data", e);
            throw new RuntimeException("Failed to validate customer data: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if customer exists by customer ID.
     * This method provides customer existence validation equivalent to COBOL file status checks,
     * supporting business logic that needs to verify customer existence before processing.
     * 
     * Replaces COBOL operations:
     * - CUSTDAT record existence check
     * - Customer existence validation for business operations
     * 
     * @param customerId 9-digit customer identifier
     * @return true if customer exists, false otherwise
     * @throws IllegalArgumentException if customer ID is null or invalid format
     */
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public boolean existsByCustomerId(String customerId) {
        logger.debug("Checking customer existence: {}", customerId);
        
        // Validate customer ID format
        ValidationResult validationResult = ValidationUtils.validateCustomerId(customerId);
        if (!validationResult.isValid()) {
            logger.warn("Invalid customer ID format for existence check: {} - {}", 
                customerId, validationResult.getErrorMessage());
            throw new IllegalArgumentException("Invalid customer ID format: " + validationResult.getErrorMessage());
        }
        
        try {
            // Check customer existence
            boolean exists = customerRepository.existsById(customerId);
            
            logger.debug("Customer existence check result: {} - {}", customerId, exists);
            return exists;
            
        } catch (Exception e) {
            logger.error("Error checking customer existence: {}", customerId, e);
            throw new RuntimeException("Failed to check customer existence: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves paginated list of customers with optional search criteria.
     * This method provides comprehensive customer browsing functionality with pagination support
     * equivalent to COBOL sequential file processing with record counting and position tracking.
     * 
     * Replaces COBOL operations:
     * - Sequential CUSTDAT file browsing with record counting
     * - Customer listing with pagination for large datasets
     * 
     * @param pageable Pagination and sorting parameters
     * @return Page of customers with pagination metadata
     * @throws IllegalArgumentException if pageable is null
     */
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public Page<Customer> findAllCustomers(Pageable pageable) {
        logger.debug("Retrieving paginated customers: page {}, size {}", 
            pageable.getPageNumber(), pageable.getPageSize());
        
        if (pageable == null) {
            logger.warn("Pageable parameter is null, using default pagination");
            pageable = PageRequest.of(0, DEFAULT_PAGE_SIZE, Sort.by("lastName", "firstName"));
        }
        
        // Validate page size
        int pageSize = pageable.getPageSize();
        if (pageSize > MAX_PAGE_SIZE) {
            logger.warn("Page size {} exceeds maximum {}, using default", pageSize, MAX_PAGE_SIZE);
            pageable = PageRequest.of(pageable.getPageNumber(), DEFAULT_PAGE_SIZE, pageable.getSort());
        }
        
        try {
            // Retrieve paginated customers
            Page<Customer> customers = customerRepository.findAllCustomersPaginated(pageable);
            
            logger.info("Retrieved {} customers (page {} of {}, total {})", 
                customers.getNumberOfElements(), customers.getNumber() + 1, 
                customers.getTotalPages(), customers.getTotalElements());
            
            return customers;
            
        } catch (Exception e) {
            logger.error("Error retrieving paginated customers", e);
            throw new RuntimeException("Failed to retrieve customers: " + e.getMessage(), e);
        }
    }

    /**
     * Searches customers by multiple criteria with flexible matching.
     * This method provides comprehensive customer search functionality supporting multiple
     * search criteria with optional parameters for flexible customer lookup operations.
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
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public Page<Customer> searchCustomersByCriteria(String lastName, String stateCode, String zipCode,
                                                    Integer minCreditScore, Integer maxCreditScore,
                                                    Pageable pageable) {
        logger.debug("Searching customers by criteria: lastName={}, stateCode={}, zipCode={}, " +
                    "minScore={}, maxScore={}", lastName, stateCode, zipCode, minCreditScore, maxCreditScore);
        
        if (pageable == null) {
            pageable = PageRequest.of(0, DEFAULT_PAGE_SIZE, Sort.by("lastName", "firstName"));
        }
        
        // Validate credit score parameters if provided
        if (minCreditScore != null) {
            ValidationResult minValidation = ValidationUtils.validateFicoCreditScore(minCreditScore);
            if (!minValidation.isValid()) {
                throw new IllegalArgumentException("Invalid minimum credit score: " + minValidation.getErrorMessage());
            }
        }
        
        if (maxCreditScore != null) {
            ValidationResult maxValidation = ValidationUtils.validateFicoCreditScore(maxCreditScore);
            if (!maxValidation.isValid()) {
                throw new IllegalArgumentException("Invalid maximum credit score: " + maxValidation.getErrorMessage());
            }
        }
        
        if (minCreditScore != null && maxCreditScore != null && minCreditScore > maxCreditScore) {
            throw new IllegalArgumentException("Minimum credit score cannot be greater than maximum credit score");
        }
        
        try {
            // Search customers with multiple criteria
            Page<Customer> customers = customerRepository.searchCustomersByCriteria(
                lastName, stateCode, zipCode, minCreditScore, maxCreditScore, pageable);
            
            logger.info("Found {} customers matching criteria (page {} of {}, total {})", 
                customers.getNumberOfElements(), customers.getNumber() + 1, 
                customers.getTotalPages(), customers.getTotalElements());
            
            return customers;
            
        } catch (Exception e) {
            logger.error("Error searching customers by criteria", e);
            throw new RuntimeException("Failed to search customers: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves customers by state code with pagination support.
     * This method supports customer geographic filtering and regional operations
     * by filtering customers based on their address state code.
     * 
     * Replaces COBOL operations:
     * - CUSTDAT sequential read with state code filtering
     * - Regional customer analysis and reporting
     * 
     * @param stateCode 2-character state code
     * @param pageable Pagination and sorting parameters
     * @return Page of customers in the specified state
     * @throws IllegalArgumentException if stateCode is invalid
     */
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public Page<Customer> findCustomersByStateCode(String stateCode, Pageable pageable) {
        logger.debug("Finding customers by state code: {}", stateCode);
        
        // Validate state code
        ValidationResult stateValidation = ValidationUtils.validateStateCode(stateCode);
        if (!stateValidation.isValid()) {
            logger.warn("Invalid state code: {} - {}", stateCode, stateValidation.getErrorMessage());
            throw new IllegalArgumentException("Invalid state code: " + stateValidation.getErrorMessage());
        }
        
        if (pageable == null) {
            pageable = PageRequest.of(0, DEFAULT_PAGE_SIZE, Sort.by("lastName", "firstName"));
        }
        
        try {
            // Find customers by state code
            Page<Customer> customers = customerRepository.findByStateCodePaginated(stateCode, pageable);
            
            logger.info("Found {} customers in state {} (page {} of {}, total {})", 
                customers.getNumberOfElements(), stateCode, customers.getNumber() + 1, 
                customers.getTotalPages(), customers.getTotalElements());
            
            return customers;
            
        } catch (Exception e) {
            logger.error("Error finding customers by state code: {}", stateCode, e);
            throw new RuntimeException("Failed to find customers by state code: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves customers by ZIP code for address-based filtering.
     * This method supports customer geographic analysis and address validation
     * by filtering customers based on their ZIP code.
     * 
     * Replaces COBOL operations:
     * - CUSTDAT sequential read with ZIP code matching
     * - Address-based customer grouping and analysis
     * 
     * @param zipCode ZIP code to search for
     * @return List of customers with matching ZIP code
     * @throws IllegalArgumentException if zipCode is invalid
     */
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public List<Customer> findCustomersByZipCode(String zipCode) {
        logger.debug("Finding customers by ZIP code: {}", zipCode);
        
        // Validate ZIP code
        ValidationResult zipValidation = ValidationUtils.validateZipCode(zipCode);
        if (!zipValidation.isValid()) {
            logger.warn("Invalid ZIP code: {} - {}", zipCode, zipValidation.getErrorMessage());
            throw new IllegalArgumentException("Invalid ZIP code: " + zipValidation.getErrorMessage());
        }
        
        try {
            // Find customers by ZIP code
            List<Customer> customers = customerRepository.findByZipCode(zipCode);
            
            logger.info("Found {} customers with ZIP code: {}", customers.size(), zipCode);
            return customers;
            
        } catch (Exception e) {
            logger.error("Error finding customers by ZIP code: {}", zipCode, e);
            throw new RuntimeException("Failed to find customers by ZIP code: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves customers by first name and last name combination.
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
     * @throws IllegalArgumentException if firstName or lastName is invalid
     */
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public List<Customer> findCustomersByName(String firstName, String lastName) {
        logger.debug("Finding customers by name: {} {}", firstName, lastName);
        
        // Validate first name
        ValidationResult firstNameValidation = ValidationUtils.validateRequiredField(firstName);
        if (!firstNameValidation.isValid()) {
            logger.warn("Invalid first name: {} - {}", firstName, firstNameValidation.getErrorMessage());
            throw new IllegalArgumentException("Invalid first name: " + firstNameValidation.getErrorMessage());
        }
        
        // Validate last name
        ValidationResult lastNameValidation = ValidationUtils.validateRequiredField(lastName);
        if (!lastNameValidation.isValid()) {
            logger.warn("Invalid last name: {} - {}", lastName, lastNameValidation.getErrorMessage());
            throw new IllegalArgumentException("Invalid last name: " + lastNameValidation.getErrorMessage());
        }
        
        try {
            // Find customers by first and last name
            List<Customer> customers = customerRepository.findByFirstNameAndLastName(firstName.trim(), lastName.trim());
            
            logger.info("Found {} customers with name: {} {}", customers.size(), firstName, lastName);
            return customers;
            
        } catch (Exception e) {
            logger.error("Error finding customers by name: {} {}", firstName, lastName, e);
            throw new RuntimeException("Failed to find customers by name: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves customers by age range calculated from date of birth.
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
     * @throws IllegalArgumentException if age range is invalid
     */
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public List<Customer> findCustomersByAgeRange(Integer minAge, Integer maxAge) {
        logger.debug("Finding customers by age range: {} - {}", minAge, maxAge);
        
        // Validate age parameters
        if (minAge == null || maxAge == null) {
            throw new IllegalArgumentException("Age range parameters cannot be null");
        }
        
        if (minAge < 0 || maxAge < 0) {
            throw new IllegalArgumentException("Age values must be non-negative");
        }
        
        if (minAge > maxAge) {
            throw new IllegalArgumentException("Minimum age cannot be greater than maximum age");
        }
        
        if (maxAge > 150) {
            throw new IllegalArgumentException("Maximum age cannot exceed 150 years");
        }
        
        try {
            // Find customers by age range
            List<Customer> customers = customerRepository.findByAgeRange(minAge, maxAge);
            
            logger.info("Found {} customers with age range: {} - {}", customers.size(), minAge, maxAge);
            return customers;
            
        } catch (Exception e) {
            logger.error("Error finding customers by age range: {} - {}", minAge, maxAge, e);
            throw new RuntimeException("Failed to find customers by age range: " + e.getMessage(), e);
        }
    }

    /**
     * Counts customers by FICO credit score range for statistical analysis.
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
     * @throws IllegalArgumentException if score range is invalid
     */
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public long countCustomersByFicoCreditScore(Integer minScore, Integer maxScore) {
        logger.debug("Counting customers by FICO credit score range: {} - {}", minScore, maxScore);
        
        // Validate FICO credit score range
        ValidationResult minValidation = ValidationUtils.validateFicoCreditScore(minScore);
        ValidationResult maxValidation = ValidationUtils.validateFicoCreditScore(maxScore);
        
        if (!minValidation.isValid()) {
            throw new IllegalArgumentException("Invalid minimum FICO score: " + minValidation.getErrorMessage());
        }
        
        if (!maxValidation.isValid()) {
            throw new IllegalArgumentException("Invalid maximum FICO score: " + maxValidation.getErrorMessage());
        }
        
        if (minScore > maxScore) {
            throw new IllegalArgumentException("Minimum FICO score cannot be greater than maximum FICO score");
        }
        
        try {
            // Count customers by FICO credit score range
            long count = customerRepository.countByFicoCreditScoreBetween(minScore, maxScore);
            
            logger.info("Found {} customers with FICO scores between {} and {}", count, minScore, maxScore);
            return count;
            
        } catch (Exception e) {
            logger.error("Error counting customers by FICO credit score range: {} - {}", minScore, maxScore, e);
            throw new RuntimeException("Failed to count customers by FICO credit score: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves customers with active accounts for portfolio analysis.
     * This method supports customer-account relationship analysis by finding customers
     * who have active accounts, supporting portfolio management operations.
     * 
     * Replaces COBOL operations:
     * - CUSTDAT and ACCTDAT cross-reference queries
     * - Customer-account relationship analysis
     * 
     * @param pageable Pagination and sorting parameters
     * @return Page of customers with active accounts
     */
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public Page<Customer> findCustomersWithActiveAccounts(Pageable pageable) {
        logger.debug("Finding customers with active accounts");
        
        if (pageable == null) {
            pageable = PageRequest.of(0, DEFAULT_PAGE_SIZE, Sort.by("lastName", "firstName"));
        }
        
        try {
            // Find customers with active accounts
            Page<Customer> customers = customerRepository.findCustomersWithAccountStatus("Y", pageable);
            
            logger.info("Found {} customers with active accounts (page {} of {}, total {})", 
                customers.getNumberOfElements(), customers.getNumber() + 1, 
                customers.getTotalPages(), customers.getTotalElements());
            
            return customers;
            
        } catch (Exception e) {
            logger.error("Error finding customers with active accounts", e);
            throw new RuntimeException("Failed to find customers with active accounts: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves customer with full PII access for authorized operations.
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
     * @throws IllegalArgumentException if customer ID is invalid
     */
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public Optional<Customer> getCustomerWithFullPIIAccess(String customerId) {
        logger.debug("Retrieving customer with full PII access: {}", customerId);
        
        // Validate customer ID format
        ValidationResult validationResult = ValidationUtils.validateCustomerId(customerId);
        if (!validationResult.isValid()) {
            logger.warn("Invalid customer ID format for full PII access: {} - {}", 
                customerId, validationResult.getErrorMessage());
            throw new IllegalArgumentException("Invalid customer ID format: " + validationResult.getErrorMessage());
        }
        
        try {
            // Log PII access for audit trail
            logger.info("Full PII access requested for customer: {}", customerId);
            
            // Retrieve customer with full PII access
            Optional<Customer> customerOptional = customerRepository.findByCustomerIdWithFullPIIAccess(customerId);
            
            if (customerOptional.isPresent()) {
                logger.info("Customer with full PII access retrieved successfully: {}", customerId);
                return customerOptional;
            } else {
                logger.warn("Customer not found for full PII access: {}", customerId);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving customer with full PII access: {}", customerId, e);
            throw new RuntimeException("Failed to retrieve customer with full PII access: " + e.getMessage(), e);
        }
    }
}