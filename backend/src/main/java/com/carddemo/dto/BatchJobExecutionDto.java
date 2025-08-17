/**
 * BatchJobExecutionDto.java
 * 
 * Data Transfer Object for batch job execution information including job ID, status, timing, and parameters.
 * Used by REST API responses to provide structured job monitoring data replacing JCL job status queries.
 * 
 * This DTO class encapsulates the execution state and metadata for Spring Batch jobs that replace
 * the original COBOL batch programs (CBTRN01C, CBACT01C, etc.) in the modernized CardDemo application.
 * It provides a standardized way to communicate batch job execution information between the
 * Spring Boot backend services and the React frontend components.
 * 
 * The class includes fields for comprehensive job monitoring:
 * - Job identification and metadata
 * - Execution timing information
 * - Status and result tracking
 * - Error handling and diagnostics
 * - Parameter management for job configuration
 * 
 * This replaces the traditional JCL job monitoring capabilities with modern Spring Batch
 * job execution tracking suitable for cloud-native deployment environments.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024-01-01
 */
package com.carddemo.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.HashMap;

/**
 * Data Transfer Object representing batch job execution information for REST API responses.
 * 
 * This class provides a structured representation of batch job execution state that can be
 * serialized to JSON and transmitted to client applications for monitoring purposes.
 * It captures all essential information about a Spring Batch job execution including
 * identification, timing, status, parameters, and error details.
 * 
 * The DTO design supports the migration from mainframe JCL job monitoring to modern
 * cloud-native batch processing with equivalent functionality and enhanced observability.
 */
public class BatchJobExecutionDto {

    /**
     * Unique identifier for the job execution instance.
     * Corresponds to Spring Batch JobExecution.getId().
     */
    private Long jobId;
    
    /**
     * Name of the batch job being executed.
     * Examples: "dailyTransactionPosting", "accountDataReport", "interestCalculation"
     * Maps to the original COBOL program names (CBTRN01C, CBACT01C, etc.)
     */
    private String jobName;
    
    /**
     * Current execution status of the batch job.
     * Valid values: STARTING, STARTED, STOPPING, STOPPED, FAILED, COMPLETED, ABANDONED
     * Corresponds to Spring Batch BatchStatus enumeration.
     */
    private String status;
    
    /**
     * Timestamp when the job execution started.
     * Used for performance monitoring and execution duration calculation.
     */
    private LocalDateTime startTime;
    
    /**
     * Timestamp when the job execution completed or failed.
     * Null if the job is still running. Used with startTime to calculate execution duration.
     */
    private LocalDateTime endTime;
    
    /**
     * Job exit code indicating the final result of execution.
     * Maps to Spring Batch ExitStatus.getExitCode().
     * Common values: "COMPLETED", "FAILED", "STOPPED"
     * Numeric codes preserve COBOL APPL-RESULT semantics (0=success, 12=error, 16=EOF, etc.)
     */
    private String exitCode;
    
    /**
     * Map of job parameters passed during job execution.
     * Contains configuration values, file paths, date ranges, and other runtime parameters.
     * Equivalent to JCL symbolic parameters and COBOL program arguments.
     */
    private Map<String, String> jobParameters;
    
    /**
     * Detailed error message if the job execution failed.
     * Contains exception details, stack traces, and diagnostic information.
     * Helps with troubleshooting and error resolution in production environments.
     */
    private String errorMessage;

    /**
     * Default constructor for JSON deserialization and framework usage.
     * Initializes jobParameters to an empty HashMap to prevent null pointer exceptions.
     */
    public BatchJobExecutionDto() {
        this.jobParameters = new HashMap<>();
    }

    /**
     * Comprehensive constructor for creating fully populated DTO instances.
     * 
     * @param jobId Unique identifier for the job execution
     * @param jobName Name of the batch job
     * @param status Current execution status
     * @param startTime Job start timestamp
     * @param endTime Job completion timestamp (may be null for running jobs)
     * @param exitCode Job exit code indicating final result
     * @param jobParameters Map of job execution parameters
     * @param errorMessage Error details if job failed (may be null for successful jobs)
     */
    public BatchJobExecutionDto(Long jobId, String jobName, String status, 
                               LocalDateTime startTime, LocalDateTime endTime, 
                               String exitCode, Map<String, String> jobParameters, 
                               String errorMessage) {
        this.jobId = jobId;
        this.jobName = jobName;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.exitCode = exitCode;
        this.jobParameters = jobParameters != null ? new HashMap<>(jobParameters) : new HashMap<>();
        this.errorMessage = errorMessage;
    }

    /**
     * Constructor for successful job executions without error messages.
     * 
     * @param jobId Unique identifier for the job execution
     * @param jobName Name of the batch job
     * @param status Current execution status
     * @param startTime Job start timestamp
     * @param endTime Job completion timestamp
     * @param exitCode Job exit code
     * @param jobParameters Map of job execution parameters
     */
    public BatchJobExecutionDto(Long jobId, String jobName, String status, 
                               LocalDateTime startTime, LocalDateTime endTime, 
                               String exitCode, Map<String, String> jobParameters) {
        this(jobId, jobName, status, startTime, endTime, exitCode, jobParameters, null);
    }

    /**
     * Constructor for running jobs without completion information.
     * 
     * @param jobId Unique identifier for the job execution
     * @param jobName Name of the batch job
     * @param status Current execution status (typically "STARTED")
     * @param startTime Job start timestamp
     * @param jobParameters Map of job execution parameters
     */
    public BatchJobExecutionDto(Long jobId, String jobName, String status, 
                               LocalDateTime startTime, Map<String, String> jobParameters) {
        this(jobId, jobName, status, startTime, null, null, jobParameters, null);
    }

    /**
     * Returns the unique identifier for the job execution.
     * 
     * @return jobId as Long, or null if not set
     */
    public Long getJobId() {
        return jobId;
    }

    /**
     * Sets the unique identifier for the job execution.
     * 
     * @param jobId Unique job execution identifier
     */
    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    /**
     * Returns the name of the batch job.
     * 
     * @return jobName as String, or null if not set
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * Sets the name of the batch job.
     * 
     * @param jobName Name of the batch job
     */
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    /**
     * Returns the current execution status of the job.
     * 
     * @return status as String representing current job state
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the execution status of the job.
     * 
     * @param status Current job execution status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Returns the job start timestamp.
     * 
     * @return startTime as LocalDateTime, or null if not started
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * Sets the job start timestamp.
     * 
     * @param startTime Job execution start time
     */
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    /**
     * Returns the job completion timestamp.
     * 
     * @return endTime as LocalDateTime, or null if still running or not completed
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }

    /**
     * Sets the job completion timestamp.
     * 
     * @param endTime Job execution end time
     */
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    /**
     * Returns the job exit code indicating execution result.
     * 
     * @return exitCode as String, or null if not completed
     */
    public String getExitCode() {
        return exitCode;
    }

    /**
     * Sets the job exit code.
     * 
     * @param exitCode Final job execution result code
     */
    public void setExitCode(String exitCode) {
        this.exitCode = exitCode;
    }

    /**
     * Returns a map of job execution parameters.
     * 
     * @return jobParameters as Map&lt;String, String&gt;, never null (empty map if no parameters)
     */
    public Map<String, String> getJobParameters() {
        return new HashMap<>(jobParameters);
    }

    /**
     * Sets the job execution parameters.
     * 
     * @param jobParameters Map of parameter name-value pairs
     */
    public void setJobParameters(Map<String, String> jobParameters) {
        this.jobParameters = jobParameters != null ? new HashMap<>(jobParameters) : new HashMap<>();
    }

    /**
     * Adds a single parameter to the job parameters map.
     * 
     * @param key Parameter name
     * @param value Parameter value
     */
    public void addJobParameter(String key, String value) {
        if (this.jobParameters == null) {
            this.jobParameters = new HashMap<>();
        }
        this.jobParameters.put(key, value);
    }

    /**
     * Returns the error message if the job failed.
     * 
     * @return errorMessage as String, or null if no error occurred
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message for failed job executions.
     * 
     * @param errorMessage Detailed error information
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Calculates the job execution duration in milliseconds.
     * 
     * @return execution duration in milliseconds, or -1 if start/end times are not available
     */
    public long getExecutionDurationMillis() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
        return -1;
    }

    /**
     * Checks if the job execution is currently running.
     * 
     * @return true if status indicates the job is running, false otherwise
     */
    public boolean isRunning() {
        return "STARTED".equals(status) || "STARTING".equals(status);
    }

    /**
     * Checks if the job execution completed successfully.
     * 
     * @return true if status is COMPLETED and exitCode indicates success
     */
    public boolean isSuccessful() {
        return "COMPLETED".equals(status) && ("COMPLETED".equals(exitCode) || "0".equals(exitCode));
    }

    /**
     * Checks if the job execution failed.
     * 
     * @return true if status indicates failure
     */
    public boolean isFailed() {
        return "FAILED".equals(status);
    }

    /**
     * Checks if two BatchJobExecutionDto objects are equal based on jobId.
     * Two job executions are considered equal if they have the same jobId.
     * 
     * @param obj Object to compare with
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BatchJobExecutionDto that = (BatchJobExecutionDto) obj;
        return Objects.equals(jobId, that.jobId) &&
               Objects.equals(jobName, that.jobName) &&
               Objects.equals(status, that.status) &&
               Objects.equals(startTime, that.startTime) &&
               Objects.equals(endTime, that.endTime) &&
               Objects.equals(exitCode, that.exitCode) &&
               Objects.equals(jobParameters, that.jobParameters) &&
               Objects.equals(errorMessage, that.errorMessage);
    }

    /**
     * Generates hash code based on all fields.
     * 
     * @return hash code for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(jobId, jobName, status, startTime, endTime, 
                          exitCode, jobParameters, errorMessage);
    }

    /**
     * Returns a string representation of the BatchJobExecutionDto.
     * Includes all major fields for debugging and logging purposes.
     * 
     * @return formatted string representation
     */
    @Override
    public String toString() {
        return "BatchJobExecutionDto{" +
               "jobId=" + jobId +
               ", jobName='" + jobName + '\'' +
               ", status='" + status + '\'' +
               ", startTime=" + startTime +
               ", endTime=" + endTime +
               ", exitCode='" + exitCode + '\'' +
               ", jobParameters=" + jobParameters +
               ", errorMessage='" + errorMessage + '\'' +
               ", executionDuration=" + getExecutionDurationMillis() + "ms" +
               '}';
    }
}