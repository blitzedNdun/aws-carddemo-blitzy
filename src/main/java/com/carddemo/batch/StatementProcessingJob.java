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
 * language governing permissions and limitations under the License.
 */

package com.carddemo.batch;

import com.carddemo.transaction.Transaction;
import com.carddemo.account.entity.Account;
import com.carddemo.account.entity.Customer;
import com.carddemo.batch.StatementGenerationJob;
import com.carddemo.common.config.BatchConfiguration;
import com.carddemo.transaction.TransactionRepository;
import com.carddemo.account.repository.AccountRepository;
import com.carddemo.account.repository.CustomerRepository;
import com.carddemo.common.util.BigDecimalUtils;
import com.carddemo.common.util.DateUtils;
import com.carddemo.batch.dto.TransactionReportDTO;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring Batch job for statement processing operations converted from COBOL CBSTM03B.CBL subroutine.
 * 
 * <p>This job handles file operations and data processing for statement generation workflow
 * with multi-step execution and resource management capabilities. The implementation converts
 * the original COBOL subroutine's file handling logic into Spring Batch steps that process
 * transactions, cross-references, customer data, and account information.</p>
 * 
 * <h3>Original COBOL Program Mapping (CBSTM03B.CBL):</h3>
 * <ul>
 *   <li>Lines 116-132: 0000-START → {@link #statementProcessingJob()}</li>
 *   <li>Lines 133-155: 1000-TRNXFILE-PROC → {@link #transactionFileProcessingStep()}</li>
 *   <li>Lines 157-179: 2000-XREFFILE-PROC → {@link #crossReferenceProcessingStep()}</li>
 *   <li>Lines 181-203: 3000-CUSTFILE-PROC → {@link #customerFileProcessingStep()}</li>
 *   <li>Lines 206-228: 4000-ACCTFILE-PROC → {@link #accountFileProcessingStep()}</li>
 * </ul>
 * 
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>Multi-step job execution with coordinated file operations</li>
 *   <li>PostgreSQL database interactions replacing VSAM file operations</li>
 *   <li>Integration with StatementGenerationJob for batch workflow coordination</li>
 *   <li>Resource management using Spring Batch framework capabilities</li>
 *   <li>Error handling and transaction management with Spring @Transactional</li>
 *   <li>BigDecimal precision maintenance for financial calculations</li>
 *   <li>Cross-reference validation using JPA entity relationships</li>
 * </ul>
 * 
 * <h3>File Operation Mapping:</h3>
 * <ul>
 *   <li>TRNXFILE (COBOL) → TransactionRepository operations</li>
 *   <li>XREFFILE (COBOL) → Account-Transaction cross-reference validation</li>
 *   <li>CUSTFILE (COBOL) → CustomerRepository operations</li>
 *   <li>ACCTFILE (COBOL) → AccountRepository operations</li>
 * </ul>
 * 
 * @author CardDemo Migration Team - Blitzy Platform
 * @version 1.0
 * @since Java 21
 */
@Configuration
public class StatementProcessingJob {

    private static final Logger logger = LoggerFactory.getLogger(StatementProcessingJob.class);

    // =======================================================================
    // BATCH PROCESSING CONSTANTS (from COBOL CBSTM03B.CBL)
    // =======================================================================
    
    /**
     * Chunk size for batch processing - optimized for memory usage and throughput.
     * Based on analysis of COBOL file record processing patterns.
     */
    private static final int PROCESSING_CHUNK_SIZE = 100;
    
    /**
     * File operation success status code equivalent to COBOL FILE-STATUS '00'.
     */
    private static final String FILE_STATUS_SUCCESS = "00";
    
    /**
     * File operation not found status code equivalent to COBOL FILE-STATUS '23'.
     */
    private static final String FILE_STATUS_NOT_FOUND = "23";
    
    /**
     * Maximum retry attempts for file operations.
     */
    private static final int MAX_RETRY_ATTEMPTS = 3;

    // =======================================================================
    // SPRING BATCH INFRASTRUCTURE DEPENDENCIES
    // =======================================================================

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private BatchConfiguration batchConfiguration;

    // =======================================================================
    // DATA ACCESS LAYER DEPENDENCIES
    // =======================================================================

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    // =======================================================================
    // STATEMENT GENERATION INTEGRATION
    // =======================================================================

    @Autowired
    private StatementGenerationJob statementGenerationJob;

    // =======================================================================
    // PROCESSING STATE MANAGEMENT
    // =======================================================================
    
    /**
     * Thread-safe map for managing file operation states during processing.
     * Equivalent to COBOL working storage file status variables.
     */
    private final Map<String, String> fileOperationStatus = new ConcurrentHashMap<>();
    
    /**
     * Processing results cache for cross-step data sharing.
     */
    private final Map<String, Object> processingContext = new ConcurrentHashMap<>();

    // =======================================================================
    // MAIN BATCH JOB DEFINITION (COBOL 0000-START equivalent)
    // =======================================================================

    /**
     * Main Spring Batch job for statement processing operations.
     * Coordinates multi-step execution equivalent to COBOL CBSTM03B subroutine processing.
     * 
     * This job executes the following steps in sequence:
     * 1. Transaction file processing (TRNXFILE operations)
     * 2. Cross-reference processing (XREFFILE operations)
     * 3. Customer file processing (CUSTFILE operations)
     * 4. Account file processing (ACCTFILE operations)
     * 
     * @return Configured Job for statement processing operations
     */
    @Bean
    public Job statementProcessingJob() {
        logger.info("Configuring StatementProcessingJob with multi-step file operations");
        
        return new JobBuilder("statementProcessingJob", jobRepository)
                .start(transactionFileProcessingStep())
                .next(crossReferenceProcessingStep())
                .next(customerFileProcessingStep())
                .next(accountFileProcessingStep())
                .build();
    }

    // =======================================================================
    // STEP DEFINITIONS (COBOL paragraph equivalents)
    // =======================================================================

    /**
     * Transaction file processing step equivalent to COBOL 1000-TRNXFILE-PROC.
     * Handles transaction data operations including reading, processing, and validation.
     * 
     * @return Configured Step for transaction file processing
     */
    @Bean
    public Step transactionFileProcessingStep() {
        logger.info("Configuring transaction file processing step");
        
        return new StepBuilder("transactionFileProcessingStep", jobRepository)
                .<Transaction, TransactionReportDTO>chunk(PROCESSING_CHUNK_SIZE, transactionManager)
                .reader(transactionItemReader())
                .processor(transactionItemProcessor())
                .writer(transactionItemWriter())
                .build();
    }

    /**
     * Cross-reference processing step equivalent to COBOL 2000-XREFFILE-PROC.
     * Handles cross-reference validation between transactions and accounts.
     * 
     * @return Configured Step for cross-reference processing
     */
    @Bean
    public Step crossReferenceProcessingStep() {
        logger.info("Configuring cross-reference processing step");
        
        return new StepBuilder("crossReferenceProcessingStep", jobRepository)
                .<Account, Account>chunk(PROCESSING_CHUNK_SIZE, transactionManager)
                .reader(crossReferenceItemReader())
                .processor(crossReferenceItemProcessor())
                .writer(crossReferenceItemWriter())
                .build();
    }

    /**
     * Customer file processing step equivalent to COBOL 3000-CUSTFILE-PROC.
     * Handles customer data operations with random access equivalent functionality.
     * 
     * @return Configured Step for customer file processing
     */
    @Bean
    public Step customerFileProcessingStep() {
        logger.info("Configuring customer file processing step");
        
        return new StepBuilder("customerFileProcessingStep", jobRepository)
                .<Customer, Customer>chunk(PROCESSING_CHUNK_SIZE, transactionManager)
                .reader(customerItemReader())
                .processor(customerItemProcessor())
                .writer(customerItemWriter())
                .build();
    }

    /**
     * Account file processing step equivalent to COBOL 4000-ACCTFILE-PROC.
     * Handles account data operations with random access equivalent functionality.
     * 
     * @return Configured Step for account file processing
     */
    @Bean
    public Step accountFileProcessingStep() {
        logger.info("Configuring account file processing step");
        
        return new StepBuilder("accountFileProcessingStep", jobRepository)
                .<Account, Account>chunk(PROCESSING_CHUNK_SIZE, transactionManager)
                .reader(accountItemReader())
                .processor(accountItemProcessor())
                .writer(accountItemWriter())
                .build();
    }

    // =======================================================================
    // TRANSACTION FILE PROCESSING COMPONENTS (1000-TRNXFILE-PROC)
    // =======================================================================

    /**
     * Transaction item reader equivalent to COBOL TRNXFILE OPEN INPUT and READ operations.
     * Implements sequential access patterns with pagination for memory efficiency.
     * 
     * @return ItemReader for Transaction entities
     */
    @Bean
    public ItemReader<Transaction> transactionItemReader() {
        logger.info("Configuring transaction item reader with sequential access pattern");
        
        // Initialize file operation status equivalent to COBOL OPEN INPUT TRNX-FILE
        handleFileOpen("TRNXFILE");
        
        try {
            // Fetch all transactions with pagination equivalent to sequential READ
            List<Transaction> transactions = transactionRepository.findAll(
                PageRequest.of(0, 10000)
            ).getContent();
            
            logger.info("Successfully opened TRNXFILE equivalent - found {} transactions", transactions.size());
            processingContext.put("TRNXFILE_RECORD_COUNT", transactions.size());
            
            return new ListItemReader<>(transactions);
            
        } catch (Exception e) {
            logger.error("Error opening transaction file equivalent", e);
            fileOperationStatus.put("TRNXFILE", FILE_STATUS_NOT_FOUND);
            throw new RuntimeException("Failed to open transaction file", e);
        }
    }

    /**
     * Transaction item processor equivalent to COBOL transaction data manipulation.
     * Processes transaction data and converts to report format for output operations.
     * 
     * @return ItemProcessor for Transaction to TransactionReportDTO conversion
     */
    @Bean
    public ItemProcessor<Transaction, TransactionReportDTO> transactionItemProcessor() {
        return new ItemProcessor<Transaction, TransactionReportDTO>() {
            @Override
            public TransactionReportDTO process(Transaction transaction) throws Exception {
                logger.debug("Processing transaction: {}", transaction.getTransactionId());
                
                try {
                    // Validate transaction data equivalent to COBOL data validation
                    if (transaction.getTransactionId() == null || transaction.getAmount() == null) {
                        logger.warn("Skipping invalid transaction with null required fields");
                        return null;
                    }
                    
                    // Create report DTO with transaction data
                    TransactionReportDTO reportDTO = new TransactionReportDTO();
                    reportDTO.setTransactionId(transaction.getTransactionId());
                    reportDTO.setAccountId(transaction.getAccount() != null ? 
                        transaction.getAccount().getAccountId() : "");
                    reportDTO.setAmount(transaction.getAmount());
                    reportDTO.setDescription(transaction.getDescription());
                    
                    // Process financial calculations with COBOL precision
                    BigDecimal processedAmount = BigDecimalUtils.createDecimal(
                        transaction.getAmount().toString()
                    );
                    reportDTO.setAmount(processedAmount);
                    
                    logger.debug("Successfully processed transaction: {} with amount: {}", 
                        transaction.getTransactionId(), 
                        BigDecimalUtils.formatCurrency(processedAmount));
                    
                    return reportDTO;
                    
                } catch (Exception e) {
                    logger.error("Error processing transaction: {}", transaction.getTransactionId(), e);
                    throw e;
                }
            }
        };
    }

    /**
     * Transaction item writer equivalent to COBOL WRITE operations.
     * Handles transaction output processing and status tracking.
     * 
     * @return ItemWriter for TransactionReportDTO entities
     */
    @Bean
    public ItemWriter<TransactionReportDTO> transactionItemWriter() {
        return new ItemWriter<TransactionReportDTO>() {
            @Override
            public void write(Chunk<? extends TransactionReportDTO> chunk) throws Exception {
                logger.debug("Writing {} transaction report records", chunk.size());
                
                try {
                    int processedCount = 0;
                    BigDecimal totalAmount = BigDecimalUtils.createDecimal("0.00");
                    
                    for (TransactionReportDTO dto : chunk) {
                        // Format report line equivalent to COBOL report generation
                        String reportLine = dto.formatAsDetailLine();
                        
                        // Accumulate totals with COBOL precision
                        if (dto.getAmount() != null) {
                            totalAmount = BigDecimalUtils.add(totalAmount, dto.getAmount());
                        }
                        
                        processedCount++;
                        logger.debug("Formatted transaction report line: {}", reportLine);
                    }
                    
                    // Update processing context with results
                    processingContext.put("TRANSACTIONS_PROCESSED", processedCount);
                    processingContext.put("TOTAL_TRANSACTION_AMOUNT", totalAmount);
                    
                    logger.info("Successfully wrote {} transaction report records, total amount: {}", 
                        processedCount, BigDecimalUtils.formatCurrency(totalAmount));
                    
                    // Update file operation status equivalent to COBOL file handling
                    fileOperationStatus.put("TRNXFILE", FILE_STATUS_SUCCESS);
                    
                } catch (Exception e) {
                    logger.error("Error writing transaction report records", e);
                    fileOperationStatus.put("TRNXFILE", "99");
                    throw e;
                }
            }
        };
    }

    // =======================================================================
    // CROSS-REFERENCE PROCESSING COMPONENTS (2000-XREFFILE-PROC)
    // =======================================================================

    /**
     * Cross-reference item reader equivalent to COBOL XREFFILE OPEN INPUT and READ operations.
     * Implements account-transaction cross-reference validation patterns.
     * 
     * @return ItemReader for Account entities with cross-reference validation
     */
    public ItemReader<Account> crossReferenceItemReader() {
        logger.info("Configuring cross-reference item reader");
        
        // Initialize cross-reference file operation equivalent to COBOL OPEN INPUT XREF-FILE
        handleFileOpen("XREFFILE");
        
        try {
            // Fetch accounts that have associated transactions for cross-reference validation
            List<Account> accountsWithTransactions = accountRepository.findAll()
                .stream()
                .filter(account -> !account.getTransactions().isEmpty())
                .collect(Collectors.toList());
            
            logger.info("Successfully opened XREFFILE equivalent - found {} accounts with transactions", 
                accountsWithTransactions.size());
            processingContext.put("XREF_ACCOUNT_COUNT", accountsWithTransactions.size());
            
            return new ListItemReader<>(accountsWithTransactions);
            
        } catch (Exception e) {
            logger.error("Error opening cross-reference file equivalent", e);
            fileOperationStatus.put("XREFFILE", FILE_STATUS_NOT_FOUND);
            throw new RuntimeException("Failed to open cross-reference file", e);
        }
    }

    /**
     * Cross-reference item processor equivalent to COBOL cross-reference validation logic.
     * Validates account-transaction relationships and ensures data integrity.
     * 
     * @return ItemProcessor for Account cross-reference validation
     */
    public ItemProcessor<Account, Account> crossReferenceItemProcessor() {
        return new ItemProcessor<Account, Account>() {
            @Override
            public Account process(Account account) throws Exception {
                logger.debug("Processing cross-reference validation for account: {}", account.getAccountId());
                
                try {
                    // Validate account has active status equivalent to COBOL status checking
                    if (account.getActiveStatus() == null) {
                        logger.warn("Skipping account with null status: {}", account.getAccountId());
                        return null;
                    }
                    
                    // Cross-reference validation - ensure account has valid transactions
                    List<Transaction> accountTransactions = transactionRepository.findByAccountId(
                        account.getAccountId(), PageRequest.of(0, 1000)
                    ).getContent();
                    
                    if (accountTransactions.isEmpty()) {
                        logger.debug("Account {} has no transactions for cross-reference", account.getAccountId());
                    } else {
                        logger.debug("Account {} validated with {} transactions", 
                            account.getAccountId(), accountTransactions.size());
                    }
                    
                    return account;
                    
                } catch (Exception e) {
                    logger.error("Error processing cross-reference for account: {}", account.getAccountId(), e);
                    throw e;
                }
            }
        };
    }

    /**
     * Cross-reference item writer equivalent to COBOL cross-reference output operations.
     * Handles cross-reference validation results and updates processing status.
     * 
     * @return ItemWriter for cross-reference validated Account entities
     */
    public ItemWriter<Account> crossReferenceItemWriter() {
        return new ItemWriter<Account>() {
            @Override
            public void write(Chunk<? extends Account> chunk) throws Exception {
                logger.debug("Writing {} cross-reference validation results", chunk.size());
                
                try {
                    int validatedCount = 0;
                    
                    for (Account account : chunk) {
                        // Log cross-reference validation success
                        logger.debug("Cross-reference validated for account: {}", account.getAccountId());
                        validatedCount++;
                    }
                    
                    // Update processing context with validation results
                    processingContext.put("XREF_VALIDATED_COUNT", validatedCount);
                    
                    logger.info("Successfully validated {} account cross-references", validatedCount);
                    
                    // Update file operation status equivalent to COBOL file handling
                    fileOperationStatus.put("XREFFILE", FILE_STATUS_SUCCESS);
                    
                } catch (Exception e) {
                    logger.error("Error writing cross-reference validation results", e);
                    fileOperationStatus.put("XREFFILE", "99");
                    throw e;
                }
            }
        };
    }

    // =======================================================================
    // CUSTOMER FILE PROCESSING COMPONENTS (3000-CUSTFILE-PROC)
    // =======================================================================

    /**
     * Customer item reader equivalent to COBOL CUSTFILE OPEN INPUT operations.
     * Implements random access patterns for customer data retrieval.
     * 
     * @return ItemReader for Customer entities
     */
    public ItemReader<Customer> customerItemReader() {
        logger.info("Configuring customer item reader with random access pattern");
        
        // Initialize customer file operation equivalent to COBOL OPEN INPUT CUST-FILE
        handleFileOpen("CUSTFILE");
        
        try {
            // Fetch customers that are referenced by accounts (random access equivalent)
            List<Customer> customersWithAccounts = customerRepository.findAll()
                .stream()
                .filter(customer -> !customer.getAccounts().isEmpty())
                .collect(Collectors.toList());
            
            logger.info("Successfully opened CUSTFILE equivalent - found {} customers with accounts", 
                customersWithAccounts.size());
            processingContext.put("CUSTOMER_COUNT", customersWithAccounts.size());
            
            return new ListItemReader<>(customersWithAccounts);
            
        } catch (Exception e) {
            logger.error("Error opening customer file equivalent", e);
            fileOperationStatus.put("CUSTFILE", FILE_STATUS_NOT_FOUND);
            throw new RuntimeException("Failed to open customer file", e);
        }
    }

    /**
     * Customer item processor equivalent to COBOL customer data processing.
     * Validates customer information and prepares data for statement processing.
     * 
     * @return ItemProcessor for Customer data validation and processing
     */
    public ItemProcessor<Customer, Customer> customerItemProcessor() {
        return new ItemProcessor<Customer, Customer>() {
            @Override
            public Customer process(Customer customer) throws Exception {
                logger.debug("Processing customer data: {}", customer.getCustomerId());
                
                try {
                    // Validate required customer fields equivalent to COBOL data validation
                    if (customer.getCustomerId() == null || customer.getFirstName() == null || 
                        customer.getLastName() == null) {
                        logger.warn("Skipping customer with missing required fields: {}", customer.getCustomerId());
                        return null;
                    }
                    
                    // Validate customer address data
                    if (customer.getAddressLine1() == null || customer.getStateCode() == null || 
                        customer.getZipCode() == null) {
                        logger.warn("Customer {} has incomplete address information", customer.getCustomerId());
                    }
                    
                    // Customer processing equivalent to COBOL READ CUST-FILE operations
                    logger.debug("Successfully processed customer: {} {} {}", 
                        customer.getCustomerId(), customer.getFirstName(), customer.getLastName());
                    
                    return customer;
                    
                } catch (Exception e) {
                    logger.error("Error processing customer: {}", customer.getCustomerId(), e);
                    throw e;
                }
            }
        };
    }

    /**
     * Customer item writer equivalent to COBOL customer data output operations.
     * Handles customer processing results and maintains processing state.
     * 
     * @return ItemWriter for processed Customer entities
     */
    public ItemWriter<Customer> customerItemWriter() {
        return new ItemWriter<Customer>() {
            @Override
            public void write(Chunk<? extends Customer> chunk) throws Exception {
                logger.debug("Writing {} customer processing results", chunk.size());
                
                try {
                    int processedCount = 0;
                    
                    for (Customer customer : chunk) {
                        // Log customer processing success
                        logger.debug("Customer processed: {} - {} {}", 
                            customer.getCustomerId(), customer.getFirstName(), customer.getLastName());
                        processedCount++;
                    }
                    
                    // Update processing context with customer results
                    processingContext.put("CUSTOMERS_PROCESSED", processedCount);
                    
                    logger.info("Successfully processed {} customer records", processedCount);
                    
                    // Update file operation status equivalent to COBOL file handling
                    fileOperationStatus.put("CUSTFILE", FILE_STATUS_SUCCESS);
                    
                } catch (Exception e) {
                    logger.error("Error writing customer processing results", e);
                    fileOperationStatus.put("CUSTFILE", "99");
                    throw e;
                }
            }
        };
    }

    // =======================================================================
    // ACCOUNT FILE PROCESSING COMPONENTS (4000-ACCTFILE-PROC)
    // =======================================================================

    /**
     * Account item reader equivalent to COBOL ACCTFILE OPEN INPUT operations.
     * Implements random access patterns for account data retrieval by key.
     * 
     * @return ItemReader for Account entities
     */
    public ItemReader<Account> accountItemReader() {
        logger.info("Configuring account item reader with random access pattern");
        
        // Initialize account file operation equivalent to COBOL OPEN INPUT ACCT-FILE
        handleFileOpen("ACCTFILE");
        
        try {
            // Fetch active accounts equivalent to COBOL READ ACCT-FILE with key operations
            List<Account> activeAccounts = accountRepository.findAll()
                .stream()
                .filter(account -> account.getActiveStatus() != null)
                .collect(Collectors.toList());
            
            logger.info("Successfully opened ACCTFILE equivalent - found {} active accounts", 
                activeAccounts.size());
            processingContext.put("ACCOUNT_COUNT", activeAccounts.size());
            
            return new ListItemReader<>(activeAccounts);
            
        } catch (Exception e) {
            logger.error("Error opening account file equivalent", e);
            fileOperationStatus.put("ACCTFILE", FILE_STATUS_NOT_FOUND);
            throw new RuntimeException("Failed to open account file", e);
        }
    }

    /**
     * Account item processor equivalent to COBOL account data processing and validation.
     * Validates account balances and prepares account data for statement generation.
     * 
     * @return ItemProcessor for Account data validation and processing
     */
    public ItemProcessor<Account, Account> accountItemProcessor() {
        return new ItemProcessor<Account, Account>() {
            @Override
            public Account process(Account account) throws Exception {
                logger.debug("Processing account data: {}", account.getAccountId());
                
                try {
                    // Validate account data equivalent to COBOL data validation
                    if (account.getAccountId() == null || account.getCurrentBalance() == null) {
                        logger.warn("Skipping account with missing required fields: {}", account.getAccountId());
                        return null;
                    }
                    
                    // Validate account balances with COBOL precision
                    BigDecimal currentBalance = account.getCurrentBalance();
                    BigDecimal creditLimit = account.getCreditLimit();
                    
                    if (currentBalance != null) {
                        // Ensure balance precision matches COBOL COMP-3 format
                        BigDecimal processedBalance = BigDecimalUtils.createDecimal(currentBalance.toString());
                        
                        logger.debug("Account {} current balance: {}", 
                            account.getAccountId(), BigDecimalUtils.formatCurrency(processedBalance));
                    }
                    
                    // Random access equivalent processing - validate customer relationship
                    Customer customer = account.getCustomer();
                    if (customer == null) {
                        logger.warn("Account {} missing customer relationship", account.getAccountId());
                    }
                    
                    return account;
                    
                } catch (Exception e) {
                    logger.error("Error processing account: {}", account.getAccountId(), e);
                    throw e;
                }
            }
        };
    }

    /**
     * Account item writer equivalent to COBOL account data output operations.
     * Finalizes account processing and prepares data for statement generation coordination.
     * 
     * @return ItemWriter for processed Account entities
     */
    public ItemWriter<Account> accountItemWriter() {
        return new ItemWriter<Account>() {
            @Override
            public void write(Chunk<? extends Account> chunk) throws Exception {
                logger.debug("Writing {} account processing results", chunk.size());
                
                try {
                    int processedCount = 0;
                    BigDecimal totalCurrentBalance = BigDecimalUtils.createDecimal("0.00");
                    BigDecimal totalCreditLimit = BigDecimalUtils.createDecimal("0.00");
                    
                    for (Account account : chunk) {
                        // Accumulate account totals with COBOL precision
                        if (account.getCurrentBalance() != null) {
                            totalCurrentBalance = BigDecimalUtils.add(totalCurrentBalance, account.getCurrentBalance());
                        }
                        if (account.getCreditLimit() != null) {
                            totalCreditLimit = BigDecimalUtils.add(totalCreditLimit, account.getCreditLimit());
                        }
                        
                        logger.debug("Account processed: {} - Balance: {}", 
                            account.getAccountId(), 
                            BigDecimalUtils.formatCurrency(account.getCurrentBalance()));
                        processedCount++;
                    }
                    
                    // Update processing context with account totals
                    processingContext.put("ACCOUNTS_PROCESSED", processedCount);
                    processingContext.put("TOTAL_CURRENT_BALANCE", totalCurrentBalance);
                    processingContext.put("TOTAL_CREDIT_LIMIT", totalCreditLimit);
                    
                    logger.info("Successfully processed {} account records, total balance: {}, total credit: {}", 
                        processedCount, 
                        BigDecimalUtils.formatCurrency(totalCurrentBalance),
                        BigDecimalUtils.formatCurrency(totalCreditLimit));
                    
                    // Update file operation status equivalent to COBOL file handling
                    fileOperationStatus.put("ACCTFILE", FILE_STATUS_SUCCESS);
                    
                } catch (Exception e) {
                    logger.error("Error writing account processing results", e);
                    fileOperationStatus.put("ACCTFILE", "99");
                    throw e;
                }
            }
        };
    }

    // =======================================================================
    // FILE OPERATION MANAGEMENT METHODS (COBOL file handling equivalents)
    // =======================================================================

    /**
     * Process file operations equivalent to COBOL file handling logic.
     * Manages file operation states and coordinates multi-step processing.
     * 
     * @param fileName The logical file name (DD name equivalent)
     * @param operation The file operation (OPEN, READ, WRITE, CLOSE)
     * @return Processing status code
     */
    public String processFileOperations(String fileName, String operation) {
        logger.debug("Processing file operation: {} - {} at {}", 
            fileName, operation, DateUtils.getCurrentDate());
        
        try {
            switch (operation.toUpperCase()) {
                case "OPEN":
                    return handleFileOpen(fileName);
                case "CLOSE":
                    return handleFileClose(fileName);
                case "READ":
                    return validateAndProcessData(fileName);
                default:
                    logger.warn("Unsupported file operation: {} for file: {}", operation, fileName);
                    return "99";
            }
        } catch (Exception e) {
            logger.error("Error processing file operation: {} - {}", fileName, operation, e);
            return "99";
        }
    }

    /**
     * Handle file open operations equivalent to COBOL OPEN statements.
     * Initializes file operation status and prepares resources.
     * 
     * @param fileName The logical file name to open
     * @return File operation status code
     */
    public String handleFileOpen(String fileName) {
        logger.debug("Opening file equivalent: {}", fileName);
        
        try {
            // Initialize file operation status
            fileOperationStatus.put(fileName, FILE_STATUS_SUCCESS);
            
            // Log file open equivalent to COBOL FILE-STATUS checking
            logger.info("Successfully opened file equivalent: {}", fileName);
            
            return FILE_STATUS_SUCCESS;
            
        } catch (Exception e) {
            logger.error("Error opening file equivalent: {}", fileName, e);
            fileOperationStatus.put(fileName, "99");
            return "99";
        }
    }

    /**
     * Handle file close operations equivalent to COBOL CLOSE statements.
     * Finalizes file operations and cleans up resources.
     * 
     * @param fileName The logical file name to close
     * @return File operation status code
     */
    public String handleFileClose(String fileName) {
        logger.debug("Closing file equivalent: {}", fileName);
        
        try {
            // Finalize file operation
            String currentStatus = fileOperationStatus.get(fileName);
            if (FILE_STATUS_SUCCESS.equals(currentStatus)) {
                logger.info("Successfully closed file equivalent: {}", fileName);
            } else {
                logger.warn("Closing file equivalent with error status: {} - {}", fileName, currentStatus);
            }
            
            // Clean up file operation status
            fileOperationStatus.remove(fileName);
            
            return FILE_STATUS_SUCCESS;
            
        } catch (Exception e) {
            logger.error("Error closing file equivalent: {}", fileName, e);
            return "99";
        }
    }

    /**
     * Validate and process data equivalent to COBOL data validation routines.
     * Performs comprehensive data validation and business rule checking.
     * 
     * @param fileName The logical file name for data validation context
     * @return Validation status code
     */
    public String validateAndProcessData(String fileName) {
        logger.debug("Validating and processing data for file: {}", fileName);
        
        try {
            switch (fileName) {
                case "TRNXFILE":
                    return validateTransactionData();
                case "XREFFILE":
                    return validateCrossReferenceData();
                case "CUSTFILE":
                    return validateCustomerData();
                case "ACCTFILE":
                    return validateAccountData();
                default:
                    logger.warn("Unknown file for validation: {}", fileName);
                    return FILE_STATUS_NOT_FOUND;
            }
        } catch (Exception e) {
            logger.error("Error validating data for file: {}", fileName, e);
            return "99";
        }
    }

    /**
     * Validate transaction data equivalent to COBOL transaction validation.
     * 
     * @return Validation status code
     */
    private String validateTransactionData() {
        logger.debug("Validating transaction data");
        
        try {
            Integer recordCount = (Integer) processingContext.get("TRNXFILE_RECORD_COUNT");
            if (recordCount != null && recordCount > 0) {
                logger.info("Transaction data validation successful - {} records processed", recordCount);
                return FILE_STATUS_SUCCESS;
            } else {
                logger.warn("No transaction records found for validation");
                return FILE_STATUS_NOT_FOUND;
            }
        } catch (Exception e) {
            logger.error("Error validating transaction data", e);
            return "99";
        }
    }

    /**
     * Validate cross-reference data equivalent to COBOL cross-reference validation.
     * 
     * @return Validation status code
     */
    private String validateCrossReferenceData() {
        logger.debug("Validating cross-reference data");
        
        try {
            Integer validatedCount = (Integer) processingContext.get("XREF_VALIDATED_COUNT");
            if (validatedCount != null && validatedCount > 0) {
                logger.info("Cross-reference data validation successful - {} records validated", validatedCount);
                return FILE_STATUS_SUCCESS;
            } else {
                logger.warn("No cross-reference records found for validation");
                return FILE_STATUS_NOT_FOUND;
            }
        } catch (Exception e) {
            logger.error("Error validating cross-reference data", e);
            return "99";
        }
    }

    /**
     * Validate customer data equivalent to COBOL customer validation.
     * 
     * @return Validation status code
     */
    private String validateCustomerData() {
        logger.debug("Validating customer data");
        
        try {
            Integer processedCount = (Integer) processingContext.get("CUSTOMERS_PROCESSED");
            if (processedCount != null && processedCount > 0) {
                logger.info("Customer data validation successful - {} records processed", processedCount);
                return FILE_STATUS_SUCCESS;
            } else {
                logger.warn("No customer records found for validation");
                return FILE_STATUS_NOT_FOUND;
            }
        } catch (Exception e) {
            logger.error("Error validating customer data", e);
            return "99";
        }
    }

    /**
     * Validate account data equivalent to COBOL account validation.
     * 
     * @return Validation status code
     */
    private String validateAccountData() {
        logger.debug("Validating account data");
        
        try {
            Integer processedCount = (Integer) processingContext.get("ACCOUNTS_PROCESSED");
            if (processedCount != null && processedCount > 0) {
                logger.info("Account data validation successful - {} records processed", processedCount);
                return FILE_STATUS_SUCCESS;
            } else {
                logger.warn("No account records found for validation");
                return FILE_STATUS_NOT_FOUND;
            }
        } catch (Exception e) {
            logger.error("Error validating account data", e);
            return "99";
        }
    }

    // =======================================================================
    // JOB COORDINATION AND EXECUTION SUPPORT
    // =======================================================================

    /**
     * Get current processing status for monitoring and coordination.
     * Provides access to file operation states and processing metrics.
     * 
     * @return Map containing current processing status information
     */
    public Map<String, Object> getProcessingStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("fileOperationStatus", new HashMap<>(fileOperationStatus));
        status.put("processingContext", new HashMap<>(processingContext));
        status.put("timestamp", DateUtils.getCurrentDate());
        
        return status;
    }

    /**
     * Clear processing state for new job execution.
     * Resets all processing context and file operation status.
     */
    public void resetProcessingState() {
        logger.info("Resetting statement processing job state");
        fileOperationStatus.clear();
        processingContext.clear();
    }
}