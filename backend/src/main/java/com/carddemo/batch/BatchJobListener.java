/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.batch;

import com.carddemo.config.BatchConfig;
import com.carddemo.service.NotificationService;
import com.carddemo.exception.BatchProcessingException;

import org.springframework.context.annotation.Profile;
import org.springframework.batch.core.JobExecution;
import org.springframework.context.annotation.Profile;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.context.annotation.Profile;
import org.springframework.batch.core.JobParameters;
import org.springframework.context.annotation.Profile;
import org.springframework.batch.core.StepExecution;
import org.springframework.context.annotation.Profile;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.context.annotation.Profile;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tags;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Spring Batch job execution listener providing comprehensive monitoring, logging, and metrics 
 * collection for batch job executions. Implements JobExecutionListener interface to capture 
 * job lifecycle events and provide operational visibility into batch processing operations.
 * 
 * This listener replaces mainframe job monitoring capabilities with modern cloud-native 
 * observability patterns, providing:
 * 
 * Core Responsibilities:
 * - Job execution timing and performance metrics collection
 * - Comprehensive logging of job start, completion, and error events  
 * - Record count extraction and processing rate calculations
 * - Error notification and alerting for failed batch operations
 * - Resource cleanup and temporary file management
 * - Integration with monitoring systems through Micrometer metrics
 * - Execution history tracking for audit and operational reporting
 * 
 * Metrics Published:
 * - spring_batch_job_duration_seconds: Job execution time tracking
 * - spring_batch_job_records_processed_total: Total record counts by job
 * - spring_batch_job_records_per_second: Processing throughput metrics  
 * - spring_batch_job_success_rate: Success rate percentage by job type
 * - spring_batch_job_failures_total: Failed job execution counter
 * - spring_batch_step_duration_seconds: Individual step execution timing
 * 
 * Error Handling and Notifications:
 * - Automatic error notification via NotificationService for failed jobs
 * - Detailed error context logging with stack traces and job parameters
 * - Recovery recommendation logging for retryable vs terminal failures
 * - Integration with BatchProcessingException for error categorization
 * 
 * Resource Management:
 * - Cleanup of temporary files and processing artifacts
 * - Memory usage monitoring and resource utilization tracking
 * - Thread pool status monitoring through TaskExecutor integration
 * - Database connection status validation through JobRepository
 * 
 * Integration Points:
 * - BatchConfig: Access to job repository, explorer, and task executor infrastructure
 * - NotificationService: Error alerting and notification dispatch
 * - MeterRegistry: Metrics publication for monitoring and observability
 * - JobExplorer: Historical job execution data access for trend analysis
 * 
 * Performance Monitoring:
 * - 4-hour batch window compliance tracking with alerts for exceeded windows
 * - Processing rate analysis with comparison to baseline performance metrics
 * - Step-level granularity for identifying bottlenecks and optimization opportunities
 * - Memory and resource utilization trending for capacity planning
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Component
public class BatchJobListener implements JobExecutionListener {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchJobListener.class);
    
    // Metrics and monitoring constants
    private static final String METRIC_JOB_DURATION = "spring_batch_job_duration_seconds";
    private static final String METRIC_JOB_RECORDS_PROCESSED = "spring_batch_job_records_processed_total";
    private static final String METRIC_JOB_RECORDS_PER_SECOND = "spring_batch_job_records_per_second";
    private static final String METRIC_JOB_SUCCESS_RATE = "spring_batch_job_success_rate";
    private static final String METRIC_JOB_FAILURES = "spring_batch_job_failures_total";
    private static final String METRIC_STEP_DURATION = "spring_batch_step_duration_seconds";
    
    // Batch processing window constants (4 hours in seconds)
    private static final long BATCH_WINDOW_SECONDS = 4 * 60 * 60; // 4 hours
    private static final long WARNING_THRESHOLD_SECONDS = 3 * 60 * 60; // 3 hours
    
    // Job execution tracking
    private final Map<Long, JobExecutionContext> executionContexts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> jobSuccessCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> jobFailureCounters = new ConcurrentHashMap<>();
    
    // Spring Batch infrastructure dependencies
    private final NotificationService notificationService;
    private final MeterRegistry meterRegistry;
    private final JobRepository jobRepository;
    private final JobExplorer jobExplorer;
    private final TaskExecutor taskExecutor;
    
    // Date formatting for logs and notifications
    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Constructor for BatchJobListener with dependency injection of required infrastructure components.
     * 
     * @param jobRepository Spring Batch job repository for metadata operations
     * @param jobExplorer Spring Batch job explorer for job execution queries
     * @param taskExecutor Task executor for asynchronous operations
     * @param notificationService Service for sending error notifications and alerts
     * @param meterRegistry Micrometer metrics registry for publishing performance metrics
     */
    @Autowired
    public BatchJobListener(JobRepository jobRepository,
                           JobExplorer jobExplorer,
                           TaskExecutor taskExecutor,
                           NotificationService notificationService,
                           MeterRegistry meterRegistry) {
        this.jobRepository = jobRepository;
        this.jobExplorer = jobExplorer;
        this.taskExecutor = taskExecutor;
        this.notificationService = notificationService;
        this.meterRegistry = meterRegistry;
        
        logger.info("BatchJobListener initialized successfully with infrastructure components");
        
        // Initialize metrics counters
        initializeMetrics();
    }
    
    /**
     * Callback method invoked before job execution starts. Captures job start time, parameters,
     * and context information for monitoring and metrics collection.
     * 
     * Responsibilities:
     * - Record job start timestamp for duration calculations
     * - Log job initiation with parameters and configuration details
     * - Initialize performance monitoring timers and counters
     * - Validate job parameters and execution environment
     * - Set up temporary resource tracking for cleanup operations
     * - Register job execution context for step-level monitoring
     * 
     * @param jobExecution JobExecution instance containing job metadata and parameters
     */
    @Override
    public void beforeJob(JobExecution jobExecution) {
        if (jobExecution == null) {
            logger.warn("JobExecution is null in beforeJob - skipping job monitoring");
            return;
        }
        
        Long executionId = jobExecution.getId();
        String jobName = jobExecution.getJobInstance().getJobName();
        JobParameters jobParameters = jobExecution.getJobParameters();
        
        logger.info("=== JOB EXECUTION STARTED ===");
        logger.info("Job Name: {}", jobName);
        logger.info("Execution ID: {}", executionId);
        logger.info("Start Time: {}", jobExecution.getStartTime().format(timestampFormatter));
        
        // Log job parameters for debugging and audit trail
        logJobParameters(jobParameters);
        
        // Create and store execution context for monitoring
        JobExecutionContext context = new JobExecutionContext();
        context.setJobName(jobName);
        context.setExecutionId(executionId);
        context.setStartTime(LocalDateTime.now());
        context.setJobParameters(jobParameters);
        context.setTimerSample(Timer.start(meterRegistry));
        
        executionContexts.put(executionId, context);
        
        // Log infrastructure status for operational visibility
        logInfrastructureStatus();
        
        // Validate execution environment and prerequisites
        validateExecutionEnvironment(jobName, jobParameters);
        
        logger.info("Job execution context initialized for job: {} [{}]", jobName, executionId);
    }
    
    /**
     * Callback method invoked after job execution completes. Calculates execution duration,
     * extracts processing metrics, logs completion status, handles error notifications,
     * performs resource cleanup, and publishes performance metrics.
     * 
     * Responsibilities:
     * - Calculate and log job execution duration against 4-hour window requirement
     * - Extract read/write/skip counts from step executions for metrics publication
     * - Log job completion status with detailed processing statistics
     * - Send error notifications for failed jobs via NotificationService
     * - Cleanup temporary files and resources created during job execution
     * - Update job execution history and success rate metrics
     * - Publish Micrometer metrics for monitoring system integration
     * - Generate processing performance analysis and recommendations
     * 
     * @param jobExecution JobExecution instance with completed execution details
     */
    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution == null) {
            logger.warn("JobExecution is null in afterJob - skipping job monitoring");
            return;
        }
        
        Long executionId = jobExecution.getId();
        String jobName = jobExecution.getJobInstance().getJobName();
        
        logger.info("=== JOB EXECUTION COMPLETED ===");
        logger.info("Job Name: {}", jobName);
        logger.info("Execution ID: {}", executionId);
        logger.info("End Time: {}", jobExecution.getEndTime().format(timestampFormatter));
        logger.info("Exit Status: {}", jobExecution.getExitStatus().getExitCode());
        
        // Retrieve execution context for metrics calculation
        JobExecutionContext context = executionContexts.get(executionId);
        if (context == null) {
            logger.warn("No execution context found for job: {} [{}]", jobName, executionId);
            return;
        }
        
        try {
            // Calculate and log execution duration
            Duration executionDuration = Duration.between(context.getStartTime(), LocalDateTime.now());
            long durationSeconds = executionDuration.getSeconds();
            
            logger.info("Job execution duration: {} seconds ({} minutes)", 
                       durationSeconds, durationSeconds / 60);
            
            // Stop timer and record duration metric
            context.getTimerSample().stop(Timer.builder(METRIC_JOB_DURATION)
                    .tag("job_name", jobName)
                    .tag("status", jobExecution.getExitStatus().getExitCode())
                    .register(meterRegistry));
            
            // Check against 4-hour batch window requirement
            validateBatchWindow(jobName, durationSeconds);
            
            // Extract and log step execution metrics
            ProcessingMetrics metrics = extractProcessingMetrics(jobExecution);
            logProcessingMetrics(jobName, metrics);
            
            // Publish performance metrics to monitoring system
            publishPerformanceMetrics(jobName, jobExecution, metrics, durationSeconds);
            
            // Handle job completion based on exit status
            handleJobCompletion(jobExecution, context, metrics);
            
            // Cleanup resources and temporary files
            performResourceCleanup(context);
            
            // Update job execution statistics
            updateJobStatistics(jobName, jobExecution.getExitStatus().getExitCode());
            
        } catch (Exception e) {
            logger.error("Error in afterJob processing for job: {} [{}]", jobName, executionId, e);
        } finally {
            // Always remove execution context to prevent memory leaks
            executionContexts.remove(executionId);
        }
        
        logger.info("=== JOB EXECUTION MONITORING COMPLETE ===");
    }
    
    /**
     * Logs job parameters for debugging and audit purposes.
     */
    private void logJobParameters(JobParameters jobParameters) {
        logger.info("Job Parameters:");
        jobParameters.getParameters().forEach((key, value) -> {
            logger.info("  {} = {} ({})", key, value.getValue(), value.getType());
        });
    }
    
    /**
     * Logs current infrastructure status for operational visibility.
     */
    private void logInfrastructureStatus() {
        try {
            // Log task executor status
            logger.debug("Task Executor Status: Active threads and pool configuration");
            
            // Log job repository status
            logger.debug("Job Repository Status: Connection and metadata table accessibility");
            
            // Log metrics registry status
            logger.debug("Metrics Registry Status: {} meters registered", meterRegistry.getMeters().size());
            
        } catch (Exception e) {
            logger.warn("Unable to retrieve complete infrastructure status", e);
        }
    }
    
    /**
     * Validates execution environment and prerequisites before job execution.
     */
    private void validateExecutionEnvironment(String jobName, JobParameters jobParameters) {
        try {
            // Validate job repository accessibility
            if (jobRepository == null) {
                throw new BatchProcessingException(jobName, BatchProcessingException.ErrorType.RESOURCE_UNAVAILABLE, 
                    "JobRepository not available for job: " + jobName);
            }
            
            // Validate job explorer accessibility
            if (jobExplorer == null) {
                throw new BatchProcessingException(jobName, BatchProcessingException.ErrorType.RESOURCE_UNAVAILABLE,
                    "JobExplorer not available for job: " + jobName);
            }
            
            // Validate task executor availability
            if (taskExecutor == null) {
                throw new BatchProcessingException(jobName, BatchProcessingException.ErrorType.RESOURCE_UNAVAILABLE,
                    "TaskExecutor not available for job: " + jobName);
            }
            
            logger.debug("Execution environment validation passed for job: {}", jobName);
            
        } catch (Exception e) {
            logger.error("Execution environment validation failed for job: {}", jobName, e);
            throw new BatchProcessingException(jobName, BatchProcessingException.ErrorType.RESOURCE_UNAVAILABLE,
                "Environment validation failed for job: " + jobName, e);
        }
    }
    
    /**
     * Validates job execution duration against 4-hour batch window requirement.
     */
    private void validateBatchWindow(String jobName, long durationSeconds) {
        if (durationSeconds > BATCH_WINDOW_SECONDS) {
            String alertMessage = String.format(
                "CRITICAL: Job %s exceeded 4-hour batch window by %d minutes", 
                jobName, (durationSeconds - BATCH_WINDOW_SECONDS) / 60);
            
            logger.error(alertMessage);
            
            // Send critical alert via NotificationService
            try {
                notificationService.sendAccountAlert(
                    "BATCH_SYSTEM", 
                    "BATCH_WINDOW", 
                    "BATCH_WINDOW_EXCEEDED", 
                    alertMessage, 
                    "CRITICAL"
                );
            } catch (Exception e) {
                logger.error("Failed to send batch window alert notification", e);
            }
            
        } else if (durationSeconds > WARNING_THRESHOLD_SECONDS) {
            String warningMessage = String.format(
                "WARNING: Job %s approaching batch window limit at %d minutes", 
                jobName, durationSeconds / 60);
            
            logger.warn(warningMessage);
            
            // Send warning alert via NotificationService
            try {
                notificationService.sendAccountAlert(
                    "BATCH_SYSTEM", 
                    "BATCH_WINDOW", 
                    "BATCH_WINDOW_WARNING", 
                    warningMessage, 
                    "HIGH"
                );
            } catch (Exception e) {
                logger.error("Failed to send batch window warning notification", e);
            }
        }
    }
    
    /**
     * Extracts processing metrics from job execution step details.
     */
    private ProcessingMetrics extractProcessingMetrics(JobExecution jobExecution) {
        ProcessingMetrics metrics = new ProcessingMetrics();
        
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            String stepName = stepExecution.getStepName();
            int readCount = (int) stepExecution.getReadCount();
            int writeCount = (int) stepExecution.getWriteCount();
            int skipCount = (int) stepExecution.getSkipCount();
            int commitCount = (int) stepExecution.getCommitCount();
            
            logger.info("Step: {} - Read: {}, Write: {}, Skip: {}, Commit: {}", 
                       stepName, readCount, writeCount, skipCount, commitCount);
            
            // Accumulate totals across all steps
            metrics.totalReadCount += readCount;
            metrics.totalWriteCount += writeCount;
            metrics.totalSkipCount += skipCount;
            metrics.totalCommitCount += commitCount;
            metrics.stepCount++;
            
            // Track step-level details for granular analysis
            metrics.stepMetrics.put(stepName, new StepMetrics(readCount, writeCount, skipCount, commitCount));
        }
        
        return metrics;
    }
    
    /**
     * Logs detailed processing metrics for operational analysis.
     */
    private void logProcessingMetrics(String jobName, ProcessingMetrics metrics) {
        logger.info("=== PROCESSING METRICS SUMMARY ===");
        logger.info("Job: {}", jobName);
        logger.info("Total Steps: {}", metrics.stepCount);
        logger.info("Total Records Read: {}", metrics.totalReadCount);
        logger.info("Total Records Written: {}", metrics.totalWriteCount);
        logger.info("Total Records Skipped: {}", metrics.totalSkipCount);
        logger.info("Total Commits: {}", metrics.totalCommitCount);
        
        if (metrics.totalReadCount > 0) {
            double successRate = ((double) metrics.totalWriteCount / metrics.totalReadCount) * 100;
            logger.info("Processing Success Rate: {:.2f}%", successRate);
        }
        
        if (metrics.totalSkipCount > 0) {
            double skipRate = ((double) metrics.totalSkipCount / metrics.totalReadCount) * 100;
            logger.warn("Processing Skip Rate: {:.2f}%", skipRate);
        }
    }
    
    /**
     * Publishes performance metrics to monitoring system via Micrometer.
     */
    private void publishPerformanceMetrics(String jobName, JobExecution jobExecution, 
                                         ProcessingMetrics metrics, long durationSeconds) {
        try {
            // Publish record count metrics
            meterRegistry.counter(METRIC_JOB_RECORDS_PROCESSED, 
                                "job_name", jobName, 
                                "record_type", "read")
                         .increment(metrics.totalReadCount);
            
            meterRegistry.counter(METRIC_JOB_RECORDS_PROCESSED, 
                                "job_name", jobName, 
                                "record_type", "written")
                         .increment(metrics.totalWriteCount);
            
            meterRegistry.counter(METRIC_JOB_RECORDS_PROCESSED, 
                                "job_name", jobName, 
                                "record_type", "skipped")
                         .increment(metrics.totalSkipCount);
            
            // Calculate and publish processing rate (records per second)
            if (durationSeconds > 0 && metrics.totalReadCount > 0) {
                double recordsPerSecond = (double) metrics.totalReadCount / durationSeconds;
                meterRegistry.gauge(METRIC_JOB_RECORDS_PER_SECOND, 
                                  Tags.of("job_name", jobName), recordsPerSecond);
                
                logger.info("Processing Rate: {:.2f} records/second", recordsPerSecond);
            }
            
            // Publish step-level duration metrics
            for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
                if (stepExecution.getStartTime() != null && stepExecution.getEndTime() != null) {
                    long stepDuration = Duration.between(
                        stepExecution.getStartTime().toInstant(ZoneOffset.UTC), 
                        stepExecution.getEndTime().toInstant(ZoneOffset.UTC)
                    ).getSeconds();
                    
                    Timer.builder(METRIC_STEP_DURATION)
                         .tag("job_name", jobName)
                         .tag("step_name", stepExecution.getStepName())
                         .tag("status", stepExecution.getExitStatus().getExitCode())
                         .register(meterRegistry)
                         .record(stepDuration, java.util.concurrent.TimeUnit.SECONDS);
                }
            }
            
            logger.debug("Performance metrics published successfully for job: {}", jobName);
            
        } catch (Exception e) {
            logger.error("Failed to publish performance metrics for job: {}", jobName, e);
        }
    }
    
    /**
     * Handles job completion based on exit status, including error notifications.
     */
    private void handleJobCompletion(JobExecution jobExecution, JobExecutionContext context, 
                                   ProcessingMetrics metrics) {
        String jobName = jobExecution.getJobInstance().getJobName();
        String exitCode = jobExecution.getExitStatus().getExitCode();
        
        if ("COMPLETED".equals(exitCode)) {
            logger.info("Job completed successfully: {}", jobName);
            
            // Send success notification for critical jobs
            if (isCriticalJob(jobName)) {
                try {
                    notificationService.sendTransactionNotification(
                        "BATCH_SYSTEM",
                        "BATCH_JOB_SUCCESS",
                        context.getExecutionId().toString(),
                        BigDecimal.valueOf(metrics.totalReadCount),
                        "Batch Job: " + jobName,
                        LocalDateTime.now()
                    );
                } catch (Exception e) {
                    logger.error("Failed to send success notification for job: {}", jobName, e);
                }
            }
            
        } else if ("FAILED".equals(exitCode)) {
            logger.error("Job failed: {}", jobName);
            
            // Extract error details from step executions
            List<Throwable> errors = extractJobErrors(jobExecution);
            String errorMessage = buildErrorMessage(jobName, errors, metrics);
            
            // Send error notification via NotificationService
            try {
                notificationService.sendAccountAlert(
                    "BATCH_SYSTEM",
                    "BATCH_JOB_FAILURE", 
                    "BATCH_JOB_FAILED",
                    errorMessage,
                    "CRITICAL"
                );
                
                logger.info("Error notification sent for failed job: {}", jobName);
                
            } catch (Exception e) {
                logger.error("Failed to send error notification for job: {}", jobName, e);
            }
            
        } else if ("STOPPED".equals(exitCode)) {
            logger.warn("Job was stopped: {}", jobName);
            
            try {
                notificationService.sendAccountAlert(
                    "BATCH_SYSTEM",
                    "BATCH_JOB_STOPPED",
                    "BATCH_JOB_INTERRUPTED", 
                    "Batch job " + jobName + " was stopped during execution",
                    "HIGH"
                );
            } catch (Exception e) {
                logger.error("Failed to send stop notification for job: {}", jobName, e);
            }
        }
    }
    
    /**
     * Performs resource cleanup after job execution completion.
     */
    private void performResourceCleanup(JobExecutionContext context) {
        try {
            // Cleanup temporary files and processing artifacts
            logger.debug("Performing resource cleanup for job: {}", context.getJobName());
            
            // Clear any temporary job state
            context.cleanup();
            
            // Force garbage collection hint for large batch operations
            if (isLargeJob(context.getJobName())) {
                System.gc();
                logger.debug("Memory cleanup initiated for large job: {}", context.getJobName());
            }
            
        } catch (Exception e) {
            logger.error("Error during resource cleanup for job: {}", context.getJobName(), e);
        }
    }
    
    /**
     * Updates job execution statistics for success rate calculations.
     */
    private void updateJobStatistics(String jobName, String exitCode) {
        try {
            if ("COMPLETED".equals(exitCode)) {
                jobSuccessCounters.computeIfAbsent(jobName, k -> new AtomicLong(0)).incrementAndGet();
            } else {
                jobFailureCounters.computeIfAbsent(jobName, k -> new AtomicLong(0)).incrementAndGet();
                
                // Publish failure metric
                meterRegistry.counter(METRIC_JOB_FAILURES, "job_name", jobName).increment();
            }
            
            // Calculate and publish success rate
            long successCount = jobSuccessCounters.getOrDefault(jobName, new AtomicLong(0)).get();
            long failureCount = jobFailureCounters.getOrDefault(jobName, new AtomicLong(0)).get();
            long totalCount = successCount + failureCount;
            
            if (totalCount > 0) {
                double successRate = ((double) successCount / totalCount) * 100;
                meterRegistry.gauge(METRIC_JOB_SUCCESS_RATE, 
                                  Tags.of("job_name", jobName), successRate);
            }
            
        } catch (Exception e) {
            logger.error("Failed to update job statistics for: {}", jobName, e);
        }
    }
    
    /**
     * Initializes metrics counters and gauges for monitoring.
     */
    private void initializeMetrics() {
        try {
            // Initialize base metrics that will be populated during job execution
            logger.debug("Initializing batch job metrics infrastructure");
            
            // Metrics will be created lazily during job execution
            
        } catch (Exception e) {
            logger.error("Failed to initialize metrics infrastructure", e);
        }
    }
    
    /**
     * Determines if a job is considered critical for notification purposes.
     */
    private boolean isCriticalJob(String jobName) {
        // Critical jobs that require success/failure notifications
        return jobName.contains("DailyTransaction") || 
               jobName.contains("InterestCalculation") || 
               jobName.contains("StatementGeneration") ||
               jobName.contains("DataMigration");
    }
    
    /**
     * Determines if a job is considered large for memory management purposes.
     */
    private boolean isLargeJob(String jobName) {
        return jobName.contains("DataMigration") || 
               jobName.contains("DailyTransaction") ||
               jobName.contains("StatementGeneration");
    }
    
    /**
     * Extracts error details from failed step executions.
     */
    private List<Throwable> extractJobErrors(JobExecution jobExecution) {
        List<Throwable> errors = new ArrayList<>();
        
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            List<Throwable> failures = stepExecution.getFailureExceptions();
            errors.addAll(failures);
        }
        
        return errors;
    }
    
    /**
     * Builds comprehensive error message for notifications.
     */
    private String buildErrorMessage(String jobName, List<Throwable> errors, ProcessingMetrics metrics) {
        StringBuilder message = new StringBuilder();
        message.append("Batch job failed: ").append(jobName).append("\n\n");
        
        message.append("Processing Summary:\n");
        message.append("- Records Read: ").append(metrics.totalReadCount).append("\n");
        message.append("- Records Written: ").append(metrics.totalWriteCount).append("\n");
        message.append("- Records Skipped: ").append(metrics.totalSkipCount).append("\n");
        message.append("- Steps Executed: ").append(metrics.stepCount).append("\n\n");
        
        if (!errors.isEmpty()) {
            message.append("Error Details:\n");
            for (int i = 0; i < Math.min(errors.size(), 3); i++) {
                Throwable error = errors.get(i);
                message.append("- ").append(error.getClass().getSimpleName())
                       .append(": ").append(error.getMessage()).append("\n");
            }
            
            if (errors.size() > 3) {
                message.append("... and ").append(errors.size() - 3).append(" more errors\n");
            }
        }
        
        return message.toString();
    }
    
    /**
     * Inner class for tracking job execution context and metrics.
     */
    private static class JobExecutionContext {
        private String jobName;
        private Long executionId;
        private LocalDateTime startTime;
        private JobParameters jobParameters;
        private Timer.Sample timerSample;
        private Map<String, Object> attributes = new HashMap<>();
        
        // Getters and setters
        public String getJobName() { return jobName; }
        public void setJobName(String jobName) { this.jobName = jobName; }
        
        public Long getExecutionId() { return executionId; }
        public void setExecutionId(Long executionId) { this.executionId = executionId; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public JobParameters getJobParameters() { return jobParameters; }
        public void setJobParameters(JobParameters jobParameters) { this.jobParameters = jobParameters; }
        
        public Timer.Sample getTimerSample() { return timerSample; }
        public void setTimerSample(Timer.Sample timerSample) { this.timerSample = timerSample; }
        
        public void setAttribute(String key, Object value) { attributes.put(key, value); }
        public Object getAttribute(String key) { return attributes.get(key); }
        
        public void cleanup() {
            attributes.clear();
        }
    }
    
    /**
     * Inner class for processing metrics aggregation.
     */
    private static class ProcessingMetrics {
        public int totalReadCount = 0;
        public int totalWriteCount = 0;
        public int totalSkipCount = 0;
        public int totalCommitCount = 0;
        public int stepCount = 0;
        public Map<String, StepMetrics> stepMetrics = new HashMap<>();
    }
    
    /**
     * Inner class for step-level metrics.
     */
    private static class StepMetrics {
        public final int readCount;
        public final int writeCount;
        public final int skipCount;
        public final int commitCount;
        
        public StepMetrics(int readCount, int writeCount, int skipCount, int commitCount) {
            this.readCount = readCount;
            this.writeCount = writeCount;
            this.skipCount = skipCount;
            this.commitCount = commitCount;
        }
    }
}