/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.controller;

import com.carddemo.batch.BatchJobLauncher;
import com.carddemo.batch.BatchProcessingException;
import com.carddemo.dto.ApiRequest;
import com.carddemo.dto.ApiResponse;
import com.carddemo.dto.BatchJobExecutionDto;
import com.carddemo.dto.BatchJobParametersDto;
import com.carddemo.dto.Message;
import com.carddemo.dto.PageResponse;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for batch job management and monitoring in the CardDemo application.
 * 
 * This controller provides REST endpoints for managing Spring Batch jobs that replace
 * the legacy JCL-based batch processing system from the mainframe COBOL environment.
 * It handles manual batch job triggering, job status queries, and batch processing control
 * for jobs converted from COBOL programs like CBTRN01C, CBTRN02C, CBACT01C, and CBACT02C.
 * 
 * Key Features:
 * - Job execution triggering with parameter validation
 * - Real-time job status monitoring and tracking
 * - Paginated job execution history
 * - Job control operations (stop, restart)
 * - Standardized API responses with error handling
 * - Integration with Spring Batch infrastructure
 * 
 * Endpoints:
 * - POST /api/batch/jobs/{jobName}/run - Trigger batch job execution
 * - GET /api/batch/jobs/{jobId}/status - Get job execution status
 * - GET /api/batch/jobs - Get paginated job history
 * - POST /api/batch/jobs/{jobId}/stop - Stop running job
 * - POST /api/batch/jobs/{jobId}/restart - Restart failed job
 * - GET /api/batch/jobs/available - Get list of available job names
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@RestController
@RequestMapping("/api/batch")
public class BatchController {

    private static final Logger logger = LoggerFactory.getLogger(BatchController.class);

    @Autowired
    private BatchJobLauncher batchJobLauncher;

    /**
     * Triggers execution of a batch job with specified parameters.
     * 
     * This endpoint replaces JCL job submission for COBOL batch programs.
     * Supports job parameter passing, validation, and immediate execution.
     * 
     * @param jobName Name of the batch job to execute (e.g., "dailyTransactionJob")
     * @param request StandardApiRequest containing job parameters and metadata
     * @return ResponseEntity with job execution details or error information
     */
    @PostMapping("/jobs/{jobName}/run")
    public ResponseEntity<ApiResponse<BatchJobExecutionDto>> runJob(
            @PathVariable String jobName,
            @Valid @RequestBody ApiRequest<BatchJobParametersDto> request) {
        
        logger.info("Received request to run batch job: {} with request ID: {}", jobName, request.getRequestId());
        
        try {
            // Extract job parameters from request payload
            BatchJobParametersDto parametersDto = request.getPayload();
            if (parametersDto != null) {
                parametersDto.setJobName(jobName);
                parametersDto.validate();
            } else {
                // Create default parameters if none provided
                parametersDto = new BatchJobParametersDto(jobName, java.time.LocalDate.now());
            }

            // Convert DTO to Spring Batch JobParameters
            JobParameters jobParameters = parametersDto.toJobParameters();
            
            // Launch the batch job using BatchJobLauncher
            BatchJobExecutionDto executionDto = batchJobLauncher.launchJob(jobName, jobParameters);
            
            // Create successful response
            ApiResponse<BatchJobExecutionDto> response = ApiResponse.success();
            response.setData(executionDto);
            response.setMessage(new Message("INFO", "Batch job '" + jobName + "' launched successfully with execution ID: " + executionDto.getJobId()));
            
            logger.info("Successfully launched batch job: {} with execution ID: {}", jobName, executionDto.getJobId());
            return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
            
        } catch (BatchProcessingException e) {
            logger.error("Failed to launch batch job: {} - Error: {}", jobName, e.getMessage(), e);
            
            ApiResponse<BatchJobExecutionDto> errorResponse = ApiResponse.error("BATCH_JOB_LAUNCH_FAILED", e.getMessage());
            errorResponse.setMessage(new Message("ERROR", "Failed to launch batch job '" + jobName + "': " + e.getMessage()));
            
            HttpStatus status = e.isRetryable() ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.BAD_REQUEST;
            return new ResponseEntity<>(errorResponse, status);
            
        } catch (Exception e) {
            logger.error("Unexpected error launching batch job: {} - Error: {}", jobName, e.getMessage(), e);
            
            ApiResponse<BatchJobExecutionDto> errorResponse = ApiResponse.error("UNEXPECTED_ERROR", "An unexpected error occurred while launching the batch job");
            errorResponse.setMessage(new Message("ERROR", "Unexpected error occurred: " + e.getMessage()));
            
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retrieves the status of a specific batch job execution.
     * 
     * Provides real-time status information including execution state,
     * timing information, exit codes, and any error messages.
     * 
     * @param jobId Job execution ID to query
     * @return ResponseEntity with job execution status details
     */
    @GetMapping("/jobs/{jobId}/status")
    public ResponseEntity<ApiResponse<BatchJobExecutionDto>> getJobStatus(@PathVariable Long jobId) {
        
        logger.info("Received request to get status for job execution ID: {}", jobId);
        
        try {
            BatchJobExecutionDto executionDto = batchJobLauncher.getJobStatus(jobId);
            
            if (executionDto != null) {
                ApiResponse<BatchJobExecutionDto> response = ApiResponse.success();
                response.setData(executionDto);
                response.setMessage(new Message("INFO", "Job status retrieved successfully for execution ID: " + jobId));
                
                logger.info("Successfully retrieved status for job execution ID: {} - Status: {}", jobId, executionDto.getStatus());
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                logger.warn("Job execution not found for ID: {}", jobId);
                
                ApiResponse<BatchJobExecutionDto> notFoundResponse = ApiResponse.error("JOB_EXECUTION_NOT_FOUND", "Job execution not found with ID: " + jobId);
                notFoundResponse.setMessage(new Message("WARNING", "Job execution not found with ID: " + jobId));
                
                return new ResponseEntity<>(notFoundResponse, HttpStatus.NOT_FOUND);
            }
            
        } catch (BatchProcessingException e) {
            logger.error("Failed to retrieve job status for execution ID: {} - Error: {}", jobId, e.getMessage(), e);
            
            ApiResponse<BatchJobExecutionDto> errorResponse = ApiResponse.error("BATCH_STATUS_ERROR", e.getMessage());
            errorResponse.setMessage(new Message("ERROR", "Failed to retrieve job status: " + e.getMessage()));
            
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            
        } catch (Exception e) {
            logger.error("Unexpected error retrieving job status for execution ID: {} - Error: {}", jobId, e.getMessage(), e);
            
            ApiResponse<BatchJobExecutionDto> errorResponse = ApiResponse.error("UNEXPECTED_ERROR", "An unexpected error occurred while retrieving job status");
            errorResponse.setMessage(new Message("ERROR", "Unexpected error occurred: " + e.getMessage()));
            
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retrieves paginated history of batch job executions.
     * 
     * Supports filtering by job name, status, date range, and pagination.
     * Useful for monitoring, troubleshooting, and operational reporting.
     * 
     * @param page Page number (0-based)
     * @param size Number of results per page
     * @param jobName Optional job name filter
     * @param status Optional status filter
     * @return ResponseEntity with paginated job execution history
     */
    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<PageResponse<BatchJobExecutionDto>>> getJobHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String jobName,
            @RequestParam(required = false) String status) {
        
        logger.info("Received request for job history - page: {}, size: {}, jobName: {}, status: {}", 
                   page, size, jobName, status);
        
        try {
            // Get job executions from BatchJobLauncher
            List<BatchJobExecutionDto> jobExecutions = batchJobLauncher.getJobExecutions(page, size, jobName, status);
            
            // Create page response (assuming total count is available from launcher)
            long totalElements = getTotalJobExecutions(jobName, status);
            PageResponse<BatchJobExecutionDto> pageResponse = PageResponse.of(
                jobExecutions, page, size, totalElements
            );
            
            ApiResponse<PageResponse<BatchJobExecutionDto>> response = ApiResponse.success();
            response.setData(pageResponse);
            response.setMessage(new Message("INFO", "Job history retrieved successfully - found " + jobExecutions.size() + " executions"));
            
            logger.info("Successfully retrieved job history - page: {}, size: {}, total: {}", 
                       page, size, totalElements);
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (BatchProcessingException e) {
            logger.error("Failed to retrieve job history - Error: {}", e.getMessage(), e);
            
            ApiResponse<PageResponse<BatchJobExecutionDto>> errorResponse = ApiResponse.error("BATCH_HISTORY_ERROR", e.getMessage());
            errorResponse.setMessage(new Message("ERROR", "Failed to retrieve job history: " + e.getMessage()));
            
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            
        } catch (Exception e) {
            logger.error("Unexpected error retrieving job history - Error: {}", e.getMessage(), e);
            
            ApiResponse<PageResponse<BatchJobExecutionDto>> errorResponse = ApiResponse.error("UNEXPECTED_ERROR", "An unexpected error occurred while retrieving job history");
            errorResponse.setMessage(new Message("ERROR", "Unexpected error occurred: " + e.getMessage()));
            
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Stops a running batch job execution.
     * 
     * Attempts to gracefully stop the specified job execution.
     * Jobs in certain states may not be stoppable.
     * 
     * @param jobId Job execution ID to stop
     * @return ResponseEntity with operation result
     */
    @PostMapping("/jobs/{jobId}/stop")
    public ResponseEntity<ApiResponse<BatchJobExecutionDto>> stopJob(@PathVariable Long jobId) {
        
        logger.info("Received request to stop job execution ID: {}", jobId);
        
        try {
            BatchJobExecutionDto executionDto = batchJobLauncher.stopJob(jobId);
            
            ApiResponse<BatchJobExecutionDto> response = ApiResponse.success();
            response.setData(executionDto);
            response.setMessage(new Message("INFO", "Job stop request processed for execution ID: " + jobId));
            
            logger.info("Successfully processed stop request for job execution ID: {} - Status: {}", jobId, executionDto.getStatus());
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (BatchProcessingException e) {
            logger.error("Failed to stop job execution ID: {} - Error: {}", jobId, e.getMessage(), e);
            
            ApiResponse<BatchJobExecutionDto> errorResponse = ApiResponse.error("BATCH_STOP_ERROR", e.getMessage());
            errorResponse.setMessage(new Message("ERROR", "Failed to stop job: " + e.getMessage()));
            
            HttpStatus status = e.isRetryable() ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.BAD_REQUEST;
            return new ResponseEntity<>(errorResponse, status);
            
        } catch (Exception e) {
            logger.error("Unexpected error stopping job execution ID: {} - Error: {}", jobId, e.getMessage(), e);
            
            ApiResponse<BatchJobExecutionDto> errorResponse = ApiResponse.error("UNEXPECTED_ERROR", "An unexpected error occurred while stopping the job");
            errorResponse.setMessage(new Message("ERROR", "Unexpected error occurred: " + e.getMessage()));
            
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Restarts a failed batch job execution.
     * 
     * Attempts to restart the specified job execution from where it failed.
     * Only failed job executions can be restarted.
     * 
     * @param jobId Job execution ID to restart
     * @return ResponseEntity with new job execution details
     */
    @PostMapping("/jobs/{jobId}/restart")
    public ResponseEntity<ApiResponse<BatchJobExecutionDto>> restartJob(@PathVariable Long jobId) {
        
        logger.info("Received request to restart job execution ID: {}", jobId);
        
        try {
            BatchJobExecutionDto executionDto = batchJobLauncher.restartJob(jobId);
            
            ApiResponse<BatchJobExecutionDto> response = ApiResponse.success();
            response.setData(executionDto);
            response.setMessage(new Message("INFO", "Job restarted successfully - new execution ID: " + executionDto.getJobId()));
            
            logger.info("Successfully restarted job execution ID: {} - New execution ID: {}", jobId, executionDto.getJobId());
            return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
            
        } catch (BatchProcessingException e) {
            logger.error("Failed to restart job execution ID: {} - Error: {}", jobId, e.getMessage(), e);
            
            ApiResponse<BatchJobExecutionDto> errorResponse = ApiResponse.error("BATCH_RESTART_ERROR", e.getMessage());
            errorResponse.setMessage(new Message("ERROR", "Failed to restart job: " + e.getMessage()));
            
            HttpStatus status = e.isRetryable() ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.BAD_REQUEST;
            return new ResponseEntity<>(errorResponse, status);
            
        } catch (Exception e) {
            logger.error("Unexpected error restarting job execution ID: {} - Error: {}", jobId, e.getMessage(), e);
            
            ApiResponse<BatchJobExecutionDto> errorResponse = ApiResponse.error("UNEXPECTED_ERROR", "An unexpected error occurred while restarting the job");
            errorResponse.setMessage(new Message("ERROR", "Unexpected error occurred: " + e.getMessage()));
            
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retrieves list of available batch job names.
     * 
     * Returns all batch job definitions available for execution,
     * including converted COBOL batch programs.
     * 
     * @return ResponseEntity with list of available job names
     */
    @GetMapping("/jobs/available")
    public ResponseEntity<ApiResponse<List<String>>> getAvailableJobs() {
        
        logger.info("Received request for available job names");
        
        try {
            List<String> jobNames = batchJobLauncher.getJobNames();
            
            ApiResponse<List<String>> response = ApiResponse.success();
            response.setData(jobNames);
            response.setMessage(new Message("INFO", "Retrieved " + jobNames.size() + " available job names"));
            
            logger.info("Successfully retrieved {} available job names", jobNames.size());
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (BatchProcessingException e) {
            logger.error("Failed to retrieve available job names - Error: {}", e.getMessage(), e);
            
            ApiResponse<List<String>> errorResponse = ApiResponse.error("BATCH_JOBS_ERROR", e.getMessage());
            errorResponse.setMessage(new Message("ERROR", "Failed to retrieve available jobs: " + e.getMessage()));
            
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            
        } catch (Exception e) {
            logger.error("Unexpected error retrieving available job names - Error: {}", e.getMessage(), e);
            
            ApiResponse<List<String>> errorResponse = ApiResponse.error("UNEXPECTED_ERROR", "An unexpected error occurred while retrieving available jobs");
            errorResponse.setMessage(new Message("ERROR", "Unexpected error occurred: " + e.getMessage()));
            
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Helper method to get total count of job executions for pagination.
     * This would typically call a count method on BatchJobLauncher.
     * 
     * @param jobName Optional job name filter
     * @param status Optional status filter  
     * @return Total count of matching job executions
     */
    private long getTotalJobExecutions(String jobName, String status) {
        try {
            // Assuming BatchJobLauncher has a count method (would need to be added)
            // For now, we'll estimate based on the page size
            return batchJobLauncher.getJobExecutions(0, Integer.MAX_VALUE, jobName, status).size();
        } catch (Exception e) {
            logger.warn("Failed to get total job execution count, returning 0", e);
            return 0;
        }
    }
}