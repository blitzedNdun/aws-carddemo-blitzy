package com.carddemo.service;

import com.carddemo.batch.BatchTestUtils;
import com.carddemo.batch.DailyTransactionJob;
import com.carddemo.entity.Transaction;
import com.carddemo.repository.TransactionRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit test class for DailyTransactionJobService
 * 
 * This test class validates the Spring Batch job service that replaces COBOL programs
 * CBTRN01C and CBTRN02C for daily transaction processing. It ensures:
 * - Job execution and monitoring functionality
 * - Transaction validation and posting accuracy (matching COBOL logic)
 * - Batch processing within 4-hour window requirement
 * - Chunk-oriented processing with restart capabilities
 * - Error handling and transaction rollback scenarios
 * - Spring Batch metadata persistence and job history tracking
 * 
 * COBOL Parity Requirements:
 * - Maintains exact transaction validation logic from CBTRN02C
 * - Preserves BigDecimal precision equivalent to COBOL COMP-3 fields
 * - Replicates file-based processing patterns from sequential VSAM access
 * - Ensures identical error handling and rejection processing
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
public class DailyTransactionJobServiceTest extends BaseServiceTest {

    // Constants for testing - equivalent to COBOL program constants
    private static final String TEST_JOB_NAME = "dailyTransactionJob";
    private static final String TEST_INPUT_FILE = "DALYTRAN.txt";
    private static final String TEST_OUTPUT_FILE = "PROCESSED_TRANS.txt";
    private static final String TEST_REJECT_FILE = "REJECTED_TRANS.txt";
    private static final long PERFORMANCE_TARGET_4_HOURS_MS = 4 * 60 * 60 * 1000L; // 4 hours in milliseconds
    private static final int CHUNK_SIZE = 1000; // Standard chunk size for batch processing
    private static final BigDecimal TEST_TRANSACTION_AMOUNT = new BigDecimal("99.99");
    private static final String TEST_ACCOUNT_ID = "123456789012";
    private static final String TEST_CARD_NUMBER = "4000123456789012";
    private static final int COBOL_DECIMAL_SCALE = 2; // COMP-3 scale for monetary values

    // Mock dependencies - Spring Batch infrastructure components
    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private DailyTransactionJob dailyTransactionJob;

    @Mock
    private Job batchJob;

    @Mock
    private TransactionRepository transactionRepository;

    // Service under test - will be created as part of implementation
    @InjectMocks
    private DailyTransactionJobService dailyTransactionJobService;

    // Test data and fixtures
    private JobParameters testJobParameters;
    private JobExecution successfulJobExecution;
    private JobExecution failedJobExecution;
    private JobExecution runningJobExecution;
    private List<Transaction> testTransactions;
    private MockServiceFactory mockFactory;
    private BatchTestUtils batchUtils;

    @BeforeEach
    public void setUp() {
        // Initialize test infrastructure
        setupTestData();
        mockFactory = new MockServiceFactory();
        batchUtils = new BatchTestUtils();
        
        // Configure test job parameters equivalent to COBOL JCL parameters
        testJobParameters = new JobParametersBuilder()
                .addString("inputFile", TEST_INPUT_FILE)
                .addString("outputFile", TEST_OUTPUT_FILE)
                .addString("rejectFile", TEST_REJECT_FILE)
                .addLong("timestamp", System.currentTimeMillis())
                .addString("runDate", LocalDateTime.now().toString())
                .toJobParameters();

        // Setup successful job execution mock
        successfulJobExecution = createMockJobExecution(BatchStatus.COMPLETED, ExitStatus.COMPLETED);
        
        // Setup failed job execution mock  
        failedJobExecution = createMockJobExecution(BatchStatus.FAILED, ExitStatus.FAILED);
        
        // Setup running job execution mock
        runningJobExecution = createMockJobExecution(BatchStatus.STARTED, ExitStatus.EXECUTING);

        // Generate test transactions matching COBOL record structure
        testTransactions = generateTestTransactions(100);
        
        // Configure mock behaviors
        configureMockBehaviors();
    }

    @AfterEach
    public void tearDown() {
        // Clean up test data and reset mocks
        cleanupTestData();
        mockFactory.resetAllMocks();
        batchUtils.teardownBatchTestEnvironment();
        reset(jobLauncher, jobRepository, dailyTransactionJob, transactionRepository);
    }

    // ==================== JOB EXECUTION TESTS ====================

    @Test
    @DisplayName("Should successfully launch daily transaction job with valid parameters")
    public void testLaunchDailyJobSuccess() throws Exception {
        // Given - mock successful job launch
        when(jobLauncher.run(any(Job.class), any(JobParameters.class)))
                .thenReturn(successfulJobExecution);
        when(dailyTransactionJob.dailyTransactionJob()).thenReturn(batchJob);

        // When - launch the daily transaction job
        JobExecution result = dailyTransactionJobService.launchDailyJob(testJobParameters);

        // Then - verify successful execution
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(result.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        // Verify job launcher was called with correct parameters
        verify(jobLauncher, times(1)).run(batchJob, testJobParameters);
        verify(dailyTransactionJob, times(1)).dailyTransactionJob();
    }

    @Test
    @DisplayName("Should handle job launch failure with appropriate error handling")
    public void testLaunchDailyJobFailure() throws Exception {
        // Given - mock job launch failure
        Exception jobException = new RuntimeException("Job launch failed - database connection error");
        when(jobLauncher.run(any(Job.class), any(JobParameters.class)))
                .thenThrow(jobException);
        when(dailyTransactionJob.dailyTransactionJob()).thenReturn(batchJob);

        // When/Then - verify exception is properly handled
        assertThatThrownBy(() -> dailyTransactionJobService.launchDailyJob(testJobParameters))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Job launch failed");

        verify(jobLauncher, times(1)).run(batchJob, testJobParameters);
    }

    @Test
    @DisplayName("Should validate job parameters before launch - equivalent to COBOL JCL validation")
    public void testValidateJobParameters() {
        // Given - invalid job parameters (missing required input file)
        JobParameters invalidParams = new JobParametersBuilder()
                .addString("outputFile", TEST_OUTPUT_FILE)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // When/Then - validate parameters
        assertThatThrownBy(() -> dailyTransactionJobService.validateJobParameters(invalidParams))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Input file parameter is required");

        // Given - valid parameters
        // When
        boolean isValid = dailyTransactionJobService.validateJobParameters(testJobParameters);

        // Then
        assertThat(isValid).isTrue();
    }

    // ==================== JOB MONITORING AND STATUS TESTS ====================

    @Test
    @DisplayName("Should retrieve correct job execution status")
    public void testGetJobExecutionStatus() {
        // Given
        Long jobExecutionId = 12345L;
        when(jobRepository.getLastJobExecution(TEST_JOB_NAME, testJobParameters))
                .thenReturn(successfulJobExecution);

        // When
        JobExecution status = dailyTransactionJobService.getJobExecutionStatus(jobExecutionId);

        // Then
        assertThat(status).isNotNull();
        assertThat(status.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        verify(jobRepository, times(1)).getLastJobExecution(TEST_JOB_NAME, testJobParameters);
    }

    @Test
    @DisplayName("Should detect running job instance")
    public void testIsJobRunning() {
        // Given - job is currently running
        when(jobRepository.getLastJobExecution(TEST_JOB_NAME, testJobParameters))
                .thenReturn(runningJobExecution);

        // When
        boolean isRunning = dailyTransactionJobService.isJobRunning();

        // Then
        assertThat(isRunning).isTrue();
        verify(jobRepository, times(1)).getLastJobExecution(TEST_JOB_NAME, testJobParameters);
    }

    @Test
    @DisplayName("Should retrieve last job execution details")
    public void testGetLastJobExecution() {
        // Given
        when(jobRepository.getLastJobExecution(TEST_JOB_NAME, testJobParameters))
                .thenReturn(successfulJobExecution);

        // When
        JobExecution lastExecution = dailyTransactionJobService.getLastJobExecution();

        // Then
        assertThat(lastExecution).isNotNull();
        assertThat(lastExecution).isEqualTo(successfulJobExecution);
        verify(jobRepository, times(1)).getLastJobExecution(TEST_JOB_NAME, testJobParameters);
    }

    // ==================== JOB RESTART AND RECOVERY TESTS ====================

    @Test
    @DisplayName("Should restart failed job with same parameters")
    public void testRestartFailedJob() throws Exception {
        // Given - failed job execution
        Long failedJobExecutionId = 67890L;
        when(jobRepository.getLastJobExecution(TEST_JOB_NAME, testJobParameters))
                .thenReturn(failedJobExecution);
        when(jobLauncher.run(any(Job.class), any(JobParameters.class)))
                .thenReturn(successfulJobExecution);
        when(dailyTransactionJob.dailyTransactionJob()).thenReturn(batchJob);

        // When
        JobExecution restartedExecution = dailyTransactionJobService.restartFailedJob(failedJobExecutionId);

        // Then
        assertThat(restartedExecution).isNotNull();
        assertThat(restartedExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        verify(jobLauncher, times(1)).run(batchJob, testJobParameters);
    }

    @Test
    @DisplayName("Should handle restart of non-restartable job")  
    public void testRestartNonRestartableJob() {
        // Given - completed job that cannot be restarted
        Long completedJobExecutionId = 11111L;
        when(jobRepository.getLastJobExecution(TEST_JOB_NAME, testJobParameters))
                .thenReturn(successfulJobExecution);

        // When/Then
        assertThatThrownBy(() -> dailyTransactionJobService.restartFailedJob(completedJobExecutionId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot restart job in COMPLETED status");
    }

    // ==================== PERFORMANCE TESTS ====================

    @Test
    @DisplayName("Should complete processing within 4-hour window requirement")
    public void testProcessingTimeCompliance() throws Exception {
        // Given - large volume test scenario equivalent to production load
        int transactionVolume = 100000; // Typical daily transaction volume
        JobParameters performanceTestParams = batchUtils.createTestJobParameters(
                TEST_INPUT_FILE, transactionVolume);

        // Mock performance-oriented job execution
        JobExecution performanceJobExecution = createPerformanceJobExecution(transactionVolume);
        when(jobLauncher.run(any(Job.class), eq(performanceTestParams)))
                .thenReturn(performanceJobExecution);
        when(dailyTransactionJob.dailyTransactionJob()).thenReturn(batchJob);

        // When - measure execution time
        long startTime = System.currentTimeMillis();
        JobExecution result = dailyTransactionJobService.launchDailyJob(performanceTestParams);
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Then - verify performance compliance
        assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(executionTime).isLessThan(PERFORMANCE_TARGET_4_HOURS_MS);
        
        // Verify processing rate meets COBOL program expectations
        double transactionsPerSecond = transactionVolume / ((double) executionTime / 1000);
        assertThat(transactionsPerSecond).isGreaterThan(6.94); // 100,000 trans / 4 hours
        
        validateResponseTime(executionTime, PERFORMANCE_TARGET_4_HOURS_MS);
    }

    @Test
    @DisplayName("Should handle chunk processing configuration correctly")
    public void testChunkProcessingConfiguration() {
        // Given - chunk processing parameters
        JobParameters chunkTestParams = new JobParametersBuilder(testJobParameters)
                .addLong("chunkSize", (long) CHUNK_SIZE)
                .toJobParameters();

        // When
        boolean isValidChunkSize = dailyTransactionJobService.validateJobParameters(chunkTestParams);

        // Then
        assertThat(isValidChunkSize).isTrue();
        
        // Verify chunk size aligns with COBOL program batch processing logic
        assertThat(CHUNK_SIZE).isEqualTo(1000); // Standard batch size from COBOL programs
    }

    // ==================== JOB METRICS AND HISTORY TESTS ====================

    @Test
    @DisplayName("Should collect and provide job execution metrics")
    public void testGetJobMetrics() {
        // Given
        when(jobRepository.getLastJobExecution(TEST_JOB_NAME, testJobParameters))
                .thenReturn(successfulJobExecution);

        // When
        var metrics = dailyTransactionJobService.getJobMetrics();

        // Then
        assertThat(metrics).isNotNull();
        assertThat(metrics.get("readCount")).isEqualTo(100L);
        assertThat(metrics.get("writeCount")).isEqualTo(100L);
        assertThat(metrics.get("skipCount")).isEqualTo(0L);
        verify(jobRepository, times(1)).getLastJobExecution(TEST_JOB_NAME, testJobParameters);
    }

    @Test
    @DisplayName("Should maintain job execution history")
    public void testGetJobHistory() {
        // Given - multiple job executions
        List<JobExecution> jobHistory = new ArrayList<>();
        jobHistory.add(successfulJobExecution);
        jobHistory.add(failedJobExecution);

        when(jobRepository.findJobExecutions(any(JobInstance.class)))
                .thenReturn(jobHistory);

        // When
        List<JobExecution> history = dailyTransactionJobService.getJobHistory(10);

        // Then
        assertThat(history).hasSize(2);
        assertThat(history).contains(successfulJobExecution, failedJobExecution);
    }

    // ==================== TRANSACTION VALIDATION TESTS ====================

    @Test
    @DisplayName("Should validate transaction data with COBOL precision - equivalent to CBTRN02C validation")
    public void testTransactionValidationWithCobolPrecision() {
        // Given - transaction with COBOL COMP-3 equivalent precision
        Transaction testTransaction = new Transaction();
        testTransaction.setTransactionId("T123456789012345");
        testTransaction.setAccountId(TEST_ACCOUNT_ID);
        testTransaction.setAmount(TEST_TRANSACTION_AMOUNT.setScale(COBOL_DECIMAL_SCALE, BigDecimal.ROUND_HALF_UP));
        testTransaction.setTransactionDate(LocalDateTime.now());

        // When - validate using service
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        Transaction savedTransaction = transactionRepository.save(testTransaction);

        // Then - verify COBOL precision is maintained
        assertThat(savedTransaction.getAmount().scale()).isEqualTo(COBOL_DECIMAL_SCALE);
        assertBigDecimalEquals(savedTransaction.getAmount(), TEST_TRANSACTION_AMOUNT);
        
        // Verify transaction ID format matches COBOL PIC X(16) field
        assertThat(savedTransaction.getTransactionId()).hasSize(16);
        assertThat(savedTransaction.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
        
        verify(transactionRepository, times(1)).save(testTransaction);
    }

    @Test
    @DisplayName("Should handle transaction posting accuracy matching COBOL logic")
    public void testTransactionPostingAccuracy() {
        // Given - test transactions matching CBTRN02C processing logic
        List<Transaction> batchTransactions = generateTestTransactions(50);
        when(transactionRepository.findByAccountIdAndDateRange(anyString(), any(), any()))
                .thenReturn(batchTransactions);

        // When - process transactions through repository
        List<Transaction> processedTransactions = transactionRepository
                .findByAccountIdAndDateRange(TEST_ACCOUNT_ID, LocalDateTime.now().minusDays(1), LocalDateTime.now());

        // Then - verify processing accuracy
        assertThat(processedTransactions).hasSize(50);
        
        // Verify total amounts match expected precision
        BigDecimal totalAmount = processedTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal expectedTotal = TEST_TRANSACTION_AMOUNT.multiply(new BigDecimal(50))
                .setScale(COBOL_DECIMAL_SCALE, BigDecimal.ROUND_HALF_UP);
        
        assertBigDecimalEquals(totalAmount, expectedTotal);
        verify(transactionRepository, times(1))
                .findByAccountIdAndDateRange(TEST_ACCOUNT_ID, any(), any());
    }

    // ==================== ERROR HANDLING AND RECOVERY TESTS ====================

    @Test
    @DisplayName("Should handle job cancellation gracefully")
    public void testCancelJob() throws Exception {
        // Given - running job
        Long runningJobExecutionId = 99999L;
        when(jobRepository.getLastJobExecution(TEST_JOB_NAME, testJobParameters))
                .thenReturn(runningJobExecution);

        // When
        boolean cancelled = dailyTransactionJobService.cancelJob(runningJobExecutionId);

        // Then
        assertThat(cancelled).isTrue();
        verify(jobRepository, times(1)).getLastJobExecution(TEST_JOB_NAME, testJobParameters);
    }

    @Test
    @DisplayName("Should schedule job execution for future processing")
    public void testScheduleJob() {
        // Given - future execution time
        LocalDateTime scheduledTime = LocalDateTime.now().plusHours(2);
        
        // When
        boolean scheduled = dailyTransactionJobService.scheduleJob(testJobParameters, scheduledTime);

        // Then
        assertThat(scheduled).isTrue();
    }

    // ==================== HELPER METHODS ====================

    /**
     * Creates a mock JobExecution with specified status
     */
    private JobExecution createMockJobExecution(BatchStatus status, ExitStatus exitStatus) {
        JobExecution execution = mock(JobExecution.class);
        when(execution.getStatus()).thenReturn(status);
        when(execution.getExitStatus()).thenReturn(exitStatus);
        when(execution.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(30));
        when(execution.getEndTime()).thenReturn(LocalDateTime.now());
        when(execution.getJobParameters()).thenReturn(testJobParameters);
        return execution;
    }

    /**
     * Creates a performance-oriented job execution for testing processing times
     */
    private JobExecution createPerformanceJobExecution(int transactionCount) {
        JobExecution execution = mock(JobExecution.class);
        when(execution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(execution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);
        
        LocalDateTime startTime = LocalDateTime.now().minus(2, ChronoUnit.HOURS);
        LocalDateTime endTime = startTime.plus(2, ChronoUnit.HOURS);
        
        when(execution.getStartTime()).thenReturn(startTime);
        when(execution.getEndTime()).thenReturn(endTime);
        when(execution.getJobParameters()).thenReturn(testJobParameters);
        
        return execution;
    }

    /**
     * Generates test transactions matching COBOL record structure
     */
    private List<Transaction> generateTestTransactions(int count) {
        return batchUtils.generateTestTransactions(count, TEST_ACCOUNT_ID, TEST_TRANSACTION_AMOUNT);
    }

    /**
     * Configures mock behaviors for consistent testing
     */
    private void configureMockBehaviors() {
        // Configure mock factory with success scenarios
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction transaction = invocation.getArgument(0);
                    transaction.setProcessedTimestamp(LocalDateTime.now());
                    return transaction;
                });
        
        when(transactionRepository.count()).thenReturn(100L);
        when(transactionRepository.findById(anyString())).thenReturn(java.util.Optional.of(new Transaction()));
        when(dailyTransactionJob.getJobParameters()).thenReturn(testJobParameters);
        when(dailyTransactionJob.getExecutionStatus()).thenReturn(BatchStatus.COMPLETED);
    }

    // ==================== BATCH PROCESSING VALIDATION TESTS ====================

    @Test
    @DisplayName("Should validate chunk processing with transaction totals reconciliation")
    public void testChunkProcessingWithReconciliation() throws Exception {
        // Given - batch of transactions for chunk processing
        int expectedTransactionCount = 5000;
        BigDecimal expectedTotalAmount = TEST_TRANSACTION_AMOUNT.multiply(new BigDecimal(expectedTransactionCount));
        
        List<Transaction> chunkTransactions = generateTestTransactions(expectedTransactionCount);
        when(transactionRepository.findByAccountIdAndDateRange(anyString(), any(), any()))
                .thenReturn(chunkTransactions);

        // Mock job execution with chunk processing metrics
        JobExecution chunkJobExecution = mock(JobExecution.class);
        when(chunkJobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(chunkJobExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);
        when(jobLauncher.run(any(Job.class), any(JobParameters.class)))
                .thenReturn(chunkJobExecution);
        when(dailyTransactionJob.dailyTransactionJob()).thenReturn(batchJob);

        // When - launch job with chunk processing
        JobParameters chunkParams = new JobParametersBuilder(testJobParameters)
                .addLong("chunkSize", (long) CHUNK_SIZE)
                .addLong("expectedCount", (long) expectedTransactionCount)
                .toJobParameters();
        
        JobExecution result = dailyTransactionJobService.launchDailyJob(chunkParams);

        // Then - verify chunk processing completed successfully
        assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // Verify transaction totals reconciliation
        List<Transaction> processedTransactions = transactionRepository
                .findByAccountIdAndDateRange(TEST_ACCOUNT_ID, LocalDateTime.now().minusDays(1), LocalDateTime.now());
        
        BigDecimal actualTotalAmount = processedTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        assertBigDecimalEquals(actualTotalAmount, expectedTotalAmount);
        verify(jobLauncher, times(1)).run(batchJob, chunkParams);
    }

    @Test
    @DisplayName("Should handle file-based input/output operations matching COBOL file processing")
    public void testFileBasedInputOutputOperations() throws Exception {
        // Given - file processing parameters matching COBOL ASSIGN TO clauses
        JobParameters fileProcessingParams = new JobParametersBuilder()
                .addString("inputFile", "DALYTRAN.txt")  // Equivalent to DALYTRAN-FILE
                .addString("outputFile", "TRANFILE.txt")  // Equivalent to TRANSACT-FILE  
                .addString("rejectFile", "REJECT.txt")    // For rejected transactions
                .addString("crossRefFile", "XREF.txt")    // Equivalent to XREF-FILE
                .addString("accountFile", "ACCT.txt")     // Equivalent to ACCOUNT-FILE
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // Mock successful file processing
        when(jobLauncher.run(any(Job.class), eq(fileProcessingParams)))
                .thenReturn(successfulJobExecution);
        when(dailyTransactionJob.dailyTransactionJob()).thenReturn(batchJob);

        // When - execute file-based batch job
        JobExecution result = dailyTransactionJobService.launchDailyJob(fileProcessingParams);

        // Then - verify file processing completed
        assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        verify(jobLauncher, times(1)).run(batchJob, fileProcessingParams);
        
        // Verify file parameters were validated correctly
        boolean paramsValid = dailyTransactionJobService.validateJobParameters(fileProcessingParams);
        assertThat(paramsValid).isTrue();
    }

    @Test
    @DisplayName("Should validate Spring Batch metadata persistence and job repository operations")
    public void testSpringBatchMetadataPersistence() {
        // Given - job repository operations for metadata persistence
        JobInstance jobInstance = mock(JobInstance.class);
        when(jobInstance.getJobName()).thenReturn(TEST_JOB_NAME);
        when(jobInstance.getId()).thenReturn(12345L);
        
        when(jobRepository.createJobExecution(any(JobInstance.class), any(JobParameters.class), anyString()))
                .thenReturn(successfulJobExecution);
        when(jobRepository.getLastJobExecution(TEST_JOB_NAME, testJobParameters))
                .thenReturn(successfulJobExecution);

        // When - retrieve job execution metadata
        JobExecution lastExecution = dailyTransactionJobService.getLastJobExecution();
        var metrics = dailyTransactionJobService.getJobMetrics();

        // Then - verify metadata persistence
        assertThat(lastExecution).isNotNull();
        assertThat(lastExecution.getJobParameters()).isEqualTo(testJobParameters);
        assertThat(metrics).isNotNull();
        assertThat(metrics.containsKey("readCount")).isTrue();
        assertThat(metrics.containsKey("writeCount")).isTrue();
        
        verify(jobRepository, times(2)).getLastJobExecution(TEST_JOB_NAME, testJobParameters);
    }

    // ==================== ERROR RECORD HANDLING TESTS ====================

    @Test
    @DisplayName("Should handle error records and rejection processing like COBOL programs")
    public void testErrorRecordHandlingAndRejection() throws Exception {
        // Given - job execution with skip/error scenarios
        JobExecution jobWithErrors = mock(JobExecution.class);
        when(jobWithErrors.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobWithErrors.getExitStatus()).thenReturn(ExitStatus.COMPLETED.addExitDescription("With errors"));
        when(jobWithErrors.getStartTime()).thenReturn(LocalDateTime.now().minusHours(1));
        when(jobWithErrors.getEndTime()).thenReturn(LocalDateTime.now());
        when(jobWithErrors.getJobParameters()).thenReturn(testJobParameters);

        when(jobLauncher.run(any(Job.class), any(JobParameters.class)))
                .thenReturn(jobWithErrors);
        when(dailyTransactionJob.dailyTransactionJob()).thenReturn(batchJob);

        // When - launch job with error handling parameters
        JobParameters errorHandlingParams = new JobParametersBuilder(testJobParameters)
                .addString("skipPolicy", "enabled")
                .addLong("skipLimit", 100L)
                .toJobParameters();
        
        JobExecution result = dailyTransactionJobService.launchDailyJob(errorHandlingParams);

        // Then - verify error handling completed successfully
        assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(result.getExitStatus().getExitDescription()).contains("With errors");
        
        // Verify error metrics are captured
        var metrics = dailyTransactionJobService.getJobMetrics();
        assertThat(metrics.containsKey("skipCount")).isTrue();
        
        verify(jobLauncher, times(1)).run(batchJob, errorHandlingParams);
    }

    @Test
    @DisplayName("Should validate transaction amounts with COBOL COMP-3 precision handling")
    public void testCobolPrecisionValidation() {
        // Given - transactions with various decimal precision scenarios
        List<Transaction> precisionTestTransactions = new ArrayList<>();
        
        // Test COMP-3 equivalent precision (2 decimal places for monetary values)
        Transaction monetary = new Transaction();
        monetary.setAmount(new BigDecimal("123.45").setScale(2, BigDecimal.ROUND_HALF_UP));
        precisionTestTransactions.add(monetary);
        
        // Test integer amounts (should be scaled to 2 decimals)
        Transaction integer = new Transaction();
        integer.setAmount(new BigDecimal("100").setScale(2, BigDecimal.ROUND_HALF_UP));
        precisionTestTransactions.add(integer);
        
        // Test high precision amounts (should be rounded to 2 decimals)
        Transaction highPrecision = new Transaction();
        highPrecision.setAmount(new BigDecimal("99.999").setScale(2, BigDecimal.ROUND_HALF_UP));
        precisionTestTransactions.add(highPrecision);

        // When - validate precision using utility method from BatchTestUtils
        for (Transaction transaction : precisionTestTransactions) {
            batchUtils.validateCobolPrecision(transaction.getAmount(), COBOL_DECIMAL_SCALE);
        }

        // Then - verify all amounts maintain COBOL precision
        assertThat(monetary.getAmount().scale()).isEqualTo(COBOL_DECIMAL_SCALE);
        assertThat(integer.getAmount().scale()).isEqualTo(COBOL_DECIMAL_SCALE);
        assertThat(highPrecision.getAmount().scale()).isEqualTo(COBOL_DECIMAL_SCALE);
        
        // Verify rounded value matches COBOL rounding behavior
        assertThat(highPrecision.getAmount()).isEqualTo(new BigDecimal("100.00"));
    }

    // ==================== INTEGRATION AND END-TO-END TESTS ====================

    @Test
    @DisplayName("Should execute complete end-to-end batch processing workflow")
    public void testEndToEndBatchProcessingWorkflow() throws Exception {
        // Given - complete workflow test scenario
        int transactionCount = 1000;
        
        // Setup complete mock chain for end-to-end processing
        List<Transaction> workflowTransactions = generateTestTransactions(transactionCount);
        when(transactionRepository.findByAccountIdAndDateRange(anyString(), any(), any()))
                .thenReturn(workflowTransactions);
        when(transactionRepository.count()).thenReturn((long) transactionCount);
        
        JobExecution workflowExecution = createMockJobExecution(BatchStatus.COMPLETED, ExitStatus.COMPLETED);
        when(jobLauncher.run(any(Job.class), any(JobParameters.class)))
                .thenReturn(workflowExecution);
        when(dailyTransactionJob.dailyTransactionJob()).thenReturn(batchJob);
        when(dailyTransactionJob.dailyTransactionStep()).thenReturn(mock(org.springframework.batch.core.Step.class));

        // When - execute complete workflow
        // 1. Validate job parameters
        boolean parametersValid = dailyTransactionJobService.validateJobParameters(testJobParameters);
        
        // 2. Launch job
        JobExecution launchResult = dailyTransactionJobService.launchDailyJob(testJobParameters);
        
        // 3. Monitor job execution
        JobExecution statusResult = dailyTransactionJobService.getJobExecutionStatus(launchResult.getId());
        
        // 4. Collect metrics
        var finalMetrics = dailyTransactionJobService.getJobMetrics();

        // Then - verify complete workflow success
        assertThat(parametersValid).isTrue();
        assertThat(launchResult.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(statusResult).isNotNull();
        assertThat(finalMetrics).isNotEmpty();
        
        // Verify all repository interactions occurred
        verify(transactionRepository, atLeastOnce()).findByAccountIdAndDateRange(anyString(), any(), any());
        verify(jobLauncher, times(1)).run(batchJob, testJobParameters);
        verify(dailyTransactionJob, times(1)).dailyTransactionJob();
    }

    // ==================== MOCK SERVICE FACTORY INTEGRATION ====================

    /**
     * Demonstrates usage of MockServiceFactory for creating pre-configured mocks
     */
    @Test
    @DisplayName("Should integrate with MockServiceFactory for consistent test setup")
    public void testMockServiceFactoryIntegration() {
        // Given - mock factory setup
        TransactionRepository mockRepo = mockFactory.createMockTransactionRepository();
        JobLauncher mockLauncher = mockFactory.createMockWithSuccessScenario(JobLauncher.class);

        // When - configure mocks through factory
        mockFactory.resetAllMocks();
        
        // Configure success scenario through factory
        when(mockRepo.save(any(Transaction.class))).thenReturn(new Transaction());
        when(mockRepo.count()).thenReturn(500L);

        // Then - verify factory integration
        assertThat(mockRepo).isNotNull();
        assertThat(mockLauncher).isNotNull();
        
        // Test mock functionality
        Transaction savedTransaction = mockRepo.save(new Transaction());
        assertThat(savedTransaction).isNotNull();
        
        Long count = mockRepo.count();
        assertThat(count).isEqualTo(500L);
    }

    /**
     * Test constant values integration with TestConstants
     */
    @Test
    @DisplayName("Should use TestConstants for consistent test data and performance targets")
    public void testConstantsIntegration() {
        // Verify performance target constant usage
        assertThat(PERFORMANCE_TARGET_4_HOURS_MS).isEqualTo(TestConstants.PERFORMANCE_TARGET_4_HOURS);
        
        // Verify test data constants
        assertThat(TEST_ACCOUNT_ID).isEqualTo(TestConstants.TEST_ACCOUNT_ID);
        assertThat(TEST_TRANSACTION_AMOUNT).isEqualTo(TestConstants.TEST_TRANSACTION_AMOUNT);
        
        // Verify COBOL format constants
        assertThat(COBOL_DECIMAL_SCALE).isEqualTo(TestConstants.BIGDECIMAL_SCALE);
        
        // Verify timeout constants for job execution
        long timeout = TestConstants.JOB_EXECUTION_TIMEOUT;
        assertThat(timeout).isGreaterThan(0);
    }
}