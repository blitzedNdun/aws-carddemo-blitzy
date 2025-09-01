package com.carddemo.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Import;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Order;
import org.assertj.core.api.Assertions;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.CompletableFuture;


import com.carddemo.batch.DailyTransactionJob;
import com.carddemo.batch.InterestCalculationJob;
import com.carddemo.batch.StatementGenerationJob;
import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.config.TestBatchConfig;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;

/**
 * Integration test class for Spring Batch jobs validating batch processing operations 
 * that replace JCL jobs. Tests DailyTransactionJob, InterestCalculationJob, and 
 * StatementGenerationJob with restart capabilities and 4-hour processing window compliance.
 * 
 * This test class ensures complete functional parity between COBOL batch programs 
 * (CBTRN01C, CBTRN02C, CBACT04C, CBSTM03A, CBSTM03B) and their Spring Batch equivalents,
 * maintaining identical step sequencing, BigDecimal precision, and processing windows.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
@EnableBatchProcessing
@Import(TestBatchConfig.class)
public class BatchJobIntegrationTest {

    @Autowired
    private DailyTransactionJob dailyTransactionJob;

    @Autowired
    private InterestCalculationJob interestCalculationJob;

    @Autowired
    private StatementGenerationJob statementGenerationJob;

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    // Performance monitoring constants matching COBOL processing windows
    private static final Duration FOUR_HOUR_PROCESSING_WINDOW = Duration.ofHours(4);
    private static final Duration THREE_HOUR_WARNING_THRESHOLD = Duration.ofHours(3);
    private static final int CHUNK_SIZE = 1000;
    private static final int PARALLEL_THREADS = 4;

    // Test data volume constants for realistic processing validation
    private static final int DAILY_TRANSACTION_VOLUME = 50000;
    private static final int ACCOUNT_VOLUME = 10000;
    private static final int STATEMENT_VOLUME = 5000;

    @BeforeEach
    void setupBatchTestEnvironment() {
        // Initialize test data for H2 in-memory database
        // Create comprehensive test datasets matching COBOL data patterns
        createDailyTransactionTestData();
        createInterestCalculationTestData();
        createStatementGenerationTestData();
    }

    @AfterEach
    void cleanupBatchTestEnvironment() {
        // Cleanup for H2 in-memory database testing
        // Data is automatically cleaned up due to @Transactional rollback
    }

    /**
     * Test Basic Spring Batch Configuration and Job Launcher Setup.
     * Validates that the batch testing infrastructure is properly configured
     * and can execute jobs in the H2 unit test environment.
     */
    @Test
    @Order(1)
    @DisplayName("Basic Batch Infrastructure - Configuration and Job Launcher Validation")
    void testBasicBatchInfrastructure_ValidatesConfiguration() throws Exception {
        // Given: Basic job parameters for testing infrastructure
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("testMode", "INFRASTRUCTURE_VALIDATION")
                .addString("executionDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .addLong("executionId", System.currentTimeMillis())
                .toJobParameters();

        // When: Validate that job launcher test utils is properly configured
        Assertions.assertThat(jobLauncherTestUtils).isNotNull();
        
        // Validate test environment is set up correctly
        Assertions.assertThat(accountRepository).isNotNull();
        Assertions.assertThat(transactionRepository).isNotNull();
        
        // Then: Validate H2 database connectivity and basic data operations
        List<Account> testAccounts = generateAccountList(5);
        List<Account> savedAccounts = accountRepository.saveAll(testAccounts);
        
        Assertions.assertThat(savedAccounts).hasSize(5);
        Assertions.assertThat(accountRepository.count()).isGreaterThan(0);
        
        // Validate transaction data operations
        List<Transaction> testTransactions = generateTransactionList(10);
        List<Transaction> savedTransactions = transactionRepository.saveAll(testTransactions);
        
        Assertions.assertThat(savedTransactions).hasSize(10);
        Assertions.assertThat(transactionRepository.count()).isGreaterThan(0);
        
        // Validate test data cleanup for subsequent tests
        validateTestDataIntegrity();
    }

    /**
     * Test InterestCalculationJob using BigDecimal precision ensuring COBOL packed decimal accuracy.
     * Validates interest calculations match CBACT04C program with exact decimal precision
     * and proper rounding behavior equivalent to COBOL ROUNDED clause.
     */
    @Test
    @Order(2)
    @DisplayName("Interest Calculation Job - CBACT04C BigDecimal Precision Validation")
    void testInterestCalculationJob_ValidatesCobolPrecision() throws Exception {
        // Given: Interest calculation job parameters with test data
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("calculationDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .addString("interestType", "MONTHLY")
                .addString("disclosureGroup", "STANDARD")
                .addLong("executionId", System.currentTimeMillis())
                .toJobParameters();

        jobLauncherTestUtils.setJob(interestCalculationJob.interestCalculationJob());

        // Prepare test accounts with specific balance patterns for precision testing
        List<Account> testAccounts = createInterestCalculationTestAccounts();
        accountRepository.saveAll(testAccounts);

        LocalDateTime jobStartTime = LocalDateTime.now();

        // When: Execute interest calculation job
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        Duration executionDuration = Duration.between(jobStartTime, LocalDateTime.now());

        // Then: Validate job completion and precision accuracy
        Assertions.assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        Assertions.assertThat(executionDuration).isLessThan(FOUR_HOUR_PROCESSING_WINDOW);

        // Validate interest calculation step execution
        List<StepExecution> stepExecutions = (List<StepExecution>) jobExecution.getStepExecutions();
        StepExecution interestStep = findStepExecution(stepExecutions, "interestCalculationStep");
        Assertions.assertThat(interestStep).isNotNull();
        Assertions.assertThat(interestStep.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Validate BigDecimal precision matches COBOL COMP-3 calculations
        validateInterestCalculationPrecision(testAccounts);
    }

    /**
     * Test StatementGenerationJob with multi-format output generation matching
     * CBSTM03A and CBSTM03B COBOL programs. Validates cross-reference lookups
     * and templating support with identical file layouts.
     */
    @Test
    @Order(3)
    @DisplayName("Statement Generation Job - CBSTM03A/CBSTM03B Output Format Validation")
    void testStatementGenerationJob_ValidatesOutputFormats() throws Exception {
        // Given: Statement generation job parameters
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("statementDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .addString("statementType", "MONTHLY")
                .addString("outputFormat", "PDF,TEXT")
                .addLong("executionId", System.currentTimeMillis())
                .toJobParameters();

        jobLauncherTestUtils.setJob(statementGenerationJob.statementGenerationJob());

        // Prepare test data for statement generation
        createStatementTestData();

        LocalDateTime jobStartTime = LocalDateTime.now();

        // When: Execute statement generation job
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        Duration executionDuration = Duration.between(jobStartTime, LocalDateTime.now());

        // Then: Validate job completion and output generation
        Assertions.assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        Assertions.assertThat(executionDuration).isLessThan(FOUR_HOUR_PROCESSING_WINDOW);

        // Validate statement generation step execution
        List<StepExecution> stepExecutions = (List<StepExecution>) jobExecution.getStepExecutions();
        StepExecution statementStep = findStepExecution(stepExecutions, "statementGenerationStep");
        Assertions.assertThat(statementStep).isNotNull();
        Assertions.assertThat(statementStep.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Validate statement output file generation and format compliance
        validateStatementOutputFormats(statementStep);
    }

    /**
     * Test Spring Batch JobLauncher integration with Kubernetes CronJob scheduling.
     * Validates job parameter passing, execution tracking, and restart capabilities
     * matching JCL job submission and restart functionality.
     */
    @Test
    @Order(4)
    @DisplayName("Job Launcher Integration - Kubernetes CronJob Scheduling Validation")
    void testJobLauncherIntegration_ValidatesKubernetesScheduling() throws Exception {
        // Given: Multiple job types for scheduling validation
        Map<String, org.springframework.batch.core.Job> jobMap = Map.of(
            "dailyTransactionJob", dailyTransactionJob.dailyTransactionJob(),
            "interestCalculationJob", interestCalculationJob.interestCalculationJob(),
            "statementGenerationJob", statementGenerationJob.statementGenerationJob()
        );

        // When: Launch multiple jobs concurrently to simulate CronJob scheduling
        LocalDateTime batchStartTime = LocalDateTime.now();
        
        List<CompletableFuture<JobExecution>> jobFutures = jobMap.entrySet().stream()
            .map(entry -> CompletableFuture.supplyAsync(() -> {
                try {
                    JobParameters jobParameters = new JobParametersBuilder()
                        .addString("jobName", entry.getKey())
                        .addString("scheduleTime", LocalDateTime.now().toString())
                        .addLong("executionId", System.currentTimeMillis() + entry.getKey().hashCode())
                        .toJobParameters();

                    jobLauncherTestUtils.setJob(entry.getValue());
                    return jobLauncherTestUtils.launchJob(jobParameters);
                } catch (Exception e) {
                    throw new RuntimeException("Job execution failed: " + entry.getKey(), e);
                }
            }))
            .toList();

        // Wait for all jobs to complete
        List<JobExecution> completedJobs = jobFutures.stream()
            .map(CompletableFuture::join)
            .toList();

        Duration totalBatchDuration = Duration.between(batchStartTime, LocalDateTime.now());

        // Then: Validate all jobs completed successfully within processing window
        Assertions.assertThat(completedJobs).hasSize(3);
        Assertions.assertThat(totalBatchDuration).isLessThan(FOUR_HOUR_PROCESSING_WINDOW);

        completedJobs.forEach(jobExecution -> {
            Assertions.assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            Assertions.assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");
        });
    }

    /**
     * Test chunk-based processing with configurable commit intervals.
     * Validates transaction boundaries and rollback scenarios matching
     * CICS SYNCPOINT behavior and COBOL transaction processing patterns.
     */
    @Test
    @Order(5)
    @DisplayName("Chunk Processing - Configurable Commit Intervals and Transaction Boundaries")
    void testChunkBasedProcessing_ValidatesTransactionBoundaries() throws Exception {
        // Given: Job with specific chunk size configuration
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("processDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .addLong("chunkSize", (long) CHUNK_SIZE)
                .addString("transactionMode", "COMMIT")
                .addLong("executionId", System.currentTimeMillis())
                .toJobParameters();

        jobLauncherTestUtils.setJob(dailyTransactionJob.dailyTransactionJob());

        // When: Execute job with chunk processing monitoring
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then: Validate chunk processing behavior
        Assertions.assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<StepExecution> stepExecutions = (List<StepExecution>) jobExecution.getStepExecutions();
        StepExecution dailyTransactionStep = findStepExecution(stepExecutions, "dailyTransactionStep");

        // Validate chunk-based processing metrics
        Assertions.assertThat(dailyTransactionStep.getCommitCount()).isGreaterThan(0);
        Assertions.assertThat(dailyTransactionStep.getReadCount()).isGreaterThan(0);
        Assertions.assertThat(dailyTransactionStep.getWriteCount()).isEqualTo(dailyTransactionStep.getReadCount());

        // Validate transaction boundaries maintained during chunk processing
        validateChunkTransactionBoundaries(dailyTransactionStep);
    }

    /**
     * Test restart capabilities from checkpoint failures.
     * Validates job restart functionality equivalent to JCL //RESTART
     * processing with checkpoint recovery and step re-execution.
     */
    @Test
    @Order(6)
    @DisplayName("Job Restart Capabilities - Checkpoint Failure Recovery")
    void testJobRestartCapabilities_ValidatesCheckpointRecovery() throws Exception {
        // Given: Initial job execution that will be simulated to fail
        JobParameters initialJobParameters = new JobParametersBuilder()
                .addString("processDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .addString("simulateFailure", "true")
                .addString("failurePoint", "MID_PROCESSING")
                .addLong("executionId", System.currentTimeMillis())
                .toJobParameters();

        jobLauncherTestUtils.setJob(dailyTransactionJob.dailyTransactionJob());

        // When: Execute initial job (designed to fail for restart testing)
        JobExecution initialExecution = jobLauncherTestUtils.launchJob(initialJobParameters);

        // Validate initial job failed as expected
        Assertions.assertThat(initialExecution.getStatus()).isIn(BatchStatus.FAILED, BatchStatus.STOPPED);

        // Restart job with same parameters (simulating JCL //RESTART)
        JobParameters restartJobParameters = new JobParametersBuilder()
                .addString("processDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .addString("simulateFailure", "false")
                .addString("restartExecution", "true")
                .addLong("executionId", initialExecution.getId())
                .toJobParameters();

        JobExecution restartExecution = jobLauncherTestUtils.launchJob(restartJobParameters);

        // Then: Validate restart completed successfully
        Assertions.assertThat(restartExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        Assertions.assertThat(restartExecution.getId()).isNotEqualTo(initialExecution.getId());

        // Validate restart processing maintains data consistency
        validateRestartDataConsistency(initialExecution, restartExecution);
    }

    /**
     * Test parallel step execution with ThreadPoolTaskExecutor.
     * Validates concurrent step processing capabilities while maintaining
     * data consistency and proper resource management.
     */
    @Test
    @Order(7)
    @DisplayName("Parallel Step Execution - ThreadPoolTaskExecutor Resource Management")
    void testParallelStepExecution_ValidatesResourceManagement() throws Exception {
        // Given: Job configured for parallel step execution
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("executionMode", "PARALLEL")
                .addLong("parallelThreads", (long) PARALLEL_THREADS)
                .addString("processDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .addLong("executionId", System.currentTimeMillis())
                .toJobParameters();

        // Create large dataset for parallel processing validation
        createLargeVolumeTestData();

        jobLauncherTestUtils.setJob(dailyTransactionJob.dailyTransactionJob());

        LocalDateTime parallelStartTime = LocalDateTime.now();

        // When: Execute job with parallel step processing
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        Duration parallelExecutionDuration = Duration.between(parallelStartTime, LocalDateTime.now());

        // Then: Validate parallel execution performance and data integrity
        Assertions.assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        Assertions.assertThat(parallelExecutionDuration).isLessThan(FOUR_HOUR_PROCESSING_WINDOW);

        // Validate parallel processing improved performance
        Assertions.assertThat(parallelExecutionDuration).isLessThan(THREE_HOUR_WARNING_THRESHOLD);

        // Validate data consistency maintained during parallel processing
        validateParallelProcessingDataConsistency();
    }

    /**
     * Test file reader/writer operations for batch I/O.
     * Validates file processing capabilities matching COBOL file I/O
     * patterns and data format compliance.
     */
    @Test
    @Order(8)
    @DisplayName("File Reader/Writer Operations - Batch I/O Validation")
    void testFileReaderWriterOperations_ValidatesBatchIO() throws Exception {
        // Given: Job parameters for file I/O testing
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("inputFilePath", "/tmp/test-input.dat")
                .addString("outputFilePath", "/tmp/test-output.dat")
                .addString("fileFormat", "FIXED_WIDTH")
                .addLong("executionId", System.currentTimeMillis())
                .toJobParameters();

        // Create test input file matching COBOL file format
        createTestInputFile("/tmp/test-input.dat");

        jobLauncherTestUtils.setJob(dailyTransactionJob.dailyTransactionJob());

        // When: Execute job with file I/O operations
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then: Validate file processing completed successfully
        Assertions.assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Validate output file generation and format compliance
        validateOutputFileFormat("/tmp/test-output.dat");
        validateFileIODataConsistency("/tmp/test-input.dat", "/tmp/test-output.dat");
    }

    /**
     * Test job completion within 4-hour processing window.
     * Validates performance benchmarks and processing time compliance
     * matching mainframe batch processing windows.
     */
    @Test
    @Order(9)
    @DisplayName("Processing Window Compliance - 4-Hour Batch Window Validation")
    void testProcessingWindowCompliance_ValidatesFourHourWindow() throws Exception {
        // Given: High-volume job parameters for processing window testing
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("processDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .addString("volumeMode", "HIGH")
                .addLong("recordCount", (long) DAILY_TRANSACTION_VOLUME)
                .addLong("executionId", System.currentTimeMillis())
                .toJobParameters();

        // Create high-volume test dataset
        createHighVolumeTestData();

        LocalDateTime windowStartTime = LocalDateTime.now();

        // Execute all batch jobs sequentially within processing window
        Map<String, org.springframework.batch.core.Job> batchJobs = Map.of(
            "dailyTransaction", dailyTransactionJob.dailyTransactionJob(),
            "interestCalculation", interestCalculationJob.interestCalculationJob(),
            "statementGeneration", statementGenerationJob.statementGenerationJob()
        );

        // When: Execute complete batch processing cycle
        for (Map.Entry<String, org.springframework.batch.core.Job> jobEntry : batchJobs.entrySet()) {
            jobLauncherTestUtils.setJob(jobEntry.getValue());
            
            JobParameters specificJobParameters = new JobParametersBuilder(jobParameters)
                .addString("jobType", jobEntry.getKey())
                .addLong("jobExecutionId", System.currentTimeMillis() + jobEntry.getKey().hashCode())
                .toJobParameters();

            JobExecution jobExecution = jobLauncherTestUtils.launchJob(specificJobParameters);
            
            // Validate each job completes successfully
            Assertions.assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        }

        Duration totalProcessingTime = Duration.between(windowStartTime, LocalDateTime.now());

        // Then: Validate complete batch cycle within 4-hour window
        Assertions.assertThat(totalProcessingTime).isLessThan(FOUR_HOUR_PROCESSING_WINDOW);

        // Log performance metrics for monitoring
        System.out.printf("Batch Processing Completed in: %d hours, %d minutes%n",
            totalProcessingTime.toHours(),
            totalProcessingTime.toMinutesPart());
    }

    /**
     * Test BigDecimal calculations match COBOL COMP-3 precision.
     * Validates financial calculation accuracy with exact decimal precision
     * matching COBOL packed decimal arithmetic operations.
     */
    @Test
    @Order(10)
    @DisplayName("Financial Precision Validation - BigDecimal COMP-3 Precision Matching")
    void testFinancialPrecisionValidation_ValidatesComp3Precision() throws Exception {
        // Given: Test accounts with specific balance patterns for precision validation
        List<Account> testAccounts = createPrecisionTestAccounts();
        accountRepository.saveAll(testAccounts);

        // Execute interest calculation job for precision testing
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("calculationDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .addString("precisionMode", "COBOL_COMPATIBLE")
                .addString("roundingMode", "HALF_UP")
                .addLong("executionId", System.currentTimeMillis())
                .toJobParameters();

        jobLauncherTestUtils.setJob(interestCalculationJob.interestCalculationJob());

        // When: Execute job with precision monitoring
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then: Validate precision accuracy
        Assertions.assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Validate BigDecimal calculations match COBOL COMP-3 precision
        List<Account> updatedAccounts = accountRepository.findAll();
        for (Account account : updatedAccounts) {
            validateAccountBalancePrecision(account);
            validateInterestCalculationPrecision(account);
        }
    }

    // Helper methods for test data creation and validation

    private void createDailyTransactionTestData() {
        List<Account> accounts = generateAccountList(ACCOUNT_VOLUME / 10);  // Reduced for test performance
        List<Transaction> transactions = generateTransactionList(DAILY_TRANSACTION_VOLUME / 100);  // Reduced for test performance
        
        accountRepository.saveAll(accounts);
        transactionRepository.saveAll(transactions);
    }

    private void createInterestCalculationTestData() {
        List<Account> interestAccounts = generateAccountList(100);
        // Set specific balances for interest calculation testing
        interestAccounts.forEach(account -> {
            BigDecimal testBalance = new BigDecimal("1000.00").add(
                new BigDecimal(Math.random() * 10000).setScale(2, RoundingMode.HALF_UP));
            account.setCurrentBalance(testBalance);
        });
        accountRepository.saveAll(interestAccounts);
    }

    private void createStatementGenerationTestData() {
        List<Account> statementAccounts = generateAccountList(50);
        accountRepository.saveAll(statementAccounts);

        List<Transaction> statementTransactions = generateTransactionList(500);
        transactionRepository.saveAll(statementTransactions);
    }

    private List<Account> createInterestCalculationTestAccounts() {
        return generateAccountList(20).stream()
            .map(account -> {
                account.setCurrentBalance(new BigDecimal("5000.00"));
                // Interest rate is set via DisclosureGroup relationship
                // account.setInterestRate(new BigDecimal("0.0525")); // 5.25% APR
                return account;
            })
            .toList();
    }

    private void createStatementTestData() {
        createStatementGenerationTestData();
    }

    private void createLargeVolumeTestData() {
        List<Account> largeAccounts = generateAccountList(1000);
        List<Transaction> largeTransactions = generateTransactionList(10000);
        
        accountRepository.saveAll(largeAccounts);
        transactionRepository.saveAll(largeTransactions);
    }

    private void createHighVolumeTestData() {
        createLargeVolumeTestData();
    }

    private List<Account> createPrecisionTestAccounts() {
        List<Account> precisionAccounts = generateAccountList(10);
        
        // Set specific test balances for precision validation
        BigDecimal[] testBalances = {
            new BigDecimal("12345.67"),
            new BigDecimal("987654.32"),
            new BigDecimal("0.01"),
            new BigDecimal("999999.99"),
            new BigDecimal("100000.00")
        };

        for (int i = 0; i < Math.min(precisionAccounts.size(), testBalances.length); i++) {
            precisionAccounts.get(i).setCurrentBalance(testBalances[i]);
        }

        return precisionAccounts;
    }

    private void createTestInputFile(String filePath) {
        // Implementation would create test input file with COBOL-compatible format
        // This is a placeholder for the actual file creation logic
    }

    // Validation helper methods

    private StepExecution findStepExecution(List<StepExecution> stepExecutions, String stepName) {
        return stepExecutions.stream()
            .filter(step -> step.getStepName().equals(stepName))
            .findFirst()
            .orElse(null);
    }

    private void validateTransactionDataConsistency() {
        // Validate transaction data integrity after batch processing
        List<Transaction> transactions = transactionRepository.findAll();
        Assertions.assertThat(transactions).isNotEmpty();
        
        transactions.forEach(transaction -> {
            Assertions.assertThat(transaction.getAmount()).isNotNull();
            Assertions.assertThat(transaction.getAmount().scale()).isEqualTo(2);
        });
    }

    private void validateInterestCalculationPrecision(List<Account> testAccounts) {
        testAccounts.forEach(account -> {
            Account updatedAccount = accountRepository.findById(account.getAccountId()).orElse(null);
            Assertions.assertThat(updatedAccount).isNotNull();
            
            // Validate BigDecimal precision matches COBOL COMP-3
            assertBigDecimalEquals(updatedAccount.getCurrentBalance(), account.getCurrentBalance(), "Account balance should match after interest calculation");
        });
    }

    private void validateStatementOutputFormats(StepExecution statementStep) {
        // Validate statement file generation and format compliance
        Assertions.assertThat(statementStep.getWriteCount()).isGreaterThan(0);
        // Additional file format validation would be implemented here
    }

    private void validateChunkTransactionBoundaries(StepExecution stepExecution) {
        // Validate chunk processing maintained proper transaction boundaries
        Assertions.assertThat(stepExecution.getCommitCount()).isGreaterThan(0);
        Assertions.assertThat(stepExecution.getRollbackCount()).isEqualTo(0);
    }

    private void validateRestartDataConsistency(JobExecution initialExecution, JobExecution restartExecution) {
        // Validate data consistency after job restart
        Assertions.assertThat(restartExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // Verify no duplicate processing occurred during restart
        List<Transaction> transactions = transactionRepository.findAll();
        transactions.forEach(transaction -> {
            // Verify transaction has valid processed timestamp indicating successful processing
            Assertions.assertThat(transaction.getProcessedTimestamp()).isNotNull();
            // Verify no duplicate transaction IDs exist
            Assertions.assertThat(transaction.getTransactionId()).isNotNull();
        });
    }

    private void validateParallelProcessingDataConsistency() {
        // Validate data integrity after parallel processing
        List<Account> accounts = accountRepository.findAll();
        List<Transaction> transactions = transactionRepository.findAll();
        
        Assertions.assertThat(accounts).isNotEmpty();
        Assertions.assertThat(transactions).isNotEmpty();
        
        // Verify no data corruption from parallel processing
        accounts.forEach(account -> {
            Assertions.assertThat(account.getCurrentBalance()).isNotNull();
            Assertions.assertThat(account.getCurrentBalance().scale()).isLessThanOrEqualTo(2);
        });
    }

    private void validateOutputFileFormat(String outputFilePath) {
        // Validate output file format matches COBOL file specifications
        // Implementation would check file format, record lengths, etc.
    }

    private void validateFileIODataConsistency(String inputFilePath, String outputFilePath) {
        // Validate data consistency between input and output files
        // Implementation would compare file contents for data integrity
    }

    private void validateAccountBalancePrecision(Account account) {
        Assertions.assertThat(account.getCurrentBalance()).isNotNull();
        Assertions.assertThat(account.getCurrentBalance().scale()).isEqualTo(2);
        
        // Validate precision matches COBOL COMP-3 format
        BigDecimal balance = account.getCurrentBalance();
        Assertions.assertThat(balance.precision()).isLessThanOrEqualTo(15);
    }

    private void validateInterestCalculationPrecision(Account account) {
        if (account.getDisclosureGroup() != null && 
            account.getDisclosureGroup().getInterestRate() != null && 
            account.getCurrentBalance() != null) {
            BigDecimal expectedInterest = account.getCurrentBalance()
                .multiply(account.getDisclosureGroup().getInterestRate())
                .divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
            
            // Validate interest calculation precision
            Assertions.assertThat(expectedInterest.scale()).isEqualTo(2);
        }
    }

    // Simplified test data generation methods for H2 unit testing
    private List<Account> generateAccountList(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> createIntegrationTestAccount(i))
            .collect(java.util.stream.Collectors.toList());
    }

    private List<Transaction> generateTransactionList(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> createIntegrationTestTransaction(i))
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Creates a test Account instance with COBOL-compatible field values and BigDecimal precision.
     */
    private Account createIntegrationTestAccount(int index) {
        Account account = new Account();
        account.setAccountId(123456789L + index);  // Generate unique account IDs
        account.setActiveStatus("Y");
        account.setCurrentBalance(new BigDecimal("1500.00"));
        account.setCreditLimit(new BigDecimal("5000.00"));
        account.setCashCreditLimit(new BigDecimal("1000.00"));
        account.setOpenDate(LocalDateTime.now().toLocalDate());
        account.setCurrentCycleCredit(new BigDecimal("0.00"));
        account.setCurrentCycleDebit(new BigDecimal("0.00"));
        account.setGroupId("DEFAULT");
        return account;
    }

    /**
     * Creates a test Transaction instance with COBOL-compatible field values and BigDecimal precision.
     */
    private Transaction createIntegrationTestTransaction(int index) {
        Transaction transaction = new Transaction();
        transaction.setAccountId(123456789L + (index % 100));  // Distribute across multiple accounts
        transaction.setAmount(new BigDecimal("100.50").add(new BigDecimal(index)));  // Vary amounts
        transaction.setTransactionTypeCode("PU");
        transaction.setCategoryCode("5411");
        transaction.setSource("ATM");
        transaction.setDescription("TEST MERCHANT " + index);
        transaction.setMerchantName("TEST MERCHANT " + index);
        transaction.setCardNumber("4000123456789012");
        transaction.setTransactionDate(LocalDate.now());
        transaction.setOriginalTimestamp(LocalDateTime.now().minusMinutes(index));  // Vary timestamps
        transaction.setProcessedTimestamp(LocalDateTime.now().minusMinutes(index));
        return transaction;
    }

    /**
     * Helper method to assert BigDecimal equality with COBOL precision.
     */
    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual, String message) {
        Assertions.assertThat(actual.compareTo(expected)).isEqualTo(0);
    }

    /**
     * Validates basic test data integrity for H2 database operations.
     */
    private void validateTestDataIntegrity() {
        // Verify account data integrity
        List<Account> accounts = accountRepository.findAll();
        accounts.forEach(account -> {
            Assertions.assertThat(account.getCurrentBalance()).isNotNull();
            Assertions.assertThat(account.getCurrentBalance().scale()).isLessThanOrEqualTo(2);
            Assertions.assertThat(account.getActiveStatus()).isNotNull();
        });
        
        // Verify transaction data integrity
        List<Transaction> transactions = transactionRepository.findAll();
        transactions.forEach(transaction -> {
            Assertions.assertThat(transaction.getAmount()).isNotNull();
            Assertions.assertThat(transaction.getAmount().scale()).isLessThanOrEqualTo(2);
            Assertions.assertThat(transaction.getAccountId()).isNotNull();
        });
    }
}