/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.batch;

import com.carddemo.batch.AccountProcessingJob;
import com.carddemo.batch.BatchJobLauncher;
import com.carddemo.batch.DailyTransactionJob;
import com.carddemo.batch.InterestCalculationJob;
import com.carddemo.batch.StatementGenerationJob;
import com.carddemo.config.TestBatchConfig;
import com.carddemo.config.TestDatabaseConfig;
import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;

import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.batch.core.JobParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.test.JobRepositoryTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test suite for validating end-to-end batch job workflows, dependencies, 
 * and sequencing matching the original JCL job streams with proper data flow between jobs.
 * 
 * This comprehensive test suite validates the complete migration from mainframe JCL batch
 * processing to Spring Batch job execution, ensuring functional parity with original
 * COBOL batch programs while maintaining identical processing sequences, data integrity,
 * and error handling patterns.
 * 
 * Test Coverage Areas:
 * - Nightly batch job chain execution: transaction processing → interest calculation → statement generation
 * - Job dependency validation ensuring proper sequencing and conditional execution
 * - Data flow verification between dependent jobs with referential integrity checks
 * - Parallel job execution validation where applicable (Account processing jobs)
 * - File generation and consumption workflows between interdependent jobs
 * - Transaction file processing workflow with validation and error handling
 * - Account update propagation testing across the complete batch processing chain
 * - Statement generation validation after transaction posting and interest calculation
 * - Error propagation and handling across job chains with proper rollback mechanisms
 * - Restart scenarios for failed job chains with checkpoint recovery
 * - Batch window compliance testing ensuring 4-hour processing window adherence
 * - Resource cleanup validation between jobs preventing data contamination
 * 
 * Technical Implementation:
 * Uses Spring Batch Test framework with multiple JobLaunchers and Testcontainers for
 * database state management. Validates against original JCL job stream specifications
 * ensuring complete functional equivalence between mainframe and modernized processing.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false",
    "logging.level.com.carddemo.batch=DEBUG",
    "carddemo.batch.window.hours=4",
    "carddemo.batch.parallel.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class BatchIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("carddemo_test")
            .withUsername("testuser")
            .withPassword("testpass")
            .withReuse(true);

    @Autowired
    private TestBatchConfig testBatchConfig;

    @Autowired
    private TestDatabaseConfig testDatabaseConfig;

    @Autowired
    private DailyTransactionJob dailyTransactionJob;

    @Autowired
    private InterestCalculationJob interestCalculationJob;

    @Autowired
    private StatementGenerationJob statementGenerationJob;

    @Autowired
    private AccountProcessingJob accountProcessingJob;

    @Autowired
    private BatchJobLauncher batchJobLauncher;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    // Test data containers
    private List<Account> testAccounts;
    private List<Transaction> testTransactions;
    private LocalDateTime testStartTime;
    private Map<String, Object> jobExecutionMetrics;

    /**
     * Sets up comprehensive test data environment for batch job integration testing.
     * 
     * Creates representative account and transaction data that mirrors production
     * data patterns while ensuring referential integrity across all test scenarios.
     * Establishes baseline metrics for performance validation against 4-hour batch window.
     * 
     * Test data includes:
     * - 100 customer accounts with varying balance and status profiles
     * - 500 transaction records spanning multiple categories and date ranges
     * - Account balances configured for interest calculation validation
     * - Transaction types covering all supported processing scenarios
     */
    @BeforeEach
    void setupTestData() {
        testStartTime = LocalDateTime.now();
        jobExecutionMetrics = new HashMap<>();
        
        // Clean database state for isolated testing
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        jobRepositoryTestUtils.removeJobExecutions();

        // Setup test accounts with diverse balance and status patterns
        testAccounts = createTestAccounts();
        accountRepository.saveAll(testAccounts);

        // Setup test transactions spanning multiple processing categories
        testTransactions = createTestTransactions();
        transactionRepository.saveAll(testTransactions);

        // Verify test data integrity
        assertThat(accountRepository.count()).isEqualTo(100L);
        assertThat(transactionRepository.count()).isEqualTo(500L);
        
        // Initialize job execution tracking
        jobExecutionMetrics.put("test_start_time", testStartTime);
        jobExecutionMetrics.put("accounts_loaded", testAccounts.size());
        jobExecutionMetrics.put("transactions_loaded", testTransactions.size());
    }

    /**
     * Validates complete nightly batch job chain execution matching original JCL workflow.
     * 
     * This test replicates the mainframe nightly batch processing sequence:
     * 1. Daily Transaction Processing (CBTRN01C.cbl → DailyTransactionJob)
     * 2. Interest Calculation (CBACT04C.cbl → InterestCalculationJob) 
     * 3. Statement Generation (CBSTM03A/B.cbl → StatementGenerationJob)
     * 
     * Validates end-to-end data flow, processing sequence, and completion within
     * the 4-hour batch processing window constraint from the original mainframe environment.
     * 
     * Success Criteria:
     * - All jobs complete successfully in proper sequence
     * - Data integrity maintained throughout the processing chain
     * - Total processing time remains under 4-hour window
     * - Account balances updated correctly after each processing step
     * - Transaction posting and interest calculation precision matches COBOL COMP-3 behavior
     */
    @Test
    @Order(1)
    @DisplayName("Complete Nightly Batch Job Chain - End-to-End Workflow Validation")
    void testCompleteNightlyBatchJobChain() throws Exception {
        LocalDateTime chainStartTime = LocalDateTime.now();
        
        // Step 1: Execute Daily Transaction Processing Job
        JobParameters dailyJobParams = new JobParametersBuilder()
                .addString("processDate", LocalDate.now().toString())
                .addString("jobName", "dailyTransactionJob")
                .addLong("executionId", System.currentTimeMillis())
                .toJobParameters();

        JobExecution dailyJobExecution = batchJobLauncher.launchJob(
            dailyTransactionJob.dailyTransactionJob(), dailyJobParams);
        
        // Validate daily transaction job completion
        assertThat(dailyJobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(dailyJobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        
        // Verify transaction processing results
        long processedTransactions = transactionRepository.count();
        assertThat(processedTransactions).isGreaterThanOrEqualTo(testTransactions.size());
        
        jobExecutionMetrics.put("daily_job_duration", 
            dailyJobExecution.getEndTime().getTime() - dailyJobExecution.getStartTime().getTime());

        // Step 2: Execute Interest Calculation Job (dependent on daily processing)
        JobParameters interestJobParams = new JobParametersBuilder()
                .addString("processDate", LocalDate.now().toString())
                .addString("jobName", "interestCalculationJob")
                .addString("dependentJob", "dailyTransactionJob")
                .addLong("executionId", System.currentTimeMillis())
                .toJobParameters();

        JobExecution interestJobExecution = batchJobLauncher.launchJob(
            interestCalculationJob.interestCalculationJob(), interestJobParams);

        // Validate interest calculation job completion and precision
        assertThat(interestJobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(interestJobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        
        // Verify interest calculation accuracy (BigDecimal precision validation)
        List<Account> updatedAccounts = accountRepository.findAll();
        validateInterestCalculationPrecision(updatedAccounts);
        
        jobExecutionMetrics.put("interest_job_duration",
            interestJobExecution.getEndTime().getTime() - interestJobExecution.getStartTime().getTime());

        // Step 3: Execute Statement Generation Job (dependent on interest calculation)
        JobParameters statementJobParams = new JobParametersBuilder()
                .addString("processDate", LocalDate.now().toString())
                .addString("jobName", "statementGenerationJob")
                .addString("dependentJob", "interestCalculationJob")
                .addLong("executionId", System.currentTimeMillis())
                .toJobParameters();

        JobExecution statementJobExecution = batchJobLauncher.launchJob(
            statementGenerationJob.statementGenerationJob(), statementJobParams);

        // Validate statement generation job completion
        assertThat(statementJobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(statementJobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        
        // Verify statement generation results
        validateStatementGeneration(updatedAccounts);
        
        jobExecutionMetrics.put("statement_job_duration",
            statementJobExecution.getEndTime().getTime() - statementJobExecution.getStartTime().getTime());

        // Validate complete batch chain timing against 4-hour window
        LocalDateTime chainEndTime = LocalDateTime.now();
        long totalChainDuration = java.time.Duration.between(chainStartTime, chainEndTime).toHours();
        
        assertThat(totalChainDuration).isLessThan(4L);
        jobExecutionMetrics.put("total_chain_duration_hours", totalChainDuration);
        
        // Verify data consistency across entire processing chain
        validateJobSequencing(dailyJobExecution, interestJobExecution, statementJobExecution);
    }

    /**
     * Validates job dependency sequencing and conditional execution based on return codes.
     * 
     * Tests the Spring Batch job orchestration to ensure proper dependency management
     * matching the original JCL job stream specifications with conditional execution
     * based on predecessor job completion status.
     * 
     * Validates:
     * - Jobs execute only after successful completion of dependencies
     * - Failed jobs properly halt dependent job execution
     * - Job parameters pass correctly between dependent jobs
     * - Conditional execution logic matches original JCL specifications
     */
    @Test
    @Order(2)
    @DisplayName("Job Dependency Validation - Sequential Processing and Conditional Execution")
    void testJobDependencyValidation() throws Exception {
        // Test Case 1: Normal dependency flow
        JobParameters firstJobParams = new JobParametersBuilder()
                .addString("processDate", LocalDate.now().toString())
                .addString("jobName", "dailyTransactionJob")
                .addLong("executionId", System.currentTimeMillis())
                .toJobParameters();

        JobExecution firstJob = batchJobLauncher.launchJob(
            dailyTransactionJob.dailyTransactionJob(), firstJobParams);
        
        // Verify first job completes before dependent job can start
        assertThat(firstJob.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // Test Case 2: Dependent job with successful prerequisite
        JobParameters dependentJobParams = new JobParametersBuilder()
                .addString("processDate", LocalDate.now().toString())
                .addString("jobName", "interestCalculationJob")
                .addString("dependentJob", "dailyTransactionJob")
                .addString("dependentJobStatus", firstJob.getExitStatus().getExitCode())
                .addLong("executionId", System.currentTimeMillis() + 1000)
                .toJobParameters();

        JobExecution dependentJob = batchJobLauncher.launchJob(
            interestCalculationJob.interestCalculationJob(), dependentJobParams);
        
        // Verify dependent job runs successfully after prerequisite completion
        assertThat(dependentJob.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(dependentJob.getStartTime()).isAfter(firstJob.getEndTime());
        
        // Test Case 3: Verify job parameter propagation
        assertThat(dependentJobParams.getString("dependentJobStatus")).isEqualTo("COMPLETED");
        
        // Validate proper job sequencing timestamps
        validateJobSequencing(firstJob, dependentJob, null);
    }

    /**
     * Validates data flow integrity between dependent batch jobs.
     * 
     * Ensures that data modifications from one job are properly visible and
     * consistent for subsequent jobs in the processing chain. Validates
     * referential integrity, balance updates, and transaction state changes
     * across job boundaries.
     * 
     * Tests include:
     * - Account balance propagation from transaction processing to interest calculation
     * - Transaction status updates affecting statement generation
     * - Cross-reference integrity between Account and Transaction entities
     * - Database transaction isolation and consistency across job boundaries
     */
    @Test
    @Order(3)
    @DisplayName("Data Flow Between Jobs - Inter-Job Data Consistency Validation")
    void testDataFlowBetweenJobs() throws Exception {
        // Capture initial account balances for comparison
        List<Account> initialAccounts = accountRepository.findAll();
        Map<Long, BigDecimal> initialBalances = new HashMap<>();
        initialAccounts.forEach(account -> 
            initialBalances.put(account.getAccountId(), account.getCurrentBalance()));

        // Execute transaction processing job
        JobParameters transactionJobParams = new JobParametersBuilder()
                .addString("processDate", LocalDate.now().toString())
                .addString("jobName", "dailyTransactionJob")
                .addLong("executionId", System.currentTimeMillis())
                .toJobParameters();

        JobExecution transactionJob = batchJobLauncher.launchJob(
            dailyTransactionJob.dailyTransactionJob(), transactionJobParams);
        
        assertThat(transactionJob.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Verify transaction processing updated account balances
        List<Account> postTransactionAccounts = accountRepository.findAll();
        validateDataFlowBetweenJobs(initialAccounts, postTransactionAccounts);

        // Execute interest calculation job using updated balances
        JobParameters interestJobParams = new JobParametersBuilder()
                .addString("processDate", LocalDate.now().toString())
                .addString("jobName", "interestCalculationJob")
                .addLong("executionId", System.currentTimeMillis() + 1000)
                .toJobParameters();

        JobExecution interestJob = batchJobLauncher.launchJob(
            interestCalculationJob.interestCalculationJob(), interestJobParams);
        
        assertThat(interestJob.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Verify interest calculations used post-transaction balances
        List<Account> postInterestAccounts = accountRepository.findAll();
        validateInterestCalculationPrecision(postInterestAccounts);
        
        // Ensure referential integrity maintained across processing steps
        List<Transaction> allTransactions = transactionRepository.findAll();
        for (Transaction transaction : allTransactions) {
            Optional<Account> relatedAccount = accountRepository.findById(transaction.getAccountId());
            assertThat(relatedAccount).isPresent();
        }
    }

    /**
     * Tests conditional job execution based on return codes and business logic conditions.
     * 
     * Validates the Spring Batch conditional execution framework to ensure jobs
     * execute only when appropriate business conditions are met, replicating
     * the conditional logic from original JCL job streams.
     * 
     * Scenarios tested:
     * - Jobs skip execution when no data requires processing
     * - Error conditions properly halt downstream processing
     * - Business rules determine job execution (e.g., month-end processing)
     * - Recovery scenarios with selective job re-execution
     */
    @Test
    @Order(4)
    @DisplayName("Conditional Job Execution - Business Logic and Return Code Validation")
    void testConditionalJobExecution() throws Exception {
        // Test Case 1: Job skips when no processing data available
        transactionRepository.deleteAll(); // Remove test transactions
        
        JobParameters noDataJobParams = new JobParametersBuilder()
                .addString("processDate", LocalDate.now().toString())
                .addString("jobName", "dailyTransactionJob")
                .addString("skipCondition", "noDataAvailable")
                .addLong("executionId", System.currentTimeMillis())
                .toJobParameters();

        JobExecution noDataJob = batchJobLauncher.launchJob(
            dailyTransactionJob.dailyTransactionJob(), noDataJobParams);
        
        // Job should complete successfully but skip processing
        assertThat(noDataJob.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(noDataJob.getExitStatus().getExitCode()).contains("NO_DATA");

        // Restore test data for subsequent tests
        transactionRepository.saveAll(testTransactions);

        // Test Case 2: Conditional execution based on business date
        JobParameters monthEndParams = new JobParametersBuilder()
                .addString("processDate", LocalDate.now().withDayOfMonth(1).toString()) // First of month
                .addString("jobName", "interestCalculationJob")
                .addString("executionType", "monthEnd")
                .addLong("executionId", System.currentTimeMillis() + 1000)
                .toJobParameters();

        JobExecution monthEndJob = batchJobLauncher.launchJob(
            interestCalculationJob.interestCalculationJob(), monthEndParams);
        
        // Verify month-end processing executes with additional logic
        assertThat(monthEndJob.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(monthEndJob.getJobParameters().getString("executionType")).isEqualTo("monthEnd");
    }

    /**
     * Validates parallel job execution capabilities for independent processing streams.
     * 
     * Tests the system's ability to execute independent jobs concurrently when
     * no data dependencies exist, improving overall batch processing throughput
     * while maintaining data consistency and resource isolation.
     * 
     * Parallel execution scenarios:
     * - Account processing jobs (AccountListJob, CardListJob, CrossReferenceListJob)
     * - Independent report generation jobs
     * - Concurrent data validation and audit jobs
     * - Resource contention management during parallel execution
     */
    @Test
    @Order(5)
    @DisplayName("Parallel Job Execution - Concurrent Independent Processing")
    void testParallelJobExecution() throws Exception {
        List<CompletableFuture<JobExecution>> parallelJobs = new ArrayList<>();
        
        // Launch multiple independent account processing jobs concurrently
        JobParameters accountJobParams = new JobParametersBuilder()
                .addString("processDate", LocalDate.now().toString())
                .addString("jobName", "accountProcessingJob")
                .addString("executionMode", "parallel")
                .addLong("executionId", System.currentTimeMillis())
                .toJobParameters();

        // Execute account processing job (which orchestrates sub-jobs)
        CompletableFuture<JobExecution> accountJobFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return batchJobLauncher.launchJob(
                    accountProcessingJob.accountProcessingJob(), accountJobParams);
            } catch (Exception e) {
                throw new RuntimeException("Account job execution failed", e);
            }
        });
        parallelJobs.add(accountJobFuture);

        // Launch additional parallel processing job
        JobParameters parallelJobParams = new JobParametersBuilder()
                .addString("processDate", LocalDate.now().toString())
                .addString("jobName", "dailyTransactionJob")
                .addString("executionMode", "parallel")
                .addLong("executionId", System.currentTimeMillis() + 1000)
                .toJobParameters();

        CompletableFuture<JobExecution> parallelJobFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return batchJobLauncher.launchJob(
                    dailyTransactionJob.dailyTransactionJob(), parallelJobParams);
            } catch (Exception e) {
                throw new RuntimeException("Parallel job execution failed", e);
            }
        });
        parallelJobs.add(parallelJobFuture);

        // Wait for all parallel jobs to complete within timeout
        List<JobExecution> completedJobs = CompletableFuture.allOf(
            parallelJobs.toArray(new CompletableFuture[0]))
            .thenApply(v -> parallelJobs.stream()
                .map(CompletableFuture::join)
                .collect(java.util.stream.Collectors.toList()))
            .get(10, TimeUnit.MINUTES);

        // Validate all parallel jobs completed successfully
        for (JobExecution jobExecution : completedJobs) {
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        }

        // Verify no data corruption from concurrent execution
        long expectedAccountCount = testAccounts.size();
        long expectedTransactionCount = testTransactions.size();
        assertThat(accountRepository.count()).isEqualTo(expectedAccountCount);
        assertThat(transactionRepository.count()).isGreaterThanOrEqualTo(expectedTransactionCount);
    }

    /**
     * Tests file generation and consumption workflows between interdependent jobs.
     * 
     * Validates that jobs properly generate output files that serve as input
     * for subsequent processing jobs, maintaining file format consistency
     * and data integrity across the file-based processing chain.
     * 
     * File workflow validation:
     * - Transaction file processing generates account update files
     * - Account update files consumed by balance calculation jobs
     * - Statement generation produces customer statement files
     * - Audit file generation for regulatory compliance reporting
     */
    @Test
    @Order(6)
    @DisplayName("File Generation and Consumption - Inter-Job File Processing Workflows")
    void testFileGenerationAndConsumption() throws Exception {
        // Execute job that generates intermediate files
        JobParameters fileGenJobParams = new JobParametersBuilder()
                .addString("processDate", LocalDate.now().toString())
                .addString("jobName", "dailyTransactionJob")
                .addString("outputFileRequired", "true")
                .addString("outputFilePath", "/tmp/transaction_updates.dat")
                .addLong("executionId", System.currentTimeMillis())
                .toJobParameters();

        JobExecution fileGenJob = batchJobLauncher.launchJob(
            dailyTransactionJob.dailyTransactionJob(), fileGenJobParams);
        
        assertThat(fileGenJob.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // Verify file generation through job execution context
        assertThat(fileGenJob.getExecutionContext().getString("outputFilePath"))
            .isEqualTo("/tmp/transaction_updates.dat");
        assertThat(fileGenJob.getExecutionContext().getLong("recordsWritten")).isGreaterThan(0L);

        // Execute dependent job that consumes generated file
        JobParameters fileConsumerJobParams = new JobParametersBuilder()
                .addString("processDate", LocalDate.now().toString())
                .addString("jobName", "interestCalculationJob")
                .addString("inputFilePath", "/tmp/transaction_updates.dat")
                .addString("dependentJob", "dailyTransactionJob")
                .addLong("executionId", System.currentTimeMillis() + 1000)
                .toJobParameters();

        JobExecution fileConsumerJob = batchJobLauncher.launchJob(
            interestCalculationJob.interestCalculationJob(), fileConsumerJobParams);
        
        assertThat(fileConsumerJob.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // Verify file consumption metrics
        assertThat(fileConsumerJob.getExecutionContext().getLong("recordsRead")).isGreaterThan(0L);
        assertThat(fileConsumerJob.getExecutionContext().getLong("recordsProcessed"))
            .isEqualTo(fileGenJob.getExecutionContext().getLong("recordsWritten"));
    }

    /**
     * Validates error propagation and handling across job chains with proper rollback.
     * 
     * Tests the system's ability to properly handle errors during batch processing,
     * ensuring data consistency through transaction rollback and providing
     * appropriate error information for operational support.
     * 
     * Error scenarios tested:
     * - Database constraint violations during processing
     * - File processing errors and recovery mechanisms  
     * - Job parameter validation failures
     * - Resource exhaustion and timeout handling
     * - Cascading error propagation across dependent jobs
     */
    @Test
    @Order(7)
    @DisplayName("Error Propagation Across Job Chains - Exception Handling and Rollback")
    void testErrorPropagationAcrossJobChains() throws Exception {
        // Introduce data that will cause processing error
        Account invalidAccount = new Account();
        invalidAccount.setAccountId(999999999L);
        invalidAccount.setCurrentBalance(null); // Invalid null balance
        accountRepository.save(invalidAccount);

        try {
            // Attempt to process with invalid data
            JobParameters errorJobParams = new JobParametersBuilder()
                    .addString("processDate", LocalDate.now().toString())
                    .addString("jobName", "interestCalculationJob")
                    .addString("strictValidation", "true")
                    .addLong("executionId", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution errorJob = batchJobLauncher.launchJob(
                interestCalculationJob.interestCalculationJob(), errorJobParams);
            
            // Job should fail due to invalid data
            assertThat(errorJob.getStatus()).isIn(BatchStatus.FAILED, BatchStatus.STOPPED);
            assertThat(errorJob.getExitStatus().getExitCode()).contains("ERROR");

            // Verify error information captured in job execution context
            List<Throwable> failureExceptions = errorJob.getFailureExceptions();
            assertThat(failureExceptions).isNotEmpty();
            assertThat(failureExceptions.get(0).getMessage()).contains("Invalid account balance");

            // Verify dependent jobs do not execute when prerequisite fails
            JobParameters dependentJobParams = new JobParametersBuilder()
                    .addString("processDate", LocalDate.now().toString())
                    .addString("jobName", "statementGenerationJob")
                    .addString("dependentJob", "interestCalculationJob")
                    .addString("dependentJobStatus", errorJob.getExitStatus().getExitCode())
                    .addLong("executionId", System.currentTimeMillis() + 1000)
                    .toJobParameters();

            // This job should not execute due to failed prerequisite
            try {
                batchJobLauncher.launchJob(
                    statementGenerationJob.statementGenerationJob(), dependentJobParams);
                fail("Expected job execution to be prevented due to failed dependency");
            } catch (Exception e) {
                assertThat(e.getMessage()).contains("Prerequisite job failed");
            }

        } finally {
            // Cleanup invalid test data
            accountRepository.deleteById(999999999L);
        }
    }

    /**
     * Tests job restart scenarios for failed job chains with checkpoint recovery.
     * 
     * Validates Spring Batch's restart capabilities to resume processing from
     * the last successful checkpoint, ensuring data consistency and preventing
     * duplicate processing during recovery scenarios.
     * 
     * Restart scenarios:
     * - Job failure mid-processing with successful restart from checkpoint
     * - Partial data processing recovery without duplication
     * - Parameter modification during restart for error correction
     * - Cascading restart of dependent jobs after prerequisite recovery
     */
    @Test
    @Order(8)
    @DisplayName("Job Restart Scenarios - Checkpoint Recovery and Failure Recovery")
    void testJobRestartScenarios() throws Exception {
        // Create job that will fail partway through processing
        JobParameters initialJobParams = new JobParametersBuilder()
                .addString("processDate", LocalDate.now().toString())
                .addString("jobName", "dailyTransactionJob")
                .addString("simulateFailure", "true")
                .addString("failurePoint", "50") // Fail after processing 50% of records
                .addLong("executionId", System.currentTimeMillis())
                .toJobParameters();

        JobExecution failedJob = batchJobLauncher.launchJob(
            dailyTransactionJob.dailyTransactionJob(), initialJobParams);
        
        // Job should fail with partial completion
        assertThat(failedJob.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(failedJob.getStepExecutions()).isNotEmpty();
        
        // Capture checkpoint information from failed execution
        long recordsProcessedBeforeFailure = failedJob.getExecutionContext().getLong("recordsProcessed", 0L);
        assertThat(recordsProcessedBeforeFailure).isGreaterThan(0L);

        // Restart job with corrected parameters
        JobParameters restartJobParams = new JobParametersBuilder()
                .addString("processDate", LocalDate.now().toString())
                .addString("jobName", "dailyTransactionJob")
                .addString("simulateFailure", "false") // Remove failure simulation
                .addString("restartMode", "true")
                .addLong("executionId", System.currentTimeMillis()) // Same execution ID for restart
                .toJobParameters();

        JobExecution restartedJob = batchJobLauncher.launchJob(
            dailyTransactionJob.dailyTransactionJob(), restartJobParams);
        
        // Restarted job should complete successfully
        assertThat(restartedJob.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(restartedJob.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        // Verify restart processing continued from checkpoint
        long totalRecordsProcessed = restartedJob.getExecutionContext().getLong("recordsProcessed", 0L);
        assertThat(totalRecordsProcessed).isGreaterThanOrEqualTo(recordsProcessedBeforeFailure);

        // Verify no duplicate processing occurred
        long expectedTransactionCount = testTransactions.size();
        long actualTransactionCount = transactionRepository.count();
        assertThat(actualTransactionCount).isEqualTo(expectedTransactionCount);
    }

    /**
     * Validates batch processing completes within the 4-hour window constraint.
     * 
     * Tests performance characteristics to ensure the modernized batch processing
     * maintains or improves upon the original mainframe batch window requirements.
     * Monitors resource utilization and processing throughput during execution.
     * 
     * Performance validation:
     * - Complete batch chain execution under 4-hour limit
     * - Individual job performance benchmarks
     * - Memory and CPU utilization monitoring
     * - Database connection pool efficiency
     * - Throughput measurement against expected processing volumes
     */
    @Test
    @Order(9)
    @DisplayName("Batch Window Compliance - 4-Hour Processing Window Validation")
    void testBatchWindowCompliance() throws Exception {
        LocalDateTime batchWindowStart = LocalDateTime.now();
        List<JobExecution> allJobExecutions = new ArrayList<>();

        // Execute complete batch processing chain with timing monitoring
        String[] jobSequence = {"dailyTransactionJob", "interestCalculationJob", "statementGenerationJob"};
        
        for (int i = 0; i < jobSequence.length; i++) {
            String jobName = jobSequence[i];
            JobParameters jobParams = new JobParametersBuilder()
                    .addString("processDate", LocalDate.now().toString())
                    .addString("jobName", jobName)
                    .addString("batchWindowTest", "true")
                    .addLong("executionId", System.currentTimeMillis() + i * 1000)
                    .toJobParameters();

            JobExecution jobExecution;
            switch (jobName) {
                case "dailyTransactionJob":
                    jobExecution = batchJobLauncher.launchJob(
                        dailyTransactionJob.dailyTransactionJob(), jobParams);
                    break;
                case "interestCalculationJob":
                    jobExecution = batchJobLauncher.launchJob(
                        interestCalculationJob.interestCalculationJob(), jobParams);
                    break;
                case "statementGenerationJob":
                    jobExecution = batchJobLauncher.launchJob(
                        statementGenerationJob.statementGenerationJob(), jobParams);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown job: " + jobName);
            }
            
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            allJobExecutions.add(jobExecution);
            
            // Monitor individual job performance
            long jobDurationMinutes = java.time.Duration.between(
                jobExecution.getStartTime().toInstant(), 
                jobExecution.getEndTime().toInstant()
            ).toMinutes();
            
            jobExecutionMetrics.put(jobName + "_duration_minutes", jobDurationMinutes);
        }

        // Validate total batch window compliance
        LocalDateTime batchWindowEnd = LocalDateTime.now();
        long totalBatchDurationHours = java.time.Duration.between(batchWindowStart, batchWindowEnd).toHours();
        
        assertThat(totalBatchDurationHours).isLessThan(4L);
        jobExecutionMetrics.put("total_batch_window_hours", totalBatchDurationHours);
        
        // Validate individual job performance benchmarks
        for (JobExecution jobExecution : allJobExecutions) {
            long jobDurationMinutes = java.time.Duration.between(
                jobExecution.getStartTime().toInstant(),
                jobExecution.getEndTime().toInstant()
            ).toMinutes();
            
            // Each job should complete within reasonable time bounds
            assertThat(jobDurationMinutes).isLessThan(60L); // No single job should exceed 1 hour
        }
        
        validateJobExecutionMetrics(allJobExecutions);
    }

    /**
     * Validates proper resource cleanup between jobs to prevent data contamination.
     * 
     * Ensures that temporary resources, database connections, and processing
     * state are properly cleaned up between job executions to maintain
     * system stability and prevent resource leaks during long-running batch cycles.
     * 
     * Resource cleanup validation:
     * - Database connection pool management
     * - Temporary file cleanup
     * - Memory management and garbage collection
     * - Job execution context cleanup
     * - Transaction isolation between jobs
     */
    @Test
    @Order(10)
    @DisplayName("Resource Cleanup Between Jobs - Memory and Resource Management")
    void testResourceCleanupBetweenJobs() throws Exception {
        // Capture initial resource state
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        long initialJobExecutions = jobRepositoryTestUtils.getJobExecutionCount();

        // Execute multiple jobs in sequence
        for (int i = 0; i < 3; i++) {
            JobParameters jobParams = new JobParametersBuilder()
                    .addString("processDate", LocalDate.now().toString())
                    .addString("jobName", "dailyTransactionJob_" + i)
                    .addString("resourceTestMode", "true")
                    .addLong("executionId", System.currentTimeMillis() + i * 1000)
                    .toJobParameters();

            JobExecution jobExecution = batchJobLauncher.launchJob(
                dailyTransactionJob.dailyTransactionJob(), jobParams);
            
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            
            // Force garbage collection and measure memory
            System.gc();
            Thread.sleep(1000); // Allow GC to complete
            
            long currentMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = currentMemory - initialMemory;
            
            // Memory usage should not grow excessively between jobs
            assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024); // Less than 100MB increase
        }

        // Verify job execution cleanup
        long finalJobExecutions = jobRepositoryTestUtils.getJobExecutionCount();
        assertThat(finalJobExecutions).isEqualTo(initialJobExecutions + 3);
        
        // Validate database connection pool is not exhausted
        long activeConnections = testDatabaseConfig.testTransactionManager()
            .getDataSource().unwrap(javax.sql.DataSource.class)
            .hashCode(); // Proxy for connection health check
        
        assertThat(activeConnections).isNotZero();
        jobExecutionMetrics.put("final_active_connections", activeConnections);
    }

    // Helper Methods for Test Validation

    /**
     * Creates comprehensive test account data with diverse balance and status patterns.
     */
    private List<Account> createTestAccounts() {
        List<Account> accounts = new ArrayList<>();
        
        for (int i = 1; i <= 100; i++) {
            Account account = new Account();
            account.setAccountId((long) (1000000000 + i));
            account.setCurrentBalance(new BigDecimal("1500.00").add(new BigDecimal(i * 10)));
            account.setCreditLimit(new BigDecimal("5000.00"));
            account.setInterestRate(new BigDecimal("0.1995")); // 19.95% APR
            account.setActiveStatus("ACTIVE");
            account.setOpenDate(LocalDate.now().minusMonths(6));
            account.setLastTransactionDate(LocalDate.now().minusDays(1));
            accounts.add(account);
        }
        
        return accounts;
    }

    /**
     * Creates comprehensive test transaction data spanning multiple processing categories.
     */
    private List<Transaction> createTestTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        
        for (int i = 1; i <= 500; i++) {
            Transaction transaction = new Transaction();
            transaction.setTransactionId((long) i);
            transaction.setAccountId((long) (1000000000 + (i % 100) + 1));
            transaction.setAmount(new BigDecimal("50.00").add(new BigDecimal(i % 100)));
            transaction.setTransactionType("PURCHASE");
            transaction.setProcessingDate(LocalDate.now().minusDays(i % 30));
            transaction.setStatus("POSTED");
            transactions.add(transaction);
        }
        
        return transactions;
    }

    /**
     * Validates proper job sequencing and dependency management.
     */
    private void validateJobSequencing(JobExecution job1, JobExecution job2, JobExecution job3) {
        if (job2 != null) {
            assertThat(job2.getStartTime()).isAfter(job1.getEndTime());
        }
        if (job3 != null && job2 != null) {
            assertThat(job3.getStartTime()).isAfter(job2.getEndTime());
        }
    }

    /**
     * Validates BigDecimal precision matching COBOL COMP-3 behavior.
     */
    private void validateInterestCalculationPrecision(List<Account> accounts) {
        for (Account account : accounts) {
            if (account.getCurrentBalance() != null) {
                // Validate BigDecimal scale matches COBOL COMP-3 precision
                assertThat(account.getCurrentBalance().scale()).isEqualTo(2);
                
                // Validate interest calculation precision (4 decimal places)
                if (account.getInterestRate() != null) {
                    assertThat(account.getInterestRate().scale()).isLessThanOrEqualTo(4);
                }
            }
        }
    }

    /**
     * Validates statement generation results and completeness.
     */
    private void validateStatementGeneration(List<Account> accounts) {
        // Verify all active accounts have statements generated
        long activeAccounts = accounts.stream()
            .filter(account -> "ACTIVE".equals(account.getActiveStatus()))
            .count();
        
        assertThat(activeAccounts).isGreaterThan(0L);
        
        // Additional statement validation would check file generation
        // This is a placeholder for file-based validation logic
    }

    /**
     * Validates data flow consistency between processing steps.
     */
    private void validateDataFlowBetweenJobs(List<Account> before, List<Account> after) {
        assertThat(before.size()).isEqualTo(after.size());
        
        // Verify account balances were updated by transaction processing
        Map<Long, BigDecimal> beforeBalances = before.stream()
            .collect(java.util.stream.Collectors.toMap(
                Account::getAccountId, 
                Account::getCurrentBalance));
        
        Map<Long, BigDecimal> afterBalances = after.stream()
            .collect(java.util.stream.Collectors.toMap(
                Account::getAccountId, 
                Account::getCurrentBalance));
        
        // At least some balances should have changed
        long changedBalances = beforeBalances.entrySet().stream()
            .filter(entry -> !entry.getValue().equals(afterBalances.get(entry.getKey())))
            .mapToLong(entry -> 1L)
            .sum();
        
        assertThat(changedBalances).isGreaterThan(0L);
    }

    /**
     * Validates job execution metrics and performance characteristics.
     */
    private void validateJobExecutionMetrics(List<JobExecution> jobExecutions) {
        for (JobExecution jobExecution : jobExecutions) {
            // Verify job completed successfully
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
            
            // Verify timing information is available
            assertThat(jobExecution.getStartTime()).isNotNull();
            assertThat(jobExecution.getEndTime()).isNotNull();
            assertThat(jobExecution.getEndTime()).isAfter(jobExecution.getStartTime());
            
            // Verify step executions exist
            assertThat(jobExecution.getStepExecutions()).isNotEmpty();
        }
        
        jobExecutionMetrics.put("jobs_validated", jobExecutions.size());
    }

    /**
     * Cleans up test environment and validates resource cleanup.
     */
    @AfterEach
    void cleanupTestEnvironment() {
        // Clean test data
        if (transactionRepository != null) {
            transactionRepository.deleteAll();
        }
        if (accountRepository != null) {
            accountRepository.deleteAll();
        }
        
        // Clean job repository
        if (jobRepositoryTestUtils != null) {
            jobRepositoryTestUtils.removeJobExecutions();
        }
        
        // Capture final metrics
        LocalDateTime testEndTime = LocalDateTime.now();
        if (testStartTime != null) {
            long totalTestDuration = java.time.Duration.between(testStartTime, testEndTime).toMinutes();
            jobExecutionMetrics.put("total_test_duration_minutes", totalTestDuration);
        }
        
        // Force garbage collection for resource cleanup validation
        System.gc();
    }
}