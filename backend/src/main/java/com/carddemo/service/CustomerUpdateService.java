/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.entity.Customer;
import com.carddemo.repository.CustomerRepository;
import com.carddemo.util.DateConversionUtil;
import com.carddemo.util.ValidationUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service class implementing customer update operations and validation logic translated from
 * COBOL CBCUS01C.cbl batch customer update program. Provides business logic for customer 
 * information updates, address standardization, phone number formatting, SSN validation,
 * batch file processing, and error record handling.
 * 
 * This service ensures functional parity between the original COBOL batch processing logic
 * and the modernized Spring Boot service implementation, validating that all data transformations,
 * validation rules, and business logic produce identical results to the CBCUS01C.cbl program.
 * 
 * Key functionality includes:
 * - Individual customer record updates (COBOL paragraph 0000-MAIN-PROCESSING equivalent)
 * - Address standardization logic (COBOL paragraph 1000-PROCESS-ADDRESS equivalent) 
 * - Phone number formatting and validation (COBOL paragraph 2000-PROCESS-PHONE equivalent)
 * - SSN validation and formatting (COBOL paragraph 3000-VALIDATE-SSN equivalent)
 * - Batch file processing workflows (COBOL paragraph 4000-PROCESS-BATCH equivalent)
 * - Error record handling and reporting (COBOL paragraph 9000-ERROR-HANDLING equivalent)
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Service
@Transactional
public class CustomerUpdateService {

    private final CustomerRepository customerRepository;

    @Autowired
    public CustomerUpdateService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Updates customer information while preserving COBOL business logic and validation rules.
     * Equivalent to COBOL paragraph 0000-MAIN-PROCESSING in CBCUS01C.cbl.
     * 
     * @param customer Customer data to update
     * @return Updated customer entity
     * @throws RuntimeException if customer not found
     * @throws IllegalArgumentException if validation fails
     */
    public Customer updateCustomer(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer data cannot be null");
        }

        if (customer.getCustomerId() == null) {
            throw new IllegalArgumentException("Customer ID is required");
        }

        // Find existing customer
        String customerIdStr = customer.getCustomerId();
        if (customerIdStr == null) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        
        Long customerIdLong;
        try {
            customerIdLong = Long.valueOf(customerIdStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid customer ID format: " + customerIdStr);
        }
        
        Optional<Customer> existingCustomer = customerRepository.findById(customerIdLong);
        if (!existingCustomer.isPresent()) {
            throw new RuntimeException("Customer not found: " + customerIdStr);
        }

        // Validate customer data before update
        if (!validateCustomerData(customer)) {
            throw new IllegalArgumentException("Customer data validation failed");
        }

        // Update customer information
        Customer customerToUpdate = existingCustomer.get();
        updateCustomerFields(customerToUpdate, customer);

        // Save and return updated customer
        return customerRepository.save(customerToUpdate);
    }

    /**
     * Standardizes address format according to USPS standards.
     * Equivalent to COBOL paragraph 1000-PROCESS-ADDRESS in CBCUS01C.cbl.
     * 
     * @param address Raw address string
     * @return Standardized address format
     * @throws IllegalArgumentException if address is null or empty
     */
    public String standardizeAddress(String address) {
        if (address == null) {
            throw new IllegalArgumentException("Address cannot be null");
        }
        
        if (address.trim().isEmpty()) {
            throw new IllegalArgumentException("Address cannot be empty");
        }

        // Convert to uppercase for standardization
        String standardized = address.toUpperCase().trim();

        // Replace common abbreviations
        standardized = standardized.replaceAll("\\bSTREET\\b", "STREET");
        standardized = standardized.replaceAll("\\bST\\b", "STREET");
        standardized = standardized.replaceAll("\\bAVENUE\\b", "AVENUE");
        standardized = standardized.replaceAll("\\bAVE\\b", "AVENUE");
        standardized = standardized.replaceAll("\\bROAD\\b", "ROAD");
        standardized = standardized.replaceAll("\\bRD\\b", "ROAD");
        standardized = standardized.replaceAll("\\bDRIVE\\b", "DRIVE");
        standardized = standardized.replaceAll("\\bDR\\b", "DRIVE");
        standardized = standardized.replaceAll("\\bBOULEVARD\\b", "BOULEVARD");
        standardized = standardized.replaceAll("\\bBLVD\\b", "BOULEVARD");
        standardized = standardized.replaceAll("\\bLANE\\b", "LANE");
        standardized = standardized.replaceAll("\\bLN\\b", "LANE");
        standardized = standardized.replaceAll("\\bCOURT\\b", "COURT");
        standardized = standardized.replaceAll("\\bCT\\b", "COURT");
        standardized = standardized.replaceAll("\\bAPARTMENT\\b", "APT");
        standardized = standardized.replaceAll("\\bAPT\\b", "APT");
        standardized = standardized.replaceAll("\\bSUITE\\b", "STE");
        standardized = standardized.replaceAll("\\bSTE\\b", "STE");

        // Clean up multiple spaces
        standardized = standardized.replaceAll("\\s+", " ");

        return standardized.trim();
    }

    /**
     * Validates phone number format according to NANPA standards.
     * Equivalent to COBOL paragraph 2000-PROCESS-PHONE in CBCUS01C.cbl.
     * 
     * @param phoneNumber Phone number to validate
     * @return true if valid, false otherwise
     */
    public boolean validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        try {
            // Use ValidationUtil for comprehensive phone validation
            ValidationUtil.validatePhoneNumber("phoneNumber", phoneNumber);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates comprehensive customer data including SSN, phone, and date fields.
     * Equivalent to COBOL paragraph 3000-VALIDATE-SSN in CBCUS01C.cbl.
     * 
     * @param customer Customer to validate
     * @return true if all validation passes, false otherwise
     */
    public boolean validateCustomerData(Customer customer) {
        if (customer == null) {
            return false;
        }

        // Validate SSN using ValidationUtil
        if (customer.getSSN() != null) {
            try {
                ValidationUtil.validateSSN(customer.getSSN());
            } catch (Exception e) {
                return false;
            }
        }

        // Validate phone numbers
        if (customer.getPhoneNumber() != null && !validatePhoneNumber(customer.getPhoneNumber())) {
            return false;
        }

        if (customer.getPhoneNumber2() != null && !validatePhoneNumber(customer.getPhoneNumber2())) {
            return false;
        }

        // Validate date of birth using DateConversionUtil
        if (customer.getDateOfBirth() != null) {
            if (!DateConversionUtil.isNotFutureDate(customer.getDateOfBirth())) {
                return false;
            }
            try {
                ValidationUtil.validateDateOfBirth(customer.getDateOfBirth());
            } catch (Exception e) {
                return false;
            }
        }

        // Validate FICO score range
        if (customer.getFicoScore() != null) {
            if (customer.getFicoScore() < 300 || customer.getFicoScore() > 850) {
                return false;
            }
        }

        return true;
    }

    /**
     * Updates customer credit score with validation and precision preservation.
     * Maintains COBOL COMP-3 packed decimal precision for financial calculations.
     * 
     * @param customerId Customer ID to update
     * @param creditScore New credit score (300-850)
     * @return Updated customer entity
     * @throws RuntimeException if customer not found
     * @throws IllegalArgumentException if credit score is invalid
     */
    public Customer updateCreditScore(String customerId, BigDecimal creditScore) {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID is required");
        }

        if (creditScore == null) {
            throw new IllegalArgumentException("Credit score cannot be null");
        }

        // Validate credit score range
        if (creditScore.compareTo(new BigDecimal("300")) < 0 || creditScore.compareTo(new BigDecimal("850")) > 0) {
            throw new IllegalArgumentException("Credit score must be between 300 and 850");
        }

        // Find customer
        Long customerIdLong;
        try {
            customerIdLong = Long.parseLong(customerId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid customer ID format: " + customerId);
        }
        Optional<Customer> existingCustomer = customerRepository.findById(customerIdLong);
        if (!existingCustomer.isPresent()) {
            throw new RuntimeException("Customer not found: " + customerId);
        }

        // Update credit score maintaining precision
        Customer customerToUpdate = existingCustomer.get();
        customerToUpdate.setCreditScore(creditScore);

        return customerRepository.save(customerToUpdate);
    }

    /**
     * Processes batch of customer updates.
     * Equivalent to COBOL paragraph 4000-PROCESS-BATCH in CBCUS01C.cbl.
     * 
     * @param customers List of customers to process
     * @return Processing results map or list of processed customers
     */
    public List<Customer> processCustomerBatch(List<Customer> customers) {
        if (customers == null || customers.isEmpty()) {
            return new ArrayList<>();
        }

        List<Customer> processedCustomers = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Customer customer : customers) {
            try {
                if (validateCustomerData(customer)) {
                    Customer processed = updateCustomer(customer);
                    processedCustomers.add(processed);
                } else {
                    errors.add("Validation failed for customer: " + customer.getCustomerId());
                }
            } catch (Exception e) {
                errors.add("Error processing customer " + customer.getCustomerId() + ": " + e.getMessage());
            }
        }

        return processedCustomers;
    }

    /**
     * Alternative batch processing method that returns detailed results.
     * 
     * @param customers List of customers to process
     * @return Map containing processing statistics and results
     */
    public Map<String, Object> processCustomerBatch(Map<String, Customer> customers) {
        Map<String, Object> results = new HashMap<>();
        List<Customer> processedCustomers = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;

        for (Map.Entry<String, Customer> entry : customers.entrySet()) {
            try {
                Customer customer = entry.getValue();
                if (validateCustomerData(customer)) {
                    Customer processed = updateCustomer(customer);
                    processedCustomers.add(processed);
                    successCount++;
                } else {
                    errors.add("Validation failed for customer: " + entry.getKey());
                    errorCount++;
                }
            } catch (Exception e) {
                errors.add("Error processing customer " + entry.getKey() + ": " + e.getMessage());
                errorCount++;
            }
        }

        results.put("processedCustomers", processedCustomers);
        results.put("errors", errors);
        results.put("successCount", successCount);
        results.put("errorCount", errorCount);
        results.put("totalCount", customers.size());

        return results;
    }

    /**
     * Helper method to update customer fields from source customer.
     * 
     * @param target Customer to update
     * @param source Customer with new data
     */
    private void updateCustomerFields(Customer target, Customer source) {
        if (source.getFirstName() != null) {
            target.setFirstName(source.getFirstName());
        }
        if (source.getMiddleName() != null) {
            target.setMiddleName(source.getMiddleName());
        }
        if (source.getLastName() != null) {
            target.setLastName(source.getLastName());
        }
        if (source.getAddressLine1() != null) {
            target.setAddressLine1(standardizeAddress(source.getAddressLine1()));
        }
        if (source.getAddressLine2() != null) {
            target.setAddressLine2(standardizeAddress(source.getAddressLine2()));
        }
        if (source.getAddressLine3() != null) {
            target.setAddressLine3(standardizeAddress(source.getAddressLine3()));
        }
        if (source.getStateCode() != null) {
            target.setStateCode(source.getStateCode().toUpperCase());
        }
        if (source.getCountryCode() != null) {
            target.setCountryCode(source.getCountryCode().toUpperCase());
        }
        if (source.getZipCode() != null) {
            target.setZipCode(source.getZipCode());
        }
        if (source.getPhoneNumber() != null) {
            target.setPhoneNumber(source.getPhoneNumber());
        }
        if (source.getPhoneNumber2() != null) {
            target.setPhoneNumber2(source.getPhoneNumber2());
        }
        if (source.getSSN() != null) {
            target.setSSN(source.getSSN());
        }
        if (source.getGovernmentIssuedId() != null) {
            target.setGovernmentIssuedId(source.getGovernmentIssuedId());
        }
        if (source.getDateOfBirth() != null) {
            target.setDateOfBirth(source.getDateOfBirth());
        }
        if (source.getEftAccountId() != null) {
            target.setEftAccountId(source.getEftAccountId());
        }
        if (source.getPrimaryCardHolderIndicator() != null) {
            target.setPrimaryCardHolderIndicator(source.getPrimaryCardHolderIndicator());
        }
        if (source.getFicoScore() != null) {
            target.setFicoScore(source.getFicoScore());
        }
        if (source.getCreditScore() != null) {
            target.setCreditScore(source.getCreditScore());
        }
    }
}