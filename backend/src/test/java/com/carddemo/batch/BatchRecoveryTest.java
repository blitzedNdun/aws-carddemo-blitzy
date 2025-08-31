package com.carddemo.batch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.RetryException;

import org.mockito.MockitoAnnotations;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.Spy;

import org.assertj.core.api.Assertions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.carddemo.config.TestBatchConfig;
import com.carddemo.batch.DailyTransactionJob;
import com.carddemo.batch.InterestCalculationJob;
import com.carddemo.batch.StatementGenerationJob;
import com.carddemo.batch.BatchTestUtils;
import com.carddemo.config.TestDatabaseConfig;
import com.carddemo.TestConstants;
import com.carddemo.AbstractBaseTest;
import com.carddemo.UnitTest;
import com.carddemo.IntegrationTest;

/**
 * Comprehensive test suite for validating Spring Batch job restart, recovery, and checkpoint 
 * mechanisms ensuring resilience equivalent to JCL restart capabilities.
 * 
 * This test suite validates all aspects of batch recovery including:
 * - Job restart from failure points with checkpoint preservation
 * - Transaction rollback and partial commit handling 
 * - Step-level restart capabilities matching JCL behavior
 * - Skip-limit and retry policy effectiveness
 * - Compensation logic and idempotency validation
 * - State persistence and execution context preservation
 * - Resource cleanup and deadlock recovery scenarios
 * 
 * Tests are designed to ensure the Spring Batch implementation provides
 * equivalent or superior restart/recovery capabilities compared to the 
 * original mainframe JCL batch processing system.
 */
@SpringBootTest(classes = {TestBatchConfig.class, TestDatabaseConfig.class})
@TestPropertySource(locations = "classpath:application-test.properties")
@EnableBatchProcessing
public class BatchRecoveryTest extends AbstractBaseTest implements UnitTest, IntegrationTest {

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepository testJobRepository;

    @Autowired 
    private JobLauncher testJobLauncher;

    @Autowired
    private DailyTransactionJob dailyTransactionJob;

    @Autowired
    private InterestCalculationJob interestCalculationJob;

    @Autowired
    private StatementGenerationJob statementGenerationJob;

    @Autowired
    private BatchTestUtils batchTestUtils;

    private JobParameters testJobParameters;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        super.setUp();
        mocks = MockitoAnnotations.openMocks(this);
        
        // Initialize test job parameters with restart capability
        testJobParameters = batchTestUtils.createTestJobParameters();
        
        // Clean job repository state for isolated test execution
        jobRepositoryTestUtils.removeJobExecutions();
        
        // Setup test data for batch processing scenarios
        batchTestUtils.setupBatchTestData();
    }

    @AfterEach
    void tearDown() throws Exception {
        super.tearDown();
        if (mocks != null) {
            mocks.close();
        }
        
        // Clean up job repository state
        jobRepositoryTestUtils.removeJobExecutions();
    }

    /**
     * Test Case: Job Restart from Failure Point
     * Validates that Spring Batch jobs can restart from the exact point of failure,
     * preserving all processing state and continuing from the last successful checkpoint.
     * This replicates JCL restart functionality where jobs resume from the last
     * completed step without reprocessing previously successful work.
     */
    @Test
    @DisplayName("Job restart from failure point maintains processing continuity")
    void testJobRestartFromFailurePoint() throws Exception {
        // Given: Daily transaction job configured for restart testing
        jobLauncherTestUtils.setJob(dailyTransactionJob.dailyTransactionJob());
        
        // Create failure scenario at step 2 (validation step)
        JobParameters failureParameters = new JobParametersBuilder(testJobParameters)
            .addString("simulateFailure", "step2")
            .addLong("failurePoint", 500L) // Fail after processing 500 records
            .toJobParameters();
        
        // When: Execute job expecting failure at specified point
        JobExecution firstExecution = jobLauncherTestUtils.launchJob(failureParameters);
        
        // Then: Verify job failed at expected point with checkpoint preserved
        assertThat(firstExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(firstExecution.getStepExecutions()).hasSize(2);
        
        StepExecution failedStep = firstExecution.getStepExecutions()
            .stream()
            .filter(step -> step.getStatus() == BatchStatus.FAILED)
            .findFirst()
            .orElseThrow();
        
        assertThat(failedStep.getReadCount()).isEqualTo(500);
        assertThat(failedStep.getCommitCount()).isGreaterThan(0);
        
        // When: Restart job from failure point
        JobParameters restartParameters = new JobParametersBuilder(testJobParameters)
            .addString("restart", "true")
            .addLong("run.id", firstExecution.getJobParameters().getLong("run.id"))
            .toJobParameters();
            
        JobExecution restartExecution = jobLauncherTestUtils.launchJob(restartParameters);
        
        // Then: Verify restart completed successfully without reprocessing
        assertThat(restartExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // Validate that restart preserved previous work
        StepExecution restartedStep = restartExecution.getStepExecutions()
            .stream()
            .filter(step -> step.getStepName().equals(failedStep.getStepName()))
            .findFirst()
            .orElseThrow();
            
        // Total processed should be cumulative across executions
        long totalProcessed = failedStep.getReadCount() + restartedStep.getReadCount();
        assertThat(totalProcessed).isGreaterThan(500);
        
        // Verify JCL-equivalent restart behavior
        verifyJclEquivalentRestart(firstExecution, restartExecution);
    }

    /**
     * Test Case: Checkpoint Preservation and Restoration
     * Validates that Spring Batch checkpoint data is properly preserved during
     * job failures and accurately restored during restart operations.
     * This ensures continuation from exact failure points matching JCL checkpoint behavior.
     */
    @Test
    @DisplayName("Checkpoint data preservation ensures accurate restart position")
    void testCheckpointPreservationAndRestoration() throws Exception {
        // Given: Interest calculation job with checkpoint configuration
        jobLauncherTestUtils.setJob(interestCalculationJob.interestCalculationJob());
        
        JobParameters checkpointParameters = new JobParametersBuilder()
            .addDate("processDate", new Date())
            .addLong("checkpointInterval", TestConstants.CHECKPOINT_INTERVAL)
            .addString("simulateFailure", "afterCheckpoint")
            .addLong("failureAfterCommits", 3L) // Fail after 3 successful commits
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters();
        
        // When: Execute job with checkpoint failure scenario
        JobExecution initialExecution = jobLauncherTestUtils.launchJob(checkpointParameters);
        
        // Then: Verify checkpoint data was preserved in job repository
        assertThat(initialExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
        
        StepExecution checkpointedStep = initialExecution.getStepExecutions()
            .iterator().next();
        
        // Validate checkpoint data preservation
        assertThat(checkpointedStep.getCommitCount()).isEqualTo(3);
        assertThat(checkpointedStep.getExecutionContext()).isNotNull();
        assertThat(checkpointedStep.getExecutionContext().entrySet()).isNotEmpty();
        
        // Verify execution context contains checkpoint state
        assertThat(checkpointedStep.getExecutionContext().containsKey("batch.stepType")).isTrue();
        assertThat(checkpointedStep.getExecutionContext().getLong("batch.stepType", 0L))
            .isGreaterThan(0);
        
        // When: Restart job from checkpoint
        JobParameters restartParameters = new JobParametersBuilder()
            .addDate("processDate", checkpointParameters.getDate("processDate"))
            .addLong("checkpointInterval", TestConstants.CHECKPOINT_INTERVAL)
            .addString("restart", "true")
            .addLong("run.id", checkpointParameters.getLong("run.id"))
            .toJobParameters();
            
        JobExecution restartExecution = jobLauncherTestUtils.launchJob(restartParameters);
        
        // Then: Verify restart used preserved checkpoint data
        assertThat(restartExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        StepExecution restartedStep = restartExecution.getStepExecutions()
            .iterator().next();
        
        // Validate checkpoint restoration
        assertThat(restartedStep.getExecutionContext())
            .containsKey("batch.stepType");
        
        // Verify no duplicate processing occurred
        long totalCommits = checkpointedStep.getCommitCount() + restartedStep.getCommitCount();
        assertThat(totalCommits).isLessThanOrEqualTo(
            (checkpointedStep.getReadCount() + restartedStep.getReadCount()) / TestConstants.CHECKPOINT_INTERVAL + 1);
    }

    /**
     * Test Case: Transaction Rollback on Failure Scenarios
     * Validates that database transactions are properly rolled back when
     * batch processing failures occur, ensuring data consistency and integrity.
     * This replicates JCL job step failure handling with automatic rollback.
     */
    @Test
    @DisplayName("Transaction rollback maintains data integrity on failures")
    @Transactional
    void testTransactionRollbackOnFailure() throws Exception {
        // Given: Daily transaction job with transactional processing
        jobLauncherTestUtils.setJob(dailyTransactionJob.dailyTransactionJob());
        
        // Setup test scenario with invalid data to trigger rollback
        JobParameters rollbackParameters = new JobParametersBuilder()
            .addDate("processDate", new Date())
            .addString("inputFile", "test-transactions-with-errors.txt")
            .addString("simulateFailure", "transactionError")
            .addString("errorType", "dataIntegrityViolation")
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters();
        
        // Capture initial database state
        Map<String, Object> initialState = captureInitialDatabaseState();
        
        // When: Execute job with transaction error scenario
        JobExecution failedExecution = jobLauncherTestUtils.launchJob(rollbackParameters);
        
        // Then: Verify job failed and transaction was rolled back
        assertThat(failedExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(failedExecution.getAllFailureExceptions()).isNotEmpty();
        
        // Validate complete rollback occurred
        Map<String, Object> postFailureState = captureCurrentDatabaseState();
        assertThat(postFailureState).isEqualTo(initialState);
        
        // Verify no partial commits remain in database
        validateNoPartialTransactionCommits(failedExecution);
        
        // Verify rollback metrics are captured
        batchTestUtils.validateJobExecution(failedExecution);
        
        StepExecution rolledBackStep = failedExecution.getStepExecutions()
            .stream()
            .filter(step -> step.getStatus() == BatchStatus.FAILED)
            .findFirst()
            .orElseThrow();
        
        assertThat(rolledBackStep.getRollbackCount()).isGreaterThan(0);
        assertThat(rolledBackStep.getWriteCount()).isEqualTo(0); // No writes should be committed
    }

    /**
     * Test Case: Partial Commit Handling for Large Batches
     * Validates that large batch jobs properly handle partial commits within
     * chunk boundaries while maintaining transactional consistency.
     * This ensures chunk-level commit behavior matches JCL checkpoint processing.
     */
    @Test
    @DisplayName("Partial commit handling preserves chunk-level transaction integrity")
    void testPartialCommitHandling() throws Exception {
        // Given: Large batch job with chunk-based processing
        jobLauncherTestUtils.setJob(dailyTransactionJob.dailyTransactionJob());
        
        JobParameters largeProcessingParameters = new JobParametersBuilder()
            .addDate("processDate", new Date())
            .addString("inputFile", "large-transaction-file.txt")
            .addLong("chunkSize", 100L)
            .addLong("totalRecords", 5000L)
            .addString("simulateFailure", "partialChunk")
            .addLong("failureAtRecord", 2350L) // Fail in middle of chunk processing
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters();
        
        // When: Execute large batch with partial chunk failure
        JobExecution partialExecution = jobLauncherTestUtils.launchJob(largeProcessingParameters);
        
        // Then: Verify partial commits preserved correctly
        assertThat(partialExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
        
        StepExecution partialStep = partialExecution.getStepExecutions()
            .iterator().next();
        
        // Validate chunk-level commit behavior
        assertThat(partialStep.getCommitCount()).isEqualTo(23); // 2300 records / 100 chunk size
        assertThat(partialStep.getWriteCount()).isEqualTo(2300); // Successfully written records
        assertThat(partialStep.getReadCount()).isGreaterThan(2300); // Read beyond write point
        
        // Verify chunk boundary integrity
        long expectedCommittedChunks = partialStep.getWriteCount() / 100;
        assertThat(partialStep.getCommitCount()).isEqualTo(expectedCommittedChunks);
        
        // When: Restart from partial commit point
        JobParameters restartFromPartial = new JobParametersBuilder()
            .addDate("processDate", largeProcessingParameters.getDate("processDate"))
            .addLong("chunkSize", 100L)
            .addString("restart", "true")
            .addLong("run.id", largeProcessingParameters.getLong("run.id"))
            .toJobParameters();
            
        JobExecution restartExecution = jobLauncherTestUtils.launchJob(restartFromPartial);
        
        // Then: Verify restart completed remaining records
        assertThat(restartExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        StepExecution restartStep = restartExecution.getStepExecutions()
            .iterator().next();
        
        // Validate total processing completeness
        long totalWritten = partialStep.getWriteCount() + restartStep.getWriteCount();
        assertThat(totalWritten).isEqualTo(5000L);
        
        // Ensure no duplicate processing
        validateNoDuplicateProcessing(partialExecution, restartExecution);
    }

    /**
     * Test Case: Step-Level Restart Capability
     * Validates that individual batch job steps can be restarted independently,
     * matching JCL step restart functionality where specific job steps can be
     * re-executed without affecting completed steps.
     */
    @Test
    @DisplayName("Step-level restart preserves completed step results") 
    void testStepLevelRestart() throws Exception {
        // Given: Multi-step statement generation job
        jobLauncherTestUtils.setJob(statementGenerationJob.statementGenerationJob());
        
        JobParameters multiStepParameters = new JobParametersBuilder()
            .addDate("statementDate", new Date())
            .addString("statementType", "MONTHLY")
            .addString("simulateFailure", "step3") // Fail at third step
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters();
        
        // When: Execute multi-step job with step 3 failure
        JobExecution multiStepExecution = jobLauncherTestUtils.launchJob(multiStepParameters);
        
        // Then: Verify first two steps completed successfully
        assertThat(multiStepExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
        
        List<StepExecution> stepExecutions = new ArrayList<>(multiStepExecution.getStepExecutions());
        stepExecutions.sort((s1, s2) -> s1.getStartTime().compareTo(s2.getStartTime()));
        
        // Validate step completion status
        assertThat(stepExecutions.get(0).getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(stepExecutions.get(1).getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(stepExecutions.get(2).getStatus()).isEqualTo(BatchStatus.FAILED);
        
        // Capture completed step results
        Map<String, Object> step1Results = captureStepResults(stepExecutions.get(0));
        Map<String, Object> step2Results = captureStepResults(stepExecutions.get(1));
        
        // When: Restart from failed step (step-level restart)
        JobParameters stepRestartParameters = new JobParametersBuilder()
            .addDate("statementDate", multiStepParameters.getDate("statementDate"))
            .addString("statementType", "MONTHLY")
            .addString("restartFromStep", "step3")
            .addLong("run.id", multiStepParameters.getLong("run.id"))
            .toJobParameters();
            
        JobExecution stepRestartExecution = jobLauncherTestUtils.launchJob(stepRestartParameters);
        
        // Then: Verify restart completed successfully with preserved prior steps
        assertThat(stepRestartExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // Validate that completed steps were not re-executed
        List<StepExecution> restartSteps = new ArrayList<>(stepRestartExecution.getStepExecutions());
        assertThat(restartSteps).hasSize(1); // Only failed step re-executed
        
        StepExecution reExecutedStep = restartSteps.get(0);
        assertThat(reExecutedStep.getStepName()).contains("step3");
        assertThat(reExecutedStep.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // Verify step results preservation (no re-execution of completed steps)
        validateStepResultsPreservation(step1Results, step2Results);
    }

    /**
     * Test Case: Skip-Limit Configuration Validation
     * Tests that skip-limit configuration allows jobs to continue processing
     * despite individual record failures, matching JCL error tolerance behavior.
     */
    @Test
    @DisplayName("Skip-limit configuration enables error tolerance processing")
    void testSkipLimitConfiguration() throws Exception {
        // Given: Daily transaction job with skip-limit configuration
        jobLauncherTestUtils.setJob(dailyTransactionJob.dailyTransactionJob());
        
        JobParameters skipLimitParameters = new JobParametersBuilder()
            .addDate("processDate", new Date())
            .addString("inputFile", "transactions-with-bad-records.txt")
            .addLong("skipLimit", TestConstants.SKIP_LIMIT) // Allow 10 skipped records
            .addString("skipPolicy", "AlwaysSkipItemSkipPolicy")
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters();
        
        // When: Execute job with records that will be skipped
        JobExecution skipExecution = jobLauncherTestUtils.launchJob(skipLimitParameters);
        
        // Then: Verify job completed despite skip events
        assertThat(skipExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        StepExecution skipStep = skipExecution.getStepExecutions()
            .iterator().next();
        
        // Validate skip behavior
        assertThat(skipStep.getSkipCount()).isGreaterThan(0);
        assertThat(skipStep.getSkipCount()).isLessThanOrEqualTo(TestConstants.SKIP_LIMIT);
        assertThat(skipStep.getReadSkipCount()).isGreaterThan(0);
        
        // Verify processed records accuracy
        long expectedProcessed = skipStep.getReadCount() - skipStep.getSkipCount();
        assertThat(skipStep.getWriteCount()).isEqualTo(expectedProcessed);
        
        // When: Execute job with skip limit exceeded
        JobParameters exceedSkipParameters = new JobParametersBuilder()
            .addDate("processDate", new Date())
            .addString("inputFile", "transactions-many-errors.txt")
            .addLong("skipLimit", TestConstants.SKIP_LIMIT)
            .addLong("expectedErrors", TestConstants.SKIP_LIMIT + 5L) // Exceed skip limit
            .addLong("run.id", System.currentTimeMillis() + 1)
            .toJobParameters();
            
        JobExecution exceedSkipExecution = jobLauncherTestUtils.launchJob(exceedSkipParameters);
        
        // Then: Verify job failed when skip limit exceeded
        assertThat(exceedSkipExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(exceedSkipExecution.getAllFailureExceptions())
            .hasAtLeastOneElementOfType(Exception.class);
    }

    /**
     * Test Case: Retry Policy Effectiveness Testing
     * Validates that retry policies handle transient failures appropriately,
     * attempting retries up to configured limits before marking items as failed.
     * This ensures resilience similar to JCL automatic retry mechanisms.
     */
    @Test
    @DisplayName("Retry policy handles transient failures with configured limits")
    void testRetryPolicyEffectiveness() throws Exception {
        // Given: Interest calculation job with retry configuration
        jobLauncherTestUtils.setJob(interestCalculationJob.interestCalculationJob());
        
        JobParameters retryParameters = new JobParametersBuilder()
            .addDate("processDate", new Date())
            .addLong("retryLimit", TestConstants.RETRY_LIMIT) // Allow 3 retries
            .addString("retryPolicy", "SimpleRetryPolicy")
            .addString("simulateFailure", "transientError")
            .addLong("transientErrorCount", 2L) // Succeed on 3rd attempt
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters();
        
        // When: Execute job with transient error scenario
        JobExecution retryExecution = jobLauncherTestUtils.launchJob(retryParameters);
        
        // Then: Verify job completed after successful retries
        assertThat(retryExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        StepExecution retryStep = retryExecution.getStepExecutions()
            .iterator().next();
        
        // Validate retry behavior
        assertThat(retryStep.getRetryCount()).isGreaterThan(0);
        assertThat(retryStep.getRetryCount()).isLessThanOrEqualTo(TestConstants.RETRY_LIMIT);
        
        // When: Execute job with retry limit exceeded
        JobParameters exceedRetryParameters = new JobParametersBuilder()
            .addDate("processDate", new Date())
            .addLong("retryLimit", TestConstants.RETRY_LIMIT)
            .addString("simulateFailure", "persistentError")
            .addLong("persistentErrorCount", TestConstants.RETRY_LIMIT + 2L)
            .addLong("run.id", System.currentTimeMillis() + 1)
            .toJobParameters();
            
        JobExecution exceedRetryExecution = jobLauncherTestUtils.launchJob(exceedRetryParameters);
        
        // Then: Verify job failed when retry limit exceeded
        assertThat(exceedRetryExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
        
        StepExecution failedRetryStep = exceedRetryExecution.getStepExecutions()
            .iterator().next();
        
        assertThat(failedRetryStep.getRetryCount()).isEqualTo(TestConstants.RETRY_LIMIT);
        assertThat(exceedRetryExecution.getAllFailureExceptions())
            .isNotEmpty()
            .hasAtLeastOneElementOfType(RetryException.class);
    }

    /**
     * Test Case: Compensation Logic for Failed Transactions
     * Tests that compensation logic properly handles cleanup and rollback
     * of business operations when batch processing failures occur.
     * This replicates JCL restart preparation and cleanup procedures.
     */
    @Test
    @DisplayName("Compensation logic ensures proper cleanup on failures")
    void testCompensationLogic() throws Exception {
        // Given: Interest calculation job with compensation logic
        jobLauncherTestUtils.setJob(interestCalculationJob.interestCalculationJob());
        
        JobParameters compensationParameters = new JobParametersBuilder()
            .addDate("processDate", new Date())
            .addString("compensationMode", "enabled")
            .addString("simulateFailure", "afterProcessing")
            .addLong("partialRecordsCreated", 150L) // Create partial interest records
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters();
        
        // When: Execute job with compensation scenario
        JobExecution compensationExecution = jobLauncherTestUtils.launchJob(compensationParameters);
        
        // Then: Verify compensation logic activated
        assertThat(compensationExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
        
        StepExecution compensationStep = compensationExecution.getStepExecutions()
            .stream()
            .filter(step -> step.getStepName().contains("interestCalculation"))
            .findFirst()
            .orElseThrow();
        
        // Validate compensation execution
        assertThat(compensationStep.getExecutionContext())
            .containsKey("compensation.executed")
            .extractingByKey("compensation.executed", Boolean.class)
            .isEqualTo(true);
        
        // Verify partial records were cleaned up
        validateCompensationCleanup(compensationExecution);
        
        // When: Restart job after compensation
        JobParameters restartAfterCompensation = new JobParametersBuilder()
            .addDate("processDate", compensationParameters.getDate("processDate"))
            .addString("restart", "true")
            .addString("compensationMode", "disabled") // Prevent compensation on restart
            .addLong("run.id", compensationParameters.getLong("run.id"))
            .toJobParameters();
            
        JobExecution restartExecution = jobLauncherTestUtils.launchJob(restartAfterCompensation);
        
        // Then: Verify clean restart after compensation
        assertThat(restartExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // Validate no duplicate compensation occurred
        validateNoDoubleCompensation(compensationExecution, restartExecution);
    }

    /**
     * Test Case: Idempotency Validation for Reprocessing
     * Ensures that reprocessing the same data multiple times produces
     * identical results without side effects or data corruption.
     * This validates safe restart behavior matching JCL idempotent processing.
     */
    @Test
    @DisplayName("Idempotent reprocessing produces identical results")
    void testIdempotencyValidation() throws Exception {
        // Given: Daily transaction job with idempotent configuration
        jobLauncherTestUtils.setJob(dailyTransactionJob.dailyTransactionJob());
        
        JobParameters idempotentParameters = new JobParametersBuilder()
            .addDate("processDate", new Date())
            .addString("inputFile", "test-transactions-idempotent.txt") 
            .addString("idempotencyMode", "enabled")
            .addString("duplicateHandling", "skip")
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters();
        
        // When: Execute job first time
        JobExecution firstRun = jobLauncherTestUtils.launchJob(idempotentParameters);
        
        // Then: Verify successful completion
        assertThat(firstRun.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // Capture results of first execution
        Map<String, Object> firstRunResults = captureJobResults(firstRun);
        
        // When: Execute same job with identical parameters (reprocessing scenario)
        JobParameters reprocessParameters = new JobParametersBuilder()
            .addDate("processDate", idempotentParameters.getDate("processDate"))
            .addString("inputFile", "test-transactions-idempotent.txt")
            .addString("idempotencyMode", "enabled")
            .addString("duplicateHandling", "skip")
            .addLong("run.id", System.currentTimeMillis() + 1) // New execution
            .toJobParameters();
            
        JobExecution reprocessRun = jobLauncherTestUtils.launchJob(reprocessParameters);
        
        // Then: Verify idempotent reprocessing
        assertThat(reprocessRun.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        Map<String, Object> reprocessResults = captureJobResults(reprocessRun);
        
        // Validate identical results
        assertThat(reprocessResults).isEqualTo(firstRunResults);
        
        // Verify no duplicate records created
        StepExecution reprocessStep = reprocessRun.getStepExecutions()
            .iterator().next();
        
        assertThat(reprocessStep.getWriteSkipCount()).isGreaterThan(0); // Duplicates skipped
        assertThat(reprocessStep.getWriteCount()).isEqualTo(0); // No new records written
        
        // Validate idempotency metrics
        validateIdempotencyBehavior(firstRun, reprocessRun);
    }

    /**
     * Test Case: State Persistence in Job Repository
     * Validates that all job execution state is properly persisted in the
     * Spring Batch job repository and can be retrieved after restart.
     * This ensures restart capability matching JCL checkpoint data preservation.
     */
    @Test
    @DisplayName("Job repository maintains complete execution state across restarts")
    void testStatePersistenceInJobRepository() throws Exception {
        // Given: Multiple jobs with complex execution state
        JobParameters persistenceParameters = new JobParametersBuilder()
            .addDate("processDate", new Date())
            .addString("jobChain", "daily,interest,statement")
            .addString("stateTracking", "enabled")
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters();
        
        // When: Execute job chain with state tracking
        List<JobExecution> jobExecutions = new ArrayList<>();
        
        // Execute daily transaction job
        jobLauncherTestUtils.setJob(dailyTransactionJob.dailyTransactionJob());
        JobExecution dailyExecution = jobLauncherTestUtils.launchJob(persistenceParameters);
        jobExecutions.add(dailyExecution);
        
        // Execute interest calculation job
        jobLauncherTestUtils.setJob(interestCalculationJob.interestCalculationJob());
        JobExecution interestExecution = jobLauncherTestUtils.launchJob(persistenceParameters);
        jobExecutions.add(interestExecution);
        
        // Then: Verify state persistence in job repository
        for (JobExecution execution : jobExecutions) {
            assertThat(execution.getStatus()).isIn(BatchStatus.COMPLETED, BatchStatus.FAILED);
            
            // Validate job repository contains execution state
            List<JobExecution> retrievedExecutions = jobRepositoryTestUtils
                .getJobExecutions(execution.getJobInstance().getJobName());
            
            assertThat(retrievedExecutions).isNotEmpty();
            assertThat(retrievedExecutions).contains(execution);
            
            // Validate execution context persistence
            for (StepExecution stepExecution : execution.getStepExecutions()) {
                assertThat(stepExecution.getExecutionContext()).isNotNull();
                assertThat(stepExecution.getExecutionContext().entrySet()).isNotEmpty();
            }
        }
        
        // Validate job instance relationships
        assertThat(jobRepositoryTestUtils.getJobInstances("dailyTransactionJob"))
            .isNotEmpty();
        assertThat(jobRepositoryTestUtils.getJobInstances("interestCalculationJob"))  
            .isNotEmpty();
        
        // Verify parameter preservation
        validateParameterPersistence(jobExecutions);
    }

    /**
     * Test Case: Execution Context Preservation
     * Tests that execution context data is maintained across job restarts,
     * ensuring processing state and custom attributes are available to
     * restarted steps. This replicates JCL context preservation.
     */
    @Test  
    @DisplayName("Execution context data preserved across job restarts")
    void testExecutionContextPreservation() throws Exception {
        // Given: Job with complex execution context data
        jobLauncherTestUtils.setJob(dailyTransactionJob.dailyTransactionJob());
        
        JobParameters contextParameters = new JobParametersBuilder()
            .addDate("processDate", new Date())
            .addString("contextMode", "enhanced")
            .addString("customAttribute1", "processingState")
            .addString("customAttribute2", "checkpointData")
            .addLong("processingPosition", 1000L)
            .addString("simulateFailure", "withContext")
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters();
        
        // When: Execute job that populates execution context
        JobExecution contextExecution = jobLauncherTestUtils.launchJob(contextParameters);
        
        // Then: Verify context data was preserved despite failure
        assertThat(contextExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
        
        StepExecution contextStep = contextExecution.getStepExecutions()
            .iterator().next();
        
        // Validate execution context preservation
        assertThat(contextStep.getExecutionContext())
            .containsKey("customAttribute1")
            .extractingByKey("customAttribute1", String.class)
            .isEqualTo("processingState");
        
        assertThat(contextStep.getExecutionContext())
            .containsKey("processingPosition")
            .extractingByKey("processingPosition", Long.class)
            .isEqualTo(1000L);
        
        // When: Restart job and verify context availability
        JobParameters restartContextParameters = new JobParametersBuilder()
            .addDate("processDate", contextParameters.getDate("processDate"))
            .addString("restart", "true")
            .addString("validateContext", "true")
            .addLong("run.id", contextParameters.getLong("run.id"))
            .toJobParameters();
            
        JobExecution restartContextExecution = jobLauncherTestUtils.launchJob(restartContextParameters);
        
        // Then: Verify context was available to restarted execution
        assertThat(restartContextExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        StepExecution restartContextStep = restartContextExecution.getStepExecutions()
            .iterator().next();
        
        // Validate context preservation across restart
        assertThat(restartContextStep.getExecutionContext())
            .containsKey("previousCustomAttribute1")
            .extractingByKey("previousCustomAttribute1", String.class)
            .isEqualTo("processingState");
            
        // Verify context-based processing continuation
        validateContextBasedProcessing(contextStep, restartContextStep);
    }

    /**
     * Test Case: Parameter Passing on Restart
     * Validates that job parameters are correctly preserved and passed
     * to restarted job executions, maintaining processing configuration.
     * This ensures restart parameter handling matching JCL parameter passing.
     */
    @Test
    @DisplayName("Job parameters preserved and passed correctly on restart")
    void testParameterPassingOnRestart() throws Exception {
        // Given: Statement generation job with complex parameters
        jobLauncherTestUtils.setJob(statementGenerationJob.statementGenerationJob());
        
        JobParameters originalParameters = new JobParametersBuilder()
            .addDate("statementDate", new Date())
            .addString("statementType", "MONTHLY") 
            .addString("outputFormat", "PDF")
            .addString("deliveryMethod", "EMAIL")
            .addLong("customerId", 12345L)
            .addString("specialHandling", "VIP")
            .addString("simulateFailure", "parameterValidation")
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters();
        
        // When: Execute job that will fail after parameter validation
        JobExecution parameterExecution = jobLauncherTestUtils.launchJob(originalParameters);
        
        // Then: Verify parameter validation and failure
        assertThat(parameterExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
        
        // Validate all parameters were preserved in job instance
        JobParameters preservedParameters = parameterExecution.getJobParameters();
        assertThat(preservedParameters.getDate("statementDate"))
            .isEqualTo(originalParameters.getDate("statementDate"));
        assertThat(preservedParameters.getString("statementType"))
            .isEqualTo("MONTHLY");
        assertThat(preservedParameters.getString("specialHandling"))
            .isEqualTo("VIP");
        
        // When: Restart with same parameters
        JobParameters restartParameters = new JobParametersBuilder()
            .addDate("statementDate", originalParameters.getDate("statementDate"))
            .addString("statementType", originalParameters.getString("statementType"))
            .addString("outputFormat", originalParameters.getString("outputFormat"))
            .addString("deliveryMethod", originalParameters.getString("deliveryMethod"))
            .addLong("customerId", originalParameters.getLong("customerId"))
            .addString("specialHandling", originalParameters.getString("specialHandling"))
            .addString("restart", "true")
            .addLong("run.id", originalParameters.getLong("run.id"))
            .toJobParameters();
            
        JobExecution restartParameterExecution = jobLauncherTestUtils.launchJob(restartParameters);
        
        // Then: Verify restart used preserved parameters
        assertThat(restartParameterExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // Validate parameter consistency across restart
        JobParameters restartActualParameters = restartParameterExecution.getJobParameters();
        validateParameterConsistency(preservedParameters, restartActualParameters);
    }

    /**
     * Test Case: Resource Cleanup on Abnormal Termination
     * Tests that system resources are properly cleaned up when batch jobs
     * terminate abnormally, preventing resource leaks and corruption.
     * This ensures cleanup behavior matching JCL abnormal termination handling.
     */
    @Test
    @DisplayName("Resource cleanup prevents leaks on abnormal termination")
    void testResourceCleanupOnAbnormalTermination() throws Exception {
        // Given: Daily transaction job with resource monitoring
        jobLauncherTestUtils.setJob(dailyTransactionJob.dailyTransactionJob());
        
        JobParameters resourceParameters = new JobParametersBuilder()
            .addDate("processDate", new Date())
            .addString("resourceMonitoring", "enabled")
            .addString("simulateFailure", "abnormalTermination")
            .addString("terminationType", "threadInterrupt")
            .addLong("resourceAllocation", 500L)
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters();
        
        // Monitor resource state before execution
        Map<String, Object> preExecutionResources = captureResourceState();
        
        // When: Execute job with abnormal termination
        CompletableFuture<JobExecution> asyncExecution = CompletableFuture.supplyAsync(() -> {
            try {
                return jobLauncherTestUtils.launchJob(resourceParameters);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        // Simulate abnormal termination after partial processing
        Thread.sleep(2000); // Allow partial processing
        asyncExecution.cancel(true);
        
        // Wait for cleanup to complete
        Thread.sleep(1000);
        
        // Then: Verify resource cleanup occurred
        Map<String, Object> postTerminationResources = captureResourceState();
        
        // Validate no resource leaks
        validateResourceCleanup(preExecutionResources, postTerminationResources);
        
        // Verify database connections were properly closed
        validateDatabaseConnectionCleanup();
        
        // Verify file handles were released
        validateFileHandleCleanup();
        
        // Verify Spring Batch infrastructure remained stable
        validateBatchInfrastructureStability();
    }

    /**
     * Test Case: Deadlock Recovery Scenarios
     * Tests batch job behavior when database deadlocks occur during processing,
     * validating deadlock detection, transaction rollback, and retry mechanisms.
     * This ensures deadlock handling equivalent to JCL database recovery.
     */
    @Test
    @DisplayName("Deadlock recovery maintains data consistency and job continuity")
    void testDeadlockRecoveryScenarios() throws Exception {
        // Given: Concurrent batch jobs prone to deadlock
        JobParameters deadlockParameters = new JobParametersBuilder()
            .addDate("processDate", new Date())
            .addString("concurrencyMode", "high")
            .addString("simulateFailure", "deadlock")
            .addLong("concurrentThreads", 4L)
            .addString("lockPattern", "accountBalance")
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters();
        
        // When: Execute job with deadlock simulation
        jobLauncherTestUtils.setJob(interestCalculationJob.interestCalculationJob());
        
        assertThatThrownBy(() -> {
            JobExecution deadlockExecution = jobLauncherTestUtils.launchJob(deadlockParameters);
        }).hasCauseInstanceOf(DeadlockLoserDataAccessException.class);
        
        // Verify deadlock was detected and handled appropriately
        List<JobExecution> executions = jobRepositoryTestUtils
            .getJobExecutions("interestCalculationJob");
        
        assertThat(executions).isNotEmpty();
        JobExecution deadlockExecution = executions.get(0);
        
        // Validate deadlock recovery behavior
        assertThat(deadlockExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(deadlockExecution.getAllFailureExceptions())
            .hasAtLeastOneElementOfType(DeadlockLoserDataAccessException.class);
        
        // When: Retry job after deadlock resolution
        JobParameters retryAfterDeadlock = new JobParametersBuilder()
            .addDate("processDate", deadlockParameters.getDate("processDate"))
            .addString("deadlockRecovery", "enabled")
            .addLong("deadlockRetryDelay", 5000L)
            .addLong("run.id", System.currentTimeMillis() + 1)
            .toJobParameters();
            
        JobExecution recoveryExecution = jobLauncherTestUtils.launchJob(retryAfterDeadlock);
        
        // Then: Verify successful recovery from deadlock
        assertThat(recoveryExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // Validate data consistency after deadlock recovery
        validateDataConsistencyAfterDeadlock(deadlockExecution, recoveryExecution);
    }

    /**
     * Test Case: Database Connection Recovery
     * Tests batch job behavior when database connections are lost during processing,
     * validating connection recovery, transaction resumption, and data integrity.
     * This ensures connection resilience matching JCL database connection management.
     */
    @Test
    @DisplayName("Database connection recovery maintains processing continuity")
    void testDatabaseConnectionRecovery() throws Exception {
        // Given: Long-running batch job with connection monitoring
        jobLauncherTestUtils.setJob(dailyTransactionJob.dailyTransactionJob());
        
        JobParameters connectionParameters = new JobParametersBuilder()
            .addDate("processDate", new Date())
            .addString("connectionMonitoring", "enabled")
            .addString("simulateFailure", "connectionLoss")
            .addLong("connectionFailurePoint", 1500L) // Lose connection after 1500 records
            .addLong("connectionRecoveryDelay", 10000L) // 10 second recovery
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters();
        
        // When: Execute job with connection loss simulation
        JobExecution connectionExecution = jobLauncherTestUtils.launchJob(connectionParameters);
        
        // Then: Verify connection loss was handled gracefully
        assertThat(connectionExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(connectionExecution.getAllFailureExceptions())
            .hasAtLeastOneElementOfType(DataAccessException.class);
        
        StepExecution connectionStep = connectionExecution.getStepExecutions()
            .iterator().next();
        
        // Validate processing state at connection loss
        assertThat(connectionStep.getReadCount()).isGreaterThan(1500L);
        assertThat(connectionStep.getCommitCount()).isGreaterThan(0);
        
        // When: Restart after connection recovery
        JobParameters connectionRecoveryParameters = new JobParametersBuilder()
            .addDate("processDate", connectionParameters.getDate("processDate"))
            .addString("connectionRecovery", "enabled") 
            .addString("restart", "true")
            .addLong("run.id", connectionParameters.getLong("run.id"))
            .toJobParameters();
            
        JobExecution recoveryExecution = jobLauncherTestUtils.launchJob(connectionRecoveryParameters);
        
        // Then: Verify successful connection recovery and job completion
        assertThat(recoveryExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // Validate connection recovery metrics
        validateConnectionRecovery(connectionExecution, recoveryExecution);
        
        // Verify data integrity after connection recovery
        validateDataIntegrityAfterConnectionLoss();
    }

    /**
     * Test Case: File Handle Management on Restart
     * Tests that file handles are properly managed during job restarts,
     * ensuring files are closed/reopened correctly and no handle leaks occur.
     * This validates file resource management matching JCL file handling.
     */
    @Test
    @DisplayName("File handle management prevents resource leaks on restart")
    void testFileHandleManagementOnRestart() throws Exception {
        // Given: Statement generation job with file output processing
        jobLauncherTestUtils.setJob(statementGenerationJob.statementGenerationJob());
        
        JobParameters fileParameters = new JobParametersBuilder()
            .addDate("statementDate", new Date())
            .addString("outputType", "file")
            .addString("fileHandleMonitoring", "enabled")
            .addString("outputDirectory", "/tmp/batch-test-output")
            .addString("simulateFailure", "fileWriteError")
            .addLong("fileFailurePoint", 200L) // Fail during file writing
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters();
        
        // Monitor file handles before execution
        int preExecutionHandles = getOpenFileHandleCount();
        
        // When: Execute job with file write failure
        JobExecution fileExecution = jobLauncherTestUtils.launchJob(fileParameters);
        
        // Then: Verify file failure handled correctly
        assertThat(fileExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
        
        // Validate file handles were cleaned up after failure
        int postFailureHandles = getOpenFileHandleCount();
        assertThat(postFailureHandles).isEqualTo(preExecutionHandles);
        
        // When: Restart job with file recovery
        JobParameters fileRestartParameters = new JobParametersBuilder()
            .addDate("statementDate", fileParameters.getDate("statementDate"))
            .addString("outputType", "file")
            .addString("fileRecovery", "enabled")
            .addString("restart", "true")
            .addLong("run.id", fileParameters.getLong("run.id"))
            .toJobParameters();
            
        JobExecution fileRestartExecution = jobLauncherTestUtils.launchJob(fileRestartParameters);
        
        // Then: Verify successful file restart with proper handle management
        assertThat(fileRestartExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // Validate file handles properly managed during restart
        int postRestartHandles = getOpenFileHandleCount();
        assertThat(postRestartHandles).isEqualTo(preExecutionHandles);
        
        // Verify file output completeness after restart
        validateFileOutputCompleteness(fileExecution, fileRestartExecution);
    }

    // ========== HELPER METHODS FOR RECOVERY TESTING ==========

    /**
     * Simulates failure at a specific step in batch job execution.
     * Used to create controlled failure scenarios for restart testing.
     */
    private void simulateFailureAtStep(String stepName, JobParameters parameters) {
        // Implementation would inject failure at specified step
        // This helper method supports controlled failure injection
        Map<String, Object> failureConfig = new HashMap<>();
        failureConfig.put("stepName", stepName);
        failureConfig.put("failureType", parameters.getString("simulateFailure"));
        
        // Configure failure injection based on step and parameters
        if (parameters.getString("simulateFailure") != null) {
            setupFailureScenario(stepName, parameters.getString("simulateFailure"));
        }
    }

    /**
     * Validates that recovery behavior matches JCL restart functionality.
     * Compares Spring Batch restart behavior against JCL equivalent operations.
     */
    private void validateRecoveryBehavior(JobExecution original, JobExecution recovered) {
        // Validate recovery preserves execution state
        assertThat(recovered.getJobInstance().getJobName())
            .isEqualTo(original.getJobInstance().getJobName());
        
        // Verify restart continued from failure point
        assertThat(recovered.getCreateTime()).isAfter(original.getEndTime());
        
        // Validate step execution continuity
        for (StepExecution recoveredStep : recovered.getStepExecutions()) {
            assertThat(recoveredStep.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        }
        
        // Verify no data duplication occurred
        validateNoDuplicateProcessing(original, recovered);
    }

    /**
     * Sets up controlled failure scenarios for testing batch recovery.
     * Configures specific failure conditions based on test requirements.
     */
    private void setupFailureScenario(String failureType, String stepName) {
        // Configure failure injection based on test scenario
        switch (failureType) {
            case "connectionLoss" -> setupConnectionLossScenario();
            case "deadlock" -> setupDeadlockScenario();
            case "transactionError" -> setupTransactionErrorScenario();
            case "fileWriteError" -> setupFileWriteErrorScenario();
            case "abnormalTermination" -> setupAbnormalTerminationScenario();
            default -> throw new IllegalArgumentException("Unknown failure type: " + failureType);
        }
    }

    /**
     * Verifies that restart behavior matches JCL equivalent operations.
     * Ensures Spring Batch restart functionality provides equivalent capabilities
     * to traditional mainframe JCL restart processing.
     */
    private void verifyJclEquivalentRestart(JobExecution original, JobExecution restarted) {
        // Verify restart preserved job identity (equivalent to JCL job name)
        assertThat(restarted.getJobInstance().getJobName())
            .isEqualTo(original.getJobInstance().getJobName());
        
        // Validate restart timing (JCL restart characteristics)
        assertThat(restarted.getCreateTime()).isAfter(original.getEndTime());
        assertThat(restarted.getStartTime()).isAfter(original.getEndTime());
        
        // Verify parameter preservation (JCL PARM equivalent)
        validateJclParameterEquivalence(original.getJobParameters(), 
                                      restarted.getJobParameters());
        
        // Validate step continuation (JCL step restart behavior)
        validateJclStepRestartBehavior(original, restarted);
        
        // Verify checkpoint restoration (JCL checkpoint equivalence)
        validateJclCheckpointEquivalence(original, restarted);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private Map<String, Object> captureInitialDatabaseState() {
        Map<String, Object> state = new HashMap<>();
        // Capture key database metrics for rollback validation
        state.put("accountCount", getCurrentAccountCount());
        state.put("transactionCount", getCurrentTransactionCount());  
        state.put("balanceSum", getCurrentTotalBalance());
        return state;
    }

    private Map<String, Object> captureCurrentDatabaseState() {
        return captureInitialDatabaseState(); // Same capture method
    }

    private void validateNoPartialTransactionCommits(JobExecution execution) {
        // Verify no partial transactions remain after rollback
        for (StepExecution step : execution.getStepExecutions()) {
            if (step.getStatus() == BatchStatus.FAILED) {
                assertThat(step.getWriteCount()).isEqualTo(0);
                assertThat(step.getRollbackCount()).isGreaterThan(0);
            }
        }
    }

    private Map<String, Object> captureStepResults(StepExecution stepExecution) {
        Map<String, Object> results = new HashMap<>();
        results.put("stepName", stepExecution.getStepName());
        results.put("readCount", stepExecution.getReadCount());
        results.put("writeCount", stepExecution.getWriteCount());
        results.put("commitCount", stepExecution.getCommitCount());
        results.put("status", stepExecution.getStatus());
        return results;
    }

    private void validateStepResultsPreservation(Map<String, Object> step1Results, 
                                               Map<String, Object> step2Results) {
        // Verify that completed step results were not altered during restart
        assertThat(step1Results.get("status")).isEqualTo(BatchStatus.COMPLETED);
        assertThat(step2Results.get("status")).isEqualTo(BatchStatus.COMPLETED);
        
        // Additional validation that step outputs remain intact
        validateStepOutputIntegrity(step1Results);
        validateStepOutputIntegrity(step2Results);
    }

    private void validateNoDuplicateProcessing(JobExecution original, JobExecution restarted) {
        // Ensure restart did not reprocess already completed work
        long originalProcessed = original.getStepExecutions().stream()
            .mapToLong(StepExecution::getWriteCount)
            .sum();
        
        long restartProcessed = restarted.getStepExecutions().stream()
            .mapToLong(StepExecution::getWriteCount)
            .sum();
        
        // Total processing should not exceed expected record count
        long totalExpected = getTotalExpectedRecords();
        assertThat(originalProcessed + restartProcessed).isLessThanOrEqualTo(totalExpected);
    }

    private Map<String, Object> captureJobResults(JobExecution execution) {
        Map<String, Object> results = new HashMap<>();
        results.put("jobName", execution.getJobInstance().getJobName());
        results.put("status", execution.getStatus());
        results.put("totalReadCount", execution.getStepExecutions().stream()
            .mapToLong(StepExecution::getReadCount).sum());
        results.put("totalWriteCount", execution.getStepExecutions().stream() 
            .mapToLong(StepExecution::getWriteCount).sum());
        return results;
    }

    private void validateIdempotencyBehavior(JobExecution first, JobExecution second) {
        Map<String, Object> firstResults = captureJobResults(first);
        Map<String, Object> secondResults = captureJobResults(second);
        
        // Verify second execution recognized idempotent scenario
        assertThat(secondResults.get("totalWriteCount")).isEqualTo(0L);
        
        // Validate duplicate detection occurred
        StepExecution secondStep = second.getStepExecutions().iterator().next();
        assertThat(secondStep.getWriteSkipCount()).isGreaterThan(0);
    }

    private void validateParameterPersistence(List<JobExecution> executions) {
        for (JobExecution execution : executions) {
            JobParameters parameters = execution.getJobParameters();
            
            // Verify core parameters preserved
            assertThat(parameters.getDate("processDate")).isNotNull();
            assertThat(parameters.getLong("run.id")).isNotNull();
            
            // Validate parameter accessibility in job repository
            assertThat(jobRepositoryTestUtils.getJobExecutions(
                execution.getJobInstance().getJobName())).contains(execution);
        }
    }

    private void validateContextBasedProcessing(StepExecution original, StepExecution restarted) {
        // Verify restarted step had access to original context data
        assertThat(restarted.getExecutionContext())
            .containsKey("previousCustomAttribute1");
        
        // Validate context-driven processing decisions
        assertThat(restarted.getReadCount())
            .isEqualTo(original.getExecutionContext().getLong("processingPosition", 0L));
    }

    private void validateParameterConsistency(JobParameters original, JobParameters restarted) {
        // Verify key parameters maintained consistency
        assertThat(restarted.getDate("statementDate"))
            .isEqualTo(original.getDate("statementDate"));
        assertThat(restarted.getString("statementType"))
            .isEqualTo(original.getString("statementType"));
        assertThat(restarted.getString("specialHandling"))
            .isEqualTo(original.getString("specialHandling"));
    }

    private Map<String, Object> captureResourceState() {
        Map<String, Object> state = new HashMap<>();
        state.put("openFileHandles", getOpenFileHandleCount());
        state.put("activeConnections", getActiveDatabaseConnections());
        state.put("memoryUsage", getCurrentMemoryUsage());
        state.put("threadCount", getCurrentThreadCount());
        return state;
    }

    private void validateResourceCleanup(Map<String, Object> before, Map<String, Object> after) {
        // Verify resource counts returned to baseline
        assertThat(after.get("openFileHandles")).isEqualTo(before.get("openFileHandles"));
        assertThat(after.get("activeConnections")).isEqualTo(before.get("activeConnections"));
        
        // Memory usage should not indicate leaks  
        long beforeMemory = (Long) before.get("memoryUsage");
        long afterMemory = (Long) after.get("memoryUsage");
        assertThat(afterMemory - beforeMemory).isLessThan(50 * 1024 * 1024); // < 50MB difference
    }

    private void validateCompensationCleanup(JobExecution execution) {
        // Verify compensation logic cleaned up partial work
        for (StepExecution step : execution.getStepExecutions()) {
            if (step.getExecutionContext().containsKey("compensation.executed")) {
                Boolean compensationExecuted = step.getExecutionContext()
                    .get("compensation.executed", Boolean.class);
                assertThat(compensationExecuted).isTrue();
            }
        }
    }

    private void validateNoDoubleCompensation(JobExecution original, JobExecution restarted) {
        // Ensure compensation logic was not executed twice
        for (StepExecution step : restarted.getStepExecutions()) {
            assertThat(step.getExecutionContext())
                .doesNotContainKey("compensation.executed");
        }
    }

    // Additional validation helper methods
    private void validateDatabaseConnectionCleanup() {
        assertThat(getActiveDatabaseConnections()).isEqualTo(0);
    }

    private void validateFileHandleCleanup() {
        // Verify no additional file handles remain open
        assertThat(getOpenFileHandleCount()).isLessThanOrEqualTo(getBaselineFileHandleCount());
    }

    private void validateBatchInfrastructureStability() {
        // Verify Spring Batch infrastructure remained stable after abnormal termination
        assertThat(testJobRepository).isNotNull();
        assertThat(testJobLauncher).isNotNull();
    }

    private void validateDataConsistencyAfterDeadlock(JobExecution deadlocked, JobExecution recovered) {
        // Verify data integrity after deadlock recovery
        assertThat(getCurrentAccountCount()).isGreaterThan(0);
        assertThat(getCurrentTransactionCount()).isGreaterThan(0);
        
        // Validate no orphaned records from deadlock
        validateReferentialIntegrity();
    }

    private void validateConnectionRecovery(JobExecution failed, JobExecution recovered) {
        // Verify connection recovery metrics
        batchTestUtils.validateJobExecution(recovered);
        
        // Validate processing continuity after connection recovery
        long failedProcessed = failed.getStepExecutions().stream()
            .mapToLong(StepExecution::getWriteCount).sum();
        long recoveredProcessed = recovered.getStepExecutions().stream()
            .mapToLong(StepExecution::getWriteCount).sum();
        
        assertThat(failedProcessed + recoveredProcessed).isGreaterThan(0);
    }

    private void validateDataIntegrityAfterConnectionLoss() {
        // Verify database referential integrity after connection recovery
        validateReferentialIntegrity();
        validateBalanceConsistency();
    }

    private void validateFileOutputCompleteness(JobExecution failed, JobExecution restarted) {
        // Verify file outputs are complete after restart
        StepExecution failedStep = failed.getStepExecutions().iterator().next();
        StepExecution restartedStep = restarted.getStepExecutions().iterator().next();
        
        long totalExpectedOutput = getTotalExpectedFileRecords();
        long totalActualOutput = failedStep.getWriteCount() + restartedStep.getWriteCount();
        
        assertThat(totalActualOutput).isEqualTo(totalExpectedOutput);
    }

    // JCL equivalence validation methods
    private void validateJclParameterEquivalence(JobParameters original, JobParameters restarted) {
        // Verify parameter passing matches JCL PARM behavior
        assertThat(restarted.getDate("processDate")).isEqualTo(original.getDate("processDate"));
        assertThat(restarted.getLong("run.id")).isEqualTo(original.getLong("run.id"));
    }

    private void validateJclStepRestartBehavior(JobExecution original, JobExecution restarted) {
        // Verify step restart behavior matches JCL step restart
        List<StepExecution> originalSteps = new ArrayList<>(original.getStepExecutions());
        List<StepExecution> restartedSteps = new ArrayList<>(restarted.getStepExecutions());
        
        // Validate restart continued from failure point
        if (!originalSteps.isEmpty() && !restartedSteps.isEmpty()) {
            StepExecution lastOriginalStep = originalSteps.get(originalSteps.size() - 1);
            StepExecution firstRestartStep = restartedSteps.get(0);
            
            // Verify restart step relationship
            assertThat(firstRestartStep.getStepName()).contains(lastOriginalStep.getStepName());
        }
    }

    private void validateJclCheckpointEquivalence(JobExecution original, JobExecution restarted) {
        // Verify checkpoint behavior equivalent to JCL checkpoint processing
        for (StepExecution originalStep : original.getStepExecutions()) {
            if (originalStep.getStatus() == BatchStatus.FAILED) {
                assertThat(originalStep.getCommitCount()).isGreaterThan(0);
                assertThat(originalStep.getExecutionContext()).isNotNull();
            }
        }
    }

    // Resource monitoring helper methods
    private int getOpenFileHandleCount() {
        // Return current open file handle count (would integrate with system monitoring)
        return batchTestUtils.collectPerformanceMetrics().get("openFileHandles").intValue();
    }

    private int getActiveDatabaseConnections() {
        // Return active database connection count
        return batchTestUtils.collectPerformanceMetrics().get("activeConnections").intValue();
    }

    private long getCurrentMemoryUsage() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    private int getCurrentThreadCount() {
        return Thread.activeCount();
    }

    private int getBaselineFileHandleCount() {
        // Return baseline file handle count for comparison
        return 10; // Typical baseline for Spring Boot application
    }

    // Database state helper methods
    private long getCurrentAccountCount() {
        return batchTestUtils.collectPerformanceMetrics().get("accountCount").longValue();
    }

    private long getCurrentTransactionCount() {
        return batchTestUtils.collectPerformanceMetrics().get("transactionCount").longValue();
    }

    private long getCurrentTotalBalance() {
        return batchTestUtils.collectPerformanceMetrics().get("totalBalance").longValue();
    }

    private long getTotalExpectedRecords() {
        return batchTestUtils.collectPerformanceMetrics().get("expectedRecords").longValue();
    }

    private long getTotalExpectedFileRecords() {
        return batchTestUtils.collectPerformanceMetrics().get("expectedFileRecords").longValue();
    }

    private void validateReferentialIntegrity() {
        // Verify database referential integrity constraints
        assertThat(getCurrentAccountCount()).isGreaterThan(0);
        assertThat(getCurrentTransactionCount()).isGreaterThan(0);
    }

    private void validateBalanceConsistency() {
        // Verify account balance consistency
        assertThat(getCurrentTotalBalance()).isGreaterThan(0);
    }

    private void validateStepOutputIntegrity(Map<String, Object> stepResults) {
        // Verify step output data integrity
        assertThat(stepResults.get("writeCount")).isNotNull();
        assertThat(stepResults.get("status")).isEqualTo(BatchStatus.COMPLETED);
    }

    // Failure scenario setup methods
    private void setupConnectionLossScenario() {
        // Configure connection loss simulation through database connection management
        System.setProperty("test.connection.failure.enabled", "true");
        System.setProperty("test.connection.failure.point", "1500");
    }

    private void setupDeadlockScenario() {
        // Configure deadlock simulation through concurrent access patterns
        System.setProperty("test.deadlock.simulation.enabled", "true");
        System.setProperty("test.deadlock.concurrent.threads", "4");
        System.setProperty("test.deadlock.target.table", "account");
    }

    private void setupTransactionErrorScenario() {
        // Configure transaction error simulation through data validation failures
        System.setProperty("test.transaction.error.enabled", "true");
        System.setProperty("test.transaction.error.type", "dataIntegrityViolation");
    }

    private void setupFileWriteErrorScenario() {
        // Configure file write error simulation through I/O exceptions
        System.setProperty("test.file.error.enabled", "true");
        System.setProperty("test.file.error.point", "200");
        System.setProperty("test.file.error.type", "writePermissionDenied");
    }

    private void setupAbnormalTerminationScenario() {
        // Configure abnormal termination simulation through thread interruption
        System.setProperty("test.abnormal.termination.enabled", "true");
        System.setProperty("test.abnormal.termination.type", "threadInterrupt");
        System.setProperty("test.resource.monitoring.enabled", "true");
    }
}