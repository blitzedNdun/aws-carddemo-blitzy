/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.batch;

import com.carddemo.config.BatchConfig;
import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.TransactionCategoryBalance;
import com.carddemo.entity.DisclosureGroup;
import com.carddemo.entity.CardXref;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.repository.TransactionCategoryBalanceRepository;
import com.carddemo.repository.DisclosureGroupRepository;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.util.CobolDataConverter;
import com.carddemo.util.DateConversionUtil;
import com.carddemo.util.AmountCalculator;
import com.carddemo.util.TransactionIdGenerator;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring Batch job implementation for monthly interest calculation replacing CBACT04C COBOL batch program.
 * 
 * This job processes transaction category balance records sequentially, groups by account ID,
 * retrieves applicable interest rates from disclosure groups, calculates monthly interest amounts,
 * generates interest transaction records, and updates account master balances.
 * 
 * COBOL Program Translation:
 * - Replaces CBACT04C batch program with identical business logic
 * - Maintains exact interest calculation formula: (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
 * - Preserves account grouping and sequential processing patterns
 * - Uses BigDecimal with HALF_UP rounding to match COBOL COMP-3 precision
 * - Implements fallback to 'DEFAULT' disclosure group when specific group not found
 * 
 * Processing Flow:
 * 1. ItemReader: Sequential read of transaction category balance records (TCATBAL-FILE equivalent)
 * 2. ItemProcessor: Interest calculation with account grouping and rate lookup (1000-3000 paragraphs)
 * 3. ItemWriter: Transaction generation and account balance updates (1300-B-WRITE-TX, 1050-UPDATE-ACCOUNT)
 * 
 * Key Features:
 * - Chunk-based processing for efficient memory usage and transaction management
 * - Account-level grouping with interest accumulation per account
 * - Disclosure group rate lookup with DEFAULT fallback logic
 * - Interest transaction generation with proper timestamps and identifiers
 * - Account balance updates with cycle credit/debit reset
 * - Comprehensive error handling and logging for operational monitoring
 * - Spring Batch restart capability for failed job recovery
 * 
 * Database Integration:
 * - Reads from transaction_category_balance table (TCATBAL-FILE replacement)
 * - Writes to transactions table (TRANSACT-FILE replacement) 
 * - Updates account_data table (ACCTFILE replacement)
 * - Lookups from disclosure_groups table (DISCGRP-FILE replacement)
 * - References card_xref table (XREF-FILE replacement)
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Component
@Profile({"!test", "!unit-test"})
public class InterestCalculationJob {

    private static final Logger logger = LoggerFactory.getLogger(InterestCalculationJob.class);

    // Constants matching COBOL program behavior
    private static final String INTEREST_TRANSACTION_TYPE = "01";        // COBOL: MOVE '01' TO TRAN-TYPE-CD
    private static final String INTEREST_CATEGORY_CODE = "05";           // COBOL: MOVE '05' TO TRAN-CAT-CD  
    private static final String SYSTEM_SOURCE = "System";                // COBOL: MOVE 'System' TO TRAN-SOURCE
    private static final String DEFAULT_GROUP_ID = "DEFAULT";            // COBOL: MOVE 'DEFAULT' TO FD-DIS-ACCT-GROUP-ID
    private static final BigDecimal INTEREST_RATE_DIVISOR = new BigDecimal("1200"); // COBOL: / 1200
    private static final int CHUNK_SIZE = 100;                           // Configurable chunk size for batch processing
    
    // Dependency injection
    @Autowired
    private JobRepository jobRepository;
    
    @Autowired
    private JobLauncher jobLauncher;
    
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    @Autowired
    private TransactionCategoryBalanceRepository transactionCategoryBalanceRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private DisclosureGroupRepository disclosureGroupRepository;
    
    @Autowired
    private CardXrefRepository cardXrefRepository;
    
    @Autowired
    private TransactionIdGenerator transactionIdGenerator;

    // Account processing state tracking
    private final Map<Long, InterestCalculationData> accountInterestMap = new ConcurrentHashMap<>();
    
    /**
     * Data holder for per-account interest calculation accumulation.
     * Maintains running totals and account information during batch processing.
     */
    private static class InterestCalculationData {
        private BigDecimal totalInterest = BigDecimal.ZERO;
        private Account account;
        private String cardNumber;
        private int transactionCount = 0;
        
        public InterestCalculationData(Account account, String cardNumber) {
            this.account = account;
            this.cardNumber = cardNumber;
        }
        
        public void addInterest(BigDecimal interest) {
            this.totalInterest = this.totalInterest.add(interest);
            this.transactionCount++;
        }
        
        // Getters
        public BigDecimal getTotalInterest() { return totalInterest; }
        public Account getAccount() { return account; }
        public String getCardNumber() { return cardNumber; }
        public int getTransactionCount() { return transactionCount; }
    }

    /**
     * Configures and returns the main interest calculation Spring Batch job.
     * 
     * This method creates the complete job definition with proper step configuration,
     * chunk-based processing, and error handling. The job processes transaction
     * category balance records and generates interest transactions matching the
     * original COBOL batch program behavior.
     * 
     * Job Configuration:
     * - Single step with reader → processor → writer pattern
     * - Chunk-based processing for efficient memory usage
     * - Transaction boundaries aligned with COBOL SYNCPOINT behavior
     * - Restart capability for failed job recovery
     * 
     * @return configured Spring Batch Job for interest calculation
     */
    public Job interestCalculationJob() {
        logger.info("Configuring interest calculation job");
        
        return new JobBuilder("interestCalculationJob", jobRepository)
                .start(interestCalculationStep())
                .build();
    }

    /**
     * Configures the interest calculation step with ItemReader, ItemProcessor, and ItemWriter.
     * 
     * This step processes transaction category balance records in chunks, calculates
     * interest amounts, and generates corresponding transaction records while updating
     * account balances. The chunk size is configured for optimal memory usage and
     * transaction management.
     * 
     * Step Configuration:
     * - Chunk-oriented processing with configurable chunk size
     * - Repository-based ItemReader for sequential data access
     * - Custom ItemProcessor for interest calculation logic
     * - Custom ItemWriter for transaction generation and account updates
     * - Transaction management with proper rollback capabilities
     * 
     * @return configured Step for interest calculation processing
     */
    public Step interestCalculationStep() {
        logger.info("Configuring interest calculation step with chunk size: {}", CHUNK_SIZE);
        
        return new StepBuilder("interestCalculationStep", jobRepository)
                .<TransactionCategoryBalance, InterestCalculationResult>chunk(CHUNK_SIZE, transactionManager)
                .reader(transactionCategoryBalanceReader())
                .processor(interestCalculationProcessor())
                .writer(interestTransactionWriter())
                .build();
    }

    /**
     * Creates ItemReader for sequential processing of transaction category balance records.
     * 
     * This reader mimics the COBOL program's sequential READ of TCATBAL-FILE, retrieving
     * transaction category balance records in account ID order for proper grouping and
     * processing. Uses Spring Data JPA repository with sorting to maintain consistent
     * processing order.
     * 
     * Reader Configuration:
     * - Repository-based reader using TransactionCategoryBalanceRepository
     * - Sorted by account ID for proper account grouping
     * - Page-based reading for memory-efficient processing
     * - No filtering - processes all records like COBOL sequential access
     * 
     * @return configured RepositoryItemReader for TransactionCategoryBalance entities
     */
    @StepScope
    public RepositoryItemReader<TransactionCategoryBalance> transactionCategoryBalanceReader() {
        logger.info("Configuring transaction category balance reader");
        
        return new RepositoryItemReaderBuilder<TransactionCategoryBalance>()
                .name("transactionCategoryBalanceReader")
                .repository(transactionCategoryBalanceRepository)
                .methodName("findAll")
                .sorts(Map.of("id.accountId", Sort.Direction.ASC))
                .pageSize(CHUNK_SIZE)
                .build();
    }

    /**
     * Creates ItemProcessor for interest calculation logic matching COBOL program flow.
     * 
     * This processor implements the core interest calculation business logic from the
     * COBOL program, including account grouping, disclosure group rate lookup with
     * DEFAULT fallback, and precise financial calculations using BigDecimal arithmetic.
     * 
     * Processing Logic:
     * 1. Group transaction category balances by account ID (like COBOL account grouping)
     * 2. For each account: retrieve account data and card cross-reference information
     * 3. Look up applicable interest rate from disclosure groups with DEFAULT fallback
     * 4. Calculate monthly interest using formula: (balance * rate) / 1200
     * 5. Accumulate interest amounts per account for balance updates
     * 6. Generate interest transaction data for ItemWriter processing
     * 
     * @return configured ItemProcessor for interest calculation
     */
    public ItemProcessor<TransactionCategoryBalance, InterestCalculationResult> interestCalculationProcessor() {
        return new ItemProcessor<TransactionCategoryBalance, InterestCalculationResult>() {
            @Override
            public InterestCalculationResult process(TransactionCategoryBalance balance) throws Exception {
                logger.debug("Processing balance record for account: {}, category: {}", 
                           balance.getAccountId(), balance.getCategoryCode());
                
                Long accountId = balance.getAccountId();
                
                // Get or create account interest calculation data
                InterestCalculationData accountData = getOrCreateAccountData(accountId);
                if (accountData == null) {
                    logger.warn("Could not retrieve account data for account ID: {}", accountId);
                    return null; // Skip this record
                }
                
                // Get interest rate from disclosure groups with DEFAULT fallback
                BigDecimal interestRate = getInterestRate(accountData.getAccount(), balance.getCategoryCode());
                
                if (interestRate.compareTo(BigDecimal.ZERO) == 0) {
                    logger.debug("Zero interest rate for account: {}, category: {}", 
                               accountId, balance.getCategoryCode());
                    return null; // Skip processing for zero rate
                }
                
                // Calculate monthly interest using COBOL formula: (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
                BigDecimal monthlyInterest = AmountCalculator.calculateMonthlyInterest(balance.getBalance(), interestRate);
                
                // Accumulate interest for account
                accountData.addInterest(monthlyInterest);
                
                logger.debug("Calculated monthly interest: {} for account: {}, category: {}, balance: {}, rate: {}", 
                           monthlyInterest, accountId, balance.getCategoryCode(), balance.getBalance(), interestRate);
                
                // Create result for ItemWriter
                return new InterestCalculationResult(accountData, balance, monthlyInterest);
            }
        };
    }

    /**
     * Creates ItemWriter for generating interest transactions and updating account balances.
     * 
     * This writer implements the COBOL program's transaction generation and account
     * update logic, creating interest transaction records and updating account master
     * balances with accumulated interest amounts.
     * 
     * Writer Operations:
     * 1. Generate interest transaction records (like COBOL 1300-B-WRITE-TX)
     * 2. Update account balances with accumulated interest (like COBOL 1050-UPDATE-ACCOUNT)
     * 3. Reset current cycle credit and debit amounts to zero
     * 4. Maintain transaction audit trail with proper timestamps
     * 
     * @return configured ItemWriter for interest transaction generation and account updates
     */
    public ItemWriter<InterestCalculationResult> interestTransactionWriter() {
        return new ItemWriter<InterestCalculationResult>() {
            @Override
            public void write(org.springframework.batch.item.Chunk<? extends InterestCalculationResult> chunk) throws Exception {
                List<? extends InterestCalculationResult> results = chunk.getItems();
                logger.info("Writing {} interest calculation results", results.size());
                
                Set<Long> processedAccounts = new HashSet<>();
                
                for (InterestCalculationResult result : results) {
                    // Generate interest transaction record
                    Transaction interestTransaction = createInterestTransaction(result);
                    transactionRepository.save(interestTransaction);
                    
                    logger.debug("Created interest transaction: {} for account: {}, amount: {}", 
                               interestTransaction.getTransactionId(), 
                               result.getAccountData().getAccount().getAccountId(),
                               result.getMonthlyInterest());
                    
                    // Track accounts for balance updates
                    processedAccounts.add(result.getAccountData().getAccount().getAccountId());
                }
                
                // Update account balances for all processed accounts
                updateAccountBalances(processedAccounts);
                
                logger.info("Completed writing interest calculations for {} accounts", processedAccounts.size());
            }
        };
    }

    /**
     * Executes the interest calculation job with optional parameters.
     * 
     * This method provides programmatic execution of the interest calculation job
     * with proper parameter handling and execution tracking. Supports both scheduled
     * execution and manual triggering through administrative interfaces.
     * 
     * Execution Features:
     * - Asynchronous job execution with completion tracking
     * - Parameter validation and default value handling
     * - Comprehensive logging for operational monitoring
     * - Exception handling with proper error reporting
     * 
     * @param executionDate optional execution date parameter (defaults to current date)
     * @return JobExecution result with completion status and metrics
     * @throws Exception if job execution fails or configuration errors occur
     */
    public JobExecution executeJob(LocalDate executionDate) throws Exception {
        logger.info("Starting interest calculation job execution for date: {}", 
                   executionDate != null ? executionDate : "current date");
        
        // Build job parameters
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("executionDate", executionDate != null ? executionDate.toString() : LocalDate.now().toString())
                .addLong("executionTime", System.currentTimeMillis())
                .toJobParameters();
        
        try {
            // Clear any previous account state
            accountInterestMap.clear();
            
            // Execute the job
            JobExecution jobExecution = jobLauncher.run(interestCalculationJob(), jobParameters);
            
            logger.info("Interest calculation job execution completed with status: {}", 
                       jobExecution.getStatus());
            
            return jobExecution;
            
        } catch (Exception e) {
            logger.error("Interest calculation job execution failed", e);
            throw e;
        }
    }

    /**
     * Retrieves or creates account interest calculation data for account grouping.
     * 
     * This method implements the COBOL program's account data retrieval logic,
     * including account master lookup and card cross-reference data gathering.
     * Maintains account state throughout batch processing for interest accumulation.
     * 
     * @param accountId the account ID to retrieve data for
     * @return InterestCalculationData containing account and card information, or null if not found
     */
    private InterestCalculationData getOrCreateAccountData(Long accountId) {
        return accountInterestMap.computeIfAbsent(accountId, id -> {
            logger.debug("Creating account data for account: {}", id);
            
            // Get account data (COBOL: 1100-GET-ACCT-DATA)
            Optional<Account> accountOpt = accountRepository.findById(id);
            if (!accountOpt.isPresent()) {
                logger.warn("Account not found: {}", id);
                return null;
            }
            
            Account account = accountOpt.get();
            
            // Get card cross-reference data (COBOL: 1110-GET-XREF-DATA)
            List<CardXref> cardXrefs = cardXrefRepository.findByXrefAcctId(id);
            String cardNumber = null;
            
            if (!cardXrefs.isEmpty()) {
                // Use the first card number found (like COBOL program)
                cardNumber = cardXrefs.get(0).getXrefCardNum();
            }
            
            if (cardNumber == null) {
                logger.warn("No card cross-reference found for account: {}", id);
                cardNumber = "0000000000000000"; // Default value like COBOL SPACES
            }
            
            logger.debug("Retrieved account data for account: {}, card: {}", id, cardNumber);
            return new InterestCalculationData(account, cardNumber);
        });
    }

    /**
     * Retrieves applicable interest rate from disclosure groups with DEFAULT fallback.
     * 
     * This method implements the COBOL program's disclosure group lookup logic,
     * including the fallback to DEFAULT group when specific group is not found.
     * Matches the exact lookup pattern from COBOL 1200-GET-INTEREST-RATE paragraph.
     * 
     * @param account the account to get interest rate for
     * @param categoryCode the transaction category code
     * @return applicable interest rate, or BigDecimal.ZERO if no rate found
     */
    private BigDecimal getInterestRate(Account account, String categoryCode) {
        String accountGroupId = account.getGroupId();
        
        logger.debug("Looking up interest rate for account group: {}, category: {}", 
                   accountGroupId, categoryCode);
        
        // First attempt: Look up by account group ID, transaction type, and category
        List<DisclosureGroup> disclosureGroups = disclosureGroupRepository.findByAccountGroupId(accountGroupId);
        
        BigDecimal interestRate = findMatchingRate(disclosureGroups, INTEREST_TRANSACTION_TYPE, categoryCode);
        
        if (interestRate != null) {
            logger.debug("Found interest rate: {} for group: {}, category: {}", 
                       interestRate, accountGroupId, categoryCode);
            return interestRate;
        }
        
        // Fallback: Try DEFAULT group (COBOL: 1200-A-GET-DEFAULT-INT-RATE)
        logger.debug("No rate found for group: {}, trying DEFAULT group", accountGroupId);
        
        List<DisclosureGroup> defaultGroups = disclosureGroupRepository.findByAccountGroupId(DEFAULT_GROUP_ID);
        interestRate = findMatchingRate(defaultGroups, INTEREST_TRANSACTION_TYPE, categoryCode);
        
        if (interestRate != null) {
            logger.debug("Found DEFAULT interest rate: {} for category: {}", interestRate, categoryCode);
            return interestRate;
        }
        
        logger.warn("No interest rate found for account group: {} or DEFAULT, category: {}", 
                   accountGroupId, categoryCode);
        return BigDecimal.ZERO;
    }

    /**
     * Finds matching interest rate from disclosure group list.
     * 
     * @param disclosureGroups list of disclosure groups to search
     * @param transactionType transaction type code to match
     * @param categoryCode transaction category code to match
     * @return matching interest rate, or null if no match found
     */
    private BigDecimal findMatchingRate(List<DisclosureGroup> disclosureGroups, 
                                       String transactionType, String categoryCode) {
        for (DisclosureGroup group : disclosureGroups) {
            if (transactionType.equals(group.getTransactionTypeCode()) && 
                categoryCode.equals(group.getTransactionCategoryCode())) {
                return group.getInterestRate();
            }
        }
        return null;
    }

    /**
     * Creates interest transaction record matching COBOL transaction generation logic.
     * 
     * This method implements the COBOL 1300-B-WRITE-TX paragraph logic, creating
     * properly formatted transaction records with all required fields and timestamps.
     * 
     * @param result the interest calculation result containing transaction details
     * @return configured Transaction entity ready for persistence
     */
    private Transaction createInterestTransaction(InterestCalculationResult result) {
        // Generate unique transaction ID (COBOL: STRING PARM-DATE, WS-TRANID-SUFFIX)
        String transactionId = transactionIdGenerator.generateTransactionId();
        
        // Get current timestamp (COBOL: Z-GET-DB2-FORMAT-TIMESTAMP)
        LocalDateTime currentTimestamp = LocalDateTime.now();
        
        // Create transaction record (transactionId will be auto-generated by database)
        Transaction transaction = Transaction.builder()
                .amount(result.getMonthlyInterest())
                .accountId(result.getAccountData().getAccount().getAccountId())
                .transactionDate(LocalDate.now())
                .description("Int. for a/c " + result.getAccountData().getAccount().getAccountId())
                .merchantId(0L)
                .merchantName("")
                .cardNumber(result.getAccountData().getCardNumber())
                .originalTimestamp(currentTimestamp)
                .processedTimestamp(currentTimestamp)
                .categoryCode(INTEREST_CATEGORY_CODE)
                .source(SYSTEM_SOURCE)
                .transactionTypeCode(INTEREST_TRANSACTION_TYPE)
                .build();
        
        return transaction;
    }

    /**
     * Updates account balances with accumulated interest amounts.
     * 
     * This method implements the COBOL 1050-UPDATE-ACCOUNT paragraph logic,
     * updating account master records with calculated interest amounts and
     * resetting cycle credit/debit amounts.
     * 
     * @param processedAccounts set of account IDs to update balances for
     */
    private void updateAccountBalances(Set<Long> processedAccounts) {
        logger.info("Updating account balances for {} accounts", processedAccounts.size());
        
        for (Long accountId : processedAccounts) {
            InterestCalculationData accountData = accountInterestMap.get(accountId);
            if (accountData != null && accountData.getTotalInterest().compareTo(BigDecimal.ZERO) > 0) {
                Account account = accountData.getAccount();
                
                // Add total interest to current balance (COBOL: ADD WS-TOTAL-INT TO ACCT-CURR-BAL)
                BigDecimal newBalance = account.getCurrentBalance().add(accountData.getTotalInterest());
                account.setCurrentBalance(newBalance);
                
                // Reset cycle credit and debit amounts (COBOL: MOVE 0 TO ACCT-CURR-CYC-CREDIT/DEBIT)
                account.setCurrentCycleCredit(BigDecimal.ZERO);
                account.setCurrentCycleDebit(BigDecimal.ZERO);
                
                // Save updated account
                accountRepository.save(account);
                
                logger.debug("Updated account: {}, new balance: {}, interest added: {}", 
                           accountId, newBalance, accountData.getTotalInterest());
            }
        }
        
        logger.info("Completed account balance updates");
    }

    /**
     * Result holder for interest calculation processing.
     * Contains all data needed for transaction generation and account updates.
     */
    private static class InterestCalculationResult {
        private final InterestCalculationData accountData;
        private final TransactionCategoryBalance balance;
        private final BigDecimal monthlyInterest;
        
        public InterestCalculationResult(InterestCalculationData accountData,
                                       TransactionCategoryBalance balance,
                                       BigDecimal monthlyInterest) {
            this.accountData = accountData;
            this.balance = balance;
            this.monthlyInterest = monthlyInterest;
        }
        
        public InterestCalculationData getAccountData() { return accountData; }
        public TransactionCategoryBalance getBalance() { return balance; }
        public BigDecimal getMonthlyInterest() { return monthlyInterest; }
    }
}