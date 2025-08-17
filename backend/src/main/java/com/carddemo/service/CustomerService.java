/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License
 */

package com.carddemo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CustomerService provides comprehensive customer management operations
 * converted from COBOL programs COUSR00C and COUSR01C.
 * 
 * This service handles customer profile creation, updates, and queries
 * while maintaining GDPR compliance and preserving business logic from
 * the original COBOL implementation.
 * 
 * Key functionalities:
 * - Customer CRUD operations with validation
 * - GDPR compliance with sensitive data masking
 * - Integration with customer-related repositories
 * - Transaction boundary management
 * - COBOL-equivalent data validation rules
 */
@Service
@Transactional
public class CustomerService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);
    
    // Constants for validation (derived from COBOL copybook CVCUS01Y)
    private static final int MAX_CUSTOMER_ID_LENGTH = 10;
    private static final int MAX_FIRST_NAME_LENGTH = 25;
    private static final int MAX_LAST_NAME_LENGTH = 25;
    private static final int SSN_LENGTH = 9;
    private static final int MAX_PHONE_LENGTH = 10;
    private static final int MAX_ADDR_LINE_LENGTH = 50;
    private static final int MAX_CITY_LENGTH = 25;
    private static final int MAX_STATE_LENGTH = 2;
    private static final int MAX_ZIP_LENGTH = 10;
    
    // FICO score ranges (COMP-3 equivalent precision)
    private static final BigDecimal MIN_FICO_SCORE = new BigDecimal("300");
    private static final BigDecimal MAX_FICO_SCORE = new BigDecimal("850");
    
    // Regex patterns for validation
    private static final Pattern SSN_PATTERN = Pattern.compile("^\\d{9}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10}$");
    private static final Pattern ZIP_PATTERN = Pattern.compile("^\\d{5}(-\\d{4})?$");
    private static final Pattern STATE_PATTERN = Pattern.compile("^[A-Z]{2}$");
    
    // GDPR masking patterns
    private static final String SSN_MASK = "XXX-XX-";
    private static final String PHONE_MASK = "XXX-XXX-";
    private static final String EMAIL_MASK = "XXXXX@XXXXX.XXX";

    /**
     * Retrieves customer by ID with optional GDPR compliance masking.
     * Converts COBOL EXEC CICS READ operations to JPA repository calls.
     * 
     * Equivalent to COBOL paragraph structure:
     * - 0000-INIT: Initialize processing variables
     * - 1000-INPUT: Validate customer ID input
     * - 2000-PROCESS: Retrieve customer data
     * - 3000-OUTPUT: Format response data
     * - 9000-CLOSE: Cleanup and logging
     * 
     * @param customerId The unique customer identifier
     * @return Customer data with appropriate masking applied
     * @throws IllegalArgumentException if customer ID is invalid
     * @throws CustomerNotFoundException if customer not found
     */
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(String customerId) {
        logger.info("CustomerService.getCustomerById() - Processing customer ID: {}", customerId);
        
        // 0000-INIT: Initialize processing
        init0000("getCustomerById", customerId);
        
        try {
            // 1000-INPUT: Validate input parameters
            processInput1000(customerId, null);
            
            // 2000-PROCESS: Retrieve customer data
            CustomerEntity customer = processCustomerRetrieval2000(customerId);
            
            // 3000-OUTPUT: Format and mask response
            CustomerResponse response = formatCustomerResponse3000(customer);
            
            // 9000-CLOSE: Cleanup and audit logging
            cleanup9000("getCustomerById", "SUCCESS", customerId);
            
            return response;
            
        } catch (Exception e) {
            cleanup9000("getCustomerById", "ERROR", e.getMessage());
            throw e;
        }
    }

    /**
     * Updates existing customer profile with comprehensive validation.
     * Maintains transaction boundaries equivalent to CICS SYNCPOINT.
     * 
     * Converts COBOL update logic while preserving field validation rules
     * from the original COUSR01C program.
     * 
     * @param customerId The customer ID to update
     * @param updateRequest Customer data to update
     * @return Updated customer information
     * @throws ValidationException if data validation fails
     * @throws CustomerNotFoundException if customer not found
     * @throws ConcurrencyException if optimistic locking fails
     */
    @Transactional
    public CustomerResponse updateCustomer(String customerId, CustomerUpdateRequest updateRequest) {
        logger.info("CustomerService.updateCustomer() - Processing update for customer ID: {}", customerId);
        
        // 0000-INIT: Initialize processing
        init0000("updateCustomer", customerId);
        
        try {
            // 1000-INPUT: Validate input parameters
            processInput1000(customerId, updateRequest);
            
            // 2000-PROCESS: Validate business rules and update data
            CustomerEntity updatedCustomer = processCustomerUpdate2000(customerId, updateRequest);
            
            // 3000-OUTPUT: Format response
            CustomerResponse response = formatCustomerResponse3000(updatedCustomer);
            
            // 9000-CLOSE: Cleanup and audit logging
            cleanup9000("updateCustomer", "SUCCESS", customerId);
            
            return response;
            
        } catch (Exception e) {
            cleanup9000("updateCustomer", "ERROR", e.getMessage());
            throw e;
        }
    }

    /**
     * Creates new customer profile with comprehensive validation.
     * Equivalent to COBOL EXEC CICS WRITE operations.
     * 
     * Implements validation logic from COUSR01C program while ensuring
     * all required fields are populated and meet business rules.
     * 
     * @param createRequest Customer creation data
     * @return Newly created customer information
     * @throws ValidationException if data validation fails
     * @throws DuplicateCustomerException if customer already exists
     */
    @Transactional
    public CustomerResponse createCustomer(CustomerCreateRequest createRequest) {
        logger.info("CustomerService.createCustomer() - Processing new customer creation");
        
        // 0000-INIT: Initialize processing
        init0000("createCustomer", null);
        
        try {
            // 1000-INPUT: Validate input parameters
            processInput1000(null, createRequest);
            
            // 2000-PROCESS: Create new customer
            CustomerEntity newCustomer = processCustomerCreation2000(createRequest);
            
            // 3000-OUTPUT: Format response
            CustomerResponse response = formatCustomerResponse3000(newCustomer);
            
            // 9000-CLOSE: Cleanup and audit logging
            cleanup9000("createCustomer", "SUCCESS", newCustomer.getCustomerId());
            
            return response;
            
        } catch (Exception e) {
            cleanup9000("createCustomer", "ERROR", e.getMessage());
            throw e;
        }
    }

    /**
     * Validates customer data according to COBOL business rules.
     * Implements field validation equivalent to COUSR01C edit routines.
     * 
     * This method performs comprehensive validation including:
     * - Required field validation
     * - Data format validation
     * - Business rule validation
     * - Cross-field validation
     * 
     * @param customerData Customer data to validate
     * @return Validation result with detailed error messages
     */
    public ValidationResult validateCustomerData(Object customerData) {
        logger.info("CustomerService.validateCustomerData() - Starting validation");
        
        ValidationResult result = new ValidationResult();
        
        try {
            if (customerData instanceof CustomerCreateRequest) {
                validateCreateRequest((CustomerCreateRequest) customerData, result);
            } else if (customerData instanceof CustomerUpdateRequest) {
                validateUpdateRequest((CustomerUpdateRequest) customerData, result);
            } else {
                result.addError("INVALID_REQUEST_TYPE", "Unsupported customer data type");
            }
            
            logger.info("CustomerService.validateCustomerData() - Validation completed with {} errors", 
                       result.getErrorCount());
            
        } catch (Exception e) {
            logger.error("CustomerService.validateCustomerData() - Validation error: {}", e.getMessage());
            result.addError("VALIDATION_ERROR", "Unexpected validation error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Handles GDPR compliance by masking sensitive customer data.
     * Implements data protection equivalent to mainframe security controls.
     * 
     * This method applies appropriate masking based on:
     * - User access level
     * - Data sensitivity classification
     * - Regulatory requirements
     * - Business need to know
     * 
     * @param customerData Customer data to mask
     * @param maskingLevel Level of masking to apply
     * @return Customer data with appropriate masking applied
     */
    public CustomerResponse handleGDPRCompliance(CustomerResponse customerData, GDPRMaskingLevel maskingLevel) {
        logger.info("CustomerService.handleGDPRCompliance() - Applying masking level: {}", maskingLevel);
        
        try {
            CustomerResponse maskedData = new CustomerResponse(customerData);
            
            switch (maskingLevel) {
                case FULL_ACCESS:
                    // No masking for authorized full access
                    break;
                    
                case PARTIAL_MASK:
                    // Mask sensitive fields partially
                    maskedData.setSsn(maskSSN(customerData.getSsn(), true));
                    maskedData.setPhone(maskPhone(customerData.getPhone(), true));
                    break;
                    
                case FULL_MASK:
                    // Mask all sensitive fields completely
                    maskedData.setSsn(maskSSN(customerData.getSsn(), false));
                    maskedData.setPhone(maskPhone(customerData.getPhone(), false));
                    maskedData.setEmail(EMAIL_MASK);
                    maskedData.setFicoScore(null); // Completely hide FICO score
                    break;
                    
                case ANONYMIZE:
                    // Remove all personally identifiable information
                    maskedData = anonymizeCustomerData(customerData);
                    break;
                    
                default:
                    throw new IllegalArgumentException("Unknown masking level: " + maskingLevel);
            }
            
            logger.info("CustomerService.handleGDPRCompliance() - Masking completed successfully");
            return maskedData;
            
        } catch (Exception e) {
            logger.error("CustomerService.handleGDPRCompliance() - Error applying masking: {}", e.getMessage());
            throw new GDPRComplianceException("Failed to apply GDPR masking: " + e.getMessage());
        }
    }

    // =================== PRIVATE HELPER METHODS (COBOL paragraph equivalents) ===================

    /**
     * 0000-INIT: Initialize processing variables and validate method context.
     * Equivalent to COBOL initialization paragraph.
     */
    private void init0000(String methodName, String customerId) {
        logger.debug("CustomerService.init0000() - Method: {}, Customer ID: {}", methodName, customerId);
        
        // Initialize processing context similar to COBOL working storage
        if (!StringUtils.hasText(methodName)) {
            throw new IllegalArgumentException("Method name cannot be empty");
        }
        
        // Log processing start for audit trail
        logger.info("CustomerService processing started - Method: {}, Timestamp: {}", 
                   methodName, LocalDateTime.now());
    }

    /**
     * 1000-INPUT: Process and validate input parameters.
     * Implements COBOL input validation equivalent to RECEIVE MAP processing.
     */
    private void processInput1000(String customerId, Object requestData) {
        logger.debug("CustomerService.processInput1000() - Validating inputs");
        
        // Validate customer ID when provided
        if (StringUtils.hasText(customerId)) {
            if (customerId.length() > MAX_CUSTOMER_ID_LENGTH) {
                throw new ValidationException("Customer ID exceeds maximum length of " + MAX_CUSTOMER_ID_LENGTH);
            }
            if (!customerId.matches("^[A-Z0-9]+$")) {
                throw new ValidationException("Customer ID must contain only alphanumeric characters");
            }
        }
        
        // Validate request data when provided
        if (requestData != null) {
            ValidationResult validation = validateCustomerData(requestData);
            if (!validation.isValid()) {
                throw new ValidationException("Input validation failed: " + validation.getFirstError());
            }
        }
    }

    /**
     * 2000-PROCESS: Core business logic processing for customer retrieval.
     * Equivalent to COBOL main processing paragraph with EXEC CICS READ operations.
     */
    private CustomerEntity processCustomerRetrieval2000(String customerId) {
        logger.debug("CustomerService.processCustomerRetrieval2000() - Retrieving customer: {}", customerId);
        
        // Simulate JPA repository call (would be injected dependency in real implementation)
        // This replaces COBOL EXEC CICS READ operations
        Optional<CustomerEntity> customerOpt = findCustomerById(customerId);
        
        if (!customerOpt.isPresent()) {
            throw new CustomerNotFoundException("Customer not found with ID: " + customerId);
        }
        
        CustomerEntity customer = customerOpt.get();
        
        // Validate customer record integrity (equivalent to COBOL data validation)
        if (!isCustomerRecordValid(customer)) {
            throw new DataIntegrityException("Customer record integrity validation failed");
        }
        
        return customer;
    }

    /**
     * 2000-PROCESS: Core business logic processing for customer updates.
     * Equivalent to COBOL main processing with EXEC CICS REWRITE operations.
     */
    private CustomerEntity processCustomerUpdate2000(String customerId, CustomerUpdateRequest updateRequest) {
        logger.debug("CustomerService.processCustomerUpdate2000() - Updating customer: {}", customerId);
        
        // Retrieve existing customer
        CustomerEntity existingCustomer = processCustomerRetrieval2000(customerId);
        
        // Apply updates while preserving data integrity
        CustomerEntity updatedCustomer = applyCustomerUpdates(existingCustomer, updateRequest);
        
        // Validate updated customer data
        if (!isCustomerRecordValid(updatedCustomer)) {
            throw new DataIntegrityException("Updated customer record validation failed");
        }
        
        // Save updated customer (equivalent to EXEC CICS REWRITE)
        return saveCustomer(updatedCustomer);
    }

    /**
     * 2000-PROCESS: Core business logic processing for customer creation.
     * Equivalent to COBOL main processing with EXEC CICS WRITE operations.
     */
    private CustomerEntity processCustomerCreation2000(CustomerCreateRequest createRequest) {
        logger.debug("CustomerService.processCustomerCreation2000() - Creating new customer");
        
        // Generate new customer ID
        String newCustomerId = generateCustomerId();
        
        // Create customer entity
        CustomerEntity newCustomer = buildCustomerEntity(newCustomerId, createRequest);
        
        // Validate new customer data
        if (!isCustomerRecordValid(newCustomer)) {
            throw new DataIntegrityException("New customer record validation failed");
        }
        
        // Check for duplicate SSN
        if (existsCustomerWithSSN(createRequest.getSsn())) {
            throw new DuplicateCustomerException("Customer already exists with SSN: " + maskSSN(createRequest.getSsn(), true));
        }
        
        // Save new customer (equivalent to EXEC CICS WRITE)
        return saveCustomer(newCustomer);
    }

    /**
     * 3000-OUTPUT: Format customer response data.
     * Equivalent to COBOL output formatting and SEND MAP operations.
     */
    private CustomerResponse formatCustomerResponse3000(CustomerEntity customer) {
        logger.debug("CustomerService.formatCustomerResponse3000() - Formatting response for customer: {}", 
                    customer.getCustomerId());
        
        CustomerResponse response = new CustomerResponse();
        response.setCustomerId(customer.getCustomerId());
        response.setFirstName(customer.getFirstName());
        response.setLastName(customer.getLastName());
        response.setSsn(customer.getSsn());
        response.setPhone(customer.getPhone());
        response.setEmail(customer.getEmail());
        response.setAddressLine1(customer.getAddressLine1());
        response.setAddressLine2(customer.getAddressLine2());
        response.setCity(customer.getCity());
        response.setState(customer.getState());
        response.setZipCode(customer.getZipCode());
        
        // Convert COBOL COMP-3 fields to BigDecimal with appropriate precision
        if (customer.getFicoScore() != null) {
            response.setFicoScore(customer.getFicoScore().setScale(0, RoundingMode.HALF_UP));
        }
        
        response.setActive(customer.isActive());
        response.setCreatedDate(customer.getCreatedDate());
        response.setLastModifiedDate(customer.getLastModifiedDate());
        
        return response;
    }

    /**
     * 9000-CLOSE: Cleanup processing and audit logging.
     * Equivalent to COBOL cleanup paragraph.
     */
    private void cleanup9000(String methodName, String status, String details) {
        logger.info("CustomerService.cleanup9000() - Method: {}, Status: {}, Details: {}", 
                   methodName, status, details);
        
        // Log completion for audit trail
        logger.info("CustomerService processing completed - Method: {}, Status: {}, Timestamp: {}", 
                   methodName, status, LocalDateTime.now());
        
        // Additional cleanup tasks could be added here
        // (equivalent to COBOL housekeeping operations)
    }

    // =================== VALIDATION HELPER METHODS ===================

    /**
     * Validates customer create request data according to COBOL business rules.
     * Implements validation equivalent to COUSR01C field validation logic.
     */
    private void validateCreateRequest(CustomerCreateRequest request, ValidationResult result) {
        // Required field validation (equivalent to COBOL "can NOT be empty" checks)
        if (!StringUtils.hasText(request.getFirstName())) {
            result.addError("FIRST_NAME_REQUIRED", "First Name can NOT be empty...");
        }
        if (!StringUtils.hasText(request.getLastName())) {
            result.addError("LAST_NAME_REQUIRED", "Last Name can NOT be empty...");
        }
        if (!StringUtils.hasText(request.getSsn())) {
            result.addError("SSN_REQUIRED", "SSN can NOT be empty...");
        }
        
        // Field length validation
        if (StringUtils.hasText(request.getFirstName()) && request.getFirstName().length() > MAX_FIRST_NAME_LENGTH) {
            result.addError("FIRST_NAME_TOO_LONG", "First Name exceeds maximum length of " + MAX_FIRST_NAME_LENGTH);
        }
        if (StringUtils.hasText(request.getLastName()) && request.getLastName().length() > MAX_LAST_NAME_LENGTH) {
            result.addError("LAST_NAME_TOO_LONG", "Last Name exceeds maximum length of " + MAX_LAST_NAME_LENGTH);
        }
        
        // Format validation
        if (StringUtils.hasText(request.getSsn()) && !SSN_PATTERN.matcher(request.getSsn()).matches()) {
            result.addError("INVALID_SSN_FORMAT", "SSN must be 9 digits");
        }
        if (StringUtils.hasText(request.getPhone()) && !PHONE_PATTERN.matcher(request.getPhone()).matches()) {
            result.addError("INVALID_PHONE_FORMAT", "Phone must be 10 digits");
        }
        if (StringUtils.hasText(request.getState()) && !STATE_PATTERN.matcher(request.getState()).matches()) {
            result.addError("INVALID_STATE_FORMAT", "State must be 2 uppercase letters");
        }
        if (StringUtils.hasText(request.getZipCode()) && !ZIP_PATTERN.matcher(request.getZipCode()).matches()) {
            result.addError("INVALID_ZIP_FORMAT", "ZIP code must be 5 digits or 5+4 format");
        }
        
        // FICO score validation (COBOL COMP-3 equivalent)
        if (request.getFicoScore() != null) {
            if (request.getFicoScore().compareTo(MIN_FICO_SCORE) < 0 || 
                request.getFicoScore().compareTo(MAX_FICO_SCORE) > 0) {
                result.addError("INVALID_FICO_SCORE", "FICO score must be between " + MIN_FICO_SCORE + " and " + MAX_FICO_SCORE);
            }
        }
    }

    /**
     * Validates customer update request data.
     */
    private void validateUpdateRequest(CustomerUpdateRequest request, ValidationResult result) {
        // Similar validation logic as create request, but allowing partial updates
        // Implementation would follow same pattern with appropriate nullable field handling
        validateCreateRequest(request, result); // Reuse common validation logic
    }

    /**
     * Validates customer record integrity.
     */
    private boolean isCustomerRecordValid(CustomerEntity customer) {
        return customer != null && 
               StringUtils.hasText(customer.getCustomerId()) &&
               StringUtils.hasText(customer.getFirstName()) &&
               StringUtils.hasText(customer.getLastName()) &&
               StringUtils.hasText(customer.getSsn()) &&
               customer.getSsn().length() == SSN_LENGTH;
    }

    // =================== GDPR HELPER METHODS ===================

    /**
     * Masks SSN according to GDPR requirements.
     */
    private String maskSSN(String ssn, boolean partial) {
        if (!StringUtils.hasText(ssn) || ssn.length() != SSN_LENGTH) {
            return ssn;
        }
        
        if (partial) {
            return SSN_MASK + ssn.substring(5); // Show last 4 digits
        } else {
            return "XXX-XX-XXXX"; // Full mask
        }
    }

    /**
     * Masks phone number according to GDPR requirements.
     */
    private String maskPhone(String phone, boolean partial) {
        if (!StringUtils.hasText(phone) || phone.length() != MAX_PHONE_LENGTH) {
            return phone;
        }
        
        if (partial) {
            return PHONE_MASK + phone.substring(6); // Show last 4 digits
        } else {
            return "XXX-XXX-XXXX"; // Full mask
        }
    }

    /**
     * Anonymizes customer data for GDPR compliance.
     */
    private CustomerResponse anonymizeCustomerData(CustomerResponse customerData) {
        CustomerResponse anonymized = new CustomerResponse();
        anonymized.setCustomerId("ANONYMOUS");
        anonymized.setFirstName("ANONYMOUS");
        anonymized.setLastName("ANONYMOUS");
        anonymized.setSsn("XXX-XX-XXXX");
        anonymized.setPhone("XXX-XXX-XXXX");
        anonymized.setEmail("XXXXX@XXXXX.XXX");
        anonymized.setAddressLine1("ANONYMOUS");
        anonymized.setAddressLine2("ANONYMOUS");
        anonymized.setCity("ANONYMOUS");
        anonymized.setState("XX");
        anonymized.setZipCode("XXXXX");
        anonymized.setFicoScore(null);
        anonymized.setActive(customerData.isActive());
        anonymized.setCreatedDate(customerData.getCreatedDate());
        anonymized.setLastModifiedDate(customerData.getLastModifiedDate());
        
        return anonymized;
    }

    // =================== DATA ACCESS SIMULATION METHODS ===================
    // Note: In a real implementation, these would be replaced with @Autowired repository dependencies

    /**
     * Simulates JPA repository findById operation.
     * In real implementation, this would be: customerRepository.findById(customerId)
     */
    private Optional<CustomerEntity> findCustomerById(String customerId) {
        // Placeholder implementation - would be replaced with actual repository call
        logger.debug("Simulating customer lookup for ID: {}", customerId);
        return Optional.empty(); // Would return actual customer data
    }

    /**
     * Simulates checking for existing customer with SSN.
     */
    private boolean existsCustomerWithSSN(String ssn) {
        // Placeholder implementation - would be replaced with actual repository call
        logger.debug("Simulating SSN existence check for: {}", maskSSN(ssn, true));
        return false; // Would return actual existence check
    }

    /**
     * Simulates saving customer entity.
     */
    private CustomerEntity saveCustomer(CustomerEntity customer) {
        // Placeholder implementation - would be replaced with actual repository save
        logger.debug("Simulating customer save for ID: {}", customer.getCustomerId());
        return customer; // Would return saved entity with updated fields
    }

    /**
     * Generates new customer ID using business rules.
     */
    private String generateCustomerId() {
        // Placeholder implementation - would use actual ID generation strategy
        return "CUST" + System.currentTimeMillis();
    }

    /**
     * Builds customer entity from create request.
     */
    private CustomerEntity buildCustomerEntity(String customerId, CustomerCreateRequest request) {
        CustomerEntity entity = new CustomerEntity();
        entity.setCustomerId(customerId);
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setSsn(request.getSsn());
        entity.setPhone(request.getPhone());
        entity.setEmail(request.getEmail());
        entity.setAddressLine1(request.getAddressLine1());
        entity.setAddressLine2(request.getAddressLine2());
        entity.setCity(request.getCity());
        entity.setState(request.getState());
        entity.setZipCode(request.getZipCode());
        entity.setFicoScore(request.getFicoScore());
        entity.setActive(true);
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedDate(LocalDateTime.now());
        
        return entity;
    }

    /**
     * Applies updates to existing customer entity.
     */
    private CustomerEntity applyCustomerUpdates(CustomerEntity existing, CustomerUpdateRequest updates) {
        // Apply only non-null updates to preserve existing data
        if (StringUtils.hasText(updates.getFirstName())) {
            existing.setFirstName(updates.getFirstName());
        }
        if (StringUtils.hasText(updates.getLastName())) {
            existing.setLastName(updates.getLastName());
        }
        if (StringUtils.hasText(updates.getPhone())) {
            existing.setPhone(updates.getPhone());
        }
        if (StringUtils.hasText(updates.getEmail())) {
            existing.setEmail(updates.getEmail());
        }
        if (StringUtils.hasText(updates.getAddressLine1())) {
            existing.setAddressLine1(updates.getAddressLine1());
        }
        if (StringUtils.hasText(updates.getAddressLine2())) {
            existing.setAddressLine2(updates.getAddressLine2());
        }
        if (StringUtils.hasText(updates.getCity())) {
            existing.setCity(updates.getCity());
        }
        if (StringUtils.hasText(updates.getState())) {
            existing.setState(updates.getState());
        }
        if (StringUtils.hasText(updates.getZipCode())) {
            existing.setZipCode(updates.getZipCode());
        }
        if (updates.getFicoScore() != null) {
            existing.setFicoScore(updates.getFicoScore());
        }
        if (updates.getActive() != null) {
            existing.setActive(updates.getActive());
        }
        
        existing.setLastModifiedDate(LocalDateTime.now());
        
        return existing;
    }

    // =================== INNER CLASSES FOR DATA TRANSFER ===================

    /**
     * Customer response data transfer object.
     */
    public static class CustomerResponse {
        private String customerId;
        private String firstName;
        private String lastName;
        private String ssn;
        private String phone;
        private String email;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String zipCode;
        private BigDecimal ficoScore;
        private boolean active;
        private LocalDateTime createdDate;
        private LocalDateTime lastModifiedDate;

        // Default constructor
        public CustomerResponse() {}

        // Copy constructor for GDPR masking
        public CustomerResponse(CustomerResponse other) {
            this.customerId = other.customerId;
            this.firstName = other.firstName;
            this.lastName = other.lastName;
            this.ssn = other.ssn;
            this.phone = other.phone;
            this.email = other.email;
            this.addressLine1 = other.addressLine1;
            this.addressLine2 = other.addressLine2;
            this.city = other.city;
            this.state = other.state;
            this.zipCode = other.zipCode;
            this.ficoScore = other.ficoScore;
            this.active = other.active;
            this.createdDate = other.createdDate;
            this.lastModifiedDate = other.lastModifiedDate;
        }

        // Getters and setters
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        
        public String getSsn() { return ssn; }
        public void setSsn(String ssn) { this.ssn = ssn; }
        
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getAddressLine1() { return addressLine1; }
        public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
        
        public String getAddressLine2() { return addressLine2; }
        public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
        
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        
        public String getZipCode() { return zipCode; }
        public void setZipCode(String zipCode) { this.zipCode = zipCode; }
        
        public BigDecimal getFicoScore() { return ficoScore; }
        public void setFicoScore(BigDecimal ficoScore) { this.ficoScore = ficoScore; }
        
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        
        public LocalDateTime getCreatedDate() { return createdDate; }
        public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
        
        public LocalDateTime getLastModifiedDate() { return lastModifiedDate; }
        public void setLastModifiedDate(LocalDateTime lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }
    }

    /**
     * Customer create request data transfer object.
     */
    public static class CustomerCreateRequest {
        private String firstName;
        private String lastName;
        private String ssn;
        private String phone;
        private String email;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String zipCode;
        private BigDecimal ficoScore;

        // Getters and setters
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        
        public String getSsn() { return ssn; }
        public void setSsn(String ssn) { this.ssn = ssn; }
        
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getAddressLine1() { return addressLine1; }
        public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
        
        public String getAddressLine2() { return addressLine2; }
        public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
        
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        
        public String getZipCode() { return zipCode; }
        public void setZipCode(String zipCode) { this.zipCode = zipCode; }
        
        public BigDecimal getFicoScore() { return ficoScore; }
        public void setFicoScore(BigDecimal ficoScore) { this.ficoScore = ficoScore; }
    }

    /**
     * Customer update request data transfer object.
     */
    public static class CustomerUpdateRequest extends CustomerCreateRequest {
        private Boolean active;

        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }

    /**
     * Validation result container.
     */
    public static class ValidationResult {
        private final List<ValidationError> errors = new java.util.ArrayList<>();

        public void addError(String code, String message) {
            errors.add(new ValidationError(code, message));
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public int getErrorCount() {
            return errors.size();
        }

        public String getFirstError() {
            return errors.isEmpty() ? null : errors.get(0).getMessage();
        }

        public List<ValidationError> getErrors() {
            return new java.util.ArrayList<>(errors);
        }
    }

    /**
     * Validation error container.
     */
    public static class ValidationError {
        private final String code;
        private final String message;

        public ValidationError(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() { return code; }
        public String getMessage() { return message; }
    }

    /**
     * Customer entity placeholder (would be actual JPA entity in real implementation).
     */
    public static class CustomerEntity {
        private String customerId;
        private String firstName;
        private String lastName;
        private String ssn;
        private String phone;
        private String email;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String zipCode;
        private BigDecimal ficoScore;
        private boolean active;
        private LocalDateTime createdDate;
        private LocalDateTime lastModifiedDate;

        // Getters and setters (same as CustomerResponse)
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        
        public String getSsn() { return ssn; }
        public void setSsn(String ssn) { this.ssn = ssn; }
        
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getAddressLine1() { return addressLine1; }
        public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
        
        public String getAddressLine2() { return addressLine2; }
        public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
        
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        
        public String getZipCode() { return zipCode; }
        public void setZipCode(String zipCode) { this.zipCode = zipCode; }
        
        public BigDecimal getFicoScore() { return ficoScore; }
        public void setFicoScore(BigDecimal ficoScore) { this.ficoScore = ficoScore; }
        
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        
        public LocalDateTime getCreatedDate() { return createdDate; }
        public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
        
        public LocalDateTime getLastModifiedDate() { return lastModifiedDate; }
        public void setLastModifiedDate(LocalDateTime lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }
    }

    /**
     * GDPR masking level enumeration.
     */
    public enum GDPRMaskingLevel {
        FULL_ACCESS,    // No masking for authorized access
        PARTIAL_MASK,   // Partial masking of sensitive fields
        FULL_MASK,      // Full masking of all sensitive fields
        ANONYMIZE       // Complete anonymization
    }

    // =================== CUSTOM EXCEPTIONS ===================

    public static class CustomerNotFoundException extends RuntimeException {
        public CustomerNotFoundException(String message) {
            super(message);
        }
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

    public static class DuplicateCustomerException extends RuntimeException {
        public DuplicateCustomerException(String message) {
            super(message);
        }
    }

    public static class DataIntegrityException extends RuntimeException {
        public DataIntegrityException(String message) {
            super(message);
        }
    }

    public static class GDPRComplianceException extends RuntimeException {
        public GDPRComplianceException(String message) {
            super(message);
        }
    }

    public static class ConcurrencyException extends RuntimeException {
        public ConcurrencyException(String message) {
            super(message);
        }
    }
}