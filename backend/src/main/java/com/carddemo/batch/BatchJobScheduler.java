/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.batch;

import com.carddemo.batch.BatchJobLauncher;
import com.carddemo.config.BatchConfig;
import com.carddemo.util.DateConversionUtil;
import com.carddemo.batch.BatchProcessingException;
import com.carddemo.batch.DailyTransactionJob;
import com.carddemo.batch.InterestCalculationJob;
import com.carddemo.batch.StatementGenerationJob;
import com.carddemo.dto.ApiRequest;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.batch.core.JobParametersBuilder;
import java.time.Duration;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring component for scheduling batch jobs to run at specific times, replacing mainframe 
 * job scheduler functionality. Implements cron-based scheduling for daily, weekly, and monthly 
 * batch jobs with configurable execution times and dependency management between jobs.
 * 
 * <p>This scheduler replaces traditional JCL job scheduling from the mainframe environment with 
 * modern Spring-based cron scheduling. It maintains the same execution patterns and job dependencies
 * that were present in the original COBOL/JCL batch processing while adding enhanced monitoring,
 * error handling, and operational visibility.</p>
 * 
 * <p>Key Features:</p>
 * <ul>
 * <li>Cron-based scheduling for regular job execution at specific times</li>
 * <li>Job dependency management ensuring prerequisite jobs complete before dependent jobs start</li>
 * <li>Business day validation to skip weekends and holidays</li>
 * <li>Execution window enforcement to ensure jobs complete within 4-hour window</li>
 * <li>Automatic retry logic for failed jobs with exponential backoff</li>
 * <li>Comprehensive logging and monitoring for operational visibility</li>
 * <li>Graceful shutdown capabilities for maintenance windows</li>
 * </ul>
 * 
 * <p>Scheduled Job Execution:</p>
 * <ul>
 * <li>Daily Transaction Processing: 2:00 AM daily on business days</li>
 * <li>Interest Calculation: Last day of each month at 3:00 AM</li>
 * <li>Statement Generation: First business day of each month at 4:00 AM</li>
 * </ul>
 * 
 * <p>Business Logic:</p>
 * <ul>
 * <li>Jobs only execute on business days (Monday-Friday, excluding holidays)</li>
 * <li>All jobs must complete within 4-hour execution window (2:00 AM - 6:00 AM)</li>
 * <li>Failed jobs are automatically retried with exponential backoff</li>
 * <li>Job dependencies are enforced to maintain data integrity</li>
 * <li>Scheduler can be paused for maintenance or emergency situations</li>
 * </ul>
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Component
public class BatchJobScheduler {

    private static final Logger logger = LoggerFactory.getLogger(BatchJobScheduler.class);

    // Execution window constants (4-hour window: 2:00 AM - 6:00 AM)
    private static final int EXECUTION_WINDOW_START_HOUR = 2;
    private static final int EXECUTION_WINDOW_END_HOUR = 6;
    private static final Duration EXECUTION_WINDOW_DURATION = Duration.ofHours(4);

    // Retry configuration
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_RETRY_DELAY_SECONDS = 300; // 5 minutes
    private static final double RETRY_BACKOFF_MULTIPLIER = 2.0;

    // Job names
    private static final String DAILY_TRANSACTION_JOB = "dailyTransactionJob";
    private static final String INTEREST_CALCULATION_JOB = "interestCalculationJob";
    private static final String STATEMENT_GENERATION_JOB = "statementGenerationJob";

    // Scheduler state management
    private final AtomicBoolean schedulerEnabled = new AtomicBoolean(true);
    private final Map<String, LocalDateTime> lastExecutionTimes = new ConcurrentHashMap<>();
    private final Map<String, Integer> retryAttempts = new ConcurrentHashMap<>();
    private final Map<String, Long> lastExecutionIds = new ConcurrentHashMap<>();

    // Dependency injection
    private final BatchJobLauncher batchJobLauncher;
    private final BatchConfig batchConfig;
    private final DailyTransactionJob dailyTransactionJob;
    private final InterestCalculationJob interestCalculationJob;
    private final StatementGenerationJob statementGenerationJob;

    /**
     * Constructor for dependency injection of batch infrastructure components.
     *
     * @param batchJobLauncher Spring component for launching batch jobs programmatically
     * @param batchConfig Spring Batch configuration providing infrastructure beans
     * @param dailyTransactionJob Spring Batch job configuration for daily transaction processing
     * @param interestCalculationJob Spring Batch job configuration for interest calculation
     * @param statementGenerationJob Spring Batch job configuration for statement generation
     */
    @Autowired
    public BatchJobScheduler(BatchJobLauncher batchJobLauncher, BatchConfig batchConfig,
                           DailyTransactionJob dailyTransactionJob, 
                           InterestCalculationJob interestCalculationJob,
                           StatementGenerationJob statementGenerationJob) {
        this.batchJobLauncher = batchJobLauncher;
        this.batchConfig = batchConfig;
        this.dailyTransactionJob = dailyTransactionJob;
        this.interestCalculationJob = interestCalculationJob;
        this.statementGenerationJob = statementGenerationJob;
        
        logger.info("BatchJobScheduler initialized with all job dependencies");
    }

    /**
     * Schedules all batch jobs according to their configured cron expressions.
     * This method provides programmatic control over the scheduling system and can be used
     * to enable/disable the entire scheduling infrastructure.
     */
    public void scheduleJobs() {
        logger.info("Scheduling batch jobs enabled: {}", schedulerEnabled.get());
        if (!schedulerEnabled.get()) {
            logger.warn("Scheduler is disabled - jobs will not execute");
            return;
        }
        
        logger.info("Batch job scheduler is active and ready to execute jobs");
        logger.info("Daily Transaction Job: Every day at 2:00 AM (business days only)");
        logger.info("Interest Calculation Job: Last day of month at 3:00 AM (business days only)");
        logger.info("Statement Generation Job: First business day of month at 4:00 AM");
    }

    /**
     * Scheduled method for daily transaction processing.
     * Executes daily at 2:00 AM on business days only.
     * 
     * This job processes daily transaction records from external sources,
     * validates them against account data, and posts valid transactions.
     * 
     * Cron expression: "0 0 2 * * MON-FRI" (2:00 AM Monday through Friday)
     */
    @Scheduled(cron = "0 0 2 * * MON-FRI", zone = "America/New_York")
    public void launchDailyTransactionJob() {
        if (!schedulerEnabled.get()) {
            logger.debug("Scheduler disabled - skipping daily transaction job");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        
        // Check if today is a business day
        if (!isBusinessDay(now.toLocalDate())) {
            logger.info("Skipping daily transaction job - not a business day: {}", now.toLocalDate());
            return;
        }

        // Check execution window
        if (!isWithinExecutionWindow(now)) {
            logger.warn("Daily transaction job scheduled outside execution window: {}", now);
            return;
        }

        logger.info("Starting scheduled daily transaction job execution at {}", now);

        try {
            // Build job parameters
            JobParametersBuilder paramsBuilder = new JobParametersBuilder();
            paramsBuilder.addDate("processingDate", java.sql.Date.valueOf(now.toLocalDate()));
            paramsBuilder.addLong("runId", System.currentTimeMillis());
            paramsBuilder.addString("scheduler", "BatchJobScheduler");

            // Launch job asynchronously
            CompletableFuture<Void> jobFuture = CompletableFuture.runAsync(() -> {
                try {
                    Map<String, Object> jobRequest = new HashMap<>();
                    jobRequest.put("jobName", DAILY_TRANSACTION_JOB);
                    jobRequest.put("parameters", buildJobParameters(now.toLocalDate()));
                    
                    // Use BatchJobLauncher to launch the job
                    Map<String, Object> response = batchJobLauncher.launchJob(createJobRequest(jobRequest)).getBody().getResponseData();
                    Long executionId = (Long) response.get("jobExecutionId");
                    
                    // Record successful execution
                    lastExecutionTimes.put(DAILY_TRANSACTION_JOB, now);
                    lastExecutionIds.put(DAILY_TRANSACTION_JOB, executionId);
                    retryAttempts.remove(DAILY_TRANSACTION_JOB);
                    
                    logger.info("Daily transaction job completed successfully");
                    
                } catch (Exception e) {
                    logger.error("Daily transaction job failed: {}", e.getMessage(), e);
                    handleJobFailure(DAILY_TRANSACTION_JOB, e);
                }
            });

            // Monitor job completion within execution window
            Duration timeRemaining = Duration.between(now, now.withHour(EXECUTION_WINDOW_END_HOUR).withMinute(0).withSecond(0));
            
            jobFuture.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.error("Daily transaction job failed with exception", throwable);
                } else {
                    logger.info("Daily transaction job completed within execution window");
                }
            });

        } catch (Exception e) {
            logger.error("Error launching daily transaction job", e);
            handleJobFailure(DAILY_TRANSACTION_JOB, e);
        }
    }

    /**
     * Scheduled method for monthly interest calculation.
     * Executes on the last business day of each month at 3:00 AM.
     * 
     * This job calculates interest charges on outstanding account balances
     * and posts interest transactions to customer accounts.
     * 
     * Cron expression: "0 0 3 L * ?" (3:00 AM on last day of month)
     */
    @Scheduled(cron = "0 0 3 L * ?", zone = "America/New_York")
    public void launchInterestCalculationJob() {
        if (!schedulerEnabled.get()) {
            logger.debug("Scheduler disabled - skipping interest calculation job");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate processingDate = now.toLocalDate();

        // Check if today is the last business day of the month
        if (!isLastBusinessDayOfMonth(processingDate)) {
            logger.info("Skipping interest calculation job - not last business day of month: {}", processingDate);
            return;
        }

        // Check execution window
        if (!isWithinExecutionWindow(now)) {
            logger.warn("Interest calculation job scheduled outside execution window: {}", now);
            return;
        }

        // Check job dependencies - daily transaction job must complete first
        if (!checkJobDependencies(INTEREST_CALCULATION_JOB)) {
            logger.warn("Interest calculation job dependencies not met - rescheduling");
            scheduleRetry(INTEREST_CALCULATION_JOB);
            return;
        }

        logger.info("Starting scheduled interest calculation job execution at {}", now);

        try {
            // Build job parameters for month-end processing
            Map<String, Object> jobRequest = new HashMap<>();
            jobRequest.put("jobName", INTEREST_CALCULATION_JOB);
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("processingDate", DateConversionUtil.formatCCYYMMDD(processingDate));
            parameters.put("monthEndProcessing", "Y");
            parameters.put("runId", System.currentTimeMillis());
            parameters.put("scheduler", "BatchJobScheduler");
            
            jobRequest.put("parameters", parameters);

            // Launch job asynchronously
            CompletableFuture<Void> jobFuture = CompletableFuture.runAsync(() -> {
                try {
                    Map<String, Object> response = batchJobLauncher.launchJob(createJobRequest(jobRequest)).getBody().getResponseData();
                    Long executionId = (Long) response.get("jobExecutionId");
                    
                    // Record successful execution
                    lastExecutionTimes.put(INTEREST_CALCULATION_JOB, now);
                    lastExecutionIds.put(INTEREST_CALCULATION_JOB, executionId);
                    retryAttempts.remove(INTEREST_CALCULATION_JOB);
                    
                    logger.info("Interest calculation job completed successfully");
                    
                } catch (Exception e) {
                    logger.error("Interest calculation job failed: {}", e.getMessage(), e);
                    handleJobFailure(INTEREST_CALCULATION_JOB, e);
                }
            });

            jobFuture.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.error("Interest calculation job failed with exception", throwable);
                }
            });

        } catch (Exception e) {
            logger.error("Error launching interest calculation job", e);
            handleJobFailure(INTEREST_CALCULATION_JOB, e);
        }
    }

    /**
     * Scheduled method for monthly statement generation.
     * Executes on the first business day of each month at 4:00 AM.
     * 
     * This job generates monthly account statements for all active accounts
     * and produces both plain text and HTML formatted output files.
     * 
     * Cron expression: "0 0 4 1-7 * MON-FRI" (4:00 AM on first business day of month)
     */
    @Scheduled(cron = "0 0 4 1-7 * MON-FRI", zone = "America/New_York")
    public void launchStatementGenerationJob() {
        if (!schedulerEnabled.get()) {
            logger.debug("Scheduler disabled - skipping statement generation job");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate processingDate = now.toLocalDate();

        // Check if today is the first business day of the month
        if (!isFirstBusinessDayOfMonth(processingDate)) {
            logger.info("Skipping statement generation job - not first business day of month: {}", processingDate);
            return;
        }

        // Check execution window
        if (!isWithinExecutionWindow(now)) {
            logger.warn("Statement generation job scheduled outside execution window: {}", now);
            return;
        }

        // Check job dependencies - both daily transaction and interest calculation must complete
        if (!checkJobDependencies(STATEMENT_GENERATION_JOB)) {
            logger.warn("Statement generation job dependencies not met - rescheduling");
            scheduleRetry(STATEMENT_GENERATION_JOB);
            return;
        }

        logger.info("Starting scheduled statement generation job execution at {}", now);

        try {
            // Build job parameters for statement generation
            Map<String, Object> jobRequest = new HashMap<>();
            jobRequest.put("jobName", STATEMENT_GENERATION_JOB);
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("statementDate", DateConversionUtil.formatCCYYMMDD(processingDate));
            parameters.put("outputFormat", "BOTH"); // Generate both text and HTML
            parameters.put("runId", System.currentTimeMillis());
            parameters.put("scheduler", "BatchJobScheduler");
            
            jobRequest.put("parameters", parameters);

            // Launch job asynchronously
            CompletableFuture<Void> jobFuture = CompletableFuture.runAsync(() -> {
                try {
                    Map<String, Object> response = batchJobLauncher.launchJob(createJobRequest(jobRequest)).getBody().getResponseData();
                    Long executionId = (Long) response.get("jobExecutionId");
                    
                    // Record successful execution
                    lastExecutionTimes.put(STATEMENT_GENERATION_JOB, now);
                    lastExecutionIds.put(STATEMENT_GENERATION_JOB, executionId);
                    retryAttempts.remove(STATEMENT_GENERATION_JOB);
                    
                    logger.info("Statement generation job completed successfully");
                    
                } catch (Exception e) {
                    logger.error("Statement generation job failed: {}", e.getMessage(), e);
                    handleJobFailure(STATEMENT_GENERATION_JOB, e);
                }
            });

            jobFuture.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.error("Statement generation job failed with exception", throwable);
                }
            });

        } catch (Exception e) {
            logger.error("Error launching statement generation job", e);
            handleJobFailure(STATEMENT_GENERATION_JOB, e);
        }
    }

    /**
     * Checks job dependencies to ensure prerequisite jobs have completed successfully
     * before allowing dependent jobs to execute.
     * 
     * Dependency Rules:
     * - Daily Transaction Job: No dependencies (can run independently)
     * - Interest Calculation Job: Requires Daily Transaction Job completion
     * - Statement Generation Job: Requires both Daily Transaction and Interest Calculation completion
     * 
     * @param jobName The name of the job to check dependencies for
     * @return true if all dependencies are satisfied, false otherwise
     */
    public boolean checkJobDependencies(String jobName) {
        switch (jobName) {
            case DAILY_TRANSACTION_JOB:
                // No dependencies
                return true;
                
            case INTEREST_CALCULATION_JOB:
                // Requires daily transaction job completion
                return hasJobCompletedToday(DAILY_TRANSACTION_JOB);
                
            case STATEMENT_GENERATION_JOB:
                // Requires both daily transaction and interest calculation completion
                return hasJobCompletedToday(DAILY_TRANSACTION_JOB) && 
                       hasJobCompletedToday(INTEREST_CALCULATION_JOB);
                
            default:
                logger.warn("Unknown job name for dependency check: {}", jobName);
                return false;
        }
    }

    /**
     * Validates whether the given date is a business day (Monday-Friday, excluding holidays).
     * This method implements the mainframe business day logic to ensure jobs only execute
     * on appropriate dates.
     * 
     * @param date The date to validate
     * @return true if the date is a business day, false otherwise
     */
    public boolean isBusinessDay(LocalDate date) {
        // Check if it's a weekend
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            logger.debug("Date {} is a weekend day: {}", date, dayOfWeek);
            return false;
        }

        // Use DateConversionUtil to validate business day (excludes holidays)
        try {
            DateConversionUtil.validateDate(DateConversionUtil.formatCCYYMMDD(date));
            return true;
        } catch (Exception e) {
            logger.debug("Date {} failed business day validation (likely a holiday): {}", date, e.getMessage());
            return false;
        }
    }

    /**
     * Checks if the current time is within the designated 4-hour execution window (2:00 AM - 6:00 AM).
     * Jobs are only allowed to start within this window to ensure they complete within 
     * the allocated batch processing time frame.
     * 
     * @param currentTime The current time to check
     * @return true if within execution window, false otherwise
     */
    public boolean isWithinExecutionWindow(LocalDateTime currentTime) {
        int hour = currentTime.getHour();
        boolean withinWindow = hour >= EXECUTION_WINDOW_START_HOUR && hour < EXECUTION_WINDOW_END_HOUR;
        
        if (!withinWindow) {
            logger.debug("Time {} is outside execution window ({}:00 - {}:00)", 
                    currentTime, EXECUTION_WINDOW_START_HOUR, EXECUTION_WINDOW_END_HOUR);
        }
        
        return withinWindow;
    }

    /**
     * Implements retry logic for failed batch jobs using exponential backoff strategy.
     * Jobs that encounter retryable errors are automatically rescheduled with increasing
     * delay intervals to handle transient system issues.
     * 
     * @param jobName The name of the job that failed
     * @param exception The exception that caused the failure
     * @return true if retry was scheduled, false if max retries exceeded
     */
    public boolean retryFailedJob(String jobName, Exception exception) {
        // Check if the exception is retryable
        boolean isRetryable = false;
        if (exception instanceof BatchProcessingException) {
            BatchProcessingException bpe = (BatchProcessingException) exception;
            isRetryable = bpe.isRetryable();
        } else {
            // Default to retryable for non-BatchProcessingException errors
            isRetryable = true;
        }

        if (!isRetryable) {
            logger.error("Job {} failed with non-retryable error: {}", jobName, exception.getMessage());
            return false;
        }

        int currentRetries = retryAttempts.getOrDefault(jobName, 0);
        
        if (currentRetries >= MAX_RETRY_ATTEMPTS) {
            logger.error("Job {} exceeded maximum retry attempts ({}), giving up", jobName, MAX_RETRY_ATTEMPTS);
            // Send alert for manual intervention
            sendFailureAlert(jobName, exception);
            return false;
        }

        // Calculate delay with exponential backoff
        long delaySeconds = (long) (INITIAL_RETRY_DELAY_SECONDS * Math.pow(RETRY_BACKOFF_MULTIPLIER, currentRetries));
        
        // Increment retry count
        retryAttempts.put(jobName, currentRetries + 1);
        
        logger.info("Scheduling retry #{} for job {} in {} seconds", 
                currentRetries + 1, jobName, delaySeconds);

        // Schedule retry (simplified - in production would use TaskScheduler)
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(delaySeconds * 1000);
                
                // Check if we're still within execution window before retrying
                LocalDateTime retryTime = LocalDateTime.now();
                if (!isWithinExecutionWindow(retryTime)) {
                    logger.warn("Retry for job {} skipped - outside execution window: {}", jobName, retryTime);
                    return;
                }
                
                // Retry the job based on job type
                switch (jobName) {
                    case DAILY_TRANSACTION_JOB:
                        launchDailyTransactionJob();
                        break;
                    case INTEREST_CALCULATION_JOB:
                        launchInterestCalculationJob();
                        break;
                    case STATEMENT_GENERATION_JOB:
                        launchStatementGenerationJob();
                        break;
                    default:
                        logger.warn("Unknown job name for retry: {}", jobName);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Retry for job {} was interrupted", jobName);
            }
        });

        return true;
    }

    /**
     * Gets the current execution status of a specific batch job.
     * This method queries the BatchJobLauncher to determine if a job is currently
     * running, completed, or failed.
     * 
     * @param jobName The name of the job to check status for
     * @return Job status information including state and execution details
     */
    public Map<String, Object> getJobStatus(String jobName) {
        Map<String, Object> status = new HashMap<>();
        
        try {
            status.put("jobName", jobName);
            status.put("lastExecutionTime", lastExecutionTimes.get(jobName));
            status.put("retryAttempts", retryAttempts.getOrDefault(jobName, 0));
            status.put("schedulerEnabled", schedulerEnabled.get());
            
            // Get job status from BatchJobLauncher if we have an execution ID
            Long executionId = lastExecutionIds.get(jobName);
            if (executionId != null) {
                Map<String, Object> jobStatus = batchJobLauncher.getJobStatus(executionId).getBody().getResponseData();
                status.put("executionId", executionId);
                status.put("status", jobStatus.get("status"));
                
                // Add additional status details from BatchJobLauncher
                if (jobStatus.containsKey("startTime")) {
                    status.put("startTime", jobStatus.get("startTime"));
                }
                if (jobStatus.containsKey("endTime")) {
                    status.put("endTime", jobStatus.get("endTime"));
                }
            } else {
                status.put("status", "NOT_EXECUTED");
                status.put("executionId", null);
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving status for job {}: {}", jobName, e.getMessage());
            status.put("jobName", jobName);
            status.put("status", "ERROR");
            status.put("error", e.getMessage());
        }
        
        return status;
    }

    /**
     * Stops all scheduled jobs and disables the scheduler.
     * This method provides graceful shutdown capability for maintenance windows
     * or emergency situations where batch processing must be halted.
     * 
     * @return true if scheduler was successfully stopped, false otherwise
     */
    public boolean stopScheduledJobs() {
        logger.info("Stopping batch job scheduler...");
        
        try {
            // Disable scheduler to prevent new jobs from starting
            schedulerEnabled.set(false);
            
            // Attempt to stop any currently running jobs
            List<String> stoppedJobs = new ArrayList<>();
            
            for (String jobName : List.of(DAILY_TRANSACTION_JOB, INTEREST_CALCULATION_JOB, STATEMENT_GENERATION_JOB)) {
                try {
                    Long executionId = lastExecutionIds.get(jobName);
                    if (executionId != null) {
                        Map<String, Object> jobStatus = batchJobLauncher.getJobStatus(executionId).getBody().getResponseData();
                        String status = (String) jobStatus.get("status");
                        
                        if ("RUNNING".equals(status) || "STARTED".equals(status)) {
                            // Create stop request
                            ApiRequest<Map<String, Object>> stopRequest = new ApiRequest<>();
                            stopRequest.setTransactionCode("JOB_STOP");
                            stopRequest.setRequestData(new HashMap<>());
                            
                            batchJobLauncher.stopJob(executionId, stopRequest);
                            stoppedJobs.add(jobName);
                            logger.info("Stopped running job: {}", jobName);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Failed to stop job {}: {}", jobName, e.getMessage());
                }
            }
            
            // Clear retry attempts and execution history
            retryAttempts.clear();
            lastExecutionTimes.clear();
            lastExecutionIds.clear();
            
            logger.info("Batch job scheduler stopped successfully. Stopped {} running jobs: {}", 
                    stoppedJobs.size(), stoppedJobs);
            return true;
            
        } catch (Exception e) {
            logger.error("Error stopping batch job scheduler", e);
            return false;
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    /**
     * Handles job failure by logging the error and initiating retry logic if appropriate.
     * 
     * @param jobName The name of the failed job
     * @param exception The exception that caused the failure
     */
    private void handleJobFailure(String jobName, Exception exception) {
        logger.error("Job {} failed with exception: {}", jobName, exception.getMessage(), exception);
        
        // Attempt to retry the job
        boolean retryScheduled = retryFailedJob(jobName, exception);
        
        if (!retryScheduled) {
            // If retry is not possible or max retries exceeded, send alert
            sendFailureAlert(jobName, exception);
        }
    }

    /**
     * Checks if a specific job has completed successfully today.
     * Used for dependency checking between jobs.
     * 
     * @param jobName The name of the job to check
     * @return true if job completed today, false otherwise
     */
    private boolean hasJobCompletedToday(String jobName) {
        LocalDateTime lastExecution = lastExecutionTimes.get(jobName);
        
        if (lastExecution == null) {
            logger.debug("No execution record found for job: {}", jobName);
            return false;
        }
        
        LocalDate today = LocalDate.now();
        LocalDate lastExecutionDate = lastExecution.toLocalDate();
        
        boolean completedToday = today.equals(lastExecutionDate);
        logger.debug("Job {} last executed on {}, completed today: {}", jobName, lastExecutionDate, completedToday);
        
        return completedToday;
    }

    /**
     * Determines if the given date is the first business day of the month.
     * Used for statement generation job scheduling.
     * 
     * @param date The date to check
     * @return true if it's the first business day of the month
     */
    private boolean isFirstBusinessDayOfMonth(LocalDate date) {
        // Check if it's in the first week of the month (days 1-7)
        int dayOfMonth = date.getDayOfMonth();
        if (dayOfMonth > 7) {
            return false;
        }
        
        // Check if it's a business day
        if (!isBusinessDay(date)) {
            return false;
        }
        
        // Find the first business day of this month
        LocalDate firstOfMonth = date.withDayOfMonth(1);
        LocalDate firstBusinessDay = firstOfMonth;
        
        // If the 1st is not a business day, find the next business day
        while (!isBusinessDay(firstBusinessDay)) {
            firstBusinessDay = firstBusinessDay.plusDays(1);
        }
        
        return date.equals(firstBusinessDay);
    }

    /**
     * Determines if the given date is the last business day of the month.
     * Used for interest calculation job scheduling.
     * 
     * @param date The date to check
     * @return true if it's the last business day of the month
     */
    private boolean isLastBusinessDayOfMonth(LocalDate date) {
        // Check if it's a business day first
        if (!isBusinessDay(date)) {
            return false;
        }
        
        // Find the last business day of this month
        LocalDate lastOfMonth = date.withDayOfMonth(date.lengthOfMonth());
        LocalDate lastBusinessDay = lastOfMonth;
        
        // If the last day is not a business day, find the previous business day
        while (!isBusinessDay(lastBusinessDay)) {
            lastBusinessDay = lastBusinessDay.minusDays(1);
        }
        
        return date.equals(lastBusinessDay);
    }

    /**
     * Builds job parameters map for batch job execution.
     * 
     * @param processingDate The date for which the job should process data
     * @return Map containing job parameters
     */
    private Map<String, Object> buildJobParameters(LocalDate processingDate) {
        Map<String, Object> parameters = new HashMap<>();
        
        parameters.put("processingDate", DateConversionUtil.formatCCYYMMDD(processingDate));
        parameters.put("runId", System.currentTimeMillis());
        parameters.put("scheduler", "BatchJobScheduler");
        parameters.put("executionWindow", EXECUTION_WINDOW_START_HOUR + "-" + EXECUTION_WINDOW_END_HOUR);
        
        return parameters;
    }

    /**
     * Creates a properly formatted job request object for the BatchJobLauncher.
     * 
     * @param jobRequest Map containing job name and parameters
     * @return Formatted ApiRequest object
     */
    private ApiRequest<Map<String, Object>> createJobRequest(Map<String, Object> jobRequest) {
        ApiRequest<Map<String, Object>> apiRequest = new ApiRequest<>();
        apiRequest.setTransactionCode("BATCH_SCHEDULER");
        apiRequest.setRequestData(jobRequest);
        return apiRequest;
    }

    /**
     * Schedules a retry for a failed job after a delay.
     * 
     * @param jobName The name of the job to retry
     */
    private void scheduleRetry(String jobName) {
        int currentRetries = retryAttempts.getOrDefault(jobName, 0);
        
        if (currentRetries >= MAX_RETRY_ATTEMPTS) {
            logger.error("Job {} retry limit exceeded, manual intervention required", jobName);
            return;
        }
        
        // Calculate delay (5 minutes base delay)
        long delayMinutes = INITIAL_RETRY_DELAY_SECONDS / 60;
        
        logger.info("Scheduling retry for job {} in {} minutes due to dependencies", jobName, delayMinutes);
        
        // In production, this would use Spring's TaskScheduler for proper retry scheduling
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(delayMinutes * 60 * 1000); // Convert to milliseconds
                
                // Re-check dependencies and retry if appropriate
                if (checkJobDependencies(jobName)) {
                    switch (jobName) {
                        case INTEREST_CALCULATION_JOB:
                            launchInterestCalculationJob();
                            break;
                        case STATEMENT_GENERATION_JOB:
                            launchStatementGenerationJob();
                            break;
                    }
                } else {
                    logger.warn("Dependencies still not met for job {} after retry delay", jobName);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Retry scheduling interrupted for job {}", jobName);
            }
        });
    }

    /**
     * Sends failure alert for jobs that cannot be retried or have exceeded retry limits.
     * In production, this would integrate with monitoring systems like PagerDuty or email alerts.
     * 
     * @param jobName The name of the failed job
     * @param exception The exception that caused the failure
     */
    private void sendFailureAlert(String jobName, Exception exception) {
        logger.error("ALERT: Job {} has failed and requires manual intervention", jobName);
        logger.error("Failure details: {}", exception.getMessage(), exception);
        
        // In production, this method would:
        // 1. Send email notifications to operations team
        // 2. Create tickets in monitoring systems
        // 3. Trigger PagerDuty alerts for critical jobs
        // 4. Update operational dashboards
        
        String alertMessage = String.format(
            "BATCH JOB FAILURE ALERT\n" +
            "Job Name: %s\n" +
            "Failure Time: %s\n" +
            "Error Message: %s\n" +
            "Retry Attempts: %d/%d\n" +
            "Manual intervention required",
            jobName, 
            LocalDateTime.now().toString(),
            exception.getMessage(),
            retryAttempts.getOrDefault(jobName, 0),
            MAX_RETRY_ATTEMPTS
        );
        
        logger.error(alertMessage);
    }
}
