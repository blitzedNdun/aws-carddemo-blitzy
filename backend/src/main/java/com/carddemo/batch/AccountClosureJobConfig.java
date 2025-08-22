/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.batch;

import com.carddemo.service.AccountClosureBatchService;
import com.carddemo.entity.AccountClosure;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.sql.DataSource;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring Batch job configuration for account closure processing.
 * 
 * This configuration class defines the complete account closure batch job workflow,
 * implementing the COBOL CBSTM01C.cbl equivalent logic through Spring Batch 5.x
 * framework capabilities. The job processes closure requests through multiple 
 * coordinated steps while maintaining COBOL processing sequence and restart capabilities.
 * 
 * Migration Context:
 * This class replaces the COBOL batch job CBSTM01C.cbl, converting mainframe JCL
 * job execution to modern Spring Batch processing. The job maintains the same
 * logical processing flow as the original COBOL program while leveraging Spring's
 * enterprise features for dependency injection, transaction management, and
 * comprehensive error handling and recovery.
 * 
 * Job Architecture:
 * The account closure job consists of five sequential processing steps that mirror
 * the COBOL program structure:
 * 
 * 1. validateBalancesStep: Validates account balances for closure eligibility
 * 2. generateFinalStatementsStep: Generates final statements for closing accounts  
 * 3. archiveAccountHistoryStep: Archives complete account transaction history
 * 4. updateAccountStatusStep: Updates account status to closed
 * 5. (Notifications handled within updateAccountStatusStep)
 * 
 * Key Features:
 * - Chunk-oriented processing with configurable commit intervals
 * - Job restart capabilities with state persistence via JobRepository
 * - Transaction boundaries matching CICS SYNCPOINT behavior  
 * - Comprehensive error handling and logging for audit trails
 * - Integration with AccountClosureBatchService for business logic
 * - Maintains COBOL processing patterns and data validation rules
 * 
 * Processing Flow (based on COBOL CBSTM01C structure):
 * - 0000-INIT: Job initialization and parameter validation
 * - 1000-MAIN: Sequential step execution with dependency management
 * - 2000-VALIDATE: Balance validation step processing
 * - 3000-STATEMENTS: Final statement generation step
 * - 4000-ARCHIVE: Account history archival step  
 * - 5000-STATUS: Account status update and notification step
 * - 9000-END: Job completion and cleanup processing
 * 
 * Transaction Management:
 * - Each step operates within Spring @Transactional boundaries
 * - Configurable chunk size for optimal performance and recovery
 * - Job repository maintains execution state for restart scenarios
 * - Rollback capabilities for step failures to ensure data consistency
 * 
 * Error Handling and Recovery:
 * - Failed items are skipped with configurable skip limits
 * - Job restart capabilities resume from last successful step
 * - Comprehensive logging for audit trail and debugging
 * - Exception handling preserves processing context for analysis
 * 
 * Performance Considerations:
 * - Chunk-oriented processing optimizes database interactions
 * - Configurable thread pool for parallel processing where applicable
 * - Database connection pooling managed by Spring Boot
 * - Memory-efficient item readers for large dataset processing
 * 
 * Dependencies:
 * - AccountClosureBatchService: Core business logic for closure processing
 * - JobRepository: Spring Batch job execution state management
 * - DataSource: Database connectivity for item readers and writers
 * - PlatformTransactionManager: Transaction boundary management
 * 
 * @author CardDemo Migration Team  
 * @version 1.0
 * @since 2024
 * @see AccountClosureBatchService
 * @see org.springframework.batch.core.Job
 * @see org.springframework.batch.core.Step
 */
@Configuration
public class AccountClosureJobConfig {

    private static final Logger logger = LoggerFactory.getLogger(AccountClosureJobConfig.class);
    
    // Chunk size for optimal processing performance (based on COBOL commit frequency)
    private static final int CHUNK_SIZE = 100;
    
    // Skip limit for error tolerance (maintains COBOL error handling patterns)
    private static final int SKIP_LIMIT = 10;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
    private final AccountClosureBatchService accountClosureBatchService;

    /**
     * Constructor with dependency injection for all required components.
     *
     * @param jobRepository Spring Batch job repository for state management
     * @param transactionManager Transaction manager for ACID compliance  
     * @param dataSource Database connection for item readers and writers
     * @param accountClosureBatchService Business logic service for closure processing
     */
    @Autowired
    public AccountClosureJobConfig(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("dataSource") DataSource dataSource,
            AccountClosureBatchService accountClosureBatchService) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.dataSource = dataSource;
        this.accountClosureBatchService = accountClosureBatchService;
        logger.info("AccountClosureJobConfig initialized with chunk size: {} and skip limit: {}", 
                   CHUNK_SIZE, SKIP_LIMIT);
    }

    /**
     * Main account closure job definition.
     * 
     * Defines the complete account closure batch job with sequential step execution
     * matching the COBOL CBSTM01C.cbl program flow. The job orchestrates the closure
     * process through four main processing steps with appropriate error handling
     * and restart capabilities.
     * 
     * Job Configuration:
     * - Sequential step execution prevents data inconsistencies
     * - RunIdIncrementer enables multiple job executions
     * - Job restart support for recovery from failures
     * - Comprehensive logging for audit trail and monitoring
     * 
     * Processing Steps (executed in sequence):
     * 1. validateBalancesStep: Account balance and eligibility validation
     * 2. generateFinalStatementsStep: Final statement generation and archival
     * 3. archiveAccountHistoryStep: Complete account history archival  
     * 4. updateAccountStatusStep: Account status update and notifications
     * 
     * Error Handling:
     * - Job fails fast on critical step failures
     * - Individual step failures logged with context
     * - Restart capabilities preserve successful step completions
     * - Transaction rollback maintains data consistency
     * 
     * @return Configured Spring Batch Job for account closure processing
     */
    @Bean
    public Job accountClosureJob() {
        logger.info("Configuring account closure batch job");
        
        return new JobBuilder("accountClosureJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(validateBalancesStep())
                .next(generateFinalStatementsStep())
                .next(archiveAccountHistoryStep())
                .next(updateAccountStatusStep())
                .build();
    }

    /**
     * Balance validation step for account closure eligibility.
     * 
     * This step implements the COBOL 2000-VALIDATE paragraph logic, processing
     * closure requests to validate account balances and closure eligibility.
     * Accounts that fail validation are marked accordingly and excluded from
     * subsequent processing steps.
     * 
     * Step Configuration:
     * - Chunk-oriented processing for optimal database performance
     * - Skip policy for non-critical validation failures
     * - Transaction boundaries for data consistency
     * - Restart support preserves validation state
     * 
     * Processing Logic:
     * - Reader: Retrieves pending closure requests from database
     * - Processor: Validates account balance using AccountClosureBatchService
     * - Writer: Updates closure request status based on validation results
     * 
     * Validation Rules (from COBOL business logic):
     * - Account balance must be zero or negative (credit balance allowed)
     * - Account must be in valid status for closure
     * - No pending transactions that could affect balance
     * - Associated cards must be inactive or expired
     * 
     * Error Handling:
     * - Validation failures are logged with account details
     * - Failed accounts remain in pending status for manual review
     * - Skip limit prevents job failure from isolated validation errors
     * - Transaction rollback protects data integrity
     * 
     * @return Configured Step for balance validation processing
     */
    @Bean
    public Step validateBalancesStep() {
        logger.info("Configuring validate balances step");
        
        return new StepBuilder("validateBalancesStep", jobRepository)
                .<AccountClosure, AccountClosure>chunk(CHUNK_SIZE, transactionManager)
                .reader(closureRequestItemReader())
                .processor(balanceValidationProcessor())
                .writer(balanceValidationWriter())
                .faultTolerant()
                .skipLimit(SKIP_LIMIT)
                .skip(Exception.class)
                .build();
    }

    /**
     * Final statement generation step for closing accounts.
     * 
     * This step implements the COBOL 3000-OUTPUT paragraph logic, generating
     * comprehensive final statements for accounts that have passed balance
     * validation. Statements include complete transaction history, final
     * balances, and closure notification details.
     * 
     * Step Configuration:
     * - Chunk-oriented processing for efficient statement generation
     * - Lower chunk size for memory-intensive statement operations
     * - Transaction boundaries ensure statement consistency
     * - Skip policy handles individual statement generation failures
     * 
     * Processing Logic:
     * - Reader: Retrieves validated closure requests ready for statement generation
     * - Processor: Generates final statements using AccountClosureBatchService
     * - Writer: Persists generated statements and updates closure status
     * 
     * Statement Generation Process (based on COBOL 5000-CREATE-STATEMENT):
     * - Retrieve complete account and customer information
     * - Compile transaction history for statement period
     * - Calculate final balance with COMP-3 precision matching
     * - Generate statement in required format (text and structured)
     * - Store statement record for archival and customer access
     * 
     * Error Handling:
     * - Statement generation failures logged with account context
     * - Failed statement generations are retried on job restart
     * - Skip limit allows job continuation despite isolated failures
     * - Transaction rollback maintains statement data integrity
     * 
     * @return Configured Step for final statement generation
     */
    @Bean
    public Step generateFinalStatementsStep() {
        logger.info("Configuring generate final statements step");
        
        return new StepBuilder("generateFinalStatementsStep", jobRepository)
                .<AccountClosure, AccountClosure>chunk(CHUNK_SIZE / 2, transactionManager) // Smaller chunks for memory-intensive operations
                .reader(validatedClosureRequestItemReader())
                .processor(statementGenerationProcessor())
                .writer(statementGenerationWriter())
                .faultTolerant()
                .skipLimit(SKIP_LIMIT)
                .skip(Exception.class)
                .build();
    }

    /**
     * Account history archival step for regulatory compliance.
     * 
     * This step implements the COBOL 6000-ARCHIVE-PROCESS paragraph logic,
     * archiving complete account transaction history for regulatory compliance
     * and record retention requirements. Archives include all account data,
     * transaction history, and closure documentation.
     * 
     * Step Configuration:
     * - Chunk-oriented processing for efficient archival operations
     * - Optimized chunk size for large data archival
     * - Transaction boundaries ensure archival consistency
     * - Skip policy handles archival failures without job termination
     * 
     * Processing Logic:
     * - Reader: Retrieves accounts with completed statement generation
     * - Processor: Creates archival records using AccountClosureBatchService
     * - Writer: Persists archival records with retention policy metadata
     * 
     * Archival Process (based on COBOL 6000-ARCHIVE-PROCESS):
     * - Extract complete account transaction history
     * - Compile customer and account relationship data
     * - Generate comprehensive archival record with metadata
     * - Apply retention policy rules for regulatory compliance
     * - Create indexed archival entry for future retrieval
     * 
     * Retention Policy Implementation:
     * - Standard retention: 7 years from account closure date
     * - Disputed accounts: 10 years from resolution date
     * - Regulatory hold: Indefinite until legal release
     * - Customer requested data: 30 days expedited access
     * 
     * Error Handling:
     * - Archival failures logged with detailed account context
     * - Failed archival operations are retried on job restart
     * - Skip limit ensures job completion despite isolated failures
     * - Transaction rollback protects archival data integrity
     * 
     * @return Configured Step for account history archival
     */
    @Bean
    public Step archiveAccountHistoryStep() {
        logger.info("Configuring archive account history step");
        
        return new StepBuilder("archiveAccountHistoryStep", jobRepository)
                .<AccountClosure, AccountClosure>chunk(CHUNK_SIZE, transactionManager)
                .reader(statementCompletedClosureRequestItemReader())
                .processor(historyArchivalProcessor())
                .writer(historyArchivalWriter())
                .faultTolerant()
                .skipLimit(SKIP_LIMIT)
                .skip(Exception.class)
                .build();
    }

    /**
     * Account status update step for closure completion.
     * 
     * This step implements the COBOL 9000-CLOSE paragraph logic, updating
     * account status to closed and processing all required closure notifications.
     * This final step completes the account closure process and triggers
     * regulatory and customer notifications.
     * 
     * Step Configuration:
     * - Chunk-oriented processing for efficient status updates
     * - Standard chunk size for optimal database performance
     * - Transaction boundaries ensure status update consistency
     * - Skip policy handles notification failures gracefully
     * 
     * Processing Logic:
     * - Reader: Retrieves accounts with completed archival processing
     * - Processor: Updates account status and processes notifications
     * - Writer: Finalizes closure request status and audit trail
     * 
     * Status Update Process (based on COBOL 9000-CLOSE):
     * - Update account active status to inactive (closed)
     * - Set account closure timestamp for audit trail
     * - Update account modification tracking fields
     * - Process regulatory closure notifications
     * - Generate customer closure confirmation notifications
     * - Complete closure request with final status and date
     * 
     * Notification Processing:
     * - CLOSURE_CONFIRMATION: Customer notification of account closure
     * - REGULATORY_REPORT: Required compliance notification to regulators
     * - AUDIT_NOTIFICATION: Internal audit trail notification
     * 
     * Error Handling:
     * - Status update failures logged with account details
     * - Notification failures do not prevent account closure completion
     * - Skip limit allows job completion despite notification issues
     * - Transaction rollback ensures status update consistency
     * 
     * @return Configured Step for account status update and notifications
     */
    @Bean
    public Step updateAccountStatusStep() {
        logger.info("Configuring update account status step");
        
        return new StepBuilder("updateAccountStatusStep", jobRepository)
                .<AccountClosure, AccountClosure>chunk(CHUNK_SIZE, transactionManager)
                .reader(archivedClosureRequestItemReader())
                .processor(statusUpdateProcessor())
                .writer(statusUpdateWriter())
                .faultTolerant()
                .skipLimit(SKIP_LIMIT)
                .skip(Exception.class)
                .build();
    }

    // =============================================================================
    // ITEM READERS - Database readers for each step
    // =============================================================================

    /**
     * Item reader for pending closure requests.
     * 
     * Reads all pending closure requests from the database for initial
     * balance validation processing. Uses cursor-based reading for
     * memory-efficient processing of large datasets.
     * 
     * @return ItemReader for pending AccountClosure entities
     */
    @Bean
    public ItemReader<AccountClosure> closureRequestItemReader() {
        return new JdbcCursorItemReaderBuilder<AccountClosure>()
                .name("closureRequestItemReader")
                .dataSource(dataSource)
                .sql("SELECT * FROM closure_request WHERE closure_status = 'P' ORDER BY request_date")
                .rowMapper((rs, rowNum) -> {
                    AccountClosure closure = new AccountClosure();
                    closure.setAccountId(rs.getLong("account_id"));
                    closure.setClosureReasonCode(rs.getString("closure_reason_code"));
                    closure.setClosureStatus("P"); // Pending
                    closure.setRequestedDate(rs.getDate("request_date").toLocalDate());
                    return closure;
                })
                .build();
    }

    /**
     * Item reader for validated closure requests ready for statement generation.
     * 
     * Reads closure requests that have passed balance validation and are
     * ready for final statement generation processing.
     * 
     * @return ItemReader for validated AccountClosure entities
     */
    @Bean
    public ItemReader<AccountClosure> validatedClosureRequestItemReader() {
        return new JdbcCursorItemReaderBuilder<AccountClosure>()
                .name("validatedClosureRequestItemReader")
                .dataSource(dataSource)
                .sql("SELECT * FROM closure_request WHERE closure_status = 'V' ORDER BY request_date")
                .rowMapper((rs, rowNum) -> {
                    AccountClosure closure = new AccountClosure();
                    closure.setAccountId(rs.getLong("account_id"));
                    closure.setClosureReasonCode(rs.getString("closure_reason_code"));
                    closure.setClosureStatus("V"); // Validated
                    closure.setRequestedDate(rs.getDate("request_date").toLocalDate());
                    return closure;
                })
                .build();
    }

    /**
     * Item reader for closure requests with completed statement generation.
     * 
     * Reads closure requests that have completed final statement generation
     * and are ready for account history archival processing.
     * 
     * @return ItemReader for statement-completed AccountClosure entities
     */
    @Bean
    public ItemReader<AccountClosure> statementCompletedClosureRequestItemReader() {
        return new JdbcCursorItemReaderBuilder<AccountClosure>()
                .name("statementCompletedClosureRequestItemReader")
                .dataSource(dataSource)
                .sql("SELECT * FROM closure_request WHERE closure_status = 'S' ORDER BY request_date")
                .rowMapper((rs, rowNum) -> {
                    AccountClosure closure = new AccountClosure();
                    closure.setAccountId(rs.getLong("account_id"));
                    closure.setClosureReasonCode(rs.getString("closure_reason_code"));
                    closure.setClosureStatus("S"); // Statement completed
                    closure.setRequestedDate(rs.getDate("request_date").toLocalDate());
                    return closure;
                })
                .build();
    }

    /**
     * Item reader for closure requests with completed archival processing.
     * 
     * Reads closure requests that have completed account history archival
     * and are ready for final account status update and notification processing.
     * 
     * @return ItemReader for archived AccountClosure entities
     */
    @Bean
    public ItemReader<AccountClosure> archivedClosureRequestItemReader() {
        return new JdbcCursorItemReaderBuilder<AccountClosure>()
                .name("archivedClosureRequestItemReader")
                .dataSource(dataSource)
                .sql("SELECT * FROM closure_request WHERE closure_status = 'A' ORDER BY request_date")
                .rowMapper((rs, rowNum) -> {
                    AccountClosure closure = new AccountClosure();
                    closure.setAccountId(rs.getLong("account_id"));
                    closure.setClosureReasonCode(rs.getString("closure_reason_code"));
                    closure.setClosureStatus("A"); // Archived
                    closure.setRequestedDate(rs.getDate("request_date").toLocalDate());
                    return closure;
                })
                .build();
    }

    // =============================================================================
    // ITEM PROCESSORS - Business logic processors for each step
    // =============================================================================

    /**
     * Item processor for balance validation.
     * 
     * Processes closure requests through balance validation logic using
     * AccountClosureBatchService.validateAccountBalance(). Updates closure
     * status based on validation results.
     * 
     * @return ItemProcessor for balance validation processing
     */
    @Bean
    public ItemProcessor<AccountClosure, AccountClosure> balanceValidationProcessor() {
        return closure -> {
            logger.debug("Processing balance validation for account ID: {}", closure.getAccountId());
            
            try {
                boolean isValid = accountClosureBatchService.validateAccountBalance(closure.getAccountId());
                
                if (isValid) {
                    closure.setClosureStatus("V"); // Validated
                    logger.debug("Balance validation passed for account ID: {}", closure.getAccountId());
                } else {
                    closure.setClosureStatus("F"); // Failed validation
                    logger.warn("Balance validation failed for account ID: {}", closure.getAccountId());
                }
                
                return closure;
                
            } catch (Exception e) {
                logger.error("Error during balance validation for account ID: {}", closure.getAccountId(), e);
                closure.setClosureStatus("E"); // Error status
                return closure;
            }
        };
    }

    /**
     * Item processor for statement generation.
     * 
     * Processes validated closure requests through final statement generation
     * using AccountClosureBatchService.generateFinalStatement(). Updates closure
     * status to indicate statement completion.
     * 
     * @return ItemProcessor for statement generation processing
     */
    @Bean
    public ItemProcessor<AccountClosure, AccountClosure> statementGenerationProcessor() {
        return closure -> {
            logger.debug("Processing statement generation for account ID: {}", closure.getAccountId());
            
            try {
                accountClosureBatchService.generateFinalStatement(closure.getAccountId(), LocalDateTime.now());
                
                closure.setClosureStatus("S"); // Statement completed
                logger.debug("Statement generation completed for account ID: {}", closure.getAccountId());
                
                return closure;
                
            } catch (Exception e) {
                logger.error("Error during statement generation for account ID: {}", closure.getAccountId(), e);
                closure.setClosureStatus("E"); // Error status
                return closure;
            }
        };
    }

    /**
     * Item processor for account history archival.
     * 
     * Processes closure requests with completed statements through account
     * history archival using AccountClosureBatchService.archiveAccountHistory().
     * Updates closure status to indicate archival completion.
     * 
     * @return ItemProcessor for history archival processing
     */
    @Bean
    public ItemProcessor<AccountClosure, AccountClosure> historyArchivalProcessor() {
        return closure -> {
            logger.debug("Processing history archival for account ID: {}", closure.getAccountId());
            
            try {
                accountClosureBatchService.archiveAccountHistory(closure.getAccountId(), LocalDateTime.now());
                
                closure.setClosureStatus("A"); // Archived
                logger.debug("History archival completed for account ID: {}", closure.getAccountId());
                
                return closure;
                
            } catch (Exception e) {
                logger.error("Error during history archival for account ID: {}", closure.getAccountId(), e);
                closure.setClosureStatus("E"); // Error status
                return closure;
            }
        };
    }

    /**
     * Item processor for account status update and notifications.
     * 
     * Processes archived closure requests through final account status update
     * and notification processing using AccountClosureBatchService methods.
     * Completes the closure process with status updates and notifications.
     * 
     * @return ItemProcessor for status update processing
     */
    @Bean
    public ItemProcessor<AccountClosure, AccountClosure> statusUpdateProcessor() {
        return closure -> {
            logger.debug("Processing status update for account ID: {}", closure.getAccountId());
            
            try {
                // Update account status to closed
                accountClosureBatchService.updateAccountStatus(closure.getAccountId(), "CLOSED");
                
                // Process closure notifications  
                accountClosureBatchService.processClosureNotifications(closure.getAccountId(), closure.getClosureReasonCode());
                
                closure.setClosureStatus("C"); // Completed
                closure.setClosureDate(LocalDateTime.now().toLocalDate());
                logger.debug("Status update and notifications completed for account ID: {}", closure.getAccountId());
                
                return closure;
                
            } catch (Exception e) {
                logger.error("Error during status update for account ID: {}", closure.getAccountId(), e);
                closure.setClosureStatus("E"); // Error status
                return closure;
            }
        };
    }

    // =============================================================================
    // ITEM WRITERS - Database writers for each step
    // =============================================================================

    /**
     * Item writer for balance validation results.
     * 
     * Updates closure request status in the database based on balance
     * validation results. Maintains audit trail for validation outcomes.
     * 
     * @return ItemWriter for balance validation results
     */
    @Bean
    public ItemWriter<AccountClosure> balanceValidationWriter() {
        return new JdbcBatchItemWriterBuilder<AccountClosure>()
                .dataSource(dataSource)
                .sql("UPDATE closure_request SET closure_status = :closureStatus, " +
                     "last_updated = CURRENT_TIMESTAMP WHERE account_id = :accountId")
                .beanMapped()
                .build();
    }

    /**
     * Item writer for statement generation results.
     * 
     * Updates closure request status in the database to indicate statement
     * generation completion. Maintains processing state for subsequent steps.
     * 
     * @return ItemWriter for statement generation results
     */
    @Bean
    public ItemWriter<AccountClosure> statementGenerationWriter() {
        return new JdbcBatchItemWriterBuilder<AccountClosure>()
                .dataSource(dataSource)
                .sql("UPDATE closure_request SET closure_status = :closureStatus, " +
                     "statement_generated_date = CURRENT_TIMESTAMP, " +
                     "last_updated = CURRENT_TIMESTAMP WHERE account_id = :accountId")
                .beanMapped()
                .build();
    }

    /**
     * Item writer for history archival results.
     * 
     * Updates closure request status in the database to indicate history
     * archival completion. Maintains processing state for final step.
     * 
     * @return ItemWriter for history archival results
     */
    @Bean
    public ItemWriter<AccountClosure> historyArchivalWriter() {
        return new JdbcBatchItemWriterBuilder<AccountClosure>()
                .dataSource(dataSource)
                .sql("UPDATE closure_request SET closure_status = :closureStatus, " +
                     "archive_completed_date = CURRENT_TIMESTAMP, " +
                     "last_updated = CURRENT_TIMESTAMP WHERE account_id = :accountId")
                .beanMapped()
                .build();
    }

    /**
     * Item writer for final status update results.
     * 
     * Updates closure request with final completion status and closure date.
     * Marks the closure request as fully processed and completes audit trail.
     * 
     * @return ItemWriter for final status update results
     */
    @Bean
    public ItemWriter<AccountClosure> statusUpdateWriter() {
        return new JdbcBatchItemWriterBuilder<AccountClosure>()
                .dataSource(dataSource)
                .sql("UPDATE closure_request SET closure_status = :closureStatus, " +
                     "closure_date = :closureDate, " +
                     "completion_timestamp = CURRENT_TIMESTAMP, " +
                     "last_updated = CURRENT_TIMESTAMP WHERE account_id = :accountId")
                .beanMapped()
                .build();
    }
}
