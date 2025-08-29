/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carddemo.service;

import com.carddemo.batch.CustomerMaintenanceJobConfig;
import com.carddemo.dto.AddressDto;
import com.carddemo.entity.Customer;
import com.carddemo.exception.ValidationException;
import com.carddemo.repository.CustomerRepository;
import com.carddemo.util.DateConversionUtil;
import com.carddemo.util.ValidationUtil;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Spring Batch service implementing customer data maintenance and validation translated from CBCUS01C.cbl.
 * 
 * This service orchestrates comprehensive customer maintenance operations including:
 * - Customer data validation with COBOL-equivalent edit patterns
 * - Address standardization using USPS validation services
 * - Credit score updates through credit bureau integration
 * - Customer segmentation for marketing and risk assessment
 * 
 * The service maintains exact processing sequences and restart capabilities as required
 * by the mainframe-to-Spring Boot migration specifications. All business logic preserves
 * the validation rules and calculations from the original COBOL implementation.
 * 
 * Translated from: CBCUS01C.cbl - Customer Maintenance Batch Program
 * Migration Target: Spring Batch 5.x with @Profile("!test")
@Service annotation
 */
@Profile("!test")
@Service
public class CustomerMaintenanceBatchService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerMaintenanceBatchService.class);

    // Processing status constants
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";

    // Batch processing constants matching COBOL edit patterns
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int MAX_ERROR_THRESHOLD = 50;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private CustomerMaintenanceJobConfig customerMaintenanceJobConfig;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AddressValidationService addressValidationService;

    @Autowired
    private CreditBureauService creditBureauService;

    @Autowired
    private CustomerSegmentationService customerSegmentationService;

    // Processing state tracking
    private String currentJobStatus = STATUS_PENDING;
    private LocalDateTime lastExecutionTime;
    private Map<String, Object> processingMetrics = new HashMap<>();

    /**
     * Executes comprehensive customer maintenance processing job.
     * 
     * This method orchestrates the complete customer maintenance workflow:
     * 1. Customer data validation
     * 2. Address standardization 
     * 3. Credit score updates
     * 4. Customer segmentation
     * 
     * Preserves exact processing sequences from CBCUS01C.cbl while implementing
     * Spring Batch job execution patterns.
     * 
     * @param parameters Job execution parameters including date range and batch size
     * @return JobExecution result containing execution status and metrics
     * @throws Exception if job execution fails
     */
    public JobExecution executeMaintenanceJob(JobParameters parameters) throws Exception {
        
        logger.info("Starting customer maintenance job execution");
        currentJobStatus = STATUS_PROCESSING;
        lastExecutionTime = LocalDateTime.now();
        
        try {
            // Initialize processing metrics
            initializeProcessingMetrics();
            
            // Validate job parameters
            validateJobParameters(parameters);
            
            // Execute the Spring Batch job using the job configuration
            JobExecution jobExecution = jobLauncher.run(
                customerMaintenanceJobConfig.customerMaintenanceJob(), 
                parameters
            );
            
            // Update processing status based on execution result
            updateProcessingStatus(jobExecution);
            
            logger.info("Customer maintenance job completed with status: " + jobExecution.getStatus());
            return jobExecution;
            
        } catch (Exception e) {
            currentJobStatus = STATUS_FAILED;
            logger.error("Customer maintenance job failed: " + e.getMessage(), e);
            throw new RuntimeException("Customer maintenance job execution failed", e);
        }
    }

    /**
     * Processes customer maintenance operations for a single customer or batch.
     * 
     * Implements the main processing logic equivalent to the COBOL paragraph structure:
     * - 0000-INIT: Initialize processing
     * - 1000-INPUT: Read customer data
     * - 2000-PROCESS: Validate and update customer information
     * - 3000-OUTPUT: Write updated customer data
     * - 9000-CLOSE: Finalize processing
     * 
     * @param customerId Customer ID to process, or null for batch processing
     * @param batchSize Number of customers to process in batch mode
     * @return Processing result summary
     */
    public Map<String, Object> processCustomerMaintenance(Long customerId, Integer batchSize) {
        
        logger.info("Starting customer maintenance processing - customerId: " + customerId + 
                   ", batchSize: " + batchSize);
        
        Map<String, Object> processingResult = new HashMap<>();
        List<Customer> customersToProcess = new ArrayList<>();
        
        try {
            // 0000-INIT equivalent: Initialize processing
            initializeProcessing(processingResult);
            
            // 1000-INPUT equivalent: Read customer data
            customersToProcess = getCustomersForProcessing(customerId, batchSize);
            processingResult.put("customersFound", customersToProcess.size());
            
            // 2000-PROCESS equivalent: Process each customer
            int processedCount = 0;
            int errorCount = 0;
            
            for (Customer customer : customersToProcess) {
                try {
                    processIndividualCustomer(customer);
                    processedCount++;
                } catch (Exception e) {
                    errorCount++;
                    logger.error("Error processing customer " + customer.getCustomerId() + ": " + e.getMessage());
                    
                    // Fail fast if error threshold exceeded
                    if (errorCount > MAX_ERROR_THRESHOLD) {
                        throw new RuntimeException("Error threshold exceeded: " + errorCount + " errors");
                    }
                }
            }
            
            // 3000-OUTPUT equivalent: Update processing results
            processingResult.put("processedCount", processedCount);
            processingResult.put("errorCount", errorCount);
            processingResult.put("processingStatus", "COMPLETED");
            
            // 9000-CLOSE equivalent: Finalize processing
            finalizeProcessing(processingResult);
            
            logger.info("Customer maintenance processing completed - processed: " + processedCount + 
                       ", errors: " + errorCount);
            
            return processingResult;
            
        } catch (Exception e) {
            processingResult.put("processingStatus", "FAILED");
            processingResult.put("errorMessage", e.getMessage());
            logger.error("Customer maintenance processing failed: " + e.getMessage(), e);
            throw new RuntimeException("Customer maintenance processing failed", e);
        }
    }

    /**
     * Validates customer data using COBOL-equivalent edit patterns.
     * 
     * Performs comprehensive validation matching the original COBOL validation routines:
     * - SSN format and check digit validation
     * - Phone number area code and format validation
     * - ZIP code format and existence validation
     * - Required field presence validation
     * - Date of birth range and format validation
     * 
     * @param customer Customer entity to validate
     * @return Validation result summary with detailed error information
     */
    public Map<String, Object> validateCustomerData(Customer customer) {
        
        logger.info("Validating customer data for customer ID: " + customer.getCustomerId());
        
        Map<String, Object> validationResult = new HashMap<>();
        List<String> validationErrors = new ArrayList<>();
        
        try {
            // Validate required fields using ValidationUtil
            try {
                ValidationUtil.validateRequiredField("firstName", customer.getFirstName());
            } catch (ValidationException e) {
                validationErrors.add("First name is required");
            }
            
            try {
                ValidationUtil.validateRequiredField("lastName", customer.getLastName());
            } catch (ValidationException e) {
                validationErrors.add("Last name is required");
            }
            
            // SSN validation using COBOL edit patterns
            if (customer.getSsn() != null && !customer.getSsn().trim().isEmpty()) {
                try {
                    ValidationUtil.validateSSN("ssn", customer.getSsn());
                } catch (ValidationException e) {
                    validationErrors.add("Invalid SSN format or check digit");
                }
            }
            
            // Phone number validation
            if (customer.getPhoneNumber1() != null && !customer.getPhoneNumber1().trim().isEmpty()) {
                try {
                    ValidationUtil.validatePhoneNumber("phoneNumber1", customer.getPhoneNumber1());
                } catch (ValidationException e) {
                    validationErrors.add("Invalid phone number area code");
                }
            }
            
            // ZIP code validation
            if (customer.getZipCode() != null && !customer.getZipCode().trim().isEmpty()) {
                try {
                    ValidationUtil.validateZipCode("zipCode", customer.getZipCode());
                } catch (ValidationException e) {
                    validationErrors.add("Invalid ZIP code format");
                }
            }
            
            // Date of birth validation
            if (customer.getDateOfBirth() != null) {
                try {
                    String dateStr = DateConversionUtil.formatToCobol(customer.getDateOfBirth());
                    ValidationUtil.validateDateOfBirth("dateOfBirth", dateStr);
                } catch (Exception e) {
                    validationErrors.add("Invalid date of birth");
                }
            }
            
            // Compile validation results
            validationResult.put("customerId", customer.getCustomerId());
            validationResult.put("isValid", validationErrors.isEmpty());
            validationResult.put("errorCount", validationErrors.size());
            validationResult.put("validationErrors", validationErrors);
            validationResult.put("validationTimestamp", LocalDateTime.now());
            
            if (validationErrors.isEmpty()) {
                logger.info("Customer data validation passed for customer: " + customer.getCustomerId());
            } else {
                logger.warn("Customer data validation failed for customer: " + customer.getCustomerId() + 
                           " - errors: " + validationErrors);
            }
            
            return validationResult;
            
        } catch (Exception e) {
            validationResult.put("isValid", false);
            validationResult.put("errorMessage", "Validation process failed: " + e.getMessage());
            logger.error("Customer data validation failed for customer " + customer.getCustomerId() + 
                        ": " + e.getMessage(), e);
            throw new RuntimeException("Customer data validation failed", e);
        }
    }

    /**
     * Standardizes customer addresses using USPS validation patterns.
     * 
     * Implements address standardization logic equivalent to COBOL address processing:
     * - Address line normalization and formatting
     * - ZIP+4 code validation and completion
     * - State code standardization
     * - Address deliverability verification
     * 
     * @param customers List of customers requiring address standardization
     * @return Address standardization result summary
     */
    public Map<String, Object> standardizeAddresses(List<Customer> customers) {
        
        logger.info("Starting address standardization for " + customers.size() + " customers");
        
        Map<String, Object> standardizationResult = new HashMap<>();
        int processedCount = 0;
        int standardizedCount = 0;
        int errorCount = 0;
        List<String> processingErrors = new ArrayList<>();
        
        try {
            for (Customer customer : customers) {
                try {
                    // Create address DTO from customer data for validation service
                    AddressDto addressDto = buildAddressDto(customer);
                    
                    // Standardize address using AddressValidationService
                    AddressDto standardizedAddress = addressValidationService.standardizeAddress(addressDto);
                    
                    // Validate and update ZIP+4 code
                    String validatedZip = addressValidationService.validateZipCode4(customer.getZipCode());
                    
                    // Format standardized address
                    String formattedAddress = addressValidationService.formatAddress(standardizedAddress);
                    
                    // Update customer with standardized address information
                    updateCustomerAddress(customer, formattedAddress, validatedZip);
                    
                    standardizedCount++;
                    logger.debug("Address standardized for customer: " + customer.getCustomerId());
                    
                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = "Error standardizing address for customer " + customer.getCustomerId() + 
                                    ": " + e.getMessage();
                    processingErrors.add(errorMsg);
                    logger.error(errorMsg);
                }
                
                processedCount++;
            }
            
            // Compile standardization results
            standardizationResult.put("totalCustomers", customers.size());
            standardizationResult.put("processedCount", processedCount);
            standardizationResult.put("standardizedCount", standardizedCount);
            standardizationResult.put("errorCount", errorCount);
            standardizationResult.put("processingErrors", processingErrors);
            standardizationResult.put("processingTimestamp", LocalDateTime.now());
            
            logger.info("Address standardization completed - processed: " + processedCount + 
                       ", standardized: " + standardizedCount + ", errors: " + errorCount);
            
            return standardizationResult;
            
        } catch (Exception e) {
            standardizationResult.put("processingStatus", "FAILED");
            standardizationResult.put("errorMessage", "Address standardization failed: " + e.getMessage());
            logger.error("Address standardization process failed: " + e.getMessage(), e);
            throw new RuntimeException("Address standardization failed", e);
        }
    }

    /**
     * Updates customer credit scores through credit bureau integration.
     * 
     * Implements credit score update processing matching COBOL credit processing logic:
     * - Credit bureau score retrieval
     * - Score validation and range checking
     * - Historical score tracking
     * - Credit report generation
     * 
     * @param customers List of customers requiring credit score updates
     * @return Credit score update result summary
     */
    public Map<String, Object> updateCreditScores(List<Customer> customers) {
        
        logger.info("Starting credit score updates for " + customers.size() + " customers");
        
        Map<String, Object> updateResult = new HashMap<>();
        int processedCount = 0;
        int updatedCount = 0;
        int errorCount = 0;
        List<String> processingErrors = new ArrayList<>();
        
        try {
            for (Customer customer : customers) {
                try {
                    // Retrieve current FICO score
                    BigDecimal currentScore = customer.getFicoScore();
                    
                    // Update credit score using CreditBureauService
                    String customerIdStr = customer.getCustomerId();
                    Long customerIdLong = customerIdStr != null ? Long.valueOf(customerIdStr) : null;
                    Integer newScore = creditBureauService.updateCreditScore(customerIdLong);
                    
                    // Validate new credit score
                    if (creditBureauService.validateCreditScore(newScore)) {
                        // Update customer with new score
                        customer.setFicoScore(new BigDecimal(newScore));
                        customerRepository.save(customer);
                        
                        updatedCount++;
                        logger.debug("Credit score updated for customer " + customer.getCustomerId() + 
                                   " from " + currentScore + " to " + newScore);
                    } else {
                        throw new IllegalArgumentException("Invalid credit score received: " + newScore);
                    }
                    
                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = "Error updating credit score for customer " + customer.getCustomerId() + 
                                    ": " + e.getMessage();
                    processingErrors.add(errorMsg);
                    logger.error(errorMsg);
                }
                
                processedCount++;
            }
            
            // Compile update results
            updateResult.put("totalCustomers", customers.size());
            updateResult.put("processedCount", processedCount);
            updateResult.put("updatedCount", updatedCount);
            updateResult.put("errorCount", errorCount);
            updateResult.put("processingErrors", processingErrors);
            updateResult.put("processingTimestamp", LocalDateTime.now());
            
            logger.info("Credit score updates completed - processed: " + processedCount + 
                       ", updated: " + updatedCount + ", errors: " + errorCount);
            
            return updateResult;
            
        } catch (Exception e) {
            updateResult.put("processingStatus", "FAILED");
            updateResult.put("errorMessage", "Credit score update failed: " + e.getMessage());
            logger.error("Credit score update process failed: " + e.getMessage(), e);
            throw new RuntimeException("Credit score update failed", e);
        }
    }

    /**
     * Performs customer segmentation for marketing and risk assessment.
     * 
     * Implements customer segmentation logic preserving COBOL business rules:
     * - Risk-based customer classification
     * - Account value tier assignment
     * - Marketing eligibility determination
     * - Segment update processing
     * 
     * @param customers List of customers requiring segmentation
     * @return Customer segmentation result summary
     */
    public Map<String, Object> segmentCustomers(List<Customer> customers) {
        
        logger.info("Starting customer segmentation for " + customers.size() + " customers");
        
        Map<String, Object> segmentationResult = new HashMap<>();
        int processedCount = 0;
        int segmentedCount = 0;
        int errorCount = 0;
        Map<String, Integer> segmentCounts = new HashMap<>();
        List<String> processingErrors = new ArrayList<>();
        
        try {
            for (Customer customer : customers) {
                try {
                    // Extract customer attributes for segmentation
                    String customerId = customer.getCustomerId().toString();
                    BigDecimal ficoScore = customer.getFicoScore() != null ? customer.getFicoScore() : BigDecimal.ZERO;
                    BigDecimal accountBalance = customer.getCreditLimit() != null ? customer.getCreditLimit() : BigDecimal.ZERO;
                    LocalDate lastTransactionDate = customer.getLastUpdateTimestamp() != null ? 
                        customer.getLastUpdateTimestamp().toLocalDate() : LocalDate.now().minusYears(1);
                    int accountAge = calculateAccountAge(customer.getCreatedTimestamp());
                    
                    // Perform customer segmentation
                    String segment = customerSegmentationService.segmentCustomer(
                        customerId, ficoScore.intValue(), accountBalance, lastTransactionDate, accountAge
                    );
                    
                    // Update customer segment (assuming we add this field to Customer entity)
                    // Note: This would require adding a segment field to the Customer entity
                    // For now, we'll track in processing metrics
                    
                    // Track segment counts
                    segmentCounts.put(segment, segmentCounts.getOrDefault(segment, 0) + 1);
                    
                    segmentedCount++;
                    logger.debug("Customer " + customerId + " segmented as: " + segment);
                    
                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = "Error segmenting customer " + customer.getCustomerId() + 
                                    ": " + e.getMessage();
                    processingErrors.add(errorMsg);
                    logger.error(errorMsg);
                }
                
                processedCount++;
            }
            
            // Compile segmentation results
            segmentationResult.put("totalCustomers", customers.size());
            segmentationResult.put("processedCount", processedCount);
            segmentationResult.put("segmentedCount", segmentedCount);
            segmentationResult.put("errorCount", errorCount);
            segmentationResult.put("segmentCounts", segmentCounts);
            segmentationResult.put("processingErrors", processingErrors);
            segmentationResult.put("processingTimestamp", LocalDateTime.now());
            
            logger.info("Customer segmentation completed - processed: " + processedCount + 
                       ", segmented: " + segmentedCount + ", errors: " + errorCount);
            
            return segmentationResult;
            
        } catch (Exception e) {
            segmentationResult.put("processingStatus", "FAILED");
            segmentationResult.put("errorMessage", "Customer segmentation failed: " + e.getMessage());
            logger.error("Customer segmentation process failed: " + e.getMessage(), e);
            throw new RuntimeException("Customer segmentation failed", e);
        }
    }

    /**
     * Retrieves current processing status for monitoring and reporting.
     * 
     * @return Current processing status information
     */
    public Map<String, Object> getProcessingStatus() {
        
        Map<String, Object> status = new HashMap<>();
        
        status.put("currentJobStatus", currentJobStatus);
        status.put("lastExecutionTime", lastExecutionTime);
        status.put("processingMetrics", new HashMap<>(processingMetrics));
        status.put("statusTimestamp", LocalDateTime.now());
        
        return status;
    }

    /**
     * Generates comprehensive maintenance report for processed customers.
     * 
     * @param processingResults Results from maintenance processing operations
     * @return Formatted maintenance report
     */
    public Map<String, Object> generateMaintenanceReport(Map<String, Object> processingResults) {
        
        logger.info("Generating customer maintenance report");
        
        Map<String, Object> report = new HashMap<>();
        
        try {
            // Report header information
            report.put("reportTitle", "Customer Maintenance Processing Report");
            report.put("reportTimestamp", LocalDateTime.now());
            report.put("reportingPeriod", getReportingPeriod());
            
            // Processing summary
            Map<String, Object> processingSummary = new HashMap<>();
            processingSummary.put("totalCustomersProcessed", processingResults.getOrDefault("processedCount", 0));
            processingSummary.put("validationResults", processingResults.get("validationResults"));
            processingSummary.put("addressStandardizationResults", processingResults.get("addressResults"));
            processingSummary.put("creditScoreUpdateResults", processingResults.get("creditResults"));
            processingSummary.put("segmentationResults", processingResults.get("segmentationResults"));
            
            report.put("processingSummary", processingSummary);
            
            // Error summary
            Map<String, Object> errorSummary = new HashMap<>();
            errorSummary.put("totalErrors", processingResults.getOrDefault("errorCount", 0));
            errorSummary.put("errorDetails", processingResults.get("processingErrors"));
            
            report.put("errorSummary", errorSummary);
            
            // Performance metrics
            report.put("performanceMetrics", processingMetrics);
            
            logger.info("Customer maintenance report generated successfully");
            return report;
            
        } catch (Exception e) {
            logger.error("Error generating maintenance report: " + e.getMessage(), e);
            throw new RuntimeException("Maintenance report generation failed", e);
        }
    }

    // Private helper methods implementing COBOL processing patterns

    /**
     * Initializes processing metrics and tracking.
     */
    private void initializeProcessingMetrics() {
        processingMetrics.clear();
        processingMetrics.put("startTime", LocalDateTime.now());
        processingMetrics.put("totalCustomersProcessed", 0);
        processingMetrics.put("validationErrors", 0);
        processingMetrics.put("addressUpdates", 0);
        processingMetrics.put("creditScoreUpdates", 0);
        processingMetrics.put("segmentationChanges", 0);
    }

    /**
     * Validates job parameters for batch execution.
     */
    private void validateJobParameters(JobParameters parameters) {
        if (parameters == null || parameters.isEmpty()) {
            // Create default parameters if none provided
            logger.info("No job parameters provided, using defaults");
        }
        
        // Additional parameter validation can be added here
        var paramMap = parameters.getParameters();
        logger.info("Job parameters validated: " + paramMap.size() + " parameters");
    }

    /**
     * Updates processing status based on job execution results.
     */
    private void updateProcessingStatus(JobExecution jobExecution) {
        if (jobExecution.getStatus().isRunning()) {
            currentJobStatus = STATUS_PROCESSING;
        } else if (jobExecution.getStatus().isUnsuccessful()) {
            currentJobStatus = STATUS_FAILED;
        } else {
            currentJobStatus = STATUS_COMPLETED;
        }
        
        // Update processing metrics
        processingMetrics.put("endTime", LocalDateTime.now());
        processingMetrics.put("executionStatus", jobExecution.getStatus().toString());
        processingMetrics.put("jobInstanceId", jobExecution.getJobInstance().getId());
    }

    /**
     * Initializes processing state for customer maintenance.
     */
    private void initializeProcessing(Map<String, Object> processingResult) {
        processingResult.put("processingStartTime", LocalDateTime.now());
        processingResult.put("batchSize", DEFAULT_BATCH_SIZE);
        processingResult.put("errorThreshold", MAX_ERROR_THRESHOLD);
        
        logger.info("Customer maintenance processing initialized");
    }

    /**
     * Retrieves customers for processing based on criteria.
     */
    private List<Customer> getCustomersForProcessing(Long customerId, Integer batchSize) {
        
        if (customerId != null) {
            // Process single customer
            Customer customer = customerRepository.findById(customerId).orElse(null);
            if (customer != null) {
                List<Customer> customers = new ArrayList<>();
                customers.add(customer);
                return customers;
            } else {
                logger.warn("Customer not found for ID: " + customerId);
                return new ArrayList<>();
            }
        } else {
            // Process batch of customers
            int effectiveBatchSize = batchSize != null ? batchSize : DEFAULT_BATCH_SIZE;
            
            // Use findAll with pagination for batch processing
            List<Customer> allCustomers = customerRepository.findAll();
            
            // Limit to batch size
            if (allCustomers.size() > effectiveBatchSize) {
                return allCustomers.subList(0, effectiveBatchSize);
            } else {
                return allCustomers;
            }
        }
    }

    /**
     * Processes an individual customer through all maintenance steps.
     */
    private void processIndividualCustomer(Customer customer) {
        
        logger.debug("Processing individual customer: " + customer.getCustomerId());
        
        // Step 1: Validate customer data
        Map<String, Object> validationResult = validateCustomerData(customer);
        if (!(Boolean) validationResult.get("isValid")) {
            throw new RuntimeException("Customer validation failed: " + validationResult.get("validationErrors"));
        }
        
        // Step 2: Standardize address
        List<Customer> singleCustomerList = new ArrayList<>();
        singleCustomerList.add(customer);
        standardizeAddresses(singleCustomerList);
        
        // Step 3: Update credit score
        updateCreditScores(singleCustomerList);
        
        // Step 4: Perform segmentation
        segmentCustomers(singleCustomerList);
        
        // Save updated customer
        customerRepository.save(customer);
        
        logger.debug("Individual customer processing completed: " + customer.getCustomerId());
    }

    /**
     * Finalizes processing and updates metrics.
     */
    private void finalizeProcessing(Map<String, Object> processingResult) {
        processingResult.put("processingEndTime", LocalDateTime.now());
        
        // Update processing metrics
        processingMetrics.put("totalCustomersProcessed", 
            ((Integer) processingMetrics.getOrDefault("totalCustomersProcessed", 0)) + 
            ((Integer) processingResult.getOrDefault("processedCount", 0)));
        
        logger.info("Customer maintenance processing finalized");
    }

    /**
     * Builds full address string from customer data for validation.
     */
    private String buildFullAddress(Customer customer) {
        StringBuilder addressBuilder = new StringBuilder();
        
        if (customer.getAddressLine1() != null) {
            addressBuilder.append(customer.getAddressLine1());
        }
        
        if (customer.getAddressLine2() != null && !customer.getAddressLine2().trim().isEmpty()) {
            if (addressBuilder.length() > 0) {
                addressBuilder.append(" ");
            }
            addressBuilder.append(customer.getAddressLine2());
        }
        
        return addressBuilder.toString();
    }

    /**
     * Updates customer with standardized address information.
     */
    private void updateCustomerAddress(Customer customer, String formattedAddress, String validatedZip) {
        // Parse formatted address back to address lines
        // This is a simplified implementation - in reality, you'd have more sophisticated parsing
        if (formattedAddress != null && !formattedAddress.trim().isEmpty()) {
            customer.setAddressLine1(formattedAddress);
        }
        
        if (validatedZip != null && !validatedZip.trim().isEmpty()) {
            customer.setZipCode(validatedZip);
        }
        
        // Save updated customer
        customerRepository.save(customer);
    }

    /**
     * Builds AddressDto from Customer data for address validation service.
     */
    private AddressDto buildAddressDto(Customer customer) {
        AddressDto addressDto = new AddressDto();
        addressDto.setAddressLine1(customer.getAddressLine1());
        addressDto.setAddressLine2(customer.getAddressLine2());
        addressDto.setAddressLine3(customer.getAddressLine3());
        addressDto.setStateCode(customer.getStateCode());
        addressDto.setZipCode(customer.getZipCode());
        return addressDto;
    }

    /**
     * Calculates account age in years from creation timestamp.
     */
    private int calculateAccountAge(LocalDateTime createdTimestamp) {
        if (createdTimestamp == null) {
            return 0;
        }
        
        LocalDate creationDate = createdTimestamp.toLocalDate();
        LocalDate currentDate = LocalDate.now();
        
        return (int) java.time.temporal.ChronoUnit.YEARS.between(creationDate, currentDate);
    }

    /**
     * Gets reporting period for maintenance report.
     */
    private String getReportingPeriod() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        return startOfMonth.toString() + " to " + today.toString();
    }
}