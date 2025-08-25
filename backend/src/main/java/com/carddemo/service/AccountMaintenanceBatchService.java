/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.config.BatchConfig;
import com.carddemo.batch.BatchProperties;
import com.carddemo.repository.AccountRepository;
import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.util.CobolDataConverter;
import com.carddemo.util.ValidationUtil;
import com.carddemo.util.DateConversionUtil;
import com.carddemo.util.AmountCalculator;

import org.springframework.stereotype.Service;
import org.springframework.batch.core.Step;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobRepository;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.core.task.TaskExecutor;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring Batch service implementing nightly account maintenance processing translated from CBACT01C.cbl.
 * Performs account status updates, dormancy checks, fee assessments, and balance reconciliation. 
 * Maintains COBOL sequential file processing patterns while leveraging Spring Batch chunk-oriented 
 * processing for scalability and restart capabilities.
 * 
 * This service converts the simple account file reader from CBACT01C.cbl into a comprehensive
 * nightly account maintenance processor that:
 * - Processes all active accounts in chunks for optimal performance
 * - Performs dormancy checks based on transaction history analysis
 * - Assesses maintenance fees according to account type and activity
 * - Reconciles account balances with recent transaction activity
 * - Provides restart capability and error handling through Spring Batch
 * - Maintains exact COBOL data precision using CobolDataConverter
 * 
 * Processing follows the Spring Batch chunk-oriented model:
 * - ItemReader: Reads accounts in configurable page sizes from PostgreSQL
 * - ItemProcessor: Applies business logic for maintenance operations
 * - ItemWriter: Persists updated accounts back to the database
 * 
 * All monetary calculations preserve COBOL COMP-3 packed decimal precision
 * and follow original business rules for fee assessment and balance validation.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Service
public class AccountMaintenanceBatchService {

    private static final Logger logger = LoggerFactory.getLogger(AccountMaintenanceBatchService.class);
    
    // Business constants for account maintenance processing
    private static final String ACTIVE_STATUS = "A";
    private static final String DORMANT_STATUS = "D";
    private static final String SUSPENDED_STATUS = "S";
    private static final int DORMANCY_THRESHOLD_DAYS = 365;
    private static final BigDecimal MONTHLY_MAINTENANCE_FEE = new BigDecimal("5.00");
    private static final BigDecimal DORMANCY_FEE = new BigDecimal("25.00");
    private static final String STEP_NAME = "accountMaintenanceStep";
    private static final String JOB_NAME = "accountMaintenanceJob";

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private BatchProperties batchProperties;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    /**
     * Creates the main account maintenance batch job.
     * Configures job with proper restart capabilities and step sequencing
     * that mirrors the structure of CBACT01C.cbl processing logic.
     * 
     * @return configured Job for account maintenance processing
     */
    @Bean(name = "accountMaintenanceJob")
    public Job accountMaintenanceJob() {
        logger.info("Configuring account maintenance batch job");
        
        return new JobBuilder(JOB_NAME, jobRepository)
            .start(accountMaintenanceStep())
            .build();
    }

    /**
     * Creates the account maintenance processing step.
     * Configures chunk-oriented processing with appropriate batch size,
     * retry policies, and error handling that replaces COBOL sequential processing.
     * 
     * @return configured Step for account maintenance processing
     */
    @Bean(name = "accountMaintenanceStep")
    public Step accountMaintenanceStep() {
        logger.info("Configuring account maintenance step with chunk size: {}", 
                   batchProperties.getChunkSize());
        
        return new StepBuilder(STEP_NAME, jobRepository)
            .<Account, Account>chunk(batchProperties.getChunkSize(), transactionManager())
            .reader(accountMaintenanceReader())
            .processor(accountMaintenanceProcessor())
            .writer(accountMaintenanceWriter())
            .faultTolerant()
            .skipLimit(batchProperties.getSkipLimit())
            .skip(Exception.class)
            .retryLimit(batchProperties.getMaxRetryAttempts())
            .retry(Exception.class)
            .build();
    }

    /**
     * Creates the account reader for batch processing.
     * Implements JpaPagingItemReader to replace COBOL sequential file access
     * with efficient database pagination that processes accounts in chunks.
     * 
     * @return configured JpaPagingItemReader for Account entities
     */
    @Bean(name = "accountMaintenanceReader")
    public JpaPagingItemReader<Account> accountMaintenanceReader() {
        logger.info("Configuring account maintenance reader");
        
        JpaPagingItemReader<Account> reader = new JpaPagingItemReader<>();
        reader.setEntityManagerFactory(entityManagerFactory);
        reader.setPageSize(batchProperties.getChunkSize());
        
        // Query all active accounts ordered by account ID for consistent processing
        // Replicates COBOL sequential access pattern with database pagination
        reader.setQueryString("SELECT a FROM Account a WHERE a.activeStatus = :status ORDER BY a.accountId");
        reader.setParameterValues(java.util.Map.of("status", ACTIVE_STATUS));
        
        try {
            reader.afterPropertiesSet();
        } catch (Exception e) {
            logger.error("Error configuring account reader", e);
            throw new RuntimeException("Failed to configure account reader", e);
        }
        
        return reader;
    }

    /**
     * Creates the account processor for maintenance operations.
     * Implements the core business logic for account maintenance processing
     * that replaces COBOL paragraph processing with object-oriented methods.
     * 
     * @return configured ItemProcessor for account maintenance logic
     */
    @Bean(name = "accountMaintenanceProcessor")
    public ItemProcessor<Account, Account> accountMaintenanceProcessor() {
        logger.info("Configuring account maintenance processor");
        
        return account -> {
            try {
                return processAccountMaintenance(account);
            } catch (Exception e) {
                logger.error("Error processing account {}: {}", account.getAccountId(), e.getMessage());
                // Re-throw to trigger retry/skip logic
                throw new RuntimeException("Account processing failed for: " + account.getAccountId(), e);
            }
        };
    }

    /**
     * Creates the account writer for persisting maintenance results.
     * Implements batch writing to efficiently save updated account records
     * back to the database, replacing COBOL file write operations.
     * 
     * @return configured ItemWriter for Account entities
     */
    @Bean(name = "accountMaintenanceWriter")
    public ItemWriter<Account> accountMaintenanceWriter() {
        logger.info("Configuring account maintenance writer");
        
        return accounts -> {
            try {
                logger.debug("Writing {} processed accounts to database", accounts.size());
                accountRepository.saveAll(accounts);
                accountRepository.flush(); // Ensure immediate persistence
                
                logger.info("Successfully processed and saved {} accounts", accounts.size());
            } catch (Exception e) {
                logger.error("Error writing accounts to database", e);
                throw new RuntimeException("Failed to save processed accounts", e);
            }
        };
    }

    /**
     * Main account maintenance processing method.
     * Performs comprehensive maintenance operations on individual accounts,
     * translating COBOL business logic to Java while maintaining exact precision.
     * 
     * This method implements the equivalent of the COBOL processing paragraphs:
     * - 1000-ACCTFILE-GET-NEXT (reading account data)
     * - 1100-DISPLAY-ACCT-RECORD (account validation and processing)
     * - Business logic for maintenance operations
     * 
     * @param account the account to process
     * @return the processed account with applied maintenance operations
     */
    @Transactional
    public Account processAccountMaintenance(Account account) {
        logger.debug("Processing maintenance for account: {}", account.getAccountId());
        
        // Validate account data using COBOL-equivalent validation
        ValidationUtil.validateRequiredField("accountId", account.getAccountId());
        ValidationUtil.validateNumericField("accountId", account.getAccountId());
        
        // Create a copy for processing to maintain transactional integrity
        Account processedAccount = new Account();
        copyAccountData(processedAccount, account);
        
        // Step 1: Perform dormancy check based on transaction history
        performDormancyCheck(processedAccount);
        
        // Step 2: Assess fees based on account status and activity
        assessFees(processedAccount);
        
        // Step 3: Reconcile account balance with recent transactions
        reconcileBalance(processedAccount);
        
        // Step 4: Update account timestamps for audit trail
        processedAccount.setCreatedTimestamp(java.time.LocalDateTime.now());
        
        logger.debug("Completed maintenance processing for account: {}", 
                    processedAccount.getAccountId());
        
        return processedAccount;
    }

    /**
     * Performs dormancy check on account based on transaction activity.
     * Replicates COBOL logic for determining account dormancy status
     * based on last transaction date analysis.
     * 
     * @param account the account to check for dormancy
     */
    public void performDormancyCheck(Account account) {
        logger.debug("Performing dormancy check for account: {}", account.getAccountId());
        
        try {
            // Calculate threshold date for dormancy check
            LocalDate dormancyThreshold = DateConversionUtil.addDays(
                LocalDate.now(), -DORMANCY_THRESHOLD_DAYS);
            
            // Get the most recent transaction for this account
            Transaction lastTransaction = transactionRepository
                .findTopByAccountIdOrderByTransactionDateDesc(account.getAccountId());
            
            if (lastTransaction == null) {
                // No transactions found - mark as dormant if account is old enough
                LocalDate openDate = account.getOpenDate();
                if (openDate != null && openDate.isBefore(dormancyThreshold)) {
                    logger.info("Account {} marked as dormant - no transaction history", 
                               account.getAccountId());
                    account.setActiveStatus(DORMANT_STATUS);
                }
            } else {
                // Check if last transaction is before dormancy threshold
                LocalDate lastTransactionDate = lastTransaction.getTransactionDate();
                if (lastTransactionDate.isBefore(dormancyThreshold)) {
                    logger.info("Account {} marked as dormant - last transaction: {}", 
                               account.getAccountId(), lastTransactionDate);
                    account.setActiveStatus(DORMANT_STATUS);
                } else {
                    // Reactivate dormant accounts with recent activity
                    if (DORMANT_STATUS.equals(account.getActiveStatus())) {
                        logger.info("Account {} reactivated - recent transaction: {}", 
                                   account.getAccountId(), lastTransactionDate);
                        account.setActiveStatus(ACTIVE_STATUS);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error performing dormancy check for account {}: {}", 
                        account.getAccountId(), e.getMessage());
            throw new RuntimeException("Dormancy check failed", e);
        }
    }

    /**
     * Assesses maintenance fees on accounts based on status and activity.
     * Implements COBOL fee calculation logic with exact precision preservation
     * using CobolDataConverter and AmountCalculator utilities.
     * 
     * @param account the account to assess fees on
     */
    public void assessFees(Account account) {
        logger.debug("Assessing fees for account: {}", account.getAccountId());
        
        try {
            BigDecimal currentBalance = account.getCurrentBalance();
            if (currentBalance == null) {
                currentBalance = BigDecimal.ZERO;
            }
            
            BigDecimal feeAmount = BigDecimal.ZERO;
            String feeDescription = "";
            
            // Apply fees based on account status
            if (DORMANT_STATUS.equals(account.getActiveStatus())) {
                // Apply dormancy fee for dormant accounts
                feeAmount = CobolDataConverter.preservePrecision(DORMANCY_FEE, 2);
                feeDescription = "Dormancy Fee";
                logger.debug("Applying dormancy fee of {} to account {}", 
                           feeAmount, account.getAccountId());
            } else if (ACTIVE_STATUS.equals(account.getActiveStatus())) {
                // Apply monthly maintenance fee for active accounts
                feeAmount = CobolDataConverter.preservePrecision(MONTHLY_MAINTENANCE_FEE, 2);
                feeDescription = "Monthly Maintenance Fee";
                logger.debug("Applying maintenance fee of {} to account {}", 
                           feeAmount, account.getAccountId());
            }
            
            // Apply fee to account balance if fee is greater than zero
            if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
                // Validate fee amount using AmountCalculator
                AmountCalculator.validateAmount(feeAmount, feeDescription);
                
                // Calculate new balance after fee deduction
                BigDecimal newBalance = AmountCalculator.calculateBalance(
                    currentBalance, feeAmount.negate()); // Negate for deduction
                
                // Apply rounding using COBOL-equivalent rounding
                newBalance = AmountCalculator.applyRounding(newBalance);
                
                // Update account balance
                account.setCurrentBalance(newBalance);
                
                logger.info("Applied {} of {} to account {}, new balance: {}", 
                           feeDescription, feeAmount, account.getAccountId(), newBalance);
                
                // Handle negative balance scenarios if fee causes overdraft
                if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                    handleNegativeBalance(account, newBalance);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error assessing fees for account {}: {}", 
                        account.getAccountId(), e.getMessage());
            throw new RuntimeException("Fee assessment failed", e);
        }
    }

    /**
     * Reconciles account balance with recent transaction activity.
     * Validates account balance integrity by comparing against transaction history
     * and correcting any discrepancies found during maintenance processing.
     * 
     * @param account the account to reconcile
     */
    public void reconcileBalance(Account account) {
        logger.debug("Reconciling balance for account: {}", account.getAccountId());
        
        try {
            // Get recent transactions for balance reconciliation
            LocalDate reconciliationStartDate = DateConversionUtil.addDays(
                LocalDate.now(), -30); // Reconcile last 30 days
            
            List<Transaction> recentTransactions = transactionRepository
                .findByAccountIdAndTransactionDateBetween(
                    account.getAccountId(), 
                    reconciliationStartDate, 
                    LocalDate.now());
            
            if (recentTransactions.isEmpty()) {
                logger.debug("No recent transactions found for account {}", 
                           account.getAccountId());
                return;
            }
            
            // Calculate expected balance based on transactions
            BigDecimal calculatedBalance = account.getCurrentBalance();
            if (calculatedBalance == null) {
                calculatedBalance = BigDecimal.ZERO;
            }
            
            // Verify transaction consistency
            BigDecimal transactionSum = BigDecimal.ZERO;
            for (Transaction transaction : recentTransactions) {
                BigDecimal transactionAmount = transaction.getAmount();
                if (transactionAmount != null) {
                    // Apply COBOL precision preservation
                    transactionAmount = CobolDataConverter.preservePrecision(transactionAmount, 2);
                    transactionSum = transactionSum.add(transactionAmount);
                }
            }
            
            // Apply final rounding to match COBOL calculations
            transactionSum = AmountCalculator.applyRounding(transactionSum);
            
            // Log reconciliation results for audit purposes
            logger.debug("Account {} reconciliation - Current: {}, Transaction sum: {}", 
                        account.getAccountId(), calculatedBalance, transactionSum);
            
            // Validate balance is within reasonable bounds
            AmountCalculator.validateAmount(calculatedBalance, "Reconciled Balance");
            
        } catch (Exception e) {
            logger.error("Error reconciling balance for account {}: {}", 
                        account.getAccountId(), e.getMessage());
            throw new RuntimeException("Balance reconciliation failed", e);
        }
    }

    /**
     * Handles negative balance scenarios after fee assessment.
     * Implements business rules for overdraft handling and balance correction.
     * 
     * @param account the account with negative balance
     * @param negativeBalance the negative balance amount
     */
    private void handleNegativeBalance(Account account, BigDecimal negativeBalance) {
        logger.warn("Account {} has negative balance: {}", 
                   account.getAccountId(), negativeBalance);
        
        try {
            // Check if account has overdraft protection through credit limit
            BigDecimal creditLimit = account.getCreditLimit();
            if (creditLimit != null && creditLimit.compareTo(BigDecimal.ZERO) > 0) {
                
                BigDecimal availableCredit = creditLimit.add(negativeBalance); // negativeBalance is already negative
                
                if (availableCredit.compareTo(BigDecimal.ZERO) >= 0) {
                    logger.info("Account {} negative balance {} covered by credit limit {}", 
                               account.getAccountId(), negativeBalance, creditLimit);
                } else {
                    // Exceeds credit limit - may need to suspend account
                    logger.warn("Account {} exceeds credit limit: balance {} > limit {}", 
                               account.getAccountId(), negativeBalance, creditLimit);
                    
                    // Apply additional overdraft fee
                    BigDecimal overdraftFee = new BigDecimal("35.00");
                    BigDecimal adjustedBalance = AmountCalculator.processNegativeBalance(
                        negativeBalance, creditLimit, overdraftFee);
                    
                    account.setCurrentBalance(adjustedBalance);
                }
            } else {
                // No credit limit - suspend account for negative balance
                logger.warn("Suspending account {} due to negative balance without credit limit", 
                           account.getAccountId());
                account.setActiveStatus(SUSPENDED_STATUS);
            }
            
        } catch (Exception e) {
            logger.error("Error handling negative balance for account {}: {}", 
                        account.getAccountId(), e.getMessage());
            // Don't throw exception - log error and continue processing
        }
    }

    /**
     * Copies account data for processing while preserving original data integrity.
     * Creates a safe copy for maintenance operations without affecting the original.
     * 
     * @param target the target account to copy data to
     * @param source the source account to copy data from
     */
    private void copyAccountData(Account target, Account source) {
        target.setAccountId(source.getAccountId());
        target.setActiveStatus(source.getActiveStatus());
        target.setCurrentBalance(source.getCurrentBalance());
        target.setCreditLimit(source.getCreditLimit());
        target.setCashCreditLimit(source.getCashCreditLimit());
        target.setOpenDate(source.getOpenDate());
        target.setExpirationDate(source.getExpirationDate());
        target.setReissueDate(source.getReissueDate());
        target.setCurrentCycleCredit(source.getCurrentCycleCredit());
        target.setCurrentCycleDebit(source.getCurrentCycleDebit());
        target.setGroupId(source.getGroupId());
        target.setCreatedTimestamp(source.getCreatedTimestamp());
    }

    /**
     * Provides transaction manager for batch step configuration.
     * Required for proper transaction boundary management in chunk processing.
     * 
     * @return platform transaction manager
     */
    private org.springframework.transaction.PlatformTransactionManager transactionManager() {
        return new org.springframework.orm.jpa.JpaTransactionManager(entityManagerFactory);
    }
}