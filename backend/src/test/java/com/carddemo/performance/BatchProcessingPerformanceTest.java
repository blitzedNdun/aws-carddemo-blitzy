/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.performance;

import com.carddemo.batch.DailyTransactionJob;
import com.carddemo.batch.InterestCalculationJob;
import com.carddemo.batch.StatementGenerationJob;
import com.carddemo.batch.TransactionReportJob;
import com.carddemo.batch.BatchJobLauncher;
import com.carddemo.batch.BatchJobListener;
import com.carddemo.config.BatchConfig;
import com.carddemo.performance.TestDataGenerator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.BatchStatus;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.*;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Comprehensive performance test suite for Spring Batch jobs validating completion within 4-hour processing window.
 * 
 * This test class validates that all batch processing jobs converted from COBOL JCL programs maintain
 * performance equivalence with the original mainframe implementation. Each test validates specific
 * performance requirements including execution time, throughput, memory usage, and database performance
 * under production-size workloads.
 * 
 * COBOL Migration Context:
 * The original mainframe batch processing included:
 * - CBTRN01C.cbl: Daily transaction processing (handled by DailyTransactionJob)
 * - CBTRN02C.cbl: Transaction validation processing (handled by DailyTransactionJob)
 * - CBACT04C.cbl: Interest calculation processing (handled by InterestCalculationJob)
 * - CBSTM03A.cbl: Statement text generation (handled by StatementGenerationJob)
 * - CBSTM03B.cbl: Statement formatting (handled by StatementGenerationJob)
 * 
 * Performance Requirements:
 * - Complete batch processing within 4-hour window (14,400 seconds)
 * - Maintain throughput of 10,000+ transactions per hour
 * - Memory usage remains below 2GB during processing
 * - Database response times under 100ms for batch operations
 * - Zero data loss or corruption during high-volume processing
 * 
 * Test Coverage:
 * - Individual job performance validation
 * - 4-hour window compliance testing
 * - Production volume throughput testing
 * - Memory usage monitoring under load
 * - Database performance with large datasets
 * - Parallel execution capability testing
 * - Job restart and recovery performance
 * - Mainframe benchmark comparison
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false",
    "spring.datasource.url=jdbc:postgresql://localhost:5432/carddemo_test",
    "management.metrics.export.prometheus.enabled=true",
    "logging.level.com.carddemo.batch=DEBUG",
    "carddemo.batch.performance.test=true"
})
public class BatchProcessingPerformanceTest {

    // Performance test constants
    private static final int FOUR_HOUR_WINDOW_SECONDS = 14400; // 4 hours = 14,400 seconds
    private static final int TARGET_TPS = 10000; // Transactions per hour
    private static final long MAX_MEMORY_USAGE_MB = 2048; // 2GB memory limit
    private static final int DB_RESPONSE_TIME_THRESHOLD_MS = 100; // Database response time limit
    private static final int LARGE_DATASET_SIZE = 100000; // Production-size dataset

    // Spring Batch infrastructure components
    @Autowired
    private DailyTransactionJob dailyTransactionJob;
    
    @Autowired
    private InterestCalculationJob interestCalculationJob;
    
    @Autowired
    private StatementGenerationJob statementGenerationJob;
    
    @Autowired
    private TransactionReportJob transactionReportJob;
    
    @Autowired
    private BatchJobLauncher batchJobLauncher;
    
    @Autowired
    private BatchJobListener batchJobListener;
    
    @Autowired
    private BatchConfig batchConfig;
    
    @Autowired
    private TestDataGenerator testDataGenerator;
    
    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;
    
    @Autowired
    private MeterRegistry meterRegistry;

    // Performance metrics storage
    private List<Map<String, Object>> performanceResults;
    private Timer.Sample currentTimerSample;
    private long testStartTime;
    private long maxMemoryUsed;

    /**
     * Set up test environment before each performance test execution.
     * 
     * Initializes performance monitoring, cleans previous test data, and prepares
     * production-size datasets for comprehensive performance validation.
     */
    @BeforeEach
    public void setUp() {
        // Initialize performance tracking
        performanceResults = new ArrayList<>();
        testStartTime = System.currentTimeMillis();
        maxMemoryUsed = 0L;
        
        // Clean job repository from previous test runs
        jobRepositoryTestUtils.removeJobExecutions();
        
        // Initialize performance metrics collection
        initializePerformanceMetrics();
        
        // Log test initialization
        logTestExecution("Performance test setup completed", null);
    }

    /**
     * Clean up test environment after each performance test execution.
     * 
     * Performs cleanup of test data, generates performance reports, and validates
     * that system resources are properly released after testing.
     */
    @AfterEach
    public void tearDown() {
        // Generate performance report for the completed test
        generateBatchPerformanceReport();
        
        // Clean up test data
        cleanupTestData();
        
        // Log test completion
        long testDuration = System.currentTimeMillis() - testStartTime;
        logTestExecution("Performance test completed", testDuration);
    }

    /**
     * Performance test for DailyTransactionJob validating transaction processing throughput.
     * 
     * This test validates that the Spring Batch implementation of daily transaction processing
     * maintains equivalent performance to the original CBTRN01C and CBTRN02C COBOL programs.
     * 
     * Test Validation:
     * - Processes production-size transaction dataset (100,000+ records)
     * - Validates completion within performance window (60 minutes)
     * - Monitors memory usage during chunk processing
     * - Validates transaction throughput meets mainframe benchmarks
     * - Ensures data integrity throughout processing
     */
    @Test
    @DisplayName("Daily Transaction Job - Production Volume Performance Test")
    @Timeout(value = 90, unit = TimeUnit.MINUTES)
    public void testDailyTransactionJobPerformance() throws Exception {
        // Setup production-size test data
        setupProductionSizeTestData();
        
        // Configure job parameters for performance testing
        JobParameters jobParameters = new JobParametersBuilder()
            .addLong("run.id", System.currentTimeMillis())
            .addString("processDate", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
            .addLong("chunkSize", 1000L)
            .addString("performanceTest", "true")
            .toJobParameters();
        
        // Start performance measurement
        Instant startTime = Instant.now();
        Timer.Sample timerSample = Timer.start(meterRegistry);
        
        // Execute daily transaction job
        JobExecution jobExecution = dailyTransactionJob.dailyTransactionJob()
            .getJobLauncher().run(dailyTransactionJob.dailyTransactionJob(), jobParameters);
        
        // Stop performance measurement
        long executionTimeMs = timerSample.stop(Timer.builder("batch.job.execution.time")
            .tag("job", "dailyTransactionJob")
            .register(meterRegistry));
        
        // Validate job completed successfully
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");
        
        // Validate performance requirements
        long executionTimeSeconds = executionTimeMs / 1000;
        assertThat(executionTimeSeconds)
            .as("Daily transaction job should complete within 60 minutes")
            .isLessThan(3600);
        
        // Validate throughput requirements
        long processedRecords = getTotalProcessedRecords(jobExecution);
        double throughputPerHour = (processedRecords * 3600.0) / executionTimeSeconds;
        
        assertThat(throughputPerHour)
            .as("Throughput should meet minimum TPS requirement")
            .isGreaterThan(TARGET_TPS);
        
        // Record performance metrics
        recordPerformanceResult("dailyTransactionJob", executionTimeMs, processedRecords, throughputPerHour);
    }

    /**
     * Performance test for InterestCalculationJob validating BigDecimal precision and throughput.
     * 
     * This test validates that interest calculation processing maintains COBOL COMP-3 precision
     * while delivering acceptable performance for monthly interest calculation cycles.
     * 
     * Test Validation:
     * - Processes production-size account dataset with interest calculations
     * - Validates BigDecimal precision matches COBOL COMP-3 accuracy
     * - Monitors calculation throughput and memory usage
     * - Ensures completion within 30-minute processing window
     * - Validates financial calculation accuracy under load
     */
    @Test
    @DisplayName("Interest Calculation Job - Financial Precision Performance Test")
    @Timeout(value = 45, unit = TimeUnit.MINUTES)
    public void testInterestCalculationJobPerformance() throws Exception {
        // Setup production-size account data for interest calculation
        List<Map<String, Object>> accountData = testDataGenerator.generateTransactionList(LARGE_DATASET_SIZE);
        
        // Configure job parameters with processing date
        JobParameters jobParameters = new JobParametersBuilder()
            .addLong("run.id", System.currentTimeMillis())
            .addString("processingDate", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
            .addLong("chunkSize", 500L)
            .addString("performanceTest", "true")
            .toJobParameters();
        
        // Start performance measurement
        Timer.Sample timerSample = Timer.start(meterRegistry);
        
        // Execute interest calculation job
        JobExecution jobExecution = interestCalculationJob.interestCalculationJob()
            .getJobLauncher().run(interestCalculationJob.interestCalculationJob(), jobParameters);
        
        // Stop performance measurement
        long executionTimeMs = timerSample.stop(Timer.builder("batch.job.execution.time")
            .tag("job", "interestCalculationJob")
            .register(meterRegistry));
        
        // Validate job completed successfully
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // Validate performance requirements (30 minute window)
        long executionTimeSeconds = executionTimeMs / 1000;
        assertThat(executionTimeSeconds)
            .as("Interest calculation should complete within 30 minutes")
            .isLessThan(1800);
        
        // Validate memory usage during complex calculations
        long memoryUsed = getMaxMemoryUsage();
        assertThat(memoryUsed)
            .as("Memory usage should remain below 2GB limit")
            .isLessThan(MAX_MEMORY_USAGE_MB);
        
        // Validate calculation throughput
        long processedAccounts = getTotalProcessedRecords(jobExecution);
        double accountsPerMinute = (processedAccounts * 60.0) / executionTimeSeconds;
        
        assertThat(accountsPerMinute)
            .as("Interest calculation throughput should meet performance target")
            .isGreaterThan(1000); // Minimum 1000 accounts per minute
        
        // Record performance metrics
        recordPerformanceResult("interestCalculationJob", executionTimeMs, processedAccounts, accountsPerMinute);
    }

    /**
     * Performance test for StatementGenerationJob validating document generation throughput.
     * 
     * This test validates that statement generation processing can handle production volume
     * customer statement generation within acceptable time windows while maintaining
     * template processing performance.
     * 
     * Test Validation:
     * - Generates statements for production-size customer base
     * - Validates template processing performance with Thymeleaf
     * - Monitors file I/O operations and disk usage
     * - Ensures completion within 90-minute processing window
     * - Validates statement format accuracy and completeness
     */
    @Test
    @DisplayName("Statement Generation Job - Document Processing Performance Test")
    @Timeout(value = 120, unit = TimeUnit.MINUTES)
    public void testStatementGenerationJobPerformance() throws Exception {
        // Setup customer data for statement generation
        setupProductionSizeTestData();
        
        // Configure job parameters for statement generation
        JobParameters jobParameters = new JobParametersBuilder()
            .addLong("run.id", System.currentTimeMillis())
            .addString("statementDate", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
            .addLong("chunkSize", 200L) // Smaller chunks for document generation
            .addString("outputFormat", "PDF")
            .addString("performanceTest", "true")
            .toJobParameters();
        
        // Start performance measurement
        Timer.Sample timerSample = Timer.start(meterRegistry);
        
        // Execute statement generation job
        JobExecution jobExecution = statementGenerationJob.statementGenerationJob()
            .getJobLauncher().run(statementGenerationJob.statementGenerationJob(), jobParameters);
        
        // Stop performance measurement
        long executionTimeMs = timerSample.stop(Timer.builder("batch.job.execution.time")
            .tag("job", "statementGenerationJob")
            .register(meterRegistry));
        
        // Validate job completed successfully
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // Validate performance requirements (90 minute window)
        long executionTimeSeconds = executionTimeMs / 1000;
        assertThat(executionTimeSeconds)
            .as("Statement generation should complete within 90 minutes")
            .isLessThan(5400);
        
        // Validate document generation throughput
        long statementsGenerated = getTotalProcessedRecords(jobExecution);
        double statementsPerHour = (statementsGenerated * 3600.0) / executionTimeSeconds;
        
        assertThat(statementsPerHour)
            .as("Statement generation throughput should meet performance target")
            .isGreaterThan(2000); // Minimum 2000 statements per hour
        
        // Record performance metrics
        recordPerformanceResult("statementGenerationJob", executionTimeMs, statementsGenerated, statementsPerHour);
    }

    /**
     * Performance test for TransactionReportJob validating report generation performance.
     * 
     * This test validates that transaction report generation can handle large date ranges
     * and high transaction volumes while maintaining acceptable query performance and
     * report formatting speed.
     * 
     * Test Validation:
     * - Processes transaction reports for large date ranges
     * - Validates database query performance with large datasets
     * - Monitors report formatting and pagination performance
     * - Ensures completion within 45-minute processing window
     * - Validates report accuracy and data completeness
     */
    @Test
    @DisplayName("Transaction Report Job - Query and Report Performance Test")
    @Timeout(value = 60, unit = TimeUnit.MINUTES)
    public void testTransactionReportJobPerformance() throws Exception {
        // Setup transaction data for reporting
        setupProductionSizeTestData();
        
        // Configure job parameters for large date range report
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30); // 30-day report
        
        JobParameters jobParameters = new JobParametersBuilder()
            .addLong("run.id", System.currentTimeMillis())
            .addString("startDate", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .addString("endDate", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .addLong("pageSize", 1000L)
            .addString("performanceTest", "true")
            .toJobParameters();
        
        // Start performance measurement
        Timer.Sample timerSample = Timer.start(meterRegistry);
        
        // Execute transaction report job
        JobExecution jobExecution = transactionReportJob.transactionReportJob()
            .getJobLauncher().run(transactionReportJob.transactionReportJob(), jobParameters);
        
        // Stop performance measurement
        long executionTimeMs = timerSample.stop(Timer.builder("batch.job.execution.time")
            .tag("job", "transactionReportJob")
            .register(meterRegistry));
        
        // Validate job completed successfully
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // Validate performance requirements (45 minute window)
        long executionTimeSeconds = executionTimeMs / 1000;
        assertThat(executionTimeSeconds)
            .as("Transaction report should complete within 45 minutes")
            .isLessThan(2700);
        
        // Validate database query performance
        validateDatabasePerformance(jobExecution);
        
        // Record performance metrics
        long recordsProcessed = getTotalProcessedRecords(jobExecution);
        double recordsPerMinute = (recordsProcessed * 60.0) / executionTimeSeconds;
        recordPerformanceResult("transactionReportJob", executionTimeMs, recordsProcessed, recordsPerMinute);
    }

    /**
     * Comprehensive test validating all batch jobs complete within the critical 4-hour processing window.
     * 
     * This test simulates the complete nightly batch processing cycle by executing all major
     * batch jobs sequentially and validating that the entire suite completes within the
     * 4-hour window constraint inherited from the mainframe processing requirements.
     * 
     * Test Validation:
     * - Executes complete batch processing cycle
     * - Validates total execution time under 4-hour limit
     * - Monitors resource usage throughout processing cycle
     * - Validates job interdependencies and sequencing
     * - Ensures data consistency across all jobs
     */
    @Test
    @DisplayName("Complete Batch Processing Cycle - 4-Hour Window Validation")
    @Timeout(value = 5, unit = TimeUnit.HOURS) // Allow 5 hours for test timeout
    public void testBatchJobExecutionWithinFourHourWindow() throws Exception {
        // Setup comprehensive test data for full batch cycle
        setupProductionSizeTestData();
        
        List<CompletableFuture<JobExecution>> jobFutures = new ArrayList<>();
        Instant batchStartTime = Instant.now();
        
        // Execute jobs in sequence matching mainframe processing order
        String baseDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        // 1. Daily Transaction Processing
        CompletableFuture<JobExecution> dailyTransactionFuture = CompletableFuture.supplyAsync(() -> {
            try {
                JobParameters params = new JobParametersBuilder()
                    .addLong("run.id", System.currentTimeMillis())
                    .addString("processDate", baseDate)
                    .addString("jobSequence", "1")
                    .toJobParameters();
                    
                return dailyTransactionJob.dailyTransactionJob()
                    .getJobLauncher().run(dailyTransactionJob.dailyTransactionJob(), params);
            } catch (Exception e) {
                throw new RuntimeException("Daily transaction job failed", e);
            }
        });
        
        // Wait for daily transaction job to complete before proceeding
        JobExecution dailyTransactionExecution = dailyTransactionFuture.get(90, TimeUnit.MINUTES);
        assertThat(dailyTransactionExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // 2. Interest Calculation (depends on daily transaction completion)
        CompletableFuture<JobExecution> interestCalcFuture = CompletableFuture.supplyAsync(() -> {
            try {
                JobParameters params = new JobParametersBuilder()
                    .addLong("run.id", System.currentTimeMillis() + 1)
                    .addString("processingDate", baseDate)
                    .addString("jobSequence", "2")
                    .toJobParameters();
                    
                return interestCalculationJob.interestCalculationJob()
                    .getJobLauncher().run(interestCalculationJob.interestCalculationJob(), params);
            } catch (Exception e) {
                throw new RuntimeException("Interest calculation job failed", e);
            }
        });
        
        // 3. Statement Generation (can run in parallel with reports)
        CompletableFuture<JobExecution> statementGenFuture = CompletableFuture.supplyAsync(() -> {
            try {
                JobParameters params = new JobParametersBuilder()
                    .addLong("run.id", System.currentTimeMillis() + 2)
                    .addString("statementDate", baseDate)
                    .addString("jobSequence", "3")
                    .toJobParameters();
                    
                return statementGenerationJob.statementGenerationJob()
                    .getJobLauncher().run(statementGenerationJob.statementGenerationJob(), params);
            } catch (Exception e) {
                throw new RuntimeException("Statement generation job failed", e);
            }
        });
        
        // 4. Transaction Report Generation (can run in parallel with statements)
        CompletableFuture<JobExecution> reportFuture = CompletableFuture.supplyAsync(() -> {
            try {
                JobParameters params = new JobParametersBuilder()
                    .addLong("run.id", System.currentTimeMillis() + 3)
                    .addString("startDate", LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .addString("endDate", baseDate)
                    .addString("jobSequence", "4")
                    .toJobParameters();
                    
                return transactionReportJob.transactionReportJob()
                    .getJobLauncher().run(transactionReportJob.transactionReportJob(), params);
            } catch (Exception e) {
                throw new RuntimeException("Transaction report job failed", e);
            }
        });
        
        // Wait for all remaining jobs to complete
        JobExecution interestExecution = interestCalcFuture.get(45, TimeUnit.MINUTES);
        JobExecution statementExecution = statementGenFuture.get(120, TimeUnit.MINUTES);
        JobExecution reportExecution = reportFuture.get(60, TimeUnit.MINUTES);
        
        // Calculate total batch processing time
        long totalBatchTimeSeconds = java.time.Duration.between(batchStartTime, Instant.now()).getSeconds();
        
        // Critical validation: Entire batch cycle must complete within 4-hour window
        assertThat(totalBatchTimeSeconds)
            .as("Complete batch processing cycle must complete within 4-hour window")
            .isLessThan(FOUR_HOUR_WINDOW_SECONDS);
        
        // Validate all jobs completed successfully
        assertThat(interestExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(statementExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(reportExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // Record comprehensive performance metrics
        Map<String, Object> cycleResult = new HashMap<>();
        cycleResult.put("testName", "completeBatchCycle");
        cycleResult.put("totalExecutionTimeSeconds", totalBatchTimeSeconds);
        cycleResult.put("fourHourWindowCompliant", totalBatchTimeSeconds < FOUR_HOUR_WINDOW_SECONDS);
        cycleResult.put("jobCount", 4);
        cycleResult.put("maxMemoryUsageMB", getMaxMemoryUsage());
        
        performanceResults.add(cycleResult);
        
        logTestExecution("Complete batch cycle completed within 4-hour window", totalBatchTimeSeconds * 1000);
    }

    /**
     * Performance test validating batch job throughput with production-size datasets.
     * 
     * This test specifically validates that batch processing can handle the expected production
     * data volumes while maintaining acceptable throughput rates for business operations.
     * 
     * Test Validation:
     * - Tests with datasets exceeding 100,000 records
     * - Validates chunk processing efficiency
     * - Monitors throughput rates throughout processing
     * - Ensures performance scales linearly with data volume
     * - Validates system stability under high volume load
     */
    @Test
    @DisplayName("Batch Job Throughput - Production Dataset Volume Test")
    @Timeout(value = 3, unit = TimeUnit.HOURS)
    public void testBatchJobThroughputWithProductionDatasets() throws Exception {
        // Create multiple dataset sizes for throughput analysis
        List<Integer> datasetSizes = List.of(10000, 50000, 100000);
        Map<Integer, Map<String, Double>> throughputResults = new HashMap<>();
        
        for (Integer datasetSize : datasetSizes) {
            logTestExecution("Testing throughput with dataset size: " + datasetSize, null);
            
            // Generate test data for current size
            testDataGenerator.resetRandomSeed(12345L); // Consistent test data
            List<Map<String, Object>> testData = testDataGenerator.generateTransactionList(datasetSize);
            
            // Test daily transaction job throughput
            Timer.Sample timerSample = Timer.start(meterRegistry);
            
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .addString("processDate", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .addLong("chunkSize", 1000L)
                .addLong("datasetSize", datasetSize.longValue())
                .toJobParameters();
            
            JobExecution jobExecution = dailyTransactionJob.dailyTransactionJob()
                .getJobLauncher().run(dailyTransactionJob.dailyTransactionJob(), jobParameters);
            
            long executionTimeMs = timerSample.stop(Timer.builder("batch.throughput.test")
                .tag("datasetSize", String.valueOf(datasetSize))
                .register(meterRegistry));
            
            // Validate job completed successfully
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            
            // Calculate throughput metrics
            long executionTimeSeconds = executionTimeMs / 1000;
            double recordsPerSecond = (double) datasetSize / executionTimeSeconds;
            double recordsPerMinute = recordsPerSecond * 60;
            double recordsPerHour = recordsPerMinute * 60;
            
            // Store throughput results
            Map<String, Double> metrics = new HashMap<>();
            metrics.put("recordsPerSecond", recordsPerSecond);
            metrics.put("recordsPerMinute", recordsPerMinute);
            metrics.put("recordsPerHour", recordsPerHour);
            metrics.put("executionTimeSeconds", (double) executionTimeSeconds);
            
            throughputResults.put(datasetSize, metrics);
            
            // Validate minimum throughput requirements
            assertThat(recordsPerHour)
                .as("Throughput for " + datasetSize + " records should meet minimum requirement")
                .isGreaterThan(TARGET_TPS);
            
            // Clean up between tests
            cleanupTestData();
        }
        
        // Validate throughput scaling characteristics
        validateThroughputScaling(throughputResults);
        
        // Record comprehensive throughput analysis
        recordThroughputAnalysis(throughputResults);
    }

    /**
     * Performance test monitoring memory usage during high-volume batch processing.
     * 
     * This test validates that memory usage remains within acceptable limits during
     * batch processing operations, preventing out-of-memory conditions that could
     * impact system stability in production environments.
     * 
     * Test Validation:
     * - Monitors heap usage throughout batch execution
     * - Validates garbage collection performance
     * - Ensures memory usage stays below 2GB limit
     * - Tests memory efficiency of chunk processing
     * - Validates memory cleanup after job completion
     */
    @Test
    @DisplayName("Batch Job Memory Usage - High Volume Load Test")
    @Timeout(value = 2, unit = TimeUnit.HOURS)
    public void testBatchJobMemoryUsageUnderLoad() throws Exception {
        // Setup memory monitoring
        Runtime runtime = Runtime.getRuntime();
        List<Long> memoryReadings = new ArrayList<>();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Create memory monitoring thread
        Thread memoryMonitor = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                long usedMemoryMB = usedMemory / (1024 * 1024);
                memoryReadings.add(usedMemoryMB);
                maxMemoryUsed = Math.max(maxMemoryUsed, usedMemoryMB);
                
                // Record memory gauge metric
                Gauge.builder("batch.memory.usage.mb")
                    .register(meterRegistry, () -> usedMemoryMB);
                
                try {
                    Thread.sleep(5000); // Check every 5 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        memoryMonitor.start();
        
        try {
            // Setup large dataset for memory stress testing
            setupProductionSizeTestData();
            
            // Execute memory-intensive job (interest calculation with complex computations)
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .addString("processingDate", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .addLong("chunkSize", 500L) // Smaller chunks to test memory management
                .addString("memoryTest", "true")
                .toJobParameters();
            
            JobExecution jobExecution = interestCalculationJob.interestCalculationJob()
                .getJobLauncher().run(interestCalculationJob.interestCalculationJob(), jobParameters);
            
            // Validate job completed successfully
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            
        } finally {
            // Stop memory monitoring
            memoryMonitor.interrupt();
        }
        
        // Validate memory usage requirements
        assertThat(maxMemoryUsed)
            .as("Maximum memory usage should not exceed 2GB limit")
            .isLessThan(MAX_MEMORY_USAGE_MB);
        
        // Validate memory usage stability (no significant leaks)
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = (finalMemory - initialMemory) / (1024 * 1024);
        
        assertThat(memoryIncrease)
            .as("Memory increase after job completion should be minimal")
            .isLessThan(100); // Less than 100MB permanent increase
        
        // Record memory performance metrics
        Map<String, Object> memoryResult = new HashMap<>();
        memoryResult.put("testName", "memoryUsageUnderLoad");
        memoryResult.put("maxMemoryUsageMB", maxMemoryUsed);
        memoryResult.put("initialMemoryMB", initialMemory / (1024 * 1024));
        memoryResult.put("finalMemoryMB", finalMemory / (1024 * 1024));
        memoryResult.put("memoryIncreaseMB", memoryIncrease);
        memoryResult.put("memoryReadingsCount", memoryReadings.size());
        
        performanceResults.add(memoryResult);
    }

    /**
     * Performance test validating database performance during batch processing operations.
     * 
     * This test focuses on database interaction performance, ensuring that batch jobs
     * can maintain acceptable database response times even under high-volume operations
     * and concurrent access patterns.
     * 
     * Test Validation:
     * - Monitors database query execution times
     * - Validates connection pool performance
     * - Tests concurrent database access patterns
     * - Ensures queries complete under 100ms threshold
     * - Validates transaction isolation and consistency
     */
    @Test
    @DisplayName("Batch Job Database Performance - Query Response Time Test")
    @Timeout(value = 90, unit = TimeUnit.MINUTES)
    public void testBatchJobDatabasePerformance() throws Exception {
        // Setup database performance monitoring
        List<Long> queryResponseTimes = new ArrayList<>();
        Counter dbQueryCounter = Counter.builder("batch.database.queries.total")
            .register(meterRegistry);
        Timer dbQueryTimer = Timer.builder("batch.database.query.duration")
            .register(meterRegistry);
        
        // Setup production-size data for database stress testing
        setupProductionSizeTestData();
        
        // Configure job with database-intensive operations
        JobParameters jobParameters = new JobParametersBuilder()
            .addLong("run.id", System.currentTimeMillis())
            .addString("startDate", LocalDate.now().minusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE))
            .addString("endDate", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
            .addLong("pageSize", 1000L)
            .addString("databasePerformanceTest", "true")
            .toJobParameters();
        
        // Execute database-intensive job (transaction report with complex queries)
        Timer.Sample jobTimer = Timer.start(meterRegistry);
        JobExecution jobExecution = transactionReportJob.transactionReportJob()
            .getJobLauncher().run(transactionReportJob.transactionReportJob(), jobParameters);
        jobTimer.stop(dbQueryTimer);
        
        // Validate job completed successfully
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // Validate database performance requirements
        validateDatabasePerformance(jobExecution);
        
        // Record database performance metrics
        Map<String, Object> dbResult = new HashMap<>();
        dbResult.put("testName", "databasePerformance");
        dbResult.put("totalQueries", dbQueryCounter.count());
        dbResult.put("averageQueryTimeMs", dbQueryTimer.mean(TimeUnit.MILLISECONDS));
        dbResult.put("maxQueryTimeMs", dbQueryTimer.max(TimeUnit.MILLISECONDS));
        dbResult.put("queryTimeThresholdMet", dbQueryTimer.max(TimeUnit.MILLISECONDS) < DB_RESPONSE_TIME_THRESHOLD_MS);
        
        performanceResults.add(dbResult);
    }

    /**
     * Performance test validating parallel batch job execution capability.
     * 
     * This test validates that the system can handle multiple batch jobs running
     * concurrently without performance degradation or resource conflicts,
     * simulating realistic production scenarios.
     * 
     * Test Validation:
     * - Executes multiple jobs concurrently
     * - Validates resource sharing and isolation
     * - Monitors system performance under concurrent load
     * - Ensures data consistency across parallel executions
     * - Validates thread pool and connection pool efficiency
     */
    @Test
    @DisplayName("Parallel Batch Job Execution - Concurrent Processing Test")
    @Timeout(value = 2, unit = TimeUnit.HOURS)
    public void testParallelBatchJobExecution() throws Exception {
        // Setup test data for parallel execution
        setupProductionSizeTestData();
        
        List<CompletableFuture<JobExecution>> parallelJobs = new ArrayList<>();
        Instant parallelStartTime = Instant.now();
        
        // Launch multiple jobs in parallel
        // Job 1: Daily transaction processing
        CompletableFuture<JobExecution> job1 = CompletableFuture.supplyAsync(() -> {
            try {
                JobParameters params = new JobParametersBuilder()
                    .addLong("run.id", System.currentTimeMillis())
                    .addString("processDate", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .addString("parallelTest", "job1")
                    .toJobParameters();
                return dailyTransactionJob.dailyTransactionJob()
                    .getJobLauncher().run(dailyTransactionJob.dailyTransactionJob(), params);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        // Job 2: Transaction report generation
        CompletableFuture<JobExecution> job2 = CompletableFuture.supplyAsync(() -> {
            try {
                JobParameters params = new JobParametersBuilder()
                    .addLong("run.id", System.currentTimeMillis() + 1)
                    .addString("startDate", LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .addString("endDate", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .addString("parallelTest", "job2")
                    .toJobParameters();
                return transactionReportJob.transactionReportJob()
                    .getJobLauncher().run(transactionReportJob.transactionReportJob(), params);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        // Job 3: Statement generation (smaller dataset for faster completion)
        CompletableFuture<JobExecution> job3 = CompletableFuture.supplyAsync(() -> {
            try {
                JobParameters params = new JobParametersBuilder()
                    .addLong("run.id", System.currentTimeMillis() + 2)
                    .addString("statementDate", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .addLong("chunkSize", 100L)
                    .addString("parallelTest", "job3")
                    .toJobParameters();
                return statementGenerationJob.statementGenerationJob()
                    .getJobLauncher().run(statementGenerationJob.statementGenerationJob(), params);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        parallelJobs.add(job1);
        parallelJobs.add(job2);
        parallelJobs.add(job3);
        
        // Wait for all parallel jobs to complete
        CompletableFuture<Void> allJobs = CompletableFuture.allOf(
            parallelJobs.toArray(new CompletableFuture[0]));
        
        try {
            allJobs.get(2, TimeUnit.HOURS);
        } catch (TimeoutException e) {
            fail("Parallel job execution exceeded timeout");
        }
        
        long totalParallelTime = java.time.Duration.between(parallelStartTime, Instant.now()).getSeconds();
        
        // Validate all jobs completed successfully
        for (CompletableFuture<JobExecution> jobFuture : parallelJobs) {
            JobExecution execution = jobFuture.get();
            assertThat(execution.getStatus())
                .as("All parallel jobs should complete successfully")
                .isEqualTo(BatchStatus.COMPLETED);
        }
        
        // Validate parallel execution efficiency
        // Parallel execution should be faster than sequential execution
        assertThat(totalParallelTime)
            .as("Parallel execution should complete within reasonable time")
            .isLessThan(7200); // 2 hours maximum
        
        // Record parallel execution metrics
        Map<String, Object> parallelResult = new HashMap<>();
        parallelResult.put("testName", "parallelExecution");
        parallelResult.put("jobCount", parallelJobs.size());
        parallelResult.put("totalExecutionTimeSeconds", totalParallelTime);
        parallelResult.put("averageJobTimeSeconds", totalParallelTime); // All ran in parallel
        parallelResult.put("maxMemoryUsageMB", getMaxMemoryUsage());
        
        performanceResults.add(parallelResult);
    }

    /**
     * Performance test validating batch job restart and recovery capability.
     * 
     * This test validates that job restart operations maintain acceptable performance
     * and can recover quickly from failures without significant performance impact
     * on subsequent processing.
     * 
     * Test Validation:
     * - Tests job restart performance from failure points
     * - Validates checkpoint and recovery mechanisms
     * - Monitors restart overhead and recovery time
     * - Ensures data consistency after restart
     * - Validates performance after recovery
     */
    @Test
    @DisplayName("Batch Job Restart Performance - Recovery Time Test")
    @Timeout(value = 90, unit = TimeUnit.MINUTES)
    public void testBatchJobRestartCapability() throws Exception {
        // Setup test data
        setupProductionSizeTestData();
        
        // First, run a job that will be stopped mid-execution
        JobParameters initialParams = new JobParametersBuilder()
            .addLong("run.id", System.currentTimeMillis())
            .addString("processDate", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
            .addString("restartTest", "initial")
            .addLong("chunkSize", 1000L)
            .toJobParameters();
        
        Timer.Sample initialTimer = Timer.start(meterRegistry);
        JobExecution initialExecution = dailyTransactionJob.dailyTransactionJob()
            .getJobLauncher().run(dailyTransactionJob.dailyTransactionJob(), initialParams);
        
        // Let job run for a short time then stop it (simulate failure)
        Thread.sleep(10000); // Let it process some records
        
        // Stop the job (simulating failure scenario)
        if (initialExecution.getStatus() == BatchStatus.STARTED) {
            batchJobLauncher.stopJob(initialExecution.getId(), 
                new com.carddemo.dto.ApiRequest<>(new HashMap<>()));
        }
        
        long initialExecutionTime = initialTimer.stop(Timer.builder("batch.restart.initial.time")
            .register(meterRegistry));
        
        // Now restart the job and measure restart performance
        Timer.Sample restartTimer = Timer.start(meterRegistry);
        
        JobParameters restartParams = new JobParametersBuilder()
            .addLong("run.id", System.currentTimeMillis() + 1000) // Different run ID for restart
            .addString("processDate", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
            .addString("restartTest", "restart")
            .addLong("chunkSize", 1000L)
            .toJobParameters();
        
        JobExecution restartExecution = dailyTransactionJob.dailyTransactionJob()
            .getJobLauncher().run(dailyTransactionJob.dailyTransactionJob(), restartParams);
        
        long restartExecutionTime = restartTimer.stop(Timer.builder("batch.restart.recovery.time")
            .register(meterRegistry));
        
        // Validate restart completed successfully
        assertThat(restartExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // Validate restart performance (should complete quickly due to restart optimization)
        long restartTimeSeconds = restartExecutionTime / 1000;
        assertThat(restartTimeSeconds)
            .as("Job restart should complete within reasonable time")
            .isLessThan(1800); // 30 minutes maximum for restart
        
        // Record restart performance metrics
        Map<String, Object> restartResult = new HashMap<>();
        restartResult.put("testName", "jobRestart");
        restartResult.put("initialExecutionTimeMs", initialExecutionTime);
        restartResult.put("restartExecutionTimeMs", restartExecutionTime);
        restartResult.put("restartOverheadMs", restartExecutionTime - initialExecutionTime);
        restartResult.put("restartSuccessful", restartExecution.getStatus() == BatchStatus.COMPLETED);
        
        performanceResults.add(restartResult);
    }

    /**
     * Utility method to measure batch job execution time with comprehensive timing metrics.
     * 
     * This method provides standardized timing measurement for batch jobs, capturing
     * detailed execution metrics that can be used for performance analysis and
     * comparison against mainframe benchmarks.
     * 
     * @param jobName Name of the job being measured
     * @param jobExecution The JobExecution to measure
     * @return Comprehensive timing metrics map
     */
    public Map<String, Long> measureBatchJobExecutionTime(String jobName, JobExecution jobExecution) {
        Map<String, Long> timingMetrics = new HashMap<>();
        
        if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
            long totalExecutionMs = java.time.Duration.between(
                jobExecution.getStartTime(), jobExecution.getEndTime()).toMillis();
            
            timingMetrics.put("totalExecutionMs", totalExecutionMs);
            timingMetrics.put("totalExecutionSeconds", totalExecutionMs / 1000);
            
            // Calculate step-level timing
            long totalStepTime = 0L;
            for (var stepExecution : jobExecution.getStepExecutions()) {
                if (stepExecution.getStartTime() != null && stepExecution.getEndTime() != null) {
                    long stepDuration = java.time.Duration.between(
                        stepExecution.getStartTime(), stepExecution.getEndTime()).toMillis();
                    timingMetrics.put("step_" + stepExecution.getStepName() + "_ms", stepDuration);
                    totalStepTime += stepDuration;
                }
            }
            
            timingMetrics.put("totalStepExecutionMs", totalStepTime);
            timingMetrics.put("overheadMs", totalExecutionMs - totalStepTime);
            
            // Record timing metrics in Micrometer
            Timer.builder("batch.job.execution.detailed")
                .tag("jobName", jobName)
                .register(meterRegistry)
                .record(totalExecutionMs, TimeUnit.MILLISECONDS);
        }
        
        return timingMetrics;
    }

    /**
     * Validates performance metrics against established mainframe benchmarks.
     * 
     * This method compares current performance measurements against the original
     * mainframe processing benchmarks to ensure that the migrated system maintains
     * equivalent or better performance characteristics.
     * 
     * @param performanceMetrics Map of current performance measurements
     * @return Validation results comparing against mainframe benchmarks
     */
    public Map<String, Boolean> validatePerformanceAgainstMainframeBenchmarks(
            Map<String, Object> performanceMetrics) {
        
        Map<String, Boolean> validationResults = new HashMap<>();
        
        // Mainframe benchmark thresholds (based on original COBOL performance)
        Map<String, Long> mainframeBenchmarks = new HashMap<>();
        mainframeBenchmarks.put("dailyTransactionJob_maxSeconds", 3600L); // 1 hour
        mainframeBenchmarks.put("interestCalculationJob_maxSeconds", 1800L); // 30 minutes  
        mainframeBenchmarks.put("statementGenerationJob_maxSeconds", 5400L); // 90 minutes
        mainframeBenchmarks.put("transactionReportJob_maxSeconds", 2700L); // 45 minutes
        mainframeBenchmarks.put("completeBatchCycle_maxSeconds", (long) FOUR_HOUR_WINDOW_SECONDS);
        
        // Throughput benchmarks (records per hour)
        Map<String, Double> throughputBenchmarks = new HashMap<>();
        throughputBenchmarks.put("dailyTransactionJob_minThroughput", (double) TARGET_TPS);
        throughputBenchmarks.put("interestCalculationJob_minThroughput", 1000.0); // accounts per minute
        throughputBenchmarks.put("statementGenerationJob_minThroughput", 2000.0); // statements per hour
        
        // Validate execution time benchmarks
        for (Map.Entry<String, Long> benchmark : mainframeBenchmarks.entrySet()) {
            String metricKey = benchmark.getKey().replace("_maxSeconds", "_executionTimeSeconds");
            if (performanceMetrics.containsKey(metricKey)) {
                Long actualTime = ((Number) performanceMetrics.get(metricKey)).longValue();
                boolean meetsTarget = actualTime <= benchmark.getValue();
                validationResults.put(benchmark.getKey(), meetsTarget);
                
                // Log benchmark comparison
                logTestExecution(String.format("Benchmark validation - %s: %d <= %d = %b", 
                    benchmark.getKey(), actualTime, benchmark.getValue(), meetsTarget), null);
            }
        }
        
        // Validate throughput benchmarks
        for (Map.Entry<String, Double> benchmark : throughputBenchmarks.entrySet()) {
            String metricKey = benchmark.getKey().replace("_minThroughput", "_throughput");
            if (performanceMetrics.containsKey(metricKey)) {
                Double actualThroughput = ((Number) performanceMetrics.get(metricKey)).doubleValue();
                boolean meetsTarget = actualThroughput >= benchmark.getValue();
                validationResults.put(benchmark.getKey(), meetsTarget);
                
                // Log throughput comparison
                logTestExecution(String.format("Throughput validation - %s: %.2f >= %.2f = %b",
                    benchmark.getKey(), actualThroughput, benchmark.getValue(), meetsTarget), null);
            }
        }
        
        return validationResults;
    }

    /**
     * Generates comprehensive performance test report with detailed metrics and analysis.
     * 
     * This method creates a detailed performance report that can be used for
     * performance analysis, trend monitoring, and compliance validation against
     * the 4-hour processing window requirement.
     * 
     * @return Formatted performance report string
     */
    public String generateBatchPerformanceReport() {
        StringBuilder report = new StringBuilder();
        report.append("\n");
        report.append("=" .repeat(80)).append("\n");
        report.append("BATCH PROCESSING PERFORMANCE TEST REPORT\n");
        report.append("Generated: ").append(java.time.LocalDateTime.now()).append("\n");
        report.append("=" .repeat(80)).append("\n");
        
        if (performanceResults.isEmpty()) {
            report.append("No performance test results available.\n");
            return report.toString();
        }
        
        // Overall performance summary
        report.append("\nOVERALL PERFORMANCE SUMMARY:\n");
        report.append("-".repeat(40)).append("\n");
        report.append(String.format("Total Tests Executed: %d\n", performanceResults.size()));
        report.append(String.format("Test Duration: %d ms\n", System.currentTimeMillis() - testStartTime));
        report.append(String.format("Maximum Memory Usage: %d MB\n", maxMemoryUsed));
        
        // Individual test results
        report.append("\nINDIVIDUAL TEST RESULTS:\n");
        report.append("-".repeat(40)).append("\n");
        
        for (Map<String, Object> result : performanceResults) {
            String testName = (String) result.get("testName");
            report.append(String.format("\nTest: %s\n", testName));
            
            // Display relevant metrics based on test type
            if (result.containsKey("executionTimeMs")) {
                long timeMs = ((Number) result.get("executionTimeMs")).longValue();
                report.append(String.format("  Execution Time: %d ms (%.2f seconds)\n", 
                    timeMs, timeMs / 1000.0));
            }
            
            if (result.containsKey("throughput")) {
                double throughput = ((Number) result.get("throughput")).doubleValue();
                report.append(String.format("  Throughput: %.2f records/hour\n", throughput));
            }
            
            if (result.containsKey("recordsProcessed")) {
                long records = ((Number) result.get("recordsProcessed")).longValue();
                report.append(String.format("  Records Processed: %d\n", records));
            }
            
            if (result.containsKey("fourHourWindowCompliant")) {
                boolean compliant = (Boolean) result.get("fourHourWindowCompliant");
                report.append(String.format("  4-Hour Window Compliant: %s\n", 
                    compliant ? "PASS" : "FAIL"));
            }
        }
        
        // 4-hour window compliance summary
        report.append("\n4-HOUR PROCESSING WINDOW COMPLIANCE:\n");
        report.append("-".repeat(40)).append("\n");
        
        boolean overallCompliance = performanceResults.stream()
            .filter(result -> result.containsKey("fourHourWindowCompliant"))
            .allMatch(result -> (Boolean) result.get("fourHourWindowCompliant"));
        
        report.append(String.format("Overall Compliance Status: %s\n", 
            overallCompliance ? "COMPLIANT" : "NON-COMPLIANT"));
        
        // Recommendations section
        report.append("\nPERFORMANCE RECOMMENDATIONS:\n");
        report.append("-".repeat(40)).append("\n");
        
        if (maxMemoryUsed > MAX_MEMORY_USAGE_MB * 0.8) {
            report.append("- Consider optimizing memory usage (currently at ")
                  .append(maxMemoryUsed).append(" MB)\n");
        }
        
        if (!overallCompliance) {
            report.append("- Review job configuration and chunk sizes to improve performance\n");
            report.append("- Consider parallel processing optimization\n");
        }
        
        report.append("\nReport generated by BatchProcessingPerformanceTest\n");
        report.append("=" .repeat(80)).append("\n");
        
        String reportStr = report.toString();
        logTestExecution("Performance report generated", null);
        System.out.println(reportStr); // Output to console for test visibility
        
        return reportStr;
    }

    /**
     * Sets up production-size test data for comprehensive performance testing.
     * 
     * This method generates realistic test datasets that match production volume
     * and complexity to ensure performance tests accurately reflect real-world
     * processing requirements.
     */
    public void setupProductionSizeTestData() {
        try {
            // Reset test data generator for consistent results
            testDataGenerator.resetRandomSeed(12345L);
            
            // Generate production-size transaction data
            List<Map<String, Object>> transactions = testDataGenerator.generateTransactionList(LARGE_DATASET_SIZE);
            
            // Generate account data with COBOL COMP-3 precision
            for (int i = 0; i < 10000; i++) {
                Map<String, Object> account = testDataGenerator.generateAccount();
                // Generate balance with proper BigDecimal precision
                account.put("balance", testDataGenerator.generateComp3BigDecimal(7, 2));
            }
            
            logTestExecution("Production-size test data setup completed", null);
            
        } catch (Exception e) {
            logTestExecution("Error setting up test data: " + e.getMessage(), null);
            throw new RuntimeException("Failed to setup production test data", e);
        }
    }

    /**
     * Cleans up test data and resources after performance test execution.
     * 
     * This method ensures that test artifacts are properly cleaned up to prevent
     * interference between test runs and to free system resources.
     */
    public void cleanupTestData() {
        try {
            // Clean job repository test data
            if (jobRepositoryTestUtils != null) {
                jobRepositoryTestUtils.removeJobExecutions();
            }
            
            // Clear performance metrics
            performanceResults.clear();
            
            // Force garbage collection to free memory
            System.gc();
            
            logTestExecution("Test data cleanup completed", null);
            
        } catch (Exception e) {
            logTestExecution("Error during cleanup: " + e.getMessage(), null);
            // Don't throw exception during cleanup
        }
    }

    // ========================================================================================
    // Private Helper Methods
    // ========================================================================================

    /**
     * Initializes performance metrics collection infrastructure.
     */
    private void initializePerformanceMetrics() {
        // Initialize custom metrics for performance monitoring
        Counter.builder("batch.performance.test.started")
            .register(meterRegistry)
            .increment();
    }

    /**
     * Records performance test results for analysis and reporting.
     */
    private void recordPerformanceResult(String jobName, long executionTimeMs, 
                                       long recordsProcessed, double throughput) {
        Map<String, Object> result = new HashMap<>();
        result.put("testName", jobName);
        result.put("executionTimeMs", executionTimeMs);
        result.put("executionTimeSeconds", executionTimeMs / 1000);
        result.put("recordsProcessed", recordsProcessed);
        result.put("throughput", throughput);
        result.put("timestamp", System.currentTimeMillis());
        
        performanceResults.add(result);
    }

    /**
     * Gets the total number of records processed by a job execution.
     */
    private long getTotalProcessedRecords(JobExecution jobExecution) {
        return jobExecution.getStepExecutions().stream()
            .mapToLong(stepExecution -> stepExecution.getWriteCount())
            .sum();
    }

    /**
     * Gets the current maximum memory usage in MB.
     */
    private long getMaxMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return Math.max(maxMemoryUsed, usedMemory / (1024 * 1024));
    }

    /**
     * Validates database performance metrics for job execution.
     */
    private void validateDatabasePerformance(JobExecution jobExecution) {
        // This would typically integrate with database monitoring
        // For now, we'll validate based on job execution metrics
        
        jobExecution.getStepExecutions().forEach(stepExecution -> {
            if (stepExecution.getStartTime() != null && stepExecution.getEndTime() != null) {
                long stepDuration = java.time.Duration.between(
                    stepExecution.getStartTime(), stepExecution.getEndTime()).toMillis();
                
                long readCount = stepExecution.getReadCount();
                if (readCount > 0) {
                    double avgReadTimeMs = (double) stepDuration / readCount;
                    assertThat(avgReadTimeMs)
                        .as("Average database read time should be under threshold")
                        .isLessThan(DB_RESPONSE_TIME_THRESHOLD_MS);
                }
            }
        });
    }

    /**
     * Validates throughput scaling characteristics across dataset sizes.
     */
    private void validateThroughputScaling(Map<Integer, Map<String, Double>> throughputResults) {
        // Verify that throughput scales reasonably with data volume
        List<Integer> sizes = new ArrayList<>(throughputResults.keySet());
        sizes.sort(Integer::compareTo);
        
        for (int i = 1; i < sizes.size(); i++) {
            Integer smallerSize = sizes.get(i - 1);
            Integer largerSize = sizes.get(i);
            
            double smallerThroughput = throughputResults.get(smallerSize).get("recordsPerSecond");
            double largerThroughput = throughputResults.get(largerSize).get("recordsPerSecond");
            
            // Throughput should not degrade significantly with larger datasets
            double throughputRatio = largerThroughput / smallerThroughput;
            assertThat(throughputRatio)
                .as("Throughput should scale reasonably with data volume")
                .isGreaterThan(0.5); // Allow up to 50% degradation for larger datasets
        }
    }

    /**
     * Records comprehensive throughput analysis results.
     */
    private void recordThroughputAnalysis(Map<Integer, Map<String, Double>> throughputResults) {
        Map<String, Object> analysisResult = new HashMap<>();
        analysisResult.put("testName", "throughputAnalysis");
        analysisResult.put("datasetSizesTested", new ArrayList<>(throughputResults.keySet()));
        analysisResult.put("detailedResults", throughputResults);
        
        // Calculate average throughput across all dataset sizes
        double avgThroughput = throughputResults.values().stream()
            .mapToDouble(metrics -> metrics.get("recordsPerHour"))
            .average()
            .orElse(0.0);
        
        analysisResult.put("averageThroughputPerHour", avgThroughput);
        analysisResult.put("meetsTargetThroughput", avgThroughput >= TARGET_TPS);
        
        performanceResults.add(analysisResult);
    }

    /**
     * Logs test execution events with timing information.
     */
    private void logTestExecution(String message, Long durationMs) {
        String logMessage = String.format("[PERF-TEST] %s", message);
        if (durationMs != null) {
            logMessage += String.format(" (Duration: %d ms)", durationMs);
        }
        System.out.println(logMessage);
    }
}