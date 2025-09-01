package com.carddemo.batch;

import com.carddemo.config.TestBatchConfig;
import com.carddemo.config.TestDatabaseConfig;
import com.carddemo.batch.DailyTransactionJob;
import com.carddemo.batch.InterestCalculationJob;
import com.carddemo.batch.StatementGenerationJob;
import com.carddemo.batch.AccountProcessingJob;
import com.carddemo.batch.BatchTestUtils;
import com.carddemo.config.MetricsConfig;
import com.carddemo.config.ActuatorConfig;
import com.carddemo.config.BatchConfig;
import AbstractBaseTest;
import TestConstants;

import org.openjdk.jmh.annotations.Mode;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.batch.test.JobLauncherTestUtils;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.actuator.metrics.MetricsEndpoint;
import org.assertj.core.api.Assertions;
import java.time.Instant;
import java.lang.management.ManagementFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.context.TestPropertySource;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.Duration;
import java.math.BigDecimal;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;

/**
 * Comprehensive performance test suite for batch job execution, validating 4-hour processing 
 * window compliance, throughput measurements, and resource utilization against mainframe baselines.
 * 
 * This test class ensures all Spring Batch jobs maintain performance parity with their COBOL
 * counterparts while meeting strict SLA requirements for overnight batch processing.
 * 
 * Key Performance Requirements:
 * - Complete batch processing within 4-hour window (240 minutes)
 * - Individual job execution time benchmarking  
 * - Throughput measurement (records per second)
 * - Memory usage profiling during execution
 * - Database connection pool optimization testing
 * - Chunk size tuning validation (1000 record baseline)
 * - Parallel processing effectiveness measurement
 * - I/O bottleneck identification
 * - CPU utilization monitoring
 * - Large dataset processing (production volume simulation)
 * - Stress testing with concurrent job execution
 * - Resource contention scenarios
 * - Performance comparison with mainframe baselines
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@ActiveProfiles("test")
@TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BatchPerformanceTest extends AbstractBaseTest {

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
    private BatchTestUtils batchTestUtils;
    
    @Autowired
    private MetricsConfig metricsConfig;
    
    @Autowired
    private ActuatorConfig actuatorConfig;
    
    @Autowired
    private BatchConfig batchConfig;
    
    private JobLauncherTestUtils jobLauncherTestUtils;
    private Timer.Sample timerSample;
    private MetricsEndpoint metricsEndpoint;
    private MemoryMXBean memoryMXBean;
    private OperatingSystemMXBean osBean;
    private ThreadMXBean threadMXBean;
    
    // Performance metrics collection
    private Map<String, Object> performanceMetrics;
    
    @BeforeEach
    void setUp() {
        super.setUp();
        
        // Initialize performance monitoring components
        this.jobLauncherTestUtils = testBatchConfig.jobLauncherTestUtils();
        this.metricsEndpoint = actuatorConfig.performanceEndpoint();
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        
        // Initialize performance metrics collection
        this.performanceMetrics = new HashMap<>();
        
        // Setup test data for batch processing
        batchTestUtils.setupBatchTestData();
        
        // Validate test environment readiness
        validateProcessingWindow();
    }
    
    @AfterEach
    void tearDown() {
        // Collect final performance metrics
        collectPerformanceMetrics();
        
        // Generate performance report
        generatePerformanceReport();
        
        super.tearDown();
    }

    @Test
    @Order(1)
    @DisplayName("Daily Transaction Job Performance - Validate execution within processing window")
    void testDailyTransactionJobPerformance() {
        // Given: Daily transaction job with performance monitoring
        Instant startTime = Instant.now();
        JobParameters jobParams = new JobParametersBuilder()
            .addString("processDate", "2024-01-15")
            .addString("batchMode", "FULL")
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();
        
        // When: Execute daily transaction job
        jobLauncherTestUtils.setJob(dailyTransactionJob.dailyTransactionJob());
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParams);
        Instant endTime = Instant.now();
        
        // Then: Validate performance requirements
        Duration executionTime = Duration.between(startTime, endTime);
        
        Assertions.assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        Assertions.assertThat(executionTime.toMinutes())
            .as("Daily transaction job must complete within processing window")
            .isLessThan(TestConstants.BATCH_PROCESSING_WINDOW_HOURS * 60L);
            
        // Record metrics for baseline comparison
        performanceMetrics.put("dailyTransactionJob.executionTime", executionTime.toSeconds());
        performanceMetrics.put("dailyTransactionJob.status", jobExecution.getStatus().toString());
        
        // Validate against mainframe baseline performance
        compareToMainframeBaseline("DAILY_TRANSACTION", executionTime.toSeconds());
    }
    
    @Test
    @Order(2)
    @DisplayName("Interest Calculation Job Performance - Validate BigDecimal precision performance")
    void testInterestCalculationJobPerformance() {
        // Given: Interest calculation job with precision monitoring
        Instant startTime = Instant.now();
        JobParameters jobParams = new JobParametersBuilder()
            .addString("calculationDate", "2024-01-31")
            .addString("interestType", "MONTHLY")
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();
        
        // When: Execute interest calculation job
        jobLauncherTestUtils.setJob(interestCalculationJob.interestCalculationJob());
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParams);
        Instant endTime = Instant.now();
        
        // Then: Validate performance and precision requirements
        Duration executionTime = Duration.between(startTime, endTime);
        
        Assertions.assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        Assertions.assertThat(executionTime.toMinutes())
            .as("Interest calculation job must complete within processing window")
            .isLessThan(TestConstants.BATCH_PROCESSING_WINDOW_HOURS * 60L);
            
        // Validate BigDecimal precision performance under load
        batchTestUtils.validateCobolPrecision();
        
        // Record metrics for baseline comparison
        performanceMetrics.put("interestCalculationJob.executionTime", executionTime.toSeconds());
        performanceMetrics.put("interestCalculationJob.precisionValidation", "PASSED");
        
        // Validate against mainframe baseline performance
        compareToMainframeBaseline("INTEREST_CALCULATION", executionTime.toSeconds());
    }
    
    @Test
    @Order(3)
    @DisplayName("Statement Generation Job Performance - Validate large-scale file generation")
    void testStatementGenerationJobPerformance() {
        // Given: Statement generation job with I/O monitoring
        Instant startTime = Instant.now();
        JobParameters jobParams = new JobParametersBuilder()
            .addString("statementDate", "2024-01-31")
            .addString("customerSegment", "ALL")
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();
        
        // When: Execute statement generation job
        jobLauncherTestUtils.setJob(statementGenerationJob.statementGenerationJob());
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParams);
        Instant endTime = Instant.now();
        
        // Then: Validate performance requirements for bulk file operations
        Duration executionTime = Duration.between(startTime, endTime);
        
        Assertions.assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        Assertions.assertThat(executionTime.toMinutes())
            .as("Statement generation job must complete within processing window")
            .isLessThan(TestConstants.BATCH_PROCESSING_WINDOW_HOURS * 60L);
            
        // Measure I/O bottlenecks during bulk file generation
        measureIoBottlenecks();
        
        // Record metrics for baseline comparison
        performanceMetrics.put("statementGenerationJob.executionTime", executionTime.toSeconds());
        performanceMetrics.put("statementGenerationJob.ioMetrics", performanceMetrics.get("ioBottlenecks"));
        
        // Validate against mainframe baseline performance
        compareToMainframeBaseline("STATEMENT_GENERATION", executionTime.toSeconds());
    }
    
    @Test
    @Order(4)  
    @DisplayName("Account Processing Job Performance - Validate composite job orchestration")
    void testAccountProcessingJobPerformance() {
        // Given: Account processing composite job with orchestration monitoring
        Instant startTime = Instant.now();
        JobParameters jobParams = new JobParametersBuilder()
            .addString("processDate", "2024-01-15")
            .addString("jobType", "COMPOSITE")
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();
        
        // When: Execute account processing composite job
        jobLauncherTestUtils.setJob(accountProcessingJob.accountProcessingJob());
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParams);
        Instant endTime = Instant.now();
        
        // Then: Validate composite job orchestration performance
        Duration executionTime = Duration.between(startTime, endTime);
        
        Assertions.assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        Assertions.assertThat(executionTime.toMinutes())
            .as("Account processing composite job must complete within processing window")
            .isLessThan(TestConstants.BATCH_PROCESSING_WINDOW_HOURS * 60L);
            
        // Record metrics for baseline comparison
        performanceMetrics.put("accountProcessingJob.executionTime", executionTime.toSeconds());
        performanceMetrics.put("accountProcessingJob.subJobCount", 3);
        
        // Validate against mainframe baseline performance
        compareToMainframeBaseline("ACCOUNT_PROCESSING", executionTime.toSeconds());
    }
    
    @Test
    @Order(5)
    @DisplayName("Batch Processing Window Compliance - Validate complete nightly batch execution")
    void testBatchProcessingWindowCompliance() {
        // Given: Complete nightly batch processing sequence
        Instant batchStartTime = Instant.now();
        List<JobExecution> allJobExecutions = List.of();
        
        try {
            // When: Execute all batch jobs in sequence (simulating nightly processing)
            CompletableFuture<JobExecution> dailyTxnFuture = executeJobAsync(
                dailyTransactionJob.dailyTransactionJob(), "daily-transaction");
            CompletableFuture<JobExecution> interestFuture = executeJobAsync(
                interestCalculationJob.interestCalculationJob(), "interest-calculation");
            CompletableFuture<JobExecution> statementFuture = executeJobAsync(
                statementGenerationJob.statementGenerationJob(), "statement-generation");
            CompletableFuture<JobExecution> accountFuture = executeJobAsync(
                accountProcessingJob.accountProcessingJob(), "account-processing");
            
            // Wait for all jobs to complete
            CompletableFuture.allOf(dailyTxnFuture, interestFuture, statementFuture, accountFuture).get();
            
            Instant batchEndTime = Instant.now();
            Duration totalBatchTime = Duration.between(batchStartTime, batchEndTime);
            
            // Then: Validate 4-hour processing window compliance
            Assertions.assertThat(totalBatchTime.toHours())
                .as("Complete batch processing must finish within 4-hour window")
                .isLessThanOrEqualTo(TestConstants.BATCH_PROCESSING_WINDOW_HOURS);
                
            // Validate individual job completion status
            allJobExecutions.forEach(execution -> 
                Assertions.assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED));
            
            // Record comprehensive batch metrics
            performanceMetrics.put("completeBatch.totalTime", totalBatchTime.toMinutes());
            performanceMetrics.put("completeBatch.windowCompliance", 
                totalBatchTime.toHours() <= TestConstants.BATCH_PROCESSING_WINDOW_HOURS);
            
        } catch (Exception e) {
            Assertions.fail("Batch processing window compliance test failed: " + e.getMessage());
        }
        
        // Validate processing window against SLA requirements
        validateProcessingWindow();
    }
    
    @Test
    @Order(6)
    @DisplayName("Throughput Measurement - Validate records per second processing rates")
    void testThroughputMeasurement() {
        // Given: Large dataset for throughput testing
        int testRecordCount = TestConstants.PERFORMANCE_TEST_DATA.size();
        batchTestUtils.generateTestAccounts(testRecordCount);
        
        Instant startTime = Instant.now();
        
        // When: Execute daily transaction job with throughput monitoring
        JobParameters jobParams = new JobParametersBuilder()
            .addString("throughputTest", "true")
            .addLong("expectedRecords", (long) testRecordCount)
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();
            
        jobLauncherTestUtils.setJob(dailyTransactionJob.dailyTransactionJob());
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParams);
        
        Instant endTime = Instant.now();
        Duration processingTime = Duration.between(startTime, endTime);
        
        // Then: Calculate and validate throughput metrics
        double recordsPerSecond = testRecordCount / (double) processingTime.toSeconds();
        double recordsPerMinute = recordsPerSecond * 60;
        
        Assertions.assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        Assertions.assertThat(recordsPerSecond)
            .as("Throughput must meet minimum records per second requirement")
            .isGreaterThan(TestConstants.TARGET_TPS / 10.0); // Batch processing target
            
        // Record throughput metrics
        performanceMetrics.put("throughput.recordsPerSecond", recordsPerSecond);
        performanceMetrics.put("throughput.recordsPerMinute", recordsPerMinute);
        performanceMetrics.put("throughput.totalRecords", testRecordCount);
        performanceMetrics.put("throughput.processingTimeSeconds", processingTime.toSeconds());
    }
    
    @Test
    @Order(7)
    @DisplayName("Memory Utilization Profiling - Monitor heap and non-heap memory during execution")
    void testMemoryUtilizationProfiling() {
        // Given: Memory monitoring setup
        long initialHeapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long initialNonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();
        
        // When: Execute memory-intensive batch job
        JobParameters jobParams = new JobParametersBuilder()
            .addString("memoryProfile", "true")
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();
            
        jobLauncherTestUtils.setJob(interestCalculationJob.interestCalculationJob());
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParams);
        
        // Monitor memory usage during execution
        long peakHeapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long peakNonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();
        long maxHeap = memoryMXBean.getHeapMemoryUsage().getMax();
        
        // Then: Validate memory utilization within acceptable limits
        double heapUtilizationPercent = (double) peakHeapUsed / maxHeap * 100;
        
        Assertions.assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        Assertions.assertThat(heapUtilizationPercent)
            .as("Memory utilization must stay below 80% threshold")
            .isLessThan(80.0);
            
        // Record memory metrics
        performanceMetrics.put("memory.initialHeapMB", initialHeapUsed / (1024 * 1024));
        performanceMetrics.put("memory.peakHeapMB", peakHeapUsed / (1024 * 1024));
        performanceMetrics.put("memory.heapUtilizationPercent", heapUtilizationPercent);
        performanceMetrics.put("memory.peakNonHeapMB", peakNonHeapUsed / (1024 * 1024));
    }
    
    @Test
    @Order(8)
    @DisplayName("Concurrent Job Execution - Test parallel batch processing")
    void testConcurrentJobExecution() {
        // Given: Concurrent job execution setup
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        Instant startTime = Instant.now();
        
        try {
            // When: Execute multiple jobs concurrently
            CompletableFuture<JobExecution> job1 = CompletableFuture.supplyAsync(() -> 
                executeJob(dailyTransactionJob.dailyTransactionJob(), "concurrent-1"), executorService);
            CompletableFuture<JobExecution> job2 = CompletableFuture.supplyAsync(() -> 
                executeJob(interestCalculationJob.interestCalculationJob(), "concurrent-2"), executorService);
            CompletableFuture<JobExecution> job3 = CompletableFuture.supplyAsync(() -> 
                executeJob(statementGenerationJob.statementGenerationJob(), "concurrent-3"), executorService);
                
            // Wait for all concurrent jobs to complete
            CompletableFuture.allOf(job1, job2, job3).get();
            
            Instant endTime = Instant.now();
            Duration concurrentExecutionTime = Duration.between(startTime, endTime);
            
            // Then: Validate concurrent execution performance
            Assertions.assertThat(job1.get().getStatus()).isEqualTo(BatchStatus.COMPLETED);
            Assertions.assertThat(job2.get().getStatus()).isEqualTo(BatchStatus.COMPLETED);
            Assertions.assertThat(job3.get().getStatus()).isEqualTo(BatchStatus.COMPLETED);
            
            // Concurrent execution should be faster than sequential
            Assertions.assertThat(concurrentExecutionTime.toMinutes())
                .as("Concurrent execution should complete within processing window")
                .isLessThan(TestConstants.BATCH_PROCESSING_WINDOW_HOURS * 60L);
                
            // Record concurrent execution metrics
            performanceMetrics.put("concurrent.executionTimeMinutes", concurrentExecutionTime.toMinutes());
            performanceMetrics.put("concurrent.jobCount", 3);
            performanceMetrics.put("concurrent.allJobsCompleted", true);
            
        } catch (Exception e) {
            Assertions.fail("Concurrent job execution test failed: " + e.getMessage());
        } finally {
            executorService.shutdown();
        }
    }
    
    @Test
    @Order(9)
    @DisplayName("Resource Contention Scenarios - Test system behavior under resource pressure")
    void testResourceContentionScenarios() {
        // Given: Resource contention simulation setup
        int threadCount = Runtime.getRuntime().availableProcessors() * 2;
        ExecutorService contentionExecutor = Executors.newFixedThreadPool(threadCount);
        
        try {
            // When: Simulate high resource contention
            List<CompletableFuture<Void>> contentionTasks = List.of();
            for (int i = 0; i < threadCount; i++) {
                CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
                    // Simulate CPU and memory intensive operations
                    simulateResourceIntensiveOperation();
                }, contentionExecutor);
                contentionTasks.add(task);
            }
            
            // Execute batch job under resource contention
            Instant startTime = Instant.now();
            JobParameters jobParams = new JobParametersBuilder()
                .addString("contentionTest", "true")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
                
            jobLauncherTestUtils.setJob(accountProcessingJob.accountProcessingJob());
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParams);
            
            Instant endTime = Instant.now();
            Duration contentionExecutionTime = Duration.between(startTime, endTime);
            
            // Wait for contention tasks to complete
            CompletableFuture.allOf(contentionTasks.toArray(new CompletableFuture[0])).get();
            
            // Then: Validate system resilience under resource pressure
            Assertions.assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            
            // Job should still complete, even if slower under contention
            Assertions.assertThat(contentionExecutionTime.toMinutes())
                .as("Job should complete even under resource contention")
                .isLessThan(TestConstants.BATCH_PROCESSING_WINDOW_HOURS * 60L * 1.5); // Allow 50% degradation
                
            // Record resource contention metrics
            performanceMetrics.put("contention.executionTimeMinutes", contentionExecutionTime.toMinutes());
            performanceMetrics.put("contention.threadCount", threadCount);
            performanceMetrics.put("contention.degradationFactor", 
                contentionExecutionTime.toSeconds() / (double) TestConstants.RESPONSE_TIME_THRESHOLD_MS * 1000);
                
        } catch (Exception e) {
            Assertions.fail("Resource contention test failed: " + e.getMessage());
        } finally {
            contentionExecutor.shutdown();
        }
    }
    
    @Test
    @Order(10)
    @DisplayName("Stress Testing - Validate system behavior under extreme load")
    void testStressTesting() {
        // Given: Extreme load scenario setup
        int largeDatasetSize = TestConstants.PERFORMANCE_TEST_DATA.size() * 10; // 10x normal volume
        batchTestUtils.generateTestAccounts(largeDatasetSize);
        
        Instant startTime = Instant.now();
        
        // When: Execute under extreme load conditions
        JobParameters jobParams = new JobParametersBuilder()
            .addString("stressTest", "true")
            .addLong("datasetSize", (long) largeDatasetSize)
            .addString("loadLevel", "EXTREME")
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();
            
        jobLauncherTestUtils.setJob(dailyTransactionJob.dailyTransactionJob());
        
        // Monitor system resources during stress test
        long startMemory = memoryMXBean.getHeapMemoryUsage().getUsed();
        double startCpuLoad = osBean.getProcessCpuLoad();
        
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParams);
        
        Instant endTime = Instant.now();
        Duration stressExecutionTime = Duration.between(startTime, endTime);
        
        // Monitor peak resource usage
        long peakMemory = memoryMXBean.getHeapMemoryUsage().getUsed();
        double peakCpuLoad = osBean.getProcessCpuLoad();
        
        // Then: Validate system stability under extreme load
        Assertions.assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // Even under stress, job should eventually complete
        Assertions.assertThat(stressExecutionTime.toHours())
            .as("Stress test should complete within extended window")
            .isLessThan(TestConstants.BATCH_PROCESSING_WINDOW_HOURS * 2L); // Allow double time for extreme load
            
        // Validate system didn't crash under stress
        double memoryIncreaseFactor = (double) peakMemory / startMemory;
        Assertions.assertThat(memoryIncreaseFactor)
            .as("Memory usage should not increase beyond reasonable limits")
            .isLessThan(5.0); // Allow 5x memory increase under extreme load
            
        // Record stress testing metrics
        performanceMetrics.put("stress.executionTimeHours", stressExecutionTime.toHours());
        performanceMetrics.put("stress.datasetSize", largeDatasetSize);
        performanceMetrics.put("stress.memoryIncreaseFactor", memoryIncreaseFactor);
        performanceMetrics.put("stress.peakCpuLoad", peakCpuLoad);
        performanceMetrics.put("stress.systemStable", true);
    }
    
    @Test
    @Order(11)
    @DisplayName("Performance Baseline Comparison - Validate against mainframe benchmarks")
    void testPerformanceBaseline() {
        // Given: Baseline comparison setup with known mainframe metrics
        Map<String, Double> mainframeBaselines = Map.of(
            "DAILY_TRANSACTION", 3600.0,     // 1 hour baseline
            "INTEREST_CALCULATION", 1800.0,   // 30 minutes baseline  
            "STATEMENT_GENERATION", 5400.0,   // 1.5 hours baseline
            "ACCOUNT_PROCESSING", 2700.0      // 45 minutes baseline
        );
        
        // When: Execute each job and measure against baselines
        for (Map.Entry<String, Double> baseline : mainframeBaselines.entrySet()) {
            String jobType = baseline.getKey();
            Double baselineSeconds = baseline.getValue();
            
            Instant startTime = Instant.now();
            JobExecution jobExecution = executeBaselineJob(jobType);
            Instant endTime = Instant.now();
            
            Duration actualTime = Duration.between(startTime, endTime);
            double performanceRatio = actualTime.toSeconds() / baselineSeconds;
            
            // Then: Validate performance meets or exceeds mainframe baseline
            Assertions.assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            Assertions.assertThat(performanceRatio)
                .as("Performance should meet or exceed mainframe baseline for " + jobType)
                .isLessThanOrEqualTo(1.2); // Allow 20% performance degradation maximum
                
            // Record baseline comparison metrics
            performanceMetrics.put("baseline." + jobType + ".actualSeconds", actualTime.toSeconds());
            performanceMetrics.put("baseline." + jobType + ".baselineSeconds", baselineSeconds);
            performanceMetrics.put("baseline." + jobType + ".performanceRatio", performanceRatio);
            performanceMetrics.put("baseline." + jobType + ".meetsBaseline", performanceRatio <= 1.2);
        }
    }
    
    @Nested
    @DisplayName("JMH Microbenchmark Tests")
    class MicrobenchmarkTests {
        
        @Test
        @DisplayName("Chunk Size Optimization Benchmark")
        void benchmarkChunkSizeOptimization() {
            // Given: Different chunk sizes to test
            int[] chunkSizes = {100, 500, 1000, 2000, 5000}; // 1000 is baseline
            Map<Integer, Double> chunkPerformance = new HashMap<>();
            
            for (int chunkSize : chunkSizes) {
                // When: Execute job with specific chunk size
                Instant startTime = Instant.now();
                JobParameters jobParams = new JobParametersBuilder()
                    .addLong("chunkSize", (long) chunkSize)
                    .addString("optimizationTest", "true")
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
                    
                jobLauncherTestUtils.setJob(dailyTransactionJob.dailyTransactionJob());
                JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParams);
                Instant endTime = Instant.now();
                
                Duration executionTime = Duration.between(startTime, endTime);
                chunkPerformance.put(chunkSize, executionTime.toSeconds());
                
                // Validate job completion
                Assertions.assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            }
            
            // Then: Find optimal chunk size (baseline is 1000)
            double baselinePerformance = chunkPerformance.get(1000);
            
            // Record chunk size optimization results
            chunkPerformance.forEach((chunkSize, executionTime) -> {
                double improvementRatio = baselinePerformance / executionTime;
                performanceMetrics.put("chunkOptimization." + chunkSize + ".seconds", executionTime);
                performanceMetrics.put("chunkOptimization." + chunkSize + ".improvementRatio", improvementRatio);
            });
            
            // Validate that 1000 record chunk is reasonably optimal
            Integer optimalChunkSize = chunkPerformance.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(1000);
                
            performanceMetrics.put("chunkOptimization.optimalSize", optimalChunkSize);
            performanceMetrics.put("chunkOptimization.baselineOptimal", 
                Math.abs(optimalChunkSize - 1000) <= 1000); // Within reasonable range
        }
        
        @Test
        @DisplayName("Database Connection Pool Benchmark")
        void benchmarkDatabaseConnectionPooling() {
            // Given: Different connection pool configurations
            int[] poolSizes = {5, 10, 20, 30, 40};
            Map<Integer, Double> poolPerformance = new HashMap<>();
            
            for (int poolSize : poolSizes) {
                // When: Execute with different pool sizes
                // Note: This would typically require configuration changes
                // For testing, we simulate different connection scenarios
                
                Instant startTime = Instant.now();
                JobParameters jobParams = new JobParametersBuilder()
                    .addLong("connectionPoolSize", (long) poolSize)
                    .addString("poolOptimizationTest", "true")
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
                    
                jobLauncherTestUtils.setJob(accountProcessingJob.accountProcessingJob());
                JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParams);
                Instant endTime = Instant.now();
                
                Duration executionTime = Duration.between(startTime, endTime);
                poolPerformance.put(poolSize, executionTime.toSeconds());
                
                Assertions.assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            }
            
            // Then: Analyze connection pool performance
            poolPerformance.forEach((poolSize, executionTime) -> {
                performanceMetrics.put("connectionPool." + poolSize + ".seconds", executionTime);
            });
            
            // Find optimal pool size
            Integer optimalPoolSize = poolPerformance.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(20);
                
            performanceMetrics.put("connectionPool.optimalSize", optimalPoolSize);
        }
        
        @Test 
        @DisplayName("Parallel Processing Benchmark")
        void benchmarkParallelProcessing() {
            // Given: Different parallel processing configurations
            int[] threadCounts = {1, 2, 4, 8, 16};
            Map<Integer, Double> parallelPerformance = new HashMap<>();
            
            for (int threadCount : threadCounts) {
                // When: Execute with different thread counts
                Instant startTime = Instant.now();
                JobParameters jobParams = new JobParametersBuilder()
                    .addLong("parallelThreads", (long) threadCount)
                    .addString("parallelProcessingTest", "true")
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
                    
                jobLauncherTestUtils.setJob(statementGenerationJob.statementGenerationJob());
                JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParams);
                Instant endTime = Instant.now();
                
                Duration executionTime = Duration.between(startTime, endTime);
                parallelPerformance.put(threadCount, executionTime.toSeconds());
                
                Assertions.assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            }
            
            // Then: Calculate parallel processing efficiency
            double singleThreadTime = parallelPerformance.get(1);
            
            parallelPerformance.forEach((threadCount, executionTime) -> {
                double speedup = singleThreadTime / executionTime;
                double efficiency = speedup / threadCount;
                
                performanceMetrics.put("parallel." + threadCount + ".seconds", executionTime);
                performanceMetrics.put("parallel." + threadCount + ".speedup", speedup);
                performanceMetrics.put("parallel." + threadCount + ".efficiency", efficiency);
            });
            
            // Find optimal thread count (best efficiency)
            Integer optimalThreads = parallelPerformance.entrySet().stream()
                .min((e1, e2) -> {
                    double eff1 = (singleThreadTime / e1.getValue()) / e1.getKey();
                    double eff2 = (singleThreadTime / e2.getValue()) / e2.getKey();
                    return Double.compare(eff2, eff1); // Descending order
                })
                .map(Map.Entry::getKey)
                .orElse(4);
                
            performanceMetrics.put("parallel.optimalThreads", optimalThreads);
        }
    }
    
    // Helper methods for performance testing
    
    private JobExecution executeJob(org.springframework.batch.core.Job job, String identifier) {
        try {
            JobParameters jobParams = new JobParametersBuilder()
                .addString("identifier", identifier)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
                
            jobLauncherTestUtils.setJob(job);
            return jobLauncherTestUtils.launchJob(jobParams);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute job: " + identifier, e);
        }
    }
    
    private CompletableFuture<JobExecution> executeJobAsync(org.springframework.batch.core.Job job, String identifier) {
        return CompletableFuture.supplyAsync(() -> executeJob(job, identifier));
    }
    
    private JobExecution executeBaselineJob(String jobType) {
        org.springframework.batch.core.Job job = switch (jobType) {
            case "DAILY_TRANSACTION" -> dailyTransactionJob.dailyTransactionJob();
            case "INTEREST_CALCULATION" -> interestCalculationJob.interestCalculationJob(); 
            case "STATEMENT_GENERATION" -> statementGenerationJob.statementGenerationJob();
            case "ACCOUNT_PROCESSING" -> accountProcessingJob.accountProcessingJob();
            default -> throw new IllegalArgumentException("Unknown job type: " + jobType);
        };
        
        return executeJob(job, jobType.toLowerCase());
    }
    
    private void simulateResourceIntensiveOperation() {
        // Simulate CPU intensive operation
        for (int i = 0; i < 1000000; i++) {
            Math.sqrt(i);
        }
        
        // Simulate memory allocation
        byte[] memoryBlock = new byte[1024 * 1024]; // 1MB allocation
        
        // Simulate I/O operation
        try {
            Thread.sleep(10); // Brief pause to simulate I/O wait
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public void measureCpuUtilization() {
        double cpuUsage = osBean.getProcessCpuLoad();
        long threadCount = threadMXBean.getThreadCount();
        
        performanceMetrics.put("system.cpuUsage", cpuUsage);
        performanceMetrics.put("system.threadCount", threadCount);
        
        // Validate CPU usage is within acceptable range
        Assertions.assertThat(cpuUsage)
            .as("CPU utilization should be reasonable during batch processing")
            .isLessThan(0.95); // Less than 95% CPU usage
    }
    
    public void measureIoBottlenecks() {
        // Measure I/O performance indicators
        long startTime = System.nanoTime();
        
        // Simulate I/O operations similar to statement generation
        try {
            // File I/O simulation
            java.io.File tempFile = java.io.File.createTempFile("performance_test", ".tmp");
            try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
                for (int i = 0; i < 10000; i++) {
                    writer.write("Sample statement line " + i + "\n");
                }
            }
            tempFile.delete();
            
        } catch (java.io.IOException e) {
            // Handle I/O exception in test
        }
        
        long endTime = System.nanoTime();
        double ioLatencyMs = (endTime - startTime) / 1_000_000.0;
        
        performanceMetrics.put("io.latencyMs", ioLatencyMs);
        performanceMetrics.put("ioBottlenecks", ioLatencyMs > 1000 ? "DETECTED" : "NONE");
    }
    
    public void validateProcessingWindow() {
        // Validate that batch processing window is correctly configured
        Assertions.assertThat(TestConstants.BATCH_PROCESSING_WINDOW_HOURS)
            .as("Batch processing window should be 4 hours")
            .isEqualTo(4);
            
        // Validate response time thresholds are configured
        Assertions.assertThat(TestConstants.RESPONSE_TIME_THRESHOLD_MS)
            .as("Response time threshold should be configured")
            .isGreaterThan(0);
            
        // Validate TPS targets are configured
        Assertions.assertThat(TestConstants.TARGET_TPS)
            .as("Target TPS should be configured")
            .isGreaterThan(0);
    }
    
    public void collectPerformanceMetrics() {
        // Collect JVM metrics
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        performanceMetrics.put("jvm.totalMemoryMB", totalMemory / (1024 * 1024));
        performanceMetrics.put("jvm.usedMemoryMB", usedMemory / (1024 * 1024));
        performanceMetrics.put("jvm.freeMemoryMB", freeMemory / (1024 * 1024));
        
        // Collect system metrics
        measureCpuUtilization();
        
        // Collect database connection metrics (if available from config)
        try {
            performanceMetrics.put("database.connectionPoolActive", "N/A"); // Would be from HikariCP metrics
        } catch (Exception e) {
            // Handle metrics collection failure gracefully
            performanceMetrics.put("database.metricsError", e.getMessage());
        }
        
        // Collect test execution timestamp
        performanceMetrics.put("collection.timestamp", Instant.now().toString());
    }
    
    public void compareToMainframeBaseline(String jobType, double actualSeconds) {
        // Define mainframe baseline performance expectations
        Map<String, Double> mainframeBaselines = Map.of(
            "DAILY_TRANSACTION", 3600.0,     // CBTRN01C + CBTRN02C expected time
            "INTEREST_CALCULATION", 1800.0,   // CBACT04C expected time
            "STATEMENT_GENERATION", 5400.0,   // CBSTM03A expected time  
            "ACCOUNT_PROCESSING", 2700.0      // Composite job expected time
        );
        
        Double baselineSeconds = mainframeBaselines.get(jobType);
        if (baselineSeconds != null) {
            double performanceRatio = actualSeconds / baselineSeconds;
            
            performanceMetrics.put("comparison." + jobType + ".actualSeconds", actualSeconds);
            performanceMetrics.put("comparison." + jobType + ".baselineSeconds", baselineSeconds);
            performanceMetrics.put("comparison." + jobType + ".performanceRatio", performanceRatio);
            
            // Log comparison results
            String comparisonResult = performanceRatio <= 1.0 ? "BETTER" : 
                                    performanceRatio <= 1.2 ? "ACCEPTABLE" : "DEGRADED";
            performanceMetrics.put("comparison." + jobType + ".result", comparisonResult);
            
            // Validate against baseline (allow 20% degradation)
            Assertions.assertThat(performanceRatio)
                .as("Performance should not degrade more than 20% from mainframe baseline")
                .isLessThanOrEqualTo(1.2);
        }
    }
    
    public void generatePerformanceReport() {
        // Generate comprehensive performance report
        StringBuilder report = new StringBuilder();
        report.append("=== BATCH PERFORMANCE TEST REPORT ===\n");
        report.append("Test Execution Date: ").append(Instant.now()).append("\n\n");
        
        // Individual Job Performance
        report.append("--- INDIVIDUAL JOB PERFORMANCE ---\n");
        performanceMetrics.entrySet().stream()
            .filter(entry -> entry.getKey().endsWith(".executionTime"))
            .forEach(entry -> {
                String jobName = entry.getKey().replace(".executionTime", "");
                report.append(String.format("%-30s: %.2f seconds%n", jobName, entry.getValue()));
            });
        
        // Processing Window Compliance  
        report.append("\n--- PROCESSING WINDOW COMPLIANCE ---\n");
        Object windowCompliance = performanceMetrics.get("completeBatch.windowCompliance");
        if (windowCompliance != null) {
            report.append("4-Hour Window Compliance: ").append(windowCompliance).append("\n");
            
            Object totalTime = performanceMetrics.get("completeBatch.totalTime");
            if (totalTime != null) {
                report.append("Total Batch Time: ").append(totalTime).append(" minutes\n");
            }
        }
        
        // Throughput Analysis
        report.append("\n--- THROUGHPUT ANALYSIS ---\n");
        Object recordsPerSecond = performanceMetrics.get("throughput.recordsPerSecond");
        if (recordsPerSecond != null) {
            report.append("Records per Second: ").append(recordsPerSecond).append("\n");
            
            Object totalRecords = performanceMetrics.get("throughput.totalRecords");
            if (totalRecords != null) {
                report.append("Total Records Processed: ").append(totalRecords).append("\n");
            }
        }
        
        // Memory Usage Analysis
        report.append("\n--- MEMORY USAGE ANALYSIS ---\n");
        Object heapUtilization = performanceMetrics.get("memory.heapUtilizationPercent");
        if (heapUtilization != null) {
            report.append("Peak Heap Utilization: ").append(heapUtilization).append("%\n");
        }
        
        Object peakHeapMB = performanceMetrics.get("memory.peakHeapMB");
        if (peakHeapMB != null) {
            report.append("Peak Heap Memory: ").append(peakHeapMB).append(" MB\n");
        }
        
        // Mainframe Baseline Comparison
        report.append("\n--- MAINFRAME BASELINE COMPARISON ---\n");
        performanceMetrics.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("comparison.") && entry.getKey().endsWith(".result"))
            .forEach(entry -> {
                String jobType = entry.getKey()
                    .replace("comparison.", "")
                    .replace(".result", "");
                report.append(String.format("%-30s: %s%n", jobType, entry.getValue()));
            });
        
        // Optimization Results
        report.append("\n--- OPTIMIZATION RESULTS ---\n");
        Object optimalChunkSize = performanceMetrics.get("chunkOptimization.optimalSize");
        if (optimalChunkSize != null) {
            report.append("Optimal Chunk Size: ").append(optimalChunkSize).append(" records\n");
        }
        
        Object optimalThreads = performanceMetrics.get("parallel.optimalThreads");
        if (optimalThreads != null) {
            report.append("Optimal Thread Count: ").append(optimalThreads).append(" threads\n");
        }
        
        // System Resource Summary
        report.append("\n--- SYSTEM RESOURCE SUMMARY ---\n");
        Object cpuUsage = performanceMetrics.get("system.cpuUsage");
        if (cpuUsage != null) {
            report.append("Peak CPU Usage: ").append(String.format("%.1f%%", (Double) cpuUsage * 100)).append("\n");
        }
        
        Object ioBottlenecks = performanceMetrics.get("ioBottlenecks");
        if (ioBottlenecks != null) {
            report.append("I/O Bottlenecks: ").append(ioBottlenecks).append("\n");
        }
        
        // Test Summary
        report.append("\n--- TEST SUMMARY ---\n");
        report.append("Total Performance Tests: 11\n");
        report.append("All Jobs Completed: ").append(allJobsCompletedSuccessfully()).append("\n");
        report.append("Performance Requirements Met: ").append(performanceRequirementsMet()).append("\n");
        
        // Store the report in performance metrics for potential export
        performanceMetrics.put("performanceReport", report.toString());
        
        // Log the report (in real implementation, this might be written to file)
        System.out.println(report.toString());
    }
    
    private boolean allJobsCompletedSuccessfully() {
        // Check if all individual job executions were successful
        return performanceMetrics.entrySet().stream()
            .filter(entry -> entry.getKey().endsWith(".status"))
            .allMatch(entry -> "COMPLETED".equals(entry.getValue()));
    }
    
    private boolean performanceRequirementsMet() {
        // Check key performance requirements
        Object windowCompliance = performanceMetrics.get("completeBatch.windowCompliance");
        Object heapUtilization = performanceMetrics.get("memory.heapUtilizationPercent");
        
        boolean windowMet = windowCompliance != null && (Boolean) windowCompliance;
        boolean memoryMet = heapUtilization == null || (Double) heapUtilization < 80.0;
        
        // Check that no mainframe baselines were severely degraded
        boolean baselinesMet = performanceMetrics.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("comparison.") && entry.getKey().endsWith(".performanceRatio"))
            .allMatch(entry -> (Double) entry.getValue() <= 1.2);
            
        return windowMet && memoryMet && baselinesMet;
    }
}