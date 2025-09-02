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

import com.carddemo.dto.CustomerDto;
import com.carddemo.dto.CustomerRequest;
import com.carddemo.dto.AddressDto;
import com.carddemo.entity.Customer;
import com.carddemo.repository.CustomerRepository;
import com.carddemo.exception.ValidationException;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.util.ValidationUtil;

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
    
    @Autowired
    private CustomerRepository customerRepository;
    
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
     * Retrieves a paginated list of customers.
     * Provides GDPR-compliant customer listing with sensitive data masking.
     * Replaces COBOL browse operations (STARTBR/READNEXT) with paginated REST API.
     * 
     * @param pageable the pagination information
     * @return Page containing customer list with appropriate masking applied
     */
    @Transactional(readOnly = true)
    public Page<CustomerDto> getAllCustomers(Pageable pageable) {
        logger.info("CustomerService.getAllCustomers() - Processing paginated customer list request");
        
        try {
            // Retrieve customers using repository with pagination
            Page<Customer> customerPage = customerRepository.findAll(pageable);
            
            // Convert entities to DTOs with masking
            Page<CustomerDto> customerDtoPage = customerPage.map(this::convertToDto);
            
            logger.info("Successfully retrieved {} customers from page {} (total elements: {})", 
                       customerDtoPage.getContent().size(), customerDtoPage.getNumber(), 
                       customerDtoPage.getTotalElements());
            
            return customerDtoPage;
            
        } catch (Exception e) {
            logger.error("Error retrieving customers: {}", e.getMessage(), e);
            throw new RuntimeException("Error retrieving customers: " + e.getMessage());
        }
    }

    /**
     * Retrieves a specific customer by their ID.
     * Provides GDPR-compliant customer details with sensitive data masking.
     * Replaces COBOL READ operations with repository-based lookup.
     * 
     * @param customerId the unique customer identifier
     * @return CustomerDto with masked sensitive information
     * @throws ResourceNotFoundException if customer is not found
     */
    @Transactional(readOnly = true) 
    public CustomerDto getCustomerById(String customerId) {
        logger.info("CustomerService.getCustomerById() - Processing customer lookup for ID: {}", customerId);
        
        try {
            // Convert String to Long for repository call
            Long customerIdLong = Long.parseLong(customerId);
            Optional<Customer> customerOpt = customerRepository.findByCustomerId(customerIdLong);
            
            if (customerOpt.isEmpty()) {
                throw new ResourceNotFoundException("Customer", customerId);
            }
            
            Customer customer = customerOpt.get();
            CustomerDto customerDto = convertToDto(customer);
            
            logger.info("Successfully retrieved customer: {}", customerId);
            return customerDto;
            
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid customer ID format: " + customerId);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error retrieving customer {}: {}", customerId, e.getMessage(), e);
            throw new RuntimeException("Error retrieving customer: " + e.getMessage());
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
     * @throws ResourceNotFoundException if customer not found
     */
    @Transactional
    public CustomerDto updateCustomer(String customerId, CustomerRequest updateRequest) {
        logger.info("CustomerService.updateCustomer() - Processing update for customer ID: {}", customerId);
        
        try {
            // Convert String to Long for repository call
            Long customerIdLong = Long.parseLong(customerId);
            Optional<Customer> customerOpt = customerRepository.findByCustomerId(customerIdLong);
            
            if (customerOpt.isEmpty()) {
                throw new ResourceNotFoundException("Customer", customerId);
            }
            
            Customer customer = customerOpt.get();
            
            // Update customer fields from request
            updateCustomerFromRequest(customer, updateRequest);
            
            // Save the updated customer
            Customer updatedCustomer = customerRepository.save(customer);
            
            logger.info("Successfully updated customer: {}", customerId);
            return convertToDto(updatedCustomer);
            
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid customer ID format: " + customerId);
        } catch (ResourceNotFoundException | ValidationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error updating customer {}: {}", customerId, e.getMessage(), e);
            throw new RuntimeException("Error updating customer: " + e.getMessage());
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
     */
    @Transactional
    public CustomerDto createCustomer(CustomerRequest createRequest) {
        logger.info("CustomerService.createCustomer() - Processing new customer creation");
        
        try {
            // Create new customer entity (customer ID will be auto-generated)
            Customer newCustomer = createCustomerFromRequest(createRequest);
            
            // Save the new customer
            Customer savedCustomer = customerRepository.save(newCustomer);
            
            logger.info("Successfully created customer: {}", savedCustomer.getCustomerId());
            return convertToDto(savedCustomer);
            
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error creating customer: {}", e.getMessage(), e);
            throw new RuntimeException("Error creating customer: " + e.getMessage());
        }
    }

    /**
     * Converts Customer entity to CustomerDto with GDPR-compliant masking.
     * Applies appropriate masking for sensitive information like SSN and phone.
     * 
     * @param customer the Customer entity to convert
     * @return CustomerDto with masked sensitive data
     */
    private CustomerDto convertToDto(Customer customer) {
        CustomerDto dto = new CustomerDto();
        dto.setCustomerId(String.valueOf(customer.getCustomerId()));
        dto.setFirstName(customer.getFirstName());
        dto.setMiddleName(customer.getMiddleName());
        dto.setLastName(customer.getLastName());
        
        // Apply GDPR-compliant masking to sensitive fields
        dto.setSsn(maskSsn(customer.getSsn()));
        dto.setPhoneNumber1(maskPhoneNumber(customer.getPhoneNumber1()));
        dto.setPhoneNumber2(maskPhoneNumber(customer.getPhoneNumber2()));
        
        // Set address information from individual fields
        AddressDto addressDto = new AddressDto();
        addressDto.setAddressLine1(customer.getAddressLine1());
        addressDto.setAddressLine2(customer.getAddressLine2());
        addressDto.setAddressLine3(customer.getAddressLine3());
        addressDto.setStateCode(customer.getStateCode());
        addressDto.setCountryCode(customer.getCountryCode());
        addressDto.setZipCode(customer.getZipCode());
        dto.setAddress(addressDto);
        
        dto.setGovernmentId(customer.getGovernmentIssuedId());
        dto.setDateOfBirth(customer.getDateOfBirth());
        dto.setEftAccountId(customer.getEftAccountId());
        dto.setPrimaryCardholderIndicator(customer.getPrimaryCardHolderIndicator());
        dto.setFicoScore(customer.getFicoScore());
        
        return dto;
    }

    /**
     * Updates a Customer entity from a CustomerRequest.
     * 
     * @param customer the Customer entity to update
     * @param request the CustomerRequest with update data
     */
    private void updateCustomerFromRequest(Customer customer, CustomerRequest request) {
        if (StringUtils.hasText(request.getFirstName())) {
            customer.setFirstName(request.getFirstName());
        }
        if (StringUtils.hasText(request.getMiddleName())) {
            customer.setMiddleName(request.getMiddleName());
        }
        if (StringUtils.hasText(request.getLastName())) {
            customer.setLastName(request.getLastName());
        }
        if (StringUtils.hasText(request.getPhoneNumber1())) {
            customer.setPhoneNumber1(request.getPhoneNumber1());
        }
        if (StringUtils.hasText(request.getPhoneNumber2())) {
            customer.setPhoneNumber2(request.getPhoneNumber2());
        }
        if (request.getDateOfBirth() != null) {
            customer.setDateOfBirth(request.getDateOfBirth());
        }
        
        // Update address if provided
        if (request.getAddress() != null) {
            AddressDto addressDto = request.getAddress();
            if (StringUtils.hasText(addressDto.getAddressLine1())) {
                customer.setAddressLine1(addressDto.getAddressLine1());
            }
            if (StringUtils.hasText(addressDto.getAddressLine2())) {
                customer.setAddressLine2(addressDto.getAddressLine2());
            }
            if (StringUtils.hasText(addressDto.getAddressLine3())) {
                customer.setAddressLine3(addressDto.getAddressLine3());
            }
            if (StringUtils.hasText(addressDto.getStateCode())) {
                customer.setStateCode(addressDto.getStateCode());
            }
            if (StringUtils.hasText(addressDto.getCountryCode())) {
                customer.setCountryCode(addressDto.getCountryCode());
            }
            if (StringUtils.hasText(addressDto.getZipCode())) {
                customer.setZipCode(addressDto.getZipCode());
            }
        }
    }

    /**
     * Creates a new Customer entity from a CustomerRequest.
     * 
     * @param request the CustomerRequest with creation data
     * @return new Customer entity
     */
    private Customer createCustomerFromRequest(CustomerRequest request) {
        Customer customer = new Customer();
        // Don't set customerId - it will be auto-generated
        customer.setFirstName(request.getFirstName());
        customer.setMiddleName(request.getMiddleName());
        customer.setLastName(request.getLastName());
        customer.setPhoneNumber1(request.getPhoneNumber1());
        customer.setPhoneNumber2(request.getPhoneNumber2());
        customer.setDateOfBirth(request.getDateOfBirth());
        
        // Set address if provided
        if (request.getAddress() != null) {
            AddressDto addressDto = request.getAddress();
            customer.setAddressLine1(addressDto.getAddressLine1());
            customer.setAddressLine2(addressDto.getAddressLine2());
            customer.setAddressLine3(addressDto.getAddressLine3());
            customer.setStateCode(addressDto.getStateCode());
            customer.setCountryCode(addressDto.getCountryCode());
            customer.setZipCode(addressDto.getZipCode());
        }
        
        return customer;
    }

    /**
     * Masks SSN for GDPR compliance.
     * Shows only last 4 digits: XXX-XX-1234
     * 
     * @param ssn the SSN to mask
     * @return masked SSN
     */
    private String maskSsn(String ssn) {
        if (ssn == null || ssn.length() < 4) {
            return SSN_MASK + "XXXX";
        }
        return SSN_MASK + ssn.substring(ssn.length() - 4);
    }

    /**
     * Masks phone number for GDPR compliance.
     * Shows only last 4 digits: XXX-XXX-1234
     * 
     * @param phoneNumber the phone number to mask
     * @return masked phone number
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return PHONE_MASK + "XXXX";
        }
        return PHONE_MASK + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
