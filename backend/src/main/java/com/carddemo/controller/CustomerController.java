/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.controller;

import com.carddemo.dto.CustomerDto;
import com.carddemo.dto.CustomerRequest;
import com.carddemo.service.CustomerService;
import com.carddemo.entity.Customer;
import com.carddemo.exception.ValidationException;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.util.ValidationUtil;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.time.LocalDate;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

/**
 * REST controller for customer management operations. Manages customer profile creation, 
 * updates, and queries. Provides CRUD operations on customer entities including personal 
 * information, addresses, phone numbers, and FICO scores. Maintains GDPR compliance for 
 * personal data handling.
 * 
 * This controller serves as the REST API layer for customer management operations, replacing
 * CICS transaction processing with modern REST endpoints. Maps legacy COBOL customer operations
 * to HTTP methods while preserving all business logic and validation rules.
 * 
 * Key Features:
 * - Customer profile CRUD operations
 * - GDPR Article 5 compliance for personal data handling
 * - Personal data masking for SSN and sensitive fields
 * - Customer validation logic from COBOL copybooks
 * - Exception handling with detailed error responses
 * 
 * REST Endpoint Mappings:
 * - GET /api/customers - List customers with pagination
 * - GET /api/customers/{id} - Get specific customer details
 * - POST /api/customers - Create new customer profile
 * - PUT /api/customers/{id} - Update existing customer profile
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);
    
    @Autowired
    private CustomerService customerService;
    
    /**
     * Retrieves a paginated list of customers.
     * Provides GDPR-compliant customer listing with sensitive data masking.
     * Replaces COBOL browse operations (STARTBR/READNEXT) with paginated REST API.
     * 
     * @param page the page number (0-based, default: 0)
     * @param size the page size (default: 20, max: 100)
     * @return ResponseEntity containing paginated customer list
     */
    @GetMapping
    public ResponseEntity<Page<CustomerDto>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("Retrieving customers - page: {}, size: {}", page, size);
        
        try {
            // Validate pagination parameters
            if (page < 0) {
                logger.warn("Invalid page number: {}", page);
                throw new ValidationException("Page number must be 0 or greater");
            }
            
            if (size <= 0 || size > 100) {
                logger.warn("Invalid page size: {}", size);
                throw new ValidationException("Page size must be between 1 and 100");
            }
            
            // Create pageable request
            Pageable pageable = PageRequest.of(page, size);
            Page<CustomerDto> customers = customerService.getAllCustomers(pageable);
            
            logger.info("Successfully retrieved {} customers from page {} of {} (total elements: {}, page size: {})", 
                       customers.getContent().size(), customers.getNumber(), customers.getTotalPages(), 
                       customers.getTotalElements(), customers.getSize());
            
            return ResponseEntity.status(HttpStatus.OK).body(customers);
            
        } catch (ValidationException e) {
            logger.error("Validation error in getCustomers: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error in getCustomers: {}", e.getMessage(), e);
            throw new RuntimeException("Error retrieving customers: " + e.getMessage());
        }
    }
    
    /**
     * Retrieves a specific customer by their ID.
     * Provides GDPR-compliant customer details with sensitive data masking.
     * Replaces COBOL READ operations with REST API access.
     * 
     * @param customerId the unique customer identifier
     * @return ResponseEntity containing customer details or 404 if not found
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerDto> getCustomerById(@PathVariable String customerId) {
        
        logger.info("Retrieving customer by ID: {}", customerId);
        
        try {
            // Validate customer ID format using ValidationUtil
            ValidationUtil.validateRequiredField("customerId", customerId);
            ValidationUtil.validateFieldLength("customerId", customerId, 9);
            ValidationUtil.validateNumericField("customerId", customerId);
            
            // Retrieve customer through service layer
            CustomerDto customer = customerService.getCustomerById(customerId);
            
            // Log success with customer ID from response
            logger.info("Successfully retrieved customer with ID: {}", customer.getCustomerId());
            
            return ResponseEntity.status(HttpStatus.OK).body(customer);
            
        } catch (ResourceNotFoundException e) {
            logger.warn("Customer not found with ID: {}", customerId);
            throw e;
        } catch (ValidationException e) {
            logger.error("Validation error for customer ID {}: {}", customerId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error retrieving customer {}: {}", customerId, e.getMessage(), e);
            throw new RuntimeException("Error retrieving customer: " + e.getMessage());
        }
    }
    
    /**
     * Creates a new customer profile.
     * Implements customer creation with comprehensive validation and GDPR compliance.
     * Replaces COBOL WRITE operations with REST API creation.
     * 
     * @param customerRequest the customer data for creation
     * @return ResponseEntity containing created customer details
     */
    @PostMapping
    public ResponseEntity<CustomerDto> createCustomer(@Valid @RequestBody CustomerRequest customerRequest) {
        
        logger.info("Creating new customer: {} {}", 
                   customerRequest.getFirstName(), customerRequest.getLastName());
        
        try {
            // Comprehensive validation using ValidationUtil methods
            ValidationUtil.validateRequiredField("firstName", customerRequest.getFirstName());
            ValidationUtil.validateRequiredField("lastName", customerRequest.getLastName());
            ValidationUtil.validateFieldLength("firstName", customerRequest.getFirstName(), 25);
            ValidationUtil.validateFieldLength("lastName", customerRequest.getLastName(), 25);
            
            // Validate address fields if provided
            if (customerRequest.getAddress() != null) {
                // Validate state code if present
                if (customerRequest.getAddress().getStateCode() != null && 
                    !customerRequest.getAddress().getStateCode().trim().isEmpty()) {
                    if (!ValidationUtil.isValidStateCode(customerRequest.getAddress().getStateCode())) {
                        throw new ValidationException("Invalid US state code: " + customerRequest.getAddress().getStateCode());
                    }
                }
                
                // Validate ZIP code if present
                if (customerRequest.getAddress().getZipCode() != null && 
                    !customerRequest.getAddress().getZipCode().trim().isEmpty()) {
                    ValidationUtil.validateZipCode("zipCode", customerRequest.getAddress().getZipCode());
                }
                
                // Validate state-ZIP combination if both are present
                if (customerRequest.getAddress().getStateCode() != null && 
                    customerRequest.getAddress().getZipCode() != null &&
                    !customerRequest.getAddress().getStateCode().trim().isEmpty() &&
                    !customerRequest.getAddress().getZipCode().trim().isEmpty()) {
                    
                    if (!ValidationUtil.validateStateZipCode(
                            customerRequest.getAddress().getStateCode(), 
                            customerRequest.getAddress().getZipCode())) {
                        throw new ValidationException("Invalid state-ZIP code combination: " + 
                                                   customerRequest.getAddress().getStateCode() + "-" + 
                                                   customerRequest.getAddress().getZipCode());
                    }
                }
            }
            
            // Validate phone number if provided
            if (customerRequest.getPhoneNumber1() != null && !customerRequest.getPhoneNumber1().trim().isEmpty()) {
                ValidationUtil.validatePhoneNumber("phoneNumber1", customerRequest.getPhoneNumber1());
                
                // Extract and validate area code using ValidationUtil
                String cleanPhone = customerRequest.getPhoneNumber1().replaceAll("\\D", "");
                if (cleanPhone.length() >= 3) {
                    String areaCode = cleanPhone.substring(0, 3);
                    if (!ValidationUtil.isValidPhoneAreaCode(areaCode)) {
                        throw new ValidationException("Invalid phone area code: " + areaCode);
                    }
                }
            }
            
            // Validate date of birth if provided (LocalDate format)
            if (customerRequest.getDateOfBirth() != null) {
                // Convert LocalDate to COBOL date format for validation
                String dobString = customerRequest.getDateOfBirth().toString().replace("-", "");
                ValidationUtil.validateDateOfBirth("dateOfBirth", dobString);
            }
            
            // Create customer through service layer
            CustomerDto createdCustomer = customerService.createCustomer(customerRequest);
            
            logger.info("Successfully created customer with ID: {}", createdCustomer.getCustomerId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCustomer);
            
        } catch (ValidationException e) {
            logger.error("Validation error creating customer: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error creating customer: {}", e.getMessage(), e);
            throw new RuntimeException("Error creating customer: " + e.getMessage());
        }
    }
    
    /**
     * Updates an existing customer profile.
     * Implements customer update with comprehensive validation and GDPR compliance.
     * Replaces COBOL REWRITE operations with REST API updates.
     * 
     * @param customerId the unique customer identifier
     * @param customerRequest the customer data for update
     * @return ResponseEntity containing updated customer details
     */
    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerDto> updateCustomer(@PathVariable String customerId, 
                                                     @Valid @RequestBody CustomerRequest customerRequest) {
        
        logger.info("Updating customer with ID: {}", customerId);
        
        try {
            // Validate customer ID format
            ValidationUtil.validateRequiredField("customerId", customerId);
            ValidationUtil.validateFieldLength("customerId", customerId, 9);
            ValidationUtil.validateNumericField("customerId", customerId);
            
            // Ensure request customer ID matches path parameter
            if (customerRequest.getCustomerId() != null && 
                !customerRequest.getCustomerId().equals(customerId)) {
                throw new ValidationException("Customer ID in request body must match path parameter");
            }
            
            // Set customer ID in request if not already set
            if (customerRequest.getCustomerId() == null) {
                customerRequest.setCustomerId(customerId);
            }
            
            // Comprehensive validation using ValidationUtil methods
            if (customerRequest.getFirstName() != null && !customerRequest.getFirstName().trim().isEmpty()) {
                ValidationUtil.validateRequiredField("firstName", customerRequest.getFirstName());
                ValidationUtil.validateFieldLength("firstName", customerRequest.getFirstName(), 25);
            }
            
            if (customerRequest.getLastName() != null && !customerRequest.getLastName().trim().isEmpty()) {
                ValidationUtil.validateRequiredField("lastName", customerRequest.getLastName());
                ValidationUtil.validateFieldLength("lastName", customerRequest.getLastName(), 25);
            }
            
            // Validate address fields if provided
            if (customerRequest.getAddress() != null) {
                // Validate state code if present
                if (customerRequest.getAddress().getStateCode() != null && 
                    !customerRequest.getAddress().getStateCode().trim().isEmpty()) {
                    if (!ValidationUtil.isValidStateCode(customerRequest.getAddress().getStateCode())) {
                        throw new ValidationException("Invalid US state code: " + customerRequest.getAddress().getStateCode());
                    }
                }
                
                // Validate ZIP code if present
                if (customerRequest.getAddress().getZipCode() != null && 
                    !customerRequest.getAddress().getZipCode().trim().isEmpty()) {
                    ValidationUtil.validateZipCode("zipCode", customerRequest.getAddress().getZipCode());
                }
                
                // Validate state-ZIP combination if both are present
                if (customerRequest.getAddress().getStateCode() != null && 
                    customerRequest.getAddress().getZipCode() != null &&
                    !customerRequest.getAddress().getStateCode().trim().isEmpty() &&
                    !customerRequest.getAddress().getZipCode().trim().isEmpty()) {
                    
                    if (!ValidationUtil.validateStateZipCode(
                            customerRequest.getAddress().getStateCode(), 
                            customerRequest.getAddress().getZipCode())) {
                        throw new ValidationException("Invalid state-ZIP code combination: " + 
                                                   customerRequest.getAddress().getStateCode() + "-" + 
                                                   customerRequest.getAddress().getZipCode());
                    }
                }
            }
            
            // Validate phone number if provided
            if (customerRequest.getPhoneNumber1() != null && !customerRequest.getPhoneNumber1().trim().isEmpty()) {
                ValidationUtil.validatePhoneNumber("phoneNumber1", customerRequest.getPhoneNumber1());
                
                // Extract and validate area code
                String cleanPhone = customerRequest.getPhoneNumber1().replaceAll("\\D", "");
                if (cleanPhone.length() >= 3) {
                    String areaCode = cleanPhone.substring(0, 3);
                    if (!ValidationUtil.isValidPhoneAreaCode(areaCode)) {
                        throw new ValidationException("Invalid phone area code: " + areaCode);
                    }
                }
            }
            
            // Validate date of birth if provided (LocalDate format)
            if (customerRequest.getDateOfBirth() != null) {
                // Convert LocalDate to COBOL date format for validation
                String dobString = customerRequest.getDateOfBirth().toString().replace("-", "");
                ValidationUtil.validateDateOfBirth("dateOfBirth", dobString);
            }
            
            // Update customer through service layer
            CustomerDto updatedCustomer = customerService.updateCustomer(customerId, customerRequest);
            
            logger.info("Successfully updated customer with ID: {}", updatedCustomer.getCustomerId());
            
            return ResponseEntity.status(HttpStatus.OK).body(updatedCustomer);
            
        } catch (ResourceNotFoundException e) {
            logger.warn("Customer not found for update with ID: {}", customerId);
            throw e;
        } catch (ValidationException e) {
            logger.error("Validation error updating customer {}: {}", customerId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error updating customer {}: {}", customerId, e.getMessage(), e);
            throw new RuntimeException("Error updating customer: " + e.getMessage());
        }
    }
}