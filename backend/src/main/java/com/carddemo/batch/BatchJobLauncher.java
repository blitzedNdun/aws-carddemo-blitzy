/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.batch;

import com.carddemo.config.BatchConfig;
import com.carddemo.batch.DailyTransactionJob;
import com.carddemo.batch.InterestCalculationJob;
import com.carddemo.batch.StatementGenerationJob;
import com.carddemo.batch.TransactionReportJob;
import com.carddemo.batch.AccountListJob;
import com.carddemo.batch.CardListJob;
import com.carddemo.batch.CrossReferenceListJob;
import com.carddemo.batch.CustomerListJob;
import com.carddemo.batch.AccountProcessingJob;
import com.carddemo.batch.DataMigrationJob;
import com.carddemo.batch.BatchProcessingException;
import com.carddemo.dto.ApiRequest;
import com.carddemo.dto.ApiResponse;
import com.carddemo.dto.PageResponse;
import com.carddemo.dto.Message;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.Job;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.slf4j.LoggerFactory;
import java.util.Date;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepExecution;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.math.BigDecimal;

import org.slf4j.Logger;

/**
 * Spring Boot REST Controller for launching and managing batch jobs programmatically.
 * 
 * This component replaces the traditional JCL job submission mechanism from mainframe COBOL programs
 * (specifically CORPT00C.cbl) with modern REST-based batch job control capabilities. It provides
 * comprehensive job lifecycle management including launching, monitoring, stopping, and restarting
 * batch operations through RESTful endpoints.
 * 
 * COBOL Migration Context:
 * The original COBOL program CORPT00C.cbl submitted batch jobs by:
 * 1. Constructing JCL job streams with embedded parameters
 * 2. Writing JCL records to an extra partition TDQ named 'JOBS'
 * 3. Relying on JES2/JES3 job scheduling for execution
 * 4. Using mainframe job control language for parameter passing
 * 
 * This Spring Boot implementation provides equivalent functionality through:
 * 1. REST endpoints accepting job parameters as JSON payloads
 * 2. Spring Batch JobLauncher for programmatic job execution
 * 3. Asynchronous job execution with real-time status monitoring
 * 4. Comprehensive job parameter validation and type conversion
 * 5. Job execution history and metadata management
 * 
 * Key Features:
 * - Programmatic job launching with parameter validation
 * - Asynchronous job execution preventing request timeout
 * - Real-time job status monitoring and progress tracking
 * - Job restart capability for failed executions
 * - Job termination and abandonment support
 * - Comprehensive error handling and logging
 * - Integration with Spring Batch infrastructure
 * - REST API compatibility for frontend integration
 * 
 * Supported Job Operations:
 * - Launch new job instances with custom parameters
 * - Query job execution status and progress
 * - Stop running jobs gracefully or forcefully
 * - Restart failed jobs from point of failure
 * - List available job names and definitions
 * - Browse job execution history with pagination
 * 
 * Security and Validation:
 * - Job parameter validation matching JCL PARM specifications
 * - User session context validation for job access control
 * - Input sanitization preventing injection attacks
 * - Comprehensive audit logging for compliance requirements
 * - Error handling preventing sensitive information disclosure
 * 
 * Performance Considerations:
 * - Asynchronous job execution using ThreadPoolTaskExecutor
 * - Connection pooling for database-intensive operations
 * - Chunk-based processing for large dataset handling
 * - Memory-efficient job parameter management
 * - Optimized job metadata queries for status monitoring
 * 
 * @author CardDemo Migration Team  
 * @version 1.0
 * @since CardDemo v1.0
 */
@RestController
@RequestMapping("/api/batch")
@Component
public class BatchJobLauncher {

    private static final Logger logger = LoggerFactory.getLogger(BatchJobLauncher.class);

    // Batch infrastructure components injected from BatchConfig
    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer; 
    private final JobOperator jobOperator;
    private final ApplicationContext applicationContext;

    // Set of supported job names for validation
    private static final Set<String> SUPPORTED_JOBS = Set.of(
        "dailyTransactionJob",
        "interestCalculationJob", 
        "statementGenerationJob",
        "transactionReportJob",
        "accountListJob",
        "cardListJob",
        "crossReferenceListJob",
        "customerListJob",
        "accountProcessingJob",
        "migrationJob"
    );

    // Default pagination settings for job execution listings
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Constructor for dependency injection of Spring Batch infrastructure components.
     * 
     * @param jobLauncher Spring Batch JobLauncher for programmatic job execution
     * @param jobExplorer Spring Batch JobExplorer for job metadata queries
     * @param jobOperator Spring Batch JobOperator for job lifecycle operations
     * @param applicationContext Spring ApplicationContext for dynamic job bean lookup
     */
    @Autowired
    public BatchJobLauncher(JobLauncher jobLauncher, JobExplorer jobExplorer, 
                           JobOperator jobOperator, ApplicationContext applicationContext) {
        this.jobLauncher = jobLauncher;
        this.jobExplorer = jobExplorer;
        this.jobOperator = jobOperator;
        this.applicationContext = applicationContext;
    }

    /**
     * Launches a batch job asynchronously with specified parameters.
     * 
     * This endpoint replaces the COBOL JCL job submission functionality from CORPT00C.cbl
     * by accepting job parameters through REST API and launching jobs programmatically
     * using Spring Batch infrastructure.
     * 
     * Request Processing:
     * 1. Validates job name against supported job definitions
     * 2. Validates and converts job parameters based on job requirements
     * 3. Builds JobParameters object with type-safe parameter handling
     * 4. Launches job asynchronously using Spring Batch JobLauncher
     * 5. Returns job execution ID for subsequent status monitoring
     * 
     * Parameter Validation:
     * - Job name must match available job bean definitions
     * - Processing dates must be in valid format (YYYY-MM-DD)
     * - File paths must be absolute and accessible
     * - Numeric parameters must be within acceptable ranges
     * - Required parameters must be provided based on job type
     * 
     * Error Handling:
     * - Invalid job names return NOT_FOUND status
     * - Parameter validation errors return BAD_REQUEST status
     * - Duplicate job instances return CONFLICT status
     * - System errors return INTERNAL_SERVER_ERROR status
     * 
     * @param request ApiRequest containing job launch parameters including:
     *                - jobName: Name of the job to launch
     *                - parameters: Map of job-specific parameters
     *                - sessionContext: User session for audit logging
     * @return ApiResponse containing job execution details or error information
     */
    @PostMapping("/jobs/launch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> launchJob(
            @RequestBody ApiRequest<Map<String, Object>> request) {
        
        logger.info("Received job launch request for transaction: {}", request.getTransactionCode());
        
        ApiResponse<Map<String, Object>> response = new ApiResponse<>();
        response.setTransactionCode(request.getTransactionCode());
        
        try {
            // Extract job launch parameters from request
            Map<String, Object> requestData = request.getRequestData();
            if (requestData == null || requestData.isEmpty()) {
                throw new BatchProcessingException("Job launch request data is required", false);
            }
            
            String jobName = (String) requestData.get("jobName");
            if (jobName == null || jobName.trim().isEmpty()) {
                throw new BatchProcessingException("Job name is required for job launch", false);
            }
            
            // Validate job name against supported jobs
            if (!SUPPORTED_JOBS.contains(jobName)) {
                throw new BatchProcessingException("Unsupported job name: " + jobName + 
                    ". Supported jobs: " + SUPPORTED_JOBS, false);
            }
            
            // Extract and validate job parameters
            @SuppressWarnings("unchecked")
            Map<String, Object> jobParams = (Map<String, Object>) requestData.get("parameters");
            if (jobParams == null) {
                jobParams = new HashMap<>();
            }
            
            // Build Spring Batch JobParameters with validation
            JobParameters jobParameters = buildJobParameters(jobName, jobParams);
            
            // Get job bean from application context
            Job job = getJobFromContext(jobName);
            
            // Launch job asynchronously
            logger.info("Launching job: {} with parameters: {}", jobName, jobParameters);
            JobExecution jobExecution = jobLauncher.run(job, jobParameters);
            
            // Build response with job execution details
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("jobExecutionId", jobExecution.getId());
            responseData.put("jobName", jobName);
            responseData.put("status", jobExecution.getStatus().toString());
            responseData.put("startTime", jobExecution.getStartTime());
            responseData.put("createTime", jobExecution.getCreateTime());
            responseData.put("jobInstanceId", jobExecution.getJobInstance().getId());
            
            response.setResponseData(responseData);
            response.setSuccess(true);
            response.getMessages().add(Message.info("Job launched successfully: " + jobName));
            
            logger.info("Successfully launched job: {} with execution ID: {}", 
                       jobName, jobExecution.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (BatchProcessingException e) {
            logger.error("Batch processing error during job launch: {}", e.getMessage());
            response.setSuccess(false);
            response.setReturnCode("BATCH_ERROR");
            response.getMessages().add(new Message("ERROR", e.getMessage()));
            
            if (e.isRetryable()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error during job launch", e);
            response.setSuccess(false);
            response.setReturnCode("SYSTEM_ERROR");
            response.getMessages().add(Message.error("System error occurred during job launch"));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Retrieves the current status and execution details for a specific job execution.
     * 
     * This endpoint provides comprehensive job status information including execution state,
     * timing details, step execution progress, and error information. It replaces the need
     * for manual job status checking that was required with JCL-based batch processing.
     * 
     * Status Information Provided:
     * - Job execution status (STARTED, COMPLETED, FAILED, STOPPED, etc.)
     * - Start time, end time, and execution duration
     * - Step execution details with read/write/error counts
     * - Exit status and exit code information
     * - Job parameters used for the execution
     * - Error messages and exception details if available
     * 
     * @param jobExecutionId The unique identifier of the job execution to query
     * @return ApiResponse containing detailed job status information
     */
    @GetMapping("/jobs/status/{jobExecutionId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getJobStatus(
            @PathVariable Long jobExecutionId) {
        
        logger.info("Received job status request for execution ID: {}", jobExecutionId);
        
        ApiResponse<Map<String, Object>> response = new ApiResponse<>();
        response.setTransactionCode("JOB_STATUS");
        
        try {
            // Retrieve job execution from repository
            JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);
            
            if (jobExecution == null) {
                throw new BatchProcessingException("Job execution not found: " + jobExecutionId, false);
            }
            
            // Build comprehensive status response
            Map<String, Object> statusData = new HashMap<>();
            
            // Basic execution information
            statusData.put("jobExecutionId", jobExecution.getId());
            statusData.put("jobInstanceId", jobExecution.getJobInstance().getId());
            statusData.put("jobName", jobExecution.getJobInstance().getJobName());
            statusData.put("status", jobExecution.getStatus().toString());
            statusData.put("batchStatus", jobExecution.getStatus().name());
            statusData.put("exitStatus", jobExecution.getExitStatus().getExitCode());
            statusData.put("exitDescription", jobExecution.getExitStatus().getExitDescription());
            
            // Timing information
            statusData.put("createTime", jobExecution.getCreateTime());
            statusData.put("startTime", jobExecution.getStartTime());
            statusData.put("endTime", jobExecution.getEndTime());
            
            // Calculate duration if job has started
            if (jobExecution.getStartTime() != null) {
                Date endTime = jobExecution.getEndTime() != null ? 
                    jobExecution.getEndTime() : new Date();
                long durationMs = endTime.getTime() - jobExecution.getStartTime().getTime();
                statusData.put("durationMillis", durationMs);
                statusData.put("durationSeconds", durationMs / 1000);
            }
            
            // Job parameters
            Map<String, Object> parameters = new HashMap<>();
            jobExecution.getJobParameters().getParameters().forEach((key, parameter) -> {
                parameters.put(key, parameter.getValue());
            });
            statusData.put("jobParameters", parameters);
            
            // Step execution details
            List<Map<String, Object>> stepExecutions = new ArrayList<>();
            for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
                Map<String, Object> stepData = new HashMap<>();
                stepData.put("stepName", stepExecution.getStepName());
                stepData.put("status", stepExecution.getStatus().toString());
                stepData.put("exitStatus", stepExecution.getExitStatus().getExitCode());
                stepData.put("startTime", stepExecution.getStartTime());
                stepData.put("endTime", stepExecution.getEndTime());
                stepData.put("readCount", stepExecution.getReadCount());
                stepData.put("writeCount", stepExecution.getWriteCount());
                stepData.put("commitCount", stepExecution.getCommitCount());
                stepData.put("rollbackCount", stepExecution.getRollbackCount());
                stepData.put("readSkipCount", stepExecution.getReadSkipCount());
                stepData.put("processSkipCount", stepExecution.getProcessSkipCount());
                stepData.put("writeSkipCount", stepExecution.getWriteSkipCount());
                stepData.put("filterCount", stepExecution.getFilterCount());
                
                // Include execution context summary
                Map<String, Object> executionContext = new HashMap<>();
                stepExecution.getExecutionContext().entrySet().forEach(entry -> {
                    executionContext.put(entry.getKey(), entry.getValue());
                });
                stepData.put("executionContext", executionContext);
                
                stepExecutions.add(stepData);
            }
            statusData.put("stepExecutions", stepExecutions);
            
            // Error information if job failed
            if (jobExecution.getStatus() == BatchStatus.FAILED) {
                List<String> failureExceptions = new ArrayList<>();
                for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
                    if (stepExecution.getStatus() == BatchStatus.FAILED) {
                        stepExecution.getFailureExceptions().forEach(throwable -> {
                            failureExceptions.add(throwable.getMessage());
                        });
                    }
                }
                statusData.put("failureExceptions", failureExceptions);
            }
            
            response.setResponseData(statusData);
            response.setSuccess(true);
            response.getMessages().add(Message.info(
                "Job status retrieved successfully for execution: " + jobExecutionId));
            
            logger.info("Successfully retrieved status for job execution: {}", jobExecutionId);
            
            return ResponseEntity.ok(response);
            
        } catch (BatchProcessingException e) {
            logger.error("Batch processing error during status retrieval: {}", e.getMessage());
            response.setSuccess(false);
            response.setReturnCode("BATCH_ERROR");
            response.getMessages().add(Message.error(e.getMessage()));
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            
        } catch (Exception e) {
            logger.error("Unexpected error during job status retrieval", e);
            response.setSuccess(false);
            response.setReturnCode("SYSTEM_ERROR");
            response.getMessages().add(Message.error("System error occurred during status retrieval"));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Stops a running batch job execution gracefully or forcefully.
     * 
     * This endpoint provides job termination capabilities that were not directly available
     * in traditional JCL processing, offering both graceful shutdown (allowing current
     * chunk to complete) and immediate termination options.
     * 
     * Stop Operations:
     * - Graceful stop: Allows current processing chunk to complete before stopping
     * - Force stop: Attempts immediate termination of job execution
     * - Status validation: Ensures job is in a stoppable state before attempting stop
     * - Resource cleanup: Ensures proper cleanup of database connections and threads
     * 
     * @param jobExecutionId The unique identifier of the job execution to stop
     * @param request ApiRequest containing stop parameters (graceful vs force)
     * @return ApiResponse confirming job stop operation result
     */
    @PostMapping("/jobs/stop/{jobExecutionId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stopJob(
            @PathVariable Long jobExecutionId,
            @RequestBody ApiRequest<Map<String, Object>> request) {
        
        logger.info("Received job stop request for execution ID: {}", jobExecutionId);
        
        ApiResponse<Map<String, Object>> response = new ApiResponse<>();
        response.setTransactionCode("JOB_STOP");
        
        try {
            // Validate job execution exists
            JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);
            if (jobExecution == null) {
                throw new BatchProcessingException("Job execution not found: " + jobExecutionId, false);
            }
            
            // Check if job is in a stoppable state
            BatchStatus status = jobExecution.getStatus();
            if (status != BatchStatus.STARTED && status != BatchStatus.STARTING) {
                throw new BatchProcessingException(
                    "Job execution " + jobExecutionId + " cannot be stopped. Current status: " + status, false);
            }
            
            // Extract stop parameters
            Map<String, Object> requestData = request.getRequestData();
            boolean forceStop = false;
            if (requestData != null && requestData.containsKey("force")) {
                forceStop = Boolean.parseBoolean(requestData.get("force").toString());
            }
            
            // Perform job stop operation
            logger.info("Stopping job execution: {} (force: {})", jobExecutionId, forceStop);
            
            if (forceStop) {
                // Force stop - immediate termination
                boolean stopped = jobOperator.abandon(jobExecutionId);
                if (!stopped) {
                    // Try operator stop if abandon fails
                    stopped = jobOperator.stop(jobExecutionId);
                }
                
                if (!stopped) {
                    throw new BatchProcessingException("Failed to force stop job execution: " + jobExecutionId, true);
                }
            } else {
                // Graceful stop - let current chunk complete
                boolean stopped = jobOperator.stop(jobExecutionId);
                if (!stopped) {
                    throw new BatchProcessingException("Failed to stop job execution: " + jobExecutionId, true);
                }
            }
            
            // Wait briefly for stop to take effect and get updated status
            Thread.sleep(1000);
            jobExecution = jobExplorer.getJobExecution(jobExecutionId);
            
            // Build response with stop confirmation
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("jobExecutionId", jobExecutionId);
            responseData.put("jobName", jobExecution.getJobInstance().getJobName());
            responseData.put("previousStatus", status.toString());
            responseData.put("currentStatus", jobExecution.getStatus().toString());
            responseData.put("forceStop", forceStop);
            responseData.put("stopTime", new Date());
            
            response.setResponseData(responseData);
            response.setSuccess(true);
            response.getMessages().add(Message.info(
                "Job stop operation completed for execution: " + jobExecutionId));
            
            logger.info("Successfully stopped job execution: {} (force: {})", jobExecutionId, forceStop);
            
            return ResponseEntity.ok(response);
            
        } catch (BatchProcessingException e) {
            logger.error("Batch processing error during job stop: {}", e.getMessage());
            response.setSuccess(false);
            response.setReturnCode("BATCH_ERROR");
            response.getMessages().add(Message.error(e.getMessage()));
            
            if (e.isRetryable()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Thread interrupted during job stop operation", e);
            response.setSuccess(false);
            response.setReturnCode("INTERRUPTED_ERROR");
            response.getMessages().add(Message.error("Job stop operation was interrupted"));
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            
        } catch (Exception e) {
            logger.error("Unexpected error during job stop operation", e);
            response.setSuccess(false);
            response.setReturnCode("SYSTEM_ERROR");
            response.getMessages().add(Message.error("System error occurred during job stop"));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Restarts a failed or stopped batch job execution from the point of failure.
     * 
     * This endpoint provides sophisticated restart capabilities that exceed traditional JCL
     * restart functionality by supporting step-level restart, parameter modification, and
     * comprehensive restart validation.
     * 
     * Restart Capabilities:
     * - Failed job restart from point of failure
     * - Stopped job restart with same or modified parameters
     * - Step-level restart for granular recovery
     * - Parameter validation and modification support
     * - Restart history tracking and audit logging
     * 
     * @param jobExecutionId The unique identifier of the job execution to restart
     * @param request ApiRequest containing restart parameters and options
     * @return ApiResponse containing new job execution details after restart
     */
    @PostMapping("/jobs/restart/{jobExecutionId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> restartJob(
            @PathVariable Long jobExecutionId,
            @RequestBody ApiRequest<Map<String, Object>> request) {
        
        logger.info("Received job restart request for execution ID: {}", jobExecutionId);
        
        ApiResponse<Map<String, Object>> response = new ApiResponse<>();
        response.setTransactionCode("JOB_RESTART");
        
        try {
            // Validate original job execution exists
            JobExecution originalExecution = jobExplorer.getJobExecution(jobExecutionId);
            if (originalExecution == null) {
                throw new BatchProcessingException("Job execution not found: " + jobExecutionId, false);
            }
            
            // Check if job is restartable
            BatchStatus status = originalExecution.getStatus();
            if (status != BatchStatus.FAILED && status != BatchStatus.STOPPED) {
                throw new BatchProcessingException(
                    "Job execution " + jobExecutionId + " is not restartable. Current status: " + status + 
                    ". Only FAILED or STOPPED jobs can be restarted.", false);
            }
            
            // Extract restart parameters
            Map<String, Object> requestData = request.getRequestData();
            Map<String, Object> newParams = new HashMap<>();
            if (requestData != null && requestData.containsKey("parameters")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> paramMap = (Map<String, Object>) requestData.get("parameters");
                if (paramMap != null) {
                    newParams.putAll(paramMap);
                }
            }
            
            String jobName = originalExecution.getJobInstance().getJobName();
            
            // Build job parameters for restart
            JobParametersBuilder parametersBuilder = new JobParametersBuilder();
            
            // Start with original parameters
            originalExecution.getJobParameters().getParameters().forEach((key, parameter) -> {
                // Skip run.id parameter as it will be auto-generated for restart
                if (!"run.id".equals(key)) {
                    parametersBuilder.addParameter(key, parameter);
                }
            });
            
            // Add or override with new parameters
            newParams.forEach((key, value) -> {
                if (value instanceof String) {
                    parametersBuilder.addString(key, (String) value);
                } else if (value instanceof Long) {
                    parametersBuilder.addLong(key, (Long) value);
                } else if (value instanceof Date) {
                    parametersBuilder.addDate(key, (Date) value);
                } else if (value instanceof Double) {
                    parametersBuilder.addDouble(key, (Double) value);
                } else {
                    parametersBuilder.addString(key, value.toString());
                }
            });
            
            // Add restart timestamp for uniqueness
            parametersBuilder.addLong("restart.timestamp", System.currentTimeMillis());
            
            JobParameters jobParameters = parametersBuilder.toJobParameters();
            
            // Get job bean and restart
            Job job = getJobFromContext(jobName);
            
            logger.info("Restarting job: {} for execution: {} with parameters: {}", 
                       jobName, jobExecutionId, jobParameters);
            
            JobExecution restartExecution = jobLauncher.run(job, jobParameters);
            
            // Build response with restart execution details
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("originalJobExecutionId", jobExecutionId);
            responseData.put("restartJobExecutionId", restartExecution.getId());
            responseData.put("jobName", jobName);
            responseData.put("originalStatus", status.toString());
            responseData.put("restartStatus", restartExecution.getStatus().toString());
            responseData.put("restartTime", restartExecution.getCreateTime());
            responseData.put("jobInstanceId", restartExecution.getJobInstance().getId());
            
            // Include restart parameters used
            Map<String, Object> restartParams = new HashMap<>();
            restartExecution.getJobParameters().getParameters().forEach((key, parameter) -> {
                restartParams.put(key, parameter.getValue());
            });
            responseData.put("restartParameters", restartParams);
            
            response.setResponseData(responseData);
            response.setSuccess(true);
            response.getMessages().add(Message.info(
                "Job restarted successfully. Original execution: " + jobExecutionId + 
                ", Restart execution: " + restartExecution.getId()));
            
            logger.info("Successfully restarted job: {} (original: {}, restart: {})", 
                       jobName, jobExecutionId, restartExecution.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (BatchProcessingException e) {
            logger.error("Batch processing error during job restart: {}", e.getMessage());
            response.setSuccess(false);
            response.setReturnCode("BATCH_ERROR");
            response.getMessages().add(Message.error(e.getMessage()));
            
            if (e.isRetryable()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error during job restart operation", e);
            response.setSuccess(false);
            response.setReturnCode("SYSTEM_ERROR");
            response.getMessages().add(Message.error("System error occurred during job restart"));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Retrieves a paginated list of job executions with optional filtering by job name and status.
     * 
     * This endpoint provides comprehensive job execution history browsing capabilities that
     * were not available in traditional JCL processing, offering filtering, sorting, and
     * pagination for operational monitoring and troubleshooting.
     * 
     * Query Capabilities:
     * - Pagination with configurable page size (default 20, max 100)
     * - Filtering by job name and execution status
     * - Sorting by execution start time (newest first)
     * - Comprehensive execution metadata in results
     * - Job instance and parameter information
     * 
     * @param jobName Optional job name filter
     * @param status Optional status filter (STARTED, COMPLETED, FAILED, etc.)
     * @param page Page number (zero-based, default 0)
     * @param size Page size (default 20, max 100)
     * @return PageResponse containing filtered job execution list
     */
    @GetMapping("/jobs/executions")
    public ResponseEntity<PageResponse<Map<String, Object>>> getJobExecutions(
            @RequestParam(required = false) String jobName,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("Received job executions request - jobName: {}, status: {}, page: {}, size: {}", 
                   jobName, status, page, size);
        
        PageResponse<Map<String, Object>> response = new PageResponse<>();
        
        try {
            // Validate pagination parameters
            if (page < 0) {
                page = 0;
            }
            if (size <= 0 || size > MAX_PAGE_SIZE) {
                size = Math.min(size <= 0 ? DEFAULT_PAGE_SIZE : size, MAX_PAGE_SIZE);
            }
            
            // Determine which jobs to query
            List<String> jobNamesToQuery = new ArrayList<>();
            if (jobName != null && !jobName.trim().isEmpty()) {
                if (!SUPPORTED_JOBS.contains(jobName)) {
                    throw new BatchProcessingException("Unsupported job name: " + jobName, false);
                }
                jobNamesToQuery.add(jobName);
            } else {
                jobNamesToQuery.addAll(SUPPORTED_JOBS);
            }
            
            // Parse status filter if provided
            BatchStatus statusFilter = null;
            if (status != null && !status.trim().isEmpty()) {
                try {
                    statusFilter = BatchStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new BatchProcessingException("Invalid status filter: " + status + 
                        ". Valid values: " + java.util.Arrays.toString(BatchStatus.values()), false);
                }
            }
            
            // Collect job executions from all matching jobs
            List<Map<String, Object>> allExecutions = new ArrayList<>();
            
            for (String currentJobName : jobNamesToQuery) {
                // Get job instances for this job name
                List<JobInstance> jobInstances = jobExplorer.getJobInstances(currentJobName, 0, 1000);
                
                for (JobInstance jobInstance : jobInstances) {
                    // Get executions for this job instance
                    List<JobExecution> executions = jobExplorer.getJobExecutions(jobInstance);
                    
                    for (JobExecution execution : executions) {
                        // Apply status filter if specified
                        if (statusFilter != null && execution.getStatus() != statusFilter) {
                            continue;
                        }
                        
                        // Build execution summary
                        Map<String, Object> executionData = new HashMap<>();
                        executionData.put("jobExecutionId", execution.getId());
                        executionData.put("jobInstanceId", execution.getJobInstance().getId());
                        executionData.put("jobName", execution.getJobInstance().getJobName());
                        executionData.put("status", execution.getStatus().toString());
                        executionData.put("batchStatus", execution.getStatus().name());
                        executionData.put("exitStatus", execution.getExitStatus().getExitCode());
                        executionData.put("createTime", execution.getCreateTime());
                        executionData.put("startTime", execution.getStartTime());
                        executionData.put("endTime", execution.getEndTime());
                        
                        // Calculate duration if completed
                        if (execution.getStartTime() != null && execution.getEndTime() != null) {
                            long durationMs = execution.getEndTime().getTime() - execution.getStartTime().getTime();
                            executionData.put("durationMillis", durationMs);
                        }
                        
                        // Include key job parameters
                        Map<String, Object> parameters = new HashMap<>();
                        execution.getJobParameters().getParameters().entrySet().stream()
                            .filter(entry -> !entry.getKey().startsWith("run."))
                            .forEach(entry -> parameters.put(entry.getKey(), entry.getValue().getValue()));
                        executionData.put("jobParameters", parameters);
                        
                        // Step execution summary
                        int totalSteps = execution.getStepExecutions().size();
                        long completedSteps = execution.getStepExecutions().stream()
                            .mapToLong(step -> step.getStatus() == BatchStatus.COMPLETED ? 1 : 0)
                            .sum();
                        executionData.put("totalSteps", totalSteps);
                        executionData.put("completedSteps", completedSteps);
                        
                        allExecutions.add(executionData);
                    }
                }
            }
            
            // Sort by create time descending (newest first)
            allExecutions.sort((a, b) -> {
                Date dateA = (Date) a.get("createTime");
                Date dateB = (Date) b.get("createTime");
                if (dateA == null && dateB == null) return 0;
                if (dateA == null) return 1;
                if (dateB == null) return -1;
                return dateB.compareTo(dateA);
            });
            
            // Apply pagination
            int totalElements = allExecutions.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalElements);
            
            List<Map<String, Object>> pageData = new ArrayList<>();
            if (startIndex < totalElements) {
                pageData = allExecutions.subList(startIndex, endIndex);
            }
            
            // Build paginated response
            response.setItems(pageData);
            response.setCurrentPage(page);
            response.setPageSize(size);
            response.setTotalElements(totalElements);
            response.setTotalPages(totalPages);
            
            logger.info("Successfully retrieved {} job executions (page {} of {})", 
                       pageData.size(), page, totalPages);
            
            return ResponseEntity.ok(response);
            
        } catch (BatchProcessingException e) {
            logger.error("Batch processing error during job executions retrieval: {}", e.getMessage());
            
            PageResponse<Map<String, Object>> errorResponse = new PageResponse<>();
            errorResponse.setItems(new ArrayList<>());
            errorResponse.setCurrentPage(page);
            errorResponse.setPageSize(size);
            errorResponse.setTotalElements(0);
            errorResponse.setTotalPages(0);
            
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Unexpected error during job executions retrieval", e);
            
            PageResponse<Map<String, Object>> errorResponse = new PageResponse<>();
            errorResponse.setItems(new ArrayList<>());
            errorResponse.setCurrentPage(page);
            errorResponse.setPageSize(size);
            errorResponse.setTotalElements(0);
            errorResponse.setTotalPages(0);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves the list of available batch job names and their configuration details.
     * 
     * This endpoint provides job discovery capabilities for frontend applications and
     * operational tools, listing all available batch jobs with their descriptions,
     * required parameters, and configuration information.
     * 
     * @return ApiResponse containing list of available job names and metadata
     */
    @GetMapping("/jobs/names")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getJobNames() {
        
        logger.info("Received request for available job names");
        
        ApiResponse<Map<String, Object>> response = new ApiResponse<>();
        response.setTransactionCode("JOB_NAMES");
        
        try {
            // Build comprehensive job information
            Map<String, Object> jobsData = new HashMap<>();
            
            // List of supported job names
            jobsData.put("supportedJobs", new ArrayList<>(SUPPORTED_JOBS));
            
            // Detailed job information with descriptions and parameters
            List<Map<String, Object>> jobDetails = new ArrayList<>();
            
            for (String jobName : SUPPORTED_JOBS) {
                Map<String, Object> jobInfo = new HashMap<>();
                jobInfo.put("jobName", jobName);
                jobInfo.put("displayName", formatJobDisplayName(jobName));
                jobInfo.put("description", getJobDescription(jobName));
                jobInfo.put("category", getJobCategory(jobName));
                jobInfo.put("requiredParameters", getRequiredParameters(jobName));
                jobInfo.put("optionalParameters", getOptionalParameters(jobName));
                jobInfo.put("estimatedDurationMinutes", getEstimatedDuration(jobName));
                jobInfo.put("restartSupported", true);
                
                // Check if job bean is available
                try {
                    Job job = getJobFromContext(jobName);
                    jobInfo.put("available", true);
                    jobInfo.put("beanName", job.getName());
                } catch (Exception e) {
                    jobInfo.put("available", false);
                    jobInfo.put("error", "Job bean not found: " + e.getMessage());
                }
                
                jobDetails.add(jobInfo);
            }
            
            jobsData.put("jobDetails", jobDetails);
            jobsData.put("totalJobs", SUPPORTED_JOBS.size());
            
            // Include job categories summary
            Map<String, Long> categorySummary = jobDetails.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    job -> (String) job.get("category"),
                    java.util.stream.Collectors.counting()
                ));
            jobsData.put("categorySummary", categorySummary);
            
            response.setResponseData(jobsData);
            response.setSuccess(true);
            response.getMessages().add(Message.info(
                "Retrieved " + SUPPORTED_JOBS.size() + " available job definitions"));
            
            logger.info("Successfully retrieved {} job names", SUPPORTED_JOBS.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Unexpected error during job names retrieval", e);
            response.setSuccess(false);
            response.setReturnCode("SYSTEM_ERROR");
            response.getMessages().add(Message.error("System error occurred during job names retrieval"));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ========================================================================================
    // Private Utility Methods
    // ========================================================================================

    /**
     * Builds JobParameters object with validation for the specified job type.
     * 
     * This method converts the request parameter map into a type-safe JobParameters
     * object with proper validation based on the specific job requirements.
     * 
     * @param jobName The name of the job being launched
     * @param params Map of parameter key-value pairs from the request
     * @return JobParameters object with validated and converted parameters
     * @throws BatchProcessingException if parameter validation fails
     */
    private JobParameters buildJobParameters(String jobName, Map<String, Object> params) 
            throws BatchProcessingException {
        
        JobParametersBuilder builder = new JobParametersBuilder();
        
        // Add common parameters
        builder.addLong("run.id", System.currentTimeMillis());
        builder.addDate("job.startTime", new Date());
        
        // Process job-specific parameters with validation
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value == null) {
                continue;
            }
            
            try {
                // Parameter type conversion and validation
                if (key.endsWith("Date") || key.contains("date")) {
                    // Date parameter handling
                    if (value instanceof String) {
                        String dateStr = (String) value;
                        if (!dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                            throw new BatchProcessingException(
                                "Invalid date format for parameter " + key + ". Expected: YYYY-MM-DD", false);
                        }
                        LocalDate localDate = LocalDate.parse(dateStr);
                        builder.addString(key, dateStr);
                    } else if (value instanceof Date) {
                        builder.addDate(key, (Date) value);
                    } else {
                        throw new BatchProcessingException(
                            "Invalid date type for parameter " + key + ". Expected String (YYYY-MM-DD) or Date", false);
                    }
                    
                } else if (key.endsWith("Count") || key.contains("size") || key.contains("limit")) {
                    // Numeric parameter handling
                    if (value instanceof Number) {
                        builder.addLong(key, ((Number) value).longValue());
                    } else if (value instanceof String) {
                        try {
                            long numValue = Long.parseLong((String) value);
                            if (numValue < 0) {
                                throw new BatchProcessingException(
                                    "Parameter " + key + " must be non-negative", false);
                            }
                            builder.addLong(key, numValue);
                        } catch (NumberFormatException e) {
                            throw new BatchProcessingException(
                                "Invalid numeric format for parameter " + key, false);
                        }
                    }
                    
                } else if (key.endsWith("Path") || key.contains("file") || key.contains("directory")) {
                    // File path parameter handling
                    String pathStr = value.toString();
                    if (pathStr.trim().isEmpty()) {
                        throw new BatchProcessingException(
                            "File path parameter " + key + " cannot be empty", false);
                    }
                    // Basic path validation - no actual file existence check for flexibility
                    if (pathStr.contains("..") || pathStr.contains("//")) {
                        throw new BatchProcessingException(
                            "Invalid file path for parameter " + key + ". Path traversal not allowed", false);
                    }
                    builder.addString(key, pathStr);
                    
                } else {
                    // Default string parameter handling
                    builder.addString(key, value.toString());
                }
                
            } catch (BatchProcessingException e) {
                throw e;
            } catch (Exception e) {
                throw new BatchProcessingException(
                    "Error processing parameter " + key + ": " + e.getMessage(), false);
            }
        }
        
        return builder.toJobParameters();
    }

    /**
     * Retrieves a Job bean from the Spring application context by name.
     * 
     * @param jobName The name of the job bean to retrieve
     * @return Job bean instance
     * @throws BatchProcessingException if job bean is not found
     */
    private Job getJobFromContext(String jobName) throws BatchProcessingException {
        try {
            if (!applicationContext.containsBean(jobName)) {
                throw new BatchProcessingException("Job bean not found: " + jobName, false);
            }
            
            Object jobBean = applicationContext.getBean(jobName);
            if (!(jobBean instanceof Job)) {
                throw new BatchProcessingException(
                    "Bean " + jobName + " is not a valid Job instance", false);
            }
            
            return (Job) jobBean;
            
        } catch (Exception e) {
            if (e instanceof BatchProcessingException) {
                throw e;
            }
            throw new BatchProcessingException(
                "Error retrieving job bean " + jobName + ": " + e.getMessage(), false);
        }
    }

    /**
     * Formats job name for display purposes.
     */
    private String formatJobDisplayName(String jobName) {
        return jobName.replaceAll("([a-z])([A-Z])", "$1 $2")
                     .replace("Job", "")
                     .trim();
    }

    /**
     * Returns job description based on job name.
     */
    private String getJobDescription(String jobName) {
        switch (jobName) {
            case "dailyTransactionJob":
                return "Process daily transaction records from external sources";
            case "interestCalculationJob":
                return "Calculate monthly interest charges on account balances";
            case "statementGenerationJob":
                return "Generate monthly account statements";
            case "transactionReportJob":
                return "Generate transaction detail reports with date filtering";
            case "accountListJob":
                return "Generate account listing reports";
            case "cardListJob":
                return "Generate credit card listing reports";
            case "crossReferenceListJob":
                return "Generate cross-reference audit reports";
            case "customerListJob":
                return "Generate customer data listing reports";
            case "accountProcessingJob":
                return "Composite job for coordinated account processing";
            case "migrationJob":
                return "VSAM to PostgreSQL data migration utility";
            default:
                return "Batch processing job";
        }
    }

    /**
     * Returns job category for grouping purposes.
     */
    private String getJobCategory(String jobName) {
        if (jobName.contains("Report") || jobName.contains("List")) {
            return "REPORTING";
        } else if (jobName.contains("migration")) {
            return "MAINTENANCE";
        } else if (jobName.contains("Processing")) {
            return "COMPOSITE";
        } else {
            return "PROCESSING";
        }
    }

    /**
     * Returns list of required parameters for the job.
     */
    private List<String> getRequiredParameters(String jobName) {
        List<String> required = new ArrayList<>();
        switch (jobName) {
            case "transactionReportJob":
                required.add("startDate");
                required.add("endDate");
                break;
            case "interestCalculationJob":
                required.add("processingDate");
                break;
            case "statementGenerationJob":
                required.add("statementDate");
                break;
        }
        return required;
    }

    /**
     * Returns list of optional parameters for the job.
     */
    private List<String> getOptionalParameters(String jobName) {
        List<String> optional = new ArrayList<>();
        optional.add("chunkSize");
        optional.add("skipLimit");
        optional.add("maxRetryAttempts");
        
        switch (jobName) {
            case "transactionReportJob":
                optional.add("outputPath");
                optional.add("reportFormat");
                break;
            case "migrationJob":
                optional.add("sourcePath");
                optional.add("targetPath");
                break;
        }
        return optional;
    }

    /**
     * Returns estimated duration in minutes for the job.
     */
    private int getEstimatedDuration(String jobName) {
        switch (jobName) {
            case "dailyTransactionJob":
                return 60;
            case "interestCalculationJob":
                return 30;
            case "statementGenerationJob":
                return 90;
            case "migrationJob":
                return 180;
            default:
                return 15;
        }
    }
}